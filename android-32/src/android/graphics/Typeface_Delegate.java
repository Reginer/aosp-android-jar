/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.graphics;

import com.android.SdkConstants;
import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.layoutlib.bridge.Bridge;
import com.android.layoutlib.bridge.android.BridgeContext;
import com.android.layoutlib.bridge.android.BridgeXmlBlockParser;
import com.android.layoutlib.bridge.android.RenderParamsFlags;
import com.android.layoutlib.bridge.impl.DelegateManager;
import com.android.layoutlib.bridge.impl.RenderAction;
import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.res.FontResourcesParser;
import android.graphics.FontFamily_Delegate.FontVariant;
import android.graphics.fonts.FontFamily_Builder_Delegate;
import android.graphics.fonts.FontVariationAxis;

import java.awt.Font;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;

import libcore.util.NativeAllocationRegistry_Delegate;

/**
 * Delegate implementing the native methods of android.graphics.Typeface
 * <p>
 * Through the layoutlib_create tool, the original native methods of Typeface have been replaced by
 * calls to methods of the same name in this delegate class.
 * <p>
 * This class behaves like the original native implementation, but in Java, keeping previously
 * native data into its own objects and mapping them to int that are sent back and forth between it
 * and the original Typeface class.
 *
 * @see DelegateManager
 */
public final class Typeface_Delegate {

    public static final String SYSTEM_FONTS = "/system/fonts/";

    public static final Map<String, FontFamily_Delegate[]> sGenericNativeFamilies = new HashMap<>();

    // ---- delegate manager ----
    private static final DelegateManager<Typeface_Delegate> sManager =
            new DelegateManager<>(Typeface_Delegate.class);
    private static long sFinalizer = -1;

    // ---- delegate data ----
    private static long sDefaultTypeface;
    @NonNull
    private final FontFamily_Delegate[] mFontFamilies;  // the reference to FontFamily_Delegate.
    @NonNull
    private final FontFamily_Builder_Delegate[] mFontFamilyBuilders;  // the reference to
    // FontFamily_Builder_Delegate.
    /** @see Font#getStyle() */
    private final int mStyle;
    private final int mWeight;


    // ---- Public Helper methods ----

    private Typeface_Delegate(@NonNull FontFamily_Delegate[] fontFamilies,
            @NonNull FontFamily_Builder_Delegate[] fontFamilyBuilders, int style,
            int weight) {
        mFontFamilies = fontFamilies;
        mFontFamilyBuilders = fontFamilyBuilders;
        mStyle = style;
        mWeight = weight;
    }

    public static Typeface_Delegate getDelegate(long nativeTypeface) {
        return sManager.getDelegate(nativeTypeface);
    }

    // ---- native methods ----

    @LayoutlibDelegate
    /*package*/ static synchronized long nativeCreateFromTypeface(long native_instance, int style) {
        Typeface_Delegate delegate = sManager.getDelegate(native_instance);
        if (delegate == null) {
            delegate = sManager.getDelegate(sDefaultTypeface);
        }
        if (delegate == null) {
            return 0;
        }

        return sManager.addNewDelegate(
                new Typeface_Delegate(delegate.mFontFamilies, delegate.mFontFamilyBuilders, style,
                        delegate.mWeight));
    }

    @LayoutlibDelegate
    /*package*/ static long nativeCreateFromTypefaceWithExactStyle(long native_instance, int weight,
            boolean italic) {
        Typeface_Delegate delegate = sManager.getDelegate(native_instance);
        if (delegate == null) {
            delegate = sManager.getDelegate(sDefaultTypeface);
        }
        if (delegate == null) {
            return 0;
        }

        int style = weight >= 600 ? (italic ? Typeface.BOLD_ITALIC : Typeface.BOLD) :
                (italic ? Typeface.ITALIC : Typeface.NORMAL);
        return sManager.addNewDelegate(
                new Typeface_Delegate(delegate.mFontFamilies, delegate.mFontFamilyBuilders, style,
                        weight));
    }

    @LayoutlibDelegate
    /*package*/ static synchronized long nativeCreateFromTypefaceWithVariation(long native_instance,
            List<FontVariationAxis> axes) {
        long newInstance = nativeCreateFromTypeface(native_instance, 0);

        if (newInstance != 0) {
            Bridge.getLog().fidelityWarning(ILayoutLog.TAG_UNSUPPORTED,
                    "nativeCreateFromTypefaceWithVariation is not supported", null, null, null);
        }
        return newInstance;
    }

    @LayoutlibDelegate
    /*package*/ static synchronized int[] nativeGetSupportedAxes(long native_instance) {
        // nativeCreateFromTypefaceWithVariation is not supported so we do not keep the axes
        return null;
    }

    @LayoutlibDelegate
    /*package*/ static long nativeCreateWeightAlias(long native_instance, int weight) {
        Typeface_Delegate delegate = sManager.getDelegate(native_instance);
        if (delegate == null) {
            delegate = sManager.getDelegate(sDefaultTypeface);
        }
        if (delegate == null) {
            return 0;
        }
        Typeface_Delegate weightAlias =
                new Typeface_Delegate(delegate.mFontFamilies, delegate.mFontFamilyBuilders,
                        delegate.mStyle,
                        weight);
        return sManager.addNewDelegate(weightAlias);
    }

    @LayoutlibDelegate
    /*package*/ static synchronized long nativeCreateFromArray(long[] familyArray, int weight,
            int italic) {
        List<FontFamily_Delegate> fontFamilies = new ArrayList<>();
        List<FontFamily_Builder_Delegate> fontFamilyBuilders = new ArrayList<>();
        for (long aFamilyArray : familyArray) {
            try {
                fontFamilies.add(FontFamily_Delegate.getDelegate(aFamilyArray));
            } catch (ClassCastException e) {
                fontFamilyBuilders.add(FontFamily_Builder_Delegate.getDelegate(aFamilyArray));
            }
        }
        if (weight == Typeface.RESOLVE_BY_FONT_TABLE) {
            weight = 400;
        }
        if (italic == Typeface.RESOLVE_BY_FONT_TABLE) {
            italic = 0;
        }
        int style = weight >= 600 ? (italic == 1 ? Typeface.BOLD_ITALIC : Typeface.BOLD) :
                (italic == 1 ? Typeface.ITALIC : Typeface.NORMAL);
        Typeface_Delegate delegate =
                new Typeface_Delegate(fontFamilies.toArray(new FontFamily_Delegate[0]),
                fontFamilyBuilders.toArray(new FontFamily_Builder_Delegate[0]),
                style, weight);
        return sManager.addNewDelegate(delegate);
    }

    @LayoutlibDelegate
    /*package*/ static long nativeGetReleaseFunc() {
        synchronized (Typeface_Delegate.class) {
            if (sFinalizer == -1) {
                sFinalizer = NativeAllocationRegistry_Delegate.createFinalizer(
                        sManager::removeJavaReferenceFor);
            }
        }
        return sFinalizer;
    }

    @LayoutlibDelegate
    /*package*/ static int nativeGetStyle(long native_instance) {
        Typeface_Delegate delegate = sManager.getDelegate(native_instance);
        if (delegate == null) {
            return 0;
        }

        return delegate.mStyle;
    }

    @LayoutlibDelegate
    /*package*/ static void nativeSetDefault(long native_instance) {
        sDefaultTypeface = native_instance;
    }

    @LayoutlibDelegate
    /*package*/ static int nativeGetWeight(long native_instance) {
        Typeface_Delegate delegate = sManager.getDelegate(native_instance);
        if (delegate == null) {
            return 0;
        }
        return delegate.mWeight;
    }

    /**
     * Loads a single font or font family from disk
     */
    @Nullable
    public static Typeface createFromDisk(@NonNull BridgeContext context, @NonNull String path,
            boolean isFramework) {
        // Check if this is an asset that we've already loaded dynamically
        Typeface typeface = Typeface.findFromCache(context.getAssets(), path);
        if (typeface != null) {
            return typeface;
        }

        String lowerCaseValue = path.toLowerCase();
        if (lowerCaseValue.endsWith(SdkConstants.DOT_XML)) {
            // create a block parser for the file
            Boolean psiParserSupport = context.getLayoutlibCallback().getFlag(
                    RenderParamsFlags.FLAG_KEY_XML_FILE_PARSER_SUPPORT);
            XmlPullParser parser;
            if (psiParserSupport != null && psiParserSupport) {
                parser = context.getLayoutlibCallback().createXmlParserForPsiFile(path);
            } else {
                parser = context.getLayoutlibCallback().createXmlParserForFile(path);
            }

            if (parser != null) {
                // TODO(b/156609434): The aapt namespace should not matter for parsing font files?
                BridgeXmlBlockParser blockParser =
                        new BridgeXmlBlockParser(
                                parser, context, ResourceNamespace.fromBoolean(isFramework));
                try {
                    FontResourcesParser.FamilyResourceEntry entry =
                            FontResourcesParser.parse(blockParser, context.getResources());
                    typeface = Typeface.createFromResources(entry, context.getAssets(), path);
                } catch (XmlPullParserException | IOException e) {
                    Bridge.getLog().error(null, "Failed to parse file " + path, e, null, null /*data
                    */);
                } finally {
                    blockParser.ensurePopped();
                }
            } else {
                Bridge.getLog().error(ILayoutLog.TAG_BROKEN,
                        String.format("File %s does not exist (or is not a file)", path),
                        null, null /*data*/);
            }
        } else {
            typeface = new Typeface.Builder(context.getAssets(), path, false, 0).build();
        }

        return typeface;
    }

    @LayoutlibDelegate
    /*package*/ static Typeface create(String familyName, int style) {
        if (familyName != null && Files.exists(Paths.get(familyName))) {
            // Workaround for b/64137851
            // Support lib will call this method after failing to create the TypefaceCompat.
            return Typeface_Delegate.createFromDisk(RenderAction.getCurrentContext(), familyName,
                    false);
        }
        return Typeface.create_Original(familyName, style);
    }

    @LayoutlibDelegate
    /*package*/ static Typeface create(Typeface family, int style) {
        return Typeface.create_Original(family, style);
    }

    @LayoutlibDelegate
    /*package*/ static Typeface create(Typeface family, int style, boolean isItalic) {
        return Typeface.create_Original(family, style, isItalic);
    }

    @LayoutlibDelegate
    /*package*/ static void nativeRegisterGenericFamily(String str, long nativePtr) {
        Typeface_Delegate delegate = sManager.getDelegate(nativePtr);
        if (delegate == null) {
            return;
        }
        sGenericNativeFamilies.put(str, delegate.mFontFamilies);
    }

    // ---- Private delegate/helper methods ----

    /**
     * Return an Iterable of fonts that match the style and variant. The list is ordered
     * according to preference of fonts.
     * <p>
     * The Iterator may contain null when the font failed to load. If null is reached when trying to
     * render with this list of fonts, then a warning should be logged letting the user know that
     * some font failed to load.
     *
     * @param variant The variant preferred. Can only be {@link FontVariant#COMPACT} or {@link
     * FontVariant#ELEGANT}
     */
    @NonNull
    public Iterable<Font> getFonts(final FontVariant variant) {
        assert variant != FontVariant.NONE;

        return new FontsIterator(mFontFamilies, mFontFamilyBuilders, variant, mWeight, mStyle);
    }

    private static class FontsIterator implements Iterator<Font>, Iterable<Font> {
        private final FontFamily_Delegate[] fontFamilies;
        private final FontFamily_Builder_Delegate[] fontFamilyBuilders;
        private final int weight;
        private final boolean isItalic;
        private final FontVariant variant;

        private int index = 0;

        private FontsIterator(@NonNull FontFamily_Delegate[] fontFamilies,
                @NonNull FontFamily_Builder_Delegate[] fontFamilyBuilders,
                @NonNull FontVariant variant, int weight, int style) {
            // Calculate the required weight based on style and weight of this typeface.
            int boldExtraWeight =
                    ((style & Font.BOLD) == 0 ? 0 : FontFamily_Delegate.BOLD_FONT_WEIGHT_DELTA);
            this.weight = Math.min(Math.max(100, weight + 50 + boldExtraWeight), 1000);
            this.isItalic = (style & Font.ITALIC) != 0;
            this.fontFamilies = fontFamilies;
            this.fontFamilyBuilders = fontFamilyBuilders;
            this.variant = variant;
        }

        @Override
        public boolean hasNext() {
            return index < (fontFamilies.length + fontFamilyBuilders.length);
        }

        @Override
        @Nullable
        public Font next() {
            Font font;
            FontVariant ffdVariant;
            if (index < fontFamilies.length) {
                FontFamily_Delegate ffd = fontFamilies[index++];
                if (ffd == null || !ffd.isValid()) {
                    return null;
                }
                font = ffd.getFont(weight, isItalic);
                ffdVariant = ffd.getVariant();
            } else {
                FontFamily_Builder_Delegate ffd = fontFamilyBuilders[index++ - fontFamilies.length];
                if (ffd == null) {
                    return null;
                }
                font = ffd.getFont(weight, isItalic);
                ffdVariant = ffd.getVariant();
            }

            if (font == null) {
                // The FontFamily is valid but doesn't contain any matching font. This means
                // that the font failed to load. We add null to the list of fonts. Don't throw
                // the warning just yet. If this is a non-english font, we don't want to warn
                // users who are trying to render only english text.
                return null;
            }

            if (ffdVariant == FontVariant.NONE || ffdVariant == variant) {
                return font;
            }

            // We cannot open each font and get locales supported, etc to match the fonts.
            // As a workaround, we hardcode certain assumptions like Elegant and Compact
            // always appear in pairs.
            if (index < fontFamilies.length) {
                assert index < fontFamilies.length - 1;
                FontFamily_Delegate ffd2 = fontFamilies[index++];
                assert ffd2 != null;

                return ffd2.getFont(weight, isItalic);
            } else {
                assert index < fontFamilies.length + fontFamilyBuilders.length - 1;
                FontFamily_Builder_Delegate ffd2 = fontFamilyBuilders[index++ - fontFamilies.length];
                assert ffd2 != null;

                return ffd2.getFont(weight, isItalic);
            }
        }

        @NonNull
        @Override
        public Iterator<Font> iterator() {
            return this;
        }

        @Override
        public Spliterator<Font> spliterator() {
            return Spliterators.spliterator(iterator(),
                    fontFamilies.length + fontFamilyBuilders.length,
                    Spliterator.IMMUTABLE | Spliterator.SIZED);
        }
    }
}
