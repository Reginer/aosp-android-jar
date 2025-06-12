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
package com.android.internal.widget.remotecompose.core.operations.layout;

import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.internal.widget.remotecompose.core.Operation;
import com.android.internal.widget.remotecompose.core.Operations;
import com.android.internal.widget.remotecompose.core.PaintContext;
import com.android.internal.widget.remotecompose.core.PaintOperation;
import com.android.internal.widget.remotecompose.core.RemoteContext;
import com.android.internal.widget.remotecompose.core.VariableSupport;
import com.android.internal.widget.remotecompose.core.WireBuffer;
import com.android.internal.widget.remotecompose.core.documentation.DocumentationBuilder;
import com.android.internal.widget.remotecompose.core.operations.ComponentValue;
import com.android.internal.widget.remotecompose.core.serialize.MapSerializer;
import com.android.internal.widget.remotecompose.core.serialize.Serializable;

import java.util.ArrayList;
import java.util.List;

/** Represents a list of canvas operations. */
public class CanvasOperations extends PaintOperation
        implements VariableSupport, Container, Serializable {
    private static final int OP_CODE = Operations.CANVAS_OPERATIONS;
    private static final String CLASS_NAME = "CanvasOperations";

    @NonNull public ArrayList<Operation> mList = new ArrayList<>();
    @Nullable LayoutComponent mComponent;

    /** The constructor */
    public CanvasOperations() {}

    @Override
    public void registerListening(RemoteContext context) {
        for (Operation operation : mList) {
            if (operation instanceof VariableSupport) {
                VariableSupport variableSupport = (VariableSupport) operation;
                variableSupport.registerListening(context);
            }
            if (operation instanceof ComponentValue) {
                ComponentValue v = (ComponentValue) operation;
                mComponent.addComponentValue(v);
            }
        }
    }

    @Override
    public void updateVariables(RemoteContext context) {
        for (Operation operation : mList) {
            if (operation instanceof VariableSupport) {
                VariableSupport variableSupport = (VariableSupport) operation;
                variableSupport.updateVariables(context);
            }
        }
    }

    /**
     * The returns a list to be filled
     *
     * @return list to be filled
     */
    @NonNull
    public ArrayList<Operation> getList() {
        return mList;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(CLASS_NAME + "\n");
        for (Operation operation : mList) {
            builder.append("  ");
            builder.append(operation);
            builder.append("\n");
        }
        return builder.toString();
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        RemoteContext remoteContext = context.getContext();
        for (Operation op : mList) {
            if (op instanceof VariableSupport && op.isDirty()) {
                ((VariableSupport) op).updateVariables(context.getContext());
            }
            remoteContext.incrementOpCount();
            op.apply(context.getContext());
        }
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "Loop";
    }

    /**
     * Apply this operation to the buffer
     *
     * @param buffer
     */
    public static void apply(@NonNull WireBuffer buffer) {
        buffer.start(OP_CODE);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        operations.add(new CanvasOperations());
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Operations", OP_CODE, name())
                .description("Impulse Process that runs a list of operations");
    }

    @Override
    public void serialize(MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("list", mList);
    }

    /**
     * Set layout component
     *
     * @param layoutComponent
     */
    public void setComponent(@Nullable LayoutComponent layoutComponent) {
        mComponent = layoutComponent;
        if (layoutComponent != null) {
            layoutComponent.setCanvasOperations(this);
        }
    }
}
