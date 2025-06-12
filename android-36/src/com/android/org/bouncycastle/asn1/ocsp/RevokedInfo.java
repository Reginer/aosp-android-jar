/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.asn1.ocsp;

import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Enumerated;
import com.android.org.bouncycastle.asn1.ASN1GeneralizedTime;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.DERTaggedObject;
import com.android.org.bouncycastle.asn1.x509.CRLReason;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class RevokedInfo
    extends ASN1Object
{
    private ASN1GeneralizedTime  revocationTime;
    private CRLReason           revocationReason;

    public RevokedInfo(ASN1GeneralizedTime revocationTime)
    {
        this(revocationTime, null);
    }

    public RevokedInfo(
        ASN1GeneralizedTime  revocationTime,
        CRLReason           revocationReason)
    {
        this.revocationTime = revocationTime;
        this.revocationReason = revocationReason;
    }

    private RevokedInfo(
        ASN1Sequence    seq)
    {
        this.revocationTime = ASN1GeneralizedTime.getInstance(seq.getObjectAt(0));

        if (seq.size() > 1)
        {
            this.revocationReason = CRLReason.getInstance(ASN1Enumerated.getInstance(
                (ASN1TaggedObject)seq.getObjectAt(1), true));
        }
    }

    public static RevokedInfo getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static RevokedInfo getInstance(
        Object  obj)
    {
        if (obj instanceof RevokedInfo)
        {
            return (RevokedInfo)obj;
        }
        else if (obj != null)
        {
            return new RevokedInfo(ASN1Sequence.getInstance(obj));
        }

        return null;
    }

    public ASN1GeneralizedTime getRevocationTime()
    {
        return revocationTime;
    }

    public CRLReason getRevocationReason()
    {
        return revocationReason;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * RevokedInfo ::= SEQUENCE {
     *      revocationTime              GeneralizedTime,
     *      revocationReason    [0]     EXPLICIT CRLReason OPTIONAL }
     * </pre>
     */
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector(2);

        v.add(revocationTime);
        if (revocationReason != null)
        {
            v.add(new DERTaggedObject(true, 0, revocationReason));
        }

        return new DERSequence(v);
    }
}
