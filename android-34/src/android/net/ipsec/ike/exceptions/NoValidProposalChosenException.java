/*
 * Copyright (C) 2018 The Android Open Source Project
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

/**
 * This exception is thrown if a SA proposal negotiation failed.
 *
 * <p>This exception indicates that either none of SA proposals from caller is acceptable or the
 * negotiated SA proposal from the remote server is invalid.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-2.7">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 * @hide
 */
// Include the NO_PROPOSAL_CHOSEN Notify payload in an encrypted response message if received
// message is an encrypted request from SA initiator.
public final class NoValidProposalChosenException extends IkeProtocolException {
    private static final int EXPECTED_ERROR_DATA_LEN = 0;

    /**
     * Construct an instance of NoValidProposalChosenException.
     *
     * <p>Except for testing, IKE library users normally do not instantiate this object themselves
     * but instead get a reference via {@link IkeSessionCallback} or {@link ChildSessionCallback}.
     *
     * @param message the descriptive message (which is saved for later retrieval by the {@link
     *     #getMessage()} method).
     */
    public NoValidProposalChosenException(@NonNull String message) {
        super(ERROR_TYPE_NO_PROPOSAL_CHOSEN, message);
    }

    /**
     * Construct an instance of NoValidProposalChosenException.
     *
     * <p>Except for testing, IKE library users normally do not instantiate this object themselves
     * but instead get a reference via {@link IkeSessionCallback} or {@link ChildSessionCallback}.
     *
     * @param message the descriptive message (which is saved for later retrieval by the {@link
     *     #getMessage()} method).
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()}
     *     method).
     */
    public NoValidProposalChosenException(@NonNull String message, @NonNull Throwable cause) {
        super(ERROR_TYPE_NO_PROPOSAL_CHOSEN, cause);
    }

    /**
     * Construct a instance of NoValidProposalChosenException from a notify payload.
     *
     * @param notifyData the notify data included in the payload.
     * @hide
     */
    public NoValidProposalChosenException(byte[] notifyData) {
        super(ERROR_TYPE_NO_PROPOSAL_CHOSEN, notifyData);
    }

    /** @hide */
    @Override
    protected boolean isValidDataLength(int dataLen) {
        return EXPECTED_ERROR_DATA_LEN == dataLen;
    }
}
