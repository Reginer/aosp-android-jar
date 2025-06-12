/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.math.ec.endo;

import java.math.BigInteger;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class GLVTypeBParameters
{
    protected final BigInteger beta, lambda;
    protected final ScalarSplitParameters splitParams;

    public GLVTypeBParameters(BigInteger beta, BigInteger lambda, ScalarSplitParameters splitParams)
    {
        this.beta = beta;
        this.lambda = lambda;
        this.splitParams = splitParams;
    }

    public BigInteger getBeta()
    {
        return beta;
    }

    public BigInteger getLambda()
    {
        return lambda;
    }

    public ScalarSplitParameters getSplitParams()
    {
        return splitParams;
    }
}
