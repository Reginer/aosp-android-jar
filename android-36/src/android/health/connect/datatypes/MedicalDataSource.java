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

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Captures the data source information of medical data. All {@link MedicalResource}s are associated
 * with a {@code MedicalDataSource}.
 *
 * <p>The medical data is represented using the <a href="https://hl7.org/fhir/">Fast Healthcare
 * Interoperability Resources (FHIR)</a> standard.
 */
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public final class MedicalDataSource implements Parcelable {
    @NonNull private final String mId;
    @NonNull private final String mPackageName;
    @NonNull private final Uri mFhirBaseUri;
    @NonNull private final String mDisplayName;
    @NonNull private final FhirVersion mFhirVersion;
    @Nullable private final Instant mLastDataUpdateTime;

    @NonNull
    public static final Creator<MedicalDataSource> CREATOR =
            new Creator<MedicalDataSource>() {
                @NonNull
                @Override
                public MedicalDataSource createFromParcel(@NonNull Parcel in) {
                    return new MedicalDataSource(in);
                }

                @NonNull
                @Override
                public MedicalDataSource[] newArray(int size) {
                    return new MedicalDataSource[size];
                }
            };

    /**
     * Creates a new instance of {@link MedicalDataSource}. Please see {@link
     * MedicalDataSource.Builder} for more detailed parameters information.
     */
    private MedicalDataSource(
            @NonNull String id,
            @NonNull String packageName,
            @NonNull Uri fhirBaseUri,
            @NonNull String displayName,
            @NonNull FhirVersion fhirVersion,
            @Nullable Instant lastDataUpdateTime) {
        requireNonNull(id);
        requireNonNull(packageName);
        requireNonNull(fhirBaseUri);
        requireNonNull(displayName);
        requireNonNull(fhirVersion);

        mId = id;
        mPackageName = packageName;
        mFhirBaseUri = fhirBaseUri;
        mDisplayName = displayName;
        mFhirVersion = fhirVersion;
        mLastDataUpdateTime = lastDataUpdateTime;
    }

    private MedicalDataSource(@NonNull Parcel in) {
        requireNonNull(in);
        mId = requireNonNull(in.readString());
        mPackageName = requireNonNull(in.readString());
        mFhirBaseUri = requireNonNull(in.readParcelable(Uri.class.getClassLoader(), Uri.class));
        mDisplayName = requireNonNull(in.readString());
        mFhirVersion =
                requireNonNull(
                        in.readParcelable(FhirVersion.class.getClassLoader(), FhirVersion.class));
        long lastDataUpdateTimeMillis = in.readLong();
        mLastDataUpdateTime =
                lastDataUpdateTimeMillis == 0
                        ? null
                        : Instant.ofEpochMilli(lastDataUpdateTimeMillis);
    }

    /** Returns the unique identifier, assigned by the Android Health Platform at insertion time. */
    @NonNull
    public String getId() {
        return mId;
    }

    /** Returns the corresponding package name of the owning app. */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /** Returns the display name. */
    @NonNull
    public String getDisplayName() {
        return mDisplayName;
    }

    /** Returns the FHIR version of {@link MedicalResource}s from this source. */
    @NonNull
    public FhirVersion getFhirVersion() {
        return mFhirVersion;
    }

    /**
     * Returns the time {@link MedicalResource}s linked to this data source were last updated, or
     * {@code null} if the data source has no linked resources.
     *
     * <p>This time is based on resources that currently exist in HealthConnect, so does not reflect
     * data deletion.
     */
    @Nullable
    public Instant getLastDataUpdateTime() {
        return mLastDataUpdateTime;
    }

    /** Returns the FHIR base URI, where data written for this data source came from. */
    @NonNull
    public Uri getFhirBaseUri() {
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
        dest.writeParcelable(mFhirBaseUri, 0);
        dest.writeString(mDisplayName);
        dest.writeParcelable(mFhirVersion, 0);
        dest.writeLong(mLastDataUpdateTime == null ? 0 : mLastDataUpdateTime.toEpochMilli());
    }

    /**
     * Validates all of the provided {@code ids} are valid.
     *
     * <p>Throws {@link IllegalArgumentException} with all invalid IDs if not.
     *
     * @hide
     */
    public static Set<UUID> validateMedicalDataSourceIds(@NonNull Set<String> ids) {
        Set<String> invalidIds = new HashSet<>();
        Set<UUID> uuids = new HashSet<>();
        for (String id : ids) {
            try {
                uuids.add(UUID.fromString(id));
            } catch (IllegalArgumentException e) {
                invalidIds.add(id);
            }
        }
        if (!invalidIds.isEmpty()) {
            throw new IllegalArgumentException("Invalid data source ID(s): " + invalidIds);
        }
        return uuids;
    }

    /** Indicates whether some other object is "equal to" this one. */
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicalDataSource that)) return false;
        return getId().equals(that.getId())
                && getPackageName().equals(that.getPackageName())
                && getFhirBaseUri().equals(that.getFhirBaseUri())
                && getDisplayName().equals(that.getDisplayName())
                && getFhirVersion().equals(that.getFhirVersion())
                && Objects.equals(getLastDataUpdateTime(), that.getLastDataUpdateTime());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return hash(
                getId(),
                getPackageName(),
                getFhirBaseUri(),
                getDisplayName(),
                getFhirVersion(),
                getLastDataUpdateTime());
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
        sb.append(",fhirVersion=").append(getFhirVersion());
        sb.append(",lastDataUpdateTime=").append(getLastDataUpdateTime());
        sb.append("}");
        return sb.toString();
    }

    /** Builder class for {@link MedicalDataSource}. */
    public static final class Builder {
        @NonNull private String mId;
        @NonNull private String mPackageName;
        @NonNull private Uri mFhirBaseUri;
        @NonNull private String mDisplayName;
        @NonNull private FhirVersion mFhirVersion;
        @Nullable private Instant mLastDataUpdateTime;

        /**
         * Constructs a new {@link MedicalDataSource.Builder} instance.
         *
         * @param id The unique identifier of this data source.
         * @param packageName The package name of the owning app.
         * @param fhirBaseUri The FHIR base URI of the data source.
         * @param displayName The display name that describes the data source.
         * @param fhirVersion The FHIR version of {@link MedicalResource}s linked to this source.
         */
        public Builder(
                @NonNull String id,
                @NonNull String packageName,
                @NonNull Uri fhirBaseUri,
                @NonNull String displayName,
                @NonNull FhirVersion fhirVersion) {
            requireNonNull(id);
            requireNonNull(packageName);
            requireNonNull(fhirBaseUri);
            requireNonNull(displayName);
            requireNonNull(fhirVersion);

            mId = id;
            mPackageName = packageName;
            mFhirBaseUri = fhirBaseUri;
            mDisplayName = displayName;
            mFhirVersion = fhirVersion;
        }

        /** Constructs a clone of the other {@link MedicalDataSource.Builder}. */
        public Builder(@NonNull Builder other) {
            requireNonNull(other);
            mId = other.mId;
            mPackageName = other.mPackageName;
            mFhirBaseUri = other.mFhirBaseUri;
            mDisplayName = other.mDisplayName;
            mFhirVersion = other.mFhirVersion;
            mLastDataUpdateTime = other.mLastDataUpdateTime;
        }

        /** Constructs a clone of the other {@link MedicalDataSource} instance. */
        public Builder(@NonNull MedicalDataSource other) {
            requireNonNull(other);
            mId = other.getId();
            mPackageName = other.getPackageName();
            mFhirBaseUri = other.getFhirBaseUri();
            mDisplayName = other.getDisplayName();
            mFhirVersion = other.getFhirVersion();
            mLastDataUpdateTime = other.getLastDataUpdateTime();
        }

        /** Sets unique identifier of this data source. */
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

        /** Sets the FHIR base URI of this data source. */
        @NonNull
        public Builder setFhirBaseUri(@NonNull Uri fhirBaseUri) {
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

        /** Sets the FHIR version of {@link MedicalResource}s linked to this source. */
        @NonNull
        public Builder setFhirVersion(@NonNull FhirVersion fhirVersion) {
            requireNonNull(fhirVersion);
            mFhirVersion = fhirVersion;
            return this;
        }

        /** Sets the time {@link MedicalResource}s linked to this data source were last updated. */
        @NonNull
        public Builder setLastDataUpdateTime(@Nullable Instant lastDataUpdateTime) {
            mLastDataUpdateTime = lastDataUpdateTime;
            return this;
        }

        /** Returns a new instance of {@link MedicalDataSource} with the specified parameters. */
        @NonNull
        public MedicalDataSource build() {
            return new MedicalDataSource(
                    mId,
                    mPackageName,
                    mFhirBaseUri,
                    mDisplayName,
                    mFhirVersion,
                    mLastDataUpdateTime);
        }
    }
}
