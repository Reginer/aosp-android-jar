/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.android.internal.annotations.VisibleForTesting.Visibility.PACKAGE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.ViewRootImpl;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;

/**
 * A {@link OnBackInvokedDispatcher} for IME that forwards {@link OnBackInvokedCallback}
 * registrations from the IME process to the app process to be registered on the app window.
 * <p>
 * The app process creates and propagates an instance of {@link ImeOnBackInvokedDispatcher}
 * to the IME to be set on the IME window's {@link WindowOnBackInvokedDispatcher}.
 * <p>
 * @see WindowOnBackInvokedDispatcher#setImeOnBackInvokedDispatcher
 *
 * @hide
 */
public class ImeOnBackInvokedDispatcher implements OnBackInvokedDispatcher, Parcelable {

    private static final String TAG = "ImeBackDispatcher";
    static final String RESULT_KEY_ID = "id";
    static final String RESULT_KEY_CALLBACK = "callback";
    static final String RESULT_KEY_PRIORITY = "priority";
    static final int RESULT_CODE_REGISTER = 0;
    static final int RESULT_CODE_UNREGISTER = 1;
    static final int RESULT_CODE_START_DISPATCHING = 2;
    static final int RESULT_CODE_STOP_DISPATCHING = 3;
    @NonNull
    private final ResultReceiver mResultReceiver;
    @NonNull
    private final BackProgressAnimator mProgressAnimator = new BackProgressAnimator();
    @NonNull
    private final BackTouchTracker mTouchTracker = new BackTouchTracker();
    // The handler to run callbacks on. This should be on the same thread
    // the ViewRootImpl holding IME's WindowOnBackInvokedDispatcher is created on.
    private Handler mHandler;

    public ImeOnBackInvokedDispatcher(Handler handler) {
        mResultReceiver = new ResultReceiver(handler) {
            @Override
            public void onReceiveResult(int resultCode, Bundle resultData) {
                WindowOnBackInvokedDispatcher dispatcher = getReceivingDispatcher();
                if (dispatcher != null) {
                    receive(resultCode, resultData, dispatcher);
                }
            }
        };
    }

    /**
     * Override this method to return the {@link WindowOnBackInvokedDispatcher} of the window
     * that should receive the forwarded callback.
     */
    @Nullable
    protected WindowOnBackInvokedDispatcher getReceivingDispatcher() {
        return null;
    }

    ImeOnBackInvokedDispatcher(Parcel in) {
        mResultReceiver = in.readTypedObject(ResultReceiver.CREATOR);
    }

    void setHandler(@NonNull Handler handler) {
        mHandler = handler;
    }

    @Override
    public void registerOnBackInvokedCallback(
            @OnBackInvokedDispatcher.Priority int priority,
            @NonNull OnBackInvokedCallback callback) {
        final Bundle bundle = new Bundle();
        // Always invoke back for ime without checking the window focus.
        // We use strong reference in the binder wrapper to avoid accidentally GC the callback.
        // This is necessary because the callback is sent to and registered from
        // the app process, which may treat the IME callback as weakly referenced. This will not
        // cause a memory leak because the app side already clears the reference correctly.
        final IOnBackInvokedCallback iCallback =
                new ImeOnBackInvokedCallbackWrapper(
                        callback,
                        mTouchTracker,
                        mProgressAnimator,
                        this,
                        mHandler != null ? mHandler : Handler.getMain(),
                        false /* useWeakRef */);
        bundle.putBinder(RESULT_KEY_CALLBACK, iCallback.asBinder());
        bundle.putInt(RESULT_KEY_PRIORITY, priority);
        bundle.putInt(RESULT_KEY_ID, callback.hashCode());
        mResultReceiver.send(RESULT_CODE_REGISTER, bundle);
    }

    @Override
    public void unregisterOnBackInvokedCallback(
            @NonNull OnBackInvokedCallback callback) {
        Bundle bundle = new Bundle();
        bundle.putInt(RESULT_KEY_ID, callback.hashCode());
        mResultReceiver.send(RESULT_CODE_UNREGISTER, bundle);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeTypedObject(mResultReceiver, flags);
    }

    /** Sets the progress thresholds for touch tracking */
    public void setProgressThresholds(float linearDistance, float maxDistance,
            float nonLinearFactor) {
        mTouchTracker.setProgressThresholds(linearDistance, maxDistance, nonLinearFactor);
    }

    @NonNull
    public static final Parcelable.Creator<ImeOnBackInvokedDispatcher> CREATOR =
            new Parcelable.Creator<ImeOnBackInvokedDispatcher>() {
                public ImeOnBackInvokedDispatcher createFromParcel(Parcel in) {
                    return new ImeOnBackInvokedDispatcher(in);
                }
                public ImeOnBackInvokedDispatcher[] newArray(int size) {
                    return new ImeOnBackInvokedDispatcher[size];
                }
            };

    private final ArrayList<ImeOnBackInvokedCallback> mImeCallbacks = new ArrayList<>();

    private void receive(
            int resultCode, Bundle resultData,
            @NonNull WindowOnBackInvokedDispatcher receivingDispatcher) {
        if (resultCode == RESULT_CODE_REGISTER) {
            final int callbackId = resultData.getInt(RESULT_KEY_ID);
            int priority = resultData.getInt(RESULT_KEY_PRIORITY);
            final IOnBackInvokedCallback callback = IOnBackInvokedCallback.Stub.asInterface(
                    resultData.getBinder(RESULT_KEY_CALLBACK));
            registerReceivedCallback(
                    callback, priority, callbackId, receivingDispatcher);
        } else if (resultCode == RESULT_CODE_UNREGISTER) {
            final int callbackId = resultData.getInt(RESULT_KEY_ID);
            unregisterReceivedCallback(callbackId, receivingDispatcher);
        } else if (resultCode == RESULT_CODE_START_DISPATCHING) {
            receiveStartDispatching(receivingDispatcher);
        } else if (resultCode == RESULT_CODE_STOP_DISPATCHING) {
            receiveStopDispatching(receivingDispatcher);
        }
    }

    private void registerReceivedCallback(
            @NonNull IOnBackInvokedCallback iCallback,
            @OnBackInvokedDispatcher.Priority int priority,
            int callbackId,
            @NonNull WindowOnBackInvokedDispatcher receivingDispatcher) {
        final ImeOnBackInvokedCallback imeCallback;
        if (priority == PRIORITY_SYSTEM) {
            // A callback registration with PRIORITY_SYSTEM indicates that a predictive back
            // animation can be played on the IME. Therefore register the
            // DefaultImeOnBackInvokedCallback with the receiving dispatcher and override the
            // priority to PRIORITY_DEFAULT.
            priority = PRIORITY_DEFAULT;
            imeCallback = new DefaultImeOnBackAnimationCallback(iCallback, callbackId, priority);
        } else {
            imeCallback = new ImeOnBackInvokedCallback(iCallback, callbackId, priority);
        }
        mImeCallbacks.add(imeCallback);
        receivingDispatcher.registerOnBackInvokedCallbackUnchecked(imeCallback, priority);
    }

    private void unregisterReceivedCallback(
            int callbackId, OnBackInvokedDispatcher receivingDispatcher) {
        ImeOnBackInvokedCallback callback = null;
        for (ImeOnBackInvokedCallback imeCallback : mImeCallbacks) {
            if (imeCallback.getId() == callbackId) {
                callback = imeCallback;
                break;
            }
        }
        if (callback == null) {
            Log.e(TAG, "Ime callback not found. Ignoring unregisterReceivedCallback. "
                    + "callbackId: " + callbackId);
            return;
        }
        receivingDispatcher.unregisterOnBackInvokedCallback(callback);
        mImeCallbacks.remove(callback);
    }

    static class ImeOnBackInvokedCallbackWrapper extends
            WindowOnBackInvokedDispatcher.OnBackInvokedCallbackWrapper {
        @NonNull
        private final ImeOnBackInvokedDispatcher mDispatcher;

        ImeOnBackInvokedCallbackWrapper(
                @NonNull OnBackInvokedCallback callback,
                @NonNull BackTouchTracker touchTracker,
                @NonNull BackProgressAnimator progressAnimator,
                @NonNull ImeOnBackInvokedDispatcher dispatcher,
                @NonNull Handler handler,
                boolean useWeakRef) {
            super(callback, touchTracker, progressAnimator, handler, useWeakRef);
            mDispatcher = dispatcher;
        }

        @Override
        public void onBackStarted(BackMotionEvent backEvent) {
            super.onBackStarted(backEvent);
            mDispatcher.sendStartDispatching();
        }

        @Override
        public void onBackCancelled() {
            super.onBackCancelled();
            mDispatcher.sendStopDispatching();
        }

        @Override
        public void onBackInvoked() throws RemoteException {
            super.onBackInvoked();
            mDispatcher.sendStopDispatching();
        }
    }

    /** Notifies the app process that we've stopped dispatching to an IME callback */
    private void sendStopDispatching() {
        mResultReceiver.send(RESULT_CODE_STOP_DISPATCHING, null /* unused bundle */);
    }

    /** Notifies the app process that we've started dispatching to an IME callback */
    private void sendStartDispatching() {
        mResultReceiver.send(RESULT_CODE_START_DISPATCHING, null /* unused bundle */);
    }

    /** Receives IME's message that dispatching has started. */
    private void receiveStopDispatching(
            @NonNull WindowOnBackInvokedDispatcher receivingDispatcher) {
        receivingDispatcher.onStopImeDispatching();
    }

    /** Receives IME's message that dispatching has stopped. */
    private void receiveStartDispatching(
            @NonNull WindowOnBackInvokedDispatcher receivingDispatcher) {
        receivingDispatcher.onStartImeDispatching();
    }

    /** Clears all registered callbacks on the instance. */
    public void clear() {
        // Unregister previously registered callbacks if there's any.
        if (getReceivingDispatcher() != null) {
            for (ImeOnBackInvokedCallback callback : mImeCallbacks) {
                getReceivingDispatcher().unregisterOnBackInvokedCallback(callback);
            }
        }
        mImeCallbacks.clear();
        // We should also stop running animations since all callbacks have been removed.
        // note: mSpring.skipToEnd(), in ProgressAnimator.reset(), requires the main handler.
        Handler.getMain().post(mProgressAnimator::reset);
        sendStopDispatching();
    }

    @VisibleForTesting(visibility = PACKAGE)
    public static class ImeOnBackInvokedCallback implements OnBackInvokedCallback {
        @NonNull
        private final IOnBackInvokedCallback mIOnBackInvokedCallback;
        /**
         * The hashcode of the callback instance in the IME process, used as a unique id to
         * identify the callback when it's passed between processes.
         */
        private final int mId;
        private final int mPriority;

        ImeOnBackInvokedCallback(@NonNull IOnBackInvokedCallback iCallback, int id,
                @Priority int priority) {
            mIOnBackInvokedCallback = iCallback;
            mId = id;
            mPriority = priority;
        }

        @Override
        public void onBackInvoked() {
            try {
                if (mIOnBackInvokedCallback != null) {
                    mIOnBackInvokedCallback.onBackInvoked();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception when invoking forwarded callback. e: ", e);
            }
        }

        private int getId() {
            return mId;
        }

        IOnBackInvokedCallback getIOnBackInvokedCallback() {
            return mIOnBackInvokedCallback;
        }

        @Override
        public String toString() {
            return "ImeCallback=ImeOnBackInvokedCallback@" + mId
                    + " Callback=" + mIOnBackInvokedCallback;
        }
    }

    /**
     * Subclass of ImeOnBackInvokedCallback indicating that a predictive IME back animation may be
     * played instead of invoking the callback.
     */
    @VisibleForTesting(visibility = PACKAGE)
    public static class DefaultImeOnBackAnimationCallback extends ImeOnBackInvokedCallback {
        DefaultImeOnBackAnimationCallback(@NonNull IOnBackInvokedCallback iCallback, int id,
                int priority) {
            super(iCallback, id, priority);
        }
    }

    /**
     * Transfers {@link ImeOnBackInvokedCallback}s registered on one {@link ViewRootImpl} to
     * another {@link ViewRootImpl} on focus change.
     *
     * @param previous the previously focused {@link ViewRootImpl}.
     * @param current the currently focused {@link ViewRootImpl}.
     */
    public void switchRootView(ViewRootImpl previous, ViewRootImpl current) {
        for (ImeOnBackInvokedCallback imeCallback : mImeCallbacks) {
            if (previous != null) {
                previous.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(imeCallback);
            }
            if (current != null) {
                current.getOnBackInvokedDispatcher().registerOnBackInvokedCallbackUnchecked(
                        imeCallback, imeCallback.mPriority);
            }
        }
    }
}
