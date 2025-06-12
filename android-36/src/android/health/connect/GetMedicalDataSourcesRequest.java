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
import android.health.connect.datatypes.MedicalDataSource;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A create request for {@link HealthConnectManager#getMedicalDataSources}.
 *
 * <p>If no {@link GetMedicalDataSourcesRequest#getPackageNames() package names} are set, requests
 * all {@link MedicalDataSource}s from all packages. Otherwise the request is limited to the
 * requested package names.
 */
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public final class GetMedicalDataSourcesRequest implements Parcelable {
    @NonNull private final Set<String> mPackageNames;

    // A full Java-language-style package name for the Android app can contain uppercase
    // or lowercase letters, numbers, and underscores ('_'). It must have at least two segments (one
    // or more dots), and individual package name parts can only start with letters. See the
    // <a
    // href="https://developer.android.com/guide/topics/manifest/manifest-element.html#package">Android developer doc</a>.
    private static final String PACKAGE_NAME_REGEX =
            "^([A-Za-z][a-zA-Z0-9_]*\\.)+[A-Za-z][a-zA-Z0-9_]*$";

    @NonNull
    public static final Creator<GetMedicalDataSourcesRequest> CREATOR =
            new Creator<>() {
                @Override
                public GetMedicalDataSourcesRequest createFromParcel(Parcel in) {
                    return new GetMedicalDataSourcesRequest(in);
                }

                @Override
                public GetMedicalDataSourcesRequest[] newArray(int size) {
                    return new GetMedicalDataSourcesRequest[size];
                }
            };

    /**
     * Creates a new instance of {@link GetMedicalDataSourcesRequest}. Please see {@link
     * GetMedicalDataSourcesRequest.Builder} for more detailed parameters information.
     */
    private GetMedicalDataSourcesRequest(@NonNull Set<String> packageNames) {
        Objects.requireNonNull(packageNames);
        validatePackageNames(packageNames);
        mPackageNames = packageNames;
    }

    /**
     * Constructs this object with the data present in {@code parcel}. It should be in the same
     * order as {@link #writeToParcel}.
     */
    private GetMedicalDataSourcesRequest(@NonNull Parcel in) {
        Objects.requireNonNull(in);
        mPackageNames = new ArraySet<>(requireNonNull(in.createStringArrayList()));
        validatePackageNames(mPackageNames);
    }

    /**
     * Returns the package names for which {@link MedicalDataSource}s are being requested, or an
     * empty set for no filter.
     */
    @NonNull
    public Set<String> getPackageNames() {
        return new ArraySet<>(mPackageNames);
    }

    /**
     * Validates all of the provided {@code packageNames} are valid, which matches with the {@link
     * #PACKAGE_NAME_REGEX}.
     *
     * @throws IllegalArgumentException with all invalid package names if not all {@code
     *     packageNames} are valid.
     */
    private static void validatePackageNames(Set<String> packageNames) {
        Pattern pattern = Pattern.compile(PACKAGE_NAME_REGEX);

        Set<String> invalidPackageNames = new HashSet<>();
        for (String packageName : packageNames) {
            Matcher matcher = pattern.matcher(packageName);
            if (!matcher.matches()) {
                invalidPackageNames.add(packageName);
            }
        }
        if (!invalidPackageNames.isEmpty()) {
            throw new IllegalArgumentException("Invalid package name(s): " + invalidPackageNames);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStringList(new ArrayList<>(mPackageNames));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetMedicalDataSourcesRequest that)) return false;
        return mPackageNames.equals(that.mPackageNames);
    }

    @Override
    public int hashCode() {
        return hash(mPackageNames);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("{");
        sb.append("packageNames=").append(mPackageNames);
        sb.append("}");
        return sb.toString();
    }

    /** Builder class for {@link GetMedicalDataSourcesRequest}. */
    public static final class Builder {
        private final Set<String> mPackageNames = new ArraySet<>();

        /** Constructs a new {@link GetMedicalDataSourcesRequest.Builder} with no filters set. */
        public Builder() {}

        /** Constructs a clone of the other {@link GetMedicalDataSourcesRequest.Builder}. */
        public Builder(@NonNull Builder other) {
            requireNonNull(other);
            mPackageNames.addAll(other.mPackageNames);
        }

        /** Constructs a clone of the other {@link GetMedicalDataSourcesRequest} instance. */
        public Builder(@NonNull GetMedicalDataSourcesRequest other) {
            requireNonNull(other);
            mPackageNames.addAll(other.getPackageNames());
        }

        /**
         * Adds a package name to limit this request to.
         *
         * <p>If the list of package names is empty, {@link MedicalDataSource}s for all packages
         * will be requested. Otherwise only those for the added package names are requested.
         *
         * @throws IllegalArgumentException if the provided {@code packageName} is not valid.
         */
        @NonNull
        public Builder addPackageName(@NonNull String packageName) {
            Objects.requireNonNull(packageName);
            validatePackageNames(Set.of(packageName));
            mPackageNames.add(packageName);
            return this;
        }

        /** Clears all package names. */
        @NonNull
        public Builder clearPackageNames() {
            mPackageNames.clear();
            return this;
        }

        /**
         * Returns a new instance of {@link GetMedicalDataSourcesRequest} with the specified
         * parameters.
         */
        @NonNull
        public GetMedicalDataSourcesRequest build() {
            return new GetMedicalDataSourcesRequest(mPackageNames);
        }
    }
}
