/*
 * Copyright (C) 2024 The Android Open Source Project
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
package android.widget.photopicker;

import static android.content.Context.BIND_AUTO_CREATE;

import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.RequiresApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * Interface to get instance of {@link EmbeddedPhotoPickerProvider} class to request a new
 * {@link EmbeddedPhotoPickerSession}.
 *
 * <p> This class creates and maintains the binding/unbinding to embedded photopicker service
 * on behalf of the caller. It makes IPC call to the service using binder
 * {@link IEmbeddedPhotoPicker} to get a new session.
 *
 * @see EmbeddedPhotoPickerProvider
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@FlaggedApi("com.android.providers.media.flags.enable_embedded_photopicker")
public class EmbeddedPhotoPickerProviderFactory {

    private EmbeddedPhotoPickerProviderFactory() {}
    /**
     * Method to maintain the count of currently opened photopicker sessions.
     *
     * To be overridden by child class
     *
     * @hide
     */
    void onSessionClosed(@NonNull EmbeddedPhotoPickerClient client) {}

    /**
     * Returns an implementation of {@link EmbeddedPhotoPickerProvider} class.
     */
    @NonNull
    public static EmbeddedPhotoPickerProvider create(@NonNull Context context) {
        return new RuntimeEmbeddedPhotoPickerProvider(context);
    }

    /**
     * Implementation of {@link EmbeddedPhotoPickerProviderFactory} and
     * {@link EmbeddedPhotoPickerProvider}.
     *
     * @hide
     */
    private static final class RuntimeEmbeddedPhotoPickerProvider
            extends EmbeddedPhotoPickerProviderFactory
            implements EmbeddedPhotoPickerProvider {

        private static final String TAG = "EmbeddedProviderFactory";
        private static final int OPEN_SESSION_ATTEMPTS_LIMIT = 5;

        private static final boolean DEBUG = false;

        // Client context object
        private Context mContext;
        // Client package name
        private String mPackageName;
        /**
         * Active ServiceConnection object. At any given point, there will only be one service
         * connection to photopicker and not more.
         */
        private ServiceConnectionHandler mConnection;
        // Photopicker service binder delegate
        private IEmbeddedPhotoPicker mEmbeddedPhotopicker;

        /**
         * EmbeddedPhotoPickerSerialExecutor for sequentially running all operations.
         * Any state mutations (service binding states, updating client entry etc) will always be
         * done by a task running in this executor. All the method with suffix Serialized
         * are being run under this same executor.
         */
        private final EmbeddedPhotoPickerSerialExecutor mSerialExecutor =
                new EmbeddedPhotoPickerSerialExecutor(Runnable::run);

        /**
         * This Queue (FIFO) will maintain all open session calls that were requested when service
         * binding was in progress. These all tasks will be performed or resumed when
         * {@link #mConnection}#onServiceConnected() is successfully invoked by system.
         */
        @NonNull
        private Queue<OpenSessionRequest> mPendingOpenSessionRequests = new ArrayDeque<>();

        /**
         * Map of client callback to its corresponding wrapper of successfully created Session
         * objects. Pair is added when {@link #openSession} is called and removed upon
         * {@link #onSessionClosed}
         */
        private Map<EmbeddedPhotoPickerClient, EmbeddedPhotoPickerClientWrapper>
                mClientCallbackToWrapperMap = new HashMap<>();

        private RuntimeEmbeddedPhotoPickerProvider(@NonNull Context context) {
            mContext = context;
            mPackageName = context.getPackageName();
            mConnection = new ServiceConnectionHandler(mContext, this);
        }

        /**
         * Implementation of {@link EmbeddedPhotoPickerProvider#openSession}.
         *
         * <p> Submits incoming request to {@link #mSerialExecutor}
         */
        @Override
        public void openSession(@NonNull IBinder hostToken, int displayId, int width, int height,
                @NonNull EmbeddedPhotoPickerFeatureInfo featureInfo,
                @NonNull Executor clientExecutor, @NonNull EmbeddedPhotoPickerClient callback) {
            requireNonNull(hostToken, "hostToken must not be null");
            requireNonNull(featureInfo, "featureInfo must not be null");
            requireNonNull(clientExecutor, "clientExecutor must not be null");
            requireNonNull(callback, "clientCallback must not be null");

            mSerialExecutor.execute(() ->
                    openSessionSerialized(hostToken, displayId, width, height, featureInfo,
                            clientExecutor, callback)
            );
        }

        private void openSessionSerialized(@NonNull IBinder hostToken, int displayId,
                int width, int height,
                @NonNull EmbeddedPhotoPickerFeatureInfo featureInfo,
                @NonNull Executor clientExecutor, @NonNull EmbeddedPhotoPickerClient callback) {

            // Create wrapper of IEmbeddedPhotopickerClient implementation that is sent to service.
            EmbeddedPhotoPickerClientWrapper clientWrapper =
                    new EmbeddedPhotoPickerClientWrapper(this,
                            callback, new EmbeddedPhotoPickerSerialExecutor(clientExecutor));
            mClientCallbackToWrapperMap.put(callback, clientWrapper);

            OpenSessionRequest sessionRequest =
                    new OpenSessionRequest(hostToken,
                            displayId, width, height, clientWrapper, featureInfo);
            if (DEBUG) {
                Log.d(TAG, "openSession request received with params = " + sessionRequest);
            }

            // Ensure that we never pass a stale ServiceConnection when we bindService.
            ensureValidServiceConnectionExistsSerialized();

            // If connection is alive, execute openSession call on remote delegate.
            // If not, try binding service and save request in a queue for future execution
            // when service gets connected.
            if (mConnection.isConnected()) {
                openSessionInternalSerialized(sessionRequest);
            } else {
                bindServiceAndSaveRequestSerialized(sessionRequest);
            }
        }

        /**
         * Creates a new {@link ServiceConnectionHandler} if existing connection is no longer valid.
         */
        private void ensureValidServiceConnectionExistsSerialized() {
            // If the previous connection had died, create a new one.
            if (!mConnection.isConnectionValid()) {
                mConnection = new ServiceConnectionHandler(mContext, this);
            }
        }

        private void bindServiceAndSaveRequestSerialized(OpenSessionRequest sessionRequest) {
            // Apart from {@link #openSessionLocked}, this method is also invoke from
            // {@link #onServiceDisconnectedLocked} for retrying pending requests.
            // So we have to ensure that we create a new ServiceConnection in that case.
            ensureValidServiceConnectionExistsSerialized();
            mConnection.bindServiceSerialized();
            if (mConnection.isBindRequested()) {
                // Failure in binding service is terminal error, so add request to queue only if
                // we were able to successfully send binding request to system.
                sessionRequest.mTotalOpenSessionAttempts += 1;
                mPendingOpenSessionRequests.add(sessionRequest);
            }
        }

        private void openSessionInternalSerialized(OpenSessionRequest sessionRequest) {
            try {
                mEmbeddedPhotopicker.openSession(
                        mPackageName,
                        sessionRequest.mHostToken,
                        sessionRequest.mDisplayId,
                        sessionRequest.mWidth,
                        sessionRequest.mHeight,
                        sessionRequest.mFeatureInfo,
                        sessionRequest.mClientCallbackWrapper);
            } catch (DeadObjectException e) {
                if (DEBUG) {
                    Log.d(TAG, "Couldn't make call to remote delegate. Retrying.");
                }
                mConnection.disposeLocked();
                mEmbeddedPhotopicker = null;
                if (sessionRequest.mTotalOpenSessionAttempts < OPEN_SESSION_ATTEMPTS_LIMIT) {
                    bindServiceAndSaveRequestSerialized(sessionRequest);
                } else {
                    reportSessionErrorLocked(sessionRequest.mClientCallbackWrapper,
                            /*cause*/ new RuntimeException("Unable to get valid remote delegate."
                            + "Please request a new session"));
                }
            } catch (RemoteException e) {
                reportSessionErrorLocked(sessionRequest.mClientCallbackWrapper,
                        new RemoteException("Remote delegate is Invalid! "
                                + "Failed to open a session"));
            }
        }

        @Override
        public void onSessionClosed(@NonNull EmbeddedPhotoPickerClient client) {
            mSerialExecutor.execute(() -> onSessionClosedSerialized(client));
        }

        private void onSessionClosedSerialized(@NonNull EmbeddedPhotoPickerClient client) {
            if (mClientCallbackToWrapperMap.remove(client) == null) {
                // Return because we have already accounted session closure for given client by
                // some other event notification.
                return;
            }

            if (mClientCallbackToWrapperMap.isEmpty()) {
                if (DEBUG) {
                    Log.d(TAG, "Unbinding service as no active session open");
                }
                mEmbeddedPhotopicker = null;
                mConnection.unbindSerialized();
            }
        }

        /**
         * Handles connection to photopicker service.
         */
        private static class ServiceConnectionHandler implements ServiceConnection {
            // Indicates if service is connected or not.
            private boolean mIsConnected;
            // Indicates if we have already requested bindService
            private boolean mIsBindRequested;
            /**
             * Indicates that the service is connected via this instance of ServiceConnection.
             * This is marked as false when service gets disconnected abruptly or when
             * we unbind service manually. This is used by methods in
             * {@link RuntimeEmbeddedPhotoPickerProvider}
             */
            private boolean mIsConnectionValid = true;

            // Client context.
            private Context mContext;

            /**
             * Reference to the outer {@link RuntimeEmbeddedPhotoPickerProvider} class.
             * This is populated when new ServiceConnection is setup and marked as null when
             * the ServiceConnection is disposed. This is done so that any pending tasks submitted
             * by system in EmbeddedPhotoPickerSerialExecutor are not executed if connection
             * is no longer valid.
             */
            private RuntimeEmbeddedPhotoPickerProvider mPhotopickerProvider;

            private static final String ACTION_EMBEDDED_PHOTOPICKER_SERVICE =
                    "com.android.photopicker.core.embedded.EmbeddedService.BIND";
            private final Intent mIntent;

            ServiceConnectionHandler(Context context,
                    RuntimeEmbeddedPhotoPickerProvider photopickerProvider) {
                mContext = context;
                mPhotopickerProvider = photopickerProvider;
                mIntent = new Intent(ACTION_EMBEDDED_PHOTOPICKER_SERVICE);
                mIntent.setPackage(getExplicitPackageName());
            }

            /**
             * Get an explicit package name that limit the component {@link #mIntent} intent will
             * resolve to.
             */
            private String getExplicitPackageName() {
                // Use {@link PackageManager.MATCH_SYSTEM_ONLY} flag to match services only
                // by system apps.
                List<ResolveInfo> services =
                        mContext.getPackageManager().queryIntentServices(
                                mIntent, PackageManager.MATCH_SYSTEM_ONLY);

                // There should only be one matching service.
                if (services == null || services.isEmpty()) {
                    throw new RuntimeException("Failed to find embedded photopicker service!");
                } else if (services.size() != 1) {
                    throw new RuntimeException(String.format(
                            "Found more than 1 (%d) service by intent %s!",
                            services.size(), ACTION_EMBEDDED_PHOTOPICKER_SERVICE));
                }

                // Check that the service info contains package name.
                ServiceInfo embeddedService = services.get(0).serviceInfo;
                if (embeddedService != null && embeddedService.packageName != null) {
                    return embeddedService.packageName;
                } else {
                    throw new RuntimeException("Failed to get valid service info or package info!");
                }
            }

            // Reset states
            public void disposeLocked() {
                if (DEBUG) {
                    Log.d(TAG, "Disposing previous states");
                }
                mPhotopickerProvider = null;
                mIsConnected = false;
                mIsBindRequested = false;
                mIsConnectionValid = false;
                mContext = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (DEBUG) {
                    Log.d(TAG, "Service is now connected");
                }
                if (mPhotopickerProvider != null) {
                    mIsConnected = true;
                    mIsBindRequested = false;
                    mPhotopickerProvider.onServiceConnectedSerialized(service);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                if (DEBUG) {
                    Log.d(TAG, "Service is disconnected");
                }
                if (mPhotopickerProvider != null) {
                    mIsConnected = false;
                    mIsBindRequested = false;
                    mPhotopickerProvider.onServiceDisconnectedSerialized();
                }
            }

            @Override
            public void onBindingDied(ComponentName name) {
                if (DEBUG) {
                    Log.d(TAG, "Binding to service died");
                }
                if (mPhotopickerProvider != null) {
                    mIsConnected = false;
                    mIsBindRequested = false;
                    mPhotopickerProvider.onBindingDiedSerialized();
                }
            }

            public void bindServiceSerialized() {
                if (mPhotopickerProvider != null && !mIsBindRequested) {
                    mIsBindRequested = mPhotopickerProvider
                            .performBindServiceSerialized(mIntent);
                }
            }

            public void unbindSerialized() {
                mContext.unbindService(this);
                disposeLocked();
            }

            public boolean isConnected() {
                return mIsConnected;
            }

            public boolean isBindRequested() {
                return mIsBindRequested;
            }

            public boolean isConnectionValid() {
                return mIsConnectionValid;
            }
        }

        private boolean performBindServiceSerialized(Intent intent) {
            boolean bindRequested;
            try {
                // Send request to bind service on {@link #mEmbeddedPhotoPickerSerialExecutor}.
                // The system will post events on the same (service connection/disconnection).
                bindRequested = mContext
                        .bindService(intent, BIND_AUTO_CREATE, mSerialExecutor, mConnection);

                // Notify all clients that binding was unsuccessful and they should request
                // a new Session.
                if (!bindRequested) {
                    while (!mPendingOpenSessionRequests.isEmpty()) {
                        reportSessionErrorLocked(
                                mPendingOpenSessionRequests.remove().mClientCallbackWrapper,
                                /*cause*/ new RuntimeException("Unable to bind photopicker service."
                                        + "Please request a new session"));
                    }
                }
            } catch (SecurityException e) {
                bindRequested = false;
                // Notify client that binding was unsuccessful for a given request and they should
                // request a new Session.
                while (!mPendingOpenSessionRequests.isEmpty()) {
                    reportSessionErrorLocked(
                            mPendingOpenSessionRequests.remove().mClientCallbackWrapper,
                            new SecurityException("Unable to bind photopicker service. "
                                    + "Please request a new session"));
                }
            }
            return bindRequested;
        }

        private void onServiceDisconnectedSerialized() {
            mEmbeddedPhotopicker = null;

            // Two set of operations are handled here.
            // 1.) Session object(s) were successfully created and sent to client.
            // Those sessions would have been released with service disconnection.
            // So for this case, report onSessionError to client so they can clean up session
            // on their end.
            // 2.) Some openSessionRequest(s) might already be in queue.
            // Retry executing those requests by rebinding the service.
            Iterator<EmbeddedPhotoPickerClientWrapper> iterator =
                    mClientCallbackToWrapperMap.values().iterator();
            while (iterator.hasNext()) {
                EmbeddedPhotoPickerClientWrapper clientWrapper = iterator.next();
                reportSessionErrorLocked(clientWrapper,
                        new RemoteException("Service Disconnected. Close the Session"));
            }

            mConnection.bindServiceSerialized();
        }

        private void onServiceConnectedSerialized(IBinder service) {
            mEmbeddedPhotopicker = IEmbeddedPhotoPicker.Stub.asInterface(service);

            // Execute all pending requests in the queue,
            while (!mPendingOpenSessionRequests.isEmpty()) {
                openSessionInternalSerialized(mPendingOpenSessionRequests.remove());
            }
        }

        private void onBindingDiedSerialized() {
            // Unbind service and rebind..
            mConnection.unbindSerialized();
            mConnection.bindServiceSerialized();
        }

        /**
         * Reports to client that due to some issue, the Session request has failed.
         * The error is wrapped in {@link android.os.ParcelableException} with message.
         *
         * @param clientWrapper client callback
         * @param cause actual cause of this exception, can also be null
         */
        private static void reportSessionErrorLocked(
                EmbeddedPhotoPickerClientWrapper clientWrapper,
                Throwable cause) {
            clientWrapper.onSessionError(
                    new ParcelableException(cause));
        }

        /**
         * Data class for storing all the details of openSession request from the caller.
         */
        private static class OpenSessionRequest {
            public final IBinder mHostToken;
            public final int mDisplayId;
            public final int mWidth;
            public final int mHeight;
            public final EmbeddedPhotoPickerClientWrapper mClientCallbackWrapper;
            public final EmbeddedPhotoPickerFeatureInfo mFeatureInfo;
            public int mTotalOpenSessionAttempts = 0;

            private OpenSessionRequest(IBinder hostToken, int displayId, int width,
                    int height, EmbeddedPhotoPickerClientWrapper clientCallbackWrapper,
                    EmbeddedPhotoPickerFeatureInfo featureInfo) {
                this.mHostToken = hostToken;
                this.mDisplayId = displayId;
                this.mWidth = width;
                this.mHeight = height;
                this.mClientCallbackWrapper = clientCallbackWrapper;
                this.mFeatureInfo = featureInfo;
            }

            @Override
            public String toString() {
                return "OpenSessionRequest{"
                        + "mHostToken=" + mHostToken
                        + ", mDisplayId=" + mDisplayId
                        + ", mWidth=" + mWidth
                        + ", mHeight=" + mHeight
                        + ", mClientCallbackWrapper=" + mClientCallbackWrapper
                        + ", mFeatureInfo=" + mFeatureInfo
                        + '}';
            }
        }
    }
}
