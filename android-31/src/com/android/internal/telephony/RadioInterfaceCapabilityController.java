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

import android.annotation.NonNull;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.storage.StorageManager;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.telephony.Rlog;

import java.util.Collections;
import java.util.Set;

/**
 * Provides the capabilities that the Radio Interface supports on the current device.
 */
public class RadioInterfaceCapabilityController extends Handler {
    private static final String LOG_TAG =
            RadioInterfaceCapabilityController.class.getSimpleName();

    private static RadioInterfaceCapabilityController sInstance;
    private final RadioConfig mRadioConfig;
    private final CommandsInterface mCommandsInterface;
    private final boolean mRegisterForOn;
    private Set<String> mRadioInterfaceCapabilities;
    private final Object mLockRadioInterfaceCapabilities = new Object();
    private static final int EVENT_GET_HAL_DEVICE_CAPABILITIES_DONE = 100;

    /**
     * Init method to instantiate the object
     * Should only be called once.
     */
    public static RadioInterfaceCapabilityController init(final RadioConfig radioConfig,
            final CommandsInterface commandsInterface) {
        synchronized (RadioInterfaceCapabilityController.class) {
            if (sInstance == null) {
                final HandlerThread handlerThread = new HandlerThread("RHC");
                handlerThread.start();
                sInstance = new RadioInterfaceCapabilityController(radioConfig, commandsInterface,
                        handlerThread.getLooper());
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    /**
     * Static method to get instance.
     */
    public static RadioInterfaceCapabilityController getInstance() {
        if (sInstance == null) {
            Log.wtf(LOG_TAG, "getInstance null");
        }

        return sInstance;
    }

    @VisibleForTesting
    public RadioInterfaceCapabilityController(final RadioConfig radioConfig,
            final CommandsInterface commandsInterface, final Looper looper) {
        super(looper);
        mRadioConfig = radioConfig;
        mCommandsInterface = commandsInterface;
        mRegisterForOn = StorageManager.inCryptKeeperBounce();
        register();
    }

    /**
     * Gets the radio interface capabilities for the device
     */
    @NonNull
    public Set<String> getCapabilities() {
        if (mRadioInterfaceCapabilities == null) {
            // Only incur cost of synchronization block if mRadioInterfaceCapabilities isn't null
            synchronized (mLockRadioInterfaceCapabilities) {
                if (mRadioInterfaceCapabilities == null) {
                    mRadioConfig.getHalDeviceCapabilities(
                            obtainMessage(EVENT_GET_HAL_DEVICE_CAPABILITIES_DONE));
                    try {
                        if (Looper.myLooper() != getLooper()) {
                            mLockRadioInterfaceCapabilities.wait(2000);
                        }
                    } catch (final InterruptedException ignored) {
                    }

                    if (mRadioInterfaceCapabilities == null) {
                        loge("getRadioInterfaceCapabilities: Radio Capabilities not "
                                + "loaded in time");
                        return new ArraySet<>();
                    }
                }
            }
        }
        return mRadioInterfaceCapabilities;
    }

    private void setupCapabilities(final @NonNull AsyncResult ar) {
        if (mRadioInterfaceCapabilities == null) {
            synchronized (mLockRadioInterfaceCapabilities) {
                if (mRadioInterfaceCapabilities == null) {
                    if (ar.exception != null) {
                        loge("setupRadioInterfaceCapabilities: " + ar.exception);
                    }
                    if (ar.result == null) {
                        loge("setupRadioInterfaceCapabilities: ar.result is null");
                        return;
                    }
                    log("setupRadioInterfaceCapabilities: "
                            + "mRadioInterfaceCapabilities now setup");
                    mRadioInterfaceCapabilities =
                            Collections.unmodifiableSet((Set<String>) ar.result);
                    if (mRadioInterfaceCapabilities != null) {
                        unregister();
                    }
                }
                mLockRadioInterfaceCapabilities.notify();
            }
        }
    }

    private void register() {
        // There is no radio HAL, capabilities are irrelevant in this case.
        if (mCommandsInterface == null) {
            mRadioInterfaceCapabilities = Collections.unmodifiableSet(new ArraySet<>());
            return;
        }

        if (mRegisterForOn) {
            mCommandsInterface.registerForOn(this, Phone.EVENT_RADIO_ON, null);
        } else {
            mCommandsInterface.registerForAvailable(this, Phone.EVENT_RADIO_AVAILABLE, null);
        }
    }

    private void unregister() {
        if (mRegisterForOn) {
            mCommandsInterface.unregisterForOn(this);
        } else {
            mCommandsInterface.unregisterForAvailable(this);
        }
    }

    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {
            case Phone.EVENT_RADIO_AVAILABLE:
            case Phone.EVENT_RADIO_ON:
                getCapabilities();
                break;
            case EVENT_GET_HAL_DEVICE_CAPABILITIES_DONE:
                setupCapabilities((AsyncResult) msg.obj);
                break;
        }
    }

    private static void log(final String s) {
        Rlog.d(LOG_TAG, s);
    }

    private static void loge(final String s) {
        Rlog.e(LOG_TAG, s);
    }
}
