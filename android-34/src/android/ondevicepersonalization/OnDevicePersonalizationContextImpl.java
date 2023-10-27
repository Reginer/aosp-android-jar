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

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.ondevicepersonalization.aidl.IDataAccessService;
import android.ondevicepersonalization.aidl.IDataAccessServiceCallback;
import android.os.Bundle;
import android.os.OutcomeReceiver;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Container for per-request state and APIs for code that runs in the isolated
 * process.
 *
 * @hide
 */
public class OnDevicePersonalizationContextImpl implements OnDevicePersonalizationContext {
    @NonNull private IDataAccessService mDataAccessService;
    @NonNull private ImmutableMap mRemoteData;
    @NonNull private MutableMap mLocalData;

    /** @hide */
    public OnDevicePersonalizationContextImpl(@NonNull IDataAccessService binder) {
        mDataAccessService = Objects.requireNonNull(binder);
        mRemoteData = new RemoteDataImpl(binder);
        mLocalData = new LocalDataImpl(binder);
    }

    @Override @NonNull public ImmutableMap getRemoteData() {
        return mRemoteData;
    }

    @Override @NonNull public MutableMap getLocalData() {
        return mLocalData;
    }

    @Override public void getEventUrl(
            int eventType,
            @NonNull String bidId,
            @NonNull EventUrlOptions options,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<String, Exception> receiver) {
        try {
            Bundle params = new Bundle();
            params.putInt(Constants.EXTRA_EVENT_TYPE, eventType);
            params.putString(Constants.EXTRA_BID_ID, bidId);
            params.putString(Constants.EXTRA_DESTINATION_URL, options.getDestinationUrl());
            mDataAccessService.onRequest(
                    Constants.DATA_ACCESS_OP_GET_EVENT_URL,
                    params,
                    new IDataAccessServiceCallback.Stub() {
                        @Override
                        public void onSuccess(@NonNull Bundle result) {
                            executor.execute(() -> {
                                try {
                                    String url = result.getString(Constants.EXTRA_RESULT);
                                    receiver.onResult(url);
                                } catch (Exception e) {
                                    receiver.onError(e);
                                }
                            });
                        }
                        @Override
                        public void onError(int errorCode) {
                            executor.execute(() -> {
                                receiver.onError(new OnDevicePersonalizationException(errorCode));
                            });
                        }
                });
        } catch (Exception e) {
            receiver.onError(e);
        }
    }
}
