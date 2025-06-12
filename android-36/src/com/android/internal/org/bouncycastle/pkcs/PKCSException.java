/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.pkcs;

/**
 * General checked Exception thrown in the cert package and its sub-packages.
 * @hide This class is not part of the Android public SDK API
 */
public class PKCSException
    extends Exception
{
    private Throwable cause;

    public PKCSException(String msg, Throwable cause)
    {
        super(msg);

        this.cause = cause;
    }

    public PKCSException(String msg)
    {
        super(msg);
    }

    public Throwable getCause()
    {
        return cause;
    }
}
