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

package android.health.connect.datatypes.validation;

import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BACK_EXTENSION;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BALL_SLAM;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BENCH_PRESS;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BIKING;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BURPEE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_CRUNCH;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DEADLIFT;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ELLIPTICAL;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_FORWARD_TWIST;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_FRONT_RAISE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_HIP_THRUST;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_HULA_HOOP;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_JUMPING_JACK;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_JUMP_ROPE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LATERAL_RAISE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_CURL;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_EXTENSION;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_PRESS;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LEG_RAISE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_LUNGE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PAUSE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PILATES;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PLANK;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PULL_UP;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_PUNCH;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_REST;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ROWING_MACHINE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SIT_UP;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SQUAT;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_STRETCHING;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_SWIMMING_POOL;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_UNKNOWN;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_UPPER_TWIST;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WALKING;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_WHEELCHAIR;
import static android.health.connect.datatypes.ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_YOGA;
import static android.health.connect.datatypes.ExerciseSegmentType.isKnownSegmentType;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING_STATIONARY;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_BOOT_CAMP;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_CALISTHENICS;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_ELLIPTICAL;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_EXERCISE_CLASS;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_GYMNASTICS;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_HIKING;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_OTHER_WORKOUT;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_PILATES;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_ROWING_MACHINE;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING_TREADMILL;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_STAIR_CLIMBING;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_STAIR_CLIMBING_MACHINE;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_STRENGTH_TRAINING;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_SWIMMING_OPEN_WATER;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_SWIMMING_POOL;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_UNKNOWN;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_WALKING;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_WEIGHTLIFTING;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_WHEELCHAIR;
import static android.health.connect.datatypes.ExerciseSessionType.EXERCISE_SESSION_TYPE_YOGA;
import static android.health.connect.datatypes.ExerciseSessionType.isKnownSessionType;

import android.health.connect.datatypes.ExerciseRoute;
import android.health.connect.datatypes.ExerciseSegment;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to validate compatibility between {@link
 * android.health.connect.datatypes.ExerciseSessionRecord} type and child {@link
 * android.health.connect.datatypes.ExerciseSegment} types.
 *
 * @hide
 */
public final class ExerciseSessionTypesValidation {
    private static final String TAG = "ExerciseSessionsValidations";

    private static final Set<Integer> UNIVERSAL_SEGMENTS =
            Set.of(
                    EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
                    EXERCISE_SEGMENT_TYPE_PAUSE,
                    EXERCISE_SEGMENT_TYPE_REST,
                    EXERCISE_SEGMENT_TYPE_STRETCHING,
                    EXERCISE_SEGMENT_TYPE_UNKNOWN);

    private static final Set<Integer> UNIVERSAL_SESSIONS =
            Set.of(
                    EXERCISE_SESSION_TYPE_BOOT_CAMP,
                    EXERCISE_SESSION_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                    EXERCISE_SESSION_TYPE_OTHER_WORKOUT,
                    EXERCISE_SESSION_TYPE_UNKNOWN);

    private static final Set<Integer> EXERCISES =
            Set.of(
                    EXERCISE_SEGMENT_TYPE_ARM_CURL,
                    EXERCISE_SEGMENT_TYPE_BACK_EXTENSION,
                    EXERCISE_SEGMENT_TYPE_BALL_SLAM,
                    EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS,
                    EXERCISE_SEGMENT_TYPE_BENCH_PRESS,
                    EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP,
                    EXERCISE_SEGMENT_TYPE_BURPEE,
                    EXERCISE_SEGMENT_TYPE_CRUNCH,
                    EXERCISE_SEGMENT_TYPE_DEADLIFT,
                    EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
                    EXERCISE_SEGMENT_TYPE_FORWARD_TWIST,
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
                    EXERCISE_SEGMENT_TYPE_UPPER_TWIST,
                    EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING);

    private static final Set<Integer> SWIMMING =
            Set.of(
                    EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER);

    private static final Map<Integer, Set<Integer>> SESSION_TO_SEGMENT_MAPPING = new ArrayMap<>();

    static {
        SESSION_TO_SEGMENT_MAPPING.put(
                EXERCISE_SESSION_TYPE_BIKING, Set.of(EXERCISE_SEGMENT_TYPE_BIKING));

        SESSION_TO_SEGMENT_MAPPING.put(
                EXERCISE_SESSION_TYPE_BIKING_STATIONARY,
                Set.of(EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY));

        SESSION_TO_SEGMENT_MAPPING.put(EXERCISE_SESSION_TYPE_CALISTHENICS, EXERCISES);

        SESSION_TO_SEGMENT_MAPPING.put(
                EXERCISE_SESSION_TYPE_ELLIPTICAL, Set.of(EXERCISE_SEGMENT_TYPE_ELLIPTICAL));

        SESSION_TO_SEGMENT_MAPPING.put(
                EXERCISE_SESSION_TYPE_EXERCISE_CLASS,
                Set.of(
                        EXERCISE_SEGMENT_TYPE_YOGA,
                        EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                        EXERCISE_SEGMENT_TYPE_PILATES,
                        EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING));
        SESSION_TO_SEGMENT_MAPPING.put(EXERCISE_SESSION_TYPE_GYMNASTICS, EXERCISES);

        SESSION_TO_SEGMENT_MAPPING.put(
                EXERCISE_SESSION_TYPE_HIKING,
                Set.of(EXERCISE_SEGMENT_TYPE_WALKING, EXERCISE_SEGMENT_TYPE_WHEELCHAIR));

        SESSION_TO_SEGMENT_MAPPING.put(
                EXERCISE_SESSION_TYPE_PILATES, Set.of(EXERCISE_SEGMENT_TYPE_PILATES));

        SESSION_TO_SEGMENT_MAPPING.put(
                EXERCISE_SESSION_TYPE_ROWING_MACHINE, Set.of(EXERCISE_SEGMENT_TYPE_ROWING_MACHINE));

        SESSION_TO_SEGMENT_MAPPING.put(
                EXERCISE_SESSION_TYPE_RUNNING,
                Set.of(EXERCISE_SEGMENT_TYPE_RUNNING, EXERCISE_SEGMENT_TYPE_WALKING));

        SESSION_TO_SEGMENT_MAPPING.put(
                EXERCISE_SESSION_TYPE_RUNNING_TREADMILL,
                Set.of(EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL));

        SESSION_TO_SEGMENT_MAPPING.put(
                EXERCISE_SESSION_TYPE_STAIR_CLIMBING, Set.of(EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING));

        SESSION_TO_SEGMENT_MAPPING.put(
                EXERCISE_SESSION_TYPE_STAIR_CLIMBING_MACHINE,
                Set.of(EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE));

        SESSION_TO_SEGMENT_MAPPING.put(EXERCISE_SESSION_TYPE_STRENGTH_TRAINING, EXERCISES);

        Set<Integer> openSwimming = new ArraySet<>(SWIMMING);
        openSwimming.add(EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER);
        SESSION_TO_SEGMENT_MAPPING.put(EXERCISE_SESSION_TYPE_SWIMMING_OPEN_WATER, openSwimming);

        Set<Integer> poolSwimming = new ArraySet<>(SWIMMING);
        poolSwimming.add(EXERCISE_SEGMENT_TYPE_SWIMMING_POOL);
        SESSION_TO_SEGMENT_MAPPING.put(EXERCISE_SESSION_TYPE_SWIMMING_POOL, poolSwimming);

        SESSION_TO_SEGMENT_MAPPING.put(
                EXERCISE_SESSION_TYPE_WALKING, Set.of(EXERCISE_SEGMENT_TYPE_WALKING));

        SESSION_TO_SEGMENT_MAPPING.put(EXERCISE_SESSION_TYPE_WEIGHTLIFTING, EXERCISES);

        SESSION_TO_SEGMENT_MAPPING.put(
                EXERCISE_SESSION_TYPE_WHEELCHAIR, Set.of(EXERCISE_SEGMENT_TYPE_WHEELCHAIR));

        SESSION_TO_SEGMENT_MAPPING.put(
                EXERCISE_SESSION_TYPE_YOGA, Set.of(EXERCISE_SEGMENT_TYPE_YOGA));
    }

    /**
     * Validates that session type is compatible with segments types. Skips checks if the session or
     * segment type is not known with the current module version.
     *
     * @hide
     */
    public static void validateSessionAndSegmentsTypes(
            int sessionType, List<ExerciseSegment> segments) {
        if (!isKnownSessionType(sessionType)) {
            Slog.w(TAG, "Unknown exercise session type: " + sessionType);
            return;
        }

        int segmentType;
        for (ExerciseSegment segment : segments) {
            segmentType = segment.getSegmentType();

            if (!isKnownSegmentType(segmentType)) {
                Slog.w(TAG, "Unknown exercise segment type: " + segmentType);
                continue;
            }

            // Both segment and session types are known, check their compatibility
            if (!UNIVERSAL_SESSIONS.contains(sessionType)) {
                if (UNIVERSAL_SEGMENTS.contains(segmentType)) {
                    continue;
                }

                if (!(SESSION_TO_SEGMENT_MAPPING.containsKey(sessionType)
                        && SESSION_TO_SEGMENT_MAPPING.get(sessionType).contains(segmentType))) {
                    throw new IllegalArgumentException(
                            "Invalid exercise segment type for given exercise session type");
                }
            }
        }
    }

    /**
     * Validates that all {@link ExerciseRoute} locations timestamps are within session start and
     * end time interval.
     *
     * @hide
     */
    public static void validateExerciseRouteTimestamps(
            Instant sessionStartTime, Instant sessionEndTime, ExerciseRoute route) {
        if (route == null) {
            return;
        }

        for (ExerciseRoute.Location location : route.getRouteLocations()) {
            if (location.getTime().isAfter(sessionEndTime)
                    || location.getTime().isBefore(sessionStartTime)) {
                throw new IllegalArgumentException(
                        "Exercise route timestamp must be within session interval, got start time: "
                                + sessionStartTime
                                + ", session end time: "
                                + sessionEndTime
                                + ", route location time: "
                                + location.getTime());
            }
        }
    }
}
