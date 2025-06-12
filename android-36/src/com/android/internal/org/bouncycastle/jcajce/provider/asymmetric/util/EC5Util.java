/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.jcajce.provider.asymmetric.util;

import java.math.BigInteger;
import java.security.spec.ECField;
import java.security.spec.ECFieldF2m;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.android.internal.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.internal.org.bouncycastle.asn1.ASN1Sequence;
// import org.bouncycastle.asn1.cryptopro.ECGOST3410NamedCurves;
// import org.bouncycastle.asn1.cryptopro.GOST3410PublicKeyAlgParameters;
import com.android.internal.org.bouncycastle.asn1.x9.ECNamedCurveTable;
import com.android.internal.org.bouncycastle.asn1.x9.X962Parameters;
import com.android.internal.org.bouncycastle.asn1.x9.X9ECParameters;
import com.android.internal.org.bouncycastle.asn1.x9.X9ECParametersHolder;
import com.android.internal.org.bouncycastle.crypto.ec.CustomNamedCurves;
import com.android.internal.org.bouncycastle.crypto.params.ECDomainParameters;
import com.android.internal.org.bouncycastle.jcajce.provider.config.ProviderConfiguration;
// import org.bouncycastle.jce.ECGOST3410NamedCurveTable;
import com.android.internal.org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.android.internal.org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import com.android.internal.org.bouncycastle.jce.spec.ECNamedCurveSpec;
import com.android.internal.org.bouncycastle.math.ec.ECAlgorithms;
import com.android.internal.org.bouncycastle.math.ec.ECCurve;
import com.android.internal.org.bouncycastle.math.field.FiniteField;
import com.android.internal.org.bouncycastle.math.field.Polynomial;
import com.android.internal.org.bouncycastle.math.field.PolynomialExtensionField;
import com.android.internal.org.bouncycastle.util.Arrays;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class EC5Util
{
    private static class CustomCurves
    {
        private static Map CURVE_MAP = createCurveMap();

        private static Map createCurveMap()
        {
            Map map = new HashMap();

            Enumeration e = CustomNamedCurves.getNames();
            while (e.hasMoreElements())
            {
                String name = (String)e.nextElement();

                X9ECParametersHolder curveParams = ECNamedCurveTable.getByNameLazy(name);
                if (curveParams != null)  // there may not be a regular curve, may just be a custom curve.
                {
                    ECCurve curve = curveParams.getCurve();
                    if (ECAlgorithms.isFpCurve(curve))
                    {
                        map.put(curve, CustomNamedCurves.getByNameLazy(name).getCurve());
                    }
                }
            }

            ECCurve c_25519 = CustomNamedCurves.getByNameLazy("Curve25519").getCurve();

            map.put(new ECCurve.Fp(
                c_25519.getField().getCharacteristic(),
                c_25519.getA().toBigInteger(),
                c_25519.getB().toBigInteger(),
                c_25519.getOrder(),
                c_25519.getCofactor(),
                true), c_25519);

            return map;
        }

        // BEGIN Android-removed: Unsupported curves
        /*
        X9ECParameters x9_25519 = CustomNamedCurves.getByName("Curve25519");
        ECCurve c_25519 = x9_25519.getCurve();

        customCurves.put(new ECCurve.Fp(
            c_25519.getField().getCharacteristic(),
            c_25519.getA().toBigInteger(),
            c_25519.getB().toBigInteger(),
            c_25519.getOrder(),
            c_25519.getCofactor()
            ), c_25519);
        */
        // END Android-removed: Unsupported curves
        static ECCurve substitute(ECCurve c)
        {
            ECCurve custom = (ECCurve)CURVE_MAP.get(c);
            return null != custom ? custom : c;
        }
    }

    public static ECCurve getCurve(
        ProviderConfiguration configuration,
        X962Parameters params)
    {
        ECCurve curve;
        Set acceptableCurves = configuration.getAcceptableNamedCurves();

        if (params.isNamedCurve())
        {
            ASN1ObjectIdentifier oid = ASN1ObjectIdentifier.getInstance(params.getParameters());

            if (acceptableCurves.isEmpty() || acceptableCurves.contains(oid))
            {
                X9ECParameters ecP = ECUtil.getNamedCurveByOid(oid);

                if (ecP == null)
                {
                    ecP = (X9ECParameters)configuration.getAdditionalECParameters().get(oid);
                }

                curve = ecP.getCurve();
            }
            else
            {
                throw new IllegalStateException("named curve not acceptable");
            }
        }
        else if (params.isImplicitlyCA())
        {
            curve = configuration.getEcImplicitlyCa().getCurve();
        }
        else
        {
            ASN1Sequence pSeq = ASN1Sequence.getInstance(params.getParameters());
            if (acceptableCurves.isEmpty())
            {
                if (pSeq.size() > 3)
                {
                    X9ECParameters ecP = X9ECParameters.getInstance(pSeq);

                    curve = ecP.getCurve();
                }
                else    // GOST parameters
                {
                    // BEGIN Android-removed: unsupported algorithms
                    /*
                    ASN1ObjectIdentifier gostCurve = ASN1ObjectIdentifier.getInstance(pSeq.getObjectAt(0));

                    curve = ECGOST3410NamedCurves.getByOIDX9(gostCurve).getCurve();
                    */
                    // END Android-removed: unsupported algorithms
                    throw new IllegalStateException("GOST is not supported");
                }
            }
            else
            {
                throw new IllegalStateException("encoded parameters not acceptable");
            }
        }

        return curve;
    }

    public static ECDomainParameters getDomainParameters(
        ProviderConfiguration configuration,
        java.security.spec.ECParameterSpec params)
    {
        ECDomainParameters domainParameters;

        if (params == null)
        {
            com.android.internal.org.bouncycastle.jce.spec.ECParameterSpec iSpec = configuration.getEcImplicitlyCa();

            domainParameters = new ECDomainParameters(iSpec.getCurve(), iSpec.getG(), iSpec.getN(), iSpec.getH(), iSpec.getSeed());
        }
        else
        {
            domainParameters = ECUtil.getDomainParameters(configuration, convertSpec(params));
        }

        return domainParameters;
    }

    public static ECParameterSpec convertToSpec(
        X962Parameters params, ECCurve curve)
    {
        ECParameterSpec ecSpec;
        EllipticCurve ellipticCurve;

        if (params.isNamedCurve())
        {
            ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier)params.getParameters();
            X9ECParameters ecP = ECUtil.getNamedCurveByOid(oid);
            if (ecP == null)
            {
                Map additionalECParameters = BouncyCastleProvider.CONFIGURATION.getAdditionalECParameters();
                if (!additionalECParameters.isEmpty())
                {
                    ecP = (X9ECParameters)additionalECParameters.get(oid);
                }
            }

            ellipticCurve = EC5Util.convertCurve(curve, ecP.getSeed());

            ecSpec = new ECNamedCurveSpec(
                ECUtil.getCurveName(oid),
                ellipticCurve,
                convertPoint(ecP.getG()),
                ecP.getN(),
                ecP.getH());
        }
        else if (params.isImplicitlyCA())
        {
            ecSpec = null;
        }
        else
        {
            ASN1Sequence pSeq = ASN1Sequence.getInstance(params.getParameters());
            if (pSeq.size() > 3)
            {
                X9ECParameters ecP = X9ECParameters.getInstance(pSeq);

                ellipticCurve = EC5Util.convertCurve(curve, ecP.getSeed());

                if (ecP.getH() != null)
                {
                    ecSpec = new ECParameterSpec(
                        ellipticCurve,
                        convertPoint(ecP.getG()),
                        ecP.getN(),
                        ecP.getH().intValue());
                }
                else
                {
                    ecSpec = new ECParameterSpec(
                        ellipticCurve,
                        convertPoint(ecP.getG()),
                        ecP.getN(),
                        1);      // TODO: not strictly correct... need to fix the test data...
                }
            }
            else    // GOST parameters
            {
                // BEGIN Android-removed: unsupported algorithms
                /*
                GOST3410PublicKeyAlgParameters gostParams = GOST3410PublicKeyAlgParameters.getInstance(pSeq);

                ECNamedCurveParameterSpec spec = ECGOST3410NamedCurveTable.getParameterSpec(ECGOST3410NamedCurves.getName(
                    gostParams.getPublicKeyParamSet()));

                curve = spec.getCurve();
                ellipticCurve = EC5Util.convertCurve(curve, spec.getSeed());

                ecSpec = new ECNamedCurveSpec(
                    ECGOST3410NamedCurves.getName(gostParams.getPublicKeyParamSet()),
                    ellipticCurve,
                    EC5Util.convertPoint(spec.getG()),
                    spec.getN(), spec.getH());

                */
                // END Android-removed: unsupported algorithms
                ecSpec = null;
            }
        }

        return ecSpec;
    }

    public static ECParameterSpec convertToSpec(
        X9ECParameters domainParameters)
    {
        return new ECParameterSpec(
            convertCurve(domainParameters.getCurve(), null),  // JDK 1.5 has trouble with this if it's not null...
            EC5Util.convertPoint(domainParameters.getG()),
            domainParameters.getN(),
            domainParameters.getH().intValue());
    }

    public static ECParameterSpec convertToSpec(
        ECDomainParameters domainParameters)
    {
        return new ECParameterSpec(
            convertCurve(domainParameters.getCurve(), null),  // JDK 1.5 has trouble with this if it's not null...
            EC5Util.convertPoint(domainParameters.getG()),
            domainParameters.getN(),
            domainParameters.getH().intValue());
    }

    public static EllipticCurve convertCurve(
        ECCurve curve, 
        byte[]  seed)
    {
        ECField field = convertField(curve.getField());
        BigInteger a = curve.getA().toBigInteger(), b = curve.getB().toBigInteger();

        // TODO: the Sun EC implementation doesn't currently handle the seed properly
        // so at the moment it's set to null. Should probably look at making this configurable
        return new EllipticCurve(field, a, b, null);
    }

    public static ECCurve convertCurve(
        EllipticCurve ec)
    {
        ECField field = ec.getField();
        BigInteger a = ec.getA();
        BigInteger b = ec.getB();

        if (field instanceof ECFieldFp)
        {
            return CustomCurves.substitute(new ECCurve.Fp(((ECFieldFp)field).getP(), a, b, null, null));
        }
        else
        {
            ECFieldF2m fieldF2m = (ECFieldF2m)field;
            int m = fieldF2m.getM();
            int ks[] = ECUtil.convertMidTerms(fieldF2m.getMidTermsOfReductionPolynomial());
            return new ECCurve.F2m(m, ks[0], ks[1], ks[2], a, b, null, null);
        }
    }

    public static ECField convertField(FiniteField field)
    {
        if (ECAlgorithms.isFpField(field))
        {
            return new ECFieldFp(field.getCharacteristic());
        }
        else //if (ECAlgorithms.isF2mField(curveField))
        {
            Polynomial poly = ((PolynomialExtensionField)field).getMinimalPolynomial();
            int[] exponents = poly.getExponentsPresent();
            int[] ks = Arrays.reverseInPlace(Arrays.copyOfRange(exponents, 1, exponents.length - 1));
            return new ECFieldF2m(poly.getDegree(), ks);
        }
    }

    public static ECParameterSpec convertSpec(
        EllipticCurve ellipticCurve,
        com.android.internal.org.bouncycastle.jce.spec.ECParameterSpec spec)
    {
        ECPoint g = convertPoint(spec.getG());

        if (spec instanceof ECNamedCurveParameterSpec)
        {
            String name = ((ECNamedCurveParameterSpec)spec).getName();

            return new ECNamedCurveSpec(name, ellipticCurve, g, spec.getN(), spec.getH());
        }
        else
        {
            return new ECParameterSpec(ellipticCurve, g, spec.getN(), spec.getH().intValue());
        }
    }

    public static com.android.internal.org.bouncycastle.jce.spec.ECParameterSpec convertSpec(ECParameterSpec ecSpec)
    {
        ECCurve curve = convertCurve(ecSpec.getCurve());

        com.android.internal.org.bouncycastle.math.ec.ECPoint g = convertPoint(curve, ecSpec.getGenerator());
        BigInteger n = ecSpec.getOrder();
        BigInteger h = BigInteger.valueOf(ecSpec.getCofactor());
        byte[] seed = ecSpec.getCurve().getSeed();

        if (ecSpec instanceof ECNamedCurveSpec)
        {
            return new com.android.internal.org.bouncycastle.jce.spec.ECNamedCurveParameterSpec(((ECNamedCurveSpec)ecSpec).getName(), curve,
                g, n, h, seed);
        }
        else
        {
            return new com.android.internal.org.bouncycastle.jce.spec.ECParameterSpec(curve, g, n, h, seed);
        }
    }

    public static com.android.internal.org.bouncycastle.math.ec.ECPoint convertPoint(ECParameterSpec ecSpec, ECPoint point)
    {
        return convertPoint(convertCurve(ecSpec.getCurve()), point);
    }

    public static com.android.internal.org.bouncycastle.math.ec.ECPoint convertPoint(ECCurve curve, ECPoint point)
    {
        return curve.createPoint(point.getAffineX(), point.getAffineY());
    }

    public static ECPoint convertPoint(com.android.internal.org.bouncycastle.math.ec.ECPoint point)
    {
        point = point.normalize();

        return new ECPoint(
            point.getAffineXCoord().toBigInteger(),
            point.getAffineYCoord().toBigInteger());
    }
}
