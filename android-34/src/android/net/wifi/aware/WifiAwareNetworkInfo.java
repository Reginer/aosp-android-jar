/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.net.wifi.aware;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.NetworkCapabilities;
import android.net.TransportInfo;
import android.os.Parcel;
import android.os.Parcelable;

import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Wi-Fi Aware-specific network information. The information can be extracted from the
 * {@link android.net.NetworkCapabilities} of the network using
 * {@link NetworkCapabilities#getTransportInfo()}.
 * The {@link NetworkCapabilities} is provided by the connectivity service to apps, e.g. received
 * through the
 * {@link android.net.ConnectivityManager.NetworkCallback#onCapabilitiesChanged(android.net.Network,
 * android.net.NetworkCapabilities)} callback.
 * <p>
 * The Wi-Fi Aware-specific network information include the peer's scoped link-local IPv6 address
 * for the Wi-Fi Aware link, as well as (optionally) the port and transport protocol specified by
 * the peer.
 * The scoped link-local IPv6, port, and transport protocol can then be used to create a
 * {@link java.net.Socket} connection to the peer.
 * <p>
 * Note: these are the peer's IPv6 and port information - not the local device's!
 */
public final class WifiAwareNetworkInfo implements TransportInfo, Parcelable {
    private final Inet6Address mIpv6Addr;
    private final int mPort;
    private final int mTransportProtocol;
    private final List<WifiAwareChannelInfo> mChannelInfos;

    /** @hide */
    public WifiAwareNetworkInfo() {
        mIpv6Addr = null;
        mPort = 0; // a value of 0 is considered invalid
        mTransportProtocol = -1;  // a value of -1 is considered invalid
        mChannelInfos = Collections.emptyList();
    }

    /** @hide */
    public WifiAwareNetworkInfo(Inet6Address ipv6Addr) {
        mIpv6Addr = ipv6Addr;
        mPort = 0; // a value of 0 is considered invalid
        mTransportProtocol = -1;  // a value of -1 is considered invalid
        mChannelInfos = Collections.emptyList();
    }

    /** @hide */
    public WifiAwareNetworkInfo(Inet6Address ipv6Addr, int port, int transportProtocol,
            List<WifiAwareChannelInfo> channelInfos) {
        mIpv6Addr = ipv6Addr;
        mPort = port;
        mTransportProtocol = transportProtocol;
        mChannelInfos = channelInfos;
    }

    private WifiAwareNetworkInfo(@Nullable WifiAwareNetworkInfo source) {
        mIpv6Addr = source != null ? source.mIpv6Addr : null;
        mPort = source != null ? source.mPort : 0;
        mTransportProtocol = source != null ? source.mTransportProtocol : -1;
        mChannelInfos = source != null ? source.mChannelInfos : Collections.emptyList();
    }

    /**
     * Get the scoped link-local IPv6 address of the Wi-Fi Aware peer (not of the local device!).
     *
     * @return An IPv6 address.
     */
    @Nullable
    public Inet6Address getPeerIpv6Addr() {
        return mIpv6Addr;
    }

    /**
     * Get the port number to be used to create a network connection to the Wi-Fi Aware peer.
     * The port information is provided by the app running on the peer which requested the
     * connection, using the {@link WifiAwareNetworkSpecifier.Builder#setPort(int)}.
     *
     * @return A port number on the peer. A value of 0 indicates that no port was specified by the
     *         peer.
     */
    public int getPort() {
        return mPort;
    }

    /**
     * Get the transport protocol to be used to communicate over a network connection to the Wi-Fi
     * Aware peer. The transport protocol is provided by the app running on the peer which requested
     * the connection, using the
     * {@link WifiAwareNetworkSpecifier.Builder#setTransportProtocol(int)}.
     * <p>
     * The transport protocol number is assigned by the Internet Assigned Numbers Authority
     * (IANA) https://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml.
     *
     * @return A transport protocol id. A value of -1 indicates that no transport protocol was
     *         specified by the peer.
     */
    public int getTransportProtocol() {
        return mTransportProtocol;
    }

    /**
     * Get a list {@link WifiAwareChannelInfo} which is used by this Aware data-path
     *
     * @return A list of {@link WifiAwareChannelInfo}
     */
    @NonNull
    public List<WifiAwareChannelInfo> getChannelInfoList() {
        return mChannelInfos;
    }

    // parcelable methods

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(mIpv6Addr.getAddress());
        NetworkInterface ni = mIpv6Addr.getScopedInterface();
        dest.writeString(ni == null ? null : ni.getName());
        dest.writeInt(mPort);
        dest.writeInt(mTransportProtocol);
        dest.writeTypedList(mChannelInfos);
    }

    public static final @NonNull Creator<WifiAwareNetworkInfo> CREATOR =
            new Creator<WifiAwareNetworkInfo>() {
                @Override
                public WifiAwareNetworkInfo createFromParcel(Parcel in) {
                    byte[] addr = in.createByteArray();
                    String interfaceName = in.readString();
                    int port = in.readInt();
                    int transportProtocol = in.readInt();
                    Inet6Address ipv6Addr;
                    List<WifiAwareChannelInfo> channelInfos = new ArrayList<>();
                    in.readTypedList(channelInfos, WifiAwareChannelInfo.CREATOR);
                    try {
                        NetworkInterface ni = null;
                        if (interfaceName != null) {
                            try {
                                ni = NetworkInterface.getByName(interfaceName);
                            } catch (SocketException e) {
                                e.printStackTrace();
                            }
                        }
                        ipv6Addr = Inet6Address.getByAddress(null, addr, ni);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                        return new WifiAwareNetworkInfo();
                    }
                    return new WifiAwareNetworkInfo(ipv6Addr, port, transportProtocol,
                            channelInfos);
                }

                @Override
                public WifiAwareNetworkInfo[] newArray(int size) {
                    return new WifiAwareNetworkInfo[size];
                }
            };


    // object methods

    @Override
    public String toString() {
        return new StringBuilder("AwareNetworkInfo: IPv6=").append(mIpv6Addr)
                .append(", port=").append(mPort)
                .append(", transportProtocol=").append(mTransportProtocol)
                .append(", channelInfos=").append(mChannelInfos)
                .toString();
    }

    /** @hide */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof WifiAwareNetworkInfo)) {
            return false;
        }

        WifiAwareNetworkInfo lhs = (WifiAwareNetworkInfo) obj;
        return Objects.equals(mIpv6Addr, lhs.mIpv6Addr) && mPort == lhs.mPort
                && mTransportProtocol == lhs.mTransportProtocol
                && mChannelInfos.equals(lhs.mChannelInfos);
    }

    /** @hide */
    @Override
    public int hashCode() {
        return Objects.hash(mIpv6Addr, mPort, mTransportProtocol, mChannelInfos);
    }
}
