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

package android.federatedcompute;

import android.annotation.NonNull;
import android.os.Bundle;

import java.io.Closeable;

/**
 * Iterator interface that client apps implement to return training examples. When FederatedCompute
 * runs a computation, it will call into this interface to fetech training examples to feed to the
 * computation.
 *
 * @hide
 */
public interface ExampleStoreIterator extends Closeable {
    /** Called when FederatedCompute needs another example. */
    void next(@NonNull IteratorCallback callback);
    /** Called by FederatedCompute when it is done using this iterator instance. */
    @Override
    void close();
    /** The client app must implement this callback return training examples. */
    public interface IteratorCallback {
        /**
         * Called when the result for {@link ExampleStoreIterator#next} is available, or when the
         * end of the collection has been reached.
         */
        boolean onIteratorNextSuccess(Bundle result);
        /** Called when an error occurred and the result cannot be returned. */
        void onIteratorNextFailure(int errorCode);
    }
}
