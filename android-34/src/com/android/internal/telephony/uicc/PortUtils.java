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

package com.android.internal.telephony.uicc;

import android.annotation.NonNull;

import com.android.internal.telephony.uicc.IccSlotStatus.MultipleEnabledProfilesMode;

/**
 * Various methods, useful for dealing with port.
 */
public class PortUtils {

    /**
     * Converts the port index to compatible with the HAL.
     *
     * @param mepMode   supported MultipleEnabledProfilesMode
     * @param portIndex port index
     * @return target index according to the MultipleEnabledProfilesMode
     */
    public static int convertToHalPortIndex(@NonNull MultipleEnabledProfilesMode mepMode,
            int portIndex) {
        // In case of MEP-A1 and MEP-A2, profiles are selected on eSIM Ports 1 and higher, hence
        // HAL expects the ports are indexed with 1, 2... etc.
        // So inorder to compatible with HAL, shift the port index.
        return mepMode.isMepAMode() ? ++portIndex : portIndex;
    }

    /**
     * Converts the port index to compatible with the HAL.
     *
     * @param slotIndex   physical slot index corresponding to the portIndex
     * @param portIndex port index
     * @return target port index according to the MultipleEnabledProfilesMode
     */
    public static int convertToHalPortIndex(int slotIndex, int portIndex) {
        return convertToHalPortIndex(UiccController.getInstance().getSupportedMepMode(slotIndex),
                portIndex);
    }

    /**
     * Converts the port index to compatible with the platform.
     *
     * @param slotIndex physical slot index corresponding to the portIndex
     * @param portIndex target port index
     * @param cardState cardState
     * @param supportedMepMode supported MEP mode
     * @return shifted port index according to the MultipleEnabledProfilesMode
     */
    public static int convertFromHalPortIndex(int slotIndex, int portIndex,
            IccCardStatus.CardState cardState, MultipleEnabledProfilesMode supportedMepMode) {
        // In case of MEP-A1 and MEP-A2, profiles are selected on eSIM Ports 1 and higher.
        // But inorder to platform code MEP mode agnostic, platform always expects the ports
        // are indexed with 0, 1... etc. Hence shift the target port index to be compatible
        // with platform.

        // When the SIM_STATUS is related to CARDSTATE_ABSENT, CardStatus will not contain proper
        // MEP mode info, fallback onto to the supportedMepMode data available in UiccSlot.
        MultipleEnabledProfilesMode mepMode = cardState.isCardPresent() ? supportedMepMode
                : UiccController.getInstance().getSupportedMepMode(slotIndex);
        return mepMode.isMepAMode() ? --portIndex : portIndex;
    }
}
