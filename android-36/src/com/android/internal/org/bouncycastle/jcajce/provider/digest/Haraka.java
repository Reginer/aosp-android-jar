/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.jcajce.provider.digest;

import com.android.internal.org.bouncycastle.crypto.digests.Haraka256Digest;
import com.android.internal.org.bouncycastle.crypto.digests.Haraka512Digest;
import com.android.internal.org.bouncycastle.jcajce.provider.config.ConfigurableProvider;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class Haraka
{
    private Haraka()
    {

    }

    /**
     * @hide This class is not part of the Android public SDK API
     */
    static public class Digest256
        extends BCMessageDigest
        implements Cloneable
    {
        public Digest256()
        {
            super(new Haraka256Digest());
        }

        public Object clone()
            throws CloneNotSupportedException
        {
            Digest256 d = (Digest256)super.clone();
            d.digest = new Haraka256Digest((Haraka256Digest)digest);

            return d;
        }
    }

    /**
     * @hide This class is not part of the Android public SDK API
     */
    static public class Digest512
        extends BCMessageDigest
        implements Cloneable
    {
        public Digest512()
        {
            super(new Haraka512Digest());
        }

        public Object clone()
            throws CloneNotSupportedException
        {
            Digest512 d = (Digest512)super.clone();
            d.digest = new Haraka512Digest((Haraka512Digest)digest);

            return d;
        }
    }

    /**
     * @hide This class is not part of the Android public SDK API
     */
    public static class Mappings
        extends DigestAlgorithmProvider
    {
        private static final String PREFIX = Haraka.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("MessageDigest.HARAKA-256", PREFIX + "$Digest256");
            provider.addAlgorithm("MessageDigest.HARAKA-512", PREFIX + "$Digest512");
        }
    }
}
