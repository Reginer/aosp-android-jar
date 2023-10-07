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
 * Antenna direction is provided as X/Y/Z values corresponding to the direction of the antenna main
 * lobe as a unit vector in CTIA coordinate system (as specified in Appendix A of Wireless device
 * CTIA OTAn test plan). CTIA coordinate system is defined relative to device’s screen when the
 * device is held in default portrait mode with screen facing the user:
 *
 * <p>Z axis is vertical along the plane of the device with positive Z pointing up and negative z
 * pointing towards bottom of the device Y axis is horizontal along the plane of the device with
 * positive Y pointing towards right of the phone screen and negative Y pointing towards left X axis
 * is orthogonal to the Y-Z plane (phone screen), pointing away from the phone screen for positive X
 * and pointing away from back of the phone for negative X.
 */
public final class AntennaDirectionWrapper {
  /** Antenna x axis direction. */
  private float mX;

  /** Antenna y axis direction. */
  private float mY;

  /** Antenna z axis direction. */
  private float mZ;

  public AntennaDirectionWrapper(float x, float y, float z) {
    mX = x;
    mY = y;
    mZ = z;
  }

  @Override
  @NonNull
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("X:");
    sb.append(mX);
    sb.append(",");

    sb.append("Y:");
    sb.append(mY);
    sb.append(",");

    sb.append("Z:");
    sb.append(mZ);
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AntennaDirectionWrapper that = (AntennaDirectionWrapper) o;
    return mX == that.mX && mY == that.mY && mZ == that.mZ;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mX, mY, mZ);
  }

  public float getX() {
    return mX;
  }

  public float getY() {
    return mY;
  }

  public float getZ() {
    return mZ;
  }
}
