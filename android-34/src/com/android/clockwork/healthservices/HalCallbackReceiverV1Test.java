package com.android.clockwork.healthservices;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import static org.junit.Assert.assertTrue;

import android.os.RemoteException;

import com.google.android.clockwork.healthservices.types.AchievedGoal;
import com.google.android.clockwork.healthservices.types.AutoExerciseEvent;
import com.google.android.clockwork.healthservices.types.AutoPauseEvent;
import com.google.android.clockwork.healthservices.types.AutoPauseResumeEventType;
import com.google.android.clockwork.healthservices.types.AutoStartEvent;
import com.google.android.clockwork.healthservices.types.AutoStopEvent;
import com.google.android.clockwork.healthservices.types.AvailabilityUpdate;
import com.google.android.clockwork.healthservices.types.DataTypeAvailability;
import com.google.android.clockwork.healthservices.types.DataTypeAvailabilityStatus;
import com.google.android.clockwork.healthservices.types.DataType;
import com.google.android.clockwork.healthservices.types.DataTypeGoal;
import com.google.android.clockwork.healthservices.types.DataTypeOffset;
import com.google.android.clockwork.healthservices.types.DataUpdate;
import com.google.android.clockwork.healthservices.types.ExerciseEvent;
import com.google.android.clockwork.healthservices.types.ExerciseType;
import com.google.android.clockwork.healthservices.types.Gender;
import com.google.android.clockwork.healthservices.types.GoalThreshold;
import com.google.android.clockwork.healthservices.types.GoalThresholdType;
import com.google.android.clockwork.healthservices.types.GolfShotDetectionEvent;
import com.google.android.clockwork.healthservices.types.GolfShotDetectionType;
import com.google.android.clockwork.healthservices.types.HealthEvent;
import com.google.android.clockwork.healthservices.types.HealthEventDetectedType;
import com.google.android.clockwork.healthservices.types.LocationAvailability;
import com.google.android.clockwork.healthservices.types.LocationAvailabilityStatus;
import com.google.android.clockwork.healthservices.types.LocationData;
import com.google.android.clockwork.healthservices.types.MetricAccuracy;
import com.google.android.clockwork.healthservices.types.MetricData;
import com.google.android.clockwork.healthservices.types.MetricValue;
import com.google.android.clockwork.healthservices.types.MetricValueType;
import com.google.android.clockwork.healthservices.types.OffsetValue;
import com.google.android.clockwork.healthservices.types.OffsetValueType;
import com.google.android.clockwork.healthservices.types.StatsData;
import com.google.android.clockwork.healthservices.types.StatsValue;
import com.google.android.clockwork.healthservices.types.TrackingConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class HalCallbackReceiverV1Test {
  @Mock HalAdapter.HalListener mockHalListener;

  private HalCallbackReceiverV1 mHalCallbackReceiverV1;

  @Before
  public void setUp() {
    initMocks(this);

    mHalCallbackReceiverV1 = new HalCallbackReceiverV1(mockHalListener);
  }

  @Test
  public void onDataUpdate_metricIntUpdate_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.DataUpdate halMetricUpdate =
        getHalIntMetricDataUpdate(DataType.EXERCISE_STEP_COUNT, 100, 123);
    vendor.google_clockwork.healthservices.DataUpdate[] updates = {halMetricUpdate};

    mHalCallbackReceiverV1.onDataUpdate(updates);

    DataUpdate expectedMetricDataUpdate =
        getIntMetricDataUpdate(DataType.EXERCISE_STEP_COUNT, 100, 123);
    DataUpdate[] expectedDataUpdates = {expectedMetricDataUpdate};
    verify(mockHalListener).onDataUpdate(expectedDataUpdates);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onAutoExerciseEvent(any());
  }

  @Test
  public void onDataUpdate_metricFloatUpdate_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.DataUpdate halMetricUpdate =
        getHalFloatMetricDataUpdate(DataType.EXERCISE_DISTANCE, 20f, 123);
    vendor.google_clockwork.healthservices.DataUpdate[] updates = {halMetricUpdate};

    mHalCallbackReceiverV1.onDataUpdate(updates);

    DataUpdate expectedMetricDataUpdate =
        getFloatMetricDataUpdate(DataType.EXERCISE_DISTANCE, 20f, 123);
    DataUpdate[] expectedDataUpdates = {expectedMetricDataUpdate};
    verify(mockHalListener).onDataUpdate(expectedDataUpdates);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onAutoExerciseEvent(any());
  }

  @Test
  public void onDataUpdate_locationUpdate_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.DataUpdate halLocationUpdate =
        getHalLocationDataUpdate(37, -122, 10, 20, 123);
    vendor.google_clockwork.healthservices.DataUpdate[] updates = {halLocationUpdate};

    mHalCallbackReceiverV1.onDataUpdate(updates);

    DataUpdate expectedLocationDataUpdate = getLocationDataUpdate(37, -122, 10, 20, 123);
    DataUpdate[] expectedDataUpdates = {expectedLocationDataUpdate};
    verify(mockHalListener).onDataUpdate(expectedDataUpdates);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onAutoExerciseEvent(any());
  }

  @Test
  public void onDataUpdate_intStatsUpdate_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.DataUpdate halStatsDataUpdate =
        getHalIntStatsDataUpdate(DataType.STEPS_PER_MINUTE_STATS, 60, 80, 70);
    vendor.google_clockwork.healthservices.DataUpdate[] updates = {halStatsDataUpdate};

    mHalCallbackReceiverV1.onDataUpdate(updates);

    DataUpdate expectedIntStatsDataUpdate =
        getIntStatsDataUpdate(DataType.STEPS_PER_MINUTE_STATS, 60, 80, 70);
    DataUpdate[] expectedDataUpdates = {expectedIntStatsDataUpdate};
    verify(mockHalListener).onDataUpdate(expectedDataUpdates);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onAutoExerciseEvent(any());
  }

  @Test
  public void onDataUpdate_floatStatsUpdate_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.DataUpdate halStatsDataUpdate =
        getHalFloatStatsDataUpdate(DataType.HEART_RATE_BPM_STATS, 80.2f, 92.7f, 85.3f);
    vendor.google_clockwork.healthservices.DataUpdate[] updates = {halStatsDataUpdate};

    mHalCallbackReceiverV1.onDataUpdate(updates);

    DataUpdate expectedFloatStatsDataUpdate =
        getFloatStatsDataUpdate(DataType.HEART_RATE_BPM_STATS, 80.2f, 92.7f, 85.3f);
    DataUpdate[] expectedDataUpdates = {expectedFloatStatsDataUpdate};
    verify(mockHalListener).onDataUpdate(expectedDataUpdates);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onAutoExerciseEvent(any());
  }

  @Test
  public void onDataUpdate_multiStatsUpdate_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.DataUpdate halIntStatsDataUpdate =
        getHalIntStatsDataUpdate(DataType.STEPS_PER_MINUTE_STATS, 60, 80, 70);
    vendor.google_clockwork.healthservices.DataUpdate halFloatStatsDataUpdate =
        getHalFloatStatsDataUpdate(DataType.HEART_RATE_BPM_STATS, 80.2f, 92.7f, 85.3f);

    vendor.google_clockwork.healthservices.DataUpdate[] updates = {
      halIntStatsDataUpdate, halFloatStatsDataUpdate
    };

    mHalCallbackReceiverV1.onDataUpdate(updates);

    DataUpdate expectedIntStatsDataUpdate =
        getIntStatsDataUpdate(DataType.STEPS_PER_MINUTE_STATS, 60, 80, 70);
    DataUpdate expectedFloatStatsDataUpdate =
        getFloatStatsDataUpdate(DataType.HEART_RATE_BPM_STATS, 80.2f, 92.7f, 85.3f);
    DataUpdate[] expectedDataUpdates = {expectedIntStatsDataUpdate, expectedFloatStatsDataUpdate};
    verify(mockHalListener).onDataUpdate(expectedDataUpdates);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onAutoExerciseEvent(any());
  }

  @Test
  public void onDataUpdate_autoPauseEvent_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.DataUpdate halAutoPauseEventUpdate =
        getHalAutoPauseEventDataUpdate(
            vendor.google_clockwork.healthservices.AutoPauseEvent.AutoPauseResumeEventType
                .AUTO_PAUSE_DETECTED,
            123);
    vendor.google_clockwork.healthservices.DataUpdate[] updates = {halAutoPauseEventUpdate};

    mHalCallbackReceiverV1.onDataUpdate(updates);

    AutoExerciseEvent expectedAutoPauseEvent =
        getAutoPauseEvent(AutoPauseResumeEventType.AUTO_PAUSE_DETECTED, 123);
    verify(mockHalListener).onAutoExerciseEvent(expectedAutoPauseEvent);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onDataUpdate(any());
  }

  @Test
  public void onDataUpdate_autoStopEvent_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.DataUpdate halAutoStopEventUpdate =
        getHalAutoStopEventDataUpdate(123);
    vendor.google_clockwork.healthservices.DataUpdate[] updates = {halAutoStopEventUpdate};

    mHalCallbackReceiverV1.onDataUpdate(updates);

    AutoExerciseEvent expectedAutoStopEvent = getAutoStopEvent(123);
    verify(mockHalListener).onAutoExerciseEvent(expectedAutoStopEvent);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onDataUpdate(any());
  }

  @Test
  public void onDataUpdate_autoStartEvent_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.DataUpdate halAutoStartEventUpdate =
        getHalAutoStartEventDataUpdate(
            vendor.google_clockwork.healthservices.ExerciseType.WALKING,
            vendor.google_clockwork.healthservices.AutoStartEvent.AutoStartEventType
                .AUTO_EXERCISE_START_DETECTED,
            123);
    vendor.google_clockwork.healthservices.DataUpdate[] updates = {halAutoStartEventUpdate};

    mHalCallbackReceiverV1.onDataUpdate(updates);

    AutoExerciseEvent expectedAutoStartEvent =
        getAutoStartEvent(
            ExerciseType.WALKING,
            AutoStartEvent.AutoStartEventType.AUTO_EXERCISE_START_DETECTED,
            123);
    verify(mockHalListener).onAutoExerciseEvent(expectedAutoStartEvent);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onDataUpdate(any());
  }

  @Test
  public void onDataUpdate_singleExerciseEvent_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.DataUpdate halDriveGolfShot =
        getHalGolfShotDetectionDataUpdate(
            123,
            vendor.google_clockwork.healthservices.GolfShotDetectionType
                .GOLF_SHOT_DETECTED_DRIVE, /*contactDetected*/
            true);
    vendor.google_clockwork.healthservices.DataUpdate[] updates = {halDriveGolfShot};

    mHalCallbackReceiverV1.onDataUpdate(updates);

    ExerciseEvent expectedDriveGolfShot =
        getGolfShotDetectionExerciseEventDataUpdate(
            123, GolfShotDetectionType.GOLF_SHOT_DETECTED_DRIVE, /*contactDetected*/ true);
    ExerciseEvent[] expectedExerciseEvents = {expectedDriveGolfShot};
    verify(mockHalListener).onExerciseEvent(expectedExerciseEvents);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onDataUpdate(any());
  }

  @Test
  public void onDataUpdate_batchedExerciseEvent_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.DataUpdate halDriveGolfShot =
        getHalGolfShotDetectionDataUpdate(
            123,
            vendor.google_clockwork.healthservices.GolfShotDetectionType
                .GOLF_SHOT_DETECTED_DRIVE, /*contactDetected*/
            true);
    vendor.google_clockwork.healthservices.DataUpdate halPuttingGolfShot =
        getHalGolfShotDetectionDataUpdate(
            200,
            vendor.google_clockwork.healthservices.GolfShotDetectionType
                .GOLF_SHOT_DETECTED_PUTTING, /*contactDetected*/
            true);
    vendor.google_clockwork.healthservices.DataUpdate[] updates = {
      halDriveGolfShot, halPuttingGolfShot
    };

    mHalCallbackReceiverV1.onDataUpdate(updates);

    ExerciseEvent expectedDriveGolfShot =
        getGolfShotDetectionExerciseEventDataUpdate(
            123, GolfShotDetectionType.GOLF_SHOT_DETECTED_DRIVE, /*contactDetected*/ true);
    ExerciseEvent expectedPuttingGolfShot =
        getGolfShotDetectionExerciseEventDataUpdate(
            200, GolfShotDetectionType.GOLF_SHOT_DETECTED_PUTTING, /*contactDetected*/ true);
    ExerciseEvent[] expectedExerciseEvents = {expectedDriveGolfShot, expectedPuttingGolfShot};
    verify(mockHalListener).onExerciseEvent(expectedExerciseEvents);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onDataUpdate(any());
  }

  @Test
  public void onDataUpdate_healthEvent_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.DataUpdate halHealthEventUpdate =
        getHalHealthEventDataUpdate(
            vendor.google_clockwork.healthservices.HealthEvent.HealthEventDetectedType
                .FALL_DETECTED,
            456);
    vendor.google_clockwork.healthservices.DataUpdate[] updates = {halHealthEventUpdate};

    mHalCallbackReceiverV1.onDataUpdate(updates);

    HealthEvent expectedHealthEvent = getHealthEvent(HealthEventDetectedType.FALL_DETECTED, 456);
    verify(mockHalListener).onHealthEventDetected(expectedHealthEvent);
    verify(mockHalListener, never()).onAutoExerciseEvent(any());
    verify(mockHalListener, never()).onDataUpdate(any());
  }

  @Test
  public void onDataUpdate_allUpdatesTogether_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.DataUpdate halIntMetricUpdate =
        getHalIntMetricDataUpdate(DataType.EXERCISE_STEP_COUNT, 100, 123);
    vendor.google_clockwork.healthservices.DataUpdate halFloatMetricUpdate =
        getHalFloatMetricDataUpdate(DataType.EXERCISE_DISTANCE, 20f, 123);
    vendor.google_clockwork.healthservices.DataUpdate halLocationUpdate =
        getHalLocationDataUpdate(37, -122, 10, 20, 123);
    vendor.google_clockwork.healthservices.DataUpdate halAutoPauseEventUpdate =
        getHalAutoPauseEventDataUpdate(
            vendor.google_clockwork.healthservices.AutoPauseEvent.AutoPauseResumeEventType
                .AUTO_PAUSE_DETECTED,
            123);
    vendor.google_clockwork.healthservices.DataUpdate halHealthEventUpdate =
        getHalHealthEventDataUpdate(
            vendor.google_clockwork.healthservices.HealthEvent.HealthEventDetectedType
                .FALL_DETECTED,
            456);
    vendor.google_clockwork.healthservices.DataUpdate halIntStatsDataUpdate =
        getHalIntStatsDataUpdate(DataType.STEPS_PER_MINUTE_STATS, 60, 80, 70);
    vendor.google_clockwork.healthservices.DataUpdate halFloatStatsDataUpdate =
        getHalFloatStatsDataUpdate(DataType.HEART_RATE_BPM_STATS, 80.2f, 92.7f, 85.3f);

    vendor.google_clockwork.healthservices.DataUpdate[] updates = {
      halIntMetricUpdate,
      halFloatMetricUpdate,
      halLocationUpdate,
      halAutoPauseEventUpdate,
      halHealthEventUpdate,
        halIntStatsDataUpdate,
        halFloatStatsDataUpdate
    };

    mHalCallbackReceiverV1.onDataUpdate(updates);

    HealthEvent expectedHealthEvent = getHealthEvent(HealthEventDetectedType.FALL_DETECTED, 456);
    verify(mockHalListener).onHealthEventDetected(expectedHealthEvent);
    AutoExerciseEvent expectedAutoPauseEvent =
        getAutoPauseEvent(AutoPauseResumeEventType.AUTO_PAUSE_DETECTED, 123);
    verify(mockHalListener).onAutoExerciseEvent(expectedAutoPauseEvent);
    DataUpdate expectedLocationDataUpdate = getLocationDataUpdate(37, -122, 10, 20, 123);
    DataUpdate expectedIntMetricDataUpdate =
        getIntMetricDataUpdate(DataType.EXERCISE_STEP_COUNT, 100, 123);
    DataUpdate expectedFloatMetricDataUpdate =
        getFloatMetricDataUpdate(DataType.EXERCISE_DISTANCE, 20f, 123);
    DataUpdate expectedIntStatsDataUpdate =
        getIntStatsDataUpdate(DataType.STEPS_PER_MINUTE_STATS, 60, 80, 70);
    DataUpdate expectedFloatStatsDataUpdate =
        getFloatStatsDataUpdate(DataType.HEART_RATE_BPM_STATS, 80.2f, 92.7f, 85.3f);
    DataUpdate[] expectedDataUpdates = {
      expectedIntMetricDataUpdate, expectedFloatMetricDataUpdate, expectedLocationDataUpdate,
        expectedIntStatsDataUpdate, expectedFloatStatsDataUpdate
    };
    verify(mockHalListener).onDataUpdate(expectedDataUpdates);
  }

  @Test
  public void onAvailabilityUpdate_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.AvailabilityUpdate halDataTypeAvailabilityUpdate =
        new vendor.google_clockwork.healthservices.AvailabilityUpdate();
    vendor.google_clockwork.healthservices.AvailabilityUpdate.DataTypeAvailability
        halDataTypeAvailability =
            new vendor.google_clockwork.healthservices.AvailabilityUpdate.DataTypeAvailability();
    halDataTypeAvailability.dataType = DataType.EXERCISE_STEP_COUNT;
    halDataTypeAvailability.elapsedTimeSinceBootNanos = 100;
    halDataTypeAvailability.dataTypeAvailabilityStatus =
        vendor.google_clockwork.healthservices.AvailabilityUpdate.DataTypeAvailability
            .DataTypeAvailabilityStatus.AVAILABLE;
    halDataTypeAvailabilityUpdate.setDataTypeAvailability(halDataTypeAvailability);
    vendor.google_clockwork.healthservices.AvailabilityUpdate halLocationAvailabilityUpdate =
        new vendor.google_clockwork.healthservices.AvailabilityUpdate();
    vendor.google_clockwork.healthservices.AvailabilityUpdate.LocationAvailability
        halLocationAvailability =
            new vendor.google_clockwork.healthservices.AvailabilityUpdate.LocationAvailability();
    halLocationAvailability.elapsedTimeSinceBootNanos = 200;
    halLocationAvailability.locationAvailabilityStatus =
        vendor.google_clockwork.healthservices.AvailabilityUpdate.LocationAvailability
            .LocationAvailabilityStatus.ACQUIRED_UNTETHERED;
    halLocationAvailabilityUpdate.setLocationAvailability(halLocationAvailability);
    vendor.google_clockwork.healthservices.AvailabilityUpdate[] updates = {
      halDataTypeAvailabilityUpdate, halLocationAvailabilityUpdate
    };

    mHalCallbackReceiverV1.onAvailabilityUpdate(updates);

    AvailabilityUpdate dataTypeAvailabilityUpdate = new AvailabilityUpdate();
    DataTypeAvailability dataTypeAvailability = new DataTypeAvailability();
    dataTypeAvailability.dataType = DataType.EXERCISE_STEP_COUNT;
    dataTypeAvailability.elapsedTimeSinceBootNanos = 100;
    dataTypeAvailability.dataTypeAvailabilityStatus = DataTypeAvailabilityStatus.AVAILABLE;
    dataTypeAvailabilityUpdate.dataTypeAvailability = dataTypeAvailability;
    AvailabilityUpdate locationAvailabilityUpdate = new AvailabilityUpdate();
    LocationAvailability locationAvailability = new LocationAvailability();
    locationAvailability.elapsedTimeSinceBootNanos = 200;
    locationAvailability.locationAvailabilityStatus =
        LocationAvailabilityStatus.ACQUIRED_UNTETHERED;
    locationAvailabilityUpdate.locationAvailability = locationAvailability;

    AvailabilityUpdate[] expectedUpdates = {dataTypeAvailabilityUpdate, locationAvailabilityUpdate};
    verify(mockHalListener).onAvailabilityUpdate(expectedUpdates);
  }

  @Test
  public void onGoalAchieved_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.AchievedGoal halIntAchievedGoal =
        new vendor.google_clockwork.healthservices.AchievedGoal();
    vendor.google_clockwork.healthservices.DataTypeGoal halIntDataTypeGoal =
        new vendor.google_clockwork.healthservices.DataTypeGoal();
    halIntDataTypeGoal.dataType = DataType.EXERCISE_STEP_COUNT;
    halIntDataTypeGoal.threshold =
        new vendor.google_clockwork.healthservices.DataTypeGoal.GoalThreshold();
    halIntDataTypeGoal.threshold.setIntThreshold(100);
    halIntAchievedGoal.goal = halIntDataTypeGoal;
    halIntAchievedGoal.elapsedTimeSinceBootNanos = 123;
    vendor.google_clockwork.healthservices.AchievedGoal halFloatAchievedGoal =
        new vendor.google_clockwork.healthservices.AchievedGoal();
    vendor.google_clockwork.healthservices.DataTypeGoal halFloatDataTypeGoal =
        new vendor.google_clockwork.healthservices.DataTypeGoal();
    halFloatDataTypeGoal.dataType = DataType.EXERCISE_DISTANCE;
    halFloatDataTypeGoal.threshold =
        new vendor.google_clockwork.healthservices.DataTypeGoal.GoalThreshold();
    halFloatDataTypeGoal.threshold.setFloatThreshold(20f);
    halFloatAchievedGoal.goal = halFloatDataTypeGoal;
    halFloatAchievedGoal.elapsedTimeSinceBootNanos = 456;
    vendor.google_clockwork.healthservices.AchievedGoal[] updates = {
      halIntAchievedGoal, halFloatAchievedGoal
    };

    mHalCallbackReceiverV1.onGoalAchieved(updates);

    AchievedGoal intAchievedGoal = new AchievedGoal();
    DataTypeGoal intDataTypeGoal = new DataTypeGoal();
    intDataTypeGoal.dataType = DataType.EXERCISE_STEP_COUNT;
    intDataTypeGoal.threshold = new GoalThreshold();
    intDataTypeGoal.threshold.type = GoalThresholdType.INT;
    intDataTypeGoal.threshold.intValue = 100;
    intAchievedGoal.goal = intDataTypeGoal;
    intAchievedGoal.elapsedTimeSinceBootNanos = 123;
    AchievedGoal floatAchievedGoal = new AchievedGoal();
    DataTypeGoal floatDataTypeGoal = new DataTypeGoal();
    floatDataTypeGoal.dataType = DataType.EXERCISE_DISTANCE;
    floatDataTypeGoal.threshold = new GoalThreshold();
    floatDataTypeGoal.threshold.type = GoalThresholdType.FLOAT;
    floatDataTypeGoal.threshold.floatValue = 20f;
    floatAchievedGoal.goal = floatDataTypeGoal;
    floatAchievedGoal.elapsedTimeSinceBootNanos = 456;

    AchievedGoal[] expectedUpdates = {intAchievedGoal, floatAchievedGoal};
    verify(mockHalListener).onGoalAchieved(expectedUpdates);
  }

  @Test
  public void onFlushCompleted_notifiesHalListener() throws RemoteException {
    mHalCallbackReceiverV1.onFlushCompleted(/* flushId= */ 1);

    verify(mockHalListener).onFlushCompleted(/* flushId= */ 1);
  }

  private vendor.google_clockwork.healthservices.DataUpdate getHalFloatMetricDataUpdate(
      int dataType, float value, long elapsedTimeSinceBootNanos) {
    vendor.google_clockwork.healthservices.MetricData halMetricData =
        new vendor.google_clockwork.healthservices.MetricData();
    halMetricData.dataType = dataType;
    halMetricData.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    vendor.google_clockwork.healthservices.MetricValue halMetricValue =
        new vendor.google_clockwork.healthservices.MetricValue();
    halMetricValue.setFloatValue(value);
    halMetricData.metricValue = halMetricValue;
    halMetricData.accuracy =
        vendor.google_clockwork.healthservices.MetricData.MetricAccuracy.ACCURACY_MEDIUM;

    vendor.google_clockwork.healthservices.DataUpdate halMetricDataUpdate =
        new vendor.google_clockwork.healthservices.DataUpdate();
    halMetricDataUpdate.setMetricData(halMetricData);
    return halMetricDataUpdate;
  }

  private DataUpdate getFloatMetricDataUpdate(
      int dataType, float value, long elapsedTimeSinceBootNanos) {
    MetricData metricData = new MetricData();
    metricData.dataType = dataType;
    metricData.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    MetricValue metricValue = new MetricValue();
    metricValue.type = MetricValueType.FLOAT;
    metricValue.floatValue = value;
    metricData.metricValue = metricValue;
    metricData.accuracy = MetricAccuracy.ACCURACY_MEDIUM;

    DataUpdate dataUpdate = new DataUpdate();
    dataUpdate.metricData = metricData;
    return dataUpdate;
  }

  private vendor.google_clockwork.healthservices.DataUpdate getHalIntMetricDataUpdate(
      int dataType, int value, long elapsedTimeSinceBootNanos) {
    vendor.google_clockwork.healthservices.MetricData halMetricData =
        new vendor.google_clockwork.healthservices.MetricData();
    halMetricData.dataType = dataType;
    halMetricData.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    vendor.google_clockwork.healthservices.MetricValue halMetricValue =
        new vendor.google_clockwork.healthservices.MetricValue();
    halMetricValue.setIntValue(value);
    halMetricData.metricValue = halMetricValue;
    halMetricData.accuracy =
        vendor.google_clockwork.healthservices.MetricData.MetricAccuracy.ACCURACY_MEDIUM;

    vendor.google_clockwork.healthservices.DataUpdate halMetricDataUpdate =
        new vendor.google_clockwork.healthservices.DataUpdate();
    halMetricDataUpdate.setMetricData(halMetricData);
    return halMetricDataUpdate;
  }

  private DataUpdate getIntMetricDataUpdate(
      int dataType, int value, long elapsedTimeSinceBootNanos) {
    MetricData metricData = new MetricData();
    metricData.dataType = dataType;
    metricData.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    MetricValue metricValue = new MetricValue();
    metricValue.type = MetricValueType.INT;
    metricValue.intValue = value;
    metricData.metricValue = metricValue;
    metricData.accuracy = MetricAccuracy.ACCURACY_MEDIUM;

    DataUpdate dataUpdate = new DataUpdate();
    dataUpdate.metricData = metricData;
    return dataUpdate;
  }

  private vendor.google_clockwork.healthservices.DataUpdate getHalLocationDataUpdate(
      double latitude,
      double longitude,
      float horizontalPositionErrorMeters,
      float bearing,
      long elapsedTimeSinceBootNanos) {
    vendor.google_clockwork.healthservices.DataUpdate halLocationDataUpdate =
        new vendor.google_clockwork.healthservices.DataUpdate();
    vendor.google_clockwork.healthservices.LocationData halLocationData =
        new vendor.google_clockwork.healthservices.LocationData();
    halLocationData.latitude = latitude;
    halLocationData.longitude = longitude;
    halLocationData.horizontalPositionErrorMeters = horizontalPositionErrorMeters;
    halLocationData.bearing = bearing;
    halLocationData.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    halLocationDataUpdate.setLocationData(halLocationData);
    return halLocationDataUpdate;
  }

  private DataUpdate getLocationDataUpdate(
      double latitude,
      double longitude,
      float horizontalPositionErrorMeters,
      float bearing,
      long elapsedTimeSinceBootNanos) {
    DataUpdate dataUpdate = new DataUpdate();
    LocationData locationData = new LocationData();
    locationData.latitude = latitude;
    locationData.longitude = longitude;
    locationData.horizontalPositionErrorMeters = horizontalPositionErrorMeters;
    locationData.bearing = bearing;
    locationData.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    dataUpdate.locationData = locationData;
    return dataUpdate;
  }

  private vendor.google_clockwork.healthservices.DataUpdate getHalIntStatsDataUpdate(
      int dataType, int minValue, int maxValue, int avgValue) {
    vendor.google_clockwork.healthservices.StatsData halStatsData =
        new vendor.google_clockwork.healthservices.StatsData();
    halStatsData.dataType = dataType;

    vendor.google_clockwork.healthservices.StatsValue.IntStatsValue halIntStatsValue =
        new vendor.google_clockwork.healthservices.StatsValue.IntStatsValue();
    halIntStatsValue.minimumValue = minValue;
    halIntStatsValue.maximumValue = maxValue;
    halIntStatsValue.averageValue = avgValue;

    vendor.google_clockwork.healthservices.StatsValue halStatsValue =
        new vendor.google_clockwork.healthservices.StatsValue();
    halStatsValue.setIntStatsValue(halIntStatsValue);
    halStatsData.statsValue = halStatsValue;

    vendor.google_clockwork.healthservices.DataUpdate halStatsDataUpdate =
        new vendor.google_clockwork.healthservices.DataUpdate();
    halStatsDataUpdate.setStatsData(halStatsData);
    return halStatsDataUpdate;
  }

  private vendor.google_clockwork.healthservices.DataUpdate getHalFloatStatsDataUpdate(
      int dataType, float minValue, float maxValue, float avgValue) {
    vendor.google_clockwork.healthservices.StatsData halStatsData =
        new vendor.google_clockwork.healthservices.StatsData();
    halStatsData.dataType = dataType;

    vendor.google_clockwork.healthservices.StatsValue.FloatStatsValue halFloatStatsValue =
        new vendor.google_clockwork.healthservices.StatsValue.FloatStatsValue();
    halFloatStatsValue.minimumValue = minValue;
    halFloatStatsValue.maximumValue = maxValue;
    halFloatStatsValue.averageValue = avgValue;

    vendor.google_clockwork.healthservices.StatsValue halStatsValue =
        new vendor.google_clockwork.healthservices.StatsValue();
    halStatsValue.setFloatStatsValue(halFloatStatsValue);
    halStatsData.statsValue = halStatsValue;

    vendor.google_clockwork.healthservices.DataUpdate halStatsDataUpdate =
        new vendor.google_clockwork.healthservices.DataUpdate();
    halStatsDataUpdate.setStatsData(halStatsData);
    return halStatsDataUpdate;
  }

  private DataUpdate getIntStatsDataUpdate(int dataType, int minValue, int maxValue, int avgValue) {
    StatsData statsData = new StatsData();
    statsData.dataType = dataType;

    StatsValue.IntStatsValue intStatsValue = new StatsValue.IntStatsValue();
    intStatsValue.minimumValue = minValue;
    intStatsValue.maximumValue = maxValue;
    intStatsValue.averageValue = avgValue;

    StatsValue statsValue = new StatsValue();
    statsValue.setIntStatsValue(intStatsValue);
    statsData.statsValue = statsValue;

    DataUpdate statsDataUpdate = new DataUpdate();
    statsDataUpdate.statsData = statsData;
    return statsDataUpdate;
  }

  private DataUpdate getFloatStatsDataUpdate(
      int dataType, float minValue, float maxValue, float avgValue) {
    StatsData statsData = new StatsData();
    statsData.dataType = dataType;

    StatsValue.FloatStatsValue floatStatsValue = new StatsValue.FloatStatsValue();
    floatStatsValue.minimumValue = minValue;
    floatStatsValue.maximumValue = maxValue;
    floatStatsValue.averageValue = avgValue;

    StatsValue statsValue = new StatsValue();
    statsValue.setFloatStatsValue(floatStatsValue);
    statsData.statsValue = statsValue;

    DataUpdate statsDataUpdate = new DataUpdate();
    statsDataUpdate.statsData = statsData;
    return statsDataUpdate;
  }

  private vendor.google_clockwork.healthservices.DataUpdate getHalAutoPauseEventDataUpdate(
      byte eventType, long elapsedTimeSinceBootNanos) {
    vendor.google_clockwork.healthservices.AutoPauseEvent halAutoPauseEvent =
        new vendor.google_clockwork.healthservices.AutoPauseEvent();
    halAutoPauseEvent.type = eventType;
    halAutoPauseEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;

    vendor.google_clockwork.healthservices.AutoExerciseEvent halAutoExerciseEvent =
        new vendor.google_clockwork.healthservices.AutoExerciseEvent();
    halAutoExerciseEvent.setAutoPauseEvent(halAutoPauseEvent);

    vendor.google_clockwork.healthservices.DataUpdate halAutoPauseEventUpdate =
        new vendor.google_clockwork.healthservices.DataUpdate();
    halAutoPauseEventUpdate.setAutoExerciseEvent(halAutoExerciseEvent);
    return halAutoPauseEventUpdate;
  }

  private AutoExerciseEvent getAutoPauseEvent(byte eventType, long elapsedTimeSinceBootNanos) {
    AutoPauseEvent autoPauseEvent = new AutoPauseEvent();
    autoPauseEvent.type = eventType;
    autoPauseEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;

    AutoExerciseEvent autoExerciseEvent = new AutoExerciseEvent();
    autoExerciseEvent.setAutoPauseEvent(autoPauseEvent);
    return autoExerciseEvent;
  }

  private vendor.google_clockwork.healthservices.DataUpdate getHalAutoStopEventDataUpdate(
      long elapsedTimeSinceBootNanos) {
    vendor.google_clockwork.healthservices.AutoStopEvent halAutoStopEvent =
        new vendor.google_clockwork.healthservices.AutoStopEvent();
    halAutoStopEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;

    vendor.google_clockwork.healthservices.AutoExerciseEvent halAutoExerciseEvent =
        new vendor.google_clockwork.healthservices.AutoExerciseEvent();
    halAutoExerciseEvent.setAutoStopEvent(halAutoStopEvent);

    vendor.google_clockwork.healthservices.DataUpdate halAutoStopEventUpdate =
        new vendor.google_clockwork.healthservices.DataUpdate();
    halAutoStopEventUpdate.setAutoExerciseEvent(halAutoExerciseEvent);
    return halAutoStopEventUpdate;
  }

  private AutoExerciseEvent getAutoStopEvent(long elapsedTimeSinceBootNanos) {
    AutoStopEvent autoStopEvent = new AutoStopEvent();
    autoStopEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;

    AutoExerciseEvent autoExerciseEvent = new AutoExerciseEvent();
    autoExerciseEvent.setAutoStopEvent(autoStopEvent);
    return autoExerciseEvent;
  }

  private vendor.google_clockwork.healthservices.DataUpdate getHalAutoStartEventDataUpdate(
      int exerciseType, byte type, long elapsedTimeSinceBootNanos) {
    vendor.google_clockwork.healthservices.AutoStartEvent halAutoStartEvent =
        new vendor.google_clockwork.healthservices.AutoStartEvent();
    halAutoStartEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    halAutoStartEvent.exerciseType = exerciseType;
    halAutoStartEvent.type = type;

    vendor.google_clockwork.healthservices.AutoExerciseEvent halAutoExerciseEvent =
        new vendor.google_clockwork.healthservices.AutoExerciseEvent();
    halAutoExerciseEvent.setAutoStartEvent(halAutoStartEvent);

    vendor.google_clockwork.healthservices.DataUpdate halAutoStartEventUpdate =
        new vendor.google_clockwork.healthservices.DataUpdate();
    halAutoStartEventUpdate.setAutoExerciseEvent(halAutoExerciseEvent);
    return halAutoStartEventUpdate;
  }

  private AutoExerciseEvent getAutoStartEvent(
      int exerciseType, byte type, long elapsedTimeSinceBootNanos) {
    AutoStartEvent autoStartEvent = new AutoStartEvent();
    autoStartEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    autoStartEvent.exerciseType = exerciseType;
    autoStartEvent.type = type;

    AutoExerciseEvent autoExerciseEvent = new AutoExerciseEvent();
    autoExerciseEvent.setAutoStartEvent(autoStartEvent);
    return autoExerciseEvent;
  }

  private vendor.google_clockwork.healthservices.DataUpdate getHalGolfShotDetectionDataUpdate(
      long elapsedTimeSinceBootNanos, int type, boolean contactDetected) {
    vendor.google_clockwork.healthservices.GolfShotDetectionEvent halGolfShotDetectionEvent =
        new vendor.google_clockwork.healthservices.GolfShotDetectionEvent();

    halGolfShotDetectionEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    halGolfShotDetectionEvent.type = type;
    halGolfShotDetectionEvent.contactDetected = contactDetected;

    vendor.google_clockwork.healthservices.ExerciseEvent halExerciseEvent =
        new vendor.google_clockwork.healthservices.ExerciseEvent();
    halExerciseEvent.setGolfShotDetectionEvent(halGolfShotDetectionEvent);

    vendor.google_clockwork.healthservices.DataUpdate halDataUpdate =
        new vendor.google_clockwork.healthservices.DataUpdate();
    halDataUpdate.setExerciseEvent(halExerciseEvent);
    return halDataUpdate;
  }

  private ExerciseEvent getGolfShotDetectionExerciseEventDataUpdate(
      long elapsedTimeSinceBootNanos, int type, boolean contactDetected) {
    GolfShotDetectionEvent golfShotDetectionEvent = new GolfShotDetectionEvent();
    golfShotDetectionEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    golfShotDetectionEvent.type = type;
    golfShotDetectionEvent.contactDetected = contactDetected;

    ExerciseEvent exerciseEvent = new ExerciseEvent();
    exerciseEvent.setGolfShotDetectionEvent(golfShotDetectionEvent);

    return exerciseEvent;
  }

  private vendor.google_clockwork.healthservices.DataUpdate getHalHealthEventDataUpdate(
      int eventType, long elapsedTimeSinceBootNanos) {
    vendor.google_clockwork.healthservices.DataUpdate halHealthEventUpdate =
        new vendor.google_clockwork.healthservices.DataUpdate();
    vendor.google_clockwork.healthservices.HealthEvent halHealthEvent =
        new vendor.google_clockwork.healthservices.HealthEvent();
    halHealthEvent.type = eventType;
    halHealthEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    halHealthEventUpdate.setHealthEvent(halHealthEvent);
    return halHealthEventUpdate;
  }

  private HealthEvent getHealthEvent(int eventType, long elapsedTimeSinceBootNanos) {
    HealthEvent healthEvent = new HealthEvent();
    healthEvent.type = eventType;
    healthEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    return healthEvent;
  }
}
