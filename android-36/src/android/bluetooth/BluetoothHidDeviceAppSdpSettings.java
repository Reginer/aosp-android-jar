/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.EventLog;

/**
 * Represents the Service Discovery Protocol (SDP) settings for a Bluetooth HID Device application.
 *
 * <p>The BluetoothHidDevice framework adds the SDP record during app registration, so that the
 * Android device can be discovered as a Bluetooth HID Device.
 *
 * @see BluetoothHidDevice
 */
public final class BluetoothHidDeviceAppSdpSettings implements Parcelable {

    private static final int MAX_DESCRIPTOR_SIZE = 2048;

    private final String mName;
    private final String mDescription;
    private final String mProvider;
    private final byte mSubclass;
    private final byte[] mDescriptors;

    /**
     * Create a BluetoothHidDeviceAppSdpSettings object for the Bluetooth SDP record.
     *
     * @param name Name of this Bluetooth HID device. Maximum length is 50 bytes.
     * @param description Description for this Bluetooth HID device. Maximum length is 50 bytes.
     * @param provider Provider of this Bluetooth HID device. Maximum length is 50 bytes.
     * @param subclass Subclass of this Bluetooth HID device. See <a
     *     href="www.usb.org/developers/hidpage/HID1_11.pdf">
     *     www.usb.org/developers/hidpage/HID1_11.pdf Section 4.2</a>
     * @param descriptors Descriptors of this Bluetooth HID device. See <a
     *     href="www.usb.org/developers/hidpage/HID1_11.pdf">
     *     www.usb.org/developers/hidpage/HID1_11.pdf Chapter 6</a> Maximum length is 2048 bytes.
     */
    public BluetoothHidDeviceAppSdpSettings(
            String name, String description, String provider, byte subclass, byte[] descriptors) {
        mName = name;
        mDescription = description;
        mProvider = provider;
        mSubclass = subclass;

        if (descriptors == null || descriptors.length > MAX_DESCRIPTOR_SIZE) {
            EventLog.writeEvent(0x534e4554, "119819889", -1, "");
            throw new IllegalArgumentException(
                    "descriptors must be not null and shorter than " + MAX_DESCRIPTOR_SIZE);
        }
        mDescriptors = descriptors.clone();
    }

    public String getName() {
        return mName;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getProvider() {
        return mProvider;
    }

    public byte getSubclass() {
        return mSubclass;
    }

    public byte[] getDescriptors() {
        return mDescriptors;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<BluetoothHidDeviceAppSdpSettings> CREATOR =
            new Creator<>() {
                @Override
                public BluetoothHidDeviceAppSdpSettings createFromParcel(Parcel in) {
                    return new BluetoothHidDeviceAppSdpSettings(
                            in.readString(),
                            in.readString(),
                            in.readString(),
                            in.readByte(),
                            in.createByteArray());
                }

                @Override
                public BluetoothHidDeviceAppSdpSettings[] newArray(int size) {
                    return new BluetoothHidDeviceAppSdpSettings[size];
                }
            };

    @Override
    public void writeToParcel(Parcel out, int flags) {
        BluetoothUtils.writeStringToParcel(out, mName);
        BluetoothUtils.writeStringToParcel(out, mDescription);
        BluetoothUtils.writeStringToParcel(out, mProvider);
        out.writeByte(mSubclass);
        out.writeByteArray(mDescriptors);
    }
}
