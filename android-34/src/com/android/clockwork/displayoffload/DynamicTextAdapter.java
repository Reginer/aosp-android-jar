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

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_CONVERSION_FAILURE;

import android.util.Log;

import vendor.google_clockwork.displayoffload.V2_0.BindingPtr;

/**
 * DynamicTextAdapter is a wrapper class for different versions of hal type dynamic text.
 *
 * The purpose of this class is to hide the version information from other components. So, those
 * components can be reused along the Hal's evolution.
 *
 * The usage of the highest available version is always preferred.
 */
class DynamicTextAdapter {
    private static final String TAG = "DynamicTextAdapter";

    private final vendor.google_clockwork.displayoffload.V1_1.DynamicTextResource mDynamicTextV1;
    private final vendor.google_clockwork.displayoffload.V2_0.DynamicText mDynamicTextV2;

    DynamicTextAdapter(
            vendor.google_clockwork.displayoffload.V1_1.DynamicTextResource dynamicTextResourceV1) {
        this.mDynamicTextV1 = dynamicTextResourceV1;
        this.mDynamicTextV2 = null;
    }

    DynamicTextAdapter(vendor.google_clockwork.displayoffload.V2_0.DynamicText dynamicTextV2) {
        this.mDynamicTextV1 = null;
        this.mDynamicTextV2 = dynamicTextV2;
    }

    vendor.google_clockwork.displayoffload.V1_1.DynamicTextResource getV1()
            throws DisplayOffloadException {
        if (mDynamicTextV1 != null) {
            return mDynamicTextV1;
        }
        throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                "Failed to get DynamicTextResource V1.1.");
    }

    vendor.google_clockwork.displayoffload.V2_0.DynamicText getV2() throws DisplayOffloadException {
        if (mDynamicTextV2 != null) {
            return mDynamicTextV2;
        }
        throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                "Failed to get DynamicText V2.0.");
    }

    int getId() {
        if (mDynamicTextV2 != null) {
            return mDynamicTextV2.id;
        }
        if (mDynamicTextV1 != null) {
            return mDynamicTextV1.id;
        }
        Log.e(TAG, this + ": getId is not supported.");
        return -1;
    }

    int getFontId() {
        if (mDynamicTextV2 != null) {
            return mDynamicTextV2.fontParam.ttfFont;
        }
        if (mDynamicTextV1 != null) {
            return mDynamicTextV1.textParam.ttfFont;
        }
        Log.e(TAG, this + ": getFontId is not supported.");
        return -1;
    }

    BindingPtr getVisibility() {
        if (mDynamicTextV2 != null) {
            return mDynamicTextV2.visible;
        }
        Log.e(TAG, this + ": getVisibility is not supported.");
        return null;
    }

    BindingPtr getColor() {
        if (mDynamicTextV2 != null) {
            return mDynamicTextV2.color;
        }
        Log.e(TAG, this + ": getColor is not supported.");
        return null;
    }

    @Override
    public String toString() {
        String dump = "invalid adapter";
        if (mDynamicTextV2 != null) {
            dump = String.format("id=%d fontId=%d (content->%d)", mDynamicTextV2.id,
                    mDynamicTextV2.fontParam.ttfFont, mDynamicTextV2.content.id);
        } else if (mDynamicTextV1 != null) {
            dump = String.format("id=%d fontId=%d (content->%s)", mDynamicTextV1.id,
                    mDynamicTextV1.textParam.ttfFont, mDynamicTextV1.bindings);
        }
        return "[" + getClass().getSimpleName() + " " + dump + "]";
    }
}
