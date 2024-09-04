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

/**
 * Identifier for exercise types, as returned by {@link ExerciseSessionRecord#getExerciseType()}.
 */
public final class ExerciseSessionType {

    /** Use this type if the type of the exercise session is not known. */
    public static final int EXERCISE_SESSION_TYPE_UNKNOWN = 0;

    /** Use this type for the badminton playing session. */
    public static final int EXERCISE_SESSION_TYPE_BADMINTON = 1;

    /** Use this type for the baseball playing session. */
    public static final int EXERCISE_SESSION_TYPE_BASEBALL = 2;

    /** Use this type for the basketball playing session. */
    public static final int EXERCISE_SESSION_TYPE_BASKETBALL = 3;

    /** Use this type for riding a bicycle session. */
    public static final int EXERCISE_SESSION_TYPE_BIKING = 4;

    /** Use this type for riding a stationary bicycle session. */
    public static final int EXERCISE_SESSION_TYPE_BIKING_STATIONARY = 5;

    /** Use this type for the boot camp session. */
    public static final int EXERCISE_SESSION_TYPE_BOOT_CAMP = 6;

    /** Use this type for boxing session. */
    public static final int EXERCISE_SESSION_TYPE_BOXING = 7;

    /** Use this type for calisthenics session. */
    public static final int EXERCISE_SESSION_TYPE_CALISTHENICS = 8;

    /** Use this type for the cricket playing session. */
    public static final int EXERCISE_SESSION_TYPE_CRICKET = 9;

    /** Use this type for the dancing session. */
    public static final int EXERCISE_SESSION_TYPE_DANCING = 10;

    /** Use this type for the exercise class session. */
    public static final int EXERCISE_SESSION_TYPE_EXERCISE_CLASS = 11;

    /** Use this type for the fencing session. */
    public static final int EXERCISE_SESSION_TYPE_FENCING = 12;

    /** Use this type for the American football playing session. */
    public static final int EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN = 13;

    /** Use this type for the Australian football playing session. */
    public static final int EXERCISE_SESSION_TYPE_FOOTBALL_AUSTRALIAN = 14;

    /** Use this type for the frisbee disc playing session. */
    public static final int EXERCISE_SESSION_TYPE_FRISBEE_DISC = 15;

    /** Use this type for the golf playing session. */
    public static final int EXERCISE_SESSION_TYPE_GOLF = 16;

    /** Use this type for the guided breathing session. */
    public static final int EXERCISE_SESSION_TYPE_GUIDED_BREATHING = 17;

    /** Use this type for the gymnastics session. */
    public static final int EXERCISE_SESSION_TYPE_GYMNASTICS = 18;

    /** Use this type for the handball playing session. */
    public static final int EXERCISE_SESSION_TYPE_HANDBALL = 19;

    /** Use this type for the high intensity interval training session. */
    public static final int EXERCISE_SESSION_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING = 20;

    /** Use this type for the hiking session. */
    public static final int EXERCISE_SESSION_TYPE_HIKING = 21;

    /** Use this type for the ice hockey playing session. */
    public static final int EXERCISE_SESSION_TYPE_ICE_HOCKEY = 22;

    /** Use this type for the ice skating session. */
    public static final int EXERCISE_SESSION_TYPE_ICE_SKATING = 23;

    /** Use this type for the martial arts training session. */
    public static final int EXERCISE_SESSION_TYPE_MARTIAL_ARTS = 24;

    /** Use this type for the paddling session. */
    public static final int EXERCISE_SESSION_TYPE_PADDLING = 25;

    /** Use this type for the paragliding session. */
    public static final int EXERCISE_SESSION_TYPE_PARAGLIDING = 26;

    /** Use this type for the pilates session. */
    public static final int EXERCISE_SESSION_TYPE_PILATES = 27;

    /** Use this type for the racquetball playing session. */
    public static final int EXERCISE_SESSION_TYPE_RACQUETBALL = 28;

    /** Use this type for the rock climbing session. */
    public static final int EXERCISE_SESSION_TYPE_ROCK_CLIMBING = 29;

    /** Use this type for the roller hockey playing session. */
    public static final int EXERCISE_SESSION_TYPE_ROLLER_HOCKEY = 30;

    /** Use this type for the rowing session. */
    public static final int EXERCISE_SESSION_TYPE_ROWING = 31;

    /** Use this type for the rugby playing session. */
    public static final int EXERCISE_SESSION_TYPE_RUGBY = 32;

    /** Use this type for the running session. */
    public static final int EXERCISE_SESSION_TYPE_RUNNING = 33;

    /** Use this type for the treadmill running session. */
    public static final int EXERCISE_SESSION_TYPE_RUNNING_TREADMILL = 34;

    /** Use this type for the sailing session. */
    public static final int EXERCISE_SESSION_TYPE_SAILING = 35;

    /** Use this type for the scuba diving session. */
    public static final int EXERCISE_SESSION_TYPE_SCUBA_DIVING = 36;

    /** Use this type for the skating session. */
    public static final int EXERCISE_SESSION_TYPE_SKATING = 37;

    /** Use this type for the skiing session. */
    public static final int EXERCISE_SESSION_TYPE_SKIING = 38;

    /** Use this type for the snowboarding session. */
    public static final int EXERCISE_SESSION_TYPE_SNOWBOARDING = 39;

    /** Use this type for the snowshoeing session. */
    public static final int EXERCISE_SESSION_TYPE_SNOWSHOEING = 40;

    /** Use this type for the soccer playing session. */
    public static final int EXERCISE_SESSION_TYPE_SOCCER = 41;

    /** Use this type for the softball playing session. */
    public static final int EXERCISE_SESSION_TYPE_SOFTBALL = 42;

    /** Use this type for the squash playing session. */
    public static final int EXERCISE_SESSION_TYPE_SQUASH = 43;

    /** Use this type for the stair climbing session. */
    public static final int EXERCISE_SESSION_TYPE_STAIR_CLIMBING = 44;

    /** Use this type for the strength training session. */
    public static final int EXERCISE_SESSION_TYPE_STRENGTH_TRAINING = 45;

    /** Use this type for the stretching session. */
    public static final int EXERCISE_SESSION_TYPE_STRETCHING = 46;

    /** Use this type for the surfing session. */
    public static final int EXERCISE_SESSION_TYPE_SURFING = 47;

    /** Use this type for the swimming in the open water session. */
    public static final int EXERCISE_SESSION_TYPE_SWIMMING_OPEN_WATER = 48;

    /** Use this type for the swimming in the pool session. */
    public static final int EXERCISE_SESSION_TYPE_SWIMMING_POOL = 49;

    /** Use this type for the table tennis playing session. */
    public static final int EXERCISE_SESSION_TYPE_TABLE_TENNIS = 50;

    /** Use this type for the tennis playing session. */
    public static final int EXERCISE_SESSION_TYPE_TENNIS = 51;

    /** Use this type for the volleyball playing session. */
    public static final int EXERCISE_SESSION_TYPE_VOLLEYBALL = 52;

    /** Use this type for the walking session. */
    public static final int EXERCISE_SESSION_TYPE_WALKING = 53;

    /** Use this type for the water polo playing session. */
    public static final int EXERCISE_SESSION_TYPE_WATER_POLO = 54;

    /** Use this type for the weightlifting session. */
    public static final int EXERCISE_SESSION_TYPE_WEIGHTLIFTING = 55;

    /** Use this type for the wheelchair session. */
    public static final int EXERCISE_SESSION_TYPE_WHEELCHAIR = 56;

    /** Use this type for the yoga session. */
    public static final int EXERCISE_SESSION_TYPE_YOGA = 57;

    /** Use this type for the other workout session. */
    public static final int EXERCISE_SESSION_TYPE_OTHER_WORKOUT = 58;

    /** Use this type for the stair climbing machine session. */
    public static final int EXERCISE_SESSION_TYPE_STAIR_CLIMBING_MACHINE = 59;

    /** Use this type for elliptical workout. */
    public static final int EXERCISE_SESSION_TYPE_ELLIPTICAL = 60;

    /** Use this type for rowing machine. */
    public static final int EXERCISE_SESSION_TYPE_ROWING_MACHINE = 61;

    private ExerciseSessionType() {}

    /** @hide */
    @IntDef({
        EXERCISE_SESSION_TYPE_UNKNOWN,
        EXERCISE_SESSION_TYPE_BADMINTON,
        EXERCISE_SESSION_TYPE_BASEBALL,
        EXERCISE_SESSION_TYPE_BASKETBALL,
        EXERCISE_SESSION_TYPE_BIKING,
        EXERCISE_SESSION_TYPE_BIKING_STATIONARY,
        EXERCISE_SESSION_TYPE_BOOT_CAMP,
        EXERCISE_SESSION_TYPE_BOXING,
        EXERCISE_SESSION_TYPE_CALISTHENICS,
        EXERCISE_SESSION_TYPE_CRICKET,
        EXERCISE_SESSION_TYPE_DANCING,
        EXERCISE_SESSION_TYPE_ELLIPTICAL,
        EXERCISE_SESSION_TYPE_EXERCISE_CLASS,
        EXERCISE_SESSION_TYPE_FENCING,
        EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN,
        EXERCISE_SESSION_TYPE_FOOTBALL_AUSTRALIAN,
        EXERCISE_SESSION_TYPE_FRISBEE_DISC,
        EXERCISE_SESSION_TYPE_GOLF,
        EXERCISE_SESSION_TYPE_GUIDED_BREATHING,
        EXERCISE_SESSION_TYPE_GYMNASTICS,
        EXERCISE_SESSION_TYPE_HANDBALL,
        EXERCISE_SESSION_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
        EXERCISE_SESSION_TYPE_HIKING,
        EXERCISE_SESSION_TYPE_ICE_HOCKEY,
        EXERCISE_SESSION_TYPE_ICE_SKATING,
        EXERCISE_SESSION_TYPE_MARTIAL_ARTS,
        EXERCISE_SESSION_TYPE_PADDLING,
        EXERCISE_SESSION_TYPE_PARAGLIDING,
        EXERCISE_SESSION_TYPE_PILATES,
        EXERCISE_SESSION_TYPE_RACQUETBALL,
        EXERCISE_SESSION_TYPE_ROCK_CLIMBING,
        EXERCISE_SESSION_TYPE_ROLLER_HOCKEY,
        EXERCISE_SESSION_TYPE_ROWING,
        EXERCISE_SESSION_TYPE_ROWING_MACHINE,
        EXERCISE_SESSION_TYPE_RUGBY,
        EXERCISE_SESSION_TYPE_RUNNING,
        EXERCISE_SESSION_TYPE_RUNNING_TREADMILL,
        EXERCISE_SESSION_TYPE_SAILING,
        EXERCISE_SESSION_TYPE_SCUBA_DIVING,
        EXERCISE_SESSION_TYPE_SKATING,
        EXERCISE_SESSION_TYPE_SKIING,
        EXERCISE_SESSION_TYPE_SNOWBOARDING,
        EXERCISE_SESSION_TYPE_SNOWSHOEING,
        EXERCISE_SESSION_TYPE_SOCCER,
        EXERCISE_SESSION_TYPE_SOFTBALL,
        EXERCISE_SESSION_TYPE_SQUASH,
        EXERCISE_SESSION_TYPE_STAIR_CLIMBING,
        EXERCISE_SESSION_TYPE_STAIR_CLIMBING_MACHINE,
        EXERCISE_SESSION_TYPE_STRENGTH_TRAINING,
        EXERCISE_SESSION_TYPE_STRETCHING,
        EXERCISE_SESSION_TYPE_SURFING,
        EXERCISE_SESSION_TYPE_SWIMMING_OPEN_WATER,
        EXERCISE_SESSION_TYPE_SWIMMING_POOL,
        EXERCISE_SESSION_TYPE_TABLE_TENNIS,
        EXERCISE_SESSION_TYPE_TENNIS,
        EXERCISE_SESSION_TYPE_VOLLEYBALL,
        EXERCISE_SESSION_TYPE_WALKING,
        EXERCISE_SESSION_TYPE_WATER_POLO,
        EXERCISE_SESSION_TYPE_WEIGHTLIFTING,
        EXERCISE_SESSION_TYPE_WHEELCHAIR,
        EXERCISE_SESSION_TYPE_OTHER_WORKOUT,
        EXERCISE_SESSION_TYPE_YOGA,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ExerciseSessionTypes {}

    // Update this set when add new type or deprecate existing type.
    private static final Set<Integer> VALID_TYPES =
            Set.of(
                    EXERCISE_SESSION_TYPE_UNKNOWN,
                    EXERCISE_SESSION_TYPE_BADMINTON,
                    EXERCISE_SESSION_TYPE_BASEBALL,
                    EXERCISE_SESSION_TYPE_BASKETBALL,
                    EXERCISE_SESSION_TYPE_BIKING,
                    EXERCISE_SESSION_TYPE_BIKING_STATIONARY,
                    EXERCISE_SESSION_TYPE_BOOT_CAMP,
                    EXERCISE_SESSION_TYPE_BOXING,
                    EXERCISE_SESSION_TYPE_CALISTHENICS,
                    EXERCISE_SESSION_TYPE_CRICKET,
                    EXERCISE_SESSION_TYPE_DANCING,
                    EXERCISE_SESSION_TYPE_ELLIPTICAL,
                    EXERCISE_SESSION_TYPE_EXERCISE_CLASS,
                    EXERCISE_SESSION_TYPE_FENCING,
                    EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN,
                    EXERCISE_SESSION_TYPE_FOOTBALL_AUSTRALIAN,
                    EXERCISE_SESSION_TYPE_FRISBEE_DISC,
                    EXERCISE_SESSION_TYPE_GOLF,
                    EXERCISE_SESSION_TYPE_GUIDED_BREATHING,
                    EXERCISE_SESSION_TYPE_GYMNASTICS,
                    EXERCISE_SESSION_TYPE_HANDBALL,
                    EXERCISE_SESSION_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                    EXERCISE_SESSION_TYPE_HIKING,
                    EXERCISE_SESSION_TYPE_ICE_HOCKEY,
                    EXERCISE_SESSION_TYPE_ICE_SKATING,
                    EXERCISE_SESSION_TYPE_MARTIAL_ARTS,
                    EXERCISE_SESSION_TYPE_PADDLING,
                    EXERCISE_SESSION_TYPE_PARAGLIDING,
                    EXERCISE_SESSION_TYPE_PILATES,
                    EXERCISE_SESSION_TYPE_RACQUETBALL,
                    EXERCISE_SESSION_TYPE_ROCK_CLIMBING,
                    EXERCISE_SESSION_TYPE_ROLLER_HOCKEY,
                    EXERCISE_SESSION_TYPE_ROWING,
                    EXERCISE_SESSION_TYPE_ROWING_MACHINE,
                    EXERCISE_SESSION_TYPE_RUGBY,
                    EXERCISE_SESSION_TYPE_RUNNING,
                    EXERCISE_SESSION_TYPE_RUNNING_TREADMILL,
                    EXERCISE_SESSION_TYPE_SAILING,
                    EXERCISE_SESSION_TYPE_SCUBA_DIVING,
                    EXERCISE_SESSION_TYPE_SKATING,
                    EXERCISE_SESSION_TYPE_SKIING,
                    EXERCISE_SESSION_TYPE_SNOWBOARDING,
                    EXERCISE_SESSION_TYPE_SNOWSHOEING,
                    EXERCISE_SESSION_TYPE_SOCCER,
                    EXERCISE_SESSION_TYPE_SOFTBALL,
                    EXERCISE_SESSION_TYPE_SQUASH,
                    EXERCISE_SESSION_TYPE_STAIR_CLIMBING,
                    EXERCISE_SESSION_TYPE_STAIR_CLIMBING_MACHINE,
                    EXERCISE_SESSION_TYPE_STRENGTH_TRAINING,
                    EXERCISE_SESSION_TYPE_STRETCHING,
                    EXERCISE_SESSION_TYPE_SURFING,
                    EXERCISE_SESSION_TYPE_SWIMMING_OPEN_WATER,
                    EXERCISE_SESSION_TYPE_SWIMMING_POOL,
                    EXERCISE_SESSION_TYPE_TABLE_TENNIS,
                    EXERCISE_SESSION_TYPE_TENNIS,
                    EXERCISE_SESSION_TYPE_VOLLEYBALL,
                    EXERCISE_SESSION_TYPE_WALKING,
                    EXERCISE_SESSION_TYPE_WATER_POLO,
                    EXERCISE_SESSION_TYPE_WEIGHTLIFTING,
                    EXERCISE_SESSION_TYPE_WHEELCHAIR,
                    EXERCISE_SESSION_TYPE_OTHER_WORKOUT,
                    EXERCISE_SESSION_TYPE_YOGA);

    /**
     * Returns whether given session type is known by current module version, throws exception if
     * the type is negative.
     *
     * @hide
     */
    public static boolean isKnownSessionType(int sessionType) {
        if (sessionType < 0) {
            throw new IllegalArgumentException("Exercise session type must be non negative.");
        }

        return VALID_TYPES.contains(sessionType);
    }
}
