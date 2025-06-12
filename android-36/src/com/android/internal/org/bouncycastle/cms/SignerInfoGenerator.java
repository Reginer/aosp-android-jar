/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cms;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.android.internal.org.bouncycastle.asn1.ASN1Encoding;
import com.android.internal.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.internal.org.bouncycastle.asn1.ASN1Set;
import com.android.internal.org.bouncycastle.asn1.DEROctetString;
import com.android.internal.org.bouncycastle.asn1.DERSet;
import com.android.internal.org.bouncycastle.asn1.cms.AttributeTable;
import com.android.internal.org.bouncycastle.asn1.cms.SignerIdentifier;
import com.android.internal.org.bouncycastle.asn1.cms.SignerInfo;
// import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
// import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import com.android.internal.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.internal.org.bouncycastle.cert.X509CertificateHolder;
import com.android.internal.org.bouncycastle.operator.ContentSigner;
import com.android.internal.org.bouncycastle.operator.DigestCalculator;
import com.android.internal.org.bouncycastle.util.Arrays;
import com.android.internal.org.bouncycastle.util.io.TeeOutputStream;

/**
 * @hide This class is not part of the Android public SDK API
 */
public class SignerInfoGenerator
{
    private final SignerIdentifier signerIdentifier;
    private final CMSAttributeTableGenerator sAttrGen;
    private final CMSAttributeTableGenerator unsAttrGen;
    private final ContentSigner signer;
    private final DigestCalculator digester;
    private final AlgorithmIdentifier digestAlgorithm;
    private final CMSSignatureEncryptionAlgorithmFinder sigEncAlgFinder;

    private byte[] calculatedDigest = null;
    private X509CertificateHolder certHolder;

    SignerInfoGenerator(
        SignerIdentifier signerIdentifier,
        ContentSigner signer,
        AlgorithmIdentifier digesterAlgorithm,
        CMSSignatureEncryptionAlgorithmFinder sigEncAlgFinder)
    {
        this.signerIdentifier = signerIdentifier;
        this.signer = signer;
        this.digestAlgorithm = digesterAlgorithm;
        this.digester = null;
        this.sAttrGen = null;
        this.unsAttrGen = null;
        this.sigEncAlgFinder = sigEncAlgFinder;
    }

    SignerInfoGenerator(
        SignerIdentifier signerIdentifier,
        ContentSigner signer,
        DigestCalculator digester,
        CMSSignatureEncryptionAlgorithmFinder sigEncAlgFinder,
        CMSAttributeTableGenerator sAttrGen,
        CMSAttributeTableGenerator unsAttrGen)
    {
        this.signerIdentifier = signerIdentifier;
        this.signer = signer;
        this.digestAlgorithm = digester.getAlgorithmIdentifier();
        this.digester = digester;
        this.sAttrGen = sAttrGen;
        this.unsAttrGen = unsAttrGen;
        this.sigEncAlgFinder = sigEncAlgFinder;
    }

    public SignerInfoGenerator(
        SignerInfoGenerator original,
        CMSAttributeTableGenerator sAttrGen,
        CMSAttributeTableGenerator unsAttrGen)
    {
        this.signerIdentifier = original.signerIdentifier;
        this.signer = original.signer;
        this.digestAlgorithm = original.digestAlgorithm;
        this.digester = original.digester;
        this.sigEncAlgFinder = original.sigEncAlgFinder;
        this.sAttrGen = sAttrGen;
        this.unsAttrGen = unsAttrGen;
    }

    public SignerIdentifier getSID()
    {
        return signerIdentifier;
    }

    public int getGeneratedVersion()
    {
        return signerIdentifier.isTagged() ? 3 : 1;
    }

    public boolean hasAssociatedCertificate()
    {
        return certHolder != null;
    }

    public X509CertificateHolder getAssociatedCertificate()
    {
        return certHolder;
    }
    
    public AlgorithmIdentifier getDigestAlgorithm()
    {
        return digestAlgorithm;
    }
    
    public OutputStream getCalculatingOutputStream()
    {
        if (digester != null)
        {
            if (sAttrGen == null)
            {
                return new TeeOutputStream(digester.getOutputStream(), signer.getOutputStream());    
            }
            return digester.getOutputStream();
        }
        else
        {
            return signer.getOutputStream();
        }
    }

    public SignerInfo generate(ASN1ObjectIdentifier contentType)
        throws CMSException
    {
        try
        {
            /* RFC 3852 5.4
             * The result of the message digest calculation process depends on
             * whether the signedAttrs field is present.  When the field is absent,
             * the result is just the message digest of the content as described
             *
             * above.  When the field is present, however, the result is the message
             * digest of the complete DER encoding of the SignedAttrs value
             * contained in the signedAttrs field.
             */
            ASN1Set signedAttr = null;

            AlgorithmIdentifier digestEncryptionAlgorithm = sigEncAlgFinder.findEncryptionAlgorithm(signer.getAlgorithmIdentifier());

            AlgorithmIdentifier digestAlg = null;

            if (sAttrGen != null)
            {
                digestAlg = digester.getAlgorithmIdentifier();
                calculatedDigest = digester.getDigest();
                Map parameters = getBaseParameters(contentType, digester.getAlgorithmIdentifier(), digestEncryptionAlgorithm, calculatedDigest);
                AttributeTable signed = sAttrGen.getAttributes(Collections.unmodifiableMap(parameters));

                signedAttr = getAttributeSet(signed);

                // sig must be composed from the DER encoding.
                OutputStream sOut = signer.getOutputStream();

                sOut.write(signedAttr.getEncoded(ASN1Encoding.DER));

                sOut.close();
            }
            else
            {
                digestAlg = digestAlgorithm;
                if (digester != null)
                {
                    calculatedDigest = digester.getDigest();
                }
                else
                {
                    calculatedDigest = null;
                }
            }

            byte[] sigBytes = signer.getSignature();

            ASN1Set unsignedAttr = null;
            if (unsAttrGen != null)
            {
                Map parameters = getBaseParameters(contentType, digestAlg, digestEncryptionAlgorithm, calculatedDigest);
                parameters.put(CMSAttributeTableGenerator.SIGNATURE, Arrays.clone(sigBytes));

                AttributeTable unsigned = unsAttrGen.getAttributes(Collections.unmodifiableMap(parameters));

                unsignedAttr = getAttributeSet(unsigned);
            }

            if (sAttrGen == null)
            {
                // BEGIN Android-removed: Unsupported algorithms
                /*
                // RFC 8419, Section 3.2 - needs to be shake-256, not shake-256-len
                if (EdECObjectIdentifiers.id_Ed448.equals(digestEncryptionAlgorithm.getAlgorithm()))
                {
                    digestAlg = new AlgorithmIdentifier(NISTObjectIdentifiers.id_shake256);
                }
                */
                // END Android-removed: Unsupported algorithms
            }

            return new SignerInfo(signerIdentifier, digestAlg,
                signedAttr, digestEncryptionAlgorithm, new DEROctetString(sigBytes), unsignedAttr);
        }
        catch (IOException e)
        {
            throw new CMSException("encoding error.", e);
        }
    }

    void setAssociatedCertificate(X509CertificateHolder certHolder)
    {
        this.certHolder = certHolder;
    }

    private ASN1Set getAttributeSet(
        AttributeTable attr)
    {
        if (attr != null)
        {
            return new DERSet(attr.toASN1EncodableVector());
        }

        return null;
    }

    private Map getBaseParameters(ASN1ObjectIdentifier contentType, AlgorithmIdentifier digAlgId, AlgorithmIdentifier sigAlgId, byte[] hash)
    {
        Map param = new HashMap();

        if (contentType != null)
        {
            param.put(CMSAttributeTableGenerator.CONTENT_TYPE, contentType);
        }

        param.put(CMSAttributeTableGenerator.DIGEST_ALGORITHM_IDENTIFIER, digAlgId);
        param.put(CMSAttributeTableGenerator.SIGNATURE_ALGORITHM_IDENTIFIER, sigAlgId);
        param.put(CMSAttributeTableGenerator.DIGEST,  Arrays.clone(hash));

        return param;
    }

    public byte[] getCalculatedDigest()
    {
        if (calculatedDigest != null)
        {
            return Arrays.clone(calculatedDigest);
        }

        return null;
    }

    public CMSAttributeTableGenerator getSignedAttributeTableGenerator()
    {
        return sAttrGen;
    }

    public CMSAttributeTableGenerator getUnsignedAttributeTableGenerator()
    {
        return unsAttrGen;
    }
}
