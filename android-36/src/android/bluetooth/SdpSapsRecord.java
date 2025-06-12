/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

/** @hide */
public class SdpSapsRecord implements Parcelable {
    private final int mRfcommChannelNumber;
    private final int mProfileVersion;
    private final String mServiceName;

    public SdpSapsRecord(int rfcommChannelNumber, int profileVersion, String serviceName) {
        mRfcommChannelNumber = rfcommChannelNumber;
        mProfileVersion = profileVersion;
        mServiceName = serviceName;
    }

    public SdpSapsRecord(Parcel in) {
        mRfcommChannelNumber = in.readInt();
        mProfileVersion = in.readInt();
        mServiceName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getRfcommCannelNumber() {
        return mRfcommChannelNumber;
    }

    public int getProfileVersion() {
        return mProfileVersion;
    }

    public String getServiceName() {
        return mServiceName;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mRfcommChannelNumber);
        dest.writeInt(mProfileVersion);
        BluetoothUtils.writeStringToParcel(dest, mServiceName);
    }

    @Override
    public String toString() {
        String ret = "Bluetooth MAS SDP Record:\n";

        if (mRfcommChannelNumber != -1) {
            ret = ret + "RFCOMM Chan Number: " + mRfcommChannelNumber + "\n";
        }
        if (mServiceName != null) {
            ret = ret + "Service Name: " + mServiceName + "\n";
        }
        if (mProfileVersion != -1) {
            ret = ret + "Profile version: " + mProfileVersion + "\n";
        }
        return ret;
    }

    public static final Parcelable.Creator CREATOR =
            new Parcelable.Creator() {
                public SdpSapsRecord createFromParcel(Parcel in) {
                    return new SdpSapsRecord(in);
                }

                public SdpRecord[] newArray(int size) {
                    return new SdpRecord[size];
                }
            };
}
