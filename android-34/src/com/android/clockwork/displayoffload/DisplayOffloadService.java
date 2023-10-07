package com.android.clockwork.displayoffload;

import static android.os.PowerManager.WAKE_REASON_APPLICATION;

import static com.android.clockwork.displayoffload.DebugUtils.DEBUG_CALLBACK;
import static com.android.clockwork.displayoffload.DebugUtils.DEBUG_DOZE;
import static com.android.clockwork.displayoffload.DebugUtils.DEBUG_HAL;
import static com.android.clockwork.displayoffload.DebugUtils.DEBUG_UID;
import static com.android.clockwork.displayoffload.Utils.TAG;
import static com.android.clockwork.displayoffload.Utils.isDisplayDozeOrOn;
import static com.android.wearable.resources.R.integer.config_onDisplayOffloadUpdateDozeIntervalMs;

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.NAME;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.RESOURCE_TYPE_ACTIVITY;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.RESOURCE_TYPE_SYSTEMUI;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.RESOURCE_TYPE_WATCHFACE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.Stub;

import android.annotation.IntDef;
import android.annotation.Nullable;
import android.app.ActivityTaskManager;
import android.app.AlarmManager;
import android.app.IActivityTaskManager;
import android.app.WallpaperColors;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;

import com.android.clockwork.common.PartialWakeLock;
import com.android.clockwork.common.WearResourceUtil;
import com.android.clockwork.display.burninprotection.BurnInProtector;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.wm.WindowManagerInternal;

import com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks;
import com.google.android.clockwork.ambient.offload.types.BitmapResource;
import com.google.android.clockwork.ambient.offload.types.TranslationGroup;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;

/** A SystemService that connects to vendor.google_clockwork.displayoffload HAL */
public class DisplayOffloadService extends SystemService {
    public static final String SERVICE_NAME = NAME;

    // Debug flags
    private static final boolean DEBUG = DEBUG_HAL || true;

    // DisplayOffload internal/external event
    // LINT.IfChange(events_int_def)
    static final int BEGIN_RECEIVE_WATCHFACE = 0;
    static final int END_RECEIVE_WATCHFACE = 1;
    static final int BEGIN_RECEIVE_ACTIVITY = 2;
    static final int END_RECEIVE_ACTIVITY = 3;
    static final int BEGIN_SEND_LAYOUT = 4;
    static final int END_SEND_LAYOUT = 5;
    static final int START_DISPLAY_CONTROL = 6;
    static final int END_DISPLAY_CONTROL = 7;
    static final int ACTIVITY_CHANGED = 8;
    static final int WATCHFACE_CHANGED = 9;
    static final int RUN_HAL_OPERATIONS_WAITING_DOZE_OR_ON = 10;
    static final int OFFLOAD_UPDATE_DOZE_INTERVAL = 11;
    static final int UPDATE_LOCALE = 13;
    static final int HAL_CONNECTED = 14;
    static final int AMBIENT_STATE_CHANGED = 15;
    static final int END_DISPLAY_MCU_RENDERING = 16;
    static final int HAL_DIED = 17;
    static final int BEGIN_RECEIVE_SYSTEMUI = 18;
    static final int END_RECEIVE_SYSTEMUI = 19;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                    BEGIN_RECEIVE_WATCHFACE,
                    END_RECEIVE_WATCHFACE,
                    BEGIN_RECEIVE_ACTIVITY,
                    END_RECEIVE_ACTIVITY,
                    BEGIN_RECEIVE_SYSTEMUI,
                    END_RECEIVE_SYSTEMUI,
                    BEGIN_SEND_LAYOUT,
                    RUN_HAL_OPERATIONS_WAITING_DOZE_OR_ON,
                    END_SEND_LAYOUT,
                    START_DISPLAY_CONTROL,
                    END_DISPLAY_CONTROL,
                    ACTIVITY_CHANGED,
                    WATCHFACE_CHANGED,
                    OFFLOAD_UPDATE_DOZE_INTERVAL,
                    UPDATE_LOCALE,
                    HAL_CONNECTED,
                    AMBIENT_STATE_CHANGED,
                    END_DISPLAY_MCU_RENDERING,
                    HAL_DIED,
            })
    @interface DisplayOffloadEvent {
    }
    // LINT.ThenChange(Utils.java:name_for_events)

    // DisplayOffload layout type
    static final int NONE = Integer.MIN_VALUE;

    static final int SYSTEMUI = 1;
    static final int WATCHFACE = 2;
    static final int ACTIVITY = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                    NONE, SYSTEMUI, WATCHFACE, ACTIVITY,
            })
    public @interface LayoutType {
    }

    private final Context mContext;
    private PowerManagerInternal mPowerManagerInternal;
    private WindowManagerInternal mWindowManagerInternal;
    private PowerManager mPowerManager;
    private AlarmManager mAlarmManager;

    private final AlarmManager.OnAlarmListener mOnAlarmListener = this::postOnOffloadUpdateCallback;
    private final Runnable mRecomputeDozeOverride = () -> recomputeDozeOverride(false);

    private BrightnessOffloadController mBrightnessOffloadController;
    private AmbientModeTracker mAmbientModeTracker;

    // For devices that want to switch display state to DOZE during OnDisplayOffloadUpdate,
    // how many ms before we return to DOZE_SUSPEND
    private int mOnUpdateDozeReleaseInterval;
    private boolean mOffloadUpdateDozeIntervalRequested;

    // DisplayOffload state
    private DisplayManager mDisplayManager;
    private DisplayManager.DisplayListener mDisplayListener;
    private final Object mDisplayOffloadStateLock = new Object();

    private DisplayOffloadShellCommand mShell;
    private WallpaperManager mWallpaperManager;
    private DisplayControlLockManager mDisplayControlLockManager;
    private PartialWakeLock mWakeLock;
    private PartialWakeLock mOffloadUpdateIntervalWakeLock;
    private IActivityTaskManager mActivityTaskManagerService;
    private PackageManager mPackageManager;

    private final ClientCallbackManager mClientCallbackManager = new ClientCallbackManager();

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler mBackgroundHandler;

    @GuardedBy("mDisplayOffloadStateLock")
    private boolean mDisplayOffloadReady = false;

    @GuardedBy("mDisplayOffloadStateLock")
    private boolean mDisplayOffloadIsControlling;

    @GuardedBy("mDisplayOffloadStateLock")
    private boolean mUpdateOnNextDoze = false;

    @GuardedBy("mDisplayOffloadStateLock")
    private boolean mHalOperationWaitingForDozeOrOn = false;

    @GuardedBy("mDisplayOffloadStateLock")
    private boolean mPendingDozeInterval;

    private int mLastResumedActivityUid = -1;
    private int mCurrentWatchFaceUid = -1;
    private int mSystemUiUid = -1;

    @GuardedBy("mDisplayOffloadStateLock")
    @Nullable
    private OffloadLayout mWatchFaceLayout;

    @GuardedBy("mDisplayOffloadStateLock")
    @Nullable
    private OffloadLayout mActivityLayout;

    @GuardedBy("mDisplayOffloadStateLock")
    @Nullable
    private OffloadLayout mSystemUILayout;

    @GuardedBy("mDisplayOffloadStateLock")
    @Nullable
    private TranslationGroup mStatusBarGroup;

    @GuardedBy("mDisplayOffloadStateLock")
    @Nullable
    private BitmapResource mStatusBarResource;

    // TODO(b/267223258): cache the OffloadLayout directly to avoid some switch cases.
    @GuardedBy("mDisplayOffloadStateLock")
    @LayoutType
    private int mCurrentLayoutType = NONE;

    // HAL service connections
    @GuardedBy("mDisplayOffloadStateLock")
    private final HalAdapter mHalAdapter;

    private final BroadcastReceiver mIntentReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    switch (action) {
                        case Intent.ACTION_LOCALE_CHANGED:
                            updateDisplayOffloadState(UPDATE_LOCALE);
                            break;
                        default:
                            break;
                    }
                }
            };

    public DisplayOffloadService(Context context) {
        super(context);
        if (DEBUG) {
            Log.d(TAG, "DisplayOffloadService instance created");
        }

        mContext = context;

        HandlerThread observerThread = new HandlerThread("DisplayOffloadBgThread");
        observerThread.start();
        mBackgroundHandler = new Handler(observerThread.getLooper());

        mHalAdapter = new HalAdapter(mContext);
        mHalAdapter.attachHalListener(
                new HalAdapter.HalListener() {
                    @Override
                    public void onHalRegistered() {
                        updateDisplayOffloadState(HAL_CONNECTED);
                    }

                    @Override
                    public void onHalDied() {
                        updateDisplayOffloadState(HAL_DIED);
                    }
                });
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase != com.android.server.SystemService.PHASE_SYSTEM_SERVICES_READY &&
                phase != com.android.server.SystemService.PHASE_BOOT_COMPLETED) {
            return;
        }
        boot(phase);
    }

    void boot(int phase) {
        Log.d(TAG, "onBootPhase: " + phase);
        if (phase == com.android.server.SystemService.PHASE_SYSTEM_SERVICES_READY) {
            // Wake Lock set up
            mWakeLock = new PartialWakeLock(mContext, "DisplayOffload");
            mOffloadUpdateIntervalWakeLock =
                    new PartialWakeLock(mContext, "DisplayOffload.OffloadUpdateInterval");

            // Managers set up
            mDisplayControlLockManager =
                    new DisplayControlLockManager(
                            () -> recomputeDozeOverride(true), () -> recomputeDozeOverride(false));
            mAlarmManager = mContext.getSystemService(AlarmManager.class);
            mPowerManager = mContext.getSystemService(PowerManager.class);
            mDisplayManager = mContext.getSystemService(DisplayManager.class);
            mWindowManagerInternal = LocalServices.getService(WindowManagerInternal.class);
            SensorManager sensorManager = mContext.getSystemService(SensorManager.class);
            mWallpaperManager = mContext.getSystemService(WallpaperManager.class);
            mActivityTaskManagerService =
                    IActivityTaskManager.Stub.asInterface(
                            ServiceManager.getService(Context.ACTIVITY_TASK_SERVICE));

            // Brightness offload
            Resources resources = WearResourceUtil.getWearableResources(mContext);
            if (resources == null) {
                Log.wtf(TAG, "Unable to load wearable framework resources");
                return;
            }
            mBrightnessOffloadController =
                    new BrightnessOffloadController(
                            mContext.getContentResolver(),
                            mContext.getResources(),
                            sensorManager,
                            mBackgroundHandler,
                            mHalAdapter);

            // Brightness offload update interval
            mOnUpdateDozeReleaseInterval = resources.getInteger(
                    config_onDisplayOffloadUpdateDozeIntervalMs);

            // Broadcast Receiver set up
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
            mContext.registerReceiver(mIntentReceiver, intentFilter);

            mAmbientModeTracker =
                    new AmbientModeTracker(
                            () -> updateDisplayOffloadState(AMBIENT_STATE_CHANGED),
                            mBackgroundHandler);
            mContext.registerReceiver(mAmbientModeTracker, mAmbientModeTracker.getIntentFilter(),
                    Context.RECEIVER_EXPORTED_UNAUDITED);

            mDisplayListener =
                    new DisplayManager.DisplayListener() {
                        @Override
                        public void onDisplayAdded(int displayId) {
                        }

                        @Override
                        public void onDisplayRemoved(int displayId) {
                        }

                        @Override
                        public void onDisplayChanged(int displayId) {
                            // Display state is not set synchronously. Call
                            // runHalOperationsWaitingDozeOrOn
                            // again when we're getting notified for display state change and if
                            // prior
                            // attempt has failed.
                            // See b/230794208 for more context.
                            mWakeLock.acquire();
                            try {
                                synchronized (mDisplayOffloadStateLock) {
                                    final int displayState =
                                            mDisplayManager
                                                    .getDisplay(Display.DEFAULT_DISPLAY)
                                                    .getState();
                                    if (mHalOperationWaitingForDozeOrOn
                                            && isDisplayDozeOrOn(displayState)
                                            && !mDisplayOffloadIsControlling) {
                                        Log.w(
                                                TAG,
                                                "onDisplayChanged: display in DOZE or ON, has hal"
                                                        + " operation waiting");
                                        runHalOperationsWaitingDozeOrOn();
                                    }
                                }
                            } finally {
                                mWakeLock.release();
                            }
                        }
                    };
            mDisplayManager.registerDisplayListener(mDisplayListener, mainHandler);

            updateDisplayOffloadState(UPDATE_LOCALE);

            mPackageManager = mContext.getPackageManager();
            try {
                mSystemUiUid = mPackageManager.getUidForSharedUser("android.uid.systemui");
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "unpackResourcesLocked: Unknown SystemUi uid.");
            }
        }

        if (phase == com.android.server.SystemService.PHASE_BOOT_COMPLETED) {
            mBrightnessOffloadController.onBootComplete();
            mWallpaperManager.addOnColorsChangedListener(
                    (WallpaperColors colors, int which) -> {
                        if (mWallpaperManager == null) {
                            return;
                        }
                        // Only call getWallpaperInfo once to prevent its value changing after we
                        // checked for null. See b/229852082.
                        WallpaperInfo wallpaperInfo = mWallpaperManager.getWallpaperInfo();
                        if (wallpaperInfo == null) {
                            return;
                        }
                        setWallpaperComponent(wallpaperInfo.getComponent());
                    }, null);
        }
    }

    private void setWallpaperComponent(ComponentName componentName) {
        Log.d(TAG, "setWallpaperComponent: " + componentName);
        if (componentName == null) {
            return;
        }
        try {
            mCurrentWatchFaceUid = mPackageManager.getPackageUid(componentName.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "unable to find uid for package: " + componentName.getPackageName());
            mCurrentWatchFaceUid = -1;
        }
        // We should update state even if the new and old UIDs are the same because many
        // watchfaces will often be in the same package and we should not use an old layout
        updateDisplayOffloadState(WATCHFACE_CHANGED);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart");

        publishBinderService(SERVICE_NAME, new BinderService());
        /*
        BinderService binderService = new BinderService();
        publishBinderService(SERVICE_NAME, binderService);
        LocalServices.addService(DisplayOffloadInternal.class, new LocalService());
        */

        mPowerManagerInternal = LocalServices.getService(PowerManagerInternal.class);
        mHalAdapter.registerHalRegistrationNotification();

        if (Build.IS_DEBUGGABLE) {
            /*
            mShell = new DisplayOffloadShellCommand(binderService);
            */
        }
    }

    private void wakeUp(String reason) {
        if (mPowerManager != null) {
            mPowerManager.wakeUp(
                    SystemClock.uptimeMillis(),
                    WAKE_REASON_APPLICATION,
                    "displayoffload: " + reason);
        }
    }

    private int getCurrentLayoutUid() {
        synchronized (mDisplayOffloadStateLock) {
            switch (mCurrentLayoutType) {
                case WATCHFACE:
                    return mCurrentWatchFaceUid;
                case ACTIVITY:
                    return mLastResumedActivityUid;
                case SYSTEMUI:
                    return mSystemUiUid;
                default:
                    return -1;
            }
        }
    }

    private void updateDisplayOffloadState(@DisplayOffloadEvent int event) {
        synchronized (mDisplayOffloadStateLock) {
            updateDisplayOffloadStateLocked(event);
        }
    }

    @GuardedBy("mDisplayOffloadStateLock")
    private void updateDisplayOffloadStateLocked(@DisplayOffloadEvent int event) {
        if (mWakeLock == null) {
            // Skip any states until onBootPhase
            return;
        }
        try {
            mWakeLock.acquire();
            updateDisplayOffloadStateLockedWakeful(event);
        } finally {
            mWakeLock.release();
        }
    }

    @GuardedBy("mDisplayOffloadStateLock")
    private void updateDisplayOffloadStateLockedWakeful(@DisplayOffloadEvent int event) {
        Log.d(TAG, "updateDisplayOffloadState: " + Utils.nameForEvent(event));
        switch (event) {
            case BEGIN_RECEIVE_WATCHFACE:
                mWatchFaceLayout.open();
                break;
            case BEGIN_RECEIVE_ACTIVITY:
                mActivityLayout.open();
                break;
            case BEGIN_RECEIVE_SYSTEMUI:
                mSystemUILayout.open();
                break;
            case END_RECEIVE_WATCHFACE:
                if (mWatchFaceLayout == null) {
                    // clearResource called before endResource
                    break;
                }
                mWatchFaceLayout.close();
                updateStatusBarLocked();
                updateLayoutLocked();
                break;
            case END_RECEIVE_ACTIVITY:
                if (mActivityLayout == null) {
                    // clearResource called before endResource
                    break;
                }
                mActivityLayout.close();
                updateLayoutLocked();
                break;
            case END_RECEIVE_SYSTEMUI:
                if (mSystemUILayout == null) {
                    break;
                }
                mSystemUILayout.close();
                updateLayoutLocked();
                break;
            case BEGIN_SEND_LAYOUT:
                // Set some state
                recomputeDozeOverrideLocked(true);
                break;
            case END_SEND_LAYOUT:
                // We are ready to offload now
                if (!mDisplayOffloadIsControlling) {
                    mDisplayOffloadReady = true;
                    if (!mainHandler.hasCallbacks(mRecomputeDozeOverride)) {
                        // If we don't have a recompute scheduled already
                        recomputeDozeOverrideLocked(false);
                    }
                }
                break;
            case RUN_HAL_OPERATIONS_WAITING_DOZE_OR_ON:
                // Acquire an overlapping wake lock for this handler
                mWakeLock.acquire();
                mainHandler.post(() -> {
                    try {
                        runHalOperationsWaitingDozeOrOn();
                    } finally {
                        mWakeLock.release();
                    }
                });
                break;
            case START_DISPLAY_CONTROL:
                boolean ok = false;
                try {
                    ok = beginDisplayLocked();
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception talking to the HAL: ", e);
                }
                if (!ok) {
                    mDisplayOffloadReady = false;
                    recomputeDozeOverrideLocked(false);
                }
                mainHandler.post(() -> {
                    FrameworkStatsLog.write(FrameworkStatsLog.AMBIENT_MODE_CHANGED,
                            FrameworkStatsLog.AMBIENT_MODE_CHANGED__STATE__OFFLOAD_ENTER);
                });
                // Note: callbacks are triggered after the synchronized block
                break;
            case END_DISPLAY_MCU_RENDERING:
                try {
                    endMcuRenderingLocked();
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception talking to the HAL: ", e);
                }
                break;
            case END_DISPLAY_CONTROL:
                try {
                    endDisplayLocked();
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception talking to the HAL: ", e);
                }
                mainHandler.post(
                        () -> {
                            FrameworkStatsLog.write(
                                    FrameworkStatsLog.AMBIENT_MODE_CHANGED,
                                    FrameworkStatsLog.AMBIENT_MODE_CHANGED__STATE__OFFLOAD_EXIT);
                        });
                // Note: callbacks are triggered after the synchronized block
                break;
            case ACTIVITY_CHANGED:
                mActivityLayout = null;
                updateLayoutLocked();
                break;
            case WATCHFACE_CHANGED:
                mWatchFaceLayout = null;

                wakeUp("watchface changed");
                // If we were currently using this watchface layout then clear it.
                if (mCurrentLayoutType == WATCHFACE) {
                    updateLayoutLocked();
                }
                break;
            case OFFLOAD_UPDATE_DOZE_INTERVAL:
                if (mDisplayOffloadIsControlling) {
                    // We're requesting an interval of no-offloading.
                    mOffloadUpdateDozeIntervalRequested = true;
                    mPendingDozeInterval = true;
                    recomputeDozeOverrideLocked(/* dozeOnly= */ true);
                }
                break;
            case AMBIENT_STATE_CHANGED:
                if (mDisplayOffloadIsControlling || mDisplayOffloadReady) {
                    // Only listen to ambient early stop during offload.
                    if (mAmbientModeTracker.isAmbientEarlyStop()) {
                        recomputeDozeOverrideLocked(false);
                    }
                }
                break;
            case UPDATE_LOCALE:
                if (mDisplayOffloadIsControlling || mDisplayOffloadReady) {
                    wakeUp("Locale changed");
                }
                // Delay here to ensure we exited offload
                mainHandler.post(
                        () -> {
                            LocaleHelper.onLocaleChanged(getContext());
                            synchronized (mDisplayOffloadStateLock) {
                                mHalAdapter.setLocaleConfig();
                            }
                        });
                break;
            case HAL_CONNECTED:
                mBrightnessOffloadController.onHalRestart();
                clearCurrentLayoutFromMCULocked();
                mCurrentLayoutType = NONE;
                updateDisplayOffloadStateLocked(UPDATE_LOCALE);
                updateLayoutLocked();
                break;
            case HAL_DIED:
                if (mDisplayOffloadIsControlling) {
                    mDisplayOffloadReady = false;
                    recomputeDozeOverrideLocked(false);
                    // TODO(b/254357821): Investigate if wake to doze is enough here.
                    wakeUp("HAL died when display is in control.");
                }
                break;
            default:
                break;
        }
    }

    private void runHalOperationsWaitingDozeOrOn() {
        synchronized (mDisplayOffloadStateLock) {
            final int displayState = mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY).getState();
            if (!isDisplayDozeOrOn(displayState)) {
                Log.w(TAG, "runHalOperationsWaitingDozeOrOn: still not in DOZE or ON,"
                        + " keep waiting, state=" + displayState);
                mHalOperationWaitingForDozeOrOn = true;
                return;
            }
            mHalOperationWaitingForDozeOrOn = false;

            if (mUpdateOnNextDoze) {
                Log.i(TAG, "runHalOperationsWaitingDozeOrOn: updateLayout");
                updateLayoutLocked();
            }

            if (mOffloadUpdateDozeIntervalRequested) {
                Log.i(TAG, "runHalOperationsWaitingDozeOrOn: offload update doze interval");
                mOffloadUpdateDozeIntervalRequested = false;
                mainHandler.removeCallbacks(mRecomputeDozeOverride);
                // Increment wake lock reference count to ensure wake lock overlapping
                mOffloadUpdateIntervalWakeLock.acquire();
                mainHandler.postDelayed(mRecomputeDozeOverride, mOnUpdateDozeReleaseInterval);
                // Decrement wake lock reference count when mRecomputeDozeOverride ran to
                // completion
                mainHandler.postDelayed(mOffloadUpdateIntervalWakeLock::release,
                        mOnUpdateDozeReleaseInterval);
            }
        }
    }

    /**
     * update offload layout sent to MCU. Two cases:
     * 1. Still in MCU:  Wait for wake up.
     * 2. Already in AP: Apply update logic.
     */
    @GuardedBy("mDisplayOffloadStateLock")
    private void updateLayoutLocked() {
        if (mDisplayOffloadIsControlling) {
            mUpdateOnNextDoze = true;
            recomputeDozeOverrideLocked(true);
        } else {
            mUpdateOnNextDoze = false;
            updateLayoutLockedNotOffloading();
        }
    }

    /**
     * Recompute layout type & send to MCU.
     */
    @GuardedBy("mDisplayOffloadStateLock")
    private void updateLayoutLockedNotOffloading() {
        OffloadLayout currentLayout = getLayoutByTypeLocked(mCurrentLayoutType);
        // Recompute the type of next layout that will take over the offload screen.
        int nextLayoutType = findNextLayoutTypeLocked();

        if (nextLayoutType != mCurrentLayoutType && currentLayout != null) {
            // Next layout type is different from current one(Non-None).
            // 1. Mark current layout dirty so next sending of this type can override.
            currentLayout.markHalResourcesDirty();
        }
        if (nextLayoutType == NONE) {
            Log.i(TAG, "updateLayoutLockedNotOffloading: next layout is NONE, so do clear.");
            clearCurrentLayoutFromMCULocked();
            mCurrentLayoutType = NONE;
            return;
        }
        OffloadLayout nextLayout = getLayoutByTypeLocked(nextLayoutType);

        updateDisplayOffloadState(BEGIN_SEND_LAYOUT);
        if (nextLayoutType != mCurrentLayoutType) {
            // 2. Clear current layout from MCU.
            clearCurrentLayoutFromMCULocked();
            // 3. Send next layout to MCU.
            if (nextLayoutType != NONE) {
                sendLayoutLocked(nextLayout);
            }
            mCurrentLayoutType = nextLayoutType;
        } else if (nextLayoutType != NONE) {
            // Next layout type is the same as current one.
            // 1. Directly send next layout MCU. Since current layout has already been marked dirty,
            //    the next layout will override.
            sendLayoutLocked(nextLayout);
        }
        // If next layout is a NONE, do nothing.
        updateDisplayOffloadState(END_SEND_LAYOUT);
    }

    /**
     * Recompute which type of layout should take over based on current context.
     *
     * @return a {@link LayoutType}.
     */
    @GuardedBy("mDisplayOffloadStateLock")
    private int findNextLayoutTypeLocked() {
        int nextLayoutType;
        if (mSystemUILayout != null) {
            nextLayoutType = SYSTEMUI;
        } else if (mWatchFaceLayout != null && canShowWatchFaceLayout()) {
            nextLayoutType = WATCHFACE;
        } else if (mActivityLayout != null && mLastResumedActivityUid == mActivityLayout.getUid()) {
            nextLayoutType = ACTIVITY;
        } else {
            nextLayoutType = NONE;
        }
        if (nextLayoutType != mCurrentLayoutType) {
            Log.d(TAG, "findNextLayoutTypeLocked: " +
                    Utils.nameForLayoutType(mCurrentLayoutType) + " -> " +
                    Utils.nameForLayoutType(nextLayoutType));
        }
        return nextLayoutType;
    }

    /**
     * Based the given {@code type}, return corresponding cached OffloadLayout.
     *
     * @param type one of {@link LayoutType}.
     * @return the cached {@link OffloadLayout} corresponding to {@code type}.
     */
    @GuardedBy("mDisplayOffloadStateLock")
    private OffloadLayout getLayoutByTypeLocked(int type) {
        switch (type) {
            case SYSTEMUI:
                return mSystemUILayout;
            case WATCHFACE:
                return mWatchFaceLayout;
            case ACTIVITY:
                return mActivityLayout;
            case NONE:
                return null;
        }
        return null;
    }

    /**
     * Clear current layout from MCU, but not the cached offload layout in this class.
     */
    @GuardedBy("mDisplayOffloadStateLock")
    private void clearCurrentLayoutFromMCULocked() {
        Log.d(TAG, "clearCurrentLayoutLocked: clearing current layout from MCU.");
        mDisplayOffloadReady = false;
        mHalAdapter.resetResource();
    }

    @GuardedBy("mDisplayOffloadStateLock")
    private void sendLayoutLocked(OffloadLayout layout) {
        if (DEBUG) {
            Log.d(TAG, "sendLayout: " + layout);
        }

        try {
            layout.validateHalResource();
        } catch (DisplayOffloadException e) {
            Log.e(TAG, "sendLayout: validateHalResource error, type=" + e.getErrorType(), e);
            postSendLayoutCallback(layout.getUid(), e);
            return;
        }

        if (mDisplayOffloadIsControlling) {
            // Can't send yet
            Log.w(TAG, "sendLayout: display still offloaded? "
                    + "This should not happen, mark waiting doze & skip.");
            layout.markWaitingDoze();
            return;
        }

        try {
            if (layout.isEmpty()) {
                Log.i(TAG, "sendLayout: layout is empty, skip sendToHal.");
            } else {
                layout.sendToHal(mHalAdapter);
            }
        } catch (DisplayOffloadException e) {
            Log.e(TAG, "sendLayout: sendToHal error, type=" + e.getErrorType(), e);
            postSendLayoutCallback(layout.getUid(), e);
            return;
        }

        postSendLayoutCallback(layout.getUid(), null);
    }

    private void invokeOffloadPreStartEndCallback(final boolean isStart, Bundle dataSnapshotOnEnd) {
        final int uid = getCurrentLayoutUid();
        if (uid < 0) {
            Log.e(TAG, "No active layout. Don't invoke pre-start/end callback.");
            return;
        }
        mClientCallbackManager.invokeCallbackRunnable(
                uid,
                isStart ? IDisplayOffloadCallbacks::onPreOffloadStart
                        : (x) -> x.onPreOffloadEnd(dataSnapshotOnEnd));
    }

    private void postOffloadStartEndCallback(final boolean isStart) {
        final int uid = getCurrentLayoutUid();
        if (uid < 0) {
            Log.e(TAG, "No active layout. Don't post start/end callback.");
            return;
        }
        mClientCallbackManager.postCallbackRunnable(
                uid,
                isStart ? IDisplayOffloadCallbacks::onOffloadStart
                        : IDisplayOffloadCallbacks::onOffloadEnd);
    }

    private void postSendLayoutCallback(int uid, final DisplayOffloadException exception) {
        mClientCallbackManager.postCallbackRunnable(
                uid,
                (exception == null)
                        ? IDisplayOffloadCallbacks::onSendLayoutSuccess
                        : (callbacks) -> callbacks.onSendLayoutFailure(exception.mErrorBundle)
        );
    }

    private void postOnOffloadUpdateCallback() {
        final int uid = getCurrentLayoutUid();
        if (uid < 0) {
            Log.e(TAG, "No active layout. Don't post update callback.");
            return;
        }

        if (mOnUpdateDozeReleaseInterval > 0) {
            updateDisplayOffloadState(OFFLOAD_UPDATE_DOZE_INTERVAL);
        }

        mClientCallbackManager.postCallbackRunnable(uid, IDisplayOffloadCallbacks::onOffloadUpdate);

        synchronized (mDisplayOffloadStateLock) {
            maybeScheduleOffloadUpdateLocked();
        }
    }

    /** Tells Display Offload to start controlling the display, if it has a valid watch face. */
    @GuardedBy("mDisplayOffloadStateLock")
    private boolean beginDisplayLocked() throws RemoteException {
        Log.i(TAG, "beginDisplayLocked()");
        if (mDisplayOffloadIsControlling) {
            Log.d(TAG, "...but Offload is already in control; doing nothing.");
            return true;
        }
        if (mCurrentLayoutType == NONE) {
            Log.d(TAG, "...but we have no layout to offload; doing nothing");
            return false;
        }
        if (mCurrentLayoutType == WATCHFACE && !canShowWatchFaceLayout()) {
            Log.d(TAG, "...but we should not show the watchface; doing nothing");
            return false;
        }

        // We don't need to check for ACTIVITY because if we have an activity then it is guaranteed
        // to be the last resumed and we should use it

        if (mHalAdapter.isHalConnected()) {
            int result = Utils.resultFromHALStatus(mHalAdapter.beginDisplay());
            Log.d(TAG, "...beginDisplay result: " + result);

            result = Utils.resultFromHALStatus(mHalAdapter.beginRendering());
            Log.d(TAG, "...beginRendering result: " + result);

            if (result != 1) {
                Log.w(TAG, "...beginRendering is not OK. Status: " + result);
                return false;
            }
        } else {
            Log.w(TAG, "...beginRendering skipped due to no HAL connected");
            return false;
        }

        setDisplayOffloadControlLocked(true);
        return true;
    }

    @GuardedBy("mDisplayOffloadStateLock")
    private void endMcuRenderingLocked() throws RemoteException {
        // Not in control
        if (!mDisplayOffloadIsControlling) {
            Log.d(TAG, "Skipping endMcuRenderingLocked - not currently in control.");
            return;
        }
        if (mHalAdapter.isRenderControlSupported()) {
            if (mHalAdapter.isHalConnected()) {
                mHalAdapter.endRendering();
                Log.d(TAG, "...endRendering returns void");
            } else {
                Log.w(TAG, "...endRendering skipped due to no HAL connected");
            }
        }
    }

    /** Display Offload must stop controlling the display no matter what. */
    @GuardedBy("mDisplayOffloadStateLock")
    private void endDisplayLocked() throws RemoteException {
        Log.i(TAG, "endDisplayLocked()");
        if (!mDisplayOffloadIsControlling) {
            Log.d(TAG, "Skipping endDisplayLocked - not currently in control.");
            return;
        }
        if (mHalAdapter.isHalConnected()) {
            mHalAdapter.endDisplay(); // This call does not return status
            Log.d(TAG, "...endDisplay returns void");
        } else {
            Log.w(TAG, "...endDisplay skipped due to no HAL connected");
        }

        setDisplayOffloadControlLocked(false);
    }

    @GuardedBy("mDisplayOffloadStateLock")
    private void setDisplayOffloadControlLocked(boolean isControlling) {
        mDisplayOffloadIsControlling = isControlling;
        if (!mDisplayOffloadIsControlling) {
            updateDisplayOffloadStateLocked(RUN_HAL_OPERATIONS_WAITING_DOZE_OR_ON);
        }
        maybeScheduleOffloadUpdateLocked();
    }

    @GuardedBy("mDisplayOffloadStateLock")
    private void maybeScheduleOffloadUpdateLocked() {
        boolean shouldSchedule = mDisplayOffloadIsControlling;
        if (shouldSchedule) {
            OffloadLayout currentLayout = null;
            switch (mCurrentLayoutType) {
                case WATCHFACE:
                    currentLayout = mWatchFaceLayout;
                    break;
                case ACTIVITY:
                    currentLayout = mActivityLayout;
                    break;
                case SYSTEMUI:
                    currentLayout = mSystemUILayout;
                    break;
            }

            if (currentLayout == null) {
                Log.e(TAG, "Can't schedule update with invalid layout, type=" + mCurrentLayoutType);
                return;
            }

            final int toUpdateMinutes = currentLayout.getMinutesValid();
            if (toUpdateMinutes >= 1) {
                long toUpdateMillis = TimeUnit.MINUTES.toMillis(toUpdateMinutes);
                // We use currentTimeMillis to compute the next wakeup time since we want to 1)
                // potentially update the watch face as soon as the time changes 2) allow other
                // components that want to wake up once a minute (like burn in protection) to
                // synchronize their wake ups and save the battery. However, we use elapsedRealtime
                // to schedule the alarm so that setting the time can't prevent ambiactive updates
                // and burn-in protection for extended periods.
                final long nowWall = System.currentTimeMillis();
                final long nowElapsed = SystemClock.elapsedRealtime();
                // And aligned to the minute.
                final long nextWall = nowWall - (nowWall % TimeUnit.MINUTES.toMillis(1))
                        + toUpdateMillis;
                // But we will use real time, so need to set it to the next full minute wall clock.
                final long nextElapsed = nowElapsed + (nextWall - nowWall);
                if (DEBUG_CALLBACK) {
                    Log.d(TAG,
                            "scheduling next alarm, nowWall: " + nowWall + ", nextWall: " + nextWall
                                    + ", nowElapsed: " + nowElapsed + ", nextElapsed: "
                                    + nextElapsed);
                }
                mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextElapsed,
                        "offloadUpdate",
                        mOnAlarmListener, null /* targetHandler */);
            }
        } else {
            mAlarmManager.cancel(mOnAlarmListener);
        }
    }

    @GuardedBy("mDisplayOffloadStateLock")
    private boolean canShowWatchFaceLayout() {
        if (Utils.isSysUiUid(mContext, mLastResumedActivityUid)
                || mWindowManagerInternal.isKeyguardShowingAndNotOccluded()) {
            return mWatchFaceLayout != null;
        }
        return false;
    }

    private void recomputeDozeOverride(boolean dozeOnly) {
        synchronized (mDisplayOffloadStateLock) {
            recomputeDozeOverrideLocked(dozeOnly);
        }
    }

    @GuardedBy("mDisplayOffloadStateLock")
    private void recomputeDozeOverrideLocked(boolean dozeOnly) {
        try {
            mWakeLock.acquire();
            // Make sure we update draw wake lock override and doze override at the same time.
            // This avoids DOZE_SUSPEND -> DOZE -> DOZE_SUSPEND flickering because the state
            // will be stable throughout display handoff.
            // Note: b/74247902 is avoided by manually inserted a DOZE state.

            if (mCurrentLayoutType == NONE
                    || mAmbientModeTracker.isAmbientEarlyStop()
                    || mAmbientModeTracker.isTheaterMode(getContext())
                    || mDisplayControlLockManager.shouldBlock()) {
                if (DEBUG_DOZE) Log.d(TAG, "DOZE override -> No override");
                mBrightnessOffloadController.setBrightnessDozeScreenFactor(1f);
                setDozeScreenStateOverride(Display.STATE_UNKNOWN, /* skipUpdate= */true);
                setDrawWakeLockOverride(false);
            } else if (dozeOnly) {
                if (DEBUG_DOZE) Log.d(TAG, "DOZE override -> DOZE");
                if (mPendingDozeInterval) {
                    boolean isSingleShotUpdated = BurnInProtector.instance().singleShotUpdate();
                    if (isSingleShotUpdated) {
                        // Double up brightness if burn-in protector is enabled.
                        mBrightnessOffloadController.setBrightnessDozeScreenFactor(2f);
                    } else {
                        // Reset brightness if burn-in protector is disabled.
                        mBrightnessOffloadController.setBrightnessDozeScreenFactor(1f);
                    }
                    mPendingDozeInterval = false;
                }
                setDozeScreenStateOverride(Display.STATE_DOZE, /* skipUpdate= */true);
                setDrawWakeLockOverride(false);
            } else if (readyToDisplayLocked()) {
                if (DEBUG_DOZE) Log.d(TAG, "DOZE override -> DOZE_SUSPEND");
                mBrightnessOffloadController.setBrightnessDozeScreenFactor(1f);
                setDozeScreenStateOverride(Display.STATE_DOZE_SUSPEND, /* skipUpdate= */true);
                setDrawWakeLockOverride(true);
            } else {
                if (DEBUG_DOZE) Log.d(TAG, "DOZE override -> No override: not ready to offload");
                mBrightnessOffloadController.setBrightnessDozeScreenFactor(1f);
                setDozeScreenStateOverride(Display.STATE_UNKNOWN, /* skipUpdate= */true);
                setDrawWakeLockOverride(false);
            }
        } finally {
            mainHandler.post(() -> mWakeLock.release());
        }
    }

    private void setDrawWakeLockOverride(boolean enabled) {
        /*
        if (mPowerManagerInternal != null) {
            mPowerManagerInternal.setDrawWakeLockOverride(enabled);
        }
        */
    }

    private void setDozeScreenStateOverride(int state, boolean skipUpdate) {
        /*
        if (mPowerManagerInternal != null) {
            mPowerManagerInternal.setDozeOverrideFromDisplayOffload(
                    state != Display.STATE_UNKNOWN, state, skipUpdate);
        }
        */
    }

    private void refreshLastResumedActivityUid() {
        try {
            ActivityTaskManager.RootTaskInfo stackInfo =
                    mActivityTaskManagerService.getFocusedRootTaskInfo();
            ComponentName topActivity = stackInfo == null ? null : stackInfo.topActivity;
            if (topActivity == null) {
                return;
            }
            ApplicationInfo info = mPackageManager.getApplicationInfo(topActivity.getPackageName(),
                    PackageManager.GET_META_DATA);
            if (DEBUG_UID) {
                Log.d(TAG, "refreshLastResumedActivityUid: " + mLastResumedActivityUid + " -> "
                        + info.uid);
            }
            mLastResumedActivityUid = info.uid;
        } catch (RemoteException | PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to get top Activity UID");
        }
    }

    @GuardedBy("mDisplayOffloadStateLock")
    private void beginEndClearResourceHelperLocked(
            int resourceType, int uid, boolean isBegin, boolean isClear, boolean replace) {
        refreshLastResumedActivityUid();
        switch (resourceType) {
            case RESOURCE_TYPE_ACTIVITY:
                if (uid != mLastResumedActivityUid) {
                    Log.e(
                            TAG,
                            "Ignoring "
                                    + (isBegin ? "begin" : "end")
                                    + " Activity from "
                                    + uid
                                    + " in favor of "
                                    + mLastResumedActivityUid);
                } else if (isClear) {
                    // clearResource
                    mActivityLayout = null;
                    updateLayoutLocked();
                } else {
                    // begin/endResource
                    if (isBegin && (replace || mActivityLayout == null
                            || mActivityLayout.getUid() != uid)) {
                        mActivityLayout = new OffloadLayout(uid, mContext, mHalAdapter);
                    }
                    updateDisplayOffloadState(
                            isBegin ? BEGIN_RECEIVE_ACTIVITY : END_RECEIVE_ACTIVITY);
                }
                break;
            case RESOURCE_TYPE_WATCHFACE:
                if (uid != mCurrentWatchFaceUid && mCurrentWatchFaceUid != -1) {
                    Log.e(
                            TAG,
                            "Ignoring "
                                    + (isBegin ? "begin" : "end")
                                    + " Watchface from "
                                    + uid
                                    + " in favor of "
                                    + mCurrentWatchFaceUid);
                } else if (isClear) {
                    // clearResource
                    mWatchFaceLayout = null;
                    updateLayoutLocked();
                } else {
                    // begin/endResource
                    if (isBegin && (replace || mWatchFaceLayout == null
                            || mWatchFaceLayout.getUid() != uid)) {
                        if (mCurrentWatchFaceUid == -1) {
                            mCurrentWatchFaceUid = uid;
                        }
                        mWatchFaceLayout = new OffloadLayout(uid, mContext, mHalAdapter);
                    }
                    updateDisplayOffloadState(
                            isBegin ? BEGIN_RECEIVE_WATCHFACE : END_RECEIVE_WATCHFACE);
                }
                break;
            case RESOURCE_TYPE_SYSTEMUI:
                if (uid != mSystemUiUid) {
                    Log.e(
                            TAG,
                            "Ignoring "
                                    + (isBegin ? "begin" : "end")
                                    + " SystemUI from "
                                    + uid
                                    + " in favor of "
                                    + mSystemUiUid);
                } else if (isClear) {
                    // clearResource
                    mSystemUILayout = null;
                    updateLayoutLocked();
                } else {
                    // begin/endResource
                    if (isBegin && (replace || mSystemUILayout == null
                            || mSystemUILayout.getUid() != uid)) {
                        mSystemUILayout = new OffloadLayout(uid, mContext, mHalAdapter);
                    }
                    updateDisplayOffloadState(
                            isBegin ? BEGIN_RECEIVE_SYSTEMUI : END_RECEIVE_SYSTEMUI);
                }
                break;
            default:
                Log.e(
                        TAG,
                        "Unsupported resource category. Wrong type?"
                                + " resourceType: "
                                + resourceType
                                + " from uid: "
                                + uid);
                break;
        }
    }

    @GuardedBy("mDisplayOffloadStateLock")
    private void unpackResourcesLocked(int resourceType, int uid, Bundle bundle) {
        OffloadLayout offloadLayout = null;
        int targetUid = -1;
        switch (resourceType) {
            case RESOURCE_TYPE_ACTIVITY:
                targetUid = mLastResumedActivityUid;
                offloadLayout = mActivityLayout;
                break;
            case RESOURCE_TYPE_WATCHFACE:
                targetUid = mCurrentWatchFaceUid;
                offloadLayout = mWatchFaceLayout;
                break;
            case RESOURCE_TYPE_SYSTEMUI:
                targetUid = mSystemUiUid;
                offloadLayout = mSystemUILayout;
                break;
            default:
                break;
        }

        if (uid != targetUid) {
            Log.e(
                    TAG,
                    "unpackResourcesLocked: "
                            + "Ignoring resource from "
                            + uid
                            + " in favor of "
                            + targetUid);
            return;
        }

        if (offloadLayout == null) {
            Log.e(
                    TAG,
                    "unpackResourcesLocked: No offload layout? This should not happen."
                            + " targetUid="
                            + targetUid);
            return;
        }

        if (offloadLayout.isOpen()) {
            offloadLayout.updateFromBundle(bundle);
        } else {
            Log.e(TAG, "unpackResourcesLocked: offload layout not open for change");
        }
    }

    @GuardedBy("mDisplayOffloadStateLock")
    private void updateStatusBarLocked() {
        if (mWatchFaceLayout != null && mStatusBarGroup != null && mStatusBarResource != null) {
            // If we are sending the actual watchface then wait till after for this
            if (!mWatchFaceLayout.isOpen()) {
                mWatchFaceLayout.addStatusBar(mStatusBarGroup, mStatusBarResource);
            }
            if (mDisplayOffloadIsControlling) {
                mWatchFaceLayout.markWaitingDoze();
                if (mCurrentLayoutType == WATCHFACE) {
                    // Only trigger offload update immediately if showing watch face
                    updateDisplayOffloadState(OFFLOAD_UPDATE_DOZE_INTERVAL);
                }
            }
        }
    }

    @GuardedBy("mDisplayOffloadStateLock")
    private boolean readyToDisplayLocked() {
        return mDisplayOffloadReady && mCurrentLayoutType != NONE;
    }

    @GuardedBy("mDisplayOffloadStateLock")
    private void dumpsysLocked(IndentingPrintWriter ipw) {
        ipw.println("DisplayOffloadService");
        ipw.increaseIndent();

        ipw.printPair(
                "HAL Version Connected", String.join(",", mHalAdapter.getConnectedHalVersions()));
        ipw.println();
        ipw.printPair("mCurrentLayoutType", Utils.nameForLayoutType(mCurrentLayoutType));
        ipw.printPair("mCurrentWatchFaceUid", mCurrentWatchFaceUid);
        ipw.printPair("mLastResumedActivityUid", mLastResumedActivityUid);
        ipw.printPair("mSystemUIUid", mSystemUiUid);
        ipw.printPair("mDisplayOffloadReady", mDisplayOffloadReady);
        ipw.println();
        ipw.printPair("mDisplayOffloadIsControlling", mDisplayOffloadIsControlling);
        ipw.printPair("mUpdateOnNextDoze", mUpdateOnNextDoze);
        ipw.println();
        ipw.printPair("readyToDisplay", readyToDisplayLocked());
        ipw.println();

        ipw.println("mSystemUILayout: ");
        ipw.increaseIndent();
        if (mSystemUILayout != null) {
            mSystemUILayout.dump(ipw);
        } else {
            ipw.println("(empty)");
        }
        ipw.decreaseIndent();
        ipw.println();

        ipw.println("mWatchFaceLayout: ");
        ipw.increaseIndent();
        if (mWatchFaceLayout != null) {
            mWatchFaceLayout.dump(ipw);
        } else {
            ipw.println("(empty)");
        }
        ipw.decreaseIndent();
        ipw.println();

        ipw.println("mActivityLayout: ");
        ipw.increaseIndent();
        if (mActivityLayout != null) {
            mActivityLayout.dump(ipw);
        } else {
            ipw.println("(empty)");
        }
        ipw.decreaseIndent();
        ipw.println();

        LocaleHelper.dump(getContext(), ipw);

        ipw.decreaseIndent();
    }

    final class BinderService extends Stub {
        @Override
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(mContext, TAG, pw)) return;

            if (Build.IS_DEBUGGABLE) {
                int res = mShell.exec(this, null, fd, null, args, null, new ResultReceiver(null));
                if (res == 0) return;
            }

            final long ident = Binder.clearCallingIdentity();
            try {
                final IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
                synchronized (mDisplayOffloadStateLock) {
                    dumpsysLocked(ipw);
                }
            } catch (Throwable throwable) {
                pw.println("caught exception while dumping " + throwable.getMessage());
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        @Override
        public int getApiVersionPreference() {
            if (mHalAdapter.getConnectedHalVersions().contains("V2.0")) {
                return 2;
            }
            return 1;
        }

        @Override
        public void attachCallbacks(IDisplayOffloadCallbacks iDisplayOffloadCallbacks) {
            if (iDisplayOffloadCallbacks == null) {
                return;
            }

            final int uid = Binder.getCallingUid();
            final long ident = Binder.clearCallingIdentity();
            try {
                synchronized (mDisplayOffloadStateLock) {
                    mClientCallbackManager.attachCallback(uid, iDisplayOffloadCallbacks);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        @Override
        public void detachCallbacks() {
            final int uid = Binder.getCallingUid();
            final long ident = Binder.clearCallingIdentity();
            try {
                synchronized (mDisplayOffloadStateLock) {
                    mClientCallbackManager.detachCallback(uid);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        @Override
        public void beginResource(int resourceType, boolean clearCache) {
            int uid = Binder.getCallingUid();
            final long ident = Binder.clearCallingIdentity();
            try {
                synchronized (mDisplayOffloadStateLock) {
                    beginEndClearResourceHelperLocked(
                            resourceType, uid,
                            /* isBegin= */ true,
                            /* isClear= */ false,
                            /* replace= */ clearCache);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        @Override
        public void endResource(int resourceType) {
            int uid = Binder.getCallingUid();
            final long ident = Binder.clearCallingIdentity();
            try {
                synchronized (mDisplayOffloadStateLock) {
                    beginEndClearResourceHelperLocked(
                            resourceType, uid,
                            /* isBegin= */ false,
                            /* isClear= */ false,
                            /* replace= */ false);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        @Override
        public void clearResource(int resourceType) {
            // TODO(b/179528367): check if a lock is required here.
            int uid = Binder.getCallingUid();
            final long ident = Binder.clearCallingIdentity();
            try {
                synchronized (mDisplayOffloadStateLock) {
                    beginEndClearResourceHelperLocked(
                            resourceType, uid,
                            /* isBegin= */ false,
                            /* isClear= */ true,
                            /* replace= */ false);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        @Override
        public void sendResource(int resourceType, Bundle bundle) {
            // TODO(b/179528367): check if a lock is required here.
            int uid = Binder.getCallingUid();
            final long ident = Binder.clearCallingIdentity();
            try {
                synchronized (mDisplayOffloadStateLock) {
                    unpackResourcesLocked(resourceType, uid, bundle);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        @Override
        public void sendStatusBar(Icon statusBar, RectF bounds) {
            final long ident = Binder.clearCallingIdentity();
            try {
                synchronized (mDisplayOffloadStateLock) {
                    mStatusBarResource = Utils.createStatusBarBitmap(statusBar, bounds);
                    mStatusBarGroup = Utils.createStatusBarTranslationGroup(bounds);

                    if (mStatusBarResource.icon.getType() == Icon.TYPE_DATA) {
                        Log.d(TAG, "RECEIVED STATUS BAR: "
                                + mStatusBarResource.icon.getDataLength());
                    } else {
                        Log.d(TAG, "RECEIVED STATUS BAR.");
                    }

                    updateStatusBarLocked();
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        @Override
        public boolean readyToDisplay() throws RemoteException {
            synchronized (mDisplayOffloadStateLock) {
                return readyToDisplayLocked();
            }
        }

        @Override
        public boolean acquireDisplayControlLock(IBinder token, String name) {
            final long ident = Binder.clearCallingIdentity();
            boolean result;
            try {
                result = mDisplayControlLockManager.acquire(token, name);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
            return result;
        }

        @Override
        public void releaseDisplayControlLock(IBinder token) {
            final long ident = Binder.clearCallingIdentity();
            try {
                mDisplayControlLockManager.release(token);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    // private final class LocalService extends DisplayOffloadInternal {
    private final class LocalService {

        // @Override
        public boolean startDisplayControl(int displayState) {
            if (!(mCurrentLayoutType == WATCHFACE &&
                    mWatchFaceLayout != null &&
                    mWatchFaceLayout.isBrightnessOffload())) {
                // Skip if current layout is watch face and using brightness offload
                invokeOffloadPreStartEndCallback(true, null);
            }
            updateDisplayOffloadState(START_DISPLAY_CONTROL);
            postOffloadStartEndCallback(/* isStart= */ true);
            return mDisplayOffloadIsControlling;
        }

        // @Override
        public void endDisplayControl() {
            updateDisplayOffloadState(END_DISPLAY_MCU_RENDERING);
            if (!(mCurrentLayoutType == WATCHFACE &&
                    mWatchFaceLayout != null &&
                    mWatchFaceLayout.isBrightnessOffload())) {
                // Skip if current layout is watch face and using brightness offload
                if (mHalAdapter.isRenderControlSupported()) {
                    Bundle bundle = null;
                    try {
                        bundle = mHalAdapter.fetchDataSnapshot();
                    } catch (RemoteException e) {
                        Log.e(TAG, "Error talking to HAL", e);
                    }
                    invokeOffloadPreStartEndCallback(false, bundle);
                }
            }
            updateDisplayOffloadState(END_DISPLAY_CONTROL);
            postOffloadStartEndCallback(/* isStart= */ false);
        }

        // @Override
        public void onActivityResumed(int uid) {
            if (DebugUtils.DEBUG_UID) {
                Log.d(TAG, "onActivityResumed: uid = " + uid + "; mLastResumedActivityUid = "
                        + mLastResumedActivityUid);
            }
            if (uid != mLastResumedActivityUid) {
                mLastResumedActivityUid = uid;
                mainHandler.post(() -> updateDisplayOffloadState(ACTIVITY_CHANGED));
            }
        }
    }
}
