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

package android.adservices.topics;

import static android.adservices.common.AdServicesStatusUtils.STATUS_SUCCESS;

import android.adservices.common.AdServicesResponse;
import android.adservices.common.AdServicesStatusUtils;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represent the result from the getTopics API.
 *
 * @hide
 */
public final class GetTopicsResult extends AdServicesResponse {
    private final List<Long> mTaxonomyVersions;
    private final List<Long> mModelVersions;
    private final List<Integer> mTopics;

    private GetTopicsResult(
            @AdServicesStatusUtils.StatusCode int resultCode,
            @Nullable String errorMessage,
            @NonNull List<Long> taxonomyVersions,
            @NonNull List<Long> modelVersions,
            @NonNull List<Integer> topics) {
        super(resultCode, errorMessage);
        mTaxonomyVersions = taxonomyVersions;
        mModelVersions = modelVersions;
        mTopics = topics;
    }

    private GetTopicsResult(@NonNull Parcel in) {
        super(in.readInt(), in.readString());

        mTaxonomyVersions = Collections.unmodifiableList(readLongList(in));
        mModelVersions = Collections.unmodifiableList(readLongList(in));
        mTopics = Collections.unmodifiableList(readIntegerList(in));
    }

    public static final @NonNull Creator<GetTopicsResult> CREATOR =
            new Parcelable.Creator<GetTopicsResult>() {
                @Override
                public GetTopicsResult createFromParcel(Parcel in) {
                    return new GetTopicsResult(in);
                }

                @Override
                public GetTopicsResult[] newArray(int size) {
                    return new GetTopicsResult[size];
                }
            };

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeInt(mStatusCode);
        out.writeString(mErrorMessage);
        writeLongList(out, mTaxonomyVersions);
        writeLongList(out, mModelVersions);
        writeIntegerList(out, mTopics);
    }

    /**
     * Returns {@code true} if {@link #getResultCode} equals {@link
     * AdServicesStatusUtils#STATUS_SUCCESS}.
     */
    public boolean isSuccess() {
        return getResultCode() == STATUS_SUCCESS;
    }

    /** Returns one of the {@code RESULT} constants defined in {@link GetTopicsResult}. */
    public @AdServicesStatusUtils.StatusCode int getResultCode() {
        return mStatusCode;
    }

    /**
     * Returns the error message associated with this result.
     *
     * <p>If {@link #isSuccess} is {@code true}, the error message is always {@code null}. The error
     * message may be {@code null} even if {@link #isSuccess} is {@code false}.
     */
    @Nullable
    public String getErrorMessage() {
        return mErrorMessage;
    }

    /** Get the Taxonomy Versions. */
    public List<Long> getTaxonomyVersions() {
        return mTaxonomyVersions;
    }

    /** Get the Model Versions. */
    public List<Long> getModelVersions() {
        return mModelVersions;
    }

    @NonNull
    public List<Integer> getTopics() {
        return mTopics;
    }

    @Override
    public String toString() {
        return "GetTopicsResult{"
                + "mResultCode="
                + mStatusCode
                + ", mErrorMessage='"
                + mErrorMessage
                + '\''
                + ", mTaxonomyVersions="
                + mTaxonomyVersions
                + ", mModelVersions="
                + mModelVersions
                + ", mTopics="
                + mTopics
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof GetTopicsResult)) {
            return false;
        }

        GetTopicsResult that = (GetTopicsResult) o;

        return mStatusCode == that.mStatusCode
                && Objects.equals(mErrorMessage, that.mErrorMessage)
                && mTaxonomyVersions.equals(that.mTaxonomyVersions)
                && mModelVersions.equals(that.mModelVersions)
                && mTopics.equals(that.mTopics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStatusCode, mErrorMessage, mTaxonomyVersions, mModelVersions, mTopics);
    }

    // Read the list of long from parcel.
    private static List<Long> readLongList(@NonNull Parcel in) {
        List<Long> list = new ArrayList<>();

        int toReadCount = in.readInt();
        // Negative toReadCount is handled implicitly
        for (int i = 0; i < toReadCount; i++) {
            list.add(in.readLong());
        }

        return list;
    }

    // Read the list of integer from parcel.
    private static List<Integer> readIntegerList(@NonNull Parcel in) {
        List<Integer> list = new ArrayList<>();

        int toReadCount = in.readInt();
        // Negative toReadCount is handled implicitly
        for (int i = 0; i < toReadCount; i++) {
            list.add(in.readInt());
        }

        return list;
    }

    // Write a List of Long to parcel.
    private static void writeLongList(@NonNull Parcel out, @Nullable List<Long> val) {
        if (val == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(val.size());
        for (Long l : val) {
            out.writeLong(l);
        }
    }

    // Write a List of Integer to parcel.
    private static void writeIntegerList(@NonNull Parcel out, @Nullable List<Integer> val) {
        if (val == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(val.size());
        for (Integer integer : val) {
            out.writeInt(integer);
        }
    }

    /**
     * Builder for {@link GetTopicsResult} objects.
     *
     * @hide
     */
    public static final class Builder {
        private @AdServicesStatusUtils.StatusCode int mResultCode;
        @Nullable private String mErrorMessage;
        private List<Long> mTaxonomyVersions = new ArrayList<>();
        private List<Long> mModelVersions = new ArrayList<>();
        private List<Integer> mTopics = new ArrayList<>();

        public Builder() {}

        /** Set the Result Code. */
        public @NonNull Builder setResultCode(@AdServicesStatusUtils.StatusCode int resultCode) {
            mResultCode = resultCode;
            return this;
        }

        /** Set the Error Message. */
        public @NonNull Builder setErrorMessage(@Nullable String errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        /** Set the Taxonomy Version. */
        public @NonNull Builder setTaxonomyVersions(@NonNull List<Long> taxonomyVersions) {
            mTaxonomyVersions = taxonomyVersions;
            return this;
        }

        /** Set the Model Version. */
        public @NonNull Builder setModelVersions(@NonNull List<Long> modelVersions) {
            mModelVersions = modelVersions;
            return this;
        }

        /** Set the list of the returned Topics */
        public @NonNull Builder setTopics(@NonNull List<Integer> topics) {
            mTopics = topics;
            return this;
        }

        /**
         * Builds a {@link GetTopicsResult} instance.
         *
         * <p>throws IllegalArgumentException if any of the params are null or there is any mismatch
         * in the size of ModelVersions and TaxonomyVersions.
         */
        public @NonNull GetTopicsResult build() {
            if (mTopics == null || mTaxonomyVersions == null || mModelVersions == null) {
                throw new IllegalArgumentException(
                        "Topics or TaxonomyVersion or ModelVersion is null");
            }

            if (mTopics.size() != mTaxonomyVersions.size()
                    || mTopics.size() != mModelVersions.size()) {
                throw new IllegalArgumentException("Size mismatch in Topics");
            }

            return new GetTopicsResult(
                    mResultCode, mErrorMessage, mTaxonomyVersions, mModelVersions, mTopics);
        }
    }
}
