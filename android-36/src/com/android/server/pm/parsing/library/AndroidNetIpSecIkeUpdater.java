/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.server.pm.parsing.library;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.pm.parsing.pkg.ParsedPackage;

/**
 * Updates a package to remove dependency on android.net.ipsec.ike library.
 *
 * @hide
 */
@VisibleForTesting
public class AndroidNetIpSecIkeUpdater extends PackageSharedLibraryUpdater {

    private static final String LIBRARY_NAME = "android.net.ipsec.ike";

    @Override
    public void updatePackage(ParsedPackage parsedPackage, boolean isSystemApp,
            boolean isUpdatedSystemApp) {
        removeLibrary(parsedPackage, LIBRARY_NAME);
    }
}
