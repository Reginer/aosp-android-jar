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

import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.ISipTransport;
import android.telephony.ims.compat.ImsService;
import android.telephony.ims.compat.feature.ImsFeature;
import android.telephony.ims.compat.feature.MMTelFeature;
import android.util.Log;
import android.util.SparseArray;

import com.android.ims.ImsFeatureBinderRepository;
import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.ims.internal.IImsMMTelFeature;
import com.android.ims.internal.IImsServiceController;
import com.android.internal.annotations.VisibleForTesting;

/**
 * Manages the Binding lifecycle of one ImsService as well as the relevant ImsFeatures that the
 * ImsService will support.
 *
 * Compatibility interface for interacting with older implementations of ImsService. The older
 * ImsService implementation is contained within the android.telephony.ims.compat.* namespace.
 * Newer implementations of ImsService should use the current APIs contained in
 * android.telephony.ims.*.
 */
public class ImsServiceControllerCompat extends ImsServiceController {

    private static final String TAG = "ImsSCCompat";

    private IImsServiceController mServiceController;

    private final SparseArray<MmTelFeatureCompatAdapter> mMmTelCompatAdapters = new SparseArray<>();
    private final SparseArray<ImsConfigCompatAdapter> mConfigCompatAdapters = new SparseArray<>();
    private final SparseArray<ImsRegistrationCompatAdapter> mRegCompatAdapters =
            new SparseArray<>();

    private final MmTelFeatureCompatFactory mMmTelFeatureFactory;

    /**
     * Used to inject test instances of MmTelFeatureCompatAdapter
     */
    @VisibleForTesting
    public interface MmTelFeatureCompatFactory {
        /**
         * @return A new instance of {@link MmTelFeatureCompatAdapter}
         */
        MmTelFeatureCompatAdapter create(Context context, int slotId,
                MmTelInterfaceAdapter compatFeature);
    }

    public ImsServiceControllerCompat(Context context, ComponentName componentName,
            ImsServiceController.ImsServiceControllerCallbacks callbacks,
            ImsFeatureBinderRepository repo) {
        super(context, componentName, callbacks, repo);
        mMmTelFeatureFactory = MmTelFeatureCompatAdapter::new;
    }

    @VisibleForTesting
    public ImsServiceControllerCompat(Context context, ComponentName componentName,
            ImsServiceControllerCallbacks callbacks, Handler handler, RebindRetry rebindRetry,
            ImsFeatureBinderRepository repo, MmTelFeatureCompatFactory factory) {
        super(context, componentName, callbacks, handler, rebindRetry, repo);
        mMmTelFeatureFactory = factory;
    }

    @Override
    protected final String getServiceInterface() {
        // Return compatibility version of String.
        return ImsService.SERVICE_INTERFACE;
    }

    /**
     * Converts the new command to {@link MMTelFeature#turnOnIms()}.
     */
    @Override
    public final void enableIms(int slotId, int subId) {
        MmTelFeatureCompatAdapter adapter = mMmTelCompatAdapters.get(slotId);
        if (adapter == null) {
            Log.w(TAG, "enableIms: adapter null for slot :" + slotId);
            return;
        }
        try {
            adapter.enableIms();
        } catch (RemoteException e) {
            Log.w(TAG, "Couldn't enable IMS: " + e.getMessage());
        }
    }

    /**
     * Converts the new command to {@link MMTelFeature#turnOffIms()}.
     */
    @Override
    public final void disableIms(int slotId, int subId) {
        MmTelFeatureCompatAdapter adapter = mMmTelCompatAdapters.get(slotId);
        if (adapter == null) {
            Log.w(TAG, "enableIms: adapter null for slot :" + slotId);
            return;
        }
        try {
            adapter.disableIms();
        } catch (RemoteException e) {
            Log.w(TAG, "Couldn't enable IMS: " + e.getMessage());
        }
    }

    /**
     * @return the IImsRegistration that corresponds to the slot id specified.
     */
    @Override
    public final IImsRegistration getRegistration(int slotId, int subId) {
        ImsRegistrationCompatAdapter adapter = mRegCompatAdapters.get(slotId);
        if (adapter == null) {
            Log.w(TAG, "getRegistration: Registration does not exist for slot " + slotId);
            return null;
        }
        return adapter.getBinder();
    }

    /**
     * @return the IImsConfig that corresponds to the slot id specified.
     */
    @Override
    public final IImsConfig getConfig(int slotId, int subId) {
        ImsConfigCompatAdapter adapter = mConfigCompatAdapters.get(slotId);
        if (adapter == null) {
            Log.w(TAG, "getConfig: Config does not exist for slot " + slotId);
            return null;
        }
        return adapter.getIImsConfig();
    }

    /**
     * Return the SIP transport interface, which is not supported on the compat version of
     * ImsService, so return null.
     */
    @Override
    public ISipTransport getSipTransport(int slotId) {
        return null;
    }

    @Override
    protected long getStaticServiceCapabilities() {
        // Older implementations do not support optional static capabilities
        return 0L;
    }

    @Override
    protected final void notifyImsServiceReady() {
        Log.d(TAG, "notifyImsServiceReady");
        // don't do anything for compat impl.
    }

    @Override
    protected final IInterface createImsFeature(int slotId, int subId, int featureType,
            long capabilities) throws RemoteException {
        switch (featureType) {
            case ImsFeature.MMTEL: {
                return createMMTelCompat(slotId);
            }
            case ImsFeature.RCS: {

                return createRcsFeature(slotId);
            }
            default:
                return null;
        }
    }

    @Override
    protected void registerImsFeatureStatusCallback(int slotId, int featureType,
            IImsFeatureStatusCallback c) throws RemoteException {
        mServiceController.addFeatureStatusCallback(slotId, featureType, c);
    }

    @Override
    protected void unregisterImsFeatureStatusCallback(int slotId, int featureType,
            IImsFeatureStatusCallback c) {
        try {
            mServiceController.removeFeatureStatusCallback(slotId, featureType, c);
        } catch (RemoteException e) {
            Log.w(TAG, "compat - unregisterImsFeatureStatusCallback - couldn't remove " + c);
        }
    }

    @Override
    protected final void removeImsFeature(int slotId, int featureType, boolean changeSubId)
            throws RemoteException {
        if (featureType == ImsFeature.MMTEL) {
            MmTelFeatureCompatAdapter adapter = mMmTelCompatAdapters.get(slotId, null);
            // Need to manually call onFeatureRemoved here, since this is normally called by the
            // ImsService itself.
            if (adapter != null) adapter.onFeatureRemoved();
            mMmTelCompatAdapters.remove(slotId);
            mRegCompatAdapters.remove(slotId);
            mConfigCompatAdapters.remove(slotId);
        }
        if (mServiceController != null) {
            mServiceController.removeImsFeature(slotId, featureType);
        }
    }

    @Override
    protected void setServiceController(IBinder serviceController) {
        mServiceController = IImsServiceController.Stub.asInterface(serviceController);
    }

    @Override
    protected boolean isServiceControllerAvailable() {
        return mServiceController != null;
    }

    private MmTelInterfaceAdapter getInterface(int slotId)
            throws RemoteException {
        IImsMMTelFeature feature = mServiceController.createMMTelFeature(slotId);
        if (feature == null) {
            Log.w(TAG, "createMMTelCompat: createMMTelFeature returned null.");
            return null;
        }
        return new MmTelInterfaceAdapter(slotId, feature.asBinder());
    }

    private IImsMmTelFeature createMMTelCompat(int slotId)
            throws RemoteException {
        MmTelInterfaceAdapter interfaceAdapter = getInterface(slotId);
        MmTelFeatureCompatAdapter mmTelAdapter = mMmTelFeatureFactory.create(mContext, slotId,
                interfaceAdapter);
        mMmTelCompatAdapters.put(slotId, mmTelAdapter);
        ImsRegistrationCompatAdapter regAdapter = new ImsRegistrationCompatAdapter();
        mmTelAdapter.addRegistrationAdapter(regAdapter);
        mRegCompatAdapters.put(slotId, regAdapter);
        mConfigCompatAdapters.put(slotId, new ImsConfigCompatAdapter(
                mmTelAdapter.getOldConfigInterface()));
        return mmTelAdapter.getBinder();
    }

    private IImsRcsFeature createRcsFeature(int slotId) {
        // Return non-null if there is a custom RCS implementation that needs a compatability layer.
        return null;
    }
}
