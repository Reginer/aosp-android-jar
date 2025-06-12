/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.asn1.ocsp;

import java.math.BigInteger;

import com.android.internal.org.bouncycastle.asn1.ASN1Enumerated;
import com.android.internal.org.bouncycastle.asn1.ASN1Object;
import com.android.internal.org.bouncycastle.asn1.ASN1Primitive;


/**
 * OCSP RFC 2560, RFC 6960
 * <p>
 * The OCSPResponseStatus enumeration.
 * <pre>
 * OCSPResponseStatus ::= ENUMERATED {
 *     successful            (0),  --Response has valid confirmations
 *     malformedRequest      (1),  --Illegal confirmation request
 *     internalError         (2),  --Internal error in issuer
 *     tryLater              (3),  --Try again later
 *                                 --(4) is not used
 *     sigRequired           (5),  --Must sign the request
 *     unauthorized          (6)   --Request unauthorized
 * }
 * </pre>
 * @hide This class is not part of the Android public SDK API
 */
public class OCSPResponseStatus
    extends ASN1Object
{
    public static final int SUCCESSFUL = 0;
    public static final int MALFORMED_REQUEST = 1;
    public static final int INTERNAL_ERROR = 2;
    public static final int TRY_LATER = 3;
    public static final int SIG_REQUIRED = 5;
    public static final int UNAUTHORIZED = 6;

    private ASN1Enumerated value;

    /**
     * RFC 2560, RFC 6960
     * <p>
     * The OCSPResponseStatus enumeration.
     * <pre>
     * OCSPResponseStatus ::= ENUMERATED {
     *     successful            (0),  --Response has valid confirmations
     *     malformedRequest      (1),  --Illegal confirmation request
     *     internalError         (2),  --Internal error in issuer
     *     tryLater              (3),  --Try again later
     *                                 --(4) is not used
     *     sigRequired           (5),  --Must sign the request
     *     unauthorized          (6)   --Request unauthorized
     * }
     * </pre>
     */
    public OCSPResponseStatus(
        int value)
    {
        this(new ASN1Enumerated(value));
    }

    private OCSPResponseStatus(
        ASN1Enumerated value)
    {
        this.value = value;
    }

    public static OCSPResponseStatus getInstance(
        Object  obj)
    {
        if (obj instanceof OCSPResponseStatus)
        {
            return (OCSPResponseStatus)obj;
        }
        else if (obj != null)
        {
            return new OCSPResponseStatus(ASN1Enumerated.getInstance(obj));
        }

        return null;
    }

    public int getIntValue()
    {
        return value.intValueExact();
    }

    public BigInteger getValue()
    {
        return value.getValue();
    }

    public ASN1Primitive toASN1Primitive()
    {
        return value;
    }
}
