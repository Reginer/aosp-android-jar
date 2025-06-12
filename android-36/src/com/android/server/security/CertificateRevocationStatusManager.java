/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.server.security;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Binder;
import android.os.Environment;
import android.util.AtomicFile;
import android.util.Slog;

import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Manages the revocation status of certificates used in remote attestation. */
class CertificateRevocationStatusManager {
    private static final String TAG = "AVF_CRL";
    // Must be unique within system server
    private static final int JOB_ID = 1737671340;
    private static final String REVOCATION_LIST_FILE_NAME = "certificate_revocation_list.json";
    @VisibleForTesting static final int MAX_OFFLINE_REVOCATION_LIST_AGE_DAYS = 30;

    @VisibleForTesting static final int NUM_HOURS_BEFORE_NEXT_FETCH = 24;

    private static final String TOP_LEVEL_JSON_PROPERTY_KEY = "entries";
    private static final Object sFileLock = new Object();

    private final Context mContext;
    private final String mTestRemoteRevocationListUrl;
    private final File mTestStoredRevocationListFile;
    private final boolean mShouldScheduleJob;

    CertificateRevocationStatusManager(Context context) {
        this(context, null, null, true);
    }

    @VisibleForTesting
    CertificateRevocationStatusManager(
            Context context,
            String testRemoteRevocationListUrl,
            File testStoredRevocationListFile,
            boolean shouldScheduleJob) {
        mContext = context;
        mTestRemoteRevocationListUrl = testRemoteRevocationListUrl;
        mTestStoredRevocationListFile = testStoredRevocationListFile;
        mShouldScheduleJob = shouldScheduleJob;
    }

    /**
     * Check the revocation status of the provided {@link X509Certificate}s.
     *
     * @param certificates List of certificates to be checked
     * @throws CertPathValidatorException if the check failed
     */
    void checkRevocationStatus(List<X509Certificate> certificates)
            throws CertPathValidatorException {
        List<String> serialNumbers = new ArrayList<>();
        for (X509Certificate certificate : certificates) {
            String serialNumber = certificate.getSerialNumber().toString(16);
            if (serialNumber == null) {
                throw new CertPathValidatorException("Certificate serial number cannot be null.");
            }
            serialNumbers.add(serialNumber);
        }
        LocalDateTime now = LocalDateTime.now();
        JSONObject revocationList;
        try {
            if (getLastModifiedDateTime(getRevocationListFile())
                    .isAfter(now.minusHours(NUM_HOURS_BEFORE_NEXT_FETCH))) {
                Slog.d(TAG, "CRL is fetched recently, do not fetch again.");
                revocationList = getStoredRevocationList();
                checkRevocationStatus(revocationList, serialNumbers);
                return;
            }
        } catch (IOException | JSONException ignored) {
            // Proceed to fetch the remote revocation list
        }
        try {
            byte[] revocationListBytes = fetchRemoteRevocationListBytes();
            silentlyStoreRevocationList(revocationListBytes);
            revocationList = parseRevocationList(revocationListBytes);
            checkRevocationStatus(revocationList, serialNumbers);
        } catch (IOException | JSONException ex) {
            Slog.d(TAG, "Fallback to check stored revocation status", ex);
            if (ex instanceof IOException && mShouldScheduleJob) {
                Binder.withCleanCallingIdentity(this::scheduleJobToFetchRemoteRevocationJob);
            }
            try {
                revocationList = getStoredRevocationList();
                checkRevocationStatus(revocationList, serialNumbers);
            } catch (IOException | JSONException ex2) {
                throw new CertPathValidatorException(
                        "Unable to load or parse stored revocation status", ex2);
            }
        }
    }

    private static void checkRevocationStatus(JSONObject revocationList, List<String> serialNumbers)
            throws CertPathValidatorException {
        for (String serialNumber : serialNumbers) {
            if (revocationList.has(serialNumber)) {
                throw new CertPathValidatorException(
                        "Certificate has been revoked: " + serialNumber);
            }
        }
    }

    private JSONObject getStoredRevocationList() throws IOException, JSONException {
        File offlineRevocationListFile = getRevocationListFile();
        if (!offlineRevocationListFile.exists()
                || isRevocationListExpired(offlineRevocationListFile)) {
            throw new FileNotFoundException("Offline copy does not exist or has expired.");
        }
        synchronized (sFileLock) {
            try (FileInputStream inputStream = new FileInputStream(offlineRevocationListFile)) {
                return parseRevocationList(inputStream.readAllBytes());
            }
        }
    }

    private boolean isRevocationListExpired(File offlineRevocationListFile) {
        LocalDateTime acceptableLastModifiedDate =
                LocalDateTime.now().minusDays(MAX_OFFLINE_REVOCATION_LIST_AGE_DAYS);
        LocalDateTime lastModifiedDate = getLastModifiedDateTime(offlineRevocationListFile);
        return lastModifiedDate.isBefore(acceptableLastModifiedDate);
    }

    private static LocalDateTime getLastModifiedDateTime(File file) {
        // if the file does not exist, file.lastModified() returns 0, so this method returns the
        // epoch time
        return LocalDateTime.ofEpochSecond(
                file.lastModified() / 1000, 0, OffsetDateTime.now().getOffset());
    }

    /**
     * Store the provided bytes to the local revocation list file.
     *
     * <p>This method does not throw an exception even if it fails to store the bytes.
     *
     * <p>This method internally synchronize file access with other methods in this class.
     *
     * @param revocationListBytes The bytes to store to the local revocation list file.
     */
    void silentlyStoreRevocationList(byte[] revocationListBytes) {
        synchronized (sFileLock) {
            AtomicFile atomicRevocationListFile = new AtomicFile(getRevocationListFile());
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = atomicRevocationListFile.startWrite();
                fileOutputStream.write(revocationListBytes);
                atomicRevocationListFile.finishWrite(fileOutputStream);
                Slog.d(TAG, "Successfully stored revocation list.");
            } catch (IOException ex) {
                Slog.e(TAG, "Failed to store the certificate revocation list.", ex);
                // this happens when fileOutputStream.write fails
                if (fileOutputStream != null) {
                    atomicRevocationListFile.failWrite(fileOutputStream);
                }
            }
        }
    }

    private File getRevocationListFile() {
        if (mTestStoredRevocationListFile != null) {
            return mTestStoredRevocationListFile;
        }
        return new File(Environment.getDataSystemDirectory(), REVOCATION_LIST_FILE_NAME);
    }

    private void scheduleJobToFetchRemoteRevocationJob() {
        JobScheduler jobScheduler = mContext.getSystemService(JobScheduler.class);
        if (jobScheduler == null) {
            Slog.e(TAG, "Unable to get job scheduler.");
            return;
        }
        Slog.d(TAG, "Scheduling job to fetch remote CRL.");
        jobScheduler.forNamespace(TAG).schedule(
                new JobInfo.Builder(
                                JOB_ID,
                                new ComponentName(
                                        mContext,
                                        UpdateCertificateRevocationStatusJobService.class))
                        .setRequiredNetwork(
                                new NetworkRequest.Builder()
                                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                        .build())
                        .build());
    }

    /**
     * Fetches the revocation list from the URL specified in
     * R.string.vendor_required_attestation_revocation_list_url
     *
     * @return The remote revocation list entries in a byte[].
     * @throws CertPathValidatorException if the URL is not defined or is malformed.
     * @throws IOException if the URL is valid but the fetch failed.
     */
    byte[] fetchRemoteRevocationListBytes() throws CertPathValidatorException, IOException {
        String urlString = getRemoteRevocationListUrl();
        if (urlString == null || urlString.isEmpty()) {
            throw new CertPathValidatorException(
                    "R.string.vendor_required_attestation_revocation_list_url is empty.");
        }
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            throw new CertPathValidatorException("Unable to parse the URL " + urlString, ex);
        }
        try (InputStream inputStream = url.openStream()) {
            return inputStream.readAllBytes();
        }
    }

    private JSONObject parseRevocationList(byte[] revocationListBytes)
            throws IOException, JSONException {
        JSONObject revocationListJson = new JSONObject(new String(revocationListBytes, UTF_8));
        return revocationListJson.getJSONObject(TOP_LEVEL_JSON_PROPERTY_KEY);
    }

    private String getRemoteRevocationListUrl() {
        if (mTestRemoteRevocationListUrl != null) {
            return mTestRemoteRevocationListUrl;
        }
        return mContext.getResources()
                .getString(R.string.vendor_required_attestation_revocation_list_url);
    }
}
