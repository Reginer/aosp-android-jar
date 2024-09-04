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

import android.annotation.NonNull;
import android.telephony.ims.RtpHeaderExtension;
import android.telephony.ims.RtpHeaderExtensionType;

import java.util.Set;

/**
 * Abstracts out details of dealing with the RTP header extensions from the {@link RtpTransport} to
 * facilitate easier testing.
 */
public interface RtpAdapter {
    public interface Callback {
        /**
         * Used to indicate when RTP header extensions are received.
         * @param extensions The extensions.
         */
        void onRtpHeaderExtensionsReceived(Set<RtpHeaderExtension> extensions);
    }

    /**
     * Used to retrieve the accepted RTP header extensions by the {@link RtpTransport}.
     * @return the accepted RTP header extensions.
     */
    Set<RtpHeaderExtensionType> getAcceptedRtpHeaderExtensions();

    /**
     * Used by the {@link RtpTransport} to send RTP header extension messages.
     * @param rtpHeaderExtensions the rtp header extension messages to send.
     */
    void sendRtpHeaderExtensions(@NonNull Set<RtpHeaderExtension> rtpHeaderExtensions);
}
