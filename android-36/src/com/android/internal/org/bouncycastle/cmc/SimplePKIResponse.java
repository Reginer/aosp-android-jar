/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cmc;

import java.io.IOException;

import com.android.internal.org.bouncycastle.asn1.ASN1Primitive;
import com.android.internal.org.bouncycastle.asn1.cms.ContentInfo;
import com.android.internal.org.bouncycastle.cert.X509CRLHolder;
import com.android.internal.org.bouncycastle.cert.X509CertificateHolder;
import com.android.internal.org.bouncycastle.cms.CMSException;
import com.android.internal.org.bouncycastle.cms.CMSSignedData;
import com.android.internal.org.bouncycastle.util.Encodable;
import com.android.internal.org.bouncycastle.util.Store;

/**
 * Carrier for a Simple PKI Response.
 * <p>
 * A Simple PKI Response is defined in RFC 5272 as a CMS SignedData object with no EncapsulatedContentInfo
 * and no SignerInfos attached.
 * </p>
 * @hide This class is not part of the Android public SDK API
 */
public class SimplePKIResponse
    implements Encodable
{
    private final CMSSignedData certificateResponse;

    private static ContentInfo parseBytes(byte[] responseEncoding)
        throws CMCException
    {
        try
        {
            return ContentInfo.getInstance(ASN1Primitive.fromByteArray(responseEncoding));
        }
        catch (Exception e)
        {
            throw new CMCException("malformed data: " + e.getMessage(), e);
        }
    }

    /**
     * Create a SimplePKIResponse from the passed in bytes.
     *
     * @param responseEncoding BER/DER encoding of the certificate.
     * @throws CMCException in the event of corrupted data, or an incorrect structure.
     */
    public SimplePKIResponse(byte[] responseEncoding)
        throws CMCException
    {
        this(parseBytes(responseEncoding));
    }

    /**
     * Create a SimplePKIResponse from the passed in ASN.1 structure.
     *
     * @param signedData a ContentInfo containing a SignedData.
     */
    public SimplePKIResponse(ContentInfo signedData)
        throws CMCException
    {
        try
        {
            this.certificateResponse = new CMSSignedData(signedData);
        }
        catch (CMSException e)
        {
            throw new CMCException("malformed response: " + e.getMessage(), e);
        }

        if (certificateResponse.getSignerInfos().size() != 0)
        {
            throw new CMCException("malformed response: SignerInfo structures found");
        }
        if (certificateResponse.getSignedContent() != null)
        {
            throw new CMCException("malformed response: Signed Content found");
        }
    }

    /**
     * Return any X.509 certificate objects in this SimplePKIResponse structure as a Store of X509CertificateHolder objects.
     *
     * @return a Store of X509CertificateHolder objects.
     */
    public Store<X509CertificateHolder> getCertificates()
    {
        return certificateResponse.getCertificates();
    }

    /**
     * Return any X.509 CRL objects in this SimplePKIResponse structure as a Store of X509CRLHolder objects.
     *
     * @return a Store of X509CRLHolder objects.
     */
    public Store<X509CRLHolder> getCRLs()
    {
        return certificateResponse.getCRLs();
    }

    /**
     * return the ASN.1 encoded representation of this object.
     */
    public byte[] getEncoded()
        throws IOException
    {
        return certificateResponse.getEncoded();
    }
}
