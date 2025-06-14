/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.crypto.paddings;

import java.security.SecureRandom;

import com.android.org.bouncycastle.crypto.InvalidCipherTextException;

/**
 * A padder that adds NULL byte padding to a block.
 * @hide This class is not part of the Android public SDK API
 */
public class ZeroBytePadding
    implements BlockCipherPadding
{
    /**
     * Initialise the padder.
     *
     * @param random - a SecureRandom if available.
     */
    public void init(SecureRandom random)
        throws IllegalArgumentException
    {
        // nothing to do.
    }

    /**
     * Return the name of the algorithm the padder implements.
     *
     * @return the name of the algorithm the padder implements.
     */
    public String getPaddingName()
    {
        return "ZeroByte";
    }

    /**
     * add the pad bytes to the passed in block, returning the
     * number of bytes added.
     */
    public int addPadding(
        byte[]  in,
        int     inOff)
    {
        int added = (in.length - inOff);

        while (inOff < in.length)
        {
            in[inOff] = (byte) 0;
            inOff++;
        }

        return added;
    }

    /**
     * return the number of pad bytes present in the block.
     */
    public int padCount(byte[] in)
        throws InvalidCipherTextException
    {
        int count = 0, still00Mask = -1;
        int i = in.length;
        while (--i >= 0)
        {
            int next = in[i] & 0xFF;
            int match00Mask = ((next ^ 0x00) - 1) >> 31;
            still00Mask &= match00Mask;
            count -= still00Mask;
        }
        return count;
    }
}
