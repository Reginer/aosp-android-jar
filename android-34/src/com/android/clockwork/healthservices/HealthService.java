package com.android.clockwork.healthservices;

import static com.android.clockwork.healthservices.Utils.DEBUG_HAL;
import static com.google.android.clockwork.healthservices.IHealthService.NAME;
import static com.google.android.clockwork.healthservices.IHealthService.Stub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.SystemService;

import com.google.android.clockwork.healthservices.types.AchievedGoal;
import com.google.android.clockwork.healthservices.types.AutoExerciseEvent;
import com.google.android.clockwork.healthservices.types.AutoStartConfig;
import com.google.android.clockwork.healthservices.types.AutoStartCapabilities;
import com.google.android.clockwork.healthservices.types.AutoStartEvent;
import com.google.android.clockwork.healthservices.types.AvailabilityUpdate;
import com.google.android.clockwork.healthservices.types.DataTypeGoal;
import com.google.android.clockwork.healthservices.types.DataTypeOffset;
import com.google.android.clockwork.healthservices.types.DataUpdate;
import com.google.android.clockwork.healthservices.types.ExerciseEvent;
import com.google.android.clockwork.healthservices.types.GolfShotDetectionParams;
import com.google.android.clockwork.healthservices.types.HealthEvent;
import com.google.android.clockwork.healthservices.types.HrAlertParams;
import com.google.android.clockwork.healthservices.IHealthServiceCallback;
import com.google.android.clockwork.healthservices.types.TrackingConfig;

import java.util.Arrays;
import java.util.ArrayList;

import vendor.google_clockwork.healthservices.V1_0.IHealthServices;

/** A {@link SystemService} that binds to the Wear Health Services application (WHS). */
public class HealthService extends SystemService
    implements IBinder.DeathRecipient, HalAdapter.HalListener {
  private static final String TAG = "HealthService";
  public static final String SERVICE_NAME = NAME;

  static final String WHS_PACKAGE = "com.google.android.wearable.healthservices";

  @VisibleForTesting static final int BOOT_BIND_DELAY_MILLIS = 4000;
  @VisibleForTesting static final int WHS_CRASH_BIND_DELAY_MILLIS = 4000;
  @VisibleForTesting static final int PACKAGE_UPDATE_BIND_DELAY_MILLIS = 4000;
  @VisibleForTesting static final int USER_UNLOCK_BIND_DELAY_MILLIS = 3000;

  /** Interface that handles binding to WHS. */
  public static interface BindingAgent {

    /** Attempts binding to WHS after {@code delayMillis} ms. */
    void bind(long delayMillis);

    /** Cancel pending bind attempts. */
    void cancelPendingBinds();
  }

  @VisibleForTesting
  final PackageEventsReceiver mWhsPackageEventsReceiver = new PackageEventsReceiver();

  private final Context mContext;

  private HandlerBindingAgent mWhsBindingAgent;
  private ConnectionTracker mWhsConnectionTracker;

  private final HalAdapter mHalAdapter;

  private final Object mHealthServicesCallbackLock = new Object();

  /**
   * The ID to use for the next incoming flush request. The SystemService is always expected to be
   * running so this should be an increasing count from boot time. It is okay if it resets after a
   * reboot scenario or due to reaching MAX_INTEGER since earlier flushes will most likely have been
   * completed at that point.
   */
  @VisibleForTesting public int nextFlushId = Utils.STARTING_FLUSH_ID;

  @GuardedBy("mHealthServicesCallbackLock")
  private IHealthServiceCallback mHealthServiceCallback;

  @VisibleForTesting
  public HealthService(
      Context context,
      HandlerBindingAgent bindingAgent,
      ConnectionTracker connectionTracker,
      HalAdapter halAdapter) {
    super(context);
    mContext = context;
    mWhsBindingAgent = bindingAgent;
    mWhsConnectionTracker = connectionTracker;
    mHalAdapter = halAdapter;
  }

  public HealthService(Context context) {
    super(context);
    mContext = context;
    mHalAdapter = new HalAdapter(this);
  }

  @Override // SystemService
  public void onStart() {
    publishBinderService();
    if (mWhsBindingAgent == null) {
      mWhsBindingAgent = new HandlerBindingAgent(mContext);
    }
    if (mWhsConnectionTracker == null) {
      mWhsConnectionTracker = new ConnectionTracker(/* deathRecipient= */ this);
    }

    mWhsBindingAgent.setConnectionTracker(mWhsConnectionTracker);
    mWhsConnectionTracker.setBindingAgent(mWhsBindingAgent);

    IntentFilter packageActionsFilter = new IntentFilter();
    packageActionsFilter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
    packageActionsFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
    packageActionsFilter.addDataScheme("package");
    mContext.registerReceiver(mWhsPackageEventsReceiver, packageActionsFilter);

    mHalAdapter.registerHalV1_0RegistrationNotification();
    mHalAdapter.maybeGetAidlHalService();
  }

  // Allow the unit tests a way to mask this method due to problems testing ServiceManager.
  @VisibleForTesting
  void publishBinderService() {
    publishBinderService(SERVICE_NAME, new BinderService());
  }

  @Override // SystemService
  public void onUserUnlocked(TargetUser user) {
    if (mWhsConnectionTracker.isConnected()) {
      Log.w(TAG, "User unlocked the device, but WHS is already connected.");
      return;
    }
    Log.d(TAG, "User unlocked. Will attempt a bind after " + USER_UNLOCK_BIND_DELAY_MILLIS + "ms.");
    mWhsBindingAgent.bind(USER_UNLOCK_BIND_DELAY_MILLIS);
  }

  @Override // SystemService
  public void onBootPhase(int phase) {
    if (phase == SystemService.PHASE_BOOT_COMPLETED) {
      Log.d(TAG, "Boot complete. Will attempt to bind after " + BOOT_BIND_DELAY_MILLIS + "ms.");
      mWhsBindingAgent.bind(BOOT_BIND_DELAY_MILLIS);
    }
  }

  @Override // IBinder.DeathRecipient
  public void binderDied() {
    Log.d(TAG, "WHS died. Will attempt to bind after " + WHS_CRASH_BIND_DELAY_MILLIS + "ms.");
    mWhsBindingAgent.bind(WHS_CRASH_BIND_DELAY_MILLIS);
  }

  @Override // HalAdapter.HalListener
  public void onHalConnected() {
    synchronized (mHealthServicesCallbackLock) {
      if (mHealthServiceCallback != null) {
        try {
          mHealthServiceCallback.onHalServiceConnected();
        } catch (RemoteException e) {
          Log.w(TAG, "Failed to notify WHS of HAL connection:", e);
        }
      } else {
        Log.w(TAG, "No callback registered! Failed to notify WHS of HAL connection");
      }
    }
  }

  @Override // HalAdapter.HalListener
  public void onHalDied() {
    synchronized (mHealthServicesCallbackLock) {
      if (mHealthServiceCallback != null) {
        try {
          mHealthServiceCallback.onHalServiceDied();
        } catch (RemoteException e) {
          Log.w(TAG, "Failed to notify WHS of HAL crash:", e);
        }
      } else {
        Log.w(TAG, "No callback registered! Failed to notify WHS of HAL Death");
      }
    }
  }

  @Override // HalAdapter.HalListener
  public void onAutoExerciseEvent(AutoExerciseEvent autoExerciseEvent) {
    if (DEBUG_HAL) {
      Log.d(TAG, "onAutoExerciseEvent");
    }
    try {
      synchronized (mHealthServicesCallbackLock) {
        if (mHealthServiceCallback == null) {
          Log.w(TAG, "No callback registered! Failed to emit auto-exercise Event");
          return;
        }
        mHealthServiceCallback.onAutoExerciseEvent(autoExerciseEvent);
      }
    } catch (RemoteException e) {
      Log.w(TAG, "Failed to emit auto-exercise event:", e);
    }
  }

  @Override // HalAdapter.HalListener
  public void onExerciseEvent(ExerciseEvent[] exerciseEvents) {
    if (DEBUG_HAL) {
      Log.d(TAG, "onExerciseEvent");
    }
    try {
      synchronized (mHealthServicesCallbackLock) {
        if (mHealthServiceCallback == null) {
          Log.w(TAG, "No callback registered! Failed to emit exercise Event");
          return;
        }
        mHealthServiceCallback.onExerciseEvent(exerciseEvents);
      }
    } catch (RemoteException e) {
      Log.w(TAG, "Failed to emit exercise event:", e);
    }
  }

  @Override // HalAdapter.HalListener
  public void onAvailabilityUpdate(AvailabilityUpdate[] availabilityUpdates) {
    if (DEBUG_HAL) {
      Log.d(TAG, "onAvailabilityUpdate");
    }
    try {
      synchronized (mHealthServicesCallbackLock) {
        if (mHealthServiceCallback == null) {
          Log.w(TAG, "No callback registered! Failed to emit availability update");
          return;
        }
        mHealthServiceCallback.onAvailabilityUpdate(availabilityUpdates);
      }
    } catch (RemoteException e) {
      Log.w(TAG, "Failed to emit availability update:", e);
    }
  }

  @Override // HalAdapter.HalListener
  public void onDataUpdate(DataUpdate[] dataUpdates) {
    if (DEBUG_HAL) {
      Log.d(TAG, "onDataUpdate");
    }
    try {
      synchronized (mHealthServicesCallbackLock) {
        if (mHealthServiceCallback == null) {
          Log.w(TAG, "No callback registered! Failed to emit data updates");
          return;
        }
        mHealthServiceCallback.onDataUpdate(dataUpdates);
      }
    } catch (RemoteException e) {
      Log.w(TAG, "Failed to emit data updates:", e);
    }
  }

  @Override // HalAdapter.HalListener
  public void onFlushCompleted(int flushId) {
    if (DEBUG_HAL) {
      Log.d(TAG, "onFlushCompleted");
    }
    try {
      synchronized (mHealthServicesCallbackLock) {
        if (mHealthServiceCallback == null) {
          Log.w(TAG, "No callback registered! Failed to emit flush complete signal");
          return;
        }
        mHealthServiceCallback.onFlushCompletedWithId(flushId);
      }
    } catch (RemoteException e) {
      Log.w(TAG, "Failed to emit flush complete signal:", e);
    }
  }

  @Override // HalAdapter.HalListener
  public void onGoalAchieved(AchievedGoal[] achievedGoals) {
    if (DEBUG_HAL) {
      Log.d(TAG, "onGoalAchieved");
    }
    try {
      synchronized (mHealthServicesCallbackLock) {
        if (mHealthServiceCallback == null) {
          Log.w(TAG, "No callback registered! Failed to emit achieved goal");
          return;
        }
        mHealthServiceCallback.onGoalAchieved(achievedGoals);
      }
    } catch (RemoteException e) {
      Log.w(TAG, "Failed to emit achieved goal:", e);
    }
  }

  @Override // HalAdapter.HalListener
  public void onHealthEventDetected(HealthEvent healthEvent) {
    if (DEBUG_HAL) {
      Log.d(TAG, "onHealthEventDetected");
    }
    try {
      synchronized (mHealthServicesCallbackLock) {
        if (mHealthServiceCallback == null) {
          Log.w(TAG, "No callback registered! Failed to emit Health Event");
          return;
        }
        mHealthServiceCallback.onHealthEventDetected(healthEvent);
      }
    } catch (RemoteException e) {
      Log.w(TAG, "Failed to emit health event:", e);
    }
  }

  @VisibleForTesting
  final class PackageEventsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (!Intent.ACTION_PACKAGE_RESTARTED.equals(action)
          && !Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
        return;
      }

      String packageFromEvent =
          mContext.getPackageManager().getNameForUid(intent.getIntExtra(Intent.EXTRA_UID, 0));
      if (!WHS_PACKAGE.equals(packageFromEvent)) {
        return;
      }

      if (mWhsConnectionTracker.isConnected()) {
        Log.w(TAG, "WHS package update, but is already connected.");
        return;
      }

      Log.d(
          TAG,
          "WHS package updated. Will attempt to bind after "
              + PACKAGE_UPDATE_BIND_DELAY_MILLIS
              + "ms.");
      mWhsBindingAgent.bind(PACKAGE_UPDATE_BIND_DELAY_MILLIS);
    }
  }

  final class BinderService extends Stub {

    @Override
    public int[] getSupportedDataTypes() {
      if (DEBUG_HAL) {
        Log.d(TAG, String.format("getSupportedDataTypes()"));
      }

      return mHalAdapter.getSupportedDataTypes();
    }

    @Override
    public DataTypeGoal[] getSupportedGoals() {
      if (DEBUG_HAL) {
        Log.d(TAG, String.format("getSupportedGoals()"));
      }

      return mHalAdapter.getSupportedGoals();
    }

    @Override
    public int[] getAutoPauseAndResumeEnabledExerciseTypes() {
      if (DEBUG_HAL) {
        Log.d(TAG, String.format("getAutoPauseAndResumeEnabledExerciseTypes()"));
      }

      return mHalAdapter.getAutoPauseAndResumeEnabledExerciseTypes();
    }

    @Override
    public int[] getAutoStopEnabledExerciseTypes() {
      if (DEBUG_HAL) {
        Log.d(TAG, "getAutoStopEnabledExerciseTypes");
      }

      return mHalAdapter.getAutoStopEnabledExerciseTypes();
    }

    /**
     * @return the AutoStartCapabilities.
     */
    @Override
    public AutoStartCapabilities getAutoStartCapabilities() {
      if (DEBUG_HAL) {
        Log.d(TAG, "getAutoStartCapabilities");
      }

      return mHalAdapter.getAutoStartCapabilities();
    }

    @Override
    public void startTracking(
        TrackingConfig config, int[] dataTypes, DataTypeOffset[] offsets, boolean isPaused) {
      if (DEBUG_HAL) {
        Log.d(
            TAG,
            String.format(
                "startTracking: %s %s %s %s",
                config, Arrays.toString(dataTypes), Arrays.toString(offsets), isPaused));
      }
      mHalAdapter.startTracking(config, dataTypes, offsets, isPaused);
    }

    /** Starts automatic exercise detection for the given configs. */
    @Override
    public void startAutomaticExerciseDetection(
        AutoStartConfig[] autoStartConfigs, AutoStartEvent autoStartOffset)
        throws RemoteException {
      if (DEBUG_HAL) {
        Log.d(
            TAG,
            String.format(
                "startAutomaticExerciseDetection: num_configs: %s", autoStartConfigs.length));
      }
      mHalAdapter.startAutomaticExerciseDetection(autoStartConfigs, autoStartOffset);
    }

    /** Stops automatic exercise detection for the given {@link ExerciseTypes}s. */
    @Override
    public void stopAutomaticExerciseDetection(int[] exerciseTypes) throws RemoteException {
      if (DEBUG_HAL) {
        Log.d(
            TAG,
            String.format(
                "stopAutomaticExerciseDetection: num_exerciseTypes: %s", exerciseTypes.length));
      }
      mHalAdapter.stopAutomaticExerciseDetection(exerciseTypes);
    }

    @Override
    public void updateTrackingConfig(TrackingConfig config, int[] dataTypes) {
      if (DEBUG_HAL) {
        Log.d(
            TAG, String.format("updateTrackingConfig: %s %s", config, Arrays.toString(dataTypes)));
      }

      mHalAdapter.updateTrackingConfig(config, dataTypes);
    }

    @Override
    public void pauseTracking(int[] dataTypes) {
      if (DEBUG_HAL) {
        Log.d(TAG, String.format("pauseTracking: %s", Arrays.toString(dataTypes)));
      }

      mHalAdapter.pauseTracking(dataTypes);
    }

    @Override
    public void resumeTracking(int[] dataTypes) {
      if (DEBUG_HAL) {
        Log.d(TAG, String.format("resumeTracking: %s", Arrays.toString(dataTypes)));
      }

      mHalAdapter.resumeTracking(dataTypes);
    }

    @Override
    public void stopTracking(int[] dataTypes) {
      if (DEBUG_HAL) {
        Log.d(TAG, String.format("stopTracking: %s", Arrays.toString(dataTypes)));
      }

      mHalAdapter.stopTracking(dataTypes);
    }

    @Override
    public int flush(int[] dataTypes) {
      if (DEBUG_HAL) {
        Log.d(TAG, String.format("flush: %s", Arrays.toString(dataTypes)));
      }
      // Note: It is assumed that flush requests are made sequentially.
      int flushIdForRequest = nextFlushId;
      nextFlushId = Utils.generateNextFlushId(flushIdForRequest);

      mHalAdapter.flush(flushIdForRequest, dataTypes);
      return flushIdForRequest;
    }

    @Override
    public void resetDataTypeOffsets(DataTypeOffset[] offsets) {
      if (DEBUG_HAL) {
        Log.d(TAG, String.format("resetDataTypeOffsets: %s", Arrays.toString(offsets)));
      }

      mHalAdapter.resetDataTypeOffsets(offsets);
    }

    @Override
    public void setHealthServiceCallback(IHealthServiceCallback callback) {
      if (DEBUG_HAL) {
        Log.d(TAG, "Setting IHealthServiceCallback");
      }
      synchronized (mHealthServicesCallbackLock) {
        if (callback == null) {
          Log.d(TAG, "Health Service Callback is null");
          return;
        }

        mHealthServiceCallback = callback;
      }
    }

    @Override
    public void addGoal(DataTypeGoal goal) {
      if (DEBUG_HAL) {
        Log.d(TAG, String.format("addGoal: %s", goal));
      }

      mHalAdapter.addGoal(goal);
    }

    @Override
    public void removeGoal(DataTypeGoal goal) {
      if (DEBUG_HAL) {
        Log.d(TAG, String.format("removeGoal: %s", goal));
      }

      mHalAdapter.removeGoal(goal);
    }

    @Override
    public void setProfile(float heightCm, float weightKg, int ageYears, byte gender) {
      if (DEBUG_HAL) {
        Log.d(TAG, "setProfile");
      }

      mHalAdapter.setProfile(heightCm, weightKg, ageYears, gender);
    }

    @Override
    public void setSwimmingPoolLength(int lengthMeters) {
      if (DEBUG_HAL) {
        Log.d(TAG, "setSwimmingPoolLength");
      }

      mHalAdapter.setSwimmingPoolLength(lengthMeters);
    }

    @Override
    public void setHeartRateAlertParams(HrAlertParams params) {
      if (DEBUG_HAL) {
        Log.d(TAG, "setHeartRateAlertParams");
      }
      mHalAdapter.setHeartRateAlertParams(params);
    }

    @Override
    public void setGolfShotDetectionParams(GolfShotDetectionParams params) {
      if (DEBUG_HAL) {
        Log.d(TAG, "setGolfShotDetectionParams");
      }

      mHalAdapter.setGolfShotDetectionParams(params);
    }

    @Override
    public void setTestMode(boolean enableTestMode) {
      if (DEBUG_HAL) {
        Log.d(TAG, "setTestMode");
      }

      mHalAdapter.setTestMode(enableTestMode);
    }

    @Override
    public void setOemCustomConfiguration(int configId, byte[] configValue) {
      if (DEBUG_HAL) {
        Log.d(TAG, String.format("setOemCustomConfiguration: %s", configId));
      }

      mHalAdapter.setOemCustomConfiguration(configId, configValue);
    }

    @Override
    public boolean isHalServiceConnected() {
      return mHalAdapter.isHalServiceConnected();
    }
  }
}
