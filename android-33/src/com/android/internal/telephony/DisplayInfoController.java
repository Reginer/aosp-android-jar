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

import android.annotation.NonNull;
import android.os.Handler;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.AccessNetworkConstants;
import android.telephony.AnomalyReporter;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.util.Pair;

import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Set;
import java.util.UUID;

import javax.sip.InvalidArgumentException;

/**
 * The DisplayInfoController updates and broadcasts all changes to {@link TelephonyDisplayInfo}.
 * It manages all the information necessary for display purposes. Clients can register for display
 * info changes via {@link #registerForTelephonyDisplayInfoChanged} and obtain the current
 * TelephonyDisplayInfo via {@link #getTelephonyDisplayInfo}.
 */
public class DisplayInfoController extends Handler {
    private static final String TAG = "DisplayInfoController";

    private final String mLogTag;
    private final LocalLog mLocalLog = new LocalLog(128);

    private static final Set<Pair<Integer, Integer>> VALID_DISPLAY_INFO_SET = Set.of(
            // LTE
            Pair.create(TelephonyManager.NETWORK_TYPE_LTE,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA),
            Pair.create(TelephonyManager.NETWORK_TYPE_LTE,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO),
            Pair.create(TelephonyManager.NETWORK_TYPE_LTE,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA),
            Pair.create(TelephonyManager.NETWORK_TYPE_LTE,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED),

            // NR
            Pair.create(TelephonyManager.NETWORK_TYPE_NR,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED)
            );

    private final Phone mPhone;
    private final NetworkTypeController mNetworkTypeController;
    private final RegistrantList mTelephonyDisplayInfoChangedRegistrants = new RegistrantList();
    private TelephonyDisplayInfo mTelephonyDisplayInfo;

    public DisplayInfoController(Phone phone) {
        mPhone = phone;
        mLogTag = "DIC-" + mPhone.getPhoneId();
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
        NetworkRegistrationInfo nri =  mPhone.getServiceState().getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        int dataNetworkType = nri == null ? TelephonyManager.NETWORK_TYPE_UNKNOWN
                : nri.getAccessNetworkTechnology();
        TelephonyDisplayInfo newDisplayInfo = new TelephonyDisplayInfo(dataNetworkType,
                mNetworkTypeController.getOverrideNetworkType());
        if (!newDisplayInfo.equals(mTelephonyDisplayInfo)) {
            logl("TelephonyDisplayInfo changed from " + mTelephonyDisplayInfo + " to "
                    + newDisplayInfo);
            validateDisplayInfo(newDisplayInfo);
            mTelephonyDisplayInfo = newDisplayInfo;
            mTelephonyDisplayInfoChangedRegistrants.notifyRegistrants();
            mPhone.notifyDisplayInfoChanged(mTelephonyDisplayInfo);
        }
    }

    /**
     * Validate the display info and trigger anomaly report if needed.
     *
     * @param displayInfo The display info to validate.
     */
    private void validateDisplayInfo(@NonNull TelephonyDisplayInfo displayInfo) {
        try {
            if (displayInfo.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE_CA) {
                throw new InvalidArgumentException("LTE_CA is not a valid network type.");
            }
            if (displayInfo.getNetworkType() < TelephonyManager.NETWORK_TYPE_UNKNOWN
                    && displayInfo.getNetworkType() > TelephonyManager.NETWORK_TYPE_NR) {
                throw new InvalidArgumentException("Invalid network type "
                        + displayInfo.getNetworkType());
            }
            if (displayInfo.getOverrideNetworkType()
                    != TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE
                    && !VALID_DISPLAY_INFO_SET.contains(Pair.create(displayInfo.getNetworkType(),
                    displayInfo.getOverrideNetworkType()))) {
                throw new InvalidArgumentException("Invalid network type override "
                        + TelephonyDisplayInfo.overrideNetworkTypeToString(
                                displayInfo.getOverrideNetworkType())
                        + " for " + TelephonyManager.getNetworkTypeName(
                                displayInfo.getNetworkType()));
            }
        } catch (InvalidArgumentException e) {
            logel(e.getMessage());
            AnomalyReporter.reportAnomaly(UUID.fromString("3aa92a2c-94ed-46a0-a744-d6b1dfec2a55"),
                    e.getMessage(), mPhone.getCarrierId());
        }
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
     * Log debug messages.
     * @param s debug messages
     */
    private void log(@NonNull String s) {
        Rlog.d(mLogTag, s);
    }

    /**
     * Log error messages.
     * @param s error messages
     */
    private void loge(@NonNull String s) {
        Rlog.e(mLogTag, s);
    }

    /**
     * Log debug messages and also log into the local log.
     * @param s debug messages
     */
    private void logl(@NonNull String s) {
        log(s);
        mLocalLog.log(s);
    }

    /**
     * Log error messages and also log into the local log.
     * @param s debug messages
     */
    private void logel(@NonNull String s) {
        loge(s);
        mLocalLog.log(s);
    }

    /**
     * Dump the current state.
     */
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println("DisplayInfoController:");
        pw.println(" mPhone=" + mPhone.getPhoneName());
        pw.println(" mTelephonyDisplayInfo=" + mTelephonyDisplayInfo.toString());
        pw.flush();
        pw.println("Local logs:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.println(" ***************************************");
        mNetworkTypeController.dump(fd, pw, args);
        pw.flush();
    }
}
