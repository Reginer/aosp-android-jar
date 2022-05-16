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

package com.android.ims.rcs.uce.options;

import android.annotation.NonNull;
import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.telephony.ims.aidl.IOptionsResponseCallback;
import android.telephony.ims.stub.RcsCapabilityExchangeImplBase;
import android.util.Log;

import com.android.ims.RcsFeatureManager;
import com.android.ims.rcs.uce.util.UceUtils;

import java.util.ArrayList;
import java.util.Set;

/**
 * The implementation of OptionsController.
 */
public class OptionsControllerImpl implements OptionsController {

    private static final String LOG_TAG = UceUtils.getLogPrefix() + "OptionsController";

    private final int mSubId;
    private final Context mContext;
    private volatile boolean mIsDestroyedFlag;
    private volatile RcsFeatureManager mRcsFeatureManager;

    public OptionsControllerImpl(Context context, int subId) {
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
        mRcsFeatureManager = null;
    }

    @Override
    public void onCarrierConfigChanged() {
        // Nothing required here.
    }

    public void sendCapabilitiesRequest(Uri contactUri, @NonNull Set<String> deviceFeatureTags,
            IOptionsResponseCallback c) throws RemoteException {

        if (mIsDestroyedFlag) {
            throw new RemoteException("OPTIONS controller is destroyed");
        }

        RcsFeatureManager featureManager = mRcsFeatureManager;
        if (featureManager == null) {
            Log.w(LOG_TAG, "sendCapabilitiesRequest: Service is unavailable");
            c.onCommandError(RcsCapabilityExchangeImplBase.COMMAND_CODE_SERVICE_UNAVAILABLE);
            return;
        }

        featureManager.sendOptionsCapabilityRequest(contactUri, new ArrayList<>(deviceFeatureTags),
            c);
    }
}
