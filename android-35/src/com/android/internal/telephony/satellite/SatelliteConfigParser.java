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

package com.android.internal.telephony.satellite;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.util.Log;

import com.android.internal.telephony.configupdate.ConfigParser;
import com.android.internal.telephony.satellite.nano.SatelliteConfigData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


/**
 * SatelliteConfigParser parses the config data and create SatelliteConfig.
 * The config data is located at "/data/misc/telephonyconfig/telephony_config.pb".
 * It is obtained through the getConfigParser() at the TelephonyConfigUpdateInstallReceiver.
 */
public class SatelliteConfigParser extends ConfigParser<SatelliteConfig> {
    private static final String TAG = "SatelliteConfigParser";

    /**
     * Create an instance of SatelliteConfigParser with byte array data.
     *
     * @param data the config data formatted as byte array.
     */
    public SatelliteConfigParser(@Nullable byte[] data) {
        super(data);
    }

    /**
     * Create an instance of SatelliteConfigParser with InputStream data.
     *
     * @param input the config data formatted as InputStream.
     */
    public SatelliteConfigParser(@NonNull InputStream input)
            throws IOException {
        super(input);
    }

    /**
     * Create an instance of SatelliteConfigParser with File data.
     *
     * @param file the config data formatted as File.
     */
    public SatelliteConfigParser(@NonNull File file) throws IOException {
        super(file);
    }

    @Override
    protected void parseData(@Nullable byte[] data) {
        boolean parseError = false;
        try {
            if (data == null) {
                Log.d(TAG, "config data is null");
                return;
            }
            SatelliteConfigData.TelephonyConfigProto telephonyConfigData =
                    SatelliteConfigData.TelephonyConfigProto.parseFrom(data);
            if (telephonyConfigData == null || telephonyConfigData.satellite == null) {
                Log.e(TAG, "telephonyConfigData or telephonyConfigData.satellite is null");
                return;
            }
            mVersion = telephonyConfigData.satellite.version;
            mConfig = new SatelliteConfig(telephonyConfigData.satellite);
            Log.d(TAG, "SatelliteConfig is created");
        } catch (Exception e) {
            parseError = true;
            Log.e(TAG, "Parse Error : " + e.getMessage());
        } finally {
            if (parseError) {
                mVersion = VERSION_UNKNOWN;
                mConfig = null;
            }
        }
    }
}
