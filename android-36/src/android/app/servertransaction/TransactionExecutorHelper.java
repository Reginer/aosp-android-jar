/*
 * Copyright 2018 The Android Open Source Project
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
import static android.app.servertransaction.ActivityLifecycleItem.PRE_ON_CREATE;
import static android.app.servertransaction.ActivityLifecycleItem.UNDEFINED;

import android.annotation.NonNull;
import android.app.Activity;
import android.app.ActivityThread.ActivityClientRecord;
import android.app.ClientTransactionHandler;
import android.os.IBinder;
import android.util.IntArray;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * Helper class for {@link TransactionExecutor} that contains utils for lifecycle path resolution.
 *
 * @hide
 */
public class TransactionExecutorHelper {
    private static final String TAG = TransactionExecutorHelper.class.getSimpleName();
    // A penalty applied to path with destruction when looking for the shortest one.
    private static final int DESTRUCTION_PENALTY = 10;

    private static final int[] ON_RESUME_PRE_EXCUTION_STATES = new int[] { ON_START, ON_PAUSE };

    // Temp holder for lifecycle path.
    // No direct transition between two states should take more than one complete cycle of 6 states.
    @ActivityLifecycleItem.LifecycleState
    private final IntArray mLifecycleSequence = new IntArray(6 /* initialCapacity */);

    /**
     * Calculate the path through main lifecycle states for an activity and fill
     * @link #mLifecycleSequence} with values starting with the state that follows the initial
     * state.
     * <p>NOTE: The returned value is used internally in this class and is not a copy. It's contents
     * may change after calling other methods of this class.</p>
     */
    @VisibleForTesting
    public IntArray getLifecyclePath(int start, int finish, boolean excludeLastState) {
        if (start == UNDEFINED || finish == UNDEFINED) {
            throw new IllegalArgumentException("Can't resolve lifecycle path for undefined state");
        }
        if (start == ON_RESTART || finish == ON_RESTART) {
            throw new IllegalArgumentException(
                    "Can't start or finish in intermittent RESTART state");
        }
        if (finish == PRE_ON_CREATE && start != finish) {
            throw new IllegalArgumentException("Can only start in pre-onCreate state");
        }

        mLifecycleSequence.clear();
        if (finish >= start) {
            if (start == ON_START && finish == ON_STOP) {
                // A case when we from start to stop state soon, we don't need to go
                // through the resumed, paused state.
                mLifecycleSequence.add(ON_STOP);
            } else {
                // just go there
                for (int i = start + 1; i <= finish; i++) {
                    mLifecycleSequence.add(i);
                }
            }
        } else { // finish < start, can't just cycle down
            if (start == ON_PAUSE && finish == ON_RESUME) {
                // Special case when we can just directly go to resumed state.
                mLifecycleSequence.add(ON_RESUME);
            } else if (start <= ON_STOP && finish >= ON_START) {
                // Restart and go to required state.

                // Go to stopped state first.
                for (int i = start + 1; i <= ON_STOP; i++) {
                    mLifecycleSequence.add(i);
                }
                // Restart
                mLifecycleSequence.add(ON_RESTART);
                // Go to required state
                for (int i = ON_START; i <= finish; i++) {
                    mLifecycleSequence.add(i);
                }
            } else {
                // Relaunch and go to required state

                // Go to destroyed state first.
                for (int i = start + 1; i <= ON_DESTROY; i++) {
                    mLifecycleSequence.add(i);
                }
                // Go to required state
                for (int i = ON_CREATE; i <= finish; i++) {
                    mLifecycleSequence.add(i);
                }
            }
        }

        // Remove last transition in case we want to perform it with some specific params.
        if (excludeLastState && mLifecycleSequence.size() != 0) {
            mLifecycleSequence.remove(mLifecycleSequence.size() - 1);
        }

        return mLifecycleSequence;
    }

    /**
     * Pick a state that goes before provided post-execution state and would require the least
     * lifecycle transitions to get to.
     * It will also make sure to try avoiding a path with activity destruction and relaunch if
     * possible.
     * @param r An activity that we're trying to resolve the transition for.
     * @param postExecutionState Post execution state to compute for.
     * @return One of states that precede the provided post-execution state, or
     *         {@link ActivityLifecycleItem#UNDEFINED} if there is not path.
     */
    @VisibleForTesting
    public int getClosestPreExecutionState(ActivityClientRecord r,
            int postExecutionState) {
        switch (postExecutionState) {
            case UNDEFINED:
                return UNDEFINED;
            case ON_RESUME:
                return getClosestOfStates(r, ON_RESUME_PRE_EXCUTION_STATES);
            default:
                throw new UnsupportedOperationException("Pre-execution states for state: "
                        + postExecutionState + " is not supported.");
        }
    }

    /**
     * Pick a state that would require the least lifecycle transitions to get to.
     * It will also make sure to try avoiding a path with activity destruction and relaunch if
     * possible.
     * @param r An activity that we're trying to resolve the transition for.
     * @param finalStates An array of valid final states.
     * @return One of the provided final states, or {@link ActivityLifecycleItem#UNDEFINED} if none
     *         were provided or there is not path.
     */
    @VisibleForTesting
    public int getClosestOfStates(ActivityClientRecord r, int[] finalStates) {
        if (finalStates == null || finalStates.length == 0) {
            return UNDEFINED;
        }
        if (r == null) {
            // Early return because the ActivityClientRecord hasn't been created or cannot be found.
            Log.w(TAG, "ActivityClientRecord was null");
            return UNDEFINED;
        }

        final int currentState = r.getLifecycleState();
        int closestState = UNDEFINED;
        for (int i = 0, shortestPath = Integer.MAX_VALUE, pathLength; i < finalStates.length; i++) {
            getLifecyclePath(currentState, finalStates[i], false /* excludeLastState */);
            pathLength = mLifecycleSequence.size();
            if (pathInvolvesDestruction(mLifecycleSequence)) {
                pathLength += DESTRUCTION_PENALTY;
            }
            if (shortestPath > pathLength) {
                shortestPath = pathLength;
                closestState = finalStates[i];
            }
        }
        return closestState;
    }

    /** Get the lifecycle state request to match the current state in the end of a transaction. */
    public static ActivityLifecycleItem getLifecycleRequestForCurrentState(ActivityClientRecord r) {
        final int prevState = r.getLifecycleState();
        final ActivityLifecycleItem lifecycleItem;
        switch (prevState) {
            // TODO(lifecycler): Extend to support all possible states.
            case ON_START:
                // Fall through to return the PAUSE item to ensure the activity is properly
                // resumed while relaunching.
            case ON_PAUSE:
                lifecycleItem = new PauseActivityItem(r.token);
                break;
            case ON_STOP:
                lifecycleItem = new StopActivityItem(r.token);
                break;
            default:
                lifecycleItem = new ResumeActivityItem(r.token, false /* isForward */,
                        false /* shouldSendCompatFakeFocus */);
                break;
        }

        return lifecycleItem;
    }

    /**
     * Check if there is a destruction involved in the path. We want to avoid a lifecycle sequence
     * that involves destruction and recreation if there is another path.
     */
    private static boolean pathInvolvesDestruction(IntArray lifecycleSequence) {
        final int size = lifecycleSequence.size();
        for (int i = 0; i < size; i++) {
            if (lifecycleSequence.get(i) == ON_DESTROY) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the index of the last callback between the start index and last index that requests
     * the state for the given activity token in which that activity will be after execution.
     * If there is a group of callbacks in the end that requests the same specific state or doesn't
     * request any - we will find the first one from such group.
     *
     * E.g. ActivityResult requests RESUMED post-execution state, Configuration does not request any
     * specific state. If there is a sequence
     *   Configuration - ActivityResult - Configuration - ActivityResult
     * index 1 will be returned, because ActivityResult request on position 1 will be the last
     * request that moves activity to the RESUMED state where it will eventually end.
     */
    private static int lastCallbackRequestingStateIndex(@NonNull List<ClientTransactionItem> items,
            int startIndex, int lastIndex, @NonNull IBinder activityToken) {
        // Go from the back of the list to front, look for the request closes to the beginning that
        // requests the state in which activity will end after all callbacks are executed.
        int lastRequestedState = UNDEFINED;
        int lastRequestingCallback = -1;
        for (int i = lastIndex; i >= startIndex; i--) {
            final ClientTransactionItem item = items.get(i);
            final int postExecutionState = item.getPostExecutionState();
            if (postExecutionState != UNDEFINED && activityToken.equals(item.getActivityToken())) {
                // Found a callback that requests some post-execution state for the given activity.
                if (lastRequestedState == UNDEFINED || lastRequestedState == postExecutionState) {
                    // It's either a first-from-end callback that requests state or it requests
                    // the same state as the last one. In both cases, we will use it as the new
                    // candidate.
                    lastRequestedState = postExecutionState;
                    lastRequestingCallback = i;
                } else {
                    break;
                }
            }
        }

        return lastRequestingCallback;
    }

    /**
     * For the transaction item at {@code currentIndex}, if it is requesting post execution state,
     * whether or not to exclude the last state. This only returns {@code true} when there is a
     * following explicit {@link ActivityLifecycleItem} requesting the same state for the same
     * activity, so that last state will be covered by the following {@link ActivityLifecycleItem}.
     */
    static boolean shouldExcludeLastLifecycleState(@NonNull List<ClientTransactionItem> items,
            int currentIndex) {
        final ClientTransactionItem item = items.get(currentIndex);
        final IBinder activityToken = item.getActivityToken();
        final int postExecutionState = item.getPostExecutionState();
        if (activityToken == null || postExecutionState == UNDEFINED) {
            // Not a transaction item requesting post execution state.
            return false;
        }
        final int nextLifecycleItemIndex = findNextLifecycleItemIndex(items, currentIndex + 1,
                activityToken);
        if (nextLifecycleItemIndex == -1) {
            // No following ActivityLifecycleItem for this activity token.
            return false;
        }
        final ActivityLifecycleItem lifecycleItem =
                (ActivityLifecycleItem) items.get(nextLifecycleItemIndex);
        if (postExecutionState != lifecycleItem.getTargetState()) {
            // The explicit ActivityLifecycleItem is not requesting the same state.
            return false;
        }
        // Only exclude for the first non-lifecycle item that requests the same specific state.
        return currentIndex == lastCallbackRequestingStateIndex(items, currentIndex,
                nextLifecycleItemIndex - 1, activityToken);
    }

    /**
     * Finds the index of the next {@link ActivityLifecycleItem} for the given activity token.
     */
    private static int findNextLifecycleItemIndex(@NonNull List<ClientTransactionItem> items,
            int startIndex, @NonNull IBinder activityToken) {
        final int size = items.size();
        for (int i = startIndex; i < size; i++) {
            final ClientTransactionItem item = items.get(i);
            if (item.isActivityLifecycleItem() && item.getActivityToken().equals(activityToken)) {
                return i;
            }
        }
        return -1;
    }

    /** Dump transaction to string. */
    static String transactionToString(@NonNull ClientTransaction transaction,
            @NonNull ClientTransactionHandler transactionHandler) {
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter pw = new PrintWriter(stringWriter);
        final String prefix = tId(transaction);
        transaction.dump(prefix, pw, transactionHandler);
        return stringWriter.toString();
    }

    /** @return A string in format "tId:<transaction hashcode> ". */
    static String tId(ClientTransaction transaction) {
        return "tId:" + transaction.hashCode() + " ";
    }

    /** Get activity string name for provided token. */
    static String getActivityName(IBinder token, ClientTransactionHandler transactionHandler) {
        final Activity activity = getActivityForToken(token, transactionHandler);
        if (activity != null) {
            return activity.getComponentName().getClassName();
        }
        return "Not found for token: " + token;
    }

    /** Get short activity class name for provided token. */
    static String getShortActivityName(IBinder token, ClientTransactionHandler transactionHandler) {
        final Activity activity = getActivityForToken(token, transactionHandler);
        if (activity != null) {
            return activity.getComponentName().getShortClassName();
        }
        return "Not found for token: " + token;
    }

    private static Activity getActivityForToken(IBinder token,
            ClientTransactionHandler transactionHandler) {
        if (token == null) {
            return null;
        }
        return transactionHandler.getActivity(token);
    }

    /** Get lifecycle state string name. */
    static String getStateName(int state) {
        switch (state) {
            case UNDEFINED:
                return "UNDEFINED";
            case PRE_ON_CREATE:
                return "PRE_ON_CREATE";
            case ON_CREATE:
                return "ON_CREATE";
            case ON_START:
                return "ON_START";
            case ON_RESUME:
                return "ON_RESUME";
            case ON_PAUSE:
                return "ON_PAUSE";
            case ON_STOP:
                return "ON_STOP";
            case ON_DESTROY:
                return "ON_DESTROY";
            case ON_RESTART:
                return "ON_RESTART";
            default:
                throw new IllegalArgumentException("Unexpected lifecycle state: " + state);
        }
    }
}
