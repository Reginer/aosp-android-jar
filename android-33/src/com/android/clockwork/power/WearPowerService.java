package com.android.clockwork.power;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;

import com.android.clockwork.flags.BooleanFlag;
import com.android.clockwork.flags.ClockworkFlags;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.SystemService;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * A {@link SystemService} to coordinate keeping track of and controlling the devices and modules
 * that influence the use of Wear's precious power resources.
 *
 * <p>This services responsiblities include:
 *
 * <ul>
 *   <li>starting the {@link WearTouchMediator}
 *   <li>providing access to {@link PowerTracker} & {@link TimeOnlyMode} to other Wear {@link
 *       SystemService}s
 * </ul>
 */
public class WearPowerService extends SystemService {
    public static final String SERVICE_NAME = WearPowerService.class.getSimpleName();

    private static final String TAG = WearPowerConstants.LOG_TAG;
    private static final String ACTION_GO_TO_SLEEEP =
            "com.android.clockwork.action.GO_TO_SLEEP";
    private static final String PERMISSION_GO_TO_SLEEP =
            "com.android.clockwork.permission.GO_TO_SLEEP";
    private static final int POWER_BUTTON_SUPPRESSION_DELAY_DEFAULT_MILLIS = 800;
    private int mPowerButtonSuppressionDelayMs = POWER_BUTTON_SUPPRESSION_DELAY_DEFAULT_MILLIS;

    private AmbientConfig mAmbientConfig;
    private PowerTracker mPowerTracker;
    private TimeOnlyMode mTimeOnlyMode;

    private WearTouchMediator mWearTouchMediator;
    private WearCoreControlMediator mWearCoreControlMediator;
    private WearBurnInProtectionMediator mWearBurnInProtectionMediator;
    private WearDisplayOffloadMediator mWearDisplayOffloadMediator;

    private final ContentObserver mSettingObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    };

    /**
     * Sends the device to sleep as a result of power button press.
     */
    private final BroadcastReceiver goToSleepReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(ACTION_GO_TO_SLEEEP)) {
                        PowerManagerInternal mPowerManagerInternal =
                                LocalServices.getService(PowerManagerInternal.class);
                        // Before we actually go to sleep, we check the last wakeup reason.
                        // If the device very recently/just woke up from a gesture/power button
                        // press
                        // then ignore the sleep instruction.
                        final PowerManager.WakeData lastWakeUp =
                                mPowerManagerInternal.getLastWakeup();
                        if (lastWakeUp != null
                                && (lastWakeUp.wakeReason == PowerManager.WAKE_REASON_POWER_BUTTON
                                ||
                                lastWakeUp.wakeReason == PowerManager.WAKE_REASON_GESTURE)) {
                            final long now = SystemClock.uptimeMillis();
                            if (mPowerButtonSuppressionDelayMs > 0
                                    && (now
                                    < lastWakeUp.wakeTime + mPowerButtonSuppressionDelayMs)) {
                                Slog.i(TAG,
                                        "Sleep from power button suppressed. Time since gesture: "
                                                + (now - lastWakeUp.wakeTime) + "ms");
                                return;
                            }
                        }

                        Slog.i(TAG, "Turning off the screen/entering ambient mode.");
                        context.getSystemService(PowerManager.class)
                                .goToSleep(SystemClock.uptimeMillis(),
                                        PowerManager.GO_TO_SLEEP_REASON_POWER_BUTTON, /* flags= */
                                        0);
                    }
                }
            };

    public WearPowerService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        publishLocalService(WearPowerServiceInternal.class, new LocalService());
        publishBinderService(SERVICE_NAME, new BinderService());
        getContext().registerReceiver(goToSleepReceiver, new IntentFilter(ACTION_GO_TO_SLEEEP),
                PERMISSION_GO_TO_SLEEP, /* scheduler= */null,
                Context.RECEIVER_EXPORTED);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == SystemService.PHASE_SYSTEM_SERVICES_READY) {
            onSystemServicesReady();
        } else if (phase == SystemService.PHASE_BOOT_COMPLETED) {
            onBootCompleted();
        }
    }

    private void onSystemServicesReady() {
        Context context = getContext();
        ContentResolver contentResolver = context.getContentResolver();

        // Set up flags
        BooleanFlag userAbsentTouchOff = ClockworkFlags.userAbsentTouchOff(contentResolver);

        mAmbientConfig = new AmbientConfig(contentResolver);
        mPowerTracker = new PowerTracker(
                context, context.getSystemService(PowerManager.class));
        mTimeOnlyMode = new TimeOnlyMode(contentResolver, mPowerTracker);

        mWearTouchMediator = new WearTouchMediator(
                context, mAmbientConfig, mPowerTracker, mTimeOnlyMode, userAbsentTouchOff);
        mWearBurnInProtectionMediator = new WearBurnInProtectionMediator(context, mAmbientConfig);

        mWearDisplayOffloadMediator = new WearDisplayOffloadMediator();

        if (WearCoreControlMediator.shouldEnable()) {
            mWearCoreControlMediator = new WearCoreControlMediator(context, mPowerTracker);
        }
        registerContentObserver();
        updateSettings();
    }

    private void registerContentObserver() {
        Context context = getContext();
        ContentResolver contentResolver = context.getContentResolver();
        contentResolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.POWER_BUTTON_SUPPRESSION_DELAY_AFTER_GESTURE_WAKE),
                /* notifyForDescendants= */ false, mSettingObserver, UserHandle.USER_ALL);
    }

    private void updateSettings() {
        mPowerButtonSuppressionDelayMs = Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.POWER_BUTTON_SUPPRESSION_DELAY_AFTER_GESTURE_WAKE,
                POWER_BUTTON_SUPPRESSION_DELAY_DEFAULT_MILLIS);
    }

    private void onBootCompleted() {
        mAmbientConfig.register();

        mPowerTracker.onBootCompleted();
        mWearTouchMediator.onBootCompleted();

        if (mWearCoreControlMediator != null) {
            mWearCoreControlMediator.onBootCompleted();
        }
    }

    private final class LocalService extends WearPowerServiceInternal {
        @Override
        public AmbientConfig getAmbientConfig() {
            return mAmbientConfig;
        }

        @Override
        public PowerTracker getPowerTracker() {
            return mPowerTracker;
        }

        @Override
        public TimeOnlyMode getTimeOnlyMode() {
            return mTimeOnlyMode;
        }
    }

    private final class BinderService extends IWearPowerService.Stub {
        @Override
        protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
            IndentingPrintWriter ipw = new IndentingPrintWriter(writer, "  " /* singleIndent */);

            ipw.println("================ WearPowerService ================");
            ipw.println();
            ipw.increaseIndent();

            mAmbientConfig.dump(ipw);
            mPowerTracker.dump(ipw);
            mTimeOnlyMode.dump(ipw);
            ipw.println();

            mWearTouchMediator.dump(ipw);

            if (mWearCoreControlMediator != null) {
                ipw.println();
                mWearCoreControlMediator.dump(ipw);
            }

            mWearDisplayOffloadMediator.dump(ipw);

            ipw.decreaseIndent();
            ipw.println();
        }

        @Override
        public int offloadBackendGetType()  {
            if (mWearDisplayOffloadMediator != null) {
                return mWearDisplayOffloadMediator.offloadBackendGetType();
            }
            return com.android.clockwork.power.IWearPowerService.OFFLOAD_BACKEND_TYPE_NA;
        }

        @Override
        public boolean offloadBackendReadyToDisplay() {
            return mWearDisplayOffloadMediator != null
                    && mWearDisplayOffloadMediator.offloadBackendReadyToDisplay();
        }

        @Override
        public void offloadBackendSetShouldControlDisplay(boolean shouldControl) {
            if (mWearDisplayOffloadMediator != null) {
                mWearDisplayOffloadMediator.setShouldControlDisplay(shouldControl);
            }
        }
    }
}
