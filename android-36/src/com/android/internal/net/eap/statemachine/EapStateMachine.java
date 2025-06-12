/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.net.eap.statemachine;

import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_AKA;
import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_AKA_PRIME;
import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_MSCHAP_V2;
import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_SIM;
import static android.net.eap.EapSessionConfig.EapMethodConfig.EAP_TYPE_TTLS;

import static com.android.internal.net.eap.EapAuthenticator.LOG;
import static com.android.internal.net.eap.message.EapData.EAP_IDENTITY;
import static com.android.internal.net.eap.message.EapData.EAP_NAK;
import static com.android.internal.net.eap.message.EapData.EAP_NOTIFICATION;
import static com.android.internal.net.eap.message.EapData.EAP_TYPE_STRING;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_FAILURE;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_REQUEST;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_RESPONSE;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_STRING;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_SUCCESS;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.net.eap.EapSessionConfig;
import android.net.eap.EapSessionConfig.EapAkaConfig;
import android.net.eap.EapSessionConfig.EapAkaPrimeConfig;
import android.net.eap.EapSessionConfig.EapMethodConfig;
import android.net.eap.EapSessionConfig.EapMethodConfig.EapMethod;
import android.net.eap.EapSessionConfig.EapMsChapV2Config;
import android.net.eap.EapSessionConfig.EapSimConfig;
import android.net.eap.EapSessionConfig.EapTtlsConfig;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.EapResult;
import com.android.internal.net.eap.EapResult.EapError;
import com.android.internal.net.eap.EapResult.EapFailure;
import com.android.internal.net.eap.EapResult.EapResponse;
import com.android.internal.net.eap.EapResult.EapSuccess;
import com.android.internal.net.eap.EapSimAkaIdentityTracker;
import com.android.internal.net.eap.exceptions.EapInvalidRequestException;
import com.android.internal.net.eap.exceptions.EapSilentException;
import com.android.internal.net.eap.exceptions.UnsupportedEapTypeException;
import com.android.internal.net.eap.message.EapData;
import com.android.internal.net.eap.message.EapMessage;
import com.android.internal.net.utils.SimpleStateMachine;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * EapStateMachine represents the valid paths for a single EAP Authentication procedure.
 *
 * <p>EAP Authentication procedures will always follow the path:
 *
 * CreatedState --> IdentityState --> Method State --+--> SuccessState
 *      |                                 ^          |
 *      +---------------------------------+          +--> FailureState
 *
 */
public class EapStateMachine extends SimpleStateMachine<byte[], EapResult> {
    private static final String TAG = EapStateMachine.class.getSimpleName();

    private final Context mContext;
    private final EapSessionConfig mEapSessionConfig;
    private final SecureRandom mSecureRandom;

    public EapStateMachine(
            @NonNull Context context,
            @NonNull EapSessionConfig eapSessionConfig,
            @NonNull SecureRandom secureRandom) {
        this.mContext = context;
        this.mEapSessionConfig = eapSessionConfig;
        this.mSecureRandom = secureRandom;

        LOG.d(
                TAG,
                "Starting EapStateMachine with EAP-Identity="
                        + LOG.pii(eapSessionConfig.getEapIdentity())
                        + " and configs=" + eapSessionConfig.getEapConfigs().keySet());

        transitionTo(new CreatedState());
    }

    @VisibleForTesting
    protected SimpleStateMachine.SimpleState getState() {
        return mState;
    }

    @VisibleForTesting
    protected void transitionTo(EapState newState) {
        LOG.d(
                TAG,
                "Transitioning from " + mState.getClass().getSimpleName()
                        + " to " + newState.getClass().getSimpleName());
        super.transitionTo(newState);
    }

    @VisibleForTesting
    protected EapResult transitionAndProcess(EapState newState, byte[] packet) {
        return super.transitionAndProcess(newState, packet);
    }

    protected abstract class EapState extends SimpleState {
        protected DecodeResult decode(@NonNull byte[] packet) {
            LOG.d(getClass().getSimpleName(),
                    "Received packet=[" + LOG.pii(packet) + "]");

            if (packet == null) {
                return new DecodeResult(new EapError(
                        new IllegalArgumentException("Attempting to decode null packet")));
            }

            try {
                EapMessage eapMessage = EapMessage.decode(packet);

                // Log inbound message in the format "EAP-<Code>/<Type>"
                String eapDataString =
                        (eapMessage.eapData == null)
                                ? ""
                                : "/" + EAP_TYPE_STRING.getOrDefault(
                                        eapMessage.eapData.eapType,
                                        "UNKNOWN (" + eapMessage.eapData.eapType + ")");
                String msg = "Decoded message: EAP-"
                        + EAP_CODE_STRING.getOrDefault(eapMessage.eapCode, "UNKNOWN")
                        + eapDataString;
                LOG.i(getClass().getSimpleName(), msg);

                if (eapMessage.eapCode == EAP_CODE_RESPONSE) {
                    EapInvalidRequestException cause =
                            new EapInvalidRequestException("Received an EAP-Response message");
                    return new DecodeResult(new EapError(cause));
                } else if (eapMessage.eapCode == EAP_CODE_REQUEST
                        && eapMessage.eapData.eapType == EAP_NAK) {
                    // RFC 3748 Section 5.3.1 states that Nak type is only valid in responses
                    EapInvalidRequestException cause =
                            new EapInvalidRequestException("Received an EAP-Request of type Nak");
                    return new DecodeResult(new EapError(cause));
                }

                return new DecodeResult(eapMessage);
            } catch (UnsupportedEapTypeException ex) {
                return new DecodeResult(
                        EapMessage.getNakResponse(
                                ex.eapIdentifier, mEapSessionConfig.getEapConfigs().keySet()));
            } catch (EapSilentException ex) {
                return new DecodeResult(new EapError(ex));
            }
        }

        protected final class DecodeResult {
            public final EapMessage eapMessage;
            public final EapResult eapResult;

            public DecodeResult(EapMessage eapMessage) {
                this.eapMessage = eapMessage;
                this.eapResult = null;
            }

            public DecodeResult(EapResult eapResult) {
                this.eapMessage = null;
                this.eapResult = eapResult;
            }

            public boolean isValidEapMessage() {
                return eapMessage != null;
            }
        }
    }

    protected class CreatedState extends EapState {
        private final String mTAG = CreatedState.class.getSimpleName();

        public EapResult process(@NonNull byte[] packet) {
            DecodeResult decodeResult = decode(packet);
            if (!decodeResult.isValidEapMessage()) {
                return decodeResult.eapResult;
            }
            EapMessage message = decodeResult.eapMessage;

            if (message.eapCode != EAP_CODE_REQUEST) {
                return new EapError(
                        new EapInvalidRequestException("Received non EAP-Request in CreatedState"));
            }

            // EapMessage#validate verifies that all EapMessage objects representing
            // EAP-Request packets have a Type value
            switch (message.eapData.eapType) {
                case EAP_NOTIFICATION:
                    return handleNotification(mTAG, message);

                case EAP_IDENTITY:
                    return transitionAndProcess(new IdentityState(), packet);

                // all EAP methods should be handled by MethodState
                default:
                    return transitionAndProcess(new MethodState(), packet);
            }
        }
    }

    protected class IdentityState extends EapState {
        private final String mTAG = IdentityState.class.getSimpleName();

        public EapResult process(@NonNull byte[] packet) {
            DecodeResult decodeResult = decode(packet);
            if (!decodeResult.isValidEapMessage()) {
                return decodeResult.eapResult;
            }
            EapMessage message = decodeResult.eapMessage;

            if (message.eapCode != EAP_CODE_REQUEST) {
                return new EapError(new EapInvalidRequestException(
                        "Received non EAP-Request in IdentityState"));
            }

            // EapMessage#validate verifies that all EapMessage objects representing
            // EAP-Request packets have a Type value
            switch (message.eapData.eapType) {
                case EAP_NOTIFICATION:
                    return handleNotification(mTAG, message);

                case EAP_IDENTITY:
                    return getIdentityResponse(message.eapIdentifier);

                // all EAP methods should be handled by MethodState
                default:
                    return transitionAndProcess(new MethodState(), packet);
            }
        }

        @VisibleForTesting
        EapResult getIdentityResponse(int eapIdentifier) {
            try {
                byte[] eapIdentity = getEapIdentity();
                LOG.d(mTAG, "Returning EAP-Identity: " + LOG.pii(eapIdentity));
                EapData identityData = new EapData(EAP_IDENTITY, eapIdentity);
                return EapResponse.getEapResponse(
                        new EapMessage(EAP_CODE_RESPONSE, eapIdentifier, identityData));
            } catch (EapSilentException ex) {
                // this should never happen - only identifier and identity bytes are variable
                LOG.wtf(mTAG,  "Failed to create Identity response for message with identifier="
                        + LOG.pii(eapIdentifier));
                return new EapError(ex);
            }
        }

        @VisibleForTesting
        byte[] getEapIdentity() {
            if (mEapSessionConfig.getEapAkaConfig() != null
                    && mEapSessionConfig.getEapAkaConfig().getEapAkaOption() != null
                    && mEapSessionConfig.getEapAkaConfig().getEapAkaOption()
                    .getReauthId() != null) {
                byte[] reauthIdBytes =
                        mEapSessionConfig.getEapAkaConfig().getEapAkaOption().getReauthId();
                String reauthId = new String(reauthIdBytes, StandardCharsets.UTF_8);
                String permanentId =
                        new String(mEapSessionConfig.getEapIdentity(), StandardCharsets.UTF_8);
                EapSimAkaIdentityTracker.ReauthInfo reauthInfo =
                        EapSimAkaIdentityTracker.getInstance().getReauthInfo(reauthId, permanentId);

                if (reauthInfo != null && reauthInfo.isValid()) {
                    return reauthIdBytes;
                }
            }
            return mEapSessionConfig.getEapIdentity();
        }
    }

    protected class MethodState extends EapState {
        private final String mTAG = MethodState.class.getSimpleName();

        @VisibleForTesting
        EapMethodStateMachine mEapMethodStateMachine;

        // Not all EAP Method implementations may support EAP-Notifications, so allow the EAP-Method
        // to handle any EAP-REQUEST/Notification messages (RFC 3748 Section 5.2)
        public EapResult process(@NonNull byte[] packet) {
            DecodeResult decodeResult = decode(packet);
            if (!decodeResult.isValidEapMessage()) {
                return decodeResult.eapResult;
            }
            EapMessage eapMessage = decodeResult.eapMessage;

            if (mEapMethodStateMachine == null) {
                if (eapMessage.eapCode == EAP_CODE_SUCCESS) {
                    // EAP-SUCCESS is required to be the last EAP message sent during the EAP
                    // protocol, so receiving a premature SUCCESS message is an unrecoverable error
                    return new EapError(
                            new EapInvalidRequestException(
                                    "Received an EAP-Success in the MethodState"));
                } else if (eapMessage.eapCode == EAP_CODE_FAILURE) {
                    transitionTo(new FailureState());
                    return new EapFailure();
                } else if (eapMessage.eapData.eapType == EAP_NOTIFICATION) {
                    // if no EapMethodStateMachine has been assigned and we receive an
                    // EAP-Notification, we should log it and respond
                    return handleNotification(mTAG, eapMessage);
                }

                int eapType = eapMessage.eapData.eapType;
                mEapMethodStateMachine = buildEapMethodStateMachine(eapType);

                if (mEapMethodStateMachine == null) {
                    return EapMessage.getNakResponse(
                            eapMessage.eapIdentifier, mEapSessionConfig.getEapConfigs().keySet());
                }
            }

            EapResult result = mEapMethodStateMachine.process(decodeResult.eapMessage);
            if (result instanceof EapSuccess) {
                transitionTo(new SuccessState());
            } else if (result instanceof EapFailure) {
                transitionTo(new FailureState());
            }
            return result;
        }

        @Nullable
        private EapMethodStateMachine buildEapMethodStateMachine(@EapMethod int eapType) {
            EapMethodConfig eapMethodConfig = mEapSessionConfig.getEapConfigs().get(eapType);
            if (eapMethodConfig == null) {
                LOG.e(
                        mTAG,
                        "No configs provided for method: "
                                + EAP_TYPE_STRING.getOrDefault(
                                        eapType, "Unknown (" + eapType + ")"));
                return null;
            }

            switch (eapType) {
                case EAP_TYPE_SIM:
                    EapSimConfig eapSimConfig = (EapSimConfig) eapMethodConfig;
                    return new EapSimMethodStateMachine(
                            mContext,
                            mEapSessionConfig.getEapIdentity(),
                            eapSimConfig,
                            mSecureRandom);
                case EAP_TYPE_AKA:
                    EapAkaConfig eapAkaConfig = (EapAkaConfig) eapMethodConfig;
                    boolean supportsEapAkaPrime =
                            mEapSessionConfig.getEapConfigs().containsKey(EAP_TYPE_AKA_PRIME);
                    return new EapAkaMethodStateMachine(
                            mContext,
                            mEapSessionConfig.getEapIdentity(),
                            eapAkaConfig,
                            supportsEapAkaPrime,
                            mSecureRandom);
                case EAP_TYPE_AKA_PRIME:
                    EapAkaPrimeConfig eapAkaPrimeConfig = (EapAkaPrimeConfig) eapMethodConfig;
                    return new EapAkaPrimeMethodStateMachine(
                            mContext, mEapSessionConfig.getEapIdentity(), eapAkaPrimeConfig);
                case EAP_TYPE_MSCHAP_V2:
                    EapMsChapV2Config eapMsChapV2Config = (EapMsChapV2Config) eapMethodConfig;
                    return new EapMsChapV2MethodStateMachine(eapMsChapV2Config, mSecureRandom);
                case EAP_TYPE_TTLS:
                    EapTtlsConfig eapTtlsConfig = (EapTtlsConfig) eapMethodConfig;
                    return new EapTtlsMethodStateMachine(mContext, eapTtlsConfig, mSecureRandom);
                default:
                    // received unsupported EAP Type. This should never happen.
                    LOG.e(mTAG, "Received unsupported EAP Type=" + eapType);
                    throw new IllegalArgumentException(
                            "Received unsupported EAP Type in MethodState constructor");
            }
        }
    }

    protected class SuccessState extends EapState {
        public EapResult process(byte[] packet) {
            return new EapError(new EapInvalidRequestException(
                    "Not possible to process messages in Success State"));
        }
    }

    protected class FailureState extends EapState {
        public EapResult process(byte[] message) {
            return new EapError(new EapInvalidRequestException(
                    "Not possible to process messages in Failure State"));
        }
    }

    protected static EapResult handleNotification(String tag, EapMessage message) {
        // Type-Data will be UTF-8 encoded ISO 10646 characters (RFC 3748 Section 5.2)
        String content = new String(message.eapData.eapTypeData, StandardCharsets.UTF_8);
        LOG.i(tag, "Received EAP-Request/Notification: [" + content + "]");
        return EapMessage.getNotificationResponse(message.eapIdentifier);
    }
}
