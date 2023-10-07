package com.android.clockwork.healthservices;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.hidl.manager.V1_0.IServiceManager;
import android.os.IBinder;
import android.os.HwBinder;
import android.os.RemoteException;
import android.os.ServiceSpecificException;

import com.android.clockwork.healthservices.HealthService.BinderService;
import com.android.server.SystemService;

import com.google.android.clockwork.healthservices.types.AutoStartConfig;
import com.google.android.clockwork.healthservices.types.AutoStartEvent;
import com.google.android.clockwork.healthservices.types.DataType;
import com.google.android.clockwork.healthservices.types.DataTypeGoal;
import com.google.android.clockwork.healthservices.types.DataTypeOffset;
import com.google.android.clockwork.healthservices.types.ExerciseType;
import com.google.android.clockwork.healthservices.types.Gender;
import com.google.android.clockwork.healthservices.types.GoalThreshold;
import com.google.android.clockwork.healthservices.types.GoalThresholdType;
import com.google.android.clockwork.healthservices.types.GolfShotDetectionParams;
import com.google.android.clockwork.healthservices.types.OffsetValue;
import com.google.android.clockwork.healthservices.types.OffsetValueType;
import com.google.android.clockwork.healthservices.types.StatsOffsetValue;
import com.google.android.clockwork.healthservices.types.StatsValue;
import com.google.android.clockwork.healthservices.types.TrackingConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public final class HalAdapterTest {
  @Mock vendor.google_clockwork.healthservices.V1_0.IHealthServices mockHealthServicesHalV0;
  @Mock vendor.google_clockwork.healthservices.IHealthServices mockHealthServicesHalV1;
  @Mock HalAdapter.HalListener mockHalListener;
  @Mock IBinder mockIBinder;
  @Mock HwBinder mockHwBinder;
  @Mock IServiceManager mockServiceManager;
  @Mock HalAdapter.ServiceManagerStub mockServiceManagerStub;

  private HalAdapter mHalAdapter;

  @Before
  public void setUp() {
    initMocks(this);

    mHalAdapter = new HalAdapter(mockHalListener, mockServiceManagerStub);
  }

  @Test
  public void maybeGetAidlHalService_halV1Connected() throws RemoteException {
    when(mockServiceManagerStub.waitForDeclaredService()).thenReturn(mockHealthServicesHalV1);
    when(mockHealthServicesHalV1.asBinder()).thenReturn(mockIBinder);

    mHalAdapter.maybeGetAidlHalService();

    assertThat(mHalAdapter.isHalServiceConnected()).isTrue();
    verify(mockHealthServicesHalV1).getInterfaceVersion();
    verify(mockHealthServicesHalV1).setDataListener(any());
    verify(mockIBinder).linkToDeath(mHalAdapter, 0);
    verify(mockHalListener).onHalConnected();
  }

  @Test
  public void maybeGetAidlHalService_halV1NotConnected() throws RemoteException {
    when(mockServiceManagerStub.waitForDeclaredService()).thenReturn(null);

    mHalAdapter.maybeGetAidlHalService();

    assertThat(mHalAdapter.isHalServiceConnected()).isFalse();
    verify(mockHealthServicesHalV1, never()).getInterfaceVersion();
    verify(mockHealthServicesHalV1, never()).setDataListener(any());
    verify(mockIBinder, never()).linkToDeath(any(), anyInt());
    verify(mockHalListener, never()).onHalConnected();
  }

  @Test
  public void registerHalV1_0RegistrationNotification_registers() throws RemoteException {
    when(mockServiceManagerStub.getIServiceManager()).thenReturn(mockServiceManager);

    mHalAdapter.registerHalV1_0RegistrationNotification();

    assertThat(mHalAdapter.isHalServiceConnected()).isFalse();
    verify(mockServiceManager)
        .registerForNotifications(
            "vendor.google_clockwork.healthservices@1.0::IHealthServices", "", mHalAdapter);
  }

  @Test
  public void onRegistration_success() throws RemoteException {
    when(mockServiceManagerStub.getV0HalService()).thenReturn(mockHealthServicesHalV0);

    mHalAdapter.onRegistration(
        "vendor.google_clockwork.healthservices@1.0::IHealthServices", "default", true);

    assertThat(mHalAdapter.isHalServiceConnected()).isTrue();
    verify(mockHealthServicesHalV0).setDataListener(any());
    verify(mockHealthServicesHalV0).linkToDeath(mHalAdapter, 1);
    verify(mockHalListener).onHalConnected();
  }

  @Test
  public void serviceDied_notifiesHalListener() throws RemoteException {
    registerAndConnectV0Hal();
    when(mockHealthServicesHalV0.asBinder()).thenReturn(mockHwBinder);

    mHalAdapter.serviceDied(1);

    verify(mockHwBinder).unlinkToDeath(mHalAdapter);
    verify(mockHalListener).onHalDied();
  }

  @Test
  public void binderDied_notifiesHalListener() throws RemoteException {
    registerAndConnectV1Hal();

    mHalAdapter.binderDied();

    verify(mockIBinder).unlinkToDeath(mHalAdapter, 0);
    verify(mockHalListener).onHalDied();
  }

  @Test
  public void getSupportedDataTypes_halIsNotConnected() {
    int[] supportedDataTypes = mHalAdapter.getSupportedDataTypes();

    assertThat(supportedDataTypes.length).isEqualTo(0);
  }

  @Test
  public void getSupportedDataTypes_halV0Connected() throws RemoteException {
    ArrayList<Integer> mockedReturnData =
        new ArrayList<>(Arrays.asList(DataType.EXERCISE_STEP_COUNT, DataType.PASSIVE_STEP_COUNT));
    when(mockHealthServicesHalV0.getSupportedMetrics()).thenReturn(mockedReturnData);
    registerAndConnectV0Hal();

    int[] supportedDataTypes = mHalAdapter.getSupportedDataTypes();

    int[] expectedDataTypes = {DataType.EXERCISE_STEP_COUNT, DataType.PASSIVE_STEP_COUNT};
    assertTrue(Arrays.equals(supportedDataTypes, expectedDataTypes));
  }

  @Test
  public void getSupportedDataTypes_halV1Connected() throws RemoteException {
    int[] expectedDataTypes = {DataType.EXERCISE_STEP_COUNT, DataType.PASSIVE_STEP_COUNT};
    when(mockHealthServicesHalV1.getSupportedMetrics()).thenReturn(expectedDataTypes);
    registerAndConnectV1Hal();

    int[] supportedDataTypes = mHalAdapter.getSupportedDataTypes();

    assertTrue(Arrays.equals(supportedDataTypes, expectedDataTypes));
  }

  @Test
  public void getSupportedGoals_halIsNotConnected() {
    DataTypeGoal[] supportedDataTypeGoals = mHalAdapter.getSupportedGoals();

    assertThat(supportedDataTypeGoals.length).isEqualTo(0);
  }

  @Test
  public void getSupportedGoals_halV0Connected() throws RemoteException {
    registerAndConnectV0Hal();
    vendor.google_clockwork.healthservices.V1_0.DataTypeGoal halDataTypeGoal =
        getHalV0DataTypeGoal(DataType.EXERCISE_STEP_COUNT, 20);
    ArrayList<vendor.google_clockwork.healthservices.V1_0.DataTypeGoal> mockedReturnData =
        new ArrayList();
    mockedReturnData.add(halDataTypeGoal);
    when(mockHealthServicesHalV0.getSupportedGoals()).thenReturn(mockedReturnData);

    DataTypeGoal[] supportedDataTypeGoals = mHalAdapter.getSupportedGoals();

    DataTypeGoal dataTypeGoal =
        getDataTypeGoal(
            DataType.EXERCISE_STEP_COUNT, 20, DataTypeGoal.ComparisonType.GREATER_THAN_OR_EQUAL);
    DataTypeGoal[] expectedGoals = {dataTypeGoal};
    assertThat(supportedDataTypeGoals.length).isEqualTo(1);
    assertTrue(Arrays.equals(supportedDataTypeGoals, expectedGoals));
  }

  @Test
  public void getSupportedGoals_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    vendor.google_clockwork.healthservices.DataTypeGoal halDataTypeGoal =
        getHalV1DataTypeGoal(
            DataType.EXERCISE_STEP_COUNT,
            20,
            vendor.google_clockwork.healthservices.DataTypeGoal.ComparisonType.GREATER_THAN);
    vendor.google_clockwork.healthservices.DataTypeGoal[] mockedReturnData = {halDataTypeGoal};
    when(mockHealthServicesHalV1.getSupportedGoals()).thenReturn(mockedReturnData);

    DataTypeGoal[] supportedDataTypeGoals = mHalAdapter.getSupportedGoals();

    DataTypeGoal dataTypeGoal =
        getDataTypeGoal(DataType.EXERCISE_STEP_COUNT, 20, DataTypeGoal.ComparisonType.GREATER_THAN);
    DataTypeGoal[] expectedGoals = {dataTypeGoal};
    assertThat(supportedDataTypeGoals.length).isEqualTo(1);
    assertThat(supportedDataTypeGoals[0]).isEqualTo(expectedGoals[0]);
  }

  @Test
  public void addGoal_halV0Connected() throws RemoteException {
    registerAndConnectV0Hal();
    DataTypeGoal dataTypeGoal =
        getDataTypeGoal(DataType.EXERCISE_STEP_COUNT, 20, DataTypeGoal.ComparisonType.GREATER_THAN);

    mHalAdapter.addGoal(dataTypeGoal);

    vendor.google_clockwork.healthservices.V1_0.DataTypeGoal halDataTypeGoal =
        getHalV0DataTypeGoal(DataType.EXERCISE_STEP_COUNT, 20);
    verify(mockHealthServicesHalV0).addGoal(halDataTypeGoal);
  }

  @Test
  public void addGoal_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    DataTypeGoal dataTypeGoal =
        getDataTypeGoal(DataType.EXERCISE_STEP_COUNT, 20, DataTypeGoal.ComparisonType.GREATER_THAN);

    mHalAdapter.addGoal(dataTypeGoal);

    vendor.google_clockwork.healthservices.DataTypeGoal halDataTypeGoal =
        getHalV1DataTypeGoal(
            DataType.EXERCISE_STEP_COUNT,
            20,
            vendor.google_clockwork.healthservices.DataTypeGoal.ComparisonType.GREATER_THAN);
    verify(mockHealthServicesHalV1).addGoal(halDataTypeGoal);
  }

  @Test
  public void removeGoal_halV0Connected() throws RemoteException {
    registerAndConnectV0Hal();
    DataTypeGoal dataTypeGoal =
        getDataTypeGoal(DataType.EXERCISE_STEP_COUNT, 20, DataTypeGoal.ComparisonType.GREATER_THAN);

    mHalAdapter.removeGoal(dataTypeGoal);

    vendor.google_clockwork.healthservices.V1_0.DataTypeGoal halDataTypeGoal =
        getHalV0DataTypeGoal(DataType.EXERCISE_STEP_COUNT, 20);
    verify(mockHealthServicesHalV0).removeGoal(halDataTypeGoal);
  }

  @Test
  public void removeGoal_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    DataTypeGoal dataTypeGoal =
        getDataTypeGoal(DataType.EXERCISE_STEP_COUNT, 20, DataTypeGoal.ComparisonType.GREATER_THAN);

    mHalAdapter.removeGoal(dataTypeGoal);

    vendor.google_clockwork.healthservices.DataTypeGoal halDataTypeGoal =
        getHalV1DataTypeGoal(
            DataType.EXERCISE_STEP_COUNT,
            20,
            vendor.google_clockwork.healthservices.DataTypeGoal.ComparisonType.GREATER_THAN);
    verify(mockHealthServicesHalV1).removeGoal(halDataTypeGoal);
  }

  @Test
  public void removeGoal_halV1Connected_handlesHalException() throws RemoteException {
    registerAndConnectV1Hal();
    DataTypeGoal dataTypeGoal =
        getDataTypeGoal(DataType.EXERCISE_STEP_COUNT, 20, DataTypeGoal.ComparisonType.GREATER_THAN);
    vendor.google_clockwork.healthservices.DataTypeGoal halDataTypeGoal =
        getHalV1DataTypeGoal(
            DataType.EXERCISE_STEP_COUNT,
            20,
            vendor.google_clockwork.healthservices.DataTypeGoal.ComparisonType.GREATER_THAN);
    doThrow(IllegalArgumentException.class)
        .when(mockHealthServicesHalV1)
        .removeGoal(halDataTypeGoal);

    mHalAdapter.removeGoal(dataTypeGoal);
  }

  @Test
  public void startTracking_noOffsets_halV0Connected() throws RemoteException {
    registerAndConnectV0Hal();
    int maxReportLatencyMs = 120000;
    int samplingPeriodMs = 60000;
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT, DataType.HEART_RATE_BPM};
    TrackingConfig config =
        getTrackingConfig(
            DataType.EXERCISE_STEP_COUNT, false, maxReportLatencyMs, samplingPeriodMs);
    DataTypeOffset[] emptyOffsets = {};
    boolean isPaused = false;

    mHalAdapter.startTracking(config, dataTypes, emptyOffsets, isPaused);

    ArrayList<Integer> dataTypesList =
        new ArrayList<>(Arrays.asList(DataType.EXERCISE_STEP_COUNT, DataType.HEART_RATE_BPM));
    vendor.google_clockwork.healthservices.V1_0.TrackingConfig halTrackingConfig =
        getHalV0TrackingConfig(
            DataType.EXERCISE_STEP_COUNT, false, maxReportLatencyMs, samplingPeriodMs);
    verify(mockHealthServicesHalV0)
        .startTracking(halTrackingConfig, dataTypesList, new ArrayList());
  }

  @Test
  public void startTracking_withOffsets_halV0Connected() throws RemoteException {
    registerAndConnectV0Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT, DataType.HEART_RATE_BPM};
    TrackingConfig config = getTrackingConfig(ExerciseType.WALKING, false, 0, 0);
    DataTypeOffset[] offsets = {
      getDataTypeOffset(DataType.EXERCISE_STEP_COUNT, 100),
    };
    boolean isPaused = false;

    mHalAdapter.startTracking(config, dataTypes, offsets, isPaused);

    ArrayList<Integer> dataTypesList =
        new ArrayList<>(Arrays.asList(DataType.EXERCISE_STEP_COUNT, DataType.HEART_RATE_BPM));
    vendor.google_clockwork.healthservices.V1_0.TrackingConfig halTrackingConfig =
        getHalV0TrackingConfig(ExerciseType.WALKING, false, 0, 0);
    ArrayList<vendor.google_clockwork.healthservices.V1_0.DataTypeOffset> halDataTypesOffsets =
        new ArrayList<>(Arrays.asList(getHalV0DataTypeOffset(DataType.EXERCISE_STEP_COUNT, 100)));
    verify(mockHealthServicesHalV0)
        .startTracking(halTrackingConfig, dataTypesList, halDataTypesOffsets);
  }

  @Test
  public void startTracking_isPaused_halV0Connected() throws RemoteException {
    registerAndConnectV0Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT, DataType.HEART_RATE_BPM};
    TrackingConfig config = getTrackingConfig(DataType.EXERCISE_STEP_COUNT, false, 0, 0);
    DataTypeOffset[] offsets = {getDataTypeOffset(DataType.EXERCISE_STEP_COUNT, 100)};
    boolean isPaused = true;

    mHalAdapter.startTracking(config, dataTypes, offsets, isPaused);

    ArrayList<Integer> dataTypesList =
        new ArrayList<>(Arrays.asList(DataType.EXERCISE_STEP_COUNT, DataType.HEART_RATE_BPM));
    vendor.google_clockwork.healthservices.V1_0.TrackingConfig halTrackingConfig =
        getHalV0TrackingConfig(DataType.EXERCISE_STEP_COUNT, false, 0, 0);
    ArrayList<vendor.google_clockwork.healthservices.V1_0.DataTypeOffset> halDataTypesOffsets =
        new ArrayList<>(Arrays.asList(getHalV0DataTypeOffset(DataType.EXERCISE_STEP_COUNT, 100)));
    verify(mockHealthServicesHalV0)
        .startTracking(halTrackingConfig, dataTypesList, halDataTypesOffsets);
    verify(mockHealthServicesHalV0).pauseTracking(dataTypesList);
  }

  @Test
  public void startTracking_noOffsets_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    int maxReportLatencyMs = 120000;
    int samplingPeriodMs = 60000;
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT, DataType.HEART_RATE_BPM};
    TrackingConfig config =
        getTrackingConfig(
            DataType.EXERCISE_STEP_COUNT, false, maxReportLatencyMs, samplingPeriodMs);
    DataTypeOffset[] emptyOffsets = {};
    boolean isPaused = false;

    mHalAdapter.startTracking(config, dataTypes, emptyOffsets, isPaused);

    vendor.google_clockwork.healthservices.TrackingConfig halTrackingConfig =
        getHalV1TrackingConfig(
            DataType.EXERCISE_STEP_COUNT, false, maxReportLatencyMs, samplingPeriodMs);
    vendor.google_clockwork.healthservices.DataTypeOffset[] emptyHalOffsets = {};
    verify(mockHealthServicesHalV1)
        .startTracking(halTrackingConfig, dataTypes, emptyHalOffsets, isPaused);
  }

  @Test
  public void startTracking_withOffsets_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    int[] dataTypes = {
      DataType.EXERCISE_STEP_COUNT,
      DataType.HEART_RATE_BPM,
      DataType.STEPS_PER_MINUTE_STATS,
      DataType.HEART_RATE_BPM_STATS
    };
    TrackingConfig config = getTrackingConfig(ExerciseType.WALKING, false, 0, 0);
    DataTypeOffset[] offsets = {
      getDataTypeOffset(DataType.EXERCISE_STEP_COUNT, 100),
      getIntStatsOffset(DataType.STEPS_PER_MINUTE_STATS, 200, 500, 300, 10, 123),
      getFloatStatsOffset(DataType.HEART_RATE_BPM_STATS, 80.2f, 98.4f, 90.3f, 33, 123)
    };
    boolean isPaused = true;

    mHalAdapter.startTracking(config, dataTypes, offsets, isPaused);

    vendor.google_clockwork.healthservices.TrackingConfig halTrackingConfig =
        getHalV1TrackingConfig(ExerciseType.WALKING, false, 0, 0);
    vendor.google_clockwork.healthservices.DataTypeOffset[] halDataTypesOffsets = {
      getHalV1DataTypeOffset(DataType.EXERCISE_STEP_COUNT, 100),
      getHalV1StatsDataTypeOffset(DataType.STEPS_PER_MINUTE_STATS, 200, 500, 300, 10, 123),
      getHalV1StatsDataTypeOffset(DataType.HEART_RATE_BPM_STATS, 80.2f, 98.4f, 90.3f, 33, 123)
    };
    verify(mockHealthServicesHalV1)
        .startTracking(halTrackingConfig, dataTypes, halDataTypesOffsets, isPaused);
  }

  @Test
  public void startTracking_halV1Connected_handlesExpectedHalException() throws RemoteException {
    registerAndConnectV1Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT, DataType.HEART_RATE_BPM};
    TrackingConfig config = getTrackingConfig(DataType.EXERCISE_STEP_COUNT, false, 0, 0);
    DataTypeOffset[] offsets = {getDataTypeOffset(DataType.EXERCISE_STEP_COUNT, 100)};
    boolean isPaused = true;
    vendor.google_clockwork.healthservices.TrackingConfig halTrackingConfig =
        getHalV1TrackingConfig(DataType.EXERCISE_STEP_COUNT, false, 0, 0);
    vendor.google_clockwork.healthservices.DataTypeOffset[] halDataTypesOffsets = {
      getHalV1DataTypeOffset(DataType.EXERCISE_STEP_COUNT, 100)
    };
    doThrow(UnsupportedOperationException.class)
        .when(mockHealthServicesHalV1)
        .startTracking(halTrackingConfig, dataTypes, halDataTypesOffsets, isPaused);

    mHalAdapter.startTracking(config, dataTypes, offsets, isPaused);
  }

  @Test
  public void startTracking_halV1Connected_handlesUnexpectedHalException() throws RemoteException {
    registerAndConnectV1Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT, DataType.HEART_RATE_BPM};
    TrackingConfig config = getTrackingConfig(DataType.EXERCISE_STEP_COUNT, false, 0, 0);
    DataTypeOffset[] offsets = {getDataTypeOffset(DataType.EXERCISE_STEP_COUNT, 100)};
    boolean isPaused = true;
    vendor.google_clockwork.healthservices.TrackingConfig halTrackingConfig =
        getHalV1TrackingConfig(DataType.EXERCISE_STEP_COUNT, false, 0, 0);
    vendor.google_clockwork.healthservices.DataTypeOffset[] halDataTypesOffsets = {
      getHalV1DataTypeOffset(DataType.EXERCISE_STEP_COUNT, 100)
    };
    doThrow(ServiceSpecificException.class)
        .when(mockHealthServicesHalV1)
        .startTracking(halTrackingConfig, dataTypes, halDataTypesOffsets, isPaused);

    mHalAdapter.startTracking(config, dataTypes, offsets, isPaused);
  }

  @Test
  public void updateTrackingConfig_halV0Connected() throws RemoteException {
    registerAndConnectV0Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};
    TrackingConfig config = getTrackingConfig(DataType.EXERCISE_STEP_COUNT, false, 0, 0);

    mHalAdapter.updateTrackingConfig(config, dataTypes);

    ArrayList<Integer> dataTypesList = new ArrayList<>(Arrays.asList(DataType.EXERCISE_STEP_COUNT));
    vendor.google_clockwork.healthservices.V1_0.TrackingConfig halTrackingConfig =
        getHalV0TrackingConfig(DataType.EXERCISE_STEP_COUNT, false, 0, 0);
    verify(mockHealthServicesHalV0).updateTrackingConfig(halTrackingConfig, dataTypesList);
  }

  @Test
  public void updateTrackingConfig_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};
    TrackingConfig config = getTrackingConfig(DataType.EXERCISE_STEP_COUNT, false, 0, 0);

    mHalAdapter.updateTrackingConfig(config, dataTypes);

    vendor.google_clockwork.healthservices.TrackingConfig halTrackingConfig =
        getHalV1TrackingConfig(DataType.EXERCISE_STEP_COUNT, false, 0, 0);
    verify(mockHealthServicesHalV1).updateTrackingConfig(halTrackingConfig, dataTypes);
  }

  @Test
  public void pauseTracking_halV0Connected() throws RemoteException {
    registerAndConnectV0Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};

    mHalAdapter.pauseTracking(dataTypes);

    ArrayList<Integer> dataTypesList = new ArrayList<>(Arrays.asList(DataType.EXERCISE_STEP_COUNT));
    verify(mockHealthServicesHalV0).pauseTracking(dataTypesList);
  }

  @Test
  public void pauseTracking_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};

    mHalAdapter.pauseTracking(dataTypes);

    verify(mockHealthServicesHalV1).pauseTracking(dataTypes);
  }

  @Test
  public void pauseTracking_halV1Connected_handlesHalException() throws RemoteException {
    registerAndConnectV1Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};
    doThrow(IllegalArgumentException.class).when(mockHealthServicesHalV1).pauseTracking(dataTypes);

    mHalAdapter.pauseTracking(dataTypes);
  }

  @Test
  public void resumeTracking_halV0Connected() throws RemoteException {
    registerAndConnectV0Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};

    mHalAdapter.resumeTracking(dataTypes);

    ArrayList<Integer> dataTypesList = new ArrayList<>(Arrays.asList(DataType.EXERCISE_STEP_COUNT));
    verify(mockHealthServicesHalV0).resumeTracking(dataTypesList);
  }

  @Test
  public void resumeTracking_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};

    mHalAdapter.resumeTracking(dataTypes);

    verify(mockHealthServicesHalV1).resumeTracking(dataTypes);
  }

  @Test
  public void resumeTracking_halV1Connected_handlesHalException() throws RemoteException {
    registerAndConnectV1Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};
    doThrow(IllegalArgumentException.class).when(mockHealthServicesHalV1).resumeTracking(dataTypes);

    mHalAdapter.resumeTracking(dataTypes);
  }

  @Test
  public void stopTracking_halV0Connected() throws RemoteException {
    registerAndConnectV0Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};

    mHalAdapter.stopTracking(dataTypes);

    ArrayList<Integer> dataTypesList = new ArrayList<>(Arrays.asList(DataType.EXERCISE_STEP_COUNT));
    verify(mockHealthServicesHalV0).stopTracking(dataTypesList);
  }

  @Test
  public void stopTracking_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};

    mHalAdapter.stopTracking(dataTypes);

    verify(mockHealthServicesHalV1).stopTracking(dataTypes);
  }

  @Test
  public void flush_halV0Connected() throws RemoteException {
    registerAndConnectV0Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};

    mHalAdapter.flush(/* flushId= */ 10, dataTypes);

    ArrayList<Integer> dataTypesList = new ArrayList<>(Arrays.asList(DataType.EXERCISE_STEP_COUNT));
    verify(mockHealthServicesHalV0).flush(dataTypesList);
  }

  @Test
  public void flush_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};

    mHalAdapter.flush(/* flushId= */ 10, dataTypes);

    verify(mockHealthServicesHalV1).flush(/* flushId= */ 10, dataTypes);
  }

  @Test
  public void flush_halV1Connected_handlesUnexpectedHalException() throws RemoteException {
    registerAndConnectV1Hal();
    int[] dataTypes = {DataType.EXERCISE_STEP_COUNT};
    doThrow(ServiceSpecificException.class).when(mockHealthServicesHalV1).flush(1, dataTypes);

    mHalAdapter.flush(/* flushId= */ 10, dataTypes);
  }

  @Test
  public void resetDataTypeOffsets_halV0Connected() throws RemoteException {
    registerAndConnectV0Hal();
    int offset = 0;
    DataTypeOffset[] offsets = {getDataTypeOffset(DataType.PASSIVE_STEP_COUNT, offset)};

    mHalAdapter.resetDataTypeOffsets(offsets);

    ArrayList<vendor.google_clockwork.healthservices.V1_0.DataTypeOffset> halDataTypesOffsets =
        new ArrayList<>(Arrays.asList(getHalV0DataTypeOffset(DataType.PASSIVE_STEP_COUNT, offset)));
    verify(mockHealthServicesHalV0).resetDataTypeOffsets(halDataTypesOffsets);
  }

  @Test
  public void resetDataTypeOffsets_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    int offset = 0;
    DataTypeOffset[] offsets = {getDataTypeOffset(DataType.PASSIVE_STEP_COUNT, offset)};

    mHalAdapter.resetDataTypeOffsets(offsets);

    vendor.google_clockwork.healthservices.DataTypeOffset[] halDataTypesOffsets = {
      getHalV1DataTypeOffset(DataType.PASSIVE_STEP_COUNT, offset)
    };
    verify(mockHealthServicesHalV1).resetDataTypeOffsets(halDataTypesOffsets);
  }

  @Test
  public void resetDataTypeOffsets_halV1Connected_handlesHalException() throws RemoteException {
    registerAndConnectV1Hal();
    int offset = 0;
    DataTypeOffset[] offsets = {getDataTypeOffset(DataType.PASSIVE_STEP_COUNT, offset)};
    vendor.google_clockwork.healthservices.DataTypeOffset[] halDataTypesOffsets = {
      getHalV1DataTypeOffset(DataType.PASSIVE_STEP_COUNT, offset)
    };
    doThrow(IllegalArgumentException.class)
        .when(mockHealthServicesHalV1)
        .resetDataTypeOffsets(halDataTypesOffsets);

    mHalAdapter.resetDataTypeOffsets(offsets);
  }

  @Test
  public void setGolfShotDetectionParams_halV0Connected_noOp() throws RemoteException {
    registerAndConnectV0Hal();
    reset(mockHealthServicesHalV0);
    GolfShotDetectionParams golfShotDetectionParams = new GolfShotDetectionParams();
    golfShotDetectionParams.location = GolfShotDetectionParams.GolfCourseLocation.FAIRWAY;

    mHalAdapter.setGolfShotDetectionParams(golfShotDetectionParams);

    verifyNoInteractions(mockHealthServicesHalV0);
  }

  @Test
  public void setGolfShotDetectionParams_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    GolfShotDetectionParams golfShotDetectionParams = new GolfShotDetectionParams();
    golfShotDetectionParams.location = GolfShotDetectionParams.GolfCourseLocation.FAIRWAY;

    mHalAdapter.setGolfShotDetectionParams(golfShotDetectionParams);

    vendor.google_clockwork.healthservices.GolfShotDetectionParams halGolfShotDetectionParams =
        new vendor.google_clockwork.healthservices.GolfShotDetectionParams();
    halGolfShotDetectionParams.location =
        vendor.google_clockwork.healthservices.GolfShotDetectionParams.GolfCourseLocation.FAIRWAY;
    verify(mockHealthServicesHalV1).setGolfShotDetectionParams(halGolfShotDetectionParams);
  }

  @Test
  public void setGolfShotDetectionParams_halV1Connected_handlesUnexpectedHalException()
      throws RemoteException {
    registerAndConnectV1Hal();
    GolfShotDetectionParams golfShotDetectionParams = new GolfShotDetectionParams();
    golfShotDetectionParams.location = GolfShotDetectionParams.GolfCourseLocation.FAIRWAY;
    vendor.google_clockwork.healthservices.GolfShotDetectionParams halGolfShotDetectionParams =
        new vendor.google_clockwork.healthservices.GolfShotDetectionParams();
    halGolfShotDetectionParams.location =
        vendor.google_clockwork.healthservices.GolfShotDetectionParams.GolfCourseLocation.FAIRWAY;
    doThrow(UnsupportedOperationException.class)
        .when(mockHealthServicesHalV1)
        .setGolfShotDetectionParams(halGolfShotDetectionParams);

    mHalAdapter.setGolfShotDetectionParams(golfShotDetectionParams);
  }

  @Test
  public void setOemCustomConfiguration_halV0Connected() throws RemoteException {
    registerAndConnectV0Hal();
    int configId = 1;
    byte[] configValue = {1, 2, 3};

    mHalAdapter.setOemCustomConfiguration(configId, configValue);

    ArrayList<Byte> configValueList = new ArrayList<Byte>(1);
    configValueList.add(((byte) 1));
    configValueList.add(((byte) 2));
    configValueList.add(((byte) 3));
    verify(mockHealthServicesHalV0).setOemCustomConfiguration(configValueList);
  }

  @Test
  public void setOemCustomConfiguration_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    int configId = 1;
    byte[] configValue = {1, 2, 3};

    mHalAdapter.setOemCustomConfiguration(configId, configValue);

    verify(mockHealthServicesHalV1).setOemCustomConfiguration(configId, configValue);
  }

  @Test
  public void setOemCustomConfiguration_halV1Connected_handlesHalException()
      throws RemoteException {
    registerAndConnectV1Hal();
    int configId = 1;
    byte[] configValue = {1, 2, 3};
    doThrow(UnsupportedOperationException.class)
        .when(mockHealthServicesHalV1)
        .setOemCustomConfiguration(configId, configValue);

    mHalAdapter.setOemCustomConfiguration(configId, configValue);
  }

  @Test
  public void startAutomaticExerciseDetection_noOffset_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    AutoStartConfig walkingAutoStartConfig = getAutoStartConfig(ExerciseType.WALKING, 100);
    AutoStartConfig runningAutoStartConfig = getAutoStartConfig(ExerciseType.RUNNING, 200);
    AutoStartConfig[] autoStartConfigs = {walkingAutoStartConfig, runningAutoStartConfig};

    mHalAdapter.startAutomaticExerciseDetection(autoStartConfigs, null);

    vendor.google_clockwork.healthservices.AutoStartConfig halWalkingAutoStartConfig =
        getHalV1AutoStartConfig(ExerciseType.WALKING, 100);
    vendor.google_clockwork.healthservices.AutoStartConfig halRunningAutoStartConfig =
        getHalV1AutoStartConfig(ExerciseType.RUNNING, 200);
    vendor.google_clockwork.healthservices.AutoStartConfig[] halAutoStartConfigs = {
      halWalkingAutoStartConfig, halRunningAutoStartConfig
    };
    verify(mockHealthServicesHalV1).startAutomaticExerciseDetection(halAutoStartConfigs, null);
  }

  @Test
  public void startAutomaticExerciseDetection_withOffset_halV1Connected() throws RemoteException {
    registerAndConnectV1Hal();
    AutoStartConfig walkingAutoStartConfig = getAutoStartConfig(ExerciseType.WALKING, 100);
    AutoStartConfig runningAutoStartConfig = getAutoStartConfig(ExerciseType.RUNNING, 200);
    AutoStartConfig[] autoStartConfigs = {walkingAutoStartConfig, runningAutoStartConfig};
    AutoStartEvent autoStartOffset =
        getAutoStartEvent(
            ExerciseType.WALKING,
            123,
            AutoStartEvent.AutoStartEventType.AUTO_EXERCISE_START_DETECTED);

    mHalAdapter.startAutomaticExerciseDetection(autoStartConfigs, autoStartOffset);

    vendor.google_clockwork.healthservices.AutoStartConfig halWalkingAutoStartConfig =
        getHalV1AutoStartConfig(ExerciseType.WALKING, 100);
    vendor.google_clockwork.healthservices.AutoStartConfig halRunningAutoStartConfig =
        getHalV1AutoStartConfig(ExerciseType.RUNNING, 200);
    vendor.google_clockwork.healthservices.AutoStartConfig[] halAutoStartConfigs = {
      halWalkingAutoStartConfig, halRunningAutoStartConfig
    };
    vendor.google_clockwork.healthservices.AutoStartEvent halAutoStartOffset =
        getHalV1AutoStartEvent(
            ExerciseType.WALKING,
            123,
            AutoStartEvent.AutoStartEventType.AUTO_EXERCISE_START_DETECTED);
    verify(mockHealthServicesHalV1)
        .startAutomaticExerciseDetection(halAutoStartConfigs, halAutoStartOffset);
  }

  @Test
  public void startAutomaticExerciseDetection_noOffset_halV1Connected_handlesHalException()
      throws RemoteException {
    registerAndConnectV1Hal();
    AutoStartConfig walkingAutoStartConfig = getAutoStartConfig(ExerciseType.WALKING, 100);
    AutoStartConfig runningAutoStartConfig = getAutoStartConfig(ExerciseType.RUNNING, 200);
    AutoStartConfig[] autoStartConfigs = {walkingAutoStartConfig, runningAutoStartConfig};

    vendor.google_clockwork.healthservices.AutoStartConfig halWalkingAutoStartConfig =
        getHalV1AutoStartConfig(ExerciseType.WALKING, 100);
    vendor.google_clockwork.healthservices.AutoStartConfig halRunningAutoStartConfig =
        getHalV1AutoStartConfig(ExerciseType.RUNNING, 200);
    vendor.google_clockwork.healthservices.AutoStartConfig[] halAutoStartConfigs = {
      halWalkingAutoStartConfig, halRunningAutoStartConfig
    };
    doThrow(UnsupportedOperationException.class)
        .when(mockHealthServicesHalV1)
        .startAutomaticExerciseDetection(halAutoStartConfigs, null);

    mHalAdapter.startAutomaticExerciseDetection(autoStartConfigs, null);
  }

  private void registerAndConnectV0Hal() throws RemoteException {
    when(mockServiceManagerStub.getV0HalService()).thenReturn(mockHealthServicesHalV0);
    mHalAdapter.onRegistration(
        "vendor.google_clockwork.healthservices@1.0::IHealthServices", "default", true);
  }

  private void registerAndConnectV1Hal() throws RemoteException {
    when(mockServiceManagerStub.waitForDeclaredService()).thenReturn(mockHealthServicesHalV1);
    when(mockHealthServicesHalV1.asBinder()).thenReturn(mockIBinder);
    mHalAdapter.maybeGetAidlHalService();
  }

  private DataTypeGoal getDataTypeGoal(int dataType, int value, byte comparisonType) {
    DataTypeGoal dataTypeGoal = new DataTypeGoal();
    dataTypeGoal.dataType = dataType;
    dataTypeGoal.threshold = new GoalThreshold();
    dataTypeGoal.threshold.type = GoalThresholdType.INT;
    dataTypeGoal.threshold.intValue = value;
    dataTypeGoal.comparisonType = comparisonType;
    return dataTypeGoal;
  }

  private vendor.google_clockwork.healthservices.V1_0.DataTypeGoal getHalV0DataTypeGoal(
      int dataType, int value) {
    vendor.google_clockwork.healthservices.V1_0.DataTypeGoal halDataTypeGoal =
        new vendor.google_clockwork.healthservices.V1_0.DataTypeGoal();
    halDataTypeGoal.dataType = dataType;
    halDataTypeGoal.threshold =
        new vendor.google_clockwork.healthservices.V1_0.DataTypeGoal.GoalThreshold();
    halDataTypeGoal.threshold.intThreshold(value);
    return halDataTypeGoal;
  }

  private vendor.google_clockwork.healthservices.DataTypeGoal getHalV1DataTypeGoal(
      int dataType, int value, byte comparisonType) {
    vendor.google_clockwork.healthservices.DataTypeGoal halDataTypeGoal =
        new vendor.google_clockwork.healthservices.DataTypeGoal();
    halDataTypeGoal.dataType = dataType;
    halDataTypeGoal.threshold =
        new vendor.google_clockwork.healthservices.DataTypeGoal.GoalThreshold();
    halDataTypeGoal.threshold.setIntThreshold(value);
    halDataTypeGoal.comparisonType = comparisonType;
    return halDataTypeGoal;
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

  private vendor.google_clockwork.healthservices.V1_0.TrackingConfig getHalV0TrackingConfig(
      int exerciseType, boolean enableGpsControl, int maxReportLatencyMs, int samplingPeriodMs) {
    vendor.google_clockwork.healthservices.V1_0.TrackingConfig halTrackingConfig =
        new vendor.google_clockwork.healthservices.V1_0.TrackingConfig();
    halTrackingConfig.exerciseType = exerciseType;
    halTrackingConfig.enableGpsControl = enableGpsControl;
    halTrackingConfig.maxReportLatencyMs = maxReportLatencyMs;
    halTrackingConfig.samplingPeriodMs = samplingPeriodMs;

    return halTrackingConfig;
  }

  private vendor.google_clockwork.healthservices.TrackingConfig getHalV1TrackingConfig(
      int exerciseType, boolean enableGpsControl, int maxReportLatencyMs, int samplingPeriodMs) {
    vendor.google_clockwork.healthservices.TrackingConfig halTrackingConfig =
        new vendor.google_clockwork.healthservices.TrackingConfig();
    halTrackingConfig.exerciseType = exerciseType;
    halTrackingConfig.enableGpsControl = enableGpsControl;
    halTrackingConfig.maxReportLatencyMs = maxReportLatencyMs;
    halTrackingConfig.samplingPeriodMs = samplingPeriodMs;

    return halTrackingConfig;
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

  private DataTypeOffset getIntStatsOffset(
      int dataType,
      int minValue,
      int maxValue,
      int avgValue,
      int numSampleDataPoints,
      long activeTrackingPeriodMs) {
    DataTypeOffset dataTypeOffset = new DataTypeOffset();
    dataTypeOffset.dataType = dataType;
    StatsOffsetValue statsOffsetValue = new StatsOffsetValue();
    statsOffsetValue.numSampleDataPoints = numSampleDataPoints;
    statsOffsetValue.activeTrackingPeriodMs = activeTrackingPeriodMs;

    StatsValue statsValue = new StatsValue();
    StatsValue.IntStatsValue intStatsValue = new StatsValue.IntStatsValue();
    intStatsValue.minimumValue = minValue;
    intStatsValue.maximumValue = maxValue;
    intStatsValue.averageValue = avgValue;
    statsValue.setIntStatsValue(intStatsValue);

    statsOffsetValue.offsetValues = statsValue;
    dataTypeOffset.statsOffsetValue = statsOffsetValue;
    return dataTypeOffset;
  }

  private DataTypeOffset getFloatStatsOffset(
      int dataType,
      float minValue,
      float maxValue,
      float avgValue,
      int numSampleDataPoints,
      long activeTrackingPeriodMs) {
    DataTypeOffset dataTypeOffset = new DataTypeOffset();
    dataTypeOffset.dataType = dataType;
    StatsOffsetValue statsOffsetValue = new StatsOffsetValue();
    statsOffsetValue.numSampleDataPoints = numSampleDataPoints;
    statsOffsetValue.activeTrackingPeriodMs = activeTrackingPeriodMs;

    StatsValue statsValue = new StatsValue();
    StatsValue.FloatStatsValue floatStatsValue = new StatsValue.FloatStatsValue();
    floatStatsValue.minimumValue = minValue;
    floatStatsValue.maximumValue = maxValue;
    floatStatsValue.averageValue = avgValue;
    statsValue.setFloatStatsValue(floatStatsValue);

    statsOffsetValue.offsetValues = statsValue;
    dataTypeOffset.statsOffsetValue = statsOffsetValue;
    return dataTypeOffset;
  }

  private vendor.google_clockwork.healthservices.V1_0.DataTypeOffset getHalV0DataTypeOffset(
      int dataType, int offset) {
    vendor.google_clockwork.healthservices.V1_0.DataTypeOffset halDataTypeOffset =
        new vendor.google_clockwork.healthservices.V1_0.DataTypeOffset();
    halDataTypeOffset.dataType = dataType;
    vendor.google_clockwork.healthservices.V1_0.DataTypeOffset.OffsetValue offsetValue =
        new vendor.google_clockwork.healthservices.V1_0.DataTypeOffset.OffsetValue();
    offsetValue.intValue(offset);
    halDataTypeOffset.offsetValue = offsetValue;

    return halDataTypeOffset;
  }

  private vendor.google_clockwork.healthservices.DataTypeOffset getHalV1DataTypeOffset(
      int dataType, int offset) {
    vendor.google_clockwork.healthservices.DataTypeOffset halDataTypeOffset =
        new vendor.google_clockwork.healthservices.DataTypeOffset();
    vendor.google_clockwork.healthservices.MetricOffset halMetricOffset =
        new vendor.google_clockwork.healthservices.MetricOffset();
    halMetricOffset.dataType = dataType;
    vendor.google_clockwork.healthservices.MetricValue offsetValue =
        new vendor.google_clockwork.healthservices.MetricValue();
    offsetValue.setIntValue(offset);
    halMetricOffset.offsetValue = offsetValue;
    halDataTypeOffset.setMetricOffset(halMetricOffset);

    return halDataTypeOffset;
  }

  private vendor.google_clockwork.healthservices.DataTypeOffset getHalV1StatsDataTypeOffset(
      int dataType,
      int minValue,
      int maxValue,
      int avgValue,
      int numSampleDataPoints,
      long activeTrackingPeriodMs) {
    vendor.google_clockwork.healthservices.DataTypeOffset halDataTypeOffset =
        new vendor.google_clockwork.healthservices.DataTypeOffset();
    vendor.google_clockwork.healthservices.StatsOffset halStatsOffset =
        new vendor.google_clockwork.healthservices.StatsOffset();
    halStatsOffset.dataType = dataType;
    halStatsOffset.numSampleDataPoints = numSampleDataPoints;
    halStatsOffset.activeTrackingPeriodMs = activeTrackingPeriodMs;

    vendor.google_clockwork.healthservices.StatsValue.IntStatsValue halIntStatsValue =
        new vendor.google_clockwork.healthservices.StatsValue.IntStatsValue();
    halIntStatsValue.minimumValue = minValue;
    halIntStatsValue.maximumValue = maxValue;
    halIntStatsValue.averageValue = avgValue;

    vendor.google_clockwork.healthservices.StatsValue offsetValue =
        new vendor.google_clockwork.healthservices.StatsValue();
    offsetValue.setIntStatsValue(halIntStatsValue);
    halStatsOffset.offsetValues = offsetValue;
    halDataTypeOffset.setStatsOffset(halStatsOffset);

    return halDataTypeOffset;
  }

  private vendor.google_clockwork.healthservices.DataTypeOffset getHalV1StatsDataTypeOffset(
      int dataType,
      float minValue,
      float maxValue,
      float avgValue,
      int numSampleDataPoints,
      long activeTrackingPeriodMs) {
    vendor.google_clockwork.healthservices.DataTypeOffset halDataTypeOffset =
        new vendor.google_clockwork.healthservices.DataTypeOffset();
    vendor.google_clockwork.healthservices.StatsOffset halStatsOffset =
        new vendor.google_clockwork.healthservices.StatsOffset();
    halStatsOffset.dataType = dataType;
    halStatsOffset.numSampleDataPoints = numSampleDataPoints;
    halStatsOffset.activeTrackingPeriodMs = activeTrackingPeriodMs;

    vendor.google_clockwork.healthservices.StatsValue.FloatStatsValue halFloatStatsValue =
        new vendor.google_clockwork.healthservices.StatsValue.FloatStatsValue();
    halFloatStatsValue.minimumValue = minValue;
    halFloatStatsValue.maximumValue = maxValue;
    halFloatStatsValue.averageValue = avgValue;

    vendor.google_clockwork.healthservices.StatsValue offsetValue =
        new vendor.google_clockwork.healthservices.StatsValue();
    offsetValue.setFloatStatsValue(halFloatStatsValue);
    halStatsOffset.offsetValues = offsetValue;
    halDataTypeOffset.setStatsOffset(halStatsOffset);

    return halDataTypeOffset;
  }

  private AutoStartConfig getAutoStartConfig(int exerciseType, int minimumNotificationLatencyMs) {
    AutoStartConfig autoStartConfig = new AutoStartConfig();
    autoStartConfig.exerciseType = exerciseType;
    autoStartConfig.minimumNotificationLatencyMs = minimumNotificationLatencyMs;
    return autoStartConfig;
  }

  private vendor.google_clockwork.healthservices.AutoStartConfig getHalV1AutoStartConfig(
      int exerciseType, int minimumNotificationLatencyMs) {
    vendor.google_clockwork.healthservices.AutoStartConfig autoStartConfig =
        new vendor.google_clockwork.healthservices.AutoStartConfig();
    autoStartConfig.exerciseType = exerciseType;
    autoStartConfig.minimumNotificationLatencyMs = minimumNotificationLatencyMs;
    return autoStartConfig;
  }

  private AutoStartEvent getAutoStartEvent(
      int exerciseType, long elapsedTimeSinceBootNanos, byte eventType) {
    AutoStartEvent autoStartEvent = new AutoStartEvent();
    autoStartEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    autoStartEvent.exerciseType = exerciseType;
    autoStartEvent.type = eventType;
    return autoStartEvent;
  }

  private vendor.google_clockwork.healthservices.AutoStartEvent getHalV1AutoStartEvent(
      int exerciseType, long elapsedTimeSinceBootNanos, byte eventType) {
    vendor.google_clockwork.healthservices.AutoStartEvent autoStartEvent =
        new vendor.google_clockwork.healthservices.AutoStartEvent();
    autoStartEvent.elapsedTimeSinceBootNanos = elapsedTimeSinceBootNanos;
    autoStartEvent.exerciseType = exerciseType;
    autoStartEvent.type = eventType;
    return autoStartEvent;
  }
}
