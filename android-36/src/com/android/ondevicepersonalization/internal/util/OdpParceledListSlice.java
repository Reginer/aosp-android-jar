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
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.List;

/**
 * Transfer a large list of parcelable objects across an IPC.  Splits into
 * multiple transactions if needed.
 *
 * @param <T> Parcelable type for the List
 * @hide
 */
public final class OdpParceledListSlice<T extends Parcelable> extends BaseOdpParceledListSlice<T> {
    @NonNull
    @SuppressWarnings("unchecked")
    public static final Parcelable.ClassLoaderCreator<OdpParceledListSlice> CREATOR =
            new Parcelable.ClassLoaderCreator<OdpParceledListSlice>() {
                public OdpParceledListSlice createFromParcel(Parcel in) {
                    return new OdpParceledListSlice(in, null);
                }

                @Override
                public OdpParceledListSlice createFromParcel(Parcel in, ClassLoader loader) {
                    return new OdpParceledListSlice(in, loader);
                }

                @Override
                public OdpParceledListSlice[] newArray(int size) {
                    return new OdpParceledListSlice[size];
                }
            };

    public OdpParceledListSlice(@NonNull List<T> list) {
        super(list);
    }

    private OdpParceledListSlice(Parcel in, ClassLoader loader) {
        super(in, loader);
    }

    /**
     * Returns an empty OdpParceledListSlice.
     */
    @NonNull
    public static <T extends Parcelable> OdpParceledListSlice<T> emptyList() {
        return new OdpParceledListSlice<T>(Collections.<T>emptyList());
    }

    @Override
    public int describeContents() {
        int contents = 0;
        final List<T> list = getList();
        for (int i = 0; i < list.size(); i++) {
            contents |= list.get(i).describeContents();
        }
        return contents;
    }

    @Override
    protected void writeElement(T parcelable, Parcel dest, int callFlags) {
        parcelable.writeToParcel(dest, callFlags);
    }

    @Override
    protected void writeParcelableCreator(T parcelable, Parcel dest) {
        dest.writeParcelableCreator((Parcelable) parcelable);
    }

    @Override
    protected Parcelable.Creator<?> readParcelableCreator(Parcel from, ClassLoader loader) {
        return from.readParcelableCreator(loader);
    }
}
