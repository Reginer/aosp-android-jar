/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.federatedcompute;

import android.annotation.NonNull;
import android.federatedcompute.ExampleStoreService.QueryCallback;
import android.federatedcompute.aidl.IExampleStoreCallback;
import android.federatedcompute.aidl.IExampleStoreIterator;
import android.federatedcompute.aidl.IExampleStoreIteratorCallback;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.util.Preconditions;

/**
 * Forward results from the app to the wrapped AIDL callback.
 *
 * @hide
 */
public class ExampleStoreQueryCallbackImpl implements QueryCallback {
    private static final String TAG = "ExampleStoreQueryCallbackImpl";
    private final IExampleStoreCallback mExampleStoreQueryCallback;

    public ExampleStoreQueryCallbackImpl(IExampleStoreCallback exampleStoreQueryCallback) {
        this.mExampleStoreQueryCallback = exampleStoreQueryCallback;
    }

    @Override
    public void onStartQuerySuccess(@NonNull ExampleStoreIterator iterator) {
        Preconditions.checkNotNull(iterator, "iterator must not be null");
        IteratorAdapter iteratorAdapter = new IteratorAdapter(iterator);
        try {
            mExampleStoreQueryCallback.onStartQuerySuccess(iteratorAdapter);
        } catch (RemoteException e) {
            Log.w(TAG, "onIteratorNextSuccess AIDL call failed, closing iterator", e);
            iteratorAdapter.close();
        }
    }

    @Override
    public void onStartQueryFailure(int errorCode) {
        try {
            mExampleStoreQueryCallback.onStartQueryFailure(errorCode);
        } catch (RemoteException e) {
            Log.w(TAG, "onIteratorNextFailure AIDL call failed, closing iterator", e);
        }
    }
    /**
     * The implementation of {@link IExampleStoreIterator}.
     *
     * @hide
     */
    public static class IteratorAdapter extends IExampleStoreIterator.Stub {
        private final ExampleStoreIterator mIterator;
        private final Object mLock = new Object();
        private boolean mClosed = false;

        public IteratorAdapter(ExampleStoreIterator iterator) {
            this.mIterator = iterator;
        }

        @Override
        public void next(IExampleStoreIteratorCallback callback) {
            Preconditions.checkNotNull(callback, "callback must not be null");
            synchronized (mLock) {
                if (mClosed) {
                    Log.w(TAG, "IExampleStoreIterator.next called after close");
                    return;
                }
                IteratorCallbackAdapter callbackAdapter =
                        new IteratorCallbackAdapter(callback, this);
                mIterator.next(callbackAdapter);
            }
        }

        @Override
        public void close() {
            synchronized (mLock) {
                if (mClosed) {
                    Log.w(TAG, "IExampleStoreIterator.close called more than once");
                    return;
                }
                mClosed = true;
            }
            mIterator.close();
        }
    }
    /**
     * The implementation of {@link ExampleStoreIterator.IteratorCallback} that FederatedCompute
     * pass to the apps.
     *
     * @hide
     */
    public static final class IteratorCallbackAdapter
            implements ExampleStoreIterator.IteratorCallback {
        private final IExampleStoreIteratorCallback mExampleStoreIteratorCallback;
        private final IteratorAdapter mIteratorAdapter;

        public IteratorCallbackAdapter(
                IExampleStoreIteratorCallback exampleStoreIteratorCallback,
                IteratorAdapter iteratorAdapter) {
            this.mExampleStoreIteratorCallback = exampleStoreIteratorCallback;
            this.mIteratorAdapter = iteratorAdapter;
        }

        @Override
        public boolean onIteratorNextSuccess(Bundle result) {
            try {
                mExampleStoreIteratorCallback.onIteratorNextSuccess(result);
                return true;
            } catch (RemoteException e) {
                Log.w(TAG, "onIteratorNextSuccess AIDL call failed, closing iterator", e);
                mIteratorAdapter.close();
            }
            return false;
        }

        @Override
        public void onIteratorNextFailure(int errorCode) {
            try {
                mExampleStoreIteratorCallback.onIteratorNextFailure(errorCode);
            } catch (RemoteException e) {
                Log.w(TAG, "onIteratorNextFailure AIDL call failed, closing iterator", e);
                mIteratorAdapter.close();
            }
        }
    }
}
