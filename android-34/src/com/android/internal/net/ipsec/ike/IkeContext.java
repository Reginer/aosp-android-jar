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
package com.android.internal.net.ipsec.ike;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Looper;

import com.android.internal.net.eap.EapAuthenticator;
import com.android.internal.net.ipsec.ike.utils.RandomnessFactory;
import com.android.internal.net.utils.IkeDeviceConfigUtils;

/** IkeContext contains all context information of an IKE Session */
public class IkeContext implements EapAuthenticator.EapContext {
    private static final String NAMESPACE_IPSEC = "ipsec";

    public static final String CONFIG_AUTO_ADDRESS_FAMILY_SELECTION_CELLULAR_PREFER_IPV4 =
            "config_auto_address_family_selection_cellular_prefer_ipv4";
    public static final String CONFIG_AUTO_NATT_KEEPALIVES_CELLULAR_TIMEOUT_OVERRIDE_SECONDS =
            "config_auto_natt_keepalives_cellular_timeout_override_seconds";

    private final Looper mLooper;
    private final Context mContext;
    private final RandomnessFactory mRandomFactory;

    /** Constructor for IkeContext */
    public IkeContext(Looper looper, Context context, RandomnessFactory randomFactory) {
        mLooper = looper;
        mContext = context;
        mRandomFactory = randomFactory;
    }

    /** Gets the Looper */
    @Override
    public Looper getLooper() {
        return mLooper;
    }

    /** Gets the Context */
    @Override
    public Context getContext() {
        return mContext;
    }

    /** Gets the RandomnessFactory which will control if the IKE Session is in test mode */
    @Override
    public RandomnessFactory getRandomnessFactory() {
        return mRandomFactory;
    }

    /** Looks up the value of an integer property for IPsec module from DeviceConfig */
    public int getDeviceConfigPropertyInt(
            String name, int minimumValue, int maximumValue, int defaultValue) {
        if (!hasReadDeviceConfigPermission()) {
            return defaultValue;
        }

        return IkeDeviceConfigUtils.getDeviceConfigPropertyInt(
                NAMESPACE_IPSEC, name, minimumValue, maximumValue, defaultValue);
    }

    /** Looks up the value of a boolean property for IPsec module from DeviceConfig */
    public boolean getDeviceConfigPropertyBoolean(String name, boolean defaultValue) {
        if (!hasReadDeviceConfigPermission()) {
            return defaultValue;
        }
        return IkeDeviceConfigUtils.getDeviceConfigPropertyBoolean(
                NAMESPACE_IPSEC, name, defaultValue);
    }

    private boolean hasReadDeviceConfigPermission() {
        return mContext.checkSelfPermission(android.Manifest.permission.READ_DEVICE_CONFIG)
                == PackageManager.PERMISSION_GRANTED;
    }
}
