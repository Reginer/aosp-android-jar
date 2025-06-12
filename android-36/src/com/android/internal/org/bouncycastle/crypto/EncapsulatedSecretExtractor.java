/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.crypto;

/**
 * @hide This class is not part of the Android public SDK API
 */
public interface EncapsulatedSecretExtractor
{
    /**
     * Extract the secret based on the recipient private key.
     *
     * @param encapsulation the encapsulated secret.
     */
    byte[] extractSecret(byte[] encapsulation);

    /**
     * Return the length in bytes of the encapsulation.
     *
     * @return length in bytes of an encapsulation for this parameter set.
     */
    int getEncapsulationLength();
}
