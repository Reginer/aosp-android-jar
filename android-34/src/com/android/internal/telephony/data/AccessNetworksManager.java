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

package com.android.internal.telephony.data;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.AccessNetworkConstants.RadioAccessNetworkType;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.Annotation.ApnType;
import android.telephony.Annotation.NetCapability;
import android.telephony.AnomalyReporter;
import android.telephony.CarrierConfigManager;
import android.telephony.data.ApnSetting;
import android.telephony.data.IQualifiedNetworksService;
import android.telephony.data.IQualifiedNetworksServiceCallback;
import android.telephony.data.QualifiedNetworksService;
import android.telephony.data.ThrottleStatus;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.util.SparseArray;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SlidingWindowEventCounter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Access network manager manages the qualified/available networks for mobile data connection.
 * It binds to the vendor's qualified networks service and actively monitors the qualified
 * networks changes.
 */
public class AccessNetworksManager extends Handler {
    private static final boolean DBG = false;

    /** Event to guide a transport type for initial data connection of emergency data network. */
    private static final int EVENT_GUIDE_TRANSPORT_TYPE_FOR_EMERGENCY = 1;

    /**
     * The counters to detect frequent QNS attempt to change preferred network transport by ApnType.
     */
    private final @NonNull SparseArray<SlidingWindowEventCounter> mApnTypeToQnsChangeNetworkCounter;

    private final String mLogTag;
    private final LocalLog mLocalLog = new LocalLog(64);
    private final UUID mAnomalyUUID = UUID.fromString("c2d1a639-00e2-4561-9619-6acf37d90590");
    private String mLastBoundPackageName;

    public static final int[] SUPPORTED_APN_TYPES = {
            ApnSetting.TYPE_DEFAULT,
            ApnSetting.TYPE_MMS,
            ApnSetting.TYPE_FOTA,
            ApnSetting.TYPE_IMS,
            ApnSetting.TYPE_CBS,
            ApnSetting.TYPE_SUPL,
            ApnSetting.TYPE_EMERGENCY,
            ApnSetting.TYPE_XCAP,
            ApnSetting.TYPE_DUN
    };

    private final Phone mPhone;

    private final CarrierConfigManager mCarrierConfigManager;

    private @Nullable DataConfigManager mDataConfigManager;

    private IQualifiedNetworksService mIQualifiedNetworksService;

    private AccessNetworksManagerDeathRecipient mDeathRecipient;

    private String mTargetBindingPackageName;

    private QualifiedNetworksServiceConnection mServiceConnection;

    // Available networks. Key is the APN type.
    private final SparseArray<int[]> mAvailableNetworks = new SparseArray<>();

    private final @TransportType int[] mAvailableTransports;

    private final RegistrantList mQualifiedNetworksChangedRegistrants = new RegistrantList();

    /**
     * The preferred transport of the APN type. The key is the APN type, and the value is the
     * transport. The preferred transports are updated as soon as QNS changes the preference.
     */
    private final Map<Integer, Integer> mPreferredTransports = new ConcurrentHashMap<>();

    /**
     * Callbacks for passing information to interested clients.
     */
    private final @NonNull Set<AccessNetworksManagerCallback> mAccessNetworksManagerCallbacks =
            new ArraySet<>();

    /**
     * Represents qualified network types list on a specific APN type.
     */
    public static class QualifiedNetworks {
        public final @ApnType int apnType;
        // The qualified networks in preferred order. Each network is a AccessNetworkType.
        public final @NonNull @RadioAccessNetworkType int[] qualifiedNetworks;
        public QualifiedNetworks(@ApnType int apnType, @NonNull int[] qualifiedNetworks) {
            this.apnType = apnType;
            this.qualifiedNetworks = Arrays.stream(qualifiedNetworks)
                    .boxed()
                    .filter(DataUtils::isValidAccessNetwork)
                    .mapToInt(Integer::intValue)
                    .toArray();
        }

        @Override
        public String toString() {
            return "[QualifiedNetworks: apnType="
                    + ApnSetting.getApnTypeString(apnType)
                    + ", networks="
                    + Arrays.stream(qualifiedNetworks)
                    .mapToObj(AccessNetworkType::toString)
                    .collect(Collectors.joining(","))
                    + "]";
        }
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case EVENT_GUIDE_TRANSPORT_TYPE_FOR_EMERGENCY:
                AsyncResult ar = (AsyncResult) msg.obj;
                int transport = (int) ar.result;
                onEmergencyDataNetworkPreferredTransportChanged(transport);
                break;
            default:
                loge("Unexpected event " + msg.what);
        }
    }

    private class AccessNetworksManagerDeathRecipient implements IBinder.DeathRecipient {
        @Override
        public void binderDied() {
            // TODO: try to rebind the service.
            String message = "Qualified network service " + mLastBoundPackageName + " died.";
            // clear the anomaly report counters when QNS crash
            mApnTypeToQnsChangeNetworkCounter.clear();
            loge(message);
            AnomalyReporter.reportAnomaly(mAnomalyUUID, message, mPhone.getCarrierId());
        }
    }

    private final class QualifiedNetworksServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (DBG) log("onServiceConnected " + name);
            mIQualifiedNetworksService = IQualifiedNetworksService.Stub.asInterface(service);
            mDeathRecipient = new AccessNetworksManagerDeathRecipient();
            mLastBoundPackageName = getQualifiedNetworksServicePackageName();

            try {
                service.linkToDeath(mDeathRecipient, 0 /* flags */);
                mIQualifiedNetworksService.createNetworkAvailabilityProvider(mPhone.getPhoneId(),
                        new QualifiedNetworksServiceCallback());
            } catch (RemoteException e) {
                loge("Remote exception. " + e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (DBG) log("onServiceDisconnected " + name);
            mTargetBindingPackageName = null;
        }

    }

    private final class QualifiedNetworksServiceCallback extends
            IQualifiedNetworksServiceCallback.Stub {
        @Override
        public void onQualifiedNetworkTypesChanged(int apnTypes,
                @NonNull int[] qualifiedNetworkTypes) {
            if (qualifiedNetworkTypes == null) {
                loge("onQualifiedNetworkTypesChanged: Ignored null input.");
                return;
            }

            log("onQualifiedNetworkTypesChanged: apnTypes = ["
                    + ApnSetting.getApnTypesStringFromBitmask(apnTypes)
                    + "], networks = [" + Arrays.stream(qualifiedNetworkTypes)
                    .mapToObj(AccessNetworkType::toString).collect(Collectors.joining(","))
                    + "]");

            if (Arrays.stream(qualifiedNetworkTypes).anyMatch(accessNetwork
                    -> !DataUtils.isValidAccessNetwork(accessNetwork))) {
                loge("Invalid access networks " + Arrays.toString(qualifiedNetworkTypes));
                if (mDataConfigManager != null
                        && mDataConfigManager.isInvalidQnsParamAnomalyReportEnabled()) {
                    reportAnomaly("QNS requested invalid Network Type",
                            "3e89a3df-3524-45fa-b5f2-0fb0e4c77ec4");
                }
                return;
            }

            List<QualifiedNetworks> qualifiedNetworksList = new ArrayList<>();
            int satisfiedApnTypes = 0;
            for (int apnType : SUPPORTED_APN_TYPES) {
                if ((apnTypes & apnType) == apnType) {
                    // skip the APN anomaly detection if not using the T data stack
                    if (mDataConfigManager != null) {
                        satisfiedApnTypes |= apnType;
                    }

                    if (mAvailableNetworks.get(apnType) != null) {
                        if (Arrays.equals(mAvailableNetworks.get(apnType),
                                qualifiedNetworkTypes)) {
                            log("Available networks for "
                                    + ApnSetting.getApnTypesStringFromBitmask(apnType)
                                    + " not changed.");
                            continue;
                        }
                    }

                    // Empty array indicates QNS did not suggest any qualified networks. In this
                    // case all network requests will be routed to cellular.
                    if (qualifiedNetworkTypes.length == 0) {
                        mAvailableNetworks.remove(apnType);
                        if (getPreferredTransport(apnType)
                                == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
                            mPreferredTransports.put(apnType,
                                    AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
                            mAccessNetworksManagerCallbacks.forEach(callback ->
                                    callback.invokeFromExecutor(() ->
                                            callback.onPreferredTransportChanged(DataUtils
                                                    .apnTypeToNetworkCapability(apnType))));
                        }
                    } else {
                        mAvailableNetworks.put(apnType, qualifiedNetworkTypes);
                        qualifiedNetworksList.add(new QualifiedNetworks(apnType,
                                qualifiedNetworkTypes));

                    }
                }
            }

            // Report anomaly if any requested APN types are unsatisfied
            if (satisfiedApnTypes != apnTypes
                    && mDataConfigManager != null
                    && mDataConfigManager.isInvalidQnsParamAnomalyReportEnabled()) {
                int unsatisfied = satisfiedApnTypes ^ apnTypes;
                reportAnomaly("QNS requested unsupported APN Types:"
                        + Integer.toBinaryString(unsatisfied),
                        "3e89a3df-3524-45fa-b5f2-0fb0e4c77ec5");
            }

            if (!qualifiedNetworksList.isEmpty()) {
                setPreferredTransports(qualifiedNetworksList);
                mQualifiedNetworksChangedRegistrants.notifyResult(qualifiedNetworksList);
            }
        }
    }

    private void onEmergencyDataNetworkPreferredTransportChanged(
            @AccessNetworkConstants.TransportType int transportType) {
        try {
            logl("onEmergencyDataNetworkPreferredTransportChanged: "
                    + AccessNetworkConstants.transportTypeToString(transportType));
            if (mIQualifiedNetworksService != null) {
                mIQualifiedNetworksService.reportEmergencyDataNetworkPreferredTransportChanged(
                        mPhone.getPhoneId(), transportType);
            }
        } catch (Exception ex) {
            loge("onEmergencyDataNetworkPreferredTransportChanged: ", ex);
        }
    }

    /**
     * Access networks manager callback. This should be only used by {@link DataNetworkController}.
     */
    public abstract static class AccessNetworksManagerCallback extends DataCallback {
        /**
         * Constructor
         *
         * @param executor The executor of the callback.
         */
        public AccessNetworksManagerCallback(@NonNull @CallbackExecutor Executor executor) {
            super(executor);
        }

        /**
         * Called when preferred transport changed.
         *
         * @param networkCapability The network capability.
         */
        public abstract void onPreferredTransportChanged(@NetCapability int networkCapability);
    }

    /**
     * Constructor
     *
     * @param phone The phone object.
     * @param looper Looper for the handler.
     */
    public AccessNetworksManager(@NonNull Phone phone, @NonNull Looper looper) {
        super(looper);
        mPhone = phone;
        mCarrierConfigManager = (CarrierConfigManager) phone.getContext().getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        mLogTag = "ANM-" + mPhone.getPhoneId();
        mApnTypeToQnsChangeNetworkCounter = new SparseArray<>();
        mAvailableTransports = new int[]{AccessNetworkConstants.TRANSPORT_TYPE_WWAN,
                AccessNetworkConstants.TRANSPORT_TYPE_WLAN};

        // bindQualifiedNetworksService posts real work to handler thread. So here we can
        // let the callback execute in binder thread to avoid post twice.
        mCarrierConfigManager.registerCarrierConfigChangeListener(Runnable::run,
                (slotIndex, subId, carrierId, specificCarrierId) -> {
                    if (slotIndex != mPhone.getPhoneId()) return;
                    // We should wait for carrier config changed event because the target binding
                    // package name can come from the carrier config. Note that we still get this
                    // event even when SIM is absent.
                    if (DBG) log("Carrier config changed. Try to bind qualified network service.");
                    bindQualifiedNetworksService();
                });
        bindQualifiedNetworksService();

        // Using post to delay the registering because data retry manager and data config
        // manager instances are created later than access networks manager.
        post(() -> {
            mPhone.getDataNetworkController().getDataRetryManager().registerCallback(
                    new DataRetryManager.DataRetryManagerCallback(this::post) {
                        @Override
                        public void onThrottleStatusChanged(List<ThrottleStatus> throttleStatuses) {
                            try {
                                logl("onThrottleStatusChanged: " + throttleStatuses);
                                if (mIQualifiedNetworksService != null) {
                                    mIQualifiedNetworksService.reportThrottleStatusChanged(
                                            mPhone.getPhoneId(), throttleStatuses);
                                }
                            } catch (Exception ex) {
                                loge("onThrottleStatusChanged: ", ex);
                            }
                        }
                    });
            mDataConfigManager = mPhone.getDataNetworkController().getDataConfigManager();
            mDataConfigManager.registerCallback(
                    new DataConfigManager.DataConfigManagerCallback(this::post) {
                        @Override
                        public void onDeviceConfigChanged() {
                            mApnTypeToQnsChangeNetworkCounter.clear();
                        }
                    });
            mPhone.registerForEmergencyDomainSelected(
                    this, EVENT_GUIDE_TRANSPORT_TYPE_FOR_EMERGENCY, null);
        });
    }

    /**
     * Trigger the anomaly report with the specified UUID.
     *
     * @param anomalyMsg Description of the event
     * @param uuid UUID associated with that event
     */
    private void reportAnomaly(@NonNull String anomalyMsg, @NonNull String uuid) {
        logl(anomalyMsg);
        AnomalyReporter.reportAnomaly(UUID.fromString(uuid), anomalyMsg, mPhone.getCarrierId());
    }

    /**
     * Find the qualified network service from configuration and binds to it. It reads the
     * configuration from carrier config if it exists. If not, read it from resources.
     */
    private void bindQualifiedNetworksService() {
        post(() -> {
            Intent intent = null;
            String packageName = getQualifiedNetworksServicePackageName();
            String className = getQualifiedNetworksServiceClassName();

            if (DBG) log("Qualified network service package = " + packageName);
            if (TextUtils.isEmpty(packageName)) {
                loge("Can't find the binding package");
                return;
            }

            if (TextUtils.isEmpty(className)) {
                intent = new Intent(QualifiedNetworksService.QUALIFIED_NETWORKS_SERVICE_INTERFACE);
                intent.setPackage(packageName);
            } else {
                ComponentName cm = new ComponentName(packageName, className);
                intent = new Intent(QualifiedNetworksService.QUALIFIED_NETWORKS_SERVICE_INTERFACE)
                        .setComponent(cm);
            }

            if (TextUtils.equals(packageName, mTargetBindingPackageName)) {
                if (DBG) log("Service " + packageName + " already bound or being bound.");
                return;
            }

            if (mIQualifiedNetworksService != null
                    && mIQualifiedNetworksService.asBinder().isBinderAlive()) {
                // Remove the network availability updater and then unbind the service.
                try {
                    mIQualifiedNetworksService.removeNetworkAvailabilityProvider(
                            mPhone.getPhoneId());
                } catch (RemoteException e) {
                    loge("Cannot remove network availability updater. " + e);
                }

                mPhone.getContext().unbindService(mServiceConnection);
            }

            try {
                mServiceConnection = new QualifiedNetworksServiceConnection();
                log("bind to " + packageName);
                if (!mPhone.getContext().bindService(intent, mServiceConnection,
                        Context.BIND_AUTO_CREATE)) {
                    loge("Cannot bind to the qualified networks service.");
                    return;
                }
                mTargetBindingPackageName = packageName;
            } catch (Exception e) {
                loge("Cannot bind to the qualified networks service. Exception: " + e);
            }
        });
    }

    /**
     * Get the qualified network service package.
     *
     * @return package name of the qualified networks service package.
     */
    private String getQualifiedNetworksServicePackageName() {
        // Read package name from the resource
        String packageName = mPhone.getContext().getResources().getString(
                com.android.internal.R.string.config_qualified_networks_service_package);

        PersistableBundle b;
        try {
            b = mCarrierConfigManager.getConfigForSubId(mPhone.getSubId(),
                    CarrierConfigManager
                            .KEY_CARRIER_QUALIFIED_NETWORKS_SERVICE_PACKAGE_OVERRIDE_STRING);
            if (b != null && !b.isEmpty()) {
                // If carrier config overrides it, use the one from carrier config
                String carrierConfigPackageName = b.getString(CarrierConfigManager
                        .KEY_CARRIER_QUALIFIED_NETWORKS_SERVICE_PACKAGE_OVERRIDE_STRING);
                if (!TextUtils.isEmpty(carrierConfigPackageName)) {
                    if (DBG) log("Found carrier config override " + carrierConfigPackageName);
                    packageName = carrierConfigPackageName;
                }
            }
        } catch (RuntimeException e) {
            loge("Carrier config loader is not available.");
        }

        return packageName;
    }

    /**
     * Get the qualified network service class name.
     *
     * @return class name of the qualified networks service package.
     */
    private String getQualifiedNetworksServiceClassName() {
        // Read package name from the resource
        String className = mPhone.getContext().getResources().getString(
                com.android.internal.R.string.config_qualified_networks_service_class);

        PersistableBundle b;
        try {
            b = mCarrierConfigManager.getConfigForSubId(mPhone.getSubId(),
                    CarrierConfigManager
                            .KEY_CARRIER_QUALIFIED_NETWORKS_SERVICE_CLASS_OVERRIDE_STRING);
            if (b != null && !b.isEmpty()) {
                // If carrier config overrides it, use the one from carrier config
                String carrierConfigClassName = b.getString(CarrierConfigManager
                        .KEY_CARRIER_QUALIFIED_NETWORKS_SERVICE_CLASS_OVERRIDE_STRING);
                if (!TextUtils.isEmpty(carrierConfigClassName)) {
                    if (DBG) log("Found carrier config override " + carrierConfigClassName);
                    className = carrierConfigClassName;
                }
            }
        } catch (RuntimeException e) {
            loge("Carrier config loader is not available.");
        }

        return className;
    }

    private @NonNull List<QualifiedNetworks> getQualifiedNetworksList() {
        List<QualifiedNetworks> qualifiedNetworksList = new ArrayList<>();
        for (int i = 0; i < mAvailableNetworks.size(); i++) {
            qualifiedNetworksList.add(new QualifiedNetworks(mAvailableNetworks.keyAt(i),
                    mAvailableNetworks.valueAt(i)));
        }

        return qualifiedNetworksList;
    }

    /**
     * Register for qualified networks changed event.
     *
     * @param h The target to post the event message to.
     * @param what The event.
     */
    public void registerForQualifiedNetworksChanged(Handler h, int what) {
        if (h != null) {
            Registrant r = new Registrant(h, what, null);
            mQualifiedNetworksChangedRegistrants.add(r);

            // Notify for the first time if there is already something in the available network
            // list.
            if (mAvailableNetworks.size() != 0) {
                r.notifyResult(getQualifiedNetworksList());
            }
        }
    }

    /**
     * @return The available transports.
     */
    public @NonNull int[] getAvailableTransports() {
        return mAvailableTransports;
    }

    private static @TransportType int getTransportFromAccessNetwork(int accessNetwork) {
        return accessNetwork == AccessNetworkType.IWLAN
                ? AccessNetworkConstants.TRANSPORT_TYPE_WLAN
                : AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
    }

    private void setPreferredTransports(@NonNull List<QualifiedNetworks> networksList) {
        for (QualifiedNetworks networks : networksList) {
            if (networks.qualifiedNetworks.length > 0) {
                int transport = getTransportFromAccessNetwork(networks.qualifiedNetworks[0]);
                if (getPreferredTransport(networks.apnType) != transport) {
                    mPreferredTransports.put(networks.apnType, transport);
                    mAccessNetworksManagerCallbacks.forEach(callback ->
                            callback.invokeFromExecutor(() ->
                                    callback.onPreferredTransportChanged(DataUtils
                                            .apnTypeToNetworkCapability(networks.apnType))));
                    logl("setPreferredTransports: apnType="
                            + ApnSetting.getApnTypeString(networks.apnType) + ", transport="
                            + AccessNetworkConstants.transportTypeToString(transport));
                }
            }
        }
    }

    /**
     * Get the  preferred transport.
     *
     * @param apnType APN type
     * @return The preferred transport.
     */
    public @TransportType int getPreferredTransport(@ApnType int apnType) {
        return mPreferredTransports.get(apnType) == null
                ? AccessNetworkConstants.TRANSPORT_TYPE_WWAN : mPreferredTransports.get(apnType);
    }

    /**
     * Get the  preferred transport by network capability.
     *
     * @param networkCapability The network capability. (Note that only APN-type capabilities are
     * supported.
     * @return The preferred transport.
     */
    public @TransportType int getPreferredTransportByNetworkCapability(
            @NetCapability int networkCapability) {
        int apnType = DataUtils.networkCapabilityToApnType(networkCapability);
        // For non-APN type capabilities, always route to WWAN.
        if (apnType == ApnSetting.TYPE_NONE) {
            return AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
        }
        return getPreferredTransport(apnType);
    }

    /**
     * Check if there is any APN type preferred on IWLAN.
     *
     * @return {@code true} if there is any APN is on IWLAN, otherwise {@code false}.
     */
    public boolean isAnyApnOnIwlan() {
        for (int apnType : AccessNetworksManager.SUPPORTED_APN_TYPES) {
            if (getPreferredTransport(apnType) == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unregister for qualified networks changed event.
     *
     * @param h The handler
     */
    public void unregisterForQualifiedNetworksChanged(Handler h) {
        if (h != null) {
            mQualifiedNetworksChangedRegistrants.remove(h);
        }
    }

    /**
     * Register the callback for receiving information from {@link AccessNetworksManager}.
     *
     * @param callback The callback.
     */
    public void registerCallback(@NonNull AccessNetworksManagerCallback callback) {
        mAccessNetworksManagerCallbacks.add(callback);
    }

    /**
     * Unregister the callback which was previously registered through
     * {@link #registerCallback(AccessNetworksManagerCallback)}.
     *
     * @param callback The callback to unregister.
     */
    public void unregisterCallback(@NonNull AccessNetworksManagerCallback callback) {
        mAccessNetworksManagerCallbacks.remove(callback);
    }

    private void log(String s) {
        Rlog.d(mLogTag, s);
    }

    private void loge(String s) {
        Rlog.e(mLogTag, s);
    }

    private void loge(String s, Exception ex) {
        Rlog.e(mLogTag, s, ex);
    }

    private void logl(String s) {
        log(s);
        mLocalLog.log(s);
    }

    /**
     * Dump the state of access networks manager
     *
     * @param fd File descriptor
     * @param printWriter Print writer
     * @param args Arguments
     */
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println(AccessNetworksManager.class.getSimpleName() + "-" + mPhone.getPhoneId() + ":");
        pw.increaseIndent();
        pw.println("preferred transports=");
        pw.increaseIndent();
        for (int apnType : AccessNetworksManager.SUPPORTED_APN_TYPES) {
            pw.println(ApnSetting.getApnTypeString(apnType)
                    + ": " + AccessNetworkConstants.transportTypeToString(
                    getPreferredTransport(apnType)));
        }

        pw.decreaseIndent();
        pw.println("Local logs=");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
        pw.flush();
    }
}
