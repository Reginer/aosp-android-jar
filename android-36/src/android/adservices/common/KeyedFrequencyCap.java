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

package android.adservices.common;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.util.Objects;

/**
 * A frequency cap for a specific ad counter key.
 *
 * <p>Frequency caps define the maximum rate an event can occur within a given time interval. If the
 * frequency cap is exceeded, the associated ad will be filtered out of ad selection.
 */
public final class KeyedFrequencyCap implements Parcelable {
    /** @hide */
    @VisibleForTesting public static final String AD_COUNTER_KEY_FIELD_NAME = "ad_counter_key";
    /** @hide */
    @VisibleForTesting public static final String MAX_COUNT_FIELD_NAME = "max_count";
    /** @hide */
    @VisibleForTesting public static final String INTERVAL_FIELD_NAME = "interval_in_seconds";

    /** @hide */
    public static final String MAX_COUNT_NOT_POSITIVE_ERROR_MESSAGE =
            "KeyedFrequencyCap max count %d must be strictly positive";
    /** @hide */
    public static final String INTERVAL_NULL_ERROR_MESSAGE =
            "KeyedFrequencyCap interval must not be null";
    /** @hide */
    public static final String INTERVAL_NOT_POSITIVE_FORMAT =
            "KeyedFrequencyCap interval %s must be strictly positive";
    /** @hide */
    public static final String MAX_INTERVAL_EXCEEDED_FORMAT =
            "KeyedFrequencyCap interval %s must be no greater than %s";
    /** @hide */
    public static final Duration MAX_INTERVAL = Duration.ofDays(100);

    // 4 bytes for the key, 12 bytes for the duration, and 4 for the maxCount
    private static final int SIZE_OF_FIXED_FIELDS = 20;

    private final int mAdCounterKey;
    private final int mMaxCount;
    @NonNull private final Duration mInterval;

    @NonNull
    public static final Creator<KeyedFrequencyCap> CREATOR =
            new Creator<KeyedFrequencyCap>() {
                @Override
                public KeyedFrequencyCap createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);
                    return new KeyedFrequencyCap(in);
                }

                @Override
                public KeyedFrequencyCap[] newArray(int size) {
                    return new KeyedFrequencyCap[size];
                }
            };

    private KeyedFrequencyCap(@NonNull Builder builder) {
        Objects.requireNonNull(builder);

        mAdCounterKey = builder.mAdCounterKey;
        mMaxCount = builder.mMaxCount;
        mInterval = builder.mInterval;
    }

    private KeyedFrequencyCap(@NonNull Parcel in) {
        Objects.requireNonNull(in);

        mAdCounterKey = in.readInt();
        mMaxCount = in.readInt();
        mInterval = Duration.ofSeconds(in.readLong());
    }

    /**
     * Returns the ad counter key that the frequency cap is applied to.
     *
     * <p>The ad counter key is defined by an adtech and is an arbitrary numeric identifier which
     * defines any criteria which may have previously been counted and persisted on the device. If
     * the on-device count exceeds the maximum count within a certain time interval, the frequency
     * cap has been exceeded.
     */
    @NonNull
    public int getAdCounterKey() {
        return mAdCounterKey;
    }

    /**
     * Returns the maximum count of event occurrences allowed within a given time interval.
     *
     * <p>If there are more events matching the ad counter key and ad event type counted on the
     * device within the time interval defined by {@link #getInterval()}, the frequency cap has been
     * exceeded, and the ad will not be eligible for ad selection.
     *
     * <p>For example, an ad that specifies a filter for a max count of two within one hour will not
     * be eligible for ad selection if the event has been counted two or more times within the hour
     * preceding the ad selection process.
     */
    public int getMaxCount() {
        return mMaxCount;
    }

    /**
     * Returns the interval, as a {@link Duration} which will be truncated to the nearest second,
     * over which the frequency cap is calculated.
     *
     * <p>When this frequency cap is computed, the number of persisted events is counted in the most
     * recent time interval. If the count of previously occurring matching events for an adtech is
     * greater than the number returned by {@link #getMaxCount()}, the frequency cap has been
     * exceeded, and the ad will not be eligible for ad selection.
     */
    @NonNull
    public Duration getInterval() {
        return mInterval;
    }

    /**
     * @return The estimated size of this object, in bytes.
     * @hide
     */
    public int getSizeInBytes() {
        return SIZE_OF_FIXED_FIELDS;
    }

    /**
     * A JSON serializer.
     *
     * @return A JSON serialization of this object.
     * @hide
     */
    public JSONObject toJson() throws JSONException {
        JSONObject toReturn = new JSONObject();
        toReturn.put(AD_COUNTER_KEY_FIELD_NAME, mAdCounterKey);
        toReturn.put(MAX_COUNT_FIELD_NAME, mMaxCount);
        toReturn.put(INTERVAL_FIELD_NAME, mInterval.getSeconds());
        return toReturn;
    }

    /**
     * A JSON de-serializer.
     *
     * @param json A JSON representation of an {@link KeyedFrequencyCap} object as would be
     *     generated by {@link #toJson()}.
     * @return An {@link KeyedFrequencyCap} object generated from the given JSON.
     * @hide
     */
    public static KeyedFrequencyCap fromJson(JSONObject json) throws JSONException {
        return new Builder(
                        json.getInt(AD_COUNTER_KEY_FIELD_NAME),
                        json.getInt(MAX_COUNT_FIELD_NAME),
                        Duration.ofSeconds(json.getLong(INTERVAL_FIELD_NAME)))
                .build();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);
        dest.writeInt(mAdCounterKey);
        dest.writeInt(mMaxCount);
        dest.writeLong(mInterval.getSeconds());
    }

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /** Checks whether the {@link KeyedFrequencyCap} objects contain the same information. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KeyedFrequencyCap)) return false;
        KeyedFrequencyCap that = (KeyedFrequencyCap) o;
        return mMaxCount == that.mMaxCount
                && mInterval.equals(that.mInterval)
                && mAdCounterKey == that.mAdCounterKey;
    }

    /** Returns the hash of the {@link KeyedFrequencyCap} object's data. */
    @Override
    public int hashCode() {
        return Objects.hash(mAdCounterKey, mMaxCount, mInterval);
    }

    @Override
    public String toString() {
        return "KeyedFrequencyCap{"
                + "mAdCounterKey="
                + mAdCounterKey
                + ", mMaxCount="
                + mMaxCount
                + ", mInterval="
                + mInterval
                + '}';
    }

    /** Builder for creating {@link KeyedFrequencyCap} objects. */
    public static final class Builder {
        private int mAdCounterKey;
        private int mMaxCount;
        @NonNull private Duration mInterval;

        public Builder(int adCounterKey, int maxCount, @NonNull Duration interval) {
            Preconditions.checkArgument(
                    maxCount > 0, MAX_COUNT_NOT_POSITIVE_ERROR_MESSAGE, maxCount);
            Objects.requireNonNull(interval, INTERVAL_NULL_ERROR_MESSAGE);
            Preconditions.checkArgument(
                    interval.getSeconds() > 0, INTERVAL_NOT_POSITIVE_FORMAT, interval);
            Preconditions.checkArgument(
                    interval.getSeconds() <= MAX_INTERVAL.getSeconds(),
                    MAX_INTERVAL_EXCEEDED_FORMAT,
                    interval,
                    MAX_INTERVAL);

            mAdCounterKey = adCounterKey;
            mMaxCount = maxCount;
            mInterval = interval;
        }

        /**
         * Sets the ad counter key the frequency cap applies to.
         *
         * <p>See {@link #getAdCounterKey()} for more information.
         */
        @NonNull
        public Builder setAdCounterKey(int adCounterKey) {
            mAdCounterKey = adCounterKey;
            return this;
        }

        /**
         * Sets the maximum count within the time interval for the frequency cap.
         *
         * <p>See {@link #getMaxCount()} for more information.
         */
        @NonNull
        public Builder setMaxCount(int maxCount) {
            Preconditions.checkArgument(
                    maxCount > 0, MAX_COUNT_NOT_POSITIVE_ERROR_MESSAGE, maxCount);
            mMaxCount = maxCount;
            return this;
        }

        /**
         * Sets the interval, as a {@link Duration} which will be truncated to the nearest second,
         * over which the frequency cap is calculated.
         *
         * <p>See {@link #getInterval()} for more information.
         */
        @NonNull
        public Builder setInterval(@NonNull Duration interval) {
            Objects.requireNonNull(interval, INTERVAL_NULL_ERROR_MESSAGE);
            Preconditions.checkArgument(
                    interval.getSeconds() > 0, INTERVAL_NOT_POSITIVE_FORMAT, interval);
            Preconditions.checkArgument(
                    interval.getSeconds() <= MAX_INTERVAL.getSeconds(),
                    MAX_INTERVAL_EXCEEDED_FORMAT,
                    interval,
                    MAX_INTERVAL);
            mInterval = interval;
            return this;
        }

        /** Builds and returns a {@link KeyedFrequencyCap} instance. */
        @NonNull
        public KeyedFrequencyCap build() {
            return new KeyedFrequencyCap(this);
        }
    }
}
