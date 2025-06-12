/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.crypto.engines;

import com.android.org.bouncycastle.crypto.CryptoServicePurpose;

class Utils
{
    static CryptoServicePurpose getPurpose(boolean forEncryption)
    {
        return forEncryption ? CryptoServicePurpose.ENCRYPTION : CryptoServicePurpose.DECRYPTION;
    }
}
