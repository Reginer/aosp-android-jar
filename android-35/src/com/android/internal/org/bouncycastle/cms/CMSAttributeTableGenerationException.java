/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cms;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class CMSAttributeTableGenerationException
    extends CMSRuntimeException
{
    Exception   e;

    public CMSAttributeTableGenerationException(
        String name)
    {
        super(name);
    }

    public CMSAttributeTableGenerationException(
        String name,
        Exception e)
    {
        super(name);

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
