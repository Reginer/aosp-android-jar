/*
 * Copyright 2018 The Android Open Source Project
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

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.sysprop.TelephonyProperties;
import android.telephony.CellInfo;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.MccTable.MccMnc;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The locale tracker keeps tracking the current locale of the phone.
 */
public class LocaleTracker extends Handler {
    private static final boolean DBG = true;

    /** Event for getting cell info from the modem */
    private static final int EVENT_REQUEST_CELL_INFO = 1;

    /** Event for service state changed */
    private static final int EVENT_SERVICE_STATE_CHANGED = 2;

    /** Event for sim state changed */
    private static final int EVENT_SIM_STATE_CHANGED = 3;

    /** Event for incoming unsolicited cell info */
    private static final int EVENT_UNSOL_CELL_INFO = 4;

    /** Event for incoming cell info */
    private static final int EVENT_RESPONSE_CELL_INFO = 5;

    /** Event to fire if the operator from ServiceState is considered truly lost */
    private static final int EVENT_OPERATOR_LOST = 6;

    /** Event to override the current locale */
    private static final int EVENT_OVERRIDE_LOCALE = 7;

    /**
     * The broadcast intent action to override the current country for testing purposes
     *
     * <p> This broadcast is not effective on user build.
     *
     * <p>Example: To override the current country <code>
     * adb root
     * adb shell am broadcast -a com.android.internal.telephony.action.COUNTRY_OVERRIDE
     * --es country us </code>
     *
     * <p> To remove the override <code>
     * adb root
     * adb shell am broadcast -a com.android.internal.telephony.action.COUNTRY_OVERRIDE
     * --ez reset true</code>
     */
    private static final String ACTION_COUNTRY_OVERRIDE =
            "com.android.internal.telephony.action.COUNTRY_OVERRIDE";

    /** The extra for country override */
    private static final String EXTRA_COUNTRY = "country";

    /** The extra for country override reset */
    private static final String EXTRA_RESET = "reset";

    // Todo: Read this from Settings.
    /** The minimum delay to get cell info from the modem */
    private static final long CELL_INFO_MIN_DELAY_MS = 2 * SECOND_IN_MILLIS;

    // Todo: Read this from Settings.
    /** The maximum delay to get cell info from the modem */
    private static final long CELL_INFO_MAX_DELAY_MS = 10 * MINUTE_IN_MILLIS;

    // Todo: Read this from Settings.
    /** The delay for periodically getting cell info from the modem */
    private static final long CELL_INFO_PERIODIC_POLLING_DELAY_MS = 10 * MINUTE_IN_MILLIS;

    /**
     * The delay after the last time the device camped on a cell before declaring that the
     * ServiceState's MCC information can no longer be used (and thus kicking in the CellInfo
     * based tracking.
     */
    private static final long SERVICE_OPERATOR_LOST_DELAY_MS = 10 * MINUTE_IN_MILLIS;

    /** The maximum fail count to prevent delay time overflow */
    private static final int MAX_FAIL_COUNT = 30;

    /** The last known country iso */
    private static final String LAST_KNOWN_COUNTRY_ISO_SHARED_PREFS_KEY =
            "last_known_country_iso";

    private String mTag;

    private final Phone mPhone;

    private final NitzStateMachine mNitzStateMachine;

    /** SIM card state. Must be one of TelephonyManager.SIM_STATE_XXX */
    private int mSimState;

    /** Current serving PLMN's MCC/MNC */
    @Nullable
    private String mOperatorNumeric;

    /** Current cell tower information */
    @Nullable
    private List<CellInfo> mCellInfoList;

    /** Count of invalid cell info we've got so far. Will reset once we get a successful one */
    private int mFailCellInfoCount;

    /** The ISO-3166 two-letter code of device's current country */
    @Nullable
    private String mCurrentCountryIso;

    /** The country override for testing purposes */
    @Nullable
    private String mCountryOverride;

    /** Current service state. Must be one of ServiceState.STATE_XXX. */
    private int mLastServiceState = ServiceState.STATE_POWER_OFF;

    private boolean mIsTracking = false;

    private final LocalLog mLocalLog = new LocalLog(32, false /* useLocalTimestamps */);

    /** Broadcast receiver to get SIM card state changed event */
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED.equals(intent.getAction())) {
                int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY, 0);
                if (phoneId == mPhone.getPhoneId()) {
                    obtainMessage(EVENT_SIM_STATE_CHANGED,
                            intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE,
                                    TelephonyManager.SIM_STATE_UNKNOWN), 0).sendToTarget();
                }
            } else if (ACTION_COUNTRY_OVERRIDE.equals(intent.getAction())) {
                // note: need to set ServiceStateTracker#PROP_FORCE_ROAMING to force roaming.
                String countryOverride = intent.getStringExtra(EXTRA_COUNTRY);
                boolean reset = intent.getBooleanExtra(EXTRA_RESET, false);
                if (reset) countryOverride = null;
                log("Received country override: " + countryOverride);
                // countryOverride null to reset the override.
                obtainMessage(EVENT_OVERRIDE_LOCALE, countryOverride).sendToTarget();
            }
        }
    };

    /**
     * Message handler
     *
     * @param msg The message
     */
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_REQUEST_CELL_INFO:
                mPhone.requestCellInfoUpdate(null, obtainMessage(EVENT_RESPONSE_CELL_INFO));
                break;

            case EVENT_UNSOL_CELL_INFO:
                processCellInfo((AsyncResult) msg.obj);
                // If the unsol happened to be useful, use it; otherwise, pretend it didn't happen.
                if (mCellInfoList != null && mCellInfoList.size() > 0) requestNextCellInfo(true);
                break;

            case EVENT_RESPONSE_CELL_INFO:
                processCellInfo((AsyncResult) msg.obj);
                // If the cellInfo was non-empty then it's business as usual. Either way, this
                // cell info was requested by us, so it's our trigger to schedule another one.
                requestNextCellInfo(mCellInfoList != null && mCellInfoList.size() > 0);
                break;

            case EVENT_SERVICE_STATE_CHANGED:
                AsyncResult ar = (AsyncResult) msg.obj;
                onServiceStateChanged((ServiceState) ar.result);
                break;

            case EVENT_SIM_STATE_CHANGED:
                onSimCardStateChanged(msg.arg1);
                break;

            case EVENT_OPERATOR_LOST:
                updateOperatorNumericImmediate("");
                updateTrackingStatus();
                break;

            case EVENT_OVERRIDE_LOCALE:
                mCountryOverride = (String) msg.obj;
                updateLocale();
                break;

            default:
                throw new IllegalStateException("Unexpected message arrives. msg = " + msg.what);
        }
    }

    /**
     * Constructor
     *
     * @param phone The phone object
     * @param nitzStateMachine NITZ state machine
     * @param looper The looper message handler
     */
    public LocaleTracker(Phone phone, NitzStateMachine nitzStateMachine, Looper looper)  {
        super(looper);
        mPhone = phone;
        mNitzStateMachine = nitzStateMachine;
        mSimState = TelephonyManager.SIM_STATE_UNKNOWN;
        mTag = LocaleTracker.class.getSimpleName() + "-" + mPhone.getPhoneId();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED);
        if (TelephonyUtils.IS_DEBUGGABLE) {
            filter.addAction(ACTION_COUNTRY_OVERRIDE);
        }
        mPhone.getContext().registerReceiver(mBroadcastReceiver, filter);

        mPhone.registerForServiceStateChanged(this, EVENT_SERVICE_STATE_CHANGED, null);
        mPhone.registerForCellInfo(this, EVENT_UNSOL_CELL_INFO, null);
    }

    /**
     * Get the device's current country.
     *
     * @return The device's current country. Empty string if the information is not available.
     */
    @NonNull
    public String getCurrentCountry() {
        return (mCurrentCountryIso != null) ? mCurrentCountryIso : "";
    }

    /**
     * Get the MCC from cell tower information.
     *
     * @return MCC in string format. Null if the information is not available.
     */
    @Nullable
    private String getMccFromCellInfo() {
        String selectedMcc = null;
        if (mCellInfoList != null) {
            Map<String, Integer> mccMap = new HashMap<>();
            int maxCount = 0;
            for (CellInfo cellInfo : mCellInfoList) {
                String mcc = cellInfo.getCellIdentity().getMccString();
                if (mcc != null) {
                    int count = 1;
                    if (mccMap.containsKey(mcc)) {
                        count = mccMap.get(mcc) + 1;
                    }
                    mccMap.put(mcc, count);
                    // This is unlikely, but if MCC from cell info looks different, we choose the
                    // MCC that occurs most.
                    if (count > maxCount) {
                        maxCount = count;
                        selectedMcc = mcc;
                    }
                }
            }
        }
        return selectedMcc;
    }

    /**
     * Get the most frequent MCC + MNC combination with the specified MCC using cell tower
     * information. If no one combination is more frequent than any other an arbitrary MCC + MNC is
     * returned with the matching MCC. The MNC value returned can be null if it is not provided by
     * the cell tower information.
     *
     * @param mccToMatch the MCC to match
     * @return a matching {@link MccMnc}. Null if the information is not available.
     */
    @Nullable
    private MccMnc getMccMncFromCellInfo(@NonNull String mccToMatch) {
        MccMnc selectedMccMnc = null;
        if (mCellInfoList != null) {
            Map<MccMnc, Integer> mccMncMap = new HashMap<>();
            int maxCount = 0;
            for (CellInfo cellInfo : mCellInfoList) {
                String mcc = cellInfo.getCellIdentity().getMccString();
                if (Objects.equals(mcc, mccToMatch)) {
                    String mnc = cellInfo.getCellIdentity().getMncString();
                    MccMnc mccMnc = new MccMnc(mcc, mnc);
                    int count = 1;
                    if (mccMncMap.containsKey(mccMnc)) {
                        count = mccMncMap.get(mccMnc) + 1;
                    }
                    mccMncMap.put(mccMnc, count);
                    // We keep track of the MCC+MNC combination that occurs most frequently, if
                    // there is one. A null MNC is treated like any other distinct MCC+MNC
                    // combination.
                    if (count > maxCount) {
                        maxCount = count;
                        selectedMccMnc = mccMnc;
                    }
                }
            }
        }
        return selectedMccMnc;
    }

    /**
     * Called when SIM card state changed. Only when we absolutely know the SIM is absent, we get
     * cell info from the network. Other SIM states like NOT_READY might be just a transitioning
     * state.
     *
     * @param state SIM card state. Must be one of TelephonyManager.SIM_STATE_XXX.
     */
    private void onSimCardStateChanged(int state) {
        mSimState = state;
        updateLocale();
        updateTrackingStatus();
    }

    /**
     * Called when service state changed.
     *
     * @param serviceState Service state
     */
    private void onServiceStateChanged(ServiceState serviceState) {
        mLastServiceState = serviceState.getState();
        updateLocale();
        updateTrackingStatus();
    }

    /**
     * Update MCC/MNC from network service state.
     *
     * @param operatorNumeric MCC/MNC of the operator
     */
    public void updateOperatorNumeric(String operatorNumeric) {
        if (TextUtils.isEmpty(operatorNumeric)) {
            sendMessageDelayed(obtainMessage(EVENT_OPERATOR_LOST), SERVICE_OPERATOR_LOST_DELAY_MS);
        } else {
            removeMessages(EVENT_OPERATOR_LOST);
            updateOperatorNumericImmediate(operatorNumeric);
        }
    }

    private void updateOperatorNumericImmediate(String operatorNumeric) {
        // Check if the operator numeric changes.
        if (!operatorNumeric.equals(mOperatorNumeric)) {
            String msg = "Operator numeric changes to \"" + operatorNumeric + "\"";
            if (DBG) log(msg);
            mLocalLog.log(msg);
            mOperatorNumeric = operatorNumeric;
            updateLocale();
        }
    }

    private void processCellInfo(AsyncResult ar) {
        if (ar == null || ar.exception != null) {
            mCellInfoList = null;
            return;
        }
        List<CellInfo> cellInfoList = (List<CellInfo>) ar.result;
        String msg = "processCellInfo: cell info=" + cellInfoList;
        if (DBG) log(msg);
        mCellInfoList = cellInfoList;
        updateLocale();
    }

    private void requestNextCellInfo(boolean succeeded) {
        if (!mIsTracking) return;

        removeMessages(EVENT_REQUEST_CELL_INFO);
        if (succeeded) {
            resetCellInfoRetry();
            // Now we need to get the cell info from the modem periodically
            // even if we already got the cell info because the user can move.
            removeMessages(EVENT_UNSOL_CELL_INFO);
            removeMessages(EVENT_RESPONSE_CELL_INFO);
            sendMessageDelayed(obtainMessage(EVENT_REQUEST_CELL_INFO),
                    CELL_INFO_PERIODIC_POLLING_DELAY_MS);
        } else {
            // If we can't get a valid cell info. Try it again later.
            long delay = getCellInfoDelayTime(++mFailCellInfoCount);
            if (DBG) log("Can't get cell info. Try again in " + delay / 1000 + " secs.");
            sendMessageDelayed(obtainMessage(EVENT_REQUEST_CELL_INFO), delay);
        }
    }

    /**
     * Get the delay time to get cell info from modem. The delay time grows exponentially to prevent
     * battery draining.
     *
     * @param failCount Count of invalid cell info we've got so far.
     * @return The delay time for next get cell info
     */
    @VisibleForTesting
    public static long getCellInfoDelayTime(int failCount) {
        // Exponentially grow the delay time. Note we limit the fail count to MAX_FAIL_COUNT to
        // prevent overflow in Math.pow().
        long delay = CELL_INFO_MIN_DELAY_MS
                * (long) Math.pow(2, Math.min(failCount, MAX_FAIL_COUNT) - 1);
        return Math.min(Math.max(delay, CELL_INFO_MIN_DELAY_MS), CELL_INFO_MAX_DELAY_MS);
    }

    /**
     * Stop retrying getting cell info from the modem. It cancels any scheduled cell info retrieving
     * request.
     */
    private void resetCellInfoRetry() {
        mFailCellInfoCount = 0;
        removeMessages(EVENT_REQUEST_CELL_INFO);
    }

    private void updateTrackingStatus() {
        boolean shouldTrackLocale =
                (mSimState == TelephonyManager.SIM_STATE_ABSENT
                        || TextUtils.isEmpty(mOperatorNumeric))
                && (mLastServiceState == ServiceState.STATE_OUT_OF_SERVICE
                        || mLastServiceState == ServiceState.STATE_EMERGENCY_ONLY);
        if (shouldTrackLocale) {
            startTracking();
        } else {
            stopTracking();
        }
    }

    private void stopTracking() {
        if (!mIsTracking) return;
        mIsTracking = false;
        String msg = "Stopping LocaleTracker";
        if (DBG) log(msg);
        mLocalLog.log(msg);
        mCellInfoList = null;
        resetCellInfoRetry();
    }

    private void startTracking() {
        if (mIsTracking) return;
        String msg = "Starting LocaleTracker";
        mLocalLog.log(msg);
        if (DBG) log(msg);
        mIsTracking = true;
        sendMessage(obtainMessage(EVENT_REQUEST_CELL_INFO));
    }

    /**
     * Update the device's current locale
     */
    private synchronized void updateLocale() {
        // If MCC is available from network service state, use it first.
        String countryIso = "";
        String countryIsoDebugInfo = "empty as default";

        // For time zone detection we want the best geographical match we can get, which may differ
        // from the countryIso.
        String timeZoneCountryIso = null;
        String timeZoneCountryIsoDebugInfo = null;

        if (!TextUtils.isEmpty(mOperatorNumeric)) {
            MccMnc mccMnc = MccMnc.fromOperatorNumeric(mOperatorNumeric);
            if (mccMnc != null) {
                countryIso = MccTable.countryCodeForMcc(mccMnc.mcc);
                countryIsoDebugInfo = "OperatorNumeric(" + mOperatorNumeric
                        + "): MccTable.countryCodeForMcc(\"" + mccMnc.mcc + "\")";
                timeZoneCountryIso = MccTable.geoCountryCodeForMccMnc(mccMnc);
                timeZoneCountryIsoDebugInfo =
                        "OperatorNumeric: MccTable.geoCountryCodeForMccMnc(" + mccMnc + ")";
            } else {
                loge("updateLocale: Can't get country from operator numeric. mOperatorNumeric = "
                        + mOperatorNumeric);
            }
        }

        // If for any reason we can't get country from operator numeric, try to get it from cell
        // info.
        if (TextUtils.isEmpty(countryIso)) {
            String mcc = getMccFromCellInfo();
            if (mcc != null) {
                countryIso = MccTable.countryCodeForMcc(mcc);
                countryIsoDebugInfo = "CellInfo: MccTable.countryCodeForMcc(\"" + mcc + "\")";

                MccMnc mccMnc = getMccMncFromCellInfo(mcc);
                if (mccMnc != null) {
                    timeZoneCountryIso = MccTable.geoCountryCodeForMccMnc(mccMnc);
                    timeZoneCountryIsoDebugInfo =
                            "CellInfo: MccTable.geoCountryCodeForMccMnc(" + mccMnc + ")";
                }
            }
        }

        if (mCountryOverride != null) {
            countryIso = mCountryOverride;
            countryIsoDebugInfo = "mCountryOverride = \"" + mCountryOverride + "\"";
            timeZoneCountryIso = countryIso;
            timeZoneCountryIsoDebugInfo = countryIsoDebugInfo;
        }

        if (!mPhone.isRadioOn()) {
            countryIso = "";
            countryIsoDebugInfo = "radio off";
        }

        log("updateLocale: countryIso = " + countryIso
                + ", countryIsoDebugInfo = " + countryIsoDebugInfo);
        if (!Objects.equals(countryIso, mCurrentCountryIso)) {
            String msg = "updateLocale: Change the current country to \"" + countryIso + "\""
                    + ", countryIsoDebugInfo = " + countryIsoDebugInfo
                    + ", mCellInfoList = " + mCellInfoList;
            log(msg);
            mLocalLog.log(msg);
            mCurrentCountryIso = countryIso;

            // Update the last known country ISO
            if (!TextUtils.isEmpty(mCurrentCountryIso)) {
                updateLastKnownCountryIso(mCurrentCountryIso);
            }

            int phoneId = mPhone.getPhoneId();
            if (SubscriptionManager.isValidPhoneId(phoneId)) {
                List<String> newProp = new ArrayList<>(
                        TelephonyProperties.operator_iso_country());
                while (newProp.size() <= phoneId) newProp.add(null);
                newProp.set(phoneId, mCurrentCountryIso);
                TelephonyProperties.operator_iso_country(newProp);
            }

            Intent intent = new Intent(TelephonyManager.ACTION_NETWORK_COUNTRY_CHANGED);
            intent.putExtra(TelephonyManager.EXTRA_NETWORK_COUNTRY, countryIso);
            intent.putExtra(TelephonyManager.EXTRA_LAST_KNOWN_NETWORK_COUNTRY,
                    getLastKnownCountryIso());
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
            mPhone.getContext().sendBroadcast(intent);
        }

        // Pass the geographical country information to the telephony time zone detection code.

        boolean isTestMcc = false;
        if (!TextUtils.isEmpty(mOperatorNumeric)) {
            // For a test cell (MCC 001), the NitzStateMachine requires handleCountryDetected("") in
            // order to pass compliance tests. http://b/142840879
            if (mOperatorNumeric.startsWith("001")) {
                isTestMcc = true;
                timeZoneCountryIso = "";
                timeZoneCountryIsoDebugInfo = "Test cell: " + mOperatorNumeric;
            }
        }
        if (timeZoneCountryIso == null) {
            // After this timeZoneCountryIso may still be null.
            timeZoneCountryIso = countryIso;
            timeZoneCountryIsoDebugInfo = "Defaulted: " + countryIsoDebugInfo;
        }
        log("updateLocale: timeZoneCountryIso = " + timeZoneCountryIso
                + ", timeZoneCountryIsoDebugInfo = " + timeZoneCountryIsoDebugInfo);

        if (TextUtils.isEmpty(timeZoneCountryIso) && !isTestMcc) {
            mNitzStateMachine.handleCountryUnavailable();
        } else {
            mNitzStateMachine.handleCountryDetected(timeZoneCountryIso);
        }
    }

    /** Exposed for testing purposes */
    public boolean isTracking() {
        return mIsTracking;
    }

    private void updateLastKnownCountryIso(String countryIso) {
        if (!TextUtils.isEmpty(countryIso)) {
            final SharedPreferences prefs = mPhone.getContext().getSharedPreferences(
                    LAST_KNOWN_COUNTRY_ISO_SHARED_PREFS_KEY, Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString(LAST_KNOWN_COUNTRY_ISO_SHARED_PREFS_KEY, countryIso);
            editor.commit();
            log("update country iso in sharedPrefs " + countryIso);
        }
    }

    /**
     *  Return the last known country ISO before device is not camping on a network
     *  (e.g. Airplane Mode)
     *
     *  @return The device's last known country ISO.
     */
    @NonNull
    public String getLastKnownCountryIso() {
        final SharedPreferences prefs = mPhone.getContext().getSharedPreferences(
                LAST_KNOWN_COUNTRY_ISO_SHARED_PREFS_KEY, Context.MODE_PRIVATE);
        return prefs.getString(LAST_KNOWN_COUNTRY_ISO_SHARED_PREFS_KEY, "");
    }

    private void log(String msg) {
        Rlog.d(mTag, msg);
    }

    private void loge(String msg) {
        Rlog.e(mTag, msg);
    }

    /**
     * Print the DeviceStateMonitor into the given stream.
     *
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param pw A PrintWriter to which the dump is to be set.
     * @param args Additional arguments to the dump request.
     */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        final IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        pw.println("LocaleTracker-" + mPhone.getPhoneId() + ":");
        ipw.increaseIndent();
        ipw.println("mIsTracking = " + mIsTracking);
        ipw.println("mOperatorNumeric = " + mOperatorNumeric);
        ipw.println("mSimState = " + mSimState);
        ipw.println("mCellInfoList = " + mCellInfoList);
        ipw.println("mCurrentCountryIso = " + mCurrentCountryIso);
        ipw.println("mFailCellInfoCount = " + mFailCellInfoCount);
        ipw.println("Local logs:");
        ipw.increaseIndent();
        mLocalLog.dump(fd, ipw, args);
        ipw.decreaseIndent();
        ipw.decreaseIndent();
        ipw.flush();
    }

    /**
     *  This getter should only be used for testing purposes in classes that wish to spoof the
     *  country ISO. An example of how this can be done is in ServiceStateTracker#InSameCountry
     * @return spoofed country iso.
     */
    @VisibleForTesting
    public String getCountryOverride() {
        return mCountryOverride;
    }
}
