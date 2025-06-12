/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.crypto;

import java.security.SecureRandom;

/**
 * Source provider for SecureRandom implementations.
 * @hide This class is not part of the Android public SDK API
 */
public interface SecureRandomProvider
{
    /**
     * Return a SecureRandom instance.
     * @return a SecureRandom
     */
    SecureRandom get();
}
