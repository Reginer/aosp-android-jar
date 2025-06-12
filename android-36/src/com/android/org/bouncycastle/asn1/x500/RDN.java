/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.org.bouncycastle.asn1.x500;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Set;
import com.android.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.org.bouncycastle.asn1.DERSet;

/**
 * Holding class for a single Relative Distinguished Name (RDN).
 * @hide This class is not part of the Android public SDK API
 */
public class RDN
    extends ASN1Object
{
    private ASN1Set values;

    private RDN(ASN1Set values)
    {
        // TODO Require minimum size of 1?
        this.values = values;
    }

    public static RDN getInstance(Object obj)
    {
        if (obj instanceof RDN)
        {
            return (RDN)obj;
        }
        else if (obj != null)
        {
            return new RDN(ASN1Set.getInstance(obj));
        }

        return null;
    }

    public static RDN getInstance(ASN1TaggedObject taggedObject, boolean declaredExplicit)
    {
        return new RDN(ASN1Set.getInstance(taggedObject, declaredExplicit));
    }

    /**
     * Create a single valued RDN.
     *
     * @param oid RDN type.
     * @param value RDN value.
     */
    public RDN(ASN1ObjectIdentifier oid, ASN1Encodable value)
    {
        this(new AttributeTypeAndValue(oid, value));
    }

    public RDN(AttributeTypeAndValue attrTAndV)
    {
        this.values = new DERSet(attrTAndV);
    }

    /**
     * Create a multi-valued RDN.
     *
     * @param aAndVs attribute type/value pairs making up the RDN
     */
    public RDN(AttributeTypeAndValue[] aAndVs)
    {
        this.values = new DERSet(aAndVs);
    }

    public boolean isMultiValued()
    {
        return this.values.size() > 1;
    }

    /**
     * Return the number of AttributeTypeAndValue objects in this RDN,
     *
     * @return size of RDN, greater than 1 if multi-valued.
     */
    public int size()
    {
        return this.values.size();
    }

    public AttributeTypeAndValue getFirst()
    {
        if (this.values.size() == 0)
        {
            return null;
        }

        return AttributeTypeAndValue.getInstance(this.values.getObjectAt(0));
    }

    public AttributeTypeAndValue[] getTypesAndValues()
    {
        AttributeTypeAndValue[] tmp = new AttributeTypeAndValue[values.size()];

        for (int i = 0; i != tmp.length; i++)
        {
            tmp[i] = AttributeTypeAndValue.getInstance(values.getObjectAt(i));
        }

        return tmp;
    }

    int collectAttributeTypes(ASN1ObjectIdentifier[] oids, int oidsOff)
    {
        int count = values.size();
        for (int i = 0; i < count; ++i)
        {
            AttributeTypeAndValue attr = AttributeTypeAndValue.getInstance(values.getObjectAt(i));
            oids[oidsOff + i] = attr.getType();
        }
        return count;
    }

    boolean containsAttributeType(ASN1ObjectIdentifier attributeType)
    {
        int count = values.size();
        for (int i = 0; i < count; ++i)
        {
            AttributeTypeAndValue attr = AttributeTypeAndValue.getInstance(values.getObjectAt(i));
            if (attr.getType().equals(attributeType))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * <pre>
     * RelativeDistinguishedName ::=
     *                     SET OF AttributeTypeAndValue

     * AttributeTypeAndValue ::= SEQUENCE {
     *        type     AttributeType,
     *        value    AttributeValue }
     * </pre>
     * @return this object as its ASN1Primitive type
     */
    public ASN1Primitive toASN1Primitive()
    {
        return values;
    }
}
