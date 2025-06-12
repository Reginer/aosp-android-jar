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

package android.ranging.uwb;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.ranging.flags.Flags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import java.util.Random;

/**
 * A Class representing the complex channel for UWB which comprises channel and preamble index
 * negotiated between peer devices out of band before ranging.
 *
 * <p> PRF (Pulse Repetition Frequency) supported:
 * <ul>
 *     <li> BPRF - Base Pulse Repetition Frequency.</li>
 *     <li> HPRF - Higher Pulse Repetition Frequency.</li>
 * </ul>
 *
 * See <a href="https://groups.firaconsortium.org/wg/members/document/1949> FiRa UCI Spec.</a>
 */
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
public final class UwbComplexChannel implements Parcelable {

    /**
     * UWB Channel selections
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE_USE})
    @IntDef(
            value = {
                    UWB_CHANNEL_5,
                    UWB_CHANNEL_6,
                    UWB_CHANNEL_8,
                    UWB_CHANNEL_9,
                    UWB_CHANNEL_10,
                    UWB_CHANNEL_12,
                    UWB_CHANNEL_13,
                    UWB_CHANNEL_14,
            })
    public @interface UwbChannel {
    }

    /** UWB channel 5 */
    public static final int UWB_CHANNEL_5 = 5;
    /** UWB channel 6 */
    public static final int UWB_CHANNEL_6 = 6;
    /** UWB channel 8 */
    public static final int UWB_CHANNEL_8 = 8;
    /** UWB channel 9 */
    public static final int UWB_CHANNEL_9 = 9;
    /** UWB channel 10 */
    public static final int UWB_CHANNEL_10 = 10;
    /** UWB channel 12 */
    public static final int UWB_CHANNEL_12 = 12;
    /** UWB channel 13 */
    public static final int UWB_CHANNEL_13 = 13;
    /** UWB channel 14 */
    public static final int UWB_CHANNEL_14 = 14;

    /**
     * UWB preamble selections
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE_USE})
    @IntDef(
            value = {
                    UWB_PREAMBLE_CODE_INDEX_9,
                    UWB_PREAMBLE_CODE_INDEX_10,
                    UWB_PREAMBLE_CODE_INDEX_11,
                    UWB_PREAMBLE_CODE_INDEX_12,
                    UWB_PREAMBLE_CODE_INDEX_25,
                    UWB_PREAMBLE_CODE_INDEX_26,
                    UWB_PREAMBLE_CODE_INDEX_27,
                    UWB_PREAMBLE_CODE_INDEX_28,
                    UWB_PREAMBLE_CODE_INDEX_29,
                    UWB_PREAMBLE_CODE_INDEX_30,
                    UWB_PREAMBLE_CODE_INDEX_31,
                    UWB_PREAMBLE_CODE_INDEX_32,
            })
    public @interface UwbPreambleCodeIndex {
    }

    /** UWB preamble code index 9 (PRF mode - BPRF). */
    public static final int UWB_PREAMBLE_CODE_INDEX_9 = 9;
    /** UWB preamble code index 10 (PRF mode - BPRF). */
    public static final int UWB_PREAMBLE_CODE_INDEX_10 = 10;
    /** UWB preamble code index 11 (PRF mode - BPRF). */
    public static final int UWB_PREAMBLE_CODE_INDEX_11 = 11;
    /** UWB preamble code index 12 (PRF mode - BPRF). */
    public static final int UWB_PREAMBLE_CODE_INDEX_12 = 12;
    /** UWB preamble code index 25 (PRF mode - HPRF). */
    public static final int UWB_PREAMBLE_CODE_INDEX_25 = 25;
    /** UWB preamble code index 26 (PRF mode - HPRF). */
    public static final int UWB_PREAMBLE_CODE_INDEX_26 = 26;
    /** UWB preamble code index 27 (PRF mode - HPRF). */
    public static final int UWB_PREAMBLE_CODE_INDEX_27 = 27;
    /** UWB preamble code index 28 (PRF mode - HPRF). */
    public static final int UWB_PREAMBLE_CODE_INDEX_28 = 28;
    /** UWB preamble code index 29 (PRF mode - HPRF). */
    public static final int UWB_PREAMBLE_CODE_INDEX_29 = 29;
    /** UWB preamble code index 30 (PRF mode - HPRF). */
    public static final int UWB_PREAMBLE_CODE_INDEX_30 = 30;
    /** UWB preamble code index 31 (PRF mode - HPRF). */
    public static final int UWB_PREAMBLE_CODE_INDEX_31 = 31;
    /** UWB preamble code index 32 (PRF mode - HPRF). */
    public static final int UWB_PREAMBLE_CODE_INDEX_32 = 32;

    private static final int[] PREAMBLE_INDEXES_BPRF =
            new int[]{UWB_PREAMBLE_CODE_INDEX_9,
                    UWB_PREAMBLE_CODE_INDEX_10,
                    UWB_PREAMBLE_CODE_INDEX_11,
                    UWB_PREAMBLE_CODE_INDEX_12};
    @UwbChannel
    private final int mChannel;
    @UwbPreambleCodeIndex
    private final int mPreambleIndex;

    private UwbComplexChannel(Builder builder) {
        mChannel = builder.mChannel;
        mPreambleIndex = builder.mPreambleIndex;
    }

    private UwbComplexChannel(@NonNull Parcel in) {
        mChannel = in.readInt();
        mPreambleIndex = in.readInt();
    }

    @NonNull
    public static final Creator<UwbComplexChannel> CREATOR = new Creator<UwbComplexChannel>() {
        @Override
        public UwbComplexChannel createFromParcel(Parcel in) {
            return new UwbComplexChannel(in);
        }

        @Override
        public UwbComplexChannel[] newArray(int size) {
            return new UwbComplexChannel[size];
        }
    };

    /**
     * Gets the UWB channel associated with this configuration.
     *
     * @return The channel number, which is one of the predefined UWB channels:
     */
    @UwbChannel
    public int getChannel() {
        return mChannel;
    }

    /**
     * Gets the UWB preamble index associated with this configuration.
     *
     * @return The preamble index, which is one of the predefined UWB preamble indices:
     *
     * See <a href="https://groups.firaconsortium.org/wg/members/document/1949> FiRa UCI Spec.</a>
     */

    @UwbPreambleCodeIndex
    public int getPreambleIndex() {
        return mPreambleIndex;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mChannel);
        dest.writeInt(mPreambleIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UwbComplexChannel other)) return false;
        return mChannel == other.mChannel && mPreambleIndex == other.mPreambleIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mChannel, mPreambleIndex);
    }

    /**
     * Builder for creating instances of {@link UwbComplexChannel}.
     */
    public static final class Builder {
        @UwbChannel
        private int mChannel = UWB_CHANNEL_5;
        @UwbPreambleCodeIndex
        private int mPreambleIndex = PREAMBLE_INDEXES_BPRF[new Random().nextInt(
                PREAMBLE_INDEXES_BPRF.length)];

        /**
         * Sets the channel for the ranging device.
         * <p> Defaults to {@link #UWB_CHANNEL_5}
         *
         * @param channel The channel number to be set.
         * @return This {@link Builder} instance.
         */
        @NonNull
        public Builder setChannel(@UwbChannel int channel) {
            mChannel = channel;
            return this;
        }

        /**
         * Sets the preamble index for the ranging device as defined in
         * See <a href="https://groups.firaconsortium.org/wg/members/document/1949> FiRa UCI
         * Spec.</a>}
         * <p> PRF (Pulse Repetition Frequency) is selected based on the preamble index set here.
         *
         * <p> Defaults to a random BPRF preamble index.
         * One among {@link #UWB_PREAMBLE_CODE_INDEX_9}, {@link #UWB_PREAMBLE_CODE_INDEX_10},
         * {@link #UWB_PREAMBLE_CODE_INDEX_11} or {@link #UWB_PREAMBLE_CODE_INDEX_12}.
         * For better performance always use a random preamble index for each ranging session.
         *
         * @param preambleIndex The preamble index to be set.
         * @return This {@link Builder} instance.
         */
        @NonNull
        public Builder setPreambleIndex(@UwbPreambleCodeIndex int preambleIndex) {
            mPreambleIndex = preambleIndex;
            return this;
        }

        /**
         * Builds and returns a new instance of {@link UwbComplexChannel}.
         *
         * @return A new {@link UwbComplexChannel} instance.
         */
        @NonNull
        public UwbComplexChannel build() {
            return new UwbComplexChannel(this);
        }
    }

    @Override
    public String toString() {
        return "UwbComplexChannel{ mChannel="
                + mChannel
                + ", mPreambleIndex="
                + mPreambleIndex
                + " }";
    }
}
