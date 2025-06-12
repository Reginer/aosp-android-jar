/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.internal.app;

import static android.app.admin.DevicePolicyResources.Strings.Core.UNLAUNCHABLE_APP_WORK_PAUSED_TITLE;
import static android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.Window;

import com.android.internal.R;

/**
 * A dialog shown to the user when they try to launch an app from a quiet profile
 * ({@link UserManager#isQuietModeEnabled(UserHandle)}.
 */
public class UnlaunchableAppActivity extends Activity
        implements DialogInterface.OnDismissListener, DialogInterface.OnClickListener {
    private static final String TAG = "UnlaunchableAppActivity";

    private static final int UNLAUNCHABLE_REASON_QUIET_MODE = 1;
    private static final String EXTRA_UNLAUNCHABLE_REASON = "unlaunchable_reason";

    private int mUserId;
    private int mReason;
    private IntentSender mTarget;
    private TelecomManager mTelecomManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // As this activity has nothing to show, we should hide the title bar also
        // TODO: Use AlertActivity so we don't need to hide title bar and create a dialog
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Intent intent = getIntent();
        mTelecomManager = getSystemService(TelecomManager.class);
        mReason = intent.getIntExtra(EXTRA_UNLAUNCHABLE_REASON, -1);
        mUserId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, UserHandle.USER_NULL);
        mTarget = intent.getParcelableExtra(Intent.EXTRA_INTENT,
                android.content.IntentSender.class);
        String targetPackageName = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME);
        Log.i(TAG, "Unlaunchable activity for target package: " + targetPackageName);
        final UserManager userManager = UserManager.get(this);

        if (mUserId == UserHandle.USER_NULL) {
            Log.wtf(TAG, "Invalid user id: " + mUserId + ". Stopping.");
            finish();
            return;
        }

        if (android.os.Flags.allowPrivateProfile()
                && android.multiuser.Flags.enablePrivateSpaceFeatures()
                && !userManager.isManagedProfile(mUserId)) {
            Log.e(TAG, "Unlaunchable activity for target package " + targetPackageName
                    + " called for a non-managed-profile " + mUserId);
            finish();
            return;
        }

        if (mReason != UNLAUNCHABLE_REASON_QUIET_MODE) {
            Log.wtf(TAG, "Invalid unlaunchable type: " + mReason);
            finish();
            return;
        }

        boolean showEmergencyCallButton =
                (targetPackageName != null && targetPackageName.equals(
                        mTelecomManager.getDefaultDialerPackage(UserHandle.of(mUserId))));

        final AlertDialog.Builder builder;
        if (showEmergencyCallButton) {
            builder = new AlertDialog.Builder(this, R.style.AlertDialogWithEmergencyButton);
            builder.setNeutralButton(R.string.work_mode_emergency_call_button, this);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(getDialogTitle())
                .setOnDismissListener(this)
                .setPositiveButton(R.string.work_mode_turn_on, this)
                .setNegativeButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();
        dialog.create();
        if (showEmergencyCallButton) {
            dialog.getWindow().findViewById(R.id.parentPanel).setPadding(0, 0, 0, 30);
            dialog.getWindow().findViewById(R.id.button3).setOutlineProvider(null);
        }

        // Prevents screen overlay attack.
        getWindow().setHideOverlayWindows(true);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setFilterTouchesWhenObscured(true);
        dialog.show();
    }

    private String getDialogTitle() {
        return getSystemService(DevicePolicyManager.class).getResources().getString(
                UNLAUNCHABLE_APP_WORK_PAUSED_TITLE, () -> getString(R.string.work_mode_off_title));
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mReason != UNLAUNCHABLE_REASON_QUIET_MODE) {
            return;
        }
        if (which == DialogInterface.BUTTON_POSITIVE) {
            UserManager userManager = UserManager.get(this);
            new Handler(Looper.getMainLooper()).post(
                    () -> userManager.requestQuietModeEnabled(
                            /* enableQuietMode= */ false, UserHandle.of(mUserId), mTarget));
        } else if (which == DialogInterface.BUTTON_NEUTRAL) {
            launchEmergencyDialer();
        }
    }

    private void launchEmergencyDialer() {
        startActivity(mTelecomManager.createLaunchEmergencyDialerIntent(
                        null /* number*/)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    private static final Intent createBaseIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("android", UnlaunchableAppActivity.class.getName()));
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return intent;
    }

    public static Intent createInQuietModeDialogIntent(int userId) {
        Intent intent = createBaseIntent();
        intent.putExtra(EXTRA_UNLAUNCHABLE_REASON, UNLAUNCHABLE_REASON_QUIET_MODE);
        intent.putExtra(Intent.EXTRA_USER_HANDLE, userId);
        return intent;
    }

    public static Intent createInQuietModeDialogIntent(int userId, IntentSender target,
            ResolveInfo resolveInfo) {
        Intent intent = createInQuietModeDialogIntent(userId);
        intent.putExtra(Intent.EXTRA_INTENT, target);
        if (resolveInfo != null) {
            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, resolveInfo.getComponentInfo().packageName);
        }
        return intent;
    }
}
