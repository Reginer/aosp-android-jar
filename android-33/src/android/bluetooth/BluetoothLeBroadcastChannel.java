/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * This class contains the Broadcast Isochronous Channel level information as defined in the BASE
 * structure of the Basic Audio Profile.
 *
 * @hide
 */
@SystemApi
public final class BluetoothLeBroadcastChannel implements Parcelable {
    private static final int UNKNOWN_VALUE_PLACEHOLDER = -1;

    private final boolean mIsSelected;
    private final int mChannelIndex;
    private final BluetoothLeAudioCodecConfigMetadata mCodecMetadata;

    private BluetoothLeBroadcastChannel(boolean isSelected, int channelIndex,
            BluetoothLeAudioCodecConfigMetadata codecMetadata) {
        mIsSelected = isSelected;
        mChannelIndex = channelIndex;
        mCodecMetadata = codecMetadata;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof BluetoothLeBroadcastChannel)) {
            return false;
        }
        final BluetoothLeBroadcastChannel other = (BluetoothLeBroadcastChannel) o;
        return mIsSelected == other.isSelected()
                && mChannelIndex == other.getChannelIndex()
                && mCodecMetadata.equals(other.getCodecMetadata());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mIsSelected, mChannelIndex, mCodecMetadata);
    }

    /**
     * Return true if the channel is selected by Broadcast Assistant for the Broadcast Sink.
     *
     * Used by Broadcast Assistant and Sink, but not Broadcast Source
     *
     * @return true if the channel is selected by Broadcast Assistant for the Broadcast Sink
     * @hide
     */
    @SystemApi
    public boolean isSelected() {
        return mIsSelected;
    }

    /**
     * Get the Broadcast Isochronous Channel index of this Broadcast Channel.
     *
     * @return Broadcast Isochronous Channel index
     * @hide
     */
    @SystemApi
    public int getChannelIndex() {
        return mChannelIndex;
    }

    /**
     * Return the codec specific configuration for this Broadcast Channel.
     *
     * @return codec specific configuration for this Broadcast Channel
     * @hide
     */
    @SystemApi
    public @NonNull BluetoothLeAudioCodecConfigMetadata getCodecMetadata() {
        return mCodecMetadata;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * @hide
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeBoolean(mIsSelected);
        out.writeInt(mChannelIndex);
        out.writeTypedObject(mCodecMetadata, 0);
    }

    /**
     * A {@link Parcelable.Creator} to create {@link BluetoothLeBroadcastChannel} from parcel.
     * @hide
     */
    @SystemApi
    public static final @NonNull Parcelable.Creator<BluetoothLeBroadcastChannel> CREATOR =
            new Parcelable.Creator<BluetoothLeBroadcastChannel>() {
                public @NonNull BluetoothLeBroadcastChannel createFromParcel(@NonNull Parcel in) {
                    BluetoothLeBroadcastChannel.Builder
                            builder = new BluetoothLeBroadcastChannel.Builder();
                    builder.setSelected(in.readBoolean());
                    builder.setChannelIndex(in.readInt());
                    builder.setCodecMetadata(
                            in.readTypedObject(BluetoothLeAudioCodecConfigMetadata.CREATOR));
                    return builder.build();
                }

                public @NonNull BluetoothLeBroadcastChannel[] newArray(int size) {
                    return new BluetoothLeBroadcastChannel[size];
                }
            };

    /**
     * Builder for {@link BluetoothLeBroadcastChannel}.
     * @hide
     */
    @SystemApi
    public static final class Builder {
        private boolean mIsSelected = false;
        private int mChannelIndex = UNKNOWN_VALUE_PLACEHOLDER;
        private BluetoothLeAudioCodecConfigMetadata mCodecMetadata = null;

        /**
         * Create an empty builder.
         * @hide
         */
        @SystemApi
        public Builder() {}

        /**
         * Create a builder with copies of information from original object.
         *
         * @param original original object
         * @hide
         */
        @SystemApi
        public Builder(@NonNull BluetoothLeBroadcastChannel original) {
            mIsSelected = original.isSelected();
            mChannelIndex = original.getChannelIndex();
            mCodecMetadata = original.getCodecMetadata();
        }

        /**
         * Set if the channel is selected by Broadcast Assistant for the Broadcast Sink.
         *
         * Used by Broadcast Assistant and Sink, but not Broadcast Source
         *
         * @param isSelected true if the channel is selected by Broadcast Assistant for the
         *                   Broadcast Sink
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setSelected(boolean isSelected) {
            mIsSelected = isSelected;
            return this;
        }

        /**
         * Set the Broadcast Isochronous Channel index of this Broadcast Channel.
         *
         * @param channelIndex Broadcast Isochronous Channel index
         * @throws IllegalArgumentException if the input argument is not valid
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setChannelIndex(int channelIndex) {
            if (channelIndex == UNKNOWN_VALUE_PLACEHOLDER) {
                throw new IllegalArgumentException("channelIndex cannot be "
                        + UNKNOWN_VALUE_PLACEHOLDER);
            }
            mChannelIndex = channelIndex;
            return this;
        }

        /**
         * Set the codec specific configuration for this Broadcast Channel.
         *
         * @param codecMetadata codec specific configuration for this Broadcast Channel
         * @throws NullPointerException if codecMetadata is null
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setCodecMetadata(
                @NonNull BluetoothLeAudioCodecConfigMetadata codecMetadata) {
            Objects.requireNonNull(codecMetadata, "codecMetadata cannot be null");
            mCodecMetadata = codecMetadata;
            return this;
        }

        /**
         * Build {@link BluetoothLeBroadcastChannel}.
         *
         * @return constructed {@link BluetoothLeBroadcastChannel}
         * @throws NullPointerException if {@link NonNull} items are null
         * @throws IllegalArgumentException if the object cannot be built
         * @hide
         */
        @SystemApi
        public @NonNull BluetoothLeBroadcastChannel build() {
            Objects.requireNonNull(mCodecMetadata, "codec metadata cannot be null");
            if (mChannelIndex == UNKNOWN_VALUE_PLACEHOLDER) {
                throw new IllegalArgumentException("mChannelIndex cannot be "
                        + UNKNOWN_VALUE_PLACEHOLDER);
            }
            return new BluetoothLeBroadcastChannel(mIsSelected, mChannelIndex, mCodecMetadata);
        }
    }
}
