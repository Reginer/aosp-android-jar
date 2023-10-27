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

package com.android.internal.net.ipsec.ike.ike3gpp;

import static android.net.ipsec.ike.IkeManager.getIkeLog;

import static com.android.internal.net.ipsec.ike.ike3gpp.Ike3gppExtensionExchange.NOTIFY_TYPE_DEVICE_IDENTITY;

import android.annotation.NonNull;
import android.net.ipsec.ike.ike3gpp.Ike3gppExtension;

import com.android.internal.net.ipsec.ike.message.IkeNotifyPayload;
import com.android.internal.net.ipsec.ike.message.IkePayload;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class Ike3gppIkeInfo extends Ike3gppExchangeBase {
    private static final String TAG = Ike3gppIkeInfo.class.getSimpleName();

    /** Initializes an Ike3gppIkeInfo. */
    Ike3gppIkeInfo(@NonNull Ike3gppExtension ike3gppExtension, @NonNull Executor userCbExecutor) {
        super(ike3gppExtension, userCbExecutor);
    }

    /**
     * Get the list of payloads to send out in an IKE Informational response based on incoming
     * payloads in the incoming IKE Informational request.
     */
    List<IkePayload> getResponsePayloads(List<IkePayload> ike3gppRequestPayloads) {
        List<IkePayload> ike3gppPayloads = new ArrayList<>();
        List<IkeNotifyPayload> notifyPayloads =
                IkePayload.getPayloadListForTypeInProvidedList(
                        IkePayload.PAYLOAD_TYPE_NOTIFY,
                        IkeNotifyPayload.class,
                        ike3gppRequestPayloads);
        for (IkeNotifyPayload notifyPayload : notifyPayloads) {
            switch (notifyPayload.notifyType) {
                case NOTIFY_TYPE_DEVICE_IDENTITY:
                    String deviceIdentity =
                            mIke3gppExtension.getIke3gppParams().getMobileDeviceIdentity();
                    if (deviceIdentity != null) {
                        ike3gppPayloads.add(
                                Ike3gppDeviceIdentityUtils.generateDeviceIdentityPayload(
                                        deviceIdentity));
                    }
                default:
                    // Can be ignored.
                    logd("Payload ignored in Ike3gppIkeInfo" + notifyPayload.getTypeString());
                    break;
            }
        }
        return ike3gppPayloads;
    }

    private void logd(String msg) {
        getIkeLog().d(TAG, msg);
    }
}
