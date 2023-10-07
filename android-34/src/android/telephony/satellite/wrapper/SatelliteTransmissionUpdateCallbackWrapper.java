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

import android.annotation.NonNull;

/**
 * A callback class for monitoring satellite position update and datagram transfer state change
 * events.
 */
public interface SatelliteTransmissionUpdateCallbackWrapper {
  /**
   * Called when the satellite position changed.
   *
   * @param pointingInfo The pointing info containing the satellite location.
   */
  void onSatellitePositionChanged(@NonNull PointingInfoWrapper pointingInfo);

  /**
   * Called when satellite datagram send state changed.
   *
   * @param state The new send datagram transfer state.
   * @param sendPendingCount The number of datagrams that are currently being sent.
   * @param errorCode If datagram transfer failed, the reason for failure.
   */
  void onSendDatagramStateChanged(
      @SatelliteManagerWrapper.SatelliteDatagramTransferState int state,
      int sendPendingCount,
      @SatelliteManagerWrapper.SatelliteError int errorCode);

  /**
   * Called when satellite datagram receive state changed.
   *
   * @param state The new receive datagram transfer state.
   * @param receivePendingCount The number of datagrams that are currently pending to be received.
   * @param errorCode If datagram transfer failed, the reason for failure.
   */
  void onReceiveDatagramStateChanged(
      @SatelliteManagerWrapper.SatelliteDatagramTransferState int state,
      int receivePendingCount,
      @SatelliteManagerWrapper.SatelliteError int errorCode);
}
