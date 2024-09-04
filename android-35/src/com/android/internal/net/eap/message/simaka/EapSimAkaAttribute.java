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

package com.android.internal.net.eap.message.simaka;

import static com.android.internal.net.eap.EapAuthenticator.LOG;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaInvalidAtPaddingException;
import com.android.internal.net.eap.exceptions.simaka.EapSimAkaInvalidAttributeException;
import com.android.internal.net.eap.exceptions.simaka.EapSimInvalidAtRandException;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * EapSimAkaAttribute represents a single EAP SIM/AKA Attribute.
 *
 * @see <a href="https://tools.ietf.org/html/rfc4186">RFC 4186, Extensible Authentication
 * Protocol for Subscriber Identity Modules (EAP-SIM)</a>
 * @see <a href="https://tools.ietf.org/html/rfc4187">RFC 4187, Extensible Authentication
 * Protocol for Authentication and Key Agreement (EAP-AKA)</a>
 * @see <a href="https://www.iana.org/assignments/eap-numbers/eap-numbers.xhtml">EAP SIM/AKA
 * Attributes</a>
 */
public abstract class EapSimAkaAttribute {
    static final int LENGTH_SCALING = 4;

    private static final int MIN_ATTR_LENGTH = 4;
    private static final int ATTR_HEADER_LEN = 4;

    public static final int SKIPPABLE_ATTRIBUTE_RANGE_START = 128;

    // EAP non-Skippable Attribute values defined by IANA
    // https://www.iana.org/assignments/eapsimaka-numbers/eapsimaka-numbers.xhtml
    public static final int EAP_AT_RAND = 1;
    public static final int EAP_AT_AUTN = 2;
    public static final int EAP_AT_RES = 3;
    public static final int EAP_AT_AUTS = 4;
    public static final int EAP_AT_PADDING = 6;
    public static final int EAP_AT_NONCE_MT = 7;
    public static final int EAP_AT_PERMANENT_ID_REQ = 10;
    public static final int EAP_AT_MAC = 11;
    public static final int EAP_AT_NOTIFICATION = 12;
    public static final int EAP_AT_ANY_ID_REQ = 13;
    public static final int EAP_AT_IDENTITY = 14;
    public static final int EAP_AT_VERSION_LIST = 15;
    public static final int EAP_AT_SELECTED_VERSION = 16;
    public static final int EAP_AT_FULLAUTH_ID_REQ = 17;
    public static final int EAP_AT_COUNTER = 19;
    public static final int EAP_AT_COUNTER_TOO_SMALL = 20;
    public static final int EAP_AT_NONCE_S = 21;
    public static final int EAP_AT_CLIENT_ERROR_CODE = 22;
    public static final int EAP_AT_KDF_INPUT = 23;
    public static final int EAP_AT_KDF = 24;

    // EAP Skippable Attribute values defined by IANA
    // https://www.iana.org/assignments/eapsimaka-numbers/eapsimaka-numbers.xhtml
    public static final int EAP_AT_IV = 129;
    public static final int EAP_AT_ENCR_DATA = 130;
    public static final int EAP_AT_NEXT_PSEUDONYM = 132;
    public static final int EAP_AT_NEXT_REAUTH_ID = 133;
    public static final int EAP_AT_CHECKCODE = 134;
    public static final int EAP_AT_RESULT_IND = 135;
    public static final int EAP_AT_BIDDING = 136;

    public static final Map<Integer, String> EAP_ATTRIBUTE_STRING = new HashMap<>();
    static {
        EAP_ATTRIBUTE_STRING.put(EAP_AT_RAND, "AT_RAND");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_AUTN, "AT_AUTN");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_RES, "AT_RES");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_AUTS, "AT_AUTS");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_PADDING, "AT_PADDING");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_NONCE_MT, "AT_NONCE_MT");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_PERMANENT_ID_REQ, "AT_PERMANENT_ID_REQ");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_MAC, "AT_MAC");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_NOTIFICATION, "AT_NOTIFICATION");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_ANY_ID_REQ, "AT_ANY_ID_REQ");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_IDENTITY, "AT_IDENTITY");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_VERSION_LIST, "AT_VERSION_LIST");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_SELECTED_VERSION, "AT_SELECTED_VERSION");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_FULLAUTH_ID_REQ, "AT_FULLAUTH_ID_REQ");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_COUNTER, "AT_COUNTER");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_COUNTER_TOO_SMALL, "AT_COUNTER_TOO_SMALL");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_NONCE_S, "AT_NONCE_S");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_CLIENT_ERROR_CODE, "AT_CLIENT_ERROR_CODE");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_KDF_INPUT, "AT_KDF_INPUT");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_KDF, "AT_KDF");

        EAP_ATTRIBUTE_STRING.put(EAP_AT_IV, "AT_IV");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_ENCR_DATA, "AT_ENCR_DATA");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_NEXT_PSEUDONYM, "AT_NEXT_PSEUDONYM");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_NEXT_REAUTH_ID, "AT_NEXT_REAUTH_ID");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_CHECKCODE, "AT_CHECKCODE");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_RESULT_IND, "AT_RESULT_IND");
        EAP_ATTRIBUTE_STRING.put(EAP_AT_BIDDING, "AT_BIDDING");
    }

    public final int attributeType;
    public final int lengthInBytes;

    protected EapSimAkaAttribute(int attributeType, int lengthInBytes)
            throws EapSimAkaInvalidAttributeException {
        this.attributeType = attributeType;
        this.lengthInBytes = lengthInBytes;

        if (lengthInBytes % LENGTH_SCALING != 0) {
            throw new EapSimAkaInvalidAttributeException("Attribute length must be multiple of 4");
        }
    }

    /**
     * Encodes this EapSimAkaAttribute into the given ByteBuffer
     *
     * @param byteBuffer the ByteBuffer that this instance will be written to
     */
    public abstract void encode(ByteBuffer byteBuffer);

    /**
     * EapSimAkaReservedBytesAttribute represents any EAP-SIM/AKA attribute that is of the format:
     *
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Attribute Type (1B)  |  Length (1B)  |  Reserved (2B)  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |  Value...
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     * <p>Note: This Attribute type ignores (but preserves) the Reserved bytes. This is needed for
     * calculating MACs in EAP-SIM/AKA.
     */
    protected abstract static class EapSimAkaReservedBytesAttribute extends EapSimAkaAttribute {
        protected static final int RESERVED_BYTES_LEN = 2;

        @VisibleForTesting public final byte[] reservedBytes = new byte[RESERVED_BYTES_LEN];

        protected EapSimAkaReservedBytesAttribute(
                int attributeType, int lengthInBytes, ByteBuffer buffer)
                throws EapSimAkaInvalidAttributeException {
            super(attributeType, lengthInBytes);

            try {
                buffer.get(reservedBytes);
            } catch (BufferUnderflowException e) {
                throw new EapSimAkaInvalidAttributeException("Invalid attribute length", e);
            }
        }

        protected EapSimAkaReservedBytesAttribute(int attributeType, int lengthInBytes)
                throws EapSimAkaInvalidAttributeException {
            super(attributeType, lengthInBytes);
        }

        protected EapSimAkaReservedBytesAttribute(
                int attributeType, int lengthInBytes, byte[] reservedBytes)
                throws EapSimAkaInvalidAttributeException {
            this(attributeType, lengthInBytes);

            if (reservedBytes.length != RESERVED_BYTES_LEN) {
                throw new EapSimAkaInvalidAttributeException("Invalid attribute length");
            }
            System.arraycopy(
                    reservedBytes,
                    0 /* srcPos */,
                    this.reservedBytes,
                    0 /* destPos */,
                    RESERVED_BYTES_LEN);
        }

        @Override
        public void encode(ByteBuffer buffer) {
            encodeAttributeHeader(buffer);

            buffer.put(reservedBytes);
        }
    }

    protected void encodeAttributeHeader(ByteBuffer byteBuffer) {
        byteBuffer.put((byte) attributeType);
        byteBuffer.put((byte) (lengthInBytes / LENGTH_SCALING));
    }

    void consumePadding(int bytesUsed, ByteBuffer byteBuffer) {
        int paddingRemaining = lengthInBytes - bytesUsed;
        byteBuffer.get(new byte[paddingRemaining]);
    }

    void addPadding(int bytesUsed, ByteBuffer byteBuffer) {
        int paddingNeeded = lengthInBytes - bytesUsed;
        byteBuffer.put(new byte[paddingNeeded]);
    }

    /**
     * EapSimAkaUnsupportedAttribute represents any unsupported, skippable EAP-SIM attribute.
     */
    public static class EapSimAkaUnsupportedAttribute extends EapSimAkaAttribute {
        // Attribute Type (1B) + Attribute Length (1B) = 2B Header
        private static final int HEADER_BYTES = 2;

        public final byte[] data;

        public EapSimAkaUnsupportedAttribute(
                int attributeType,
                int lengthInBytes,
                ByteBuffer byteBuffer) throws EapSimAkaInvalidAttributeException {
            super(attributeType, lengthInBytes);

            // Attribute not supported, but remaining attribute still needs to be saved
            int remainingBytes = lengthInBytes - HEADER_BYTES;
            data = new byte[remainingBytes];
            byteBuffer.get(data);
        }

        @VisibleForTesting
        public EapSimAkaUnsupportedAttribute(int attributeType, int lengthInBytes, byte[] data)
                throws EapSimAkaInvalidAttributeException {
            super(attributeType, lengthInBytes);
            this.data = data;
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);
            byteBuffer.put(data);
        }
    }

    /**
     * AtVersionList represents the AT_VERSION_LIST attribute defined in RFC 4186#10.2
     */
    public static class AtVersionList extends EapSimAkaAttribute {
        private static final int BYTES_PER_VERSION = 2;

        public final List<Integer> versions = new ArrayList<>();

        public AtVersionList(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_VERSION_LIST, lengthInBytes);

            // number of bytes used to represent list (RFC 4186 Section 10.2)
            int bytesInList = Short.toUnsignedInt(byteBuffer.getShort());
            if (bytesInList % BYTES_PER_VERSION != 0) {
                throw new EapSimAkaInvalidAttributeException(
                        "Actual Version List Length must be multiple of 2");
            }

            int numVersions =  bytesInList / BYTES_PER_VERSION;
            for (int i = 0; i < numVersions; i++) {
                versions.add(Short.toUnsignedInt(byteBuffer.getShort()));
            }

            int bytesUsed = MIN_ATTR_LENGTH + (BYTES_PER_VERSION * versions.size());
            consumePadding(bytesUsed, byteBuffer);
        }

        @VisibleForTesting
        public AtVersionList(int lengthInBytes, int... versions)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_VERSION_LIST, lengthInBytes);
            for (int version : versions) {
                this.versions.add(version);
            }
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);

            byteBuffer.putShort((short) (versions.size() * BYTES_PER_VERSION));
            for (int i : versions) {
                byteBuffer.putShort((short) i);
            }

            int bytesUsed = MIN_ATTR_LENGTH + (BYTES_PER_VERSION * versions.size());
            addPadding(bytesUsed, byteBuffer);
        }
    }

    /**
     * AtSelectedVersion represents the AT_SELECTED_VERSION attribute defined in RFC 4186#10.3
     */
    public static class AtSelectedVersion extends EapSimAkaAttribute {
        private static final String TAG = AtSelectedVersion.class.getSimpleName();
        private static final int LENGTH = LENGTH_SCALING;

        public static final int SUPPORTED_VERSION = 1;

        public final int selectedVersion;

        public AtSelectedVersion(int lengthInBytes, int selectedVersion)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_SELECTED_VERSION, LENGTH);
            this.selectedVersion = selectedVersion;

            if (lengthInBytes != LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Invalid Length specified");
            }
        }

        @VisibleForTesting
        public AtSelectedVersion(int selectedVersion) throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_SELECTED_VERSION, LENGTH);
            this.selectedVersion = selectedVersion;
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);
            byteBuffer.putShort((short) selectedVersion);
        }

        /**
         * Constructs and returns an AtSelectedVersion for the only supported version of EAP-SIM
         *
         * @return an AtSelectedVersion for the supported version (1) of EAP-SIM
         */
        public static AtSelectedVersion getSelectedVersion() {
            try {
                return new AtSelectedVersion(LENGTH, SUPPORTED_VERSION);
            } catch (EapSimAkaInvalidAttributeException ex) {
                // this should never happen
                LOG.wtf(TAG,
                        "Error thrown while creating AtSelectedVersion with correct length", ex);
                throw new AssertionError("Impossible exception encountered", ex);
            }
        }
    }

    /**
     * AtNonceMt represents the AT_NONCE_MT attribute defined in RFC 4186#10.4
     */
    public static class AtNonceMt extends EapSimAkaReservedBytesAttribute {
        private static final int LENGTH = 5 * LENGTH_SCALING;

        public static final int NONCE_MT_LENGTH = 16;

        public final byte[] nonceMt = new byte[NONCE_MT_LENGTH];

        public AtNonceMt(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_NONCE_MT, LENGTH, byteBuffer);
            if (lengthInBytes != LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Invalid Length specified");
            }

            byteBuffer.get(nonceMt);
        }

        @VisibleForTesting
        public AtNonceMt(byte[] nonceMt) throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_NONCE_MT, LENGTH);

            if (nonceMt.length != NONCE_MT_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("NonceMt length must be 16B");
            }
            System.arraycopy(nonceMt, 0, this.nonceMt, 0, NONCE_MT_LENGTH);
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            super.encode(byteBuffer);

            byteBuffer.put(nonceMt);
        }
    }

    private abstract static class AtIdReq extends EapSimAkaReservedBytesAttribute {
        private static final int ATTR_LENGTH = LENGTH_SCALING;

        protected AtIdReq(int lengthInBytes, int attributeType, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(attributeType, ATTR_LENGTH, byteBuffer);

            if (lengthInBytes != ATTR_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Invalid Length specified");
            }
        }

        @VisibleForTesting
        protected AtIdReq(int attributeType) throws EapSimAkaInvalidAttributeException {
            super(attributeType, ATTR_LENGTH);
        }
    }

    /**
     * AtPermanentIdReq represents the AT_PERMANENT_ID_REQ attribute defined in RFC 4186#10.5 and
     * RFC 4187#10.2
     */
    public static class AtPermanentIdReq extends AtIdReq {
        public AtPermanentIdReq(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(lengthInBytes, EAP_AT_PERMANENT_ID_REQ, byteBuffer);
        }

        @VisibleForTesting
        public AtPermanentIdReq() throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_PERMANENT_ID_REQ);
        }
    }

    /**
     * AtAnyIdReq represents the AT_ANY_ID_REQ attribute defined in RFC 4186#10.6 and RFC 4187#10.3
     */
    public static class AtAnyIdReq extends AtIdReq {
        public AtAnyIdReq(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(lengthInBytes, EAP_AT_ANY_ID_REQ, byteBuffer);
        }

        @VisibleForTesting
        public AtAnyIdReq() throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_ANY_ID_REQ);
        }
    }

    /**
     * AtFullauthIdReq represents the AT_FULLAUTH_ID_REQ attribute defined in RFC 4186#10.7 and RFC
     * 4187#10.4
     */
    public static class AtFullauthIdReq extends AtIdReq {
        public AtFullauthIdReq(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(lengthInBytes, EAP_AT_FULLAUTH_ID_REQ, byteBuffer);
        }

        @VisibleForTesting
        public AtFullauthIdReq() throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_FULLAUTH_ID_REQ);
        }
    }

    /**
     * AtIdentity represents the AT_IDENTITY attribute defined in RFC 4186#10.8 and RFC 4187#10.5
     */
    public static class AtIdentity extends EapSimAkaAttribute {
        public final byte[] identity;

        public AtIdentity(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_IDENTITY, lengthInBytes);

            int identityLength = Short.toUnsignedInt(byteBuffer.getShort());
            identity = new byte[identityLength];
            byteBuffer.get(identity);

            int bytesUsed = MIN_ATTR_LENGTH + identityLength;
            consumePadding(bytesUsed, byteBuffer);
        }

        @VisibleForTesting
        public AtIdentity(int lengthInBytes, byte[] identity)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_IDENTITY, lengthInBytes);
            this.identity = identity;
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);
            byteBuffer.putShort((short) identity.length);
            byteBuffer.put(identity);

            int bytesUsed = MIN_ATTR_LENGTH + identity.length;
            addPadding(bytesUsed, byteBuffer);
        }

        /**
         * Creates and returns an AtIdentity instance for the given identity.
         *
         * @param identity byte-array representing the identity for the AtIdentity
         * @return AtIdentity instance for the given identity byte-array
         */
        public static AtIdentity getAtIdentity(byte[] identity)
                throws EapSimAkaInvalidAttributeException {
            int lengthInBytes = MIN_ATTR_LENGTH + identity.length;
            if (lengthInBytes % LENGTH_SCALING != 0) {
                lengthInBytes += LENGTH_SCALING - (lengthInBytes % LENGTH_SCALING);
            }

            return new AtIdentity(lengthInBytes, identity);
        }
    }

    /**
     * AtRandSim represents the AT_RAND attribute for EAP-SIM defined in RFC 4186#10.9
     */
    public static class AtRandSim extends EapSimAkaReservedBytesAttribute {
        private static final int RAND_LENGTH = 16;
        private static final int MIN_RANDS = 2;
        private static final int MAX_RANDS = 3;

        public final List<byte[]> rands = new ArrayList<>(MAX_RANDS);

        public AtRandSim(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_RAND, lengthInBytes, byteBuffer);

            int numRands = (lengthInBytes - MIN_ATTR_LENGTH) / RAND_LENGTH;
            if (!isValidNumRands(numRands)) {
                throw new EapSimInvalidAtRandException("Unexpected number of rands: " + numRands);
            }

            for (int i = 0; i < numRands; i++) {
                byte[] rand = new byte[RAND_LENGTH];
                byteBuffer.get(rand);

                // check for rand being unique (RFC 4186 Section 10.9)
                for (int j = 0; j < i; j++) {
                    byte[] otherRand = rands.get(j);
                    if (Arrays.equals(rand, otherRand)) {
                        throw new EapSimAkaInvalidAttributeException("Received identical RANDs");
                    }
                }
                rands.add(rand);
            }
        }

        @VisibleForTesting
        public AtRandSim(int lengthInBytes, byte[]... rands)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_RAND, lengthInBytes);

            if (!isValidNumRands(rands.length)) {
                throw new EapSimInvalidAtRandException("Unexpected number of rands: "
                        + rands.length);
            }
            for (byte[] rand : rands) {
                this.rands.add(rand);
            }
        }

        private boolean isValidNumRands(int numRands) {
            // numRands is valid iff 2 <= numRands <= 3
            return MIN_RANDS <= numRands && numRands <= MAX_RANDS;
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            super.encode(byteBuffer);

            for (byte[] rand : rands) {
                byteBuffer.put(rand);
            }
        }
    }

    /**
     * AtRandAka represents the AT_RAND attribute for EAP-AKA defined in RFC 4187#10.6
     */
    public static class AtRandAka extends EapSimAkaReservedBytesAttribute {
        private static final int ATTR_LENGTH = 5 * LENGTH_SCALING;
        private static final int RAND_LENGTH = 16;

        public final byte[] rand = new byte[RAND_LENGTH];

        public AtRandAka(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_RAND, lengthInBytes, byteBuffer);

            if (lengthInBytes != ATTR_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Length must be 20B");
            }

            byteBuffer.get(rand);
        }

        @VisibleForTesting
        public AtRandAka(byte[] rand)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_RAND, ATTR_LENGTH);

            if (rand.length != RAND_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Rand must be 16B");
            }

            System.arraycopy(rand, 0, this.rand, 0, RAND_LENGTH);
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            super.encode(byteBuffer);

            byteBuffer.put(rand);
        }
    }

    /**
     * AtPadding represents the AT_PADDING attribute defined in RFC 4186#10.12 and RFC 4187#10.12
     */
    public static class AtPadding extends EapSimAkaAttribute {
        private static final int ATTR_HEADER = 2;

        public AtPadding(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_PADDING, lengthInBytes);

            int remainingBytes = lengthInBytes - ATTR_HEADER;
            for (int i = 0; i < remainingBytes; i++) {
                // Padding must be checked to all be 0x00 bytes (RFC 4186 Section 10.12)
                if (byteBuffer.get() != 0) {
                    throw new EapSimAkaInvalidAtPaddingException("Padding bytes must all be 0x00");
                }
            }
        }

        @VisibleForTesting
        public AtPadding(int lengthInBytes) throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_PADDING, lengthInBytes);
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);

            addPadding(ATTR_HEADER, byteBuffer);
        }
    }

    /**
     * AtMac represents the AT_MAC attribute defined in RFC 4186#10.14 and RFC 4187#10.15
     */
    public static class AtMac extends EapSimAkaReservedBytesAttribute {
        private static final int ATTR_LENGTH = 5 * LENGTH_SCALING;

        public static final int MAC_LENGTH = 4 * LENGTH_SCALING;

        public final byte[] mac;

        public AtMac(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_MAC, lengthInBytes, byteBuffer);

            if (lengthInBytes != ATTR_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Invalid Length specified");
            }

            mac = new byte[MAC_LENGTH];
            byteBuffer.get(mac);
        }

        // Constructs an AtMac with an empty MAC and empty RESERVED bytes. Should only be used for
        // calculating MACs in outbound messages.
        public AtMac() throws EapSimAkaInvalidAttributeException {
            this(new byte[MAC_LENGTH]);
        }

        public AtMac(byte[] mac) throws EapSimAkaInvalidAttributeException {
            this(new byte[RESERVED_BYTES_LEN], mac);
        }

        @VisibleForTesting
        public AtMac(byte[] reservedBytes, byte[] mac) throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_MAC, ATTR_LENGTH, reservedBytes);

            if (mac.length != MAC_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Invalid length for MAC");
            }
            this.mac = mac;
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            super.encode(byteBuffer);

            byteBuffer.put(mac);
        }

        /**
         * Returns a copy of this AtMac with the MAC cleared (and the reserved bytes preserved).
         *
         * <p>Per RFC 4186 Section 10.14, the MAC should be calculated over the entire packet, with
         * the value field of the MAC attribute set to zero.
         */
        public AtMac getAtMacWithMacCleared() throws EapSimAkaInvalidAttributeException {
            return new AtMac(reservedBytes, new byte[MAC_LENGTH]);
        }
    }

    /**
     * AtCounter represents the AT_COUNTER attribute defined in RFC 4186#10.15 and RFC 4187#10.16
     */
    public static class AtCounter extends EapSimAkaAttribute {
        private static final int ATTR_LENGTH = LENGTH_SCALING;

        public final int counter;

        public AtCounter(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_COUNTER, lengthInBytes);

            if (lengthInBytes != ATTR_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Invalid Length specified");
            }

            this.counter = Short.toUnsignedInt(byteBuffer.getShort());
        }

        @VisibleForTesting
        public AtCounter(int counter) throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_COUNTER, ATTR_LENGTH);
            this.counter = counter;
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);
            byteBuffer.putShort((short) counter);
        }
    }


    /**
     * AtCounterTooSmall represents the AT_COUNTER_TOO_SMALL attribute defined in RFC 4186#10.16 and
     * RFC 4187#10.17
     */
    public static class AtCounterTooSmall extends EapSimAkaAttribute {
        private static final int ATTR_LENGTH = LENGTH_SCALING;
        private static final int ATTR_HEADER = 2;

        public AtCounterTooSmall(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_COUNTER_TOO_SMALL, lengthInBytes);

            if (lengthInBytes != ATTR_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Invalid Length specified");
            }
            consumePadding(ATTR_HEADER, byteBuffer);
        }

        public AtCounterTooSmall() throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_COUNTER_TOO_SMALL, ATTR_LENGTH);
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);
            addPadding(ATTR_HEADER, byteBuffer);
        }
    }

    /**
     * AtNonceS represents the AT_NONCE_S attribute defined in RFC 4186#10.17 and RFC 4187#10.18
     *
     * <p>This Nonce is generated by the server and used for fast re-authentication only.
     */
    public static class AtNonceS extends EapSimAkaReservedBytesAttribute {
        private static final int ATTR_LENGTH = 5 * LENGTH_SCALING;
        private static final int NONCE_S_LENGTH = 4 * LENGTH_SCALING;

        public final byte[] nonceS = new byte[NONCE_S_LENGTH];

        public AtNonceS(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_NONCE_S, lengthInBytes, byteBuffer);

            if (lengthInBytes != ATTR_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Invalid Length specified");
            }

            byteBuffer.get(nonceS);
        }

        @VisibleForTesting
        public AtNonceS(byte[] nonceS) throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_NONCE_S, ATTR_LENGTH);

            if (nonceS.length != NONCE_S_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("NonceS length must be 16B");
            }

            System.arraycopy(nonceS, 0, this.nonceS, 0, NONCE_S_LENGTH);
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            super.encode(byteBuffer);

            byteBuffer.put(nonceS);
        }
    }

    /**
     * AtNotification represents the AT_NOTIFICATION attribute defined in RFC 4186#10.18 and RFC
     * 4187#10.19
     */
    public static class AtNotification extends EapSimAkaAttribute {
        private static final int ATTR_LENGTH = 4;
        private static final int SUCCESS_MASK = 0x8000;
        private static final int PRE_SUCCESSFUL_CHALLENGE_MASK = 0x4000;

        // Notification codes defined in RFC 4186 Section 10.18
        public static final int GENERAL_FAILURE_POST_CHALLENGE = 0;
        public static final int GENERAL_FAILURE_PRE_CHALLENGE = 16384; // 0x4000
        public static final int SUCCESS = 32768; // 0x8000
        public static final int DENIED_ACCESS_POST_CHALLENGE = 1026;
        public static final int USER_NOT_SUBSCRIBED_POST_CHALLENGE = 1031;

        private static final Map<Integer, String> CODE_DEFS = loadCodeDefs();

        public final boolean isSuccessCode;
        public final boolean isPreSuccessfulChallenge;
        public final int notificationCode;

        public AtNotification(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_NOTIFICATION, lengthInBytes);

            if (lengthInBytes != ATTR_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Invalid Length specified");
            }

            notificationCode = Short.toUnsignedInt(byteBuffer.getShort());

            // If Success bit == 0, failure is implied
            isSuccessCode = (notificationCode & SUCCESS_MASK) != 0;

            // if Phase bit == 0, notification code can only be used after a successful
            isPreSuccessfulChallenge = (notificationCode & PRE_SUCCESSFUL_CHALLENGE_MASK) != 0;

            if (isSuccessCode && isPreSuccessfulChallenge) {
                throw new EapSimAkaInvalidAttributeException("Invalid state specified");
            }
        }

        @VisibleForTesting
        public AtNotification(int notificationCode) throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_NOTIFICATION, ATTR_LENGTH);
            this.notificationCode = notificationCode;

            // If Success bit == 0, failure is implied
            isSuccessCode = (notificationCode & SUCCESS_MASK) != 0;

            // if Phase bit == 0, notification code can only be used after a successful challenge
            isPreSuccessfulChallenge = (notificationCode & PRE_SUCCESSFUL_CHALLENGE_MASK) != 0;

            if (isSuccessCode && isPreSuccessfulChallenge) {
                throw new EapSimAkaInvalidAttributeException("Invalid state specified");
            }
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);
            byteBuffer.putShort((short) notificationCode);
        }

        @Override
        public String toString() {
            String description = CODE_DEFS.getOrDefault(notificationCode, "Code not recognized");
            return "{Notification Code=" + notificationCode + ", descr=" + description + "}";
        }

        private static Map<Integer, String> loadCodeDefs() {
            Map<Integer, String> defs = new HashMap<>();
            defs.put(GENERAL_FAILURE_POST_CHALLENGE,
                    "General failure after authentication. (Implies failure, used after successful"
                    + " authentication.)");
            defs.put(GENERAL_FAILURE_PRE_CHALLENGE,
                    "General failure. (Implies failure, used before authentication.)");
            defs.put(SUCCESS,
                    "Success.  User has been successfully authenticated. (Does not imply failure,"
                    + " used after successful authentication).");
            defs.put(DENIED_ACCESS_POST_CHALLENGE,
                    "User has been temporarily denied access to the requested service. (Implies"
                    + " failure, used after successful authentication.)");
            defs.put(USER_NOT_SUBSCRIBED_POST_CHALLENGE,
                    "User has not subscribed to the requested service.  (Implies failure, used"
                    + " after successful authentication.)");
            return defs;
        }
    }

    /**
     * AtClientErrorCode represents the AT_CLIENT_ERROR_CODE attribute defined in RFC 4186#10.19 and
     * RFC 4187#10.20
     */
    public static class AtClientErrorCode extends EapSimAkaAttribute {
        private static final String TAG = AtClientErrorCode.class.getSimpleName();
        private static final int ATTR_LENGTH = 4;

        // Error codes defined in RFC 4186 Section 10.19
        public static final AtClientErrorCode UNABLE_TO_PROCESS = getClientErrorCode(0);
        public static final AtClientErrorCode UNSUPPORTED_VERSION = getClientErrorCode(1);
        public static final AtClientErrorCode INSUFFICIENT_CHALLENGES = getClientErrorCode(2);
        public static final AtClientErrorCode STALE_RANDS = getClientErrorCode(3);

        public final int errorCode;

        public AtClientErrorCode(int lengthInBytes, int errorCode)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_CLIENT_ERROR_CODE, lengthInBytes);

            if (lengthInBytes != ATTR_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Invalid Length specified");
            }

            this.errorCode = errorCode;
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);
            byteBuffer.putShort((short) errorCode);
        }

        private static AtClientErrorCode getClientErrorCode(int errorCode) {
            try {
                return new AtClientErrorCode(ATTR_LENGTH, errorCode);
            } catch (EapSimAkaInvalidAttributeException exception) {
                LOG.wtf(TAG, "Exception thrown while making AtClientErrorCodeConstants");
                return null;
            }
        }
    }

    /**
     * AtAutn represents the AT_AUTN attribute defined in RFC 4187#10.7
     */
    public static class AtAutn extends EapSimAkaReservedBytesAttribute {
        private static final int ATTR_LENGTH = 5 * LENGTH_SCALING;
        private static final int AUTN_LENGTH = 16;

        public final byte[] autn = new byte[AUTN_LENGTH];

        public AtAutn(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_AUTN, lengthInBytes, byteBuffer);

            if (lengthInBytes != ATTR_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Length must be 20B");
            }

            byteBuffer.get(autn);
        }

        @VisibleForTesting
        public AtAutn(byte[] autn) throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_AUTN, ATTR_LENGTH);

            if (autn.length != AUTN_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Autn must be 16B");
            }

            System.arraycopy(autn, 0, this.autn, 0, AUTN_LENGTH);
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            super.encode(byteBuffer);

            byteBuffer.put(autn);
        }
    }

    /**
     * AtRes respresents the AT_RES attribute defined in RFC 4187#10.8
     */
    public static class AtRes extends EapSimAkaAttribute {
        private static final int BITS_PER_BYTE = 8;
        private static final int MIN_RES_LEN_BYTES = 4;
        private static final int MAX_RES_LEN_BYTES = 16;

        public final byte[] res;

        public AtRes(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_RES, lengthInBytes);

            // RES length is in bits (RFC 4187#10.8).
            // RES length should be a multiple of 8 bits (TS 133 105#5.1.7.8)
            int resLength = Short.toUnsignedInt(byteBuffer.getShort());
            if (resLength % BITS_PER_BYTE != 0) {
                throw new EapSimAkaInvalidAttributeException("RES length must be multiple of 8");
            }
            int resLengthBytes = resLength / BITS_PER_BYTE;
            if (resLengthBytes < MIN_RES_LEN_BYTES || resLengthBytes > MAX_RES_LEN_BYTES) {
                throw new EapSimAkaInvalidAttributeException(
                        "RES length must be: 4B <= len <= 16B");
            }

            res = new byte[resLengthBytes];
            byteBuffer.get(res);

            int bytesUsed = MIN_ATTR_LENGTH + resLengthBytes;
            consumePadding(bytesUsed, byteBuffer);
        }

        @VisibleForTesting
        public AtRes(int lengthInBytes, byte[] res) throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_RES, lengthInBytes);

            if (res.length < MIN_RES_LEN_BYTES || res.length > MAX_RES_LEN_BYTES) {
                throw new EapSimAkaInvalidAttributeException(
                        "RES length must be: 4B <= len <= 16B");
            }

            this.res = res;
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);

            int resLenBits = res.length * BITS_PER_BYTE;
            byteBuffer.putShort((short) resLenBits);
            byteBuffer.put(res);

            int bytesUsed = MIN_ATTR_LENGTH + res.length;
            addPadding(bytesUsed, byteBuffer);
        }

        /**
         * Creates and returns an AtRes instance with the given res value.
         *
         * @param res byte-array RES value to be used for this
         * @return AtRes instance for the given RES value
         * @throws EapSimAkaInvalidAttributeException if the given res value has an invalid length
         */
        public static AtRes getAtRes(byte[] res) throws EapSimAkaInvalidAttributeException {
            // Attributes must be 4B-aligned, so there can be 0 to 3 padding bytes added
            int resLenBytes = MIN_ATTR_LENGTH + res.length;
            if (resLenBytes % LENGTH_SCALING != 0) {
                resLenBytes += LENGTH_SCALING - (resLenBytes % LENGTH_SCALING);
            }

            return new AtRes(resLenBytes, res);
        }

        /**
         * Checks whether the given RES length is valid.
         *
         * @param resLenBytes the RES length to be checked
         * @return true iff the given resLen is valid
         */
        public static boolean isValidResLen(int resLenBytes) {
            return resLenBytes >= MIN_RES_LEN_BYTES && resLenBytes <= MAX_RES_LEN_BYTES;
        }
    }

    /**
     * AtAuts represents the AT_AUTS attribute defined in RFC 4187#10.9
     */
    public static class AtAuts extends EapSimAkaAttribute {
        private static final int ATTR_LENGTH = 4 * LENGTH_SCALING;
        public static final int AUTS_LENGTH = 14;

        public final byte[] auts = new byte[AUTS_LENGTH];

        public AtAuts(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_AUTS, lengthInBytes);

            if (lengthInBytes != ATTR_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Length must be 16B");
            }

            byteBuffer.get(auts);
        }

        public AtAuts(byte[] auts) throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_AUTS, ATTR_LENGTH);

            if (auts.length != AUTS_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Auts must be 14B");
            }

            System.arraycopy(auts, 0, this.auts, 0, AUTS_LENGTH);
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);

            byteBuffer.put(auts);
        }
    }

    /**
     * AtKdfInput represents the AT_KDF_INPUT attribute defined in RFC 5448#3.1
     */
    public static class AtKdfInput extends EapSimAkaAttribute {
        public final byte[] networkName;

        public AtKdfInput(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_KDF_INPUT, lengthInBytes);

            int networkNameLength = Short.toUnsignedInt(byteBuffer.getShort());
            networkName = new byte[networkNameLength];
            byteBuffer.get(networkName);

            int bytesUsed = MIN_ATTR_LENGTH + networkNameLength;
            consumePadding(bytesUsed, byteBuffer);
        }

        @VisibleForTesting
        public AtKdfInput(int lengthInbytes, byte[] networkName)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_KDF_INPUT, lengthInbytes);

            this.networkName = networkName;
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);
            byteBuffer.putShort((short) networkName.length);
            byteBuffer.put(networkName);

            int bytesUsed = MIN_ATTR_LENGTH + networkName.length;
            addPadding(bytesUsed, byteBuffer);
        }
    }

    /**
     * AdKdf represents the AT_KDF attribute defined in RFC 5448#3.2
     */
    public static class AtKdf extends EapSimAkaAttribute {
        private static final int ATTR_LENGTH = MIN_ATTR_LENGTH;

        public final int kdf;

        public AtKdf(int lengthInBytes, ByteBuffer buffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_KDF, lengthInBytes);

            if (lengthInBytes != ATTR_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("AtKdf length must be 4B");
            }

            kdf = Short.toUnsignedInt(buffer.getShort());
        }

        @VisibleForTesting
        public AtKdf(int kdf) throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_KDF, ATTR_LENGTH);

            this.kdf = kdf;
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);

            byteBuffer.putShort((short) kdf);
        }
    }

    /**
     * AtBidding represents the AT_BIDDING attribute defined in RFC 5448#4
     */
    public static class AtBidding extends EapSimAkaAttribute {
        private static final int ATTR_LENGTH = MIN_ATTR_LENGTH;
        private static final int SUPPORTS_EAP_AKA_PRIME_MASK = 0x8000;

        public final boolean doesServerSupportEapAkaPrime;

        public AtBidding(int lengthInBytes, ByteBuffer buffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_BIDDING, lengthInBytes);

            if (lengthInBytes != ATTR_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("AtBidding length must be 4B");
            }

            int serverFlag = Short.toUnsignedInt(buffer.getShort());
            doesServerSupportEapAkaPrime = (serverFlag & SUPPORTS_EAP_AKA_PRIME_MASK) != 0;
        }

        @VisibleForTesting
        public AtBidding(boolean doesServerSupportEapAkaPrime)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_BIDDING, ATTR_LENGTH);

            this.doesServerSupportEapAkaPrime = doesServerSupportEapAkaPrime;
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);

            int flagToWrite = doesServerSupportEapAkaPrime ? SUPPORTS_EAP_AKA_PRIME_MASK : 0;
            byteBuffer.putShort((short) flagToWrite);
        }
    }

    /** AtIv represents the AT_IV attribute defined in RFC 4187#10.12 */
    public static class AtIv extends EapSimAkaReservedBytesAttribute {
        private static final int ATTR_LENGTH = 5 * LENGTH_SCALING;

        /** EAP-AKA uses AES-CBC,so IV is 128bit(16B) */
        private static final int IV_LENGTH = 16;

        public final byte[] iv = new byte[IV_LENGTH];

        public AtIv(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_IV, ATTR_LENGTH, byteBuffer);
            if (lengthInBytes != ATTR_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("Invalid Length, AtIv must be 20B");
            }

            byteBuffer.get(iv);
        }

        public AtIv(SecureRandom secureRandom) throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_IV, ATTR_LENGTH);
            secureRandom.nextBytes(iv);
            if (iv.length != IV_LENGTH) {
                throw new EapSimAkaInvalidAttributeException("IV length must be 16B");
            }
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            super.encode(byteBuffer);

            byteBuffer.put(iv);
        }
    }

    /** AtEncrData represents the AT_ENCR_DATA attribute defined in RFC 4187#10.12 */
    public static class AtEncrData extends EapSimAkaReservedBytesAttribute {
        private static final String CIPHER_ALGORITHM = "AES_128/CBC/NoPadding";
        public static final int CIPHER_BLOCK_LENGTH = 16;

        public final byte[] encrData;

        public AtEncrData(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_ENCR_DATA, lengthInBytes, byteBuffer);
            int encrDataLength = lengthInBytes - ATTR_HEADER_LEN;
            if (encrDataLength % CIPHER_BLOCK_LENGTH != 0) {
                throw new EapSimAkaInvalidAttributeException(
                        "encrData len needs to be multiple of 16B");
            }
            encrData = new byte[encrDataLength];
            byteBuffer.get(encrData);
        }

        public AtEncrData(byte[] plainData, byte[] key, byte[] iv)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_ENCR_DATA, plainData.length + ATTR_HEADER_LEN);
            if (plainData.length % CIPHER_BLOCK_LENGTH != 0) {
                throw new EapSimAkaInvalidAttributeException(
                        "encrData len needs to be multiple of 16B");
            }
            this.encrData = doCipherOperation(plainData, key, iv, Cipher.ENCRYPT_MODE);
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            super.encode(byteBuffer);

            byteBuffer.put(encrData);
        }

        /**
         * getDecryptedData returns decrypted data of AT_ENCR_DATA.
         *
         * @param key K_encr with byte array
         * @parma iv IV from AT_IV
         * @return decrypted data with byte array
         */
        public byte[] getDecryptedData(byte[] key, byte[] iv)
                throws EapSimAkaInvalidAttributeException {
            byte[] decryptedEncr = doCipherOperation(encrData, key, iv, Cipher.DECRYPT_MODE);
            return decryptedEncr;
        }

        private byte[] doCipherOperation(byte[] inputBytes, byte[] key, byte[] iv, int opmode)
                throws EapSimAkaInvalidAttributeException {
            Cipher cipherAlgorithm;
            try {
                cipherAlgorithm = Cipher.getInstance(CIPHER_ALGORITHM);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                throw new EapSimAkaInvalidAttributeException(
                        "Failed to construct Cihper for EAP SIMAKA");
            }
            try {
                SecretKeySpec secretKey = new SecretKeySpec(key, CIPHER_ALGORITHM);
                IvParameterSpec ivParam = new IvParameterSpec(iv);
                cipherAlgorithm.init(opmode, secretKey, ivParam);
                ByteBuffer inputBuffer = ByteBuffer.wrap(inputBytes);
                ByteBuffer outputBuffer = ByteBuffer.allocate(inputBytes.length);
                cipherAlgorithm.doFinal(inputBuffer, outputBuffer);
                return outputBuffer.array();
            } catch (InvalidKeyException
                    | InvalidAlgorithmParameterException
                    | BadPaddingException
                    | ShortBufferException
                    | IllegalBlockSizeException e) {
                throw new EapSimAkaInvalidAttributeException("Failed to decrypt data: ", e);
            }
        }
    }

    /** AtNextReauthId represents the AT_NEXT_REAUTH_ID attribute defined in RFC 4187#10.11 */
    public static class AtNextReauthId extends EapSimAkaAttribute {
        private static final String TAG = AtNextReauthId.class.getSimpleName();

        public final byte[] reauthId;

        public AtNextReauthId(int lengthInBytes, ByteBuffer byteBuffer)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_NEXT_REAUTH_ID, lengthInBytes);
            int identityLength = Short.toUnsignedInt(byteBuffer.getShort());
            reauthId = new byte[identityLength];

            byteBuffer.get(reauthId);
            StringBuilder builder = new StringBuilder();
            for (byte data : reauthId) {
                builder.append(String.format("%02X ", data));
            }
            LOG.d(TAG, "Next re-authId:" + builder);

            int bytesUsed = ATTR_HEADER_LEN + identityLength;
            consumePadding(bytesUsed, byteBuffer);
        }

        private AtNextReauthId(int lengthInBytes, byte[] identity)
                throws EapSimAkaInvalidAttributeException {
            super(EAP_AT_NEXT_REAUTH_ID, lengthInBytes);
            this.reauthId = identity;
        }

        @Override
        public void encode(ByteBuffer byteBuffer) {
            encodeAttributeHeader(byteBuffer);
            byteBuffer.putShort((short) reauthId.length);
            byteBuffer.put(reauthId);

            int bytesUsed = ATTR_HEADER_LEN + reauthId.length;
            addPadding(bytesUsed, byteBuffer);
        }

        /**
         * Creates and returns an AtNextReauthId instance for the given identity.
         *
         * @param identity byte-array representing the identity for the AtNextReauthId
         * @return AtNextReauthId instance for the given identity byte-array
         */
        @VisibleForTesting
        public static AtNextReauthId getAtNextReauthId(byte[] identity)
                throws EapSimAkaInvalidAttributeException {
            int lengthInBytes = ATTR_HEADER_LEN + identity.length;
            if (lengthInBytes % LENGTH_SCALING != 0) {
                lengthInBytes += LENGTH_SCALING - (lengthInBytes % LENGTH_SCALING);
            }

            return new AtNextReauthId(lengthInBytes, identity);
        }
    }
}
