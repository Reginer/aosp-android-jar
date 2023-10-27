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
package android.net.ipsec.ike.exceptions;

import android.annotation.NonNull;
import android.net.ipsec.ike.ChildSessionCallback;
import android.net.ipsec.ike.IkeSessionCallback;

import java.io.IOException;

/**
 * This exception is thrown when there is an IKE retransmission timeout.
 *
 * <p>This exception indicates that the IKE Session failed to receive an IKE response before hitting
 * the maximum number of retransmission attempts configured in {@link
 * android.net.ipsec.ike.IkeSessionParams.Builder#setRetransmissionTimeoutsMillis(int[])}.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7296#section-2.1">RFC 7296, Internet Key
 *     Exchange Protocol Version 2 (IKEv2)</a>
 */
public final class IkeTimeoutException extends IOException {

    /**
     * Constructs an {@code IkeTimeoutException} with the specified detail message.
     *
     * <p>Callers are not generally expected to instantiate this object themselves, except for
     * testing. A reference is passed via {@link IkeSessionCallback} or {@link
     * ChildSessionCallback}.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link
     *     #getMessage()} method)
     */
    public IkeTimeoutException(@NonNull String message) {
        super(message);
    }
}
