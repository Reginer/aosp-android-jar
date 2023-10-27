/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.internal.net.ipsec.ike.ike3gpp;

import static android.net.ipsec.ike.IkeManager.getIkeLog;

import android.annotation.NonNull;
import android.net.ipsec.ike.ike3gpp.Ike3gppData;
import android.net.ipsec.ike.ike3gpp.Ike3gppExtension;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Ike3gppExchangeBase is the base for IKE exchange-specific 3GPP functionality.
 *
 * <p>This class is package-private.
 */
abstract class Ike3gppExchangeBase {
    private static final String TAG = Ike3gppExchangeBase.class.getSimpleName();

    @NonNull protected final Ike3gppExtension mIke3gppExtension;
    @NonNull private final Executor mUserCbExecutor;

    /** Initializes an Ike3gppExchangeBase. */
    Ike3gppExchangeBase(
            @NonNull Ike3gppExtension ike3gppExtension, @NonNull Executor userCbExecutor) {
        mIke3gppExtension =
                Objects.requireNonNull(ike3gppExtension, "ike3gppExtension must not be null");
        mUserCbExecutor = Objects.requireNonNull(userCbExecutor, "userCbExecutor must not be null");
    }

    void maybeInvokeUserCallback(List<Ike3gppData> ike3gppDataList) {
        if (ike3gppDataList.isEmpty()) return;

        try {
            mUserCbExecutor.execute(
                    () ->
                            mIke3gppExtension
                                    .getIke3gppDataListener()
                                    .onIke3gppDataReceived(ike3gppDataList));
        } catch (Exception e) {
            getIkeLog().d(TAG, "Ike3gppDataListener#onIke3gppDataReceived execution failed", e);
        }
    }
}
