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


package com.android.internal.telephony;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothMapClient;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.net.Uri;
import android.telecom.PhoneAccount;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.util.Log;

import java.util.Collection;
import java.util.Collections;


/**
 * BtSmsInterfaceManager to provide a mechanism for sending SMS over Bluetooth
 */
public class BtSmsInterfaceManager {

    private static final String LOG_TAG = "BtSmsInterfaceManager";

    /**
     * Sends text through connected Bluetooth device
     */
    public void sendText(Context context, String destAddr, String text, PendingIntent sentIntent,
            PendingIntent deliveryIntent, SubscriptionInfo info) {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            // No bluetooth service on this platform?
            sendErrorInPendingIntent(sentIntent, SmsManager.RESULT_NO_BLUETOOTH_SERVICE);
            return;
        }
        BluetoothDevice device = btAdapter.getRemoteDevice(info.getIccId());
        if (device == null) {
            Log.d(LOG_TAG, "Bluetooth device addr invalid: " + Rlog.pii(LOG_TAG, info.getIccId()));
            sendErrorInPendingIntent(sentIntent, SmsManager.RESULT_INVALID_BLUETOOTH_ADDRESS);
            return;
        }
        if (btAdapter.getProfileProxy(context.getApplicationContext(),
                new MapMessageSender(destAddr, text, device, sentIntent, deliveryIntent),
                BluetoothProfile.MAP_CLIENT)) {
            return;
        }
        throw new RuntimeException("Can't send message through BluetoothMapClient");
    }

    private void sendErrorInPendingIntent(PendingIntent intent, int errorCode) {
        if (intent == null) {
            return;
        }
        try {
            intent.send(errorCode);
        } catch (PendingIntent.CanceledException e) {
            // PendingIntent is cancelled. ignore sending this error code back to
            // caller.
            Log.d(LOG_TAG, "PendingIntent.CanceledException: " + e.getMessage());
        }
    }

    private class MapMessageSender implements BluetoothProfile.ServiceListener {

        final Collection<Uri> mDestAddr;
        private String mMessage;
        final BluetoothDevice mDevice;
        final PendingIntent mSentIntent;
        final PendingIntent mDeliveryIntent;

        MapMessageSender(final String destAddr, final String message, final BluetoothDevice device,
                final PendingIntent sentIntent, final PendingIntent deliveryIntent) {
            super();
            mDestAddr = Collections.singleton(new Uri.Builder()
                    .appendPath(destAddr)
                    .scheme(PhoneAccount.SCHEME_TEL)
                    .build());
            mMessage = message;
            mDevice = device;
            mSentIntent = sentIntent;
            mDeliveryIntent = deliveryIntent;
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(LOG_TAG, "Service connected");
            if (profile != BluetoothProfile.MAP_CLIENT) {
                return;
            }
            BluetoothMapClient mapProfile = (BluetoothMapClient) proxy;
            if (mMessage != null) {
                Log.d(LOG_TAG, "Sending message thru bluetooth");
                mapProfile.sendMessage(mDevice, mDestAddr, mMessage, mSentIntent, mDeliveryIntent);
                mMessage = null;
            }
            BluetoothAdapter.getDefaultAdapter()
                    .closeProfileProxy(BluetoothProfile.MAP_CLIENT, mapProfile);
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (mMessage != null) {
                Log.d(LOG_TAG, "Bluetooth disconnected before sending the message");
                sendErrorInPendingIntent(mSentIntent, SmsManager.RESULT_BLUETOOTH_DISCONNECTED);
                mMessage = null;
            }
        }
    }
}
