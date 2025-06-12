/*
 * Copyright (C) 2019 The Android Open Source Project
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
package android.net;

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;

import android.Manifest;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;
import com.android.net.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * This class provides the APIs to control the tethering service.
 * <p> The primary responsibilities of this class are to provide the APIs for applications to
 * start tethering, stop tethering, query configuration and query status.
 *
 */
@SuppressLint({"NotCloseable", "UnflaggedApi"})
public class TetheringManager {
    private static final String TAG = TetheringManager.class.getSimpleName();
    private static final int DEFAULT_TIMEOUT_MS = 60_000;
    private static final long CONNECTOR_POLL_INTERVAL_MILLIS = 200L;

    @GuardedBy("mConnectorWaitQueue")
    @Nullable
    private ITetheringConnector mConnector;
    @GuardedBy("mConnectorWaitQueue")
    @NonNull
    private final List<ConnectorConsumer> mConnectorWaitQueue = new ArrayList<>();
    private final Supplier<IBinder> mConnectorSupplier;

    private final TetheringCallbackInternal mCallback;
    private final Context mContext;
    private final ArrayMap<TetheringEventCallback, ITetheringEventCallback>
            mTetheringEventCallbacks = new ArrayMap<>();

    private volatile TetheringConfigurationParcel mTetheringConfiguration;
    private volatile TetherStatesParcel mTetherStatesParcel;

    /**
     * Broadcast Action: A tetherable connection has come or gone.
     * Uses {@code TetheringManager.EXTRA_AVAILABLE_TETHER},
     * {@code TetheringManager.EXTRA_ACTIVE_LOCAL_ONLY},
     * {@code TetheringManager.EXTRA_ACTIVE_TETHER}, and
     * {@code TetheringManager.EXTRA_ERRORED_TETHER} to indicate
     * the current state of tethering.  Each include a list of
     * interface names in that state (may be empty).
     * @hide
     *
     * @deprecated New client should use TetheringEventCallback instead.
     */
    @Deprecated
    @SystemApi
    public static final String ACTION_TETHER_STATE_CHANGED =
            "android.net.conn.TETHER_STATE_CHANGED";

    /**
     * gives a String[] listing all the interfaces configured for
     * tethering and currently available for tethering.
     * @hide
     */
    @SystemApi
    public static final String EXTRA_AVAILABLE_TETHER = "availableArray";

    /**
     * gives a String[] listing all the interfaces currently in local-only
     * mode (ie, has DHCPv4+IPv6-ULA support and no packet forwarding)
     * @hide
     */
    @SystemApi
    public static final String EXTRA_ACTIVE_LOCAL_ONLY = "android.net.extra.ACTIVE_LOCAL_ONLY";

    /**
     * gives a String[] listing all the interfaces currently tethered
     * (ie, has DHCPv4 support and packets potentially forwarded/NATed)
     * @hide
     */
    @SystemApi
    public static final String EXTRA_ACTIVE_TETHER = "tetherArray";

    /**
     * gives a String[] listing all the interfaces we tried to tether and
     * failed.  Use {@link #getLastTetherError} to find the error code
     * for any interfaces listed here.
     * @hide
     */
    @SystemApi
    public static final String EXTRA_ERRORED_TETHER = "erroredArray";

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = false, value = {
            TETHERING_WIFI,
            TETHERING_USB,
            TETHERING_BLUETOOTH,
            TETHERING_WIFI_P2P,
            TETHERING_NCM,
            TETHERING_ETHERNET,
            TETHERING_VIRTUAL,
    })
    public @interface TetheringType {
    }

    /**
     * Invalid tethering type.
     * @see #startTethering.
     * @hide
     */
    @SystemApi
    public static final int TETHERING_INVALID   = -1;

    /**
     * Wifi tethering type.
     * @see #startTethering.
     */
    @SuppressLint("UnflaggedApi")
    public static final int TETHERING_WIFI      = 0;

    /**
     * USB tethering type.
     * @see #startTethering.
     * @hide
     */
    @SystemApi
    public static final int TETHERING_USB       = 1;

    /**
     * Bluetooth tethering type.
     * @see #startTethering.
     * @hide
     */
    @SystemApi
    public static final int TETHERING_BLUETOOTH = 2;

    /**
     * Wifi P2p tethering type.
     * Wifi P2p tethering is set through events automatically, and don't
     * need to start from #startTethering.
     * @hide
     */
    @SystemApi
    public static final int TETHERING_WIFI_P2P = 3;

    /**
     * Ncm local tethering type.
     * @see #startTethering(TetheringRequest, Executor, StartTetheringCallback)
     * @hide
     */
    @SystemApi
    public static final int TETHERING_NCM = 4;

    /**
     * Ethernet tethering type.
     * @see #startTethering(TetheringRequest, Executor, StartTetheringCallback)
     * @hide
     */
    @SystemApi
    public static final int TETHERING_ETHERNET = 5;

    /**
     * WIGIG tethering type. Use a separate type to prevent
     * conflicts with TETHERING_WIFI
     * This type is only used internally by the tethering module
     * @hide
     */
    public static final int TETHERING_WIGIG = 6;

    /**
     * VIRTUAL tethering type.
     *
     * This tethering type is for providing external network to virtual machines
     * running on top of Android devices, which are created and managed by
     * AVF(Android Virtualization Framework).
     * @hide
     */
    @FlaggedApi(Flags.FLAG_TETHERING_REQUEST_VIRTUAL)
    @SystemApi
    public static final int TETHERING_VIRTUAL = 7;

    /**
     * The int value of last tethering type.
     * @hide
     */
    public static final int MAX_TETHERING_TYPE = TETHERING_VIRTUAL;

    private static String typeToString(@TetheringType int type) {
        switch (type) {
            case TETHERING_INVALID: return "TETHERING_INVALID";
            case TETHERING_WIFI: return "TETHERING_WIFI";
            case TETHERING_USB: return "TETHERING_USB";
            case TETHERING_BLUETOOTH: return "TETHERING_BLUETOOTH";
            case TETHERING_WIFI_P2P: return "TETHERING_WIFI_P2P";
            case TETHERING_NCM: return "TETHERING_NCM";
            case TETHERING_ETHERNET: return "TETHERING_ETHERNET";
            default:
                return "TETHERING_UNKNOWN(" + type + ")";
        }
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            TETHER_ERROR_NO_ERROR,
            TETHER_ERROR_PROVISIONING_FAILED,
            TETHER_ERROR_ENTITLEMENT_UNKNOWN,
    })
    public @interface EntitlementResult {
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            TETHER_ERROR_NO_ERROR,
            TETHER_ERROR_UNKNOWN_IFACE,
            TETHER_ERROR_SERVICE_UNAVAIL,
            TETHER_ERROR_INTERNAL_ERROR,
            TETHER_ERROR_TETHER_IFACE_ERROR,
            TETHER_ERROR_ENABLE_FORWARDING_ERROR,
            TETHER_ERROR_DISABLE_FORWARDING_ERROR,
            TETHER_ERROR_IFACE_CFG_ERROR,
            TETHER_ERROR_DHCPSERVER_ERROR,
    })
    public @interface TetheringIfaceError {
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            TETHER_ERROR_SERVICE_UNAVAIL,
            TETHER_ERROR_UNSUPPORTED,
            TETHER_ERROR_INTERNAL_ERROR,
            TETHER_ERROR_NO_CHANGE_TETHERING_PERMISSION,
            TETHER_ERROR_UNKNOWN_TYPE,
            TETHER_ERROR_DUPLICATE_REQUEST,
    })
    public @interface StartTetheringError {
    }

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            TETHER_ERROR_NO_ERROR,
            TETHER_ERROR_UNKNOWN_REQUEST,
    })
    public @interface StopTetheringError {
    }

    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_NO_ERROR = 0;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_UNKNOWN_IFACE = 1;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_SERVICE_UNAVAIL = 2;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_UNSUPPORTED = 3;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_UNAVAIL_IFACE = 4;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_INTERNAL_ERROR = 5;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_TETHER_IFACE_ERROR = 6;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_UNTETHER_IFACE_ERROR = 7;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_ENABLE_FORWARDING_ERROR = 8;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_DISABLE_FORWARDING_ERROR = 9;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_IFACE_CFG_ERROR = 10;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_PROVISIONING_FAILED = 11;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_DHCPSERVER_ERROR = 12;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_ENTITLEMENT_UNKNOWN = 13;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_NO_CHANGE_TETHERING_PERMISSION = 14;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_NO_ACCESS_TETHERING_PERMISSION = 15;
    @SuppressLint("UnflaggedApi")
    public static final int TETHER_ERROR_UNKNOWN_TYPE = 16;
    @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
    public static final int TETHER_ERROR_UNKNOWN_REQUEST = 17;
    @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
    public static final int TETHER_ERROR_DUPLICATE_REQUEST = 18;
    /**
     * Never used outside Tethering.java.
     * @hide
     */
    public static final int TETHER_ERROR_BLUETOOTH_SERVICE_PENDING = 19;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = false, value = {
            TETHER_HARDWARE_OFFLOAD_STOPPED,
            TETHER_HARDWARE_OFFLOAD_STARTED,
            TETHER_HARDWARE_OFFLOAD_FAILED,
    })
    public @interface TetherOffloadStatus {
    }

    /**
     * Tethering offload status is stopped.
     * @hide
     */
    @SystemApi
    public static final int TETHER_HARDWARE_OFFLOAD_STOPPED = 0;
    /**
     * Tethering offload status is started.
     * @hide
     */
    @SystemApi
    public static final int TETHER_HARDWARE_OFFLOAD_STARTED = 1;
    /**
     * Fail to start tethering offload.
     * @hide
     */
    @SystemApi
    public static final int TETHER_HARDWARE_OFFLOAD_FAILED = 2;

    /**
     * Create a TetheringManager object for interacting with the tethering service.
     *
     * @param context Context for the manager.
     * @param connectorSupplier Supplier for the manager connector; may return null while the
     *                          service is not connected.
     * {@hide}
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public TetheringManager(@NonNull final Context context,
            @NonNull Supplier<IBinder> connectorSupplier) {
        mContext = context;
        mCallback = new TetheringCallbackInternal(this);
        mConnectorSupplier = connectorSupplier;

        final String pkgName = mContext.getOpPackageName();

        final IBinder connector = mConnectorSupplier.get();
        // If the connector is available on start, do not start a polling thread. This introduces
        // differences in the thread that sends the oneway binder calls to the service between the
        // first few seconds after boot and later, but it avoids always having differences between
        // the first usage of TetheringManager from a process and subsequent usages (so the
        // difference is only on boot). On boot binder calls may be queued until the service comes
        // up and be sent from a worker thread; later, they are always sent from the caller thread.
        // Considering that it's just oneway binder calls, and ordering is preserved, this seems
        // better than inconsistent behavior persisting after boot.
        // If system server restarted, mConnectorSupplier might temporarily return a stale (i.e.
        // dead) version of TetheringService.
        if (connector != null && connector.isBinderAlive()) {
            mConnector = ITetheringConnector.Stub.asInterface(connector);
        } else {
            startPollingForConnector();
        }

        Log.i(TAG, "registerTetheringEventCallback:" + pkgName);
        getConnector(c -> c.registerTetheringEventCallback(mCallback, pkgName));
    }

    /** @hide */
    @Override
    protected void finalize() throws Throwable {
        final String pkgName = mContext.getOpPackageName();
        Log.i(TAG, "unregisterTetheringEventCallback:" + pkgName);
        // 1. It's generally not recommended to perform long operations in finalize, but while
        // unregisterTetheringEventCallback does an IPC, it's a oneway IPC so should not block.
        // 2. If the connector is not yet connected, TetheringManager is impossible to finalize
        // because the connector polling thread strong reference the TetheringManager object. So
        // it's guaranteed that registerTetheringEventCallback was already called before calling
        // unregisterTetheringEventCallback in finalize.
        if (mConnector == null) Log.wtf(TAG, "null connector in finalize!");
        getConnector(c -> c.unregisterTetheringEventCallback(mCallback, pkgName));

        super.finalize();
    }

    private void startPollingForConnector() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(CONNECTOR_POLL_INTERVAL_MILLIS);
                } catch (InterruptedException e) {
                    // Not much to do here, the system needs to wait for the connector
                }
                final IBinder connector = mConnectorSupplier.get();
                if (connector != null && connector.isBinderAlive()) {
                    onTetheringConnected(ITetheringConnector.Stub.asInterface(connector));
                    return;
                }
            }
        }).start();
    }

    private interface ConnectorConsumer {
        void onConnectorAvailable(ITetheringConnector connector) throws RemoteException;
    }

    private void onTetheringConnected(ITetheringConnector connector) {
        // Process the connector wait queue in order, including any items that are added
        // while processing.
        //
        // 1. Copy the queue to a local variable under lock.
        // 2. Drain the local queue with the lock released (otherwise, enqueuing future commands
        //    would block on the lock).
        // 3. Acquire the lock again. If any new tasks were queued during step 2, goto 1.
        //    If not, set mConnector to non-null so future tasks are run immediately, not queued.
        //
        // For this to work, all calls to the tethering service must use getConnector(), which
        // ensures that tasks are added to the queue with the lock held.
        //
        // Once mConnector is set to non-null, it will never be null again. If the network stack
        // process crashes, no recovery is possible.
        // TODO: evaluate whether it is possible to recover from network stack process crashes
        // (though in most cases the system will have crashed when the network stack process
        // crashes).
        do {
            final List<ConnectorConsumer> localWaitQueue;
            synchronized (mConnectorWaitQueue) {
                localWaitQueue = new ArrayList<>(mConnectorWaitQueue);
                mConnectorWaitQueue.clear();
            }

            // Allow more tasks to be added at the end without blocking while draining the queue.
            for (ConnectorConsumer task : localWaitQueue) {
                try {
                    task.onConnectorAvailable(connector);
                } catch (RemoteException e) {
                    // Most likely the network stack process crashed, which is likely to crash the
                    // system. Keep processing other requests but report the error loudly.
                    Log.wtf(TAG, "Error processing request for the tethering connector", e);
                }
            }

            synchronized (mConnectorWaitQueue) {
                if (mConnectorWaitQueue.size() == 0) {
                    mConnector = connector;
                    return;
                }
            }
        } while (true);
    }

    /**
     * Asynchronously get the ITetheringConnector to execute some operation.
     *
     * <p>If the connector is already available, the operation will be executed on the caller's
     * thread. Otherwise it will be queued and executed on a worker thread. The operation should be
     * limited to performing oneway binder calls to minimize differences due to threading.
     */
    private void getConnector(ConnectorConsumer consumer) {
        final ITetheringConnector connector;
        synchronized (mConnectorWaitQueue) {
            connector = mConnector;
            if (connector == null) {
                mConnectorWaitQueue.add(consumer);
                return;
            }
        }

        try {
            consumer.onConnectorAvailable(connector);
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
    }

    private interface RequestHelper {
        void runRequest(ITetheringConnector connector, IIntResultListener listener);
    }

    // Used to dispatch legacy ConnectivityManager methods that expect tethering to be able to
    // return results and perform operations synchronously.
    // TODO: remove once there are no callers of these legacy methods.
    private static class RequestDispatcher {
        private final ConditionVariable mWaiting;
        public volatile int mRemoteResult;

        private final IIntResultListener mListener = new IIntResultListener.Stub() {
                @Override
                public void onResult(final int resultCode) {
                    mRemoteResult = resultCode;
                    mWaiting.open();
                }
        };

        RequestDispatcher() {
            mWaiting = new ConditionVariable();
        }

        int waitForResult(final RequestHelper request, final TetheringManager mgr) {
            mgr.getConnector(c -> request.runRequest(c, mListener));
            if (!mWaiting.block(DEFAULT_TIMEOUT_MS)) {
                throw new IllegalStateException("Callback timeout");
            }

            throwIfPermissionFailure(mRemoteResult);

            return mRemoteResult;
        }
    }

    private static void throwIfPermissionFailure(final int errorCode) {
        switch (errorCode) {
            case TETHER_ERROR_NO_CHANGE_TETHERING_PERMISSION:
                throw new SecurityException("No android.permission.TETHER_PRIVILEGED"
                        + " or android.permission.WRITE_SETTINGS permission");
            case TETHER_ERROR_NO_ACCESS_TETHERING_PERMISSION:
                throw new SecurityException(
                        "No android.permission.ACCESS_NETWORK_STATE permission");
        }
    }

    /**
     * A request for a tethered interface.
     *
     * There are two reasons why this doesn't implement CLoseable:
     * 1. To consistency with the existing EthernetManager.TetheredInterfaceRequest, which is
     * already released.
     * 2. This is not synchronous, so it's not useful to use try-with-resources.
     *
     * {@hide}
     */
    @SystemApi(client = MODULE_LIBRARIES)
    @SuppressLint("NotCloseable")
    public interface TetheredInterfaceRequest {
        /**
         * Release the request to tear down tethered interface.
         */
        void release();
    }

    /**
     * Callback for requestTetheredInterface.
     *
     * {@hide}
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public interface TetheredInterfaceCallback {
        /**
         * Called when the tethered interface is available.
         * @param iface The name of the interface.
         */
        void onAvailable(@NonNull String iface);

        /**
         * Called when the tethered interface is now unavailable.
         */
        void onUnavailable();
    }

    private static class TetheringCallbackInternal extends ITetheringEventCallback.Stub {
        private volatile int mError = TETHER_ERROR_NO_ERROR;
        private final ConditionVariable mWaitForCallback = new ConditionVariable();
        // This object is never garbage collected because the Tethering code running in
        // the system server always maintains a reference to it for as long as
        // mCallback is registered.
        //
        // Don't keep a strong reference to TetheringManager because otherwise
        // TetheringManager cannot be garbage collected, and because TetheringManager
        // stores the Context that it was created from, this will prevent the calling
        // Activity from being garbage collected as well.
        private final WeakReference<TetheringManager> mTetheringMgrRef;

        TetheringCallbackInternal(final TetheringManager tm) {
            mTetheringMgrRef = new WeakReference<>(tm);
        }

        @Override
        public void onCallbackStarted(TetheringCallbackStartedParcel parcel) {
            TetheringManager tetheringMgr = mTetheringMgrRef.get();
            if (tetheringMgr != null) {
                tetheringMgr.mTetheringConfiguration = parcel.config;
                tetheringMgr.mTetherStatesParcel = parcel.states;
                mWaitForCallback.open();
            }
        }

        @Override
        public void onCallbackStopped(int errorCode) {
            TetheringManager tetheringMgr = mTetheringMgrRef.get();
            if (tetheringMgr != null) {
                mError = errorCode;
                mWaitForCallback.open();
            }
        }

        @Override
        public void onSupportedTetheringTypes(long supportedBitmap) { }

        @Override
        public void onUpstreamChanged(Network network) { }

        @Override
        public void onConfigurationChanged(TetheringConfigurationParcel config) {
            TetheringManager tetheringMgr = mTetheringMgrRef.get();
            if (tetheringMgr != null) tetheringMgr.mTetheringConfiguration = config;
        }

        @Override
        public void onTetherStatesChanged(TetherStatesParcel states) {
            TetheringManager tetheringMgr = mTetheringMgrRef.get();
            if (tetheringMgr != null) tetheringMgr.mTetherStatesParcel = states;
        }

        @Override
        public void onTetherClientsChanged(List<TetheredClient> clients) { }

        @Override
        public void onOffloadStatusChanged(int status) { }

        public void waitForStarted() {
            mWaitForCallback.block(DEFAULT_TIMEOUT_MS);
            throwIfPermissionFailure(mError);
        }
    }

    private void unsupportedAfterV() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            throw new UnsupportedOperationException("Not supported after SDK version "
                    + Build.VERSION_CODES.VANILLA_ICE_CREAM);
        }
    }

    /**
     * Attempt to tether the named interface.  This will setup a dhcp server
     * on the interface, forward and NAT IP v4 packets and forward DNS requests
     * to the best active upstream network interface.  Note that if no upstream
     * IP network interface is available, dhcp will still run and traffic will be
     * allowed between the tethered devices and this device, though upstream net
     * access will of course fail until an upstream network interface becomes
     * active.
     *
     * @deprecated Legacy tethering API. Callers should instead use
     *             {@link #startTethering(int, Executor, StartTetheringCallback)}.
     *             On SDK versions after {@link Build.VERSION_CODES.VANILLA_ICE_CREAM}, this will
     *             throw an UnsupportedOperationException.
     *
     * @param iface the interface name to tether.
     * @return error a {@code TETHER_ERROR} value indicating success or failure type
     *
     * {@hide}
     */
    @Deprecated
    @SystemApi(client = MODULE_LIBRARIES)
    public int tether(@NonNull final String iface) {
        unsupportedAfterV();

        final String callerPkg = mContext.getOpPackageName();
        Log.i(TAG, "tether caller:" + callerPkg);
        final RequestDispatcher dispatcher = new RequestDispatcher();

        return dispatcher.waitForResult((connector, listener) -> {
            try {
                connector.tether(iface, callerPkg, getAttributionTag(), listener);
            } catch (RemoteException e) {
                throw new IllegalStateException(e);
            }
        }, this);
    }

    /**
     * @return the context's attribution tag
     */
    private @Nullable String getAttributionTag() {
        return mContext.getAttributionTag();
    }

    /**
     * Stop tethering the named interface.
     *
     * @deprecated Legacy tethering API. Callers should instead use
     *             {@link #stopTethering(int)}.
     *             On SDK versions after {@link Build.VERSION_CODES.VANILLA_ICE_CREAM}, this will
     *             throw an UnsupportedOperationException.
     *
     * {@hide}
     */
    @Deprecated
    @SystemApi(client = MODULE_LIBRARIES)
    public int untether(@NonNull final String iface) {
        unsupportedAfterV();

        final String callerPkg = mContext.getOpPackageName();
        Log.i(TAG, "untether caller:" + callerPkg);

        final RequestDispatcher dispatcher = new RequestDispatcher();

        return dispatcher.waitForResult((connector, listener) -> {
            try {
                connector.untether(iface, callerPkg, getAttributionTag(), listener);
            } catch (RemoteException e) {
                throw new IllegalStateException(e);
            }
        }, this);
    }

    /**
     * Attempt to both alter the mode of USB and Tethering of USB.
     *
     * @deprecated New clients should not use this API anymore. All clients should use
     * #startTethering or #stopTethering which encapsulate proper entitlement logic. If the API is
     * used and an entitlement check is needed, downstream USB tethering will be enabled but will
     * not have any upstream.
     *
     * {@hide}
     */
    @Deprecated
    @SystemApi(client = MODULE_LIBRARIES)
    public int setUsbTethering(final boolean enable) {
        final String callerPkg = mContext.getOpPackageName();
        Log.i(TAG, "setUsbTethering caller:" + callerPkg);

        final RequestDispatcher dispatcher = new RequestDispatcher();

        return dispatcher.waitForResult((connector, listener) -> {
            try {
                connector.setUsbTethering(enable, callerPkg, getAttributionTag(),
                        listener);
            } catch (RemoteException e) {
                throw new IllegalStateException(e);
            }
        }, this);
    }

    /**
     * Indicates that this tethering connection will provide connectivity beyond this device (e.g.,
     * global Internet access).
     */
    @SuppressLint("UnflaggedApi")
    public static final int CONNECTIVITY_SCOPE_GLOBAL = 1;

    /**
     * Indicates that this tethering connection will only provide local connectivity.
     * @hide
     */
    @SystemApi
    public static final int CONNECTIVITY_SCOPE_LOCAL = 2;

    /**
     * Connectivity scopes for {@link TetheringRequest.Builder#setConnectivityScope}.
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "CONNECTIVITY_SCOPE_", value = {
            CONNECTIVITY_SCOPE_GLOBAL,
            CONNECTIVITY_SCOPE_LOCAL,
    })
    public @interface ConnectivityScope {}

    private static String connectivityScopeToString(@ConnectivityScope int scope) {
        switch (scope) {
            case CONNECTIVITY_SCOPE_GLOBAL:
                return "CONNECTIVITY_SCOPE_GLOBAL";
            case CONNECTIVITY_SCOPE_LOCAL:
                return "CONNECTIVITY_SCOPE_LOCAL";
            default:
                return "CONNECTIVITY_SCOPE_UNKNOWN(" + scope + ")";
        }
    }

    /**
     *  Use with {@link #startTethering} to specify additional parameters when starting tethering.
     */
    @SuppressLint("UnflaggedApi")
    public static final class TetheringRequest implements Parcelable {
        /**
         * Tethering started by an explicit call to startTethering.
         * @hide
         */
        public static final int REQUEST_TYPE_EXPLICIT = 0;

        /**
         * Tethering implicitly started by broadcasts (LOHS and P2P). Can never be pending.
         * @hide
         */
        public static final int REQUEST_TYPE_IMPLICIT = 1;

        /**
         * Tethering started by the legacy tether() call. Can only happen on V-.
         * @hide
         */
        public static final int REQUEST_TYPE_LEGACY = 2;

        /**
         * Tethering started but there was no pending request found. This may happen if Tethering is
         * started and immediately stopped before the link layer goes up, or if we get a link layer
         * event without a prior call to startTethering (e.g. adb shell cmd wifi start-softap).
         * @hide
         */
        public static final int REQUEST_TYPE_PLACEHOLDER = 3;

        /**
         * Type of request, used to keep track of whether the request was explicitly sent by
         * startTethering, implicitly created by broadcasts, or via legacy tether().
         * @hide
         */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(prefix = "TYPE_", value = {
                REQUEST_TYPE_EXPLICIT,
                REQUEST_TYPE_IMPLICIT,
                REQUEST_TYPE_LEGACY,
                REQUEST_TYPE_PLACEHOLDER,
        })
        public @interface RequestType {}

        /** A configuration set for TetheringRequest. */
        private final TetheringRequestParcel mRequestParcel;

        /**
         * @hide
         */
        @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
        public TetheringRequest(@NonNull final TetheringRequestParcel request) {
            mRequestParcel = request;
        }

        private TetheringRequest(@NonNull Parcel in) {
            mRequestParcel = in.readParcelable(TetheringRequestParcel.class.getClassLoader());
        }

        @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
        @NonNull
        public static final Creator<TetheringRequest> CREATOR = new Creator<>() {
            @Override
            public TetheringRequest createFromParcel(@NonNull Parcel in) {
                return new TetheringRequest(in);
            }

            @Override
            public TetheringRequest[] newArray(int size) {
                return new TetheringRequest[size];
            }
        };

        @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
        @Override
        public int describeContents() {
            return 0;
        }

        @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeParcelable(mRequestParcel, flags);
        }

        /** Builder used to create TetheringRequest. */
        @SuppressLint({"UnflaggedApi", "StaticFinalBuilder"})
        public static class Builder {
            private final TetheringRequestParcel mBuilderParcel;

            /** Default constructor of Builder. */
            @SuppressLint("UnflaggedApi")
            public Builder(@TetheringType final int type) {
                mBuilderParcel = new TetheringRequestParcel();
                mBuilderParcel.tetheringType = type;
                mBuilderParcel.localIPv4Address = null;
                mBuilderParcel.staticClientAddress = null;
                mBuilderParcel.exemptFromEntitlementCheck = false;
                mBuilderParcel.showProvisioningUi = true;
                mBuilderParcel.connectivityScope = getDefaultConnectivityScope(type);
                mBuilderParcel.uid = Process.INVALID_UID;
                mBuilderParcel.softApConfig = null;
                mBuilderParcel.interfaceName = null;
                mBuilderParcel.requestType = REQUEST_TYPE_EXPLICIT;
            }

            /**
             * Configure tethering with static IPv4 assignment.
             *
             * A DHCP server will be started, but will only be able to offer the client address.
             * The two addresses must be in the same prefix.
             *
             * @param localIPv4Address The preferred local IPv4 link address to use.
             * @param clientAddress The static client address.
             * @hide
             */
            @SystemApi
            @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
            @NonNull
            public Builder setStaticIpv4Addresses(@NonNull final LinkAddress localIPv4Address,
                    @NonNull final LinkAddress clientAddress) {
                Objects.requireNonNull(localIPv4Address);
                Objects.requireNonNull(clientAddress);
                if (!checkStaticAddressConfiguration(localIPv4Address, clientAddress)) {
                    throw new IllegalArgumentException("Invalid server or client addresses");
                }

                mBuilderParcel.localIPv4Address = localIPv4Address;
                mBuilderParcel.staticClientAddress = clientAddress;
                return this;
            }

            /**
             * Start tethering without entitlement checks.
             * @hide
             */
            @SystemApi
            @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
            @NonNull
            public Builder setExemptFromEntitlementCheck(boolean exempt) {
                mBuilderParcel.exemptFromEntitlementCheck = exempt;
                return this;
            }

            /**
             * If an entitlement check is needed, sets whether to show the entitlement UI or to
             * perform a silent entitlement check. By default, the entitlement UI is shown.
             * @hide
             */
            @SystemApi
            @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
            @NonNull
            public Builder setShouldShowEntitlementUi(boolean showUi) {
                mBuilderParcel.showProvisioningUi = showUi;
                return this;
            }

            /**
             * Sets the name of the interface. Currently supported only for
             * - {@link #TETHERING_VIRTUAL}.
             * - {@link #TETHERING_WIFI} (for Local-only Hotspot)
             * - {@link #TETHERING_WIFI_P2P}
             * @hide
             */
            @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
            @RequiresPermission(anyOf = {
                    android.Manifest.permission.NETWORK_SETTINGS,
                    android.Manifest.permission.NETWORK_STACK,
                    NetworkStack.PERMISSION_MAINLINE_NETWORK_STACK,
            })
            @NonNull
            @SystemApi(client = MODULE_LIBRARIES)
            public Builder setInterfaceName(@Nullable final String interfaceName) {
                switch (mBuilderParcel.tetheringType) {
                    case TETHERING_VIRTUAL:
                    case TETHERING_WIFI_P2P:
                    case TETHERING_WIFI:
                        break;
                    default:
                        throw new IllegalArgumentException("Interface name cannot be set for"
                                + " tethering type " + interfaceName);
                }
                mBuilderParcel.interfaceName = interfaceName;
                return this;
            }

            /**
             * Sets the connectivity scope to be provided by this tethering downstream.
             * @hide
             */
            @SystemApi
            @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
            @NonNull
            public Builder setConnectivityScope(@ConnectivityScope int scope) {
                if (!checkConnectivityScope(mBuilderParcel.tetheringType, scope)) {
                    throw new IllegalArgumentException("Invalid connectivity scope " + scope);
                }

                mBuilderParcel.connectivityScope = scope;
                return this;
            }

            /**
             * Set the desired SoftApConfiguration for {@link #TETHERING_WIFI}. If this is null or
             * not set, then the persistent tethering SoftApConfiguration from
             * {@link WifiManager#getSoftApConfiguration()} will be used.
             * </p>
             * If TETHERING_WIFI is already enabled and a new request is made with a different
             * SoftApConfiguration, the request will be accepted if the device can support an
             * additional tethering Wi-Fi AP interface. Otherwise, the request will be rejected.
             * </p>
             * Non-system callers using TETHERING_WIFI must specify a SoftApConfiguration.
             *
             * @param softApConfig SoftApConfiguration to use.
             * @throws IllegalArgumentException if the tethering type isn't TETHERING_WIFI.
             */
            @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
            @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
            @NonNull
            public Builder setSoftApConfiguration(@Nullable SoftApConfiguration softApConfig) {
                if (mBuilderParcel.tetheringType != TETHERING_WIFI) {
                    throw new IllegalArgumentException(
                            "SoftApConfiguration can only be set for TETHERING_WIFI");
                }
                mBuilderParcel.softApConfig = softApConfig;
                return this;
            }

            /** Build {@link TetheringRequest} with the currently set configuration. */
            @NonNull
            @SuppressLint("UnflaggedApi")
            public TetheringRequest build() {
                return new TetheringRequest(mBuilderParcel);
            }
        }

        /**
         * Get the local IPv4 address, if one was configured with
         * {@link Builder#setStaticIpv4Addresses}.
         * @hide
         */
        @SystemApi
        @Nullable
        public LinkAddress getLocalIpv4Address() {
            return mRequestParcel.localIPv4Address;
        }

        /**
         * Get the static IPv4 address of the client, if one was configured with
         * {@link Builder#setStaticIpv4Addresses}.
         * @hide
         */
        @SystemApi
        @Nullable
        public LinkAddress getClientStaticIpv4Address() {
            return mRequestParcel.staticClientAddress;
        }

        /**
         * Get tethering type.
         * @hide
         */
        @SystemApi
        @TetheringType
        public int getTetheringType() {
            return mRequestParcel.tetheringType;
        }

        /**
         * Get connectivity type
         * @hide
         */
        @SystemApi
        @ConnectivityScope
        public int getConnectivityScope() {
            return mRequestParcel.connectivityScope;
        }

        /**
         * Check if exempt from entitlement check.
         * @hide
         */
        @SystemApi
        public boolean isExemptFromEntitlementCheck() {
            return mRequestParcel.exemptFromEntitlementCheck;
        }

        /**
         * Check if show entitlement ui.
         * @hide
         */
        @SystemApi
        public boolean getShouldShowEntitlementUi() {
            return mRequestParcel.showProvisioningUi;
        }

        /**
         * Get interface name.
         * @hide
         */
        @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
        @Nullable
        @SystemApi(client = MODULE_LIBRARIES)
        public String getInterfaceName() {
            return mRequestParcel.interfaceName;
        }

        /**
         * Check whether the two addresses are ipv4 and in the same prefix.
         * @hide
         */
        public static boolean checkStaticAddressConfiguration(
                @NonNull final LinkAddress localIPv4Address,
                @NonNull final LinkAddress clientAddress) {
            return localIPv4Address.getPrefixLength() == clientAddress.getPrefixLength()
                    && localIPv4Address.isIpv4() && clientAddress.isIpv4()
                    && new IpPrefix(localIPv4Address.toString()).equals(
                    new IpPrefix(clientAddress.toString()));
        }

        /**
         * Returns the default connectivity scope for the given tethering type. Usually this is
         * CONNECTIVITY_SCOPE_GLOBAL, except for NCM which for historical reasons defaults to local.
         * @hide
         */
        public static @ConnectivityScope int getDefaultConnectivityScope(int tetheringType) {
            return tetheringType != TETHERING_NCM
                    ? CONNECTIVITY_SCOPE_GLOBAL
                    : CONNECTIVITY_SCOPE_LOCAL;
        }

        /**
         * Checks whether the requested connectivity scope is allowed.
         * @hide
         */
        private static boolean checkConnectivityScope(int type, int scope) {
            if (scope == CONNECTIVITY_SCOPE_GLOBAL) return true;
            return type == TETHERING_USB || type == TETHERING_ETHERNET || type == TETHERING_NCM;
        }

        /**
         * Get the desired SoftApConfiguration of the request, if one was specified.
         */
        @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
        @Nullable
        public SoftApConfiguration getSoftApConfiguration() {
            return mRequestParcel.softApConfig;
        }

        /**
         * Sets the UID of the app that sent this request. This should always be overridden when
         * receiving TetheringRequest from an external source.
         * @hide
         */
        public void setUid(int uid) {
            mRequestParcel.uid = uid;
        }

        /**
         * Sets the package name of the app that sent this request. This should always be overridden
         * when receiving a TetheringRequest from an external source.
         * @hide
         */
        public void setPackageName(String packageName) {
            mRequestParcel.packageName = packageName;
        }

        /**
         * Gets the UID of the app that sent this request. This defaults to
         * {@link Process#INVALID_UID} if unset.
         * @hide
         */
        @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
        @SystemApi(client = MODULE_LIBRARIES)
        public int getUid() {
            return mRequestParcel.uid;
        }

        /**
         * Gets the package name of the app that sent this request. This defaults to {@code null} if
         * unset.
         * @hide
         */
        @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
        @SystemApi(client = MODULE_LIBRARIES)
        @Nullable
        public String getPackageName() {
            return mRequestParcel.packageName;
        }

        /**
         * Get a TetheringRequestParcel from the configuration
         * @hide
         */
        public TetheringRequestParcel getParcel() {
            return mRequestParcel;
        }

        /**
         * Get the type of the request.
         * @hide
         */
        public @RequestType int getRequestType() {
            return mRequestParcel.requestType;
        }

        /**
         * String of TetheringRequest detail.
         * @hide
         */
        @SystemApi
        public String toString() {
            StringJoiner sj = new StringJoiner(", ", "TetheringRequest[ ", " ]");
            sj.add(typeToString(mRequestParcel.tetheringType));
            if (mRequestParcel.requestType == REQUEST_TYPE_IMPLICIT) {
                sj.add("IMPLICIT");
            } else if (mRequestParcel.requestType == REQUEST_TYPE_LEGACY) {
                sj.add("LEGACY");
            } else if (mRequestParcel.requestType == REQUEST_TYPE_PLACEHOLDER) {
                sj.add("PLACEHOLDER");
            }
            if (mRequestParcel.localIPv4Address != null) {
                sj.add("localIpv4Address=" + mRequestParcel.localIPv4Address);
            }
            if (mRequestParcel.staticClientAddress != null) {
                sj.add("staticClientAddress=" + mRequestParcel.staticClientAddress);
            }
            if (mRequestParcel.exemptFromEntitlementCheck) {
                sj.add("exemptFromEntitlementCheck");
            }
            if (mRequestParcel.showProvisioningUi) {
                sj.add("showProvisioningUi");
            }
            sj.add(connectivityScopeToString(mRequestParcel.connectivityScope));
            if (mRequestParcel.softApConfig != null) {
                sj.add("softApConfig=" + mRequestParcel.softApConfig);
            }
            if (mRequestParcel.uid != Process.INVALID_UID) {
                sj.add("uid=" + mRequestParcel.uid);
            }
            if (mRequestParcel.packageName != null) {
                sj.add("packageName=" + mRequestParcel.packageName);
            }
            if (mRequestParcel.interfaceName != null) {
                sj.add("interfaceName=" + mRequestParcel.interfaceName);
            }
            return sj.toString();
        }

        /**
         * @hide
         */
        @SystemApi
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TetheringRequest otherRequest)) return false;
            if (!equalsIgnoreUidPackage(otherRequest)) return false;
            TetheringRequestParcel parcel = getParcel();
            TetheringRequestParcel otherParcel = otherRequest.getParcel();
            return parcel.uid == otherParcel.uid
                    && Objects.equals(parcel.packageName, otherParcel.packageName);
        }

        /**
         * @hide
         */
        public boolean equalsIgnoreUidPackage(TetheringRequest otherRequest) {
            TetheringRequestParcel parcel = getParcel();
            TetheringRequestParcel otherParcel = otherRequest.getParcel();
            return parcel.requestType == otherParcel.requestType
                    && parcel.tetheringType == otherParcel.tetheringType
                    && Objects.equals(parcel.localIPv4Address, otherParcel.localIPv4Address)
                    && Objects.equals(parcel.staticClientAddress, otherParcel.staticClientAddress)
                    && parcel.exemptFromEntitlementCheck == otherParcel.exemptFromEntitlementCheck
                    && parcel.showProvisioningUi == otherParcel.showProvisioningUi
                    && parcel.connectivityScope == otherParcel.connectivityScope
                    && Objects.equals(parcel.softApConfig, otherParcel.softApConfig)
                    && Objects.equals(parcel.interfaceName, otherParcel.interfaceName);
        }

        /**
         * @hide
         */
        @SystemApi
        @Override
        public int hashCode() {
            TetheringRequestParcel parcel = getParcel();
            return Objects.hash(parcel.tetheringType, parcel.localIPv4Address,
                    parcel.staticClientAddress, parcel.exemptFromEntitlementCheck,
                    parcel.showProvisioningUi, parcel.connectivityScope, parcel.softApConfig,
                    parcel.uid, parcel.packageName, parcel.interfaceName);
        }
    }

    /**
     * Callback for use with {@link #startTethering} to find out whether tethering succeeded.
     */
    @SuppressLint("UnflaggedApi")
    public interface StartTetheringCallback {
        /**
         * Called when tethering has been successfully started.
         */
        @SuppressLint("UnflaggedApi")
        default void onTetheringStarted() {}

        /**
         * Called when starting tethering failed.
         *
         * @param error The error that caused the failure.
         */
        @SuppressLint("UnflaggedApi")
        default void onTetheringFailed(@StartTetheringError final int error) {}
    }

    /**
     * Callback for use with {@link #stopTethering} to find out whether stop tethering succeeded.
     */
    @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
    public interface StopTetheringCallback {
        /**
         * Called when tethering has been successfully stopped.
         */
        default void onStopTetheringSucceeded() {}

        /**
         * Called when starting tethering failed.
         *
         * @param error The error that caused the failure.
         */
        default void onStopTetheringFailed(@StopTetheringError final int error) {}
    }

    /**
     * Starts tethering and runs tether provisioning for the given type if needed. If provisioning
     * fails, stopTethering will be called automatically.
     *
     * @param request a {@link TetheringRequest} which can specify the preferred configuration.
     * @param executor {@link Executor} to specify the thread upon which the callback of
     *         TetheringRequest will be invoked.
     * @param callback A callback that will be called to indicate the success status of the
     *                 tethering start request.
     */
    @RequiresPermission(value = android.Manifest.permission.TETHER_PRIVILEGED, conditional = true)
    @SuppressLint("UnflaggedApi")
    public void startTethering(@NonNull final TetheringRequest request,
            @NonNull final Executor executor, @NonNull final StartTetheringCallback callback) {
        final String callerPkg = mContext.getOpPackageName();
        Log.i(TAG, "startTethering caller:" + callerPkg);

        final IIntResultListener listener = new IIntResultListener.Stub() {
            @Override
            public void onResult(final int resultCode) {
                executor.execute(() -> {
                    if (resultCode == TETHER_ERROR_NO_ERROR) {
                        callback.onTetheringStarted();
                    } else {
                        callback.onTetheringFailed(resultCode);
                    }
                });
            }
        };
        getConnector(c -> c.startTethering(request.getParcel(), callerPkg,
                getAttributionTag(), listener));
    }

    /**
     * Starts tethering and runs tether provisioning for the given type if needed. If provisioning
     * fails, stopTethering will be called automatically.
     *
     * @param type The tethering type, on of the {@code TetheringManager#TETHERING_*} constants.
     * @param executor {@link Executor} to specify the thread upon which the callback of
     *         TetheringRequest will be invoked.
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
    @SystemApi(client = MODULE_LIBRARIES)
    public void startTethering(int type, @NonNull final Executor executor,
            @NonNull final StartTetheringCallback callback) {
        startTethering(new TetheringRequest.Builder(type).build(), executor, callback);
    }

    /**
     * Stops tethering for the given type. Also cancels any provisioning rechecks for that type if
     * applicable.
     *
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
    @SystemApi
    public void stopTethering(@TetheringType final int type) {
        final String callerPkg = mContext.getOpPackageName();
        Log.i(TAG, "stopTethering caller:" + callerPkg);

        getConnector(c -> c.stopTethering(type, callerPkg, getAttributionTag(),
                new IIntResultListener.Stub() {
            @Override
            public void onResult(int resultCode) {
                // TODO: provide an API to obtain result
                // This has never been possible as stopTethering has always been void and never
                // taken a callback object. The only indication that callers have is if the call
                // results in a TETHER_STATE_CHANGE broadcast.
            }
        }));
    }

    /**
     * Stops tethering for the given request. Operation will fail with
     * {@link #TETHER_ERROR_UNKNOWN_REQUEST} if there is no request that matches it.
     */
    @RequiresPermission(value = android.Manifest.permission.TETHER_PRIVILEGED, conditional = true)
    @FlaggedApi(Flags.FLAG_TETHERING_WITH_SOFT_AP_CONFIG)
    public void stopTethering(@NonNull TetheringRequest request,
            @NonNull final Executor executor, @NonNull final StopTetheringCallback callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        final String callerPkg = mContext.getOpPackageName();
        Log.i(TAG, "stopTethering: request=" + request + ", caller=" + callerPkg);
        getConnector(c -> c.stopTetheringRequest(request, callerPkg, getAttributionTag(),
                new IIntResultListener.Stub() {
                    @Override
                    public void onResult(final int resultCode) {
                        executor.execute(() -> {
                            if (resultCode == TETHER_ERROR_NO_ERROR) {
                                callback.onStopTetheringSucceeded();
                            } else {
                                callback.onStopTetheringFailed(resultCode);
                            }
                        });
                    }
                }));
    }

    /**
     * Callback for use with {@link #getLatestTetheringEntitlementResult} to find out whether
     * entitlement succeeded.
     * @hide
     */
    @SystemApi
    public interface OnTetheringEntitlementResultListener  {
        /**
         * Called to notify entitlement result.
         *
         * @param resultCode an int value of entitlement result. It may be one of
         *         {@link #TETHER_ERROR_NO_ERROR},
         *         {@link #TETHER_ERROR_PROVISIONING_FAILED}, or
         *         {@link #TETHER_ERROR_ENTITLEMENT_UNKNOWN}.
         */
        void onTetheringEntitlementResult(@EntitlementResult int result);
    }

    /**
     * Request the latest value of the tethering entitlement check.
     *
     * <p>This method will only return the latest entitlement result if it is available. If no
     * cached entitlement result is available, and {@code showEntitlementUi} is false,
     * {@link #TETHER_ERROR_ENTITLEMENT_UNKNOWN} will be returned. If {@code showEntitlementUi} is
     * true, entitlement will be run.
     *
     * @param type the downstream type of tethering. Must be one of {@code #TETHERING_*} constants.
     * @param showEntitlementUi a boolean indicating whether to check result for the UI-based
     *         entitlement check or the silent entitlement check.
     * @param executor the executor on which callback will be invoked.
     * @param listener an {@link OnTetheringEntitlementResultListener} which will be called to
     *         notify the caller of the result of entitlement check. The listener may be called zero
     *         or one time.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
    public void requestLatestTetheringEntitlementResult(@TetheringType int type,
            boolean showEntitlementUi,
            @NonNull Executor executor,
            @NonNull final OnTetheringEntitlementResultListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException(
                    "OnTetheringEntitlementResultListener cannot be null.");
        }

        ResultReceiver wrappedListener = new ResultReceiver(null /* handler */) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                executor.execute(() -> {
                    listener.onTetheringEntitlementResult(resultCode);
                });
            }
        };

        requestLatestTetheringEntitlementResult(type, wrappedListener,
                    showEntitlementUi);
    }

    /**
     * Helper function of #requestLatestTetheringEntitlementResult to remain backwards compatible
     * with ConnectivityManager#getLatestTetheringEntitlementResult
     *
     * {@hide}
     */
    // TODO: improve the usage of ResultReceiver, b/145096122
    @SystemApi(client = MODULE_LIBRARIES)
    public void requestLatestTetheringEntitlementResult(@TetheringType final int type,
            @NonNull final ResultReceiver receiver, final boolean showEntitlementUi) {
        final String callerPkg = mContext.getOpPackageName();
        Log.i(TAG, "getLatestTetheringEntitlementResult caller:" + callerPkg);

        getConnector(c -> c.requestLatestTetheringEntitlementResult(
                type, receiver, showEntitlementUi, callerPkg, getAttributionTag()));
    }

    /**
     * Callback for use with {@link registerTetheringEventCallback} to find out tethering
     * upstream status.
     */
    @SuppressLint("UnflaggedApi")
    public interface TetheringEventCallback {
        /**
         * Called when tethering supported status changed.
         *
         * <p>This callback will be called immediately after the callback is
         * registered, and never be called if there is changes afterward.
         *
         * <p>Tethering may be disabled via system properties, device configuration, or device
         * policy restrictions.
         *
         * @param supported whether any tethering type is supported.
         * @hide
         */
        @SystemApi
        default void onTetheringSupported(boolean supported) {}

        /**
         * Called when tethering supported status changed.
         *
         * <p>This will be called immediately after the callback is registered, and may be called
         * multiple times later upon changes.
         *
         * <p>Tethering may be disabled via system properties, device configuration, or device
         * policy restrictions.
         *
         * @param supportedTypes a set of @TetheringType which is supported.
         * @hide
         */
        default void onSupportedTetheringTypes(@NonNull Set<Integer> supportedTypes) {}

        /**
         * Called when tethering upstream changed.
         *
         * <p>This will be called immediately after the callback is registered, and may be called
         * multiple times later upon changes.
         *
         * @param network the {@link Network} of tethering upstream. Null means tethering doesn't
         * have any upstream.
         * @hide
         */
        @SystemApi
        default void onUpstreamChanged(@Nullable Network network) {}

        /**
         * Called when there was a change in tethering interface regular expressions.
         *
         * <p>This will be called immediately after the callback is registered, and may be called
         * multiple times later upon changes.
         * @param reg The new regular expressions.
         *
         * @deprecated New clients should use the callbacks with {@link TetheringInterface} which
         * has the mapping between tethering type and interface. InterfaceRegex is no longer needed
         * to determine the mapping of tethering type and interface.
         *
         * @hide
         */
        @Deprecated
        @SystemApi(client = MODULE_LIBRARIES)
        default void onTetherableInterfaceRegexpsChanged(@NonNull TetheringInterfaceRegexps reg) {}

        /**
         * Called when there was a change in the list of tetherable interfaces. Tetherable
         * interface means this interface is available and can be used for tethering.
         *
         * <p>This will be called immediately after the callback is registered, and may be called
         * multiple times later upon changes.
         * @param interfaces The list of tetherable interface names.
         * @hide
         */
        @SystemApi
        default void onTetherableInterfacesChanged(@NonNull List<String> interfaces) {}

        /**
         * Called when there was a change in the list of tetherable interfaces. Tetherable
         * interface means this interface is available and can be used for tethering.
         *
         * <p>This will be called immediately after the callback is registered, and may be called
         * multiple times later upon changes.
         * @param interfaces The set of TetheringInterface of currently tetherable interface.
         * @hide
         */
        @SystemApi
        default void onTetherableInterfacesChanged(@NonNull Set<TetheringInterface> interfaces) {
            // By default, the new callback calls the old callback, so apps
            // implementing the old callback just work.
            onTetherableInterfacesChanged(toIfaces(interfaces));
        }

        /**
         * Called when there was a change in the list of tethered interfaces.
         *
         * <p>This will be called immediately after the callback is registered, and may be called
         * multiple times later upon changes.
         * @param interfaces The lit of 0 or more String of currently tethered interface names.
         * @hide
         */
        @SystemApi
        default void onTetheredInterfacesChanged(@NonNull List<String> interfaces) {}

        /**
         * Called when there was a change in the list of tethered interfaces.
         *
         * <p>This will be called immediately after the callback is registered, and may be called
         * multiple times later upon changes.
         * @param interfaces The set of 0 or more TetheringInterface of currently tethered
         * interface.
         */
        @SuppressLint("UnflaggedApi")
        default void onTetheredInterfacesChanged(@NonNull Set<TetheringInterface> interfaces) {
            // By default, the new callback calls the old callback, so apps
            // implementing the old callback just work.
            onTetheredInterfacesChanged(toIfaces(interfaces));
        }

        /**
         * Called when there was a change in the list of local-only interfaces.
         *
         * <p>This will be called immediately after the callback is registered, and may be called
         * multiple times later upon changes.
         * @param interfaces The list of 0 or more String of active local-only interface names.
         * @hide
         */
        @SystemApi
        default void onLocalOnlyInterfacesChanged(@NonNull List<String> interfaces) {}

        /**
         * Called when there was a change in the list of local-only interfaces.
         *
         * <p>This will be called immediately after the callback is registered, and may be called
         * multiple times later upon changes.
         * @param interfaces The set of 0 or more TetheringInterface of active local-only
         * interface.
         * @hide
         */
        @SystemApi
        default void onLocalOnlyInterfacesChanged(@NonNull Set<TetheringInterface> interfaces) {
            // By default, the new callback calls the old callback, so apps
            // implementing the old callback just work.
            onLocalOnlyInterfacesChanged(toIfaces(interfaces));
        }

        /**
         * Called when an error occurred configuring tethering.
         *
         * <p>This will be called immediately after the callback is registered if the latest status
         * on the interface is an error, and may be called multiple times later upon changes.
         * @param ifName Name of the interface.
         * @param error One of {@code TetheringManager#TETHER_ERROR_*}.
         * @hide
         */
        @SystemApi
        default void onError(@NonNull String ifName, @TetheringIfaceError int error) {}

        /**
         * Called when an error occurred configuring tethering.
         *
         * <p>This will be called immediately after the callback is registered if the latest status
         * on the interface is an error, and may be called multiple times later upon changes.
         * @param iface The interface that experienced the error.
         * @param error One of {@code TetheringManager#TETHER_ERROR_*}.
         * @hide
         */
        @SystemApi
        default void onError(@NonNull TetheringInterface iface, @TetheringIfaceError int error) {
            // By default, the new callback calls the old callback, so apps
            // implementing the old callback just work.
            onError(iface.getInterface(), error);
        }

        /**
         * Called when the list of tethered clients changes.
         *
         * <p>This callback provides best-effort information on connected clients based on state
         * known to the system, however the list cannot be completely accurate (and should not be
         * used for security purposes). For example, clients behind a bridge and using static IP
         * assignments are not visible to the tethering device; or even when using DHCP, such
         * clients may still be reported by this callback after disconnection as the system cannot
         * determine if they are still connected.
         * @param clients The new set of tethered clients; the collection is not ordered.
         * @hide
         */
        @SystemApi
        default void onClientsChanged(@NonNull Collection<TetheredClient> clients) {}

        /**
         * Called when tethering offload status changes.
         *
         * <p>This will be called immediately after the callback is registered.
         * @param status The offload status.
         * @hide
         */
        @SystemApi
        default void onOffloadStatusChanged(@TetherOffloadStatus int status) {}
    }

    /**
     * Covert DownStreamInterface collection to interface String array list. Internal use only.
     *
     * @hide
     */
    public static ArrayList<String> toIfaces(Collection<TetheringInterface> tetherIfaces) {
        final ArrayList<String> ifaces = new ArrayList<>();
        for (TetheringInterface tether : tetherIfaces) {
            ifaces.add(tether.getInterface());
        }

        return ifaces;
    }

    private static String[] toIfaces(TetheringInterface[] tetherIfaces) {
        final String[] ifaces = new String[tetherIfaces.length];
        for (int i = 0; i < tetherIfaces.length; i++) {
            ifaces[i] = tetherIfaces[i].getInterface();
        }

        return ifaces;
    }


    /**
     * Regular expressions used to identify tethering interfaces.
     *
     * @deprecated Instead of using regex to determine tethering type. New client could use the
     * callbacks with {@link TetheringInterface} which has the mapping of type and interface.
     * @hide
     */
    @Deprecated
    @SystemApi(client = MODULE_LIBRARIES)
    public static class TetheringInterfaceRegexps {
        private final String[] mTetherableBluetoothRegexs;
        private final String[] mTetherableUsbRegexs;
        private final String[] mTetherableWifiRegexs;

        /** @hide */
        public TetheringInterfaceRegexps(@NonNull String[] tetherableBluetoothRegexs,
                @NonNull String[] tetherableUsbRegexs, @NonNull String[] tetherableWifiRegexs) {
            mTetherableBluetoothRegexs = tetherableBluetoothRegexs.clone();
            mTetherableUsbRegexs = tetherableUsbRegexs.clone();
            mTetherableWifiRegexs = tetherableWifiRegexs.clone();
        }

        @NonNull
        public List<String> getTetherableBluetoothRegexs() {
            return Collections.unmodifiableList(Arrays.asList(mTetherableBluetoothRegexs));
        }

        @NonNull
        public List<String> getTetherableUsbRegexs() {
            return Collections.unmodifiableList(Arrays.asList(mTetherableUsbRegexs));
        }

        @NonNull
        public List<String> getTetherableWifiRegexs() {
            return Collections.unmodifiableList(Arrays.asList(mTetherableWifiRegexs));
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    Arrays.hashCode(mTetherableBluetoothRegexs),
                    Arrays.hashCode(mTetherableUsbRegexs),
                    Arrays.hashCode(mTetherableWifiRegexs));
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof TetheringInterfaceRegexps)) return false;
            final TetheringInterfaceRegexps other = (TetheringInterfaceRegexps) obj;
            return Arrays.equals(mTetherableBluetoothRegexs, other.mTetherableBluetoothRegexs)
                    && Arrays.equals(mTetherableUsbRegexs, other.mTetherableUsbRegexs)
                    && Arrays.equals(mTetherableWifiRegexs, other.mTetherableWifiRegexs);
        }
    }

    /**
     * Start listening to tethering change events. Any new added callback will receive the last
     * tethering status right away. If callback is registered,
     * {@link TetheringEventCallback#onUpstreamChanged} will immediately be called. If tethering
     * has no upstream or disabled, the argument of callback will be null. The same callback object
     * cannot be registered twice.
     *
     * @param executor the executor on which callback will be invoked.
     * @param callback the callback to be called when tethering has change events.
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    @SuppressLint("UnflaggedApi")
    public void registerTetheringEventCallback(@NonNull Executor executor,
            @NonNull TetheringEventCallback callback) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);

        final String callerPkg = mContext.getOpPackageName();
        Log.i(TAG, "registerTetheringEventCallback caller:" + callerPkg);

        synchronized (mTetheringEventCallbacks) {
            if (mTetheringEventCallbacks.containsKey(callback)) {
                throw new IllegalArgumentException("callback was already registered.");
            }
            final ITetheringEventCallback remoteCallback = new ITetheringEventCallback.Stub() {
                // Only accessed with a lock on this object
                private final HashMap<TetheringInterface, Integer> mErrorStates = new HashMap<>();
                private TetheringInterface[] mLastTetherableInterfaces = null;
                private TetheringInterface[] mLastTetheredInterfaces = null;
                private TetheringInterface[] mLastLocalOnlyInterfaces = null;

                @Override
                public void onUpstreamChanged(Network network) throws RemoteException {
                    executor.execute(() -> {
                        callback.onUpstreamChanged(network);
                    });
                }

                private synchronized void sendErrorCallbacks(final TetherStatesParcel newStates) {
                    for (int i = 0; i < newStates.erroredIfaceList.length; i++) {
                        final TetheringInterface tetherIface = newStates.erroredIfaceList[i];
                        final Integer lastError = mErrorStates.get(tetherIface);
                        final int newError = newStates.lastErrorList[i];
                        if (newError != TETHER_ERROR_NO_ERROR
                                && !Objects.equals(lastError, newError)) {
                            callback.onError(tetherIface, newError);
                        }
                        mErrorStates.put(tetherIface, newError);
                    }
                }

                private synchronized void maybeSendTetherableIfacesChangedCallback(
                        final TetherStatesParcel newStates) {
                    if (Arrays.equals(mLastTetherableInterfaces, newStates.availableList)) return;
                    mLastTetherableInterfaces = newStates.availableList.clone();
                    callback.onTetherableInterfacesChanged(
                            Collections.unmodifiableSet((new ArraySet(mLastTetherableInterfaces))));
                }

                private synchronized void maybeSendTetheredIfacesChangedCallback(
                        final TetherStatesParcel newStates) {
                    if (Arrays.equals(mLastTetheredInterfaces, newStates.tetheredList)) return;
                    mLastTetheredInterfaces = newStates.tetheredList.clone();
                    callback.onTetheredInterfacesChanged(
                            Collections.unmodifiableSet((new ArraySet(mLastTetheredInterfaces))));
                }

                private synchronized void maybeSendLocalOnlyIfacesChangedCallback(
                        final TetherStatesParcel newStates) {
                    if (Arrays.equals(mLastLocalOnlyInterfaces, newStates.localOnlyList)) return;
                    mLastLocalOnlyInterfaces = newStates.localOnlyList.clone();
                    callback.onLocalOnlyInterfacesChanged(
                            Collections.unmodifiableSet((new ArraySet(mLastLocalOnlyInterfaces))));
                }

                // Called immediately after the callbacks are registered.
                @Override
                public void onCallbackStarted(TetheringCallbackStartedParcel parcel) {
                    executor.execute(() -> {
                        callback.onSupportedTetheringTypes(unpackBits(parcel.supportedTypes));
                        callback.onTetheringSupported(parcel.supportedTypes != 0);
                        callback.onUpstreamChanged(parcel.upstreamNetwork);
                        sendErrorCallbacks(parcel.states);
                        sendRegexpsChanged(parcel.config);
                        maybeSendTetherableIfacesChangedCallback(parcel.states);
                        maybeSendTetheredIfacesChangedCallback(parcel.states);
                        maybeSendLocalOnlyIfacesChangedCallback(parcel.states);
                        callback.onClientsChanged(parcel.tetheredClients);
                        callback.onOffloadStatusChanged(parcel.offloadStatus);
                    });
                }

                @Override
                public void onCallbackStopped(int errorCode) {
                    executor.execute(() -> {
                        throwIfPermissionFailure(errorCode);
                    });
                }

                @Override
                public void onSupportedTetheringTypes(long supportedBitmap) {
                    executor.execute(() -> {
                        callback.onSupportedTetheringTypes(unpackBits(supportedBitmap));
                    });
                }

                private void sendRegexpsChanged(TetheringConfigurationParcel parcel) {
                    callback.onTetherableInterfaceRegexpsChanged(new TetheringInterfaceRegexps(
                            parcel.tetherableBluetoothRegexs,
                            parcel.tetherableUsbRegexs,
                            parcel.tetherableWifiRegexs));
                }

                @Override
                public void onConfigurationChanged(TetheringConfigurationParcel config) {
                    executor.execute(() -> sendRegexpsChanged(config));
                }

                @Override
                public void onTetherStatesChanged(TetherStatesParcel states) {
                    executor.execute(() -> {
                        sendErrorCallbacks(states);
                        maybeSendTetherableIfacesChangedCallback(states);
                        maybeSendTetheredIfacesChangedCallback(states);
                        maybeSendLocalOnlyIfacesChangedCallback(states);
                    });
                }

                @Override
                public void onTetherClientsChanged(final List<TetheredClient> clients) {
                    executor.execute(() -> callback.onClientsChanged(clients));
                }

                @Override
                public void onOffloadStatusChanged(final int status) {
                    executor.execute(() -> callback.onOffloadStatusChanged(status));
                }
            };
            getConnector(c -> c.registerTetheringEventCallback(remoteCallback, callerPkg));
            mTetheringEventCallbacks.put(callback, remoteCallback);
        }
    }

    /**
     * Unpack bitmap to a set of bit position intergers.
     * @hide
     */
    public static ArraySet<Integer> unpackBits(long val) {
        final ArraySet<Integer> result = new ArraySet<>(Long.bitCount(val));
        int bitPos = 0;
        while (val != 0) {
            if ((val & 1) == 1) result.add(bitPos);

            val = val >>> 1;
            bitPos++;
        }

        return result;
    }

    /**
     * Remove tethering event callback previously registered with
     * {@link #registerTetheringEventCallback}.
     *
     * @param callback previously registered callback.
     */
    @RequiresPermission(anyOf = {
            Manifest.permission.TETHER_PRIVILEGED,
            Manifest.permission.ACCESS_NETWORK_STATE
    })
    @SuppressLint("UnflaggedApi")
    public void unregisterTetheringEventCallback(@NonNull final TetheringEventCallback callback) {
        Objects.requireNonNull(callback);

        final String callerPkg = mContext.getOpPackageName();
        Log.i(TAG, "unregisterTetheringEventCallback caller:" + callerPkg);

        synchronized (mTetheringEventCallbacks) {
            ITetheringEventCallback remoteCallback = mTetheringEventCallbacks.remove(callback);
            if (remoteCallback == null) {
                throw new IllegalArgumentException("callback was not registered.");
            }

            getConnector(c -> c.unregisterTetheringEventCallback(remoteCallback, callerPkg));
        }
    }

    /**
     * Get a more detailed error code after a Tethering or Untethering
     * request asynchronously failed.
     *
     * @param iface The name of the interface of interest
     * @return error The error code of the last error tethering or untethering the named
     *               interface
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public int getLastTetherError(@NonNull final String iface) {
        mCallback.waitForStarted();
        if (mTetherStatesParcel == null) return TETHER_ERROR_NO_ERROR;

        int i = 0;
        for (TetheringInterface errored : mTetherStatesParcel.erroredIfaceList) {
            if (iface.equals(errored.getInterface())) return mTetherStatesParcel.lastErrorList[i];

            i++;
        }
        return TETHER_ERROR_NO_ERROR;
    }

    /**
     * Get the list of regular expressions that define any tetherable
     * USB network interfaces.  If USB tethering is not supported by the
     * device, this list should be empty.
     *
     * @return an array of 0 or more regular expression Strings defining
     *        what interfaces are considered tetherable usb interfaces.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public @NonNull String[] getTetherableUsbRegexs() {
        mCallback.waitForStarted();
        return mTetheringConfiguration.tetherableUsbRegexs;
    }

    /**
     * Get the list of regular expressions that define any tetherable
     * Wifi network interfaces.  If Wifi tethering is not supported by the
     * device, this list should be empty.
     *
     * @return an array of 0 or more regular expression Strings defining
     *        what interfaces are considered tetherable wifi interfaces.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public @NonNull String[] getTetherableWifiRegexs() {
        mCallback.waitForStarted();
        return mTetheringConfiguration.tetherableWifiRegexs;
    }

    /**
     * Get the list of regular expressions that define any tetherable
     * Bluetooth network interfaces.  If Bluetooth tethering is not supported by the
     * device, this list should be empty.
     *
     * @return an array of 0 or more regular expression Strings defining
     *        what interfaces are considered tetherable bluetooth interfaces.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public @NonNull String[] getTetherableBluetoothRegexs() {
        mCallback.waitForStarted();
        return mTetheringConfiguration.tetherableBluetoothRegexs;
    }

    /**
     * Get the set of tetherable, available interfaces.  This list is limited by
     * device configuration and current interface existence.
     *
     * @return an array of 0 or more Strings of tetherable interface names.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public @NonNull String[] getTetherableIfaces() {
        mCallback.waitForStarted();
        if (mTetherStatesParcel == null) return new String[0];

        return toIfaces(mTetherStatesParcel.availableList);
    }

    /**
     * Get the set of tethered interfaces.
     *
     * @return an array of 0 or more String of currently tethered interface names.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public @NonNull String[] getTetheredIfaces() {
        mCallback.waitForStarted();
        if (mTetherStatesParcel == null) return new String[0];

        return toIfaces(mTetherStatesParcel.tetheredList);
    }

    /**
     * Get the set of interface names which attempted to tether but
     * failed.  Re-attempting to tether may cause them to reset to the Tethered
     * state.  Alternatively, causing the interface to be destroyed and recreated
     * may cause them to reset to the available state.
     * {@link TetheringManager#getLastTetherError} can be used to get more
     * information on the cause of the errors.
     *
     * @return an array of 0 or more String indicating the interface names
     *        which failed to tether.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public @NonNull String[] getTetheringErroredIfaces() {
        mCallback.waitForStarted();
        if (mTetherStatesParcel == null) return new String[0];

        return toIfaces(mTetherStatesParcel.erroredIfaceList);
    }

    /**
     * Get the set of tethered dhcp ranges.
     *
     * @deprecated This API just return the default value which is not used in DhcpServer.
     * @hide
     */
    @Deprecated
    public @NonNull String[] getTetheredDhcpRanges() {
        mCallback.waitForStarted();
        return mTetheringConfiguration.legacyDhcpRanges;
    }

    /**
     * Check if the device allows for tethering.  It may be disabled via
     * {@code ro.tether.denied} system property, Settings.TETHER_SUPPORTED or
     * due to device configuration.
     *
     * @return a boolean - {@code true} indicating Tethering is supported.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public boolean isTetheringSupported() {
        final String callerPkg = mContext.getOpPackageName();

        return isTetheringSupported(callerPkg);
    }

    /**
     * Check if the device allows for tethering. It may be disabled via {@code ro.tether.denied}
     * system property, Settings.TETHER_SUPPORTED or due to device configuration. This is useful
     * for system components that query this API on behalf of an app. In particular, Bluetooth
     * has @UnsupportedAppUsage calls that will let apps turn on bluetooth tethering if they have
     * the right permissions, but such an app needs to know whether it can (permissions as well
     * as support from the device) turn on tethering in the first place to show the appropriate UI.
     *
     * @param callerPkg The caller package name, if it is not matching the calling uid,
     *       SecurityException would be thrown.
     * @return a boolean - {@code true} indicating Tethering is supported.
     * @hide
     */
    @SystemApi(client = MODULE_LIBRARIES)
    public boolean isTetheringSupported(@NonNull final String callerPkg) {

        final RequestDispatcher dispatcher = new RequestDispatcher();
        final int ret = dispatcher.waitForResult((connector, listener) -> {
            try {
                connector.isTetheringSupported(callerPkg, getAttributionTag(), listener);
            } catch (RemoteException e) {
                throw new IllegalStateException(e);
            }
        }, this);

        return ret == TETHER_ERROR_NO_ERROR;
    }

    /**
     * Stop all active tethering.
     * @hide
     */
    @SystemApi
    @RequiresPermission(android.Manifest.permission.TETHER_PRIVILEGED)
    public void stopAllTethering() {
        final String callerPkg = mContext.getOpPackageName();
        Log.i(TAG, "stopAllTethering caller:" + callerPkg);

        getConnector(c -> c.stopAllTethering(callerPkg, getAttributionTag(),
                new IIntResultListener.Stub() {
                    @Override
                    public void onResult(int resultCode) {
                        // TODO: add an API parameter to send result to caller.
                        // This has never been possible as stopAllTethering has always been void
                        // and never taken a callback object. The only indication that callers have
                        // is if the call results in a TETHER_STATE_CHANGE broadcast.
                    }
                }));
    }

    /**
     * Whether to treat networks that have TRANSPORT_TEST as Tethering upstreams. The effects of
     * this method apply to any test networks that are already present on the system.
     *
     * @throws SecurityException If the caller doesn't have the NETWORK_SETTINGS permission.
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.NETWORK_SETTINGS)
    public void setPreferTestNetworks(final boolean prefer) {
        Log.i(TAG, "setPreferTestNetworks caller: " + mContext.getOpPackageName());

        final RequestDispatcher dispatcher = new RequestDispatcher();
        final int ret = dispatcher.waitForResult((connector, listener) -> {
            try {
                connector.setPreferTestNetworks(prefer, listener);
            } catch (RemoteException e) {
                throw new IllegalStateException(e);
            }
        }, this);
    }
}
