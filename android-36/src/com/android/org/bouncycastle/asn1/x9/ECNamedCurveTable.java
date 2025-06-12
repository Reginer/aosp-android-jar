/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.asn1.x9;

import java.util.Enumeration;
import java.util.Vector;

import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
// Android-removed: Unsupported curves
// import org.bouncycastle.asn1.anssi.ANSSINamedCurves;
// import org.bouncycastle.asn1.cryptopro.ECGOST3410NamedCurves;
// import org.bouncycastle.asn1.gm.GMNamedCurves;
// import org.bouncycastle.asn1.cryptlib.CryptlibObjectIdentifiers;
import com.android.org.bouncycastle.asn1.nist.NISTNamedCurves;
import com.android.org.bouncycastle.asn1.sec.SECNamedCurves;
// Android-removed: Unsupported curves
// import org.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves;
import com.android.org.bouncycastle.crypto.ec.CustomNamedCurves;

/**
 * A general class that reads all X9.62 style EC curve tables.
 * @hide This class is not part of the Android public SDK API
 */
public class ECNamedCurveTable
{
    /**
     * return a X9ECParameters object representing the passed in named
     * curve. The routine returns null if the curve is not present.
     *
     * @param name the name of the curve requested
     * @return an X9ECParameters object or null if the curve is not available.
     */
    public static X9ECParameters getByName(
        String name)
    {
        X9ECParameters ecP = X962NamedCurves.getByName(name);

        if (ecP == null)
        {
            ecP = SECNamedCurves.getByName(name);
        }

        if (ecP == null)
        {
            ecP = NISTNamedCurves.getByName(name);
        }

        // BEGIN Android-removed: Unsupported curves
        /*
        if (ecP == null)
        {
            ecP = TeleTrusTNamedCurves.getByName(name);
        }

        if (ecP == null)
        {
            ecP = ANSSINamedCurves.getByName(name);
        }

        if (ecP == null)
        {
            ecP = ECGOST3410NamedCurves.getByNameX9(name);
        }

        if (ecP == null)
        {
            ecP = GMNamedCurves.getByName(name);
        }
        */
        // END Android-removed: Unsupported curves

        return ecP;
    }

    public static X9ECParametersHolder getByNameLazy(String name)
    {
        X9ECParametersHolder holder = X962NamedCurves.getByNameLazy(name);

        if (null == holder)
        {
            holder = SECNamedCurves.getByNameLazy(name);
        }

        if (null == holder)
        {
            holder = NISTNamedCurves.getByNameLazy(name);
        }

        // BEGIN Android-removed: Unsupported curves
        /*
        if (null == holder)
        {
            holder = TeleTrusTNamedCurves.getByNameLazy(name);
        }

        if (null == holder)
        {
            holder = ANSSINamedCurves.getByNameLazy(name);
        }

        if (null == holder)
        {
            holder = ECGOST3410NamedCurves.getByNameLazy(name);
        }

        if (null == holder)
        {
            holder = GMNamedCurves.getByNameLazy(name);
        }
        */
        // END Android-removed: Unsupported curves

        return holder;
    }

    /**
     * return the object identifier signified by the passed in name. Null
     * if there is no object identifier associated with name.
     *
     * @return the object identifier associated with name, if present.
     */
    public static ASN1ObjectIdentifier getOID(
        String name)
    {
        ASN1ObjectIdentifier oid = X962NamedCurves.getOID(name);

        if (oid == null)
        {
            oid = SECNamedCurves.getOID(name);
        }

        if (oid == null)
        {
            oid = NISTNamedCurves.getOID(name);
        }

        // BEGIN Android-removed: Unsupported curves
        /*
        if (oid == null)
        {
            oid = TeleTrusTNamedCurves.getOID(name);
        }

        if (oid == null)
        {
            oid = ANSSINamedCurves.getOID(name);
        }

        if (oid == null)
        {
            oid = ECGOST3410NamedCurves.getOID(name);
        }

        if (oid == null)
        {
            oid = GMNamedCurves.getOID(name);
        }

        if (oid == null && name.equals("curve25519"))
        {
            oid = CryptlibObjectIdentifiers.curvey25519;
        }
        */
        // END Android-removed: Unsupported curves

        return oid;
    }

    /**
     * return a X9ECParameters object representing the passed in named
     * curve.
     *
     * @param oid the object id of the curve requested
     * @return a standard name for the curve.
     */
    public static String getName(
        ASN1ObjectIdentifier oid)
    {
        String name = X962NamedCurves.getName(oid);

        if (name == null)
        {
            name = SECNamedCurves.getName(oid);
        }

        if (name == null)
        {
            name = NISTNamedCurves.getName(oid);
        }

        // BEGIN Android-removed: Unsupported curves
        /*
        if (name == null)
        {
            name = TeleTrusTNamedCurves.getName(oid);
        }

        if (name == null)
        {
            name = ANSSINamedCurves.getName(oid);
        }

        if (name == null)
        {
            name = ECGOST3410NamedCurves.getName(oid);
        }

        if (name == null)
        {
            name = GMNamedCurves.getName(oid);
        }

        if (name == null)
        {
            name = CustomNamedCurves.getName(oid);
        }
        */
        // END Android-removed: Unsupported curves

        return name;
    }

    /**
     * return a X9ECParameters object representing the passed in named
     * curve.
     *
     * @param oid the object id of the curve requested
     * @return an X9ECParameters object or null if the curve is not available.
     */
    public static X9ECParameters getByOID(
        ASN1ObjectIdentifier oid)
    {
        X9ECParameters ecP = X962NamedCurves.getByOID(oid);

        if (ecP == null)
        {
            ecP = SECNamedCurves.getByOID(oid);
        }

        // NOTE: All the NIST curves are currently from SEC, so no point in redundant OID lookup

        // BEGIN Android-removed: Unsupported curves
        /*
        if (ecP == null)
        {
            ecP = TeleTrusTNamedCurves.getByOID(oid);
        }

        if (ecP == null)
        {
            ecP = ANSSINamedCurves.getByOID(oid);
        }

        if (ecP == null)
        {
            ecP = ECGOST3410NamedCurves.getByOIDX9(oid);
        }

        if (ecP == null)
        {
            ecP = GMNamedCurves.getByOID(oid);
        }
        */
        // END Android-removed: Unsupported curves

        return ecP;
    }

    public static X9ECParametersHolder getByOIDLazy(ASN1ObjectIdentifier oid)
    {
        X9ECParametersHolder holder = X962NamedCurves.getByOIDLazy(oid);

        if (null == holder)
        {
            holder = SECNamedCurves.getByOIDLazy(oid);
        }

        // NOTE: All the NIST curves are currently from SEC, so no point in redundant OID lookup

        // BEGIN Android-removed: Unsupported curves
        /*
        if (null == holder)
        {
            holder = TeleTrusTNamedCurves.getByOIDLazy(oid);
        }

        if (null == holder)
        {
            holder = ANSSINamedCurves.getByOIDLazy(oid);
        }

        if (null == holder)
        {
            holder = ECGOST3410NamedCurves.getByOIDLazy(oid);
        }

        if (null == holder)
        {
            holder = GMNamedCurves.getByOIDLazy(oid);
        }
        */
        // END Android-removed: Unsupported curves

        return holder;
    }

    /**
     * return an enumeration of the names of the available curves.
     *
     * @return an enumeration of the names of the available curves.
     */
    public static Enumeration getNames()
    {
        Vector v = new Vector();

        addEnumeration(v, X962NamedCurves.getNames());
        addEnumeration(v, SECNamedCurves.getNames());
        addEnumeration(v, NISTNamedCurves.getNames());
        // BEGIN Android-removed: Unsupported curves
        // addEnumeration(v, TeleTrusTNamedCurves.getNames());
        // addEnumeration(v, ANSSINamedCurves.getNames());
        // addEnumeration(v, ECGOST3410NamedCurves.getNames());
        // addEnumeration(v, GMNamedCurves.getNames());
        // END Android-removed: Unsupported curves

        return v.elements();
    }

    private static void addEnumeration(
        Vector v,
        Enumeration e)
    {
        while (e.hasMoreElements())
        {
            v.addElement(e.nextElement());
        }
    }
}
