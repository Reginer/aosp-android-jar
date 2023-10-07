package com.android.clockwork.healthservices;

import android.annotation.Nullable;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.util.Log;

import com.google.android.clockwork.healthservices.types.AchievedGoal;
import com.google.android.clockwork.healthservices.types.AutoExerciseEvent;
import com.google.android.clockwork.healthservices.types.AutoStartCapabilities;
import com.google.android.clockwork.healthservices.types.AutoStartConfig;
import com.google.android.clockwork.healthservices.types.AutoStartEvent;
import com.google.android.clockwork.healthservices.types.AvailabilityUpdate;
import com.google.android.clockwork.healthservices.types.DataTypeOffset;
import com.google.android.clockwork.healthservices.types.DataTypeGoal;
import com.google.android.clockwork.healthservices.types.DataUpdate;
import com.google.android.clockwork.healthservices.types.ExerciseEvent;
import com.google.android.clockwork.healthservices.types.GolfShotDetectionParams;
import com.google.android.clockwork.healthservices.types.HealthEvent;
import com.google.android.clockwork.healthservices.types.HrAlertParams;
import com.google.android.clockwork.healthservices.types.TrackingConfig;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;

/**
 * HalAdapter is a wrapper class for different versions of the IHealthServices HAL.
 *
 * <p>The purpose of this class is to decouple the HAL references from HealthServices system service
 * so requests can be made for HAL operations without caring about HAL version.
 *
 * <p>The HalAdapter registers all versions of HAL. When an HAL operation is requested, HalAdapter
 * tries to use the highest version of (successfully registered) HAL.
 */
public class HalAdapter extends IServiceNotification.Stub
    implements IBinder.DeathRecipient, IHwBinder.DeathRecipient {

  private static final String TAG = "HsHalAdapter";

  public static final String HAL_NAME =
      "vendor.google_clockwork.healthservices.IHealthServices/default";

  // HAL service connections.
  // Note: Confusingly, HIDL interface uses V1_0 versioning while first AIDL interface is called V1.
  // To simplify, we rename the HIDL interface V0 locally.
  private vendor.google_clockwork.healthservices.V1_0.IHealthServices mHealthServicesHalV0 = null;
  private vendor.google_clockwork.healthservices.IHealthServices mHealthServicesHalV1 = null;

  // HAL callback receivers.
  private final HalCallbackReceiverHidl mHalCallbackReceiverV0;
  private final HalCallbackReceiverV1 mHalCallbackReceiverV1;

  private final HalListener mHalListener;
  private final ServiceManagerStub mServiceManagerStub;

  HalAdapter(HalListener halListener, HalAdapter.ServiceManagerStub serviceManagerStub) {
    mHalListener = halListener;
    mHalCallbackReceiverV0 = new HalCallbackReceiverHidl(halListener);
    mHalCallbackReceiverV1 = new HalCallbackReceiverV1(halListener);
    mServiceManagerStub = serviceManagerStub;
  }

  HalAdapter(HalListener halListener) {
    this(halListener, new HalAdapter.ServiceManagerStub() {});
  }

  /** Stub interface into {@link ServiceManager} for testing. */
  interface ServiceManagerStub {

    default @Nullable vendor.google_clockwork.healthservices.IHealthServices
        waitForDeclaredService() {
      return vendor.google_clockwork.healthservices.IHealthServices.Stub.asInterface(
          ServiceManager.waitForDeclaredService(HAL_NAME));
    }

    default IServiceManager getIServiceManager() throws RemoteException {
      return IServiceManager.getService();
    }

    default vendor.google_clockwork.healthservices.V1_0.IHealthServices getV0HalService()
        throws RemoteException {
      return vendor.google_clockwork.healthservices.V1_0.IHealthServices.getService();
    }
  }

  public synchronized void onRegistration(String fqName, String name, boolean preexisting) {
    Log.d(
        TAG,
        "[HAL] onRegistration: service notification fqName="
            + fqName
            + ", name="
            + name
            + ", preexisting="
            + preexisting);
    try {
      vendor.google_clockwork.healthservices.V1_0.IHealthServices hal =
          mServiceManagerStub.getV0HalService();
      Log.d(TAG, "[HAL] Got V0.");
      mHealthServicesHalV0 = hal;
      mHealthServicesHalV0.setDataListener(mHalCallbackReceiverV0);
      mHealthServicesHalV0.linkToDeath(/* recipient= */ this, /* cookie= */ 1);

      mHalListener.onHalConnected();
    } catch (RemoteException e) {
      Log.e(TAG, "[HAL] Exception talking to the HAL: ", e);
    }
  }

  public synchronized void registerHalV1_0RegistrationNotification() {
    try {
      IServiceManager serviceManager = mServiceManagerStub.getIServiceManager();
      if (serviceManager != null
          && !serviceManager.registerForNotifications(
              vendor.google_clockwork.healthservices.V1_0.IHealthServices.kInterfaceName,
              "",
              this)) {
        Log.e(TAG, "serviceManager callback registration failed");
      }
    } catch (RemoteException e) {
      Log.e(TAG, "Exception while registering HAL registration notification: " + e);
    }
  }

  @Override // IHwBinder.DeathRecipient
  public synchronized void serviceDied(long cookie) {
    Log.d(TAG, "HAL V1.0 died.");
    if (mHealthServicesHalV0 != null) {
      mHealthServicesHalV0.asBinder().unlinkToDeath(this);
      mHealthServicesHalV0 = null;
    }
    mHalListener.onHalDied();
  }

  public synchronized void maybeGetAidlHalService() {
    try {
      Log.d(TAG, "[HAL] Getting HAL (aidl) service.");
      vendor.google_clockwork.healthservices.IHealthServices mHal =
          mServiceManagerStub.waitForDeclaredService();
      Log.d(TAG, "[HAL] Done waiting for service.");
      if (mHal == null) {
        Log.d(TAG, "[HAL] HAL V1 is NULL");
        return;
      }
      mHealthServicesHalV1 = mHal;
    } catch (SecurityException e) {
      Log.e(TAG, "[HAL] Problem starting V1 HAL.", e);
    }

    try {
      int remoteVer = mHealthServicesHalV1.getInterfaceVersion();
      Log.i(TAG, "[HAL] HAL V1 version: " + remoteVer);
    } catch (RemoteException e) {
      Log.e(TAG, "[HAL] Problem reading HAL version.", e);
    }

    try {
      mHealthServicesHalV1.setDataListener(mHalCallbackReceiverV1);
      mHealthServicesHalV1.asBinder().linkToDeath(this, 0);
      mHalListener.onHalConnected();
    } catch (RemoteException e) {
      Log.e(TAG, "Error setting up HAL listeners:", e);
    }
  }

  @Override // IBinder.DeathRecipient
  public synchronized void binderDied() {
    Log.d(TAG, "HAL V1 died.");
    if (mHealthServicesHalV1 != null) {
      mHealthServicesHalV1.asBinder().unlinkToDeath(this, 0);
    }
    mHalListener.onHalDied();

    // Try to reconnect.
    maybeGetAidlHalService();
  }

  public synchronized int[] getSupportedDataTypes() {
    try {
      if (mHealthServicesHalV1 != null) {
        return mHealthServicesHalV1.getSupportedMetrics();
      } else if (mHealthServicesHalV0 != null) {
        ArrayList<Integer> halDataTypes = mHealthServicesHalV0.getSupportedMetrics();
        int[] supportedDataTypes = new int[halDataTypes.size()];
        for (int i = 0; i < halDataTypes.size(); i++) {
          // Note: This requires the HAL types to match the aidl types exactly.
          supportedDataTypes[i] = halDataTypes.get(i);
        }
        return supportedDataTypes;
      } else {
        Log.d(TAG, "HAL implementation is currently unconnected.");
      }
    } catch (RemoteException e) {
      Log.w(TAG, "Error reading supported data types out of WHS HAL:", e);
    }

    return new int[0];
  }

  public synchronized DataTypeGoal[] getSupportedGoals() {
    try {
      if (mHealthServicesHalV1 != null) {
        vendor.google_clockwork.healthservices.DataTypeGoal[] halDataTypeGoals =
            mHealthServicesHalV1.getSupportedGoals();
        return HalTypeConverterV1.fromHalDataTypeGoals(halDataTypeGoals);
      } else if (mHealthServicesHalV0 != null) {
        ArrayList<vendor.google_clockwork.healthservices.V1_0.DataTypeGoal> halDataTypeGoals =
            mHealthServicesHalV0.getSupportedGoals();
        DataTypeGoal[] supportedGoals = new DataTypeGoal[halDataTypeGoals.size()];
        for (int i = 0; i < halDataTypeGoals.size(); i++) {
          supportedGoals[i] = HalTypeConverterHidl.toAidlDataTypeGoal(halDataTypeGoals.get(i));
        }
        return supportedGoals;
      } else {
        Log.d(TAG, "HAL implementation is currently unconnected.");
      }
    } catch (RemoteException e) {
      Log.w(TAG, "Error reading supported data type goals out of WHS HAL:", e);
    }
    return new DataTypeGoal[0];
  }

  public synchronized int[] getAutoPauseAndResumeEnabledExerciseTypes() {
    try {
      if (mHealthServicesHalV1 != null) {
        return mHealthServicesHalV1.getAutoPauseAndResumeEnabledExerciseTypes();
      } else if (mHealthServicesHalV0 != null) {
        ArrayList<Integer> halExerciseTypes =
            mHealthServicesHalV0.getAutoPauseAndResumeEnabledExerciseTypes();
        int[] supportedExerciseTypes = new int[halExerciseTypes.size()];
        for (int i = 0; i < halExerciseTypes.size(); i++) {
          // Note: This requires the HAL types to match the aidl types exactly.
          supportedExerciseTypes[i] = halExerciseTypes.get(i);
        }
        return supportedExerciseTypes;
      } else {
        Log.d(TAG, "HAL implementation is currently unconnected.");
      }
    } catch (RemoteException e) {
      Log.w(TAG, "Error reading auto-pause enabled exercise types from HAL:", e);
    }

    return new int[0];
  }

  public synchronized int[] getAutoStopEnabledExerciseTypes() {
    try {
      if (mHealthServicesHalV1 != null) {
        return mHealthServicesHalV1.getAutoStopEnabledExerciseTypes();
      } else if (mHealthServicesHalV0 != null) {
        // HAL V0 doesn't support auto-stop.
        return new int[0];
      } else {
        Log.d(TAG, "HAL implementation is currently unconnected.");
      }
    } catch (RemoteException e) {
      Log.w(TAG, "Error reading auto-stop enabled exercise types from HAL:", e);
    }

    return new int[0];
  }

  public synchronized AutoStartCapabilities getAutoStartCapabilities() {
    try {
      if (mHealthServicesHalV1 != null) {
        vendor.google_clockwork.healthservices.AutoStartCapabilities halAutoStartCapabilities =
            mHealthServicesHalV1.getAutoStartCapabilities();
        return HalTypeConverterV1.fromHalAutoStartCapabilities(halAutoStartCapabilities);
      } else if (mHealthServicesHalV0 != null) {
        // HAL V0 doesn't support auto-start.
        return new AutoStartCapabilities();
      } else {
        Log.d(TAG, "HAL implementation is currently unconnected.");
      }
    } catch (RemoteException e) {
      Log.w(TAG, "Error reading AutoStartCapabilities from HAL:", e);
    }

    return new AutoStartCapabilities();
  }

  public synchronized void startTracking(
      TrackingConfig config, int[] dataTypes, DataTypeOffset[] offsets, boolean isPaused) {
    if (mHealthServicesHalV1 != null) {
      vendor.google_clockwork.healthservices.TrackingConfig halTrackingConfig =
          HalTypeConverterV1.toHalTrackingConfig(config);
      vendor.google_clockwork.healthservices.DataTypeOffset[] halDataTypesOffsets =
          HalTypeConverterV1.toHalDataTypeOffsets(offsets);
      try {
        mHealthServicesHalV1.startTracking(
            halTrackingConfig, dataTypes, halDataTypesOffsets, isPaused);
      } catch (UnsupportedOperationException e) {
        Log.e(TAG, "Trying to start unsupported DataType:", e);
      } catch (RemoteException e) {
        Log.w(TAG, "Error starting tracking in the HAL:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to start tracking:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      vendor.google_clockwork.healthservices.V1_0.TrackingConfig halTrackingConfig =
          HalTypeConverterHidl.toHalTrackingConfig(config);
      ArrayList<Integer> dataTypesList = HalTypeConverterHidl.convertToHalDataTypeList(dataTypes);
      ArrayList<vendor.google_clockwork.healthservices.V1_0.DataTypeOffset> halDataTypesOffsets =
          HalTypeConverterHidl.convertToHalDataTypeOffsets(offsets);
      try {
        mHealthServicesHalV0.startTracking(halTrackingConfig, dataTypesList, halDataTypesOffsets);
        if (isPaused) {
          mHealthServicesHalV0.pauseTracking(dataTypesList);
        }
      } catch (RemoteException e) {
        Log.w(TAG, "Error starting tracking:", e);
      }
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public void startAutomaticExerciseDetection(
      AutoStartConfig[] autoStartConfigs, AutoStartEvent autoStartOffset) {
    if (mHealthServicesHalV1 != null) {
      vendor.google_clockwork.healthservices.AutoStartConfig[] halAutoStartConfigs =
          HalTypeConverterV1.toHalAutoStartConfigs(autoStartConfigs);
      vendor.google_clockwork.healthservices.AutoStartEvent halAutoStartEvent =
          autoStartOffset != null ? HalTypeConverterV1.toHalAutoStartEvent(autoStartOffset) : null;
      try {
        mHealthServicesHalV1.startAutomaticExerciseDetection(
            halAutoStartConfigs, halAutoStartEvent);
      } catch (UnsupportedOperationException e) {
        Log.e(TAG, "Trying to start unsupported automatic exercise tracking:", e);
      } catch (RemoteException e) {
        Log.w(TAG, "Error starting auto exercise tracking in the HAL:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to start automatic exercise tracking:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      Log.w(TAG, "Something has gone wrong. Attempting to start AutoStart tracking on V0 HAL.");
      return;
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public void stopAutomaticExerciseDetection(int[] exerciseTypes) {
    if (mHealthServicesHalV1 != null) {
      try {
        mHealthServicesHalV1.stopAutomaticExerciseDetection(exerciseTypes);
      } catch (RemoteException e) {
        Log.w(TAG, "Error stopping auto exercise tracking in the HAL:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to stop automatic exercise tracking:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      Log.w(TAG, "Something has gone wrong. Attempting to stop AutoStart tracking on V0 HAL.");
      return;
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public synchronized void updateTrackingConfig(TrackingConfig config, int[] dataTypes) {
    if (mHealthServicesHalV1 != null) {
      vendor.google_clockwork.healthservices.TrackingConfig halTrackingConfig =
          HalTypeConverterV1.toHalTrackingConfig(config);
      try {
        mHealthServicesHalV1.updateTrackingConfig(halTrackingConfig, dataTypes);
      } catch (IllegalArgumentException e) {
        Log.e(TAG, "Trying to update config for data types that are not being tracked:", e);
      } catch (RemoteException e) {
        Log.w(TAG, "Error updating tracking config:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to update tracking config:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      vendor.google_clockwork.healthservices.V1_0.TrackingConfig halTrackingConfig =
          HalTypeConverterHidl.toHalTrackingConfig(config);
      ArrayList<Integer> dataTypesList = HalTypeConverterHidl.convertToHalDataTypeList(dataTypes);
      try {
        mHealthServicesHalV0.updateTrackingConfig(halTrackingConfig, dataTypesList);
      } catch (RemoteException e) {
        Log.w(TAG, "Error updating tracking config:", e);
      }
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public synchronized void pauseTracking(int[] dataTypes) {
    if (mHealthServicesHalV1 != null) {
      try {
        mHealthServicesHalV1.pauseTracking(dataTypes);
      } catch (IllegalArgumentException e) {
        Log.e(TAG, "Trying to pause data types that are not being tracked:", e);
      } catch (RemoteException e) {
        Log.w(TAG, "Error pausing data types:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to pause tracking:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      ArrayList<Integer> dataTypesList = HalTypeConverterHidl.convertToHalDataTypeList(dataTypes);
      try {
        mHealthServicesHalV0.pauseTracking(dataTypesList);
      } catch (RemoteException e) {
        Log.w(TAG, "Error pausing data types:", e);
      }
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public synchronized void resumeTracking(int[] dataTypes) {
    if (mHealthServicesHalV1 != null) {
      try {
        mHealthServicesHalV1.resumeTracking(dataTypes);
      } catch (IllegalArgumentException e) {
        Log.e(TAG, "Trying to resume data types that are not being tracked:", e);
      } catch (RemoteException e) {
        Log.w(TAG, "Error resume data types:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to resume tracking:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      ArrayList<Integer> dataTypesList = HalTypeConverterHidl.convertToHalDataTypeList(dataTypes);
      try {
        mHealthServicesHalV0.resumeTracking(dataTypesList);
      } catch (RemoteException e) {
        Log.w(TAG, "Error resuming data types:", e);
      }
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public synchronized void stopTracking(int[] dataTypes) {
    if (mHealthServicesHalV1 != null) {
      try {
        mHealthServicesHalV1.stopTracking(dataTypes);
      } catch (RemoteException e) {
        Log.w(TAG, "Error stopping data types:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to stop data types:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      ArrayList<Integer> dataTypesList = HalTypeConverterHidl.convertToHalDataTypeList(dataTypes);
      try {
        mHealthServicesHalV0.stopTracking(dataTypesList);
      } catch (RemoteException e) {
        Log.w(TAG, "Error stopping data types:", e);
      }
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public synchronized void flush(int flushId, int[] dataTypes) {
    if (mHealthServicesHalV1 != null) {
      try {
        mHealthServicesHalV1.flush(flushId, dataTypes);
      } catch (RemoteException e) {
        Log.w(TAG, "Error flushing data types:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to flush data types:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      ArrayList<Integer> dataTypesList = HalTypeConverterHidl.convertToHalDataTypeList(dataTypes);
      try {
        // Flush ID coordination is handled in HalCallbackReceiverHidl.
        mHealthServicesHalV0.flush(dataTypesList);
      } catch (RemoteException e) {
        Log.w(TAG, "Error flushing data types:", e);
      }
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public synchronized void resetDataTypeOffsets(DataTypeOffset[] offsets) {
    if (mHealthServicesHalV1 != null) {
      vendor.google_clockwork.healthservices.DataTypeOffset[] halDataTypesOffsets =
          HalTypeConverterV1.toHalDataTypeOffsets(offsets);
      try {
        mHealthServicesHalV1.resetDataTypeOffsets(halDataTypesOffsets);
      } catch (IllegalArgumentException e) {
        Log.e(TAG, "Trying to reset data type offsets that are not being tracked:", e);
      } catch (RemoteException e) {
        Log.w(TAG, "Error resetting data types:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to reset data type offsets:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      ArrayList<vendor.google_clockwork.healthservices.V1_0.DataTypeOffset> halDataTypesOffsets =
          HalTypeConverterHidl.convertToHalDataTypeOffsets(offsets);
      try {
        mHealthServicesHalV0.resetDataTypeOffsets(halDataTypesOffsets);
      } catch (RemoteException e) {
        Log.w(TAG, "Error resetting data offsets:", e);
      }
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public synchronized void addGoal(DataTypeGoal goal) {
    if (mHealthServicesHalV1 != null) {
      vendor.google_clockwork.healthservices.DataTypeGoal halGoal =
          HalTypeConverterV1.toHalDataTypeGoal(goal);
      try {
        mHealthServicesHalV1.addGoal(halGoal);
      } catch (IllegalArgumentException e) {
        Log.e(TAG, "Trying to add goal for data type that is not being tracked:", e);
      } catch (UnsupportedOperationException e) {
        Log.e(TAG, "Trying to add goal for data type that is not supported by HAL:", e);
      } catch (RemoteException e) {
        Log.w(TAG, "Error adding goal:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to add goal:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      vendor.google_clockwork.healthservices.V1_0.DataTypeGoal halGoal =
          HalTypeConverterHidl.toHalDataTypeGoal(goal);
      try {
        mHealthServicesHalV0.addGoal(halGoal);
      } catch (RemoteException e) {
        Log.w(TAG, "Error adding goal:", e);
      }
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public synchronized void removeGoal(DataTypeGoal goal) {
    if (mHealthServicesHalV1 != null) {
      vendor.google_clockwork.healthservices.DataTypeGoal halGoal =
          HalTypeConverterV1.toHalDataTypeGoal(goal);
      try {
        mHealthServicesHalV1.removeGoal(halGoal);
      } catch (RemoteException e) {
        Log.w(TAG, "Error removing goal:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to remove goal:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      vendor.google_clockwork.healthservices.V1_0.DataTypeGoal halGoal =
          HalTypeConverterHidl.toHalDataTypeGoal(goal);
      try {
        mHealthServicesHalV0.removeGoal(halGoal);
      } catch (RemoteException e) {
        Log.w(TAG, "Error removing goal:", e);
      }
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public synchronized void setProfile(float heightCm, float weightKg, int ageYears, byte gender) {
    if (mHealthServicesHalV1 != null) {
      try {
        mHealthServicesHalV1.setProfile(heightCm, weightKg, ageYears, gender);
      } catch (RemoteException e) {
        Log.w(TAG, "Error setting profile:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to set profile:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      try {
        mHealthServicesHalV0.setProfile(heightCm, weightKg, ageYears, gender);
      } catch (RemoteException e) {
        Log.w(TAG, "Error setting user profile:", e);
      }

    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public synchronized void setSwimmingPoolLength(int lengthMeters) {
    if (mHealthServicesHalV1 != null) {
      try {
        mHealthServicesHalV1.setSwimmingPoolLength(lengthMeters);
      } catch (RemoteException e) {
        Log.w(TAG, "Error setting swimming pool length:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to set swimming pool length:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      try {
        mHealthServicesHalV0.setSwimmingPoolLength(lengthMeters);
      } catch (RemoteException e) {
        Log.w(TAG, "Error setting swimming pool length:", e);
      }
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public synchronized void setHeartRateAlertParams(HrAlertParams params) {
    if (mHealthServicesHalV1 != null) {
      vendor.google_clockwork.healthservices.HrAlertParams halHrAlertParams =
          HalTypeConverterV1.toHalHrAlertParams(params);
      try {
        mHealthServicesHalV1.setHeartRateAlertParams(halHrAlertParams);
      } catch (UnsupportedOperationException e) {
        Log.e(TAG, "Trying to set heart rate alert params when not supported by HAL:", e);
      } catch (RemoteException e) {
        Log.w(TAG, "Error setting heart rate alert params:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to set heart rate alert params:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      vendor.google_clockwork.healthservices.V1_0.HrAlertParams halHrAlertParams =
          HalTypeConverterHidl.toHalHrAlertParams(params);
      try {
        mHealthServicesHalV0.setHeartRateAlertParams(halHrAlertParams);
      } catch (RemoteException e) {
        Log.w(TAG, "Error setting HR alert params:", e);
      }
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public void setGolfShotDetectionParams(GolfShotDetectionParams params) {
    if (mHealthServicesHalV1 != null) {
      vendor.google_clockwork.healthservices.GolfShotDetectionParams halGolfShotDetectionParams =
          HalTypeConverterV1.toHalGolfShotDetectionParams(params);
      try {
        mHealthServicesHalV1.setGolfShotDetectionParams(halGolfShotDetectionParams);
      } catch (RemoteException e) {
        Log.w(TAG, "Error setting golf shot detection params:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to set golf shot detection params:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      Log.d(TAG, "HAL V0 does not support golf shot detection params.");
      return;
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public synchronized void setTestMode(boolean enableTestMode) {
    if (mHealthServicesHalV1 != null) {
      try {
        mHealthServicesHalV1.setTestMode(enableTestMode);
      } catch (RemoteException e) {
        Log.w(TAG, "Error setting test mode:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to set test mode:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      try {
        mHealthServicesHalV0.setTestMode(enableTestMode);
      } catch (RemoteException e) {
        Log.w(TAG, "Error setting test mode:", e);
      }
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public synchronized void setOemCustomConfiguration(int configId, byte[] configValue) {
    if (mHealthServicesHalV1 != null) {
      try {
        mHealthServicesHalV1.setOemCustomConfiguration(configId, configValue);
      } catch (RemoteException e) {
        Log.w(TAG, "Error setting OEM custom config:", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected problem trying to set oem custom config:", e);
      }
    } else if (mHealthServicesHalV0 != null) {
      try {
        ArrayList<Byte> customConfig = new ArrayList<Byte>(configValue.length);
        for (int i = 0; i < configValue.length; i++) {
          customConfig.add(configValue[i]);
        }
        mHealthServicesHalV0.setOemCustomConfiguration(customConfig);
      } catch (RemoteException e) {
        Log.w(TAG, "Error setting OEM custom config:", e);
      }
    } else {
      Log.d(TAG, "HAL implementation is currently unconnected.");
      return;
    }
  }

  public synchronized boolean isHalServiceConnected() {
    return mHealthServicesHalV1 != null || mHealthServicesHalV0 != null;
  }

  // Listener for changes in HAL connection state.
  interface HalListener {

    void onHalConnected();

    void onHalDied();

    void onAutoExerciseEvent(AutoExerciseEvent autoExerciseEvent);

    void onAvailabilityUpdate(AvailabilityUpdate[] availabilityUpdates);

    void onDataUpdate(DataUpdate[] dataUpdates);

    void onExerciseEvent(ExerciseEvent[] autoExerciseEvent);

    void onFlushCompleted(int flushId);

    void onGoalAchieved(AchievedGoal[] achievedGoals);

    void onHealthEventDetected(HealthEvent healthEvent);
  }
}
