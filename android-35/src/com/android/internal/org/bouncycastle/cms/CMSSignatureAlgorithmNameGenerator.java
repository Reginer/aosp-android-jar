/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cms;

import com.android.internal.org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * @hide This class is not part of the Android public SDK API
 */
public interface CMSSignatureAlgorithmNameGenerator
{
    /**
     * Return the digest algorithm using one of the standard string
     * representations rather than the algorithm object identifier (if possible).
     *
     * @param digestAlg the digest algorithm id.
     * @param encryptionAlg the encryption, or signing, algorithm id.
     */
    String getSignatureName(AlgorithmIdentifier digestAlg, AlgorithmIdentifier encryptionAlg);
}
