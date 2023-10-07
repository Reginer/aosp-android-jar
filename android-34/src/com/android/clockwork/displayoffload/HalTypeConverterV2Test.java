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

import static com.google.android.clockwork.ambient.offload.types.UnaryOperationTypeEnum.CEILING;
import static com.google.android.clockwork.ambient.offload.types.UnaryOperationTypeEnum.FLOOR;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.clockwork.ambient.offload.types.BinaryOperationTypeEnum;
import com.google.android.clockwork.ambient.offload.types.BindingPtrTypeEnum;
import com.google.android.clockwork.ambient.offload.types.CustomResource;
import com.google.android.clockwork.ambient.offload.types.DynamicTextResource;
import com.google.android.clockwork.ambient.offload.types.KeyValuePair;
import com.google.android.clockwork.ambient.offload.types.LinearMetricMapping;
import com.google.android.clockwork.ambient.offload.types.ModuloMapping;
import com.google.android.clockwork.ambient.offload.types.NumberFormatMapping;
import com.google.android.clockwork.ambient.offload.types.OffloadConstant;
import com.google.android.clockwork.ambient.offload.types.OffloadConstantType;
import com.google.android.clockwork.ambient.offload.types.OffloadMetric;
import com.google.android.clockwork.ambient.offload.types.OffloadRawMetric;
import com.google.android.clockwork.ambient.offload.types.RangeMapping;
import com.google.android.clockwork.ambient.offload.types.RawMetricType;
import com.google.android.clockwork.ambient.offload.types.TextParam;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import vendor.google_clockwork.displayoffload.V2_0.ArcPath;
import vendor.google_clockwork.displayoffload.V2_0.BinaryOperation;
import vendor.google_clockwork.displayoffload.V2_0.BinaryOperationType;
import vendor.google_clockwork.displayoffload.V2_0.BindingPtr;
import vendor.google_clockwork.displayoffload.V2_0.Bitmap;
import vendor.google_clockwork.displayoffload.V2_0.BitmapDrawable;
import vendor.google_clockwork.displayoffload.V2_0.DynamicText;
import vendor.google_clockwork.displayoffload.V2_0.FontParam;
import vendor.google_clockwork.displayoffload.V2_0.NumberFormatResource;
import vendor.google_clockwork.displayoffload.V2_0.Primitive;
import vendor.google_clockwork.displayoffload.V2_0.Primitive.PrimitiveSafeUnion;
import vendor.google_clockwork.displayoffload.V2_0.RectShape;
import vendor.google_clockwork.displayoffload.V2_0.RotationGroup;
import vendor.google_clockwork.displayoffload.V2_0.RoundRectShape;
import vendor.google_clockwork.displayoffload.V2_0.ShapeParam;
import vendor.google_clockwork.displayoffload.V2_0.StaticText;
import vendor.google_clockwork.displayoffload.V2_0.TernaryOperation;
import vendor.google_clockwork.displayoffload.V2_0.TernaryOperationType;
import vendor.google_clockwork.displayoffload.V2_0.TranslationGroup;
import vendor.google_clockwork.displayoffload.V2_0.Type;
import vendor.google_clockwork.displayoffload.V2_0.UnaryOperation;
import vendor.google_clockwork.displayoffload.V2_0.UnaryOperationType;

/** Tests for {@link com.android.clockwork.displayoffload.HalTypeConverterV2}. */
@RunWith(AndroidJUnit4.class)
public class HalTypeConverterV2Test {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final UniqueIdGenerator mUniqueIdGenerator = new UniqueIdGenerator((x) -> false);
    private final HalTypeConverter mHalTypeConverterV2 =
            new HalTypeConverterV2(mContext, mUniqueIdGenerator);
    private final OffloadRawMetric mGlobalDataSourceMetricV2 = new OffloadRawMetric();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mUniqueIdGenerator.reset();
        mHalTypeConverterV2.begin();
        mGlobalDataSourceMetricV2.name = 666;
        mGlobalDataSourceMetricV2.data = "dataSource";
    }

    @After
    public void teardown() {
        mHalTypeConverterV2.end();
    }

    private static <T> Optional<T> findFirst(T[] o, Predicate<? super T> predicate) {
        // Slow, but this is just for test.
        return Arrays.stream(o).filter(predicate).findFirst();
    }

    private static <T> T[] filterByType(Object[] o, Class<T> type) {
        // Slow, but this is just for test.
        return Arrays.stream(o)
                .filter(type::isInstance)
                .toArray((capacity) -> (T[]) Array.newInstance(type, capacity));
    }

    private static Object[] unpackResourceObjects(List<ResourceObject> resourceObjects) {
        return resourceObjects.stream().map(ResourceObject::getObject).toArray(Object[]::new);
    }

    @Test
    public void basicDynamicTextNumberFormat() throws DisplayOffloadException {
        DynamicTextResource aidlDynamicText = new DynamicTextResource();
        aidlDynamicText.id = mUniqueIdGenerator.nextId();
        aidlDynamicText.textParam = new TextParam();
        aidlDynamicText.binding = 1;

        NumberFormatMapping aidlNumberFormat = new NumberFormatMapping();
        aidlNumberFormat.name = 2;
        aidlNumberFormat.grouping = false;
        aidlNumberFormat.maximumFractionDigits = 1;
        aidlNumberFormat.minimumFractionDigits = 1;
        aidlNumberFormat.minimumIntegerDigits = 1;

        OffloadMetric aidlMetric = new OffloadMetric();
        aidlMetric.name = aidlDynamicText.binding;
        aidlMetric.mapping = aidlNumberFormat.name;
        aidlMetric.boundDataSource = mGlobalDataSourceMetricV2.name;

        int metricV1Id = mUniqueIdGenerator.getId(aidlMetric.name);
        int dataSourceId = mUniqueIdGenerator.getId(mGlobalDataSourceMetricV2.name);
        int numberFormatId = mUniqueIdGenerator.getId(aidlNumberFormat.name);

        List<ResourceObject> result = new LinkedList<>();
        result.addAll(mHalTypeConverterV2.toHalOffloadRawMetric(mGlobalDataSourceMetricV2));
        result.addAll(mHalTypeConverterV2.toHalOffloadMetric(aidlMetric));
        result.addAll(mHalTypeConverterV2.toHalNumberFormatMapping(aidlNumberFormat));
        result.addAll(mHalTypeConverterV2.toHalDynamicText(aidlDynamicText));
        Object[] halObjects = result.stream().map(ResourceObject::getObject).toArray(Object[]::new);

        // BinaryOperation(metricName, numberFormat, dataSource)
        BinaryOperation[] binaryOperations = filterByType(halObjects, BinaryOperation.class);
        assertThat(binaryOperations.length).isEqualTo(1);
        assertThat(binaryOperations[0].id).isEqualTo(metricV1Id);
        assertThat(binaryOperations[0].op).isEqualTo(BinaryOperationType.NUMBER_FORMAT);
        assertThat(binaryOperations[0].arg1.id).isEqualTo(numberFormatId);
        assertThat(binaryOperations[0].arg1.type).isEqualTo(Type.NUMBER_FORMAT);
        assertThat(binaryOperations[0].arg2.id).isEqualTo(dataSourceId);
        assertThat(binaryOperations[0].arg2.type).isEqualTo(Type.FLOAT);

        // DynamicText HAL type should point to the correct metricId
        DynamicTextAdapter[] dynamicTextResources =
                filterByType(halObjects, DynamicTextAdapter.class);
        assertThat(dynamicTextResources.length).isEqualTo(1);
        assertThat(dynamicTextResources[0].getV2().content.id).isEqualTo(metricV1Id);
        assertThat(dynamicTextResources[0].getV2().content.type).isEqualTo(Type.STRING);
    }

    @Test
    public void basicDynamicTextNumberFormat_BinaryOp() throws DisplayOffloadException {
        NumberFormatMapping aidlNumberFormat = new NumberFormatMapping();
        aidlNumberFormat.name = mUniqueIdGenerator.nextId();
        aidlNumberFormat.grouping = false;
        aidlNumberFormat.maximumFractionDigits = 1;
        aidlNumberFormat.minimumFractionDigits = 2;
        aidlNumberFormat.minimumIntegerDigits = 3;

        OffloadRawMetric aidlRawMetric = new OffloadRawMetric();
        aidlRawMetric.name = mUniqueIdGenerator.nextId();
        aidlRawMetric.rawMetricType = RawMetricType.DATA_SOURCE;
        aidlRawMetric.data = "whatever";

        com.google.android.clockwork.ambient.offload.types.BinaryOperation binaryOperation =
                new com.google.android.clockwork.ambient.offload.types.BinaryOperation();
        binaryOperation.name = mUniqueIdGenerator.nextId();
        binaryOperation.opType = BinaryOperationTypeEnum.NUMBER_FORMAT;
        binaryOperation.arg1 = new com.google.android.clockwork.ambient.offload.types.BindingPtr();
        binaryOperation.arg1.type = BindingPtrTypeEnum.NUMBER_FORMAT;
        binaryOperation.arg1.name = aidlNumberFormat.name;
        binaryOperation.arg2 = new com.google.android.clockwork.ambient.offload.types.BindingPtr();
        binaryOperation.arg2.type = BindingPtrTypeEnum.FLOAT;
        binaryOperation.arg2.name = aidlRawMetric.name;

        DynamicTextResource aidlDynamicText = new DynamicTextResource();
        aidlDynamicText.id = mUniqueIdGenerator.nextId();
        aidlDynamicText.textParam = new TextParam();
        aidlDynamicText.binding = binaryOperation.name;

        // Conversion
        List<ResourceObject> result = new ArrayList<>();
        result.addAll(mHalTypeConverterV2.toHalOffloadRawMetric(aidlRawMetric));
        result.addAll(mHalTypeConverterV2.toHalNumberFormatMapping(aidlNumberFormat));
        result.addAll(mHalTypeConverterV2.toHalBinaryOperation(binaryOperation));
        result.addAll(mHalTypeConverterV2.toHalDynamicText(aidlDynamicText));
        Object[] halObjects = result.stream().map(ResourceObject::getObject).toArray(Object[]::new);

        // Verify if conversion result is correct
        // OffloadMetric should point to data source
        vendor.google_clockwork.displayoffload.V2_0.OffloadMetric[] halOffloadMetric =
                filterByType(
                        halObjects,
                        vendor.google_clockwork.displayoffload.V2_0.OffloadMetric.class);
        assertThat(halOffloadMetric.length).isEqualTo(1);
        assertThat(halOffloadMetric[0].target).isEqualTo(aidlRawMetric.data);

        // Only one NumberFormatResource HAL type should exist.
        NumberFormatResource[] halNumberFormats =
                filterByType(halObjects, NumberFormatResource.class);
        assertThat(halNumberFormats.length).isEqualTo(1);

        // BinaryOperation HAL type should have correct operation type and point to correct
        // argument objects. In this case: op type should be NUMBER_FORMAT.
        // First argument be the NumberFormatResource and second argument be OffloadMetric.
        BinaryOperation[] halBinaryOps = filterByType(halObjects, BinaryOperation.class);
        assertThat(halBinaryOps.length).isEqualTo(1);
        assertThat(halBinaryOps[0].op).isEqualTo(BinaryOperationType.NUMBER_FORMAT);
        assertThat(halBinaryOps[0].arg1.id).isEqualTo(halNumberFormats[0].id);
        assertThat(halBinaryOps[0].arg1.type).isEqualTo(Type.NUMBER_FORMAT);
        assertThat(halBinaryOps[0].arg2.id).isEqualTo(halOffloadMetric[0].id);
        assertThat(halBinaryOps[0].arg2.type).isEqualTo(Type.FLOAT);

        // DynamicText HAL type should point to the correct BinaryOperation id
        DynamicTextAdapter[] dynamicTextResources =
                filterByType(halObjects, DynamicTextAdapter.class);
        assertThat(dynamicTextResources.length).isEqualTo(1);
        assertThat(dynamicTextResources[0].getV2().content.type).isEqualTo(Type.STRING);
        assertThat(dynamicTextResources[0].getV2().content.id).isEqualTo(halBinaryOps[0].id);
    }

    @Test
    public void basicRangeMapping() throws DisplayOffloadException {
        float[] thresholds = new float[]{10.0f, 20.0f, 30.0f};
        RangeMapping aidlRangeMapping = new RangeMapping();
        aidlRangeMapping.name = 1;
        aidlRangeMapping.thresholds = thresholds;
        aidlRangeMapping.val = new int[]{4, 5, 6, 7};

        OffloadMetric aidlMetric = new OffloadMetric();
        aidlMetric.name = 2;
        aidlMetric.mapping = aidlRangeMapping.name;
        aidlMetric.boundDataSource = mGlobalDataSourceMetricV2.name;

        int thresholdLength = aidlRangeMapping.thresholds.length;
        int resultId = mUniqueIdGenerator.getId(aidlMetric.name);
        int dataSourceId = mUniqueIdGenerator.getId(mGlobalDataSourceMetricV2.name);
        int[] valIds =
                IntStream.range(0, aidlRangeMapping.val.length)
                        .map((x) -> mUniqueIdGenerator.getId(aidlRangeMapping.val[x]))
                        .toArray();

        List<ResourceObject> result = new LinkedList<>();
        result.addAll(mHalTypeConverterV2.toHalOffloadRawMetric(mGlobalDataSourceMetricV2));
        result.addAll(mHalTypeConverterV2.toHalOffloadMetric(aidlMetric));
        result.addAll(mHalTypeConverterV2.toHalRangeMapping(aidlRangeMapping));
        Object[] halObjects = result.stream().map(ResourceObject::getObject).toArray(Object[]::new);

        // Should have exactly thresholdLength primitives
        Primitive[] primitives = filterByType(halObjects, Primitive.class);
        assertThat(primitives.length).isEqualTo(thresholdLength);
        Optional<Primitive> p0 = findFirst(primitives, p -> p.val.floatVal() == thresholds[0]);
        Optional<Primitive> p1 = findFirst(primitives, p -> p.val.floatVal() == thresholds[1]);
        Optional<Primitive> p2 = findFirst(primitives, p -> p.val.floatVal() == thresholds[2]);

        // Should have exactly thresholdLength ternary operations
        TernaryOperation[] ternaryOps = filterByType(halObjects, TernaryOperation.class);
        assertThat(ternaryOps.length).isEqualTo(thresholdLength);
        Optional<TernaryOperation> ifElse0 = findFirst(ternaryOps, (p -> p.id == resultId));
        Optional<TernaryOperation> ifElse1 =
                findFirst(ternaryOps, (p -> p.id == ifElse0.get().arg3.id));
        Optional<TernaryOperation> ifElse2 =
                findFirst(ternaryOps, (p -> p.id == ifElse1.get().arg3.id));
        assertThat(ifElse0.get().op).isEqualTo(TernaryOperationType.IF_ELSE);
        assertThat(ifElse1.get().op).isEqualTo(TernaryOperationType.IF_ELSE);
        assertThat(ifElse2.get().op).isEqualTo(TernaryOperationType.IF_ELSE);
        assertThat(ifElse0.get().arg2.id).isEqualTo(valIds[0]);
        assertThat(ifElse1.get().arg2.id).isEqualTo(valIds[1]);
        assertThat(ifElse2.get().arg2.id).isEqualTo(valIds[2]);
        assertThat(ifElse2.get().arg3.id).isEqualTo(valIds[3]);

        // Should have exactly thresholdLength of binary operations
        BinaryOperation[] binaryOp = filterByType(halObjects, BinaryOperation.class);
        assertThat(binaryOp.length).isEqualTo(thresholdLength);
        Optional<BinaryOperation> check0 =
                findFirst(binaryOp, (p -> p.id == ifElse0.get().arg1.id));
        Optional<BinaryOperation> check1 =
                findFirst(binaryOp, (p -> p.id == ifElse1.get().arg1.id));
        Optional<BinaryOperation> check2 =
                findFirst(binaryOp, (p -> p.id == ifElse2.get().arg1.id));
        assertThat(check0.get().op).isEqualTo(BinaryOperationType.LESS_THAN);
        assertThat(check1.get().op).isEqualTo(BinaryOperationType.LESS_THAN);
        assertThat(check2.get().op).isEqualTo(BinaryOperationType.LESS_THAN);
        assertThat(check0.get().arg1.id).isEqualTo(dataSourceId);
        assertThat(check1.get().arg1.id).isEqualTo(dataSourceId);
        assertThat(check2.get().arg1.id).isEqualTo(dataSourceId);
        assertThat(check0.get().arg2.id).isEqualTo(p0.get().id);
        assertThat(check1.get().arg2.id).isEqualTo(p1.get().id);
        assertThat(check2.get().arg2.id).isEqualTo(p2.get().id);
    }

    @Test
    public void basicLinearMetricMapping() throws DisplayOffloadException {
        LinearMetricMapping aidlLinearMap = new LinearMetricMapping();
        aidlLinearMap.name = 1;
        aidlLinearMap.b = 42.0f;
        aidlLinearMap.m = 24.0f;

        OffloadMetric aidlMetric = new OffloadMetric();
        aidlMetric.name = 2;
        aidlMetric.mapping = aidlLinearMap.name;
        aidlMetric.boundDataSource = mGlobalDataSourceMetricV2.name;

        int metricId = mUniqueIdGenerator.getId(aidlMetric.name);
        int dataSourceId = mUniqueIdGenerator.getId(mGlobalDataSourceMetricV2.name);

        List<ResourceObject> result = new LinkedList<>();
        result.addAll(mHalTypeConverterV2.toHalOffloadRawMetric(mGlobalDataSourceMetricV2));
        result.addAll(mHalTypeConverterV2.toHalOffloadMetric(aidlMetric));
        result.addAll(mHalTypeConverterV2.toHalLinearMetricMapping(aidlLinearMap));
        Object[] halObjects = result.stream().map(ResourceObject::getObject).toArray(Object[]::new);

        // Result has exactly ONE addition and ONE multiply
        BinaryOperation[] binaryOperations = filterByType(halObjects, BinaryOperation.class);
        assertThat(binaryOperations.length).isEqualTo(2);
        BinaryOperation[] add =
                Arrays.stream(binaryOperations)
                        .filter((binaryOperation -> binaryOperation.op == BinaryOperationType.ADD))
                        .toArray(BinaryOperation[]::new);
        BinaryOperation[] multiply =
                Arrays.stream(binaryOperations)
                        .filter(
                                (binaryOperation ->
                                        binaryOperation.op == BinaryOperationType.MULTIPLY))
                        .toArray(BinaryOperation[]::new);
        assertThat(add.length).isEqualTo(1);
        assertThat(multiply.length).isEqualTo(1);
        // Multiplication node's arg2 should be connected to data source id
        assertThat(multiply[0].arg2.id).isEqualTo(dataSourceId);
        // Addition node's arg2 should be connected to multiply object's id
        assertThat(add[0].arg2.id).isEqualTo(multiply[0].id);
        // Addition node's id should match original metric id after remap
        assertThat(add[0].id).isEqualTo(metricId);

        // Result has exactly TWO primitives
        Primitive[] primitives = filterByType(halObjects, Primitive.class);
        assertThat(primitives.length).isEqualTo(2);
        // Multiplication node's arg1 should be m
        Optional<Primitive> m = findFirst(primitives, (p -> multiply[0].arg1.id == p.id));
        assertThat(m.get().val.floatVal()).isEqualTo(aidlLinearMap.m);
        // Addition node's arg1 should be b
        Optional<Primitive> b = findFirst(primitives, (p -> add[0].arg1.id == p.id));
        assertThat(b.get().val.floatVal()).isEqualTo(aidlLinearMap.b);

        // Result has exactly ONE offload metric
        vendor.google_clockwork.displayoffload.V2_0.OffloadMetric[] offloadMetrics =
                filterByType(
                        halObjects,
                        vendor.google_clockwork.displayoffload.V2_0.OffloadMetric.class);
        assertThat(offloadMetrics.length).isEqualTo(1);
        assertThat(offloadMetrics[0].id).isEqualTo(dataSourceId);
        assertThat(offloadMetrics[0].target).isEqualTo(mGlobalDataSourceMetricV2.data);
    }

    @Test
    public void chainedLinearMetricMapping() throws DisplayOffloadException {
        // (1 + 2 * (3 + 4 * dataSource))
        LinearMetricMapping aidlLinearMap1 = new LinearMetricMapping();
        aidlLinearMap1.name = 1;
        aidlLinearMap1.b = 1.0f;
        aidlLinearMap1.m = 2.0f;

        OffloadMetric aidlMetric1 = new OffloadMetric();
        aidlMetric1.name = 3;
        aidlMetric1.mapping = aidlLinearMap1.name;
        aidlMetric1.boundDataSource = 4;

        LinearMetricMapping aidlLinearMap2 = new LinearMetricMapping();
        aidlLinearMap2.name = 2;
        aidlLinearMap2.b = 3.0f;
        aidlLinearMap2.m = 4.0f;

        OffloadMetric aidlMetric2 = new OffloadMetric();
        aidlMetric2.name = aidlMetric1.boundDataSource;
        aidlMetric2.mapping = aidlLinearMap2.name;
        aidlMetric2.boundDataSource = mGlobalDataSourceMetricV2.name;

        int resultId = mUniqueIdGenerator.getId(aidlMetric1.name);
        int intermediateId = mUniqueIdGenerator.getId(aidlMetric1.boundDataSource);
        int dataSourceId = mUniqueIdGenerator.getId(mGlobalDataSourceMetricV2.name);

        List<ResourceObject> result = new LinkedList<>();
        result.addAll(mHalTypeConverterV2.toHalOffloadRawMetric(mGlobalDataSourceMetricV2));
        result.addAll(mHalTypeConverterV2.toHalOffloadMetric(aidlMetric1));
        result.addAll(mHalTypeConverterV2.toHalOffloadMetric(aidlMetric2));
        result.addAll(mHalTypeConverterV2.toHalLinearMetricMapping(aidlLinearMap1));
        result.addAll(mHalTypeConverterV2.toHalLinearMetricMapping(aidlLinearMap2));
        Object[] halObjects = unpackResourceObjects(result);

        // Result has exactly TWO addition and TWO multiply
        BinaryOperation[] binaryOperations = filterByType(halObjects, BinaryOperation.class);
        assertThat(binaryOperations.length).isEqualTo(4);
        BinaryOperation[] add =
                Arrays.stream(binaryOperations)
                        .filter((binaryOperation -> binaryOperation.op == BinaryOperationType.ADD))
                        .toArray(BinaryOperation[]::new);
        BinaryOperation[] multiply =
                Arrays.stream(binaryOperations)
                        .filter(
                                (binaryOperation ->
                                        binaryOperation.op == BinaryOperationType.MULTIPLY))
                        .toArray(BinaryOperation[]::new);
        assertThat(add.length).isEqualTo(2);
        assertThat(multiply.length).isEqualTo(2);

        // result = [b + (m * intermediate)]
        Optional<BinaryOperation> resultAddBinOp = findFirst(add, (p -> p.id == resultId));
        assertThat(resultAddBinOp.get().op).isEqualTo(BinaryOperationType.ADD);
        // result = b + (m * intermediate)
        Optional<BinaryOperation> resultMultiplyBinOp =
                findFirst(multiply, (p -> p.id == resultAddBinOp.get().arg2.id));
        assertThat(resultMultiplyBinOp.get().op).isEqualTo(BinaryOperationType.MULTIPLY);
        // intermediate = [b + (m * dataSource)]
        assertThat(resultMultiplyBinOp.get().arg2.id).isEqualTo(intermediateId);
        Optional<BinaryOperation> intermediateAddBinOp =
                findFirst(add, (p -> p.id == intermediateId));
        assertThat(intermediateAddBinOp.get().op).isEqualTo(BinaryOperationType.ADD);
        // intermediate = b + (m * dataSource)
        Optional<BinaryOperation> intermediateMultiplyBinOp =
                findFirst(multiply, (p -> p.id == intermediateAddBinOp.get().arg2.id));
        assertThat(intermediateMultiplyBinOp.get().op).isEqualTo(BinaryOperationType.MULTIPLY);
        assertThat(intermediateMultiplyBinOp.get().arg2.id).isEqualTo(dataSourceId);

        // Result has exactly FOUR primitives
        Primitive[] primitives = filterByType(halObjects, Primitive.class);
        assertThat(primitives.length).isEqualTo(4);

        // Primitives should match the value as they are defined in the OffloadMetric
        Optional<Primitive> b1 = findFirst(primitives, (p -> resultAddBinOp.get().arg1.id == p.id));
        Optional<Primitive> m1 =
                findFirst(primitives, (p -> resultMultiplyBinOp.get().arg1.id == p.id));
        Optional<Primitive> b2 =
                findFirst(primitives, (p -> intermediateAddBinOp.get().arg1.id == p.id));
        Optional<Primitive> m2 =
                findFirst(primitives, (p -> intermediateMultiplyBinOp.get().arg1.id == p.id));
        assertThat(b1.get().val.floatVal()).isEqualTo(aidlLinearMap1.b);
        assertThat(m1.get().val.floatVal()).isEqualTo(aidlLinearMap1.m);
        assertThat(b2.get().val.floatVal()).isEqualTo(aidlLinearMap2.b);
        assertThat(m2.get().val.floatVal()).isEqualTo(aidlLinearMap2.m);
    }

    @Test
    public void commonDataSourceLinearMetricMapping_shouldGenerateOneOffloadMetric()
            throws DisplayOffloadException {
        String commonDataSource = "dataSource";

        OffloadRawMetric aidlRawMetricExtra = new OffloadRawMetric();
        aidlRawMetricExtra.name = 6;
        aidlRawMetricExtra.data = commonDataSource;

        // (1 + 2 * dataSource)
        LinearMetricMapping aidlLinearMap1 = new LinearMetricMapping();
        aidlLinearMap1.name = 1;
        aidlLinearMap1.b = 1.0f;
        aidlLinearMap1.m = 2.0f;

        OffloadMetric aidlMetric1 = new OffloadMetric();
        aidlMetric1.name = 3;
        aidlMetric1.mapping = aidlLinearMap1.name;
        aidlMetric1.boundDataSource = mUniqueIdGenerator.getId(mGlobalDataSourceMetricV2.name);

        // (3 + 4 * dataSource)
        LinearMetricMapping aidlLinearMap2 = new LinearMetricMapping();
        aidlLinearMap2.name = 2;
        aidlLinearMap2.b = 3.0f;
        aidlLinearMap2.m = 4.0f;

        OffloadMetric aidlMetric2 = new OffloadMetric();
        aidlMetric2.name = 4;
        aidlMetric2.mapping = aidlLinearMap2.name;
        aidlMetric2.boundDataSource = mUniqueIdGenerator.getId(aidlRawMetricExtra.name);

        int result1Id = mUniqueIdGenerator.getId(aidlMetric1.name);
        int result2Id = mUniqueIdGenerator.getId(aidlMetric2.name);
        int firstMetDataSourceId = mUniqueIdGenerator.getId(mGlobalDataSourceMetricV2.name);

        List<ResourceObject> result = new LinkedList<>();
        result.addAll(mHalTypeConverterV2.toHalOffloadRawMetric(mGlobalDataSourceMetricV2));
        result.addAll(mHalTypeConverterV2.toHalOffloadRawMetric(aidlRawMetricExtra));
        result.addAll(mHalTypeConverterV2.toHalOffloadMetric(aidlMetric1));
        result.addAll(mHalTypeConverterV2.toHalOffloadMetric(aidlMetric2));
        result.addAll(mHalTypeConverterV2.toHalLinearMetricMapping(aidlLinearMap1));
        result.addAll(mHalTypeConverterV2.toHalLinearMetricMapping(aidlLinearMap2));
        Object[] halObjects = unpackResourceObjects(result);

        // Result has exactly ONE offload metric. Since two offload metrics hold the same string.
        // So the first met metric's id is corresponding to the data source string.
        vendor.google_clockwork.displayoffload.V2_0.OffloadMetric[] offloadMetrics =
                filterByType(
                        halObjects,
                        vendor.google_clockwork.displayoffload.V2_0.OffloadMetric.class);
        assertThat(offloadMetrics.length).isEqualTo(1);
        assertThat(offloadMetrics[0].id).isEqualTo(firstMetDataSourceId);
        assertThat(offloadMetrics[0].target).isEqualTo(commonDataSource);

        // Result has exactly TWO addition and TWO multiply
        BinaryOperation[] binaryOperations = filterByType(halObjects, BinaryOperation.class);
        assertThat(binaryOperations.length).isEqualTo(4);
        BinaryOperation[] add =
                Arrays.stream(binaryOperations)
                        .filter((binaryOperation -> binaryOperation.op == BinaryOperationType.ADD))
                        .toArray(BinaryOperation[]::new);
        BinaryOperation[] multiply =
                Arrays.stream(binaryOperations)
                        .filter(
                                (binaryOperation ->
                                        binaryOperation.op == BinaryOperationType.MULTIPLY))
                        .toArray(BinaryOperation[]::new);
        assertThat(add.length).isEqualTo(2);
        assertThat(multiply.length).isEqualTo(2);
        // Check the ids from TWO v1 OffloadMetrics, whose name corresponds to the result of the
        // result of linear mapping. But the intermediate multiplication ids should be unknown.
        assertThat(Arrays.stream(add).map((x) -> x.id).collect(Collectors.toList()))
                .containsExactly(result1Id, result2Id);
    }

    @Test
    public void basicUnaryOperationCeilFloor() throws DisplayOffloadException {
        String dataSource = "dataSource";

        // ceil(dataSource)
        com.google.android.clockwork.ambient.offload.types.UnaryOperation aidlCeil
                = new com.google.android.clockwork.ambient.offload.types.UnaryOperation();
        aidlCeil.name = 1;
        aidlCeil.operation = CEILING;
        com.google.android.clockwork.ambient.offload.types.BindingPtr operandCeilBindingPtr
                = new com.google.android.clockwork.ambient.offload.types.BindingPtr();
        operandCeilBindingPtr.name = mUniqueIdGenerator.getId(dataSource);
        operandCeilBindingPtr.type = BindingPtrTypeEnum.FLOAT;
        aidlCeil.operand = operandCeilBindingPtr;

        // floor(dataSource)
        com.google.android.clockwork.ambient.offload.types.UnaryOperation aidlFloor
                = new com.google.android.clockwork.ambient.offload.types.UnaryOperation();
        aidlFloor.name = 2;
        aidlFloor.operation = FLOOR;
        com.google.android.clockwork.ambient.offload.types.BindingPtr operandFloorBindingPtr
                = new com.google.android.clockwork.ambient.offload.types.BindingPtr();
        operandFloorBindingPtr.name = mUniqueIdGenerator.getId(dataSource);
        operandFloorBindingPtr.type = BindingPtrTypeEnum.FLOAT;
        aidlFloor.operand = operandFloorBindingPtr;

        int resultCeilId = mUniqueIdGenerator.getId(aidlCeil.name);
        int resultFloorId = mUniqueIdGenerator.getId(aidlFloor.name);
        int dataSourceId = mUniqueIdGenerator.getId(aidlFloor.operand.name);

        List<ResourceObject> capturedResources = new LinkedList<>();

        capturedResources
                .addAll(mHalTypeConverterV2.toHalOffloadRawMetric(mGlobalDataSourceMetricV2));
        capturedResources.addAll(mHalTypeConverterV2.toHalUnaryOperation(aidlCeil));
        capturedResources.addAll(mHalTypeConverterV2.toHalUnaryOperation(aidlFloor));

        Object[] halObjects = unpackResourceObjects(capturedResources);

        // Result has exactly ONE offload metric
        vendor.google_clockwork.displayoffload.V2_0.OffloadMetric[] offloadMetrics =
                filterByType(
                        halObjects,
                        vendor.google_clockwork.displayoffload.V2_0.OffloadMetric.class);
        assertThat(offloadMetrics.length).isEqualTo(1);

        // Result has exactly TWO unary operations
        UnaryOperation[] unaryOps = filterByType(halObjects, UnaryOperation.class);
        assertThat(unaryOps.length).isEqualTo(2);

        Optional<UnaryOperation> ceil = findFirst(unaryOps, (p -> p.id == resultCeilId));
        assertThat(ceil.get().op).isEqualTo(UnaryOperationType.CEILING);
        assertThat(ceil.get().arg1.id).isEqualTo(dataSourceId);

        Optional<UnaryOperation> floor = findFirst(unaryOps, (p -> p.id == resultFloorId));
        assertThat(floor.get().op).isEqualTo(UnaryOperationType.FLOOR);
        assertThat(floor.get().arg1.id).isEqualTo(dataSourceId);
    }

    @Test
    public void basicModuloMapping() throws DisplayOffloadException {
        // Modulo mapping
        int mod = 42;
        ModuloMapping aidlModulo = new ModuloMapping();
        aidlModulo.name = 1;
        aidlModulo.modulus = mod;

        OffloadMetric aidlMetric = new OffloadMetric();
        aidlMetric.name = 3;
        aidlMetric.mapping = aidlModulo.name;
        aidlMetric.boundDataSource = mGlobalDataSourceMetricV2.name;

        int resultId = mUniqueIdGenerator.getId(aidlMetric.name);
        int dataSourceId = mUniqueIdGenerator.getId(mGlobalDataSourceMetricV2.name);

        List<ResourceObject> result = new LinkedList<>();
        result.addAll(mHalTypeConverterV2.toHalOffloadRawMetric(mGlobalDataSourceMetricV2));
        result.addAll(mHalTypeConverterV2.toHalOffloadMetric(aidlMetric));
        result.addAll(mHalTypeConverterV2.toHalModuloMapping(aidlModulo));
        Object[] halObjects = unpackResourceObjects(result);

        // Result has exactly ONE offload metric
        vendor.google_clockwork.displayoffload.V2_0.OffloadMetric[] offloadMetrics =
                filterByType(
                        halObjects,
                        vendor.google_clockwork.displayoffload.V2_0.OffloadMetric.class);
        assertThat(offloadMetrics.length).isEqualTo(1);

        // Result has exactly ONE unary op for rounding to INT
        UnaryOperation[] unaryOps = filterByType(halObjects, UnaryOperation.class);
        Optional<UnaryOperation> round = findFirst(unaryOps, (p -> p.arg1.id == dataSourceId));
        assertThat(round.get().op).isEqualTo(UnaryOperationType.ROUND);

        // Result has exactly ONE binary op for modulo
        BinaryOperation[] binOps = filterByType(halObjects, BinaryOperation.class);
        Optional<BinaryOperation> modBinOp = findFirst(binOps, (p -> p.id == resultId));
        assertThat(modBinOp.get().op).isEqualTo(BinaryOperationType.MODULO);
        assertThat(modBinOp.get().arg2.id).isEqualTo(round.get().id);

        // Result has exactly ONE primitive
        Primitive[] primitives = filterByType(halObjects, Primitive.class);
        assertThat(primitives.length).isEqualTo(1);
        assertThat(primitives[0].val.int32Val()).isEqualTo(mod);
    }

    @Test
    public void offloadConstants_shouldGenerateCorrectPrimitives() throws DisplayOffloadException {
        Primitive[] primitive;
        int primitiveId;
        OffloadConstant offloadConstant = new OffloadConstant();

        mHalTypeConverterV2.begin();
        offloadConstant.name = 1;
        offloadConstant.valueType = OffloadConstantType.INT32;
        offloadConstant.intValue = 0;
        primitiveId = mUniqueIdGenerator.getId(offloadConstant.name);
        primitive =
                filterByType(
                        unpackResourceObjects(
                                mHalTypeConverterV2.toHalOffloadConstant(offloadConstant)),
                        Primitive.class);
        assertThat(primitive.length).isEqualTo(1);
        assertThat(primitive[0].id).isEqualTo(primitiveId);
        assertThat(primitive[0].val.getDiscriminator())
                .isEqualTo(PrimitiveSafeUnion.hidl_discriminator.int32Val);
        assertThat(primitive[0].val.int32Val()).isEqualTo(offloadConstant.intValue);
        mHalTypeConverterV2.end();

        mHalTypeConverterV2.begin();
        offloadConstant.name = 2;
        offloadConstant.valueType = OffloadConstantType.FLOAT;
        offloadConstant.floatValue = 1.0f;
        primitiveId = mUniqueIdGenerator.getId(offloadConstant.name);
        primitive =
                filterByType(
                        unpackResourceObjects(
                                mHalTypeConverterV2.toHalOffloadConstant(offloadConstant)),
                        Primitive.class);
        assertThat(primitive.length).isEqualTo(1);
        assertThat(primitive[0].id).isEqualTo(primitiveId);
        assertThat(primitive[0].val.getDiscriminator())
                .isEqualTo(PrimitiveSafeUnion.hidl_discriminator.floatVal);
        assertThat(primitive[0].val.floatVal()).isEqualTo(offloadConstant.floatValue);
        mHalTypeConverterV2.end();

        mHalTypeConverterV2.begin();
        offloadConstant.name = 3;
        offloadConstant.valueType = OffloadConstantType.STRING;
        offloadConstant.stringValue = "TEST";
        primitiveId = mUniqueIdGenerator.getId(offloadConstant.name);
        primitive =
                filterByType(
                        unpackResourceObjects(
                                mHalTypeConverterV2.toHalOffloadConstant(offloadConstant)),
                        Primitive.class);
        assertThat(primitive.length).isEqualTo(1);
        assertThat(primitive[0].id).isEqualTo(primitiveId);
        assertThat(primitive[0].val.getDiscriminator())
                .isEqualTo(PrimitiveSafeUnion.hidl_discriminator.int32Val);
        assertThat(primitive[0].val.int32Val()).isEqualTo(0);
        mHalTypeConverterV2.end();

        mHalTypeConverterV2.begin();
        offloadConstant.name = 4;
        offloadConstant.valueType = OffloadConstantType.NONE;
        primitiveId = mUniqueIdGenerator.getId(offloadConstant.name);
        primitive =
                filterByType(
                        unpackResourceObjects(
                                mHalTypeConverterV2.toHalOffloadConstant(offloadConstant)),
                        Primitive.class);
        assertThat(primitive.length).isEqualTo(1);
        assertThat(primitive[0].id).isEqualTo(primitiveId);
        assertThat(primitive[0].val.getDiscriminator())
                .isEqualTo(PrimitiveSafeUnion.hidl_discriminator.int32Val);
        assertThat(primitive[0].val.int32Val()).isEqualTo(0);
        mHalTypeConverterV2.end();
    }

    @Test
    public void testToHalCustomResource() throws DisplayOffloadException {
        int pairCount = 10;
        CustomResource customResource = new CustomResource();
        customResource.id = 100;
        KeyValuePair[] aidlKVPairs = new KeyValuePair[pairCount];
        for (int i = 0; i < aidlKVPairs.length; i++) {
            aidlKVPairs[i] = makeAidlKVPair(i);
        }
        customResource.keyValues = aidlKVPairs;

        List<ResourceObject> resultObjects
                = mHalTypeConverterV2.toHalCustomResource(customResource);

        assertThat(resultObjects.size()).isEqualTo(1);
        assertThat(resultObjects.get(0).getObject().getClass())
                .isEqualTo(vendor.google_clockwork.displayoffload.V2_0.CustomResource.class);

        ArrayList<vendor.google_clockwork.displayoffload.V1_0.KeyValuePair> halKVPairs =
                ((vendor.google_clockwork.displayoffload.V2_0.CustomResource)
                        resultObjects.get(0).getObject()).keyValues;
        for (int i = 0; i < pairCount; i++) {
            vendor.google_clockwork.displayoffload.V1_0.KeyValuePair resultObject
                    = halKVPairs.get(i);
            assertThat(convertByteArrayListToInt((resultObject.value))).isEqualTo(i);
        }
    }

    private KeyValuePair makeAidlKVPair(int intId) {
        KeyValuePair aidlKVPair = new KeyValuePair();
        aidlKVPair.key = String.valueOf(intId);
        aidlKVPair.value = intToBytes(intId);
        return aidlKVPair;
    }

    private byte[] intToBytes(int i) {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(i);
        return bb.array();
    }

    private int convertByteArrayListToInt(ArrayList<Byte> byteArrayList) {
        byte[] bytes = new byte[byteArrayList.size()];
        for (int i = 0; i < byteArrayList.size(); i++) {
            bytes[i] = byteArrayList.get(i);
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        return byteBuffer.getInt();
    }

    @Test
    public void testGetIdReferenced() throws DisplayOffloadException {
        Set<Integer> referencesAns = new HashSet<>();

        // TranslationGroup
        checkSizeAndContentThenClear(referencesAns,
                mHalTypeConverterV2.getIdReferenced(createTranslationGroup(referencesAns,
                        createIntegerArrayListOfSize(5))));

        // RotationGroup
        checkSizeAndContentThenClear(referencesAns,
                mHalTypeConverterV2.getIdReferenced(createRotationGroup(referencesAns,
                        createIntegerArrayListOfSize(5))));

        // StaticTextAdapter
        checkSizeAndContentThenClear(referencesAns,
                mHalTypeConverterV2.getIdReferenced(createStaticTextAdapter(referencesAns,
                        createIntegerArrayListOfSize(3),
                        createFloatArrayListOfSize(4))));

        // DynamicTextAdapter
        checkSizeAndContentThenClear(referencesAns,
                mHalTypeConverterV2.getIdReferenced(createDynamicTextAdapter(referencesAns)));

        // BitmapDrawable
        checkSizeAndContentThenClear(
                referencesAns,
                mHalTypeConverterV2.getIdReferenced(createBitmapDrawable(referencesAns)));

        // UnaryOperation
        checkSizeAndContentThenClear(referencesAns,
                mHalTypeConverterV2.getIdReferenced(createUnaryOperation(referencesAns)));

        // BinaryOperation
        checkSizeAndContentThenClear(referencesAns,
                mHalTypeConverterV2.getIdReferenced(createBinaryOperation(referencesAns)));

        // TernaryOperation
        checkSizeAndContentThenClear(
                referencesAns,
                mHalTypeConverterV2.getIdReferenced(createTernaryOperation(referencesAns)));

        // RectShape
        checkSizeAndContentThenClear(
                referencesAns, mHalTypeConverterV2.getIdReferenced(createRectShape(referencesAns)));

        // RoundRectShape
        checkSizeAndContentThenClear(
                referencesAns,
                mHalTypeConverterV2.getIdReferenced(createRoundRectShape(referencesAns)));

        // ArcPath
        checkSizeAndContentThenClear(
                referencesAns, mHalTypeConverterV2.getIdReferenced(createArcPath(referencesAns)));
    }

    private void checkSizeAndContentThenClear(Set<Integer> ans, List<Integer> input) {
        assertThat(ans.size()).isEqualTo(input.size());
        assertThat(ans.containsAll(input)).isTrue();
        ans.clear();
    }

    private ArrayList<Integer> createIntegerArrayListOfSize(int size) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            arrayList.add(mUniqueIdGenerator.nextId());
        }
        return arrayList;
    }

    private ArrayList<Float> createFloatArrayListOfSize(int size) {
        ArrayList<Integer> arrayListInteger = createIntegerArrayListOfSize(size);
        ArrayList<Float> arrayListFloat = new ArrayList<>();
        arrayListInteger.forEach(integer -> arrayListFloat.add(integer.floatValue()));
        return arrayListFloat;
    }

    private BindingPtr createBindingPtr() {
        BindingPtr bindingPtr = new BindingPtr();
        bindingPtr.id = mUniqueIdGenerator.nextId();
        return bindingPtr;
    }

    private TranslationGroup createTranslationGroup(Set<Integer> references,
            ArrayList<Integer> contents) {
        TranslationGroup translationGroup = new TranslationGroup();
        translationGroup.id = mUniqueIdGenerator.nextId();
        translationGroup.offsetY = createBindingPtr();
        translationGroup.offsetX = createBindingPtr();
        translationGroup.visible = createBindingPtr();
        translationGroup.contents = contents == null ? new ArrayList<>() : contents;

        references.add(translationGroup.offsetX.id);
        references.add(translationGroup.offsetY.id);
        references.addAll(translationGroup.contents);
        references.add(translationGroup.visible.id);

        return translationGroup;
    }

    private RotationGroup createRotationGroup(Set<Integer> references,
            ArrayList<Integer> contents) {
        RotationGroup rotationGroup = new RotationGroup();
        rotationGroup.id = mUniqueIdGenerator.nextId();
        rotationGroup.pivotY = createBindingPtr();
        rotationGroup.pivotX = createBindingPtr();
        rotationGroup.visible = createBindingPtr();
        rotationGroup.angleDeg = createBindingPtr();
        rotationGroup.contents = contents == null ? new ArrayList<>() : contents;

        references.add(rotationGroup.pivotY.id);
        references.add(rotationGroup.pivotX.id);
        references.addAll(rotationGroup.contents);
        references.add(rotationGroup.visible.id);
        references.add(rotationGroup.angleDeg.id);

        return rotationGroup;
    }

    private StaticTextAdapter createStaticTextAdapter(Set<Integer> references,
            ArrayList<Integer> shapedGlyphIndices, ArrayList<Float> shapedGlyphPositions) {
        StaticText staticText = new StaticText();
        staticText.id = mUniqueIdGenerator.nextId();
        staticText.shapedGlyphIndices = shapedGlyphIndices;
        staticText.shapedGlyphPositions = shapedGlyphPositions;
        staticText.visible = createBindingPtr();
        staticText.fontParam = new FontParam();
        staticText.fontParam.ttfFont = mUniqueIdGenerator.nextId();
        staticText.color = createBindingPtr();
        staticText.originalString = "dummy_String";

        references.add(staticText.visible.id);
        references.add(staticText.color.id);
        references.add(staticText.fontParam.ttfFont);
        return new StaticTextAdapter(staticText);
    }

    private DynamicTextAdapter createDynamicTextAdapter(Set<Integer> references) {
        DynamicText dynamicText = new DynamicText();
        dynamicText.id = mUniqueIdGenerator.nextId();
        dynamicText.fontParam = new FontParam();
        dynamicText.fontParam.ttfFont = mUniqueIdGenerator.nextId();
        dynamicText.content = createBindingPtr();
        dynamicText.visible = createBindingPtr();
        dynamicText.color = createBindingPtr();

        references.add(dynamicText.fontParam.ttfFont);
        references.add(dynamicText.visible.id);
        references.add(dynamicText.color.id);
        references.add(dynamicText.content.id);

        return new DynamicTextAdapter(dynamicText);
    }

    private BitmapDrawable createBitmapDrawable(Set<Integer> references) {
        BitmapDrawable bmpResource = new BitmapDrawable();
        bmpResource.id = mUniqueIdGenerator.nextId();
        bmpResource.visible = createBindingPtr();
        bmpResource.bitmap = new Bitmap();
        bmpResource.bitmap.data = null;

        references.add(bmpResource.visible.id);

        return bmpResource;
    }

    private UnaryOperation createUnaryOperation(Set<Integer> references) {
        UnaryOperation unaryOperation = new UnaryOperation();
        unaryOperation.id = mUniqueIdGenerator.nextId();
        unaryOperation.op = UnaryOperationType.TO_INT;
        unaryOperation.arg1 = createBindingPtr();

        references.add(unaryOperation.arg1.id);

        return unaryOperation;
    }

    private BinaryOperation createBinaryOperation(Set<Integer> references) {
        BinaryOperation binaryOperation = new BinaryOperation();
        binaryOperation.id = mUniqueIdGenerator.nextId();
        binaryOperation.op = BinaryOperationType.ADD;
        binaryOperation.arg1 = createBindingPtr();
        binaryOperation.arg2 = createBindingPtr();

        references.add(binaryOperation.arg1.id);
        references.add(binaryOperation.arg2.id);

        return binaryOperation;
    }

    private TernaryOperation createTernaryOperation(Set<Integer> references) {
        TernaryOperation ternaryOperation = new TernaryOperation();
        ternaryOperation.id = mUniqueIdGenerator.nextId();
        ternaryOperation.op = TernaryOperationType.IF_ELSE;
        ternaryOperation.arg1 = createBindingPtr();
        ternaryOperation.arg2 = createBindingPtr();
        ternaryOperation.arg3 = createBindingPtr();

        references.add(ternaryOperation.arg1.id);
        references.add(ternaryOperation.arg2.id);
        references.add(ternaryOperation.arg3.id);

        return ternaryOperation;
    }

    private RectShape createRectShape(Set<Integer> references) {
        RectShape o = new RectShape();
        o.id = mUniqueIdGenerator.nextId();
        o.visible = createBindingPtr();
        o.height = createBindingPtr();
        o.width = createBindingPtr();
        o.shapeParam = createShapeParam(references);

        references.add(o.visible.id);
        references.add(o.height.id);
        references.add(o.width.id);

        return o;
    }

    private RoundRectShape createRoundRectShape(Set<Integer> references) {
        RoundRectShape o = new RoundRectShape();
        o.id = mUniqueIdGenerator.nextId();
        o.visible = createBindingPtr();
        o.height = createBindingPtr();
        o.width = createBindingPtr();
        o.cornerRadius = createBindingPtr();
        o.shapeParam = createShapeParam(references);

        references.add(o.visible.id);
        references.add(o.height.id);
        references.add(o.width.id);
        references.add(o.cornerRadius.id);

        return o;
    }

    private ArcPath createArcPath(Set<Integer> references) {
        ArcPath o = new ArcPath();
        o.id = mUniqueIdGenerator.nextId();
        o.visible = createBindingPtr();
        o.height = createBindingPtr();
        o.width = createBindingPtr();
        o.startDeg = createBindingPtr();
        o.sweepDeg = createBindingPtr();
        o.shapeParam = createShapeParam(references);

        references.add(o.visible.id);
        references.add(o.height.id);
        references.add(o.width.id);
        references.add(o.startDeg.id);
        references.add(o.sweepDeg.id);

        return o;
    }

    private ShapeParam createShapeParam(Set<Integer> references) {
        ShapeParam p = new ShapeParam();
        p.color = createBindingPtr();

        references.add(p.color.id);

        return p;
    }
}
