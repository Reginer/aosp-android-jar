/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.its.asn1;

import com.android.internal.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.internal.org.bouncycastle.asn1.ASN1Object;
import com.android.internal.org.bouncycastle.asn1.ASN1Primitive;
import com.android.internal.org.bouncycastle.asn1.DERSequence;

/**
 * <pre>
 *     SequenceOfCertificate ::= SEQUENCE OF Certificate
 * </pre>
 * @hide This class is not part of the Android public SDK API
 */
public class SequenceOfCertificate
    extends ASN1Object
{
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        return new DERSequence(v);
    }
}
