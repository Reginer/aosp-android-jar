/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.internal.telephony.uicc;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.R;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;

/**
 * This class launches its logic on Uicc cards state changes to / from a
 * {@link #CARDSTATE_RESTRICTED} to notify a device provisioning package {@link
 * com.android.internal.R.string.config_deviceProvisioningPackage}, which manages user notifications
 * that inserted SIM is not supported on the device.
 *
 * @see #CARDSTATE_RESTRICTED
 *
 * {@hide}
 */
public class UiccStateChangedLauncher extends Handler {
    private static final String TAG = UiccStateChangedLauncher.class.getName();
    private static final int EVENT_ICC_CHANGED = 1;

    private static String sDeviceProvisioningPackage = null;
    private Context mContext;
    private UiccController mUiccController;
    private boolean[] mIsRestricted = null;

    public UiccStateChangedLauncher(Context context, UiccController controller) {
        sDeviceProvisioningPackage = context.getResources().getString(
                R.string.config_deviceProvisioningPackage);
        if (sDeviceProvisioningPackage != null && !sDeviceProvisioningPackage.isEmpty()) {
            mContext = context;
            mUiccController = controller;
            mUiccController.registerForIccChanged(this, EVENT_ICC_CHANGED, null);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch(msg.what) {
            case (EVENT_ICC_CHANGED):
                boolean shouldNotify = false;
                if (mIsRestricted == null) {
                    mIsRestricted = new boolean[TelephonyManager.getDefault().getPhoneCount()];
                    shouldNotify = true;
                }
                for (int i = 0; i < mIsRestricted.length; ++i) {
                    // Update only if restricted state changes.

                    UiccCard uiccCard = mUiccController.getUiccCardForPhone(i);
                    if ((uiccCard == null
                            || uiccCard.getCardState() != CardState.CARDSTATE_RESTRICTED)
                            != mIsRestricted[i]) {
                        mIsRestricted[i] = !mIsRestricted[i];
                        shouldNotify = true;
                    }
                }
                if (shouldNotify) {
                    notifyStateChanged();
                }
                break;
            default:
                throw new RuntimeException("unexpected event not handled");
        }
    }

    /**
     * Send an explicit intent to device provisioning package.
     */
    private void notifyStateChanged() {
        Intent intent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        intent.setPackage(sDeviceProvisioningPackage);
        try {
            mContext.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
}
