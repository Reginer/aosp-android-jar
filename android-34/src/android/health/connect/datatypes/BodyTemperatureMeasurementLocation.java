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

package android.health.connect.datatypes;

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

/** Where on the user's body a temperature measurement was taken from. */
public final class BodyTemperatureMeasurementLocation {
    /** Body measurement location unknown */
    public static final int MEASUREMENT_LOCATION_UNKNOWN = 0;
    /** Armpit (axillary) body temperature measurement. */
    public static final int MEASUREMENT_LOCATION_ARMPIT = 1;
    /** Finger body temperature measurement. */
    public static final int MEASUREMENT_LOCATION_FINGER = 2;
    /** Forehead body temperature measurement. */
    public static final int MEASUREMENT_LOCATION_FOREHEAD = 3;
    /** Mouth body temperature measurement. */
    public static final int MEASUREMENT_LOCATION_MOUTH = 4;
    /** Rectum body temperature measurement. */
    public static final int MEASUREMENT_LOCATION_RECTUM = 5;
    /** Temporal artery temperature measurement. */
    public static final int MEASUREMENT_LOCATION_TEMPORAL_ARTERY = 6;
    /** Toe body temperature measurement. */
    public static final int MEASUREMENT_LOCATION_TOE = 7;
    /** Ear (tympanic) body temperature measurement. */
    public static final int MEASUREMENT_LOCATION_EAR = 8;
    /** Wrist body temperature measurement. */
    public static final int MEASUREMENT_LOCATION_WRIST = 9;
    /** Vaginal body temperature measurement. */
    public static final int MEASUREMENT_LOCATION_VAGINA = 10;

    /**
     * Valid set of values for this IntDef. Update this set when add new type or deprecate existing
     * type.
     *
     * @hide
     */
    public static final Set<Integer> VALID_TYPES =
            Set.of(
                    MEASUREMENT_LOCATION_UNKNOWN,
                    MEASUREMENT_LOCATION_ARMPIT,
                    MEASUREMENT_LOCATION_FINGER,
                    MEASUREMENT_LOCATION_FOREHEAD,
                    MEASUREMENT_LOCATION_MOUTH,
                    MEASUREMENT_LOCATION_RECTUM,
                    MEASUREMENT_LOCATION_TEMPORAL_ARTERY,
                    MEASUREMENT_LOCATION_TOE,
                    MEASUREMENT_LOCATION_EAR,
                    MEASUREMENT_LOCATION_WRIST,
                    MEASUREMENT_LOCATION_VAGINA);

    private BodyTemperatureMeasurementLocation() {}

    /** @hide */
    @IntDef({
        MEASUREMENT_LOCATION_UNKNOWN,
        MEASUREMENT_LOCATION_ARMPIT,
        MEASUREMENT_LOCATION_FINGER,
        MEASUREMENT_LOCATION_FOREHEAD,
        MEASUREMENT_LOCATION_MOUTH,
        MEASUREMENT_LOCATION_RECTUM,
        MEASUREMENT_LOCATION_TEMPORAL_ARTERY,
        MEASUREMENT_LOCATION_TOE,
        MEASUREMENT_LOCATION_EAR,
        MEASUREMENT_LOCATION_WRIST,
        MEASUREMENT_LOCATION_VAGINA
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface BodyTemperatureMeasurementLocations {}
}
