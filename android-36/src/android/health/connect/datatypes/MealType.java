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

/** Identifier for the meal type. */
public final class MealType {
    public static final int MEAL_TYPE_UNKNOWN = 0;

    /** Use this for the first meal of the day, usually the morning meal. */
    public static final int MEAL_TYPE_BREAKFAST = 1;

    /** Use this for the noon meal. */
    public static final int MEAL_TYPE_LUNCH = 2;

    /** Use this for last meal of the day, usually the evening meal. */
    public static final int MEAL_TYPE_DINNER = 3;

    /** Any meal outside of the usual three meals per day. */
    public static final int MEAL_TYPE_SNACK = 4;

    /**
     * Valid set of values for this IntDef. Update this set when add new type or deprecate existing
     * type.
     *
     * @hide
     */
    public static final Set<Integer> VALID_TYPES =
            Set.of(
                    MEAL_TYPE_UNKNOWN,
                    MEAL_TYPE_BREAKFAST,
                    MEAL_TYPE_LUNCH,
                    MEAL_TYPE_DINNER,
                    MEAL_TYPE_SNACK);

    private MealType() {}

    /** @hide */
    @IntDef({
        MEAL_TYPE_UNKNOWN,
        MEAL_TYPE_BREAKFAST,
        MEAL_TYPE_LUNCH,
        MEAL_TYPE_DINNER,
        MEAL_TYPE_SNACK
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MealTypes {}
}
