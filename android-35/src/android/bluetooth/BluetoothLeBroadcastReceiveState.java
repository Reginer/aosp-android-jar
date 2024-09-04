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

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The {@link BluetoothLeBroadcastReceiveState} is used by the BASS server to expose information
 * about a Broadcast Source.
 *
 * <p>It represents the current synchronization state of the server to a PA and/or a BIG containing
 * one or more subgroups containing one or more BISes transmitted by that Broadcast Source. The
 * Broadcast Receive State characteristic is also used to inform clients whether the server has
 * detected that the BIS is encrypted, whether the server requires a Broadcast_Code, and whether the
 * server is decrypting the BIS.
 *
 * @hide
 */
@SystemApi
public final class BluetoothLeBroadcastReceiveState implements Parcelable {
    /**
     * Periodic Advertising Synchronization state.
     *
     * <p>Periodic Advertising (PA) enables the LE Audio Broadcast Assistant to discover broadcast
     * audio streams as well as the audio stream configuration on behalf of an LE Audio Broadcast
     * Sink. This information can then be transferred to the LE Audio Broadcast Sink using the
     * Periodic Advertising Synchronization Transfer (PAST) procedure.
     *
     * @hide
     */
    @IntDef(
            prefix = "PA_SYNC_STATE_",
            value = {
                PA_SYNC_STATE_IDLE,
                PA_SYNC_STATE_SYNCINFO_REQUEST,
                PA_SYNC_STATE_SYNCHRONIZED,
                PA_SYNC_STATE_FAILED_TO_SYNCHRONIZE,
                PA_SYNC_STATE_NO_PAST
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PaSyncState {}

    /**
     * Indicates that the Broadcast Sink is not synchronized with the Periodic Advertisements (PA)
     *
     * @hide
     */
    @SystemApi public static final int PA_SYNC_STATE_IDLE = 0;

    /**
     * Indicates that the Broadcast Sink requested the Broadcast Assistant to synchronize with the
     * Periodic Advertisements (PA).
     *
     * <p>This is also known as scan delegation or scan offloading.
     *
     * @hide
     */
    @SystemApi public static final int PA_SYNC_STATE_SYNCINFO_REQUEST = 1;

    /**
     * Indicates that the Broadcast Sink is synchronized with the Periodic Advertisements (PA).
     *
     * @hide
     */
    @SystemApi public static final int PA_SYNC_STATE_SYNCHRONIZED = 2;

    /**
     * Indicates that the Broadcast Sink was unable to synchronize with the Periodic Advertisements
     * (PA).
     *
     * @hide
     */
    @SystemApi public static final int PA_SYNC_STATE_FAILED_TO_SYNCHRONIZE = 3;

    /**
     * Indicates that the Broadcast Sink should be synchronized with the Periodic Advertisements
     * (PA) using the Periodic Advertisements Synchronization Transfer (PAST) procedure.
     *
     * @hide
     */
    @SystemApi public static final int PA_SYNC_STATE_NO_PAST = 4;

    /**
     * Indicates that the Broadcast Sink synchronization state is invalid.
     *
     * @hide
     */
    public static final int PA_SYNC_STATE_INVALID = 0xFFFF;

    /** @hide */
    @IntDef(
            prefix = "BIG_ENCRYPTION_STATE_",
            value = {
                BIG_ENCRYPTION_STATE_NOT_ENCRYPTED,
                BIG_ENCRYPTION_STATE_CODE_REQUIRED,
                BIG_ENCRYPTION_STATE_DECRYPTING,
                BIG_ENCRYPTION_STATE_BAD_CODE
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface BigEncryptionState {}

    /**
     * Indicates that the Broadcast Sink is synchronized with an unencrypted audio stream from a
     * Broadcast Source
     *
     * @hide
     */
    @SystemApi public static final int BIG_ENCRYPTION_STATE_NOT_ENCRYPTED = 0;

    /**
     * Indicates that the Broadcast Sink needs a Broadcast Code to synchronize with an audio stream
     * from a Broadcast Source, which was not provided when the audio stream from the Broadcast
     * Source was added.
     *
     * @hide
     */
    @SystemApi public static final int BIG_ENCRYPTION_STATE_CODE_REQUIRED = 1;

    /**
     * Indicates that the Broadcast Sink is synchronized with an encrypted audio stream from a
     * Broadcast Source.
     *
     * @hide
     */
    @SystemApi public static final int BIG_ENCRYPTION_STATE_DECRYPTING = 2;

    /**
     * Indicates that the Broadcast Sink is unable to decrypt an audio stream from a Broadcast
     * Source due to an incorrect Broadcast Code.
     *
     * @hide
     */
    @SystemApi public static final int BIG_ENCRYPTION_STATE_BAD_CODE = 3;

    /**
     * Indicates that the Broadcast Sink encryption state is invalid.
     *
     * @hide
     */
    public static final int BIG_ENCRYPTION_STATE_INVALID = 0xFFFF;

    private final int mSourceId;
    private final @BluetoothDevice.AddressType int mSourceAddressType;
    private final BluetoothDevice mSourceDevice;
    private final int mSourceAdvertisingSid;
    private final int mBroadcastId;
    private final @PaSyncState int mPaSyncState;
    private final @BigEncryptionState int mBigEncryptionState;
    private final byte[] mBadCode;
    private final int mNumSubgroups;
    private final List<Long> mBisSyncState;
    private final List<BluetoothLeAudioContentMetadata> mSubgroupMetadata;

    private static String paSyncStateToString(int paSyncState) {
        switch (paSyncState) {
            case 0x00:
                return "Not synchronized to PA: [" + Integer.toString(paSyncState) + "]";
            case 0x01:
                return "SyncInfo Request: [" + Integer.toString(paSyncState) + "]";
            case 0x02:
                return "Synchronized to PA: [" + Integer.toString(paSyncState) + "]";
            case 0x03:
                return "Failed to synchronize to PA: [" + Integer.toString(paSyncState) + "]";
            case 0x04:
                return "No PAST: [" + Integer.toString(paSyncState) + "]";
            default:
                return "RFU: [" + Integer.toString(paSyncState) + "]";
        }
    }

    private static String bigEncryptionStateToString(int bigEncryptionState) {
        switch (bigEncryptionState) {
            case 0x00:
                return "Not encrypted: [" + Integer.toString(bigEncryptionState) + "]";
            case 0x01:
                return "Broadcast_Code required: [" + Integer.toString(bigEncryptionState) + "]";
            case 0x02:
                return "Decrypting: [" + Integer.toString(bigEncryptionState) + "]";
            case 0x03:
                return "Bad_Code (incorrect encryption key): ["
                        + Integer.toString(bigEncryptionState)
                        + "]";
            default:
                return "RFU: [" + Integer.toString(bigEncryptionState) + "]";
        }
    }

    private static String bisSyncStateToString(Long bisSyncState, int bisSyncStateIndex) {
        if (bisSyncState == 0) {
            return "Not synchronized to BIS_index["
                    + Integer.toString(bisSyncStateIndex)
                    + "]: ["
                    + String.valueOf(bisSyncState)
                    + "]";
        } else if (bisSyncState > 0 && bisSyncState < 0xFFFFFFFF) {
            return "Synchronized to BIS_index["
                    + Integer.toString(bisSyncStateIndex)
                    + "]: ["
                    + String.valueOf(bisSyncState)
                    + "]";
        } else if (bisSyncState == 0xFFFFFFFF) {
            return "Failed to sync to BIG: [" + String.valueOf(bisSyncState) + "]";
        } else {
            return "[" + String.valueOf(bisSyncState) + "]";
        }
    }

    /**
     * Constructor to create a read-only {@link BluetoothLeBroadcastReceiveState} instance.
     *
     * @throws NullPointerException if sourceDevice, bisSyncState, or subgroupMetadata is null
     * @throws IllegalArgumentException if sourceID is not [0, 0xFF] or if sourceAddressType is
     *     invalid or if bisSyncState.size() != numSubgroups or if subgroupMetadata.size() !=
     *     numSubgroups or if paSyncState or bigEncryptionState is not recognized bye IntDef
     * @hide
     */
    public BluetoothLeBroadcastReceiveState(
            @IntRange(from = 0x00, to = 0xFF) int sourceId,
            @BluetoothDevice.AddressType int sourceAddressType,
            @NonNull BluetoothDevice sourceDevice,
            int sourceAdvertisingSid,
            int broadcastId,
            @PaSyncState int paSyncState,
            @BigEncryptionState int bigEncryptionState,
            byte[] badCode,
            @IntRange(from = 0x00) int numSubgroups,
            @NonNull List<Long> bisSyncState,
            @NonNull List<BluetoothLeAudioContentMetadata> subgroupMetadata) {
        if (sourceId < 0x00 || sourceId > 0xFF) {
            throw new IllegalArgumentException(
                    "sourceId " + sourceId + " does not fall between 0x00 and 0xFF");
        }
        Objects.requireNonNull(sourceDevice, "sourceDevice cannot be null");
        if (sourceAddressType == BluetoothDevice.ADDRESS_TYPE_UNKNOWN) {
            throw new IllegalArgumentException("sourceAddressType cannot be ADDRESS_TYPE_UNKNOWN");
        }
        if (sourceAddressType != BluetoothDevice.ADDRESS_TYPE_RANDOM
                && sourceAddressType != BluetoothDevice.ADDRESS_TYPE_PUBLIC) {
            throw new IllegalArgumentException(
                    "sourceAddressType " + sourceAddressType + " is invalid");
        }
        Objects.requireNonNull(bisSyncState, "bisSyncState cannot be null");
        if (bisSyncState.size() != numSubgroups) {
            throw new IllegalArgumentException(
                    "bisSyncState.size() "
                            + bisSyncState.size()
                            + " must be equal to numSubgroups "
                            + numSubgroups);
        }
        Objects.requireNonNull(subgroupMetadata, "subgroupMetadata cannot be null");
        if (subgroupMetadata.size() != numSubgroups) {
            throw new IllegalArgumentException(
                    "subgroupMetadata.size()  "
                            + subgroupMetadata.size()
                            + " must be equal to numSubgroups "
                            + numSubgroups);
        }
        if (paSyncState != PA_SYNC_STATE_IDLE
                && paSyncState != PA_SYNC_STATE_SYNCINFO_REQUEST
                && paSyncState != PA_SYNC_STATE_SYNCHRONIZED
                && paSyncState != PA_SYNC_STATE_FAILED_TO_SYNCHRONIZE
                && paSyncState != PA_SYNC_STATE_NO_PAST
                && paSyncState != PA_SYNC_STATE_INVALID) {
            throw new IllegalArgumentException("unrecognized paSyncState " + paSyncState);
        }
        if (bigEncryptionState != BIG_ENCRYPTION_STATE_NOT_ENCRYPTED
                && bigEncryptionState != BIG_ENCRYPTION_STATE_CODE_REQUIRED
                && bigEncryptionState != BIG_ENCRYPTION_STATE_DECRYPTING
                && bigEncryptionState != BIG_ENCRYPTION_STATE_BAD_CODE
                && bigEncryptionState != BIG_ENCRYPTION_STATE_INVALID) {
            throw new IllegalArgumentException(
                    "unrecognized bigEncryptionState " + bigEncryptionState);
        }
        if (badCode != null && badCode.length != 16) {
            throw new IllegalArgumentException(
                    "badCode must be 16 bytes long of null, but is "
                            + badCode.length
                            + " + bytes long");
        }
        mSourceId = sourceId;
        mSourceAddressType = sourceAddressType;
        mSourceDevice = sourceDevice;
        mSourceAdvertisingSid = sourceAdvertisingSid;
        mBroadcastId = broadcastId;
        mPaSyncState = paSyncState;
        mBigEncryptionState = bigEncryptionState;
        mBadCode = badCode;
        mNumSubgroups = numSubgroups;
        mBisSyncState = bisSyncState;
        mSubgroupMetadata = subgroupMetadata;
    }

    /**
     * Get the source ID assigned by the BASS server
     *
     * <p>Shall be unique for each instance of the Broadcast Receive State characteristic exposed by
     * the server
     *
     * @return source ID assigned by the BASS server
     * @hide
     */
    @SystemApi
    public @IntRange(from = 0x00, to = 0xFF) int getSourceId() {
        return mSourceId;
    }

    /**
     * Get the address type of the Broadcast Source
     *
     * Can be either {@link BluetoothDevice#ADDRESS_TYPE_PUBLIC} or
     * {@link BluetoothDevice#ADDRESS_TYPE_RANDOM
     *
     * @return address type of the Broadcast Source
     * @hide
     */
    @SystemApi
    public @BluetoothDevice.AddressType int getSourceAddressType() {
        return mSourceAddressType;
    }

    /**
     * Get the MAC address of the Broadcast Source, which can be Public Device Address, Random
     * Device Address, Public Identity Address or Random (static) Identity Address
     *
     * @return MAC address of the Broadcast Source
     * @hide
     */
    @SystemApi
    public @NonNull BluetoothDevice getSourceDevice() {
        return mSourceDevice;
    }

    /**
     * Get Advertising_SID subfield of the ADI field of the AUX_ADV_IND PDU or the
     * LL_PERIODIC_SYNC_IND containing the SyncInfo that points to the PA transmitted by the
     * Broadcast Source.
     *
     * @return 1-byte long Advertising_SID of the Broadcast Source
     * @hide
     */
    @SystemApi
    public int getSourceAdvertisingSid() {
        return mSourceAdvertisingSid;
    }

    /**
     * Broadcast_ID of the Broadcast Source
     *
     * @return 3-byte long Broadcast_ID of the Broadcast Source
     * @hide
     */
    @SystemApi
    public int getBroadcastId() {
        return mBroadcastId;
    }

    /**
     * Get the Periodic Advertisement synchronization state between the Broadcast Sink and the
     * Broadcast source
     *
     * <p>Possible values are {@link #PA_SYNC_STATE_IDLE}, {@link #PA_SYNC_STATE_SYNCINFO_REQUEST},
     * {@link #PA_SYNC_STATE_SYNCHRONIZED}, {@link #PA_SYNC_STATE_FAILED_TO_SYNCHRONIZE}, {@link
     * #PA_SYNC_STATE_NO_PAST}
     *
     * @return Periodic Advertisement synchronization state
     * @hide
     */
    @SystemApi
    public @PaSyncState int getPaSyncState() {
        return mPaSyncState;
    }

    /**
     * Get the encryption state of a Broadcast Isochronous Group (BIG)
     *
     * <p>Possible values are {@link #BIG_ENCRYPTION_STATE_NOT_ENCRYPTED}, {@link
     * #BIG_ENCRYPTION_STATE_CODE_REQUIRED}, {@link #BIG_ENCRYPTION_STATE_DECRYPTING}, {@link
     * #BIG_ENCRYPTION_STATE_DECRYPTING}, and {@link #BIG_ENCRYPTION_STATE_BAD_CODE}
     *
     * @return encryption state of a Broadcast Isochronous Group (BIG)
     * @hide
     */
    @SystemApi
    public @BigEncryptionState int getBigEncryptionState() {
        return mBigEncryptionState;
    }

    /**
     * If {@link #getBigEncryptionState()} returns {@link #BIG_ENCRYPTION_STATE_BAD_CODE}, this
     * method returns the value of the incorrect 16-octet Broadcast Code that fails to decrypt an
     * audio stream from a Broadcast Source.
     *
     * @return 16-octet Broadcast Code, or null if {@link #getBigEncryptionState()} does not return
     *     {@link #BIG_ENCRYPTION_STATE_BAD_CODE}
     * @hide
     */
    @SystemApi
    public @Nullable byte[] getBadCode() {
        return mBadCode;
    }

    /**
     * Get number of Broadcast subgroups being added to this sink
     *
     * @return number of Broadcast subgroups being added to this sink
     */
    public int getNumSubgroups() {
        return mNumSubgroups;
    }

    /**
     * Get a list of bitfield on whether a Broadcast Isochronous Stream (BIS) is synchronized
     * between the sink and source
     *
     * <p>The number of items in the returned list is the same as {@link #getNumSubgroups()}. For
     * each subgroup, at most 31 BISes are available and their synchronization state is indicated by
     * its bit value at the particular offset (i.e. Bit 0-30 = BIS_index[1-31])
     *
     * <p>For example, if (BisSyncState & 0b1 << 5) != 0, BIS 5 is synchronized between source and
     * sync
     *
     * <p>There is a special case, 0xFFFFFFFF to indicate Broadcast Sink failed to synchronize to a
     * particular subgroup
     *
     * @return a list of bitfield on whether a Broadcast Isochronous Stream (BIS) is synchronized
     *     between the sink and source
     * @hide
     */
    @SystemApi
    public @NonNull List<Long> getBisSyncState() {
        return mBisSyncState;
    }

    /**
     * Get metadata for every subgroup added to this Broadcast Sink
     *
     * <p>The number of items in the returned list is the same as {@link #getNumSubgroups()}.
     *
     * @return metadata for every subgroup added to this Broadcast Sink
     * @hide
     */
    @SystemApi
    public @NonNull List<BluetoothLeAudioContentMetadata> getSubgroupMetadata() {
        return mSubgroupMetadata;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mSourceId);
        out.writeInt(mSourceAddressType);
        out.writeTypedObject(mSourceDevice, 0);
        out.writeInt(mSourceAdvertisingSid);
        out.writeInt(mBroadcastId);
        out.writeInt(mPaSyncState);
        out.writeInt(mBigEncryptionState);

        if (mBadCode != null) {
            out.writeInt(mBadCode.length);
            out.writeByteArray(mBadCode);
        } else {
            // -1 indicates that there is no "bad broadcast code"
            out.writeInt(-1);
        }
        out.writeInt(mNumSubgroups);
        out.writeList(mBisSyncState);
        out.writeTypedList(mSubgroupMetadata);
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    public String toString() {
        String receiveState =
                ("Receiver state: "
                        + "\n  Source ID:"
                        + mSourceId
                        + "\n  Source Address Type:"
                        + (int) mSourceAddressType
                        + "\n  Source Address:"
                        + mSourceDevice.toString()
                        + "\n  Source Adv SID:"
                        + mSourceAdvertisingSid
                        + "\n  Broadcast ID:"
                        + mBroadcastId
                        + "\n  PA Sync State:"
                        + paSyncStateToString(mPaSyncState)
                        + "\n  BIG Encryption Status:"
                        + bigEncryptionStateToString(mBigEncryptionState)
                        + "\n  Bad Broadcast Code:"
                        + Arrays.toString(mBadCode)
                        + "\n  Number Of Subgroups:"
                        + mNumSubgroups);
        for (int i = 0; i < mNumSubgroups; i++) {
            receiveState +=
                    ("\n    Subgroup index:"
                                    + i
                                    + "\n      BIS Sync State:"
                                    + bisSyncStateToString(mBisSyncState.get(i), i))
                            + "\n      Metadata:"
                            + "\n        ProgramInfo:"
                            + mSubgroupMetadata.get(i).getProgramInfo()
                            + "\n        Language:"
                            + mSubgroupMetadata.get(i).getLanguage()
                            + "\n        RawData:"
                            + Arrays.toString(mSubgroupMetadata.get(i).getRawMetadata());
        }
        return receiveState;
    }

    /**
     * A {@link Parcelable.Creator} to create {@link BluetoothLeBroadcastReceiveState} from parcel.
     *
     * @hide
     */
    @SystemApi @NonNull
    public static final Creator<BluetoothLeBroadcastReceiveState> CREATOR =
            new Creator<>() {
                public @NonNull BluetoothLeBroadcastReceiveState createFromParcel(
                        @NonNull Parcel in) {
                    final int sourceId = in.readInt();
                    final int sourceAddressType = in.readInt();
                    final BluetoothDevice sourceDevice =
                            in.readTypedObject(BluetoothDevice.CREATOR);
                    final int sourceAdvertisingSid = in.readInt();
                    final int broadcastId = in.readInt();
                    final int paSyncState = in.readInt();
                    final int bigEncryptionState = in.readInt();
                    final int badCodeLen = in.readInt();
                    byte[] badCode = null;

                    if (badCodeLen != -1) {
                        badCode = new byte[badCodeLen];
                        if (badCodeLen > 0) {
                            in.readByteArray(badCode);
                        }
                    }
                    final byte numSubGroups = in.readByte();
                    final List<Long> bisSyncState =
                            in.readArrayList(Long.class.getClassLoader(), Long.class);
                    final List<BluetoothLeAudioContentMetadata> subgroupMetadata =
                            new ArrayList<>();
                    in.readTypedList(subgroupMetadata, BluetoothLeAudioContentMetadata.CREATOR);

                    return new BluetoothLeBroadcastReceiveState(
                            sourceId,
                            sourceAddressType,
                            sourceDevice,
                            sourceAdvertisingSid,
                            broadcastId,
                            paSyncState,
                            bigEncryptionState,
                            badCode,
                            numSubGroups,
                            bisSyncState,
                            subgroupMetadata);
                }

                public @NonNull BluetoothLeBroadcastReceiveState[] newArray(int size) {
                    return new BluetoothLeBroadcastReceiveState[size];
                }
            };
}
