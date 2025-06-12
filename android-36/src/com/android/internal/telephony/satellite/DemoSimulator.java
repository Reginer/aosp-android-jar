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

package com.android.internal.telephony.satellite;

import android.annotation.NonNull;
import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.IIntegerConsumer;
import android.telephony.satellite.stub.ISatelliteListener;
import android.telephony.satellite.stub.NtnSignalStrength;
import android.telephony.satellite.stub.SatelliteModemState;
import android.telephony.satellite.stub.SatelliteResult;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

public class DemoSimulator extends StateMachine {
    private static final String TAG = "DemoSimulator";
    private static final boolean DBG = true;

    private static final int EVENT_SATELLITE_MODE_ON = 1;
    private static final int EVENT_SATELLITE_MODE_OFF = 2;
    private static final int EVENT_DEVICE_ALIGNED_WITH_SATELLITE = 3;
    protected static final int EVENT_DEVICE_ALIGNED = 4;
    protected static final int EVENT_DEVICE_NOT_ALIGNED = 5;

    @NonNull private static DemoSimulator sInstance;

    @NonNull private final Context mContext;
    @NonNull private final SatelliteController mSatelliteController;
    @NonNull private final PowerOffState mPowerOffState = new PowerOffState();
    @NonNull private final NotConnectedState mNotConnectedState = new NotConnectedState();
    @NonNull private final ConnectedState mConnectedState = new ConnectedState();
    @NonNull private final Object mLock = new Object();
    @GuardedBy("mLock")
    private boolean mIsAligned = false;
    private ISatelliteListener mISatelliteListener;

    /**
     * @return The singleton instance of DemoSimulator.
     */
    public static DemoSimulator getInstance() {
        if (sInstance == null) {
            Log.e(TAG, "DemoSimulator was not yet initialized.");
        }
        return sInstance;
    }

    /**
     * Create the DemoSimulator singleton instance.
     *
     * @param context The Context for the DemoSimulator.
     * @return The singleton instance of DemoSimulator.
     */
    public static DemoSimulator make(@NonNull Context context,
            @NonNull SatelliteController satelliteController) {
        if (sInstance == null) {
            sInstance = new DemoSimulator(context, Looper.getMainLooper(), satelliteController);
        }
        return sInstance;
    }

    /**
     * Create a DemoSimulator.
     *
     * @param context The Context for the DemoSimulator.
     * @param looper The looper associated with the handler of this class.
     */
    protected DemoSimulator(@NonNull Context context, @NonNull Looper looper,
            @NonNull SatelliteController satelliteController) {
        super(TAG, looper);

        mContext = context;
        mSatelliteController = satelliteController;
        addState(mPowerOffState);
        addState(mNotConnectedState);
        addState(mConnectedState);
        setInitialState(mPowerOffState);
        start();
    }

    private class PowerOffState extends State {
        @Override
        public void enter() {
            logd("Entering PowerOffState");
        }

        @Override
        public void exit() {
            logd("Exiting PowerOffState");
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("PowerOffState: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_SATELLITE_MODE_ON:
                    transitionTo(mNotConnectedState);
                    break;
            }
            // Ignore all unexpected events.
            return HANDLED;
        }
    }

    private class NotConnectedState extends State {
        @Override
        public void enter() {
            logd("Entering NotConnectedState");

            try {
                NtnSignalStrength ntnSignalStrength = new NtnSignalStrength();
                ntnSignalStrength.signalStrengthLevel = 0;
                mISatelliteListener.onSatelliteModemStateChanged(
                        SatelliteModemState.SATELLITE_MODEM_STATE_OUT_OF_SERVICE);
                mISatelliteListener.onNtnSignalStrengthChanged(ntnSignalStrength);

                synchronized (mLock) {
                    if (mIsAligned) {
                        handleEventDeviceAlignedWithSatellite(true);
                    }
                }
            } catch (RemoteException e) {
                loge("NotConnectedState: RemoteException " + e);
            }
        }

        @Override
        public void exit() {
            logd("Exiting NotConnectedState");
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("NotConnectedState: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_SATELLITE_MODE_OFF:
                    transitionTo(mPowerOffState);
                    break;
                case EVENT_DEVICE_ALIGNED_WITH_SATELLITE:
                    handleEventDeviceAlignedWithSatellite((boolean) msg.obj);
                    break;
                case EVENT_DEVICE_ALIGNED:
                    transitionTo(mConnectedState);
                    break;
            }
            // Ignore all unexpected events.
            return HANDLED;
        }

        private void handleEventDeviceAlignedWithSatellite(boolean isAligned) {
            if (isAligned && !hasMessages(EVENT_DEVICE_ALIGNED)) {
                long durationMillis = mSatelliteController.getDemoPointingAlignedDurationMillis();
                logd("NotConnectedState: handleEventAlignedWithSatellite isAligned=true."
                        + " Send delayed EVENT_DEVICE_ALIGNED message in"
                        + " durationMillis=" + durationMillis);
                sendMessageDelayed(EVENT_DEVICE_ALIGNED, durationMillis);
            } else if (!isAligned && hasMessages(EVENT_DEVICE_ALIGNED)) {
                logd("NotConnectedState: handleEventAlignedWithSatellite isAligned=false."
                        + " Remove EVENT_DEVICE_ALIGNED message.");
                removeMessages(EVENT_DEVICE_ALIGNED);
            }
        }
    }

    private class ConnectedState extends State {
        @Override
        public void enter() {
            logd("Entering ConnectedState");

            try {
                NtnSignalStrength ntnSignalStrength = new NtnSignalStrength();
                ntnSignalStrength.signalStrengthLevel = 2;
                mISatelliteListener.onSatelliteModemStateChanged(
                        SatelliteModemState.SATELLITE_MODEM_STATE_IN_SERVICE);
                mISatelliteListener.onNtnSignalStrengthChanged(ntnSignalStrength);

                synchronized (mLock) {
                    if (!mIsAligned) {
                        handleEventDeviceAlignedWithSatellite(false);
                    }
                }
            } catch (RemoteException e) {
                loge("ConnectedState: RemoteException " + e);
            }
        }

        @Override
        public void exit() {
            logd("Exiting ConnectedState");
        }

        @Override
        public boolean processMessage(Message msg) {
            if (DBG) log("ConnectedState: processing " + getWhatToString(msg.what));
            switch (msg.what) {
                case EVENT_SATELLITE_MODE_OFF:
                    transitionTo(mPowerOffState);
                    break;
                case EVENT_DEVICE_ALIGNED_WITH_SATELLITE:
                    handleEventDeviceAlignedWithSatellite((boolean) msg.obj);
                    break;
                case EVENT_DEVICE_NOT_ALIGNED:
                    transitionTo(mNotConnectedState);
                    break;
            }
            // Ignore all unexpected events.
            return HANDLED;
        }

        private void handleEventDeviceAlignedWithSatellite(boolean isAligned) {
            if (!isAligned && !hasMessages(EVENT_DEVICE_NOT_ALIGNED)) {
                long durationMillis =
                        mSatelliteController.getDemoPointingNotAlignedDurationMillis();
                logd("ConnectedState: handleEventAlignedWithSatellite isAligned=false."
                        + " Send delayed EVENT_DEVICE_NOT_ALIGNED message"
                        + " in durationMillis=" + durationMillis);
                sendMessageDelayed(EVENT_DEVICE_NOT_ALIGNED, durationMillis);
            } else if (isAligned && hasMessages(EVENT_DEVICE_NOT_ALIGNED)) {
                logd("ConnectedState: handleEventAlignedWithSatellite isAligned=true."
                        + " Remove EVENT_DEVICE_NOT_ALIGNED message.");
                removeMessages(EVENT_DEVICE_NOT_ALIGNED);
            }
        }
    }

    /**
     * @return the string for msg.what
     */
    @Override
    protected String getWhatToString(int what) {
        String whatString;
        switch (what) {
            case EVENT_SATELLITE_MODE_ON:
                whatString = "EVENT_SATELLITE_MODE_ON";
                break;
            case EVENT_SATELLITE_MODE_OFF:
                whatString = "EVENT_SATELLITE_MODE_OFF";
                break;
            case EVENT_DEVICE_ALIGNED_WITH_SATELLITE:
                whatString = "EVENT_DEVICE_ALIGNED_WITH_SATELLITE";
                break;
            case EVENT_DEVICE_ALIGNED:
                whatString = "EVENT_DEVICE_ALIGNED";
                break;
            case EVENT_DEVICE_NOT_ALIGNED:
                whatString = "EVENT_DEVICE_NOT_ALIGNED";
                break;
            default:
                whatString = "UNKNOWN EVENT " + what;
        }
        return whatString;
    }

    /**
     * Register the callback interface with satellite service.
     *
     * @param listener The callback interface to handle satellite service indications.
     */
    public void setSatelliteListener(@NonNull ISatelliteListener listener) {
        mISatelliteListener = listener;
    }

    /**
     * Allow cellular modem scanning while satellite mode is on.
     *
     * @param enabled  {@code true} to enable cellular modem while satellite mode is on
     *                             and {@code false} to disable
     * @param errorCallback The callback to receive the error code result of the operation.
     */
    public void enableTerrestrialNetworkScanWhileSatelliteModeIsOn(boolean enabled,
            @NonNull IIntegerConsumer errorCallback) {
        try {
            errorCallback.accept(SatelliteResult.SATELLITE_RESULT_SUCCESS);
        } catch (RemoteException e) {
            loge("enableTerrestrialNetworkScanWhileSatelliteModeIsOn: RemoteException " + e);
        }
    }

    /**
     * This function is used by {@link SatelliteSessionController} to notify {@link DemoSimulator}
     * that satellite mode is ON.
     */
    public void onSatelliteModeOn() {
        if (mSatelliteController.isDemoModeEnabled()) {
            sendMessage(EVENT_SATELLITE_MODE_ON);
        }
    }

    /**
     * This function is used by {@link SatelliteSessionController} to notify {@link DemoSimulator}
     * that satellite mode is OFF.
     */
    public void onSatelliteModeOff() {
        sendMessage(EVENT_SATELLITE_MODE_OFF);
    }

    /**
     * Set whether the device is aligned with the satellite.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void setDeviceAlignedWithSatellite(boolean isAligned) {
        synchronized (mLock) {
            if (mSatelliteController.isDemoModeEnabled()) {
                mIsAligned = isAligned;
                sendMessage(EVENT_DEVICE_ALIGNED_WITH_SATELLITE, isAligned);
            }
        }
    }
}
