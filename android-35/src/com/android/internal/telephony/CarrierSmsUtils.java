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
 * limitations under the License.
 */

package com.android.internal.telephony;

import android.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.telephony.ims.feature.ImsFeature;

import com.android.internal.telephony.ims.ImsResolver;
import com.android.telephony.Rlog;

import java.util.List;

/**
 * This is a basic utility class for common Carrier SMS Functions
 */
public class CarrierSmsUtils {
    protected static final boolean VDBG = false;
    protected static final String TAG = CarrierSmsUtils.class.getSimpleName();

    /**
     * Return the package name of the ImsService that is implementing RCS features for the device.
     * @param context calling context
     * @param phone object from telephony
     * @param intent that should match a CarrierSmsFilter
     * @return the name of the ImsService implementing RCS features on the device.
     */
    @Nullable
    public static String getImsRcsPackageForIntent(
            Context context, Phone phone, Intent intent) {

        String carrierImsPackage = getImsRcsPackage(phone);
        if (carrierImsPackage == null) {
            if (VDBG) Rlog.v(TAG, "No ImsService found implementing RCS.");
            return null;
        }

        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> receivers = packageManager.queryIntentServices(intent, 0);
        for (ResolveInfo info : receivers) {
            if (info.serviceInfo == null) {
                Rlog.e(TAG, "Can't get service information from " + info);
                continue;
            }

            if (carrierImsPackage.equals(info.serviceInfo.packageName)) {
                return carrierImsPackage;
            }
        }
        return null;
    }

    /**
     * @return the package name of the ImsService that is configured to implement RCS, or null if
     * there is none configured/available.
     */
    @Nullable
    private static String getImsRcsPackage(Phone phone) {
        ImsResolver resolver = ImsResolver.getInstance();
        if (resolver == null) {
            Rlog.i(TAG, "getImsRcsPackage: Device does not support IMS - skipping");
            return null;
        }

        final long identity = Binder.clearCallingIdentity();
        try {
            return resolver.getConfiguredImsServicePackageName(phone.getPhoneId(),
                    ImsFeature.FEATURE_RCS);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private CarrierSmsUtils() {}
}
