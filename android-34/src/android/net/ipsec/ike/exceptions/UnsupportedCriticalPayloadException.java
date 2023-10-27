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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This exception is thrown when payload type is not supported and critical bit is set
 *
 * @see <a href="https://tools.ietf.org/html/rfc7296#section-2.5">RFC 7296, Internet Key Exchange
 *     Protocol Version 2 (IKEv2)</a>
 * @hide
 */
// Include UNSUPPORTED_CRITICAL_PAYLOAD Notify payloads in a response message. Each payload
// contains only one payload type.
public final class UnsupportedCriticalPayloadException extends IkeProtocolException {
    private static final int EXPECTED_ERROR_DATA_LEN = 1;

    private final List<Integer> mPayloadTypeList;

    /**
     * Construct an instance of UnsupportedCriticalPayloadException.
     *
     * <p>To keep IkeProtocolException simpler, we only pass the first payload type to the
     * superclass which can be retrieved by users.
     *
     * @param payloadList the list of all unsupported critical payload types.
     */
    public UnsupportedCriticalPayloadException(@NonNull List<Integer> payloadList) {
        super(
                ERROR_TYPE_UNSUPPORTED_CRITICAL_PAYLOAD,
                integerToByteArray(payloadList.get(0), EXPECTED_ERROR_DATA_LEN));
        Objects.requireNonNull(payloadList, "payloadList is null");
        mPayloadTypeList = Collections.unmodifiableList(new ArrayList<>(payloadList));
    }

    /**
     * Construct a instance of UnsupportedCriticalPayloadException from a notify payload.
     *
     * @param notifyData the notify data included in the payload.
     * @hide
     */
    public UnsupportedCriticalPayloadException(byte[] notifyData) {
        super(ERROR_TYPE_UNSUPPORTED_CRITICAL_PAYLOAD, notifyData);
        mPayloadTypeList = Collections.singletonList(byteArrayToInteger(notifyData));
    }

    /**
     * Return the all the unsupported critical payloads included in this exception.
     *
     * @return the unsupported critical payload list.
     */
    @NonNull
    public List<Integer> getUnsupportedCriticalPayloadList() {
        return Collections.unmodifiableList(mPayloadTypeList);
    }

    /** @hide */
    @Override
    protected boolean isValidDataLength(int dataLen) {
        return EXPECTED_ERROR_DATA_LEN == dataLen;
    }
}
