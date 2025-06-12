/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.util.apk;

import static android.util.apk.ApkSigningBlockUtils.CONTENT_DIGEST_VERITY_CHUNKED_SHA256;
import static android.util.apk.ApkSigningBlockUtils.compareSignatureAlgorithm;
import static android.util.apk.ApkSigningBlockUtils.getContentDigestAlgorithmJcaDigestAlgorithm;
import static android.util.apk.ApkSigningBlockUtils.getLengthPrefixedSlice;
import static android.util.apk.ApkSigningBlockUtils.getSignatureAlgorithmContentDigestAlgorithm;
import static android.util.apk.ApkSigningBlockUtils.getSignatureAlgorithmJcaKeyAlgorithm;
import static android.util.apk.ApkSigningBlockUtils.getSignatureAlgorithmJcaSignatureAlgorithm;
import static android.util.apk.ApkSigningBlockUtils.isSupportedSignatureAlgorithm;
import static android.util.apk.ApkSigningBlockUtils.readLengthPrefixedByteArray;

import android.util.ArrayMap;
import android.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * APK Signature Scheme v2 verifier.
 *
 * <p>APK Signature Scheme v2 is a whole-file signature scheme which aims to protect every single
 * bit of the APK, as opposed to the JAR Signature Scheme which protects only the names and
 * uncompressed contents of ZIP entries.
 *
 * @see <a href="https://source.android.com/security/apksigning/v2.html">APK Signature Scheme v2</a>
 *
 * @hide for internal use only.
 */
public class ApkSignatureSchemeV2Verifier {

    /**
     * ID of this signature scheme as used in X-Android-APK-Signed header used in JAR signing.
     */
    public static final int SF_ATTRIBUTE_ANDROID_APK_SIGNED_ID = 2;

    private static final int APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871a;

    /**
     * The maximum number of signers supported by the v2 APK signature scheme.
     */
    private static final int MAX_V2_SIGNERS = 10;

    /**
     * Returns {@code true} if the provided APK contains an APK Signature Scheme V2 signature.
     *
     * <p><b>NOTE: This method does not verify the signature.</b>
     */
    public static boolean hasSignature(String apkFile) throws IOException {
        try (RandomAccessFile apk = new RandomAccessFile(apkFile, "r")) {
            findSignature(apk);
            return true;
        } catch (SignatureNotFoundException e) {
            return false;
        }
    }

    /**
     * Verifies APK Signature Scheme v2 signatures of the provided APK and returns the certificates
     * associated with each signer.
     *
     * @throws SignatureNotFoundException if the APK is not signed using APK Signature Scheme v2.
     * @throws SecurityException          if an APK Signature Scheme v2 signature of this APK does
     *                                    not verify.
     * @throws IOException                if an I/O error occurs while reading the APK file.
     */
    public static X509Certificate[][] verify(String apkFile)
            throws SignatureNotFoundException, SecurityException, IOException {
        VerifiedSigner vSigner = verify(apkFile, true);
        return vSigner.certs;
    }

    /**
     * Returns the certificates associated with each signer for the given APK without verification.
     * This method is dangerous and should not be used, unless the caller is absolutely certain the
     * APK is trusted.  Specifically, verification is only done for the APK Signature Scheme v2
     * Block while gathering signer information.  The APK contents are not verified.
     *
     * @throws SignatureNotFoundException if the APK is not signed using APK Signature Scheme v2.
     * @throws IOException if an I/O error occurs while reading the APK file.
     */
    public static X509Certificate[][] unsafeGetCertsWithoutVerification(String apkFile)
            throws SignatureNotFoundException, SecurityException, IOException {
        VerifiedSigner vSigner = verify(apkFile, false);
        return vSigner.certs;
    }

    /**
     * Same as above returns the full signer object, containing additional info e.g. digest.
     */
    public static VerifiedSigner verify(String apkFile, boolean verifyIntegrity)
            throws SignatureNotFoundException, SecurityException, IOException {
        try (RandomAccessFile apk = new RandomAccessFile(apkFile, "r")) {
            return verify(apk, verifyIntegrity);
        }
    }

    /**
     * Verifies APK Signature Scheme v2 signatures of the provided APK and returns the certificates
     * associated with each signer.
     *
     * @throws SignatureNotFoundException if the APK is not signed using APK Signature Scheme v2.
     * @throws SecurityException if an APK Signature Scheme v2 signature of this APK does not
     *         verify.
     * @throws IOException if an I/O error occurs while reading the APK file.
     */
    private static VerifiedSigner verify(RandomAccessFile apk, boolean verifyIntegrity)
            throws SignatureNotFoundException, SecurityException, IOException {
        SignatureInfo signatureInfo = findSignature(apk);
        return verify(apk, signatureInfo, verifyIntegrity);
    }

    /**
     * Returns the APK Signature Scheme v2 block contained in the provided APK file and the
     * additional information relevant for verifying the block against the file.
     *
     * @throws SignatureNotFoundException if the APK is not signed using APK Signature Scheme v2.
     * @throws IOException if an I/O error occurs while reading the APK file.
     */
    public static SignatureInfo findSignature(RandomAccessFile apk)
            throws IOException, SignatureNotFoundException {
        return ApkSigningBlockUtils.findSignature(apk, APK_SIGNATURE_SCHEME_V2_BLOCK_ID);
    }

    /**
     * Verifies the contents of the provided APK file against the provided APK Signature Scheme v2
     * Block.
     *
     * @param signatureInfo APK Signature Scheme v2 Block and information relevant for verifying it
     *        against the APK file.
     */
    private static VerifiedSigner verify(
            RandomAccessFile apk,
            SignatureInfo signatureInfo,
            boolean doVerifyIntegrity) throws SecurityException, IOException {
        int signerCount = 0;
        Map<Integer, byte[]> contentDigests = new ArrayMap<>();
        List<X509Certificate[]> signerCerts = new ArrayList<>();
        CertificateFactory certFactory;
        try {
            certFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new RuntimeException("Failed to obtain X.509 CertificateFactory", e);
        }
        ByteBuffer signers;
        try {
            signers = getLengthPrefixedSlice(signatureInfo.signatureBlock);
        } catch (IOException e) {
            throw new SecurityException("Failed to read list of signers", e);
        }
        while (signers.hasRemaining()) {
            signerCount++;
            if (signerCount > MAX_V2_SIGNERS) {
                throw new SecurityException(
                        "APK Signature Scheme v2 only supports a maximum of " + MAX_V2_SIGNERS
                                + " signers");
            }
            try {
                ByteBuffer signer = getLengthPrefixedSlice(signers);
                X509Certificate[] certs = verifySigner(signer, contentDigests, certFactory);
                signerCerts.add(certs);
            } catch (IOException | BufferUnderflowException | SecurityException e) {
                throw new SecurityException(
                        "Failed to parse/verify signer #" + signerCount + " block",
                        e);
            }
        }

        if (signerCount < 1) {
            throw new SecurityException("No signers found");
        }

        if (contentDigests.isEmpty()) {
            throw new SecurityException("No content digests found");
        }

        if (doVerifyIntegrity) {
            ApkSigningBlockUtils.verifyIntegrity(contentDigests, apk, signatureInfo);
        }

        byte[] verityRootHash = null;
        if (contentDigests.containsKey(CONTENT_DIGEST_VERITY_CHUNKED_SHA256)) {
            byte[] verityDigest = contentDigests.get(CONTENT_DIGEST_VERITY_CHUNKED_SHA256);
            verityRootHash = ApkSigningBlockUtils.parseVerityDigestAndVerifySourceLength(
                    verityDigest, apk.getChannel().size(), signatureInfo);
        }

        return new VerifiedSigner(
                signerCerts.toArray(new X509Certificate[signerCerts.size()][]),
                verityRootHash, contentDigests);
    }

    private static X509Certificate[] verifySigner(
            ByteBuffer signerBlock,
            Map<Integer, byte[]> contentDigests,
            CertificateFactory certFactory) throws SecurityException, IOException {
        ByteBuffer signedData = getLengthPrefixedSlice(signerBlock);
        ByteBuffer signatures = getLengthPrefixedSlice(signerBlock);
        byte[] publicKeyBytes = readLengthPrefixedByteArray(signerBlock);

        int signatureCount = 0;
        int bestSigAlgorithm = -1;
        byte[] bestSigAlgorithmSignatureBytes = null;
        List<Integer> signaturesSigAlgorithms = new ArrayList<>();
        while (signatures.hasRemaining()) {
            signatureCount++;
            try {
                ByteBuffer signature = getLengthPrefixedSlice(signatures);
                if (signature.remaining() < 8) {
                    throw new SecurityException("Signature record too short");
                }
                int sigAlgorithm = signature.getInt();
                signaturesSigAlgorithms.add(sigAlgorithm);
                if (!isSupportedSignatureAlgorithm(sigAlgorithm)) {
                    continue;
                }
                if ((bestSigAlgorithm == -1)
                        || (compareSignatureAlgorithm(sigAlgorithm, bestSigAlgorithm) > 0)) {
                    bestSigAlgorithm = sigAlgorithm;
                    bestSigAlgorithmSignatureBytes = readLengthPrefixedByteArray(signature);
                }
            } catch (IOException | BufferUnderflowException e) {
                throw new SecurityException(
                        "Failed to parse signature record #" + signatureCount,
                        e);
            }
        }
        if (bestSigAlgorithm == -1) {
            if (signatureCount == 0) {
                throw new SecurityException("No signatures found");
            } else {
                throw new SecurityException("No supported signatures found");
            }
        }

        String keyAlgorithm = getSignatureAlgorithmJcaKeyAlgorithm(bestSigAlgorithm);
        Pair<String, ? extends AlgorithmParameterSpec> signatureAlgorithmParams =
                getSignatureAlgorithmJcaSignatureAlgorithm(bestSigAlgorithm);
        String jcaSignatureAlgorithm = signatureAlgorithmParams.first;
        AlgorithmParameterSpec jcaSignatureAlgorithmParams = signatureAlgorithmParams.second;
        boolean sigVerified;
        try {
            PublicKey publicKey =
                    KeyFactory.getInstance(keyAlgorithm)
                            .generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            Signature sig = Signature.getInstance(jcaSignatureAlgorithm);
            sig.initVerify(publicKey);
            if (jcaSignatureAlgorithmParams != null) {
                sig.setParameter(jcaSignatureAlgorithmParams);
            }
            sig.update(signedData);
            sigVerified = sig.verify(bestSigAlgorithmSignatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException
                | InvalidAlgorithmParameterException | SignatureException e) {
            throw new SecurityException(
                    "Failed to verify " + jcaSignatureAlgorithm + " signature", e);
        }
        if (!sigVerified) {
            throw new SecurityException(jcaSignatureAlgorithm + " signature did not verify");
        }

        // Signature over signedData has verified.

        byte[] contentDigest = null;
        signedData.clear();
        ByteBuffer digests = getLengthPrefixedSlice(signedData);
        List<Integer> digestsSigAlgorithms = new ArrayList<>();
        int digestCount = 0;
        while (digests.hasRemaining()) {
            digestCount++;
            try {
                ByteBuffer digest = getLengthPrefixedSlice(digests);
                if (digest.remaining() < 8) {
                    throw new IOException("Record too short");
                }
                int sigAlgorithm = digest.getInt();
                digestsSigAlgorithms.add(sigAlgorithm);
                if (sigAlgorithm == bestSigAlgorithm) {
                    contentDigest = readLengthPrefixedByteArray(digest);
                }
            } catch (IOException | BufferUnderflowException e) {
                throw new IOException("Failed to parse digest record #" + digestCount, e);
            }
        }

        if (!signaturesSigAlgorithms.equals(digestsSigAlgorithms)) {
            throw new SecurityException(
                    "Signature algorithms don't match between digests and signatures records");
        }
        int digestAlgorithm = getSignatureAlgorithmContentDigestAlgorithm(bestSigAlgorithm);
        byte[] previousSignerDigest = contentDigests.put(digestAlgorithm, contentDigest);
        if ((previousSignerDigest != null)
                && (!MessageDigest.isEqual(previousSignerDigest, contentDigest))) {
            throw new SecurityException(
                    getContentDigestAlgorithmJcaDigestAlgorithm(digestAlgorithm)
                    + " contents digest does not match the digest specified by a preceding signer");
        }

        ByteBuffer certificates = getLengthPrefixedSlice(signedData);
        List<X509Certificate> certs = new ArrayList<>();
        int certificateCount = 0;
        while (certificates.hasRemaining()) {
            certificateCount++;
            byte[] encodedCert = readLengthPrefixedByteArray(certificates);
            X509Certificate certificate;
            try {
                certificate = (X509Certificate)
                        certFactory.generateCertificate(new ByteArrayInputStream(encodedCert));
            } catch (CertificateException e) {
                throw new SecurityException("Failed to decode certificate #" + certificateCount, e);
            }
            certificate = new VerbatimX509Certificate(certificate, encodedCert);
            certs.add(certificate);
        }

        if (certs.isEmpty()) {
            throw new SecurityException("No certificates listed");
        }
        X509Certificate mainCertificate = certs.get(0);
        byte[] certificatePublicKeyBytes = mainCertificate.getPublicKey().getEncoded();
        if (!Arrays.equals(publicKeyBytes, certificatePublicKeyBytes)) {
            throw new SecurityException(
                    "Public key mismatch between certificate and signature record");
        }

        ByteBuffer additionalAttrs = getLengthPrefixedSlice(signedData);
        verifyAdditionalAttributes(additionalAttrs);

        return certs.toArray(new X509Certificate[certs.size()]);
    }

    // Attribute to check whether a newer APK Signature Scheme signature was stripped
    private static final int STRIPPING_PROTECTION_ATTR_ID = 0xbeeff00d;

    private static void verifyAdditionalAttributes(ByteBuffer attrs)
            throws SecurityException, IOException {
        while (attrs.hasRemaining()) {
            ByteBuffer attr = getLengthPrefixedSlice(attrs);
            if (attr.remaining() < 4) {
                throw new IOException("Remaining buffer too short to contain additional attribute "
                        + "ID. Remaining: " + attr.remaining());
            }
            int id = attr.getInt();
            switch (id) {
                case STRIPPING_PROTECTION_ATTR_ID:
                    if (attr.remaining() < 4) {
                        throw new IOException("V2 Signature Scheme Stripping Protection Attribute "
                                + " value too small.  Expected 4 bytes, but found "
                                + attr.remaining());
                    }
                    int vers = attr.getInt();
                    if (vers == ApkSignatureSchemeV3Verifier.SF_ATTRIBUTE_ANDROID_APK_SIGNED_ID) {
                        throw new SecurityException("V2 signature indicates APK is signed using APK"
                                + " Signature Scheme v3, but none was found. Signature stripped?");
                    }
                    break;
                default:
                    // not the droid we're looking for, move along, move along.
                    break;
            }
        }
    }

    static byte[] getVerityRootHash(String apkPath)
            throws IOException, SignatureNotFoundException, SecurityException {
        try (RandomAccessFile apk = new RandomAccessFile(apkPath, "r")) {
            SignatureInfo signatureInfo = findSignature(apk);
            VerifiedSigner vSigner = verify(apk, false);
            return vSigner.verityRootHash;
        }
    }

    static byte[] generateApkVerity(String apkPath, ByteBufferFactory bufferFactory)
            throws IOException, SignatureNotFoundException, SecurityException, DigestException,
                   NoSuchAlgorithmException {
        try (RandomAccessFile apk = new RandomAccessFile(apkPath, "r")) {
            SignatureInfo signatureInfo = findSignature(apk);
            return VerityBuilder.generateApkVerity(apkPath, bufferFactory, signatureInfo);
        }
    }

    /**
     * Verified APK Signature Scheme v2 signer.
     *
     * @hide for internal use only.
     */
    public static class VerifiedSigner {
        public final X509Certificate[][] certs;

        public final byte[] verityRootHash;
        // Algorithm -> digest map of signed digests in the signature.
        // All these are verified if requested.
        public final Map<Integer, byte[]> contentDigests;

        public VerifiedSigner(X509Certificate[][] certs, byte[] verityRootHash,
                Map<Integer, byte[]> contentDigests) {
            this.certs = certs;
            this.verityRootHash = verityRootHash;
            this.contentDigests = contentDigests;
        }

    }
}
