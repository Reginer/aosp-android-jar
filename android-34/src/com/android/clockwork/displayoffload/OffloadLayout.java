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

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_CONVERSION_FAILURE;

import android.content.Context;

import com.android.clockwork.displayoffload.HalTypeConverter.HalTypeConverterSupplier;
import com.android.internal.annotations.VisibleForTesting;

import com.google.android.clockwork.ambient.offload.types.StringResource;

import java.util.ArrayList;

/** Encapsulates information about an offloaded layout pinned to an UID. */
public class OffloadLayout extends BaseOffloadLayout {
    OffloadLayout(int uid, Context context, HalTypeConverterSupplier halTypeConverterSupplier) {
        super(uid, halTypeConverterSupplier, new HalResourceStore(halTypeConverterSupplier),
                new TextPreprocessor(context, halTypeConverterSupplier));
    }

    @VisibleForTesting
    OffloadLayout(int uid, HalTypeConverterSupplier halTypeConverterSupplier,
            HalResourceStore halResourceStore, TextPreprocessor textPreprocessor) {
        super(uid, halTypeConverterSupplier, halResourceStore, textPreprocessor);
    }

    // LINT.IfChange(buildHalResourceHelper)
    protected void buildHalResourceHelper() throws DisplayOffloadException {
        HalTypeConverter halTypeConverter = mHalTypeConverter.getConverter();

        halTypeConverter.begin();

        // Register OffloadRawMetric types first so that we know all the terminal data sources.
        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mOffloadRawMetrics)));
        mOffloadRawMetrics.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mOffloadMetrics)));
        mOffloadMetrics.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mCustomResources)));
        mCustomResources.clear();

        // Variables & Mappings
        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mOffloadStrings)));
        mOffloadStrings.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mOffloadConstants)));
        mOffloadConstants.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mLinearMetricMappings)));
        mLinearMetricMappings.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mRangeMappings)));
        mRangeMappings.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mModuloMappings)));
        mModuloMappings.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mNumberFormatMappings)));
        mNumberFormatMappings.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mUnaryOperations)));
        mUnaryOperations.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mBinaryOperations)));
        mBinaryOperations.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mTernaryOperations)));
        mTernaryOperations.clear();

        // UI Components
        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mTranslationGroups)));
        mTranslationGroups.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mRotationGroups)));
        mRotationGroups.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mBitmapResources)));
        mBitmapResources.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mArcPathResources)));
        mArcPathResources.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mRectShapeResources)));
        mRectShapeResources.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mRoundRectShapeResources)));
        mRoundRectShapeResources.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mLineShapeResources)));
        mLineShapeResources.clear();

        mHalResourceStore.addReplaceResource(
                halTypeConverter.toHalObject(new ArrayList<>(mSpriteSheetPngResources)));
        mSpriteSheetPngResources.clear();

        // Text & Font
        mTextPreprocessor.addFont(mHalResourceStore, mTtfFontResources);
        mTtfFontResources.clear();

        for (StringResource t : mStringResources) {
            if (mHalResourceStore.get(t.font) instanceof TtfFontAdapter) {
                // API no longer emits StringResource for TTF
                throw new DisplayOffloadException(
                        ERROR_LAYOUT_CONVERSION_FAILURE,
                        "StringResource use of TTF font is deprecated.");
            }
            mHalResourceStore.addReplaceResource(
                    ResourceObject.of(t.id, halTypeConverter.toHalObject(t)));
        }
        mStringResources.clear();

        mTextPreprocessor.addStaticText(mHalResourceStore, mStaticTextResources);
        mStaticTextResources.clear();

        mTextPreprocessor.addDynamicText(mHalResourceStore, mDynamicTextResources);
        mDynamicTextResources.clear();

        // Subset TTF fonts based on what glyphs were actually used
        mTextPreprocessor.processTtfFontSubsetting(mHalResourceStore);

        halTypeConverter.end();
    }
    // LINT.ThenChange()
}
