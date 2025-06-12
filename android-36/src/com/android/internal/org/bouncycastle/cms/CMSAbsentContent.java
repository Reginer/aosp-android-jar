/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.android.internal.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.internal.org.bouncycastle.asn1.cms.CMSObjectIdentifiers;

/**
 * a class representing null or absent content.
 * @hide This class is not part of the Android public SDK API
 */
public class CMSAbsentContent
    implements CMSTypedData, CMSReadable
{
    private final ASN1ObjectIdentifier type;

    public CMSAbsentContent()
    {
        this(CMSObjectIdentifiers.data);
    }

    public CMSAbsentContent(
        ASN1ObjectIdentifier type)
    {
        this.type = type;
    }

    public InputStream getInputStream()
    {
        return null;
    }

    public void write(OutputStream zOut)
        throws IOException, CMSException
    {
        // do nothing
    }

    public Object getContent()
    {
        return null;
    }

    public ASN1ObjectIdentifier getContentType()
    {
        return type;
    }
}
