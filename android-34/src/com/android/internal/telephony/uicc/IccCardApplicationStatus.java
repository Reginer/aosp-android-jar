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

package com.android.internal.telephony.uicc;

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;

import com.android.internal.telephony.uicc.IccCardStatus.PinState;
import com.android.telephony.Rlog;

/**
 * See also RIL_AppStatus in include/telephony/ril.h
 *
 * {@hide}
 */
public class IccCardApplicationStatus {
    // TODO: Replace with constants from PhoneConstants.APPTYPE_xxx
    @UnsupportedAppUsage(implicitMember =
            "values()[Lcom/android/internal/telephony/uicc/IccCardApplicationStatus$AppType;")
    public enum AppType{
        @UnsupportedAppUsage
        APPTYPE_UNKNOWN,
        @UnsupportedAppUsage
        APPTYPE_SIM,
        @UnsupportedAppUsage
        APPTYPE_USIM,
        @UnsupportedAppUsage
        APPTYPE_RUIM,
        @UnsupportedAppUsage
        APPTYPE_CSIM,
        @UnsupportedAppUsage
        APPTYPE_ISIM
    }

    @UnsupportedAppUsage(implicitMember =
            "values()[Lcom/android/internal/telephony/uicc/IccCardApplicationStatus$AppState;")
    public enum AppState{
        @UnsupportedAppUsage
        APPSTATE_UNKNOWN,
        @UnsupportedAppUsage
        APPSTATE_DETECTED,
        @UnsupportedAppUsage
        APPSTATE_PIN,
        @UnsupportedAppUsage
        APPSTATE_PUK,
        @UnsupportedAppUsage
        APPSTATE_SUBSCRIPTION_PERSO,
        @UnsupportedAppUsage
        APPSTATE_READY;

        boolean isPinRequired() {
            return this == APPSTATE_PIN;
        }

        boolean isPukRequired() {
            return this == APPSTATE_PUK;
        }

        boolean isSubscriptionPersoEnabled() {
            return this == APPSTATE_SUBSCRIPTION_PERSO;
        }

        boolean isAppReady() {
            return this == APPSTATE_READY;
        }

        boolean isAppNotReady() {
            return this == APPSTATE_UNKNOWN  ||
                   this == APPSTATE_DETECTED;
        }
    }

    @UnsupportedAppUsage(implicitMember =
            "values()[Lcom/android/internal/telephony/uicc/IccCardApplicationStatus$PersoSubState;")
    public enum PersoSubState {
        @UnsupportedAppUsage
        PERSOSUBSTATE_UNKNOWN,
        PERSOSUBSTATE_IN_PROGRESS,
        PERSOSUBSTATE_READY,
        @UnsupportedAppUsage
        PERSOSUBSTATE_SIM_NETWORK,
        @UnsupportedAppUsage
        PERSOSUBSTATE_SIM_NETWORK_SUBSET,
        PERSOSUBSTATE_SIM_CORPORATE,
        @UnsupportedAppUsage
        PERSOSUBSTATE_SIM_SERVICE_PROVIDER,
        PERSOSUBSTATE_SIM_SIM,
        PERSOSUBSTATE_SIM_NETWORK_PUK,
        @UnsupportedAppUsage
        PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK,
        PERSOSUBSTATE_SIM_CORPORATE_PUK,
        @UnsupportedAppUsage
        PERSOSUBSTATE_SIM_SERVICE_PROVIDER_PUK,
        PERSOSUBSTATE_SIM_SIM_PUK,
        PERSOSUBSTATE_RUIM_NETWORK1,
        PERSOSUBSTATE_RUIM_NETWORK2,
        PERSOSUBSTATE_RUIM_HRPD,
        PERSOSUBSTATE_RUIM_CORPORATE,
        PERSOSUBSTATE_RUIM_SERVICE_PROVIDER,
        PERSOSUBSTATE_RUIM_RUIM,
        PERSOSUBSTATE_RUIM_NETWORK1_PUK,
        PERSOSUBSTATE_RUIM_NETWORK2_PUK,
        PERSOSUBSTATE_RUIM_HRPD_PUK,
        PERSOSUBSTATE_RUIM_CORPORATE_PUK,
        PERSOSUBSTATE_RUIM_SERVICE_PROVIDER_PUK,
        PERSOSUBSTATE_RUIM_RUIM_PUK,
        PERSOSUBSTATE_SIM_SPN,
        PERSOSUBSTATE_SIM_SPN_PUK,
        PERSOSUBSTATE_SIM_SP_EHPLMN,
        PERSOSUBSTATE_SIM_SP_EHPLMN_PUK,
        PERSOSUBSTATE_SIM_ICCID,
        PERSOSUBSTATE_SIM_ICCID_PUK,
        PERSOSUBSTATE_SIM_IMPI,
        PERSOSUBSTATE_SIM_IMPI_PUK,
        PERSOSUBSTATE_SIM_NS_SP,
        PERSOSUBSTATE_SIM_NS_SP_PUK;

        boolean isPersoSubStateUnknown() {
            return this == PERSOSUBSTATE_UNKNOWN;
        }

        public static boolean isPersoLocked(PersoSubState mState) {
            switch (mState) {
                case PERSOSUBSTATE_UNKNOWN:
                case PERSOSUBSTATE_IN_PROGRESS:
                case PERSOSUBSTATE_READY:
                    return false;
                default:
                    return true;
            }
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public AppType        app_type;
    public AppState       app_state;
    // applicable only if app_state == RIL_APPSTATE_SUBSCRIPTION_PERSO
    public PersoSubState  perso_substate;
    // null terminated string, e.g., from 0xA0, 0x00 -> 0x41, 0x30, 0x30, 0x30 */
    public String         aid;
    // null terminated string
    public String         app_label;
    // applicable to USIM and CSIM
    public boolean        pin1_replaced;
    public PinState       pin1;
    public PinState       pin2;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public IccCardApplicationStatus() {
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public AppType AppTypeFromRILInt(int type) {
        AppType newType;
        /* RIL_AppType ril.h */
        switch(type) {
            case 0: newType = AppType.APPTYPE_UNKNOWN; break;
            case 1: newType = AppType.APPTYPE_SIM;     break;
            case 2: newType = AppType.APPTYPE_USIM;    break;
            case 3: newType = AppType.APPTYPE_RUIM;    break;
            case 4: newType = AppType.APPTYPE_CSIM;    break;
            case 5: newType = AppType.APPTYPE_ISIM;    break;
            default:
                newType = AppType.APPTYPE_UNKNOWN;
                loge("AppTypeFromRILInt: bad RIL_AppType: " + type + " use APPTYPE_UNKNOWN");
        }
        return newType;
    }

    public AppState AppStateFromRILInt(int state) {
        AppState newState;
        /* RIL_AppState ril.h */
        switch(state) {
            case 0: newState = AppState.APPSTATE_UNKNOWN;  break;
            case 1: newState = AppState.APPSTATE_DETECTED; break;
            case 2: newState = AppState.APPSTATE_PIN; break;
            case 3: newState = AppState.APPSTATE_PUK; break;
            case 4: newState = AppState.APPSTATE_SUBSCRIPTION_PERSO; break;
            case 5: newState = AppState.APPSTATE_READY; break;
            default:
                newState = AppState.APPSTATE_UNKNOWN;
                loge("AppStateFromRILInt: bad state: " + state + " use APPSTATE_UNKNOWN");
        }
        return newState;
    }

    public PersoSubState PersoSubstateFromRILInt(int substate) {
        PersoSubState newSubState;
        /* RIL_PeroSubstate ril.h */
        switch(substate) {
            case 0:  newSubState = PersoSubState.PERSOSUBSTATE_UNKNOWN;  break;
            case 1:  newSubState = PersoSubState.PERSOSUBSTATE_IN_PROGRESS; break;
            case 2:  newSubState = PersoSubState.PERSOSUBSTATE_READY; break;
            case 3:  newSubState = PersoSubState.PERSOSUBSTATE_SIM_NETWORK; break;
            case 4:  newSubState = PersoSubState.PERSOSUBSTATE_SIM_NETWORK_SUBSET; break;
            case 5:  newSubState = PersoSubState.PERSOSUBSTATE_SIM_CORPORATE; break;
            case 6:  newSubState = PersoSubState.PERSOSUBSTATE_SIM_SERVICE_PROVIDER; break;
            case 7:  newSubState = PersoSubState.PERSOSUBSTATE_SIM_SIM;  break;
            case 8:  newSubState = PersoSubState.PERSOSUBSTATE_SIM_NETWORK_PUK; break;
            case 9:  newSubState = PersoSubState.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK; break;
            case 10: newSubState = PersoSubState.PERSOSUBSTATE_SIM_CORPORATE_PUK; break;
            case 11: newSubState = PersoSubState.PERSOSUBSTATE_SIM_SERVICE_PROVIDER_PUK; break;
            case 12: newSubState = PersoSubState.PERSOSUBSTATE_SIM_SIM_PUK; break;
            case 13: newSubState = PersoSubState.PERSOSUBSTATE_RUIM_NETWORK1; break;
            case 14: newSubState = PersoSubState.PERSOSUBSTATE_RUIM_NETWORK2; break;
            case 15: newSubState = PersoSubState.PERSOSUBSTATE_RUIM_HRPD; break;
            case 16: newSubState = PersoSubState.PERSOSUBSTATE_RUIM_CORPORATE; break;
            case 17: newSubState = PersoSubState.PERSOSUBSTATE_RUIM_SERVICE_PROVIDER; break;
            case 18: newSubState = PersoSubState.PERSOSUBSTATE_RUIM_RUIM; break;
            case 19: newSubState = PersoSubState.PERSOSUBSTATE_RUIM_NETWORK1_PUK; break;
            case 20: newSubState = PersoSubState.PERSOSUBSTATE_RUIM_NETWORK2_PUK; break;
            case 21: newSubState = PersoSubState.PERSOSUBSTATE_RUIM_HRPD_PUK ; break;
            case 22: newSubState = PersoSubState.PERSOSUBSTATE_RUIM_CORPORATE_PUK; break;
            case 23: newSubState = PersoSubState.PERSOSUBSTATE_RUIM_SERVICE_PROVIDER_PUK; break;
            case 24: newSubState = PersoSubState.PERSOSUBSTATE_RUIM_RUIM_PUK; break;
            case 25: newSubState = PersoSubState.PERSOSUBSTATE_SIM_SPN; break;
            case 26: newSubState = PersoSubState.PERSOSUBSTATE_SIM_SPN_PUK; break;
            case 27: newSubState = PersoSubState.PERSOSUBSTATE_SIM_SP_EHPLMN; break;
            case 28: newSubState = PersoSubState.PERSOSUBSTATE_SIM_SP_EHPLMN_PUK; break;
            case 29: newSubState = PersoSubState.PERSOSUBSTATE_SIM_ICCID; break;
            case 30: newSubState = PersoSubState.PERSOSUBSTATE_SIM_ICCID_PUK; break;
            case 31: newSubState = PersoSubState.PERSOSUBSTATE_SIM_IMPI; break;
            case 32: newSubState = PersoSubState.PERSOSUBSTATE_SIM_IMPI_PUK; break;
            case 33: newSubState = PersoSubState.PERSOSUBSTATE_SIM_NS_SP; break;
            case 34: newSubState = PersoSubState.PERSOSUBSTATE_SIM_NS_SP_PUK; break;

            default:
                newSubState = PersoSubState.PERSOSUBSTATE_UNKNOWN;
                loge("PersoSubstateFromRILInt: bad substate: " + substate
                        + " use PERSOSUBSTATE_UNKNOWN");
        }
        return newSubState;
    }

    public PinState PinStateFromRILInt(int state) {
        PinState newPinState;
        switch(state) {
            case 0:
                newPinState = PinState.PINSTATE_UNKNOWN;
                break;
            case 1:
                newPinState = PinState.PINSTATE_ENABLED_NOT_VERIFIED;
                break;
            case 2:
                newPinState = PinState.PINSTATE_ENABLED_VERIFIED;
                break;
            case 3:
                newPinState = PinState.PINSTATE_DISABLED;
                break;
            case 4:
                newPinState = PinState.PINSTATE_ENABLED_BLOCKED;
                break;
            case 5:
                newPinState = PinState.PINSTATE_ENABLED_PERM_BLOCKED;
                break;
            default:
                newPinState = PinState.PINSTATE_UNKNOWN;
                loge("PinStateFromRILInt: bad pin state: " + state + " use PINSTATE_UNKNOWN");
        }
        return newPinState;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{").append(app_type).append(",").append(app_state);
        if (app_state == AppState.APPSTATE_SUBSCRIPTION_PERSO) {
            sb.append(",").append(perso_substate);
        }
        if (app_type == AppType.APPTYPE_CSIM ||
                app_type == AppType.APPTYPE_USIM ||
                app_type == AppType.APPTYPE_ISIM) {
            sb.append(",pin1=").append(pin1);
            sb.append(",pin2=").append(pin2);
        }
        sb.append("}");
        return sb.toString();
    }

    private void loge(String s) {
        Rlog.e("IccCardApplicationStatus", s);
    }
}
