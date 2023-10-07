package com.android.clockwork.healthservices;

import static com.android.clockwork.healthservices.HealthService.BOOT_BIND_DELAY_MILLIS;
import static com.android.clockwork.healthservices.HealthService.PACKAGE_UPDATE_BIND_DELAY_MILLIS;
import static com.android.clockwork.healthservices.HealthService.USER_UNLOCK_BIND_DELAY_MILLIS;
import static com.android.clockwork.healthservices.HealthService.WHS_CRASH_BIND_DELAY_MILLIS;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserManager;

import com.android.clockwork.healthservices.HealthService.BinderService;
import com.android.server.SystemService;
import com.android.server.SystemService.TargetUser;

import com.google.android.clockwork.healthservices.types.AutoExerciseEvent;
import com.google.android.clockwork.healthservices.types.AutoPauseEvent;
import com.google.android.clockwork.healthservices.types.AutoPauseResumeEventType;
import com.google.android.clockwork.healthservices.types.AutoStartConfig;
import com.google.android.clockwork.healthservices.types.AutoStartEvent;
import com.google.android.clockwork.healthservices.types.DataType;
import com.google.android.clockwork.healthservices.types.DataTypeGoal;
import com.google.android.clockwork.healthservices.types.DataTypeOffset;
import com.google.android.clockwork.healthservices.types.ExerciseType;
import com.google.android.clockwork.healthservices.types.GoalThreshold;
import com.google.android.clockwork.healthservices.types.GoalThresholdType;
import com.google.android.clockwork.healthservices.types.OffsetValue;
import com.google.android.clockwork.healthservices.types.OffsetValueType;
import com.google.android.clockwork.healthservices.types.TrackingConfig;
import com.google.android.clockwork.healthservices.IHealthServiceCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;

import vendor.google_clockwork.healthservices.V1_0.IHealthServices;

@RunWith(RobolectricTestRunner.class)
public final class HealthServiceTest {
  private static final int TEST_WHS_PACKAGE_UID = 1234;
  private static final int TEST_NON_WHS_PACKAGE_UID = 5;

  private final ArgumentCaptor<IntentFilter> mIntentFilterCaptor =
      ArgumentCaptor.forClass(IntentFilter.class);
  private final ArgumentCaptor<Intent> mIntentCaptor = ArgumentCaptor.forClass(Intent.class);

  @Mock Context mockContext;
  @Mock PackageManager mockPackageManager;
  @Mock HandlerBindingAgent mockBindingHandler;
  @Mock ConnectionTracker mockConnectionTracker;
  @Mock IBinder mockBinder;
  @Mock HalAdapter mockHalAdapter;
  @Mock IHealthServiceCallback mockIHealthServiceCallback;

  private BinderService mBinderService;
  private TargetUser mTargetUser;
  private HealthService mService;

  @Before
  public void setUp() {
    initMocks(this);
    when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
    when(mockPackageManager.getNameForUid(TEST_WHS_PACKAGE_UID))
        .thenReturn(HealthService.WHS_PACKAGE);
    when(mockPackageManager.getNameForUid(TEST_NON_WHS_PACKAGE_UID)).thenReturn("random_package");
    mService =
        spy(
            new HealthService(
                mockContext, mockBindingHandler, mockConnectionTracker, mockHalAdapter));
    doNothing().when(mService).publishBinderService();
    mBinderService = mService.new BinderService();
    UserInfo userInfo = new UserInfo(1, "rand", null, 0, UserManager.USER_TYPE_PROFILE_MANAGED);
    mTargetUser = new TargetUser(userInfo);
  }

  @Test
  public void onStart_registersBroadcastReceiver() {
    mService.onStart();

    verify(mockContext)
        .registerReceiver(
            any(HealthService.PackageEventsReceiver.class), mIntentFilterCaptor.capture());
    IntentFilter capturedFilter = mIntentFilterCaptor.getValue();
    assertThat(capturedFilter.hasAction(Intent.ACTION_PACKAGE_REPLACED)).isTrue();
    assertThat(capturedFilter.hasAction(Intent.ACTION_PACKAGE_RESTARTED)).isTrue();
    verify(mockHalAdapter).registerHalV1_0RegistrationNotification();
    verify(mockHalAdapter).maybeGetAidlHalService();
  }

  @Test
  public void onBootComplete_schedulesBind() {
    mService.onBootPhase(SystemService.PHASE_BOOT_COMPLETED);

    verify(mockBindingHandler).bind(BOOT_BIND_DELAY_MILLIS);
  }

  @Test
  public void onUserUnlock_notConnected_schedulesBind() {
    when(mockConnectionTracker.isConnected()).thenReturn(false);

    mService.onUserUnlocked(mTargetUser);

    verify(mockBindingHandler).bind(USER_UNLOCK_BIND_DELAY_MILLIS);
  }

  @Test
  public void onUserUnlock_connected_noBind() {
    when(mockConnectionTracker.isConnected()).thenReturn(false);

    mService.onUserUnlocked(mTargetUser);

    verify(mockBindingHandler, never()).bind(anyInt());
  }

  @Test
  public void onBinderDied_schedulesBind() {
    mService.binderDied();

    verify(mockBindingHandler).bind(WHS_CRASH_BIND_DELAY_MILLIS);
  }

  @Test
  public void onReceive_unwantedAction_noBind() {
    when(mockConnectionTracker.isConnected()).thenReturn(true);

    mService.mWhsPackageEventsReceiver.onReceive(mockContext, new Intent("random_action"));

    verify(mockBindingHandler, never()).bind(anyInt());
  }

  @Test
  public void onReceive_packageReplaced_nonWhsPackage_noBind() {
    when(mockConnectionTracker.isConnected()).thenReturn(true);

    mService.mWhsPackageEventsReceiver.onReceive(
        mockContext,
        new Intent(Intent.ACTION_PACKAGE_REPLACED)
            .putExtra(Intent.EXTRA_UID, TEST_NON_WHS_PACKAGE_UID));

    verify(mockBindingHandler, never()).bind(anyInt());
  }

  @Test
  public void onReceive_packageRestarted_nonWhsPackage_noBind() {
    when(mockConnectionTracker.isConnected()).thenReturn(true);

    mService.mWhsPackageEventsReceiver.onReceive(
        mockContext,
        new Intent(Intent.ACTION_PACKAGE_RESTARTED)
            .putExtra(Intent.EXTRA_UID, TEST_NON_WHS_PACKAGE_UID));

    verify(mockBindingHandler, never()).bind(anyInt());
  }

  @Test
  public void onReceive_packageReplaced_whsPackage_connected_noBind() {
    when(mockConnectionTracker.isConnected()).thenReturn(true);

    mService.mWhsPackageEventsReceiver.onReceive(
        mockContext,
        new Intent(Intent.ACTION_PACKAGE_REPLACED)
            .putExtra(Intent.EXTRA_UID, TEST_WHS_PACKAGE_UID));

    verify(mockBindingHandler, never()).bind(anyInt());
  }

  @Test
  public void onReceive_packageRestarted_whsPackage_connected_noBind() {
    when(mockConnectionTracker.isConnected()).thenReturn(true);

    mService.mWhsPackageEventsReceiver.onReceive(
        mockContext,
        new Intent(Intent.ACTION_PACKAGE_RESTARTED)
            .putExtra(Intent.EXTRA_UID, TEST_WHS_PACKAGE_UID));

    verify(mockBindingHandler, never()).bind(anyInt());
  }

  @Test
  public void onReceive_packageReplaced_whsPackage_notConnected_binds() {
    when(mockConnectionTracker.isConnected()).thenReturn(false);

    mService.mWhsPackageEventsReceiver.onReceive(
        mockContext,
        new Intent(Intent.ACTION_PACKAGE_REPLACED)
            .putExtra(Intent.EXTRA_UID, TEST_WHS_PACKAGE_UID));

    verify(mockBindingHandler).bind(PACKAGE_UPDATE_BIND_DELAY_MILLIS);
  }

  @Test
  public void onReceive_packageRestarted_whsPackage_notConnected_binds() {
    when(mockConnectionTracker.isConnected()).thenReturn(false);

    mService.mWhsPackageEventsReceiver.onReceive(
        mockContext,
        new Intent(Intent.ACTION_PACKAGE_RESTARTED)
            .putExtra(Intent.EXTRA_UID, TEST_WHS_PACKAGE_UID));

    verify(mockBindingHandler).bind(PACKAGE_UPDATE_BIND_DELAY_MILLIS);
  }

  @Test
  public void addGoal_forwardsToHalAdapter() throws RemoteException {
    DataTypeGoal dataTypeGoal = getDataTypeGoal(DataType.EXERCISE_STEP_COUNT, 20);

    mBinderService.addGoal(dataTypeGoal);

    verify(mockHalAdapter).addGoal(dataTypeGoal);
  }

  @Test
  public void removeGoal_forwardsToHalAdapter() throws RemoteException {
    DataTypeGoal dataTypeGoal = getDataTypeGoal(DataType.EXERCISE_STEP_COUNT, 20);

    mBinderService.removeGoal(dataTypeGoal);

    verify(mockHalAdapter).removeGoal(dataTypeGoal);
  }

  @Test
  public void startTracking_noOffsets_forwardsToHalAdapter() throws RemoteException {
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};
    TrackingConfig config = getTrackingConfig(ExerciseType.WALKING, false, 0, 0);
    DataTypeOffset[] offsets = {getDataTypeOffset(DataType.EXERCISE_STEP_COUNT, 0)};
    boolean isPaused = false;

    mBinderService.startTracking(config, dataTypes, offsets, isPaused);

    verify(mockHalAdapter).startTracking(config, dataTypes, offsets, isPaused);
  }

  @Test
  public void startTracking_nonZeroOffset_forwardsToHalAdapter() throws RemoteException {
    int offset = 100;
    int maxReportLatencyMs = 120000;
    int samplingPeriodMs = 60000;
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};
    TrackingConfig config =
        getTrackingConfig(ExerciseType.WALKING, false, maxReportLatencyMs, samplingPeriodMs);
    DataTypeOffset[] offsets = {getDataTypeOffset(DataType.EXERCISE_STEP_COUNT, offset)};
    boolean isPaused = true;

    mBinderService.startTracking(config, dataTypes, offsets, isPaused);

    verify(mockHalAdapter).startTracking(config, dataTypes, offsets, isPaused);
  }

  @Test
  public void startAutomaticExerciseDetection_noOffset_forwardsToHalAdapter()
      throws RemoteException {
    AutoStartConfig autoStartConfig = new AutoStartConfig();
    autoStartConfig.exerciseType = ExerciseType.WALKING;
    autoStartConfig.minimumNotificationLatencyMs = 123;
    AutoStartConfig[] autoStartConfigs = {autoStartConfig};

    mBinderService.startAutomaticExerciseDetection(autoStartConfigs, null);

    verify(mockHalAdapter).startAutomaticExerciseDetection(autoStartConfigs, null);
  }

  @Test
  public void startAutomaticExerciseDetection_withOffset_forwardsToHalAdapter()
      throws RemoteException {
    AutoStartConfig autoStartConfig = new AutoStartConfig();
    autoStartConfig.exerciseType = ExerciseType.WALKING;
    autoStartConfig.minimumNotificationLatencyMs = 123;
    AutoStartConfig[] autoStartConfigs = {autoStartConfig};
    AutoStartEvent autoStartEvent = new AutoStartEvent();
    autoStartEvent.elapsedTimeSinceBootNanos = 123;
    autoStartEvent.exerciseType = ExerciseType.WALKING;
    autoStartEvent.type = AutoStartEvent.AutoStartEventType.AUTO_EXERCISE_START_DETECTED;

    mBinderService.startAutomaticExerciseDetection(autoStartConfigs, autoStartEvent);

    verify(mockHalAdapter).startAutomaticExerciseDetection(autoStartConfigs, autoStartEvent);
  }

  @Test
  public void stopAutomaticExerciseDetection_forwardsToHalAdapter() throws RemoteException {
    int[] exerciseTypes = {ExerciseType.WALKING, ExerciseType.RUNNING};

    mBinderService.stopAutomaticExerciseDetection(exerciseTypes);

    verify(mockHalAdapter).stopAutomaticExerciseDetection(exerciseTypes);
  }

  @Test
  public void updateTrackingConfig_forwardsToHalAdapter() throws RemoteException {
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};
    TrackingConfig config = getTrackingConfig(ExerciseType.WALKING, false, 0, 0);

    mBinderService.updateTrackingConfig(config, dataTypes);

    verify(mockHalAdapter).updateTrackingConfig(config, dataTypes);
  }

  @Test
  public void pauseTracking_forwardsToHalAdapter() throws RemoteException {
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};

    mBinderService.pauseTracking(dataTypes);

    verify(mockHalAdapter).pauseTracking(dataTypes);
  }

  @Test
  public void resumeTracking_forwardsToHalAdapter() throws RemoteException {
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};

    mBinderService.resumeTracking(dataTypes);

    verify(mockHalAdapter).resumeTracking(dataTypes);
  }

  @Test
  public void stopTracking_forwardsToHalAdapter() throws RemoteException {
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};

    mBinderService.stopTracking(dataTypes);

    verify(mockHalAdapter).stopTracking(dataTypes);
  }

  @Test
  public void flush_generatesNewId_forwardsToHalAdapter_singleRequest() throws RemoteException {
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};

    mBinderService.flush(dataTypes);

    verify(mockHalAdapter).flush(0, dataTypes);
  }

  @Test
  public void flush_generatesNewId_forwardsToHalAdapter_multiRequest() throws RemoteException {
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};

    mBinderService.flush(dataTypes);
    mBinderService.flush(dataTypes);
    mBinderService.flush(dataTypes);

    verify(mockHalAdapter).flush(0, dataTypes);
    verify(mockHalAdapter).flush(1, dataTypes);
    verify(mockHalAdapter).flush(2, dataTypes);
  }

  @Test
  public void flush_generatesNewId_forwardsToHalAdapter_flushIdWrapsAtMaxInt()
      throws RemoteException {
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};
    mService.nextFlushId = Integer.MAX_VALUE;

    mBinderService.flush(dataTypes);
    mBinderService.flush(dataTypes);

    verify(mockHalAdapter).flush(Integer.MAX_VALUE, dataTypes);
    verify(mockHalAdapter).flush(0, dataTypes);
  }

  @Test
  public void resetDataTypeOffsets_forwardsToHalAdapter() throws RemoteException {
    int offset = 10;
    DataTypeOffset[] offsets = {getDataTypeOffset(DataType.EXERCISE_STEP_COUNT, offset)};

    mBinderService.resetDataTypeOffsets(offsets);

    verify(mockHalAdapter).resetDataTypeOffsets(offsets);
  }

  @Test
  public void setOemCustomConfiguration_forwardsToHalAdapter() throws RemoteException {
    int configId = 1;
    byte[] configValue = {1};

    mBinderService.setOemCustomConfiguration(configId, configValue);

    verify(mockHalAdapter).setOemCustomConfiguration(configId, configValue);
  }

  @Test
  public void onHalConnected_callbackSet_forwardsToCallback() throws RemoteException {
    mBinderService.setHealthServiceCallback(mockIHealthServiceCallback);

    mService.onHalConnected();

    verify(mockIHealthServiceCallback).onHalServiceConnected();
  }

  @Test
  public void onHalDied_callbackSet_forwardsToCallback() throws RemoteException {
    mBinderService.setHealthServiceCallback(mockIHealthServiceCallback);

    mService.onHalDied();

    verify(mockIHealthServiceCallback).onHalServiceDied();
  }

  @Test
  public void onAutoExerciseEvent_callbackSet_forwardsToCallback() throws RemoteException {
    mBinderService.setHealthServiceCallback(mockIHealthServiceCallback);
    AutoPauseEvent autoPauseEvent = new AutoPauseEvent();
    autoPauseEvent.type = AutoPauseResumeEventType.AUTO_PAUSE_DETECTED;
    autoPauseEvent.elapsedTimeSinceBootNanos = 123;
    AutoExerciseEvent autoExerciseEvent = new AutoExerciseEvent();
    autoExerciseEvent.setAutoPauseEvent(autoPauseEvent);

    mService.onAutoExerciseEvent(autoExerciseEvent);

    verify(mockIHealthServiceCallback).onAutoExerciseEvent(autoExerciseEvent);
  }

  private DataTypeGoal getDataTypeGoal(int dataType, int value) {
    DataTypeGoal dataTypeGoal = new DataTypeGoal();
    dataTypeGoal.dataType = dataType;
    dataTypeGoal.threshold = new GoalThreshold();
    dataTypeGoal.threshold.type = GoalThresholdType.INT;
    dataTypeGoal.threshold.intValue = value;
    return dataTypeGoal;
  }

  private TrackingConfig getTrackingConfig(
      int exerciseType, boolean enableGpsControl, int maxReportLatencyMs, int samplingPeriodMs) {
    TrackingConfig config = new TrackingConfig();
    config.exerciseType = exerciseType;
    config.enableGpsControl = enableGpsControl;
    config.maxReportLatencyMs = maxReportLatencyMs;
    config.samplingPeriodMs = samplingPeriodMs;

    return config;
  }

  private DataTypeOffset getDataTypeOffset(int dataType, int offset) {
    DataTypeOffset dataTypeOffset = new DataTypeOffset();
    dataTypeOffset.dataType = dataType;
    OffsetValue offsetValue = new OffsetValue();
    offsetValue.type = OffsetValueType.INT;
    offsetValue.intValue = offset;
    dataTypeOffset.offsetValue = offsetValue;

    return dataTypeOffset;
  }
}
