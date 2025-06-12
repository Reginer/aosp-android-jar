/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.jcajce.provider.digest;

import java.security.DigestException;
import java.security.MessageDigest;

import com.android.org.bouncycastle.crypto.Digest;
// BEGIN Android-removed:
// import org.bouncycastle.crypto.Xof;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class BCMessageDigest
    extends MessageDigest
{
    protected Digest  digest;
    protected int     digestSize;

    protected BCMessageDigest(
        Digest digest)
    {
        super(digest.getAlgorithmName());

        this.digest = digest;
        this.digestSize = digest.getDigestSize();
    }

    // BEGIN Android-removed:
    /*
    protected BCMessageDigest(
        Xof digest, int outputSize)
    {
        super(digest.getAlgorithmName());

        this.digest = digest;
        this.digestSize = outputSize / 8;
    }
    */
    // END Android-removed:
    public void engineReset() 
    {
        digest.reset();
    }

    public void engineUpdate(
        byte    input) 
    {
        digest.update(input);
    }

    public void engineUpdate(
        byte[]  input,
        int     offset,
        int     len) 
    {
        digest.update(input, offset, len);
    }

    public int engineGetDigestLength()
    {
        return digestSize;
    }

    public byte[] engineDigest() 
    {
        byte[]  digestBytes = new byte[digestSize];

        digest.doFinal(digestBytes, 0);

        return digestBytes;
    }

    public int engineDigest(byte[] buf, int off, int len) throws DigestException
    {
        if (len < digestSize)
            throw new DigestException("partial digests not returned");
        if (buf.length - off < digestSize)
            throw new DigestException("insufficient space in the output buffer to store the digest");

        digest.doFinal(buf, off);

        return digestSize;
    }
}
