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

package com.android.server.wm;

import android.annotation.NonNull;
import android.app.IApplicationThread;
import android.app.compat.CompatChanges;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.ClientTransactionItem;
import android.app.servertransaction.LaunchActivityItem;
import android.compat.annotation.ChangeId;
import android.compat.annotation.EnabledSince;
import android.os.Build;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Class that is able to combine multiple client lifecycle transition requests and/or callbacks,
 * and execute them as a single transaction.
 *
 * @see ClientTransaction
 */
class ClientLifecycleManager {

    private static final String TAG = "ClientLifecycleManager";

    /**
     * To prevent any existing apps from having app compat issue with the non-sdk usages of
     * {@link ClientTransaction#getActivityToken()}, only allow bundling {@link LaunchActivityItem}
     * for apps with targetSDK of V and above.
     */
    @ChangeId
    @EnabledSince(targetSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private static final long ENABLE_BUNDLE_LAUNCH_ACTIVITY_ITEM = 324203798L;

    /** Mapping from client process binder to its pending transaction. */
    @VisibleForTesting
    final ArrayMap<IBinder, ClientTransaction> mPendingTransactions = new ArrayMap<>();

    private WindowManagerService mWms;

    void setWindowManager(@NonNull WindowManagerService wms) {
        mWms = wms;
    }

    /**
     * Schedules a transaction, which may consist of multiple callbacks and a lifecycle request.
     * @param transaction A sequence of client transaction items.
     * @return {@code false} if the transaction failed because of {@link RemoteException}.
     * @see ClientTransaction
     */
    @VisibleForTesting
    boolean scheduleTransaction(@NonNull ClientTransaction transaction) {
        final RemoteException e = transaction.schedule();
        if (e != null) {
            final WindowProcessController wpc = mWms.mAtmService.getProcessController(
                    transaction.getClient());
            Slog.w(TAG, "Failed to deliver transaction for " + wpc + "\ntransaction=" + this, e);
            return false;
        }
        return true;
    }

    /**
     * Similar to {@link #scheduleTransactionItem}, but it sends the transaction immediately and
     * it can be called without WM lock.
     *
     * @return {@code false} if the transaction failed because of {@link RemoteException}.
     * @see WindowProcessController#setReportedProcState(int)
     */
    boolean scheduleTransactionItemNow(@NonNull IApplicationThread client,
            @NonNull ClientTransactionItem transactionItem) throws RemoteException {
        final ClientTransaction clientTransaction = new ClientTransaction(client);
        clientTransaction.addTransactionItem(transactionItem);
        final boolean res = scheduleTransaction(clientTransaction);
        if (!com.android.window.flags.Flags.cleanupDispatchPendingTransactionsRemoteException()
                && !res) {
            throw new DeadObjectException();
        }
        return res;
    }

    /**
     * Schedules a transaction with the given item, delivery to client application, which may be
     * queued to dispatched later.
     *
     * @return {@code false} if the transaction was dispatched immediately, but failed because of
     *         {@link RemoteException}. If the transaction is queued, any failure will be ignored.
     * @see ClientTransactionItem
     */
    boolean scheduleTransactionItem(@NonNull IApplicationThread client,
            @NonNull ClientTransactionItem item) throws RemoteException {
        // Wait until RootWindowContainer#performSurfacePlacementNoTrace to dispatch all pending
        // transactions at once.
        final ClientTransaction clientTransaction = getOrCreatePendingTransaction(client);
        clientTransaction.addTransactionItem(item);

        final boolean res = onClientTransactionItemScheduled(clientTransaction,
                false /* shouldDispatchImmediately */);
        if (!com.android.window.flags.Flags.cleanupDispatchPendingTransactionsRemoteException()
                && !res) {
            throw new DeadObjectException();
        }
        return res;
    }

    /**
     * Schedules a transaction with the given items, delivery to client application, which may be
     * queued to dispatched later.
     *
     * @return {@code false} if the transaction was dispatched immediately, but failed because of
     *         {@link RemoteException}. If the transaction is queued, any failure will be ignored.
     * @see ClientTransactionItem
     */
    boolean scheduleTransactionItems(@NonNull IApplicationThread client,
            @NonNull ClientTransactionItem... items) throws RemoteException {
        return scheduleTransactionItems(client, false /* shouldDispatchImmediately */, items);
    }

    /**
     * Schedules a transaction with the given items, delivery to client application, which may be
     * queued to dispatched later.
     *
     * @param shouldDispatchImmediately whether or not to dispatch the transaction immediately. This
     *                                  should only be {@code true} when it is important to know the
     *                                  result of dispatching immediately. For example, when cold
     *                                  launches an app, the server needs to know if the transaction
     *                                  is dispatched successfully, and may restart the process if
     *                                  not.
     * @return {@code false} if the transaction was dispatched immediately, but failed because of
     *         {@link RemoteException}. If the transaction is queued, any failure will be ignored.
     * @see ClientTransactionItem
     */
    boolean scheduleTransactionItems(@NonNull IApplicationThread client,
            boolean shouldDispatchImmediately,
            @NonNull ClientTransactionItem... items) throws RemoteException {
        // Wait until RootWindowContainer#performSurfacePlacementNoTrace to dispatch all pending
        // transactions at once.
        final ClientTransaction clientTransaction = getOrCreatePendingTransaction(client);

        final int size = items.length;
        for (int i = 0; i < size; i++) {
            clientTransaction.addTransactionItem(items[i]);
        }

        final boolean res = onClientTransactionItemScheduled(clientTransaction,
                shouldDispatchImmediately);
        if (!com.android.window.flags.Flags.cleanupDispatchPendingTransactionsRemoteException()
                && !res) {
            throw new DeadObjectException();
        }
        return res;
    }

    /** Executes all the pending transactions. */
    void dispatchPendingTransactions() {
        if (mPendingTransactions.isEmpty()) {
            return;
        }
        Trace.traceBegin(Trace.TRACE_TAG_WINDOW_MANAGER, "clientTransactionsDispatched");
        final int size = mPendingTransactions.size();
        for (int i = 0; i < size; i++) {
            final ClientTransaction transaction = mPendingTransactions.valueAt(i);
            scheduleTransaction(transaction);
        }
        mPendingTransactions.clear();
        Trace.traceEnd(Trace.TRACE_TAG_WINDOW_MANAGER);
    }

    /** Executes the pending transaction for the given client process. */
    void dispatchPendingTransaction(@NonNull IApplicationThread client) {
        final ClientTransaction pendingTransaction = mPendingTransactions.remove(client.asBinder());
        if (pendingTransaction != null) {
            scheduleTransaction(pendingTransaction);
        }
    }

    /**
     * Called to when {@link WindowSurfacePlacer#continueLayout}.
     * Dispatches all pending transactions unless there is an ongoing/scheduled layout, in which
     * case the pending transactions will be dispatched in
     * {@link RootWindowContainer#performSurfacePlacementNoTrace}.
     */
    void onLayoutContinued() {
        if (shouldDispatchPendingTransactionsImmediately()) {
            // Dispatch the pending transactions immediately if there is no ongoing/scheduled layout
            dispatchPendingTransactions();
        }
    }

    /** Must only be called with WM lock. */
    @NonNull
    private ClientTransaction getOrCreatePendingTransaction(@NonNull IApplicationThread client) {
        final IBinder clientBinder = client.asBinder();
        final ClientTransaction pendingTransaction = mPendingTransactions.get(clientBinder);
        if (pendingTransaction != null) {
            return pendingTransaction;
        }

        // Create new transaction if there is no existing.
        final ClientTransaction transaction = new ClientTransaction(client);
        mPendingTransactions.put(clientBinder, transaction);
        return transaction;
    }

    /**
     * Must only be called with WM lock.
     * If the transaction should not be queued, it will be dispatched immediately.
     *
     * @return {@code false} if the transaction was dispatched immediately, but failed because of
     *         {@link RemoteException}.
     */
    private boolean onClientTransactionItemScheduled(
            @NonNull ClientTransaction clientTransaction,
            boolean shouldDispatchImmediately) {
        if (shouldDispatchImmediately || shouldDispatchPendingTransactionsImmediately()) {
            // Dispatch the pending transaction immediately.
            mPendingTransactions.remove(clientTransaction.getClient().asBinder());
            return scheduleTransaction(clientTransaction);
        }
        return true;
    }

    /** Must only be called with WM lock. */
    private boolean shouldDispatchPendingTransactionsImmediately() {
        if (mWms == null) {
            return true;
        }
        // Do not dispatch when
        // 1. Layout deferred.
        // 2. Layout requested.
        // 3. Layout in process.
        // The pending transactions will be dispatched during layout in
        // RootWindowContainer#performSurfacePlacementNoTrace.
        return !mWms.mWindowPlacerLocked.isLayoutDeferred()
                && !mWms.mWindowPlacerLocked.isTraversalScheduled()
                && !mWms.mWindowPlacerLocked.isInLayout();
    }

    /** Guards bundling {@link LaunchActivityItem} with targetSDK. */
    static boolean shouldDispatchLaunchActivityItemIndependently(
            @NonNull String appPackageName, int appUid) {
        return !CompatChanges.isChangeEnabled(ENABLE_BUNDLE_LAUNCH_ACTIVITY_ITEM,
                appPackageName,
                UserHandle.getUserHandleForUid(appUid));
    }
}
