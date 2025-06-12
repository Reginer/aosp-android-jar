/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.its.asn1;

import com.android.internal.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.internal.org.bouncycastle.asn1.ASN1Object;
import com.android.internal.org.bouncycastle.asn1.ASN1OctetString;
import com.android.internal.org.bouncycastle.asn1.ASN1Primitive;
import com.android.internal.org.bouncycastle.asn1.ASN1Sequence;
import com.android.internal.org.bouncycastle.asn1.DEROctetString;
import com.android.internal.org.bouncycastle.asn1.DERSequence;
import com.android.internal.org.bouncycastle.util.Arrays;

/**
 * <pre>
 *     BitmapSspRange ::= SEQUENCE {
 *         sspValue OCTET STRING (SIZE(1..32)),
 *         sspBitmask OCTET STRING (SIZE(1..32))
 *     }
 * </pre>
 * @hide This class is not part of the Android public SDK API
 */
public class BitmapSspRange
    extends ASN1Object
{
    private final byte[] sspValue;
    private final byte[] sspBitmask;

    private BitmapSspRange(ASN1Sequence seq)
    {
        if (seq.size() != 2)
        {
            throw new IllegalArgumentException("expected sequence with sspValue and sspBitmask");
        }

        sspValue = Utils.octetStringFixed(
            ASN1OctetString.getInstance(seq.getObjectAt(0)).getOctets());
        sspBitmask = Utils.octetStringFixed(
            ASN1OctetString.getInstance(seq.getObjectAt(1)).getOctets());
    }

    public static BitmapSspRange getInstance(Object o)
    {
        if (o instanceof BitmapSspRange)
        {
            return (BitmapSspRange)o;
        }
        else if (o != null)
        {
            return new BitmapSspRange(ASN1Sequence.getInstance(o));
        }

        return null;
    }

    public byte[] getSspValue()
    {
        return Arrays.clone(sspValue);
    }

    public byte[] getSspBitmask()
    {
        return Arrays.clone(sspBitmask);
    }

    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector avec = new ASN1EncodableVector();

        avec.add(new DEROctetString(sspValue));
        avec.add(new DEROctetString(sspBitmask));

        return new DERSequence(avec);
    }
}
