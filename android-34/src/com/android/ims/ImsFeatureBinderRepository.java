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

package com.android.ims;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.RemoteException;
import android.telephony.ims.ImsService;
import android.telephony.ims.feature.ImsFeature;
import android.util.LocalLog;
import android.util.Log;

import com.android.ims.internal.IImsServiceFeatureCallback;
import com.android.internal.annotations.GuardedBy;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * A repository of ImsFeature connections made available by an ImsService once it has been
 * successfully bound.
 *
 * Provides the ability for listeners to register callbacks and the repository notify registered
 * listeners when a connection has been created/removed for a specific connection type.
 */
public class ImsFeatureBinderRepository {

    private static final String TAG = "ImsFeatureBinderRepo";

    /**
     * Internal class representing a listener that is listening for changes to specific
     * ImsFeature instances.
     */
    private static class ListenerContainer {
        private final IImsServiceFeatureCallback mCallback;
        private final Executor mExecutor;

        public ListenerContainer(@NonNull IImsServiceFeatureCallback c, @NonNull Executor e) {
            mCallback = c;
            mExecutor = e;
        }

        public void notifyFeatureCreatedOrRemoved(ImsFeatureContainer connector, int subId) {
            if (connector == null) {
                mExecutor.execute(() -> {
                    try {
                        mCallback.imsFeatureRemoved(
                                FeatureConnector.UNAVAILABLE_REASON_DISCONNECTED);
                    } catch (RemoteException e) {
                        // This listener will eventually be caught and removed during stale checks.
                    }
                });
            }
            else {
                mExecutor.execute(() -> {
                    try {
                        mCallback.imsFeatureCreated(connector, subId);
                    } catch (RemoteException e) {
                        // This listener will eventually be caught and removed during stale checks.
                    }
                });
            }
        }

        public void notifyStateChanged(int state, int subId) {
            mExecutor.execute(() -> {
                try {
                    mCallback.imsStatusChanged(state, subId);
                } catch (RemoteException e) {
                    // This listener will eventually be caught and removed during stale checks.
                }
            });
        }

        public void notifyUpdateCapabilties(long caps) {
            mExecutor.execute(() -> {
                try {
                    mCallback.updateCapabilities(caps);
                } catch (RemoteException e) {
                    // This listener will eventually be caught and removed during stale checks.
                }
            });
        }

        public boolean isStale() {
            return !mCallback.asBinder().isBinderAlive();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ListenerContainer that = (ListenerContainer) o;
            // Do not count executor for equality.
            return mCallback.equals(that.mCallback);
        }

        @Override
        public int hashCode() {
            // Do not use executor for hash.
            return Objects.hash(mCallback);
        }

        @Override
        public String toString() {
            return "ListenerContainer{" + "cb=" + mCallback + '}';
        }
    }

    /**
     * Contains the mapping from ImsFeature type (MMTEL/RCS) to List of listeners listening for
     * updates to the ImsFeature instance contained in the ImsFeatureContainer.
     */
    private static final class UpdateMapper {
        public final int phoneId;
        public int subId;
        public final @ImsFeature.FeatureType int imsFeatureType;
        private final List<ListenerContainer> mListeners = new ArrayList<>();
        private ImsFeatureContainer mFeatureContainer;
        private final Object mLock = new Object();


        public UpdateMapper(int pId, @ImsFeature.FeatureType int t) {
            phoneId = pId;
            imsFeatureType = t;
        }

        public void addFeatureContainer(ImsFeatureContainer c) {
            List<ListenerContainer> listeners;
            synchronized (mLock) {
                if (Objects.equals(c, mFeatureContainer)) return;
                mFeatureContainer = c;
                listeners = copyListenerList(mListeners);
            }
            listeners.forEach(l -> l.notifyFeatureCreatedOrRemoved(mFeatureContainer, subId));
        }

        public ImsFeatureContainer removeFeatureContainer() {
            ImsFeatureContainer oldContainer;
            List<ListenerContainer> listeners;
            synchronized (mLock) {
                if (mFeatureContainer == null) return null;
                oldContainer = mFeatureContainer;
                mFeatureContainer = null;
                listeners = copyListenerList(mListeners);
            }
            listeners.forEach(l -> l.notifyFeatureCreatedOrRemoved(mFeatureContainer, subId));
            return oldContainer;
        }

        public ImsFeatureContainer getFeatureContainer() {
            synchronized(mLock) {
                return mFeatureContainer;
            }
        }

        public void addListener(ListenerContainer c) {
            ImsFeatureContainer featureContainer;
            synchronized (mLock) {
                removeStaleListeners();
                if (mListeners.contains(c)) {
                    return;
                }
                featureContainer = mFeatureContainer;
                mListeners.add(c);
            }
            // Do not call back until the feature container has been set.
            if (featureContainer != null) {
                c.notifyFeatureCreatedOrRemoved(featureContainer, subId);
            }
        }

        public void removeListener(IImsServiceFeatureCallback callback) {
            synchronized (mLock) {
                removeStaleListeners();
                List<ListenerContainer> oldListeners = mListeners.stream()
                        .filter((c) -> Objects.equals(c.mCallback, callback))
                        .collect(Collectors.toList());
                mListeners.removeAll(oldListeners);
            }
        }

        public void notifyStateUpdated(int newState) {
            ImsFeatureContainer featureContainer;
            List<ListenerContainer> listeners;
            synchronized (mLock) {
                removeStaleListeners();
                featureContainer = mFeatureContainer;
                listeners = copyListenerList(mListeners);
                if (mFeatureContainer != null) {
                    if (mFeatureContainer.getState() != newState) {
                        mFeatureContainer.setState(newState);
                    }
                }
            }
            // Only update if the feature container is set.
            if (featureContainer != null) {
                listeners.forEach(l -> l.notifyStateChanged(newState, subId));
            }
        }

        public void notifyUpdateCapabilities(long caps) {
            ImsFeatureContainer featureContainer;
            List<ListenerContainer> listeners;
            synchronized (mLock) {
                removeStaleListeners();
                featureContainer = mFeatureContainer;
                listeners = copyListenerList(mListeners);
                if (mFeatureContainer != null) {
                    if (mFeatureContainer.getCapabilities() != caps) {
                        mFeatureContainer.setCapabilities(caps);
                    }
                }
            }
            // Only update if the feature container is set.
            if (featureContainer != null) {
                listeners.forEach(l -> l.notifyUpdateCapabilties(caps));
            }
        }

        public void updateSubId(int newSubId) {
            subId = newSubId;
        }

        @GuardedBy("mLock")
        private void removeStaleListeners() {
            List<ListenerContainer> staleListeners = mListeners.stream().filter(
                    ListenerContainer::isStale)
                    .collect(Collectors.toList());
            mListeners.removeAll(staleListeners);
        }

        @Override
        public String toString() {
            synchronized (mLock) {
                return "UpdateMapper{" + "phoneId=" + phoneId + ", type="
                        + ImsFeature.FEATURE_LOG_MAP.get(imsFeatureType) + ", container="
                        + mFeatureContainer + '}';
            }
        }


        private List<ListenerContainer> copyListenerList(List<ListenerContainer> listeners) {
            return new ArrayList<>(listeners);
        }
    }

    private final List<UpdateMapper> mFeatures = new ArrayList<>();
    private final LocalLog mLocalLog = new LocalLog(50 /*lines*/);

    public ImsFeatureBinderRepository() {
        logInfoLineLocked(-1, "FeatureConnectionRepository - created");
    }

    /**
     * Get the Container for a specific ImsFeature now if it exists.
     *
     * @param phoneId The phone ID that the connection is related to.
     * @param type The ImsFeature type to get the cotnainr for (MMTEL/RCS).
     * @return The Container containing the requested ImsFeature if it exists.
     */
    public Optional<ImsFeatureContainer> getIfExists(
            int phoneId, @ImsFeature.FeatureType int type) {
        if (type < 0 || type >= ImsFeature.FEATURE_MAX) {
            throw new IllegalArgumentException("Incorrect feature type");
        }
        UpdateMapper m;
        m = getUpdateMapper(phoneId, type);
        ImsFeatureContainer c = m.getFeatureContainer();
        logVerboseLineLocked(phoneId, "getIfExists, type= " + ImsFeature.FEATURE_LOG_MAP.get(type)
                + ", result= " + c);
        return Optional.ofNullable(c);
    }

    /**
     * Register a callback that will receive updates when the requested ImsFeature type becomes
     * available or unavailable for the specified phone ID.
     * <p>
     * This callback will not be called the first time until there is a valid ImsFeature.
     * @param phoneId The phone ID that the connection will be related to.
     * @param type The ImsFeature type to get (MMTEL/RCS).
     * @param callback The callback that will be used to notify when the callback is
     *                 available/unavailable.
     * @param executor The executor that the callback will be run on.
     */
    public void registerForConnectionUpdates(int phoneId,
            @ImsFeature.FeatureType int type, @NonNull IImsServiceFeatureCallback callback,
            @NonNull Executor executor) {
        if (type < 0 || type >= ImsFeature.FEATURE_MAX || callback == null || executor == null) {
            throw new IllegalArgumentException("One or more invalid arguments have been passed in");
        }
        ListenerContainer container = new ListenerContainer(callback, executor);
        logInfoLineLocked(phoneId, "registerForConnectionUpdates, type= "
                + ImsFeature.FEATURE_LOG_MAP.get(type) +", conn= " + container);
        UpdateMapper m = getUpdateMapper(phoneId, type);
        m.addListener(container);
    }

    /**
     * Unregister for updates on a previously registered callback.
     *
     * @param callback The callback to unregister.
     */
    public void unregisterForConnectionUpdates(@NonNull IImsServiceFeatureCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("this method does not accept null arguments");
        }
        logInfoLineLocked(-1, "unregisterForConnectionUpdates, callback= " + callback);
        synchronized (mFeatures) {
            for (UpdateMapper m : mFeatures) {
                // warning: no callbacks should be called while holding locks
                m.removeListener(callback);
            }
        }
    }

    /**
     * Add a Container containing the IBinder interfaces associated with a specific ImsFeature type
     * (MMTEL/RCS). If one already exists, it will be replaced. This will notify listeners of the
     * change.
     * @param phoneId The phone ID associated with this Container.
     * @param type The ImsFeature type to get (MMTEL/RCS).
     * @param newConnection A Container containing the IBinder interface connections associated with
     *                      the ImsFeature type.
     */
    public void addConnection(int phoneId, int subId, @ImsFeature.FeatureType int type,
            @Nullable ImsFeatureContainer newConnection) {
        if (type < 0 || type >= ImsFeature.FEATURE_MAX) {
            throw new IllegalArgumentException("The type must valid");
        }
        logInfoLineLocked(phoneId, "addConnection, subId=" + subId + ", type="
                + ImsFeature.FEATURE_LOG_MAP.get(type) + ", conn=" + newConnection);
        UpdateMapper m = getUpdateMapper(phoneId, type);
        m.updateSubId(subId);
        m.addFeatureContainer(newConnection);
    }

    /**
     * Remove the IBinder Container associated with a specific ImsService type. Listeners will be
     * notified of this change.
     * @param phoneId The phone ID associated with this connection.
     * @param type The ImsFeature type to get (MMTEL/RCS).
     */
    public ImsFeatureContainer removeConnection(int phoneId, @ImsFeature.FeatureType int type) {
        if (type < 0 || type >= ImsFeature.FEATURE_MAX) {
            throw new IllegalArgumentException("The type must valid");
        }
        logInfoLineLocked(phoneId, "removeConnection, type="
                + ImsFeature.FEATURE_LOG_MAP.get(type));
        UpdateMapper m = getUpdateMapper(phoneId, type);
        return m.removeFeatureContainer();
    }

    /**
     * Notify listeners that the state of a specific ImsFeature that this repository is
     * tracking has changed. Listeners will be notified of the change in the ImsFeature's state.
     * @param phoneId The phoneId of the feature that has changed state.
     * @param type The ImsFeature type to get (MMTEL/RCS).
     * @param state The new state of the ImsFeature
     */
    public void notifyFeatureStateChanged(int phoneId, @ImsFeature.FeatureType int type,
            @ImsFeature.ImsState int state) {
        logInfoLineLocked(phoneId, "notifyFeatureStateChanged, type="
                + ImsFeature.FEATURE_LOG_MAP.get(type) + ", state="
                + ImsFeature.STATE_LOG_MAP.get(state));
        UpdateMapper m = getUpdateMapper(phoneId, type);
        m.notifyStateUpdated(state);
    }

    /**
     *  Notify listeners that the capabilities of a specific ImsFeature that this repository is
     * tracking has changed. Listeners will be notified of the change in the ImsFeature's
     * capabilities.
     * @param phoneId The phoneId of the feature that has changed capabilities.
     * @param type The ImsFeature type to get (MMTEL/RCS).
     * @param capabilities The new capabilities of the ImsFeature
     */
    public void notifyFeatureCapabilitiesChanged(int phoneId, @ImsFeature.FeatureType int type,
            @ImsService.ImsServiceCapability long capabilities) {
        logInfoLineLocked(phoneId, "notifyFeatureCapabilitiesChanged, type="
                + ImsFeature.FEATURE_LOG_MAP.get(type) + ", caps="
                + ImsService.getCapabilitiesString(capabilities));
        UpdateMapper m = getUpdateMapper(phoneId, type);
        m.notifyUpdateCapabilities(capabilities);
    }

    /**
     * Prints the dump of log events that have occurred on this repository.
     */
    public void dump(PrintWriter printWriter) {
        synchronized (mLocalLog) {
            mLocalLog.dump(printWriter);
        }
    }

    private UpdateMapper getUpdateMapper(int phoneId, int type) {
        synchronized (mFeatures) {
            UpdateMapper mapper = mFeatures.stream()
                    .filter((c) -> ((c.phoneId == phoneId) && (c.imsFeatureType == type)))
                    .findFirst().orElse(null);
            if (mapper == null) {
                mapper = new UpdateMapper(phoneId, type);
                mFeatures.add(mapper);
            }
            return mapper;
        }
    }

    private void logVerboseLineLocked(int phoneId, String log) {
        if (!Log.isLoggable(TAG, Log.VERBOSE)) return;
        final String phoneIdPrefix = "[" + phoneId + "] ";
        Log.v(TAG, phoneIdPrefix + log);
        synchronized (mLocalLog) {
            mLocalLog.log(phoneIdPrefix + log);
        }
    }

    private void logInfoLineLocked(int phoneId, String log) {
        final String phoneIdPrefix = "[" + phoneId + "] ";
        Log.i(TAG, phoneIdPrefix + log);
        synchronized (mLocalLog) {
            mLocalLog.log(phoneIdPrefix + log);
        }
    }
}
