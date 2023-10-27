/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;

import java.util.Collections;
import java.util.List;

/**
 * Transfer a large list of byte[] objects across an IPC.  Splits into
 * multiple transactions if needed.
 *
 * @hide
 */
public final class ByteArrayParceledListSlice extends BaseOdpParceledListSlice<byte[]> {
    @SuppressWarnings("unchecked")
    @NonNull
    public static final Creator<byte[]> BYTE_ARRAY_CREATOR = new Creator<byte[]>() {
                public byte[] createFromParcel(Parcel in) {
                    byte[] arr = new byte[in.readInt()];
                    in.readByteArray(arr);
                    return arr;
                }

                public byte[][] newArray(int size) {
                    return new byte[size][];
                }
            };

    @SuppressWarnings("unchecked")
    @Nullable
    public static final ClassLoaderCreator<ByteArrayParceledListSlice> CREATOR =
            new ClassLoaderCreator<ByteArrayParceledListSlice>() {
                public ByteArrayParceledListSlice createFromParcel(Parcel in) {
                    return new ByteArrayParceledListSlice(in, null);
                }

                @Override
                public ByteArrayParceledListSlice createFromParcel(Parcel in, ClassLoader loader) {
                    return new ByteArrayParceledListSlice(in, loader);
                }

                @Override
                public ByteArrayParceledListSlice[] newArray(int size) {
                    return new ByteArrayParceledListSlice[size];
                }
            };

    public ByteArrayParceledListSlice(@Nullable List<byte[]> list) {
        super(list);
    }

    private ByteArrayParceledListSlice(@Nullable Parcel in, @Nullable ClassLoader loader) {
        super(in, loader);
    }

    /**
     * Returns an empty list
     */
    @Nullable
    public static ByteArrayParceledListSlice emptyList() {
        return new ByteArrayParceledListSlice(Collections.emptyList());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeElement(@Nullable byte[] parcelable, @Nullable Parcel reply, int callFlags) {
        reply.writeInt(parcelable.length);
        reply.writeByteArray(parcelable);
    }

    @Override
    @NonNull
    public List<byte[]> getList() {
        return super.getList();
    }

    @Override
    public void writeParcelableCreator(@Nullable byte[] parcelable, @Nullable Parcel dest) {
        return;
    }

    @Override
    @Nullable
    public Creator<?> readParcelableCreator(@Nullable Parcel from, @Nullable ClassLoader loader) {
        return BYTE_ARRAY_CREATOR;
    }
}
