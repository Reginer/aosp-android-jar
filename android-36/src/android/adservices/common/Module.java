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

package android.adservices.common;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;

import com.android.adservices.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents AdServices Module type.
 *
 * <p>This class is used to identify the adservices feature that we want to turn on/off or set user
 * consent.
 *
 * @hide
 */
@FlaggedApi(Flags.FLAG_ADSERVICES_ENABLE_PER_MODULE_OVERRIDES_API)
public final class Module {

    /** Measurement module. */
    public static final int MEASUREMENT = AdServicesCommonManager.MODULE_MEASUREMENT;

    /** Privacy Sandbox module. */
    public static final int PROTECTED_AUDIENCE = AdServicesCommonManager.MODULE_PROTECTED_AUDIENCE;

    /** Privacy Sandbox Attribution module. */
    public static final int PROTECTED_APP_SIGNALS =
            AdServicesCommonManager.MODULE_PROTECTED_APP_SIGNALS;

    /** Topics module. */
    public static final int TOPICS = AdServicesCommonManager.MODULE_TOPICS;

    /** On-device Personalization(ODP) module. */
    public static final int ON_DEVICE_PERSONALIZATION =
            AdServicesCommonManager.MODULE_ON_DEVICE_PERSONALIZATION;

    /** ADID module. */
    public static final int ADID = AdServicesCommonManager.MODULE_ADID;

    /** Default Contractor, make it private so that it won't show in the system-current.txt */
    private Module() {}

    /**
     * ModuleCode IntDef.
     *
     * @hide
     */
    @IntDef(
            value = {
                MEASUREMENT,
                PROTECTED_AUDIENCE,
                PROTECTED_APP_SIGNALS,
                TOPICS,
                ON_DEVICE_PERSONALIZATION,
                ADID
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ModuleCode {}

    /**
     * Validates a module.
     *
     * @param module module to validate
     * @return module
     */
    @ModuleCode
    static int validate(@ModuleCode int module) {
        return switch (module) {
            case ADID,
                            MEASUREMENT,
                            ON_DEVICE_PERSONALIZATION,
                            PROTECTED_APP_SIGNALS,
                            PROTECTED_AUDIENCE,
                            TOPICS ->
                    module;
            default -> throw new IllegalArgumentException("Invalid Module Code:" + module);
        };
    }
}
