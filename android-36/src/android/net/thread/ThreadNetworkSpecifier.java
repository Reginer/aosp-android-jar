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

package android.net.thread;

import static android.net.thread.ActiveOperationalDataset.LENGTH_EXTENDED_PAN_ID;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.NetworkSpecifier;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.net.module.util.HexDump;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents and identifies a Thread network.
 *
 * @hide
 */
public final class ThreadNetworkSpecifier extends NetworkSpecifier implements Parcelable {
    /** The Extended PAN ID of a Thread network. */
    @NonNull private final byte[] mExtendedPanId;

    /** The Active Timestamp of a Thread network. */
    @Nullable private final OperationalDatasetTimestamp mActiveTimestamp;

    private final boolean mRouterEligibleForLeader;

    private ThreadNetworkSpecifier(@NonNull Builder builder) {
        mExtendedPanId = builder.mExtendedPanId.clone();
        mActiveTimestamp = builder.mActiveTimestamp;
        mRouterEligibleForLeader = builder.mRouterEligibleForLeader;
    }

    /** Returns the Extended PAN ID of the Thread network this specifier refers to. */
    @NonNull
    public byte[] getExtendedPanId() {
        return mExtendedPanId.clone();
    }

    /**
     * Returns the Active Timestamp of the Thread network this specifier refers to, or {@code null}
     * if not specified.
     */
    @Nullable
    public OperationalDatasetTimestamp getActiveTimestamp() {
        return mActiveTimestamp;
    }

    /**
     * Returns {@code true} if this device can be a leader during attachment when there are no
     * nearby routers.
     */
    public boolean isRouterEligibleForLeader() {
        return mRouterEligibleForLeader;
    }

    /**
     * Returns {@code true} if both {@link #getExtendedPanId()} and {@link #getActiveTimestamp()}
     * (if not {@code null}) of the two {@link ThreadNetworkSpecifier} objects are equal.
     *
     * <p>Note value of {@link #isRouterEligibleForLeader()} is expiclitly excluded because this is
     * not part of the identifier.
     *
     * @hide
     */
    @Override
    public boolean canBeSatisfiedBy(@Nullable NetworkSpecifier other) {
        if (!(other instanceof ThreadNetworkSpecifier)) {
            return false;
        }
        ThreadNetworkSpecifier otherSpecifier = (ThreadNetworkSpecifier) other;

        if (mActiveTimestamp != null && !mActiveTimestamp.equals(otherSpecifier.mActiveTimestamp)) {
            return false;
        }

        return Arrays.equals(mExtendedPanId, otherSpecifier.mExtendedPanId);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof ThreadNetworkSpecifier)) {
            return false;
        } else if (this == other) {
            return true;
        }

        ThreadNetworkSpecifier otherSpecifier = (ThreadNetworkSpecifier) other;

        return Arrays.equals(mExtendedPanId, otherSpecifier.mExtendedPanId)
                && Objects.equals(mActiveTimestamp, otherSpecifier.mActiveTimestamp)
                && mRouterEligibleForLeader == otherSpecifier.mRouterEligibleForLeader;
    }

    @Override
    public int hashCode() {
        return deepHashCode(mExtendedPanId, mActiveTimestamp, mRouterEligibleForLeader);
    }

    /** An easy-to-use wrapper of {@link Arrays#deepHashCode}. */
    private static int deepHashCode(Object... values) {
        return Arrays.deepHashCode(values);
    }

    @Override
    public String toString() {
        return "ThreadNetworkSpecifier{extendedPanId="
                + HexDump.toHexString(mExtendedPanId)
                + ", activeTimestamp="
                + mActiveTimestamp
                + ", routerEligibleForLeader="
                + mRouterEligibleForLeader
                + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeByteArray(mExtendedPanId);
        dest.writeByteArray(mActiveTimestamp != null ? mActiveTimestamp.toTlvValue() : null);
        dest.writeBoolean(mRouterEligibleForLeader);
    }

    public static final @NonNull Parcelable.Creator<ThreadNetworkSpecifier> CREATOR =
            new Parcelable.Creator<ThreadNetworkSpecifier>() {
                @Override
                public ThreadNetworkSpecifier createFromParcel(Parcel in) {
                    byte[] extendedPanId = in.createByteArray();
                    byte[] activeTimestampBytes = in.createByteArray();
                    OperationalDatasetTimestamp activeTimestamp =
                            (activeTimestampBytes != null)
                                    ? OperationalDatasetTimestamp.fromTlvValue(activeTimestampBytes)
                                    : null;
                    boolean routerEligibleForLeader = in.readBoolean();

                    return new Builder(extendedPanId)
                            .setActiveTimestamp(activeTimestamp)
                            .setRouterEligibleForLeader(routerEligibleForLeader)
                            .build();
                }

                @Override
                public ThreadNetworkSpecifier[] newArray(int size) {
                    return new ThreadNetworkSpecifier[size];
                }
            };

    /** The builder for creating {@link ActiveOperationalDataset} objects. */
    public static final class Builder {
        @NonNull private final byte[] mExtendedPanId;
        @Nullable private OperationalDatasetTimestamp mActiveTimestamp;
        private boolean mRouterEligibleForLeader;

        /**
         * Creates a new {@link Builder} object with given Extended PAN ID.
         *
         * @throws IllegalArgumentException if {@code extendedPanId} is {@code null} or the length
         *     is not {@link ActiveOperationalDataset#LENGTH_EXTENDED_PAN_ID}
         */
        public Builder(@NonNull byte[] extendedPanId) {
            if (extendedPanId == null || extendedPanId.length != LENGTH_EXTENDED_PAN_ID) {
                throw new IllegalArgumentException(
                        "extendedPanId is null or length is not "
                                + LENGTH_EXTENDED_PAN_ID
                                + ": "
                                + Arrays.toString(extendedPanId));
            }
            mExtendedPanId = extendedPanId.clone();
            mRouterEligibleForLeader = false;
        }

        /**
         * Creates a new {@link Builder} object by copying the data in the given {@code specifier}
         * object.
         */
        public Builder(@NonNull ThreadNetworkSpecifier specifier) {
            this(specifier.getExtendedPanId());
            setActiveTimestamp(specifier.getActiveTimestamp());
            setRouterEligibleForLeader(specifier.isRouterEligibleForLeader());
        }

        /** Sets the Active Timestamp of the Thread network. */
        @NonNull
        public Builder setActiveTimestamp(@Nullable OperationalDatasetTimestamp activeTimestamp) {
            mActiveTimestamp = activeTimestamp;
            return this;
        }

        /**
         * Sets whether this device should be a leader during attachment when there are no nearby
         * routers.
         */
        @NonNull
        public Builder setRouterEligibleForLeader(boolean eligible) {
            mRouterEligibleForLeader = eligible;
            return this;
        }

        /** Creates a new {@link ThreadNetworkSpecifier} object from values set so far. */
        @NonNull
        public ThreadNetworkSpecifier build() {
            return new ThreadNetworkSpecifier(this);
        }
    }
}
