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

import android.os.RemoteException;

/**
 * A holder for IRadio services. Use getHidl to get IRadio 1.0 and call the HIDL implementations or
 * getAidl to get the AIDL service and call the AIDL implementations of the HAL APIs.
 */
public abstract class RadioServiceProxy {
    boolean mIsAidl;
    HalVersion mHalVersion = RIL.RADIO_HAL_VERSION_UNKNOWN;
    volatile android.hardware.radio.V1_0.IRadio mRadioProxy = null;

    /**
     * Whether RadioServiceProxy is an AIDL or HIDL implementation
     * @return true if AIDL, false if HIDL
     */
    public boolean isAidl() {
        return mIsAidl;
    }

    /**
     * Set IRadio as the HIDL implementation for RadioServiceProxy
     * @param halVersion Radio HAL version
     * @param radio      IRadio implementation
     */
    public void setHidl(HalVersion halVersion, android.hardware.radio.V1_0.IRadio radio) {
        mHalVersion = halVersion;
        mRadioProxy = radio;
        mIsAidl = false;
    }

    /**
     * Get the HIDL implementation of RadioServiceProxy
     * @return IRadio implementation
     */
    public android.hardware.radio.V1_0.IRadio getHidl() {
        return mRadioProxy;
    }

    /**
     * Reset RadioServiceProxy
     */
    public void clear() {
        mHalVersion = RIL.RADIO_HAL_VERSION_UNKNOWN;
        mRadioProxy = null;
    }

    /**
     * Check whether an implementation exists for this service
     * @return false if there is neither a HIDL nor AIDL implementation
     */
    public boolean isEmpty() {
        return mRadioProxy == null;
    }

    /**
     * Call responseAcknowledgement for the service
     * @throws RemoteException
     */
    public void responseAcknowledgement() throws RemoteException {
        if (isEmpty()) return;
        if (!isAidl()) mRadioProxy.responseAcknowledgement();
    }
}
