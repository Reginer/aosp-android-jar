/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.net;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Range;

import com.android.net.module.util.InetAddressUtils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Objects;


/**
 * DSCP policy to be set on the requesting NetworkAgent.
 * @hide
 */
@SystemApi
public final class DscpPolicy implements Parcelable {
     /**
     * Indicates that the policy does not specify a protocol.
     */
    public static final int PROTOCOL_ANY = -1;

    /**
     * Indicates that the policy does not specify a port.
     */
    public static final int SOURCE_PORT_ANY = -1;

    /** The unique policy ID. Each requesting network is responsible for maintaining policy IDs
     * unique within that network. In the case where a policy with an existing ID is created, the
     * new policy will update the existing policy with the same ID.
     */
    private final int mPolicyId;

    /** The QoS DSCP marking to be added to packets matching the policy. */
    private final int mDscp;

    /** The source IP address. */
    private final @Nullable InetAddress mSrcAddr;

    /** The destination IP address. */
    private final @Nullable InetAddress mDstAddr;

    /** The source port. */
    private final int mSrcPort;

    /** The IP protocol that the policy requires. */
    private final int mProtocol;

    /** Destination port range. Inclusive range. */
    private final @Nullable Range<Integer> mDstPortRange;

    /**
     * Implement the Parcelable interface
     *
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    /* package */ DscpPolicy(
            int policyId,
            int dscp,
            @Nullable InetAddress srcAddr,
            @Nullable InetAddress dstAddr,
            int srcPort,
            int protocol,
            Range<Integer> dstPortRange) {
        this.mPolicyId = policyId;
        this.mDscp = dscp;
        this.mSrcAddr = srcAddr;
        this.mDstAddr = dstAddr;
        this.mSrcPort = srcPort;
        this.mProtocol = protocol;
        this.mDstPortRange = dstPortRange;

        if (mPolicyId < 1 || mPolicyId > 255) {
            throw new IllegalArgumentException("Policy ID not in valid range: " + mPolicyId);
        }
        if (mDscp < 0 || mDscp > 63) {
            throw new IllegalArgumentException("DSCP value not in valid range: " + mDscp);
        }
        // Since SOURCE_PORT_ANY is the default source port value need to allow it as well.
        // TODO: Move the default value into this constructor or throw an error from the
        // instead.
        if (mSrcPort < -1 || mSrcPort > 65535) {
            throw new IllegalArgumentException("Source port not in valid range: " + mSrcPort);
        }
        if (mDstPortRange != null
                && (dstPortRange.getLower() < 0 || mDstPortRange.getLower() > 65535)
                && (mDstPortRange.getUpper() < 0 || mDstPortRange.getUpper() > 65535)) {
            throw new IllegalArgumentException("Destination port not in valid range");
        }
        if (mSrcAddr != null && mDstAddr != null && (mSrcAddr instanceof Inet6Address)
                != (mDstAddr instanceof Inet6Address)) {
            throw new IllegalArgumentException("Source/destination address of different family");
        }
    }

    /**
     * The unique policy ID.
     *
     * Each requesting network is responsible for maintaining unique
     * policy IDs. In the case where a policy with an existing ID is created, the new
     * policy will update the existing policy with the same ID
     *
     * @return Policy ID set in Builder.
     */
    public int getPolicyId() {
        return mPolicyId;
    }

    /**
     * The QoS DSCP marking to be added to packets matching the policy.
     *
     * @return DSCP value set in Builder.
     */
    public int getDscpValue() {
        return mDscp;
    }

    /**
     * The source IP address.
     *
     * @return Source IP address set in Builder or {@code null} if none was set.
     */
    public @Nullable InetAddress getSourceAddress() {
        return mSrcAddr;
    }

    /**
     * The destination IP address.
     *
     * @return Destination IP address set in Builder or {@code null} if none was set.
     */
    public @Nullable InetAddress getDestinationAddress() {
        return mDstAddr;
    }

    /**
     * The source port.
     *
     * @return Source port set in Builder or {@link #SOURCE_PORT_ANY} if no port was set.
     */
    public int getSourcePort() {
        return mSrcPort;
    }

    /**
     * The IP protocol that the policy requires.
     *
     * @return Protocol set in Builder or {@link #PROTOCOL_ANY} if no protocol was set.
     *         {@link #PROTOCOL_ANY} indicates that any protocol will be matched.
     */
    public int getProtocol() {
        return mProtocol;
    }

    /**
     * Destination port range. Inclusive range.
     *
     * @return Range<Integer> set in Builder or {@code null} if none was set.
     */
    public @Nullable Range<Integer> getDestinationPortRange() {
        return mDstPortRange;
    }

    @Override
    public String toString() {
        return "DscpPolicy { "
                + "policyId = " + mPolicyId + ", "
                + "dscp = " + mDscp + ", "
                + "srcAddr = " + mSrcAddr + ", "
                + "dstAddr = " + mDstAddr + ", "
                + "srcPort = " + mSrcPort + ", "
                + "protocol = " + mProtocol + ", "
                + "dstPortRange = "
                + (mDstPortRange == null ? "none" : mDstPortRange.toString())
                + " }";
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof DscpPolicy)) return false;
        DscpPolicy that = (DscpPolicy) o;
        return true
                && mPolicyId == that.mPolicyId
                && mDscp == that.mDscp
                && Objects.equals(mSrcAddr, that.mSrcAddr)
                && Objects.equals(mDstAddr, that.mDstAddr)
                && mSrcPort == that.mSrcPort
                && mProtocol == that.mProtocol
                && Objects.equals(mDstPortRange, that.mDstPortRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPolicyId, mDscp, mSrcAddr.hashCode(),
                mDstAddr.hashCode(), mSrcPort, mProtocol, mDstPortRange.hashCode());
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mPolicyId);
        dest.writeInt(mDscp);
        InetAddressUtils.parcelInetAddress(dest, mSrcAddr, flags);
        InetAddressUtils.parcelInetAddress(dest, mDstAddr, flags);
        dest.writeInt(mSrcPort);
        dest.writeInt(mProtocol);
        dest.writeBoolean(mDstPortRange != null ? true : false);
        if (mDstPortRange != null) {
            dest.writeInt(mDstPortRange.getLower());
            dest.writeInt(mDstPortRange.getUpper());
        }
    }

    /** @hide */
    DscpPolicy(@NonNull Parcel in) {
        this.mPolicyId = in.readInt();
        this.mDscp = in.readInt();
        this.mSrcAddr = InetAddressUtils.unparcelInetAddress(in);
        this.mDstAddr = InetAddressUtils.unparcelInetAddress(in);
        this.mSrcPort = in.readInt();
        this.mProtocol = in.readInt();
        if (in.readBoolean()) {
            this.mDstPortRange = new Range<Integer>(in.readInt(), in.readInt());
        } else {
            this.mDstPortRange = null;
        }
    }

    /** @hide */
    public @SystemApi static final @NonNull Parcelable.Creator<DscpPolicy> CREATOR =
            new Parcelable.Creator<DscpPolicy>() {
                @Override
                public DscpPolicy[] newArray(int size) {
                    return new DscpPolicy[size];
                }

                @Override
                public DscpPolicy createFromParcel(@NonNull android.os.Parcel in) {
                    return new DscpPolicy(in);
                }
            };

    /**
     * A builder for {@link DscpPolicy}
     *
     */
    public static final class Builder {

        private final int mPolicyId;
        private final int mDscp;
        private @Nullable InetAddress mSrcAddr;
        private @Nullable InetAddress mDstAddr;
        private int mSrcPort = SOURCE_PORT_ANY;
        private int mProtocol = PROTOCOL_ANY;
        private @Nullable Range<Integer> mDstPortRange;

        private long mBuilderFieldsSet = 0L;

        /**
         * Creates a new Builder.
         *
         * @param policyId The unique policy ID. Each requesting network is responsible for
         *                 maintaining unique policy IDs. In the case where a policy with an
         *                 existing ID is created, the new policy will update the existing
         *                 policy with the same ID
         * @param dscpValue The DSCP value to set.
         */
        public Builder(int policyId, int dscpValue) {
            mPolicyId = policyId;
            mDscp = dscpValue;
        }

        /**
         * Specifies that this policy matches packets with the specified source IP address.
         */
        public @NonNull Builder setSourceAddress(@NonNull InetAddress value) {
            mSrcAddr = value;
            return this;
        }

        /**
         * Specifies that this policy matches packets with the specified destination IP address.
         */
        public @NonNull Builder setDestinationAddress(@NonNull InetAddress value) {
            mDstAddr = value;
            return this;
        }

        /**
         * Specifies that this policy matches packets with the specified source port.
         */
        public @NonNull Builder setSourcePort(int value) {
            mSrcPort = value;
            return this;
        }

        /**
         * Specifies that this policy matches packets with the specified protocol.
         */
        public @NonNull Builder setProtocol(int value) {
            mProtocol = value;
            return this;
        }

        /**
         * Specifies that this policy matches packets with the specified destination port range.
         */
        public @NonNull Builder setDestinationPortRange(@NonNull Range<Integer> range) {
            mDstPortRange = range;
            return this;
        }

        /**
         * Constructs a DscpPolicy with the specified parameters.
         */
        public @NonNull DscpPolicy build() {
            return new DscpPolicy(
                    mPolicyId,
                    mDscp,
                    mSrcAddr,
                    mDstAddr,
                    mSrcPort,
                    mProtocol,
                    mDstPortRange);
        }
    }
}
