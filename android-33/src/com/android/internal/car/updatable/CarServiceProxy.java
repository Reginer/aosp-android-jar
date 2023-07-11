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

package com.android.internal.car.updatable;

import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_STARTING;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_STOPPED;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_STOPPING;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_SWITCHING;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_UNLOCKED;
import static com.android.car.internal.common.CommonConstants.USER_LIFECYCLE_EVENT_TYPE_UNLOCKING;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.car.ICarResultReceiver;
import android.car.builtin.os.UserManagerHelper;
import android.car.builtin.util.Slogf;
import android.car.builtin.util.TimingsTraceLog;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.SparseArray;
import android.util.SparseIntArray;

import com.android.car.internal.ICarSystemServerClient;
import com.android.car.internal.common.CommonConstants.UserLifecycleEventType;
import com.android.car.internal.util.DebugUtils;
import com.android.car.internal.util.IndentingPrintWriter;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * Manages CarService operations requested by CarServiceHelperService.
 *
 * <p>
 * It is used to send and re-send binder calls to CarService when it connects and dies & reconnects.
 * It does not simply queue the operations, because it needs to "replay" some of them on every
 * reconnection.
 */
final class CarServiceProxy {

    /*
     * The logic of re-queue:
     *
     * There are two sparse array - mLastUserLifecycle and mPendingOperations
     *
     * First sparse array - mLastUserLifecycle - is to keep track of the life-cycle events for each
     * user. It would have the last life-cycle event of each running user (typically user 0 and the
     * current user). All life-cycle events seen so far would be replayed on connection and
     * reconnection.
     *
     * Second sparse array - mPendingOperations - would keep all the non-life-cycle events related
     * operations, which are represented by PendintOperation and PendingOperationId.
     * Most operations (like initBootUser and preCreateUsers) just need to be sent only, but some
     * need to be queued (like onUserRemoved).
     */

    // Operation ID for each non life-cycle event calls
    // NOTE: public because of DebugUtils
    public static final int PO_INIT_BOOT_USER = 0;
    public static final int PO_ON_USER_REMOVED = 1;
    public static final int PO_ON_FACTORY_RESET = 2;

    @IntDef(prefix = { "PO_" }, value = {
            PO_INIT_BOOT_USER,
            PO_ON_USER_REMOVED,
            PO_ON_FACTORY_RESET
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PendingOperationId{}

    @VisibleForTesting
    static final String TAG = CarServiceProxy.class.getSimpleName();

    private static final long TRACE_TAG_SYSTEM_SERVER = 1L << 19;
    private static final boolean DBG = false;

    private static final int USER_SYSTEM = UserHandle.SYSTEM.getIdentifier();

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private boolean mCarServiceCrashed;
    @UserIdInt
    @GuardedBy("mLock")
    private int mLastSwitchedUser = UserManagerHelper.USER_NULL;
    @UserIdInt
    @GuardedBy("mLock")
    private int mPreviousUserOfLastSwitchedUser = UserManagerHelper.USER_NULL;
    // Key: user id, value: life-cycle
    @GuardedBy("mLock")
    private final SparseIntArray mLastUserLifecycle = new SparseIntArray();
    // Key: @PendingOperationId, value: PendingOperation
    @GuardedBy("mLock")
    private final SparseArray<PendingOperation> mPendingOperations = new SparseArray<>();

    @GuardedBy("mLock")
    private ICarSystemServerClient mCarService;

    private final CarServiceHelperServiceUpdatableImpl mCarServiceHelperServiceUpdatableImpl;
    private final UserMetrics mUserMetrics = new UserMetrics();
    @GuardedBy("mLock")
    private UserHandle mInitialUser;

    CarServiceProxy(CarServiceHelperServiceUpdatableImpl carServiceHelperServiceUpdatableImpl) {
        mCarServiceHelperServiceUpdatableImpl = carServiceHelperServiceUpdatableImpl;
    }

    /**
     * Handles new CarService Connection.
     */
    void handleCarServiceConnection(ICarSystemServerClient carService) {
        Slogf.i(TAG, "CarService connected.");
        TimingsTraceLog t = newTimingsTraceLog();
        t.traceBegin("handleCarServiceConnection");
        synchronized (mLock) {
            mCarService = carService;
            mCarServiceCrashed = false;
            runQueuedOperationLocked(PO_INIT_BOOT_USER);
            runQueuedOperationLocked(PO_ON_USER_REMOVED);
            runQueuedOperationLocked(PO_ON_FACTORY_RESET);
        }
        sendLifeCycleEvents();
        sendInitialUser();
        t.traceEnd();
    }

    private void sendInitialUser() {
        UserHandle initialUser;
        ICarSystemServerClient carService;
        synchronized (mLock) {
            initialUser = mInitialUser;
            carService = mCarService;
        }
        if (initialUser != null && carService != null) {
            try {
                carService.setInitialUser(initialUser);
            } catch (RemoteException e) {
                Slogf.w(TAG, "RemoteException from car service while calling setInitialUser.", e);
            }
        } else {
            Slogf.i(TAG, "Didn't send Initial User, User: %s, CarService: %s", initialUser,
                    carService);
        }
    }

    void saveInitialUser(UserHandle user) {
        synchronized (mLock) {
            if (user != null || user.getIdentifier() != UserManagerHelper.USER_NULL) {
                mInitialUser = user;
            }
        }
    }

    @GuardedBy("mLock")
    private void runQueuedOperationLocked(@PendingOperationId int operationId) {
        PendingOperation pendingOperation = mPendingOperations.get(operationId);
        if (pendingOperation != null) {
            runLocked(operationId, pendingOperation.value);
            return;
        }
        if (DBG) {
            Slogf.d(TAG, "No queued operation of type " + pendingOperationToString(operationId));
        }
    }

    private void sendLifeCycleEvents() {
        int lastSwitchedUser;
        SparseIntArray lastUserLifecycle;

        synchronized (mLock) {
            lastSwitchedUser = mLastSwitchedUser;
            lastUserLifecycle = mLastUserLifecycle.clone();
        }

        // Send user0 events first
        int user0Lifecycle = lastUserLifecycle.get(USER_SYSTEM);
        boolean user0IsCurrent = lastSwitchedUser == USER_SYSTEM;
        // If user0Lifecycle is 0, then no life-cycle event received yet.
        if (user0Lifecycle != 0) {
            sendAllLifecyleToUser(USER_SYSTEM, user0Lifecycle,
                    user0IsCurrent);
        }
        lastUserLifecycle.delete(USER_SYSTEM);

        // Send current user events next
        if (!user0IsCurrent) {
            int currentUserLifecycle = lastUserLifecycle.get(lastSwitchedUser);
            // If currentUserLifecycle is 0, then no life-cycle event received yet.
            if (currentUserLifecycle != 0) {
                sendAllLifecyleToUser(lastSwitchedUser, currentUserLifecycle,
                        /* isCurrentUser= */ true);
            }
        }

        lastUserLifecycle.delete(lastSwitchedUser);

        // Send all other users' events
        for (int i = 0; i < lastUserLifecycle.size(); i++) {
            int userId = lastUserLifecycle.keyAt(i);
            int lifecycle = lastUserLifecycle.valueAt(i);
            sendAllLifecyleToUser(userId, lifecycle, /* isCurrentUser= */ false);
        }
    }

    private void sendAllLifecyleToUser(@UserIdInt int userId, int lifecycle,
            boolean isCurrentUser) {
        if (DBG) {
            Slogf.d(TAG, "sendAllLifecyleToUser, user:" + userId + " lifecycle:" + lifecycle);
        }
        if (lifecycle >= USER_LIFECYCLE_EVENT_TYPE_STARTING) {
            sendUserLifecycleEventInternal(USER_LIFECYCLE_EVENT_TYPE_STARTING,
                    UserManagerHelper.USER_NULL, userId);
        }

        if (isCurrentUser && userId != USER_SYSTEM) {
            synchronized (mLock) {
                sendUserLifecycleEventInternal(USER_LIFECYCLE_EVENT_TYPE_SWITCHING,
                        mPreviousUserOfLastSwitchedUser, userId);
            }
        }

        if (lifecycle >= USER_LIFECYCLE_EVENT_TYPE_UNLOCKING) {
            sendUserLifecycleEventInternal(USER_LIFECYCLE_EVENT_TYPE_UNLOCKING,
                    UserManagerHelper.USER_NULL, userId);
        }

        if (lifecycle >= USER_LIFECYCLE_EVENT_TYPE_UNLOCKED) {
            sendUserLifecycleEventInternal(USER_LIFECYCLE_EVENT_TYPE_UNLOCKED,
                    UserManagerHelper.USER_NULL, userId);
        }
    }

    /**
     * Initializes boot user.
     */
    void initBootUser() {
        if (DBG) Slogf.d(TAG, "initBootUser()");

        saveOrRun(PO_INIT_BOOT_USER);
    }

    // TODO(b/173664653): add unit test
    /**
     * Callback to indifcate the given user was removed.
     */
    void onUserRemoved(@NonNull UserHandle user) {
        if (DBG) Slogf.d(TAG, "onUserRemoved(): " + user);

        saveOrRun(PO_ON_USER_REMOVED, user);
    }

    // TODO(b/173664653): add unit test
    /**
     * Callback to ask user to confirm if it's ok to factory reset the device.
     */
    void onFactoryReset(@NonNull ICarResultReceiver callback) {
        if (DBG) Slogf.d(TAG, "onFactoryReset(): " + callback);

        saveOrRun(PO_ON_FACTORY_RESET, callback);
    }

    private void saveOrRun(@PendingOperationId int operationId) {
        saveOrRun(operationId, /* value= */ null);
    }

    private void saveOrRun(@PendingOperationId int operationId, @Nullable Object value) {
        synchronized (mLock) {
            if (mCarService == null) {
                if (DBG) {
                    Slogf.d(TAG, "CarService null. Operation "
                            + pendingOperationToString(operationId)
                            + (value == null ? "" : "(" + value + ")") + " deferred.");
                }
                savePendingOperationLocked(operationId, value);
                return;
            }
            if (operationId == PO_ON_FACTORY_RESET) {
                // Must always persist it, so it's sent again if CarService is crashed before
                // the next reboot or suspension-to-ram
                savePendingOperationLocked(operationId, value);
            }
            runLocked(operationId, value);
        }
    }

    @GuardedBy("mLock")
    private void runLocked(@PendingOperationId int operationId, @Nullable Object value) {
        if (DBG) {
            Slogf.d(TAG, "runLocked(): " + pendingOperationToString(operationId) + "/" + value);
        }
        try {
            if (isServiceCrashedLoggedLocked(operationId)) {
                return;
            }
            sendCarServiceActionLocked(operationId, value);
            if (operationId == PO_ON_FACTORY_RESET) {
                if (DBG) Slogf.d(TAG, "NOT removing " + pendingOperationToString(operationId));
                return;
            }
            if (DBG) Slogf.d(TAG, "removing " + pendingOperationToString(operationId));
            mPendingOperations.delete(operationId);
        } catch (RemoteException e) {
            Slogf.w(TAG, "RemoteException from car service", e);
            handleCarServiceCrash();
        }
    }

    @GuardedBy("mLock")
    private void savePendingOperationLocked(@PendingOperationId int operationId,
            @Nullable Object value) {
        PendingOperation pendingOperation = mPendingOperations.get(operationId);

        if (pendingOperation == null) {
            pendingOperation = new PendingOperation(operationId, value);
            if (DBG) Slogf.d(TAG, "Created " + pendingOperation);
            mPendingOperations.put(operationId, pendingOperation);
            return;
        }
        switch (operationId) {
            case PO_ON_USER_REMOVED:
                Preconditions.checkArgument((value instanceof UserHandle),
                        "invalid value passed to ON_USER_REMOVED", value);
                if (pendingOperation.value instanceof ArrayList) {
                    if (DBG) Slogf.d(TAG, "Adding " + value + " to existing " + pendingOperation);
                    ((ArrayList) pendingOperation.value).add(value);
                } else if (pendingOperation.value instanceof UserHandle) {
                    ArrayList<Object> list = new ArrayList<>(2);
                    list.add(pendingOperation.value);
                    list.add(value);
                    if (DBG) Slogf.d(TAG, "Converting " + pendingOperation.value + " to " + list);
                    pendingOperation.value = list;
                } else {
                    throw new IllegalStateException("Invalid value for ON_USER_REMOVED: " + value);
                }
                break;
            case PO_ON_FACTORY_RESET:
                PendingOperation newOperation = new PendingOperation(operationId, value);
                if (DBG) Slogf.d(TAG, "Replacing " + pendingOperation + " by " + newOperation);
                mPendingOperations.put(operationId, newOperation);
                break;
            default:
                if (DBG) {
                    Slogf.d(TAG, "Already saved operation of type "
                            + pendingOperationToString(operationId));
                }
        }
    }

    @GuardedBy("mLock")
    private void sendCarServiceActionLocked(@PendingOperationId int operationId,
            @Nullable Object value) throws RemoteException {
        if (DBG) {
            Slogf.d(TAG, "sendCarServiceActionLocked: Operation "
                    + pendingOperationToString(operationId));
        }
        switch (operationId) {
            case PO_INIT_BOOT_USER:
                mCarService.initBootUser();
                break;
            case PO_ON_USER_REMOVED:
                if (value instanceof ArrayList) {
                    ArrayList<Object> list = (ArrayList<Object>) value;
                    if (DBG) Slogf.d(TAG, "Sending " + list.size() + " onUserRemoved() calls");
                    for (Object user: list) {
                        onUserRemovedLocked(user);
                    }
                } else {
                    onUserRemovedLocked(value);
                }
                break;
            case PO_ON_FACTORY_RESET:
                mCarService.onFactoryReset((ICarResultReceiver) value);
                break;
            default:
                Slogf.wtf(TAG, "Invalid Operation. OperationId -" + operationId);
        }
    }

    @GuardedBy("mLock")
    private void onUserRemovedLocked(@NonNull Object value) throws RemoteException {
        Preconditions.checkArgument((value instanceof UserHandle),
                "Invalid value for ON_USER_REMOVED: %s", value);
        UserHandle user = (UserHandle) value;
        if (DBG) Slogf.d(TAG, "Sending onUserRemoved(): " + user);
        mCarService.onUserRemoved(user);
    }

    /**
     * Sends user life-cycle events to CarService.
     */
    void sendUserLifecycleEvent(@UserLifecycleEventType int eventType, @UserIdInt int fromId,
            @UserIdInt int toId) {
        long now = System.currentTimeMillis();
        mUserMetrics.onEvent(eventType, now, fromId, toId);

        synchronized (mLock) {
            if (eventType == USER_LIFECYCLE_EVENT_TYPE_SWITCHING) {
                mLastSwitchedUser = toId;
                mPreviousUserOfLastSwitchedUser = fromId;
                mLastUserLifecycle.put(toId, eventType);
            } else if (eventType == USER_LIFECYCLE_EVENT_TYPE_STOPPING
                    || eventType == USER_LIFECYCLE_EVENT_TYPE_STOPPED) {
                mLastUserLifecycle.delete(toId);
            } else {
                mLastUserLifecycle.put(toId, eventType);
            }
            if (mCarService == null) {
                if (DBG) {
                    Slogf.d(TAG, "CarService null. sendUserLifecycleEvent() deferred for lifecycle"
                            + " event " + eventType + " for user " + toId);
                }
                return;
            }
        }
        sendUserLifecycleEventInternal(eventType, fromId, toId);
    }

    private void sendUserLifecycleEventInternal(@UserLifecycleEventType int eventType,
            @UserIdInt int fromId, @UserIdInt int toId) {
        if (DBG) {
            Slogf.d(TAG, "sendUserLifecycleEvent():" + " eventType=" + eventType + ", fromId="
                    + fromId + ", toId=" + toId);
        }
        try {
            synchronized (mLock) {
                if (isServiceCrashedLoggedLocked("sendUserLifecycleEvent")) return;
                mCarService.onUserLifecycleEvent(eventType, fromId, toId);
            }
        } catch (RemoteException e) {
            Slogf.w(TAG, "RemoteException from car service", e);
            handleCarServiceCrash();
        }
    }

    private void handleCarServiceCrash() {
        synchronized (mLock) {
            mCarServiceCrashed = true;
            mCarService = null;
        }
        Slogf.w(TAG, "CarServiceCrashed. No more car service calls before reconnection.");
        mCarServiceHelperServiceUpdatableImpl.handleCarServiceCrash();
    }

    private TimingsTraceLog newTimingsTraceLog() {
        return new TimingsTraceLog(TAG, TRACE_TAG_SYSTEM_SERVER);
    }

    @GuardedBy("mLock")
    private boolean isServiceCrashedLoggedLocked(@PendingOperationId int operationId) {
        return isServiceCrashedLoggedLocked(pendingOperationToString(operationId));
    }

    @GuardedBy("mLock")
    private boolean isServiceCrashedLoggedLocked(@NonNull String operation) {
        if (mCarServiceCrashed) {
            Slogf.w(TAG, "CarServiceCrashed. " + operation + " will be executed after "
                    + "reconnection");
            return true;
        }
        return false;
    }

    /**
     * Dump
     */
    void dump(IndentingPrintWriter writer) {
        // Do not change the next line, Used in cts test: testCarServiceHelperServiceDump
        writer.println("CarServiceProxy");
        writer.increaseIndent();
        synchronized (mLock) {
            writer.printf("mLastSwitchedUser=%s\n", mLastSwitchedUser);
            writer.printf("mInitialUser=%s\n", mInitialUser);
            writer.printf("mLastUserLifecycle:\n");
            int user0Lifecycle = mLastUserLifecycle.get(USER_SYSTEM, 0);
            if (user0Lifecycle != 0) {
                writer.printf("SystemUser Lifecycle Event:%s\n", user0Lifecycle);
            } else {
                writer.println("SystemUser not initialized");
            }

            int lastUserLifecycle = mLastUserLifecycle.get(mLastSwitchedUser, 0);
            if (mLastSwitchedUser != USER_SYSTEM && user0Lifecycle != 0) {
                writer.printf("last user (%s) Lifecycle Event:%s\n",
                        mLastSwitchedUser, lastUserLifecycle);
            }

            int size = mPendingOperations.size();
            if (size == 0) {
                writer.println("No pending operations");
            } else {
                writer.printf("%d pending operation%s:\n", size, size == 1 ? "" : "s");
                writer.increaseIndent();
                for (int i = 0; i < size; i++) {
                    writer.println(mPendingOperations.valueAt(i));
                }
                writer.decreaseIndent();
            }
        }
        writer.decreaseIndent();
        dumpUserMetrics(writer);
    }

    /**
     * Dump User metrics
     */
    void dumpUserMetrics(IndentingPrintWriter writer) {
        mUserMetrics.dump(writer);
    }

    private final class PendingOperation {
        public final int id;
        public @Nullable Object value;

        PendingOperation(int id, @Nullable Object value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public String toString() {
            return "PendingOperation[" + pendingOperationToString(id)
                + (value == null ? "" : ": " + value) + "]";
        }
    }

    @NonNull
    private String pendingOperationToString(@PendingOperationId int operationType) {
        return DebugUtils.constantToString(CarServiceProxy.class, "PO_" , operationType);
    }
}
