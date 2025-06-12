/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.its.asn1;

import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.DERSequence;

/**
 * <pre>
 *     CertificateBase ::= SEQUENCE {
 *         version Uint8(3),
 *         type CertificateType,
 *         issuer IssuerIdentifier,
 *         toBeSigned ToBeSignedCertificate,
 *         signature Signature OPTIONAL
 *     }
 * </pre>
 * @hide This class is not part of the Android public SDK API
 */
public class CertificateBase
    extends ASN1Object
{
    private CertificateType type;
    private byte[] version;

    protected CertificateBase(ASN1Sequence seq)
    {

    }

    public static CertificateBase getInstance(Object o)
    {
        if (o instanceof ImplicitCertificate)
        {
            return (ImplicitCertificate)o;
        }
        if (o instanceof ExplicitCertificate)
        {
            return (ExplicitCertificate)o;
        }

        if (o != null)
        {
            ASN1Sequence seq = ASN1Sequence.getInstance(o);

            if (seq.getObjectAt(1).equals(CertificateType.Implicit))
            {
                return ImplicitCertificate.getInstance(seq);
            }
            if (seq.getObjectAt(1).equals(CertificateType.Explicit))
            {
                return ExplicitCertificate.getInstance(seq);
            }
            throw new IllegalArgumentException("unknown certificate type");
        }

        return null;
    }

    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        return new DERSequence(v);
    }
}
