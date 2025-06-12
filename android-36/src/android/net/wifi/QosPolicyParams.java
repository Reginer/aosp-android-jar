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

package android.net.wifi;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.DscpPolicy;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;
import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Objects;

/**
 * Parameters for QoS policies requested by system applications.
 * @hide
 */
@SystemApi
public final class QosPolicyParams implements Parcelable {
    private static final String TAG = "QosPolicyParams";

    /**
     * Indicates that the policy does not specify a DSCP value.
     */
    public static final int DSCP_ANY = -1;

    /**
     * Indicates that the policy does not specify a protocol.
     */
    public static final int PROTOCOL_ANY = DscpPolicy.PROTOCOL_ANY;

    /**
     * Policy should match packets using the TCP protocol.
     */
    public static final int PROTOCOL_TCP = 6;

    /**
     * Policy should match packets using the UDP protocol.
     */
    public static final int PROTOCOL_UDP = 17;

    /**
     * Policy should match packets using the ESP protocol.
     */
    public static final int PROTOCOL_ESP = 50;

    /** @hide */
    @IntDef(prefix = { "PROTOCOL_" }, value = {
            PROTOCOL_ANY,
            PROTOCOL_TCP,
            PROTOCOL_UDP,
            PROTOCOL_ESP
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Protocol {}

    /**
     * Policy should match packets in the uplink direction.
     */
    public static final int DIRECTION_UPLINK = 0;

    /**
     * Policy should match packets in the downlink direction.
     */
    public static final int DIRECTION_DOWNLINK = 1;


    /** @hide */
    @IntDef(prefix = { "DIRECTION_" }, value = {
            DIRECTION_UPLINK,
            DIRECTION_DOWNLINK,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Direction {}

    /**
     * Indicates that the policy does not specify a User Priority.
     */
    public static final int USER_PRIORITY_ANY = -1;

    /**
     * Policy should be assigned a low background priority.
     */
    public static final int USER_PRIORITY_BACKGROUND_LOW = 1;

    /**
     * Policy should be assigned a high background priority.
     */
    public static final int USER_PRIORITY_BACKGROUND_HIGH = 2;

    /**
     * Policy should be assigned a low best-effort priority.
     */
    public static final int USER_PRIORITY_BEST_EFFORT_LOW = 0;

    /**
     * Policy should be assigned a high best-effort priority.
     */
    public static final int USER_PRIORITY_BEST_EFFORT_HIGH = 3;

    /**
     * Policy should be assigned a low video priority.
     */
    public static final int USER_PRIORITY_VIDEO_LOW = 4;

    /**
     * Policy should be assigned a high video priority.
     */
    public static final int USER_PRIORITY_VIDEO_HIGH = 5;

    /**
     * Policy should be assigned a low voice priority.
     */
    public static final int USER_PRIORITY_VOICE_LOW = 6;

    /**
     * Policy should be assigned a high voice priority.
     */
    public static final int USER_PRIORITY_VOICE_HIGH = 7;

    /** @hide */
    @IntDef(prefix = { "USER_PRIORITY_" }, value = {
            USER_PRIORITY_ANY,
            USER_PRIORITY_BACKGROUND_LOW,
            USER_PRIORITY_BACKGROUND_HIGH,
            USER_PRIORITY_BEST_EFFORT_LOW,
            USER_PRIORITY_BEST_EFFORT_HIGH,
            USER_PRIORITY_VIDEO_LOW,
            USER_PRIORITY_VIDEO_HIGH,
            USER_PRIORITY_VOICE_LOW,
            USER_PRIORITY_VOICE_HIGH,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface UserPriority {}

    /**
     * Indicates that the policy does not specify an IP version.
     */
    public static final int IP_VERSION_ANY = -1;

    /**
     * Policy should match packets using IPv4.
     */
    public static final int IP_VERSION_4 = 4;

    /**
     * Policy should match packets using IPv6.
     */
    public static final int IP_VERSION_6 = 6;

    /** @hide */
    @IntDef(prefix = { "IP_VERSION_" }, value = {
            IP_VERSION_ANY,
            IP_VERSION_4,
            IP_VERSION_6
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface IpVersion {}

    /**
     * Indicates that the policy does not specify a destination port.
     */
    public static final int DESTINATION_PORT_ANY = -1;

    /**
     * Unique policy ID. See {@link Builder#Builder(int, int)} for more information.
     */
    private final int mPolicyId;

    /**
     * Translated policy ID. Should only be set by the Wi-Fi service.
     * @hide
     */
    private int mTranslatedPolicyId;

    // QoS DSCP marking. See {@link Builder#setDscp(int)} for more information.
    private final int mDscp;

    // User priority to apply to packets matching the policy. Only applicable to downlink requests.
    private final int mUserPriority;

    // Source IP address.
    private final @Nullable InetAddress mSrcIp;

    // Destination IP address.
    private final @Nullable InetAddress mDstIp;

    // Source port.
    private final int mSrcPort;

    // IP protocol that the policy requires.
    private final @Protocol int mProtocol;

    // Single destination port. Only applicable to downlink requests.
    private final int mDstPort;

    // Destination port range. Inclusive range. Only applicable to uplink requests.
    private final @Nullable int[] mDstPortRange;

    // Direction of traffic stream.
    private final @Direction int mDirection;

    // IP version. Only applicable to downlink requests.
    private final @IpVersion int mIpVersion;

    // Flow label. Only applicable to downlink requests using IPv6.
    private final @Nullable byte[] mFlowLabel;

    // QoS characteristics. Mandatory for uplink requests.
    private final @Nullable QosCharacteristics mQosCharacteristics;

    private QosPolicyParams(int policyId, int dscp, @UserPriority int userPriority,
            @Nullable InetAddress srcIp, @Nullable InetAddress dstIp, int srcPort,
            @Protocol int protocol, @Nullable int[] dstPortRange, @Direction int direction,
            @IpVersion int ipVersion, int dstPort, @Nullable byte[] flowLabel,
            @Nullable QosCharacteristics qosCharacteristics) {
        this.mPolicyId = policyId;
        this.mDscp = dscp;
        this.mUserPriority = userPriority;
        this.mSrcIp = srcIp;
        this.mDstIp = dstIp;
        this.mSrcPort = srcPort;
        this.mProtocol = protocol;
        this.mDstPort = dstPort;
        this.mDstPortRange = dstPortRange;
        this.mDirection = direction;
        this.mIpVersion = ipVersion;
        this.mFlowLabel = flowLabel;
        this.mQosCharacteristics = qosCharacteristics;
    }

    /**
     * Validate the parameters in this instance.
     *
     * @return true if all parameters are valid, false otherwise
     * @hide
     */
    public boolean validate() {
        if (mPolicyId < 1 || mPolicyId > 255) {
            Log.e(TAG, "Policy ID not in valid range: " + mPolicyId);
            return false;
        }
        if (mDscp < DSCP_ANY || mDscp > 63) {
            Log.e(TAG, "DSCP value not in valid range: " + mDscp);
            return false;
        }
        if (mUserPriority < USER_PRIORITY_ANY || mUserPriority > USER_PRIORITY_VOICE_HIGH) {
            Log.e(TAG, "User priority not in valid range: " + mUserPriority);
            return false;
        }
        if (mSrcPort < DscpPolicy.SOURCE_PORT_ANY || mSrcPort > 65535) {
            Log.e(TAG, "Source port not in valid range: " + mSrcPort);
            return false;
        }
        if (mDstPort < DESTINATION_PORT_ANY || mDstPort > 65535) {
            Log.e(TAG, "Destination port not in valid range: " + mDstPort);
            return false;
        }
        if (mDstPortRange != null && (mDstPortRange[0] < 0 || mDstPortRange[0] > 65535
                || mDstPortRange[1] < 0 || mDstPortRange[1] > 65535)) {
            Log.e(TAG, "Dst port range value not valid. start="
                    + mDstPortRange[0] + ", end=" + mDstPortRange[1]);
            return false;
        }
        if (!(mDirection == DIRECTION_UPLINK || mDirection == DIRECTION_DOWNLINK)) {
            Log.e(TAG, "Invalid direction enum: " + mDirection);
            return false;
        }
        if (!(mIpVersion == IP_VERSION_ANY || mIpVersion == IP_VERSION_4
                || mIpVersion == IP_VERSION_6)) {
            Log.e(TAG, "Invalid ipVersion enum: " + mIpVersion);
            return false;
        }
        if (mIpVersion == IP_VERSION_4) {
            if (mSrcIp != null && !(mSrcIp instanceof Inet4Address)) {
                Log.e(TAG, "Src address does not match IP version " + mIpVersion);
                return false;
            }
            if (mDstIp != null && !(mDstIp instanceof Inet4Address)) {
                Log.e(TAG, "Dst address does not match IP version " + mIpVersion);
                return false;
            }
        }
        if (mIpVersion == IP_VERSION_6) {
            if (mSrcIp != null && !(mSrcIp instanceof Inet6Address)) {
                Log.e(TAG, "Src address does not match IP version " + mIpVersion);
                return false;
            }
            if (mDstIp != null && !(mDstIp instanceof Inet6Address)) {
                Log.e(TAG, "Dst address does not match IP version " + mIpVersion);
                return false;
            }
        }
        if (mQosCharacteristics != null && !mQosCharacteristics.validate()) {
            Log.e(TAG, "Invalid QoS characteristics provided");
            return false;
        }

        // Check required parameters based on direction.
        if (mDirection == DIRECTION_UPLINK) {
            if (mQosCharacteristics == null) {
                Log.e(TAG, "QoS characteristics must be provided for uplink requests");
                return false;
            }
            if (mIpVersion != IP_VERSION_ANY) {
                Log.e(TAG, "IP Version should not be set for uplink requests");
                return false;
            }
            if (mDstPort != DESTINATION_PORT_ANY) {
                Log.e(TAG, "Single destination port should not be set for uplink requests");
                return false;
            }
            if (mFlowLabel != null) {
                Log.e(TAG, "Flow label should not be set for uplink requests");
                return false;
            }
        } else {
            if (mUserPriority == USER_PRIORITY_ANY) {
                Log.e(TAG, "User priority must be provided for downlink requests");
                return false;
            }
            if (mIpVersion == IP_VERSION_ANY) {
                Log.e(TAG, "IP version must be provided for downlink requests");
                return false;
            }
            if (mDstPortRange != null) {
                Log.e(TAG, "Destination port range should not be set for downlink requests");
                return false;
            }
            if (mFlowLabel != null) {
                if (mIpVersion != IP_VERSION_6) {
                    Log.e(TAG, "Flow label can only be used with IP version 6");
                    return false;
                }
                if (mFlowLabel.length != 3) {
                    Log.e(TAG, "Flow label must be of size 3, provided size is "
                            + mFlowLabel.length);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Set the translated policy ID for this policy.
     *
     * Note: Translated policy IDs should only be set by the Wi-Fi service.
     * @hide
     */
    public void setTranslatedPolicyId(int translatedPolicyId) {
        mTranslatedPolicyId = translatedPolicyId;
    }

    /**
     * Get the ID for this policy.
     *
     * See {@link Builder#Builder(int, int)} for more information.
     */
    @IntRange(from = 1, to = 255)
    public int getPolicyId() {
        return mPolicyId;
    }

    /**
     * Get the translated ID for this policy.
     *
     * See {@link #setTranslatedPolicyId} for more information.
     * @hide
     */
    public int getTranslatedPolicyId() {
        return mTranslatedPolicyId;
    }


    /**
     * Get the DSCP value for this policy.
     *
     * See {@link Builder#setDscp(int)} for more information.
     *
     * @return DSCP value, or {@link #DSCP_ANY} if not assigned.
     */
    @IntRange(from = DSCP_ANY, to = 63)
    public int getDscp() {
        return mDscp;
    }

    /**
     * Get the User Priority (UP) for this policy.
     *
     * See {@link Builder#setUserPriority(int)} for more information.
     *
     * @return User Priority value, or {@link #USER_PRIORITY_ANY} if not assigned.
     */
    public @UserPriority int getUserPriority() {
        return mUserPriority;
    }

    /**
     * Get the source IP address for this policy.
     *
     * See {@link Builder#setSourceAddress(InetAddress)} for more information.
     *
     * @return source IP address, or null if not assigned.
     */
    public @Nullable InetAddress getSourceAddress() {
        return mSrcIp;
    }

    /**
     * Get the destination IP address for this policy.
     *
     * See {@link Builder#setDestinationAddress(InetAddress)} for more information.
     *
     * @return destination IP address, or null if not assigned.
     */
    public @Nullable InetAddress getDestinationAddress() {
        return mDstIp;
    }

    /**
     * Get the source port for this policy.
     *
     * See {@link Builder#setSourcePort(int)} for more information.
     *
     * @return source port, or {@link DscpPolicy#SOURCE_PORT_ANY} if not assigned.
     */
    @IntRange(from = DscpPolicy.SOURCE_PORT_ANY, to = 65535)
    public int getSourcePort() {
        return mSrcPort;
    }

    /**
     * Get the protocol for this policy.
     *
     * See {@link Builder#setProtocol(int)} for more information.
     *
     * @return protocol, or {@link #PROTOCOL_ANY} if not assigned.
     */
    public @Protocol int getProtocol() {
        return mProtocol;
    }

    /**
     * Get the destination port for this policy.
     *
     * See {@link Builder#setDestinationPort(int)} for more information.
     *
     * @return destination port, or {@link #DESTINATION_PORT_ANY} if not assigned.
     */
    @IntRange(from = DESTINATION_PORT_ANY, to = 65535)
    public int getDestinationPort() {
        return mDstPort;
    }

    /**
     * Get the destination port range for this policy.
     *
     * See {@link Builder#setDestinationPortRange(int, int)} for more information.
     *
     * @return destination port range, or null if not assigned.
     */
    public @Nullable int[] getDestinationPortRange() {
        return mDstPortRange;
    }

    /**
     * Get the direction for this policy.
     *
     * See {@link Builder#Builder(int, int)} for more information.
     */
    public @Direction int getDirection() {
        return mDirection;
    }

    /**
     * Get the IP version for this policy.
     *
     * See {@link Builder#setIpVersion(int)} for more information.
     *
     * @return IP version, or {@link #IP_VERSION_ANY} if not assigned.
     */
    public @IpVersion int getIpVersion() {
        return mIpVersion;
    }

    /**
     * Get the flow label for this policy.
     *
     * See {@link Builder#setFlowLabel(byte[])} for more information.
     *
     * @return flow label, or null if not assigned.
     */
    public @Nullable byte[] getFlowLabel() {
        return mFlowLabel;
    }

    /**
     * Get the QoS characteristics for this policy.
     *
     * See {@link Builder#setQosCharacteristics(QosCharacteristics)} for more information.
     *
     * @return QoS characteristics object, or null if not assigned.
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public @Nullable QosCharacteristics getQosCharacteristics() {
        if (!SdkLevel.isAtLeastV()) {
            throw new UnsupportedOperationException();
        }
        return mQosCharacteristics;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QosPolicyParams that = (QosPolicyParams) o;
        return mPolicyId == that.mPolicyId
                && mDscp == that.mDscp
                && mUserPriority == that.mUserPriority
                && mSrcIp.equals(that.mSrcIp)
                && mDstIp.equals(that.mDstIp)
                && mSrcPort == that.mSrcPort
                && mProtocol == that.mProtocol
                && mDstPort == that.mDstPort
                && Arrays.equals(mDstPortRange, that.mDstPortRange)
                && mDirection == that.mDirection
                && mIpVersion == that.mIpVersion
                && mFlowLabel == that.mFlowLabel
                && Objects.equals(mQosCharacteristics, that.mQosCharacteristics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPolicyId, mDscp, mUserPriority, mSrcIp, mDstIp, mSrcPort,
                mProtocol, Arrays.hashCode(mDstPortRange), mDirection, mIpVersion, mDstPort,
                Arrays.hashCode(mFlowLabel), mQosCharacteristics);
    }

    @Override
    public String toString() {
        return "{policyId=" + mPolicyId + ", "
                + "dscp=" + mDscp + ", "
                + "userPriority=" + mUserPriority + ", "
                + "srcIp=" + mSrcIp + ", "
                + "dstIp=" + mDstIp + ", "
                + "srcPort=" + mSrcPort + ", "
                + "protocol=" + mProtocol + ", "
                + "dstPort=" + mDstPort + ", "
                + "dstPortRange=" + Arrays.toString(mDstPortRange) + ", "
                + "direction=" + mDirection + ", "
                + "ipVersion=" + mIpVersion + ", "
                + "flowLabel=" + Arrays.toString(mFlowLabel) + ", "
                + "qosCharacteristics=" + mQosCharacteristics + "}";
    }

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    private InetAddress getInetAddrOrNull(byte[] byteAddr) {
        if (byteAddr == null) return null;
        try {
            return InetAddress.getByAddress(byteAddr);
        } catch (Exception e) {
            return null;
        }
    }

    /** @hide */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mPolicyId);
        dest.writeInt(mDscp);
        dest.writeInt(mUserPriority);
        dest.writeByteArray(mSrcIp != null ? mSrcIp.getAddress() : null);
        dest.writeByteArray(mDstIp != null ? mDstIp.getAddress() : null);
        dest.writeInt(mSrcPort);
        dest.writeInt(mProtocol);
        dest.writeInt(mDstPort);
        dest.writeIntArray(mDstPortRange);
        dest.writeInt(mDirection);
        dest.writeInt(mIpVersion);
        dest.writeByteArray(mFlowLabel);
        if (SdkLevel.isAtLeastV()) {
            dest.writeParcelable(mQosCharacteristics, 0);
        }
    }

    /** @hide */
    QosPolicyParams(@NonNull Parcel in) {
        this.mPolicyId = in.readInt();
        this.mDscp = in.readInt();
        this.mUserPriority = in.readInt();
        this.mSrcIp = getInetAddrOrNull(in.createByteArray());
        this.mDstIp = getInetAddrOrNull(in.createByteArray());
        this.mSrcPort = in.readInt();
        this.mProtocol = in.readInt();
        this.mDstPort = in.readInt();
        this.mDstPortRange = in.createIntArray();
        this.mDirection = in.readInt();
        this.mIpVersion = in.readInt();
        this.mFlowLabel = in.createByteArray();
        if (SdkLevel.isAtLeastV()) {
            this.mQosCharacteristics = in.readParcelable(
                    QosCharacteristics.class.getClassLoader(), QosCharacteristics.class);
        } else {
            this.mQosCharacteristics = null;
        }
    }

    public static final @NonNull Parcelable.Creator<QosPolicyParams> CREATOR =
            new Parcelable.Creator<QosPolicyParams>() {
                @Override
                public QosPolicyParams createFromParcel(Parcel in) {
                    return new QosPolicyParams(in);
                }

                @Override
                public QosPolicyParams[] newArray(int size) {
                    return new QosPolicyParams[size];
                }
            };

    /**
     * Builder for {@link QosPolicyParams}.
     */
    public static final class Builder {
        private final int mPolicyId;
        private final @Direction int mDirection;
        private @Nullable InetAddress mSrcIp;
        private @Nullable InetAddress mDstIp;
        private int mDscp = DSCP_ANY;
        private @UserPriority int mUserPriority = USER_PRIORITY_ANY;
        private int mSrcPort = DscpPolicy.SOURCE_PORT_ANY;
        private int mProtocol = PROTOCOL_ANY;
        private int mDstPort = DESTINATION_PORT_ANY;
        private @Nullable int[] mDstPortRange;
        private @IpVersion int mIpVersion = IP_VERSION_ANY;
        private byte[] mFlowLabel;
        private @Nullable QosCharacteristics mQosCharacteristics;

        /**
         * Constructor for {@link Builder}.
         *
         * @param policyId Unique ID to identify this policy. Each requesting application is
         *                 responsible for maintaining policy IDs unique for that app. IDs must be
         *                 in the range 1 <= policyId <= 255.
         *
         *                 In the case where a policy with an existing ID is created, the new policy
         *                 will be rejected. To update an existing policy, remove the existing one
         *                 before sending the new one.
         * @param direction Whether this policy applies to the uplink or downlink direction.
         */
        public Builder(@IntRange(from = 1, to = 255) int policyId, @Direction int direction) {
            mPolicyId = policyId;
            mDirection = direction;
        }

        /**
         * Specifies that this policy matches packets with the provided source IP address.
         */
        public @NonNull Builder setSourceAddress(@Nullable InetAddress value) {
            mSrcIp = value;
            return this;
        }

        /**
         * Specifies that this policy matches packets with the provided destination IP address.
         */
        public @NonNull Builder setDestinationAddress(@Nullable InetAddress value) {
            mDstIp = value;
            return this;
        }

        /**
         * Specifies the DSCP value. For uplink requests, this value will be applied to packets
         * that match the classifier. For downlink requests, this will be part of the classifier.
         */
        public @NonNull Builder setDscp(@IntRange(from = DSCP_ANY, to = 63) int value) {
            mDscp = value;
            return this;
        }

        /**
         * Specifies that the provided User Priority should be applied to packets that
         * match this classifier. Only applicable to downlink requests.
         */
        public @NonNull Builder setUserPriority(@UserPriority int value) {
            mUserPriority = value;
            return this;
        }

        /**
         * Specifies that this policy matches packets with the provided source port.
         */
        public @NonNull Builder setSourcePort(
                @IntRange(from = DscpPolicy.SOURCE_PORT_ANY, to = 65535) int value) {
            mSrcPort = value;
            return this;
        }

        /**
         * Specifies that this policy matches packets with the provided protocol.
         */
        public @NonNull Builder setProtocol(@Protocol int value) {
            mProtocol = value;
            return this;
        }

        /**
         * Specifies that this policy matches packets with the provided destination port.
         * Only applicable to downlink requests.
         */
        public @NonNull Builder setDestinationPort(
                @IntRange(from = DESTINATION_PORT_ANY, to = 65535) int value) {
            mDstPort = value;
            return this;
        }

        /**
         * Specifies that this policy matches packets with the provided destination port range.
         * Only applicable to uplink requests.
         */
        public @NonNull Builder setDestinationPortRange(
                @IntRange(from = 0, to = 65535) int start,
                @IntRange(from = 0, to = 65535) int end) {
            mDstPortRange = new int[]{start, end};
            return this;
        }

        /**
         * Specifies that this policy matches packets with the provided IP version.
         * This argument is mandatory for downlink requests, and is ignored for uplink requests.
         */
        public @NonNull Builder setIpVersion(@IpVersion int value) {
            mIpVersion = value;
            return this;
        }

        /**
         * Specifies that this policy matches packets with the provided flow label.
         * Only applicable to downlink requests using IPv6.
         */
        public @NonNull Builder setFlowLabel(@Nullable byte[] value) {
            mFlowLabel = value;
            return this;
        }

        /**
         * Specifies traffic flow parameters to use for this policy request.
         * This argument is mandatory for uplink requests, but optional for downlink requests.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        public @NonNull Builder setQosCharacteristics(
                @Nullable QosCharacteristics qosCharacteristics) {
            if (!SdkLevel.isAtLeastV()) {
                throw new UnsupportedOperationException();
            }
            mQosCharacteristics = qosCharacteristics;
            return this;
        }

        /**
         * Construct a QosPolicyParams object with the specified parameters.
         */
        public @NonNull QosPolicyParams build() {
            QosPolicyParams params = new QosPolicyParams(mPolicyId, mDscp, mUserPriority, mSrcIp,
                    mDstIp, mSrcPort, mProtocol, mDstPortRange, mDirection, mIpVersion, mDstPort,
                    mFlowLabel, mQosCharacteristics);
            if (!params.validate()) {
                throw new IllegalArgumentException("Provided parameters are invalid");
            }
            return params;
        }
    }
}
