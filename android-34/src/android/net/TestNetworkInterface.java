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
package android.net;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.util.Log;

import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * This class is used to return the interface name, fd, MAC, and MTU of the test interface
 *
 * TestNetworkInterfaces are created by TestNetworkService and provide a
 * wrapper around a tun/tap interface that can be used in integration tests.
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class TestNetworkInterface implements Parcelable {
    private static final String TAG = "TestNetworkInterface";

    @NonNull
    private final ParcelFileDescriptor mFileDescriptor;
    @NonNull
    private final String mInterfaceName;
    @Nullable
    private final MacAddress mMacAddress;
    private final int mMtu;

    @Override
    public int describeContents() {
        return (mFileDescriptor != null) ? Parcelable.CONTENTS_FILE_DESCRIPTOR : 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeParcelable(mFileDescriptor, flags);
        out.writeString(mInterfaceName);
        out.writeParcelable(mMacAddress, flags);
        out.writeInt(mMtu);
    }

    public TestNetworkInterface(@NonNull ParcelFileDescriptor pfd, @NonNull String intf) {
        mFileDescriptor = pfd;
        mInterfaceName = intf;

        MacAddress macAddress = null;
        int mtu = 1500;
        try {
            // This constructor is called by TestNetworkManager which runs inside the system server,
            // which has permission to read the MacAddress.
            NetworkInterface nif = NetworkInterface.getByName(mInterfaceName);

            // getHardwareAddress() returns null for tun interfaces.
            byte[] hardwareAddress = nif.getHardwareAddress();
            if (hardwareAddress != null) {
                macAddress = MacAddress.fromBytes(nif.getHardwareAddress());
            }
            mtu = nif.getMTU();
        } catch (SocketException e) {
            Log.e(TAG, "Failed to fetch MacAddress or MTU size from NetworkInterface", e);
        }
        mMacAddress = macAddress;
        mMtu = mtu;
    }

    private TestNetworkInterface(@NonNull Parcel in) {
        mFileDescriptor = in.readParcelable(ParcelFileDescriptor.class.getClassLoader());
        mInterfaceName = in.readString();
        mMacAddress = in.readParcelable(MacAddress.class.getClassLoader());
        mMtu = in.readInt();
    }

    @NonNull
    public ParcelFileDescriptor getFileDescriptor() {
        return mFileDescriptor;
    }

    @NonNull
    public String getInterfaceName() {
        return mInterfaceName;
    }

    /**
     * Returns the tap interface MacAddress.
     *
     * When TestNetworkInterface wraps a tun interface, the MAC address is null.
     *
     * @return the tap interface MAC address or null.
     */
    @Nullable
    public MacAddress getMacAddress() {
        return mMacAddress;
    }

    /**
     * Returns the interface MTU.
     *
     * MTU defaults to 1500 if an error occurs.
     *
     * @return MTU in bytes.
     */
    public int getMtu() {
        return mMtu;
    }

    @NonNull
    public static final Parcelable.Creator<TestNetworkInterface> CREATOR =
            new Parcelable.Creator<TestNetworkInterface>() {
                public TestNetworkInterface createFromParcel(Parcel in) {
                    return new TestNetworkInterface(in);
                }

                public TestNetworkInterface[] newArray(int size) {
                    return new TestNetworkInterface[size];
                }
            };
}
