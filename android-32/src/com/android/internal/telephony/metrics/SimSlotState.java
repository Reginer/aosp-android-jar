/*
 * Copyright (C) 2020 The Android Open Source Project
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

import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccSlot;
import com.android.telephony.Rlog;

/** Snapshots and stores the current SIM state. */
public class SimSlotState {
    private static final String TAG = SimSlotState.class.getSimpleName();

    public final int numActiveSlots;
    public final int numActiveSims;
    public final int numActiveEsims;

    /** Returns the current SIM state. */
    public static SimSlotState getCurrentState() {
        int numActiveSlots = 0;
        int numActiveSims = 0;
        int numActiveEsims = 0;
        UiccController uiccController = UiccController.getInstance();
        // since we cannot hold lock insider UiccController, using getUiccSlots() for length only
        for (int i = 0; i < uiccController.getUiccSlots().length; i++) {
            UiccSlot slot = uiccController.getUiccSlot(i);
            if (slot != null && slot.isActive()) {
                numActiveSlots++;
                // avoid CardState.isCardPresent() since this should not include restricted cards
                if (slot.getCardState() == CardState.CARDSTATE_PRESENT) {
                    if (slot.isEuicc()) {
                        // need to check active profiles besides the presence of eSIM cards
                        UiccCard card = slot.getUiccCard();
                        if (card != null && card.getNumApplications() > 0) {
                            numActiveSims++;
                            numActiveEsims++;
                        }
                    } else {
                        // physical SIMs do not always have non-null card
                        numActiveSims++;
                    }
                }
            }
        }
        return new SimSlotState(numActiveSlots, numActiveSims, numActiveEsims);
    }

    private SimSlotState(int numActiveSlots, int numActiveSims, int numActiveEsims) {
        this.numActiveSlots = numActiveSlots;
        this.numActiveSims = numActiveSims;
        this.numActiveEsims = numActiveEsims;
    }

    /** Returns whether the given phone is using a eSIM. */
    public static boolean isEsim(int phoneId) {
        UiccSlot slot = UiccController.getInstance().getUiccSlotForPhone(phoneId);
        if (slot != null) {
            return slot.isEuicc();
        } else {
            // should not happen, but assume we are not using eSIM
            Rlog.e(TAG, "isEsim: slot=null for phone " + phoneId);
            return false;
        }
    }

    /** Returns whether the device has multiple active SIM profiles. */
    public static boolean isMultiSim() {
        return (getCurrentState().numActiveSims > 1);
    }
}
