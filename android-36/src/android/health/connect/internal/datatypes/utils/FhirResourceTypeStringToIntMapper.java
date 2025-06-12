/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.android.healthfitness.flags.AconfigFlagHelper.isPersonalHealthRecordEnabled;

import android.annotation.NonNull;
import android.health.connect.datatypes.FhirResource;
import android.health.connect.datatypes.FhirResource.FhirResourceType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** @hide */
public final class FhirResourceTypeStringToIntMapper {
    private static final Map<String, Integer> sFhirResourceTypeStringToIntMap = new HashMap<>();

    private static final String FHIR_RESOURCE_TYPE_IMMUNIZATION_STR = "IMMUNIZATION";
    private static final String FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE_STR = "ALLERGYINTOLERANCE";
    private static final String FHIR_RESOURCE_TYPE_OBSERVATION_STR = "OBSERVATION";
    private static final String FHIR_RESOURCE_TYPE_CONDITION_STR = "CONDITION";
    private static final String FHIR_RESOURCE_TYPE_PROCEDURE_STR = "PROCEDURE";
    private static final String FHIR_RESOURCE_TYPE_MEDICATION_STR = "MEDICATION";
    private static final String FHIR_RESOURCE_TYPE_MEDICATION_REQUEST_STR = "MEDICATIONREQUEST";
    private static final String FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT_STR = "MEDICATIONSTATEMENT";
    private static final String FHIR_RESOURCE_TYPE_PATIENT_STR = "PATIENT";
    private static final String FHIR_RESOURCE_TYPE_PRACTITIONER_STR = "PRACTITIONER";
    private static final String FHIR_RESOURCE_TYPE_PRACTITIONER_ROLE_STR = "PRACTITIONERROLE";
    private static final String FHIR_RESOURCE_TYPE_ENCOUNTER_STR = "ENCOUNTER";
    private static final String FHIR_RESOURCE_TYPE_LOCATION_STR = "LOCATION";
    private static final String FHIR_RESOURCE_TYPE_ORGANIZATION_STR = "ORGANIZATION";

    /**
     * Returns the corresponding {@code IntDef} {@link FhirResourceType} from a {@code String}
     * {@code fhirResourceType}.
     *
     * @throws IllegalArgumentException if the type is not supported.
     */
    @FhirResourceType
    public static int getFhirResourceTypeInt(@NonNull String fhirResourceType) {
        if (!isPersonalHealthRecordEnabled()) {
            throw new UnsupportedOperationException("getFhirResourceTypeInt is not supported");
        }

        populateFhirResourceTypeStringToIntMap();

        Integer fhirResourceTypeInt =
                sFhirResourceTypeStringToIntMap.get(fhirResourceType.toUpperCase(Locale.ROOT));
        if (fhirResourceTypeInt == null) {
            throw new IllegalArgumentException(
                    "Unsupported FHIR resource type: " + fhirResourceType);
        }

        return fhirResourceTypeInt;
    }

    @SuppressWarnings("FlaggedApi") // Initial if statement checks flag, but lint can't know that
    private static void populateFhirResourceTypeStringToIntMap() {
        if (!isPersonalHealthRecordEnabled()) {
            throw new UnsupportedOperationException(
                    "populateFhirResourceTypeStringToIntMap is not supported");
        }

        if (!sFhirResourceTypeStringToIntMap.isEmpty()) {
            return;
        }

        sFhirResourceTypeStringToIntMap.put(
                FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE_STR,
                FhirResource.FHIR_RESOURCE_TYPE_ALLERGY_INTOLERANCE);
        sFhirResourceTypeStringToIntMap.put(
                FHIR_RESOURCE_TYPE_IMMUNIZATION_STR, FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION);
        sFhirResourceTypeStringToIntMap.put(
                FHIR_RESOURCE_TYPE_OBSERVATION_STR, FhirResource.FHIR_RESOURCE_TYPE_OBSERVATION);
        sFhirResourceTypeStringToIntMap.put(
                FHIR_RESOURCE_TYPE_CONDITION_STR, FhirResource.FHIR_RESOURCE_TYPE_CONDITION);
        sFhirResourceTypeStringToIntMap.put(
                FHIR_RESOURCE_TYPE_PROCEDURE_STR, FhirResource.FHIR_RESOURCE_TYPE_PROCEDURE);
        sFhirResourceTypeStringToIntMap.put(
                FHIR_RESOURCE_TYPE_MEDICATION_STR, FhirResource.FHIR_RESOURCE_TYPE_MEDICATION);
        sFhirResourceTypeStringToIntMap.put(
                FHIR_RESOURCE_TYPE_MEDICATION_REQUEST_STR,
                FhirResource.FHIR_RESOURCE_TYPE_MEDICATION_REQUEST);
        sFhirResourceTypeStringToIntMap.put(
                FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT_STR,
                FhirResource.FHIR_RESOURCE_TYPE_MEDICATION_STATEMENT);
        sFhirResourceTypeStringToIntMap.put(
                FHIR_RESOURCE_TYPE_PATIENT_STR, FhirResource.FHIR_RESOURCE_TYPE_PATIENT);
        sFhirResourceTypeStringToIntMap.put(
                FHIR_RESOURCE_TYPE_PRACTITIONER_STR, FhirResource.FHIR_RESOURCE_TYPE_PRACTITIONER);
        sFhirResourceTypeStringToIntMap.put(
                FHIR_RESOURCE_TYPE_PRACTITIONER_ROLE_STR,
                FhirResource.FHIR_RESOURCE_TYPE_PRACTITIONER_ROLE);
        sFhirResourceTypeStringToIntMap.put(
                FHIR_RESOURCE_TYPE_ENCOUNTER_STR, FhirResource.FHIR_RESOURCE_TYPE_ENCOUNTER);
        sFhirResourceTypeStringToIntMap.put(
                FHIR_RESOURCE_TYPE_LOCATION_STR, FhirResource.FHIR_RESOURCE_TYPE_LOCATION);
        sFhirResourceTypeStringToIntMap.put(
                FHIR_RESOURCE_TYPE_ORGANIZATION_STR, FhirResource.FHIR_RESOURCE_TYPE_ORGANIZATION);
    }
}
