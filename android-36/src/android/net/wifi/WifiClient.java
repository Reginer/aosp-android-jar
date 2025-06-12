/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.MacAddress;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.android.wifi.flags.Flags;

import java.util.Objects;

/** @hide */
@SystemApi
public final class WifiClient implements Parcelable {

    private static final String TAG = "WifiClient";

    private final MacAddress mMacAddress;

    /** The identifier of the AP instance which the client connected. */
    private final String mApInstanceIdentifier;

    /**
     * Reason for disconnection, if known.
     *
     * <p>This field is only meaningful when a client disconnects.
     * It will not be updated while a client is connected.
     */
    @WifiAnnotations.SoftApDisconnectReason
    private final int mDisconnectReason;

    /**
     * The mac address of this client.
     */
    @NonNull
    public MacAddress getMacAddress() {
        return mMacAddress;
    }

    /**
     * Get AP instance identifier.
     *
     * The AP instance identifier is a unique identity which can be used to
     * associate the {@link SoftApInfo} to a specific {@link WifiClient}
     * - see {@link SoftApInfo#getApInstanceIdentifier()}
     * @hide
     */
    @NonNull
    public String getApInstanceIdentifier() {
        return mApInstanceIdentifier;
    }

    /**
     * Get the reason the client disconnected from the AP.
     *
     * <p>This field is only populated when the WifiClient is returned via
     * {@link WifiManager.SoftApCallback#onClientsDisconnected}.
     * The value {@link DeauthenticationReasonCode#REASON_UNKNOWN} is used as the default value
     * and in the case where a client connects.
     * @return a disconnection reason code to provide information on why the disconnect happened.
     */
    @FlaggedApi(Flags.FLAG_SOFTAP_DISCONNECT_REASON)
    @WifiAnnotations.SoftApDisconnectReason
    public int getDisconnectReason() {
        return mDisconnectReason;
    }

    private WifiClient(Parcel in) {
        mMacAddress = in.readParcelable(null);
        mApInstanceIdentifier = in.readString();
        mDisconnectReason = in.readInt();
    }

    /** @hide */
    public WifiClient(@NonNull MacAddress macAddress, @NonNull String apInstanceIdentifier) {
        this(macAddress, apInstanceIdentifier, DeauthenticationReasonCode.REASON_UNKNOWN);
    }

    /** @hide */
    public WifiClient(@NonNull MacAddress macAddress, @NonNull String apInstanceIdentifier,
            @WifiAnnotations.SoftApDisconnectReason int disconnectReason) {
        if (macAddress == null) {
            Log.wtf(TAG, "Null MacAddress provided");
            this.mMacAddress = WifiManager.ALL_ZEROS_MAC_ADDRESS;
        } else {
            this.mMacAddress = macAddress;
        }
        this.mApInstanceIdentifier = apInstanceIdentifier;
        this.mDisconnectReason = disconnectReason;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mMacAddress, flags);
        dest.writeString(mApInstanceIdentifier);
        dest.writeInt(mDisconnectReason);
    }

    @NonNull
    public static final Creator<WifiClient> CREATOR = new Creator<WifiClient>() {
        public WifiClient createFromParcel(Parcel in) {
            return new WifiClient(in);
        }

        public WifiClient[] newArray(int size) {
            return new WifiClient[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return "WifiClient{"
                + "mMacAddress=" + mMacAddress
                + "mApInstanceIdentifier=" + mApInstanceIdentifier
                + "mDisconnectReason=" + mDisconnectReason
                + '}';
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof WifiClient client)) return false;
        return Objects.equals(mMacAddress, client.mMacAddress)
                && mApInstanceIdentifier.equals(client.mApInstanceIdentifier)
                && mDisconnectReason == client.mDisconnectReason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mMacAddress, mApInstanceIdentifier, mDisconnectReason);
    }
}
