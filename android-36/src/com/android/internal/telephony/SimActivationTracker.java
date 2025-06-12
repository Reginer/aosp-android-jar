/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.telephony.TelephonyManager.SIM_ACTIVATION_STATE_ACTIVATED;
import static android.telephony.TelephonyManager.SIM_ACTIVATION_STATE_ACTIVATING;
import static android.telephony.TelephonyManager.SIM_ACTIVATION_STATE_DEACTIVATED;
import static android.telephony.TelephonyManager.SIM_ACTIVATION_STATE_RESTRICTED;
import static android.telephony.TelephonyManager.SIM_ACTIVATION_STATE_UNKNOWN;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.LocalLog;
import android.util.Log;

import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SimActivationTracker {
    /**
     * SimActivationTracker(SAT) serves as a central place to keep track of all knowledge of
     * voice & data activation state which is set by custom/default carrier apps.
     * Each phone object maintains a single activation tracker.
     */
    private static final boolean DBG = true;
    private static final String LOG_TAG = "SAT";
    private static final boolean VDBG = Rlog.isLoggable(LOG_TAG, Log.VERBOSE);

    private Phone mPhone;

    /**
     * Voice Activation State
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_UNKNOWN
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_ACTIVATING
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_ACTIVATED
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_DEACTIVATED
     */
    private int mVoiceActivationState;

    /**
     * Data Activation State
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_UNKNOWN
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_ACTIVATING
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_ACTIVATED
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_DEACTIVATED
     * @see android.telephony.TelephonyManager#SIM_ACTIVATION_STATE_RESTRICTED
     */
    private int mDataActivationState;
    private final LocalLog mVoiceActivationStateLog = new LocalLog(8);
    private final LocalLog mDataActivationStateLog = new LocalLog(8);
    private final BroadcastReceiver mReceiver;

    public SimActivationTracker(Phone phone) {
        mPhone = phone;
        mVoiceActivationState = SIM_ACTIVATION_STATE_UNKNOWN;
        mDataActivationState = SIM_ACTIVATION_STATE_UNKNOWN;

        mReceiver = new  BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (VDBG) log("action: " + action);
                if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)){
                    if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(
                            intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE))) {
                        if (DBG) log("onSimAbsent, reset activation state to UNKNOWN");
                        setVoiceActivationState(SIM_ACTIVATION_STATE_UNKNOWN);
                        setDataActivationState(SIM_ACTIVATION_STATE_UNKNOWN);
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        mPhone.getContext().registerReceiver(mReceiver, intentFilter);
    }

    public void setVoiceActivationState(int state) {
        if (!isValidActivationState(state) || (SIM_ACTIVATION_STATE_RESTRICTED == state)) {
            throw new IllegalArgumentException("invalid voice activation state: " + state);
        }
        if (DBG) log("setVoiceActivationState=" + state);
        mVoiceActivationState = state;
        mVoiceActivationStateLog.log(toString(state));
        mPhone.notifyVoiceActivationStateChanged(state);
    }

    public void setDataActivationState(int state) {
        if (!isValidActivationState(state)) {
            throw new IllegalArgumentException("invalid data activation state: " + state);
        }
        if (DBG) log("setDataActivationState=" + state);
        mDataActivationState = state;
        mDataActivationStateLog.log(toString(state));
        mPhone.notifyDataActivationStateChanged(state);
    }

    public int getVoiceActivationState() {
        return mVoiceActivationState;
    }

    public int getDataActivationState() {
        return mDataActivationState;
    }

    private static boolean isValidActivationState(int state) {
        switch (state) {
            case SIM_ACTIVATION_STATE_UNKNOWN:
            case SIM_ACTIVATION_STATE_ACTIVATING:
            case SIM_ACTIVATION_STATE_ACTIVATED:
            case SIM_ACTIVATION_STATE_DEACTIVATED:
            case SIM_ACTIVATION_STATE_RESTRICTED:
                return true;
            default:
                return false;
        }
    }

    private static String toString(int state) {
        switch (state) {
            case SIM_ACTIVATION_STATE_UNKNOWN:
                return "unknown";
            case SIM_ACTIVATION_STATE_ACTIVATING:
                return "activating";
            case SIM_ACTIVATION_STATE_ACTIVATED:
                return "activated";
            case SIM_ACTIVATION_STATE_DEACTIVATED:
                return "deactivated";
            case SIM_ACTIVATION_STATE_RESTRICTED:
                return "restricted";
            default:
                return "invalid";
        }
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, "[" + mPhone.getPhoneId() + "]" + s);
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, "[" + mPhone.getPhoneId() + "]" + s);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        pw.println(" mVoiceActivationState Log:");
        ipw.increaseIndent();
        mVoiceActivationStateLog.dump(fd, ipw, args);
        ipw.decreaseIndent();

        pw.println(" mDataActivationState Log:");
        ipw.increaseIndent();
        mDataActivationStateLog.dump(fd, ipw, args);
        ipw.decreaseIndent();
    }

    public void dispose() {
        mPhone.getContext().unregisterReceiver(mReceiver);
    }
}