/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.media.metrics;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Media network event.
 */
public final class NetworkEvent extends Event implements Parcelable {
    /** Network type is not known. Default type. */
    public static final int NETWORK_TYPE_UNKNOWN = 0;
    /** Other network type */
    public static final int NETWORK_TYPE_OTHER = 1;
    /** Wi-Fi network */
    public static final int NETWORK_TYPE_WIFI = 2;
    /** Ethernet network */
    public static final int NETWORK_TYPE_ETHERNET = 3;
    /** 2G network */
    public static final int NETWORK_TYPE_2G = 4;
    /** 3G network */
    public static final int NETWORK_TYPE_3G = 5;
    /** 4G network */
    public static final int NETWORK_TYPE_4G = 6;
    /** 5G NSA network */
    public static final int NETWORK_TYPE_5G_NSA = 7;
    /** 5G SA network */
    public static final int NETWORK_TYPE_5G_SA = 8;
    /** Not network connected */
    public static final int NETWORK_TYPE_OFFLINE = 9;

    private final int mNetworkType;
    private final long mTimeSinceCreatedMillis;

    /** @hide */
    @IntDef(prefix = "NETWORK_TYPE_", value = {
        NETWORK_TYPE_UNKNOWN,
        NETWORK_TYPE_OTHER,
        NETWORK_TYPE_WIFI,
        NETWORK_TYPE_ETHERNET,
        NETWORK_TYPE_2G,
        NETWORK_TYPE_3G,
        NETWORK_TYPE_4G,
        NETWORK_TYPE_5G_NSA,
        NETWORK_TYPE_5G_SA,
        NETWORK_TYPE_OFFLINE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface NetworkType {}

    /**
     * Network type to string.
     * @hide
     */
    public static String networkTypeToString(@NetworkType int value) {
        switch (value) {
            case NETWORK_TYPE_UNKNOWN:
                return "NETWORK_TYPE_UNKNOWN";
            case NETWORK_TYPE_OTHER:
                return "NETWORK_TYPE_OTHER";
            case NETWORK_TYPE_WIFI:
                return "NETWORK_TYPE_WIFI";
            case NETWORK_TYPE_ETHERNET:
                return "NETWORK_TYPE_ETHERNET";
            case NETWORK_TYPE_2G:
                return "NETWORK_TYPE_2G";
            case NETWORK_TYPE_3G:
                return "NETWORK_TYPE_3G";
            case NETWORK_TYPE_4G:
                return "NETWORK_TYPE_4G";
            case NETWORK_TYPE_5G_NSA:
                return "NETWORK_TYPE_5G_NSA";
            case NETWORK_TYPE_5G_SA:
                return "NETWORK_TYPE_5G_SA";
            case NETWORK_TYPE_OFFLINE:
                return "NETWORK_TYPE_OFFLINE";
            default:
                return Integer.toHexString(value);
        }
    }

    /**
     * Creates a new NetworkEvent.
     */
    private NetworkEvent(@NetworkType int type, long timeSinceCreatedMillis,
            @NonNull Bundle extras) {
        this.mNetworkType = type;
        this.mTimeSinceCreatedMillis = timeSinceCreatedMillis;
        this.mMetricsBundle = extras == null ? null : extras.deepCopy();
    }

    /**
     * Gets network type.
     */
    @NetworkType
    public int getNetworkType() {
        return mNetworkType;
    }

    /**
     * Gets timestamp since the creation of the log session in milliseconds.
     * @return the timestamp since the creation in milliseconds, or -1 if unknown.
     * @see LogSessionId
     * @see PlaybackSession
     * @see RecordingSession
     */
    @Override
    @IntRange(from = -1)
    public long getTimeSinceCreatedMillis() {
        return mTimeSinceCreatedMillis;
    }

    /**
     * Gets metrics-related information that is not supported by dedicated methods.
     * <p>It is intended to be used for backwards compatibility by the metrics infrastructure.
     */
    @Override
    @NonNull
    public Bundle getMetricsBundle() {
        return mMetricsBundle;
    }

    @Override
    public String toString() {
        return "NetworkEvent { "
                + "networkType = " + mNetworkType + ", "
                + "timeSinceCreatedMillis = " + mTimeSinceCreatedMillis
                + " }";
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkEvent that = (NetworkEvent) o;
        return mNetworkType == that.mNetworkType
                && mTimeSinceCreatedMillis == that.mTimeSinceCreatedMillis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mNetworkType, mTimeSinceCreatedMillis);
    }

    @Override
    public void writeToParcel(@NonNull android.os.Parcel dest, int flags) {
        dest.writeInt(mNetworkType);
        dest.writeLong(mTimeSinceCreatedMillis);
        dest.writeBundle(mMetricsBundle);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private NetworkEvent(@NonNull android.os.Parcel in) {
        int type = in.readInt();
        long timeSinceCreatedMillis = in.readLong();
        Bundle extras = in.readBundle();

        this.mNetworkType = type;
        this.mTimeSinceCreatedMillis = timeSinceCreatedMillis;
        this.mMetricsBundle = extras;
    }

    /**
     * Used to read a NetworkEvent from a Parcel.
     */
    public static final @NonNull Parcelable.Creator<NetworkEvent> CREATOR =
            new Parcelable.Creator<NetworkEvent>() {
        @Override
        public NetworkEvent[] newArray(int size) {
            return new NetworkEvent[size];
        }

        @Override
        public NetworkEvent createFromParcel(@NonNull Parcel in) {
            return new NetworkEvent(in);
        }
    };

    /**
     * A builder for {@link NetworkEvent}
     */
    public static final class Builder {
        private int mNetworkType = NETWORK_TYPE_UNKNOWN;
        private long mTimeSinceCreatedMillis = -1;
        private Bundle mMetricsBundle = new Bundle();

        /**
         * Creates a new Builder.
         */
        public Builder() {
        }

        /**
         * Sets network type.
         */
        public @NonNull Builder setNetworkType(@NetworkType int value) {
            mNetworkType = value;
            return this;
        }

        /**
         * Sets timestamp since the creation in milliseconds.
         * @param value the timestamp since the creation in milliseconds.
         *              -1 indicates the value is unknown.
         * @see #getTimeSinceCreatedMillis()
         */
        public @NonNull Builder setTimeSinceCreatedMillis(@IntRange(from = -1) long value) {
            mTimeSinceCreatedMillis = value;
            return this;
        }

        /**
         * Sets metrics-related information that is not supported by dedicated
         * methods.
         * <p>It is intended to be used for backwards compatibility by the
         * metrics infrastructure.
         */
        public @NonNull Builder setMetricsBundle(@NonNull Bundle metricsBundle) {
            mMetricsBundle = metricsBundle;
            return this;
        }

        /** Builds the instance. */
        public @NonNull NetworkEvent build() {
            NetworkEvent o =
                    new NetworkEvent(mNetworkType, mTimeSinceCreatedMillis, mMetricsBundle);
            return o;
        }
    }
}
