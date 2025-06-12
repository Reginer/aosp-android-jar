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
 * OCSP RFC 2560, RFC 6960
 * <pre>
 * OCSPResponse ::= SEQUENCE {
 *     responseStatus         OCSPResponseStatus,
 *     responseBytes          [0] EXPLICIT ResponseBytes OPTIONAL }
 * </pre>
 * @see OCSPResponseStatus
 * @see ResponseBytes
 * @hide This class is not part of the Android public SDK API
 */

public class OCSPResponse
    extends ASN1Object
{
    OCSPResponseStatus    responseStatus;
    ResponseBytes        responseBytes;

    public OCSPResponse(
        OCSPResponseStatus  responseStatus,
        ResponseBytes       responseBytes)
    {
        this.responseStatus = responseStatus;
        this.responseBytes = responseBytes;
    }

    private OCSPResponse(
        ASN1Sequence    seq)
    {
        responseStatus = OCSPResponseStatus.getInstance(seq.getObjectAt(0));

        if (seq.size() == 2)
        {
            responseBytes = ResponseBytes.getInstance(
                                (ASN1TaggedObject)seq.getObjectAt(1), true);
        }
    }

    public static OCSPResponse getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static OCSPResponse getInstance(
        Object  obj)
    {
        if (obj instanceof OCSPResponse)
        {
            return (OCSPResponse)obj;
        }
        else if (obj != null)
        {
            return new OCSPResponse(ASN1Sequence.getInstance(obj));
        }

        return null;
    }

    public OCSPResponseStatus getResponseStatus()
    {
        return responseStatus;
    }

    public ResponseBytes getResponseBytes()
    {
        return responseBytes;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * OCSPResponse ::= SEQUENCE {
     *     responseStatus         OCSPResponseStatus,
     *     responseBytes          [0] EXPLICIT ResponseBytes OPTIONAL }
     * </pre>
     */
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector(2);

        v.add(responseStatus);

        if (responseBytes != null)
        {
            v.add(new DERTaggedObject(true, 0, responseBytes));
        }

        return new DERSequence(v);
    }
}
