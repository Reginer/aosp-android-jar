/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.ims;

import android.annotation.NonNull;
import android.content.Context;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.ims.aidl.ICapabilityExchangeEventListener;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.IImsRegistrationCallback;
import android.telephony.ims.aidl.IPublishResponseCallback;
import android.telephony.ims.aidl.IOptionsResponseCallback;
import android.telephony.ims.aidl.ISipTransport;
import android.telephony.ims.aidl.ISubscribeResponseCallback;
import android.telephony.ims.feature.CapabilityChangeRequest;

import com.android.internal.annotations.VisibleForTesting;
import com.android.telephony.Rlog;

import java.util.List;

/**
 * A container of the IImsServiceController binder, which implements all of the RcsFeatures that
 * the platform currently supports: RCS
 */
public class RcsFeatureConnection extends FeatureConnection {
    private static final String TAG = "RcsFeatureConnection";

    public class AvailabilityCallbackManager extends
            ImsCallbackAdapterManager<IImsCapabilityCallback> {

        AvailabilityCallbackManager(Context context) {
            super(context, new Object() /* Lock object */, mSlotId, mSubId);
        }

        @Override
        public void registerCallback(IImsCapabilityCallback localCallback) {
            try {
                addCapabilityCallback(localCallback);
            } catch (RemoteException e) {
                loge("Register capability callback error: " + e);
                throw new IllegalStateException(
                        " CapabilityCallbackManager: Register callback error");
            }
        }

        @Override
        public void unregisterCallback(IImsCapabilityCallback localCallback) {
            try {
                removeCapabilityCallback(localCallback);
            } catch (RemoteException e) {
                loge("Cannot remove capability callback: " + e);
            }
        }
    }

    private class RegistrationCallbackManager extends
            ImsCallbackAdapterManager<IImsRegistrationCallback> {

        public RegistrationCallbackManager(Context context) {
            super(context, new Object() /* Lock object */, mSlotId, mSubId);
        }

        @Override
        public void registerCallback(IImsRegistrationCallback localCallback) {
            IImsRegistration imsRegistration = getRegistration();
            if (imsRegistration == null) {
                loge("Register IMS registration callback: ImsRegistration is null");
                throw new IllegalStateException("RegistrationCallbackAdapter: RcsFeature is"
                        + " not available!");
            }

            try {
                imsRegistration.addRegistrationCallback(localCallback);
            } catch (RemoteException e) {
                throw new IllegalStateException("RegistrationCallbackAdapter: RcsFeature"
                        + " binder is dead.");
            }
        }

        @Override
        public void unregisterCallback(IImsRegistrationCallback localCallback) {
            IImsRegistration imsRegistration = getRegistration();
            if (imsRegistration == null) {
                logi("Unregister IMS registration callback: ImsRegistration is null");
                return;
            }

            try {
                imsRegistration.removeRegistrationCallback(localCallback);
            } catch (RemoteException e) {
                loge("Cannot remove registration callback: " + e);
            }
        }
    }

    @VisibleForTesting
    public AvailabilityCallbackManager mAvailabilityCallbackManager;
    @VisibleForTesting
    public RegistrationCallbackManager mRegistrationCallbackManager;

    public RcsFeatureConnection(Context context, int slotId, int subId, IImsRcsFeature feature,
            IImsConfig c, IImsRegistration r, ISipTransport s) {
        super(context, slotId, subId, c, r, s);
        setBinder(feature != null ? feature.asBinder() : null);
        mAvailabilityCallbackManager = new AvailabilityCallbackManager(mContext);
        mRegistrationCallbackManager = new RegistrationCallbackManager(mContext);
    }

    public void close() {
        removeCapabilityExchangeEventListener();
        mAvailabilityCallbackManager.close();
        mRegistrationCallbackManager.close();
    }

    @Override
    protected void onRemovedOrDied() {
        close();
        super.onRemovedOrDied();
    }

    public void setCapabilityExchangeEventListener(ICapabilityExchangeEventListener listener)
            throws RemoteException {
        synchronized (mLock) {
            // Only check if service is alive. The feature status may not be READY.
            checkServiceIsAlive();
            getServiceInterface(mBinder).setCapabilityExchangeEventListener(listener);
        }
    }

    public void removeCapabilityExchangeEventListener() {
        try {
            setCapabilityExchangeEventListener(null);
        } catch (RemoteException e) {
            // If we are not still connected, there is no need to fail removing.
        }
    }

    private void checkServiceIsAlive() throws RemoteException {
        if (!sImsSupportedOnDevice) {
            throw new RemoteException("IMS is not supported on this device.");
        }
        if (!isBinderAlive()) {
            throw new RemoteException("ImsServiceProxy is not alive.");
        }
    }

    public int queryCapabilityStatus() throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            return getServiceInterface(mBinder).queryCapabilityStatus();
        }
    }

    public void addCallbackForSubscription(int subId, IImsCapabilityCallback cb) {
        mAvailabilityCallbackManager.addCallbackForSubscription(cb, subId);
    }

    public void addCallbackForSubscription(int subId, IImsRegistrationCallback cb) {
        mRegistrationCallbackManager.addCallbackForSubscription(cb, subId);
    }

    public void addCallback(IImsRegistrationCallback cb) {
        mRegistrationCallbackManager.addCallback(cb);
    }

    public void removeCallbackForSubscription(int subId, IImsCapabilityCallback cb) {
        mAvailabilityCallbackManager.removeCallback(cb);
    }

    public void removeCallbackForSubscription(int subId, IImsRegistrationCallback cb) {
        mRegistrationCallbackManager.removeCallback(cb);
    }

    public void removeCallback(IImsRegistrationCallback cb) {
        mRegistrationCallbackManager.removeCallback(cb);
    }

    // Add callback to remote service
    private void addCapabilityCallback(IImsCapabilityCallback callback) throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).addCapabilityCallback(callback);
        }
    }

    // Remove callback to remote service
    private void removeCapabilityCallback(IImsCapabilityCallback callback) throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).removeCapabilityCallback(callback);
        }
    }

    public void queryCapabilityConfiguration(int capability, int radioTech,
            IImsCapabilityCallback c) throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).queryCapabilityConfiguration(capability, radioTech, c);
        }
    }

    public void changeEnabledCapabilities(CapabilityChangeRequest request,
            IImsCapabilityCallback callback) throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).changeCapabilitiesConfiguration(request, callback);
        }
    }

    public void requestPublication(String pidfXml, IPublishResponseCallback responseCallback)
            throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).publishCapabilities(pidfXml, responseCallback);
        }
    }

    public void requestCapabilities(List<Uri> uris, ISubscribeResponseCallback c)
            throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).subscribeForCapabilities(uris, c);
        }
    }

    public void sendOptionsCapabilityRequest(Uri contactUri, List<String> myCapabilities,
            IOptionsResponseCallback callback) throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).sendOptionsCapabilityRequest(contactUri, myCapabilities,
                    callback);
        }
    }

    @Override
    @VisibleForTesting
    public Integer retrieveFeatureState() {
        if (mBinder != null) {
            try {
                return getServiceInterface(mBinder).getFeatureState();
            } catch (RemoteException e) {
                // Status check failed, don't update cache
            }
        }
        return null;
    }

    @Override
    public void onFeatureCapabilitiesUpdated(long capabilities)
    {
        // doesn't do anything for RCS yet.
    }

    @VisibleForTesting
    public IImsRcsFeature getServiceInterface(IBinder b) {
        return IImsRcsFeature.Stub.asInterface(b);
    }
    private void log(String s) {
        Rlog.d(TAG + " [" + mSlotId + "]", s);
    }

    private void logi(String s) {
        Rlog.i(TAG + " [" + mSlotId + "]", s);
    }

    private void loge(String s) {
        Rlog.e(TAG + " [" + mSlotId + "]", s);
    }
}
