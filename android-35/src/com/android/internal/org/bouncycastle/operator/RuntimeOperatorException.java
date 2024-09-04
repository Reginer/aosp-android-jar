/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.operator;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class RuntimeOperatorException
    extends RuntimeException
{
    private Throwable cause;

    public RuntimeOperatorException(String msg)
    {
        super(msg);
    }

    public RuntimeOperatorException(String msg, Throwable cause)
    {
        super(msg);

        this.cause = cause;
    }

    public Throwable getCause()
    {
        return cause;
    }
}
