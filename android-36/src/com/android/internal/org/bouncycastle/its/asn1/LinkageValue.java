/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.its.asn1;

import com.android.internal.org.bouncycastle.asn1.ASN1Object;
import com.android.internal.org.bouncycastle.asn1.ASN1OctetString;
import com.android.internal.org.bouncycastle.asn1.ASN1Primitive;
import com.android.internal.org.bouncycastle.asn1.DEROctetString;
import com.android.internal.org.bouncycastle.util.Arrays;

/**
 * <pre>
 *     LinkageValue ::= OCTET STRING (SIZE(9))
 * </pre>
 * @hide This class is not part of the Android public SDK API
 */
public class LinkageValue
    extends ASN1Object
{
    private final byte[] value;

    private LinkageValue(ASN1OctetString octs)
    {
        this.value = Arrays.clone(Utils.octetStringFixed(octs.getOctets(), 9));
    }

    public static LinkageValue getInstance(Object src)
    {
        if (src instanceof LinkageValue)
        {
            return (LinkageValue)src;
        }
        else if (src != null)
        {
            return new LinkageValue(ASN1OctetString.getInstance(src));
        }

        return null;
    }

    public ASN1Primitive toASN1Primitive()
    {
        return new DEROctetString(Arrays.clone(value));
    }
}
