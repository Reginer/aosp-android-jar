/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.util;

import java.io.IOException;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class Exceptions
{
    public static IllegalArgumentException illegalArgumentException(String message, Throwable cause)
    {
        return new IllegalArgumentException(message, cause);
    }

    public static IllegalStateException illegalStateException(String message, Throwable cause)
    {
        return new IllegalStateException(message, cause);
    }

    public static IOException ioException(String message, Throwable cause)
    {
        return new IOException(message, cause);
    }

}
