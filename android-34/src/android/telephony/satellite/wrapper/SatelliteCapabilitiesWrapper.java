/*
 * Copyright (C) 2022 The Android Open Source Project
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
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Encapsulates the capabilities of Satellite */
public final class SatelliteCapabilitiesWrapper {
  /** List of technologies supported by the satellite modem. */
  @NonNull @SatelliteManagerWrapper.NTRadioTechnology
  private final Set<Integer> mSupportedRadioTechnologies;

  /** Whether UE needs to point to a satellite to send and receive data. */
  private final boolean mIsPointingRequired;

  /** The maximum number of bytes per datagram that can be sent over satellite. */
  private final int mMaxBytesPerOutgoingDatagram;

  /**
   * Antenna Position received from satellite modem which gives information about antenna direction
   * to be used with satellite communication and suggested device hold positions. Map key: {@link
   * SatelliteManager.DeviceHoldPosition} value: AntennaPosition
   */
  @NonNull private final Map<Integer, AntennaPositionWrapper> mAntennaPositionMap;

  public SatelliteCapabilitiesWrapper(
      Set<Integer> supportedRadioTechnologies,
      boolean isPointingRequired,
      int maxBytesPerOutgoingDatagram,
      @NonNull Map<Integer, AntennaPositionWrapper> antennaPositionMap) {
    this.mSupportedRadioTechnologies =
        supportedRadioTechnologies == null ? new HashSet<>() : supportedRadioTechnologies;
    this.mIsPointingRequired = isPointingRequired;
    this.mMaxBytesPerOutgoingDatagram = maxBytesPerOutgoingDatagram;
    this.mAntennaPositionMap = antennaPositionMap;
  }

  /**
   * @return The list of technologies supported by the satellite modem.
   */
  @NonNull
  @SatelliteManagerWrapper.NTRadioTechnology
  public Set<Integer> getSupportedRadioTechnologies() {
    return mSupportedRadioTechnologies;
  }

  /**
   * Get whether UE needs to point to a satellite to send and receive data.
   *
   * @return {@code true} if UE needs to point to a satellite to send and receive data and {@code
   *     false} otherwise.
   */
  public boolean isPointingRequired() {
    return mIsPointingRequired;
  }

  /**
   * The maximum number of bytes per datagram that can be sent over satellite.
   *
   * @return The maximum number of bytes per datagram that can be sent over satellite.
   */
  public int getMaxBytesPerOutgoingDatagram() {
    return mMaxBytesPerOutgoingDatagram;
  }

  /**
   * Antenna Position received from satellite modem which gives information about antenna direction
   * to be used with satellite communication and suggested device hold positions.
   *
   * @return Map key: {@link SatelliteManager.DeviceHoldPosition} value: AntennaPosition
   */
  @NonNull
  public Map<Integer, AntennaPositionWrapper> getAntennaPositionMap() {
    return mAntennaPositionMap;
  }

  @Override
  @NonNull
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("SupportedRadioTechnology:");
    if (mSupportedRadioTechnologies != null && !mSupportedRadioTechnologies.isEmpty()) {
      for (int technology : mSupportedRadioTechnologies) {
        sb.append(technology);
        sb.append(",");
      }
    } else {
      sb.append("none,");
    }

    sb.append("isPointingRequired:");
    sb.append(mIsPointingRequired);
    sb.append(",");

    sb.append("maxBytesPerOutgoingDatagram:");
    sb.append(mMaxBytesPerOutgoingDatagram);
    sb.append(",");

    sb.append("antennaPositionMap:");
    sb.append(mAntennaPositionMap);
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SatelliteCapabilitiesWrapper that = (SatelliteCapabilitiesWrapper) o;
    return Objects.equals(mSupportedRadioTechnologies, that.mSupportedRadioTechnologies)
        && mIsPointingRequired == that.mIsPointingRequired
        && mMaxBytesPerOutgoingDatagram == that.mMaxBytesPerOutgoingDatagram
        && Objects.equals(mAntennaPositionMap, that.mAntennaPositionMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        mSupportedRadioTechnologies,
        mIsPointingRequired,
        mMaxBytesPerOutgoingDatagram,
        mAntennaPositionMap);
  }
}
