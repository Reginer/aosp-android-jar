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

package com.android.internal.telephony.dataconnection;

import android.annotation.NonNull;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.Annotation.ApnType;
import android.telephony.AnomalyReporter;
import android.telephony.CarrierConfigManager;
import android.telephony.data.ApnSetting;
import android.telephony.data.IQualifiedNetworksService;
import android.telephony.data.IQualifiedNetworksServiceCallback;
import android.telephony.data.QualifiedNetworksService;
import android.telephony.data.ThrottleStatus;
import android.text.TextUtils;
import android.util.IndentingPrintWriter;
import android.util.SparseArray;

import com.android.internal.telephony.Phone;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Access network manager manages the qualified/available networks for mobile data connection.
 * It binds to the vendor's qualified networks service and actively monitors the qualified
 * networks changes.
 */
public class AccessNetworksManager extends Handler {
    private final String mLogTag;
    private static final boolean DBG = false;
    private final UUID mAnomalyUUID = UUID.fromString("c2d1a639-00e2-4561-9619-6acf37d90590");
    private String mLastBoundPackageName;

    private static final int[] SUPPORTED_APN_TYPES = {
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
        public final int[] qualifiedNetworks;
        public QualifiedNetworks(@ApnType int apnType, int[] qualifiedNetworks) {
            this.apnType = apnType;
            this.qualifiedNetworks = qualifiedNetworks;
        }

        @Override
        public String toString() {
            List<String> accessNetworkStrings = new ArrayList<>();
            for (int network : qualifiedNetworks) {
                accessNetworkStrings.add(AccessNetworkType.toString(network));
            }
            return "[QualifiedNetworks: apnType="
                    + ApnSetting.getApnTypeString(apnType)
                    + ", networks="
                    + Arrays.stream(qualifiedNetworks)
                    .mapToObj(type -> AccessNetworkType.toString(type))
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
            AnomalyReporter.reportAnomaly(mAnomalyUUID, message);
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

                    mIQualifiedNetworksService.reportThrottleStatusChanged(mPhone.getPhoneId(),
                            throttleStatusesBySlot);
                } catch (Exception ex) {
                    loge("onThrottleStatusChanged", ex);
                }
            });
        }
    }

    private final class QualifiedNetworksServiceCallback extends
            IQualifiedNetworksServiceCallback.Stub {
        @Override
        public void onQualifiedNetworkTypesChanged(int apnTypes, int[] qualifiedNetworkTypes) {
            log("onQualifiedNetworkTypesChanged. apnTypes = ["
                    + ApnSetting.getApnTypesStringFromBitmask(apnTypes)
                    + "], networks = [" + Arrays.stream(qualifiedNetworkTypes)
                    .mapToObj(i -> AccessNetworkType.toString(i)).collect(Collectors.joining(","))
                    + "]");
            List<QualifiedNetworks> qualifiedNetworksList = new ArrayList<>();
            for (int supportedApnType : SUPPORTED_APN_TYPES) {
                if ((apnTypes & supportedApnType) == supportedApnType) {
                    // TODO: Verify the preference from data settings manager to make sure the order
                    // of the networks do not violate users/carrier's preference.
                    if (mAvailableNetworks.get(supportedApnType) != null) {
                        if (Arrays.equals(mAvailableNetworks.get(supportedApnType),
                                qualifiedNetworkTypes)) {
                            log("Available networks for "
                                    + ApnSetting.getApnTypesStringFromBitmask(supportedApnType)
                                    + " not changed.");
                            continue;
                        }
                    }
                    mAvailableNetworks.put(supportedApnType, qualifiedNetworkTypes);
                    qualifiedNetworksList.add(new QualifiedNetworks(supportedApnType,
                            qualifiedNetworkTypes));
                }
            }

            if (!qualifiedNetworksList.isEmpty()) {
                mQualifiedNetworksChangedRegistrants.notifyResult(qualifiedNetworksList);
            }
        }
    }

    /**
     * Constructor
     *
     * @param phone The phone object
     */
    public AccessNetworksManager(Phone phone) {
        mPhone = phone;
        mCarrierConfigManager = (CarrierConfigManager) phone.getContext().getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        mLogTag = "ANM-" + mPhone.getPhoneId();

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
     * Dump the state of transport manager
     *
     * @param fd File descriptor
     * @param pw Print writer
     * @param args Arguments
     */
    public void dump(FileDescriptor fd, IndentingPrintWriter pw, String[] args) {
        pw.println("AccessNetworksManager:");
        pw.increaseIndent();
        pw.println("Available networks:");
        pw.increaseIndent();

        for (int i = 0; i < mAvailableNetworks.size(); i++) {
            pw.println("APN type "
                    + ApnSetting.getApnTypeString(mAvailableNetworks.keyAt(i))
                    + ": [" + Arrays.stream(mAvailableNetworks.valueAt(i))
                    .mapToObj(AccessNetworkType::toString)
                    .collect(Collectors.joining(",")) + "]");
        }
        pw.decreaseIndent();
        pw.decreaseIndent();
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

}
