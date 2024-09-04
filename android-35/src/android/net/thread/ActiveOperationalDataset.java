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

package android.net.thread;

import static com.android.internal.util.Preconditions.checkArgument;
import static com.android.internal.util.Preconditions.checkState;
import static com.android.net.module.util.HexDump.toHexString;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.Size;
import android.annotation.SystemApi;
import android.net.IpPrefix;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;

import java.io.ByteArrayOutputStream;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Data interface for managing a Thread Active Operational Dataset.
 *
 * <p>An example usage of creating an Active Operational Dataset with randomized parameters:
 *
 * <pre>{@code
 * ActiveOperationalDataset activeDataset = controller.createRandomizedDataset("MyNet");
 * }</pre>
 *
 * <p>or randomized Dataset with customized channel:
 *
 * <pre>{@code
 * ActiveOperationalDataset activeDataset =
 *         new ActiveOperationalDataset.Builder(controller.createRandomizedDataset("MyNet"))
 *                 .setChannel(CHANNEL_PAGE_24_GHZ, 17)
 *                 .setActiveTimestamp(OperationalDatasetTimestamp.fromInstant(Instant.now()))
 *                 .build();
 * }</pre>
 *
 * <p>If the Active Operational Dataset is already known as <a
 * href="https://www.threadgroup.org">Thread TLVs</a>, you can simply use:
 *
 * <pre>{@code
 * ActiveOperationalDataset activeDataset = ActiveOperationalDataset.fromThreadTlvs(datasetTlvs);
 * }</pre>
 *
 * @hide
 */
@FlaggedApi(ThreadNetworkFlags.FLAG_THREAD_ENABLED)
@SystemApi
public final class ActiveOperationalDataset implements Parcelable {
    /** The maximum length of the Active Operational Dataset TLV array in bytes. */
    public static final int LENGTH_MAX_DATASET_TLVS = 254;

    /** The length of Extended PAN ID in bytes. */
    public static final int LENGTH_EXTENDED_PAN_ID = 8;

    /** The minimum length of Network Name as UTF-8 bytes. */
    public static final int LENGTH_MIN_NETWORK_NAME_BYTES = 1;

    /** The maximum length of Network Name as UTF-8 bytes. */
    public static final int LENGTH_MAX_NETWORK_NAME_BYTES = 16;

    /** The length of Network Key in bytes. */
    public static final int LENGTH_NETWORK_KEY = 16;

    /** The length of Mesh-Local Prefix in bits. */
    public static final int LENGTH_MESH_LOCAL_PREFIX_BITS = 64;

    /** The length of PSKc in bytes. */
    public static final int LENGTH_PSKC = 16;

    /** The 2.4 GHz channel page. */
    public static final int CHANNEL_PAGE_24_GHZ = 0;

    /** The minimum 2.4GHz channel. */
    public static final int CHANNEL_MIN_24_GHZ = 11;

    /** The maximum 2.4GHz channel. */
    public static final int CHANNEL_MAX_24_GHZ = 26;

    /** @hide */
    @VisibleForTesting public static final int TYPE_CHANNEL = 0;

    /** @hide */
    @VisibleForTesting public static final int TYPE_PAN_ID = 1;

    /** @hide */
    @VisibleForTesting public static final int TYPE_EXTENDED_PAN_ID = 2;

    /** @hide */
    @VisibleForTesting public static final int TYPE_NETWORK_NAME = 3;

    /** @hide */
    @VisibleForTesting public static final int TYPE_PSKC = 4;

    /** @hide */
    @VisibleForTesting public static final int TYPE_NETWORK_KEY = 5;

    /** @hide */
    @VisibleForTesting public static final int TYPE_MESH_LOCAL_PREFIX = 7;

    /** @hide */
    @VisibleForTesting public static final int TYPE_SECURITY_POLICY = 12;

    /** @hide */
    @VisibleForTesting public static final int TYPE_ACTIVE_TIMESTAMP = 14;

    /** @hide */
    @VisibleForTesting public static final int TYPE_CHANNEL_MASK = 53;

    /** @hide */
    public static final byte MESH_LOCAL_PREFIX_FIRST_BYTE = (byte) 0xfd;

    private static final int LENGTH_CHANNEL = 3;
    private static final int LENGTH_PAN_ID = 2;

    @NonNull
    public static final Creator<ActiveOperationalDataset> CREATOR =
            new Creator<>() {
                @Override
                public ActiveOperationalDataset createFromParcel(Parcel in) {
                    return ActiveOperationalDataset.fromThreadTlvs(in.createByteArray());
                }

                @Override
                public ActiveOperationalDataset[] newArray(int size) {
                    return new ActiveOperationalDataset[size];
                }
            };

    private final OperationalDatasetTimestamp mActiveTimestamp;
    private final String mNetworkName;
    private final byte[] mExtendedPanId;
    private final int mPanId;
    private final int mChannel;
    private final int mChannelPage;
    private final SparseArray<byte[]> mChannelMask;
    private final byte[] mPskc;
    private final byte[] mNetworkKey;
    private final IpPrefix mMeshLocalPrefix;
    private final SecurityPolicy mSecurityPolicy;
    private final SparseArray<byte[]> mUnknownTlvs;

    private ActiveOperationalDataset(Builder builder) {
        this(
                requireNonNull(builder.mActiveTimestamp),
                requireNonNull(builder.mNetworkName),
                requireNonNull(builder.mExtendedPanId),
                requireNonNull(builder.mPanId),
                requireNonNull(builder.mChannelPage),
                requireNonNull(builder.mChannel),
                requireNonNull(builder.mChannelMask),
                requireNonNull(builder.mPskc),
                requireNonNull(builder.mNetworkKey),
                requireNonNull(builder.mMeshLocalPrefix),
                requireNonNull(builder.mSecurityPolicy),
                requireNonNull(builder.mUnknownTlvs));
    }

    private ActiveOperationalDataset(
            OperationalDatasetTimestamp activeTimestamp,
            String networkName,
            byte[] extendedPanId,
            int panId,
            int channelPage,
            int channel,
            SparseArray<byte[]> channelMask,
            byte[] pskc,
            byte[] networkKey,
            IpPrefix meshLocalPrefix,
            SecurityPolicy securityPolicy,
            SparseArray<byte[]> unknownTlvs) {
        this.mActiveTimestamp = activeTimestamp;
        this.mNetworkName = networkName;
        this.mExtendedPanId = extendedPanId.clone();
        this.mPanId = panId;
        this.mChannel = channel;
        this.mChannelPage = channelPage;
        this.mChannelMask = deepCloneSparseArray(channelMask);
        this.mPskc = pskc.clone();
        this.mNetworkKey = networkKey.clone();
        this.mMeshLocalPrefix = meshLocalPrefix;
        this.mSecurityPolicy = securityPolicy;
        this.mUnknownTlvs = deepCloneSparseArray(unknownTlvs);
    }

    /**
     * Creates a new {@link ActiveOperationalDataset} object from a series of Thread TLVs.
     *
     * <p>{@code tlvs} can be obtained from the value of a Thread Active Operational Dataset TLV
     * (see the <a href="https://www.threadgroup.org/support#specifications">Thread
     * specification</a> for the definition) or the return value of {@link #toThreadTlvs}.
     *
     * @param tlvs a series of Thread TLVs which contain the Active Operational Dataset
     * @return the decoded Active Operational Dataset
     * @throws IllegalArgumentException if {@code tlvs} is malformed or the length is larger than
     *     {@link LENGTH_MAX_DATASET_TLVS}
     */
    @NonNull
    public static ActiveOperationalDataset fromThreadTlvs(@NonNull byte[] tlvs) {
        requireNonNull(tlvs, "tlvs cannot be null");
        if (tlvs.length > LENGTH_MAX_DATASET_TLVS) {
            throw new IllegalArgumentException(
                    String.format(
                            "tlvs length exceeds max length %d (actual is %d)",
                            LENGTH_MAX_DATASET_TLVS, tlvs.length));
        }

        Builder builder = new Builder();
        int i = 0;
        while (i < tlvs.length) {
            int type = tlvs[i++] & 0xff;
            if (i >= tlvs.length) {
                throw new IllegalArgumentException(
                        String.format(
                                "Found TLV type %d at end of operational dataset with length %d",
                                type, tlvs.length));
            }

            int length = tlvs[i++] & 0xff;
            if (i + length > tlvs.length) {
                throw new IllegalArgumentException(
                        String.format(
                                "Found TLV type %d with length %d which exceeds the remaining data"
                                        + " in the operational dataset with length %d",
                                type, length, tlvs.length));
            }

            initWithTlv(builder, type, Arrays.copyOfRange(tlvs, i, i + length));
            i += length;
        }
        try {
            return builder.build();
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(
                    "Failed to build the ActiveOperationalDataset object", e);
        }
    }

    private static void initWithTlv(Builder builder, int type, byte[] value) {
        // The max length of the dataset is 254 bytes, so the max length of a single TLV value is
        // 252 (254 - 1 - 1)
        if (value.length > LENGTH_MAX_DATASET_TLVS - 2) {
            throw new IllegalArgumentException(
                    String.format(
                            "Length of TLV %d exceeds %d (actualLength = %d)",
                            (type & 0xff), LENGTH_MAX_DATASET_TLVS - 2, value.length));
        }

        switch (type) {
            case TYPE_CHANNEL:
                checkArgument(
                        value.length == LENGTH_CHANNEL,
                        "Invalid channel (length = %d, expectedLength = %d)",
                        value.length,
                        LENGTH_CHANNEL);
                builder.setChannel((value[0] & 0xff), ((value[1] & 0xff) << 8) | (value[2] & 0xff));
                break;
            case TYPE_PAN_ID:
                checkArgument(
                        value.length == LENGTH_PAN_ID,
                        "Invalid PAN ID (length = %d, expectedLength = %d)",
                        value.length,
                        LENGTH_PAN_ID);
                builder.setPanId(((value[0] & 0xff) << 8) | (value[1] & 0xff));
                break;
            case TYPE_EXTENDED_PAN_ID:
                builder.setExtendedPanId(value);
                break;
            case TYPE_NETWORK_NAME:
                builder.setNetworkName(new String(value, UTF_8));
                break;
            case TYPE_PSKC:
                builder.setPskc(value);
                break;
            case TYPE_NETWORK_KEY:
                builder.setNetworkKey(value);
                break;
            case TYPE_MESH_LOCAL_PREFIX:
                builder.setMeshLocalPrefix(value);
                break;
            case TYPE_SECURITY_POLICY:
                builder.setSecurityPolicy(SecurityPolicy.fromTlvValue(value));
                break;
            case TYPE_ACTIVE_TIMESTAMP:
                builder.setActiveTimestamp(OperationalDatasetTimestamp.fromTlvValue(value));
                break;
            case TYPE_CHANNEL_MASK:
                builder.setChannelMask(decodeChannelMask(value));
                break;
            default:
                builder.addUnknownTlv(type & 0xff, value);
                break;
        }
    }

    private static SparseArray<byte[]> decodeChannelMask(byte[] tlvValue) {
        SparseArray<byte[]> channelMask = new SparseArray<>();
        int i = 0;
        while (i < tlvValue.length) {
            int channelPage = tlvValue[i++] & 0xff;
            if (i >= tlvValue.length) {
                throw new IllegalArgumentException(
                        "Invalid channel mask - channel mask length is missing");
            }

            int maskLength = tlvValue[i++] & 0xff;
            if (i + maskLength > tlvValue.length) {
                throw new IllegalArgumentException(
                        String.format(
                                "Invalid channel mask - channel mask is incomplete "
                                        + "(offset = %d, length = %d, totalLength = %d)",
                                i, maskLength, tlvValue.length));
            }

            channelMask.put(channelPage, Arrays.copyOfRange(tlvValue, i, i + maskLength));
            i += maskLength;
        }
        return channelMask;
    }

    private static void encodeChannelMask(
            SparseArray<byte[]> channelMask, ByteArrayOutputStream outputStream) {
        ByteArrayOutputStream entryStream = new ByteArrayOutputStream();

        for (int i = 0; i < channelMask.size(); i++) {
            int key = channelMask.keyAt(i);
            byte[] value = channelMask.get(key);
            entryStream.write(key);
            entryStream.write(value.length);
            entryStream.write(value, 0, value.length);
        }

        byte[] entries = entryStream.toByteArray();

        outputStream.write(TYPE_CHANNEL_MASK);
        outputStream.write(entries.length);
        outputStream.write(entries, 0, entries.length);
    }

    private static boolean areByteSparseArraysEqual(
            @NonNull SparseArray<byte[]> first, @NonNull SparseArray<byte[]> second) {
        if (first == second) {
            return true;
        } else if (first == null || second == null) {
            return false;
        } else if (first.size() != second.size()) {
            return false;
        } else {
            for (int i = 0; i < first.size(); i++) {
                int firstKey = first.keyAt(i);
                int secondKey = second.keyAt(i);
                if (firstKey != secondKey) {
                    return false;
                }

                byte[] firstValue = first.valueAt(i);
                byte[] secondValue = second.valueAt(i);
                if (!Arrays.equals(firstValue, secondValue)) {
                    return false;
                }
            }
            return true;
        }
    }

    /** An easy-to-use wrapper of {@link Arrays#deepHashCode}. */
    private static int deepHashCode(Object... values) {
        return Arrays.deepHashCode(values);
    }

    /**
     * Converts this {@link ActiveOperationalDataset} object to a series of Thread TLVs.
     *
     * <p>See the <a href="https://www.threadgroup.org/support#specifications">Thread
     * specification</a> for the definition of the Thread TLV format.
     *
     * @return a series of Thread TLVs which contain this Active Operational Dataset
     */
    @NonNull
    public byte[] toThreadTlvs() {
        ByteArrayOutputStream dataset = new ByteArrayOutputStream();

        dataset.write(TYPE_ACTIVE_TIMESTAMP);
        byte[] activeTimestampBytes = mActiveTimestamp.toTlvValue();
        dataset.write(activeTimestampBytes.length);
        dataset.write(activeTimestampBytes, 0, activeTimestampBytes.length);

        dataset.write(TYPE_NETWORK_NAME);
        byte[] networkNameBytes = mNetworkName.getBytes(UTF_8);
        dataset.write(networkNameBytes.length);
        dataset.write(networkNameBytes, 0, networkNameBytes.length);

        dataset.write(TYPE_EXTENDED_PAN_ID);
        dataset.write(mExtendedPanId.length);
        dataset.write(mExtendedPanId, 0, mExtendedPanId.length);

        dataset.write(TYPE_PAN_ID);
        dataset.write(LENGTH_PAN_ID);
        dataset.write(mPanId >> 8);
        dataset.write(mPanId);

        dataset.write(TYPE_CHANNEL);
        dataset.write(LENGTH_CHANNEL);
        dataset.write(mChannelPage);
        dataset.write(mChannel >> 8);
        dataset.write(mChannel);

        encodeChannelMask(mChannelMask, dataset);

        dataset.write(TYPE_PSKC);
        dataset.write(mPskc.length);
        dataset.write(mPskc, 0, mPskc.length);

        dataset.write(TYPE_NETWORK_KEY);
        dataset.write(mNetworkKey.length);
        dataset.write(mNetworkKey, 0, mNetworkKey.length);

        dataset.write(TYPE_MESH_LOCAL_PREFIX);
        dataset.write(mMeshLocalPrefix.getPrefixLength() / 8);
        dataset.write(mMeshLocalPrefix.getRawAddress(), 0, mMeshLocalPrefix.getPrefixLength() / 8);

        dataset.write(TYPE_SECURITY_POLICY);
        byte[] securityPolicyBytes = mSecurityPolicy.toTlvValue();
        dataset.write(securityPolicyBytes.length);
        dataset.write(securityPolicyBytes, 0, securityPolicyBytes.length);

        for (int i = 0; i < mUnknownTlvs.size(); i++) {
            byte[] value = mUnknownTlvs.valueAt(i);
            dataset.write(mUnknownTlvs.keyAt(i));
            dataset.write(value.length);
            dataset.write(value, 0, value.length);
        }

        return dataset.toByteArray();
    }

    /** Returns the Active Timestamp. */
    @NonNull
    public OperationalDatasetTimestamp getActiveTimestamp() {
        return mActiveTimestamp;
    }

    /** Returns the Network Name. */
    @NonNull
    @Size(min = LENGTH_MIN_NETWORK_NAME_BYTES, max = LENGTH_MAX_NETWORK_NAME_BYTES)
    public String getNetworkName() {
        return mNetworkName;
    }

    /** Returns the Extended PAN ID. */
    @NonNull
    @Size(LENGTH_EXTENDED_PAN_ID)
    public byte[] getExtendedPanId() {
        return mExtendedPanId.clone();
    }

    /** Returns the PAN ID. */
    @IntRange(from = 0, to = 0xfffe)
    public int getPanId() {
        return mPanId;
    }

    /** Returns the Channel. */
    @IntRange(from = 0, to = 65535)
    public int getChannel() {
        return mChannel;
    }

    /** Returns the Channel Page. */
    @IntRange(from = 0, to = 255)
    public int getChannelPage() {
        return mChannelPage;
    }

    /**
     * Returns the Channel masks. For the returned {@link SparseArray}, the key is the Channel Page
     * and the value is the Channel Mask.
     */
    @NonNull
    @Size(min = 1)
    public SparseArray<byte[]> getChannelMask() {
        return deepCloneSparseArray(mChannelMask);
    }

    private static SparseArray<byte[]> deepCloneSparseArray(SparseArray<byte[]> src) {
        SparseArray<byte[]> dst = new SparseArray<>(src.size());
        for (int i = 0; i < src.size(); i++) {
            dst.put(src.keyAt(i), src.valueAt(i).clone());
        }
        return dst;
    }

    /** Returns the PSKc. */
    @NonNull
    @Size(LENGTH_PSKC)
    public byte[] getPskc() {
        return mPskc.clone();
    }

    /** Returns the Network Key. */
    @NonNull
    @Size(LENGTH_NETWORK_KEY)
    public byte[] getNetworkKey() {
        return mNetworkKey.clone();
    }

    /**
     * Returns the Mesh-local Prefix. The length of the returned prefix is always {@link
     * #LENGTH_MESH_LOCAL_PREFIX_BITS}.
     */
    @NonNull
    public IpPrefix getMeshLocalPrefix() {
        return mMeshLocalPrefix;
    }

    /** Returns the Security Policy. */
    @NonNull
    public SecurityPolicy getSecurityPolicy() {
        return mSecurityPolicy;
    }

    /**
     * Returns Thread TLVs which are not recognized by this device. The returned {@link SparseArray}
     * associates TLV values to their keys.
     *
     * @hide
     */
    @NonNull
    public SparseArray<byte[]> getUnknownTlvs() {
        return deepCloneSparseArray(mUnknownTlvs);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeByteArray(toThreadTlvs());
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (!(other instanceof ActiveOperationalDataset)) {
            return false;
        } else {
            ActiveOperationalDataset otherDataset = (ActiveOperationalDataset) other;
            return mActiveTimestamp.equals(otherDataset.mActiveTimestamp)
                    && mNetworkName.equals(otherDataset.mNetworkName)
                    && Arrays.equals(mExtendedPanId, otherDataset.mExtendedPanId)
                    && mPanId == otherDataset.mPanId
                    && mChannelPage == otherDataset.mChannelPage
                    && mChannel == otherDataset.mChannel
                    && areByteSparseArraysEqual(mChannelMask, otherDataset.mChannelMask)
                    && Arrays.equals(mPskc, otherDataset.mPskc)
                    && Arrays.equals(mNetworkKey, otherDataset.mNetworkKey)
                    && mMeshLocalPrefix.equals(otherDataset.mMeshLocalPrefix)
                    && mSecurityPolicy.equals(otherDataset.mSecurityPolicy)
                    && areByteSparseArraysEqual(mUnknownTlvs, otherDataset.mUnknownTlvs);
        }
    }

    @Override
    public int hashCode() {
        return deepHashCode(
                mActiveTimestamp,
                mNetworkName,
                mExtendedPanId,
                mPanId,
                mChannel,
                mChannelPage,
                mChannelMask,
                mPskc,
                mNetworkKey,
                mMeshLocalPrefix,
                mSecurityPolicy);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{networkName=")
                .append(getNetworkName())
                .append(", extendedPanId=")
                .append(toHexString(getExtendedPanId()))
                .append(", panId=")
                .append(getPanId())
                .append(", channel=")
                .append(getChannel())
                .append(", activeTimestamp=")
                .append(getActiveTimestamp())
                .append("}");
        return sb.toString();
    }

    static String checkNetworkName(@NonNull String networkName) {
        requireNonNull(networkName, "networkName cannot be null");

        int nameLength = networkName.getBytes(UTF_8).length;
        checkArgument(
                nameLength >= LENGTH_MIN_NETWORK_NAME_BYTES
                        && nameLength <= LENGTH_MAX_NETWORK_NAME_BYTES,
                "Invalid network name (length = %d, expectedLengthRange = [%d, %d])",
                nameLength,
                LENGTH_MIN_NETWORK_NAME_BYTES,
                LENGTH_MAX_NETWORK_NAME_BYTES);
        return networkName;
    }

    /** The builder for creating {@link ActiveOperationalDataset} objects. */
    public static final class Builder {
        private OperationalDatasetTimestamp mActiveTimestamp;
        private String mNetworkName;
        private byte[] mExtendedPanId;
        private Integer mPanId;
        private Integer mChannel;
        private Integer mChannelPage;
        private SparseArray<byte[]> mChannelMask;
        private byte[] mPskc;
        private byte[] mNetworkKey;
        private IpPrefix mMeshLocalPrefix;
        private SecurityPolicy mSecurityPolicy;
        private SparseArray<byte[]> mUnknownTlvs;

        /**
         * Creates a {@link Builder} object with values from an {@link ActiveOperationalDataset}
         * object.
         */
        public Builder(@NonNull ActiveOperationalDataset activeOpDataset) {
            requireNonNull(activeOpDataset, "activeOpDataset cannot be null");

            this.mActiveTimestamp = activeOpDataset.mActiveTimestamp;
            this.mNetworkName = activeOpDataset.mNetworkName;
            this.mExtendedPanId = activeOpDataset.mExtendedPanId.clone();
            this.mPanId = activeOpDataset.mPanId;
            this.mChannel = activeOpDataset.mChannel;
            this.mChannelPage = activeOpDataset.mChannelPage;
            this.mChannelMask = deepCloneSparseArray(activeOpDataset.mChannelMask);
            this.mPskc = activeOpDataset.mPskc.clone();
            this.mNetworkKey = activeOpDataset.mNetworkKey.clone();
            this.mMeshLocalPrefix = activeOpDataset.mMeshLocalPrefix;
            this.mSecurityPolicy = activeOpDataset.mSecurityPolicy;
            this.mUnknownTlvs = deepCloneSparseArray(activeOpDataset.mUnknownTlvs);
        }

        /**
         * Creates an empty {@link Builder} object.
         *
         * <p>An empty builder cannot build a new {@link ActiveOperationalDataset} object. The
         * Active Operational Dataset parameters must be set with setters of this builder.
         */
        public Builder() {
            mChannelMask = new SparseArray<>();
            mUnknownTlvs = new SparseArray<>();
        }

        /**
         * Sets the Active Timestamp.
         *
         * @param activeTimestamp Active Timestamp of the Operational Dataset
         */
        @NonNull
        public Builder setActiveTimestamp(@NonNull OperationalDatasetTimestamp activeTimestamp) {
            requireNonNull(activeTimestamp, "activeTimestamp cannot be null");
            this.mActiveTimestamp = activeTimestamp;
            return this;
        }

        /**
         * Sets the Network Name.
         *
         * @param networkName the name of the Thread network
         * @throws IllegalArgumentException if length of the UTF-8 representation of {@code
         *     networkName} isn't in range of [{@link #LENGTH_MIN_NETWORK_NAME_BYTES}, {@link
         *     #LENGTH_MAX_NETWORK_NAME_BYTES}]
         */
        @NonNull
        public Builder setNetworkName(
                @NonNull
                        @Size(
                                min = LENGTH_MIN_NETWORK_NAME_BYTES,
                                max = LENGTH_MAX_NETWORK_NAME_BYTES)
                        String networkName) {
            this.mNetworkName = checkNetworkName(networkName);
            return this;
        }

        /**
         * Sets the Extended PAN ID.
         *
         * <p>Use with caution. A randomized Extended PAN ID should be used for real Thread
         * networks. It's discouraged to call this method to override the default value created by
         * {@link ThreadNetworkController#createRandomizedDataset} in production.
         *
         * @throws IllegalArgumentException if length of {@code extendedPanId} is not {@link
         *     #LENGTH_EXTENDED_PAN_ID}.
         */
        @NonNull
        public Builder setExtendedPanId(
                @NonNull @Size(LENGTH_EXTENDED_PAN_ID) byte[] extendedPanId) {
            requireNonNull(extendedPanId, "extendedPanId cannot be null");
            checkArgument(
                    extendedPanId.length == LENGTH_EXTENDED_PAN_ID,
                    "Invalid extended PAN ID (length = %d, expectedLength = %d)",
                    extendedPanId.length,
                    LENGTH_EXTENDED_PAN_ID);
            this.mExtendedPanId = extendedPanId.clone();
            return this;
        }

        /**
         * Sets the PAN ID.
         *
         * @throws IllegalArgumentException if {@code panId} is not in range of 0x0-0xfffe
         */
        @NonNull
        public Builder setPanId(@IntRange(from = 0, to = 0xfffe) int panId) {
            checkArgument(
                    panId >= 0 && panId <= 0xfffe,
                    "PAN ID exceeds allowed range (panid = %d, allowedRange = [0x0, 0xffff])",
                    panId);
            this.mPanId = panId;
            return this;
        }

        /**
         * Sets the Channel Page and Channel.
         *
         * <p>Channel Pages other than {@link #CHANNEL_PAGE_24_GHZ} are undefined and may lead to
         * unexpected behavior if it's applied to Thread devices.
         *
         * @throws IllegalArgumentException if invalid channel is specified for the {@code
         *     channelPage}
         */
        @NonNull
        public Builder setChannel(
                @IntRange(from = 0, to = 255) int page,
                @IntRange(from = 0, to = 65535) int channel) {
            checkArgument(
                    page >= 0 && page <= 255,
                    "Invalid channel page (page = %d, allowedRange = [0, 255])",
                    page);
            if (page == CHANNEL_PAGE_24_GHZ) {
                checkArgument(
                        channel >= CHANNEL_MIN_24_GHZ && channel <= CHANNEL_MAX_24_GHZ,
                        "Invalid channel %d in page %d (allowedChannelRange = [%d, %d])",
                        channel,
                        page,
                        CHANNEL_MIN_24_GHZ,
                        CHANNEL_MAX_24_GHZ);
            } else {
                checkArgument(
                        channel >= 0 && channel <= 65535,
                        "Invalid channel %d in page %d "
                                + "(channel = %d, allowedChannelRange = [0, 65535])",
                        channel,
                        page,
                        channel);
            }

            this.mChannelPage = page;
            this.mChannel = channel;
            return this;
        }

        /**
         * Sets the Channel Mask.
         *
         * @throws IllegalArgumentException if {@code channelMask} is empty
         */
        @NonNull
        public Builder setChannelMask(@NonNull @Size(min = 1) SparseArray<byte[]> channelMask) {
            requireNonNull(channelMask, "channelMask cannot be null");
            checkArgument(channelMask.size() > 0, "channelMask is empty");
            this.mChannelMask = deepCloneSparseArray(channelMask);
            return this;
        }

        /**
         * Sets the PSKc.
         *
         * <p>Use with caution. A randomly generated PSKc should be used for real Thread networks.
         * It's discouraged to call this method to override the default value created by {@link
         * ThreadNetworkController#createRandomizedDataset} in production.
         *
         * @param pskc the key stretched version of the Commissioning Credential for the network
         * @throws IllegalArgumentException if length of {@code pskc} is not {@link #LENGTH_PSKC}
         */
        @NonNull
        public Builder setPskc(@NonNull @Size(LENGTH_PSKC) byte[] pskc) {
            requireNonNull(pskc, "pskc cannot be null");
            checkArgument(
                    pskc.length == LENGTH_PSKC,
                    "Invalid PSKc length (length = %d, expectedLength = %d)",
                    pskc.length,
                    LENGTH_PSKC);
            this.mPskc = pskc.clone();
            return this;
        }

        /**
         * Sets the Network Key.
         *
         * <p>Use with caution, randomly generated Network Key should be used for real Thread
         * networks. It's discouraged to call this method to override the default value created by
         * {@link ThreadNetworkController#createRandomizedDataset} in production.
         *
         * @param networkKey a 128-bit security key-derivation key for the Thread Network
         * @throws IllegalArgumentException if length of {@code networkKey} is not {@link
         *     #LENGTH_NETWORK_KEY}
         */
        @NonNull
        public Builder setNetworkKey(@NonNull @Size(LENGTH_NETWORK_KEY) byte[] networkKey) {
            requireNonNull(networkKey, "networkKey cannot be null");
            checkArgument(
                    networkKey.length == LENGTH_NETWORK_KEY,
                    "Invalid network key length (length = %d, expectedLength = %d)",
                    networkKey.length,
                    LENGTH_NETWORK_KEY);
            this.mNetworkKey = networkKey.clone();
            return this;
        }

        /**
         * Sets the Mesh-Local Prefix.
         *
         * @param meshLocalPrefix the prefix used for realm-local traffic within the mesh
         * @throws IllegalArgumentException if prefix length of {@code meshLocalPrefix} isn't {@link
         *     #LENGTH_MESH_LOCAL_PREFIX_BITS} or {@code meshLocalPrefix} doesn't start with {@code
         *     0xfd}
         */
        @NonNull
        public Builder setMeshLocalPrefix(@NonNull IpPrefix meshLocalPrefix) {
            requireNonNull(meshLocalPrefix, "meshLocalPrefix cannot be null");
            checkArgument(
                    meshLocalPrefix.getPrefixLength() == LENGTH_MESH_LOCAL_PREFIX_BITS,
                    "Invalid mesh-local prefix length (length = %d, expectedLength = %d)",
                    meshLocalPrefix.getPrefixLength(),
                    LENGTH_MESH_LOCAL_PREFIX_BITS);
            checkArgument(
                    meshLocalPrefix.getRawAddress()[0] == MESH_LOCAL_PREFIX_FIRST_BYTE,
                    "Mesh-local prefix must start with 0xfd: " + meshLocalPrefix);
            this.mMeshLocalPrefix = meshLocalPrefix;
            return this;
        }

        /**
         * Sets the Mesh-Local Prefix.
         *
         * @param meshLocalPrefix the prefix used for realm-local traffic within the mesh
         * @throws IllegalArgumentException if {@code meshLocalPrefix} doesn't start with {@code
         *     0xfd} or has length other than {@code LENGTH_MESH_LOCAL_PREFIX_BITS / 8}
         * @hide
         */
        @NonNull
        public Builder setMeshLocalPrefix(byte[] meshLocalPrefix) {
            final int prefixLength = meshLocalPrefix.length * 8;
            checkArgument(
                    prefixLength == LENGTH_MESH_LOCAL_PREFIX_BITS,
                    "Invalid mesh-local prefix length (length = %d, expectedLength = %d)",
                    prefixLength,
                    LENGTH_MESH_LOCAL_PREFIX_BITS);
            byte[] ip6RawAddress = new byte[16];
            System.arraycopy(meshLocalPrefix, 0, ip6RawAddress, 0, meshLocalPrefix.length);
            try {
                return setMeshLocalPrefix(
                        new IpPrefix(Inet6Address.getByAddress(ip6RawAddress), prefixLength));
            } catch (UnknownHostException e) {
                // Can't happen because numeric address is provided
                throw new AssertionError(e);
            }
        }

        /** Sets the Security Policy. */
        @NonNull
        public Builder setSecurityPolicy(@NonNull SecurityPolicy securityPolicy) {
            requireNonNull(securityPolicy, "securityPolicy cannot be null");
            this.mSecurityPolicy = securityPolicy;
            return this;
        }

        /**
         * Sets additional unknown TLVs.
         *
         * @hide
         */
        @NonNull
        public Builder setUnknownTlvs(@NonNull SparseArray<byte[]> unknownTlvs) {
            requireNonNull(unknownTlvs, "unknownTlvs cannot be null");
            mUnknownTlvs = deepCloneSparseArray(unknownTlvs);
            return this;
        }

        /** Adds one more unknown TLV. @hide */
        @VisibleForTesting
        @NonNull
        public Builder addUnknownTlv(int type, byte[] value) {
            mUnknownTlvs.put(type, value);
            return this;
        }

        /**
         * Creates a new {@link ActiveOperationalDataset} object.
         *
         * @throws IllegalStateException if any of the fields isn't set or the total length exceeds
         *     {@link #LENGTH_MAX_DATASET_TLVS} bytes
         */
        @NonNull
        public ActiveOperationalDataset build() {
            checkState(mActiveTimestamp != null, "Active Timestamp is missing");
            checkState(mNetworkName != null, "Network Name is missing");
            checkState(mExtendedPanId != null, "Extended PAN ID is missing");
            checkState(mPanId != null, "PAN ID is missing");
            checkState(mChannel != null, "Channel is missing");
            checkState(mChannelPage != null, "Channel Page is missing");
            checkState(mChannelMask.size() != 0, "Channel Mask is missing");
            checkState(mPskc != null, "PSKc is missing");
            checkState(mNetworkKey != null, "Network Key is missing");
            checkState(mMeshLocalPrefix != null, "Mesh Local Prefix is missing");
            checkState(mSecurityPolicy != null, "Security Policy is missing");

            int length = getTotalDatasetLength();
            if (length > LENGTH_MAX_DATASET_TLVS) {
                throw new IllegalStateException(
                        String.format(
                                "Total dataset length exceeds max length %d (actual is %d)",
                                LENGTH_MAX_DATASET_TLVS, length));
            }

            return new ActiveOperationalDataset(this);
        }

        private int getTotalDatasetLength() {
            int length =
                    2 * 9 // 9 fields with 1 byte of type and 1 byte of length
                            + OperationalDatasetTimestamp.LENGTH_TIMESTAMP
                            + mNetworkName.getBytes(UTF_8).length
                            + LENGTH_EXTENDED_PAN_ID
                            + LENGTH_PAN_ID
                            + LENGTH_CHANNEL
                            + LENGTH_PSKC
                            + LENGTH_NETWORK_KEY
                            + LENGTH_MESH_LOCAL_PREFIX_BITS / 8
                            + mSecurityPolicy.toTlvValue().length;

            for (int i = 0; i < mChannelMask.size(); i++) {
                length += 2 + mChannelMask.valueAt(i).length;
            }

            // For the type and length bytes of the Channel Mask TLV because the masks are encoded
            // as TLVs in TLV.
            length += 2;

            for (int i = 0; i < mUnknownTlvs.size(); i++) {
                length += 2 + mUnknownTlvs.valueAt(i).length;
            }

            return length;
        }
    }

    /**
     * The Security Policy of Thread Operational Dataset which provides an administrator with a way
     * to enable or disable certain security related behaviors.
     */
    public static final class SecurityPolicy {
        /** The default Rotation Time in hours. */
        public static final int DEFAULT_ROTATION_TIME_HOURS = 672;

        /** The minimum length of Security Policy flags in bytes. */
        public static final int LENGTH_MIN_SECURITY_POLICY_FLAGS = 1;

        /** The length of Rotation Time TLV value in bytes. */
        private static final int LENGTH_SECURITY_POLICY_ROTATION_TIME = 2;

        private final int mRotationTimeHours;
        private final byte[] mFlags;

        /**
         * Creates a new {@link SecurityPolicy} object.
         *
         * @param rotationTimeHours the value for Thread key rotation in hours. Must be in range of
         *     0x1-0xffff.
         * @param flags security policy flags with length of either 1 byte for Thread 1.1 or 2 bytes
         *     for Thread 1.2 or higher.
         * @throws IllegalArgumentException if {@code rotationTimeHours} is not in range of
         *     0x1-0xffff or length of {@code flags} is smaller than {@link
         *     #LENGTH_MIN_SECURITY_POLICY_FLAGS}.
         */
        public SecurityPolicy(
                @IntRange(from = 0x1, to = 0xffff) int rotationTimeHours,
                @NonNull @Size(min = LENGTH_MIN_SECURITY_POLICY_FLAGS) byte[] flags) {
            requireNonNull(flags, "flags cannot be null");
            checkArgument(
                    rotationTimeHours >= 1 && rotationTimeHours <= 0xffff,
                    "Rotation time exceeds allowed range (rotationTimeHours = %d, allowedRange ="
                            + " [0x1, 0xffff])",
                    rotationTimeHours);
            checkArgument(
                    flags.length >= LENGTH_MIN_SECURITY_POLICY_FLAGS,
                    "Invalid security policy flags length (length = %d, minimumLength = %d)",
                    flags.length,
                    LENGTH_MIN_SECURITY_POLICY_FLAGS);
            this.mRotationTimeHours = rotationTimeHours;
            this.mFlags = flags.clone();
        }

        /**
         * Creates a new {@link SecurityPolicy} object from the Security Policy TLV value.
         *
         * @hide
         */
        @VisibleForTesting
        @NonNull
        public static SecurityPolicy fromTlvValue(byte[] encodedSecurityPolicy) {
            checkArgument(
                    encodedSecurityPolicy.length
                            >= LENGTH_SECURITY_POLICY_ROTATION_TIME
                                    + LENGTH_MIN_SECURITY_POLICY_FLAGS,
                    "Invalid Security Policy TLV length (length = %d, minimumLength = %d)",
                    encodedSecurityPolicy.length,
                    LENGTH_SECURITY_POLICY_ROTATION_TIME + LENGTH_MIN_SECURITY_POLICY_FLAGS);

            return new SecurityPolicy(
                    ((encodedSecurityPolicy[0] & 0xff) << 8) | (encodedSecurityPolicy[1] & 0xff),
                    Arrays.copyOfRange(
                            encodedSecurityPolicy,
                            LENGTH_SECURITY_POLICY_ROTATION_TIME,
                            encodedSecurityPolicy.length));
        }

        /**
         * Converts this {@link SecurityPolicy} object to Security Policy TLV value.
         *
         * @hide
         */
        @VisibleForTesting
        @NonNull
        public byte[] toTlvValue() {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            result.write(mRotationTimeHours >> 8);
            result.write(mRotationTimeHours);
            result.write(mFlags, 0, mFlags.length);
            return result.toByteArray();
        }

        /** Returns the Security Policy Rotation Time in hours. */
        @IntRange(from = 0x1, to = 0xffff)
        public int getRotationTimeHours() {
            return mRotationTimeHours;
        }

        /** Returns 1 byte flags for Thread 1.1 or 2 bytes flags for Thread 1.2. */
        @NonNull
        @Size(min = LENGTH_MIN_SECURITY_POLICY_FLAGS)
        public byte[] getFlags() {
            return mFlags.clone();
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            } else if (!(other instanceof SecurityPolicy)) {
                return false;
            } else {
                SecurityPolicy otherSecurityPolicy = (SecurityPolicy) other;
                return mRotationTimeHours == otherSecurityPolicy.mRotationTimeHours
                        && Arrays.equals(mFlags, otherSecurityPolicy.mFlags);
            }
        }

        @Override
        public int hashCode() {
            return deepHashCode(mRotationTimeHours, mFlags);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{rotation=")
                    .append(mRotationTimeHours)
                    .append(", flags=")
                    .append(toHexString(mFlags))
                    .append("}");
            return sb.toString();
        }
    }
}
