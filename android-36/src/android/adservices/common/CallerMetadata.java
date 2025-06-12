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

package android.adservices.common;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * A class to hold the metadata of an IPC call.
 *
 * @hide
 */
public class CallerMetadata implements Parcelable {
    private @NonNull long mBinderElapsedTimestamp;

    private CallerMetadata(@NonNull long binderElapsedTimestamp) {
        mBinderElapsedTimestamp = binderElapsedTimestamp;
    }

    private CallerMetadata(@NonNull Parcel in) {
        mBinderElapsedTimestamp = in.readLong();
    }

    @NonNull
    public static final Parcelable.Creator<CallerMetadata> CREATOR =
            new Parcelable.Creator<CallerMetadata>() {
                @Override
                public CallerMetadata createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new CallerMetadata(in);
                }

                @Override
                public CallerMetadata[] newArray(int size) {
                    return new CallerMetadata[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeLong(mBinderElapsedTimestamp);
    }

    /** Get the binder elapsed timestamp. */
    public long getBinderElapsedTimestamp() {
        return mBinderElapsedTimestamp;
    }

    /** Builder for {@link CallerMetadata} objects. */
    public static final class Builder {
        private long mBinderElapsedTimestamp;

        public Builder() {
        }

        /** Set the binder elapsed timestamp. */
        public @NonNull CallerMetadata.Builder setBinderElapsedTimestamp(
                @NonNull long binderElapsedTimestamp) {
            mBinderElapsedTimestamp = binderElapsedTimestamp;
            return this;
        }

        /** Builds a {@link CallerMetadata} instance. */
        public @NonNull CallerMetadata build() {
            return new CallerMetadata(mBinderElapsedTimestamp);
        }
    }
}
