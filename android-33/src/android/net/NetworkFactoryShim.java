/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.net;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Looper;
import android.os.Message;

import com.android.internal.annotations.VisibleForTesting;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Extract an interface for multiple implementation of {@link NetworkFactory}, depending on the SDK
 * version.
 *
 * Known implementations:
 * - {@link NetworkFactoryImpl}: For Android S+
 * - {@link NetworkFactoryLegacyImpl}: For Android R-
 *
 * @hide
 */
interface NetworkFactoryShim {
    void register(String logTag);

    default void registerIgnoringScore(String logTag) {
        throw new UnsupportedOperationException();
    }

    void terminate();

    void releaseRequestAsUnfulfillableByAnyFactory(NetworkRequest r);

    void reevaluateAllRequests();

    void setScoreFilter(int score);

    void setScoreFilter(@NonNull NetworkScore score);

    void setCapabilityFilter(NetworkCapabilities netCap);

    int getRequestCount();

    int getSerialNumber();

    NetworkProvider getProvider();

    void dump(FileDescriptor fd, PrintWriter writer, String[] args);

    // All impls inherit Handler
    @VisibleForTesting
    Message obtainMessage(int what, int arg1, int arg2, @Nullable Object obj);

    Looper getLooper();
}
