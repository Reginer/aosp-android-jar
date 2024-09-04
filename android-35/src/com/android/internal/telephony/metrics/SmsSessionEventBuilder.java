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
 * limitations under the License.
 */

package com.android.internal.telephony.metrics;

import com.android.internal.telephony.nano.TelephonyProto.ImsCapabilities;
import com.android.internal.telephony.nano.TelephonyProto.ImsConnectionState;
import com.android.internal.telephony.nano.TelephonyProto.RilDataCall;
import com.android.internal.telephony.nano.TelephonyProto.SmsSession;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyServiceState;
import com.android.internal.telephony.nano.TelephonyProto.TelephonySettings;

public class SmsSessionEventBuilder {
    SmsSession.Event mEvent = new SmsSession.Event();

    public SmsSession.Event build() {
        return mEvent;
    }

    public SmsSessionEventBuilder(int type) {
        mEvent.type = type;
    }

    public SmsSessionEventBuilder setDelay(int delay) {
        mEvent.delay = delay;
        return this;
    }

    public SmsSessionEventBuilder setTech(int tech) {
        mEvent.tech = tech;
        return this;
    }

    public SmsSessionEventBuilder setErrorCode(int code) {
        mEvent.errorCode = code;
        return this;
    }

    public SmsSessionEventBuilder setRilErrno(int errno) {
        mEvent.error = errno;
        return this;
    }

    public SmsSessionEventBuilder setImsServiceErrno(int errno) {
        mEvent.imsError = errno;
        return this;
    }

    public SmsSessionEventBuilder setSettings(TelephonySettings settings) {
        mEvent.settings = settings;
        return this;
    }

    public SmsSessionEventBuilder setServiceState(TelephonyServiceState state) {
        mEvent.serviceState = state;
        return this;
    }

    public SmsSessionEventBuilder setImsConnectionState(ImsConnectionState state) {
        mEvent.imsConnectionState = state;
        return this;
    }

    public SmsSessionEventBuilder setImsCapabilities(ImsCapabilities capabilities) {
        mEvent.imsCapabilities = capabilities;
        return this;
    }

    public SmsSessionEventBuilder setDataCalls(RilDataCall[] dataCalls) {
        mEvent.dataCalls = dataCalls;
        return this;
    }

    public SmsSessionEventBuilder setRilRequestId(int id) {
        mEvent.rilRequestId = id;
        return this;
    }

    public SmsSessionEventBuilder setFormat(int format) {
        mEvent.format = format;
        return this;
    }

    public SmsSessionEventBuilder setCellBroadcastMessage(SmsSession.Event.CBMessage msg) {
        mEvent.cellBroadcastMessage = msg;
        return this;
    }

    /** Set details on incomplete SMS */
    public SmsSessionEventBuilder setIncompleteSms(SmsSession.Event.IncompleteSms msg) {
        mEvent.incompleteSms = msg;
        return this;
    }

    /** Set indication if SMS was blocked */
    public SmsSessionEventBuilder setBlocked(boolean blocked) {
        mEvent.blocked = blocked;
        return this;
    }

    /** Set SMS type */
    public SmsSessionEventBuilder setSmsType(int type) {
        mEvent.smsType = type;
        return this;
    }

    /** Set message id */
    public SmsSessionEventBuilder setMessageId(long messageId) {
        mEvent.messageId = messageId;
        return this;
    }
}
