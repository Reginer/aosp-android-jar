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

import static com.android.clockwork.displayoffload.HalTypeConverterV2Utils.binaryOp;
import static com.android.clockwork.displayoffload.HalTypeConverterV2Utils.bindingPtr;
import static com.android.clockwork.displayoffload.HalTypeConverterV2Utils.rotationGroup;
import static com.android.clockwork.displayoffload.HalTypeConverterV2Utils.round;
import static com.android.clockwork.displayoffload.HalTypeConverterV2Utils.ternaryOp;
import static com.android.clockwork.displayoffload.HalTypeConverterV2Utils.translationGroup;
import static com.android.clockwork.displayoffload.HalTypeConverterV2Utils.unaryOp;
import static com.android.clockwork.displayoffload.Utils.checkNameValid;
import static com.android.clockwork.displayoffload.Utils.isArrayLengthOne;

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_CONVERSION_FAILURE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_INVALID_RESOURCE;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.HidlMemoryUtil;
import android.os.SharedMemory;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import com.google.android.clockwork.ambient.offload.types.ArcPathResource;
import com.google.android.clockwork.ambient.offload.types.BindableBoolean;
import com.google.android.clockwork.ambient.offload.types.BindableColor;
import com.google.android.clockwork.ambient.offload.types.BindableFloat;
import com.google.android.clockwork.ambient.offload.types.BitmapResource;
import com.google.android.clockwork.ambient.offload.types.CustomResource;
import com.google.android.clockwork.ambient.offload.types.DynamicTextResource;
import com.google.android.clockwork.ambient.offload.types.LineShapeResource;
import com.google.android.clockwork.ambient.offload.types.LinearMetricMapping;
import com.google.android.clockwork.ambient.offload.types.ModuloMapping;
import com.google.android.clockwork.ambient.offload.types.NumberFormatMapping;
import com.google.android.clockwork.ambient.offload.types.OffloadConstant;
import com.google.android.clockwork.ambient.offload.types.OffloadConstantType;
import com.google.android.clockwork.ambient.offload.types.OffloadMetric;
import com.google.android.clockwork.ambient.offload.types.OffloadRawMetric;
import com.google.android.clockwork.ambient.offload.types.OffloadString;
import com.google.android.clockwork.ambient.offload.types.RangeMapping;
import com.google.android.clockwork.ambient.offload.types.RawMetricType;
import com.google.android.clockwork.ambient.offload.types.RectShapeResource;
import com.google.android.clockwork.ambient.offload.types.RotationGroup;
import com.google.android.clockwork.ambient.offload.types.RoundRectShapeResource;
import com.google.android.clockwork.ambient.offload.types.SpriteSheetPngResource;
import com.google.android.clockwork.ambient.offload.types.StaticTextResource;
import com.google.android.clockwork.ambient.offload.types.TextParam;
import com.google.android.clockwork.ambient.offload.types.TranslationGroup;
import com.google.android.clockwork.ambient.offload.types.TtfFontResource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import vendor.google_clockwork.displayoffload.V2_0.ArcPath;
import vendor.google_clockwork.displayoffload.V2_0.BinaryOperation;
import vendor.google_clockwork.displayoffload.V2_0.BinaryOperationType;
import vendor.google_clockwork.displayoffload.V2_0.BindingPtr;
import vendor.google_clockwork.displayoffload.V2_0.FontParam;
import vendor.google_clockwork.displayoffload.V2_0.LinePath;
import vendor.google_clockwork.displayoffload.V2_0.NumberFormatResource;
import vendor.google_clockwork.displayoffload.V2_0.Primitive;
import vendor.google_clockwork.displayoffload.V2_0.Primitive.PrimitiveSafeUnion;
import vendor.google_clockwork.displayoffload.V2_0.ShapeParam;
import vendor.google_clockwork.displayoffload.V2_0.TernaryOperation;
import vendor.google_clockwork.displayoffload.V2_0.TernaryOperationType;
import vendor.google_clockwork.displayoffload.V2_0.Type;
import vendor.google_clockwork.displayoffload.V2_0.UnaryOperation;

// LINT.IfChange(aidl_type_conversions)
/**
 * Helper class that handles all the conversion between AIDL types and HAL types for DisplayOffload
 * V2.
 */
public class HalTypeConverterV2 extends HalTypeConverter {
    private static final boolean DEBUG_ALLOW_UNHANDLED_RESOURCES = true;
    private final Context mContext;
    private final Map<Object, Primitive> mPrimitiveMap = new ArrayMap<>();
    private final Map<Integer, Set<MetricUsage>> mMappingNameToMetricDataSource = new ArrayMap<>();
    private final Set<Integer> mExistingDataSourceWithCreatedMetric = new ArraySet<>();
    private final Map<Integer, BindingPtr> mKnownBindingPtrs = new ArrayMap<>();
    private final UniqueIdGenerator mIdGen;

    HalTypeConverterV2(Context context) {
        this(context, new UniqueIdGenerator(HalTypeConverter::isSystemResourceId));
    }

    @VisibleForTesting
    HalTypeConverterV2(Context context, UniqueIdGenerator idGenerator) {
        mContext = context;
        mIdGen = idGenerator;
    }

    @Override
    void begin() {
        mIdGen.reset();
        mMappingNameToMetricDataSource.clear();
        mKnownBindingPtrs.clear();
        mPrimitiveMap.clear();
        mExistingDataSourceWithCreatedMetric.clear();
    }

    private <T> BindingPtr primitiveBindingPtr(
            Consumer<ResourceObject> addToList,
            BiConsumer<PrimitiveSafeUnion, T> assignFunc,
            T value) {
        BindingPtr ptr =
                HalTypeConverterV2Utils.primitiveBindingPtr(
                        mPrimitiveMap, addToList, assignFunc, mIdGen.nextId(), value);
        return cacheBindingPtr(ptr);
    }

    private BindingPtr cacheBindingPtr(BindingPtr ptr) {
        BindingPtr cached = mKnownBindingPtrs.get(ptr.id);
        if (cached == null || cached.type != ptr.type) {
            Log.w(TAG, "Type mismatch for id=%d, update to type=%d" + ptr.type);
            cached = ptr;
            mKnownBindingPtrs.put(ptr.id, cached);
        }
        return cached;
    }

    private BindingPtr cacheBindingPtr(int id, short type) {
        return cacheBindingPtr(bindingPtr(id, type));
    }

    private BindingPtr primitiveBindingPtr(Consumer<ResourceObject> addToList, float value) {
        return primitiveBindingPtr(addToList, PrimitiveSafeUnion::floatVal, value);
    }

    private BindingPtr primitiveBindingPtr(Consumer<ResourceObject> addToList, boolean value) {
        return primitiveBindingPtr(addToList, PrimitiveSafeUnion::boolVal, value);
    }

    private BindingPtr primitiveBindingPtr(Consumer<ResourceObject> addToList, int value) {
        return primitiveBindingPtr(addToList, PrimitiveSafeUnion::int32Val, value);
    }

    private BindingPtr bindingPtrForBindable(
            Consumer<ResourceObject> addToList, BindableBoolean in) {
        if (in == null) {
            Log.e(TAG, "Null BindableBoolean, default to true.");
            return primitiveBindingPtr(addToList, true);
        } else if (isArrayLengthOne(in.binding)) {
            int remappedId = mIdGen.getId(in.binding[0]);
            return mKnownBindingPtrs.getOrDefault(remappedId, bindingPtr(remappedId, Type.BOOL));
        } else {
            return primitiveBindingPtr(addToList, in.value);
        }
    }

    private BindingPtr bindingPtrForBindable(Consumer<ResourceObject> addToList, BindableFloat in)
            throws DisplayOffloadException {
        if (in == null) {
            throw new DisplayOffloadException(ERROR_LAYOUT_INVALID_RESOURCE,
                    "Null BindableFloat can't be converted to BindingPtr.");
        } else if (isArrayLengthOne(in.binding)) {
            return cacheBindingPtr(mIdGen.getId(in.binding[0]), Type.FLOAT);
        } else {
            return primitiveBindingPtr(addToList, in.value);
        }
    }

    private BindingPtr bindingPtrForBindable(Consumer<ResourceObject> addToList, BindableColor in) {
        if (in == null) {
            return primitiveBindingPtr(addToList, PrimitiveSafeUnion::uint32Val, 0xFF000000);
        } else if (isArrayLengthOne(in.binding)) {
            return cacheBindingPtr(mIdGen.getId(in.binding[0]), Type.UINT32);
        } else {
            return primitiveBindingPtr(addToList, in.value);
        }
    }

    @Override
    List<ResourceObject> createDummyTranslationGroup(int id, ArrayList<Integer> remappedRootIds) {
        List<ResourceObject> resources = new LinkedList<>();
        // rootIds are already remapped. No need to remap again.
        vendor.google_clockwork.displayoffload.V2_0.TranslationGroup group =
                translationGroup(id,
                        remappedRootIds == null ? new ArrayList<>() : remappedRootIds,
                        primitiveBindingPtr(resources::add, 0.0f),
                        primitiveBindingPtr(resources::add, 0.0f),
                        primitiveBindingPtr(resources::add, true));
        resources.add(ResourceObject.of(group.id, group));
        return resources;
    }

    @Override
    List<ResourceObject> toHalObject(Object in) throws DisplayOffloadException {
        if (in instanceof com.google.android.clockwork.ambient.offload.types.BinaryOperation) {
            return toHalBinaryOperation(
                    (com.google.android.clockwork.ambient.offload.types.BinaryOperation) in);
        }
        if (in instanceof com.google.android.clockwork.ambient.offload.types.TernaryOperation) {
            return toHalTernaryOperation(
                    (com.google.android.clockwork.ambient.offload.types.TernaryOperation) in);
        }
        if (in instanceof LineShapeResource) {
            checkResourceIdRange(((LineShapeResource) in).id);
            return toHalLinePath((LineShapeResource) in);
        }

        List<ResourceObject> out = super.toHalObject(in);
        if (out != null) {
            return out;
        }
        if (!DEBUG_ALLOW_UNHANDLED_RESOURCES) {
            throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                    "Unsupported type: " + in.getClass().getSimpleName());
        } else {
            return null;
        }
    }

    @Override
    List<Integer> getIdReferenced(Object object) throws DisplayOffloadException {
        ArrayList<Integer> refs = new ArrayList<>();
        if (object instanceof vendor.google_clockwork.displayoffload.V2_0.TranslationGroup) {
            vendor.google_clockwork.displayoffload.V2_0.TranslationGroup
                    group = (vendor.google_clockwork.displayoffload.V2_0.TranslationGroup) object;
            refs.addAll(group.contents);
            refs.add(group.visible.id);
            refs.add(group.offsetX.id);
            refs.add(group.offsetY.id);
        } else if (object instanceof vendor.google_clockwork.displayoffload.V2_0.RotationGroup) {
            vendor.google_clockwork.displayoffload.V2_0.RotationGroup
                    group = (vendor.google_clockwork.displayoffload.V2_0.RotationGroup) object;
            refs.addAll(group.contents);
            refs.add(group.visible.id);
            refs.add(group.pivotX.id);
            refs.add(group.pivotY.id);
            refs.add(group.angleDeg.id);
        } else if (object instanceof StaticTextAdapter) {
            StaticTextAdapter staticTextAdapter = (StaticTextAdapter) object;
            refs.add(staticTextAdapter.getVisibility().id);
            refs.add(staticTextAdapter.getColor().id);
            refs.add(staticTextAdapter.getFontId());
        } else if (object instanceof DynamicTextAdapter) {
            DynamicTextAdapter dynamicTextAdapter = (DynamicTextAdapter) object;
            refs.add(dynamicTextAdapter.getVisibility().id);
            refs.add(dynamicTextAdapter.getColor().id);
            refs.add(dynamicTextAdapter.getFontId());
            refs.add(dynamicTextAdapter.getV2().content.id);
        } else if (object instanceof vendor.google_clockwork.displayoffload.V2_0.BitmapDrawable) {
            vendor.google_clockwork.displayoffload.V2_0.BitmapDrawable bmpResource =
                    (vendor.google_clockwork.displayoffload.V2_0.BitmapDrawable) object;
            refs.add(bmpResource.visible.id);
        } else if (object instanceof vendor.google_clockwork.displayoffload.V2_0.UnaryOperation) {
            vendor.google_clockwork.displayoffload.V2_0.UnaryOperation op =
                    (vendor.google_clockwork.displayoffload.V2_0.UnaryOperation) object;
            refs.add(op.arg1.id);
        } else if (object instanceof vendor.google_clockwork.displayoffload.V2_0.BinaryOperation) {
            vendor.google_clockwork.displayoffload.V2_0.BinaryOperation op =
                    (vendor.google_clockwork.displayoffload.V2_0.BinaryOperation) object;
            refs.add(op.arg1.id);
            refs.add(op.arg2.id);
        } else if (object instanceof vendor.google_clockwork.displayoffload.V2_0.TernaryOperation) {
            vendor.google_clockwork.displayoffload.V2_0.TernaryOperation op =
                    (vendor.google_clockwork.displayoffload.V2_0.TernaryOperation) object;
            refs.add(op.arg1.id);
            refs.add(op.arg2.id);
            refs.add(op.arg3.id);
        } else if (object instanceof vendor.google_clockwork.displayoffload.V2_0.RectShape) {
            vendor.google_clockwork.displayoffload.V2_0.RectShape o =
                    (vendor.google_clockwork.displayoffload.V2_0.RectShape) object;
            refs.add(o.visible.id);
            refs.add(o.width.id);
            refs.add(o.height.id);
            refs.addAll(getIdReferenced(o.shapeParam));
        } else if (object instanceof vendor.google_clockwork.displayoffload.V2_0.RoundRectShape) {
            vendor.google_clockwork.displayoffload.V2_0.RoundRectShape o =
                    (vendor.google_clockwork.displayoffload.V2_0.RoundRectShape) object;
            refs.add(o.visible.id);
            refs.add(o.width.id);
            refs.add(o.height.id);
            refs.add(o.cornerRadius.id);
            refs.addAll(getIdReferenced(o.shapeParam));
        } else if (object instanceof vendor.google_clockwork.displayoffload.V2_0.LinePath) {
            vendor.google_clockwork.displayoffload.V2_0.LinePath o =
                    (vendor.google_clockwork.displayoffload.V2_0.LinePath) object;
            refs.add(o.visible.id);
            refs.add(o.x.id);
            refs.add(o.y.id);
            refs.addAll(getIdReferenced(o.shapeParam));
        } else if (object instanceof vendor.google_clockwork.displayoffload.V2_0.ArcPath) {
            vendor.google_clockwork.displayoffload.V2_0.ArcPath o =
                    (vendor.google_clockwork.displayoffload.V2_0.ArcPath) object;
            refs.add(o.visible.id);
            refs.add(o.width.id);
            refs.add(o.height.id);
            refs.add(o.startDeg.id);
            refs.add(o.sweepDeg.id);
            refs.addAll(getIdReferenced(o.shapeParam));
        } else if (object instanceof vendor.google_clockwork.displayoffload.V2_0.ShapeParam) {
            vendor.google_clockwork.displayoffload.V2_0.ShapeParam o =
                    (vendor.google_clockwork.displayoffload.V2_0.ShapeParam) object;
            refs.add(o.color.id);
        }
        return refs;
    }

    @Override
    protected List<ResourceObject> toHalCustomResource(CustomResource aidlType)
            throws DisplayOffloadException {
        if (aidlType.keyValues == null) {
            throw new DisplayOffloadException(
                    ERROR_LAYOUT_CONVERSION_FAILURE, "Failed to convert: " + aidlType.id);
        }
        vendor.google_clockwork.displayoffload.V2_0.CustomResource customResource
                = new vendor.google_clockwork.displayoffload.V2_0.CustomResource();
        customResource.id = mIdGen.getId(aidlType.id);
        customResource.keyValues = HalTypeConverterUtils.toHalKVPairArrayList(aidlType.keyValues);
        return Collections.singletonList(ResourceObject.of(customResource.id, customResource));
    }

    // TODO(b/259152785): Make xValue & yValue bindable float, like degValue. Write test.
    @Override
    protected List<ResourceObject> toHalTranslationGroup(TranslationGroup aidlType)
            throws DisplayOffloadException {
        List<ResourceObject> resources = new LinkedList<>();
        vendor.google_clockwork.displayoffload.V2_0.TranslationGroup group =
                translationGroup(
                        mIdGen.getId(aidlType.id),
                        Utils.convertToArrayListInteger(
                                Arrays.stream(aidlType.contents).map(mIdGen::getId).toArray()),
                        bindingPtrForBindable(resources::add, aidlType.offsetX),
                        bindingPtrForBindable(resources::add, aidlType.offsetY),
                        bindingPtrForBindable(resources::add, aidlType.visibility));
        resources.add(ResourceObject.of(group.id, group));
        return resources;
    }

    @Override
    protected List<ResourceObject> toHalRotationGroup(RotationGroup aidlType)
            throws DisplayOffloadException {
        List<ResourceObject> resources = new LinkedList<>();
        vendor.google_clockwork.displayoffload.V2_0.RotationGroup group =
                rotationGroup(
                        mIdGen.getId(aidlType.id),
                        Utils.convertToArrayListInteger(
                                Arrays.stream(aidlType.contents).map(mIdGen::getId).toArray()),
                        bindingPtrForBindable(resources::add, aidlType.pivotX),
                        bindingPtrForBindable(resources::add, aidlType.pivotY),
                        bindingPtrForBindable(resources::add, aidlType.angleDeg),
                        bindingPtrForBindable(resources::add, aidlType.visibility));
        resources.add(ResourceObject.of(group.id, group));
        return resources;
    }

    @Override
    protected List<ResourceObject> toHalBitmapResource(BitmapResource aidlType)
            throws DisplayOffloadException {
        Bitmap bitmap = aidlType.icon.getBitmap();
        // if it is not a bitmap then load the icon as a drawable and draw it into an 8888 bitmap
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(aidlType.width, aidlType.height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Drawable drawable = aidlType.icon.loadDrawable(mContext);
            drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
            drawable.draw(canvas);
        }

        // assert that the bitmaps width and height are equal to the aidl values
        if (bitmap.getWidth() != aidlType.width || bitmap.getHeight() != aidlType.height) {
            throw new DisplayOffloadException(
                    ERROR_LAYOUT_INVALID_RESOURCE, "Incorrect bitmap dimensions: " + aidlType.id);
        }

        List<ResourceObject> resources = new LinkedList<>();
        vendor.google_clockwork.displayoffload.V2_0.BitmapDrawable bmpResource =
                new vendor.google_clockwork.displayoffload.V2_0.BitmapDrawable();
        vendor.google_clockwork.displayoffload.V2_0.Bitmap halBitmap =
                new vendor.google_clockwork.displayoffload.V2_0.Bitmap();

        switch (bitmap.getConfig()) {
            case ALPHA_8:
                halBitmap.format = vendor.google_clockwork.displayoffload.V2_0.PixelFormat.ALPHA_8;
                break;
            case RGB_565:
                halBitmap.format = vendor.google_clockwork.displayoffload.V2_0.PixelFormat.RGB_565;
                break;
            case ARGB_8888:
                halBitmap.format =
                        vendor.google_clockwork.displayoffload.V2_0.PixelFormat.RGBA_8888;
                break;
            default:
                halBitmap.format =
                        vendor.google_clockwork.displayoffload.V2_0.PixelFormat.RGBA_8888;
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false).asShared();
        }

        SharedMemory shmem = bitmap.getSharedMemory();
        if (shmem != null && shmem.getFileDescriptor().valid()) {
            halBitmap.data = HidlMemoryUtil.sharedMemoryToHidlMemory(shmem);
        } else {
            int size = bitmap.getRowBytes() * bitmap.getHeight();
            ByteBuffer byteBuffer = ByteBuffer.allocate(size);
            bitmap.copyPixelsToBuffer(byteBuffer);
            halBitmap.data = HidlMemoryUtil.byteArrayToHidlMemory(byteBuffer.array());
        }

        bmpResource.id = mIdGen.getId(aidlType.id);
        bmpResource.visible = bindingPtrForBindable(resources::add, aidlType.visibility);
        bmpResource.color = bindingPtrForBindable(resources::add, aidlType.color);
        bmpResource.blendMode = aidlType.blendMode;

        halBitmap.width = bitmap.getWidth();
        halBitmap.height = bitmap.getHeight();
        halBitmap.rowBytes = bitmap.getRowBytes();
        bmpResource.bitmap = halBitmap;

        resources.add(ResourceObject.of(bmpResource.id, bmpResource));
        return resources;
    }

    @Override
    protected List<ResourceObject> toHalRectShapeResource(RectShapeResource aidlType)
            throws DisplayOffloadException {
        List<ResourceObject> resources = new LinkedList<>();
        vendor.google_clockwork.displayoffload.V2_0.RectShape rectShape
                = new vendor.google_clockwork.displayoffload.V2_0.RectShape();
        rectShape.id = mIdGen.getId(aidlType.id);
        rectShape.visible = bindingPtrForBindable(resources::add, aidlType.visibility);
        rectShape.shapeParam = toHalShapeParam(resources::add, aidlType.shapeParam);
        rectShape.width = bindingPtrForBindable(resources::add, aidlType.width);
        rectShape.height = bindingPtrForBindable(resources::add, aidlType.height);
        resources.add(ResourceObject.of(rectShape.id, rectShape));
        return resources;
    }

    @Override
    protected List<ResourceObject> toHalRoundRectShapeResource(RoundRectShapeResource aidlType)
            throws DisplayOffloadException {
        List<ResourceObject> resources = new LinkedList<>();
        vendor.google_clockwork.displayoffload.V2_0.RoundRectShape roundRectShape
                = new vendor.google_clockwork.displayoffload.V2_0.RoundRectShape();
        roundRectShape.id = mIdGen.getId(aidlType.id);
        roundRectShape.visible = bindingPtrForBindable(resources::add, aidlType.visibility);
        roundRectShape.shapeParam = toHalShapeParam(resources::add, aidlType.shapeParam);
        roundRectShape.width = bindingPtrForBindable(resources::add, aidlType.width);
        roundRectShape.height = bindingPtrForBindable(resources::add, aidlType.height);
        roundRectShape.cornerRadius = bindingPtrForBindable(resources::add, aidlType.cornerRadius);
        resources.add(ResourceObject.of(roundRectShape.id, roundRectShape));
        return resources;
    }

    @Override
    protected List<ResourceObject> toHalLinePath(LineShapeResource aidlType)
            throws DisplayOffloadException {
        List<ResourceObject> resources = new LinkedList<>();
        LinePath linePath = new LinePath();
        linePath.id = mIdGen.getId(aidlType.id);
        linePath.visible = bindingPtrForBindable(resources::add, aidlType.visibility);
        linePath.shapeParam = toHalShapeParam(resources::add, aidlType.shapeParam);
        linePath.x = bindingPtrForBindable(resources::add, aidlType.endX);
        linePath.y = bindingPtrForBindable(resources::add, aidlType.endY);
        resources.add(ResourceObject.of(linePath.id, linePath));
        return resources;
    }

    private ShapeParam toHalShapeParam(
            Consumer<ResourceObject> addToList,
            com.google.android.clockwork.ambient.offload.types.ShapeParam aidlType) {
        if (aidlType == null) {
            return null;
        }
        ShapeParam shapeParam = new ShapeParam();
        shapeParam.color = bindingPtrForBindable(addToList, aidlType.color);
        shapeParam.strokeWidth = aidlType.strokeWidth;
        shapeParam.style = aidlType.style;
        // TODO(b/241795879): handle "isAA".
        shapeParam.isAA = true;
        shapeParam.blendMode = aidlType.blendMode;
        return shapeParam;
    }

    @Override
    protected List<ResourceObject> toHalArcPathResource(ArcPathResource aidlType)
            throws DisplayOffloadException {
        List<ResourceObject> resources = new LinkedList<>();
        ArcPath arcPath = new ArcPath();
        arcPath.id = mIdGen.getId(aidlType.id);
        arcPath.visible = bindingPtrForBindable(resources::add, aidlType.visibility);
        arcPath.width = bindingPtrForBindable(resources::add, aidlType.width);
        arcPath.height = bindingPtrForBindable(resources::add, aidlType.height);
        arcPath.startDeg = bindingPtrForBindable(resources::add, aidlType.startDeg);
        arcPath.sweepDeg = bindingPtrForBindable(resources::add, aidlType.sweepDeg);
        arcPath.shapeParam = toHalShapeParam(resources::add, aidlType.shapeParam);
        arcPath.endCapStyle = aidlType.endCapStyle;
        resources.add(ResourceObject.of(arcPath.id, arcPath));
        return resources;
    }

    @Override
    public List<ResourceObject> toHalTtfFontResource(TtfFontResource aidlType)
            throws DisplayOffloadException {
        vendor.google_clockwork.displayoffload.V2_0.TtfFontResource ttfFontResource =
                new vendor.google_clockwork.displayoffload.V2_0.TtfFontResource();

        ttfFontResource.id = mIdGen.getId(aidlType.id);
        // Create an empty buffer for safety, to be replaced by subsetted ttf.
        ttfFontResource.ttf = new ArrayList<>();

        return Collections.singletonList(ResourceObject.of(ttfFontResource.id,
                new TtfFontAdapter(ttfFontResource)));
    }

    @Override
    protected List<ResourceObject> toHalSpriteSheetPngResource(SpriteSheetPngResource aidlType)
            throws DisplayOffloadException {
        // No longer supported
        return null;
    }

    @Override
    protected List<ResourceObject> toHalOffloadString(OffloadString aidlType)
            throws DisplayOffloadException {
        // No longer supported
        return null;
    }

    @Override
    List<ResourceObject> toHalStaticText(StaticTextResource aidlType)
            throws DisplayOffloadException {
        if (aidlType.textParam == null) {
            throw new DisplayOffloadException(
                    ERROR_LAYOUT_CONVERSION_FAILURE, "Failed to convert: " + aidlType.id);
        }
        List<ResourceObject> resources = new LinkedList<>();

        vendor.google_clockwork.displayoffload.V2_0.StaticText
                staticText = new vendor.google_clockwork.displayoffload.V2_0.StaticText();
        staticText.id = mIdGen.getId(aidlType.id);
        staticText.visible = bindingPtrForBindable(resources::add, aidlType.visibility);
        staticText.color = bindingPtrForBindable(resources::add, aidlType.textParam.color);
        staticText.originalString = aidlType.value;
        staticText.fontParam = toHalFontParam(aidlType.textParam);

        resources.add(ResourceObject.of(staticText.id, new StaticTextAdapter(staticText)));
        return resources;
    }

    @Override
    protected List<ResourceObject> toHalDynamicText(DynamicTextResource aidlType)
            throws DisplayOffloadException {
        if (aidlType.textParam == null) {
            throw new DisplayOffloadException(
                    ERROR_LAYOUT_CONVERSION_FAILURE, "Failed to convert: " + aidlType.id);
        }
        List<ResourceObject> resources = new LinkedList<>();

        vendor.google_clockwork.displayoffload.V2_0.DynamicText
                dynamicText = new vendor.google_clockwork.displayoffload.V2_0.DynamicText();
        dynamicText.id = mIdGen.getId(aidlType.id);
        dynamicText.visible = bindingPtrForBindable(resources::add, aidlType.visibility);
        int contentId = mIdGen.getId(aidlType.binding);
        dynamicText.content = cacheBindingPtr(contentId, Type.STRING);
        dynamicText.color = bindingPtrForBindable(resources::add, aidlType.textParam.color);
        dynamicText.fontParam = toHalFontParam(aidlType.textParam);

        resources.add(ResourceObject.of(dynamicText.id, new DynamicTextAdapter(dynamicText)));
        return resources;
    }

    private FontParam toHalFontParam(TextParam textParam) {
        FontParam fontParam = new FontParam();
        fontParam.ttfFont = mIdGen.getId(textParam.ttfFont);
        fontParam.ttfFontSize = textParam.ttfFontSize;
        // TODO(b/241795879): Handle Hal FontParam.ttfFontWeight
        return fontParam;
    }

    @Override
    protected List<ResourceObject> toHalOffloadRawMetric(OffloadRawMetric rawMetric)
            throws DisplayOffloadException {
        if (rawMetric.rawMetricType == RawMetricType.DATA_SOURCE) {
            return extractDataSourceInfo(rawMetric);
        } else if (rawMetric.rawMetricType == RawMetricType.EXPRESSION) {
            throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                    "Offload Expression is not supported in V2.");
        }
        throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                "Unknown RawMetricType = " + rawMetric.rawMetricType);
    }

    private List<ResourceObject> extractDataSourceInfo(OffloadRawMetric aidlType)
            throws DisplayOffloadException {
        if (TextUtils.isEmpty(aidlType.data)) {
            throw new DisplayOffloadException(
                    ERROR_LAYOUT_CONVERSION_FAILURE, "Failed to convert: " + aidlType.name);
        }
        List<ResourceObject> resourceObjects = new LinkedList<>();
        String dataSource = aidlType.data;
        int dataSourceId = mIdGen.getId(dataSource);
        // Assigned id for current data source is duplicate with an already created metric.
        if (!mExistingDataSourceWithCreatedMetric.contains(dataSourceId)) {
            vendor.google_clockwork.displayoffload.V2_0.OffloadMetric offloadMetric =
                    new vendor.google_clockwork.displayoffload.V2_0.OffloadMetric();
            offloadMetric.id = mIdGen.getId(aidlType.name);
            offloadMetric.target = dataSource;
            resourceObjects.add(ResourceObject.of(offloadMetric.id, offloadMetric));
            mExistingDataSourceWithCreatedMetric.add(dataSourceId);
        }
        return resourceObjects;
    }

    @Override
    protected List<ResourceObject> toHalOffloadMetric(OffloadMetric aidlType)
            throws DisplayOffloadException {
        mMappingNameToMetricDataSource.compute(
                mIdGen.getId(aidlType.mapping),
                (key, val) -> {
                    if (val == null) {
                        val = new ArraySet<>();
                    }
                    MetricUsage metricUsage = new MetricUsage();
                    metricUsage.mMetricName = mIdGen.getId(aidlType.name);
                    metricUsage.mArgument = mIdGen.getId(aidlType.boundDataSource);
                    val.add(metricUsage);
                    return val;
                });
        return new LinkedList<>();
    }

    @Override
    protected List<ResourceObject> toHalLinearMetricMapping(LinearMetricMapping aidlType)
            throws DisplayOffloadException {
        Set<MetricUsage> usages =
                mMappingNameToMetricDataSource.get(mIdGen.getId(checkNameValid(aidlType.name)));
        if (usages == null || usages.size() == 0) {
            // Unused mapping
            return new LinkedList<>();
        }

        List<ResourceObject> resourceObjects = new LinkedList<>();
        BindingPtr b = primitiveBindingPtr(resourceObjects::add, aidlType.b);
        BindingPtr m = primitiveBindingPtr(resourceObjects::add, aidlType.m);
        for (MetricUsage usage : usages) {
            // Always mount primitive at arg1
            BinaryOperation multiplication =
                    binaryOp(
                            mIdGen.nextId(),
                            BinaryOperationType.MULTIPLY,
                            m,
                            bindingPtr(usage.mArgument, Type.FLOAT));
            BinaryOperation addition =
                    binaryOp(
                            usage.mMetricName,
                            BinaryOperationType.ADD,
                            b,
                            bindingPtr(multiplication.id, Type.FLOAT));
            resourceObjects.add(ResourceObject.of(multiplication.id, multiplication));
            resourceObjects.add(ResourceObject.of(addition.id, addition));

            cacheBindingPtr(addition.id, Type.FLOAT);
        }

        return resourceObjects;
    }

    @Override
    protected List<ResourceObject> toHalRangeMapping(RangeMapping aidlType)
            throws DisplayOffloadException {
        Set<MetricUsage> usages =
                mMappingNameToMetricDataSource.get(mIdGen.getId(checkNameValid(aidlType.name)));
        if (usages == null || usages.size() == 0) {
            // Unused mapping
            return new LinkedList<>();
        }

        if (aidlType.thresholds == null
                || aidlType.val == null
                || aidlType.thresholds.length + 1 != aidlType.val.length) {
            throw new DisplayOffloadException(
                    ERROR_LAYOUT_CONVERSION_FAILURE, "Failed to convert: " + aidlType.name);
        }

        // Try checking if we know the type of aidlType.val[0]
        // Note: we won't be able to know the exact type if there are chained range mappings.
        BindingPtr bindingPtr = mKnownBindingPtrs.get(mIdGen.getId(aidlType.val[0]));
        short chainType = bindingPtr == null ? Type.FLOAT : bindingPtr.type;

        int thresholdsLength = aidlType.thresholds.length;
        List<ResourceObject> resourceObjects = new LinkedList<>();
        for (MetricUsage usage : usages) {
            BindingPtr input = bindingPtr(usage.mArgument, Type.FLOAT);

            BindingPtr falseVal =
                    bindingPtr(mIdGen.getId(aidlType.val[thresholdsLength]), chainType);
            for (int i = thresholdsLength - 1; i >= 0; i--) {
                BindingPtr threshold =
                        primitiveBindingPtr(resourceObjects::add, aidlType.thresholds[i]);
                BindingPtr trueVal = bindingPtr(mIdGen.getId(aidlType.val[i]), chainType);
                BinaryOperation lessThan =
                        binaryOp(mIdGen.nextId(), BinaryOperationType.LESS_THAN, input, threshold);
                TernaryOperation current = new TernaryOperation();
                current.id = (i == 0) ? usage.mMetricName : mIdGen.nextId();
                current.op = TernaryOperationType.IF_ELSE;
                current.arg1 = bindingPtr(lessThan, Type.BOOL);
                current.arg2 = trueVal;
                current.arg3 = falseVal;
                resourceObjects.add(ResourceObject.of(lessThan.id, lessThan));
                resourceObjects.add(ResourceObject.of(current.id, current));

                falseVal = bindingPtr(current.id, chainType);
            }
            cacheBindingPtr(usage.mMetricName, chainType);
        }
        return resourceObjects;
    }

    @Override
    protected List<ResourceObject> toHalNumberFormatMapping(NumberFormatMapping aidlType)
            throws DisplayOffloadException {
        List<ResourceObject> resourceObjects = new LinkedList<>();
        NumberFormatResource numberFormatResource = new NumberFormatResource();
        numberFormatResource.id = mIdGen.getId(aidlType.name);
        numberFormatResource.grouping = aidlType.grouping;
        numberFormatResource.maximumFractionDigits = (byte) aidlType.maximumFractionDigits;
        numberFormatResource.minimumFractionDigits = (byte) aidlType.minimumFractionDigits;
        numberFormatResource.minimumIntegerDigits = (byte) aidlType.minimumIntegerDigits;
        numberFormatResource.zeroPadding = false;
        resourceObjects.add(ResourceObject.of(numberFormatResource.id, numberFormatResource));

        Set<MetricUsage> usages =
                mMappingNameToMetricDataSource.get(mIdGen.getId(checkNameValid(aidlType.name)));
        if (usages == null || usages.size() == 0) {
            // Unused mapping
            return resourceObjects;
        }

        for (MetricUsage usage : usages) {
            BinaryOperation formatTextOp =
                    binaryOp(
                            usage.mMetricName,
                            BinaryOperationType.NUMBER_FORMAT,
                            bindingPtr(numberFormatResource.id, Type.NUMBER_FORMAT),
                            bindingPtr(usage.mArgument, Type.FLOAT));
            resourceObjects.add(ResourceObject.of(formatTextOp.id, formatTextOp));
            cacheBindingPtr(formatTextOp.id, Type.STRING);
        }
        return resourceObjects;
    }

    @Override
    protected List<ResourceObject> toHalOffloadConstant(OffloadConstant aidlType)
            throws DisplayOffloadException {
        List<ResourceObject> resourceObjects = new LinkedList<>();
        int id = mIdGen.getId(checkNameValid(aidlType.name));
        switch (aidlType.valueType) {
            case OffloadConstantType.FLOAT:
                cacheBindingPtr(
                        HalTypeConverterV2Utils.primitiveBindingPtr(
                                mPrimitiveMap,
                                resourceObjects::add,
                                PrimitiveSafeUnion::floatVal,
                                id,
                                aidlType.floatValue));
                break;
            case OffloadConstantType.INT32:
                cacheBindingPtr(
                        HalTypeConverterV2Utils.primitiveBindingPtr(
                                mPrimitiveMap,
                                resourceObjects::add,
                                PrimitiveSafeUnion::int32Val,
                                id,
                                aidlType.intValue));
                break;
            case OffloadConstantType.STRING:
            case OffloadConstantType.NONE:
            default:
                // Not supported, supply zero as fallback.
                cacheBindingPtr(
                        HalTypeConverterV2Utils.primitiveBindingPtr(
                                mPrimitiveMap,
                                resourceObjects::add,
                                PrimitiveSafeUnion::int32Val,
                                id,
                                0));
                break;
        }
        return resourceObjects;
    }

    @Override
    protected List<ResourceObject> toHalModuloMapping(ModuloMapping aidlType)
            throws DisplayOffloadException {
        Set<MetricUsage> usages =
                mMappingNameToMetricDataSource.get(mIdGen.getId(checkNameValid(aidlType.name)));
        if (usages == null || usages.size() == 0) {
            // Unused mapping
            return new LinkedList<>();
        }

        List<ResourceObject> resourceObjects = new LinkedList<>();
        BindingPtr m = primitiveBindingPtr(resourceObjects::add, aidlType.modulus);
        for (MetricUsage usage : usages) {
            UnaryOperation toIntOp =
                    round(mIdGen.nextId(), bindingPtr(usage.mArgument, Type.FLOAT));
            // Always mount primitive at arg1
            BinaryOperation moduloOp =
                    binaryOp(
                            usage.mMetricName,
                            BinaryOperationType.MODULO,
                            m,
                            bindingPtr(toIntOp.id, Type.INT32));
            resourceObjects.add(ResourceObject.of(toIntOp.id, toIntOp));
            resourceObjects.add(ResourceObject.of(moduloOp.id, moduloOp));
            cacheBindingPtr(moduloOp.id, Type.INT32);
        }
        return resourceObjects;
    }

    @Override
    protected List<ResourceObject> toHalUnaryOperation(
            com.google.android.clockwork.ambient.offload.types.UnaryOperation aidlType) {
        List<ResourceObject> resourceObjects = new LinkedList<>();

        BindingPtr operandBindingPtr =
                bindingPtr(mIdGen.getId(aidlType.operand.name), (short) aidlType.operand.type);

        UnaryOperation unaryOperation =
                unaryOp(mIdGen.getId(aidlType.name), (byte) aidlType.operation, operandBindingPtr);

        resourceObjects.add(ResourceObject.of(unaryOperation.id, unaryOperation));
        return resourceObjects;
    }

    // Should put this at last to make sure other Objects' BindingPtr were created and stored.
    @Override
    protected List<ResourceObject> toHalBinaryOperation(
            com.google.android.clockwork.ambient.offload.types.BinaryOperation aidlType) {
        List<ResourceObject> resourceObjects = new LinkedList<>();

        BindingPtr arg1BindingPtr =
                bindingPtr(mIdGen.getId(aidlType.arg1.name), (short) aidlType.arg1.type);
        BindingPtr arg2BindingPtr =
                bindingPtr(mIdGen.getId(aidlType.arg2.name), (short) aidlType.arg2.type);

        BinaryOperation binaryOperation =
                binaryOp(
                        mIdGen.getId(aidlType.name),
                        (byte) aidlType.opType,
                        arg1BindingPtr,
                        arg2BindingPtr);

        resourceObjects.add(ResourceObject.of(binaryOperation.id, binaryOperation));
        return resourceObjects;
    }

    @Override
    protected List<ResourceObject> toHalTernaryOperation(
            com.google.android.clockwork.ambient.offload.types.TernaryOperation aidlType) {
        List<ResourceObject> resourceObjects = new LinkedList<>();

        BindingPtr conditionBindingPtr =
                bindingPtr(mIdGen.getId(aidlType.condition.name), (short) aidlType.condition.type);
        BindingPtr ifTrueBindingPtr =
                bindingPtr(mIdGen.getId(aidlType.ifTrue.name), (short) aidlType.ifTrue.type);
        BindingPtr ifFalseBindingPtr =
                bindingPtr(mIdGen.getId(aidlType.ifFalse.name), (short) aidlType.ifFalse.type);

        TernaryOperation ternaryOperation =
                ternaryOp(
                        mIdGen.getId(aidlType.name),
                        conditionBindingPtr,
                        ifTrueBindingPtr,
                        ifFalseBindingPtr);

        resourceObjects.add(ResourceObject.of(ternaryOperation.id, ternaryOperation));
        return resourceObjects;
    }

    private static class MetricUsage {
        int mMetricName;
        int mArgument;

        public static MetricUsage of(int metricId, int argument) {
            MetricUsage m = new MetricUsage();
            m.mMetricName = metricId;
            m.mArgument = argument;
            return m;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof MetricUsage) {
                return mMetricName == ((MetricUsage) o).mMetricName
                        && mArgument == ((MetricUsage) o).mArgument;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return mMetricName;
        }

        @Override
        public String toString() {
            return "[MetricUsage: key=" + mMetricName + "]";
        }
    }
}
// LINT.ThenChange(HalAdapter.java:hal_send)
