/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.asn1.ocsp;

import com.android.internal.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.internal.org.bouncycastle.asn1.ASN1GeneralizedTime;
import com.android.internal.org.bouncycastle.asn1.ASN1Integer;
import com.android.internal.org.bouncycastle.asn1.ASN1Object;
import com.android.internal.org.bouncycastle.asn1.ASN1Primitive;
import com.android.internal.org.bouncycastle.asn1.ASN1Sequence;
import com.android.internal.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.internal.org.bouncycastle.asn1.DERSequence;
import com.android.internal.org.bouncycastle.asn1.DERTaggedObject;
import com.android.internal.org.bouncycastle.asn1.x509.Extensions;
import com.android.internal.org.bouncycastle.asn1.x509.X509Extensions;

/**
 * OCSP RFC 2560, RFC 6960
 * <pre>
 * ResponseData ::= SEQUENCE {
 *     version              [0] EXPLICIT Version DEFAULT v1,
 *     responderID              ResponderID,
 *     producedAt               GeneralizedTime,
 *     responses                SEQUENCE OF SingleResponse,
 *     responseExtensions   [1] EXPLICIT Extensions OPTIONAL }
 * </pre>
 * @hide This class is not part of the Android public SDK API
 */
public class ResponseData
    extends ASN1Object
{
    private static final ASN1Integer V1 = new ASN1Integer(0);
    
    private boolean             versionPresent;
    
    private ASN1Integer          version;
    private ResponderID         responderID;
    private ASN1GeneralizedTime  producedAt;
    private ASN1Sequence        responses;
    private Extensions      responseExtensions;

    public ResponseData(
        ASN1Integer          version,
        ResponderID         responderID,
        ASN1GeneralizedTime  producedAt,
        ASN1Sequence        responses,
        Extensions      responseExtensions)
    {
        this.version = version;
        this.responderID = responderID;
        this.producedAt = producedAt;
        this.responses = responses;
        this.responseExtensions = responseExtensions;
    }

    /**
     * @deprecated use method taking Extensions
     * @param responderID
     * @param producedAt
     * @param responses
     * @param responseExtensions
     */
    public ResponseData(
        ResponderID         responderID,
        ASN1GeneralizedTime  producedAt,
        ASN1Sequence        responses,
        X509Extensions responseExtensions)
    {
        this(V1, responderID, ASN1GeneralizedTime.getInstance(producedAt), responses, Extensions.getInstance(responseExtensions));
    }

    public ResponseData(
        ResponderID         responderID,
        ASN1GeneralizedTime  producedAt,
        ASN1Sequence        responses,
        Extensions      responseExtensions)
    {
        this(V1, responderID, producedAt, responses, responseExtensions);
    }
    
    private ResponseData(
        ASN1Sequence    seq)
    {
        int index = 0;

        if (seq.getObjectAt(0) instanceof ASN1TaggedObject)
        {
            ASN1TaggedObject    o = (ASN1TaggedObject)seq.getObjectAt(0);

            if (o.getTagNo() == 0)
            {
                this.versionPresent = true;
                this.version = ASN1Integer.getInstance(
                                (ASN1TaggedObject)seq.getObjectAt(0), true);
                index++;
            }
            else
            {
                this.version = V1;
            }
        }
        else
        {
            this.version = V1;
        }

        this.responderID = ResponderID.getInstance(seq.getObjectAt(index++));
        this.producedAt = ASN1GeneralizedTime.getInstance(seq.getObjectAt(index++));
        this.responses = (ASN1Sequence)seq.getObjectAt(index++);

        if (seq.size() > index)
        {
            this.responseExtensions = Extensions.getInstance(
                                (ASN1TaggedObject)seq.getObjectAt(index), true);
        }
    }

    public static ResponseData getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static ResponseData getInstance(
        Object  obj)
    {
        if (obj instanceof ResponseData)
        {
            return (ResponseData)obj;
        }
        else if (obj != null)
        {
            return new ResponseData(ASN1Sequence.getInstance(obj));
        }

        return null;
    }

    public ASN1Integer getVersion()
    {
        return version;
    }

    public ResponderID getResponderID()
    {
        return responderID;
    }

    public ASN1GeneralizedTime getProducedAt()
    {
        return producedAt;
    }

    public ASN1Sequence getResponses()
    {
        return responses;
    }

    public Extensions getResponseExtensions()
    {
        return responseExtensions;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * ResponseData ::= SEQUENCE {
     *     version              [0] EXPLICIT Version DEFAULT v1,
     *     responderID              ResponderID,
     *     producedAt               GeneralizedTime,
     *     responses                SEQUENCE OF SingleResponse,
     *     responseExtensions   [1] EXPLICIT Extensions OPTIONAL }
     * </pre>
     */
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector(5);

        if (versionPresent || !version.equals(V1))
        {
            v.add(new DERTaggedObject(true, 0, version));
        }

        v.add(responderID);
        v.add(producedAt);
        v.add(responses);
        if (responseExtensions != null)
        {
            v.add(new DERTaggedObject(true, 1, responseExtensions));
        }

        return new DERSequence(v);
    }
}
