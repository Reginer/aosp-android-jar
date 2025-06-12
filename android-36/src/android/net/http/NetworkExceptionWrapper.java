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

package android.net.http;

public class NetworkExceptionWrapper extends android.net.http.NetworkException {

    private final org.chromium.net.NetworkException backend;

    NetworkExceptionWrapper(org.chromium.net.NetworkException backend) {
        this(backend, false);
    }

    NetworkExceptionWrapper(
            org.chromium.net.NetworkException backend, boolean expectQuicException) {
        super(backend.getMessage(), backend);
        this.backend = backend;

        if (!expectQuicException && backend instanceof org.chromium.net.QuicException) {
            throw new IllegalArgumentException(
                    "Translating QuicException as NetworkException results in loss of information."
                        + " Make sure you handle QuicException first. See the stacktrace for where"
                        + " the translation is being performed, and the cause for the exception"
                        + " being translated.");
        }
    }

    @Override
    public int getErrorCode() {
        return backend.getErrorCode();
    }

    @Override
    public boolean isImmediatelyRetryable() {
        return backend.immediatelyRetryable();
    }
}
