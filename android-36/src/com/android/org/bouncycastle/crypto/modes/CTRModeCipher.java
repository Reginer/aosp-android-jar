/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.crypto.modes;

import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.MultiBlockCipher;
import com.android.org.bouncycastle.crypto.SkippingStreamCipher;

/**
 * @hide This class is not part of the Android public SDK API
 */
public interface CTRModeCipher
    extends MultiBlockCipher, SkippingStreamCipher
{
    /**
     * return the underlying block cipher that we are wrapping.
     *
     * @return the underlying block cipher that we are wrapping.
     */
    BlockCipher getUnderlyingCipher();
}
