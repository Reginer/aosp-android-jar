/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.jcajce.spec;

import java.security.spec.EncodedKeySpec;

/**
 * An encoded key spec that just wraps the minimal data for a public/private key representation.
 * @hide This class is not part of the Android public SDK API
 */
public class RawEncodedKeySpec
    extends EncodedKeySpec
{
    /**
     * Base constructor - just the minimal data.
     *
     * @param bytes the public/private key data.
     */
    public RawEncodedKeySpec(byte[] bytes)
    {
        super(bytes);
    }

    public String getFormat()
    {
        return "RAW";
    }
}
