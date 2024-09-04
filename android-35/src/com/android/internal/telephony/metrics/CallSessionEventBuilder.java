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

import com.android.internal.telephony.nano.TelephonyProto.EmergencyNumberInfo;
import com.android.internal.telephony.nano.TelephonyProto.ImsCapabilities;
import com.android.internal.telephony.nano.TelephonyProto.ImsConnectionState;
import com.android.internal.telephony.nano.TelephonyProto.ImsReasonInfo;
import com.android.internal.telephony.nano.TelephonyProto.RilDataCall;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.CallQuality;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.CallQualitySummary;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyServiceState;
import com.android.internal.telephony.nano.TelephonyProto.TelephonySettings;

public class CallSessionEventBuilder {
    private final TelephonyCallSession.Event mEvent = new TelephonyCallSession.Event();

    public TelephonyCallSession.Event build() {
        return mEvent;
    }

    public CallSessionEventBuilder(int type) {
        mEvent.type = type;
    }

    public CallSessionEventBuilder setDelay(int delay) {
        mEvent.delay = delay;
        return this;
    }

    public CallSessionEventBuilder setRilRequest(int rilRequestType) {
        mEvent.rilRequest = rilRequestType;
        return this;
    }

    public CallSessionEventBuilder setRilRequestId(int rilRequestId) {
        mEvent.rilRequestId = rilRequestId;
        return this;
    }

    public CallSessionEventBuilder setRilError(int rilError) {
        mEvent.error = rilError;
        return this;
    }

    public CallSessionEventBuilder setCallIndex(int callIndex) {
        mEvent.callIndex = callIndex;
        return this;
    }

    public CallSessionEventBuilder setCallState(int state) {
        mEvent.callState = state;
        return this;
    }

    public CallSessionEventBuilder setSrvccState(int srvccState) {
        mEvent.srvccState = srvccState;
        return this;
    }

    public CallSessionEventBuilder setImsCommand(int imsCommand) {
        mEvent.imsCommand = imsCommand;
        return this;
    }

    public CallSessionEventBuilder setImsReasonInfo(ImsReasonInfo reasonInfo) {
        mEvent.reasonInfo = reasonInfo;
        return this;
    }

    public CallSessionEventBuilder setSrcAccessTech(int tech) {
        mEvent.srcAccessTech = tech;
        return this;
    }

    public CallSessionEventBuilder setTargetAccessTech(int tech) {
        mEvent.targetAccessTech = tech;
        return this;
    }

    public CallSessionEventBuilder setSettings(TelephonySettings settings) {
        mEvent.settings = settings;
        return this;
    }

    public CallSessionEventBuilder setServiceState(TelephonyServiceState state) {
        mEvent.serviceState = state;
        return this;
    }

    public CallSessionEventBuilder setImsConnectionState(ImsConnectionState state) {
        mEvent.imsConnectionState = state;
        return this;
    }

    public CallSessionEventBuilder setImsCapabilities(ImsCapabilities capabilities) {
        mEvent.imsCapabilities = capabilities;
        return this;
    }

    public CallSessionEventBuilder setDataCalls(RilDataCall[] dataCalls) {
        mEvent.dataCalls = dataCalls;
        return this;
    }

    public CallSessionEventBuilder setPhoneState(int phoneState) {
        mEvent.phoneState = phoneState;
        return this;
    }

    public CallSessionEventBuilder setNITZ(long timestamp) {
        mEvent.nitzTimestampMillis = timestamp;
        return this;
    }

    public CallSessionEventBuilder setRilCalls(RilCall[] rilCalls) {
        mEvent.calls = rilCalls;
        return this;
    }

    /** Set the audio codec. */
    public CallSessionEventBuilder setAudioCodec(int audioCodec) {
        mEvent.audioCodec = audioCodec;
        return this;
    }

    /** Set the call quality. */
    public CallSessionEventBuilder setCallQuality(CallQuality callQuality) {
        mEvent.callQuality = callQuality;
        return this;
    }

    /** Set the downlink call quality summary. */
    public CallSessionEventBuilder setCallQualitySummaryDl(CallQualitySummary callQualitySummary) {
        mEvent.callQualitySummaryDl = callQualitySummary;
        return this;
    }

    /** Set the uplink call quality summary. */
    public CallSessionEventBuilder setCallQualitySummaryUl(CallQualitySummary callQualitySummary) {
        mEvent.callQualitySummaryUl = callQualitySummary;
        return this;
    }

    /** Set if the Ims call is emergency. */
    public CallSessionEventBuilder setIsImsEmergencyCall(boolean isImsEmergencyCall) {
        mEvent.isImsEmergencyCall = isImsEmergencyCall;
        return this;
    }

    /** Set the emergency number database version in Ims emergency call information. */
    public CallSessionEventBuilder setEmergencyNumberDatabaseVersion(
            int emergencyNumberDatabaseVersion) {
        mEvent.emergencyNumberDatabaseVersion = emergencyNumberDatabaseVersion;
        return this;
    }

    /** Set the Ims emergency call information. */
    public CallSessionEventBuilder setImsEmergencyNumberInfo(
            EmergencyNumberInfo imsEmergencyNumberInfo) {
        mEvent.imsEmergencyNumberInfo = imsEmergencyNumberInfo;
        return this;
    }
}
