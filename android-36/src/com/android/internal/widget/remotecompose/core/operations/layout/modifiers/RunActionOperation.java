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
package com.android.internal.widget.remotecompose.core.operations.layout.modifiers;

import android.annotation.NonNull;

import com.android.internal.widget.remotecompose.core.CoreDocument;
import com.android.internal.widget.remotecompose.core.Operation;
import com.android.internal.widget.remotecompose.core.Operations;
import com.android.internal.widget.remotecompose.core.PaintContext;
import com.android.internal.widget.remotecompose.core.PaintOperation;
import com.android.internal.widget.remotecompose.core.WireBuffer;
import com.android.internal.widget.remotecompose.core.documentation.DocumentationBuilder;
import com.android.internal.widget.remotecompose.core.operations.layout.ActionOperation;
import com.android.internal.widget.remotecompose.core.operations.layout.Component;
import com.android.internal.widget.remotecompose.core.operations.layout.Container;
import com.android.internal.widget.remotecompose.core.serialize.MapSerializer;

import java.util.ArrayList;
import java.util.List;

/** Contains actions and immediately runs them */
public class RunActionOperation extends PaintOperation implements Container {
    private static final int OP_CODE = Operations.RUN_ACTION;
    private static final String CLASS_NAME = "RunActionOperation";

    @NonNull public ArrayList<Operation> mList = new ArrayList<>();

    public RunActionOperation() {}

    @NonNull
    @Override
    public String toString() {
        return "RunActionOperation()";
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

    /**
     * Returns the serialized name for this operation
     *
     * @return the serialized name
     */
    @NonNull
    public String serializedName() {
        return "RUN_ACTION";
    }

    @Override
    public void serialize(MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("list", mList);
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        CoreDocument document = context.getContext().getDocument();
        Component component = context.getContext().mLastComponent;
        if (document == null || component == null) {
            return;
        }
        for (Operation op : getList()) {
            if (op instanceof ActionOperation) {
                ActionOperation actionOperation = (ActionOperation) op;
                actionOperation.runAction(context.getContext(), document, component, 0f, 0f);
            }
        }
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    @Override
    public boolean isDirty() {
        // Always execute
        return true;
    }

    @Override
    public void markNotDirty() {
        // nothing
    }

    @Override
    public void markDirty() {
        // nothing
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer);
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer a WireBuffer
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
        operations.add(new RunActionOperation());
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Operations", OP_CODE, "RunAction")
                .description("This operation runs child actions");
    }
}
