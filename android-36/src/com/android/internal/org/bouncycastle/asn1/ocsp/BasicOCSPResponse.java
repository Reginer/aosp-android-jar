/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.asn1.ocsp;

import com.android.internal.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.internal.org.bouncycastle.asn1.ASN1Object;
import com.android.internal.org.bouncycastle.asn1.ASN1Primitive;
import com.android.internal.org.bouncycastle.asn1.ASN1Sequence;
import com.android.internal.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.internal.org.bouncycastle.asn1.DERBitString;
import com.android.internal.org.bouncycastle.asn1.DERSequence;
import com.android.internal.org.bouncycastle.asn1.DERTaggedObject;
import com.android.internal.org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * OCSP RFC 2560, RFC 6960
 * <pre>
 * BasicOCSPResponse       ::= SEQUENCE {
 *    tbsResponseData      ResponseData,
 *    signatureAlgorithm   AlgorithmIdentifier,
 *    signature            BIT STRING,
 *    certs                [0] EXPLICIT SEQUENCE OF Certificate OPTIONAL }
 * </pre>
 * @hide This class is not part of the Android public SDK API
 */
public class BasicOCSPResponse
    extends ASN1Object
{
    private ResponseData        tbsResponseData;
    private AlgorithmIdentifier signatureAlgorithm;
    private DERBitString        signature;
    private ASN1Sequence        certs;

    public BasicOCSPResponse(
        ResponseData        tbsResponseData,
        AlgorithmIdentifier signatureAlgorithm,
        DERBitString        signature,
        ASN1Sequence        certs)
    {
        this.tbsResponseData = tbsResponseData;
        this.signatureAlgorithm = signatureAlgorithm;
        this.signature = signature;
        this.certs = certs;
    }

    private BasicOCSPResponse(
        ASN1Sequence    seq)
    {
        this.tbsResponseData = ResponseData.getInstance(seq.getObjectAt(0));
        this.signatureAlgorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(1));
        this.signature = (DERBitString)seq.getObjectAt(2);

        if (seq.size() > 3)
        {
            this.certs = ASN1Sequence.getInstance((ASN1TaggedObject)seq.getObjectAt(3), true);
        }
    }

    public static BasicOCSPResponse getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static BasicOCSPResponse getInstance(
        Object  obj)
    {
        if (obj instanceof BasicOCSPResponse)
        {
            return (BasicOCSPResponse)obj;
        }
        else if (obj != null)
        {
            return new BasicOCSPResponse(ASN1Sequence.getInstance(obj));
        }

        return null;
    }

    public ResponseData getTbsResponseData()
    {
        return tbsResponseData;
    }

    public AlgorithmIdentifier getSignatureAlgorithm()
    {
        return signatureAlgorithm;
    }

    public DERBitString getSignature()
    {
        return signature;
    }

    public ASN1Sequence getCerts()
    {
        return certs;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * BasicOCSPResponse       ::= SEQUENCE {
     *      tbsResponseData      ResponseData,
     *      signatureAlgorithm   AlgorithmIdentifier,
     *      signature            BIT STRING,
     *      certs                [0] EXPLICIT SEQUENCE OF Certificate OPTIONAL }
     * </pre>
     */
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector(4);

        v.add(tbsResponseData);
        v.add(signatureAlgorithm);
        v.add(signature);
        if (certs != null)
        {
            v.add(new DERTaggedObject(true, 0, certs));
        }

        return new DERSequence(v);
    }
}
