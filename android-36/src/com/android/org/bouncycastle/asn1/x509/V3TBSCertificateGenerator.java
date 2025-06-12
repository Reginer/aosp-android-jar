/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.asn1.x509;

import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.ASN1UTCTime;
import com.android.org.bouncycastle.asn1.DERBitString;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.DERTaggedObject;
import com.android.org.bouncycastle.asn1.x500.X500Name;

/**
 * Generator for Version 3 TBSCertificateStructures.
 * <pre>
 * TBSCertificate ::= SEQUENCE {
 *      version          [ 0 ]  Version DEFAULT v1(0),
 *      serialNumber            CertificateSerialNumber,
 *      signature               AlgorithmIdentifier,
 *      issuer                  Name,
 *      validity                Validity,
 *      subject                 Name,
 *      subjectPublicKeyInfo    SubjectPublicKeyInfo,
 *      issuerUniqueID    [ 1 ] IMPLICIT UniqueIdentifier OPTIONAL,
 *      subjectUniqueID   [ 2 ] IMPLICIT UniqueIdentifier OPTIONAL,
 *      extensions        [ 3 ] Extensions OPTIONAL
 *      }
 * </pre>
 * @hide This class is not part of the Android public SDK API
 *
 */
public class V3TBSCertificateGenerator
{
    DERTaggedObject         version = new DERTaggedObject(true, 0, new ASN1Integer(2));

    ASN1Integer              serialNumber;
    AlgorithmIdentifier     signature;
    X500Name                issuer;
    Time                    startDate, endDate;
    X500Name                subject;
    SubjectPublicKeyInfo    subjectPublicKeyInfo;
    Extensions              extensions;

    private boolean altNamePresentAndCritical;
    private DERBitString issuerUniqueID;
    private DERBitString subjectUniqueID;

    @android.compat.annotation.UnsupportedAppUsage
    public V3TBSCertificateGenerator()
    {
    }

    @android.compat.annotation.UnsupportedAppUsage
    public void setSerialNumber(
        ASN1Integer  serialNumber)
    {
        this.serialNumber = serialNumber;
    }

    @android.compat.annotation.UnsupportedAppUsage
    public void setSignature(
        AlgorithmIdentifier    signature)
    {
        this.signature = signature;
    }

        /**
     * @deprecated use X500Name method
     */
    @android.compat.annotation.UnsupportedAppUsage
    public void setIssuer(
        X509Name    issuer)
    {
        this.issuer = X500Name.getInstance(issuer);
    }

    public void setIssuer(
        X500Name issuer)
    {
        this.issuer = issuer;
    }
    
    public void setStartDate(
        ASN1UTCTime startDate)
    {
        this.startDate = new Time(startDate);
    }

    @android.compat.annotation.UnsupportedAppUsage
    public void setStartDate(
        Time startDate)
    {
        this.startDate = startDate;
    }

    public void setEndDate(
        ASN1UTCTime endDate)
    {
        this.endDate = new Time(endDate);
    }

    @android.compat.annotation.UnsupportedAppUsage
    public void setEndDate(
        Time endDate)
    {
        this.endDate = endDate;
    }

        /**
     * @deprecated use X500Name method
     */
    @android.compat.annotation.UnsupportedAppUsage
    public void setSubject(
        X509Name    subject)
    {
        this.subject = X500Name.getInstance(subject.toASN1Primitive());
    }

    public void setSubject(
        X500Name subject)
    {
        this.subject = subject;
    }

    public void setIssuerUniqueID(
        DERBitString uniqueID)
    {
        this.issuerUniqueID = uniqueID;
    }

    public void setSubjectUniqueID(
        DERBitString uniqueID)
    {
        this.subjectUniqueID = uniqueID;
    }

    @android.compat.annotation.UnsupportedAppUsage
    public void setSubjectPublicKeyInfo(
        SubjectPublicKeyInfo    pubKeyInfo)
    {
        this.subjectPublicKeyInfo = pubKeyInfo;
    }

    /**
     * @deprecated use method taking Extensions
     * @param extensions
     */
    public void setExtensions(
        X509Extensions    extensions)
    {
        setExtensions(Extensions.getInstance(extensions));
    }

    public void setExtensions(
        Extensions    extensions)
    {
        this.extensions = extensions;
        if (extensions != null)
        {
            Extension altName = extensions.getExtension(Extension.subjectAlternativeName);

            if (altName != null && altName.isCritical())
            {
                altNamePresentAndCritical = true;
            }
        }
    }

    public ASN1Sequence generatePreTBSCertificate()
    {
        if (signature != null)
        {
            throw new IllegalStateException("signature field should not be set in PreTBSCertificate");
        }
        if ((serialNumber == null)
            || (issuer == null) || (startDate == null) || (endDate == null)
            || (subject == null && !altNamePresentAndCritical) || (subjectPublicKeyInfo == null))
        {
            throw new IllegalStateException("not all mandatory fields set in V3 TBScertificate generator");
        }

        return generateTBSStructure();
    }

    private ASN1Sequence generateTBSStructure()
    {
        ASN1EncodableVector v = new ASN1EncodableVector(10);

        v.add(version);
        v.add(serialNumber);

        if (signature != null)
        {
            v.add(signature);
        }
        
        v.add(issuer);

        //
        // before and after dates
        //
        {
            ASN1EncodableVector validity = new ASN1EncodableVector(2);
            validity.add(startDate);
            validity.add(endDate);

            v.add(new DERSequence(validity));
        }

        if (subject != null)
        {
            v.add(subject);
        }
        else
        {
            v.add(new DERSequence());
        }

        v.add(subjectPublicKeyInfo);

        if (issuerUniqueID != null)
        {
            v.add(new DERTaggedObject(false, 1, issuerUniqueID));
        }

        if (subjectUniqueID != null)
        {
            v.add(new DERTaggedObject(false, 2, subjectUniqueID));
        }

        if (extensions != null)
        {
            v.add(new DERTaggedObject(true, 3, extensions));
        }

        return new DERSequence(v);
    }

    @android.compat.annotation.UnsupportedAppUsage
    public TBSCertificate generateTBSCertificate()
    {
        if ((serialNumber == null) || (signature == null)
            || (issuer == null) || (startDate == null) || (endDate == null)
            || (subject == null && !altNamePresentAndCritical) || (subjectPublicKeyInfo == null))
        {
            throw new IllegalStateException("not all mandatory fields set in V3 TBScertificate generator");
        }

        return TBSCertificate.getInstance(generateTBSStructure());
    }
}
