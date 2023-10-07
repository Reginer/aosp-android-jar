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

package com.android.clockwork.display;

import static com.android.wearable.resources.R.bool.config_disableAODWhilePlugged;

import android.Manifest;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.display.DisplayManagerInternal;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import com.android.clockwork.common.WearResourceUtil;
import com.android.clockwork.display.burninprotection.BurnInProtector;
import com.android.clockwork.power.AmbientConfig;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;

import com.google.android.clockwork.display.IWearDisplayService;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

/** Wear-specific display mechanisms. */
public class WearDisplayService extends SystemService {
    private static final String TAG = "WearDisplayService";

    private static final String SET_DISPLAY_OFFSET = Manifest.permission.SET_DISPLAY_OFFSET;
    private static final int UNPLUGGED = 0;
    private static final String DUMPSYS_CMD_DISABLE_AOD_WHILE_PLUGGED = "disable_aod_while_plugged";

    private final Context mContext;
    private final WallpaperManager mWallpaperManager;
    private final PowerManager mPowerManager;
    private final BinderService mBinderService;
    private final AmbientConfig mAmbientConfig;
    private final BurnInProtector mBurnInProtector;

    private DisplayManagerInternal mDisplayManager;

    //  The name of a watchface that we have forced TTW ON for
    private String mForcedTtwOnWatchFace;
    private Boolean mSavedDecomposableWatchface;

    private boolean mInitialized = false;
    private boolean mDisableAodWhilePlugged;

    public WearDisplayService(Context context) {
        this(context,
                WallpaperManager.getInstance(context),
                context.getSystemService(PowerManager.class));
    }

    @VisibleForTesting WearDisplayService(
            Context context, WallpaperManager wallpaperManager, PowerManager powerManager) {
        super(context);
        mContext = context;
        mWallpaperManager = wallpaperManager;
        mPowerManager = powerManager;

        mBinderService = new BinderService();
        mAmbientConfig = new AmbientConfig(context.getContentResolver());
        mDisableAodWhilePlugged = shouldDisableAODWhilePluggedFromConfig();
        mBurnInProtector = BurnInProtector.create(context);
    }

    @Override
    public void onStart() {
        publishBinderService(TAG, mBinderService);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == SystemService.PHASE_SYSTEM_SERVICES_READY) {
            mDisplayManager = getLocalService(DisplayManagerInternal.class);
        } else if (phase == SystemService.PHASE_BOOT_COMPLETED) {
            TheaterMode theaterMode = new TheaterMode(mContext);

            // Monitor ambient-related settings.
            onAmbientConfigChanged();

            mAmbientConfig.addListener(this::onAmbientConfigChanged);
            mAmbientConfig.register();

            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
            intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
            intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            mContext.registerReceiver(
                    new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
                                onAmbientConfigChanged(/* plugged= */ true);
                            } else if (intent.getAction()
                                    .equals(Intent.ACTION_POWER_DISCONNECTED)) {
                                    onAmbientConfigChanged(/* plugged= */ false);
                            }
                        }
                    },
                    intentFilter);

            mInitialized = true;

            mBurnInProtector.init(mContext);
        }
    }

    @VisibleForTesting final class BinderService extends IWearDisplayService.Stub {
        @Override
        @RequiresPermission(Manifest.permission.SET_DISPLAY_OFFSET)
        public void setDisplayOffsets(int displayId, int x, int y) {
            if (mContext.checkCallingOrSelfPermission(SET_DISPLAY_OFFSET)
                    != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires SET_DISPLAY_OFFSET permission.");
            }
            if (mDisplayManager != null) {
                mDisplayManager.setDisplayOffsets(displayId, x, y);
            }
        }

        @Override
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(mContext, TAG, pw)) return;
            final long ident = Binder.clearCallingIdentity();
            try {
                WearDisplayService.this.dump(fd, pw, args);
            } catch (Throwable throwable) {
                pw.println("caught exception while dumping " + throwable.getMessage());
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    @VisibleForTesting void onAmbientConfigChanged() {
        onAmbientConfigChanged(isPlugged());
    }

    private void onAmbientConfigChanged(boolean plugged) {
        boolean ambientEnabled = mAmbientConfig.isAmbientEnabled();
        if (plugged && mDisableAodWhilePlugged) {
            ambientEnabled = false;
        }
        boolean ambientTiltToWake = mAmbientConfig.isTiltToWake();
        boolean ambientTiltToBright = mAmbientConfig.isUserTiltToBright();
        boolean decomposableWatchface = mAmbientConfig.isWatchfaceDecomposable();

        updateDoze(ambientEnabled, ambientTiltToBright, ambientTiltToWake, decomposableWatchface);

        // Run this only if decomposable watch face has changed.
        if (mSavedDecomposableWatchface == null
                || decomposableWatchface != mSavedDecomposableWatchface) {
            checkWatchFaceChange(ambientTiltToBright, ambientTiltToWake, decomposableWatchface);
        }
        mSavedDecomposableWatchface = decomposableWatchface;
    }

    private void updateDoze(
            boolean ambientEnabled,
            boolean ambientTiltToBright,
            boolean ambientTiltToWake,
            boolean decomposableWatchface) {
        if (Build.IS_DEBUGGABLE) {
            Log.d(
                    TAG,
                    "Updating Ambient setting, TTB: "
                            + ambientTiltToBright
                            + ", TTW: "
                            + ambientTiltToWake
                            + ", decomposable: "
                            + decomposableWatchface
                            + ", AOD: "
                            + ambientEnabled);
        }
        // We need to keep doze enabled when Power Saver Tilt / TTB is turned on
        boolean shouldDozeBeEnabled =
                ambientEnabled || (ambientTiltToBright && decomposableWatchface);
        if (isDozeEnabled() != shouldDozeBeEnabled) {
            long token = Binder.clearCallingIdentity();
            try {
                Settings.Secure.putInt(
                        mContext.getContentResolver(),
                        Settings.Secure.DOZE_ENABLED,
                        shouldDozeBeEnabled ? 1 : 0);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
            if (mInitialized) {
                Log.d(TAG, "Waking up on doze change");
                mPowerManager.wakeUp(
                        SystemClock.uptimeMillis(),
                        PowerManager.WAKE_REASON_APPLICATION,
                        "onDozeChanged");
            }
        }
    }

    private void checkWatchFaceChange(
            boolean ambientTiltToBright, boolean ambientTiltToWake, boolean decomposableWatchface) {
        String currentWatchFace = getCurrentWatchFaceComponentString();
        // During normal circumstances, currentWatchFace will never be null, check anyway
        if (currentWatchFace != null) {
            // If we switch to non-decomposable watch face, TTB was ON, and TTW was OFF,
            // turn TTW ON
            if (!decomposableWatchface && ambientTiltToBright && !ambientTiltToWake) {
                Log.d(TAG, "Forcing TTW ON for non-decomposable watchface " + currentWatchFace);
                mAmbientConfig.setTiltToWake(true);
                mForcedTtwOnWatchFace = currentWatchFace;

                // If we turned TTW ON with above logic for this watch face, but we now find
                // that it is decomposable, turn TTW back OFF
            } else if (decomposableWatchface && currentWatchFace.equals(mForcedTtwOnWatchFace)) {
                Log.d(TAG, "Restoring TTW OFF for decomposable watch face" + currentWatchFace);
                mAmbientConfig.setTiltToWake(false);
                mForcedTtwOnWatchFace = "";

                // If neither of above is applicable, reset the forced watch face value
            } else {
                mForcedTtwOnWatchFace = "";
            }
        } else {
            Log.w(TAG, "Watch face info is null!");
        }
    }

    @Nullable
    private String getCurrentWatchFaceComponentString() {
        WallpaperInfo wallpaperInfo = mWallpaperManager.getWallpaperInfo();
        return wallpaperInfo == null ? null : wallpaperInfo.getComponent().flattenToString();
    }

    private boolean isDozeEnabled() {
        return Settings.Secure.getInt(
                        mContext.getContentResolver(), Settings.Secure.DOZE_ENABLED, /* def= */ 1)
                == 1;
    }

    /**
     * Determines if WearDisplayService should disable AOD while the device is plugged in.
     *
     * @return false, unless specified otherwise with the config_disableAODWhilePlugged resource.
     */
    private boolean shouldDisableAODWhilePluggedFromConfig() {
        Resources wearableResources = WearResourceUtil.getWearableResources(mContext);
        if (wearableResources != null) {
            return wearableResources.getBoolean(config_disableAODWhilePlugged);
        }
        return false;
    }

    /**
     * Determines if the device is plugged in. Note that this method does not check if the device
     * is charging, because when battery defender is active the device may be plugged in but not
     * actively charging.
     *
     * @return true if the device is plugged in, false if otherwise.
     */
    private boolean isPlugged() {
        Intent intentBatteryChanged =
                mContext.registerReceiver(null,
                        new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        boolean returnValue = false;
        if (intentBatteryChanged != null) {
            returnValue =
                    intentBatteryChanged.getIntExtra(BatteryManager.EXTRA_PLUGGED, UNPLUGGED)
                            != UNPLUGGED;
        }
        if (Build.IS_DEBUGGABLE) {
            Log.d(TAG, "isPlugged: " + returnValue);
        }
        return returnValue;
    }


    @VisibleForTesting BinderService getBinderServiceInstance() {
        return mBinderService;
    }

    @VisibleForTesting
    void setDisableAodWhilePlugged(boolean disableAodWhilePlugged) {
        mDisableAodWhilePlugged = disableAodWhilePlugged;
    }

    private void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        final IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        if (maybeExecuteDumpsysCommand(args, ipw)) {
            return;
        }
        ipw.println("WearDisplayService");
        ipw.increaseIndent();
        ipw.println("mInitialized: " + mInitialized);
        ipw.println("isPlugged: " + isPlugged());
        ipw.println("mDisableAodWhilePlugged: " + mDisableAodWhilePlugged);
        ipw.println("ambientEnabled: " + mAmbientConfig.isAmbientEnabled());
        ipw.println("TTWEnabled: " + mAmbientConfig.isTiltToWake());
        ipw.println("TTBEnabled: " + mAmbientConfig.isUserTiltToBright());
        ipw.println("watchFaceDecomposable: " + mAmbientConfig.isWatchfaceDecomposable());
        ipw.println("mSavedDecomposableWatchface: " + mSavedDecomposableWatchface);
        ipw.println("mForcedTtwOnWatchFace: " + mForcedTtwOnWatchFace);
        ipw.println("dozeEnabled: " + isDozeEnabled());
        ipw.decreaseIndent();
    }

    private boolean maybeExecuteDumpsysCommand(String[] args, IndentingPrintWriter ipw) {
        if (args.length == 0) {
            return false;
        }
        try {
            if (DUMPSYS_CMD_DISABLE_AOD_WHILE_PLUGGED.equals(args[0])) {
                boolean val = Boolean.valueOf(args[1]);
                if (mDisableAodWhilePlugged != val) {
                    mDisableAodWhilePlugged = val;
                    onAmbientConfigChanged();
                }
                ipw.println("Disable aod while plugged is set to " + mDisableAodWhilePlugged);
                return true;
            } else {
                ipw.println("Unrecognized command \"" + args[0] + "\"");
            }
        } catch (Throwable t) {
            String msg = "Failed to execute dumpsys command " + Arrays.toString(args);
            Log.e(TAG, msg);
            ipw.println(msg);
        }
        return false;
    }
}
