/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.internal.telephony;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RegistrantList;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.util.Pair;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.util.WorkerThread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class is used to detect the country where the device is.
 *
 * {@link LocaleTracker} also tracks country of a device based on the information provided by
 * network operators. However, it won't work when a device is out of service. In such cases and if
 * Wi-Fi is available, {@link Geocoder} can be used to query the country for the current location of
 * the device. {@link TelephonyCountryDetector} uses both {@link LocaleTracker} and {@link Geocoder}
 * to track country of a device.
 */
public class TelephonyCountryDetector extends Handler {
    private static final String TAG = "TelephonyCountryDetector";
    private static final String ALLOW_MOCK_MODEM_PROPERTY = "persist.radio.allow_mock_modem";
    private static final String BOOT_ALLOW_MOCK_MODEM_PROPERTY = "ro.boot.radio.allow_mock_modem";
    private static final boolean DEBUG = !"user".equals(Build.TYPE);
    private static final int EVENT_LOCATION_CHANGED = 1;
    private static final int EVENT_LOCATION_COUNTRY_CODE_CHANGED = 2;
    private static final int EVENT_NETWORK_COUNTRY_CODE_CHANGED = 3;
    private static final int EVENT_WIFI_CONNECTIVITY_STATE_CHANGED = 4;
    private static final int EVENT_LOCATION_UPDATE_REQUEST_QUOTA_RESET = 5;

    // Wait 12 hours between location updates
    private static final long TIME_BETWEEN_LOCATION_UPDATES_MILLIS = TimeUnit.HOURS.toMillis(12);
    // Minimum distance before a location update is triggered, in meters. We don't need this to be
    // too exact because all we care about is in what country the device is.
    private static final float DISTANCE_BETWEEN_LOCATION_UPDATES_METERS = 2000;
    protected static final long WAIT_FOR_LOCATION_UPDATE_REQUEST_QUOTA_RESET_TIMEOUT_MILLIS =
            TimeUnit.MINUTES.toMillis(30);

    private static TelephonyCountryDetector sInstance;

    @NonNull private final Geocoder mGeocoder;
    @NonNull private final LocationManager mLocationManager;
    @NonNull private final ConnectivityManager mConnectivityManager;
    @NonNull private final RegistrantList mWifiConnectivityStateChangedRegistrantList =
            new RegistrantList();
    @NonNull private final Object mLock = new Object();
    @NonNull
    @GuardedBy("mLock")
    private final Map<Integer, NetworkCountryCodeInfo> mNetworkCountryCodeInfoPerPhone =
            new HashMap<>();
    @GuardedBy("mLock")
    private String mLocationCountryCode = null;
    /** This should be used by CTS only */
    @GuardedBy("mLock")
    private String mOverriddenLocationCountryCode = null;
    @GuardedBy("mLock")
    private boolean mIsLocationUpdateRequested = false;
    @GuardedBy("mLock")
    private long mLocationCountryCodeUpdatedTimestampNanos = 0;
    /** This should be used by CTS only */
    @GuardedBy("mLock")
    private long mOverriddenLocationCountryCodeUpdatedTimestampNanos = 0;
    @GuardedBy("mLock")
    private List<String> mOverriddenCurrentNetworkCountryCodes = null;
    @GuardedBy("mLock")
    private Map<String, Long> mOverriddenCachedNetworkCountryCodes = new HashMap<>();
    @GuardedBy("mLock")
    private boolean mIsCountryCodesOverridden = false;
    private final RegistrantList mCountryCodeChangedRegistrants = new RegistrantList();
    private boolean mIsWifiNetworkConnected = false;

    private FeatureFlags mFeatureFlags = null;

    @NonNull private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            logd("onLocationChanged: " + (location != null));
            if (location != null) {
                sendRequestAsync(EVENT_LOCATION_CHANGED, location);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            logd("onProviderDisabled: provider=" + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            logd("onProviderEnabled: provider=" + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            logd("onStatusChanged: provider=" + provider + ", status=" + status
                    + ", extras=" + extras);
        }
    };

    private class TelephonyGeocodeListener implements Geocoder.GeocodeListener {
        private long mLocationUpdatedTime;
        TelephonyGeocodeListener(long locationUpdatedTime) {
            mLocationUpdatedTime = locationUpdatedTime;
        }

        @Override
        public void onGeocode(List<Address> addresses) {
            if (addresses != null && !addresses.isEmpty()) {
                logd("onGeocode: addresses is available");
                String countryCode = addresses.get(0).getCountryCode();
                sendRequestAsync(EVENT_LOCATION_COUNTRY_CODE_CHANGED,
                        new Pair<>(countryCode, mLocationUpdatedTime));
            } else {
                logd("onGeocode: addresses is not available");
            }
        }

        @Override
        public void onError(String errorMessage) {
            loge("GeocodeListener.onError=" + errorMessage);
        }
    }

    /**
     * Container class to store country code per Phone.
     */
    private static class NetworkCountryCodeInfo {
        public int phoneId;
        public String countryCode;
        public long timestamp;

        @Override
        public String toString() {
            return "NetworkCountryCodeInfo[phoneId: " + phoneId
                    + ", countryCode: " + countryCode
                    + ", timestamp: " + timestamp + "]";
        }
    }

    /**
     * Create the singleton instance of {@link TelephonyCountryDetector}.
     *
     * @param looper The looper to run the {@link TelephonyCountryDetector} instance.
     * @param context The context used by the instance.
     * @param locationManager The LocationManager instance.
     * @param connectivityManager The ConnectivityManager instance.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected TelephonyCountryDetector(@NonNull Looper looper, @NonNull Context context,
            @NonNull LocationManager locationManager,
            @NonNull ConnectivityManager connectivityManager,
            FeatureFlags featureFlags) {
        super(looper);
        mLocationManager = locationManager;
        mGeocoder = new Geocoder(context);
        mConnectivityManager = connectivityManager;
        mFeatureFlags = featureFlags;
        initialize();
    }

    /** @return the singleton instance of the {@link TelephonyCountryDetector} */
    public static synchronized TelephonyCountryDetector getInstance(@NonNull Context context,
            FeatureFlags featureFlags) {
        if (sInstance == null) {
            if (featureFlags.threadShred()) {
                sInstance = new TelephonyCountryDetector(WorkerThread.get().getLooper(), context,
                        context.getSystemService(LocationManager.class),
                        context.getSystemService(ConnectivityManager.class),
                        featureFlags);
            } else {
                HandlerThread handlerThread = new HandlerThread("TelephonyCountryDetector");
                handlerThread.start();
                sInstance = new TelephonyCountryDetector(handlerThread.getLooper(), context,
                        context.getSystemService(LocationManager.class),
                        context.getSystemService(ConnectivityManager.class),
                        featureFlags);
            }
        }
        return sInstance;
    }

    /**
     * @return The list of current network country ISOs if available, an empty list otherwise.
     */
    @NonNull public List<String> getCurrentNetworkCountryIso() {
        synchronized (mLock) {
            if (mIsCountryCodesOverridden) {
                logd("mOverriddenCurrentNetworkCountryCodes="
                        + String.join(", ", mOverriddenCurrentNetworkCountryCodes));
                return mOverriddenCurrentNetworkCountryCodes;
            }
        }

        List<String> result = new ArrayList<>();
        for (Phone phone : PhoneFactory.getPhones()) {
            String countryIso = getNetworkCountryIsoForPhone(phone);
            if (isValid(countryIso)) {
                String countryIsoInUpperCase = countryIso.toUpperCase(Locale.US);
                if (!result.contains(countryIsoInUpperCase)) {
                    result.add(countryIsoInUpperCase);
                }
            } else {
                logd("getCurrentNetworkCountryIso: invalid countryIso=" + countryIso
                        + " for phoneId=" + phone.getPhoneId() + ", subId=" + phone.getSubId());
            }
        }
        return result;
    }

    /**
     * @return The cached location country code and its updated timestamp.
     */
    @NonNull public Pair<String, Long> getCachedLocationCountryIsoInfo() {
        synchronized (mLock) {
            if (mIsCountryCodesOverridden) {
                logd("mOverriddenLocationCountryCode=" + mOverriddenLocationCountryCode
                        + " will be used");
                return new Pair<>(mOverriddenLocationCountryCode,
                        mOverriddenLocationCountryCodeUpdatedTimestampNanos);
            }
            return new Pair<>(mLocationCountryCode, mLocationCountryCodeUpdatedTimestampNanos);
        }
    }

    /**
     * This API should be used only when {@link #getCurrentNetworkCountryIso()} returns an empty
     * list.
     *
     * @return The list of cached network country codes and their updated timestamps.
     */
    @NonNull public Map<String, Long> getCachedNetworkCountryIsoInfo() {
        synchronized (mLock) {
            if (mIsCountryCodesOverridden) {
                logd("mOverriddenCachedNetworkCountryCodes = "
                        + String.join(", ", mOverriddenCachedNetworkCountryCodes.keySet())
                        + " will be used");
                return mOverriddenCachedNetworkCountryCodes;
            }
            Map<String, Long> result = new HashMap<>();
            for (NetworkCountryCodeInfo countryCodeInfo :
                    mNetworkCountryCodeInfoPerPhone.values()) {
                boolean alreadyAdded = result.containsKey(countryCodeInfo.countryCode);
                if (!alreadyAdded || (alreadyAdded
                        && result.get(countryCodeInfo.countryCode) < countryCodeInfo.timestamp)) {
                    result.put(countryCodeInfo.countryCode, countryCodeInfo.timestamp);
                }
            }
            return result;
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_LOCATION_CHANGED:
                queryCountryCodeForLocation((Location) msg.obj);
                break;
            case EVENT_LOCATION_COUNTRY_CODE_CHANGED:
                setLocationCountryCode((Pair) msg.obj);
                break;
            case EVENT_NETWORK_COUNTRY_CODE_CHANGED:
                handleNetworkCountryCodeChangedEvent((NetworkCountryCodeInfo) msg.obj);
                break;
            case EVENT_WIFI_CONNECTIVITY_STATE_CHANGED:
                handleEventWifiConnectivityStateChanged((boolean) msg.obj);
                break;
            case EVENT_LOCATION_UPDATE_REQUEST_QUOTA_RESET:
                evaluateRequestingLocationUpdates();
                break;
            default:
                logw("CountryDetectorHandler: unexpected message code: " + msg.what);
                break;
        }
    }

    /**
     * This API is called by {@link LocaleTracker} whenever there is a change in network country
     * code of a phone.
     */
    public void onNetworkCountryCodeChanged(
            @NonNull Phone phone, @Nullable String currentCountryCode) {
        NetworkCountryCodeInfo networkCountryCodeInfo = new NetworkCountryCodeInfo();
        networkCountryCodeInfo.phoneId = phone.getPhoneId();
        networkCountryCodeInfo.countryCode = currentCountryCode;
        sendRequestAsync(EVENT_NETWORK_COUNTRY_CODE_CHANGED, networkCountryCodeInfo);
    }

    /**
     * This API should be used by only CTS tests to forcefully set the telephony country codes.
     */
    public boolean setCountryCodes(boolean reset, @NonNull List<String> currentNetworkCountryCodes,
            @NonNull Map<String, Long> cachedNetworkCountryCodes, String locationCountryCode,
            long locationCountryCodeTimestampNanos) {
        if (!isMockModemAllowed()) {
            logd("setCountryCodes: mock modem is not allowed");
            return false;
        }
        logd("setCountryCodes: currentNetworkCountryCodes="
                + String.join(", ", currentNetworkCountryCodes)
                + ", locationCountryCode=" + locationCountryCode
                + ", locationCountryCodeTimestampNanos" + locationCountryCodeTimestampNanos
                + ", reset=" + reset + ", cachedNetworkCountryCodes="
                + String.join(", ", cachedNetworkCountryCodes.keySet()));

        synchronized (mLock) {
            if (reset) {
                mIsCountryCodesOverridden = false;
            } else {
                mIsCountryCodesOverridden = true;
                mOverriddenCachedNetworkCountryCodes = cachedNetworkCountryCodes;
                mOverriddenCurrentNetworkCountryCodes = currentNetworkCountryCodes;
                mOverriddenLocationCountryCode = locationCountryCode;
                mOverriddenLocationCountryCodeUpdatedTimestampNanos =
                        locationCountryCodeTimestampNanos;
            }
        }
        return true;
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected void queryCountryCodeForLocation(@NonNull Location location) {
        mGeocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1,
                new TelephonyGeocodeListener(location.getElapsedRealtimeNanos()));
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    protected long getElapsedRealtimeNanos() {
        return SystemClock.elapsedRealtimeNanos();
    }

    private void initialize() {
        evaluateRequestingLocationUpdates();
        registerForWifiConnectivityStateChanged();
    }

    private boolean isGeoCoderImplemented() {
        return Geocoder.isPresent();
    }

    private void registerForLocationUpdates() {
        // If the device does not implement Geocoder, there is no point trying to get location
        // updates because we cannot retrieve the country based on the location anyway.
        if (!isGeoCoderImplemented()) {
            logd("Geocoder is not implemented on the device");
            return;
        }

        synchronized (mLock) {
            if (mIsLocationUpdateRequested) {
                logd("Already registered for location updates");
                return;
            }

            logd("Registering for location updates");
            /*
             * PASSIVE_PROVIDER can be used to passively receive location updates when other
             * applications or services request them without actually requesting the locations
             * ourselves. This provider will only return locations generated by other providers.
             * This provider is used to make sure there is no impact on the thermal and battery of
             * a device.
             */
            mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
                    TIME_BETWEEN_LOCATION_UPDATES_MILLIS, DISTANCE_BETWEEN_LOCATION_UPDATES_METERS,
                    mLocationListener);
            mIsLocationUpdateRequested = true;
            mLocationListener.onLocationChanged(getLastKnownLocation());
        }
    }

    @Nullable
    private Location getLastKnownLocation() {
        Location result = null;
        for (String provider : mLocationManager.getProviders(true)) {
            Location location = mLocationManager.getLastKnownLocation(provider);
            if (location != null && (result == null
                    || result.getElapsedRealtimeNanos() < location.getElapsedRealtimeNanos())) {
                result = location;
            }
        }
        return result;
    }

    private void unregisterForLocationUpdates() {
        synchronized (mLock) {
            if (!mIsLocationUpdateRequested) {
                logd("Location update was not requested yet");
                return;
            }
            if (isLocationUpdateRequestQuotaExceeded()) {
                logd("Removing location updates will be re-evaluated after the quota is refilled");
                return;
            }
            mLocationManager.removeUpdates(mLocationListener);
            mIsLocationUpdateRequested = false;
            sendMessageDelayed(obtainMessage(EVENT_LOCATION_UPDATE_REQUEST_QUOTA_RESET),
                    WAIT_FOR_LOCATION_UPDATE_REQUEST_QUOTA_RESET_TIMEOUT_MILLIS);
        }
    }

    private boolean isLocationUpdateRequestQuotaExceeded() {
        return hasMessages(EVENT_LOCATION_UPDATE_REQUEST_QUOTA_RESET);
    }

    private boolean shouldRequestLocationUpdate() {
        return getCurrentNetworkCountryIso().isEmpty() && isWifiNetworkConnected();
    }

    /**
     * Posts the specified command to be executed on the main thread and returns immediately.
     *
     * @param command command to be executed on the main thread
     * @param argument additional parameters required to perform of the operation
     */
    private void sendRequestAsync(int command, @NonNull Object argument) {
        Message msg = this.obtainMessage(command, argument);
        msg.sendToTarget();
    }

    private void handleNetworkCountryCodeChangedEvent(
            @NonNull NetworkCountryCodeInfo currentNetworkCountryCodeInfo) {
        logd("currentNetworkCountryCodeInfo=" + currentNetworkCountryCodeInfo);
        if (isValid(currentNetworkCountryCodeInfo.countryCode)) {
            synchronized (mLock) {
                NetworkCountryCodeInfo cachedNetworkCountryCodeInfo =
                        mNetworkCountryCodeInfoPerPhone.computeIfAbsent(
                                currentNetworkCountryCodeInfo.phoneId,
                                k -> new NetworkCountryCodeInfo());
                cachedNetworkCountryCodeInfo.phoneId = currentNetworkCountryCodeInfo.phoneId;
                cachedNetworkCountryCodeInfo.timestamp = getElapsedRealtimeNanos();
                cachedNetworkCountryCodeInfo.countryCode =
                        currentNetworkCountryCodeInfo.countryCode.toUpperCase(Locale.US);
            }
        } else {
            logd("handleNetworkCountryCodeChangedEvent: Got invalid or empty country code for "
                    + "phoneId=" + currentNetworkCountryCodeInfo.phoneId);
            synchronized (mLock) {
                if (mNetworkCountryCodeInfoPerPhone.containsKey(
                        currentNetworkCountryCodeInfo.phoneId)) {
                    // The country code has changed from valid to invalid. Thus, we need to update
                    // the last valid timestamp.
                    NetworkCountryCodeInfo cachedNetworkCountryCodeInfo =
                            mNetworkCountryCodeInfoPerPhone.get(
                                    currentNetworkCountryCodeInfo.phoneId);
                    cachedNetworkCountryCodeInfo.timestamp = getElapsedRealtimeNanos();
                }
            }
        }
        evaluateRequestingLocationUpdates();
        logd("mCountryCodeChangedRegistrants.notifyRegistrants()");
        mCountryCodeChangedRegistrants.notifyRegistrants();
    }

    private void handleEventWifiConnectivityStateChanged(boolean connected) {
        logd("handleEventWifiConnectivityStateChanged: " + connected);
        evaluateNotifyWifiConnectivityStateChangedEvent(connected);
        evaluateRequestingLocationUpdates();
    }

    private void evaluateNotifyWifiConnectivityStateChangedEvent(boolean connected) {
        if (connected != mIsWifiNetworkConnected) {
            mIsWifiNetworkConnected = connected;
            mWifiConnectivityStateChangedRegistrantList.notifyResult(mIsWifiNetworkConnected);
            logd("evaluateNotifyWifiConnectivityStateChangedEvent: wifi connectivity state has "
                    + "changed to " + connected);
        }
    }

    private void setLocationCountryCode(@NonNull Pair<String, Long> countryCodeInfo) {
        logd("Set location country code to: " + countryCodeInfo.first);
        if (!isValid(countryCodeInfo.first)) {
            logd("Received invalid location country code");
        } else {
            synchronized (mLock) {
                mLocationCountryCode = countryCodeInfo.first.toUpperCase(Locale.US);
                mLocationCountryCodeUpdatedTimestampNanos = countryCodeInfo.second;
            }
        }
    }

    private String getNetworkCountryIsoForPhone(@NonNull Phone phone) {
        ServiceStateTracker serviceStateTracker = phone.getServiceStateTracker();
        if (serviceStateTracker == null) {
            logw("getNetworkCountryIsoForPhone: serviceStateTracker is null");
            return null;
        }

        LocaleTracker localeTracker = serviceStateTracker.getLocaleTracker();
        if (localeTracker == null) {
            logw("getNetworkCountryIsoForPhone: localeTracker is null");
            return null;
        }

        return localeTracker.getCurrentCountry();
    }

    private void registerForWifiConnectivityStateChanged() {
        logd("registerForWifiConnectivityStateChanged");
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        ConnectivityManager.NetworkCallback networkCallback =
                new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onCapabilitiesChanged(Network network,
                            NetworkCapabilities networkCapabilities) {
                        logd("onCapabilitiesChanged: " + networkCapabilities);
                        sendRequestAsync(EVENT_WIFI_CONNECTIVITY_STATE_CHANGED,
                                isInternetAvailable(networkCapabilities));
                    }

                    @Override
                    public void onLost(Network network) {
                        logd("Wifi network lost: " + network);
                        sendRequestAsync(EVENT_WIFI_CONNECTIVITY_STATE_CHANGED, false);
                    }
                };
        mConnectivityManager.registerNetworkCallback(builder.build(), networkCallback);
    }

    private void evaluateRequestingLocationUpdates() {
        if (shouldRequestLocationUpdate()) {
            registerForLocationUpdates();
        } else {
            unregisterForLocationUpdates();
        }
    }

    /**
     * Check whether Wi-Fi network is connected or not.
     * @return {@code true} is Wi-Fi is connected, and internet is available, {@code false}
     * otherwise.
     */
    public boolean isWifiNetworkConnected() {
        logd("isWifiNetworkConnected: " + mIsWifiNetworkConnected);
        return mIsWifiNetworkConnected;
    }

    private boolean isInternetAvailable(NetworkCapabilities networkCapabilities) {
        boolean isWifiConnected =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        logd("isWifiConnected: " + isWifiConnected);
        return isWifiConnected;
    }

    /**
     * Register a callback to receive Wi-Fi connectivity state changes.
     * @param h Handler for notification message
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForWifiConnectivityStateChanged(@NonNull Handler h, int what,
            @Nullable Object obj) {
        mWifiConnectivityStateChangedRegistrantList.add(h, what, obj);
    }

    /**
     * Unregisters for Wi-Fi connectivity state changes.
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForWifiConnectivityStateChanged(@NonNull Handler h) {
        mWifiConnectivityStateChangedRegistrantList.remove(h);
    }

    /**
     * Check whether this is a valid country code.
     *
     * @param countryCode A 2-Character alphanumeric country code.
     * @return {@code true} if the countryCode is valid, {@code false} otherwise.
     */
    private static boolean isValid(String countryCode) {
        return countryCode != null && countryCode.length() == 2
                && countryCode.chars().allMatch(Character::isLetterOrDigit);
    }

    private static boolean isMockModemAllowed() {
        return (DEBUG || SystemProperties.getBoolean(ALLOW_MOCK_MODEM_PROPERTY, false)
                || SystemProperties.getBoolean(BOOT_ALLOW_MOCK_MODEM_PROPERTY, false));
    }

    /**
     * Register a callback for country code changed events
     *
     * @param h    Handler to notify
     * @param what msg.what when the message is delivered
     * @param obj  AsyncResult.userObj when the message is delivered
     */
    public void registerForCountryCodeChanged(Handler h, int what, Object obj) {
        mCountryCodeChangedRegistrants.add(h, what, obj);
    }

    /**
     * Unregister a callback for country code changed events
     *
     * @param h Handler to notifyf
     */
    public void unregisterForCountryCodeChanged(Handler h) {
        mCountryCodeChangedRegistrants.remove(h);
    }

    private static void logd(@NonNull String log) {
        Rlog.d(TAG, log);
    }

    private static void logw(@NonNull String log) {
        Rlog.w(TAG, log);
    }

    private static void loge(@NonNull String log) {
        Rlog.e(TAG, log);
    }
}
