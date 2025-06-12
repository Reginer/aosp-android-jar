/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cert.ocsp;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class OCSPException
    extends Exception
{
    private Throwable   cause;

    public OCSPException(
        String name)
    {
        super(name);
    }

    public OCSPException(
        String name,
        Throwable cause)
    {
        super(name);

        this.cause = cause;
    }

    public Throwable getCause()
    {
        return cause;
    }
}
