/*
 * Copyright 2018 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.CarrierConfigManager;
import android.telephony.INetworkService;
import android.telephony.INetworkServiceCallback;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.NetworkService;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;

import com.android.telephony.Rlog;

import java.util.Hashtable;
import java.util.Map;

/**
 * Class that serves as the layer between NetworkService and ServiceStateTracker. It helps binding,
 * sending request and registering for state change to NetworkService.
 */
public class NetworkRegistrationManager extends Handler {
    private final String mTag;

    private static final int EVENT_BIND_NETWORK_SERVICE = 1;

    private final int mTransportType;

    private final Phone mPhone;

    private final CarrierConfigManager mCarrierConfigManager;

    // Registrants who listens registration state change callback from this class.
    private final RegistrantList mRegStateChangeRegistrants = new RegistrantList();

    private INetworkService mINetworkService;

    private RegManagerDeathRecipient mDeathRecipient;

    private String mTargetBindingPackageName;

    private NetworkServiceConnection mServiceConnection;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED.equals(action)
                    && mPhone.getPhoneId() == intent.getIntExtra(
                    CarrierConfigManager.EXTRA_SLOT_INDEX, 0)) {
                // We should wait for carrier config changed event because the target binding
                // package name can come from the carrier config. Note that we still get this event
                // even when SIM is absent.
                logd("Carrier config changed. Try to bind network service.");
                sendEmptyMessage(EVENT_BIND_NETWORK_SERVICE);
            }
        }
    };

    public NetworkRegistrationManager(@TransportType int transportType, Phone phone) {
        mTransportType = transportType;
        mPhone = phone;

        String tagSuffix = "-" + ((transportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                ? "C" : "I") + "-" + mPhone.getPhoneId();
        mTag = "NRM" + tagSuffix;

        mCarrierConfigManager = (CarrierConfigManager) phone.getContext().getSystemService(
                Context.CARRIER_CONFIG_SERVICE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        try {
            Context contextAsUser = phone.getContext().createPackageContextAsUser(
                phone.getContext().getPackageName(), 0, UserHandle.ALL);
            contextAsUser.registerReceiver(mBroadcastReceiver, intentFilter,
                null /* broadcastPermission */, null);
        } catch (PackageManager.NameNotFoundException e) {
            loge("Package name not found: " + e.getMessage());
        }
        PhoneConfigurationManager.registerForMultiSimConfigChange(
                this, EVENT_BIND_NETWORK_SERVICE, null);

        sendEmptyMessage(EVENT_BIND_NETWORK_SERVICE);
    }

    /**
     * Handle message events
     *
     * @param msg The message to handle
     */
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_BIND_NETWORK_SERVICE:
                rebindService();
                break;
            default:
                loge("Unhandled event " + msg.what);
        }
    }

    public boolean isServiceConnected() {
        return (mINetworkService != null) && (mINetworkService.asBinder().isBinderAlive());
    }

    public void unregisterForNetworkRegistrationInfoChanged(Handler h) {
        mRegStateChangeRegistrants.remove(h);
    }

    public void registerForNetworkRegistrationInfoChanged(Handler h, int what, Object obj) {
        logd("registerForNetworkRegistrationInfoChanged");
        mRegStateChangeRegistrants.addUnique(h, what, obj);
    }

    private final Map<NetworkRegStateCallback, Message> mCallbackTable = new Hashtable();

    public void requestNetworkRegistrationInfo(@NetworkRegistrationInfo.Domain int domain,
                                               Message onCompleteMessage) {
        if (onCompleteMessage == null) return;

        if (!isServiceConnected()) {
            loge("service not connected. Domain = "
                    + ((domain == NetworkRegistrationInfo.DOMAIN_CS) ? "CS" : "PS"));
            onCompleteMessage.obj = new AsyncResult(onCompleteMessage.obj, null,
                    new IllegalStateException("Service not connected."));
            onCompleteMessage.sendToTarget();
            return;
        }

        NetworkRegStateCallback callback = new NetworkRegStateCallback();
        try {
            mCallbackTable.put(callback, onCompleteMessage);
            mINetworkService.requestNetworkRegistrationInfo(mPhone.getPhoneId(), domain, callback);
        } catch (RemoteException e) {
            loge("requestNetworkRegistrationInfo RemoteException " + e);
            mCallbackTable.remove(callback);
            onCompleteMessage.obj = new AsyncResult(onCompleteMessage.obj, null, e);
            onCompleteMessage.sendToTarget();
        }
    }

    private class RegManagerDeathRecipient implements IBinder.DeathRecipient {

        private final ComponentName mComponentName;

        RegManagerDeathRecipient(ComponentName name) {
            mComponentName = name;
        }

        @Override
        public void binderDied() {
            // TODO: try to restart the service.
            logd("Network service " + mComponentName + " for transport type "
                    + AccessNetworkConstants.transportTypeToString(mTransportType) + " died.");
        }
    }

    private class NetworkServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            logd("service " + name + " for transport "
                    + AccessNetworkConstants.transportTypeToString(mTransportType)
                    + " is now connected.");
            mINetworkService = INetworkService.Stub.asInterface(service);
            mDeathRecipient = new RegManagerDeathRecipient(name);
            try {
                service.linkToDeath(mDeathRecipient, 0);
                mINetworkService.createNetworkServiceProvider(mPhone.getPhoneId());
                mINetworkService.registerForNetworkRegistrationInfoChanged(mPhone.getPhoneId(),
                        new NetworkRegStateCallback());
            } catch (RemoteException exception) {
                // Remote exception means that the binder already died.
                logd("RemoteException " + exception);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            logd("service " + name + " for transport "
                    + AccessNetworkConstants.transportTypeToString(mTransportType)
                    + " is now disconnected.");
            mTargetBindingPackageName = null;
        }
    }

    private class NetworkRegStateCallback extends INetworkServiceCallback.Stub {
        @Override
        public void onRequestNetworkRegistrationInfoComplete(
                int result, NetworkRegistrationInfo info) {
            logd("onRequestNetworkRegistrationInfoComplete result: "
                    + result + ", info: " + info);
            Message onCompleteMessage = mCallbackTable.remove(this);
            if (onCompleteMessage != null) {
                onCompleteMessage.arg1 = result;
                onCompleteMessage.obj = new AsyncResult(onCompleteMessage.obj,
                        new NetworkRegistrationInfo(info), null);
                onCompleteMessage.sendToTarget();
            } else {
                loge("onCompleteMessage is null");
            }
        }

        @Override
        public void onNetworkStateChanged() {
            logd("onNetworkStateChanged");
            mRegStateChangeRegistrants.notifyRegistrants();
        }
    }

    private void unbindService() {
        if (mINetworkService != null && mINetworkService.asBinder().isBinderAlive()) {
            logd("unbinding service");
            // Remove the network availability updater and then unbind the service.
            try {
                mINetworkService.removeNetworkServiceProvider(mPhone.getPhoneId());
            } catch (RemoteException e) {
                loge("Cannot remove data service provider. " + e);
            }
        }

        if (mServiceConnection != null) {
            mPhone.getContext().unbindService(mServiceConnection);
        }
        mINetworkService = null;
        mServiceConnection = null;
        mTargetBindingPackageName = null;
    }

    private void bindService(String packageName) {
        if (mPhone == null || !SubscriptionManager.isValidPhoneId(mPhone.getPhoneId())) {
            loge("can't bindService with invalid phone or phoneId.");
            return;
        }

        if (TextUtils.isEmpty(packageName)) {
            loge("Can't find the binding package");
            return;
        }

        Intent intent = null;
        String className = getClassName();
        if (TextUtils.isEmpty(className)) {
            intent = new Intent(NetworkService.SERVICE_INTERFACE);
            intent.setPackage(packageName);
        } else {
            ComponentName cm = new ComponentName(packageName, className);
            intent = new Intent(NetworkService.SERVICE_INTERFACE).setComponent(cm);
        }

        try {
            // We bind this as a foreground service because it is operating directly on the SIM,
            // and we do not want it subjected to power-savings restrictions while doing so.
            logd("Trying to bind " + getPackageName() + " for transport "
                    + AccessNetworkConstants.transportTypeToString(mTransportType));
            mServiceConnection = new NetworkServiceConnection();
            if (!mPhone.getContext().bindService(intent, mServiceConnection,
                    Context.BIND_AUTO_CREATE)) {
                loge("Cannot bind to the data service.");
                return;
            }
            mTargetBindingPackageName = packageName;
        } catch (SecurityException e) {
            loge("bindService failed " + e);
        }
    }

    private void rebindService() {
        String packageName = getPackageName();
        // Do nothing if no need to rebind.
        if (SubscriptionManager.isValidPhoneId(mPhone.getPhoneId())
                && TextUtils.equals(packageName, mTargetBindingPackageName)) {
            logd("Service " + packageName + " already bound or being bound.");
            return;
        }

        unbindService();
        bindService(packageName);
    }

    private String getPackageName() {
        String packageName;
        int resourceId;
        String carrierConfig;

        switch (mTransportType) {
            case AccessNetworkConstants.TRANSPORT_TYPE_WWAN:
                resourceId = com.android.internal.R.string.config_wwan_network_service_package;
                carrierConfig = CarrierConfigManager
                        .KEY_CARRIER_NETWORK_SERVICE_WWAN_PACKAGE_OVERRIDE_STRING;
                break;
            case AccessNetworkConstants.TRANSPORT_TYPE_WLAN:
                resourceId = com.android.internal.R.string.config_wlan_network_service_package;
                carrierConfig = CarrierConfigManager
                        .KEY_CARRIER_NETWORK_SERVICE_WLAN_PACKAGE_OVERRIDE_STRING;
                break;
            default:
                throw new IllegalStateException("Transport type not WWAN or WLAN. type="
                        + mTransportType);
        }

        // Read package name from resource overlay
        packageName = mPhone.getContext().getResources().getString(resourceId);

        PersistableBundle b = mCarrierConfigManager.getConfigForSubId(mPhone.getSubId());

        if (b != null && !TextUtils.isEmpty(b.getString(carrierConfig))) {
            // If carrier config overrides it, use the one from carrier config
            packageName = b.getString(carrierConfig, packageName);
        }

        return packageName;
    }

    private String getClassName() {
        String className;
        int resourceId;
        String carrierConfig;

        switch (mTransportType) {
            case AccessNetworkConstants.TRANSPORT_TYPE_WWAN:
                resourceId = com.android.internal.R.string.config_wwan_network_service_class;
                carrierConfig = CarrierConfigManager
                        .KEY_CARRIER_NETWORK_SERVICE_WWAN_CLASS_OVERRIDE_STRING;
                break;
            case AccessNetworkConstants.TRANSPORT_TYPE_WLAN:
                resourceId = com.android.internal.R.string.config_wlan_network_service_class;
                carrierConfig = CarrierConfigManager
                        .KEY_CARRIER_NETWORK_SERVICE_WLAN_CLASS_OVERRIDE_STRING;
                break;
            default:
                throw new IllegalStateException("Transport type not WWAN or WLAN. type="
                        + mTransportType);
        }

        // Read class name from resource overlay
        className = mPhone.getContext().getResources().getString(resourceId);

        PersistableBundle b = mCarrierConfigManager.getConfigForSubId(mPhone.getSubId());

        if (b != null && !TextUtils.isEmpty(b.getString(carrierConfig))) {
            // If carrier config overrides it, use the one from carrier config
            className = b.getString(carrierConfig, className);
        }

        return className;
    }
    private void logd(String msg) {
        Rlog.d(mTag, msg);
    }

    private void loge(String msg) {
        Rlog.e(mTag, msg);
    }
}
