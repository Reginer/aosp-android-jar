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

package com.android.testutils;

import android.text.TextUtils;
import android.util.Pair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for device information.
 */
public class DeviceInfoUtils {
    /**
     * Class for a three-part kernel version number.
     */
    public static class KVersion {
        public final int major;
        public final int minor;
        public final int sub;

        public KVersion(int major, int minor, int sub) {
            this.major = major;
            this.minor = minor;
            this.sub = sub;
        }

        /**
         * Compares with other version numerically.
         *
         * @param  other the other version to compare
         * @return the value 0 if this == other;
         *         a value less than 0 if this < other and
         *         a value greater than 0 if this > other.
         */
        public int compareTo(final KVersion other) {
            int res = Integer.compare(this.major, other.major);
            if (res == 0) {
                res = Integer.compare(this.minor, other.minor);
            }
            if (res == 0) {
                res = Integer.compare(this.sub, other.sub);
            }
            return res;
        }

        /**
         * At least satisfied with the given version.
         *
         * @param  from the start version to compare
         * @return return true if this version is at least satisfied with the given version.
         *         otherwise, return false.
         */
        public boolean isAtLeast(final KVersion from) {
            return compareTo(from) >= 0;
        }

        /**
         * Falls within the given range [from, to).
         *
         * @param  from the start version to compare
         * @param  to   the end version to compare
         * @return return true if this version falls within the given range.
         *         otherwise, return false.
         */
        public boolean isInRange(final KVersion from, final KVersion to) {
            return isAtLeast(from) && !isAtLeast(to);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof KVersion)) return false;
            KVersion that = (KVersion) o;
            return this.major == that.major
                    && this.minor == that.minor
                    && this.sub == that.sub;
        }
    };

    /**
     * Get a two-part kernel version number (major and minor) from a given string.
     *
     * TODO: use class KVersion.
     */
    private static Pair<Integer, Integer> getMajorMinorVersion(String version) {
        // Only gets major and minor number of the version string.
        final Pattern versionPattern = Pattern.compile("^(\\d+)(\\.(\\d+))?.*");
        final Matcher m = versionPattern.matcher(version);
        if (m.matches()) {
            final int major = Integer.parseInt(m.group(1));
            final int minor = TextUtils.isEmpty(m.group(3)) ? 0 : Integer.parseInt(m.group(3));
            return new Pair<>(major, minor);
        } else {
            return new Pair<>(0, 0);
        }
    }

    /**
     * Compares two version strings numerically. Compare only major and minor number of the
     * version string. The version comparison uses #Integer.compare. Possible version
     * 5, 5.10, 5-beta1, 4.8-RC1, 4.7.10.10 and so on.
     *
     * @param  s1 the first version string to compare
     * @param  s2 the second version string to compare
     * @return the value 0 if s1 == s2;
     *         a value less than 0 if s1 < s2 and
     *         a value greater than 0 if s1 > s2.
     *
     * TODO: use class KVersion.
     */
    public static int compareMajorMinorVersion(final String s1, final String s2) {
        final Pair<Integer, Integer> v1 = getMajorMinorVersion(s1);
        final Pair<Integer, Integer> v2 = getMajorMinorVersion(s2);

        if (v1.first == v2.first) {
            return Integer.compare(v1.second, v2.second);
        } else {
            return Integer.compare(v1.first, v2.first);
        }
    }

    /**
     * Get a three-part kernel version number (major, minor and subminor) from a given string.
     * Any version string must at least have major and minor number. If the subminor number can't
     * be parsed from string. Assign zero as subminor number. Invalid version is treated as
     * version 0.0.0.
     */
    public static KVersion getMajorMinorSubminorVersion(final String version) {
        // The kernel version is a three-part version number (major, minor and subminor). Get
        // the three-part version numbers and discard the remaining stuff if any.
        // For example:
        //   4.19.220-g500ede0aed22-ab8272303 --> 4.19.220
        //   5.17-rc6-g52099515ca00-ab8032400 --> 5.17.0
        final Pattern versionPattern = Pattern.compile("^(\\d+)\\.(\\d+)(\\.(\\d+))?.*");
        final Matcher m = versionPattern.matcher(version);
        if (m.matches()) {
            final int major = Integer.parseInt(m.group(1));
            final int minor = Integer.parseInt(m.group(2));
            final int sub = TextUtils.isEmpty(m.group(4)) ? 0 : Integer.parseInt(m.group(4));
            return new KVersion(major, minor, sub);
        } else {
            return new KVersion(0, 0, 0);
        }
    }
}
