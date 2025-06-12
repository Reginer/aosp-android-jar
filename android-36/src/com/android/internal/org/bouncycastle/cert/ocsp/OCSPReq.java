/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cert.ocsp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import com.android.internal.org.bouncycastle.asn1.ASN1Encoding;
import com.android.internal.org.bouncycastle.asn1.ASN1Exception;
import com.android.internal.org.bouncycastle.asn1.ASN1InputStream;
import com.android.internal.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.internal.org.bouncycastle.asn1.ASN1OutputStream;
import com.android.internal.org.bouncycastle.asn1.ASN1Sequence;
import com.android.internal.org.bouncycastle.asn1.ocsp.OCSPRequest;
import com.android.internal.org.bouncycastle.asn1.ocsp.Request;
import com.android.internal.org.bouncycastle.asn1.x509.Certificate;
import com.android.internal.org.bouncycastle.asn1.x509.Extension;
import com.android.internal.org.bouncycastle.asn1.x509.Extensions;
import com.android.internal.org.bouncycastle.asn1.x509.GeneralName;
import com.android.internal.org.bouncycastle.cert.CertIOException;
import com.android.internal.org.bouncycastle.cert.X509CertificateHolder;
import com.android.internal.org.bouncycastle.operator.ContentVerifier;
import com.android.internal.org.bouncycastle.operator.ContentVerifierProvider;

/**
 * <pre>
 * OCSPRequest     ::=     SEQUENCE {
 *       tbsRequest                  TBSRequest,
 *       optionalSignature   [0]     EXPLICIT Signature OPTIONAL }
 *
 *   TBSRequest      ::=     SEQUENCE {
 *       version             [0]     EXPLICIT Version DEFAULT v1,
 *       requestorName       [1]     EXPLICIT GeneralName OPTIONAL,
 *       requestList                 SEQUENCE OF Request,
 *       requestExtensions   [2]     EXPLICIT Extensions OPTIONAL }
 *
 *   Signature       ::=     SEQUENCE {
 *       signatureAlgorithm      AlgorithmIdentifier,
 *       signature               BIT STRING,
 *       certs               [0] EXPLICIT SEQUENCE OF Certificate OPTIONAL}
 *
 *   Version         ::=             INTEGER  {  v1(0) }
 *
 *   Request         ::=     SEQUENCE {
 *       reqCert                     CertID,
 *       singleRequestExtensions     [0] EXPLICIT Extensions OPTIONAL }
 *
 *   CertID          ::=     SEQUENCE {
 *       hashAlgorithm       AlgorithmIdentifier,
 *       issuerNameHash      OCTET STRING, -- Hash of Issuer's DN
 *       issuerKeyHash       OCTET STRING, -- Hash of Issuers public key
 *       serialNumber        CertificateSerialNumber }
 * </pre>
 * @hide This class is not part of the Android public SDK API
 */
public class OCSPReq
{
    private static final X509CertificateHolder[] EMPTY_CERTS = new X509CertificateHolder[0];

    private OCSPRequest    req;
    private Extensions extensions;

    public OCSPReq(
        OCSPRequest req)
    {
        this.req = req;
        this.extensions = req.getTbsRequest().getRequestExtensions();
    }
    
    public OCSPReq(
        byte[]          req)
        throws IOException
    {
        this(new ASN1InputStream(req));
    }

    private OCSPReq(
        ASN1InputStream aIn)
        throws IOException
    {
        try
        {
            this.req = OCSPRequest.getInstance(aIn.readObject());
            if (req == null)
            {
                throw new CertIOException("malformed request: no request data found");
            }
            this.extensions = req.getTbsRequest().getRequestExtensions();
        }
        catch (IllegalArgumentException e)
        {
            throw new CertIOException("malformed request: " + e.getMessage(), e);
        }
        catch (ClassCastException e)
        {
            throw new CertIOException("malformed request: " + e.getMessage(), e);
        }
        catch (ASN1Exception e)
        {
            throw new CertIOException("malformed request: " + e.getMessage(), e);
        }
    }

    public int getVersionNumber()
    {
        return req.getTbsRequest().getVersion().intValueExact() + 1;
    }

    public GeneralName getRequestorName()
    {
        return GeneralName.getInstance(req.getTbsRequest().getRequestorName());
    }

    public Req[] getRequestList()
    {
        ASN1Sequence    seq = req.getTbsRequest().getRequestList();
        Req[]           requests = new Req[seq.size()];

        for (int i = 0; i != requests.length; i++)
        {
            requests[i] = new Req(Request.getInstance(seq.getObjectAt(i)));
        }

        return requests;
    }

    public boolean hasExtensions()
    {
        return extensions != null;
    }

    public Extension getExtension(ASN1ObjectIdentifier oid)
    {
        if (extensions != null)
        {
            return extensions.getExtension(oid);
        }

        return null;
    }

    public List getExtensionOIDs()
    {
        return OCSPUtils.getExtensionOIDs(extensions);
    }

    public Set getCriticalExtensionOIDs()
    {
        return OCSPUtils.getCriticalExtensionOIDs(extensions);
    }

    public Set getNonCriticalExtensionOIDs()
    {
        return OCSPUtils.getNonCriticalExtensionOIDs(extensions);
    }

    /**
     * return the object identifier representing the signature algorithm
     */
    public ASN1ObjectIdentifier getSignatureAlgOID()
    {
        if (!this.isSigned())
        {
            return null;
        }

        return req.getOptionalSignature().getSignatureAlgorithm().getAlgorithm();
    }

    public byte[] getSignature()
    {
        if (!this.isSigned())
        {
            return null;
        }

        return req.getOptionalSignature().getSignature().getOctets();
    }

    public X509CertificateHolder[] getCerts()
    {
        //
        // load the certificates if we have any
        //
        if (req.getOptionalSignature() != null)
        {
            ASN1Sequence s = req.getOptionalSignature().getCerts();

            if (s != null)
            {
                X509CertificateHolder[] certs = new X509CertificateHolder[s.size()];

                for (int i = 0; i != certs.length; i++)
                {
                    certs[i] = new X509CertificateHolder(Certificate.getInstance(s.getObjectAt(i)));
                }

                return certs;
            }

            return EMPTY_CERTS;
        }
        else
        {
            return EMPTY_CERTS;
        }
    }
    
    /**
     * Return whether or not this request is signed.
     * 
     * @return true if signed false otherwise.
     */
    public boolean isSigned()
    {
        return req.getOptionalSignature() != null;
    }

    /**
     * verify the signature against the TBSRequest object we contain.
     */
    public boolean isSignatureValid(
        ContentVerifierProvider verifierProvider)
        throws OCSPException
    {
        if (!this.isSigned())
        {
            throw new OCSPException("attempt to verify signature on unsigned object");
        }

        try
        {
            ContentVerifier verifier = verifierProvider.get(req.getOptionalSignature().getSignatureAlgorithm());
            OutputStream sOut = verifier.getOutputStream();

            sOut.write(req.getTbsRequest().getEncoded(ASN1Encoding.DER));

            return verifier.verify(this.getSignature());
        }
        catch (Exception e)
        {
            throw new OCSPException("exception processing signature: " + e, e);
        }
    }

    /**
     * return the ASN.1 encoded representation of this object.
     */
    public byte[] getEncoded() throws IOException
    {
        return req.getEncoded();
    }
}
