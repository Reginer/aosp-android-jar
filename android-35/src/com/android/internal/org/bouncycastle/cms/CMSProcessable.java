/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cms;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Use CMSTypedData instead of this. See CMSProcessableFile/ByteArray for defaults.
 * @hide This class is not part of the Android public SDK API
 */
public interface CMSProcessable
{
    /**
     * generic routine to copy out the data we want processed - the OutputStream
     * passed in will do the handling on it's own.
     * <p>
     * Note: this routine may be called multiple times.
     */
    public void write(OutputStream out)
        throws IOException, CMSException;

    public Object getContent();
}
