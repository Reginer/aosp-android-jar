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

package android.bluetooth;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.bluetooth.flags.Flags;

/**
 * Represents a supported source codec type for a Bluetooth A2DP device. See {@link
 * BluetoothA2dp#getSupportedCodecTypes}. The codec type is uniquely identified by its name and
 * codec identifier.
 */
public final class BluetoothCodecType implements Parcelable {
    private final int mNativeCodecType;
    private final long mCodecId;
    private final @NonNull String mCodecName;

    private BluetoothCodecType(Parcel in) {
        mNativeCodecType = in.readInt();
        mCodecId = in.readLong() & 0xFFFFFFFFL;
        mCodecName = in.readString();
    }

    /** SBC codec identifier. See {@link BluetoothCodecType#getCodecId}. */
    public static final long CODEC_ID_SBC = 0x0000000000;

    /** AAC codec identifier. See {@link BluetoothCodecType#getCodecId}. */
    public static final long CODEC_ID_AAC = 0x0000000002;

    /** AptX codec identifier. See {@link BluetoothCodecType#getCodecId}. */
    public static final long CODEC_ID_APTX = 0x0001004fff;

    /** Aptx HD codec identifier. See {@link BluetoothCodecType#getCodecId}. */
    public static final long CODEC_ID_APTX_HD = 0x002400d7ff;

    /** LDAC codec identifier. See {@link BluetoothCodecType#getCodecId}. */
    public static final long CODEC_ID_LDAC = 0x00aa012dff;

    /** Opus codec identifier. See {@link BluetoothCodecType#getCodecId}. */
    public static final long CODEC_ID_OPUS = 0x000100e0ff;

    /** LHDC codec identifier. See {@link BluetoothCodecType#getCodecId}. */
    @FlaggedApi(Flags.FLAG_A2DP_LHDC_API)
    public static final long CODEC_ID_LHDCV5 = 0x4c35_053a_ffL;

    /**
     * Create the bluetooth codec type from the static codec type index.
     *
     * @param codecType the static codec type
     * @param codecId the unique codec id
     */
    private BluetoothCodecType(@BluetoothCodecConfig.SourceCodecType int codecType, long codecId) {
        mNativeCodecType = codecType;
        mCodecId = codecId & 0xFFFFFFFFL;
        mCodecName = BluetoothCodecConfig.getCodecName(codecType);
    }

    /**
     * Create the bluetooth codec type from the static codec type index.
     *
     * @param codecType the static codec type
     * @param codecId the unique codec id
     * @param codecName the codec name
     * @hide
     */
    @SystemApi
    public BluetoothCodecType(int codecType, long codecId, @NonNull String codecName) {
        mNativeCodecType = codecType;
        mCodecId = codecId & 0xFFFFFFFFL;
        mCodecName = codecName;
    }

    /** Returns if the codec type is mandatory in the Bluetooth specification. */
    public boolean isMandatoryCodec() {
        return mNativeCodecType == BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC;
    }

    /**
     * Returns the codec unique identifier.
     *
     * <p>The codec identifier is 40 bits:
     *
     * <ul>
     *   <li>Bits 0-7: Audio Codec ID, as defined by [ID 6.5.1]
     *       <ul>
     *         <li>0x00: SBC
     *         <li>0x02: AAC
     *         <li>0xFF: Vendor
     *       </ul>
     *   <li>Bits 8-23: Company ID, set to 0, if octet 0 is not 0xFF.
     *   <li>Bits 24-39: Vendor-defined codec ID, set to 0, if octet 0 is not 0xFF.
     * </ul>
     */
    public long getCodecId() {
        return mCodecId;
    }

    /** Returns the codec name. */
    public @NonNull String getCodecName() {
        return mCodecName;
    }

    /**
     * Returns the native codec type. The native codec type is arbitrarily assigned to the codec.
     * Prefer {@link BluetoothCodecType#getCodecId}.
     *
     * @hide
     */
    public int getNativeCodecType() {
        return mNativeCodecType;
    }

    @Override
    public String toString() {
        return mCodecName;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(mCodecId);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof BluetoothCodecType) {
            BluetoothCodecType other = (BluetoothCodecType) o;
            return other.mCodecId == mCodecId;
        }
        return false;
    }

    /** @hide */
    public static @NonNull BluetoothCodecType createFromParcel(Parcel in) {
        return new BluetoothCodecType(in);
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mNativeCodecType);
        dest.writeLong(mCodecId);
        BluetoothUtils.writeStringToParcel(dest, mCodecName);
    }

    /**
     * Create the bluetooth codec type from the static codec type index.
     *
     * @param codecType the static codec type
     * @return the codec type if valid
     * @hide
     */
    @SystemApi
    public static @Nullable BluetoothCodecType createFromType(
            @BluetoothCodecConfig.SourceCodecType int codecType) {
        long codecId =
                switch (codecType) {
                    case BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC -> CODEC_ID_SBC;
                    case BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC -> CODEC_ID_AAC;
                    case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX -> CODEC_ID_APTX;
                    case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD -> CODEC_ID_APTX_HD;
                    case BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC -> CODEC_ID_LDAC;
                    case BluetoothCodecConfig.SOURCE_CODEC_TYPE_OPUS -> CODEC_ID_OPUS;
                    case BluetoothCodecConfig.SOURCE_CODEC_TYPE_LC3,
                                    BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID ->
                            -1;
                    default -> -1;
                };
        if (codecId == -1) {
            return null;
        }
        return new BluetoothCodecType(codecType, codecId);
    }

    /**
     * @return 0
     * @hide
     */
    @Override
    public int describeContents() {
        return 0;
    }

    public static final @NonNull Creator<BluetoothCodecType> CREATOR =
            new Creator<>() {
                public BluetoothCodecType createFromParcel(Parcel in) {
                    return new BluetoothCodecType(in);
                }

                public BluetoothCodecType[] newArray(int size) {
                    return new BluetoothCodecType[size];
                }
            };
}
