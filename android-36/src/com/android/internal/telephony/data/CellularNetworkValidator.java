/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.telephony.data;

import static android.telephony.AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
import static android.telephony.CarrierConfigManager.KEY_DATA_SWITCH_VALIDATION_MIN_INTERVAL_MILLIS_LONG;
import static android.telephony.NetworkRegistrationInfo.DOMAIN_PS;

import android.annotation.NonNull;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.TelephonyNetworkSpecifier;
import android.os.Handler;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConfigurationManager;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent;
import com.android.internal.telephony.subscription.SubscriptionInfoInternal;
import com.android.internal.telephony.subscription.SubscriptionManagerService;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class will validate whether cellular network verified by Connectivity's
 * validation process. It listens request on a specific subId, sends a network request
 * to Connectivity and listens to its callback or timeout.
 */
public class CellularNetworkValidator {
    private static final String LOG_TAG = "NetworkValidator";

    // States of validator. Only one validation can happen at once.
    // IDLE: no validation going on.
    private static final int STATE_IDLE                = 0;
    // VALIDATING: validation going on.
    private static final int STATE_VALIDATING          = 1;
    // VALIDATED: validation is done and successful.
    // Waiting for stopValidation() to release
    // validation NetworkRequest.
    private static final int STATE_VALIDATED           = 2;

    // Singleton instance.
    private static CellularNetworkValidator sInstance;
    @VisibleForTesting
    public static final long MAX_VALIDATION_CACHE_TTL = TimeUnit.DAYS.toMillis(1);

    private int mState = STATE_IDLE;
    private int mSubId;
    private boolean mRequireTestPass;

    private ValidationCallback mValidationCallback;
    private final Context mContext;
    private final ConnectivityManager mConnectivityManager;
    @VisibleForTesting
    public Handler mHandler = new Handler();
    @VisibleForTesting
    public ConnectivityNetworkCallback mNetworkCallback;
    private final ValidatedNetworkCache mValidatedNetworkCache = new ValidatedNetworkCache();

    @NonNull
    private final FeatureFlags mFlags;
    private class ValidatedNetworkCache {
        // A cache with fixed size. It remembers 10 most recently successfully validated networks.
        private static final int VALIDATED_NETWORK_CACHE_SIZE = 10;
        private final PriorityQueue<ValidatedNetwork> mValidatedNetworkPQ =
                new PriorityQueue<>((Comparator<ValidatedNetwork>) Comparator.comparingLong(
                        (ValidatedNetwork n) -> n.mValidationTimeStamp));
        private final Map<String, ValidatedNetwork> mValidatedNetworkMap = new HashMap<>();

        private static final class ValidatedNetwork {
            ValidatedNetwork(String identity, long timeStamp) {
                mValidationIdentity = identity;
                mValidationTimeStamp = timeStamp;
            }
            void update(long timeStamp) {
                mValidationTimeStamp = timeStamp;
            }
            final String mValidationIdentity;
            long mValidationTimeStamp;
        }

        synchronized boolean isRecentlyValidated(int subId) {
            long cacheTtl = getValidationCacheTtl(subId);
            String networkIdentity = getValidationNetworkIdentity(subId);
            if (networkIdentity == null || !mValidatedNetworkMap.containsKey(networkIdentity)) {
                return false;
            }
            long validatedTime = mValidatedNetworkMap.get(networkIdentity).mValidationTimeStamp;
            boolean recentlyValidated = System.currentTimeMillis() - validatedTime < cacheTtl;
            logd("isRecentlyValidated on subId " + subId + " ? " + recentlyValidated);
            return recentlyValidated;
        }

        synchronized void storeLastValidationResult(int subId, boolean validated) {
            String networkIdentity = getValidationNetworkIdentity(subId);
            logd("storeLastValidationResult for subId " + subId
                    + (validated ? " validated." : " not validated."));
            if (networkIdentity == null) return;

            if (!validated) {
                // If validation failed, clear it from the cache.
                mValidatedNetworkPQ.remove(mValidatedNetworkMap.get(networkIdentity));
                mValidatedNetworkMap.remove(networkIdentity);
                return;
            }
            long time =  System.currentTimeMillis();
            ValidatedNetwork network = mValidatedNetworkMap.get(networkIdentity);
            if (network != null) {
                // Already existed in cache, update.
                network.update(time);
                // Re-add to re-sort.
                mValidatedNetworkPQ.remove(network);
                mValidatedNetworkPQ.add(network);
            } else {
                network = new ValidatedNetwork(networkIdentity, time);
                mValidatedNetworkMap.put(networkIdentity, network);
                mValidatedNetworkPQ.add(network);
            }
            // If exceeded max size, remove the one with smallest validation timestamp.
            if (mValidatedNetworkPQ.size() > VALIDATED_NETWORK_CACHE_SIZE) {
                ValidatedNetwork networkToRemove = mValidatedNetworkPQ.poll();
                mValidatedNetworkMap.remove(networkToRemove.mValidationIdentity);
            }
        }

        private String getValidationNetworkIdentity(int subId) {
            if (!SubscriptionManager.isUsableSubscriptionId(subId)) return null;
            Phone phone = PhoneFactory.getPhone(SubscriptionManagerService.getInstance()
                    .getPhoneId(subId));
            if (phone == null || phone.getServiceState() == null) return null;

            NetworkRegistrationInfo regInfo = phone.getServiceState().getNetworkRegistrationInfo(
                    DOMAIN_PS, TRANSPORT_TYPE_WWAN);
            if (regInfo == null || regInfo.getCellIdentity() == null) return null;

            CellIdentity cellIdentity = regInfo.getCellIdentity();
            // TODO: add support for other technologies.
            if (cellIdentity.getType() != CellInfo.TYPE_LTE
                    || cellIdentity.getMccString() == null || cellIdentity.getMncString() == null
                    || ((CellIdentityLte) cellIdentity).getTac() == CellInfo.UNAVAILABLE) {
                return null;
            }

            return cellIdentity.getMccString() + cellIdentity.getMncString() + "_"
                    + ((CellIdentityLte) cellIdentity).getTac() + "_" + subId;
        }

        private long getValidationCacheTtl(int subId) {
            long ttl = 0;
            CarrierConfigManager configManager = (CarrierConfigManager)
                    mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (configManager != null) {
                PersistableBundle b = configManager.getConfigForSubId(subId);
                if (b != null) {
                    ttl = b.getLong(KEY_DATA_SWITCH_VALIDATION_MIN_INTERVAL_MILLIS_LONG);
                }
            }
            // Ttl can't be bigger than one day for now.
            return Math.min(ttl, MAX_VALIDATION_CACHE_TTL);
        }
    }

    /**
     * Callback to pass in when starting validation.
     */
    public interface ValidationCallback {
        /**
         * Validation failed, passed or timed out.
         */
        void onValidationDone(boolean validated, int subId);
        /**
         * Called when a corresponding network becomes available.
         */
        void onNetworkAvailable(Network network, int subId);
    }

    /**
     * Create instance.
     */
    public static CellularNetworkValidator make(Context context,
            @NonNull FeatureFlags featureFlags) {
        if (sInstance != null) {
            logd("createCellularNetworkValidator failed. Instance already exists.");
        } else {
            sInstance = new CellularNetworkValidator(context, featureFlags);
        }

        return sInstance;
    }

    /**
     * Get instance.
     */
    public static CellularNetworkValidator getInstance() {
        return sInstance;
    }

    /**
     * Check whether this feature is supported or not.
     */
    public boolean isValidationFeatureSupported() {
        return PhoneConfigurationManager.getInstance().getCurrentPhoneCapability()
                .isNetworkValidationBeforeSwitchSupported();
    }

    @VisibleForTesting
    public CellularNetworkValidator(Context context, @NonNull FeatureFlags featureFlags) {
        mContext = context;
        mFlags = featureFlags;
        mConnectivityManager = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * API to start a validation
     */
    public synchronized void validate(int subId, long timeoutInMs,
            boolean requireTestPass, ValidationCallback callback) {
        // If it's already validating the same subscription, do nothing.
        if (subId == mSubId) return;

        SubscriptionInfoInternal subInfo = SubscriptionManagerService.getInstance()
                .getSubscriptionInfoInternal(subId);
        if (subInfo == null || !subInfo.isActive()) {
            logd("Failed to start validation. Inactive subId " + subId);
            callback.onValidationDone(false, subId);
            return;
        }

        if (isValidating()) {
            stopValidation();
        }

        mState = STATE_VALIDATING;
        mSubId = subId;
        mValidationCallback = callback;
        mRequireTestPass = requireTestPass;

        logd("Start validating subId " + mSubId + " timeoutInMs " + timeoutInMs
                + " mRequireTestPass " + mRequireTestPass);

        mNetworkCallback = new ConnectivityNetworkCallback(subId);

        mConnectivityManager.requestNetwork(
                createNetworkRequest(subId), mNetworkCallback, mHandler);
        mHandler.postDelayed(() -> onValidationTimeout(subId), timeoutInMs);
    }

    private synchronized void onValidationTimeout(int subId) {
        logd("timeout on subId " + subId + " validation.");
        // Remember latest validated network.
        mValidatedNetworkCache.storeLastValidationResult(subId, false);
        reportValidationResult(false, subId);
    }

    /**
     * API to stop the current validation.
     */
    public synchronized void stopValidation() {
        if (!isValidating()) {
            logd("No need to stop validation.");
            return;
        }
        if (mNetworkCallback != null) {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        }
        mState = STATE_IDLE;
        mHandler.removeCallbacksAndMessages(null);
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    /**
     * Return which subscription is under validating.
     */
    public synchronized int getSubIdInValidation() {
        return mSubId;
    }

    /**
     * Return whether there's an ongoing validation.
     */
    public synchronized boolean isValidating() {
        return mState != STATE_IDLE;
    }

    private NetworkRequest createNetworkRequest(int subId) {
        NetworkRequest.Builder req = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .setNetworkSpecifier(new TelephonyNetworkSpecifier.Builder()
                        .setSubscriptionId(subId).build());

        // Satellite is considered valid as long as it can serve restricted requests.
        Phone target = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(subId));
        boolean isSatellite = target != null
                && target.getServiceState().isUsingNonTerrestrialNetwork();
        if (isSatellite) {
            req.addTransportType(NetworkCapabilities.TRANSPORT_SATELLITE)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
        }
        return req.build();
    }

    private synchronized void reportValidationResult(boolean passed, int subId) {
        // If the validation result is not for current subId, do nothing.
        if (mSubId != subId) return;

        mHandler.removeCallbacksAndMessages(null);

        // Deal with the result only when state is still VALIDATING. This is to avoid
        // receiving multiple callbacks in queue.
        if (mState == STATE_VALIDATING) {
            mValidationCallback.onValidationDone(passed, mSubId);
            mState = STATE_VALIDATED;
            boolean keepRequest = mFlags.keepPingRequest()
                    ? (passed || !mRequireTestPass) : (!mRequireTestPass && passed);
            // If validation passed and per request to NOT release after validation, delay cleanup.
            if (keepRequest) {
                mHandler.postDelayed(this::stopValidation, 500);
            } else {
                stopValidation();
            }

            TelephonyMetrics.getInstance().writeNetworkValidate(passed
                    ? TelephonyEvent.NetworkValidationState.NETWORK_VALIDATION_STATE_PASSED
                    : TelephonyEvent.NetworkValidationState.NETWORK_VALIDATION_STATE_FAILED);
        }
    }

    private synchronized void reportNetworkAvailable(Network network, int subId) {
        // If the validation result is not for current subId, do nothing.
        if (mSubId != subId) return;
        mValidationCallback.onNetworkAvailable(network, subId);
    }

    @VisibleForTesting
    public class ConnectivityNetworkCallback extends ConnectivityManager.NetworkCallback {
        private final int mSubId;

        ConnectivityNetworkCallback(int subId) {
            mSubId = subId;
        }
        /**
         * ConnectivityManager.NetworkCallback implementation
         */
        @Override
        public void onAvailable(@NonNull Network network) {
            logd("network onAvailable " + network);
            TelephonyMetrics.getInstance().writeNetworkValidate(
                    TelephonyEvent.NetworkValidationState.NETWORK_VALIDATION_STATE_AVAILABLE);
            // If it hits validation cache, we report as validation passed; otherwise we report
            // network is available.
            if (mValidatedNetworkCache.isRecentlyValidated(mSubId)) {
                reportValidationResult(true, ConnectivityNetworkCallback.this.mSubId);
            } else {
                reportNetworkAvailable(network, ConnectivityNetworkCallback.this.mSubId);
            }
        }

        @Override
        public void onLosing(@NonNull Network network, int maxMsToLive) {
            logd("network onLosing " + network + " maxMsToLive " + maxMsToLive);
            mValidatedNetworkCache.storeLastValidationResult(
                    ConnectivityNetworkCallback.this.mSubId, false);
            reportValidationResult(false, ConnectivityNetworkCallback.this.mSubId);
        }

        @Override
        public void onLost(@NonNull Network network) {
            logd("network onLost " + network);
            mValidatedNetworkCache.storeLastValidationResult(
                    ConnectivityNetworkCallback.this.mSubId, false);
            reportValidationResult(false, ConnectivityNetworkCallback.this.mSubId);
        }

        @Override
        public void onUnavailable() {
            logd("onUnavailable");
            mValidatedNetworkCache.storeLastValidationResult(
                    ConnectivityNetworkCallback.this.mSubId, false);
            reportValidationResult(false, ConnectivityNetworkCallback.this.mSubId);
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network,
                NetworkCapabilities networkCapabilities) {
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                logd("onValidated");
                mValidatedNetworkCache.storeLastValidationResult(
                        ConnectivityNetworkCallback.this.mSubId, true);
                reportValidationResult(true, ConnectivityNetworkCallback.this.mSubId);
            }
        }
    }

    private static void logd(String log) {
        Log.d(LOG_TAG, log);
    }
}
