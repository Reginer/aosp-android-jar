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

import android.annotation.NonNull;
import android.compat.annotation.UnsupportedAppUsage;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.DisconnectCause;
import android.telephony.ServiceState;
import android.telephony.ServiceState.RilRadioTechnology;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.ims.RtpHeaderExtension;
import android.util.Log;

import com.android.ims.internal.ConferenceParticipant;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.emergency.EmergencyNumberTracker;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * {@hide}
 */
public abstract class Connection {
    private static final String TAG = "Connection";

    public static final String ADHOC_CONFERENCE_ADDRESS = "tel:conf-factory";

    public interface PostDialListener {
        void onPostDialWait();
        void onPostDialChar(char c);
    }

    /**
     * Capabilities that will be mapped to telecom connection
     * capabilities.
     */
    public static class Capability {

        /**
         * For an IMS video call, indicates that the local side of the call supports downgrading
         * from a video call to an audio-only call.
         */
        public static final int SUPPORTS_DOWNGRADE_TO_VOICE_LOCAL = 0x00000001;

        /**
         * For an IMS video call, indicates that the peer supports downgrading to an audio-only
         * call.
         */
        public static final int SUPPORTS_DOWNGRADE_TO_VOICE_REMOTE = 0x00000002;

        /**
         * For an IMS call, indicates that the call supports video locally.
         */
        public static final int SUPPORTS_VT_LOCAL_BIDIRECTIONAL = 0x00000004;

        /**
         * For an IMS call, indicates that the peer supports video.
         */
        public static final int SUPPORTS_VT_REMOTE_BIDIRECTIONAL = 0x00000008;

        /**
         * Indicates that the connection is an external connection (e.g. an instance of the class
         * {@link com.android.internal.telephony.imsphone.ImsExternalConnection}.
         */
        public static final int IS_EXTERNAL_CONNECTION = 0x00000010;

        /**
         * Indicates that this external connection can be pulled from the remote device to the
         * local device.
         */
        public static final int IS_PULLABLE = 0x00000020;

        /**
         * For an IMS call, indicates that the peer supports RTT.
         */
        public static final int SUPPORTS_RTT_REMOTE = 0x00000040;
    }

    /**
     * Listener interface for events related to the connection which should be reported to the
     * {@link android.telecom.Connection}.
     */
    public interface Listener {
        public void onVideoStateChanged(int videoState);
        public void onConnectionCapabilitiesChanged(int capability);
        public void onCallRadioTechChanged(@RilRadioTechnology int vrat);
        public void onVideoProviderChanged(
                android.telecom.Connection.VideoProvider videoProvider);
        public void onAudioQualityChanged(int audioQuality);
        public void onMediaAttributesChanged();
        public void onConferenceParticipantsChanged(List<ConferenceParticipant> participants);
        public void onCallSubstateChanged(int callSubstate);
        public void onMultipartyStateChanged(boolean isMultiParty);
        public void onConferenceMergedFailed();
        public void onExtrasChanged(Bundle extras);
        public void onExitedEcmMode();
        public void onCallPullFailed(Connection externalConnection);
        public void onHandoverToWifiFailed();
        public void onConnectionEvent(String event, Bundle extras);
        public void onRttModifyRequestReceived();
        public void onRttModifyResponseReceived(int status);
        public void onDisconnect(int cause);
        public void onRttInitiated();
        public void onRttTerminated();
        public void onOriginalConnectionReplaced(Connection newConnection);
        public void onIsNetworkEmergencyCallChanged(boolean isEmergencyCall);

        /**
         * Indicates a DTMF digit has been received from the network.
         * @param digit The DTMF digit.
         */
        public void onReceivedDtmfDigit(char digit);

        /**
         * Indicates data from an RTP header extension has been received from the network.
         * @param extensionData The extension data.
         */
        public void onReceivedRtpHeaderExtensions(@NonNull Set<RtpHeaderExtension> extensionData);
    }

    /**
     * Base listener implementation.
     */
    public abstract static class ListenerBase implements Listener {
        @Override
        public void onVideoStateChanged(int videoState) {}
        @Override
        public void onConnectionCapabilitiesChanged(int capability) {}
        @Override
        public void onCallRadioTechChanged(@RilRadioTechnology int vrat) {}
        @Override
        public void onVideoProviderChanged(
                android.telecom.Connection.VideoProvider videoProvider) {}
        @Override
        public void onAudioQualityChanged(int audioQuality) {}
        @Override
        public void onMediaAttributesChanged() {}
        @Override
        public void onConferenceParticipantsChanged(List<ConferenceParticipant> participants) {}
        @Override
        public void onCallSubstateChanged(int callSubstate) {}
        @Override
        public void onMultipartyStateChanged(boolean isMultiParty) {}
        @Override
        public void onConferenceMergedFailed() {}
        @Override
        public void onExtrasChanged(Bundle extras) {}
        @Override
        public void onExitedEcmMode() {}
        @Override
        public void onCallPullFailed(Connection externalConnection) {}
        @Override
        public void onHandoverToWifiFailed() {}
        @Override
        public void onConnectionEvent(String event, Bundle extras) {}
        @Override
        public void onRttModifyRequestReceived() {}
        @Override
        public void onRttModifyResponseReceived(int status) {}
        @Override
        public void onDisconnect(int cause) {}
        @Override
        public void onRttInitiated() {}
        @Override
        public void onRttTerminated() {}
        @Override
        public void onOriginalConnectionReplaced(Connection newConnection) {}
        @Override
        public void onIsNetworkEmergencyCallChanged(boolean isEmergencyCall) {}
        @Override
        public void onReceivedDtmfDigit(char digit) {}
        @Override
        public void onReceivedRtpHeaderExtensions(@NonNull Set<RtpHeaderExtension> extensionData) {}
    }

    public static final int AUDIO_QUALITY_STANDARD = 1;
    public static final int AUDIO_QUALITY_HIGH_DEFINITION = 2;
    // the threshold used to compare mAudioCodecBitrateKbps and mAudioCodecBandwidth.
    public static final float THRESHOLD = 0.01f;

    /**
     * The telecom internal call ID associated with this connection.  Only to be used for debugging
     * purposes.
     */
    private String mTelecomCallId;

    //Caller Name Display
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected String mCnapName;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected int mCnapNamePresentation  = PhoneConstants.PRESENTATION_ALLOWED;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected String mAddress;     // MAY BE NULL!!!
    // The VERSTAT number verification status; defaults to not verified.
    protected @android.telecom.Connection.VerificationStatus int mNumberVerificationStatus =
            android.telecom.Connection.VERIFICATION_STATUS_NOT_VERIFIED;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected String mDialString;          // outgoing calls only
    protected String[] mParticipantsToDial;// outgoing calls only
    protected boolean mIsAdhocConference;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected int mNumberPresentation = PhoneConstants.PRESENTATION_ALLOWED;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected boolean mIsIncoming;
    /*
     * These time/timespan values are based on System.currentTimeMillis(),
     * i.e., "wall clock" time.
     */
    protected long mCreateTime;
    protected long mConnectTime;
    /*
     * These time/timespan values are based on SystemClock.elapsedRealTime(),
     * i.e., time since boot.  They are appropriate for comparison and
     * calculating deltas.
     */
    protected long mConnectTimeReal;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected long mDuration;
    protected long mHoldingStartTime;  // The time when the Connection last transitioned
                            // into HOLDING
    protected Connection mOrigConnection;
    private List<PostDialListener> mPostDialListeners = new ArrayList<>();
    public Set<Listener> mListeners = new CopyOnWriteArraySet<>();

    protected boolean mNumberConverted = false;
    protected String mConvertedNumber;

    protected ArrayList<String> mForwardedNumber = null; //May be null. Incoming calls only.

    protected String mPostDialString;      // outgoing calls only
    protected int mNextPostDialChar;       // index into postDialString

    protected int mCause = DisconnectCause.NOT_DISCONNECTED;
    protected PostDialState mPostDialState = PostDialState.NOT_STARTED;

    // Store the current audio code
    protected int mAudioCodec;
    // audio codec bitrate in kbps
    protected float mAudioCodecBitrateKbps;
    // audio codec bandwidth in kHz
    protected float mAudioCodecBandwidthKhz;

    @UnsupportedAppUsage
    private static String LOG_TAG = "Connection";

    Object mUserData;
    private int mVideoState;
    private int mConnectionCapabilities;
    /**
     * Determines the call radio technology for current connection.
     *
     * This is used to propagate the call radio technology to upper layer.
     */
    private @RilRadioTechnology int mCallRadioTech =
            ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
    private boolean mAudioModeIsVoip;
    private int mAudioQuality;
    private int mCallSubstate;
    private android.telecom.Connection.VideoProvider mVideoProvider;
    public Call.State mPreHandoverState = Call.State.IDLE;
    private Bundle mExtras;
    private int mPhoneType;
    private boolean mAnsweringDisconnectsActiveCall;
    private boolean mAllowAddCallDuringVideoCall;
    private boolean mAllowHoldingVideoCall;

    private boolean mIsEmergencyCall;

    /**
     * The emergency number information, only valid if {@link #isEmergencyCall} returns
     * {@code true}.
     */
    private EmergencyNumber mEmergencyNumberInfo;

    /**
     * Whether the call is from emergency dialer, only valid if {@link #isEmergencyCall} returns
     * {@code true}.
     */
    private boolean mHasKnownUserIntentEmergency;

    /**
     * When {@code true}, the network has indicated that this is an emergency call.
     */
    private boolean mIsNetworkIdentifiedEmergencyCall;

    /**
     * Used to indicate that this originated from pulling a {@link android.telecom.Connection} with
     * {@link android.telecom.Connection#PROPERTY_IS_EXTERNAL_CALL}.
     */
    private boolean mIsPulledCall = false;

    /**
     * Where {@link #mIsPulledCall} is {@code true}, contains the dialog Id of the external call
     * which is being pulled (e.g.
     * {@link com.android.internal.telephony.imsphone.ImsExternalConnection#getCallId()}).
     */
    private int mPulledDialogId;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected Connection(int phoneType) {
        mPhoneType = phoneType;
    }

    /* Instance Methods */

    /**
     * @return The telecom internal call ID associated with this connection.  Only to be used for
     * debugging purposes.
     */
    public String getTelecomCallId() {
        return mTelecomCallId;
    }

    /**
     * Sets the telecom call ID associated with this connection.
     *
     * @param telecomCallId The telecom call ID.
     */
    public void setTelecomCallId(String telecomCallId) {
        mTelecomCallId = telecomCallId;
    }

    /**
     * Gets address (e.g. phone number) associated with connection.
     * TODO: distinguish reasons for unavailability
     *
     * @return address or null if unavailable
     */

    @UnsupportedAppUsage
    public String getAddress() {
        return mAddress;
    }

    /**
     * Gets the participants address (e.g. phone number) associated with connection.
     *
     * @return address or null if unavailable
     */
    public String[] getParticipantsToDial() {
        return mParticipantsToDial;
    }

    // return whether connection is AdhocConference or not
    public boolean isAdhocConference() {
        return mIsAdhocConference;
    }

    /**
     * Gets redirecting address (e.g. phone number) associated with connection.
     *
     * @return ArrayList of the forwarded number or null if unavailable
     */
    public ArrayList<String> getForwardedNumber() {
        return mForwardedNumber;
    }

    /**
     * Gets CNAP name associated with connection.
     * @return cnap name or null if unavailable
     */
    public String getCnapName() {
        return mCnapName;
    }

    /**
     * Get original dial string.
     * @return original dial string or null if unavailable
     */
    public String getOrigDialString(){
        return null;
    }

    /**
     * Get the number, as set by {@link #restoreDialedNumberAfterConversion(String)}.
     * @return The converted number.
     */
    @VisibleForTesting
    public String getConvertedNumber() {
        return mConvertedNumber;
    }

    /**
     * Gets CNAP presentation associated with connection.
     * @return cnap name or null if unavailable
     */

    public int getCnapNamePresentation() {
       return mCnapNamePresentation;
    }

    /**
     * @return Call that owns this Connection, or null if none
     */
    @UnsupportedAppUsage
    public abstract Call getCall();

    /**
     * Connection create time in currentTimeMillis() format
     * Basically, set when object is created.
     * Effectively, when an incoming call starts ringing or an
     * outgoing call starts dialing
     */
    @UnsupportedAppUsage
    public long getCreateTime() {
        return mCreateTime;
    }

    /**
     * Connection connect time in currentTimeMillis() format.
     * For outgoing calls: Begins at (DIALING|ALERTING) -> ACTIVE transition.
     * For incoming calls: Begins at (INCOMING|WAITING) -> ACTIVE transition.
     * Returns 0 before then.
     */
    @UnsupportedAppUsage
    public long getConnectTime() {
        return mConnectTime;
    }

    /**
     * Sets the Connection connect time in currentTimeMillis() format.
     *
     * @param connectTime the new connect time.
     */
    public void setConnectTime(long connectTime) {
        mConnectTime = connectTime;
    }

    /**
     * Sets the Connection connect time in {@link SystemClock#elapsedRealtime()} format.
     *
     * @param connectTimeReal the new connect time.
     */
    public void setConnectTimeReal(long connectTimeReal) {
        mConnectTimeReal = connectTimeReal;
    }

    /**
     * Connection connect time in elapsedRealtime() format.
     * For outgoing calls: Begins at (DIALING|ALERTING) -> ACTIVE transition.
     * For incoming calls: Begins at (INCOMING|WAITING) -> ACTIVE transition.
     * Returns 0 before then.
     */
    public long getConnectTimeReal() {
        return mConnectTimeReal;
    }

    /**
     * Disconnect time in currentTimeMillis() format.
     * The time when this Connection makes a transition into ENDED or FAIL.
     * Returns 0 before then.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public abstract long getDisconnectTime();

    /**
     * Returns the number of milliseconds the call has been connected,
     * or 0 if the call has never connected.
     * If the call is still connected, then returns the elapsed
     * time since connect.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public long getDurationMillis() {
        if (mConnectTimeReal == 0) {
            return 0;
        } else if (mDuration == 0) {
            return SystemClock.elapsedRealtime() - mConnectTimeReal;
        } else {
            return mDuration;
        }
    }

    /**
     * The time when this Connection last transitioned into HOLDING
     * in elapsedRealtime() format.
     * Returns 0, if it has never made a transition into HOLDING.
     */
    public long getHoldingStartTime() {
        return mHoldingStartTime;
    }

    /**
     * If this connection is HOLDING, return the number of milliseconds
     * that it has been on hold for (approximately).
     * If this connection is in any other state, return 0.
     */

    public abstract long getHoldDurationMillis();

    /**
     * Returns call disconnect cause. Values are defined in
     * {@link android.telephony.DisconnectCause}. If the call is not yet
     * disconnected, NOT_DISCONNECTED is returned.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int getDisconnectCause() {
        return mCause;
    }

    /**
     * Returns a string disconnect cause which is from vendor.
     * Vendors may use this string to explain the underline causes of failed calls.
     * There is no guarantee that it is non-null nor it'll have meaningful stable values.
     * Only use it when getDisconnectCause() returns a value that is not specific enough, like
     * ERROR_UNSPECIFIED.
     */
    public abstract String getVendorDisconnectCause();

    /**
     * Returns true of this connection originated elsewhere
     * ("MT" or mobile terminated; another party called this terminal)
     * or false if this call originated here (MO or mobile originated).
     */
    @UnsupportedAppUsage
    public boolean isIncoming() {
        return mIsIncoming;
    }

    /**
     * Sets whether this call is an incoming call or not.
     * @param isIncoming {@code true} if the call is an incoming call, {@code false} if it is an
     *                               outgoing call.
     */
    public void setIsIncoming(boolean isIncoming) {
        mIsIncoming = isIncoming;
    }

    /**
     * Checks if the connection is for an emergency call.
     *
     * @return {@code true} if the call is an emergency call
     *         or {@code false} otherwise.
     */
    public boolean isEmergencyCall() {
        return mIsEmergencyCall;
    }

    /**
     * Get the emergency number info. The value is valid only if {@link #isEmergencyCall()}
     * returns {@code true}.
     *
     * @return the emergency number info
     */
    public EmergencyNumber getEmergencyNumberInfo() {
        return mEmergencyNumberInfo;
    }

    /**
     * Checks if we have known the user's intent for the call is emergency.
     *
     * This is only used to specify when the dialed number is ambiguous, identified as both
     * emergency number and any other non-emergency number; e.g. in some situation, 611 could
     * be both an emergency number in a country and a non-emergency number of a carrier's
     * customer service hotline.
     *
     * @return whether the call is from emergency dialer
     */
    public boolean hasKnownUserIntentEmergency() {
        return mHasKnownUserIntentEmergency;
    }

    /**
     * Set the emergency number information if it is an emergency call.
     *
     * @hide
     */
    public void setEmergencyCallInfo(CallTracker ct) {
        if (ct != null) {
            Phone phone = ct.getPhone();
            if (phone != null) {
                EmergencyNumberTracker tracker = phone.getEmergencyNumberTracker();
                if (tracker != null) {
                    EmergencyNumber num = tracker.getEmergencyNumber(mAddress);
                    if (num != null) {
                        mIsEmergencyCall = true;
                        mEmergencyNumberInfo = num;
                    } else {
                        Rlog.e(TAG, "setEmergencyCallInfo: emergency number is null");
                    }
                } else {
                    Rlog.e(TAG, "setEmergencyCallInfo: emergency number tracker is null");
                }
            } else {
                Rlog.e(TAG, "setEmergencyCallInfo: phone is null");
            }
        } else {
            Rlog.e(TAG, "setEmergencyCallInfo: call tracker is null");
        }
    }

    /**
     * Set the non-detectable emergency number information.
     */
    public void setNonDetectableEmergencyCallInfo(int eccCategory) {
        if (!mIsEmergencyCall) {
            mIsEmergencyCall = true;
            mEmergencyNumberInfo = new EmergencyNumber(mAddress, ""/*countryIso*/,
                                    ""/*mnc*/, eccCategory,
                                    new ArrayList<String>(),
                                    EmergencyNumber.EMERGENCY_NUMBER_SOURCE_NETWORK_SIGNALING,
                                    EmergencyNumber.EMERGENCY_CALL_ROUTING_UNKNOWN);
        }
    }

    /**
     * Set if we have known the user's intent for the call is emergency.
     *
     * This is only used to specify when the dialed number is ambiguous, identified as both
     * emergency number and any other non-emergency number; e.g. in some situation, 611 could
     * be both an emergency number in a country and a non-emergency number of a carrier's
     * customer service hotline.
     *
     * @hide
     */
    public void setHasKnownUserIntentEmergency(boolean hasKnownUserIntentEmergency) {
        mHasKnownUserIntentEmergency = hasKnownUserIntentEmergency;
    }

    /**
     * If this Connection is connected, then it is associated with
     * a Call.
     *
     * Returns getCall().getState() or Call.State.IDLE if not
     * connected
     */
    @UnsupportedAppUsage
    public Call.State getState() {
        Call c;

        c = getCall();

        if (c == null) {
            return Call.State.IDLE;
        } else {
            return c.getState();
        }
    }

    /**
     * If this connection went through handover return the state of the
     * call that contained this connection before handover.
     */
    public Call.State getStateBeforeHandover() {
        return mPreHandoverState;
   }

    /**
     * Get the details of conference participants. Expected to be
     * overwritten by the Connection subclasses.
     */
    public List<ConferenceParticipant> getConferenceParticipants() {
        Call c;

        c = getCall();

        if (c == null) {
            return null;
        } else {
            return c.getConferenceParticipants();
        }
    }

    /**
     * isAlive()
     *
     * @return true if the connection isn't disconnected
     * (could be active, holding, ringing, dialing, etc)
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean
    isAlive() {
        return getState().isAlive();
    }

    /**
     * Returns true if Connection is connected and is INCOMING or WAITING
     */
    public boolean
    isRinging() {
        return getState().isRinging();
    }

    /**
     *
     * @return the userdata set in setUserData()
     */
    @UnsupportedAppUsage
    public Object getUserData() {
        return mUserData;
    }

    /**
     *
     * @param userdata user can store an any userdata in the Connection object.
     */
    public void setUserData(Object userdata) {
        mUserData = userdata;
    }

    /**
     * Deflect individual Connection
     */
    public abstract void deflect(String number) throws CallStateException;

    /**
     * Transfer individual Connection
     */
    public abstract void transfer(String number, boolean isConfirmationRequired)
            throws CallStateException;

    /**
     * Transfer individual Connection for consultative transfer
     */
    public abstract void consultativeTransfer(Connection other) throws CallStateException;

    /**
     * Hangup individual Connection
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public abstract void hangup() throws CallStateException;

    /**
     * Separate this call from its owner Call and assigns it to a new Call
     * (eg if it is currently part of a Conference call
     * TODO: Throw exception? Does GSM require error display on failure here?
     */
    public abstract void separate() throws CallStateException;

    public enum PostDialState {
        @UnsupportedAppUsage
        NOT_STARTED,    /* The post dial string playback hasn't
                           been started, or this call is not yet
                           connected, or this is an incoming call */
        @UnsupportedAppUsage
        STARTED,        /* The post dial string playback has begun */
        @UnsupportedAppUsage
        WAIT,           /* The post dial string playback is waiting for a
                           call to proceedAfterWaitChar() */
        @UnsupportedAppUsage
        WILD,           /* The post dial string playback is waiting for a
                           call to proceedAfterWildChar() */
        @UnsupportedAppUsage
        COMPLETE,       /* The post dial string playback is complete */
        @UnsupportedAppUsage
        CANCELLED,       /* The post dial string playback was cancelled
                           with cancelPostDial() */
        PAUSE           /* The post dial string playback is pausing for a
                           call to processNextPostDialChar*/
    }

    public void clearUserData(){
        mUserData = null;
    }

    public void addPostDialListener(PostDialListener listener) {
        if (!mPostDialListeners.contains(listener)) {
            mPostDialListeners.add(listener);
        }
    }

    public final void removePostDialListener(PostDialListener listener) {
        mPostDialListeners.remove(listener);
    }

    protected final void clearPostDialListeners() {
        if (mPostDialListeners != null) {
            mPostDialListeners.clear();
        }
    }

    protected final void notifyPostDialListeners() {
        if (getPostDialState() == PostDialState.WAIT) {
            for (PostDialListener listener : new ArrayList<>(mPostDialListeners)) {
                listener.onPostDialWait();
            }
        }
    }

    protected final void notifyPostDialListenersNextChar(char c) {
        for (PostDialListener listener : new ArrayList<>(mPostDialListeners)) {
            listener.onPostDialChar(c);
        }
    }

    public PostDialState getPostDialState() {
        return mPostDialState;
    }

    /**
     * Returns the portion of the post dial string that has not
     * yet been dialed, or "" if none
     */
    public String getRemainingPostDialString() {
        if (mPostDialState == PostDialState.CANCELLED
                || mPostDialState == PostDialState.COMPLETE
                || mPostDialString == null
                || mPostDialString.length() <= mNextPostDialChar) {
            return "";
        }

        return mPostDialString.substring(mNextPostDialChar);
    }

    /**
     * See Phone.setOnPostDialWaitCharacter()
     */

    public abstract void proceedAfterWaitChar();

    /**
     * See Phone.setOnPostDialWildCharacter()
     */
    public abstract void proceedAfterWildChar(String str);
    /**
     * Cancel any post
     */
    public abstract void cancelPostDial();

    /** Called when the connection has been disconnected */
    public boolean onDisconnect(int cause) {
        return false;
    }

    /**
     * Returns the caller id presentation type for incoming and waiting calls
     * @return one of PRESENTATION_*
     */
    public abstract int getNumberPresentation();

    /**
     * Returns the User to User Signaling (UUS) information associated with
     * incoming and waiting calls
     * @return UUSInfo containing the UUS userdata.
     */
    public abstract UUSInfo getUUSInfo();

    /**
     * Returns the CallFail reason provided by the RIL with the result of
     * RIL_REQUEST_LAST_CALL_FAIL_CAUSE
     */
    public abstract int getPreciseDisconnectCause();

    /**
     * Returns the original Connection instance associated with
     * this Connection
     */
    public Connection getOrigConnection() {
        return mOrigConnection;
    }

    /**
     * Returns whether the original ImsPhoneConnection was a member
     * of a conference call
     * @return valid only when getOrigConnection() is not null
     */
    public abstract boolean isMultiparty();

    /**
     * Applicable only for IMS Call. Determines if this call is the origin of the conference call
     * (i.e. {@code #isConferenceHost()} is {@code true}), or if it is a member of a conference
     * hosted on another device.
     *
     * @return {@code true} if this call is the origin of the conference call it is a member of,
     *      {@code false} otherwise.
     */
    public boolean isConferenceHost() {
        return false;
    }

    /**
     * Applicable only for IMS Call. Determines if a connection is a member of a conference hosted
     * on another device.
     *
     * @return {@code true} if the connection is a member of a conference hosted on another device.
     */
    public boolean isMemberOfPeerConference() {
        return false;
    }

    public void migrateFrom(Connection c) {
        if (c == null) return;
        mListeners = c.mListeners;
        mDialString = c.getOrigDialString();
        mCreateTime = c.getCreateTime();
        mConnectTime = c.getConnectTime();
        mConnectTimeReal = c.getConnectTimeReal();
        mHoldingStartTime = c.getHoldingStartTime();
        mOrigConnection = c.getOrigConnection();
        mPostDialString = c.mPostDialString;
        mNextPostDialChar = c.mNextPostDialChar;
        mPostDialState = c.mPostDialState;

        // Migrate Emergency call parameters
        mIsEmergencyCall = c.isEmergencyCall();
        mEmergencyNumberInfo = c.getEmergencyNumberInfo();
        mHasKnownUserIntentEmergency = c.hasKnownUserIntentEmergency();
    }

    /**
     * Assign a listener to be notified of state changes.
     *
     * @param listener A listener.
     */
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    /**
     * Removes a listener.
     *
     * @param listener A listener.
     */
    public final void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Returns the current video state of the connection.
     *
     * @return The video state of the connection.
     */
    public int getVideoState() {
        return mVideoState;
    }

    /**
     * Called to get Connection capabilities.Returns Capabilities bitmask.
     * @See Connection.Capability.
     */
    public int getConnectionCapabilities() {
        return mConnectionCapabilities;
    }

    /**
     * @return {@code} true if the connection has the specified capabilities.
     */
    public boolean hasCapabilities(int connectionCapabilities) {
        return (mConnectionCapabilities & connectionCapabilities) == connectionCapabilities;
    }

    /**
     * Applies a capability to a capabilities bit-mask.
     *
     * @param capabilities The capabilities bit-mask.
     * @param capability The capability to apply.
     * @return The capabilities bit-mask with the capability applied.
     */
    public static int addCapability(int capabilities, int capability) {
        return capabilities | capability;
    }

    /**
     * Removes a capability to a capabilities bit-mask.
     *
     * @param capabilities The capabilities bit-mask.
     * @param capability The capability to remove.
     * @return The capabilities bit-mask with the capability removed.
     */
    public static int removeCapability(int capabilities, int capability) {
        return capabilities & ~capability;
    }

    /**
     * Returns whether the connection is using a wifi network.
     *
     * @return {@code True} if the connection is using a wifi network.
     */
    public boolean isWifi() {
        return getCallRadioTech() == ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN;
    }

    /**
     * Returns radio technology is used for the connection.
     *
     * @return the RIL Voice Radio Technology used for current connection,
     *         see {@code RIL_RADIO_TECHNOLOGY_*} in {@link android.telephony.ServiceState}.
     */
    public @RilRadioTechnology int getCallRadioTech() {
        return mCallRadioTech;
    }

    /**
     * Returns whether the connection uses voip audio mode
     *
     * @return {@code True} if the connection uses voip audio mode
     */
    public boolean getAudioModeIsVoip() {
        return mAudioModeIsVoip;
    }

    /**
     * Returns the {@link android.telecom.Connection.VideoProvider} for the connection.
     *
     * @return The {@link android.telecom.Connection.VideoProvider}.
     */
    public android.telecom.Connection.VideoProvider getVideoProvider() {
        return mVideoProvider;
    }

    /**
     * Returns the audio-quality for the connection.
     *
     * @return The audio quality for the connection.
     */
    public int getAudioQuality() {
        return mAudioQuality;
    }


    /**
     * Returns the current call substate of the connection.
     *
     * @return The call substate of the connection.
     */
    public int getCallSubstate() {
        return mCallSubstate;
    }


    /**
     * Sets the videoState for the current connection and reports the changes to all listeners.
     * Valid video states are defined in {@link android.telecom.VideoProfile}.
     *
     * @return The video state.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void setVideoState(int videoState) {
        mVideoState = videoState;
        for (Listener l : mListeners) {
            l.onVideoStateChanged(mVideoState);
        }
    }

    /**
     * Called to set Connection capabilities.  This will take Capabilities bitmask as input which is
     * converted from Capabilities constants.
     *
     * @See Connection.Capability.
     * @param capabilities The Capabilities bitmask.
     */
    public void setConnectionCapabilities(int capabilities) {
        if (mConnectionCapabilities != capabilities) {
            mConnectionCapabilities = capabilities;
            for (Listener l : mListeners) {
                l.onConnectionCapabilitiesChanged(mConnectionCapabilities);
            }
        }
    }

    /**
     * Sets RIL voice radio technology used for current connection.
     *
     * @param vrat the RIL voice radio technology for current connection,
     *             see {@code RIL_RADIO_TECHNOLOGY_*} in {@link android.telephony.ServiceState}.
     */
    public void setCallRadioTech(@RilRadioTechnology int vrat) {
        if (mCallRadioTech == vrat) {
            return;
        }
        mCallRadioTech = vrat;
        for (Listener l : mListeners) {
            l.onCallRadioTechChanged(vrat);
        }
    }

    /**
     * Set the voip audio mode for the connection
     *
     * @param isVoip {@code True} if voip audio mode is being used.
     */
    public void setAudioModeIsVoip(boolean isVoip) {
        mAudioModeIsVoip = isVoip;
    }

    /**
     * Set the audio quality for the connection.
     *
     * @param audioQuality The audio quality.
     */
    public void setAudioQuality(int audioQuality) {
        mAudioQuality = audioQuality;
        for (Listener l : mListeners) {
            l.onAudioQualityChanged(mAudioQuality);
        }
    }

    /**
     * Notifies interested parties of changes to the media attributes of the call.
     */
    public void notifyMediaAttributesChanged() {
        for (Listener l: mListeners) {
            l.onMediaAttributesChanged();
        }
    }

    /**
     * Notifies listeners that connection extras has changed.
     * @param extras New connection extras. This Bundle will be cloned to ensure that any concurrent
     * modifications to the extras Bundle do not affect Bundle operations in the onExtrasChanged
     * listeners.
     */
    public void setConnectionExtras(Bundle extras) {
        if (extras != null) {
            mExtras = new Bundle(extras);

            int previousCount = mExtras.size();
            // Prevent vendors from passing in extras other than primitive types and android API
            // parcelables.
            mExtras = TelephonyUtils.filterValues(mExtras);
            int filteredCount = mExtras.size();
            if (filteredCount != previousCount) {
                Rlog.i(TAG, "setConnectionExtras: filtering " + (previousCount - filteredCount)
                        + " invalid extras.");
            }
        } else {
            mExtras = null;
        }

        for (Listener l : mListeners) {
            l.onExtrasChanged(mExtras);
        }
    }

    /**
     * Retrieves the current connection extras.
     * @return the connection extras.
     */
    public Bundle getConnectionExtras() {
        return mExtras == null ? null : new Bundle(mExtras);
    }

    /**
     * @return {@code true} if answering the call will cause the current active call to be
     *      disconnected, {@code false} otherwise.
     */
    public boolean isActiveCallDisconnectedOnAnswer() {
        return mAnsweringDisconnectsActiveCall;
    }

    /**
     * Sets whether answering this call will cause the active call to be disconnected.
     * <p>
     * Should only be set {@code true} if there is an active call and this call is ringing.
     *
     * @param answeringDisconnectsActiveCall {@code true} if answering the call will call the active
     *      call to be disconnected.
     */
    public void setActiveCallDisconnectedOnAnswer(boolean answeringDisconnectsActiveCall) {
        mAnsweringDisconnectsActiveCall = answeringDisconnectsActiveCall;
    }

    public boolean shouldAllowAddCallDuringVideoCall() {
        return mAllowAddCallDuringVideoCall;
    }

    public void setAllowAddCallDuringVideoCall(boolean allowAddCallDuringVideoCall) {
        mAllowAddCallDuringVideoCall = allowAddCallDuringVideoCall;
    }

    public boolean shouldAllowHoldingVideoCall() {
        return mAllowHoldingVideoCall;
    }

    public void setAllowHoldingVideoCall(boolean allowHoldingVideoCall) {
        mAllowHoldingVideoCall = allowHoldingVideoCall;
    }

    /**
     * Sets whether the connection is the result of an external call which was pulled to the local
     * device.
     *
     * @param isPulledCall {@code true} if this connection is the result of pulling an external call
     *      to the local device.
     */
    public void setIsPulledCall(boolean isPulledCall) {
        mIsPulledCall = isPulledCall;
    }

    public boolean isPulledCall() {
        return mIsPulledCall;
    }

    /**
     * For an external call which is being pulled (e.g. {@link #isPulledCall()} is {@code true}),
     * sets the dialog Id for the external call.  Used to handle failures to pull a call so that the
     * pulled call can be reconciled with its original external connection.
     *
     * @param pulledDialogId The dialog id associated with a pulled call.
     */
    public void setPulledDialogId(int pulledDialogId) {
        mPulledDialogId = pulledDialogId;
    }

    public int getPulledDialogId() {
        return mPulledDialogId;
    }

    /**
     * Sets the call substate for the current connection and reports the changes to all listeners.
     * Valid call substates are defined in {@link android.telecom.Connection}.
     *
     * @return The call substate.
     */
    public void setCallSubstate(int callSubstate) {
        mCallSubstate = callSubstate;
        for (Listener l : mListeners) {
            l.onCallSubstateChanged(mCallSubstate);
        }
    }

    /**
     * Sets the {@link android.telecom.Connection.VideoProvider} for the connection.
     *
     * @param videoProvider The video call provider.
     */
    public void setVideoProvider(android.telecom.Connection.VideoProvider videoProvider) {
        mVideoProvider = videoProvider;
        for (Listener l : mListeners) {
            l.onVideoProviderChanged(mVideoProvider);
        }
    }

    /**
     * {@link CallTracker#convertNumberIfNecessary(Phone, String)} can be used to convert a dialed
     * number to another number based on carrier config.  This is used where a carrier wishes to
     * redirect certain short codes such as *55 to another number (e.g. a 1-800 service number).
     * The {@link CallTracker} sub-classes call
     * {@link CallTracker#convertNumberIfNecessary(Phone, String)} to retrieve the newly converted
     * number and instantiate the {@link Connection} instance using the converted number so that the
     * system will dial out the substitution number instead of the originally dialed one.  This gem
     * of a method is called after the dialing process to restore the originally dialed number and
     * keep track of the fact that a converted number was used to place the call.
     * @param oriNumber The original number prior to conversion.
     */
    public void restoreDialedNumberAfterConversion(String oriNumber) {
        mNumberConverted = true;
        mConvertedNumber = mAddress;
        mAddress = oriNumber;
        mDialString = oriNumber;
    }

    /**
     * Changes the address and presentation for this call.
     * @param newAddress The new address.
     * @param numberPresentation The number presentation for the address.
     */
    public void setAddress(String newAddress, int numberPresentation) {
        Rlog.i(TAG, "setAddress = " + newAddress);
        mAddress = newAddress;
        mNumberPresentation = numberPresentation;
    }

    public void setDialString(String newDialString) {
        mDialString = newDialString;
    }

    /**
     * Notifies listeners of a change to conference participant(s).
     *
     * @param conferenceParticipants The participant(s).
     */
    public void updateConferenceParticipants(List<ConferenceParticipant> conferenceParticipants) {
        for (Listener l : mListeners) {
            l.onConferenceParticipantsChanged(conferenceParticipants);
        }
    }

    /**
     * Notifies listeners of a change to the multiparty state of the connection.
     *
     * @param isMultiparty The participant(s).
     */
    public void updateMultipartyState(boolean isMultiparty) {
        for (Listener l : mListeners) {
            l.onMultipartyStateChanged(isMultiparty);
        }
    }

    /**
     * Notifies listeners of a failure in merging this connection with the background connection.
     */
    public void onConferenceMergeFailed() {
        for (Listener l : mListeners) {
            l.onConferenceMergedFailed();
        }
    }

    /**
     * Notifies that the underlying phone has exited ECM mode.
     */
    public void onExitedEcmMode() {
        for (Listener l : mListeners) {
            l.onExitedEcmMode();
        }
    }

    /**
     * Notifies the connection that a call to {@link #pullExternalCall()} has failed to pull the
     * call to the local device.
     *
     * @param externalConnection The original
     *      {@link com.android.internal.telephony.imsphone.ImsExternalConnection} from which the
     *      pull was initiated.
     */
    public void onCallPullFailed(Connection externalConnection) {
        for (Listener l : mListeners) {
            l.onCallPullFailed(externalConnection);
        }
    }

    public void onOriginalConnectionReplaced(Connection newConnection) {
        for (Listener l : mListeners) {
            l.onOriginalConnectionReplaced(newConnection);
        }
    }
    /**
     * Notifies the connection that there was a failure while handing over to WIFI.
     */
    public void onHandoverToWifiFailed() {
        for (Listener l : mListeners) {
            l.onHandoverToWifiFailed();
        }
    }

    /**
     * Notifies the connection of a connection event.
     */
    public void onConnectionEvent(String event, Bundle extras) {
        for (Listener l : mListeners) {
            l.onConnectionEvent(event, extras);
        }
    }

    /**
     * Notifies this Connection of a request to disconnect a participant of the conference managed
     * by the connection.
     *
     * @param endpoint the {@link Uri} of the participant to disconnect.
     */
    public void onDisconnectConferenceParticipant(Uri endpoint) {
    }

    /**
     * Called by a {@link android.telecom.Connection} to indicate that this call should be pulled
     * to the local device.
     */
    public void pullExternalCall() {
    }

    public void onRttModifyRequestReceived() {
        for (Listener l : mListeners) {
            l.onRttModifyRequestReceived();
        }
    }

    public void onRttModifyResponseReceived(int status) {
        for (Listener l : mListeners) {
            l.onRttModifyResponseReceived(status);
        }
    }

    public void onRttInitiated() {
        for (Listener l : mListeners) {
            l.onRttInitiated();
        }
    }

    public void onRttTerminated() {
        for (Listener l : mListeners) {
            l.onRttTerminated();
        }
    }
    /**
     * Notify interested parties that this connection disconnected.
     * {@code TelephonyConnection}, for example, uses this.
     * @param reason the disconnect code, per {@link DisconnectCause}.
     */
    protected void notifyDisconnect(int reason) {
        Rlog.i(TAG, "notifyDisconnect: callId=" + getTelecomCallId() + ", reason=" + reason);
        for (Listener l : mListeners) {
            l.onDisconnect(reason);
        }
    }

    /**
     *
     */
    public int getPhoneType() {
        return mPhoneType;
    }

    /**
     * Reset the Connection time and Duration
     */
    public void resetConnectionTime() {
        if (mPhoneType == PhoneConstants.PHONE_TYPE_CDMA_LTE ||
                mPhoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            mConnectTime = System.currentTimeMillis();
            mConnectTimeReal = SystemClock.elapsedRealtime();
            mDuration = 0;
        }
    }

    /**
     * Sets whether this {@link Connection} has been identified by the network as an emergency call.
     * @param isNetworkIdentifiedEmergencyCall {@code true} if ecall, {@code false} otherwise.
     */
    public void setIsNetworkIdentifiedEmergencyCall(boolean isNetworkIdentifiedEmergencyCall) {
        mIsNetworkIdentifiedEmergencyCall = isNetworkIdentifiedEmergencyCall;
        for (Listener l : mListeners) {
            l.onIsNetworkEmergencyCallChanged(isNetworkIdentifiedEmergencyCall);
        }
    }

    /**
     * @return Whether this {@link Connection} has been identified by the network as an emergency
     * call.
     */
    public boolean isNetworkIdentifiedEmergencyCall() {
        return mIsNetworkIdentifiedEmergencyCall;
    }

    /**
     * Build a human representation of a connection instance, suitable for debugging.
     * Don't log personal stuff unless in debug mode.
     * @return a string representing the internal state of this connection.
     */
    public String toString() {
        StringBuilder str = new StringBuilder(128);

        str.append(" callId: " + getTelecomCallId());
        str.append(" objId: " + System.identityHashCode(this));
        str.append(" isExternal: " + (((mConnectionCapabilities & Capability.IS_EXTERNAL_CONNECTION)
                == Capability.IS_EXTERNAL_CONNECTION) ? "Y" : "N"));
        if (Rlog.isLoggable(LOG_TAG, Log.DEBUG)) {
            str.append("addr: " + getAddress())
                    .append(" pres.: " + getNumberPresentation())
                    .append(" dial: " + getOrigDialString())
                    .append(" postdial: " + getRemainingPostDialString())
                    .append(" cnap name: " + getCnapName())
                    .append("(" + getCnapNamePresentation() + ")");
        }
        str.append(" incoming: " + isIncoming())
                .append(" state: " + getState())
                .append(" post dial state: " + getPostDialState());
        return str.toString();
    }

    /**
     * Get current audio codec.
     * @return current audio codec.
     */
    public int getAudioCodec() {
        return mAudioCodec;
    }

    /**
     * @return the audio codec bitrate in kbps.
     */
    public float getAudioCodecBitrateKbps() {
        return mAudioCodecBitrateKbps;
    }

    /**
     * @return the audio codec bandwidth in kHz.
     */
    public float getAudioCodecBandwidthKhz() {
        return mAudioCodecBandwidthKhz;
    }

    /**
     * @return The number verification status; only applicable for IMS calls.
     */
    public @android.telecom.Connection.VerificationStatus int getNumberVerificationStatus() {
        return mNumberVerificationStatus;
    }

    /**
     * Sets the number verification status.
     * @param verificationStatus The new verification status
     */
    public void setNumberVerificationStatus(
            @android.telecom.Connection.VerificationStatus int verificationStatus) {
        mNumberVerificationStatus = verificationStatus;
    }

    /**
     * Called to report a DTMF digit received from the network.
     * @param digit the received digit.
     */
    public void receivedDtmfDigit(char digit) {
        for (Listener l : mListeners) {
            l.onReceivedDtmfDigit(digit);
        }
    }

    /**
     * Called to report RTP header extensions received from the network.
     * @param extensionData the received extension data.
     */
    public void receivedRtpHeaderExtensions(@NonNull Set<RtpHeaderExtension> extensionData) {
        for (Listener l : mListeners) {
            l.onReceivedRtpHeaderExtensions(extensionData);
        }
    }
}
