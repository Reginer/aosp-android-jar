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

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.PersistableBundle;

import com.android.adservices.ondevicepersonalization.flags.Flags;
import com.android.ondevicepersonalization.internal.util.ByteArrayParceledSlice;
import com.android.ondevicepersonalization.internal.util.PersistableBundleUtils;

import java.util.Objects;

/**
 * The input data for {@link IsolatedWorker#onExecute(ExecuteInput, android.os.OutcomeReceiver)}.
 *
 */
@FlaggedApi(Flags.FLAG_ON_DEVICE_PERSONALIZATION_APIS_ENABLED)
public final class ExecuteInput {
    @NonNull private final String mAppPackageName;
    @Nullable private final ByteArrayParceledSlice mSerializedAppParams;
    @NonNull private final Object mAppParamsLock = new Object();
    @NonNull private volatile PersistableBundle mAppParams = null;

    /** @hide */
    public ExecuteInput(@NonNull ExecuteInputParcel parcel) {
        mAppPackageName = Objects.requireNonNull(parcel.getAppPackageName());
        mSerializedAppParams = parcel.getSerializedAppParams();
    }

    /**
     * The package name of the calling app.
     */
    @NonNull public String getAppPackageName() {
        return mAppPackageName;
    }

    /**
     * The parameters provided by the app to the {@link IsolatedService}. The service
     * defines the expected keys in this {@link PersistableBundle}.
     */
    @NonNull public PersistableBundle getAppParams() {
        if (mAppParams != null) {
            return mAppParams;
        }
        synchronized (mAppParamsLock) {
            if (mAppParams != null) {
                return mAppParams;
            }
            try {
                mAppParams = (mSerializedAppParams != null)
                        ? PersistableBundleUtils.fromByteArray(
                                mSerializedAppParams.getByteArray())
                        : PersistableBundle.EMPTY;
                return mAppParams;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
