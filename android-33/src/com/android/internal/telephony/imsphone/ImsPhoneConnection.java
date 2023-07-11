/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.internal.telephony.imsphone;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.SystemClock;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.DisconnectCause;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.ims.AudioCodecAttributes;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsStreamMediaProfile;
import android.telephony.ims.RtpHeaderExtension;
import android.telephony.ims.RtpHeaderExtensionType;
import android.text.TextUtils;

import com.android.ims.ImsCall;
import com.android.ims.ImsException;
import com.android.ims.internal.ImsVideoCallProviderWrapper;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.emergency.EmergencyNumberTracker;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * {@hide}
 */
public class ImsPhoneConnection extends Connection implements
        ImsVideoCallProviderWrapper.ImsVideoProviderWrapperCallback {

    private static final String LOG_TAG = "ImsPhoneConnection";
    private static final boolean DBG = true;

    //***** Instance Variables

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private ImsPhoneCallTracker mOwner;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private ImsPhoneCall mParent;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private ImsCall mImsCall;
    private Bundle mExtras = new Bundle();
    private TelephonyMetrics mMetrics = TelephonyMetrics.getInstance();

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean mDisconnected;

    /*
    int mIndex;          // index in ImsPhoneCallTracker.connections[], -1 if unassigned
                        // The GSM index is 1 + this
    */

    /*
     * These time/timespan values are based on System.currentTimeMillis(),
     * i.e., "wall clock" time.
     */
    private long mDisconnectTime;

    private UUSInfo mUusInfo;
    private Handler mHandler;
    private final Messenger mHandlerMessenger;

    private PowerManager.WakeLock mPartialWakeLock;

    // The cached connect time of the connection when it turns into a conference.
    private long mConferenceConnectTime = 0;

    // The cached delay to be used between DTMF tones fetched from carrier config.
    private int mDtmfToneDelay = 0;

    private boolean mIsEmergency = false;

    private boolean mIsWpsCall = false;

    /**
     * Used to indicate that video state changes detected by
     * {@link #updateMediaCapabilities(ImsCall)} should be ignored.  When a video state change from
     * unpaused to paused occurs, we set this flag and then update the existing video state when
     * new {@link #onReceiveSessionModifyResponse(int, VideoProfile, VideoProfile)} callbacks come
     * in.  When the video un-pauses we continue receiving the video state updates.
     */
    private boolean mShouldIgnoreVideoStateChanges = false;

    private ImsVideoCallProviderWrapper mImsVideoCallProviderWrapper;

    private int mPreciseDisconnectCause = 0;

    private ImsRttTextHandler mRttTextHandler;
    private android.telecom.Connection.RttTextStream mRttTextStream;
    // This reflects the RTT status as reported to us by the IMS stack via the media profile.
    private boolean mIsRttEnabledForCall = false;

    /**
     * Used to indicate that this call is in the midst of being merged into a conference.
     */
    private boolean mIsMergeInProcess = false;

    /**
     * Used as an override to determine whether video is locally available for this call.
     * This allows video availability to be overridden in the case that the modem says video is
     * currently available, but mobile data is off and the carrier is metering data for video
     * calls.
     */
    private boolean mIsLocalVideoCapable = true;

    /**
     * When the call is in a disconnected, state, will be set to the {@link ImsReasonInfo}
     * associated with the disconnection, if known.
     */
    private ImsReasonInfo mImsReasonInfo;

    //***** Event Constants
    private static final int EVENT_DTMF_DONE = 1;
    private static final int EVENT_PAUSE_DONE = 2;
    private static final int EVENT_NEXT_POST_DIAL = 3;
    private static final int EVENT_WAKE_LOCK_TIMEOUT = 4;
    private static final int EVENT_DTMF_DELAY_DONE = 5;

    //***** Constants
    @VisibleForTesting static final int PAUSE_DELAY_MILLIS = 3 * 1000;
    private static final int WAKE_LOCK_TIMEOUT_MILLIS = 60*1000;

    //***** Inner Classes

    class MyHandler extends Handler {
        MyHandler(Looper l) {super(l);}

        @Override
        public void
        handleMessage(Message msg) {

            switch (msg.what) {
                case EVENT_NEXT_POST_DIAL:
                case EVENT_DTMF_DELAY_DONE:
                case EVENT_PAUSE_DONE:
                    processNextPostDialChar();
                    break;
                case EVENT_WAKE_LOCK_TIMEOUT:
                    releaseWakeLock();
                    break;
                case EVENT_DTMF_DONE:
                    // We may need to add a delay specified by carrier between DTMF tones that are
                    // sent out.
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_DTMF_DELAY_DONE),
                            mDtmfToneDelay);
                    break;
            }
        }
    }

    //***** Constructors

    /** This is probably an MT call */
    public ImsPhoneConnection(Phone phone, ImsCall imsCall, ImsPhoneCallTracker ct,
           ImsPhoneCall parent, boolean isUnknown) {
        super(PhoneConstants.PHONE_TYPE_IMS);
        createWakeLock(phone.getContext());
        acquireWakeLock();

        mOwner = ct;
        mHandler = new MyHandler(mOwner.getLooper());
        mHandlerMessenger = new Messenger(mHandler);
        mImsCall = imsCall;
        mIsAdhocConference = isMultiparty();

        if ((imsCall != null) && (imsCall.getCallProfile() != null)) {
            mAddress = imsCall.getCallProfile().getCallExtra(ImsCallProfile.EXTRA_OI);
            mCnapName = imsCall.getCallProfile().getCallExtra(ImsCallProfile.EXTRA_CNA);
            mNumberPresentation = ImsCallProfile.OIRToPresentation(
                    imsCall.getCallProfile().getCallExtraInt(ImsCallProfile.EXTRA_OIR));
            mCnapNamePresentation = ImsCallProfile.OIRToPresentation(
                    imsCall.getCallProfile().getCallExtraInt(ImsCallProfile.EXTRA_CNAP));
            setNumberVerificationStatus(toTelecomVerificationStatus(
                    imsCall.getCallProfile().getCallerNumberVerificationStatus()));
            updateMediaCapabilities(imsCall);
        } else {
            mNumberPresentation = PhoneConstants.PRESENTATION_UNKNOWN;
            mCnapNamePresentation = PhoneConstants.PRESENTATION_UNKNOWN;
        }

        mIsIncoming = !isUnknown;
        mCreateTime = System.currentTimeMillis();
        mUusInfo = null;

        // Ensure any extras set on the ImsCallProfile at the start of the call are cached locally
        // in the ImsPhoneConnection.  This isn't going to inform any listeners (since the original
        // connection is not likely to be associated with a TelephonyConnection yet).
        updateExtras(imsCall);

        mParent = parent;
        mParent.attach(this,
                (mIsIncoming? ImsPhoneCall.State.INCOMING: ImsPhoneCall.State.DIALING));

        fetchDtmfToneDelay(phone);

        if (phone.getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_use_voip_mode_for_ims)) {
            setAudioModeIsVoip(true);
        }
    }

    /** This is an MO call, created when dialing */
    public ImsPhoneConnection(Phone phone, String dialString, ImsPhoneCallTracker ct,
            ImsPhoneCall parent, boolean isEmergency, boolean isWpsCall) {
        super(PhoneConstants.PHONE_TYPE_IMS);
        createWakeLock(phone.getContext());
        acquireWakeLock();

        mOwner = ct;
        mHandler = new MyHandler(mOwner.getLooper());
        mHandlerMessenger = new Messenger(mHandler);

        mDialString = dialString;

        mAddress = PhoneNumberUtils.extractNetworkPortionAlt(dialString);
        mPostDialString = PhoneNumberUtils.extractPostDialPortion(dialString);

        //mIndex = -1;

        mIsIncoming = false;
        mCnapName = null;
        mCnapNamePresentation = PhoneConstants.PRESENTATION_ALLOWED;
        mNumberPresentation = PhoneConstants.PRESENTATION_ALLOWED;
        mCreateTime = System.currentTimeMillis();

        mParent = parent;
        parent.attachFake(this, ImsPhoneCall.State.DIALING);

        mIsEmergency = isEmergency;
        if (isEmergency) {
            setEmergencyCallInfo(mOwner);
        }

        mIsWpsCall = isWpsCall;

        fetchDtmfToneDelay(phone);

        if (phone.getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_use_voip_mode_for_ims)) {
            setAudioModeIsVoip(true);
        }
    }

    /** This is an MO conference call, created when dialing */
    public ImsPhoneConnection(Phone phone, String[] participantsToDial, ImsPhoneCallTracker ct,
            ImsPhoneCall parent, boolean isEmergency) {
        super(PhoneConstants.PHONE_TYPE_IMS);
        createWakeLock(phone.getContext());
        acquireWakeLock();

        mOwner = ct;
        mHandler = new MyHandler(mOwner.getLooper());
        mHandlerMessenger = new Messenger(mHandler);

        mDialString = mAddress = Connection.ADHOC_CONFERENCE_ADDRESS;
        mParticipantsToDial = participantsToDial;
        mIsAdhocConference = true;

        mIsIncoming = false;
        mCnapName = null;
        mCnapNamePresentation = PhoneConstants.PRESENTATION_ALLOWED;
        mNumberPresentation = PhoneConstants.PRESENTATION_ALLOWED;
        mCreateTime = System.currentTimeMillis();

        mParent = parent;
        parent.attachFake(this, ImsPhoneCall.State.DIALING);

        if (phone.getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_use_voip_mode_for_ims)) {
            setAudioModeIsVoip(true);
        }
    }

    @VisibleForTesting
    public void setTelephonyMetrics(TelephonyMetrics tm) {
        mMetrics = tm;
    }

    public void dispose() {
    }

    static boolean
    equalsHandlesNulls (Object a, Object b) {
        return (a == null) ? (b == null) : a.equals (b);
    }

    static boolean
    equalsBaseDialString (String a, String b) {
        return (a == null) ? (b == null) : (b != null && a.startsWith (b));
    }

    private int applyLocalCallCapabilities(ImsCallProfile localProfile, int capabilities) {
        Rlog.i(LOG_TAG, "applyLocalCallCapabilities - localProfile = " + localProfile);
        capabilities = removeCapability(capabilities,
                Connection.Capability.SUPPORTS_VT_LOCAL_BIDIRECTIONAL);

        if (!mIsLocalVideoCapable) {
            Rlog.i(LOG_TAG, "applyLocalCallCapabilities - disabling video (overidden)");
            return capabilities;
        }
        switch (localProfile.mCallType) {
            case ImsCallProfile.CALL_TYPE_VT:
                // Fall-through
            case ImsCallProfile.CALL_TYPE_VIDEO_N_VOICE:
                capabilities = addCapability(capabilities,
                        Connection.Capability.SUPPORTS_VT_LOCAL_BIDIRECTIONAL);
                break;
        }
        return capabilities;
    }

    private static int applyRemoteCallCapabilities(ImsCallProfile remoteProfile, int capabilities) {
        Rlog.w(LOG_TAG, "applyRemoteCallCapabilities - remoteProfile = "+remoteProfile);
        capabilities = removeCapability(capabilities,
                Connection.Capability.SUPPORTS_VT_REMOTE_BIDIRECTIONAL);
        capabilities = removeCapability(capabilities,
                Connection.Capability.SUPPORTS_RTT_REMOTE);

        switch (remoteProfile.mCallType) {
            case ImsCallProfile.CALL_TYPE_VT:
                // fall-through
            case ImsCallProfile.CALL_TYPE_VIDEO_N_VOICE:
                capabilities = addCapability(capabilities,
                        Connection.Capability.SUPPORTS_VT_REMOTE_BIDIRECTIONAL);
                break;
        }

        if (remoteProfile.getMediaProfile().getRttMode() == ImsStreamMediaProfile.RTT_MODE_FULL) {
            capabilities = addCapability(capabilities, Connection.Capability.SUPPORTS_RTT_REMOTE);
        }
        return capabilities;
    }

    @Override
    public String getOrigDialString(){
        return mDialString;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public ImsPhoneCall getCall() {
        return mParent;
    }

    @Override
    public long getDisconnectTime() {
        return mDisconnectTime;
    }

    @Override
    public long getHoldingStartTime() {
        return mHoldingStartTime;
    }

    @Override
    public long getHoldDurationMillis() {
        if (getState() != ImsPhoneCall.State.HOLDING) {
            // If not holding, return 0
            return 0;
        } else {
            return SystemClock.elapsedRealtime() - mHoldingStartTime;
        }
    }

    public void setDisconnectCause(int cause) {
        Rlog.d(LOG_TAG, "setDisconnectCause: cause=" + cause);
        mCause = cause;
    }

    /** Get the disconnect cause for connection*/
    public int getDisconnectCause() {
        Rlog.d(LOG_TAG, "getDisconnectCause: cause=" + mCause);
        return mCause;
    }

    public boolean isIncomingCallAutoRejected() {
        return mCause == DisconnectCause.INCOMING_AUTO_REJECTED ? true : false;
    }

    @Override
    public String getVendorDisconnectCause() {
      return null;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public ImsPhoneCallTracker getOwner () {
        return mOwner;
    }

    @Override
    public ImsPhoneCall.State getState() {
        if (mDisconnected) {
            return ImsPhoneCall.State.DISCONNECTED;
        } else {
            return super.getState();
        }
    }

    @Override
    public void deflect(String number) throws CallStateException {
        if (mParent.getState().isRinging()) {
            try {
                if (mImsCall != null) {
                    mImsCall.deflect(number);
                } else {
                    throw new CallStateException("no valid ims call to deflect");
                }
            } catch (ImsException e) {
                throw new CallStateException("cannot deflect call");
            }
        } else {
            throw new CallStateException("phone not ringing");
        }
    }

    @Override
    public void transfer(String number, boolean isConfirmationRequired) throws CallStateException {
        try {
            if (mImsCall != null) {
                mImsCall.transfer(number, isConfirmationRequired);
            } else {
                throw new CallStateException("no valid ims call to transfer");
            }
        } catch (ImsException e) {
            throw new CallStateException("cannot transfer call");
        }
    }

    @Override
    public void consultativeTransfer(Connection other) throws CallStateException {
        try {
            if (mImsCall != null) {
                mImsCall.consultativeTransfer(((ImsPhoneConnection) other).getImsCall());
            } else {
                throw new CallStateException("no valid ims call to transfer");
            }
        } catch (ImsException e) {
            throw new CallStateException("cannot transfer call");
        }
    }

    @Override
    public void hangup() throws CallStateException {
        if (!mDisconnected) {
            mOwner.hangup(this);
        } else {
            throw new CallStateException ("disconnected");
        }
    }

    @Override
    public void separate() throws CallStateException {
        throw new CallStateException ("not supported");
    }

    @Override
    public void proceedAfterWaitChar() {
        if (mPostDialState != PostDialState.WAIT) {
            Rlog.w(LOG_TAG, "ImsPhoneConnection.proceedAfterWaitChar(): Expected "
                    + "getPostDialState() to be WAIT but was " + mPostDialState);
            return;
        }

        setPostDialState(PostDialState.STARTED);

        processNextPostDialChar();
    }

    @Override
    public void proceedAfterWildChar(String str) {
        if (mPostDialState != PostDialState.WILD) {
            Rlog.w(LOG_TAG, "ImsPhoneConnection.proceedAfterWaitChar(): Expected "
                    + "getPostDialState() to be WILD but was " + mPostDialState);
            return;
        }

        setPostDialState(PostDialState.STARTED);

        // make a new postDialString, with the wild char replacement string
        // at the beginning, followed by the remaining postDialString.

        StringBuilder buf = new StringBuilder(str);
        buf.append(mPostDialString.substring(mNextPostDialChar));
        mPostDialString = buf.toString();
        mNextPostDialChar = 0;
        if (Phone.DEBUG_PHONE) {
            Rlog.d(LOG_TAG, "proceedAfterWildChar: new postDialString is " +
                    mPostDialString);
        }

        processNextPostDialChar();
    }

    @Override
    public void cancelPostDial() {
        setPostDialState(PostDialState.CANCELLED);
    }

    /**
     * Called when this Connection is being hung up locally (eg, user pressed "end")
     */
    public void onHangupLocal() {
        mCause = DisconnectCause.LOCAL;
    }

    /** Called when the connection has been disconnected */
    @Override
    public boolean onDisconnect(int cause) {
        Rlog.d(LOG_TAG, "onDisconnect: cause=" + cause);
        if (mCause != DisconnectCause.LOCAL || cause == DisconnectCause.INCOMING_REJECTED) {
            mCause = cause;
        }
        return onDisconnect();
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean onDisconnect() {
        boolean changed = false;

        if (!mDisconnected) {
            //mIndex = -1;

            mDisconnectTime = System.currentTimeMillis();
            mDuration = SystemClock.elapsedRealtime() - mConnectTimeReal;
            mDisconnected = true;

            mOwner.mPhone.notifyDisconnect(this);
            notifyDisconnect(mCause);

            if (mParent != null) {
                changed = mParent.connectionDisconnected(this);
            } else {
                Rlog.d(LOG_TAG, "onDisconnect: no parent");
            }
            synchronized (this) {
                if (mRttTextHandler != null) {
                    mRttTextHandler.tearDown();
                }
                if (mImsCall != null) mImsCall.close();
                mImsCall = null;
                if (mImsVideoCallProviderWrapper != null) {
                    mImsVideoCallProviderWrapper.tearDown();
                }
            }
        }
        releaseWakeLock();
        return changed;
    }

    /**
     * An incoming or outgoing call has connected
     */
    void
    onConnectedInOrOut() {
        mConnectTime = System.currentTimeMillis();
        mConnectTimeReal = SystemClock.elapsedRealtime();
        mDuration = 0;

        if (Phone.DEBUG_PHONE) {
            Rlog.d(LOG_TAG, "onConnectedInOrOut: connectTime=" + mConnectTime);
        }

        if (!mIsIncoming) {
            // outgoing calls only
            processNextPostDialChar();
        }
        releaseWakeLock();
    }

    /*package*/ void
    onStartedHolding() {
        mHoldingStartTime = SystemClock.elapsedRealtime();
    }
    /**
     * Performs the appropriate action for a post-dial char, but does not
     * notify application. returns false if the character is invalid and
     * should be ignored
     */
    private boolean
    processPostDialChar(char c) {
        if (PhoneNumberUtils.is12Key(c)) {
            Message dtmfComplete = mHandler.obtainMessage(EVENT_DTMF_DONE);
            dtmfComplete.replyTo = mHandlerMessenger;
            mOwner.sendDtmf(c, dtmfComplete);
        } else if (c == PhoneNumberUtils.PAUSE) {
            // From TS 22.101:
            // It continues...
            // Upon the called party answering the UE shall send the DTMF digits
            // automatically to the network after a delay of 3 seconds( 20 ).
            // The digits shall be sent according to the procedures and timing
            // specified in 3GPP TS 24.008 [13]. The first occurrence of the
            // "DTMF Control Digits Separator" shall be used by the ME to
            // distinguish between the addressing digits (i.e. the phone number)
            // and the DTMF digits. Upon subsequent occurrences of the
            // separator,
            // the UE shall pause again for 3 seconds ( 20 ) before sending
            // any further DTMF digits.
            mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_PAUSE_DONE),
                    PAUSE_DELAY_MILLIS);
        } else if (c == PhoneNumberUtils.WAIT) {
            setPostDialState(PostDialState.WAIT);
        } else if (c == PhoneNumberUtils.WILD) {
            setPostDialState(PostDialState.WILD);
        } else {
            return false;
        }

        return true;
    }

    @Override
    protected void finalize() {
        releaseWakeLock();
    }

    private void
    processNextPostDialChar() {
        char c = 0;
        Registrant postDialHandler;

        if (mPostDialState == PostDialState.CANCELLED) {
            //Rlog.d(LOG_TAG, "##### processNextPostDialChar: postDialState == CANCELLED, bail");
            return;
        }

        if (mPostDialString == null || mPostDialString.length() <= mNextPostDialChar) {
            setPostDialState(PostDialState.COMPLETE);

            // notifyMessage.arg1 is 0 on complete
            c = 0;
        } else {
            boolean isValid;

            setPostDialState(PostDialState.STARTED);

            c = mPostDialString.charAt(mNextPostDialChar++);

            isValid = processPostDialChar(c);

            if (!isValid) {
                // Will call processNextPostDialChar
                mHandler.obtainMessage(EVENT_NEXT_POST_DIAL).sendToTarget();
                // Don't notify application
                Rlog.e(LOG_TAG, "processNextPostDialChar: c=" + c + " isn't valid!");
                return;
            }
        }

        notifyPostDialListenersNextChar(c);

        // TODO: remove the following code since the handler no longer executes anything.
        postDialHandler = mOwner.mPhone.getPostDialHandler();

        Message notifyMessage;

        if (postDialHandler != null
                && (notifyMessage = postDialHandler.messageForRegistrant()) != null) {
            // The AsyncResult.result is the Connection object
            PostDialState state = mPostDialState;
            AsyncResult ar = AsyncResult.forMessage(notifyMessage);
            ar.result = this;
            ar.userObj = state;

            // arg1 is the character that was/is being processed
            notifyMessage.arg1 = c;

            //Rlog.v(LOG_TAG,
            //      "##### processNextPostDialChar: send msg to postDialHandler, arg1=" + c);
            notifyMessage.sendToTarget();
        }
    }

    /**
     * Set post dial state and acquire wake lock while switching to "started"
     * state, the wake lock will be released if state switches out of "started"
     * state or after WAKE_LOCK_TIMEOUT_MILLIS.
     * @param s new PostDialState
     */
    private void setPostDialState(PostDialState s) {
        if (mPostDialState != PostDialState.STARTED
                && s == PostDialState.STARTED) {
            acquireWakeLock();
            Message msg = mHandler.obtainMessage(EVENT_WAKE_LOCK_TIMEOUT);
            mHandler.sendMessageDelayed(msg, WAKE_LOCK_TIMEOUT_MILLIS);
        } else if (mPostDialState == PostDialState.STARTED
                && s != PostDialState.STARTED) {
            mHandler.removeMessages(EVENT_WAKE_LOCK_TIMEOUT);
            releaseWakeLock();
        }
        mPostDialState = s;
        notifyPostDialListeners();
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void
    createWakeLock(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mPartialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void
    acquireWakeLock() {
        Rlog.d(LOG_TAG, "acquireWakeLock");
        mPartialWakeLock.acquire();
    }

    void
    releaseWakeLock() {
        if (mPartialWakeLock != null) {
            synchronized (mPartialWakeLock) {
                if (mPartialWakeLock.isHeld()) {
                    Rlog.d(LOG_TAG, "releaseWakeLock");
                    mPartialWakeLock.release();
                }
            }
        }
    }

    private void fetchDtmfToneDelay(Phone phone) {
        CarrierConfigManager configMgr = (CarrierConfigManager)
                phone.getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = configMgr.getConfigForSubId(phone.getSubId());
        if (b != null) {
            mDtmfToneDelay = b.getInt(CarrierConfigManager.KEY_IMS_DTMF_TONE_DELAY_INT);
        }
    }

    @Override
    public int getNumberPresentation() {
        return mNumberPresentation;
    }

    @Override
    public UUSInfo getUUSInfo() {
        return mUusInfo;
    }

    @Override
    public Connection getOrigConnection() {
        return null;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public synchronized boolean isMultiparty() {
        return mImsCall != null && mImsCall.isMultiparty();
    }

    /**
     * Where {@link #isMultiparty()} is {@code true}, determines if this {@link ImsCall} is the
     * origin of the conference call (i.e. {@code #isConferenceHost()} is {@code true}), or if this
     * {@link ImsCall} is a member of a conference hosted on another device.
     *
     * @return {@code true} if this call is the origin of the conference call it is a member of,
     *      {@code false} otherwise.
     */
    @Override
    public synchronized boolean isConferenceHost() {
        return mImsCall != null && mImsCall.isConferenceHost();
    }

    @Override
    public boolean isMemberOfPeerConference() {
        return !isConferenceHost();
    }

    public synchronized ImsCall getImsCall() {
        return mImsCall;
    }

    public synchronized void setImsCall(ImsCall imsCall) {
        mImsCall = imsCall;
    }

    public void changeParent(ImsPhoneCall parent) {
        mParent = parent;
    }

    /**
     * @return {@code true} if the {@link ImsPhoneConnection} or its media capabilities have been
     *     changed, and {@code false} otherwise.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean update(ImsCall imsCall, ImsPhoneCall.State state) {
        if (state == ImsPhoneCall.State.ACTIVE) {
            // If the state of the call is active, but there is a pending request to the RIL to hold
            // the call, we will skip this update.  This is really a signalling delay or failure
            // from the RIL, but we will prevent it from going through as we will end up erroneously
            // making this call active when really it should be on hold.
            if (imsCall.isPendingHold()) {
                Rlog.w(LOG_TAG, "update : state is ACTIVE, but call is pending hold, skipping");
                return false;
            }

            if (mParent.getState().isRinging() || mParent.getState().isDialing()) {
                onConnectedInOrOut();
            }

            if (mParent.getState().isRinging() || mParent == mOwner.mBackgroundCall) {
                //mForegroundCall should be IDLE
                //when accepting WAITING call
                //before accept WAITING call,
                //the ACTIVE call should be held ahead
                mParent.detach(this);
                mParent = mOwner.mForegroundCall;
                mParent.attach(this);
            }
        } else if (state == ImsPhoneCall.State.HOLDING) {
            onStartedHolding();
        }

        boolean updateParent = mParent.update(this, imsCall, state);
        boolean updateAddressDisplay = updateAddressDisplay(imsCall);
        boolean updateMediaCapabilities = updateMediaCapabilities(imsCall);
        boolean updateExtras = updateExtras(imsCall);

        return updateParent || updateAddressDisplay || updateMediaCapabilities || updateExtras;
    }

    /**
     * Re-evaluate whether ringback should be playing.
     */
    public void maybeChangeRingbackState() {
        Rlog.i(LOG_TAG, "maybeChangeRingbackState");
        mParent.maybeChangeRingbackState(mImsCall);
    }

    @Override
    public int getPreciseDisconnectCause() {
        return mPreciseDisconnectCause;
    }

    public void setPreciseDisconnectCause(int cause) {
        mPreciseDisconnectCause = cause;
    }

    /**
     * Notifies this Connection of a request to disconnect a participant of the conference managed
     * by the connection.
     *
     * @param endpoint the {@link android.net.Uri} of the participant to disconnect.
     */
    @Override
    public void onDisconnectConferenceParticipant(Uri endpoint) {
        ImsCall imsCall = getImsCall();
        if (imsCall == null) {
            return;
        }
        try {
            imsCall.removeParticipants(new String[]{endpoint.toString()});
        } catch (ImsException e) {
            // No session in place -- no change
            Rlog.e(LOG_TAG, "onDisconnectConferenceParticipant: no session in place. "+
                    "Failed to disconnect endpoint = " + endpoint);
        }
    }

    /**
     * Sets the conference connect time.  Used when an {@code ImsConference} is created to out of
     * this phone connection.
     *
     * @param conferenceConnectTime The conference connect time.
     */
    public void setConferenceConnectTime(long conferenceConnectTime) {
        mConferenceConnectTime = conferenceConnectTime;
    }

    /**
     * @return The conference connect time.
     */
    public long getConferenceConnectTime() {
        return mConferenceConnectTime;
    }

    /**
     * Check for a change in the address display related fields for the {@link ImsCall}, and
     * update the {@link ImsPhoneConnection} with this information.
     *
     * @param imsCall The call to check for changes in address display fields.
     * @return Whether the address display fields have been changed.
     */
    public boolean updateAddressDisplay(ImsCall imsCall) {
        if (imsCall == null) {
            return false;
        }

        boolean changed = false;
        ImsCallProfile callProfile = imsCall.getCallProfile();
        if (callProfile != null && isIncoming()) {
            // Only look for changes to the address for incoming calls.  The originating identity
            // can change for outgoing calls due to, for example, a call being forwarded to
            // voicemail.  This address change does not need to be presented to the user.
            String address = callProfile.getCallExtra(ImsCallProfile.EXTRA_OI);
            String name = callProfile.getCallExtra(ImsCallProfile.EXTRA_CNA);
            int nump = ImsCallProfile.OIRToPresentation(
                    callProfile.getCallExtraInt(ImsCallProfile.EXTRA_OIR));
            int namep = ImsCallProfile.OIRToPresentation(
                    callProfile.getCallExtraInt(ImsCallProfile.EXTRA_CNAP));
            if (Phone.DEBUG_PHONE) {
                Rlog.d(LOG_TAG, "updateAddressDisplay: callId = " + getTelecomCallId()
                        + " address = " + Rlog.pii(LOG_TAG, address) + " name = "
                        + Rlog.pii(LOG_TAG, name) + " nump = " + nump + " namep = " + namep);
            }
            if (!mIsMergeInProcess) {
                // Only process changes to the name and address when a merge is not in process.
                // When call A initiated a merge with call B to form a conference C, there is a
                // point in time when the ImsCall transfers the conference call session into A,
                // at which point the ImsConferenceController creates the conference in Telecom.
                // For some carriers C will have a unique conference URI address.  Swapping the
                // conference session into A, which is about to be disconnected, to be logged to
                // the call log using the conference address.  To prevent this we suppress updates
                // to the call address while a merge is in process.
                if (!equalsBaseDialString(mAddress, address)) {
                    mAddress = address;
                    changed = true;
                }
                if (TextUtils.isEmpty(name)) {
                    if (!TextUtils.isEmpty(mCnapName)) {
                        mCnapName = "";
                        changed = true;
                    }
                } else if (!name.equals(mCnapName)) {
                    mCnapName = name;
                    changed = true;
                }
                if (mNumberPresentation != nump) {
                    mNumberPresentation = nump;
                    changed = true;
                }
                if (mCnapNamePresentation != namep) {
                    mCnapNamePresentation = namep;
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * Check for a change in the video capabilities and audio quality for the {@link ImsCall}, and
     * update the {@link ImsPhoneConnection} with this information.
     *
     * @param imsCall The call to check for changes in media capabilities.
     * @return Whether the media capabilities have been changed.
     */
    public boolean updateMediaCapabilities(ImsCall imsCall) {
        if (imsCall == null) {
            return false;
        }

        boolean changed = false;

        try {
            // The actual call profile (negotiated between local and peer).
            ImsCallProfile negotiatedCallProfile = imsCall.getCallProfile();

            if (negotiatedCallProfile != null) {
                int oldVideoState = getVideoState();
                int newVideoState = ImsCallProfile
                        .getVideoStateFromImsCallProfile(negotiatedCallProfile);

                if (oldVideoState != newVideoState) {
                    // The video state has changed.  See also code in onReceiveSessionModifyResponse
                    // below.  When the video enters a paused state, subsequent changes to the video
                    // state will not be reported by the modem.  In onReceiveSessionModifyResponse
                    // we will be updating the current video state while paused to include any
                    // changes the modem reports via the video provider.  When the video enters an
                    // unpaused state, we will resume passing the video states from the modem as is.
                    if (VideoProfile.isPaused(oldVideoState) &&
                            !VideoProfile.isPaused(newVideoState)) {
                        // Video entered un-paused state; recognize updates from now on; we want to
                        // ensure that the new un-paused state is propagated to Telecom, so change
                        // this now.
                        mShouldIgnoreVideoStateChanges = false;
                    }

                    if (!mShouldIgnoreVideoStateChanges) {
                        updateVideoState(newVideoState);
                        changed = true;
                    } else {
                        Rlog.d(LOG_TAG, "updateMediaCapabilities - ignoring video state change " +
                                "due to paused state.");
                    }

                    if (!VideoProfile.isPaused(oldVideoState) &&
                            VideoProfile.isPaused(newVideoState)) {
                        // Video entered pause state; ignore updates until un-paused.  We do this
                        // after setVideoState is called above to ensure Telecom is notified that
                        // the device has entered paused state.
                        mShouldIgnoreVideoStateChanges = true;
                    }
                }

                if (negotiatedCallProfile.mMediaProfile != null) {
                    mIsRttEnabledForCall = negotiatedCallProfile.mMediaProfile.isRttCall();

                    if (mIsRttEnabledForCall && mRttTextHandler == null) {
                        Rlog.d(LOG_TAG, "updateMediaCapabilities -- turning RTT on, profile="
                                + negotiatedCallProfile);
                        startRttTextProcessing();
                        onRttInitiated();
                        changed = true;
                        mOwner.getPhone().getVoiceCallSessionStats().onRttStarted(this);
                    } else if (!mIsRttEnabledForCall && mRttTextHandler != null) {
                        Rlog.d(LOG_TAG, "updateMediaCapabilities -- turning RTT off, profile="
                                + negotiatedCallProfile);
                        mRttTextHandler.tearDown();
                        mRttTextHandler = null;
                        mRttTextStream = null;
                        onRttTerminated();
                        changed = true;
                    }
                }
            }

            // Check for a change in the capabilities for the call and update
            // {@link ImsPhoneConnection} with this information.
            int capabilities = getConnectionCapabilities();

            // Use carrier config to determine if downgrading directly to audio-only is supported.
            if (mOwner.isCarrierDowngradeOfVtCallSupported()) {
                capabilities = addCapability(capabilities,
                        Connection.Capability.SUPPORTS_DOWNGRADE_TO_VOICE_REMOTE |
                                Capability.SUPPORTS_DOWNGRADE_TO_VOICE_LOCAL);
            } else {
                capabilities = removeCapability(capabilities,
                        Connection.Capability.SUPPORTS_DOWNGRADE_TO_VOICE_REMOTE |
                                Capability.SUPPORTS_DOWNGRADE_TO_VOICE_LOCAL);
            }

            // Get the current local call capabilities which might be voice or video or both.
            ImsCallProfile localCallProfile = imsCall.getLocalCallProfile();
            Rlog.v(LOG_TAG, "update localCallProfile=" + localCallProfile);
            if (localCallProfile != null) {
                capabilities = applyLocalCallCapabilities(localCallProfile, capabilities);
            }

            // Get the current remote call capabilities which might be voice or video or both.
            ImsCallProfile remoteCallProfile = imsCall.getRemoteCallProfile();
            Rlog.v(LOG_TAG, "update remoteCallProfile=" + remoteCallProfile);
            if (remoteCallProfile != null) {
                capabilities = applyRemoteCallCapabilities(remoteCallProfile, capabilities);
            }
            if (getConnectionCapabilities() != capabilities) {
                setConnectionCapabilities(capabilities);
                changed = true;
            }

            if (!mOwner.isViLteDataMetered()) {
                Rlog.v(LOG_TAG, "data is not metered");
            } else {
                if (mImsVideoCallProviderWrapper != null) {
                    mImsVideoCallProviderWrapper.setIsVideoEnabled(
                            hasCapabilities(Connection.Capability.SUPPORTS_VT_LOCAL_BIDIRECTIONAL));
                }
            }

            boolean mediaAttributesChanged = false;

            // Metrics for audio codec
            if (localCallProfile != null
                    && localCallProfile.mMediaProfile.mAudioQuality != mAudioCodec) {
                mAudioCodec = localCallProfile.mMediaProfile.mAudioQuality;
                mMetrics.writeAudioCodecIms(mOwner.mPhone.getPhoneId(), imsCall.getCallSession());
                mOwner.getPhone().getVoiceCallSessionStats().onAudioCodecChanged(this, mAudioCodec);
                changed = true;
                mediaAttributesChanged = true;
            }

            if (localCallProfile != null
                    && localCallProfile.mMediaProfile.getAudioCodecAttributes() != null) {
                AudioCodecAttributes audioCodecAttributes =
                        localCallProfile.mMediaProfile.getAudioCodecAttributes();

                if (Math.abs(mAudioCodecBitrateKbps
                        - audioCodecAttributes.getBitrateRangeKbps().getUpper()) > THRESHOLD) {
                    mAudioCodecBitrateKbps = audioCodecAttributes.getBitrateRangeKbps().getUpper();
                    changed = true;
                    mediaAttributesChanged = true;
                }
                if (Math.abs(mAudioCodecBandwidthKhz
                        - audioCodecAttributes.getBandwidthRangeKhz().getUpper()) > THRESHOLD) {
                    mAudioCodecBandwidthKhz =
                            audioCodecAttributes.getBandwidthRangeKhz().getUpper();
                    changed = true;
                    mediaAttributesChanged = true;
                }
            }

            if (mediaAttributesChanged) {
                Rlog.i(LOG_TAG, "updateMediaCapabilities: mediate attributes changed: codec = "
                        + mAudioCodec + ", bitRate=" + mAudioCodecBitrateKbps + ", bandwidth="
                        + mAudioCodecBandwidthKhz);
                notifyMediaAttributesChanged();
            }

            int newAudioQuality =
                    getAudioQualityFromCallProfile(localCallProfile, remoteCallProfile);
            if (getAudioQuality() != newAudioQuality) {
                setAudioQuality(newAudioQuality);
                changed = true;
            }
        } catch (ImsException e) {
            // No session in place -- no change
        }

        return changed;
    }

    private void updateVideoState(int newVideoState) {
        if (mImsVideoCallProviderWrapper != null) {
            mImsVideoCallProviderWrapper.onVideoStateChanged(newVideoState);
        }
        setVideoState(newVideoState);
        mOwner.getPhone().getVoiceCallSessionStats().onVideoStateChange(this, newVideoState);
    }


    /**
     * Send a RTT upgrade request to the remote party.
     * @param textStream RTT text stream to use
     */
    public void startRtt(android.telecom.Connection.RttTextStream textStream) {
        ImsCall imsCall = getImsCall();
        if (imsCall != null) {
            getImsCall().sendRttModifyRequest(true);
            setCurrentRttTextStream(textStream);
        }
    }

    /**
     * Terminate the current RTT session.
     */
    public void stopRtt() {
        getImsCall().sendRttModifyRequest(false);
    }

    /**
     * Sends the user's response to a remotely-issued RTT upgrade request
     *
     * @param textStream A valid {@link android.telecom.Connection.RttTextStream} if the user
     *                   accepts, {@code null} if not.
     */
    public void sendRttModifyResponse(android.telecom.Connection.RttTextStream textStream) {
        boolean accept = textStream != null;
        ImsCall imsCall = getImsCall();

        if (imsCall != null) {
            imsCall.sendRttModifyResponse(accept);
            if (accept) {
                setCurrentRttTextStream(textStream);
            } else {
                Rlog.e(LOG_TAG, "sendRttModifyResponse: foreground call has no connections");
            }
        }
    }

    public void onRttMessageReceived(String message) {
        synchronized (this) {
            if (mRttTextHandler == null) {
                Rlog.w(LOG_TAG, "onRttMessageReceived: RTT text handler not available."
                        + " Attempting to create one.");
                if (mRttTextStream == null) {
                    Rlog.e(LOG_TAG, "onRttMessageReceived:"
                            + " Unable to process incoming message. No textstream available");
                    return;
                }
                createRttTextHandler();
            }
        }
        mRttTextHandler.sendToInCall(message);
    }

    public void onRttAudioIndicatorChanged(ImsStreamMediaProfile profile) {
        Bundle extras = new Bundle();
        extras.putBoolean(android.telecom.Connection.EXTRA_IS_RTT_AUDIO_PRESENT,
                profile.isReceivingRttAudio());
        onConnectionEvent(android.telecom.Connection.EVENT_RTT_AUDIO_INDICATION_CHANGED,
                extras);
    }

    public void setCurrentRttTextStream(android.telecom.Connection.RttTextStream rttTextStream) {
        synchronized (this) {
            mRttTextStream = rttTextStream;
            if (mRttTextHandler == null && mIsRttEnabledForCall) {
                Rlog.i(LOG_TAG, "setCurrentRttTextStream: Creating a text handler");
                createRttTextHandler();
            }
        }
    }

    /**
     * Get the corresponding EmergencyNumberTracker associated with the connection.
     * @return the EmergencyNumberTracker
     */
    public EmergencyNumberTracker getEmergencyNumberTracker() {
        if (mOwner != null) {
            Phone phone = mOwner.getPhone();
            if (phone != null) {
                return phone.getEmergencyNumberTracker();
            }
        }
        return null;
    }

    public boolean hasRttTextStream() {
        return mRttTextStream != null;
    }

    public boolean isRttEnabledForCall() {
        return mIsRttEnabledForCall;
    }

    public void startRttTextProcessing() {
        synchronized (this) {
            if (mRttTextStream == null) {
                Rlog.w(LOG_TAG, "startRttTextProcessing: no RTT text stream. Ignoring.");
                return;
            }
            if (mRttTextHandler != null) {
                Rlog.w(LOG_TAG, "startRttTextProcessing: RTT text handler already exists");
                return;
            }
            createRttTextHandler();
        }
    }

    // Make sure to synchronize on ImsPhoneConnection.this before calling.
    private void createRttTextHandler() {
        mRttTextHandler = new ImsRttTextHandler(Looper.getMainLooper(),
                (message) -> {
                    ImsCall imsCall = getImsCall();
                    if (imsCall != null) {
                        imsCall.sendRttMessage(message);
                    }
                });
        mRttTextHandler.initialize(mRttTextStream);
    }

    /**
     * Updates the IMS call rat based on the {@link ImsCallProfile#EXTRA_CALL_RAT_TYPE}.
     *
     * @param extras The ImsCallProfile extras.
     */
    private void updateImsCallRatFromExtras(Bundle extras) {
        if (extras == null) {
            return;
        }
        if (extras.containsKey(ImsCallProfile.EXTRA_CALL_NETWORK_TYPE)
                || extras.containsKey(ImsCallProfile.EXTRA_CALL_RAT_TYPE)
                || extras.containsKey(ImsCallProfile.EXTRA_CALL_RAT_TYPE_ALT)) {

            ImsCall call = getImsCall();
            int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
            if (call != null) {
                networkType = call.getNetworkType();
            }

            // Report any changes for network type change
            setCallRadioTech(ServiceState.networkTypeToRilRadioTechnology(networkType));
        }
    }

    private void updateEmergencyCallFromExtras(Bundle extras) {
        if (extras == null) {
            return;
        }
        if (extras.getBoolean(ImsCallProfile.EXTRA_EMERGENCY_CALL)) {
            setIsNetworkIdentifiedEmergencyCall(true);
        }
    }

    private void updateForwardedNumberFromExtras(Bundle extras) {
        if (extras == null) {
            return;
        }
        if (extras.containsKey(ImsCallProfile.EXTRA_FORWARDED_NUMBER)) {
            String[] forwardedNumberArray =
                    extras.getStringArray(ImsCallProfile.EXTRA_FORWARDED_NUMBER);
            if (forwardedNumberArray != null) {
                mForwardedNumber = new ArrayList<String>(Arrays.asList(forwardedNumberArray));
            }
        }
    }

    /**
     * Check for a change in call extras of {@link ImsCall}, and
     * update the {@link ImsPhoneConnection} accordingly.
     *
     * @param imsCall The call to check for changes in extras.
     * @return Whether the extras fields have been changed.
     */
     boolean updateExtras(ImsCall imsCall) {
        if (imsCall == null) {
            return false;
        }

        final ImsCallProfile callProfile = imsCall.getCallProfile();
        final Bundle extras = callProfile != null ? callProfile.mCallExtras : null;
        if (extras == null && DBG) {
            Rlog.d(LOG_TAG, "Call profile extras are null.");
        }

        final boolean changed = !areBundlesEqual(extras, mExtras);
        if (changed) {
            updateImsCallRatFromExtras(extras);
            updateEmergencyCallFromExtras(extras);
            updateForwardedNumberFromExtras(extras);
            mExtras.clear();
            if (extras != null) {
                mExtras.putAll(extras);
            }
            setConnectionExtras(mExtras);
        }
        return changed;
    }

    private static boolean areBundlesEqual(Bundle extras, Bundle newExtras) {
        if (extras == null || newExtras == null) {
            return extras == newExtras;
        }

        if (extras.size() != newExtras.size()) {
            return false;
        }

        for(String key : extras.keySet()) {
            if (key != null) {
                final Object value = extras.get(key);
                final Object newValue = newExtras.get(key);
                if (!Objects.equals(value, newValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Determines the {@link ImsPhoneConnection} audio quality based on the local and remote
     * {@link ImsCallProfile}. Indicate a HD audio call if the local stream profile
     * is AMR_WB, EVRC_WB, EVS_WB, EVS_SWB, EVS_FB and
     * there is no remote restrict cause.
     *
     * @param localCallProfile The local call profile.
     * @param remoteCallProfile The remote call profile.
     * @return The audio quality.
     */
    private int getAudioQualityFromCallProfile(
            ImsCallProfile localCallProfile, ImsCallProfile remoteCallProfile) {
        if (localCallProfile == null || remoteCallProfile == null
                || localCallProfile.mMediaProfile == null) {
            return AUDIO_QUALITY_STANDARD;
        }

        final boolean isEvsCodecHighDef = (localCallProfile.mMediaProfile.mAudioQuality
                        == ImsStreamMediaProfile.AUDIO_QUALITY_EVS_WB
                || localCallProfile.mMediaProfile.mAudioQuality
                        == ImsStreamMediaProfile.AUDIO_QUALITY_EVS_SWB
                || localCallProfile.mMediaProfile.mAudioQuality
                        == ImsStreamMediaProfile.AUDIO_QUALITY_EVS_FB);

        final boolean isHighDef = (localCallProfile.mMediaProfile.mAudioQuality
                        == ImsStreamMediaProfile.AUDIO_QUALITY_AMR_WB
                || localCallProfile.mMediaProfile.mAudioQuality
                        == ImsStreamMediaProfile.AUDIO_QUALITY_EVRC_WB
                || isEvsCodecHighDef)
                && remoteCallProfile.getRestrictCause() == ImsCallProfile.CALL_RESTRICT_CAUSE_NONE;
        return isHighDef ? AUDIO_QUALITY_HIGH_DEFINITION : AUDIO_QUALITY_STANDARD;
    }

    /**
     * Provides a string representation of the {@link ImsPhoneConnection}.  Primarily intended for
     * use in log statements.
     *
     * @return String representation of call.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ImsPhoneConnection objId: ");
        sb.append(System.identityHashCode(this));
        sb.append(" telecomCallID: ");
        sb.append(getTelecomCallId());
        sb.append(" address: ");
        sb.append(Rlog.pii(LOG_TAG, getAddress()));
        sb.append(" isAdhocConf: ");
        sb.append(isAdhocConference() ? "Y" : "N");
        sb.append(" ImsCall: ");
        synchronized (this) {
            if (mImsCall == null) {
                sb.append("null");
            } else {
                sb.append(mImsCall);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public void setVideoProvider(android.telecom.Connection.VideoProvider videoProvider) {
        super.setVideoProvider(videoProvider);

        if (videoProvider instanceof ImsVideoCallProviderWrapper) {
            mImsVideoCallProviderWrapper = (ImsVideoCallProviderWrapper) videoProvider;
        }
    }

    /**
     * Indicates whether current phone connection is emergency or not
     * @return boolean: true if emergency, false otherwise
     */
    protected boolean isEmergency() {
        return mIsEmergency;
    }

    protected boolean isWpsCall() {
        return mIsWpsCall;
    }

    /**
     * Indicates whether current phone connection is cross sim calling or not
     * @return boolean: true if cross sim calling, false otherwise
     */
    public boolean isCrossSimCall() {
        return mImsCall != null && mImsCall.isCrossSimCall();
    }

    /**
     * Handles notifications from the {@link ImsVideoCallProviderWrapper} of session modification
     * responses received.
     *
     * @param status The status of the original request.
     * @param requestProfile The requested video profile.
     * @param responseProfile The response upon video profile.
     */
    @Override
    public void onReceiveSessionModifyResponse(int status, VideoProfile requestProfile,
            VideoProfile responseProfile) {
        if (status == android.telecom.Connection.VideoProvider.SESSION_MODIFY_REQUEST_SUCCESS &&
                mShouldIgnoreVideoStateChanges) {
            int currentVideoState = getVideoState();
            int newVideoState = responseProfile.getVideoState();

            // If the current video state is paused, the modem will not send us any changes to
            // the TX and RX bits of the video state.  Until the video is un-paused we will
            // "fake out" the video state by applying the changes that the modem reports via a
            // response.

            // First, find out whether there was a change to the TX or RX bits:
            int changedBits = currentVideoState ^ newVideoState;
            changedBits &= VideoProfile.STATE_BIDIRECTIONAL;
            if (changedBits == 0) {
                // No applicable change, bail out.
                return;
            }

            // Turn off any existing bits that changed.
            currentVideoState &= ~(changedBits & currentVideoState);
            // Turn on any new bits that turned on.
            currentVideoState |= changedBits & newVideoState;

            Rlog.d(LOG_TAG, "onReceiveSessionModifyResponse : received " +
                    VideoProfile.videoStateToString(requestProfile.getVideoState()) +
                    " / " +
                    VideoProfile.videoStateToString(responseProfile.getVideoState()) +
                    " while paused ; sending new videoState = " +
                    VideoProfile.videoStateToString(currentVideoState));
            setVideoState(currentVideoState);
        }
    }

    /**
     * Issues a request to pause the video using {@link VideoProfile#STATE_PAUSED} from a source
     * other than the InCall UI.
     *
     * @param source The source of the pause request.
     */
    public void pauseVideo(int source) {
        if (mImsVideoCallProviderWrapper == null) {
            return;
        }

        mImsVideoCallProviderWrapper.pauseVideo(getVideoState(), source);
    }

    /**
     * Issues a request to resume the video using {@link VideoProfile#STATE_PAUSED} from a source
     * other than the InCall UI.
     *
     * @param source The source of the resume request.
     */
    public void resumeVideo(int source) {
        if (mImsVideoCallProviderWrapper == null) {
            return;
        }

        mImsVideoCallProviderWrapper.resumeVideo(getVideoState(), source);
    }

    /**
     * Determines if a specified source has issued a pause request.
     *
     * @param source The source.
     * @return {@code true} if the source issued a pause request, {@code false} otherwise.
     */
    public boolean wasVideoPausedFromSource(int source) {
        if (mImsVideoCallProviderWrapper == null) {
            return false;
        }

        return mImsVideoCallProviderWrapper.wasVideoPausedFromSource(source);
    }

    /**
     * Mark the call as in the process of being merged and inform the UI of the merge start.
     */
    public void handleMergeStart() {
        mIsMergeInProcess = true;
        onConnectionEvent(android.telecom.Connection.EVENT_MERGE_START, null);
    }

    /**
     * Mark the call as done merging and inform the UI of the merge start.
     */
    public void handleMergeComplete() {
        mIsMergeInProcess = false;
        onConnectionEvent(android.telecom.Connection.EVENT_MERGE_COMPLETE, null);
    }

    public void changeToPausedState() {
        int newVideoState = getVideoState() | VideoProfile.STATE_PAUSED;
        Rlog.i(LOG_TAG, "ImsPhoneConnection: changeToPausedState - setting paused bit; "
                + "newVideoState=" + VideoProfile.videoStateToString(newVideoState));
        updateVideoState(newVideoState);
        mShouldIgnoreVideoStateChanges = true;
    }

    public void changeToUnPausedState() {
        int newVideoState = getVideoState() & ~VideoProfile.STATE_PAUSED;
        Rlog.i(LOG_TAG, "ImsPhoneConnection: changeToUnPausedState - unsetting paused bit; "
                + "newVideoState=" + VideoProfile.videoStateToString(newVideoState));
        updateVideoState(newVideoState);
        mShouldIgnoreVideoStateChanges = false;
    }

    public void setLocalVideoCapable(boolean isVideoEnabled) {
        mIsLocalVideoCapable = isVideoEnabled;
        Rlog.i(LOG_TAG, "setLocalVideoCapable: mIsLocalVideoCapable = " + mIsLocalVideoCapable
                + "; updating local video availability.");
        updateMediaCapabilities(getImsCall());
    }

    /**
     * Sends RTP header extension data.
     * @param rtpHeaderExtensions the RTP header extension data to send.
     */
    public void sendRtpHeaderExtensions(@NonNull Set<RtpHeaderExtension> rtpHeaderExtensions) {
        if (mImsCall == null) {
            Rlog.w(LOG_TAG, "sendRtpHeaderExtensions: Not an IMS call");
            return;
        }
        Rlog.i(LOG_TAG, "sendRtpHeaderExtensions: numExtensions = " + rtpHeaderExtensions.size());
        mImsCall.sendRtpHeaderExtensions(rtpHeaderExtensions);
    }

    /**
     * @return the RTP header extensions accepted for this call.
     */
    public Set<RtpHeaderExtensionType> getAcceptedRtpHeaderExtensions() {
        if (mImsCall == null || mImsCall.getCallProfile() == null) {
            return Collections.EMPTY_SET;
        }
        return mImsCall.getCallProfile().getAcceptedRtpHeaderExtensionTypes();
    }

    /**
     * For a connection being disconnected, sets the {@link ImsReasonInfo} which describes the
     * reason for the disconnection.
     * @param imsReasonInfo The IMS reason info.
     */
    public void setImsReasonInfo(@Nullable ImsReasonInfo imsReasonInfo) {
        mImsReasonInfo = imsReasonInfo;
    }

    /**
     * @return the {@link ImsReasonInfo} describing why this connection disconnected, or
     * {@code null} otherwise.
     */
    public @Nullable ImsReasonInfo getImsReasonInfo() {
        return mImsReasonInfo;
    }

    /**
     * Converts an {@link ImsCallProfile} verification status to a
     * {@link android.telecom.Connection} verification status.
     * @param verificationStatus The {@link ImsCallProfile} verification status.
     * @return The telecom verification status.
     */
    public static @android.telecom.Connection.VerificationStatus int toTelecomVerificationStatus(
            @ImsCallProfile.VerificationStatus int verificationStatus) {
        switch (verificationStatus) {
            case ImsCallProfile.VERIFICATION_STATUS_PASSED:
                return android.telecom.Connection.VERIFICATION_STATUS_PASSED;
            case ImsCallProfile.VERIFICATION_STATUS_FAILED:
                return android.telecom.Connection.VERIFICATION_STATUS_FAILED;
            case ImsCallProfile.VERIFICATION_STATUS_NOT_VERIFIED:
                // fall through on purpose
            default:
                return android.telecom.Connection.VERIFICATION_STATUS_NOT_VERIFIED;
        }
    }

    /**
     * The priority of the call to the user. A higher number means higher priority.
     */
    protected int getCallPriority() {
        if (isEmergency()) {
            return 2;
        } else if (isWpsCall()) {
            return 1;
        }
        return 0;
    }
}
