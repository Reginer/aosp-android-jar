/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.net.metrics;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import com.android.internal.util.MessageUtils;

/**
 * Event class used to record error events when parsing DHCP response packets.
 * {@hide}
 * @deprecated The event may not be sent in Android S and above. The events
 * are logged by a single caller in the system using signature permissions
 * and that caller is migrating to statsd.
 */
@Deprecated
@SystemApi
public final class DhcpErrorEvent implements IpConnectivityLog.Event {
    public static final int L2_ERROR   = 1;
    public static final int L3_ERROR   = 2;
    public static final int L4_ERROR   = 3;
    public static final int DHCP_ERROR = 4;
    public static final int MISC_ERROR = 5;

    // error code byte format (MSB to LSB):
    // byte 0: error type
    // byte 1: error subtype
    // byte 2: unused
    // byte 3: optional code
    /** @hide */
    public final int errorCode;

    private static final int L2_ERROR_TYPE = L2_ERROR << 8;
    private static final int L3_ERROR_TYPE = L3_ERROR << 8;
    private static final int L4_ERROR_TYPE = L4_ERROR << 8;
    private static final int DHCP_ERROR_TYPE = DHCP_ERROR << 8;
    private static final int MISC_ERROR_TYPE = MISC_ERROR << 8;

    public static final int L2_TOO_SHORT               = (L2_ERROR_TYPE | 0x1) << 16;
    public static final int L2_WRONG_ETH_TYPE          = (L2_ERROR_TYPE | 0x2) << 16;

    public static final int L3_TOO_SHORT               = (L3_ERROR_TYPE | 0x1) << 16;
    public static final int L3_NOT_IPV4                = (L3_ERROR_TYPE | 0x2) << 16;
    public static final int L3_INVALID_IP              = (L3_ERROR_TYPE | 0x3) << 16;

    public static final int L4_NOT_UDP                 = (L4_ERROR_TYPE | 0x1) << 16;
    public static final int L4_WRONG_PORT              = (L4_ERROR_TYPE | 0x2) << 16;

    public static final int BOOTP_TOO_SHORT            = (DHCP_ERROR_TYPE | 0x1) << 16;
    public static final int DHCP_BAD_MAGIC_COOKIE      = (DHCP_ERROR_TYPE | 0x2) << 16;
    public static final int DHCP_INVALID_OPTION_LENGTH = (DHCP_ERROR_TYPE | 0x3) << 16;
    public static final int DHCP_NO_MSG_TYPE           = (DHCP_ERROR_TYPE | 0x4) << 16;
    public static final int DHCP_UNKNOWN_MSG_TYPE      = (DHCP_ERROR_TYPE | 0x5) << 16;
    public static final int DHCP_NO_COOKIE             = (DHCP_ERROR_TYPE | 0x6) << 16;

    public static final int BUFFER_UNDERFLOW           = (MISC_ERROR_TYPE | 0x1) << 16;
    public static final int RECEIVE_ERROR              = (MISC_ERROR_TYPE | 0x2) << 16;
    public static final int PARSING_ERROR              = (MISC_ERROR_TYPE | 0x3) << 16;

    public DhcpErrorEvent(int errorCode) {
        this.errorCode = errorCode;
    }

    private DhcpErrorEvent(Parcel in) {
        this.errorCode = in.readInt();
    }

    /** @hide */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(errorCode);
    }

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    public static final @android.annotation.NonNull Parcelable.Creator<DhcpErrorEvent> CREATOR
        = new Parcelable.Creator<DhcpErrorEvent>() {
        public DhcpErrorEvent createFromParcel(Parcel in) {
            return new DhcpErrorEvent(in);
        }

        public DhcpErrorEvent[] newArray(int size) {
            return new DhcpErrorEvent[size];
        }
    };

    public static int errorCodeWithOption(int errorCode, int option) {
        return (0xFFFF0000 & errorCode) | (0xFF & option);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("DhcpErrorEvent(%s)", Decoder.constants.get(errorCode));
    }

    final static class Decoder {
        static final SparseArray<String> constants = MessageUtils.findMessageNames(
                new Class[]{DhcpErrorEvent.class},
                new String[]{"L2_", "L3_", "L4_", "BOOTP_", "DHCP_", "BUFFER_", "RECEIVE_",
                "PARSING_"});
    }
}
