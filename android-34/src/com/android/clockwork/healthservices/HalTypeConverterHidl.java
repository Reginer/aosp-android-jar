package com.android.clockwork.healthservices;

import java.util.ArrayList;

import vendor.google_clockwork.healthservices.V1_0.AchievedGoal;
import vendor.google_clockwork.healthservices.V1_0.AutoPauseEvent;
import vendor.google_clockwork.healthservices.V1_0.AvailabilityUpdate;
import vendor.google_clockwork.healthservices.V1_0.DataTypeAvailability;
import vendor.google_clockwork.healthservices.V1_0.DataTypeGoal;
import vendor.google_clockwork.healthservices.V1_0.DataTypeOffset;
import vendor.google_clockwork.healthservices.V1_0.HealthEvent;
import vendor.google_clockwork.healthservices.V1_0.HrAlertParams;
import vendor.google_clockwork.healthservices.V1_0.LocationAvailability;
import vendor.google_clockwork.healthservices.V1_0.LocationData;
import vendor.google_clockwork.healthservices.V1_0.MetricData;
import vendor.google_clockwork.healthservices.V1_0.TrackingConfig;

/** Helper class that handles all the conversion between AIDL types and HIDL HAL types. */
final class HalTypeConverterHidl {

  static ArrayList<Integer> convertToHalDataTypeList(int[] aidlDataTypes) {
    ArrayList<Integer> dataTypeList = new ArrayList<Integer>(aidlDataTypes.length);
    for (int aidlDataType : aidlDataTypes) {
      dataTypeList.add(aidlDataType);
    }
    return dataTypeList;
  }

  static ArrayList<DataTypeOffset> convertToHalDataTypeOffsets(
      com.google.android.clockwork.healthservices.types.DataTypeOffset[] aidlDataOffsets) {
    ArrayList<DataTypeOffset> halDataOffsets =
        new ArrayList<DataTypeOffset>(aidlDataOffsets.length);
    for (com.google.android.clockwork.healthservices.types.DataTypeOffset aidlDataOffset :
        aidlDataOffsets) {
      DataTypeOffset halDataOffset = new DataTypeOffset();
      halDataOffset.dataType = aidlDataOffset.dataType;
      halDataOffset.offsetValue = new DataTypeOffset.OffsetValue();
      if (aidlDataOffset.offsetValue.type
          == com.google.android.clockwork.healthservices.types.OffsetValueType.INT) {
        halDataOffset.offsetValue.intValue(aidlDataOffset.offsetValue.intValue);
      } else if (aidlDataOffset.offsetValue.type
          == com.google.android.clockwork.healthservices.types.OffsetValueType.FLOAT) {
        halDataOffset.offsetValue.floatValue(aidlDataOffset.offsetValue.floatValue);
      } else {
        // The HIDL HAL doesn't support byte array offsets so those should be skipped.
        continue;
      }
      // TODO(b/225088270): Figure out error handling with these conversion libraries. Should we
      // explicitly check for FLOAT type and throw an exception if invalid?

      halDataOffsets.add(halDataOffset);
    }
    return halDataOffsets;
  }

  static TrackingConfig toHalTrackingConfig(
      com.google.android.clockwork.healthservices.types.TrackingConfig aidlType) {
    TrackingConfig trackingConfig = new TrackingConfig();

    trackingConfig.exerciseType = aidlType.exerciseType;
    trackingConfig.enableGpsControl = aidlType.enableGpsControl;
    trackingConfig.maxReportLatencyMs = aidlType.maxReportLatencyMs;
    trackingConfig.samplingPeriodMs = aidlType.samplingPeriodMs;

    return trackingConfig;
  }

  static HrAlertParams toHalHrAlertParams(
      com.google.android.clockwork.healthservices.types.HrAlertParams aidlType) {
    HrAlertParams hrAlertParams = new HrAlertParams();

    hrAlertParams.type = aidlType.type;
    hrAlertParams.durationSec = aidlType.durationSec;
    hrAlertParams.thresholdBpm = aidlType.thresholdBpm;

    return hrAlertParams;
  }

  static DataTypeGoal toHalDataTypeGoal(
      com.google.android.clockwork.healthservices.types.DataTypeGoal aidlType) {
    DataTypeGoal dataTypeGoal = new DataTypeGoal();

    dataTypeGoal.dataType = aidlType.dataType;
    dataTypeGoal.threshold = new DataTypeGoal.GoalThreshold();
    if (aidlType.threshold.type
        == com.google.android.clockwork.healthservices.types.GoalThresholdType.INT) {
      dataTypeGoal.threshold.intThreshold(aidlType.threshold.intValue);
    } else {
      dataTypeGoal.threshold.floatThreshold(aidlType.threshold.floatValue);
    }

    return dataTypeGoal;
  }

  static com.google.android.clockwork.healthservices.types.DataTypeGoal toAidlDataTypeGoal(
      DataTypeGoal halType) {
    com.google.android.clockwork.healthservices.types.DataTypeGoal dataTypeGoal =
        new com.google.android.clockwork.healthservices.types.DataTypeGoal();

    dataTypeGoal.dataType = halType.dataType;
    dataTypeGoal.threshold = new com.google.android.clockwork.healthservices.types.GoalThreshold();
    switch (halType.threshold.getDiscriminator()) {
      case DataTypeGoal.GoalThreshold.hidl_discriminator.intThreshold:
        dataTypeGoal.threshold.type =
            com.google.android.clockwork.healthservices.types.GoalThresholdType.INT;
        dataTypeGoal.threshold.intValue = halType.threshold.intThreshold();
        break;
      case DataTypeGoal.GoalThreshold.hidl_discriminator.floatThreshold:
        dataTypeGoal.threshold.type =
            com.google.android.clockwork.healthservices.types.GoalThresholdType.FLOAT;
        dataTypeGoal.threshold.floatValue = halType.threshold.floatThreshold();
        break;
    }

    // V0 goals are always GREATER_THAN_OR_EQUAL.
    dataTypeGoal.comparisonType =
        com.google.android.clockwork.healthservices.types.DataTypeGoal.ComparisonType
            .GREATER_THAN_OR_EQUAL;

    return dataTypeGoal;
  }

  static com.google.android.clockwork.healthservices.types.AchievedGoal toAidlAchievedGoal(
      AchievedGoal halAchievedGoal) {
    com.google.android.clockwork.healthservices.types.AchievedGoal aidlAchievedGoal =
        new com.google.android.clockwork.healthservices.types.AchievedGoal();

    aidlAchievedGoal.goal = toAidlDataTypeGoal(halAchievedGoal.goal);
    aidlAchievedGoal.elapsedTimeSinceBootNanos = halAchievedGoal.elapsedTimeSinceBootNanos;

    return aidlAchievedGoal;
  }

  static com.google.android.clockwork.healthservices.types.AvailabilityUpdate
      toAidlAvailabilityUpdate(AvailabilityUpdate halAvailabilityUpdate) {
    com.google.android.clockwork.healthservices.types.AvailabilityUpdate aidlAvailabilityUpdate =
        new com.google.android.clockwork.healthservices.types.AvailabilityUpdate();
    switch (halAvailabilityUpdate.getDiscriminator()) {
      case AvailabilityUpdate.hidl_discriminator.dataTypeAvailability:
        com.google.android.clockwork.healthservices.types.DataTypeAvailability
            dataTypeAvailability =
                new com.google.android.clockwork.healthservices.types.DataTypeAvailability();
        DataTypeAvailability halDataTypeAvailability = halAvailabilityUpdate.dataTypeAvailability();
        dataTypeAvailability.dataType = halDataTypeAvailability.dataType;
        dataTypeAvailability.dataTypeAvailabilityStatus =
            halDataTypeAvailability.dataTypeAvailabilityStatus;
        dataTypeAvailability.elapsedTimeSinceBootNanos =
            halDataTypeAvailability.elapsedTimeSinceBootNanos;
        aidlAvailabilityUpdate.dataTypeAvailability = dataTypeAvailability;
        break;
      case AvailabilityUpdate.hidl_discriminator.locationAvailability:
        com.google.android.clockwork.healthservices.types.LocationAvailability
            locationAvailability =
                new com.google.android.clockwork.healthservices.types.LocationAvailability();
        LocationAvailability halLocationAvailability = halAvailabilityUpdate.locationAvailability();
        locationAvailability.locationAvailabilityStatus =
            halLocationAvailability.locationAvailabilityStatus;
        locationAvailability.elapsedTimeSinceBootNanos =
            halLocationAvailability.elapsedTimeSinceBootNanos;
        aidlAvailabilityUpdate.locationAvailability = locationAvailability;
        break;
    }
    return aidlAvailabilityUpdate;
  }

  static com.google.android.clockwork.healthservices.types.HealthEvent toAidlHealthEvent(
      HealthEvent halHealthEvent) {
    com.google.android.clockwork.healthservices.types.HealthEvent aidlHealthEvent =
        new com.google.android.clockwork.healthservices.types.HealthEvent();

    aidlHealthEvent.type = halHealthEvent.type;
    aidlHealthEvent.elapsedTimeSinceBootNanos = halHealthEvent.elapsedTimeSinceBootNanos;

    return aidlHealthEvent;
  }

  static com.google.android.clockwork.healthservices.types.AutoExerciseEvent fromHalAutoPauseEvent(
      AutoPauseEvent halAutoPauseEvent) {
    com.google.android.clockwork.healthservices.types.AutoPauseEvent aidlAutoPauseEvent =
        new com.google.android.clockwork.healthservices.types.AutoPauseEvent();

    aidlAutoPauseEvent.type = halAutoPauseEvent.type;
    aidlAutoPauseEvent.elapsedTimeSinceBootNanos = halAutoPauseEvent.elapsedTimeSinceBootNanos;

    com.google.android.clockwork.healthservices.types.AutoExerciseEvent aidlAutoExerciseEvent =
        new com.google.android.clockwork.healthservices.types.AutoExerciseEvent();
    aidlAutoExerciseEvent.setAutoPauseEvent(aidlAutoPauseEvent);
    return aidlAutoExerciseEvent;
  }

  static com.google.android.clockwork.healthservices.types.DataUpdate toAidlLocationData(
      LocationData halLocationData) {
    com.google.android.clockwork.healthservices.types.DataUpdate aidlDataUpdate =
        new com.google.android.clockwork.healthservices.types.DataUpdate();

    com.google.android.clockwork.healthservices.types.LocationData aidlLocationData =
        new com.google.android.clockwork.healthservices.types.LocationData();

    aidlLocationData.latitude = halLocationData.latitude;
    aidlLocationData.longitude = halLocationData.longitude;
    aidlLocationData.horizontalPositionErrorMeters = halLocationData.horizontalPositionErrorMeters;
    aidlLocationData.bearing = halLocationData.bearing;
    aidlLocationData.elapsedTimeSinceBootNanos = halLocationData.elapsedTimeSinceBootNanos;

    aidlDataUpdate.locationData = aidlLocationData;

    return aidlDataUpdate;
  }

  static com.google.android.clockwork.healthservices.types.DataUpdate toAidlMetricData(
      MetricData halMetricData) {
    com.google.android.clockwork.healthservices.types.DataUpdate aidlDataUpdate =
        new com.google.android.clockwork.healthservices.types.DataUpdate();

    com.google.android.clockwork.healthservices.types.MetricData aidlMetricData =
        new com.google.android.clockwork.healthservices.types.MetricData();

    aidlMetricData.dataType = halMetricData.dataType;
    aidlMetricData.accuracy = halMetricData.accuracy;
    aidlMetricData.elapsedTimeSinceBootNanos = halMetricData.elapsedTimeSinceBootNanos;

    com.google.android.clockwork.healthservices.types.MetricValue aidlMetricValue =
        new com.google.android.clockwork.healthservices.types.MetricValue();

    switch (halMetricData.metricValue.getDiscriminator()) {
      case MetricData.MetricValue.hidl_discriminator.intValue:
        aidlMetricValue.type =
            com.google.android.clockwork.healthservices.types.MetricValueType.INT;
        aidlMetricValue.intValue = halMetricData.metricValue.intValue();
        break;
      case MetricData.MetricValue.hidl_discriminator.floatValue:
        aidlMetricValue.type =
            com.google.android.clockwork.healthservices.types.MetricValueType.FLOAT;
        aidlMetricValue.floatValue = halMetricData.metricValue.floatValue();
        break;
      case MetricData.MetricValue.hidl_discriminator.byteValue:
        aidlMetricValue.type =
            com.google.android.clockwork.healthservices.types.MetricValueType.BYTE_ARRAY;
        ArrayList<Byte> byteArrayList = halMetricData.metricValue.byteValue();
        byte[] byteArray = new byte[byteArrayList.size()];
        for (int i = 0; i < byteArrayList.size(); i++) {
          byteArray[i] = byteArrayList.get(i);
        }
        aidlMetricValue.byteArrayValue = byteArray;
        break;
    }
    aidlMetricData.metricValue = aidlMetricValue;

    aidlDataUpdate.metricData = aidlMetricData;
    return aidlDataUpdate;
  }

  private HalTypeConverterHidl() {}
}
