/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.its.asn1;

import java.math.BigInteger;

import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.util.BigIntegers;

/**
 * <pre>
 *     Uint16 ::= INTEGER (0..65535)
 *
 *     IValue ::= Uint16
 * </pre>
 * @hide This class is not part of the Android public SDK API
 */
public class IValue
    extends ASN1Object
{
    private final BigInteger value;

    private IValue(ASN1Integer value)
    {
        int i = BigIntegers.intValueExact(value.getValue());

        if (i < 0 || i > 65535)
        {
            throw new IllegalArgumentException("value out of range");
        }

        this.value = value.getValue();
    }

    public static IValue getInstance(Object src)
    {
        if (src instanceof IValue)
        {
            return (IValue)src;
        }
        else if (src != null)
        {
            return new IValue(ASN1Integer.getInstance(src));
        }

        return null;
    }

    public ASN1Primitive toASN1Primitive()
    {
        return new ASN1Integer(value);
    }
}
