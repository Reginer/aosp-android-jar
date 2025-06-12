/*
 * Copyright (C) 2018 The Android Open Source Project
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
package android.app.prediction;

import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.app.prediction.IPredictionCallback.Stub;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.appprediction.flags.Flags;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;

import dalvik.system.CloseGuard;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Class that represents an App Prediction client.
 *
 * <p>
 * Usage: <pre> {@code
 *
 * class MyActivity {
 *    private AppPredictor mClient
 *
 *    void onCreate() {
 *         mClient = new AppPredictor(...)
 *         mClient.registerPredictionUpdates(...)
 *    }
 *
 *    void onStart() {
 *        mClient.requestPredictionUpdate()
 *    }
 *
 *    void onClick(...) {
 *        mClient.notifyAppTargetEvent(...)
 *    }
 *
 *    void onDestroy() {
 *        mClient.unregisterPredictionUpdates()
 *        mClient.close()
 *    }
 *
 * }</pre>
 *
 * @hide
 */
@SystemApi
public final class AppPredictor {

    private static final String TAG = AppPredictor.class.getSimpleName();

    private final IPredictionManager mPredictionManager;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private final AtomicBoolean mIsClosed = new AtomicBoolean(false);

    private final AppPredictionSessionId mSessionId;
    @GuardedBy("itself")
    private final ArrayMap<Callback, CallbackWrapper> mRegisteredCallbacks = new ArrayMap<>();

    /**
     * Creates a new Prediction client.
     * <p>
     * The caller should call {@link AppPredictor#destroy()} to dispose the client once it
     * no longer used.
     *
     * @param context The {@link Context} of the user of this {@link AppPredictor}.
     * @param predictionContext The prediction context.
     */
    AppPredictor(@NonNull Context context, @NonNull AppPredictionContext predictionContext) {
        IBinder b = ServiceManager.getService(Context.APP_PREDICTION_SERVICE);
        mPredictionManager = IPredictionManager.Stub.asInterface(b);
        mSessionId = new AppPredictionSessionId(
                context.getPackageName() + ":" + UUID.randomUUID(), context.getUserId());
        try {
            mPredictionManager.createPredictionSession(predictionContext, mSessionId, getToken());
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to create predictor", e);
            e.rethrowAsRuntimeException();
        }

        mCloseGuard.open("AppPredictor.close");
    }

    /**
     * Notifies the prediction service of an app target event.
     *
     * @param event The {@link AppTargetEvent} that represents the app target event.
     */
    public void notifyAppTargetEvent(@NonNull AppTargetEvent event) {
        if (mIsClosed.get()) {
            throw new IllegalStateException("This client has already been destroyed.");
        }

        try {
            mPredictionManager.notifyAppTargetEvent(mSessionId, event);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to notify app target event", e);
            e.rethrowAsRuntimeException();
        }
    }

    /**
     * Notifies the prediction service when the targets in a launch location are shown to the user.
     *
     * @param launchLocation The launch location where the targets are shown to the user.
     * @param targetIds List of {@link AppTargetId}s that are shown to the user.
     */
    public void notifyLaunchLocationShown(@NonNull String launchLocation,
            @NonNull List<AppTargetId> targetIds) {
        if (mIsClosed.get()) {
            throw new IllegalStateException("This client has already been destroyed.");
        }

        try {
            mPredictionManager.notifyLaunchLocationShown(mSessionId, launchLocation,
                    new ParceledListSlice<>(targetIds));
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to notify location shown event", e);
            e.rethrowAsRuntimeException();
        }
    }

    /**
     * Requests the prediction service provide continuous updates of App predictions via the
     * provided callback, until the given callback is unregistered.
     *
     * @see Callback#onTargetsAvailable(List).
     *
     * @param callbackExecutor The callback executor to use when calling the callback.
     * @param callback The Callback to be called when updates of App predictions are available.
     */
    public void registerPredictionUpdates(@NonNull @CallbackExecutor Executor callbackExecutor,
            @NonNull AppPredictor.Callback callback) {
        synchronized (mRegisteredCallbacks) {
            registerPredictionUpdatesLocked(callbackExecutor, callback);
        }
    }

    @GuardedBy("mRegisteredCallbacks")
    private void registerPredictionUpdatesLocked(
            @NonNull @CallbackExecutor Executor callbackExecutor,
            @NonNull AppPredictor.Callback callback) {
        if (mIsClosed.get()) {
            throw new IllegalStateException("This client has already been destroyed.");
        }

        if (mRegisteredCallbacks.containsKey(callback)) {
            // Skip if this callback is already registered
            return;
        }
        try {
            final CallbackWrapper callbackWrapper = new CallbackWrapper(callbackExecutor,
                    callback::onTargetsAvailable);
            mPredictionManager.registerPredictionUpdates(mSessionId, callbackWrapper);
            mRegisteredCallbacks.put(callback, callbackWrapper);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register for prediction updates", e);
            e.rethrowAsRuntimeException();
        }
    }

    /**
     * Requests the prediction service to stop providing continuous updates to the provided
     * callback until the callback is re-registered.
     *
     * @see {@link AppPredictor#registerPredictionUpdates(Executor, Callback)}.
     *
     * @param callback The callback to be unregistered.
     */
    public void unregisterPredictionUpdates(@NonNull AppPredictor.Callback callback) {
        synchronized (mRegisteredCallbacks) {
            unregisterPredictionUpdatesLocked(callback);
        }
    }

    @GuardedBy("mRegisteredCallbacks")
    private void unregisterPredictionUpdatesLocked(@NonNull AppPredictor.Callback callback) {
        if (mIsClosed.get()) {
            throw new IllegalStateException("This client has already been destroyed.");
        }

        if (!mRegisteredCallbacks.containsKey(callback)) {
            // Skip if this callback was never registered
            return;
        }
        try {
            final CallbackWrapper callbackWrapper = mRegisteredCallbacks.remove(callback);
            mPredictionManager.unregisterPredictionUpdates(mSessionId, callbackWrapper);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to unregister for prediction updates", e);
            e.rethrowAsRuntimeException();
        }
    }

    /**
     * Requests the prediction service to dispatch a new set of App predictions via the provided
     * callback.
     *
     * @see Callback#onTargetsAvailable(List).
     */
    public void requestPredictionUpdate() {
        if (mIsClosed.get()) {
            throw new IllegalStateException("This client has already been destroyed.");
        }

        try {
            mPredictionManager.requestPredictionUpdate(mSessionId);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to request prediction update", e);
            e.rethrowAsRuntimeException();
        }
    }

    /**
     * Returns a new list of AppTargets sorted based on prediction rank or {@code null} if the
     * ranker is not available.
     *
     * @param targets List of app targets to be sorted.
     * @param callbackExecutor The callback executor to use when calling the callback.
     * @param callback The callback to return the sorted list of app targets.
     */
    @Nullable
    public void sortTargets(@NonNull List<AppTarget> targets,
            @NonNull Executor callbackExecutor, @NonNull Consumer<List<AppTarget>> callback) {
        if (mIsClosed.get()) {
            throw new IllegalStateException("This client has already been destroyed.");
        }

        try {
            mPredictionManager.sortAppTargets(mSessionId, new ParceledListSlice<>(targets),
                    new CallbackWrapper(callbackExecutor, callback));
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to sort targets", e);
            e.rethrowAsRuntimeException();
        }
    }

    /**
     * Requests a Bundle which includes service features info or {@code null} if the service is not
     * available.
     *
     * @param callbackExecutor The callback executor to use when calling the callback. It cannot be
     *                        null.
     * @param callback The callback to return the Bundle which includes service features info. It
     *                cannot be null.
     *
     * @throws IllegalStateException If this AppPredictor has already been destroyed.
     * @throws RuntimeException If there is a failure communicating with the remote service.
     */
    @FlaggedApi(Flags.FLAG_SERVICE_FEATURES_API)
    public void requestServiceFeatures(@NonNull Executor callbackExecutor,
            @NonNull Consumer<Bundle> callback) {
        if (mIsClosed.get()) {
            throw new IllegalStateException("This client has already been destroyed.");
        }

        try {
            mPredictionManager.requestServiceFeatures(mSessionId,
                    new RemoteCallbackWrapper(callbackExecutor, callback));
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to request service feature info", e);
            e.rethrowAsRuntimeException();
        }
    }

    /**
     * Destroys the client and unregisters the callback. Any method on this class after this call
     * with throw {@link IllegalStateException}.
     */
    public void destroy() {
        if (!mIsClosed.getAndSet(true)) {
            mCloseGuard.close();

            synchronized (mRegisteredCallbacks) {
                destroySessionLocked();
            }
        } else {
            throw new IllegalStateException("This client has already been destroyed.");
        }
    }

    @GuardedBy("mRegisteredCallbacks")
    private void destroySessionLocked() {
        try {
            mPredictionManager.onDestroyPredictionSession(mSessionId);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to notify app target event", e);
            e.rethrowAsRuntimeException();
        }
        mRegisteredCallbacks.clear();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mCloseGuard != null) {
                mCloseGuard.warnIfOpen();
            }
            if (!mIsClosed.get()) {
                destroy();
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Returns the id of this prediction session.
     *
     * @hide
     */
    @TestApi
    public AppPredictionSessionId getSessionId() {
        return mSessionId;
    }

    /**
     * Callback for receiving prediction updates.
     */
    public interface Callback {

        /**
         * Called when a new set of predicted app targets are available.
         * @param targets Sorted list of predicted targets.
         */
        void onTargetsAvailable(@NonNull List<AppTarget> targets);
    }

    static class CallbackWrapper extends Stub {

        private final Consumer<List<AppTarget>> mCallback;
        private final Executor mExecutor;

        CallbackWrapper(@NonNull Executor callbackExecutor,
                @NonNull Consumer<List<AppTarget>> callback) {
            mCallback = callback;
            mExecutor = callbackExecutor;
        }

        @Override
        public void onResult(ParceledListSlice result) {
            final long identity = Binder.clearCallingIdentity();
            try {
                mExecutor.execute(() -> mCallback.accept(result.getList()));
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    static class RemoteCallbackWrapper extends IRemoteCallback.Stub {

        private final Consumer<Bundle> mCallback;
        private final Executor mExecutor;

        RemoteCallbackWrapper(@NonNull Executor callbackExecutor,
                @NonNull Consumer<Bundle> callback) {
            mExecutor = callbackExecutor;
            mCallback = callback;
        }

        @Override
        public void sendResult(Bundle result) {
            final long identity = Binder.clearCallingIdentity();
            try {
                mExecutor.execute(() -> mCallback.accept(result));
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private static class Token {
        static final IBinder sBinder = new Binder(TAG);
    }

    private static IBinder getToken() {
        return Token.sBinder;
    }
}
