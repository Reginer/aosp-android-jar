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

package android.net;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 * A class representing a configuration for multicast routing.
 *
 * Internal usage to Connectivity
 * @hide
 */
// @SystemApi(client = MODULE_LIBRARIES)
public final class MulticastRoutingConfig implements Parcelable {
    private static final String TAG = MulticastRoutingConfig.class.getSimpleName();

    /** Do not forward any multicast packets. */
    public static final int FORWARD_NONE = 0;
    /**
     * Forward only multicast packets with destination in the list of listening addresses.
     * Ignore the min scope.
     */
    public static final int FORWARD_SELECTED = 1;
    /**
     * Forward all multicast packets with scope greater or equal than the min scope.
     * Ignore the list of listening addresses.
     */
    public static final int FORWARD_WITH_MIN_SCOPE = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "FORWARD_" }, value = {
            FORWARD_NONE,
            FORWARD_SELECTED,
            FORWARD_WITH_MIN_SCOPE
    })
    public @interface MulticastForwardingMode {}

    /**
     * Not a multicast scope, for configurations that do not use the min scope.
     */
    public static final int MULTICAST_SCOPE_NONE = -1;

    /** @hide */
    public static final MulticastRoutingConfig CONFIG_FORWARD_NONE =
            new MulticastRoutingConfig(FORWARD_NONE, MULTICAST_SCOPE_NONE, null);

    @MulticastForwardingMode
    private final int mForwardingMode;

    private final int mMinScope;

    @NonNull
    private final Set<Inet6Address> mListeningAddresses;

    private MulticastRoutingConfig(@MulticastForwardingMode final int mode, final int scope,
            @Nullable final Set<Inet6Address> addresses) {
        mForwardingMode = mode;
        mMinScope = scope;
        if (null != addresses) {
            mListeningAddresses = Collections.unmodifiableSet(new ArraySet<>(addresses));
        } else {
            mListeningAddresses = Collections.emptySet();
        }
    }

    /**
     * Returns the forwarding mode.
     */
    @MulticastForwardingMode
    public int getForwardingMode() {
        return mForwardingMode;
    }

    /**
     * Returns the minimal group address scope that is allowed for forwarding.
     * If the forwarding mode is not FORWARD_WITH_MIN_SCOPE, will be MULTICAST_SCOPE_NONE.
     */
    public int getMinimumScope() {
        return mMinScope;
    }

    /**
     * Returns the list of group addresses listened by the outgoing interface.
     * The list will be empty if the forwarding mode is not FORWARD_SELECTED.
     */
    @NonNull
    public Set<Inet6Address> getListeningAddresses() {
        return mListeningAddresses;
    }

    private MulticastRoutingConfig(Parcel in) {
        mForwardingMode = in.readInt();
        mMinScope = in.readInt();
        final int count = in.readInt();
        final ArraySet<Inet6Address> listeningAddresses = new ArraySet<>(count);
        final byte[] buffer = new byte[16]; // Size of an Inet6Address
        for (int i = 0; i < count; ++i) {
            in.readByteArray(buffer);
            try {
                listeningAddresses.add((Inet6Address) Inet6Address.getByAddress(buffer));
            } catch (UnknownHostException e) {
                Log.wtf(TAG, "Can't read inet6address : " + Arrays.toString(buffer));
            }
        }
        mListeningAddresses = Collections.unmodifiableSet(listeningAddresses);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mForwardingMode);
        dest.writeInt(mMinScope);
        dest.writeInt(mListeningAddresses.size());
        for (final Inet6Address addr : mListeningAddresses) {
            dest.writeByteArray(addr.getAddress());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<MulticastRoutingConfig> CREATOR = new Creator<>() {
        @Override
        public MulticastRoutingConfig createFromParcel(@NonNull Parcel in) {
            return new MulticastRoutingConfig(in);
        }

        @Override
        public MulticastRoutingConfig[] newArray(int size) {
            return new MulticastRoutingConfig[size];
        }
    };

    private static String forwardingModeToString(final int forwardingMode) {
        switch (forwardingMode) {
            case FORWARD_NONE: return "NONE";
            case FORWARD_SELECTED: return "SELECTED";
            case FORWARD_WITH_MIN_SCOPE: return "WITH_MIN_SCOPE";
            default: return "UNKNOWN";
        }
    }

    public static final class Builder {
        @MulticastForwardingMode
        private final int mForwardingMode;
        private int mMinScope;
        private final ArraySet<Inet6Address> mListeningAddresses;

        // The two constructors with runtime checks for the mode and scope are arguably
        // less convenient than three static factory methods, but API guidelines mandates
        // that Builders are built with a constructor and not factory methods.
        /**
         * Create a new builder for forwarding mode FORWARD_NONE or FORWARD_SELECTED.
         *
         * <p>On a Builder for FORWARD_NONE, no properties can be set.
         * <p>On a Builder for FORWARD_SELECTED, listening addresses can be added and removed
         * but the minimum scope can't be set.
         *
         * @param mode {@link #FORWARD_NONE} or {@link #FORWARD_SELECTED}. Any other
         *             value will result in IllegalArgumentException.
         * @see #Builder(int, int)
         */
        public Builder(@MulticastForwardingMode final int mode) {
            if (FORWARD_NONE != mode && FORWARD_SELECTED != mode) {
                if (FORWARD_WITH_MIN_SCOPE == mode) {
                    throw new IllegalArgumentException("FORWARD_WITH_MIN_SCOPE requires "
                            + "passing the scope as a second argument");
                } else {
                    throw new IllegalArgumentException("Unknown forwarding mode : " + mode);
                }
            }
            mForwardingMode = mode;
            mMinScope = MULTICAST_SCOPE_NONE;
            mListeningAddresses = new ArraySet<>();
        }

        /**
         * Create a new builder for forwarding mode FORWARD_WITH_MIN_SCOPE.
         *
         * <p>On this Builder the scope can be set with {@link #setMinimumScope}, but
         * listening addresses can't be added or removed.
         *
         * @param mode Must be {@link #FORWARD_WITH_MIN_SCOPE}.
         * @param scope the minimum scope for this multicast routing config.
         * @see Builder#Builder(int)
         */
        public Builder(@MulticastForwardingMode final int mode, int scope) {
            if (FORWARD_WITH_MIN_SCOPE != mode) {
                throw new IllegalArgumentException("Forwarding with a min scope must "
                        + "use forward mode FORWARD_WITH_MIN_SCOPE");
            }
            mForwardingMode = mode;
            mMinScope = scope;
            mListeningAddresses = new ArraySet<>();
        }

        /**
         * Sets the minimum scope for this multicast routing config.
         * This is only meaningful (indeed, allowed) for configs in FORWARD_WITH_MIN_SCOPE mode.
         * @return this builder
         */
        @NonNull
        public Builder setMinimumScope(final int scope) {
            if (FORWARD_WITH_MIN_SCOPE != mForwardingMode) {
                throw new IllegalArgumentException("Can't set the scope on a builder in mode "
                        + modeToString(mForwardingMode));
            }
            mMinScope = scope;
            return this;
        }

        /**
         * Add an address to the set of listening addresses.
         *
         * This is only meaningful (indeed, allowed) for configs in FORWARD_SELECTED mode.
         * If this address was already added, this is a no-op.
         * @return this builder
         */
        @NonNull
        public Builder addListeningAddress(@NonNull final Inet6Address address) {
            if (FORWARD_SELECTED != mForwardingMode) {
                throw new IllegalArgumentException("Can't add an address on a builder in mode "
                        + modeToString(mForwardingMode));
            }
            // TODO : should we check that this is a multicast address ?
            mListeningAddresses.add(address);
            return this;
        }

        /**
         * Remove an address from the set of listening addresses.
         *
         * This is only meaningful (indeed, allowed) for configs in FORWARD_SELECTED mode.
         * If this address was not added, or was already removed, this is a no-op.
         * @return this builder
         */
        @NonNull
        public Builder clearListeningAddress(@NonNull final Inet6Address address) {
            if (FORWARD_SELECTED != mForwardingMode) {
                throw new IllegalArgumentException("Can't remove an address on a builder in mode "
                        + modeToString(mForwardingMode));
            }
            mListeningAddresses.remove(address);
            return this;
        }

        /**
         * Build the config.
         */
        @NonNull
        public MulticastRoutingConfig build() {
            return new MulticastRoutingConfig(mForwardingMode, mMinScope, mListeningAddresses);
        }
    }

    private static String modeToString(@MulticastForwardingMode final int mode) {
        switch (mode) {
            case FORWARD_NONE: return "FORWARD_NONE";
            case FORWARD_SELECTED: return "FORWARD_SELECTED";
            case FORWARD_WITH_MIN_SCOPE: return "FORWARD_WITH_MIN_SCOPE";
            default: return "unknown multicast routing mode " + mode;
        }
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (!(other instanceof MulticastRoutingConfig)) {
            return false;
        } else {
            final MulticastRoutingConfig otherConfig = (MulticastRoutingConfig) other;
            return mForwardingMode == otherConfig.mForwardingMode
                && mMinScope == otherConfig.mMinScope
                && mListeningAddresses.equals(otherConfig.mListeningAddresses);
        }
    }

    public int hashCode() {
        return Objects.hash(mForwardingMode, mMinScope, mListeningAddresses);
    }

    public String toString() {
        final StringJoiner resultJoiner = new StringJoiner(" ", "{", "}");

        resultJoiner.add("ForwardingMode:");
        resultJoiner.add(modeToString(mForwardingMode));

        if (mForwardingMode == FORWARD_WITH_MIN_SCOPE) {
            resultJoiner.add("MinScope:");
            resultJoiner.add(Integer.toString(mMinScope));
        }

        if (mForwardingMode == FORWARD_SELECTED && !mListeningAddresses.isEmpty()) {
            resultJoiner.add("ListeningAddresses: [");
            resultJoiner.add(TextUtils.join(",", mListeningAddresses));
            resultJoiner.add("]");
        }

        return resultJoiner.toString();
    }
}
