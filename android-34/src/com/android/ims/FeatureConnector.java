/*
 * Copyright (c) 2019 The Android Open Source Project
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

import android.annotation.IntDef;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsService;
import android.telephony.ims.feature.ImsFeature;

import com.android.ims.internal.IImsServiceFeatureCallback;
import com.android.internal.annotations.VisibleForTesting;
import com.android.telephony.Rlog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Helper class for managing a connection to the ImsFeature manager.
 */
public class FeatureConnector<U extends FeatureUpdates> {
    private static final String TAG = "FeatureConnector";
    private static final boolean DBG = false;

    /**
     * This Connection has become unavailable due to the ImsService being disconnected due to
     * an event such as SIM Swap, carrier configuration change, etc...
     *
     * {@link Listener#connectionReady}  will be called when a new Manager is available.
     */
    public static final int UNAVAILABLE_REASON_DISCONNECTED = 0;

    /**
     * This Connection has become unavailable due to the ImsService moving to the NOT_READY state.
     *
     * {@link Listener#connectionReady}  will be called when the manager moves back to ready.
     */
    public static final int UNAVAILABLE_REASON_NOT_READY = 1;

    /**
     * IMS is not supported on this device. This should be considered a permanent error and
     * a Manager will never become available.
     */
    public static final int UNAVAILABLE_REASON_IMS_UNSUPPORTED = 2;

    /**
     * The server of this information has crashed or otherwise generated an error that will require
     * a retry to connect. This is rare, however in this case, {@link #disconnect()} and
     * {@link #connect()} will need to be called again to recreate the connection with the server.
     * <p>
     * Only applicable if this is used outside of the server's own process.
     */
    public static final int UNAVAILABLE_REASON_SERVER_UNAVAILABLE = 3;

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = "UNAVAILABLE_REASON_", value = {
            UNAVAILABLE_REASON_DISCONNECTED,
            UNAVAILABLE_REASON_NOT_READY,
            UNAVAILABLE_REASON_IMS_UNSUPPORTED,
            UNAVAILABLE_REASON_SERVER_UNAVAILABLE
    })
    public @interface UnavailableReason {}

    /**
     * Factory used to create a new instance of the manager that this FeatureConnector is waiting
     * to connect the FeatureConnection to.
     * @param <U> The Manager that this FeatureConnector has been created for.
     */
    public interface ManagerFactory<U extends FeatureUpdates> {
        /**
         * Create a manager instance, which will connect to the FeatureConnection.
         */
        U createManager(Context context, int phoneId);
    }

    /**
     * Listener interface used by Listeners of FeatureConnector that are waiting for a Manager
     * interface for a specific ImsFeature.
     * @param <U> The Manager that the listener is listening for.
     */
    public interface Listener<U extends FeatureUpdates> {
        /**
         * ImsFeature manager is connected to the underlying IMS implementation.
         */
        void connectionReady(U manager, int subId) throws ImsException;

        /**
         * The underlying IMS implementation is unavailable and can not be used to communicate.
         */
        void connectionUnavailable(@UnavailableReason int reason);
    }

    private final IImsServiceFeatureCallback mCallback = new IImsServiceFeatureCallback.Stub() {

        @Override
        public void imsFeatureCreated(ImsFeatureContainer c, int subId) {
            log("imsFeatureCreated: " + c + ", subId: " + subId);
            synchronized (mLock) {
                mManager.associate(c, subId);
                mManager.updateFeatureCapabilities(c.getCapabilities());
                mDisconnectedReason = null;
            }
            // Notifies executor, so notify outside of lock
            imsStatusChanged(c.getState(), subId);
        }

        @Override
        public void imsFeatureRemoved(@UnavailableReason int reason) {
            log("imsFeatureRemoved: reason=" + reason);
            synchronized (mLock) {
                // only generate new events if the disconnect event isn't the same as before, except
                // for UNAVAILABLE_REASON_SERVER_UNAVAILABLE, which indicates a local issue and
                // each event is actionable.
                if (mDisconnectedReason != null
                        && (mDisconnectedReason == reason
                        && mDisconnectedReason != UNAVAILABLE_REASON_SERVER_UNAVAILABLE)) {
                    log("imsFeatureRemoved: ignore");
                    return;
                }
                mDisconnectedReason = reason;
                // Ensure that we set ready state back to false so that we do not miss setting ready
                // later if the initial state when recreated is READY.
                mLastReadyState = false;
            }
            // Allow the listener to do cleanup while the connection still potentially valid (unless
            // the process crashed).
            mExecutor.execute(() -> mListener.connectionUnavailable(reason));
            mManager.invalidate();
        }

        @Override
        public void imsStatusChanged(int status, int subId) {
            log("imsStatusChanged: status=" + ImsFeature.STATE_LOG_MAP.get(status));
            final U manager;
            final boolean isReady;
            synchronized (mLock) {
                if (mDisconnectedReason != null) {
                    log("imsStatusChanged: ignore");
                    return;
                }
                mManager.updateFeatureState(status);
                manager = mManager;
                isReady = mReadyFilter.contains(status);
                boolean didReadyChange = isReady ^ mLastReadyState;
                mLastReadyState = isReady;
                if (!didReadyChange) {
                    log("imsStatusChanged: ready didn't change, ignore");
                    return;
                }
            }
            mExecutor.execute(() -> {
                try {
                    if (isReady) {
                        notifyReady(manager, subId);
                    } else {
                        notifyNotReady();
                    }
                } catch (ImsException e) {
                    if (e.getCode()
                            == ImsReasonInfo.CODE_LOCAL_IMS_NOT_SUPPORTED_ON_DEVICE) {
                        mListener.connectionUnavailable(UNAVAILABLE_REASON_IMS_UNSUPPORTED);
                    } else {
                        notifyNotReady();
                    }
                }
            });
        }

        @Override
        public void updateCapabilities(long caps) {
            log("updateCapabilities: capabilities=" + ImsService.getCapabilitiesString(caps));
            synchronized (mLock) {
                if (mDisconnectedReason != null) {
                    log("updateCapabilities: ignore");
                    return;
                }
                mManager.updateFeatureCapabilities(caps);
            }
        }
    };

    private final int mPhoneId;
    private final Context mContext;
    private final ManagerFactory<U> mFactory;
    private final Listener<U> mListener;
    private final Executor mExecutor;
    private final Object mLock = new Object();
    private final String mLogPrefix;
    // A List of integers, each corresponding to an ImsFeature.ImsState, that the FeatureConnector
    // will use to call Listener#connectionReady when the ImsFeature that this connector is waiting
    // for changes into one of the states in this list.
    private final List<Integer> mReadyFilter = new ArrayList<>();

    private U mManager;
    // Start in disconnected state;
    private Integer mDisconnectedReason = UNAVAILABLE_REASON_DISCONNECTED;
    // Stop redundant connectionAvailable if the ready filter contains multiple states.
    // Also, do not send the first unavailable until after we have moved to available once.
    private boolean mLastReadyState = false;



    @VisibleForTesting
    public FeatureConnector(Context context, int phoneId, ManagerFactory<U> factory,
            String logPrefix, List<Integer> readyFilter, Listener<U> listener, Executor executor) {
        mContext = context;
        mPhoneId = phoneId;
        mFactory = factory;
        mLogPrefix = logPrefix;
        mReadyFilter.addAll(readyFilter);
        mListener = listener;
        mExecutor = executor;
    }

    /**
     * Start the creation of a connection to the underlying ImsService implementation. When the
     * service is connected, {@link FeatureConnector.Listener#connectionReady} will be
     * called with an active instance.
     *
     * If this device does not support an ImsStack (i.e. doesn't support
     * {@link PackageManager#FEATURE_TELEPHONY_IMS} feature), this method will do nothing.
     */
    public void connect() {
        if (DBG) log("connect");
        if (!isSupported()) {
            mExecutor.execute(() -> mListener.connectionUnavailable(
                    UNAVAILABLE_REASON_IMS_UNSUPPORTED));
            logw("connect: not supported.");
            return;
        }
        synchronized (mLock) {
            if (mManager == null) {
                mManager = mFactory.createManager(mContext, mPhoneId);
            }
        }
        mManager.registerFeatureCallback(mPhoneId, mCallback);
    }

    // Check if this ImsFeature is supported or not.
    private boolean isSupported() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_IMS);
    }

    /**
     * Disconnect from the ImsService Implementation and clean up. When this is complete,
     * {@link FeatureConnector.Listener#connectionUnavailable(int)} will be called one last time.
     */
    public void disconnect() {
        if (DBG) log("disconnect");
        final U manager;
        synchronized (mLock) {
            manager = mManager;
        }
        if (manager == null) return;

        manager.unregisterFeatureCallback(mCallback);
        try {
            mCallback.imsFeatureRemoved(UNAVAILABLE_REASON_DISCONNECTED);
        } catch (RemoteException ignore) {} // local call
    }

    // Should be called on executor
    private void notifyReady(U manager, int subId) throws ImsException {
        try {
            if (DBG) log("notifyReady");
            mListener.connectionReady(manager, subId);
        }
        catch (ImsException e) {
            if(DBG) log("notifyReady exception: " + e.getMessage());
            throw e;
        }
    }

    // Should be called on executor.
    private void notifyNotReady() {
        if (DBG) log("notifyNotReady");
        mListener.connectionUnavailable(UNAVAILABLE_REASON_NOT_READY);
    }

    private void log(String message) {
        Rlog.d(TAG, "[" + mLogPrefix + ", " + mPhoneId + "] " + message);
    }

    private void logw(String message) {
        Rlog.w(TAG, "[" + mLogPrefix + ", " + mPhoneId + "] " + message);
    }
}
