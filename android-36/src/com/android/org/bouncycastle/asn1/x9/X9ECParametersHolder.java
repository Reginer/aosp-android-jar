/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.asn1.x9;

import com.android.org.bouncycastle.math.ec.ECCurve;

/**
 * A holding class that allows for X9ECParameters to be lazily constructed.
 * @hide This class is not part of the Android public SDK API
 */
public abstract class X9ECParametersHolder
{
    private ECCurve curve;
    private X9ECParameters params;

    public synchronized ECCurve getCurve()
    {
        if (curve == null)
        {
            curve = createCurve();
        }

        return curve;
    }

    public synchronized X9ECParameters getParameters()
    {
        if (params == null)
        {
            params = createParameters();
        }

        return params;
    }

    protected ECCurve createCurve()
    {
        return createParameters().getCurve();
    }

    protected abstract X9ECParameters createParameters();
}
