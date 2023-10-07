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

import static com.android.clockwork.displayoffload.DebugUtils.DEBUG_DATA_SNAPSHOT;

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_CONVERSION_FAILURE;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.RESOURCE_ID_EMPTY_LAYOUT;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.RESOURCE_ID_STATUS_BAR_BITMAP;
import static com.google.android.clockwork.ambient.offload.IDisplayOffloadService.RESOURCE_ID_STATUS_BAR_GROUP;

import android.os.Bundle;
import android.util.Log;

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
import com.google.android.clockwork.ambient.offload.types.TernaryOperation;
import com.google.android.clockwork.ambient.offload.types.TranslationGroup;
import com.google.android.clockwork.ambient.offload.types.TtfFontResource;
import com.google.android.clockwork.ambient.offload.types.UnaryOperation;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import vendor.google_clockwork.displayoffload.V1_0.KeyValuePair;

/**
 * Abstract class that handles all the conversion from AIDL types.
 */
public abstract class HalTypeConverter {
    protected static final String TAG = "DOHalTypeConverter";

    private static final int RESOURCE_ID_MIN = 0;
    private static final int RESOURCE_ID_MAX = 0x10000; // 65536

    protected static boolean isSystemResourceId(int id) {
        switch (id) {
            case RESOURCE_ID_STATUS_BAR_BITMAP:
            case RESOURCE_ID_STATUS_BAR_GROUP:
            case RESOURCE_ID_EMPTY_LAYOUT:
                return true;
        }
        return false;
    }

    void addEmptyTranslationGroup(HalResourceStore halResourceStore)
            throws DisplayOffloadException {
        halResourceStore.addReplaceResource(
                createDummyTranslationGroup(RESOURCE_ID_EMPTY_LAYOUT, null));
    }

    protected void checkResourceIdRange(int id) throws DisplayOffloadException {
        if (HalTypeConverter.isSystemResourceId(id)) return;

        if (id < RESOURCE_ID_MIN || id >= RESOURCE_ID_MAX) {
            throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                    String.format("Resource id %d is out of bound.", id));
        }
    }

    protected abstract List<ResourceObject> toHalUnaryOperation(
            UnaryOperation unaryOperation) throws DisplayOffloadException;

    abstract List<Integer> getIdReferenced(Object object) throws DisplayOffloadException;

    // V2
    protected List<ResourceObject> toHalBinaryOperation(
            BinaryOperation binaryOperation) throws DisplayOffloadException {
        return null;
    }

    protected List<ResourceObject> toHalTernaryOperation(
            TernaryOperation ternaryOperation) throws DisplayOffloadException {
        return null;
    }

    protected List<ResourceObject> toHalLinePath(
            LineShapeResource lineShapeResource) throws DisplayOffloadException {
        return null;
    }

    /**
     * Create a dummy TranslationGroup object that is either used as a virtual root while
     * generating order for sending resources or as an empty TranslationGroup object that has no
     * children.
     * When argument is null, the method returns an empty TranslationGroup, otherwise, it returns
     * virtual root TranslationGroup.
     *
     * @param remappedRootIds a list of (remapped)integer ids of other Translation/RotationGroup.
     *                        These ids are remapped by UniqueIdGenerator.
     * @return A TranslationGroup object.
     */
    abstract List<ResourceObject> createDummyTranslationGroup(int id,
            ArrayList<Integer> remappedRootIds)
            throws DisplayOffloadException;

    /**
     * Signal HalTypeConverter to prepare to start a conversion session.
     */
    void begin() {
    }

    /**
     * Signal HalTypeConverter to clean up and end a conversion session.
     */
    void end() {
    }

    // TODO(b/241309329): refactor to make hal type conversion batch operations.
    // LINT.IfChange
    // Convert single aidl object to hal object and related resources(e.g. bindings).
    List<ResourceObject> toHalObject(Object in) throws DisplayOffloadException {
        if (in instanceof CustomResource) {
            checkResourceIdRange(((CustomResource) in).id);
            return toHalCustomResource((CustomResource) in);
        } else if (in instanceof TranslationGroup) {
            checkResourceIdRange(((TranslationGroup) in).id);
            return toHalTranslationGroup((TranslationGroup) in);
        } else if (in instanceof RotationGroup) {
            checkResourceIdRange(((RotationGroup) in).id);
            return toHalRotationGroup((RotationGroup) in);
        } else if (in instanceof BitmapResource) {
            checkResourceIdRange(((BitmapResource) in).id);
            return toHalBitmapResource((BitmapResource) in);
        } else if (in instanceof ArcPathResource) {
            checkResourceIdRange(((ArcPathResource) in).id);
            return toHalArcPathResource((ArcPathResource) in);
        } else if (in instanceof RectShapeResource) {
            checkResourceIdRange(((RectShapeResource) in).id);
            return toHalRectShapeResource((RectShapeResource) in);
        } else if (in instanceof RoundRectShapeResource) {
            checkResourceIdRange(((RoundRectShapeResource) in).id);
            return toHalRoundRectShapeResource((RoundRectShapeResource) in);
        } else if (in instanceof SpriteSheetPngResource) {
            checkResourceIdRange(((SpriteSheetPngResource) in).id);
            return toHalSpriteSheetPngResource((SpriteSheetPngResource) in);
        } else if (in instanceof OffloadString) {
            return toHalOffloadString((OffloadString) in);
        } else if (in instanceof OffloadMetric) {
            return toHalOffloadMetric((OffloadMetric) in);
        } else if (in instanceof OffloadRawMetric) {
            return toHalOffloadRawMetric((OffloadRawMetric) in);
        } else if (in instanceof OffloadConstant) {
            return toHalOffloadConstant((OffloadConstant) in);
        } else if (in instanceof LinearMetricMapping) {
            return toHalLinearMetricMapping((LinearMetricMapping) in);
        } else if (in instanceof RangeMapping) {
            return toHalRangeMapping((RangeMapping) in);
        } else if (in instanceof ModuloMapping) {
            return toHalModuloMapping((ModuloMapping) in);
        } else if (in instanceof NumberFormatMapping) {
            return toHalNumberFormatMapping((NumberFormatMapping) in);
        } else if (in instanceof DynamicTextResource) {
            return toHalDynamicText((DynamicTextResource) in);
        } else if (in instanceof UnaryOperation) {
            return toHalUnaryOperation((UnaryOperation) in);
        }
        return null;
    }
    // LINT.ThenChange(:aidl_type_conversions)

    // Convert all aidl objects in the argument List to hal objects and their related resources.
    List<ResourceObject> toHalObject(List<Object> objects) throws DisplayOffloadException {
        List<ResourceObject> resourceObjects = new LinkedList<>();
        for (Object obj : objects) {
            List<ResourceObject> partialResult = toHalObject(obj);
            if (partialResult != null) {
                resourceObjects.addAll(partialResult);
            }
        }
        return resourceObjects;
    }

    Bundle callbackBundleFromAnnotatedKeyValuePairs(ArrayList<KeyValuePair> keyValuePair) {
        Bundle bundle = new Bundle();
        if (keyValuePair == null) {
            Log.i(TAG, "Data snapshot is empty.");
            return bundle;
        }

        if (DEBUG_DATA_SNAPSHOT) {
            for (KeyValuePair kv : keyValuePair) {
                Log.d(TAG, "key=" + kv.key + ", value=" + kv.value.stream().map(
                        (v) -> String.format("0x%02X", v)).collect(
                        Collectors.joining(",", "[", "]")));
            }
        }

        for (KeyValuePair kv : keyValuePair) {
            String key = kv.key;
            int i = key.indexOf(":");
            boolean isRawType = i < 0 || kv.value.size() == 0;
            byte[] byteArray = Utils.convertToByteArray(kv.value);
            String actualKey = key.substring(i + 1);
            switch (key.substring(0, i)) {
                case "uint8":
                case "int8":
                    if (byteArray.length != 1) {
                        isRawType = true;
                        break;
                    }
                    bundle.putByte(actualKey, ByteBuffer.wrap(byteArray).get());
                    break;
                case "uint16":
                case "int16":
                    if (byteArray.length != 2) {
                        isRawType = true;
                        break;
                    }
                    bundle.putShort(actualKey, ByteBuffer.wrap(byteArray).getShort());
                case "int32":
                case "uint32":
                    if (byteArray.length != 4) {
                        isRawType = true;
                        break;
                    }
                    bundle.putInt(actualKey, ByteBuffer.wrap(byteArray).getInt());
                    break;
                case "float":
                    if (byteArray.length != 4) {
                        isRawType = true;
                        break;
                    }
                    bundle.putFloat(actualKey, ByteBuffer.wrap(byteArray).getFloat());
                    break;
                case "int64":
                case "uint64":
                    if (byteArray.length != 8) {
                        isRawType = true;
                        break;
                    }
                    bundle.putLong(actualKey, ByteBuffer.wrap(byteArray).getLong());
                    break;
                case "double":
                    if (byteArray.length != 8) {
                        isRawType = true;
                        break;
                    }
                    bundle.putDouble(actualKey, ByteBuffer.wrap(byteArray).getDouble());
                    break;
                default:
                    isRawType = true;
                    break;
            }
            if (isRawType) {
                bundle.putByteArray(key, byteArray);
            }
        }
        return bundle;
    }

    // LINT.IfChange(aidl_type_conversions)
    protected abstract List<ResourceObject> toHalOffloadRawMetric(OffloadRawMetric rawMetric)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalCustomResource(CustomResource aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalTranslationGroup(TranslationGroup aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalRotationGroup(RotationGroup aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalBitmapResource(BitmapResource aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalRectShapeResource(RectShapeResource aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalRoundRectShapeResource(
            RoundRectShapeResource aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalArcPathResource(ArcPathResource aidlType)
            throws DisplayOffloadException;

    abstract List<ResourceObject> toHalTtfFontResource(TtfFontResource aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalSpriteSheetPngResource(
            SpriteSheetPngResource aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalOffloadString(OffloadString aidlType)
            throws DisplayOffloadException;

    abstract List<ResourceObject> toHalStaticText(StaticTextResource aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalDynamicText(DynamicTextResource aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalOffloadMetric(OffloadMetric aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalLinearMetricMapping(LinearMetricMapping aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalRangeMapping(RangeMapping aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalNumberFormatMapping(NumberFormatMapping aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalOffloadConstant(OffloadConstant aidlType)
            throws DisplayOffloadException;

    protected abstract List<ResourceObject> toHalModuloMapping(ModuloMapping aidlType)
            throws DisplayOffloadException;
    // LINT.ThenChange(HalAdapter.java:hal_send)

    interface HalTypeConverterSupplier {
        HalTypeConverter getConverter() throws DisplayOffloadException;
    }
}
