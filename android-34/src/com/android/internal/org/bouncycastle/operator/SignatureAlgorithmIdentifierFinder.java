/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.operator;

import com.android.internal.org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * @hide This class is not part of the Android public SDK API
 */
public interface SignatureAlgorithmIdentifierFinder
{
    /**
     * Find the signature algorithm identifier that matches with
     * the passed in signature algorithm name.
     *
     * @param sigAlgName the name of the signature algorithm of interest.
     * @return an algorithm identifier for the corresponding signature.
     */
    AlgorithmIdentifier find(String sigAlgName);
}