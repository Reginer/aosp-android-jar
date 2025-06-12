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
package com.android.internal.net.ipsec.ike.utils;

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Utility class for logging of IKE metrics. */
public class IkeMetrics {

    /**
     * Values for Caller from IkeCaller enum proto
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"IKE_CALLER_"},
            value = {
                IKE_CALLER_UNKNOWN,
                IKE_CALLER_IWLAN,
                IKE_CALLER_VCN,
                IKE_CALLER_VPN,
            })
    public @interface IkeCaller {}

    /** @hide */
    public static final int IKE_CALLER_UNKNOWN =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_CALLER__CALLER_UNKNOWN;
    /** @hide */
    public static final int IKE_CALLER_IWLAN =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_CALLER__CALLER_IWLAN;
    /** @hide */
    public static final int IKE_CALLER_VCN =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_CALLER__CALLER_VCN;
    /** @hide */
    public static final int IKE_CALLER_VPN =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_CALLER__CALLER_VPN;

    /**
     * Values for SessionType from SessionType enum proto
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"IKE_SESSION_TYPE_"},
            value = {
                IKE_SESSION_TYPE_UNKNOWN,
                IKE_SESSION_TYPE_IKE,
                IKE_SESSION_TYPE_CHILD,
            })
    public @interface IkeSessionType {}

    /** @hide */
    public static final int IKE_SESSION_TYPE_UNKNOWN =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__SESSION_TYPE__SESSION_UNKNOWN;
    /** @hide */
    public static final int IKE_SESSION_TYPE_IKE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__SESSION_TYPE__SESSION_IKE;
    /** @hide */
    public static final int IKE_SESSION_TYPE_CHILD =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__SESSION_TYPE__SESSION_CHILD;

    /**
     * Values for DhGroups from DhGroups enum proto
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"DH_GROUP_"},
            value = {
                DH_GROUP_UNSPECIFIED,
                DH_GROUP_NONE,
                DH_GROUP_1024_BIT_MODP,
                DH_GROUP_1536_BIT_MODP,
                DH_GROUP_2048_BIT_MODP,
                DH_GROUP_3072_BIT_MODP,
                DH_GROUP_4096_BIT_MODP,
                DH_GROUP_CURVE_25519,
            })
    public @interface DhGroups {}

    /** @hide */
    public static final int DH_GROUP_UNSPECIFIED =
            IkeMetricsInterface.NEGOTIATED_SECURITY_ASSOCIATION__DH_GROUP__DH_GROUP_UNSPECIFIED;

    /** @hide */
    public static final int DH_GROUP_NONE =
            IkeMetricsInterface.NEGOTIATED_SECURITY_ASSOCIATION__DH_GROUP__DH_GROUP_NONE;

    /** @hide */
    public static final int DH_GROUP_1024_BIT_MODP =
            IkeMetricsInterface.NEGOTIATED_SECURITY_ASSOCIATION__DH_GROUP__DH_GROUP_1024_BIT_MODP;

    /** @hide */
    public static final int DH_GROUP_1536_BIT_MODP =
            IkeMetricsInterface.NEGOTIATED_SECURITY_ASSOCIATION__DH_GROUP__DH_GROUP_1536_BIT_MODP;

    /** @hide */
    public static final int DH_GROUP_2048_BIT_MODP =
            IkeMetricsInterface.NEGOTIATED_SECURITY_ASSOCIATION__DH_GROUP__DH_GROUP_2048_BIT_MODP;

    /** @hide */
    public static final int DH_GROUP_3072_BIT_MODP =
            IkeMetricsInterface.NEGOTIATED_SECURITY_ASSOCIATION__DH_GROUP__DH_GROUP_3072_BIT_MODP;

    /** @hide */
    public static final int DH_GROUP_4096_BIT_MODP =
            IkeMetricsInterface.NEGOTIATED_SECURITY_ASSOCIATION__DH_GROUP__DH_GROUP_4096_BIT_MODP;

    /** @hide */
    public static final int DH_GROUP_CURVE_25519 =
            IkeMetricsInterface.NEGOTIATED_SECURITY_ASSOCIATION__DH_GROUP__DH_GROUP_CURVE_25519;

    /**
     * Values for IntegrityAlgorithms from IntegrityAlgorithms enum proto
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"INTEGRITY_ALGORITHM_"},
            value = {
                INTEGRITY_ALGORITHM_UNSPECIFIED,
                INTEGRITY_ALGORITHM_NONE,
                INTEGRITY_ALGORITHM_HMAC_SHA1_96,
                INTEGRITY_ALGORITHM_AES_XCBC_96,
                INTEGRITY_ALGORITHM_AES_CMAC_96,
                INTEGRITY_ALGORITHM_HMAC_SHA2_256_128,
                INTEGRITY_ALGORITHM_HMAC_SHA2_384_192,
                INTEGRITY_ALGORITHM_HMAC_SHA2_512_256,
            })
    public @interface IntegrityAlgorithms {}

    /** @hide */
    public static final int INTEGRITY_ALGORITHM_UNSPECIFIED =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__INTEGRITY_ALGORITHM__INTEGRITY_ALGORITHM_UNSPECIFIED;

    /** @hide */
    public static final int INTEGRITY_ALGORITHM_NONE =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__INTEGRITY_ALGORITHM__INTEGRITY_ALGORITHM_NONE;

    /** @hide */
    public static final int INTEGRITY_ALGORITHM_HMAC_SHA1_96 =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__INTEGRITY_ALGORITHM__INTEGRITY_ALGORITHM_HMAC_SHA1_96;

    /** @hide */
    public static final int INTEGRITY_ALGORITHM_AES_XCBC_96 =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__INTEGRITY_ALGORITHM__INTEGRITY_ALGORITHM_AES_XCBC_96;

    /** @hide */
    public static final int INTEGRITY_ALGORITHM_AES_CMAC_96 =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__INTEGRITY_ALGORITHM__INTEGRITY_ALGORITHM_AES_CMAC_96;

    /** @hide */
    public static final int INTEGRITY_ALGORITHM_HMAC_SHA2_256_128 =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__INTEGRITY_ALGORITHM__INTEGRITY_ALGORITHM_HMAC_SHA2_256_128;

    /** @hide */
    public static final int INTEGRITY_ALGORITHM_HMAC_SHA2_384_192 =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__INTEGRITY_ALGORITHM__INTEGRITY_ALGORITHM_HMAC_SHA2_384_192;

    /** @hide */
    public static final int INTEGRITY_ALGORITHM_HMAC_SHA2_512_256 =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__INTEGRITY_ALGORITHM__INTEGRITY_ALGORITHM_HMAC_SHA2_512_256;

    /**
     * Values for PrfAlgorithms from PrfAlgorithms enum proto
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"PSEUDORANDOM_FUNCTION_"},
            value = {
                PSEUDORANDOM_FUNCTION_UNSPECIFIED,
                PSEUDORANDOM_FUNCTION_HMAC_SHA1,
                PSEUDORANDOM_FUNCTION_AES128_XCBC,
                PSEUDORANDOM_FUNCTION_SHA2_256,
                PSEUDORANDOM_FUNCTION_SHA2_384,
                PSEUDORANDOM_FUNCTION_SHA2_512,
                PSEUDORANDOM_FUNCTION_AES128_CMAC,
            })
    public @interface PrfAlgorithms {}

    /** @hide */
    public static final int PSEUDORANDOM_FUNCTION_UNSPECIFIED =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__PRF_ALGORITHMS__PSEUDORANDOM_FUNCTION_UNSPECIFIED;

    /** @hide */
    public static final int PSEUDORANDOM_FUNCTION_HMAC_SHA1 =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__PRF_ALGORITHMS__PSEUDORANDOM_FUNCTION_HMAC_SHA1;

    /** @hide */
    public static final int PSEUDORANDOM_FUNCTION_AES128_XCBC =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__PRF_ALGORITHMS__PSEUDORANDOM_FUNCTION_AES128_XCBC;

    /** @hide */
    public static final int PSEUDORANDOM_FUNCTION_SHA2_256 =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__PRF_ALGORITHMS__PSEUDORANDOM_FUNCTION_SHA2_256;

    /** @hide */
    public static final int PSEUDORANDOM_FUNCTION_SHA2_384 =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__PRF_ALGORITHMS__PSEUDORANDOM_FUNCTION_SHA2_384;

    /** @hide */
    public static final int PSEUDORANDOM_FUNCTION_SHA2_512 =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__PRF_ALGORITHMS__PSEUDORANDOM_FUNCTION_SHA2_512;

    /** @hide */
    public static final int PSEUDORANDOM_FUNCTION_AES128_CMAC =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__PRF_ALGORITHMS__PSEUDORANDOM_FUNCTION_AES128_CMAC;

    /**
     * Values for EncryptionAlgorithms from EncryptionAlgorithms enum proto
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"ENCRYPTION_ALGORITHM_"},
            value = {
                ENCRYPTION_ALGORITHM_UNSPECIFIED,
                ENCRYPTION_ALGORITHM_3DES,
                ENCRYPTION_ALGORITHM_AES_CBC,
                ENCRYPTION_ALGORITHM_AES_CTR,
                ENCRYPTION_ALGORITHM_AES_GCM_8,
                ENCRYPTION_ALGORITHM_AES_GCM_12,
                ENCRYPTION_ALGORITHM_AES_GCM_16,
                ENCRYPTION_ALGORITHM_CHACHA20_POLY1305,
            })
    public @interface EncryptionAlgorithms {}

    /** @hide */
    public static final int ENCRYPTION_ALGORITHM_UNSPECIFIED =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__ENCRYPTION_ALGORITHM__ENCRYPTION_ALGORITHM_UNSPECIFIED;

    /** @hide */
    public static final int ENCRYPTION_ALGORITHM_3DES =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__ENCRYPTION_ALGORITHM__ENCRYPTION_ALGORITHM_3DES;

    /** @hide */
    public static final int ENCRYPTION_ALGORITHM_AES_CBC =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__ENCRYPTION_ALGORITHM__ENCRYPTION_ALGORITHM_AES_CBC;

    /** @hide */
    public static final int ENCRYPTION_ALGORITHM_AES_CTR =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__ENCRYPTION_ALGORITHM__ENCRYPTION_ALGORITHM_AES_CTR;

    /** @hide */
    public static final int ENCRYPTION_ALGORITHM_AES_GCM_8 =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__ENCRYPTION_ALGORITHM__ENCRYPTION_ALGORITHM_AES_GCM_8;

    /** @hide */
    public static final int ENCRYPTION_ALGORITHM_AES_GCM_12 =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__ENCRYPTION_ALGORITHM__ENCRYPTION_ALGORITHM_AES_GCM_12;

    /** @hide */
    public static final int ENCRYPTION_ALGORITHM_AES_GCM_16 =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__ENCRYPTION_ALGORITHM__ENCRYPTION_ALGORITHM_AES_GCM_16;

    /** @hide */
    public static final int ENCRYPTION_ALGORITHM_CHACHA20_POLY1305 =
            IkeMetricsInterface
                    .NEGOTIATED_SECURITY_ASSOCIATION__ENCRYPTION_ALGORITHM__ENCRYPTION_ALGORITHM_CHACHA20_POLY1305;

    /**
     * Values for KeyLengths from KeyLengths enum proto
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"KEY_LEN_"},
            value = {
                KEY_LEN_UNSPECIFIED,
                KEY_LEN_UNUSED,
                KEY_LEN_AES_128,
                KEY_LEN_AES_192,
                KEY_LEN_AES_256,
            })
    public @interface KeyLengths {}

    /** @hide */
    public static final int KEY_LEN_UNSPECIFIED =
            IkeMetricsInterface.NEGOTIATED_SECURITY_ASSOCIATION__KEY_LENGTH__KEY_LEN_UNSPECIFIED;

    /** @hide */
    public static final int KEY_LEN_UNUSED =
            IkeMetricsInterface.NEGOTIATED_SECURITY_ASSOCIATION__KEY_LENGTH__KEY_LEN_UNUSED;

    /** @hide */
    public static final int KEY_LEN_AES_128 =
            IkeMetricsInterface.NEGOTIATED_SECURITY_ASSOCIATION__KEY_LENGTH__KEY_LEN_AES_128;

    /** @hide */
    public static final int KEY_LEN_AES_192 =
            IkeMetricsInterface.NEGOTIATED_SECURITY_ASSOCIATION__KEY_LENGTH__KEY_LEN_AES_192;

    /** @hide */
    public static final int KEY_LEN_AES_256 =
            IkeMetricsInterface.NEGOTIATED_SECURITY_ASSOCIATION__KEY_LENGTH__KEY_LEN_AES_256;

    /**
     * Values for State from IkeState enum proto
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"IKE_STATE_"},
            value = {
                IKE_STATE_UNKNOWN,
                IKE_STATE_IKE_KILL,
                IKE_STATE_IKE_INITIAL,
                IKE_STATE_IKE_CREATE_LOCAL_IKE_INIT,
                IKE_STATE_IKE_CREATE_LOCAL_IKE_AUTH,
                IKE_STATE_IKE_CREATE_LOCAL_IKE_AUTH_IN_EAP,
                IKE_STATE_IKE_CREATE_LOCAL_IKE_AUTH_POST_EAP,
                IKE_STATE_IKE_IDLE,
                IKE_STATE_IKE_CHILD_PROCEDURE_ONGOING,
                IKE_STATE_IKE_RECEIVING,
                IKE_STATE_IKE_REKEY_LOCAL_CREATE,
                IKE_STATE_IKE_SIMULTANEOUS_REKEY_LOCAL_CREATE,
                IKE_STATE_IKE_SIMULTANEOUS_REKEY_LOCAL_DELETE_REMOTE_DELETE,
                IKE_STATE_IKE_SIMULTANEOUS_REKEY_LOCAL_DELETE,
                IKE_STATE_IKE_SIMULTANEOUS_REKEY_REMOTE_DELETE,
                IKE_STATE_IKE_REKEY_LOCAL_DELETE,
                IKE_STATE_IKE_REKEY_REMOTE_DELETE,
                IKE_STATE_IKE_DELETE_LOCAL_DELETE,
                IKE_STATE_IKE_DPD_LOCAL_INFO,
                IKE_STATE_IKE_MOBIKE_LOCAL_INFO,
                IKE_STATE_IKE_DPD_ON_DEMAND_LOCAL_INFO,
                IKE_STATE_CHILD_KILL,
                IKE_STATE_CHILD_INITIAL,
                IKE_STATE_CHILD_CREATE_LOCAL_CREATE,
                IKE_STATE_CHILD_IDLE,
                IKE_STATE_CHILD_IDLE_WITH_DEFERRED_REQUEST,
                IKE_STATE_CHILD_CLOSE_AND_AWAIT_RESPONSE,
                IKE_STATE_CHILD_DELETE_LOCAL_DELETE,
                IKE_STATE_CHILD_DELETE_REMOTE_DELETE,
                IKE_STATE_CHILD_REKEY_LOCAL_CREATE,
                IKE_STATE_CHILD_MOBIKE_REKEY_LOCAL_CREATE,
                IKE_STATE_CHILD_REKEY_REMOTE_CREATE,
                IKE_STATE_CHILD_REKEY_LOCAL_DELETE,
                IKE_STATE_CHILD_REKEY_REMOTE_DELETE,
            })
    public @interface IkeState {}

    /** @hide */
    public static final int IKE_STATE_UNKNOWN =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_UNKNOWN;
    /** @hide */
    public static final int IKE_STATE_IKE_KILL =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_KILL;
    /** @hide */
    public static final int IKE_STATE_IKE_INITIAL =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_INITIAL;
    /** @hide */
    public static final int IKE_STATE_IKE_CREATE_LOCAL_IKE_INIT =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_CREATE_LOCAL_IKE_INIT;
    /** @hide */
    public static final int IKE_STATE_IKE_CREATE_LOCAL_IKE_AUTH =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_CREATE_LOCAL_IKE_AUTH;
    /** @hide */
    public static final int IKE_STATE_IKE_CREATE_LOCAL_IKE_AUTH_IN_EAP =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_CREATE_LOCAL_IKE_AUTH_IN_EAP;
    /** @hide */
    public static final int IKE_STATE_IKE_CREATE_LOCAL_IKE_AUTH_POST_EAP =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_CREATE_LOCAL_IKE_AUTH_POST_EAP;
    /** @hide */
    public static final int IKE_STATE_IKE_IDLE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_IDLE;
    /** @hide */
    public static final int IKE_STATE_IKE_CHILD_PROCEDURE_ONGOING =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_CHILD_PROCEDURE_ONGOING;
    /** @hide */
    public static final int IKE_STATE_IKE_RECEIVING =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_RECEIVING;
    /** @hide */
    public static final int IKE_STATE_IKE_REKEY_LOCAL_CREATE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_REKEY_LOCAL_CREATE;
    /** @hide */
    public static final int IKE_STATE_IKE_SIMULTANEOUS_REKEY_LOCAL_CREATE =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_SIMULTANEOUS_REKEY_LOCAL_CREATE;
    /** @hide */
    public static final int IKE_STATE_IKE_SIMULTANEOUS_REKEY_LOCAL_DELETE_REMOTE_DELETE =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_SIMULTANEOUS_REKEY_LOCAL_DELETE_REMOTE_DELETE;
    /** @hide */
    public static final int IKE_STATE_IKE_SIMULTANEOUS_REKEY_LOCAL_DELETE =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_SIMULTANEOUS_REKEY_LOCAL_DELETE;
    /** @hide */
    public static final int IKE_STATE_IKE_SIMULTANEOUS_REKEY_REMOTE_DELETE =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_SIMULTANEOUS_REKEY_REMOTE_DELETE;
    /** @hide */
    public static final int IKE_STATE_IKE_REKEY_LOCAL_DELETE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_REKEY_LOCAL_DELETE;
    /** @hide */
    public static final int IKE_STATE_IKE_REKEY_REMOTE_DELETE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_REKEY_REMOTE_DELETE;
    /** @hide */
    public static final int IKE_STATE_IKE_DELETE_LOCAL_DELETE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_DELETE_LOCAL_DELETE;
    /** @hide */
    public static final int IKE_STATE_IKE_DPD_LOCAL_INFO =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_DPD_LOCAL_INFO;
    /** @hide */
    public static final int IKE_STATE_IKE_MOBIKE_LOCAL_INFO =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_MOBIKE_LOCAL_INFO;
    /** @hide */
    public static final int IKE_STATE_IKE_DPD_ON_DEMAND_LOCAL_INFO =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_STATE__STATE_IKE_DPD_ON_DEMAND_LOCAL_INFO;
    /** @hide */
    public static final int IKE_STATE_CHILD_KILL =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_CHILD_KILL;
    /** @hide */
    public static final int IKE_STATE_CHILD_INITIAL =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_CHILD_INITIAL;
    /** @hide */
    public static final int IKE_STATE_CHILD_CREATE_LOCAL_CREATE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_CHILD_CREATE_LOCAL_CREATE;
    /** @hide */
    public static final int IKE_STATE_CHILD_IDLE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_CHILD_IDLE;
    /** @hide */
    public static final int IKE_STATE_CHILD_IDLE_WITH_DEFERRED_REQUEST =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_STATE__STATE_CHILD_IDLE_WITH_DEFERRED_REQUEST;
    /** @hide */
    public static final int IKE_STATE_CHILD_CLOSE_AND_AWAIT_RESPONSE =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_STATE__STATE_CHILD_CLOSE_AND_AWAIT_RESPONSE;
    /** @hide */
    public static final int IKE_STATE_CHILD_DELETE_LOCAL_DELETE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_CHILD_DELETE_LOCAL_DELETE;
    /** @hide */
    public static final int IKE_STATE_CHILD_DELETE_REMOTE_DELETE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_CHILD_DELETE_REMOTE_DELETE;
    /** @hide */
    public static final int IKE_STATE_CHILD_REKEY_LOCAL_CREATE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_CHILD_REKEY_LOCAL_CREATE;
    /** @hide */
    public static final int IKE_STATE_CHILD_MOBIKE_REKEY_LOCAL_CREATE =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_STATE__STATE_CHILD_MOBIKE_REKEY_LOCAL_CREATE;
    /** @hide */
    public static final int IKE_STATE_CHILD_REKEY_REMOTE_CREATE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_CHILD_REKEY_REMOTE_CREATE;
    /** @hide */
    public static final int IKE_STATE_CHILD_REKEY_LOCAL_DELETE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_CHILD_REKEY_LOCAL_DELETE;
    /** @hide */
    public static final int IKE_STATE_CHILD_REKEY_REMOTE_DELETE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_STATE__STATE_CHILD_REKEY_REMOTE_DELETE;

    /**
     * Values for Error from IkeError enum proto
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"IKE_ERROR_"},
            value = {
                IKE_ERROR_UNKNOWN,
                IKE_ERROR_NONE,
                IKE_ERROR_INTERNAL,
                IKE_ERROR_NETWORK_LOST,
                IKE_ERROR_IO_GENERAL,
                IKE_ERROR_IO_TIMEOUT,
                IKE_ERROR_IO_DNS_FAILURE,
                IKE_ERROR_PROTOCOL_UNKNOWN,
                IKE_ERROR_PROTOCOL_UNSUPPORTED_CRITICAL_PAYLOAD,
                IKE_ERROR_PROTOCOL_INVALID_IKE_SPI,
                IKE_ERROR_PROTOCOL_INVALID_MAJOR_VERSION,
                IKE_ERROR_PROTOCOL_INVALID_SYNTAX,
                IKE_ERROR_PROTOCOL_INVALID_MESSAGE_ID,
                IKE_ERROR_PROTOCOL_NO_PROPOSAL_CHOSEN,
                IKE_ERROR_PROTOCOL_INVALID_KE_PAYLOAD,
                IKE_ERROR_PROTOCOL_AUTHENTICATION_FAILED,
                IKE_ERROR_PROTOCOL_SINGLE_PAIR_REQUIRED,
                IKE_ERROR_PROTOCOL_NO_ADDITIONAL_SAS,
                IKE_ERROR_PROTOCOL_INTERNAL_ADDRESS_FAILURE,
                IKE_ERROR_PROTOCOL_FAILED_CP_REQUIRED,
                IKE_ERROR_PROTOCOL_TS_UNACCEPTABLE,
                IKE_ERROR_PROTOCOL_INVALID_SELECTORS,
                IKE_ERROR_PROTOCOL_TEMPORARY_FAILURE,
                IKE_ERROR_PROTOCOL_CHILD_SA_NOT_FOUND,
            })
    public @interface IkeError {}

    /** @hide */
    public static final int IKE_ERROR_UNKNOWN =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_UNKNOWN;
    /** @hide */
    public static final int IKE_ERROR_NONE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_NONE;
    /** @hide */
    public static final int IKE_ERROR_INTERNAL =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_INTERNAL;
    /** @hide */
    public static final int IKE_ERROR_NETWORK_LOST =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_NETWORK_LOST;
    /** @hide */
    public static final int IKE_ERROR_IO_GENERAL =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_IO_GENERAL;
    /** @hide */
    public static final int IKE_ERROR_IO_TIMEOUT =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_IO_TIMEOUT;
    /** @hide */
    public static final int IKE_ERROR_IO_DNS_FAILURE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_IO_DNS_FAILURE;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_UNKNOWN =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_UNKNOWN;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_UNSUPPORTED_CRITICAL_PAYLOAD =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_UNSUPPORTED_CRITICAL_PAYLOAD;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_INVALID_IKE_SPI =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_INVALID_IKE_SPI;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_INVALID_MAJOR_VERSION =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_INVALID_MAJOR_VERSION;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_INVALID_SYNTAX =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_INVALID_SYNTAX;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_INVALID_MESSAGE_ID =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_INVALID_MESSAGE_ID;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_NO_PROPOSAL_CHOSEN =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_NO_PROPOSAL_CHOSEN;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_INVALID_KE_PAYLOAD =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_INVALID_KE_PAYLOAD;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_AUTHENTICATION_FAILED =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_AUTHENTICATION_FAILED;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_SINGLE_PAIR_REQUIRED =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_SINGLE_PAIR_REQUIRED;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_NO_ADDITIONAL_SAS =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_NO_ADDITIONAL_SAS;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_INTERNAL_ADDRESS_FAILURE =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_INTERNAL_ADDRESS_FAILURE;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_FAILED_CP_REQUIRED =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_FAILED_CP_REQUIRED;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_TS_UNACCEPTABLE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_TS_UNACCEPTABLE;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_INVALID_SELECTORS =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_INVALID_SELECTORS;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_TEMPORARY_FAILURE =
            IkeMetricsInterface.IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_TEMPORARY_FAILURE;
    /** @hide */
    public static final int IKE_ERROR_PROTOCOL_CHILD_SA_NOT_FOUND =
            IkeMetricsInterface
                    .IKE_SESSION_TERMINATED__IKE_ERROR__ERROR_PROTOCOL_CHILD_SA_NOT_FOUND;
    /** @hide */
    public static final int IKE_TASK_UNSPECIFIED =
            IkeMetricsInterface
                    .IKE_LIVENESS_CHECK_SESSION_VALIDATED__IKE_TASK__IKE_TASK_UNSPECIFIED;
    /**
     * Values for UnderlyingNetworkType from IkeUnderlyingNetworkType enum proto
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"IKE_UNDERLYING_NETWORK_TYPE_"},
            value = {
                IKE_UNDERLYING_NETWORK_TYPE_UNSPECIFIED,
                IKE_UNDERLYING_NETWORK_TYPE_WIFI,
                IKE_UNDERLYING_NETWORK_TYPE_CELLULAR,
            })
    public @interface IkeUnderlyingNetworkType {}

    /** @hide */
    public static final int IKE_UNDERLYING_NETWORK_TYPE_UNSPECIFIED =
            IkeMetricsInterface
                    .IKE_LIVENESS_CHECK_SESSION_VALIDATED__IKE_UNDERLYING_NETWORK_TYPE__NETWORK_UNSPECIFIED;
    /** @hide */
    public static final int IKE_UNDERLYING_NETWORK_TYPE_WIFI =
            IkeMetricsInterface
                    .IKE_LIVENESS_CHECK_SESSION_VALIDATED__IKE_UNDERLYING_NETWORK_TYPE__NETWORK_WIFI;
    /** @hide */
    public static final int IKE_UNDERLYING_NETWORK_TYPE_CELLULAR =
            IkeMetricsInterface
                    .IKE_LIVENESS_CHECK_SESSION_VALIDATED__IKE_UNDERLYING_NETWORK_TYPE__NETWORK_CELLULAR;

    /**
     * Log an atom when an IKE session or a Child Session is terminated.
     *
     * <p>This method should not be called if the Child Session is terminated due to IKE Session
     * closure.
     */
    public void logSessionTerminated(
            @IkeCaller int ikeCaller,
            @IkeSessionType int ikeSessionType,
            @IkeState int ikeState,
            @IkeError int ikeError) {
        IkeMetricsInterface.write(
                IkeMetricsInterface.IKE_SESSION_TERMINATED,
                ikeCaller,
                ikeSessionType,
                ikeState,
                ikeError);
    }

    /**
     * Log an atom when an liveness check is completed.
     *
     * <p>This method requests a liveness check from the client and records the associated
     * information when the liveness check is completed regardless of success of failure.
     */
    public void logLivenessCheckCompleted(
            @IkeCaller int ikeCaller,
            @IkeState int ikeState,
            @IkeUnderlyingNetworkType int ikeUnderlyingNetworkType,
            int elapsedTimeInMillis,
            int numberOfOnGoing,
            boolean resultSuccess) {
        IkeMetricsInterface.write(
                IkeMetricsInterface.IKE_LIVENESS_CHECK_SESSION_VALIDATED,
                ikeCaller,
                IKE_TASK_UNSPECIFIED,
                ikeUnderlyingNetworkType,
                elapsedTimeInMillis,
                numberOfOnGoing,
                resultSuccess,
                ikeState);
    }

    /**
     * Log an atom when IKE or Child SA negotiation completed (success or failure).
     *
     * <p>This method captures negotiated security association transforms or any failures during
     * negotiation.
     */
    public void logSaNegotiation(
            @IkeCaller int ikeCaller,
            @IkeSessionType int ikeSessionType,
            @IkeState int ikeState,
            @DhGroups int dhGroup,
            @EncryptionAlgorithms int encryptionAlgorithm,
            @KeyLengths int keyLength,
            @IntegrityAlgorithms int integrityAlgorithm,
            @PrfAlgorithms int prfAlgorithm,
            @IkeError int ikeError) {
        IkeMetricsInterface.write(
                IkeMetricsInterface.NEGOTIATED_SECURITY_ASSOCIATION,
                ikeCaller,
                ikeSessionType,
                ikeState,
                dhGroup,
                encryptionAlgorithm,
                keyLength,
                integrityAlgorithm,
                prfAlgorithm,
                ikeError);
    }
}
