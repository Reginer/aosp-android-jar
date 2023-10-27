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

package android.health.connect.migration;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds miscellaneous user-set preferences.
 *
 * @hide
 */
@SystemApi
public final class MetadataMigrationPayload extends MigrationPayload implements Parcelable {

    @NonNull
    public static final Creator<MetadataMigrationPayload> CREATOR =
            new Creator<>() {
                @Override
                public MetadataMigrationPayload createFromParcel(Parcel in) {
                    in.readInt(); // Skip the type
                    return new MetadataMigrationPayload(in);
                }

                @Override
                public MetadataMigrationPayload[] newArray(int size) {
                    return new MetadataMigrationPayload[size];
                }
            };

    private final int mRecordRetentionPeriodDays;

    private MetadataMigrationPayload(@IntRange(from = 0, to = 7300) int recordRetentionPeriodDays) {
        mRecordRetentionPeriodDays = recordRetentionPeriodDays;
    }

    MetadataMigrationPayload(@NonNull Parcel in) {
        mRecordRetentionPeriodDays = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(TYPE_METADATA);

        dest.writeInt(mRecordRetentionPeriodDays);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** Returns record retention period in days. */
    public int getRecordRetentionPeriodDays() {
        return mRecordRetentionPeriodDays;
    }

    /** Builder for {@link MetadataMigrationPayload}. */
    public static final class Builder {
        private static final int MIN_RRP = 0;
        private static final int MAX_RRP = 7300;

        private int mRecordRetentionPeriodDays = 0;

        /** Sets the value for {@link MetadataMigrationPayload#getRecordRetentionPeriodDays()}. */
        @NonNull
        public Builder setRecordRetentionPeriodDays(
                @IntRange(from = MIN_RRP, to = MAX_RRP) int recordRetentionPeriodDays) {
            if ((recordRetentionPeriodDays < MIN_RRP) || (recordRetentionPeriodDays > MAX_RRP)) {
                throw new IllegalArgumentException(
                        "recordRetentionPeriodDays is not within the range, was: "
                                + recordRetentionPeriodDays);
            }

            mRecordRetentionPeriodDays = recordRetentionPeriodDays;
            return this;
        }

        /**
         * Creates a new instance of {@link MetadataMigrationPayload} with the specified arguments.
         */
        @NonNull
        public MetadataMigrationPayload build() {
            return new MetadataMigrationPayload(mRecordRetentionPeriodDays);
        }
    }
}
