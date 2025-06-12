/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.asn1.ocsp;

import com.android.internal.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.internal.org.bouncycastle.asn1.ASN1Object;
import com.android.internal.org.bouncycastle.asn1.ASN1Primitive;
import com.android.internal.org.bouncycastle.asn1.ASN1Sequence;
import com.android.internal.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.internal.org.bouncycastle.asn1.DERSequence;
import com.android.internal.org.bouncycastle.asn1.DERTaggedObject;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class OCSPRequest
    extends ASN1Object
{
    TBSRequest      tbsRequest;
    Signature       optionalSignature;

    public OCSPRequest(
        TBSRequest  tbsRequest,
        Signature   optionalSignature)
    {
        this.tbsRequest = tbsRequest;
        this.optionalSignature = optionalSignature;
    }

    private OCSPRequest(
        ASN1Sequence    seq)
    {
        tbsRequest = TBSRequest.getInstance(seq.getObjectAt(0));

        if (seq.size() == 2)
        {
            optionalSignature = Signature.getInstance(
                                (ASN1TaggedObject)seq.getObjectAt(1), true);
        }
    }
    
    public static OCSPRequest getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static OCSPRequest getInstance(
        Object  obj)
    {
        if (obj instanceof OCSPRequest)
        {
            return (OCSPRequest)obj;
        }
        else if (obj != null)
        {
            return new OCSPRequest(ASN1Sequence.getInstance(obj));
        }

        return null;
    }
    
    public TBSRequest getTbsRequest()
    {
        return tbsRequest;
    }

    public Signature getOptionalSignature()
    {
        return optionalSignature;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * OCSPRequest     ::=     SEQUENCE {
     *     tbsRequest                  TBSRequest,
     *     optionalSignature   [0]     EXPLICIT Signature OPTIONAL }
     * </pre>
     */
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector(2);

        v.add(tbsRequest);

        if (optionalSignature != null)
        {
            v.add(new DERTaggedObject(true, 0, optionalSignature));
        }

        return new DERSequence(v);
    }
}
