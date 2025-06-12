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

import static android.annotation.SystemApi.Client.PRIVILEGED_APPS;

import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * Allows applications to request that the system periodically send specific packets on their
 * behalf, using hardware offload to save battery power.
 *
 * To request that the system send keepalives, call one of the methods that return a
 * {@link SocketKeepalive} object, such as {@link ConnectivityManager#createSocketKeepalive},
 * passing in a non-null callback. If the {@link SocketKeepalive} is successfully
 * started, the callback's {@code onStarted} method will be called. If an error occurs,
 * {@code onError} will be called, specifying one of the {@code ERROR_*} constants in this
 * class.
 *
 * To stop an existing keepalive, call {@link SocketKeepalive#stop}. The system will call
 * {@link SocketKeepalive.Callback#onStopped} if the operation was successful or
 * {@link SocketKeepalive.Callback#onError} if an error occurred.
 *
 * For cellular, the device MUST support at least 1 keepalive slot.
 *
 * For WiFi, the device SHOULD support keepalive offload. If it does not, it MUST reply with
 * {@link SocketKeepalive.Callback#onError} with {@code ERROR_UNSUPPORTED} to any keepalive offload
 * request. If it does, it MUST support at least 3 concurrent keepalive slots.
 */
public abstract class SocketKeepalive implements AutoCloseable {
    /** @hide */
    protected static final String TAG = "SocketKeepalive";

    /**
     * Success. It indicates there is no error.
     * @hide
     */
    @SystemApi
    public static final int SUCCESS = 0;

    /**
     * Success when trying to suspend.
     * @hide
     */
    public static final int SUCCESS_PAUSED = 1;

    /**
     * No keepalive. This should only be internally as it indicates There is no keepalive.
     * It should not propagate to applications.
     * @hide
     */
    public static final int NO_KEEPALIVE = -1;

    /**
     * Data received.
     * @hide
     */
    public static final int DATA_RECEIVED = -2;

    /**
     * The binder died.
     * @hide
     */
    public static final int BINDER_DIED = -10;

    /**
     * The invalid network. It indicates the specified {@code Network} is not connected.
     */
    public static final int ERROR_INVALID_NETWORK = -20;

    /**
     * The invalid IP addresses. Indicates the specified IP addresses are invalid.
     * For example, the specified source IP address is not configured on the
     * specified {@code Network}.
     */
    public static final int ERROR_INVALID_IP_ADDRESS = -21;

    /**
     * The port is invalid.
     */
    public static final int ERROR_INVALID_PORT = -22;

    /**
     * The length is invalid (e.g. too long).
     */
    public static final int ERROR_INVALID_LENGTH = -23;

    /**
     * The interval is invalid (e.g. too short).
     */
    public static final int ERROR_INVALID_INTERVAL = -24;

    /**
     * The socket is invalid.
     */
    public static final int ERROR_INVALID_SOCKET = -25;

    /**
     * The socket is not idle.
     */
    public static final int ERROR_SOCKET_NOT_IDLE = -26;

    /**
     * The stop reason is uninitialized. This should only be internally used as initial state
     * of stop reason, instead of propagating to application.
     * @hide
     */
    public static final int ERROR_STOP_REASON_UNINITIALIZED = -27;

    /**
     * The request is unsupported.
     */
    public static final int ERROR_UNSUPPORTED = -30;

    /**
     * There was a hardware error.
     */
    public static final int ERROR_HARDWARE_ERROR = -31;

    /**
     * Resources are insufficient (e.g. all hardware slots are in use).
     */
    public static final int ERROR_INSUFFICIENT_RESOURCES = -32;

    /**
     * There was no such slot, or no keepalive running on this slot.
     * @hide
     */
    @SystemApi
    public static final int ERROR_NO_SUCH_SLOT = -33;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "ERROR_" }, value = {
            ERROR_INVALID_NETWORK,
            ERROR_INVALID_IP_ADDRESS,
            ERROR_INVALID_PORT,
            ERROR_INVALID_LENGTH,
            ERROR_INVALID_INTERVAL,
            ERROR_INVALID_SOCKET,
            ERROR_SOCKET_NOT_IDLE,
            ERROR_NO_SUCH_SLOT
    })
    public @interface ErrorCode {}

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            SUCCESS,
            ERROR_INVALID_LENGTH,
            ERROR_UNSUPPORTED,
            ERROR_INSUFFICIENT_RESOURCES,
    })
    public @interface KeepaliveEvent {}

    /**
     * Whether the system automatically toggles keepalive when no TCP connection is open on the VPN.
     *
     * If this flag is present, the system will monitor the VPN(s) running on top of the specified
     * network for open TCP connections. When no such connections are open, it will turn off the
     * keepalives to conserve battery power. When there is at least one such connection it will
     * turn on the keepalives to make sure functionality is preserved.
     *
     * This only works with {@link NattSocketKeepalive}.
     * @hide
     */
    @SystemApi
    public static final int FLAG_AUTOMATIC_ON_OFF = 1 << 0;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "FLAG_"}, flag = true, value = {
            FLAG_AUTOMATIC_ON_OFF
    })
    public @interface StartFlags {}

    /**
     * The minimum interval in seconds between keepalive packet transmissions.
     *
     * @hide
     **/
    public static final int MIN_INTERVAL_SEC = 10;

    /**
     * The maximum interval in seconds between keepalive packet transmissions.
     *
     * @hide
     **/
    public static final int MAX_INTERVAL_SEC = 3600;

    /**
     * An exception that embarks an error code.
     * @hide
     */
    public static class ErrorCodeException extends Exception {
        public final int error;
        public ErrorCodeException(final int error, final Throwable e) {
            super(e);
            this.error = error;
        }
        public ErrorCodeException(final int error) {
            this.error = error;
        }
    }

    /**
     * This socket is invalid.
     * See the error code for details, and the optional cause.
     * @hide
     */
    public static class InvalidSocketException extends ErrorCodeException {
        public InvalidSocketException(final int error, final Throwable e) {
            super(error, e);
        }
        public InvalidSocketException(final int error) {
            super(error);
        }
    }

    /** @hide */
    @NonNull protected final IConnectivityManager mService;
    /** @hide */
    @NonNull protected final Network mNetwork;
    /** @hide */
    @NonNull protected final ParcelFileDescriptor mPfd;
    /** @hide */
    @NonNull protected final Executor mExecutor;
    /** @hide */
    @NonNull protected final ISocketKeepaliveCallback mCallback;

    /** @hide */
    public SocketKeepalive(@NonNull IConnectivityManager service, @NonNull Network network,
            @NonNull ParcelFileDescriptor pfd,
            @NonNull Executor executor, @NonNull Callback callback) {
        mService = service;
        mNetwork = network;
        mPfd = pfd;
        mExecutor = executor;
        mCallback = new ISocketKeepaliveCallback.Stub() {
            @Override
            public void onStarted() {
                final long token = Binder.clearCallingIdentity();
                try {
                    mExecutor.execute(() -> {
                        callback.onStarted();
                    });
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            @Override
            public void onResumed() {
                final long token = Binder.clearCallingIdentity();
                try {
                    mExecutor.execute(() -> {
                        callback.onResumed();
                    });
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            @Override
            public void onStopped() {
                final long token = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> {
                        callback.onStopped();
                    });
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            @Override
            public void onPaused() {
                final long token = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> {
                        callback.onPaused();
                    });
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            @Override
            public void onError(int error) {
                final long token = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> {
                        callback.onError(error);
                    });
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }

            @Override
            public void onDataReceived() {
                final long token = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> {
                        callback.onDataReceived();
                    });
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        };
    }

    /**
     * Request that keepalive be started with the given {@code intervalSec}.
     *
     * See {@link SocketKeepalive}. If the remote binder dies, or the binder call throws an
     * exception when invoking start or stop of the {@link SocketKeepalive}, a
     * {@link RuntimeException} caused by a {@link RemoteException} will be thrown into the
     * {@link Executor}. This is typically not important to catch because the remote party is
     * the system, so if it is not in shape to communicate through binder the system is going
     * down anyway. If the caller still cares, it can use a custom {@link Executor} to catch the
     * {@link RuntimeException}.
     *
     * @param intervalSec The target interval in seconds between keepalive packet transmissions.
     *                    The interval should be between 10 seconds and 3600 seconds, otherwise
     *                    {@link #ERROR_INVALID_INTERVAL} will be returned.
     */
    public final void start(@IntRange(from = MIN_INTERVAL_SEC, to = MAX_INTERVAL_SEC)
            int intervalSec) {
        startImpl(intervalSec, 0 /* flags */, null /* underpinnedNetwork */);
    }

    /**
     * Request that keepalive be started with the given {@code intervalSec}.
     *
     * See {@link SocketKeepalive}. If the remote binder dies, or the binder call throws an
     * exception when invoking start or stop of the {@link SocketKeepalive}, a
     * {@link RuntimeException} caused by a {@link RemoteException} will be thrown into the
     * {@link Executor}. This is typically not important to catch because the remote party is
     * the system, so if it is not in shape to communicate through binder the system is going
     * down anyway. If the caller still cares, it can use a custom {@link Executor} to catch the
     * {@link RuntimeException}.
     *
     * @param intervalSec The target interval in seconds between keepalive packet transmissions.
     *                    The interval should be between 10 seconds and 3600 seconds. Otherwise,
     *                    the supplied {@link Callback} will see a call to
     *                    {@link Callback#onError(int)} with {@link #ERROR_INVALID_INTERVAL}.
     * @param flags Flags to enable/disable available options on this keepalive.
     * @param underpinnedNetwork an optional network running over mNetwork that this
     *                           keepalive is intended to keep up, e.g. an IPSec
     *                           tunnel running over mNetwork.
     * @hide
     */
    @SystemApi(client = PRIVILEGED_APPS)
    public final void start(@IntRange(from = MIN_INTERVAL_SEC, to = MAX_INTERVAL_SEC)
            int intervalSec, @StartFlags int flags, @Nullable Network underpinnedNetwork) {
        startImpl(intervalSec, flags, underpinnedNetwork);
    }

    /** @hide */
    protected abstract void startImpl(int intervalSec, @StartFlags int flags,
            Network underpinnedNetwork);

    /**
     * Requests that keepalive be stopped. The application must wait for {@link Callback#onStopped}
     * before using the object. See {@link SocketKeepalive}.
     */
    public final void stop() {
        stopImpl();
    }

    /** @hide */
    protected abstract void stopImpl();

    /**
     * Deactivate this {@link SocketKeepalive} and free allocated resources. The instance won't be
     * usable again if {@code close()} is called.
     */
    @Override
    public final void close() {
        stop();
        try {
            mPfd.close();
        } catch (IOException e) {
            // Nothing much can be done.
        }
    }

    /**
     * The callback which app can use to learn the status changes of {@link SocketKeepalive}. See
     * {@link SocketKeepalive}.
     */
    public static class Callback {
        /** The requested keepalive was successfully started. */
        public void onStarted() {}
        /**
         * The keepalive was resumed by the system after being suspended.
         * @hide
         **/
        public void onResumed() {}
        /** The keepalive was successfully stopped. */
        public void onStopped() {}
        /**
         * The keepalive was paused by the system because it's not necessary right now.
         * @hide
         **/
        public void onPaused() {}
        /** An error occurred. */
        public void onError(@ErrorCode int error) {}
        /** The keepalive on a TCP socket was stopped because the socket received data. This is
         * never called for UDP sockets. */
        public void onDataReceived() {}
    }
}
