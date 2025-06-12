/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony.configupdate;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.os.FileUtils;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.satellite.SatelliteConfig;
import com.android.internal.telephony.satellite.SatelliteConfigParser;
import com.android.internal.telephony.satellite.SatelliteConstants;
import com.android.internal.telephony.satellite.metrics.ConfigUpdaterMetricsStats;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.server.updates.ConfigUpdateInstallReceiver;

import libcore.io.IoUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class TelephonyConfigUpdateInstallReceiver extends ConfigUpdateInstallReceiver implements
        ConfigProviderAdaptor {

    private static final String TAG = "TelephonyConfigUpdateInstallReceiver";
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected static final String UPDATE_DIR = "/data/misc/telephonyconfig";
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected static final String NEW_CONFIG_CONTENT_PATH = "new_telephony_config.pb";
    protected static final String VALID_CONFIG_CONTENT_PATH = "valid_telephony_config.pb";
    protected static final String UPDATE_METADATA_PATH = "metadata/";
    public static final String VERSION = "version";

    private ConcurrentHashMap<Executor, Callback> mCallbackHashMap = new ConcurrentHashMap<>();
    @NonNull
    private final Object mConfigParserLock = new Object();
    @GuardedBy("mConfigParserLock")
    private ConfigParser mConfigParser;
    @NonNull private final ConfigUpdaterMetricsStats mConfigUpdaterMetricsStats;


    public static TelephonyConfigUpdateInstallReceiver sReceiverAdaptorInstance =
            new TelephonyConfigUpdateInstallReceiver();

    /**
     * @return The singleton instance of TelephonyConfigUpdateInstallReceiver
     */
    @NonNull
    public static TelephonyConfigUpdateInstallReceiver getInstance() {
        return sReceiverAdaptorInstance;
    }

    public TelephonyConfigUpdateInstallReceiver() {
        super(UPDATE_DIR, NEW_CONFIG_CONTENT_PATH, UPDATE_METADATA_PATH, VERSION);
        mConfigUpdaterMetricsStats = ConfigUpdaterMetricsStats.getOrCreateInstance();
    }

    /**
     * @return byte array type of config data protobuffer file
     */
    @Nullable
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public byte[] getContentFromContentPath(@NonNull File contentPath) {
        try {
            return IoUtils.readFileAsByteArray(contentPath.getCanonicalPath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to read current content : " + contentPath);
            return null;
        }
    }

    /**
     * @param parser target of validation.
     * @return {@code true} if all the config data are valid {@code false} otherwise.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public boolean isValidSatelliteCarrierConfigData(@NonNull ConfigParser parser) {
        SatelliteConfig satelliteConfig = (SatelliteConfig) parser.getConfig();
        if (satelliteConfig == null) {
            Log.e(TAG, "satelliteConfig is null");
            mConfigUpdaterMetricsStats.reportOemAndCarrierConfigError(
                    SatelliteConstants.CONFIG_UPDATE_RESULT_NO_SATELLITE_DATA);
            return false;
        }

        // If no carrier config exist then it is considered as a valid config
        Set<Integer> carrierIds = satelliteConfig.getAllSatelliteCarrierIds();
        for (int carrierId : carrierIds) {
            Map<String, Set<Integer>> plmnsServices =
                    satelliteConfig.getSupportedSatelliteServices(carrierId);
            Set<String> plmns = plmnsServices.keySet();
            for (String plmn : plmns) {
                if (!TelephonyUtils.isValidPlmn(plmn)) {
                    Log.e(TAG, "found invalid plmn : " + plmn);
                    mConfigUpdaterMetricsStats.reportCarrierConfigError(
                            SatelliteConstants.CONFIG_UPDATE_RESULT_CARRIER_DATA_INVALID_PLMN);
                    return false;
                }
                Set<Integer> serviceSet = plmnsServices.get(plmn);
                for (int service : serviceSet) {
                    if (!TelephonyUtils.isValidService(service)) {
                        Log.e(TAG, "found invalid service : " + service);
                        mConfigUpdaterMetricsStats.reportCarrierConfigError(SatelliteConstants
                                .CONFIG_UPDATE_RESULT_CARRIER_DATA_INVALID_SUPPORTED_SERVICES);
                        return false;
                    }
                }
            }
        }
        Log.d(TAG, "the config data is valid");
        return true;
    }


    @Override
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PROTECTED)
    public void postInstall(Context context, Intent intent) {
        Log.d(TAG, "Telephony config is updated in file partition");

        ConfigParser newConfigParser = getNewConfigParser(DOMAIN_SATELLITE,
                getContentFromContentPath(updateContent));

        if (newConfigParser == null) {
            Log.e(TAG, "newConfigParser is null");
            return;
        }

        if (!isValidSatelliteCarrierConfigData(newConfigParser)) {
            Log.e(TAG, "received config data has invalid satellite carrier config data");
            return;
        }

        synchronized (getInstance().mConfigParserLock) {
            if (getInstance().mConfigParser != null) {
                int updatedVersion = newConfigParser.mVersion;
                int previousVersion = getInstance().mConfigParser.mVersion;
                Log.d(TAG, "previous proto version is " + previousVersion
                        + " | updated proto version is " + updatedVersion);

                if (updatedVersion <= previousVersion) {
                    Log.e(TAG, "updated proto Version [" + updatedVersion
                            + "] is smaller than previous proto Version [" + previousVersion + "]");
                    mConfigUpdaterMetricsStats.reportOemAndCarrierConfigError(
                            SatelliteConstants.CONFIG_UPDATE_RESULT_INVALID_VERSION);
                    return;
                }
            }
            getInstance().mConfigParser = newConfigParser;
            mConfigUpdaterMetricsStats.setConfigVersion(getInstance().mConfigParser.getVersion());
        }

        if (!getInstance().mCallbackHashMap.keySet().isEmpty()) {
            Iterator<Executor> iterator = getInstance().mCallbackHashMap.keySet().iterator();
            while (iterator.hasNext()) {
                Executor executor = iterator.next();
                getInstance().mCallbackHashMap.get(executor).onChanged(newConfigParser);
            }
        }

        if (!copySourceFileToTargetFile(NEW_CONFIG_CONTENT_PATH, VALID_CONFIG_CONTENT_PATH)) {
            Log.e(TAG, "fail to copy to the valid satellite carrier config data");
            mConfigUpdaterMetricsStats.reportOemAndCarrierConfigError(
                    SatelliteConstants.CONFIG_UPDATE_RESULT_IO_ERROR);
        }
    }

    @Nullable
    @Override
    public ConfigParser getConfigParser(String domain) {
        Log.d(TAG, "getConfigParser");
        synchronized (getInstance().mConfigParserLock) {
            if (getInstance().mConfigParser == null) {
                Log.d(TAG, "CreateNewConfigParser with domain " + domain);
                getInstance().mConfigParser = getNewConfigParser(
                        domain, getContentFromContentPath(new File(updateDir,
                                VALID_CONFIG_CONTENT_PATH)));
            }
            return getInstance().mConfigParser;
        }
    }

    @Override
    public void registerCallback(@NonNull Executor executor, @NonNull Callback callback) {
        mCallbackHashMap.put(executor, callback);
    }

    @Override
    public void unregisterCallback(@NonNull Callback callback) {
        Iterator<Executor> iterator = mCallbackHashMap.keySet().iterator();
        while (iterator.hasNext()) {
            Executor executor = iterator.next();
            if (mCallbackHashMap.get(executor) == callback) {
                mCallbackHashMap.remove(executor);
                break;
            }
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public File getUpdateDir() {
        return getInstance().updateDir;
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public File getUpdateContent() {
        return getInstance().updateContent;
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public ConcurrentHashMap<Executor, Callback> getCallbackMap() {
        return getInstance().mCallbackHashMap;
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void setCallbackMap(ConcurrentHashMap<Executor, Callback> map) {
        getInstance().mCallbackHashMap = map;
    }

    /**
     * @param data byte array type of config data
     * @return when data is null, return null otherwise return ConfigParser
     */
    @Nullable
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public ConfigParser getNewConfigParser(String domain, @Nullable byte[] data) {
        if (data == null) {
            Log.d(TAG, "content data is null");
            mConfigUpdaterMetricsStats.reportOemAndCarrierConfigError(
                    SatelliteConstants.CONFIG_UPDATE_RESULT_NO_DATA);
            return null;
        }
        switch (domain) {
            case DOMAIN_SATELLITE:
                return new SatelliteConfigParser(data);
            default:
                Log.e(TAG, "DOMAIN should be specified");
                mConfigUpdaterMetricsStats.reportOemAndCarrierConfigError(
                        SatelliteConstants.CONFIG_UPDATE_RESULT_INVALID_DOMAIN);
                return null;
        }
    }

    /**
     * @param sourceFileName source file name
     * @param targetFileName target file name
     * @return {@code true} if successful, {@code false} otherwise
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public boolean copySourceFileToTargetFile(
            @NonNull String sourceFileName, @NonNull String targetFileName) {
        try {
            File sourceFile = new File(UPDATE_DIR, sourceFileName);
            File targetFile = new File(UPDATE_DIR, targetFileName);
            Log.d(TAG, "copy " + sourceFile.getName() + " >> " + targetFile.getName());

            if (sourceFile.exists()) {
                if (targetFile.exists()) {
                    targetFile.delete();
                }
                FileUtils.copy(sourceFile, targetFile);
                FileUtils.copyPermissions(sourceFile, targetFile);
                Log.d(TAG, "success to copy the file " + sourceFile.getName() + " to "
                        + targetFile.getName());
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "copy error : " + e);
            return false;
        }
        Log.d(TAG, "source file is not exist, no file to copy");
        return false;
    }
}
