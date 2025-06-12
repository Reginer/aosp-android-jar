/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.asn1.x509;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import com.android.internal.org.bouncycastle.asn1.ASN1Encodable;
import com.android.internal.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.internal.org.bouncycastle.asn1.ASN1Encoding;
import com.android.internal.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.internal.org.bouncycastle.asn1.ASN1ParsingException;
import com.android.internal.org.bouncycastle.asn1.ASN1Sequence;
import com.android.internal.org.bouncycastle.asn1.DEROctetString;
import com.android.internal.org.bouncycastle.asn1.DERSequence;
import com.android.internal.org.bouncycastle.util.Arrays;

/**
 * Generator for X.509 extensions
 * @hide This class is not part of the Android public SDK API
 */
public class ExtensionsGenerator
{
    private Hashtable extensions = new Hashtable();
    private Vector extOrdering = new Vector();
    private static final Set dupsAllowed;


    static
    {
        Set dups = new HashSet();
        dups.add(Extension.subjectAlternativeName);
        dups.add(Extension.issuerAlternativeName);
        dups.add(Extension.subjectDirectoryAttributes);
        dups.add(Extension.certificateIssuer);
        dupsAllowed = Collections.unmodifiableSet(dups);
    }

    /**
     * Reset the generator
     */
    public void reset()
    {
        extensions = new Hashtable();
        extOrdering = new Vector();
    }

    /**
     * Add an extension with the given oid and the passed in value to be included
     * in the OCTET STRING associated with the extension.
     *
     * @param oid      OID for the extension.
     * @param critical true if critical, false otherwise.
     * @param value    the ASN.1 object to be included in the extension.
     */
    public void addExtension(
        ASN1ObjectIdentifier oid,
        boolean critical,
        ASN1Encodable value)
        throws IOException
    {
        this.addExtension(oid, critical, value.toASN1Primitive().getEncoded(ASN1Encoding.DER));
    }

    /**
     * Add an extension with the given oid and the passed in byte array to be wrapped in the
     * OCTET STRING associated with the extension.
     *
     * @param oid      OID for the extension.
     * @param critical true if critical, false otherwise.
     * @param value    the byte array to be wrapped.
     */
    public void addExtension(
        ASN1ObjectIdentifier oid,
        boolean critical,
        byte[] value)
    {
        if (extensions.containsKey(oid))
        {
            if (dupsAllowed.contains(oid))
            {
                Extension existingExtension = (Extension)extensions.get(oid);
                ASN1Sequence seq1 = ASN1Sequence.getInstance(DEROctetString.getInstance(existingExtension.getExtnValue()).getOctets());
                ASN1Sequence seq2 = ASN1Sequence.getInstance(value);

                ASN1EncodableVector items = new ASN1EncodableVector(seq1.size() + seq2.size());
                for (Enumeration en = seq1.getObjects(); en.hasMoreElements();)
                {
                    items.add((ASN1Encodable)en.nextElement());
                }
                for (Enumeration en = seq2.getObjects(); en.hasMoreElements();)
                {
                    items.add((ASN1Encodable)en.nextElement());
                }
                
                try
                {
                    extensions.put(oid, new Extension(oid, critical, new DERSequence(items).getEncoded()));
                }
                catch (IOException e)
                {
                    throw new ASN1ParsingException(e.getMessage(), e);
                }
            }
            else
            {
                throw new IllegalArgumentException("extension " + oid + " already added");
            }
        }
        else
        {
            extOrdering.addElement(oid);
            extensions.put(oid, new Extension(oid, critical, new DEROctetString(Arrays.clone(value))));
        }
    }

    /**
     * Add a given extension.
     *
     * @param extension the full extension value.
     */
    public void addExtension(
        Extension extension)
    {
        if (extensions.containsKey(extension.getExtnId()))
        {
            throw new IllegalArgumentException("extension " + extension.getExtnId() + " already added");
        }

        extOrdering.addElement(extension.getExtnId());
        extensions.put(extension.getExtnId(), extension);
    }

    /**
     * Replace an extension with the given oid and the passed in value to be included
     * in the OCTET STRING associated with the extension.
     *
     * @param oid      OID for the extension.
     * @param critical true if critical, false otherwise.
     * @param value    the ASN.1 object to be included in the extension.
     */
    public void replaceExtension(
        ASN1ObjectIdentifier oid,
        boolean critical,
        ASN1Encodable value)
        throws IOException
    {
        this.replaceExtension(oid, critical, value.toASN1Primitive().getEncoded(ASN1Encoding.DER));
    }

    /**
     * Replace an extension with the given oid and the passed in byte array to be wrapped in the
     * OCTET STRING associated with the extension.
     *
     * @param oid      OID for the extension.
     * @param critical true if critical, false otherwise.
     * @param value    the byte array to be wrapped.
     */
    public void replaceExtension(
        ASN1ObjectIdentifier oid,
        boolean critical,
        byte[] value)
    {
        this.replaceExtension(new Extension(oid, critical, value));
    }

    /**
     * Replace a given extension.
     *
     * @param extension the full extension value.
     */
    public void replaceExtension(
        Extension extension)
    {
        if (!extensions.containsKey(extension.getExtnId()))
        {
            throw new IllegalArgumentException("extension " + extension.getExtnId() + " not present");
        }

        extensions.put(extension.getExtnId(), extension);
    }

    /**
     * Remove a given extension.
     *
     * @param oid OID for the extension to remove.
     */
    public void removeExtension(
        ASN1ObjectIdentifier oid)
    {
        if (!extensions.containsKey(oid))
        {
            throw new IllegalArgumentException("extension " + oid + " not present");
        }

        extOrdering.removeElement(oid);
        extensions.remove(oid);
    }

    /**
     * Return if the extension indicated by OID is present.
     *
     * @param oid the OID for the extension of interest.
     * @return the Extension, or null if it is not present.
     */
    public boolean hasExtension(ASN1ObjectIdentifier oid)
    {
        return extensions.containsKey(oid);
    }

    /**
     * Return the current value of the extension for OID.
     *
     * @param oid the OID for the extension we want to fetch.
     * @return true if a matching extension is present, false otherwise.
     */
    public Extension getExtension(ASN1ObjectIdentifier oid)
    {
        return (Extension)extensions.get(oid);
    }

    /**
     * Return true if there are no extension present in this generator.
     *
     * @return true if empty, false otherwise
     */
    public boolean isEmpty()
    {
        return extOrdering.isEmpty();
    }

    /**
     * Generate an Extensions object based on the current state of the generator.
     *
     * @return an X09Extensions object.
     */
    public Extensions generate()
    {
        Extension[] exts = new Extension[extOrdering.size()];

        for (int i = 0; i != extOrdering.size(); i++)
        {
            exts[i] = (Extension)extensions.get(extOrdering.elementAt(i));
        }

        return new Extensions(exts);
    }

    public void addExtension(Extensions extensions)
    {
        ASN1ObjectIdentifier[] oids = extensions.getExtensionOIDs();
        for (int i = 0; i != oids.length; i++)
        {
            ASN1ObjectIdentifier ident = oids[i];
            Extension ext = extensions.getExtension(ident);
            addExtension(ASN1ObjectIdentifier.getInstance(ident), ext.isCritical(), ext.getExtnValue().getOctets());
        }
    }
}
