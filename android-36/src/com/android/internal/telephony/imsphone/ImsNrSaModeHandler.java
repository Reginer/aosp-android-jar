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

import static com.android.internal.telephony.CommandsInterface.IMS_MMTEL_CAPABILITY_VOICE;

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
public class ImsNrSaModeHandler extends Handler {

    public static final String TAG = "ImsNrSaModeHandler";

    private static final int MSG_PRECISE_CALL_STATE_CHANGED = 101;
    private static final int MSG_RESULT_IS_VONR_ENABLED = 102;

    private final @NonNull ImsPhone mPhone;
    private @Nullable CarrierConfigManager mCarrierConfigManager;

    @FunctionalInterface
    public interface N1ModeSetter {
        /** Override-able for testing */
        void setN1ModeEnabled(boolean enabled, @Nullable Message message);
    }

    private N1ModeSetter mN1ModeSetter;

    private @NrSaDisablePolicy int mNrSaDisablePolicy;
    private boolean mIsNrSaDisabledForWfc;
    private boolean mIsWifiRegistered;
    private boolean mIsInImsCall;
    private boolean mIsNrSaSupported;
    private boolean mIsVoiceCapable;

    private final CarrierConfigManager.CarrierConfigChangeListener mCarrierConfigChangeListener =
            (slotIndex, subId, carrierId, specificCarrierId) -> setNrSaDisablePolicy(subId);

    public ImsNrSaModeHandler(@NonNull ImsPhone phone, Looper looper) {
        super(looper);

        mPhone = phone;
        mN1ModeSetter = mPhone.getDefaultPhone()::setN1ModeEnabled;

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
        if (!mIsNrSaSupported || mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_NONE) {
            return;
        }

        Log.d(TAG, "onImsRegistered: ImsRegistrationTech = " + imsRadioTech);

        final boolean isNewWifiRegistered = imsRadioTech == REGISTRATION_TECH_IWLAN;
        if (isWifiRegistered() != isNewWifiRegistered) {
            setWifiRegStatus(isNewWifiRegistered);
            calculateAndControlNrSaIfNeeded();
        }
    }

    /**
     * Based on changed VoWiFi reg state and call state, handles NR SA mode if needed.
     *
     * @param imsRadioTech The current un-registered RAT.
     */
    public void onImsUnregistered(
            @ImsRegistrationTech int imsRadioTech) {
        if (!mIsNrSaSupported || mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_NONE
                || imsRadioTech != REGISTRATION_TECH_IWLAN || !isWifiRegistered()) {
            return;
        }

        Log.d(TAG, "onImsUnregistered : ImsRegistrationTech = " + imsRadioTech);

        setWifiRegStatus(false);
        calculateAndControlNrSaIfNeeded();
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

        if (isWifiRegistered() && isImsCallStatusChanged) {
            calculateAndControlNrSaIfNeeded();
        }
    }

    /**
     * Updates Capability.
     */
    public void updateImsCapability(int capabilities) {
        if (!mIsNrSaSupported || mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_NONE) {
            return;
        }

        boolean isVoiceCapable = (IMS_MMTEL_CAPABILITY_VOICE & capabilities) != 0;
        if (mIsVoiceCapable != isVoiceCapable) {
            mIsVoiceCapable = isVoiceCapable;
            calculateAndControlNrSaIfNeeded();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;

        switch (msg.what) {
            case MSG_PRECISE_CALL_STATE_CHANGED :
                onPreciseCallStateChanged();
                break;
            case MSG_RESULT_IS_VONR_ENABLED :
                ar = (AsyncResult) msg.obj;

                if (ar.result != null) {
                    boolean vonrEnabled = ((Boolean) ar.result).booleanValue();

                    Log.d(TAG, "result isVoNrEnabled = " + vonrEnabled);
                    if (isWifiCallingOngoing() && !vonrEnabled) {
                        // If still WiFi calling is ongoing and VoNR is disabled, disable NR SA.
                        setNrSaMode(false);
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
            int[] nrAvailabilities = bundle.getIntArray(KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY);
            mIsNrSaSupported = nrAvailabilities != null
                    && Arrays.stream(nrAvailabilities).anyMatch(
                            value -> value == CARRIER_NR_AVAILABILITY_SA);

            if (!mIsNrSaSupported) {
                return;
            }

            mNrSaDisablePolicy = bundle.getInt(KEY_NR_SA_DISABLE_POLICY_INT);

            Log.d(TAG, "setNrSaDisablePolicy : NrSaDisablePolicy = " + mNrSaDisablePolicy);

            if (mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_WFC_ESTABLISHED_WHEN_VONR_DISABLED
                    || mNrSaDisablePolicy == NR_SA_DISABLE_POLICY_WFC_ESTABLISHED) {
                registerForPreciseCallStateChanges();
            } else {
                unregisterForPreciseCallStateChanges();
            }
        }
    }

    @VisibleForTesting
    public void setN1ModeSetter(N1ModeSetter setter) {
        mN1ModeSetter = setter;
    }

    private void setNrSaMode(boolean onOrOff) {
        mN1ModeSetter.setN1ModeEnabled(onOrOff, null);
        Log.i(TAG, "setNrSaMode : " + onOrOff);

        setNrSaDisabledForWfc(!onOrOff);
    }

    /**
     * Sets WiFi reg status.
     */
    @VisibleForTesting
    public void setWifiRegStatus(boolean registered) {
        Log.d(TAG, "setWifiRegStatus : " + registered);
        mIsWifiRegistered = registered;
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
    public boolean isWifiRegistered() {
        return mIsWifiRegistered;
    }

    @VisibleForTesting
    public boolean isImsCallOngoing() {
        return mIsInImsCall;
    }

    private boolean isNrSaDisabledForWfc() {
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

    private void calculateAndControlNrSaIfNeeded() {
        switch (mNrSaDisablePolicy) {
            case NR_SA_DISABLE_POLICY_VOWIFI_REGISTERED:
                if (isNrSaDisabledForWfc() == isWifiRegisteredForVoice()) {
                    // NR SA is already disabled or condition is not met for disabling NR SA.
                    // So, no need for further action
                    return;
                }

                // Disable NR SA if VoWiFi registered otherwise enable
                setNrSaMode(!isWifiRegisteredForVoice());
                return;
            case NR_SA_DISABLE_POLICY_WFC_ESTABLISHED:
                if (isNrSaDisabledForWfc() == isWifiCallingOngoing()) {
                    // NR SA is already disabled or condition is not met for disabling NR SA.
                    // So, no need for further action
                    return;
                }

                // Disable NR SA if VoWiFi call established otherwise enable
                setNrSaMode(!isWifiCallingOngoing());
                return;
            case NR_SA_DISABLE_POLICY_WFC_ESTABLISHED_WHEN_VONR_DISABLED:
                if (isNrSaDisabledForWfc() == isWifiCallingOngoing()) {
                    // NR SA is already disabled or condition is not met for disabling NR SA.
                    // So, no need for further action
                    return;
                }

                if (isWifiCallingOngoing()) {
                    // Query whether VoNR is enabled or not.
                    mPhone.getDefaultPhone().mCi.isVoNrEnabled(
                            obtainMessage(MSG_RESULT_IS_VONR_ENABLED), null);
                    return;
                }

                // Enable NR SA if there are no VoWiFi calls.
                setNrSaMode(true);
                return;
            default:
                break;
        }
    }

    private boolean isWifiRegisteredForVoice() {
        return isWifiRegistered() && mIsVoiceCapable;
    }

    private boolean isWifiCallingOngoing() {
        return isWifiRegistered() && mIsVoiceCapable && isImsCallOngoing();
    }
}
