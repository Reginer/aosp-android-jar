/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Transfer a large byte[] object across an IPC.  Splits into
 * multiple transactions if needed.
 *
 * @hide
 */
public final class ByteArrayParceledSlice implements Parcelable {
    private static final int MAX_SLICE_SIZE = IBinder.getSuggestedMaxIpcSizeBytes() - 32;

    @NonNull private final ByteArrayParceledListSlice mContents;

    public ByteArrayParceledSlice(@Nullable byte[] input) {
        if (input != null) {
            final int numSlices = (input.length + MAX_SLICE_SIZE - 1) / MAX_SLICE_SIZE;
            ArrayList<byte[]> slices = new ArrayList<>(numSlices);
            for (int i = 0; i < numSlices; ++i) {
                int startOffset = i * MAX_SLICE_SIZE;
                int count = Math.min(MAX_SLICE_SIZE, input.length - startOffset);
                byte[] slice = new byte[count];
                System.arraycopy(input, startOffset, slice, 0, count);
                slices.add(slice);
            }
            mContents = new ByteArrayParceledListSlice(slices);
        } else {
            mContents = null;
        }
    }

    /** Returns the byte array. */
    @Nullable public byte[] getByteArray() {
        if (mContents == null) {
            return null;
        }
        List<byte[]> slices = mContents.getList();
        if (slices == null) {
            return null;
        }
        int totalCount = 0;
        for (int i = 0; i < slices.size(); ++i) {
            totalCount += slices.get(i).length;
        }
        byte[] result = new byte[totalCount];
        int offset = 0;
        for (int i = 0; i < slices.size(); ++i) {
            System.arraycopy(slices.get(i), 0, result, offset, slices.get(i).length);
            offset += slices.get(i).length;
        }
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(mContents, flags);
    }

    public static final Parcelable.Creator<ByteArrayParceledSlice> CREATOR =
            new Parcelable.Creator<ByteArrayParceledSlice>() {
                public ByteArrayParceledSlice createFromParcel(Parcel in) {
                    return new ByteArrayParceledSlice(in);
                }

                public ByteArrayParceledSlice[] newArray(int size) {
                    return new ByteArrayParceledSlice[size];
                }
            };

    private ByteArrayParceledSlice(Parcel in) {
        mContents = in.readParcelable(null, ByteArrayParceledListSlice.class);
    }
}
