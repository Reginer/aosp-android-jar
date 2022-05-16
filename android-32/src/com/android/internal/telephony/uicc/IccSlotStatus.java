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

import android.telephony.SubscriptionInfo;
import android.text.TextUtils;

/**
 * This class represents the status of the physical UICC slots.
 */
public class IccSlotStatus {

    public enum SlotState {
        SLOTSTATE_INACTIVE,
        SLOTSTATE_ACTIVE;
    }

    public IccCardStatus.CardState  cardState;
    public SlotState  slotState;
    public int        logicalSlotIndex;
    public String     atr;
    public String     iccid;
    public String     eid;

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
     * Set the slotState according to the input state.
     */
    public void setSlotState(int state) {
        switch(state) {
            case 0:
                slotState = SlotState.SLOTSTATE_INACTIVE;
                break;
            case 1:
                slotState = SlotState.SLOTSTATE_ACTIVE;
                break;
            default:
                throw new RuntimeException("Unrecognized RIL_SlotState: " + state);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IccSlotStatus {").append(cardState).append(",")
                .append(slotState).append(",")
                .append("logicalSlotIndex=").append(logicalSlotIndex).append(",")
                .append("atr=").append(atr).append(",iccid=")
                .append(SubscriptionInfo.givePrintableIccid(iccid)).append(",")
                .append("eid=").append(eid);

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
                && (slotState == that.slotState)
                && (logicalSlotIndex == that.logicalSlotIndex)
                && (TextUtils.equals(atr, that.atr))
                && (TextUtils.equals(iccid, that.iccid))
                && (TextUtils.equals(eid, that.eid));
    }

}
