/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cms;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import com.android.internal.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.internal.org.bouncycastle.asn1.DEROctetString;
import com.android.internal.org.bouncycastle.asn1.DERSet;
import com.android.internal.org.bouncycastle.asn1.cms.Attribute;
import com.android.internal.org.bouncycastle.asn1.cms.AttributeTable;
import com.android.internal.org.bouncycastle.asn1.cms.CMSAlgorithmProtection;
import com.android.internal.org.bouncycastle.asn1.cms.CMSAttributes;
import com.android.internal.org.bouncycastle.asn1.cms.Time;
import com.android.internal.org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * Default signed attributes generator.
 * @hide This class is not part of the Android public SDK API
 */
public class DefaultSignedAttributeTableGenerator
    implements CMSAttributeTableGenerator
{
    private final Hashtable table;

    /**
     * Initialise to use all defaults
     */
    public DefaultSignedAttributeTableGenerator()
    {
        table = new Hashtable();
    }

    /**
     * Initialise with some extra attributes or overrides.
     *
     * @param attributeTable initial attribute table to use.
     */
    public DefaultSignedAttributeTableGenerator(
        AttributeTable attributeTable)
    {
        if (attributeTable != null)
        {
            table = attributeTable.toHashtable();
        }
        else
        {
            table = new Hashtable();
        }
    }

    /**
     * Create a standard attribute table from the passed in parameters - this will
     * normally include contentType, signingTime, messageDigest, and CMS algorithm protection.
     * If the constructor using an AttributeTable was used, entries in it for contentType, signingTime, and
     * messageDigest will override the generated ones.
     *
     * @param parameters source parameters for table generation.
     *
     * @return a filled in Hashtable of attributes.
     */
    protected Hashtable createStandardAttributeTable(
        Map parameters)
    {
        Hashtable std = copyHashTable(table);

        if (!std.containsKey(CMSAttributes.contentType))
        {
            ASN1ObjectIdentifier contentType = ASN1ObjectIdentifier.getInstance(
                parameters.get(CMSAttributeTableGenerator.CONTENT_TYPE));

            // contentType will be null if we're trying to generate a counter signature.
            if (contentType != null)
            {
                Attribute attr = new Attribute(CMSAttributes.contentType,
                    new DERSet(contentType));
                std.put(attr.getAttrType(), attr);
            }
        }

        if (!std.containsKey(CMSAttributes.signingTime))
        {
            Date signingTime = new Date();
            Attribute attr = new Attribute(CMSAttributes.signingTime,
                new DERSet(new Time(signingTime)));
            std.put(attr.getAttrType(), attr);
        }

        if (!std.containsKey(CMSAttributes.messageDigest))
        {
            byte[] messageDigest = (byte[])parameters.get(
                CMSAttributeTableGenerator.DIGEST);
            Attribute attr = new Attribute(CMSAttributes.messageDigest,
                new DERSet(new DEROctetString(messageDigest)));
            std.put(attr.getAttrType(), attr);
        }

        if (!std.contains(CMSAttributes.cmsAlgorithmProtect))
        {
            Attribute attr = new Attribute(CMSAttributes.cmsAlgorithmProtect, new DERSet(new CMSAlgorithmProtection(
                (AlgorithmIdentifier)parameters.get(CMSAttributeTableGenerator.DIGEST_ALGORITHM_IDENTIFIER),
                CMSAlgorithmProtection.SIGNATURE, (AlgorithmIdentifier)parameters.get(CMSAttributeTableGenerator.SIGNATURE_ALGORITHM_IDENTIFIER))));
            std.put(attr.getAttrType(), attr);
        }

        return std;
    }

    /**
     * @param parameters source parameters
     * @return the populated attribute table
     */
    public AttributeTable getAttributes(Map parameters)
    {
        return new AttributeTable(createStandardAttributeTable(parameters));
    }

    private static Hashtable copyHashTable(Hashtable paramsMap)
    {
        Hashtable newTable = new Hashtable();

        Enumeration keys = paramsMap.keys();
        while (keys.hasMoreElements())
        {
            Object key = keys.nextElement();
            newTable.put(key, paramsMap.get(key));
        }

        return newTable;
    }
}
