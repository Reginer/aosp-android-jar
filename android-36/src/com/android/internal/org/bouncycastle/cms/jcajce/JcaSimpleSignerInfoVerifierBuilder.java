/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cms.jcajce;

import java.security.Provider;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import com.android.internal.org.bouncycastle.cert.X509CertificateHolder;
import com.android.internal.org.bouncycastle.cms.DefaultCMSSignatureAlgorithmNameGenerator;
import com.android.internal.org.bouncycastle.cms.SignerInformationVerifier;
import com.android.internal.org.bouncycastle.operator.ContentVerifierProvider;
import com.android.internal.org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import com.android.internal.org.bouncycastle.operator.DigestCalculatorProvider;
import com.android.internal.org.bouncycastle.operator.OperatorCreationException;
import com.android.internal.org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import com.android.internal.org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class JcaSimpleSignerInfoVerifierBuilder
{
    private Helper helper = new Helper();

    public JcaSimpleSignerInfoVerifierBuilder setProvider(Provider provider)
    {
        this.helper = new ProviderHelper(provider);

        return this;
    }

    public JcaSimpleSignerInfoVerifierBuilder setProvider(String providerName)
    {
        this.helper = new NamedHelper(providerName);

        return this;
    }

    public SignerInformationVerifier build(X509CertificateHolder certHolder)
        throws OperatorCreationException, CertificateException
    {
        return new SignerInformationVerifier(new DefaultCMSSignatureAlgorithmNameGenerator(), new DefaultSignatureAlgorithmIdentifierFinder(), helper.createContentVerifierProvider(certHolder), helper.createDigestCalculatorProvider());
    }

    public SignerInformationVerifier build(X509Certificate certificate)
        throws OperatorCreationException
    {
        return new SignerInformationVerifier(new DefaultCMSSignatureAlgorithmNameGenerator(), new DefaultSignatureAlgorithmIdentifierFinder(), helper.createContentVerifierProvider(certificate), helper.createDigestCalculatorProvider());
    }

    public SignerInformationVerifier build(PublicKey pubKey)
        throws OperatorCreationException
    {
        return new SignerInformationVerifier(new DefaultCMSSignatureAlgorithmNameGenerator(), new DefaultSignatureAlgorithmIdentifierFinder(), helper.createContentVerifierProvider(pubKey), helper.createDigestCalculatorProvider());
    }

    private static class Helper
    {
        ContentVerifierProvider createContentVerifierProvider(PublicKey publicKey)
            throws OperatorCreationException
        {
            return new JcaContentVerifierProviderBuilder().build(publicKey);
        }

        ContentVerifierProvider createContentVerifierProvider(X509Certificate certificate)
            throws OperatorCreationException
        {
            return new JcaContentVerifierProviderBuilder().build(certificate);
        }

        ContentVerifierProvider createContentVerifierProvider(X509CertificateHolder certHolder)
            throws OperatorCreationException, CertificateException
        {
            return new JcaContentVerifierProviderBuilder().build(certHolder);
        }

        DigestCalculatorProvider createDigestCalculatorProvider()
            throws OperatorCreationException
        {
            return new JcaDigestCalculatorProviderBuilder().build();
        }
    }

    private static class NamedHelper
        extends Helper
    {
        private final String providerName;

        public NamedHelper(String providerName)
        {
            this.providerName = providerName;
        }

        ContentVerifierProvider createContentVerifierProvider(PublicKey publicKey)
            throws OperatorCreationException
        {
            return new JcaContentVerifierProviderBuilder().setProvider(providerName).build(publicKey);
        }

        ContentVerifierProvider createContentVerifierProvider(X509Certificate certificate)
            throws OperatorCreationException
        {
            return new JcaContentVerifierProviderBuilder().setProvider(providerName).build(certificate);
        }

        DigestCalculatorProvider createDigestCalculatorProvider()
            throws OperatorCreationException
        {
            return new JcaDigestCalculatorProviderBuilder().setProvider(providerName).build();
        }

        ContentVerifierProvider createContentVerifierProvider(X509CertificateHolder certHolder)
            throws OperatorCreationException, CertificateException
        {
            return new JcaContentVerifierProviderBuilder().setProvider(providerName).build(certHolder);
        }
    }

    private static class ProviderHelper
        extends Helper
    {
        private final Provider provider;

        public ProviderHelper(Provider provider)
        {
            this.provider = provider;
        }

        ContentVerifierProvider createContentVerifierProvider(PublicKey publicKey)
            throws OperatorCreationException
        {
            return new JcaContentVerifierProviderBuilder().setProvider(provider).build(publicKey);
        }

        ContentVerifierProvider createContentVerifierProvider(X509Certificate certificate)
            throws OperatorCreationException
        {
            return new JcaContentVerifierProviderBuilder().setProvider(provider).build(certificate);
        }

        DigestCalculatorProvider createDigestCalculatorProvider()
            throws OperatorCreationException
        {
            return new JcaDigestCalculatorProviderBuilder().setProvider(provider).build();
        }

        ContentVerifierProvider createContentVerifierProvider(X509CertificateHolder certHolder)
            throws OperatorCreationException, CertificateException
        {
            return new JcaContentVerifierProviderBuilder().setProvider(provider).build(certHolder);
        }
    }
}
