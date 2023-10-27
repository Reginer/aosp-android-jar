/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.operator;

import com.android.internal.org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * The base interface for a provider of DigestCalculator implementations.
 * @hide This class is not part of the Android public SDK API
 */
public interface DigestCalculatorProvider
{
    DigestCalculator get(AlgorithmIdentifier digestAlgorithmIdentifier)
        throws OperatorCreationException;
}
