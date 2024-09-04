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

package android.net.wifi.twt;

import static android.net.wifi.MloLink.INVALID_MLO_LINK_ID;
import static android.net.wifi.MloLink.MAX_MLO_LINK_ID;
import static android.net.wifi.MloLink.MIN_MLO_LINK_ID;

import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.net.wifi.MloLink;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.wifi.flags.Flags;

/**
 * Defines target wake time (TWT) request class.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
public final class TwtRequest implements Parcelable {
    private final int mMinWakeDurationMicros;
    private final int mMaxWakeDurationMicros;
    private final long mMinWakeIntervalMicros;
    private final long mMaxWakeIntervalMicros;
    private final int mLinkId;

    private TwtRequest(TwtRequest.Builder builder) {
        mMinWakeDurationMicros = builder.mMinWakeDurationMicros;
        mMaxWakeDurationMicros = builder.mMaxWakeDurationMicros;
        mMinWakeIntervalMicros = builder.mMinWakeIntervalMicros;
        mMaxWakeIntervalMicros = builder.mMaxWakeIntervalMicros;
        mLinkId = builder.mLinkId;
    }

    /**
     * Get minimum TWT wake duration in microseconds.
     *
     * @return Minimum wake duration in microseconds
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int getMinWakeDurationMicros() {
        return mMinWakeDurationMicros;
    }

    /**
     * Get maximum TWT wake duration in microseconds.
     *
     * @return Maximum wake duration in microseconds
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int getMaxWakeDurationMicros() {
        return mMaxWakeDurationMicros;
    }

    /**
     * Get minimum TWT wake interval in microseconds.
     *
     * @return Minimum wake interval in microseconds
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public long getMinWakeIntervalMicros() {
        return mMinWakeIntervalMicros;
    }

    /**
     * Get maximum TWT wake interval in microseconds.
     *
     * @return Maximum wake interval in microseconds
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public long getMaxWakeIntervalMicros() {
        return mMaxWakeIntervalMicros;
    }


    /**
     * Get link id (valid only in case of Multi-link operation).
     *
     * @return MLO link id in the range {@link MloLink#MIN_MLO_LINK_ID} to
     * {@link MloLink#MAX_MLO_LINK_ID}. Returns {@link MloLink#INVALID_MLO_LINK_ID} if not set.
     */
    @IntRange(from = INVALID_MLO_LINK_ID, to = MAX_MLO_LINK_ID)
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int getLinkId() {
        return mLinkId;
    }

    @NonNull
    public static final Creator<TwtRequest> CREATOR = new Creator<TwtRequest>() {
        @Override
        public TwtRequest createFromParcel(Parcel in) {
            Builder builder = new TwtRequest.Builder(in.readInt(), in.readInt(), in.readLong(),
                    in.readLong());
            int mloLinkId = in.readInt();
            if (mloLinkId >= MIN_MLO_LINK_ID && mloLinkId <= MAX_MLO_LINK_ID) {
                builder.setLinkId(mloLinkId);
            }
            return builder.build();
        }

        @Override
        public TwtRequest[] newArray(int size) {
            return new TwtRequest[size];
        }
    };


    /**
     * Describe the kinds of special objects contained in this Parcelable
     * instance's marshaled representation.
     *
     * @return a bitmask indicating the set of special object types marshaled
     * by this Parcelable object instance.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     * May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mMinWakeDurationMicros);
        dest.writeInt(mMaxWakeDurationMicros);
        dest.writeLong(mMinWakeIntervalMicros);
        dest.writeLong(mMinWakeIntervalMicros);
        dest.writeInt(mLinkId);
    }

    /**
     * Builder class used to construct {@link TwtRequest} objects.
     *
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final class Builder {
        private final int mMinWakeDurationMicros;
        private final int mMaxWakeDurationMicros;
        private final long mMinWakeIntervalMicros;
        private final long mMaxWakeIntervalMicros;
        private int mLinkId = INVALID_MLO_LINK_ID;

        /**
         * Set link id (valid only in case of Multi-link operation).
         *
         * @param linkId Link id, which should be in the range {@link MloLink#MIN_MLO_LINK_ID} to
         *               {@link MloLink#MAX_MLO_LINK_ID}
         * @return The builder to facilitate chaining
         * @throws IllegalArgumentException if argument is invalid
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public TwtRequest.Builder setLinkId(
                @IntRange(from = MIN_MLO_LINK_ID, to = MAX_MLO_LINK_ID) int linkId) {
            if (linkId < MIN_MLO_LINK_ID || linkId > MAX_MLO_LINK_ID) {
                throw new IllegalArgumentException("linkId is out of range");
            }
            mLinkId = linkId;
            return this;
        }

        /**
         * Constructor for {@link TwtRequest.Builder}.
         *
         * @param minWakeDurationMicros Minimum TWT wake duration in microseconds.
         * @param maxWakeDurationMicros Maximum TWT wake duration in microseconds
         * @param minWakeIntervalMicros Minimum TWT wake interval in microseconds
         * @param maxWakeIntervalMicros Maximum TWT wake interval in microseconds
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public Builder(int minWakeDurationMicros, int maxWakeDurationMicros,
                long minWakeIntervalMicros, long maxWakeIntervalMicros) {
            mMinWakeDurationMicros = minWakeDurationMicros;
            mMaxWakeDurationMicros = maxWakeDurationMicros;
            mMinWakeIntervalMicros = minWakeIntervalMicros;
            mMaxWakeIntervalMicros = maxWakeIntervalMicros;
        }

        /**
         * Build {@link TwtRequest} given the current configurations made on the builder.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public TwtRequest build() {
            return new TwtRequest(this);
        }
    }
}
