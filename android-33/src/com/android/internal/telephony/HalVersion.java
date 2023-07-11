/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.internal.telephony;

import java.util.Objects;

/**
 * Represent the version of underlying vendor HAL service.
 * @see <a href="https://source.android.com/devices/architecture/hidl/versioning">
 * HIDL versioning</a>.
 */
public class HalVersion implements Comparable<HalVersion> {

    /** The HAL Version indicating that the version is unknown or invalid */
    public static final HalVersion UNKNOWN = new HalVersion(-1, -1);

    public final int major;

    public final int minor;

    public HalVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    @Override
    public int compareTo(HalVersion ver) {
        if (ver == null) {
            return 1;
        }
        if (this.major > ver.major) {
            return 1;
        } else if (this.major < ver.major) {
            return -1;
        } else if (this.minor > ver.minor) {
            return 1;
        } else if (this.minor < ver.minor) {
            return -1;
        }
        return 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor);
    }

    @Override
    public boolean equals(Object o) {
        return ((o instanceof HalVersion) && (o == this || compareTo((HalVersion) o) == 0));
    }

    /**
     * @return True if the version is greater than the compared version.
     */
    public boolean greater(HalVersion ver) {
        return compareTo(ver) > 0;
    }

    /**
     * @return True if the version is less than the compared version.
     */
    public boolean less(HalVersion ver) {
        return compareTo(ver) < 0;
    }

    /**
     * @return True if the version is greater than or equal to the compared version.
     */
    public boolean greaterOrEqual(HalVersion ver) {
        return greater(ver) || equals(ver);
    }

    /**
     * @return True if the version is less than or equal to the compared version.
     */
    public boolean lessOrEqual(HalVersion ver) {
        return less(ver) || equals(ver);
    }

    @Override
    public String toString() {
        return major + "." + minor;
    }
}
