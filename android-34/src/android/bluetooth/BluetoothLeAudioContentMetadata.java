/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.bluetooth.BluetoothUtils.TypeValueEntry;
import android.os.Parcel;
import android.os.Parcelable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A class representing the media metadata information defined in the Basic Audio Profile.
 *
 * @hide
 */
@SystemApi
public final class BluetoothLeAudioContentMetadata implements Parcelable {
    // From Generic Audio assigned numbers
    private static final int PROGRAM_INFO_TYPE = 0x03;
    private static final int LANGUAGE_TYPE = 0x04;
    private static final int LANGUAGE_LENGTH = 0x03;

    private final String mProgramInfo;
    private final String mLanguage;
    private final byte[] mRawMetadata;

    private BluetoothLeAudioContentMetadata(String programInfo, String language,
            byte[] rawMetadata) {
        mProgramInfo = programInfo;
        mLanguage = language;
        mRawMetadata = rawMetadata;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof BluetoothLeAudioContentMetadata)) {
            return false;
        }
        final BluetoothLeAudioContentMetadata other = (BluetoothLeAudioContentMetadata) o;
        return Objects.equals(mProgramInfo, other.getProgramInfo())
                && Objects.equals(mLanguage, other.getLanguage())
                && Arrays.equals(mRawMetadata, other.getRawMetadata());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mProgramInfo, mLanguage, Arrays.hashCode(mRawMetadata));
    }

    /**
     * Get the title and/or summary of Audio Stream content in UTF-8 format.
     *
     * @return title and/or summary of Audio Stream content in UTF-8 format, null if this metadata
     * does not exist
     * @hide
     */
    @SystemApi
    public @Nullable String getProgramInfo() {
        return mProgramInfo;
    }

    /**
     * Get language of the audio stream in 3-byte, lower case language code as defined in ISO 639-3.
     *
     * @return ISO 639-3 formatted language code, null if this metadata does not exist
     * @hide
     */
    @SystemApi
    public @Nullable String getLanguage() {
        return mLanguage;
    }

    /**
     * Get the raw bytes of stream metadata in Bluetooth LTV format as defined in the Generic Audio
     * section of <a href="https://www.bluetooth.com/specifications/assigned-numbers/">Bluetooth Assigned Numbers</a>,
     * including metadata that was not covered by the getter methods in this class
     *
     * @return raw bytes of stream metadata in Bluetooth LTV format
     */
    public @NonNull byte[] getRawMetadata() {
        return mRawMetadata;
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
        out.writeString(mProgramInfo);
        out.writeString(mLanguage);
        out.writeInt(mRawMetadata.length);
        out.writeByteArray(mRawMetadata);
    }

    /**
     * A {@link Parcelable.Creator} to create {@link BluetoothLeAudioContentMetadata} from parcel.
     * @hide
     */
    @SystemApi
    @NonNull
    public static final Creator<BluetoothLeAudioContentMetadata> CREATOR = new Creator<>() {
        public @NonNull BluetoothLeAudioContentMetadata createFromParcel(@NonNull Parcel in) {
            final String programInfo = in.readString();
            final String language = in.readString();
            final int rawMetadataLength = in.readInt();
            byte[] rawMetadata = new byte[rawMetadataLength];
            in.readByteArray(rawMetadata);
            return new BluetoothLeAudioContentMetadata(programInfo, language, rawMetadata);
        }

        public @NonNull BluetoothLeAudioContentMetadata[] newArray(int size) {
            return new BluetoothLeAudioContentMetadata[size];
        }
    };

    /**
     * Construct a {@link BluetoothLeAudioContentMetadata} from raw bytes.
     *
     * The byte array will be parsed and values for each getter will be populated
     *
     * Raw metadata cannot be set using builder in order to maintain raw bytes and getter value
     * consistency
     *
     * @param rawBytes raw bytes of stream metadata in Bluetooth LTV format
     * @return parsed {@link BluetoothLeAudioContentMetadata} object
     * @throws IllegalArgumentException if <var>rawBytes</var> is null or when the raw bytes cannot
     * be parsed to build the object
     * @hide
     */
    @SystemApi
    public static @NonNull BluetoothLeAudioContentMetadata fromRawBytes(@NonNull byte[] rawBytes) {
        if (rawBytes == null) {
            throw new IllegalArgumentException("Raw bytes cannot be null");
        }
        List<TypeValueEntry> entries = BluetoothUtils.parseLengthTypeValueBytes(rawBytes);
        if (rawBytes.length > 0 && rawBytes[0] > 0 && entries.isEmpty()) {
            throw new IllegalArgumentException("No LTV entries are found from rawBytes of size "
                    + rawBytes.length);
        }
        String programInfo = null;
        String language = null;
        for (TypeValueEntry entry : entries) {
            // Only use the first value of each type
            if (programInfo == null && entry.getType() == PROGRAM_INFO_TYPE) {
                byte[] bytes = entry.getValue();
                programInfo = new String(bytes, StandardCharsets.UTF_8);
            } else if (language == null && entry.getType() == LANGUAGE_TYPE) {
                byte[] bytes = entry.getValue();
                if (bytes.length != LANGUAGE_LENGTH) {
                    throw new IllegalArgumentException("Language byte size " + bytes.length
                            + " is less than " + LANGUAGE_LENGTH + ", needed for ISO 639-3");
                }
                // Parse 3 bytes ISO 639-3 only
                language = new String(bytes, 0, LANGUAGE_LENGTH, StandardCharsets.US_ASCII);
            }
        }
        return new BluetoothLeAudioContentMetadata(programInfo, language, rawBytes);
    }

    /**
     * Builder for {@link BluetoothLeAudioContentMetadata}.
     * @hide
     */
    @SystemApi
    public static final class Builder {
        private String mProgramInfo = null;
        private String mLanguage = null;
        private byte[] mRawMetadata = null;

        /**
         * Create an empty builder
         *
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
        public Builder(@NonNull BluetoothLeAudioContentMetadata original) {
            mProgramInfo = original.getProgramInfo();
            mLanguage = original.getLanguage();
            mRawMetadata = original.getRawMetadata();
        }

        /**
         * Set the title and/or summary of Audio Stream content in UTF-8 format.
         *
         * @param programInfo  title and/or summary of Audio Stream content in UTF-8 format, null
         *                     if this metadata does not exist
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setProgramInfo(@Nullable String programInfo) {
            mProgramInfo = programInfo;
            return this;
        }

        /**
         * Set language of the audio stream in 3-byte, lower case language code as defined in
         * ISO 639-3.
         *
         * @return this builder
         * @hide
         */
        @SystemApi
        public @NonNull Builder setLanguage(@Nullable String language) {
            mLanguage = language;
            return this;
        }

        /**
         * Build {@link BluetoothLeAudioContentMetadata}.
         *
         * @return constructed {@link BluetoothLeAudioContentMetadata}
         * @throws IllegalArgumentException if the object cannot be built
         * @hide
         */
        @SystemApi
        public @NonNull BluetoothLeAudioContentMetadata build() {
            List<TypeValueEntry> entries = new ArrayList<>();
            if (mRawMetadata != null) {
                entries = BluetoothUtils.parseLengthTypeValueBytes(mRawMetadata);
                if (mRawMetadata.length > 0 && mRawMetadata[0] > 0 && entries.isEmpty()) {
                    throw new IllegalArgumentException("No LTV entries are found from rawBytes of"
                            + " size " + mRawMetadata.length + " please check the original object"
                            + " passed to Builder's copy constructor");
                }
            }
            if (mProgramInfo != null) {
                entries.removeIf(entry -> entry.getType() == PROGRAM_INFO_TYPE);
                entries.add(new TypeValueEntry(PROGRAM_INFO_TYPE,
                        mProgramInfo.getBytes(StandardCharsets.UTF_8)));
            }
            if (mLanguage != null) {
                String cleanedLanguage = mLanguage.toLowerCase().strip();
                byte[] languageBytes = cleanedLanguage.getBytes(StandardCharsets.US_ASCII);
                if (languageBytes.length != LANGUAGE_LENGTH) {
                    throw new IllegalArgumentException("Language byte size " + languageBytes.length
                            + " is less than " + LANGUAGE_LENGTH + ", needed ISO 639-3, to build");
                }
                entries.removeIf(entry -> entry.getType() == LANGUAGE_TYPE);
                entries.add(new TypeValueEntry(LANGUAGE_TYPE, languageBytes));
            }
            byte[] rawBytes = BluetoothUtils.serializeTypeValue(entries);
            if (rawBytes == null) {
                throw new IllegalArgumentException("Failed to serialize entries to bytes");
            }
            return new BluetoothLeAudioContentMetadata(mProgramInfo, mLanguage, rawBytes);
        }
    }
}
