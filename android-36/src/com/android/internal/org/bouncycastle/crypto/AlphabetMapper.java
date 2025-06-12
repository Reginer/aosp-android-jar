/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.crypto;

/**
 * Base interface for mapping from an alphabet to a set of indexes
 * suitable for use with FPE.
 * @hide This class is not part of the Android public SDK API
 */
public interface AlphabetMapper
{
    /**
     * Return the number of characters in the alphabet.
     *
     * @return the radix for the alphabet.
     */
    int getRadix();

    /**
     * Return the passed in char[] as a byte array of indexes (indexes
     * can be more than 1 byte)
     *
     * @param input characters to be mapped.
     * @return an index array.
     */
    byte[] convertToIndexes(char[] input);

    /**
     * Return a char[] for this alphabet based on the indexes passed.
     *
     * @param input input array of indexes.
     * @return an array of char corresponding to the index values.
     */
    char[] convertToChars(byte[] input);
}
