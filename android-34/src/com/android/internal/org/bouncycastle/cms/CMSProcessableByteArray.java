/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.android.internal.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.internal.org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import com.android.internal.org.bouncycastle.util.Arrays;

/**
 * a holding class for a byte array of data to be processed.
 * @hide This class is not part of the Android public SDK API
 */
public class CMSProcessableByteArray
    implements CMSTypedData, CMSReadable
{
    private final ASN1ObjectIdentifier type;
    private final byte[]  bytes;

    public CMSProcessableByteArray(
        byte[]  bytes)
    {
        this(CMSObjectIdentifiers.data, bytes);
    }

    public CMSProcessableByteArray(
        ASN1ObjectIdentifier type,
        byte[]  bytes)
    {
        this.type = type;
        this.bytes = bytes;
    }

    public InputStream getInputStream()
    {
        return new ByteArrayInputStream(bytes);
    }

    public void write(OutputStream zOut)
        throws IOException, CMSException
    {
        zOut.write(bytes);
    }

    public Object getContent()
    {
        return Arrays.clone(bytes);
    }

    public ASN1ObjectIdentifier getContentType()
    {
        return type;
    }
}
