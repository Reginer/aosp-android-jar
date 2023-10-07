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

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.satellite.ISatelliteStateCallback;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.IIntegerConsumer;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.satellite.SatelliteController;
import com.android.telephony.Rlog;

import java.util.Locale;

/**
 * Helper class that listens to a Phone's radio state and sends an onComplete callback when we
 * return true for isOkToCall.
 */
public class RadioOnStateListener {

    public interface Callback {
        /**
         * Receives the result of the RadioOnStateListener's attempt to turn on the radio
         * and turn off the satellite modem.
         */
        void onComplete(RadioOnStateListener listener, boolean isRadioReady);

        /**
         * Returns whether or not this phone is ok to call.
         * If it is, onComplete will be called shortly after.
         *
         * @param phone The Phone associated.
         * @param serviceState The service state of that phone.
         * @param imsVoiceCapable The IMS voice capability of that phone.
         * @return {@code true} if this phone is ok to call. Otherwise, {@code false}.
         */
        boolean isOkToCall(Phone phone, int serviceState, boolean imsVoiceCapable);

        /**
         * Returns whether or not this phone is ok to call.
         * This callback will be called when timeout happens.
         * If this returns {@code true}, onComplete will be called shortly after.
         * Otherwise, a new timer will be started again to keep waiting for next timeout.
         * The timeout interval will be passed to {@link #waitForRadioOn()} when registering
         * this callback.
         *
         * @param phone The Phone associated.
         * @param serviceState The service state of that phone.
         * @param imsVoiceCapable The IMS voice capability of that phone.
         * @return {@code true} if this phone is ok to call. Otherwise, {@code false}.
         */
        boolean onTimeout(Phone phone, int serviceState, boolean imsVoiceCapable);
    }

    private static final String TAG = "RadioOnStateListener";

    // Number of times to retry the call, and time between retry attempts.
    // not final for testing
    private static int MAX_NUM_RETRIES = 5;
    // not final for testing
    private static long TIME_BETWEEN_RETRIES_MILLIS = 5000; // msec

    // Handler message codes; see handleMessage()
    private static final int MSG_START_SEQUENCE = 1;
    @VisibleForTesting
    public static final int MSG_SERVICE_STATE_CHANGED = 2;
    private static final int MSG_RETRY_TIMEOUT = 3;
    @VisibleForTesting
    public static final int MSG_RADIO_ON = 4;
    public static final int MSG_RADIO_OFF_OR_NOT_AVAILABLE = 5;
    public static final int MSG_IMS_CAPABILITY_CHANGED = 6;
    public static final int MSG_TIMEOUT_ONTIMEOUT_CALLBACK = 7;
    @VisibleForTesting
    public static final int MSG_SATELLITE_ENABLED_CHANGED = 8;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_SEQUENCE:
                    SomeArgs args = (SomeArgs) msg.obj;
                    try {
                        Phone phone = (Phone) args.arg1;
                        RadioOnStateListener.Callback callback =
                                (RadioOnStateListener.Callback) args.arg2;
                        boolean forEmergencyCall = (boolean) args.arg3;
                        boolean isSelectedPhoneForEmergencyCall = (boolean) args.arg4;
                        int onTimeoutCallbackInterval = args.argi1;
                        startSequenceInternal(phone, callback, forEmergencyCall,
                                isSelectedPhoneForEmergencyCall, onTimeoutCallbackInterval);
                    } finally {
                        args.recycle();
                    }
                    break;
                case MSG_SERVICE_STATE_CHANGED:
                    onServiceStateChanged((ServiceState) ((AsyncResult) msg.obj).result);
                    break;
                case MSG_RADIO_ON:
                    onRadioOn();
                    break;
                case MSG_RADIO_OFF_OR_NOT_AVAILABLE:
                    registerForRadioOn();
                    break;
                case MSG_RETRY_TIMEOUT:
                    onRetryTimeout();
                    break;
                case MSG_IMS_CAPABILITY_CHANGED:
                    onImsCapabilityChanged();
                    break;
                case MSG_TIMEOUT_ONTIMEOUT_CALLBACK:
                    onTimeoutCallbackTimeout();
                    break;
                case MSG_SATELLITE_ENABLED_CHANGED:
                    onSatelliteEnabledChanged();
                    break;
                default:
                    Rlog.w(TAG, String.format(Locale.getDefault(),
                        "handleMessage: unexpected message: %d.", msg.what));
                    break;
            }
        }
    };

    private final ISatelliteStateCallback mSatelliteCallback = new ISatelliteStateCallback.Stub() {
        @Override
        public void onSatelliteModemStateChanged(int state) {
            mHandler.obtainMessage(MSG_SATELLITE_ENABLED_CHANGED).sendToTarget();
        }
    };

    private Callback mCallback; // The callback to notify upon completion.
    private Phone mPhone; // The phone that will attempt to place the call.
    // SatelliteController instance to check whether satellite has been disabled.
    private SatelliteController mSatelliteController;
    private boolean mForEmergencyCall; // Whether radio is being turned on for emergency call.
    // Whether this phone is selected to place emergency call. Can be true only if
    // mForEmergencyCall is true.
    private boolean mSelectedPhoneForEmergencyCall;
    private int mNumRetriesSoFar;
    private int mOnTimeoutCallbackInterval; // the interval between onTimeout callbacks

    /**
     * Starts the "wait for radio" sequence. This is the (single) external API of the
     * RadioOnStateListener class.
     *
     * This method kicks off the following sequence:
     * - Listen for the service state change event telling us the radio has come up.
     * - Listen for the satellite state changed event telling us the satellite service is disabled.
     * - Retry if we've gone {@link #TIME_BETWEEN_RETRIES_MILLIS} without any response from the
     *   radio.
     * - Finally, clean up any leftover state.
     *
     * This method is safe to call from any thread, since it simply posts a message to the
     * RadioOnStateListener's handler (thus ensuring that the rest of the sequence is entirely
     * serialized, and runs only on the handler thread.)
     */
    public void waitForRadioOn(Phone phone, Callback callback,
            boolean forEmergencyCall, boolean isSelectedPhoneForEmergencyCall,
            int onTimeoutCallbackInterval) {
        Rlog.d(TAG, "waitForRadioOn: Phone " + phone.getPhoneId());

        if (mPhone != null) {
            // If there already is an ongoing request, ignore the new one!
            return;
        }

        SomeArgs args = SomeArgs.obtain();
        args.arg1 = phone;
        args.arg2 = callback;
        args.arg3 = forEmergencyCall;
        args.arg4 = isSelectedPhoneForEmergencyCall;
        args.argi1 = onTimeoutCallbackInterval;
        mHandler.obtainMessage(MSG_START_SEQUENCE, args).sendToTarget();
    }

    /**
     * Actual implementation of waitForRadioOn(), guaranteed to run on the handler thread.
     *
     * @see #waitForRadioOn
     */
    private void startSequenceInternal(Phone phone, Callback callback,
            boolean forEmergencyCall, boolean isSelectedPhoneForEmergencyCall,
            int onTimeoutCallbackInterval) {
        Rlog.d(TAG, "startSequenceInternal: Phone " + phone.getPhoneId());
        mSatelliteController = SatelliteController.getInstance();

        // First of all, clean up any state left over from a prior RadioOn call sequence. This
        // ensures that we'll behave sanely if another startTurnOnRadioSequence() comes in while
        // we're already in the middle of the sequence.
        cleanup();

        mPhone = phone;
        mCallback = callback;
        mForEmergencyCall = forEmergencyCall;
        mSelectedPhoneForEmergencyCall = isSelectedPhoneForEmergencyCall;
        mOnTimeoutCallbackInterval = onTimeoutCallbackInterval;

        registerForServiceStateChanged();
        // Register for RADIO_OFF to handle cases where emergency call is dialed before
        // we receive UNSOL_RESPONSE_RADIO_STATE_CHANGED with RADIO_OFF.
        registerForRadioOff();
        if (mSatelliteController.isSatelliteEnabled()) {
            // Register for satellite modem state changed to notify when satellite is disabled.
            registerForSatelliteEnabledChanged();
        }
        // Next step: when the SERVICE_STATE_CHANGED or SATELLITE_ENABLED_CHANGED event comes in,
        // we'll retry the call; see onServiceStateChanged() and onSatelliteEnabledChanged().
        // But also, just in case, start a timer to make sure we'll retry the call even if the
        // SERVICE_STATE_CHANGED or SATELLITE_ENABLED_CHANGED events never come in for some reason.
        startRetryTimer();
        registerForImsCapabilityChanged();
        startOnTimeoutCallbackTimer();
    }

    private void onImsCapabilityChanged() {
        if (mPhone == null) {
            return;
        }

        boolean imsVoiceCapable = mPhone.isVoiceOverCellularImsEnabled();

        Rlog.d(TAG, String.format("onImsCapabilityChanged, capable = %s, Phone = %s",
                imsVoiceCapable, mPhone.getPhoneId()));

        if (isOkToCall(mPhone.getServiceState().getState(), imsVoiceCapable)) {
            Rlog.d(TAG, "onImsCapabilityChanged: ok to call!");

            onComplete(true);
            cleanup();
        } else {
            // The IMS capability changed, but we're still not ready to call yet.
            Rlog.d(TAG, "onImsCapabilityChanged: not ready to call yet, keep waiting.");
        }
    }

    private void onTimeoutCallbackTimeout() {
        if (mPhone == null) {
            return;
        }

        if (onTimeout(mPhone.getServiceState().getState(),
                  mPhone.isVoiceOverCellularImsEnabled())) {
            Rlog.d(TAG, "onTimeout: ok to call!");

            onComplete(true);
            cleanup();
        } else if (mNumRetriesSoFar > MAX_NUM_RETRIES) {
            Rlog.w(TAG, "onTimeout: Hit MAX_NUM_RETRIES; giving up.");
            cleanup();
        } else {
            Rlog.d(TAG, "onTimeout: not ready to call yet, keep waiting.");
            startOnTimeoutCallbackTimer();
        }
    }

    /**
     * Handles the SERVICE_STATE_CHANGED event. This event tells us that the radio state has changed
     * and is probably coming up. We can now check to see if the conditions are met to place the
     * call with {@link Callback#isOkToCall}
     */
    private void onServiceStateChanged(ServiceState state) {
        if (mPhone == null) {
            return;
        }
        Rlog.d(TAG, String.format("onServiceStateChanged(), new state = %s, Phone = %s", state,
                mPhone.getPhoneId()));

        // Possible service states:
        // - STATE_IN_SERVICE        // Normal operation
        // - STATE_OUT_OF_SERVICE    // Still searching for an operator to register to,
        //                           // or no radio signal
        // - STATE_EMERGENCY_ONLY    // Only emergency numbers are allowed; currently not used
        // - STATE_POWER_OFF         // Radio is explicitly powered off (airplane mode)

        if (isOkToCall(state.getState(), mPhone.isVoiceOverCellularImsEnabled())) {
            // Woo hoo! It's OK to actually place the call.
            Rlog.d(TAG, "onServiceStateChanged: ok to call!");

            onComplete(true);
            cleanup();
        } else {
            // The service state changed, but we're still not ready to call yet.
            Rlog.d(TAG, "onServiceStateChanged: not ready to call yet, keep waiting.");
        }
    }

    private void onRadioOn() {
        if (mPhone == null) {
            return;
        }
        ServiceState state = mPhone.getServiceState();
        Rlog.d(TAG, String.format("onRadioOn, state = %s, Phone = %s", state, mPhone.getPhoneId()));
        if (isOkToCall(state.getState(), mPhone.isVoiceOverCellularImsEnabled())) {
            onComplete(true);
            cleanup();
        } else {
            Rlog.d(TAG, "onRadioOn: not ready to call yet, keep waiting.");
        }
    }

    private void onSatelliteEnabledChanged() {
        if (mPhone == null) {
            return;
        }
        if (isOkToCall(mPhone.getServiceState().getState(),
                mPhone.isVoiceOverCellularImsEnabled())) {
            onComplete(true);
            cleanup();
        } else {
            Rlog.d(TAG, "onSatelliteEnabledChanged: not ready to call yet, keep waiting.");
        }
    }

    /**
     * Callback to see if it is okay to call yet, given the current conditions.
     */
    private boolean isOkToCall(int serviceState, boolean imsVoiceCapable) {
        return (mCallback == null)
                ? false : mCallback.isOkToCall(mPhone, serviceState, imsVoiceCapable);
    }

    /**
     * Callback to see if it is okay to call yet, given the current conditions.
     */
    private boolean onTimeout(int serviceState, boolean imsVoiceCapable) {
        return (mCallback == null)
                ? false : mCallback.onTimeout(mPhone, serviceState, imsVoiceCapable);
    }

    /**
     * Handles the retry timer expiring.
     */
    private void onRetryTimeout() {
        if (mPhone == null) {
            return;
        }
        int serviceState = mPhone.getServiceState().getState();
        Rlog.d(TAG,
                String.format(Locale.getDefault(),
                        "onRetryTimeout():  phone state = %s, service state = %d, retries = %d.",
                        mPhone.getState(), serviceState, mNumRetriesSoFar));

        // - If we're actually in a call, we've succeeded.
        // - Otherwise, if the radio is now on, that means we successfully got out of airplane mode
        //   but somehow didn't get the service state change event. In that case, try to place the
        //   call.
        // - If the radio is still powered off, try powering it on again.

        if (isOkToCall(serviceState, mPhone.isVoiceOverCellularImsEnabled())) {
            Rlog.d(TAG, "onRetryTimeout: Radio is on. Cleaning up.");

            // Woo hoo -- we successfully got out of airplane mode.
            onComplete(true);
            cleanup();
        } else {
            // Uh oh; we've waited the full TIME_BETWEEN_RETRIES_MILLIS and the radio is still not
            // powered-on. Try again.

            mNumRetriesSoFar++;
            Rlog.d(TAG, "mNumRetriesSoFar is now " + mNumRetriesSoFar);

            if (mNumRetriesSoFar > MAX_NUM_RETRIES) {
                if (mHandler.hasMessages(MSG_TIMEOUT_ONTIMEOUT_CALLBACK)) {
                    Rlog.w(TAG, "Hit MAX_NUM_RETRIES; waiting onTimeout callback");
                    return;
                }
                Rlog.w(TAG, "Hit MAX_NUM_RETRIES; giving up.");
                cleanup();
            } else {
                Rlog.d(TAG, "Trying (again) to turn the radio on and satellite modem off.");
                mPhone.setRadioPower(true, mForEmergencyCall, mSelectedPhoneForEmergencyCall,
                        false);
                if (mSatelliteController.isSatelliteEnabled()) {
                    mSatelliteController.requestSatelliteEnabled(mPhone.getSubId(),
                            false /* enableSatellite */, false /* enableDemoMode */,
                            new IIntegerConsumer.Stub() {
                                @Override
                                public void accept(int result) {
                                    mHandler.obtainMessage(MSG_SATELLITE_ENABLED_CHANGED)
                                            .sendToTarget();
                                }
                            });
                }
                startRetryTimer();
            }
        }
    }

    /**
     * Clean up when done with the whole sequence: either after successfully turning on the radio,
     * or after bailing out because of too many failures.
     *
     * The exact cleanup steps are:
     * - Notify callback if we still hadn't sent it a response.
     * - Double-check that we're not still registered for any telephony events
     * - Clean up any extraneous handler messages (like retry timeouts) still in the queue
     *
     * Basically this method guarantees that there will be no more activity from the
     * RadioOnStateListener until someone kicks off the whole sequence again with another call to
     * {@link #waitForRadioOn}
     *
     * TODO: Do the work for the comment below: Note we don't call this method simply after a
     * successful call to placeCall(), since it's still possible the call will disconnect very
     * quickly with an OUT_OF_SERVICE error.
     */
    public void cleanup() {
        Rlog.d(TAG, "cleanup()");

        // This will send a failure call back if callback has yet to be invoked. If the callback was
        // already invoked, it's a no-op.
        onComplete(false);

        unregisterForServiceStateChanged();
        unregisterForRadioOff();
        unregisterForRadioOn();
        unregisterForSatelliteEnabledChanged();
        cancelRetryTimer();
        unregisterForImsCapabilityChanged();

        // Used for unregisterForServiceStateChanged() so we null it out here instead.
        mPhone = null;
        mNumRetriesSoFar = 0;
        mOnTimeoutCallbackInterval = 0;
    }

    private void startRetryTimer() {
        cancelRetryTimer();
        mHandler.sendEmptyMessageDelayed(MSG_RETRY_TIMEOUT, TIME_BETWEEN_RETRIES_MILLIS);
    }

    private void cancelRetryTimer() {
        mHandler.removeMessages(MSG_RETRY_TIMEOUT);
    }

    private void registerForServiceStateChanged() {
        // Unregister first, just to make sure we never register ourselves twice. (We need this
        // because Phone.registerForServiceStateChanged() does not prevent multiple registration of
        // the same handler.)
        unregisterForServiceStateChanged();
        mPhone.registerForServiceStateChanged(mHandler, MSG_SERVICE_STATE_CHANGED, null);
    }

    private void unregisterForServiceStateChanged() {
        // This method is safe to call even if we haven't set mPhone yet.
        if (mPhone != null) {
            mPhone.unregisterForServiceStateChanged(mHandler); // Safe even if unnecessary
        }
        mHandler.removeMessages(MSG_SERVICE_STATE_CHANGED); // Clean up any pending messages too
    }

    private void registerForRadioOff() {
        mPhone.mCi.registerForOffOrNotAvailable(mHandler, MSG_RADIO_OFF_OR_NOT_AVAILABLE, null);
    }

    private void unregisterForRadioOff() {
        // This method is safe to call even if we haven't set mPhone yet.
        if (mPhone != null) {
            mPhone.mCi.unregisterForOffOrNotAvailable(mHandler); // Safe even if unnecessary
        }
        mHandler.removeMessages(MSG_RADIO_OFF_OR_NOT_AVAILABLE); // Clean up any pending messages
    }

    private void registerForRadioOn() {
        unregisterForRadioOff();
        mPhone.mCi.registerForOn(mHandler, MSG_RADIO_ON, null);
    }

    private void unregisterForRadioOn() {
        // This method is safe to call even if we haven't set mPhone yet.
        if (mPhone != null) {
            mPhone.mCi.unregisterForOn(mHandler); // Safe even if unnecessary
        }
        mHandler.removeMessages(MSG_RADIO_ON); // Clean up any pending messages too
    }

    private void registerForSatelliteEnabledChanged() {
        mSatelliteController.registerForSatelliteModemStateChanged(
                mPhone.getSubId(), mSatelliteCallback);
    }

    private void unregisterForSatelliteEnabledChanged() {
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (mPhone != null) {
            subId = mPhone.getSubId();
        }
        mSatelliteController.unregisterForSatelliteModemStateChanged(subId, mSatelliteCallback);
        mHandler.removeMessages(MSG_SATELLITE_ENABLED_CHANGED);
    }

    private void registerForImsCapabilityChanged() {
        unregisterForImsCapabilityChanged();
        mPhone.getServiceStateTracker()
                .registerForImsCapabilityChanged(mHandler, MSG_IMS_CAPABILITY_CHANGED, null);
    }

    private void unregisterForImsCapabilityChanged() {
        if (mPhone != null) {
            mPhone.getServiceStateTracker()
                    .unregisterForImsCapabilityChanged(mHandler);
        }
        mHandler.removeMessages(MSG_IMS_CAPABILITY_CHANGED);
    }

    private void startOnTimeoutCallbackTimer() {
        Rlog.d(TAG, "startOnTimeoutCallbackTimer: mOnTimeoutCallbackInterval="
                + mOnTimeoutCallbackInterval);
        mHandler.removeMessages(MSG_TIMEOUT_ONTIMEOUT_CALLBACK);
        if (mOnTimeoutCallbackInterval > 0) {
            mHandler.sendEmptyMessageDelayed(MSG_TIMEOUT_ONTIMEOUT_CALLBACK,
                    mOnTimeoutCallbackInterval);
        }
    }

    private void onComplete(boolean isRadioReady) {
        if (mCallback != null) {
            Callback tempCallback = mCallback;
            mCallback = null;
            tempCallback.onComplete(this, isRadioReady);
        }
    }

    @VisibleForTesting
    public Handler getHandler() {
        return mHandler;
    }

    @VisibleForTesting
    public void setMaxNumRetries(int retries) {
        MAX_NUM_RETRIES = retries;
    }

    @VisibleForTesting
    public void setTimeBetweenRetriesMillis(long timeMs) {
        TIME_BETWEEN_RETRIES_MILLIS = timeMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !getClass().equals(o.getClass()))
            return false;

        RadioOnStateListener that = (RadioOnStateListener) o;

        if (mNumRetriesSoFar != that.mNumRetriesSoFar) {
            return false;
        }
        if (mCallback != null ? !mCallback.equals(that.mCallback) : that.mCallback != null) {
            return false;
        }
        return mPhone != null ? mPhone.equals(that.mPhone) : that.mPhone == null;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + mNumRetriesSoFar;
        hash = 31 * hash + (mCallback == null ? 0 : mCallback.hashCode());
        hash = 31 * hash + (mPhone == null ? 0 : mPhone.hashCode());
        return hash;
    }
}
