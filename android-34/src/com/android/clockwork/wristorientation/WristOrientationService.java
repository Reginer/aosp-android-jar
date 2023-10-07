/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.clockwork.wristorientation;

import static com.android.clockwork.common.WristOrientationConstants.LEFT_WRIST_ROTATION_0;
import static com.android.clockwork.common.WristOrientationConstants.LEFT_WRIST_ROTATION_180;
import static com.android.clockwork.common.WristOrientationConstants.RIGHT_WRIST_ROTATION_180;

import android.content.Context;
import android.database.ContentObserver;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.os.Handler;
import android.os.HwRemoteBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import android.view.Surface;
import android.view.WindowManagerGlobal;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.SystemService;

import vendor.google_clockwork.wristorientation.V1_0.IWristOrientation;

/*
 * Wear-specific service for enabling multiple wrist orientation modes.
 */

public class WristOrientationService extends SystemService {
    static final String TAG = "WristOrientationService";

    @VisibleForTesting
    static final String KEY_PROP_WRIST_ORIENTATION = "persist.sys.wrist_orientation";

    private enum Status {
        SUCCESS,
        ERROR_SET_HAL,
        ERROR_SET_DISPLAY_ROTATION,
        ERROR_NO_SYSTEM_SERVICE,
    };

    private int mHalRemoteExceptionCount = 0;
    private Context mContext;
    private final Object mLock = new Object();

    @VisibleForTesting
    IWristOrientation mWristOrientationHal;
    @VisibleForTesting
    IWindowManager mWindowManager;
    @VisibleForTesting
    int mOrientation = LEFT_WRIST_ROTATION_0;

    private Status mStatus;

    public WristOrientationService(Context ctx) {
        super(ctx);

        mContext = ctx;
        mWindowManager = WindowManagerGlobal.getWindowManagerService();

        mOrientation = Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.Wearable.WRIST_ORIENTATION_MODE,
                LEFT_WRIST_ROTATION_0);
        //  We sync the content provider value to the system property to account for the following:
        //  1. The service crashed before we could write to the sys prop in updateOrientation
        //  2. The setting was changed on the phone while the watch was disconnected
        SystemProperties.set(KEY_PROP_WRIST_ORIENTATION, Integer.toString(mOrientation));

    }

    @Override
    public void onStart() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.i(TAG, "onStart called");
        }

        try {
            IServiceManager serviceManager = IServiceManager.getService();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.i(TAG, "got service manager: " + serviceManager);
            }

            // Register callback for death of service manager.
            if (!serviceManager.linkToDeath(mServiceManagerDeathCb, 0)) {
                Log.e(TAG, "linkToDeath on serviceManager failed");
                return;
            }

            // Register a callback to be invoked when the WristOrientation
            // HAL has finished loading.
            if (!serviceManager.registerForNotifications(
                    IWristOrientation.kInterfaceName, "", mServiceManagerCb)) {
                Log.e(TAG, "serviceManager callback registration failed");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Exception while registering callbacks: " + e);
            mHalRemoteExceptionCount++;
        }

        updateOrientation(mOrientation);

        mContext.getContentResolver()
                .registerContentObserver(Settings.Global
                                .getUriFor(Settings.Global.Wearable.WRIST_ORIENTATION_MODE),
                        false, mContentObserver);
    }

    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateOrientation(Settings.Global.getInt(
                    mContext.getContentResolver(),
                    Settings.Global.Wearable.WRIST_ORIENTATION_MODE,
                    LEFT_WRIST_ROTATION_0));
        }
    };

    @VisibleForTesting
    int getOrientation() {
        return mOrientation;
    }

    @VisibleForTesting
    void updateOrientation(int orientation) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, " updateOrientation called with orientation " + orientation);
        }

        // Notify hal clients of the change in orientation
        if (mWristOrientationHal != null) {
            try {
                mWristOrientationHal.setOrientation((byte) orientation);
            } catch (RemoteException e) {
                Log.e(TAG, "Exception while setting orientation: " + e);
                mHalRemoteExceptionCount++;
                mStatus = Status.ERROR_SET_HAL;
            }
        }

        // Rotate the display at the frameworks level
        boolean isRotated180 = orientation == LEFT_WRIST_ROTATION_180
                || orientation == RIGHT_WRIST_ROTATION_180;
        try {
            mWindowManager.freezeRotation(isRotated180 ? Surface.ROTATION_180 : Surface.ROTATION_0);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception while setting surface rotation : " + e);
            mStatus = Status.ERROR_SET_DISPLAY_ROTATION;
        }


        // Commit the change in orientation to the sys prop
        SystemProperties.set(KEY_PROP_WRIST_ORIENTATION, Integer.toString(orientation));

        mOrientation = orientation;

        mStatus =  Status.SUCCESS;
    }

    private final HwRemoteBinder.DeathRecipient mServiceManagerDeathCb =
            cookie -> {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.i(TAG, "ServiceManager died: cookie=" + cookie);
                }
            };


    IServiceNotification mServiceManagerCb = new IServiceNotification.Stub() {
        public void onRegistration(String fqName, String name, boolean preexisting) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.i(TAG, "got mServiceManagerCb notification for: "
                        + fqName + ", " + name + " preexisting=" + preexisting);
            }
            synchronized (mLock) {
                try {
                    IWristOrientation hal = IWristOrientation.getService();
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.i(TAG, "Got WristOrientation: " + hal);
                    }
                    mWristOrientationHal = hal;
                    if (mWristOrientationHal != null) {
                        mWristOrientationHal.setOrientation((byte) mOrientation);
                     }
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception talking to the HAL: ", e);
                    mHalRemoteExceptionCount++;
                }
            }
        }
    };

    @Override
    public void onBootPhase(int phase) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, " onBootPhase: called");
        }
    }
}
