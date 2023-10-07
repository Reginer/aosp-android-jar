/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.text.TextUtils;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;


/**
 * {@hide}
 */
public abstract class CallTracker extends Handler {

    private static final boolean DBG_POLL = false;

    //***** Constants

    static final int POLL_DELAY_MSEC = 250;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected int mPendingOperations;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected boolean mNeedsPoll;
    protected Message mLastRelevantPoll;
    protected ArrayList<Connection> mHandoverConnections = new ArrayList<Connection>();

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public CommandsInterface mCi;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected boolean mNumberConverted = false;
    private final int VALID_COMPARE_LENGTH   = 3;

    //***** Events

    protected static final int EVENT_POLL_CALLS_RESULT             = 1;
    protected static final int EVENT_CALL_STATE_CHANGE             = 2;
    protected static final int EVENT_REPOLL_AFTER_DELAY            = 3;
    protected static final int EVENT_OPERATION_COMPLETE            = 4;
    protected static final int EVENT_GET_LAST_CALL_FAIL_CAUSE      = 5;

    protected static final int EVENT_SWITCH_RESULT                 = 8;
    protected static final int EVENT_RADIO_AVAILABLE               = 9;
    protected static final int EVENT_RADIO_NOT_AVAILABLE           = 10;
    protected static final int EVENT_CONFERENCE_RESULT             = 11;
    protected static final int EVENT_SEPARATE_RESULT               = 12;
    protected static final int EVENT_ECT_RESULT                    = 13;
    protected static final int EVENT_EXIT_ECM_RESPONSE_CDMA        = 14;
    protected static final int EVENT_CALL_WAITING_INFO_CDMA        = 15;
    protected static final int EVENT_THREE_WAY_DIAL_L2_RESULT_CDMA = 16;
    protected static final int EVENT_THREE_WAY_DIAL_BLANK_FLASH    = 20;

    @UnsupportedAppUsage
    public CallTracker() {
    }

    protected void pollCallsWhenSafe() {
        mNeedsPoll = true;

        if (checkNoOperationsPending()) {
            mLastRelevantPoll = obtainMessage(EVENT_POLL_CALLS_RESULT);
            mCi.getCurrentCalls(mLastRelevantPoll);
        }
    }

    protected void
    pollCallsAfterDelay() {
        Message msg = obtainMessage();

        msg.what = EVENT_REPOLL_AFTER_DELAY;
        sendMessageDelayed(msg, POLL_DELAY_MSEC);
    }

    protected boolean
    isCommandExceptionRadioNotAvailable(Throwable e) {
        return e != null && e instanceof CommandException
                && ((CommandException)e).getCommandError()
                        == CommandException.Error.RADIO_NOT_AVAILABLE;
    }

    protected abstract void handlePollCalls(AsyncResult ar);

    protected abstract Phone getPhone();

    protected Connection getHoConnection(DriverCall dc) {
        for (Connection hoConn : mHandoverConnections) {
            log("getHoConnection - compare number: hoConn= " + hoConn.toString());
            if (hoConn.getAddress() != null && hoConn.getAddress().contains(dc.number)) {
                log("getHoConnection: Handover connection match found = " + hoConn.toString());
                return hoConn;
            }
        }
        for (Connection hoConn : mHandoverConnections) {
            log("getHoConnection: compare state hoConn= " + hoConn.toString());
            if (hoConn.getStateBeforeHandover() == Call.stateFromDCState(dc.state)) {
                log("getHoConnection: Handover connection match found = " + hoConn.toString());
                return hoConn;
            }
        }
        return null;
    }

    protected void notifySrvccState(Call.SrvccState state, ArrayList<Connection> c) {
        if (state == Call.SrvccState.STARTED && c != null) {
            // SRVCC started. Prepare handover connections list
            mHandoverConnections.addAll(c);
        } else if (state != Call.SrvccState.COMPLETED) {
            // SRVCC FAILED/CANCELED. Clear the handover connections list
            // Individual connections will be removed from the list in handlePollCalls()
            mHandoverConnections.clear();
        }
        log("notifySrvccState: state=" + state.name() + ", mHandoverConnections= "
                + mHandoverConnections.toString());
    }

    protected void handleRadioAvailable() {
        pollCallsWhenSafe();
    }

    /**
     * Obtain a complete message that indicates that this operation
     * does not require polling of getCurrentCalls(). However, if other
     * operations that do need getCurrentCalls() are pending or are
     * scheduled while this operation is pending, the invocation
     * of getCurrentCalls() will be postponed until this
     * operation is also complete.
     */
    protected Message
    obtainNoPollCompleteMessage(int what) {
        mPendingOperations++;
        mLastRelevantPoll = null;
        return obtainMessage(what);
    }

    /**
     * @return true if we're idle or there's a call to getCurrentCalls() pending
     * but nothing else
     */
    private boolean
    checkNoOperationsPending() {
        if (DBG_POLL) log("checkNoOperationsPending: pendingOperations=" +
                mPendingOperations);
        return mPendingOperations == 0;
    }

    protected String convertNumberIfNecessary(Phone phone, String dialNumber) {
        if (dialNumber == null) {
            return dialNumber;
        }
        String[] convertMaps = null;
        CarrierConfigManager configManager = (CarrierConfigManager)
                phone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle bundle = configManager.getConfigForSubId(phone.getSubId());
        if (bundle != null) {
            convertMaps = (shouldPerformInternationalNumberRemapping(phone, bundle))
                    ? bundle.getStringArray(CarrierConfigManager
                            .KEY_INTERNATIONAL_ROAMING_DIAL_STRING_REPLACE_STRING_ARRAY)
                    : bundle.getStringArray(CarrierConfigManager
                            .KEY_DIAL_STRING_REPLACE_STRING_ARRAY);
        }
        if (convertMaps == null) {
            // By default no replacement is necessary
            log("convertNumberIfNecessary convertMaps is null");
            return dialNumber;
        }

        log("convertNumberIfNecessary Roaming"
            + " convertMaps.length " + convertMaps.length
            + " dialNumber.length() " + dialNumber.length());

        if (convertMaps.length < 1 || dialNumber.length() < VALID_COMPARE_LENGTH) {
            return dialNumber;
        }

        String[] entry;
        String outNumber = "";
        for(String convertMap : convertMaps) {
            log("convertNumberIfNecessary: " + convertMap);
            // entry format is  "dialStringToReplace:dialStringReplacement"
            entry = convertMap.split(":");
            if (entry != null && entry.length > 1) {
                String dsToReplace = entry[0];
                String dsReplacement = entry[1];
                if (!TextUtils.isEmpty(dsToReplace) && dialNumber.equals(dsToReplace)) {
                    // Needs to be converted
                    if (!TextUtils.isEmpty(dsReplacement) && dsReplacement.endsWith("MDN")) {
                        String mdn = phone.getLine1Number();
                        if (!TextUtils.isEmpty(mdn)) {
                            if (mdn.startsWith("+")) {
                                outNumber = mdn;
                            } else {
                                outNumber = dsReplacement.substring(0, dsReplacement.length() -3)
                                        + mdn;
                            }
                        }
                    } else {
                        outNumber = dsReplacement;
                    }
                    break;
                }
            }
        }

        if (!TextUtils.isEmpty(outNumber)) {
            log("convertNumberIfNecessary: convert service number");
            mNumberConverted = true;
            return outNumber;
        }

        return dialNumber;

    }

    /**
     * Helper function to determine if the phones service is in ROAMING_TYPE_INTERNATIONAL.
     * @param phone object that contains the service state.
     * @param bundle object that contains the bundle with mapped dial strings.
     * @return  true if the phone is in roaming state with a set bundle. Otherwise, false.
     */
    private boolean shouldPerformInternationalNumberRemapping(Phone phone,
            PersistableBundle bundle) {
        if (phone == null || phone.getDefaultPhone() == null) {
            log("shouldPerformInternationalNumberRemapping: phone was null");
            return false;
        }

        if (bundle.getStringArray(CarrierConfigManager
                .KEY_INTERNATIONAL_ROAMING_DIAL_STRING_REPLACE_STRING_ARRAY) == null) {
            log("shouldPerformInternationalNumberRemapping: did not set the "
                    + "KEY_INTERNATIONAL_ROAMING_DIAL_STRING_REPLACE_STRING_ARRAY");
            return false;
        }

        return phone.getDefaultPhone().getServiceState().getVoiceRoamingType()
                == ServiceState.ROAMING_TYPE_INTERNATIONAL;
    }

    private boolean compareGid1(Phone phone, String serviceGid1) {
        String gid1 = phone.getGroupIdLevel1();
        int gid_length = serviceGid1.length();
        boolean ret = true;

        if (serviceGid1 == null || serviceGid1.equals("")) {
            log("compareGid1 serviceGid is empty, return " + ret);
            return ret;
        }
        // Check if gid1 match service GID1
        if (!((gid1 != null) && (gid1.length() >= gid_length) &&
                gid1.substring(0, gid_length).equalsIgnoreCase(serviceGid1))) {
            log(" gid1 " + gid1 + " serviceGid1 " + serviceGid1);
            ret = false;
        }
        log("compareGid1 is " + (ret?"Same":"Different"));
        return ret;
    }

    /**
     * Get the ringing connections which during SRVCC handover.
     */
    public Connection getRingingHandoverConnection() {
        for (Connection hoConn : mHandoverConnections) {
            if (hoConn.getCall().isRinging()) {
                return hoConn;
            }
        }
        return null;
    }

    //***** Overridden from Handler
    @Override
    public abstract void handleMessage (Message msg);
    public abstract void registerForVoiceCallStarted(Handler h, int what, Object obj);
    public abstract void unregisterForVoiceCallStarted(Handler h);
    @UnsupportedAppUsage
    public abstract void registerForVoiceCallEnded(Handler h, int what, Object obj);
    public abstract void unregisterForVoiceCallEnded(Handler h);
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public abstract PhoneConstants.State getState();
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected abstract void log(String msg);

    /**
     * Called when the call tracker should attempt to reconcile its calls against its underlying
     * phone implementation and cleanup any stale calls.
     */
    public void cleanupCalls() {
        // no base implementation
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("CallTracker:");
        pw.println(" mPendingOperations=" + mPendingOperations);
        pw.println(" mNeedsPoll=" + mNeedsPoll);
        pw.println(" mLastRelevantPoll=" + mLastRelevantPoll);
    }
}
