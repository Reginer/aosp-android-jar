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
import android.telephony.SubscriptionInfo;

import com.android.internal.telephony.util.TelephonyUtils;
import com.android.telephony.Rlog;

/**
 * See also RIL_CardStatus in include/telephony/ril.h
 *
 * {@hide}
 */
public class IccCardStatus {
    public static final int CARD_MAX_APPS = 8;

    public enum CardState {
        @UnsupportedAppUsage
        CARDSTATE_ABSENT,
        @UnsupportedAppUsage
        CARDSTATE_PRESENT,
        @UnsupportedAppUsage
        CARDSTATE_ERROR,
        CARDSTATE_RESTRICTED;

        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public boolean isCardPresent() {
            return this == CARDSTATE_PRESENT ||
                this == CARDSTATE_RESTRICTED;
        }
    }

    public enum PinState {
        PINSTATE_UNKNOWN,
        PINSTATE_ENABLED_NOT_VERIFIED,
        PINSTATE_ENABLED_VERIFIED,
        @UnsupportedAppUsage
        PINSTATE_DISABLED,
        @UnsupportedAppUsage
        PINSTATE_ENABLED_BLOCKED,
        @UnsupportedAppUsage
        PINSTATE_ENABLED_PERM_BLOCKED;

        boolean isPermBlocked() {
            return this == PINSTATE_ENABLED_PERM_BLOCKED;
        }

        boolean isPinRequired() {
            return this == PINSTATE_ENABLED_NOT_VERIFIED;
        }

        boolean isPukRequired() {
            return this == PINSTATE_ENABLED_BLOCKED;
        }
    }

    @UnsupportedAppUsage
    public CardState  mCardState;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public PinState   mUniversalPinState;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int        mGsmUmtsSubscriptionAppIndex;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int        mCdmaSubscriptionAppIndex;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int        mImsSubscriptionAppIndex;
    public String     atr;
    public String     iccid;
    public String     eid;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public IccCardApplicationStatus[] mApplications;

    public IccSlotPortMapping mSlotPortMapping;

    public void setCardState(int state) {
        switch(state) {
        case 0:
            mCardState = CardState.CARDSTATE_ABSENT;
            break;
        case 1:
            mCardState = CardState.CARDSTATE_PRESENT;
            break;
        case 2:
            mCardState = CardState.CARDSTATE_ERROR;
            break;
        case 3:
            mCardState = CardState.CARDSTATE_RESTRICTED;
            break;
        default:
            throw new RuntimeException("Unrecognized RIL_CardState: " + state);
        }
    }

    public void setUniversalPinState(int state) {
        switch(state) {
        case 0:
            mUniversalPinState = PinState.PINSTATE_UNKNOWN;
            break;
        case 1:
            mUniversalPinState = PinState.PINSTATE_ENABLED_NOT_VERIFIED;
            break;
        case 2:
            mUniversalPinState = PinState.PINSTATE_ENABLED_VERIFIED;
            break;
        case 3:
            mUniversalPinState = PinState.PINSTATE_DISABLED;
            break;
        case 4:
            mUniversalPinState = PinState.PINSTATE_ENABLED_BLOCKED;
            break;
        case 5:
            mUniversalPinState = PinState.PINSTATE_ENABLED_PERM_BLOCKED;
            break;
        default:
            throw new RuntimeException("Unrecognized RIL_PinState: " + state);
        }
    }

    @Override
    public String toString() {
        IccCardApplicationStatus app;

        StringBuilder sb = new StringBuilder();
        sb.append("IccCardState {").append(mCardState).append(",")
        .append(mUniversalPinState);
        if (mApplications != null) {
            sb.append(",num_apps=").append(mApplications.length);
        } else {
            sb.append(",mApplications=null");
        }

        sb.append(",gsm_id=").append(mGsmUmtsSubscriptionAppIndex);
        if (mApplications != null
                && mGsmUmtsSubscriptionAppIndex >= 0
                && mGsmUmtsSubscriptionAppIndex < mApplications.length) {
            app = mApplications[mGsmUmtsSubscriptionAppIndex];
            sb.append(app == null ? "null" : app);
        }

        sb.append(",cdma_id=").append(mCdmaSubscriptionAppIndex);
        if (mApplications != null
                && mCdmaSubscriptionAppIndex >= 0
                && mCdmaSubscriptionAppIndex < mApplications.length) {
            app = mApplications[mCdmaSubscriptionAppIndex];
            sb.append(app == null ? "null" : app);
        }

        sb.append(",ims_id=").append(mImsSubscriptionAppIndex);
        if (mApplications != null
                && mImsSubscriptionAppIndex >= 0
                && mImsSubscriptionAppIndex < mApplications.length) {
            app = mApplications[mImsSubscriptionAppIndex];
            sb.append(app == null ? "null" : app);
        }

        sb.append(",atr=").append(atr);
        sb.append(",iccid=").append(SubscriptionInfo.givePrintableIccid(iccid));
        sb.append(",eid=").append(Rlog.pii(TelephonyUtils.IS_DEBUGGABLE, eid));
        sb.append(",SlotPortMapping=").append(mSlotPortMapping);

        sb.append("}");
        return sb.toString();
    }

}
