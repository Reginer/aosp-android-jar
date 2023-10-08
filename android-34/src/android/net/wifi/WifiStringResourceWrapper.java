/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.net.wifi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Provide a wrapper to reading string overlay resources for WiFi.
 *
 * Specifically intended to provide a mechanism to store and read carrier-specific translatable
 * string overlays. Carrier-specific (MVNO) overlays are not supported - but Carrier Configurations
 * which do support MVNOs do not support translatable strings.
 *
 * Structure:
 * <string name="wifi_eap_error_message_code_32760">EAP authentication error 32760</string>
 * <string-array name=”wifi_eap_error_message_code_32760_carrier_overrides”>
 * <item><xliff:g id="carrier_id_prefix">:::1234:::</xliff:g>EAP error 32760 for carrier 1234</item>
 * <item><xliff:g id="carrier_id_prefix">:::5678:::</xliff:g>EAP error 32760 for carrier 5678</item>
 * …
 * </string-array>
 *
 * The WiFi-stack specific solution is to store the strings in the general name-space with a known
 * prefix.
 *
 * @hide
 */
public class WifiStringResourceWrapper {
    private static final String TAG = "WifiStringResourceWrapper";

    private final WifiContext mContext;
    private final int mSubId;
    private final int mCarrierId;

    private final Resources mResources;
    private final String mCarrierIdPrefix;

    @VisibleForTesting
    static final String CARRIER_ID_RESOURCE_NAME_SUFFIX = "_carrier_overrides";

    @VisibleForTesting
    static final String CARRIER_ID_RESOURCE_SEPARATOR = ":::";

    /**
     * @param context a WifiContext
     * @param subId   the sub ID to use for all the resources (overlays or carrier ID)
     */
    public WifiStringResourceWrapper(WifiContext context, int subId, int carrierId) {
        mContext = context;
        mSubId = subId;
        mCarrierId = carrierId;

        mResources = getResourcesForSubId();
        mCarrierIdPrefix =
                CARRIER_ID_RESOURCE_SEPARATOR + mCarrierId + CARRIER_ID_RESOURCE_SEPARATOR;
    }

    /**
     * Returns the string corresponding to the resource ID - or null if no resources exist.
     */
    public String getString(String name, Object... args) {
        if (mResources == null) return null;
        int resourceId = mResources.getIdentifier(name, "string",
                mContext.getWifiOverlayApkPkgName());
        if (resourceId == 0) return null;

        // check if there's a carrier-specific override array
        if (mCarrierId != TelephonyManager.UNKNOWN_CARRIER_ID) {
            String carrierOverrideString = getCarrierOverrideString(name, args);
            if (carrierOverrideString != null) {
                return carrierOverrideString;
            }
        }

        try {
            return mResources.getString(resourceId, args);
        } catch (java.util.IllegalFormatException e) {
            Log.e(TAG, "Resource formatting error - '" + name + "' - " + e);
            return null;
        }
    }

    /**
     * Returns the int corresponding to the resource ID - or the default value if no resources
     * exist.
     */
    public int getInt(String name, int defaultValue) {
        if (mResources == null) return defaultValue;
        int resourceId = mResources.getIdentifier(name, "integer",
                mContext.getWifiOverlayApkPkgName());
        if (resourceId == 0) return defaultValue;

        // check if there's a carrier-specific override array
        if (mCarrierId != TelephonyManager.UNKNOWN_CARRIER_ID) {
            String carrierOverrideString = getCarrierOverrideString(name);
            if (carrierOverrideString != null) {
                try {
                    return Integer.parseInt(carrierOverrideString);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse String into int. String=" + carrierOverrideString);
                }
            }
        }
        return mResources.getInteger(resourceId);
    }

    /**
     * Returns the boolean corresponding to the resource ID - or the default value if no resources
     * exist.
     */
    public boolean getBoolean(String name, boolean defaultValue) {
        if (mResources == null) return defaultValue;
        int resourceId = mResources.getIdentifier(name, "bool",
                mContext.getWifiOverlayApkPkgName());
        if (resourceId == 0) return defaultValue;

        // check if there's a carrier-specific override array
        if (mCarrierId != TelephonyManager.UNKNOWN_CARRIER_ID) {
            String carrierOverrideString = getCarrierOverrideString(name);
            if (carrierOverrideString != null) {
                try {
                    return Boolean.parseBoolean(carrierOverrideString);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse String into boolean. String="
                            + carrierOverrideString);
                }
            }
        }
        return mResources.getBoolean(resourceId);
    }

    /**
     * Return the String resource override by the carrier, or null if no override is found.
     */
    private String getCarrierOverrideString(String name, Object... args) {
        int arrayResourceId = mResources.getIdentifier(name + CARRIER_ID_RESOURCE_NAME_SUFFIX,
                "array", mContext.getWifiOverlayApkPkgName());
        if (arrayResourceId != 0) {
            String[] carrierIdOverlays = mResources.getStringArray(arrayResourceId);
            // check for the :::carrier-id::: prefix and if exists format and return it
            for (String carrierIdOverlay : carrierIdOverlays) {
                if (carrierIdOverlay.indexOf(mCarrierIdPrefix) != 0) continue;
                try {
                    return String.format(carrierIdOverlay.substring(mCarrierIdPrefix.length()),
                            args);
                } catch (java.util.IllegalFormatException e) {
                    Log.e(TAG, "Resource formatting error - '" + name + "' - " + e);
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Returns the resources from the given context for the MCC/MNC
     * associated with the subscription.
     */
    private Resources getResourcesForSubId() {
        try {
            Context resourceContext = mContext.createPackageContext(
                    mContext.getWifiOverlayApkPkgName(), 0);
            return SubscriptionManager.getResourcesForSubId(resourceContext, mSubId);
        } catch (PackageManager.NameNotFoundException ex) {
            return null;
        }
    }
}
