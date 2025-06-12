/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.crypto.params;

import java.math.BigInteger;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class RSAPrivateCrtKeyParameters
    extends RSAKeyParameters
{
    private BigInteger  e;
    private BigInteger  p;
    private BigInteger  q;
    private BigInteger  dP;
    private BigInteger  dQ;
    private BigInteger  qInv;

    /**
     * 
     */
    public RSAPrivateCrtKeyParameters(
         BigInteger  modulus,
         BigInteger  publicExponent,
         BigInteger  privateExponent,
         BigInteger  p,
         BigInteger  q,
         BigInteger  dP,
         BigInteger  dQ,
         BigInteger  qInv)
     {
         this(modulus, publicExponent, privateExponent, p, q, dP, dQ, qInv, false);
     }

    public RSAPrivateCrtKeyParameters(
        BigInteger  modulus,
        BigInteger  publicExponent,
        BigInteger  privateExponent,
        BigInteger  p,
        BigInteger  q,
        BigInteger  dP,
        BigInteger  dQ,
        BigInteger  qInv,
        boolean     isInternal)
    {
        super(true, modulus, privateExponent, isInternal);

        this.e = publicExponent;
        this.p = p;
        this.q = q;
        this.dP = dP;
        this.dQ = dQ;
        this.qInv = qInv;
    }

    public BigInteger getPublicExponent()
    {
        return e;
    }

    public BigInteger getP()
    {
        return p;
    }

    public BigInteger getQ()
    {
        return q;
    }

    public BigInteger getDP()
    {
        return dP;
    }

    public BigInteger getDQ()
    {
        return dQ;
    }

    public BigInteger getQInv()
    {
        return qInv;
    }
}
