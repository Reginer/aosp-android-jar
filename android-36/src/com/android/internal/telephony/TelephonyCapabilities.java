/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.annotation.NonNull;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemProperties;

import com.android.internal.telephony.flags.FeatureFlags;
import com.android.telephony.Rlog;

/**
 * Utilities that check if the phone supports specified capabilities.
 */
public class TelephonyCapabilities {
    private static final String LOG_TAG = "TelephonyCapabilities";

    /** This class is never instantiated. */
    private TelephonyCapabilities() {
    }

    /**
     * Return true if the current phone supports ECM ("Emergency Callback
     * Mode"), which is a feature where the device goes into a special
     * state for a short period of time after making an outgoing emergency
     * call.
     *
     * (On current devices, that state lasts 5 minutes.  It prevents data
     * usage by other apps, to avoid conflicts with any possible incoming
     * calls.  It also puts up a notification in the status bar, showing a
     * countdown while ECM is active, and allowing the user to exit ECM.)
     *
     * Currently this is assumed to be true for CDMA phones, and false
     * otherwise.
     */
    public static boolean supportsEcm(Phone phone) {
        Rlog.d(LOG_TAG, "supportsEcm: Phone type = " + phone.getPhoneType() +
                  " Ims Phone = " + phone.getImsPhone());
        return (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA ||
                phone.getImsPhone() != null);
    }

    /**
     * Return true if the current phone supports Over The Air Service
     * Provisioning (OTASP)
     *
     * Currently this is assumed to be true for CDMA phones, and false
     * otherwise.
     *
     * TODO: Watch out: this is also highly carrier-specific, since the
     * OTASP procedure is different from one carrier to the next, *and* the
     * different carriers may want very different onscreen UI as well.
     * The procedure may even be different for different devices with the
     * same carrier.
     *
     * So we eventually will need a much more flexible, pluggable design.
     * This method here is just a placeholder to reduce hardcoded
     * "if (CDMA)" checks sprinkled throughout the phone app.
     */
    public static boolean supportsOtasp(Phone phone) {
        return (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA);
    }

    /**
     * Return true if the current phone supports voice message count.
     * and the count is available
     * Both CDMA and GSM phones support voice message count
     */
    public static boolean supportsVoiceMessageCount(Phone phone) {
        return (phone.getVoiceMessageCount() != -1);
    }

    /**
     * Return true if this phone allows the user to select which
     * network to use.
     *
     * Currently this is assumed to be true only on GSM phones.
     *
     * TODO: Should CDMA phones allow this as well?
     */
    public static boolean supportsNetworkSelection(Phone phone) {
        return (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM);
    }

    /**
     * Returns a resource ID for a label to use when displaying the
     * "device id" of the current device.  (This is currently used as the
     * title of the "device id" dialog.)
     *
     * This is specific to the device's telephony technology: the device
     * id is called "IMEI" on GSM phones and "MEID" on CDMA phones.
     */
    public static int getDeviceIdLabel(Phone phone) {
        if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
            return com.android.internal.R.string.imei;
        } else if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            return com.android.internal.R.string.meid;
        } else {
            Rlog.w(LOG_TAG, "getDeviceIdLabel: no known label for phone "
                  + phone.getPhoneName());
            return 0;
        }
    }

    /**
     * Return true if the current phone supports the ability to explicitly
     * manage the state of a conference call (i.e. view the participants,
     * and hangup or separate individual callers.)
     *
     * The in-call screen's "Manage conference" UI is available only on
     * devices that support this feature.
     *
     * Currently this is assumed to be true on GSM phones and false otherwise.
     */
    public static boolean supportsConferenceCallManagement(Phone phone) {
        return ((phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM)
                || (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_SIP));
    }

    /**
     * Return true if the current phone supports explicit "Hold" and
     * "Unhold" actions for an active call.  (If so, the in-call UI will
     * provide onscreen "Hold" / "Unhold" buttons.)
     *
     * Currently this is assumed to be true on GSM phones and false
     * otherwise.  (In particular, CDMA has no concept of "putting a call
     * on hold.")
     */
    public static boolean supportsHoldAndUnhold(Phone phone) {
        return ((phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM)
                || (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_SIP)
                || (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_IMS));
    }

    /**
     * Return true if the current phone supports distinct "Answer & Hold"
     * and "Answer & End" behaviors in the call-waiting scenario.  If so,
     * the in-call UI may provide separate buttons or menu items for these
     * two actions.
     *
     * Currently this is assumed to be true on GSM phones and false
     * otherwise.  (In particular, CDMA has no concept of explicitly
     * managing the background call, or "putting a call on hold.")
     *
     * TODO: It might be better to expose this capability in a more
     * generic form, like maybe "supportsExplicitMultipleLineManagement()"
     * rather than focusing specifically on call-waiting behavior.
     */
    public static boolean supportsAnswerAndHold(Phone phone) {
        return ((phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM)
                || (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_SIP));
    }

    /**
     * Return true if phones with the given phone type support ADN
     * (Abbreviated Dialing Numbers).
     *
     * Currently this returns true when the phone type is GSM
     * ({@link PhoneConstants#PHONE_TYPE_GSM}).
     *
     * This is using int for an argument for letting apps outside
     * Phone process access to it, while other methods in this class is
     * using Phone object.
     *
     * TODO: Theoretically phones other than GSM may have the ADN capability.
     * Consider having better check here, or have better capability as part
     * of public API, with which the argument should be replaced with
     * something more appropriate.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static boolean supportsAdn(int phoneType) {
        return phoneType == PhoneConstants.PHONE_TYPE_GSM;
    }

    /**
     * Returns true if the device can distinguish the phone's dialing state
     * (Call.State.DIALING/ALERTING) and connected state (Call.State.ACTIVE).
     *
     * Currently this returns true for GSM phones as we cannot know when a CDMA
     * phone has transitioned from dialing/active to connected.
     */
    public static boolean canDistinguishDialingAndConnected(int phoneType) {
        return phoneType == PhoneConstants.PHONE_TYPE_GSM;
    }

    /**
     * Returns true if Calling/Data/Messaging features should be checked on this device.
     */
    public static boolean minimalTelephonyCdmCheck(@NonNull FeatureFlags featureFlags) {
        // Check SDK version of the vendor partition.
        final int vendorApiLevel = SystemProperties.getInt(
                "ro.vendor.api_level", Build.VERSION.DEVICE_INITIAL_SDK_INT);
        if (vendorApiLevel < Build.VERSION_CODES.VANILLA_ICE_CREAM) return false;

        return featureFlags.minimalTelephonyCdmCheck();
    }

    /**
     * @return true if this device supports telephony calling, false if it does not.
     */
    public static boolean supportsTelephonyCalling(@NonNull FeatureFlags featureFlags,
            Context context) {
        if (!TelephonyCapabilities.minimalTelephonyCdmCheck(featureFlags)) return true;
        return context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY_CALLING);
    }
}
