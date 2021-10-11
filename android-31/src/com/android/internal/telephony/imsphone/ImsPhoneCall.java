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

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.telephony.DisconnectCause;
import android.telephony.ims.ImsStreamMediaProfile;
import android.util.Log;

import com.android.ims.ImsCall;
import com.android.ims.ImsException;
import com.android.ims.internal.ConferenceParticipant;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.List;

/**
 * {@hide}
 */
public class ImsPhoneCall extends Call {
    private static final String LOG_TAG = "ImsPhoneCall";

    // This flag is meant to be used as a debugging tool to quickly see all logs
    // regardless of the actual log level set on this component.
    private static final boolean FORCE_DEBUG = false; /* STOPSHIP if true */
    private static final boolean DBG = FORCE_DEBUG || Rlog.isLoggable(LOG_TAG, Log.DEBUG);
    private static final boolean VDBG = FORCE_DEBUG || Rlog.isLoggable(LOG_TAG, Log.VERBOSE);

    /*************************** Instance Variables **************************/
    public static final String CONTEXT_UNKNOWN = "UK";
    public static final String CONTEXT_RINGING = "RG";
    public static final String CONTEXT_FOREGROUND = "FG";
    public static final String CONTEXT_BACKGROUND = "BG";
    public static final String CONTEXT_HANDOVER = "HO";

    /*package*/ ImsPhoneCallTracker mOwner;

    private boolean mIsRingbackTonePlaying = false;

    // Determines what type of ImsPhoneCall this is.  ImsPhoneCallTracker uses instances of
    // ImsPhoneCall to for fg, bg, etc calls.  This is used as a convenience for logging so that it
    // can be made clear whether a call being logged is the foreground, background, etc.
    private final String mCallContext;

    /****************************** Constructors *****************************/
    /*package*/
    ImsPhoneCall() {
        mCallContext = CONTEXT_UNKNOWN;
    }

    public ImsPhoneCall(ImsPhoneCallTracker owner, String context) {
        mOwner = owner;
        mCallContext = context;
    }

    public void dispose() {
        try {
            mOwner.hangup(this);
        } catch (CallStateException ex) {
            //Rlog.e(LOG_TAG, "dispose: unexpected error on hangup", ex);
            //while disposing, ignore the exception and clean the connections
        } finally {
            List<Connection> connections = getConnections();
            for (Connection conn : connections) {
                conn.onDisconnect(DisconnectCause.LOST_SIGNAL);
            }
        }
    }

    /************************** Overridden from Call *************************/

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public ArrayList<Connection> getConnections() {
        return super.getConnections();
    }

    @Override
    public Phone
    getPhone() {
        return mOwner.getPhone();
    }

    @Override
    public boolean
    isMultiparty() {
        ImsCall imsCall = getImsCall();
        if (imsCall == null) {
            return false;
        }

        return imsCall.isMultiparty();
    }

    /** Please note: if this is the foreground call and a
     *  background call exists, the background call will be resumed.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void
    hangup() throws CallStateException {
        mOwner.hangup(this);
    }

    @Override
    public void hangup(@android.telecom.Call.RejectReason int rejectReason)
            throws CallStateException {
        mOwner.hangup(this, rejectReason);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<Connection> connections = getConnections();
        sb.append("[ImsPhoneCall ");
        sb.append(mCallContext);
        sb.append(" state: ");
        sb.append(mState.toString());
        sb.append(" ");
        if (connections.size() > 1) {
            sb.append(" ERROR_MULTIPLE ");
        }
        for (Connection conn : connections) {
            sb.append(conn);
            sb.append(" ");
        }

        sb.append("]");
        return sb.toString();
    }

    @Override
    public List<ConferenceParticipant> getConferenceParticipants() {
         if (!mOwner.isConferenceEventPackageEnabled()) {
             return null;
         }
         ImsCall call = getImsCall();
         if (call == null) {
             return null;
         }
         return call.getConferenceParticipants();
    }

    //***** Called from ImsPhoneConnection

    public void attach(Connection conn) {
        if (VDBG) {
            Rlog.v(LOG_TAG, "attach : " + mCallContext + " conn = " + conn);
        }
        clearDisconnected();
        addConnection(conn);

        mOwner.logState();
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void attach(Connection conn, State state) {
        if (VDBG) {
            Rlog.v(LOG_TAG, "attach : " + mCallContext + " state = " +
                    state.toString());
        }
        this.attach(conn);
        mState = state;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void attachFake(Connection conn, State state) {
        attach(conn, state);
    }

    /**
     * Called by ImsPhoneConnection when it has disconnected
     */
    public boolean connectionDisconnected(ImsPhoneConnection conn) {
        if (mState != State.DISCONNECTED) {
            /* If only disconnected connections remain, we are disconnected*/

            boolean hasOnlyDisconnectedConnections = true;

            ArrayList<Connection> connections = getConnections();
            for (Connection cn : connections) {
                if (cn.getState() != State.DISCONNECTED) {
                    hasOnlyDisconnectedConnections = false;
                    break;
                }
            }

            if (hasOnlyDisconnectedConnections) {
                synchronized(this) {
                    mState = State.DISCONNECTED;
                }
                if (VDBG) {
                    Rlog.v(LOG_TAG, "connectionDisconnected : " + mCallContext + " state = " +
                            mState);
                }
                return true;
            }
        }

        return false;
    }

    public void detach(ImsPhoneConnection conn) {
        if (VDBG) {
            Rlog.v(LOG_TAG, "detach : " + mCallContext + " conn = " + conn);
        }
        removeConnection(conn);
        clearDisconnected();

        mOwner.logState();
    }

    /**
     * @return true if there's no space in this call for additional
     * connections to be added via "conference"
     */
    /*package*/ boolean
    isFull() {
        return getConnectionsCount() == ImsPhoneCallTracker.MAX_CONNECTIONS_PER_CALL;
    }

    //***** Called from ImsPhoneCallTracker
    /**
     * Called when this Call is being hung up locally (eg, user pressed "end")
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @VisibleForTesting
    public void onHangupLocal() {
        ArrayList<Connection> connections = getConnections();
        for (Connection conn : connections) {
            ImsPhoneConnection imsConn = (ImsPhoneConnection) conn;
            imsConn.onHangupLocal();
        }
        synchronized(this) {
            if (mState.isAlive()) {
                mState = State.DISCONNECTING;
            }
        }
        if (VDBG) {
            Rlog.v(LOG_TAG, "onHangupLocal : " + mCallContext + " state = " + mState);
        }
    }

    @VisibleForTesting
    public ImsPhoneConnection getFirstConnection() {
        List<Connection> connections = getConnections();
        if (connections.size() == 0) return null;

        return (ImsPhoneConnection) connections.get(0);
    }

    /**
     * Sets the mute state of the call.
     * @param mute {@code true} if the call could be muted; {@code false} otherwise.
     */
    @VisibleForTesting
    public void setMute(boolean mute) {
        ImsPhoneConnection connection = getFirstConnection();
        ImsCall imsCall = connection == null ? null : connection.getImsCall();
        if (imsCall != null) {
            try {
                imsCall.setMute(mute);
            } catch (ImsException e) {
                Rlog.e(LOG_TAG, "setMute failed : " + e.getMessage());
            }
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    /* package */ void
    merge(ImsPhoneCall that, State state) {
        // This call is the conference host and the "that" call is the one being merged in.
        // Set the connect time for the conference; this will have been determined when the
        // conference was initially created.
        ImsPhoneConnection imsPhoneConnection = getFirstConnection();
        if (imsPhoneConnection != null) {
            long conferenceConnectTime = imsPhoneConnection.getConferenceConnectTime();
            if (conferenceConnectTime > 0) {
                imsPhoneConnection.setConnectTime(conferenceConnectTime);
                imsPhoneConnection.setConnectTimeReal(imsPhoneConnection.getConnectTimeReal());
            } else {
                if (DBG) {
                    Rlog.d(LOG_TAG, "merge: conference connect time is 0");
                }
            }
        }
        if (DBG) {
            Rlog.d(LOG_TAG, "merge(" + mCallContext + "): " + that + "state = "
                    + state);
        }
    }

    /**
     * Retrieves the {@link ImsCall} for the current {@link ImsPhoneCall}.
     * <p>
     * Marked as {@code VisibleForTesting} so that the
     * {@link com.android.internal.telephony.TelephonyTester} class can inject a test conference
     * event package into a regular ongoing IMS call.
     *
     * @return The {@link ImsCall}.
     */
    @VisibleForTesting
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public ImsCall getImsCall() {
        ImsPhoneConnection connection = getFirstConnection();
        return (connection == null) ? null : connection.getImsCall();
    }

    /*package*/ static boolean isLocalTone(ImsCall imsCall) {
        if ((imsCall == null) || (imsCall.getCallProfile() == null)
                || (imsCall.getCallProfile().mMediaProfile == null)) {
            return false;
        }

        ImsStreamMediaProfile mediaProfile = imsCall.getCallProfile().mMediaProfile;
        boolean shouldPlayRingback =
                (mediaProfile.mAudioDirection == ImsStreamMediaProfile.DIRECTION_INACTIVE)
                        ? true : false;
        Rlog.i(LOG_TAG, "isLocalTone: audioDirection=" + mediaProfile.mAudioDirection
                + ", playRingback=" + shouldPlayRingback);
        return shouldPlayRingback;
    }

    public boolean update(ImsPhoneConnection conn, ImsCall imsCall, State state) {
        boolean changed = false;
        State oldState = mState;

        // We will try to start or stop ringback whenever the call has major call state changes.
        maybeChangeRingbackState(imsCall, state);

        if ((state != mState) && (state != State.DISCONNECTED)) {
            mState = state;
            changed = true;
        } else if (state == State.DISCONNECTED) {
            changed = true;
        }

        if (VDBG) {
            Rlog.v(LOG_TAG, "update : " + mCallContext + " state: " + oldState + " --> " + mState);
        }

        return changed;
    }

    /**
     * Determines whether to change the ringback state for a call.
     * @param imsCall The call.
     */
    public void maybeChangeRingbackState(ImsCall imsCall) {
        maybeChangeRingbackState(imsCall, mState);
    }

    /**
     * Determines whether local ringback should be playing for the call.  We will play local
     * ringback when a call is in an ALERTING state and the audio direction is DIRECTION_INACTIVE.
     * @param imsCall The call the change pertains to.
     * @param state The current state of the call.
     */
    private void maybeChangeRingbackState(ImsCall imsCall, State state) {
        //ImsCall.Listener.onCallProgressing can be invoked several times
        //and ringback tone mode can be changed during the call setup procedure
        Rlog.i(LOG_TAG, "maybeChangeRingbackState: state=" + state);
        if (state == State.ALERTING) {
            if (mIsRingbackTonePlaying && !isLocalTone(imsCall)) {
                Rlog.i(LOG_TAG, "maybeChangeRingbackState: stop ringback");
                getPhone().stopRingbackTone();
                mIsRingbackTonePlaying = false;
            } else if (!mIsRingbackTonePlaying && isLocalTone(imsCall)) {
                Rlog.i(LOG_TAG, "maybeChangeRingbackState: start ringback");
                getPhone().startRingbackTone();
                mIsRingbackTonePlaying = true;
            }
        } else {
            if (mIsRingbackTonePlaying) {
                Rlog.i(LOG_TAG, "maybeChangeRingbackState: stop ringback");
                getPhone().stopRingbackTone();
                mIsRingbackTonePlaying = false;
            }
        }
    }

    /* package */ ImsPhoneConnection
    getHandoverConnection() {
        return (ImsPhoneConnection) getEarliestConnection();
    }

    public void switchWith(ImsPhoneCall that) {
        if (VDBG) {
            Rlog.v(LOG_TAG, "switchWith : switchCall = " + this + " withCall = " + that);
        }
        synchronized (ImsPhoneCall.class) {
            ImsPhoneCall tmp = new ImsPhoneCall();
            tmp.takeOver(this);
            this.takeOver(that);
            that.takeOver(tmp);
        }
        mOwner.logState();
    }

    /**
     * Stops ringback tone playing if it is playing.
     */
    public void maybeStopRingback() {
        if (mIsRingbackTonePlaying) {
            getPhone().stopRingbackTone();
            mIsRingbackTonePlaying = false;
        }
    }

    public boolean isRingbackTonePlaying() {
        return mIsRingbackTonePlaying;
    }

    private void takeOver(ImsPhoneCall that) {
        copyConnectionFrom(that);
        mState = that.mState;
        for (Connection c : getConnections()) {
            ((ImsPhoneConnection) c).changeParent(this);
        }
    }
}
