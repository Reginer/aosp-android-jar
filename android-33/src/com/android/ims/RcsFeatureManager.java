/*
 * Copyright (c) 2019 The Android Open Source Project
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

package com.android.ims;

import android.content.Context;
import android.net.Uri;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.telephony.BinderCacheManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyFrameworkInitializer;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsService;
import android.telephony.ims.RcsUceAdapter.StackPublishTriggerType;
import android.telephony.ims.RegistrationManager;
import android.telephony.ims.aidl.ICapabilityExchangeEventListener;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsRcsController;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.IImsRegistrationCallback;
import android.telephony.ims.aidl.IOptionsRequestCallback;
import android.telephony.ims.aidl.IOptionsResponseCallback;
import android.telephony.ims.aidl.IPublishResponseCallback;
import android.telephony.ims.aidl.ISipTransport;
import android.telephony.ims.aidl.ISubscribeResponseCallback;
import android.telephony.ims.feature.CapabilityChangeRequest;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.feature.RcsFeature;
import android.telephony.ims.feature.RcsFeature.RcsImsCapabilities;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.Log;

import com.android.ims.internal.IImsServiceFeatureCallback;
import com.android.internal.annotations.VisibleForTesting;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Encapsulates all logic related to the RcsFeature:
 * - Updating RcsFeature capabilities.
 * - Registering/Unregistering availability/registration callbacks.
 * - Querying Registration and Capability information.
 */
public class RcsFeatureManager implements FeatureUpdates {
    private static final String TAG = "RcsFeatureManager";
    private static boolean DBG = true;

    private static final int CAPABILITY_OPTIONS = RcsImsCapabilities.CAPABILITY_TYPE_OPTIONS_UCE;
    private static final int CAPABILITY_PRESENCE = RcsImsCapabilities.CAPABILITY_TYPE_PRESENCE_UCE;

    /**
     * The capability exchange event callbacks from the RcsFeature.
     */
    public interface CapabilityExchangeEventCallback {
        /**
         * Triggered by RcsFeature to publish the device's capabilities to the network.
         */
        void onRequestPublishCapabilities(@StackPublishTriggerType int publishTriggerType);

        /**
         * Notify that the devices is unpublished.
         */
        void onUnpublish();

        /**
         * Notify the framework that the ImsService has refreshed the PUBLISH
         * internally, which has resulted in a new PUBLISH result.
         * <p>
         * This method must be called to notify the framework of SUCCESS (200 OK) and FAILURE (300+)
         * codes in order to keep the AOSP stack up to date.
         */
        void onPublishUpdated(int reasonCode, String reasonPhrase,
                int reasonHeaderCause, String reasonHeaderText);

        /**
         * Receive a capabilities request from the remote client.
         */
        void onRemoteCapabilityRequest(Uri contactUri,
                List<String> remoteCapabilities, IOptionsRequestCallback cb);
    }

    /*
     * Setup the listener to listen to the requests and updates from ImsService.
     */
    private ICapabilityExchangeEventListener mCapabilityEventListener =
            new ICapabilityExchangeEventListener.Stub() {
                @Override
                public void onRequestPublishCapabilities(@StackPublishTriggerType int type) {
                    mCapabilityEventCallback.forEach(
                            callback -> callback.onRequestPublishCapabilities(type));
                }

                @Override
                public void onUnpublish() {
                    mCapabilityEventCallback.forEach(callback -> callback.onUnpublish());
                }

                @Override
                public void onPublishUpdated(int reasonCode, String reasonPhrase,
                        int reasonHeaderCause, String reasonHeaderText) {
                    mCapabilityEventCallback.forEach(callback -> callback.onPublishUpdated(
                            reasonCode, reasonPhrase, reasonHeaderCause, reasonHeaderText));
                }

                @Override
                public void onRemoteCapabilityRequest(Uri contactUri,
                        List<String> remoteCapabilities, IOptionsRequestCallback cb) {
                    mCapabilityEventCallback.forEach(
                            callback -> callback.onRemoteCapabilityRequest(
                                    contactUri, remoteCapabilities, cb));
                }
            };

    private final int mSlotId;
    private final Context mContext;
    private final Set<CapabilityExchangeEventCallback> mCapabilityEventCallback
            = new CopyOnWriteArraySet<>();
    private final BinderCacheManager<IImsRcsController> mBinderCache
            = new BinderCacheManager<>(RcsFeatureManager::getIImsRcsControllerInterface);

    @VisibleForTesting
    public RcsFeatureConnection mRcsFeatureConnection;

    /**
     * Use to obtain a FeatureConnector, which will maintain a consistent listener to the
     * RcsFeature attached to the specified slotId. If the RcsFeature changes (due to things like
     * SIM swap), a new RcsFeatureManager will be delivered to this Listener.
     * @param context The Context this connector should use.
     * @param slotId The slotId associated with the Listener and requested RcsFeature
     * @param listener The listener, which will be used to generate RcsFeatureManager instances.
     * @param executor The executor that the Listener callbacks will be called on.
     * @param logPrefix The prefix used in logging of the FeatureConnector for notable events.
     * @return A FeatureConnector, which will start delivering RcsFeatureManagers as the underlying
     * RcsFeature instances become available to the platform.
     * @see {@link FeatureConnector#connect()}.
     */
    public static FeatureConnector<RcsFeatureManager> getConnector(Context context, int slotId,
            FeatureConnector.Listener<RcsFeatureManager> listener, Executor executor,
            String logPrefix) {
        ArrayList<Integer> filter = new ArrayList<>();
        filter.add(ImsFeature.STATE_READY);
        return new FeatureConnector<>(context, slotId, RcsFeatureManager::new, logPrefix, filter,
                listener, executor);
    }

    /**
     * Use {@link #getConnector} to get an instance of this class.
     */
    private RcsFeatureManager(Context context, int slotId) {
        mContext = context;
        mSlotId = slotId;
    }

    /**
     * Opens a persistent connection to the RcsFeature. This must be called before the RcsFeature
     * can be used to communicate.
     */
    public void openConnection() throws android.telephony.ims.ImsException {
        try {
            mRcsFeatureConnection.setCapabilityExchangeEventListener(mCapabilityEventListener);
        } catch (RemoteException e){
            throw new android.telephony.ims.ImsException("Service is not available.",
                    android.telephony.ims.ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Closes the persistent connection to the RcsFeature. This must be called when this manager
     * wishes to no longer be used to communicate with the RcsFeature.
     */
    public void releaseConnection() {
        try {
            mRcsFeatureConnection.setCapabilityExchangeEventListener(null);
        } catch (RemoteException e){
            // Connection may not be available at this point.
        }
        mRcsFeatureConnection.close();
        mCapabilityEventCallback.clear();
    }

    /**
     * Adds a callback for {@link CapabilityExchangeEventCallback}.
     * Note: These callbacks will be sent on the binder thread used to notify the callback.
     */
    public void addCapabilityEventCallback(CapabilityExchangeEventCallback listener) {
        mCapabilityEventCallback.add(listener);
    }

    /**
     * Removes an existing {@link CapabilityExchangeEventCallback}.
     */
    public void removeCapabilityEventCallback(CapabilityExchangeEventCallback listener) {
        mCapabilityEventCallback.remove(listener);
    }

    /**
     * Update the capabilities for this RcsFeature.
     */
    public void updateCapabilities(int newSubId) throws android.telephony.ims.ImsException {
        boolean optionsSupport = isOptionsSupported(newSubId);
        boolean presenceSupported = isPresenceSupported(newSubId);

        logi("Update capabilities for slot " + mSlotId + " and sub " + newSubId + ": options="
                + optionsSupport+ ", presence=" + presenceSupported);

        if (optionsSupport || presenceSupported) {
            CapabilityChangeRequest request = new CapabilityChangeRequest();
            if (optionsSupport) {
                addRcsUceCapability(request, CAPABILITY_OPTIONS);
            }
            if (presenceSupported) {
                addRcsUceCapability(request, CAPABILITY_PRESENCE);
            }
            sendCapabilityChangeRequest(request);
        } else {
            disableAllRcsUceCapabilities();
        }
    }

    /**
     * Add a {@link RegistrationManager.RegistrationCallback} callback that gets called when IMS
     * registration has changed for a specific subscription.
     */
    public void registerImsRegistrationCallback(int subId, IImsRegistrationCallback callback)
            throws android.telephony.ims.ImsException {
        try {
            mRcsFeatureConnection.addCallbackForSubscription(subId, callback);
        } catch (IllegalStateException e) {
            loge("registerImsRegistrationCallback error: ", e);
            throw new android.telephony.ims.ImsException("Can not register callback",
                    android.telephony.ims.ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Add a {@link RegistrationManager.RegistrationCallback} callback that gets called when IMS
     * registration has changed, independent of the subscription it is currently on.
     */
    public void registerImsRegistrationCallback(IImsRegistrationCallback callback)
            throws android.telephony.ims.ImsException {
        try {
            mRcsFeatureConnection.addCallback(callback);
        } catch (IllegalStateException e) {
            loge("registerImsRegistrationCallback error: ", e);
            throw new android.telephony.ims.ImsException("Can not register callback",
                    android.telephony.ims.ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Removes a previously registered {@link RegistrationManager.RegistrationCallback} callback
     * that is associated with a specific subscription.
     */
    public void unregisterImsRegistrationCallback(int subId, IImsRegistrationCallback callback) {
        mRcsFeatureConnection.removeCallbackForSubscription(subId, callback);
    }

    /**
     * Removes a previously registered {@link RegistrationManager.RegistrationCallback} callback
     * that was not associated with a subscription.
     */
    public void unregisterImsRegistrationCallback(IImsRegistrationCallback callback) {
        mRcsFeatureConnection.removeCallback(callback);
    }

    /**
     * Get the IMS RCS registration technology for this Phone,
     * defined in {@link ImsRegistrationImplBase}.
     */
    public void getImsRegistrationTech(Consumer<Integer> callback) {
        try {
            int tech = mRcsFeatureConnection.getRegistrationTech();
            callback.accept(tech);
        } catch (RemoteException e) {
            loge("getImsRegistrationTech error: ", e);
            callback.accept(ImsRegistrationImplBase.REGISTRATION_TECH_NONE);
        }
    }

    /**
     * Register an ImsCapabilityCallback with RCS service, which will provide RCS availability
     * updates.
     */
    public void registerRcsAvailabilityCallback(int subId, IImsCapabilityCallback callback)
            throws android.telephony.ims.ImsException {
        try {
            mRcsFeatureConnection.addCallbackForSubscription(subId, callback);
        } catch (IllegalStateException e) {
            loge("registerRcsAvailabilityCallback: ", e);
            throw new android.telephony.ims.ImsException("Can not register callback",
                    android.telephony.ims.ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Remove an registered ImsCapabilityCallback from RCS service.
     */
    public void unregisterRcsAvailabilityCallback(int subId, IImsCapabilityCallback callback) {
            mRcsFeatureConnection.removeCallbackForSubscription(subId, callback);
    }

    public boolean isImsServiceCapable(@ImsService.ImsServiceCapability long capabilities)
            throws ImsException {
        try {
            return mRcsFeatureConnection.isCapable(capabilities);
        } catch (RemoteException e) {
            throw new ImsException(e.getMessage(), ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * @return The SipTransport interface if it exists or {@code null} if it does not exist due to
     * the ImsService not supporting it.
     */
    public ISipTransport getSipTransport() throws ImsException {
        if (!isImsServiceCapable(ImsService.CAPABILITY_SIP_DELEGATE_CREATION)) {
            return null;
        }
        return mRcsFeatureConnection.getSipTransport();
    }

    public IImsRegistration getImsRegistration() {
        return mRcsFeatureConnection.getRegistration();
    }

    /**
     * Query for the specific capability.
     */
    public boolean isCapable(
            @RcsImsCapabilities.RcsImsCapabilityFlag int capability,
            @ImsRegistrationImplBase.ImsRegistrationTech int radioTech)
            throws android.telephony.ims.ImsException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> capableRef = new AtomicReference<>();

        IImsCapabilityCallback callback = new IImsCapabilityCallback.Stub() {
            @Override
            public void onQueryCapabilityConfiguration(
                    int resultCapability, int resultRadioTech, boolean enabled) {
                if ((capability != resultCapability) || (radioTech != resultRadioTech)) {
                    return;
                }
                if (DBG) log("capable result:capability=" + capability + ", enabled=" + enabled);
                capableRef.set(enabled);
                latch.countDown();
            }

            @Override
            public void onCapabilitiesStatusChanged(int config) {
                // Don't handle it
            }

            @Override
            public void onChangeCapabilityConfigurationError(int capability, int radioTech,
                    int reason) {
                // Don't handle it
            }
        };

        try {
            if (DBG) log("Query capability: " + capability + ", radioTech=" + radioTech);
            mRcsFeatureConnection.queryCapabilityConfiguration(capability, radioTech, callback);
            return awaitResult(latch, capableRef);
        } catch (RemoteException e) {
            loge("isCapable error: ", e);
            throw new android.telephony.ims.ImsException("Can not determine capabilities",
                    android.telephony.ims.ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
        }
    }

    private static <T> T awaitResult(CountDownLatch latch, AtomicReference<T> resultRef) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return resultRef.get();
    }

    /**
     * Query the availability of an IMS RCS capability.
     */
    public boolean isAvailable(@RcsImsCapabilities.RcsImsCapabilityFlag int capability,
            @ImsRegistrationImplBase.ImsRegistrationTech int radioTech)
            throws android.telephony.ims.ImsException {
        try {
            if (mRcsFeatureConnection.getRegistrationTech() != radioTech) {
                return false;
            }
            int currentStatus = mRcsFeatureConnection.queryCapabilityStatus();
            return new RcsImsCapabilities(currentStatus).isCapable(capability);
        } catch (RemoteException e) {
            loge("isAvailable error: ", e);
            throw new android.telephony.ims.ImsException("Can not determine availability",
                    android.telephony.ims.ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Add UCE capabilities with given type.
     * @param capability the specific RCS UCE capability wants to enable
     */
    public void addRcsUceCapability(CapabilityChangeRequest request,
            @RcsImsCapabilities.RcsImsCapabilityFlag int capability) {
        request.addCapabilitiesToEnableForTech(capability,
                ImsRegistrationImplBase.REGISTRATION_TECH_NR);
        request.addCapabilitiesToEnableForTech(capability,
                ImsRegistrationImplBase.REGISTRATION_TECH_LTE);
        request.addCapabilitiesToEnableForTech(capability,
                ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN);
    }

    public void requestPublication(String pidfXml, IPublishResponseCallback responseCallback)
            throws RemoteException {
        mRcsFeatureConnection.requestPublication(pidfXml, responseCallback);
    }

    public void requestCapabilities(List<Uri> uris, ISubscribeResponseCallback c)
            throws RemoteException {
        mRcsFeatureConnection.requestCapabilities(uris, c);
    }

    public void sendOptionsCapabilityRequest(Uri contactUri, List<String> myCapabilities,
            IOptionsResponseCallback callback) throws RemoteException {
        mRcsFeatureConnection.sendOptionsCapabilityRequest(contactUri, myCapabilities, callback);
    }

    /**
     * Disable all of the UCE capabilities.
     */
    private void disableAllRcsUceCapabilities() throws android.telephony.ims.ImsException {
        final int techNr = ImsRegistrationImplBase.REGISTRATION_TECH_NR;
        final int techLte = ImsRegistrationImplBase.REGISTRATION_TECH_LTE;
        final int techIWlan = ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN;
        CapabilityChangeRequest request = new CapabilityChangeRequest();
        request.addCapabilitiesToDisableForTech(CAPABILITY_OPTIONS, techNr);
        request.addCapabilitiesToDisableForTech(CAPABILITY_OPTIONS, techLte);
        request.addCapabilitiesToDisableForTech(CAPABILITY_OPTIONS, techIWlan);
        request.addCapabilitiesToDisableForTech(CAPABILITY_PRESENCE, techNr);
        request.addCapabilitiesToDisableForTech(CAPABILITY_PRESENCE, techLte);
        request.addCapabilitiesToDisableForTech(CAPABILITY_PRESENCE, techIWlan);
        sendCapabilityChangeRequest(request);
    }

    private void sendCapabilityChangeRequest(CapabilityChangeRequest request)
            throws android.telephony.ims.ImsException {
        try {
            if (DBG) log("sendCapabilityChangeRequest: " + request);
            mRcsFeatureConnection.changeEnabledCapabilities(request, null);
        } catch (RemoteException e) {
            throw new android.telephony.ims.ImsException("Can not connect to service",
                    android.telephony.ims.ImsException.CODE_ERROR_SERVICE_UNAVAILABLE);
        }
    }

    private boolean isOptionsSupported(int subId) {
        return isCapabilityTypeSupported(mContext, subId, CAPABILITY_OPTIONS);
    }

    private boolean isPresenceSupported(int subId) {
        return isCapabilityTypeSupported(mContext, subId, CAPABILITY_PRESENCE);
    }

    /*
     * Check if the given type of capability is supported.
     */
    private static boolean isCapabilityTypeSupported(
        Context context, int subId, int capabilityType) {

        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.e(TAG, "isCapabilityTypeSupported: Invalid subId=" + subId);
            return false;
        }

        CarrierConfigManager configManager =
            (CarrierConfigManager) context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager == null) {
            Log.e(TAG, "isCapabilityTypeSupported: CarrierConfigManager is null, " + subId);
            return false;
        }

        PersistableBundle b = configManager.getConfigForSubId(subId);
        if (b == null) {
            Log.e(TAG, "isCapabilityTypeSupported: PersistableBundle is null, " + subId);
            return false;
        }

        if (capabilityType == CAPABILITY_OPTIONS) {
            return b.getBoolean(CarrierConfigManager.KEY_USE_RCS_SIP_OPTIONS_BOOL, false);
        } else if (capabilityType == CAPABILITY_PRESENCE) {
            return b.getBoolean(CarrierConfigManager.Ims.KEY_ENABLE_PRESENCE_PUBLISH_BOOL, false);
        }
        return false;
    }

    @Override
    public void registerFeatureCallback(int slotId, IImsServiceFeatureCallback cb) {
        IImsRcsController controller = mBinderCache.listenOnBinder(cb, () -> {
            try {
                cb.imsFeatureRemoved(
                        FeatureConnector.UNAVAILABLE_REASON_SERVER_UNAVAILABLE);
            } catch (RemoteException ignore) {} // This is local.
        });

        try {
            if (controller == null) {
                Log.e(TAG, "registerRcsFeatureListener: IImsRcsController is null");
                cb.imsFeatureRemoved(FeatureConnector.UNAVAILABLE_REASON_SERVER_UNAVAILABLE);
                return;
            }
            controller.registerRcsFeatureCallback(slotId, cb);
        } catch (ServiceSpecificException e) {
            try {
                switch (e.errorCode) {
                    case ImsException.CODE_ERROR_UNSUPPORTED_OPERATION:
                        cb.imsFeatureRemoved(FeatureConnector.UNAVAILABLE_REASON_IMS_UNSUPPORTED);
                        break;
                    default: {
                        cb.imsFeatureRemoved(
                                FeatureConnector.UNAVAILABLE_REASON_SERVER_UNAVAILABLE);
                    }
                }
            } catch (RemoteException ignore) {} // Already dead anyway if this happens.
        } catch (RemoteException e) {
            try {
                cb.imsFeatureRemoved(FeatureConnector.UNAVAILABLE_REASON_SERVER_UNAVAILABLE);
            } catch (RemoteException ignore) {} // Already dead if this happens.
        }
    }

    @Override
    public void unregisterFeatureCallback(IImsServiceFeatureCallback cb) {
        try {
            IImsRcsController imsRcsController = mBinderCache.removeRunnable(cb);
            if (imsRcsController != null) {
                imsRcsController.unregisterImsFeatureCallback(cb);
            }
        } catch (RemoteException e) {
            // This means that telephony died, so do not worry about it.
            Rlog.e(TAG, "unregisterImsFeatureCallback (RCS), RemoteException: "
                    + e.getMessage());
        }
    }

    private IImsRcsController getIImsRcsController() {
        return mBinderCache.getBinder();
    }

    private static IImsRcsController getIImsRcsControllerInterface() {
        IBinder binder = TelephonyFrameworkInitializer
                .getTelephonyServiceManager()
                .getTelephonyImsServiceRegisterer()
                .get();
        IImsRcsController c = IImsRcsController.Stub.asInterface(binder);
        return c;
    }

    @Override
    public void associate(ImsFeatureContainer c, int subId) {
        IImsRcsFeature f = IImsRcsFeature.Stub.asInterface(c.imsFeature);
        mRcsFeatureConnection = new RcsFeatureConnection(mContext, mSlotId, subId, f, c.imsConfig,
                c.imsRegistration, c.sipTransport);
    }

    @Override
    public void invalidate() {
        mRcsFeatureConnection.onRemovedOrDied();
    }

    @Override
    public void updateFeatureState(int state) {
        mRcsFeatureConnection.updateFeatureState(state);
    }

    @Override
    public void updateFeatureCapabilities(long capabilities) {
        mRcsFeatureConnection.updateFeatureCapabilities(capabilities);
    }

    /**
     * Testing interface used to mock SubscriptionManager in testing
     * @hide
     */
    @VisibleForTesting
    public interface SubscriptionManagerProxy {
        /**
         * Mock-able interface for {@link SubscriptionManager#getSubId(int)} used for testing.
         */
        int getSubId(int slotId);
    }

    public IImsConfig getConfig() {
        return mRcsFeatureConnection.getConfig();
    }

    private static SubscriptionManagerProxy sSubscriptionManagerProxy
            = slotId -> {
                int[] subIds = SubscriptionManager.getSubId(slotId);
                if (subIds != null) {
                    return subIds[0];
                }
                return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            };

    /**
     * Testing function used to mock SubscriptionManager in testing
     * @hide
     */
    @VisibleForTesting
    public static void setSubscriptionManager(SubscriptionManagerProxy proxy) {
        sSubscriptionManagerProxy = proxy;
    }

    private void log(String s) {
        Rlog.d(TAG + " [" + mSlotId + "]", s);
    }

    private void logi(String s) {
        Rlog.i(TAG + " [" + mSlotId + "]", s);
    }

    private void loge(String s) {
        Rlog.e(TAG + " [" + mSlotId + "]", s);
    }

    private void loge(String s, Throwable t) {
        Rlog.e(TAG + " [" + mSlotId + "]", s, t);
    }
}
