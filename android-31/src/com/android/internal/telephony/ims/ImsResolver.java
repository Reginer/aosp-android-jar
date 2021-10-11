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

package com.android.internal.telephony.ims;

import android.Manifest;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsService;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsFeatureConfiguration;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.LocalLog;
import android.util.Log;
import android.util.SparseArray;

import com.android.ims.ImsFeatureBinderRepository;
import com.android.ims.ImsFeatureContainer;
import com.android.ims.internal.IImsServiceFeatureCallback;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.PhoneConfigurationManager;
import com.android.internal.util.IndentingPrintWriter;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Creates a list of ImsServices that are available to bind to based on the Device configuration
 * overlay values "config_ims_rcs_package" and "config_ims_mmtel_package" as well as Carrier
 * Configuration value "config_ims_rcs_package_override_string" and
 * "config_ims_mmtel_package_override_string".
 * These ImsServices are then bound to in the following order for each mmtel and rcs feature:
 *
 * 1. Carrier Config defined override value per SIM.
 * 2. Device overlay default value (including no SIM case).
 *
 * ImsManager can then retrieve the binding to the correct ImsService using
 * {@link #listenForFeature} on a per-slot and per feature basis.
 */

public class ImsResolver implements ImsServiceController.ImsServiceControllerCallbacks {

    private static final String TAG = "ImsResolver";
    private static final int GET_IMS_SERVICE_TIMEOUT_MS = 5000;

    @VisibleForTesting
    public static final String METADATA_EMERGENCY_MMTEL_FEATURE =
            "android.telephony.ims.EMERGENCY_MMTEL_FEATURE";
    @VisibleForTesting
    public static final String METADATA_MMTEL_FEATURE = "android.telephony.ims.MMTEL_FEATURE";
    @VisibleForTesting
    public static final String METADATA_RCS_FEATURE = "android.telephony.ims.RCS_FEATURE";
    // Overrides the correctness permission check of android.permission.BIND_IMS_SERVICE for any
    // ImsService that is connecting to the platform.
    // This should ONLY be used for testing and should not be used in production ImsServices.
    private static final String METADATA_OVERRIDE_PERM_CHECK = "override_bind_check";

    // Based on updates from PackageManager
    private static final int HANDLER_ADD_PACKAGE = 0;
    // Based on updates from PackageManager
    private static final int HANDLER_REMOVE_PACKAGE = 1;
    // Based on updates from CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED
    private static final int HANDLER_CONFIG_CHANGED = 2;
    // A query has been started for an ImsService to relay the features they support.
    private static final int HANDLER_START_DYNAMIC_FEATURE_QUERY = 3;
    // A dynamic query to request ImsService features has completed.
    private static final int HANDLER_DYNAMIC_FEATURE_CHANGE = 4;
    // Testing: Overrides the current configuration for ImsService binding
    private static final int HANDLER_OVERRIDE_IMS_SERVICE_CONFIG = 5;
    // Based on boot complete indication. When this happens, there may be ImsServices that are not
    // direct boot aware that need to be started.
    private static final int HANDLER_BOOT_COMPLETE = 6;
    // Sent when the number of slots has dynamically changed on the device. We will need to
    // resize available ImsServiceController slots and perform dynamic queries again.
    private static final int HANDLER_MSIM_CONFIGURATION_CHANGE = 7;
    // clear any carrier ImsService test overrides.
    private static final int HANDLER_CLEAR_CARRIER_IMS_SERVICE_CONFIG = 8;

    // Delay between dynamic ImsService queries.
    private static final int DELAY_DYNAMIC_QUERY_MS = 5000;

    private static ImsResolver sInstance;

    /**
     * Create the ImsResolver Service singleton instance.
     */
    public static void make(Context context, String defaultMmTelPackageName,
            String defaultRcsPackageName, int numSlots, ImsFeatureBinderRepository repo) {
        if (sInstance == null) {
            sInstance = new ImsResolver(context, defaultMmTelPackageName, defaultRcsPackageName,
                    numSlots, repo);
        }
    }

    /**
     * @return The ImsResolver Service instance. May be {@code null} if no ImsResolver was created
     * due to IMS not being supported.
     */
    public static @Nullable ImsResolver getInstance() {
        return sInstance;
    }

    private static class OverrideConfig {
        public final int slotId;
        public final boolean isCarrierService;
        public final Map<Integer, String> featureTypeToPackageMap;

        OverrideConfig(int slotIndex, boolean isCarrier, Map<Integer, String> feature) {
            slotId = slotIndex;
            isCarrierService = isCarrier;
            featureTypeToPackageMap = feature;
        }
    }

    /**
     * Stores information about an ImsService, including the package name, class name, and features
     * that the service supports.
     */
    @VisibleForTesting
    public static class ImsServiceInfo {
        public ComponentName name;
        // Determines if features were created from metadata in the manifest or through dynamic
        // query.
        public boolean featureFromMetadata = true;
        public ImsServiceControllerFactory controllerFactory;

        // Map slotId->Feature
        private final HashSet<ImsFeatureConfiguration.FeatureSlotPair> mSupportedFeatures;

        public ImsServiceInfo() {
            mSupportedFeatures = new HashSet<>();
        }

        void addFeatureForAllSlots(int numSlots, int feature) {
            for (int i = 0; i < numSlots; i++) {
                mSupportedFeatures.add(new ImsFeatureConfiguration.FeatureSlotPair(i, feature));
            }
        }

        void replaceFeatures(Set<ImsFeatureConfiguration.FeatureSlotPair> newFeatures) {
            mSupportedFeatures.clear();
            mSupportedFeatures.addAll(newFeatures);
        }

        @VisibleForTesting
        public Set<ImsFeatureConfiguration.FeatureSlotPair> getSupportedFeatures() {
            return mSupportedFeatures;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ImsServiceInfo that = (ImsServiceInfo) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            if (!mSupportedFeatures.equals(that.mSupportedFeatures)) {
                return false;
            }
            return controllerFactory != null ? controllerFactory.equals(that.controllerFactory)
                    : that.controllerFactory == null;
        }

        @Override
        public int hashCode() {
            // We do not include mSupportedFeatures in hashcode because the internal structure
            // changes after adding.
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (controllerFactory != null ? controllerFactory.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "[ImsServiceInfo] name="
                    + name
                    + ", featureFromMetadata="
                    + featureFromMetadata
                    + ","
                    + printFeatures(mSupportedFeatures);
        }
    }

    // Receives broadcasts from the system involving changes to the installed applications. If
    // an ImsService that we are configured to use is installed, we must bind to it.
    private final BroadcastReceiver mAppChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final String packageName = intent.getData().getSchemeSpecificPart();
            switch (action) {
                case Intent.ACTION_PACKAGE_ADDED:
                    // intentional fall-through
                case Intent.ACTION_PACKAGE_REPLACED:
                    // intentional fall-through
                case Intent.ACTION_PACKAGE_CHANGED:
                    mHandler.obtainMessage(HANDLER_ADD_PACKAGE, packageName).sendToTarget();
                    break;
                case Intent.ACTION_PACKAGE_REMOVED:
                    mHandler.obtainMessage(HANDLER_REMOVE_PACKAGE, packageName).sendToTarget();
                    break;
                default:
                    return;
            }
        }
    };

    // Receives the broadcast that a new Carrier Config has been loaded in order to possibly
    // unbind from one service and bind to another.
    private final BroadcastReceiver mConfigChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int slotId = intent.getIntExtra(CarrierConfigManager.EXTRA_SLOT_INDEX,
                    SubscriptionManager.INVALID_SIM_SLOT_INDEX);

            if (slotId == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                Log.i(TAG, "Received CCC for invalid slot id.");
                return;
            }

            int subId = intent.getIntExtra(CarrierConfigManager.EXTRA_SUBSCRIPTION_INDEX,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            int slotSimState = mTelephonyManagerProxy.getSimState(mContext, slotId);
            if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID
                    && (slotSimState != TelephonyManager.SIM_STATE_ABSENT
                    && slotSimState != TelephonyManager.SIM_STATE_NOT_READY)) {
                // We only care about carrier config updates that happen when a slot is known to be
                // absent, the subscription is disabled (not ready), or populated and the carrier
                // config has been loaded.
                Log.i(TAG, "Received CCC for slot " + slotId + " and sim state "
                        + slotSimState + ", ignoring.");
                return;
            }

            Log.i(TAG, "Received Carrier Config Changed for SlotId: " + slotId
                    + ", sim state: " + slotSimState);

            mHandler.obtainMessage(HANDLER_CONFIG_CHANGED, slotId).sendToTarget();
        }
    };

    // Receives the broadcast that the device has finished booting (and the device is no longer
    // encrypted).
    private final BroadcastReceiver mBootCompleted = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received BOOT_COMPLETED");
            // Recalculate all cached services to pick up ones that have just been enabled since
            // boot complete.
            mHandler.obtainMessage(HANDLER_BOOT_COMPLETE, null).sendToTarget();
        }
    };

    /**
     * Testing interface used to mock SubscriptionManager in testing
     */
    @VisibleForTesting
    public interface SubscriptionManagerProxy {
        /**
         * Mock-able interface for {@link SubscriptionManager#getSubId(int)} used for testing.
         */
        int getSubId(int slotId);
        /**
         * Mock-able interface for {@link SubscriptionManager#getSlotIndex(int)} used for testing.
         */
        int getSlotIndex(int subId);
    }

    /**
     * Testing interface used to stub out TelephonyManager dependencies.
     */
    @VisibleForTesting
    public interface TelephonyManagerProxy {
        /**
         * @return the SIM state for the slot ID specified.
         */
        int getSimState(Context context, int slotId);
    }

    private TelephonyManagerProxy mTelephonyManagerProxy = new TelephonyManagerProxy() {
        @Override
        public int getSimState(Context context, int slotId) {
            TelephonyManager tm = context.getSystemService(TelephonyManager.class);
            if (tm == null) {
                return TelephonyManager.SIM_STATE_UNKNOWN;
            }
            return tm.getSimState(slotId);
        }
    };

    private SubscriptionManagerProxy mSubscriptionManagerProxy = new SubscriptionManagerProxy() {
        @Override
        public int getSubId(int slotId) {
            int[] subIds = SubscriptionManager.getSubId(slotId);
            if (subIds != null) {
                // This is done in all other places getSubId is used.
                return subIds[0];
            }
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }

        @Override
        public int getSlotIndex(int subId) {
            return SubscriptionManager.getSlotIndex(subId);
        }
    };

    /**
     * Testing interface for injecting mock ImsServiceControllers.
     */
    @VisibleForTesting
    public interface ImsServiceControllerFactory {
        /**
         * @return the Service Interface String used for binding the ImsService.
         */
        String getServiceInterface();
        /**
         * @return the ImsServiceController created using the context and componentName supplied.
         */
        ImsServiceController create(Context context, ComponentName componentName,
                ImsServiceController.ImsServiceControllerCallbacks callbacks,
                ImsFeatureBinderRepository repo);
    }

    private ImsServiceControllerFactory mImsServiceControllerFactory =
            new ImsServiceControllerFactory() {

        @Override
        public String getServiceInterface() {
            return ImsService.SERVICE_INTERFACE;
        }

        @Override
        public ImsServiceController create(Context context, ComponentName componentName,
                ImsServiceController.ImsServiceControllerCallbacks callbacks,
                ImsFeatureBinderRepository repo) {
            return new ImsServiceController(context, componentName, callbacks, repo);
        }
    };

    /**
     * Used for testing.
     */
    @VisibleForTesting
    public interface ImsDynamicQueryManagerFactory {
        ImsServiceFeatureQueryManager create(Context context,
                ImsServiceFeatureQueryManager.Listener listener);
    }

    private final ImsServiceControllerFactory mImsServiceControllerFactoryCompat =
            new ImsServiceControllerFactory() {
                @Override
                public String getServiceInterface() {
                    return android.telephony.ims.compat.ImsService.SERVICE_INTERFACE;
                }

                @Override
                public ImsServiceController create(Context context, ComponentName componentName,
                        ImsServiceController.ImsServiceControllerCallbacks callbacks,
                        ImsFeatureBinderRepository repo) {
                    return new ImsServiceControllerCompat(context, componentName, callbacks, repo);
                }
            };

    private ImsDynamicQueryManagerFactory mDynamicQueryManagerFactory =
            ImsServiceFeatureQueryManager::new;

    private final CarrierConfigManager mCarrierConfigManager;
    private final Context mContext;
    // Special context created only for registering receivers for all users using UserHandle.ALL.
    // The lifetime of a registered receiver is bounded by the lifetime of the context it's
    // registered through, so we must retain the Context as long as we need the receiver to be
    // active.
    private final Context mReceiverContext;
    private final ImsFeatureBinderRepository mRepo;
    // Locks mBoundImsServicesByFeature only. Be careful to avoid deadlocks from
    // ImsServiceController callbacks.
    private final Object mBoundServicesLock = new Object();
    private int mNumSlots;
    // Array index corresponds to slot, per slot there is a feature->package name mapping.
    // should only be accessed from handler
    private final SparseArray<Map<Integer, String>> mCarrierServices;
    // Package name of the default device services, Maps ImsFeature -> packageName.
    // Must synchronize on this object to access.
    private final Map<Integer, String> mDeviceServices = new ArrayMap<>();
    // Persistent Logging
    private final LocalLog mEventLog = new LocalLog(50);

    private boolean mBootCompletedHandlerRan = false;
    private boolean mCarrierConfigReceived = false;

    // Synchronize all events on a handler to ensure that the cache includes the most recent
    // version of the installed ImsServices.
    private final Handler mHandler = new Handler(Looper.getMainLooper(), (msg) -> {
        switch (msg.what) {
            case HANDLER_ADD_PACKAGE: {
                String packageName = (String) msg.obj;
                maybeAddedImsService(packageName);
                break;
            }
            case HANDLER_REMOVE_PACKAGE: {
                String packageName = (String) msg.obj;
                maybeRemovedImsService(packageName);
                break;
            }
            case HANDLER_BOOT_COMPLETE: {
                if (!mBootCompletedHandlerRan) {
                    mBootCompletedHandlerRan = true;
                    mEventLog.log("handling BOOT_COMPLETE");
                    if (mCarrierConfigReceived) {
                        mEventLog.log("boot complete - reeval");
                        // Re-evaluate bound services for all slots after requerying packagemanager
                        maybeAddedImsService(null /*packageName*/);
                    } else {
                        mEventLog.log("boot complete - update cache");
                        // Do not bind any ImsServices yet, just update the cache to include new
                        // services. All will be re-evaluated after first carrier config changed.
                        updateInstalledServicesCache();
                    }
                }
                break;
            }
            case HANDLER_CONFIG_CHANGED: {
                int slotId = (Integer) msg.obj;
                // If the msim config has changed and there is a residual event for an invalid slot,
                // ignore.
                if (slotId >= mNumSlots) {
                    Log.w(TAG, "HANDLER_CONFIG_CHANGED for invalid slotid=" + slotId);
                    break;
                }
                mCarrierConfigReceived = true;
                carrierConfigChanged(slotId);
                break;
            }
            case HANDLER_START_DYNAMIC_FEATURE_QUERY: {
                ImsServiceInfo info = (ImsServiceInfo) msg.obj;
                startDynamicQuery(info);
                break;
            }
            case HANDLER_DYNAMIC_FEATURE_CHANGE: {
                SomeArgs args = (SomeArgs) msg.obj;
                ComponentName name = (ComponentName) args.arg1;
                Set<ImsFeatureConfiguration.FeatureSlotPair> features =
                        (Set<ImsFeatureConfiguration.FeatureSlotPair>) args.arg2;
                args.recycle();
                dynamicQueryComplete(name, features);
                break;
            }
            case HANDLER_OVERRIDE_IMS_SERVICE_CONFIG: {
                OverrideConfig config = (OverrideConfig) msg.obj;
                if (config.isCarrierService) {
                    overrideCarrierService(config.slotId,
                            config.featureTypeToPackageMap);
                } else {
                    overrideDeviceService(config.featureTypeToPackageMap);
                }
                break;
            }
            case HANDLER_MSIM_CONFIGURATION_CHANGE: {
                AsyncResult result = (AsyncResult) msg.obj;
                handleMsimConfigChange((Integer) result.result);
                break;
            }
            case HANDLER_CLEAR_CARRIER_IMS_SERVICE_CONFIG: {
                clearCarrierServiceOverrides(msg.arg1);
                break;
            }
            default:
                return false;
        }
        return true;
    });

    private final HandlerExecutor mRunnableExecutor = new HandlerExecutor(mHandler);

    // Results from dynamic queries to ImsService regarding the features they support.
    private final ImsServiceFeatureQueryManager.Listener mDynamicQueryListener =
            new ImsServiceFeatureQueryManager.Listener() {

                @Override
                public void onComplete(ComponentName name,
                        Set<ImsFeatureConfiguration.FeatureSlotPair> features) {
                    Log.d(TAG, "onComplete called for name: " + name + printFeatures(features));
                    handleFeaturesChanged(name, features);
                }

                @Override
                public void onError(ComponentName name) {
                    Log.w(TAG, "onError: " + name + "returned with an error result");
                    mEventLog.log("onError - dynamic query error for " + name);
                    scheduleQueryForFeatures(name, DELAY_DYNAMIC_QUERY_MS);
                }

                @Override
                public void onPermanentError(ComponentName name) {
                    Log.w(TAG, "onPermanentError: component=" + name);
                    mEventLog.log("onPermanentError - error for " + name);
                    mHandler.obtainMessage(HANDLER_REMOVE_PACKAGE,
                            name.getPackageName()).sendToTarget();
                }
            };

    // Used during testing, overrides the carrier services while non-empty.
    // Array index corresponds to slot, per slot there is a feature->package name mapping.
    // should only be accessed from handler
    private final SparseArray<SparseArray<String>> mOverrideServices;
    // Outer array index corresponds to Slot Id, Maps ImsFeature.FEATURE->bound ImsServiceController
    // Locked on mBoundServicesLock
    private final SparseArray<SparseArray<ImsServiceController>> mBoundImsServicesByFeature;
    // not locked, only accessed on a handler thread.
    // Tracks list of all installed ImsServices
    private final Map<ComponentName, ImsServiceInfo> mInstalledServicesCache = new HashMap<>();
    // not locked, only accessed on a handler thread.
    // Active ImsServiceControllers, which are bound to ImsServices.
    private final Map<ComponentName, ImsServiceController> mActiveControllers = new HashMap<>();
    private ImsServiceFeatureQueryManager mFeatureQueryManager;

    public ImsResolver(Context context, String defaultMmTelPackageName,
            String defaultRcsPackageName, int numSlots, ImsFeatureBinderRepository repo) {
        Log.i(TAG, "device MMTEL package: " + defaultMmTelPackageName + ", device RCS package:"
                + defaultRcsPackageName);
        mContext = context;
        mNumSlots = numSlots;
        mRepo = repo;
        mReceiverContext = context.createContextAsUser(UserHandle.ALL, 0 /*flags*/);

        mCarrierServices = new SparseArray<>(mNumSlots);
        setDeviceConfiguration(defaultMmTelPackageName, ImsFeature.FEATURE_EMERGENCY_MMTEL);
        setDeviceConfiguration(defaultMmTelPackageName, ImsFeature.FEATURE_MMTEL);
        setDeviceConfiguration(defaultRcsPackageName, ImsFeature.FEATURE_RCS);
        mCarrierConfigManager = (CarrierConfigManager) mContext.getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        mOverrideServices = new SparseArray<>(0 /*initial size*/);
        mBoundImsServicesByFeature = new SparseArray<>(mNumSlots);
    }

    @VisibleForTesting
    public void setTelephonyManagerProxy(TelephonyManagerProxy proxy) {
        mTelephonyManagerProxy = proxy;
    }

    @VisibleForTesting
    public void setSubscriptionManagerProxy(SubscriptionManagerProxy proxy) {
        mSubscriptionManagerProxy = proxy;
    }

    @VisibleForTesting
    public void setImsServiceControllerFactory(ImsServiceControllerFactory factory) {
        mImsServiceControllerFactory = factory;
    }

    @VisibleForTesting
    public Handler getHandler() {
        return mHandler;
    }

    @VisibleForTesting
    public void setImsDynamicQueryManagerFactory(ImsDynamicQueryManagerFactory m) {
        mDynamicQueryManagerFactory = m;
    }

    /**
     * Needs to be called after the constructor to kick off the process of binding to ImsServices.
     */
    public void initialize() {
        mEventLog.log("Initializing");
        Log.i(TAG, "Initializing cache.");
        PhoneConfigurationManager.registerForMultiSimConfigChange(mHandler,
                HANDLER_MSIM_CONFIGURATION_CHANGE, null);
        mFeatureQueryManager = mDynamicQueryManagerFactory.create(mContext, mDynamicQueryListener);

        updateInstalledServicesCache();

        IntentFilter appChangedFilter = new IntentFilter();
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        appChangedFilter.addDataScheme("package");
        mReceiverContext.registerReceiver(mAppChangedReceiver, appChangedFilter);
        mReceiverContext.registerReceiver(mConfigChangedReceiver, new IntentFilter(
                CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED));

        UserManager userManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        if (userManager.isUserUnlocked()) {
            mHandler.obtainMessage(HANDLER_BOOT_COMPLETE, null).sendToTarget();
        } else {
            mReceiverContext.registerReceiver(mBootCompleted, new IntentFilter(
                    Intent.ACTION_BOOT_COMPLETED));
            if (userManager.isUserUnlocked()) {
                mHandler.obtainMessage(HANDLER_BOOT_COMPLETE, null).sendToTarget();
            }
        }

        // Update the package names of the carrier ImsServices if they do not exist already and
        // possibly bind if carrier configs exist. Otherwise wait for CarrierConfigChanged
        // indication.
        bindCarrierServicesIfAvailable();
    }

    /**
     * Query the system for all registered ImsServices and add them to the cache if there are any
     * new ones that are not tracked.
     */
    private void updateInstalledServicesCache() {
        // This will get all services with the correct intent filter from PackageManager
        for (ImsServiceInfo info : getImsServiceInfo(null)) {
            if (!mInstalledServicesCache.containsKey(info.name)) {
                mInstalledServicesCache.put(info.name, info);
            }
        }
    }

    /**
     * Destroys this ImsResolver. Used for tearing down static resources during testing.
     */
    @VisibleForTesting
    public void destroy() {
        PhoneConfigurationManager.unregisterForMultiSimConfigChange(mHandler);
        mHandler.removeCallbacksAndMessages(null);
    }

    // Only start the bind if there is an existing Carrier Configuration. Otherwise, wait for
    // carrier config changed.
    private void bindCarrierServicesIfAvailable() {
        boolean hasConfigChanged = false;
        for (int slotId = 0; slotId < mNumSlots; slotId++) {
            Map<Integer, String> featureMap = getImsPackageOverrideConfig(slotId);
            for (int f = ImsFeature.FEATURE_EMERGENCY_MMTEL; f < ImsFeature.FEATURE_MAX; f++) {
                String newPackageName = featureMap.getOrDefault(f, "");
                if (!TextUtils.isEmpty(newPackageName)) {
                    mEventLog.log("bindCarrierServicesIfAvailable - carrier package found: "
                            + newPackageName + " on slot " + slotId);
                    // Carrier configs are already available, so mark received.
                    mCarrierConfigReceived = true;
                    setCarrierConfiguredPackageName(newPackageName, slotId, f);
                    ImsServiceInfo info = getImsServiceInfoFromCache(newPackageName);
                    // We do not want to trigger feature configuration changes unless there is
                    // already a valid carrier config change.
                    if (info != null && info.featureFromMetadata) {
                        hasConfigChanged = true;
                    } else {
                        // Config will change when this query completes
                        scheduleQueryForFeatures(info);
                    }
                }
            }
        }
        if (hasConfigChanged) calculateFeatureConfigurationChange();
    }

    /**
     * Notify ImsService to enable IMS for the framework. This will trigger IMS registration and
     * trigger ImsFeature status updates.
     */
    public void enableIms(int slotId) {
        SparseArray<ImsServiceController> controllers = getImsServiceControllers(slotId);
        if (controllers != null) {
            for (int i = 0; i < controllers.size(); i++) {
                int key = controllers.keyAt(i);
                controllers.get(key).enableIms(slotId);
            }
        }
    }

    /**
     * Notify ImsService to disable IMS for the framework. This will trigger IMS de-registration and
     * trigger ImsFeature capability status to become false.
     */
    public void disableIms(int slotId) {
        SparseArray<ImsServiceController> controllers = getImsServiceControllers(slotId);
        if (controllers != null) {
            for (int i = 0; i < controllers.size(); i++) {
                int key = controllers.keyAt(i);
                controllers.get(key).disableIms(slotId);
            }
        }
    }

    /**
     * Returns the ImsRegistration structure associated with the slotId and feature specified.
     */
    public @Nullable IImsRegistration getImsRegistration(int slotId, int feature) {
        ImsFeatureContainer fc = mRepo.getIfExists(slotId, feature).orElse(null);
        return  (fc != null) ? fc.imsRegistration : null;
    }

    /**
     * Returns the ImsConfig structure associated with the slotId and feature specified.
     */
    public @Nullable IImsConfig getImsConfig(int slotId, int feature) {
        ImsFeatureContainer fc = mRepo.getIfExists(slotId, feature).orElse(null);
        return  (fc != null) ? fc.imsConfig : null;
    }

    private  SparseArray<ImsServiceController> getImsServiceControllers(int slotId) {
        if (slotId < 0 || slotId >= mNumSlots) {
            return null;
        }
        synchronized (mBoundServicesLock) {
            SparseArray<ImsServiceController> services = mBoundImsServicesByFeature.get(slotId);
            if (services == null) {
                return null;
            }
            return services;
        }
    }

    /**
     * Register a new listener for the feature type and slot specified. ImsServiceController will
     * update the connections as they become available.
     */
    public void listenForFeature(int slotId, int feature, IImsServiceFeatureCallback callback) {
        mRepo.registerForConnectionUpdates(slotId, feature, callback, mRunnableExecutor);
    }

    /**
     * Unregister a previously registered IImsServiceFeatureCallback through
     * {@link #listenForFeature(int, int, IImsServiceFeatureCallback)}.
     * @param callback The callback to be unregistered.
     */
    public void unregisterImsFeatureCallback(IImsServiceFeatureCallback callback) {
        mRepo.unregisterForConnectionUpdates(callback);
    }

    // Used for testing only.
    public boolean clearCarrierImsServiceConfiguration(int slotId) {
        if (slotId < 0 || slotId >= mNumSlots) {
            Log.w(TAG, "clearCarrierImsServiceConfiguration: invalid slotId!");
            return false;
        }

        Message.obtain(mHandler, HANDLER_CLEAR_CARRIER_IMS_SERVICE_CONFIG, slotId, 0 /*arg2*/)
                .sendToTarget();
        return true;
    }

    // Used for testing only.
    public boolean overrideImsServiceConfiguration(int slotId, boolean isCarrierService,
            Map<Integer, String> featureConfig) {
        if (slotId < 0 || slotId >= mNumSlots) {
            Log.w(TAG, "overrideImsServiceConfiguration: invalid slotId!");
            return false;
        }

        OverrideConfig overrideConfig = new OverrideConfig(slotId, isCarrierService, featureConfig);
        Message.obtain(mHandler, HANDLER_OVERRIDE_IMS_SERVICE_CONFIG, overrideConfig)
                .sendToTarget();
        return true;
    }

    private String getDeviceConfiguration(@ImsFeature.FeatureType int featureType) {
        synchronized (mDeviceServices) {
            return mDeviceServices.getOrDefault(featureType, "");
        }
    }

    private void setDeviceConfiguration(String name, @ImsFeature.FeatureType int featureType) {
        synchronized (mDeviceServices) {
            mDeviceServices.put(featureType, name);
        }
    }

    // not synchronized, access in handler ONLY.
    private void setCarrierConfiguredPackageName(@NonNull String packageName, int slotId,
            @ImsFeature.FeatureType int featureType) {
        getCarrierConfiguredPackageNames(slotId).put(featureType, packageName);
    }

    // not synchronized, access in handler ONLY.
    private @NonNull String getCarrierConfiguredPackageName(int slotId,
            @ImsFeature.FeatureType int featureType) {
        return getCarrierConfiguredPackageNames(slotId).getOrDefault(featureType, "");
    }

    // not synchronized, access in handler ONLY.
    private @NonNull Map<Integer, String> getCarrierConfiguredPackageNames(int slotId) {
        Map<Integer, String> carrierConfig = mCarrierServices.get(slotId);
        if (carrierConfig == null) {
            carrierConfig = new ArrayMap<>();
            mCarrierServices.put(slotId, carrierConfig);
        }
        return carrierConfig;
    }

    // not synchronized, access in handler ONLY.
    private void removeOverridePackageName(int slotId) {
        for (int f = ImsFeature.FEATURE_EMERGENCY_MMTEL; f < ImsFeature.FEATURE_MAX; f++) {
            getOverridePackageName(slotId).remove(f);
        }
    }

    // not synchronized, access in handler ONLY.
    private void setOverridePackageName(@Nullable String packageName, int slotId,
            @ImsFeature.FeatureType int featureType) {
        getOverridePackageName(slotId).put(featureType, packageName);
    }

    // not synchronized, access in handler ONLY.
    private @Nullable String getOverridePackageName(int slotId,
            @ImsFeature.FeatureType int featureType) {
        return getOverridePackageName(slotId).get(featureType);
    }

    // not synchronized, access in handler ONLY.
    private @NonNull SparseArray<String> getOverridePackageName(int slotId) {
        SparseArray<String> carrierConfig = mOverrideServices.get(slotId);
        if (carrierConfig == null) {
            carrierConfig = new SparseArray<>();
            mOverrideServices.put(slotId, carrierConfig);
        }
        return carrierConfig;
    }

    /**
     * @return true if there is a carrier configuration that exists for the slot & featureType pair
     * and the cached carrier ImsService associated with the configuration also supports the
     * requested ImsFeature type.
     */
    // not synchronized, access in handler ONLY.
    private boolean doesCarrierConfigurationExist(int slotId,
            @ImsFeature.FeatureType int featureType) {
        String carrierPackage = getCarrierConfiguredPackageName(slotId, featureType);
        if (TextUtils.isEmpty(carrierPackage)) {
            return false;
        }
        // Config exists, but the carrier ImsService also needs to support this feature
        return doesCachedImsServiceExist(carrierPackage, slotId, featureType);
    }

    /**
     * Check the cached ImsServices that exist on this device to determine if there is a ImsService
     * with the same package name that matches the provided configuration.
     */
    // not synchronized, access in handler ONLY.
    private boolean doesCachedImsServiceExist(String packageName, int slotId,
            @ImsFeature.FeatureType int featureType) {
        // Config exists, but the carrier ImsService also needs to support this feature
        ImsServiceInfo info = getImsServiceInfoFromCache(packageName);
        return info != null && info.getSupportedFeatures().stream().anyMatch(
                feature -> feature.slotId == slotId && feature.featureType == featureType);
    }

    /**
     * @return the package name of the ImsService with the requested configuration.
     */
    // used in shell commands queries during testing only.
    public String getImsServiceConfiguration(int slotId, boolean isCarrierService,
            @ImsFeature.FeatureType int featureType) {
        if (slotId < 0 || slotId >= mNumSlots) {
            Log.w(TAG, "getImsServiceConfiguration: invalid slotId!");
            return "";
        }

        LinkedBlockingQueue<String> result = new LinkedBlockingQueue<>(1);
        // access the configuration on the handler.
        mHandler.post(() -> result.offer(isCarrierService
                ? getCarrierConfiguredPackageName(slotId, featureType) :
                getDeviceConfiguration(featureType)));
        try {
            return result.poll(GET_IMS_SERVICE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.w(TAG, "getImsServiceConfiguration: exception=" + e.getMessage());
            return null;
        }
    }

    /**
     * Determines if there is a valid ImsService configured for the specified ImsFeature.
     * @param slotId The slot ID to check for.
     * @param featureType The ImsFeature featureType to check for.
     * @return true if there is an ImsService configured for the specified ImsFeature type, false
     * if there is not.
     */
    public boolean isImsServiceConfiguredForFeature(int slotId,
            @ImsFeature.FeatureType int featureType) {
        if (!TextUtils.isEmpty(getDeviceConfiguration(featureType))) {
            // Shortcut a little bit here - instead of dynamically looking up the configured
            // package name, which can be a long operation depending on the state, just return true
            // if there is a configured device ImsService for the requested feature because that
            // means there will always be at least a device configured ImsService.
            return true;
        }
        return !TextUtils.isEmpty(getConfiguredImsServicePackageName(slotId, featureType));
    }

    /**
     * Resolves the PackageName of the ImsService that is configured to be bound for the slotId and
     * FeatureType specified and returns it.
     * <p>
     * If there is a PackageName that is configured, but there is no application on the device that
     * fulfills that configuration, this method will also return {@code null} as the ImsService will
     * not be bound.
     *
     * @param slotId The slot ID that the request is for.
     * @param featureType The ImsService feature type that the request is for.
     * @return The package name of the ImsService that will be bound from telephony for the provided
     * slot id and featureType.
     */
    public String getConfiguredImsServicePackageName(int slotId,
            @ImsFeature.FeatureType int featureType) {
        if (slotId < 0 || slotId >= mNumSlots || featureType <= ImsFeature.FEATURE_INVALID
                || featureType >= ImsFeature.FEATURE_MAX) {
            Log.w(TAG, "getResolvedImsServicePackageName received invalid parameters - slot: "
                    + slotId + ", feature: " + featureType);
            return null;
        }
        CompletableFuture<String> packageNameFuture = new CompletableFuture<>();
        final long startTimeMs = System.currentTimeMillis();
        if (mHandler.getLooper().isCurrentThread()) {
            // If we are on the same thread as the Handler's looper, run the internal method
            // directly.
            packageNameFuture.complete(getConfiguredImsServicePackageNameInternal(slotId,
                    featureType));
        } else {
            mHandler.post(() -> {
                try {
                    packageNameFuture.complete(getConfiguredImsServicePackageNameInternal(slotId,
                            featureType));
                } catch (Exception e) {
                    // Catch all Exceptions to ensure we do not block indefinitely in the case of an
                    // unexpected error.
                    packageNameFuture.completeExceptionally(e);
                }
            });
        }
        try {
            String packageName = packageNameFuture.get();
            long timeDiff = System.currentTimeMillis() - startTimeMs;
            if (timeDiff > 50) {
                // Took an unusually long amount of time (> 50 ms), so log it.
                mEventLog.log("getResolvedImsServicePackageName - [" + slotId + ", "
                        + ImsFeature.FEATURE_LOG_MAP.get(featureType)
                        + "], async query complete, took " + timeDiff + " ms with package name: "
                        + packageName);
                Log.w(TAG, "getResolvedImsServicePackageName: [" + slotId + ", "
                        + ImsFeature.FEATURE_LOG_MAP.get(featureType)
                        + "], async query complete, took " + timeDiff + " ms with package name: "
                        + packageName);
            }
            return packageName;
        } catch (Exception e) {
            mEventLog.log("getResolvedImsServicePackageName - [" + slotId + ", "
                    + ImsFeature.FEATURE_LOG_MAP.get(featureType) + "] -> Exception: " + e);
            Log.w(TAG, "getResolvedImsServicePackageName: [" + slotId + ", "
                    + ImsFeature.FEATURE_LOG_MAP.get(featureType) + "] returned Exception: " + e);
            return null;
        }
    }

    /**
     * @return the package name for the configured carrier ImsService if it exists on the device and
     * supports the supplied slotId and featureType. If no such configuration exists, fall back to
     * the device ImsService. If neither exist, then return {@code null};
     */
    // Not synchronized, access on Handler ONLY!
    private String getConfiguredImsServicePackageNameInternal(int slotId,
            @ImsFeature.FeatureType int featureType) {
        // If a carrier ImsService is configured to be used for the provided slotId and
        // featureType, then return that one.
        String carrierPackage = getCarrierConfiguredPackageName(slotId, featureType);
        if (!TextUtils.isEmpty(carrierPackage)
                && doesCachedImsServiceExist(carrierPackage, slotId, featureType)) {
            return carrierPackage;
        }
        // If there is no carrier ImsService configured for that configuration, then
        // return the device's default ImsService for the provided slotId and
        // featureType.
        String devicePackage = getDeviceConfiguration(featureType);
        if (!TextUtils.isEmpty(devicePackage)
                && doesCachedImsServiceExist(devicePackage, slotId, featureType)) {
            return devicePackage;
        }
        // There is no ImsService configuration that exists for the slotId and
        // featureType.
        return null;
    }

    private void putImsController(int slotId, int feature, ImsServiceController controller) {
        if (slotId < 0 || slotId >= mNumSlots || feature <= ImsFeature.FEATURE_INVALID
                || feature >= ImsFeature.FEATURE_MAX) {
            Log.w(TAG, "putImsController received invalid parameters - slot: " + slotId
                    + ", feature: " + feature);
            return;
        }
        synchronized (mBoundServicesLock) {
            SparseArray<ImsServiceController> services = mBoundImsServicesByFeature.get(slotId);
            if (services == null) {
                services = new SparseArray<>();
                mBoundImsServicesByFeature.put(slotId, services);
            }
            mEventLog.log("putImsController - [" + slotId + ", "
                    + ImsFeature.FEATURE_LOG_MAP.get(feature) + "] -> " + controller);
            Log.i(TAG, "ImsServiceController added on slot: " + slotId + " with feature: "
                    + ImsFeature.FEATURE_LOG_MAP.get(feature) + " using package: "
                    + controller.getComponentName());
            services.put(feature, controller);
        }
    }

    private ImsServiceController removeImsController(int slotId, int feature) {
        if (slotId < 0 || feature <= ImsFeature.FEATURE_INVALID
                || feature >= ImsFeature.FEATURE_MAX) {
            Log.w(TAG, "removeImsController received invalid parameters - slot: " + slotId
                    + ", feature: " + feature);
            return null;
        }
        synchronized (mBoundServicesLock) {
            SparseArray<ImsServiceController> services = mBoundImsServicesByFeature.get(slotId);
            if (services == null) {
                return null;
            }
            ImsServiceController c = services.get(feature, null);
            if (c != null) {
                mEventLog.log("removeImsController - [" + slotId + ", "
                        + ImsFeature.FEATURE_LOG_MAP.get(feature) + "] -> " + c);
                Log.i(TAG, "ImsServiceController removed on slot: " + slotId + " with feature: "
                        + ImsFeature.FEATURE_LOG_MAP.get(feature) + " using package: "
                        + c.getComponentName());
                services.remove(feature);
            }
            return c;
        }
    }

    // Update the current cache with the new ImsService(s) if it has been added or update the
    // supported IMS features if they have changed.
    // Called from the handler ONLY
    private void maybeAddedImsService(String packageName) {
        Log.d(TAG, "maybeAddedImsService, packageName: " + packageName);
        List<ImsServiceInfo> infos = getImsServiceInfo(packageName);
        // Wait until all ImsServiceInfo is cached before calling
        // calculateFeatureConfigurationChange to reduce churn.
        boolean requiresCalculation = false;
        for (ImsServiceInfo info : infos) {
            // Checking to see if the ComponentName is the same, so we can update the supported
            // features. Will only be one (if it exists), since it is a set.
            ImsServiceInfo match = getInfoByComponentName(mInstalledServicesCache, info.name);
            if (match != null) {
                // for dynamic query the new "info" will have no supported features yet. Don't wipe
                // out the cache for the existing features or update yet. Instead start a query
                // for features dynamically.
                if (info.featureFromMetadata) {
                    mEventLog.log("maybeAddedImsService - updating features for " + info.name
                            + ": " + printFeatures(match.getSupportedFeatures()) + " -> "
                            + printFeatures(info.getSupportedFeatures()));
                    Log.i(TAG, "Updating features in cached ImsService: " + info.name);
                    Log.d(TAG, "Updating features - Old features: " + match + " new features: "
                            + info);
                    // update features in the cache
                    match.replaceFeatures(info.getSupportedFeatures());
                    requiresCalculation = true;
                } else {
                    mEventLog.log("maybeAddedImsService - scheduling query for " + info);
                    // start a query to get ImsService features
                    scheduleQueryForFeatures(info);
                }
            } else {
                Log.i(TAG, "Adding newly added ImsService to cache: " + info.name);
                mEventLog.log("maybeAddedImsService - adding new ImsService: " + info);
                mInstalledServicesCache.put(info.name, info);
                if (info.featureFromMetadata) {
                    requiresCalculation = true;
                } else {
                    // newly added ImsServiceInfo that has not had features queried yet. Start async
                    // bind and query features.
                    scheduleQueryForFeatures(info);
                }
            }
        }
        if (requiresCalculation) calculateFeatureConfigurationChange();
    }

    // Remove the ImsService from the cache. This may have been due to the ImsService being removed
    // from the device or was returning permanent errors when bound.
    // Called from the handler ONLY
    private boolean maybeRemovedImsService(String packageName) {
        ImsServiceInfo match = getInfoByPackageName(mInstalledServicesCache, packageName);
        if (match != null) {
            mInstalledServicesCache.remove(match.name);
            mEventLog.log("maybeRemovedImsService - removing ImsService: " + match);
            Log.i(TAG, "Removing ImsService: " + match.name);
            unbindImsService(match);
            calculateFeatureConfigurationChange();
            return true;
        }
        return false;
    }

    private boolean isDeviceService(ImsServiceInfo info) {
        if (info == null) return false;
        synchronized (mDeviceServices) {
            return mDeviceServices.containsValue(info.name.getPackageName());
        }
    }

    private List<Integer> getSlotsForActiveCarrierService(ImsServiceInfo info) {
        if (info == null) return Collections.emptyList();
        List<Integer> slots = new ArrayList<>(mNumSlots);
        for (int i = 0; i < mNumSlots; i++) {
            if (!TextUtils.isEmpty(getCarrierConfiguredPackageNames(i).values().stream()
                    .filter(e -> e.equals(info.name.getPackageName())).findAny().orElse(""))) {
                slots.add(i);
            }
        }
        return slots;
    }

    private ImsServiceController getControllerByServiceInfo(
            Map<ComponentName, ImsServiceController> searchMap, ImsServiceInfo matchValue) {
        return searchMap.values().stream()
                .filter(c -> Objects.equals(c.getComponentName(), matchValue.name))
                .findFirst().orElse(null);
    }

    private ImsServiceInfo getInfoByPackageName(Map<ComponentName, ImsServiceInfo> searchMap,
            String matchValue) {
        return searchMap.values().stream()
                .filter((i) -> Objects.equals(i.name.getPackageName(), matchValue))
                .findFirst().orElse(null);
    }

    private ImsServiceInfo getInfoByComponentName(
            Map<ComponentName, ImsServiceInfo> searchMap, ComponentName matchValue) {
        return searchMap.get(matchValue);
    }

    private void bindImsServiceWithFeatures(ImsServiceInfo info,
            Set<ImsFeatureConfiguration.FeatureSlotPair> features) {
        // Only bind if there are features that will be created by the service.
        if (shouldFeaturesCauseBind(features)) {
            // Check to see if an active controller already exists
            ImsServiceController controller = getControllerByServiceInfo(mActiveControllers, info);
            if (controller != null) {
                Log.i(TAG, "ImsService connection exists for " + info.name + ", updating features "
                        + features);
                try {
                    controller.changeImsServiceFeatures(features);
                    // Features have been set, there was an error adding/removing. When the
                    // controller recovers, it will add/remove again.
                } catch (RemoteException e) {
                    Log.w(TAG, "bindImsService: error=" + e.getMessage());
                }
            } else {
                controller = info.controllerFactory.create(mContext, info.name, this, mRepo);
                Log.i(TAG, "Binding ImsService: " + controller.getComponentName()
                        + " with features: " + features);
                controller.bind(features);
                mEventLog.log("bindImsServiceWithFeatures - create new controller: "
                        + controller);
            }
            mActiveControllers.put(info.name, controller);
        }
    }

    // Clean up and unbind from an ImsService
    private void unbindImsService(ImsServiceInfo info) {
        if (info == null) {
            return;
        }
        ImsServiceController controller = getControllerByServiceInfo(mActiveControllers, info);
        if (controller != null) {
            // Calls imsServiceFeatureRemoved on all features in the controller
            try {
                Log.i(TAG, "Unbinding ImsService: " + controller.getComponentName());
                mEventLog.log("unbindImsService - unbinding and removing " + controller);
                controller.unbind();
            } catch (RemoteException e) {
                Log.e(TAG, "unbindImsService: Remote Exception: " + e.getMessage());
            }
            mActiveControllers.remove(info.name);
        }
    }

    // Calculate which features an ImsServiceController will need. If it is the carrier specific
    // ImsServiceController, it will be granted all of the features it requests on the associated
    // slot. If it is the device ImsService, it will get all of the features not covered by the
    // carrier implementation.
    private HashSet<ImsFeatureConfiguration.FeatureSlotPair> calculateFeaturesToCreate(
            ImsServiceInfo info) {
        HashSet<ImsFeatureConfiguration.FeatureSlotPair> imsFeaturesBySlot = new HashSet<>();
        List<Integer> slots = getSlotsForActiveCarrierService(info);
        if (!slots.isEmpty()) {
            // There is an active carrier config associated with this. Return with the ImsService's
            // supported features that are also within the carrier configuration
            imsFeaturesBySlot.addAll(info.getSupportedFeatures().stream()
                    .filter(feature -> info.name.getPackageName().equals(
                            getCarrierConfiguredPackageName(feature.slotId, feature.featureType)))
                    .collect(Collectors.toList()));
            return imsFeaturesBySlot;
        }
        if (isDeviceService(info)) {
            imsFeaturesBySlot.addAll(info.getSupportedFeatures().stream()
                    // only allow supported features that are also set for this package as the
                    // device configuration.
                    .filter(feature -> info.name.getPackageName().equals(
                            getDeviceConfiguration(feature.featureType)))
                    // filter out any separate carrier configuration, since that feature is handled
                    // by the carrier ImsService.
                    .filter(feature -> !doesCarrierConfigurationExist(feature.slotId,
                            feature.featureType))
                    .collect(Collectors.toList()));
        }
        return imsFeaturesBySlot;
    }

    /**
     * Implementation of
     * {@link ImsServiceController.ImsServiceControllerCallbacks#imsServiceFeatureCreated}, which
     * removes the ImsServiceController from the mBoundImsServicesByFeature structure.
     */
    @Override
    public void imsServiceFeatureCreated(int slotId, int feature, ImsServiceController controller) {
        putImsController(slotId, feature, controller);
    }

    /**
     * Implementation of
     * {@link ImsServiceController.ImsServiceControllerCallbacks#imsServiceFeatureRemoved}, which
     * removes the ImsServiceController from the mBoundImsServicesByFeature structure.
     */
    @Override
    public void imsServiceFeatureRemoved(int slotId, int feature, ImsServiceController controller) {
        removeImsController(slotId, feature);
    }

    /**
     * Implementation of
     * {@link ImsServiceController.ImsServiceControllerCallbacks#imsServiceFeaturesChanged, which
     * notify the ImsResolver of a change to the supported ImsFeatures of a connected ImsService.
     */
    public void imsServiceFeaturesChanged(ImsFeatureConfiguration config,
            ImsServiceController controller) {
        if (controller == null || config == null) {
            return;
        }
        Log.i(TAG, "imsServiceFeaturesChanged: config=" + config.getServiceFeatures()
                + ", ComponentName=" + controller.getComponentName());
        mEventLog.log("imsServiceFeaturesChanged - for " + controller + ", new config "
                + config.getServiceFeatures());
        handleFeaturesChanged(controller.getComponentName(), config.getServiceFeatures());
    }

    @Override
    public void imsServiceBindPermanentError(ComponentName name) {
        if (name == null) {
            return;
        }
        Log.w(TAG, "imsServiceBindPermanentError: component=" + name);
        mEventLog.log("imsServiceBindPermanentError - for " + name);
        mHandler.obtainMessage(HANDLER_REMOVE_PACKAGE, name.getPackageName()).sendToTarget();
    }

    /**
     * Determines if the features specified should cause a bind or keep a binding active to an
     * ImsService.
     * @return true if MMTEL or RCS features are present, false if they are not or only
     * EMERGENCY_MMTEL is specified.
     */
    private boolean shouldFeaturesCauseBind(Set<ImsFeatureConfiguration.FeatureSlotPair> features) {
        long bindableFeatures = features.stream()
                // remove all emergency features
                .filter(f -> f.featureType != ImsFeature.FEATURE_EMERGENCY_MMTEL).count();
        return bindableFeatures > 0;
    }

    // Possibly rebind to another ImsService for testing carrier ImsServices.
    // Called from the handler ONLY
    private void overrideCarrierService(int slotId, Map<Integer, String> featureMap) {
        for (Integer featureType : featureMap.keySet()) {
            String overridePackageName = featureMap.get(featureType);
            mEventLog.log("overriding carrier ImsService to " + overridePackageName
                    + " on slot " + slotId + " for feature "
                    + ImsFeature.FEATURE_LOG_MAP.getOrDefault(featureType, "invalid"));
            setOverridePackageName(overridePackageName, slotId, featureType);
        }
        updateBoundServices(slotId, Collections.emptyMap());
    }

    // Possibly rebind to another ImsService for testing carrier ImsServices.
    // Called from the handler ONLY
    private void clearCarrierServiceOverrides(int slotId) {
        Log.i(TAG, "clearing carrier ImsService overrides");
        mEventLog.log("clearing carrier ImsService overrides");
        removeOverridePackageName(slotId);
        carrierConfigChanged(slotId);
    }

    // Possibly rebind to another ImsService for testing carrier ImsServices.
    // Called from the handler ONLY
    private void overrideDeviceService(Map<Integer, String> featureMap) {
        boolean requiresRecalc = false;
        for (Integer featureType : featureMap.keySet()) {
            String overridePackageName = featureMap.get(featureType);
            mEventLog.log("overriding device ImsService to " + overridePackageName + " for feature "
                    + ImsFeature.FEATURE_LOG_MAP.getOrDefault(featureType, "invalid"));
            String oldPackageName = getDeviceConfiguration(featureType);
            if (!TextUtils.equals(oldPackageName, overridePackageName)) {
                Log.i(TAG, "overrideDeviceService - device package changed (override): "
                        + oldPackageName + " -> " + overridePackageName);
                mEventLog.log("overrideDeviceService - device package changed (override): "
                        + oldPackageName + " -> " + overridePackageName);
                setDeviceConfiguration(overridePackageName, featureType);
                ImsServiceInfo info = getImsServiceInfoFromCache(overridePackageName);
                if (info == null || info.featureFromMetadata) {
                    requiresRecalc = true;
                } else {
                    // Config will change when this query completes
                    scheduleQueryForFeatures(info);
                }
            }
        }
        if (requiresRecalc) calculateFeatureConfigurationChange();
    }

    // Called from handler ONLY.
    private void carrierConfigChanged(int slotId) {
        updateBoundDeviceServices();

        if (slotId <= SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            // not specified, update carrier override cache and possibly rebind on all slots.
            for (int i = 0; i < mNumSlots; i++) {
                updateBoundServices(i, getImsPackageOverrideConfig(i));
            }
        }
        updateBoundServices(slotId, getImsPackageOverrideConfig(slotId));
    }

    private void updateBoundDeviceServices() {
        Log.d(TAG, "updateBoundDeviceServices: called");
        ArrayMap<String, ImsServiceInfo> featureDynamicImsPackages = new ArrayMap<>();
        for (int f = ImsFeature.FEATURE_EMERGENCY_MMTEL; f < ImsFeature.FEATURE_MAX; f++) {
            String packageName = getDeviceConfiguration(f);
            ImsServiceInfo serviceInfo = getImsServiceInfoFromCache(packageName);
            if (serviceInfo != null && !serviceInfo.featureFromMetadata
                    && !featureDynamicImsPackages.containsKey(packageName)) {
                featureDynamicImsPackages.put(packageName, serviceInfo);

                Log.d(TAG, "updateBoundDeviceServices: Schedule query for package=" + packageName);
                scheduleQueryForFeatures(featureDynamicImsPackages.get(packageName));
            }
        }
    }

    private void updateBoundServices(int slotId, Map<Integer, String> featureMap) {
        if (slotId <= SubscriptionManager.INVALID_SIM_SLOT_INDEX || slotId >= mNumSlots) {
            return;
        }
        boolean hasConfigChanged = false;
        boolean didQuerySchedule = false;
        for (int f = ImsFeature.FEATURE_EMERGENCY_MMTEL; f < ImsFeature.FEATURE_MAX; f++) {
            String overridePackageName = getOverridePackageName(slotId, f);
            String oldPackageName = getCarrierConfiguredPackageName(slotId, f);
            String newPackageName = featureMap.getOrDefault(f, "");
            if (!TextUtils.isEmpty(overridePackageName)) {
                // Do not allow carrier config changes to change the override package while it
                // is in effect.
                Log.i(TAG, String.format("updateBoundServices: overriding %s with %s for feature"
                                + " %s on slot %d",
                        TextUtils.isEmpty(newPackageName) ? "(none)" : newPackageName,
                        overridePackageName,
                        ImsFeature.FEATURE_LOG_MAP.getOrDefault(f, "invalid"), slotId));
                newPackageName = overridePackageName;
            }

            setCarrierConfiguredPackageName(newPackageName, slotId, f);
            // Carrier config may have not changed, but we still want to kick off a recalculation
            // in case there has been a change to the supported device features.
            ImsServiceInfo info = getImsServiceInfoFromCache(newPackageName);
            Log.i(TAG, "updateBoundServices - carrier package changed: "
                    + oldPackageName + " -> " + newPackageName + " on slot " + slotId
                    + ", hasConfigChanged=" + hasConfigChanged);
            mEventLog.log("updateBoundServices - carrier package changed: "
                    + oldPackageName + " -> " + newPackageName + " on slot " + slotId
                    + ", hasConfigChanged=" + hasConfigChanged);
            if (info == null || info.featureFromMetadata) {
                hasConfigChanged = true;
            } else {
                // Config will change when this query completes
                scheduleQueryForFeatures(info);
                didQuerySchedule = true;
            }
        }
        if (hasConfigChanged) calculateFeatureConfigurationChange();

        if (hasConfigChanged && didQuerySchedule) {
            mEventLog.log("[warning] updateBoundServices - both hasConfigChange and query "
                    + "scheduled on slot " + slotId);
        }
    }

    private @NonNull Map<Integer, String> getImsPackageOverrideConfig(int slotId) {
        int subId = mSubscriptionManagerProxy.getSubId(slotId);
        PersistableBundle config = mCarrierConfigManager.getConfigForSubId(subId);
        if (config == null) return Collections.emptyMap();
        String packageNameMmTel = config.getString(
                CarrierConfigManager.KEY_CONFIG_IMS_PACKAGE_OVERRIDE_STRING, null);
        // Set the config equal for the deprecated key.
        String packageNameRcs = packageNameMmTel;
        packageNameMmTel = config.getString(
                CarrierConfigManager.KEY_CONFIG_IMS_MMTEL_PACKAGE_OVERRIDE_STRING,
                packageNameMmTel);
        packageNameRcs = config.getString(
                CarrierConfigManager.KEY_CONFIG_IMS_RCS_PACKAGE_OVERRIDE_STRING, packageNameRcs);
        Map<Integer, String> result = new ArrayMap<>();
        if (!TextUtils.isEmpty(packageNameMmTel)) {
            result.put(ImsFeature.FEATURE_EMERGENCY_MMTEL, packageNameMmTel);
            result.put(ImsFeature.FEATURE_MMTEL, packageNameMmTel);
        }
        if (!TextUtils.isEmpty(packageNameRcs)) {
            result.put(ImsFeature.FEATURE_RCS, packageNameRcs);
        }
        return result;
    }

    /**
     * Schedules a query for dynamic ImsService features.
     */
    private void scheduleQueryForFeatures(ImsServiceInfo service, int delayMs) {
        if (service == null) {
            return;
        }
        Message msg = Message.obtain(mHandler, HANDLER_START_DYNAMIC_FEATURE_QUERY, service);
        if (mHandler.hasMessages(HANDLER_START_DYNAMIC_FEATURE_QUERY, service)) {
            Log.d(TAG, "scheduleQueryForFeatures: dynamic query for " + service.name
                    + " already scheduled");
            return;
        }
        Log.d(TAG, "scheduleQueryForFeatures: starting dynamic query for " + service.name
                + " in " + delayMs + "ms.");
        mHandler.sendMessageDelayed(msg, delayMs);
    }

    private void scheduleQueryForFeatures(ComponentName name, int delayMs) {
        ImsServiceInfo service = getImsServiceInfoFromCache(name.getPackageName());
        if (service == null) {
            Log.w(TAG, "scheduleQueryForFeatures: Couldn't find cached info for name: " + name);
            return;
        }
        scheduleQueryForFeatures(service, delayMs);
    }

    private void scheduleQueryForFeatures(ImsServiceInfo service) {
        scheduleQueryForFeatures(service, 0);
    }

    /**
     * Schedules the processing of a completed query.
     */
    private void handleFeaturesChanged(ComponentName name,
            Set<ImsFeatureConfiguration.FeatureSlotPair> features) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = name;
        args.arg2 = features;
        mHandler.obtainMessage(HANDLER_DYNAMIC_FEATURE_CHANGE, args).sendToTarget();
    }

    private void handleMsimConfigChange(Integer newNumSlots) {
        int oldLen = mNumSlots;
        if (oldLen == newNumSlots) {
            return;
        }
        mNumSlots = newNumSlots;
        Log.i(TAG, "handleMsimConfigChange: oldLen=" + oldLen + ", newLen=" + newNumSlots);
        mEventLog.log("MSIM config change: " + oldLen + " -> " + newNumSlots);
        if (newNumSlots < oldLen) {
            // we need to trim data structures that use slots, however mBoundImsServicesByFeature
            // will be updated by ImsServiceController changing to remove features on old slots.
            // start at the index of the new highest slot + 1.
            for (int oldSlot = newNumSlots; oldSlot < oldLen; oldSlot++) {
                // First clear old carrier configs
                Map<Integer, String> carrierConfigs = getCarrierConfiguredPackageNames(oldSlot);
                for (Integer feature : carrierConfigs.keySet()) {
                    setCarrierConfiguredPackageName("", oldSlot, feature);
                }
                // next clear old overrides
                SparseArray<String> overrideConfigs = getOverridePackageName(oldSlot);
                for (int i = 0; i < overrideConfigs.size(); i++) {
                    int feature = overrideConfigs.keyAt(i);
                    setOverridePackageName("", oldSlot, feature);
                }
            }
        }
        // Get the new config for each ImsService. For manifest queries, this will update the
        // number of slots.
        // This will get all services with the correct intent filter from PackageManager
        List<ImsServiceInfo> infos = getImsServiceInfo(null);
        for (ImsServiceInfo info : infos) {
            ImsServiceInfo cachedInfo = mInstalledServicesCache.get(info.name);
            if (cachedInfo != null) {
                if (info.featureFromMetadata) {
                    cachedInfo.replaceFeatures(info.getSupportedFeatures());
                } else {
                    // Remove features that are no longer supported by the device configuration.
                    cachedInfo.getSupportedFeatures()
                            .removeIf(filter -> filter.slotId >= newNumSlots);
                }
            } else {
                // This is unexpected, put the new service on the queue to be added
                mEventLog.log("handleMsimConfigChange: detected untracked service - " + info);
                Log.w(TAG, "handleMsimConfigChange: detected untracked package, queueing to add "
                        + info);
                mHandler.obtainMessage(HANDLER_ADD_PACKAGE,
                        info.name.getPackageName()).sendToTarget();
            }
        }

        if (newNumSlots < oldLen) {
            // A CarrierConfigChange will happen for the new slot, so only recalculate if there are
            // less new slots because we need to remove the old capabilities.
            calculateFeatureConfigurationChange();
        }
    }

    // Starts a dynamic query. Called from handler ONLY.
    private void startDynamicQuery(ImsServiceInfo service) {
        // if not current device/carrier service, don't perform query. If this changes, this method
        // will be called again.
        if (!isDeviceService(service) && getSlotsForActiveCarrierService(service).isEmpty()) {
            Log.i(TAG, "scheduleQueryForFeatures: skipping query for ImsService that is not"
                    + " set as carrier/device ImsService.");
            return;
        }
        mEventLog.log("startDynamicQuery - starting query for " + service);
        boolean queryStarted = mFeatureQueryManager.startQuery(service.name,
                service.controllerFactory.getServiceInterface());
        if (!queryStarted) {
            Log.w(TAG, "startDynamicQuery: service could not connect. Retrying after delay.");
            mEventLog.log("startDynamicQuery - query failed. Retrying in "
                    + DELAY_DYNAMIC_QUERY_MS + " mS");
            scheduleQueryForFeatures(service, DELAY_DYNAMIC_QUERY_MS);
        } else {
            Log.d(TAG, "startDynamicQuery: Service queried, waiting for response.");
        }
    }

    // process complete dynamic query. Called from handler ONLY.
    private void dynamicQueryComplete(ComponentName name,
            Set<ImsFeatureConfiguration.FeatureSlotPair> features) {
        ImsServiceInfo service = getImsServiceInfoFromCache(name.getPackageName());
        if (service == null) {
            Log.w(TAG, "dynamicQueryComplete: Couldn't find cached info for name: "
                    + name);
            return;
        }
        sanitizeFeatureConfig(features);
        mEventLog.log("dynamicQueryComplete: for package " + name + ", features: "
                + printFeatures(service.getSupportedFeatures()) + " -> " + printFeatures(features));
        // Add features to service
        service.replaceFeatures(features);
        // Wait until all queries have completed before changing the configuration to reduce churn.
        if (!mFeatureQueryManager.isQueryInProgress()) {
            if (mHandler.hasMessages(HANDLER_DYNAMIC_FEATURE_CHANGE)) {
                mEventLog.log("[warning] dynamicQueryComplete - HANDLER_DYNAMIC_FEATURE_CHANGE "
                        + "pending with calculateFeatureConfigurationChange()");
            }
            calculateFeatureConfigurationChange();
        }
    }

    /**
     * Sanitize feature configurations from the ImsService.
     * <ul>
     *     <li> Strip out feature configs for inactive slots.</li>
     *     <li> Ensure the feature includes MMTEL when it supports EMERGENCY_MMTEL, if not, remove.
     *     </li>
     * </ul>
     */
    private void sanitizeFeatureConfig(Set<ImsFeatureConfiguration.FeatureSlotPair> features) {
        // remove configs for slots that are mot active.
        features.removeIf(f -> f.slotId >= mNumSlots);
        // Ensure that if EMERGENCY_MMTEL is defined for a slot, MMTEL is also defined.
        Set<ImsFeatureConfiguration.FeatureSlotPair> emergencyMmtelFeatures = features.stream()
                .filter(feature -> feature.featureType == ImsFeature.FEATURE_EMERGENCY_MMTEL)
                .collect(Collectors.toSet());
        for (ImsFeatureConfiguration.FeatureSlotPair feature : emergencyMmtelFeatures) {
            if (!features.contains(new ImsFeatureConfiguration.FeatureSlotPair(feature.slotId,
                    ImsFeature.FEATURE_MMTEL))) {
                features.remove(feature);
            }
        }
    }

    // Calculate the new configuration for the bound ImsServices.
    // Should ONLY be called from the handler.
    private void calculateFeatureConfigurationChange() {
        for (ImsServiceInfo info : mInstalledServicesCache.values()) {
            Set<ImsFeatureConfiguration.FeatureSlotPair> features = calculateFeaturesToCreate(info);
            if (shouldFeaturesCauseBind(features)) {
                bindImsServiceWithFeatures(info, features);
            } else {
                unbindImsService(info);
            }
        }
    }

    private static String printFeatures(Set<ImsFeatureConfiguration.FeatureSlotPair> features) {
        StringBuilder featureString = new StringBuilder();
        featureString.append(" features: [");
        if (features != null) {
            for (ImsFeatureConfiguration.FeatureSlotPair feature : features) {
                featureString.append("{");
                featureString.append(feature.slotId);
                featureString.append(",");
                featureString.append(ImsFeature.FEATURE_LOG_MAP.get(feature.featureType));
                featureString.append("}");
            }
            featureString.append("]");
        }
        return featureString.toString();
    }

    /**
     * Returns the ImsServiceInfo that matches the provided packageName. Visible for testing
     * the ImsService caching functionality.
     */
    @VisibleForTesting
    public ImsServiceInfo getImsServiceInfoFromCache(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        ImsServiceInfo infoFilter = getInfoByPackageName(mInstalledServicesCache, packageName);
        if (infoFilter != null) {
            return infoFilter;
        } else {
            return null;
        }
    }

    // Return the ImsServiceInfo specified for the package name. If the package name is null,
    // get all packages that support ImsServices.
    private List<ImsServiceInfo> getImsServiceInfo(String packageName) {
        List<ImsServiceInfo> infos = new ArrayList<>();
        // Search for Current ImsService implementations
        infos.addAll(searchForImsServices(packageName, mImsServiceControllerFactory));
        // Search for compat ImsService Implementations
        infos.addAll(searchForImsServices(packageName, mImsServiceControllerFactoryCompat));
        return infos;
    }

    private List<ImsServiceInfo> searchForImsServices(String packageName,
            ImsServiceControllerFactory controllerFactory) {
        List<ImsServiceInfo> infos = new ArrayList<>();

        Intent serviceIntent = new Intent(controllerFactory.getServiceInterface());
        serviceIntent.setPackage(packageName);

        PackageManager packageManager = mContext.getPackageManager();
        for (ResolveInfo entry : packageManager.queryIntentServicesAsUser(
                serviceIntent,
                PackageManager.GET_META_DATA,
                UserHandle.of(UserHandle.myUserId()))) {
            ServiceInfo serviceInfo = entry.serviceInfo;

            if (serviceInfo != null) {
                ImsServiceInfo info = new ImsServiceInfo();
                info.name = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                info.controllerFactory = controllerFactory;

                // we will allow the manifest method of declaring manifest features in two cases:
                // 1) it is the device overlay "default" ImsService, where the features do not
                // change (the new method can still be used if the default does not define manifest
                // entries).
                // 2) using the "compat" ImsService, which only supports manifest query.
                if (isDeviceService(info)
                        || mImsServiceControllerFactoryCompat == controllerFactory) {
                    if (serviceInfo.metaData != null) {
                        if (serviceInfo.metaData.getBoolean(METADATA_MMTEL_FEATURE, false)) {
                            info.addFeatureForAllSlots(mNumSlots, ImsFeature.FEATURE_MMTEL);
                            // only allow FEATURE_EMERGENCY_MMTEL if FEATURE_MMTEL is defined.
                            if (serviceInfo.metaData.getBoolean(METADATA_EMERGENCY_MMTEL_FEATURE,
                                    false)) {
                                info.addFeatureForAllSlots(mNumSlots,
                                        ImsFeature.FEATURE_EMERGENCY_MMTEL);
                            }
                        }
                        if (serviceInfo.metaData.getBoolean(METADATA_RCS_FEATURE, false)) {
                            info.addFeatureForAllSlots(mNumSlots, ImsFeature.FEATURE_RCS);
                        }
                    }
                    // Only dynamic query if we are not a compat version of ImsService and the
                    // default service.
                    if (mImsServiceControllerFactoryCompat != controllerFactory
                            && info.getSupportedFeatures().isEmpty()) {
                        // metadata empty, try dynamic query instead
                        info.featureFromMetadata = false;
                    }
                } else {
                    // We are a carrier service and not using the compat version of ImsService.
                    info.featureFromMetadata = false;
                }
                Log.i(TAG, "service name: " + info.name + ", manifest query: "
                        + info.featureFromMetadata);
                // Check manifest permission to be sure that the service declares the correct
                // permissions. Overridden if the METADATA_OVERRIDE_PERM_CHECK metadata is set to
                // true.
                // NOTE: METADATA_OVERRIDE_PERM_CHECK should only be set for testing.
                if (TextUtils.equals(serviceInfo.permission, Manifest.permission.BIND_IMS_SERVICE)
                        || serviceInfo.metaData.getBoolean(METADATA_OVERRIDE_PERM_CHECK, false)) {
                    infos.add(info);
                } else {
                    Log.w(TAG, "ImsService is not protected with BIND_IMS_SERVICE permission: "
                            + info.name);
                }
            }
        }
        return infos;
    }

    // Dump is called on the main thread, since ImsResolver Handler is also handled on main thread,
    // we shouldn't need to worry about concurrent access of private params.
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println("ImsResolver:");
        pw.increaseIndent();
        pw.println("Configurations:");
        pw.increaseIndent();
        pw.println("Device:");
        pw.increaseIndent();
        synchronized (mDeviceServices) {
            for (Integer i : mDeviceServices.keySet()) {
                pw.println(ImsFeature.FEATURE_LOG_MAP.get(i) + " -> " + mDeviceServices.get(i));
            }
        }
        pw.decreaseIndent();
        pw.println("Carrier: ");
        pw.increaseIndent();
        for (int i = 0; i < mNumSlots; i++) {
            for (int j = 0; j < MmTelFeature.FEATURE_MAX; j++) {
                pw.print("slot=");
                pw.print(i);
                pw.print(", feature=");
                pw.print(ImsFeature.FEATURE_LOG_MAP.getOrDefault(j, "?"));
                pw.println(": ");
                pw.increaseIndent();
                String name = getCarrierConfiguredPackageName(i, j);
                pw.println(TextUtils.isEmpty(name) ? "none" : name);
                pw.decreaseIndent();
            }
        }
        pw.decreaseIndent();
        pw.decreaseIndent();
        pw.println("Cached ImsServices:");
        pw.increaseIndent();
        for (ImsServiceInfo i : mInstalledServicesCache.values()) {
            pw.println(i);
        }
        pw.decreaseIndent();
        pw.println("Active controllers:");
        pw.increaseIndent();
        for (ImsServiceController c : mActiveControllers.values()) {
            pw.println(c);
            pw.increaseIndent();
            c.dump(pw);
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
        pw.println("Connection Repository Log:");
        pw.increaseIndent();
        mRepo.dump(pw);
        pw.decreaseIndent();
        pw.println("Event Log:");
        pw.increaseIndent();
        mEventLog.dump(pw);
        pw.decreaseIndent();
    }
}
