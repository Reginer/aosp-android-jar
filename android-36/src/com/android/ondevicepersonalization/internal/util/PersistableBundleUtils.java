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

package com.android.ondevicepersonalization.internal.util;

import android.os.PersistableBundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** @hide */
public class PersistableBundleUtils {
    /** Serialize a PersistableBundle to a String. */
    public static byte[] toByteArray(PersistableBundle input) throws IOException {
        if (input == null) {
            return null;
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        input.writeToStream(stream);
        return stream.toByteArray();
    }

    /** Deserialize a String to a PersistableBundle. */
    public static PersistableBundle fromByteArray(byte[] input) throws IOException {
        if (input == null) {
            return null;
        }
        ByteArrayInputStream stream = new ByteArrayInputStream(input);
        return PersistableBundle.readFromStream(stream);
    }

    private PersistableBundleUtils() {}
}
