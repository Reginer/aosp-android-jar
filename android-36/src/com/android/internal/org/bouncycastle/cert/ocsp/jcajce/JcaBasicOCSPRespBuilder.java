/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cert.ocsp.jcajce;

import java.security.PublicKey;

import javax.security.auth.x500.X500Principal;

import com.android.internal.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.internal.org.bouncycastle.cert.ocsp.BasicOCSPRespBuilder;
import com.android.internal.org.bouncycastle.cert.ocsp.OCSPException;
import com.android.internal.org.bouncycastle.operator.DigestCalculator;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class JcaBasicOCSPRespBuilder
    extends BasicOCSPRespBuilder
{
    public JcaBasicOCSPRespBuilder(X500Principal principal)
    {
        super(new JcaRespID(principal));
    }

    public JcaBasicOCSPRespBuilder(PublicKey key, DigestCalculator digCalc)
        throws OCSPException
    {
        super(SubjectPublicKeyInfo.getInstance(key.getEncoded()), digCalc);
    }
}
