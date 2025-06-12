/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.asn1.x509;

import java.io.IOException;
import java.util.Enumeration;

import com.android.org.bouncycastle.asn1.ASN1BitString;
import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.org.bouncycastle.asn1.DERBitString;
import com.android.org.bouncycastle.asn1.DERSequence;

/**
 * The object that contains the public key stored in a certificate.
 * <p>
 * The getEncoded() method in the public keys in the JCE produces a DER
 * encoded one of these.
 * @hide This class is not part of the Android public SDK API
 */
public class SubjectPublicKeyInfo
    extends ASN1Object
{
    private AlgorithmIdentifier     algId;
    private ASN1BitString           keyData;

    public static SubjectPublicKeyInfo getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    @android.compat.annotation.UnsupportedAppUsage
    public static SubjectPublicKeyInfo getInstance(
        Object  obj)
    {
        if (obj instanceof SubjectPublicKeyInfo)
        {
            return (SubjectPublicKeyInfo)obj;
        }
        else if (obj != null)
        {
            return new SubjectPublicKeyInfo(ASN1Sequence.getInstance(obj));
        }

        return null;
    }

    public SubjectPublicKeyInfo(
        AlgorithmIdentifier algId,
        ASN1BitString publicKey)
    {
        this.keyData = publicKey;
        this.algId = algId;
    }

    public SubjectPublicKeyInfo(
        AlgorithmIdentifier algId,
        ASN1Encodable       publicKey)
        throws IOException
    {
        this.keyData = new DERBitString(publicKey);
        this.algId = algId;
    }

    public SubjectPublicKeyInfo(
        AlgorithmIdentifier algId,
        byte[]              publicKey)
    {
        this.keyData = new DERBitString(publicKey);
        this.algId = algId;
    }

    /**
     @deprecated use SubjectPublicKeyInfo.getInstance()
     */
    public SubjectPublicKeyInfo(
        ASN1Sequence  seq)
    {
        if (seq.size() != 2)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                    + seq.size());
        }

        Enumeration e = seq.getObjects();

        this.algId = AlgorithmIdentifier.getInstance(e.nextElement());
        this.keyData = ASN1BitString.getInstance(e.nextElement());
    }

    public AlgorithmIdentifier getAlgorithm()
    {
        return algId;
    }

    /**
     * @deprecated use getAlgorithm()
     * @return    alg ID.
     */
    public AlgorithmIdentifier getAlgorithmId()
    {
        return algId;
    }

    /**
     * for when the public key is an encoded object - if the bitstring
     * can't be decoded this routine throws an IOException.
     *
     * @exception IOException - if the bit string doesn't represent a DER
     * encoded object.
     * @return the public key as an ASN.1 primitive.
     */
    public ASN1Primitive parsePublicKey()
        throws IOException
    {
        return ASN1Primitive.fromByteArray(keyData.getOctets());
    }

    /**
     * for when the public key is an encoded object - if the bitstring
     * can't be decoded this routine throws an IOException.
     *
     * @exception IOException - if the bit string doesn't represent a DER
     * encoded object.
     * @deprecated use parsePublicKey
     * @return the public key as an ASN.1 primitive.
     */
    public ASN1Primitive getPublicKey()
        throws IOException
    {
        return ASN1Primitive.fromByteArray(keyData.getOctets());
    }

    /**
     * for when the public key is raw bits.
     *
     * @return the public key as the raw bit string...
     */
    public ASN1BitString getPublicKeyData()
    {
        return keyData;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * SubjectPublicKeyInfo ::= SEQUENCE {
     *                          algorithm AlgorithmIdentifier,
     *                          publicKey BIT STRING }
     * </pre>
     */
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector(2);

        v.add(algId);
        v.add(keyData);

        return new DERSequence(v);
    }
}
