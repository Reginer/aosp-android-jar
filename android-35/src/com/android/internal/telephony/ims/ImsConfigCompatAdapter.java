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

import android.os.RemoteException;
import android.telephony.ims.stub.ImsConfigImplBase;
import android.util.Log;

import com.android.ims.internal.IImsConfig;

public class ImsConfigCompatAdapter extends ImsConfigImplBase {

    private static final String TAG = "ImsConfigCompatAdapter";

    private final IImsConfig mOldConfigInterface;

    // Compat constants
    public static final int UNKNOWN = -1;
    public static final int SUCCESS = 0;
    public static final int FAILED =  1;

    public ImsConfigCompatAdapter(IImsConfig config) {
        mOldConfigInterface = config;
    }

    @Override
    public int setConfig(int item, int value) {
        try {
            if (mOldConfigInterface.setProvisionedValue(item, value) == SUCCESS) {
                return CONFIG_RESULT_SUCCESS;
            }
        } catch (RemoteException e) {
            Log.w(TAG, "setConfig: item=" + item + " value=" + value + "failed: "
                    + e.getMessage());
        }
        return CONFIG_RESULT_FAILED;
    }

    @Override
    public int setConfig(int item, String value) {
        try {
            if (mOldConfigInterface.setProvisionedStringValue(item, value) == SUCCESS) {
                return CONFIG_RESULT_SUCCESS;
            }
        } catch (RemoteException e) {
            Log.w(TAG, "setConfig: item=" + item + " value=" + value + "failed: "
                    + e.getMessage());
        }
        return CONFIG_RESULT_FAILED;
    }

    @Override
    public int getConfigInt(int item) {
        try {
            int value = mOldConfigInterface.getProvisionedValue(item);
            if (value != UNKNOWN) {
                return value;
            }
        } catch (RemoteException e) {
            Log.w(TAG, "getConfigInt: item=" + item + "failed: " + e.getMessage());
        }
        return CONFIG_RESULT_UNKNOWN;
    }

    @Override
    public String getConfigString(int item) {
        try {
            return mOldConfigInterface.getProvisionedStringValue(item);
        } catch (RemoteException e) {
            Log.w(TAG, "getConfigInt: item=" + item + "failed: " + e.getMessage());
        }
        return null;
    }
}
