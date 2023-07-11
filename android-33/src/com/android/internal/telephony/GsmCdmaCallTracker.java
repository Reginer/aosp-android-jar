/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.sysprop.TelephonyProperties;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.DisconnectCause;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState.RilRadioTechnology;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.PhoneInternalInterface.DialArgs;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * {@hide}
 */
public class GsmCdmaCallTracker extends CallTracker {
    private static final String LOG_TAG = "GsmCdmaCallTracker";
    private static final boolean REPEAT_POLLING = false;

    private static final boolean DBG_POLL = false;
    private static final boolean VDBG = false;

    //***** Constants

    public static final int MAX_CONNECTIONS_GSM = 19;   //7 allowed in GSM + 12 from IMS for SRVCC
    private static final int MAX_CONNECTIONS_PER_CALL_GSM = 5; //only 5 connections allowed per call

    private static final int MAX_CONNECTIONS_CDMA = 8;
    private static final int MAX_CONNECTIONS_PER_CALL_CDMA = 1; //only 1 connection allowed per call

    //***** Instance Variables
    @VisibleForTesting
    public GsmCdmaConnection[] mConnections;
    private RegistrantList mVoiceCallEndedRegistrants = new RegistrantList();
    private RegistrantList mVoiceCallStartedRegistrants = new RegistrantList();

    // connections dropped during last poll
    private ArrayList<GsmCdmaConnection> mDroppedDuringPoll =
            new ArrayList<GsmCdmaConnection>(MAX_CONNECTIONS_GSM);

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public GsmCdmaCall mRingingCall = new GsmCdmaCall(this);
    // A call that is ringing or (call) waiting
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public GsmCdmaCall mForegroundCall = new GsmCdmaCall(this);
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public GsmCdmaCall mBackgroundCall = new GsmCdmaCall(this);

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private GsmCdmaConnection mPendingMO;
    private boolean mHangupPendingMO;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private GsmCdmaPhone mPhone;

    private boolean mDesiredMute = false;    // false = mute off

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public PhoneConstants.State mState = PhoneConstants.State.IDLE;

    private TelephonyMetrics mMetrics = TelephonyMetrics.getInstance();

    // Following member variables are for CDMA only
    private RegistrantList mCallWaitingRegistrants = new RegistrantList();
    private boolean mPendingCallInEcm;
    private boolean mIsInEmergencyCall;
    private int mPendingCallClirMode;
    private int m3WayCallFlashDelay;

    /**
     * Listens for Emergency Callback Mode state change intents
     */
    private BroadcastReceiver mEcmExitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED)) {

                boolean isInEcm = intent.getBooleanExtra(
                        TelephonyManager.EXTRA_PHONE_IN_ECM_STATE, false);
                log("Received ACTION_EMERGENCY_CALLBACK_MODE_CHANGED isInEcm = " + isInEcm);

                // If we exit ECM mode, notify all connections.
                if (!isInEcm) {
                    // Although mConnections seems to be the place to look, it is not guaranteed
                    // to have all of the connections we're tracking.  THe best place to look is in
                    // the Call objects associated with the tracker.
                    List<Connection> toNotify = new ArrayList<Connection>();
                    toNotify.addAll(mRingingCall.getConnections());
                    toNotify.addAll(mForegroundCall.getConnections());
                    toNotify.addAll(mBackgroundCall.getConnections());
                    if (mPendingMO != null) {
                        toNotify.add(mPendingMO);
                    }

                    // Notify connections that ECM mode exited.
                    for (Connection connection : toNotify) {
                        if (connection != null) {
                            connection.onExitedEcmMode();
                        }
                    }
                }
            }
        }
    };

    //***** Events


    //***** Constructors

    public GsmCdmaCallTracker (GsmCdmaPhone phone) {
        this.mPhone = phone;
        mCi = phone.mCi;
        mCi.registerForCallStateChanged(this, EVENT_CALL_STATE_CHANGE, null);
        mCi.registerForOn(this, EVENT_RADIO_AVAILABLE, null);
        mCi.registerForNotAvailable(this, EVENT_RADIO_NOT_AVAILABLE, null);

        // Register receiver for ECM exit
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        mPhone.getContext().registerReceiver(mEcmExitReceiver, filter);

        updatePhoneType(true);
    }

    public void updatePhoneType() {
        updatePhoneType(false);
    }

    private void updatePhoneType(boolean duringInit) {
        if (!duringInit) {
            reset();
            pollCallsWhenSafe();
        }
        if (mPhone.isPhoneTypeGsm()) {
            mConnections = new GsmCdmaConnection[MAX_CONNECTIONS_GSM];
            mCi.unregisterForCallWaitingInfo(this);
            // Prior to phone switch to GSM, if CDMA has any emergency call
            // data will be in disabled state, after switching to GSM enable data.
            if (mIsInEmergencyCall) {
                if (!mPhone.isUsingNewDataStack()) {
                    mPhone.getDataEnabledSettings().setInternalDataEnabled(true);
                }
            }
        } else {
            mConnections = new GsmCdmaConnection[MAX_CONNECTIONS_CDMA];
            mPendingCallInEcm = false;
            mIsInEmergencyCall = false;
            mPendingCallClirMode = CommandsInterface.CLIR_DEFAULT;
            mPhone.setEcmCanceledForEmergency(false /*isCanceled*/);
            m3WayCallFlashDelay = 0;
            mCi.registerForCallWaitingInfo(this, EVENT_CALL_WAITING_INFO_CDMA, null);
        }
    }

    private void reset() {
        Rlog.d(LOG_TAG, "reset");

        for (GsmCdmaConnection gsmCdmaConnection : mConnections) {
            if (gsmCdmaConnection != null) {
                gsmCdmaConnection.onDisconnect(DisconnectCause.ERROR_UNSPECIFIED);
                gsmCdmaConnection.dispose();
            }
        }

        if (mPendingMO != null) {
            // Send the notification that the pending call was disconnected to the higher layers.
            mPendingMO.onDisconnect(DisconnectCause.ERROR_UNSPECIFIED);
            mPendingMO.dispose();
        }

        mConnections = null;
        mPendingMO = null;
        clearDisconnected();
    }

    @Override
    protected void finalize() {
        Rlog.d(LOG_TAG, "GsmCdmaCallTracker finalized");
    }

    //***** Instance Methods

    //***** Public Methods
    @Override
    public void registerForVoiceCallStarted(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mVoiceCallStartedRegistrants.add(r);
        // Notify if in call when registering
        if (mState != PhoneConstants.State.IDLE) {
            r.notifyRegistrant(new AsyncResult(null, null, null));
        }
    }

    @Override
    public void unregisterForVoiceCallStarted(Handler h) {
        mVoiceCallStartedRegistrants.remove(h);
    }

    @Override
    public void registerForVoiceCallEnded(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mVoiceCallEndedRegistrants.add(r);
    }

    @Override
    public void unregisterForVoiceCallEnded(Handler h) {
        mVoiceCallEndedRegistrants.remove(h);
    }

    public void registerForCallWaiting(Handler h, int what, Object obj) {
        Registrant r = new Registrant (h, what, obj);
        mCallWaitingRegistrants.add(r);
    }

    public void unregisterForCallWaiting(Handler h) {
        mCallWaitingRegistrants.remove(h);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void fakeHoldForegroundBeforeDial() {
        // We need to make a copy here, since fakeHoldBeforeDial()
        // modifies the lists, and we don't want to reverse the order
        ArrayList<Connection> connCopy = mForegroundCall.getConnections();

        for (Connection conn : connCopy) {
            GsmCdmaConnection gsmCdmaConn = (GsmCdmaConnection) conn;
            gsmCdmaConn.fakeHoldBeforeDial();
        }
    }

    //GSM
    /**
     * clirMode is one of the CLIR_ constants
     */
    public synchronized Connection dialGsm(String dialString, DialArgs dialArgs)
            throws CallStateException {
        int clirMode = dialArgs.clirMode;
        UUSInfo uusInfo = dialArgs.uusInfo;
        Bundle intentExtras = dialArgs.intentExtras;
        boolean isEmergencyCall = dialArgs.isEmergency;
        if (isEmergencyCall) {
            clirMode = CommandsInterface.CLIR_SUPPRESSION;
            if (Phone.DEBUG_PHONE) log("dial gsm emergency call, set clirModIe=" + clirMode);

        }

        // note that this triggers call state changed notif
        clearDisconnected();

        // Check for issues which would preclude dialing and throw a CallStateException.
        checkForDialIssues(isEmergencyCall);

        String origNumber = dialString;
        dialString = convertNumberIfNecessary(mPhone, dialString);

        // The new call must be assigned to the foreground call.
        // That call must be idle, so place anything that's
        // there on hold
        if (mForegroundCall.getState() == GsmCdmaCall.State.ACTIVE) {
            // this will probably be done by the radio anyway
            // but the dial might fail before this happens
            // and we need to make sure the foreground call is clear
            // for the newly dialed connection
            switchWaitingOrHoldingAndActive();

            // This is a hack to delay DIAL so that it is sent out to RIL only after
            // EVENT_SWITCH_RESULT is received. We've seen failures when adding a new call to
            // multi-way conference calls due to DIAL being sent out before SWITCH is processed
            // TODO: setup duration metrics won't capture this
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // do nothing
            }

            // Fake local state so that
            // a) foregroundCall is empty for the newly dialed connection
            // b) hasNonHangupStateChanged remains false in the
            // next poll, so that we don't clear a failed dialing call
            fakeHoldForegroundBeforeDial();
        }

        if (mForegroundCall.getState() != GsmCdmaCall.State.IDLE) {
            //we should have failed in !canDial() above before we get here
            throw new CallStateException("cannot dial in current state");
        }

        mPendingMO = new GsmCdmaConnection(mPhone, dialString, this, mForegroundCall,
                dialArgs);

        if (intentExtras != null) {
            Rlog.d(LOG_TAG, "dialGsm - emergency dialer: " + intentExtras.getBoolean(
                    TelecomManager.EXTRA_IS_USER_INTENT_EMERGENCY_CALL));
            mPendingMO.setHasKnownUserIntentEmergency(intentExtras.getBoolean(
                    TelecomManager.EXTRA_IS_USER_INTENT_EMERGENCY_CALL));
        }
        mHangupPendingMO = false;

        mMetrics.writeRilDial(mPhone.getPhoneId(), mPendingMO, clirMode, uusInfo);
        mPhone.getVoiceCallSessionStats().onRilDial(mPendingMO);

        if ( mPendingMO.getAddress() == null || mPendingMO.getAddress().length() == 0
                || mPendingMO.getAddress().indexOf(PhoneNumberUtils.WILD) >= 0) {
            // Phone number is invalid
            mPendingMO.mCause = DisconnectCause.INVALID_NUMBER;

            // handlePollCalls() will notice this call not present
            // and will mark it as dropped.
            pollCallsWhenSafe();
        } else {

            // Always unmute when initiating a new call
            setMute(false);

            mCi.dial(mPendingMO.getAddress(), mPendingMO.isEmergencyCall(),
                    mPendingMO.getEmergencyNumberInfo(), mPendingMO.hasKnownUserIntentEmergency(),
                    clirMode, uusInfo, obtainCompleteMessage());
        }

        if (mNumberConverted) {
            mPendingMO.restoreDialedNumberAfterConversion(origNumber);
            mNumberConverted = false;
        }

        updatePhoneState();
        mPhone.notifyPreciseCallStateChanged();

        return mPendingMO;
    }

    //CDMA
    /**
     * Handle Ecm timer to be canceled or re-started
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void handleEcmTimer(int action) {
        mPhone.handleTimerInEmergencyCallbackMode(action);
    }

    //CDMA
    /**
     * Disable data call when emergency call is connected
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void disableDataCallInEmergencyCall(String dialString) {
        TelephonyManager tm =
                (TelephonyManager) mPhone.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.isEmergencyNumber(dialString)) {
            if (Phone.DEBUG_PHONE) log("disableDataCallInEmergencyCall");
            setIsInEmergencyCall();
        }
    }

    //CDMA
    public void setIsInEmergencyCall() {
        mIsInEmergencyCall = true;
        if (!mPhone.isUsingNewDataStack()) {
            mPhone.getDataEnabledSettings().setInternalDataEnabled(false);
        }
        mPhone.notifyEmergencyCallRegistrants(true);
        mPhone.sendEmergencyCallStateChange(true);
    }

    //CDMA
    /**
     * clirMode is one of the CLIR_ constants
     */
    private Connection dialCdma(String dialString, DialArgs dialArgs)
            throws CallStateException {
        int clirMode = dialArgs.clirMode;
        Bundle intentExtras = dialArgs.intentExtras;
        boolean isEmergencyCall = dialArgs.isEmergency;

        if (isEmergencyCall) {
            clirMode = CommandsInterface.CLIR_SUPPRESSION;
            if (Phone.DEBUG_PHONE) log("dial cdma emergency call, set clirModIe=" + clirMode);
        }

        // note that this triggers call state changed notif
        clearDisconnected();

        // Check for issues which would preclude dialing and throw a CallStateException.
        checkForDialIssues(isEmergencyCall);

        TelephonyManager tm =
                (TelephonyManager) mPhone.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String origNumber = dialString;
        String operatorIsoContry = tm.getNetworkCountryIso(mPhone.getPhoneId());
        String simIsoContry = tm.getSimCountryIsoForPhone(mPhone.getPhoneId());
        boolean internationalRoaming = !TextUtils.isEmpty(operatorIsoContry)
                && !TextUtils.isEmpty(simIsoContry)
                && !simIsoContry.equals(operatorIsoContry);
        if (internationalRoaming) {
            if ("us".equals(simIsoContry)) {
                internationalRoaming = internationalRoaming && !"vi".equals(operatorIsoContry);
            } else if ("vi".equals(simIsoContry)) {
                internationalRoaming = internationalRoaming && !"us".equals(operatorIsoContry);
            }
        }
        if (internationalRoaming) {
            dialString = convertNumberIfNecessary(mPhone, dialString);
        }

        boolean isPhoneInEcmMode = mPhone.isInEcm();

        // Cancel Ecm timer if a second emergency call is originating in Ecm mode
        if (isPhoneInEcmMode && isEmergencyCall) {
            mPhone.handleTimerInEmergencyCallbackMode(GsmCdmaPhone.CANCEL_ECM_TIMER);
        }

        // The new call must be assigned to the foreground call.
        // That call must be idle, so place anything that's
        // there on hold
        if (mForegroundCall.getState() == GsmCdmaCall.State.ACTIVE) {
            return dialThreeWay(dialString, dialArgs);
        }

        mPendingMO = new GsmCdmaConnection(mPhone, dialString, this, mForegroundCall,
                dialArgs);

        if (intentExtras != null) {
            Rlog.d(LOG_TAG, "dialGsm - emergency dialer: " + intentExtras.getBoolean(
                    TelecomManager.EXTRA_IS_USER_INTENT_EMERGENCY_CALL));
            mPendingMO.setHasKnownUserIntentEmergency(intentExtras.getBoolean(
                    TelecomManager.EXTRA_IS_USER_INTENT_EMERGENCY_CALL));
        }
        mHangupPendingMO = false;

        if ( mPendingMO.getAddress() == null || mPendingMO.getAddress().length() == 0
                || mPendingMO.getAddress().indexOf(PhoneNumberUtils.WILD) >= 0 ) {
            // Phone number is invalid
            mPendingMO.mCause = DisconnectCause.INVALID_NUMBER;

            // handlePollCalls() will notice this call not present
            // and will mark it as dropped.
            pollCallsWhenSafe();
        } else {
            // Always unmute when initiating a new call
            setMute(false);

            // Check data call
            disableDataCallInEmergencyCall(dialString);

            // In Ecm mode, if another emergency call is dialed, Ecm mode will not exit.
            if(!isPhoneInEcmMode || (isPhoneInEcmMode && isEmergencyCall)) {
                mCi.dial(mPendingMO.getAddress(), mPendingMO.isEmergencyCall(),
                        mPendingMO.getEmergencyNumberInfo(),
                        mPendingMO.hasKnownUserIntentEmergency(),
                        clirMode, obtainCompleteMessage());
            } else {
                mPhone.exitEmergencyCallbackMode();
                mPhone.setOnEcbModeExitResponse(this,EVENT_EXIT_ECM_RESPONSE_CDMA, null);
                mPendingCallClirMode=clirMode;
                mPendingCallInEcm=true;
            }
        }

        if (mNumberConverted) {
            mPendingMO.restoreDialedNumberAfterConversion(origNumber);
            mNumberConverted = false;
        }

        updatePhoneState();
        mPhone.notifyPreciseCallStateChanged();

        return mPendingMO;
    }

    //CDMA
    private Connection dialThreeWay(String dialString, DialArgs dialArgs) {
        Bundle intentExtras = dialArgs.intentExtras;

        if (!mForegroundCall.isIdle()) {
            // Check data call and possibly set mIsInEmergencyCall
            disableDataCallInEmergencyCall(dialString);

            // Attach the new connection to foregroundCall
            mPendingMO = new GsmCdmaConnection(mPhone, dialString, this, mForegroundCall,
                    dialArgs);
            if (intentExtras != null) {
                Rlog.d(LOG_TAG, "dialThreeWay - emergency dialer " + intentExtras.getBoolean(
                        TelecomManager.EXTRA_IS_USER_INTENT_EMERGENCY_CALL));
                mPendingMO.setHasKnownUserIntentEmergency(intentExtras.getBoolean(
                        TelecomManager.EXTRA_IS_USER_INTENT_EMERGENCY_CALL));
            }
            // Some networks need an empty flash before sending the normal one
            CarrierConfigManager configManager = (CarrierConfigManager)
                    mPhone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle bundle = configManager.getConfigForSubId(mPhone.getSubId());
            if (bundle != null) {
                m3WayCallFlashDelay =
                        bundle.getInt(CarrierConfigManager.KEY_CDMA_3WAYCALL_FLASH_DELAY_INT);
            } else {
                // The default 3-way call flash delay is 0s
                m3WayCallFlashDelay = 0;
            }
            if (m3WayCallFlashDelay > 0) {
                mCi.sendCDMAFeatureCode("", obtainMessage(EVENT_THREE_WAY_DIAL_BLANK_FLASH));
            } else {
                mCi.sendCDMAFeatureCode(mPendingMO.getAddress(),
                        obtainMessage(EVENT_THREE_WAY_DIAL_L2_RESULT_CDMA));
            }
            return mPendingMO;
        }
        return null;
    }

    public Connection dial(String dialString, DialArgs dialArgs) throws CallStateException {
        if (isPhoneTypeGsm()) {
            return dialGsm(dialString, dialArgs);
        } else {
            return dialCdma(dialString, dialArgs);
        }
    }

    //GSM
    public Connection dialGsm(String dialString, UUSInfo uusInfo, Bundle intentExtras)
            throws CallStateException {
        return dialGsm(dialString, new DialArgs.Builder<>()
                        .setUusInfo(uusInfo)
                        .setClirMode(CommandsInterface.CLIR_DEFAULT)
                        .setIntentExtras(intentExtras)
                        .build());
    }

    //GSM
    private Connection dialGsm(String dialString, int clirMode, Bundle intentExtras)
            throws CallStateException {
        return dialGsm(dialString, new DialArgs.Builder<>()
                        .setClirMode(clirMode)
                        .setIntentExtras(intentExtras)
                        .build());
    }

    //GSM
    public Connection dialGsm(String dialString, int clirMode, UUSInfo uusInfo, Bundle intentExtras)
             throws CallStateException {
        return dialGsm(dialString, new DialArgs.Builder<>()
                        .setClirMode(clirMode)
                        .setUusInfo(uusInfo)
                        .setIntentExtras(intentExtras)
                        .build());
    }

    public void acceptCall() throws CallStateException {
        // FIXME if SWITCH fails, should retry with ANSWER
        // in case the active/holding call disappeared and this
        // is no longer call waiting

        if (mRingingCall.getState() == GsmCdmaCall.State.INCOMING) {
            Rlog.i("phone", "acceptCall: incoming...");
            // Always unmute when answering a new call
            setMute(false);
            mPhone.getVoiceCallSessionStats().onRilAcceptCall(mRingingCall.getConnections());
            mCi.acceptCall(obtainCompleteMessage());
        } else if (mRingingCall.getState() == GsmCdmaCall.State.WAITING) {
            if (isPhoneTypeGsm()) {
                setMute(false);
            } else {
                GsmCdmaConnection cwConn = (GsmCdmaConnection)(mRingingCall.getLatestConnection());

                // Since there is no network response for supplimentary
                // service for CDMA, we assume call waiting is answered.
                // ringing Call state change to idle is in GsmCdmaCall.detach
                // triggered by updateParent.
                cwConn.updateParent(mRingingCall, mForegroundCall);
                cwConn.onConnectedInOrOut();
                updatePhoneState();
            }
            switchWaitingOrHoldingAndActive();
        } else {
            throw new CallStateException("phone not ringing");
        }
    }

    public void rejectCall() throws CallStateException {
        // AT+CHLD=0 means "release held or UDUB"
        // so if the phone isn't ringing, this could hang up held
        if (mRingingCall.getState().isRinging()) {
            mCi.rejectCall(obtainCompleteMessage());
        } else {
            throw new CallStateException("phone not ringing");
        }
    }

    //CDMA
    private void flashAndSetGenericTrue() {
        mCi.sendCDMAFeatureCode("", obtainMessage(EVENT_SWITCH_RESULT));

        mPhone.notifyPreciseCallStateChanged();
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void switchWaitingOrHoldingAndActive() throws CallStateException {
        // Should we bother with this check?
        if (mRingingCall.getState() == GsmCdmaCall.State.INCOMING) {
            throw new CallStateException("cannot be in the incoming state");
        } else {
            if (isPhoneTypeGsm()) {
                mCi.switchWaitingOrHoldingAndActive(
                        obtainCompleteMessage(EVENT_SWITCH_RESULT));
            } else {
                if (mForegroundCall.getConnectionsCount() > 1) {
                    flashAndSetGenericTrue();
                } else {
                    // Send a flash command to CDMA network for putting the other party on hold.
                    // For CDMA networks which do not support this the user would just hear a beep
                    // from the network. For CDMA networks which do support it will put the other
                    // party on hold.
                    mCi.sendCDMAFeatureCode("", obtainMessage(EVENT_SWITCH_RESULT));
                }
            }
        }
    }

    public void conference() {
        if (isPhoneTypeGsm()) {
            mCi.conference(obtainCompleteMessage(EVENT_CONFERENCE_RESULT));
        } else {
            // Should we be checking state?
            flashAndSetGenericTrue();
        }
    }

    public void explicitCallTransfer() {
        mCi.explicitCallTransfer(obtainCompleteMessage(EVENT_ECT_RESULT));
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void clearDisconnected() {
        internalClearDisconnected();

        updatePhoneState();
        mPhone.notifyPreciseCallStateChanged();
    }

    public boolean canConference() {
        return mForegroundCall.getState() == GsmCdmaCall.State.ACTIVE
                && mBackgroundCall.getState() == GsmCdmaCall.State.HOLDING
                && !mBackgroundCall.isFull()
                && !mForegroundCall.isFull();
    }

    /**
     * Determines if there are issues which would preclude dialing an outgoing call.  Throws a
     * {@link CallStateException} if there is an issue.
     * @throws CallStateException
     */
    public void checkForDialIssues(boolean isEmergencyCall) throws CallStateException {
        boolean disableCall = TelephonyProperties.disable_call().orElse(false);

        if (mCi.getRadioState() != TelephonyManager.RADIO_POWER_ON) {
            throw new CallStateException(CallStateException.ERROR_POWER_OFF,
                    "Modem not powered");
        }
        if (disableCall) {
            throw new CallStateException(CallStateException.ERROR_CALLING_DISABLED,
                    "Calling disabled via ro.telephony.disable-call property");
        }
        if (mPendingMO != null) {
            throw new CallStateException(CallStateException.ERROR_ALREADY_DIALING,
                    "A call is already dialing.");
        }
        if (mRingingCall.isRinging()) {
            throw new CallStateException(CallStateException.ERROR_CALL_RINGING,
                    "Can't call while a call is ringing.");
        }
        if (isPhoneTypeGsm()
                && mForegroundCall.getState().isAlive() && mBackgroundCall.getState().isAlive()) {
            throw new CallStateException(CallStateException.ERROR_TOO_MANY_CALLS,
                    "There is already a foreground and background call.");
        }
        if (!isPhoneTypeGsm()
                // Essentially foreground call state is one of:
                // HOLDING, DIALING, ALERTING, INCOMING, WAITING
                && mForegroundCall.getState().isAlive()
                && mForegroundCall.getState() != GsmCdmaCall.State.ACTIVE

                && mBackgroundCall.getState().isAlive()) {
            throw new CallStateException(CallStateException.ERROR_TOO_MANY_CALLS,
                    "There is already a foreground and background call.");
        }
        if (!isEmergencyCall && isInOtaspCall()) {
            throw new CallStateException(CallStateException.ERROR_OTASP_PROVISIONING_IN_PROCESS,
                    "OTASP provisioning is in process.");
        }
    }

    public boolean canTransfer() {
        if (isPhoneTypeGsm()) {
            return (mForegroundCall.getState() == GsmCdmaCall.State.ACTIVE
                    || mForegroundCall.getState() == GsmCdmaCall.State.ALERTING
                    || mForegroundCall.getState() == GsmCdmaCall.State.DIALING)
                    && mBackgroundCall.getState() == GsmCdmaCall.State.HOLDING;
        } else {
            Rlog.e(LOG_TAG, "canTransfer: not possible in CDMA");
            return false;
        }
    }

    //***** Private Instance Methods

    private void internalClearDisconnected() {
        mRingingCall.clearDisconnected();
        mForegroundCall.clearDisconnected();
        mBackgroundCall.clearDisconnected();
    }

    /**
     * Obtain a message to use for signalling "invoke getCurrentCalls() when
     * this operation and all other pending operations are complete
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private Message obtainCompleteMessage() {
        return obtainCompleteMessage(EVENT_OPERATION_COMPLETE);
    }

    /**
     * Obtain a message to use for signalling "invoke getCurrentCalls() when
     * this operation and all other pending operations are complete
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private Message obtainCompleteMessage(int what) {
        mPendingOperations++;
        mLastRelevantPoll = null;
        mNeedsPoll = true;

        if (DBG_POLL) log("obtainCompleteMessage: pendingOperations=" +
                mPendingOperations + ", needsPoll=" + mNeedsPoll);

        return obtainMessage(what);
    }

    private void operationComplete() {
        mPendingOperations--;

        if (DBG_POLL) log("operationComplete: pendingOperations=" +
                mPendingOperations + ", needsPoll=" + mNeedsPoll);

        if (mPendingOperations == 0 && mNeedsPoll) {
            mLastRelevantPoll = obtainMessage(EVENT_POLL_CALLS_RESULT);
            mCi.getCurrentCalls(mLastRelevantPoll);
        } else if (mPendingOperations < 0) {
            // this should never happen
            Rlog.e(LOG_TAG,"GsmCdmaCallTracker.pendingOperations < 0");
            mPendingOperations = 0;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void updatePhoneState() {
        PhoneConstants.State oldState = mState;
        if (mRingingCall.isRinging()) {
            mState = PhoneConstants.State.RINGING;
        } else if (mPendingMO != null ||
                !(mForegroundCall.isIdle() && mBackgroundCall.isIdle())) {
            mState = PhoneConstants.State.OFFHOOK;
        } else {
            Phone imsPhone = mPhone.getImsPhone();
            if ( mState == PhoneConstants.State.OFFHOOK && (imsPhone != null)){
                imsPhone.callEndCleanupHandOverCallIfAny();
            }
            mState = PhoneConstants.State.IDLE;
        }

        if (mState == PhoneConstants.State.IDLE && oldState != mState) {
            mVoiceCallEndedRegistrants.notifyRegistrants(
                new AsyncResult(null, null, null));
        } else if (oldState == PhoneConstants.State.IDLE && oldState != mState) {
            mVoiceCallStartedRegistrants.notifyRegistrants (
                    new AsyncResult(null, null, null));
        }
        if (Phone.DEBUG_PHONE) {
            log("update phone state, old=" + oldState + " new="+ mState);
        }
        if (mState != oldState) {
            mPhone.notifyPhoneStateChanged();
            mMetrics.writePhoneState(mPhone.getPhoneId(), mState);
        }
    }

    // ***** Overwritten from CallTracker

    @Override
    protected synchronized void handlePollCalls(AsyncResult ar) {
        List polledCalls;

        if (VDBG) log("handlePollCalls");
        if (ar.exception == null) {
            polledCalls = (List)ar.result;
        } else if (isCommandExceptionRadioNotAvailable(ar.exception)) {
            // just a placeholder empty ArrayList to cause the loop
            // to hang up all the calls
            polledCalls = new ArrayList();
        } else {
            // Radio probably wasn't ready--try again in a bit
            // But don't keep polling if the channel is closed
            pollCallsAfterDelay();
            return;
        }

        Connection newRinging = null; //or waiting
        ArrayList<Connection> newUnknownConnectionsGsm = new ArrayList<Connection>();
        Connection newUnknownConnectionCdma = null;
        boolean hasNonHangupStateChanged = false;   // Any change besides
                                                    // a dropped connection
        boolean hasAnyCallDisconnected = false;
        boolean needsPollDelay = false;
        boolean unknownConnectionAppeared = false;
        int handoverConnectionsSize = mHandoverConnections.size();

        //CDMA
        boolean noConnectionExists = true;

        for (int i = 0, curDC = 0, dcSize = polledCalls.size()
                ; i < mConnections.length; i++) {
            GsmCdmaConnection conn = mConnections[i];
            DriverCall dc = null;

            // polledCall list is sparse
            if (curDC < dcSize) {
                dc = (DriverCall) polledCalls.get(curDC);

                if (dc.index == i+1) {
                    curDC++;
                } else {
                    dc = null;
                }
            }

            //CDMA
            if (conn != null || dc != null) {
                noConnectionExists = false;
            }

            if (DBG_POLL) log("poll: conn[i=" + i + "]=" +
                    conn+", dc=" + dc);

            if (conn == null && dc != null) {
                // Connection appeared in CLCC response that we don't know about
                if (mPendingMO != null && mPendingMO.compareTo(dc)) {

                    if (DBG_POLL) log("poll: pendingMO=" + mPendingMO);

                    // It's our pending mobile originating call
                    mConnections[i] = mPendingMO;
                    mPendingMO.mIndex = i;
                    mPendingMO.update(dc);
                    mPendingMO = null;

                    // Someone has already asked to hangup this call
                    if (mHangupPendingMO) {
                        mHangupPendingMO = false;

                        // Re-start Ecm timer when an uncompleted emergency call ends
                        if (!isPhoneTypeGsm() && mPhone.isEcmCanceledForEmergency()) {
                            mPhone.handleTimerInEmergencyCallbackMode(
                                    GsmCdmaPhone.RESTART_ECM_TIMER);
                        }

                        try {
                            if (Phone.DEBUG_PHONE) log(
                                    "poll: hangupPendingMO, hangup conn " + i);
                            hangup(mConnections[i]);
                        } catch (CallStateException ex) {
                            Rlog.e(LOG_TAG, "unexpected error on hangup");
                        }

                        // Do not continue processing this poll
                        // Wait for hangup and repoll
                        return;
                    }
                } else {
                    if (Phone.DEBUG_PHONE) {
                        log("pendingMo=" + mPendingMO + ", dc=" + dc);
                    }

                    mConnections[i] = new GsmCdmaConnection(mPhone, dc, this, i);
                    log("New connection is not mPendingMO. Creating new GsmCdmaConnection,"
                            + " objId=" + System.identityHashCode(mConnections[i]));

                    Connection hoConnection = getHoConnection(dc);
                    if (hoConnection != null) {
                        log("Handover connection found.");
                        // Single Radio Voice Call Continuity (SRVCC) completed
                        mConnections[i].migrateFrom(hoConnection);
                        // Updating connect time for silent redial cases (ex: Calls are transferred
                        // from DIALING/ALERTING/INCOMING/WAITING to ACTIVE)
                        if (hoConnection.mPreHandoverState != GsmCdmaCall.State.ACTIVE &&
                                hoConnection.mPreHandoverState != GsmCdmaCall.State.HOLDING &&
                                dc.state == DriverCall.State.ACTIVE) {
                            mConnections[i].onConnectedInOrOut();
                        } else {
                            mConnections[i].onConnectedConnectionMigrated();
                        }

                        mHandoverConnections.remove(hoConnection);

                        if (isPhoneTypeGsm()) {
                            for (Iterator<Connection> it = mHandoverConnections.iterator();
                                 it.hasNext(); ) {
                                Connection c = it.next();
                                Rlog.i(LOG_TAG, "HO Conn state is " + c.mPreHandoverState);
                                if (c.mPreHandoverState == mConnections[i].getState()) {
                                    Rlog.i(LOG_TAG, "Removing HO conn "
                                            + hoConnection + c.mPreHandoverState);
                                    it.remove();
                                }
                            }
                        }

                        mPhone.notifyHandoverStateChanged(mConnections[i]);
                    } else {
                        // find if the MT call is a new ring or unknown connection
                        log("New connection is not mPendingMO nor a pending handover.");
                        newRinging = checkMtFindNewRinging(dc,i);
                        if (newRinging == null) {
                            unknownConnectionAppeared = true;
                            if (isPhoneTypeGsm()) {
                                newUnknownConnectionsGsm.add(mConnections[i]);
                            } else {
                                newUnknownConnectionCdma = mConnections[i];
                            }
                        }
                    }
                }
                hasNonHangupStateChanged = true;
            } else if (conn != null && dc == null) {
                if (isPhoneTypeGsm()) {
                    // Connection missing in CLCC response that we were
                    // tracking.
                    mDroppedDuringPoll.add(conn);
                } else {
                    // This case means the RIL has no more active call anymore and
                    // we need to clean up the foregroundCall and ringingCall.
                    // Loop through foreground call connections as
                    // it contains the known logical connections.
                    ArrayList<Connection> connections = mForegroundCall.getConnections();
                    for (Connection cn : connections) {
                        if (Phone.DEBUG_PHONE) {
                            log("adding fgCall cn " + cn + "to droppedDuringPoll");
                        }
                        mDroppedDuringPoll.add((GsmCdmaConnection) cn);
                    }

                    connections = mRingingCall.getConnections();
                    // Loop through ringing call connections as
                    // it may contain the known logical connections.
                    for (Connection cn : connections) {
                        if (Phone.DEBUG_PHONE) {
                            log("adding rgCall cn " + cn + "to droppedDuringPoll");
                        }
                        mDroppedDuringPoll.add((GsmCdmaConnection) cn);
                    }

                    // Re-start Ecm timer when the connected emergency call ends
                    if (mPhone.isEcmCanceledForEmergency()) {
                        mPhone.handleTimerInEmergencyCallbackMode(GsmCdmaPhone.RESTART_ECM_TIMER);
                    }
                    // If emergency call is not going through while dialing
                    checkAndEnableDataCallAfterEmergencyCallDropped();
                }
                // Dropped connections are removed from the CallTracker
                // list but kept in the Call list
                mConnections[i] = null;
            } else if (conn != null && dc != null && !conn.compareTo(dc) && isPhoneTypeGsm()) {
                // Connection in CLCC response does not match what
                // we were tracking. Assume dropped call and new call

                mDroppedDuringPoll.add(conn);
                mConnections[i] = new GsmCdmaConnection (mPhone, dc, this, i);

                if (mConnections[i].getCall() == mRingingCall) {
                    newRinging = mConnections[i];
                } // else something strange happened
                hasNonHangupStateChanged = true;
            } else if (conn != null && dc != null) { /* implicit conn.compareTo(dc) */
                // Call collision case
                if (!isPhoneTypeGsm() && conn.isIncoming() != dc.isMT) {
                    if (dc.isMT == true) {
                        // Mt call takes precedence than Mo,drops Mo
                        mDroppedDuringPoll.add(conn);
                        // find if the MT call is a new ring or unknown connection
                        newRinging = checkMtFindNewRinging(dc,i);
                        if (newRinging == null) {
                            unknownConnectionAppeared = true;
                            newUnknownConnectionCdma = conn;
                        }
                        checkAndEnableDataCallAfterEmergencyCallDropped();
                    } else {
                        // Call info stored in conn is not consistent with the call info from dc.
                        // We should follow the rule of MT calls taking precedence over MO calls
                        // when there is conflict, so here we drop the call info from dc and
                        // continue to use the call info from conn, and only take a log.
                        Rlog.e(LOG_TAG,"Error in RIL, Phantom call appeared " + dc);
                    }
                } else {
                    boolean changed;
                    changed = conn.update(dc);
                    hasNonHangupStateChanged = hasNonHangupStateChanged || changed;
                }
            }

            if (REPEAT_POLLING) {
                if (dc != null) {
                    // FIXME with RIL, we should not need this anymore
                    if ((dc.state == DriverCall.State.DIALING
                            /*&& cm.getOption(cm.OPTION_POLL_DIALING)*/)
                        || (dc.state == DriverCall.State.ALERTING
                            /*&& cm.getOption(cm.OPTION_POLL_ALERTING)*/)
                        || (dc.state == DriverCall.State.INCOMING
                            /*&& cm.getOption(cm.OPTION_POLL_INCOMING)*/)
                        || (dc.state == DriverCall.State.WAITING
                            /*&& cm.getOption(cm.OPTION_POLL_WAITING)*/)) {
                        // Sometimes there's no unsolicited notification
                        // for state transitions
                        needsPollDelay = true;
                    }
                }
            }
        }

        // Safety check so that obj is not stuck with mIsInEmergencyCall set to true (and data
        // disabled). This should never happen though.
        if (!isPhoneTypeGsm() && noConnectionExists) {
            checkAndEnableDataCallAfterEmergencyCallDropped();
        }

        // This is the first poll after an ATD.
        // We expect the pending call to appear in the list
        // If it does not, we land here
        if (mPendingMO != null) {
            Rlog.d(LOG_TAG, "Pending MO dropped before poll fg state:"
                    + mForegroundCall.getState());

            mDroppedDuringPoll.add(mPendingMO);
            mPendingMO = null;
            mHangupPendingMO = false;

            if (!isPhoneTypeGsm()) {
                if( mPendingCallInEcm) {
                    mPendingCallInEcm = false;
                }
                checkAndEnableDataCallAfterEmergencyCallDropped();
            }
        }

        if (newRinging != null) {
            mPhone.notifyNewRingingConnection(newRinging);
        }

        // clear the "local hangup" and "missed/rejected call"
        // cases from the "dropped during poll" list
        // These cases need no "last call fail" reason
        ArrayList<GsmCdmaConnection> locallyDisconnectedConnections = new ArrayList<>();
        for (int i = mDroppedDuringPoll.size() - 1; i >= 0 ; i--) {
            GsmCdmaConnection conn = mDroppedDuringPoll.get(i);
            //CDMA
            boolean wasDisconnected = false;

            if (conn.isIncoming() && conn.getConnectTime() == 0) {
                // Missed or rejected call
                int cause;
                if (conn.mCause == DisconnectCause.LOCAL) {
                    cause = DisconnectCause.INCOMING_REJECTED;
                } else {
                    cause = DisconnectCause.INCOMING_MISSED;
                }

                if (Phone.DEBUG_PHONE) {
                    log("missed/rejected call, conn.cause=" + conn.mCause);
                    log("setting cause to " + cause);
                }
                mDroppedDuringPoll.remove(i);
                hasAnyCallDisconnected |= conn.onDisconnect(cause);
                wasDisconnected = true;
                locallyDisconnectedConnections.add(conn);
            } else if (conn.mCause == DisconnectCause.LOCAL
                    || conn.mCause == DisconnectCause.INVALID_NUMBER) {
                mDroppedDuringPoll.remove(i);
                hasAnyCallDisconnected |= conn.onDisconnect(conn.mCause);
                wasDisconnected = true;
                locallyDisconnectedConnections.add(conn);
            }

            if (!isPhoneTypeGsm() && wasDisconnected && unknownConnectionAppeared
                    && conn == newUnknownConnectionCdma) {
                unknownConnectionAppeared = false;
                newUnknownConnectionCdma = null;
            }
        }

        if (locallyDisconnectedConnections.size() > 0) {
            mMetrics.writeRilCallList(mPhone.getPhoneId(), locallyDisconnectedConnections,
                    getNetworkCountryIso());
            mPhone.getVoiceCallSessionStats().onRilCallListChanged(locallyDisconnectedConnections);
        }

        /* Disconnect any pending Handover connections */
        for (Iterator<Connection> it = mHandoverConnections.iterator();
                it.hasNext();) {
            Connection hoConnection = it.next();
            log("handlePollCalls - disconnect hoConn= " + hoConnection +
                    " hoConn.State= " + hoConnection.getState());
            if (hoConnection.getState().isRinging()) {
                hoConnection.onDisconnect(DisconnectCause.INCOMING_MISSED);
            } else {
                hoConnection.onDisconnect(DisconnectCause.NOT_VALID);
            }
            // TODO: Do we need to update these hoConnections in Metrics ?
            it.remove();
        }

        // Any non-local disconnects: determine cause
        if (mDroppedDuringPoll.size() > 0) {
            mCi.getLastCallFailCause(
                obtainNoPollCompleteMessage(EVENT_GET_LAST_CALL_FAIL_CAUSE));
        }

        if (needsPollDelay) {
            pollCallsAfterDelay();
        }

        // Cases when we can no longer keep disconnected Connection's
        // with their previous calls
        // 1) the phone has started to ring
        // 2) A Call/Connection object has changed state...
        //    we may have switched or held or answered (but not hung up)
        if (newRinging != null || hasNonHangupStateChanged || hasAnyCallDisconnected) {
            internalClearDisconnected();
        }

        if (VDBG) log("handlePollCalls calling updatePhoneState()");
        updatePhoneState();

        if (unknownConnectionAppeared) {
            if (isPhoneTypeGsm()) {
                for (Connection c : newUnknownConnectionsGsm) {
                    log("Notify unknown for " + c);
                    mPhone.notifyUnknownConnection(c);
                }
            } else {
                mPhone.notifyUnknownConnection(newUnknownConnectionCdma);
            }
        }

        if (hasNonHangupStateChanged || newRinging != null || hasAnyCallDisconnected) {
            mPhone.notifyPreciseCallStateChanged();
            updateMetrics(mConnections);
        }

        // If all handover connections are mapped during this poll process clean it up
        if (handoverConnectionsSize > 0 && mHandoverConnections.size() == 0) {
            Phone imsPhone = mPhone.getImsPhone();
            if (imsPhone != null) {
                imsPhone.callEndCleanupHandOverCallIfAny();
            }
        }
        //dumpState();
    }

    private void updateMetrics(GsmCdmaConnection[] connections) {
        ArrayList<GsmCdmaConnection> activeConnections = new ArrayList<>();
        for (GsmCdmaConnection conn : connections) {
            if (conn != null) activeConnections.add(conn);
        }
        mMetrics.writeRilCallList(mPhone.getPhoneId(), activeConnections, getNetworkCountryIso());
        mPhone.getVoiceCallSessionStats().onRilCallListChanged(activeConnections);
    }

    private void handleRadioNotAvailable() {
        // handlePollCalls will clear out its
        // call list when it gets the CommandException
        // error result from this
        pollCallsWhenSafe();
    }

    private void dumpState() {
        List l;

        Rlog.i(LOG_TAG,"Phone State:" + mState);

        Rlog.i(LOG_TAG,"Ringing call: " + mRingingCall.toString());

        l = mRingingCall.getConnections();
        for (int i = 0, s = l.size(); i < s; i++) {
            Rlog.i(LOG_TAG,l.get(i).toString());
        }

        Rlog.i(LOG_TAG,"Foreground call: " + mForegroundCall.toString());

        l = mForegroundCall.getConnections();
        for (int i = 0, s = l.size(); i < s; i++) {
            Rlog.i(LOG_TAG,l.get(i).toString());
        }

        Rlog.i(LOG_TAG,"Background call: " + mBackgroundCall.toString());

        l = mBackgroundCall.getConnections();
        for (int i = 0, s = l.size(); i < s; i++) {
            Rlog.i(LOG_TAG,l.get(i).toString());
        }

    }

    //***** Called from GsmCdmaConnection

    public void hangup(GsmCdmaConnection conn) throws CallStateException {
        if (conn.mOwner != this) {
            throw new CallStateException ("GsmCdmaConnection " + conn
                                    + "does not belong to GsmCdmaCallTracker " + this);
        }

        if (conn == mPendingMO) {
            // We're hanging up an outgoing call that doesn't have it's
            // GsmCdma index assigned yet

            if (Phone.DEBUG_PHONE) log("hangup: set hangupPendingMO to true");
            mHangupPendingMO = true;
        } else if (!isPhoneTypeGsm()
                && conn.getCall() == mRingingCall
                && mRingingCall.getState() == GsmCdmaCall.State.WAITING) {
            // Handle call waiting hang up case.
            //
            // The ringingCall state will change to IDLE in GsmCdmaCall.detach
            // if the ringing call connection size is 0. We don't specifically
            // set the ringing call state to IDLE here to avoid a race condition
            // where a new call waiting could get a hang up from an old call
            // waiting ringingCall.
            //
            // PhoneApp does the call log itself since only PhoneApp knows
            // the hangup reason is user ignoring or timing out. So conn.onDisconnect()
            // is not called here. Instead, conn.onLocalDisconnect() is called.
            conn.onLocalDisconnect();

            updatePhoneState();
            mPhone.notifyPreciseCallStateChanged();
            return;
        } else {
            try {
                mMetrics.writeRilHangup(mPhone.getPhoneId(), conn, conn.getGsmCdmaIndex(),
                        getNetworkCountryIso());
                mCi.hangupConnection (conn.getGsmCdmaIndex(), obtainCompleteMessage());
            } catch (CallStateException ex) {
                // Ignore "connection not found"
                // Call may have hung up already
                Rlog.w(LOG_TAG,"GsmCdmaCallTracker WARN: hangup() on absent connection "
                                + conn);
            }
        }

        conn.onHangupLocal();
    }

    public void separate(GsmCdmaConnection conn) throws CallStateException {
        if (conn.mOwner != this) {
            throw new CallStateException ("GsmCdmaConnection " + conn
                                    + "does not belong to GsmCdmaCallTracker " + this);
        }
        try {
            mCi.separateConnection (conn.getGsmCdmaIndex(),
                obtainCompleteMessage(EVENT_SEPARATE_RESULT));
        } catch (CallStateException ex) {
            // Ignore "connection not found"
            // Call may have hung up already
            Rlog.w(LOG_TAG,"GsmCdmaCallTracker WARN: separate() on absent connection " + conn);
        }
    }

    //***** Called from GsmCdmaPhone

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void setMute(boolean mute) {
        mDesiredMute = mute;
        mCi.setMute(mDesiredMute, null);
    }

    public boolean getMute() {
        return mDesiredMute;
    }


    //***** Called from GsmCdmaCall

    public void hangup(GsmCdmaCall call) throws CallStateException {
        if (call.getConnectionsCount() == 0) {
            throw new CallStateException("no connections in call");
        }

        if (call == mRingingCall) {
            if (Phone.DEBUG_PHONE) log("(ringing) hangup waiting or background");
            logHangupEvent(call);
            mCi.hangupWaitingOrBackground(obtainCompleteMessage());
        } else if (call == mForegroundCall) {
            if (call.isDialingOrAlerting()) {
                if (Phone.DEBUG_PHONE) {
                    log("(foregnd) hangup dialing or alerting...");
                }
                hangup((GsmCdmaConnection)(call.getConnections().get(0)));
            } else if (isPhoneTypeGsm()
                    && mRingingCall.isRinging()) {
                // Do not auto-answer ringing on CHUP, instead just end active calls
                log("hangup all conns in active/background call, without affecting ringing call");
                hangupAllConnections(call);
            } else {
                logHangupEvent(call);
                hangupForegroundResumeBackground();
            }
        } else if (call == mBackgroundCall) {
            if (mRingingCall.isRinging()) {
                if (Phone.DEBUG_PHONE) {
                    log("hangup all conns in background call");
                }
                hangupAllConnections(call);
            } else {
                hangupWaitingOrBackground();
            }
        } else {
            throw new RuntimeException ("GsmCdmaCall " + call +
                    "does not belong to GsmCdmaCallTracker " + this);
        }

        call.onHangupLocal();
        mPhone.notifyPreciseCallStateChanged();
    }

    private void logHangupEvent(GsmCdmaCall call) {
        for (Connection conn : call.getConnections()) {
            GsmCdmaConnection c = (GsmCdmaConnection) conn;
            int call_index;
            try {
                call_index = c.getGsmCdmaIndex();
            } catch (CallStateException e) {
                call_index = -1;
            }
            mMetrics.writeRilHangup(mPhone.getPhoneId(), c, call_index, getNetworkCountryIso());
        }
        if (VDBG) {
            Rlog.v(LOG_TAG, "logHangupEvent logged " + call.getConnectionsCount()
                    + " Connections ");
        }
    }

    public void hangupWaitingOrBackground() {
        if (Phone.DEBUG_PHONE) log("hangupWaitingOrBackground");
        logHangupEvent(mBackgroundCall);
        mCi.hangupWaitingOrBackground(obtainCompleteMessage());
    }

    public void hangupForegroundResumeBackground() {
        if (Phone.DEBUG_PHONE) log("hangupForegroundResumeBackground");
        mCi.hangupForegroundResumeBackground(obtainCompleteMessage());
    }

    public void hangupConnectionByIndex(GsmCdmaCall call, int index)
            throws CallStateException {
        for (Connection conn : call.getConnections()) {
            GsmCdmaConnection c = (GsmCdmaConnection) conn;
            if (!c.mDisconnected && c.getGsmCdmaIndex() == index) {
                mMetrics.writeRilHangup(mPhone.getPhoneId(), c, c.getGsmCdmaIndex(),
                        getNetworkCountryIso());
                mCi.hangupConnection(index, obtainCompleteMessage());
                return;
            }
        }
        throw new CallStateException("no GsmCdma index found");
    }

    public void hangupAllConnections(GsmCdmaCall call) {
        try {
            for (Connection conn : call.getConnections()) {
                GsmCdmaConnection c = (GsmCdmaConnection) conn;
                if (!c.mDisconnected) {
                    mMetrics.writeRilHangup(mPhone.getPhoneId(), c, c.getGsmCdmaIndex(),
                            getNetworkCountryIso());
                    mCi.hangupConnection(c.getGsmCdmaIndex(), obtainCompleteMessage());
                }
            }
        } catch (CallStateException ex) {
            Rlog.e(LOG_TAG, "hangupConnectionByIndex caught " + ex);
        }
    }

    public GsmCdmaConnection getConnectionByIndex(GsmCdmaCall call, int index)
            throws CallStateException {
        for (Connection conn : call.getConnections()) {
            GsmCdmaConnection c = (GsmCdmaConnection) conn;
            if (!c.mDisconnected && c.getGsmCdmaIndex() == index) {
                return c;
            }
        }
        return null;
    }

    //CDMA
    private void notifyCallWaitingInfo(CdmaCallWaitingNotification obj) {
        if (mCallWaitingRegistrants != null) {
            mCallWaitingRegistrants.notifyRegistrants(new AsyncResult(null, obj, null));
        }
    }

    //CDMA
    private void handleCallWaitingInfo(CdmaCallWaitingNotification cw) {
        // Create a new GsmCdmaConnection which attaches itself to ringingCall.
        new GsmCdmaConnection(mPhone.getContext(), cw, this, mRingingCall);
        updatePhoneState();

        // Finally notify application
        notifyCallWaitingInfo(cw);
    }

    private Phone.SuppService getFailedService(int what) {
        switch (what) {
            case EVENT_SWITCH_RESULT:
                return Phone.SuppService.SWITCH;
            case EVENT_CONFERENCE_RESULT:
                return Phone.SuppService.CONFERENCE;
            case EVENT_SEPARATE_RESULT:
                return Phone.SuppService.SEPARATE;
            case EVENT_ECT_RESULT:
                return Phone.SuppService.TRANSFER;
        }
        return Phone.SuppService.UNKNOWN;
    }

    //****** Overridden from Handler

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;

        switch (msg.what) {
            case EVENT_POLL_CALLS_RESULT:
                Rlog.d(LOG_TAG, "Event EVENT_POLL_CALLS_RESULT Received");

                if (msg == mLastRelevantPoll) {
                    if (DBG_POLL) log(
                            "handle EVENT_POLL_CALL_RESULT: set needsPoll=F");
                    mNeedsPoll = false;
                    mLastRelevantPoll = null;
                    handlePollCalls((AsyncResult)msg.obj);
                }
            break;

            case EVENT_OPERATION_COMPLETE:
                operationComplete();
            break;

            case EVENT_CONFERENCE_RESULT:
                if (isPhoneTypeGsm()) {
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        // The conference merge failed, so notify listeners.  Ultimately this
                        // bubbles up to Telecom, which will inform the InCall UI of the failure.
                        Connection connection = mForegroundCall.getLatestConnection();
                        if (connection != null) {
                            connection.onConferenceMergeFailed();
                        }
                    }
                }
                // fall through
            case EVENT_SEPARATE_RESULT:
            case EVENT_ECT_RESULT:
            case EVENT_SWITCH_RESULT:
                if (isPhoneTypeGsm()) {
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        if (msg.what == EVENT_SWITCH_RESULT) {
                            Connection connection = mForegroundCall.getLatestConnection();
                            if (connection != null) {
                                if (mBackgroundCall.getState() != GsmCdmaCall.State.HOLDING) {
                                    connection.onConnectionEvent(
                                            android.telecom.Connection.EVENT_CALL_HOLD_FAILED,
                                            null);
                                } else {
                                    connection.onConnectionEvent(
                                            android.telecom.Connection.EVENT_CALL_SWITCH_FAILED,
                                            null);
                                }
                            }
                        }
                        mPhone.notifySuppServiceFailed(getFailedService(msg.what));
                    }
                    operationComplete();
                } else {
                    if (msg.what != EVENT_SWITCH_RESULT) {
                        // EVENT_SWITCH_RESULT in GSM call triggers operationComplete() which gets
                        // the current call list. But in CDMA there is no list so there is nothing
                        // to do. Other messages however are not expected in CDMA.
                        throw new RuntimeException("unexpected event " + msg.what + " not handled by " +
                                "phone type " + mPhone.getPhoneType());
                    }
                }
            break;

            case EVENT_GET_LAST_CALL_FAIL_CAUSE:
                int causeCode;
                String vendorCause = null;
                ar = (AsyncResult)msg.obj;

                operationComplete();

                if (ar.exception != null) {
                    if (ar.exception instanceof CommandException) {
                        // If we get a CommandException, there are some modem-reported command
                        // errors which are truly exceptional.  We shouldn't treat these as
                        // NORMAL_CLEARING, so we'll re-map to ERROR_UNSPECIFIED.
                        CommandException commandException = (CommandException) ar.exception;
                        switch (commandException.getCommandError()) {
                            case RADIO_NOT_AVAILABLE:
                                // Intentional fall-through.
                            case NO_MEMORY:
                                // Intentional fall-through.
                            case INTERNAL_ERR:
                                // Intentional fall-through.
                            case NO_RESOURCES:
                                causeCode = CallFailCause.ERROR_UNSPECIFIED;

                                // Report the actual internal command error as the vendor cause;
                                // this will ensure it gets bubbled up into the Telecom logs.
                                vendorCause = commandException.getCommandError().toString();
                                break;
                            default:
                                causeCode = CallFailCause.NORMAL_CLEARING;
                        }
                    } else {
                        // An exception occurred...just treat the disconnect
                        // cause as "normal"
                        causeCode = CallFailCause.NORMAL_CLEARING;
                        Rlog.i(LOG_TAG,
                                "Exception during getLastCallFailCause, assuming normal "
                                        + "disconnect");
                    }
                } else {
                    LastCallFailCause failCause = (LastCallFailCause)ar.result;
                    causeCode = failCause.causeCode;
                    vendorCause = failCause.vendorCause;
                }
                // Log the causeCode if its not normal
                if (causeCode == CallFailCause.NO_CIRCUIT_AVAIL ||
                    causeCode == CallFailCause.TEMPORARY_FAILURE ||
                    causeCode == CallFailCause.SWITCHING_CONGESTION ||
                    causeCode == CallFailCause.CHANNEL_NOT_AVAIL ||
                    causeCode == CallFailCause.QOS_NOT_AVAIL ||
                    causeCode == CallFailCause.BEARER_NOT_AVAIL ||
                    causeCode == CallFailCause.ERROR_UNSPECIFIED) {

                    CellLocation loc = mPhone.getCurrentCellIdentity().asCellLocation();
                    int cid = -1;
                    if (loc != null) {
                        if (loc instanceof GsmCellLocation) {
                            cid = ((GsmCellLocation)loc).getCid();
                        } else if (loc instanceof CdmaCellLocation) {
                            cid = ((CdmaCellLocation)loc).getBaseStationId();
                        }
                    }
                    EventLog.writeEvent(EventLogTags.CALL_DROP, causeCode, cid,
                            TelephonyManager.getDefault().getNetworkType());
                }

                if (isEmcRetryCause(causeCode) && mPhone.useImsForEmergency()) {
                    String dialString = "";
                    for(Connection conn : mForegroundCall.mConnections) {
                        GsmCdmaConnection gsmCdmaConnection = (GsmCdmaConnection)conn;
                        dialString = gsmCdmaConnection.getOrigDialString();
                        gsmCdmaConnection.getCall().detach(gsmCdmaConnection);
                        mDroppedDuringPoll.remove(gsmCdmaConnection);
                    }
                    mPhone.notifyVolteSilentRedial(dialString, causeCode);
                    updatePhoneState();
                    if (mDroppedDuringPoll.isEmpty()) {
                        log("LAST_CALL_FAIL_CAUSE - no Dropped normal Call");
                        return;
                    }
                }

                for (int i = 0, s = mDroppedDuringPoll.size(); i < s ; i++) {
                    GsmCdmaConnection conn = mDroppedDuringPoll.get(i);

                    conn.onRemoteDisconnect(causeCode, vendorCause);
                }

                updatePhoneState();

                mPhone.notifyPreciseCallStateChanged();
                mMetrics.writeRilCallList(mPhone.getPhoneId(), mDroppedDuringPoll,
                        getNetworkCountryIso());
                mPhone.getVoiceCallSessionStats().onRilCallListChanged(mDroppedDuringPoll);
                mDroppedDuringPoll.clear();
            break;

            case EVENT_REPOLL_AFTER_DELAY:
            case EVENT_CALL_STATE_CHANGE:
                pollCallsWhenSafe();
            break;

            case EVENT_RADIO_AVAILABLE:
                handleRadioAvailable();
            break;

            case EVENT_RADIO_NOT_AVAILABLE:
                handleRadioNotAvailable();
            break;

            case EVENT_EXIT_ECM_RESPONSE_CDMA:
                if (!isPhoneTypeGsm()) {
                    // no matter the result, we still do the same here
                    if (mPendingCallInEcm) {
                        mCi.dial(mPendingMO.getAddress(), mPendingMO.isEmergencyCall(),
                                mPendingMO.getEmergencyNumberInfo(),
                                mPendingMO.hasKnownUserIntentEmergency(),
                                mPendingCallClirMode, obtainCompleteMessage());
                        mPendingCallInEcm = false;
                    }
                    mPhone.unsetOnEcbModeExitResponse(this);
                } else {
                    throw new RuntimeException("unexpected event " + msg.what + " not handled by " +
                            "phone type " + mPhone.getPhoneType());
                }
                break;

            case EVENT_CALL_WAITING_INFO_CDMA:
                if (!isPhoneTypeGsm()) {
                    ar = (AsyncResult)msg.obj;
                    if (ar.exception == null) {
                        handleCallWaitingInfo((CdmaCallWaitingNotification)ar.result);
                        Rlog.d(LOG_TAG, "Event EVENT_CALL_WAITING_INFO_CDMA Received");
                    }
                } else {
                    throw new RuntimeException("unexpected event " + msg.what + " not handled by " +
                            "phone type " + mPhone.getPhoneType());
                }
                break;

            case EVENT_THREE_WAY_DIAL_L2_RESULT_CDMA:
                if (!isPhoneTypeGsm()) {
                    ar = (AsyncResult)msg.obj;
                    if (ar.exception == null) {
                        // Assume 3 way call is connected
                        mPendingMO.onConnectedInOrOut();
                        mPendingMO = null;
                    }
                } else {
                    throw new RuntimeException("unexpected event " + msg.what + " not handled by " +
                            "phone type " + mPhone.getPhoneType());
                }
                break;

            case EVENT_THREE_WAY_DIAL_BLANK_FLASH:
                if (!isPhoneTypeGsm()) {
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        postDelayed(
                                new Runnable() {
                                    public void run() {
                                        if (mPendingMO != null) {
                                            mCi.sendCDMAFeatureCode(mPendingMO.getAddress(),
                                                    obtainMessage(EVENT_THREE_WAY_DIAL_L2_RESULT_CDMA));
                                        }
                                    }
                                }, m3WayCallFlashDelay);
                    } else {
                        mPendingMO = null;
                        Rlog.w(LOG_TAG, "exception happened on Blank Flash for 3-way call");
                    }
                } else {
                    throw new RuntimeException("unexpected event " + msg.what + " not handled by " +
                            "phone type " + mPhone.getPhoneType());
                }
                break;

            default:{
                throw new RuntimeException("unexpected event " + msg.what + " not handled by " +
                        "phone type " + mPhone.getPhoneType());
            }
        }
    }

    /**
     * Dispatches the CS call radio technology to all exist connections.
     *
     * @param vrat the RIL voice radio technology for CS calls,
     *             see {@code RIL_RADIO_TECHNOLOGY_*} in {@link android.telephony.ServiceState}.
     */
    public void dispatchCsCallRadioTech(@RilRadioTechnology int vrat) {
        if (mConnections == null) {
            log("dispatchCsCallRadioTech: mConnections is null");
            return;
        }
        for (GsmCdmaConnection gsmCdmaConnection : mConnections) {
            if (gsmCdmaConnection != null) {
                gsmCdmaConnection.setCallRadioTech(vrat);
            }
        }
    }

    //CDMA
    /**
     * Check and enable data call after an emergency call is dropped if it's
     * not in ECM
     */
    private void checkAndEnableDataCallAfterEmergencyCallDropped() {
        if (mIsInEmergencyCall) {
            mIsInEmergencyCall = false;
            boolean inEcm = mPhone.isInEcm();
            if (Phone.DEBUG_PHONE) {
                log("checkAndEnableDataCallAfterEmergencyCallDropped,inEcm=" + inEcm);
            }
            if (!inEcm) {
                // Re-initiate data connection
                if (!mPhone.isUsingNewDataStack()) {
                    mPhone.getDataEnabledSettings().setInternalDataEnabled(true);
                }
                mPhone.notifyEmergencyCallRegistrants(false);
            }
            mPhone.sendEmergencyCallStateChange(false);
        }
    }

    /**
     * Check the MT call to see if it's a new ring or
     * a unknown connection.
     */
    private Connection checkMtFindNewRinging(DriverCall dc, int i) {

        Connection newRinging = null;

        // it's a ringing call
        if (mConnections[i].getCall() == mRingingCall) {
            newRinging = mConnections[i];
            if (Phone.DEBUG_PHONE) log("Notify new ring " + dc);
        } else {
            // Something strange happened: a call which is neither
            // a ringing call nor the one we created. It could be the
            // call collision result from RIL
            Rlog.e(LOG_TAG,"Phantom call appeared " + dc);
            // If it's a connected call, set the connect time so that
            // it's non-zero.  It may not be accurate, but at least
            // it won't appear as a Missed Call.
            if (dc.state != DriverCall.State.ALERTING
                    && dc.state != DriverCall.State.DIALING) {
                mConnections[i].onConnectedInOrOut();
                if (dc.state == DriverCall.State.HOLDING) {
                    // We've transitioned into HOLDING
                    mConnections[i].onStartedHolding();
                }
            }
        }
        return newRinging;
    }

    //CDMA
    /**
     * Check if current call is in emergency call
     *
     * @return true if it is in emergency call
     *         false if it is not in emergency call
     */
    public boolean isInEmergencyCall() {
        return mIsInEmergencyCall;
    }

    /**
     * @return {@code true} if the pending outgoing call or active call is an OTASP call,
     * {@code false} otherwise.
     */
    public boolean isInOtaspCall() {
        return mPendingMO != null && mPendingMO.isOtaspCall()
                || (mForegroundCall.getConnections().stream()
                .filter(connection -> ((connection instanceof GsmCdmaConnection)
                        && (((GsmCdmaConnection) connection).isOtaspCall())))
                .count() > 0);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean isPhoneTypeGsm() {
        return mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public GsmCdmaPhone getPhone() {
        return mPhone;
    }

    private boolean isEmcRetryCause(int causeCode) {
        if (causeCode == CallFailCause.EMC_REDIAL_ON_IMS ||
            causeCode == CallFailCause.EMC_REDIAL_ON_VOWIFI) {
            return true;
        }
        return false;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    protected void log(String msg) {
        Rlog.d(LOG_TAG, "[" + mPhone.getPhoneId() + "] " + msg);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("GsmCdmaCallTracker extends:");
        super.dump(fd, pw, args);
        pw.println("mConnections: length=" + mConnections.length);
        for(int i=0; i < mConnections.length; i++) {
            pw.printf("  mConnections[%d]=%s\n", i, mConnections[i]);
        }
        pw.println(" mVoiceCallEndedRegistrants=" + mVoiceCallEndedRegistrants);
        pw.println(" mVoiceCallStartedRegistrants=" + mVoiceCallStartedRegistrants);
        if (!isPhoneTypeGsm()) {
            pw.println(" mCallWaitingRegistrants=" + mCallWaitingRegistrants);
        }
        pw.println(" mDroppedDuringPoll: size=" + mDroppedDuringPoll.size());
        for(int i = 0; i < mDroppedDuringPoll.size(); i++) {
            pw.printf( "  mDroppedDuringPoll[%d]=%s\n", i, mDroppedDuringPoll.get(i));
        }
        pw.println(" mRingingCall=" + mRingingCall);
        pw.println(" mForegroundCall=" + mForegroundCall);
        pw.println(" mBackgroundCall=" + mBackgroundCall);
        pw.println(" mPendingMO=" + mPendingMO);
        pw.println(" mHangupPendingMO=" + mHangupPendingMO);
        pw.println(" mPhone=" + mPhone);
        pw.println(" mDesiredMute=" + mDesiredMute);
        pw.println(" mState=" + mState);
        if (!isPhoneTypeGsm()) {
            pw.println(" mPendingCallInEcm=" + mPendingCallInEcm);
            pw.println(" mIsInEmergencyCall=" + mIsInEmergencyCall);
            pw.println(" mPendingCallClirMode=" + mPendingCallClirMode);
        }

    }

    @Override
    public PhoneConstants.State getState() {
        return mState;
    }

    public int getMaxConnectionsPerCall() {
        return mPhone.isPhoneTypeGsm() ?
                MAX_CONNECTIONS_PER_CALL_GSM :
                MAX_CONNECTIONS_PER_CALL_CDMA;
    }

    private String getNetworkCountryIso() {
        String countryIso = "";
        if (mPhone != null) {
            ServiceStateTracker sst = mPhone.getServiceStateTracker();
            if (sst != null) {
                LocaleTracker lt = sst.getLocaleTracker();
                if (lt != null) {
                    countryIso = lt.getCurrentCountry();
                }
            }
        }
        return countryIso;
    }

    /**
     * Called to force the call tracker to cleanup any stale calls.  Does this by triggering
     * {@code GET_CURRENT_CALLS} on the RIL.
     */
    @Override
    public void cleanupCalls() {
        pollCallsWhenSafe();
    }
}
