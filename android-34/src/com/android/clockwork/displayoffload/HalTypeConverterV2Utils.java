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

import java.util.ArrayList;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import vendor.google_clockwork.displayoffload.V2_0.BinaryOperation;
import vendor.google_clockwork.displayoffload.V2_0.BindingPtr;
import vendor.google_clockwork.displayoffload.V2_0.Primitive;
import vendor.google_clockwork.displayoffload.V2_0.Primitive.PrimitiveSafeUnion;
import vendor.google_clockwork.displayoffload.V2_0.RotationGroup;
import vendor.google_clockwork.displayoffload.V2_0.TernaryOperation;
import vendor.google_clockwork.displayoffload.V2_0.TernaryOperationType;
import vendor.google_clockwork.displayoffload.V2_0.TranslationGroup;
import vendor.google_clockwork.displayoffload.V2_0.Type;
import vendor.google_clockwork.displayoffload.V2_0.UnaryOperation;
import vendor.google_clockwork.displayoffload.V2_0.UnaryOperationType;

class HalTypeConverterV2Utils {
    static TernaryOperation ternaryOp(int id,
            BindingPtr condition, BindingPtr ifTrue, BindingPtr ifFalse) {
        TernaryOperation op = new TernaryOperation();
        op.id = id;
        op.op = TernaryOperationType.IF_ELSE;
        op.arg1 = condition;
        op.arg2 = ifTrue;
        op.arg3 = ifFalse;
        return op;
    }

    static BinaryOperation binaryOp(int id, byte type, BindingPtr arg1, BindingPtr arg2) {
        BinaryOperation op = new BinaryOperation();
        op.id = id;
        op.op = type;
        op.arg1 = arg1;
        op.arg2 = arg2;
        return op;
    }

    static UnaryOperation unaryOp(int id, byte type, BindingPtr arg1) {
        UnaryOperation op = new UnaryOperation();
        op.id = id;
        op.op = type;
        op.arg1 = arg1;
        return op;
    }

    static UnaryOperation round(int id, BindingPtr bindingPtr) {
        return unaryOp(id, UnaryOperationType.ROUND, bindingPtr);
    }

    static BindingPtr bindingPtr(int id, short type) {
        BindingPtr bindingPtr = new BindingPtr();
        bindingPtr.id = id;
        bindingPtr.type = type;
        return bindingPtr;
    }

    static Primitive primitive(int id, PrimitiveSafeUnion val) {
        Primitive primitive = new Primitive();
        primitive.id = id;
        primitive.val = val;
        return primitive;
    }

    static BindingPtr bindingPtr(Primitive primitive) {
        BindingPtr bindingPtr = new BindingPtr();
        bindingPtr.id = primitive.id;
        switch (primitive.val.getDiscriminator()) {
            case PrimitiveSafeUnion.hidl_discriminator.boolVal:
                bindingPtr.type = Type.BOOL;
                break;
            case PrimitiveSafeUnion.hidl_discriminator.floatVal:
                bindingPtr.type = Type.FLOAT;
                break;
            case PrimitiveSafeUnion.hidl_discriminator.int32Val:
                bindingPtr.type = Type.INT32;
                break;
            case PrimitiveSafeUnion.hidl_discriminator.int64Val:
                bindingPtr.type = Type.INT64;
                break;
            case PrimitiveSafeUnion.hidl_discriminator.uint32Val:
                bindingPtr.type = Type.UINT32;
                break;
            case PrimitiveSafeUnion.hidl_discriminator.uint64Val:
                bindingPtr.type = Type.UINT64;
                break;
            default:
                bindingPtr.type = Type.NOT_BINDABLE;
                break;
        }
        return bindingPtr;
    }

    static BindingPtr bindingPtr(UnaryOperation unOp, short type) {
        BindingPtr bindingPtr = new BindingPtr();
        bindingPtr.id = unOp.id;
        bindingPtr.type = type;
        return bindingPtr;
    }

    static BindingPtr bindingPtr(BinaryOperation binOp, short type) {
        BindingPtr bindingPtr = new BindingPtr();
        bindingPtr.id = binOp.id;
        bindingPtr.type = type;
        return bindingPtr;
    }

    static BindingPtr bindingPtr(TernaryOperation TerOp, short type) {
        BindingPtr bindingPtr = new BindingPtr();
        bindingPtr.id = TerOp.id;
        bindingPtr.type = type;
        return bindingPtr;
    }

    static <T> BindingPtr primitiveBindingPtr(
            Map<Object, Primitive> primitiveCache,
            Consumer<ResourceObject> addToList,
            BiConsumer<PrimitiveSafeUnion, T> assignFunc,
            int id,
            T value) {
        Primitive cached = primitiveCache.get(value);
        if (cached != null) return bindingPtr(cached);

        PrimitiveSafeUnion valUnion = new PrimitiveSafeUnion();
        assignFunc.accept(valUnion, value);
        Primitive newlyCreated = primitive(id, valUnion);
        addToList.accept(ResourceObject.of(newlyCreated.id, newlyCreated));
        primitiveCache.put(value, newlyCreated);
        return bindingPtr(newlyCreated);
    }

    // TODO(b/259152785): Make xValue & yValue bindable float, like degValue. Write test.
    static TranslationGroup translationGroup(
            int id,
            ArrayList<Integer> remappedContent,
            BindingPtr xValue,
            BindingPtr yValue,
            BindingPtr visible) {
        vendor.google_clockwork.displayoffload.V2_0.TranslationGroup group
                = new vendor.google_clockwork.displayoffload.V2_0.TranslationGroup();
        group.id = id;
        group.visible = visible;
        group.contents = remappedContent;
        group.offsetX = xValue;
        group.offsetY = yValue;
        return group;
    }

    static RotationGroup rotationGroup(
            int id,
            ArrayList<Integer> remappedContent,
            BindingPtr xValue,
            BindingPtr yValue,
            BindingPtr degValue,
            BindingPtr visible) {
        vendor.google_clockwork.displayoffload.V2_0.RotationGroup group
                = new vendor.google_clockwork.displayoffload.V2_0.RotationGroup();
        group.id = id;
        group.visible = visible;
        group.contents = remappedContent;
        group.pivotX = xValue;
        group.pivotY = yValue;
        group.angleDeg = degValue;
        return group;
    }
}
