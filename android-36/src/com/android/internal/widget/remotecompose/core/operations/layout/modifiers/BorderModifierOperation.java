/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.internal.widget.remotecompose.core.operations.layout.modifiers;

import static com.android.internal.widget.remotecompose.core.documentation.DocumentedOperation.FLOAT;

import android.annotation.NonNull;

import com.android.internal.widget.remotecompose.core.Operation;
import com.android.internal.widget.remotecompose.core.Operations;
import com.android.internal.widget.remotecompose.core.PaintContext;
import com.android.internal.widget.remotecompose.core.RemoteContext;
import com.android.internal.widget.remotecompose.core.WireBuffer;
import com.android.internal.widget.remotecompose.core.documentation.DocumentationBuilder;
import com.android.internal.widget.remotecompose.core.operations.layout.Component;
import com.android.internal.widget.remotecompose.core.operations.paint.PaintBundle;
import com.android.internal.widget.remotecompose.core.operations.utilities.StringSerializer;
import com.android.internal.widget.remotecompose.core.serialize.MapSerializer;
import com.android.internal.widget.remotecompose.core.serialize.SerializeTags;

import java.util.List;

/** Component size-aware border draw */
public class BorderModifierOperation extends DecoratorModifierOperation {
    private static final int OP_CODE = Operations.MODIFIER_BORDER;
    public static final String CLASS_NAME = "BorderModifierOperation";

    float mX;
    float mY;
    float mWidth;
    float mHeight;
    float mBorderWidth;
    float mRoundedCorner;
    float mR;
    float mG;
    float mB;
    float mA;
    int mShapeType = ShapeType.RECTANGLE;

    @NonNull public PaintBundle paint = new PaintBundle();

    public BorderModifierOperation(
            float x,
            float y,
            float width,
            float height,
            float borderWidth,
            float roundedCorner,
            float r,
            float g,
            float b,
            float a,
            int shapeType) {
        this.mX = x;
        this.mY = y;
        this.mWidth = width;
        this.mHeight = height;
        this.mBorderWidth = borderWidth;
        this.mRoundedCorner = roundedCorner;
        this.mR = r;
        this.mG = g;
        this.mB = b;
        this.mA = a;
        this.mShapeType = shapeType;
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                "BORDER = ["
                        + mX
                        + ", "
                        + mY
                        + ", "
                        + mWidth
                        + ", "
                        + mHeight
                        + "] "
                        + "color ["
                        + mR
                        + ", "
                        + mG
                        + ", "
                        + mB
                        + ", "
                        + mA
                        + "] "
                        + "border ["
                        + mBorderWidth
                        + ", "
                        + mRoundedCorner
                        + "] "
                        + "shape ["
                        + mShapeType
                        + "]");
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(
                buffer,
                mX,
                mY,
                mWidth,
                mHeight,
                mBorderWidth,
                mRoundedCorner,
                mR,
                mG,
                mB,
                mA,
                mShapeType);
    }

    @Override
    public void layout(
            @NonNull RemoteContext context, Component component, float width, float height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    @NonNull
    @Override
    public String toString() {
        return "BorderModifierOperation("
                + mX
                + ","
                + mY
                + " - "
                + mWidth
                + " x "
                + mHeight
                + ") "
                + "borderWidth("
                + mBorderWidth
                + ") "
                + "color("
                + mR
                + ","
                + mG
                + ","
                + mB
                + ","
                + mA
                + ")";
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return CLASS_NAME;
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return OP_CODE;
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer the WireBuffer
     * @param x x coordinate of the border rect
     * @param y y coordinate of the border rect
     * @param width width of the border rect
     * @param height height of the border rect
     * @param borderWidth the width of the border outline
     * @param roundedCorner rounded corner value in pixels
     * @param r red component of the border color
     * @param g green component of the border color
     * @param b blue component of the border color
     * @param a alpha component of the border color
     * @param shapeType the shape type (0 = RECTANGLE, 1 = CIRCLE)
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            float x,
            float y,
            float width,
            float height,
            float borderWidth,
            float roundedCorner,
            float r,
            float g,
            float b,
            float a,
            int shapeType) {
        buffer.start(OP_CODE);
        buffer.writeFloat(x);
        buffer.writeFloat(y);
        buffer.writeFloat(width);
        buffer.writeFloat(height);
        buffer.writeFloat(borderWidth);
        buffer.writeFloat(roundedCorner);
        buffer.writeFloat(r);
        buffer.writeFloat(g);
        buffer.writeFloat(b);
        buffer.writeFloat(a);
        // shape type
        buffer.writeInt(shapeType);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        float x = buffer.readFloat();
        float y = buffer.readFloat();
        float width = buffer.readFloat();
        float height = buffer.readFloat();
        float bw = buffer.readFloat();
        float rc = buffer.readFloat();
        float r = buffer.readFloat();
        float g = buffer.readFloat();
        float b = buffer.readFloat();
        float a = buffer.readFloat();
        // shape type
        int shapeType = buffer.readInt();
        operations.add(
                new BorderModifierOperation(x, y, width, height, bw, rc, r, g, b, a, shapeType));
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        context.savePaint();
        paint.reset();
        paint.setColor(mR, mG, mB, mA);
        paint.setStrokeWidth(mBorderWidth * context.getContext().getDensity());
        paint.setStyle(PaintBundle.STYLE_STROKE);
        context.replacePaint(paint);
        if (mShapeType == ShapeType.RECTANGLE) {
            context.drawRect(0f, 0f, mWidth, mHeight);
        } else {
            float size = mRoundedCorner;
            if (mShapeType == ShapeType.CIRCLE) {
                size = Math.min(mWidth, mHeight) / 2f;
            }
            context.drawRoundRect(0f, 0f, mWidth, mHeight, size, size);
        }
        context.restorePaint();
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Modifier Operations", OP_CODE, CLASS_NAME)
                .description("define the Border Modifier")
                .field(FLOAT, "x", "")
                .field(FLOAT, "y", "")
                .field(FLOAT, "width", "")
                .field(FLOAT, "height", "")
                .field(FLOAT, "borderWidth", "")
                .field(FLOAT, "roundedCorner", "")
                .field(FLOAT, "r", "")
                .field(FLOAT, "g", "")
                .field(FLOAT, "b", "")
                .field(FLOAT, "a", "")
                .field(FLOAT, "shapeType", "");
    }

    @Override
    public void serialize(MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.MODIFIER)
                .addType("BorderModifierOperation")
                .add("x", mX)
                .add("y", mY)
                .add("width", mWidth)
                .add("height", mHeight)
                .add("borderWidth", mBorderWidth)
                .add("roundedCornerRadius", mRoundedCorner)
                .add("color", mA, mR, mG, mB)
                .add("shapeType", ShapeType.getString(mShapeType));
    }
}
