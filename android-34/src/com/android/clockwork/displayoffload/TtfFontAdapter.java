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

import java.util.ArrayList;

/**
 * TtfFontAdapter is a wrapper class for different versions of hal type dynamic text.
 *
 * The purpose of this class is to hide the version information from other components. So, those
 * components can be reused along the Hal's evolution.
 *
 * The usage of the highest available version is always preferred.
 */
public class TtfFontAdapter {

    private final vendor.google_clockwork.displayoffload.V1_0.TtfFontResource mTtfFontV1_0;
    private final vendor.google_clockwork.displayoffload.V2_0.TtfFontResource mTtfFontV2_0;

    TtfFontAdapter(vendor.google_clockwork.displayoffload.V1_0.TtfFontResource font) {
        this.mTtfFontV1_0 = font;
        this.mTtfFontV2_0 = null;
    }

    TtfFontAdapter(vendor.google_clockwork.displayoffload.V2_0.TtfFontResource font) {
        this.mTtfFontV1_0 = null;
        this.mTtfFontV2_0 = font;
    }

    vendor.google_clockwork.displayoffload.V1_0.TtfFontResource getTtfFontResourceV1()
            throws DisplayOffloadException {
        if (mTtfFontV1_0 != null) {
            return mTtfFontV1_0;
        }
        throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                "Failed to get TtfFontResource V1.0 from TtfFontAdapter.");
    }

    vendor.google_clockwork.displayoffload.V2_0.TtfFontResource getTtfFontResourceV2()
            throws DisplayOffloadException {
        if (mTtfFontV2_0 != null) {
            return mTtfFontV2_0;
        }
        throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                "Failed to get TtfFontResource V2.0 from TtfFontAdapter.");
    }

    int getId() throws DisplayOffloadException {
        if (mTtfFontV2_0 != null) {
            return mTtfFontV2_0.id;
        }
        if (mTtfFontV1_0 != null) {
            return mTtfFontV1_0.id;
        }
        throw new DisplayOffloadException(ERROR_LAYOUT_CONVERSION_FAILURE,
                this + ": Failed to get id from TtfFontAdapter.");
    }

    void setTtf(ArrayList<Byte> ttf) {
        if (mTtfFontV2_0 != null) {
            mTtfFontV2_0.ttf = ttf;
        } else if (mTtfFontV1_0 != null) {
            mTtfFontV1_0.ttf = ttf;
        }
    }

    @Override
    public String toString() {
        String dump = "invalid adapter";
        if (mTtfFontV2_0 != null) {
            dump = String.format("id=%d size(byte)=%d", mTtfFontV2_0.id, mTtfFontV2_0.ttf.size());
        } else if (mTtfFontV1_0 != null) {
            dump = String.format("id=%d size(byte)=%d", mTtfFontV1_0.id, mTtfFontV1_0.ttf.size());
        }
        return "[" + getClass().getSimpleName() + " " + dump + "]";
    }
}
