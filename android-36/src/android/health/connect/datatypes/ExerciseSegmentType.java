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
import java.util.List;
import java.util.Set;

/** Identifier for exercise types, as returned by {@link ExerciseSegment#getSegmentType()}. */
public final class ExerciseSegmentType {

    /** Use this type if the type of the exercise segment is not known. */
    public static final int EXERCISE_SEGMENT_TYPE_UNKNOWN = 0;

    /** Use this type for barbel shoulder press. */
    public static final int EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS = 1;

    /** Use this type for bench sit up. */
    public static final int EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP = 2;

    /** Use this type for biking. */
    public static final int EXERCISE_SEGMENT_TYPE_BIKING = 3;

    /** Use this type for stationary biking. */
    public static final int EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY = 4;

    /** Use this type for left arm dumbbell curl. */
    public static final int EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM = 5;

    /** Use this type for right arm dumbbell curl. */
    public static final int EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM = 6;

    /** Use this type for right arm dumbbell front raise. */
    public static final int EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE = 7;

    /** Use this type for dumbbell lateral raises. */
    public static final int EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE = 8;

    /** Use this type for left arm triceps extensions. */
    public static final int EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM = 9;

    /** Use this type for right arm triceps extensions. */
    public static final int EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM = 10;

    /** Use this type for two arms triceps extensions. */
    public static final int EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM = 11;

    /** Use this type for elliptical workout. */
    public static final int EXERCISE_SEGMENT_TYPE_ELLIPTICAL = 12;

    /** Use this type for forward twists. */
    public static final int EXERCISE_SEGMENT_TYPE_FORWARD_TWIST = 13;

    /** Use this type for pilates. */
    public static final int EXERCISE_SEGMENT_TYPE_PILATES = 14;

    /** Use this type for rowing machine workout. */
    public static final int EXERCISE_SEGMENT_TYPE_ROWING_MACHINE = 15;

    /** Use this type for running. */
    public static final int EXERCISE_SEGMENT_TYPE_RUNNING = 16;

    /** Use this type for treadmill running. */
    public static final int EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL = 17;

    /** Use this type for stair climbing. */
    public static final int EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING = 18;

    /** Use this type for stair climbing machine. */
    public static final int EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE = 19;

    /** Use this type for stretching. */
    public static final int EXERCISE_SEGMENT_TYPE_STRETCHING = 20;

    /** Use this type for swimming in open water. */
    public static final int EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER = 21;

    /** Use this type for swimming in the pool. */
    public static final int EXERCISE_SEGMENT_TYPE_SWIMMING_POOL = 22;

    /** Use this type for upper twists. */
    public static final int EXERCISE_SEGMENT_TYPE_UPPER_TWIST = 23;

    /** Use this type for walking. */
    public static final int EXERCISE_SEGMENT_TYPE_WALKING = 24;

    /** Use this type for wheelchair. */
    public static final int EXERCISE_SEGMENT_TYPE_WHEELCHAIR = 25;

    /** Use this type for arm curls. */
    public static final int EXERCISE_SEGMENT_TYPE_ARM_CURL = 26;

    /** Use this type for back extensions. */
    public static final int EXERCISE_SEGMENT_TYPE_BACK_EXTENSION = 27;

    /** Use this type for ball slams. */
    public static final int EXERCISE_SEGMENT_TYPE_BALL_SLAM = 28;

    /** Use this type for bench presses. */
    public static final int EXERCISE_SEGMENT_TYPE_BENCH_PRESS = 29;

    /** Use this type for burpees. */
    public static final int EXERCISE_SEGMENT_TYPE_BURPEE = 30;

    /** Use this type for crunches. */
    public static final int EXERCISE_SEGMENT_TYPE_CRUNCH = 31;

    /** Use this type for deadlifts. */
    public static final int EXERCISE_SEGMENT_TYPE_DEADLIFT = 32;

    /** Use this type for double arms triceps extensions. */
    public static final int EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION = 33;

    /** Use this type for dumbbells rows. */
    public static final int EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW = 34;

    /** Use this type for front raises. */
    public static final int EXERCISE_SEGMENT_TYPE_FRONT_RAISE = 35;

    /** Use this type for hip thrusts. */
    public static final int EXERCISE_SEGMENT_TYPE_HIP_THRUST = 36;

    /** Use this type for hula-hoops. */
    public static final int EXERCISE_SEGMENT_TYPE_HULA_HOOP = 37;

    /** Use this type for jumping jacks. */
    public static final int EXERCISE_SEGMENT_TYPE_JUMPING_JACK = 38;

    /** Use this type for jump rope. */
    public static final int EXERCISE_SEGMENT_TYPE_JUMP_ROPE = 39;

    /** Use this type for kettlebell swings. */
    public static final int EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING = 40;

    /** Use this type for lateral raises. */
    public static final int EXERCISE_SEGMENT_TYPE_LATERAL_RAISE = 41;

    /** Use this type for lat pull-downs. */
    public static final int EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN = 42;

    /** Use this type for leg curls. */
    public static final int EXERCISE_SEGMENT_TYPE_LEG_CURL = 43;

    /** Use this type for leg extensions. */
    public static final int EXERCISE_SEGMENT_TYPE_LEG_EXTENSION = 44;

    /** Use this type for leg presses. */
    public static final int EXERCISE_SEGMENT_TYPE_LEG_PRESS = 45;

    /** Use this type for leg raises. */
    public static final int EXERCISE_SEGMENT_TYPE_LEG_RAISE = 46;

    /** Use this type for lunges. */
    public static final int EXERCISE_SEGMENT_TYPE_LUNGE = 47;

    /** Use this type for mountain climber. */
    public static final int EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER = 48;

    /** Use this type for plank. */
    public static final int EXERCISE_SEGMENT_TYPE_PLANK = 49;

    /** Use this type for pull-ups. */
    public static final int EXERCISE_SEGMENT_TYPE_PULL_UP = 50;

    /** Use this type for punches. */
    public static final int EXERCISE_SEGMENT_TYPE_PUNCH = 51;

    /** Use this type for shoulder press. */
    public static final int EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS = 52;

    /** Use this type for single arm triceps extension. */
    public static final int EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION = 53;

    /** Use this type for sit-ups. */
    public static final int EXERCISE_SEGMENT_TYPE_SIT_UP = 54;

    /** Use this type for squats. */
    public static final int EXERCISE_SEGMENT_TYPE_SQUAT = 55;

    /** Use this type for freestyle swimming. */
    public static final int EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE = 56;

    /** Use this type for backstroke swimming. */
    public static final int EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE = 57;

    /** Use this type for breaststroke swimming. */
    public static final int EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE = 58;

    /** Use this type for butterfly swimming. */
    public static final int EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY = 59;

    /** Use this type for mixed swimming. */
    public static final int EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED = 60;

    /** Use this type if other swimming styles are not suitable. */
    public static final int EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER = 61;

    /** Use this type for high intensity training. */
    public static final int EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING = 62;

    /** Use this type for weightlifting. */
    public static final int EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING = 63;

    /** Use this type for other workout. */
    public static final int EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT = 64;

    /** Use this type for yoga. */
    public static final int EXERCISE_SEGMENT_TYPE_YOGA = 65;

    /** Use this type for the rest. */
    public static final int EXERCISE_SEGMENT_TYPE_REST = 66;

    /** Use this type for the pause. */
    public static final int EXERCISE_SEGMENT_TYPE_PAUSE = 67;

    private ExerciseSegmentType() {}

    /** @hide */
    @IntDef({
        EXERCISE_SEGMENT_TYPE_UNKNOWN,
        EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS,
        EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP,
        EXERCISE_SEGMENT_TYPE_BIKING,
        EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
        EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM,
        EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM,
        EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE,
        EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE,
        EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
        EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
        EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
        EXERCISE_SEGMENT_TYPE_FORWARD_TWIST,
        EXERCISE_SEGMENT_TYPE_ELLIPTICAL,
        EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
        EXERCISE_SEGMENT_TYPE_PILATES,
        EXERCISE_SEGMENT_TYPE_ROWING_MACHINE,
        EXERCISE_SEGMENT_TYPE_RUNNING,
        EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL,
        EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING,
        EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE,
        EXERCISE_SEGMENT_TYPE_STRETCHING,
        EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER,
        EXERCISE_SEGMENT_TYPE_SWIMMING_POOL,
        EXERCISE_SEGMENT_TYPE_UPPER_TWIST,
        EXERCISE_SEGMENT_TYPE_WALKING,
        EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
        EXERCISE_SEGMENT_TYPE_WHEELCHAIR,
        EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
        EXERCISE_SEGMENT_TYPE_YOGA,
        EXERCISE_SEGMENT_TYPE_ARM_CURL,
        EXERCISE_SEGMENT_TYPE_BACK_EXTENSION,
        EXERCISE_SEGMENT_TYPE_BALL_SLAM,
        EXERCISE_SEGMENT_TYPE_BENCH_PRESS,
        EXERCISE_SEGMENT_TYPE_BURPEE,
        EXERCISE_SEGMENT_TYPE_CRUNCH,
        EXERCISE_SEGMENT_TYPE_DEADLIFT,
        EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION,
        EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW,
        EXERCISE_SEGMENT_TYPE_FRONT_RAISE,
        EXERCISE_SEGMENT_TYPE_HIP_THRUST,
        EXERCISE_SEGMENT_TYPE_HULA_HOOP,
        EXERCISE_SEGMENT_TYPE_JUMPING_JACK,
        EXERCISE_SEGMENT_TYPE_JUMP_ROPE,
        EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING,
        EXERCISE_SEGMENT_TYPE_LATERAL_RAISE,
        EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN,
        EXERCISE_SEGMENT_TYPE_LEG_CURL,
        EXERCISE_SEGMENT_TYPE_LEG_EXTENSION,
        EXERCISE_SEGMENT_TYPE_LEG_PRESS,
        EXERCISE_SEGMENT_TYPE_LEG_RAISE,
        EXERCISE_SEGMENT_TYPE_LUNGE,
        EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER,
        EXERCISE_SEGMENT_TYPE_PLANK,
        EXERCISE_SEGMENT_TYPE_PULL_UP,
        EXERCISE_SEGMENT_TYPE_PUNCH,
        EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS,
        EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION,
        EXERCISE_SEGMENT_TYPE_SIT_UP,
        EXERCISE_SEGMENT_TYPE_SQUAT,
        EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE,
        EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
        EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE,
        EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY,
        EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED,
        EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER,
        EXERCISE_SEGMENT_TYPE_REST,
        EXERCISE_SEGMENT_TYPE_PAUSE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ExerciseSegmentTypes {}

    /**
     * Exercise segments types which are excluded from exercise session duration.
     *
     * @hide
     */
    public static final List<Integer> DURATION_EXCLUDE_TYPES =
            List.of(
                    ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PAUSE,
                    ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_REST);

    // Update this set when add new type or deprecate existing type.
    private static final Set<Integer> VALID_TYPES =
            Set.of(
                    EXERCISE_SEGMENT_TYPE_UNKNOWN,
                    EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS,
                    EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP,
                    EXERCISE_SEGMENT_TYPE_BIKING,
                    EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
                    EXERCISE_SEGMENT_TYPE_FORWARD_TWIST,
                    EXERCISE_SEGMENT_TYPE_ELLIPTICAL,
                    EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                    EXERCISE_SEGMENT_TYPE_PILATES,
                    EXERCISE_SEGMENT_TYPE_ROWING_MACHINE,
                    EXERCISE_SEGMENT_TYPE_RUNNING,
                    EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL,
                    EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING,
                    EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE,
                    EXERCISE_SEGMENT_TYPE_STRETCHING,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_POOL,
                    EXERCISE_SEGMENT_TYPE_UPPER_TWIST,
                    EXERCISE_SEGMENT_TYPE_WALKING,
                    EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
                    EXERCISE_SEGMENT_TYPE_WHEELCHAIR,
                    EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
                    EXERCISE_SEGMENT_TYPE_YOGA,
                    EXERCISE_SEGMENT_TYPE_ARM_CURL,
                    EXERCISE_SEGMENT_TYPE_BACK_EXTENSION,
                    EXERCISE_SEGMENT_TYPE_BALL_SLAM,
                    EXERCISE_SEGMENT_TYPE_BENCH_PRESS,
                    EXERCISE_SEGMENT_TYPE_BURPEE,
                    EXERCISE_SEGMENT_TYPE_CRUNCH,
                    EXERCISE_SEGMENT_TYPE_DEADLIFT,
                    EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW,
                    EXERCISE_SEGMENT_TYPE_FRONT_RAISE,
                    EXERCISE_SEGMENT_TYPE_HIP_THRUST,
                    EXERCISE_SEGMENT_TYPE_HULA_HOOP,
                    EXERCISE_SEGMENT_TYPE_JUMPING_JACK,
                    EXERCISE_SEGMENT_TYPE_JUMP_ROPE,
                    EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING,
                    EXERCISE_SEGMENT_TYPE_LATERAL_RAISE,
                    EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN,
                    EXERCISE_SEGMENT_TYPE_LEG_CURL,
                    EXERCISE_SEGMENT_TYPE_LEG_EXTENSION,
                    EXERCISE_SEGMENT_TYPE_LEG_PRESS,
                    EXERCISE_SEGMENT_TYPE_LEG_RAISE,
                    EXERCISE_SEGMENT_TYPE_LUNGE,
                    EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER,
                    EXERCISE_SEGMENT_TYPE_PLANK,
                    EXERCISE_SEGMENT_TYPE_PULL_UP,
                    EXERCISE_SEGMENT_TYPE_PUNCH,
                    EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS,
                    EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION,
                    EXERCISE_SEGMENT_TYPE_SIT_UP,
                    EXERCISE_SEGMENT_TYPE_SQUAT,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER,
                    EXERCISE_SEGMENT_TYPE_REST,
                    EXERCISE_SEGMENT_TYPE_PAUSE);

    /**
     * Returns whether given segment type is known by current module version, throws exception if
     * the type is negative.
     *
     * @hide
     */
    public static boolean isKnownSegmentType(int segmentType) {
        if (segmentType < 0) {
            throw new IllegalArgumentException("Exercise segment type must be non negative.");
        }

        return VALID_TYPES.contains(segmentType);
    }
}
