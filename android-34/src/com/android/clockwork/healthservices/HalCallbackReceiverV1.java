package com.android.clockwork.healthservices;

import android.os.RemoteException;

import com.google.android.clockwork.healthservices.types.AchievedGoal;
import com.google.android.clockwork.healthservices.types.AutoExerciseEvent;
import com.google.android.clockwork.healthservices.types.AvailabilityUpdate;
import com.google.android.clockwork.healthservices.types.DataTypeGoal;
import com.google.android.clockwork.healthservices.types.DataTypeOffset;
import com.google.android.clockwork.healthservices.types.DataUpdate;
import com.google.android.clockwork.healthservices.types.ExerciseEvent;
import com.google.android.clockwork.healthservices.types.HealthEvent;
import com.google.android.clockwork.healthservices.types.HrAlertParams;
import com.google.android.clockwork.healthservices.IHealthServiceCallback;
import com.google.android.clockwork.healthservices.types.TrackingConfig;

import java.util.ArrayList;

import vendor.google_clockwork.healthservices.IHealthServicesCallback;

/** Converts HAL AIDL V1 callbacks into a form that can be processed by the HAL Listener. */
final class HalCallbackReceiverV1 extends IHealthServicesCallback.Stub {

  private HalAdapter.HalListener halListener;

  public HalCallbackReceiverV1(HalAdapter.HalListener halListener) {
    this.halListener = halListener;
  }

  /** Called anytime there is a new data metric event. */
  @Override // IHealthServicesCallback
  public void onDataUpdate(vendor.google_clockwork.healthservices.DataUpdate[] halDataUpdates) {
    ArrayList<DataUpdate> dataMetricUpdates = new ArrayList<>();
    ArrayList<ExerciseEvent> exerciseEventsList = new ArrayList<>();
    for (vendor.google_clockwork.healthservices.DataUpdate halDataUpdate : halDataUpdates) {
      switch (halDataUpdate.getTag()) {
        case vendor.google_clockwork.healthservices.DataUpdate.metricData:
          dataMetricUpdates.add(
              HalTypeConverterV1.fromHalMetricData(halDataUpdate.getMetricData()));
          break;
        case vendor.google_clockwork.healthservices.DataUpdate.locationData:
          dataMetricUpdates.add(
              HalTypeConverterV1.fromHalLocationData(halDataUpdate.getLocationData()));
          break;
        case vendor.google_clockwork.healthservices.DataUpdate.statsData:
          dataMetricUpdates.add(
              HalTypeConverterV1.fromHalStatsData(halDataUpdate.getStatsData()));
          break;
        case vendor.google_clockwork.healthservices.DataUpdate.healthEvent:
          HealthEvent healthEvent =
              HalTypeConverterV1.fromHalHealthEvent(halDataUpdate.getHealthEvent());
          halListener.onHealthEventDetected(healthEvent);
          break;
        case vendor.google_clockwork.healthservices.DataUpdate.autoExerciseEvent:
          AutoExerciseEvent autoExerciseEvent =
              HalTypeConverterV1.fromHalAutoExerciseEvent(halDataUpdate.getAutoExerciseEvent());
          halListener.onAutoExerciseEvent(autoExerciseEvent);
          break;
        case vendor.google_clockwork.healthservices.DataUpdate.exerciseEvent:
          ExerciseEvent exerciseEvent =
              HalTypeConverterV1.fromHalExerciseEvent(halDataUpdate.getExerciseEvent());
          exerciseEventsList.add(exerciseEvent);
          break;
      }
    }

    if (!dataMetricUpdates.isEmpty()) {
      DataUpdate[] dataUpdates = dataMetricUpdates.toArray(new DataUpdate[0]);
      halListener.onDataUpdate(dataUpdates);
    }
    if (!exerciseEventsList.isEmpty()) {
      ExerciseEvent[] exerciseEvents = exerciseEventsList.toArray(new ExerciseEvent[0]);
      halListener.onExerciseEvent(exerciseEvents);
    }
  }

  /** Called whenever a tracked goal has been met. */
  @Override // IHealthServicesCallback
  public void onGoalAchieved(vendor.google_clockwork.healthservices.AchievedGoal[] goals) {
    AchievedGoal[] achievedGoals = new AchievedGoal[goals.length];
    for (int i = 0; i < goals.length; i++) {
      achievedGoals[i] = HalTypeConverterV1.fromHalAchievedGoal(goals[i]);
    }
    halListener.onGoalAchieved(achievedGoals);
  }

  /** Called whenever there is a change in availability of an exercise DataType. */
  @Override // IHealthServicesCallback
  public void onAvailabilityUpdate(
      vendor.google_clockwork.healthservices.AvailabilityUpdate[] halAvailabilityUpdates) {
    AvailabilityUpdate[] availabilityUpdates =
        new AvailabilityUpdate[halAvailabilityUpdates.length];
    for (int i = 0; i < halAvailabilityUpdates.length; i++) {
      availabilityUpdates[i] =
          HalTypeConverterV1.fromHalAvailabilityUpdate(halAvailabilityUpdates[i]);
    }
    halListener.onAvailabilityUpdate(availabilityUpdates);
  }

  /**
   * Called after all requested metrics have been flushed and emitted from the HAL implementation
   * after a flush() call.
   */
  @Override // IHealthServicesCallback
  public void onFlushCompleted(int flushId) {
    halListener.onFlushCompleted(flushId);
  }

  @Override
  public int getInterfaceVersion() {
    return this.VERSION;
  }

  @Override
  public String getInterfaceHash() {
    return this.HASH;
  }
}
