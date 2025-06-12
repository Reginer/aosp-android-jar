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

package android.adservices.ondevicepersonalization;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.adservices.ondevicepersonalization.flags.Flags;

/**
 * The input data for
 * {@link IsolatedWorker#onRender(RenderInput, android.os.OutcomeReceiver)}.
 *
 */
public final class RenderInput {
    /** The width of the slot. */
    private int mWidth = 0;

    /** The height of the slot. */
    private int mHeight = 0;

    /**
     * A {@link RenderingConfig} within an {@link ExecuteOutput} that was returned by
     * {@link IsolatedWorker#onExecute(ExecuteInput, android.os.OutcomeReceiver)}.
     */
    @Nullable RenderingConfig mRenderingConfig = null;

    /** @hide */
    public RenderInput(@NonNull RenderInputParcel parcel) {
        this(parcel.getWidth(), parcel.getHeight(), parcel.getRenderingConfig());
    }

    /**
     * Creates a new RenderInput.
     *
     * @param width
     *   The width of the slot.
     * @param height
     *   The height of the slot.
     * @param renderingConfig
     *   A {@link RenderingConfig} within an {@link ExecuteOutput} that was returned by
     *   {@link IsolatedWorker#onExecute(ExecuteInput, android.os.OutcomeReceiver)}.
     */
    @FlaggedApi(Flags.FLAG_DATA_CLASS_MISSING_CTORS_AND_GETTERS_ENABLED)
    public RenderInput(
            int width,
            int height,
            @Nullable RenderingConfig renderingConfig) {
        this.mWidth = width;
        this.mHeight = height;
        this.mRenderingConfig = renderingConfig;

        // onConstructed(); // You can define this method to get a callback
    }

    /**
     * The width of the slot.
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * The height of the slot.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * A {@link RenderingConfig} within an {@link ExecuteOutput} that was returned by
     * {@link IsolatedWorker#onExecute(ExecuteInput, android.os.OutcomeReceiver)}.
     */
    public @Nullable RenderingConfig getRenderingConfig() {
        return mRenderingConfig;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        // You can override field equality logic by defining either of the methods like:
        // boolean fieldNameEquals(RenderInput other) { ... }
        // boolean fieldNameEquals(FieldType otherValue) { ... }

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        RenderInput that = (RenderInput) o;
        //noinspection PointlessBooleanExpression
        return true
                && mWidth == that.mWidth
                && mHeight == that.mHeight
                && java.util.Objects.equals(mRenderingConfig, that.mRenderingConfig);
    }

    @Override
    public int hashCode() {
        // You can override field hashCode logic by defining methods like:
        // int fieldNameHashCode() { ... }

        int _hash = 1;
        _hash = 31 * _hash + mWidth;
        _hash = 31 * _hash + mHeight;
        _hash = 31 * _hash + java.util.Objects.hashCode(mRenderingConfig);
        return _hash;
    }
}
