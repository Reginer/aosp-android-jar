/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.operator;

import java.io.IOException;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class OperatorStreamException
    extends IOException
{
    private Throwable cause;

    public OperatorStreamException(String msg, Throwable cause)
    {
        super(msg);

        this.cause = cause;
    }

    public Throwable getCause()
    {
        return cause; 
    }
}
