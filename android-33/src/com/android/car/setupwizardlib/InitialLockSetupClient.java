/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.setupwizardlib;

import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.android.car.setupwizardlib.InitialLockSetupConstants.LockTypes;
import com.android.car.setupwizardlib.InitialLockSetupConstants.PasswordComplexity;
import com.android.car.setupwizardlib.InitialLockSetupConstants.SetLockCodes;
import com.android.car.setupwizardlib.InitialLockSetupConstants.ValidateLockFlags;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for communicating with the InitialLockSetupService in Car Settings.
 * This allows a device setup wizard to set the initial lock on a device that does not have one.
 */
public class InitialLockSetupClient implements ServiceConnection {

    private static final String INITIAL_LOCK_SERVICE_PACKAGE = "com.android.car.settings";
    private static final String INITIAL_LOCK_SERVICE_CLASS_NAME =
            INITIAL_LOCK_SERVICE_PACKAGE + ".setupservice.InitialLockSetupService";
    private static final Intent INITIAL_LOCK_SERVICE_INTENT = new Intent().setComponent(
            new ComponentName(INITIAL_LOCK_SERVICE_PACKAGE, INITIAL_LOCK_SERVICE_CLASS_NAME));

    private static final String TAG = "InitialLockSetupClient";
    // Arbitrary timeout before giving up on connecting to service.
    private static final long CONNECTION_TIMEOUT_MS = 5000;

    private final Runnable mTimeoutRunnable = this::connectionTimeout;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private InitialLockListener mInitialLockListener;
    private Context mContext;
    private KeyguardManager mKeyguardManager;
    private IInitialLockSetupService mInitialLockSetupService;
    private ValidateLockAsyncTask mCurrentValidateLockTask;
    private SaveLockAsyncTask mCurrentSaveLockTask;
    private boolean mCurrentlyBinding;
    // Tracks whether the connection has timed out to prevent duplicate callbacks to listener.
    private boolean mTimedOut;

    public InitialLockSetupClient(Context context) {
        mContext = context.getApplicationContext();
        mKeyguardManager = mContext.getSystemService(KeyguardManager.class);
    }

    /**
     * Starts the connection to the service.
     */
    public void startConnection(InitialLockListener listener) {
        logVerbose("startConnection");
        mInitialLockListener = listener;
        if (mInitialLockSetupService != null || mCurrentlyBinding) {
            logVerbose("Unable to bind to initial setup service, already connected or connecting.");
            return;
        }
        // Reset whether this has timed out.
        mTimedOut = false;
        try {
            mHandler.postDelayed(mTimeoutRunnable, CONNECTION_TIMEOUT_MS);
            mCurrentlyBinding = mContext.bindService(INITIAL_LOCK_SERVICE_INTENT, /* connection= */
                    this,
                    Context.BIND_AUTO_CREATE);

        } catch (Exception e) {
            e.printStackTrace();
            mInitialLockListener.onConnectionAttemptFinished(false);
        }
        if (!mCurrentlyBinding) {
            logVerbose("Unable to bind to initial setup service");
        }
    }

    /**
     * Stops the connection and removes the active listener. Does nothing if not connected.
     */
    public void stopConnection() {
        logVerbose("stopConnection");
        if (mInitialLockSetupService == null) {
            logVerbose("Attempting to disconnect from service when not connected");
            return;
        }
        mContext.unbindService(this);
        mInitialLockSetupService = null;
        mInitialLockListener = null;
        mCurrentlyBinding = false;
    }

    /**
     * Returns whether the client is currently connected to the service.
     */
    public boolean isServiceConnected() {
        return false;
    }

    /**
     * Fetches the set of {@link LockConfig}s that define the lock constraints for the device.
     */
    public void getLockConfigs() {
        LockConfigsAsyncTask lockConfigsAsyncTask = new LockConfigsAsyncTask(
                mInitialLockListener, mInitialLockSetupService, mKeyguardManager);
        lockConfigsAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
    }

    /**
     * Returns whether the current password fits the Car Settings password
     * criteria. Otherwise returns the related error code (length, character
     * types, etc).
     */
    public void checkValidPassword(byte[] password) {
        logVerbose("checkValidPassword");
        if (mCurrentValidateLockTask != null
                && mCurrentValidateLockTask.getStatus() != AsyncTask.Status.FINISHED) {
            mCurrentValidateLockTask.cancel(true);
        }
        mCurrentValidateLockTask = new ValidateLockAsyncTask(mInitialLockListener,
                mInitialLockSetupService, mKeyguardManager, LockTypes.PASSWORD);
        mCurrentValidateLockTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, password);
    }

    /**
     * Returns whether the current PIN fits the Car Settings PIN criteria.
     * Otherwise returns the related error code (length, character types, etc).
     */
    public void checkValidPin(byte[] pin) {
        logVerbose("checkValidPin");
        if (mCurrentValidateLockTask != null
                && mCurrentValidateLockTask.getStatus() != AsyncTask.Status.FINISHED) {
            mCurrentValidateLockTask.cancel(true);
        }
        mCurrentValidateLockTask = new ValidateLockAsyncTask(mInitialLockListener,
                mInitialLockSetupService, mKeyguardManager, LockTypes.PIN);
        mCurrentValidateLockTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pin);
    }

    /**
     * Returns whether the current pattern fits the Car Settings pattern
     * criteria. Otherwise returns the related error code.
     */
    public void checkValidPattern(byte[] pattern) {
        logVerbose("checkValidPattern");
        if (mCurrentValidateLockTask != null
                && mCurrentValidateLockTask.getStatus() != AsyncTask.Status.FINISHED) {
            mCurrentValidateLockTask.cancel(true);
        }
        mCurrentValidateLockTask = new ValidateLockAsyncTask(mInitialLockListener,
                mInitialLockSetupService, mKeyguardManager, LockTypes.PATTERN);
        mCurrentValidateLockTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pattern);
    }

    /**
     * Calls to the service to set the given password as the initial device
     * password.
     */
    public void saveLockPassword(byte[] password) {
        if (mCurrentSaveLockTask != null
                && mCurrentSaveLockTask.getStatus() != AsyncTask.Status.FINISHED) {
            // If a save operation is already started, ignore this. Should not try to save
            // multiple locks at once.
            Log.e(TAG, "Can't save multiple passwords at once");
            return;
        }
        mCurrentSaveLockTask = new SaveLockAsyncTask(mInitialLockListener,
                mInitialLockSetupService, mKeyguardManager, LockTypes.PASSWORD);
        mCurrentSaveLockTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, password);
    }

    /**
     * Calls to the service to set the given PIN as the initial device PIN.
     */
    public void saveLockPin(byte[] pin) {
        if (mCurrentSaveLockTask != null
                && mCurrentSaveLockTask.getStatus() != AsyncTask.Status.FINISHED) {
            // If a save operation is already started, ignore this. Should not try to save
            // multiple locks at once.
            Log.e(TAG, "Can't save multiple passwords at once");
            return;
        }
        mCurrentSaveLockTask = new SaveLockAsyncTask(mInitialLockListener,
                mInitialLockSetupService, mKeyguardManager, LockTypes.PIN);
        mCurrentSaveLockTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pin);
    }

    /**
     * Calls the service to set the given pattern as the initial device lock
     * pattern.
     */
    public void saveLockPattern(byte[] pattern) {
        if (mCurrentSaveLockTask != null
                && mCurrentSaveLockTask.getStatus() != AsyncTask.Status.FINISHED) {
            // If a save operation is already started, ignore this. Should not try to save
            // multiple locks at once.
            Log.e(TAG, "Can't save multiple passwords at once");
            return;
        }
        mCurrentSaveLockTask = new SaveLockAsyncTask(mInitialLockListener,
                mInitialLockSetupService, mKeyguardManager, LockTypes.PATTERN);
        mCurrentSaveLockTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pattern);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        logVerbose("onServiceConnected");
        if (mTimedOut) {
            return;
        }
        mHandler.removeCallbacks(mTimeoutRunnable);
        mCurrentlyBinding = false;
        mInitialLockSetupService = IInitialLockSetupService.Stub.asInterface(service);
        if (mInitialLockListener != null) {
            mInitialLockListener.onConnectionAttemptFinished(true);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        logVerbose("onServiceDisconnected");
        mCurrentlyBinding = false;
        mInitialLockSetupService = null;
    }

    @Override
    public void onNullBinding(ComponentName name) {
        logVerbose("onNullBinding");
        if (mTimedOut) {
            return;
        }
        mHandler.removeCallbacks(mTimeoutRunnable);
        mCurrentlyBinding = false;
        if (mInitialLockListener != null) {
            mInitialLockListener.onConnectionAttemptFinished(false);
        }
    }

    @Override
    public void onBindingDied(ComponentName name) {
        logVerbose("onBindingDied");
        if (mTimedOut) {
            return;
        }
        mHandler.removeCallbacks(mTimeoutRunnable);
        mCurrentlyBinding = false;
        if (mInitialLockListener != null) {
            mInitialLockListener.onConnectionAttemptFinished(false);
        }
    }

    private void connectionTimeout() {
        logVerbose("connectionTimeout");
        if (mInitialLockListener == null) {
            return;
        }
        mInitialLockListener.onConnectionAttemptFinished(false);
        mTimedOut = true;
    }

    private static class LockConfigsAsyncTask extends
            AsyncTask<Void, Void, Map<Integer, LockConfig>> {

        private WeakReference<InitialLockListener> mInitialLockListener;
        private WeakReference<IInitialLockSetupService> mInitialLockSetupService;
        private WeakReference<KeyguardManager> mKeyguardManager;

        LockConfigsAsyncTask(InitialLockListener initialLockListener,
                IInitialLockSetupService initialLockSetupService,
                KeyguardManager keyguardManager) {
            mInitialLockListener = new WeakReference<>(initialLockListener);
            mInitialLockSetupService = new WeakReference<>(initialLockSetupService);
            mKeyguardManager = new WeakReference<>(keyguardManager);
        }

        @Override
        protected Map<Integer, LockConfig> doInBackground(Void... voids) {
            LockConfig passwordConfig, pinConfig, patternConfig;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                KeyguardManager km = mKeyguardManager.get();
                if (km == null) {
                    InitialLockSetupClient.logVerbose(
                            "Lost reference to keyguardManager in LockConfigsAsyncTask");
                    return null;
                }
                passwordConfig =
                    new LockConfig(
                        /* enabled= */ true,
                        km.getMinLockLength(
                            /* isPin= */ false, PasswordComplexity.PASSWORD_COMPLEXITY_MEDIUM));
                pinConfig =
                    new LockConfig(
                        /* enabled= */ true,
                        km.getMinLockLength(
                            /* isPin= */ true, PasswordComplexity.PASSWORD_COMPLEXITY_MEDIUM));
                patternConfig =
                    new LockConfig(
                        /* enabled= */ true,
                        km.getMinLockLength(
                            /* isPin= */ false, PasswordComplexity.PASSWORD_COMPLEXITY_LOW));
            } else {
                IInitialLockSetupService initialLockSetupService = mInitialLockSetupService.get();
                if (initialLockSetupService == null) {
                    InitialLockSetupClient.logVerbose(
                            "Lost reference to service in LockConfigsAsyncTask");
                    return null;
                }
                try {
                    passwordConfig = initialLockSetupService.getLockConfig(LockTypes.PASSWORD);
                    pinConfig = initialLockSetupService.getLockConfig(LockTypes.PIN);
                    patternConfig = initialLockSetupService.getLockConfig(LockTypes.PATTERN);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            Map<Integer, LockConfig> map = new HashMap<>();
            map.put(LockTypes.PASSWORD, passwordConfig);
            map.put(LockTypes.PIN, pinConfig);
            map.put(LockTypes.PATTERN, patternConfig);
            return map;
        }

        @Override
        protected void onPostExecute(Map<Integer, LockConfig> map) {
            InitialLockListener listener = mInitialLockListener.get();
            if (listener == null) {
                return;
            }
            listener.onGetLockConfigs(map);
        }
    }

    private static class ValidateLockAsyncTask extends AsyncTask<byte[], Void, Integer> {

        private WeakReference<InitialLockListener> mInitialLockListener;
        private WeakReference<IInitialLockSetupService> mInitialLockSetupService;
        private WeakReference<KeyguardManager> mKeyguardManager;
        private int mLockType;

        ValidateLockAsyncTask(
                InitialLockListener initialLockListener,
                IInitialLockSetupService initialLockSetupService,
                KeyguardManager keyguardManager,
                @LockTypes int lockType) {
            mInitialLockListener = new WeakReference<>(initialLockListener);
            mInitialLockSetupService = new WeakReference<>(initialLockSetupService);
            mKeyguardManager = new WeakReference<>(keyguardManager);
            mLockType = lockType;
        }

        @Override
        protected Integer doInBackground(byte[]... passwords) {
            InitialLockSetupClient.logVerbose("ValidateLockAsyncTask doInBackground");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                KeyguardManager km = mKeyguardManager.get();
                if (km == null) {
                    InitialLockSetupClient.logVerbose(
                            "Lost reference to keyguardManager in LockConfigsAsyncTask");
                    return null;
                }
                int complexity;
                switch (mLockType) {
                    case LockTypes.PASSWORD:
                        complexity = PasswordComplexity.PASSWORD_COMPLEXITY_MEDIUM;
                        break;
                    case LockTypes.PIN:
                        complexity = PasswordComplexity.PASSWORD_COMPLEXITY_LOW;
                        break;
                    case LockTypes.PATTERN:
                        complexity = PasswordComplexity.PASSWORD_COMPLEXITY_LOW;
                        passwords[0] =
                                InitialLockSetupHelper.getNumericEquivalentByteArray(passwords[0]);
                        break;
                    default:
                        Log.e(TAG, "other lock type, returning generic error");
                        return ValidateLockFlags.INVALID_GENERIC;
                }
                return km.isValidLockPasswordComplexity(mLockType, passwords[0], complexity)
                    ? 0
                    : ValidateLockFlags.INVALID_GENERIC;
            } else {
                IInitialLockSetupService initialLockSetupService = mInitialLockSetupService.get();
                if (initialLockSetupService == null) {
                    InitialLockSetupClient.logVerbose(
                            "Lost reference to service in ValidateLockAsyncTask");
                    return ValidateLockFlags.INVALID_GENERIC;
                }
                try {
                    int output = initialLockSetupService.checkValidLock(mLockType, passwords[0]);
                    return output;
                } catch (RemoteException e) {
                    e.printStackTrace();
                    return ValidateLockFlags.INVALID_GENERIC;
                }
            }
        }

        @Override
        protected void onPostExecute(Integer resultCode) {
            if (isCancelled()) {
                return;
            }
            InitialLockListener listener = mInitialLockListener.get();
            if (listener == null) {
                return;
            }
            listener.onLockValidated(resultCode);
        }
    }

    private static class SaveLockAsyncTask extends AsyncTask<byte[], Void, Integer> {

        private WeakReference<InitialLockListener> mInitialLockListener;
        private WeakReference<IInitialLockSetupService> mInitialLockSetupService;
        private WeakReference<KeyguardManager> mKeyguardManager;
        private int mLockType;

        SaveLockAsyncTask(
                InitialLockListener initialLockListener,
                IInitialLockSetupService initialLockSetupService,
                KeyguardManager keyguardManager,
                @LockTypes int lockType) {
            mInitialLockListener = new WeakReference<>(initialLockListener);
            mInitialLockSetupService = new WeakReference<>(initialLockSetupService);
            mKeyguardManager = new WeakReference<>(keyguardManager);
            mLockType = lockType;
        }

        @Override
        protected Integer doInBackground(byte[]... passwords) {
            InitialLockSetupClient.logVerbose("SaveLockAsyncTask doInBackground");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                KeyguardManager km = mKeyguardManager.get();
                if (km == null) {
                    InitialLockSetupClient.logVerbose(
                            "Lost reference to keyguardManager in SaveLockAsyncTask");
                    return null;
                }
                int complexity;
                switch (mLockType) {
                    case LockTypes.PASSWORD:
                        complexity = PasswordComplexity.PASSWORD_COMPLEXITY_MEDIUM;
                        break;
                    case LockTypes.PIN:
                        complexity = PasswordComplexity.PASSWORD_COMPLEXITY_LOW;
                        break;
                    case LockTypes.PATTERN:
                        complexity = PasswordComplexity.PASSWORD_COMPLEXITY_LOW;
                        passwords[0] =
                            InitialLockSetupHelper.getNumericEquivalentByteArray(passwords[0]);
                        break;
                    default:
                        Log.e(TAG, "other lock type, returning generic error");
                        return SetLockCodes.FAIL_LOCK_GENERIC;
                }
                return km.setLock(mLockType, passwords[0], complexity)
                    ? 1
                    : SetLockCodes.FAIL_LOCK_GENERIC;
            } else {
                IInitialLockSetupService initialLockSetupService = mInitialLockSetupService.get();
                if (initialLockSetupService == null) {
                    InitialLockSetupClient.logVerbose(
                            "Lost reference to service in SaveLockAsyncTask");
                    return SetLockCodes.FAIL_LOCK_GENERIC;
                }
                try {
                    int output = initialLockSetupService.setLock(mLockType, passwords[0]);
                    return output;
                } catch (RemoteException e) {
                    e.printStackTrace();
                    return SetLockCodes.FAIL_LOCK_GENERIC;
                }
            }
        }

        @Override
        protected void onPostExecute(Integer resultCode) {
            InitialLockListener listener = mInitialLockListener.get();
            if (listener == null) {
                return;
            }
            listener.onSetLockFinished(resultCode);
        }
    }

    private static void logVerbose(String logStatement) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, logStatement);
        }
    }
}

