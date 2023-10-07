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

package com.android.internal.telephony.imsphone;

import static android.telephony.CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA;
import static android.telephony.CarrierConfigManager.Ims.KEY_NR_SA_DISABLE_POLICY_INT;
import static android.telephony.CarrierConfigManager.Ims.NR_SA_DISABLE_POLICY_NONE;
import static android.telephony.CarrierConfigManager.Ims.NR_SA_DISABLE_POLICY_VOWIFI_REGISTERED;
import static android.telephony.CarrierConfigManager.Ims.NR_SA_DISABLE_POLICY_WFC_ESTABLISHED;
import static android.telephony.CarrierConfigManager.Ims.NR_SA_DISABLE_POLICY_WFC_ESTABLISHED_WHEN_VONR_DISABLED;
import static android.telephony.CarrierConfigManager.Ims.NrSaDisablePolicy;
import static android.telephony.CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY;
import static android.telephony.ims.stub.ImsRegistrationImplBase.ImsRegistrationTech;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;

import java.util.Arrays;
import java.util.Set;

/**
 * Enables or Disables NR-SA mode temporarily under certain conditions where WFC is established or
 * IMS is registered over WiFi in order to improve the delay or voice mute issue when the handover
 * from ePDG to NR is not supported in UE or network.
 */
public class ImsNrSaModeHandler extends Handler{

    public static final String TAG = "ImsNrSaModeHandler";
    public static final String MMTEL_FEATURE_TAG =
            "+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.mmtel\"";

    private static final int MSG_PRECISE_CALL_STATE_CHANGED = 101;
    private static final int MSG_REQUEST_IS_VONR_ENABLED = 102;
    private static final int MSG_RESULT_IS_VONR_ENABLED = 103;

    private final @NonNull ImsPhone mPhone;
    private @Nullable CarrierConfigManager mCarrierConfigManager;

    private @NrSaDisablePolicy int mNrSaDisablePolicy;
    private boolean mIsNrSaDisabledForWfc;
    private boolean mIsVowifiRegistered;
    private boolean mIsInImsCall;
    private boolean mIsNrSaSupported;

    private final CarrierConfigManager.CarrierConfigChangeListener mCarrierConfigChangeListener =
            (slotIndex, subId, carrierId, specificCarrierId) -> setNrSaDisablePolicy(subId);

    public ImsNrSaModeHandler(@NonNull ImsPhone phone, Looper looper) {
        super(looper);
        mPhone = phone;
        mCarrierConfigManager = (CarrierConfigManager) mPhone.getContext()
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);

        registerForCarrierConfigChanges();
    }

    /**
     * Performs any cleanup required before the ImsNrSaModeHandler is destroyed.
     */
    public void tearDown() {
        unregisterForCarrierConfigChanges();
        unregisterForPreciseCallStateChanges();

        if (isNrSaDisabledForWfc()) {
            setNrSaMode(true);
        }
    }

    /**
     * Based on changed VoWiFi reg state and call state, handles NR SA mode if needed.
     * It is including handover case.
     *
     * @param imsRadioTech The current registered RAT.
     */
    public void onImsRegistered(
            @ImsRegistrationTech int imsRadioTech, @NonNull Set<String> featureTags) {
        if (mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_NONE) {
            return;
        }

        Log.d(TAG, "onImsRegistered: ImsRegistrationTech = " + imsRadioTech);

        boolean isVowifiRegChanged = false;

        if (isVowifiRegistered() && imsRadioTech != REGISTRATION_TECH_IWLAN) {
            setVowifiRegStatus(false);
            isVowifiRegChanged = true;
        } else if (!isVowifiRegistered() && imsRadioTech == REGISTRATION_TECH_IWLAN
                && featureTags.contains(MMTEL_FEATURE_TAG)) {
            setVowifiRegStatus(true);
            isVowifiRegChanged = true;
        }

        if (isVowifiRegChanged) {
            if (mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_VOWIFI_REGISTERED) {
                setNrSaMode(!isVowifiRegistered());
            } else if ((mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_WFC_ESTABLISHED
                    || mNrSaDisablePolicy
                    == NR_SA_DISABLE_POLICY_WFC_ESTABLISHED_WHEN_VONR_DISABLED)
                    && isImsCallOngoing()) {
                if (mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_WFC_ESTABLISHED_WHEN_VONR_DISABLED) {
                    requestIsVonrEnabled(!isVowifiRegistered());
                    return;
                }

                setNrSaMode(!isVowifiRegistered());
            }
        }
    }

    /**
     * Based on changed VoWiFi reg state and call state, handles NR SA mode if needed.
     *
     * @param imsRadioTech The current un-registered RAT.
     */
    public void onImsUnregistered(
            @ImsRegistrationTech int imsRadioTech) {
        if (mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_NONE
                || imsRadioTech != REGISTRATION_TECH_IWLAN || !isVowifiRegistered()) {
            return;
        }

        Log.d(TAG, "onImsUnregistered : ImsRegistrationTech = " + imsRadioTech);

        setVowifiRegStatus(false);

        if (mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_VOWIFI_REGISTERED) {
            setNrSaMode(true);
        } else if ((mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_WFC_ESTABLISHED
                || mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_WFC_ESTABLISHED_WHEN_VONR_DISABLED)
                && isImsCallOngoing()) {
            if (mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_WFC_ESTABLISHED_WHEN_VONR_DISABLED) {
                requestIsVonrEnabled(true);
                return;
            }

            setNrSaMode(true);
        }
    }

    /**
     * Based on changed precise call state and VoWiFi reg state, handles NR SA mode if needed.
     */
    public void onPreciseCallStateChanged() {
        Log.d(TAG, "onPreciseCallStateChanged :  foreground state = "
                + mPhone.getForegroundCall().getState() + ", background state = "
                + mPhone.getBackgroundCall().getState());

        boolean isImsCallStatusChanged = false;

        if (isImsCallJustEstablished()) {
            setImsCallStatus(true);
            isImsCallStatusChanged = true;
        } else if (isImsCallJustTerminated()) {
            setImsCallStatus(false);
            isImsCallStatusChanged = true;
        }

        if (isVowifiRegistered() && isImsCallStatusChanged) {
            if (mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_WFC_ESTABLISHED_WHEN_VONR_DISABLED) {
                requestIsVonrEnabled(!isImsCallOngoing());
                return;
            }

            setNrSaMode(!isImsCallOngoing());
        }
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;

        switch (msg.what) {
            case MSG_PRECISE_CALL_STATE_CHANGED :
                onPreciseCallStateChanged();
                break;
            case MSG_REQUEST_IS_VONR_ENABLED :
                Log.d(TAG, "request isVoNrEnabled");
                mPhone.getDefaultPhone().mCi.isVoNrEnabled(
                        obtainMessage(MSG_RESULT_IS_VONR_ENABLED, msg.obj), null);
                break;
            case MSG_RESULT_IS_VONR_ENABLED :
                ar = (AsyncResult) msg.obj;

                if (ar.result != null) {
                    boolean vonrEnabled = ((Boolean) ar.result).booleanValue();

                    Log.d(TAG, "result isVoNrEnabled = " + vonrEnabled);
                    if (!vonrEnabled) {
                        setNrSaMode(((Boolean) ar.userObj).booleanValue());
                    }
                }

                ar = null;
                break;
            default :
                break;
        }
    }

    /**
     * Registers for precise call state changes.
     */
    private void registerForPreciseCallStateChanges() {
        mPhone.registerForPreciseCallStateChanged(this, MSG_PRECISE_CALL_STATE_CHANGED, null);
    }

    /**
     * Unregisters for precise call state changes.
     */
    private void unregisterForPreciseCallStateChanges() {
        mPhone.unregisterForPreciseCallStateChanged(this);
    }

    /**
     * Registers for carrier config changes.
     */
    private void registerForCarrierConfigChanges() {
        if (mCarrierConfigManager != null) {
            mCarrierConfigManager.registerCarrierConfigChangeListener(
                    this::post, mCarrierConfigChangeListener);
        }
    }

    /**
     * Unregisters for carrier config changes.
     */
    private void unregisterForCarrierConfigChanges() {
        if (mCarrierConfigManager != null) {
            mCarrierConfigManager.unregisterCarrierConfigChangeListener(
                    mCarrierConfigChangeListener);
        }
    }

    private void setNrSaDisablePolicy(int subId) {
        if (mPhone.getSubId() == subId && mCarrierConfigManager != null) {
            PersistableBundle bundle = mCarrierConfigManager.getConfigForSubId(mPhone.getSubId(),
                    KEY_NR_SA_DISABLE_POLICY_INT, KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY);
            mNrSaDisablePolicy = bundle.getInt(KEY_NR_SA_DISABLE_POLICY_INT);
            mIsNrSaSupported = Arrays.stream(
                    bundle.getIntArray(KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY)).anyMatch(
                        value -> value == CARRIER_NR_AVAILABILITY_SA);

            Log.d(TAG, "setNrSaDisablePolicy : NrSaDisablePolicy = "
                    + mNrSaDisablePolicy + ", IsNrSaSupported = "  + mIsNrSaSupported);

            if (mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_WFC_ESTABLISHED_WHEN_VONR_DISABLED
                    || mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_WFC_ESTABLISHED) {
                registerForPreciseCallStateChanges();
            } else {
                unregisterForPreciseCallStateChanges();
            }
        }
    }

    private void requestIsVonrEnabled(boolean onOrOff) {
        Message msg = obtainMessage(MSG_REQUEST_IS_VONR_ENABLED, onOrOff);
        msg.sendToTarget();
    }

    private void setNrSaMode(boolean onOrOff) {
        if (mIsNrSaSupported) {
            mPhone.getDefaultPhone().mCi.setN1ModeEnabled(onOrOff, null);
            Log.i(TAG, "setNrSaMode : " + onOrOff);

            setNrSaDisabledForWfc(!onOrOff);
        }
    }

    /**
     * Sets VoWiFi reg status.
     */
    @VisibleForTesting
    public void setVowifiRegStatus(boolean registered) {
        Log.d(TAG, "setVowifiRegStatus : " + registered);
        mIsVowifiRegistered = registered;
    }

    /**
     * Sets IMS call status
     */
    @VisibleForTesting
    public void setImsCallStatus(boolean inImsCall) {
        Log.d(TAG, "setImsCallStatus : " + inImsCall);
        mIsInImsCall = inImsCall;
    }

    @VisibleForTesting
    public boolean isVowifiRegistered() {
        return mIsVowifiRegistered;
    }

    @VisibleForTesting
    public boolean isImsCallOngoing() {
        return mIsInImsCall;
    }

    @VisibleForTesting
    public boolean isNrSaDisabledForWfc() {
        return mIsNrSaDisabledForWfc;
    }

    @VisibleForTesting
    public void setNrSaDisabledForWfc(boolean disabled) {
        mIsNrSaDisabledForWfc = disabled;
    }

    private boolean isImsCallJustEstablished() {
        if (!isImsCallOngoing()) {
            if ((mPhone.getForegroundCall().getState() == Call.State.ACTIVE)
                    || (mPhone.getBackgroundCall().getState() == Call.State.ACTIVE)) {
                Log.d(TAG, "isImsCallJustEstablished");
                return true;
            }
        }

        return false;
    }

    private boolean isImsCallJustTerminated() {
        if (isImsCallOngoing() && (!mPhone.getForegroundCall().getState().isAlive()
                && !mPhone.getBackgroundCall().getState().isAlive())) {
            Log.d(TAG, "isImsCallJustTerminated");
            return true;
        }

        return false;
    }
}
