/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.jcajce.provider.asymmetric.ec;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.EllipticCurve;
import java.util.Enumeration;

import com.android.internal.org.bouncycastle.asn1.ASN1BitString;
import com.android.internal.org.bouncycastle.asn1.ASN1Encodable;
import com.android.internal.org.bouncycastle.asn1.ASN1Encoding;
import com.android.internal.org.bouncycastle.asn1.ASN1Integer;
import com.android.internal.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.internal.org.bouncycastle.asn1.ASN1Primitive;
import com.android.internal.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.android.internal.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.internal.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.internal.org.bouncycastle.asn1.x9.ECNamedCurveTable;
import com.android.internal.org.bouncycastle.asn1.x9.X962Parameters;
import com.android.internal.org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import com.android.internal.org.bouncycastle.crypto.params.ECDomainParameters;
import com.android.internal.org.bouncycastle.crypto.params.ECNamedDomainParameters;
import com.android.internal.org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import com.android.internal.org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import com.android.internal.org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import com.android.internal.org.bouncycastle.jcajce.provider.asymmetric.util.PKCS12BagAttributeCarrierImpl;
import com.android.internal.org.bouncycastle.jcajce.provider.config.ProviderConfiguration;
import com.android.internal.org.bouncycastle.jce.interfaces.ECPointEncoder;
import com.android.internal.org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import com.android.internal.org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.android.internal.org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import com.android.internal.org.bouncycastle.math.ec.ECCurve;
import com.android.internal.org.bouncycastle.util.Arrays;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class BCECPrivateKey
    implements ECPrivateKey, com.android.internal.org.bouncycastle.jce.interfaces.ECPrivateKey, PKCS12BagAttributeCarrier, ECPointEncoder
{
    static final long serialVersionUID = 994553197664784084L;

    private String algorithm = "EC";
    private boolean withCompression;

    private transient BigInteger d;
    private transient ECParameterSpec ecSpec;
    private transient ProviderConfiguration configuration;
    private transient ASN1BitString publicKey;
    private transient PrivateKeyInfo privateKeyInfo;
    private transient byte[] encoding;

    private transient ECPrivateKeyParameters baseKey;
    private transient PKCS12BagAttributeCarrierImpl attrCarrier = new PKCS12BagAttributeCarrierImpl();


    protected BCECPrivateKey()
    {
    }

    public BCECPrivateKey(
        ECPrivateKey key,
        ProviderConfiguration configuration)
    {
        this.d = key.getS();
        this.algorithm = key.getAlgorithm();
        this.ecSpec = key.getParams();
        this.configuration = configuration;
        this.baseKey = convertToBaseKey(this);
    }

    public BCECPrivateKey(
        String algorithm,
        com.android.internal.org.bouncycastle.jce.spec.ECPrivateKeySpec spec,
        ProviderConfiguration configuration)
    {
        this.algorithm = algorithm;
        this.d = spec.getD();

        if (spec.getParams() != null) // can be null if implicitlyCA
        {
            ECCurve curve = spec.getParams().getCurve();
            EllipticCurve ellipticCurve;

            ellipticCurve = EC5Util.convertCurve(curve, spec.getParams().getSeed());

            this.ecSpec = EC5Util.convertSpec(ellipticCurve, spec.getParams());
        }
        else
        {
            this.ecSpec = null;
        }

        this.configuration = configuration;
        this.baseKey = convertToBaseKey(this);
    }


    public BCECPrivateKey(
        String algorithm,
        ECPrivateKeySpec spec,
        ProviderConfiguration configuration)
    {
        this.algorithm = algorithm;
        this.d = spec.getS();
        this.ecSpec = spec.getParams();
        this.configuration = configuration;
        this.baseKey = convertToBaseKey(this);
    }

    public BCECPrivateKey(
        String algorithm,
        BCECPrivateKey key)
    {
        this.algorithm = algorithm;
        this.d = key.d;
        this.ecSpec = key.ecSpec;
        this.withCompression = key.withCompression;
        this.attrCarrier = key.attrCarrier;
        this.publicKey = key.publicKey;
        this.configuration = key.configuration;
        this.baseKey = key.baseKey;
    }

    public BCECPrivateKey(
        String algorithm,
        ECPrivateKeyParameters params,
        BCECPublicKey pubKey,
        ECParameterSpec spec,
        ProviderConfiguration configuration)
    {
        this.algorithm = algorithm;
        this.d = params.getD();
        this.configuration = configuration;
        this.baseKey = params;

        if (spec == null)
        {
            ECDomainParameters dp = params.getParameters();
            EllipticCurve ellipticCurve = EC5Util.convertCurve(dp.getCurve(), dp.getSeed());

            this.ecSpec = new ECParameterSpec(
                ellipticCurve,
                EC5Util.convertPoint(dp.getG()),
                dp.getN(),
                dp.getH().intValue());
        }
        else
        {
            this.ecSpec = spec;
        }

        this.publicKey = getPublicKeyDetails(pubKey);
    }

    public BCECPrivateKey(
        String algorithm,
        ECPrivateKeyParameters params,
        BCECPublicKey pubKey,
        com.android.internal.org.bouncycastle.jce.spec.ECParameterSpec spec,
        ProviderConfiguration configuration)
    {
        this.algorithm = algorithm;
        this.d = params.getD();
        this.configuration = configuration;
        this.baseKey = params;

        if (spec == null)
        {
            ECDomainParameters dp = params.getParameters();
            EllipticCurve ellipticCurve = EC5Util.convertCurve(dp.getCurve(), dp.getSeed());

            this.ecSpec = new ECParameterSpec(
                ellipticCurve,
                EC5Util.convertPoint(dp.getG()),
                dp.getN(),
                dp.getH().intValue());
        }
        else
        {
            EllipticCurve ellipticCurve = EC5Util.convertCurve(spec.getCurve(), spec.getSeed());

            this.ecSpec = EC5Util.convertSpec(ellipticCurve, spec);
        }

        try
        {
            this.publicKey = getPublicKeyDetails(pubKey);
        }
        catch (Exception e)
        {
            this.publicKey = null; // not all curves are encodable
        }
    }

    public BCECPrivateKey(
        String algorithm,
        ECPrivateKeyParameters params,
        ProviderConfiguration configuration)
    {
        this.algorithm = algorithm;
        this.d = params.getD();
        this.ecSpec = null;
        this.configuration = configuration;
        this.baseKey = params;
    }

    BCECPrivateKey(
        String algorithm,
        PrivateKeyInfo info,
        ProviderConfiguration configuration)
        throws IOException
    {
        this.algorithm = algorithm;
        this.configuration = configuration;
        populateFromPrivKeyInfo(info);
    }

    private void populateFromPrivKeyInfo(PrivateKeyInfo info)
        throws IOException
    {
        X962Parameters params = X962Parameters.getInstance(info.getPrivateKeyAlgorithm().getParameters());

        ECCurve curve = EC5Util.getCurve(configuration, params);
        ecSpec = EC5Util.convertToSpec(params, curve);

        ASN1Encodable privKey = info.parsePrivateKey();
        if (privKey instanceof ASN1Integer)
        {
            ASN1Integer derD = ASN1Integer.getInstance(privKey);

            this.d = derD.getValue();
        }
        else
        {
            com.android.internal.org.bouncycastle.asn1.sec.ECPrivateKey ec = com.android.internal.org.bouncycastle.asn1.sec.ECPrivateKey.getInstance(privKey);

            this.d = ec.getKey();
            this.publicKey = ec.getPublicKey();
        }
        this.baseKey = convertToBaseKey(this);
    }

    public String getAlgorithm()
    {
        return algorithm;
    }

    /**
     * return the encoding format we produce in getEncoded().
     *
     * @return the string "PKCS#8"
     */
    public String getFormat()
    {
        return "PKCS#8";
    }

    /**
     * Return a PKCS8 representation of the key. The sequence returned
     * represents a full PrivateKeyInfo object.
     *
     * @return a PKCS8 representation of the key.
     */
    public byte[] getEncoded()
    {
        if (encoding == null)
        {
            PrivateKeyInfo info = getPrivateKeyInfo();

            if (info == null)
            {
                return null;
            }

            try
            {
                encoding = info.getEncoded(ASN1Encoding.DER);
            }
            catch (IOException e)
            {
                return null;
            }
        }

        return Arrays.clone(encoding);
    }

    private PrivateKeyInfo getPrivateKeyInfo()
    {
        if (privateKeyInfo == null)
        {
            X962Parameters params = ECUtils.getDomainParametersFromName(ecSpec, withCompression);

            int orderBitLength;
            if (ecSpec == null)
            {
                orderBitLength = ECUtil.getOrderBitLength(configuration, null, this.getS());
            }
            else
            {
                orderBitLength = ECUtil.getOrderBitLength(configuration, ecSpec.getOrder(), this.getS());
            }

            com.android.internal.org.bouncycastle.asn1.sec.ECPrivateKey keyStructure;

            if (publicKey != null)
            {
                keyStructure = new com.android.internal.org.bouncycastle.asn1.sec.ECPrivateKey(orderBitLength, this.getS(), publicKey, params);
            }
            else
            {
                keyStructure = new com.android.internal.org.bouncycastle.asn1.sec.ECPrivateKey(orderBitLength, this.getS(), params);
            }

            try
            {
                privateKeyInfo = new PrivateKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, params), keyStructure);
            }
            catch (IOException e)
            {
                return null;
            }
        }

        return privateKeyInfo;
    }

    public ECPrivateKeyParameters engineGetKeyParameters()
    {
        return baseKey;
    }

    public ECParameterSpec getParams()
    {
        return ecSpec;
    }

    public com.android.internal.org.bouncycastle.jce.spec.ECParameterSpec getParameters()
    {
        if (ecSpec == null)
        {
            return null;
        }
        return EC5Util.convertSpec(ecSpec);
    }

    com.android.internal.org.bouncycastle.jce.spec.ECParameterSpec engineGetSpec()
    {
        if (ecSpec != null)
        {
            return EC5Util.convertSpec(ecSpec);
        }

        return configuration.getEcImplicitlyCa();
    }

    public BigInteger getS()
    {
        return d;
    }

    public BigInteger getD()
    {
        return d;
    }

    public void setBagAttribute(
        ASN1ObjectIdentifier oid,
        ASN1Encodable attribute)
    {
        attrCarrier.setBagAttribute(oid, attribute);
    }

    public ASN1Encodable getBagAttribute(
        ASN1ObjectIdentifier oid)
    {
        return attrCarrier.getBagAttribute(oid);
    }

    public Enumeration getBagAttributeKeys()
    {
        return attrCarrier.getBagAttributeKeys();
    }

    public void setPointFormat(String style)
    {
        withCompression = !("UNCOMPRESSED".equalsIgnoreCase(style));
    }

    public boolean equals(Object o)
    {
        if (o instanceof ECPrivateKey)
        {
            ECPrivateKey other = (ECPrivateKey)o;

            PrivateKeyInfo info = this.getPrivateKeyInfo();
            PrivateKeyInfo otherInfo = (other instanceof BCECPrivateKey) ? ((BCECPrivateKey)other).getPrivateKeyInfo() : PrivateKeyInfo.getInstance(other.getEncoded());

            if (info == null || otherInfo == null)
            {
                return false;
            }

            try
            {
                boolean algEquals = Arrays.constantTimeAreEqual(info.getPrivateKeyAlgorithm().getEncoded(), otherInfo.getPrivateKeyAlgorithm().getEncoded());
                boolean keyEquals = Arrays.constantTimeAreEqual(this.getS().toByteArray(), other.getS().toByteArray());

                return algEquals & keyEquals;
            }
            catch (IOException e)
            {
                return false;
            }
        }

        return false;
    }

    public int hashCode()
    {
        return getD().hashCode() ^ engineGetSpec().hashCode();
    }

    public String toString()
    {
        return ECUtil.privateKeyToString("EC", d, engineGetSpec());
    }

    private ASN1BitString getPublicKeyDetails(BCECPublicKey pub)
    {
        try
        {
            SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray(pub.getEncoded()));

            return info.getPublicKeyData();
        }
        catch (IOException e)
        {   // should never happen
            return null;
        }
    }

    private void readObject(
        ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        byte[] enc = (byte[])in.readObject();

        this.configuration = BouncyCastleProvider.CONFIGURATION;

        populateFromPrivKeyInfo(PrivateKeyInfo.getInstance(ASN1Primitive.fromByteArray(enc)));

        this.attrCarrier = new PKCS12BagAttributeCarrierImpl();
    }

    private void writeObject(
        ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        out.writeObject(this.getEncoded());
    }

    private static ECPrivateKeyParameters convertToBaseKey(BCECPrivateKey key)
    {
        com.android.internal.org.bouncycastle.jce.interfaces.ECPrivateKey k = (com.android.internal.org.bouncycastle.jce.interfaces.ECPrivateKey)key;
        com.android.internal.org.bouncycastle.jce.spec.ECParameterSpec s = k.getParameters();

        if (s == null)
        {
            s = BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa();
        }

        if (k.getParameters() instanceof ECNamedCurveParameterSpec)
        {
            String name = ((ECNamedCurveParameterSpec)k.getParameters()).getName();
            if (name != null)
            {
                return new ECPrivateKeyParameters(
                    k.getD(),
                    new ECNamedDomainParameters(ECNamedCurveTable.getOID(name),
                        s.getCurve(), s.getG(), s.getN(), s.getH(), s.getSeed()));
            }
        }

        return new ECPrivateKeyParameters(
                k.getD(),
                new ECDomainParameters(s.getCurve(), s.getG(), s.getN(), s.getH(), s.getSeed()));
    }
}
