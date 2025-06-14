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
package com.android.internal.widget.remotecompose.core.operations;

import android.annotation.NonNull;

import com.android.internal.widget.remotecompose.core.Operation;
import com.android.internal.widget.remotecompose.core.Operations;
import com.android.internal.widget.remotecompose.core.PaintContext;
import com.android.internal.widget.remotecompose.core.PaintOperation;
import com.android.internal.widget.remotecompose.core.WireBuffer;
import com.android.internal.widget.remotecompose.core.documentation.DocumentationBuilder;
import com.android.internal.widget.remotecompose.core.documentation.DocumentedOperation;
import com.android.internal.widget.remotecompose.core.semantics.AccessibleComponent;
import com.android.internal.widget.remotecompose.core.serialize.MapSerializer;

import java.util.List;

/** Operation to draw a given cached bitmap */
public class DrawBitmapInt extends PaintOperation implements AccessibleComponent {
    private static final int OP_CODE = Operations.DRAW_BITMAP_INT;
    private static final String CLASS_NAME = "DrawBitmapInt";
    int mImageId;
    int mSrcLeft;
    int mSrcTop;
    int mSrcRight;
    int mSrcBottom;
    int mDstLeft;
    int mDstTop;
    int mDstRight;
    int mDstBottom;
    int mContentDescId = 0;

    public DrawBitmapInt(
            int imageId,
            int srcLeft,
            int srcTop,
            int srcRight,
            int srcBottom,
            int dstLeft,
            int dstTop,
            int dstRight,
            int dstBottom,
            int cdId) {
        this.mImageId = imageId;
        this.mSrcLeft = srcLeft;
        this.mSrcTop = srcTop;
        this.mSrcRight = srcRight;
        this.mSrcBottom = srcBottom;
        this.mDstLeft = dstLeft;
        this.mDstTop = dstTop;
        this.mDstRight = dstRight;
        this.mDstBottom = dstBottom;
        this.mContentDescId = cdId;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(
                buffer,
                mImageId,
                mSrcLeft,
                mSrcTop,
                mSrcRight,
                mSrcBottom,
                mDstLeft,
                mDstTop,
                mDstRight,
                mDstBottom,
                mContentDescId);
    }

    @NonNull
    @Override
    public String toString() {
        return "DRAW_BITMAP_INT "
                + mImageId
                + " on "
                + mSrcLeft
                + " "
                + mSrcTop
                + " "
                + mSrcRight
                + " "
                + mSrcBottom
                + " "
                + "- "
                + mDstLeft
                + " "
                + mDstTop
                + " "
                + mDstRight
                + " "
                + mDstBottom
                + ";";
    }

    @Override
    public Integer getContentDescriptionId() {
        return mContentDescId;
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
     * Draw a bitmap using integer coordinates
     *
     * @param buffer the buffer to write to
     * @param imageId the id of the bitmap
     * @param srcLeft the left most pixel in the bitmap
     * @param srcTop the top most pixel in the bitmap
     * @param srcRight the right most pixel in the bitmap
     * @param srcBottom the bottom most pixel in the bitmap
     * @param dstLeft the left most pixel in the destination
     * @param dstTop the top most pixel in the destination
     * @param dstRight the right most pixel in the destination
     * @param dstBottom the bottom most pixel in the destination
     * @param cdId the content discription id
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int imageId,
            int srcLeft,
            int srcTop,
            int srcRight,
            int srcBottom,
            int dstLeft,
            int dstTop,
            int dstRight,
            int dstBottom,
            int cdId) {
        buffer.start(Operations.DRAW_BITMAP_INT);
        buffer.writeInt(imageId);
        buffer.writeInt(srcLeft);
        buffer.writeInt(srcTop);
        buffer.writeInt(srcRight);
        buffer.writeInt(srcBottom);
        buffer.writeInt(dstLeft);
        buffer.writeInt(dstTop);
        buffer.writeInt(dstRight);
        buffer.writeInt(dstBottom);
        buffer.writeInt(cdId);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int imageId = buffer.readInt();
        int sLeft = buffer.readInt();
        int srcTop = buffer.readInt();
        int srcRight = buffer.readInt();
        int srcBottom = buffer.readInt();
        int dstLeft = buffer.readInt();
        int dstTop = buffer.readInt();
        int dstRight = buffer.readInt();
        int dstBottom = buffer.readInt();
        int cdId = buffer.readInt();
        DrawBitmapInt op =
                new DrawBitmapInt(
                        imageId, sLeft, srcTop, srcRight, srcBottom, dstLeft, dstTop, dstRight,
                        dstBottom, cdId);

        operations.add(op);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Draw Operations", OP_CODE, CLASS_NAME)
                .description("Draw a bitmap using integer coordinates")
                .field(DocumentedOperation.INT, "id", "id of bitmap")
                .field(DocumentedOperation.INT, "srcLeft", "The left side of the image")
                .field(DocumentedOperation.INT, "srcTop", "The top of the image")
                .field(DocumentedOperation.INT, "srcRight", "The right side of the image")
                .field(DocumentedOperation.INT, "srcBottom", "The bottom of the image")
                .field(DocumentedOperation.INT, "dstLeft", "The left side of the image")
                .field(DocumentedOperation.INT, "dstTop", "The top of the image")
                .field(DocumentedOperation.INT, "dstRight", "The right side of the image")
                .field(DocumentedOperation.INT, "dstBottom", "The bottom of the image")
                .field(DocumentedOperation.INT, "cdId", "id of string");
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        context.drawBitmap(
                mImageId,
                mSrcLeft,
                mSrcTop,
                mSrcRight,
                mSrcBottom,
                mDstLeft,
                mDstTop,
                mDstRight,
                mDstBottom,
                mContentDescId);
    }

    @Override
    public void serialize(MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("imageId", mImageId)
                .add("contentDescriptionId", mContentDescId)
                .add("srcLeft", mSrcLeft)
                .add("srcTop", mSrcTop)
                .add("srcRight", mSrcRight)
                .add("srcBottom", mSrcBottom)
                .add("dstLeft", mDstLeft)
                .add("dstTop", mDstTop)
                .add("dstRight", mDstRight)
                .add("dstBottom", mDstBottom);
    }
}
