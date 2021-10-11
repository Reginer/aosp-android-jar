/*
 * Copyright 2017 The Android Open Source Project
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

package com.android.internal.telephony.dataconnection;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class serves to pass around the parameters of Keepalive session
 * status within the telephony framework.
 *
 * {@hide}
 */
public class KeepaliveStatus implements Parcelable {
    private static final String LOG_TAG = "KeepaliveStatus";

    /** This should match the HAL Radio::1_1::KeepaliveStatusCode */
    public static final int STATUS_ACTIVE = 0;
    public static final int STATUS_INACTIVE = 1;
    public static final int STATUS_PENDING = 2;

    public static final int ERROR_NONE = 0;
    public static final int ERROR_UNSUPPORTED = 1;
    public static final int ERROR_NO_RESOURCES = 2;
    public static final int ERROR_UNKNOWN = 3;

    public static final int INVALID_HANDLE = Integer.MAX_VALUE;

    /** An opaque value that identifies this Keepalive status to the modem */
    public final int sessionHandle;

    /**
     * A status code indicating whether this Keepalive session is
     * active, inactive, or pending activation
     */
    public final int statusCode;

    /** An error code indicating a lower layer failure, if any */
    public final int errorCode;

    public KeepaliveStatus(int error) {
        sessionHandle = INVALID_HANDLE;
        statusCode = STATUS_INACTIVE;
        errorCode = error;
    }

    public KeepaliveStatus(int handle, int code) {
        sessionHandle = handle;
        statusCode = code;
        errorCode = ERROR_NONE;
    }


    @Override
    public String toString() {
        return String.format("{errorCode=%d, sessionHandle=%d, statusCode=%d}",
                errorCode, sessionHandle, statusCode);
    }

    // Parcelable Implementation
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(errorCode);
        dest.writeInt(sessionHandle);
        dest.writeInt(statusCode);
    }

    private KeepaliveStatus(Parcel p) {
        errorCode = p.readInt();
        sessionHandle = p.readInt();
        statusCode = p.readInt();
    }

    public static final Parcelable.Creator<KeepaliveStatus> CREATOR =
            new Parcelable.Creator<KeepaliveStatus>() {
                @Override
                public KeepaliveStatus createFromParcel(Parcel source) {
                    return new KeepaliveStatus(source);
                }

                @Override
                public KeepaliveStatus[] newArray(int size) {
                    return new KeepaliveStatus[size];
                }
            };
}
