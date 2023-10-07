package com.android.clockwork.power;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.InputDevice;

import com.android.clockwork.common.EventHistory;
import com.android.clockwork.emulator.EmulatorUtil;
import com.android.clockwork.flags.BooleanFlag;
import com.android.clockwork.remote.Home;
import com.android.clockwork.remote.WetMode;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

/**
 * Coordinate various components that have an interest in enabling and disabling the touch input of
 * Wear devices.
 */
public class WearTouchMediator
        implements
        AmbientConfig.Listener,
        PowerTracker.Listener,
        TimeOnlyMode.Listener {
    private static final String TAG = WearPowerConstants.LOG_TAG;
    private static final String PERMISSION_TOUCH =
            "com.google.android.clockwork.settings.WATCH_TOUCH";
    private final Context mContext;
    // Flags
    private final BooleanFlag mUserAbsentTouchOff;
    private final EventHistory<TouchDecision> mDecisionHistory =
            new EventHistory<>("Touch Input Decision History", 30, false);
    private final AmbientConfig mAmbientConfig;
    private final PowerTracker mPowerTracker;
    private final TimeOnlyMode mTimeOnlyMode;
    private final InputDeviceProvider mTouchInputProvider;
    private boolean mInteractive = true;
    private boolean mHomeTouchEnabled = true;
    private boolean mTouchLock;
    private boolean mUserAbsentTouchOffEnabled;
    @VisibleForTesting
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, String.format("[WearTouchMediator] received action: %s",
                        action));
            }

            switch (action) {
                case Intent.ACTION_SCREEN_ON:
                    onInteractiveStateChanged(true);
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    onInteractiveStateChanged(false);
                    break;
                default:
                    if (Log.isLoggable(TAG, Log.WARN)) {
                        Log.w(TAG, String.format("[WearTouchMediator] unknown action: %s",
                                action));
                    }
            }
        }
    };

    @VisibleForTesting
    final ContentObserver mBedtimeModeObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, String.format("[WearTouchMediator] observer change (bedtime): %s",
                        isBedtimeControlEnabled() ? "enabled" : "disabled"));
            }
            updateState("bedtime mode changed");
        }
    };

    @VisibleForTesting
    BroadcastReceiver mTouchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, String.format("[WearTouchMediator] received action: %s",
                        action));
            }

            switch (action) {
                case Home.ACTION_ENABLE_TOUCH:
                    onHomeTouchChanged(true);
                    break;
                case Home.ACTION_DISABLE_TOUCH:
                    onHomeTouchChanged(false);
                    break;
                case Home.ACTION_ENABLE_TOUCH_THEATER_MODE_END:
                    updateState("onUpdateTouchState");
                    break;
                case WetMode.ACTION_WET_MODE_STARTED:
                    onTouchLockChanged(true);
                    break;
                case WetMode.ACTION_WET_MODE_ENDED:
                    onTouchLockChanged(false);
                    break;
                default:
                    if (Log.isLoggable(TAG, Log.WARN)) {
                        Log.w(TAG, String.format("[WearTouchMediator] unknown action: %s",
                                action));
                    }
            }
        }
    };

    public WearTouchMediator(
            Context context,
            AmbientConfig ambientConfig,
            PowerTracker powerTracker,
            TimeOnlyMode timeOnlyMode,
            BooleanFlag userAbsentTouchOffObserver) {
        this(context,
                ambientConfig,
                powerTracker,
                timeOnlyMode,
                new InputDeviceProvider(context, InputDevice.SOURCE_TOUCHSCREEN),
                userAbsentTouchOffObserver);
    }

    @VisibleForTesting
    WearTouchMediator(
            Context context,
            AmbientConfig ambientConfig,
            PowerTracker powerTracker,
            TimeOnlyMode timeOnlyMode,
            InputDeviceProvider touchInputProvider,
            BooleanFlag userAbsentTouchOffObserver) {
        mContext = context;

        mUserAbsentTouchOff = userAbsentTouchOffObserver;
        mUserAbsentTouchOff.addListener(this::onUserAbsentTouchOffChanged);

        mTouchInputProvider = touchInputProvider;

        mAmbientConfig = ambientConfig;
        mAmbientConfig.addListener(this);

        mPowerTracker = powerTracker;
        mPowerTracker.addListener(this);

        mTimeOnlyMode = timeOnlyMode;
        mTimeOnlyMode.addListener(this);
    }

    public void onBootCompleted() {
        mUserAbsentTouchOff.register();
        mUserAbsentTouchOffEnabled = mUserAbsentTouchOff.isEnabled();

        /* Receiver for intents to disable/enable touch needs to be protected by
         * "com.google.android.clockwork.settings.WATCH_TOUCH" permission. */
        IntentFilter touchIntentFilter = new IntentFilter();
        touchIntentFilter.addAction(Home.ACTION_ENABLE_TOUCH);
        touchIntentFilter.addAction(Home.ACTION_DISABLE_TOUCH);
        touchIntentFilter.addAction(Home.ACTION_ENABLE_TOUCH_THEATER_MODE_END);
        touchIntentFilter.addAction(WetMode.ACTION_WET_MODE_STARTED);
        touchIntentFilter.addAction(WetMode.ACTION_WET_MODE_ENDED);
        mContext.registerReceiver(mTouchReceiver, touchIntentFilter,
                PERMISSION_TOUCH, null, Context.RECEIVER_EXPORTED);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(receiver, intentFilter);

        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.Wearable.BEDTIME_MODE),
                /* notifyForDescendants= */ false,
                mBedtimeModeObserver);

        mTouchInputProvider.startListeningForDeviceUpdates();

        updateState("onBootCompleted");
    }

    /**
     * Turn on or off the touch sensor.
     *
     * Be very careful when adding a new rule!  The order in which these rules are laid out
     * defines their priority and conditionality.  Each rule is subject to the conditions of
     * all the rules above it, but not vice versa.
     */
    private void updateState(String reason) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, String.format("[WearTouchMediator] updateState(%s)", reason));
        }

        if (EmulatorUtil.isEmulator()) {
            // Emulator does not work correctly with its touch input disabled due to b/72399634.
            // For now keep the touch sensor always enabled in emulator.
            Log.w(TAG, "Emulator doesn't support touch disabling inputs, ignoring.");
            changeTouchEnabled(Reason.ON_EMULATOR);
        } else if (mPowerTracker.isDeviceIdle() && mUserAbsentTouchOffEnabled
                && !allowTouchInDozeMode()) {
            changeTouchEnabled(Reason.OFF_DOZE);
        } else if (mTouchLock) {
            changeTouchEnabled(Reason.OFF_TOUCH_LOCK);
        } else if (isBedtimeControlEnabled()) {
            changeTouchEnabled(Reason.OFF_BEDTIME_MODE);
        } else if (!mInteractive && mTimeOnlyMode.isInTimeOnlyMode() &&
                mTimeOnlyMode.isTouchToWakeDisabled()) {
            changeTouchEnabled(Reason.OFF_BATTERY_SAVER);
        } else if (!mInteractive && shouldLockDuringNonInteractive()) {
            changeTouchEnabled(Reason.OFF_AMBIENT);
        } else if (!mInteractive && isTheaterModeEnabled()) {
            changeTouchEnabled(Reason.OFF_THEATER_MODE);
        } else if (!mHomeTouchEnabled) {
            changeTouchEnabled(Reason.OFF_HOME_REQUEST);
        } else {
            changeTouchEnabled(Reason.ON_AUTO);
        }
    }

    private boolean isTheaterModeEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.THEATER_MODE_ON, 0) == 1;
    }

    private boolean shouldLockDuringNonInteractive() {
        // For non-interactive screens, we disable screen touch if touch-to-wake is disabled, or if
        // Bedtime mode is enabled (to avoid user disturbance and reduce power usage during sleep).
        return !mAmbientConfig.isTouchToWake() || isBedtimeModeEnabled();
    }

    private boolean isBedtimeControlEnabled() {
        return isBedtimeModeEnabled() && isBedtimeLockingTouch();
    }

    private boolean isBedtimeModeEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.Wearable.BEDTIME_MODE, /* def= */ 0) == 1;
    }

    private boolean isBedtimeLockingTouch() {
        // TODO(b/225086396): configure when this may return true.
        return false;
    }

    private void changeTouchEnabled(Reason reason) {
        EnablableInputDevice device = mTouchInputProvider.getDevice();
        if (device == null) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "[WearTouchMediator] could not find touch input!");
            }
            return;
        }

        if (!Build.TYPE.equals("user") || Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, String.format("[WearTouchMediator] changing touch input to %s (%s)",
                    reason.enabled ? "enabled" : "disabled", reason.name()));
        }

        mDecisionHistory.recordEvent(new TouchDecision(reason));

        if (reason.enabled) {
            device.enable();
        } else {
            device.disable();
        }
    }

    @Override
    public void onDeviceIdleModeChanged() {
        if (!allowTouchInDozeMode()) {
            updateState("onDeviceIdleModeChanged()");
        } else {
            Log.d(TAG, "Ignoring doze mode intent as Touch is being kept enabled during doze.");
        }
    }

    private boolean allowTouchInDozeMode() {
        return mPowerTracker.getDozeModeAllowListedFeatures().get(
                PowerTracker.DOZE_MODE_TOUCH_INDEX);
    }

    @Override
    public void onAmbientConfigChanged() {
        updateState("onAmbientConfigChanged");
    }

    @Override
    public void onTimeOnlyModeChanged(boolean timeOnlyMode) {
        updateState(String.format("onTimeOnlyModeChanged(%b)", timeOnlyMode));
    }

    public void onUserAbsentTouchOffChanged(boolean enabled) {
        mUserAbsentTouchOffEnabled = enabled;
        updateState(String.format("onUserAbsentTouchOffChanged(%b)", enabled));
    }

    public void onInteractiveStateChanged(boolean interactive) {
        mInteractive = interactive;
        updateState(String.format("onInteractiveStateChanged(%b)", interactive));
    }

    /**
     * Home sends broadcasts asking to turn the touch sensor on or off.
     */
    public void onHomeTouchChanged(boolean enabled) {
        mHomeTouchEnabled = enabled;
        updateState(String.format("onHomeTouchChanged(%b)", enabled));
    }

    public void onTouchLockChanged(boolean enabled) {
        mTouchLock = enabled;
        updateState("onTouchLockChanged");
    }

    public void dump(IndentingPrintWriter ipw) {
        ipw.println("======== WearTouchMediator ========");

        String touchStatus = "unknown!";
        EnablableInputDevice device = mTouchInputProvider.getDevice();
        if (device != null) {
            touchStatus = device.isEnabled() ? "enabled" : "disabled";
        }
        ipw.printPair("Touch Input", touchStatus);
        ipw.println();
        ipw.println();

        ipw.printPair("Device Interactive", mInteractive);
        ipw.println();
        ipw.printPair("Device Idle", mPowerTracker.isDeviceIdle());
        ipw.println();
        ipw.println();

        ipw.printPair("Touch To Wake", mAmbientConfig.isTouchToWake());
        ipw.println();
        ipw.printPair("Touch Lock", mTouchLock);
        ipw.println();
        ipw.printPair("Time Only Mode", mTimeOnlyMode.isInTimeOnlyMode());
        ipw.println();
        ipw.printPair("Home Touch Enabled", mHomeTouchEnabled);
        ipw.println();
        ipw.printPair("Allowed during doze mode",
                mPowerTracker.getDozeModeAllowListedFeatures()
                        .get(PowerTracker.DOZE_MODE_TOUCH_INDEX));
        ipw.println();
        ipw.println();

        ipw.println("--- Flags ---");
        ipw.printPair("mUserAbsentTouchOffEnabled", mUserAbsentTouchOffEnabled);
        ipw.println();
        ipw.println();

        ipw.increaseIndent();
        mDecisionHistory.dump(ipw);
        ipw.decreaseIndent();
        ipw.println();
    }

    public enum Reason {
        /** The device is in ambient mode and touch to wake is disabled. */
        OFF_AMBIENT(false),
        /** The device is in extended battery saver mode AKA "time only mode". */
        OFF_BATTERY_SAVER(false),
        /** The device is in deep doze. */
        OFF_DOZE(false),
        /** Home has requested the touch sensor be disabled. */
        OFF_HOME_REQUEST(false),
        /** the device is in theater mode */
        OFF_THEATER_MODE(false),
        /** The device is in Bedtime mode. */
        OFF_BEDTIME_MODE(false),
        /** The device is in touch lock mode AKA "wet mode". */
        OFF_TOUCH_LOCK(false),
        /**
         * The default state. Nothing has requested that touch be off and nothing is forcing it on.
         */
        ON_AUTO(true),
        /**
         * Forced on since the device is an emulator and the emulator does not yet support disabling
         * the touch device due to b/72399634.
         */
        ON_EMULATOR(true);

        final boolean enabled;

        Reason(boolean enabled) {
            this.enabled = enabled;
        }
    }

    private class TouchDecision extends EventHistory.Event {
        public final Reason reason;

        public TouchDecision(Reason reason) {
            this.reason = reason;
        }

        @Override
        public String getName() {
            return reason.name();
        }
    }
}
