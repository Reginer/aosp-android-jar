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

package android.adservices.ondevicepersonalization;

import android.adservices.ondevicepersonalization.aidl.IDataAccessService;
import android.adservices.ondevicepersonalization.aidl.IDataAccessServiceCallback;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Bundle;
import android.os.RemoteException;

import com.android.ondevicepersonalization.internal.util.ByteArrayParceledSlice;
import com.android.ondevicepersonalization.internal.util.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/** @hide */
public class LocalDataImpl implements MutableKeyValueStore {
    private static final String TAG = "LocalDataImpl";
    private static final LoggerFactory.Logger sLogger = LoggerFactory.getLogger();
    @NonNull private final IDataAccessService mDataAccessService;

    /** @hide */
    public LocalDataImpl(@NonNull IDataAccessService binder) {
        mDataAccessService = Objects.requireNonNull(binder);
    }

    @Override @Nullable
    public byte[] get(@NonNull String key) {
        final long startTimeMillis = System.currentTimeMillis();
        Objects.requireNonNull(key);
        Bundle params = new Bundle();
        params.putString(Constants.EXTRA_LOOKUP_KEYS, key);
        return handleLookupRequest(
                Constants.DATA_ACCESS_OP_LOCAL_DATA_LOOKUP, params,
                Constants.API_NAME_LOCAL_DATA_GET, startTimeMillis);
    }

    @Override @Nullable
    public byte[] put(@NonNull String key, byte[] value) {
        final long startTimeMillis = System.currentTimeMillis();
        Objects.requireNonNull(key);
        Bundle params = new Bundle();
        params.putString(Constants.EXTRA_LOOKUP_KEYS, key);
        params.putParcelable(Constants.EXTRA_VALUE, new ByteArrayParceledSlice(value));
        return handleLookupRequest(
                Constants.DATA_ACCESS_OP_LOCAL_DATA_PUT, params,
                Constants.API_NAME_LOCAL_DATA_PUT, startTimeMillis);
    }

    @Override @Nullable
    public byte[] remove(@NonNull String key) {
        final long startTimeMillis = System.currentTimeMillis();
        Objects.requireNonNull(key);
        Bundle params = new Bundle();
        params.putString(Constants.EXTRA_LOOKUP_KEYS, key);
        return handleLookupRequest(
                Constants.DATA_ACCESS_OP_LOCAL_DATA_REMOVE, params,
                Constants.API_NAME_LOCAL_DATA_REMOVE, startTimeMillis);
    }

    private byte[] handleLookupRequest(
            int op, Bundle params, int apiName, long startTimeMillis) {
        int responseCode = Constants.STATUS_SUCCESS;
        try {
            CallbackResult callbackResult = handleAsyncRequest(op, params);
            if (callbackResult.mErrorCode != 0) {
                responseCode = callbackResult.mErrorCode;
                return null;
            }
            Bundle result = callbackResult.mResult;
            if (result == null
                    || result.getParcelable(Constants.EXTRA_RESULT, ByteArrayParceledSlice.class)
                    == null) {
                responseCode = Constants.STATUS_SUCCESS_EMPTY_RESULT;
                return null;
            }
            ByteArrayParceledSlice data =
                    result.getParcelable(Constants.EXTRA_RESULT, ByteArrayParceledSlice.class);
            return data.getByteArray();
        } catch (RuntimeException e) {
            responseCode = Constants.STATUS_INTERNAL_ERROR;
            throw e;
        } finally {
            try {
                mDataAccessService.logApiCallStats(
                        apiName,
                        System.currentTimeMillis() - startTimeMillis,
                        responseCode);
            } catch (Exception e) {
                sLogger.d(e, TAG + ": failed to log metrics");
            }
        }
    }

    @Override @NonNull
    public Set<String> keySet() {
        final long startTimeMillis = System.currentTimeMillis();
        int responseCode = Constants.STATUS_SUCCESS;
        try {
            CallbackResult callbackResult =
                    handleAsyncRequest(Constants.DATA_ACCESS_OP_LOCAL_DATA_KEYSET, Bundle.EMPTY);
            if (callbackResult.mErrorCode != 0) {
                responseCode = callbackResult.mErrorCode;
                return Collections.emptySet();
            }
            Bundle result = callbackResult.mResult;
            if (result == null
                    || result.getSerializable(Constants.EXTRA_RESULT, HashSet.class) == null) {
                responseCode = Constants.STATUS_SUCCESS_EMPTY_RESULT;
                return Collections.emptySet();
            }
            return result.getSerializable(Constants.EXTRA_RESULT, HashSet.class);
        } catch (RuntimeException e) {
            responseCode = Constants.STATUS_INTERNAL_ERROR;
            throw e;
        } finally {
            try {
                mDataAccessService.logApiCallStats(
                        Constants.API_NAME_LOCAL_DATA_KEYSET,
                        System.currentTimeMillis() - startTimeMillis,
                        responseCode);
            } catch (Exception e) {
                sLogger.d(e, TAG + ": failed to log metrics");
            }
        }
    }

    @Override
    public int getTableId() {
        return ModelId.TABLE_ID_LOCAL_DATA;
    }

    private CallbackResult handleAsyncRequest(int op, Bundle params) {
        // Blocks on the calling thread and waits for the response from the data access service.
        try {
            BlockingQueue<CallbackResult> asyncResult = new ArrayBlockingQueue<>(1);
            mDataAccessService.onRequest(
                    op,
                    params,
                    new IDataAccessServiceCallback.Stub() {
                        @Override
                        public void onSuccess(@NonNull Bundle result) {
                            asyncResult.add(new CallbackResult(result, /* errorCode= */ 0));
                        }

                        @Override
                        public void onError(int errorCode) {
                            asyncResult.add(new CallbackResult(/* result= */ null, errorCode));
                        }
                    });
            return asyncResult.take();
        } catch (InterruptedException | RemoteException e) {
            sLogger.e(TAG + ": Failed to retrieve result from localData", e);
            throw new IllegalStateException(e);
        }
    }

    private static class CallbackResult {
        private final Bundle mResult;
        private final int mErrorCode;

        private CallbackResult(Bundle result, int errorCode) {
            mResult = result;
            mErrorCode = errorCode;
        }
    }
}
