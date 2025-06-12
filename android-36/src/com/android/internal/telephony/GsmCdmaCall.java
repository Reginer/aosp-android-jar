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
import android.os.Build;

import com.android.internal.annotations.VisibleForTesting;

/**
 * {@hide}
 */
public class GsmCdmaCall extends Call {
    /*************************** Instance Variables **************************/

    /*package*/ GsmCdmaCallTracker mOwner;

    /****************************** Constructors *****************************/
    /*package*/
    public GsmCdmaCall (GsmCdmaCallTracker owner) {
        mOwner = owner;
    }

    /************************** Overridden from Call *************************/

    @Override
    public Phone getPhone() {
        return mOwner.getPhone();
    }

    @Override
    public boolean isMultiparty() {
        return getConnectionsCount() > 1;
    }

    /** Please note: if this is the foreground call and a
     *  background call exists, the background call will be resumed
     *  because an AT+CHLD=1 will be sent
     */
    @Override
    public void hangup() throws CallStateException {
        mOwner.hangup(this);
    }

    /**
     * Hangup the ringing call with a specified reason; reason is not supported on GSM/CDMA.
     * @param rejectReason
     */
    @Override
    public void hangup(@android.telecom.Call.RejectReason int rejectReason)
            throws CallStateException {
        mOwner.hangup(this);
    }

    @Override
    public String toString() {
        return mState.toString();
    }

    //***** Called from GsmCdmaConnection

    public void attach(Connection conn, DriverCall dc) {
        addConnection(conn);

        mState = stateFromDCState (dc.state);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void attachFake(Connection conn, State state) {
        addConnection(conn);

        mState = state;
    }

    /**
     * Called by GsmCdmaConnection when it has disconnected
     */
    public boolean connectionDisconnected(GsmCdmaConnection conn) {
        if (mState != State.DISCONNECTED) {
            /* If only disconnected connections remain, we are disconnected*/

            boolean hasOnlyDisconnectedConnections = true;

            for (Connection c : getConnections()) {
                if (c.getState() != State.DISCONNECTED) {
                    hasOnlyDisconnectedConnections = false;
                    break;
                }
            }

            if (hasOnlyDisconnectedConnections) {
                mState = State.DISCONNECTED;
                return true;
            }
        }

        return false;
    }

    public void detach(GsmCdmaConnection conn) {
        removeConnection(conn);

        if (getConnectionsCount() == 0) {
            mState = State.IDLE;
        }
    }

    /*package*/ boolean update (GsmCdmaConnection conn, DriverCall dc) {
        State newState;
        boolean changed = false;

        newState = stateFromDCState(dc.state);

        if (newState != mState) {
            mState = newState;
            changed = true;
        }

        return changed;
    }

    /**
     * @return true if there's no space in this call for additional
     * connections to be added via "conference"
     */
    /*package*/ boolean isFull() {
        return getConnectionsCount() == mOwner.getMaxConnectionsPerCall();
    }

    //***** Called from GsmCdmaCallTracker


    /**
     * Called when this Call is being hung up locally (eg, user pressed "end")
     * Note that at this point, the hangup request has been dispatched to the radio
     * but no response has yet been received so update() has not yet been called
     */
    @VisibleForTesting
    public void onHangupLocal() {
        if (!mState.isAlive()) {
            return;
        }
        for (Connection conn : getConnections()) {
            ((GsmCdmaConnection) conn).onHangupLocal();
        }
        mState = State.DISCONNECTING;
    }
}