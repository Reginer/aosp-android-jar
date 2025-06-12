/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cms;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class CMSException
    extends Exception
{
    Exception   e;

    public CMSException(
        String msg)
    {
        super(msg);
    }

    public CMSException(
        String msg,
        Exception e)
    {
        super(msg);

        this.e = e;
    }

    public Exception getUnderlyingException()
    {
        return e;
    }
    
    public Throwable getCause()
    {
        return e;
    }
}
