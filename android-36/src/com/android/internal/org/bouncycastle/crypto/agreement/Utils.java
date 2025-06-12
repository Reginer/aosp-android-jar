/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.crypto.agreement;

import com.android.internal.org.bouncycastle.crypto.CryptoServiceProperties;
import com.android.internal.org.bouncycastle.crypto.CryptoServicePurpose;
import com.android.internal.org.bouncycastle.crypto.constraints.ConstraintUtils;
import com.android.internal.org.bouncycastle.crypto.constraints.DefaultServiceProperties;
import com.android.internal.org.bouncycastle.crypto.params.DHKeyParameters;
import com.android.internal.org.bouncycastle.crypto.params.ECKeyParameters;
// Android-removed: unsupported algorithms
// import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
// import org.bouncycastle.crypto.params.X448PrivateKeyParameters;

class Utils
{
    static CryptoServiceProperties getDefaultProperties(String algorithm, ECKeyParameters k)
    {
        return new DefaultServiceProperties(algorithm, ConstraintUtils.bitsOfSecurityFor(k.getParameters().getCurve()), k, CryptoServicePurpose.AGREEMENT);
    }

    static CryptoServiceProperties getDefaultProperties(String algorithm, DHKeyParameters k)
    {
        return new DefaultServiceProperties(algorithm, ConstraintUtils.bitsOfSecurityFor(k.getParameters().getP()), k, CryptoServicePurpose.AGREEMENT);
    }

    // BEGIN Android-removed: unsupported algorithms
    /*
    static CryptoServiceProperties getDefaultProperties(String algorithm, X448PrivateKeyParameters k)
    {
        return new DefaultServiceProperties(algorithm, 224, k, CryptoServicePurpose.AGREEMENT);
    }

    static CryptoServiceProperties getDefaultProperties(String algorithm, X25519PrivateKeyParameters k)
    {
        return new DefaultServiceProperties(algorithm, 128, k, CryptoServicePurpose.AGREEMENT);
    }
    */
    // END Android-removed: unsupported algorithms
}


