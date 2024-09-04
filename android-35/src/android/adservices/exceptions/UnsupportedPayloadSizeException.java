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

package android.adservices.exceptions;

import android.annotation.Nullable;

/**
 * Exception used when the size of a payload is not supported.
 *
 * @hide
 */
public class UnsupportedPayloadSizeException extends IllegalStateException {
    private final int mPayloadSizeKb;

    /** Constructs a {@link UnsupportedPayloadSizeException} */
    public UnsupportedPayloadSizeException(int payloadSizeKb, @Nullable String message) {
        super(message);
        this.mPayloadSizeKb = payloadSizeKb;
    }

    /** Returns the size of the payload in Kb */
    public int getPayloadSizeKb() {
        return mPayloadSizeKb;
    }
}
