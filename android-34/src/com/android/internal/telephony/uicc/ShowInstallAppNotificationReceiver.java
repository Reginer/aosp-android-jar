/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.internal.telephony.uicc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receiver used to show a notification that prompts the user to install the package (contained in
 * string extras) from the play store
 */
public class ShowInstallAppNotificationReceiver extends BroadcastReceiver {
    private static final String EXTRA_PACKAGE_NAME = "package_name";

    /** Returns intent used to send a broadcast to this receiver */
    public static Intent get(Context context, String pkgName) {
        Intent intent = new Intent(context, ShowInstallAppNotificationReceiver.class);
        intent.putExtra(EXTRA_PACKAGE_NAME, pkgName);
        return intent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String pkgName = intent.getStringExtra(EXTRA_PACKAGE_NAME);

        if (!UiccProfile.isPackageBundled(context, pkgName)) {
            InstallCarrierAppUtils.showNotification(context, pkgName);
            InstallCarrierAppUtils.registerPackageInstallReceiver(context);
        }
    }
}
