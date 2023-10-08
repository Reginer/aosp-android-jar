/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.net.wifi.p2p;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

/**
 * A class representing Wifi Display information for a device.
 *
 * See Wifi Display technical specification v1.0.0, section 5.1.2.
 * See Wifi Display technical specification v2.0.0, section 5.1.12 for Wifi Display R2.
 */
public final class WifiP2pWfdInfo implements Parcelable {

    private boolean mEnabled;

    /** Device information bitmap */
    private int mDeviceInfo;

    /** R2 Device information bitmap */
    private int mR2DeviceInfo = -1;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "DEVICE_TYPE_" }, value = {
            DEVICE_TYPE_WFD_SOURCE,
            DEVICE_TYPE_PRIMARY_SINK,
            DEVICE_TYPE_SECONDARY_SINK,
            DEVICE_TYPE_SOURCE_OR_PRIMARY_SINK})
    public @interface DeviceType {}

    /** The device is a Wifi Display Source. */
    public static final int DEVICE_TYPE_WFD_SOURCE = 0;
    /** The device is a primary sink. */
    public static final int DEVICE_TYPE_PRIMARY_SINK = 1;
    /** The device is a secondary sink. This type is only supported by R1. */
    public static final int DEVICE_TYPE_SECONDARY_SINK = 2;
    /** The device is dual-role capable i.e. either a WFD source or a primary sink. */
    public static final int DEVICE_TYPE_SOURCE_OR_PRIMARY_SINK = 3;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "PREFERRED_CONNECTIVITY_" }, value = {
            PREFERRED_CONNECTIVITY_P2P,
            PREFERRED_CONNECTIVITY_TDLS})
    public @interface PreferredConnectivity {}

    /** Wifi Display (WFD) preferred connectivity is Wifi Direct (P2P). */
    public static final int PREFERRED_CONNECTIVITY_P2P = 0;
    /** Wifi Display (WFD) preferred connectivity is TDLS. */
    public static final int PREFERRED_CONNECTIVITY_TDLS = 1;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, prefix = {"DEVICE_INFO_"}, value = {
            DEVICE_INFO_DEVICE_TYPE_MASK,
            DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SOURCE,
            DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SINK,
            DEVICE_INFO_SESSION_AVAILABLE_MASK,
            DEVICE_INFO_WFD_SERVICE_DISCOVERY_SUPPORT,
            DEVICE_INFO_PREFERRED_CONNECTIVITY_MASK,
            DEVICE_INFO_CONTENT_PROTECTION_SUPPORT,
            DEVICE_INFO_TIME_SYNCHRONIZATION_SUPPORT,
            DEVICE_INFO_AUDIO_UNSUPPORTED_AT_PRIMARY_SINK,
            DEVICE_INFO_AUDIO_ONLY_SUPPORT_AT_SOURCE,
            DEVICE_INFO_TDLS_PERSISTENT_GROUP,
            DEVICE_INFO_TDLS_PERSISTENT_GROUP_REINVOKE})
    public @interface DeviceInfoMask {}

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, prefix = {"DEVICE_INFO_"}, value = {DEVICE_INFO_DEVICE_TYPE_MASK})
    public @interface R2DeviceInfoMask {}

    /**
     * {@link #getDeviceInfo()} & {@link #DEVICE_INFO_DEVICE_TYPE_MASK} is one of
     * {@link #DEVICE_TYPE_WFD_SOURCE}, {@link #DEVICE_TYPE_PRIMARY_SINK},
     * {@link #DEVICE_TYPE_SECONDARY_SINK} or {@link #DEVICE_TYPE_SOURCE_OR_PRIMARY_SINK}.
     *
     * The bit definition is listed in 5.1.2 WFD Device Information Subelement and
     * 5.1.12 WFD R2 Device Information Subelement in Wifi Display Technical Specification.
     */
    public static final int DEVICE_INFO_DEVICE_TYPE_MASK = 1 << 1 | 1 << 0;
    /**
     * Bit field for {@link #getDeviceInfo()}, indicates that coupled sink is supported at source.
     *
     * The bit definition is listed in 5.1.2 WFD Device Information Subelement in
     * Wifi Display Technical Specification.
     */
    public static final int DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SOURCE = 1 << 2;
    /**
     * Bit field for {@link #getDeviceInfo()}, indicates that coupled sink is supporeted at sink.
     *
     * The bit definition is listed in 5.1.2 WFD Device Information Subelement in
     * Wifi Display Technical Specification.
     */
    public static final int DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SINK = 1 << 3;
    private static final int SESSION_AVAILABLE_BIT1 = 1 << 4;
    private static final int SESSION_AVAILABLE_BIT2 = 1 << 5;
    /**
     * Bit field for {@link #getDeviceInfo()}, indicates that Wifi Display session is available.
     *
     * The bit definition is listed in 5.1.2 WFD Device Information Subelement in
     * Wifi Display Technical Specification.
     */
    public static final int DEVICE_INFO_SESSION_AVAILABLE_MASK =
            SESSION_AVAILABLE_BIT2 | SESSION_AVAILABLE_BIT1;
    /**
     * Bit field for {@link #getDeviceInfo()}, indicates that Wifi Display discovery is supported.
     *
     * The bit definition is listed in 5.1.2 WFD Device Information Subelement in
     * Wifi Display Technical Specification.
     */
    public static final int DEVICE_INFO_WFD_SERVICE_DISCOVERY_SUPPORT = 1 << 6;
    /**
     * Bit field for {@link #getDeviceInfo()}, indicate the preferred connectifity for Wifi Display.
     *
     * The bit definition is listed in 5.1.2 WFD Device Information Subelement in
     * Wifi Display Technical Specification.
     * The value is one of {@link #PREFERRED_CONNECTIVITY_P2P} or
     * {@link #PREFERRED_CONNECTIVITY_TDLS}.
     */
    public static final int DEVICE_INFO_PREFERRED_CONNECTIVITY_MASK = 1 << 7;
    /**
     * Bit field for {@link #getDeviceInfo()}, indicate the support of Content Protection
     * using the HDCP system 2.0/2.1.
     *
     * The bit definition is listed in 5.1.2 WFD Device Information Subelement in
     * Wifi Display Technical Specification.
     */
    public static final int DEVICE_INFO_CONTENT_PROTECTION_SUPPORT = 1 << 8;
    /**
     * Bit field for {@link #getDeviceInfo()}, indicate time synchronization
     * using 802.1AS is supported.
     *
     * The bit definition is listed in 5.1.2 WFD Device Information Subelement in
     * Wifi Display Technical Specification.
     */
    public static final int DEVICE_INFO_TIME_SYNCHRONIZATION_SUPPORT = 1 << 9;
    /**
     * Bit field for {@link #getDeviceInfo()}, indicate audio is not supported at primary sink.
     *
     * The bit definition is listed in 5.1.2 WFD Device Information Subelement in
     * Wifi Display Technical Specification.
     */
    public static final int DEVICE_INFO_AUDIO_UNSUPPORTED_AT_PRIMARY_SINK = 1 << 10;
    /**
     * Bit field for {@link #getDeviceInfo()}, indicate audo is only supported at source.
     *
     * The bit definition is listed in 5.1.2 WFD Device Information Subelement in
     * Wifi Display Technical Specification.
     */
    public static final int DEVICE_INFO_AUDIO_ONLY_SUPPORT_AT_SOURCE = 1 << 11;
    /** Bit field for {@link #getDeviceInfo()}, indicate that TDLS persistent group is intended.
     *
     * The bit definition is listed in 5.1.2 WFD Device Information Subelement in
     * Wifi Display Technical Specification.
     */
    public static final int DEVICE_INFO_TDLS_PERSISTENT_GROUP = 1 << 12;
    /** Bit field for {@link #getDeviceInfo()}, indicate that the request is for
     * re-invocation of TDLS persistent group.
     *
     * The bit definition is listed in 5.1.2 WFD Device Information Subelement in
     * Wifi Display Technical Specification.
     */
    public static final int DEVICE_INFO_TDLS_PERSISTENT_GROUP_REINVOKE = 1 << 13;

    private int mCtrlPort;

    private int mMaxThroughput;

    /** Default constructor. */
    public WifiP2pWfdInfo() {}

    /** @hide */
    @UnsupportedAppUsage
    public WifiP2pWfdInfo(int devInfo, int ctrlPort, int maxTput) {
        mEnabled = true;
        mDeviceInfo = devInfo;
        mCtrlPort = ctrlPort;
        mMaxThroughput = maxTput;
        mR2DeviceInfo = -1;
    }

    /**
     * Return R1 raw device info, See
     * Wifi Display technical specification v1.0.0, section 5.1.2.
     * Access bit fields by DEVICE_INFO_* constants.
     */
    @DeviceInfoMask
    public int getDeviceInfo() {
        return mDeviceInfo;
    }

    /**
     * Set Wifi Display R2 raw device info, see
     * Wifi Display technical specification v2.0.0, section 5.1.12.
     * Access bit fields by {@link #DEVICE_INFO_DEVICE_TYPE_MASK}.
     *
     * @param r2DeviceInfo the raw data of R2 device info.
     * @hide
     */
    public void setR2DeviceInfo(int r2DeviceInfo) {
        mR2DeviceInfo = r2DeviceInfo;
    }

    /**
     * Return R2 raw device info, See
     * Wifi Display technical specification v2.0.0, section 5.1.12.
     * Access bit fields by {@link #DEVICE_INFO_DEVICE_TYPE_MASK}.
     */
    @R2DeviceInfoMask
    public int getR2DeviceInfo() {
        return mR2DeviceInfo;
    }

    /** Returns true is Wifi Display is enabled, false otherwise. */
    public boolean isEnabled() {
        return mEnabled;
    }

    /** Returns true is Wifi Display R2 is enabled, false otherwise. */
    public boolean isR2Supported() {
        return mR2DeviceInfo >= 0;
    }

    /**
     * Sets whether Wifi Display should be enabled.
     *
     * @param enabled true to enable Wifi Display, false to disable
     */
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    /**
     * Sets the type of the Wifi Display R2 device.
     * See Wifi Display technical specification v2.0.0, section 5.1.12 for Wifi Display R2.
     * Before calling this API, call {@link WifiManager#isWifiDisplayR2Supported()
     * to know whether Wifi Display R2 is supported or not.
     * If R2 info was filled without Wifi Display R2 support,
     * {@link WifiP2pManager#setWfdInfo(Channel, WifiP2pWfdInfo, ActionListener)
     * would fail.
     *
     * @param deviceType One of {@link #DEVICE_TYPE_WFD_SOURCE}, {@link #DEVICE_TYPE_PRIMARY_SINK},
     * {@link #DEVICE_TYPE_SOURCE_OR_PRIMARY_SINK}
     * @return true if the device type was successfully set, false otherwise
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public boolean setR2DeviceType(@DeviceType int deviceType) {
        if (!SdkLevel.isAtLeastS()) {
            throw new UnsupportedOperationException();
        }
        if (DEVICE_TYPE_WFD_SOURCE != deviceType
                && DEVICE_TYPE_PRIMARY_SINK != deviceType
                && DEVICE_TYPE_SOURCE_OR_PRIMARY_SINK != deviceType) {
            return false;
        }
        if (!isR2Supported()) mR2DeviceInfo = 0;
        mR2DeviceInfo &= ~DEVICE_INFO_DEVICE_TYPE_MASK;
        mR2DeviceInfo |= deviceType;
        return true;
    }

    /**
     * Get the type of the device.
     * One of {@link #DEVICE_TYPE_WFD_SOURCE}, {@link #DEVICE_TYPE_PRIMARY_SINK},
     * {@link #DEVICE_TYPE_SECONDARY_SINK}, {@link #DEVICE_TYPE_SOURCE_OR_PRIMARY_SINK}
     */
    @DeviceType
    public int getDeviceType() {
        return mDeviceInfo & DEVICE_INFO_DEVICE_TYPE_MASK;
    }

    /**
     * Get the type of the R2 device.
     * One of {@link #DEVICE_TYPE_WFD_SOURCE}, {@link #DEVICE_TYPE_PRIMARY_SINK},
     * or {@link #DEVICE_TYPE_SOURCE_OR_PRIMARY_SINK}
     */
    @DeviceType
    public int getR2DeviceType() {
        return mR2DeviceInfo & DEVICE_INFO_DEVICE_TYPE_MASK;
    }

    /**
     * Sets the type of the device.
     *
     * @param deviceType One of {@link #DEVICE_TYPE_WFD_SOURCE}, {@link #DEVICE_TYPE_PRIMARY_SINK},
     * {@link #DEVICE_TYPE_SECONDARY_SINK}, {@link #DEVICE_TYPE_SOURCE_OR_PRIMARY_SINK}
     * @return true if the device type was successfully set, false otherwise
     */
    public boolean setDeviceType(@DeviceType int deviceType) {
        if (DEVICE_TYPE_WFD_SOURCE <= deviceType
                && deviceType <= DEVICE_TYPE_SOURCE_OR_PRIMARY_SINK) {
            mDeviceInfo &= ~DEVICE_INFO_DEVICE_TYPE_MASK;
            mDeviceInfo |= deviceType;
            return true;
        }
        return false;
    }

    /** Returns true if a session is available, false otherwise. */
    public boolean isSessionAvailable() {
        return (mDeviceInfo & DEVICE_INFO_SESSION_AVAILABLE_MASK) != 0;
    }

    /**
     * Sets whether a session is available.
     *
     * @param enabled true to indicate that a session is available, false otherwise.
     */
    public void setSessionAvailable(boolean enabled) {
        if (enabled) {
            mDeviceInfo |= SESSION_AVAILABLE_BIT1;
            mDeviceInfo &= ~SESSION_AVAILABLE_BIT2;
        } else {
            mDeviceInfo &= ~DEVICE_INFO_SESSION_AVAILABLE_MASK;
        }
    }

    /**
     * @return true if Content Protection using the HDCP system 2.0/2.1 is supported.
     */
    public boolean isContentProtectionSupported() {
        return (mDeviceInfo & DEVICE_INFO_CONTENT_PROTECTION_SUPPORT) != 0;
    }

    /**
     * Sets whether Content Protection using the HDCP system 2.0/2.1 is supported.
     *
     * @param enabled true to indicate that Content Protection is supported, false otherwise.
     */
    public void setContentProtectionSupported(boolean enabled) {
        if (enabled) {
            mDeviceInfo |= DEVICE_INFO_CONTENT_PROTECTION_SUPPORT;
        } else {
            mDeviceInfo &= ~DEVICE_INFO_CONTENT_PROTECTION_SUPPORT;
        }
    }

    /**
     * Returns true if Coupled Sink is supported by WFD Source.
     * See Wifi Display technical specification v1.0.0, section 4.9.
     */
    public boolean isCoupledSinkSupportedAtSource() {
        return (mDeviceInfo & DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SOURCE) != 0;
    }

    /**
     * Sets whether Coupled Sink feature is supported by WFD Source.
     * See Wifi Display technical specification v1.0.0, section 4.9.
     *
     * @param enabled true to indicate support for coupled sink, false otherwise.
     */
    public void setCoupledSinkSupportAtSource(boolean enabled) {
        if (enabled) {
            mDeviceInfo |= DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SOURCE;
        } else {
            mDeviceInfo &= ~DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SOURCE;
        }
    }

    /**
     * Returns true if Coupled Sink is supported by WFD Sink.
     * See Wifi Display technical specification v1.0.0, section 4.9.
     */
    public boolean isCoupledSinkSupportedAtSink() {
        return (mDeviceInfo & DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SINK) != 0;
    }

    /**
     * Sets whether Coupled Sink feature is supported by WFD Sink.
     * See Wifi Display technical specification v1.0.0, section 4.9.
     *
     * @param enabled true to indicate support for coupled sink, false otherwise.
     */
    public void setCoupledSinkSupportAtSink(boolean enabled) {
        if (enabled) {
            mDeviceInfo |= DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SINK;
        } else {
            mDeviceInfo &= ~DEVICE_INFO_COUPLED_SINK_SUPPORT_AT_SINK;
        }
    }

    /** Returns the TCP port at which the WFD Device listens for RTSP messages. */
    public int getControlPort() {
        return mCtrlPort;
    }

    /** Sets the TCP port at which the WFD Device listens for RTSP messages. */
    public void setControlPort(@IntRange(from = 0) int port) {
        mCtrlPort = port;
    }

    /** Sets the maximum average throughput capability of the WFD Device, in megabits/second. */
    public void setMaxThroughput(@IntRange(from = 0) int maxThroughput) {
        mMaxThroughput = maxThroughput;
    }

    /** Returns the maximum average throughput capability of the WFD Device, in megabits/second. */
    public int getMaxThroughput() {
        return mMaxThroughput;
    }

    /** @hide */
    public String getDeviceInfoHex() {
        return String.format(
                Locale.US, "%04x%04x%04x", mDeviceInfo, mCtrlPort, mMaxThroughput);
    }

    /** @hide */
    public String getR2DeviceInfoHex() {
        return String.format(Locale.US, "%04x%04x", 2, mR2DeviceInfo);
    }

    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("WFD enabled: ").append(mEnabled);
        sbuf.append("WFD DeviceInfo: ").append(mDeviceInfo);
        sbuf.append("\n WFD CtrlPort: ").append(mCtrlPort);
        sbuf.append("\n WFD MaxThroughput: ").append(mMaxThroughput);
        sbuf.append("\n WFD R2 DeviceInfo: ").append(mR2DeviceInfo);
        return sbuf.toString();
    }

    /** Implement the Parcelable interface */
    public int describeContents() {
        return 0;
    }

    /** Copy constructor. */
    public WifiP2pWfdInfo(@Nullable WifiP2pWfdInfo source) {
        if (source != null) {
            mEnabled = source.mEnabled;
            mDeviceInfo = source.mDeviceInfo;
            mCtrlPort = source.mCtrlPort;
            mMaxThroughput = source.mMaxThroughput;
            mR2DeviceInfo = source.mR2DeviceInfo;
        }
    }

    /** Implement the Parcelable interface */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mEnabled ? 1 : 0);
        dest.writeInt(mDeviceInfo);
        dest.writeInt(mCtrlPort);
        dest.writeInt(mMaxThroughput);
        dest.writeInt(mR2DeviceInfo);
    }

    private void readFromParcel(Parcel in) {
        mEnabled = (in.readInt() == 1);
        mDeviceInfo = in.readInt();
        mCtrlPort = in.readInt();
        mMaxThroughput = in.readInt();
        mR2DeviceInfo = in.readInt();
    }

    /** Implement the Parcelable interface */
    public static final @NonNull Creator<WifiP2pWfdInfo> CREATOR =
        new Creator<WifiP2pWfdInfo>() {
            public WifiP2pWfdInfo createFromParcel(Parcel in) {
                WifiP2pWfdInfo device = new WifiP2pWfdInfo();
                device.readFromParcel(in);
                return device;
            }

            public WifiP2pWfdInfo[] newArray(int size) {
                return new WifiP2pWfdInfo[size];
            }
        };
}
