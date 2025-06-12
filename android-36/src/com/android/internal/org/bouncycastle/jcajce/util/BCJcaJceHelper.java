/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.jcajce.util;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import com.android.internal.org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * A JCA/JCE helper that refers to the BC provider for all it's needs.
 * @hide This class is not part of the Android public SDK API
 */
public class BCJcaJceHelper
    extends ProviderJcaJceHelper
{
    private static volatile Provider bcProvider;

    private static synchronized Provider getBouncyCastleProvider()
    {
        final Provider system = Security.getProvider("BC");
        // Avoid using the old, deprecated system BC provider on Android.
        // See: https://android-developers.googleblog.com/2018/03/cryptography-changes-in-android-p.html
        if (system instanceof BouncyCastleProvider)
        {
            return system;
        }
        else if (bcProvider != null)
        {
            return bcProvider;
        }
        else
        {
            bcProvider = new BouncyCastleProvider();

            return bcProvider;
        }
    }

    public BCJcaJceHelper()
    {
        super(getBouncyCastleProvider());
    }

    // BEGIN Android-added: Look up algorithms in private provider if not found in main Provider.
    //
    // If code is using a BCJcajceHelper to ensure it gets its implementation from BC, then
    // also search in the privately provided algorithms if not found in the main set.
    //
    // If any error occurs while searching the private Provider, typically a
    // NoSuchAlgorithmException being thrown, then the original NoSuchAlgorithmException
    // from the BC Provider is thrown for consistency.
    @Override
    public Cipher createCipher(String algorithm)
        throws NoSuchAlgorithmException, NoSuchPaddingException {
        try {
            return super.createCipher(algorithm);
        } catch (NoSuchAlgorithmException originalException) {
            try {
                return Cipher.getInstance(algorithm, getPrivateProvider());
            } catch (Throwable throwable) {
                throw originalException;
            }
        }
    }

    @Override
    public SecretKeyFactory createSecretKeyFactory(String algorithm)
        throws NoSuchAlgorithmException {
        try {
            return super.createSecretKeyFactory(algorithm);
        } catch (NoSuchAlgorithmException originalException) {
            try {
                return SecretKeyFactory.getInstance(algorithm, getPrivateProvider());
            } catch (Throwable throwable) {
                throw originalException;
            }
        }
    }

    @Override
    public Mac createMac(String algorithm) throws NoSuchAlgorithmException {
        try {
            return super.createMac(algorithm);
        } catch (NoSuchAlgorithmException originalException) {
            try {
                return Mac.getInstance(algorithm, getPrivateProvider());
            } catch (Throwable throwable) {
                throw originalException;
            }
        }
    }

    private Provider getPrivateProvider() {
        if (provider instanceof BouncyCastleProvider) {
            return ((BouncyCastleProvider) provider).getPrivateProvider();
        }
        throw new IllegalStateException("Internal error in BCJcaJceHelper");
    }
    // END Android-added: Look up algorithms in private provider if not found in main Provider.
}
