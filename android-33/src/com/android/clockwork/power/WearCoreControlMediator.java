package com.android.clockwork.power;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.util.Log;

import com.android.clockwork.common.EventHistory;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

/** This class computes preferred core control mode based on device interactive states. */
public class WearCoreControlMediator implements PowerTracker.Listener {
    private static final String TAG = WearPowerConstants.LOG_TAG;

    @VisibleForTesting static final String SYSPROP_CORE_CONTROL_ENABLE = "ro.enable_core_ctl";
    @VisibleForTesting static final String SYSPROP_CORE_CONTROL_MODE = "sys.cw_corectl_mode";
    @VisibleForTesting static final String MODE_PERF = "perf";
    @VisibleForTesting static final String MODE_IDLE = "idle";

    // Reasons for different core control mode sorted by decision priority
    enum Reason {
        // Performance mode when device just finished boot
        PERF_BOOT_COMPLETE(true),
        // Idle mode when PowerManager.isDeviceIdleMode() is true
        IDLE_DEVICE_IDLE(false),
        // Performance mode when screen is on
        PERF_SCREEN_ON(true),
        // Performance mode when charging
        PERF_CHARGING(true),
        // Idle mode when screen off
        IDLE_SCREEN_OFF(false);

        final boolean performanceMode;

        Reason(boolean performanceMode) {
            this.performanceMode = performanceMode;
        }
    }

    private final EventHistory<CoreCtlDecision> mDecisionHistory =
            new EventHistory<>("Core Control Decision History", 10, false);

    private final Context mContext;
    private final AlarmManager mAlarmManager;
    private final PowerTracker mPowerTracker;

    private boolean mScreenOn = true;
    private boolean mIsCharging = false;
    private boolean mDeviceIdle = false;

    private boolean mPerformanceMode = true;

    static boolean shouldEnable() {
        return SystemProperties.getBoolean(SYSPROP_CORE_CONTROL_ENABLE, false);
    }

    public WearCoreControlMediator(Context context, PowerTracker powerTracker) {
        this(context, context.getSystemService(AlarmManager.class), powerTracker);
    }

    @VisibleForTesting
    public WearCoreControlMediator(
            Context context, AlarmManager alarmManager, PowerTracker powerTracker) {
        Log.d(TAG, "WearCoreControlMediator enabled");

        mContext = context;
        mAlarmManager = alarmManager;
        // TODO(b/156985829): Use AlarmManager to pin and unpin performance mode.
        mPowerTracker = powerTracker;
        mPowerTracker.addListener(this);
    }

    private void updateCoreControlDecision() {
        final boolean isCharging = mPowerTracker.isCharging();
        final boolean isDeviceIdle = mPowerTracker.isDeviceIdle();

        Reason reason = Reason.IDLE_SCREEN_OFF;
        if (isDeviceIdle) {
            reason = Reason.IDLE_DEVICE_IDLE;
        } else if (mScreenOn) {
            reason = Reason.PERF_SCREEN_ON;
        } else if (isCharging) {
            reason = Reason.PERF_CHARGING;
        }

        if (reason.performanceMode == mPerformanceMode) {
            // De-dup state transition
            return;
        }
        updateCoreControlDecision(reason);
    }

    private void updateCoreControlDecision(Reason reason) {
        mPerformanceMode = reason.performanceMode;
        SystemProperties.set(SYSPROP_CORE_CONTROL_MODE, mPerformanceMode ? MODE_PERF : MODE_IDLE);
        mDecisionHistory.recordEvent(new CoreCtlDecision(reason));
    }

    public void onBootCompleted() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(receiver, intentFilter);

        // TODO(b/156985829): Use AlarmManager to pin and unpin performance mode.
        updateCoreControlDecision(Reason.PERF_BOOT_COMPLETE);
    }

    @Override
    public void onDeviceIdleModeChanged() {
        updateCoreControlDecision();
    }

    @Override
    public void onChargingStateChanged() {
        updateCoreControlDecision();
    }

    BroadcastReceiver receiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.v(TAG, String.format("[WearCoreControl] received action: %s", action));
                    }

                    switch (action) {
                        case Intent.ACTION_SCREEN_ON:
                            mScreenOn = true;
                            break;
                        case Intent.ACTION_SCREEN_OFF:
                            mScreenOn = false;
                            break;
                        default:
                            return;
                    }

                    // Recompute decision
                    updateCoreControlDecision();
                }
            };

    private class CoreCtlDecision extends EventHistory.Event {
        private final Reason mReason;

        public CoreCtlDecision(Reason reason) {
            this.mReason = reason;
        }

        @Override
        public String getName() {
            return mReason.name();
        }
    }

    public void dump(IndentingPrintWriter ipw) {
        // Other states are already logged by WearPowerService.
        ipw.println("======== WearCoreControlMediator ========");
        ipw.println();

        ipw.increaseIndent();
        mDecisionHistory.dump(ipw);
        ipw.decreaseIndent();
        ipw.println();
    }
}
