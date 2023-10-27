/*
 * Copyright 2021 HIMSA II K/S - www.himsa.com.
 * Represented by EHIMA - www.ehima.com
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
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Represents the Hearing Access Profile preset.
 * @hide
 */
@SystemApi
public final class BluetoothHapPresetInfo implements Parcelable {
    private int mPresetIndex;
    private String mPresetName = "";
    private boolean mIsWritable;
    private boolean mIsAvailable;

    /**
     * HapPresetInfo constructor
     *
     * @param presetIndex Preset index
     * @param presetName Preset Name
     * @param isWritable Is writable flag
     * @param isAvailable Is available flag
     */
    /*package*/ BluetoothHapPresetInfo(int presetIndex, @NonNull String presetName,
            boolean isWritable, boolean isAvailable) {
        this.mPresetIndex = presetIndex;
        this.mPresetName = presetName;
        this.mIsWritable = isWritable;
        this.mIsAvailable = isAvailable;
    }

    /**
     * HapPresetInfo constructor
     *
     * @param in HapPresetInfo parcel
     */
    private BluetoothHapPresetInfo(@NonNull Parcel in) {
        mPresetIndex = in.readInt();
        mPresetName = in.readString();
        mIsWritable = in.readBoolean();
        mIsAvailable = in.readBoolean();
    }

    /**
     * HapPresetInfo preset index
     *
     * @return Preset index
     */
    public int getIndex() {
        return mPresetIndex;
    }

    /**
     * HapPresetInfo preset name
     *
     * @return Preset name
     */
    public @NonNull String getName() {
        return mPresetName;
    }

    /**
     * HapPresetInfo preset writability
     *
     * @return If preset is writable
     */
    public boolean isWritable() {
        return mIsWritable;
    }

    /**
     * HapPresetInfo availability
     *
     * @return If preset is available
     */
    public boolean isAvailable() {
        return mIsAvailable;
    }

    /**
     * HapPresetInfo array creator
     */
    public static final @NonNull Creator<BluetoothHapPresetInfo> CREATOR =
            new Creator<BluetoothHapPresetInfo>() {
                public BluetoothHapPresetInfo createFromParcel(@NonNull Parcel in) {
                    return new BluetoothHapPresetInfo(in);
                }

                public BluetoothHapPresetInfo[] newArray(int size) {
                    return new BluetoothHapPresetInfo[size];
                }
            };

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mPresetIndex);
        dest.writeString(mPresetName);
        dest.writeBoolean(mIsWritable);
        dest.writeBoolean(mIsAvailable);
    }

    /**
     * Builder for {@link BluetoothHapPresetInfo}.
     * <p> By default, the preset index will be set to
     * {@link BluetoothHapClient#PRESET_INDEX_UNAVAILABLE}, the name to an empty string,
     * writability and availability both to false.
     * @hide
     */
    public static final class Builder {
        private int mPresetIndex = BluetoothHapClient.PRESET_INDEX_UNAVAILABLE;
        private String mPresetName = "";
        private boolean mIsWritable = false;
        private boolean mIsAvailable = false;

        /**
         * Creates a new builder.
         *
         * @param index The preset index for HAP preset info
         * @param name The preset name for HAP preset info
         */
        public Builder(int index, @NonNull String name) {
            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException("The size of the preset name for HAP shall be at"
                        + " least one character long.");
            }
            if (index < 0) {
                throw new IllegalArgumentException(
                        "Preset index for HAP shall be a non-negative value.");
            }

            mPresetIndex = index;
            mPresetName = name;
        }

        /**
         * Set preset writability for HAP preset info.
         *
         * @param isWritable whether preset is writable
         * @return the same Builder instance
         */
        public @NonNull Builder setWritable(boolean isWritable) {
            mIsWritable = isWritable;
            return this;
        }

        /**
         * Set preset availability for HAP preset info.
         *
         * @param isAvailable whether preset is currently available to select
         * @return the same Builder instance
         */
        public @NonNull Builder setAvailable(boolean isAvailable) {
            mIsAvailable = isAvailable;
            return this;
        }

        /**
         * Build {@link BluetoothHapPresetInfo}.
         * @return new BluetoothHapPresetInfo built
         */
        public @NonNull BluetoothHapPresetInfo build() {
            return new BluetoothHapPresetInfo(mPresetIndex, mPresetName, mIsWritable, mIsAvailable);
        }
    }
}
