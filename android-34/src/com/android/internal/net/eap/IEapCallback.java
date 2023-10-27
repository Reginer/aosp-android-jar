/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.net.eap;

import android.annotation.Nullable;
import android.net.eap.EapInfo;

/**
 * IEapCallback represents a Callback interface to be implemented by clients of the
 * {@link EapAuthenticator}.
 *
 * <p>Exactly one of these callbacks will be called for each message processed by the
 * {@link EapAuthenticator}.
 *
 * @see <a href="https://tools.ietf.org/html/rfc3748#section-4">RFC 3748, Extensible Authentication
 * Protocol (EAP)</a>
 */
public interface IEapCallback {
    /**
     * Callback used to indicate that the EAP Authentication session was successful.
     *
     * @param msk The Master Session Key (MSK) generated in the session
     * @param emsk The Extended Master Session Key (EMSK) generated in the session
     * @param eapInfo EAP information {@link EapInfo}
     */
    void onSuccess(byte[] msk, byte[] emsk, @Nullable EapInfo eapInfo);

    /**
     * Callback used to indicate that the EAP Authentication Session was unsuccessful.
     */
    void onFail();

    /**
     * Callback used to return an EAP-Response message for the message being processed.
     *
     * @param eapMsg byte-array encoded EAP-Response message to be sent to the Authentication server
     * @param flagMask contains flags that convey additional high level EAP state information that
     *     is relevant to the clients. Clients can use
     *     {@link com.android.internal.net.eap.EapResult.EapResponse.hasFlag() to check if a
     *     specific flag is set in the flagMask. The flagMask is set 0 when no flags are set.
     */
    void onResponse(byte[] eapMsg, int flagMask);

    /**
     * Callback used to indicate that there was an error processing the current EAP message.
     *
     * @param cause The cause of the processing error
     */
    void onError(Throwable cause);
}
