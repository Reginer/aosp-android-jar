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

package android.ondevicepersonalization;

import android.annotation.NonNull;
import android.ondevicepersonalization.aidl.IDataAccessService;
import android.ondevicepersonalization.aidl.IDataAccessServiceCallback;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/** @hide */
public class RemoteDataImpl implements ImmutableMap {
    private static final String TAG = "RemoteDataImpl";
    @NonNull
    IDataAccessService mDataAccessService;

    private static final long ASYNC_TIMEOUT_MS = 1000;

    public RemoteDataImpl(@NonNull IDataAccessService binder) {
        mDataAccessService = Objects.requireNonNull(binder);
    }

    @Override
    public byte[] get(@NonNull String key) throws OnDevicePersonalizationException {
        try {
            BlockingQueue<Bundle> asyncResult = new ArrayBlockingQueue<>(1);
            Bundle params = new Bundle();
            params.putStringArray(Constants.EXTRA_LOOKUP_KEYS, new String[]{key});
            mDataAccessService.onRequest(
                    Constants.DATA_ACCESS_OP_REMOTE_DATA_LOOKUP,
                    params,
                    new IDataAccessServiceCallback.Stub() {
                        @Override
                        public void onSuccess(@NonNull Bundle result) {
                            asyncResult.add(result);
                        }

                        @Override
                        public void onError(int errorCode) {
                            asyncResult.add(Bundle.EMPTY);
                        }
                    });
            Bundle result = asyncResult.poll(ASYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (null == result) {
                Log.e(TAG, "Timed out waiting for result of remoteData lookup");
                throw new OnDevicePersonalizationException(Constants.STATUS_INTERNAL_ERROR);
            }
            HashMap<String, byte[]> data = result.getSerializable(
                            Constants.EXTRA_RESULT, HashMap.class);
            if (null == data) {
                Log.e(TAG, "No EXTRA_RESULT was present in bundle");
                throw new OnDevicePersonalizationException(Constants.STATUS_INTERNAL_ERROR);
            }
            return data.get(key);
        } catch (InterruptedException | RemoteException e) {
            Log.e(TAG, "Failed to retrieve key from remoteData", e);
            throw new OnDevicePersonalizationException(Constants.STATUS_INTERNAL_ERROR);
        }
    }

    @Override
    public Set<String> keySet() throws OnDevicePersonalizationException {
        try {
            BlockingQueue<Bundle> asyncResult = new ArrayBlockingQueue<>(1);
            mDataAccessService.onRequest(
                    Constants.DATA_ACCESS_OP_REMOTE_DATA_KEYSET,
                    Bundle.EMPTY,
                    new IDataAccessServiceCallback.Stub() {
                        @Override
                        public void onSuccess(@NonNull Bundle result) {
                            asyncResult.add(result);
                        }

                        @Override
                        public void onError(int errorCode) {
                            asyncResult.add(Bundle.EMPTY);
                        }
                    });
            Bundle result = asyncResult.poll(ASYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (null == result) {
                Log.e(TAG, "Timed out waiting for result of remoteData keySet");
                throw new OnDevicePersonalizationException(Constants.STATUS_INTERNAL_ERROR);
            }
            HashSet<String> resultSet =
                    result.getSerializable(Constants.EXTRA_RESULT, HashSet.class);
            if (null == resultSet) {
                Log.e(TAG, "No EXTRA_RESULT was present in bundle");
                throw new OnDevicePersonalizationException(Constants.STATUS_INTERNAL_ERROR);
            }
            return resultSet;
        } catch (InterruptedException | RemoteException e) {
            Log.e(TAG, "Failed to retrieve keySet from remoteData", e);
            throw new OnDevicePersonalizationException(Constants.STATUS_INTERNAL_ERROR);
        }
    }
}
