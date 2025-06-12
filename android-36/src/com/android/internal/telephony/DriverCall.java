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
import android.telephony.PhoneNumberUtils;

import com.android.telephony.Rlog;

/**
 * {@hide}
 */
public class DriverCall implements Comparable<DriverCall> {
    static final String LOG_TAG = "DriverCall";

    @UnsupportedAppUsage(implicitMember =
            "values()[Lcom/android/internal/telephony/DriverCall$State;")
    public enum State {
        @UnsupportedAppUsage
        ACTIVE,
        @UnsupportedAppUsage
        HOLDING,
        @UnsupportedAppUsage
        DIALING,    // MO call only
        @UnsupportedAppUsage
        ALERTING,   // MO call only
        @UnsupportedAppUsage
        INCOMING,   // MT call only
        @UnsupportedAppUsage
        WAITING;    // MT call only
        // If you add a state, make sure to look for the switch()
        // statements that use this enum
    }

    /**
     * Audio information
     */
    /** Unspecified audio codec */
    public static final int AUDIO_QUALITY_UNSPECIFIED = 0;
    /** AMR (Narrowband) audio codec */
    public static final int AUDIO_QUALITY_AMR = 1;
    /** AMR (Wideband) audio codec */
    public static final int AUDIO_QUALITY_AMR_WB = 2;
    /** GSM Enhanced Full-Rate audio codec */
    public static final int AUDIO_QUALITY_GSM_EFR = 3;
    /** GSM Full-Rate audio codec */
    public static final int AUDIO_QUALITY_GSM_FR = 4;
    /** GSM Half-Rate audio codec */
    public static final int AUDIO_QUALITY_GSM_HR = 5;
    /** Enhanced Variable rate codec */
    public static final int AUDIO_QUALITY_EVRC = 6;
    /** Enhanced Variable rate codec revision B */
    public static final int AUDIO_QUALITY_EVRC_B = 7;
    /** Enhanced Variable rate codec (Wideband) */
    public static final int AUDIO_QUALITY_EVRC_WB = 8;
    /** Enhanced Variable rate codec (Narrowband) */
    public static final int AUDIO_QUALITY_EVRC_NW = 9;

    @UnsupportedAppUsage
    public int index;
    @UnsupportedAppUsage
    public boolean isMT;
    @UnsupportedAppUsage
    public State state;     // May be null if unavail
    public boolean isMpty;
    @UnsupportedAppUsage
    public String number;
    public String forwardedNumber;     // May be null. Incoming calls only.
    public int TOA;
    @UnsupportedAppUsage
    public boolean isVoice;
    public boolean isVoicePrivacy;
    public int als;
    @UnsupportedAppUsage
    public int numberPresentation;
    @UnsupportedAppUsage
    public String name;
    public int namePresentation;
    public UUSInfo uusInfo;
    public int audioQuality = AUDIO_QUALITY_UNSPECIFIED;

    /** returns null on error */
    static DriverCall
    fromCLCCLine(String line) {
        DriverCall ret = new DriverCall();

        //+CLCC: 1,0,2,0,0,\"+18005551212\",145
        //     index,isMT,state,mode,isMpty(,number,TOA)?
        ATResponseParser p = new ATResponseParser(line);

        try {
            ret.index = p.nextInt();
            ret.isMT = p.nextBoolean();
            ret.state = stateFromCLCC(p.nextInt());

            ret.isVoice = (0 == p.nextInt());
            ret.isMpty = p.nextBoolean();

            // use ALLOWED as default presentation while parsing CLCC
            ret.numberPresentation = PhoneConstants.PRESENTATION_ALLOWED;

            if (p.hasMore()) {
                // Some lame implementations return strings
                // like "NOT AVAILABLE" in the CLCC line
                ret.number = PhoneNumberUtils.extractNetworkPortionAlt(p.nextString());

                if (ret.number.length() == 0) {
                    ret.number = null;
                }

                ret.TOA = p.nextInt();

                // Make sure there's a leading + on addresses with a TOA
                // of 145

                ret.number = PhoneNumberUtils.stringFromStringAndTOA(
                                ret.number, ret.TOA);

            }
        } catch (ATParseEx ex) {
            Rlog.e(LOG_TAG,"Invalid CLCC line: '" + line + "'");
            return null;
        }

        return ret;
    }

    @UnsupportedAppUsage
    public
    DriverCall() {
    }

    @Override
    public String
    toString() {
        return "id=" + index + ","
                + state + ","
                + "toa=" + TOA + ","
                + (isMpty ? "conf" : "norm") + ","
                + (isMT ? "mt" : "mo") + ","
                + als + ","
                + (isVoice ? "voc" : "nonvoc") + ","
                + (isVoicePrivacy ? "evp" : "noevp") + ","
                /*+ "number=" + number */ + ",cli=" + numberPresentation + ","
                /*+ "name="+ name */ + "," + namePresentation + ","
                + "audioQuality=" + audioQuality;
    }

    public static State
    stateFromCLCC(int state) throws ATParseEx {
        switch(state) {
            case 0: return State.ACTIVE;
            case 1: return State.HOLDING;
            case 2: return State.DIALING;
            case 3: return State.ALERTING;
            case 4: return State.INCOMING;
            case 5: return State.WAITING;
            default:
                throw new ATParseEx("illegal call state " + state);
        }
    }

    public static int
    presentationFromCLIP(int cli) throws ATParseEx
    {
        switch(cli) {
            case 0: return PhoneConstants.PRESENTATION_ALLOWED;
            case 1: return PhoneConstants.PRESENTATION_RESTRICTED;
            case 2: return PhoneConstants.PRESENTATION_UNKNOWN;
            case 3: return PhoneConstants.PRESENTATION_PAYPHONE;
            default:
                throw new ATParseEx("illegal presentation " + cli);
        }
    }

    //***** Comparable Implementation

    /** For sorting by index */
    @Override
    public int
    compareTo(DriverCall dc) {

        if (index < dc.index) {
            return -1;
        } else if (index == dc.index) {
            return 0;
        } else { /*index > dc.index*/
            return 1;
        }
    }
}
