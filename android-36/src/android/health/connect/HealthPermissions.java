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

package android.health.connect;

import static android.health.connect.Constants.DEFAULT_INT;
import static android.health.connect.HealthPermissionCategory.ACTIVE_CALORIES_BURNED;
import static android.health.connect.HealthPermissionCategory.BASAL_BODY_TEMPERATURE;
import static android.health.connect.HealthPermissionCategory.BASAL_METABOLIC_RATE;
import static android.health.connect.HealthPermissionCategory.BLOOD_GLUCOSE;
import static android.health.connect.HealthPermissionCategory.BLOOD_PRESSURE;
import static android.health.connect.HealthPermissionCategory.BODY_FAT;
import static android.health.connect.HealthPermissionCategory.BODY_TEMPERATURE;
import static android.health.connect.HealthPermissionCategory.BODY_WATER_MASS;
import static android.health.connect.HealthPermissionCategory.BONE_MASS;
import static android.health.connect.HealthPermissionCategory.CERVICAL_MUCUS;
import static android.health.connect.HealthPermissionCategory.DISTANCE;
import static android.health.connect.HealthPermissionCategory.ELEVATION_GAINED;
import static android.health.connect.HealthPermissionCategory.EXERCISE;
import static android.health.connect.HealthPermissionCategory.FLOORS_CLIMBED;
import static android.health.connect.HealthPermissionCategory.HEART_RATE;
import static android.health.connect.HealthPermissionCategory.HEART_RATE_VARIABILITY;
import static android.health.connect.HealthPermissionCategory.HEIGHT;
import static android.health.connect.HealthPermissionCategory.HYDRATION;
import static android.health.connect.HealthPermissionCategory.INTERMENSTRUAL_BLEEDING;
import static android.health.connect.HealthPermissionCategory.LEAN_BODY_MASS;
import static android.health.connect.HealthPermissionCategory.MENSTRUATION;
import static android.health.connect.HealthPermissionCategory.MINDFULNESS;
import static android.health.connect.HealthPermissionCategory.NUTRITION;
import static android.health.connect.HealthPermissionCategory.OVULATION_TEST;
import static android.health.connect.HealthPermissionCategory.OXYGEN_SATURATION;
import static android.health.connect.HealthPermissionCategory.PLANNED_EXERCISE;
import static android.health.connect.HealthPermissionCategory.POWER;
import static android.health.connect.HealthPermissionCategory.RESPIRATORY_RATE;
import static android.health.connect.HealthPermissionCategory.RESTING_HEART_RATE;
import static android.health.connect.HealthPermissionCategory.SEXUAL_ACTIVITY;
import static android.health.connect.HealthPermissionCategory.SKIN_TEMPERATURE;
import static android.health.connect.HealthPermissionCategory.SLEEP;
import static android.health.connect.HealthPermissionCategory.SPEED;
import static android.health.connect.HealthPermissionCategory.STEPS;
import static android.health.connect.HealthPermissionCategory.TOTAL_CALORIES_BURNED;
import static android.health.connect.HealthPermissionCategory.VO2_MAX;
import static android.health.connect.HealthPermissionCategory.WEIGHT;
import static android.health.connect.HealthPermissionCategory.WHEELCHAIR_PUSHES;

import static com.android.healthfitness.flags.AconfigFlagHelper.isPersonalHealthRecordEnabled;
import static com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY;
import static com.android.healthfitness.flags.Flags.FLAG_MINDFULNESS;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PermissionInfo;
import android.health.connect.datatypes.ExerciseRoute;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.healthfitness.flags.Flags;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// TODO(b/255340973): consider generate this class.
/**
 * Permissions for accessing the HealthConnect APIs.
 *
 * <p>Apps must support {@link android.content.Intent#ACTION_VIEW_PERMISSION_USAGE} with {@link
 * HealthConnectManager#CATEGORY_HEALTH_PERMISSIONS} category to be granted read/write health data
 * permissions.
 */
public final class HealthPermissions {
    /**
     * Allows an application to grant/revoke health-related permissions.
     *
     * <p>Protection level: signature.
     *
     * @hide
     */
    @SystemApi
    public static final String MANAGE_HEALTH_PERMISSIONS =
            "android.permission.MANAGE_HEALTH_PERMISSIONS";

    // Below permission was earlier declared in HealthConnectManager since it was only permission
    // used by access logs API, is now declared here along with the other system permission.
    // Please suggest if it will be ok to have it here.
    /**
     * Allows an application to modify health data.
     *
     * <p>Protection level: privileged.
     *
     * @hide
     */
    @SystemApi
    public static final String MANAGE_HEALTH_DATA_PERMISSION =
            "android.permission.MANAGE_HEALTH_DATA";

    /**
     * Allows an application to launch client onboarding activities responsible for connecting to
     * Health Connect. This permission can only be held by the system. Client apps that choose to
     * export an onboarding activity must guard it with this permission so that only the system can
     * launch it.
     *
     * <p>See {@link HealthConnectManager#ACTION_SHOW_ONBOARDING} for the corresponding intent used
     * by the system to launch onboarding activities.
     *
     * <p>Protection level: signature.
     *
     * @hide
     */
    public static final String START_ONBOARDING = "android.permission.health.START_ONBOARDING";

    /**
     * Used for runtime permissions which grant access to Health Connect data.
     *
     * @hide
     */
    @SystemApi
    public static final String HEALTH_PERMISSION_GROUP = "android.permission-group.HEALTH";

    /**
     * Allows an application to read health data (of any type) in background.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi("com.android.healthconnect.flags.background_read")
    public static final String READ_HEALTH_DATA_IN_BACKGROUND =
            "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND";

    /**
     * Allows an application to read the entire history of health data (of any type).
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi("com.android.healthconnect.flags.history_read")
    public static final String READ_HEALTH_DATA_HISTORY =
            "android.permission.health.READ_HEALTH_DATA_HISTORY";

    /**
     * Allows an application to read the user's active calories burned data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_ACTIVE_CALORIES_BURNED =
            "android.permission.health.READ_ACTIVE_CALORIES_BURNED";

    /**
     * Allows an application to read the user's activity intensity data.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_ACTIVITY_INTENSITY)
    public static final String READ_ACTIVITY_INTENSITY =
            "android.permission.health.READ_ACTIVITY_INTENSITY";

    /**
     * Allows an application to read the user's distance data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_DISTANCE = "android.permission.health.READ_DISTANCE";

    /**
     * Allows an application to read the user's elevation gained data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_ELEVATION_GAINED =
            "android.permission.health.READ_ELEVATION_GAINED";

    /**
     * Allows an application to read the user's exercise data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_EXERCISE = "android.permission.health.READ_EXERCISE";

    /**
     * Allows an application to read any {@link ExerciseRoute}. Not connected with READ_EXERCISE
     * permission, as it's used only by HealthConnectController to show routes in UI and share one
     * particular route with third party app after one-time user consent.
     *
     * <p>Protection level: signature.
     *
     * @hide
     */
    public static final String READ_EXERCISE_ROUTE =
            "android.permission.health.READ_EXERCISE_ROUTE";

    /**
     * Allows an application to read {@link ExerciseRoute}.
     *
     * <p>This permission can only be granted manually by a user in Health Connect settings or in
     * the route request activity which can be launched using {@link ACTION_REQUEST_EXERCISE_ROUTE}.
     * Attempts to request the permission by applications will be ignored.
     *
     * <p>Applications should check if the permission has been granted before reading {@link
     * ExerciseRoute}.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi("com.android.healthconnect.flags.read_exercise_routes_all_enabled")
    public static final String READ_EXERCISE_ROUTES =
            "android.permission.health.READ_EXERCISE_ROUTES";

    /**
     * Allows an application to read the user's floors climbed data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_FLOORS_CLIMBED =
            "android.permission.health.READ_FLOORS_CLIMBED";

    /**
     * Allows an application to read the user's steps data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_STEPS = "android.permission.health.READ_STEPS";

    /**
     * Allows an application to read the user's total calories burned data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_TOTAL_CALORIES_BURNED =
            "android.permission.health.READ_TOTAL_CALORIES_BURNED";

    /**
     * Allows an application to read the user's vo2 maximum data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_VO2_MAX = "android.permission.health.READ_VO2_MAX";

    /**
     * Allows an application to read the user's wheelchair pushes data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_WHEELCHAIR_PUSHES =
            "android.permission.health.READ_WHEELCHAIR_PUSHES";

    /**
     * Allows an application to read the user's power data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_POWER = "android.permission.health.READ_POWER";

    /**
     * Allows an application to read the user's speed data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_SPEED = "android.permission.health.READ_SPEED";

    /**
     * Allows an application to read the user's basal metabolic rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BASAL_METABOLIC_RATE =
            "android.permission.health.READ_BASAL_METABOLIC_RATE";

    /**
     * Allows an application to read the user's body fat data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BODY_FAT = "android.permission.health.READ_BODY_FAT";

    /**
     * Allows an application to read the user's body water mass data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BODY_WATER_MASS =
            "android.permission.health.READ_BODY_WATER_MASS";

    /**
     * Allows an application to read the user's bone mass data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BONE_MASS = "android.permission.health.READ_BONE_MASS";

    /**
     * Allows an application to read the user's height data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_HEIGHT = "android.permission.health.READ_HEIGHT";

    /**
     * Allows an application to read the user's lean body mass data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_LEAN_BODY_MASS =
            "android.permission.health.READ_LEAN_BODY_MASS";

    /**
     * Allows an application to read the user's weight data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_WEIGHT = "android.permission.health.READ_WEIGHT";

    /**
     * Allows an application to read the user's cervical mucus data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_CERVICAL_MUCUS =
            "android.permission.health.READ_CERVICAL_MUCUS";

    /**
     * Allows an application to read the user's menstruation data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_MENSTRUATION = "android.permission.health.READ_MENSTRUATION";

    /**
     * Allows an application to read the user's intermenstrual bleeding data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_INTERMENSTRUAL_BLEEDING =
            "android.permission.health.READ_INTERMENSTRUAL_BLEEDING";

    /**
     * Allows an application to read the user's ovulation test data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_OVULATION_TEST =
            "android.permission.health.READ_OVULATION_TEST";

    /**
     * Allows an application to read the user's sexual activity data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_SEXUAL_ACTIVITY =
            "android.permission.health.READ_SEXUAL_ACTIVITY";

    /**
     * Allows an application to read the user's hydration data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_HYDRATION = "android.permission.health.READ_HYDRATION";

    /**
     * Allows an application to read the user's nutrition data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_NUTRITION = "android.permission.health.READ_NUTRITION";

    /**
     * Allows an application to read the user's sleep data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_SLEEP = "android.permission.health.READ_SLEEP";

    /**
     * Allows an application to read the user's body temperature data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BASAL_BODY_TEMPERATURE =
            "android.permission.health.READ_BASAL_BODY_TEMPERATURE";

    /**
     * Allows an application to read the user's blood glucose data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BLOOD_GLUCOSE = "android.permission.health.READ_BLOOD_GLUCOSE";

    /**
     * Allows an application to read the user's blood pressure data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BLOOD_PRESSURE =
            "android.permission.health.READ_BLOOD_PRESSURE";

    /**
     * Allows an application to read the user's body temperature data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_BODY_TEMPERATURE =
            "android.permission.health.READ_BODY_TEMPERATURE";

    /**
     * Allows an application to read the user's heart rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_HEART_RATE = "android.permission.health.READ_HEART_RATE";

    /**
     * Allows an application to read the user's heart rate variability data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_HEART_RATE_VARIABILITY =
            "android.permission.health.READ_HEART_RATE_VARIABILITY";

    /**
     * Allows an application to read the user's oxygen saturation data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_OXYGEN_SATURATION =
            "android.permission.health.READ_OXYGEN_SATURATION";

    /**
     * Allows an application to read the user's respiratory rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_RESPIRATORY_RATE =
            "android.permission.health.READ_RESPIRATORY_RATE";

    /**
     * Allows an application to read the user's resting heart rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String READ_RESTING_HEART_RATE =
            "android.permission.health.READ_RESTING_HEART_RATE";

    /**
     * Allows an application to read the user's skin temperature data.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi("com.android.healthconnect.flags.skin_temperature")
    public static final String READ_SKIN_TEMPERATURE =
            "android.permission.health.READ_SKIN_TEMPERATURE";

    /**
     * Allows an application to read the user's training plan data.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final String READ_PLANNED_EXERCISE =
            "android.permission.health.READ_PLANNED_EXERCISE";

    /**
     * Allows an application to read user's mindfulness data.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_MINDFULNESS)
    public static final String READ_MINDFULNESS = "android.permission.health.READ_MINDFULNESS";

    /**
     * Allows an application to write the user's calories burned data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_ACTIVE_CALORIES_BURNED =
            "android.permission.health.WRITE_ACTIVE_CALORIES_BURNED";

    /**
     * Allows an application to write the user's activity intensity data.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_ACTIVITY_INTENSITY)
    public static final String WRITE_ACTIVITY_INTENSITY =
            "android.permission.health.WRITE_ACTIVITY_INTENSITY";

    /**
     * Allows an application to write the user's distance data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_DISTANCE = "android.permission.health.WRITE_DISTANCE";

    /**
     * Allows an application to write the user's elevation gained data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_ELEVATION_GAINED =
            "android.permission.health.WRITE_ELEVATION_GAINED";

    /**
     * Allows an application to write the user's exercise data. Additional permission {@link
     * HealthPermissions#WRITE_EXERCISE_ROUTE} is required to write user's exercise route.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_EXERCISE = "android.permission.health.WRITE_EXERCISE";

    /**
     * Allows an application to write the user's exercise route.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_EXERCISE_ROUTE =
            "android.permission.health.WRITE_EXERCISE_ROUTE";

    /**
     * Allows an application to write the user's floors climbed data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_FLOORS_CLIMBED =
            "android.permission.health.WRITE_FLOORS_CLIMBED";

    /**
     * Allows an application to write the user's steps data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_STEPS = "android.permission.health.WRITE_STEPS";

    /**
     * Allows an application to write the user's total calories burned data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_TOTAL_CALORIES_BURNED =
            "android.permission.health.WRITE_TOTAL_CALORIES_BURNED";

    /**
     * Allows an application to write the user's vo2 maximum data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_VO2_MAX = "android.permission.health.WRITE_VO2_MAX";

    /**
     * Allows an application to write the user's wheelchair pushes data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_WHEELCHAIR_PUSHES =
            "android.permission.health.WRITE_WHEELCHAIR_PUSHES";

    /**
     * Allows an application to write the user's power data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_POWER = "android.permission.health.WRITE_POWER";

    /**
     * Allows an application to write the user's speed data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_SPEED = "android.permission.health.WRITE_SPEED";

    /**
     * Allows an application to write the user's basal metabolic rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BASAL_METABOLIC_RATE =
            "android.permission.health.WRITE_BASAL_METABOLIC_RATE";

    /**
     * Allows an application to write the user's body fat data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BODY_FAT = "android.permission.health.WRITE_BODY_FAT";

    /**
     * Allows an application to write the user's body water mass data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BODY_WATER_MASS =
            "android.permission.health.WRITE_BODY_WATER_MASS";

    /**
     * Allows an application to write the user's bone mass data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BONE_MASS = "android.permission.health.WRITE_BONE_MASS";

    /**
     * Allows an application to write the user's height data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_HEIGHT = "android.permission.health.WRITE_HEIGHT";

    /**
     * Allows an application to write the user's lean body mass data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_LEAN_BODY_MASS =
            "android.permission.health.WRITE_LEAN_BODY_MASS";

    /**
     * Allows an application to write the user's weight data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_WEIGHT = "android.permission.health.WRITE_WEIGHT";

    /**
     * Allows an application to write the user's cervical mucus data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_CERVICAL_MUCUS =
            "android.permission.health.WRITE_CERVICAL_MUCUS";

    /**
     * Allows an application to write the user's menstruation data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_MENSTRUATION = "android.permission.health.WRITE_MENSTRUATION";

    /**
     * Allows an application to write the user's intermenstrual bleeding data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_INTERMENSTRUAL_BLEEDING =
            "android.permission.health.WRITE_INTERMENSTRUAL_BLEEDING";

    /**
     * Allows an application to write the user's ovulation test data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_OVULATION_TEST =
            "android.permission.health.WRITE_OVULATION_TEST";

    /**
     * Allows an application to write the user's sexual activity data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_SEXUAL_ACTIVITY =
            "android.permission.health.WRITE_SEXUAL_ACTIVITY";

    /**
     * Allows an application to write the user's hydration data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_HYDRATION = "android.permission.health.WRITE_HYDRATION";

    /**
     * Allows an application to write the user's nutrition data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_NUTRITION = "android.permission.health.WRITE_NUTRITION";

    /**
     * Allows an application to write the user's sleep data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_SLEEP = "android.permission.health.WRITE_SLEEP";

    /**
     * Allows an application to write the user's basal body temperature data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BASAL_BODY_TEMPERATURE =
            "android.permission.health.WRITE_BASAL_BODY_TEMPERATURE";

    /**
     * Allows an application to write the user's blood glucose data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BLOOD_GLUCOSE =
            "android.permission.health.WRITE_BLOOD_GLUCOSE";

    /**
     * Allows an application to write the user's blood pressure data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BLOOD_PRESSURE =
            "android.permission.health.WRITE_BLOOD_PRESSURE";

    /**
     * Allows an application to write the user's body temperature data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_BODY_TEMPERATURE =
            "android.permission.health.WRITE_BODY_TEMPERATURE";

    /**
     * Allows an application to write the user's heart rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_HEART_RATE = "android.permission.health.WRITE_HEART_RATE";

    /**
     * Allows an application to write the user's heart rate variability data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_HEART_RATE_VARIABILITY =
            "android.permission.health.WRITE_HEART_RATE_VARIABILITY";

    /**
     * Allows an application to write the user's oxygen saturation data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_OXYGEN_SATURATION =
            "android.permission.health.WRITE_OXYGEN_SATURATION";

    /**
     * Allows an application to write the user's respiratory rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_RESPIRATORY_RATE =
            "android.permission.health.WRITE_RESPIRATORY_RATE";

    /**
     * Allows an application to write the user's resting heart rate data.
     *
     * <p>Protection level: dangerous.
     */
    public static final String WRITE_RESTING_HEART_RATE =
            "android.permission.health.WRITE_RESTING_HEART_RATE";

    /**
     * Allows an application to write the user's skin temperature data.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi("com.android.healthconnect.flags.skin_temperature")
    public static final String WRITE_SKIN_TEMPERATURE =
            "android.permission.health.WRITE_SKIN_TEMPERATURE";

    /**
     * Allows an application to write the user's training plan data.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi("com.android.healthconnect.flags.training_plans")
    public static final String WRITE_PLANNED_EXERCISE =
            "android.permission.health.WRITE_PLANNED_EXERCISE";

    /**
     * Allows an application to write user's mindfulness data.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_MINDFULNESS)
    public static final String WRITE_MINDFULNESS = "android.permission.health.WRITE_MINDFULNESS";

    /* Personal Health Record permissions */

    /**
     * Allows an application to read the user's data about allergies and intolerances.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public static final String READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES =
            "android.permission.health.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES";

    /**
     * Allows an application to read the user's data about medical conditions.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public static final String READ_MEDICAL_DATA_CONDITIONS =
            "android.permission.health.READ_MEDICAL_DATA_CONDITIONS";

    /**
     * Allows an application to read the user's laboratory result data.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public static final String READ_MEDICAL_DATA_LABORATORY_RESULTS =
            "android.permission.health.READ_MEDICAL_DATA_LABORATORY_RESULTS";

    /**
     * Allows an application to read the user's medication data.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public static final String READ_MEDICAL_DATA_MEDICATIONS =
            "android.permission.health.READ_MEDICAL_DATA_MEDICATIONS";

    /**
     * Allows an application to read the user's personal details.
     *
     * <p>This is demographic information such as name, date of birth, contact details like address
     * or telephone number and so on. For more examples see the <a
     * href="https://www.hl7.org/fhir/patient.html">FHIR Patient resource</a>.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public static final String READ_MEDICAL_DATA_PERSONAL_DETAILS =
            "android.permission.health.READ_MEDICAL_DATA_PERSONAL_DETAILS";

    /**
     * Allows an application to read the user's data about the practitioners who have interacted
     * with them in their medical record. This is the information about the clinicians (doctors,
     * nurses, etc) but also other practitioners (masseurs, physiotherapists, etc) who have been
     * involved with the patient.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public static final String READ_MEDICAL_DATA_PRACTITIONER_DETAILS =
            "android.permission.health.READ_MEDICAL_DATA_PRACTITIONER_DETAILS";

    /**
     * Allows an application to read the user's pregnancy data.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public static final String READ_MEDICAL_DATA_PREGNANCY =
            "android.permission.health.READ_MEDICAL_DATA_PREGNANCY";

    /**
     * Allows an application to read the user's data about medical procedures.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public static final String READ_MEDICAL_DATA_PROCEDURES =
            "android.permission.health.READ_MEDICAL_DATA_PROCEDURES";

    /**
     * Allows an application to read the user's social history data.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public static final String READ_MEDICAL_DATA_SOCIAL_HISTORY =
            "android.permission.health.READ_MEDICAL_DATA_SOCIAL_HISTORY";

    /**
     * Allows an application to read the user's data about immunizations and vaccinations.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public static final String READ_MEDICAL_DATA_VACCINES =
            "android.permission.health.READ_MEDICAL_DATA_VACCINES";

    /**
     * Allows an application to read the user's information about their encounters with health care
     * practitioners, including things like location, time of appointment, and name of organization
     * the visit was with. Despite the name visit it covers remote encounters such as telephone or
     * videoconference appointments.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public static final String READ_MEDICAL_DATA_VISITS =
            "android.permission.health.READ_MEDICAL_DATA_VISITS";

    /**
     * Allows an application to read the user's vital signs data.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public static final String READ_MEDICAL_DATA_VITAL_SIGNS =
            "android.permission.health.READ_MEDICAL_DATA_VITAL_SIGNS";

    /**
     * Allows an application to write the user's medical data.
     *
     * <p>Protection level: dangerous.
     */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public static final String WRITE_MEDICAL_DATA = "android.permission.health.WRITE_MEDICAL_DATA";

    private static final Set<String> sWritePermissionsSet =
            new ArraySet<>(
                    Set.of(
                            WRITE_ACTIVE_CALORIES_BURNED,
                            WRITE_DISTANCE,
                            WRITE_ELEVATION_GAINED,
                            WRITE_EXERCISE,
                            WRITE_FLOORS_CLIMBED,
                            WRITE_STEPS,
                            WRITE_TOTAL_CALORIES_BURNED,
                            WRITE_VO2_MAX,
                            WRITE_WHEELCHAIR_PUSHES,
                            WRITE_POWER,
                            WRITE_SPEED,
                            WRITE_BASAL_METABOLIC_RATE,
                            WRITE_BODY_FAT,
                            WRITE_BODY_WATER_MASS,
                            WRITE_BONE_MASS,
                            WRITE_HEIGHT,
                            WRITE_LEAN_BODY_MASS,
                            WRITE_WEIGHT,
                            WRITE_CERVICAL_MUCUS,
                            WRITE_MENSTRUATION,
                            WRITE_INTERMENSTRUAL_BLEEDING,
                            WRITE_OVULATION_TEST,
                            WRITE_SEXUAL_ACTIVITY,
                            WRITE_HYDRATION,
                            WRITE_NUTRITION,
                            WRITE_SLEEP,
                            WRITE_BASAL_BODY_TEMPERATURE,
                            WRITE_BLOOD_GLUCOSE,
                            WRITE_BLOOD_PRESSURE,
                            WRITE_BODY_TEMPERATURE,
                            WRITE_HEART_RATE,
                            WRITE_HEART_RATE_VARIABILITY,
                            WRITE_OXYGEN_SATURATION,
                            WRITE_RESPIRATORY_RATE,
                            WRITE_RESTING_HEART_RATE,
                            WRITE_SKIN_TEMPERATURE,
                            WRITE_PLANNED_EXERCISE,
                            WRITE_MINDFULNESS));

    private static final Map<String, Integer> sWriteHealthPermissionToHealthDataCategoryMap =
            new ArrayMap<>();
    private static final Map<Integer, String> sHealthCategoryToReadPermissionMap = new ArrayMap<>();
    private static final Map<Integer, String> sHealthCategoryToWritePermissionMap =
            new ArrayMap<>();

    private static final Map<Integer, String[]> sDataCategoryToWritePermissionsMap =
            new ArrayMap<>();

    private HealthPermissions() {}

    /**
     * @return true if {@code permissionName} is a write-permission
     * @hide
     * @deprecated use {@link HealthConnectMappings#isWritePermission(String)}
     */
    @Deprecated
    public static boolean isWritePermission(@NonNull String permissionName) {
        Objects.requireNonNull(permissionName);

        return sWritePermissionsSet.contains(permissionName);
    }

    /**
     * @deprecated Use {@link HealthConnectMappings#getHealthDataCategoryForWritePermission(String)}
     * @return {@link HealthDataCategory} for a WRITE {@code permissionName}. -1 if permission
     *     category for {@code permissionName} is not found (or if {@code permissionName} is READ)
     * @hide
     */
    @Deprecated
    @HealthDataCategory.Type
    public static int getHealthDataCategoryForWritePermission(@Nullable String permissionName) {
        if (sWriteHealthPermissionToHealthDataCategoryMap.isEmpty()) {
            populateWriteHealthPermissionToHealthDataCategoryMap();
        }

        return sWriteHealthPermissionToHealthDataCategoryMap.getOrDefault(
                permissionName, DEFAULT_INT);
    }

    /**
     * @return {@link HealthDataCategory} for {@code permissionName}. -1 if permission category for
     *     {@code permissionName} is not found
     * @deprecated Use {@link HealthConnectMappings#getWriteHealthPermissionsFor(int)}
     * @hide
     */
    @Deprecated
    public static String[] getWriteHealthPermissionsFor(@HealthDataCategory.Type int dataCategory) {
        if (sDataCategoryToWritePermissionsMap.isEmpty()) {
            populateWriteHealthPermissionToHealthDataCategoryMap();
        }

        return sDataCategoryToWritePermissionsMap.getOrDefault(dataCategory, new String[] {});
    }

    /**
     * @deprecated Use {@link HealthConnectMappings#getHealthReadPermission(int)}.
     * @hide
     */
    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    @Deprecated
    public static String getHealthReadPermission(
            @HealthPermissionCategory.Type int permissionCategory) {
        if (sHealthCategoryToReadPermissionMap.isEmpty()) {
            populateHealthPermissionToHealthPermissionCategoryMap();
        }

        return sHealthCategoryToReadPermissionMap.get(permissionCategory);
    }

    /**
     * @deprecated Use {@link HealthConnectMappings#getHealthWritePermission(int)}.
     * @hide
     */
    @Deprecated
    public static String getHealthWritePermission(
            @HealthPermissionCategory.Type int permissionCategory) {
        if (sHealthCategoryToWritePermissionMap.isEmpty()) {
            populateHealthPermissionToHealthPermissionCategoryMap();
        }

        String healthWritePermission = sHealthCategoryToWritePermissionMap.get(permissionCategory);
        Objects.requireNonNull(
                healthWritePermission,
                "Health write permission not found for "
                        + "PermissionCategory : "
                        + permissionCategory);
        return healthWritePermission;
    }

    /**
     * Returns all medical permissions (read and write).
     *
     * @hide
     */
    public static Set<String> getAllMedicalPermissions() {
        if (!isPersonalHealthRecordEnabled()) {
            throw new UnsupportedOperationException("getAllMedicalPermissions is not supported");
        }

        Set<String> permissions = new ArraySet<>();
        permissions.add(WRITE_MEDICAL_DATA);
        permissions.add(READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES);
        permissions.add(READ_MEDICAL_DATA_CONDITIONS);
        permissions.add(READ_MEDICAL_DATA_LABORATORY_RESULTS);
        permissions.add(READ_MEDICAL_DATA_MEDICATIONS);
        permissions.add(READ_MEDICAL_DATA_PERSONAL_DETAILS);
        permissions.add(READ_MEDICAL_DATA_PRACTITIONER_DETAILS);
        permissions.add(READ_MEDICAL_DATA_PREGNANCY);
        permissions.add(READ_MEDICAL_DATA_PROCEDURES);
        permissions.add(READ_MEDICAL_DATA_SOCIAL_HISTORY);
        permissions.add(READ_MEDICAL_DATA_VACCINES);
        permissions.add(READ_MEDICAL_DATA_VISITS);
        permissions.add(READ_MEDICAL_DATA_VITAL_SIGNS);
        return permissions;
    }

    /**
     * Returns a set of dataCategories for which this package has WRITE permissions
     *
     * @hide
     */
    @NonNull
    public static Set<Integer> getDataCategoriesWithWritePermissionsForPackage(
            @NonNull PackageInfo packageInfo, @NonNull Context context) {

        Set<Integer> dataCategoriesWithPermissions = new HashSet<>();

        for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
            String currPerm = packageInfo.requestedPermissions[i];
            if (!HealthConnectManager.isHealthPermission(context, currPerm)) {
                continue;
            }
            if ((packageInfo.requestedPermissionsFlags[i]
                            & PackageInfo.REQUESTED_PERMISSION_GRANTED)
                    == 0) {
                continue;
            }

            int dataCategory = getHealthDataCategoryForWritePermission(currPerm);
            if (dataCategory >= 0) {
                dataCategoriesWithPermissions.add(dataCategory);
            }
        }

        return dataCategoriesWithPermissions;
    }

    /**
     * Returns true if this package has at least one granted WRITE permission for this category.
     *
     * @hide
     */
    public static boolean getPackageHasWriteHealthPermissionsForCategory(
            @NonNull PackageInfo packageInfo,
            @HealthDataCategory.Type int dataCategory,
            @NonNull Context context) {
        return getDataCategoriesWithWritePermissionsForPackage(packageInfo, context)
                .contains(dataCategory);
    }

    /** @hide */
    public static boolean isValidHealthPermission(PermissionInfo permissionInfo) {
        return HEALTH_PERMISSION_GROUP.equals(permissionInfo.group)
                && isPermissionEnabled(permissionInfo.name);
    }

    /** @hide */
    // TODO(b/377285620): flag the permissions in the Manifest when fully supported.
    static boolean isPermissionEnabled(@NonNull String permission) {
        return switch (permission) {
            case READ_ACTIVITY_INTENSITY, WRITE_ACTIVITY_INTENSITY -> Flags.activityIntensity();
            default -> true;
        };
    }

    private static synchronized void populateHealthPermissionToHealthPermissionCategoryMap() {
        if (!sHealthCategoryToWritePermissionMap.isEmpty()) {
            return;
        }

        // Populate permission category to write permission map
        sHealthCategoryToWritePermissionMap.put(
                ACTIVE_CALORIES_BURNED, WRITE_ACTIVE_CALORIES_BURNED);
        sHealthCategoryToWritePermissionMap.put(DISTANCE, WRITE_DISTANCE);
        sHealthCategoryToWritePermissionMap.put(ELEVATION_GAINED, WRITE_ELEVATION_GAINED);
        sHealthCategoryToWritePermissionMap.put(EXERCISE, WRITE_EXERCISE);
        sHealthCategoryToWritePermissionMap.put(FLOORS_CLIMBED, WRITE_FLOORS_CLIMBED);
        sHealthCategoryToWritePermissionMap.put(STEPS, WRITE_STEPS);
        sHealthCategoryToWritePermissionMap.put(TOTAL_CALORIES_BURNED, WRITE_TOTAL_CALORIES_BURNED);
        sHealthCategoryToWritePermissionMap.put(VO2_MAX, WRITE_VO2_MAX);
        sHealthCategoryToWritePermissionMap.put(WHEELCHAIR_PUSHES, WRITE_WHEELCHAIR_PUSHES);
        sHealthCategoryToWritePermissionMap.put(POWER, WRITE_POWER);
        sHealthCategoryToWritePermissionMap.put(SPEED, WRITE_SPEED);
        sHealthCategoryToWritePermissionMap.put(BASAL_METABOLIC_RATE, WRITE_BASAL_METABOLIC_RATE);
        sHealthCategoryToWritePermissionMap.put(BODY_FAT, WRITE_BODY_FAT);
        sHealthCategoryToWritePermissionMap.put(BODY_WATER_MASS, WRITE_BODY_WATER_MASS);
        sHealthCategoryToWritePermissionMap.put(BONE_MASS, WRITE_BONE_MASS);
        sHealthCategoryToWritePermissionMap.put(HEIGHT, WRITE_HEIGHT);
        sHealthCategoryToWritePermissionMap.put(LEAN_BODY_MASS, WRITE_LEAN_BODY_MASS);
        sHealthCategoryToWritePermissionMap.put(WEIGHT, WRITE_WEIGHT);
        sHealthCategoryToWritePermissionMap.put(CERVICAL_MUCUS, WRITE_CERVICAL_MUCUS);
        sHealthCategoryToWritePermissionMap.put(MENSTRUATION, WRITE_MENSTRUATION);
        sHealthCategoryToWritePermissionMap.put(
                INTERMENSTRUAL_BLEEDING, WRITE_INTERMENSTRUAL_BLEEDING);
        sHealthCategoryToWritePermissionMap.put(OVULATION_TEST, WRITE_OVULATION_TEST);
        sHealthCategoryToWritePermissionMap.put(SEXUAL_ACTIVITY, WRITE_SEXUAL_ACTIVITY);
        sHealthCategoryToWritePermissionMap.put(HYDRATION, WRITE_HYDRATION);
        sHealthCategoryToWritePermissionMap.put(NUTRITION, WRITE_NUTRITION);
        sHealthCategoryToWritePermissionMap.put(SLEEP, WRITE_SLEEP);
        sHealthCategoryToWritePermissionMap.put(
                BASAL_BODY_TEMPERATURE, WRITE_BASAL_BODY_TEMPERATURE);
        sHealthCategoryToWritePermissionMap.put(BLOOD_GLUCOSE, WRITE_BLOOD_GLUCOSE);
        sHealthCategoryToWritePermissionMap.put(BLOOD_PRESSURE, WRITE_BLOOD_PRESSURE);
        sHealthCategoryToWritePermissionMap.put(BODY_TEMPERATURE, WRITE_BODY_TEMPERATURE);
        sHealthCategoryToWritePermissionMap.put(HEART_RATE, WRITE_HEART_RATE);
        sHealthCategoryToWritePermissionMap.put(
                HEART_RATE_VARIABILITY, WRITE_HEART_RATE_VARIABILITY);
        sHealthCategoryToWritePermissionMap.put(OXYGEN_SATURATION, WRITE_OXYGEN_SATURATION);
        sHealthCategoryToWritePermissionMap.put(RESPIRATORY_RATE, WRITE_RESPIRATORY_RATE);
        sHealthCategoryToWritePermissionMap.put(RESTING_HEART_RATE, WRITE_RESTING_HEART_RATE);
        sHealthCategoryToWritePermissionMap.put(SKIN_TEMPERATURE, WRITE_SKIN_TEMPERATURE);
        sHealthCategoryToWritePermissionMap.put(PLANNED_EXERCISE, WRITE_PLANNED_EXERCISE);
        sHealthCategoryToWritePermissionMap.put(MINDFULNESS, WRITE_MINDFULNESS);

        // Populate permission category to read permission map
        sHealthCategoryToReadPermissionMap.put(ACTIVE_CALORIES_BURNED, READ_ACTIVE_CALORIES_BURNED);
        sHealthCategoryToReadPermissionMap.put(DISTANCE, READ_DISTANCE);
        sHealthCategoryToReadPermissionMap.put(ELEVATION_GAINED, READ_ELEVATION_GAINED);
        sHealthCategoryToReadPermissionMap.put(EXERCISE, READ_EXERCISE);
        sHealthCategoryToReadPermissionMap.put(FLOORS_CLIMBED, READ_FLOORS_CLIMBED);
        sHealthCategoryToReadPermissionMap.put(STEPS, READ_STEPS);
        sHealthCategoryToReadPermissionMap.put(TOTAL_CALORIES_BURNED, READ_TOTAL_CALORIES_BURNED);
        sHealthCategoryToReadPermissionMap.put(VO2_MAX, READ_VO2_MAX);
        sHealthCategoryToReadPermissionMap.put(WHEELCHAIR_PUSHES, READ_WHEELCHAIR_PUSHES);
        sHealthCategoryToReadPermissionMap.put(POWER, READ_POWER);
        sHealthCategoryToReadPermissionMap.put(SPEED, READ_SPEED);
        sHealthCategoryToReadPermissionMap.put(BASAL_METABOLIC_RATE, READ_BASAL_METABOLIC_RATE);
        sHealthCategoryToReadPermissionMap.put(BODY_FAT, READ_BODY_FAT);
        sHealthCategoryToReadPermissionMap.put(BODY_WATER_MASS, READ_BODY_WATER_MASS);
        sHealthCategoryToReadPermissionMap.put(BONE_MASS, READ_BONE_MASS);
        sHealthCategoryToReadPermissionMap.put(HEIGHT, READ_HEIGHT);
        sHealthCategoryToReadPermissionMap.put(LEAN_BODY_MASS, READ_LEAN_BODY_MASS);
        sHealthCategoryToReadPermissionMap.put(WEIGHT, READ_WEIGHT);
        sHealthCategoryToReadPermissionMap.put(CERVICAL_MUCUS, READ_CERVICAL_MUCUS);
        sHealthCategoryToReadPermissionMap.put(MENSTRUATION, READ_MENSTRUATION);
        sHealthCategoryToReadPermissionMap.put(
                INTERMENSTRUAL_BLEEDING, READ_INTERMENSTRUAL_BLEEDING);
        sHealthCategoryToReadPermissionMap.put(OVULATION_TEST, READ_OVULATION_TEST);
        sHealthCategoryToReadPermissionMap.put(SEXUAL_ACTIVITY, READ_SEXUAL_ACTIVITY);
        sHealthCategoryToReadPermissionMap.put(HYDRATION, READ_HYDRATION);
        sHealthCategoryToReadPermissionMap.put(NUTRITION, READ_NUTRITION);
        sHealthCategoryToReadPermissionMap.put(SLEEP, READ_SLEEP);
        sHealthCategoryToReadPermissionMap.put(BASAL_BODY_TEMPERATURE, READ_BASAL_BODY_TEMPERATURE);
        sHealthCategoryToReadPermissionMap.put(BLOOD_GLUCOSE, READ_BLOOD_GLUCOSE);
        sHealthCategoryToReadPermissionMap.put(BLOOD_PRESSURE, READ_BLOOD_PRESSURE);
        sHealthCategoryToReadPermissionMap.put(BODY_TEMPERATURE, READ_BODY_TEMPERATURE);
        sHealthCategoryToReadPermissionMap.put(HEART_RATE, READ_HEART_RATE);
        sHealthCategoryToReadPermissionMap.put(HEART_RATE_VARIABILITY, READ_HEART_RATE_VARIABILITY);
        sHealthCategoryToReadPermissionMap.put(OXYGEN_SATURATION, READ_OXYGEN_SATURATION);
        sHealthCategoryToReadPermissionMap.put(RESPIRATORY_RATE, READ_RESPIRATORY_RATE);
        sHealthCategoryToReadPermissionMap.put(RESTING_HEART_RATE, READ_RESTING_HEART_RATE);
        sHealthCategoryToReadPermissionMap.put(SKIN_TEMPERATURE, READ_SKIN_TEMPERATURE);
        sHealthCategoryToReadPermissionMap.put(PLANNED_EXERCISE, READ_PLANNED_EXERCISE);
        sHealthCategoryToReadPermissionMap.put(MINDFULNESS, READ_MINDFULNESS);
    }

    private static synchronized void populateWriteHealthPermissionToHealthDataCategoryMap() {
        if (!sWriteHealthPermissionToHealthDataCategoryMap.isEmpty()) {
            return;
        }

        // Write permissions
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_ACTIVE_CALORIES_BURNED, HealthDataCategory.ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_DISTANCE, HealthDataCategory.ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_ELEVATION_GAINED, HealthDataCategory.ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_EXERCISE, HealthDataCategory.ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_PLANNED_EXERCISE, HealthDataCategory.ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_FLOORS_CLIMBED, HealthDataCategory.ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_STEPS, HealthDataCategory.ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_TOTAL_CALORIES_BURNED, HealthDataCategory.ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_VO2_MAX, HealthDataCategory.ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_WHEELCHAIR_PUSHES, HealthDataCategory.ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_POWER, HealthDataCategory.ACTIVITY);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_SPEED, HealthDataCategory.ACTIVITY);

        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_BASAL_METABOLIC_RATE, HealthDataCategory.BODY_MEASUREMENTS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_BODY_FAT, HealthDataCategory.BODY_MEASUREMENTS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_BODY_WATER_MASS, HealthDataCategory.BODY_MEASUREMENTS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_BONE_MASS, HealthDataCategory.BODY_MEASUREMENTS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_HEIGHT, HealthDataCategory.BODY_MEASUREMENTS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_LEAN_BODY_MASS, HealthDataCategory.BODY_MEASUREMENTS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_WEIGHT, HealthDataCategory.BODY_MEASUREMENTS);

        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_CERVICAL_MUCUS, HealthDataCategory.CYCLE_TRACKING);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_MENSTRUATION, HealthDataCategory.CYCLE_TRACKING);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_OVULATION_TEST, HealthDataCategory.CYCLE_TRACKING);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_SEXUAL_ACTIVITY, HealthDataCategory.CYCLE_TRACKING);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_INTERMENSTRUAL_BLEEDING, HealthDataCategory.CYCLE_TRACKING);

        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_HYDRATION, HealthDataCategory.NUTRITION);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_NUTRITION, HealthDataCategory.NUTRITION);
        sWriteHealthPermissionToHealthDataCategoryMap.put(WRITE_SLEEP, HealthDataCategory.SLEEP);

        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_BASAL_BODY_TEMPERATURE, HealthDataCategory.VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_SKIN_TEMPERATURE, HealthDataCategory.VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_BLOOD_GLUCOSE, HealthDataCategory.VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_BLOOD_PRESSURE, HealthDataCategory.VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_BODY_TEMPERATURE, HealthDataCategory.VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_HEART_RATE, HealthDataCategory.VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_HEART_RATE_VARIABILITY, HealthDataCategory.VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_OXYGEN_SATURATION, HealthDataCategory.VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_RESPIRATORY_RATE, HealthDataCategory.VITALS);
        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_RESTING_HEART_RATE, HealthDataCategory.VITALS);

        sWriteHealthPermissionToHealthDataCategoryMap.put(
                WRITE_MINDFULNESS, HealthDataCategory.WELLNESS);

        sDataCategoryToWritePermissionsMap.put(
                HealthDataCategory.ACTIVITY,
                new String[] {
                    WRITE_ACTIVE_CALORIES_BURNED,
                    WRITE_DISTANCE,
                    WRITE_ELEVATION_GAINED,
                    WRITE_EXERCISE,
                    WRITE_PLANNED_EXERCISE,
                    WRITE_FLOORS_CLIMBED,
                    WRITE_STEPS,
                    WRITE_TOTAL_CALORIES_BURNED,
                    WRITE_VO2_MAX,
                    WRITE_WHEELCHAIR_PUSHES,
                    WRITE_POWER,
                    WRITE_SPEED
                });

        sDataCategoryToWritePermissionsMap.put(
                HealthDataCategory.BODY_MEASUREMENTS,
                new String[] {
                    WRITE_BASAL_METABOLIC_RATE,
                    WRITE_BODY_FAT,
                    WRITE_BODY_WATER_MASS,
                    WRITE_BONE_MASS,
                    WRITE_HEIGHT,
                    WRITE_LEAN_BODY_MASS,
                    WRITE_WEIGHT
                });

        sDataCategoryToWritePermissionsMap.put(
                HealthDataCategory.CYCLE_TRACKING,
                new String[] {
                    WRITE_CERVICAL_MUCUS,
                    WRITE_MENSTRUATION,
                    WRITE_OVULATION_TEST,
                    WRITE_SEXUAL_ACTIVITY,
                    WRITE_INTERMENSTRUAL_BLEEDING
                });

        sDataCategoryToWritePermissionsMap.put(
                HealthDataCategory.NUTRITION, new String[] {WRITE_HYDRATION, WRITE_NUTRITION});

        sDataCategoryToWritePermissionsMap.put(
                HealthDataCategory.SLEEP, new String[] {WRITE_SLEEP});

        sDataCategoryToWritePermissionsMap.put(
                HealthDataCategory.VITALS,
                new String[] {
                    WRITE_BASAL_BODY_TEMPERATURE,
                    WRITE_BLOOD_GLUCOSE,
                    WRITE_BLOOD_PRESSURE,
                    WRITE_BODY_TEMPERATURE,
                    WRITE_HEART_RATE,
                    WRITE_HEART_RATE_VARIABILITY,
                    WRITE_OXYGEN_SATURATION,
                    WRITE_RESPIRATORY_RATE,
                    WRITE_RESTING_HEART_RATE,
                    WRITE_SKIN_TEMPERATURE
                });

        if (Flags.mindfulness()) {
            sDataCategoryToWritePermissionsMap.put(
                    HealthDataCategory.WELLNESS, new String[] {WRITE_MINDFULNESS});
        }
    }
}
