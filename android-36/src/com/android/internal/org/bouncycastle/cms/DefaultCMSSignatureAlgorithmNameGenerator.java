/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cms;

import java.util.HashMap;
import java.util.Map;

import com.android.internal.org.bouncycastle.asn1.ASN1ObjectIdentifier;
// Android-removed: Unsupported algorithms
// import org.bouncycastle.asn1.bc.BCObjectIdentifiers;
// import org.bouncycastle.asn1.bsi.BSIObjectIdentifiers;
// import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
// import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
// import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
// import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
// import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
// import org.bouncycastle.asn1.misc.MiscObjectIdentifiers;
import com.android.internal.org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import com.android.internal.org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import com.android.internal.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
// import org.bouncycastle.asn1.rosstandart.RosstandartObjectIdentifiers;
import com.android.internal.org.bouncycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
import com.android.internal.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.internal.org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import com.android.internal.org.bouncycastle.asn1.x9.X9ObjectIdentifiers;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class DefaultCMSSignatureAlgorithmNameGenerator
    implements CMSSignatureAlgorithmNameGenerator
{
    private final Map encryptionAlgs = new HashMap();
    private final Map     digestAlgs = new HashMap();
    private final Map     simpleAlgs = new HashMap();

    private void addEntries(ASN1ObjectIdentifier alias, String digest, String encryption)
    {
        digestAlgs.put(alias, digest);
        encryptionAlgs.put(alias, encryption);
    }

    public DefaultCMSSignatureAlgorithmNameGenerator()
    {
        addEntries(NISTObjectIdentifiers.dsa_with_sha224, "SHA224", "DSA");
        addEntries(NISTObjectIdentifiers.dsa_with_sha256, "SHA256", "DSA");
        addEntries(NISTObjectIdentifiers.dsa_with_sha384, "SHA384", "DSA");
        addEntries(NISTObjectIdentifiers.dsa_with_sha512, "SHA512", "DSA");
        // BEGIN Android-removed: Unsupported algorithms
        /*
        addEntries(NISTObjectIdentifiers.id_dsa_with_sha3_224, "SHA3-224", "DSA");
        addEntries(NISTObjectIdentifiers.id_dsa_with_sha3_256, "SHA3-256", "DSA");
        addEntries(NISTObjectIdentifiers.id_dsa_with_sha3_384, "SHA3-384", "DSA");
        addEntries(NISTObjectIdentifiers.id_dsa_with_sha3_512, "SHA3-512", "DSA");
        addEntries(NISTObjectIdentifiers.id_rsassa_pkcs1_v1_5_with_sha3_224, "SHA3-224", "RSA");
        addEntries(NISTObjectIdentifiers.id_rsassa_pkcs1_v1_5_with_sha3_256, "SHA3-256", "RSA");
        addEntries(NISTObjectIdentifiers.id_rsassa_pkcs1_v1_5_with_sha3_384, "SHA3-384", "RSA");
        addEntries(NISTObjectIdentifiers.id_rsassa_pkcs1_v1_5_with_sha3_512, "SHA3-512", "RSA");
        addEntries(NISTObjectIdentifiers.id_ecdsa_with_sha3_224, "SHA3-224", "ECDSA");
        addEntries(NISTObjectIdentifiers.id_ecdsa_with_sha3_256, "SHA3-256", "ECDSA");
        addEntries(NISTObjectIdentifiers.id_ecdsa_with_sha3_384, "SHA3-384", "ECDSA");
        addEntries(NISTObjectIdentifiers.id_ecdsa_with_sha3_512, "SHA3-512", "ECDSA");
        */
        // END Android-removed: Unsupported algorithms
        addEntries(OIWObjectIdentifiers.dsaWithSHA1, "SHA1", "DSA");
        // BEGIN Android-removed: Unsupported algorithms
        // addEntries(OIWObjectIdentifiers.md4WithRSA, "MD4", "RSA");
        // addEntries(OIWObjectIdentifiers.md4WithRSAEncryption, "MD4", "RSA");
        // END Android-removed: Unsupported algorithms
        addEntries(OIWObjectIdentifiers.md5WithRSA, "MD5", "RSA");
        addEntries(OIWObjectIdentifiers.sha1WithRSA, "SHA1", "RSA");
        // BEGIN Android-removed: Unsupported algorithms
        // addEntries(PKCSObjectIdentifiers.md2WithRSAEncryption, "MD2", "RSA");
        // addEntries(PKCSObjectIdentifiers.md4WithRSAEncryption, "MD4", "RSA");
        // END Android-removed: Unsupported algorithms
        addEntries(PKCSObjectIdentifiers.md5WithRSAEncryption, "MD5", "RSA");
        addEntries(PKCSObjectIdentifiers.sha1WithRSAEncryption, "SHA1", "RSA");
        addEntries(PKCSObjectIdentifiers.sha224WithRSAEncryption, "SHA224", "RSA");
        addEntries(PKCSObjectIdentifiers.sha256WithRSAEncryption, "SHA256", "RSA");
        addEntries(PKCSObjectIdentifiers.sha384WithRSAEncryption, "SHA384", "RSA");
        addEntries(PKCSObjectIdentifiers.sha512WithRSAEncryption, "SHA512", "RSA");
        addEntries(PKCSObjectIdentifiers.sha512_224WithRSAEncryption, "SHA512(224)", "RSA");
        addEntries(PKCSObjectIdentifiers.sha512_256WithRSAEncryption, "SHA512(256)", "RSA");
        addEntries(NISTObjectIdentifiers.id_rsassa_pkcs1_v1_5_with_sha3_224, "SHA3-224", "RSA");
        addEntries(NISTObjectIdentifiers.id_rsassa_pkcs1_v1_5_with_sha3_256, "SHA3-256", "RSA");
        addEntries(NISTObjectIdentifiers.id_rsassa_pkcs1_v1_5_with_sha3_384, "SHA3-384", "RSA");
        addEntries(NISTObjectIdentifiers.id_rsassa_pkcs1_v1_5_with_sha3_512, "SHA3-512", "RSA");

        // BEGIN Android-removed: Unsupported algorithms
        /*
        addEntries(CMSObjectIdentifiers.id_RSASSA_PSS_SHAKE128, "SHAKE128", "RSAPSS");
        addEntries(CMSObjectIdentifiers.id_RSASSA_PSS_SHAKE256, "SHAKE256", "RSAPSS");
        addEntries(TeleTrusTObjectIdentifiers.rsaSignatureWithripemd128, "RIPEMD128", "RSA");
        addEntries(TeleTrusTObjectIdentifiers.rsaSignatureWithripemd160, "RIPEMD160", "RSA");
        addEntries(TeleTrusTObjectIdentifiers.rsaSignatureWithripemd256, "RIPEMD256", "RSA");
        addEntries(CMSObjectIdentifiers.id_ecdsa_with_shake128, "SHAKE128", "ECDSA");
        addEntries(CMSObjectIdentifiers.id_ecdsa_with_shake256, "SHAKE256", "ECDSA");
        */
        // END Android-removed: Unsupported algorithms

        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA1, "SHA1", "ECDSA");
        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA224, "SHA224", "ECDSA");
        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA256, "SHA256", "ECDSA");
        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA384, "SHA384", "ECDSA");
        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA512, "SHA512", "ECDSA");
        addEntries(X9ObjectIdentifiers.id_dsa_with_sha1, "SHA1", "DSA");
        // BEGIN Android-removed: Unsupported algorithms
        /*
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_1, "SHA1", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_224, "SHA224", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_256, "SHA256", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_384, "SHA384", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_512, "SHA512", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_RSA_v1_5_SHA_1, "SHA1", "RSA");
        addEntries(EACObjectIdentifiers.id_TA_RSA_v1_5_SHA_256, "SHA256", "RSA");
        addEntries(EACObjectIdentifiers.id_TA_RSA_PSS_SHA_1, "SHA1", "RSAandMGF1");
        addEntries(EACObjectIdentifiers.id_TA_RSA_PSS_SHA_256, "SHA256", "RSAandMGF1");
        addEntries(BSIObjectIdentifiers.ecdsa_plain_SHA1, "SHA1", "PLAIN-ECDSA");
        addEntries(BSIObjectIdentifiers.ecdsa_plain_SHA224, "SHA224", "PLAIN-ECDSA");
        addEntries(BSIObjectIdentifiers.ecdsa_plain_SHA256, "SHA256", "PLAIN-ECDSA");
        addEntries(BSIObjectIdentifiers.ecdsa_plain_SHA384, "SHA384", "PLAIN-ECDSA");
        addEntries(BSIObjectIdentifiers.ecdsa_plain_SHA512, "SHA512", "PLAIN-ECDSA");
        addEntries(BSIObjectIdentifiers.ecdsa_plain_RIPEMD160, "RIPEMD160", "PLAIN-ECDSA");
        addEntries(BSIObjectIdentifiers.ecdsa_plain_SHA3_224, "SHA3-224", "PLAIN-ECDSA");
        addEntries(BSIObjectIdentifiers.ecdsa_plain_SHA3_256, "SHA3-256", "PLAIN-ECDSA");
        addEntries(BSIObjectIdentifiers.ecdsa_plain_SHA3_384, "SHA3-384", "PLAIN-ECDSA");
        addEntries(BSIObjectIdentifiers.ecdsa_plain_SHA3_512, "SHA3-512", "PLAIN-ECDSA");

//        addEntries(GMObjectIdentifiers.sm2sign_with_rmd160, "RIPEMD160", "SM2");
//        addEntries(GMObjectIdentifiers.sm2sign_with_sha1, "SHA1", "SM2");
//        addEntries(GMObjectIdentifiers.sm2sign_with_sha224, "SHA224", "SM2");
        addEntries(GMObjectIdentifiers.sm2sign_with_sha256, "SHA256", "SM2");
//        addEntries(GMObjectIdentifiers.sm2sign_with_sha384, "SHA384", "SM2");
//        addEntries(GMObjectIdentifiers.sm2sign_with_sha512, "SHA512", "SM2");
        addEntries(GMObjectIdentifiers.sm2sign_with_sm3, "SM3", "SM2");

        addEntries(BCObjectIdentifiers.sphincs256_with_SHA512, "SHA512", "SPHINCS256");
        addEntries(BCObjectIdentifiers.sphincs256_with_SHA3_512, "SHA3-512", "SPHINCS256");

        addEntries(BCObjectIdentifiers.picnic_with_shake256, "SHAKE256", "Picnic");
        addEntries(BCObjectIdentifiers.picnic_with_sha512, "SHA512", "Picnic");
        addEntries(BCObjectIdentifiers.picnic_with_sha3_512, "SHA3-512", "Picnic");

        addEntries(GMObjectIdentifiers.sm2sign_with_rmd160, "RIPEMD160", "SM2");
        addEntries(GMObjectIdentifiers.sm2sign_with_sha1, "SHA1", "SM2");
        addEntries(GMObjectIdentifiers.sm2sign_with_sha224, "SHA224", "SM2");
        addEntries(GMObjectIdentifiers.sm2sign_with_sha256, "SHA256", "SM2");
        addEntries(GMObjectIdentifiers.sm2sign_with_sha384, "SHA384", "SM2");
        addEntries(GMObjectIdentifiers.sm2sign_with_sha512, "SHA512", "SM2");
        addEntries(GMObjectIdentifiers.sm2sign_with_sm3, "SM3", "SM2");
        */
        // END Android-removed: Unsupported algorithms

        encryptionAlgs.put(X9ObjectIdentifiers.id_dsa, "DSA");
        encryptionAlgs.put(PKCSObjectIdentifiers.rsaEncryption, "RSA");
        encryptionAlgs.put(TeleTrusTObjectIdentifiers.teleTrusTRSAsignatureAlgorithm, "RSA");
        encryptionAlgs.put(X509ObjectIdentifiers.id_ea_rsa, "RSA");
        encryptionAlgs.put(PKCSObjectIdentifiers.id_RSASSA_PSS, "RSAandMGF1");
        // BEGIN Android-removed: Unsupported algorithms
        /*
        encryptionAlgs.put(CryptoProObjectIdentifiers.gostR3410_94, "GOST3410");
        encryptionAlgs.put(CryptoProObjectIdentifiers.gostR3410_2001, "ECGOST3410");
        encryptionAlgs.put(new ASN1ObjectIdentifier("1.3.6.1.4.1.5849.1.6.2"), "ECGOST3410");
        encryptionAlgs.put(new ASN1ObjectIdentifier("1.3.6.1.4.1.5849.1.1.5"), "GOST3410");
        encryptionAlgs.put(RosstandartObjectIdentifiers.id_tc26_gost_3410_12_256, "ECGOST3410-2012-256");
        encryptionAlgs.put(RosstandartObjectIdentifiers.id_tc26_gost_3410_12_512, "ECGOST3410-2012-512");
        encryptionAlgs.put(CryptoProObjectIdentifiers.gostR3411_94_with_gostR3410_2001, "ECGOST3410");
        encryptionAlgs.put(CryptoProObjectIdentifiers.gostR3411_94_with_gostR3410_94, "GOST3410");
        encryptionAlgs.put(RosstandartObjectIdentifiers.id_tc26_signwithdigest_gost_3410_12_256, "ECGOST3410-2012-256");
        encryptionAlgs.put(RosstandartObjectIdentifiers.id_tc26_signwithdigest_gost_3410_12_512, "ECGOST3410-2012-512");
        encryptionAlgs.put(X9ObjectIdentifiers.id_ecPublicKey, "ECDSA");

        digestAlgs.put(PKCSObjectIdentifiers.md2, "MD2");
        digestAlgs.put(PKCSObjectIdentifiers.md4, "MD4");
        */
        // END Android-removed: Unsupported algorithms
        digestAlgs.put(PKCSObjectIdentifiers.md5, "MD5");
        digestAlgs.put(OIWObjectIdentifiers.idSHA1, "SHA1");
        digestAlgs.put(NISTObjectIdentifiers.id_sha224, "SHA224");
        digestAlgs.put(NISTObjectIdentifiers.id_sha256, "SHA256");
        digestAlgs.put(NISTObjectIdentifiers.id_sha384, "SHA384");
        digestAlgs.put(NISTObjectIdentifiers.id_sha512, "SHA512");
        // BEGIN Android-removed: Unsupported algorithms
        /*
        digestAlgs.put(NISTObjectIdentifiers.id_sha512_224, "SHA512(224)");
        digestAlgs.put(NISTObjectIdentifiers.id_sha512_256, "SHA512(256)");
        digestAlgs.put(NISTObjectIdentifiers.id_shake128, "SHAKE128");
        digestAlgs.put(NISTObjectIdentifiers.id_shake256, "SHAKE256");
        digestAlgs.put(NISTObjectIdentifiers.id_sha3_224, "SHA3-224");
        digestAlgs.put(NISTObjectIdentifiers.id_sha3_256, "SHA3-256");
        digestAlgs.put(NISTObjectIdentifiers.id_sha3_384, "SHA3-384");
        digestAlgs.put(NISTObjectIdentifiers.id_sha3_512, "SHA3-512");
        digestAlgs.put(TeleTrusTObjectIdentifiers.ripemd128, "RIPEMD128");
        digestAlgs.put(TeleTrusTObjectIdentifiers.ripemd160, "RIPEMD160");
        digestAlgs.put(TeleTrusTObjectIdentifiers.ripemd256, "RIPEMD256");
        digestAlgs.put(CryptoProObjectIdentifiers.gostR3411,  "GOST3411");
        digestAlgs.put(new ASN1ObjectIdentifier("1.3.6.1.4.1.5849.1.2.1"),  "GOST3411");
        digestAlgs.put(RosstandartObjectIdentifiers.id_tc26_gost_3411_12_256,  "GOST3411-2012-256");
        digestAlgs.put(RosstandartObjectIdentifiers.id_tc26_gost_3411_12_512,  "GOST3411-2012-512");
        digestAlgs.put(GMObjectIdentifiers.sm3,  "SM3");

        simpleAlgs.put(EdECObjectIdentifiers.id_Ed25519, "Ed25519");
        simpleAlgs.put(EdECObjectIdentifiers.id_Ed448, "Ed448");
        simpleAlgs.put(PKCSObjectIdentifiers.id_alg_hss_lms_hashsig, "LMS");

        simpleAlgs.put(MiscObjectIdentifiers.id_alg_composite, "COMPOSITE");
        simpleAlgs.put(BCObjectIdentifiers.falcon_512, "Falcon-512");
        simpleAlgs.put(BCObjectIdentifiers.falcon_1024, "Falcon-1024");
        simpleAlgs.put(BCObjectIdentifiers.dilithium2, "Dilithium2");
        simpleAlgs.put(BCObjectIdentifiers.dilithium3, "Dilithium3");
        simpleAlgs.put(BCObjectIdentifiers.dilithium5, "Dilithium5");
        simpleAlgs.put(BCObjectIdentifiers.sphincsPlus_sha2_128s, "SPHINCS+-SHA2-128s");
        simpleAlgs.put(BCObjectIdentifiers.sphincsPlus_sha2_128f, "SPHINCS+-SHA2-128f");
        simpleAlgs.put(BCObjectIdentifiers.sphincsPlus_sha2_192s, "SPHINCS+-SHA2-192s");
        simpleAlgs.put(BCObjectIdentifiers.sphincsPlus_sha2_192f, "SPHINCS+-SHA2-192f");
        simpleAlgs.put(BCObjectIdentifiers.sphincsPlus_sha2_256s, "SPHINCS+-SHA2-256s");
        simpleAlgs.put(BCObjectIdentifiers.sphincsPlus_sha2_256f, "SPHINCS+-SHA2-256f");
        simpleAlgs.put(BCObjectIdentifiers.sphincsPlus_shake_128s, "SPHINCS+-SHAKE-128s");
        simpleAlgs.put(BCObjectIdentifiers.sphincsPlus_shake_128f, "SPHINCS+-SHAKE-128f");
        simpleAlgs.put(BCObjectIdentifiers.sphincsPlus_shake_192s, "SPHINCS+-SHAKE-192s");
        simpleAlgs.put(BCObjectIdentifiers.sphincsPlus_shake_192f, "SPHINCS+-SHAKE-192f");
        simpleAlgs.put(BCObjectIdentifiers.sphincsPlus_shake_256s, "SPHINCS+-SHAKE-256s");
        simpleAlgs.put(BCObjectIdentifiers.sphincsPlus_shake_256f, "SPHINCS+-SHAKE-256f");
        simpleAlgs.put(BCObjectIdentifiers.dilithium2, "Dilithium2");
        simpleAlgs.put(BCObjectIdentifiers.dilithium3, "Dilithium3");
        simpleAlgs.put(BCObjectIdentifiers.dilithium5, "Dilithium5");

        simpleAlgs.put(BCObjectIdentifiers.picnic_signature, "Picnic");
        */
        // END Android-removed: Unsupported algorithms
    }

    /**
     * Return the digest algorithm using one of the standard JCA string
     * representations rather than the algorithm identifier (if possible).
     */
    private String getDigestAlgName(
        ASN1ObjectIdentifier digestAlgOID)
    {
        String algName = (String)digestAlgs.get(digestAlgOID);

        if (algName != null)
        {
            return algName;
        }

        return digestAlgOID.getId();
    }

    /**
     * Return the digest encryption algorithm using one of the standard
     * JCA string representations rather the the algorithm identifier (if
     * possible).
     */
    private String getEncryptionAlgName(
        ASN1ObjectIdentifier encryptionAlgOID)
    {
        String algName = (String)encryptionAlgs.get(encryptionAlgOID);

        if (algName != null)
        {
            return algName;
        }

        return encryptionAlgOID.getId();
    }

    /**
     * Set the mapping for the encryption algorithm used in association with a SignedData generation
     * or interpretation.
     *
     * @param oid object identifier to map.
     * @param algorithmName algorithm name to use.
     */
    protected void setSigningEncryptionAlgorithmMapping(ASN1ObjectIdentifier oid, String algorithmName)
    {
        encryptionAlgs.put(oid, algorithmName);
    }

    /**
     * Set the mapping for the digest algorithm to use in conjunction with a SignedData generation
     * or interpretation.
     *
     * @param oid object identifier to map.
     * @param algorithmName algorithm name to use.
     */
    protected void setSigningDigestAlgorithmMapping(ASN1ObjectIdentifier oid, String algorithmName)
    {
        digestAlgs.put(oid, algorithmName);
    }

    public String getSignatureName(AlgorithmIdentifier digestAlg, AlgorithmIdentifier encryptionAlg)
    {
        // BEGIN Android-removed: unsupported algorithms
        /*
        if (EdECObjectIdentifiers.id_Ed25519.equals(encryptionAlg.getAlgorithm()))
        {
            return "Ed25519";
        }
        if (EdECObjectIdentifiers.id_Ed448.equals(encryptionAlg.getAlgorithm()))
        {
            return "Ed448";
        }

        // if (encryptionAlgOID.on(BCObjectIdentifiers.sphincsPlus))
        // {
        //     return "SPHINCSPlus";
        // }
        */
        // END Android-removed: unsupported algorithms

        ASN1ObjectIdentifier encryptionAlgOID = encryptionAlg.getAlgorithm();

        String simpleAlgName = (String)simpleAlgs.get(encryptionAlgOID);
        if (simpleAlgName != null)
        {
            return simpleAlgName;
        }

        String digestName = getDigestAlgName(encryptionAlgOID);

        if (!digestName.equals(encryptionAlgOID.getId()))
        {
            return digestName + "with" + getEncryptionAlgName(encryptionAlgOID);
        }

        return getDigestAlgName(digestAlg.getAlgorithm()) + "with" + getEncryptionAlgName(encryptionAlgOID);
    }
}
