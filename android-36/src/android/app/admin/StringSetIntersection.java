/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.app.admin;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.TestApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Set;

/**
 * Class to identify a intersection resolution mechanism for {@code Set<String>} policies, it's
 * used to resolve the enforced policy when being set by multiple admins (see {@link
 * PolicyState#getResolutionMechanism()}).
 *
 * @hide
 */
public final class StringSetIntersection extends ResolutionMechanism<Set<String>> {

    /**
     * Intersection resolution for policies represented {@code Set<String>} which resolves as the
     * intersection of all sets.
     */
    @NonNull
    public static final StringSetIntersection STRING_SET_INTERSECTION = new StringSetIntersection();

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "StringSetIntersection {}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {}

    @NonNull
    public static final Parcelable.Creator<StringSetIntersection> CREATOR =
            new Parcelable.Creator<StringSetIntersection>() {
                @Override
                public StringSetIntersection createFromParcel(Parcel source) {
                    return new StringSetIntersection();
                }

                @Override
                public StringSetIntersection[] newArray(int size) {
                    return new StringSetIntersection[size];
                }
            };
}
