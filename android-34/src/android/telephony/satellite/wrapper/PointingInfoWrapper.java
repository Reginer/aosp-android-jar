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

/** Encapsulates the Pointing Information. */
public final class PointingInfoWrapper {
  /** Satellite azimuth in degrees */
  private final float mSatelliteAzimuthDegrees;

  /** Satellite elevation in degrees */
  private final float mSatelliteElevationDegrees;

  public PointingInfoWrapper(float satelliteAzimuthDegrees, float satelliteElevationDegrees) {
    this.mSatelliteAzimuthDegrees = satelliteAzimuthDegrees;
    this.mSatelliteElevationDegrees = satelliteElevationDegrees;
  }

  public float getSatelliteAzimuthDegrees() {
    return mSatelliteAzimuthDegrees;
  }

  public float getSatelliteElevationDegrees() {
    return mSatelliteElevationDegrees;
  }

  @NonNull
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("SatelliteAzimuthDegrees:");
    sb.append(mSatelliteAzimuthDegrees);
    sb.append(",");

    sb.append("SatelliteElevationDegrees:");
    sb.append(mSatelliteElevationDegrees);
    return sb.toString();
  }
}
