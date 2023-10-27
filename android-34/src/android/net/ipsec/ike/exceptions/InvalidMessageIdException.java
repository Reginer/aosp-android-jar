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
package android.net.ipsec.ike.exceptions;

import android.net.ipsec.ike.ChildSessionCallback;
import android.net.ipsec.ike.IkeSessionCallback;

/**
 * This exception is thrown when the remote server received a message with out-of-window-size ID.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-2.3">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 * @hide
 */
// Notifications based on this exception contains the four-octet invalid message ID. It MUST only
// ever be sent in an INFORMATIONAL request. Sending this notification is OPTIONAL, and
// notifications of this type MUST be rate limited.
public final class InvalidMessageIdException extends IkeProtocolException {
    private static final int EXPECTED_ERROR_DATA_LEN = 4;

    /**
     * Construct a instance of InvalidMessageIdException
     *
     * <p>Except for testing, IKE library users normally do not instantiate this object themselves
     * but instead get a reference via {@link IkeSessionCallback} or {@link ChildSessionCallback}.
     *
     * @param messageId the invalid Message ID.
     */
    public InvalidMessageIdException(int messageId) {
        super(
                ERROR_TYPE_INVALID_MESSAGE_ID,
                integerToByteArray(messageId, EXPECTED_ERROR_DATA_LEN));
    }

    /**
     * Construct a instance of InvalidMessageIdException from a notify payload.
     *
     * @param notifyData the notify data included in the payload.
     * @hide
     */
    public InvalidMessageIdException(byte[] notifyData) {
        super(ERROR_TYPE_INVALID_MESSAGE_ID, notifyData);
    }

    /**
     * Return the invalid message ID included in this exception.
     *
     * @return the message ID.
     */
    public int getMessageId() {
        return byteArrayToInteger(getErrorData());
    }

    /** @hide */
    @Override
    protected boolean isValidDataLength(int dataLen) {
        return EXPECTED_ERROR_DATA_LEN == dataLen;
    }
}
