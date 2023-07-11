/*
 * Copyright (C) 2021 The Android Open Source Project
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

import java.util.Objects;

public class IccSimPortInfo {

    public String mIccId;      /* iccid of the currently enabled profile */
    public int mLogicalSlotIndex; /* logical slotId of the active slot */
    public boolean mPortActive;     /* port state in the slot */

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        IccSimPortInfo that = (IccSimPortInfo) obj;
        return (mPortActive == that.mPortActive)
                && (mLogicalSlotIndex == that.mLogicalSlotIndex)
                && (TextUtils.equals(mIccId, that.mIccId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPortActive,  mLogicalSlotIndex, mIccId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{").append("iccid=")
                .append(SubscriptionInfo.givePrintableIccid(mIccId)).append(",")
                .append("logicalSlotIndex=").append(mLogicalSlotIndex).append(",")
                .append("portActive=").append(mPortActive)
                .append("}");
        return sb.toString();
    }
}