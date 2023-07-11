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
import android.os.Build;

import com.android.ims.internal.ConferenceParticipant;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@hide}
 */
public abstract class Call {
    protected final String LOG_TAG = "Call";

    @UnsupportedAppUsage
    public Call() {
    }

    /* Enums */
    @UnsupportedAppUsage(implicitMember = "values()[Lcom/android/internal/telephony/Call$State;")
    public enum State {
        @UnsupportedAppUsage IDLE,
        ACTIVE,
        @UnsupportedAppUsage HOLDING,
        @UnsupportedAppUsage DIALING,
        @UnsupportedAppUsage ALERTING,
        @UnsupportedAppUsage INCOMING,
        @UnsupportedAppUsage WAITING,
        @UnsupportedAppUsage DISCONNECTED,
        @UnsupportedAppUsage DISCONNECTING;

        @UnsupportedAppUsage
        public boolean isAlive() {
            return !(this == IDLE || this == DISCONNECTED || this == DISCONNECTING);
        }

        @UnsupportedAppUsage
        public boolean isRinging() {
            return this == INCOMING || this == WAITING;
        }

        public boolean isDialing() {
            return this == DIALING || this == ALERTING;
        }
    }

    public static State
    stateFromDCState (DriverCall.State dcState) {
        switch (dcState) {
            case ACTIVE:        return State.ACTIVE;
            case HOLDING:       return State.HOLDING;
            case DIALING:       return State.DIALING;
            case ALERTING:      return State.ALERTING;
            case INCOMING:      return State.INCOMING;
            case WAITING:       return State.WAITING;
            default:            throw new RuntimeException ("illegal call state:" + dcState);
        }
    }

    public enum SrvccState {
        NONE, STARTED, COMPLETED, FAILED, CANCELED;
    }

    /* Instance Variables */

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public State mState = State.IDLE;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public ArrayList<Connection> mConnections = new ArrayList<>();

    private Object mLock = new Object();

    /* Instance Methods */

    /** Do not modify the List result!!! This list is not yours to keep
     *  It will change across event loop iterations            top
     */

    @UnsupportedAppUsage
    public ArrayList<Connection> getConnections() {
        synchronized (mLock) {
            return (ArrayList<Connection>) mConnections.clone();
        }
    }

    /**
     * Get mConnections field from another Call instance.
     * @param other
     */
    public void copyConnectionFrom(Call other) {
        mConnections = other.getConnections();
    }

    /**
     * Get connections count of this instance.
     * @return the count to return
     */
    public int getConnectionsCount() {
        synchronized (mLock) {
            return mConnections.size();
        }
    }

    /**
     * @return returns a summary of the connections held in this call.
     */
    public String getConnectionSummary() {
        synchronized (mLock) {
            return mConnections.stream()
                    .map(c -> c.getTelecomCallId() + "/objId:" + System.identityHashCode(c))
                    .collect(Collectors.joining(", "));
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public abstract Phone getPhone();
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public abstract boolean isMultiparty();
    @UnsupportedAppUsage
    public abstract void hangup() throws CallStateException;

    public abstract void hangup(@android.telecom.Call.RejectReason int rejectReason)
            throws CallStateException;

    /**
     * hasConnection
     *
     * @param c a Connection object
     * @return true if the call contains the connection object passed in
     */
    public boolean hasConnection(Connection c) {
        return c.getCall() == this;
    }

    /**
     * hasConnections
     * @return true if the call contains one or more connections
     */
    public boolean hasConnections() {
        List<Connection> connections = getConnections();

        if (connections == null) {
            return false;
        }

        return connections.size() > 0;
    }

    /**
     * removeConnection
     *
     * @param conn the connection to be removed
     */
    public void removeConnection(Connection conn) {
        synchronized (mLock) {
            mConnections.remove(conn);
        }
    }

    /**
     * addConnection
     *
     * @param conn the connection to be added
     */
    public void addConnection(Connection conn) {
        synchronized (mLock) {
            mConnections.add(conn);
        }
    }

    /**
     * clearConnection
     */
    public void clearConnections() {
        synchronized (mLock) {
            mConnections.clear();
        }
    }

    /**
     * getState
     * @return state of class call
     */
    @UnsupportedAppUsage
    public State getState() {
        return mState;
    }

    /**
     * getConferenceParticipants
     * @return List of conference participants.
     */
    public List<ConferenceParticipant> getConferenceParticipants() {
        return null;
    }

    /**
     * isIdle
     *
     * FIXME rename
     * @return true if the call contains only disconnected connections (if any)
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public boolean isIdle() {
        return !getState().isAlive();
    }

    /**
     * Returns the Connection associated with this Call that was created
     * first, or null if there are no Connections in this Call
     */
    @UnsupportedAppUsage
    public Connection
    getEarliestConnection() {
        List<Connection> l;
        long time = Long.MAX_VALUE;
        Connection c;
        Connection earliest = null;

        l = getConnections();

        if (l.size() == 0) {
            return null;
        }

        for (int i = 0, s = l.size() ; i < s ; i++) {
            c = l.get(i);
            long t;

            t = c.getCreateTime();

            if (t < time) {
                earliest = c;
                time = t;
            }
        }

        return earliest;
    }

    public long
    getEarliestCreateTime() {
        List<Connection> l;
        long time = Long.MAX_VALUE;

        l = getConnections();

        if (l.size() == 0) {
            return 0;
        }

        for (int i = 0, s = l.size() ; i < s ; i++) {
            Connection c = l.get(i);
            long t;

            t = c.getCreateTime();

            time = t < time ? t : time;
        }

        return time;
    }

    public long
    getEarliestConnectTime() {
        long time = Long.MAX_VALUE;
        List<Connection> l = getConnections();

        if (l.size() == 0) {
            return 0;
        }

        for (int i = 0, s = l.size() ; i < s ; i++) {
            Connection c = l.get(i);
            long t;

            t = c.getConnectTime();

            time = t < time ? t : time;
        }

        return time;
    }


    public boolean
    isDialingOrAlerting() {
        return getState().isDialing();
    }

    public boolean
    isRinging() {
        return getState().isRinging();
    }

    /**
     * Returns the Connection associated with this Call that was created
     * last, or null if there are no Connections in this Call
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public Connection
    getLatestConnection() {
        List<Connection> l = getConnections();
        if (l.size() == 0) {
            return null;
        }

        long time = 0;
        Connection latest = null;
        for (int i = 0, s = l.size() ; i < s ; i++) {
            Connection c = l.get(i);
            long t = c.getCreateTime();

            if (t > time) {
                latest = c;
                time = t;
            }
        }

        return latest;
    }

    /**
     * Hangup call if it is alive
     */
    public void hangupIfAlive() {
        if (getState().isAlive()) {
            try {
                hangup();
            } catch (CallStateException ex) {
                Rlog.w(LOG_TAG, " hangupIfActive: caught " + ex);
            }
        }
    }

    /**
     * Called when it's time to clean up disconnected Connection objects
     */
    public void clearDisconnected() {
        for (Connection conn : getConnections()) {
            if (conn.getState() == State.DISCONNECTED) {
                removeConnection(conn);
            }
        }

        if (getConnectionsCount() == 0) {
            setState(State.IDLE);
        }
    }

    protected void setState(State newState) {
        mState = newState;
    }
}
