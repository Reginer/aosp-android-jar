/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.adservices.signals;

import android.annotation.NonNull;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * The input object wrapping the parameters for the updateSignals API.
 *
 * <p>Refer to {@link UpdateSignalsRequest} for more information about the parameters.
 *
 * @hide
 */
public final class UpdateSignalsInput implements Parcelable {
    @NonNull private final Uri mUpdateUri;
    @NonNull private final String mCallerPackageName;

    @NonNull
    public static final Creator<UpdateSignalsInput> CREATOR =
            new Creator<>() {
                @NonNull
                @Override
                public UpdateSignalsInput createFromParcel(@NonNull Parcel in) {
                    return new UpdateSignalsInput(in);
                }

                @NonNull
                @Override
                public UpdateSignalsInput[] newArray(int size) {
                    return new UpdateSignalsInput[size];
                }
            };

    private UpdateSignalsInput(@NonNull Uri updateUri, @NonNull String callerPackageName) {
        Objects.requireNonNull(updateUri);
        Objects.requireNonNull(callerPackageName);

        mUpdateUri = updateUri;
        mCallerPackageName = callerPackageName;
    }

    private UpdateSignalsInput(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        Uri updateUri = Uri.CREATOR.createFromParcel(in);
        Objects.requireNonNull(updateUri);
        mUpdateUri = updateUri;
        String callerPackageName = in.readString();
        Objects.requireNonNull(callerPackageName);
        mCallerPackageName = callerPackageName;
    }

    /**
     * @return the {@link Uri} from which the signal updates will be fetched.
     */
    @NonNull
    public Uri getUpdateUri() {
        return mUpdateUri;
    }

    /**
     * @return the caller app's package name.
     */
    @NonNull
    public String getCallerPackageName() {
        return mCallerPackageName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);

        mUpdateUri.writeToParcel(dest, flags);
        dest.writeString(mCallerPackageName);
    }

    /**
     * @return {@code true} if and only if the other object is {@link UpdateSignalsRequest} with the
     *     same update URI and package name
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UpdateSignalsInput)) return false;
        UpdateSignalsInput that = (UpdateSignalsInput) o;
        return mUpdateUri.equals(that.mUpdateUri)
                && mCallerPackageName.equals(that.mCallerPackageName);
    }

    /**
     * @return the hash of the {@link UpdateSignalsInput} object's data.
     */
    @Override
    public int hashCode() {
        return Objects.hash(mUpdateUri, mCallerPackageName);
    }

    @Override
    public String toString() {
        return "UpdateSignalsInput{"
                + "mUpdateUri="
                + mUpdateUri
                + ", mCallerPackageName='"
                + mCallerPackageName
                + '\''
                + '}';
    }

    /**
     * Builder for {@link UpdateSignalsInput} objects.
     *
     * @hide
     */
    public static final class Builder {
        @NonNull private Uri mUpdateUri;
        @NonNull private String mCallerPackageName;

        /**
         * Instantiates a {@link UpdateSignalsInput.Builder} with the {@link Uri} from which the
         * JSON is to be fetched and the caller app's package name.
         *
         * @throws NullPointerException if any non-null parameter is null.
         */
        public Builder(@NonNull Uri updateUri, @NonNull String callerPackageName) {
            Objects.requireNonNull(updateUri);
            Objects.requireNonNull(callerPackageName);

            this.mUpdateUri = updateUri;
            this.mCallerPackageName = callerPackageName;
        }

        /**
         * Sets the {@link Uri} from which the signal updates will be fetched.
         *
         * <p>See {@link #getUpdateUri()} ()} for details.
         */
        @NonNull
        public Builder setUpdateUri(@NonNull Uri updateUri) {
            Objects.requireNonNull(updateUri);
            this.mUpdateUri = updateUri;
            return this;
        }

        /**
         * Sets the caller app's package name.
         *
         * <p>See {@link #getCallerPackageName()} for details.
         */
        @NonNull
        public Builder setCallerPackageName(@NonNull String callerPackageName) {
            Objects.requireNonNull(callerPackageName);
            this.mCallerPackageName = callerPackageName;
            return this;
        }

        /**
         * Builds an instance of a {@link UpdateSignalsInput}.
         *
         * @throws NullPointerException if any non-null parameter is null.
         */
        @NonNull
        public UpdateSignalsInput build() {
            return new UpdateSignalsInput(mUpdateUri, mCallerPackageName);
        }
    }
}
