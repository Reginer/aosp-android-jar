/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.internal.os;

import android.os.Bundle;

/**
 * "Backend" interface used by {@link android.os.BinaryTransparencyManager} to talk to the
 * BinaryTransparencyService that actually implements the measurement and information aggregation
 * functionality.
 *
 * @see BinaryTransparencyManager
 */
interface IBinaryTransparencyService {
    String getSignedImageInfo();

    void recordMeasurementsForAllPackages();

    parcelable ApexInfo {
        String packageName;
        long longVersion;
        byte[] digest;
        int digestAlgorithm;
        String[] signerDigests;

        // Test only
        String moduleName;
    }

    parcelable AppInfo {
        String packageName;
        long longVersion;
        String splitName;
        byte[] digest;
        int digestAlgorithm;
        String[] signerDigests;
        int mbaStatus;
        String initiator;
        String[] initiatorSignerDigests;
        String installer;
        String originator;
    }

    /** Test only */
    List<ApexInfo> collectAllApexInfo(boolean includeTestOnly);
    List<AppInfo> collectAllUpdatedPreloadInfo(in Bundle packagesToSkip);
    List<AppInfo> collectAllSilentInstalledMbaInfo(in Bundle packagesToSkip);
}