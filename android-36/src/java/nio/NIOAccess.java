/*
 * Copyright (C) 2007 The Android Open Source Project
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

package java.nio;

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;

import android.annotation.SystemApi;

import android.compat.annotation.UnsupportedAppUsage;

/**
 * This class is used via JNI by code in frameworks/base/ and
 * by the JniConstants cache in libnativehelper/.
 * @hide
 */
// @VisibleForTesting : was default
@SystemApi(client = MODULE_LIBRARIES)
public final class NIOAccess {

    private NIOAccess() {}

    /**
     * Returns the underlying native pointer to the data of the given
     * Buffer starting at the Buffer's current position, or 0 if the
     * Buffer is not backed by native heap storage.
     * @hide
     */
    // @VisibleForTesting : was default
    @UnsupportedAppUsage
    public static long getBasePointer(Buffer b) {
        long address = b.address;
        if (address == 0L) {
            return 0L;
        }
        return address + (b.position << b._elementSizeShift);
    }

    /**
     * Returns the underlying Java array containing the data of the
     * given Buffer, or null if the Buffer is not backed by a Java array.
     *
     * @param b  {@code Buffer} to get its underlying data array
     * @return   underlying Java array
     *
     * @hide
     */
    @UnsupportedAppUsage
    @SystemApi(client = MODULE_LIBRARIES)
    public static Object getBaseArray(Buffer b) {
        return b.hasArray() ? b.array() : null;
    }

    /**
     * Returns the offset in bytes from the start of the underlying
     * Java array object containing the data of the given Buffer to
     * the actual start of the data. The start of the data takes into
     * account the Buffer's current position. This method is only
     * meaningful if {@link #getBaseArray(Buffer)} returns non-null.
     *
     * @param b {@code Buffer} to get its underlying data array's base offset
     * @return  underlying Java array's base offset
     *
     * @hide
     */
    @UnsupportedAppUsage
    @SystemApi(client = MODULE_LIBRARIES)
    public static int getBaseArrayOffset(Buffer b) {
        return b.hasArray() ? ((b.arrayOffset() + b.position) << b._elementSizeShift) : 0;
    }
}
