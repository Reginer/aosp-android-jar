/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.HandlerThread;
import android.telephony.PhysicalChannelConfig;
import android.telephony.PreciseDataConnectionState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.BackgroundThread;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyStatsLog;
import com.android.internal.telephony.flags.Flags;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;

/**
 *  This Tracker is monitoring precise data connection states for each APNs which are used for IMS
 * calling such as IMS and Emergency APN. It uses a SparseArray to track each SIM's connection
 * state.
 *  The tracker is started by {@link VoiceCallSessionStats} and update the states to
 * VoiceCallSessionStats directly.
 */
public class DataConnectionStateTracker {
    private static final SparseArray<DataConnectionStateTracker> sDataConnectionStateTracker =
            new SparseArray<>();
    private final Executor mExecutor;
    private Phone mPhone;
    private int mSubId;
    private HashMap<Integer, PreciseDataConnectionState> mLastPreciseDataConnectionState =
            new HashMap<>();
    private TelephonyListenerImpl mTelephonyListener;
    private int mActiveDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private int mChannelCountEnum = TelephonyStatsLog
            .CONNECTED_CHANNEL_CHANGED__CONNECTED_CHANNEL_COUNT__CHANNEL_COUNT_UNSPECIFIED;

    private final SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionsChangedListener =
            new SubscriptionManager.OnSubscriptionsChangedListener() {
                @Override
                public void onSubscriptionsChanged() {
                    if (mPhone == null) {
                        return;
                    }
                    int newSubId = mPhone.getSubId();
                    if (mSubId == newSubId
                            || newSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        return;
                    }

                    unregisterTelephonyListener();
                    mSubId = newSubId;
                    registerTelephonyListener(mSubId);
                }
            };

    private DataConnectionStateTracker() {
        if (Flags.threadShred()) {
            mExecutor = BackgroundThread.getExecutor();
        } else {
            HandlerThread handlerThread =
                    new HandlerThread(DataConnectionStateTracker.class.getSimpleName());
            handlerThread.start();
            mExecutor = new HandlerExecutor(new Handler(handlerThread.getLooper()));
        }
    }

    /** Getting or Creating DataConnectionStateTracker based on phoneId */
    public static synchronized DataConnectionStateTracker getInstance(int phoneId) {
        DataConnectionStateTracker dataConnectionStateTracker =
                sDataConnectionStateTracker.get(phoneId);
        if (dataConnectionStateTracker != null) {
            return dataConnectionStateTracker;
        }

        dataConnectionStateTracker = new DataConnectionStateTracker();
        sDataConnectionStateTracker.put(phoneId, dataConnectionStateTracker);
        return dataConnectionStateTracker;
    }

    /** Starting to monitor the precise data connection states */
    public void start(Phone phone) {
        mPhone = phone;
        mSubId = mPhone.getSubId();
        registerTelephonyListener(mSubId);
        SubscriptionManager mSubscriptionManager = mPhone.getContext()
                .getSystemService(SubscriptionManager.class);
        if (mSubscriptionManager != null) {
            mSubscriptionManager
                    .addOnSubscriptionsChangedListener(mExecutor, mSubscriptionsChangedListener);
        }
    }

    /** Stopping monitoring for the precise data connection states */
    public void stop() {
        if (mPhone == null) {
            return;
        }
        SubscriptionManager mSubscriptionManager = mPhone.getContext()
                .getSystemService(SubscriptionManager.class);
        if (mSubscriptionManager != null) {
            mSubscriptionManager
                    .removeOnSubscriptionsChangedListener(mSubscriptionsChangedListener);
        }
        unregisterTelephonyListener();
        mPhone = null;
        mLastPreciseDataConnectionState.clear();
    }

    /** Returns data state of the last notified precise data connection state for apn type */
    public int getDataState(int apnType) {
        if (!mLastPreciseDataConnectionState.containsKey(apnType)) {
            return TelephonyManager.DATA_UNKNOWN;
        }
        return mLastPreciseDataConnectionState.get(apnType).getState();
    }

    private void registerTelephonyListener(int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return;
        }
        TelephonyManager telephonyManager =
                mPhone.getContext().getSystemService(TelephonyManager.class);
        if (telephonyManager != null) {
            mTelephonyListener = new TelephonyListenerImpl(mExecutor);
            mTelephonyListener.register(telephonyManager.createForSubscriptionId(subId));
        }
    }

    private void unregisterTelephonyListener() {
        if (mTelephonyListener != null) {
            mTelephonyListener.unregister();
            mTelephonyListener = null;
        }
    }

    @VisibleForTesting
    public void notifyDataConnectionStateChanged(PreciseDataConnectionState connectionState) {
        List<Integer> apnTypes = connectionState.getApnSetting().getApnTypes();
        if (apnTypes != null) {
            for (int apnType : apnTypes) {
                mLastPreciseDataConnectionState.put(apnType, connectionState);
            }
        }

        mPhone.getVoiceCallSessionStats().onPreciseDataConnectionStateChanged(connectionState);
    }

    static int getActiveDataSubId() {
        if (sDataConnectionStateTracker.size() == 0) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        return sDataConnectionStateTracker.valueAt(0).mActiveDataSubId;
    }

    /**
     * Log RAT if the active data subId changes to another subId with a different RAT.
     *
     * @param subId the current active data subId
     */
    private void logRATChanges(int subId) {
        if (mSubId == subId && mActiveDataSubId != subId) {
            int newDataRat = mPhone.getServiceStateTracker()
                    .getServiceStateStats().getCurrentDataRat();
            for (int i = 0; i < sDataConnectionStateTracker.size(); i++) {
                DataConnectionStateTracker dataConnectionStateTracker =
                        sDataConnectionStateTracker.valueAt(0);
                if (dataConnectionStateTracker.mSubId == mActiveDataSubId) {
                    int previousDataRat = dataConnectionStateTracker.mPhone
                            .getServiceStateTracker().getServiceStateStats()
                            .getCurrentDataRat();
                    if (newDataRat != previousDataRat) {
                        TelephonyStatsLog.write(TelephonyStatsLog.DATA_RAT_STATE_CHANGED,
                                newDataRat);
                    }
                }
            }
        }
    }

    private class TelephonyListenerImpl extends TelephonyCallback
            implements TelephonyCallback.PreciseDataConnectionStateListener,
            TelephonyCallback.ActiveDataSubscriptionIdListener,
            TelephonyCallback.PhysicalChannelConfigListener {
        private final Executor mExecutor;
        private TelephonyManager mTelephonyManager = null;

        TelephonyListenerImpl(Executor executor) {
            mExecutor = executor;
        }

        public void register(TelephonyManager tm) {
            if (tm == null) {
                return;
            }
            mTelephonyManager = tm;
            mTelephonyManager.registerTelephonyCallback(mExecutor, this);
        }

        public void unregister() {
            if (mTelephonyManager != null) {
                mTelephonyManager.unregisterTelephonyCallback(this);
                mTelephonyManager = null;
            }
        }

        @Override
        public void onPreciseDataConnectionStateChanged(
                PreciseDataConnectionState connectionState) {
            notifyDataConnectionStateChanged(connectionState);
        }

        @Override
        public void onActiveDataSubscriptionIdChanged(int subId) {
            logRATChanges(subId);
            mActiveDataSubId = subId;
        }

        @Override
        public void onPhysicalChannelConfigChanged(List<PhysicalChannelConfig> configs) {
            logChannelChange(configs);
        }

        /** Log channel number if it changes for active data subscription*/
        private void logChannelChange(List<PhysicalChannelConfig> configs) {
            int connectedChannelCount = configs.size();
            int channelCountEnum = TelephonyStatsLog
                    .CONNECTED_CHANNEL_CHANGED__CONNECTED_CHANNEL_COUNT__CHANNEL_COUNT_UNSPECIFIED;
            switch(connectedChannelCount) {
                case 0:
                    channelCountEnum = TelephonyStatsLog
                            .CONNECTED_CHANNEL_CHANGED__CONNECTED_CHANNEL_COUNT__CHANNEL_COUNT_ONE;
                    break;
                case 1:
                    channelCountEnum = TelephonyStatsLog
                            .CONNECTED_CHANNEL_CHANGED__CONNECTED_CHANNEL_COUNT__CHANNEL_COUNT_ONE;
                    break;
                case 2:
                    channelCountEnum = TelephonyStatsLog
                            .CONNECTED_CHANNEL_CHANGED__CONNECTED_CHANNEL_COUNT__CHANNEL_COUNT_TWO;
                    break;
                case 3:
                    channelCountEnum = TelephonyStatsLog
                            .CONNECTED_CHANNEL_CHANGED__CONNECTED_CHANNEL_COUNT__CHANNEL_COUNT_THREE;
                    break;
                case 4:
                    channelCountEnum = TelephonyStatsLog
                            .CONNECTED_CHANNEL_CHANGED__CONNECTED_CHANNEL_COUNT__CHANNEL_COUNT_FOUR;
                    break;
                // Greater than 4
                default:
                    channelCountEnum = TelephonyStatsLog
                            .CONNECTED_CHANNEL_CHANGED__CONNECTED_CHANNEL_COUNT__CHANNEL_COUNT_FIVE;
            }
            if (mChannelCountEnum != channelCountEnum) {
                if (mSubId != mActiveDataSubId) {
                    TelephonyStatsLog.write(TelephonyStatsLog.CONNECTED_CHANNEL_CHANGED,
                            channelCountEnum);
                }
                mChannelCountEnum = channelCountEnum;
            }
        }
    }
}
