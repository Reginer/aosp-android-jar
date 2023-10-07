/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.clockwork.displayoffload;

import static com.android.clockwork.displayoffload.DebugUtils.DEBUG_FONT_DUMP;
import static com.android.clockwork.displayoffload.DebugUtils.DEBUG_FONT_SUBSETTING;
import static com.android.clockwork.displayoffload.DebugUtils.dumpAsFile;
import static com.android.clockwork.displayoffload.Utils.convertToArrayListByte;
import static com.android.clockwork.displayoffload.Utils.convertToIntArray;

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_CONVERSION_FAILURE;

import android.annotation.NonNull;
import android.content.Context;
import android.os.SharedMemory;
import android.system.ErrnoException;
import android.util.ArrayMap;
import android.util.Log;

import com.android.clockwork.displayoffload.HalTypeConverter.HalTypeConverterSupplier;
import com.android.internal.annotations.VisibleForTesting;

import com.google.android.clockwork.ambient.offload.types.DynamicTextResource;
import com.google.android.clockwork.ambient.offload.types.StaticTextResource;
import com.google.android.clockwork.ambient.offload.types.TtfFontResource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// TODO(b/238262154): possible refactor for "add" methods.
/** Helper Class for processing text related resources. */
public class TextPreprocessor {
    private static final String TAG = "DOTextPreprocessor";

    private final Map<Integer, SharedMemory> mTtfFontCache = new ArrayMap<>();
    private final Map<Integer, ByteBuffer> mTtfFontByteBufferCache = new ArrayMap<>();
    private final Map<Integer, TtfReservationInfo> mTtfSubsetCache = new ArrayMap<>();
    private final SystemFontHelper mSystemFontHelper;
    private final ITextPreprocessorNative mTextPreprocessorNative;
    private final Context mContext;
    private final HalTypeConverterSupplier mHalTypeConverter;

    public TextPreprocessor(Context context, HalTypeConverterSupplier halTypeConverter) {
        this(context, halTypeConverter, new TextPreprocessorNative(), new SystemFontHelper());
    }

    @VisibleForTesting
    TextPreprocessor(Context context, HalTypeConverterSupplier halTypeConverter,
            ITextPreprocessorNative nativeHelper, SystemFontHelper systemFontHelper) {
        this.mContext = context;
        this.mHalTypeConverter = halTypeConverter;
        this.mTextPreprocessorNative = nativeHelper;
        this.mSystemFontHelper = systemFontHelper;
    }


    public void addStaticText(HalResourceStore halResourceStore, List<StaticTextResource> texts)
            throws DisplayOffloadException {
        for (StaticTextResource text : texts) {
            addStaticText(halResourceStore, text);
        }
    }

    @VisibleForTesting
    void addStaticText(HalResourceStore halResourceStore, StaticTextResource text)
            throws DisplayOffloadException {
        List<ResourceObject> halStaticTextResources =
                mHalTypeConverter.getConverter().toHalStaticText(text);
        if (halStaticTextResources == null || halStaticTextResources.isEmpty()) {
            return;
        }

        // Convention of HalTypeConverter, always put the "main" ResourceObject at the end of the
        // list.
        Object staticTextAdapterObj = halStaticTextResources
                .get(halStaticTextResources.size() - 1).getObject();
        if (!(staticTextAdapterObj instanceof StaticTextAdapter)) {
            throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE);
        }

        StaticTextAdapter staticTextAdapter = (StaticTextAdapter) staticTextAdapterObj;
        final int textFontId = staticTextAdapter.getFontId();

        if (!mTtfFontByteBufferCache.containsKey(textFontId)) {
            loadTtfByteBuffer(textFontId);
        }
        shapeTextIfPossible(staticTextAdapter, text.textParam.useTabularNum);

        halResourceStore.addReplaceResource(halStaticTextResources);
    }

    public void addDynamicText(HalResourceStore halResourceStore, List<DynamicTextResource> texts)
            throws DisplayOffloadException {
        for (DynamicTextResource text : texts) {
            addDynamicText(halResourceStore, text);
        }
    }

    @VisibleForTesting
    void addDynamicText(HalResourceStore halResourceStore, DynamicTextResource text)
            throws DisplayOffloadException {
        List<ResourceObject> halDynamicTextResources = mHalTypeConverter.getConverter()
                .toHalObject(text);
        if (halDynamicTextResources == null || halDynamicTextResources.isEmpty()) {
            return;
        }
        halResourceStore.addReplaceResource(halDynamicTextResources);

        Object dynamicTextAdapterObj = halDynamicTextResources
                .get(halDynamicTextResources.size() - 1).getObject();
        if (!(dynamicTextAdapterObj instanceof DynamicTextAdapter)) {
            throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE);
        }

        DynamicTextAdapter dynamicTextAdapter = (DynamicTextAdapter) dynamicTextAdapterObj;
        final int textFontId = dynamicTextAdapter.getFontId();

        if (!mTtfFontByteBufferCache.containsKey(textFontId)) {
            loadTtfByteBuffer(textFontId);
        }
        if (mTtfSubsetCache.containsKey(textFontId)) {
            mTtfSubsetCache.get(textFontId).numeric = true;
        } else {
            mTtfSubsetCache.put(textFontId, new TtfReservationInfo(true));
        }
    }

    public void addFont(HalResourceStore halResourceStore, List<TtfFontResource> fonts)
            throws DisplayOffloadException {
        for (TtfFontResource font : fonts) {
            addFont(halResourceStore, font);
        }
    }

    public void addFont(HalResourceStore halResourceStore, TtfFontResource font)
            throws DisplayOffloadException {
        List<ResourceObject> fontResources = mHalTypeConverter.getConverter().toHalTtfFontResource(
                font);
        if (fontResources == null || fontResources.isEmpty()) {
            return;
        }
        halResourceStore.addReplaceResource(fontResources);

        Object ttfFontAdapterObj = fontResources.get(fontResources.size() - 1).getObject();
        if (!(ttfFontAdapterObj instanceof TtfFontAdapter)) {
            throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE);
        }
        TtfFontAdapter ttfFontAdapter = (TtfFontAdapter) ttfFontAdapterObj;
        final int fontId = ttfFontAdapter.getId();

        if (font.fontPath != null) {
            // Load system font
            ByteBuffer buffer = mSystemFontHelper.mapFont(font.fontPath);
            mSystemFontHelper.setFontIndexForResourceId(fontId, font.fontIndex);
            if (buffer == null) {
                throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                        "Unable to open font at path: " + font.fontPath);
            }
            mTtfFontByteBufferCache.put(fontId, buffer);
        } else if (font.ttfMemory != null) {
            // App-bundled font
            mTtfFontCache.put(fontId, font.ttfMemory);
        } else {
            throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                    "Invalid ttf font id=" + fontId);
        }
    }

    public void processTtfFontSubsetting(HalResourceStore halResourceStore)
            throws DisplayOffloadException {
        int[] numericCodepoints = LocaleHelper.getCurrentLocaleNumericCodepoints(mContext);
        int[] empty = new int[0];
        for (Integer ttfId : mTtfSubsetCache.keySet()) {
            // Type check
            Object ttfFontAdapterObject = halResourceStore.get(ttfId);
            if (!(ttfFontAdapterObject instanceof TtfFontAdapter)) {
                throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                        "Invalid ttf font id=" + ttfId);
            }
            TtfFontAdapter ttfFontAdapter = (TtfFontAdapter) ttfFontAdapterObject;

            TtfReservationInfo reservation = mTtfSubsetCache.get(ttfId);
            int[] glyphIdsToReserve = reservation.getGlyphIdsAsArray();
            int[] glyphIdsAfterSubset = glyphIdsToReserve.clone();

            if (DEBUG_FONT_SUBSETTING) {
                Log.d(TAG, "TTF subsetting: full TTF bytes size is " + mTtfFontByteBufferCache.get(
                        ttfId).capacity());
                Log.i(TAG, "TTF subsetting: reservation for " + ttfId + " has "
                        + reservation.glyphIdMappings.size() + " glyphs" + ", numeric="
                        + reservation.numeric);
                Log.d(TAG,
                        "TTF subsetting: glyphIdsToReserve: " + Arrays.toString(glyphIdsToReserve));
            }

            ByteBuffer fontByteBuffer = mTtfFontByteBufferCache.get(ttfId);
            if (fontByteBuffer == null) {
                Log.e(TAG, "TTF subset failed due to missing byte buffer for " + ttfId);
                continue;
            }
            byte[] subsetted = mTextPreprocessorNative.subsetTtf(
                    fontByteBuffer,
                    mSystemFontHelper.getFontIndexForResourceId(ttfId),
                    glyphIdsAfterSubset,
                    reservation.numeric ? numericCodepoints : empty);
            if (subsetted == null) {
                throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                        "Failed to subset font id=" + ttfId);
            }
            if (subsetted.length == 0) {
                Log.w(TAG, "TTF subsetting: empty subsetted font");
            }

            for (int i = 0; i < glyphIdsToReserve.length; i++) {
                reservation.remapGlyphIds(glyphIdsToReserve[i], glyphIdsAfterSubset[i]);
            }

            if (DEBUG_FONT_SUBSETTING) {
                Log.d(TAG, "TTF subsetting: subsetted TTF bytes size is " + subsetted.length);
                Log.d(TAG, "TTF subsetting: glyphIdsAfterSubset: " + Arrays.toString(
                        glyphIdsAfterSubset));
                if (DEBUG_FONT_DUMP) {
                    Log.d(TAG, "TTF subsetting: dumping...");
                    dumpAsFile(subsetted, "subsetted." + ttfId + ".ttf");
                }
            }

            ttfFontAdapter.setTtf(convertToArrayListByte(subsetted));
            reservation.applyToHalTexts();
        }
    }

    public void cleanup() {
        for (Map.Entry<Integer, ByteBuffer> entry : mTtfFontByteBufferCache.entrySet()) {
            if (mTtfFontCache.containsKey(entry.getKey())) {
                SharedMemory.unmap(entry.getValue());
            }
        }
        for (Map.Entry<Integer, SharedMemory> entry : mTtfFontCache.entrySet()) {
            entry.getValue().close();
        }
        mSystemFontHelper.clear();
        mTtfFontCache.clear();
        mTtfSubsetCache.clear();
    }

    private void loadTtfByteBuffer(int ttfId) throws DisplayOffloadException {
        boolean success;
        try {
            ByteBuffer buffer = mTtfFontCache.get(ttfId).mapReadOnly();
            mTtfFontByteBufferCache.put(ttfId, buffer);
            success = buffer.capacity() > 0;
        } catch (ErrnoException e) {
            Log.e(TAG, "Error opening shared memory: " + e);
            success = false;
        }

        if (!success) {
            throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                    "Unable to load font id=" + ttfId);
        }
    }

    private void shapeTextIfPossible(StaticTextAdapter staticTextAdapter, boolean useTabularNum)
            throws DisplayOffloadException {
        int ttfId = staticTextAdapter.getFontId();

        ByteBuffer fontByteBuffer = mTtfFontByteBufferCache.get(ttfId);
        if (fontByteBuffer == null) {
            Log.e(TAG, "Shaping failed due to missing byte buffer for " + ttfId);
            return;
        }
        mTextPreprocessorNative.shapeText(
                staticTextAdapter.getOriginalString(),
                staticTextAdapter.getFontSize(),
                ttfId,
                mSystemFontHelper.getFontIndexForResourceId(ttfId),
                fontByteBuffer,
                staticTextAdapter.getShapedGlyphIndices(),
                staticTextAdapter.getShapedGlyphPositions(), useTabularNum);
        if (staticTextAdapter.getShapedGlyphIndices().size() == 0) {
            throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE, "Failed to shape "
                    + "static text: " + staticTextAdapter.getId());
        }
        // reserve all required glyph IDs for purposes of font subsetting
        markGlyphIdsReservedForStaticText(staticTextAdapter);
    }

    private void markGlyphIdsReservedForStaticText(StaticTextAdapter staticTextAdapter) {
        if (staticTextAdapter == null || staticTextAdapter.getShapedGlyphIndices() == null
                || staticTextAdapter.getShapedGlyphIndices().isEmpty()) {
            return;
        }
        int ttfId = staticTextAdapter.getFontId();
        if (!mTtfSubsetCache.containsKey(ttfId)) {
            mTtfSubsetCache.put(ttfId, new TtfReservationInfo());
        }
        TtfReservationInfo reservation = mTtfSubsetCache.get(ttfId);
        if (reservation != null) {
            reservation.markReserve(staticTextAdapter);
        }
    }

    /**
     * Helper interface for isolating JNI logic from rest of
     * {@link com.google.android.clockwork.displayoffload.TextPreprocessor} for better testability.
     */
    @VisibleForTesting
    interface ITextPreprocessorNative {
        /**
         * Shape a given string using a TTF font, generate the offsets and glyph ids.
         */
        void shapeText(String text, float fontSize,
                int fontid, int fontIndex,
                ByteBuffer fontData, ArrayList<Integer> glyphs, ArrayList<Float> positions,
                boolean useTabularNum);

        /**
         * Creates a new subset TTF from the given TTF, keeping only the specified set of glyphs and
         * codepoints.
         */
        byte[] subsetTtf(ByteBuffer fontData, int fontIndex,
                int[] glyphIds, int[] codepoints);
    }

    /** Class containing necessary information during subsetting & glyph remapping. */
    private static class TtfReservationInfo {
        private final Map<Integer, Integer> glyphIdMappings;
        private final List<StaticTextAdapter> shapedTargetStaticTexts;
        boolean numeric;

        TtfReservationInfo() {
            this(false);
        }

        TtfReservationInfo(boolean includeNumeric) {
            glyphIdMappings = new ArrayMap<>();
            shapedTargetStaticTexts = new ArrayList<>();
            numeric = includeNumeric;
        }

        void markReserve(@NonNull StaticTextAdapter target) {
            for (Integer glyphId : target.getShapedGlyphIndices()) {
                glyphIdMappings.put(glyphId, glyphId);
            }
            shapedTargetStaticTexts.add(target);
        }

        void remapGlyphIds(int oldGlyphId, int newGlyphId) {
            glyphIdMappings.replace(oldGlyphId, newGlyphId);
        }

        void applyToHalTexts() {
            for (StaticTextAdapter target : shapedTargetStaticTexts) {
                for (int i = 0; i < target.getShapedGlyphIndices().size(); i++) {
                    final Integer newId = glyphIdMappings.get(
                            target.getShapedGlyphIndices().get(i));
                    target.setShapedGlyphIndices(i, newId);
                }
            }
        }

        int[] getGlyphIdsAsArray() {
            return convertToIntArray(glyphIdMappings.keySet());
        }
    }
}
