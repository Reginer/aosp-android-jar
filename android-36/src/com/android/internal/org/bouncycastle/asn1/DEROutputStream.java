/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.asn1;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Stream that outputs encoding based on distinguished encoding rules.
 * @hide This class is not part of the Android public SDK API
 */
// BEGIN Android-changed: Class is package-private in upstream.
// Leaving as public as it's used by build/make/tools/signapk/src/com/android/signapk/SignApk.java
public class DEROutputStream
    extends ASN1OutputStream
{
    public DEROutputStream(OutputStream os)
    {
        super(os);
    }

    DEROutputStream getDERSubStream()
    {
        return this;
    }

    void writeElements(ASN1Encodable[] elements)
        throws IOException
    {
        for (int i = 0, count = elements.length; i < count; ++i)
        {
            elements[i].toASN1Primitive().toDERObject().encode(this, true);
        }
    }

    void writePrimitive(ASN1Primitive primitive, boolean withTag) throws IOException
    {
        primitive.toDERObject().encode(this, withTag);
    }

    void writePrimitives(ASN1Primitive[] primitives)
        throws IOException
    {
        int count = primitives.length;
        for (int i = 0; i < count; ++i)
        {
            primitives[i].toDERObject().encode(this, true);
        }
    }
}
