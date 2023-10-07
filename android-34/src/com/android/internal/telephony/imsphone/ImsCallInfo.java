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

package com.android.internal.telephony.imsphone;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.telephony.AccessNetworkConstants;
import android.telephony.ServiceState;

import com.android.internal.telephony.Call;

/**
 * Contains call state to be notified to modem.
 */
public class ImsCallInfo {

    private final int mIndex;
    private @Nullable ImsPhoneConnection mConnection = null;
    private Call.State mState = Call.State.IDLE;
    private boolean mIsHeldByRemote = false;

    public ImsCallInfo(int index) {
        mIndex = index;
    }

    /** Clears the call state. */
    public void reset() {
        mConnection = null;
        mState = Call.State.IDLE;
        mIsHeldByRemote = false;
    }

    /**
     * Updates the state of the IMS call.
     *
     * @param c The instance of {@link ImsPhoneConnection}.
     */
    public void update(@NonNull ImsPhoneConnection c) {
        mConnection = c;
        mState = c.getState();
    }

    /**
     * Updates the state of the IMS call.
     *
     * @param c The instance of {@link ImsPhoneConnection}.
     * @param holdReceived {@code true} if the remote party held the call.
     * @param resumeReceived {@code true} if the remote party resumed the call.
     */
    public boolean update(@NonNull ImsPhoneConnection c,
            boolean holdReceived, boolean resumeReceived) {
        Call.State state = c.getState();
        boolean changed = mState != state;
        mState = state;

        if (holdReceived && !mIsHeldByRemote) {
            changed = true;
            mIsHeldByRemote = true;
        } else if (resumeReceived && mIsHeldByRemote) {
            changed = true;
            mIsHeldByRemote = false;
        }

        return changed;
    }

    /** Called when clearing orphaned connection. */
    public void onDisconnect() {
        mState = Call.State.DISCONNECTED;
    }

    /** @return the call index. */
    public int getIndex() {
        return mIndex;
    }

    /** @return the call state. */
    public Call.State getCallState() {
        return mState;
    }

    /** @return whether the remote party is holding the call. */
    public boolean isHeldByRemote() {
        return mIsHeldByRemote;
    }

    /** @return {@code true} if the call is an incoming call. */
    public boolean isIncoming() {
        return mConnection.isIncoming();
    }

    /** @return {@code true} if the call is an emergency call. */
    public boolean isEmergencyCall() {
        return mConnection.isEmergencyCall();
    }

    /** @return the radio technology used for current connection. */
    public @AccessNetworkConstants.RadioAccessNetworkType int getCallRadioTech() {
        return ServiceState.rilRadioTechnologyToAccessNetworkType(mConnection.getCallRadioTech());
    }

    @Override
    public String toString() {
        return "[ id=" + mIndex + ", state=" + mState
                + ", isMT=" + isIncoming() + ", heldByRemote=" + mIsHeldByRemote + " ]";
    }
}
