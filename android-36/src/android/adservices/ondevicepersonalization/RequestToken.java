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
import android.adservices.ondevicepersonalization.aidl.IFederatedComputeService;
import android.adservices.ondevicepersonalization.aidl.IIsolatedModelService;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.SystemClock;

import java.util.Objects;

/**
 * An opaque token that identifies the current request to an {@link IsolatedService}. This token
 * must be passed as a parameter to all service methods that depend on per-request state.
 */
public class RequestToken {
    @NonNull
    private final IDataAccessService mDataAccessService;

    @Nullable
    private final IFederatedComputeService mFcService;

    @Nullable private final IIsolatedModelService mModelService;

    @Nullable
    private final UserData mUserData;

    private final long mStartTimeMillis;

    /** @hide */
    RequestToken(
            @NonNull IDataAccessService binder,
            @Nullable IFederatedComputeService fcServiceBinder,
            @Nullable IIsolatedModelService modelServiceBinder,
            @Nullable UserData userData) {
        mDataAccessService = Objects.requireNonNull(binder);
        mFcService = fcServiceBinder;
        mModelService = modelServiceBinder;
        mUserData = userData;
        mStartTimeMillis = SystemClock.elapsedRealtime();
    }

    /** @hide */
    @NonNull
    IDataAccessService getDataAccessService() {
        return mDataAccessService;
    }

    /** @hide */
    @Nullable
    IFederatedComputeService getFederatedComputeService() {
        return mFcService;
    }

    /** @hide */
    @Nullable
    IIsolatedModelService getModelService() {
        return mModelService;
    }

    /** @hide */
    @Nullable
    UserData getUserData() {
        return mUserData;
    }

    /** @hide */
    long getStartTimeMillis() {
        return mStartTimeMillis;
    }
}
