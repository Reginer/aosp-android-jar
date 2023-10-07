package com.android.clockwork.healthservices;

import vendor.google_clockwork.healthservices.AchievedGoal;
import vendor.google_clockwork.healthservices.AutoExerciseEvent;
import vendor.google_clockwork.healthservices.AutoPauseEvent;
import vendor.google_clockwork.healthservices.AutoStartCapabilities;
import vendor.google_clockwork.healthservices.AutoStartConfig;
import vendor.google_clockwork.healthservices.AutoStartEvent;
import vendor.google_clockwork.healthservices.AutoStopEvent;
import vendor.google_clockwork.healthservices.AvailabilityUpdate;
import vendor.google_clockwork.healthservices.DataTypeGoal;
import vendor.google_clockwork.healthservices.DataTypeOffset;
import vendor.google_clockwork.healthservices.ExerciseEvent;
import vendor.google_clockwork.healthservices.GolfShotDetectionEvent;
import vendor.google_clockwork.healthservices.GolfShotDetectionParams;
import vendor.google_clockwork.healthservices.HealthEvent;
import vendor.google_clockwork.healthservices.HrAlertParams;
import vendor.google_clockwork.healthservices.LocationData;
import vendor.google_clockwork.healthservices.MetricData;
import vendor.google_clockwork.healthservices.MetricOffset;
import vendor.google_clockwork.healthservices.MetricValue;
import vendor.google_clockwork.healthservices.StatsData;
import vendor.google_clockwork.healthservices.StatsOffset;
import vendor.google_clockwork.healthservices.StatsValue;
import vendor.google_clockwork.healthservices.TrackingConfig;

/**
 * Helper class that handles all the conversion between SystemService AIDL types and HAL V1 AIDL
 * types.
 */
final class HalTypeConverterV1 {

  static DataTypeOffset[] toHalDataTypeOffsets(
      com.google.android.clockwork.healthservices.types.DataTypeOffset[] dataOffsets) {
    DataTypeOffset[] halDataOffsets = new DataTypeOffset[dataOffsets.length];
    for (int i = 0; i < dataOffsets.length; i++) {
      com.google.android.clockwork.healthservices.types.DataTypeOffset dataOffset = dataOffsets[i];
      DataTypeOffset halDataOffset = new DataTypeOffset();
      if (dataOffset.offsetValue != null) {
        MetricOffset metricOffset = new MetricOffset();
        metricOffset.dataType = dataOffset.dataType;
        metricOffset.offsetValue = new MetricValue();
        if (dataOffset.offsetValue.type
            == com.google.android.clockwork.healthservices.types.OffsetValueType.INT) {
          metricOffset.offsetValue.setIntValue(dataOffset.offsetValue.intValue);
        } else if (dataOffset.offsetValue.type
            == com.google.android.clockwork.healthservices.types.OffsetValueType.FLOAT) {
          metricOffset.offsetValue.setFloatValue(dataOffset.offsetValue.floatValue);
        } else {
          metricOffset.offsetValue.setByteValue(dataOffset.offsetValue.byteValue);
        }
        halDataOffset.setMetricOffset(metricOffset);
      } else { // statsOffsetValue
        StatsOffset halStatsOffset = new StatsOffset();
        halStatsOffset.dataType = dataOffset.dataType;

        com.google.android.clockwork.healthservices.types.StatsOffsetValue statsOffsetValue =
            dataOffset.statsOffsetValue;
        halStatsOffset.numSampleDataPoints = statsOffsetValue.numSampleDataPoints;
        halStatsOffset.activeTrackingPeriodMs = statsOffsetValue.activeTrackingPeriodMs;

        com.google.android.clockwork.healthservices.types.StatsValue statsValue =
            statsOffsetValue.offsetValues;

        StatsValue halOffsetValue = new StatsValue();
        switch (statsValue.getTag()) {
          case com.google.android.clockwork.healthservices.types.StatsValue.intStatsValue:
            com.google.android.clockwork.healthservices.types.StatsValue.IntStatsValue
                intStatsValue = statsValue.getIntStatsValue();
            StatsValue.IntStatsValue halIntStatsValue = new StatsValue.IntStatsValue();
            halIntStatsValue.minimumValue = intStatsValue.minimumValue;
            halIntStatsValue.maximumValue = intStatsValue.maximumValue;
            halIntStatsValue.averageValue = intStatsValue.averageValue;

            halOffsetValue.setIntStatsValue(halIntStatsValue);
            break;
          case com.google.android.clockwork.healthservices.types.StatsValue.floatStatsValue:
            com.google.android.clockwork.healthservices.types.StatsValue.FloatStatsValue
                floatStatsValue = statsValue.getFloatStatsValue();
            StatsValue.FloatStatsValue halFloatStatsValue = new StatsValue.FloatStatsValue();
            halFloatStatsValue.minimumValue = floatStatsValue.minimumValue;
            halFloatStatsValue.maximumValue = floatStatsValue.maximumValue;
            halFloatStatsValue.averageValue = floatStatsValue.averageValue;

            halOffsetValue.setFloatStatsValue(halFloatStatsValue);
            break;
        }

        halStatsOffset.offsetValues = halOffsetValue;
        halDataOffset.setStatsOffset(halStatsOffset);
      }
      // TODO(b/225088270): Figure out error handling with these conversion libraries. Should we
      // explicitly check for FLOAT type and throw an exception if invalid?

      halDataOffsets[i] = halDataOffset;
    }
    return halDataOffsets;
  }

  static TrackingConfig toHalTrackingConfig(
      com.google.android.clockwork.healthservices.types.TrackingConfig trackingConfig) {
    TrackingConfig halTrackingConfig = new TrackingConfig();

    halTrackingConfig.exerciseType = trackingConfig.exerciseType;
    halTrackingConfig.enableGpsControl = trackingConfig.enableGpsControl;
    halTrackingConfig.maxReportLatencyMs = trackingConfig.maxReportLatencyMs;
    halTrackingConfig.samplingPeriodMs = trackingConfig.samplingPeriodMs;

    return halTrackingConfig;
  }

  static HrAlertParams toHalHrAlertParams(
      com.google.android.clockwork.healthservices.types.HrAlertParams hrAlertParams) {
    vendor.google_clockwork.healthservices.HrAlertParams halHrAlertParams =
        new vendor.google_clockwork.healthservices.HrAlertParams();

    halHrAlertParams.type = hrAlertParams.type;
    halHrAlertParams.durationSec = hrAlertParams.durationSec;
    halHrAlertParams.thresholdBpm = hrAlertParams.thresholdBpm;

    return halHrAlertParams;
  }

  static GolfShotDetectionParams toHalGolfShotDetectionParams(
      com.google.android.clockwork.healthservices.types.GolfShotDetectionParams
          golfShotDetectionParams) {
    GolfShotDetectionParams halGolfShotDetectionParams = new GolfShotDetectionParams();

    halGolfShotDetectionParams.location = golfShotDetectionParams.location;

    return halGolfShotDetectionParams;
  }

  static DataTypeGoal toHalDataTypeGoal(
      com.google.android.clockwork.healthservices.types.DataTypeGoal dataTypeGoal) {
    DataTypeGoal halDataTypeGoal = new DataTypeGoal();

    halDataTypeGoal.dataType = dataTypeGoal.dataType;
    halDataTypeGoal.threshold = new DataTypeGoal.GoalThreshold();
    if (dataTypeGoal.threshold.type
        == com.google.android.clockwork.healthservices.types.GoalThresholdType.INT) {
      halDataTypeGoal.threshold.setIntThreshold(dataTypeGoal.threshold.intValue);
    } else if (dataTypeGoal.threshold.type
        == com.google.android.clockwork.healthservices.types.GoalThresholdType.FLOAT) {
      halDataTypeGoal.threshold.setFloatThreshold(dataTypeGoal.threshold.floatValue);
    } else {
      halDataTypeGoal.threshold.setByteArrayThreshold(dataTypeGoal.threshold.byteValue);
    }

    halDataTypeGoal.comparisonType = dataTypeGoal.comparisonType;

    return halDataTypeGoal;
  }

  static com.google.android.clockwork.healthservices.types.DataTypeGoal fromHalDataTypeGoal(
      DataTypeGoal halDataTypeGoal) {
    com.google.android.clockwork.healthservices.types.DataTypeGoal dataTypeGoal =
        new com.google.android.clockwork.healthservices.types.DataTypeGoal();

    dataTypeGoal.dataType = halDataTypeGoal.dataType;
    dataTypeGoal.threshold = new com.google.android.clockwork.healthservices.types.GoalThreshold();
    switch (halDataTypeGoal.threshold.getTag()) {
      case DataTypeGoal.GoalThreshold.floatThreshold:
        dataTypeGoal.threshold.type =
            com.google.android.clockwork.healthservices.types.GoalThresholdType.FLOAT;
        dataTypeGoal.threshold.floatValue = halDataTypeGoal.threshold.getFloatThreshold();
        break;
      case DataTypeGoal.GoalThreshold.intThreshold:
        dataTypeGoal.threshold.type =
            com.google.android.clockwork.healthservices.types.GoalThresholdType.INT;
        dataTypeGoal.threshold.intValue = halDataTypeGoal.threshold.getIntThreshold();
        break;
    }

    dataTypeGoal.comparisonType = halDataTypeGoal.comparisonType;
    return dataTypeGoal;
  }

  static com.google.android.clockwork.healthservices.types.DataTypeGoal[] fromHalDataTypeGoals(
      DataTypeGoal[] halDataTypeGoals) {
    com.google.android.clockwork.healthservices.types.DataTypeGoal[] dataTypeGoals =
        new com.google.android.clockwork.healthservices.types.DataTypeGoal[halDataTypeGoals.length];

    for (int i = 0; i < halDataTypeGoals.length; i++) {
      DataTypeGoal halDataTypeGoal = halDataTypeGoals[i];
      dataTypeGoals[i] = fromHalDataTypeGoal(halDataTypeGoal);
    }

    return dataTypeGoals;
  }

  static com.google.android.clockwork.healthservices.types.AchievedGoal fromHalAchievedGoal(
      AchievedGoal halAchievedGoal) {
    com.google.android.clockwork.healthservices.types.AchievedGoal achievedGoal =
        new com.google.android.clockwork.healthservices.types.AchievedGoal();

    achievedGoal.goal = fromHalDataTypeGoal(halAchievedGoal.goal);
    achievedGoal.elapsedTimeSinceBootNanos = halAchievedGoal.elapsedTimeSinceBootNanos;

    return achievedGoal;
  }

  static com.google.android.clockwork.healthservices.types.AvailabilityUpdate
      fromHalAvailabilityUpdate(AvailabilityUpdate halAvailabilityUpdate) {
    com.google.android.clockwork.healthservices.types.AvailabilityUpdate availabilityUpdate =
        new com.google.android.clockwork.healthservices.types.AvailabilityUpdate();
    switch (halAvailabilityUpdate.getTag()) {
      case AvailabilityUpdate.dataTypeAvailability:
        com.google.android.clockwork.healthservices.types.DataTypeAvailability
            dataTypeAvailability =
                new com.google.android.clockwork.healthservices.types.DataTypeAvailability();
        AvailabilityUpdate.DataTypeAvailability halDataTypeAvailability =
            halAvailabilityUpdate.getDataTypeAvailability();
        dataTypeAvailability.dataType = halDataTypeAvailability.dataType;
        dataTypeAvailability.dataTypeAvailabilityStatus =
            halDataTypeAvailability.dataTypeAvailabilityStatus;
        dataTypeAvailability.elapsedTimeSinceBootNanos =
            halDataTypeAvailability.elapsedTimeSinceBootNanos;
        availabilityUpdate.dataTypeAvailability = dataTypeAvailability;
        break;
      case AvailabilityUpdate.locationAvailability:
        com.google.android.clockwork.healthservices.types.LocationAvailability
            locationAvailability =
                new com.google.android.clockwork.healthservices.types.LocationAvailability();
        AvailabilityUpdate.LocationAvailability halLocationAvailability =
            halAvailabilityUpdate.getLocationAvailability();
        locationAvailability.locationAvailabilityStatus =
            halLocationAvailability.locationAvailabilityStatus;
        locationAvailability.elapsedTimeSinceBootNanos =
            halLocationAvailability.elapsedTimeSinceBootNanos;
        availabilityUpdate.locationAvailability = locationAvailability;
        break;
    }
    return availabilityUpdate;
  }

  static com.google.android.clockwork.healthservices.types.HealthEvent fromHalHealthEvent(
      HealthEvent halHealthEvent) {
    com.google.android.clockwork.healthservices.types.HealthEvent healthEvent =
        new com.google.android.clockwork.healthservices.types.HealthEvent();

    healthEvent.type = halHealthEvent.type;
    healthEvent.elapsedTimeSinceBootNanos = halHealthEvent.elapsedTimeSinceBootNanos;

    return healthEvent;
  }

  static com.google.android.clockwork.healthservices.types.ExerciseEvent fromHalExerciseEvent(
      ExerciseEvent halExerciseEvent) {
    // GolfShotDetection is currently the only event in place.
    return fromHalGolfShotDetectionEvent(halExerciseEvent.getGolfShotDetectionEvent());
  }

  static com.google.android.clockwork.healthservices.types.ExerciseEvent
      fromHalGolfShotDetectionEvent(GolfShotDetectionEvent halGolfShotDetectionEvent) {
    com.google.android.clockwork.healthservices.types.GolfShotDetectionEvent
        golfShotDetectionEvent =
            new com.google.android.clockwork.healthservices.types.GolfShotDetectionEvent();

    golfShotDetectionEvent.elapsedTimeSinceBootNanos =
        halGolfShotDetectionEvent.elapsedTimeSinceBootNanos;
    golfShotDetectionEvent.type = halGolfShotDetectionEvent.type;
    golfShotDetectionEvent.contactDetected = halGolfShotDetectionEvent.contactDetected;

    com.google.android.clockwork.healthservices.types.ExerciseEvent exerciseEvent =
        new com.google.android.clockwork.healthservices.types.ExerciseEvent();
    exerciseEvent.setGolfShotDetectionEvent(golfShotDetectionEvent);
    return exerciseEvent;
  }

  static com.google.android.clockwork.healthservices.types.AutoExerciseEvent
      fromHalAutoExerciseEvent(AutoExerciseEvent halAutoExerciseEvent) {
    switch (halAutoExerciseEvent.getTag()) {
      case vendor.google_clockwork.healthservices.AutoExerciseEvent.autoPauseEvent:
        return fromHalAutoPauseEvent(halAutoExerciseEvent.getAutoPauseEvent());
      case vendor.google_clockwork.healthservices.AutoExerciseEvent.autoStartEvent:
        return fromHalAutoStartEvent(halAutoExerciseEvent.getAutoStartEvent());
      default: // vendor.google_clockwork.healthservices.AutoExerciseEvent.autoStopEvent
        return fromHalAutoStopEvent(halAutoExerciseEvent.getAutoStopEvent());
    }
  }

  static com.google.android.clockwork.healthservices.types.AutoExerciseEvent fromHalAutoPauseEvent(
      AutoPauseEvent halAutoPauseEvent) {
    com.google.android.clockwork.healthservices.types.AutoPauseEvent autoPauseEvent =
        new com.google.android.clockwork.healthservices.types.AutoPauseEvent();

    autoPauseEvent.type = halAutoPauseEvent.type;
    autoPauseEvent.elapsedTimeSinceBootNanos = halAutoPauseEvent.elapsedTimeSinceBootNanos;

    com.google.android.clockwork.healthservices.types.AutoExerciseEvent autoExerciseEvent =
        new com.google.android.clockwork.healthservices.types.AutoExerciseEvent();
    autoExerciseEvent.setAutoPauseEvent(autoPauseEvent);
    return autoExerciseEvent;
  }

  static com.google.android.clockwork.healthservices.types.AutoExerciseEvent fromHalAutoStartEvent(
      AutoStartEvent halAutoStartEvent) {
    com.google.android.clockwork.healthservices.types.AutoStartEvent autoStartEvent =
        new com.google.android.clockwork.healthservices.types.AutoStartEvent();

    autoStartEvent.elapsedTimeSinceBootNanos = halAutoStartEvent.elapsedTimeSinceBootNanos;
    autoStartEvent.exerciseType = halAutoStartEvent.exerciseType;
    autoStartEvent.type = halAutoStartEvent.type;

    com.google.android.clockwork.healthservices.types.AutoExerciseEvent autoExerciseEvent =
        new com.google.android.clockwork.healthservices.types.AutoExerciseEvent();
    autoExerciseEvent.setAutoStartEvent(autoStartEvent);
    return autoExerciseEvent;
  }

  static AutoStartEvent toHalAutoStartEvent(
      com.google.android.clockwork.healthservices.types.AutoStartEvent autoStartEvent) {
    AutoStartEvent halAutoStartEvent = new AutoStartEvent();

    halAutoStartEvent.elapsedTimeSinceBootNanos = autoStartEvent.elapsedTimeSinceBootNanos;
    halAutoStartEvent.exerciseType = autoStartEvent.exerciseType;
    halAutoStartEvent.type = autoStartEvent.type;

    return halAutoStartEvent;
  }

  static com.google.android.clockwork.healthservices.types.AutoExerciseEvent fromHalAutoStopEvent(
      AutoStopEvent halAutoStopEvent) {
    com.google.android.clockwork.healthservices.types.AutoStopEvent autoStopEvent =
        new com.google.android.clockwork.healthservices.types.AutoStopEvent();

    autoStopEvent.elapsedTimeSinceBootNanos = halAutoStopEvent.elapsedTimeSinceBootNanos;

    com.google.android.clockwork.healthservices.types.AutoExerciseEvent autoExerciseEvent =
        new com.google.android.clockwork.healthservices.types.AutoExerciseEvent();
    autoExerciseEvent.setAutoStopEvent(autoStopEvent);
    return autoExerciseEvent;
  }

  static AutoStartConfig[] toHalAutoStartConfigs(
      com.google.android.clockwork.healthservices.types.AutoStartConfig[] autoStartConfigs) {
    AutoStartConfig[] halAutoStartConfigs = new AutoStartConfig[autoStartConfigs.length];

    for (int i = 0; i < autoStartConfigs.length; i++) {
      halAutoStartConfigs[i] = toHalAutoStartConfig(autoStartConfigs[i]);
    }
    return halAutoStartConfigs;
  }

  static AutoStartConfig toHalAutoStartConfig(
      com.google.android.clockwork.healthservices.types.AutoStartConfig autoStartConfig) {
    AutoStartConfig halAutoStartConfig = new AutoStartConfig();

    halAutoStartConfig.exerciseType = autoStartConfig.exerciseType;
    halAutoStartConfig.minimumNotificationLatencyMs = autoStartConfig.minimumNotificationLatencyMs;

    return halAutoStartConfig;
  }

  static com.google.android.clockwork.healthservices.types.DataUpdate fromHalLocationData(
      LocationData halLocationData) {
    com.google.android.clockwork.healthservices.types.DataUpdate dataUpdate =
        new com.google.android.clockwork.healthservices.types.DataUpdate();

    com.google.android.clockwork.healthservices.types.LocationData locationData =
        new com.google.android.clockwork.healthservices.types.LocationData();

    locationData.latitude = halLocationData.latitude;
    locationData.longitude = halLocationData.longitude;
    locationData.horizontalPositionErrorMeters = halLocationData.horizontalPositionErrorMeters;
    locationData.bearing = halLocationData.bearing;
    locationData.elapsedTimeSinceBootNanos = halLocationData.elapsedTimeSinceBootNanos;

    dataUpdate.locationData = locationData;

    return dataUpdate;
  }

  static com.google.android.clockwork.healthservices.types.DataUpdate fromHalStatsData(
      StatsData halStatsData) {
    com.google.android.clockwork.healthservices.types.DataUpdate dataUpdate =
        new com.google.android.clockwork.healthservices.types.DataUpdate();

    com.google.android.clockwork.healthservices.types.StatsData statsData =
        new com.google.android.clockwork.healthservices.types.StatsData();
    statsData.dataType = halStatsData.dataType;
    // TODO(b/267839982): Pipe through elapsedTimeSinceBootNanos when present.

    com.google.android.clockwork.healthservices.types.StatsValue statsValue =
        new com.google.android.clockwork.healthservices.types.StatsValue();

    switch (halStatsData.statsValue.getTag()) {
      case StatsValue.intStatsValue:
        StatsValue.IntStatsValue halIntStatsValue = halStatsData.statsValue.getIntStatsValue();

        com.google.android.clockwork.healthservices.types.StatsValue.IntStatsValue intStatsValue =
            new com.google.android.clockwork.healthservices.types.StatsValue.IntStatsValue();
        intStatsValue.minimumValue = halIntStatsValue.minimumValue;
        intStatsValue.maximumValue = halIntStatsValue.maximumValue;
        intStatsValue.averageValue = halIntStatsValue.averageValue;

        statsValue.setIntStatsValue(intStatsValue);
        break;
      case StatsValue.floatStatsValue:
        StatsValue.FloatStatsValue halFloatStatsValue =
            halStatsData.statsValue.getFloatStatsValue();

        com.google.android.clockwork.healthservices.types.StatsValue.FloatStatsValue
            floatStatsValue =
                new com.google.android.clockwork.healthservices.types.StatsValue.FloatStatsValue();
        floatStatsValue.minimumValue = halFloatStatsValue.minimumValue;
        floatStatsValue.maximumValue = halFloatStatsValue.maximumValue;
        floatStatsValue.averageValue = halFloatStatsValue.averageValue;

        statsValue.setFloatStatsValue(floatStatsValue);
        break;
    }

    statsData.statsValue = statsValue;
    dataUpdate.statsData = statsData;

    return dataUpdate;
  }

  static com.google.android.clockwork.healthservices.types.DataUpdate fromHalMetricData(
      MetricData halMetricData) {
    com.google.android.clockwork.healthservices.types.DataUpdate dataUpdate =
        new com.google.android.clockwork.healthservices.types.DataUpdate();

    com.google.android.clockwork.healthservices.types.MetricData metricData =
        new com.google.android.clockwork.healthservices.types.MetricData();

    metricData.dataType = halMetricData.dataType;
    metricData.accuracy = halMetricData.accuracy;
    metricData.elapsedTimeSinceBootNanos = halMetricData.elapsedTimeSinceBootNanos;

    com.google.android.clockwork.healthservices.types.MetricValue metricValue =
        new com.google.android.clockwork.healthservices.types.MetricValue();

    switch (halMetricData.metricValue.getTag()) {
      case MetricValue.intValue:
        metricValue.type = com.google.android.clockwork.healthservices.types.MetricValueType.INT;
        metricValue.intValue = halMetricData.metricValue.getIntValue();
        break;
      case MetricValue.floatValue:
        metricValue.type = com.google.android.clockwork.healthservices.types.MetricValueType.FLOAT;
        metricValue.floatValue = halMetricData.metricValue.getFloatValue();
        break;
      case MetricValue.byteValue:
        metricValue.type =
            com.google.android.clockwork.healthservices.types.MetricValueType.BYTE_ARRAY;
        metricValue.byteArrayValue = halMetricData.metricValue.getByteValue();
        break;
    }
    metricData.metricValue = metricValue;

    dataUpdate.metricData = metricData;
    return dataUpdate;
  }

  static com.google.android.clockwork.healthservices.types.AutoStartCapabilities
      fromHalAutoStartCapabilities(AutoStartCapabilities halAutoStartCapabilities) {
    com.google.android.clockwork.healthservices.types.AutoStartCapabilities autoStartCapabilities =
        new com.google.android.clockwork.healthservices.types.AutoStartCapabilities();
    autoStartCapabilities.capabilities =
        new com.google.android.clockwork.healthservices.types.AutoStartCapabilities
                .AutoStartCapability[halAutoStartCapabilities.capabilities.length];

    for (int i = 0; i < halAutoStartCapabilities.capabilities.length; i++) {
      autoStartCapabilities.capabilities[i] =
          fromHalAutoStartCapability(halAutoStartCapabilities.capabilities[i]);
    }
    return autoStartCapabilities;
  }

  static com.google.android.clockwork.healthservices.types.AutoStartCapabilities.AutoStartCapability
      fromHalAutoStartCapability(AutoStartCapabilities.AutoStartCapability halAutoStartCapability) {
    com.google.android.clockwork.healthservices.types.AutoStartCapabilities.AutoStartCapability
        autoStartCapability =
            new com.google.android.clockwork.healthservices.types.AutoStartCapabilities
                .AutoStartCapability();
    autoStartCapability.exerciseType = halAutoStartCapability.exerciseType;
    autoStartCapability.supportedDataTypes = halAutoStartCapability.supportedDataTypes;

    return autoStartCapability;
  }

  private HalTypeConverterV1() {}
}
