/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.annotation.Nullable;

import com.android.internal.widget.remotecompose.core.Operation;
import com.android.internal.widget.remotecompose.core.Operations;
import com.android.internal.widget.remotecompose.core.PaintContext;
import com.android.internal.widget.remotecompose.core.PaintOperation;
import com.android.internal.widget.remotecompose.core.WireBuffer;
import com.android.internal.widget.remotecompose.core.documentation.DocumentationBuilder;
import com.android.internal.widget.remotecompose.core.operations.layout.LayoutComponent;
import com.android.internal.widget.remotecompose.core.serialize.MapSerializer;
import com.android.internal.widget.remotecompose.core.serialize.Serializable;

import java.util.List;

/** The DrawContent command */
public class DrawContent extends PaintOperation implements Serializable {
    private static final int OP_CODE = Operations.DRAW_CONTENT;
    private static final String CLASS_NAME = "DrawContent";
    private @Nullable LayoutComponent mComponent;

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer);
    }

    /**
     * Set the component to be painted
     *
     * @param component
     */
    public void setComponent(@Nullable LayoutComponent component) {
        mComponent = component;
    }

    @NonNull
    @Override
    public String toString() {
        return "DrawContent;";
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        DrawContent op = new DrawContent();
        operations.add(op);
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
     * add a draw content operation to the buffer
     *
     * @param buffer the buffer to add to
     */
    public static void apply(@NonNull WireBuffer buffer) {
        buffer.start(Operations.DRAW_CONTENT);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Layout Operations", OP_CODE, CLASS_NAME)
                .description("Draw the component content");
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        if (mComponent != null) {
            mComponent.drawContent(context);
        }
    }

    @Override
    public void serialize(MapSerializer serializer) {
        serializer.addType(CLASS_NAME);
    }
}
