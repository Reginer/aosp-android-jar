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

package com.android.server.location.contexthub;

import static com.android.server.location.contexthub.ContextHubTransactionManager.RELIABLE_MESSAGE_DUPLICATE_DETECTION_TIMEOUT;

import android.annotation.NonNull;
import android.app.AppOpsManager;
import android.content.Context;
import android.hardware.contexthub.EndpointInfo;
import android.hardware.contexthub.ErrorCode;
import android.hardware.contexthub.HubEndpointInfo;
import android.hardware.contexthub.HubMessage;
import android.hardware.contexthub.IContextHubEndpoint;
import android.hardware.contexthub.IContextHubEndpointCallback;
import android.hardware.contexthub.IEndpointCommunication;
import android.hardware.contexthub.Message;
import android.hardware.contexthub.MessageDeliveryStatus;
import android.hardware.contexthub.Reason;
import android.hardware.location.ContextHubTransaction;
import android.hardware.location.IContextHubTransactionCallback;
import android.hardware.location.NanoAppState;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A class that represents a broker for the endpoint registered by the client app. This class
 * manages direct IContextHubEndpoint/IContextHubEndpointCallback API/callback calls.
 *
 * @hide
 */
public class ContextHubEndpointBroker extends IContextHubEndpoint.Stub
        implements IBinder.DeathRecipient, AppOpsManager.OnOpChangedListener {
    private static final String TAG = "ContextHubEndpointBroker";

    /** Message used by noteOp when this client receives a message from an endpoint. */
    private static final String RECEIVE_MSG_NOTE = "ContextHubEndpointMessageDelivery";

    /** The duration of wakelocks acquired during HAL callbacks */
    private static final long WAKELOCK_TIMEOUT_MILLIS = 5 * 1000;

    /** The timeout of open session request */
    @VisibleForTesting static final long OPEN_SESSION_REQUEST_TIMEOUT_SECONDS = 10;

    /*
     * Internal interface used to invoke client callbacks.
     */
    interface CallbackConsumer {
        void accept(@NonNull IContextHubEndpointCallback callback) throws RemoteException;
    }

    /** The context of the service. */
    private final Context mContext;

    /** The shared executor service for handling session operation timeout. */
    private final ScheduledExecutorService mSessionTimeoutExecutor;

    /** The proxy to talk to the Context Hub HAL for endpoint communication. */
    private final IEndpointCommunication mHubInterface;

    /** The manager that registered this endpoint. */
    private final ContextHubEndpointManager mEndpointManager;

    /** Manager used for noting permissions usage of this broker. */
    private final AppOpsManager mAppOpsManager;

    /** Metadata about this endpoint (app-facing container). */
    private final HubEndpointInfo mEndpointInfo;

    /** Metadata about this endpoint (HAL-facing container). */
    private final EndpointInfo mHalEndpointInfo;

    /** The remote callback interface for this endpoint. */
    @NonNull private final IContextHubEndpointCallback mContextHubEndpointCallback;

    /** True if this endpoint is registered with the service/HAL. */
    @GuardedBy("mRegistrationLock")
    private boolean mIsRegistered = false;

    private final Object mRegistrationLock = new Object();

    private final Object mOpenSessionLock = new Object();

    static class Session {
        enum SessionState {
            /* The session is pending acceptance from the remote endpoint. */
            PENDING,
            /* The session is active and can transport messages. */
            ACTIVE,
        };

        private final HubEndpointInfo mRemoteEndpointInfo;

        private SessionState mSessionState = SessionState.PENDING;

        private ScheduledFuture<?> mSessionOpenTimeoutFuture;

        private final boolean mRemoteInitiated;

        /**
         * The set of seq # for pending reliable messages started by this endpoint for this session.
         */
        private final Set<Integer> mPendingSequenceNumbers = new HashSet<>();

        /**
         * Stores the history of received messages that are timestamped. We use a LinkedHashMap to
         * guarantee insertion ordering for easier manipulation of removing expired entries.
         *
         * <p>The key is the sequence number, and the value is the timestamp in milliseconds.
         */
        private final LinkedHashMap<Integer, Long> mRxMessageHistoryMap = new LinkedHashMap<>();

        Session(HubEndpointInfo remoteEndpointInfo, boolean remoteInitiated) {
            mRemoteEndpointInfo = remoteEndpointInfo;
            mRemoteInitiated = remoteInitiated;
        }

        public boolean isRemoteInitiated() {
            return mRemoteInitiated;
        }

        public HubEndpointInfo getRemoteEndpointInfo() {
            return mRemoteEndpointInfo;
        }

        public void setSessionState(SessionState state) {
            mSessionState = state;
        }

        public void setSessionOpenTimeoutFuture(ScheduledFuture<?> future) {
            mSessionOpenTimeoutFuture = future;
        }

        public void cancelSessionOpenTimeoutFuture() {
            if (mSessionOpenTimeoutFuture != null) {
                mSessionOpenTimeoutFuture.cancel(false);
            }
            mSessionOpenTimeoutFuture = null;
        }

        public boolean isActive() {
            return mSessionState == SessionState.ACTIVE;
        }

        public boolean isReliableMessagePending(int sequenceNumber) {
            return mPendingSequenceNumbers.contains(sequenceNumber);
        }

        public void setReliableMessagePending(int sequenceNumber) {
            mPendingSequenceNumbers.add(sequenceNumber);
        }

        public void setReliableMessageCompleted(int sequenceNumber) {
            mPendingSequenceNumbers.remove(sequenceNumber);
        }

        public void forEachPendingReliableMessage(Consumer<Integer> consumer) {
            for (int sequenceNumber : mPendingSequenceNumbers) {
                consumer.accept(sequenceNumber);
            }
        }

        public boolean isInReliableMessageHistory(HubMessage message) {
            if (!message.isResponseRequired()) return false;
            // Clean up the history
            Iterator<Map.Entry<Integer, Long>> iterator =
                    mRxMessageHistoryMap.entrySet().iterator();
            long nowMillis = System.currentTimeMillis();
            while (iterator.hasNext()) {
                Map.Entry<Integer, Long> nextEntry = iterator.next();
                long expiryMillis = RELIABLE_MESSAGE_DUPLICATE_DETECTION_TIMEOUT.toMillis();
                if (nowMillis >= nextEntry.getValue() + expiryMillis) {
                    iterator.remove();
                } else {
                    // Safe to break since LinkedHashMap is insertion-ordered, so the next entry
                    // will have a later timestamp and will not be expired.
                    break;
                }
            }

            return mRxMessageHistoryMap.containsKey(message.getMessageSequenceNumber());
        }

        public void addReliableMessageToHistory(HubMessage message) {
            if (!message.isResponseRequired()) return;
            if (mRxMessageHistoryMap.containsKey(message.getMessageSequenceNumber())) {
                long value = mRxMessageHistoryMap.get(message.getMessageSequenceNumber());
                Log.w(
                        TAG,
                        "Message already exists in history (inserted @ "
                                + value
                                + " ms): "
                                + message);
                return;
            }
            mRxMessageHistoryMap.put(
                    message.getMessageSequenceNumber(), System.currentTimeMillis());
        }
    }

    /** A map between a session ID which maps to its current state. */
    @GuardedBy("mOpenSessionLock")
    private final SparseArray<Session> mSessionMap = new SparseArray<>();

    /** The package name of the app that created the endpoint */
    private final String mPackageName;

    /** The attribution tag of the module that created the endpoint */
    private final String mAttributionTag;

    /** Transaction manager used for sending reliable messages */
    private final ContextHubTransactionManager mTransactionManager;

    /** The PID/UID of the endpoint package providing IContextHubEndpointCallback */
    private final int mPid;

    private final int mUid;

    /** Wakelock held while nanoapp message are in flight to the client */
    private final WakeLock mWakeLock;

    /* package */ ContextHubEndpointBroker(
            Context context,
            IEndpointCommunication hubInterface,
            ContextHubEndpointManager endpointManager,
            EndpointInfo halEndpointInfo,
            @NonNull IContextHubEndpointCallback callback,
            String packageName,
            String attributionTag,
            ContextHubTransactionManager transactionManager,
            ScheduledExecutorService sessionTimeoutExecutor) {
        mContext = context;
        mHubInterface = hubInterface;
        mEndpointManager = endpointManager;
        mEndpointInfo = new HubEndpointInfo(halEndpointInfo);
        mHalEndpointInfo = halEndpointInfo;
        mContextHubEndpointCallback = callback;
        mPackageName = packageName;
        mAttributionTag = attributionTag;
        mTransactionManager = transactionManager;
        mSessionTimeoutExecutor = sessionTimeoutExecutor;

        mPid = Binder.getCallingPid();
        mUid = Binder.getCallingUid();

        mAppOpsManager = context.getSystemService(AppOpsManager.class);
        mAppOpsManager.startWatchingMode(AppOpsManager.OP_NONE, mPackageName, this);

        PowerManager powerManager = context.getSystemService(PowerManager.class);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.setWorkSource(new WorkSource(mUid, mPackageName));
        mWakeLock.setReferenceCounted(true);
    }

    @Override
    public HubEndpointInfo getAssignedHubEndpointInfo() {
        return mEndpointInfo;
    }

    @Override
    @android.annotation.EnforcePermission(android.Manifest.permission.ACCESS_CONTEXT_HUB)
    public int openSession(HubEndpointInfo destination, String serviceDescriptor)
            throws RemoteException {
        super.openSession_enforcePermission();
        if (!isRegistered()) throw new IllegalStateException("Endpoint is not registered");
        if (!hasEndpointPermissions(destination)) {
            throw new SecurityException(
                    "Insufficient permission to open a session with endpoint: " + destination);
        }

        int sessionId = mEndpointManager.reserveSessionId();
        EndpointInfo halEndpointInfo = ContextHubServiceUtil.convertHalEndpointInfo(destination);
        Log.d(TAG, "openSession: sessionId=" + sessionId);

        synchronized (mOpenSessionLock) {
            try {
                mSessionMap.put(sessionId, new Session(destination, false));
                mHubInterface.openEndpointSession(
                        sessionId, halEndpointInfo.id, mHalEndpointInfo.id, serviceDescriptor);
            } catch (RemoteException | IllegalArgumentException | UnsupportedOperationException e) {
                Log.e(TAG, "Exception while calling HAL openEndpointSession", e);
                cleanupSessionResources(sessionId);
                throw e;
            }

            return sessionId;
        }
    }

    @Override
    @android.annotation.EnforcePermission(android.Manifest.permission.ACCESS_CONTEXT_HUB)
    public void closeSession(int sessionId, int reason) throws RemoteException {
        super.closeSession_enforcePermission();
        if (!isRegistered()) throw new IllegalStateException("Endpoint is not registered");
        if (!cleanupSessionResources(sessionId)) {
            throw new IllegalArgumentException(
                    "Unknown session ID in closeSession: id=" + sessionId);
        }
        Log.d(TAG, "closeSession: sessionId=" + sessionId + " reason=" + reason);
        mEndpointManager.halCloseEndpointSession(
                sessionId, ContextHubServiceUtil.toHalReason(reason));
    }

    @Override
    @android.annotation.EnforcePermission(android.Manifest.permission.ACCESS_CONTEXT_HUB)
    public void unregister() {
        super.unregister_enforcePermission();
        synchronized (mOpenSessionLock) {
            // Iterate in reverse since cleanupSessionResources will remove the entry
            for (int i = mSessionMap.size() - 1; i >= 0; i--) {
                int id = mSessionMap.keyAt(i);
                mEndpointManager.halCloseEndpointSessionNoThrow(id, Reason.ENDPOINT_GONE);
                cleanupSessionResources(id);
            }
        }
        synchronized (mRegistrationLock) {
            if (!isRegistered()) {
                Log.w(TAG, "Attempting to unregister when already unregistered");
                return;
            }
            mIsRegistered = false;
            try {
                mHubInterface.unregisterEndpoint(mHalEndpointInfo);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while calling HAL unregisterEndpoint", e);
            }
        }
        mEndpointManager.unregisterEndpoint(mEndpointInfo.getIdentifier().getEndpoint());
        releaseWakeLockOnExit();
    }

    @Override
    @android.annotation.EnforcePermission(android.Manifest.permission.ACCESS_CONTEXT_HUB)
    public void openSessionRequestComplete(int sessionId) {
        super.openSessionRequestComplete_enforcePermission();
        synchronized (mOpenSessionLock) {
            Session info = mSessionMap.get(sessionId);
            if (info == null) {
                throw new IllegalArgumentException(
                        "openSessionRequestComplete for invalid session id=" + sessionId);
            }
            try {
                mHubInterface.endpointSessionOpenComplete(sessionId);
                info.cancelSessionOpenTimeoutFuture();
                info.setSessionState(Session.SessionState.ACTIVE);
            } catch (RemoteException | IllegalArgumentException | UnsupportedOperationException e) {
                Log.e(TAG, "Exception while calling endpointSessionOpenComplete", e);
            }
        }
    }

    @Override
    @android.annotation.EnforcePermission(android.Manifest.permission.ACCESS_CONTEXT_HUB)
    public void sendMessage(
            int sessionId, HubMessage message, IContextHubTransactionCallback callback) {
        super.sendMessage_enforcePermission();
        synchronized (mOpenSessionLock) {
            Session info = mSessionMap.get(sessionId);
            if (info == null) {
                throw new IllegalArgumentException(
                        "sendMessage for invalid session id=" + sessionId);
            }
            if (!info.isActive()) {
                throw new SecurityException(
                        "sendMessage called on inactive session (id= " + sessionId + ")");
            }

            Message halMessage = ContextHubServiceUtil.createHalMessage(message);
            if (callback == null) {
                try {
                    mHubInterface.sendMessageToEndpoint(sessionId, halMessage);
                } catch (RemoteException e) {
                    Log.e(
                            TAG,
                            "Exception while sending message on session "
                                    + sessionId
                                    + ", closing session",
                            e);
                    notifySessionClosedToBoth(sessionId, Reason.UNSPECIFIED);
                }
            } else {
                IContextHubTransactionCallback wrappedCallback =
                        new IContextHubTransactionCallback.Stub() {
                            @Override
                            public void onQueryResponse(int result, List<NanoAppState> appStates)
                                    throws RemoteException {
                                Log.w(TAG, "Unexpected onQueryResponse callback");
                            }

                            @Override
                            public void onTransactionComplete(int result) throws RemoteException {
                                callback.onTransactionComplete(result);
                                if (result != ContextHubTransaction.RESULT_SUCCESS) {
                                    Log.e(
                                            TAG,
                                            "Failed to send reliable message "
                                                    + message
                                                    + ", closing session");
                                    notifySessionClosedToBoth(sessionId, Reason.UNSPECIFIED);
                                }
                            }
                        };
                ContextHubServiceTransaction transaction =
                        mTransactionManager.createSessionMessageTransaction(
                                mHubInterface,
                                sessionId,
                                halMessage,
                                mPackageName,
                                wrappedCallback);
                try {
                    mTransactionManager.addTransaction(transaction);
                    info.setReliableMessagePending(transaction.getMessageSequenceNumber());
                } catch (IllegalStateException e) {
                    Log.e(
                            TAG,
                            "Unable to add a transaction in sendMessageToEndpoint "
                                    + "(session ID = "
                                    + sessionId
                                    + ")",
                            e);
                    transaction.onTransactionComplete(
                            ContextHubTransaction.RESULT_FAILED_SERVICE_INTERNAL_FAILURE);
                }
            }
        }
    }

    @Override
    @android.annotation.EnforcePermission(android.Manifest.permission.ACCESS_CONTEXT_HUB)
    public void sendMessageDeliveryStatus(int sessionId, int messageSeqNumber, byte errorCode) {
        super.sendMessageDeliveryStatus_enforcePermission();
        MessageDeliveryStatus status = new MessageDeliveryStatus();
        status.messageSequenceNumber = messageSeqNumber;
        status.errorCode = errorCode;
        try {
            mHubInterface.sendMessageDeliveryStatusToEndpoint(sessionId, status);
        } catch (RemoteException e) {
            Log.w(
                    TAG,
                    "Exception while sending message delivery status on session " + sessionId,
                    e);
        }
    }

    @Override
    @android.annotation.EnforcePermission(android.Manifest.permission.ACCESS_CONTEXT_HUB)
    public void onCallbackFinished() {
        super.onCallbackFinished_enforcePermission();
        releaseWakeLock();
    }

    /** Invoked when the underlying binder of this broker has died at the client process. */
    @Override
    public void binderDied() {
        if (isRegistered()) {
            unregister();
        }
    }

    @Override
    public void onOpChanged(String op, String packageName) {
        if (!packageName.equals(mPackageName)) {
            Log.w(
                    TAG,
                    "onOpChanged called with invalid package "
                            + packageName
                            + " expected "
                            + mPackageName);
        } else {
            synchronized (mOpenSessionLock) {
                // Iterate in reverse since cleanupSessionResources will remove the entry
                for (int i = mSessionMap.size() - 1; i >= 0; i--) {
                    int id = mSessionMap.keyAt(i);
                    HubEndpointInfo target = mSessionMap.get(id).getRemoteEndpointInfo();
                    if (!hasEndpointPermissions(target)) {
                        notifySessionClosedToBoth(id, Reason.PERMISSION_DENIED);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(mEndpointInfo).append(", ");
        sb.append("package: ").append(mPackageName).append(", ");
        synchronized (mWakeLock) {
            sb.append("wakelock: ").append(mWakeLock);
        }
        synchronized (mOpenSessionLock) {
            if (mSessionMap.size() != 0) {
                sb.append(System.lineSeparator());
                sb.append(" sessions: ");
                sb.append(System.lineSeparator());
            }
            for (int i = 0; i < mSessionMap.size(); i++) {
                int id = mSessionMap.keyAt(i);
                int count = i + 1;
                sb.append(
                        "  "
                                + count
                                + ". id="
                                + id
                                + ", remote:"
                                + mSessionMap.get(id).getRemoteEndpointInfo());
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    /**
     * Registers this endpoints with the Context Hub HAL.
     *
     * @throws RemoteException if the registrations fails with a RemoteException
     */
    /* package */ void register() throws RemoteException {
        synchronized (mRegistrationLock) {
            if (isRegistered()) {
                Log.w(TAG, "Attempting to register when already registered");
            } else {
                mHubInterface.registerEndpoint(mHalEndpointInfo);
                mIsRegistered = true;
            }
        }
    }

    /* package */ void attachDeathRecipient() throws RemoteException {
        mContextHubEndpointCallback.asBinder().linkToDeath(this, 0 /* flags */);
    }

    /** Handle close endpoint callback to the client side */
    /* package */ void onCloseEndpointSession(int sessionId, byte reason) {
        if (!cleanupSessionResources(sessionId)) {
            Log.w(TAG, "Unknown session ID in onCloseEndpointSession: id=" + sessionId);
            return;
        }

        invokeCallback(
                (consumer) ->
                        consumer.onSessionClosed(
                                sessionId, ContextHubServiceUtil.toAppHubEndpointReason(reason)));
    }

    /* package */ void onEndpointSessionOpenComplete(int sessionId) {
        synchronized (mOpenSessionLock) {
            if (!hasSessionId(sessionId)) {
                Log.w(TAG, "Unknown session ID in onEndpointSessionOpenComplete: id=" + sessionId);
                return;
            }
            mSessionMap.get(sessionId).setSessionState(Session.SessionState.ACTIVE);
        }

        invokeCallback((consumer) -> consumer.onSessionOpenComplete(sessionId));
    }

    /* package */ void onMessageReceived(int sessionId, HubMessage message) {
        byte errorCode = onMessageReceivedInternal(sessionId, message);
        if (errorCode != ErrorCode.OK) {
            Log.e(TAG, "Failed to send message to endpoint: " + message + ", closing session");
            if (message.isResponseRequired()) {
                sendMessageDeliveryStatus(sessionId, message.getMessageSequenceNumber(), errorCode);
            } else {
                notifySessionClosedToBoth(
                        sessionId,
                        (errorCode == ErrorCode.PERMISSION_DENIED)
                                ? Reason.PERMISSION_DENIED
                                : Reason.UNSPECIFIED);
            }
        }
    }

    /* package */ void onMessageDeliveryStatusReceived(
            int sessionId, int sequenceNumber, byte errorCode) {
        synchronized (mOpenSessionLock) {
            Session info = mSessionMap.get(sessionId);
            if (info == null || !info.isActive()) {
                Log.w(TAG, "Received delivery status for invalid session: id=" + sessionId);
                return;
            }
            if (!info.isReliableMessagePending(sequenceNumber)) {
                Log.w(TAG, "Received delivery status for unknown seq: " + sequenceNumber);
                return;
            }
            info.setReliableMessageCompleted(sequenceNumber);
        }
        mTransactionManager.onMessageDeliveryResponse(sequenceNumber, errorCode == ErrorCode.OK);
    }

    /* package */ boolean hasSessionId(int sessionId) {
        synchronized (mOpenSessionLock) {
            return mSessionMap.contains(sessionId);
        }
    }

    /* package */ void onHalRestart() {
        synchronized (mRegistrationLock) {
            mIsRegistered = false;
            try {
                register();
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while calling HAL registerEndpoint", e);
            }
        }
        synchronized (mOpenSessionLock) {
            for (int i = mSessionMap.size() - 1; i >= 0; i--) {
                int id = mSessionMap.keyAt(i);
                onCloseEndpointSession(id, Reason.HUB_RESET);
            }
        }
    }

    /* package */ Optional<Byte> onEndpointSessionOpenRequest(
            int sessionId, HubEndpointInfo initiator, String serviceDescriptor) {
        if (!hasEndpointPermissions(initiator)) {
            Log.e(
                    TAG,
                    "onEndpointSessionOpenRequest: "
                            + initiator
                            + " doesn't have permission for "
                            + mEndpointInfo);
            byte reason = Reason.PERMISSION_DENIED;
            onCloseEndpointSession(sessionId, reason);
            return Optional.of(reason);
        }

        // Check & handle error cases for duplicated session id.
        synchronized (mOpenSessionLock) {
            final boolean existingSession;
            final boolean existingSessionActive;

            if (hasSessionId(sessionId)) {
                existingSession = true;
                existingSessionActive = mSessionMap.get(sessionId).isActive();
                Log.w(
                        TAG,
                        "onEndpointSessionOpenRequest: "
                                + "Existing session ID: "
                                + sessionId
                                + ", isActive: "
                                + existingSessionActive);
            } else {
                existingSession = false;
                existingSessionActive = false;
                Session pendingSession = new Session(initiator, true);
                pendingSession.setSessionOpenTimeoutFuture(
                        mSessionTimeoutExecutor.schedule(
                                () -> onEndpointSessionOpenRequestTimeout(sessionId),
                                OPEN_SESSION_REQUEST_TIMEOUT_SECONDS,
                                TimeUnit.SECONDS));
                mSessionMap.put(sessionId, pendingSession);
            }

            if (existingSession) {
                if (existingSessionActive) {
                    // Existing session is already active, call onSessionOpenComplete.
                    openSessionRequestComplete(sessionId);
                }
                // Silence this request. The session open timeout future will handle clean up.
                return Optional.empty();
            }
        }

        boolean success =
                invokeCallback(
                        (consumer) ->
                                consumer.onSessionOpenRequest(
                                        sessionId, initiator, serviceDescriptor));
        byte reason = Reason.UNSPECIFIED;
        if (!success) {
            onCloseEndpointSession(sessionId, reason);
        }
        return success ? Optional.empty() : Optional.of(reason);
    }

    private void onEndpointSessionOpenRequestTimeout(int sessionId) {
        synchronized (mOpenSessionLock) {
            Session s = mSessionMap.get(sessionId);
            if (s == null || s.isActive()) {
                return;
            }
            Log.w(
                    TAG,
                    "onEndpointSessionOpenRequestTimeout: " + "clean up session, id: " + sessionId);
            cleanupSessionResources(sessionId);
            mEndpointManager.halCloseEndpointSessionNoThrow(sessionId, Reason.TIMEOUT);
        }
    }

    private byte onMessageReceivedInternal(int sessionId, HubMessage message) {
        synchronized (mOpenSessionLock) {
            if (!isSessionActive(sessionId)) {
                Log.e(
                        TAG,
                        "Dropping message for inactive session (id="
                                + sessionId
                                + ") with message: "
                                + message);
                return ErrorCode.PERMANENT_ERROR;
            }
            HubEndpointInfo remote = mSessionMap.get(sessionId).getRemoteEndpointInfo();
            if (mSessionMap.get(sessionId).isInReliableMessageHistory(message)) {
                Log.e(TAG, "Dropping duplicate message: " + message);
                return ErrorCode.TRANSIENT_ERROR;
            }

            try {
                Binder.withCleanCallingIdentity(
                        () -> {
                            if (!notePermissions(remote)) {
                                throw new RuntimeException(
                                        "Dropping message from "
                                                + remote
                                                + ". "
                                                + mPackageName
                                                + " doesn't have permission");
                            }
                        });
            } catch (RuntimeException e) {
                Log.e(TAG, e.getMessage());
                return ErrorCode.PERMISSION_DENIED;
            }

            boolean success =
                    invokeCallback((consumer) -> consumer.onMessageReceived(sessionId, message));
            if (success) {
                mSessionMap.get(sessionId).addReliableMessageToHistory(message);
            }
            return success ? ErrorCode.OK : ErrorCode.TRANSIENT_ERROR;
        }
    }

    /**
     * Cleans up resources related to a session with the provided ID.
     *
     * @param sessionId The session ID to clean up resources for
     * @return false if the session ID was invalid
     */
    private boolean cleanupSessionResources(int sessionId) {
        synchronized (mOpenSessionLock) {
            Session info = mSessionMap.get(sessionId);
            if (info != null) {
                if (!info.isRemoteInitiated()) {
                    mEndpointManager.returnSessionId(sessionId);
                }
                info.forEachPendingReliableMessage(
                        (sequenceNumber) -> {
                            mTransactionManager.onMessageDeliveryResponse(
                                    sequenceNumber, /* success= */ false);
                        });
                mSessionMap.remove(sessionId);
            }
            return info != null;
        }
    }

    /**
     * @param sessionId The ID of the session to check
     * @return true if the session with the given ID is currently active
     */
    private boolean isSessionActive(int sessionId) {
        synchronized (mOpenSessionLock) {
            return hasSessionId(sessionId) && mSessionMap.get(sessionId).isActive();
        }
    }

    /**
     * @param targetEndpointInfo The target endpoint to check permissions for
     * @return true if this endpoint has sufficient permission to the provided target endpoint
     */
    private boolean hasEndpointPermissions(HubEndpointInfo targetEndpointInfo) {
        Collection<String> requiredPermissions = targetEndpointInfo.getRequiredPermissions();
        return ContextHubServiceUtil.hasPermissions(mContext, mPid, mUid, requiredPermissions);
    }

    private void acquireWakeLock() {
        Binder.withCleanCallingIdentity(
                () -> {
                    if (isRegistered()) {
                        mWakeLock.acquire(WAKELOCK_TIMEOUT_MILLIS);
                    }
                });
    }

    private void releaseWakeLock() {
        Binder.withCleanCallingIdentity(
                () -> {
                    if (mWakeLock.isHeld()) {
                        try {
                            mWakeLock.release();
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Releasing the wakelock fails - ", e);
                        }
                    }
                });
    }

    private void releaseWakeLockOnExit() {
        Binder.withCleanCallingIdentity(
                () -> {
                    while (mWakeLock.isHeld()) {
                        try {
                            mWakeLock.release();
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Releasing the wakelock for all acquisitions fails - ", e);
                            break;
                        }
                    }
                });
    }

    /**
     * Invokes a callback and acquires a wakelock.
     *
     * @param consumer The callback invoke
     * @return false if the callback threw a RemoteException
     */
    private boolean invokeCallback(CallbackConsumer consumer) {
        acquireWakeLock();
        try {
            consumer.accept(mContextHubEndpointCallback);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while calling endpoint callback", e);
            releaseWakeLock();
            return false;
        }
        return true;
    }

    private boolean isRegistered() {
        synchronized (mRegistrationLock) {
            return mIsRegistered;
        }
    }

    /**
     * Utility to call notePermissions for e.g. when processing a message from a given endpoint for
     * this broker.
     *
     * @param endpoint The endpoint to check permissions for this broker.
     */
    private boolean notePermissions(HubEndpointInfo endpoint) {
        return ContextHubServiceUtil.notePermissions(
                mAppOpsManager,
                mUid,
                mPackageName,
                mAttributionTag,
                endpoint.getRequiredPermissions(),
                RECEIVE_MSG_NOTE
                        + "-0x"
                        + Long.toHexString(endpoint.getIdentifier().getHub())
                        + "-0x"
                        + Long.toHexString(endpoint.getIdentifier().getEndpoint()));
    }

    /**
     * Notifies to both the HAL and the app that a session has been closed.
     *
     * @param sessionId The ID of the session that was closed
     * @param halReason The HAL reason for closing the session
     */
    private void notifySessionClosedToBoth(int sessionId, byte halReason) {
        Log.d(TAG, "notifySessionClosedToBoth: sessionId=" + sessionId + ", reason=" + halReason);
        mEndpointManager.halCloseEndpointSessionNoThrow(sessionId, halReason);
        onCloseEndpointSession(sessionId, halReason);
    }
}
