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

package com.android.internal.telephony;

import static android.telephony.TelephonyManager.HAL_SERVICE_NETWORK;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.telephony.AccessNetworkConstants;
import android.telephony.AnomalyReporter;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellInfo;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SignalStrengthUpdateRequest;
import android.telephony.SignalThresholdInfo;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.internal.telephony.util.ArrayUtils;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.PatternSyntaxException;

/**
 * SignalStrengthController handles signal polling request and unsolicited signal strength update.
 */
public class SignalStrengthController extends Handler {
    private static final boolean DBG = false; /* STOPSHIP if true */
    private static final String TAG = "SSCtr";

    private static final long SIGNAL_STRENGTH_REFRESH_THRESHOLD_IN_MS =
            TimeUnit.SECONDS.toMillis(10);
    /** Signal strength poll rate. */
    private static final long POLL_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(20);
    private static final int INVALID_ARFCN = -1;

    /** Required magnitude change between unsolicited SignalStrength reports. */
    private static final int REPORTING_HYSTERESIS_DB = 2;
    /** Minimum time between unsolicited SignalStrength reports. */
    private static final int REPORTING_HYSTERESIS_MILLIS = 3000;
    /**
     * A threshold within which (inclusive) the application requested signal strength
     * thresholds will be aligned with threholds set in advance (by system or other apps).
     * Since the alignment applies to both directions, the value is set to halt of
     * REPORTING_HYSTERESIS_DB to respect it while without introducing additional gaps for
     * thresholds set by apps.
     */
    private static final int ALIGNMENT_HYSTERESIS_DB = 1;

    private static final int EVENT_SET_SIGNAL_STRENGTH_UPDATE_REQUEST       = 1;
    private static final int EVENT_CLEAR_SIGNAL_STRENGTH_UPDATE_REQUEST     = 2;
    private static final int EVENT_ON_DEVICE_IDLE_STATE_CHANGED             = 3;
    private static final int EVENT_RIL_CONNECTED                            = 4;
    private static final int EVENT_RADIO_AVAILABLE                          = 5;
    private static final int EVENT_GET_SIGNAL_STRENGTH                      = 6;
    private static final int EVENT_POLL_SIGNAL_STRENGTH                     = 7;
    private static final int EVENT_SIGNAL_STRENGTH_UPDATE                   = 8;
    public static final int EVENT_POLL_SIGNAL_STRENGTH_DONE                = 9;
    private static final int EVENT_SERVICE_STATE_CHANGED                    = 10;

    @NonNull
    private final Phone mPhone;
    @NonNull
    private final CommandsInterface mCi;
    @NonNull
    private final RegistrantList mSignalStrengthChangedRegistrants = new RegistrantList();
    @NonNull
    private SignalStrength mSignalStrength;
    private long mSignalStrengthUpdatedTime;
    @Nullable
    private SignalStrength mLastSignalStrength = null;

    /**
     * List of LTE EARFCNs (E-UTRAN Absolute Radio Frequency Channel Number,
     * Reference: 3GPP TS 36.104 5.4.3)
     * inclusive ranges for which the lte rsrp boost is applied
     */
    @Nullable
    private ArrayList<Pair<Integer, Integer>> mEarfcnPairListForRsrpBoost = null;
    /**
     * Offset which is reduced from the rsrp threshold while calculating signal strength level.
     */
    private int mLteRsrpBoost = 0;
    /**
     * Ranges of NR ARFCNs (5G Absolute Radio Frequency Channel Number,
     * Reference: 3GPP TS 38.104)
     * inclusive ranges for which the corresponding nr rsrp boost is applied
     */
    @Nullable
    private ArrayList<Pair<Integer, Integer>> mNrarfcnRangeListForRsrpBoost = null;
    @Nullable
    private int[] mNrRsrpBoost = null;
    @NonNull
    private final Object mRsrpBoostLock = new Object();

    @NonNull
    private final List<SignalRequestRecord> mSignalRequestRecords = new ArrayList<>();

    @NonNull
    private PersistableBundle mCarrierConfig;

    @NonNull
    private final LocalLog mLocalLog = new LocalLog(64);

    private final AtomicBoolean mNTNConnected = new AtomicBoolean(false);

    public SignalStrengthController(@NonNull Phone phone) {
        mPhone = phone;
        mCi = mPhone.mCi;

        mCi.registerForRilConnected(this, EVENT_RIL_CONNECTED, null);
        mCi.registerForAvailable(this, EVENT_RADIO_AVAILABLE, null);
        mCi.setOnSignalStrengthUpdate(this, EVENT_SIGNAL_STRENGTH_UPDATE, null);
        setSignalStrengthDefaultValues();

        CarrierConfigManager ccm = mPhone.getContext().getSystemService(CarrierConfigManager.class);
        mCarrierConfig = getCarrierConfig();
        // Callback which directly handle config change should be executed on handler thread
        if (ccm != null) {
            ccm.registerCarrierConfigChangeListener(this::post,
                    (slotIndex, subId, carrierId, specificCarrierId) ->
                            onCarrierConfigurationChanged(slotIndex));
        }

        mPhone.registerForServiceStateChanged(this, EVENT_SERVICE_STATE_CHANGED, null);
    }

    @Override
    public void handleMessage(Message msg) {
        if (DBG) log("received event " + msg.what);
        AsyncResult ar;

        switch (msg.what) {
            case EVENT_RIL_CONNECTED: // fall through
            case EVENT_RADIO_AVAILABLE:
                onReset();
                break;
            case EVENT_SET_SIGNAL_STRENGTH_UPDATE_REQUEST: {
                Pair<SignalRequestRecord, Message> pair =
                        (Pair<SignalRequestRecord, Message>) msg.obj;
                SignalRequestRecord record = pair.first;
                Message onCompleted = pair.second;
                AsyncResult ret = AsyncResult.forMessage(onCompleted);

                // TODO(b/177956310): Check subId to filter out old request until a better solution
                boolean dupRequest = mSignalRequestRecords.stream().anyMatch(
                        srr -> srr.mCallingUid == record.mCallingUid
                                && srr.mSubId == record.mSubId);
                if (dupRequest) {
                    ret.exception = new IllegalStateException(
                            "setSignalStrengthUpdateRequest called again with same subId");
                    onCompleted.sendToTarget();
                    break;
                }

                try {
                    record.mRequest.getLiveToken().linkToDeath(record, 0);
                } catch (RemoteException | NullPointerException ex) {
                    ret.exception = new IllegalStateException(
                            "Signal request client is already dead.");
                    onCompleted.sendToTarget();
                    break;
                }

                mSignalRequestRecords.add(record);

                updateAlwaysReportSignalStrength();
                updateReportingCriteria();

                onCompleted.sendToTarget();
                break;
            }

            case EVENT_CLEAR_SIGNAL_STRENGTH_UPDATE_REQUEST: {
                Pair<SignalRequestRecord, Message> pair =
                        (Pair<SignalRequestRecord, Message>) msg.obj;
                SignalRequestRecord record = pair.first;
                Message onCompleted = pair.second;

                // for loop with removal may cause ConcurrentModificationException
                Iterator<SignalRequestRecord> it = mSignalRequestRecords.iterator();
                while (it.hasNext()) {
                    SignalRequestRecord srr = it.next();
                    if (srr.mRequest.getLiveToken().equals(record.mRequest.getLiveToken())) {
                        try {
                            srr.mRequest.getLiveToken().unlinkToDeath(srr, 0);
                        } catch (NoSuchElementException ignored) {
                            // Either never linked or has already unlinked, ignore anyway
                        }
                        it.remove();
                    }
                }

                updateAlwaysReportSignalStrength();
                updateReportingCriteria();

                if (onCompleted != null) {
                    AsyncResult ret = AsyncResult.forMessage(onCompleted);
                    onCompleted.sendToTarget();
                }
                break;
            }

            case EVENT_ON_DEVICE_IDLE_STATE_CHANGED: {
                updateReportingCriteria();
                break;
            }

            case EVENT_POLL_SIGNAL_STRENGTH_DONE: // fall through
            case EVENT_GET_SIGNAL_STRENGTH: {
                // This callback is called when signal strength is polled
                // all by itself

                if (!(mCi.getRadioState() == TelephonyManager.RADIO_POWER_ON)) {
                    // Polling will continue when radio turns back on
                    return;
                }
                ar = (AsyncResult) msg.obj;
                onSignalStrengthResult(ar);
                break;
            }

            case EVENT_POLL_SIGNAL_STRENGTH: {
                // Just poll signal strength...not part of pollState()

                mCi.getSignalStrength(obtainMessage(EVENT_POLL_SIGNAL_STRENGTH_DONE));
                break;
            }

            case EVENT_SIGNAL_STRENGTH_UPDATE: {
                // This is a notification from CommandsInterface.setOnSignalStrengthUpdate

                ar = (AsyncResult) msg.obj;
                onSignalStrengthResult(ar);
                break;
            }

            case EVENT_SERVICE_STATE_CHANGED: {
                onServiceStateChanged((ServiceState) ((AsyncResult) msg.obj).result);
                break;
            }

            default:
                log("Unhandled message with number: " + msg.what);
                break;
        }
    }

    void dispose() {
        mCi.unSetOnSignalStrengthUpdate(this);
    }

    /**
     * Called when RIL is connected during boot up or after modem restart. Set the default criteria
     * so that modem can start with default state before updated criteria is ready.
     */
    private void onReset() {
        setDefaultSignalStrengthReportingCriteria();
    }

    void getSignalStrengthFromCi() {
        mCi.getSignalStrength(obtainMessage(EVENT_GET_SIGNAL_STRENGTH));
    }

    /**
     * Send signal-strength-changed notification if changed. Called for both solicited and
     * unsolicited signal strength updates.
     */
    private void onSignalStrengthResult(@NonNull AsyncResult ar) {
        // This signal is used for both voice and data radio signal so parse all fields.

        SignalStrength signalStrength;
        if ((ar.exception == null) && (ar.result != null)) {
            signalStrength = (SignalStrength) ar.result;
        } else {
            loge("onSignalStrengthResult() Exception from RIL : " + ar.exception);
            signalStrength = new SignalStrength();
        }
        updateSignalStrength(signalStrength);
    }

    /**
     * Set {@code mSignalStrength} to the input argument {@code signalStrength}, update its level,
     * and send signal-strength-changed notification if changed.
     *
     * @param signalStrength The new SignalStrength used for updating {@code mSignalStrength}.
     */
    private void updateSignalStrength(@NonNull SignalStrength signalStrength) {
        mSignalStrength = maybeOverrideSignalStrengthForTest(signalStrength);
        ServiceStateTracker serviceStateTracker = mPhone.getServiceStateTracker();
        if (serviceStateTracker != null) {
            mSignalStrength.updateLevel(mCarrierConfig, serviceStateTracker.mSS);
        } else {
            loge("updateSignalStrength: serviceStateTracker is null");
        }
        mSignalStrengthUpdatedTime = System.currentTimeMillis();
        notifySignalStrength();
    }

    /**
     * For debug test build, override signal strength for testing.
     * @param original The real signal strength to use if not in testing mode.
     * @return The signal strength to broadcast to the external.
     */
    @NonNull private SignalStrength maybeOverrideSignalStrengthForTest(
            @NonNull SignalStrength original) {
        return TelephonyUtils.IS_DEBUGGABLE && mPhone.getTelephonyTester() != null
                ? Objects.requireNonNullElse(mPhone.getTelephonyTester()
                .getOverriddenSignalStrength(), original) : original;
    }

    /**
     * @return signal strength
     */
    @NonNull
    public SignalStrength getSignalStrength() {
        if (shouldRefreshSignalStrength()) {
            log("getSignalStrength() refreshing signal strength.");
            obtainMessage(EVENT_POLL_SIGNAL_STRENGTH).sendToTarget();
        }
        return mSignalStrength;
    }

    private boolean shouldRefreshSignalStrength() {
        long curTime = System.currentTimeMillis();

        // If last signal strength is older than 10 seconds, or somehow if curTime is smaller
        // than mSignalStrengthUpdatedTime (system time update), it's considered stale.
        boolean isStale = (mSignalStrengthUpdatedTime > curTime)
                || (curTime - mSignalStrengthUpdatedTime > SIGNAL_STRENGTH_REFRESH_THRESHOLD_IN_MS);
        if (!isStale) return false;

        List<SubscriptionInfo> subInfoList = SubscriptionManagerService.getInstance()
                .getActiveSubscriptionInfoList(mPhone.getContext().getOpPackageName(),
                        mPhone.getContext().getAttributionTag(), true/*isForAllProfile*/);

        if (!ArrayUtils.isEmpty(subInfoList)) {
            for (SubscriptionInfo info : subInfoList) {
                // If we have an active opportunistic subscription whose data is IN_SERVICE,
                // we need to get signal strength to decide data switching threshold. In this case,
                // we poll latest signal strength from modem.
                if (info.isOpportunistic()) {
                    TelephonyManager tm = TelephonyManager.from(mPhone.getContext())
                            .createForSubscriptionId(info.getSubscriptionId());
                    ServiceState ss = tm.getServiceState();
                    if (ss != null
                            && ss.getDataRegistrationState() == ServiceState.STATE_IN_SERVICE) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Update signal strength reporting criteria from the carrier config
     */
    @VisibleForTesting
    public void updateReportingCriteria() {
        List<SignalThresholdInfo> signalThresholdInfos = new ArrayList<>();

        int[] gsmRssiThresholds = mCarrierConfig.getIntArray(
                CarrierConfigManager.KEY_GSM_RSSI_THRESHOLDS_INT_ARRAY);
        if (gsmRssiThresholds != null) {
            signalThresholdInfos.add(
                    validateAndCreateSignalThresholdInfo(
                            SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSSI,
                            gsmRssiThresholds,
                            AccessNetworkThresholds.GERAN,
                            AccessNetworkConstants.AccessNetworkType.GERAN,
                            true));
        }

        int[] wcdmaRscpThresholds = mCarrierConfig.getIntArray(
                CarrierConfigManager.KEY_WCDMA_RSCP_THRESHOLDS_INT_ARRAY);
        if (wcdmaRscpThresholds != null) {
            signalThresholdInfos.add(
                    validateAndCreateSignalThresholdInfo(
                            SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSCP,
                            wcdmaRscpThresholds,
                            AccessNetworkThresholds.UTRAN,
                            AccessNetworkConstants.AccessNetworkType.UTRAN,
                            true));
        }

        int lteMeasurementEnabled = mCarrierConfig.getInt(isUsingNonTerrestrialNetwork()
                        ? CarrierConfigManager.KEY_PARAMETERS_USED_FOR_NTN_LTE_SIGNAL_BAR_INT
                        : CarrierConfigManager.KEY_PARAMETERS_USED_FOR_LTE_SIGNAL_BAR_INT,
                CellSignalStrengthLte.USE_RSRP);
        int[] lteRsrpThresholds = mCarrierConfig.getIntArray(isUsingNonTerrestrialNetwork()
                ? CarrierConfigManager.KEY_NTN_LTE_RSRP_THRESHOLDS_INT_ARRAY
                : CarrierConfigManager.KEY_LTE_RSRP_THRESHOLDS_INT_ARRAY);
        if (lteRsrpThresholds != null) {
            signalThresholdInfos.add(
                    validateAndCreateSignalThresholdInfo(
                            SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSRP,
                            lteRsrpThresholds,
                            AccessNetworkThresholds.EUTRAN_RSRP,
                            AccessNetworkConstants.AccessNetworkType.EUTRAN,
                            (lteMeasurementEnabled & CellSignalStrengthLte.USE_RSRP) != 0));
        }

        if (mPhone.getHalVersion(HAL_SERVICE_NETWORK).greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            int[] lteRsrqThresholds = mCarrierConfig.getIntArray(isUsingNonTerrestrialNetwork()
                    ? CarrierConfigManager.KEY_NTN_LTE_RSRQ_THRESHOLDS_INT_ARRAY :
                    CarrierConfigManager.KEY_LTE_RSRQ_THRESHOLDS_INT_ARRAY);
            if (lteRsrqThresholds != null) {
                signalThresholdInfos.add(
                        validateAndCreateSignalThresholdInfo(
                                SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSRQ,
                                lteRsrqThresholds,
                                AccessNetworkThresholds.EUTRAN_RSRQ,
                                AccessNetworkConstants.AccessNetworkType.EUTRAN,
                                (lteMeasurementEnabled & CellSignalStrengthLte.USE_RSRQ) != 0));
            }

            int[] lteRssnrThresholds = mCarrierConfig.getIntArray(isUsingNonTerrestrialNetwork()
                    ? CarrierConfigManager.KEY_NTN_LTE_RSSNR_THRESHOLDS_INT_ARRAY :
                    CarrierConfigManager.KEY_LTE_RSSNR_THRESHOLDS_INT_ARRAY);
            if (lteRssnrThresholds != null) {
                signalThresholdInfos.add(
                        validateAndCreateSignalThresholdInfo(
                                SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSSNR,
                                lteRssnrThresholds,
                                AccessNetworkThresholds.EUTRAN_RSSNR,
                                AccessNetworkConstants.AccessNetworkType.EUTRAN,
                                (lteMeasurementEnabled & CellSignalStrengthLte.USE_RSSNR) != 0));
            }

            int nrMeasurementEnabled = mCarrierConfig.getInt(CarrierConfigManager
                    .KEY_PARAMETERS_USE_FOR_5G_NR_SIGNAL_BAR_INT, CellSignalStrengthNr.USE_SSRSRP);
            int[] nrSsrsrpThresholds = mCarrierConfig.getIntArray(
                    CarrierConfigManager.KEY_5G_NR_SSRSRP_THRESHOLDS_INT_ARRAY);
            if (nrSsrsrpThresholds != null) {
                signalThresholdInfos.add(
                        validateAndCreateSignalThresholdInfo(
                                SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_SSRSRP,
                                nrSsrsrpThresholds,
                                AccessNetworkThresholds.NGRAN_SSRSRP,
                                AccessNetworkConstants.AccessNetworkType.NGRAN,
                                (nrMeasurementEnabled & CellSignalStrengthNr.USE_SSRSRP) != 0));
            }

            int[] nrSsrsrqThresholds = mCarrierConfig.getIntArray(
                    CarrierConfigManager.KEY_5G_NR_SSRSRQ_THRESHOLDS_INT_ARRAY);
            if (nrSsrsrqThresholds != null) {
                signalThresholdInfos.add(
                        validateAndCreateSignalThresholdInfo(
                                SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_SSRSRQ,
                                nrSsrsrqThresholds,
                                AccessNetworkThresholds.NGRAN_SSRSRQ,
                                AccessNetworkConstants.AccessNetworkType.NGRAN,
                                (nrMeasurementEnabled & CellSignalStrengthNr.USE_SSRSRQ) != 0));
            }

            int[] nrSssinrThresholds = mCarrierConfig.getIntArray(
                    CarrierConfigManager.KEY_5G_NR_SSSINR_THRESHOLDS_INT_ARRAY);
            if (nrSssinrThresholds != null) {
                signalThresholdInfos.add(
                        validateAndCreateSignalThresholdInfo(
                                SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_SSSINR,
                                nrSssinrThresholds,
                                AccessNetworkThresholds.NGRAN_SSSINR,
                                AccessNetworkConstants.AccessNetworkType.NGRAN,
                                (nrMeasurementEnabled & CellSignalStrengthNr.USE_SSSINR) != 0));
            }

            int[] wcdmaEcnoThresholds = mCarrierConfig.getIntArray(
                    CarrierConfigManager.KEY_WCDMA_ECNO_THRESHOLDS_INT_ARRAY);
            if (wcdmaEcnoThresholds != null) {
                signalThresholdInfos.add(
                        validateAndCreateSignalThresholdInfo(
                                SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_ECNO,
                                wcdmaEcnoThresholds,
                                AccessNetworkThresholds.UTRAN_ECNO,
                                AccessNetworkConstants.AccessNetworkType.UTRAN,
                                false));
            }

        }

        consolidatedAndSetReportingCriteria(signalThresholdInfos);
    }

    private void setDefaultSignalStrengthReportingCriteria() {
        List<SignalThresholdInfo> signalThresholdInfos = new ArrayList<>();

        signalThresholdInfos.add(
                createSignalThresholdsInfo(
                        SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSSI,
                        AccessNetworkThresholds.GERAN,
                        AccessNetworkConstants.AccessNetworkType.GERAN,
                        true));
        signalThresholdInfos.add(
                createSignalThresholdsInfo(
                        SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSCP,
                        AccessNetworkThresholds.UTRAN,
                        AccessNetworkConstants.AccessNetworkType.UTRAN,
                        true));
        signalThresholdInfos.add(
                createSignalThresholdsInfo(
                        SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSRP,
                        AccessNetworkThresholds.EUTRAN_RSRP,
                        AccessNetworkConstants.AccessNetworkType.EUTRAN,
                        true));
        signalThresholdInfos.add(
                createSignalThresholdsInfo(
                        SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSSI,
                        AccessNetworkThresholds.CDMA2000,
                        AccessNetworkConstants.AccessNetworkType.CDMA2000,
                        true));

        if (mPhone.getHalVersion(HAL_SERVICE_NETWORK).greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            signalThresholdInfos.add(
                    createSignalThresholdsInfo(
                            SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSRQ,
                            AccessNetworkThresholds.EUTRAN_RSRQ,
                            AccessNetworkConstants.AccessNetworkType.EUTRAN,
                            false));
            signalThresholdInfos.add(
                    createSignalThresholdsInfo(
                            SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSSNR,
                            AccessNetworkThresholds.EUTRAN_RSSNR,
                            AccessNetworkConstants.AccessNetworkType.EUTRAN,
                            true));

            // Defaultly we only need SSRSRP for NGRAN signal criteria reporting
            signalThresholdInfos.add(
                    createSignalThresholdsInfo(
                            SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_SSRSRP,
                            AccessNetworkThresholds.NGRAN_SSRSRP,
                            AccessNetworkConstants.AccessNetworkType.NGRAN,
                            true));
            signalThresholdInfos.add(
                    createSignalThresholdsInfo(
                            SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_SSRSRQ,
                            AccessNetworkThresholds.NGRAN_SSRSRQ,
                            AccessNetworkConstants.AccessNetworkType.NGRAN,
                            false));
            signalThresholdInfos.add(
                    createSignalThresholdsInfo(
                            SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_SSSINR,
                            AccessNetworkThresholds.NGRAN_SSSINR,
                            AccessNetworkConstants.AccessNetworkType.NGRAN,
                            false));
            signalThresholdInfos.add(
                    createSignalThresholdsInfo(
                            SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_ECNO,
                            AccessNetworkThresholds.UTRAN_ECNO,
                            AccessNetworkConstants.AccessNetworkType.UTRAN,
                            false));
        }

        consolidatedAndSetReportingCriteria(signalThresholdInfos);
    }

    private void consolidatedAndSetReportingCriteria(
            @NonNull List<SignalThresholdInfo> signalThresholdInfos) {
        List<SignalThresholdInfo> consolidatedSignalThresholdInfos = new ArrayList<>(
                signalThresholdInfos.size());
        for (SignalThresholdInfo signalThresholdInfo : signalThresholdInfos) {
            final int ran = signalThresholdInfo.getRadioAccessNetworkType();
            final int measurementType = signalThresholdInfo.getSignalMeasurementType();
            final boolean isEnabledForSystem =
                    signalThresholdInfo.isEnabled() && shouldHonorSystemThresholds();
            int[] consolidatedThresholds =
                    getConsolidatedSignalThresholds(
                            ran,
                            measurementType,
                            isEnabledForSystem
                                    ? signalThresholdInfo.getThresholds()
                                    : new int[]{},
                            ALIGNMENT_HYSTERESIS_DB);
            boolean isEnabledForAppRequest =
                    shouldEnableSignalThresholdForAppRequest(
                            ran,
                            measurementType,
                            mPhone.getSubId(),
                            mPhone.isDeviceIdle());
            int hysteresisDb = getMinimumHysteresisDb(isEnabledForAppRequest, ran, measurementType,
                    consolidatedThresholds);
            consolidatedSignalThresholdInfos.add(
                    new SignalThresholdInfo.Builder()
                            .setRadioAccessNetworkType(ran)
                            .setSignalMeasurementType(measurementType)
                            .setHysteresisMs(REPORTING_HYSTERESIS_MILLIS)
                            .setHysteresisDb(hysteresisDb)
                            .setThresholds(consolidatedThresholds, true /*isSystem*/)
                            .setIsEnabled(isEnabledForSystem || isEnabledForAppRequest)
                            .build());
        }
        mCi.setSignalStrengthReportingCriteria(consolidatedSignalThresholdInfos, null);

        localLog("setSignalStrengthReportingCriteria consolidatedSignalThresholdInfos="
                        + consolidatedSignalThresholdInfos);
    }

    /**
     * Return the minimum hysteresis dB from all available sources:
     * - system default
     * - value set by client through API
     * - threshold delta
     */
    @VisibleForTesting
    public int getMinimumHysteresisDb(boolean isEnabledForAppRequest, int ran, int measurementType,
              final int[] consolidatedThresholdList) {

        int currHysteresisDb = getHysteresisDbFromCarrierConfig(ran, measurementType);

        if (isEnabledForAppRequest) {
            // Get minimum hysteresisDb at api
            int apiHysteresisDb =
                    getHysteresisDbFromSignalThresholdInfoRequests(ran, measurementType);

            // Choose minimum of hysteresisDb between api Vs current system/cc value set
            currHysteresisDb = Math.min(currHysteresisDb, apiHysteresisDb);

            // Hal Req: choose hysteresis db value to be smaller of smallest of threshold delta
            currHysteresisDb =  computeHysteresisDbOnSmallestThresholdDelta(
                    currHysteresisDb, consolidatedThresholdList);
        }
        return currHysteresisDb;
    }

    /**
     * Get the hysteresis db value from Signal Requests
     * Note: Based on the current use case, there does not exist multile App signal threshold info
     * requests with hysteresis db value, so this logic picks the latest hysteresis db value set.
     *
     * TODO(b/262655157): Support Multiple App Hysteresis DB value customisation
     */
    private int getHysteresisDbFromSignalThresholdInfoRequests(
            @AccessNetworkConstants.RadioAccessNetworkType int ran,
            @SignalThresholdInfo.SignalMeasurementType int measurement) {
        int apiHysteresisDb = REPORTING_HYSTERESIS_DB;
        for (SignalRequestRecord record : mSignalRequestRecords) {
            for (SignalThresholdInfo info : record.mRequest.getSignalThresholdInfos()) {
                if (isRanAndSignalMeasurementTypeMatch(ran, measurement, info)) {
                    if (info.getHysteresisDb() >= 0) {
                        apiHysteresisDb = info.getHysteresisDb();
                    }
                }
            }
        }
        return apiHysteresisDb;
    }

    private int getHysteresisDbFromCarrierConfig(int ran, int measurement) {
        int configHysteresisDb = REPORTING_HYSTERESIS_DB;
        String configKey = null;

        switch (ran) {
            case AccessNetworkConstants.AccessNetworkType.GERAN:
                if (measurement == SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSSI) {
                    configKey = CarrierConfigManager.KEY_GERAN_RSSI_HYSTERESIS_DB_INT;
                }
                break;
            case AccessNetworkConstants.AccessNetworkType.UTRAN:
                if (measurement == SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSCP) {
                    configKey = CarrierConfigManager.KEY_UTRAN_RSCP_HYSTERESIS_DB_INT;
                } else if (measurement == SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_ECNO) {
                    configKey = CarrierConfigManager.KEY_UTRAN_ECNO_HYSTERESIS_DB_INT;
                }
                break;
            case AccessNetworkConstants.AccessNetworkType.EUTRAN:
                if (measurement == SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSRP) {
                    configKey = CarrierConfigManager.KEY_EUTRAN_RSRP_HYSTERESIS_DB_INT;
                } else if (measurement == SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSRQ) {
                    configKey = CarrierConfigManager.KEY_EUTRAN_RSRQ_HYSTERESIS_DB_INT;
                } else if (measurement == SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_RSSNR) {
                    configKey = CarrierConfigManager.KEY_EUTRAN_RSSNR_HYSTERESIS_DB_INT;
                }
                break;
            case AccessNetworkConstants.AccessNetworkType.NGRAN:
                if (measurement == SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_SSRSRP) {
                    configKey = CarrierConfigManager.KEY_NGRAN_SSRSRP_HYSTERESIS_DB_INT;
                } else if (measurement == SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_SSRSRQ) {
                    configKey = CarrierConfigManager.KEY_NGRAN_SSRSRQ_HYSTERESIS_DB_INT;
                } else if (measurement == SignalThresholdInfo.SIGNAL_MEASUREMENT_TYPE_SSSINR) {
                    configKey = CarrierConfigManager.KEY_NGRAN_SSSINR_HYSTERESIS_DB_INT;
                }
                break;
            default:
                localLog("No matching configuration");
        }
        if (configKey != null) {
            configHysteresisDb = mCarrierConfig.getInt(configKey, REPORTING_HYSTERESIS_DB);
        }
        return configHysteresisDb >= SignalThresholdInfo.HYSTERESIS_DB_MINIMUM
                ? configHysteresisDb : REPORTING_HYSTERESIS_DB;
    }

    /**
     * This method computes the hysteresis db value between smaller of the smallest Threshold Delta
     * and system / cc / api hysteresis db value determined.
     *
     * @param currMinHysteresisDb  smaller value between system / cc / api hysteresis db value
     * @param signalThresholdInfoArray consolidated threshold info with App request consolidated.
     * @return current minimum hysteresis db value computed between above params.
     *
     */
    private int computeHysteresisDbOnSmallestThresholdDelta(
            int currMinHysteresisDb, final int[] signalThresholdInfoArray) {
        int index = 0;
        if (signalThresholdInfoArray.length > 1) {
            while (index != signalThresholdInfoArray.length - 1) {
                if (signalThresholdInfoArray[index + 1] - signalThresholdInfoArray[index]
                        < currMinHysteresisDb) {
                    currMinHysteresisDb =
                            signalThresholdInfoArray[index + 1] - signalThresholdInfoArray[index];
                }
                index++;
            }
        }
        return currMinHysteresisDb;
    }

    void setSignalStrengthDefaultValues() {
        mSignalStrength = maybeOverrideSignalStrengthForTest(new SignalStrength());
        mSignalStrengthUpdatedTime = System.currentTimeMillis();
    }

    void notifySignalStrength() {
        if (!mSignalStrength.equals(mLastSignalStrength)) {
            try {
                mSignalStrengthChangedRegistrants.notifyRegistrants();
                mPhone.notifySignalStrength();
                mLastSignalStrength = mSignalStrength;
            } catch (NullPointerException ex) {
                loge("updateSignalStrength() Phone already destroyed: " + ex
                        + "SignalStrength not notified");
            }
        }
    }

    /**
     * Register for SignalStrength changed.
     * @param h Handler to notify
     * @param what msg.what when the message is delivered
     * @param obj msg.obj when the message is delivered
     */
    public void registerForSignalStrengthChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mSignalStrengthChangedRegistrants.add(r);
    }

    /**
     * Unregister for SignalStrength changed.
     * @param h Handler to notify
     */
    public void unregisterForSignalStrengthChanged(Handler h) {
        mSignalStrengthChangedRegistrants.remove(h);
    }

    /**
     * Print the SignalStrengthController states into the given stream.
     *
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param pw A PrintWriter to which the dump is to be set.
     * @param args Additional arguments to the dump request.
     */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("SignalStrengthController - phoneId: " + mPhone.getPhoneId());
        pw.println("SignalStrengthController - Log Begin ----");
        mLocalLog.dump(fd, pw, args);
        pw.println("SignalStrengthController - Log End ----");

        final IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        ipw.increaseIndent();
        pw.println("mSignalRequestRecords=" + mSignalRequestRecords);
        pw.println(" mLastSignalStrength=" + mLastSignalStrength);
        pw.println(" mSignalStrength=" + mSignalStrength);
        pw.println(" mLteRsrpBoost=" + mLteRsrpBoost);
        pw.println(" mNrRsrpBoost=" + Arrays.toString(mNrRsrpBoost));
        pw.println(" mEarfcnPairListForRsrpBoost=" + mEarfcnPairListForRsrpBoost);
        pw.println(" mNrarfcnRangeListForRsrpBoost=" + mNrarfcnRangeListForRsrpBoost);
        ipw.decreaseIndent();
        ipw.flush();
    }

    /**
     * Set a new request to update the signal strength thresholds.
     */
    public void setSignalStrengthUpdateRequest(int subId, int callingUid,
            @NonNull SignalStrengthUpdateRequest request, @NonNull Message onCompleted) {
        SignalRequestRecord record = new SignalRequestRecord(subId, callingUid, request);
        sendMessage(obtainMessage(EVENT_SET_SIGNAL_STRENGTH_UPDATE_REQUEST,
                new Pair<SignalRequestRecord, Message>(record, onCompleted)));

        localLog("setSignalStrengthUpdateRequest"
                + " subId=" + subId
                + " callingUid=" + callingUid
                + " request=" + request);
    }

    /**
     * Clear the previously set request.
     */
    public void clearSignalStrengthUpdateRequest(int subId, int callingUid,
            @NonNull SignalStrengthUpdateRequest request, @Nullable Message onCompleted) {
        SignalRequestRecord record = new SignalRequestRecord(subId, callingUid, request);
        sendMessage(obtainMessage(EVENT_CLEAR_SIGNAL_STRENGTH_UPDATE_REQUEST,
                new Pair<SignalRequestRecord, Message>(record, onCompleted)));

        localLog("clearSignalStrengthUpdateRequest"
                + " subId=" + subId
                + " callingUid=" + callingUid
                + " request=" + request);
    }

    /**
     * Align all the qualified thresholds set from applications to the {@code systemThresholds}
     * and consolidate a new thresholds array, follow rules below:
     * 1. All threshold values (whose interval is guaranteed to be larger than hysteresis) in
     *    {@code systemThresholds} will keep as it.
     * 2. Any threshold from apps that has interval less than hysteresis from any threshold in
     *    {@code systemThresholds} will be removed.
     * 3. The target thresholds will be {@code systemThresholds} + all qualified thresholds from
     *    apps, sorted in ascending order.
     */
    @VisibleForTesting
    @NonNull
    public int[] getConsolidatedSignalThresholds(int ran, int measurement,
            @Nullable int[] systemThresholds, int hysteresis) {

        // TreeSet with comparator that will filter element with interval less than hysteresis
        // from any current element
        Set<Integer> target = new TreeSet<>((x, y) -> {
            if (y >= x - hysteresis && y <= x + hysteresis) {
                return 0;
            }
            return Integer.compare(x, y);
        });

        if (systemThresholds != null) {
            for (int systemThreshold : systemThresholds) {
                target.add(systemThreshold);
            }
        }

        final boolean isDeviceIdle = mPhone.isDeviceIdle();
        final int curSubId = mPhone.getSubId();
        // The total number of record is small (10~15 tops). With each request has at most 5
        // SignalThresholdInfo which has at most 8 thresholds arrays. So the nested loop should
        // not be a concern here.
        for (SignalRequestRecord record : mSignalRequestRecords) {
            if (curSubId != record.mSubId
                    || (isDeviceIdle && !record.mRequest.isReportingRequestedWhileIdle())) {
                continue;
            }
            for (SignalThresholdInfo info : record.mRequest.getSignalThresholdInfos()) {
                if (isRanAndSignalMeasurementTypeMatch(ran, measurement, info)) {
                    for (int appThreshold : info.getThresholds()) {
                        target.add(appThreshold);
                    }
                }
            }
        }

        int[] targetArray = new int[target.size()];
        int i = 0;
        for (int element : target) {
            targetArray[i++] = element;
        }
        return targetArray;
    }

    /**
     * Return true if system thresholds should be honored when consolidating.
     */
    @VisibleForTesting
    public boolean shouldHonorSystemThresholds() {
        if (!mPhone.isDeviceIdle()) {
            return true;
        }

        final int curSubId = mPhone.getSubId();
        return mSignalRequestRecords.stream().anyMatch(
                srr -> curSubId == srr.mSubId
                        && srr.mRequest.isSystemThresholdReportingRequestedWhileIdle());
    }

    /**
     * Get notified when device idle state changed
     */
    @VisibleForTesting
    public void onDeviceIdleStateChanged(boolean isDeviceIdle) {
        sendMessage(obtainMessage(EVENT_ON_DEVICE_IDLE_STATE_CHANGED, isDeviceIdle));

        localLog("onDeviceIdleStateChanged isDeviceIdle=" + isDeviceIdle);
    }

    /**
     * Return true if signal threshold should be enabled due to the apps requests.
     */
    @VisibleForTesting
    public boolean shouldEnableSignalThresholdForAppRequest(
            @AccessNetworkConstants.RadioAccessNetworkType int ran,
            @SignalThresholdInfo.SignalMeasurementType int measurement,
            int subId,
            boolean isDeviceIdle) {
        for (SignalRequestRecord record : mSignalRequestRecords) {
            if (subId != record.mSubId) {
                continue;
            }
            for (SignalThresholdInfo info : record.mRequest.getSignalThresholdInfos()) {
                if (isRanAndSignalMeasurementTypeMatch(ran, measurement, info)
                        && (!isDeviceIdle || isSignalReportRequestedWhileIdle(record.mRequest))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRanAndSignalMeasurementTypeMatch(
            @AccessNetworkConstants.RadioAccessNetworkType int ran,
            @SignalThresholdInfo.SignalMeasurementType int measurement,
            @NonNull SignalThresholdInfo info) {
        return ran == info.getRadioAccessNetworkType()
                && measurement == info.getSignalMeasurementType();
    }

    private static boolean isSignalReportRequestedWhileIdle(
            @NonNull SignalStrengthUpdateRequest request) {
        return request.isSystemThresholdReportingRequestedWhileIdle()
                || request.isReportingRequestedWhileIdle();
    }

    /**
     * Gets the carrier configuration values for a particular subscription.
     *
     * @return A {@link PersistableBundle} containing the config for the given subId,
     *         or default values for an invalid subId.
     */
    @NonNull
    private PersistableBundle getCarrierConfig() {
        CarrierConfigManager configManager = (CarrierConfigManager) mPhone.getContext()
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            // If an invalid subId is used, this bundle will contain default values.
            PersistableBundle config = configManager.getConfigForSubId(mPhone.getSubId());
            if (config != null) {
                return config;
            }
        }
        // Return static default defined in CarrierConfigManager.
        return CarrierConfigManager.getDefaultConfig();
    }

    private class SignalRequestRecord implements IBinder.DeathRecipient {
        final int mSubId; // subId the request originally applied to
        final int mCallingUid;
        @NonNull
        final SignalStrengthUpdateRequest mRequest;

        SignalRequestRecord(int subId, int uid, @NonNull SignalStrengthUpdateRequest request) {
            this.mCallingUid = uid;
            this.mSubId = subId;
            this.mRequest = request;
        }

        @Override
        public void binderDied() {
            localLog("binderDied record=" + this);
            clearSignalStrengthUpdateRequest(mSubId, mCallingUid, mRequest, null /*onCompleted*/);
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("SignalRequestRecord {");
            sb.append("mSubId=").append(mSubId);
            sb.append(" mCallingUid=").append(mCallingUid);
            sb.append(" mRequest=").append(mRequest).append("}");
            return sb.toString();
        }
    }

    private void updateAlwaysReportSignalStrength() {
        final int curSubId = mPhone.getSubId();
        boolean alwaysReport = mSignalRequestRecords.stream().anyMatch(
                srr -> srr.mSubId == curSubId && isSignalReportRequestedWhileIdle(srr.mRequest));

        // TODO(b/177924721): TM#setAlwaysReportSignalStrength will be removed and we will not
        // worry about unset flag which was set by other client.
        mPhone.setAlwaysReportSignalStrength(alwaysReport);
    }

    void updateArfcnLists() {
        synchronized (mRsrpBoostLock) {
            mLteRsrpBoost = mCarrierConfig.getInt(
                    CarrierConfigManager.KEY_LTE_EARFCNS_RSRP_BOOST_INT, 0);
            String[] earfcnsStringArrayForRsrpBoost = mCarrierConfig.getStringArray(
                    CarrierConfigManager.KEY_BOOSTED_LTE_EARFCNS_STRING_ARRAY);
            mEarfcnPairListForRsrpBoost = convertEarfcnStringArrayToPairList(
                    earfcnsStringArrayForRsrpBoost);

            mNrRsrpBoost = mCarrierConfig.getIntArray(
                    CarrierConfigManager.KEY_NRARFCNS_RSRP_BOOST_INT_ARRAY);
            String[] nrarfcnsStringArrayForRsrpBoost = mCarrierConfig.getStringArray(
                    CarrierConfigManager.KEY_BOOSTED_NRARFCNS_STRING_ARRAY);
            mNrarfcnRangeListForRsrpBoost = convertEarfcnStringArrayToPairList(
                    nrarfcnsStringArrayForRsrpBoost);

            if ((mNrRsrpBoost == null && mNrarfcnRangeListForRsrpBoost != null)
                    || (mNrRsrpBoost != null && mNrarfcnRangeListForRsrpBoost == null)
                    || (mNrRsrpBoost != null && mNrarfcnRangeListForRsrpBoost != null
                    && mNrRsrpBoost.length != mNrarfcnRangeListForRsrpBoost.size())) {
                loge("Invalid parameters for NR RSRP boost");
                mNrRsrpBoost = null;
                mNrarfcnRangeListForRsrpBoost = null;
            }
        }
    }

    // package private access from ServiceStateTracker
    // TODO(b/219572311): Maintains ArfcnRsrpBoost here only without forwarding by ServiceState
    void updateServiceStateArfcnRsrpBoost(@NonNull ServiceState serviceState,
            @Nullable CellIdentity cellIdentity) {
        if (cellIdentity == null) return;

        int rsrpBoost = 0;
        int arfcn;

        synchronized (mRsrpBoostLock) {
            switch (cellIdentity.getType()) {
                case CellInfo.TYPE_LTE:
                    arfcn = ((CellIdentityLte) cellIdentity).getEarfcn();
                    if (arfcn != INVALID_ARFCN
                            && containsEarfcnInEarfcnRange(mEarfcnPairListForRsrpBoost,
                            arfcn) != -1) {
                        rsrpBoost = mLteRsrpBoost;
                    }
                    break;
                case CellInfo.TYPE_NR:
                    arfcn = ((CellIdentityNr) cellIdentity).getNrarfcn();
                    if (arfcn != INVALID_ARFCN) {
                        int index = containsEarfcnInEarfcnRange(mNrarfcnRangeListForRsrpBoost,
                                arfcn);
                        if (index != -1 && mNrRsrpBoost != null) {
                            rsrpBoost = mNrRsrpBoost[index];
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        serviceState.setArfcnRsrpBoost(rsrpBoost);
    }

    /**
     * Checks if the provided earfcn falls within the range of earfcns.
     *
     * return int index in earfcnPairList if earfcn falls within the provided range; -1 otherwise.
     */
    private static int containsEarfcnInEarfcnRange(
            @Nullable ArrayList<Pair<Integer, Integer>> earfcnPairList, int earfcn) {
        int index = 0;
        if (earfcnPairList != null) {
            for (Pair<Integer, Integer> earfcnPair : earfcnPairList) {
                if ((earfcn >= earfcnPair.first) && (earfcn <= earfcnPair.second)) {
                    return index;
                }
                index++;
            }
        }

        return -1;
    }

    /**
     * Convert the earfcnStringArray to list of pairs.
     *
     * Format of the earfcnsList is expected to be {"erafcn1_start-earfcn1_end",
     * "earfcn2_start-earfcn2_end" ... }
     */
    @Nullable
    private static ArrayList<Pair<Integer, Integer>> convertEarfcnStringArrayToPairList(
            @Nullable String[] earfcnsList) {
        ArrayList<Pair<Integer, Integer>> earfcnPairList = new ArrayList<Pair<Integer, Integer>>();

        if (earfcnsList != null) {
            int earfcnStart;
            int earfcnEnd;
            for (int i = 0; i < earfcnsList.length; i++) {
                try {
                    String[] earfcns = earfcnsList[i].split("-");
                    if (earfcns.length != 2) {
                        if (DBG) {
                            log("Invalid earfcn range format");
                        }
                        return null;
                    }

                    earfcnStart = Integer.parseInt(earfcns[0]);
                    earfcnEnd = Integer.parseInt(earfcns[1]);

                    if (earfcnStart > earfcnEnd) {
                        if (DBG) {
                            log("Invalid earfcn range format");
                        }
                        return null;
                    }

                    earfcnPairList.add(new Pair<Integer, Integer>(earfcnStart, earfcnEnd));
                } catch (PatternSyntaxException pse) {
                    if (DBG) {
                        log("Invalid earfcn range format");
                    }
                    return null;
                } catch (NumberFormatException nfe) {
                    if (DBG) {
                        log("Invalid earfcn number format");
                    }
                    return null;
                }
            }
        }

        return earfcnPairList;
    }

    private void onCarrierConfigurationChanged(int slotIndex) {
        if (slotIndex != mPhone.getPhoneId()) return;

        mCarrierConfig = getCarrierConfig();
        log("Carrier Config changed.");

        updateArfcnLists();
        updateReportingCriteria();
        updateSignalStrength(new SignalStrength(mSignalStrength));
    }

    private static SignalThresholdInfo createSignalThresholdsInfo(
            int measurementType, @NonNull int[] thresholds, int ran, boolean isEnabled) {
        return new SignalThresholdInfo.Builder()
                .setSignalMeasurementType(measurementType)
                .setThresholds(thresholds)
                .setRadioAccessNetworkType(ran)
                .setIsEnabled(isEnabled)
                .build();
    }

    /**
     * Validate the provided signal {@code thresholds} info and fall back to use the
     * {@code defaultThresholds} and report anomaly if invalid to prevent crashing Phone.
     */
    private static SignalThresholdInfo validateAndCreateSignalThresholdInfo(
            int measurementType, @NonNull int[] thresholds, @NonNull int[] defaultThresholds,
            int ran, boolean isEnabled) {
        SignalThresholdInfo signalThresholdInfo;
        try {
            signalThresholdInfo = new SignalThresholdInfo.Builder()
                    .setSignalMeasurementType(measurementType)
                    .setThresholds(thresholds)
                    .setRadioAccessNetworkType(ran)
                    .setIsEnabled(isEnabled)
                    .build();
        // TODO(b/295236831): only catch IAE when phone global exception handler is introduced.
        // Although SignalThresholdInfo only throws IAE for invalid carrier configs, we catch
        // all exception to prevent crashing phone before global exception handler is available.
        } catch (Exception e) {
            signalThresholdInfo = new SignalThresholdInfo.Builder()
                    .setSignalMeasurementType(measurementType)
                    .setThresholds(defaultThresholds)
                    .setRadioAccessNetworkType(ran)
                    .setIsEnabled(isEnabled)
                    .build();

            AnomalyReporter.reportAnomaly(
                    UUID.fromString("28232bc4-78ff-447e-b597-7c054c802407"),
                    "Invalid parameter to generate SignalThresholdInfo: "
                            + "measurementType=" + measurementType
                            + ", thresholds=" + Arrays.toString(thresholds)
                            + ", RAN=" + ran
                            + ", isEnabled=" + isEnabled
                            + ". Replaced with default thresholds: " + Arrays.toString(
                            defaultThresholds));
        }
        return signalThresholdInfo;
    }

    /**
     * dBm thresholds that correspond to changes in signal strength indications.
     */
    private static final class AccessNetworkThresholds {

        /**
         * List of dBm thresholds for GERAN {@link AccessNetworkConstants.AccessNetworkType}.
         *
         * Calculated from GSM asu level thresholds - TS 27.007 Sec 8.5
         */
        public static final int[] GERAN = new int[]{
                -109,
                -103,
                -97,
                -89,
        };

        /**
         * List of default dBm thresholds for UTRAN
         * {@link AccessNetworkConstants.AccessNetworkType}.
         *
         * These thresholds are taken from the WCDMA RSCP defaults in {@link CarrierConfigManager}.
         * See TS 27.007 Sec 8.69.
         */
        public static final int[] UTRAN = new int[]{
                -114, /* SIGNAL_STRENGTH_POOR */
                -104, /* SIGNAL_STRENGTH_MODERATE */
                -94,  /* SIGNAL_STRENGTH_GOOD */
                -84   /* SIGNAL_STRENGTH_GREAT */
        };

        /**
         * List of default dBm RSRP thresholds for EUTRAN
         * {@link AccessNetworkConstants.AccessNetworkType}.
         *
         * These thresholds are taken from the LTE RSRP defaults in {@link CarrierConfigManager}.
         */
        public static final int[] EUTRAN_RSRP = new int[]{
                -128, /* SIGNAL_STRENGTH_POOR */
                -118, /* SIGNAL_STRENGTH_MODERATE */
                -108, /* SIGNAL_STRENGTH_GOOD */
                -98,  /* SIGNAL_STRENGTH_GREAT */
        };

        /**
         * List of default dB RSRQ thresholds for EUTRAN
         * {@link AccessNetworkConstants.AccessNetworkType}.
         *
         * These thresholds are taken from the LTE RSRQ defaults in {@link CarrierConfigManager}.
         */
        public static final int[] EUTRAN_RSRQ = new int[]{
                -20,  /* SIGNAL_STRENGTH_POOR */
                -17,  /* SIGNAL_STRENGTH_MODERATE */
                -14,  /* SIGNAL_STRENGTH_GOOD */
                -11   /* SIGNAL_STRENGTH_GREAT */
        };

        /**
         * List of default dB RSSNR thresholds for EUTRAN
         * {@link AccessNetworkConstants.AccessNetworkType}.
         *
         * These thresholds are taken from the LTE RSSNR defaults in {@link CarrierConfigManager}.
         */
        public static final int[] EUTRAN_RSSNR = new int[]{
                -3,  /* SIGNAL_STRENGTH_POOR */
                1,   /* SIGNAL_STRENGTH_MODERATE */
                5,   /* SIGNAL_STRENGTH_GOOD */
                13   /* SIGNAL_STRENGTH_GREAT */
        };

        /**
         * List of dBm thresholds for CDMA2000 {@link AccessNetworkConstants.AccessNetworkType}.
         *
         * These correspond to EVDO level thresholds.
         */
        public static final int[] CDMA2000 = new int[]{
                -105,
                -90,
                -75,
                -65
        };

        /**
         * List of dB thresholds for NGRAN {@link AccessNetworkConstants.AccessNetworkType} SSRSRP
         */
        public static final int[] NGRAN_SSRSRP = new int[]{
                -110, /* SIGNAL_STRENGTH_POOR */
                -90, /* SIGNAL_STRENGTH_MODERATE */
                -80, /* SIGNAL_STRENGTH_GOOD */
                -65,  /* SIGNAL_STRENGTH_GREAT */
        };

        /**
         * List of dB thresholds for NGRAN {@link AccessNetworkConstants.AccessNetworkType} SSRSRQ
         */
        public static final int[] NGRAN_SSRSRQ = new int[]{
                -31, /* SIGNAL_STRENGTH_POOR */
                -19, /* SIGNAL_STRENGTH_MODERATE */
                -7, /* SIGNAL_STRENGTH_GOOD */
                6  /* SIGNAL_STRENGTH_GREAT */
        };

        /**
         * List of dB thresholds for NGRAN {@link AccessNetworkConstants.AccessNetworkType} SSSINR
         */
        public static final int[] NGRAN_SSSINR = new int[]{
                -5, /* SIGNAL_STRENGTH_POOR */
                5, /* SIGNAL_STRENGTH_MODERATE */
                15, /* SIGNAL_STRENGTH_GOOD */
                30  /* SIGNAL_STRENGTH_GREAT */
        };

        /**
         * List of dBm thresholds for UTRAN {@link AccessNetworkConstants.AccessNetworkType} ECNO
         */
        public static final int[] UTRAN_ECNO = new int[]{
                -24, /* SIGNAL_STRENGTH_POOR */
                -14, /* SIGNAL_STRENGTH_MODERATE */
                -6, /* SIGNAL_STRENGTH_GOOD */
                1  /* SIGNAL_STRENGTH_GREAT */
        };
    }

    private void onServiceStateChanged(ServiceState state) {
        if (state.getState() != ServiceState.STATE_IN_SERVICE) {
            return;
        }

        if (mNTNConnected.get() != state.isUsingNonTerrestrialNetwork()) {
            log("onServiceStateChanged: update it to " + state.isUsingNonTerrestrialNetwork());
            updateReportingCriteria();
            mNTNConnected.set(state.isUsingNonTerrestrialNetwork());
        }
    }

    private boolean isUsingNonTerrestrialNetwork() {
        if (mPhone.getServiceState() == null) {
            return false;
        }
        return mPhone.getServiceState().isUsingNonTerrestrialNetwork();
    }

    private static void log(String msg) {
        if (DBG) Rlog.d(TAG, msg);
    }

    private static void loge(String msg) {
        Rlog.e(TAG, msg);
    }

    /** Print to both Radio log and LocalLog, used only for critical but non-sensitive msg. */
    private void localLog(String msg) {
        Rlog.d(TAG, msg);
        mLocalLog.log(TAG + ": " + msg);
    }
}
