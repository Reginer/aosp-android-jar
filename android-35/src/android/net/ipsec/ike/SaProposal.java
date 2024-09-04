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

package android.net.ipsec.ike;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.PersistableBundle;
import android.util.Pair;
import android.util.SparseArray;

import com.android.internal.net.ipsec.ike.message.IkePayload;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.DhGroupTransform;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.EncryptionTransform;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.IntegrityTransform;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.PrfTransform;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.Transform;
import com.android.modules.utils.build.SdkLevel;
import com.android.server.vcn.util.PersistableBundleUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * SaProposal represents a proposed configuration to negotiate an IKE or Child SA.
 *
 * <p>SaProposal will contain cryptograhic algorithms and key generation materials for the
 * negotiation of an IKE or Child SA.
 *
 * <p>User must provide at least one valid SaProposal when they are creating a new IKE or Child SA.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public abstract class SaProposal {
    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        ENCRYPTION_ALGORITHM_3DES,
        ENCRYPTION_ALGORITHM_AES_CBC,
        ENCRYPTION_ALGORITHM_AES_CTR,
        ENCRYPTION_ALGORITHM_AES_GCM_8,
        ENCRYPTION_ALGORITHM_AES_GCM_12,
        ENCRYPTION_ALGORITHM_AES_GCM_16,
        ENCRYPTION_ALGORITHM_CHACHA20_POLY1305
    })
    public @interface EncryptionAlgorithm {}

    /** 3DES Encryption/Ciphering Algorithm. */
    public static final int ENCRYPTION_ALGORITHM_3DES = 3;
    /** AES-CBC Encryption/Ciphering Algorithm. */
    public static final int ENCRYPTION_ALGORITHM_AES_CBC = 12;
    /** AES-CTR Encryption/Ciphering Algorithm. */
    public static final int ENCRYPTION_ALGORITHM_AES_CTR = 13;
    /**
     * AES-GCM Authentication/Integrity + Encryption/Ciphering Algorithm with 8-octet ICV
     * (truncation).
     */
    public static final int ENCRYPTION_ALGORITHM_AES_GCM_8 = 18;
    /**
     * AES-GCM Authentication/Integrity + Encryption/Ciphering Algorithm with 12-octet ICV
     * (truncation).
     */
    public static final int ENCRYPTION_ALGORITHM_AES_GCM_12 = 19;
    /**
     * AES-GCM Authentication/Integrity + Encryption/Ciphering Algorithm with 16-octet ICV
     * (truncation).
     */
    public static final int ENCRYPTION_ALGORITHM_AES_GCM_16 = 20;
    /**
     * ChaCha20-Poly1305 Authentication/Integrity + Encryption/Ciphering Algorithm with 16-octet ICV
     * (truncation).
     */
    public static final int ENCRYPTION_ALGORITHM_CHACHA20_POLY1305 = 28;

    /** @hide */
    protected static final SparseArray<String> SUPPORTED_ENCRYPTION_ALGO_TO_STR;

    static {
        SUPPORTED_ENCRYPTION_ALGO_TO_STR = new SparseArray<>();
        SUPPORTED_ENCRYPTION_ALGO_TO_STR.put(ENCRYPTION_ALGORITHM_3DES, "ENCR_3DES");
        SUPPORTED_ENCRYPTION_ALGO_TO_STR.put(ENCRYPTION_ALGORITHM_AES_CBC, "ENCR_AES_CBC");
        SUPPORTED_ENCRYPTION_ALGO_TO_STR.put(ENCRYPTION_ALGORITHM_AES_CTR, "ENCR_AES_CTR");
        SUPPORTED_ENCRYPTION_ALGO_TO_STR.put(ENCRYPTION_ALGORITHM_AES_GCM_8, "ENCR_AES_GCM_8");
        SUPPORTED_ENCRYPTION_ALGO_TO_STR.put(ENCRYPTION_ALGORITHM_AES_GCM_12, "ENCR_AES_GCM_12");
        SUPPORTED_ENCRYPTION_ALGO_TO_STR.put(ENCRYPTION_ALGORITHM_AES_GCM_16, "ENCR_AES_GCM_16");
        SUPPORTED_ENCRYPTION_ALGO_TO_STR.put(
                ENCRYPTION_ALGORITHM_CHACHA20_POLY1305, "ENCR_CHACHA20_POLY1305");
    }

    /**
     * Key length unused.
     *
     * <p>This value should only be used with the Encryption/Ciphering Algorithm that accepts a
     * fixed key size such as {@link #ENCRYPTION_ALGORITHM_3DES}.
     */
    public static final int KEY_LEN_UNUSED = 0;
    /** AES Encryption/Ciphering Algorithm key length 128 bits. */
    public static final int KEY_LEN_AES_128 = 128;
    /** AES Encryption/Ciphering Algorithm key length 192 bits. */
    public static final int KEY_LEN_AES_192 = 192;
    /** AES Encryption/Ciphering Algorithm key length 256 bits. */
    public static final int KEY_LEN_AES_256 = 256;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        PSEUDORANDOM_FUNCTION_HMAC_SHA1,
        PSEUDORANDOM_FUNCTION_AES128_XCBC,
        PSEUDORANDOM_FUNCTION_SHA2_256,
        PSEUDORANDOM_FUNCTION_SHA2_384,
        PSEUDORANDOM_FUNCTION_SHA2_512,
        PSEUDORANDOM_FUNCTION_AES128_CMAC
    })
    public @interface PseudorandomFunction {}

    /** HMAC-SHA1 Pseudorandom Function. */
    public static final int PSEUDORANDOM_FUNCTION_HMAC_SHA1 = 2;
    /** AES128-XCBC Pseudorandom Function. */
    public static final int PSEUDORANDOM_FUNCTION_AES128_XCBC = 4;
    /** HMAC-SHA2-256 Pseudorandom Function. */
    public static final int PSEUDORANDOM_FUNCTION_SHA2_256 = 5;
    /** HMAC-SHA2-384 Pseudorandom Function. */
    public static final int PSEUDORANDOM_FUNCTION_SHA2_384 = 6;
    /** HMAC-SHA2-384 Pseudorandom Function. */
    public static final int PSEUDORANDOM_FUNCTION_SHA2_512 = 7;
    /** AES128-CMAC Pseudorandom Function. */
    public static final int PSEUDORANDOM_FUNCTION_AES128_CMAC = 8;

    /** @hide */
    protected static final SparseArray<String> SUPPORTED_PRF_TO_STR;

    static {
        SUPPORTED_PRF_TO_STR = new SparseArray<>();
        SUPPORTED_PRF_TO_STR.put(PSEUDORANDOM_FUNCTION_HMAC_SHA1, "PRF_HMAC_SHA1");
        SUPPORTED_PRF_TO_STR.put(PSEUDORANDOM_FUNCTION_AES128_XCBC, "PRF_AES128_XCBC");
        SUPPORTED_PRF_TO_STR.put(PSEUDORANDOM_FUNCTION_SHA2_256, "PRF_HMAC2_256");
        SUPPORTED_PRF_TO_STR.put(PSEUDORANDOM_FUNCTION_SHA2_384, "PRF_HMAC2_384");
        SUPPORTED_PRF_TO_STR.put(PSEUDORANDOM_FUNCTION_SHA2_512, "PRF_HMAC2_512");
        SUPPORTED_PRF_TO_STR.put(PSEUDORANDOM_FUNCTION_AES128_CMAC, "PRF_AES128_CMAC");
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        INTEGRITY_ALGORITHM_NONE,
        INTEGRITY_ALGORITHM_HMAC_SHA1_96,
        INTEGRITY_ALGORITHM_AES_XCBC_96,
        INTEGRITY_ALGORITHM_AES_CMAC_96,
        INTEGRITY_ALGORITHM_HMAC_SHA2_256_128,
        INTEGRITY_ALGORITHM_HMAC_SHA2_384_192,
        INTEGRITY_ALGORITHM_HMAC_SHA2_512_256
    })
    public @interface IntegrityAlgorithm {}

    /** None Authentication/Integrity Algorithm. */
    public static final int INTEGRITY_ALGORITHM_NONE = 0;
    /** HMAC-SHA1 Authentication/Integrity Algorithm. */
    public static final int INTEGRITY_ALGORITHM_HMAC_SHA1_96 = 2;
    /** AES-XCBC-96 Authentication/Integrity Algorithm. */
    public static final int INTEGRITY_ALGORITHM_AES_XCBC_96 = 5;
    /** AES-CMAC-96 Authentication/Integrity Algorithm. */
    public static final int INTEGRITY_ALGORITHM_AES_CMAC_96 = 8;
    /** HMAC-SHA256 Authentication/Integrity Algorithm with 128-bit truncation. */
    public static final int INTEGRITY_ALGORITHM_HMAC_SHA2_256_128 = 12;
    /** HMAC-SHA384 Authentication/Integrity Algorithm with 192-bit truncation. */
    public static final int INTEGRITY_ALGORITHM_HMAC_SHA2_384_192 = 13;
    /** HMAC-SHA512 Authentication/Integrity Algorithm with 256-bit truncation. */
    public static final int INTEGRITY_ALGORITHM_HMAC_SHA2_512_256 = 14;

    /** @hide */
    protected static final SparseArray<String> SUPPORTED_INTEGRITY_ALGO_TO_STR;

    static {
        SUPPORTED_INTEGRITY_ALGO_TO_STR = new SparseArray<>();
        SUPPORTED_INTEGRITY_ALGO_TO_STR.put(INTEGRITY_ALGORITHM_NONE, "AUTH_NONE");
        SUPPORTED_INTEGRITY_ALGO_TO_STR.put(INTEGRITY_ALGORITHM_HMAC_SHA1_96, "AUTH_HMAC_SHA1_96");
        SUPPORTED_INTEGRITY_ALGO_TO_STR.put(INTEGRITY_ALGORITHM_AES_XCBC_96, "AUTH_AES_XCBC_96");
        SUPPORTED_INTEGRITY_ALGO_TO_STR.put(INTEGRITY_ALGORITHM_AES_CMAC_96, "AUTH_AES_CMAC_96");
        SUPPORTED_INTEGRITY_ALGO_TO_STR.put(
                INTEGRITY_ALGORITHM_HMAC_SHA2_256_128, "AUTH_HMAC_SHA2_256_128");
        SUPPORTED_INTEGRITY_ALGO_TO_STR.put(
                INTEGRITY_ALGORITHM_HMAC_SHA2_384_192, "AUTH_HMAC_SHA2_384_192");
        SUPPORTED_INTEGRITY_ALGO_TO_STR.put(
                INTEGRITY_ALGORITHM_HMAC_SHA2_512_256, "AUTH_HMAC_SHA2_512_256");
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        DH_GROUP_NONE,
        DH_GROUP_1024_BIT_MODP,
        DH_GROUP_1536_BIT_MODP,
        DH_GROUP_2048_BIT_MODP,
        DH_GROUP_3072_BIT_MODP,
        DH_GROUP_4096_BIT_MODP,
        DH_GROUP_CURVE_25519
    })
    public @interface DhGroup {}

    /** None Diffie-Hellman Group. */
    public static final int DH_GROUP_NONE = 0;
    /** 1024-bit MODP Diffie-Hellman Group. */
    public static final int DH_GROUP_1024_BIT_MODP = 2;
    /** 1536-bit MODP Diffie-Hellman Group. */
    public static final int DH_GROUP_1536_BIT_MODP = 5;
    /** 2048-bit MODP Diffie-Hellman Group. */
    public static final int DH_GROUP_2048_BIT_MODP = 14;
    /** 3072-bit MODP Diffie-Hellman Group. */
    public static final int DH_GROUP_3072_BIT_MODP = 15;
    /** 4096-bit MODP Diffie-Hellman Group. */
    public static final int DH_GROUP_4096_BIT_MODP = 16;
    /** Elliptic Curve Diffie-Hellman 25519. */
    public static final int DH_GROUP_CURVE_25519 = 31;

    private static final SparseArray<String> SUPPORTED_DH_GROUP_TO_STR;

    static {
        SUPPORTED_DH_GROUP_TO_STR = new SparseArray<>();
        SUPPORTED_DH_GROUP_TO_STR.put(DH_GROUP_NONE, "DH_NONE");
        SUPPORTED_DH_GROUP_TO_STR.put(DH_GROUP_1024_BIT_MODP, "DH_1024_BIT_MODP");
        SUPPORTED_DH_GROUP_TO_STR.put(DH_GROUP_1536_BIT_MODP, "DH_1536_BIT_MODP");
        SUPPORTED_DH_GROUP_TO_STR.put(DH_GROUP_2048_BIT_MODP, "DH_2048_BIT_MODP");
        SUPPORTED_DH_GROUP_TO_STR.put(DH_GROUP_3072_BIT_MODP, "DH_3072_BIT_MODP");
        SUPPORTED_DH_GROUP_TO_STR.put(DH_GROUP_4096_BIT_MODP, "DH_4096_BIT_MODP");
        SUPPORTED_DH_GROUP_TO_STR.put(DH_GROUP_CURVE_25519, "DH_GROUP_CURVE_25519");
    }

    private static final String PROTOCOL_ID_KEY = "mProtocolId";
    /** @hide */
    protected static final String ENCRYPT_ALGO_KEY = "mEncryptionAlgorithms";
    /** @hide */
    protected static final String INTEGRITY_ALGO_KEY = "mIntegrityAlgorithms";
    /** @hide */
    protected static final String DH_GROUP_KEY = "mDhGroups";

    @IkePayload.ProtocolId private final int mProtocolId;
    private final EncryptionTransform[] mEncryptionAlgorithms;
    private final IntegrityTransform[] mIntegrityAlgorithms;
    private final DhGroupTransform[] mDhGroups;

    /** @hide */
    protected SaProposal(
            @IkePayload.ProtocolId int protocol,
            EncryptionTransform[] encryptionAlgos,
            IntegrityTransform[] integrityAlgos,
            DhGroupTransform[] dhGroups) {
        mProtocolId = protocol;
        mEncryptionAlgorithms = encryptionAlgos;
        mIntegrityAlgorithms = integrityAlgos;
        mDhGroups = dhGroups;
    }

    /**
     * Constructs this object by deserializing a PersistableBundle
     *
     * @hide
     */
    @NonNull
    public static SaProposal fromPersistableBundle(@NonNull PersistableBundle in) {
        Objects.requireNonNull(in, "PersistableBundle is null");

        int protocolId = in.getInt(PROTOCOL_ID_KEY);
        switch (protocolId) {
            case IkePayload.PROTOCOL_ID_IKE:
                return IkeSaProposal.fromPersistableBundle(in);
            case IkePayload.PROTOCOL_ID_ESP:
                return ChildSaProposal.fromPersistableBundle(in);
            default:
                throw new IllegalArgumentException("Invalid protocol ID " + protocolId);
        }
    }

    /**
     * Serializes this object to a PersistableBundle
     *
     * @hide
     */
    @NonNull
    public PersistableBundle toPersistableBundle() {
        final PersistableBundle result = new PersistableBundle();

        result.putInt(PROTOCOL_ID_KEY, mProtocolId);

        PersistableBundle encryptionBundle =
                PersistableBundleUtils.fromList(
                        Arrays.asList(mEncryptionAlgorithms),
                        EncryptionTransform::toPersistableBundle);
        result.putPersistableBundle(ENCRYPT_ALGO_KEY, encryptionBundle);

        int[] integrityAlgoIdArray = getIntegrityAlgorithms().stream().mapToInt(i -> i).toArray();
        result.putIntArray(INTEGRITY_ALGO_KEY, integrityAlgoIdArray);

        int[] dhGroupArray = getDhGroups().stream().mapToInt(i -> i).toArray();
        result.putIntArray(DH_GROUP_KEY, dhGroupArray);

        return result;
    }

    /**
     * Check if the current SaProposal from the SA responder is consistent with the selected
     * reqProposal from the SA initiator.
     *
     * <p>As per RFC 7296, The accepted cryptographic suite MUST contain exactly one transform of
     * each type included in the proposal. But for interoperability reason, IKE library allows
     * exceptions when the accepted suite or the request proposal has a NONE value transform.
     * Currently only IntegrityTransform and DhGroupTransform have NONE value transform ID defined.
     *
     * @param reqProposal selected SaProposal from SA initiator
     * @return if current SaProposal from SA responder is consistent with the selected reqProposal
     *     from SA initiator.
     * @hide
     */
    public boolean isNegotiatedFrom(SaProposal reqProposal) {
        return this.mProtocolId == reqProposal.mProtocolId
                && isTransformSelectedFrom(mEncryptionAlgorithms, reqProposal.mEncryptionAlgorithms)
                && isIntegrityTransformSelectedFrom(
                        mIntegrityAlgorithms, reqProposal.mIntegrityAlgorithms)
                && isDhGroupTransformSelectedFrom(mDhGroups, reqProposal.mDhGroups);
    }

    /**
     * Check if the response transform can be selected from the request transforms
     *
     * <p>Package private
     */
    static boolean isTransformSelectedFrom(Transform[] selected, Transform[] selectFrom) {
        // If the selected proposal has multiple transforms with the same type, the responder MUST
        // choose a single one.
        if ((selected.length > 1) || (selected.length == 0) != (selectFrom.length == 0)) {
            return false;
        }

        if (selected.length == 0) return true;

        return Arrays.asList(selectFrom).contains(selected[0]);
    }

    /**
     * Check if the response integrity transform can be selected from the request integrity
     * transforms.
     *
     * <p>For interoperability reason, it is allowed to do not include integrity transform in the
     * response proposal when the request proposal has a NONE value integrity transform; and it is
     * also allowed to have a NONE value integrity transform when the request proposal does not have
     * integrity transforms.
     */
    private static boolean isIntegrityTransformSelectedFrom(
            IntegrityTransform[] selected, IntegrityTransform[] selectFrom) {
        if (selected.length == 0) {
            selected = new IntegrityTransform[] {new IntegrityTransform(INTEGRITY_ALGORITHM_NONE)};
        }
        if (selectFrom.length == 0) {
            selectFrom =
                    new IntegrityTransform[] {new IntegrityTransform(INTEGRITY_ALGORITHM_NONE)};
        }
        return isTransformSelectedFrom(selected, selectFrom);
    }

    /**
     * Check if the response DH group can be selected from the request DH groups
     *
     * <p>For interoperability reason, it is allowed to do not include DH group in the response
     * proposal when the request proposal has a NONE value DH group; and it is also allowed to have
     * a NONE value DH group when the request proposal does not have DH groups.
     */
    private static boolean isDhGroupTransformSelectedFrom(
            DhGroupTransform[] selected, DhGroupTransform[] selectFrom) {
        if (selected.length == 0) {
            selected = new DhGroupTransform[] {new DhGroupTransform(DH_GROUP_NONE)};
        }
        if (selectFrom.length == 0) {
            selectFrom = new DhGroupTransform[] {new DhGroupTransform(DH_GROUP_NONE)};
        }
        return isTransformSelectedFrom(selected, selectFrom);
    }

    /** @hide */
    @IkePayload.ProtocolId
    public int getProtocolId() {
        return mProtocolId;
    }

    /**
     * Gets all proposed encryption algorithms
     *
     * @return A list of Pairs, with the IANA-defined ID for the proposed encryption algorithm as
     *     the first item, and the key length (in bits) as the second.
     */
    @NonNull
    public List<Pair<Integer, Integer>> getEncryptionAlgorithms() {
        final List<Pair<Integer, Integer>> result = new ArrayList<>();
        for (EncryptionTransform transform : mEncryptionAlgorithms) {
            result.add(new Pair(transform.id, transform.getSpecifiedKeyLength()));
        }
        return result;
    }

    /**
     * Gets all proposed integrity algorithms
     *
     * @return A list of the IANA-defined IDs for the proposed integrity algorithms
     */
    @NonNull
    public List<Integer> getIntegrityAlgorithms() {
        final List<Integer> result = new ArrayList<>();
        for (Transform transform : mIntegrityAlgorithms) {
            result.add(transform.id);
        }
        return result;
    }

    /**
     * Gets all proposed Diffie-Hellman groups
     *
     * @return A list of the IANA-defined IDs for the proposed Diffie-Hellman groups
     */
    @NonNull
    public List<Integer> getDhGroups() {
        final List<Integer> result = new ArrayList<>();
        for (Transform transform : mDhGroups) {
            result.add(transform.id);
        }
        return result;
    }

    /** @hide */
    public EncryptionTransform[] getEncryptionTransforms() {
        return mEncryptionAlgorithms;
    }

    /** @hide */
    public IntegrityTransform[] getIntegrityTransforms() {
        return mIntegrityAlgorithms;
    }

    /** @hide */
    public DhGroupTransform[] getDhGroupTransforms() {
        return mDhGroups;
    }

    /** @hide */
    protected List<Transform> getAllTransformsAsList() {
        List<Transform> transformList = new LinkedList<>();

        transformList.addAll(Arrays.asList(mEncryptionAlgorithms));
        transformList.addAll(Arrays.asList(mIntegrityAlgorithms));
        transformList.addAll(Arrays.asList(mDhGroups));

        return transformList;
    }

    /**
     * Return all SA Transforms in this SaProposal to be encoded for building an outbound IKE
     * message.
     *
     * <p>This method should be called by only IKE library.
     *
     * @return Array of Transforms to be encoded.
     * @hide
     */
    public abstract Transform[] getAllTransforms();

    /**
     * This class is an abstract Builder for building a SaProposal.
     *
     * @hide
     */
    protected abstract static class Builder {
        protected static final String ERROR_TAG = "Invalid SA Proposal: ";

        // Use LinkedHashSet to ensure uniqueness and that ordering is maintained.
        protected final LinkedHashSet<EncryptionTransform> mProposedEncryptAlgos =
                new LinkedHashSet<>();
        protected final LinkedHashSet<PrfTransform> mProposedPrfs = new LinkedHashSet<>();
        protected final LinkedHashSet<IntegrityTransform> mProposedIntegrityAlgos =
                new LinkedHashSet<>();
        protected final LinkedHashSet<DhGroupTransform> mProposedDhGroups = new LinkedHashSet<>();

        protected boolean mHasAead = false;

        protected static boolean isAead(@EncryptionAlgorithm int algorithm) {
            switch (algorithm) {
                case ENCRYPTION_ALGORITHM_3DES:
                    // Fall through
                case ENCRYPTION_ALGORITHM_AES_CBC:
                    // Fall through
                case ENCRYPTION_ALGORITHM_AES_CTR:
                    return false;
                case ENCRYPTION_ALGORITHM_AES_GCM_8:
                    // Fall through
                case ENCRYPTION_ALGORITHM_AES_GCM_12:
                    // Fall through
                case ENCRYPTION_ALGORITHM_AES_GCM_16:
                    // Fall through
                case ENCRYPTION_ALGORITHM_CHACHA20_POLY1305:
                    return true;
                default:
                    // Won't hit here.
                    throw new IllegalArgumentException("Unsupported Encryption Algorithm.");
            }
        }

        protected EncryptionTransform[] buildEncryptAlgosOrThrow() {
            if (mProposedEncryptAlgos.isEmpty()) {
                throw new IllegalArgumentException(
                        ERROR_TAG + "Encryption algorithm must be proposed.");
            }

            return mProposedEncryptAlgos.toArray(
                    new EncryptionTransform[mProposedEncryptAlgos.size()]);
        }

        protected void validateAndAddEncryptAlgo(
                @EncryptionAlgorithm int algorithm, int keyLength, boolean isChild) {
            // Construct EncryptionTransform and validate proposed algorithm during
            // construction.
            EncryptionTransform encryptionTransform = new EncryptionTransform(algorithm, keyLength);

            // For Child SA algorithm, check if that is supported by IPsec
            if (SdkLevel.isAtLeastS()
                    && isChild
                    && !ChildSaProposal.getSupportedEncryptionAlgorithms().contains(algorithm)) {
                throw new IllegalArgumentException("Unsupported encryption algorithm " + algorithm);
            }

            // Validate that only one mode encryption algorithm has been proposed.
            boolean isCurrentAead = isAead(algorithm);
            if (!mProposedEncryptAlgos.isEmpty() && (mHasAead ^ isCurrentAead)) {
                throw new IllegalArgumentException(
                        ERROR_TAG
                                + "Proposal cannot has both normal ciphers "
                                + "and combined-mode ciphers.");
            }
            if (isCurrentAead) mHasAead = true;

            mProposedEncryptAlgos.add(encryptionTransform);
        }

        protected void validateAndAddIntegrityAlgo(
                @IntegrityAlgorithm int algorithm, boolean isChild) {
            // For Child SA algorithm, check if that is supported by IPsec
            if (SdkLevel.isAtLeastS()
                    && isChild
                    && !ChildSaProposal.getSupportedIntegrityAlgorithms().contains(algorithm)) {
                throw new IllegalArgumentException("Unsupported integrity algorithm " + algorithm);
            }

            // Construct IntegrityTransform and validate proposed algorithm during
            // construction.
            mProposedIntegrityAlgos.add(new IntegrityTransform(algorithm));
        }

        protected void addDh(@DhGroup int dhGroup) {
            // Construct DhGroupTransform and validate proposed dhGroup during
            // construction.
            mProposedDhGroups.add(new DhGroupTransform(dhGroup));
        }
    }

    /** @hide */
    @Override
    @NonNull
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(IkePayload.getProtocolTypeString(mProtocolId)).append(": ");

        int len = getAllTransforms().length;
        for (int i = 0; i < len; i++) {
            sb.append(getAllTransforms()[i].toString());
            if (i < len - 1) sb.append("|");
        }

        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mProtocolId,
                Arrays.hashCode(mEncryptionAlgorithms),
                Arrays.hashCode(mIntegrityAlgorithms),
                Arrays.hashCode(mDhGroups));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SaProposal)) {
            return false;
        }

        SaProposal other = (SaProposal) o;

        return mProtocolId == other.mProtocolId
                && Arrays.equals(mEncryptionAlgorithms, other.mEncryptionAlgorithms)
                && Arrays.equals(mIntegrityAlgorithms, other.mIntegrityAlgorithms)
                && Arrays.equals(mDhGroups, other.mDhGroups);
    }

    /** @hide */
    protected static Set<Integer> getKeySet(SparseArray array) {
        Set<Integer> result = new HashSet<>();
        for (int i = 0; i < array.size(); i++) {
            result.add(array.keyAt(i));
        }

        return result;
    }

    /** Returns supported DH groups for IKE and Child SA proposal negotiation. */
    @NonNull
    public static Set<Integer> getSupportedDhGroups() {
        final Set<Integer> supportedSet = new HashSet<>();
        for (int dh : getKeySet(SUPPORTED_DH_GROUP_TO_STR)) {
            if (dh == DH_GROUP_CURVE_25519 && !SdkLevel.isAtLeastS()) {
                continue;
            } else {
                supportedSet.add(dh);
            }
        }
        return supportedSet;
    }

    /**
     * Return the encryption algorithm as a String.
     *
     * @hide
     */
    public static String getEncryptionAlgorithmString(int algorithm) {
        if (SUPPORTED_ENCRYPTION_ALGO_TO_STR.contains(algorithm)) {
            return SUPPORTED_ENCRYPTION_ALGO_TO_STR.get(algorithm);
        }
        return "ENC_Unknown_" + algorithm;
    }

    /**
     * Return the pseudorandom function as a String.
     *
     * @hide
     */
    public static String getPseudorandomFunctionString(int algorithm) {
        if (SUPPORTED_PRF_TO_STR.contains(algorithm)) {
            return SUPPORTED_PRF_TO_STR.get(algorithm);
        }
        return "PRF_Unknown_" + algorithm;
    }

    /**
     * Return the integrity algorithm as a String.
     *
     * @hide
     */
    public static String getIntegrityAlgorithmString(int algorithm) {
        if (SUPPORTED_INTEGRITY_ALGO_TO_STR.contains(algorithm)) {
            return SUPPORTED_INTEGRITY_ALGO_TO_STR.get(algorithm);
        }
        return "AUTH_Unknown_" + algorithm;
    }

    /**
     * Return Diffie-Hellman Group as a String.
     *
     * @hide
     */
    public static String getDhGroupString(int dhGroup) {
        if (SUPPORTED_DH_GROUP_TO_STR.contains(dhGroup)) {
            return SUPPORTED_DH_GROUP_TO_STR.get(dhGroup);
        }
        return "DH_Unknown_" + dhGroup;
    }
}
