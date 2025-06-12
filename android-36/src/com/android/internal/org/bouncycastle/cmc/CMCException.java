/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cmc;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class CMCException
    extends Exception
{
    private final Throwable cause;

    public CMCException(String msg)
    {
        this(msg, null);
    }

    public CMCException(String msg, Throwable cause)
    {
        super(msg);
        this.cause = cause;
    }

    public Throwable getCause()
    {
        return cause;
    }
}
