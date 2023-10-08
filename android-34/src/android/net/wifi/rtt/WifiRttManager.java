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

package android.net.wifi.rtt;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.CHANGE_WIFI_STATE;
import static android.Manifest.permission.LOCATION_HARDWARE;
import static android.Manifest.permission.NEARBY_WIFI_DEVICES;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.StringDef;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Log;

import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * This class provides the primary API for measuring distance (range) to other devices using the
 * IEEE 802.11mc Wi-Fi Round Trip Time (RTT) technology.
 * <p>
 * The devices which can be ranged include:
 * <li>Access Points (APs)
 * <li>Wi-Fi Aware peers
 * <p>
 * Ranging requests are triggered using
 * {@link #startRanging(RangingRequest, Executor, RangingResultCallback)}. Results (in case of
 * successful operation) are returned in the {@link RangingResultCallback#onRangingResults(List)}
 * callback.
 * <p>
 *     Wi-Fi RTT may not be usable at some points, e.g. when Wi-Fi is disabled. To validate that
 *     the functionality is available use the {@link #isAvailable()} function. To track
 *     changes in RTT usability register for the {@link #ACTION_WIFI_RTT_STATE_CHANGED}
 *     broadcast. Note that this broadcast is not sticky - you should register for it and then
 *     check the above API to avoid a race condition.
 */
@SystemService(Context.WIFI_RTT_RANGING_SERVICE)
public class WifiRttManager {
    private static final String TAG = "WifiRttManager";
    private static final boolean VDBG = false;

    private final Context mContext;
    private final IWifiRttManager mService;

    /**
     * Broadcast intent action to indicate that the state of Wi-Fi RTT availability has changed.
     * Use the {@link #isAvailable()} to query the current status.
     * This broadcast is <b>not</b> sticky, use the {@link #isAvailable()} API after registering
     * the broadcast to check the current state of Wi-Fi RTT.
     * <p>Note: The broadcast is only delivered to registered receivers - no manifest registered
     * components will be launched.
     */
    @SdkConstant(SdkConstant.SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_WIFI_RTT_STATE_CHANGED =
            "android.net.wifi.rtt.action.WIFI_RTT_STATE_CHANGED";

    /**
     * Bundle key to access if one-sided Wi-Fi RTT is supported. When it is not supported, only
     * two-sided RTT can be performed, which requires responder supports IEEE 802.11mc and this can
     * be determined by the method {@link ScanResult#is80211mcResponder()}
     */
    public static final String CHARACTERISTICS_KEY_BOOLEAN_ONE_SIDED_RTT = "key_one_sided_rtt";
     /**
     * Bundle key to access if getting the Location Configuration Information(LCI) from responder is
      * supported.
     * @see ResponderLocation
     */
    public static final String CHARACTERISTICS_KEY_BOOLEAN_LCI = "key_lci";
    /**
     * Bundle key to access if getting the Location Civic Report(LCR) from responder is supported.
     * @see ResponderLocation
     */
    public static final String CHARACTERISTICS_KEY_BOOLEAN_LCR = "key_lcr";

    /**
     * Bundle key to access if device supports to be a responder in station mode
     */
    public static final String CHARACTERISTICS_KEY_BOOLEAN_STA_RESPONDER = "key_sta_responder";

    /** @hide */
    @StringDef(prefix = { "CHARACTERISTICS_KEY_"}, value = {
            CHARACTERISTICS_KEY_BOOLEAN_ONE_SIDED_RTT,
            CHARACTERISTICS_KEY_BOOLEAN_LCI,
            CHARACTERISTICS_KEY_BOOLEAN_LCR,
            CHARACTERISTICS_KEY_BOOLEAN_STA_RESPONDER,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RttCharacteristicsKey {}

    /** @hide */
    public WifiRttManager(@NonNull Context context, @NonNull IWifiRttManager service) {
        mContext = context;
        mService = service;
    }

    /**
     * Returns the current status of RTT API: whether or not RTT is available. To track
     * changes in the state of RTT API register for the
     * {@link #ACTION_WIFI_RTT_STATE_CHANGED} broadcast.
     * <p>Note: availability of RTT does not mean that the app can use the API. The app's
     * permissions and platform Location Mode are validated at run-time.
     *
     * @return A boolean indicating whether the app can use the RTT API at this time (true) or
     * not (false).
     */
    public boolean isAvailable() {
        try {
            return mService.isAvailable();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Initiate a request to range to a set of devices specified in the {@link RangingRequest}.
     * Results will be returned in the {@link RangingResultCallback} set of callbacks.
     * <p>
     * Ranging request with only Wifi Aware peers can be performed with either
     * {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation", or
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}. All other types of ranging requests
     * require {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param request  A request specifying a set of devices whose distance measurements are
     *                 requested.
     * @param executor The Executor on which to run the callback.
     * @param callback A callback for the result of the ranging request.
     */
    @RequiresPermission(allOf = {ACCESS_FINE_LOCATION, CHANGE_WIFI_STATE, ACCESS_WIFI_STATE,
            NEARBY_WIFI_DEVICES})
    public void startRanging(@NonNull RangingRequest request,
            @NonNull @CallbackExecutor Executor executor, @NonNull RangingResultCallback callback) {
        startRanging(null, request, executor, callback);
    }

    /**
     * Initiate a request to range to a set of devices specified in the {@link RangingRequest}.
     * Results will be returned in the {@link RangingResultCallback} set of callbacks.
     * <p>
     * Ranging request with only Wifi Aware peers can be performed with either
     * {@link android.Manifest.permission#NEARBY_WIFI_DEVICES} with
     * android:usesPermissionFlags="neverForLocation", or
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}. All other types of ranging requests
     * require {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * @param workSource A mechanism to specify an alternative work-source for the request.
     * @param request  A request specifying a set of devices whose distance measurements are
     *                 requested.
     * @param executor The Executor on which to run the callback.
     * @param callback A callback for the result of the ranging request.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {LOCATION_HARDWARE, ACCESS_FINE_LOCATION, CHANGE_WIFI_STATE,
            ACCESS_WIFI_STATE, NEARBY_WIFI_DEVICES}, conditional = true)
    public void startRanging(@Nullable WorkSource workSource, @NonNull RangingRequest request,
            @NonNull @CallbackExecutor Executor executor, @NonNull RangingResultCallback callback) {
        if (VDBG) {
            Log.v(TAG, "startRanging: workSource=" + workSource + ", request=" + request
                    + ", callback=" + callback + ", executor=" + executor);
        }

        if (executor == null) {
            throw new IllegalArgumentException("Null executor provided");
        }
        if (callback == null) {
            throw new IllegalArgumentException("Null callback provided");
        }

        Binder binder = new Binder();
        try {
            Bundle extras = new Bundle();
            if (SdkLevel.isAtLeastS()) {
                extras.putParcelable(WifiManager.EXTRA_PARAM_KEY_ATTRIBUTION_SOURCE,
                        mContext.getAttributionSource());
            }
            mService.startRanging(binder, mContext.getOpPackageName(),
                    mContext.getAttributionTag(), workSource, request, new IRttCallback.Stub() {
                        @Override
                        public void onRangingFailure(int status) throws RemoteException {
                            clearCallingIdentity();
                            executor.execute(() -> callback.onRangingFailure(status));
                        }

                        @Override
                        public void onRangingResults(List<RangingResult> results)
                                throws RemoteException {
                            clearCallingIdentity();
                            executor.execute(() -> callback.onRangingResults(results));
                        }
                    }, extras);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Cancel all ranging requests for the specified work sources. The requests have been requested
     * using {@link #startRanging(WorkSource, RangingRequest, Executor, RangingResultCallback)}.
     *
     * @param workSource The work-sources of the requesters.
     *
     * @hide
     */
    @SystemApi
    @RequiresPermission(allOf = {LOCATION_HARDWARE})
    public void cancelRanging(@Nullable WorkSource workSource) {
        if (VDBG) {
            Log.v(TAG, "cancelRanging: workSource=" + workSource);
        }

        try {
            mService.cancelRanging(workSource);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a Bundle which represents the characteristics of the Wi-Fi RTT interface: a set of
     * parameters which specify feature support. Each parameter can be accessed by the specified
     * Bundle key, one of the {@code CHARACTERISTICS_KEY_*} value.
     * <p>
     * May return an empty Bundle if the Wi-Fi RTT service is not initialized.
     *
     * @return A Bundle specifying feature support of RTT.
     */
    @RequiresPermission(ACCESS_WIFI_STATE)
    @NonNull
    public Bundle getRttCharacteristics() {
        try {
            return mService.getRttCharacteristics();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
