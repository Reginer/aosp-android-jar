/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresApi;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.wifi.flags.Flags;

import java.util.Arrays;

/**
 * A class for a response for USD based service discovery.
 * For the details of the configuration, refer Wi-Fi Alliance Wi-Fi Direct R2 specification
 * - Appendix H - Unsynchronized Service Discovery (as defined in Wi-Fi Aware) and section
 * 4.2.13 USD frame format.
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@FlaggedApi(Flags.FLAG_WIFI_DIRECT_R2)
public final class WifiP2pUsdBasedServiceResponse implements Parcelable {
    /**
     * Service discovery protocol tye. It's defined in table 129 in Wi-Fi Direct R2 specification.
     */
    private int mServiceProtocolType = -1;

    /**
     * Optional Service specific information content send in the response frame.
     */
    private byte[] mServiceSpecificInfo;

    /**
     * Hidden constructor. This is only used in framework.
     *
     * @param serviceProtocolType The service protocol type.
     * @param serviceSpecificInfo The service specific information.
     * @hide
     *
     */
    public WifiP2pUsdBasedServiceResponse(int serviceProtocolType,
            @Nullable byte[] serviceSpecificInfo) {
        mServiceProtocolType = serviceProtocolType;
        mServiceSpecificInfo = serviceSpecificInfo;
    }

    /**
     * Get the service protocol type provided by the peer device in the USD service response.
     * See also {@link WifiP2pUsdBasedServiceConfig.Builder#setServiceProtocolType(int)}
     *
     * @return A non-negative service layer protocol type.
     */
    @IntRange(from = 0, to = 255)
    public int getServiceProtocolType() {
        return mServiceProtocolType;
    }

    /** Get the service specific information provided by the peer device in the USD service
     * response.
     * See also {@link WifiP2pUsdBasedServiceConfig.Builder#setServiceSpecificInfo(byte[])}
     *
     *  @return A byte-array of service specification information, or null if unset.
     */
    @Nullable
    public byte[] getServiceSpecificInfo() {
        return mServiceSpecificInfo;
    }

    /**
     * Generates a string of all the defined elements.
     *
     * @return a compiled string representing all elements
     */
    public String toString() {
        StringBuilder sbuf = new StringBuilder("WifiP2pUsdBasedServiceResponse:");
        sbuf.append("\n Protocol type: ").append(mServiceProtocolType);
        sbuf.append("\n Service specific info : ").append((mServiceSpecificInfo == null)
                ? "<null>" : Arrays.toString(mServiceSpecificInfo));
        return sbuf.toString();
    }

    /** Implement the Parcelable interface */
    public int describeContents() {
        return 0;
    }

    /** Implement the Parcelable interface */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mServiceProtocolType);
        dest.writeByteArray(mServiceSpecificInfo);
    }

    /** Implement the Parcelable interface */
    @NonNull
    public static final Creator<WifiP2pUsdBasedServiceResponse> CREATOR =
            new Creator<WifiP2pUsdBasedServiceResponse>() {
                public WifiP2pUsdBasedServiceResponse createFromParcel(Parcel in) {
                    int serviceProtocolType = in.readInt();
                    byte[] ssi = in.createByteArray();
                    return new WifiP2pUsdBasedServiceResponse(serviceProtocolType, ssi);
                }

                public WifiP2pUsdBasedServiceResponse[] newArray(int size) {
                    return new WifiP2pUsdBasedServiceResponse[size];
                }
            };
}
