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

package com.android.internal.telephony;

import android.annotation.NonNull;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.AnomalyReporter;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.IndentingPrintWriter;
import android.util.LocalLog;
import android.util.Pair;

import com.android.internal.telephony.flags.FeatureFlags;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Set;
import java.util.UUID;

import javax.sip.InvalidArgumentException;

/**
 * The DisplayInfoController updates and broadcasts all changes to {@link TelephonyDisplayInfo}.
 * It manages all the information necessary for display purposes. Clients can register for display
 * info changes via {@link #registerForTelephonyDisplayInfoChanged} and obtain the current
 * TelephonyDisplayInfo via {@link #getTelephonyDisplayInfo}.
 */
public class DisplayInfoController extends Handler {
    private final String mLogTag;
    private final LocalLog mLocalLog = new LocalLog(128);

    private static final Set<Pair<Integer, Integer>> VALID_DISPLAY_INFO_SET = Set.of(
            // LTE
            Pair.create(TelephonyManager.NETWORK_TYPE_LTE,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA),
            Pair.create(TelephonyManager.NETWORK_TYPE_LTE,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO),
            Pair.create(TelephonyManager.NETWORK_TYPE_LTE,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA),
            Pair.create(TelephonyManager.NETWORK_TYPE_LTE,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED),

            // NR
            Pair.create(TelephonyManager.NETWORK_TYPE_NR,
                    TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED)
            );

    /** Event for service state changed (roaming). */
    private static final int EVENT_SERVICE_STATE_CHANGED = 1;
    /** Event for carrier config changed. */
    private static final int EVENT_CARRIER_CONFIG_CHANGED = 2;

    @NonNull private final Phone mPhone;
    @NonNull private final NetworkTypeController mNetworkTypeController;
    @NonNull private final RegistrantList mTelephonyDisplayInfoChangedRegistrants =
            new RegistrantList();
    @NonNull private final FeatureFlags mFeatureFlags;
    @NonNull private TelephonyDisplayInfo mTelephonyDisplayInfo;
    @NonNull private ServiceState mServiceState;
    @NonNull private PersistableBundle mConfigs;

    public DisplayInfoController(@NonNull Phone phone, @NonNull FeatureFlags featureFlags) {
        mPhone = phone;
        mFeatureFlags = featureFlags;
        mLogTag = "DIC-" + mPhone.getPhoneId();
        mServiceState = mPhone.getServiceStateTracker().getServiceState();
        mConfigs = new PersistableBundle();
        CarrierConfigManager ccm = mPhone.getContext().getSystemService(CarrierConfigManager.class);
        try {
            if (ccm != null) {
                mConfigs = ccm.getConfigForSubId(mPhone.getSubId(),
                        CarrierConfigManager.KEY_SHOW_ROAMING_INDICATOR_BOOL);
            }
        } catch (Exception ignored) {
            // CarrierConfigLoader might not be available yet.
            // Once it's available, configs will be updated through the listener.
        }
        mPhone.getServiceStateTracker()
                .registerForServiceStateChanged(this, EVENT_SERVICE_STATE_CHANGED, null);
        if (ccm != null) {
            ccm.registerCarrierConfigChangeListener(Runnable::run,
                    (slotIndex, subId, carrierId, specificCarrierId) -> {
                        if (slotIndex == mPhone.getPhoneId()) {
                            obtainMessage(EVENT_CARRIER_CONFIG_CHANGED).sendToTarget();
                        }
                    });
        }
        mTelephonyDisplayInfo = new TelephonyDisplayInfo(
                TelephonyManager.NETWORK_TYPE_UNKNOWN,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE,
                false, false, false);
        mNetworkTypeController = new NetworkTypeController(phone, this, featureFlags);
        // EVENT_UPDATE will transition from DefaultState to the current state
        // and update the TelephonyDisplayInfo based on the current state.
        mNetworkTypeController.sendMessage(NetworkTypeController.EVENT_UPDATE);

        // To Support Satellite bandwidth constrained data capability status at telephony
        // display info
        log("register for satellite network callback");
        mNetworkTypeController.registerForSatelliteNetwork();
    }

    /**
     * @return the current TelephonyDisplayInfo
     */
    @NonNull public TelephonyDisplayInfo getTelephonyDisplayInfo() {
        return mTelephonyDisplayInfo;
    }

    /**
     * Update TelephonyDisplayInfo based on network type and override network type, received from
     * NetworkTypeController.
     */
    public void updateTelephonyDisplayInfo() {
        if (mNetworkTypeController != null && mServiceState != null) {
            TelephonyDisplayInfo newDisplayInfo = new TelephonyDisplayInfo(
                    mNetworkTypeController.getDataNetworkType(),
                    mNetworkTypeController.getOverrideNetworkType(),
                    isRoaming(),
                    mServiceState.isUsingNonTerrestrialNetwork(),
                    mNetworkTypeController.getSatelliteConstrainedData());
            if (!newDisplayInfo.equals(mTelephonyDisplayInfo)) {
                logl("TelephonyDisplayInfo changed from " + mTelephonyDisplayInfo + " to "
                        + newDisplayInfo);
                validateDisplayInfo(newDisplayInfo);
                mTelephonyDisplayInfo = newDisplayInfo;
                mTelephonyDisplayInfoChangedRegistrants.notifyRegistrants();
                mPhone.notifyDisplayInfoChanged(mTelephonyDisplayInfo);
            }
        } else {
            loge("Found null object");
        }
    }

    /**
     * Determine the roaming status for icon display only.
     * If this is {@code true}, the roaming indicator will be shown, and if this is {@code false},
     * the roaming indicator will not be shown.
     * To get the actual roaming status, use {@link ServiceState#getRoaming()} instead.
     *
     * @return Whether the device is considered roaming for display purposes.
     */
    private boolean isRoaming() {
        boolean roaming = mServiceState.getRoaming();
        if (roaming && !mConfigs.getBoolean(CarrierConfigManager.KEY_SHOW_ROAMING_INDICATOR_BOOL)) {
            logl("Override roaming for display due to carrier configs.");
            roaming = false;
        }
        return roaming;
    }

    /**
     * Validate the display info and trigger anomaly report if needed.
     *
     * @param displayInfo The display info to validate.
     */
    private void validateDisplayInfo(@NonNull TelephonyDisplayInfo displayInfo) {
        try {
            if (displayInfo.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE_CA) {
                throw new InvalidArgumentException("LTE_CA is not a valid network type.");
            }
            if (displayInfo.getNetworkType() < TelephonyManager.NETWORK_TYPE_UNKNOWN
                    && displayInfo.getNetworkType() > TelephonyManager.NETWORK_TYPE_NR) {
                throw new InvalidArgumentException("Invalid network type "
                        + displayInfo.getNetworkType());
            }
            if (displayInfo.getOverrideNetworkType()
                    != TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE
                    && !VALID_DISPLAY_INFO_SET.contains(Pair.create(displayInfo.getNetworkType(),
                    displayInfo.getOverrideNetworkType()))) {
                throw new InvalidArgumentException("Invalid network type override "
                        + TelephonyDisplayInfo.overrideNetworkTypeToString(
                                displayInfo.getOverrideNetworkType())
                        + " for " + TelephonyManager.getNetworkTypeName(
                                displayInfo.getNetworkType()));
            }
        } catch (InvalidArgumentException e) {
            logel(e.getMessage());
            AnomalyReporter.reportAnomaly(UUID.fromString("3aa92a2c-94ed-46a0-a744-d6b1dfec2a56"),
                    e.getMessage(), mPhone.getCarrierId());
        }
    }

    /**
     * Register for TelephonyDisplayInfo changed.
     * @param h Handler to notify
     * @param what msg.what when the message is delivered
     * @param obj msg.obj when the message is delivered
     */
    public void registerForTelephonyDisplayInfoChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mTelephonyDisplayInfoChangedRegistrants.add(r);
    }

    /**
     * Unregister for TelephonyDisplayInfo changed.
     * @param h Handler to notify
     */
    public void unregisterForTelephonyDisplayInfoChanged(Handler h) {
        mTelephonyDisplayInfoChangedRegistrants.remove(h);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case EVENT_SERVICE_STATE_CHANGED:
                mServiceState = mPhone.getServiceStateTracker().getServiceState();
                log("ServiceState updated, isRoaming=" + mServiceState.getRoaming());
                updateTelephonyDisplayInfo();
                break;
            case EVENT_CARRIER_CONFIG_CHANGED:
                mConfigs = mPhone.getContext().getSystemService(CarrierConfigManager.class)
                        .getConfigForSubId(mPhone.getSubId(),
                                CarrierConfigManager.KEY_SHOW_ROAMING_INDICATOR_BOOL);
                log("Carrier configs updated: " + mConfigs);
                updateTelephonyDisplayInfo();
                break;
        }
    }

    /**
     * Log debug messages.
     * @param s debug messages
     */
    private void log(@NonNull String s) {
        Rlog.d(mLogTag, s);
    }

    /**
     * Log error messages.
     * @param s error messages
     */
    private void loge(@NonNull String s) {
        Rlog.e(mLogTag, s);
    }

    /**
     * Log debug messages and also log into the local log.
     * @param s debug messages
     */
    private void logl(@NonNull String s) {
        log(s);
        mLocalLog.log(s);
    }

    /**
     * Log error messages and also log into the local log.
     * @param s debug messages
     */
    private void logel(@NonNull String s) {
        loge(s);
        mLocalLog.log(s);
    }

    /**
     * Dump the current state.
     */
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println("DisplayInfoController:");
        pw.println(" mPhone=" + mPhone.getPhoneName());
        pw.println(" mTelephonyDisplayInfo=" + mTelephonyDisplayInfo.toString());
        pw.flush();
        pw.println("Local logs:");
        pw.increaseIndent();
        mLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
        pw.println(" ***************************************");
        mNetworkTypeController.dump(fd, pw, args);
        pw.flush();
    }
}
