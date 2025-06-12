/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.its.asn1;

import com.android.org.bouncycastle.asn1.ASN1Enumerated;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1Primitive;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class SymmAlgorithm
    extends ASN1Object
{
    public static SymmAlgorithm aes128Ccm = new SymmAlgorithm(new ASN1Enumerated(0));
    private ASN1Enumerated symmAlgorithm;

    private SymmAlgorithm(ASN1Enumerated symmAlgorithm)
    {
        this.symmAlgorithm = symmAlgorithm;
    }

    public SymmAlgorithm(int ordinal)
    {
        this.symmAlgorithm = new ASN1Enumerated(ordinal);
    }

    public SymmAlgorithm getInstance(Object src)
    {
        if (src == null)
        {
            return null;
        }
        else if (src instanceof SymmAlgorithm)
        {
            return (SymmAlgorithm)src;
        }
        else
        {
            return new SymmAlgorithm(ASN1Enumerated.getInstance(src));
        }
    }

    public ASN1Enumerated getSymmAlgorithm()
    {
        return symmAlgorithm;
    }

    public void setSymmAlgorithm(ASN1Enumerated symmAlgorithm)
    {
        this.symmAlgorithm = symmAlgorithm;
    }

    public ASN1Primitive toASN1Primitive()
    {
        return symmAlgorithm.toASN1Primitive();
    }
}
