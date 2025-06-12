/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.math.ec.endo;

import java.math.BigInteger;

import com.android.internal.org.bouncycastle.math.ec.ECConstants;
import com.android.internal.org.bouncycastle.math.ec.ECCurve;
import com.android.internal.org.bouncycastle.math.ec.ECPoint;
import com.android.internal.org.bouncycastle.math.ec.PreCompCallback;
import com.android.internal.org.bouncycastle.math.ec.PreCompInfo;

/**
 * @hide This class is not part of the Android public SDK API
 */
public abstract class EndoUtil
{
    public static final String PRECOMP_NAME = "bc_endo";

    public static BigInteger[] decomposeScalar(ScalarSplitParameters p, BigInteger k)
    {
        int bits = p.getBits();
        BigInteger b1 = calculateB(k, p.getG1(), bits);
        BigInteger b2 = calculateB(k, p.getG2(), bits);

        BigInteger a = k.subtract((b1.multiply(p.getV1A())).add(b2.multiply(p.getV2A())));
        BigInteger b = (b1.multiply(p.getV1B())).add(b2.multiply(p.getV2B())).negate();

        return new BigInteger[]{ a, b };
    }

    public static ECPoint mapPoint(final ECEndomorphism endomorphism, final ECPoint p)
    {
        final ECCurve c = p.getCurve();

        EndoPreCompInfo precomp = (EndoPreCompInfo)c.precompute(p, PRECOMP_NAME, new PreCompCallback()
        {
            public PreCompInfo precompute(PreCompInfo existing)
            {
                EndoPreCompInfo existingEndo = (existing instanceof EndoPreCompInfo) ? (EndoPreCompInfo)existing : null;

                if (checkExisting(existingEndo, endomorphism))
                {
                    return existingEndo;
                }

                ECPoint mappedPoint = endomorphism.getPointMap().map(p);

                EndoPreCompInfo result = new EndoPreCompInfo();
                result.setEndomorphism(endomorphism);
                result.setMappedPoint(mappedPoint);
                return result;
            }

            private boolean checkExisting(EndoPreCompInfo existingEndo, ECEndomorphism endomorphism)
            {
                return null != existingEndo
                    && existingEndo.getEndomorphism() == endomorphism
                    && existingEndo.getMappedPoint() != null;
            }
        });

        return precomp.getMappedPoint();
    }

    private static BigInteger calculateB(BigInteger k, BigInteger g, int t)
    {
        boolean negative = (g.signum() < 0);
        BigInteger b = k.multiply(g.abs());
        boolean extra = b.testBit(t - 1);
        b = b.shiftRight(t);
        if (extra)
        {
            b = b.add(ECConstants.ONE);
        }
        return negative ? b.negate() : b;
    }
}
