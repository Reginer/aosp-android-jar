/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.operator;

import com.android.internal.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.internal.org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * @hide This class is not part of the Android public SDK API
 */
public interface DigestAlgorithmIdentifierFinder
{
    /**
     * Find the digest algorithm identifier that matches with
     * the passed in signature algorithm identifier.
     *
     * @param sigAlgId the signature algorithm of interest.
     * @return an algorithm identifier for the corresponding digest.
     */
    AlgorithmIdentifier find(AlgorithmIdentifier sigAlgId);

    /**
     * Find the algorithm identifier that matches with
     * the passed in digest OID.
     *
     * @param digestOid the OID of the digest algorithm of interest.
     * @return an algorithm identifier for the digest signature.
     */
    AlgorithmIdentifier find(ASN1ObjectIdentifier digestOid);

    /**
     * Find the algorithm identifier that matches with
     * the passed in digest name.
     *
     * @param digAlgName the name of the digest algorithm of interest.
     * @return an algorithm identifier for the digest signature.
     */
    AlgorithmIdentifier find(String digAlgName);
}