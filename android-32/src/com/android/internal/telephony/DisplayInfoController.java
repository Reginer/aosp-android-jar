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

package com.android.internal.telephony;

import android.os.Handler;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.TelephonyDisplayInfo;

import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * The DisplayInfoController updates and broadcasts all changes to {@link TelephonyDisplayInfo}.
 * It manages all the information necessary for display purposes. Clients can register for display
 * info changes via {@link #registerForTelephonyDisplayInfoChanged} and obtain the current
 * TelephonyDisplayInfo via {@link #getTelephonyDisplayInfo}.
 */
public class DisplayInfoController extends Handler {
    private static final String TAG = "DisplayInfoController";
    private final Phone mPhone;
    private final NetworkTypeController mNetworkTypeController;
    private final RegistrantList mTelephonyDisplayInfoChangedRegistrants = new RegistrantList();
    private TelephonyDisplayInfo mTelephonyDisplayInfo;

    public DisplayInfoController(Phone phone) {
        mPhone = phone;
        mNetworkTypeController = new NetworkTypeController(phone, this);
        mNetworkTypeController.sendMessage(NetworkTypeController.EVENT_UPDATE);
    }

    /**
     * @return the current TelephonyDisplayInfo
     */
    public TelephonyDisplayInfo getTelephonyDisplayInfo() {
        return mTelephonyDisplayInfo;
    }

    /**
     * Update TelephonyDisplayInfo based on network type and override network type, received from
     * NetworkTypeController.
     */
    public void updateTelephonyDisplayInfo() {
        TelephonyDisplayInfo newDisplayInfo = new TelephonyDisplayInfo(
                mPhone.getServiceState().getDataNetworkType(),
                mNetworkTypeController.getOverrideNetworkType());
        if (!newDisplayInfo.equals(mTelephonyDisplayInfo)) {
            Rlog.d(TAG, "TelephonyDisplayInfo[" + mPhone.getPhoneId() + "] changed from "
                    + mTelephonyDisplayInfo + " to " + newDisplayInfo);
            mTelephonyDisplayInfo = newDisplayInfo;
            mTelephonyDisplayInfoChangedRegistrants.notifyRegistrants();
            mPhone.notifyDisplayInfoChanged(mTelephonyDisplayInfo);
        }
    }

    /**
     * @return True if either the primary or secondary 5G hysteresis timer is active,
     * and false if neither are.
     */
    public boolean is5GHysteresisActive() {
        return mNetworkTypeController.is5GHysteresisActive();
    }

    /**
     * Register for TelephonyDisplayInfo changed.
     * @param h Handler to notify
     * @param what msg.what when the message is delivered
     * @param obj msg.obj when the message is delivered
     */
    public void registerForTelephonyDisplayInfoChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mTelephonyDisplayInfoChangedRegistrants.add(r);
    }

    /**
     * Unregister for TelephonyDisplayInfo changed.
     * @param h Handler to notify
     */
    public void unregisterForTelephonyDisplayInfoChanged(Handler h) {
        mTelephonyDisplayInfoChangedRegistrants.remove(h);
    }

    /**
     * Dump the current state.
     */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("DisplayInfoController:");
        pw.println(" mPhone=" + mPhone.getPhoneName());
        pw.println(" mTelephonyDisplayInfo=" + mTelephonyDisplayInfo.toString());
        pw.flush();
        pw.println(" ***************************************");
        mNetworkTypeController.dump(fd, pw, args);
        pw.flush();
    }
}
