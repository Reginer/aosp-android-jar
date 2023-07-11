/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.ims.rcs.uce.request;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.RcsContactUceCapability.CapabilityMechanism;
import android.telephony.ims.RcsUceAdapter;
import android.telephony.ims.aidl.IOptionsRequestCallback;
import android.telephony.ims.aidl.IRcsUceControllerCallback;
import android.text.TextUtils;
import android.util.Log;

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber;
import com.android.ims.rcs.uce.UceController;
import com.android.ims.rcs.uce.UceController.UceControllerCallback;
import com.android.ims.rcs.uce.UceDeviceState;
import com.android.ims.rcs.uce.UceDeviceState.DeviceStateResult;
import com.android.ims.rcs.uce.eab.EabCapabilityResult;
import com.android.ims.rcs.uce.options.OptionsController;
import com.android.ims.rcs.uce.presence.subscribe.SubscribeController;
import com.android.ims.rcs.uce.request.UceRequest.UceRequestType;
import com.android.ims.rcs.uce.request.UceRequestCoordinator.UceRequestUpdate;
import com.android.ims.rcs.uce.util.UceUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Managers the capabilities requests and the availability requests from UceController.
 */
public class UceRequestManager {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "UceRequestManager";

    /**
     * When enabled, skip the request queue for requests that have numbers with valid cached
     * capabilities and return that cached info directly.
     * Note: This also has a CTS test associated with it, so this can not be disabled without
     * disabling the corresponding RcsUceAdapterTest#testCacheQuerySuccessWhenNetworkBlocked test.
     */
    private static final boolean FEATURE_SHORTCUT_QUEUE_FOR_CACHED_CAPS = true;

    /**
     * Testing interface used to mock UceUtils in testing.
     */
    @VisibleForTesting
    public interface UceUtilsProxy {
        /**
         * The interface for {@link UceUtils#isPresenceCapExchangeEnabled(Context, int)} used for
         * testing.
         */
        boolean isPresenceCapExchangeEnabled(Context context, int subId);

        /**
         * The interface for {@link UceUtils#isPresenceSupported(Context, int)} used for testing.
         */
        boolean isPresenceSupported(Context context, int subId);

        /**
         * The interface for {@link UceUtils#isSipOptionsSupported(Context, int)} used for testing.
         */
        boolean isSipOptionsSupported(Context context, int subId);

        /**
         * @return true when the Presence group subscribe is enabled.
         */
        boolean isPresenceGroupSubscribeEnabled(Context context, int subId);

        /**
         * Retrieve the maximum number of contacts that can be included in a request.
         */
        int getRclMaxNumberEntries(int subId);

        /**
         * @return true if the given phone number is blocked by the network.
         */
        boolean isNumberBlocked(Context context, String phoneNumber);
    }

    private static UceUtilsProxy sUceUtilsProxy = new UceUtilsProxy() {
        @Override
        public boolean isPresenceCapExchangeEnabled(Context context, int subId) {
            return UceUtils.isPresenceCapExchangeEnabled(context, subId);
        }

        @Override
        public boolean isPresenceSupported(Context context, int subId) {
            return UceUtils.isPresenceSupported(context, subId);
        }

        @Override
        public boolean isSipOptionsSupported(Context context, int subId) {
            return UceUtils.isSipOptionsSupported(context, subId);
        }

        @Override
        public boolean isPresenceGroupSubscribeEnabled(Context context, int subId) {
            return UceUtils.isPresenceGroupSubscribeEnabled(context, subId);
        }

        @Override
        public int getRclMaxNumberEntries(int subId) {
            return UceUtils.getRclMaxNumberEntries(subId);
        }

        @Override
        public boolean isNumberBlocked(Context context, String phoneNumber) {
            return UceUtils.isNumberBlocked(context, phoneNumber);
        }
    };

    @VisibleForTesting
    public void setsUceUtilsProxy(UceUtilsProxy uceUtilsProxy) {
        sUceUtilsProxy = uceUtilsProxy;
    }

    /**
     * The callback interface to receive the request and the result from the UceRequest.
     */
    public interface RequestManagerCallback {
        /**
         * Notify sending the UceRequest
         */
        void notifySendingRequest(long coordinator, long taskId, long delayTimeMs);

        /**
         * Retrieve the contact capabilities from the cache.
         */
        List<EabCapabilityResult> getCapabilitiesFromCache(List<Uri> uriList);

        /**
         * Retrieve the contact capabilities from the cache including the expired capabilities.
         */
        List<EabCapabilityResult> getCapabilitiesFromCacheIncludingExpired(List<Uri> uriList);

        /**
         * Retrieve the contact availability from the cache.
         */
        EabCapabilityResult getAvailabilityFromCache(Uri uri);

        /**
         * Retrieve the contact availability from the cache including the expired capabilities.
         */
        EabCapabilityResult getAvailabilityFromCacheIncludingExpired(Uri uri);

        /**
         * Store the given contact capabilities to the cache.
         */
        void saveCapabilities(List<RcsContactUceCapability> contactCapabilities);

        /**
         * Retrieve the device's capabilities.
         */
        RcsContactUceCapability getDeviceCapabilities(@CapabilityMechanism int capMechanism);

        /**
         * Get the device state to check whether the device is disallowed by the network or not.
         */
        DeviceStateResult getDeviceState();

        /**
         * Refresh the device state. It is called when receive the UCE request response.
         */
        void refreshDeviceState(int sipCode, String reason);

        /**
         * Notify that the UceRequest associated with the given taskId encounters error.
         */
        void notifyRequestError(long requestCoordinatorId, long taskId);

        /**
         * Notify that the UceRequest received the onCommandError callback from the ImsService.
         */
        void notifyCommandError(long requestCoordinatorId, long taskId);

        /**
         * Notify that the UceRequest received the onNetworkResponse callback from the ImsService.
         */
        void notifyNetworkResponse(long requestCoordinatorId, long taskId);

        /**
         * Notify that the UceRequest received the onTerminated callback from the ImsService.
         */
        void notifyTerminated(long requestCoordinatorId, long taskId);

        /**
         * Notify that some contacts are not RCS anymore. It will updated the cached capabilities
         * and trigger the callback IRcsUceControllerCallback#onCapabilitiesReceived
         */
        void notifyResourceTerminated(long requestCoordinatorId, long taskId);

        /**
         * Notify that the capabilities updates. It will update the cached and trigger the callback
         * IRcsUceControllerCallback#onCapabilitiesReceived
         */
        void notifyCapabilitiesUpdated(long requestCoordinatorId, long taskId);

        /**
         * Notify that some of the request capabilities can be retrieved from the cached.
         */
        void notifyCachedCapabilitiesUpdated(long requestCoordinatorId, long taskId);

        /**
         * Notify that all the requested capabilities can be retrieved from the cache. It does not
         * need to request capabilities from the network.
         */
        void notifyNoNeedRequestFromNetwork(long requestCoordinatorId, long taskId);

        /**
         * Notify that the remote options request is done. This is sent by RemoteOptionsRequest and
         * it will notify the RemoteOptionsCoordinator to handle it.
         */
        void notifyRemoteRequestDone(long requestCoordinatorId, long taskId);

        /**
         * Set the timer for the request timeout. It will cancel the request when the time is up.
         */
        void setRequestTimeoutTimer(long requestCoordinatorId, long taskId, long timeoutAfterMs);

        /**
         * Remove the timeout timer of the capabilities request.
         */
        void removeRequestTimeoutTimer(long taskId);

        /**
         * Notify that the UceRequest has finished. This is sent by UceRequestCoordinator.
         */
        void notifyUceRequestFinished(long requestCoordinatorId, long taskId);

        /**
         * Notify that the RequestCoordinator has finished. This is sent by UceRequestCoordinator
         * to remove the coordinator from the UceRequestRepository.
         */
        void notifyRequestCoordinatorFinished(long requestCoordinatorId);

        /**
         * Check whether the given uris are in the throttling list.
         * @param uriList the uris to check if it is in the throttling list
         * @return the uris in the throttling list
         */
        List<Uri> getInThrottlingListUris(List<Uri> uriList);

        /**
         * Add the given uris to the throttling list because the capabilities request result
         * is inconclusive.
         */
        void addToThrottlingList(List<Uri> uriList, int sipCode);
    }

    private RequestManagerCallback mRequestMgrCallback = new RequestManagerCallback() {
        @Override
        public void notifySendingRequest(long coordinatorId, long taskId, long delayTimeMs) {
            mHandler.sendRequestMessage(coordinatorId, taskId, delayTimeMs);
        }

        @Override
        public List<EabCapabilityResult> getCapabilitiesFromCache(List<Uri> uriList) {
            return mControllerCallback.getCapabilitiesFromCache(uriList);
        }

        @Override
        public List<EabCapabilityResult> getCapabilitiesFromCacheIncludingExpired(List<Uri> uris) {
            return mControllerCallback.getCapabilitiesFromCacheIncludingExpired(uris);
        }

        @Override
        public EabCapabilityResult getAvailabilityFromCache(Uri uri) {
            return mControllerCallback.getAvailabilityFromCache(uri);
        }

        @Override
        public EabCapabilityResult getAvailabilityFromCacheIncludingExpired(Uri uri) {
            return mControllerCallback.getAvailabilityFromCacheIncludingExpired(uri);
        }

        @Override
        public void saveCapabilities(List<RcsContactUceCapability> contactCapabilities) {
            mControllerCallback.saveCapabilities(contactCapabilities);
        }

        @Override
        public RcsContactUceCapability getDeviceCapabilities(@CapabilityMechanism int mechanism) {
            return mControllerCallback.getDeviceCapabilities(mechanism);
        }

        @Override
        public DeviceStateResult getDeviceState() {
            return mControllerCallback.getDeviceState();
        }

        @Override
        public void refreshDeviceState(int sipCode, String reason) {
            mControllerCallback.refreshDeviceState(sipCode, reason,
                    UceController.REQUEST_TYPE_CAPABILITY);
        }

        @Override
        public void notifyRequestError(long requestCoordinatorId, long taskId) {
            mHandler.sendRequestUpdatedMessage(requestCoordinatorId, taskId,
                    UceRequestCoordinator.REQUEST_UPDATE_ERROR);
        }

        @Override
        public void notifyCommandError(long requestCoordinatorId, long taskId) {
            mHandler.sendRequestUpdatedMessage(requestCoordinatorId, taskId,
                    UceRequestCoordinator.REQUEST_UPDATE_COMMAND_ERROR);
        }

        @Override
        public void notifyNetworkResponse(long requestCoordinatorId, long taskId) {
            mHandler.sendRequestUpdatedMessage(requestCoordinatorId, taskId,
                    UceRequestCoordinator.REQUEST_UPDATE_NETWORK_RESPONSE);
        }
        @Override
        public void notifyTerminated(long requestCoordinatorId, long taskId) {
            mHandler.sendRequestUpdatedMessage(requestCoordinatorId, taskId,
                    UceRequestCoordinator.REQUEST_UPDATE_TERMINATED);
        }
        @Override
        public void notifyResourceTerminated(long requestCoordinatorId, long taskId) {
            mHandler.sendRequestUpdatedMessage(requestCoordinatorId, taskId,
                    UceRequestCoordinator.REQUEST_UPDATE_RESOURCE_TERMINATED);
        }
        @Override
        public void notifyCapabilitiesUpdated(long requestCoordinatorId, long taskId) {
            mHandler.sendRequestUpdatedMessage(requestCoordinatorId, taskId,
                    UceRequestCoordinator.REQUEST_UPDATE_CAPABILITY_UPDATE);
        }

        @Override
        public void notifyCachedCapabilitiesUpdated(long requestCoordinatorId, long taskId) {
            mHandler.sendRequestUpdatedMessage(requestCoordinatorId, taskId,
                    UceRequestCoordinator.REQUEST_UPDATE_CACHED_CAPABILITY_UPDATE);
        }

        @Override
        public void notifyNoNeedRequestFromNetwork(long requestCoordinatorId, long taskId) {
            mHandler.sendRequestUpdatedMessage(requestCoordinatorId, taskId,
                    UceRequestCoordinator.REQUEST_UPDATE_NO_NEED_REQUEST_FROM_NETWORK);
        }

        @Override
        public void notifyRemoteRequestDone(long requestCoordinatorId, long taskId) {
            mHandler.sendRequestUpdatedMessage(requestCoordinatorId, taskId,
                    UceRequestCoordinator.REQUEST_UPDATE_REMOTE_REQUEST_DONE);
        }

        @Override
        public void setRequestTimeoutTimer(long coordinatorId, long taskId, long timeoutAfterMs) {
            mHandler.sendRequestTimeoutTimerMessage(coordinatorId, taskId, timeoutAfterMs);
        }

        @Override
        public void removeRequestTimeoutTimer(long taskId) {
            mHandler.removeRequestTimeoutTimer(taskId);
        }

        @Override
        public void notifyUceRequestFinished(long requestCoordinatorId, long taskId) {
            mHandler.sendRequestFinishedMessage(requestCoordinatorId, taskId);
        }

        @Override
        public void notifyRequestCoordinatorFinished(long requestCoordinatorId) {
            mHandler.sendRequestCoordinatorFinishedMessage(requestCoordinatorId);
        }

        @Override
        public List<Uri> getInThrottlingListUris(List<Uri> uriList) {
            return mThrottlingList.getInThrottlingListUris(uriList);
        }

        @Override
        public void addToThrottlingList(List<Uri> uriList, int sipCode) {
            mThrottlingList.addToThrottlingList(uriList, sipCode);
        }
    };

    private final int mSubId;
    private final Context mContext;
    private final UceRequestHandler mHandler;
    private final UceRequestRepository mRequestRepository;
    private final ContactThrottlingList mThrottlingList;
    private volatile boolean mIsDestroyed;

    private OptionsController mOptionsCtrl;
    private SubscribeController mSubscribeCtrl;
    private UceControllerCallback mControllerCallback;

    public UceRequestManager(Context context, int subId, Looper looper, UceControllerCallback c) {
        mSubId = subId;
        mContext = context;
        mControllerCallback = c;
        mHandler = new UceRequestHandler(this, looper);
        mThrottlingList = new ContactThrottlingList(mSubId);
        mRequestRepository = new UceRequestRepository(subId, mRequestMgrCallback);
        logi("create");
    }

    @VisibleForTesting
    public UceRequestManager(Context context, int subId, Looper looper, UceControllerCallback c,
            UceRequestRepository requestRepository) {
        mSubId = subId;
        mContext = context;
        mControllerCallback = c;
        mHandler = new UceRequestHandler(this, looper);
        mRequestRepository = requestRepository;
        mThrottlingList = new ContactThrottlingList(mSubId);
    }

    /**
     * Set the OptionsController for requestiong capabilities by OPTIONS mechanism.
     */
    public void setOptionsController(OptionsController controller) {
        mOptionsCtrl = controller;
    }

    /**
     * Set the SubscribeController for requesting capabilities by Subscribe mechanism.
     */
    public void setSubscribeController(SubscribeController controller) {
        mSubscribeCtrl = controller;
    }

    /**
     * Notify that the request manager instance is destroyed.
     */
    public void onDestroy() {
        logi("onDestroy");
        mIsDestroyed = true;
        mHandler.onDestroy();
        mThrottlingList.reset();
        mRequestRepository.onDestroy();
    }

    /**
     * Clear the throttling list.
     */
    public void resetThrottlingList() {
        mThrottlingList.reset();
    }

    /**
     * Send a new capability request. It is called by UceController.
     */
    public void sendCapabilityRequest(List<Uri> uriList, boolean skipFromCache,
            IRcsUceControllerCallback callback) throws RemoteException {
        if (mIsDestroyed) {
            callback.onError(RcsUceAdapter.ERROR_GENERIC_FAILURE, 0L);
            return;
        }
        sendRequestInternal(UceRequest.REQUEST_TYPE_CAPABILITY, uriList, skipFromCache, callback);
    }

    /**
     * Send a new availability request. It is called by UceController.
     */
    public void sendAvailabilityRequest(Uri uri, IRcsUceControllerCallback callback)
            throws RemoteException {
        if (mIsDestroyed) {
            callback.onError(RcsUceAdapter.ERROR_GENERIC_FAILURE, 0L);
            return;
        }
        sendRequestInternal(UceRequest.REQUEST_TYPE_AVAILABILITY,
                Collections.singletonList(uri), false /* skipFromCache */, callback);
    }

    private void sendRequestInternal(@UceRequestType int type, List<Uri> uriList,
            boolean skipFromCache, IRcsUceControllerCallback callback) throws RemoteException {
        UceRequestCoordinator requestCoordinator = null;
        List<Uri> nonCachedUris = uriList;
        if (FEATURE_SHORTCUT_QUEUE_FOR_CACHED_CAPS && !skipFromCache) {
            nonCachedUris = sendCachedCapInfoToRequester(type, uriList, callback);
            if (uriList.size() != nonCachedUris.size()) {
                logd("sendRequestInternal: shortcut queue for caps - request reduced from "
                        + uriList.size() + " entries to " + nonCachedUris.size() + " entries");
            } else {
                logd("sendRequestInternal: shortcut queue for caps - no cached caps.");
            }
            if (nonCachedUris.isEmpty()) {
                logd("sendRequestInternal: shortcut complete, sending success result");
                callback.onComplete();
                return;
            }
        }
        if (sUceUtilsProxy.isPresenceCapExchangeEnabled(mContext, mSubId) &&
                sUceUtilsProxy.isPresenceSupported(mContext, mSubId)) {
            requestCoordinator = createSubscribeRequestCoordinator(type, nonCachedUris,
                    skipFromCache, callback);
        } else if (sUceUtilsProxy.isSipOptionsSupported(mContext, mSubId)) {
            requestCoordinator = createOptionsRequestCoordinator(type, nonCachedUris, callback);
        }

        if (requestCoordinator == null) {
            logw("sendRequestInternal: Neither Presence nor OPTIONS are supported");
            callback.onError(RcsUceAdapter.ERROR_NOT_ENABLED, 0L);
            return;
        }

        StringBuilder builder = new StringBuilder("sendRequestInternal: ");
        builder.append("requestType=").append(type)
                .append(", requestCoordinatorId=").append(requestCoordinator.getCoordinatorId())
                .append(", taskId={")
                .append(requestCoordinator.getActivatedRequestTaskIds().stream()
                        .map(Object::toString).collect(Collectors.joining(","))).append("}");
        logd(builder.toString());

        // Add this RequestCoordinator to the UceRequestRepository.
        addRequestCoordinator(requestCoordinator);
    }

    /**
     * Try to get the valid capabilities associated with the URI List specified from the EAB cache.
     * If one or more of the numbers from the URI List have valid cached capabilities, return them
     * to the requester now and remove them from the returned List of URIs that will require a
     * network query.
     * @param type The type of query
     * @param uriList The List of URIs that we want to send cached capabilities for
     * @param callback The callback used to communicate with the remote requester
     * @return The List of URIs that were not found in the capability cache and will require a
     * network query.
     */
    private List<Uri> sendCachedCapInfoToRequester(int type, List<Uri> uriList,
            IRcsUceControllerCallback callback) {
        List<Uri> nonCachedUris = new ArrayList<>(uriList);
        List<RcsContactUceCapability> numbersWithCachedCaps =
                getCapabilitiesFromCache(type, nonCachedUris);
        try {
            if (!numbersWithCachedCaps.isEmpty()) {
                logd("sendCachedCapInfoToRequester: cached caps found for "
                        + numbersWithCachedCaps.size() + " entries. Notifying requester.");
                // Notify caller of the numbers that have cached caps
                callback.onCapabilitiesReceived(numbersWithCachedCaps);
            }
        } catch (RemoteException e) {
            logw("sendCachedCapInfoToRequester, error sending cap info back to requester: " + e);
        }
        // remove these numbers from the numbers pending a cap query from the network.
        for (RcsContactUceCapability c : numbersWithCachedCaps) {
            nonCachedUris.removeIf(uri -> c.getContactUri().equals(uri));
        }
        return nonCachedUris;
    }

    /**
     * Get the capabilities for the List of given URIs
     * @param requestType The request type, used to determine if the cached info is "fresh" enough.
     * @param uriList The List of URIs that we will be requesting cached capabilities for.
     * @return A list of capabilities corresponding to the subset of numbers that still have
     * valid cache data associated with them.
     */
    private List<RcsContactUceCapability> getCapabilitiesFromCache(int requestType,
            List<Uri> uriList) {
        List<EabCapabilityResult> resultList = Collections.emptyList();
        if (requestType == UceRequest.REQUEST_TYPE_CAPABILITY) {
            resultList = mRequestMgrCallback.getCapabilitiesFromCache(uriList);
        } else if (requestType == UceRequest.REQUEST_TYPE_AVAILABILITY) {
            // Always get the first element if the request type is availability.
            resultList = Collections.singletonList(
                    mRequestMgrCallback.getAvailabilityFromCache(uriList.get(0)));
        }
        // Map from EabCapabilityResult -> RcsContactUceCapability.
        // Pull out only items that have valid cache data.
        return resultList.stream().filter(Objects::nonNull)
                .filter(result -> result.getStatus() == EabCapabilityResult.EAB_QUERY_SUCCESSFUL)
                .map(EabCapabilityResult::getContactCapabilities)
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    private UceRequestCoordinator createSubscribeRequestCoordinator(final @UceRequestType int type,
            final List<Uri> uriList, boolean skipFromCache, IRcsUceControllerCallback callback) {
        SubscribeRequestCoordinator.Builder builder;

        if (!sUceUtilsProxy.isPresenceGroupSubscribeEnabled(mContext, mSubId)) {
            // When the group subscribe is disabled, each contact is required to be encapsulated
            // into individual UceRequest.
            List<UceRequest> requestList = new ArrayList<>();
            uriList.forEach(uri -> {
                List<Uri> individualUri = Collections.singletonList(uri);
                // Entity-uri, which is used as a request-uri, uses only a single subscription case
                List<RcsContactUceCapability> capabilities =
                        getCapabilitiesFromCache(type, individualUri);
                if (!capabilities.isEmpty()) {
                    RcsContactUceCapability capability = capabilities.get(0);
                    Uri entityUri = capability.getEntityUri();
                    if (entityUri != null) {
                        // The query uri has been replaced by the stored entity uri.
                        individualUri = Collections.singletonList(entityUri);
                    } else {
                        if (UceUtils.isSipUriForPresenceSubscribeEnabled(mContext, mSubId)) {
                            individualUri = Collections.singletonList(getSipUriFromUri(uri));
                        }
                    }
                } else {
                    if (UceUtils.isSipUriForPresenceSubscribeEnabled(mContext, mSubId)) {
                        individualUri = Collections.singletonList(getSipUriFromUri(uri));
                    }
                }
                UceRequest request = createSubscribeRequest(type, individualUri, skipFromCache);
                requestList.add(request);
            });
            builder = new SubscribeRequestCoordinator.Builder(mSubId, requestList,
                    mRequestMgrCallback);
            builder.setCapabilitiesCallback(callback);
        } else {
            // Even when the group subscribe is supported by the network, the number of contacts in
            // a UceRequest still cannot exceed the maximum.
            List<UceRequest> requestList = new ArrayList<>();
            final int rclMaxNumber = sUceUtilsProxy.getRclMaxNumberEntries(mSubId);
            int numRequestCoordinators = uriList.size() / rclMaxNumber;
            for (int count = 0; count < numRequestCoordinators; count++) {
                List<Uri> subUriList = new ArrayList<>();
                for (int index = 0; index < rclMaxNumber; index++) {
                    subUriList.add(uriList.get(count * rclMaxNumber + index));
                }
                requestList.add(createSubscribeRequest(type, subUriList, skipFromCache));
            }

            List<Uri> subUriList = new ArrayList<>();
            for (int i = numRequestCoordinators * rclMaxNumber; i < uriList.size(); i++) {
                subUriList.add(uriList.get(i));
            }
            requestList.add(createSubscribeRequest(type, subUriList, skipFromCache));

            builder = new SubscribeRequestCoordinator.Builder(mSubId, requestList,
                    mRequestMgrCallback);
            builder.setCapabilitiesCallback(callback);
        }
        return builder.build();
    }

    private UceRequestCoordinator createOptionsRequestCoordinator(@UceRequestType int type,
            List<Uri> uriList, IRcsUceControllerCallback callback) {
        OptionsRequestCoordinator.Builder builder;
        List<UceRequest> requestList = new ArrayList<>();
        uriList.forEach(uri -> {
            List<Uri> individualUri = Collections.singletonList(uri);
            UceRequest request = createOptionsRequest(type, individualUri, false);
            requestList.add(request);
        });
        builder = new OptionsRequestCoordinator.Builder(mSubId, requestList, mRequestMgrCallback);
        builder.setCapabilitiesCallback(callback);
        return builder.build();
    }

    private CapabilityRequest createSubscribeRequest(int type, List<Uri> uriList,
            boolean skipFromCache) {
        CapabilityRequest request = new SubscribeRequest(mSubId, type, mRequestMgrCallback,
                mSubscribeCtrl);
        request.setContactUri(uriList);
        request.setSkipGettingFromCache(skipFromCache);
        return request;
    }

    private CapabilityRequest createOptionsRequest(int type, List<Uri> uriList,
            boolean skipFromCache) {
        CapabilityRequest request = new OptionsRequest(mSubId, type, mRequestMgrCallback,
                mOptionsCtrl);
        request.setContactUri(uriList);
        request.setSkipGettingFromCache(skipFromCache);
        return request;
    }

    /**
     * Retrieve the device's capabilities. This request is from the ImsService to send the
     * capabilities to the remote side.
     */
    public void retrieveCapabilitiesForRemote(Uri contactUri, List<String> remoteCapabilities,
            IOptionsRequestCallback requestCallback) {
        RemoteOptionsRequest request = new RemoteOptionsRequest(mSubId, mRequestMgrCallback);
        request.setContactUri(Collections.singletonList(contactUri));
        request.setRemoteFeatureTags(remoteCapabilities);

        // If the remote number is blocked, do not send capabilities back.
        String number = getNumberFromUri(contactUri);
        if (!TextUtils.isEmpty(number)) {
            request.setIsRemoteNumberBlocked(sUceUtilsProxy.isNumberBlocked(mContext, number));
        }

        // Create the RemoteOptionsCoordinator instance
        RemoteOptionsCoordinator.Builder CoordBuilder = new RemoteOptionsCoordinator.Builder(
                mSubId, Collections.singletonList(request), mRequestMgrCallback);
        CoordBuilder.setOptionsRequestCallback(requestCallback);
        RemoteOptionsCoordinator requestCoordinator = CoordBuilder.build();

        StringBuilder builder = new StringBuilder("retrieveCapabilitiesForRemote: ");
        builder.append("requestCoordinatorId ").append(requestCoordinator.getCoordinatorId())
                .append(", taskId={")
                .append(requestCoordinator.getActivatedRequestTaskIds().stream()
                        .map(Object::toString).collect(Collectors.joining(","))).append("}");
        logd(builder.toString());

        // Add this RequestCoordinator to the UceRequestRepository.
        addRequestCoordinator(requestCoordinator);
    }

    private static class UceRequestHandler extends Handler {
        private static final int EVENT_EXECUTE_REQUEST = 1;
        private static final int EVENT_REQUEST_UPDATED = 2;
        private static final int EVENT_REQUEST_TIMEOUT = 3;
        private static final int EVENT_REQUEST_FINISHED = 4;
        private static final int EVENT_COORDINATOR_FINISHED = 5;

        private final Map<Long, SomeArgs> mRequestTimeoutTimers;
        private final WeakReference<UceRequestManager> mUceRequestMgrRef;

        public UceRequestHandler(UceRequestManager requestManager, Looper looper) {
            super(looper);
            mRequestTimeoutTimers = new HashMap<>();
            mUceRequestMgrRef = new WeakReference<>(requestManager);
        }

        /**
         * Send the capabilities request message.
         */
        public void sendRequestMessage(Long coordinatorId, Long taskId, long delayTimeMs) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = coordinatorId;
            args.arg2 = taskId;

            Message message = obtainMessage();
            message.what = EVENT_EXECUTE_REQUEST;
            message.obj = args;
            sendMessageDelayed(message, delayTimeMs);
        }

        /**
         * Send the Uce request updated message.
         */
        public void sendRequestUpdatedMessage(Long coordinatorId, Long taskId,
                @UceRequestUpdate int requestEvent) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = coordinatorId;
            args.arg2 = taskId;
            args.argi1 = requestEvent;

            Message message = obtainMessage();
            message.what = EVENT_REQUEST_UPDATED;
            message.obj = args;
            sendMessage(message);
        }

        /**
         * Set the timeout timer to cancel the capabilities request.
         */
        public void sendRequestTimeoutTimerMessage(Long coordId, Long taskId, Long timeoutAfterMs) {
            synchronized (mRequestTimeoutTimers) {
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = coordId;
                args.arg2 = taskId;

                // Add the message object to the collection. It can be used to find this message
                // when the request is completed and remove the timeout timer.
                mRequestTimeoutTimers.put(taskId, args);

                Message message = obtainMessage();
                message.what = EVENT_REQUEST_TIMEOUT;
                message.obj = args;
                sendMessageDelayed(message, timeoutAfterMs);
            }
        }

        /**
         * Remove the timeout timer because the capabilities request is finished.
         */
        public void removeRequestTimeoutTimer(Long taskId) {
            synchronized (mRequestTimeoutTimers) {
                SomeArgs args = mRequestTimeoutTimers.remove(taskId);
                if (args == null) {
                    return;
                }
                Log.d(LOG_TAG, "removeRequestTimeoutTimer: taskId=" + taskId);
                removeMessages(EVENT_REQUEST_TIMEOUT, args);
                args.recycle();
            }
        }

        public void sendRequestFinishedMessage(Long coordinatorId, Long taskId) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = coordinatorId;
            args.arg2 = taskId;

            Message message = obtainMessage();
            message.what = EVENT_REQUEST_FINISHED;
            message.obj = args;
            sendMessage(message);
        }

        /**
         * Finish the UceRequestCoordinator associated with the given id.
         */
        public void sendRequestCoordinatorFinishedMessage(Long coordinatorId) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = coordinatorId;

            Message message = obtainMessage();
            message.what = EVENT_COORDINATOR_FINISHED;
            message.obj = args;
            sendMessage(message);
        }

        /**
         * Remove all the messages from the handler
         */
        public void onDestroy() {
            removeCallbacksAndMessages(null);
            // Recycle all the arguments in the mRequestTimeoutTimers
            synchronized (mRequestTimeoutTimers) {
                mRequestTimeoutTimers.forEach((taskId, args) -> {
                    try {
                        args.recycle();
                    } catch (Exception e) {}
                });
                mRequestTimeoutTimers.clear();
            }
        }

        @Override
        public void handleMessage(Message msg) {
            UceRequestManager requestManager = mUceRequestMgrRef.get();
            if (requestManager == null) {
                return;
            }
            SomeArgs args = (SomeArgs) msg.obj;
            final Long coordinatorId = (Long) args.arg1;
            final Long taskId = (Long) Optional.ofNullable(args.arg2).orElse(-1L);
            final Integer requestEvent = Optional.ofNullable(args.argi1).orElse(-1);
            args.recycle();

            requestManager.logd("handleMessage: " + EVENT_DESCRIPTION.get(msg.what)
                    + ", coordinatorId=" + coordinatorId + ", taskId=" + taskId);
            switch (msg.what) {
                case EVENT_EXECUTE_REQUEST: {
                    UceRequest request = requestManager.getUceRequest(taskId);
                    if (request == null) {
                        requestManager.logw("handleMessage: cannot find request, taskId=" + taskId);
                        return;
                    }
                    request.executeRequest();
                    break;
                }
                case EVENT_REQUEST_UPDATED: {
                    UceRequestCoordinator requestCoordinator =
                            requestManager.getRequestCoordinator(coordinatorId);
                    if (requestCoordinator == null) {
                        requestManager.logw("handleMessage: cannot find UceRequestCoordinator");
                        return;
                    }
                    requestCoordinator.onRequestUpdated(taskId, requestEvent);
                    break;
                }
                case EVENT_REQUEST_TIMEOUT: {
                    UceRequestCoordinator requestCoordinator =
                            requestManager.getRequestCoordinator(coordinatorId);
                    if (requestCoordinator == null) {
                        requestManager.logw("handleMessage: cannot find UceRequestCoordinator");
                        return;
                    }
                    // The timeout timer is triggered, remove this record from the collection.
                    synchronized (mRequestTimeoutTimers) {
                        mRequestTimeoutTimers.remove(taskId);
                    }
                    // Notify that the request is timeout.
                    requestCoordinator.onRequestUpdated(taskId,
                            UceRequestCoordinator.REQUEST_UPDATE_TIMEOUT);
                    break;
                }
                case EVENT_REQUEST_FINISHED: {
                    // Notify the repository that the request is finished.
                    requestManager.notifyRepositoryRequestFinished(taskId);
                    break;
                }
                case EVENT_COORDINATOR_FINISHED: {
                    UceRequestCoordinator requestCoordinator =
                            requestManager.removeRequestCoordinator(coordinatorId);
                    if (requestCoordinator != null) {
                        requestCoordinator.onFinish();
                    }
                    break;
                }
                default: {
                    break;
                }
            }
        }

        private static Map<Integer, String> EVENT_DESCRIPTION = new HashMap<>();
        static {
            EVENT_DESCRIPTION.put(EVENT_EXECUTE_REQUEST, "EXECUTE_REQUEST");
            EVENT_DESCRIPTION.put(EVENT_REQUEST_UPDATED, "REQUEST_UPDATE");
            EVENT_DESCRIPTION.put(EVENT_REQUEST_TIMEOUT, "REQUEST_TIMEOUT");
            EVENT_DESCRIPTION.put(EVENT_REQUEST_FINISHED, "REQUEST_FINISHED");
            EVENT_DESCRIPTION.put(EVENT_COORDINATOR_FINISHED, "REMOVE_COORDINATOR");
        }
    }

    private void addRequestCoordinator(UceRequestCoordinator coordinator) {
        mRequestRepository.addRequestCoordinator(coordinator);
    }

    private UceRequestCoordinator removeRequestCoordinator(Long coordinatorId) {
        return mRequestRepository.removeRequestCoordinator(coordinatorId);
    }

    private UceRequestCoordinator getRequestCoordinator(Long coordinatorId) {
        return mRequestRepository.getRequestCoordinator(coordinatorId);
    }

    private UceRequest getUceRequest(Long taskId) {
        return mRequestRepository.getUceRequest(taskId);
    }

    private void notifyRepositoryRequestFinished(Long taskId) {
        mRequestRepository.notifyRequestFinished(taskId);
    }

    private Uri getSipUriFromUri(Uri uri) {
        Uri convertedUri = uri;
        String number = convertedUri.getSchemeSpecificPart();
        String[] numberParts = number.split("[@;:]");
        number = numberParts[0];

        TelephonyManager manager = mContext.getSystemService(TelephonyManager.class);
        if (manager.getIsimDomain() == null) {
            return convertedUri;
        }
        String simCountryIso = manager.getSimCountryIso();
        if (TextUtils.isEmpty(simCountryIso)) {
            return convertedUri;
        }
        simCountryIso = simCountryIso.toUpperCase();
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber phoneNumber = util.parse(number, simCountryIso);
            number = util.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
            String sipUri = "sip:" + number + "@" + manager.getIsimDomain();
            convertedUri = Uri.parse(sipUri);
        } catch (NumberParseException e) {
            Log.w(LOG_TAG, "formatNumber: could not format " + number + ", error: " + e);
        }
        return convertedUri;
    }

    @VisibleForTesting
    public UceRequestHandler getUceRequestHandler() {
        return mHandler;
    }

    @VisibleForTesting
    public RequestManagerCallback getRequestManagerCallback() {
        return mRequestMgrCallback;
    }

    private void logi(String log) {
        Log.i(LOG_TAG, getLogPrefix().append(log).toString());
    }

    private void logd(String log) {
        Log.d(LOG_TAG, getLogPrefix().append(log).toString());
    }

    private void logw(String log) {
        Log.w(LOG_TAG, getLogPrefix().append(log).toString());
    }

    private StringBuilder getLogPrefix() {
        StringBuilder builder = new StringBuilder("[");
        builder.append(mSubId);
        builder.append("] ");
        return builder;
    }

    private String getNumberFromUri(Uri uri) {
        if (uri == null) return null;
        String number = uri.getSchemeSpecificPart();
        String[] numberParts = number.split("[@;:]");

        if (numberParts.length == 0) {
            return null;
        }
        return numberParts[0];
    }
}
