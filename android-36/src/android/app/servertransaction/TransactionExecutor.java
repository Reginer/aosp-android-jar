/*
 * Copyright 2017 The Android Open Source Project
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

package android.app.servertransaction;

import static android.app.servertransaction.ActivityLifecycleItem.ON_CREATE;
import static android.app.servertransaction.ActivityLifecycleItem.ON_DESTROY;
import static android.app.servertransaction.ActivityLifecycleItem.ON_PAUSE;
import static android.app.servertransaction.ActivityLifecycleItem.ON_RESTART;
import static android.app.servertransaction.ActivityLifecycleItem.ON_RESUME;
import static android.app.servertransaction.ActivityLifecycleItem.ON_START;
import static android.app.servertransaction.ActivityLifecycleItem.ON_STOP;
import static android.app.servertransaction.ActivityLifecycleItem.UNDEFINED;
import static android.app.servertransaction.TransactionExecutorHelper.getShortActivityName;
import static android.app.servertransaction.TransactionExecutorHelper.getStateName;
import static android.app.servertransaction.TransactionExecutorHelper.shouldExcludeLastLifecycleState;
import static android.app.servertransaction.TransactionExecutorHelper.tId;
import static android.app.servertransaction.TransactionExecutorHelper.transactionToString;

import android.annotation.NonNull;
import android.app.ActivityThread.ActivityClientRecord;
import android.app.ClientTransactionHandler;
import android.content.Context;
import android.os.IBinder;
import android.os.Trace;
import android.util.IntArray;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;

import java.util.List;

/**
 * Class that manages transaction execution in the correct order.
 *
 * @hide
 */
public class TransactionExecutor {

    private static final boolean DEBUG_RESOLVER = false;
    private static final String TAG = "TransactionExecutor";

    private final ClientTransactionHandler mTransactionHandler;
    private final PendingTransactionActions mPendingActions = new PendingTransactionActions();
    private final TransactionExecutorHelper mHelper = new TransactionExecutorHelper();

    /** Initialize an instance with transaction handler, that will execute all requested actions. */
    public TransactionExecutor(@NonNull ClientTransactionHandler clientTransactionHandler) {
        mTransactionHandler = clientTransactionHandler;
    }

    /**
     * Resolve transaction.
     * First all callbacks will be executed in the order they appear in the list. If a callback
     * requires a certain pre- or post-execution state, the client will be transitioned accordingly.
     * Then the client will cycle to the final lifecycle state if provided. Otherwise, it will
     * either remain in the initial state, or last state needed by a callback.
     */
    public void execute(@NonNull ClientTransaction transaction) {
        if (DEBUG_RESOLVER) {
            Slog.d(TAG, tId(transaction) + "Start resolving transaction");
            Slog.d(TAG, transactionToString(transaction, mTransactionHandler));
        }

        Trace.traceBegin(Trace.TRACE_TAG_WINDOW_MANAGER, "clientTransactionExecuted");
        try {
            executeTransactionItems(transaction);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to execute the transaction: "
                    + transactionToString(transaction, mTransactionHandler));
            throw e;
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_WINDOW_MANAGER);
        }

        mPendingActions.clear();
        if (DEBUG_RESOLVER) Slog.d(TAG, tId(transaction) + "End resolving transaction");
    }

    /** Cycles through all transaction items and execute them at proper times. */
    @VisibleForTesting
    public void executeTransactionItems(@NonNull ClientTransaction transaction) {
        final List<ClientTransactionItem> items = transaction.getTransactionItems();
        final int size = items.size();
        for (int i = 0; i < size; i++) {
            final ClientTransactionItem item = items.get(i);
            if (item.isActivityLifecycleItem()) {
                executeLifecycleItem(transaction, (ActivityLifecycleItem) item);
            } else {
                executeNonLifecycleItem(transaction, item,
                        shouldExcludeLastLifecycleState(items, i));
            }
        }
    }

    private void executeNonLifecycleItem(@NonNull ClientTransaction transaction,
            @NonNull ClientTransactionItem item, boolean shouldExcludeLastLifecycleState) {
        final IBinder token = item.getActivityToken();
        ActivityClientRecord r = mTransactionHandler.getActivityClient(token);

        if (token != null && r == null
                && mTransactionHandler.getActivitiesToBeDestroyed().containsKey(token)) {
            // The activity has not been created but has been requested to destroy, so all
            // transactions for the token are just like being cancelled.
            Slog.w(TAG, "Skip pre-destroyed transaction item:\n" + item);
            return;
        }

        if (DEBUG_RESOLVER) Slog.d(TAG, tId(transaction) + "Resolving callback: " + item);
        final int postExecutionState = item.getPostExecutionState();

        if (item.shouldHaveDefinedPreExecutionState()) {
            final int closestPreExecutionState = mHelper.getClosestPreExecutionState(r,
                    postExecutionState);
            if (closestPreExecutionState != UNDEFINED) {
                cycleToPath(r, closestPreExecutionState, transaction);
            }
        }

        item.execute(mTransactionHandler, mPendingActions);

        item.postExecute(mTransactionHandler, mPendingActions);
        if (r == null) {
            // Launch activity request will create an activity record.
            r = mTransactionHandler.getActivityClient(token);
        }

        if (postExecutionState != UNDEFINED && r != null) {
            cycleToPath(r, postExecutionState, shouldExcludeLastLifecycleState, transaction);
        }
    }

    private void executeLifecycleItem(@NonNull ClientTransaction transaction,
            @NonNull ActivityLifecycleItem lifecycleItem) {
        final IBinder token = lifecycleItem.getActivityToken();
        final ActivityClientRecord r = mTransactionHandler.getActivityClient(token);
        if (DEBUG_RESOLVER) {
            Slog.d(TAG, tId(transaction) + "Resolving lifecycle state: "
                    + lifecycleItem + " for activity: "
                    + getShortActivityName(token, mTransactionHandler));
        }

        if (r == null) {
            if (mTransactionHandler.getActivitiesToBeDestroyed().get(token) == lifecycleItem) {
                // Always cleanup for destroy item.
                lifecycleItem.postExecute(mTransactionHandler, mPendingActions);
            }
            // Ignore requests for non-existent client records for now.
            return;
        }

        // Cycle to the state right before the final requested state.
        cycleToPath(r, lifecycleItem.getTargetState(), true /* excludeLastState */, transaction);

        // Execute the final transition with proper parameters.
        lifecycleItem.execute(mTransactionHandler, mPendingActions);
        lifecycleItem.postExecute(mTransactionHandler, mPendingActions);
    }

    /** Transition the client between states. */
    @VisibleForTesting
    public void cycleToPath(ActivityClientRecord r, int finish, ClientTransaction transaction) {
        cycleToPath(r, finish, false /* excludeLastState */, transaction);
    }

    /**
     * Transition the client between states with an option not to perform the last hop in the
     * sequence. This is used when resolving lifecycle state request, when the last transition must
     * be performed with some specific parameters.
     */
    private void cycleToPath(ActivityClientRecord r, int finish, boolean excludeLastState,
            ClientTransaction transaction) {
        final int start = r.getLifecycleState();
        if (DEBUG_RESOLVER) {
            Slog.d(TAG, tId(transaction) + "Cycle activity: "
                    + getShortActivityName(r.token, mTransactionHandler)
                    + " from: " + getStateName(start) + " to: " + getStateName(finish)
                    + " excludeLastState: " + excludeLastState);
        }
        final IntArray path = mHelper.getLifecyclePath(start, finish, excludeLastState);
        performLifecycleSequence(r, path, transaction);
    }

    /** Transition the client through previously initialized state sequence. */
    private void performLifecycleSequence(ActivityClientRecord r, IntArray path,
            ClientTransaction transaction) {
        final int size = path.size();
        for (int i = 0, state; i < size; i++) {
            state = path.get(i);
            if (DEBUG_RESOLVER) {
                Slog.d(TAG, tId(transaction) + "Transitioning activity: "
                        + getShortActivityName(r.token, mTransactionHandler)
                        + " to state: " + getStateName(state));
            }
            switch (state) {
                case ON_CREATE:
                    mTransactionHandler.handleLaunchActivity(r, mPendingActions,
                            Context.DEVICE_ID_INVALID, null /* customIntent */);
                    break;
                case ON_START:
                    mTransactionHandler.handleStartActivity(r, mPendingActions,
                            null /* sceneTransitionInfo */);
                    break;
                case ON_RESUME:
                    mTransactionHandler.handleResumeActivity(r, false /* finalStateRequest */,
                            r.isForward, false /* shouldSendCompatFakeFocus */,
                            "LIFECYCLER_RESUME_ACTIVITY");
                    break;
                case ON_PAUSE:
                    mTransactionHandler.handlePauseActivity(r, false /* finished */,
                            false /* userLeaving */,
                            false /* autoEnteringPip */, mPendingActions,
                            "LIFECYCLER_PAUSE_ACTIVITY");
                    break;
                case ON_STOP:
                    mTransactionHandler.handleStopActivity(r,
                            mPendingActions, false /* finalStateRequest */,
                            "LIFECYCLER_STOP_ACTIVITY");
                    break;
                case ON_DESTROY:
                    mTransactionHandler.handleDestroyActivity(r, false /* finishing */,
                            false /* getNonConfigInstance */,
                            "performLifecycleSequence. cycling to:" + path.get(size - 1));
                    break;
                case ON_RESTART:
                    mTransactionHandler.performRestartActivity(r, false /* start */);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected lifecycle state: " + state);
            }
        }
    }
}
