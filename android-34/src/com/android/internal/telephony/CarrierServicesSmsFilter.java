/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.service.carrier.CarrierMessagingService;
import android.service.carrier.CarrierMessagingServiceWrapper;
import android.service.carrier.CarrierMessagingServiceWrapper.CarrierMessagingCallback;
import android.service.carrier.MessagePdu;
import android.telephony.AnomalyReporter;
import android.util.LocalLog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Filters incoming SMS with carrier services.
 *
 * <p>A new instance must be created for filtering each message.
 *
 * <p>Note that if a carrier services app is unavailable at the time a message is received because
 * credential-encrypted storage is unavailable and it is not direct-boot aware, and the message ends
 * up being handled by a filter further down the chain, that message will not be redelivered to the
 * carrier app once the user unlocks the storage.
 */
public class CarrierServicesSmsFilter {
    protected static final boolean DBG = true;
    /** onFilterComplete is not called. */
    public static final int EVENT_ON_FILTER_COMPLETE_NOT_CALLED = 1;

    /** onFilterComplete timeout. */
    public static final int FILTER_COMPLETE_TIMEOUT_MS = 10 * 60 * 1000; //10 minutes

    /** SMS anomaly uuid -- CarrierMessagingService did not respond */
    private static final UUID sAnomalyNoResponseFromCarrierMessagingService =
            UUID.fromString("94095e8e-b516-4065-a8be-e05b84071002");

    private final Context mContext;
    private final Phone mPhone;
    private final byte[][] mPdus;
    private final int mDestPort;
    private final String mPduFormat;
    private final CarrierServicesSmsFilterCallbackInterface mCarrierServicesSmsFilterCallback;
    private final String mLogTag;
    private final CallbackTimeoutHandler mCallbackTimeoutHandler;
    private final LocalLog mLocalLog;
    private final long mMessageId;
    private FilterAggregator mFilterAggregator;

    @VisibleForTesting
    public CarrierServicesSmsFilter(
            Context context,
            Phone phone,
            byte[][] pdus,
            int destPort,
            String pduFormat,
            CarrierServicesSmsFilterCallbackInterface carrierServicesSmsFilterCallback,
            String logTag,
            LocalLog localLog,
            long msgId) {
        mContext = context;
        mPhone = phone;
        mPdus = pdus;
        mDestPort = destPort;
        mPduFormat = pduFormat;
        mCarrierServicesSmsFilterCallback = carrierServicesSmsFilterCallback;
        mLogTag = logTag;
        mCallbackTimeoutHandler = new CallbackTimeoutHandler();
        mLocalLog = localLog;
        mMessageId = msgId;
    }

    /**
     * @return {@code true} if the SMS was handled by a carrier application or an ImsService
     * implementing RCS features.
     */
    @VisibleForTesting
    public boolean filter() {
        Optional<String> carrierAppForFiltering = getCarrierAppPackageForFiltering();
        List<String> smsFilterPackages = new ArrayList<>();
        if (carrierAppForFiltering.isPresent()) {
            smsFilterPackages.add(carrierAppForFiltering.get());
        }
        String imsRcsPackage = CarrierSmsUtils.getImsRcsPackageForIntent(mContext, mPhone,
                new Intent(CarrierMessagingService.SERVICE_INTERFACE));
        if (imsRcsPackage != null) {
            smsFilterPackages.add(imsRcsPackage);
        }

        if (mFilterAggregator != null) {
            String errMsg = "filter: Cannot reuse the same CarrierServiceSmsFilter object for "
                    + "filtering";
            loge(errMsg);
            throw new RuntimeException(errMsg);
        }

        int numPackages = smsFilterPackages.size();
        if (numPackages > 0) {
            mFilterAggregator = new FilterAggregator(numPackages);
            //start the timer
            mCallbackTimeoutHandler.sendMessageDelayed(mCallbackTimeoutHandler
                            .obtainMessage(EVENT_ON_FILTER_COMPLETE_NOT_CALLED, mFilterAggregator),
                    FILTER_COMPLETE_TIMEOUT_MS);
            for (String smsFilterPackage : smsFilterPackages) {
                filterWithPackage(smsFilterPackage, mFilterAggregator);
            }
            return true;
        } else {
            return false;
        }
    }

    private Optional<String> getCarrierAppPackageForFiltering() {
        List<String> carrierPackages = null;
        CarrierPrivilegesTracker cpt = mPhone.getCarrierPrivilegesTracker();
        if (cpt != null) {
            carrierPackages =
                    cpt.getCarrierPackageNamesForIntent(
                            new Intent(CarrierMessagingService.SERVICE_INTERFACE));
        } else {
            loge("getCarrierAppPackageForFiltering: UiccCard not initialized");
        }
        if (carrierPackages != null && carrierPackages.size() == 1) {
            log("getCarrierAppPackageForFiltering: Found carrier package: "
                    + carrierPackages.get(0));
            return Optional.of(carrierPackages.get(0));
        }

        // It is possible that carrier app is not present as a CarrierPackage, but instead as a
        // system app
        List<String> systemPackages =
                getSystemAppForIntent(new Intent(CarrierMessagingService.SERVICE_INTERFACE));

        if (systemPackages != null && systemPackages.size() == 1) {
            log("getCarrierAppPackageForFiltering: Found system package: " + systemPackages.get(0));
            return Optional.of(systemPackages.get(0));
        }
        logv("getCarrierAppPackageForFiltering: Unable to find carrierPackages: " + carrierPackages
                + " or systemPackages: " + systemPackages);
        return Optional.empty();
    }

    private void filterWithPackage(String packageName, FilterAggregator filterAggregator) {
        CarrierSmsFilter smsFilter = new CarrierSmsFilter(mPdus, mDestPort, mPduFormat,
                packageName);
        CarrierSmsFilterCallback smsFilterCallback =
                new CarrierSmsFilterCallback(filterAggregator,
                        smsFilter.mCarrierMessagingServiceWrapper, packageName);
        filterAggregator.addToCallbacks(smsFilterCallback);

        smsFilter.filterSms(smsFilterCallback);
    }

    private List<String> getSystemAppForIntent(Intent intent) {
        List<String> packages = new ArrayList<String>();
        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> receivers = packageManager.queryIntentServices(intent, 0);
        String carrierFilterSmsPerm = "android.permission.CARRIER_FILTER_SMS";

        for (ResolveInfo info : receivers) {
            if (info.serviceInfo == null) {
                loge("getSystemAppForIntent: Can't get service information from " + info);
                continue;
            }
            String packageName = info.serviceInfo.packageName;
            if (packageManager.checkPermission(carrierFilterSmsPerm, packageName)
                    == packageManager.PERMISSION_GRANTED) {
                packages.add(packageName);
                if (DBG) log("getSystemAppForIntent: added package " + packageName);
            }
        }
        return packages;
    }

    private void log(String message) {
        Rlog.d(mLogTag, message + ", id: " + mMessageId);
    }

    private void loge(String message) {
        Rlog.e(mLogTag, message + ", id: " + mMessageId);
    }

    private void logv(String message) {
        Rlog.v(mLogTag, message + ", id: " + mMessageId);
    }

    /**
     * Result of filtering SMS is returned in this callback.
     */
    @VisibleForTesting
    public interface CarrierServicesSmsFilterCallbackInterface {
        void onFilterComplete(int result);
    }

    /**
     * Asynchronously binds to the carrier messaging service, and filters out the message if
     * instructed to do so by the carrier messaging service. A new instance must be used for every
     * message.
     */
    private final class CarrierSmsFilter {
        private final byte[][] mPdus;
        private final int mDestPort;
        private final String mSmsFormat;
        // Instantiated in filterSms.
        private volatile CarrierSmsFilterCallback mSmsFilterCallback;
        private final String mPackageName;
        protected final CarrierMessagingServiceWrapper mCarrierMessagingServiceWrapper =
                new CarrierMessagingServiceWrapper();

        CarrierSmsFilter(byte[][] pdus, int destPort, String smsFormat, String packageName) {
            mPdus = pdus;
            mDestPort = destPort;
            mSmsFormat = smsFormat;
            mPackageName = packageName;
        }

        /**
         * Attempts to bind to a {@link CarrierMessagingService}. Filtering is initiated
         * asynchronously once the service is ready using {@link #onServiceReady()}.
         */
        void filterSms(CarrierSmsFilterCallback smsFilterCallback) {
            mSmsFilterCallback = smsFilterCallback;
            if (!mCarrierMessagingServiceWrapper.bindToCarrierMessagingService(
                    mContext, mPackageName, runnable -> runnable.run(), ()-> onServiceReady())) {
                loge("CarrierSmsFilter::filterSms: bindService() failed for " + mPackageName);
                smsFilterCallback.onReceiveSmsComplete(
                        CarrierMessagingService.RECEIVE_OPTIONS_DEFAULT);
            } else {
                logv("CarrierSmsFilter::filterSms: bindService() succeeded for "
                        + mPackageName);
            }
        }

        /**
         * Invokes the {@code carrierMessagingService} to filter messages. The filtering result is
         * delivered to {@code smsFilterCallback}.
         */
        private void onServiceReady() {
            try {
                log("onServiceReady: calling filterSms on " + mPackageName);
                mCarrierMessagingServiceWrapper.receiveSms(
                        new MessagePdu(Arrays.asList(mPdus)), mSmsFormat, mDestPort,
                        mPhone.getSubId(), runnable -> runnable.run(), mSmsFilterCallback);
            } catch (RuntimeException e) {
                loge("Exception filtering the SMS with " + mPackageName + ": " + e);
                mSmsFilterCallback.onReceiveSmsComplete(
                        CarrierMessagingService.RECEIVE_OPTIONS_DEFAULT);
            }
        }
    }

    /**
     * A callback used to notify the platform of the carrier messaging app filtering result. Once
     * the result is ready, the carrier messaging service connection is disposed.
     */
    private final class CarrierSmsFilterCallback implements CarrierMessagingCallback {
        private final FilterAggregator mFilterAggregator;
        private final CarrierMessagingServiceWrapper mCarrierMessagingServiceWrapper;
        private boolean mIsOnFilterCompleteCalled;
        private final String mPackageName;

        CarrierSmsFilterCallback(FilterAggregator filterAggregator,
                CarrierMessagingServiceWrapper carrierMessagingServiceWrapper, String packageName) {
            mFilterAggregator = filterAggregator;
            mCarrierMessagingServiceWrapper = carrierMessagingServiceWrapper;
            mIsOnFilterCompleteCalled = false;
            mPackageName = packageName;
        }

        /**
         * This method should be called only once.
         */
        @Override
        public void onReceiveSmsComplete(int result) {
            log("CarrierSmsFilterCallback::onFilterComplete: Called from " + mPackageName
                    + " with result: " + result);
            // in the case that timeout has already passed and triggered, but the initial callback
            // is run afterwards, we should not follow through
            if (!mIsOnFilterCompleteCalled) {
                mIsOnFilterCompleteCalled = true;
                mCarrierMessagingServiceWrapper.disconnect();
                mFilterAggregator.onFilterComplete(result, this);
            }
        }

        @Override
        public void onSendSmsComplete(int result, int messageRef) {
            loge("onSendSmsComplete: Unexpected call from " + mPackageName
                    + " with result: " + result);
        }

        @Override
        public void onSendMultipartSmsComplete(int result, int[] messageRefs) {
            loge("onSendMultipartSmsComplete: Unexpected call from " + mPackageName
                    + " with result: " + result);
        }

        @Override
        public void onSendMmsComplete(int result, byte[] sendConfPdu) {
            loge("onSendMmsComplete: Unexpected call from " + mPackageName
                    + " with result: " + result);
        }

        @Override
        public void onDownloadMmsComplete(int result) {
            loge("onDownloadMmsComplete: Unexpected call from " + mPackageName
                    + " with result: " + result);
        }
    }

    private final class FilterAggregator {
        private final Object mFilterLock = new Object();
        private int mNumPendingFilters;
        private final Set<CarrierSmsFilterCallback> mCallbacks;
        private int mFilterResult;

        FilterAggregator(int numFilters) {
            mNumPendingFilters = numFilters;
            mCallbacks = new HashSet<>();
            mFilterResult = CarrierMessagingService.RECEIVE_OPTIONS_DEFAULT;
        }

        void onFilterComplete(int result, CarrierSmsFilterCallback callback) {
            synchronized (mFilterLock) {
                mNumPendingFilters--;
                mCallbacks.remove(callback);
                combine(result);
                if (mNumPendingFilters == 0) {
                    // Calling identity was the CarrierMessagingService in this callback, change it
                    // back to ours.
                    long token = Binder.clearCallingIdentity();
                    try {
                        mCarrierServicesSmsFilterCallback.onFilterComplete(mFilterResult);
                    } finally {
                        // return back to the CarrierMessagingService, restore the calling identity.
                        Binder.restoreCallingIdentity(token);
                    }
                    //all onFilterCompletes called before timeout has triggered
                    //remove the pending message
                    log("FilterAggregator::onFilterComplete: called successfully with result = "
                            + result);
                    mCallbackTimeoutHandler.removeMessages(EVENT_ON_FILTER_COMPLETE_NOT_CALLED);
                } else {
                    log("FilterAggregator::onFilterComplete: waiting for pending filters "
                            + mNumPendingFilters);
                }
            }
        }

        private void combine(int result) {
            mFilterResult = mFilterResult | result;
        }

        private void addToCallbacks(CarrierSmsFilterCallback callback) {
            mCallbacks.add(callback);
        }

    }

    protected final class CallbackTimeoutHandler extends Handler {

        private static final boolean DBG = true;

        @Override
        public void handleMessage(Message msg) {
            if (DBG) {
                log("CallbackTimeoutHandler: handleMessage(" + msg.what + ")");
            }

            switch(msg.what) {
                case EVENT_ON_FILTER_COMPLETE_NOT_CALLED:
                    mLocalLog.log("CarrierServicesSmsFilter: onFilterComplete timeout: not"
                            + " called before " + FILTER_COMPLETE_TIMEOUT_MS + " milliseconds.");
                    FilterAggregator filterAggregator = (FilterAggregator) msg.obj;
                    String packages = filterAggregator.mCallbacks.stream()
                            .map(callback -> callback.mPackageName)
                            .collect(Collectors.joining(", "));
                    AnomalyReporter.reportAnomaly(sAnomalyNoResponseFromCarrierMessagingService,
                            "No response from " + packages, mPhone.getCarrierId());
                    handleFilterCallbacksTimeout();
                    break;
            }
        }

        private void handleFilterCallbacksTimeout() {
            for (CarrierSmsFilterCallback callback : mFilterAggregator.mCallbacks) {
                log("handleFilterCallbacksTimeout: calling onFilterComplete");
                callback.onReceiveSmsComplete(CarrierMessagingService.RECEIVE_OPTIONS_DEFAULT);
            }
        }
    }
}
