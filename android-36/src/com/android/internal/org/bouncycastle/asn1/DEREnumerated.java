/* GENERATED SOURCE. DO NOT MODIFY. */
// Android-added: keep DER classes for backwards compatibility
package com.android.internal.org.bouncycastle.asn1;

import java.math.BigInteger;

/**
 * @deprecated Use ASN1Enumerated instead of this.
 * @hide This class is not part of the Android public SDK API
 */
public class DEREnumerated
    extends ASN1Enumerated
{
    /**
     * @param bytes the value of this enumerated as an encoded BigInteger (signed).
     * @deprecated use ASN1Enumerated
     */
    DEREnumerated(byte[] bytes)
    {
        super(bytes);
    }

    /**
     * @param value the value of this enumerated.
     * @deprecated use ASN1Enumerated
     */
    public DEREnumerated(BigInteger value)
    {
        super(value);
    }

    /**
     * @param value the value of this enumerated.
     * @deprecated use ASN1Enumerated
     */
    public DEREnumerated(int value)
    {
        super(value);
    }
}
// Android-added: keep DER classes for backwards compatibility
