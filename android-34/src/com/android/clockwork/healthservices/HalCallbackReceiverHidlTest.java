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
import com.google.android.clockwork.healthservices.types.AvailabilityUpdate;
import com.google.android.clockwork.healthservices.types.DataTypeAvailability;
import com.google.android.clockwork.healthservices.types.DataTypeAvailabilityStatus;
import com.google.android.clockwork.healthservices.types.DataType;
import com.google.android.clockwork.healthservices.types.DataTypeGoal;
import com.google.android.clockwork.healthservices.types.DataTypeOffset;
import com.google.android.clockwork.healthservices.types.DataUpdate;
import com.google.android.clockwork.healthservices.types.Gender;
import com.google.android.clockwork.healthservices.types.GoalThreshold;
import com.google.android.clockwork.healthservices.types.GoalThresholdType;
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
import com.google.android.clockwork.healthservices.types.TrackingConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public final class HalCallbackReceiverHidlTest {
  @Mock HalAdapter.HalListener mockHalListener;

  private HalCallbackReceiverHidl mHalCallbackReceiverV0;

  @Before
  public void setUp() {
    initMocks(this);

    mHalCallbackReceiverV0 = new HalCallbackReceiverHidl(mockHalListener);
  }

  @Test
  public void onDataUpdate_metricIntUpdate_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.V1_0.DataUpdate halMetricUpdate =
        getHalIntMetricDataUpdate(DataType.EXERCISE_STEP_COUNT, 100, 123);
    ArrayList<vendor.google_clockwork.healthservices.V1_0.DataUpdate> updates =
        new ArrayList<>(Arrays.asList(halMetricUpdate));

    mHalCallbackReceiverV0.onDataUpdate(updates);

    DataUpdate expectedMetricDataUpdate =
        getIntMetricDataUpdate(DataType.EXERCISE_STEP_COUNT, 100, 123);
    DataUpdate[] expectedDataUpdates = {expectedMetricDataUpdate};
    verify(mockHalListener).onDataUpdate(expectedDataUpdates);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onAutoExerciseEvent(any());
  }

  @Test
  public void onDataUpdate_metricFloatUpdate_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.V1_0.DataUpdate halMetricUpdate =
        getHalFloatMetricDataUpdate(DataType.EXERCISE_DISTANCE, 20f, 123);
    ArrayList<vendor.google_clockwork.healthservices.V1_0.DataUpdate> updates =
        new ArrayList<>(Arrays.asList(halMetricUpdate));

    mHalCallbackReceiverV0.onDataUpdate(updates);

    DataUpdate expectedMetricDataUpdate =
        getFloatMetricDataUpdate(DataType.EXERCISE_DISTANCE, 20f, 123);
    DataUpdate[] expectedDataUpdates = {expectedMetricDataUpdate};
    verify(mockHalListener).onDataUpdate(expectedDataUpdates);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onAutoExerciseEvent(any());
  }

  @Test
  public void onDataUpdate_locationUpdate_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.V1_0.DataUpdate halLocationUpdate =
        getHalLocationDataUpdate(37, -122, 10, 20, 123);
    ArrayList<vendor.google_clockwork.healthservices.V1_0.DataUpdate> updates =
        new ArrayList<>(Arrays.asList(halLocationUpdate));

    mHalCallbackReceiverV0.onDataUpdate(updates);

    DataUpdate expectedLocationDataUpdate = getLocationDataUpdate(37, -122, 10, 20, 123);
    DataUpdate[] expectedDataUpdates = {expectedLocationDataUpdate};
    verify(mockHalListener).onDataUpdate(expectedDataUpdates);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onAutoExerciseEvent(any());
  }

  @Test
  public void onDataUpdate_autoPauseEvent_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.V1_0.DataUpdate halAutoPauseEventUpdate =
        getHalAutoPauseEventDataUpdate(
            vendor.google_clockwork.healthservices.V1_0.AutoPauseResumeEventType
                .AUTO_PAUSE_DETECTED,
            123);
    ArrayList<vendor.google_clockwork.healthservices.V1_0.DataUpdate> updates =
        new ArrayList<>(Arrays.asList(halAutoPauseEventUpdate));

    mHalCallbackReceiverV0.onDataUpdate(updates);

    AutoExerciseEvent expectedAutoPauseEvent =
        getAutoPauseEvent(AutoPauseResumeEventType.AUTO_PAUSE_DETECTED, 123);
    verify(mockHalListener).onAutoExerciseEvent(expectedAutoPauseEvent);
    verify(mockHalListener, never()).onHealthEventDetected(any());
    verify(mockHalListener, never()).onDataUpdate(any());
  }

  @Test
  public void onDataUpdate_healthEvent_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.V1_0.DataUpdate halHealthEventUpdate =
        getHalHealthEventDataUpdate(
            vendor.google_clockwork.healthservices.V1_0.HealthEventDetectedType.FALL_DETECTED, 456);
    ArrayList<vendor.google_clockwork.healthservices.V1_0.DataUpdate> updates =
        new ArrayList<>(Arrays.asList(halHealthEventUpdate));

    mHalCallbackReceiverV0.onDataUpdate(updates);

    HealthEvent expectedHealthEvent = getHealthEvent(HealthEventDetectedType.FALL_DETECTED, 456);
    verify(mockHalListener).onHealthEventDetected(expectedHealthEvent);
    verify(mockHalListener, never()).onAutoExerciseEvent(any());
    verify(mockHalListener, never()).onDataUpdate(any());
  }

  @Test
  public void onDataUpdate_allUpdatesTogether_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.V1_0.DataUpdate halIntMetricUpdate =
        getHalIntMetricDataUpdate(DataType.EXERCISE_STEP_COUNT, 100, 123);
    vendor.google_clockwork.healthservices.V1_0.DataUpdate halFloatMetricUpdate =
        getHalFloatMetricDataUpdate(DataType.EXERCISE_DISTANCE, 20f, 123);
    vendor.google_clockwork.healthservices.V1_0.DataUpdate halLocationUpdate =
        getHalLocationDataUpdate(37, -122, 10, 20, 123);
    vendor.google_clockwork.healthservices.V1_0.DataUpdate halAutoPauseEventUpdate =
        getHalAutoPauseEventDataUpdate(
            vendor.google_clockwork.healthservices.V1_0.AutoPauseResumeEventType
                .AUTO_PAUSE_DETECTED,
            123);
    vendor.google_clockwork.healthservices.V1_0.DataUpdate halHealthEventUpdate =
        getHalHealthEventDataUpdate(
            vendor.google_clockwork.healthservices.V1_0.HealthEventDetectedType.FALL_DETECTED, 456);
    ArrayList<vendor.google_clockwork.healthservices.V1_0.DataUpdate> updates =
        new ArrayList<>(
            Arrays.asList(
                halIntMetricUpdate,
                halFloatMetricUpdate,
                halLocationUpdate,
                halAutoPauseEventUpdate,
                halHealthEventUpdate));

    mHalCallbackReceiverV0.onDataUpdate(updates);

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
    DataUpdate[] expectedDataUpdates = {
      expectedIntMetricDataUpdate, expectedFloatMetricDataUpdate, expectedLocationDataUpdate
    };
    verify(mockHalListener).onDataUpdate(expectedDataUpdates);
  }

  @Test
  public void onAvailabilityUpdate_notifiesHalListener() throws RemoteException {
    vendor.google_clockwork.healthservices.V1_0.AvailabilityUpdate halDataTypeAvailabilityUpdate =
        new vendor.google_clockwork.healthservices.V1_0.AvailabilityUpdate();
    vendor.google_clockwork.healthservices.V1_0.DataTypeAvailability halDataTypeAvailability =
        new vendor.google_clockwork.healthservices.V1_0.DataTypeAvailability();
    halDataTypeAvailability.dataType = DataType.EXERCISE_STEP_COUNT;
    halDataTypeAvailability.elapsedTimeSinceBootNanos = 100;
    halDataTypeAvailability.dataTypeAvailabilityStatus =
        vendor.google_clockwork.healthservices.V1_0.DataTypeAvailabilityStatus.AVAILABLE;
    halDataTypeAvailabilityUpdate.dataTypeAvailability(halDataTypeAvailability);
    vendor.google_clockwork.healthservices.V1_0.AvailabilityUpdate halLocationAvailabilityUpdate =
        new vendor.google_clockwork.healthservices.V1_0.AvailabilityUpdate();
    vendor.google_clockwork.healthservices.V1_0.LocationAvailability halLocationAvailability =
        new vendor.google_clockwork.healthservices.V1_0.LocationAvailability();
    halLocationAvailability.elapsedTimeSinceBootNanos = 200;
    halLocationAvailability.locationAvailabilityStatus =
        vendor.google_clockwork.healthservices.V1_0.LocationAvailabilityStatus.ACQUIRED_UNTETHERED;
    halLocationAvailabilityUpdate.locationAvailability(halLocationAvailability);
    ArrayList<vendor.google_clockwork.healthservices.V1_0.AvailabilityUpdate> updates =
        new ArrayList<>(
            Arrays.asList(halDataTypeAvailabilityUpdate, halLocationAvailabilityUpdate));

    mHalCallbackReceiverV0.onAvailabilityUpdate(updates);

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
    vendor.google_clockwork.healthservices.V1_0.AchievedGoal halIntAchievedGoal =
        new vendor.google_clockwork.healthservices.V1_0.AchievedGoal();
    vendor.google_clockwork.healthservices.V1_0.DataTypeGoal halIntDataTypeGoal =
        new vendor.google_clockwork.healthservices.V1_0.DataTypeGoal();
    halIntDataTypeGoal.dataType = DataType.EXERCISE_STEP_COUNT;
    halIntDataTypeGoal.threshold =
        new vendor.google_clockwork.healthservices.V1_0.DataTypeGoal.GoalThreshold();
    halIntDataTypeGoal.threshold.intThreshold(100);
    halIntAchievedGoal.goal = halIntDataTypeGoal;
    halIntAchievedGoal.elapsedTimeSinceBootNanos = 123;
    vendor.google_clockwork.healthservices.V1_0.AchievedGoal halFloatAchievedGoal =
        new vendor.google_clockwork.healthservices.V1_0.AchievedGoal();
    vendor.google_clockwork.healthservices.V1_0.DataTypeGoal halFloatDataTypeGoal =
        new vendor.google_clockwork.healthservices.V1_0.DataTypeGoal();
    halFloatDataTypeGoal.dataType = DataType.EXERCISE_DISTANCE;
    halFloatDataTypeGoal.threshold =
        new vendor.google_clockwork.healthservices.V1_0.DataTypeGoal.GoalThreshold();
    halFloatDataTypeGoal.threshold.floatThreshold(20f);
    halFloatAchievedGoal.goal = halFloatDataTypeGoal;
    halFloatAchievedGoal.elapsedTimeSinceBootNanos = 456;
    ArrayList<vendor.google_clockwork.healthservices.V1_0.AchievedGoal> updates =
        new ArrayList<>(Arrays.asList(halIntAchievedGoal, halFloatAchievedGoal));

    mHalCallbackReceiverV0.onGoalAchieved(updates);

    AchievedGoal intAchievedGoal = new AchievedGoal();
    DataTypeGoal intDataTypeGoal = new DataTypeGoal();
    intDataTypeGoal.dataType = DataType.EXERCISE_STEP_COUNT;
    intDataTypeGoal.threshold = new GoalThreshold();
    intDataTypeGoal.threshold.type = GoalThresholdType.INT;
    intDataTypeGoal.threshold.intValue = 100;
    intDataTypeGoal.comparisonType = DataTypeGoal.ComparisonType.GREATER_THAN_OR_EQUAL;
    intAchievedGoal.goal = intDataTypeGoal;
    intAchievedGoal.elapsedTimeSinceBootNanos = 123;
    AchievedGoal floatAchievedGoal = new AchievedGoal();
    DataTypeGoal floatDataTypeGoal = new DataTypeGoal();
    floatDataTypeGoal.dataType = DataType.EXERCISE_DISTANCE;
    floatDataTypeGoal.threshold = new GoalThreshold();
    floatDataTypeGoal.threshold.type = GoalThresholdType.FLOAT;
    floatDataTypeGoal.threshold.floatValue = 20f;
    floatDataTypeGoal.comparisonType = DataTypeGoal.ComparisonType.GREATER_THAN_OR_EQUAL;
    floatAchievedGoal.goal = floatDataTypeGoal;
    floatAchievedGoal.elapsedTimeSinceBootNanos = 456;
    AchievedGoal[] expectedUpdates = {intAchievedGoal, floatAchievedGoal};
    verify(mockHalListener).onGoalAchieved(expectedUpdates);
  }

  @Test
  public void onFlushCompleted_notifiesHalListener_singleCallback() throws RemoteException {
    mHalCallbackReceiverV0.onFlushCompleted();

    verify(mockHalListener).onFlushCompleted(/* flushId= */ 0);
  }

  @Test
  public void onFlushCompleted_notifiesHalListener_multiCallback() throws RemoteException {
    mHalCallbackReceiverV0.onFlushCompleted();
    mHalCallbackReceiverV0.onFlushCompleted();
    mHalCallbackReceiverV0.onFlushCompleted();

    verify(mockHalListener).onFlushCompleted(/* flushId= */ 0);
    verify(mockHalListener).onFlushCompleted(/* flushId= */ 1);
    verify(mockHalListener).onFlushCompleted(/* flushId= */ 2);
  }

  @Test
  public void onFlushCompleted_notifiesHalListener_flushIdWrapsAtMaxInt() throws RemoteException {
    mHalCallbackReceiverV0.nextFlushId = Integer.MAX_VALUE;
    mHalCallbackReceiverV0.onFlushCompleted();
    mHalCallbackReceiverV0.onFlushCompleted();
    mHalCallbackReceiverV0.onFlushCompleted();

    verify(mockHalListener).onFlushCompleted(/* flushId= */ Integer.MAX_VALUE);
    verify(mockHalListener).onFlushCompleted(/* flushId= */ 0);
    verify(mockHalListener).onFlushCompleted(/* flushId= */ 1);
  }

  private vendor.google_clockwork.healthservices.V1_0.DataUpdate getHalFloatMetricDataUpdate(
      int dataType, float value, long elapsedTimeSinceBootNanos) {
    vendor.google_clockwork.healthservices.V1_0.MetricData halMetricData =
        new vendor.google_clockwork.healthservices.V1_0.MetricData();
    halMetricData.dataType = dataType;
    halMetricData.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    vendor.google_clockwork.healthservices.V1_0.MetricData.MetricValue halMetricValue =
        new vendor.google_clockwork.healthservices.V1_0.MetricData.MetricValue();
    halMetricValue.floatValue(value);
    halMetricData.metricValue = halMetricValue;
    halMetricData.accuracy =
        vendor.google_clockwork.healthservices.V1_0.MetricAccuracy.ACCURACY_MEDIUM;

    vendor.google_clockwork.healthservices.V1_0.DataUpdate halMetricDataUpdate =
        new vendor.google_clockwork.healthservices.V1_0.DataUpdate();
    halMetricDataUpdate.metricData(halMetricData);
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

  private vendor.google_clockwork.healthservices.V1_0.DataUpdate getHalIntMetricDataUpdate(
      int dataType, int value, long elapsedTimeSinceBootNanos) {
    vendor.google_clockwork.healthservices.V1_0.MetricData halMetricData =
        new vendor.google_clockwork.healthservices.V1_0.MetricData();
    halMetricData.dataType = dataType;
    halMetricData.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    vendor.google_clockwork.healthservices.V1_0.MetricData.MetricValue halMetricValue =
        new vendor.google_clockwork.healthservices.V1_0.MetricData.MetricValue();
    halMetricValue.intValue(value);
    halMetricData.metricValue = halMetricValue;
    halMetricData.accuracy =
        vendor.google_clockwork.healthservices.V1_0.MetricAccuracy.ACCURACY_MEDIUM;

    vendor.google_clockwork.healthservices.V1_0.DataUpdate halMetricDataUpdate =
        new vendor.google_clockwork.healthservices.V1_0.DataUpdate();
    halMetricDataUpdate.metricData(halMetricData);
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

  private vendor.google_clockwork.healthservices.V1_0.DataUpdate getHalLocationDataUpdate(
      double latitude,
      double longitude,
      float horizontalPositionErrorMeters,
      float bearing,
      long elapsedTimeSinceBootNanos) {
    vendor.google_clockwork.healthservices.V1_0.DataUpdate halLocationDataUpdate =
        new vendor.google_clockwork.healthservices.V1_0.DataUpdate();
    vendor.google_clockwork.healthservices.V1_0.LocationData halLocationData =
        new vendor.google_clockwork.healthservices.V1_0.LocationData();
    halLocationData.latitude = latitude;
    halLocationData.longitude = longitude;
    halLocationData.horizontalPositionErrorMeters = horizontalPositionErrorMeters;
    halLocationData.bearing = bearing;
    halLocationData.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    halLocationDataUpdate.locationData(halLocationData);
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

  private vendor.google_clockwork.healthservices.V1_0.DataUpdate getHalAutoPauseEventDataUpdate(
      byte eventType, long elapsedTimeSinceBootNanos) {
    vendor.google_clockwork.healthservices.V1_0.DataUpdate halAutoPauseEventUpdate =
        new vendor.google_clockwork.healthservices.V1_0.DataUpdate();
    vendor.google_clockwork.healthservices.V1_0.AutoPauseEvent halAutoPauseEvent =
        new vendor.google_clockwork.healthservices.V1_0.AutoPauseEvent();
    halAutoPauseEvent.type = eventType;
    halAutoPauseEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    halAutoPauseEventUpdate.autoPauseEvent(halAutoPauseEvent);
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

  private vendor.google_clockwork.healthservices.V1_0.DataUpdate getHalHealthEventDataUpdate(
      int eventType, long elapsedTimeSinceBootNanos) {
    vendor.google_clockwork.healthservices.V1_0.DataUpdate halHealthEventUpdate =
        new vendor.google_clockwork.healthservices.V1_0.DataUpdate();
    vendor.google_clockwork.healthservices.V1_0.HealthEvent halHealthEvent =
        new vendor.google_clockwork.healthservices.V1_0.HealthEvent();
    halHealthEvent.type = eventType;
    halHealthEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    halHealthEventUpdate.healthEvent(halHealthEvent);
    return halHealthEventUpdate;
  }

  private HealthEvent getHealthEvent(int eventType, long elapsedTimeSinceBootNanos) {
    HealthEvent healthEvent = new HealthEvent();
    healthEvent.type = eventType;
    healthEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    return healthEvent;
  }
}
