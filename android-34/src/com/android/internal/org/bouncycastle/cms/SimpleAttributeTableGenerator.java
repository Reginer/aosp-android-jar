/* GENERATED SOURCE. DO NOT MODIFY. */
package com.android.internal.org.bouncycastle.cms;

import com.android.internal.org.bouncycastle.asn1.cms.AttributeTable;

import java.util.Map;

/**
 * Basic generator that just returns a preconstructed attribute table
 * @hide This class is not part of the Android public SDK API
 */
public class SimpleAttributeTableGenerator
    implements CMSAttributeTableGenerator
{
    private final AttributeTable attributes;

    public SimpleAttributeTableGenerator(
        AttributeTable attributes)
    {
        this.attributes = attributes;
    }

    public AttributeTable getAttributes(Map parameters)
    {
        return attributes;
    }
}
