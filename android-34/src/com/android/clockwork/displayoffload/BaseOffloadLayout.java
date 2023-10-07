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

import static com.android.clockwork.displayoffload.Utils.TAG;

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_ARC_SHAPE_RESOURCE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_BINARY_OPERATION;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_BITMAP_RESOURCE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_CUSTOM_RESOURCE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_DYNAMIC_TEXT;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_LINE_SHAPE_RESOURCE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_MAPPING_LINEAR;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_MAPPING_MODULO;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_MAPPING_NUMBER_FORMAT;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_MAPPING_RANGE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_MINUTES_VALID;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_OFFLOAD_CONSTANT;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_OFFLOAD_METRIC;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_OFFLOAD_RAW_METRIC;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_OFFLOAD_STRING;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_RECT_SHAPE_RESOURCE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_ROTATION_GROUP;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_ROUND_RECT_SHAPE_RESOURCE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_SPRITE_SHEET_PNG_RESOURCE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_STATIC_TEXT;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_STRING_RESOURCE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_TERNARY_OPERATION;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_TRANSLATION_GROUP;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_TTF_FONT_RESOURCE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.FIELD_UNARY_OPERATION;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.RESOURCE_ID_EMPTY_LAYOUT;

import android.os.Bundle;
import android.util.Log;

import com.android.clockwork.displayoffload.HalTypeConverter.HalTypeConverterSupplier;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import com.google.android.clockwork.ambient.offload.types.ArcPathResource;
import com.google.android.clockwork.ambient.offload.types.BinaryOperation;
import com.google.android.clockwork.ambient.offload.types.BitmapResource;
import com.google.android.clockwork.ambient.offload.types.CustomResource;
import com.google.android.clockwork.ambient.offload.types.DynamicTextResource;
import com.google.android.clockwork.ambient.offload.types.LineShapeResource;
import com.google.android.clockwork.ambient.offload.types.LinearMetricMapping;
import com.google.android.clockwork.ambient.offload.types.ModuloMapping;
import com.google.android.clockwork.ambient.offload.types.NumberFormatMapping;
import com.google.android.clockwork.ambient.offload.types.OffloadConstant;
import com.google.android.clockwork.ambient.offload.types.OffloadMetric;
import com.google.android.clockwork.ambient.offload.types.OffloadRawMetric;
import com.google.android.clockwork.ambient.offload.types.OffloadString;
import com.google.android.clockwork.ambient.offload.types.RangeMapping;
import com.google.android.clockwork.ambient.offload.types.RectShapeResource;
import com.google.android.clockwork.ambient.offload.types.RotationGroup;
import com.google.android.clockwork.ambient.offload.types.RoundRectShapeResource;
import com.google.android.clockwork.ambient.offload.types.SpriteSheetPngResource;
import com.google.android.clockwork.ambient.offload.types.StaticTextResource;
import com.google.android.clockwork.ambient.offload.types.StringResource;
import com.google.android.clockwork.ambient.offload.types.TernaryOperation;
import com.google.android.clockwork.ambient.offload.types.TranslationGroup;
import com.google.android.clockwork.ambient.offload.types.TtfFontResource;
import com.google.android.clockwork.ambient.offload.types.UnaryOperation;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseOffloadLayout {
    @VisibleForTesting
    protected final HalResourceStore mHalResourceStore;
    protected final HalTypeConverterSupplier mHalTypeConverter;
    protected final TextPreprocessor mTextPreprocessor;
    // LINT.IfChange(offload_type_fields)
    // Aidl types
    protected final List<CustomResource> mCustomResources = new ArrayList<>();
    protected final List<RotationGroup> mRotationGroups = new ArrayList<>();
    protected final List<TranslationGroup> mTranslationGroups = new ArrayList<>();
    protected final List<BitmapResource> mBitmapResources = new ArrayList<>();
    protected final List<ArcPathResource> mArcPathResources = new ArrayList<>();
    protected final List<RectShapeResource> mRectShapeResources = new ArrayList<>();
    protected final List<RoundRectShapeResource> mRoundRectShapeResources = new ArrayList<>();
    protected final List<LineShapeResource> mLineShapeResources = new ArrayList<>();
    protected final List<SpriteSheetPngResource> mSpriteSheetPngResources = new ArrayList<>();
    protected final List<TtfFontResource> mTtfFontResources = new ArrayList<>();
    protected final List<StringResource> mStringResources = new ArrayList<>();
    protected final List<OffloadString> mOffloadStrings = new ArrayList<>();
    protected final List<StaticTextResource> mStaticTextResources = new ArrayList<>();
    protected final List<DynamicTextResource> mDynamicTextResources = new ArrayList<>();
    protected final List<OffloadRawMetric> mOffloadRawMetrics = new ArrayList<>();
    protected final List<OffloadMetric> mOffloadMetrics = new ArrayList<>();
    protected final List<OffloadConstant> mOffloadConstants = new ArrayList<>();
    protected final List<LinearMetricMapping> mLinearMetricMappings = new ArrayList<>();
    protected final List<RangeMapping> mRangeMappings = new ArrayList<>();
    protected final List<ModuloMapping> mModuloMappings = new ArrayList<>();
    protected final List<NumberFormatMapping> mNumberFormatMappings = new ArrayList<>();
    protected final List<UnaryOperation> mUnaryOperations = new ArrayList<>();
    protected final List<BinaryOperation> mBinaryOperations = new ArrayList<>();
    protected final List<TernaryOperation> mTernaryOperations = new ArrayList<>();
    private final int mCreatorPackageUid;
    // LINT.ThenChange(:unpack_aidl_type_fields)
    /**
     * How many minutes this layout is valid for before we should wake up the device and send an
     * onOffloadUpdate event to the application.  Defaults to -1 for valid indefinitely.
     */
    private int mMinutesValid = -1;
    private boolean mShouldClearResource = true;
    private boolean mIsOpen = false;
    private boolean mIsWaitingDoze = false;
    // Special case for brightness offload
    private boolean mIsBrightnessOffload = false;

    @VisibleForTesting
    BaseOffloadLayout(int uid,
            HalTypeConverterSupplier halTypeConverter,
            HalResourceStore halResourceStore,
            TextPreprocessor textPreprocessor
    ) {
        mCreatorPackageUid = uid;
        mHalTypeConverter = halTypeConverter;
        mHalResourceStore = halResourceStore;
        mTextPreprocessor = textPreprocessor;
    }

    void open() {
        mIsOpen = true;
    }

    void close() {
        mIsOpen = false;
    }

    boolean isOpen() {
        return mIsOpen;
    }

    boolean isWaitingDoze() {
        return mIsWaitingDoze;
    }

    int getUid() {
        return mCreatorPackageUid;
    }

    int getMinutesValid() {
        return mMinutesValid;
    }

    // LINT.IfChange(unpack_aidl_type_fields)
    void updateFromBundle(Bundle bundle) {
        // TODO(b/179528367): add functions and impls for partial replacement
        bundle.setClassLoader(CustomResource.class.getClassLoader());
        ArrayList<CustomResource> customResources = bundle.getParcelableArrayList(
                FIELD_CUSTOM_RESOURCE);
        if (customResources != null) {
            mCustomResources.addAll(customResources);
        }

        mMinutesValid = bundle.getInt(FIELD_MINUTES_VALID, -1);

        bundle.setClassLoader(TranslationGroup.class.getClassLoader());
        ArrayList<TranslationGroup> translationGroups = bundle.getParcelableArrayList(
                FIELD_TRANSLATION_GROUP);
        if (translationGroups != null) {
            // Special case for brightness offload
            if (translationGroups.size() == 1 && translationGroups.get(0).id
                    == RESOURCE_ID_EMPTY_LAYOUT) {
                mIsBrightnessOffload = true;
                Log.i(TAG, "[BrightnessOffload] Enabled, layout uid=" + mCreatorPackageUid);
                return;
            }
            mTranslationGroups.addAll(translationGroups);
        }

        bundle.setClassLoader(OffloadRawMetric.class.getClassLoader());
        ArrayList<OffloadRawMetric> offloadRawMetrics
                = bundle.getParcelableArrayList(FIELD_OFFLOAD_RAW_METRIC);
        if (offloadRawMetrics != null) {
            mOffloadRawMetrics.addAll(offloadRawMetrics);
        }

        bundle.setClassLoader(OffloadMetric.class.getClassLoader());
        ArrayList<OffloadMetric> offloadMetrics = bundle.getParcelableArrayList(
                FIELD_OFFLOAD_METRIC);
        if (offloadMetrics != null) {
            mOffloadMetrics.addAll(offloadMetrics);
        }

        bundle.setClassLoader(RotationGroup.class.getClassLoader());
        ArrayList<RotationGroup> rotationGroups = bundle.getParcelableArrayList(
                FIELD_ROTATION_GROUP);
        if (rotationGroups != null) {
            mRotationGroups.addAll(rotationGroups);
        }

        bundle.setClassLoader(BitmapResource.class.getClassLoader());
        ArrayList<BitmapResource> bitmapResources =
                bundle.getParcelableArrayList(FIELD_BITMAP_RESOURCE);
        if (bitmapResources != null) {
            mBitmapResources.addAll(bitmapResources);
        }

        bundle.setClassLoader(ArcPathResource.class.getClassLoader());
        ArrayList<ArcPathResource> arcShapeResources = bundle.getParcelableArrayList(
                FIELD_ARC_SHAPE_RESOURCE);
        if (arcShapeResources != null) {
            mArcPathResources.addAll(arcShapeResources);
        }

        bundle.setClassLoader(RectShapeResource.class.getClassLoader());
        ArrayList<RectShapeResource> rectShapeResources = bundle.getParcelableArrayList(
                FIELD_RECT_SHAPE_RESOURCE);
        if (rectShapeResources != null) {
            mRectShapeResources.addAll(rectShapeResources);
        }

        bundle.setClassLoader(RoundRectShapeResource.class.getClassLoader());
        ArrayList<RoundRectShapeResource> roundRectShapeResources = bundle.getParcelableArrayList(
                FIELD_ROUND_RECT_SHAPE_RESOURCE);
        if (roundRectShapeResources != null) {
            mRoundRectShapeResources.addAll(roundRectShapeResources);
        }

        bundle.setClassLoader(LineShapeResource.class.getClassLoader());
        ArrayList<LineShapeResource> lineShapeResources = bundle.getParcelableArrayList(
                FIELD_LINE_SHAPE_RESOURCE);
        if (lineShapeResources != null) {
            mLineShapeResources.addAll(lineShapeResources);
        }

        bundle.setClassLoader(SpriteSheetPngResource.class.getClassLoader());
        ArrayList<SpriteSheetPngResource> spriteSheetPngResource = bundle.getParcelableArrayList(
                FIELD_SPRITE_SHEET_PNG_RESOURCE);
        if (spriteSheetPngResource != null) {
            mSpriteSheetPngResources.addAll(spriteSheetPngResource);
        }

        bundle.setClassLoader(TtfFontResource.class.getClassLoader());
        ArrayList<TtfFontResource> ttfFontResources = bundle.getParcelableArrayList(
                FIELD_TTF_FONT_RESOURCE);
        if (ttfFontResources != null) {
            mTtfFontResources.addAll(ttfFontResources);
        }

        bundle.setClassLoader(StringResource.class.getClassLoader());
        ArrayList<StringResource> stringResources = bundle.getParcelableArrayList(
                FIELD_STRING_RESOURCE);
        if (stringResources != null) {
            mStringResources.addAll(stringResources);
        }

        bundle.setClassLoader(OffloadConstant.class.getClassLoader());
        ArrayList<OffloadConstant> offloadConstants = bundle.getParcelableArrayList(
                FIELD_OFFLOAD_CONSTANT);
        if (offloadConstants != null) {
            mOffloadConstants.addAll(offloadConstants);
        }

        bundle.setClassLoader(OffloadString.class.getClassLoader());
        ArrayList<OffloadString> offloadStrings = bundle.getParcelableArrayList(
                FIELD_OFFLOAD_STRING);
        if (offloadStrings != null) {
            mOffloadStrings.addAll(offloadStrings);
        }

        bundle.setClassLoader(StaticTextResource.class.getClassLoader());
        ArrayList<StaticTextResource> staticTextResources = bundle.getParcelableArrayList(
                FIELD_STATIC_TEXT);
        if (staticTextResources != null) {
            mStaticTextResources.addAll(staticTextResources);
        }

        bundle.setClassLoader(DynamicTextResource.class.getClassLoader());
        ArrayList<DynamicTextResource> dynamicTextResources = bundle.getParcelableArrayList(
                FIELD_DYNAMIC_TEXT);
        if (dynamicTextResources != null) {
            mDynamicTextResources.addAll(dynamicTextResources);
        }

        bundle.setClassLoader(LinearMetricMapping.class.getClassLoader());
        ArrayList<LinearMetricMapping> linearMappings = bundle.getParcelableArrayList(
                FIELD_MAPPING_LINEAR);
        if (linearMappings != null) {
            mLinearMetricMappings.addAll(linearMappings);
        }

        bundle.setClassLoader(RangeMapping.class.getClassLoader());
        ArrayList<RangeMapping> rangeMappings = bundle.getParcelableArrayList(FIELD_MAPPING_RANGE);
        if (rangeMappings != null) {
            mRangeMappings.addAll(rangeMappings);
        }

        bundle.setClassLoader(ModuloMapping.class.getClassLoader());
        ArrayList<ModuloMapping> moduloMappings = bundle.getParcelableArrayList(
                FIELD_MAPPING_MODULO);
        if (moduloMappings != null) {
            mModuloMappings.addAll(moduloMappings);
        }

        bundle.setClassLoader(NumberFormatMapping.class.getClassLoader());
        ArrayList<NumberFormatMapping> numberFormatMappings = bundle.getParcelableArrayList(
                FIELD_MAPPING_NUMBER_FORMAT);
        if (numberFormatMappings != null) {
            mNumberFormatMappings.addAll(numberFormatMappings);
        }

        bundle.setClassLoader(UnaryOperation.class.getClassLoader());
        ArrayList<UnaryOperation> unaryOperation = bundle.getParcelableArrayList(
                FIELD_UNARY_OPERATION);
        if (unaryOperation != null) {
            mUnaryOperations.addAll(unaryOperation);
        }

        bundle.setClassLoader(BinaryOperation.class.getClassLoader());
        ArrayList<BinaryOperation> binaryOperations = bundle.getParcelableArrayList(
                FIELD_BINARY_OPERATION);
        if (binaryOperations != null) {
            mBinaryOperations.addAll(binaryOperations);
        }

        bundle.setClassLoader(TernaryOperation.class.getClassLoader());
        ArrayList<TernaryOperation> ternaryOperations = bundle.getParcelableArrayList(
                FIELD_TERNARY_OPERATION);
        if (ternaryOperations != null) {
            mTernaryOperations.addAll(ternaryOperations);
        }
    }
    // LINT.ThenChange(OffloadLayout.java:buildHalResourceHelper)

    void addStatusBar(TranslationGroup statusBarGroup, BitmapResource statusBarBitmap) {
        if (mIsBrightnessOffload) {
            Log.i(TAG, "[BrightnessOffload] Skipping status bar.");
            return;
        }
        mTranslationGroups.add(statusBarGroup);
        mBitmapResources.add(statusBarBitmap);
    }

    protected abstract void buildHalResourceHelper() throws DisplayOffloadException;

    public void markWaitingDoze() {
        mIsWaitingDoze = true;
    }

    public void markHalResourcesDirty() {
        Log.i(TAG, "markHalResourcesDirty: uid=" + mCreatorPackageUid);
        mHalResourceStore.markAllDirty();
        mShouldClearResource = true;
        mIsWaitingDoze = false;
    }

    /**
     * Convert and validate HAL resource, generates a send order based on topological sort
     *
     * @throws DisplayOffloadException if conversion or validation fails
     */
    public void validateHalResource() throws DisplayOffloadException {
        if (mIsBrightnessOffload) {
            // mHalResourceStore should only contain a single TranslationGroup in this case
            Log.i(TAG, "[BrightnessOffload] Creating empty layout.");
            mHalResourceStore.clear();
            mHalTypeConverter.getConverter().begin();
            mHalTypeConverter.getConverter().addEmptyTranslationGroup(mHalResourceStore);
            mHalTypeConverter.getConverter().end();
        } else {
            try {
                buildHalResourceHelper();
            } catch (DisplayOffloadException e) {
                Log.e(TAG, "buildHalResource: error while building hal resources", e);
                throw e;
            } finally {
                // Clean up
                mTextPreprocessor.cleanup();
            }
        }

        mHalResourceStore.generateSendOrder();
    }

    /**
     * Send dirty resources to HAL with lock to HAL held.
     *
     * @param halAdapter for abstracting HAL details
     * @throws DisplayOffloadException if HAL call fails in anyways
     */
    public void sendToHal(HalAdapter halAdapter) throws DisplayOffloadException {
        if (mShouldClearResource) {
            halAdapter.resetResource();
            mHalResourceStore.markAllDirty();
        }

        mHalResourceStore.sendToHal(halAdapter);
        mShouldClearResource = false;
        mIsWaitingDoze = false;
    }

    /**
     * Returns if the OffloadLayout contains any valid resources.
     * Must be called after validateHalResource.
     *
     * @return true if the OffloadLayout contains at least one valid resource
     */
    public boolean isEmpty() {
        return mHalResourceStore.isEmpty();
    }

    /**
     * Compute the root resource of this OffloadLayout
     *
     * @return id of the HAL resource that's the root
     * @throws DisplayOffloadException if there are no root
     */
    public int getRootId() throws DisplayOffloadException {
        return mHalResourceStore.getRootId();
    }

    boolean isBrightnessOffload() {
        return mIsBrightnessOffload;
    }

    void dump(IndentingPrintWriter ipw) {
        ipw.printPair("mIsOpen", mIsOpen);
        ipw.printPair("mCreatorPackageUid", mCreatorPackageUid);
        ipw.printPair("mIsBrightnessOffload", mIsBrightnessOffload);
        ipw.println();

        ipw.println("mHalResourceStore");
        ipw.increaseIndent();
        mHalResourceStore.dump(ipw);
        ipw.decreaseIndent();
    }
}
