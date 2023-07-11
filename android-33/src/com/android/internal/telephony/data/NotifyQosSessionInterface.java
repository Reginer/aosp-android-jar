/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.internal.telephony.data;

import android.annotation.NonNull;
import android.net.NetworkAgent;
import android.net.QosSessionAttributes;

/**
 * The temporary interface that is shared by
 * {@link com.android.internal.telephony.dataconnection.DcNetworkAgent} and
 * {@link com.android.internal.telephony.data.TelephonyNetworkAgent} so they can both interact
 * with {@link QosCallbackTracker}.
 */
// TODO: Remove after DcNetworkAgent is removed.
public interface NotifyQosSessionInterface {
    /**
     * Sends the attributes of Qos Session back to the Application. This method is create for
     * Mockito to mock since
     * {@link NetworkAgent#sendQosSessionAvailable(int, int, QosSessionAttributes)} is
     * {@code final} that can't be mocked.
     *
     * @param qosCallbackId the callback id that the session belongs to.
     * @param sessionId the unique session id across all Qos Sessions.
     * @param attributes the attributes of the Qos Session.
     */
    void notifyQosSessionAvailable(int qosCallbackId, int sessionId,
            @NonNull QosSessionAttributes attributes);

    /**
     * Sends event that the Qos Session was lost. This method is create for Mockito to mock
     * since {@link NetworkAgent#sendQosSessionLost(int, int, int)} is {@code final} that can't be
     * mocked..
     *
     * @param qosCallbackId the callback id that the session belongs to.
     * @param sessionId the unique session id across all Qos Sessions.
     * @param qosSessionType the session type {@code QosSession#QosSessionType}.
     */
    void notifyQosSessionLost(int qosCallbackId, int sessionId, int qosSessionType);
}
