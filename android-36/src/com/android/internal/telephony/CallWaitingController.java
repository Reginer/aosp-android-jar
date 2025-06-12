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

package com.android.internal.telephony;

import static android.telephony.CarrierConfigManager.ImsSs.CALL_WAITING_SYNC_FIRST_CHANGE;
import static android.telephony.CarrierConfigManager.ImsSs.CALL_WAITING_SYNC_FIRST_POWER_UP;
import static android.telephony.CarrierConfigManager.ImsSs.CALL_WAITING_SYNC_IMS_ONLY;
import static android.telephony.CarrierConfigManager.ImsSs.CALL_WAITING_SYNC_NONE;
import static android.telephony.CarrierConfigManager.ImsSs.CALL_WAITING_SYNC_USER_CHANGE;
import static android.telephony.CarrierConfigManager.ImsSs.KEY_TERMINAL_BASED_CALL_WAITING_DEFAULT_ENABLED_BOOL;
import static android.telephony.CarrierConfigManager.ImsSs.KEY_TERMINAL_BASED_CALL_WAITING_SYNC_TYPE_INT;
import static android.telephony.CarrierConfigManager.ImsSs.KEY_UT_TERMINAL_BASED_SERVICES_INT_ARRAY;
import static android.telephony.CarrierConfigManager.ImsSs.SUPPLEMENTARY_SERVICE_CW;

import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_NONE;
import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_VOICE;

import android.annotation.Nullable;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.PrintWriter;

/**
 * Controls the change of the user setting of the call waiting service
 */
public class CallWaitingController extends Handler {

    public static final String LOG_TAG = "CallWaitingCtrl";
    private static final boolean DBG = false; /* STOPSHIP if true */

    // Terminal-based call waiting is not supported. */
    public static final int TERMINAL_BASED_NOT_SUPPORTED = -1;
    // Terminal-based call waiting is supported but not activated. */
    public static final int TERMINAL_BASED_NOT_ACTIVATED = 0;
    // Terminal-based call waiting is supported and activated. */
    public static final int TERMINAL_BASED_ACTIVATED = 1;

    private static final int EVENT_SET_CALL_WAITING_DONE = 1;
    private static final int EVENT_GET_CALL_WAITING_DONE = 2;
    private static final int EVENT_REGISTERED_TO_NETWORK = 3;

    // Class to pack mOnComplete object passed by the caller
    private static class Cw {
        final boolean mEnable;
        final Message mOnComplete;
        final boolean mImsRegistered;

        Cw(boolean enable, boolean imsRegistered, Message onComplete) {
            mEnable = enable;
            mOnComplete = onComplete;
            mImsRegistered = imsRegistered;
        }
    }

    @VisibleForTesting
    public static final String PREFERENCE_TBCW = "terminal_based_call_waiting";
    @VisibleForTesting
    public static final String KEY_SUB_ID = "subId";
    @VisibleForTesting
    public static final String KEY_STATE = "state";
    @VisibleForTesting
    public static final String KEY_CS_SYNC = "cs_sync";

    private final CarrierConfigManager.CarrierConfigChangeListener mCarrierConfigChangeListener =
            (slotIndex, subId, carrierId, specificCarrierId) -> onCarrierConfigurationChanged(
                    slotIndex);

    private boolean mSupportedByImsService = false;
    private boolean mValidSubscription = false;

    // The user's last setting of terminal-based call waiting
    private int mCallWaitingState = TERMINAL_BASED_NOT_SUPPORTED;

    private int mSyncPreference = CALL_WAITING_SYNC_NONE;
    private int mLastSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    private boolean mCsEnabled = false;
    private boolean mRegisteredForNetworkAttach = false;
    private boolean mImsRegistered = false;

    private final GsmCdmaPhone mPhone;
    private final ServiceStateTracker mSST;
    private final Context mContext;

    // Constructors
    public CallWaitingController(GsmCdmaPhone phone) {
        mPhone = phone;
        mSST = phone.getServiceStateTracker();
        mContext = phone.getContext();
    }

    private void initialize() {
        CarrierConfigManager ccm = mContext.getSystemService(CarrierConfigManager.class);
        if (ccm != null) {
            // Callback directly handle carrier config change should be executed in handler thread
            ccm.registerCarrierConfigChangeListener(this::post, mCarrierConfigChangeListener);
        } else {
            loge("CarrierConfigLoader is not available.");
        }

        int phoneId = mPhone.getPhoneId();
        int subId = mPhone.getSubId();
        SharedPreferences sp =
                mContext.getSharedPreferences(PREFERENCE_TBCW, Context.MODE_PRIVATE);
        mLastSubId = sp.getInt(KEY_SUB_ID + phoneId, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mCallWaitingState = sp.getInt(KEY_STATE + subId, TERMINAL_BASED_NOT_SUPPORTED);
        mSyncPreference = sp.getInt(KEY_CS_SYNC + phoneId, CALL_WAITING_SYNC_NONE);

        logi("initialize phoneId=" + phoneId
                + ", lastSubId=" + mLastSubId + ", subId=" + subId
                + ", state=" + mCallWaitingState + ", sync=" + mSyncPreference
                + ", csEnabled=" + mCsEnabled);
    }

    /**
     * Returns the cached user setting.
     *
     * Possible values are
     * {@link #TERMINAL_BASED_NOT_SUPPORTED},
     * {@link #TERMINAL_BASED_NOT_ACTIVATED}, and
     * {@link #TERMINAL_BASED_ACTIVATED}.
     *
     * @param forCsOnly indicates the caller expects the result for CS calls only
     */
    @VisibleForTesting
    public synchronized int getTerminalBasedCallWaitingState(boolean forCsOnly) {
        if (forCsOnly && (!mImsRegistered) && mSyncPreference == CALL_WAITING_SYNC_IMS_ONLY) {
            return TERMINAL_BASED_NOT_SUPPORTED;
        }
        if (!mValidSubscription) return TERMINAL_BASED_NOT_SUPPORTED;
        return mCallWaitingState;
    }

    /**
     * Serves the user's requests to interrogate the call waiting service
     *
     * @return true when terminal-based call waiting is supported, otherwise false
     */
    @VisibleForTesting
    public synchronized boolean getCallWaiting(@Nullable Message onComplete) {
        if (mCallWaitingState == TERMINAL_BASED_NOT_SUPPORTED) return false;

        logi("getCallWaiting " + mCallWaitingState);

        if (mSyncPreference == CALL_WAITING_SYNC_FIRST_CHANGE) {
            // Interrogate CW in CS network
            if (!mCsEnabled) {
                // skip interrogation if CS is not available and IMS is registered
                if (isCircuitSwitchedNetworkAvailable() || !isImsRegistered()) {
                    Cw cw = new Cw(false, isImsRegistered(), onComplete);
                    Message resp = obtainMessage(EVENT_GET_CALL_WAITING_DONE, 0, 0, cw);
                    mPhone.mCi.queryCallWaiting(SERVICE_CLASS_NONE, resp);
                    return true;
                }
            }
        }

        if (mSyncPreference == CALL_WAITING_SYNC_NONE
                || mSyncPreference == CALL_WAITING_SYNC_FIRST_CHANGE
                || mSyncPreference == CALL_WAITING_SYNC_FIRST_POWER_UP
                || isSyncImsOnly()) {
            sendGetCallWaitingResponse(onComplete);
            return true;
        } else if (mSyncPreference == CALL_WAITING_SYNC_USER_CHANGE
                || mSyncPreference == CALL_WAITING_SYNC_IMS_ONLY) {
            Cw cw = new Cw(false, isImsRegistered(), onComplete);
            Message resp = obtainMessage(EVENT_GET_CALL_WAITING_DONE, 0, 0, cw);
            mPhone.mCi.queryCallWaiting(SERVICE_CLASS_NONE, resp);
            return true;
        }

        return false;
    }

    /**
     * Serves the user's requests to set the call waiting service
     *
     * @param serviceClass the target service class. Values are CommandsInterface.SERVICE_CLASS_*.
     * @return true when terminal-based call waiting is supported, otherwise false
     */
    @VisibleForTesting
    public synchronized boolean setCallWaiting(boolean enable,
            int serviceClass, @Nullable Message onComplete) {
        if (mCallWaitingState == TERMINAL_BASED_NOT_SUPPORTED) return false;

        if ((serviceClass & SERVICE_CLASS_VOICE) != SERVICE_CLASS_VOICE) return false;

        logi("setCallWaiting enable=" + enable + ", service=" + serviceClass);

        if (mSyncPreference == CALL_WAITING_SYNC_FIRST_CHANGE) {
            // Enable CW in the CS network
            if (!mCsEnabled && enable) {
                if (isCircuitSwitchedNetworkAvailable() || !isImsRegistered()) {
                    Cw cw = new Cw(true, isImsRegistered(), onComplete);
                    Message resp = obtainMessage(EVENT_SET_CALL_WAITING_DONE, 0, 0, cw);
                    mPhone.mCi.setCallWaiting(true, serviceClass, resp);
                    return true;
                } else {
                    // CS network is not available, however, IMS is registered.
                    // Enabling the service in the CS network will be delayed.
                    registerForNetworkAttached();
                }
            }
        }

        if (mSyncPreference == CALL_WAITING_SYNC_NONE
                || mSyncPreference == CALL_WAITING_SYNC_FIRST_CHANGE
                || mSyncPreference == CALL_WAITING_SYNC_FIRST_POWER_UP
                || isSyncImsOnly()) {
            updateState(
                    enable ? TERMINAL_BASED_ACTIVATED : TERMINAL_BASED_NOT_ACTIVATED);

            sendToTarget(onComplete, null, null);
            return true;
        } else if (mSyncPreference == CALL_WAITING_SYNC_USER_CHANGE
                || mSyncPreference == CALL_WAITING_SYNC_IMS_ONLY) {
            Cw cw = new Cw(enable, isImsRegistered(), onComplete);
            Message resp = obtainMessage(EVENT_SET_CALL_WAITING_DONE, 0, 0, cw);
            mPhone.mCi.setCallWaiting(enable, serviceClass, resp);
            return true;
        }

        return false;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_SET_CALL_WAITING_DONE:
                onSetCallWaitingDone((AsyncResult) msg.obj);
                break;
            case EVENT_GET_CALL_WAITING_DONE:
                onGetCallWaitingDone((AsyncResult) msg.obj);
                break;
            case EVENT_REGISTERED_TO_NETWORK:
                onRegisteredToNetwork();
                break;
            default:
                break;
        }
    }

    private synchronized void onSetCallWaitingDone(AsyncResult ar) {
        if (ar.userObj == null) {
            // For the case, CALL_WAITING_SYNC_FIRST_POWER_UP
            if (DBG) logd("onSetCallWaitingDone to sync on network attached");
            if (ar.exception == null) {
                updateSyncState(true);
            } else {
                loge("onSetCallWaitingDone e=" + ar.exception);
            }
            return;
        }

        if (!(ar.userObj instanceof Cw)) {
            // Unexpected state
            if (DBG) logd("onSetCallWaitingDone unexpected result");
            return;
        }

        if (DBG) logd("onSetCallWaitingDone");
        Cw cw = (Cw) ar.userObj;

        if (mSyncPreference == CALL_WAITING_SYNC_IMS_ONLY) {
            // do not synchronize service state between CS and IMS
            sendToTarget(cw.mOnComplete, ar.result, ar.exception);
            return;
        }

        if (ar.exception == null) {
            if (mSyncPreference == CALL_WAITING_SYNC_FIRST_CHANGE) {
                // SYNC_FIRST_CHANGE implies cw.mEnable is true.
                updateSyncState(true);
            }
            updateState(
                    cw.mEnable ? TERMINAL_BASED_ACTIVATED : TERMINAL_BASED_NOT_ACTIVATED);
        } else if (mSyncPreference == CALL_WAITING_SYNC_FIRST_CHANGE) {
            if (cw.mImsRegistered) {
                // IMS is registered. Do not notify error.
                // SYNC_FIRST_CHANGE implies cw.mEnable is true.
                updateState(TERMINAL_BASED_ACTIVATED);
                sendToTarget(cw.mOnComplete, null, null);
                return;
            }
        }
        sendToTarget(cw.mOnComplete, ar.result, ar.exception);
    }

    private synchronized void onGetCallWaitingDone(AsyncResult ar) {
        if (ar.userObj == null) {
            // For the case, CALL_WAITING_SYNC_FIRST_POWER_UP
            if (DBG) logd("onGetCallWaitingDone to sync on network attached");
            boolean enabled = false;
            if (ar.exception == null) {
                //resp[0]: 1 if enabled, 0 otherwise
                //resp[1]: bitwise ORs of SERVICE_CLASS_* constants
                int[] resp = (int[]) ar.result;
                if (resp != null && resp.length > 1) {
                    enabled = (resp[0] == 1)
                            && (resp[1] & SERVICE_CLASS_VOICE) == SERVICE_CLASS_VOICE;
                } else {
                    loge("onGetCallWaitingDone unexpected response");
                }
            } else {
                loge("onGetCallWaitingDone e=" + ar.exception);
            }
            if (enabled) {
                updateSyncState(true);
            } else {
                logi("onGetCallWaitingDone enabling CW service in CS network");
                mPhone.mCi.setCallWaiting(true, SERVICE_CLASS_VOICE,
                        obtainMessage(EVENT_SET_CALL_WAITING_DONE));
            }
            unregisterForNetworkAttached();
            return;
        }

        if (!(ar.userObj instanceof Cw)) {
            // Unexpected state
            if (DBG) logd("onGetCallWaitingDone unexpected result");
            return;
        }

        if (DBG) logd("onGetCallWaitingDone");
        Cw cw = (Cw) ar.userObj;

        if (mSyncPreference == CALL_WAITING_SYNC_IMS_ONLY) {
            // do not synchronize service state between CS and IMS
            sendToTarget(cw.mOnComplete, ar.result, ar.exception);
            return;
        }

        if (ar.exception == null) {
            int[] resp = (int[]) ar.result;
            //resp[0]: 1 if enabled, 0 otherwise
            //resp[1]: bitwise ORs of SERVICE_CLASS_
            if (resp == null || resp.length < 2) {
                logi("onGetCallWaitingDone unexpected response");
                if (mSyncPreference == CALL_WAITING_SYNC_FIRST_CHANGE) {
                    // no exception but unexpected response, local setting is preferred.
                    sendGetCallWaitingResponse(cw.mOnComplete);
                } else {
                    sendToTarget(cw.mOnComplete, ar.result, ar.exception);
                }
                return;
            }

            boolean enabled = resp[0] == 1
                    && (resp[1] & SERVICE_CLASS_VOICE) == SERVICE_CLASS_VOICE;

            if (mSyncPreference == CALL_WAITING_SYNC_FIRST_CHANGE) {
                updateSyncState(enabled);

                if (!enabled && !cw.mImsRegistered) {
                    // IMS is not registered, change the local setting
                    logi("onGetCallWaitingDone CW in CS network is disabled.");
                    updateState(TERMINAL_BASED_NOT_ACTIVATED);
                }

                // return the user setting saved
                sendGetCallWaitingResponse(cw.mOnComplete);
                return;
            }
            updateState(enabled ? TERMINAL_BASED_ACTIVATED : TERMINAL_BASED_NOT_ACTIVATED);
        } else if (mSyncPreference == CALL_WAITING_SYNC_FIRST_CHANGE) {
            // Got an exception
            if (cw.mImsRegistered) {
                // queryCallWaiting failed. However, IMS is registered. Do not notify error.
                // return the user setting saved
                logi("onGetCallWaitingDone get an exception, but IMS is registered");
                sendGetCallWaitingResponse(cw.mOnComplete);
                return;
            }
        }
        sendToTarget(cw.mOnComplete, ar.result, ar.exception);
    }

    private void sendToTarget(Message onComplete, Object result, Throwable exception) {
        if (onComplete != null) {
            AsyncResult.forMessage(onComplete, result, exception);
            onComplete.sendToTarget();
        }
    }

    private void sendGetCallWaitingResponse(Message onComplete) {
        if (onComplete != null) {
            int serviceClass = SERVICE_CLASS_NONE;
            if (mCallWaitingState == TERMINAL_BASED_ACTIVATED) {
                serviceClass = SERVICE_CLASS_VOICE;
            }
            sendToTarget(onComplete, new int[] { mCallWaitingState, serviceClass }, null);
        }
    }

    private synchronized void onRegisteredToNetwork() {
        if (mCsEnabled) return;

        if (DBG) logd("onRegisteredToNetwork");

        mPhone.mCi.queryCallWaiting(SERVICE_CLASS_NONE,
                obtainMessage(EVENT_GET_CALL_WAITING_DONE));
    }

    private synchronized void onCarrierConfigurationChanged(int slotIndex) {
        if (slotIndex != mPhone.getPhoneId()) return;

        int subId = mPhone.getSubId();
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            logi("onCarrierConfigChanged invalid subId=" + subId);

            mValidSubscription = false;
            unregisterForNetworkAttached();
            return;
        }

        if (!updateCarrierConfig(subId, false /* ignoreSavedState */)) {
            return;
        }

        logi("onCarrierConfigChanged cs_enabled=" + mCsEnabled);

        if (mSyncPreference == CALL_WAITING_SYNC_FIRST_POWER_UP) {
            if (!mCsEnabled) {
                registerForNetworkAttached();
            }
        }
    }

    /**
     * @param ignoreSavedState only used for test
     * @return true when succeeded.
     */
    @VisibleForTesting
    public boolean updateCarrierConfig(int subId, boolean ignoreSavedState) {
        mValidSubscription = true;

        PersistableBundle b =
                CarrierConfigManager.getCarrierConfigSubset(
                        mContext,
                        subId,
                        KEY_UT_TERMINAL_BASED_SERVICES_INT_ARRAY,
                        KEY_TERMINAL_BASED_CALL_WAITING_SYNC_TYPE_INT,
                        KEY_TERMINAL_BASED_CALL_WAITING_DEFAULT_ENABLED_BOOL);
        if (b.isEmpty()) return false;

        boolean supportsTerminalBased = false;
        int[] services = b.getIntArray(KEY_UT_TERMINAL_BASED_SERVICES_INT_ARRAY);
        if (services != null) {
            for (int service : services) {
                if (service == SUPPLEMENTARY_SERVICE_CW) {
                    supportsTerminalBased = true;
                }
            }
        }
        int syncPreference = b.getInt(KEY_TERMINAL_BASED_CALL_WAITING_SYNC_TYPE_INT,
                CALL_WAITING_SYNC_FIRST_CHANGE);
        boolean activated = b.getBoolean(KEY_TERMINAL_BASED_CALL_WAITING_DEFAULT_ENABLED_BOOL);
        int defaultState = supportsTerminalBased
                ? (activated ? TERMINAL_BASED_ACTIVATED : TERMINAL_BASED_NOT_ACTIVATED)
                : TERMINAL_BASED_NOT_SUPPORTED;
        int savedState = getSavedState(subId);

        if (DBG) {
            logd("updateCarrierConfig phoneId=" + mPhone.getPhoneId()
                    + ", subId=" + subId + ", support=" + supportsTerminalBased
                    + ", sync=" + syncPreference + ", default=" + defaultState
                    + ", savedState=" + savedState);
        }

        int desiredState = savedState;

        if (ignoreSavedState) {
            desiredState = defaultState;
        } else if ((mLastSubId != subId)
                && (syncPreference == CALL_WAITING_SYNC_FIRST_POWER_UP
                        || syncPreference == CALL_WAITING_SYNC_FIRST_CHANGE)) {
            desiredState = defaultState;
        } else {
            if (defaultState == TERMINAL_BASED_NOT_SUPPORTED) {
                desiredState = TERMINAL_BASED_NOT_SUPPORTED;
            } else if (savedState == TERMINAL_BASED_NOT_SUPPORTED) {
                desiredState = defaultState;
            }
        }

        updateState(desiredState, syncPreference, ignoreSavedState);
        return true;
    }

    private void updateState(int state) {
        updateState(state, mSyncPreference, false);
    }

    private void updateState(int state, int syncPreference, boolean ignoreSavedState) {
        int subId = mPhone.getSubId();

        if (mLastSubId == subId
                && mCallWaitingState == state
                && mSyncPreference == syncPreference
                && (!ignoreSavedState)) {
            return;
        }

        int phoneId = mPhone.getPhoneId();

        logi("updateState phoneId=" + phoneId
                + ", subId=" + subId + ", state=" + state
                + ", sync=" + syncPreference + ", ignoreSavedState=" + ignoreSavedState);

        SharedPreferences sp =
                mContext.getSharedPreferences(PREFERENCE_TBCW, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(KEY_SUB_ID + phoneId, subId);
        editor.putInt(KEY_STATE + subId, state);
        editor.putInt(KEY_CS_SYNC + phoneId, syncPreference);
        editor.apply();

        mCallWaitingState = state;
        mLastSubId = subId;
        mSyncPreference = syncPreference;
        if (mLastSubId != subId) {
            mCsEnabled = false;
        }

        mPhone.setTerminalBasedCallWaitingStatus(mCallWaitingState);
    }

    private int getSavedState(int subId) {
        SharedPreferences sp =
                mContext.getSharedPreferences(PREFERENCE_TBCW, Context.MODE_PRIVATE);
        int state = sp.getInt(KEY_STATE + subId, TERMINAL_BASED_NOT_SUPPORTED);

        logi("getSavedState subId=" + subId + ", state=" + state);

        return state;
    }

    private void updateSyncState(boolean enabled) {
        int phoneId = mPhone.getPhoneId();

        logi("updateSyncState phoneId=" + phoneId + ", enabled=" + enabled);

        mCsEnabled = enabled;
    }

    /**
     * @return whether the service is enabled in the CS network
     */
    @VisibleForTesting
    public boolean getSyncState() {
        return mCsEnabled;
    }

    private boolean isCircuitSwitchedNetworkAvailable() {
        logi("isCircuitSwitchedNetworkAvailable="
                + (mSST.getServiceState().getState() == ServiceState.STATE_IN_SERVICE));
        return mSST.getServiceState().getState() == ServiceState.STATE_IN_SERVICE;
    }

    private boolean isImsRegistered() {
        logi("isImsRegistered " + mImsRegistered);
        return mImsRegistered;
    }

    /**
     * Sets the registration state of IMS service.
     */
    public synchronized void setImsRegistrationState(boolean registered) {
        logi("setImsRegistrationState prev=" + mImsRegistered
                + ", new=" + registered);
        mImsRegistered = registered;
    }

    private void registerForNetworkAttached() {
        logi("registerForNetworkAttached");
        if (mRegisteredForNetworkAttach) return;

        mSST.registerForNetworkAttached(this, EVENT_REGISTERED_TO_NETWORK, null);
        mRegisteredForNetworkAttach = true;
    }

    private void unregisterForNetworkAttached() {
        logi("unregisterForNetworkAttached");
        if (!mRegisteredForNetworkAttach) return;

        mSST.unregisterForNetworkAttached(this);
        removeMessages(EVENT_REGISTERED_TO_NETWORK);
        mRegisteredForNetworkAttach = false;
    }

    /**
     * Sets whether the device supports the terminal-based call waiting.
     * Only for test
     */
    @VisibleForTesting
    public synchronized void setTerminalBasedCallWaitingSupported(boolean supported) {
        if (mSupportedByImsService == supported) return;

        logi("setTerminalBasedCallWaitingSupported " + supported);

        mSupportedByImsService = supported;

        if (supported) {
            initialize();
            onCarrierConfigurationChanged(mPhone.getPhoneId());
        } else {
            CarrierConfigManager ccm = mContext.getSystemService(CarrierConfigManager.class);
            if (ccm != null && mCarrierConfigChangeListener != null) {
                ccm.unregisterCarrierConfigChangeListener(mCarrierConfigChangeListener);
            }
            updateState(TERMINAL_BASED_NOT_SUPPORTED);
        }
    }

    /**
     * Notifies that the UE has attached to the network
     * Only for test
     */
    @VisibleForTesting
    public void notifyRegisteredToNetwork() {
        sendEmptyMessage(EVENT_REGISTERED_TO_NETWORK);
    }

    private boolean isSyncImsOnly() {
        return (mSyncPreference == CALL_WAITING_SYNC_IMS_ONLY && mImsRegistered);
    }

    /**
     * Dump this instance into a readable format for dumpsys usage.
     */
    public void dump(PrintWriter printWriter) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.increaseIndent();
        pw.println("CallWaitingController:");
        pw.println(" mSupportedByImsService=" + mSupportedByImsService);
        pw.println(" mValidSubscription=" + mValidSubscription);
        pw.println(" mCallWaitingState=" + mCallWaitingState);
        pw.println(" mSyncPreference=" + mSyncPreference);
        pw.println(" mLastSubId=" + mLastSubId);
        pw.println(" mCsEnabled=" + mCsEnabled);
        pw.println(" mRegisteredForNetworkAttach=" + mRegisteredForNetworkAttach);
        pw.println(" mImsRegistered=" + mImsRegistered);
        pw.decreaseIndent();
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, "[" + mPhone.getPhoneId() + "] " + msg);
    }

    private void logi(String msg) {
        Rlog.i(LOG_TAG, "[" + mPhone.getPhoneId() + "] " + msg);
    }

    private void logd(String msg) {
        Rlog.d(LOG_TAG, "[" + mPhone.getPhoneId() + "] " + msg);
    }
}
