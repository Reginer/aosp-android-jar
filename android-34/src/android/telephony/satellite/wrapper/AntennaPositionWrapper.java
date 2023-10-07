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
import java.util.Objects;

/**
 * Antenna Position received from satellite modem which gives information about antenna direction to
 * be used with satellite communication and suggested device hold positions.
 */
public final class AntennaPositionWrapper {
  /** Antenna direction used for satellite communication. */
  @NonNull AntennaDirectionWrapper mAntennaDirection;

  /** Enum corresponding to device hold position to be used by the end user. */
  @SatelliteManagerWrapper.DeviceHoldPosition int mSuggestedHoldPosition;

  public AntennaPositionWrapper(
      @NonNull AntennaDirectionWrapper antennaDirection, int suggestedHoldPosition) {
    this.mAntennaDirection = antennaDirection;
    this.mSuggestedHoldPosition = suggestedHoldPosition;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AntennaPositionWrapper that = (AntennaPositionWrapper) o;
    return Objects.equals(mAntennaDirection, that.mAntennaDirection)
        && mSuggestedHoldPosition == that.mSuggestedHoldPosition;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mAntennaDirection, mSuggestedHoldPosition);
  }

  @Override
  @NonNull
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("antennaDirection:");
    sb.append(mAntennaDirection);
    sb.append(",");

    sb.append("suggestedHoldPosition:");
    sb.append(mSuggestedHoldPosition);
    return sb.toString();
  }

  @NonNull
  public AntennaDirectionWrapper getAntennaDirection() {
    return mAntennaDirection;
  }

  @SatelliteManagerWrapper.DeviceHoldPosition
  public int getSuggestedHoldPosition() {
    return mSuggestedHoldPosition;
  }
}
