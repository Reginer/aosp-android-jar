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

package android.net.wifi.p2p;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.RequiresApi;
import android.net.MacAddress;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.wifi.flags.Flags;

import java.util.Arrays;

/**
 * This object contains the Device Identity Resolution (DIR) Info to check if the device is a
 * previously paired device.
 * The device advertises this information in Bluetooth LE advertising packets
 * and Unsynchronized Service Discovery (USD) frames. The device receiving DIR
 * Info uses this information to identify that the peer device is a previously paired device.
 * For Details, refer Wi-Fi Alliance Wi-Fi Direct R2 specification section 3.8.2 Pairing Identity
 * and section 3.9.2.3.2 Optional Advertising Data Elements.
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
public final class WifiP2pDirInfo implements Parcelable {
    /**
     * The MAC address of the P2P device interface.
     */
    private MacAddress mMacAddress;

    /**
     * Random number of 8 octets.
     */
    private byte[] mNonce;

    /**
     * A resolvable identity value of 8 octets.
     */
    private byte[] mDirTag;

    /**
     * @return the MAC address of the P2P device interface.
     */
    @NonNull
    public MacAddress getMacAddress() {
        return mMacAddress;
    }

    /**
     * Get the nonce value used to derive DIR Tag.
     * See {@link WifiP2pDirInfo}
     *
     *  @return A byte-array of random number of size 8 octets.
     */
    @NonNull
    public byte[] getNonce() {
        return mNonce;
    }

    /**
     * Get the DIR Tag value.
     * See {@link WifiP2pDirInfo}
     *
     *  @return A byte-array of Tag value of size 8 octets.
     */
    @NonNull
    public byte[] getDirTag() {
        return mDirTag;
    }

    /**
     * Constructor for Device Identity Resolution (DIR) Info generated based on the 128 bit Device
     * Identity key. For details, refer Wi-Fi Alliance Wi-Fi Direct R2 specification Table 8.
     *
     * @param macAddress The MAC address of the P2P device interface.
     * @param nonce Random number of 8 octets.
     * @param dirTag Resolvable identity value of 8 octets derived based on the device MAC address,
     *               device identity key and P2P device MAC address.
     *               Tag =  Truncate-64(HMAC-SHA-256(DevIk, "DIR" || P2P Device Address || Nonce))
     *
     */
    public WifiP2pDirInfo(@NonNull MacAddress macAddress, @NonNull byte[] nonce,
            @NonNull byte[] dirTag) {
        mMacAddress = macAddress;
        mNonce = nonce;
        mDirTag = dirTag;
    }

    /**
     * Generates a string of all the defined elements.
     *
     * @return a compiled string representing all elements
     */
    public String toString() {
        StringBuilder sbuf = new StringBuilder("WifiP2pDirInfo:");
        sbuf.append("\n Mac Address: ").append(mMacAddress);
        sbuf.append("\n Nonce : ").append((mNonce == null)
                ? "<null>" : Arrays.toString(mNonce));
        sbuf.append("\n DIR Tag : ").append((mDirTag == null)
                ? "<null>" : Arrays.toString(mDirTag));
        return sbuf.toString();
    }

    /** Implement the Parcelable interface */
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        mMacAddress.writeToParcel(dest, flags);
        dest.writeByteArray(mNonce);
        dest.writeByteArray(mDirTag);
    }

    /** Implement the Parcelable interface */
    @NonNull
    public static final Creator<WifiP2pDirInfo> CREATOR =
            new Creator<WifiP2pDirInfo>() {
                public WifiP2pDirInfo createFromParcel(Parcel in) {
                    return new WifiP2pDirInfo(MacAddress.CREATOR.createFromParcel(in),
                            in.createByteArray(), in.createByteArray());
                }

                public WifiP2pDirInfo[] newArray(int size) {
                    return new WifiP2pDirInfo[size];
                }
            };
}
