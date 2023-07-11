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

import static android.telephony.SubscriptionManager.PLACEHOLDER_SUBSCRIPTION_ID_BASE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ChangedPackages;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.os.UserHandle;
import android.permission.LegacyPermissionManager;
import android.telephony.AnomalyReporter;
import android.telephony.SubscriptionManager;
import android.telephony.ims.ImsService;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.IImsServiceController;
import android.telephony.ims.aidl.ISipTransport;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.stub.ImsFeatureConfiguration;
import android.util.LocalLog;
import android.util.Log;
import android.util.SparseIntArray;

import com.android.ims.ImsFeatureBinderRepository;
import com.android.ims.ImsFeatureContainer;
import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.ExponentialBackoff;
import com.android.internal.telephony.util.TelephonyUtils;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Manages the Binding lifecycle of one ImsService as well as the relevant ImsFeatures that the
 * ImsService will support.
 *
 * When the ImsService is first bound, {@link ImsService#createMmTelFeature(int)} and
 * {@link ImsService#createRcsFeature(int)} will be called
 * on each feature that the service supports. For each ImsFeature that is created,
 * {@link ImsServiceControllerCallbacks#imsServiceFeatureCreated} will be called to notify the
 * listener that the ImsService now supports that feature.
 *
 * When {@link #changeImsServiceFeatures} is called with a set of features that is different from
 * the original set, create*Feature and {@link IImsServiceController#removeImsFeature} will be
 * called for each feature that is created/removed.
 */
public class ImsServiceController {
    private final UUID mAnomalyUUID = UUID.fromString("e93b05e4-6d0a-4755-a6da-a2d2dbfb10d6");
    private int mLastSequenceNumber = 0;
    private ChangedPackages mChangedPackages;
    private PackageManager mPackageManager;
    class ImsServiceConnection implements ServiceConnection {
        // Track the status of whether or not the Service has died in case we need to permanently
        // unbind (see onNullBinding below).
        private boolean mIsServiceConnectionDead = false;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (mLock) {
                mBackoff.stop();
                mIsBound = true;
                mIsBinding = false;
                try {
                    mLocalLog.log("onServiceConnected");
                    Log.d(LOG_TAG, "ImsService(" + name + "): onServiceConnected with binder: "
                            + service);
                    setServiceController(service);
                    notifyImsServiceReady();
                    retrieveStaticImsServiceCapabilities();
                    // create all associated features in the ImsService
                    for (ImsFeatureConfiguration.FeatureSlotPair i : mImsFeatures) {
                        long caps = modifyCapabiltiesForSlot(mImsFeatures, i.slotId,
                                mServiceCapabilities);
                        addImsServiceFeature(i, caps, mSlotIdToSubIdMap.get(i.slotId));
                    }
                } catch (RemoteException e) {
                    mIsBound = false;
                    mIsBinding = false;
                    // RemoteException means that the process holding the binder died or something
                    // unexpected happened... try a full rebind.
                    cleanupConnection();
                    unbindService();
                    startDelayedRebindToService();
                    mLocalLog.log("onConnected exception=" + e.getMessage() + ", retry in "
                            + mBackoff.getCurrentDelay() + " mS");
                    Log.e(LOG_TAG, "ImsService(" + name + ") RemoteException:"
                            + e.getMessage());
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (mLock) {
                mIsBinding = false;
                cleanupConnection();
            }
            mLocalLog.log("onServiceDisconnected");
            Log.w(LOG_TAG, "ImsService(" + name + "): onServiceDisconnected. Waiting...");
            // Service disconnected, but we are still technically bound. Waiting for reconnect.
            checkAndReportAnomaly(name);
        }

        @Override
        public void onBindingDied(ComponentName name) {
            mIsServiceConnectionDead = true;
            synchronized (mLock) {
                mIsBinding = false;
                mIsBound = false;
                // according to the docs, we should fully unbind before rebinding again.
                cleanupConnection();
                unbindService();
                startDelayedRebindToService();
            }
            Log.w(LOG_TAG, "ImsService(" + name + "): onBindingDied. Starting rebind...");
            mLocalLog.log("onBindingDied, retrying in " + mBackoff.getCurrentDelay() + " mS");
        }

        @Override
        public void onNullBinding(ComponentName name) {
            Log.w(LOG_TAG, "ImsService(" + name + "): onNullBinding. Is service dead = "
                    + mIsServiceConnectionDead);
            mLocalLog.log("onNullBinding, is service dead = " + mIsServiceConnectionDead);
            // onNullBinding will happen after onBindingDied. In this case, we should not
            // permanently unbind and instead let the automatic rebind occur.
            if (mIsServiceConnectionDead) return;
            synchronized (mLock) {
                mIsBinding = false;
                // Service connection exists, so we are bound but the binder is null. Wait for
                // ImsResolver to trigger the unbind here.
                mIsBound = true;
                cleanupConnection();
            }
            if (mCallbacks != null) {
                // Will trigger an unbind.
                mCallbacks.imsServiceBindPermanentError(getComponentName());
            }
        }

        // Does not clear feature configuration, just cleans up the active callbacks and
        // invalidates remote FeatureConnections.
        // This should only be called when locked
        private void cleanupConnection() {
            cleanupAllFeatures();
            setServiceController(null);
        }
    }

    /**
     * Defines callbacks that are used by the ImsServiceController to notify when an ImsService
     * has created or removed a new feature as well as the associated ImsServiceController.
     */
    public interface ImsServiceControllerCallbacks {
        /**
         * Called by ImsServiceController when a new MMTEL or RCS feature has been created.
         */
        void imsServiceFeatureCreated(int slotId, int feature, ImsServiceController controller);
        /**
         * Called by ImsServiceController when a new MMTEL or RCS feature has been removed.
         */
        void imsServiceFeatureRemoved(int slotId, int feature, ImsServiceController controller);

        /**
         * Called by the ImsServiceController when the ImsService has notified the framework that
         * its features have changed.
         */
        void imsServiceFeaturesChanged(ImsFeatureConfiguration config,
                ImsServiceController controller);

        /**
         * Called by the ImsServiceController when there has been an error binding that is
         * not recoverable, such as the ImsService returning a null binder.
         */
        void imsServiceBindPermanentError(ComponentName name);
    }

    /**
     * Returns the currently defined rebind retry timeout. Used for testing.
     */
    @VisibleForTesting
    public interface RebindRetry {
        /**
         * Returns a long in ms indicating how long the ImsServiceController should wait before
         * rebinding for the first time.
         */
        long getStartDelay();

        /**
         * Returns a long in ms indicating the maximum time the ImsServiceController should wait
         * before rebinding.
         */
        long getMaximumDelay();
    }

    private static final String LOG_TAG = "ImsServiceController";
    private static final int REBIND_START_DELAY_MS = 2 * 1000; // 2 seconds
    private static final int REBIND_MAXIMUM_DELAY_MS = 60 * 1000; // 1 minute
    private static final long CHANGE_PERMISSION_TIMEOUT_MS = 15 * 1000; // 15 seconds
    // Enforce ImsService has both MMTEL and RCS supported in order to enable SIP transport API.
    // Enable ImsServiceControllerTest and SipDelegateManagerTest cases if this is re-enabled.
    private static final boolean ENFORCE_SINGLE_SERVICE_FOR_SIP_TRANSPORT = false;
    private final ComponentName mComponentName;
    private final HandlerThread mHandlerThread = new HandlerThread("ImsServiceControllerHandler");
    private final LegacyPermissionManager mPermissionManager;
    private ImsFeatureBinderRepository mRepo;
    private ImsServiceControllerCallbacks mCallbacks;
    private ExponentialBackoff mBackoff;

    private boolean mIsBound = false;
    private boolean mIsBinding = false;
    // Set of a pair of slotId->feature
    private Set<ImsFeatureConfiguration.FeatureSlotPair> mImsFeatures;
    private SparseIntArray mSlotIdToSubIdMap;
    private IImsServiceController mIImsServiceController;
    // The Capabilities bitmask of the connected ImsService (see ImsService#ImsServiceCapability).
    private long mServiceCapabilities;
    private ImsServiceConnection mImsServiceConnection;
    // Only added or removed, never accessed on purpose.
    private Set<ImsFeatureStatusCallback> mFeatureStatusCallbacks = new HashSet<>();
    private final LocalLog mLocalLog = new LocalLog(8);

    protected final Object mLock = new Object();
    protected final Context mContext;

    private ImsService.Listener mFeatureChangedListener = new ImsService.Listener() {
        @Override
        public void onUpdateSupportedImsFeatures(ImsFeatureConfiguration c) {
            if (mCallbacks == null) {
                return;
            }
            mLocalLog.log("onUpdateSupportedImsFeatures to " + c.getServiceFeatures());
            mCallbacks.imsServiceFeaturesChanged(c, ImsServiceController.this);
        }
    };

    /**
     * Container class for the IImsFeatureStatusCallback callback implementation. This class is
     * never used directly, but we need to keep track of the IImsFeatureStatusCallback
     * implementations explicitly.
     */
    private class ImsFeatureStatusCallback {
        private int mSlotId;
        private int mFeatureType;

        private final IImsFeatureStatusCallback mCallback = new IImsFeatureStatusCallback.Stub() {

            @Override
            public void notifyImsFeatureStatus(int featureStatus) throws RemoteException {
                Log.i(LOG_TAG, "notifyImsFeatureStatus: slot=" + mSlotId + ", feature="
                        + ImsFeature.FEATURE_LOG_MAP.get(mFeatureType) + ", status="
                        + ImsFeature.STATE_LOG_MAP.get(featureStatus));
                mRepo.notifyFeatureStateChanged(mSlotId, mFeatureType, featureStatus);
            }
        };

        ImsFeatureStatusCallback(int slotId, int featureType) {
            mSlotId = slotId;
            mFeatureType = featureType;
        }

        public IImsFeatureStatusCallback getCallback() {
            return mCallback;
        }
    }

    // Retry the bind to the ImsService that has died after mRebindRetry timeout.
    private Runnable mRestartImsServiceRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mLock) {
                if (mIsBound) {
                    return;
                }
                bind(mImsFeatures, mSlotIdToSubIdMap);
            }
        }
    };

    private RebindRetry mRebindRetry = new RebindRetry() {
        @Override
        public long getStartDelay() {
            return REBIND_START_DELAY_MS;
        }

        @Override
        public long getMaximumDelay() {
            return REBIND_MAXIMUM_DELAY_MS;
        }
    };

    public ImsServiceController(Context context, ComponentName componentName,
            ImsServiceControllerCallbacks callbacks, ImsFeatureBinderRepository repo) {
        mContext = context;
        mComponentName = componentName;
        mCallbacks = callbacks;
        mHandlerThread.start();
        mBackoff = new ExponentialBackoff(
                mRebindRetry.getStartDelay(),
                mRebindRetry.getMaximumDelay(),
                2, /* multiplier */
                mHandlerThread.getLooper(),
                mRestartImsServiceRunnable);
        mPermissionManager = (LegacyPermissionManager) mContext.getSystemService(
                Context.LEGACY_PERMISSION_SERVICE);
        mRepo = repo;

        mPackageManager = mContext.getPackageManager();
        if (mPackageManager != null) {
            mChangedPackages = mPackageManager.getChangedPackages(mLastSequenceNumber);
            if (mChangedPackages != null) {
                mLastSequenceNumber = mChangedPackages.getSequenceNumber();
            }
        }
    }

    @VisibleForTesting
    // Creating a new HandlerThread and background handler for each test causes a segfault, so for
    // testing, use a handler supplied by the testing system.
    public ImsServiceController(Context context, ComponentName componentName,
            ImsServiceControllerCallbacks callbacks, Handler handler, RebindRetry rebindRetry,
            ImsFeatureBinderRepository repo) {
        mContext = context;
        mComponentName = componentName;
        mCallbacks = callbacks;
        mBackoff = new ExponentialBackoff(
                rebindRetry.getStartDelay(),
                rebindRetry.getMaximumDelay(),
                2, /* multiplier */
                handler,
                mRestartImsServiceRunnable);
        mPermissionManager = null;
        mRepo = repo;
    }

    /**
     * Sends request to bind to ImsService designated by the {@link ComponentName} with the feature
     * set imsFeatureSet.
     *
     * @param imsFeatureSet a Set of Pairs that designate the slotId->featureId that need to be
     *                      created once the service is bound.
     * @return {@link true} if the service is in the process of being bound, {@link false} if it
     * has failed.
     */
    public boolean bind(Set<ImsFeatureConfiguration.FeatureSlotPair> imsFeatureSet,
            SparseIntArray  slotIdToSubIdMap) {
        synchronized (mLock) {
            if (!mIsBound && !mIsBinding) {
                mIsBinding = true;
                sanitizeFeatureConfig(imsFeatureSet);
                mImsFeatures = imsFeatureSet;
                mSlotIdToSubIdMap = slotIdToSubIdMap;
                grantPermissionsToService();
                Intent imsServiceIntent = new Intent(getServiceInterface()).setComponent(
                        mComponentName);
                mImsServiceConnection = new ImsServiceConnection();
                int serviceFlags = Context.BIND_AUTO_CREATE | Context.BIND_FOREGROUND_SERVICE
                        | Context.BIND_IMPORTANT;
                mLocalLog.log("binding " + imsFeatureSet);
                Log.i(LOG_TAG, "Binding ImsService:" + mComponentName);
                try {
                    boolean bindSucceeded = mContext.bindService(imsServiceIntent,
                            mImsServiceConnection, serviceFlags);
                    if (!bindSucceeded) {
                        mLocalLog.log("    binding failed, retrying in "
                                + mBackoff.getCurrentDelay() + " mS");
                        mIsBinding = false;
                        mBackoff.notifyFailed();
                    }
                    return bindSucceeded;
                } catch (Exception e) {
                    mBackoff.notifyFailed();
                    mLocalLog.log("    binding exception=" + e.getMessage() + ", retrying in "
                            + mBackoff.getCurrentDelay() + " mS");
                    Log.e(LOG_TAG, "Error binding (" + mComponentName + ") with exception: "
                            + e.getMessage() + ", rebinding in " + mBackoff.getCurrentDelay()
                            + " ms");
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    /**
     * Ensure the feature includes MMTEL when it supports EMERGENCY_MMTEL, if not, remove.
     */
    private void sanitizeFeatureConfig(Set<ImsFeatureConfiguration.FeatureSlotPair> features) {
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

    /**
     * Calls {@link IImsServiceController#removeImsFeature} on all features that the
     * ImsService supports and then unbinds the service.
     */
    public void unbind() throws RemoteException {
        synchronized (mLock) {
            mBackoff.stop();
            // Clean up all features
            changeImsServiceFeatures(new HashSet<>(), mSlotIdToSubIdMap);
            mIsBound = false;
            mIsBinding = false;
            setServiceController(null);
            unbindService();
        }
    }

    /**
     * For every feature that is added, the service calls the associated create. For every
     * ImsFeature that is removed, {@link IImsServiceController#removeImsFeature} is called.
     */
    public void changeImsServiceFeatures(
            Set<ImsFeatureConfiguration.FeatureSlotPair> newImsFeatures,
                    SparseIntArray  slotIdToSubIdMap) throws RemoteException {
        sanitizeFeatureConfig(newImsFeatures);
        synchronized (mLock) {
            HashSet<Integer> slotIDs = newImsFeatures.stream().map(e -> e.slotId).collect(
                    Collectors.toCollection(HashSet::new));
            // detect which subIds have changed on a per-slot basis
            SparseIntArray changedSubIds = new SparseIntArray(slotIDs.size());
            for (Integer slotID : slotIDs) {
                int oldSubId = mSlotIdToSubIdMap.get(slotID, PLACEHOLDER_SUBSCRIPTION_ID_BASE);
                int newSubId = slotIdToSubIdMap.get(slotID);
                if (oldSubId != newSubId) {
                    changedSubIds.put(slotID, newSubId);
                    mLocalLog.log("subId changed for slot: " + slotID + ", " + oldSubId + " -> "
                            + newSubId);
                    Log.i(LOG_TAG, "subId changed for slot: " + slotID + ", " + oldSubId + " -> "
                            + newSubId);
                }
            }
            mSlotIdToSubIdMap = slotIdToSubIdMap;
            // no change, return early.
            if (mImsFeatures.equals(newImsFeatures) && changedSubIds.size() == 0) {
                return;
            }
            mLocalLog.log("Features (" + mImsFeatures + "->" + newImsFeatures + ")");
            Log.i(LOG_TAG, "Features (" + mImsFeatures + "->" + newImsFeatures + ") for "
                    + "ImsService: " + mComponentName);
            HashSet<ImsFeatureConfiguration.FeatureSlotPair> oldImsFeatures =
                    new HashSet<>(mImsFeatures);
            // Set features first in case we lose binding and need to rebind later.
            mImsFeatures = newImsFeatures;
            if (mIsBound) {
                // add features to service.
                HashSet<ImsFeatureConfiguration.FeatureSlotPair> newFeatures =
                        new HashSet<>(mImsFeatures);
                newFeatures.removeAll(oldImsFeatures);
                for (ImsFeatureConfiguration.FeatureSlotPair i : newFeatures) {
                    long caps = modifyCapabiltiesForSlot(mImsFeatures, i.slotId,
                            mServiceCapabilities);
                    addImsServiceFeature(i, caps, mSlotIdToSubIdMap.get(i.slotId));
                }
                // remove old features
                HashSet<ImsFeatureConfiguration.FeatureSlotPair> oldFeatures =
                        new HashSet<>(oldImsFeatures);
                oldFeatures.removeAll(mImsFeatures);
                for (ImsFeatureConfiguration.FeatureSlotPair i : oldFeatures) {
                    removeImsServiceFeature(i, false);
                }
                // ensure the capabilities have been updated for unchanged features.
                HashSet<ImsFeatureConfiguration.FeatureSlotPair> unchangedFeatures =
                        new HashSet<>(mImsFeatures);
                unchangedFeatures.removeAll(oldFeatures);
                unchangedFeatures.removeAll(newFeatures);
                // Go through ImsFeatures whose associated subId have changed and recreate them.
                if (changedSubIds.size() > 0) {
                    for (int slotId : changedSubIds.copyKeys()) {
                        int subId = changedSubIds.get(slotId,
                                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                        HashSet<ImsFeatureConfiguration.FeatureSlotPair>
                                removeAddFeatures = unchangedFeatures.stream()
                                .filter(e -> e.slotId == slotId).collect(
                                        Collectors.toCollection(HashSet::new));
                        for (ImsFeatureConfiguration.FeatureSlotPair i : removeAddFeatures) {
                            removeImsServiceFeature(i, true);
                        }
                        for (ImsFeatureConfiguration.FeatureSlotPair i : removeAddFeatures) {
                            long caps = modifyCapabiltiesForSlot(mImsFeatures, i.slotId,
                                    mServiceCapabilities);
                            addImsServiceFeature(i, caps, subId);
                        }
                        unchangedFeatures.removeAll(removeAddFeatures);
                    }
                }
                for (ImsFeatureConfiguration.FeatureSlotPair p : unchangedFeatures) {
                    long caps = modifyCapabiltiesForSlot(mImsFeatures, p.slotId,
                            mServiceCapabilities);
                    mRepo.notifyFeatureCapabilitiesChanged(p.slotId, p.featureType, caps);
                }
            }
        }
    }

    @VisibleForTesting
    public IImsServiceController getImsServiceController() {
        return mIImsServiceController;
    }

    @VisibleForTesting
    public long getRebindDelay() {
        return mBackoff.getCurrentDelay();
    }

    @VisibleForTesting
    public void stopBackoffTimerForTesting() {
        mBackoff.stop();
    }

    public ComponentName getComponentName() {
        return mComponentName;
    }

    /**
     * Notify ImsService to enable IMS for the framework. This will trigger IMS registration and
     * trigger ImsFeature status updates.
     */
    public void enableIms(int slotId, int subId) {
        try {
            synchronized (mLock) {
                if (isServiceControllerAvailable()) {
                    mIImsServiceController.enableIms(slotId, subId);
                }
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Couldn't enable IMS: " + e.getMessage());
        }
    }

    /**
     * Notify ImsService to disable IMS for the framework. This will trigger IMS de-registration and
     * trigger ImsFeature capability status to become false.
     */
    public void disableIms(int slotId, int subId) {
        try {
            synchronized (mLock) {
                if (isServiceControllerAvailable()) {
                    mIImsServiceController.disableIms(slotId, subId);
                }
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Couldn't disable IMS: " + e.getMessage());
        }
    }

    /**
     * @return the IImsRegistration that corresponds to the slot id specified.
     */
    public IImsRegistration getRegistration(int slotId, int subId) throws RemoteException {
        synchronized (mLock) {
            return isServiceControllerAvailable()
                    ? mIImsServiceController.getRegistration(slotId, subId) : null;
        }
    }

    /**
     * @return the IImsConfig that corresponds to the slot id specified.
     */
    public IImsConfig getConfig(int slotId, int subId) throws RemoteException {
        synchronized (mLock) {
            return isServiceControllerAvailable()
                    ? mIImsServiceController.getConfig(slotId, subId) : null;
        }
    }

    /**
     * @return the ISipTransport instance associated with the requested slot ID.
     */
    public ISipTransport getSipTransport(int slotId) throws RemoteException {
        synchronized (mLock) {
            return isServiceControllerAvailable()
                    ? mIImsServiceController.getSipTransport(slotId) : null;
        }
    }

    protected long getStaticServiceCapabilities() throws RemoteException {
        synchronized (mLock) {
            return isServiceControllerAvailable()
                    ? mIImsServiceController.getImsServiceCapabilities() : 0L;
        }
    }

    /**
     * notify the ImsService that the ImsService is ready for feature creation.
     */
    protected void notifyImsServiceReady() throws RemoteException {
        synchronized (mLock) {
            if (isServiceControllerAvailable()) {
                Log.d(LOG_TAG, "notifyImsServiceReady");
                mIImsServiceController.setListener(mFeatureChangedListener);
                mIImsServiceController.notifyImsServiceReadyForFeatureCreation();
            }
        }
    }

    private void retrieveStaticImsServiceCapabilities() throws RemoteException {
        long caps = getStaticServiceCapabilities();
        Log.i(LOG_TAG, "retrieveStaticImsServiceCapabilities: "
                + ImsService.getCapabilitiesString(caps));
        mLocalLog.log("retrieveStaticImsServiceCapabilities: "
                + ImsService.getCapabilitiesString(caps));
        synchronized (mLock) {
            mServiceCapabilities = caps;
        }
    }

    protected String getServiceInterface() {
        return ImsService.SERVICE_INTERFACE;
    }

    /**
     * Sets the IImsServiceController instance. Overridden by compat layers to set compatibility
     * versions of this service controller.
     */
    protected void setServiceController(IBinder serviceController) {
        mIImsServiceController = IImsServiceController.Stub.asInterface(serviceController);
    }

    /**
     * Check to see if the service controller is available, overridden for compat versions,
     * @return true if available, false otherwise;
     */
    protected boolean isServiceControllerAvailable() {
        return mIImsServiceController != null;
    }

    // Only add a new rebind if there are no pending rebinds waiting.
    private void startDelayedRebindToService() {
        mBackoff.start();
    }

    private void unbindService() {
        synchronized (mLock) {
            if (mImsServiceConnection != null) {
                Log.i(LOG_TAG, "Unbinding ImsService: " + mComponentName);
                mLocalLog.log("unbinding: " + mComponentName);
                mContext.unbindService(mImsServiceConnection);
                mImsServiceConnection = null;
            } else {
                Log.i(LOG_TAG, "unbindService called on already unbound ImsService: "
                        + mComponentName);
                mLocalLog.log("Note: unbindService called with no ServiceConnection on "
                        + mComponentName);
            }
        }
    }

    /**
     * Modify the capabilities returned by the ImsService based on the state of this controller:
     * - CAPABILITY_EMERGENCY_OVER_MMTEL should only be set if features contains
     * FEATURE_EMERGENCY_MMTEL (This is not set by the ImsService itself).
     * - CAPABILITY_SIP_DELEGATE_CREATION should only be set in the case that this ImsService is
     * handling both MMTEL and RCS features for this slot.
     */
    private long modifyCapabiltiesForSlot(
            Set<ImsFeatureConfiguration.FeatureSlotPair> features, int slotId, long serviceCaps) {
        long caps = serviceCaps;
        List<Integer> featureTypes = getFeaturesForSlot(slotId, features);
        if (featureTypes.contains(ImsFeature.FEATURE_EMERGENCY_MMTEL)) {
            // We only consider MMTEL_EMERGENCY as a capability here, so set the capability if
            // the ImsService has declared it.
            caps |= ImsService.CAPABILITY_EMERGENCY_OVER_MMTEL;
        }

        if (ENFORCE_SINGLE_SERVICE_FOR_SIP_TRANSPORT) {
            if (!featureTypes.contains(ImsFeature.FEATURE_MMTEL)
                    || !featureTypes.contains(ImsFeature.FEATURE_RCS)) {
                // Only allow SipDelegate creation if this ImsService is providing both MMTEL and
                // RCS features.
                caps &= ~(ImsService.CAPABILITY_SIP_DELEGATE_CREATION);
            }
        } else {
            Log.i(LOG_TAG, "skipping single service enforce check...");
        }
        return caps;
    }

    // Grant runtime permissions to ImsService. PermissionManager ensures that the ImsService is
    // system/signed before granting permissions.
    private void grantPermissionsToService() {
        mLocalLog.log("grant permissions to " + getComponentName());
        Log.i(LOG_TAG, "Granting Runtime permissions to:" + getComponentName());
        String[] pkgToGrant = {mComponentName.getPackageName()};
        try {
            if (mPermissionManager != null) {
                CountDownLatch latch = new CountDownLatch(1);
                mPermissionManager.grantDefaultPermissionsToEnabledImsServices(
                        pkgToGrant, UserHandle.of(UserHandle.myUserId()), Runnable::run,
                        isSuccess -> {
                            if (isSuccess) {
                                latch.countDown();
                            } else {
                                Log.e(LOG_TAG, "Failed to grant permissions to service.");
                            }
                        });
                TelephonyUtils.waitUntilReady(latch, CHANGE_PERMISSION_TIMEOUT_MS);
            }
        } catch (RuntimeException e) {
            Log.w(LOG_TAG, "Unable to grant permissions, binder died.");
        }
    }

    // This method should only be called when synchronized on mLock
    private void addImsServiceFeature(ImsFeatureConfiguration.FeatureSlotPair featurePair,
            long capabilities, int subId) throws RemoteException {
        if (!isServiceControllerAvailable() || mCallbacks == null) {
            Log.w(LOG_TAG, "addImsServiceFeature called with null values.");
            return;
        }
        if (featurePair.featureType != ImsFeature.FEATURE_EMERGENCY_MMTEL) {
            IInterface f = createImsFeature(
                    featurePair.slotId, subId, featurePair.featureType, capabilities);
            addImsFeatureBinder(featurePair.slotId, subId, featurePair.featureType,
                    f, capabilities);
            addImsFeatureStatusCallback(featurePair.slotId, featurePair.featureType);
        } else {
            // Don't update ImsService for emergency MMTEL feature.
            Log.i(LOG_TAG, "supports emergency calling on slot " + featurePair.slotId);
        }
        // Signal ImsResolver to change supported ImsFeatures for this ImsServiceController
        mCallbacks.imsServiceFeatureCreated(featurePair.slotId, featurePair.featureType, this);
    }

    // This method should only be called when synchronized on mLock
    private void removeImsServiceFeature(ImsFeatureConfiguration.FeatureSlotPair featurePair,
            boolean changeSubId) {
        if (!isServiceControllerAvailable() || mCallbacks == null) {
            Log.w(LOG_TAG, "removeImsServiceFeature called with null values.");
            return;
        }
        // Signal ImsResolver to change supported ImsFeatures for this ImsServiceController
        mCallbacks.imsServiceFeatureRemoved(featurePair.slotId, featurePair.featureType, this);
        if (featurePair.featureType != ImsFeature.FEATURE_EMERGENCY_MMTEL) {
            removeImsFeatureStatusCallback(featurePair.slotId, featurePair.featureType);
            removeImsFeatureBinder(featurePair.slotId, featurePair.featureType);
            try {
                removeImsFeature(featurePair.slotId, featurePair.featureType, changeSubId);
            } catch (RemoteException e) {
                // The connection to this ImsService doesn't exist. This may happen if the service
                // has died and we are removing features.
                Log.i(LOG_TAG, "Couldn't remove feature {"
                        + ImsFeature.FEATURE_LOG_MAP.get(featurePair.featureType)
                        + "}, connection is down: " + e.getMessage());
            }
        } else {
            // Don't update ImsService for emergency MMTEL feature.
            Log.i(LOG_TAG, "doesn't support emergency calling on slot " + featurePair.slotId);
        }
    }

    // This method should only be called when already synchronized on mLock.
    // overridden by compat layer to create features
    protected IInterface createImsFeature(int slotId, int subId, int featureType, long capabilities)
            throws RemoteException {
        switch (featureType) {
            case ImsFeature.FEATURE_MMTEL: {
                if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    boolean emergencyAvailable =
                            (capabilities & ImsService.CAPABILITY_EMERGENCY_OVER_MMTEL) > 0;
                    if (emergencyAvailable) {
                        return mIImsServiceController.createEmergencyOnlyMmTelFeature(slotId);
                    } else {
                        return null;
                    }
                }
                return mIImsServiceController.createMmTelFeature(slotId, subId);
            }
            case ImsFeature.FEATURE_RCS: {
                return mIImsServiceController.createRcsFeature(slotId, subId);
            }
            default:
                return null;
        }
    }

    // This method should only be called when already synchronized on mLock.
    private void addImsFeatureStatusCallback(int slotId, int featureType) throws RemoteException {
        ImsFeatureStatusCallback c = new ImsFeatureStatusCallback(slotId, featureType);
        mFeatureStatusCallbacks.add(c);
        registerImsFeatureStatusCallback(slotId, featureType, c.getCallback());
    }

    // This method should only be called when already synchronized on mLock.
    private void removeImsFeatureStatusCallback(int slotId, int featureType) {
        ImsFeatureStatusCallback callbackToRemove = mFeatureStatusCallbacks.stream().filter(c ->
                c.mSlotId == slotId && c.mFeatureType == featureType).findFirst().orElse(null);
        // Remove status callbacks from list.
        if (callbackToRemove != null) {
            mFeatureStatusCallbacks.remove(callbackToRemove);
            unregisterImsFeatureStatusCallback(slotId, featureType, callbackToRemove.getCallback());
        }
    }

    // overridden by compat layer to register feature status callbacks
    protected void registerImsFeatureStatusCallback(int slotId, int featureType,
            IImsFeatureStatusCallback c) throws RemoteException {
        mIImsServiceController.addFeatureStatusCallback(slotId, featureType, c);
    }

    // overridden by compat layer to deregister feature status callbacks
    protected void unregisterImsFeatureStatusCallback(int slotId, int featureType,
            IImsFeatureStatusCallback c) {
        try {
            mIImsServiceController.removeFeatureStatusCallback(slotId, featureType, c);
        } catch (RemoteException e) {
            mLocalLog.log("unregisterImsFeatureStatusCallback - couldn't remove " + c);
        }
    }


    // overridden by compat layer to remove features
    protected void removeImsFeature(int slotId, int featureType, boolean changeSubId)
            throws RemoteException {
        mIImsServiceController.removeImsFeature(slotId, featureType, changeSubId);
    }

    private void addImsFeatureBinder(int slotId, int subId, int featureType, IInterface b,
            long capabilities)
            throws RemoteException {
        if (b == null) {

            Log.w(LOG_TAG, "addImsFeatureBinder: null IInterface reported for "
                    + ImsFeature.FEATURE_LOG_MAP.get(featureType));
            mLocalLog.log("addImsFeatureBinder: null IInterface reported for "
                    + ImsFeature.FEATURE_LOG_MAP.get(featureType));
            return;
        }
        ImsFeatureContainer fc = createFeatureContainer(slotId, subId, b.asBinder(), capabilities);
        mRepo.addConnection(slotId, subId, featureType, fc);
    }

    private void removeImsFeatureBinder(int slotId, int featureType) {
        mRepo.removeConnection(slotId, featureType);
    }

    private ImsFeatureContainer createFeatureContainer(int slotId, int subId, IBinder b,
            long capabilities)
            throws RemoteException {
        IImsConfig config = getConfig(slotId, subId);
        IImsRegistration reg = getRegistration(slotId, subId);
        // When either is null, this is an unexpected condition. Do not report the ImsService as
        // being available.
        if (config == null || reg == null) {
            Log.w(LOG_TAG, "createFeatureContainer: invalid state. Reporting as not "
                    + "available. componentName= " + getComponentName());
            mLocalLog.log("createFeatureContainer: invalid state. Reporting as not "
                    + "available.");
            return null;
        }
        // SipTransport AIDL may be null for older devices, this is expected.
        ISipTransport transport = getSipTransport(slotId);
        return new ImsFeatureContainer(b, config, reg, transport, capabilities);
    }

    private List<Integer> getFeaturesForSlot(int slotId,
            Set<ImsFeatureConfiguration.FeatureSlotPair> features) {
        return features.stream().filter(f -> f.slotId == slotId).map(f -> f.featureType)
                .collect(Collectors.toList());
    }

    private void cleanupAllFeatures() {
        synchronized (mLock) {
            // Remove all features and clean up all associated Binders.
            for (ImsFeatureConfiguration.FeatureSlotPair i : mImsFeatures) {
                removeImsServiceFeature(i, false);
            }
        }
    }

    private void checkAndReportAnomaly(ComponentName name) {
        if (mPackageManager == null) {
            Log.w(LOG_TAG, "mPackageManager null");
            return;
        }
        ChangedPackages curChangedPackages =
                            mPackageManager.getChangedPackages(mLastSequenceNumber);
        if (curChangedPackages != null) {
            mLastSequenceNumber = curChangedPackages.getSequenceNumber();
            List<String> packagesNames = curChangedPackages.getPackageNames();
            if (packagesNames.contains(name.getPackageName())) {
                Log.d(LOG_TAG, "Ignore due to updated, package: " + name.getPackageName());
                return;
            }
        }
        String message = "IMS Service Crashed";
        AnomalyReporter.reportAnomaly(mAnomalyUUID, message);
    }

    @Override
    public String toString() {
        synchronized (mLock) {
            return "[ImsServiceController: componentName=" + getComponentName() + ", features="
                    + mImsFeatures + ", isBinding=" + mIsBinding + ", isBound=" + mIsBound
                    + ", serviceController=" + getImsServiceController() + ", rebindDelay="
                    + getRebindDelay() + "]";
        }
    }

    public void dump(PrintWriter printWriter) {
        mLocalLog.dump(printWriter);
    }
}
