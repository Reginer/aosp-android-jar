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
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.internal.ParcelUtils;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A read response for {@link HealthConnectManager#readMedicalResources}. */
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public final class ReadMedicalResourcesResponse implements Parcelable {
    @NonNull private final List<MedicalResource> mMedicalResources;
    @Nullable private final String mNextPageToken;
    private final int mRemainingCount;

    /**
     * Constructs a new {@link ReadMedicalResourcesResponse} instance.
     *
     * @param medicalResources List of {@link MedicalResource}s.
     * @param nextPageToken The token value of the read result which can be used as input token for
     *     next read request. {@code null} if there are no more pages available.
     * @param remainingCount the total number of medical resources remaining, excluding the ones in
     *     this response
     */
    public ReadMedicalResourcesResponse(
            @NonNull List<MedicalResource> medicalResources,
            @Nullable String nextPageToken,
            int remainingCount) {
        requireNonNull(medicalResources);
        if (nextPageToken == null && remainingCount > 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Remaining count must be 0 to have a null next page token, but was %d",
                            remainingCount));
        }
        if (nextPageToken != null && remainingCount == 0) {
            throw new IllegalArgumentException("Next page token provided with no remaining data");
        }
        mMedicalResources = medicalResources;
        mNextPageToken = nextPageToken;
        mRemainingCount = remainingCount;
    }

    private ReadMedicalResourcesResponse(@NonNull Parcel in) {
        requireNonNull(in);
        in = ParcelUtils.getParcelForSharedMemoryIfRequired(in);
        mMedicalResources = new ArrayList<>();
        in.readParcelableList(
                mMedicalResources, MedicalResource.class.getClassLoader(), MedicalResource.class);
        mNextPageToken = in.readString();
        mRemainingCount = in.readInt();
    }

    @NonNull
    public static final Creator<ReadMedicalResourcesResponse> CREATOR =
            new Creator<>() {
                @Override
                public ReadMedicalResourcesResponse createFromParcel(Parcel in) {
                    return new ReadMedicalResourcesResponse(in);
                }

                @Override
                public ReadMedicalResourcesResponse[] newArray(int size) {
                    return new ReadMedicalResourcesResponse[size];
                }
            };

    /** Returns list of {@link MedicalResource}s. */
    @NonNull
    public List<MedicalResource> getMedicalResources() {
        return mMedicalResources;
    }

    /**
     * Returns a page token to read the next page of the result. {@code null} if there are no more
     * pages available.
     */
    @Nullable
    public String getNextPageToken() {
        return mNextPageToken;
    }

    /**
     * Returns the count of medical resources still remaining which were not returned due to
     * pagination.
     *
     * <p>For a response with a null next page token, this will be 0. This result is accurate at the
     * time the request was made, and with the permissions when the request was made. However, the
     * actual results may change if permissions change or resources are inserted or deleted.
     */
    public int getRemainingCount() {
        return mRemainingCount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        requireNonNull(dest);
        ParcelUtils.putToRequiredMemory(dest, flags, this::writeToParcelInternal);
    }

    private void writeToParcelInternal(@NonNull Parcel dest) {
        requireNonNull(dest);
        dest.writeParcelableList(mMedicalResources, 0);
        dest.writeString(mNextPageToken);
        dest.writeInt(mRemainingCount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReadMedicalResourcesResponse that)) return false;
        return getMedicalResources().equals(that.getMedicalResources())
                && Objects.equals(getNextPageToken(), that.getNextPageToken())
                && mRemainingCount == that.getRemainingCount();
    }

    @Override
    public int hashCode() {
        return hash(getMedicalResources(), getNextPageToken(), mRemainingCount);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("{");
        sb.append("medicalResources=").append(getMedicalResources());
        sb.append(",nextPageToken=").append(getNextPageToken());
        sb.append("}");
        return sb.toString();
    }
}
