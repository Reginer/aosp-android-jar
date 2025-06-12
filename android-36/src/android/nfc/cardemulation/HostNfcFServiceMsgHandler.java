/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.nfc.cardemulation;

import static android.nfc.cardemulation.HostApduService.KEY_DATA;
import static android.nfc.cardemulation.HostApduService.MSG_DEACTIVATED;
import static android.nfc.cardemulation.HostNfcFService.MSG_COMMAND_PACKET;
import static android.nfc.cardemulation.HostNfcFService.MSG_RESPONSE_PACKET;

import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

/**
 * @hide
 */
public class HostNfcFServiceMsgHandler extends Handler {
    /**
     * @hide
     */
    private static final String TAG = "HostNfcFServMsgHandler";
    /**
     * @hide
     */
    private final HostNfcFService mHostNfcFService;
    /**
     * @hide
     */
    public HostNfcFServiceMsgHandler(HostNfcFService hostNfcFService) {
        this.mHostNfcFService = hostNfcFService;
    }

    @Override
    @UnsupportedAppUsage
    public void handleMessage(@Nullable Message msg) {
        switch (msg.what) {
            case MSG_COMMAND_PACKET:
                Bundle dataBundle = msg.getData();
                if (dataBundle == null) {
                    return;
                }
                if (mHostNfcFService.getNfcService() == null) {
                    mHostNfcFService.setNfcService(
                            msg.replyTo);
                }

                byte[] packet = dataBundle.getByteArray(KEY_DATA);
                if (packet != null) {
                    byte[] responsePacket = mHostNfcFService.processNfcFPacket(packet, null);
                    if (responsePacket != null) {
                        if (mHostNfcFService.getNfcService() == null) {
                            Log.e(TAG, "Response not sent; service was deactivated.");
                            return;
                        }
                        Message responseMsg = Message.obtain(null, MSG_RESPONSE_PACKET);
                        Bundle responseBundle = new Bundle();
                        responseBundle.putByteArray(KEY_DATA, responsePacket);
                        responseMsg.setData(responseBundle);
                        responseMsg.replyTo = mHostNfcFService.getMessenger();
                        try {
                            mHostNfcFService.getNfcService().send(responseMsg);
                        } catch (RemoteException e) {
                            Log.e("TAG", "Response not sent; RemoteException calling into " +
                                    "NfcService.");
                        }
                    }
                } else {
                    Log.e(TAG, "Received MSG_COMMAND_PACKET without data.");
                }
                break;
            case MSG_RESPONSE_PACKET:
                if (mHostNfcFService.getNfcService() == null) {
                    Log.e(TAG, "Response not sent; service was deactivated.");
                    return;
                }
                try {
                    msg.replyTo = mHostNfcFService.getMessenger();
                    mHostNfcFService.getNfcService().send(msg);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException calling into NfcService.");
                }
                break;
            case MSG_DEACTIVATED:
                // Make sure we won't call into NfcService again
                mHostNfcFService.setNfcService(null);
                mHostNfcFService.onDeactivated(msg.arg1);
                break;
            default:
                super.handleMessage(msg);
        }
    }
}
