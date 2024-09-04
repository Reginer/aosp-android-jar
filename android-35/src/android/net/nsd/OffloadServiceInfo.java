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

package android.net.nsd;

import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresApi;
import android.annotation.SystemApi;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.net.module.util.HexDump;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The OffloadServiceInfo class contains all the necessary information the OffloadEngine needs to
 * know about how to offload an mDns service. The OffloadServiceInfo is keyed on
 * {@link OffloadServiceInfo.Key} which is a (serviceName, serviceType) pair.
 *
 * @hide
 */
@FlaggedApi("com.android.net.flags.register_nsd_offload_engine_api")
@SystemApi
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public final class OffloadServiceInfo implements Parcelable {
    @NonNull
    private final Key mKey;
    @NonNull
    private final String mHostname;
    @NonNull final List<String> mSubtypes;
    @Nullable
    private final byte[] mOffloadPayload;
    private final int mPriority;
    private final long mOffloadType;

    /**
     * Creates a new OffloadServiceInfo object with the specified parameters.
     *
     * @param key The key of the service.
     * @param subtypes The list of subTypes of the service.
     * @param hostname The name of the host offering the service. It is meaningful only when
     *                 offloadType contains OFFLOAD_REPLY.
     * @param offloadPayload The raw udp payload for hardware offloading.
     * @param priority The priority of the service, @see #getPriority.
     * @param offloadType The type of the service offload, @see #getOffloadType.
     */
    public OffloadServiceInfo(@NonNull Key key,
            @NonNull List<String> subtypes, @NonNull String hostname,
            @Nullable byte[] offloadPayload,
            @IntRange(from = 0, to = Integer.MAX_VALUE) int priority,
            @OffloadEngine.OffloadType long offloadType) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(subtypes);
        Objects.requireNonNull(hostname);
        mKey = key;
        mSubtypes = subtypes;
        mHostname = hostname;
        mOffloadPayload = offloadPayload;
        mPriority = priority;
        mOffloadType = offloadType;
    }

    /**
     * Creates a new OffloadServiceInfo object from a Parcel.
     *
     * @param in The Parcel to read the object from.
     *
     * @hide
     */
    public OffloadServiceInfo(@NonNull Parcel in) {
        mKey = in.readParcelable(Key.class.getClassLoader(),
                Key.class);
        mSubtypes = in.createStringArrayList();
        mHostname = in.readString();
        mOffloadPayload = in.createByteArray();
        mPriority = in.readInt();
        mOffloadType = in.readLong();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mKey, flags);
        dest.writeStringList(mSubtypes);
        dest.writeString(mHostname);
        dest.writeByteArray(mOffloadPayload);
        dest.writeInt(mPriority);
        dest.writeLong(mOffloadType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<OffloadServiceInfo> CREATOR = new Creator<OffloadServiceInfo>() {
        @Override
        public OffloadServiceInfo createFromParcel(Parcel in) {
            return new OffloadServiceInfo(in);
        }

        @Override
        public OffloadServiceInfo[] newArray(int size) {
            return new OffloadServiceInfo[size];
        }
    };

    /**
     * Get the {@link Key}.
     */
    @NonNull
    public Key getKey() {
        return mKey;
    }

    /**
     * Get the host name. (e.g. "Android.local" )
     */
    @NonNull
    public String getHostname() {
        return mHostname;
    }

    /**
     * Get the service subtypes. (e.g. ["_ann"] )
     */
    @NonNull
    public List<String> getSubtypes() {
        return Collections.unmodifiableList(mSubtypes);
    }

    /**
     * Get the raw udp payload that the OffloadEngine can use to directly reply the incoming query.
     * <p>
     * It is null if the OffloadEngine can not handle transmit. The packet must be sent as-is when
     * replying to query.
     */
    @Nullable
    public byte[] getOffloadPayload() {
        if (mOffloadPayload == null) {
            return null;
        } else {
            return mOffloadPayload.clone();
        }
    }

    /**
     * Create a new OffloadServiceInfo with payload updated.
     *
     * @hide
     */
    @NonNull
    public OffloadServiceInfo withOffloadPayload(@NonNull byte[] offloadPayload) {
        return new OffloadServiceInfo(
                this.getKey(),
                this.getSubtypes(),
                this.getHostname(),
                offloadPayload,
                this.getPriority(),
                this.getOffloadType()
        );
    }

    /**
     * Get the offloadType.
     * <p>
     * For example, if the {@link com.android.server.NsdService} requests the OffloadEngine to both
     * filter the mDNS queries and replies, the {@link #mOffloadType} =
     * ({@link OffloadEngine#OFFLOAD_TYPE_FILTER_QUERIES} |
     * {@link OffloadEngine#OFFLOAD_TYPE_FILTER_REPLIES}).
     */
    @OffloadEngine.OffloadType public long getOffloadType() {
        return mOffloadType;
    }

    /**
     * Get the priority for the OffloadServiceInfo.
     * <p>
     * When OffloadEngine don't have enough resource
     * (e.g. not enough memory) to offload all the OffloadServiceInfo. The OffloadServiceInfo
     * having lower priority values should be handled by the OffloadEngine first.
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * Only for debug purpose, the string can be long as the raw packet is dump in the string.
     */
    @Override
    public String toString() {
        return String.format(
                "OffloadServiceInfo{ mOffloadServiceInfoKey=%s, mHostName=%s, "
                        + "mOffloadPayload=%s, mPriority=%d, mOffloadType=%d, mSubTypes=%s }",
                mKey,
                mHostname, HexDump.dumpHexString(mOffloadPayload), mPriority,
                mOffloadType, mSubtypes.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OffloadServiceInfo)) return false;
        OffloadServiceInfo that = (OffloadServiceInfo) o;
        return mPriority == that.mPriority && mOffloadType == that.mOffloadType
                && mKey.equals(that.mKey)
                && mHostname.equals(
                that.mHostname) && Arrays.equals(mOffloadPayload,
                that.mOffloadPayload)
                && mSubtypes.equals(that.mSubtypes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mKey, mHostname, mPriority,
                mOffloadType, mSubtypes);
        result = 31 * result + Arrays.hashCode(mOffloadPayload);
        return result;
    }

    /**
     * The {@link OffloadServiceInfo.Key} is the (serviceName, serviceType) pair.
     */
    public static final class Key implements Parcelable {
        @NonNull
        private final String mServiceName;
        @NonNull
        private final String mServiceType;

        /**
         * Creates a new OffloadServiceInfoKey object with the specified parameters.
         *
         * @param serviceName The name of the service.
         * @param serviceType The type of the service.
         */
        public Key(@NonNull String serviceName, @NonNull String serviceType) {
            Objects.requireNonNull(serviceName);
            Objects.requireNonNull(serviceType);
            mServiceName = serviceName;
            mServiceType = serviceType;
        }

        /**
         * Creates a new OffloadServiceInfoKey object from a Parcel.
         *
         * @param in The Parcel to read the object from.
         *
         * @hide
         */
        public Key(@NonNull Parcel in) {
            mServiceName = in.readString();
            mServiceType = in.readString();
        }
        /**
         * Get the service name. (e.g. "NsdChat")
         */
        @NonNull
        public String getServiceName() {
            return mServiceName;
        }

        /**
         * Get the service type. (e.g. "_http._tcp.local" )
         */
        @NonNull
        public String getServiceType() {
            return mServiceType;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeString(mServiceName);
            dest.writeString(mServiceType);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @NonNull
        public static final Creator<Key> CREATOR =
                new Creator<Key>() {
            @Override
            public Key createFromParcel(Parcel in) {
                return new Key(in);
            }

            @Override
            public Key[] newArray(int size) {
                return new Key[size];
            }
        };

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key that = (Key) o;
            return Objects.equals(mServiceName, that.mServiceName) && Objects.equals(
                    mServiceType, that.mServiceType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mServiceName, mServiceType);
        }

        @Override
        public String toString() {
            return String.format("OffloadServiceInfoKey{ mServiceName=%s, mServiceType=%s }",
                    mServiceName, mServiceType);
        }
    }
}
