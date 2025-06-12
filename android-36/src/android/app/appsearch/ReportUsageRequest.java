/*
 * Copyright 2021 The Android Open Source Project
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

package android.app.appsearch;

import android.annotation.CurrentTimeMillisLong;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.app.appsearch.annotation.CanIgnoreReturnValue;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.appsearch.flags.Flags;

import java.util.Objects;

/**
 * A request to report usage of a document.
 *
 * <p>See {@link AppSearchSession#reportUsage} for a detailed description of usage reporting.
 *
 * @see AppSearchSession#reportUsage
 */
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings({"HiddenSuperclass", "JSpecifyNullness"})
@SafeParcelable.Class(creator = "ReportUsageRequestCreator")
public final class ReportUsageRequest extends AbstractSafeParcelable {

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    public static final @NonNull Parcelable.Creator<ReportUsageRequest> CREATOR =
            new ReportUsageRequestCreator();

    @Field(id = 1, getter = "getNamespace")
    private final @NonNull String mNamespace;

    @Field(id = 2, getter = "getDocumentId")
    private final @NonNull String mDocumentId;

    @Field(id = 3, getter = "getUsageTimestampMillis")
    private final long mUsageTimestampMillis;

    @Constructor
    ReportUsageRequest(
            @Param(id = 1) @NonNull String namespace,
            @Param(id = 2) @NonNull String documentId,
            @Param(id = 3) long usageTimestampMillis) {
        mNamespace = Objects.requireNonNull(namespace);
        mDocumentId = Objects.requireNonNull(documentId);
        mUsageTimestampMillis = usageTimestampMillis;
    }

    /** Returns the namespace of the document that was used. */
    public @NonNull String getNamespace() {
        return mNamespace;
    }

    /** Returns the ID of document that was used. */
    public @NonNull String getDocumentId() {
        return mDocumentId;
    }

    /**
     * Returns the timestamp in milliseconds of the usage report (the time at which the document was
     * used).
     *
     * <p>The value is in the {@link System#currentTimeMillis} time base.
     */
    @CurrentTimeMillisLong
    public long getUsageTimestampMillis() {
        return mUsageTimestampMillis;
    }

    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        ReportUsageRequestCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link ReportUsageRequest} objects. */
    public static final class Builder {
        private final String mNamespace;
        private final String mDocumentId;
        private Long mUsageTimestampMillis;

        /**
         * Creates a new {@link ReportUsageRequest.Builder} instance.
         *
         * @param namespace The namespace of the document that was used (such as from {@link
         *     GenericDocument#getNamespace}.
         * @param documentId The ID of document that was used (such as from {@link
         *     GenericDocument#getId}.
         */
        public Builder(@NonNull String namespace, @NonNull String documentId) {
            mNamespace = Objects.requireNonNull(namespace);
            mDocumentId = Objects.requireNonNull(documentId);
        }

        /**
         * Sets the timestamp in milliseconds of the usage report (the time at which the document
         * was used).
         *
         * <p>The value is in the {@link System#currentTimeMillis} time base.
         *
         * <p>If unset, this defaults to the current timestamp at the time that the {@link
         * ReportUsageRequest} is constructed.
         */
        @CanIgnoreReturnValue
        public @NonNull ReportUsageRequest.Builder setUsageTimestampMillis(
                @CurrentTimeMillisLong long usageTimestampMillis) {
            mUsageTimestampMillis = usageTimestampMillis;
            return this;
        }

        /** Builds a new {@link ReportUsageRequest}. */
        public @NonNull ReportUsageRequest build() {
            if (mUsageTimestampMillis == null) {
                mUsageTimestampMillis = System.currentTimeMillis();
            }
            return new ReportUsageRequest(mNamespace, mDocumentId, mUsageTimestampMillis);
        }
    }
}
