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

import android.annotation.Nullable;
import android.content.Context;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsService;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsRegistration;
import android.telephony.ims.aidl.ISipTransport;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import java.util.NoSuchElementException;

/**
 * Base class of MmTelFeatureConnection and RcsFeatureConnection.
 */
public abstract class FeatureConnection {
    protected static final String TAG = "FeatureConnection";

    protected static boolean sImsSupportedOnDevice = true;

    protected final int mSlotId;
    protected final int mSubId;
    protected Context mContext;
    protected IBinder mBinder;

    // We are assuming the feature is available when started.
    protected volatile boolean mIsAvailable = true;
    // ImsFeature Status from the ImsService. Cached.
    protected Integer mFeatureStateCached = null;
    protected long mFeatureCapabilities;
    private final IImsRegistration mRegistrationBinder;
    private final IImsConfig mConfigBinder;
    private final ISipTransport mSipTransportBinder;
    protected final Object mLock = new Object();

    public FeatureConnection(Context context, int slotId, int subId, IImsConfig c,
            IImsRegistration r, ISipTransport s) {
        mSlotId = slotId;
        mSubId = subId;
        mContext = context;
        mRegistrationBinder = r;
        mConfigBinder = c;
        mSipTransportBinder = s;
    }

    protected TelephonyManager getTelephonyManager() {
        return (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    /**
     * Set the binder which type is IImsMmTelFeature or IImsRcsFeature to connect to MmTelFeature
     * or RcsFeature.
     */
    public void setBinder(IBinder binder) {
        synchronized (mLock) {
            mBinder = binder;
            try {
                if (mBinder != null) {
                    mBinder.linkToDeath(mDeathRecipient, 0);
                }
            } catch (RemoteException e) {
                Log.w(TAG, "setBinder: linkToDeath on already dead Binder, setting null");
                mBinder = null;
            }
        }
    }

    protected final IBinder.DeathRecipient mDeathRecipient = () -> {
        Log.w(TAG, "DeathRecipient triggered, binder died.");
        if (mContext != null && Looper.getMainLooper() != null) {
            // Move this signal to the main thread, notifying ImsManager of the Binder
            // death on another thread may lead to deadlocks.
            mContext.getMainExecutor().execute(this::onRemovedOrDied);
            return;
        }
        // No choice - execute on the current Binder thread.
        onRemovedOrDied();
    };

    /**
     * Called when the MmTelFeature/RcsFeature has either been removed by Telephony or crashed.
     */
    protected void onRemovedOrDied() {
        synchronized (mLock) {
            if (mIsAvailable) {
                mIsAvailable = false;
                try {
                    if (mBinder != null) {
                        mBinder.unlinkToDeath(mDeathRecipient, 0);
                    }
                } catch (NoSuchElementException e) {
                    Log.w(TAG, "onRemovedOrDied: unlinkToDeath called on unlinked Binder.");
                }
            }
        }
    }

    public @ImsRegistrationImplBase.ImsRegistrationTech int getRegistrationTech()
            throws RemoteException {
        IImsRegistration registration = getRegistration();
        if (registration != null) {
            return registration.getRegistrationTechnology();
        } else {
            Log.w(TAG, "getRegistrationTech: ImsRegistration is null");
            return ImsRegistrationImplBase.REGISTRATION_TECH_NONE;
        }
    }

    public @Nullable IImsRegistration getRegistration() {
        return mRegistrationBinder;
    }

    public @Nullable IImsConfig getConfig() {
        return mConfigBinder;
    }

    public @Nullable ISipTransport getSipTransport() {
        return mSipTransportBinder;
    }

    @VisibleForTesting
    public void checkServiceIsReady() throws RemoteException {
        if (!sImsSupportedOnDevice) {
            throw new RemoteException("IMS is not supported on this device.");
        }
        if (!isBinderReady()) {
            throw new RemoteException("ImsServiceProxy is not ready to accept commands.");
        }
    }

    /**
     * @return Returns true if the ImsService is ready to take commands, false otherwise. If this
     * method returns false, it doesn't mean that the Binder connection is not available (use
     * {@link #isBinderReady()} to check that), but that the ImsService is not accepting commands
     * at this time.
     *
     * For example, for DSDS devices, only one slot can be {@link ImsFeature#STATE_READY} to take
     * commands at a time, so the other slot must stay at {@link ImsFeature#STATE_UNAVAILABLE}.
     */
    public boolean isBinderReady() {
        return isBinderAlive() && getFeatureState() == ImsFeature.STATE_READY;
    }

    /**
     * @return false if the binder connection is no longer alive.
     */
    public boolean isBinderAlive() {
        return mIsAvailable && mBinder != null && mBinder.isBinderAlive();
    }

    public void updateFeatureState(int state) {
        synchronized (mLock) {
            mFeatureStateCached = state;
        }
    }

    public long getFeatureCapabilties() {
        synchronized (mLock) {
            return mFeatureCapabilities;
        }
    }

    public void updateFeatureCapabilities(long caps) {
        synchronized (mLock) {
            if (mFeatureCapabilities != caps) {
                mFeatureCapabilities = caps;
                onFeatureCapabilitiesUpdated(caps);
            }
        }
    }

    public boolean isCapable(@ImsService.ImsServiceCapability long capabilities)
            throws RemoteException {
        if (!isBinderAlive()) {
            throw new RemoteException("isCapable: ImsService is not alive");
        }
        return (getFeatureCapabilties() & capabilities) > 0;
    }

    /**
     * @return an integer describing the current Feature Status, defined in
     * {@link ImsFeature.ImsState}.
     */
    public int getFeatureState() {
        synchronized (mLock) {
            if (isBinderAlive() && mFeatureStateCached != null) {
                return mFeatureStateCached;
            }
        }
        // Don't synchronize on Binder call.
        Integer state = retrieveFeatureState();
        synchronized (mLock) {
            if (state == null) {
                return ImsFeature.STATE_UNAVAILABLE;
            }
            // Cache only non-null value for feature status.
            mFeatureStateCached = state;
        }
        Log.i(TAG + " [" + mSlotId + "]", "getFeatureState - returning "
                + ImsFeature.STATE_LOG_MAP.get(state));
        return state;
    }

    public int getSubId() {
        return mSubId;
    }

    /**
     * Internal method used to retrieve the feature status from the corresponding ImsService.
     */
    protected abstract Integer retrieveFeatureState();

    protected abstract void onFeatureCapabilitiesUpdated(long capabilities);
}
