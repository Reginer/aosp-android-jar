/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.internal.telephony.imsphone;

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.telephony.ims.ImsExternalCallState;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Phone;

/**
 * Companion class for {@link ImsExternalConnection}; represents an external call which was
 * received via {@link ImsExternalCallState} info.
 */
public class ImsExternalCall extends Call {

    private Phone mPhone;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public ImsExternalCall(Phone phone, ImsExternalConnection connection) {
        mPhone = phone;
        addConnection(connection);
    }

    @Override
    public Phone getPhone() {
        return mPhone;
    }

    @Override
    public boolean isMultiparty() {
        return false;
    }

    @Override
    public void hangup() throws CallStateException {

    }

    @Override
    public void hangup(@android.telecom.Call.RejectReason int rejectReason)
            throws CallStateException {
        // tumbleweed
    }

    /**
     * Sets the call state to active.
     */
    public void setActive() {
        setState(State.ACTIVE);
    }

    /**
     * Sets the call state to terminated.
     */
    public void setTerminated() {
        setState(State.DISCONNECTED);
    }
}
