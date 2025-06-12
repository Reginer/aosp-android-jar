/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.crypto.engines;

/**
 * an implementation of the AES Key Wrapper from the NIST Key Wrap
 * Specification.
 * <p>
 * For further details see: <a href="https://csrc.nist.gov/encryption/kms/key-wrap.pdf">https://csrc.nist.gov/encryption/kms/key-wrap.pdf</a>.
 * @hide This class is not part of the Android public SDK API
 */
public class AESWrapEngine
    extends RFC3394WrapEngine
{
    /**
     * Create a regular AESWrapEngine specifying the encrypt for wrapping, decrypt for unwrapping.
     */
    public AESWrapEngine()
    {
        super(AESEngine.newInstance());
    }

    /**
     * Create an AESWrapEngine where the underlying cipher is (optionally) set to decrypt for wrapping, encrypt for unwrapping.
     *
     * @param useReverseDirection true if underlying cipher should be used in decryption mode, false otherwise.
     */
    public AESWrapEngine(boolean useReverseDirection)
    {
        super(AESEngine.newInstance(), useReverseDirection);
    }
}
