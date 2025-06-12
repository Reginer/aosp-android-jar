/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.operator;

import com.android.internal.org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * @hide This class is not part of the Android public SDK API
 */
public interface PBEMacCalculatorProvider
{
    MacCalculator get(AlgorithmIdentifier algorithm, char[] password)
        throws OperatorCreationException;
}
