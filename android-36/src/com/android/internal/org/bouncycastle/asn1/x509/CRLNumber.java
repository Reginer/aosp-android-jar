/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.asn1.x509;

import java.math.BigInteger;

import com.android.internal.org.bouncycastle.asn1.ASN1Integer;
import com.android.internal.org.bouncycastle.asn1.ASN1Object;
import com.android.internal.org.bouncycastle.asn1.ASN1Primitive;
import com.android.internal.org.bouncycastle.util.BigIntegers;

/**
 * The CRLNumber object.
 * <pre>
 * CRLNumber::= INTEGER(0..MAX)
 * </pre>
 * @hide This class is not part of the Android public SDK API
 */
public class CRLNumber
    extends ASN1Object
{
    private BigInteger number;

    public CRLNumber(
        BigInteger number)
    {
        if (BigIntegers.ZERO.compareTo(number) > 0)
        {
            throw new IllegalArgumentException("Invalid CRL number : not in (0..MAX)");
        }
        this.number = number;
    }

    public BigInteger getCRLNumber()
    {
        return number;
    }

    public String toString()
    {
        return "CRLNumber: " + getCRLNumber();
    }

    public ASN1Primitive toASN1Primitive()
    {
        return new ASN1Integer(number);
    }

    public static CRLNumber getInstance(Object o)
    {
        if (o instanceof CRLNumber)
        {
            return (CRLNumber)o;
        }
        else if (o != null)
        {
            return new CRLNumber(ASN1Integer.getInstance(o).getValue());
        }

        return null;
    }
}
