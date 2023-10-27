/*
 * Copyright (c) 2019, The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     - Neither the name of The Android Open Source Project nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL MOTOROLA MOBILITY LLC BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package com.android.service.ims.presence;

import android.telephony.ims.RcsContactUceCapability;

import java.util.List;

public interface ContactCapabilityResponse {

    /**
     * Called when a capability request returns a "200 OK" (means capable) or "404 xxxx" for SIP
     * request of single contact number (means not capable).
     *
     * @param reqId the request ID which is returned by requestCapability or
     * requestAvailability
     */
    void onSuccess(int reqId);

    /**
     * Called when a local error is generated a SIP error is generated from the network.
     *
     * @param reqId the request ID which is returned by requestCapability or
     * requestAvailability
     * @param resultCode the result code which is defined in RcsManager.ResultCode.
     */
    void onError(int reqId, int resultCode);

    /**
     * Called when the request returns a "terminated notify" indication from the network.
     * The presence service will not receive any more notifications for the request after this
     * indication is received.
     *
     * @param reqId the request ID which is returned by requestCapability or
     * requestAvailability
     */
    void onFinish(int reqId);

    /**
     * Called when there is a timeout waiting for the "terminated notify" indication from the
     * network.
     *
     * @param reqId the request ID which is returned by requestCapability or
     * requestAvailability.
     */
    void onTimeout(int reqId);

    /**
     * Called when there is an update to the capabilities from the network. On error, the
     * capabilities will also be updates as not capable.
     */
    void onCapabilitiesUpdated(int reqId, List<RcsContactUceCapability> contactCapabilities,
            boolean updateLastTimestamp);
}
