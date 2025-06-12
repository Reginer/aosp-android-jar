/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.compat.annotation.UnsupportedAppUsage;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.android.net.module.util.ProxyUtils;

import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;

/**
 * Describes a proxy configuration.
 *
 * Proxy configurations are already integrated within the {@code java.net} and
 * Apache HTTP stack. So {@link URLConnection} and Apache's {@code HttpClient} will use
 * them automatically.
 *
 * Other HTTP stacks will need to obtain the proxy info by watching for the
 * {@link Proxy#PROXY_CHANGE_ACTION} broadcast and calling methods such as
 * {@link android.net.ConnectivityManager#getDefaultProxy}.
 */
public class ProxyInfo implements Parcelable {

    private final String mHost;
    private final int mPort;
    private final String mExclusionList;
    private final String[] mParsedExclusionList;
    // Uri.EMPTY if none.
    @NonNull
    private final Uri mPacFileUrl;

    /**
     *@hide
     */
    public static final String LOCAL_EXCL_LIST = "";
    /**
     *@hide
     */
    public static final int LOCAL_PORT = -1;
    /**
     *@hide
     */
    public static final String LOCAL_HOST = "localhost";

    /**
     * Constructs a {@link ProxyInfo} object that points at a Direct proxy
     * on the specified host and port.
     */
    public static ProxyInfo buildDirectProxy(String host, int port) {
        return new ProxyInfo(host, port, null);
    }

    /**
     * Constructs a {@link ProxyInfo} object that points at a Direct proxy
     * on the specified host and port.
     *
     * The proxy will not be used to access any host in exclusion list, exclList.
     *
     * @param exclList Hosts to exclude using the proxy on connections for.  These
     *                 hosts can use wildcards such as *.example.com.
     */
    public static ProxyInfo buildDirectProxy(String host, int port, List<String> exclList) {
        String[] array = exclList.toArray(new String[exclList.size()]);
        return new ProxyInfo(host, port, TextUtils.join(",", array), array);
    }

    /**
     * Construct a {@link ProxyInfo} that will download and run the PAC script
     * at the specified URL.
     */
    public static ProxyInfo buildPacProxy(Uri pacUri) {
        return new ProxyInfo(pacUri);
    }

    /**
     * Construct a {@link ProxyInfo} object that will download and run the PAC script at the
     * specified URL and port.
     */
    @NonNull
    public static ProxyInfo buildPacProxy(@NonNull Uri pacUrl, int port) {
        return new ProxyInfo(pacUrl, port);
    }

    /**
     * Create a ProxyProperties that points at a HTTP Proxy.
     * @hide
     */
    @UnsupportedAppUsage
    public ProxyInfo(String host, int port, String exclList) {
        mHost = host;
        mPort = port;
        mExclusionList = exclList;
        mParsedExclusionList = parseExclusionList(mExclusionList);
        mPacFileUrl = Uri.EMPTY;
    }

    /**
     * Create a ProxyProperties that points at a PAC URL.
     * @hide
     */
    public ProxyInfo(@NonNull Uri pacFileUrl) {
        mHost = LOCAL_HOST;
        mPort = LOCAL_PORT;
        mExclusionList = LOCAL_EXCL_LIST;
        mParsedExclusionList = parseExclusionList(mExclusionList);
        if (pacFileUrl == null) {
            throw new NullPointerException();
        }
        mPacFileUrl = pacFileUrl;
    }

    /**
     * Only used in PacProxyService after Local Proxy is bound.
     * @hide
     */
    public ProxyInfo(@NonNull Uri pacFileUrl, int localProxyPort) {
        mHost = LOCAL_HOST;
        mPort = localProxyPort;
        mExclusionList = LOCAL_EXCL_LIST;
        mParsedExclusionList = parseExclusionList(mExclusionList);
        if (pacFileUrl == null) {
            throw new NullPointerException();
        }
        mPacFileUrl = pacFileUrl;
    }

    private static String[] parseExclusionList(String exclusionList) {
        if (exclusionList == null) {
            return new String[0];
        } else {
            return exclusionList.toLowerCase(Locale.ROOT).split(",");
        }
    }

    private ProxyInfo(String host, int port, String exclList, String[] parsedExclList) {
        mHost = host;
        mPort = port;
        mExclusionList = exclList;
        mParsedExclusionList = parsedExclList;
        mPacFileUrl = Uri.EMPTY;
    }

    /**
     * A copy constructor to hold proxy properties.
     */
    public ProxyInfo(@Nullable ProxyInfo source) {
        if (source != null) {
            mHost = source.getHost();
            mPort = source.getPort();
            mPacFileUrl = source.mPacFileUrl;
            mExclusionList = source.getExclusionListAsString();
            mParsedExclusionList = source.mParsedExclusionList;
        } else {
            mHost = null;
            mPort = 0;
            mExclusionList = null;
            mParsedExclusionList = null;
            mPacFileUrl = Uri.EMPTY;
        }
    }

    /**
     * @hide
     */
    public InetSocketAddress getSocketAddress() {
        InetSocketAddress inetSocketAddress = null;
        try {
            inetSocketAddress = new InetSocketAddress(mHost, mPort);
        } catch (IllegalArgumentException e) { }
        return inetSocketAddress;
    }

    /**
     * Returns the URL of the current PAC script or null if there is
     * no PAC script.
     */
    public Uri getPacFileUrl() {
        return mPacFileUrl;
    }

    /**
     * When configured to use a Direct Proxy this returns the host
     * of the proxy.
     */
    public String getHost() {
        return mHost;
    }

    /**
     * When configured to use a Direct Proxy this returns the port
     * of the proxy
     */
    public int getPort() {
        return mPort;
    }

    /**
     * When configured to use a Direct Proxy this returns the list
     * of hosts for which the proxy is ignored.
     */
    public String[] getExclusionList() {
        return mParsedExclusionList;
    }

    /**
     * comma separated
     * @hide
     */
    @Nullable
    public String getExclusionListAsString() {
        return mExclusionList;
    }

    /**
     * Return true if the pattern of proxy is valid, otherwise return false.
     */
    public boolean isValid() {
        if (!Uri.EMPTY.equals(mPacFileUrl)) return true;
        return ProxyUtils.PROXY_VALID == ProxyUtils.validate(mHost == null ? "" : mHost,
                mPort == 0 ? "" : Integer.toString(mPort),
                mExclusionList == null ? "" : mExclusionList);
    }

    /**
     * @hide
     */
    public java.net.Proxy makeProxy() {
        java.net.Proxy proxy = java.net.Proxy.NO_PROXY;
        if (mHost != null) {
            try {
                InetSocketAddress inetSocketAddress = new InetSocketAddress(mHost, mPort);
                proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, inetSocketAddress);
            } catch (IllegalArgumentException e) {
            }
        }
        return proxy;
    }

    /**
     * @hide
     * @return whether this proxy uses a Proxy Auto Configuration URL.
     */
    public boolean isPacProxy() {
        return !Uri.EMPTY.equals(mPacFileUrl);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!Uri.EMPTY.equals(mPacFileUrl)) {
            sb.append("PAC Script: ");
            sb.append(mPacFileUrl);
        }
        if (mHost != null) {
            sb.append("[");
            sb.append(mHost);
            sb.append("] ");
            sb.append(Integer.toString(mPort));
            if (mExclusionList != null) {
                sb.append(" xl=").append(mExclusionList);
            }
        } else {
            sb.append("[ProxyProperties.mHost == null]");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof ProxyInfo)) return false;
        ProxyInfo p = (ProxyInfo)o;
        // If PAC URL is present in either then they must be equal.
        // Other parameters will only be for fall back.
        if (!Uri.EMPTY.equals(mPacFileUrl)) {
            return mPacFileUrl.equals(p.getPacFileUrl()) && mPort == p.mPort;
        }
        if (!Uri.EMPTY.equals(p.mPacFileUrl)) {
            return false;
        }
        if (mExclusionList != null && !mExclusionList.equals(p.getExclusionListAsString())) {
            return false;
        }
        if (mHost != null && p.getHost() != null && mHost.equals(p.getHost()) == false) {
            return false;
        }
        if (mHost != null && p.mHost == null) return false;
        if (mHost == null && p.mHost != null) return false;
        if (mPort != p.mPort) return false;
        return true;
    }

    /**
     * Implement the Parcelable interface
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    @Override
    /*
     * generate hashcode based on significant fields
     */
    public int hashCode() {
        return ((null == mHost) ? 0 : mHost.hashCode())
                + ((null == mExclusionList) ? 0 : mExclusionList.hashCode())
                + mPort;
    }

    /**
     * Implement the Parcelable interface.
     * @hide
     */
    public void writeToParcel(Parcel dest, int flags) {
        if (!Uri.EMPTY.equals(mPacFileUrl)) {
            dest.writeByte((byte)1);
            mPacFileUrl.writeToParcel(dest, 0);
            dest.writeInt(mPort);
            return;
        } else {
            dest.writeByte((byte)0);
        }
        if (mHost != null) {
            dest.writeByte((byte)1);
            dest.writeString(mHost);
            dest.writeInt(mPort);
        } else {
            dest.writeByte((byte)0);
        }
        dest.writeString(mExclusionList);
        dest.writeStringArray(mParsedExclusionList);
    }

    public static final @android.annotation.NonNull Creator<ProxyInfo> CREATOR =
        new Creator<ProxyInfo>() {
            public ProxyInfo createFromParcel(Parcel in) {
                String host = null;
                int port = 0;
                if (in.readByte() != 0) {
                    Uri url = Uri.CREATOR.createFromParcel(in);
                    int localPort = in.readInt();
                    return new ProxyInfo(url, localPort);
                }
                if (in.readByte() != 0) {
                    host = in.readString();
                    port = in.readInt();
                }
                String exclList = in.readString();
                String[] parsedExclList = in.createStringArray();
                ProxyInfo proxyProperties = new ProxyInfo(host, port, exclList, parsedExclList);
                return proxyProperties;
            }

            public ProxyInfo[] newArray(int size) {
                return new ProxyInfo[size];
            }
        };
}
