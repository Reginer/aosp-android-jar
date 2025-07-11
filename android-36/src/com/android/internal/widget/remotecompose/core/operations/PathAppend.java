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

import static com.android.internal.widget.remotecompose.core.documentation.DocumentedOperation.FLOAT_ARRAY;
import static com.android.internal.widget.remotecompose.core.documentation.DocumentedOperation.INT;

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
import com.android.internal.widget.remotecompose.core.documentation.DocumentedOperation;
import com.android.internal.widget.remotecompose.core.serialize.MapSerializer;
import com.android.internal.widget.remotecompose.core.serialize.Serializable;

import java.util.Arrays;
import java.util.List;

public class PathAppend extends PaintOperation implements VariableSupport, Serializable {
    private static final int OP_CODE = Operations.PATH_ADD;
    private static final String CLASS_NAME = "PathAppend";
    int mInstanceId;
    float[] mFloatPath;
    float[] mOutputPath;

    PathAppend(int instanceId, float[] floatPath) {
        mInstanceId = instanceId;
        mFloatPath = floatPath;
        mOutputPath = Arrays.copyOf(mFloatPath, mFloatPath.length);
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        for (int i = 0; i < mFloatPath.length; i++) {
            float v = mFloatPath[i];
            if (Utils.isVariable(v)) {
                mOutputPath[i] = Float.isNaN(v) ? context.getFloat(Utils.idFromNan(v)) : v;
            } else {
                mOutputPath[i] = v;
            }
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        for (float v : mFloatPath) {
            if (Float.isNaN(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mInstanceId, mOutputPath);
    }

    @NonNull
    @Override
    public String deepToString(String indent) {
        return PathData.pathString(mFloatPath);
    }

    @NonNull
    @Override
    public String toString() {
        return "PathAppend[" + mInstanceId + "] += " + "\"" + pathString(mOutputPath) + "\"";
    }

    /**
     * public float[] getFloatPath(PaintContext context) { float[] ret = mRetFloats; // Assume
     * retFloats is declared elsewhere if (ret == null) { return mFloatPath; // Assume floatPath is
     * declared elsewhere } float[] localRef = mRef; // Assume ref is of type Float[] if (localRef
     * == null) { for (int i = 0; i < mFloatPath.length; i++) { ret[i] = mFloatPath[i]; } } else {
     * for (int i = 0; i < mFloatPath.length; i++) { float lr = localRef[i]; if (Float.isNaN(lr)) {
     * ret[i] = Utils.getActualValue(lr); } else { ret[i] = mFloatPath[i]; } } } return ret; }
     */
    public static final int MOVE = 10;

    public static final int LINE = 11;
    public static final int QUADRATIC = 12;
    public static final int CONIC = 13;
    public static final int CUBIC = 14;
    public static final int CLOSE = 15;
    public static final int DONE = 16;
    public static final int RESET = 17;
    public static final float MOVE_NAN = Utils.asNan(MOVE);
    public static final float LINE_NAN = Utils.asNan(LINE);
    public static final float QUADRATIC_NAN = Utils.asNan(QUADRATIC);
    public static final float CONIC_NAN = Utils.asNan(CONIC);
    public static final float CUBIC_NAN = Utils.asNan(CUBIC);
    public static final float CLOSE_NAN = Utils.asNan(CLOSE);
    public static final float DONE_NAN = Utils.asNan(DONE);
    public static final float RESET_NAN = Utils.asNan(RESET);

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
     * add a path append operation to the buffer. With PathCreate allows you create a path
     * dynamically
     *
     * @param buffer add the data to this buffer
     * @param id id of the path
     * @param data the path data to append
     */
    public static void apply(@NonNull WireBuffer buffer, int id, @NonNull float[] data) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeInt(data.length);
        for (float datum : data) {
            buffer.writeFloat(datum);
        }
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readInt();
        int len = buffer.readInt();
        float[] data = new float[len];
        for (int i = 0; i < data.length; i++) {
            data[i] = buffer.readFloat();
        }
        operations.add(new PathAppend(id, data));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description("Append to a Path")
                .field(DocumentedOperation.INT, "id", "id string")
                .field(INT, "length", "id string")
                .field(FLOAT_ARRAY, "pathData", "length", "path encoded as floats");
    }

    @Override
    public void paint(PaintContext context) {
        apply(context.getContext());
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        float[] data = context.getPathData(mInstanceId);
        float[] out = mOutputPath;
        if (Float.floatToRawIntBits(out[0]) == Float.floatToRawIntBits(RESET_NAN)) {
            context.loadPathData(mInstanceId, new float[0]);
            return;
        }
        if (data != null) {
            out = new float[data.length + mOutputPath.length];

            for (int i = 0; i < data.length; i++) {
                out[i] = data[i];
            }
            for (int i = 0; i < mOutputPath.length; i++) {
                out[i + data.length] = mOutputPath[i];
            }
        } else {
            System.out.println(">>>>>>>>>>> DATA IS NULL");
        }
        context.loadPathData(mInstanceId, out);
    }

    /**
     * Convert a path to a string
     *
     * @param path the path to convert
     * @return text representation of path
     */
    @NonNull
    public static String pathString(@Nullable float[] path) {
        if (path == null) {
            return "null";
        }
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < path.length; i++) {
            if (Float.isNaN(path[i])) {
                int id = Utils.idFromNan(path[i]); // Assume idFromNan is defined elsewhere
                if (id <= DONE) { // Assume DONE is a constant
                    switch (id) {
                        case MOVE:
                            str.append("M");
                            break;
                        case LINE:
                            str.append("L");
                            break;
                        case QUADRATIC:
                            str.append("Q");
                            break;
                        case CONIC:
                            str.append("R");
                            break;
                        case CUBIC:
                            str.append("C");
                            break;
                        case CLOSE:
                            str.append("Z");
                            break;
                        case DONE:
                            str.append(".");
                            break;
                        default:
                            str.append("[" + id + "]");
                            break;
                    }
                } else {
                    str.append("(" + id + ")");
                }
            }
        }
        return str.toString();
    }

    @Override
    public void serialize(MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("id", mInstanceId).addPath("path", mFloatPath);
    }
}
