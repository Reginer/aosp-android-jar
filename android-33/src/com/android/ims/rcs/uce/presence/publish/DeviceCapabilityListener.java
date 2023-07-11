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

package com.android.ims.rcs.uce.presence.publish;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.provider.Telephony;
import android.telecom.TelecomManager;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.ImsMmTelManager.CapabilityCallback;
import android.telephony.ims.ImsRcsManager;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsRegistrationAttributes;
import android.telephony.ims.ProvisioningManager;
import android.telephony.ims.RegistrationManager;
import android.telephony.ims.feature.MmTelFeature.MmTelCapabilities;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.util.Log;

import com.android.ims.rcs.uce.UceStatsWriter;
import com.android.ims.rcs.uce.presence.publish.PublishController.PublishControllerCallback;
import com.android.ims.rcs.uce.presence.publish.PublishController.PublishTriggerType;
import com.android.ims.rcs.uce.util.UceUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.util.HandlerExecutor;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Listen to the device changes and notify the PublishController to publish the device's
 * capabilities to the Presence server.
 */
public class DeviceCapabilityListener {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "DeviceCapListener";

    private static final long REGISTER_IMS_CHANGED_DELAY = 15000L;  // 15 seconds

    private final UceStatsWriter mUceStatsWriter;

    /**
     * Used to inject ImsMmTelManager instances for testing.
     */
    @VisibleForTesting
    public interface ImsMmTelManagerFactory {
        ImsMmTelManager getImsMmTelManager(int subId);
    }

    /**
     * Used to inject ImsRcsManager instances for testing.
     */
    @VisibleForTesting
    public interface ImsRcsManagerFactory {
        ImsRcsManager getImsRcsManager(int subId);
    }

    /**
     * Used to inject ProvisioningManager instances for testing.
     */
    @VisibleForTesting
    public interface ProvisioningManagerFactory {
        ProvisioningManager getProvisioningManager(int subId);
    }

    /*
     * Handle registering IMS callback and triggering the publish request because of the
     * capabilities changed.
     */
    private class DeviceCapabilityHandler extends Handler {
        private static final long TRIGGER_PUBLISH_REQUEST_DELAY_MS = 500L;

        private static final int EVENT_REGISTER_IMS_CONTENT_CHANGE = 1;
        private static final int EVENT_UNREGISTER_IMS_CHANGE = 2;
        private static final int EVENT_REQUEST_PUBLISH = 3;
        private static final int EVENT_IMS_UNREGISTERED = 4;

        DeviceCapabilityHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            logd("handleMessage: " + msg.what);
            if (mIsDestroyed) return;
            switch (msg.what) {
                case EVENT_REGISTER_IMS_CONTENT_CHANGE:
                    registerImsProvisionCallback();
                    break;
                case EVENT_UNREGISTER_IMS_CHANGE:
                    unregisterImsProvisionCallback();
                    break;
                case EVENT_REQUEST_PUBLISH:
                    int triggerType = msg.arg1;
                    mCallback.requestPublishFromInternal(triggerType);
                    break;
                case EVENT_IMS_UNREGISTERED:
                    mCallback.updateImsUnregistered();
                    break;
            }
        }

        public void sendRegisterImsContentChangedMessage(long delay) {
            // Remove the existing message and send a new one with the delayed time.
            removeMessages(EVENT_REGISTER_IMS_CONTENT_CHANGE);
            Message msg = obtainMessage(EVENT_REGISTER_IMS_CONTENT_CHANGE);
            sendMessageDelayed(msg, delay);
        }

        public void removeRegisterImsContentChangedMessage() {
            removeMessages(EVENT_REGISTER_IMS_CONTENT_CHANGE);
        }

        public void sendUnregisterImsCallbackMessage() {
            removeMessages(EVENT_REGISTER_IMS_CONTENT_CHANGE);
            sendEmptyMessage(EVENT_UNREGISTER_IMS_CHANGE);
        }

        public void sendTriggeringPublishMessage(@PublishTriggerType int type) {
            logd("sendTriggeringPublishMessage: type=" + type);
            // Remove the existing message and resend a new message.
            removeMessages(EVENT_REQUEST_PUBLISH);
            Message message = obtainMessage();
            message.what = EVENT_REQUEST_PUBLISH;
            message.arg1 = type;
            sendMessageDelayed(message, TRIGGER_PUBLISH_REQUEST_DELAY_MS);
        }

        public void sendImsUnregisteredMessage() {
            logd("sendImsUnregisteredMessage");
            // Remove the existing message and resend a new message.
            removeMessages(EVENT_IMS_UNREGISTERED);
            Message msg = obtainMessage(EVENT_IMS_UNREGISTERED);
            sendMessageDelayed(msg, TRIGGER_PUBLISH_REQUEST_DELAY_MS);
        }
    }

    private final int mSubId;
    private final Context mContext;
    private final LocalLog mLocalLog = new LocalLog(UceUtils.LOG_SIZE);
    private volatile boolean mInitialized;
    private volatile boolean mIsDestroyed;
    private volatile boolean mIsRcsConnected;
    private volatile boolean mIsImsCallbackRegistered;

    // The callback to trigger the internal publish request
    private final PublishControllerCallback mCallback;
    private final DeviceCapabilityInfo mCapabilityInfo;
    private final HandlerThread mHandlerThread;
    private final DeviceCapabilityHandler mHandler;
    private final HandlerExecutor mHandlerExecutor;

    private ImsMmTelManager mImsMmTelManager;
    private ImsMmTelManagerFactory mImsMmTelManagerFactory = (subId) -> getImsMmTelManager(subId);

    private ImsRcsManager mImsRcsManager;
    private ImsRcsManagerFactory mImsRcsManagerFactory = (subId) -> getImsRcsManager(subId);

    private ProvisioningManager mProvisioningManager;
    private ProvisioningManagerFactory mProvisioningMgrFactory = (subId)
            -> ProvisioningManager.createForSubscriptionId(subId);

    private ContentObserver mMobileDataObserver = null;
    private ContentObserver mSimInfoContentObserver = null;

    private final Object mLock = new Object();

    public DeviceCapabilityListener(Context context, int subId, DeviceCapabilityInfo info,
            PublishControllerCallback callback, UceStatsWriter uceStatsWriter) {
        mSubId = subId;
        logi("create");

        mContext = context;
        mCallback = callback;
        mCapabilityInfo = info;
        mInitialized = false;
        mUceStatsWriter = uceStatsWriter;

        mHandlerThread = new HandlerThread("DeviceCapListenerThread");
        mHandlerThread.start();
        mHandler = new DeviceCapabilityHandler(mHandlerThread.getLooper());
        mHandlerExecutor = new HandlerExecutor(mHandler);
    }

    /**
     * Turn on the device capabilities changed listener
     */
    public void initialize() {
        synchronized (mLock) {
            if (mIsDestroyed) {
                logw("initialize: This instance is already destroyed");
                return;
            }
            if (mInitialized) return;

            logi("initialize");
            mImsMmTelManager = mImsMmTelManagerFactory.getImsMmTelManager(mSubId);
            mImsRcsManager = mImsRcsManagerFactory.getImsRcsManager(mSubId);
            mProvisioningManager = mProvisioningMgrFactory.getProvisioningManager(mSubId);
            registerReceivers();
            registerImsProvisionCallback();

            mInitialized = true;
        }
    }

    // The RcsFeature has been connected to the framework
    public void onRcsConnected() {
        mIsRcsConnected = true;
        mHandler.sendRegisterImsContentChangedMessage(0L);
    }

    // The framework has lost the binding to the RcsFeature.
    public void onRcsDisconnected() {
        mIsRcsConnected = false;
        mHandler.sendUnregisterImsCallbackMessage();
    }

    /**
     * Notify the instance is destroyed
     */
    public void onDestroy() {
        logi("onDestroy");
        mIsDestroyed = true;
        synchronized (mLock) {
            if (!mInitialized) return;
            logi("turnOffListener");
            mInitialized = false;
            unregisterReceivers();
            unregisterImsProvisionCallback();
            mHandlerThread.quit();
        }
    }

    /*
     * Register receivers to listen to the data changes.
     */
    private void registerReceivers() {
        logd("registerReceivers");
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(TelecomManager.ACTION_TTY_PREFERRED_MODE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);

        ContentResolver resolver = mContext.getContentResolver();
        if (resolver != null) {
            // Listen to the mobile data content changed.
            resolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.MOBILE_DATA), false,
                    getMobileDataObserver());
            // Listen to the SIM info content changed.
            resolver.registerContentObserver(Telephony.SimInfo.CONTENT_URI, false,
                    getSimInfoContentObserver());
        }
    }

    private void unregisterReceivers() {
        logd("unregisterReceivers");
        mContext.unregisterReceiver(mReceiver);
        ContentResolver resolver = mContext.getContentResolver();
        if (resolver != null) {
            resolver.unregisterContentObserver(getMobileDataObserver());
            resolver.unregisterContentObserver(getSimInfoContentObserver());
        }
    }

    private void registerImsProvisionCallback() {
        if (mIsImsCallbackRegistered) {
            logd("registerImsProvisionCallback: already registered.");
            return;
        }

        logd("registerImsProvisionCallback");
        try {
            // Register mmtel callback
            if (mImsMmTelManager != null) {
                mImsMmTelManager.registerImsRegistrationCallback(mHandlerExecutor,
                        mMmtelRegistrationCallback);
                mImsMmTelManager.registerMmTelCapabilityCallback(mHandlerExecutor,
                        mMmtelCapabilityCallback);
            }

            // Register rcs callback
            if (mImsRcsManager != null) {
                mImsRcsManager.registerImsRegistrationCallback(mHandlerExecutor,
                        mRcsRegistrationCallback);
            }

            // Register provisioning changed callback
            mProvisioningManager.registerProvisioningChangedCallback(mHandlerExecutor,
                    mProvisionChangedCallback);

            // Set the IMS callback is registered.
            mIsImsCallbackRegistered = true;
        } catch (ImsException e) {
            logw("registerImsProvisionCallback error: " + e);
            // Unregister the callback
            unregisterImsProvisionCallback();

            // Retry registering IMS callback only when the RCS is connected.
            if (mIsRcsConnected) {
                mHandler.sendRegisterImsContentChangedMessage(REGISTER_IMS_CHANGED_DELAY);
            }
        }
    }

    private void unregisterImsProvisionCallback() {
        logd("unregisterImsProvisionCallback");

        // Clear the registering IMS callback message from the handler thread
        mHandler.removeRegisterImsContentChangedMessage();

        // Unregister mmtel callback
        if (mImsMmTelManager != null) {
            try {
                mImsMmTelManager.unregisterImsRegistrationCallback(mMmtelRegistrationCallback);
            } catch (RuntimeException e) {
                logw("unregister MMTel registration error: " + e.getMessage());
            }
            try {
                mImsMmTelManager.unregisterMmTelCapabilityCallback(mMmtelCapabilityCallback);
            } catch (RuntimeException e) {
                logw("unregister MMTel capability error: " + e.getMessage());
            }
        }

        // Unregister rcs callback
        if (mImsRcsManager != null) {
            try {
                mImsRcsManager.unregisterImsRegistrationCallback(mRcsRegistrationCallback);
            } catch (RuntimeException e) {
                logw("unregister rcs capability error: " + e.getMessage());
            }
        }

        try {
            // Unregister provisioning changed callback
            mProvisioningManager.unregisterProvisioningChangedCallback(mProvisionChangedCallback);
        } catch (RuntimeException e) {
            logw("unregister provisioning callback error: " + e.getMessage());
        }

        // Clear the IMS callback registered flag.
        mIsImsCallbackRegistered = false;
    }

    @VisibleForTesting
    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            switch (intent.getAction()) {
                case TelecomManager.ACTION_TTY_PREFERRED_MODE_CHANGED:
                    int preferredMode = intent.getIntExtra(TelecomManager.EXTRA_TTY_PREFERRED_MODE,
                            TelecomManager.TTY_MODE_OFF);
                    handleTtyPreferredModeChanged(preferredMode);
                    break;

                case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                    boolean airplaneMode = intent.getBooleanExtra("state", false);
                    handleAirplaneModeChanged(airplaneMode);
                    break;
            }
        }
    };

    private ContentObserver getMobileDataObserver() {
        synchronized (mLock) {
            if (mMobileDataObserver == null) {
                mMobileDataObserver = new ContentObserver(new Handler(mHandler.getLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        boolean isEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                                Settings.Global.MOBILE_DATA, 1) == 1;
                        handleMobileDataChanged(isEnabled);
                    }
                };
            }
            return mMobileDataObserver;
        }
    }

    private ContentObserver getSimInfoContentObserver() {
        synchronized (mLock) {
            if (mSimInfoContentObserver == null) {
                mSimInfoContentObserver = new ContentObserver(new Handler(mHandler.getLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        if (mImsMmTelManager == null) {
                            logw("SimInfo change error: MmTelManager is null");
                            return;
                        }

                        try {
                            boolean isEnabled = mImsMmTelManager.isVtSettingEnabled();
                            handleVtSettingChanged(isEnabled);
                        } catch (RuntimeException e) {
                            logw("SimInfo change error: " + e);
                        }
                    }
                };
            }
            return mSimInfoContentObserver;
        }
    }

    private ImsMmTelManager getImsMmTelManager(int subId) {
        try {
            ImsManager imsManager = mContext.getSystemService(
                    android.telephony.ims.ImsManager.class);
            return (imsManager == null) ? null : imsManager.getImsMmTelManager(subId);
        } catch (IllegalArgumentException e) {
            logw("getImsMmTelManager error: " + e.getMessage());
            return null;
        }
    }

    private ImsRcsManager getImsRcsManager(int subId) {
        try {
            ImsManager imsManager = mContext.getSystemService(
                    android.telephony.ims.ImsManager.class);
            return (imsManager == null) ? null : imsManager.getImsRcsManager(subId);
        } catch (IllegalArgumentException e) {
            logw("getImsRcsManager error: " + e.getMessage());
            return null;
        }
    }

    @VisibleForTesting
    public final RegistrationManager.RegistrationCallback mRcsRegistrationCallback =
            new RegistrationManager.RegistrationCallback() {
                @Override
                public void onRegistered(ImsRegistrationAttributes attributes) {
                    synchronized (mLock) {
                        logi("onRcsRegistered: " + attributes);
                        if (!mIsImsCallbackRegistered) return;

                        List<String> featureTagList = new ArrayList<>(attributes.getFeatureTags());
                        int registrationTech = attributes.getRegistrationTechnology();

                        mUceStatsWriter.setImsRegistrationFeatureTagStats(
                                mSubId, featureTagList, registrationTech);
                        handleImsRcsRegistered(attributes);
                    }
                }

                @Override
                public void onUnregistered(ImsReasonInfo info) {
                    synchronized (mLock) {
                        logi("onRcsUnregistered: " + info);
                        if (!mIsImsCallbackRegistered) return;
                        mUceStatsWriter.setStoreCompleteImsRegistrationFeatureTagStats(mSubId);
                        handleImsRcsUnregistered();
                    }
                }

                @Override
                public void onSubscriberAssociatedUriChanged(Uri[] uris) {
                    synchronized (mLock) {
                        logi("onRcsSubscriberAssociatedUriChanged");
                        handleRcsSubscriberAssociatedUriChanged(uris, true);
                    }
                }
    };

    @VisibleForTesting
    public final RegistrationManager.RegistrationCallback mMmtelRegistrationCallback =
            new RegistrationManager.RegistrationCallback() {
                @Override
                public void onRegistered(@TransportType int transportType) {
                    synchronized (mLock) {
                        String type = AccessNetworkConstants.transportTypeToString(transportType);
                        logi("onMmTelRegistered: " + type);
                        if (!mIsImsCallbackRegistered) return;
                        handleImsMmtelRegistered(transportType);
                    }
                }

                @Override
                public void onUnregistered(ImsReasonInfo info) {
                    synchronized (mLock) {
                        logi("onMmTelUnregistered: " + info);
                        if (!mIsImsCallbackRegistered) return;
                        handleImsMmtelUnregistered();
                    }
                }

                @Override
                public void onSubscriberAssociatedUriChanged(Uri[] uris) {
                    synchronized (mLock) {
                        logi("onMmTelSubscriberAssociatedUriChanged");
                        handleMmTelSubscriberAssociatedUriChanged(uris, true);
                    }
                }
            };

    @VisibleForTesting
    public final ImsMmTelManager.CapabilityCallback mMmtelCapabilityCallback =
            new CapabilityCallback() {
                @Override
                public void onCapabilitiesStatusChanged(MmTelCapabilities capabilities) {
                    if (capabilities == null) {
                        logw("onCapabilitiesStatusChanged: parameter is null");
                        return;
                    }
                    synchronized (mLock) {
                        handleMmtelCapabilitiesStatusChanged(capabilities);
                    }
                }
            };

    @VisibleForTesting
    public final ProvisioningManager.Callback mProvisionChangedCallback =
            new ProvisioningManager.Callback() {
                @Override
                public void onProvisioningIntChanged(int item, int value) {
                    logi("onProvisioningIntChanged: item=" + item + ", value=" + value);
                    switch (item) {
                        case ProvisioningManager.KEY_EAB_PROVISIONING_STATUS:
                        case ProvisioningManager.KEY_VOLTE_PROVISIONING_STATUS:
                        case ProvisioningManager.KEY_VT_PROVISIONING_STATUS:
                            handleProvisioningChanged();
                            break;
                        case ProvisioningManager.KEY_RCS_PUBLISH_SOURCE_THROTTLE_MS:
                            handlePublishThrottleChanged(value);
                            break;
                    }
                }
            };

    private void handleTtyPreferredModeChanged(int preferredMode) {
        boolean isChanged = mCapabilityInfo.updateTtyPreferredMode(preferredMode);
        logi("TTY preferred mode changed: " + preferredMode + ", isChanged=" + isChanged);
        if (isChanged) {
            mHandler.sendTriggeringPublishMessage(
                PublishController.PUBLISH_TRIGGER_TTY_PREFERRED_CHANGE);
        }
    }

    private void handleAirplaneModeChanged(boolean state) {
        boolean isChanged = mCapabilityInfo.updateAirplaneMode(state);
        logi("Airplane mode changed: " + state + ", isChanged="+ isChanged);
        if (isChanged) {
            mHandler.sendTriggeringPublishMessage(
                    PublishController.PUBLISH_TRIGGER_AIRPLANE_MODE_CHANGE);
        }
    }

    private void handleMobileDataChanged(boolean isEnabled) {
        boolean isChanged = mCapabilityInfo.updateMobileData(isEnabled);
        logi("Mobile data changed: " + isEnabled + ", isChanged=" + isChanged);
        if (isChanged) {
            mHandler.sendTriggeringPublishMessage(
                    PublishController.PUBLISH_TRIGGER_MOBILE_DATA_CHANGE);
        }
    }

    private void handleVtSettingChanged(boolean isEnabled) {
        boolean isChanged = mCapabilityInfo.updateVtSetting(isEnabled);
        logi("VT setting changed: " + isEnabled + ", isChanged=" + isChanged);
        if (isChanged) {
            mHandler.sendTriggeringPublishMessage(
                    PublishController.PUBLISH_TRIGGER_VT_SETTING_CHANGE);
        }
    }

    /*
     * This method is called when the MMTEL is registered.
     */
    private void handleImsMmtelRegistered(int imsTransportType) {
        mCapabilityInfo.updateImsMmtelRegistered(imsTransportType);
        mHandler.sendTriggeringPublishMessage(
                PublishController.PUBLISH_TRIGGER_MMTEL_REGISTERED);
    }

    /*
     * This method is called when the MMTEL is unregistered.
     */
    private void handleImsMmtelUnregistered() {
        mCapabilityInfo.updateImsMmtelUnregistered();
        // When the MMTEL is unregistered, the mmtel associated uri should be cleared.
        handleMmTelSubscriberAssociatedUriChanged(null, false);

        // If the RCS is already unregistered, it informs that the IMS is unregistered.
        if (mCapabilityInfo.isImsRegistered() == false) {
            mHandler.sendImsUnregisteredMessage();
        }
    }

    /*
     * This method is called when the MMTEL associated uri has changed.
     */
    private void handleMmTelSubscriberAssociatedUriChanged(Uri[] uris, boolean triggerPublish) {
        Uri originalUri = mCapabilityInfo.getMmtelAssociatedUri();
        mCapabilityInfo.updateMmTelAssociatedUri(uris);
        Uri currentUri = mCapabilityInfo.getMmtelAssociatedUri();

        boolean hasChanged = !(Objects.equals(originalUri, currentUri));
        logi("handleMmTelSubscriberAssociatedUriChanged: triggerPublish=" + triggerPublish +
                ", hasChanged=" + hasChanged);

        if (triggerPublish && hasChanged) {
            mHandler.sendTriggeringPublishMessage(
                    PublishController.PUBLISH_TRIGGER_MMTEL_URI_CHANGE);
        }
    }

    private void handleMmtelCapabilitiesStatusChanged(MmTelCapabilities capabilities) {
        boolean isChanged = mCapabilityInfo.updateMmtelCapabilitiesChanged(capabilities);
        logi("MMTel capabilities status changed: isChanged=" + isChanged);
        if (isChanged) {
            mHandler.sendTriggeringPublishMessage(
                    PublishController.PUBLISH_TRIGGER_MMTEL_CAPABILITY_CHANGE);
        }
    }

    /*
     * This method is called when RCS is registered.
     */
    private void handleImsRcsRegistered(ImsRegistrationAttributes attr) {
        if (mCapabilityInfo.updateImsRcsRegistered(attr)) {
            mHandler.sendTriggeringPublishMessage(PublishController.PUBLISH_TRIGGER_RCS_REGISTERED);
        }
    }

    /*
     * This method is called when RCS is unregistered.
     */
    private void handleImsRcsUnregistered() {
        boolean hasChanged = mCapabilityInfo.updateImsRcsUnregistered();
        // When the RCS is unregistered, the rcs associated uri should be cleared.
        handleRcsSubscriberAssociatedUriChanged(null, false);
        // If the MMTEL is already unregistered, it informs that the IMS is unregistered.
        if (mCapabilityInfo.isImsRegistered() == false) {
            mHandler.sendImsUnregisteredMessage();
        }
    }

    /*
     * This method is called when the RCS associated uri has changed.
     */
    private void handleRcsSubscriberAssociatedUriChanged(Uri[] uris, boolean triggerPublish) {
        Uri originalUri = mCapabilityInfo.getRcsAssociatedUri();
        mCapabilityInfo.updateRcsAssociatedUri(uris);
        Uri currentUri = mCapabilityInfo.getRcsAssociatedUri();

        boolean hasChanged = !(Objects.equals(originalUri, currentUri));
        logi("handleRcsSubscriberAssociatedUriChanged: triggerPublish=" + triggerPublish +
                ", hasChanged=" + hasChanged);

        if (triggerPublish && hasChanged) {
            mHandler.sendTriggeringPublishMessage(PublishController.PUBLISH_TRIGGER_RCS_URI_CHANGE);
        }
    }

    /*
     * This method is called when the provisioning is changed
     */
    private void handleProvisioningChanged() {
        mHandler.sendTriggeringPublishMessage(
                PublishController.PUBLISH_TRIGGER_PROVISIONING_CHANGE);
    }

    /*
     * Update the publish throttle.
     */
    private void handlePublishThrottleChanged(int value) {
        mCallback.updatePublishThrottle(value);
    }

    @VisibleForTesting
    public Handler getHandler() {
        return mHandler;
    }

    @VisibleForTesting
    public void setImsMmTelManagerFactory(ImsMmTelManagerFactory factory) {
        mImsMmTelManagerFactory = factory;
    }

    @VisibleForTesting
    public void setImsRcsManagerFactory(ImsRcsManagerFactory factory) {
        mImsRcsManagerFactory = factory;
    }

    @VisibleForTesting
    public void setProvisioningMgrFactory(ProvisioningManagerFactory factory) {
        mProvisioningMgrFactory = factory;
    }

    @VisibleForTesting
    public void setImsCallbackRegistered(boolean registered) {
        mIsImsCallbackRegistered = registered;
    }

    private void logd(String log) {
        Log.d(LOG_TAG, getLogPrefix().append(log).toString());
        mLocalLog.log("[D] " + log);
    }

    private void logi(String log) {
        Log.i(LOG_TAG, getLogPrefix().append(log).toString());
        mLocalLog.log("[I] " + log);
    }

    private void logw(String log) {
        Log.w(LOG_TAG, getLogPrefix().append(log).toString());
        mLocalLog.log("[W] " + log);
    }

    private StringBuilder getLogPrefix() {
        StringBuilder builder = new StringBuilder("[");
        builder.append(mSubId);
        builder.append("] ");
        return builder;
    }

    public void dump(PrintWriter printWriter) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println("DeviceCapListener" + "[subId: " + mSubId + "]:");
        pw.increaseIndent();

        mCapabilityInfo.dump(pw);

        pw.println("Log:");
        pw.increaseIndent();
        mLocalLog.dump(pw);
        pw.decreaseIndent();
        pw.println("---");

        pw.decreaseIndent();
    }
}
