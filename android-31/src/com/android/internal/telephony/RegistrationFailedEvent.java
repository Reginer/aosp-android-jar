/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.annotation.NonNull;
import android.telephony.CellIdentity;
import android.telephony.NetworkRegistrationInfo;

/**
 * Event fired to notify of a registration failure.
 */
public class RegistrationFailedEvent {
    /** The Cell Identity of the cell on which registration failed */
    public final CellIdentity cellIdentity;

    /** The PLMN for that cell on which registration failed */
    public final String chosenPlmn;

    /** The registration domain(s) for this registration attempt */
    @NetworkRegistrationInfo.Domain public final int domain;

    /** The registration cause code */
    public final int causeCode;

    /** The additional cause code in case of a combined procedure */
    public final int additionalCauseCode;

    /** Constructor for this event */
    public RegistrationFailedEvent(@NonNull CellIdentity cellIdentity,
            @NonNull String chosenPlmn, int domain, int causeCode, int additionalCauseCode) {
        this.cellIdentity = cellIdentity;
        this.chosenPlmn = chosenPlmn;
        this.domain = domain;
        this.causeCode = causeCode;
        this.additionalCauseCode = additionalCauseCode;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{CellIdentity=")
                .append(cellIdentity)
                .append(", chosenPlmn=")
                .append(chosenPlmn)
                .append(", domain=")
                .append(domain)
                .append(", causeCode=")
                .append(causeCode)
                .append(", additionalCauseCode=")
                .append(additionalCauseCode)
                .toString();
    }
}
