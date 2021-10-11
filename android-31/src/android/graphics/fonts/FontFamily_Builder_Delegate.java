/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.graphics.fonts;

import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.layoutlib.bridge.Bridge;
import com.android.layoutlib.bridge.impl.DelegateManager;
import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.FontFamily_Delegate.FontInfo;
import android.graphics.FontFamily_Delegate.FontVariant;
import android.graphics.Paint;

import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import libcore.util.NativeAllocationRegistry_Delegate;

import static android.graphics.FontFamily_Delegate.computeMatch;
import static android.graphics.FontFamily_Delegate.deriveFont;

/**
 * Delegate implementing the native methods of android.graphics.fonts.FontFamily$Builder
 * <p>
 * Through the layoutlib_create tool, the original native methods of FontFamily$Builder have been
 * replaced by calls to methods of the same name in this delegate class.
 * <p>
 * This class behaves like the original native implementation, but in Java, keeping previously
 * native data into its own objects and mapping them to int that are sent back and forth between it
 * and the original FontFamily$Builder class.
 *
 * @see DelegateManager
 */
public class FontFamily_Builder_Delegate {
    private static final DelegateManager<FontFamily_Builder_Delegate> sBuilderManager =
            new DelegateManager<>(FontFamily_Builder_Delegate.class);

    private static long sFontFamilyFinalizer = -1;

    // Order does not really matter but we use a LinkedHashMap to get reproducible results across
    // render calls
    private Map<FontInfo, Font> mFonts = new LinkedHashMap<>();
    /**
     * The variant of the Font Family - compact or elegant.
     * <p/>
     * 0 is unspecified, 1 is compact and 2 is elegant. This needs to be kept in sync with values in
     * android.graphics.FontFamily
     *
     * @see Paint#setElegantTextHeight(boolean)
     */
    private FontVariant mVariant;
    private boolean mIsCustomFallback;

    @LayoutlibDelegate
    /*package*/ static long nInitBuilder() {
        return sBuilderManager.addNewDelegate(new FontFamily_Builder_Delegate());
    }

    @LayoutlibDelegate
    /*package*/ static void nAddFont(long builderPtr, long fontPtr) {
        FontFamily_Builder_Delegate familyBuilder = sBuilderManager.getDelegate(builderPtr);
        Font_Builder_Delegate fontBuilder = Font_Builder_Delegate.sBuilderManager.getDelegate(fontPtr);
        if (familyBuilder == null || fontBuilder == null) {
            return;
        }
        Font font;
        if (fontBuilder.filePath.equals("")) {
            font = loadFontBuffer(fontBuilder.mBuffer);

        } else {
            font = loadFontPath(fontBuilder.filePath);
        }
        if (font != null) {
            familyBuilder.addFont(font, fontBuilder.mWeight, fontBuilder.mItalic);
        }
    }

    @LayoutlibDelegate
    /*package*/ static long nBuild(long builderPtr, String langTags, int variant,
            boolean isCustomFallback) {
        FontFamily_Builder_Delegate builder = sBuilderManager.getDelegate(builderPtr);
        if (builder != null) {
            assert variant < 3;
            builder.mVariant = FontVariant.values()[variant];
            builder.mIsCustomFallback = isCustomFallback;
        }
        return builderPtr;
    }

    @LayoutlibDelegate
    /*package*/ static long nGetReleaseNativeFamily() {
        synchronized (Font_Builder_Delegate.class) {
            if (sFontFamilyFinalizer == -1) {
                sFontFamilyFinalizer = NativeAllocationRegistry_Delegate.createFinalizer(
                        sBuilderManager::removeJavaReferenceFor);
            }
        }
        return sFontFamilyFinalizer;
    }

    public static FontFamily_Builder_Delegate getDelegate(long nativeFontFamily) {
        return sBuilderManager.getDelegate(nativeFontFamily);
    }

    @Nullable
    public Font getFont(int desiredWeight, boolean isItalic) {
        FontInfo desiredStyle = new FontInfo();
        desiredStyle.mWeight = desiredWeight;
        desiredStyle.mIsItalic = isItalic;

        Font cachedFont = mFonts.get(desiredStyle);
        if (cachedFont != null) {
            return cachedFont;
        }

        FontInfo bestFont = null;

        if (mFonts.size() == 1) {
            // No need to compute the match since we only have one candidate
            bestFont = mFonts.keySet().iterator().next();
        } else {
            int bestMatch = Integer.MAX_VALUE;

            for (FontInfo font : mFonts.keySet()) {
                int match = computeMatch(font, desiredStyle);
                if (match < bestMatch) {
                    bestMatch = match;
                    bestFont = font;
                    if (bestMatch == 0) {
                        break;
                    }
                }
            }
        }

        if (bestFont == null) {
            return null;
        }


        // Derive the font as required and add it to the list of Fonts.
        deriveFont(bestFont, desiredStyle);
        addFont(desiredStyle);
        return desiredStyle.mFont;
    }

    public FontVariant getVariant() {
        return mVariant;
    }

    // ---- private helper methods ----

    private void addFont(@NonNull Font font, int weight, boolean italic) {
        FontInfo fontInfo = new FontInfo();
        fontInfo.mFont = font;
        fontInfo.mWeight = weight;
        fontInfo.mIsItalic = italic;
        addFont(fontInfo);
    }

    private void addFont(@NonNull FontInfo fontInfo) {
        mFonts.putIfAbsent(fontInfo, fontInfo.mFont);
    }

    private static Font loadFontBuffer(@NonNull ByteBuffer buffer) {
        try {
            byte[] byteArray = new byte[buffer.limit()];
            buffer.rewind();
            buffer.get(byteArray);
            return Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(byteArray));
        } catch (Exception e) {
            Bridge.getLog().fidelityWarning(ILayoutLog.TAG_BROKEN, "Unable to load font",
                    e, null, null);
        }

        return null;
    }

    private static Font loadFontPath(@NonNull String path) {
        try {
            File file = new File(path);
            return Font.createFont(Font.TRUETYPE_FONT, file);
        } catch (Exception e) {
            Bridge.getLog().fidelityWarning(ILayoutLog.TAG_BROKEN, "Unable to load font",
                    e, null, null);
        }

        return null;
    }
}
