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

package android.adservices.common;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;

import com.android.adservices.flags.Flags;

/**
 * Callback interface intended for use when an asynchronous operation may result in a failure. Exact
 * copy of the {@link android.os.OutcomeReceiver} class, re-defined in the AdServices package for
 * backwards compatibility to Android R.
 *
 * <p>This interface may be used in cases where an asynchronous API may complete either with a value
 * or with a {@link Throwable} that indicates an error.
 *
 * @param <R> The type of the result that's being sent.
 * @param <E> The type of the {@link Throwable} that contains more information about the error.
 * @deprecated use {@link android.os.OutcomeReceiver} instead. Android R is no longer supported.
 */
@Deprecated
@FlaggedApi(Flags.FLAG_ADSERVICES_OUTCOMERECEIVER_R_API_DEPRECATED)
public interface AdServicesOutcomeReceiver<R, E extends Throwable> {
    /**
     * Called when the asynchronous operation succeeds and delivers a result value.
     *
     * @param result The value delivered by the asynchronous operation.
     */
    void onResult(R result);

    /**
     * Called when the asynchronous operation fails. The mode of failure is indicated by the {@link
     * Throwable} passed as an argument to this method.
     *
     * @param error A subclass of {@link Throwable} with more details about the error that occurred.
     */
    default void onError(@NonNull E error) {}
}
