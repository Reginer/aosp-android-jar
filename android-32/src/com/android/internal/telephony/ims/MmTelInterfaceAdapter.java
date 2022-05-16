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
 * limitations under the License
 */

package com.android.internal.telephony.ims;

import android.app.PendingIntent;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;

import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsConfig;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMMTelFeature;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsRegistrationListener;
import com.android.ims.internal.IImsUt;

/**
 * Defines "generic" MmTel commands and provides a concrete implementation for compatibility
 * purposes.
 */

public class MmTelInterfaceAdapter {

    protected IBinder mBinder;
    protected int mSlotId;

    public MmTelInterfaceAdapter(int slotId, IBinder binder) {
        mBinder = binder;
        mSlotId = slotId;
    }

    public int startSession(PendingIntent incomingCallIntent, IImsRegistrationListener listener)
            throws RemoteException {
        return getInterface().startSession(incomingCallIntent, listener);
    }

    public void endSession(int sessionId) throws RemoteException {
        getInterface().endSession(sessionId);
    }

    public boolean isConnected(int callSessionType, int callType) throws RemoteException {
        return getInterface().isConnected(callSessionType, callType);
    }

    public boolean isOpened() throws RemoteException {
        return getInterface().isOpened();
    }

    public int getFeatureState() throws RemoteException {
        return getInterface().getFeatureStatus();
    }

    public void addRegistrationListener(IImsRegistrationListener listener) throws RemoteException {
        getInterface().addRegistrationListener(listener);
    }

    public void removeRegistrationListener(IImsRegistrationListener listener)
            throws RemoteException {
        getInterface().removeRegistrationListener(listener);
    }

    public ImsCallProfile createCallProfile(int sessionId, int callSessionType, int callType)
            throws RemoteException {
        return getInterface().createCallProfile(sessionId, callSessionType, callType);
    }

    public IImsCallSession createCallSession(int sessionId, ImsCallProfile profile)
            throws RemoteException {
        return getInterface().createCallSession(sessionId, profile);
    }

    public IImsCallSession getPendingCallSession(int sessionId, String callId)
            throws RemoteException {
        return getInterface().getPendingCallSession(sessionId, callId);
    }

    public IImsUt getUtInterface() throws RemoteException {
        return getInterface().getUtInterface();
    }

    public IImsConfig getConfigInterface() throws RemoteException {
        return getInterface().getConfigInterface();
    }

    public void turnOnIms() throws RemoteException {
        getInterface().turnOnIms();
    }

    public void turnOffIms() throws RemoteException {
        getInterface().turnOffIms();
    }

    public IImsEcbm getEcbmInterface() throws RemoteException {
        return getInterface().getEcbmInterface();
    }

    public void setUiTTYMode(int uiTtyMode, Message onComplete) throws RemoteException {
        getInterface().setUiTTYMode(uiTtyMode, onComplete);
    }

    public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
        return getInterface().getMultiEndpointInterface();
    }

    private IImsMMTelFeature getInterface() throws RemoteException {
        IImsMMTelFeature feature = IImsMMTelFeature.Stub.asInterface(mBinder);
        if (feature == null) {
            throw new RemoteException("Binder not Available");
        }
        return feature;
    }
}
