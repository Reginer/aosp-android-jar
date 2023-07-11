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
import android.annotation.StringDef;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
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
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.dataconnection.DataThrottler;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
    public static final String SYSTEM_PROPERTIES_IWLAN_OPERATION_MODE =
            "ro.telephony.iwlan_operation_mode";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef(prefix = {"IWLAN_OPERATION_MODE_"},
            value = {
                    IWLAN_OPERATION_MODE_DEFAULT,
                    IWLAN_OPERATION_MODE_LEGACY,
                    IWLAN_OPERATION_MODE_AP_ASSISTED})
    public @interface IwlanOperationMode {}

    /**
     * IWLAN default mode. On device that has IRadio 1.4 or above, it means
     * {@link #IWLAN_OPERATION_MODE_AP_ASSISTED}. On device that has IRadio 1.3 or below, it means
     * {@link #IWLAN_OPERATION_MODE_LEGACY}.
     */
    public static final String IWLAN_OPERATION_MODE_DEFAULT = "default";

    /**
     * IWLAN legacy mode. IWLAN is completely handled by the modem, and when the device is on
     * IWLAN, modem reports IWLAN as a RAT.
     */
    public static final String IWLAN_OPERATION_MODE_LEGACY = "legacy";

    /**
     * IWLAN application processor assisted mode. IWLAN is handled by the bound IWLAN data service
     * and network service separately.
     */
    public static final String IWLAN_OPERATION_MODE_AP_ASSISTED = "AP-assisted";

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
            ApnSetting.TYPE_XCAP
    };

    private final Phone mPhone;

    private final CarrierConfigManager mCarrierConfigManager;

    private IQualifiedNetworksService mIQualifiedNetworksService;

    private AccessNetworksManagerDeathRecipient mDeathRecipient;

    private String mTargetBindingPackageName;

    private QualifiedNetworksServiceConnection mServiceConnection;

    // Available networks. Key is the APN type.
    private final SparseArray<int[]> mAvailableNetworks = new SparseArray<>();

    private final @TransportType int[] mAvailableTransports;

    private final RegistrantList mQualifiedNetworksChangedRegistrants = new RegistrantList();

    private final Set<DataThrottler> mDataThrottlers = new HashSet<>();

    private final BroadcastReceiver mConfigChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED.equals(action)
                    && mPhone.getPhoneId() == intent.getIntExtra(
                    CarrierConfigManager.EXTRA_SLOT_INDEX, 0)) {
                // We should wait for carrier config changed event because the target binding
                // package name can come from the carrier config. Note that we still get this event
                // even when SIM is absent.
                if (DBG) log("Carrier config changed. Try to bind qualified network service.");
                bindQualifiedNetworksService();
            }
        }
    };

    /**
     * The current transport of the APN type. The key is the APN type, and the value is the
     * transport.
     */
    private final Map<Integer, Integer> mCurrentTransports = new ConcurrentHashMap<>();

    /**
     * The preferred transport of the APN type. The key is the APN type, and the value is the
     * transport. The preferred transports are updated as soon as QNS changes the preference, while
     * the current transports are updated after handover complete.
     */
    // TODO: Deprecate mPreferredTransports. Should expose mAvailableNetworks to
    //  DataNetworkController after we support multi preferred access networks (i.e.
    //  DataNetworkController might select 2nd preferred access network in some scenarios.)
    private final Map<Integer, Integer> mPreferredTransports = new ConcurrentHashMap<>();

    /**
     * Callbacks for passing information to interested clients.
     */
    private final @NonNull Set<AccessNetworksManagerCallback> mAccessNetworksManagerCallbacks =
            new ArraySet<>();

    /**
     * Registers the data throttler in order to receive APN status changes.
     *
     * @param dataThrottler the data throttler to register
     */
    public void registerDataThrottler(DataThrottler dataThrottler) {
        this.post(() -> {
            QualifiedNetworksServiceConnection serviceConnection = mServiceConnection;
            this.mDataThrottlers.add(dataThrottler);
            if (serviceConnection != null) {
                serviceConnection.registerDataThrottler(dataThrottler);
            }
        });
    }

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

    private class AccessNetworksManagerDeathRecipient implements IBinder.DeathRecipient {
        @Override
        public void binderDied() {
            // TODO: try to rebind the service.
            String message = "Qualified network service " + mLastBoundPackageName + " died.";
            loge(message);
            AnomalyReporter.reportAnomaly(mAnomalyUUID, message, mPhone.getCarrierId());
        }
    }

    private final class QualifiedNetworksServiceConnection implements ServiceConnection {

        /**
         * The APN throttle status callback is attached to the service connection so that they have
         * the same life cycle.
         */
        @NonNull
        private final ThrottleStatusChangedCallback mThrottleStatusCallback;

        QualifiedNetworksServiceConnection() {
            mThrottleStatusCallback = new ThrottleStatusChangedCallback();
        }

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

                registerDataThrottlersFirstTime();

            } catch (RemoteException e) {
                loge("Remote exception. " + e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (DBG) log("onServiceDisconnected " + name);
            unregisterForThrottleCallbacks();
            mTargetBindingPackageName = null;
        }

        /**
         * Runs on all of the data throttlers when the service is connected
         */
        private void registerDataThrottlersFirstTime() {
            post(() -> {
                for (DataThrottler dataThrottler : mDataThrottlers) {
                    dataThrottler.registerForThrottleStatusChanges(mThrottleStatusCallback);
                }
            });
        }

        private void registerDataThrottler(DataThrottler dataThrottler) {
            post(() -> {
                dataThrottler.registerForThrottleStatusChanges(mThrottleStatusCallback);
            });
        }

        private void unregisterForThrottleCallbacks() {
            post(() -> {
                for (DataThrottler dataThrottler : mDataThrottlers) {
                    dataThrottler.unregisterForThrottleStatusChanges(mThrottleStatusCallback);
                }
            });
        }
    }

    private class ThrottleStatusChangedCallback implements DataThrottler.Callback {
        @Override
        public void onThrottleStatusChanged(List<ThrottleStatus> throttleStatuses) {
            post(() -> {
                try {
                    List<ThrottleStatus> throttleStatusesBySlot =
                            throttleStatuses
                                    .stream()
                                    .filter(x -> x.getSlotIndex() == mPhone.getPhoneId())
                                    .collect(Collectors.toList());
                    if (mIQualifiedNetworksService != null) {
                        mIQualifiedNetworksService.reportThrottleStatusChanged(mPhone.getPhoneId(),
                                throttleStatusesBySlot);
                    }
                } catch (Exception ex) {
                    loge("onThrottleStatusChanged", ex);
                }
            });
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
                return;
            }

            List<QualifiedNetworks> qualifiedNetworksList = new ArrayList<>();
            for (int apnType : SUPPORTED_APN_TYPES) {
                if ((apnTypes & apnType) == apnType) {
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

            if (!qualifiedNetworksList.isEmpty()) {
                setPreferredTransports(qualifiedNetworksList);
                mQualifiedNetworksChangedRegistrants.notifyResult(qualifiedNetworksList);
            }
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

        if (isInLegacyMode()) {
            log("operates in legacy mode.");
            // For legacy mode, WWAN is the only transport to handle all data connections, even
            // the IWLAN ones.
            mAvailableTransports = new int[]{AccessNetworkConstants.TRANSPORT_TYPE_WWAN};
        } else {
            log("operates in AP-assisted mode.");
            mAvailableTransports = new int[]{AccessNetworkConstants.TRANSPORT_TYPE_WWAN,
                    AccessNetworkConstants.TRANSPORT_TYPE_WLAN};
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
            try {
                Context contextAsUser = phone.getContext().createPackageContextAsUser(
                        phone.getContext().getPackageName(), 0, UserHandle.ALL);
                contextAsUser.registerReceiver(mConfigChangedReceiver, intentFilter,
                        null /* broadcastPermission */, null);
            } catch (PackageManager.NameNotFoundException e) {
                loge("Package name not found: ", e);
            }
            bindQualifiedNetworksService();
        }

        if (phone.isUsingNewDataStack()) {
            // Using post to delay the registering because data retry manager instance is created
            // later than access networks manager.
            post(() -> mPhone.getDataNetworkController().getDataRetryManager().registerCallback(
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
                    }));
        }
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
     * @return package name of the qualified networks service package. Return empty string when in
     * legacy mode (i.e. Dedicated IWLAN data/network service is not supported).
     */
    private String getQualifiedNetworksServicePackageName() {
        // Read package name from the resource
        String packageName = mPhone.getContext().getResources().getString(
                com.android.internal.R.string.config_qualified_networks_service_package);

        PersistableBundle b = mCarrierConfigManager.getConfigForSubId(mPhone.getSubId());

        if (b != null) {
            // If carrier config overrides it, use the one from carrier config
            String carrierConfigPackageName =  b.getString(CarrierConfigManager
                    .KEY_CARRIER_QUALIFIED_NETWORKS_SERVICE_PACKAGE_OVERRIDE_STRING);
            if (!TextUtils.isEmpty(carrierConfigPackageName)) {
                if (DBG) log("Found carrier config override " + carrierConfigPackageName);
                packageName = carrierConfigPackageName;
            }
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

        PersistableBundle b = mCarrierConfigManager.getConfigForSubId(mPhone.getSubId());

        if (b != null) {
            // If carrier config overrides it, use the one from carrier config
            String carrierConfigClassName =  b.getString(CarrierConfigManager
                    .KEY_CARRIER_QUALIFIED_NETWORKS_SERVICE_CLASS_OVERRIDE_STRING);
            if (!TextUtils.isEmpty(carrierConfigClassName)) {
                if (DBG) log("Found carrier config override " + carrierConfigClassName);
                className = carrierConfigClassName;
            }
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
     * @return {@code true} if the device operates in legacy mode, otherwise {@code false}.
     */
    public boolean isInLegacyMode() {
        // Get IWLAN operation mode from the system property. If the system property is configured
        // to default or not configured, the mode is tied to IRadio version. For 1.4 or above, it's
        // AP-assisted mode, for 1.3 or below, it's legacy mode.
        String mode = SystemProperties.get(SYSTEM_PROPERTIES_IWLAN_OPERATION_MODE);

        if (mode.equals(IWLAN_OPERATION_MODE_AP_ASSISTED)) {
            return false;
        } else if (mode.equals(IWLAN_OPERATION_MODE_LEGACY)) {
            return true;
        }

        return mPhone.getHalVersion().less(RIL.RADIO_HAL_VERSION_1_4);
    }

    /**
     * @return The available transports. Note that on legacy devices, the only available transport
     * would be WWAN only. If the device is configured as AP-assisted mode, the available transport
     * will always be WWAN and WLAN (even if the device is not camped on IWLAN).
     * See {@link #isInLegacyMode()} for mode details.
     */
    public synchronized @NonNull int[] getAvailableTransports() {
        return mAvailableTransports;
    }

    /**
     * Get the transport based on the network capability.
     *
     * @param netCap The network capability.
     * @return The transport type.
     */
    public @TransportType int getCurrentTransportByNetworkCapability(@NetCapability int netCap) {
        return getCurrentTransport(DataUtils.networkCapabilityToApnType(netCap));
    }

    /**
     * Get the transport based on the APN type.
     *
     * @param apnType APN type
     * @return The transport type
     */
    // TODO: Remove this after TransportManager is removed.
    public @TransportType int getCurrentTransport(@ApnType int apnType) {
        // In legacy mode, always route to cellular.
        if (isInLegacyMode()) {
            return AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
        }

        // If we can't find the corresponding transport, always route to cellular.
        return mCurrentTransports.get(apnType) == null
                ? AccessNetworkConstants.TRANSPORT_TYPE_WWAN : mCurrentTransports.get(apnType);
    }

    /**
     * Set the current transport of a network capability.
     *
     * @param netCap The network capability.
     * @param transport The transport.
     */
    public void setCurrentTransportByNetworkCapability(@NetCapability int netCap,
            @TransportType int transport) {
        setCurrentTransport(DataUtils.networkCapabilityToApnType(netCap), transport);
    }

    /**
     * Set the current transport of apn type.
     *
     * @param apnType The APN type
     * @param transport The transport.
     */
    // TODO: Remove this after TransportManager is removed.
    public void setCurrentTransport(@ApnType int apnType, @TransportType int transport) {
        Integer previousTransport = mCurrentTransports.put(apnType, transport);
        if (previousTransport == null || previousTransport != transport) {
            logl("setCurrentTransport: apnType=" + ApnSetting.getApnTypeString(apnType)
                    + ", transport=" + AccessNetworkConstants.transportTypeToString(transport));
        }
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
        // In legacy mode, always preferred on cellular.
        if (isInLegacyMode()) {
            return AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
        }

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
     * Check if there is any APN type's current transport is on IWLAN.
     *
     * @return {@code true} if there is any APN is on IWLAN, otherwise {@code false}.
     */
    public boolean isAnyApnOnIwlan() {
        for (int apnType : AccessNetworksManager.SUPPORTED_APN_TYPES) {
            if (mPhone.isUsingNewDataStack()) {
                if (getPreferredTransport(apnType) == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
                    return true;
                }
            } else {
                if (getCurrentTransport(apnType) == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
                    return true;
                }
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
        pw.println("current transports=");
        pw.increaseIndent();
        for (int apnType : AccessNetworksManager.SUPPORTED_APN_TYPES) {
            pw.println(ApnSetting.getApnTypeString(apnType)
                    + ": " + AccessNetworkConstants.transportTypeToString(
                    getCurrentTransport(apnType)));
        }
        pw.decreaseIndent();
        pw.println("preferred transports=");
        pw.increaseIndent();
        for (int apnType : AccessNetworksManager.SUPPORTED_APN_TYPES) {
            pw.println(ApnSetting.getApnTypeString(apnType)
                    + ": " + AccessNetworkConstants.transportTypeToString(
                    getPreferredTransport(apnType)));
        }

        pw.decreaseIndent();
        pw.println("isInLegacy=" + isInLegacyMode());
        pw.println("IWLAN operation mode="
                + SystemProperties.get(SYSTEM_PROPERTIES_IWLAN_OPERATION_MODE));
        pw.println("Local logs=");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.decreaseIndent();
        pw.flush();
    }
}
