/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.asn1;

import java.io.IOException;

/**
 * An ASN1 class that encodes to nothing, used in the OER library to deal with the Optional type.
 * @hide This class is not part of the Android public SDK API
 */
public class ASN1Absent
    extends ASN1Primitive
{

    public static final ASN1Absent INSTANCE = new ASN1Absent();

    private ASN1Absent()
    {

    }

    public int hashCode()
    {
        return 0;
    }

    boolean encodeConstructed()
    {
        return false;
    }

    int encodedLength(boolean withTag)
        throws IOException
    {
        return 0;
    }

    void encode(ASN1OutputStream out, boolean withTag)
        throws IOException
    {

    }

    boolean asn1Equals(ASN1Primitive o)
    {
        return o == this;
    }
}
