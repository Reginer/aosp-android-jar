/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.jcajce.provider.asymmetric.x509;

import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.util.Date;
import java.util.Enumeration;

import javax.security.auth.x500.X500Principal;

import com.android.org.bouncycastle.asn1.ASN1BitString;
import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1Encoding;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.x509.BasicConstraints;
import com.android.org.bouncycastle.asn1.x509.Extension;
import com.android.org.bouncycastle.asn1.x509.Extensions;
import com.android.org.bouncycastle.asn1.x509.GeneralName;
import com.android.org.bouncycastle.asn1.x509.KeyUsage;
// BEGIN Android-added: Unknown reason
import com.android.org.bouncycastle.asn1.x509.X509Name;
// END Android-added: Unknown reason
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.PKCS12BagAttributeCarrierImpl;
import com.android.org.bouncycastle.jcajce.util.JcaJceHelper;
import com.android.org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import com.android.org.bouncycastle.util.Arrays;

class X509CertificateObject
    extends X509CertificateImpl
    implements PKCS12BagAttributeCarrier
{
    private final Object                cacheLock = new Object();
    private X509CertificateInternal     internalCertificateValue;
    private X500Principal               issuerValue;
    private PublicKey                   publicKeyValue;
    private X500Principal               subjectValue;
    private long[]                      validityValues;

    private volatile boolean            hashValueSet;
    private volatile int                hashValue;

    private PKCS12BagAttributeCarrier   attrCarrier = new PKCS12BagAttributeCarrierImpl();

    X509CertificateObject(JcaJceHelper bcHelper, com.android.org.bouncycastle.asn1.x509.Certificate c)
        throws CertificateParsingException
    {
        super(bcHelper, c, createBasicConstraints(c), createKeyUsage(c), createSigAlgName(c), createSigAlgParams(c));
    }

    public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException
    {
        long checkTime = date.getTime();
        long[] validityValues = getValidityValues();

        if (checkTime > validityValues[1])  // for other VM compatibility
        {
            throw new CertificateExpiredException("certificate expired on " + c.getEndDate().getTime());
        }
        if (checkTime < validityValues[0])
        {
            throw new CertificateNotYetValidException("certificate not valid till " + c.getStartDate().getTime());
        }
    }

    public X500Principal getIssuerX500Principal()
    {
        synchronized (cacheLock)
        {
            if (null != issuerValue)
            {
                return issuerValue;
            }
        }

        X500Principal temp = super.getIssuerX500Principal();

        synchronized (cacheLock)
        {
            if (null == issuerValue)
            {
                issuerValue = temp;
            }

            return issuerValue;
        }
    }

    public PublicKey getPublicKey()
    {
        // Cache the public key to support repeated-use optimizations
        synchronized (cacheLock)
        {
            if (null != publicKeyValue)
            {
                return publicKeyValue;
            }
        }

        PublicKey temp = super.getPublicKey();
        if (null == temp)
        {
            return null;
        }

        synchronized (cacheLock)
        {
            if (null == publicKeyValue)
            {
                publicKeyValue = temp;
            }

            return publicKeyValue;
        }
    }

    public X500Principal getSubjectX500Principal()
    {
        synchronized (cacheLock)
        {
            if (null != subjectValue)
            {
                return subjectValue;
            }
        }

        X500Principal temp = super.getSubjectX500Principal();

        synchronized (cacheLock)
        {
            if (null == subjectValue)
            {
                subjectValue = temp;
            }

            return subjectValue;
        }
    }

    public long[] getValidityValues()
    {
        synchronized (cacheLock)
        {
            if (null != validityValues)
            {
                return validityValues;
            }
        }

        long[] temp = new long[]
        {
            super.getNotBefore().getTime(),
            super.getNotAfter().getTime()
        };

        synchronized (cacheLock)
        {
            if (null == validityValues)
            {
                validityValues = temp;
            }

            return validityValues;
        }
    }

    // Android-added: Cache the encoded certificate
    private byte[] encoded;
    public byte[] getEncoded()
        throws CertificateEncodingException
    {
        try
        {
            // BEGIN Android-changed: Cache the encoded certificate
            if (encoded == null) {
                encoded = c.getEncoded(ASN1Encoding.DER);
            }
            return encoded;
            // END Android-changed: Cache the encoded certificate
        }
        catch (IOException e)
        {
            throw new CertificateEncodingException(e.toString());
        }
    }

    public boolean equals(
        Object other)
    {
        if (other == this)
        {
            return true;
        }

        if (other instanceof X509CertificateObject)
        {
            X509CertificateObject otherBC = (X509CertificateObject)other;

            if (this.hashValueSet && otherBC.hashValueSet)
            {
                if (this.hashValue != otherBC.hashValue)
                {
                    return false;
                }
            }
            else if (null == internalCertificateValue || null == otherBC.internalCertificateValue)
            {
                ASN1BitString signature = c.getSignature();
                if (null != signature && !signature.equals(otherBC.c.getSignature()))
                {
                    return false;
                }
            }

            return getInternalCertificate().equals(otherBC.getInternalCertificate());
        }

        return getInternalCertificate().equals(other);
    }

    public int hashCode()
    {
        if (!hashValueSet)
        {
            hashValue = getInternalCertificate().hashCode();
            hashValueSet = true;
        }

        return hashValue;
    }

    /**
     * Returns the original hash code for Certificates pre-JDK 1.8.
     *
     * @return the pre-JDK 1.8 hashcode calculation.
     */
    public int originalHashCode()
    {
        try
        {
            int hashCode = 0;
            byte[] certData = getInternalCertificate().getEncoded();
            for (int i = 1; i < certData.length; i++)
            {
                 hashCode += certData[i] * i;
            }
            return hashCode;
        }
        catch (CertificateEncodingException e)
        {
            return 0;
        }
    }

    public void setBagAttribute(ASN1ObjectIdentifier oid, ASN1Encodable attribute)
    {
        attrCarrier.setBagAttribute(oid, attribute);
    }

    public ASN1Encodable getBagAttribute(ASN1ObjectIdentifier oid)
    {
        return attrCarrier.getBagAttribute(oid);
    }

    public Enumeration getBagAttributeKeys()
    {
        return attrCarrier.getBagAttributeKeys();
    }

    private X509CertificateInternal getInternalCertificate()
    {
        synchronized (cacheLock)
        {
            if (null != internalCertificateValue)
            {
                return internalCertificateValue;
            }
        }

        byte[] encoding = null;
        CertificateEncodingException exception = null;
        try
        {
            encoding = c.getEncoded(ASN1Encoding.DER);
        }
        catch (IOException e)
        {
            exception = new X509CertificateEncodingException(e);
        }

        X509CertificateInternal temp = new X509CertificateInternal(bcHelper, c, basicConstraints, keyUsage, sigAlgName,
            sigAlgParams, encoding, exception);

        synchronized (cacheLock)
        {
            if (null == internalCertificateValue)
            {
                internalCertificateValue = temp;
            }

            return internalCertificateValue;
        }
    }

    private static BasicConstraints createBasicConstraints(com.android.org.bouncycastle.asn1.x509.Certificate c)
        throws CertificateParsingException
    {
        try
        {
            byte[] extOctets = getExtensionOctets(c, "2.5.29.19");
            if (null == extOctets)
            {
                return null;
            }

            return BasicConstraints.getInstance(ASN1Primitive.fromByteArray(extOctets));
        }
        catch (Exception e)
        {
            throw new CertificateParsingException("cannot construct BasicConstraints: " + e);
        }
    }

    private static boolean[] createKeyUsage(com.android.org.bouncycastle.asn1.x509.Certificate c) throws CertificateParsingException
    {
        try
        {
            byte[] extOctets = getExtensionOctets(c, "2.5.29.15");
            if (null == extOctets)
            {
                return null;
            }

            ASN1BitString bits = ASN1BitString.getInstance(ASN1Primitive.fromByteArray(extOctets));

            byte[] bytes = bits.getBytes();
            int length = (bytes.length * 8) - bits.getPadBits();

            boolean[] keyUsage = new boolean[(length < 9) ? 9 : length];

            for (int i = 0; i != length; i++)
            {
                keyUsage[i] = (bytes[i / 8] & (0x80 >>> (i % 8))) != 0;
            }

            return keyUsage;
        }
        catch (Exception e)
        {
            throw new CertificateParsingException("cannot construct KeyUsage: " + e);
        }
    }

    private static String createSigAlgName(com.android.org.bouncycastle.asn1.x509.Certificate c) throws CertificateParsingException
    {
        try
        {
            return X509SignatureUtil.getSignatureName(c.getSignatureAlgorithm());
        }
        catch (Exception e)
        {
            throw new CertificateParsingException("cannot construct SigAlgName: " + e);
        }
    }

    private static byte[] createSigAlgParams(com.android.org.bouncycastle.asn1.x509.Certificate c) throws CertificateParsingException
    {
        try
        {
            ASN1Encodable parameters = c.getSignatureAlgorithm().getParameters();
            if (null == parameters)
            {
                return null;
            }

            return parameters.toASN1Primitive().getEncoded(ASN1Encoding.DER);
        }
        catch (Exception e)
        {
            throw new CertificateParsingException("cannot construct SigAlgParams: " + e);
        }
    }

    private static class X509CertificateEncodingException
        extends CertificateEncodingException
    {
        private final Throwable cause;

        X509CertificateEncodingException(Throwable cause)
        {
            this.cause = cause;
        }

        public Throwable getCause()
        {
            return cause;
        }
    }
}
