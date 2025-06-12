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

package android.health.connect;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.datatypes.FhirVersion;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A create request for {@link HealthConnectManager#createMedicalDataSource}.
 *
 * <p>Medical data is represented using the <a href="https://hl7.org/fhir/">Fast Healthcare
 * Interoperability Resources (FHIR)</a> standard.
 */
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public final class CreateMedicalDataSourceRequest implements Parcelable {
    // The character limit for the {@code mDisplayName}
    private static final int DISPLAY_NAME_CHARACTER_LIMIT = 90;
    // The character limit for the {@code mFhirBaseUri}
    private static final int FHIR_BASE_URI_CHARACTER_LIMIT = 2000;

    @NonNull private final Uri mFhirBaseUri;
    @NonNull private final String mDisplayName;
    @NonNull private final FhirVersion mFhirVersion;
    private long mDataSize;

    @NonNull
    public static final Creator<CreateMedicalDataSourceRequest> CREATOR =
            new Creator<CreateMedicalDataSourceRequest>() {
                @NonNull
                @Override
                /*
                 * @throws IllegalArgumentException if the {@code mFhirBaseUri} or
                 * {@code mDisplayName} exceed the character limits.
                 */
                public CreateMedicalDataSourceRequest createFromParcel(@NonNull Parcel in) {
                    return new CreateMedicalDataSourceRequest(in);
                }

                @NonNull
                @Override
                public CreateMedicalDataSourceRequest[] newArray(int size) {
                    return new CreateMedicalDataSourceRequest[size];
                }
            };

    /**
     * Creates a new instance of {@link CreateMedicalDataSourceRequest}. Please see {@link
     * CreateMedicalDataSourceRequest.Builder} for more detailed parameters information.
     */
    private CreateMedicalDataSourceRequest(
            @NonNull Uri fhirBaseUri,
            @NonNull String displayName,
            @NonNull FhirVersion fhirVersion) {
        requireNonNull(fhirBaseUri);
        requireNonNull(displayName);
        requireNonNull(fhirVersion);
        validateFhirBaseUriCharacterLimit(fhirBaseUri);
        validateDisplayNameCharacterLimit(displayName);
        validateFhirVersion(fhirVersion);

        mFhirBaseUri = fhirBaseUri;
        mDisplayName = displayName;
        mFhirVersion = fhirVersion;
    }

    private CreateMedicalDataSourceRequest(@NonNull Parcel in) {
        requireNonNull(in);
        mDataSize = in.dataSize();

        mFhirBaseUri = requireNonNull(in.readParcelable(Uri.class.getClassLoader(), Uri.class));
        mDisplayName = requireNonNull(in.readString());
        mFhirVersion =
                requireNonNull(
                        in.readParcelable(FhirVersion.class.getClassLoader(), FhirVersion.class));

        validateFhirBaseUriCharacterLimit(mFhirBaseUri);
        validateDisplayNameCharacterLimit(mDisplayName);
        validateFhirVersion(mFhirVersion);
    }

    /**
     * Returns the FHIR base URI. For data coming from a FHIR server this is <a
     * href="https://hl7.org/fhir/R4/http.html#root">the base URL</a>.
     */
    @NonNull
    public Uri getFhirBaseUri() {
        return mFhirBaseUri;
    }

    /** Returns the display name. For the request to succeed this must be unique per app. */
    @NonNull
    public String getDisplayName() {
        return mDisplayName;
    }

    /**
     * Returns the FHIR version. For the request to succeeds this must be a version supported by
     * Health Connect, as documented on the {@link FhirVersion}.
     */
    @NonNull
    public FhirVersion getFhirVersion() {
        return mFhirVersion;
    }

    /**
     * Returns the size of the parcel when the class was created from Parcel.
     *
     * @hide
     */
    public long getDataSize() {
        return mDataSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mFhirBaseUri, 0);
        dest.writeString(mDisplayName);
        dest.writeParcelable(mFhirVersion, 0);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof CreateMedicalDataSourceRequest that)) return false;
        return getFhirBaseUri().equals(that.getFhirBaseUri())
                && getDisplayName().equals(that.getDisplayName())
                && getFhirVersion().equals(that.getFhirVersion());
    }

    @Override
    public int hashCode() {
        return hash(getFhirBaseUri(), getDisplayName(), getFhirVersion());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("{");
        sb.append("fhirBaseUri=").append(getFhirBaseUri());
        sb.append(",displayName=").append(getDisplayName());
        sb.append(",fhirVersion=").append(getFhirVersion());
        sb.append("}");
        return sb.toString();
    }

    /** Builder class for {@link CreateMedicalDataSourceRequest}. */
    public static final class Builder {
        @NonNull private Uri mFhirBaseUri;
        @NonNull private String mDisplayName;
        @NonNull private FhirVersion mFhirVersion;

        /**
         * Constructs a new {@link CreateMedicalDataSourceRequest.Builder} instance.
         *
         * @param fhirBaseUri The FHIR base URI of the data source. For data coming from a FHIR
         *     server this should be the <a href="https://hl7.org/fhir/R4/http.html#root">FHIR base
         *     URL</a> (e.g. `https://example.com/fhir`). If the data is generated by an app without
         *     a FHIR URL, this can be populated by a unique and understandable URI defined by the
         *     app (e.g. `myapp://..`) that points to the source of the data. We recommend not to
         *     use a domain name that you don't control. If your app supports <a
         *     href="https://developer.android.com/training/app-links/deep-linking">app deep
         *     linking</a>, this URI would ideally link to the source data. The maximum length for
         *     the URI is 2000 characters.
         * @param displayName The display name that describes the data source. The maximum length
         *     for the display name is 90 characters. This must be unique per app.
         * @param fhirVersion The FHIR version of the medical data that will be linked to this data
         *     source. This has to be a version supported by Health Connect, as documented on the
         *     {@link FhirVersion}.
         */
        public Builder(
                @NonNull Uri fhirBaseUri,
                @NonNull String displayName,
                @NonNull FhirVersion fhirVersion) {
            requireNonNull(fhirBaseUri);
            requireNonNull(displayName);
            requireNonNull(fhirVersion);

            mFhirBaseUri = fhirBaseUri;
            mDisplayName = displayName;
            mFhirVersion = fhirVersion;
        }

        /** Constructs a clone of the other {@link CreateMedicalDataSourceRequest.Builder}. */
        public Builder(@NonNull Builder other) {
            requireNonNull(other);
            mFhirBaseUri = other.mFhirBaseUri;
            mDisplayName = other.mDisplayName;
            mFhirVersion = other.mFhirVersion;
        }

        /** Constructs a clone of the other {@link CreateMedicalDataSourceRequest} instance. */
        public Builder(@NonNull CreateMedicalDataSourceRequest other) {
            requireNonNull(other);
            mFhirBaseUri = other.getFhirBaseUri();
            mDisplayName = other.getDisplayName();
            mFhirVersion = other.getFhirVersion();
        }

        /**
         * Sets the FHIR base URI. For data coming from a FHIR server this should be the <a
         * href="https://hl7.org/fhir/R4/http.html#root">FHIR base URL</a> (e.g.
         * `https://example.com/fhir`).
         *
         * <p>If the data is generated by an app without a FHIR base URL, this can be populated by a
         * URI defined by the app (e.g. `myapp://..`) that should:
         *
         * <ul>
         *   <li>Be a unique and understandable URI.
         *   <li>Point to the source of the data. We recommend not to use a domain name that you
         *       don't control.
         *   <li>Ideally allow linking to the data source if your app supports <a
         *       href="https://developer.android.com/training/app-links/deep-linking">app deep
         *       linking</a> to the data.
         * </ul>
         *
         * <p>The URI may not exceed 2000 characters.
         */
        @NonNull
        public Builder setFhirBaseUri(@NonNull Uri fhirBaseUri) {
            requireNonNull(fhirBaseUri);
            mFhirBaseUri = fhirBaseUri;
            return this;
        }

        /**
         * Sets the display name. For the request to succeed this must be unique per app.
         *
         * <p>The display name may not exceed 90 characters.
         */
        @NonNull
        public Builder setDisplayName(@NonNull String displayName) {
            requireNonNull(displayName);
            mDisplayName = displayName;
            return this;
        }

        /**
         * Sets the FHIR version of data from this data source.
         *
         * <p>This has to be a version supported by Health Connect, as documented on the {@link
         * FhirVersion}.
         */
        @NonNull
        public Builder setFhirVersion(@NonNull FhirVersion fhirVersion) {
            requireNonNull(fhirVersion);
            mFhirVersion = fhirVersion;
            return this;
        }

        /**
         * Returns a new instance of {@link CreateMedicalDataSourceRequest} with the specified
         * parameters.
         *
         * @throws IllegalArgumentException if the {@code mFhirBaseUri} or {@code mDisplayName}
         *     exceed the character limits or if the {@code mFhirVersion} is not supported by Health
         *     Connect.
         */
        @NonNull
        public CreateMedicalDataSourceRequest build() {
            return new CreateMedicalDataSourceRequest(mFhirBaseUri, mDisplayName, mFhirVersion);
        }
    }

    private static void validateDisplayNameCharacterLimit(String displayName) {
        if (displayName.isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be empty.");
        }
        if (displayName.length() > DISPLAY_NAME_CHARACTER_LIMIT) {
            throw new IllegalArgumentException(
                    "Display name cannot be longer than "
                            + DISPLAY_NAME_CHARACTER_LIMIT
                            + " characters.");
        }
    }

    private static void validateFhirBaseUriCharacterLimit(Uri fhirBaseUri) {
        String fhirBaseUriString = fhirBaseUri.toString();
        if (fhirBaseUriString.isEmpty()) {
            throw new IllegalArgumentException("FHIR base URI cannot be empty.");
        }
        if (fhirBaseUriString.length() > FHIR_BASE_URI_CHARACTER_LIMIT) {
            throw new IllegalArgumentException(
                    "FHIR base URI cannot be longer than "
                            + FHIR_BASE_URI_CHARACTER_LIMIT
                            + " characters.");
        }
    }

    private static void validateFhirVersion(FhirVersion fhirVersion) {
        if (!fhirVersion.isSupportedFhirVersion()) {
            throw new IllegalArgumentException("Unsupported FHIR version " + fhirVersion + ".");
        }
    }
}
