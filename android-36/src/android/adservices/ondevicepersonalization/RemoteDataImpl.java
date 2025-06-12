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
public class RemoteDataImpl implements KeyValueStore {
    private static final LoggerFactory.Logger sLogger = LoggerFactory.getLogger();
    private static final String TAG = "RemoteDataImpl";
    @NonNull
    IDataAccessService mDataAccessService;

    /** @hide */
    public RemoteDataImpl(@NonNull IDataAccessService binder) {
        mDataAccessService = Objects.requireNonNull(binder);
    }

    @Override @Nullable
    public byte[] get(@NonNull String key) {
        Objects.requireNonNull(key);
        final long startTimeMillis = System.currentTimeMillis();
        int responseCode = Constants.STATUS_SUCCESS;
        try {
            BlockingQueue<CallbackResult> asyncResult = new ArrayBlockingQueue<>(1);
            Bundle params = new Bundle();
            params.putString(Constants.EXTRA_LOOKUP_KEYS, key);
            mDataAccessService.onRequest(
                    Constants.DATA_ACCESS_OP_REMOTE_DATA_LOOKUP,
                    params,
                    new IDataAccessServiceCallback.Stub() {
                        @Override
                        public void onSuccess(@NonNull Bundle result) {
                            asyncResult.add(new CallbackResult(result, 0));
                        }

                        @Override
                        public void onError(int errorCode) {
                            asyncResult.add(new CallbackResult(Bundle.EMPTY, errorCode));
                        }
                    });

            CallbackResult callbackResult = asyncResult.take();
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
        } catch (InterruptedException | RemoteException e) {
            sLogger.e(TAG + ": Failed to retrieve key from remoteData", e);
            responseCode = Constants.STATUS_INTERNAL_ERROR;
            throw new IllegalStateException(e);
        } finally {
            try {
                mDataAccessService.logApiCallStats(
                        Constants.API_NAME_REMOTE_DATA_GET,
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
            BlockingQueue<CallbackResult> asyncResult = new ArrayBlockingQueue<>(1);
            mDataAccessService.onRequest(
                    Constants.DATA_ACCESS_OP_REMOTE_DATA_KEYSET,
                    Bundle.EMPTY,
                    new IDataAccessServiceCallback.Stub() {
                        @Override
                        public void onSuccess(@NonNull Bundle result) {
                            asyncResult.add(new CallbackResult(result, 0));
                        }

                        @Override
                        public void onError(int errorCode) {
                            asyncResult.add(new CallbackResult(null, errorCode));
                        }
                    });
            CallbackResult callbackResult = asyncResult.take();
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
        } catch (InterruptedException | RemoteException e) {
            sLogger.e(TAG + ": Failed to retrieve keySet from remoteData", e);
            responseCode = Constants.STATUS_INTERNAL_ERROR;
            throw new IllegalStateException(e);
        } finally {
            try {
                mDataAccessService.logApiCallStats(
                        Constants.API_NAME_REMOTE_DATA_KEYSET,
                        System.currentTimeMillis() - startTimeMillis,
                        responseCode);
            } catch (Exception e) {
                sLogger.d(e, TAG + ": failed to log metrics");
            }
        }
    }

    @Override
    public int getTableId() {
        return ModelId.TABLE_ID_REMOTE_DATA;
    }

    private static class CallbackResult {
        final Bundle mResult;
        final int mErrorCode;

        CallbackResult(Bundle result, int errorCode) {
            mResult = result;
            mErrorCode = errorCode;
        }
    }
}
