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

package android.net.wifi.twt;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;

import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * API interface for target wake time (TWT) session Callback.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
public interface TwtSessionCallback {
    /**
     * Generic error
     */
    int TWT_ERROR_CODE_FAIL = 0;
    /**
     * AP does not support TWT
     */
    int TWT_ERROR_CODE_AP_NOT_SUPPORTED = 1;
    /**
     * AP is blocklisted due to interoperability issue reported with TWT
     */
    int TWT_ERROR_CODE_AP_OUI_BLOCKLISTED = 2;
    /**
     * AP rejects TWT request
     */
    int TWT_ERROR_CODE_AP_REJECTED = 3;
    /**
     * Invalid parameters
     */
    int TWT_ERROR_CODE_INVALID_PARAMS = 4;
    /**
     * Maximum TWT sessions reached
     */
    int TWT_ERROR_CODE_MAX_SESSIONS_REACHED = 5;
    /**
     * TWT is not available now
     */
    int TWT_ERROR_CODE_NOT_AVAILABLE = 6;
    /**
     * TWT is not supported by the local device
     */
    int TWT_ERROR_CODE_NOT_SUPPORTED = 7;
    /**
     * TWT operation Timed out
     */
    int TWT_ERROR_CODE_TIMEOUT = 8;

    /**
     * @hide
     */
    @IntDef(prefix = {"TWT_ERROR_CODE_"}, value = {TWT_ERROR_CODE_FAIL,
            TWT_ERROR_CODE_AP_NOT_SUPPORTED, TWT_ERROR_CODE_AP_OUI_BLOCKLISTED,
            TWT_ERROR_CODE_AP_REJECTED, TWT_ERROR_CODE_INVALID_PARAMS,
            TWT_ERROR_CODE_MAX_SESSIONS_REACHED, TWT_ERROR_CODE_NOT_AVAILABLE,
            TWT_ERROR_CODE_NOT_SUPPORTED, TWT_ERROR_CODE_TIMEOUT})
    @Retention(RetentionPolicy.SOURCE)
    @interface TwtErrorCode {
    }

    /**
     * Unknown reason code
     */
    int TWT_REASON_CODE_UNKNOWN = 0;
    /**
     * Locally requested
     */
    int TWT_REASON_CODE_LOCALLY_REQUESTED = 1;
    /**
     * Internally initiated by the driver or firmware
     */
    int TWT_REASON_CODE_INTERNALLY_INITIATED = 2;
    /**
     * Peer initiated
     */
    int TWT_REASON_CODE_PEER_INITIATED = 3;

    /**
     * @hide
     */
    @IntDef(prefix = {"TWT_REASON_CODE_"}, value = {TWT_REASON_CODE_UNKNOWN,
            TWT_REASON_CODE_LOCALLY_REQUESTED, TWT_REASON_CODE_INTERNALLY_INITIATED,
            TWT_REASON_CODE_PEER_INITIATED})
    @Retention(RetentionPolicy.SOURCE)
    @interface TwtReasonCode {
    }

    /**
     * Called when a TWT operation fails.
     *
     * @param errorCode error code
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    void onFailure(@TwtSessionCallback.TwtErrorCode int errorCode);

    /**
     * Called when a TWT session is torn down or closed. Check the
     * {@link TwtReasonCode} for more details.
     *
     * @param reasonCode reason for TWT session teardown
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    void onTeardown(@TwtSessionCallback.TwtReasonCode int reasonCode);

    /**
     * Called when the TWT session is created.
     *
     * @param twtSession TWT session
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    void onCreate(@NonNull TwtSession twtSession);
}
