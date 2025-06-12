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

package android.window;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SuppressLint;
import android.annotation.TestApi;
import android.app.ActivityTaskManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Singleton;
import android.view.RemoteAnimationAdapter;
import android.view.SurfaceControl;

/**
 * Base class for organizing specific types of windows like Tasks and DisplayAreas
 *
 * @hide
 */
@TestApi
public class WindowOrganizer {

    /**
     * Apply multiple WindowContainer operations at once.
     *
     * Note that using this API requires the caller to hold
     * {@link android.Manifest.permission#MANAGE_ACTIVITY_TASKS}.
     *
     * @param t The transaction to apply.
     */
    @RequiresPermission(value = android.Manifest.permission.MANAGE_ACTIVITY_TASKS)
    public void applyTransaction(@NonNull WindowContainerTransaction t) {
        try {
            if (!t.isEmpty()) {
                getWindowOrganizerController().applyTransaction(t);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Apply multiple WindowContainer operations at once.
     *
     * Note that using this API requires the caller to hold
     * {@link android.Manifest.permission#MANAGE_ACTIVITY_TASKS}.
     *
     * @param t The transaction to apply.
     * @param callback This transaction will use the synchronization scheme described in
     *        BLASTSyncEngine.java. The SurfaceControl transaction containing the effects of this
     *        WindowContainer transaction will be passed to this callback when ready.
     * @return An ID for the sync operation which will later be passed to transactionReady callback.
     *         This lets the caller differentiate overlapping sync operations.
     */
    @RequiresPermission(value = android.Manifest.permission.MANAGE_ACTIVITY_TASKS)
    public int applySyncTransaction(@NonNull WindowContainerTransaction t,
            @NonNull WindowContainerTransactionCallback callback) {
        try {
            return getWindowOrganizerController().applySyncTransaction(t, callback.mInterface);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Starts a new transition, don't use this to start an already created one.
     * @param type The type of the transition. This is ignored if a transitionToken is provided.
     * @param t The set of window operations that are part of this transition.
     * @return A token identifying the transition. This will be the same as transitionToken if it
     *         was provided.
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.MANAGE_ACTIVITY_TASKS)
    @NonNull
    public IBinder startNewTransition(int type, @Nullable WindowContainerTransaction t) {
        try {
            return getWindowOrganizerController().startNewTransition(type, t);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Starts an already created transition.
     * @param transitionToken An existing transition to start.
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.MANAGE_ACTIVITY_TASKS)
    public void startTransition(@NonNull IBinder transitionToken,
            @Nullable WindowContainerTransaction t) {
        try {
            getWindowOrganizerController().startTransition(transitionToken, t);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Finishes a running transition.
     * @param transitionToken The transition to finish. Can't be null.
     * @param t A set of window operations to apply before finishing.
     *
     * @hide
     */
    @SuppressLint("ExecutorRegistration")
    @RequiresPermission(android.Manifest.permission.MANAGE_ACTIVITY_TASKS)
    public void finishTransition(@NonNull IBinder transitionToken,
            @Nullable WindowContainerTransaction t) {
        try {
            getWindowOrganizerController().finishTransition(transitionToken, t);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Register an ITransitionPlayer to handle transition animations.
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.MANAGE_ACTIVITY_TASKS)
    public void registerTransitionPlayer(@NonNull ITransitionPlayer player) {
        try {
            getWindowOrganizerController().registerTransitionPlayer(player);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Unregister a previously-registered ITransitionPlayer.
     * @hide
     */
    @RequiresPermission(android.Manifest.permission.MANAGE_ACTIVITY_TASKS)
    public void unregisterTransitionPlayer(@NonNull ITransitionPlayer player) {
        try {
            getWindowOrganizerController().unregisterTransitionPlayer(player);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * @see TransitionMetrics
     * @hide
     */
    public static ITransitionMetricsReporter getTransitionMetricsReporter() {
        try {
            return getWindowOrganizerController().getTransitionMetricsReporter();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Use WM's transaction-queue instead of Shell's independent one. This is necessary
     * if WM and Shell need to coordinate transactions (eg. for shell transitions).
     * @return true if successful, false otherwise.
     * @hide
     */
    public boolean shareTransactionQueue() {
        final IBinder wmApplyToken;
        try {
            wmApplyToken = getWindowOrganizerController().getApplyToken();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
        if (wmApplyToken == null) {
            return false;
        }
        SurfaceControl.Transaction.setDefaultApplyToken(wmApplyToken);
        return true;
    }

    static IWindowOrganizerController getWindowOrganizerController() {
        return IWindowOrganizerControllerSingleton.get();
    }

    private static final Singleton<IWindowOrganizerController> IWindowOrganizerControllerSingleton =
            new Singleton<IWindowOrganizerController>() {
                @Override
                protected IWindowOrganizerController create() {
                    try {
                        return ActivityTaskManager.getService().getWindowOrganizerController();
                    } catch (RemoteException e) {
                        return null;
                    }
                }
            };
}
