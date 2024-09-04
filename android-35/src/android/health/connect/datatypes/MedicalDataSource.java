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

package android.health.connect.datatypes;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Captures the data source information of medical data. All {@link MedicalResource}s are associated
 * with a {@code MedicalDataSource}.
 *
 * @hide
 */
public final class MedicalDataSource implements Parcelable {
    @NonNull private final String mId;
    @NonNull private final String mPackageName;
    @NonNull private final String mFhirBaseUri;
    @NonNull private final String mDisplayName;

    @NonNull
    public static final Creator<MedicalDataSource> CREATOR =
            new Creator<MedicalDataSource>() {
                @Override
                public MedicalDataSource createFromParcel(Parcel in) {
                    return new MedicalDataSource(in);
                }

                @Override
                public MedicalDataSource[] newArray(int size) {
                    return new MedicalDataSource[size];
                }
            };

    /**
     * @param id The unique identifier of this data source, assigned by the Android Health Platform
     *     at insertion time.
     * @param packageName The package name of the contributing package. Auto-populated by the
     *     platform at source creation time.
     * @param fhirBaseUri The fhir base URI of this data source.
     * @param displayName The display name that describes this data source.
     */
    private MedicalDataSource(
            @NonNull String id,
            @NonNull String packageName,
            @NonNull String fhirBaseUri,
            @NonNull String displayName) {
        requireNonNull(id);
        requireNonNull(packageName);
        requireNonNull(fhirBaseUri);
        requireNonNull(displayName);

        mId = id;
        mPackageName = packageName;
        mFhirBaseUri = fhirBaseUri;
        mDisplayName = displayName;
    }

    private MedicalDataSource(Parcel in) {
        mId = in.readString();
        mPackageName = in.readString();
        mFhirBaseUri = in.readString();
        mDisplayName = in.readString();
    }

    /** Returns the identifier. */
    @NonNull
    public String getId() {
        return mId;
    }

    /** Returns the corresponding package name. */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /** Returns the display name. */
    @NonNull
    public String getDisplayName() {
        return mDisplayName;
    }

    /** Returns the FHIR base URI, where data written for this data source came from. */
    @NonNull
    public String getFhirBaseUri() {
        return mFhirBaseUri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mPackageName);
        dest.writeString(mFhirBaseUri);
        dest.writeString(mDisplayName);
    }

    /** Indicates whether some other object is "equal to" this one. */
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicalDataSource that)) return false;
        return getId().equals(that.getId())
                && getPackageName().equals(that.getPackageName())
                && getFhirBaseUri().equals(that.getFhirBaseUri())
                && getDisplayName().equals(that.getDisplayName());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return hash(getId(), getPackageName(), getFhirBaseUri(), getDisplayName());
    }

    /** Returns a string representation of this {@link MedicalDataSource}. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("{");
        sb.append("id=").append(getId());
        sb.append(",packageName=").append(getPackageName());
        sb.append(",fhirBaseUri=").append(getFhirBaseUri());
        sb.append(",displayName=").append(getDisplayName());
        sb.append("}");
        return sb.toString();
    }

    /** Builder class for {@link MedicalDataSource} */
    public static final class Builder {
        @NonNull private String mId = "";
        @NonNull private String mPackageName = "";
        @NonNull private String mFhirBaseUri = "";
        @NonNull private String mDisplayName = "";

        /**
         * @param id The unique identifier of this data source, assigned by the Android Health
         *     Platform at insertion time.
         * @param packageName The package name of the contributing package. Auto-populated by the
         *     platform at source creation time.
         * @param fhirBaseUri The fhir base URI of the data source.
         * @param displayName The display name that describes the data source.
         */
        public Builder(
                @NonNull String id,
                @NonNull String packageName,
                @NonNull String fhirBaseUri,
                @NonNull String displayName) {
            requireNonNull(id);
            requireNonNull(packageName);
            requireNonNull(fhirBaseUri);
            requireNonNull(displayName);

            mId = id;
            mPackageName = packageName;
            mFhirBaseUri = fhirBaseUri;
            mDisplayName = displayName;
        }

        public Builder(@NonNull Builder original) {
            requireNonNull(original);
            mId = original.mId;
            mPackageName = original.mPackageName;
            mFhirBaseUri = original.mFhirBaseUri;
            mDisplayName = original.mDisplayName;
        }

        public Builder(@NonNull MedicalDataSource original) {
            requireNonNull(original);
            mId = original.getId();
            mPackageName = original.getPackageName();
            mFhirBaseUri = original.getFhirBaseUri();
            mDisplayName = original.getDisplayName();
        }

        /**
         * Sets unique identifier of this data source, assigned by the Android Health Platform at
         * insertion time.
         */
        @NonNull
        public Builder setId(@NonNull String id) {
            requireNonNull(id);
            mId = id;
            return this;
        }

        /**
         * Sets the package name of the contributing package. Auto-populated by the platform at
         * source creation time.
         */
        @NonNull
        public Builder setPackageName(@NonNull String packageName) {
            requireNonNull(packageName);
            mPackageName = packageName;
            return this;
        }

        /** Sets the fhir base URI of this data source. */
        @NonNull
        public Builder setFhirBaseUri(@NonNull String fhirBaseUri) {
            requireNonNull(fhirBaseUri);
            mFhirBaseUri = fhirBaseUri;
            return this;
        }

        /** Sets the display name that describes this data source. */
        @NonNull
        public Builder setDisplayName(@NonNull String displayName) {
            requireNonNull(displayName);
            mDisplayName = displayName;
            return this;
        }

        /** Returns a new instance of {@link MedicalDataSource} with the specified parameters. */
        @NonNull
        public MedicalDataSource build() {
            return new MedicalDataSource(mId, mPackageName, mFhirBaseUri, mDisplayName);
        }
    }
}
