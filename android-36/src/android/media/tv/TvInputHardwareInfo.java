/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.media.tv;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.hardware.tv.input.V1_0.Constants;
import android.media.AudioManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.lang.annotation.Retention;

/**
 * Simple container for information about TV input hardware.
 * Not for third-party developers.
 *
 * @hide
 */
@SystemApi
public final class TvInputHardwareInfo implements Parcelable {
    static final String TAG = "TvInputHardwareInfo";

    // Match hardware/libhardware/include/hardware/tv_input.h
    public static final int TV_INPUT_TYPE_OTHER_HARDWARE = Constants.TV_INPUT_TYPE_OTHER;
    public static final int TV_INPUT_TYPE_TUNER          = Constants.TV_INPUT_TYPE_TUNER;
    public static final int TV_INPUT_TYPE_COMPOSITE      = Constants.TV_INPUT_TYPE_COMPOSITE;
    public static final int TV_INPUT_TYPE_SVIDEO         = Constants.TV_INPUT_TYPE_SVIDEO;
    public static final int TV_INPUT_TYPE_SCART          = Constants.TV_INPUT_TYPE_SCART;
    public static final int TV_INPUT_TYPE_COMPONENT      = Constants.TV_INPUT_TYPE_COMPONENT;
    public static final int TV_INPUT_TYPE_VGA            = Constants.TV_INPUT_TYPE_VGA;
    public static final int TV_INPUT_TYPE_DVI            = Constants.TV_INPUT_TYPE_DVI;
    public static final int TV_INPUT_TYPE_HDMI           = Constants.TV_INPUT_TYPE_HDMI;
    public static final int TV_INPUT_TYPE_DISPLAY_PORT   = Constants.TV_INPUT_TYPE_DISPLAY_PORT;

    /** @hide */
    @Retention(SOURCE)
    @IntDef({CABLE_CONNECTION_STATUS_UNKNOWN, CABLE_CONNECTION_STATUS_CONNECTED,
        CABLE_CONNECTION_STATUS_DISCONNECTED})
    public @interface CableConnectionStatus {}

    // Match hardware/interfaces/tv/input/1.0/types.hal
    /**
     * The hardware is unsure about the connection status or does not support cable detection.
     */
    public static final int CABLE_CONNECTION_STATUS_UNKNOWN =
            Constants.CABLE_CONNECTION_STATUS_UNKNOWN;

    /**
     * Cable is connected to the hardware.
     */
    public static final int CABLE_CONNECTION_STATUS_CONNECTED =
            Constants.CABLE_CONNECTION_STATUS_CONNECTED;

    /**
     * Cable is disconnected to the hardware.
     */
    public static final int CABLE_CONNECTION_STATUS_DISCONNECTED =
            Constants.CABLE_CONNECTION_STATUS_DISCONNECTED;

    public static final @android.annotation.NonNull Parcelable.Creator<TvInputHardwareInfo> CREATOR =
            new Parcelable.Creator<TvInputHardwareInfo>() {
        @Override
        public TvInputHardwareInfo createFromParcel(Parcel source) {
            try {
                TvInputHardwareInfo info = new TvInputHardwareInfo();
                info.readFromParcel(source);
                return info;
            } catch (Exception e) {
                Log.e(TAG, "Exception creating TvInputHardwareInfo from parcel", e);
                return null;
            }
        }

        @Override
        public TvInputHardwareInfo[] newArray(int size) {
            return new TvInputHardwareInfo[size];
        }
    };

    private int mDeviceId;
    private int mType;
    private int mAudioType;
    private String mAudioAddress;
    private int mHdmiPortId;
    @CableConnectionStatus
    private int mCableConnectionStatus;

    private TvInputHardwareInfo() {
    }

    public int getDeviceId() {
        return mDeviceId;
    }

    public int getType() {
        return mType;
    }

    public int getAudioType() {
        return mAudioType;
    }

    public String getAudioAddress() {
        return mAudioAddress;
    }

    public int getHdmiPortId() {
        if (mType != TV_INPUT_TYPE_HDMI) {
            throw new IllegalStateException();
        }
        return mHdmiPortId;
    }

    /**
     * Gets the cable connection status of the hardware.
     *
     * @return {@code CABLE_CONNECTION_STATUS_CONNECTED} if cable is connected.
     *         {@code CABLE_CONNECTION_STATUS_DISCONNECTED} if cable is disconnected.
     *         {@code CABLE_CONNECTION_STATUS_UNKNOWN} if the hardware is unsure about the
     *         connection status or does not support cable detection.
     */
    @CableConnectionStatus
    public int getCableConnectionStatus() {
        return mCableConnectionStatus;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(128);
        b.append("TvInputHardwareInfo {id=").append(mDeviceId);
        b.append(", type=").append(mType);
        b.append(", audio_type=").append(mAudioType);
        b.append(", audio_addr=").append(mAudioAddress);
        if (mType == TV_INPUT_TYPE_HDMI) {
            b.append(", hdmi_port=").append(mHdmiPortId);
        }
        b.append(", cable_connection_status=").append(mCableConnectionStatus);
        b.append("}");
        return b.toString();
    }

    // Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mDeviceId);
        dest.writeInt(mType);
        dest.writeInt(mAudioType);
        dest.writeString(mAudioAddress);
        if (mType == TV_INPUT_TYPE_HDMI) {
            dest.writeInt(mHdmiPortId);
        }
        dest.writeInt(mCableConnectionStatus);
    }

    public void readFromParcel(Parcel source) {
        mDeviceId = source.readInt();
        mType = source.readInt();
        mAudioType = source.readInt();
        mAudioAddress = source.readString();
        if (mType == TV_INPUT_TYPE_HDMI) {
            mHdmiPortId = source.readInt();
        }
        mCableConnectionStatus = source.readInt();
    }

    /** @hide */
    public Builder toBuilder() {
        Builder newBuilder = new Builder()
            .deviceId(mDeviceId)
            .type(mType)
            .audioType(mAudioType)
            .audioAddress(mAudioAddress)
            .cableConnectionStatus(mCableConnectionStatus);
        if (mType == TV_INPUT_TYPE_HDMI) {
            newBuilder.hdmiPortId(mHdmiPortId);
        }
        return newBuilder;
    }

    public static final class Builder {
        private Integer mDeviceId = null;
        private Integer mType = null;
        private int mAudioType = AudioManager.DEVICE_NONE;
        private String mAudioAddress = "";
        private Integer mHdmiPortId = null;
        private Integer mCableConnectionStatus = CABLE_CONNECTION_STATUS_UNKNOWN;

        public Builder() {
        }

        public Builder deviceId(int deviceId) {
            mDeviceId = deviceId;
            return this;
        }

        public Builder type(int type) {
            mType = type;
            return this;
        }

        public Builder audioType(int audioType) {
            mAudioType = audioType;
            return this;
        }

        public Builder audioAddress(String audioAddress) {
            mAudioAddress = audioAddress;
            return this;
        }

        public Builder hdmiPortId(int hdmiPortId) {
            mHdmiPortId = hdmiPortId;
            return this;
        }

        /**
         * Sets cable connection status.
         */
        public Builder cableConnectionStatus(@CableConnectionStatus int cableConnectionStatus) {
            mCableConnectionStatus = cableConnectionStatus;
            return this;
        }

        public TvInputHardwareInfo build() {
            if (mDeviceId == null || mType == null) {
                throw new UnsupportedOperationException();
            }
            if ((mType == TV_INPUT_TYPE_HDMI && mHdmiPortId == null) ||
                    (mType != TV_INPUT_TYPE_HDMI && mHdmiPortId != null)) {
                throw new UnsupportedOperationException();
            }

            TvInputHardwareInfo info = new TvInputHardwareInfo();
            info.mDeviceId = mDeviceId;
            info.mType = mType;
            info.mAudioType = mAudioType;
            if (info.mAudioType != AudioManager.DEVICE_NONE) {
                info.mAudioAddress = mAudioAddress;
            }
            if (mHdmiPortId != null) {
                info.mHdmiPortId = mHdmiPortId;
            }
            info.mCableConnectionStatus = mCableConnectionStatus;
            return info;
        }
    }
}
