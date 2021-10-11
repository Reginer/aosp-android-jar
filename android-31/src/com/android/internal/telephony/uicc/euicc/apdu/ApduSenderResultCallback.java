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

package com.android.internal.telephony.uicc.euicc.apdu;

import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.euicc.async.AsyncResultCallback;

/**
 * Class to deliver the returned bytes from {@link ApduSender}.
 *
 * @hide
 */
public abstract class ApduSenderResultCallback extends AsyncResultCallback<byte[]> {

    /**
     * This will be called when a result to an intermediate APDU is returned. It will be called on
     * all APDU commands except the last one and executed in the {@link ApduSender}'s thread.
     *
     * @return True if next APDU command should be sent, otherwise the remaining APDU commands will
     *     be skipped.
     */
    public abstract boolean shouldContinueOnIntermediateResult(IccIoResult result);
}
