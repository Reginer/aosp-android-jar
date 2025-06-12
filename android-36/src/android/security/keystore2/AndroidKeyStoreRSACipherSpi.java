/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.security.keystore2;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.hardware.security.keymint.KeyParameter;
import android.security.keymaster.KeymasterDefs;
import android.security.keystore.KeyProperties;
import android.system.keystore2.Authorization;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherSpi;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * Base class for {@link CipherSpi} providing Android KeyStore backed RSA encryption/decryption.
 *
 * @hide
 */
abstract class AndroidKeyStoreRSACipherSpi extends AndroidKeyStoreCipherSpiBase {

    /**
     * Raw RSA cipher without any padding.
     */
    public static final class NoPadding extends AndroidKeyStoreRSACipherSpi {
        public NoPadding() {
            super(KeymasterDefs.KM_PAD_NONE);
        }

        @Override
        protected boolean adjustConfigForEncryptingWithPrivateKey() {
            // RSA encryption with no padding using private key is a way to implement raw RSA
            // signatures which JCA does not expose via Signature. We thus have to support this.
            setKeymasterPurposeOverride(KeymasterDefs.KM_PURPOSE_SIGN);
            return true;
        }

        @Override
        protected void initAlgorithmSpecificParameters() throws InvalidKeyException {}

        @Override
        protected void initAlgorithmSpecificParameters(@Nullable AlgorithmParameterSpec params)
                throws InvalidAlgorithmParameterException {
            if (params != null) {
                throw new InvalidAlgorithmParameterException(
                        "Unexpected parameters: " + params + ". No parameters supported");
            }
        }

        @Override
        protected void initAlgorithmSpecificParameters(@Nullable AlgorithmParameters params)
                throws InvalidAlgorithmParameterException {

            if (params != null) {
                throw new InvalidAlgorithmParameterException(
                        "Unexpected parameters: " + params + ". No parameters supported");
            }
        }

        @Override
        protected AlgorithmParameters engineGetParameters() {
            return null;
        }

        @Override
        protected final int getAdditionalEntropyAmountForBegin() {
            return 0;
        }

        @Override
        protected final int getAdditionalEntropyAmountForFinish() {
            return 0;
        }
    }

    /**
     * RSA cipher with PKCS#1 v1.5 encryption padding.
     */
    public static final class PKCS1Padding extends AndroidKeyStoreRSACipherSpi {
        public PKCS1Padding() {
            super(KeymasterDefs.KM_PAD_RSA_PKCS1_1_5_ENCRYPT);
        }

        @Override
        protected boolean adjustConfigForEncryptingWithPrivateKey() {
            // RSA encryption with PCKS#1 padding using private key is a way to implement RSA
            // signatures with PKCS#1 padding. We have to support this for legacy reasons.
            setKeymasterPurposeOverride(KeymasterDefs.KM_PURPOSE_SIGN);
            setKeymasterPaddingOverride(KeymasterDefs.KM_PAD_RSA_PKCS1_1_5_SIGN);
            return true;
        }

        @Override
        protected void initAlgorithmSpecificParameters() throws InvalidKeyException {}

        @Override
        protected void initAlgorithmSpecificParameters(@Nullable AlgorithmParameterSpec params)
                throws InvalidAlgorithmParameterException {
            if (params != null) {
                throw new InvalidAlgorithmParameterException(
                        "Unexpected parameters: " + params + ". No parameters supported");
            }
        }

        @Override
        protected void initAlgorithmSpecificParameters(@Nullable AlgorithmParameters params)
                throws InvalidAlgorithmParameterException {

            if (params != null) {
                throw new InvalidAlgorithmParameterException(
                        "Unexpected parameters: " + params + ". No parameters supported");
            }
        }

        @Override
        protected AlgorithmParameters engineGetParameters() {
            return null;
        }

        @Override
        protected final int getAdditionalEntropyAmountForBegin() {
            return 0;
        }

        @Override
        protected final int getAdditionalEntropyAmountForFinish() {
            return (isEncrypting()) ? getModulusSizeBytes() : 0;
        }
    }

    /**
     * RSA cipher with OAEP encryption padding.
     */
    abstract static class OAEPWithMGF1Padding extends AndroidKeyStoreRSACipherSpi {

        private static final String MGF_ALGORITHM_MGF1 = "MGF1";

        private int mKeymasterDigest = -1;
        private int mDigestOutputSizeBytes;
        private int mKeymasterMgf1Digest = KeymasterDefs.KM_DIGEST_SHA1; // Default MGF1 digest

        OAEPWithMGF1Padding(int keymasterDigest) {
            super(KeymasterDefs.KM_PAD_RSA_OAEP);
            mKeymasterDigest = keymasterDigest;
            mDigestOutputSizeBytes =
                    (KeymasterUtils.getDigestOutputSizeBits(keymasterDigest) + 7) / 8;
        }

        @Override
        protected final void initAlgorithmSpecificParameters() throws InvalidKeyException {}

        @Override
        protected final void initAlgorithmSpecificParameters(
                @Nullable AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
            if (params == null) {
                return;
            }

            if (!(params instanceof OAEPParameterSpec)) {
                throw new InvalidAlgorithmParameterException(
                        "Unsupported parameter spec: " + params
                        + ". Only OAEPParameterSpec supported");
            }
            OAEPParameterSpec spec = (OAEPParameterSpec) params;
            if (!MGF_ALGORITHM_MGF1.equalsIgnoreCase(spec.getMGFAlgorithm())) {
                throw new InvalidAlgorithmParameterException(
                        "Unsupported MGF: " + spec.getMGFAlgorithm()
                        + ". Only " + MGF_ALGORITHM_MGF1 + " supported");
            }
            String jcaDigest = spec.getDigestAlgorithm();
            int keymasterDigest;
            try {
                keymasterDigest = KeyProperties.Digest.toKeymaster(jcaDigest);
            } catch (IllegalArgumentException e) {
                throw new InvalidAlgorithmParameterException(
                        "Unsupported digest: " + jcaDigest, e);
            }
            switch (keymasterDigest) {
                case KeymasterDefs.KM_DIGEST_SHA1:
                case KeymasterDefs.KM_DIGEST_SHA_2_224:
                case KeymasterDefs.KM_DIGEST_SHA_2_256:
                case KeymasterDefs.KM_DIGEST_SHA_2_384:
                case KeymasterDefs.KM_DIGEST_SHA_2_512:
                    // Permitted.
                    break;
                default:
                    throw new InvalidAlgorithmParameterException(
                            "Unsupported digest: " + jcaDigest);
            }
            AlgorithmParameterSpec mgfParams = spec.getMGFParameters();
            if (mgfParams == null) {
                throw new InvalidAlgorithmParameterException("MGF parameters must be provided");
            }
            // Check whether MGF parameters match the OAEPParameterSpec
            if (!(mgfParams instanceof MGF1ParameterSpec)) {
                throw new InvalidAlgorithmParameterException("Unsupported MGF parameters"
                        + ": " + mgfParams + ". Only MGF1ParameterSpec supported");
            }
            MGF1ParameterSpec mgfSpec = (MGF1ParameterSpec) mgfParams;
            String mgf1JcaDigest = mgfSpec.getDigestAlgorithm();
            PSource pSource = spec.getPSource();
            if (!(pSource instanceof PSource.PSpecified)) {
                throw new InvalidAlgorithmParameterException(
                        "Unsupported source of encoding input P: " + pSource
                        + ". Only pSpecifiedEmpty (PSource.PSpecified.DEFAULT) supported");
            }
            PSource.PSpecified pSourceSpecified = (PSource.PSpecified) pSource;
            byte[] pSourceValue = pSourceSpecified.getValue();
            if ((pSourceValue != null) && (pSourceValue.length > 0)) {
                throw new InvalidAlgorithmParameterException(
                        "Unsupported source of encoding input P: " + pSource
                        + ". Only pSpecifiedEmpty (PSource.PSpecified.DEFAULT) supported");
            }
            mKeymasterDigest = keymasterDigest;
            mKeymasterMgf1Digest = KeyProperties.Digest.toKeymaster(mgf1JcaDigest);
            mDigestOutputSizeBytes =
                    (KeymasterUtils.getDigestOutputSizeBits(keymasterDigest) + 7) / 8;
        }

        @Override
        protected final void initAlgorithmSpecificParameters(@Nullable AlgorithmParameters params)
                throws InvalidAlgorithmParameterException {
            if (params == null) {
                return;
            }

            OAEPParameterSpec spec;
            try {
                spec = params.getParameterSpec(OAEPParameterSpec.class);
            } catch (InvalidParameterSpecException e) {
                throw new InvalidAlgorithmParameterException("OAEP parameters required"
                        + ", but not found in parameters: " + params, e);
            }
            if (spec == null) {
                throw new InvalidAlgorithmParameterException("OAEP parameters required"
                        + ", but not provided in parameters: " + params);
            }
            initAlgorithmSpecificParameters(spec);
        }

        @Override
        protected final AlgorithmParameters engineGetParameters() {
            OAEPParameterSpec spec =
                    new OAEPParameterSpec(
                        KeyProperties.Digest.fromKeymaster(mKeymasterDigest),
                        MGF_ALGORITHM_MGF1,
                        KeyProperties.Digest.fromKeymasterToMGF1ParameterSpec(mKeymasterMgf1Digest),
                        PSource.PSpecified.DEFAULT);
            try {
                AlgorithmParameters params = AlgorithmParameters.getInstance("OAEP");
                params.init(spec);
                return params;
            } catch (NoSuchAlgorithmException e) {
                throw new ProviderException(
                        "Failed to obtain OAEP AlgorithmParameters", e);
            } catch (InvalidParameterSpecException e) {
                throw new ProviderException(
                        "Failed to initialize OAEP AlgorithmParameters with an IV",
                        e);
            }
        }

        private static boolean isMgfDigestTagPresentInKeyProperties(
                Authorization[] keyCharacteristics) {
            for (Authorization authorization : keyCharacteristics) {
                if (authorization.keyParameter.tag == KeymasterDefs.KM_TAG_RSA_OAEP_MGF_DIGEST) {
                    return true;
                }
            }

            return false;
        }

        @Override
        protected final void addAlgorithmSpecificParametersToBegin(
                @NonNull List<KeyParameter> parameters, Authorization[] keyCharacteristics) {
            super.addAlgorithmSpecificParametersToBegin(parameters, keyCharacteristics);
            parameters.add(KeyStore2ParameterUtils.makeEnum(
                    KeymasterDefs.KM_TAG_DIGEST, mKeymasterDigest
            ));
            // Only add the KM_TAG_RSA_OAEP_MGF_DIGEST tag to begin() if the MGF Digest is
            // present in the key properties. Keys generated prior to Android 14 did not have
            // this tag (Keystore didn't add it) so specifying any MGF digest tag would cause
            // a begin() operation (on an Android 14 device) to fail (with a key that was generated
            // on Android 13 or below).
            if (isMgfDigestTagPresentInKeyProperties(keyCharacteristics)) {
                parameters.add(KeyStore2ParameterUtils.makeEnum(
                        KeymasterDefs.KM_TAG_RSA_OAEP_MGF_DIGEST, mKeymasterMgf1Digest
                ));
            }
        }

        @Override
        protected final void loadAlgorithmSpecificParametersFromBeginResult(
                KeyParameter[] parameters) {
            super.loadAlgorithmSpecificParametersFromBeginResult(parameters);
        }

        @Override
        protected final int getAdditionalEntropyAmountForBegin() {
            return 0;
        }

        @Override
        protected final int getAdditionalEntropyAmountForFinish() {
            return (isEncrypting()) ? mDigestOutputSizeBytes : 0;
        }

        @Override
        protected final String getTransform() {
            switch (mKeymasterDigest) {
                case KeymasterDefs.KM_DIGEST_SHA1:
                    return "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";
                case KeymasterDefs.KM_DIGEST_SHA_2_224:
                    return "RSA/ECB/OAEPWithSHA-224AndMGF1Padding";
                case KeymasterDefs.KM_DIGEST_SHA_2_256:
                    return "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
                case KeymasterDefs.KM_DIGEST_SHA_2_384:
                    return "RSA/ECB/OAEPWithSHA-384AndMGF1Padding";
                case KeymasterDefs.KM_DIGEST_SHA_2_512:
                    return "RSA/ECB/OAEPWithSHA-512AndMGF1Padding";
                default:
                    return "RSA/ECB/OAEPPadding";
            }
        }

    }

    public static class OAEPWithSHA1AndMGF1Padding extends OAEPWithMGF1Padding {
        public OAEPWithSHA1AndMGF1Padding() {
            super(KeymasterDefs.KM_DIGEST_SHA1);
        }
    }

    public static class OAEPWithSHA224AndMGF1Padding extends OAEPWithMGF1Padding {
        public OAEPWithSHA224AndMGF1Padding() {
            super(KeymasterDefs.KM_DIGEST_SHA_2_224);
        }
    }

    public static class OAEPWithSHA256AndMGF1Padding extends OAEPWithMGF1Padding {
        public OAEPWithSHA256AndMGF1Padding() {
            super(KeymasterDefs.KM_DIGEST_SHA_2_256);
        }
    }

    public static class OAEPWithSHA384AndMGF1Padding extends OAEPWithMGF1Padding {
        public OAEPWithSHA384AndMGF1Padding() {
            super(KeymasterDefs.KM_DIGEST_SHA_2_384);
        }
    }

    public static class OAEPWithSHA512AndMGF1Padding extends OAEPWithMGF1Padding {
        public OAEPWithSHA512AndMGF1Padding() {
            super(KeymasterDefs.KM_DIGEST_SHA_2_512);
        }
    }

    private final int mKeymasterPadding;
    private int mKeymasterPaddingOverride;

    private int mModulusSizeBytes = -1;

    AndroidKeyStoreRSACipherSpi(int keymasterPadding) {
        mKeymasterPadding = keymasterPadding;
    }

    @Override
    protected String getTransform() {
        return "RSA/ECB/" + KeyProperties.EncryptionPadding.fromKeymaster(mKeymasterPadding);
    }

    @Override
    protected final void initKey(int opmode, Key key) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("Unsupported key: null");
        }
        if (!KeyProperties.KEY_ALGORITHM_RSA.equalsIgnoreCase(key.getAlgorithm())) {
            throw new InvalidKeyException("Unsupported key algorithm: " + key.getAlgorithm()
                    + ". Only " + KeyProperties.KEY_ALGORITHM_RSA + " supported");
        }
        AndroidKeyStoreKey keystoreKey;
        if (key instanceof AndroidKeyStorePrivateKey) {
            keystoreKey = (AndroidKeyStoreKey) key;
        } else if (key instanceof AndroidKeyStorePublicKey) {
            keystoreKey = (AndroidKeyStoreKey) key;
        } else {
            throw new InvalidKeyException("Unsupported key type: " + key);
        }

        if (keystoreKey instanceof PrivateKey) {
            // Private key
            switch (opmode) {
                case Cipher.DECRYPT_MODE:
                case Cipher.UNWRAP_MODE:
                    // Permitted
                    break;
                case Cipher.ENCRYPT_MODE:
                case Cipher.WRAP_MODE:
                    if (!adjustConfigForEncryptingWithPrivateKey()) {
                        throw new InvalidKeyException(
                                "RSA private keys cannot be used with " + opmodeToString(opmode)
                                + " and padding "
                                + KeyProperties.EncryptionPadding.fromKeymaster(mKeymasterPadding)
                                + ". Only RSA public keys supported for this mode");
                    }
                    break;
                default:
                    throw new InvalidKeyException(
                            "RSA private keys cannot be used with opmode: " + opmode);
            }
        } else {
            // Public key
            switch (opmode) {
                case Cipher.ENCRYPT_MODE:
                case Cipher.WRAP_MODE:
                    // Permitted
                    break;
                case Cipher.DECRYPT_MODE:
                case Cipher.UNWRAP_MODE:
                    throw new InvalidKeyException(
                            "RSA public keys cannot be used with " + opmodeToString(opmode)
                            + " and padding "
                            + KeyProperties.EncryptionPadding.fromKeymaster(mKeymasterPadding)
                            + ". Only RSA private keys supported for this opmode.");
                    // break;
                default:
                    throw new InvalidKeyException(
                            "RSA public keys cannot be used with " + opmodeToString(opmode));
            }
        }

        long keySizeBits = -1;
        for (Authorization a : keystoreKey.getAuthorizations()) {
            if (a.keyParameter.tag == KeymasterDefs.KM_TAG_KEY_SIZE) {
                keySizeBits = KeyStore2ParameterUtils.getUnsignedInt(a);
            }
        }

        if (keySizeBits == -1) {
            throw new InvalidKeyException("Size of key not known");
        } else if (keySizeBits > Integer.MAX_VALUE) {
            throw new InvalidKeyException("Key too large: " + keySizeBits + " bits");
        }
        mModulusSizeBytes = (int) ((keySizeBits + 7) / 8);

        setKey(keystoreKey);
    }

    /**
     * Adjusts the configuration of this cipher for encrypting using the private key.
     *
     * <p>The default implementation does nothing and refuses to adjust the configuration.
     *
     * @return {@code true} if the configuration has been adjusted, {@code false} if encrypting
     *         using private key is not permitted for this cipher.
     */
    protected boolean adjustConfigForEncryptingWithPrivateKey() {
        return false;
    }

    @Override
    protected final void resetAll() {
        mModulusSizeBytes = -1;
        mKeymasterPaddingOverride = -1;
        super.resetAll();
    }

    @Override
    protected final void resetWhilePreservingInitState() {
        super.resetWhilePreservingInitState();
    }

    @Override
    protected void addAlgorithmSpecificParametersToBegin(
            @NonNull List<KeyParameter> parameters) {
        parameters.add(KeyStore2ParameterUtils.makeEnum(
                KeymasterDefs.KM_TAG_ALGORITHM, KeymasterDefs.KM_ALGORITHM_RSA
        ));
        int keymasterPadding = getKeymasterPaddingOverride();
        if (keymasterPadding == -1) {
            keymasterPadding = mKeymasterPadding;
        }
        parameters.add(KeyStore2ParameterUtils.makeEnum(
                KeymasterDefs.KM_TAG_PADDING, keymasterPadding
        ));
        int purposeOverride = getKeymasterPurposeOverride();
        if ((purposeOverride != -1)
                && ((purposeOverride == KeymasterDefs.KM_PURPOSE_SIGN)
                || (purposeOverride == KeymasterDefs.KM_PURPOSE_VERIFY))) {
            // Keymaster sign/verify requires digest to be specified.
            // For raw sign/verify it's NONE.
            parameters.add(KeyStore2ParameterUtils.makeEnum(
                    KeymasterDefs.KM_TAG_DIGEST, KeymasterDefs.KM_DIGEST_NONE
            ));
        }
    }

    @Override
    protected void loadAlgorithmSpecificParametersFromBeginResult(
            KeyParameter[] parameters) {
    }

    @Override
    protected final int engineGetBlockSize() {
        // Not a block cipher, according to the RI
        return 0;
    }

    @Override
    protected final byte[] engineGetIV() {
        // IV never used
        return null;
    }

    @Override
    protected final int engineGetOutputSize(int inputLen) {
        return getModulusSizeBytes();
    }

    protected final int getModulusSizeBytes() {
        if (mModulusSizeBytes == -1) {
            throw new IllegalStateException("Not initialized");
        }
        return mModulusSizeBytes;
    }

    /**
     * Overrides the default padding of the crypto operation.
     */
    protected final void setKeymasterPaddingOverride(int keymasterPadding) {
        mKeymasterPaddingOverride = keymasterPadding;
    }

    protected final int getKeymasterPaddingOverride() {
        return mKeymasterPaddingOverride;
    }
}
