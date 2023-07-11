/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.annotation.AnyThread;
import android.annotation.NonNull;
import android.net.Uri;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.RegistrationManager;
import android.telephony.ims.aidl.IImsRegistrationCallback;
import android.util.Log;

import java.util.concurrent.Executor;

/**
 * A helper class to manager the ImsRegistrationCallback can notify the state changed to listener.
 */
@AnyThread
public class ImsRegistrationCallbackHelper {
    private static final String TAG = "ImsRegCallbackHelper";

    /**
     * The interface to receive IMS registration updated.
     */
    public interface ImsRegistrationUpdate {
        /**
         * Handle the callback when IMS is registered.
         */
        void handleImsRegistered(int imsRadioTech);

        /**
         * Handle the callback when IMS is registering.
         */
        void handleImsRegistering(int imsRadioTech);

        /**
         * Handle the callback when IMS is unregistered.
         */
        void handleImsUnregistered(ImsReasonInfo imsReasonInfo);

        /**
         * Handle the callback when the list of subscriber {@link Uri}s associated with this IMS
         * subscription changed.
         */
        void handleImsSubscriberAssociatedUriChanged(Uri[] uris);
    }

    private ImsRegistrationUpdate mImsRegistrationUpdate;
    private int mRegistrationState = RegistrationManager.REGISTRATION_STATE_NOT_REGISTERED;
    private final Object mLock = new Object();

    private final RegistrationManager.RegistrationCallback mImsRegistrationCallback =
            new RegistrationManager.RegistrationCallback() {
                @Override
                public void onRegistered(int imsRadioTech) {
                    updateRegistrationState(RegistrationManager.REGISTRATION_STATE_REGISTERED);
                    mImsRegistrationUpdate.handleImsRegistered(imsRadioTech);
                }

                @Override
                public void onRegistering(int imsRadioTech) {
                    updateRegistrationState(RegistrationManager.REGISTRATION_STATE_REGISTERING);
                    mImsRegistrationUpdate.handleImsRegistering(imsRadioTech);
                }

                @Override
                public void onUnregistered(ImsReasonInfo imsReasonInfo) {
                    updateRegistrationState(RegistrationManager.REGISTRATION_STATE_NOT_REGISTERED);
                    mImsRegistrationUpdate.handleImsUnregistered(imsReasonInfo);
                }

                @Override
                public void onSubscriberAssociatedUriChanged(Uri[] uris) {
                    mImsRegistrationUpdate.handleImsSubscriberAssociatedUriChanged(uris);
                }
            };

    public ImsRegistrationCallbackHelper(@NonNull ImsRegistrationUpdate registrationUpdate,
            Executor executor) {
        mImsRegistrationCallback.setExecutor(executor);
        mImsRegistrationUpdate = registrationUpdate;
    }

    /**
     * Reset the IMS registration state.
     */
    public void reset() {
        Log.d(TAG, "reset");
        updateRegistrationState(RegistrationManager.REGISTRATION_STATE_NOT_REGISTERED);
    }

    /**
     * Update the latest IMS registration state.
     */
    public synchronized void updateRegistrationState(
            @RegistrationManager.ImsRegistrationState int newState) {
        synchronized (mLock) {
            Log.d(TAG, "updateRegistrationState: registration state from "
                    + RegistrationManager.registrationStateToString(mRegistrationState)
                    + " to " + RegistrationManager.registrationStateToString(newState));
            mRegistrationState = newState;
        }
    }

    public int getImsRegistrationState() {
        synchronized (mLock) {
            return mRegistrationState;
        }
    }

    public boolean isImsRegistered() {
        return getImsRegistrationState() == RegistrationManager.REGISTRATION_STATE_REGISTERED;
    }

    public RegistrationManager.RegistrationCallback getCallback() {
        return mImsRegistrationCallback;
    }

    public IImsRegistrationCallback getCallbackBinder() {
        return mImsRegistrationCallback.getBinder();
    }
}
