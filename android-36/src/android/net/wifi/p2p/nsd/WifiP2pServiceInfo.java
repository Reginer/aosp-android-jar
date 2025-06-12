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
import android.net.wifi.util.Environment;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.wifi.flags.Flags;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A class for storing service information that is advertised
 * over a Wi-Fi peer-to-peer setup
 *
 * @see WifiP2pUpnpServiceInfo
 * @see WifiP2pDnsSdServiceInfo
 */
public class WifiP2pServiceInfo implements Parcelable {

    /**
     * All service protocol types.
     */
    public static final int SERVICE_TYPE_ALL             = 0;

    /**
     * DNS based service discovery protocol.
     */
    public static final int SERVICE_TYPE_BONJOUR         = 1;

    /**
     * UPnP protocol.
     */
    public static final int SERVICE_TYPE_UPNP            = 2;

    /**
     * WS-Discovery protocol
     * @hide
     */
    public static final int SERVICE_TYPE_WS_DISCOVERY    = 3;

    /**
     * Vendor Specific protocol
     */
    public static final int SERVICE_TYPE_VENDOR_SPECIFIC = 255;

    /**
     * the list of query string for wpa_supplicant
     *
     * e.g)
     * # IP Printing over TCP (PTR) (RDATA=MyPrinter._ipp._tcp.local.)
     * {"bonjour", "045f697070c00c000c01", "094d795072696e746572c027"
     *
     * # IP Printing over TCP (TXT) (RDATA=txtvers=1,pdl=application/postscript)
     * {"bonjour", "096d797072696e746572045f697070c00c001001",
     *  "09747874766572733d311a70646c3d6170706c69636174696f6e2f706f7374736372797074"}
     *
     * [UPnP]
     * # UPnP uuid
     * {"upnp", "10", "uuid:6859dede-8574-59ab-9332-123456789012"}
     *
     * # UPnP rootdevice
     * {"upnp", "10", "uuid:6859dede-8574-59ab-9332-123456789012::upnp:rootdevice"}
     *
     * # UPnP device
     * {"upnp", "10", "uuid:6859dede-8574-59ab-9332-123456789012::urn:schemas-upnp
     * -org:device:InternetGatewayDevice:1"}
     *
     *  # UPnP service
     * {"upnp", "10", "uuid:6859dede-8574-59ab-9322-123456789012::urn:schemas-upnp
     * -org:service:ContentDirectory:2"}
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    private List<String> mQueryList;

    /**
     * This field is used only when the service advertisement is using un-synchronized service
     * discovery (USD) protocol. Refer Wi-Fi Alliance Wi-Fi Direct R2 specification section 3.7 -
     * "Unsynchronized Service Discovery (USD)" for the details.
     */
    private WifiP2pUsdBasedServiceConfig mUsdServiceConfig;

    /**
     * Service advertisement session ID / Publish ID for USD based service advertisement.
     * This is a nonzero value used to identify the instance of service advertisement.
     * This value is filled in the service discovery response frame (USD publish frame),
     * Service descriptor attribute (SDA) - instance ID field.
     */
    /**
     * Service advertisement session ID (Advertiser ID) for USD based service discovery response.
     * The session ID is used to identify a local advertisement session.
     * It is a nonzero ID in the range of 1 to 255 filled in the Service descriptor attribute (SDA)
     * - instance ID field of the service discovery response frame (Publish frame).
     * Zero by default indicates that the USD session for this service is not running.
     */
    private int mUsdSessionId;

    /**
     * This is only used in subclass.
     *
     * @param queryList query string for wpa_supplicant
     * @hide
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.P, trackingBug = 115609023)
    protected WifiP2pServiceInfo(List<String> queryList) {
        if (queryList == null) {
            throw new IllegalArgumentException("query list cannot be null");
        }
        mQueryList = queryList;
    }

    /**
     * This constructor is only used in Parcelable.
     *
     * @param queryList query string for wpa_supplicant
     * @param usdConfig See {@link WifiP2pUsdBasedServiceConfig}
     * @param usdSessionId The USD based service advertisement session ID.
     */
    private WifiP2pServiceInfo(List<String> queryList,
            @NonNull WifiP2pUsdBasedServiceConfig usdConfig, int usdSessionId) {
        mQueryList = queryList;
        mUsdServiceConfig = usdConfig;
        mUsdSessionId = usdSessionId;
    }

    /**
     * Constructor for creating a service information for advertising the service using
     * un-synchronized service discovery (USD) protocol. Refer Wi-Fi Alliance Wi-Fi Direct R2
     * specification section 3.7 - "Unsynchronized Service Discovery (USD)" for the details.
     *
     * @param usdConfig See {@link WifiP2pUsdBasedServiceConfig}
     *
     * @return service info containing USD based service configuration.
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    @FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
    public WifiP2pServiceInfo(@NonNull WifiP2pUsdBasedServiceConfig usdConfig) {
        if (!Environment.isSdkAtLeastB()) {
            throw new UnsupportedOperationException();
        }
        Objects.requireNonNull(usdConfig, "usd based service config cannot be null");
        mUsdServiceConfig = usdConfig;
    }

   /**
    * Return the list of the query string for wpa_supplicant.
    *
    * @return the list of the query string for wpa_supplicant.
    * @hide
    */
   public List<String> getSupplicantQueryList() {
       return mQueryList;
   }

    /**
     * Get the service information configured to advertise using un-synchronized service discovery
     * (USD) protocol.
     * See {@link #WifiP2pServiceInfo(WifiP2pUsdBasedServiceConfig)}
     *
     * @return A valid or not null {@link WifiP2pUsdBasedServiceConfig} if the service information
     * is configured to advertise using un-synchronized service discovery (USD) protocol.
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
     * Return the Service advertisement session ID for USD based service advertisement.
     *
     * @return session id
     * @hide
     */
    /**
     * Return the Service advertisement session ID for USD based service advertisement.
     * This ID is used to identify a service advertisement session.
     *
     * @return A nonzero ID in the range of 1 to 255 when the session is running.
     * @hide
     */
    public int getUsdSessionId() {
        return mUsdSessionId;
    }

    /**
     * Set the service advertisement session ID for USD based service advertisement.
     * Default value is zero.
     *
     * @param sessionId nonzero session ID is set when the USD session for this service is started.
     * @hide
     */
    public void setUsdSessionId(int sessionId) {
        mUsdSessionId = sessionId;
    }

   /**
    * Converts byte array to hex string.
    *
    * @param data
    * @return hex string.
    * @hide
    */
   static String bin2HexStr(byte[] data) {
       StringBuffer sb = new StringBuffer();

       for (byte b: data) {
           String s = null;
           try {
               s = Integer.toHexString(b & 0xff);
           } catch (Exception e) {
               e.printStackTrace();
               return null;
           }
           //add 0 padding
           if (s.length() == 1) {
               sb.append('0');
           }
           sb.append(s);
       }
       return sb.toString();
   }

   @Override
   public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof WifiP2pServiceInfo)) {
            return false;
        }

       /*
        * Don't compare USD based service advertisement session ID.
        * The session ID may be changed on each service discovery advertisement.
        */
        WifiP2pServiceInfo servInfo = (WifiP2pServiceInfo) o;
        return Objects.equals(mQueryList, servInfo.mQueryList)
                && Objects.equals(mUsdServiceConfig, servInfo.mUsdServiceConfig);
   }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (mQueryList == null ? 0 : mQueryList.hashCode());
        result = 31 * result + (mUsdServiceConfig == null ? 0 : mUsdServiceConfig.hashCode());
        return result;
    }

    /** Implement the Parcelable interface {@hide} */
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface {@hide} */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(mQueryList);
        if (Environment.isSdkAtLeastB() && Flags.wifiDirectR2()) {
            dest.writeParcelable(mUsdServiceConfig, flags);
            dest.writeInt(mUsdSessionId);
        }
    }

    /** Implement the Parcelable interface {@hide} */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final @android.annotation.NonNull Creator<WifiP2pServiceInfo> CREATOR =
            new Creator<WifiP2pServiceInfo>() {
                public WifiP2pServiceInfo createFromParcel(Parcel in) {
                    List<String> data = new ArrayList<String>();
                    in.readStringList(data);
                    WifiP2pUsdBasedServiceConfig config = null;
                    int usdSessionId = 0;
                    if (Environment.isSdkAtLeastB() && Flags.wifiDirectR2()) {
                        config = in.readParcelable(
                                WifiP2pUsdBasedServiceConfig.class.getClassLoader());
                        usdSessionId = in.readInt();
                    }
                    return new WifiP2pServiceInfo(data, config, usdSessionId);
                }
                public WifiP2pServiceInfo[] newArray(int size) {
                    return new WifiP2pServiceInfo[size];
                }
            };
}
