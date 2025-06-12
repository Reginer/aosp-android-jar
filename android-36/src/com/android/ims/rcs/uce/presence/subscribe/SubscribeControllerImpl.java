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

package com.android.ims.rcs.uce.presence.subscribe;

import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.telephony.ims.aidl.ISubscribeResponseCallback;
import android.telephony.ims.stub.RcsCapabilityExchangeImplBase;
import android.util.Log;

import com.android.ims.RcsFeatureManager;
import com.android.ims.rcs.uce.util.UceUtils;

import java.util.List;

/**
 * The implementation of the SubscribeController.
 */
public class SubscribeControllerImpl implements SubscribeController {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "SubscribeController";

    private final int mSubId;
    private final Context mContext;
    private volatile boolean mIsDestroyedFlag;
    private volatile RcsFeatureManager mRcsFeatureManager;

    public SubscribeControllerImpl(Context context, int subId) {
        mSubId = subId;
        mContext = context;
    }

    @Override
    public void onRcsConnected(RcsFeatureManager manager) {
        mRcsFeatureManager = manager;
    }

    @Override
    public void onRcsDisconnected() {
        mRcsFeatureManager = null;
    }

    @Override
    public void onDestroy() {
        mIsDestroyedFlag = true;
    }

    @Override
    public void onCarrierConfigChanged() {
        // Nothing Required Here.
    }

    @Override
    public void requestCapabilities(List<Uri> contactUris, ISubscribeResponseCallback c)
            throws RemoteException {

        if (mIsDestroyedFlag) {
            throw new RemoteException("Subscribe controller is destroyed");
        }

        RcsFeatureManager featureManager = mRcsFeatureManager;
        if (featureManager == null) {
            Log.w(LOG_TAG, "requestCapabilities: Service is unavailable");
            c.onCommandError(RcsCapabilityExchangeImplBase.COMMAND_CODE_SERVICE_UNAVAILABLE);
            return;
        }

        featureManager.requestCapabilities(contactUris, c);
    }
}
