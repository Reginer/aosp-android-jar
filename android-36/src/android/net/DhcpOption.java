/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class representing an option in the DHCP protocol.
 *
 * @hide
 */
@SystemApi(client = SystemApi.Client.MODULE_LIBRARIES)
public final class DhcpOption implements Parcelable {
    private final byte mType;
    private final byte[] mValue;

    /**
     * Constructs a DhcpOption object.
     *
     * @param type the type of this option. For more information, see
     *           https://www.iana.org/assignments/bootp-dhcp-parameters/bootp-dhcp-parameters.xhtml.
     * @param value the value of this option. If {@code null}, DHCP packets containing this option
     *              will include the option type in the Parameter Request List. Otherwise, DHCP
     *              packets containing this option will include the option in the options section.
     */
    public DhcpOption(@SuppressLint("NoByteOrShort") byte type, @Nullable byte[] value) {
        mType = type;
        mValue = value;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeByte(mType);
        dest.writeByteArray(mValue);
    }

    /** Implement the Parcelable interface */
    public static final @NonNull Creator<DhcpOption> CREATOR =
            new Creator<DhcpOption>() {
                public DhcpOption createFromParcel(Parcel in) {
                    return new DhcpOption(in.readByte(), in.createByteArray());
                }

                public DhcpOption[] newArray(int size) {
                    return new DhcpOption[size];
                }
            };

    /** Get the type of DHCP option */
    @SuppressLint("NoByteOrShort")
    public byte getType() {
        return mType;
    }

    /** Get the value of DHCP option */
    @Nullable public byte[] getValue() {
        return mValue == null ? null : mValue.clone();
    }
}
