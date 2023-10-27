/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.operator;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class OperatorException
    extends Exception
{
    private Throwable cause;

    public OperatorException(String msg, Throwable cause)
    {
        super(msg);

        this.cause = cause;
    }

    public OperatorException(String msg)
    {
        super(msg);
    }

    public Throwable getCause()
    {
        return cause;
    }
}
