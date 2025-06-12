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

package android.hardware.location;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import libcore.util.HexEncoding;

import java.util.Arrays;

/**
 * @deprecated Use {@link android.hardware.location.NanoAppMessage} instead to send messages with
 *             {@link android.hardware.location.ContextHubClient#sendMessageToNanoApp(
 *             NanoAppMessage)} and receive messages with
 *             {@link android.hardware.location.ContextHubClientCallback#onMessageFromNanoApp(
 *             ContextHubClient, NanoAppMessage)}.
 *
 * @hide
 */
@SystemApi
@Deprecated
public class ContextHubMessage implements Parcelable {
    private static final int DEBUG_LOG_NUM_BYTES = 16;
    private int mType;
    private int mVersion;
    private byte[]mData;

    /**
     * Get the message type
     *
     * @return int - message type
     */
    public int getMsgType() {
        return mType;
    }

    /**
     * get message version
     *
     * @return int - message version
     */
    public int getVersion() {
        return mVersion;
    }

    /**
     * get message data
     *
     * @return byte[] - message data buffer
     */
    public byte[] getData() {
        return Arrays.copyOf(mData, mData.length);
    }

    /**
     * set message type
     *
     * @param msgType - message type
     */
    public void setMsgType(int msgType) {
        mType = msgType;
    }

    /**
     * Set message version
     *
     * @param version - message version
     */
    public void setVersion(int version) {
        mVersion = version;
    }

    /**
     * set message data
     *
     * @param data - message buffer
     */
    public void setMsgData(byte[] data) {
        mData = Arrays.copyOf(data, data.length);
    }

    /**
     * Constructor for a context hub message
     *
     * @param msgType - message type
     * @param version - version
     * @param data    - message buffer
     */
    public ContextHubMessage(int msgType, int version, byte[] data) {
        mType = msgType;
        mVersion = version;
        mData = Arrays.copyOf(data, data.length);
    }

    public int describeContents() {
        return 0;
    }

    private ContextHubMessage(Parcel in) {
        mType = in.readInt();
        mVersion = in.readInt();
        int bufferLength = in.readInt();
        mData = new byte[bufferLength];
        in.readByteArray(mData);
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mType);
        out.writeInt(mVersion);
        out.writeInt(mData.length);
        out.writeByteArray(mData);
    }

    public static final @NonNull Parcelable.Creator<ContextHubMessage> CREATOR
            = new Parcelable.Creator<ContextHubMessage>() {
        public ContextHubMessage createFromParcel(Parcel in) {
            return new ContextHubMessage(in);
        }

        public ContextHubMessage[] newArray(int size) {
            return new ContextHubMessage[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        int length = mData.length;

        String ret =
                "ContextHubMessage[type = " + mType + ", length = " + mData.length + " bytes](";
        if (length > 0) {
            ret += "data = 0x";
        }
        for (int i = 0; i < Math.min(length, DEBUG_LOG_NUM_BYTES); i++) {
            ret += HexEncoding.encodeToString(mData[i], true /* upperCase */);

            if ((i + 1) % 4 == 0) {
                ret += " ";
            }
        }
        if (length > DEBUG_LOG_NUM_BYTES) {
            ret += "...";
        }
        ret += ")";

        return ret;
    }
}
