/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.server.updates;

import android.content.Context;
import android.content.Intent;

import java.io.File;

public class CertPinInstallReceiver extends ConfigUpdateInstallReceiver {
    private static final String KEYCHAIN_DIR = "/data/misc/keychain/";

    public CertPinInstallReceiver() {
        super("/data/misc/keychain/", "pins", "metadata/", "version");
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (com.android.server.flags.Flags.certpininstallerRemoval()) {
                File pins = new File(KEYCHAIN_DIR + "pins");
                if (pins.exists()) {
                    pins.delete();
                }
                File version = new File(KEYCHAIN_DIR + "metadata/version");
                if (version.exists()) {
                    version.delete();
                }
                File metadata = new File(KEYCHAIN_DIR + "metadata");
                if (metadata.exists()) {
                    metadata.delete();
                }
            }
        } else if (!com.android.server.flags.Flags.certpininstallerRemoval()) {
            super.onReceive(context, intent);
        }
    }
}
