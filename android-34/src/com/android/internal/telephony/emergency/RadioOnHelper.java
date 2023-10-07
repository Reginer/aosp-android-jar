/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.internal.telephony.emergency;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.IIntegerConsumer;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.satellite.SatelliteController;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class that implements special behavior related to emergency calls or making phone calls
 * when the radio is in the POWER_OFF STATE. Specifically, this class handles the case of the user
 * trying to dial an emergency number while the radio is off (i.e. the device is in airplane mode)
 * or a normal number while the radio is off (because of the device is on Bluetooth), by turning the
 * radio back on, waiting for it to come up, and then retrying the call.
 */
public class RadioOnHelper implements RadioOnStateListener.Callback {

    private static final String TAG = "RadioOnStateListener";

    private final Context mContext;
    private RadioOnStateListener.Callback mCallback;
    private List<RadioOnStateListener> mListeners;
    private List<RadioOnStateListener> mInProgressListeners;
    private boolean mIsRadioReady;

    public RadioOnHelper(Context context) {
        mContext = context;
        mInProgressListeners = new ArrayList<>(2);
    }

    private void setupListeners() {
        if (mListeners == null) {
            mListeners = new ArrayList<>(2);
        }
        int activeModems = TelephonyManager.from(mContext).getActiveModemCount();
        // Add new listeners if active modem count increased.
        while (mListeners.size() < activeModems) {
            mListeners.add(new RadioOnStateListener());
        }
        // Clean up listeners if active modem count decreased.
        while (mListeners.size() > activeModems) {
            mListeners.get(mListeners.size() - 1).cleanup();
            mListeners.remove(mListeners.size() - 1);
        }
    }

    /**
     * Starts the "turn on radio" sequence. This is the (single) external API of the RadioOnHelper
     * class.
     *
     * This method kicks off the following sequence:
     * - Power on the radio for each Phone and disable the satellite modem
     * - Listen for events telling us the radio has come up or the satellite modem is disabled.
     * - Retry if we've gone a significant amount of time without any response.
     * - Finally, clean up any leftover state.
     *
     * This method is safe to call from any thread, since it simply posts a message to the
     * RadioOnHelper's handler (thus ensuring that the rest of the sequence is entirely serialized,
     * and runs on the main looper.)
     */
    public void triggerRadioOnAndListen(RadioOnStateListener.Callback callback,
            boolean forEmergencyCall, Phone phoneForEmergencyCall, boolean isTestEmergencyNumber,
            int emergencyTimeoutIntervalMillis) {
        setupListeners();
        mCallback = callback;
        mInProgressListeners.clear();
        mIsRadioReady = false;
        for (int i = 0; i < TelephonyManager.from(mContext).getActiveModemCount(); i++) {
            Phone phone = PhoneFactory.getPhone(i);
            if (phone == null) {
                continue;
            }

            int timeoutCallbackInterval = (forEmergencyCall && phone == phoneForEmergencyCall)
                    ? emergencyTimeoutIntervalMillis : 0;
            mInProgressListeners.add(mListeners.get(i));
            mListeners.get(i).waitForRadioOn(phone, this, forEmergencyCall, forEmergencyCall
                    && phone == phoneForEmergencyCall, timeoutCallbackInterval);
        }
        powerOnRadio(forEmergencyCall, phoneForEmergencyCall, isTestEmergencyNumber);
        if (SatelliteController.getInstance().isSatelliteEnabled()) {
            powerOffSatellite(phoneForEmergencyCall);
        }
    }

    /**
     * Attempt to power on the radio (i.e. take the device out of airplane mode). We'll eventually
     * get an onServiceStateChanged() callback when the radio successfully comes up.
     */
    private void powerOnRadio(boolean forEmergencyCall, Phone phoneForEmergencyCall,
            boolean isTestEmergencyNumber) {

        // Always try to turn on the radio here independent of APM setting - if we got here in the
        // first place, the radio is off independent of APM setting.
        for (Phone phone : PhoneFactory.getPhones()) {
            Rlog.d(TAG, "powerOnRadio, enabling Radio");
            if (isTestEmergencyNumber) {
                phone.setRadioPowerOnForTestEmergencyCall(phone == phoneForEmergencyCall);
            } else {
                phone.setRadioPower(true, forEmergencyCall, phone == phoneForEmergencyCall,
                        false);
            }
        }

        // If airplane mode is on, we turn it off the same way that the Settings activity turns it
        // off to keep the setting in sync.
        if (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) > 0) {
            Rlog.d(TAG, "==> Turning off airplane mode for emergency call.");

            // Change the system setting
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0);

            // Post the broadcast intend for change in airplane mode TODO: We really should not be
            // in charge of sending this broadcast. If changing the setting is sufficient to trigger
            // all of the rest of the logic, then that should also trigger the broadcast intent.
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", false);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    /**
     * Attempt to power off the satellite modem. We'll eventually get an
     * onSatelliteModemStateChanged() callback when the satellite modem is successfully disabled.
     */
    private void powerOffSatellite(Phone phoneForEmergencyCall) {
        SatelliteController satelliteController = SatelliteController.getInstance();
        satelliteController.requestSatelliteEnabled(phoneForEmergencyCall.getSubId(),
                false /* enableSatellite */, false /* enableDemoMode */,
                new IIntegerConsumer.Stub() {
                    @Override
                    public void accept(int result) {

                    }
                });
    }

    /**
     * This method is called from multiple Listeners on the Main Looper. Synchronization is not
     * necessary.
     */
    @Override
    public void onComplete(RadioOnStateListener listener, boolean isRadioReady) {
        mIsRadioReady |= isRadioReady;
        mInProgressListeners.remove(listener);
        if (mCallback != null && mInProgressListeners.isEmpty()) {
            mCallback.onComplete(null, mIsRadioReady);
        }
    }

    @Override
    public boolean isOkToCall(Phone phone, int serviceState, boolean imsVoiceCapable) {
        return (mCallback == null)
                ? false : mCallback.isOkToCall(phone, serviceState, imsVoiceCapable);
    }

    @Override
    public boolean onTimeout(Phone phone, int serviceState, boolean imsVoiceCapable) {
        return (mCallback == null)
                ? false : mCallback.onTimeout(phone, serviceState, imsVoiceCapable);
    }
}
