package com.android.clockwork.healthservices;

import android.os.RemoteException;

import com.google.android.clockwork.healthservices.types.AchievedGoal;
import com.google.android.clockwork.healthservices.types.AutoExerciseEvent;
import com.google.android.clockwork.healthservices.types.AvailabilityUpdate;
import com.google.android.clockwork.healthservices.types.DataTypeGoal;
import com.google.android.clockwork.healthservices.types.DataTypeOffset;
import com.google.android.clockwork.healthservices.types.DataUpdate;
import com.google.android.clockwork.healthservices.types.HealthEvent;
import com.google.android.clockwork.healthservices.types.HrAlertParams;
import com.google.android.clockwork.healthservices.IHealthServiceCallback;
import com.google.android.clockwork.healthservices.types.TrackingConfig;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;

import vendor.google_clockwork.healthservices.V1_0.IHealthServicesCallback;

/** Converts HAL HIDL V1_0 callbacks into a form that can be processed by the HAL Listener. */
final class HalCallbackReceiverHidl extends IHealthServicesCallback.Stub {

  private HalAdapter.HalListener halListener;

  /**
   * The HIDL HAL does not support flush IDs. Instead flushes are expected to be completed in the
   * same order that there were made. For now, to keep things simple, we take advantage of the fact
   * that the IDs are monotonically increasing, generated in the SystemService to replicate that
   * logic here for outgoing IDs.
   */
  @VisibleForTesting public int nextFlushId = Utils.STARTING_FLUSH_ID;

  public HalCallbackReceiverHidl(HalAdapter.HalListener halListener) {
    this.halListener = halListener;
  }

  /** Called anytime there is a new data metric event. */
  @Override // IHealthServicesCallback
  public void onDataUpdate(
      ArrayList<vendor.google_clockwork.healthservices.V1_0.DataUpdate> halDataUpdates) {
    ArrayList<DataUpdate> dataMetricUpdates = new ArrayList<>();
    for (vendor.google_clockwork.healthservices.V1_0.DataUpdate halDataUpdate : halDataUpdates) {
      switch (halDataUpdate.getDiscriminator()) {
        case vendor.google_clockwork.healthservices.V1_0.DataUpdate.hidl_discriminator.metricData:
          dataMetricUpdates.add(HalTypeConverterHidl.toAidlMetricData(halDataUpdate.metricData()));
          break;
        case vendor.google_clockwork.healthservices.V1_0.DataUpdate.hidl_discriminator.locationData:
          dataMetricUpdates.add(
              HalTypeConverterHidl.toAidlLocationData(halDataUpdate.locationData()));
          break;
        case vendor.google_clockwork.healthservices.V1_0.DataUpdate.hidl_discriminator.healthEvent:
          HealthEvent healthEvent =
              HalTypeConverterHidl.toAidlHealthEvent(halDataUpdate.healthEvent());
          halListener.onHealthEventDetected(healthEvent);
          break;
        case vendor.google_clockwork.healthservices.V1_0.DataUpdate.hidl_discriminator
            .autoPauseEvent:
          AutoExerciseEvent autoExerciseEvent =
              HalTypeConverterHidl.fromHalAutoPauseEvent(halDataUpdate.autoPauseEvent());
          halListener.onAutoExerciseEvent(autoExerciseEvent);
          break;
      }
    }

    if (dataMetricUpdates.isEmpty()) {
      return;
    }
    DataUpdate[] aidlDataUpdates = dataMetricUpdates.toArray(new DataUpdate[0]);
    halListener.onDataUpdate(aidlDataUpdates);
  }

  /** Called whenever a tracked goal has been met. */
  @Override // IHealthServicesCallback
  public void onGoalAchieved(
      ArrayList<vendor.google_clockwork.healthservices.V1_0.AchievedGoal> goals) {
    AchievedGoal[] achievedGoals = new AchievedGoal[goals.size()];
    for (int i = 0; i < goals.size(); i++) {
      achievedGoals[i] = HalTypeConverterHidl.toAidlAchievedGoal(goals.get(i));
    }
    halListener.onGoalAchieved(achievedGoals);
  }

  /** Called whenever there is a change in availability of an exercise DataType. */
  @Override // IHealthServicesCallback
  public void onAvailabilityUpdate(
      ArrayList<vendor.google_clockwork.healthservices.V1_0.AvailabilityUpdate>
          halAvailabilityUpdates) {
    AvailabilityUpdate[] availabilityUpdates =
        new AvailabilityUpdate[halAvailabilityUpdates.size()];
    for (int i = 0; i < halAvailabilityUpdates.size(); i++) {
      availabilityUpdates[i] =
          HalTypeConverterHidl.toAidlAvailabilityUpdate(halAvailabilityUpdates.get(i));
    }
    halListener.onAvailabilityUpdate(availabilityUpdates);
  }

  /**
   * Called after all requested metrics have been flushed and emitted from the HAL implementation
   * after a flush() call.
   */
  @Override // IHealthServicesCallback
  public void onFlushCompleted() {
    halListener.onFlushCompleted(nextFlushId);
    nextFlushId = Utils.generateNextFlushId(nextFlushId);
  }
}
