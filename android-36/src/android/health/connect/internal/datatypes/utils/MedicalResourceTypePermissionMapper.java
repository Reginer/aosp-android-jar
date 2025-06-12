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

package android.health.connect.internal.datatypes.utils;

import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_CONDITIONS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_MEDICATIONS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PREGNANCY;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PROCEDURES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VISITS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VITAL_SIGNS;

import static com.android.healthfitness.flags.AconfigFlagHelper.isPersonalHealthRecordEnabled;

import android.annotation.NonNull;
import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.MedicalResource.MedicalResourceType;
import android.util.ArrayMap;

import java.util.Map;

/** @hide */
public final class MedicalResourceTypePermissionMapper {

    private static final Map<Integer, String> sMedicalResourceTypeToReadPermissionMap =
            new ArrayMap<>();
    private static final Map<String, Integer> sMedicalResourceReadPermissionToTypeMap =
            new ArrayMap<>();

    private MedicalResourceTypePermissionMapper() {}

    private static synchronized void populateMedicalResourceTypeAndReadPermissionMaps() {
        if (!isPersonalHealthRecordEnabled()) {
            throw new UnsupportedOperationException(
                    "populateMedicalResourceTypeToPermissionMap is not supported");
        }

        if (!sMedicalResourceTypeToReadPermissionMap.isEmpty()) {
            return;
        }

        // Populate sMedicalResourceTypeToReadPermissionMap.
        sMedicalResourceTypeToReadPermissionMap.put(
                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES);
        sMedicalResourceTypeToReadPermissionMap.put(
                MEDICAL_RESOURCE_TYPE_CONDITIONS, HealthPermissions.READ_MEDICAL_DATA_CONDITIONS);
        sMedicalResourceTypeToReadPermissionMap.put(
                MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS,
                HealthPermissions.READ_MEDICAL_DATA_LABORATORY_RESULTS);
        sMedicalResourceTypeToReadPermissionMap.put(
                MEDICAL_RESOURCE_TYPE_MEDICATIONS, HealthPermissions.READ_MEDICAL_DATA_MEDICATIONS);
        sMedicalResourceTypeToReadPermissionMap.put(
                MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS,
                HealthPermissions.READ_MEDICAL_DATA_PERSONAL_DETAILS);
        sMedicalResourceTypeToReadPermissionMap.put(
                MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS,
                HealthPermissions.READ_MEDICAL_DATA_PRACTITIONER_DETAILS);
        sMedicalResourceTypeToReadPermissionMap.put(
                MEDICAL_RESOURCE_TYPE_PREGNANCY, HealthPermissions.READ_MEDICAL_DATA_PREGNANCY);
        sMedicalResourceTypeToReadPermissionMap.put(
                MEDICAL_RESOURCE_TYPE_PROCEDURES, HealthPermissions.READ_MEDICAL_DATA_PROCEDURES);
        sMedicalResourceTypeToReadPermissionMap.put(
                MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY,
                HealthPermissions.READ_MEDICAL_DATA_SOCIAL_HISTORY);
        sMedicalResourceTypeToReadPermissionMap.put(
                MEDICAL_RESOURCE_TYPE_VACCINES, HealthPermissions.READ_MEDICAL_DATA_VACCINES);
        sMedicalResourceTypeToReadPermissionMap.put(
                MEDICAL_RESOURCE_TYPE_VISITS, HealthPermissions.READ_MEDICAL_DATA_VISITS);
        sMedicalResourceTypeToReadPermissionMap.put(
                MEDICAL_RESOURCE_TYPE_VITAL_SIGNS, HealthPermissions.READ_MEDICAL_DATA_VITAL_SIGNS);

        // Populate sMedicalResourceTypeToReadPermissionMap.
        sMedicalResourceTypeToReadPermissionMap.forEach(
                (key, value) -> {
                    sMedicalResourceReadPermissionToTypeMap.put(value, key);
                });
    }

    /**
     * Returns {@link HealthPermissions} for the input {@link MedicalResourceType}.
     *
     * @throws IllegalArgumentException if there is no read permission associated to the given
     *     {@link MedicalResourceType}.
     */
    public static String getMedicalReadPermission(@MedicalResourceType int resourceType) {
        populateMedicalResourceTypeAndReadPermissionMaps();

        String medicalReadPermission = sMedicalResourceTypeToReadPermissionMap.get(resourceType);
        if (medicalReadPermission != null) {
            return medicalReadPermission;
        }

        throw new IllegalArgumentException(
                "Medical read permission not found for the Medical Resource Type: " + resourceType);
    }

    /**
     * Returns {@link MedicalResourceType} for the input {@code readPermission}.
     *
     * @throws IllegalArgumentException if there is no {@link MedicalResourceType} associated to the
     *     given read permission.
     */
    @MedicalResourceType
    public static int getMedicalResourceType(@NonNull String readPermission) {
        populateMedicalResourceTypeAndReadPermissionMaps();

        if (sMedicalResourceReadPermissionToTypeMap.containsKey(readPermission)) {
            return sMedicalResourceReadPermissionToTypeMap.get(readPermission);
        }

        throw new IllegalArgumentException("Medical Resource Type not found for " + readPermission);
    }
}
