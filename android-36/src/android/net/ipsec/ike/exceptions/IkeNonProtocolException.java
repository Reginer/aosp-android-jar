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

package android.net.ipsec.ike.exceptions;

/**
 * IkeNonProtocolException encapsulates all implementation-specific non-protocol IKE errors.
 */
public abstract class IkeNonProtocolException extends IkeException {
    /** @hide */
    protected IkeNonProtocolException() {
        super();
    }

    /** @hide */
    protected IkeNonProtocolException(String message) {
        super(message);
    }

    /** @hide */
    protected IkeNonProtocolException(Throwable cause) {
        super(cause);
    }

    /** @hide */
    protected IkeNonProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
