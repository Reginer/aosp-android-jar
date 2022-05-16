/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.util;

import com.android.ide.common.rendering.api.AttrResourceValue;
import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.internal.util.XmlUtils;
import com.android.layoutlib.bridge.Bridge;
import com.android.layoutlib.bridge.BridgeConstants;
import com.android.layoutlib.bridge.android.BridgeContext;
import com.android.layoutlib.bridge.android.UnresolvedResourceValue;
import com.android.layoutlib.bridge.android.XmlPullParserResolver;
import com.android.layoutlib.bridge.impl.ResourceHelper;
import com.android.resources.ResourceType;

import org.xmlpull.v1.XmlPullParser;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.util.Map;
import java.util.function.Function;

/**
 * A correct implementation of the {@link AttributeSet} interface on top of a XmlPullParser
 */
public class BridgeXmlPullAttributes extends XmlPullAttributes implements ResolvingAttributeSet {

    interface EnumValueSupplier {
        @Nullable
        Map<String, Integer> getEnumValues(
                @NonNull ResourceNamespace namespace, @NonNull String attrName);
    }

    private final BridgeContext mContext;
    private final ResourceNamespace mXmlFileResourceNamespace;
    private final ResourceNamespace.Resolver mResourceNamespaceResolver;
    private final Function<String, Map<String, Integer>> mFrameworkEnumValueSupplier;
    private final EnumValueSupplier mProjectEnumValueSupplier;

    // VisibleForTesting
    BridgeXmlPullAttributes(
            @NonNull XmlPullParser parser,
            @NonNull BridgeContext context,
            @NonNull ResourceNamespace xmlFileResourceNamespace,
            @NonNull Function<String, Map<String, Integer>> frameworkEnumValueSupplier,
            @NonNull EnumValueSupplier projectEnumValueSupplier) {
        super(parser);
        mContext = context;
        mFrameworkEnumValueSupplier = frameworkEnumValueSupplier;
        mProjectEnumValueSupplier = projectEnumValueSupplier;
        mXmlFileResourceNamespace = xmlFileResourceNamespace;
        mResourceNamespaceResolver =
                new XmlPullParserResolver(
                        mParser, context.getLayoutlibCallback().getImplicitNamespaces());
    }

    public BridgeXmlPullAttributes(
            @NonNull XmlPullParser parser,
            @NonNull BridgeContext context,
            @NonNull ResourceNamespace xmlFileResourceNamespace) {
        this(parser, context, xmlFileResourceNamespace, Bridge::getEnumValues, (ns, attrName) -> {
            ResourceValue attr =
                    context.getRenderResources().getUnresolvedResource(
                            ResourceReference.attr(ns, attrName));
            return attr instanceof AttrResourceValue
                    ? ((AttrResourceValue) attr).getAttributeValues()
                    : null;
        });
    }

    /*
     * (non-Javadoc)
     * @see android.util.XmlPullAttributes#getAttributeNameResource(int)
     *
     * This methods must return com.android.internal.R.attr.<name> matching
     * the name of the attribute.
     * It returns 0 if it doesn't find anything.
     */
    @Override
    public int getAttributeNameResource(int index) {
        // get the attribute name.
        String name = getAttributeName(index);

        // get the attribute namespace
        String ns = mParser.getAttributeNamespace(index);

        if (BridgeConstants.NS_RESOURCES.equals(ns)) {
            return Bridge.getResourceId(ResourceType.ATTR, name);
        }

        // TODO(b/156609434): cache the namespace objects.
        ResourceNamespace namespace = ResourceNamespace.fromNamespaceUri(ns);
        if (namespace != null) {
            return mContext.getLayoutlibCallback().getOrGenerateResourceId(
                    ResourceReference.attr(namespace, name));
        }

        return 0;
    }

    @Override
    public int getAttributeListValue(String namespace, String attribute,
            String[] options, int defaultValue) {
        String value = getAttributeValue(namespace, attribute);
        if (value != null) {
            ResourceValue r = getResourceValue(value);

            if (r != null) {
                value = r.getValue();
            }

            return XmlUtils.convertValueToList(value, options, defaultValue);
        }

        return defaultValue;
    }

    @Override
    public boolean getAttributeBooleanValue(String namespace, String attribute,
            boolean defaultValue) {
        String value = getAttributeValue(namespace, attribute);
        if (value != null) {
            ResourceValue r = getResourceValue(value);

            if (r != null) {
                value = r.getValue();
            }

            return XmlUtils.convertValueToBoolean(value, defaultValue);
        }

        return defaultValue;
    }

    @Override
    public int getAttributeResourceValue(String namespace, String attribute, int defaultValue) {
        String value = getAttributeValue(namespace, attribute);

        return resolveResourceValue(value, defaultValue);
    }

    @Override
    public int getAttributeIntValue(String namespace, String attribute, int defaultValue) {
        String value = getAttributeValue(namespace, attribute);
        if (value == null) {
            return defaultValue;
        }

        ResourceValue r = getResourceValue(value);

        if (r != null) {
            value = r.getValue();
        }

        if (value.charAt(0) == '#') {
            return ResourceHelper.getColor(value);
        }

        try {
            return XmlUtils.convertValueToInt(value, defaultValue);
        } catch (NumberFormatException e) {
            // This is probably an enum
            Map<String, Integer> enumValues = null;
            if (BridgeConstants.NS_RESOURCES.equals(namespace)) {
                enumValues = mFrameworkEnumValueSupplier.apply(attribute);
            } else {
                ResourceNamespace attrNamespace = ResourceNamespace.fromNamespaceUri(namespace);
                if (attrNamespace != null) {
                    enumValues = mProjectEnumValueSupplier.getEnumValues(attrNamespace, attribute);
                }
            }

            Integer enumValue = enumValues != null ? enumValues.get(value) : null;
            if (enumValue != null) {
                return enumValue;
            }

            // We weren't able to find the enum int value
            throw e;
        }
    }

    @Override
    public int getAttributeUnsignedIntValue(String namespace, String attribute,
            int defaultValue) {
        String value = getAttributeValue(namespace, attribute);
        if (value != null) {
            ResourceValue r = getResourceValue(value);

            if (r != null) {
                value = r.getValue();
            }

            return XmlUtils.convertValueToUnsignedInt(value, defaultValue);
        }

        return defaultValue;
    }

    @Override
    public float getAttributeFloatValue(String namespace, String attribute,
            float defaultValue) {
        String s = getAttributeValue(namespace, attribute);
        if (s != null) {
            ResourceValue r = getResourceValue(s);

            if (r != null) {
                s = r.getValue();
            }

            return Float.parseFloat(s);
        }

        return defaultValue;
    }

    @Override
    public int getAttributeListValue(int index,
            String[] options, int defaultValue) {
        return XmlUtils.convertValueToList(
            getAttributeValue(index), options, defaultValue);
    }

    @Override
    public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
        String value = getAttributeValue(index);
        if (value != null) {
            ResourceValue r = getResourceValue(value);

            if (r != null) {
                value = r.getValue();
            }

            return XmlUtils.convertValueToBoolean(value, defaultValue);
        }

        return defaultValue;
    }

    @Override
    public int getAttributeResourceValue(int index, int defaultValue) {
        String value = getAttributeValue(index);

        return resolveResourceValue(value, defaultValue);
    }

    @Override
    public int getAttributeIntValue(int index, int defaultValue) {
        return getAttributeIntValue(
                mParser.getAttributeNamespace(index), getAttributeName(index), defaultValue);
    }

    @Override
    public int getAttributeUnsignedIntValue(int index, int defaultValue) {
        String value = getAttributeValue(index);
        if (value != null) {
            ResourceValue r = getResourceValue(value);

            if (r != null) {
                value = r.getValue();
            }

            return XmlUtils.convertValueToUnsignedInt(value, defaultValue);
        }

        return defaultValue;
    }

    @Override
    public float getAttributeFloatValue(int index, float defaultValue) {
        String s = getAttributeValue(index);
        if (s != null) {
            ResourceValue r = getResourceValue(s);

            if (r != null) {
                s = r.getValue();
            }

            return Float.parseFloat(s);
        }

        return defaultValue;
    }

    @Override
    @Nullable
    public ResourceValue getResolvedAttributeValue(@Nullable String namespace,
            @NonNull String name) {
        String s = getAttributeValue(namespace, name);
        return s == null ? null : getResourceValue(s);
    }

    // -- private helper methods

    /**
     * Returns a resolved {@link ResourceValue} from a given value.
     */
    private ResourceValue getResourceValue(String value) {
        // now look for this particular value
        return mContext.getRenderResources().resolveResValue(
                new UnresolvedResourceValue(
                        value, mXmlFileResourceNamespace, mResourceNamespaceResolver));
    }

    /**
     * Resolves and return a value to its associated integer.
     */
    private int resolveResourceValue(String value, int defaultValue) {
        ResourceValue resource = getResourceValue(value);
        if (resource != null) {
            return resource.isFramework() ?
                    Bridge.getResourceId(resource.getResourceType(), resource.getName()) :
                    mContext.getLayoutlibCallback().getOrGenerateResourceId(resource.asReference());
        }

        return defaultValue;
    }
}
