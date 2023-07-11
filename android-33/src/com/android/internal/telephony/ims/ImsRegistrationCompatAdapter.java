/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.internal.telephony.ims;

import static android.telephony.ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN;
import static android.telephony.ServiceState.RIL_RADIO_TECHNOLOGY_LTE;
import static android.telephony.ServiceState.RIL_RADIO_TECHNOLOGY_NR;

import android.net.Uri;
import android.os.RemoteException;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.ArrayMap;

import com.android.ims.internal.IImsRegistrationListener;

import java.util.Map;

public class ImsRegistrationCompatAdapter extends ImsRegistrationImplBase {

    // Maps "RAT" based radio technologies to ImsRegistrationImplBase definitions.
    private static final Map<Integer, Integer> RADIO_TECH_MAPPER = new ArrayMap<>(2);
    static {
        RADIO_TECH_MAPPER.put(RIL_RADIO_TECHNOLOGY_NR, REGISTRATION_TECH_NR);
        RADIO_TECH_MAPPER.put(RIL_RADIO_TECHNOLOGY_LTE, REGISTRATION_TECH_LTE);
        RADIO_TECH_MAPPER.put(RIL_RADIO_TECHNOLOGY_IWLAN, REGISTRATION_TECH_IWLAN);
    }

    // Trampolines "old" listener events to the new interface.
    private final IImsRegistrationListener mListener = new IImsRegistrationListener.Stub() {
        @Override
        public void registrationConnected() throws RemoteException {
            onRegistered(REGISTRATION_TECH_NONE);
        }

        @Override
        public void registrationProgressing() throws RemoteException {
            onRegistering(REGISTRATION_TECH_NONE);
        }

        @Override
        public void registrationConnectedWithRadioTech(int imsRadioTech) throws RemoteException {
            onRegistered(RADIO_TECH_MAPPER.getOrDefault(imsRadioTech, REGISTRATION_TECH_NONE));
        }

        @Override
        public void registrationProgressingWithRadioTech(int imsRadioTech) throws RemoteException {
            onRegistering(RADIO_TECH_MAPPER.getOrDefault(imsRadioTech, REGISTRATION_TECH_NONE));
        }

        @Override
        public void registrationDisconnected(ImsReasonInfo imsReasonInfo) throws RemoteException {
            onDeregistered(imsReasonInfo);
        }

        @Override
        public void registrationResumed() throws RemoteException {
            // Don't care
        }

        @Override
        public void registrationSuspended() throws RemoteException {
            // Don't care
        }

        @Override
        public void registrationServiceCapabilityChanged(int serviceClass, int event)
                throws RemoteException {
            // Don't care
        }

        @Override
        public void registrationFeatureCapabilityChanged(int serviceClass, int[] enabledFeatures,
                int[] disabledFeatures) throws RemoteException {
            // Implemented in the MMTel Adapter
        }

        @Override
        public void voiceMessageCountUpdate(int count) throws RemoteException {
            // Implemented in the MMTel Adapter
        }

        @Override
        public void registrationAssociatedUriChanged(Uri[] uris) throws RemoteException {
            onSubscriberAssociatedUriChanged(uris);
        }

        @Override
        public void registrationChangeFailed(int targetAccessTech, ImsReasonInfo imsReasonInfo)
                throws RemoteException {
            onTechnologyChangeFailed(RADIO_TECH_MAPPER.getOrDefault(targetAccessTech,
                    REGISTRATION_TECH_NONE), imsReasonInfo);
        }
    };

    /**
     * Need access to the listener in order to register for events in MMTelFeature adapter
     */
    public IImsRegistrationListener getRegistrationListener() {
        return mListener;
    }
}
