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

package android.content.res;

import com.android.ide.common.rendering.api.ArrayResourceValue;
import com.android.ide.common.rendering.api.AttrResourceValue;
import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.ide.common.rendering.api.RenderResources;
import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.ide.common.rendering.api.ResourceNamespace.Resolver;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.StyleResourceValue;
import com.android.ide.common.rendering.api.TextResourceValue;
import com.android.ide.common.resources.ValueXmlHelper;
import com.android.internal.util.XmlUtils;
import com.android.layoutlib.bridge.Bridge;
import com.android.layoutlib.bridge.android.BridgeContext;
import com.android.layoutlib.bridge.android.UnresolvedResourceValue;
import com.android.layoutlib.bridge.impl.ResourceHelper;
import com.android.resources.ResourceType;
import com.android.resources.ResourceUrl;

import android.annotation.Nullable;
import android.content.res.Resources.Theme;
import android.graphics.Typeface;
import android.graphics.Typeface_Accessor;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater_Delegate;
import android.view.ViewGroup.LayoutParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static android.text.Html.FROM_HTML_MODE_COMPACT;
import static android.util.TypedValue.TYPE_ATTRIBUTE;
import static android.util.TypedValue.TYPE_DIMENSION;
import static android.util.TypedValue.TYPE_FLOAT;
import static android.util.TypedValue.TYPE_INT_BOOLEAN;
import static android.util.TypedValue.TYPE_INT_COLOR_ARGB4;
import static android.util.TypedValue.TYPE_INT_COLOR_ARGB8;
import static android.util.TypedValue.TYPE_INT_COLOR_RGB4;
import static android.util.TypedValue.TYPE_INT_COLOR_RGB8;
import static android.util.TypedValue.TYPE_INT_DEC;
import static android.util.TypedValue.TYPE_INT_HEX;
import static android.util.TypedValue.TYPE_NULL;
import static android.util.TypedValue.TYPE_REFERENCE;
import static android.util.TypedValue.TYPE_STRING;
import static com.android.SdkConstants.PREFIX_RESOURCE_REF;
import static com.android.SdkConstants.PREFIX_THEME_REF;
import static com.android.ide.common.rendering.api.RenderResources.REFERENCE_EMPTY;
import static com.android.ide.common.rendering.api.RenderResources.REFERENCE_NULL;
import static com.android.ide.common.rendering.api.RenderResources.REFERENCE_UNDEFINED;

/**
 * Custom implementation of TypedArray to handle non compiled resources.
 */
public final class BridgeTypedArray extends TypedArray {
    private static final String MATCH_PARENT_INT_STRING = String.valueOf(LayoutParams.MATCH_PARENT);
    private static final String WRAP_CONTENT_INT_STRING = String.valueOf(LayoutParams.WRAP_CONTENT);

    private final Resources mBridgeResources;
    private final BridgeContext mContext;

    private final int[] mResourceId;
    private final ResourceValue[] mResourceData;
    private final String[] mNames;
    private final ResourceNamespace[] mNamespaces;

    // Contains ids that are @empty. We still store null in mResourceData for that index, since we
    // want to save on the check against empty, each time a resource value is requested.
    @Nullable
    private int[] mEmptyIds;

    public BridgeTypedArray(Resources resources, BridgeContext context, int len) {
        super(resources);
        mBridgeResources = resources;
        mContext = context;
        mResourceId = new int[len];
        mResourceData = new ResourceValue[len];
        mNames = new String[len];
        mNamespaces = new ResourceNamespace[len];
    }

    /**
     * A bridge-specific method that sets a value in the type array
     * @param index the index of the value in the TypedArray
     * @param name the name of the attribute
     * @param namespace namespace of the attribute
     * @param resourceId the reference id of this resource
     * @param value the value of the attribute
     */
    public void bridgeSetValue(int index, String name, ResourceNamespace namespace, int resourceId,
            ResourceValue value) {
        mResourceId[index] = resourceId;
        mResourceData[index] = value;
        mNames[index] = name;
        mNamespaces[index] = namespace;
    }

    /**
     * Seals the array after all calls to
     * {@link #bridgeSetValue(int, String, ResourceNamespace, int, ResourceValue)} have been done.
     * <p/>This allows to compute the list of non default values, permitting
     * {@link #getIndexCount()} to return the proper value.
     */
    public void sealArray() {
        // fills TypedArray.mIndices which is used to implement getIndexCount/getIndexAt
        // first count the array size
        int count = 0;
        ArrayList<Integer> emptyIds = null;
        for (int i = 0; i < mResourceData.length; i++) {
            ResourceValue data = mResourceData[i];
            if (data != null) {
                String dataValue = data.getValue();
                if (REFERENCE_NULL.equals(dataValue) || REFERENCE_UNDEFINED.equals(dataValue)) {
                    mResourceData[i] = null;
                } else if (REFERENCE_EMPTY.equals(dataValue)) {
                    mResourceData[i] = null;
                    if (emptyIds == null) {
                        emptyIds = new ArrayList<>(4);
                    }
                    emptyIds.add(i);
                } else {
                    count++;
                }
            }
        }

        if (emptyIds != null) {
            mEmptyIds = new int[emptyIds.size()];
            for (int i = 0; i < emptyIds.size(); i++) {
                mEmptyIds[i] = emptyIds.get(i);
            }
        }

        // allocate the table with an extra to store the size
        mIndices = new int[count+1];
        mIndices[0] = count;

        // fill the array with the indices.
        int index = 1;
        for (int i = 0 ; i < mResourceData.length ; i++) {
            if (mResourceData[i] != null) {
                mIndices[index++] = i;
            }
        }
    }

    /**
     * Set the theme to be used for inflating drawables.
     */
    public void setTheme(Theme theme) {
        mTheme = theme;
    }

    /**
     * Return the number of values in this array.
     */
    @Override
    public int length() {
        return mResourceData.length;
    }

    /**
     * Return the Resources object this array was loaded from.
     */
    @Override
    public Resources getResources() {
        return mBridgeResources;
    }

    /**
     * Retrieve the styled string value for the attribute at <var>index</var>.
     *
     * @param index Index of attribute to retrieve.
     *
     * @return CharSequence holding string data.  May be styled.  Returns
     *         null if the attribute is not defined.
     */
    @Override
    public CharSequence getText(int index) {
        if (!hasValue(index)) {
            return null;
        }
        // As unfortunate as it is, it's possible to use enums with all attribute formats,
        // not just integers/enums. So, we need to search the enums always. In case
        // enums are used, the returned value is an integer.
        Integer v = resolveEnumAttribute(index);
        if (v != null) {
            return String.valueOf((int) v);
        }
        ResourceValue resourceValue = mResourceData[index];
        String value = resourceValue.getValue();
        if (resourceValue instanceof TextResourceValue) {
            String rawValue =
                    ValueXmlHelper.unescapeResourceString(resourceValue.getRawXmlValue(),
                            true, false);
            if (rawValue != null && !rawValue.equals(value)) {
                return Html.fromHtml(rawValue, FROM_HTML_MODE_COMPACT);
            }
        }
        return value;
    }

    /**
     * Retrieve the string value for the attribute at <var>index</var>.
     *
     * @param index Index of attribute to retrieve.
     *
     * @return String holding string data.  Any styling information is
     * removed.  Returns null if the attribute is not defined.
     */
    @Override
    public String getString(int index) {
        if (!hasValue(index)) {
            return null;
        }
        // As unfortunate as it is, it's possible to use enums with all attribute formats,
        // not just integers/enums. So, we need to search the enums always. In case
        // enums are used, the returned value is an integer.
        Integer v = resolveEnumAttribute(index);
        return v == null ? mResourceData[index].getValue() : String.valueOf((int) v);
    }

    /**
     * Retrieve the boolean value for the attribute at <var>index</var>.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined.
     *
     * @return Attribute boolean value, or defValue if not defined.
     */
    @Override
    public boolean getBoolean(int index, boolean defValue) {
        String s = getString(index);
        return s == null ? defValue : XmlUtils.convertValueToBoolean(s, defValue);

    }

    /**
     * Retrieve the integer value for the attribute at <var>index</var>.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined.
     *
     * @return Attribute int value, or defValue if not defined.
     */
    @Override
    public int getInt(int index, int defValue) {
        String s = getString(index);
        try {
            return convertValueToInt(s, defValue);
        } catch (NumberFormatException e) {
            Bridge.getLog().warning(ILayoutLog.TAG_RESOURCES_FORMAT,
                    String.format("\"%1$s\" in attribute \"%2$s\" is not a valid integer",
                            s, mNames[index]),
                    null, null);
        }
        return defValue;
    }

    /**
     * Retrieve the float value for the attribute at <var>index</var>.
     *
     * @param index Index of attribute to retrieve.
     *
     * @return Attribute float value, or defValue if not defined..
     */
    @Override
    public float getFloat(int index, float defValue) {
        String s = getString(index);
        try {
            if (s != null) {
                    return Float.parseFloat(s);
            }
        } catch (NumberFormatException e) {
            Bridge.getLog().warning(ILayoutLog.TAG_RESOURCES_FORMAT,
                    String.format("\"%1$s\" in attribute \"%2$s\" cannot be converted to float.",
                            s, mNames[index]),
                    null, null);
        }
        return defValue;
    }

    /**
     * Retrieve the color value for the attribute at <var>index</var>.  If
     * the attribute references a color resource holding a complex
     * {@link android.content.res.ColorStateList}, then the default color from
     * the set is returned.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined or
     *                 not a resource.
     *
     * @return Attribute color value, or defValue if not defined.
     */
    @Override
    public int getColor(int index, int defValue) {
        if (index < 0 || index >= mResourceData.length) {
            return defValue;
        }

        if (mResourceData[index] == null) {
            return defValue;
        }

        ColorStateList colorStateList = ResourceHelper.getColorStateList(
                mResourceData[index], mContext, mTheme);
        if (colorStateList != null) {
            return colorStateList.getDefaultColor();
        }

        return defValue;
    }

    @Override
    public ColorStateList getColorStateList(int index) {
        if (!hasValue(index)) {
            return null;
        }

        return ResourceHelper.getColorStateList(mResourceData[index], mContext, mTheme);
    }

    @Override
    public ComplexColor getComplexColor(int index) {
        if (!hasValue(index)) {
            return null;
        }

        return ResourceHelper.getComplexColor(mResourceData[index], mContext, mTheme);
    }

    /**
     * Retrieve the integer value for the attribute at <var>index</var>.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined or
     *                 not a resource.
     *
     * @return Attribute integer value, or defValue if not defined.
     */
    @Override
    public int getInteger(int index, int defValue) {
        return getInt(index, defValue);
    }

    /**
     * Retrieve a dimensional unit attribute at <var>index</var>.  Unit
     * conversions are based on the current {@link DisplayMetrics}
     * associated with the resources this {@link TypedArray} object
     * came from.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined or
     *                 not a resource.
     *
     * @return Attribute dimension value multiplied by the appropriate
     * metric, or defValue if not defined.
     *
     * @see #getDimensionPixelOffset
     * @see #getDimensionPixelSize
     */
    @Override
    public float getDimension(int index, float defValue) {
        String s = getString(index);
        if (s == null) {
            return defValue;
        }
        // Check if the value is a magic constant that doesn't require a unit.
        if (MATCH_PARENT_INT_STRING.equals(s)) {
            return LayoutParams.MATCH_PARENT;
        }
        if (WRAP_CONTENT_INT_STRING.equals(s)) {
            return LayoutParams.WRAP_CONTENT;
        }

        if (ResourceHelper.parseFloatAttribute(mNames[index], s, mValue, true)) {
            return mValue.getDimension(mBridgeResources.getDisplayMetrics());
        }

        return defValue;
    }

    /**
     * Retrieve a dimensional unit attribute at <var>index</var> for use
     * as an offset in raw pixels.  This is the same as
     * {@link #getDimension}, except the returned value is converted to
     * integer pixels for you.  An offset conversion involves simply
     * truncating the base value to an integer.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined or
     *                 not a resource.
     *
     * @return Attribute dimension value multiplied by the appropriate
     * metric and truncated to integer pixels, or defValue if not defined.
     *
     * @see #getDimension
     * @see #getDimensionPixelSize
     */
    @Override
    public int getDimensionPixelOffset(int index, int defValue) {
        return (int) getDimension(index, defValue);
    }

    /**
     * Retrieve a dimensional unit attribute at <var>index</var> for use
     * as a size in raw pixels.  This is the same as
     * {@link #getDimension}, except the returned value is converted to
     * integer pixels for use as a size.  A size conversion involves
     * rounding the base value, and ensuring that a non-zero base value
     * is at least one pixel in size.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined or
     *                 not a resource.
     *
     * @return Attribute dimension value multiplied by the appropriate
     * metric and truncated to integer pixels, or defValue if not defined.
     *
     * @see #getDimension
     * @see #getDimensionPixelOffset
     */
    @Override
    public int getDimensionPixelSize(int index, int defValue) {
        String s = getString(index);
        if (s == null) {
            return defValue;
        }

        if (MATCH_PARENT_INT_STRING.equals(s)) {
            return LayoutParams.MATCH_PARENT;
        }
        if (WRAP_CONTENT_INT_STRING.equals(s)) {
            return LayoutParams.WRAP_CONTENT;
        }

        if (ResourceHelper.parseFloatAttribute(mNames[index], s, mValue, true)) {
            float f = mValue.getDimension(mBridgeResources.getDisplayMetrics());

            final int res = (int) (f + 0.5f);
            if (res != 0) return res;
            if (f == 0) return 0;
            if (f > 0) return 1;
        }

        // looks like we were unable to resolve the dimension value
        Bridge.getLog().warning(ILayoutLog.TAG_RESOURCES_FORMAT,
                String.format("\"%1$s\" in attribute \"%2$s\" is not a valid format.", s, mNames[index]),
                null, null);

        return defValue;
    }

    /**
     * Special version of {@link #getDimensionPixelSize} for retrieving
     * {@link android.view.ViewGroup}'s layout_width and layout_height
     * attributes.  This is only here for performance reasons; applications
     * should use {@link #getDimensionPixelSize}.
     *
     * @param index Index of the attribute to retrieve.
     * @param name Textual name of attribute for error reporting.
     *
     * @return Attribute dimension value multiplied by the appropriate
     * metric and truncated to integer pixels.
     */
    @Override
    public int getLayoutDimension(int index, String name) {
        String s = getString(index);
        if (s != null) {
            // Check if the value is a magic constant that doesn't require a unit.
            if (MATCH_PARENT_INT_STRING.equals(s)) {
                return LayoutParams.MATCH_PARENT;
            }
            if (WRAP_CONTENT_INT_STRING.equals(s)) {
                return LayoutParams.WRAP_CONTENT;
            }

            if (ResourceHelper.parseFloatAttribute(mNames[index], s, mValue, true)) {
                float f = mValue.getDimension(mBridgeResources.getDisplayMetrics());

                final int res = (int) (f + 0.5f);
                if (res != 0) return res;
                if (f == 0) return 0;
                if (f > 0) return 1;
            }
        }

        if (LayoutInflater_Delegate.sIsInInclude) {
            throw new RuntimeException("Layout Dimension '" + name + "' not found.");
        }

        Bridge.getLog().warning(ILayoutLog.TAG_RESOURCES_FORMAT,
                "You must supply a " + name + " attribute.", null, null);

        return 0;
    }

    @Override
    public int getLayoutDimension(int index, int defValue) {
        return getDimensionPixelSize(index, defValue);
    }

    /**
     * Retrieve a fractional unit attribute at <var>index</var>.
     *
     * @param index Index of attribute to retrieve.
     * @param base The base value of this fraction.  In other words, a
     *             standard fraction is multiplied by this value.
     * @param pbase The parent base value of this fraction.  In other
     *             words, a parent fraction (nn%p) is multiplied by this
     *             value.
     * @param defValue Value to return if the attribute is not defined or
     *                 not a resource.
     *
     * @return Attribute fractional value multiplied by the appropriate
     * base value, or defValue if not defined.
     */
    @Override
    public float getFraction(int index, int base, int pbase, float defValue) {
        String value = getString(index);
        if (value == null) {
            return defValue;
        }

        if (ResourceHelper.parseFloatAttribute(mNames[index], value, mValue, false)) {
            return mValue.getFraction(base, pbase);
        }

        // looks like we were unable to resolve the fraction value
        Bridge.getLog().warning(ILayoutLog.TAG_RESOURCES_FORMAT,
                String.format(
                        "\"%1$s\" in attribute \"%2$s\" cannot be converted to a fraction.",
                        value, mNames[index]),
                null, null);

        return defValue;
    }

    /**
     * Retrieve the resource identifier for the attribute at
     * <var>index</var>.  Note that attribute resource as resolved when
     * the overall {@link TypedArray} object is retrieved.  As a
     * result, this function will return the resource identifier of the
     * final resource value that was found, <em>not</em> necessarily the
     * original resource that was specified by the attribute.
     *
     * @param index Index of attribute to retrieve.
     * @param defValue Value to return if the attribute is not defined or
     *                 not a resource.
     *
     * @return Attribute resource identifier, or defValue if not defined.
     */
    @Override
    public int getResourceId(int index, int defValue) {
        if (index < 0 || index >= mResourceData.length) {
            return defValue;
        }

        // get the Resource for this index
        ResourceValue resValue = mResourceData[index];

        // no data, return the default value.
        if (resValue == null) {
            return defValue;
        }

        // check if this is a style resource
        if (resValue instanceof StyleResourceValue) {
            // get the id that will represent this style.
            return mContext.getDynamicIdByStyle((StyleResourceValue)resValue);
        }

        // If the attribute was a reference to a resource, and not a declaration of an id (@+id),
        // then the xml attribute value was "resolved" which leads us to a ResourceValue with a
        // valid type, name, namespace and a potentially null value.
        if (!(resValue instanceof UnresolvedResourceValue)) {
            return mContext.getResourceId(resValue.asReference(), defValue);
        }

        // else, try to get the value, and resolve it somehow.
        String value = resValue.getValue();
        if (value == null) {
            return defValue;
        }
        value = value.trim();


        // `resValue` failed to be resolved. We extract the interesting bits and get rid of this
        // broken object. The namespace and resolver come from where the XML attribute was defined.
        ResourceNamespace contextNamespace = resValue.getNamespace();
        Resolver namespaceResolver = resValue.getNamespaceResolver();

        if (value.startsWith("#")) {
            // this looks like a color, do not try to parse it
            return defValue;
        }

        if (Typeface_Accessor.isSystemFont(value)) {
            // A system font family value, do not try to parse
            return defValue;
        }

        // Handle the @id/<name>, @+id/<name> and @android:id/<name>
        // We need to return the exact value that was compiled (from the various R classes),
        // as these values can be reused internally with calls to findViewById().
        // There's a trick with platform layouts that not use "android:" but their IDs are in
        // fact in the android.R and com.android.internal.R classes.
        // The field mPlatformFile will indicate that all IDs are to be looked up in the android R
        // classes exclusively.

        // if this is a reference to an id, find it.
        ResourceUrl resourceUrl = ResourceUrl.parse(value);
        if (resourceUrl != null) {
            if (resourceUrl.type == ResourceType.ID) {
                ResourceReference referencedId =
                        resourceUrl.resolve(contextNamespace, namespaceResolver);

                // Look for the idName in project or android R class depending on isPlatform.
                if (resourceUrl.isCreate()) {
                    int idValue;
                    if (referencedId.getNamespace() == ResourceNamespace.ANDROID) {
                        idValue = Bridge.getResourceId(ResourceType.ID, resourceUrl.name);
                    } else {
                        idValue = mContext.getLayoutlibCallback().getOrGenerateResourceId(referencedId);
                    }
                    return idValue;
                }
                // This calls the same method as in if(create), but doesn't create a dynamic id, if
                // one is not found.
                return mContext.getResourceId(referencedId, defValue);
            }
            else if (resourceUrl.type == ResourceType.AAPT) {
                ResourceReference referencedId =
                        resourceUrl.resolve(contextNamespace, namespaceResolver);
                return mContext.getLayoutlibCallback().getOrGenerateResourceId(referencedId);
            }
        }
        // not a direct id valid reference. First check if it's an enum (this is a corner case
        // for attributes that have a reference|enum type), then fallback to resolve
        // as an ID without prefix.
        Integer enumValue = resolveEnumAttribute(index);
        if (enumValue != null) {
            return enumValue;
        }

        return defValue;
    }

    @Override
    public int getThemeAttributeId(int index, int defValue) {
        // TODO: Get the right Theme Attribute ID to enable caching of the drawables.
        return defValue;
    }

    /**
     * Retrieve the Drawable for the attribute at <var>index</var>.  This
     * gets the resource ID of the selected attribute, and uses
     * {@link Resources#getDrawable Resources.getDrawable} of the owning
     * Resources object to retrieve its Drawable.
     *
     * @param index Index of attribute to retrieve.
     *
     * @return Drawable for the attribute, or null if not defined.
     */
    @Override
    @Nullable
    public Drawable getDrawable(int index) {
        if (!hasValue(index)) {
            return null;
        }

        ResourceValue value = mResourceData[index];
        return ResourceHelper.getDrawable(value, mContext, mTheme);
    }

    /**
     * Version of {@link #getDrawable(int)} that accepts an override density.
     * @hide
     */
    @Override
    @Nullable
    public Drawable getDrawableForDensity(int index, int density) {
        return getDrawable(index);
    }

    /**
     * Retrieve the Typeface for the attribute at <var>index</var>.
     * @param index Index of attribute to retrieve.
     *
     * @return Typeface for the attribute, or null if not defined.
     */
    @Override
    public Typeface getFont(int index) {
        if (!hasValue(index)) {
            return null;
        }

        ResourceValue value = mResourceData[index];
        return ResourceHelper.getFont(value, mContext, mTheme);
    }

    /**
     * Retrieve the CharSequence[] for the attribute at <var>index</var>.
     * This gets the resource ID of the selected attribute, and uses
     * {@link Resources#getTextArray Resources.getTextArray} of the owning
     * Resources object to retrieve its String[].
     *
     * @param index Index of attribute to retrieve.
     *
     * @return CharSequence[] for the attribute, or null if not defined.
     */
    @Override
    public CharSequence[] getTextArray(int index) {
        if (!hasValue(index)) {
            return null;
        }
        ResourceValue resVal = mResourceData[index];
        if (resVal instanceof ArrayResourceValue) {
            ArrayResourceValue array = (ArrayResourceValue) resVal;
            int count = array.getElementCount();
            return count >= 0 ?
                    Resources_Delegate.resolveValues(mBridgeResources, array) :
                    null;
        }
        int id = getResourceId(index, 0);
        String resIdMessage = id > 0 ? " (resource id 0x" + Integer.toHexString(id) + ')' : "";
        assert false :
                String.format("%1$s in %2$s%3$s is not a valid array resource.", resVal.getValue(),
                        mNames[index], resIdMessage);

        return new CharSequence[0];
    }

    @Override
    public int[] extractThemeAttrs() {
        // The drawables are always inflated with a Theme and we don't care about caching. So,
        // just return.
        return null;
    }

    @Override
    public int getChangingConfigurations() {
        // We don't care about caching. Any change in configuration is a fresh render. So,
        // just return.
        return 0;
    }

    /**
     * Retrieve the raw TypedValue for the attribute at <var>index</var>.
     *
     * @param index Index of attribute to retrieve.
     * @param outValue TypedValue object in which to place the attribute's
     *                 data.
     *
     * @return Returns true if the value was retrieved, else false.
     */
    @Override
    public boolean getValue(int index, TypedValue outValue) {
        // TODO: more switch cases for other types.
        outValue.type = getType(index);
        switch (outValue.type) {
            case TYPE_NULL:
                return false;
            case TYPE_STRING:
                outValue.string = getString(index);
                return true;
            case TYPE_REFERENCE:
                outValue.resourceId = mResourceId[index];
                return true;
            case TYPE_INT_COLOR_ARGB4:
            case TYPE_INT_COLOR_ARGB8:
            case TYPE_INT_COLOR_RGB4:
            case TYPE_INT_COLOR_RGB8:
                ColorStateList colorStateList = getColorStateList(index);
                if (colorStateList == null) {
                    return false;
                }
                outValue.data = colorStateList.getDefaultColor();
                return true;
            default:
                // For back-compatibility, parse as float.
                String s = getString(index);
                return s != null &&
                        ResourceHelper.parseFloatAttribute(mNames[index], s, outValue, false);
        }
    }

    @Override
    public int getType(int index) {
        String value = getString(index);
        return getType(value);
    }

    /**
     * Determines whether there is an attribute at <var>index</var>.
     *
     * @param index Index of attribute to retrieve.
     *
     * @return True if the attribute has a value, false otherwise.
     */
    @Override
    public boolean hasValue(int index) {
        return index >= 0 && index < mResourceData.length && mResourceData[index] != null;
    }

    @Override
    public boolean hasValueOrEmpty(int index) {
        return hasValue(index) || index >= 0 && index < mResourceData.length &&
                mEmptyIds != null && Arrays.binarySearch(mEmptyIds, index) >= 0;
    }

    /**
     * Retrieve the raw TypedValue for the attribute at <var>index</var>
     * and return a temporary object holding its data.  This object is only
     * valid until the next call on to {@link TypedArray}.
     *
     * @param index Index of attribute to retrieve.
     *
     * @return Returns a TypedValue object if the attribute is defined,
     *         containing its data; otherwise returns null.  (You will not
     *         receive a TypedValue whose type is TYPE_NULL.)
     */
    @Override
    public TypedValue peekValue(int index) {
        if (index < 0 || index >= mResourceData.length) {
            return null;
        }

        if (getValue(index, mValue)) {
            return mValue;
        }

        return null;
    }

    /**
     * Returns a message about the parser state suitable for printing error messages.
     */
    @Override
    public String getPositionDescription() {
        return "<internal -- stub if needed>";
    }

    /**
     * Give back a previously retrieved TypedArray, for later re-use.
     */
    @Override
    public void recycle() {
        // pass
    }

    @Override
    public String toString() {
        return Arrays.toString(mResourceData);
    }

    /**
     * Searches for the string in the attributes (flag or enums) and returns the integer.
     * If found, it will return an integer matching the value.
     *
     * @param index Index of attribute to retrieve.
     *
     * @return Attribute int value, or null if not defined.
     */
    private Integer resolveEnumAttribute(int index) {
        // Get the map of attribute-constant -> IntegerValue
        Map<String, Integer> map = null;
        if (mNamespaces[index] == ResourceNamespace.ANDROID) {
            map = Bridge.getEnumValues(mNames[index]);
        } else {
            // get the styleable matching the resolved name
            RenderResources res = mContext.getRenderResources();
            ResourceValue attr = res.getResolvedResource(
                    ResourceReference.attr(mNamespaces[index], mNames[index]));
            if (attr instanceof AttrResourceValue) {
                map = ((AttrResourceValue) attr).getAttributeValues();
            }
        }

        if (map != null && !map.isEmpty()) {
            // Accumulator to store the value of the 1+ constants.
            int result = 0;
            boolean found = false;

            String value = mResourceData[index].getValue();
            if (!value.isEmpty()) {
                // Check if the value string is already representing an integer and return it if so.
                // Resources coming from res.apk in an AAR may have flags and enums in integer form.
                char c = value.charAt(0);
                if (Character.isDigit(c) || c == '-' || c == '+') {
                    try {
                        return convertValueToInt(value, 0);
                    } catch (NumberFormatException e) {
                        // Ignore and continue.
                    }
                }
                // Split the value in case it is a mix of several flags.
                String[] keywords = value.split("\\|");
                for (String keyword : keywords) {
                    Integer i = map.get(keyword.trim());
                    if (i != null) {
                        result |= i;
                        found = true;
                    }
                    // TODO: We should act smartly and log a warning for incorrect keywords. However,
                    // this method is currently called even if the resourceValue is not an enum.
                }
                if (found) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Copied from {@link XmlUtils#convertValueToInt(CharSequence, int)}, but adapted to account
     * for aapt, and the fact that host Java VM's Integer.parseInt("XXXXXXXX", 16) cannot handle
     * "XXXXXXXX" > 80000000.
     */
    private static int convertValueToInt(@Nullable String charSeq, int defValue) {
        if (null == charSeq || charSeq.isEmpty())
            return defValue;

        int sign = 1;
        int index = 0;
        int len = charSeq.length();
        int base = 10;

        if ('-' == charSeq.charAt(0)) {
            sign = -1;
            index++;
        }

        if ('0' == charSeq.charAt(index)) {
            //  Quick check for a zero by itself
            if (index == (len - 1))
                return 0;

            char c = charSeq.charAt(index + 1);

            if ('x' == c || 'X' == c) {
                index += 2;
                base = 16;
            } else {
                index++;
                // Leave the base as 10. aapt removes the preceding zero, and thus when framework
                // sees the value, it only gets the decimal value.
            }
        } else if ('#' == charSeq.charAt(index)) {
            return ResourceHelper.getColor(charSeq) * sign;
        } else if ("true".equals(charSeq) || "TRUE".equals(charSeq)) {
            return -1;
        } else if ("false".equals(charSeq) || "FALSE".equals(charSeq)) {
            return 0;
        }

        // Use Long, since we want to handle hex ints > 80000000.
        return ((int)Long.parseLong(charSeq.substring(index), base)) * sign;
    }

    protected static int getType(@Nullable String value) {
        if (value == null) {
            return TYPE_NULL;
        }
        if (value.startsWith(PREFIX_RESOURCE_REF)) {
            return TYPE_REFERENCE;
        }
        if (value.startsWith(PREFIX_THEME_REF)) {
            return TYPE_ATTRIBUTE;
        }
        if (value.equals("true") || value.equals("false")) {
            return TYPE_INT_BOOLEAN;
        }
        if (value.startsWith("0x") || value.startsWith("0X")) {
            try {
                // Check if it is a hex value.
                Long.parseLong(value.substring(2), 16);
                return TYPE_INT_HEX;
            } catch (NumberFormatException e) {
                return TYPE_STRING;
            }
        }
        if (value.startsWith("#")) {
            try {
                // Check if it is a color.
                ResourceHelper.getColor(value);
                int length = value.length() - 1;
                if (length == 3) {  // rgb
                    return TYPE_INT_COLOR_RGB4;
                }
                if (length == 4) {  // argb
                    return TYPE_INT_COLOR_ARGB4;
                }
                if (length == 6) {  // rrggbb
                    return TYPE_INT_COLOR_RGB8;
                }
                if (length == 8) {  // aarrggbb
                    return TYPE_INT_COLOR_ARGB8;
                }
            } catch (NumberFormatException e) {
                return TYPE_STRING;
            }
        }
        if (!Character.isDigit(value.charAt(value.length() - 1))) {
            // Check if it is a dimension.
            if (ResourceHelper.parseFloatAttribute(null, value, new TypedValue(), false)) {
                return TYPE_DIMENSION;
            } else {
                return TYPE_STRING;
            }
        }
        try {
            // Check if it is an int.
            convertValueToInt(value, 0);
            return TYPE_INT_DEC;
        } catch (NumberFormatException ignored) {
            try {
                // Check if it is a float.
                Float.parseFloat(value);
                return TYPE_FLOAT;
            } catch (NumberFormatException ignore) {
            }
        }
        // TODO: handle fractions.
        return TYPE_STRING;
    }

    static TypedArray obtain(Resources res, int len) {
        return new BridgeTypedArray(res, null, len);
    }
}
