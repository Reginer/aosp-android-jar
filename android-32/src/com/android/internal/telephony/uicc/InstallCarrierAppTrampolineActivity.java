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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.concurrent.TimeUnit;

/**
 * Trampoline activity used to start the full screen dialog that is shown when a SIM is inserted
 * and requires a carrier app download
 */
public class InstallCarrierAppTrampolineActivity extends Activity {
    private static final String LOG_TAG = "CarrierAppInstall";
    private static final int INSTALL_CARRIER_APP_DIALOG_REQUEST = 1;

    // TODO(b/73648962): Move DOWNLOAD_RESULT and CARRIER_NAME to a shared location
    /**
     * This must remain in sync with
     * {@link com.android.simappdialog.InstallCarrierAppActivity#DOWNLOAD_RESULT}
     */
    private static final int DOWNLOAD_RESULT = 2;

    /**
     * This must remain in sync with
     * {@link com.android.simappdialog.InstallCarrierAppActivity#BUNDLE_KEY_CARRIER_NAME}
     */
    private static final String CARRIER_NAME = "carrier_name";

    /** Bundle key for the name of the package to be downloaded */
    private static final String BUNDLE_KEY_PACKAGE_NAME = "package_name";

    /** Returns intent used to start this activity */
    public static Intent get(Context context, String packageName) {
        Intent intent = new Intent(context, InstallCarrierAppTrampolineActivity.class);
        intent.putExtra(BUNDLE_KEY_PACKAGE_NAME, packageName);
        return intent;
    }

    private String mPackageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            mPackageName = intent.getStringExtra(BUNDLE_KEY_PACKAGE_NAME);
        }

        // If this is the first activity creation, show notification after delay regardless of
        // result code, but only if the app is not installed.
        if (savedInstanceState == null) {
            long sleepTimeMillis = Settings.Global.getLong(getContentResolver(),
                    Settings.Global.INSTALL_CARRIER_APP_NOTIFICATION_SLEEP_MILLIS,
                    TimeUnit.HOURS.toMillis(24));
            Log.d(LOG_TAG, "Sleeping carrier app install notification for : " + sleepTimeMillis
                    + " millis");
            InstallCarrierAppUtils.showNotificationIfNotInstalledDelayed(
                    this,
                    mPackageName,
                    sleepTimeMillis);
        }

        // Display dialog activity if available
        Intent showDialogIntent = new Intent();
        ComponentName dialogComponentName = ComponentName.unflattenFromString(
                Resources.getSystem().getString(
                        com.android.internal.R.string.config_carrierAppInstallDialogComponent));
        showDialogIntent.setComponent(dialogComponentName);
        String appName = InstallCarrierAppUtils.getAppNameFromPackageName(this, mPackageName);
        if (!TextUtils.isEmpty(appName)) {
            showDialogIntent.putExtra(CARRIER_NAME, appName);
        }

        if (showDialogIntent.resolveActivity(getPackageManager()) == null) {
            Log.d(LOG_TAG, "Could not resolve activity for installing the carrier app");
            finishNoAnimation();
        } else {
            startActivityForResult(showDialogIntent, INSTALL_CARRIER_APP_DIALOG_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INSTALL_CARRIER_APP_DIALOG_REQUEST) {
            if (resultCode == DOWNLOAD_RESULT) {
                startActivity(InstallCarrierAppUtils.getPlayStoreIntent(mPackageName));
            }
            finishNoAnimation();
        }
    }

    private void finishNoAnimation() {
        finish();
        overridePendingTransition(0, 0);
    }
}
