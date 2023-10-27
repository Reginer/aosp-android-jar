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

package android.net.ipsec.ike;

import static com.android.internal.net.ipsec.ike.message.IkeSaPayload.EsnTransform.ESN_POLICY_NO_EXTENDED;

import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.net.IpSecAlgorithm;
import android.os.PersistableBundle;
import android.util.ArraySet;

import com.android.internal.net.ipsec.ike.crypto.IkeCipher;
import com.android.internal.net.ipsec.ike.crypto.IkeMacIntegrity;
import com.android.internal.net.ipsec.ike.message.IkePayload;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.DhGroupTransform;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.EncryptionTransform;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.EsnTransform;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.IntegrityTransform;
import com.android.internal.net.ipsec.ike.message.IkeSaPayload.Transform;
import com.android.modules.utils.build.SdkLevel;
import com.android.server.vcn.util.PersistableBundleUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * ChildSaProposal represents a proposed configuration to negotiate a Child SA.
 *
 * <p>ChildSaProposal will contain cryptograhic algorithms and key generation materials for the
 * negotiation of a Child SA.
 *
 * <p>User must provide at least one valid ChildSaProposal when they are creating a new Child SA.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-3.3">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 */
public final class ChildSaProposal extends SaProposal {
    // Before SDK S, there is no API in IpSecAlgorithm to retrieve supported algorithms. Thus hard
    // coded these algorithms here.
    private static final Set<Integer> SUPPORTED_IPSEC_ENCRYPTION_BEFORE_SDK_S;
    private static final Set<Integer> SUPPORTED_IPSEC_INTEGRITY_BEFORE_SDK_S;

    static {
        SUPPORTED_IPSEC_ENCRYPTION_BEFORE_SDK_S = new ArraySet<>();
        SUPPORTED_IPSEC_ENCRYPTION_BEFORE_SDK_S.add(ENCRYPTION_ALGORITHM_AES_CBC);
        SUPPORTED_IPSEC_ENCRYPTION_BEFORE_SDK_S.add(ENCRYPTION_ALGORITHM_AES_GCM_8);
        SUPPORTED_IPSEC_ENCRYPTION_BEFORE_SDK_S.add(ENCRYPTION_ALGORITHM_AES_GCM_12);
        SUPPORTED_IPSEC_ENCRYPTION_BEFORE_SDK_S.add(ENCRYPTION_ALGORITHM_AES_GCM_16);

        SUPPORTED_IPSEC_INTEGRITY_BEFORE_SDK_S = new ArraySet<>();
        SUPPORTED_IPSEC_INTEGRITY_BEFORE_SDK_S.add(INTEGRITY_ALGORITHM_HMAC_SHA1_96);
        SUPPORTED_IPSEC_INTEGRITY_BEFORE_SDK_S.add(INTEGRITY_ALGORITHM_HMAC_SHA2_256_128);
        SUPPORTED_IPSEC_INTEGRITY_BEFORE_SDK_S.add(INTEGRITY_ALGORITHM_HMAC_SHA2_384_192);
        SUPPORTED_IPSEC_INTEGRITY_BEFORE_SDK_S.add(INTEGRITY_ALGORITHM_HMAC_SHA2_512_256);
    }

    private static final String ESN_KEY = "mEsns";
    private final EsnTransform[] mEsns;

    /**
     * Construct an instance of ChildSaProposal.
     *
     * <p>This constructor is either called by ChildSaPayload for building an inbound proposal from
     * a decoded packet, or called by the inner Builder to build an outbound proposal from user
     * provided parameters
     *
     * @param encryptionAlgos encryption algorithms
     * @param integrityAlgos integrity algorithms
     * @param dhGroups Diffie-Hellman Groups
     * @param esns ESN policies
     * @hide
     */
    public ChildSaProposal(
            EncryptionTransform[] encryptionAlgos,
            IntegrityTransform[] integrityAlgos,
            DhGroupTransform[] dhGroups,
            EsnTransform[] esns) {
        super(IkePayload.PROTOCOL_ID_ESP, encryptionAlgos, integrityAlgos, dhGroups);
        mEsns = esns;
    }

    /**
     * Constructs this object by deserializing a PersistableBundle
     *
     * <p>Constructed proposals are guaranteed to be valid, as checked by the
     * ChildSaProposal.Builder.
     *
     * @hide
     */
    @NonNull
    public static ChildSaProposal fromPersistableBundle(@NonNull PersistableBundle in) {
        Objects.requireNonNull(in, "PersistableBundle is null");

        ChildSaProposal.Builder builder = new ChildSaProposal.Builder();

        PersistableBundle encryptionBundle = in.getPersistableBundle(ENCRYPT_ALGO_KEY);
        Objects.requireNonNull(encryptionBundle, "Encryption algo bundle is null");
        List<EncryptionTransform> encryptList =
                PersistableBundleUtils.toList(
                        encryptionBundle, EncryptionTransform::fromPersistableBundle);
        for (EncryptionTransform t : encryptList) {
            builder.addEncryptionAlgorithm(t.id, t.getSpecifiedKeyLength());
        }

        int[] integrityAlgoIdArray = in.getIntArray(INTEGRITY_ALGO_KEY);
        Objects.requireNonNull(integrityAlgoIdArray, "Integrity algo array is null");
        for (int algo : integrityAlgoIdArray) {
            builder.addIntegrityAlgorithm(algo);
        }

        int[] dhGroupArray = in.getIntArray(DH_GROUP_KEY);
        Objects.requireNonNull(dhGroupArray, "DH Group array is null");
        for (int dh : dhGroupArray) {
            builder.addDhGroup(dh);
        }

        int[] esnPolicies = in.getIntArray(ESN_KEY);
        Objects.requireNonNull(esnPolicies, "ESN policy array is null");

        for (int p : esnPolicies) {
            switch (p) {
                case ESN_POLICY_NO_EXTENDED:
                    // Ignored. All ChildSaProposal(s) are proposed with this automatically
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Proposing ESN policy: " + p + " is unsupported");
            }
        }

        return builder.build();
    }

    /**
     * Serializes this object to a PersistableBundle
     *
     * @hide
     */
    @Override
    @NonNull
    public PersistableBundle toPersistableBundle() {
        final PersistableBundle result = super.toPersistableBundle();
        int[] esnPolicies = Arrays.asList(mEsns).stream().mapToInt(esn -> esn.id).toArray();
        result.putIntArray(ESN_KEY, esnPolicies);

        return result;
    }

    /**
     * Returns supported encryption algorithms for Child SA proposal negotiation.
     *
     * <p>Some algorithms may not be supported on old devices.
     */
    @NonNull
    public static Set<Integer> getSupportedEncryptionAlgorithms() {
        if (SdkLevel.isAtLeastS()) {
            Set<Integer> algoIds = new ArraySet<>();
            for (int i = 0; i < SUPPORTED_ENCRYPTION_ALGO_TO_STR.size(); i++) {
                int ikeAlgoId = SUPPORTED_ENCRYPTION_ALGO_TO_STR.keyAt(i);
                String ipSecAlgoName = IkeCipher.getIpSecAlgorithmName(ikeAlgoId);
                if (IpSecAlgorithm.getSupportedAlgorithms().contains(ipSecAlgoName)) {
                    algoIds.add(ikeAlgoId);
                }
            }
            return algoIds;
        } else {
            return SUPPORTED_IPSEC_ENCRYPTION_BEFORE_SDK_S;
        }
    }

    /**
     * Returns supported integrity algorithms for Child SA proposal negotiation.
     *
     * <p>Some algorithms may not be supported on old devices.
     */
    @NonNull
    public static Set<Integer> getSupportedIntegrityAlgorithms() {
        Set<Integer> algoIds = new ArraySet<>();

        // Although IpSecAlgorithm does not support INTEGRITY_ALGORITHM_NONE, IKE supports
        // negotiating it and won't build IpSecAlgorithm with it.
        algoIds.add(INTEGRITY_ALGORITHM_NONE);

        if (SdkLevel.isAtLeastS()) {
            for (int i = 0; i < SUPPORTED_INTEGRITY_ALGO_TO_STR.size(); i++) {
                int ikeAlgoId = SUPPORTED_INTEGRITY_ALGO_TO_STR.keyAt(i);
                String ipSecAlgoName = IkeMacIntegrity.getIpSecAlgorithmName(ikeAlgoId);
                if (IpSecAlgorithm.getSupportedAlgorithms().contains(ipSecAlgoName)) {
                    algoIds.add(ikeAlgoId);
                }
            }
        } else {
            algoIds.addAll(SUPPORTED_IPSEC_INTEGRITY_BEFORE_SDK_S);
        }
        return algoIds;
    }

    /**
     * Gets all ESN policies.
     *
     * @hide
     */
    public EsnTransform[] getEsnTransforms() {
        return mEsns;
    }

    /**
     * Gets a copy of proposal without all proposed DH groups.
     *
     * <p>This is used to avoid negotiating DH Group for negotiating first Child SA.
     *
     * @hide
     */
    public ChildSaProposal getCopyWithoutDhTransform() {
        return new ChildSaProposal(
                getEncryptionTransforms(),
                getIntegrityTransforms(),
                new DhGroupTransform[0],
                getEsnTransforms());
    }

    /** @hide */
    @Override
    public Transform[] getAllTransforms() {
        List<Transform> transformList = getAllTransformsAsList();
        transformList.addAll(Arrays.asList(mEsns));

        return transformList.toArray(new Transform[transformList.size()]);
    }

    /** @hide */
    @Override
    public boolean isNegotiatedFrom(SaProposal reqProposal) {
        return super.isNegotiatedFrom(reqProposal)
                && isTransformSelectedFrom(mEsns, ((ChildSaProposal) reqProposal).mEsns);
    }

    /** @hide */
    public boolean isNegotiatedFromExceptDhGroup(SaProposal saProposal) {
        return getProtocolId() == saProposal.getProtocolId()
                && isTransformSelectedFrom(
                        getEncryptionTransforms(), saProposal.getEncryptionTransforms())
                && isTransformSelectedFrom(
                        getIntegrityTransforms(), saProposal.getIntegrityTransforms())
                && isTransformSelectedFrom(mEsns, ((ChildSaProposal) saProposal).mEsns);
    }

    /** @hide */
    public ChildSaProposal getCopyWithAdditionalDhTransform(int dhGroup) {
        return new ChildSaProposal(
                getEncryptionTransforms(),
                getIntegrityTransforms(),
                new DhGroupTransform[] {new DhGroupTransform(dhGroup)},
                getEsnTransforms());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), Arrays.hashCode(mEsns));
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o) || !(o instanceof ChildSaProposal)) {
            return false;
        }

        return Arrays.equals(mEsns, ((ChildSaProposal) o).mEsns);
    }

    /**
     * This class is used to incrementally construct a ChildSaProposal. ChildSaProposal instances
     * are immutable once built.
     */
    public static final class Builder extends SaProposal.Builder {
        /**
         * Adds an encryption algorithm with a specific key length to the SA proposal being built.
         *
         * @param algorithm encryption algorithm to add to ChildSaProposal.
         * @param keyLength key length of algorithm. For algorithms that have fixed key length (e.g.
         *     3DES) only {@link SaProposal#KEY_LEN_UNUSED} is allowed.
         * @return Builder of ChildSaProposal.
         */
        // The matching getter is defined in the super class. Please see {@link
        // SaProposal#getEncryptionAlgorithms}
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addEncryptionAlgorithm(@EncryptionAlgorithm int algorithm, int keyLength) {
            validateAndAddEncryptAlgo(algorithm, keyLength, true /* isChild */);
            return this;
        }

        /**
         * Adds an integrity algorithm to the SA proposal being built.
         *
         * @param algorithm integrity algorithm to add to ChildSaProposal.
         * @return Builder of ChildSaProposal.
         */
        // The matching getter is defined in the super class. Please see
        // {@link SaProposal#getIntegrityAlgorithms}
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addIntegrityAlgorithm(@IntegrityAlgorithm int algorithm) {
            validateAndAddIntegrityAlgo(algorithm, true /* isChild */);
            return this;
        }

        /**
         * Adds a Diffie-Hellman Group to the SA proposal being built.
         *
         * <p>If this ChildSaProposal will be used for the first Child SA created as part of IKE
         * AUTH exchange, DH groups configured here will only apply when the Child SA is later
         * rekeyed. In this case, configuring different DH groups for IKE and Child SA may cause
         * Rekey Child to fail.
         *
         * <p>If no DH groups are supplied here, but the server requests a DH exchange during rekey,
         * the IKE SA's negotiated DH group will still be accepted.
         *
         * @param dhGroup to add to ChildSaProposal.
         * @return Builder of ChildSaProposal.
         */
        // The matching getter is defined in the super class. Please see
        // {@link SaProposal#getDhGroups}
        @SuppressLint("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addDhGroup(@DhGroup int dhGroup) {
            addDh(dhGroup);
            return this;
        }

        private IntegrityTransform[] buildIntegAlgosOrThrow() {
            // When building Child SA Proposal with normal-mode ciphers, there is no contraint on
            // integrity algorithm. When building Child SA Proposal with combined-mode ciphers,
            // mProposedIntegrityAlgos must be either empty or only have INTEGRITY_ALGORITHM_NONE.
            for (IntegrityTransform transform : mProposedIntegrityAlgos) {
                if (transform.id != INTEGRITY_ALGORITHM_NONE && mHasAead) {
                    throw new IllegalArgumentException(
                            ERROR_TAG
                                    + "Only INTEGRITY_ALGORITHM_NONE can be"
                                    + " proposed with combined-mode ciphers in any proposal.");
                }
            }

            return mProposedIntegrityAlgos.toArray(
                    new IntegrityTransform[mProposedIntegrityAlgos.size()]);
        }

        /**
         * Validates and builds the ChildSaProposal.
         *
         * @return the validated ChildSaProposal.
         */
        @NonNull
        public ChildSaProposal build() {
            EncryptionTransform[] encryptionTransforms = buildEncryptAlgosOrThrow();
            IntegrityTransform[] integrityTransforms = buildIntegAlgosOrThrow();

            return new ChildSaProposal(
                    encryptionTransforms,
                    integrityTransforms,
                    mProposedDhGroups.toArray(new DhGroupTransform[mProposedDhGroups.size()]),
                    new EsnTransform[] {new EsnTransform()});
        }
    }
}
