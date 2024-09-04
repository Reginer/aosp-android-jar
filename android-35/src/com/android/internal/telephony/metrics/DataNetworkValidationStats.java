/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.annotation.ElapsedRealtimeLong;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.SystemClock;
import android.telephony.Annotation.ApnType;
import android.telephony.Annotation.NetworkType;
import android.telephony.PreciseDataConnectionState;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyStatsLog;
import com.android.internal.telephony.nano.PersistAtomsProto.DataNetworkValidation;

/**
 * DataNetworkValidationStats logs the atoms for response after a validation request from the
 * DataNetwork in framework.
 */
public class DataNetworkValidationStats {

    private static final String TAG = DataNetworkValidationStats.class.getSimpleName();

    @NonNull
    private final Phone mPhone;

    @NonNull
    private final PersistAtomsStorage mAtomsStorage =
            PhoneFactory.getMetricsCollector().getAtomsStorage();

    @Nullable
    private DataNetworkValidation mDataNetworkValidation;

    @ElapsedRealtimeLong
    private long mRequestedTimeInMillis;

    /** constructor */
    public DataNetworkValidationStats(@NonNull Phone phone) {
        mPhone = phone;
    }


    /**
     * Create a new ongoing atom when NetworkValidation requested.
     *
     * Create a data network validation proto for a new atom record and write the start time to
     * calculate the elapsed time required.
     *
     * @param apnTypeBitMask APN type bitmask of DataNetwork.
     */
    public void onRequestNetworkValidation(@ApnType int apnTypeBitMask) {
        if (mDataNetworkValidation == null) {
            mDataNetworkValidation = getDefaultProto(apnTypeBitMask);
            mRequestedTimeInMillis = getTimeMillis();
        }
    }

    /** Mark the Handover Attempt field as true if validation was requested */
    public void onHandoverAttempted() {
        if (mDataNetworkValidation != null) {
            mDataNetworkValidation.handoverAttempted = true;
        }
    }

    /**
     * Called when data network is disconnected.
     *
     * Since network validation is based on the data network, validation must also end when the data
     * network is disconnected. At this time, validation has not been completed, save an atom as
     * unspecified. and clear.
     *
     * @param networkType Current Network Type of the Data Network.
     */
    public void onDataNetworkDisconnected(@NetworkType int networkType) {
        // Nothing to do, if never requested validation
        if (mDataNetworkValidation == null) {
            return;
        }

        // Set data for and atom.
        calcElapsedTime();
        mDataNetworkValidation.networkType = networkType;
        mDataNetworkValidation.signalStrength = mPhone.getSignalStrength().getLevel();
        mDataNetworkValidation.validationResult = TelephonyStatsLog
                .DATA_NETWORK_VALIDATION__VALIDATION_RESULT__VALIDATION_RESULT_UNSPECIFIED;

        // Store.
        mAtomsStorage.addDataNetworkValidation(mDataNetworkValidation);

        // clear all values.
        clear();
    }

    /**
     * Store an atom by updated state.
     *
     * Called when the validation status is updated, and saves the atom when a failure or success
     * result is received.
     *
     * @param status Data Network Validation Status.
     * @param networkType Current Network Type of the Data Network.
     */
    public void onUpdateNetworkValidationState(
            @PreciseDataConnectionState.NetworkValidationStatus int status,
            @NetworkType int networkType) {
        // Nothing to do, if never requested validation
        if (mDataNetworkValidation == null) {
            return;
        }

        switch (status) {
            // Immediately after requesting validation, these messages may occur. In this case,
            // ignore it and wait for the next update.
            case PreciseDataConnectionState.NETWORK_VALIDATION_NOT_REQUESTED: // fall-through
            case PreciseDataConnectionState.NETWORK_VALIDATION_IN_PROGRESS:
                return;
            // If status is unsupported, NetworkValidation should not be requested initially. logs
            // this for abnormal tracking.
            case PreciseDataConnectionState.NETWORK_VALIDATION_UNSUPPORTED:
                mDataNetworkValidation.validationResult = TelephonyStatsLog
                    .DATA_NETWORK_VALIDATION__VALIDATION_RESULT__VALIDATION_RESULT_NOT_SUPPORTED;
                break;
            // Success or failure corresponds to the result, store an atom.
            case PreciseDataConnectionState.NETWORK_VALIDATION_SUCCESS:
            case PreciseDataConnectionState.NETWORK_VALIDATION_FAILURE:
                mDataNetworkValidation.validationResult = status;
                break;
        }

        // Set data for and atom.
        calcElapsedTime();
        mDataNetworkValidation.networkType = networkType;
        mDataNetworkValidation.signalStrength = mPhone.getSignalStrength().getLevel();

        // Store.
        mAtomsStorage.addDataNetworkValidation(mDataNetworkValidation);

        // clear all values.
        clear();
    }

    /**
     * Calculate the current time required based on when network validation is requested.
     */
    private void calcElapsedTime() {
        if (mDataNetworkValidation != null && mRequestedTimeInMillis != 0) {
            mDataNetworkValidation.elapsedTimeInMillis = getTimeMillis() - mRequestedTimeInMillis;
        }
    }

    /**
     * Returns current time in millis from boot.
     */
    @VisibleForTesting
    @ElapsedRealtimeLong
    protected long getTimeMillis() {
        return SystemClock.elapsedRealtime();
    }

    /**
     * Clear all values.
     */
    private void clear() {
        mDataNetworkValidation = null;
        mRequestedTimeInMillis = 0;
    }


    /** Creates a DataNetworkValidation proto with default values. */
    @NonNull
    private DataNetworkValidation getDefaultProto(@ApnType int apnTypeBitmask) {
        DataNetworkValidation proto = new DataNetworkValidation();
        proto.networkType =
                TelephonyStatsLog.DATA_NETWORK_VALIDATION__NETWORK_TYPE__NETWORK_TYPE_UNKNOWN;
        proto.apnTypeBitmask = apnTypeBitmask;
        proto.signalStrength =
                TelephonyStatsLog
                        .DATA_NETWORK_VALIDATION__SIGNAL_STRENGTH__SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        proto.validationResult =
                TelephonyStatsLog
                        .DATA_NETWORK_VALIDATION__VALIDATION_RESULT__VALIDATION_RESULT_UNSPECIFIED;
        proto.elapsedTimeInMillis = 0;
        proto.handoverAttempted = false;
        proto.networkValidationCount = 1;
        return proto;
    }
}

