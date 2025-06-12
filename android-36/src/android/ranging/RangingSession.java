/*
 * Copyright 2024 The Android Open Source Project
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

package android.ranging;

import android.Manifest;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.content.AttributionSource;
import android.os.CancellationSignal;
import android.os.RemoteException;
import android.ranging.oob.DeviceHandle;
import android.ranging.oob.OobHandle;
import android.ranging.oob.OobInitiatorRangingConfig;
import android.ranging.oob.OobResponderRangingConfig;
import android.ranging.oob.TransportHandle;
import android.ranging.raw.RawResponderRangingConfig;
import android.util.Log;

import com.android.ranging.flags.Flags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * Represents a session for performing ranging operations. A {@link RangingSession} manages
 * the lifecycle of a ranging operation, including start, stop, and event callbacks.
 *
 * <p>All methods are asynchronous and rely on the provided {@link Executor} to invoke
 * callbacks on appropriate threads.
 *
 * <p>This class implements {@link AutoCloseable}, ensuring that resources can be
 * automatically released when the session is closed.
 *
 */
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
public final class RangingSession implements AutoCloseable {
    private static final String TAG = "RangingSession";
    private final AttributionSource mAttributionSource;
    private final SessionHandle mSessionHandle;
    private final IRangingAdapter mRangingAdapter;
    private final RangingSessionManager mRangingSessionManager;
    private final Callback mCallback;
    private final Executor mExecutor;
    private final Map<RangingDevice, android.ranging.oob.TransportHandle> mTransportHandles =
            new ConcurrentHashMap<>();


    /**
     * @hide
     */
    public RangingSession(RangingSessionManager rangingSessionManager,
            AttributionSource attributionSource,
            SessionHandle sessionHandle, IRangingAdapter rangingAdapter,
            Callback callback, Executor executor) {
        mRangingSessionManager = rangingSessionManager;
        mAttributionSource = attributionSource;
        mSessionHandle = sessionHandle;
        mRangingAdapter = rangingAdapter;
        mCallback = callback;
        mExecutor = executor;
    }

    /**
     * Starts the ranging session with the provided ranging preferences.
     * <p>The {@link Callback#onOpened()} will be called when the session finishes starting.
     *
     * <p>The provided {@link RangingPreference} determines the configuration for the session.
     * A {@link CancellationSignal} is returned to allow the caller to cancel the session
     * if needed. If the session is canceled, the {@link #close()} method will be invoked
     * automatically to release resources.
     *
     * @param rangingPreference {@link RangingPreference} the preferences for configuring the
     *                          ranging session.
     * @return a {@link CancellationSignal} to close the session.
     */
    @RequiresPermission(Manifest.permission.RANGING)
    @NonNull
    public CancellationSignal start(@NonNull RangingPreference rangingPreference) {
        if (rangingPreference.getRangingParams().getRangingSessionType()
                == RangingConfig.RANGING_SESSION_OOB) {
            mRangingSessionManager.registerOobSendDataListener();
            setupTransportHandles(rangingPreference);
        }
        Log.v(TAG, "Start ranging - " + mSessionHandle);
        try {
            mRangingAdapter.startRanging(mAttributionSource, mSessionHandle, rangingPreference,
                    mRangingSessionManager);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
        CancellationSignal cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(this::close);

        return cancellationSignal;
    }

    private void setupTransportHandles(RangingPreference rangingPreference) {
        List<DeviceHandle> deviceHandleList = new ArrayList<>();
        if (rangingPreference.getRangingParams() instanceof OobInitiatorRangingConfig) {
            deviceHandleList.addAll(((OobInitiatorRangingConfig)
                    rangingPreference.getRangingParams()).getDeviceHandles());
        } else if (rangingPreference.getRangingParams() instanceof OobResponderRangingConfig) {
            deviceHandleList.add(((OobResponderRangingConfig)
                    rangingPreference.getRangingParams()).getDeviceHandle());
        }
        for (DeviceHandle deviceHandle : deviceHandleList) {
            TransportHandleReceiveCallback receiveCallback =
                    new TransportHandleReceiveCallback(deviceHandle.getRangingDevice());
            deviceHandle.getTransportHandle().registerReceiveCallback(
                    Executors.newCachedThreadPool(), receiveCallback);
            mTransportHandles.put(deviceHandle.getRangingDevice(),
                    deviceHandle.getTransportHandle());
        }
    }

    /**
     * Adds a new device to an ongoing ranging session.
     * <p>
     * This method allows for adding a new device to an active ranging session using raw ranging
     * parameters. Only devices represented by {@link RawResponderRangingConfig} is supported.
     * If the provided {@link RangingConfig} does not match one of these types, the addition fails
     * and invokes {@link Callback#onOpenFailed(int)} with a reason of
     * {@link Callback#REASON_UNSUPPORTED}.
     * </p>
     *
     * @param deviceRangingParams the ranging parameters for the device to be added,
     *                            which must be an instance of {@link RawResponderRangingConfig}
     *
     * @apiNote If the underlying ranging technology cannot support this dynamic addition, failure
     * will be indicated via {@code Callback#onStartFailed(REASON_UNSUPPORTED, RangingDevice)}
     *
     */
    @RequiresPermission(Manifest.permission.RANGING)
    public void addDeviceToRangingSession(@NonNull RangingConfig deviceRangingParams) {
        Log.v(TAG, " Add device - " + mSessionHandle);
        try {
            if (deviceRangingParams instanceof RawResponderRangingConfig) {
                mRangingAdapter.addRawDevice(mSessionHandle,
                        (RawResponderRangingConfig) deviceRangingParams);
            } else {
                mCallback.onOpenFailed(Callback.REASON_UNSUPPORTED);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Removes a specific device from an ongoing ranging session.
     * <p>
     * This method removes a specified device from the active ranging session, stopping
     * further ranging operations for that device. The operation is handled by the system
     * server and may throw a {@link RemoteException} in case of server-side communication
     * issues.
     * </p>
     *
     * @param rangingDevice the device to be removed from the session.
     * @apiNote Currently, this API is supported only for UWB multicast session if using
     * {@link RangingConfig#RANGING_SESSION_RAW}.
     *
     */
    @RequiresPermission(Manifest.permission.RANGING)
    public void removeDeviceFromRangingSession(@NonNull RangingDevice rangingDevice) {
        Log.v(TAG, " Remove device - " + mSessionHandle);
        try {
            mRangingAdapter.removeDevice(mSessionHandle, rangingDevice);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Reconfigures the ranging interval for the current session by setting the interval
     * skip count. The {@code intervalSkipCount} defines how many intervals should be skipped
     * between successive ranging rounds. Valid values range from 0 to 255.
     *
     * @param intervalSkipCount the number of intervals to skip, ranging from 0 to 255.
     */
    @RequiresPermission(Manifest.permission.RANGING)
    public void reconfigureRangingInterval(@IntRange(from = 0, to = 255) int intervalSkipCount) {
        Log.v(TAG, " Reconfiguring ranging interval - " + mSessionHandle);
        try {
            mRangingAdapter.reconfigureRangingInterval(mSessionHandle, intervalSkipCount);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Stops the ranging session.
     *
     * <p>This method releases any ongoing ranging operations. If the operation fails,
     * it will propagate a {@link RemoteException} from the system server.
     */
    @RequiresPermission(Manifest.permission.RANGING)
    public void stop() {
        Log.v(TAG, "Stop ranging - " + mSessionHandle);
        try {
            mRangingAdapter.stopRanging(mSessionHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * @hide
     */
    public void onOpened() {
        mExecutor.execute(mCallback::onOpened);
    }

    /**
     * @hide
     */
    public void onOpenFailed(@Callback.Reason int reason) {
        mExecutor.execute(() -> mCallback.onOpenFailed(reason));
    }

    /**
     * @hide
     */
    public void onStarted(RangingDevice peer, @RangingManager.RangingTechnology int technology) {
        mExecutor.execute(() -> mCallback.onStarted(peer, technology));
    }

    /**
     * @hide
     */
    public void onResults(RangingDevice peer, RangingData data) {
        mExecutor.execute(() -> mCallback.onResults(peer, data));
    }

    /**
     * @hide
     */
    public void onStopped(RangingDevice peer, @RangingManager.RangingTechnology int technology) {
        mExecutor.execute(() -> mCallback.onStopped(peer, technology));
    }

    /**
     * @hide
     */
    public void onClosed(@Callback.Reason int reason) {
        mExecutor.execute(() -> mCallback.onClosed(reason));
    }

    /**
     * @hide
     */
    void sendOobData(RangingDevice toDevice, byte[] data) {
        if (!mTransportHandles.containsKey(toDevice)) {
            Log.e(TAG, "TransportHandle not found for session: " + mSessionHandle + ", device: "
                    + toDevice);
        }
        mTransportHandles.get(toDevice).sendData(data);
    }

    @RequiresPermission(Manifest.permission.RANGING)
    @Override
    public void close() {
        stop();
    }

    /**
     * Callback interface for receiving ranging session events.
     */
    public interface Callback {
        /**
         * @hide
         */
        @Retention(RetentionPolicy.SOURCE)
        @Target({ElementType.TYPE_USE})
        @IntDef(value = {
                REASON_UNKNOWN,
                REASON_LOCAL_REQUEST,
                REASON_REMOTE_REQUEST,
                REASON_UNSUPPORTED,
                REASON_SYSTEM_POLICY,
                REASON_NO_PEERS_FOUND,
        })
        @interface Reason {
        }

        /**
         * Indicates that the session was closed due to an unknown reason.
         */
        int REASON_UNKNOWN = 0;

        /**
         * Indicates that the session was closed because {@link AutoCloseable#close()} or
         * {@link RangingSession#stop()} was called.
         */
        int REASON_LOCAL_REQUEST = 1;

        /**
         * Indicates that the session was closed at the request of a remote peer.
         */
        int REASON_REMOTE_REQUEST = 2;

        /**
         * Indicates that the session closed because the provided session parameters were not
         * supported.
         */
        int REASON_UNSUPPORTED = 3;

        /**
         * Indicates that the local system policy forced the session to close, such
         * as power management policy, airplane mode etc.
         */
        int REASON_SYSTEM_POLICY = 4;

        /**
         * Indicates that the session was closed because none of the specified peers were found.
         */
        int REASON_NO_PEERS_FOUND = 5;

        /**
         * Called when the ranging session opens successfully.
         */
        void onOpened();

        /**
         * Called when the ranging session failed to open.
         *
         * @param reason the reason for the failure, limited to values defined by
         *               {@link Reason}.
         */
        void onOpenFailed(@Reason int reason);

        /**
         * Called when ranging has started with a particular peer using a particular technology
         * during an ongoing session.
         *
         * @param peer       {@link RangingDevice} the peer with which ranging has started.
         * @param technology {@link android.ranging.RangingManager.RangingTechnology}
         *                   the ranging technology that started.
         */
        void onStarted(
                @NonNull RangingDevice peer, @RangingManager.RangingTechnology int technology);

        /**
         * Called when ranging data has been received from a peer.
         *
         * @param peer {@link RangingDevice} the peer from which ranging data was received.
         * @param data {@link RangingData} the received.
         */
        void onResults(@NonNull RangingDevice peer, @NonNull RangingData data);

        /**
         * Called when ranging has stopped with a particular peer using a particular technology
         * during an ongoing session.
         *
         * @param peer       {@link RangingDevice} the peer with which ranging has stopped.
         * @param technology {@link android.ranging.RangingManager.RangingTechnology}
         *                   the ranging technology that stopped.
         */
        void onStopped(
                @NonNull RangingDevice peer, @RangingManager.RangingTechnology int technology);

        /**
         * Called when the ranging session has closed.
         *
         * @param reason the reason why the session was closed, limited to values
         *               defined by {@link Reason}.
         */
        void onClosed(@Reason int reason);

    }

    class TransportHandleReceiveCallback implements TransportHandle.ReceiveCallback {

        private final android.ranging.oob.OobHandle mOobHandle;

        TransportHandleReceiveCallback(RangingDevice device) {
            mOobHandle = new OobHandle(mSessionHandle, device);
        }

        @Override
        public void onReceiveData(byte[] data) {
            mRangingSessionManager.oobDataReceived(mOobHandle, data);
        }

        @Override
        public void onSendFailed() {
        }

        @Override
        public void onDisconnect() {
            mRangingSessionManager.deviceOobDisconnected(mOobHandle);
        }

        @Override
        public void onReconnect() {
            mRangingSessionManager.deviceOobReconnected(mOobHandle);
        }

        @Override
        public void onClose() {
            mRangingSessionManager.deviceOobClosed(mOobHandle);
        }
    }

    @Override
    public String toString() {
        return "RangingSession{ "
                + "mSessionHandle="
                + mSessionHandle
                + ", mRangingAdapter="
                + mRangingAdapter
                + ", mRangingSessionManager="
                + mRangingSessionManager
                + ", mCallback="
                + mCallback
                + ", mExecutor="
                + mExecutor
                + ", mTransportHandles="
                + mTransportHandles
                + " }";
    }
}
