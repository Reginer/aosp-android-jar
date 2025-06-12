/*
 * Copyright 2024 The Android Open Source Project
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

import android.annotation.NonNull;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.MatchAllNetworkSpecifier;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkProvider;
import android.net.NetworkProvider.NetworkOfferCallback;
import android.net.NetworkRequest;
import android.net.NetworkScore;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionManager;
import android.util.ArrayMap;
import android.util.LocalLog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.data.PhoneSwitcher.PhoneSwitcherCallback;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Map;

/**
 * TelephonyNetworkProvider is a singleton network provider responsible for providing all
 * telephony related networks including networks on cellular and IWLAN across all active SIMs.
 */
public class TelephonyNetworkProvider extends NetworkProvider implements NetworkOfferCallback {

    public final String LOG_TAG = "TNP";

    /** Android feature flags */
    @NonNull
    private final FeatureFlags mFlags;

    /** The event handler */
    @NonNull
    private final Handler mHandler;

    /** Phone switcher responsible to determine request routing on dual-SIM device */
    @NonNull
    private final PhoneSwitcher mPhoneSwitcher;

    /** Network requests map. Key is the network request, value is the phone id it applies to. */
    private final Map<TelephonyNetworkRequest, Integer> mNetworkRequests = new ArrayMap<>();

    /** Persisted log */
    @NonNull
    private final LocalLog mLocalLog = new LocalLog(256);

    /**
     * Constructor
     *
     * @param looper The looper for event handling
     * @param context The context
     * @param featureFlags Android feature flags
     */
    public TelephonyNetworkProvider(@NonNull Looper looper, @NonNull Context context,
                                    @NonNull FeatureFlags featureFlags) {
        super(context, looper, TelephonyNetworkProvider.class.getSimpleName());

        mFlags = featureFlags;
        mHandler = new Handler(looper);
        mPhoneSwitcher = PhoneSwitcher.getInstance();

        // Register for subscription changed event.
        context.getSystemService(SubscriptionManager.class)
                .addOnSubscriptionsChangedListener(mHandler::post,
                        new SubscriptionManager.OnSubscriptionsChangedListener() {
                        @Override
                        public void onSubscriptionsChanged() {
                            logl("Subscription changed.");
                            reevaluateNetworkRequests("subscription changed");
                        }});

        // Register for preferred data changed event
        mPhoneSwitcher.registerCallback(new PhoneSwitcherCallback(mHandler::post) {
                    @Override
                    public void onPreferredDataPhoneIdChanged(int phoneId) {
                        logl("Preferred data sub phone id changed to " + phoneId);
                        reevaluateNetworkRequests("Preferred data subscription changed");
                    }
                });

        // Register the provider and tell connectivity service what network offer telephony can
        // provide
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        if (cm != null) {
            cm.registerNetworkProvider(this);
            NetworkCapabilities caps = makeNetworkFilter();
            registerNetworkOffer(new NetworkScore.Builder().build(), caps, mHandler::post, this);
            logl("registerNetworkOffer: " + caps);
        }
    }

    /**
     * Get the phone id for the network request.
     *
     * @param request The network request
     * @return The id of the phone where the network request should route to. If the network request
     * can't be applied to any phone, {@link SubscriptionManager#INVALID_PHONE_INDEX} will be
     * returned.
     */
    private int getPhoneIdForNetworkRequest(@NonNull TelephonyNetworkRequest request) {
        for (Phone phone : PhoneFactory.getPhones()) {
            int phoneId = phone.getPhoneId();
            if (mPhoneSwitcher.shouldApplyNetworkRequest(request, phoneId)) {
                // Return here because by design the network request can be only applied to *one*
                // phone. It's not possible to have two DataNetworkController to attempt to setup
                // data call for the same network request.
                return phoneId;
            }
        }

        return SubscriptionManager.INVALID_PHONE_INDEX;
    }

    /**
     * Called when receiving a network request from connectivity service. This is the entry point
     * that a network request arrives telephony.
     *
     * @param request The network request
     */
    @Override
    public void onNetworkNeeded(@NonNull NetworkRequest request) {
        TelephonyNetworkRequest networkRequest = new TelephonyNetworkRequest(request, mFlags);
        if (mNetworkRequests.containsKey(networkRequest)) {
            loge("Duplicate network request " + networkRequest);
            return;
        }

        mPhoneSwitcher.onRequestNetwork(networkRequest);

        // Check with PhoneSwitcher to see where to route the request.
        int phoneId = getPhoneIdForNetworkRequest(networkRequest);
        if (phoneId != SubscriptionManager.INVALID_PHONE_INDEX) {
            logl("onNetworkNeeded: phoneId=" + phoneId + ", " + networkRequest);
            PhoneFactory.getPhone(phoneId).getDataNetworkController()
                    .addNetworkRequest(networkRequest);
        } else {
            logl("onNetworkNeeded: Not applied. " + networkRequest);
        }

        mNetworkRequests.put(networkRequest, phoneId);
    }

    /**
     * Called when connectivity service remove the network request. Note this will not result in
     * network tear down. Even there is no network request attached to the network, telephony still
     * relies on {@link NetworkAgent#onNetworkUnwanted()} to tear down the network.
     *
     * @param request The released network request
     *
     * @see TelephonyNetworkAgent#onNetworkUnwanted()
     */
    @Override
    public void onNetworkUnneeded(@NonNull NetworkRequest request) {
        TelephonyNetworkRequest networkRequest = mNetworkRequests.keySet().stream()
                .filter(r -> r.getNativeNetworkRequest().equals(request))
                .findFirst()
                .orElse(null);
        if (networkRequest == null) {
            loge("onNetworkUnneeded: Cannot find " + request);
            return;
        }

        mPhoneSwitcher.onReleaseNetwork(networkRequest);
        int phoneId = mNetworkRequests.remove(networkRequest);
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            logl("onNetworkUnneeded: phoneId=" + phoneId + ", " + networkRequest);
            // Remove the network request from network controller. Note this will not result
            // in disconnecting the data network.
            phone.getDataNetworkController().removeNetworkRequest(networkRequest);
        } else {
            loge("onNetworkUnneeded: Unable to get phone. phoneId=" + phoneId);
        }
    }

    /**
     * Re-evaluate the existing networks and re-apply to the applicable phone.
     *
     * @param reason The reason for re-evaluating network request. Note this can be only used for
     * debugging message purposes.
     */
    private void reevaluateNetworkRequests(@NonNull String reason) {
        logl("reevaluateNetworkRequests: " + reason + ".");
        mNetworkRequests.forEach((request, oldPhoneId) -> {
            int newPhoneId = getPhoneIdForNetworkRequest(request);
            if (newPhoneId != oldPhoneId) {
                // We need to move the request from old phone to the new phone. This can happen
                // when the user changes the default data subscription.

                if (oldPhoneId != SubscriptionManager.INVALID_PHONE_INDEX) {
                    PhoneFactory.getPhone(oldPhoneId).getDataNetworkController()
                            .removeNetworkRequest(request);
                }

                if (newPhoneId != SubscriptionManager.INVALID_PHONE_INDEX) {
                    PhoneFactory.getPhone(newPhoneId).getDataNetworkController()
                            .addNetworkRequest(request);
                }

                logl("Request moved. phoneId " + oldPhoneId + " -> " + newPhoneId + " " + request);
                mNetworkRequests.put(request, newPhoneId);
            }
        });
    }

    /**
     * @return The maximal network capabilities that telephony can support.
     */
    @VisibleForTesting
    @NonNull
    public NetworkCapabilities makeNetworkFilter() {
        final NetworkCapabilities.Builder builder = new NetworkCapabilities.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_SATELLITE)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_IA)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_MMTEL)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VCN_MANAGED)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_1)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_2)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_3)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_4)
                .addEnterpriseId(NetworkCapabilities.NET_ENTERPRISE_ID_5)
                // Ideally TelephonyNetworkProvider should only accept TelephonyNetworkSpecifier,
                // but this network provider is a singleton across all SIMs, and
                // TelephonyNetworkSpecifier can't accept more than one subscription id, so we let
                // the provider accepts all different kinds NetworkSpecifier.
                .setNetworkSpecifier(new MatchAllNetworkSpecifier());
        TelephonyNetworkRequest.getAllSupportedNetworkCapabilities()
                .forEach(builder::addCapability);

        return builder.build();
    }

    /**
     * Log debug message to logcat.
     *
     * @param s The debug message to log
     */
    private void log(@NonNull String s) {
        Rlog.d(LOG_TAG, s);
    }

    /**
     * Log error debug messages to logcat.
     * @param s The error debug messages
     */
    private void loge(@NonNull String s) {
        Rlog.e(LOG_TAG, s);
    }

    /**
     * Log to logcat and persisted local log.
     *
     * @param s The debug message to log
     */
    private void logl(@NonNull String s) {
        log(s);
        mLocalLog.log(s);
    }

    /**
     * Dump the state of telephony network provider.
     *
     * @param fd File descriptor
     * @param writer Print writer
     * @param args Arguments
     */
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        pw.println("TelephonyNetworkProvider:");
        pw.increaseIndent();

        pw.println("mPreferredDataPhoneId=" + mPhoneSwitcher.getPreferredDataPhoneId());
        int defaultDataSubId = SubscriptionManagerService.getInstance().getDefaultDataSubId();
        pw.println("DefaultDataSubId=" + defaultDataSubId);
        pw.println("DefaultDataPhoneId=" + SubscriptionManagerService.getInstance()
                .getPhoneId(defaultDataSubId));

        pw.println("Registered capabilities: " + makeNetworkFilter());
        pw.println("Network requests:");
        pw.increaseIndent();
        for (Phone phone : PhoneFactory.getPhones()) {
            pw.println("Phone " + phone.getPhoneId() + ":");
            pw.increaseIndent();
            mNetworkRequests.forEach((request, phoneId) -> {
                if (phoneId == phone.getPhoneId()) {
                    pw.println(request);
                }
            });
            pw.decreaseIndent();
        }
        pw.println("Not applied requests:");
        pw.increaseIndent();
        mNetworkRequests.forEach((request, phoneId) -> {
            if (phoneId == SubscriptionManager.INVALID_PHONE_INDEX) {
                pw.println(request);
            }
        });
        pw.decreaseIndent();
        pw.decreaseIndent();
        pw.println();
        pw.println("Local logs:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
    }
}
