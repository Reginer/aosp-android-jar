/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.telephony.satellite.wrapper;

import static android.telephony.satellite.SatelliteManager.SatelliteException;

import android.annotation.CallbackExecutor;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;
import android.telephony.satellite.AntennaPosition;
import android.telephony.satellite.PointingInfo;
import android.telephony.satellite.SatelliteCapabilities;
import android.telephony.satellite.SatelliteDatagram;
import android.telephony.satellite.SatelliteDatagramCallback;
import android.telephony.satellite.SatelliteManager;
import android.telephony.satellite.SatelliteProvisionStateCallback;
import android.telephony.satellite.SatelliteStateCallback;
import android.telephony.satellite.SatelliteTransmissionUpdateCallback;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Wrapper for satellite operations such as provisioning, pointing, messaging, location sharing,
 * etc. To get the object, call {@link Context#getSystemService(String)}.
 */
public class SatelliteManagerWrapper {
  private static final String TAG = "SatelliteManagerWrapper";

  private static final ConcurrentHashMap<
          SatelliteProvisionStateCallbackWrapper, SatelliteProvisionStateCallback>
      sSatelliteProvisionStateCallbackWrapperMap = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<SatelliteStateCallbackWrapper, SatelliteStateCallback>
      sSatelliteStateCallbackWrapperMap = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<
          SatelliteTransmissionUpdateCallbackWrapper, SatelliteTransmissionUpdateCallback>
      sSatelliteTransmissionUpdateCallbackWrapperMap = new ConcurrentHashMap<>();

  private static final ConcurrentHashMap<
          SatelliteDatagramCallbackWrapper, SatelliteDatagramCallback>
      sSatelliteDatagramCallbackWrapperMap = new ConcurrentHashMap<>();

  private final SatelliteManager mSatelliteManager;

  SatelliteManagerWrapper(Context context) {
    mSatelliteManager = (SatelliteManager) context.getSystemService("satellite");
  }

  /**
   * Factory method.
   *
   * @param context context of application
   */
  public static SatelliteManagerWrapper getInstance(Context context) {
    return new SatelliteManagerWrapper(context);
  }

  /**
   * Datagram type is unknown. This generic datagram type should be used only when the datagram type
   * cannot be mapped to other specific datagram types.
   */
  public static final int DATAGRAM_TYPE_UNKNOWN = 0;
  /** Datagram type indicating that the datagram to be sent or received is of type SOS message. */
  public static final int DATAGRAM_TYPE_SOS_MESSAGE = 1;
  /**
   * Datagram type indicating that the datagram to be sent or received is of type location sharing.
   */
  public static final int DATAGRAM_TYPE_LOCATION_SHARING = 2;

  /** @hide */
  @IntDef(
      prefix = "DATAGRAM_TYPE_",
      value = {DATAGRAM_TYPE_UNKNOWN, DATAGRAM_TYPE_SOS_MESSAGE, DATAGRAM_TYPE_LOCATION_SHARING})
  @Retention(RetentionPolicy.SOURCE)
  public @interface DatagramType {}

  /**
   * Unknown Non-Terrestrial radio technology. This generic radio technology should be used only
   * when the radio technology cannot be mapped to other specific radio technologies.
   */
  public static final int NT_RADIO_TECHNOLOGY_UNKNOWN = 0;
  /** 3GPP NB-IoT (Narrowband Internet of Things) over Non-Terrestrial-Networks technology. */
  public static final int NT_RADIO_TECHNOLOGY_NB_IOT_NTN = 1;
  /** 3GPP 5G NR over Non-Terrestrial-Networks technology. */
  public static final int NT_RADIO_TECHNOLOGY_NR_NTN = 2;
  /** 3GPP eMTC (enhanced Machine-Type Communication) over Non-Terrestrial-Networks technology. */
  public static final int NT_RADIO_TECHNOLOGY_EMTC_NTN = 3;
  /** Proprietary technology. */
  public static final int NT_RADIO_TECHNOLOGY_PROPRIETARY = 4;

  /** @hide */
  @IntDef(
      prefix = "NT_RADIO_TECHNOLOGY_",
      value = {
        NT_RADIO_TECHNOLOGY_UNKNOWN,
        NT_RADIO_TECHNOLOGY_NB_IOT_NTN,
        NT_RADIO_TECHNOLOGY_NR_NTN,
        NT_RADIO_TECHNOLOGY_EMTC_NTN,
        NT_RADIO_TECHNOLOGY_PROPRIETARY
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface NTRadioTechnology {}

  /** Satellite modem is in idle state. */
  public static final int SATELLITE_MODEM_STATE_IDLE = 0;
  /** Satellite modem is listening for incoming datagrams. */
  public static final int SATELLITE_MODEM_STATE_LISTENING = 1;
  /** Satellite modem is sending and/or receiving datagrams. */
  public static final int SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING = 2;
  /** Satellite modem is retrying to send and/or receive datagrams. */
  public static final int SATELLITE_MODEM_STATE_DATAGRAM_RETRYING = 3;
  /** Satellite modem is powered off. */
  public static final int SATELLITE_MODEM_STATE_OFF = 4;
  /** Satellite modem is unavailable. */
  public static final int SATELLITE_MODEM_STATE_UNAVAILABLE = 5;
  /**
   * Satellite modem state is unknown. This generic modem state should be used only when the modem
   * state cannot be mapped to other specific modem states.
   */
  public static final int SATELLITE_MODEM_STATE_UNKNOWN = -1;

  /** @hide */
  @IntDef(
      prefix = {"SATELLITE_MODEM_STATE_"},
      value = {
        SATELLITE_MODEM_STATE_IDLE,
        SATELLITE_MODEM_STATE_LISTENING,
        SATELLITE_MODEM_STATE_DATAGRAM_TRANSFERRING,
        SATELLITE_MODEM_STATE_DATAGRAM_RETRYING,
        SATELLITE_MODEM_STATE_OFF,
        SATELLITE_MODEM_STATE_UNAVAILABLE,
        SATELLITE_MODEM_STATE_UNKNOWN
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface SatelliteModemState {}

  /**
   * The default state indicating that datagram transfer is idle. This should be sent if there are
   * no message transfer activity happening.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE = 0;
  /** A transition state indicating that a datagram is being sent. */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING = 1;
  /**
   * An end state indicating that datagram sending completed successfully. After datagram transfer
   * completes, {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE} will be sent if no more messages are
   * pending.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_SUCCESS = 2;
  /**
   * An end state indicating that datagram sending completed with a failure. After datagram transfer
   * completes, {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE} must be sent before reporting any
   * additional datagram transfer state changes. All pending messages will be reported as failed, to
   * the corresponding applications.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED = 3;
  /** A transition state indicating that a datagram is being received. */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING = 4;
  /**
   * An end state indicating that datagram receiving completed successfully. After datagram transfer
   * completes, {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE} will be sent if no more messages are
   * pending.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_SUCCESS = 5;
  /**
   * An end state indicating that datagram receive operation found that there are no messages to be
   * retrieved from the satellite. After datagram transfer completes, {@link
   * #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE} will be sent if no more messages are pending.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_NONE = 6;
  /**
   * An end state indicating that datagram receive completed with a failure. After datagram transfer
   * completes, {@link #SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE} will be sent if no more messages are
   * pending.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_FAILED = 7;
  /**
   * The datagram transfer state is unknown. This generic datagram transfer state should be used
   * only when the datagram transfer state cannot be mapped to other specific datagram transfer
   * states.
   */
  public static final int SATELLITE_DATAGRAM_TRANSFER_STATE_UNKNOWN = -1;

  /** @hide */
  @IntDef(
      prefix = {"SATELLITE_DATAGRAM_TRANSFER_STATE_"},
      value = {
        SATELLITE_DATAGRAM_TRANSFER_STATE_IDLE,
        SATELLITE_DATAGRAM_TRANSFER_STATE_SENDING,
        SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_SUCCESS,
        SATELLITE_DATAGRAM_TRANSFER_STATE_SEND_FAILED,
        SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVING,
        SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_SUCCESS,
        SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_NONE,
        SATELLITE_DATAGRAM_TRANSFER_STATE_RECEIVE_FAILED,
        SATELLITE_DATAGRAM_TRANSFER_STATE_UNKNOWN
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface SatelliteDatagramTransferState {}

  /** The request was successfully processed. */
  public static final int SATELLITE_ERROR_NONE = 0;
  /** A generic error which should be used only when other specific errors cannot be used. */
  public static final int SATELLITE_ERROR = 1;
  /** Error received from the satellite server. */
  public static final int SATELLITE_SERVER_ERROR = 2;
  /**
   * Error received from the vendor service. This generic error code should be used only when the
   * error cannot be mapped to other specific service error codes.
   */
  public static final int SATELLITE_SERVICE_ERROR = 3;
  /**
   * Error received from satellite modem. This generic error code should be used only when the error
   * cannot be mapped to other specific modem error codes.
   */
  public static final int SATELLITE_MODEM_ERROR = 4;
  /**
   * Error received from the satellite network. This generic error code should be used only when the
   * error cannot be mapped to other specific network error codes.
   */
  public static final int SATELLITE_NETWORK_ERROR = 5;
  /** Telephony is not in a valid state to receive requests from clients. */
  public static final int SATELLITE_INVALID_TELEPHONY_STATE = 6;
  /** Satellite modem is not in a valid state to receive requests from clients. */
  public static final int SATELLITE_INVALID_MODEM_STATE = 7;
  /**
   * Either vendor service, or modem, or Telephony framework has received a request with invalid
   * arguments from its clients.
   */
  public static final int SATELLITE_INVALID_ARGUMENTS = 8;
  /**
   * Telephony framework failed to send a request or receive a response from the vendor service or
   * satellite modem due to internal error.
   */
  public static final int SATELLITE_REQUEST_FAILED = 9;
  /** Radio did not start or is resetting. */
  public static final int SATELLITE_RADIO_NOT_AVAILABLE = 10;
  /** The request is not supported by either the satellite modem or the network. */
  public static final int SATELLITE_REQUEST_NOT_SUPPORTED = 11;
  /** Satellite modem or network has no resources available to handle requests from clients. */
  public static final int SATELLITE_NO_RESOURCES = 12;
  /** Satellite service is not provisioned yet. */
  public static final int SATELLITE_SERVICE_NOT_PROVISIONED = 13;
  /** Satellite service provision is already in progress. */
  public static final int SATELLITE_SERVICE_PROVISION_IN_PROGRESS = 14;
  /**
   * The ongoing request was aborted by either the satellite modem or the network. This error is
   * also returned when framework decides to abort current send request as one of the previous send
   * request failed.
   */
  public static final int SATELLITE_REQUEST_ABORTED = 15;
  /** The device/subscriber is barred from accessing the satellite service. */
  public static final int SATELLITE_ACCESS_BARRED = 16;
  /**
   * Satellite modem timeout to receive ACK or response from the satellite network after sending a
   * request to the network.
   */
  public static final int SATELLITE_NETWORK_TIMEOUT = 17;
  /** Satellite network is not reachable from the modem. */
  public static final int SATELLITE_NOT_REACHABLE = 18;
  /** The device/subscriber is not authorized to register with the satellite service provider. */
  public static final int SATELLITE_NOT_AUTHORIZED = 19;
  /** The device does not support satellite. */
  public static final int SATELLITE_NOT_SUPPORTED = 20;
  /** The current request is already in-progress. */
  public static final int SATELLITE_REQUEST_IN_PROGRESS = 21;
  /** Satellite modem is currently busy due to which current request cannot be processed. */
  public static final int SATELLITE_MODEM_BUSY = 22;

  /** @hide */
  @IntDef(
      prefix = {"SATELLITE_"},
      value = {
        SATELLITE_ERROR_NONE,
        SATELLITE_ERROR,
        SATELLITE_SERVER_ERROR,
        SATELLITE_SERVICE_ERROR,
        SATELLITE_MODEM_ERROR,
        SATELLITE_NETWORK_ERROR,
        SATELLITE_INVALID_TELEPHONY_STATE,
        SATELLITE_INVALID_MODEM_STATE,
        SATELLITE_INVALID_ARGUMENTS,
        SATELLITE_REQUEST_FAILED,
        SATELLITE_RADIO_NOT_AVAILABLE,
        SATELLITE_REQUEST_NOT_SUPPORTED,
        SATELLITE_NO_RESOURCES,
        SATELLITE_SERVICE_NOT_PROVISIONED,
        SATELLITE_SERVICE_PROVISION_IN_PROGRESS,
        SATELLITE_REQUEST_ABORTED,
        SATELLITE_ACCESS_BARRED,
        SATELLITE_NETWORK_TIMEOUT,
        SATELLITE_NOT_REACHABLE,
        SATELLITE_NOT_AUTHORIZED,
        SATELLITE_NOT_SUPPORTED,
        SATELLITE_REQUEST_IN_PROGRESS,
        SATELLITE_MODEM_BUSY
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface SatelliteError {}

  /** Suggested device hold position is unknown. */
  public static final int DEVICE_HOLD_POSITION_UNKNOWN = 0;
  /** User is suggested to hold the device in portrait mode. */
  public static final int DEVICE_HOLD_POSITION_PORTRAIT = 1;
  /** User is suggested to hold the device in landscape mode with left hand. */
  public static final int DEVICE_HOLD_POSITION_LANDSCAPE_LEFT = 2;
  /** User is suggested to hold the device in landscape mode with right hand. */
  public static final int DEVICE_HOLD_POSITION_LANDSCAPE_RIGHT = 3;

  /** @hide */
  @IntDef(
      prefix = {"DEVICE_HOLD_POSITION_"},
      value = {
        DEVICE_HOLD_POSITION_UNKNOWN,
        DEVICE_HOLD_POSITION_PORTRAIT,
        DEVICE_HOLD_POSITION_LANDSCAPE_LEFT,
        DEVICE_HOLD_POSITION_LANDSCAPE_RIGHT
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface DeviceHoldPosition {}

  /** Exception from the satellite service containing the {@link SatelliteError} error code. */
  public static class SatelliteExceptionWrapper extends Exception {
    private final int mErrorCode;

    /** Create a SatelliteException with a given error code. */
    public SatelliteExceptionWrapper(int errorCode) {
      mErrorCode = errorCode;
    }

    /** Get the error code returned from the satellite service. */
    public int getErrorCode() {
      return mErrorCode;
    }
  }

  /**
   * Request to enable or disable the satellite modem and demo mode. If the satellite modem is
   * enabled, this may also disable the cellular modem, and if the satellite modem is disabled, this
   * may also re-enable the cellular modem.
   */
  public void requestSatelliteEnabled(
      boolean enableSatellite,
      boolean enableDemoMode,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteError @NonNull Consumer<Integer> resultListener) {
    mSatelliteManager.requestSatelliteEnabled(
        enableSatellite, enableDemoMode, executor, resultListener);
  }

  /** Request to get whether the satellite modem is enabled. */
  public void requestIsSatelliteEnabled(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    OutcomeReceiver internalCallback =
        new OutcomeReceiver<Boolean, SatelliteException>() {
          @Override
          public void onResult(Boolean result) {
            callback.onResult(result);
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestIsSatelliteEnabled(executor, internalCallback);
  }

  /** Request to get whether the satellite service demo mode is enabled. */
  public void requestIsDemoModeEnabled(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    OutcomeReceiver internalCallback =
        new OutcomeReceiver<Boolean, SatelliteException>() {
          @Override
          public void onResult(Boolean result) {
            callback.onResult(result);
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestIsDemoModeEnabled(executor, internalCallback);
  }

  /** Request to get whether the satellite service is supported on the device. */
  public void requestIsSatelliteSupported(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    OutcomeReceiver internalCallback =
        new OutcomeReceiver<Boolean, SatelliteException>() {
          @Override
          public void onResult(Boolean result) {
            callback.onResult(result);
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestIsSatelliteSupported(executor, internalCallback);
  }

  /** Request to get the {@link SatelliteCapabilities} of the satellite service. */
  public void requestSatelliteCapabilities(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<SatelliteCapabilitiesWrapper, SatelliteExceptionWrapper> callback) {
    OutcomeReceiver internalCallback =
        new OutcomeReceiver<SatelliteCapabilities, SatelliteException>() {
          @Override
          public void onResult(SatelliteCapabilities result) {
            callback.onResult(
                new SatelliteCapabilitiesWrapper(
                    result.getSupportedRadioTechnologies(),
                    result.isPointingRequired(),
                    result.getMaxBytesPerOutgoingDatagram(),
                    transformToAntennaPositionWrapperMap(result.getAntennaPositionMap())));
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestSatelliteCapabilities(executor, internalCallback);
  }

  /**
   * Start receiving satellite transmission updates. This can be called by the pointing UI when the
   * user starts pointing to the satellite. Modem should continue to report the pointing input as
   * the device or satellite moves. Satellite transmission updates are started only on {@link
   * #SATELLITE_ERROR_NONE}. All other results indicate that this operation failed. Once satellite
   * transmission updates begin, position and datagram transfer state updates will be sent through
   * {@link SatelliteTransmissionUpdateCallback}.
   */
  public void startSatelliteTransmissionUpdates(
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteError @NonNull Consumer<Integer> resultListener,
      @NonNull SatelliteTransmissionUpdateCallbackWrapper callback) {

    SatelliteTransmissionUpdateCallback internalCallback =
        new SatelliteTransmissionUpdateCallback() {

          @Override
          public void onSendDatagramStateChanged(
              @SatelliteDatagramTransferState int state,
              int sendPendingCount,
              @SatelliteError int errorCode) {
            callback.onSendDatagramStateChanged(state, sendPendingCount, errorCode);
          }

          @Override
          public void onReceiveDatagramStateChanged(
              @SatelliteDatagramTransferState int state,
              int receivePendingCount,
              @SatelliteError int errorCode) {
            callback.onReceiveDatagramStateChanged(state, receivePendingCount, errorCode);
          }

          @Override
          public void onSatellitePositionChanged(@NonNull PointingInfo pointingInfo) {
            callback.onSatellitePositionChanged(
                new PointingInfoWrapper(
                    pointingInfo.getSatelliteAzimuthDegrees(),
                    pointingInfo.getSatelliteElevationDegrees()));
          }
        };
    sSatelliteTransmissionUpdateCallbackWrapperMap.put(callback, internalCallback);

    mSatelliteManager.startSatelliteTransmissionUpdates(executor, resultListener, internalCallback);
  }

  /**
   * Stop receiving satellite transmission updates. This can be called by the pointing UI when the
   * user stops pointing to the satellite. Satellite transmission updates are stopped and the
   * callback is unregistered only on {@link #SATELLITE_ERROR_NONE}. All other results that this
   * operation failed.
   */
  public void stopSatelliteTransmissionUpdates(
      @NonNull SatelliteTransmissionUpdateCallbackWrapper callback,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteError @NonNull Consumer<Integer> resultListener) {
    SatelliteTransmissionUpdateCallback internalCallback =
        sSatelliteTransmissionUpdateCallbackWrapperMap.get(callback);
    if (internalCallback != null) {
      mSatelliteManager.stopSatelliteTransmissionUpdates(
          internalCallback, executor, resultListener);
    }
  }

  /**
   * Provision the device with a satellite provider. This is needed if the provider allows dynamic
   * registration.
   */
  public void provisionSatelliteService(
      @NonNull String token,
      @NonNull byte[] provisionData,
      @Nullable CancellationSignal cancellationSignal,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteError @NonNull Consumer<Integer> resultListener) {
    mSatelliteManager.provisionSatelliteService(
        token, provisionData, cancellationSignal, executor, resultListener);
  }

  /**
   * Deprovision the device with the satellite provider. This is needed if the provider allows
   * dynamic registration. Once deprovisioned, {@link
   * SatelliteProvisionStateCallback#onSatelliteProvisionStateChanged(boolean)} should report as
   * deprovisioned.
   */
  public void deprovisionSatelliteService(
      @NonNull String token,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteError @NonNull Consumer<Integer> resultListener) {
    mSatelliteManager.deprovisionSatelliteService(token, executor, resultListener);
  }

  /** Registers for the satellite provision state changed. */
  @SatelliteError
  public int registerForSatelliteProvisionStateChanged(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull SatelliteProvisionStateCallbackWrapper callback) {
    SatelliteProvisionStateCallback internalCallback =
        new SatelliteProvisionStateCallback() {
          @Override
          public void onSatelliteProvisionStateChanged(boolean provisioned) {
            callback.onSatelliteProvisionStateChanged(provisioned);
          }
        };
    sSatelliteProvisionStateCallbackWrapperMap.put(callback, internalCallback);
    int result =
        mSatelliteManager.registerForSatelliteProvisionStateChanged(executor, internalCallback);
    return result;
  }

  /**
   * Unregisters for the satellite provision state changed. If callback was not registered before,
   * the request will be ignored.
   */
  public void unregisterForSatelliteProvisionStateChanged(
      @NonNull SatelliteProvisionStateCallbackWrapper callback) {
    SatelliteProvisionStateCallback internalCallback =
        sSatelliteProvisionStateCallbackWrapperMap.get(callback);
    if (internalCallback != null) {
      mSatelliteManager.unregisterForSatelliteProvisionStateChanged(internalCallback);
    }
  }

  /** Request to get whether this device is provisioned with a satellite provider. */
  public void requestIsSatelliteProvisioned(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    OutcomeReceiver internalCallback =
        new OutcomeReceiver<Boolean, SatelliteException>() {
          @Override
          public void onResult(Boolean result) {
            callback.onResult(result);
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestIsSatelliteProvisioned(executor, internalCallback);
  }

  /** Registers for modem state changed from satellite modem. */
  @SatelliteError
  public int registerForSatelliteModemStateChanged(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull SatelliteStateCallbackWrapper callback) {
    SatelliteStateCallback internalCallback =
        new SatelliteStateCallback() {
          public void onSatelliteModemStateChanged(@SatelliteModemState int state) {
            callback.onSatelliteModemStateChanged(state);
          }
        };
    sSatelliteStateCallbackWrapperMap.put(callback, internalCallback);

    int result =
        mSatelliteManager.registerForSatelliteModemStateChanged(executor, internalCallback);
    return result;
  }

  /**
   * Unregisters for modem state changed from satellite modem. If callback was not registered
   * before, the request will be ignored.
   */
  public void unregisterForSatelliteModemStateChanged(
      @NonNull SatelliteStateCallbackWrapper callback) {
    SatelliteStateCallback internalCallback = sSatelliteStateCallbackWrapperMap.get(callback);
    if (internalCallback != null) {
      mSatelliteManager.unregisterForSatelliteModemStateChanged(internalCallback);
    }
  }

  /** Register to receive incoming datagrams over satellite. */
  @SatelliteError
  public int registerForSatelliteDatagram(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull SatelliteDatagramCallbackWrapper callback) {
    SatelliteDatagramCallback internalCallback =
        new SatelliteDatagramCallback() {
          @Override
          public void onSatelliteDatagramReceived(
              long datagramId,
              @NonNull SatelliteDatagram datagram,
              int pendingCount,
              @NonNull Consumer<Void> internalCallback) {
            callback.onSatelliteDatagramReceived(
                datagramId,
                new SatelliteDatagramWrapper(datagram.getSatelliteDatagram()),
                pendingCount,
                internalCallback);
          }
        };
    sSatelliteDatagramCallbackWrapperMap.put(callback, internalCallback);
    int result = mSatelliteManager.registerForSatelliteDatagram(executor, internalCallback);
    return result;
  }

  /**
   * Unregister to stop receiving incoming datagrams over satellite. If callback was not registered
   * before, the request will be ignored.
   */
  public void unregisterForSatelliteDatagram(@NonNull SatelliteDatagramCallbackWrapper callback) {
    SatelliteDatagramCallback internalCallback = sSatelliteDatagramCallbackWrapperMap.get(callback);
    if (internalCallback != null) {
      mSatelliteManager.unregisterForSatelliteDatagram(internalCallback);
    }
  }

  /** Poll pending satellite datagrams over satellite. */
  public void pollPendingSatelliteDatagrams(
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteError @NonNull Consumer<Integer> resultListener) {
    mSatelliteManager.pollPendingSatelliteDatagrams(executor, resultListener);
  }

  /**
   * Send datagram over satellite.
   *
   * <p>Gateway encodes SOS message or location sharing message into a datagram and passes it as
   * input to this method. Datagram received here will be passed down to modem without any encoding
   * or encryption.
   */
  public void sendSatelliteDatagram(
      @DatagramType int datagramType,
      @NonNull SatelliteDatagramWrapper datagram,
      boolean needFullScreenPointingUI,
      @NonNull @CallbackExecutor Executor executor,
      @SatelliteError @NonNull Consumer<Integer> resultListener) {
    SatelliteDatagram datagramInternal = new SatelliteDatagram(datagram.getSatelliteDatagram());
    mSatelliteManager.sendSatelliteDatagram(
        datagramType, datagramInternal, needFullScreenPointingUI, executor, resultListener);
  }

  /** Request to get whether satellite communication is allowed for the current location. */
  public void requestIsSatelliteCommunicationAllowedForCurrentLocation(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Boolean, SatelliteExceptionWrapper> callback) {
    OutcomeReceiver internalCallback =
        new OutcomeReceiver<Boolean, SatelliteException>() {
          @Override
          public void onResult(Boolean result) {
            callback.onResult(result);
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestIsSatelliteCommunicationAllowedForCurrentLocation(
        executor, internalCallback);
  }

  /**
   * Request to get the duration in seconds after which the satellite will be visible. This will be
   * {@link Duration#ZERO} if the satellite is currently visible.
   */
  public void requestTimeForNextSatelliteVisibility(
      @NonNull @CallbackExecutor Executor executor,
      @NonNull OutcomeReceiver<Duration, SatelliteExceptionWrapper> callback) {
    OutcomeReceiver internalCallback =
        new OutcomeReceiver<Duration, SatelliteException>() {
          @Override
          public void onResult(Duration result) {
            callback.onResult(result);
          }

          @Override
          public void onError(SatelliteException exception) {
            callback.onError(new SatelliteExceptionWrapper(exception.getErrorCode()));
          }
        };
    mSatelliteManager.requestTimeForNextSatelliteVisibility(executor, internalCallback);
  }

  /**
   * Inform whether the device is aligned with the satellite for demo mode.
   */
  public void onDeviceAlignedWithSatellite(boolean isAligned) {
    mSatelliteManager.onDeviceAlignedWithSatellite(isAligned);
  }

  private Map<Integer, AntennaPositionWrapper> transformToAntennaPositionWrapperMap(
      Map<Integer, AntennaPosition> input) {
    Map<Integer, AntennaPositionWrapper> output = new HashMap<>();
    for (Map.Entry<Integer, AntennaPosition> entry : input.entrySet()) {
      AntennaPosition position = entry.getValue();

      output.put(
          entry.getKey(),
          new AntennaPositionWrapper(
              new AntennaDirectionWrapper(
                  position.getAntennaDirection().getX(),
                  position.getAntennaDirection().getY(),
                  position.getAntennaDirection().getZ()),
              position.getSuggestedHoldPosition()));
    }

    return output;
  }
}
