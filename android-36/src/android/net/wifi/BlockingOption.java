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
package android.net.wifi;

import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.android.wifi.flags.Flags;

import java.util.Objects;

/**
 * Options for blocking a network through
 * {@link WifiManager#disallowCurrentSuggestedNetwork(BlockingOption)}
 */
@FlaggedApi(Flags.FLAG_BSSID_BLOCKLIST_FOR_SUGGESTION)
public final class BlockingOption implements Parcelable {
    private final int mDisableTime;
    private final boolean mBSSIDOnly;

    /**
     * @hide
     */
    public BlockingOption(int disableTime, boolean bssidOnly) {
        mDisableTime = disableTime;
        mBSSIDOnly = bssidOnly;
    }

    /**
     * @hide
     */
    public BlockingOption(Parcel in) {
        mDisableTime = in.readInt();
        mBSSIDOnly = in.readBoolean();
    }

    /**
     * Get the blocking time which is set by {@link Builder#Builder(int)}
     * @return Blocking time in seconds
     */
    public int getBlockingTimeSeconds() {
        return mDisableTime;
    }

    /**
     * Return whether or not a single BSSID is being blocked, which is set by
     * {@link Builder#setBlockingBssidOnly(boolean)}
     * @return True for blocking single BSSID, false otherwise.
     */
    public boolean isBlockingBssidOnly() {
        return mBSSIDOnly;
    }

    /**
     * Builder used to create {@link BlockingOption} objects.
     */
    @FlaggedApi(Flags.FLAG_BSSID_BLOCKLIST_FOR_SUGGESTION)
    public static final class Builder {
        private int mDisableTime;
        private boolean mBSSIDOnly;

        /**
         * Create a {@link Builder} with blocking time for the network
         *
         * @param blockingTimeSec Time period to block the network in seconds
         * @throws IllegalArgumentException if input is invalid.
         */
        public Builder(@IntRange(from = 1, to = 86400) int blockingTimeSec) {
            if (blockingTimeSec < 1 || blockingTimeSec > 86400) {
                throw new IllegalArgumentException("blockingTimeSec should between 1 to 86400");
            }
            mDisableTime = blockingTimeSec;
            mBSSIDOnly = false;
        }

        /**
         * Set to configure blocking the whole network or a single BSSID. By default, the whole
         * network will be blocked.
         * @param bssidOnly True for a single BSSID, otherwise the whole network will be blocked
         * @return Instance of {@link Builder} to enable chaining of the builder method.
         */
        @NonNull public Builder setBlockingBssidOnly(boolean bssidOnly) {
            mBSSIDOnly = bssidOnly;
            return this;
        }

        /**
         * Create a BlockingOption object for use in
         * {@link WifiManager#disallowCurrentSuggestedNetwork(BlockingOption)}.
         */
        @NonNull public BlockingOption build() {
            return new BlockingOption(mDisableTime, mBSSIDOnly);
        }
    }

    @NonNull
    public static final Creator<BlockingOption> CREATOR = new Creator<BlockingOption>() {
        @Override
        public BlockingOption createFromParcel(Parcel in) {
            return new BlockingOption(in);
        }

        @Override
        public BlockingOption[] newArray(int size) {
            return new BlockingOption[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mDisableTime);
        dest.writeBoolean(mBSSIDOnly);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BlockingOption lhs)) {
            return false;
        }
        return mBSSIDOnly == lhs.mBSSIDOnly && mDisableTime == lhs.mDisableTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mBSSIDOnly, mDisableTime);
    }

    @Override
    public String toString() {
        return "BlockingOption[ "
                + "DisableTime=" + mDisableTime
                + ", BSSIDOnly=" + mBSSIDOnly;
    }
}
