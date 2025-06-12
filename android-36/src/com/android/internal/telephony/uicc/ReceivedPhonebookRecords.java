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

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Represents the received ADN entries from the SIM.
 *
 * {@hide}
 */
public class ReceivedPhonebookRecords {
    @PhonebookReceivedState
    private int mPhonebookReceivedState;
    private List<SimPhonebookRecord> mEntries;

    @IntDef(value = {
        RS_OK,
        RS_ERROR,
        RS_ABORT,
        RS_FINAL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface PhonebookReceivedState {}

    public static final int RS_OK = 1;
    public static final int RS_ERROR = 2;
    public static final int RS_ABORT = 3;
    public static final int RS_FINAL = 4;

    public ReceivedPhonebookRecords(@PhonebookReceivedState int state,
            List<SimPhonebookRecord> entries) {
        mPhonebookReceivedState = state;
        mEntries = entries;
    }

    public boolean isCompleted() {
        return mPhonebookReceivedState == RS_FINAL;
    }

    public boolean isRetryNeeded() {
        return mPhonebookReceivedState == RS_ABORT;
    }

    public boolean isOk() {
        return mPhonebookReceivedState == RS_OK;
    }
    public List<SimPhonebookRecord> getPhonebookRecords() {
        return mEntries;
    }
}
