/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.server.wm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.view.Window;
import android.view.WindowManager;

import com.android.internal.R;
import com.android.server.utils.AppInstallerUtil;

class DeprecatedTargetSdkVersionDialog extends AppWarnings.BaseDialog {

    DeprecatedTargetSdkVersionDialog(final AppWarnings manager, Context context,
            ApplicationInfo appInfo, int userId) {
        super(manager, context, appInfo.packageName, userId);

        final PackageManager pm = context.getPackageManager();
        final CharSequence label = appInfo.loadSafeLabel(pm,
                PackageItemInfo.DEFAULT_MAX_LABEL_SIZE_PX,
                PackageItemInfo.SAFE_LABEL_FLAG_FIRST_LINE
                        | PackageItemInfo.SAFE_LABEL_FLAG_TRIM);
        final CharSequence message = context.getString(R.string.deprecated_target_sdk_message);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setPositiveButton(R.string.ok, (dialog, which) ->
                    manager.setPackageFlag(
                            mUserId, mPackageName, AppWarnings.FLAG_HIDE_DEPRECATED_SDK, true))
                .setMessage(message)
                .setTitle(label);

        // If we might be able to update the app, show a button.
        final Intent installerIntent = AppInstallerUtil.createIntent(context, appInfo.packageName);
        if (installerIntent != null) {
            builder.setNeutralButton(R.string.deprecated_target_sdk_app_store,
                    (dialog, which) -> {
                        context.startActivity(installerIntent);
                    });
        }

        // Ensure the content view is prepared.
        mDialog = builder.create();
        mDialog.create();

        final Window window = mDialog.getWindow();
        window.setType(WindowManager.LayoutParams.TYPE_PHONE);

        // DO NOT MODIFY. Used by CTS to verify the dialog is displayed.
        window.getAttributes().setTitle("DeprecatedTargetSdkVersionDialog");
    }
}
