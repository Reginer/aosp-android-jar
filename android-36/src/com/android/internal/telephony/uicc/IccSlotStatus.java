/*
 * Copyright 2017 The Android Open Source Project
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

package com.android.internal.telephony.uicc;

import android.text.TextUtils;

import com.android.internal.telephony.util.TelephonyUtils;
import com.android.telephony.Rlog;

import java.util.Arrays;

/**
 * This class represents the status of the physical UICC slots.
 */
public class IccSlotStatus {
    /* Added state active to check slotState in old HAL case.*/
    public static final int STATE_ACTIVE = 1;

    public enum MultipleEnabledProfilesMode {
        /**
         * If there is no jointly supported MEP mode, set supported MEP mode to NONE.
         */
        NONE,
        /**
         * In case of MEP-A1, the ISD-R is selected on eSIM port 0 only and profiles are selected
         * on eSIM ports 1 and higher, with the eSIM port being assigned by the LPA or platform.
         */
        MEP_A1,
        /**
         * In case of MEP-A2, the ISD-R is selected on eSIM port 0 only and profiles are selected
         * on eSIM ports 1 and higher, with the eSIM port being assigned by the eUICC.
         */
        MEP_A2,
        /**
         * In case of MEP-B, profiles are selected on eSIM ports 0 and higher, with the ISD-R being
         * selectable on any of these eSIM ports.
         */
        MEP_B;

        public boolean isMepAMode() {
            return (this == MEP_A1 || this == MEP_A2);
        }

        public boolean isMepA1Mode() {
            return this == MEP_A1;
        }

        public boolean isMepMode() {
            return this != NONE;
        }
    }

    public IccCardStatus.CardState  cardState;
    public String     atr;
    public String     eid;

    public IccSimPortInfo[] mSimPortInfos;
    public MultipleEnabledProfilesMode mSupportedMepMode = MultipleEnabledProfilesMode.NONE;

    /**
     * Set the cardState according to the input state.
     */
    public void setCardState(int state) {
        switch(state) {
            case 0:
                cardState = IccCardStatus.CardState.CARDSTATE_ABSENT;
                break;
            case 1:
                cardState = IccCardStatus.CardState.CARDSTATE_PRESENT;
                break;
            case 2:
                cardState = IccCardStatus.CardState.CARDSTATE_ERROR;
                break;
            case 3:
                cardState = IccCardStatus.CardState.CARDSTATE_RESTRICTED;
                break;
            default:
                throw new RuntimeException("Unrecognized RIL_CardState: " + state);
        }
    }

    /**
     * Set the MultipleEnabledProfilesMode according to the input mode.
     */
    public void setMultipleEnabledProfilesMode(int mode) {
        switch(mode) {
            case 0:
                mSupportedMepMode = MultipleEnabledProfilesMode.NONE;
                break;
            case 1:
                mSupportedMepMode = MultipleEnabledProfilesMode.MEP_A1;
                break;
            case 2:
                mSupportedMepMode = MultipleEnabledProfilesMode.MEP_A2;
                break;
            case 3:
                mSupportedMepMode = MultipleEnabledProfilesMode.MEP_B;
                break;
            default:
                throw new RuntimeException("Unrecognized RIL_MultipleEnabledProfilesMode: " + mode);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IccSlotStatus {").append(cardState).append(",")
                .append("atr=").append(atr).append(",")
                .append("eid=").append(Rlog.pii(TelephonyUtils.IS_DEBUGGABLE, eid)).append(",");
        if (mSimPortInfos != null) {
            sb.append("num_ports=").append(mSimPortInfos.length);
            for (int i =0; i < mSimPortInfos.length; i++) {
                sb.append(", IccSimPortInfo-" + i + mSimPortInfos[i]);
            }
        } else {
            sb.append("num_ports=null");
        }
        sb.append(", SupportedMepMode=" + mSupportedMepMode);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        IccSlotStatus that = (IccSlotStatus) obj;
        return (cardState == that.cardState)
                && (TextUtils.equals(atr, that.atr))
                && (TextUtils.equals(eid, that.eid))
                && Arrays.equals(mSimPortInfos, that.mSimPortInfos);
    }

}
