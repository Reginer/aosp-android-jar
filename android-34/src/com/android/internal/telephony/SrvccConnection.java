/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.telephony.PreciseCallState.PRECISE_CALL_STATE_ACTIVE;
import static android.telephony.PreciseCallState.PRECISE_CALL_STATE_ALERTING;
import static android.telephony.PreciseCallState.PRECISE_CALL_STATE_DIALING;
import static android.telephony.PreciseCallState.PRECISE_CALL_STATE_HOLDING;
import static android.telephony.PreciseCallState.PRECISE_CALL_STATE_INCOMING;
import static android.telephony.PreciseCallState.PRECISE_CALL_STATE_INCOMING_SETUP;
import static android.telephony.PreciseCallState.PRECISE_CALL_STATE_WAITING;

import android.net.Uri;
import android.telephony.Annotation.PreciseCallStates;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsStreamMediaProfile;
import android.text.TextUtils;

import com.android.ims.internal.ConferenceParticipant;
import com.android.internal.telephony.imsphone.ImsPhoneConnection;
import com.android.telephony.Rlog;

/**
 * Connection information for SRVCC
 */
public class SrvccConnection {
    private static final String TAG = "SrvccConnection";

    public static final int CALL_TYPE_NORMAL = 0;
    public static final int CALL_TYPE_EMERGENCY = 1;

    public static final int SUBSTATE_NONE = 0;
    /** Pre-alerting state. Applicable for MT calls only */
    public static final int SUBSTATE_PREALERTING = 1;

    public static final int TONE_NONE = 0;
    public static final int TONE_LOCAL = 1;
    public static final int TONE_NETWORK = 2;

    /** Values are CALL_TYPE_ */
    private int mType = CALL_TYPE_NORMAL;

    /** Values are Call.State */
    private Call.State mState;

    /** Values are SUBSTATE_ */
    private int mSubstate = SUBSTATE_NONE;

    /** Values are TONE_ */
    private int mRingbackToneType = TONE_NONE;

    /** true if it is a multi-party call */
    private boolean mIsMpty = false;

    /** true if it is a mobile terminated call */
    private boolean mIsMT;

    /** Remote party nummber */
    private String mNumber;

    /** Values are PhoneConstants.PRESENTATION_ */
    private int mNumPresentation;

    /** Remote party name */
    private String mName;

    /** Values are PhoneConstants.PRESENTATION_ */
    private int mNamePresentation;

    public SrvccConnection(ImsCallProfile profile,
            ImsPhoneConnection c, @PreciseCallStates int preciseCallState) {
        mState = toCallState(preciseCallState);
        if (mState == Call.State.ALERTING) {
            mRingbackToneType = isLocalTone(profile) ? TONE_LOCAL : TONE_NETWORK;
        }
        if (preciseCallState == PRECISE_CALL_STATE_INCOMING_SETUP) {
            mSubstate = SUBSTATE_PREALERTING;
        }

        if (c == null) {
            initialize(profile);
        } else {
            initialize(c);
        }
    }

    public SrvccConnection(ConferenceParticipant cp, @PreciseCallStates int preciseCallState) {
        Rlog.d(TAG, "initialize with ConferenceParticipant");
        mState = toCallState(preciseCallState);
        mIsMT = cp.getCallDirection() == android.telecom.Call.Details.DIRECTION_INCOMING;
        mNumber = getParticipantAddress(cp.getHandle());
        mNumPresentation = cp.getParticipantPresentation();
        if (mNumPresentation == PhoneConstants.PRESENTATION_RESTRICTED) {
            mNumber = "";
        }
        mName = cp.getDisplayName();
        if (!TextUtils.isEmpty(mName)) {
            mNamePresentation = PhoneConstants.PRESENTATION_ALLOWED;
        } else {
            mNamePresentation = PhoneConstants.PRESENTATION_UNKNOWN;
        }
        mIsMpty = true;
    }

    private static String getParticipantAddress(Uri address) {
        if (address == null) {
            return null;
        }

        String number = address.getSchemeSpecificPart();
        if (TextUtils.isEmpty(number)) {
            return null;
        }

        String[] numberParts = number.split("[@;:]");
        if (numberParts.length == 0) return null;

        return numberParts[0];
    }

    // MT call in alerting or prealerting state
    private void initialize(ImsCallProfile profile) {
        Rlog.d(TAG, "initialize with ImsCallProfile");
        mIsMT = true;
        mNumber = profile.getCallExtra(ImsCallProfile.EXTRA_OI);
        mName = profile.getCallExtra(ImsCallProfile.EXTRA_CNA);
        mNumPresentation = ImsCallProfile.OIRToPresentation(
                profile.getCallExtraInt(ImsCallProfile.EXTRA_OIR));
        mNamePresentation = ImsCallProfile.OIRToPresentation(
                profile.getCallExtraInt(ImsCallProfile.EXTRA_CNAP));
    }

    private void initialize(ImsPhoneConnection c) {
        Rlog.d(TAG, "initialize with ImsPhoneConnection");
        if (c.isEmergencyCall()) {
            mType = CALL_TYPE_EMERGENCY;
        }
        mIsMT = c.isIncoming();
        mNumber = c.getAddress();
        mNumPresentation = c.getNumberPresentation();
        mName = c.getCnapName();
        mNamePresentation = c.getCnapNamePresentation();
    }

    private boolean isLocalTone(ImsCallProfile profile) {
        if (profile == null) return false;

        ImsStreamMediaProfile mediaProfile = profile.getMediaProfile();
        if (mediaProfile == null) return false;

        boolean shouldPlayRingback =
                (mediaProfile.getAudioDirection() == ImsStreamMediaProfile.DIRECTION_INACTIVE)
                        ? true : false;
        return shouldPlayRingback;
    }

    private static Call.State toCallState(int preciseCallState) {
        switch (preciseCallState) {
            case PRECISE_CALL_STATE_ACTIVE: return Call.State.ACTIVE;
            case PRECISE_CALL_STATE_HOLDING: return Call.State.HOLDING;
            case PRECISE_CALL_STATE_DIALING: return Call.State.DIALING;
            case PRECISE_CALL_STATE_ALERTING: return Call.State.ALERTING;
            case PRECISE_CALL_STATE_INCOMING: return Call.State.INCOMING;
            case PRECISE_CALL_STATE_WAITING: return Call.State.WAITING;
            case PRECISE_CALL_STATE_INCOMING_SETUP: return Call.State.INCOMING;
            default:
        }
        return Call.State.DISCONNECTED;
    }

    /** Returns the type of the call */
    public int getType() {
        return mType;
    }

    /** Returns the state */
    public Call.State getState() {
        return mState;
    }

    /** Updates the state */
    public void setState(Call.State state) {
        mState = state;
    }

    /** Returns the sub state */
    public int getSubState() {
        return mSubstate;
    }

    /** Returns the ringback tone type */
    public int getRingbackToneType() {
        return mRingbackToneType;
    }

    /** true if it is a multi-party call */
    public boolean isMultiParty() {
        return mIsMpty;
    }

    /** true if it is a mobile terminated call */
    public boolean isIncoming() {
        return mIsMT;
    }

    /** Returns the remote party nummber */
    public String getNumber() {
        return mNumber;
    }

    /** Returns the number presentation */
    public int getNumberPresentation() {
        return mNumPresentation;
    }

    /** Returns the remote party name */
    public String getName() {
        return mName;
    }

    /** Returns the name presentation */
    public int getNamePresentation() {
        return mNamePresentation;
    }

    /**
     * Build a human representation of a connection instance, suitable for debugging.
     * Don't log personal stuff unless in debug mode.
     * @return a string representing the internal state of this connection.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" type:").append(getType());
        sb.append(", state:").append(getState());
        sb.append(", subState:").append(getSubState());
        sb.append(", toneType:").append(getRingbackToneType());
        sb.append(", mpty:").append(isMultiParty());
        sb.append(", incoming:").append(isIncoming());
        sb.append(", numberPresentation:").append(getNumberPresentation());
        sb.append(", number:").append(Rlog.pii(TAG, getNumber()));
        sb.append(", namePresentation:").append(getNamePresentation());
        sb.append(", name:").append(Rlog.pii(TAG, getName()));
        return sb.toString();
    }
}
