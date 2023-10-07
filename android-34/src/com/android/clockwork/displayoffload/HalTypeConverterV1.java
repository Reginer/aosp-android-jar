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

import static com.android.clockwork.displayoffload.Utils.checkNameValid;
import static com.android.clockwork.displayoffload.Utils.isArrayLengthOne;

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_CONVERSION_FAILURE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.RESOURCE_ID_EMPTY_LAYOUT;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.RESOURCE_ID_VIRTUAL_ROOT;
import static com.google.android.clockwork.ambient.offload.types.UnaryOperationTypeEnum.CEILING;
import static com.google.android.clockwork.ambient.offload.types.UnaryOperationTypeEnum.FLOOR;
import static com.google.android.clockwork.ambient.offload.types.UnaryOperationTypeEnum.RECIPROCAL;

import android.content.Context;
import android.util.ArrayMap;

import com.google.android.clockwork.ambient.offload.types.OffloadConstantType;
import com.google.android.clockwork.ambient.offload.types.OffloadRawMetric;
import com.google.android.clockwork.ambient.offload.types.RawMetricType;
import com.google.android.clockwork.ambient.offload.types.UnaryOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import vendor.google_clockwork.displayoffload.V1_0.BindableColor;
import vendor.google_clockwork.displayoffload.V1_0.BindableFloat;
import vendor.google_clockwork.displayoffload.V1_0.BindableString;
import vendor.google_clockwork.displayoffload.V1_0.CustomResource;
import vendor.google_clockwork.displayoffload.V1_0.FixedSizeSprite;
import vendor.google_clockwork.displayoffload.V1_0.FontInfo;
import vendor.google_clockwork.displayoffload.V1_0.FontMetrics;
import vendor.google_clockwork.displayoffload.V1_0.KerningInfo;
import vendor.google_clockwork.displayoffload.V1_0.LinearMetricMapping;
import vendor.google_clockwork.displayoffload.V1_0.ModuloMapping;
import vendor.google_clockwork.displayoffload.V1_0.OffloadConstant;
import vendor.google_clockwork.displayoffload.V1_0.OffloadMetric;
import vendor.google_clockwork.displayoffload.V1_0.OffloadString;
import vendor.google_clockwork.displayoffload.V1_0.PngResource;
import vendor.google_clockwork.displayoffload.V1_0.ProportionalFont;
import vendor.google_clockwork.displayoffload.V1_0.RangeMapping;
import vendor.google_clockwork.displayoffload.V1_0.RotationGroup;
import vendor.google_clockwork.displayoffload.V1_0.SpriteSheetInfo;
import vendor.google_clockwork.displayoffload.V1_0.SpriteSheetPngResource;
import vendor.google_clockwork.displayoffload.V1_0.StringResource;
import vendor.google_clockwork.displayoffload.V1_0.TranslationGroup;
import vendor.google_clockwork.displayoffload.V1_0.TtfFontResource;
import vendor.google_clockwork.displayoffload.V1_1.ArcPathResource;
import vendor.google_clockwork.displayoffload.V1_1.CeilMapping;
import vendor.google_clockwork.displayoffload.V1_1.DynamicTextResource;
import vendor.google_clockwork.displayoffload.V1_1.FloorMapping;
import vendor.google_clockwork.displayoffload.V1_1.NumberFormatMapping;
import vendor.google_clockwork.displayoffload.V1_1.ReciprocalMapping;
import vendor.google_clockwork.displayoffload.V1_1.RectShapeResource;
import vendor.google_clockwork.displayoffload.V1_1.RoundRectShapeResource;
import vendor.google_clockwork.displayoffload.V1_1.ShapeParam;
import vendor.google_clockwork.displayoffload.V1_1.StaticTextResource;
import vendor.google_clockwork.displayoffload.V1_1.TextParam;

// LINT.IfChange(aidl_type_conversions)

/**
 * Helper class that handles all the conversion between AIDL types and HAL types for DisplayOffload
 * V1.
 */
class HalTypeConverterV1 extends HalTypeConverter {
    private final String MAPPING_NAME_CEIL = "ceil";
    private final String MAPPING_NAME_FLOOR = "floor";
    private final String MAPPING_NAME_RECIPROCAL = "reciprocal";
    // TODO(b/249594256): make final decision of using prefix or not.
    private static final String DATASOURCE_CONSTANT_PREFIX = "b_";

    private final Context mContext;
    private final Map<Integer, String> mBindingIdToDataSource = new ArrayMap<>();

    public HalTypeConverterV1(Context context) {
        mContext = context;
    }

    @Override
    void begin() {
        mBindingIdToDataSource.clear();
    }

    @Override
    List<ResourceObject> toHalObject(Object in) throws DisplayOffloadException {
        List<ResourceObject> out = super.toHalObject(in);
        if (out != null) {
            return out;
        }
        // StringResource is a V1-only type.
        if (in instanceof com.google.android.clockwork.ambient.offload.types.StringResource) {
            com.google.android.clockwork.ambient.offload.types.StringResource stringResource =
                    (com.google.android.clockwork.ambient.offload.types.StringResource) in;
            checkResourceIdRange(stringResource.id);
            return toHalStringResource(stringResource);
        }
        throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                "Unsupported type: " + in.getClass().getSimpleName());
    }

    @Override
    List<ResourceObject> createDummyTranslationGroup(int id, ArrayList<Integer> remappedRootIds) {
        TranslationGroup translationGroup = new TranslationGroup();
        if (remappedRootIds == null) {
            translationGroup.id = RESOURCE_ID_EMPTY_LAYOUT;
            translationGroup.contents = new ArrayList<>();
        } else {
            translationGroup.id = RESOURCE_ID_VIRTUAL_ROOT;
            translationGroup.contents = remappedRootIds;
        }
        translationGroup.offsetX = 0;
        translationGroup.offsetY = 0;
        return Collections.singletonList(ResourceObject.of(translationGroup.id, translationGroup));
    }

    @Override
    protected List<ResourceObject> toHalCustomResource(
            com.google.android.clockwork.ambient.offload.types.CustomResource aidlType)
            throws DisplayOffloadException {
        if (aidlType.keyValues == null) {
            throw new DisplayOffloadException(
                    ERROR_LAYOUT_CONVERSION_FAILURE, "Failed to convert: " + aidlType.id);
        }
        CustomResource customResource = new CustomResource();
        customResource.id = aidlType.id;
        customResource.keyValues = HalTypeConverterUtils.toHalKVPairArrayList(aidlType.keyValues);

        return Collections.singletonList(ResourceObject.of(customResource.id, customResource));
    }

    @Override
    protected List<ResourceObject> toHalTranslationGroup(
            com.google.android.clockwork.ambient.offload.types.TranslationGroup aidlType)
            throws DisplayOffloadException {
        if (aidlType.offsetX == null
                || aidlType.offsetY == null
                || aidlType.offsetX.binding != null
                || aidlType.offsetY.binding != null) {
            throw new DisplayOffloadException(
                    ERROR_LAYOUT_CONVERSION_FAILURE, "Failed to convert: " + aidlType.id);
        }
        TranslationGroup translationGroup = new TranslationGroup();
        translationGroup.id = aidlType.id;
        translationGroup.contents = Utils.convertToArrayListInteger(aidlType.contents);
        translationGroup.offsetX = aidlType.offsetX.value;
        translationGroup.offsetY = aidlType.offsetY.value;

        return Collections.singletonList(ResourceObject.of(translationGroup.id, translationGroup));
    }

    @Override
    protected List<ResourceObject> toHalRotationGroup(
            com.google.android.clockwork.ambient.offload.types.RotationGroup aidlType)
            throws DisplayOffloadException {
        if (aidlType.pivotX == null
                || aidlType.pivotY == null
                || aidlType.pivotX.binding != null
                || aidlType.pivotY.binding != null) {
            throw new DisplayOffloadException(
                    ERROR_LAYOUT_CONVERSION_FAILURE, "Failed to convert: " + aidlType.id);
        }
        RotationGroup rotationGroup = new RotationGroup();
        rotationGroup.id = aidlType.id;
        rotationGroup.contents = Utils.convertToArrayListInteger(aidlType.contents);
        rotationGroup.pivotX = aidlType.pivotX.value;
        rotationGroup.pivotY = aidlType.pivotY.value;
        rotationGroup.angleDeg = toHalBindableFloat(aidlType.angleDeg);

        return Collections.singletonList(ResourceObject.of(rotationGroup.id, rotationGroup));
    }

    @Override
    protected List<ResourceObject> toHalBitmapResource(
            com.google.android.clockwork.ambient.offload.types.BitmapResource aidlType) {
        PngResource pngResource = new PngResource();
        pngResource.id = aidlType.id;
        pngResource.height = aidlType.height;
        pngResource.width = aidlType.width;
        pngResource.color = toHalBindableColor(aidlType.color);
        pngResource.data = Utils.convertToArrayListByte(mContext, aidlType.icon);
        return Collections.singletonList(ResourceObject.of(pngResource.id, pngResource));
    }

    @Override
    protected List<ResourceObject> toHalRectShapeResource(
            com.google.android.clockwork.ambient.offload.types.RectShapeResource aidlType) {
        RectShapeResource rect = new RectShapeResource();
        rect.id = aidlType.id;
        rect.width = toHalBindableFloat(aidlType.width);
        rect.height = toHalBindableFloat(aidlType.height);
        rect.shapeParam = toHalShapeParam(aidlType.shapeParam);
        return Collections.singletonList(ResourceObject.of(rect.id, rect));
    }

    @Override
    protected List<ResourceObject> toHalRoundRectShapeResource(
            com.google.android.clockwork.ambient.offload.types.RoundRectShapeResource aidlType) {
        RoundRectShapeResource rect = new RoundRectShapeResource();
        rect.id = aidlType.id;
        rect.cornerRadius = toHalBindableFloat(aidlType.cornerRadius);
        rect.width = toHalBindableFloat(aidlType.width);
        rect.height = toHalBindableFloat(aidlType.height);
        rect.shapeParam = toHalShapeParam(aidlType.shapeParam);
        return Collections.singletonList(ResourceObject.of(rect.id, rect));
    }

    @Override
    protected List<ResourceObject> toHalArcPathResource(
            com.google.android.clockwork.ambient.offload.types.ArcPathResource aidlType) {
        ArcPathResource arc = new ArcPathResource();
        arc.id = aidlType.id;
        arc.width = toHalBindableFloat(aidlType.width);
        arc.height = toHalBindableFloat(aidlType.height);
        arc.startDeg = toHalBindableFloat(aidlType.startDeg);
        arc.sweepDeg = toHalBindableFloat(aidlType.sweepDeg);
        arc.shapeParam = toHalShapeParam(aidlType.shapeParam);
        arc.endCapStyle = aidlType.endCapStyle;
        return Collections.singletonList(ResourceObject.of(arc.id, arc));
    }

    private List<ResourceObject> toHalStringResource(
            com.google.android.clockwork.ambient.offload.types.StringResource aidlType) {
        StringResource str = new StringResource();
        str.id = aidlType.id;
        str.color = toHalBindableColor(aidlType.color);
        str.content = toHalBindableString(aidlType.content);
        str.font = aidlType.font;
        str.textAlignment = aidlType.textAlignment;
        str.ttfFontSize = aidlType.ttfFontSize;

        return Collections.singletonList(ResourceObject.of(str.id, str));
    }

    private BindableString toHalBindableString(
            com.google.android.clockwork.ambient.offload.types.BindableString aidlType) {
        BindableString str = new BindableString();
        str.value("");
        if (isArrayLengthOne(aidlType.binding)) {
            str.binding(getDataSourceOrBinding(aidlType.binding[0]));
        } else if (aidlType.value != null) {
            str.value(aidlType.value);
        } else if (aidlType.glyphIndexBindings != null) {
            str.glyphIndexBindings(
                    getDataSourceOrBindingBatched(aidlType.glyphIndexBindings));
        }
        return str;
    }

    @Override
    public List<ResourceObject> toHalTtfFontResource(
            com.google.android.clockwork.ambient.offload.types.TtfFontResource aidlType)
            throws DisplayOffloadException {
        checkResourceIdRange(aidlType.id);

        // V1
        TtfFontResource ttfFontResource = new TtfFontResource();
        ttfFontResource.id = aidlType.id;
        // Create an empty buffer for safety, to be replaced by subsetted ttf.
        ttfFontResource.ttf = new ArrayList<>();

        return Collections.singletonList(
                ResourceObject.of(ttfFontResource.id, new TtfFontAdapter(ttfFontResource)));
    }

    @Override
    protected List<ResourceObject> toHalSpriteSheetPngResource(
            com.google.android.clockwork.ambient.offload.types.SpriteSheetPngResource aidlType)
            throws DisplayOffloadException {
        checkResourceIdRange(aidlType.id);

        SpriteSheetPngResource halType = new SpriteSheetPngResource();
        halType.id = aidlType.id;
        halType.count = (short) aidlType.count;
        halType.tintable = aidlType.tintable;
        halType.sheetPng = Utils.convertToArrayListByte(aidlType.sheetPng);
        halType.fontInfo = toHalFontInfo(aidlType.fontInfo);
        halType.spriteInfo = toHalSpriteInfo(aidlType.spriteInfo);
        return Collections.singletonList(ResourceObject.of(halType.id, halType));
    }

    private SpriteSheetInfo toHalSpriteInfo(
            com.google.android.clockwork.ambient.offload.types.SpriteSheetInfo spriteInfo) {
        SpriteSheetInfo halType = new SpriteSheetInfo();
        if (spriteInfo.fixed != null) {
            FixedSizeSprite fixedSizeSprite = new FixedSizeSprite();
            fixedSizeSprite.spriteHeight = (short) spriteInfo.fixed.spriteHeight;
            fixedSizeSprite.spriteWidth = (short) spriteInfo.fixed.spriteWidth;
            halType.fixed(fixedSizeSprite);
        } else if (spriteInfo.proportional != null) {
            ProportionalFont proportionalFont = new ProportionalFont();
            proportionalFont.glyphHeight = (short) spriteInfo.proportional.glyphHeight;
            proportionalFont.glyphWidth = Utils.convertToArrayListShort(
                    spriteInfo.proportional.glyphWidth);
            proportionalFont.kerningInfo = toHalKerningInfo(spriteInfo.proportional.kerningInfo);
            halType.proportional(proportionalFont);
        }
        return halType;
    }

    private KerningInfo toHalKerningInfo(
            com.google.android.clockwork.ambient.offload.types.KerningInfo kerningInfo) {
        // Optional property
        if (kerningInfo == null) return null;

        KerningInfo halType = new KerningInfo();
        halType.first = Utils.convertToArrayListShort(kerningInfo.first);
        halType.second = Utils.convertToArrayListShort(kerningInfo.second);
        halType.kern = Utils.convertToArrayListInteger(kerningInfo.kern);
        return halType;
    }

    private FontInfo toHalFontInfo(
            com.google.android.clockwork.ambient.offload.types.FontInfo fontInfo) {
        FontInfo halType = new FontInfo();
        if (fontInfo != null && fontInfo.encodingMap != null) {
            halType.encodingMap = fontInfo.encodingMap;
        } else {
            halType.encodingMap = "";
        }
        halType.textMetrics = toHalTextMetric(fontInfo == null ? null : fontInfo.textMetrics);
        return halType;
    }

    private FontMetrics toHalTextMetric(
            com.google.android.clockwork.ambient.offload.types.FontMetrics textMetrics) {
        FontMetrics halType = new FontMetrics();
        // Optional property
        if (textMetrics == null) {
            halType.ascent = 0;
            halType.descent = 0;
            halType.top = 0;
            halType.bottom = 0;
            halType.leading = 0;
            return halType;
        }
        halType.ascent = textMetrics.ascent;
        halType.descent = textMetrics.descent;
        halType.top = textMetrics.top;
        halType.bottom = textMetrics.bottom;
        halType.leading = textMetrics.leading;
        return halType;
    }

    private ShapeParam toHalShapeParam(
            com.google.android.clockwork.ambient.offload.types.ShapeParam aidlType) {
        if (aidlType == null) {
            return null;
        }
        ShapeParam shapeParam = new ShapeParam();
        // TODO(b/179179625): update HAL type to separate stroke and fill
        shapeParam.color = toHalBindableColor(aidlType.color);
        shapeParam.strokeWidth = aidlType.strokeWidth;
        shapeParam.style = aidlType.style;
        // TODO: expose AA up the stack.
        shapeParam.isAA = true;
        shapeParam.blendMode = aidlType.blendMode;
        return shapeParam;
    }

    @Override
    protected List<ResourceObject> toHalOffloadString(
            com.google.android.clockwork.ambient.offload.types.OffloadString aidlType)
            throws DisplayOffloadException {
        checkNameValid(aidlType.name);

        OffloadString offloadString = new OffloadString();
        offloadString.name = "" + aidlType.name;
        offloadString.dataSources = getDataSourceOrBindingBatched(aidlType.dataSources);
        offloadString.formatString = aidlType.formatString;

        return Collections.singletonList(ResourceObject.of(offloadString.name, offloadString));
    }

    @Override
    List<ResourceObject> toHalStaticText(
            com.google.android.clockwork.ambient.offload.types.StaticTextResource aidlType)
            throws DisplayOffloadException {
        checkResourceIdRange(aidlType.id);
        StaticTextResource staticTextResource = new StaticTextResource();
        staticTextResource.id = aidlType.id;
        staticTextResource.value = aidlType.value;
        staticTextResource.textParam = toHalTextParam(aidlType.textParam);
        return Collections.singletonList(
                ResourceObject.of(staticTextResource.id,
                        new StaticTextAdapter(staticTextResource)));
    }

    @Override
    protected List<ResourceObject> toHalDynamicText(
            com.google.android.clockwork.ambient.offload.types.DynamicTextResource aidlType)
            throws DisplayOffloadException {
        checkResourceIdRange(aidlType.id);
        DynamicTextResource dynamicTextResource = new DynamicTextResource();
        dynamicTextResource.id = aidlType.id;

        ArrayList<String> bindings = new ArrayList<>();
        bindings.add(getDataSourceOrBinding(aidlType.binding));
        dynamicTextResource.bindings = bindings;
        dynamicTextResource.textParam = toHalTextParam(aidlType.textParam);

        return Collections.singletonList(
                ResourceObject.of(dynamicTextResource.id,
                        new DynamicTextAdapter(dynamicTextResource)));
    }

    private TextParam toHalTextParam(
            com.google.android.clockwork.ambient.offload.types.TextParam aidlType) {
        TextParam textParam = new TextParam();
        textParam.color = toHalBindableColor(aidlType.color);
        textParam.textAlignment = aidlType.textAlignment;
        textParam.ttfFont = aidlType.ttfFont;
        textParam.ttfFontSize = aidlType.ttfFontSize;
        return textParam;
    }

    @Override
    protected List<ResourceObject> toHalOffloadRawMetric(OffloadRawMetric rawMetric)
            throws DisplayOffloadException {
        checkNameValid(rawMetric.name);
        if (rawMetric.rawMetricType == RawMetricType.DATA_SOURCE
                || rawMetric.rawMetricType == RawMetricType.EXPRESSION) {
            return extractDataSourceInfo(rawMetric);
        }
        throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                "Unknown RawMetricType = " + rawMetric.rawMetricType);
    }

    private List<ResourceObject> extractDataSourceInfo(
            com.google.android.clockwork.ambient.offload.types.OffloadRawMetric aidlType) {
        mBindingIdToDataSource.put(aidlType.name, aidlType.data);
        return new LinkedList<>();
    }

    @Override
    protected List<ResourceObject> toHalOffloadMetric(
            com.google.android.clockwork.ambient.offload.types.OffloadMetric aidlType)
            throws DisplayOffloadException {
        checkNameValid(aidlType.name);
        OffloadMetric offloadMetric = getOffloadMetric(aidlType.name,
                getDataSourceOrBinding(aidlType.boundDataSource),
                "" + aidlType.mapping);
        return Collections.singletonList(ResourceObject.of(offloadMetric.name, offloadMetric));
    }

    OffloadMetric getOffloadMetric(int name, String bindingOrDataSource, String mapping) {
        OffloadMetric offloadMetric = new OffloadMetric();
        offloadMetric.name = DATASOURCE_CONSTANT_PREFIX + name;
        offloadMetric.dataSource = bindingOrDataSource;
        offloadMetric.mapping = mapping;
        return offloadMetric;
    }

    private String getDataSourceOrBinding(int bindingId) {
        return mBindingIdToDataSource.getOrDefault(bindingId,
                DATASOURCE_CONSTANT_PREFIX + bindingId);
    }

    private ArrayList<String> getDataSourceOrBindingBatched(int[] bindingIds) {
        ArrayList<String> bindingIdsOrDataSources = new ArrayList<>();
        for (int bindingId : bindingIds) {
            bindingIdsOrDataSources.add(getDataSourceOrBinding(bindingId));
        }
        return bindingIdsOrDataSources;
    }

    @Override
    protected List<ResourceObject> toHalLinearMetricMapping(
            com.google.android.clockwork.ambient.offload.types.LinearMetricMapping aidlType)
            throws DisplayOffloadException {
        checkNameValid(aidlType.name);
        LinearMetricMapping linearMetricMapping = new LinearMetricMapping();
        linearMetricMapping.name = "" + aidlType.name;
        linearMetricMapping.m = aidlType.m;
        linearMetricMapping.b = aidlType.b;

        return Collections.singletonList(
                ResourceObject.of(linearMetricMapping.name, linearMetricMapping));
    }

    @Override
    protected List<ResourceObject> toHalRangeMapping(
            com.google.android.clockwork.ambient.offload.types.RangeMapping aidlType)
            throws DisplayOffloadException {
        checkNameValid(aidlType.name);
        RangeMapping rangeMapping = new RangeMapping();
        rangeMapping.name = "" + aidlType.name;
        rangeMapping.thresholds = Utils.convertToArrayListFloat(aidlType.thresholds);
        rangeMapping.val = getDataSourceOrBindingBatched(aidlType.val);

        return Collections.singletonList(ResourceObject.of(rangeMapping.name, rangeMapping));
    }

    @Override
    protected List<ResourceObject> toHalNumberFormatMapping(
            com.google.android.clockwork.ambient.offload.types.NumberFormatMapping aidlType)
            throws DisplayOffloadException {
        checkNameValid(aidlType.name);
        NumberFormatMapping numberFormatMapping = new NumberFormatMapping();
        numberFormatMapping.name = "" + aidlType.name;
        numberFormatMapping.grouping = aidlType.grouping;
        numberFormatMapping.maximumFractionDigits = (byte) aidlType.maximumFractionDigits;
        numberFormatMapping.minimumFractionDigits = (byte) aidlType.minimumFractionDigits;
        numberFormatMapping.minimumIntegerDigits = (byte) aidlType.minimumIntegerDigits;

        return Collections.singletonList(
                ResourceObject.of(numberFormatMapping.name, numberFormatMapping));
    }

    @Override
    protected List<ResourceObject> toHalOffloadConstant(
            com.google.android.clockwork.ambient.offload.types.OffloadConstant aidlType)
            throws DisplayOffloadException {

        checkNameValid(aidlType.name);

        OffloadConstant offloadConstant = new OffloadConstant();
        offloadConstant.name = DATASOURCE_CONSTANT_PREFIX + aidlType.name;
        switch (aidlType.valueType) {
            case OffloadConstantType.FLOAT:
                offloadConstant.value.floatValue(aidlType.floatValue);
                break;
            case OffloadConstantType.INT32:
                offloadConstant.value.intValue(aidlType.intValue);
                break;
            case OffloadConstantType.STRING:
                offloadConstant.value.stringValue(aidlType.stringValue);
                break;
            case OffloadConstantType.NONE:
            default:
                // Not recognized, supply zero as fallback.
                offloadConstant.value.intValue(0);
        }
        return Collections.singletonList(ResourceObject.of(offloadConstant.name, offloadConstant));
    }

    @Override
    protected List<ResourceObject> toHalModuloMapping(
            com.google.android.clockwork.ambient.offload.types.ModuloMapping aidlType)
            throws DisplayOffloadException {
        checkNameValid(aidlType.name);
        ModuloMapping moduloMapping = new ModuloMapping();
        moduloMapping.name = "" + aidlType.name;
        moduloMapping.modulus = aidlType.modulus;
        return Collections.singletonList(ResourceObject.of(moduloMapping.name, moduloMapping));
    }

    BindableColor toHalBindableColor(
            com.google.android.clockwork.ambient.offload.types.BindableColor in) {
        BindableColor bindableColor = new BindableColor();
        if (in == null) {
            // Black but not transparent
            bindableColor.value(0xFF000000);
        } else if (isArrayLengthOne(in.binding)) {
            bindableColor.binding(getDataSourceOrBinding(in.binding[0]));
        } else {
            bindableColor.value(in.value);
        }
        return bindableColor;
    }

    BindableFloat toHalBindableFloat(
            com.google.android.clockwork.ambient.offload.types.BindableFloat in) {
        BindableFloat bindableFloat = new BindableFloat();
        if (isArrayLengthOne(in.binding)) {
            bindableFloat.binding(getDataSourceOrBinding(in.binding[0]));
        } else {
            bindableFloat.value(in.value);
        }
        return bindableFloat;
    }

    @Override
    protected List<ResourceObject> toHalUnaryOperation(
            UnaryOperation unaryOperation)
            throws DisplayOffloadException {
        List<ResourceObject> resourceObjects = new LinkedList<>();
        if (unaryOperation == null) {
            return resourceObjects;
        }
        final ResourceObject mappingResourceObject;
        final ResourceObject metricResourceObject;

        int operationName = unaryOperation.name;
        String operationOperand = getDataSourceOrBinding(unaryOperation.operand.name);

        switch (unaryOperation.operation) {
            case CEILING:
                CeilMapping ceilMapping = new CeilMapping();
                ceilMapping.name = MAPPING_NAME_CEIL;
                OffloadMetric offloadMetricCeil = getOffloadMetric(operationName,
                        operationOperand, ceilMapping.name);
                mappingResourceObject = ResourceObject.of(ceilMapping.name, ceilMapping);
                metricResourceObject = ResourceObject.of(offloadMetricCeil.name, offloadMetricCeil);
                break;
            case FLOOR:
                FloorMapping floorMapping = new FloorMapping();
                floorMapping.name = MAPPING_NAME_FLOOR;
                OffloadMetric offloadMetricFloor = getOffloadMetric(operationName,
                        operationOperand, floorMapping.name);
                mappingResourceObject = ResourceObject.of(floorMapping.name, floorMapping);
                metricResourceObject = ResourceObject.of(offloadMetricFloor.name,
                        offloadMetricFloor);
                break;
            case RECIPROCAL:
                ReciprocalMapping reciprocalMapping = new ReciprocalMapping();
                reciprocalMapping.name = MAPPING_NAME_RECIPROCAL;
                OffloadMetric offloadMetricReciprocal = getOffloadMetric(operationName,
                        operationOperand, reciprocalMapping.name);
                mappingResourceObject = ResourceObject.of(reciprocalMapping.name,
                        reciprocalMapping);
                metricResourceObject = ResourceObject.of(offloadMetricReciprocal.name,
                        offloadMetricReciprocal);
                break;
            default:
                throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                        String.format("UnaryOperation type %d is not recognized.",
                                unaryOperation.operation));
        }
        resourceObjects.add(mappingResourceObject);
        resourceObjects.add(metricResourceObject);
        return resourceObjects;
    }

    @Override
    List<Integer> getIdReferenced(Object object) {
        // Check if it's a container
        if (object instanceof TranslationGroup) {
            TranslationGroup group = (TranslationGroup) object;
            return group.contents;
        } else if (object instanceof RotationGroup) {
            RotationGroup group = (RotationGroup) object;
            return group.contents;
        } else if (object instanceof StringResource) {
            StringResource stringResource = (StringResource) object;
            return new ArrayList<>(Collections.singletonList(stringResource.font));
        } else if (object instanceof StaticTextAdapter) {
            StaticTextAdapter staticTextAdapter = (StaticTextAdapter) object;
            return new ArrayList<>(Collections.singletonList(staticTextAdapter.getFontId()));
        } else if (object instanceof DynamicTextAdapter) {
            DynamicTextAdapter dynamicTextAdapter = (DynamicTextAdapter) object;
            return new ArrayList<>(Collections.singletonList(dynamicTextAdapter.getFontId()));
        } else {
            return new ArrayList<>();
        }
    }
}
// LINT.ThenChange(HalAdapter.java:hal_send)
