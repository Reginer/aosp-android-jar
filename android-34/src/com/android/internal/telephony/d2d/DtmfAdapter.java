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

package com.android.internal.telephony.d2d;

import android.os.Message;

/**
 * Abstracts interaction with DTMF communication APIs.
 */
public interface DtmfAdapter {
    /**
     * Called when a DTMF digit should be sent to the network.
     * <p>
     * In concrete implementations, should be linked to
     * {@link android.telephony.ims.stub.ImsCallSessionImplBase#sendDtmf(char, Message)}.
     * @param digit The DTMF digit to send.
     */
    void sendDtmf(char digit);
}
