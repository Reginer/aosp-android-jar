/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cert.ocsp;

import java.io.IOException;

import com.android.internal.org.bouncycastle.asn1.ASN1OctetString;
import com.android.internal.org.bouncycastle.asn1.DEROctetString;
import com.android.internal.org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import com.android.internal.org.bouncycastle.asn1.ocsp.OCSPResponse;
import com.android.internal.org.bouncycastle.asn1.ocsp.OCSPResponseStatus;
import com.android.internal.org.bouncycastle.asn1.ocsp.ResponseBytes;

/**
 * base generator for an OCSP response - at the moment this only supports the
 * generation of responses containing BasicOCSP responses.
 * @hide This class is not part of the Android public SDK API
 */
public class OCSPRespBuilder
{
    public static final int SUCCESSFUL = 0;  // Response has valid confirmations
    public static final int MALFORMED_REQUEST = 1;  // Illegal confirmation request
    public static final int INTERNAL_ERROR = 2;  // Internal error in issuer
    public static final int TRY_LATER = 3;  // Try again later
    // (4) is not used
    public static final int SIG_REQUIRED = 5;  // Must sign the request
    public static final int UNAUTHORIZED = 6;  // Request unauthorized

    public OCSPResp build(
        int status,
        Object response)
        throws OCSPException
    {
        if (response == null)
        {
            return new OCSPResp(new OCSPResponse(new OCSPResponseStatus(status), null));
        }

        if (response instanceof BasicOCSPResp)
        {
            BasicOCSPResp r = (BasicOCSPResp)response;
            ASN1OctetString octs;

            try
            {
                octs = new DEROctetString(r.getEncoded());
            }
            catch (IOException e)
            {
                throw new OCSPException("can't encode object.", e);
            }

            ResponseBytes rb = new ResponseBytes(
                OCSPObjectIdentifiers.id_pkix_ocsp_basic, octs);

            return new OCSPResp(new OCSPResponse(
                new OCSPResponseStatus(status), rb));
        }

        throw new OCSPException("unknown response object");
    }
}
