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

package android.net.wifi.p2p.nsd;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresApi;
import android.compat.annotation.UnsupportedAppUsage;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.util.Environment;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.wifi.flags.Flags;

import java.util.Locale;
import java.util.Objects;

/**
 * A class for creating a service discovery request for use with
 * {@link WifiP2pManager#addServiceRequest} and {@link WifiP2pManager#removeServiceRequest}
 *
 * <p>This class is used to create service discovery request for custom
 * vendor specific service discovery protocol {@link WifiP2pServiceInfo#SERVICE_TYPE_VENDOR_SPECIFIC}
 * or to search all service protocols {@link WifiP2pServiceInfo#SERVICE_TYPE_ALL}.
 *
 * <p>For the purpose of creating a UPnP or Bonjour service request, use
 * {@link WifiP2pUpnpServiceRequest} or {@link WifiP2pDnsSdServiceRequest} respectively.
 *
 * {@see WifiP2pManager}
 * {@see WifiP2pUpnpServiceRequest}
 * {@see WifiP2pDnsSdServiceRequest}
 */
public class WifiP2pServiceRequest implements Parcelable {

    /**
     * Service discovery protocol. It's defined in table63 in Wi-Fi Direct specification.
     */
    private int mProtocolType;

    /**
     * The length of the service request TLV.
     * The value is equal to 2 plus the number of octets in the
     * query data field.
     */
    private int mLength;

    /**
     * Service transaction ID.
     * This is a nonzero value used to match the service request/response TLVs.
     */
    private int mTransId;

    /**
     * The hex dump string of query data for the requested service information.
     *
     * e.g) DnsSd apple file sharing over tcp (dns name=_afpovertcp._tcp.local.)
     * 0b5f6166706f766572746370c00c000c01
     */
    private String mQuery;

    /**
     * This field is used only when the service discovery request is using un-synchronized service
     * discovery (USD) protocol. Refer Wi-Fi Alliance Wi-Fi Direct R2 specification section 3.7 -
     * "Unsynchronized Service Discovery (USD)" for the details.
     */
    private WifiP2pUsdBasedServiceConfig mUsdServiceConfig;

    /**
     * Service discovery request session ID (Seeker ID) for USD based service discovery.
     * The session ID is used to match the USD based service discovery request/response frames.
     * A nonzero ID in the range of 1 to 255 is filled in the Service descriptor attribute (SDA) -
     * instance ID field of the service discovery request frame (Subscribe frame). The responding
     * device copies this ID in the Service descriptor attribute (SDA) - requester instance ID
     * field of the service discovery response frame (Publish frame).
     * Zero by default indicates that the USD session for this service is not running.
     */
    private int mUsdSessionId = 0;

    /**
     * This constructor is only used in newInstance().
     *
     * @param protocolType service discovery protocol.
     * @param query The part of service specific query.
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    protected WifiP2pServiceRequest(int protocolType, String query) {
        validateQuery(query);

        mProtocolType = protocolType;
        mQuery = query;
        if (query != null) {
            mLength = query.length()/2 + 2;
        } else {
            mLength = 2;
        }
    }

    /**
     * This constructor is only used in parcelable.
     *
     * @param serviceType service discovery type.
     * @param length the length of service discovery packet.
     * @param transId the transaction id
     * @param query The part of service specific query.
     * @param usdConfig The USD based service config.
     * @param usdSessionId The USD based service discovery request session ID.
     */
    private WifiP2pServiceRequest(int serviceType, int length,
            int transId, String query, @NonNull WifiP2pUsdBasedServiceConfig usdConfig,
            int usdSessionId) {
        mProtocolType = serviceType;
        mLength = length;
        mTransId = transId;
        mQuery = query;
        mUsdServiceConfig = usdConfig;
        mUsdSessionId = usdSessionId;
    }

    /**
     * Return transaction id.
     *
     * @return transaction id
     * @hide
     */
    public int getTransactionId() {
        return mTransId;
    }

    /**
     * Set transaction id.
     *
     * @param id
     * @hide
     */
    public void setTransactionId(int id) {
        mTransId = id;
    }

    /**
     * Return wpa_supplicant request string.
     *
     * The format is the hex dump of the following frame.
     * <pre>
     * _______________________________________________________________
     * |        Length (2)        |   Type (1)   | Transaction ID (1) |
     * |                  Query Data (variable)                       |
     * </pre>
     *
     * @return wpa_supplicant request string.
     * @hide
     */
    public String getSupplicantQuery() {
        StringBuffer sb = new StringBuffer();
        // length is retained as little endian format.
        sb.append(String.format(Locale.US, "%02x", (mLength) & 0xff));
        sb.append(String.format(Locale.US, "%02x", (mLength >> 8) & 0xff));
        sb.append(String.format(Locale.US, "%02x", mProtocolType));
        sb.append(String.format(Locale.US, "%02x", mTransId));
        if (mQuery != null) {
            sb.append(mQuery);
        }

        return sb.toString();
    }

    /**
     * Return the USD based service discovery request session ID.
     * This ID is used to match the USD based service request/response frames.
     *
     * @return A nonzero ID in the range of 1 to 255 when the session is running.
     * @hide
     */
    public int getUsdSessionId() {
        return mUsdSessionId;
    }

    /**
     * Set the USD based service discovery request session ID.
     * Default value is zero.
     *
     * @param sessionId nonzero session ID is set when the USD session for this service is started.
     * @hide
     */
    public void setUsdSessionId(int sessionId) {
        mUsdSessionId = sessionId;
    }

    /**
     /**
     * Get the service information configured to discover a service using un-synchronized service
     * discovery (USD) protocol.
     * See {@link #WifiP2pServiceRequest(WifiP2pUsdBasedServiceConfig)}.
     *
     * @return A valid or not null {@link WifiP2pUsdBasedServiceConfig} if the service information
     * is configured to discover a service using un-synchronized service discovery (USD) protocol.
     * Otherwise, it is null.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    @FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
    @Nullable
    public WifiP2pUsdBasedServiceConfig getWifiP2pUsdBasedServiceConfig() {
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        return mUsdServiceConfig;
    }

    /**
     * Validate query.
     *
     * <p>If invalid, throw IllegalArgumentException.
     * @param query The part of service specific query.
     */
    private void validateQuery(String query) {
        if (query == null) {
            return;
        }

        int UNSIGNED_SHORT_MAX = 0xffff;
        if (query.length()%2 == 1) {
            throw new IllegalArgumentException(
                    "query size is invalid. query=" + query);
        }
        if (query.length()/2 > UNSIGNED_SHORT_MAX) {
            throw new IllegalArgumentException(
                    "query size is too large. len=" + query.length());
        }

        // check whether query is hex string.
        query = query.toLowerCase(Locale.ROOT);
        char[] chars = query.toCharArray();
        for (char c: chars) {
            if (!((c >= '0' && c <= '9') ||
                    (c >= 'a' && c <= 'f'))){
                throw new IllegalArgumentException(
                        "query should be hex string. query=" + query);
            }
        }
    }

    /**
     * Create a service discovery request.
     *
     * @param protocolType can be {@link WifiP2pServiceInfo#SERVICE_TYPE_ALL}
     * or {@link WifiP2pServiceInfo#SERVICE_TYPE_VENDOR_SPECIFIC}.
     * In order to create a UPnP or Bonjour service request, use
     * {@link WifiP2pUpnpServiceRequest} or {@link WifiP2pDnsSdServiceRequest}
     * respectively
     *
     * @param queryData hex string that is vendor specific.  Can be null.
     * @return service discovery request.
     */
    public static WifiP2pServiceRequest newInstance(int protocolType, String queryData) {
        return new WifiP2pServiceRequest(protocolType, queryData);
    }

    /**
     * Create a service discovery request.
     *
     * @param protocolType can be {@link WifiP2pServiceInfo#SERVICE_TYPE_ALL}
     * or {@link WifiP2pServiceInfo#SERVICE_TYPE_VENDOR_SPECIFIC}.
     * In order to create a UPnP or Bonjour service request, use
     * {@link WifiP2pUpnpServiceRequest} or {@link WifiP2pDnsSdServiceRequest}
     * respectively
     *
     * @return service discovery request.
     */
    public static WifiP2pServiceRequest newInstance(int protocolType ) {
        return new WifiP2pServiceRequest(protocolType, null);
    }

    /**
     * Constructor for creating a service discovery request for discovering the service using
     * un-synchronized service discovery (USD) protocol. Refer Wi-Fi Alliance Wi-Fi Direct R2
     * specification section 3.7 - "Unsynchronized Service Discovery (USD)" for the details.
     *
     * @param usdConfig See {@link WifiP2pUsdBasedServiceConfig}
     *
     * @return service discovery request containing USD based service configuration.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    @FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
    public WifiP2pServiceRequest(@NonNull WifiP2pUsdBasedServiceConfig usdConfig) {
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        Objects.requireNonNull(usdConfig, "usdConfig cannot be null");
        mUsdServiceConfig = usdConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof WifiP2pServiceRequest)) {
            return false;
        }

        WifiP2pServiceRequest req = (WifiP2pServiceRequest)o;

        /*
         * Not compare transaction id.
         * Transaction id may be changed on each service discovery operation.
         */
        return mProtocolType == req.mProtocolType
                && mLength == req.mLength
                && Objects.equals(mQuery, req.mQuery)
                && Objects.equals(mUsdServiceConfig, req.mUsdServiceConfig);
   }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + mProtocolType;
        result = 31 * result + mLength;
        result = 31 * result + (mQuery == null ? 0 : mQuery.hashCode());
        result = 31 * result + (mUsdServiceConfig == null ? 0 : mUsdServiceConfig.hashCode());
        return result;
    }

    /** Implement the Parcelable interface {@hide} */
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface {@hide} */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mProtocolType);
        dest.writeInt(mLength);
        dest.writeInt(mTransId);
        dest.writeString(mQuery);
        if (Environment.isSdkAtLeastB() && Flags.wifiDirectR2()) {
            dest.writeParcelable(mUsdServiceConfig, flags);
            dest.writeInt(mUsdSessionId);
        }
    }

    /** Implement the Parcelable interface {@hide} */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final @android.annotation.NonNull Creator<WifiP2pServiceRequest> CREATOR =
            new Creator<WifiP2pServiceRequest>() {
                public WifiP2pServiceRequest createFromParcel(Parcel in) {
                    int servType = in.readInt();
                    int length = in.readInt();
                    int transId = in.readInt();
                    String query = in.readString();
                    WifiP2pUsdBasedServiceConfig config = null;
                    int usdSessionId = 0;
                    if (Environment.isSdkAtLeastB() && Flags.wifiDirectR2()) {
                        config = in.readParcelable(
                                WifiP2pUsdBasedServiceConfig.class.getClassLoader());
                        usdSessionId = in.readInt();
                    }
                    return new WifiP2pServiceRequest(servType, length, transId, query, config,
                            usdSessionId);
                }
                public WifiP2pServiceRequest[] newArray(int size) {
                    return new WifiP2pServiceRequest[size];
                }
            };
}
