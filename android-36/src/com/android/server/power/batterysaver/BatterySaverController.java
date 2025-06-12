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
package com.android.server.power.batterysaver;

import android.Manifest;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManagerInternal;
import android.hardware.power.Mode;
import android.os.BatteryManager;
import android.os.BatterySaverPolicyConfig;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.PowerManagerInternal.LowPowerModeListener;
import android.os.PowerSaveState;
import android.os.UserHandle;
import android.util.Slog;

import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.power.batterysaver.BatterySaverPolicy.BatterySaverPolicyListener;
import com.android.server.power.batterysaver.BatterySaverPolicy.Policy;
import com.android.server.power.batterysaver.BatterySaverPolicy.PolicyLevel;
import com.android.server.power.batterysaver.BatterySavingStats.BatterySaverState;
import com.android.server.power.batterysaver.BatterySavingStats.DozeState;
import com.android.server.power.batterysaver.BatterySavingStats.InteractiveState;
import com.android.server.power.batterysaver.BatterySavingStats.PlugState;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * Responsible for battery saver mode transition logic.
 *
 * IMPORTANT: This class shares the power manager lock, which is very low in the lock hierarchy.
 * Do not call out with the lock held. (Settings provider is okay.)
 */
public class BatterySaverController implements BatterySaverPolicyListener {
    static final String TAG = "BatterySaverController";

    static final boolean DEBUG = BatterySaverPolicy.DEBUG;

    private final Object mLock;
    private final Context mContext;
    private final MyHandler mHandler;

    private PowerManager mPowerManager;

    private final BatterySaverPolicy mBatterySaverPolicy;

    private final BatterySavingStats mBatterySavingStats;

    @GuardedBy("mLock")
    private final ArrayList<LowPowerModeListener> mListeners = new ArrayList<>();

    /**
     * Do not access directly; always use {@link #setFullEnabledLocked}
     * and {@link #getFullEnabledLocked}
     */
    @GuardedBy("mLock")
    private boolean mFullEnabledRaw;

    /**
     * Do not access directly; always use {@link #setAdaptiveEnabledLocked} and
     * {@link #getAdaptiveEnabledLocked}.
     */
    @GuardedBy("mLock")
    private boolean mAdaptiveEnabledRaw;

    @GuardedBy("mLock")
    private boolean mIsPluggedIn;

    /**
     * Whether full was previously enabled or not; only for the event logging. Only use it from
     * {@link #handleBatterySaverStateChanged}.
     */
    private boolean mFullPreviouslyEnabled;

    /**
     * Whether adaptive was previously enabled or not; only for the event logging. Only use it from
     * {@link #handleBatterySaverStateChanged}.
     */
    private boolean mAdaptivePreviouslyEnabled;

    @GuardedBy("mLock")
    private boolean mIsInteractive;

    /**
     * Package name that will receive an explicit manifest broadcast for
     * {@link PowerManager#ACTION_POWER_SAVE_MODE_CHANGED}. It's {@code null} if it hasn't been
     * retrieved yet.
     */
    @Nullable
    private Optional<String> mPowerSaveModeChangedListenerPackage;

    public static final int REASON_PERCENTAGE_AUTOMATIC_ON = 0;
    public static final int REASON_PERCENTAGE_AUTOMATIC_OFF = 1;
    public static final int REASON_MANUAL_ON = 2;
    public static final int REASON_MANUAL_OFF = 3;
    public static final int REASON_STICKY_RESTORE = 4;
    public static final int REASON_INTERACTIVE_CHANGED = 5;
    public static final int REASON_POLICY_CHANGED = 6;
    public static final int REASON_PLUGGED_IN = 7;
    public static final int REASON_SETTING_CHANGED = 8;
    public static final int REASON_DYNAMIC_POWER_SAVINGS_AUTOMATIC_ON = 9;
    public static final int REASON_DYNAMIC_POWER_SAVINGS_AUTOMATIC_OFF = 10;
    public static final int REASON_ADAPTIVE_DYNAMIC_POWER_SAVINGS_CHANGED = 11;
    public static final int REASON_TIMEOUT = 12;
    public static final int REASON_FULL_POWER_SAVINGS_CHANGED = 13;

    static String reasonToString(int reason) {
        switch (reason) {
            case BatterySaverController.REASON_PERCENTAGE_AUTOMATIC_ON:
                return "Percentage Auto ON";
            case BatterySaverController.REASON_PERCENTAGE_AUTOMATIC_OFF:
                return "Percentage Auto OFF";
            case BatterySaverController.REASON_MANUAL_ON:
                return "Manual ON";
            case BatterySaverController.REASON_MANUAL_OFF:
                return "Manual OFF";
            case BatterySaverController.REASON_STICKY_RESTORE:
                return "Sticky restore";
            case BatterySaverController.REASON_INTERACTIVE_CHANGED:
                return "Interactivity changed";
            case BatterySaverController.REASON_POLICY_CHANGED:
                return "Policy changed";
            case BatterySaverController.REASON_PLUGGED_IN:
                return "Plugged in";
            case BatterySaverController.REASON_SETTING_CHANGED:
                return "Setting changed";
            case BatterySaverController.REASON_DYNAMIC_POWER_SAVINGS_AUTOMATIC_ON:
                return "Dynamic Warning Auto ON";
            case BatterySaverController.REASON_DYNAMIC_POWER_SAVINGS_AUTOMATIC_OFF:
                return "Dynamic Warning Auto OFF";
            case BatterySaverController.REASON_ADAPTIVE_DYNAMIC_POWER_SAVINGS_CHANGED:
                return "Adaptive Power Savings changed";
            case BatterySaverController.REASON_TIMEOUT:
                return "timeout";
            case BatterySaverController.REASON_FULL_POWER_SAVINGS_CHANGED:
                return "Full Power Savings changed";
            default:
                return "Unknown reason: " + reason;
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) {
                Slog.d(TAG, "onReceive: " + intent);
            }
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                case Intent.ACTION_SCREEN_OFF:
                    if (!isPolicyEnabled()) {
                        updateBatterySavingStats();
                        return; // No need to send it if not enabled.
                    }
                    // We currently evaluate state only for CPU frequency changes.
                    // Don't send the broadcast, because we never did so in this case.
                    mHandler.postStateChanged(/*sendBroadcast=*/ false,
                            REASON_INTERACTIVE_CHANGED);
                    break;
                case Intent.ACTION_BATTERY_CHANGED:
                    synchronized (mLock) {
                        mIsPluggedIn = (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0);
                    }
                    // Fall-through.
                case PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED:
                case PowerManager.ACTION_LIGHT_DEVICE_IDLE_MODE_CHANGED:
                    updateBatterySavingStats();
                    break;
            }
        }
    };

    /**
     * Constructor.
     */
    public BatterySaverController(Object lock, Context context, Looper looper,
            BatterySaverPolicy policy, BatterySavingStats batterySavingStats) {
        mLock = lock;
        mContext = context;
        mHandler = new MyHandler(looper);
        mBatterySaverPolicy = policy;
        mBatterySaverPolicy.addListener(this);
        mBatterySavingStats = batterySavingStats;

        PowerManager.invalidatePowerSaveModeCaches();
    }

    /**
     * Add a listener.
     */
    public void addListener(LowPowerModeListener listener) {
        synchronized (mLock) {
            mListeners.add(listener);
        }
    }

    /**
     * Called by {@link BatterySaverStateMachine} on system ready, *with no lock held*.
     */
    public void systemReady() {
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        filter.addAction(PowerManager.ACTION_LIGHT_DEVICE_IDLE_MODE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);

        mHandler.postSystemReady();
    }

    private PowerManager getPowerManager() {
        if (mPowerManager == null) {
            mPowerManager =
                    Objects.requireNonNull(mContext.getSystemService(PowerManager.class));
        }
        return mPowerManager;
    }

    @Override
    public void onBatterySaverPolicyChanged(BatterySaverPolicy policy) {
        if (!isPolicyEnabled()) {
            return; // No need to send it if not enabled.
        }
        mHandler.postStateChanged(/*sendBroadcast=*/ true, REASON_POLICY_CHANGED);
    }

    private class MyHandler extends Handler {
        private static final int MSG_STATE_CHANGED = 1;

        private static final int ARG_DONT_SEND_BROADCAST = 0;
        private static final int ARG_SEND_BROADCAST = 1;

        private static final int MSG_SYSTEM_READY = 2;

        public MyHandler(Looper looper) {
            super(looper);
        }

        void postStateChanged(boolean sendBroadcast, int reason) {
            obtainMessage(MSG_STATE_CHANGED, sendBroadcast ?
                    ARG_SEND_BROADCAST : ARG_DONT_SEND_BROADCAST, reason).sendToTarget();
        }

        public void postSystemReady() {
            obtainMessage(MSG_SYSTEM_READY, 0, 0).sendToTarget();
        }

        @Override
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case MSG_STATE_CHANGED:
                    handleBatterySaverStateChanged(
                            msg.arg1 == ARG_SEND_BROADCAST,
                            msg.arg2);
                    break;
            }
        }
    }

    /** Enable or disable full battery saver. */
    @VisibleForTesting
    public void enableBatterySaver(boolean enable, int reason) {
        synchronized (mLock) {
            if (getFullEnabledLocked() == enable) {
                return;
            }
            setFullEnabledLocked(enable);

            if (updatePolicyLevelLocked()) {
                mHandler.postStateChanged(/*sendBroadcast=*/ true, reason);
            }
        }
    }

    private boolean updatePolicyLevelLocked() {
        if (getFullEnabledLocked()) {
            return mBatterySaverPolicy.setPolicyLevel(BatterySaverPolicy.POLICY_LEVEL_FULL);
        } else if (getAdaptiveEnabledLocked()) {
            return mBatterySaverPolicy.setPolicyLevel(BatterySaverPolicy.POLICY_LEVEL_ADAPTIVE);
        } else {
            return mBatterySaverPolicy.setPolicyLevel(BatterySaverPolicy.POLICY_LEVEL_OFF);
        }
    }

    BatterySaverPolicyConfig getPolicyLocked(@PolicyLevel int policyLevel) {
        return mBatterySaverPolicy.getPolicyLocked(policyLevel).toConfig();
    }

    /**
     * @return whether battery saver is enabled or not. This takes into
     * account whether a policy says to advertise isEnabled so this can be propagated externally.
     */
    public boolean isEnabled() {
        synchronized (mLock) {
            return getFullEnabledLocked() || (getAdaptiveEnabledLocked()
                    && mBatterySaverPolicy.shouldAdvertiseIsEnabled());
        }
    }

    /**
     * @return whether battery saver policy is enabled or not. This does not take into account
     * whether a policy says to advertise isEnabled, so this shouldn't be propagated externally.
     */
    private boolean isPolicyEnabled() {
        synchronized (mLock) {
            return getFullEnabledLocked() || getAdaptiveEnabledLocked();
        }
    }

    boolean isFullEnabled() {
        synchronized (mLock) {
            return getFullEnabledLocked();
        }
    }

    boolean setFullPolicyLocked(BatterySaverPolicyConfig config, int reason) {
        return setFullPolicyLocked(BatterySaverPolicy.Policy.fromConfig(config), reason);
    }

    boolean setFullPolicyLocked(Policy policy, int reason) {
        if (mBatterySaverPolicy.setFullPolicyLocked(policy)) {
            mHandler.postStateChanged(/*sendBroadcast=*/ true, reason);
            return true;
        }
        return false;
    }

    boolean isAdaptiveEnabled() {
        synchronized (mLock) {
            return getAdaptiveEnabledLocked();
        }
    }

    boolean setAdaptivePolicyLocked(BatterySaverPolicyConfig config, int reason) {
        return setAdaptivePolicyLocked(BatterySaverPolicy.Policy.fromConfig(config), reason);
    }

    boolean setAdaptivePolicyLocked(Policy policy, int reason) {
        if (mBatterySaverPolicy.setAdaptivePolicyLocked(policy)) {
            mHandler.postStateChanged(/*sendBroadcast=*/ true, reason);
            return true;
        }
        return false;
    }

    boolean resetAdaptivePolicyLocked(int reason) {
        if (mBatterySaverPolicy.resetAdaptivePolicyLocked()) {
            mHandler.postStateChanged(/*sendBroadcast=*/ true, reason);
            return true;
        }
        return false;
    }

    boolean setAdaptivePolicyEnabledLocked(boolean enabled, int reason) {
        if (getAdaptiveEnabledLocked() == enabled) {
            return false;
        }
        setAdaptiveEnabledLocked(enabled);
        if (updatePolicyLevelLocked()) {
            mHandler.postStateChanged(/*sendBroadcast=*/ true, reason);
            return true;
        }
        return false;
    }

    /** @return whether device is in interactive state. */
    public boolean isInteractive() {
        synchronized (mLock) {
            return mIsInteractive;
        }
    }

    /** @return Battery saver policy. */
    public BatterySaverPolicy getBatterySaverPolicy() {
        return mBatterySaverPolicy;
    }

    /**
     * @return true if launch boost should currently be disabled.
     */
    public boolean isLaunchBoostDisabled() {
        return isPolicyEnabled() && mBatterySaverPolicy.isLaunchBoostDisabled();
    }

    /**
     * Dispatch power save events to the listeners.
     *
     * This method is always called on the handler thread.
     *
     * This method is called only in the following cases:
     * - When battery saver becomes activated.
     * - When battery saver becomes deactivated.
     * - When battery saver is on and the interactive state changes.
     * - When battery saver is on and the battery saver policy changes.
     * - When adaptive battery saver becomes activated.
     * - When adaptive battery saver becomes deactivated.
     * - When adaptive battery saver is active (and full is off) and the policy changes.
     */
    void handleBatterySaverStateChanged(boolean sendBroadcast, int reason) {
        final LowPowerModeListener[] listeners;

        final boolean enabled;
        final boolean isInteractive = getPowerManager().isInteractive();

        synchronized (mLock) {
            enabled = getFullEnabledLocked() || getAdaptiveEnabledLocked();

            EventLogTags.writeBatterySaverMode(
                    mFullPreviouslyEnabled ? 1 : 0, // Previously off or on.
                    mAdaptivePreviouslyEnabled ? 1 : 0, // Previously off or on.
                    getFullEnabledLocked() ? 1 : 0, // Now off or on.
                    getAdaptiveEnabledLocked() ? 1 : 0, // Now off or on.
                    isInteractive ?  1 : 0, // Device interactive state.
                    enabled ? mBatterySaverPolicy.toEventLogString() : "",
                    reason);

            mFullPreviouslyEnabled = getFullEnabledLocked();
            mAdaptivePreviouslyEnabled = getAdaptiveEnabledLocked();

            listeners = mListeners.toArray(new LowPowerModeListener[0]);

            mIsInteractive = isInteractive;
        }

        final PowerManagerInternal pmi = LocalServices.getService(PowerManagerInternal.class);
        if (pmi != null) {
            pmi.setPowerMode(Mode.LOW_POWER, isEnabled());
        }

        updateBatterySavingStats();

        if (sendBroadcast) {

            if (DEBUG) {
                Slog.i(TAG, "Sending broadcasts for mode: " + isEnabled());
            }

            // Send the broadcasts and notify the listeners. We only do this when the battery saver
            // mode changes, but not when only the screen state changes.
            Intent intent = new Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);

            // Send the broadcast to a manifest-registered receiver that is specified in the config.
            if (getPowerSaveModeChangedListenerPackage().isPresent()) {
                intent = new Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                        .setPackage(getPowerSaveModeChangedListenerPackage().get())
                        .addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND
                                | Intent.FLAG_RECEIVER_FOREGROUND);
                mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            }

            // Send internal version that requires signature permission.
            intent = new Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED_INTERNAL);
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL,
                    Manifest.permission.DEVICE_POWER);

            for (LowPowerModeListener listener : listeners) {
                final PowerSaveState result =
                        mBatterySaverPolicy.getBatterySaverPolicy(listener.getServiceType());
                listener.onLowPowerModeChanged(result);
            }
        }
    }

    private Optional<String> getPowerSaveModeChangedListenerPackage() {
        if (mPowerSaveModeChangedListenerPackage == null) {
            String configPowerSaveModeChangedListenerPackage =
                    mContext.getString(R.string.config_powerSaveModeChangedListenerPackage);
            mPowerSaveModeChangedListenerPackage =
                    LocalServices
                            .getService(PackageManagerInternal.class)
                            .isSystemPackage(configPowerSaveModeChangedListenerPackage)
                            ? Optional.of(configPowerSaveModeChangedListenerPackage)
                            : Optional.empty();
        }
        return mPowerSaveModeChangedListenerPackage;
    }

    private void updateBatterySavingStats() {
        final PowerManager pm = getPowerManager();
        if (pm == null) {
            Slog.wtf(TAG, "PowerManager not initialized");
            return;
        }
        final boolean isInteractive = pm.isInteractive();
        final int dozeMode =
                pm.isDeviceIdleMode() ? DozeState.DEEP
                        : pm.isLightDeviceIdleMode() ? DozeState.LIGHT
                        : DozeState.NOT_DOZING;

        synchronized (mLock) {
            mBatterySavingStats.transitionState(
                    getFullEnabledLocked() ? BatterySaverState.ON :
                            (getAdaptiveEnabledLocked() ? BatterySaverState.ADAPTIVE :
                            BatterySaverState.OFF),
                            isInteractive ? InteractiveState.INTERACTIVE :
                            InteractiveState.NON_INTERACTIVE,
                            dozeMode,
                    mIsPluggedIn ? PlugState.PLUGGED : PlugState.UNPLUGGED);
        }
    }

    @GuardedBy("mLock")
    private void setFullEnabledLocked(boolean value) {
        if (mFullEnabledRaw == value) {
            return;
        }
        PowerManager.invalidatePowerSaveModeCaches();
        mFullEnabledRaw = value;
    }

    /** Non-blocking getter exists as a reminder not to directly modify the cached field */
    private boolean getFullEnabledLocked() {
        return mFullEnabledRaw;
    }

    @GuardedBy("mLock")
    private void setAdaptiveEnabledLocked(boolean value) {
        if (mAdaptiveEnabledRaw == value) {
            return;
        }
        PowerManager.invalidatePowerSaveModeCaches();
        mAdaptiveEnabledRaw = value;
    }

    /** Non-blocking getter exists as a reminder not to directly modify the cached field */
    private boolean getAdaptiveEnabledLocked() {
        return mAdaptiveEnabledRaw;
    }
}
