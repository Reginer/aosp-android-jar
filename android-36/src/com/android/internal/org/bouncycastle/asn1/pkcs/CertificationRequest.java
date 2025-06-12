/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.asn1.pkcs;

import com.android.internal.org.bouncycastle.asn1.ASN1BitString;
import com.android.internal.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.internal.org.bouncycastle.asn1.ASN1Object;
import com.android.internal.org.bouncycastle.asn1.ASN1Primitive;
import com.android.internal.org.bouncycastle.asn1.ASN1Sequence;
import com.android.internal.org.bouncycastle.asn1.DERBitString;
import com.android.internal.org.bouncycastle.asn1.DERSequence;
import com.android.internal.org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * PKCS10 Certification request object.
 * <pre>
 * CertificationRequest ::= SEQUENCE {
 *   certificationRequestInfo  CertificationRequestInfo,
 *   signatureAlgorithm        AlgorithmIdentifier{{ SignatureAlgorithms }},
 *   signature                 BIT STRING
 * }
 * </pre>
 * @hide This class is not part of the Android public SDK API
 */
public class CertificationRequest
    extends ASN1Object
{
    protected CertificationRequestInfo reqInfo = null;
    protected AlgorithmIdentifier sigAlgId = null;
    protected ASN1BitString sigBits = null;

    public static CertificationRequest getInstance(Object o)
    {
        if (o instanceof CertificationRequest)
        {
            return (CertificationRequest)o;
        }

        if (o != null)
        {
            return new CertificationRequest(ASN1Sequence.getInstance(o));
        }

        return null;
    }

    protected CertificationRequest()
    {
    }

    public CertificationRequest(
        CertificationRequestInfo requestInfo,
        AlgorithmIdentifier     algorithm,
        ASN1BitString            signature)
    {
        this.reqInfo = requestInfo;
        this.sigAlgId = algorithm;
        this.sigBits = signature;
    }

    /**
     * @deprecated use getInstance()
     */
    public CertificationRequest(
        ASN1Sequence seq)
    {
        reqInfo = CertificationRequestInfo.getInstance(seq.getObjectAt(0));
        sigAlgId = AlgorithmIdentifier.getInstance(seq.getObjectAt(1));
        sigBits = (DERBitString)seq.getObjectAt(2);
    }

    public CertificationRequestInfo getCertificationRequestInfo()
    {
        return reqInfo;
    }

    public AlgorithmIdentifier getSignatureAlgorithm()
    {
        return sigAlgId;
    }

    public ASN1BitString getSignature()
    {
        return sigBits;
    }

    public ASN1Primitive toASN1Primitive()
    {
        // Construct the CertificateRequest
        ASN1EncodableVector v = new ASN1EncodableVector(3);

        v.add(reqInfo);
        v.add(sigAlgId);
        v.add(sigBits);

        return new DERSequence(v);
    }
}
