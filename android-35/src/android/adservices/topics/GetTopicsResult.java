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
import java.util.Arrays;
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
    private final List<byte[]> mEncryptedTopics;
    private final List<String> mEncryptionKeys;
    private final List<byte[]> mEncapsulatedKeys;

    private GetTopicsResult(
            @AdServicesStatusUtils.StatusCode int resultCode,
            String errorMessage,
            List<Long> taxonomyVersions,
            List<Long> modelVersions,
            List<Integer> topics,
            List<byte[]> encryptedTopics,
            List<String> encryptionKeys,
            List<byte[]> encapsulatedKeys) {
        super(resultCode, errorMessage);
        mTaxonomyVersions = taxonomyVersions;
        mModelVersions = modelVersions;
        mTopics = topics;
        mEncryptedTopics = encryptedTopics;
        mEncryptionKeys = encryptionKeys;
        mEncapsulatedKeys = encapsulatedKeys;
    }

    private GetTopicsResult(@NonNull Parcel in) {
        super(in.readInt(), in.readString());

        mTaxonomyVersions = Collections.unmodifiableList(readLongList(in));
        mModelVersions = Collections.unmodifiableList(readLongList(in));
        mTopics = Collections.unmodifiableList(readIntegerList(in));
        mEncryptedTopics = Collections.unmodifiableList(readByteArrayList(in));
        mEncryptionKeys = Collections.unmodifiableList(readStringList(in));
        mEncapsulatedKeys = Collections.unmodifiableList(readByteArrayList(in));
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
        writeByteArrayList(out, mEncryptedTopics);
        writeStringList(out, mEncryptionKeys);
        writeByteArrayList(out, mEncapsulatedKeys);
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

    @NonNull
    public List<byte[]> getEncryptedTopics() {
        return mEncryptedTopics;
    }

    @NonNull
    public List<String> getEncryptionKeys() {
        return mEncryptionKeys;
    }

    @NonNull
    public List<byte[]> getEncapsulatedKeys() {
        return mEncapsulatedKeys;
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
                + ", mEncryptedTopics="
                + prettyPrint(mEncryptedTopics)
                + ", mEncryptionKeys="
                + mEncryptionKeys
                + ", mEncapsulatedKeys="
                + prettyPrint(mEncapsulatedKeys)
                + '}';
    }

    private String prettyPrint(List<byte[]> listOfByteArrays) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        for (int index = 0; index < listOfByteArrays.size(); index++) {
            stringBuilder.append(Arrays.toString(listOfByteArrays.get(index)));
            if (index != listOfByteArrays.size() - 1) {
                stringBuilder.append(", ");
            }
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
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
                && mTopics.equals(that.mTopics)
                && equals(mEncryptedTopics, that.mEncryptedTopics)
                && mEncryptionKeys.equals(that.mEncryptionKeys);
    }

    private static boolean equals(List<byte[]> list1, List<byte[]> list2) {
        if (list1 == null || list2 == null) {
            return false;
        }
        if (list1.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list1.size(); i++) {
            if (!Arrays.equals(list1.get(i), list2.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mStatusCode,
                mErrorMessage,
                mTaxonomyVersions,
                mModelVersions,
                mTopics,
                hashCode(mEncryptedTopics),
                mEncryptionKeys,
                hashCode(mEncryptedTopics));
    }

    private static int hashCode(List<byte[]> list) {
        int hash = 0;
        for (byte[] bytes : list) {
            hash += Arrays.hashCode(bytes);
        }
        return hash;
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

    // Read the list of integer from parcel.
    private static List<String> readStringList(@NonNull Parcel in) {
        List<String> list = new ArrayList<>();

        int toReadCount = in.readInt();
        // Negative toReadCount is handled implicitly
        for (int i = 0; i < toReadCount; i++) {
            list.add(in.readString());
        }

        return list;
    }

    // Read the list of byte arrays from parcel.
    private static List<byte[]> readByteArrayList(@NonNull Parcel in) {
        List<byte[]> list = new ArrayList<>();

        int toReadCount = in.readInt();
        // Negative toReadCount is handled implicitly
        for (int i = 0; i < toReadCount; i++) {
            list.add(in.createByteArray());
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

    // Write a List of String to parcel.
    private static void writeStringList(@NonNull Parcel out, @Nullable List<String> val) {
        if (val == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(val.size());
        for (String string : val) {
            out.writeString(string);
        }
    }

    // Write a List of byte array to parcel.
    private static void writeByteArrayList(@NonNull Parcel out, @Nullable List<byte[]> val) {
        if (val == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(val.size());
        for (byte[] bytes : val) {
            out.writeByteArray(bytes);
        }
    }

    /**
     * Builder for {@link GetTopicsResult} objects.
     *
     * @hide
     */
    public static final class Builder {
        private @AdServicesStatusUtils.StatusCode int mResultCode;
        private String mErrorMessage;
        private List<Long> mTaxonomyVersions = new ArrayList<>();
        private List<Long> mModelVersions = new ArrayList<>();
        private List<Integer> mTopics = new ArrayList<>();
        private List<byte[]> mEncryptedTopics = new ArrayList<>();
        private List<String> mEncryptionKeys = new ArrayList<>();
        private List<byte[]> mEncapsulatedKeys = new ArrayList<>();

        public Builder() {}

        /** Set the Result Code. */
        @NonNull
        public Builder setResultCode(@AdServicesStatusUtils.StatusCode int resultCode) {
            mResultCode = resultCode;
            return this;
        }

        /** Set the Error Message. */
        @NonNull
        public Builder setErrorMessage(@Nullable String errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        /** Set the Taxonomy Version. */
        @NonNull
        public Builder setTaxonomyVersions(@NonNull List<Long> taxonomyVersions) {
            mTaxonomyVersions = taxonomyVersions;
            return this;
        }

        /** Set the Model Version. */
        @NonNull
        public Builder setModelVersions(@NonNull List<Long> modelVersions) {
            mModelVersions = modelVersions;
            return this;
        }

        /** Set the list of the returned Topics */
        @NonNull
        public Builder setTopics(@NonNull List<Integer> topics) {
            mTopics = topics;
            return this;
        }

        /** Set the list of the returned encrypted topics */
        @NonNull
        public Builder setEncryptedTopics(@NonNull List<byte[]> encryptedTopics) {
            mEncryptedTopics = encryptedTopics;
            return this;
        }

        /** Set the list of the encryption keys */
        @NonNull
        public Builder setEncryptionKeys(@NonNull List<String> encryptionKeys) {
            mEncryptionKeys = encryptionKeys;
            return this;
        }

        /** Set the list of encapsulated keys generated via encryption */
        @NonNull
        public Builder setEncapsulatedKeys(@NonNull List<byte[]> encapsulatedKeys) {
            mEncapsulatedKeys = encapsulatedKeys;
            return this;
        }

        /**
         * Builds a {@link GetTopicsResult} instance.
         *
         * <p>throws IllegalArgumentException if any of the params are null or there is any mismatch
         * in the size of lists.
         */
        @NonNull
        public GetTopicsResult build() {
            if (mTopics == null
                    || mTaxonomyVersions == null
                    || mModelVersions == null
                    || mEncryptedTopics == null
                    || mEncryptionKeys == null) {
                throw new IllegalArgumentException(
                        "One of the mandatory params of GetTopicsResult is null");
            }

            if (mTopics.size() != mTaxonomyVersions.size()
                    || mTopics.size() != mModelVersions.size()) {
                throw new IllegalArgumentException("Size mismatch in Topics");
            }

            if (mEncryptedTopics.size() != mEncryptionKeys.size()
                    || mEncryptedTopics.size() != mEncapsulatedKeys.size()) {
                throw new IllegalArgumentException("Size mismatch in EncryptedTopic lists");
            }

            return new GetTopicsResult(
                    mResultCode,
                    mErrorMessage,
                    mTaxonomyVersions,
                    mModelVersions,
                    mTopics,
                    mEncryptedTopics,
                    mEncryptionKeys,
                    mEncapsulatedKeys);
        }
    }
}
