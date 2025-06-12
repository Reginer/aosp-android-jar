/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.jcajce.spec;

import java.security.PrivateKey;
import java.security.spec.AlgorithmParameterSpec;

import com.android.internal.org.bouncycastle.util.Arrays;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class KEMExtractSpec
    implements AlgorithmParameterSpec
{
    private final PrivateKey privateKey;
    private final byte[] encapsulation;
    private final String keyAlgorithmName;
    private final int keySizeInBits;

    public KEMExtractSpec(PrivateKey privateKey, byte[] encapsulation, String keyAlgorithmName)
    {
        this(privateKey, encapsulation, keyAlgorithmName, 256);
    }

    public KEMExtractSpec(PrivateKey privateKey, byte[] encapsulation, String keyAlgorithmName, int keySizeInBits)
    {
        this.privateKey = privateKey;
        this.encapsulation = Arrays.clone(encapsulation);
        this.keyAlgorithmName = keyAlgorithmName;
        this.keySizeInBits = keySizeInBits;
    }

    public byte[] getEncapsulation()
    {
        return Arrays.clone(encapsulation);
    }

    public PrivateKey getPrivateKey()
    {
        return privateKey;
    }

    public String getKeyAlgorithmName()
    {
        return keyAlgorithmName;
    }

    public int getKeySize()
    {
        return keySizeInBits;
    }
}
