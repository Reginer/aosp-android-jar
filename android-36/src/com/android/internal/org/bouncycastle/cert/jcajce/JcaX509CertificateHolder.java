/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cert.jcajce;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import com.android.internal.org.bouncycastle.asn1.x509.Certificate;
import com.android.internal.org.bouncycastle.cert.X509CertificateHolder;

/**
 * JCA helper class for converting an X509Certificate into a X509CertificateHolder object.
 * @hide This class is not part of the Android public SDK API
 */
public class JcaX509CertificateHolder
    extends X509CertificateHolder
{
    /**
     * Base constructor.
     *
     * @param cert certificate to be used a the source for the holder creation.
     * @throws CertificateEncodingException if there is a problem extracting the certificate information.
     */
    public JcaX509CertificateHolder(X509Certificate cert)
        throws CertificateEncodingException
    {
        super(Certificate.getInstance(cert.getEncoded()));
    }
}
