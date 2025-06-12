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

import android.os.Build;
import android.os.OutcomeReceiver;

import androidx.annotation.RequiresApi;

/**
 * Utility class to convert between {@link OutcomeReceiver} and {@link AdServicesOutcomeReceiver}.
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.S)
public final class OutcomeReceiverConverter {
    private OutcomeReceiverConverter() {
        // Prevent instantiation
    }

    /**
     * Converts an instance of custom {@link AdServicesOutcomeReceiver} to a {@link
     * OutcomeReceiver}.
     *
     * @param callback the instance of {@link AdServicesOutcomeReceiver} to wrap
     * @return an {@link OutcomeReceiver} that wraps the original input
     * @param <R> the type of Result that the receiver can process
     * @param <E> the type of Exception that can be handled by the receiver
     */
    public static <R, E extends Throwable> OutcomeReceiver<R, E> toOutcomeReceiver(
            AdServicesOutcomeReceiver<R, E> callback) {
        if (callback == null) {
            return null;
        }

        return new OutcomeReceiver<R, E>() {
            @Override
            public void onResult(R result) {
                callback.onResult(result);
            }

            @Override
            public void onError(E error) {
                callback.onError(error);
            }
        };
    }
}
