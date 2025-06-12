// Copyright 2012 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.net;

import android.annotation.SuppressLint;

import androidx.annotation.VisibleForTesting;

import org.jni_zero.CalledByNative;
import org.jni_zero.JNINamespace;
import org.jni_zero.NativeClassQualifiedName;
import org.jni_zero.NativeMethods;

import org.chromium.base.ObserverList;
import org.chromium.base.metrics.ScopedSysTraceEvent;

import java.util.ArrayList;

/**
 * Triggers updates to the underlying network state in Chrome.
 *
 * By default, connectivity is assumed and changes must be pushed from the embedder via the
 * forceConnectivityState function.
 * Embedders may choose to have this class auto-detect changes in network connectivity by invoking
 * the setAutoDetectConnectivityState function.
 *
 * WARNING: This class is not thread-safe.
 */
@JNINamespace("net")
public class NetworkChangeNotifier {
    /**
     * Alerted when the connection type of the network changes.
     * The alert is fired on the UI thread.
     */
    public interface ConnectionTypeObserver {
        public void onConnectionTypeChanged(int connectionType);
    }

    private final ArrayList<Long> mNativeChangeNotifiers;
    private final ObserverList<ConnectionTypeObserver> mConnectionTypeObservers;
    private NetworkChangeNotifierAutoDetect mAutoDetector;
    // Last value broadcast via ConnectionTypeChange signal.
    private int mCurrentConnectionType = ConnectionType.CONNECTION_UNKNOWN;
    // Last value broadcast via ConnectionCostChange signal.
    private int mCurrentConnectionCost = ConnectionCost.UNKNOWN;

    @SuppressLint("StaticFieldLeak")
    private static NetworkChangeNotifier sInstance;

    @VisibleForTesting
    protected NetworkChangeNotifier() {
        mNativeChangeNotifiers = new ArrayList<Long>();
        mConnectionTypeObservers = new ObserverList<ConnectionTypeObserver>();
    }

    /** Initializes the singleton once. */
    @CalledByNative
    public static NetworkChangeNotifier init() {
        if (sInstance == null) {
            sInstance = new NetworkChangeNotifier();
        }
        return sInstance;
    }

    public static boolean isInitialized() {
        return sInstance != null;
    }

    public static void resetInstanceForTests() {
        sInstance = new NetworkChangeNotifier();
    }

    public static void resetInstanceForTests(NetworkChangeNotifier notifier) {
        sInstance = notifier;
    }

    @CalledByNative
    public int getCurrentConnectionType() {
        return mCurrentConnectionType;
    }

    @CalledByNative
    public int getCurrentConnectionSubtype() {
        try (ScopedSysTraceEvent event =
                ScopedSysTraceEvent.scoped("NetworkChangeNotifier.getCurrentConnectionSubtype")) {
            if (mAutoDetector == null) return ConnectionSubtype.SUBTYPE_UNKNOWN;

            mAutoDetector.updateCurrentNetworkState();
            return mAutoDetector.getCurrentNetworkState().getConnectionSubtype();
        }
    }

    @CalledByNative
    public int getCurrentConnectionCost() {
        return mCurrentConnectionCost;
    }

    /**
     * Returns NetID of device's current default connected network used for
     * communication. Only available when auto-detection has been enabled,
     * returns NetId.INVALID otherwise.
     */
    @CalledByNative
    public long getCurrentDefaultNetId() {
        return mAutoDetector == null ? NetId.INVALID : mAutoDetector.getDefaultNetId();
    }

    /**
     * Returns an array of all of the device's currently connected
     * networks and ConnectionTypes. Array elements are a repeated sequence of:
     *   NetID of network
     *   ConnectionType of network
     * Only available when auto-detection has been enabled.
     */
    @CalledByNative
    public long[] getCurrentNetworksAndTypes() {
        try (ScopedSysTraceEvent event =
                ScopedSysTraceEvent.scoped(
                        "NetworkChangeNotifierAutoDetect.getCurrentNetworksAndTypes")) {
            return mAutoDetector == null ? new long[0] : mAutoDetector.getNetworksAndTypes();
        }
    }

    /** Adds a native-side observer. */
    @CalledByNative
    public void addNativeObserver(long nativeChangeNotifier) {
        mNativeChangeNotifiers.add(nativeChangeNotifier);
    }

    /** Removes a native-side observer. */
    @CalledByNative
    public void removeNativeObserver(long nativeChangeNotifier) {
        mNativeChangeNotifiers.remove(nativeChangeNotifier);
    }

    /**
     * Returns {@code true} if NetworkCallback failed to register, indicating that network-specific
     * callbacks will not be issued.
     */
    @CalledByNative
    public boolean registerNetworkCallbackFailed() {
        return mAutoDetector == null ? false : mAutoDetector.registerNetworkCallbackFailed();
    }

    /** Returns the singleton instance. */
    public static NetworkChangeNotifier getInstance() {
        assert sInstance != null;
        return sInstance;
    }

    /**
     * Enables auto detection of the current network state based on notifications from the system.
     * Note that passing true here requires the embedding app have the platform ACCESS_NETWORK_STATE
     * permission. Also note that in this case the auto detection is enabled based on the status of
     * the application (@see ApplicationStatus).
     * Declare @CalledByNative only for testing.
     *
     * @param shouldAutoDetect true if the NetworkChangeNotifier should listen for system changes in
     *    network connectivity.
     */
    @CalledByNative
    public static void setAutoDetectConnectivityState(boolean shouldAutoDetect) {
        getInstance()
                .setAutoDetectConnectivityStateInternal(
                        shouldAutoDetect, new RegistrationPolicyApplicationStatus());
    }

    /**
     * Registers to always receive network change notifications no matter if
     * the app is in the background or foreground.
     * Note that in normal circumstances, chrome embedders should use
     * {@code setAutoDetectConnectivityState} to listen to network changes only
     * when the app is in the foreground, because network change observers
     * might perform expensive work depending on the network connectivity.
     */
    public static void registerToReceiveNotificationsAlways() {
        getInstance()
                .setAutoDetectConnectivityStateInternal(
                        true, new RegistrationPolicyAlwaysRegister());
    }

    /**
     * Registers to receive network change notification based on the provided registration policy.
     * By default, queries current network state from the system after creating the
     * NetworkChangeNotifierAutoDetect instance. Callers can override this by passing
     * forceUpdateNetworkState = false to speed up the execution.
     */
    public static void setAutoDetectConnectivityState(
            NetworkChangeNotifierAutoDetect.RegistrationPolicy policy,
            boolean forceUpdateNetworkState) {
        getInstance().setAutoDetectConnectivityStateInternal(true, policy, forceUpdateNetworkState);
    }

    private void destroyAutoDetector() {
        if (mAutoDetector != null) {
            mAutoDetector.destroy();
            mAutoDetector = null;
        }
    }

    private void setAutoDetectConnectivityStateInternal(
            boolean shouldAutoDetect, NetworkChangeNotifierAutoDetect.RegistrationPolicy policy) {
        setAutoDetectConnectivityStateInternal(
                shouldAutoDetect, policy, /* forceUpdateNetworkState= */ true);
    }

    private void setAutoDetectConnectivityStateInternal(
            boolean shouldAutoDetect,
            NetworkChangeNotifierAutoDetect.RegistrationPolicy policy,
            boolean forceUpdateNetworkState) {
        try (ScopedSysTraceEvent event =
                ScopedSysTraceEvent.scoped(
                        "NetworkChangeNotifier.setAutoDetectConnectivityStateInternal")) {
            if (shouldAutoDetect) {
                if (mAutoDetector == null) {
                    mAutoDetector =
                            new NetworkChangeNotifierAutoDetect(
                                    new NetworkChangeNotifierAutoDetect.Observer() {
                                        @Override
                                        public void onConnectionTypeChanged(int newConnectionType) {
                                            updateCurrentConnectionType(newConnectionType);
                                        }

                                        @Override
                                        public void onConnectionCostChanged(int newConnectionCost) {
                                            notifyObserversOfConnectionCostChange(
                                                    newConnectionCost);
                                        }

                                        @Override
                                        public void onConnectionSubtypeChanged(
                                                int newConnectionSubtype) {
                                            notifyObserversOfConnectionSubtypeChange(
                                                    newConnectionSubtype);
                                        }

                                        @Override
                                        public void onNetworkConnect(
                                                long netId, int connectionType) {
                                            notifyObserversOfNetworkConnect(netId, connectionType);
                                        }

                                        @Override
                                        public void onNetworkSoonToDisconnect(long netId) {
                                            notifyObserversOfNetworkSoonToDisconnect(netId);
                                        }

                                        @Override
                                        public void onNetworkDisconnect(long netId) {
                                            notifyObserversOfNetworkDisconnect(netId);
                                        }

                                        @Override
                                        public void purgeActiveNetworkList(long[] activeNetIds) {
                                            notifyObserversToPurgeActiveNetworkList(activeNetIds);
                                        }
                                    },
                                    policy);
                    if (forceUpdateNetworkState) mAutoDetector.updateCurrentNetworkState();

                    final NetworkChangeNotifierAutoDetect.NetworkState networkState =
                            mAutoDetector.getCurrentNetworkState();

                    updateCurrentConnectionType(networkState.getConnectionType());
                    updateCurrentConnectionCost(networkState.getConnectionCost());
                    notifyObserversOfConnectionSubtypeChange(networkState.getConnectionSubtype());
                }
            } else {
                destroyAutoDetector();
            }
        }
    }

    /**
     * For testing, updates the perceived network state when not auto-detecting changes to
     * connectivity.
     *
     * @param networkAvailable True if the NetworkChangeNotifier should perceive a "connected"
     *    state, false implies "disconnected".
     */
    @CalledByNative
    public static void forceConnectivityState(boolean networkAvailable) {
        setAutoDetectConnectivityState(false);
        getInstance().forceConnectivityStateInternal(networkAvailable);
    }

    private void forceConnectivityStateInternal(boolean forceOnline) {
        boolean connectionCurrentlyExists =
                mCurrentConnectionType != ConnectionType.CONNECTION_NONE;
        if (connectionCurrentlyExists != forceOnline) {
            updateCurrentConnectionType(
                    forceOnline
                            ? ConnectionType.CONNECTION_UNKNOWN
                            : ConnectionType.CONNECTION_NONE);
            notifyObserversOfConnectionSubtypeChange(
                    forceOnline
                            ? ConnectionSubtype.SUBTYPE_UNKNOWN
                            : ConnectionSubtype.SUBTYPE_NONE);
        }
    }

    // For testing, pretend a network connected.
    @CalledByNative
    public static void fakeNetworkConnected(long netId, int connectionType) {
        setAutoDetectConnectivityState(false);
        getInstance().notifyObserversOfNetworkConnect(netId, connectionType);
    }

    // For testing, pretend a network will soon disconnect.
    @CalledByNative
    public static void fakeNetworkSoonToBeDisconnected(long netId) {
        setAutoDetectConnectivityState(false);
        getInstance().notifyObserversOfNetworkSoonToDisconnect(netId);
    }

    // For testing, pretend a network disconnected.
    @CalledByNative
    public static void fakeNetworkDisconnected(long netId) {
        setAutoDetectConnectivityState(false);
        getInstance().notifyObserversOfNetworkDisconnect(netId);
    }

    // For testing, pretend a network lists should be purged.
    @CalledByNative
    public static void fakePurgeActiveNetworkList(long[] activeNetIds) {
        setAutoDetectConnectivityState(false);
        getInstance().notifyObserversToPurgeActiveNetworkList(activeNetIds);
    }

    // For testing, pretend a default network changed.
    @CalledByNative
    public static void fakeDefaultNetwork(long netId, int connectionType) {
        setAutoDetectConnectivityState(false);
        getInstance().notifyObserversOfConnectionTypeChange(connectionType, netId);
    }

    // For testing, pretend the connection cost has changed.
    @CalledByNative
    @VisibleForTesting
    public static void fakeConnectionCostChanged(int connectionCost) {
        setAutoDetectConnectivityState(false);
        getInstance().notifyObserversOfConnectionCostChange(connectionCost);
    }

    // For testing, pretend the connection subtype has changed.
    @CalledByNative
    public static void fakeConnectionSubtypeChanged(int connectionSubtype) {
        setAutoDetectConnectivityState(false);
        getInstance().notifyObserversOfConnectionSubtypeChange(connectionSubtype);
    }

    private void updateCurrentConnectionType(int newConnectionType) {
        mCurrentConnectionType = newConnectionType;
        notifyObserversOfConnectionTypeChange(newConnectionType);
    }

    /** Alerts all observers of a connection change. */
    void notifyObserversOfConnectionTypeChange(int newConnectionType) {
        notifyObserversOfConnectionTypeChange(newConnectionType, getCurrentDefaultNetId());
    }

    private void notifyObserversOfConnectionTypeChange(int newConnectionType, long defaultNetId) {
        for (Long nativeChangeNotifier : mNativeChangeNotifiers) {
            NetworkChangeNotifierJni.get()
                    .notifyConnectionTypeChanged(
                            nativeChangeNotifier,
                            NetworkChangeNotifier.this,
                            newConnectionType,
                            defaultNetId);
        }
        for (ConnectionTypeObserver observer : mConnectionTypeObservers) {
            observer.onConnectionTypeChanged(newConnectionType);
        }
    }

    private void updateCurrentConnectionCost(int newConnectionCost) {
        mCurrentConnectionCost = newConnectionCost;
        notifyObserversOfConnectionCostChange(newConnectionCost);
    }

    /** Alerts all observers of a connection cost change. */
    void notifyObserversOfConnectionCostChange(int newConnectionCost) {
        for (Long nativeChangeNotifier : mNativeChangeNotifiers) {
            NetworkChangeNotifierJni.get()
                    .notifyConnectionCostChanged(
                            nativeChangeNotifier, NetworkChangeNotifier.this, newConnectionCost);
        }
    }

    /** Alerts all observers of a bandwidth change. */
    void notifyObserversOfConnectionSubtypeChange(int connectionSubtype) {
        for (Long nativeChangeNotifier : mNativeChangeNotifiers) {
            NetworkChangeNotifierJni.get()
                    .notifyConnectionSubtypeChanged(
                            nativeChangeNotifier, NetworkChangeNotifier.this, connectionSubtype);
        }
    }

    /** Alerts all observers of a network connect. */
    void notifyObserversOfNetworkConnect(long netId, int connectionType) {
        for (Long nativeChangeNotifier : mNativeChangeNotifiers) {
            NetworkChangeNotifierJni.get()
                    .notifyOfNetworkConnect(
                            nativeChangeNotifier,
                            NetworkChangeNotifier.this,
                            netId,
                            connectionType);
        }
    }

    /** Alerts all observers of a network soon to be disconnected. */
    void notifyObserversOfNetworkSoonToDisconnect(long netId) {
        for (Long nativeChangeNotifier : mNativeChangeNotifiers) {
            NetworkChangeNotifierJni.get()
                    .notifyOfNetworkSoonToDisconnect(
                            nativeChangeNotifier, NetworkChangeNotifier.this, netId);
        }
    }

    /** Alerts all observers of a network disconnect. */
    void notifyObserversOfNetworkDisconnect(long netId) {
        for (Long nativeChangeNotifier : mNativeChangeNotifiers) {
            NetworkChangeNotifierJni.get()
                    .notifyOfNetworkDisconnect(
                            nativeChangeNotifier, NetworkChangeNotifier.this, netId);
        }
    }

    /**
     * Alerts all observers to purge cached lists of active networks, of any
     * networks not in the accompanying list of active networks. This is
     * issued if a period elapsed where disconnected notifications may have
     * been missed, and acts to keep cached lists of active networks accurate.
     */
    void notifyObserversToPurgeActiveNetworkList(long[] activeNetIds) {
        for (Long nativeChangeNotifier : mNativeChangeNotifiers) {
            NetworkChangeNotifierJni.get()
                    .notifyPurgeActiveNetworkList(
                            nativeChangeNotifier, NetworkChangeNotifier.this, activeNetIds);
        }
    }

    /** Adds an observer for any connection type changes. */
    public static void addConnectionTypeObserver(ConnectionTypeObserver observer) {
        getInstance().addConnectionTypeObserverInternal(observer);
    }

    private void addConnectionTypeObserverInternal(ConnectionTypeObserver observer) {
        mConnectionTypeObservers.addObserver(observer);
    }

    /** Removes an observer for any connection type changes. */
    public static void removeConnectionTypeObserver(ConnectionTypeObserver observer) {
        getInstance().removeConnectionTypeObserverInternal(observer);
    }

    private void removeConnectionTypeObserverInternal(ConnectionTypeObserver observer) {
        mConnectionTypeObservers.removeObserver(observer);
    }

    // For testing only.
    public static NetworkChangeNotifierAutoDetect getAutoDetectorForTest() {
        return getInstance().mAutoDetector;
    }

    /** Checks if there currently is connectivity. */
    public static boolean isOnline() {
        int connectionType = getInstance().getCurrentConnectionType();
        return connectionType != ConnectionType.CONNECTION_NONE;
    }

    @NativeMethods
    interface Natives {
        @NativeClassQualifiedName("NetworkChangeNotifierDelegateAndroid")
        void notifyConnectionTypeChanged(
                long nativePtr,
                NetworkChangeNotifier caller,
                int newConnectionType,
                long defaultNetId);

        @NativeClassQualifiedName("NetworkChangeNotifierDelegateAndroid")
        void notifyConnectionCostChanged(
                long nativePtr, NetworkChangeNotifier caller, int newConnectionCost);

        @NativeClassQualifiedName("NetworkChangeNotifierDelegateAndroid")
        void notifyConnectionSubtypeChanged(
                long nativePtr, NetworkChangeNotifier caller, int subType);

        @NativeClassQualifiedName("NetworkChangeNotifierDelegateAndroid")
        void notifyOfNetworkConnect(
                long nativePtr, NetworkChangeNotifier caller, long netId, int connectionType);

        @NativeClassQualifiedName("NetworkChangeNotifierDelegateAndroid")
        void notifyOfNetworkSoonToDisconnect(
                long nativePtr, NetworkChangeNotifier caller, long netId);

        @NativeClassQualifiedName("NetworkChangeNotifierDelegateAndroid")
        void notifyOfNetworkDisconnect(long nativePtr, NetworkChangeNotifier caller, long netId);

        @NativeClassQualifiedName("NetworkChangeNotifierDelegateAndroid")
        void notifyPurgeActiveNetworkList(
                long nativePtr, NetworkChangeNotifier caller, long[] activeNetIds);
    }
}
