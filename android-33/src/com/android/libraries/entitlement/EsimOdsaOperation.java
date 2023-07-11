/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.libraries.entitlement;

import com.google.auto.value.AutoValue;

/**
 * HTTP request parameters specific to on device service actiavation (ODSA). See GSMA spec TS.43
 * section 6.2.
 */
@AutoValue
public abstract class EsimOdsaOperation {
    /**
     * OSDA operation: CheckEligibility.
     */
    public static final String OPERATION_CHECK_ELIGIBILITY = "CheckEligibility";
    /**
     * OSDA operation: ManageSubscription.
     */
    public static final String OPERATION_MANAGE_SUBSCRIPTION = "ManageSubscription";
    /**
     * OSDA operation: ManageService.
     */
    public static final String OPERATION_MANAGE_SERVICE = "ManageService";
    /**
     * OSDA operation: AcquireConfiguration.
     */
    public static final String OPERATION_ACQUIRE_CONFIGURATION = "AcquireConfiguration";

    /**
     * Indicates that operation_type is not set.
     */
    public static final int OPERATION_TYPE_NOT_SET = -1;
    /**
     * To activate a subscription, used by {@link #OPERATION_MANAGE_SUBSCRIPTION}.
     */
    public static final int OPERATION_TYPE_SUBSCRIBE = 0;
    /**
     * To cancel a subscription, used by {@link #OPERATION_MANAGE_SUBSCRIPTION}.
     */
    public static final int OPERATION_TYPE_UNSUBSCRIBE = 1;
    /**
     * To manage an existing subscription, for {@link #OPERATION_MANAGE_SUBSCRIPTION}.
     */
    public static final int OPERATION_TYPE_CHANGE_SUBSCRIPTION = 2;
    /**
     * To transfer a subscription from an existing device, used by {@link
     * #OPERATION_MANAGE_SUBSCRIPTION}.
     */
    public static final int OPERATION_TYPE_TRANSFER_SUBSCRIPTION = 3;
    /**
     * To inform the network of a subscription update, used by
     * {@link #OPERATION_MANAGE_SUBSCRIPTION}.
     */
    public static final int OPERATION_TYPE_UPDATE_SUBSCRIPTION = 4;
    /**
     * To activate a service, used by {@link #OPERATION_MANAGE_SERVICE}.
     */
    public static final int OPERATION_TYPE_ACTIVATE_SERVICE = 10;
    /**
     * To deactivate a service, used by {@link #OPERATION_MANAGE_SERVICE}.
     */
    public static final int OPERATION_TYPE_DEACTIVATE_SERVICE = 11;

    /**
     * Indicates the companion device carries the same MSISDN as the primary device.
     */
    public static final String COMPANION_SERVICE_SHAERED_NUMBER = "SharedNumber";
    /**
     * Indicates the companion device carries a different MSISDN as the primary device.
     */
    public static final String COMPANION_SERVICE_DIFFERENT_NUMBER = "DiffNumber";

    /**
     * Returns the eSIM ODSA operation. Used by HTTP parameter "operation".
     */
    public abstract String operation();

    /**
     * Returns the detiled type of the eSIM ODSA operation. Used by HTTP parameter
     * "operation_type".
     */
    public abstract int operationType();

    /**
     * Returns the unique identifier of the companion device, like IMEI. Used by HTTP parameter
     * "companion_terminal_id".
     */
    public abstract String companionTerminalId();

    /**
     * Returns the OEM of the companion device. Used by HTTP parameter "companion_terminal_vendor".
     */
    public abstract String companionTerminalVendor();

    /**
     * Returns the model of the companion device. Used by HTTP parameter
     * "companion_terminal_model".
     */
    public abstract String companionTerminalModel();

    /**
     * Returns the software version of the companion device. Used by HTTP parameter
     * "companion_terminal_sw_version".
     */
    public abstract String companionTerminalSoftwareVersion();

    /**
     * Returns the user-friendly version of the companion device. Used by HTTP parameter
     * "companion_terminal_friendly_name".
     */
    public abstract String companionTerminalFriendlyName();

    /**
     * Returns the service type of the companion device, e.g. if the MSISDN is same as the primary
     * device. Used by HTTP parameter "companion_terminal_service".
     */
    public abstract String companionTerminalService();

    /**
     * Returns the ICCID of the companion device. Used by HTTP parameter
     * "companion_terminal_iccid".
     */
    public abstract String companionTerminalIccid();

    /**
     * Returns the EID of the companion device. Used by HTTP parameter "companion_terminal_eid".
     */
    public abstract String companionTerminalEid();

    /**
     * Returns the ICCID of the primary device eSIM. Used by HTTP parameter "terminal_iccid".
     */
    public abstract String terminalIccid();

    /**
     * Returns the eUICC identifier (EID) of the primary device eSIM. Used by HTTP parameter
     * "terminal_eid".
     */
    public abstract String terminalEid();

    /**
     * Returns the unique identifier of the primary device eSIM, like the IMEI associated with the
     * eSIM. Used by HTTP parameter "target_terminal_id".
     */
    public abstract String targetTerminalId();

    /**
     * Returns the ICCID primary device eSIM. Used by HTTP parameter "target_terminal_iccid".
     */
    public abstract String targetTerminalIccid();

    /**
     * Returns the eUICC identifier (EID) of the primary device eSIM. Used by HTTP parameter
     * "target_terminal_eid".
     */
    public abstract String targetTerminalEid();

    /**
     * Returns a new {@link Builder} object.
     */
    public static Builder builder() {
        return new AutoValue_EsimOdsaOperation.Builder()
                .setOperation("")
                .setOperationType(OPERATION_TYPE_NOT_SET)
                .setCompanionTerminalId("")
                .setCompanionTerminalVendor("")
                .setCompanionTerminalModel("")
                .setCompanionTerminalSoftwareVersion("")
                .setCompanionTerminalFriendlyName("")
                .setCompanionTerminalService("")
                .setCompanionTerminalIccid("")
                .setCompanionTerminalEid("")
                .setTerminalIccid("")
                .setTerminalEid("")
                .setTargetTerminalId("")
                .setTargetTerminalIccid("")
                .setTargetTerminalEid("");
    }

    /**
     * Builder.
     *
     * <p>For ODSA, the rule of which parameters are required varies or each
     * operation/opeation_type. The Javadoc below gives high-level description, but please refer to
     * GMSA spec TS.43 section 6.2 for details.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * Sets the eSIM ODSA operation. Used by HTTP parameter "operation".
         *
         * <p>Required.
         *
         * @see #OPERATION_CHECK_ELIGIBILITY
         * @see #OPERATION_MANAGE_SUBSCRIPTION
         * @see #OPERATION_MANAGE_SERVICE
         * @see #OPERATION_ACQUIRE_CONFIGURATION
         */
        public abstract Builder setOperation(String value);

        /**
         * Sets the detiled type of the eSIM ODSA operation. Used by HTTP parameter "operation_type"
         * if set.
         *
         * <p>Required by some operation.
         *
         * @see #OPERATION_TYPE_SUBSCRIBE
         * @see #OPERATION_TYPE_UNSUBSCRIBE
         * @see #OPERATION_TYPE_CHANGE_SUBSCRIPTION
         * @see #OPERATION_TYPE_TRANSFER_SUBSCRIPTION
         * @see #OPERATION_TYPE_UPDATE_SUBSCRIPTION
         * @see #OPERATION_TYPE_ACTIVATE_SERVICE
         * @see #OPERATION_TYPE_DEACTIVATE_SERVICE
         */
        public abstract Builder setOperationType(int value);

        /**
         * Sets the unique identifier of the companion device, like IMEI. Used by HTTP parameter
         * "companion_terminal_id" if set.
         *
         * <p>Used by companion device ODSA operation.
         */
        public abstract Builder setCompanionTerminalId(String value);

        /**
         * Sets the OEM of the companion device. Used by HTTP parameter "companion_terminal_vendor"
         * if set.
         *
         * <p>Used by companion device ODSA operation.
         */
        public abstract Builder setCompanionTerminalVendor(String value);

        /**
         * Sets the model of the companion device. Used by HTTP parameter "companion_terminal_model"
         * if set.
         *
         * <p>Used by companion device ODSA operation.
         */
        public abstract Builder setCompanionTerminalModel(String value);

        /**
         * Sets the software version of the companion device. Used by HTTP parameter
         * "companion_terminal_sw_version" if set.
         *
         * <p>Used by companion device ODSA operation.
         */
        public abstract Builder setCompanionTerminalSoftwareVersion(String value);

        /**
         * Sets the user-friendly version of the companion device. Used by HTTP parameter
         * "companion_terminal_friendly_name" if set.
         *
         * <p>Used by companion device ODSA operation.
         */
        public abstract Builder setCompanionTerminalFriendlyName(String value);

        /**
         * Sets the service type of the companion device, e.g. if the MSISDN is same as the primary
         * device. Used by HTTP parameter "companion_terminal_service" if set.
         *
         * <p>Used by companion device ODSA operation.
         *
         * @see #COMPANION_SERVICE_SHAERED_NUMBER
         * @see #COMPANION_SERVICE_DIFFERENT_NUMBER
         */
        public abstract Builder setCompanionTerminalService(String value);

        /**
         * Sets the ICCID of the companion device. Used by HTTP parameter "companion_terminal_iccid"
         * if set.
         *
         * <p>Used by companion device ODSA operation.
         */
        public abstract Builder setCompanionTerminalIccid(String value);

        /**
         * Sets the eUICC identifier (EID) of the companion device. Used by HTTP parameter
         * "companion_terminal_eid" if set.
         *
         * <p>Used by companion device ODSA operation.
         */
        public abstract Builder setCompanionTerminalEid(String value);

        /**
         * Sets the ICCID of the primary device eSIM in case of primary SIM not present. Used by
         * HTTP parameter "terminal_eid" if set.
         *
         * <p>Used by primary device ODSA operation.
         */
        public abstract Builder setTerminalIccid(String value);

        /**
         * Sets the eUICC identifier (EID) of the primary device eSIM in case of primary SIM not
         * present. Used by HTTP parameter "terminal_eid" if set.
         *
         * <p>Used by primary device ODSA operation.
         */
        public abstract Builder setTerminalEid(String value);

        /**
         * Sets the unique identifier of the primary device eSIM in case of multiple SIM, like the
         * IMEI associated with the eSIM. Used by HTTP parameter "target_terminal_id" if set.
         *
         * <p>Used by primary device ODSA operation.
         */
        public abstract Builder setTargetTerminalId(String value);

        /**
         * Sets the ICCID primary device eSIM in case of multiple SIM. Used by HTTP parameter
         * "target_terminal_iccid" if set.
         *
         * <p>Used by primary device ODSA operation.
         */
        public abstract Builder setTargetTerminalIccid(String value);

        /**
         * Sets the eUICC identifier (EID) of the primary device eSIM in case of multiple SIM. Used
         * by HTTP parameter "target_terminal_eid" if set.
         *
         * <p>Used by primary device ODSA operation.
         */
        public abstract Builder setTargetTerminalEid(String value);

        public abstract EsimOdsaOperation build();
    }
}
