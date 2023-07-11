/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.ims;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsService;
import android.telephony.ims.RtpHeaderExtensionType;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsConfigCallback;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.IImsRegistrationCallback;
import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.aidl.ISipTransport;
import android.telephony.ims.feature.CapabilityChangeRequest;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsEcbmImplBase;
import android.telephony.ims.stub.ImsSmsImplBase;
import android.util.Log;

import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsUt;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

/**
 * A container of the IImsServiceController binder, which implements all of the ImsFeatures that
 * the platform currently supports: MMTel
 */

public class MmTelFeatureConnection extends FeatureConnection {
    protected static final String TAG = "MmTelFeatureConn";

    private class ImsRegistrationCallbackAdapter extends
            ImsCallbackAdapterManager<IImsRegistrationCallback> {

        public ImsRegistrationCallbackAdapter(Context context, Object lock) {
            super(context, lock, mSlotId, mSubId);
        }

        @Override
        public void registerCallback(IImsRegistrationCallback localCallback) {
            IImsRegistration imsRegistration = getRegistration();
            if (imsRegistration != null) {
                try {
                    imsRegistration.addRegistrationCallback(localCallback);
                } catch (RemoteException e) {
                    throw new IllegalStateException("ImsRegistrationCallbackAdapter: MmTelFeature"
                            + " binder is dead.");
                }
            } else {
                Log.e(TAG + " [" + mSlotId + "]", "ImsRegistrationCallbackAdapter: ImsRegistration"
                        + " is null");
                throw new IllegalStateException("ImsRegistrationCallbackAdapter: MmTelFeature is"
                        + "not available!");
            }
        }

        @Override
        public void unregisterCallback(IImsRegistrationCallback localCallback) {
            IImsRegistration imsRegistration = getRegistration();
            if (imsRegistration != null) {
                try {
                    imsRegistration.removeRegistrationCallback(localCallback);
                } catch (RemoteException | IllegalStateException e) {
                    Log.w(TAG + " [" + mSlotId + "]", "ImsRegistrationCallbackAdapter -"
                            + " unregisterCallback: couldn't remove registration callback"
                            + " Exception: " + e.getMessage());
                }
            } else {
                Log.e(TAG + " [" + mSlotId + "]", "ImsRegistrationCallbackAdapter: ImsRegistration"
                        + " is null");
            }
        }
    }

    private class CapabilityCallbackManager extends ImsCallbackAdapterManager<IImsCapabilityCallback> {

        public CapabilityCallbackManager(Context context, Object lock) {
            super(context, lock, mSlotId, mSubId);
        }

        @Override
        public void registerCallback(IImsCapabilityCallback localCallback) {
            IImsMmTelFeature binder;
            synchronized (mLock) {
                try {
                    checkServiceIsReady();
                    binder = getServiceInterface(mBinder);
                } catch (RemoteException e) {
                    throw new IllegalStateException("CapabilityCallbackManager - MmTelFeature"
                            + " binder is dead.");
                }
            }
            if (binder != null) {
                try {
                binder.addCapabilityCallback(localCallback);
                } catch (RemoteException e) {
                    throw new IllegalStateException(" CapabilityCallbackManager - MmTelFeature"
                            + " binder is null.");
                }
            } else {
                Log.w(TAG + " [" + mSlotId + "]", "CapabilityCallbackManager, register: Couldn't"
                        + " get binder");
                throw new IllegalStateException("CapabilityCallbackManager: MmTelFeature is"
                        + " not available!");
            }
        }

        @Override
        public void unregisterCallback(IImsCapabilityCallback localCallback) {
            IImsMmTelFeature binder;
            synchronized (mLock) {
                if (!isBinderAlive()) {
                    Log.w(TAG + " [" + mSlotId + "]", "CapabilityCallbackManager, unregister:"
                            + " binder is not alive");
                    return;
                }
                binder = getServiceInterface(mBinder);
            }
            if (binder != null) {
                try {
                    binder.removeCapabilityCallback(localCallback);
                } catch (RemoteException | IllegalStateException e) {
                    Log.w(TAG + " [" + mSlotId + "]", "CapabilityCallbackManager, unregister:"
                            + " Binder is dead. Exception: " + e.getMessage());
                }
            } else {
                Log.w(TAG + " [" + mSlotId + "]", "CapabilityCallbackManager, unregister:"
                        + " binder is null.");
            }
        }
    }

    private class ProvisioningCallbackManager extends ImsCallbackAdapterManager<IImsConfigCallback> {
        public ProvisioningCallbackManager (Context context, Object lock) {
            super(context, lock, mSlotId, mSubId);
        }

        @Override
        public void registerCallback(IImsConfigCallback localCallback) {
            IImsConfig binder = getConfig();
            if (binder == null) {
                // Config interface is not currently available.
                Log.w(TAG + " [" + mSlotId + "]", "ProvisioningCallbackManager - couldn't register,"
                        + " binder is null.");
                throw new IllegalStateException("ImsConfig is not available!");
            }
            try {
                binder.addImsConfigCallback(localCallback);
            }catch (RemoteException e) {
                throw new IllegalStateException("ImsService is not available!");
            }
        }

        @Override
        public void unregisterCallback(IImsConfigCallback localCallback) {
            IImsConfig binder = getConfig();
            if (binder == null) {
                Log.w(TAG + " [" + mSlotId + "]", "ProvisioningCallbackManager - couldn't"
                        + " unregister, binder is null.");
                return;
            }
            try {
                binder.removeImsConfigCallback(localCallback);
            } catch (RemoteException | IllegalStateException e) {
                Log.w(TAG + " [" + mSlotId + "]", "ProvisioningCallbackManager - couldn't"
                        + " unregister, binder is dead. Exception: " + e.getMessage());
            }
        }
    }

    private static final class BinderAccessState<T> {
        /**
         * We have not tried to get the interface yet.
         */
        static final int STATE_NOT_SET = 0;
        /**
         * We have tried to get the interface, but it is not supported.
         */
        static final int STATE_NOT_SUPPORTED = 1;
        /**
         * The interface is available from the service.
         */
        static final int STATE_AVAILABLE = 2;

        public static <T> BinderAccessState<T> of(T value) {
            return new BinderAccessState<>(value);
        }

        private final int mState;
        private final T mInterface;

        public BinderAccessState(int state) {
            mState = state;
            mInterface = null;
        }

        public BinderAccessState(T binderInterface) {
            mState = STATE_AVAILABLE;
            mInterface = binderInterface;
        }

        public int getState() {
            return mState;
        }

        public T getInterface() {
            return mInterface;
        }
    }

    // Updated by IImsServiceFeatureCallback when FEATURE_EMERGENCY_MMTEL is sent.
    private boolean mSupportsEmergencyCalling = false;
    private BinderAccessState<ImsEcbm> mEcbm =
            new BinderAccessState<>(BinderAccessState.STATE_NOT_SET);
    private BinderAccessState<ImsMultiEndpoint> mMultiEndpoint =
            new BinderAccessState<>(BinderAccessState.STATE_NOT_SET);
    private MmTelFeature.Listener mMmTelFeatureListener;
    private ImsUt mUt;

    private final ImsRegistrationCallbackAdapter mRegistrationCallbackManager;
    private final CapabilityCallbackManager mCapabilityCallbackManager;
    private final ProvisioningCallbackManager mProvisioningCallbackManager;

    public MmTelFeatureConnection(Context context, int slotId, int subId, IImsMmTelFeature f,
            IImsConfig c, IImsRegistration r, ISipTransport s) {
        super(context, slotId, subId, c, r, s);

        setBinder((f != null) ? f.asBinder() : null);
        mRegistrationCallbackManager = new ImsRegistrationCallbackAdapter(context, mLock);
        mCapabilityCallbackManager = new CapabilityCallbackManager(context, mLock);
        mProvisioningCallbackManager = new ProvisioningCallbackManager(context, mLock);
    }

    @Override
    protected void onRemovedOrDied() {
        // Release all callbacks being tracked and unregister them from the connected MmTelFeature.
        mRegistrationCallbackManager.close();
        mCapabilityCallbackManager.close();
        mProvisioningCallbackManager.close();
        // Close mUt interface separately from other listeners, as it is not tied directly to
        // calling. There is still a limitation currently that only one UT listener can be set
        // (through ImsPhoneCallTracker), but this could be relaxed in the future via the ability
        // to register multiple callbacks.
        synchronized (mLock) {
            if (mUt != null) {
                mUt.close();
                mUt = null;
            }
            closeConnection();
            super.onRemovedOrDied();
        }
    }

    public boolean isEmergencyMmTelAvailable() {
        return mSupportsEmergencyCalling;
    }

    /**
     * Opens the connection to the {@link MmTelFeature} and establishes a listener back to the
     * framework. Calling this method multiple times will reset the listener attached to the
     * {@link MmTelFeature}.
     * @param mmTelListener A {@link MmTelFeature.Listener} that will be used by the
     *         {@link MmTelFeature} to notify the framework of mmtel calling updates.
     * @param ecbmListener Listener used to listen for ECBM updates from {@link ImsEcbmImplBase}
     *         implementation.
     */
    public void openConnection(MmTelFeature.Listener mmTelListener,
            ImsEcbmStateListener ecbmListener,
            ImsExternalCallStateListener multiEndpointListener) throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            mMmTelFeatureListener = mmTelListener;
            getServiceInterface(mBinder).setListener(mmTelListener);
            setEcbmInterface(ecbmListener);
            setMultiEndpointInterface(multiEndpointListener);
        }
    }

    /**
     * Closes the connection to the {@link MmTelFeature} if it was previously opened via
     * {@link #openConnection} by removing all listeners.
     */
    public void closeConnection() {
        synchronized (mLock) {
            if (!isBinderAlive()) return;
            try {
                if (mMmTelFeatureListener != null) {
                    mMmTelFeatureListener = null;
                    getServiceInterface(mBinder).setListener(null);
                }
                if (mEcbm.getState() == BinderAccessState.STATE_AVAILABLE) {
                    mEcbm.getInterface().setEcbmStateListener(null);
                    mEcbm = new BinderAccessState<>(BinderAccessState.STATE_NOT_SET);
                }
                if (mMultiEndpoint.getState() == BinderAccessState.STATE_AVAILABLE) {
                    mMultiEndpoint.getInterface().setExternalCallStateListener(null);
                    mMultiEndpoint = new BinderAccessState<>(BinderAccessState.STATE_NOT_SET);
                }
            } catch (RemoteException | IllegalStateException e) {
                Log.w(TAG + " [" + mSlotId + "]", "closeConnection: couldn't remove listeners!" +
                        " Exception: " + e.getMessage());
            }
        }
    }

    public void addRegistrationCallback(IImsRegistrationCallback callback) {
        mRegistrationCallbackManager.addCallback(callback);
    }

    public void addRegistrationCallbackForSubscription(IImsRegistrationCallback callback,
            int subId) {
        mRegistrationCallbackManager.addCallbackForSubscription(callback , subId);
    }

    public void removeRegistrationCallback(IImsRegistrationCallback callback) {
        mRegistrationCallbackManager.removeCallback(callback);
    }

    public void removeRegistrationCallbackForSubscription(IImsRegistrationCallback callback,
            int subId) {
        mRegistrationCallbackManager.removeCallback(callback);
    }

    public void addCapabilityCallback(IImsCapabilityCallback callback) {
        mCapabilityCallbackManager.addCallback(callback);
    }

    public void addCapabilityCallbackForSubscription(IImsCapabilityCallback callback,
            int subId) {
        mCapabilityCallbackManager.addCallbackForSubscription(callback, subId);
    }

    public void removeCapabilityCallback(IImsCapabilityCallback callback) {
        mCapabilityCallbackManager.removeCallback(callback);
    }

    public void removeCapabilityCallbackForSubscription(IImsCapabilityCallback callback,
            int subId) {
        mCapabilityCallbackManager.removeCallback(callback);
    }

    public void addProvisioningCallbackForSubscription(IImsConfigCallback callback,
            int subId) {
        mProvisioningCallbackManager.addCallbackForSubscription(callback, subId);
    }

    public void removeProvisioningCallbackForSubscription(IImsConfigCallback callback,
            int subId) {
        mProvisioningCallbackManager.removeCallback(callback);
    }

    public void changeEnabledCapabilities(CapabilityChangeRequest request,
            IImsCapabilityCallback callback) throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).changeCapabilitiesConfiguration(request, callback);
        }
    }

    public void queryEnabledCapabilities(int capability, int radioTech,
            IImsCapabilityCallback callback) throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).queryCapabilityConfiguration(capability, radioTech,
                    callback);
        }
    }

    public MmTelFeature.MmTelCapabilities queryCapabilityStatus() throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            return new MmTelFeature.MmTelCapabilities(
                    getServiceInterface(mBinder).queryCapabilityStatus());
        }
    }

    public ImsCallProfile createCallProfile(int callServiceType, int callType)
            throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            return getServiceInterface(mBinder).createCallProfile(callServiceType, callType);
        }
    }

    public void changeOfferedRtpHeaderExtensionTypes(Set<RtpHeaderExtensionType> types)
            throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).changeOfferedRtpHeaderExtensionTypes(
                    new ArrayList<>(types));
        }
    }

    public IImsCallSession createCallSession(ImsCallProfile profile)
            throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            return getServiceInterface(mBinder).createCallSession(profile);
        }
    }

    public ImsUt createOrGetUtInterface() throws RemoteException {
        synchronized (mLock) {
            if (mUt != null) return mUt;

            checkServiceIsReady();
            IImsUt imsUt = getServiceInterface(mBinder).getUtInterface();
            // This will internally set up a listener on the ImsUtImplBase interface, and there is
            // a limitation that there can only be one. If multiple connections try to create this
            // UT interface, it will throw an IllegalStateException.
            mUt = (imsUt != null) ? new ImsUt(imsUt, mContext.getMainExecutor()) : null;
            return mUt;
        }
    }

    private void setEcbmInterface(ImsEcbmStateListener ecbmListener) throws RemoteException {
        synchronized (mLock) {
            if (mEcbm.getState() != BinderAccessState.STATE_NOT_SET) {
                throw new IllegalStateException("ECBM interface already open");
            }

            checkServiceIsReady();
            IImsEcbm imsEcbm = getServiceInterface(mBinder).getEcbmInterface();
            mEcbm = (imsEcbm != null) ? BinderAccessState.of(new ImsEcbm(imsEcbm)) :
                    new BinderAccessState<>(BinderAccessState.STATE_NOT_SUPPORTED);
            if (mEcbm.getState() == BinderAccessState.STATE_AVAILABLE) {
                // May throw an IllegalStateException if a listener already exists.
                mEcbm.getInterface().setEcbmStateListener(ecbmListener);
            }
        }
    }

    public ImsEcbm getEcbmInterface() {
        synchronized (mLock) {
            if (mEcbm.getState() == BinderAccessState.STATE_NOT_SET) {
                throw new IllegalStateException("ECBM interface has not been opened");
            }

            return mEcbm.getState() == BinderAccessState.STATE_AVAILABLE ?
                    mEcbm.getInterface() : null;
        }
    }

    public void setUiTTYMode(int uiTtyMode, Message onComplete)
            throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).setUiTtyMode(uiTtyMode, onComplete);
        }
    }

    private void setMultiEndpointInterface(ImsExternalCallStateListener listener)
            throws RemoteException {
        synchronized (mLock) {
            if (mMultiEndpoint.getState() != BinderAccessState.STATE_NOT_SET) {
                throw new IllegalStateException("multiendpoint interface is already open");
            }

            checkServiceIsReady();
            IImsMultiEndpoint imEndpoint = getServiceInterface(mBinder).getMultiEndpointInterface();
            mMultiEndpoint = (imEndpoint != null)
                    ? BinderAccessState.of(new ImsMultiEndpoint(imEndpoint)) :
                    new BinderAccessState<>(BinderAccessState.STATE_NOT_SUPPORTED);
            if (mMultiEndpoint.getState() == BinderAccessState.STATE_AVAILABLE) {
                // May throw an IllegalStateException if a listener already exists.
                mMultiEndpoint.getInterface().setExternalCallStateListener(listener);
            }
        }
    }

    public void sendSms(int token, int messageRef, String format, String smsc, boolean isRetry,
            byte[] pdu) throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).sendSms(token, messageRef, format, smsc, isRetry,
                    pdu);
        }
    }

    public void acknowledgeSms(int token, int messageRef,
            @ImsSmsImplBase.SendStatusResult int result) throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).acknowledgeSms(token, messageRef, result);
        }
    }

    public void acknowledgeSmsReport(int token, int messageRef,
            @ImsSmsImplBase.StatusReportResult int result) throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).acknowledgeSmsReport(token, messageRef, result);
        }
    }

    public String getSmsFormat() throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            return getServiceInterface(mBinder).getSmsFormat();
        }
    }

    public void onSmsReady() throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).onSmsReady();
        }
    }

    public void setSmsListener(IImsSmsListener listener) throws RemoteException {
        synchronized (mLock) {
            checkServiceIsReady();
            getServiceInterface(mBinder).setSmsListener(listener);
        }
    }

    public @MmTelFeature.ProcessCallResult int shouldProcessCall(boolean isEmergency,
            String[] numbers) throws RemoteException {
        if (isEmergency && !isEmergencyMmTelAvailable()) {
            // Don't query the ImsService if emergency calling is not available on the ImsService.
            Log.i(TAG + " [" + mSlotId + "]", "MmTel does not support emergency over IMS, fallback"
                    + " to CS.");
            return MmTelFeature.PROCESS_CALL_CSFB;
        }
        synchronized (mLock) {
            checkServiceIsReady();
            return getServiceInterface(mBinder).shouldProcessCall(numbers);
        }
    }

    @Override
    protected Integer retrieveFeatureState() {
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
        synchronized (mLock) {
            mSupportsEmergencyCalling =
                    ((capabilities | ImsService.CAPABILITY_EMERGENCY_OVER_MMTEL) > 0);
        }
    }

    private IImsMmTelFeature getServiceInterface(IBinder b) {
        return IImsMmTelFeature.Stub.asInterface(b);
    }
}
