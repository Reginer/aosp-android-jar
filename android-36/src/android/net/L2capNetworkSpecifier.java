/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.net.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * A {@link NetworkSpecifier} used to identify an L2CAP network over BLE.
 *
 * An L2CAP network is not symmetrical, meaning there exists both a server (Bluetooth peripheral)
 * and a client (Bluetooth central) node. This specifier contains the information required to
 * request a client L2CAP network using {@link ConnectivityManager#requestNetwork} while specifying
 * the remote MAC address, and Protocol/Service Multiplexer (PSM). It can also contain information
 * allocated by the system when reserving a server network using {@link
 * ConnectivityManager#reserveNetwork} such as the Protocol/Service Multiplexer (PSM). In both
 * cases, the header compression option must be specified.
 *
 * An L2CAP server network allocates a Protocol/Service Multiplexer (PSM) to be advertised to the
 * client. A new server network must always be reserved using {@code
 * ConnectivityManager#reserveNetwork}. The subsequent {@link
 * ConnectivityManager.NetworkCallback#onReserved(NetworkCapabilities)} callback includes an {@code
 * L2CapNetworkSpecifier}. The {@link getPsm()} method will return the Protocol/Service Multiplexer
 * (PSM) of the reserved network so that the server can advertise it to the client and the client
 * can connect.
 * An L2CAP server network is backed by a {@link android.bluetooth.BluetoothServerSocket} which can,
 * in theory, accept many connections. However, before SDK version {@link
 * Build.VERSION_CODES.VANILLA_ICE_CREAM} Bluetooth APIs do not expose the channel ID, so these
 * connections are indistinguishable. In practice, this means that the network matching semantics in
 * ConnectivityService will tear down all but the first connection.
 *
 * When the connection between client and server completes, a {@link Network} whose capabilities
 * satisfy this {@code L2capNetworkSpecifier} will connect and the usual callbacks, such as {@link
 * NetworkCallback#onAvailable}, will be called on the callback object passed to {@code
 * ConnectivityManager#reserveNetwork} or {@code ConnectivityManager#requestNetwork}.
 */
@FlaggedApi(Flags.FLAG_IPV6_OVER_BLE)
public final class L2capNetworkSpecifier extends NetworkSpecifier implements Parcelable {
    /**
     * Match any role.
     *
     * This role is only meaningful in {@link NetworkRequest}s. Specifiers for actual L2CAP
     * networks never have this role set.
     */
    public static final int ROLE_ANY = 0;
    /** Specifier describes a client network, i.e., the device is the Bluetooth central. */
    public static final int ROLE_CLIENT = 1;
    /** Specifier describes a server network, i.e., the device is the Bluetooth peripheral. */
    public static final int ROLE_SERVER = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = false, prefix = "ROLE_", value = {
        ROLE_ANY,
        ROLE_CLIENT,
        ROLE_SERVER
    })
    public @interface Role {}
    /** Role used to distinguish client from server networks. */
    @Role
    private final int mRole;

    /**
     * Accept any form of header compression.
     *
     * This option is only meaningful in {@link NetworkRequest}s. Specifiers for actual L2CAP
     * networks never have this option set.
     */
    public static final int HEADER_COMPRESSION_ANY = 0;
    /** Do not compress packets on this network. */
    public static final int HEADER_COMPRESSION_NONE = 1;
    /** Use 6lowpan header compression as specified in rfc6282. */
    public static final int HEADER_COMPRESSION_6LOWPAN = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = false, prefix = "HEADER_COMPRESSION_", value = {
        HEADER_COMPRESSION_ANY,
        HEADER_COMPRESSION_NONE,
        HEADER_COMPRESSION_6LOWPAN
    })
    public @interface HeaderCompression {}
    /** Header compression mechanism used on this network. */
    @HeaderCompression
    private final int mHeaderCompression;

    /** The MAC address of the remote. */
    @Nullable
    private final MacAddress mRemoteAddress;

    /**
     * Match any Protocol/Service Multiplexer (PSM).
     *
     * This PSM value is only meaningful in {@link NetworkRequest}s. Specifiers for actual L2CAP
     * networks never have this value set.
     */
    public static final int PSM_ANY = 0;

    /** The Bluetooth L2CAP Protocol/Service Multiplexer (PSM). */
    private final int mPsm;

    private L2capNetworkSpecifier(Parcel in) {
        mRole = in.readInt();
        mHeaderCompression = in.readInt();
        mRemoteAddress = in.readParcelable(getClass().getClassLoader());
        mPsm = in.readInt();
    }

    /** @hide */
    public L2capNetworkSpecifier(@Role int role, @HeaderCompression int headerCompression,
            MacAddress remoteAddress, int psm) {
        mRole = role;
        mHeaderCompression = headerCompression;
        mRemoteAddress = remoteAddress;
        mPsm = psm;
    }

    /** Returns the role to be used for this network. */
    @Role
    public int getRole() {
        return mRole;
    }

    /** Returns the compression mechanism for this network. */
    @HeaderCompression
    public int getHeaderCompression() {
        return mHeaderCompression;
    }

    /**
     * Returns the remote MAC address for this network to connect to.
     *
     * The remote address is only meaningful for networks that have ROLE_CLIENT.
     *
     * When receiving this {@link L2capNetworkSpecifier} from Connectivity APIs such as a {@link
     * ConnectivityManager.NetworkCallback}, the MAC address is redacted.
     */
    public @Nullable MacAddress getRemoteAddress() {
        return mRemoteAddress;
    }

    /** Returns the Protocol/Service Multiplexer (PSM) for this network to connect to. */
    public int getPsm() {
        return mPsm;
    }

    /** A builder class for L2capNetworkSpecifier. */
    public static final class Builder {
        @Role
        private int mRole = ROLE_ANY;
        @HeaderCompression
        private int mHeaderCompression = HEADER_COMPRESSION_ANY;
        @Nullable
        private MacAddress mRemoteAddress;
        private int mPsm = PSM_ANY;

        /**
         * Set the role to use for this network.
         *
         * If not set, defaults to {@link ROLE_ANY}.
         *
         * @param role the role to use.
         */
        @NonNull
        public Builder setRole(@Role int role) {
            mRole = role;
            return this;
        }

        /**
         * Set the header compression mechanism to use for this network.
         *
         * If not set, defaults to {@link HEADER_COMPRESSION_ANY}. This option must be specified
         * (i.e. must not be set to {@link HEADER_COMPRESSION_ANY}) when requesting or reserving a
         * new network.
         *
         * @param headerCompression the header compression mechanism to use.
         */
        @NonNull
        public Builder setHeaderCompression(@HeaderCompression int headerCompression) {
            mHeaderCompression = headerCompression;
            return this;
        }

        /**
         * Set the remote address for the client to connect to.
         *
         * Only valid for client networks. If not set, the specifier matches any MAC address.
         *
         * @param remoteAddress the MAC address to connect to, or null to match any MAC address.
         */
        @NonNull
        public Builder setRemoteAddress(@Nullable MacAddress remoteAddress) {
            mRemoteAddress = remoteAddress;
            return this;
        }

        /**
         * Set the Protocol/Service Multiplexer (PSM) for the client to connect to.
         *
         * If not set, defaults to {@link PSM_ANY}.
         *
         * @param psm the Protocol/Service Multiplexer (PSM) to connect to.
         */
        @NonNull
        public Builder setPsm(@IntRange(from = 0, to = 255) int psm) {
            if (psm < 0 /* PSM_ANY */ || psm > 0xFF) {
                throw new IllegalArgumentException("PSM must be PSM_ANY or within range [1, 255]");
            }
            mPsm = psm;
            return this;
        }

        /** Create the L2capNetworkSpecifier object. */
        @NonNull
        public L2capNetworkSpecifier build() {
            if (mRole == ROLE_SERVER && mRemoteAddress != null) {
                throw new IllegalArgumentException(
                        "Specifying a remote address is not valid for server role.");
            }
            return new L2capNetworkSpecifier(mRole, mHeaderCompression, mRemoteAddress, mPsm);
        }
    }

    /** @hide */
    @Override
    public boolean canBeSatisfiedBy(NetworkSpecifier other) {
        if (!(other instanceof L2capNetworkSpecifier)) return false;
        final L2capNetworkSpecifier rhs = (L2capNetworkSpecifier) other;

        // A network / offer cannot be ROLE_ANY, but it is added for consistency.
        if (mRole != rhs.mRole && mRole != ROLE_ANY && rhs.mRole != ROLE_ANY) {
            return false;
        }

        if (mHeaderCompression != rhs.mHeaderCompression
                && mHeaderCompression != HEADER_COMPRESSION_ANY
                && rhs.mHeaderCompression != HEADER_COMPRESSION_ANY) {
            return false;
        }

        if (!Objects.equals(mRemoteAddress, rhs.mRemoteAddress)
                && mRemoteAddress != null && rhs.mRemoteAddress != null) {
            return false;
        }

        if (mPsm != rhs.mPsm && mPsm != PSM_ANY && rhs.mPsm != PSM_ANY) {
            return false;
        }
        return true;
    }

    /** @hide */
    @Override
    @Nullable
    public NetworkSpecifier redact() {
        final NetworkSpecifier redactedSpecifier = new Builder()
                .setRole(mRole)
                .setHeaderCompression(mHeaderCompression)
                // The remote address is redacted.
                .setRemoteAddress(null)
                .setPsm(mPsm)
                .build();
        return redactedSpecifier;
    }

    /** @hide */
    @Override
    public int hashCode() {
        return Objects.hash(mRole, mHeaderCompression, mRemoteAddress, mPsm);
    }

    /** @hide */
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof L2capNetworkSpecifier)) return false;

        final L2capNetworkSpecifier rhs = (L2capNetworkSpecifier) obj;
        return mRole == rhs.mRole
                && mHeaderCompression == rhs.mHeaderCompression
                && Objects.equals(mRemoteAddress, rhs.mRemoteAddress)
                && mPsm == rhs.mPsm;
    }

    /** @hide */
    @Override
    public String toString() {
        final String role;
        switch (mRole) {
            case ROLE_CLIENT:
                role = "ROLE_CLIENT";
                break;
            case ROLE_SERVER:
                role = "ROLE_SERVER";
                break;
            default:
                role = "ROLE_ANY";
                break;
        }

        final String headerCompression;
        switch (mHeaderCompression) {
            case HEADER_COMPRESSION_NONE:
                headerCompression = "HEADER_COMPRESSION_NONE";
                break;
            case HEADER_COMPRESSION_6LOWPAN:
                headerCompression = "HEADER_COMPRESSION_6LOWPAN";
                break;
            default:
                headerCompression = "HEADER_COMPRESSION_ANY";
                break;
        }

        final String psm = (mPsm == PSM_ANY) ? "PSM_ANY" : String.valueOf(mPsm);

        return String.format("L2capNetworkSpecifier(%s, %s, RemoteAddress=%s, PSM=%s)",
                role, headerCompression, Objects.toString(mRemoteAddress), psm);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mRole);
        dest.writeInt(mHeaderCompression);
        dest.writeParcelable(mRemoteAddress, flags);
        dest.writeInt(mPsm);
    }

    public static final @NonNull Creator<L2capNetworkSpecifier> CREATOR = new Creator<>() {
        @Override
        public L2capNetworkSpecifier createFromParcel(Parcel in) {
            return new L2capNetworkSpecifier(in);
        }

        @Override
        public L2capNetworkSpecifier[] newArray(int size) {
            return new L2capNetworkSpecifier[size];
        }
    };
}
