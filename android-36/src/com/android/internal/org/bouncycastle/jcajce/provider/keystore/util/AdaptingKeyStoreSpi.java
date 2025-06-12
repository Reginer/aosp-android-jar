/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.jcajce.provider.keystore.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Enumeration;

import com.android.internal.org.bouncycastle.jcajce.provider.keystore.pkcs12.PKCS12KeyStoreSpi;
import com.android.internal.org.bouncycastle.jcajce.util.JcaJceHelper;
import com.android.internal.org.bouncycastle.util.Properties;

/**
 * Implements a certificate only JKS key store.
 * @hide This class is not part of the Android public SDK API
 */
public class AdaptingKeyStoreSpi
    extends KeyStoreSpi
{
    public static final String COMPAT_OVERRIDE = "keystore.type.compat";

    private final JKSKeyStoreSpi jksStore;
    private final KeyStoreSpi primaryStore;

    private KeyStoreSpi keyStoreSpi;

    public AdaptingKeyStoreSpi(JcaJceHelper helper, KeyStoreSpi primaryStore)
    {
        this.jksStore = new JKSKeyStoreSpi(helper);
        this.primaryStore = primaryStore;
        this.keyStoreSpi = primaryStore;
    }

    public boolean engineProbe(InputStream stream)
        throws IOException
    {
        if (keyStoreSpi instanceof PKCS12KeyStoreSpi)
        {
            return ((PKCS12KeyStoreSpi)keyStoreSpi).engineProbe(stream);
        }
        return false;
    }

    public Key engineGetKey(String alias, char[] password)
        throws NoSuchAlgorithmException, UnrecoverableKeyException
    {
        return keyStoreSpi.engineGetKey(alias, password);
    }

    public Certificate[] engineGetCertificateChain(String alias)
    {
        return keyStoreSpi.engineGetCertificateChain(alias);
    }

    public Certificate engineGetCertificate(String alias)
    {
        return keyStoreSpi.engineGetCertificate(alias);
    }

    public Date engineGetCreationDate(String alias)
    {
        return keyStoreSpi.engineGetCreationDate(alias);
    }

    public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain)
        throws KeyStoreException
    {
        keyStoreSpi.engineSetKeyEntry(alias, key, password, chain);
    }

    public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain)
        throws KeyStoreException
    {
        keyStoreSpi.engineSetKeyEntry(alias, key, chain);
    }

    public void engineSetCertificateEntry(String alias, Certificate cert)
        throws KeyStoreException
    {
        keyStoreSpi.engineSetCertificateEntry(alias, cert);
    }

    public void engineDeleteEntry(String alias)
        throws KeyStoreException
    {
        keyStoreSpi.engineDeleteEntry(alias);
    }

    public Enumeration<String> engineAliases()
    {
        return keyStoreSpi.engineAliases();
    }

    public boolean engineContainsAlias(String alias)
    {
        return keyStoreSpi.engineContainsAlias(alias);
    }

    public int engineSize()
    {
        return keyStoreSpi.engineSize();
    }

    public boolean engineIsKeyEntry(String alias)
    {
        return keyStoreSpi.engineIsKeyEntry(alias);
    }

    public boolean engineIsCertificateEntry(String alias)
    {
        return keyStoreSpi.engineIsCertificateEntry(alias);
    }

    public String engineGetCertificateAlias(Certificate cert)
    {
        return keyStoreSpi.engineGetCertificateAlias(cert);
    }

    public void engineStore(OutputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        keyStoreSpi.engineStore(stream, password);
    }

    public void engineStore(KeyStore.LoadStoreParameter parameter)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        keyStoreSpi.engineStore(parameter);
    }

    public void engineLoad(InputStream stream, char[] password)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        if (stream == null)
        {
            keyStoreSpi = primaryStore;
            keyStoreSpi.engineLoad(null, password);
        }
        else
        {
            // the FIPS BCFKS/JKS compatibility is explicit and doesn't use the override.
            if (Properties.isOverrideSet(COMPAT_OVERRIDE) || !(primaryStore instanceof PKCS12KeyStoreSpi))
            {
                if (!stream.markSupported())
                {
                    stream = new BufferedInputStream(stream);
                }

                stream.mark(8);
                if (jksStore.engineProbe(stream))
                {
                    keyStoreSpi = jksStore;
                }
                else
                {
                    keyStoreSpi = primaryStore;
                }

                stream.reset();
            }
            else
            {
                keyStoreSpi = primaryStore;
            }

            keyStoreSpi.engineLoad(stream, password);
        }
    }

    public void engineLoad(KeyStore.LoadStoreParameter parameter)
        throws IOException, NoSuchAlgorithmException, CertificateException
    {
        keyStoreSpi.engineLoad(parameter);
    }
}

