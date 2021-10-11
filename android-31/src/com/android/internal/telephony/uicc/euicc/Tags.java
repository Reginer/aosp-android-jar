/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.internal.telephony.uicc.euicc;

/**
 * ASN1 tags used by {@link EuiccCard} implementation.
 */
class Tags {
    // ASN.1 tags for commands
    static final int TAG_GET_PROFILES = 0xBF2D;
    static final int TAG_DISABLE_PROFILE = 0xBF32;
    static final int TAG_ENABLE_PROFILE = 0xBF31;
    static final int TAG_GET_EID = 0xBF3E;
    static final int TAG_SET_NICKNAME = 0xBF29;
    static final int TAG_DELETE_PROFILE = 0xBF33;
    static final int TAG_GET_CONFIGURED_ADDRESSES = 0xBF3C;
    static final int TAG_SET_DEFAULT_SMDP_ADDRESS = 0xBF3F;
    static final int TAG_GET_RAT = 0xBF43;
    static final int TAG_EUICC_MEMORY_RESET = 0xBF34;
    static final int TAG_GET_EUICC_CHALLENGE = 0xBF2E;
    static final int TAG_GET_EUICC_INFO_1 = 0xBF20;
    static final int TAG_GET_EUICC_INFO_2 = 0xBF22;
    static final int TAG_LIST_NOTIFICATION = 0xBF28;
    static final int TAG_RETRIEVE_NOTIFICATIONS_LIST = 0xBF2B;
    static final int TAG_REMOVE_NOTIFICATION_FROM_LIST = 0xBF30;
    static final int TAG_AUTHENTICATE_SERVER = 0xBF38;
    static final int TAG_PREPARE_DOWNLOAD = 0xBF21;
    static final int TAG_INITIALISE_SECURE_CHANNEL = 0xBF23;

    // Universal tags
    static final int TAG_UNI_2 = 0x02;
    static final int TAG_UNI_4 = 0x04;
    static final int TAG_SEQUENCE = 0x30;
    // Context tags for primitive types
    static final int TAG_CTX_0 = 0x80;
    static final int TAG_CTX_1 = 0x81;
    static final int TAG_CTX_2 = 0x82;
    static final int TAG_CTX_3 = 0x83;
    static final int TAG_CTX_4 = 0x84;
    static final int TAG_CTX_5 = 0x85;
    static final int TAG_CTX_6 = 0x86;
    static final int TAG_CTX_7 = 0x87;
    static final int TAG_CTX_8 = 0x88;
    static final int TAG_CTX_9 = 0x89;
    static final int TAG_CTX_10 = 0x8A;

    // Context tags for constructed (compound) types
    static final int TAG_CTX_COMP_0 = 0xA0;
    static final int TAG_CTX_COMP_1 = 0xA1;
    static final int TAG_CTX_COMP_2 = 0xA2;
    static final int TAG_CTX_COMP_3 = 0xA3;

    // Command data tags
    static final int TAG_PROFILE_INSTALLATION_RESULT = 0xBF37;
    static final int TAG_PROFILE_INSTALLATION_RESULT_DATA = 0xBF27;
    static final int TAG_NOTIFICATION_METADATA = 0xBF2F;
    static final int TAG_SEQ = TAG_CTX_0;
    static final int TAG_TARGET_ADDR = 0x0C;
    static final int TAG_EVENT = TAG_CTX_1;
    static final int TAG_CANCEL_SESSION = 0xBF41;
    static final int TAG_PROFILE_INFO = 0xE3;
    static final int TAG_TAG_LIST = 0x5C;
    static final int TAG_EID = 0x5A;
    static final int TAG_NICKNAME = 0x90;
    static final int TAG_ICCID = 0x5A;
    static final int TAG_PROFILE_STATE = 0x9F70;
    static final int TAG_SERVICE_PROVIDER_NAME = 0x91;
    static final int TAG_PROFILE_CLASS = 0x95;
    static final int TAG_PROFILE_POLICY_RULE = 0x99;
    static final int TAG_PROFILE_NAME = 0x92;
    static final int TAG_OPERATOR_ID = 0xB7;
    static final int TAG_CARRIER_PRIVILEGE_RULES = 0xBF76;

    // Tags from the RefArDo data standard - https://source.android.com/devices/tech/config/uicc
    static final int TAG_REF_AR_DO = 0xE2;
    static final int TAG_REF_DO = 0xE1;
    static final int TAG_DEVICE_APP_ID_REF_DO = 0xC1;
    static final int TAG_PKG_REF_DO = 0xCA;
    static final int TAG_AR_DO = 0xE3;
    static final int TAG_PERM_AR_DO = 0xDB;

    // TAG list for Euicc Profile
    static final byte[] EUICC_PROFILE_TAGS = new byte[] {
            TAG_ICCID,
            (byte) TAG_NICKNAME,
            (byte) TAG_SERVICE_PROVIDER_NAME,
            (byte) TAG_PROFILE_NAME,
            (byte) TAG_OPERATOR_ID,
            (byte) (TAG_PROFILE_STATE / 256),
            (byte) (TAG_PROFILE_STATE % 256),
            (byte) TAG_PROFILE_CLASS,
            (byte) TAG_PROFILE_POLICY_RULE,
            (byte) (TAG_CARRIER_PRIVILEGE_RULES / 256),
            (byte) (TAG_CARRIER_PRIVILEGE_RULES % 256),
    };

    private Tags() {}
}
