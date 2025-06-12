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

import static com.android.internal.net.eap.EapAuthenticator.LOG;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_REQUEST;
import static com.android.internal.net.eap.message.EapMessage.EAP_CODE_RESPONSE;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtEncrData.CIPHER_BLOCK_LENGTH;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_COUNTER;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_ENCR_DATA;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_IV;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_MAC;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_NEXT_REAUTH_ID;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_NOTIFICATION;
import static com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.EAP_AT_PADDING;

import android.net.eap.EapSessionConfig.EapUiccConfig;
import android.telephony.TelephonyManager;
import android.util.Base64;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.EapResult;
import com.android.internal.net.eap.EapResult.EapError;
import com.android.internal.net.eap.EapResult.EapResponse;
import com.android.internal.net.eap.crypto.Fips186_2Prf;
import com.android.internal.net.eap.exceptions.EapInvalidRequestException;
import com.android.internal.net.eap.exceptions.EapSilentException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaAuthenticationFailureException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaInvalidAttributeException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaUnsupportedAttributeException;
import com.android.internal.net.eap.message.EapData;
import com.android.internal.net.eap.message.EapMessage;
import com.android.internal.net.eap.message.simaka.EapAkaAttributeFactory;
import com.android.internal.net.eap.message.simaka.EapAkaTypeData;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtClientErrorCode;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtEncrData;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtIv;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtMac;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtNextReauthId;
import com.android.internal.net.eap.message.simaka.EapSimAkaAttribute.AtNotification;
import com.android.internal.net.eap.message.simaka.EapSimAkaTypeData;
import com.android.internal.net.utils.Log;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * EapSimAkaMethodStateMachine represents an abstract state machine for managing EAP-SIM and EAP-AKA
 * sessions.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4186">RFC 4186, Extensible Authentication
 * Protocol for Subscriber Identity Modules (EAP-SIM)</a>
 * @see <a href="https://tools.ietf.org/html/rfc4187">RFC 4187, Extensible Authentication
 * Protocol for Authentication and Key Agreement (EAP-AKA)</a>
 */
public abstract class EapSimAkaMethodStateMachine extends EapMethodStateMachine {
    public static final String MASTER_KEY_GENERATION_ALG = "SHA-1";
    public static final String MAC_ALGORITHM_STRING = "HmacSHA1";

    // Master Key(SHA 1) length is 20 bytes(160bits) (RFC 3174 #1)
    public static final int MASTER_KEY_LENGTH = 20;
    // K_encr and K_aut lengths are 16 bytes (RFC 4186#7, RFC 4187#7)
    public static final int KEY_LEN = 16;

    // Session Key lengths are 64 bytes (RFC 4186#7, RFC 4187#7)
    public static final int SESSION_KEY_LENGTH = 64;

    // COUNTER SIZE 2bytes(16bit)(RFC 4187 #10.16)
    private static final int COUNTER_SIZE = 2;

    public final byte[] mMk = new byte[getMkLength()];
    public final byte[] mKEncr = new byte[getKEncrLength()];
    public final byte[] mKAut = new byte[getKAutLength()];
    public final byte[] mMsk = new byte[getMskLength()];
    public final byte[] mEmsk = new byte[getEmskLength()];
    @VisibleForTesting boolean mHasReceivedSimAkaNotification = false;

    final TelephonyManager mTelephonyManager;
    final byte[] mEapIdentity;
    final EapUiccConfig mEapUiccConfig;

    @VisibleForTesting Mac mMacAlgorithm;
    @VisibleForTesting SecureRandom mSecureRandom;

    EapSimAkaMethodStateMachine(
            TelephonyManager telephonyManager, byte[] eapIdentity, EapUiccConfig eapUiccConfig) {
        if (telephonyManager == null) {
            throw new IllegalArgumentException("TelephonyManager must be non-null");
        } else if (eapIdentity == null) {
            throw new IllegalArgumentException("EapIdentity must be non-null");
        } else if (eapUiccConfig == null) {
            throw new IllegalArgumentException("EapUiccConfig must be non-null");
        }
        this.mTelephonyManager = telephonyManager;
        this.mEapIdentity = eapIdentity;
        this.mEapUiccConfig = eapUiccConfig;

        LOG.d(
                this.getClass().getSimpleName(),
                mEapUiccConfig.getClass().getSimpleName() + ":"
                        + " subId=" + mEapUiccConfig.getSubId()
                        + " apptype=" + mEapUiccConfig.getAppType());
    }

    protected int getMkLength() {
        return MASTER_KEY_LENGTH;
    }

    protected int getKEncrLength() {
        return KEY_LEN;
    }

    protected int getKAutLength() {
        return KEY_LEN;
    }

    protected int getMskLength() {
        return SESSION_KEY_LENGTH;
    }

    protected int getEmskLength() {
        return SESSION_KEY_LENGTH;
    }

    @Override
    EapResult handleEapNotification(String tag, EapMessage message) {
        return EapStateMachine.handleNotification(tag, message);
    }

    protected String getMacAlgorithm() {
        return MAC_ALGORITHM_STRING;
    }

    @VisibleForTesting
    EapResult buildClientErrorResponse(
            int eapIdentifier,
            int eapMethodType,
            AtClientErrorCode clientErrorCode) {
        mIsExpectingEapFailure = true;
        EapSimAkaTypeData eapSimAkaTypeData = getEapSimAkaTypeData(clientErrorCode);
        byte[] encodedTypeData = eapSimAkaTypeData.encode();

        EapData eapData = new EapData(eapMethodType, encodedTypeData);
        try {
            EapMessage response = new EapMessage(EAP_CODE_RESPONSE, eapIdentifier, eapData);
            return EapResult.EapResponse.getEapResponse(response);
        } catch (EapSilentException ex) {
            return new EapResult.EapError(ex);
        }
    }

    @VisibleForTesting
    EapResult buildResponseMessage(
            int eapType,
            int eapSubtype,
            int identifier,
            List<EapSimAkaAttribute> attributes) {
        EapSimAkaTypeData eapSimTypeData = getEapSimAkaTypeData(eapSubtype, attributes);
        EapData eapData = new EapData(eapType, eapSimTypeData.encode());

        try {
            EapMessage eapMessage = new EapMessage(EAP_CODE_RESPONSE, identifier, eapData);
            return EapResult.EapResponse.getEapResponse(eapMessage);
        } catch (EapSilentException ex) {
            return new EapResult.EapError(ex);
        }
    }

    @VisibleForTesting
    protected void generateAndPersistKeys(
            String tag, MessageDigest sha1, Fips186_2Prf prf, byte[] mkInput) {
        byte[] mk = sha1.digest(mkInput);
        System.arraycopy(mk, 0, mMk, 0, MASTER_KEY_LENGTH);

        // run mk through FIPS 186-2
        int outputBytes = mKEncr.length + mKAut.length + mMsk.length + mEmsk.length;
        byte[] prfResult = prf.getRandom(mk, outputBytes);

        ByteBuffer prfResultBuffer = ByteBuffer.wrap(prfResult);
        prfResultBuffer.get(mKEncr);
        prfResultBuffer.get(mKAut);
        prfResultBuffer.get(mMsk);
        prfResultBuffer.get(mEmsk);

        // Log as hash unless PII debug mode enabled
        LOG.d(tag, "MK input=" + LOG.pii(mkInput));
        LOG.d(tag, "MK=" + LOG.pii(mk));
        LOG.d(tag, "K_encr=" + LOG.pii(mKEncr));
        LOG.d(tag, "K_aut=" + LOG.pii(mKAut));
        LOG.d(tag, "MSK=" + LOG.pii(mMsk));
        LOG.d(tag, "EMSK=" + LOG.pii(mEmsk));
    }

    @VisibleForTesting
    protected void generateAndPersistReauthKeys(
            String tag,
            MessageDigest sha1,
            Fips186_2Prf prf,
            byte[] reauthId,
            int count,
            byte[] nonceS,
            byte[] mk) {
        int numInputBytes = reauthId.length + COUNTER_SIZE + nonceS.length + mk.length;

        // XKEY' generated per reauth key generation procedure RFC 4187:
        // SHA1(Identity|counter|NONCE_S| MK)
        ByteBuffer buffer = ByteBuffer.allocate(numInputBytes);
        buffer.put(reauthId);
        buffer.putShort((short) count);
        buffer.put(nonceS);
        buffer.put(mk);
        byte[] xKeyPrimeInput = buffer.array();
        byte[] xKeyPrime = sha1.digest(xKeyPrimeInput);

        // run mk through FIPS 186-2
        int outputBytes = mMsk.length + mEmsk.length;
        byte[] prfResult = prf.getRandom(xKeyPrime, outputBytes);

        ByteBuffer prfResultBuffer = ByteBuffer.wrap(prfResult);
        prfResultBuffer.get(mMsk);
        prfResultBuffer.get(mEmsk);

        // Log as hash unless PII debug mode enabled
        LOG.d(tag, "MK=" + LOG.pii(mk));
        LOG.d(tag, "XKEY' INPUT=" + LOG.pii(xKeyPrimeInput));
        LOG.d(tag, "XKEY' =" + LOG.pii(xKeyPrime));
        LOG.d(tag, "K_encr=" + LOG.pii(mKEncr));
        LOG.d(tag, "K_aut=" + LOG.pii(mKAut));
        LOG.d(tag, "MSK=" + LOG.pii(mMsk));
        LOG.d(tag, "EMSK=" + LOG.pii(mEmsk));
    }

    @VisibleForTesting
    byte[] processUiccAuthentication(String tag, int authType, byte[] formattedChallenge) throws
            EapSimAkaAuthenticationFailureException {
        String base64Challenge = Base64.encodeToString(formattedChallenge, Base64.NO_WRAP);
        String base64Response =
                mTelephonyManager.getIccAuthentication(
                        mEapUiccConfig.getAppType(), authType, base64Challenge);

        if (base64Response == null) {
            String msg = "UICC authentication failed. Input: " + LOG.pii(formattedChallenge);
            LOG.e(tag, msg);
            throw new EapSimAkaAuthenticationFailureException(msg);
        }

        return Base64.decode(base64Response, Base64.DEFAULT);
    }

    @VisibleForTesting
    boolean isValidMac(String tag, EapMessage message, EapSimAkaTypeData typeData, byte[] extraData)
            throws GeneralSecurityException, EapSimAkaInvalidAttributeException,
                    EapSilentException {
        mMacAlgorithm = Mac.getInstance(getMacAlgorithm());
        mMacAlgorithm.init(new SecretKeySpec(mKAut, getMacAlgorithm()));

        LOG.d(tag, "Computing MAC (raw msg): " + LOG.pii(message.encode()));

        byte[] mac = getMac(message.eapCode, message.eapIdentifier, typeData, extraData);

        // attributes are 'valid', so must have AtMac
        AtMac atMac = (AtMac) typeData.attributeMap.get(EAP_AT_MAC);

        boolean isValidMac = Arrays.equals(mac, atMac.mac);
        if (!isValidMac) {
            // MAC in message != calculated mac
            LOG.e(
                    tag,
                    "Received message with invalid Mac."
                            + " received=" + Log.byteArrayToHexString(atMac.mac)
                            + ", computed=" + Log.byteArrayToHexString(mac));
        }

        return isValidMac;
    }

    @VisibleForTesting
    LinkedHashMap<Integer, EapSimAkaAttribute> retrieveSecuredAttributes(
            String tag, EapSimAkaTypeData typeData) {
        AtEncrData atEncrData = (AtEncrData) typeData.attributeMap.get(EAP_AT_ENCR_DATA);

        if (atEncrData == null) {
            LOG.d(tag, "AT_ENCR_DATA is not included.");
            return null;
        } else {
            AtIv atIv = (AtIv) typeData.attributeMap.get(EAP_AT_IV);
            if (atIv == null) {
                LOG.d(tag, "AT_IV is not included. can't decrypt ENCR DATA");
                return null;
            }

            byte[] decryptedData;
            try {
                decryptedData = atEncrData.getDecryptedData(mKEncr, atIv.iv);
            } catch (EapSimAkaInvalidAttributeException e) {
                LOG.d(tag, "Decrypt Fail, can't decrypt ENCR DATA");
                return null;
            }

            LinkedHashMap<Integer, EapSimAkaAttribute> securedAttributes;
            try {
                securedAttributes = getSecureAttributes(tag, decryptedData);
            } catch (EapSimAkaInvalidAttributeException e) {
                LOG.d(tag, "Decode Fail, can't decode decrypted ENCR DATA.");
                return null;
            }
            return securedAttributes;
        }
    }

    @VisibleForTesting
    byte[] retrieveNextReauthId(String tag, EapAkaTypeData typeData) {
        LinkedHashMap<Integer, EapSimAkaAttribute> securedAttributes =
                retrieveSecuredAttributes(tag, typeData);
        if (securedAttributes == null) {
            return null;
        } else {
            AtNextReauthId atNextReauthId =
                    (AtNextReauthId) securedAttributes.get(EAP_AT_NEXT_REAUTH_ID);
            if (atNextReauthId != null && atNextReauthId.reauthId != null) {
                return atNextReauthId.reauthId.clone();
            } else {
                return null;
            }
        }
    }

    @VisibleForTesting
    static LinkedHashMap<Integer, EapSimAkaAttribute> getSecureAttributes(String tag,
            byte[] decryptedData)
            throws EapSimAkaInvalidAttributeException {
        ByteBuffer secureDataByteBuffer = ByteBuffer.wrap(decryptedData);
        LinkedHashMap<Integer, EapSimAkaAttribute> attributeMap = new LinkedHashMap<>();
        EapAkaAttributeFactory attributeFactory = EapAkaAttributeFactory.getInstance();
        while (secureDataByteBuffer.hasRemaining()) {
            EapSimAkaAttribute attribute;
            try {
                attribute = attributeFactory.getAttribute(secureDataByteBuffer);
            } catch (EapSimAkaUnsupportedAttributeException e) {
                LOG.e(tag, "Unrecognized, non-skippable attribute encountered", e);
                throw new EapSimAkaInvalidAttributeException("Decode fail");
            }
            if (attributeMap.containsKey(attribute.attributeType)) {
                // Duplicate attributes are not allowed (RFC 4186#6.3.1, RFC 4187#6.3.1)
                LOG.e(tag, "Duplicate attribute in parsed EAP-Message");
                throw new EapSimAkaInvalidAttributeException("Duplicated attributes");
            }

            if (attribute instanceof EapSimAkaAttribute.EapSimAkaUnsupportedAttribute) {
                LOG.d(tag, "Unsupported EAP attribute during decoding: " + attribute.attributeType);
            }
            attributeMap.put(attribute.attributeType, attribute);
        }
        return attributeMap;
    }

    @VisibleForTesting
    static List<EapSimAkaAttribute> buildReauthResponse(
            int counter, boolean isCounterSmall, byte[] kEncr, AtIv atIv)
            throws EapSimAkaInvalidAttributeException {
        List<EapSimAkaAttribute> attrList = new ArrayList<>();
        ByteBuffer buffer;
        EapSimAkaAttribute atCounter = new EapSimAkaAttribute.AtCounter(counter);
        if (isCounterSmall) {
            EapSimAkaAttribute atCounterSmall = new EapSimAkaAttribute.AtCounterTooSmall();
            int paddingSize =
                    getPaddingSize(
                            CIPHER_BLOCK_LENGTH,
                            atCounter.lengthInBytes + atCounterSmall.lengthInBytes);
            EapSimAkaAttribute atPadding = new EapSimAkaAttribute.AtPadding(paddingSize);
            buffer =
                    ByteBuffer.allocate(
                            atCounter.lengthInBytes + atCounterSmall.lengthInBytes + paddingSize);
            atCounterSmall.encode(buffer);
            atCounter.encode(buffer);
            atPadding.encode(buffer);
        } else {
            int paddingSize = getPaddingSize(CIPHER_BLOCK_LENGTH, atCounter.lengthInBytes);
            EapSimAkaAttribute atPadding = new EapSimAkaAttribute.AtPadding(paddingSize);
            buffer = ByteBuffer.allocate(atCounter.lengthInBytes + paddingSize);
            atCounter.encode(buffer);
            atPadding.encode(buffer);
        }
        EapSimAkaAttribute atEncrData =
                new EapSimAkaAttribute.AtEncrData(buffer.array(), kEncr, atIv.iv);
        attrList.add(atIv);
        attrList.add(atEncrData);
        return attrList;
    }

    @VisibleForTesting
    static int getPaddingSize(int blockSize, int dataLength) {
        int remain = dataLength % blockSize;
        if (remain == 0) return 0;
        else return blockSize - remain;
    }

    @VisibleForTesting
    byte[] getMac(int eapCode, int eapIdentifier, EapSimAkaTypeData typeData, byte[] extraData)
            throws EapSimAkaInvalidAttributeException, EapSilentException {
        if (mMacAlgorithm == null) {
            throw new IllegalStateException(
                    "Can't calculate MAC before mMacAlgorithm is set in ChallengeState");
        }

        // cache original Mac so it can be restored after calculating the Mac
        AtMac originalMac = (AtMac) typeData.attributeMap.get(EAP_AT_MAC);
        typeData.attributeMap.put(EAP_AT_MAC, originalMac.getAtMacWithMacCleared());

        byte[] typeDataWithEmptyMac = typeData.encode();
        EapData eapData = new EapData(getEapMethod(), typeDataWithEmptyMac);
        EapMessage messageForMac = new EapMessage(eapCode, eapIdentifier, eapData);

        LOG.d(this.getClass().getSimpleName(),
                "Computing MAC (mac cleared): " + LOG.pii(messageForMac.encode()));

        ByteBuffer buffer = ByteBuffer.allocate(messageForMac.eapLength + extraData.length);
        buffer.put(messageForMac.encode());
        buffer.put(extraData);
        byte[] mac = mMacAlgorithm.doFinal(buffer.array());

        typeData.attributeMap.put(EAP_AT_MAC, originalMac);

        // need HMAC-SHA1-128 - first 16 bytes of SHA1 (RFC 4186#10.14, RFC 4187#10.15)
        return Arrays.copyOfRange(mac, 0, AtMac.MAC_LENGTH);
    }

    @VisibleForTesting
    EapResult buildResponseMessageWithMac(int identifier, int eapSubtype, byte[] extraData) {
        // capacity of 1 for AtMac to be added
        return buildResponseMessageWithMac(
                identifier,
                eapSubtype,
                extraData,
                new ArrayList<>(1) /* attributes */,
                null /* flagsToAdd */);
    }

    @VisibleForTesting
    EapResult buildResponseMessageWithMac(
            int identifier,
            int eapSubtype,
            byte[] extraData,
            List<EapSimAkaAttribute> attributes,
            @EapResponse.EapResponseFlag int[] flagsToAdd) {
        try {
            attributes = new ArrayList<>(attributes);
            attributes.add(new AtMac());
            EapSimAkaTypeData eapSimAkaTypeData = getEapSimAkaTypeData(eapSubtype, attributes);

            byte[] mac = getMac(EAP_CODE_RESPONSE, identifier, eapSimAkaTypeData, extraData);

            eapSimAkaTypeData.attributeMap.put(EAP_AT_MAC, new AtMac(mac));
            EapData eapData = new EapData(getEapMethod(), eapSimAkaTypeData.encode());
            EapMessage eapMessage = new EapMessage(EAP_CODE_RESPONSE, identifier, eapData);
            return EapResponse.getEapResponse(eapMessage, flagsToAdd);
        } catch (EapSimAkaInvalidAttributeException | EapSilentException ex) {
            // this should never happen
            return new EapError(ex);
        }
    }

    // AT_COUNTER attribute MUST be included in EAP-AKA notifications and MUST be encrypted.
    private int validateReauthAkaNotifyAndGetCounter(EapSimAkaTypeData eapSimAkaTypeData) {
        Set<Integer> attrs = eapSimAkaTypeData.attributeMap.keySet();
        if (attrs.contains(EAP_AT_IV)
                && attrs.contains(EAP_AT_ENCR_DATA)
                && attrs.contains(EAP_AT_MAC)) {
            LinkedHashMap<Integer, EapSimAkaAttribute> securedAttributes =
                    retrieveSecuredAttributes("Notification", eapSimAkaTypeData);
            Set<Integer> securedAttrKeySet = securedAttributes.keySet();

            if (securedAttrKeySet.contains(EAP_AT_COUNTER)
                    && (securedAttrKeySet.size() == 1
                            || (securedAttrKeySet.size() == 2
                                    && securedAttrKeySet.contains(EAP_AT_PADDING)))) {
                return ((EapSimAkaAttribute.AtCounter) securedAttributes.get(EAP_AT_COUNTER))
                        .counter;
            }
        }
        return -1;
    }

    @VisibleForTesting
    EapResult handleEapSimAkaNotification(
            String tag,
            boolean isPreChallengeState,
            boolean isReauthState,
            boolean hadSuccessfulAuthLocal,
            int identifier,
            int counterForReauth,
            EapSimAkaTypeData eapSimAkaTypeData) {
        // EAP-SIM exchanges must not include more than one EAP-SIM notification round
        // (RFC 4186#6.1, RFC 4187#6.1)
        if (mHasReceivedSimAkaNotification) {
            return new EapError(
                    new EapInvalidRequestException("Received multiple EAP-SIM notifications"));
        }

        mHasReceivedSimAkaNotification = true;
        AtNotification atNotification =
                (AtNotification) eapSimAkaTypeData.attributeMap.get(EAP_AT_NOTIFICATION);

        LOG.d(
                tag,
                "Received AtNotification:"
                        + " S=" + (atNotification.isSuccessCode ? "1" : "0")
                        + " P=" + (atNotification.isPreSuccessfulChallenge ? "1" : "0")
                        + " Code=" + atNotification.notificationCode);

        // Notification with P bit being set is accepted in both before and in ChallengeState.
        // Specifically in ChallengeState, after client sends out the challenge response, it's
        // unclear to the client whether the challenge succeeds or not, and server might send at
        // most one AKA-Notification after that. Thus, P-0 should be accepted in ChallengeState.
        if (atNotification.isPreSuccessfulChallenge) {
            // AT_MAC attribute must not be included when the P bit is set (RFC 4186#9.8,
            // RFC 4187#9.10)
            if (eapSimAkaTypeData.attributeMap.containsKey(EAP_AT_MAC)) {
                return buildClientErrorResponse(
                        identifier, getEapMethod(), AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            return buildResponseMessage(
                    getEapMethod(), eapSimAkaTypeData.eapSubtype, identifier, Arrays.asList());
        }

        // Notification with an unset P bit MUST not be sent before the challenge exchange.
        if (isPreChallengeState) {
            return buildClientErrorResponse(
                    identifier, getEapMethod(), AtClientErrorCode.UNABLE_TO_PROCESS);
        }

        if (!eapSimAkaTypeData.attributeMap.containsKey(EAP_AT_MAC) || !hadSuccessfulAuthLocal) {
            // Zero P bit notification should be received after server authenticated & MAC must be
            // included in that notification. (RFC 4186#9.8, RFC 4187#9.10)
            return buildClientErrorResponse(
                    identifier, getEapMethod(), AtClientErrorCode.UNABLE_TO_PROCESS);
        }

        try {
            byte[] mac = getMac(EAP_CODE_REQUEST, identifier, eapSimAkaTypeData, new byte[0]);
            AtMac atMac = (AtMac) eapSimAkaTypeData.attributeMap.get(EAP_AT_MAC);
            if (!Arrays.equals(mac, atMac.mac)) {
                // MAC in message != calculated mac
                return buildClientErrorResponse(
                        identifier, getEapMethod(), AtClientErrorCode.UNABLE_TO_PROCESS);
            }

            if (!isReauthState) {
                // server has been authenticated, so we can send a response
                return buildResponseMessageWithMac(
                        identifier, eapSimAkaTypeData.eapSubtype, new byte[0]);
            } else {
                // AT_COUNTER attribute MUST be included in EAP-AKA notifications, if they are used
                // after successful authentication in order to provide replay protection.
                int receivedCounter = validateReauthAkaNotifyAndGetCounter(eapSimAkaTypeData);
                LOG.d(
                        tag,
                        "Counter in Notification: "
                                + receivedCounter
                                + ",  Expecting counter for reauth"
                                + counterForReauth);
                if (counterForReauth == receivedCounter) {
                    EapSimAkaAttribute.AtIv atIv = new EapSimAkaAttribute.AtIv(mSecureRandom);
                    List<EapSimAkaAttribute> attributeList =
                            buildReauthResponse(counterForReauth, false, mKEncr, atIv);
                    return buildResponseMessageWithMac(
                            identifier,
                            eapSimAkaTypeData.eapSubtype,
                            new byte[0],
                            attributeList,
                            null);
                } else {
                    return buildClientErrorResponse(
                            identifier, getEapMethod(), AtClientErrorCode.UNABLE_TO_PROCESS);
                }
            }
        } catch (EapSilentException | EapSimAkaInvalidAttributeException ex) {
            // We can't continue if the MAC can't be generated
            return new EapError(ex);
        }
    }

    abstract EapSimAkaTypeData getEapSimAkaTypeData(AtClientErrorCode clientErrorCode);
    abstract EapSimAkaTypeData getEapSimAkaTypeData(
            int eapSubtype, List<EapSimAkaAttribute> attributes);
}
