/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.pkcs;

import com.android.internal.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.internal.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.internal.org.bouncycastle.asn1.DERSequence;
import com.android.internal.org.bouncycastle.asn1.DERSet;
import com.android.internal.org.bouncycastle.asn1.DERTaggedObject;
import com.android.internal.org.bouncycastle.asn1.pkcs.Attribute;
import com.android.internal.org.bouncycastle.asn1.x500.X500Name;
import com.android.internal.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.internal.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class DeltaCertificateRequestAttributeValueBuilder
{
    private final SubjectPublicKeyInfo subjectPublicKey;

    private AlgorithmIdentifier signatureAlgorithm;
    private X500Name subject;

    public DeltaCertificateRequestAttributeValueBuilder(SubjectPublicKeyInfo subjectPublicKey)
    {
        this.subjectPublicKey = subjectPublicKey;
    }

    public DeltaCertificateRequestAttributeValueBuilder setSignatureAlgorithm(AlgorithmIdentifier signatureAlgorithm)
    {
       this.signatureAlgorithm = signatureAlgorithm;

       return this;
    }

    public DeltaCertificateRequestAttributeValueBuilder setSubject(X500Name subject)
    {
       this.subject = subject;

       return this;
    }

    public DeltaCertificateRequestAttributeValue build()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        if (subject != null)
        {
            v.add(new DERTaggedObject(false, 0, subject));
        }
        v.add(subjectPublicKey);
        if (signatureAlgorithm != null)
        {
            v.add(new DERTaggedObject(false, 2, signatureAlgorithm));
        }

        
        return new DeltaCertificateRequestAttributeValue(new Attribute(new ASN1ObjectIdentifier("2.16.840.1.114027.80.6.2"),
                        new DERSet(new DERSequence(v))));
    }
}
