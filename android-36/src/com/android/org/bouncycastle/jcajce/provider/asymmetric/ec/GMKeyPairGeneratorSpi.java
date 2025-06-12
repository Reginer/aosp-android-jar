/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.jcajce.provider.asymmetric.ec;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.util.Hashtable;

import com.android.org.bouncycastle.asn1.x9.X9ECParameters;
import com.android.org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import com.android.org.bouncycastle.crypto.CryptoServicesRegistrar;
import com.android.org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import com.android.org.bouncycastle.crypto.params.ECDomainParameters;
import com.android.org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import com.android.org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import com.android.org.bouncycastle.crypto.params.ECPublicKeyParameters;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import com.android.org.bouncycastle.jcajce.provider.config.ProviderConfiguration;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.android.org.bouncycastle.jce.spec.ECNamedCurveGenParameterSpec;
import com.android.org.bouncycastle.jce.spec.ECNamedCurveSpec;
import com.android.org.bouncycastle.jce.spec.ECParameterSpec;
import com.android.org.bouncycastle.math.ec.ECCurve;
import com.android.org.bouncycastle.math.ec.ECPoint;
import com.android.org.bouncycastle.util.Integers;

/**
 * @hide This class is not part of the Android public SDK API
 */
public abstract class GMKeyPairGeneratorSpi
    extends java.security.KeyPairGenerator
{
    public GMKeyPairGeneratorSpi(String algorithmName)
    {
        super(algorithmName);
    }

    /**
     * @hide This class is not part of the Android public SDK API
     */
    public static class BaseSM2
        extends GMKeyPairGeneratorSpi
    {
        ECKeyGenerationParameters   param;
        ECKeyPairGenerator          engine = new ECKeyPairGenerator();
        Object                      ecParams = null;
        int                         strength = 239;
        SecureRandom                random = CryptoServicesRegistrar.getSecureRandom();
        boolean                     initialised = false;
        String                      algorithm;
        ProviderConfiguration       configuration;

        static private Hashtable    ecParameters;

        static
        {
            ecParameters = new Hashtable();

            ecParameters.put(Integers.valueOf(192), new ECNamedCurveGenParameterSpec("prime192v1")); // a.k.a P-192
            ecParameters.put(Integers.valueOf(239), new ECNamedCurveGenParameterSpec("prime239v1"));
            ecParameters.put(Integers.valueOf(256), new ECNamedCurveGenParameterSpec("prime256v1")); // a.k.a P-256

            ecParameters.put(Integers.valueOf(224), new ECNamedCurveGenParameterSpec("P-224"));
            ecParameters.put(Integers.valueOf(384), new ECNamedCurveGenParameterSpec("P-384"));
            ecParameters.put(Integers.valueOf(521), new ECNamedCurveGenParameterSpec("P-521"));
        }

        public BaseSM2()
        {
            super("EC");
            this.algorithm = "EC";
            this.configuration = BouncyCastleProvider.CONFIGURATION;
        }

        public BaseSM2(
            String  algorithm,
            ProviderConfiguration configuration)
        {
            super(algorithm);
            this.algorithm = algorithm;
            this.configuration = configuration;
        }

        public void initialize(
            int             strength,
            SecureRandom    random)
        {
            this.strength = strength;
            this.random = random;

            ECNamedCurveGenParameterSpec ecParams = (ECNamedCurveGenParameterSpec)ecParameters.get(Integers.valueOf(strength));
            if (ecParams == null)
            {
                throw new InvalidParameterException("unknown key size.");
            }

            try
            {
                initialize(ecParams, random);
            }
            catch (InvalidAlgorithmParameterException e)
            {
                throw new InvalidParameterException("key size not configurable.");
            }
        }

        public void initialize(
            AlgorithmParameterSpec  params,
            SecureRandom            random)
            throws InvalidAlgorithmParameterException
        {
            if (params == null)
            {
                ECParameterSpec implicitCA = configuration.getEcImplicitlyCa();
                if (implicitCA == null)
                {
                    throw new InvalidAlgorithmParameterException("null parameter passed but no implicitCA set");
                }

                this.ecParams = null;
                this.param = createKeyGenParamsBC(implicitCA, random);
            }
            else if (params instanceof ECParameterSpec)
            {
                this.ecParams = params;
                this.param = createKeyGenParamsBC((ECParameterSpec)params, random);
            }
            else if (params instanceof java.security.spec.ECParameterSpec)
            {
                this.ecParams = params;
                this.param = createKeyGenParamsJCE((java.security.spec.ECParameterSpec)params, random);
            }
            else if (params instanceof ECGenParameterSpec)
            {
                initializeNamedCurve(((ECGenParameterSpec)params).getName(), random);
            }
            else if (params instanceof ECNamedCurveGenParameterSpec)
            {
                initializeNamedCurve(((ECNamedCurveGenParameterSpec)params).getName(), random);
            }
            else
            {
                String name = ECUtil.getNameFrom(params);

                if (name != null)
                {
                    initializeNamedCurve(name, random);
                }
                else
                {
                    throw new InvalidAlgorithmParameterException("invalid parameterSpec: " + params);
                }
            }

            engine.init(param);
            initialised = true;
        }

        public KeyPair generateKeyPair()
        {
            if (!initialised)
            {
                initialize(strength, new SecureRandom());
            }

            AsymmetricCipherKeyPair     pair = engine.generateKeyPair();
            ECPublicKeyParameters       pub = (ECPublicKeyParameters)pair.getPublic();
            ECPrivateKeyParameters      priv = (ECPrivateKeyParameters)pair.getPrivate();

            if (ecParams instanceof ECParameterSpec)
            {
                ECParameterSpec p = (ECParameterSpec)ecParams;

                BCECPublicKey pubKey = new BCECPublicKey(algorithm, pub, p, configuration);
                return new KeyPair(pubKey,
                                   new BCECPrivateKey(algorithm, priv, pubKey, p, configuration));
            }
            else if (ecParams == null)
            {
               return new KeyPair(new BCECPublicKey(algorithm, pub, configuration),
                                   new BCECPrivateKey(algorithm, priv, configuration));
            }
            else
            {
                java.security.spec.ECParameterSpec p = (java.security.spec.ECParameterSpec)ecParams;

                BCECPublicKey pubKey = new BCECPublicKey(algorithm, pub, p, configuration);
                
                return new KeyPair(pubKey, new BCECPrivateKey(algorithm, priv, pubKey, p, configuration));
            }
        }

        protected ECKeyGenerationParameters createKeyGenParamsBC(ECParameterSpec p, SecureRandom r)
        {
            return new ECKeyGenerationParameters(new ECDomainParameters(p.getCurve(), p.getG(), p.getN(), p.getH()), r);
        }

        protected ECKeyGenerationParameters createKeyGenParamsJCE(java.security.spec.ECParameterSpec p, SecureRandom r)
        {
            if (p instanceof ECNamedCurveSpec)
            {
                String curveName = ((ECNamedCurveSpec)p).getName();

                X9ECParameters x9 = ECUtils.getDomainParametersFromName(curveName, configuration);
                if (null != x9)
                {
                    return createKeyGenParamsJCE(x9, r);
                }
            }

            ECCurve curve = EC5Util.convertCurve(p.getCurve());
            ECPoint g = EC5Util.convertPoint(curve, p.getGenerator());
            BigInteger n = p.getOrder();
            BigInteger h = BigInteger.valueOf(p.getCofactor());
            ECDomainParameters dp = new ECDomainParameters(curve, g, n, h);
            return new ECKeyGenerationParameters(dp, r);
        }

        protected ECKeyGenerationParameters createKeyGenParamsJCE(X9ECParameters x9, SecureRandom r)
        {
            ECDomainParameters dp = new ECDomainParameters(x9.getCurve(), x9.getG(), x9.getN(), x9.getH());

            return new ECKeyGenerationParameters(dp, r);
        }

        protected void initializeNamedCurve(String curveName, SecureRandom random)
            throws InvalidAlgorithmParameterException
        {
            X9ECParameters x9 = ECUtils.getDomainParametersFromName(curveName, configuration);
            if (null == x9)
            {
                throw new InvalidAlgorithmParameterException("unknown curve name: " + curveName);
            }

            // Work-around for JDK bug -- it won't look up named curves properly if seed is present
            byte[] seed = null; //p.getSeed();

            this.ecParams = new ECNamedCurveSpec(curveName, x9.getCurve(), x9.getG(), x9.getN(), x9.getH(), seed);
            this.param = createKeyGenParamsJCE(x9, random);
        }
    }

    /**
     * @hide This class is not part of the Android public SDK API
     */
    public static class SM2
        extends BaseSM2
    {
        public SM2()
        {
            super("SM2", BouncyCastleProvider.CONFIGURATION);
        }
    }
}