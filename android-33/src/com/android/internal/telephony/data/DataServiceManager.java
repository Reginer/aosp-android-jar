/*
 * Copyright 2021 The Android Open Source Project
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

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.LinkProperties;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.permission.LegacyPermissionManager;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.AnomalyReporter;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataProfile;
import android.telephony.data.DataService;
import android.telephony.data.DataServiceCallback;
import android.telephony.data.IDataService;
import android.telephony.data.IDataServiceCallback;
import android.telephony.data.NetworkSliceInfo;
import android.telephony.data.TrafficDescriptor;
import android.text.TextUtils;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConfigurationManager;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.telephony.Rlog;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Data service manager manages handling data requests and responses on data services (e.g.
 * Cellular data service, IWLAN data service).
 */
public class DataServiceManager extends Handler {
    private static final boolean DBG = true;

    static final String DATA_CALL_RESPONSE = "data_call_response";

    private static final int EVENT_BIND_DATA_SERVICE = 1;

    private static final int EVENT_WATCHDOG_TIMEOUT = 2;

    private static final long REQUEST_UNRESPONDED_TIMEOUT = 10 * MINUTE_IN_MILLIS; // 10 mins

    private static final long CHANGE_PERMISSION_TIMEOUT_MS = 15 * SECOND_IN_MILLIS; // 15 secs

    private final Phone mPhone;

    private final String mTag;

    private final CarrierConfigManager mCarrierConfigManager;
    private final AppOpsManager mAppOps;
    private final LegacyPermissionManager mPermissionManager;

    private final int mTransportType;

    private boolean mBound;

    private IDataService mIDataService;

    private DataServiceManagerDeathRecipient mDeathRecipient;

    private final RegistrantList mServiceBindingChangedRegistrants = new RegistrantList();

    private final Map<IBinder, Message> mMessageMap = new ConcurrentHashMap<>();

    private final RegistrantList mDataCallListChangedRegistrants = new RegistrantList();

    private final RegistrantList mApnUnthrottledRegistrants = new RegistrantList();

    private String mTargetBindingPackageName;

    private CellularDataServiceConnection mServiceConnection;

    private String mLastBoundPackageName;

    private List<DataCallResponse> mLastDataCallResponseList = Collections.EMPTY_LIST;

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
                if (DBG) log("Carrier config changed. Try to bind data service.");
                sendEmptyMessage(EVENT_BIND_DATA_SERVICE);
            }
        }
    };

    private class DataServiceManagerDeathRecipient implements IBinder.DeathRecipient {
        @Override
        public void binderDied() {
            // TODO: try to rebind the service.
            String message = "Data service " + mLastBoundPackageName +  " for transport type "
                    + AccessNetworkConstants.transportTypeToString(mTransportType) + " died.";
            loge(message);
            AnomalyReporter.reportAnomaly(UUID.fromString("fc1956de-c080-45de-8431-a1faab687110"),
                    message, mPhone.getCarrierId());

            // Cancel all pending requests
            for (Message m : mMessageMap.values()) {
                sendCompleteMessage(m, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
            }
            mMessageMap.clear();

            // Tear down all connections
            mLastDataCallResponseList = Collections.EMPTY_LIST;
            mDataCallListChangedRegistrants.notifyRegistrants(
                    new AsyncResult(null, Collections.EMPTY_LIST, null));
        }
    }

    private void grantPermissionsToService(String packageName) {
        final String[] pkgToGrant = {packageName};
        CountDownLatch latch = new CountDownLatch(1);
        try {
            mPermissionManager.grantDefaultPermissionsToEnabledTelephonyDataServices(
                    pkgToGrant, UserHandle.of(UserHandle.myUserId()), Runnable::run,
                    isSuccess -> {
                        if (isSuccess) {
                            latch.countDown();
                        } else {
                            loge("Failed to grant permissions to service.");
                        }
                    });
            TelephonyUtils.waitUntilReady(latch, CHANGE_PERMISSION_TIMEOUT_MS);
            mAppOps.setMode(AppOpsManager.OPSTR_MANAGE_IPSEC_TUNNELS,
                    UserHandle.myUserId(), pkgToGrant[0], AppOpsManager.MODE_ALLOWED);
            mAppOps.setMode(AppOpsManager.OPSTR_FINE_LOCATION,
                    UserHandle.myUserId(), pkgToGrant[0], AppOpsManager.MODE_ALLOWED);
        } catch (RuntimeException e) {
            loge("Binder to package manager died, permission grant for DataService failed.");
            throw e;
        }
    }

    /**
     * Loop through all DataServices installed on the system and revoke permissions from any that
     * are not currently the WWAN or WLAN data service.
     */
    private void revokePermissionsFromUnusedDataServices() {
        // Except the current data services from having their permissions removed.
        Set<String> dataServices = getAllDataServicePackageNames();
        for (int transportType : mPhone.getAccessNetworksManager().getAvailableTransports()) {
            dataServices.remove(getDataServicePackageName(transportType));
        }

        CountDownLatch latch = new CountDownLatch(1);
        try {
            String[] dataServicesArray = new String[dataServices.size()];
            dataServices.toArray(dataServicesArray);
            mPermissionManager.revokeDefaultPermissionsFromDisabledTelephonyDataServices(
                    dataServicesArray, UserHandle.of(UserHandle.myUserId()), Runnable::run,
                    isSuccess -> {
                        if (isSuccess) {
                            latch.countDown();
                        } else {
                            loge("Failed to revoke permissions from data services.");
                        }
                    });
            TelephonyUtils.waitUntilReady(latch, CHANGE_PERMISSION_TIMEOUT_MS);
            for (String pkg : dataServices) {
                mAppOps.setMode(AppOpsManager.OPSTR_MANAGE_IPSEC_TUNNELS, UserHandle.myUserId(),
                        pkg, AppOpsManager.MODE_ERRORED);
                mAppOps.setMode(AppOpsManager.OPSTR_FINE_LOCATION, UserHandle.myUserId(),
                        pkg, AppOpsManager.MODE_ERRORED);
            }
        } catch (RuntimeException e) {
            loge("Binder to package manager died; failed to revoke DataService permissions.");
            throw e;
        }
    }

    private final class CellularDataServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (DBG) log("onServiceConnected: " + name);
            mIDataService = IDataService.Stub.asInterface(service);
            mDeathRecipient = new DataServiceManagerDeathRecipient();
            mBound = true;
            mLastBoundPackageName = getDataServicePackageName();
            removeMessages(EVENT_WATCHDOG_TIMEOUT);

            try {
                service.linkToDeath(mDeathRecipient, 0);
                mIDataService.createDataServiceProvider(mPhone.getPhoneId());
                mIDataService.registerForDataCallListChanged(mPhone.getPhoneId(),
                        new DataServiceCallbackWrapper("dataCallListChanged"));
                mIDataService.registerForUnthrottleApn(mPhone.getPhoneId(),
                        new DataServiceCallbackWrapper("unthrottleApn"));
            } catch (RemoteException e) {
                loge("Remote exception. " + e);
                return;
            }
            mServiceBindingChangedRegistrants.notifyResult(true);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (DBG) log("onServiceDisconnected");
            removeMessages(EVENT_WATCHDOG_TIMEOUT);
            mIDataService = null;
            mBound = false;
            mServiceBindingChangedRegistrants.notifyResult(false);
            mTargetBindingPackageName = null;
        }
    }

    private final class DataServiceCallbackWrapper extends IDataServiceCallback.Stub {

        private final String mTag;

        DataServiceCallbackWrapper(String tag) {
            mTag = tag;
        }

        public String getTag() {
            return mTag;
        }

        @Override
        public void onSetupDataCallComplete(@DataServiceCallback.ResultCode int resultCode,
                DataCallResponse response) {
            if (DBG) {
                log("onSetupDataCallComplete. resultCode = " + resultCode + ", response = "
                        + response);
            }
            removeMessages(EVENT_WATCHDOG_TIMEOUT, DataServiceCallbackWrapper.this);
            Message msg = mMessageMap.remove(asBinder());
            if (msg != null) {
                msg.getData().putParcelable(DATA_CALL_RESPONSE, response);
                sendCompleteMessage(msg, resultCode);
            } else {
                loge("Unable to find the message for setup call response.");
            }
        }

        @Override
        public void onDeactivateDataCallComplete(@DataServiceCallback.ResultCode int resultCode) {
            if (DBG) log("onDeactivateDataCallComplete. resultCode = " + resultCode);
            removeMessages(EVENT_WATCHDOG_TIMEOUT, DataServiceCallbackWrapper.this);
            Message msg = mMessageMap.remove(asBinder());
            sendCompleteMessage(msg, resultCode);
        }

        @Override
        public void onSetInitialAttachApnComplete(@DataServiceCallback.ResultCode int resultCode) {
            if (DBG) log("onSetInitialAttachApnComplete. resultCode = " + resultCode);
            Message msg = mMessageMap.remove(asBinder());
            sendCompleteMessage(msg, resultCode);
        }

        @Override
        public void onSetDataProfileComplete(@DataServiceCallback.ResultCode int resultCode) {
            if (DBG) log("onSetDataProfileComplete. resultCode = " + resultCode);
            Message msg = mMessageMap.remove(asBinder());
            sendCompleteMessage(msg, resultCode);
        }

        @Override
        public void onRequestDataCallListComplete(@DataServiceCallback.ResultCode int resultCode,
                List<DataCallResponse> dataCallList) {
            if (DBG) {
                log("onRequestDataCallListComplete. resultCode = "
                        + DataServiceCallback.resultCodeToString(resultCode));
            }
            Message msg = mMessageMap.remove(asBinder());
            if (msg != null) {
                msg.getData().putParcelableList(DATA_CALL_RESPONSE, dataCallList);
            }
            sendCompleteMessage(msg, resultCode);

            // Handle data stall case on WWAN transport
            if (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
                if (mLastDataCallResponseList.size() != dataCallList.size()
                        || !mLastDataCallResponseList.containsAll(dataCallList)) {
                    String message = "RIL reported mismatched data call response list for WWAN: "
                            + "mLastDataCallResponseList=" + mLastDataCallResponseList
                            + ", dataCallList=" + dataCallList;
                    loge(message);
                    if (!dataCallList.stream().map(DataCallResponse::getId)
                            .collect(Collectors.toSet()).equals(mLastDataCallResponseList.stream()
                                    .map(DataCallResponse::getId).collect(Collectors.toSet()))) {
                        AnomalyReporter.reportAnomaly(
                                UUID.fromString("150323b2-360a-446b-a158-3ce6425821f6"),
                                message,
                                mPhone.getCarrierId());
                    }
                }
                onDataCallListChanged(dataCallList);
            }
        }

        @Override
        public void onDataCallListChanged(List<DataCallResponse> dataCallList) {
            mLastDataCallResponseList =
                    dataCallList != null ? dataCallList : Collections.EMPTY_LIST;
            mDataCallListChangedRegistrants.notifyRegistrants(
                    new AsyncResult(null, dataCallList, null));
        }

        @Override
        public void onHandoverStarted(@DataServiceCallback.ResultCode int resultCode) {
            if (DBG) log("onHandoverStarted. resultCode = " + resultCode);
            removeMessages(EVENT_WATCHDOG_TIMEOUT, DataServiceCallbackWrapper.this);
            Message msg = mMessageMap.remove(asBinder());
            sendCompleteMessage(msg, resultCode);
        }

        @Override
        public void onHandoverCancelled(@DataServiceCallback.ResultCode int resultCode) {
            if (DBG) log("onHandoverCancelled. resultCode = " + resultCode);
            removeMessages(EVENT_WATCHDOG_TIMEOUT, DataServiceCallbackWrapper.this);
            Message msg = mMessageMap.remove(asBinder());
            sendCompleteMessage(msg, resultCode);
        }

        @Override
        public void onApnUnthrottled(String apn) {
            if (apn != null) {
                mApnUnthrottledRegistrants.notifyRegistrants(
                        new AsyncResult(null, apn, null));
            } else {
                loge("onApnUnthrottled: apn is null");
            }
        }

        @Override
        public void onDataProfileUnthrottled(DataProfile dataProfile) {
            if (dataProfile != null) {
                mApnUnthrottledRegistrants.notifyRegistrants(
                        new AsyncResult(null, dataProfile, null));
            } else {
                loge("onDataProfileUnthrottled: dataProfile is null");
            }
        }
    }

    /**
     * Constructor
     *
     * @param phone The phone instance
     * @param looper Looper for the handler
     * @param transportType The transport type
     */
    public DataServiceManager(@NonNull Phone phone, @NonNull Looper looper,
            @TransportType int transportType) {
        super(looper);
        mPhone = phone;
        mTransportType = transportType;
        mTag = "DSM-" + (mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN ? "C-"
                : "I-") + mPhone.getPhoneId();
        mBound = false;
        mCarrierConfigManager = (CarrierConfigManager) phone.getContext().getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        // NOTE: Do NOT use AppGlobals to retrieve the permission manager; AppGlobals
        // caches the service instance, but we need to explicitly request a new service
        // so it can be mocked out for tests
        mPermissionManager = (LegacyPermissionManager) phone.getContext().getSystemService(
                Context.LEGACY_PERMISSION_SERVICE);
        mAppOps = (AppOpsManager) phone.getContext().getSystemService(Context.APP_OPS_SERVICE);

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
                this, EVENT_BIND_DATA_SERVICE, null);

        rebindDataService();
    }

    /**
     * Handle message events
     *
     * @param msg The message to handle
     */
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_BIND_DATA_SERVICE:
                rebindDataService();
                break;
            case EVENT_WATCHDOG_TIMEOUT:
                handleRequestUnresponded((DataServiceCallbackWrapper) msg.obj);
                break;
            default:
                loge("Unhandled event " + msg.what);
        }
    }

    private void handleRequestUnresponded(DataServiceCallbackWrapper callback) {
        String message = "Request " + callback.getTag() + " unresponded on transport "
                + AccessNetworkConstants.transportTypeToString(mTransportType) + " in "
                + REQUEST_UNRESPONDED_TIMEOUT / 1000 + " seconds.";
        log(message);
        // Using fixed UUID to avoid duplicate bugreport notification
        AnomalyReporter.reportAnomaly(
                UUID.fromString("f5d5cbe6-9bd6-4009-b764-42b1b649b1de"),
                message, mPhone.getCarrierId());
    }

    private void unbindDataService() {
        // Start by cleaning up all packages that *shouldn't* have permissions.
        revokePermissionsFromUnusedDataServices();
        if (mIDataService != null && mIDataService.asBinder().isBinderAlive()) {
            log("unbinding service");
            // Remove the network availability updater and then unbind the service.
            try {
                mIDataService.removeDataServiceProvider(mPhone.getPhoneId());
            } catch (RemoteException e) {
                loge("Cannot remove data service provider. " + e);
            }
        }

        if (mServiceConnection != null) {
            mPhone.getContext().unbindService(mServiceConnection);
        }
        mIDataService = null;
        mServiceConnection = null;
        mTargetBindingPackageName = null;
        mBound = false;
    }

    private void bindDataService(String packageName) {
        if (mPhone == null || !SubscriptionManager.isValidPhoneId(mPhone.getPhoneId())) {
            loge("can't bindDataService with invalid phone or phoneId.");
            return;
        }

        if (TextUtils.isEmpty(packageName)) {
            loge("Can't find the binding package");
            return;
        }

        Intent intent = null;
        String className = getDataServiceClassName();
        if (TextUtils.isEmpty(className)) {
            intent = new Intent(DataService.SERVICE_INTERFACE);
            intent.setPackage(packageName);
        } else {
            ComponentName cm = new ComponentName(packageName, className);
            intent = new Intent(DataService.SERVICE_INTERFACE).setComponent(cm);
        }

        // Then pre-emptively grant the permissions to the package we will bind.
        grantPermissionsToService(packageName);

        try {
            mServiceConnection = new CellularDataServiceConnection();
            if (!mPhone.getContext().bindService(
                    intent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
                loge("Cannot bind to the data service.");
                return;
            }
            mTargetBindingPackageName = packageName;
        } catch (Exception e) {
            loge("Cannot bind to the data service. Exception: " + e);
        }
    }

    private void rebindDataService() {
        String packageName = getDataServicePackageName();
        // Do nothing if no need to rebind.
        if (SubscriptionManager.isValidPhoneId(mPhone.getPhoneId())
                && TextUtils.equals(packageName, mTargetBindingPackageName)) {
            if (DBG) log("Service " + packageName + " already bound or being bound.");
            return;
        }

        unbindDataService();
        bindDataService(packageName);
    }

    private @NonNull Set<String> getAllDataServicePackageNames() {
        // Cowardly using the public PackageManager interface here.
        // Note: This matches only packages that were installed on the system image. If we ever
        // expand the permissions model to allow CarrierPrivileged packages, then this will need
        // to be updated.
        List<ResolveInfo> dataPackages =
                mPhone.getContext().getPackageManager().queryIntentServices(
                        new Intent(DataService.SERVICE_INTERFACE),
                                PackageManager.MATCH_SYSTEM_ONLY);
        HashSet<String> packageNames = new HashSet<>();
        for (ResolveInfo info : dataPackages) {
            if (info.serviceInfo == null) continue;
            packageNames.add(info.serviceInfo.packageName);
        }
        return packageNames;
    }

    /**
     * Get the data service package name for our current transport type.
     *
     * @return package name of the data service package for the current transportType.
     */
    public String getDataServicePackageName() {
        return getDataServicePackageName(mTransportType);
    }

    /**
     * Get the data service package by transport type.
     *
     * When we bind to a DataService package, we need to revoke permissions from stale
     * packages; we need to exclude data packages for all transport types, so we need to
     * to be able to query by transport type.
     *
     * @param transportType The transport type
     * @return package name of the data service package for the specified transportType.
     */
    private String getDataServicePackageName(@TransportType int transportType) {
        String packageName;
        int resourceId;
        String carrierConfig;

        switch (transportType) {
            case AccessNetworkConstants.TRANSPORT_TYPE_WWAN:
                resourceId = com.android.internal.R.string.config_wwan_data_service_package;
                carrierConfig = CarrierConfigManager
                        .KEY_CARRIER_DATA_SERVICE_WWAN_PACKAGE_OVERRIDE_STRING;
                break;
            case AccessNetworkConstants.TRANSPORT_TYPE_WLAN:
                resourceId = com.android.internal.R.string.config_wlan_data_service_package;
                carrierConfig = CarrierConfigManager
                        .KEY_CARRIER_DATA_SERVICE_WLAN_PACKAGE_OVERRIDE_STRING;
                break;
            default:
                throw new IllegalStateException("Transport type not WWAN or WLAN. type="
                        + AccessNetworkConstants.transportTypeToString(mTransportType));
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

    /**
     * Get the data service class name for our current transport type.
     *
     * @return class name of the data service package for the current transportType.
     */
    private String getDataServiceClassName() {
        return getDataServiceClassName(mTransportType);
    }


    /**
     * Get the data service class by transport type.
     *
     * @param transportType either WWAN or WLAN
     * @return class name of the data service package for the specified transportType.
     */
    private String getDataServiceClassName(int transportType) {
        String className;
        int resourceId;
        String carrierConfig;
        switch (transportType) {
            case AccessNetworkConstants.TRANSPORT_TYPE_WWAN:
                resourceId = com.android.internal.R.string.config_wwan_data_service_class;
                carrierConfig = CarrierConfigManager
                        .KEY_CARRIER_DATA_SERVICE_WWAN_CLASS_OVERRIDE_STRING;
                break;
            case AccessNetworkConstants.TRANSPORT_TYPE_WLAN:
                resourceId = com.android.internal.R.string.config_wlan_data_service_class;
                carrierConfig = CarrierConfigManager
                        .KEY_CARRIER_DATA_SERVICE_WLAN_CLASS_OVERRIDE_STRING;
                break;
            default:
                throw new IllegalStateException("Transport type not WWAN or WLAN. type="
                        + transportType);
        }

        // Read package name from resource overlay
        className = mPhone.getContext().getResources().getString(resourceId);

        PersistableBundle b = mCarrierConfigManager.getConfigForSubId(mPhone.getSubId());

        if (b != null && !TextUtils.isEmpty(b.getString(carrierConfig))) {
            // If carrier config overrides it, use the one from carrier config
            className = b.getString(carrierConfig, className);
        }

        return className;
    }

    private void sendCompleteMessage(Message msg, @DataServiceCallback.ResultCode int code) {
        if (msg != null) {
            msg.arg1 = code;
            msg.sendToTarget();
        }
    }

    /**
     * Setup a data connection. The data service provider must implement this method to support
     * establishing a packet data connection. When completed or error, the service must invoke
     * the provided callback to notify the platform.
     *
     * @param accessNetworkType Access network type that the data network will be established on.
     * Must be one of {@link AccessNetworkConstants.AccessNetworkType}.
     * @param dataProfile Data profile used for data network setup. See {@link DataProfile}
     * @param isRoaming True if the device is data roaming.
     * @param allowRoaming True if data roaming is allowed by the user.
     * @param reason The reason for data setup. Must be {@link DataService#REQUEST_REASON_NORMAL} or
     * {@link DataService#REQUEST_REASON_HANDOVER}.
     * @param linkProperties If {@code reason} is {@link DataService#REQUEST_REASON_HANDOVER}, this
     * is the link properties of the existing data connection, otherwise null.
     * @param pduSessionId The pdu session id to be used for this data network.  A value of -1 means
     * no pdu session id was attached to this call. Reference: 3GPP TS 24.007 Section 11.2.3.1b.
     * @param sliceInfo The slice that represents S-NSSAI. Reference: 3GPP TS 24.501.
     * @param trafficDescriptor The traffic descriptor for this data network, used for URSP
     * matching. Reference: 3GPP TS TS 24.526 Section 5.2.
     * @param matchAllRuleAllowed True if using the default match-all URSP rule for this request is
     * allowed.
     * @param onCompleteMessage The result message for this request. Null if the client does not
     * care about the result.
     */
    public void setupDataCall(int accessNetworkType, DataProfile dataProfile, boolean isRoaming,
            boolean allowRoaming, int reason, LinkProperties linkProperties, int pduSessionId,
            @Nullable NetworkSliceInfo sliceInfo, @Nullable TrafficDescriptor trafficDescriptor,
            boolean matchAllRuleAllowed, Message onCompleteMessage) {
        if (DBG) log("setupDataCall");
        if (!mBound) {
            loge("setupDataCall: Data service not bound.");
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
            return;
        }

        DataServiceCallbackWrapper callback = new DataServiceCallbackWrapper("setupDataCall");
        if (onCompleteMessage != null) {
            mMessageMap.put(callback.asBinder(), onCompleteMessage);
        }
        try {
            sendMessageDelayed(obtainMessage(EVENT_WATCHDOG_TIMEOUT, callback),
                    REQUEST_UNRESPONDED_TIMEOUT);
            mIDataService.setupDataCall(mPhone.getPhoneId(), accessNetworkType, dataProfile,
                    isRoaming, allowRoaming, reason, linkProperties, pduSessionId, sliceInfo,
                    trafficDescriptor, matchAllRuleAllowed, callback);
        } catch (RemoteException e) {
            loge("setupDataCall: Cannot invoke setupDataCall on data service.");
            mMessageMap.remove(callback.asBinder());
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
        }
    }

    /**
     * Deactivate a data connection. The data service provider must implement this method to
     * support data connection tear down. When completed or error, the service must invoke the
     * provided callback to notify the platform.
     *
     * @param cid Call id returned in the callback of {@link #setupDataCall(int, DataProfile,
     * boolean, boolean, int, LinkProperties, int, NetworkSliceInfo, TrafficDescriptor, boolean,
     * Message)}
     * @param reason The reason for data deactivation. Must be
     * {@link DataService#REQUEST_REASON_NORMAL}, {@link DataService#REQUEST_REASON_SHUTDOWN}
     * or {@link DataService#REQUEST_REASON_HANDOVER}.
     * @param onCompleteMessage The result message for this request. Null if the client does not
     * care about the result.
     */
    public void deactivateDataCall(int cid, int reason, Message onCompleteMessage) {
        if (DBG) log("deactivateDataCall");
        if (!mBound) {
            loge("Data service not bound.");
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
            return;
        }

        DataServiceCallbackWrapper callback =
                new DataServiceCallbackWrapper("deactivateDataCall");
        if (onCompleteMessage != null) {
            mMessageMap.put(callback.asBinder(), onCompleteMessage);
        }
        try {
            sendMessageDelayed(obtainMessage(EVENT_WATCHDOG_TIMEOUT, callback),
                    REQUEST_UNRESPONDED_TIMEOUT);
            mIDataService.deactivateDataCall(mPhone.getPhoneId(), cid, reason, callback);
        } catch (RemoteException e) {
            loge("Cannot invoke deactivateDataCall on data service.");
            mMessageMap.remove(callback.asBinder());
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
        }
    }

    /**
     * Indicates that a handover has begun.  This is called on the source transport.
     *
     * Any resources being transferred cannot be released while a
     * handover is underway.
     *
     * If a handover was unsuccessful, then the framework calls DataServiceManager#cancelHandover.
     * The target transport retains ownership over any of the resources being transferred.
     *
     * If a handover was successful, the framework calls DataServiceManager#deactivateDataCall with
     * reason HANDOVER. The target transport now owns the transferred resources and is
     * responsible for releasing them.
     *
     * @param cid The identifier of the data network which is provided in DataCallResponse
     * @param onCompleteMessage The result callback for this request.
     */
    public void startHandover(int cid, @NonNull Message onCompleteMessage) {
        DataServiceCallbackWrapper callback =
                setupCallbackHelper("startHandover", onCompleteMessage);
        if (callback == null) {
            loge("startHandover: callback == null");
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
            return;
        }

        try {
            sendMessageDelayed(obtainMessage(EVENT_WATCHDOG_TIMEOUT, callback),
                    REQUEST_UNRESPONDED_TIMEOUT);
            mIDataService.startHandover(mPhone.getPhoneId(), cid, callback);
        } catch (RemoteException e) {
            loge("Cannot invoke startHandover on data service.");
            mMessageMap.remove(callback.asBinder());
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
        }
    }

    /**
     * Indicates that a handover was cancelled after a call to DataServiceManager#startHandover.
     * This is called on the source transport.
     *
     * Since the handover was unsuccessful, the source transport retains ownership over any of
     * the resources being transferred and is still responsible for releasing them.
     *
     * @param cid The identifier of the data network which is provided in DataCallResponse
     * @param onCompleteMessage The result callback for this request.
     */
    public void cancelHandover(int cid, @NonNull Message onCompleteMessage) {
        DataServiceCallbackWrapper callback =
                setupCallbackHelper("cancelHandover", onCompleteMessage);
        if (callback == null) {
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
            return;
        }

        try {
            sendMessageDelayed(obtainMessage(EVENT_WATCHDOG_TIMEOUT, callback),
                    REQUEST_UNRESPONDED_TIMEOUT);
            mIDataService.cancelHandover(mPhone.getPhoneId(), cid, callback);
        } catch (RemoteException e) {
            loge("Cannot invoke cancelHandover on data service.");
            mMessageMap.remove(callback.asBinder());
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
        }
    }

    @Nullable
    private DataServiceCallbackWrapper setupCallbackHelper(
            @NonNull final String operationName, @NonNull final Message onCompleteMessage) {
        if (DBG) log(operationName);
        if (!mBound) {
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
            return null;
        }

        DataServiceCallbackWrapper callback =
                new DataServiceCallbackWrapper(operationName);
        if (onCompleteMessage != null) {
            if (DBG) log(operationName + ": onCompleteMessage set");
            mMessageMap.put(callback.asBinder(), onCompleteMessage);
        } else {
            if (DBG) log(operationName + ": onCompleteMessage not set");
        }
        return callback;
    }

    /**
     * Set an APN to initial attach network.
     *
     * @param dataProfile Data profile used for data network setup. See {@link DataProfile}.
     * @param isRoaming True if the device is data roaming.
     * @param onCompleteMessage The result message for this request. Null if the client does not
     * care about the result.
     */
    public void setInitialAttachApn(DataProfile dataProfile, boolean isRoaming,
                                    Message onCompleteMessage) {
        if (DBG) log("setInitialAttachApn");
        if (!mBound) {
            loge("Data service not bound.");
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
            return;
        }

        DataServiceCallbackWrapper callback =
                new DataServiceCallbackWrapper("setInitialAttachApn");
        if (onCompleteMessage != null) {
            mMessageMap.put(callback.asBinder(), onCompleteMessage);
        }
        try {
            mIDataService.setInitialAttachApn(mPhone.getPhoneId(), dataProfile, isRoaming,
                    callback);
        } catch (RemoteException e) {
            loge("Cannot invoke setInitialAttachApn on data service.");
            mMessageMap.remove(callback.asBinder());
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
        }
    }

    /**
     * Send current carrier's data profiles to the data service for data network setup. This is
     * only for CDMA carrier that can change the profile through OTA. The data service should
     * always uses the latest data profile sent by the framework.
     *
     * @param dps A list of data profiles.
     * @param isRoaming True if the device is data roaming.
     * @param onCompleteMessage The result message for this request. Null if the client does not
     * care about the result.
     */
    public void setDataProfile(List<DataProfile> dps, boolean isRoaming,
            Message onCompleteMessage) {
        if (DBG) log("setDataProfile");
        if (!mBound) {
            loge("Data service not bound.");
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
            return;
        }

        DataServiceCallbackWrapper callback = new DataServiceCallbackWrapper("setDataProfile");
        if (onCompleteMessage != null) {
            mMessageMap.put(callback.asBinder(), onCompleteMessage);
        }
        try {
            mIDataService.setDataProfile(mPhone.getPhoneId(), dps, isRoaming, callback);
        } catch (RemoteException e) {
            loge("Cannot invoke setDataProfile on data service.");
            mMessageMap.remove(callback.asBinder());
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
        }
    }

    /**
     * Get the active data network list.
     *
     * @param onCompleteMessage The result message for this request. Null if the client does not
     * care about the result.
     */
    public void requestDataCallList(Message onCompleteMessage) {
        if (DBG) log("requestDataCallList");
        if (!mBound) {
            loge("Data service not bound.");
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
            return;
        }

        DataServiceCallbackWrapper callback =
                new DataServiceCallbackWrapper("requestDataCallList");
        if (onCompleteMessage != null) {
            mMessageMap.put(callback.asBinder(), onCompleteMessage);
        }
        try {
            mIDataService.requestDataCallList(mPhone.getPhoneId(), callback);
        } catch (RemoteException e) {
            loge("Cannot invoke requestDataCallList on data service.");
            if (callback != null) {
                mMessageMap.remove(callback.asBinder());
            }
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
        }
    }

    /**
     * Register for data network list changed event.
     *
     * @param h The target to post the event message to.
     * @param what The event.
     */
    public void registerForDataCallListChanged(Handler h, int what) {
        if (h != null) {
            mDataCallListChangedRegistrants.addUnique(h, what, mTransportType);
        }
    }

    /**
     * Unregister for data network list changed event.
     *
     * @param h The handler
     */
    public void unregisterForDataCallListChanged(Handler h) {
        if (h != null) {
            mDataCallListChangedRegistrants.remove(h);
        }
    }

    /**
     * Register apn unthrottled event
     *
     * @param h The target to post the event message to.
     * @param what The event.
     */
    public void registerForApnUnthrottled(Handler h, int what) {
        if (h != null) {
            mApnUnthrottledRegistrants.addUnique(h, what, mTransportType);
        }
    }

    /**
     * Unregister for apn unthrottled event
     *
     * @param h The handler
     */
    public void unregisterForApnUnthrottled(Handler h) {
        if (h != null) {
            mApnUnthrottledRegistrants.remove(h);
        }
    }

    /**
     * Register for data service binding status changed event.
     *
     * @param h The target to post the event message to.
     * @param what The event.
     */
    public void registerForServiceBindingChanged(Handler h, int what) {
        if (h != null) {
            mServiceBindingChangedRegistrants.remove(h);
            Registrant r = new Registrant(h, what, mTransportType);
            mServiceBindingChangedRegistrants.add(r);
            if (mBound) {
                r.notifyResult(true);
            }
        }
    }

    /**
     * Unregister for data service binding status changed event.
     *
     * @param h The handler
     */
    public void unregisterForServiceBindingChanged(Handler h) {
        if (h != null) {
            mServiceBindingChangedRegistrants.remove(h);
        }
    }

    private void log(String s) {
        Rlog.d(mTag, s);
    }

    private void loge(String s) {
        Rlog.e(mTag, s);
    }
}
