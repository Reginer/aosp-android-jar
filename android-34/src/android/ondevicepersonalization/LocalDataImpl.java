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
public class LocalDataImpl implements MutableMap {
    private static final String TAG = "LocalDataImpl";
    @NonNull
    IDataAccessService mDataAccessService;

    private static final long ASYNC_TIMEOUT_MS = 1000;

    public LocalDataImpl(@NonNull IDataAccessService binder) {
        mDataAccessService = Objects.requireNonNull(binder);
    }

    @Override
    public byte[] get(@NonNull String key) throws OnDevicePersonalizationException {
        Bundle params = new Bundle();
        params.putStringArray(Constants.EXTRA_LOOKUP_KEYS, new String[]{key});
        return handleLookupRequest(Constants.DATA_ACCESS_OP_LOCAL_DATA_LOOKUP, key, params);
    }

    @Override
    public byte[] put(@NonNull String key, byte[] value) throws OnDevicePersonalizationException {
        Bundle params = new Bundle();
        params.putStringArray(Constants.EXTRA_LOOKUP_KEYS, new String[]{key});
        params.putByteArray(Constants.EXTRA_VALUE, value);
        return handleLookupRequest(Constants.DATA_ACCESS_OP_LOCAL_DATA_PUT, key, params);
    }

    @Override
    public byte[] remove(@NonNull String key) throws OnDevicePersonalizationException {
        Bundle params = new Bundle();
        params.putStringArray(Constants.EXTRA_LOOKUP_KEYS, new String[]{key});
        return handleLookupRequest(Constants.DATA_ACCESS_OP_LOCAL_DATA_REMOVE, key, params);
    }

    private byte[] handleLookupRequest(int op, String key, Bundle params)
            throws OnDevicePersonalizationException {
        Bundle result = handleAsyncRequest(op, params);
        if (null == result) {
            Log.e(TAG, "Timed out waiting for result of lookup for op: " + op);
            throw new OnDevicePersonalizationException(Constants.STATUS_INTERNAL_ERROR);
        }
        HashMap<String, byte[]> data = result.getSerializable(
                Constants.EXTRA_RESULT, HashMap.class);
        if (null == data) {
            Log.e(TAG, "No EXTRA_RESULT was present in bundle");
            throw new OnDevicePersonalizationException(Constants.STATUS_INTERNAL_ERROR);
        }
        return data.get(key);
    }

    @Override
    public Set<String> keySet() throws OnDevicePersonalizationException {
        Bundle result = handleAsyncRequest(Constants.DATA_ACCESS_OP_LOCAL_DATA_KEYSET,
                Bundle.EMPTY);
        if (null == result) {
            Log.e(TAG, "Timed out waiting for result of keySet");
            throw new OnDevicePersonalizationException(Constants.STATUS_INTERNAL_ERROR);
        }
        HashSet<String> resultSet =
                result.getSerializable(Constants.EXTRA_RESULT, HashSet.class);
        if (null == resultSet) {
            Log.e(TAG, "No EXTRA_RESULT was present in bundle");
            throw new OnDevicePersonalizationException(Constants.STATUS_INTERNAL_ERROR);
        }
        return resultSet;
    }

    private Bundle handleAsyncRequest(int op, Bundle params)
            throws OnDevicePersonalizationException {
        try {
            BlockingQueue<Bundle> asyncResult = new ArrayBlockingQueue<>(1);
            mDataAccessService.onRequest(
                    op,
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
            return asyncResult.poll(ASYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | RemoteException e) {
            Log.e(TAG, "Failed to retrieve result from localData", e);
            throw new OnDevicePersonalizationException(Constants.STATUS_INTERNAL_ERROR);
        }
    }
}
