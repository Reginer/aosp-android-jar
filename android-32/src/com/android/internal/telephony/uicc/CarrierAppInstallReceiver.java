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
import android.text.TextUtils;
import android.util.Log;

/**
 * Hides the carrier app install notification if the correct packages are installed
 */
public class CarrierAppInstallReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "CarrierAppInstall";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
            Log.d(LOG_TAG, "Received package install intent");
            String intentPackageName = intent.getData().getSchemeSpecificPart();
            if (TextUtils.isEmpty(intentPackageName)) {
                Log.w(LOG_TAG, "Package is empty, ignoring");
                return;
            }

            InstallCarrierAppUtils.hideNotification(context, intentPackageName);

            if (!InstallCarrierAppUtils.isPackageInstallNotificationActive(context)) {
                InstallCarrierAppUtils.unregisterPackageInstallReceiver(context);
            }
        }
    }
}
