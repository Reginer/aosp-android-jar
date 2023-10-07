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

import java.util.ArrayList;

import vendor.google_clockwork.displayoffload.V2_0.BindingPtr;

/**
 * StaticTextAdapter is a wrapper class for different versions of hal type dynamic text.
 *
 * The purpose of this class is to hide the version information from other components. So, those
 * components can be reused along the Hal's evolution.
 *
 * The usage of the highest available version is always preferred.
 */
class StaticTextAdapter {
    private static final String TAG = "StaticTextAdapter";

    private final vendor.google_clockwork.displayoffload.V1_1.StaticTextResource mStaticTextV1_1;
    private final vendor.google_clockwork.displayoffload.V2_0.StaticText mStaticTextV2_0;

    StaticTextAdapter(
            vendor.google_clockwork.displayoffload.V1_1.StaticTextResource staticText) {
        this.mStaticTextV1_1 = staticText;
        this.mStaticTextV2_0 = null;
    }

    StaticTextAdapter(vendor.google_clockwork.displayoffload.V2_0.StaticText staticText) {
        this.mStaticTextV1_1 = null;
        this.mStaticTextV2_0 = staticText;
    }

    vendor.google_clockwork.displayoffload.V1_1.StaticTextResource getV1()
            throws DisplayOffloadException {
        if (mStaticTextV1_1 != null) {
            return mStaticTextV1_1;
        }
        throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                "Failed to get StaticTextResource V1.1.");
    }

    vendor.google_clockwork.displayoffload.V2_0.StaticText getV2()
            throws DisplayOffloadException {
        if (mStaticTextV2_0 != null) {
            return mStaticTextV2_0;
        }
        throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                "Failed to get StaticText V2.0.");
    }

    int getId() {
        if (mStaticTextV2_0 != null) {
            return mStaticTextV2_0.id;
        }
        if (mStaticTextV1_1 != null) {
            return mStaticTextV1_1.id;
        }
        Log.e(TAG, this + ": getId is not supported.");
        return -1;
    }

    String getOriginalString() {
        if (mStaticTextV2_0 != null) {
            return mStaticTextV2_0.originalString;
        }
        if (mStaticTextV1_1 != null) {
            return mStaticTextV1_1.value;
        }
        Log.e(TAG, this + ": getOriginalString is not supported.");
        return null;
    }

    ArrayList<Integer> getShapedGlyphIndices() {
        if (mStaticTextV2_0 != null) {
            return mStaticTextV2_0.shapedGlyphIndices;
        }
        if (mStaticTextV1_1 != null) {
            return mStaticTextV1_1.shapedGlyphIndices;
        }
        Log.e(TAG, this + ": getShapedGlyphIndices is not supported.");
        return null;
    }


    void setShapedGlyphIndices(int index, int newId) {
        if (mStaticTextV2_0 != null || mStaticTextV1_1 != null) {
            getShapedGlyphIndices().set(index, newId);
        } else {
            Log.e(TAG, this + ": setShapedGlyphIndices is not supported.");
        }
    }

    ArrayList<Float> getShapedGlyphPositions() {
        if (mStaticTextV2_0 != null) {
            return mStaticTextV2_0.shapedGlyphPositions;
        }
        if (mStaticTextV1_1 != null) {
            return mStaticTextV1_1.shapedGlyphPositions;
        }
        Log.e(TAG, this + ": getShapedGlyphPositions is not supported.");
        return null;
    }

    int getFontId() {
        if (mStaticTextV2_0 != null) {
            return mStaticTextV2_0.fontParam.ttfFont;
        }
        if (mStaticTextV1_1 != null) {
            return mStaticTextV1_1.textParam.ttfFont;
        }
        Log.e(TAG, this + ": getFontId is not supported.");
        return -1;
    }

    float getFontSize() {
        if (mStaticTextV2_0 != null) {
            return mStaticTextV2_0.fontParam.ttfFontSize;
        }
        if (mStaticTextV1_1 != null) {
            return mStaticTextV1_1.textParam.ttfFontSize;
        }
        Log.e(TAG, this + ": getTextParamFontSize is not supported.");
        return -1;
    }

    BindingPtr getVisibility() {
        if (mStaticTextV2_0 != null) {
            return mStaticTextV2_0.visible;
        }
        Log.e(TAG, this + ": getVisibility is not supported.");
        return null;
    }

    BindingPtr getColor() {
        if (mStaticTextV2_0 != null) {
            return mStaticTextV2_0.color;
        }
        Log.e(TAG, this + ": getColor is not supported.");
        return null;
    }

    @Override
    public String toString() {
        String dump = "invalid adapter";
        if (mStaticTextV2_0 != null) {
            dump = String.format("id=%d fontId=%d value=%s", mStaticTextV2_0.id,
                    mStaticTextV2_0.fontParam.ttfFont, mStaticTextV2_0.originalString);
        } else if (mStaticTextV1_1 != null) {
            dump = String.format("id=%d fontId=%d value=%s", mStaticTextV1_1.id,
                    mStaticTextV1_1.textParam.ttfFont, mStaticTextV1_1.value);
        }
        return "[" + getClass().getSimpleName() + " " + dump + "]";
    }
}
