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

package com.android.internal.telephony.dataconnection;

import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Message;
import android.os.OutcomeReceiver;
import android.os.Registrant;
import android.os.RegistrantList;
import android.preference.PreferenceManager;
import android.telephony.AccessNetworkConstants;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityTdscdma;
import android.telephony.CellIdentityWcdma;
import android.telephony.ModemActivityInfo;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.LocalLog;
import android.util.Pair;
import android.view.Display;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyFacade;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.nano.TelephonyProto.NrMode;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Objects;

/**
 * Link Bandwidth Estimator based on the byte counts in TrafficStats and the time reported in modem
 * activity.
 */
public class LinkBandwidthEstimator extends Handler {
    private static final String TAG = LinkBandwidthEstimator.class.getSimpleName();
    private static final boolean DBG = false;
    @VisibleForTesting
    static final int MSG_SCREEN_STATE_CHANGED = 1;
    @VisibleForTesting
    static final int MSG_TRAFFIC_STATS_POLL = 2;
    @VisibleForTesting
    static final int MSG_MODEM_ACTIVITY_RETURNED = 3;
    @VisibleForTesting
    static final int MSG_DEFAULT_NETWORK_CHANGED = 4;
    @VisibleForTesting
    static final int MSG_SIGNAL_STRENGTH_CHANGED = 5;
    @VisibleForTesting
    static final int MSG_NR_FREQUENCY_CHANGED = 6;
    @VisibleForTesting
    static final int MSG_NR_STATE_CHANGED = 7;
    @VisibleForTesting
    static final int MSG_ACTIVE_PHONE_CHANGED = 8;
    @VisibleForTesting
    static final int MSG_DATA_REG_STATE_OR_RAT_CHANGED = 9;

    // TODO: move the following parameters to xml file
    private static final int TRAFFIC_STATS_POLL_INTERVAL_MS = 1_000;
    private static final int MODEM_POLL_MIN_INTERVAL_MS = 5_000;
    private static final int TRAFFIC_MODEM_POLL_BYTE_RATIO = 8;
    private static final int TRAFFIC_POLL_BYTE_THRESHOLD_MAX = 20_000;
    private static final int BYTE_DELTA_ACC_THRESHOLD_MAX_KB = 8_000;
    private static final int MODEM_POLL_TIME_DELTA_MAX_MS = 10_000;
    private static final int FILTER_UPDATE_MAX_INTERVAL_MS = 5_100;
    // BW samples with Tx or Rx time below the following value is ignored.
    private static final int TX_RX_TIME_MIN_MS = 200;
    // The large time constant used in BW filter
    private static final int TIME_CONSTANT_LARGE_SEC = 6;
    // The small time constant used in BW filter
    private static final int TIME_CONSTANT_SMALL_SEC = 6;
    // If RSSI changes by more than the below value, update BW filter with small time constant
    private static final int RSSI_DELTA_THRESHOLD_DB = 6;
    // The up-scaling factor of filter coefficient.
    private static final int FILTER_SCALE = 128;
    // Force weight to 0 if the elapsed time is above LARGE_TIME_DECAY_RATIO * time constant
    private static final int LARGE_TIME_DECAY_RATIO = 4;
    // Modem Tx time may contain Rx time as defined in HAL. To work around the issue, if Tx time
    // over Rx time ratio is above the following value, use Tx time + Rx time as Rx time.
    private static final int TX_OVER_RX_TIME_RATIO_THRESHOLD_NUM = 3;
    private static final int TX_OVER_RX_TIME_RATIO_THRESHOLD_DEN = 2;
    // Default Link bandwidth value if the RAT entry is not found in static BW table.
    private static final int DEFAULT_LINK_BAND_WIDTH_KBPS = 14;
    // If Tx or Rx link bandwidth change is above the following value, send the BW update
    private static final int BW_UPDATE_THRESHOLD_PERCENT = 15;

    // To be used in link bandwidth estimation, each TrafficStats poll sample needs to be above
    // a predefine threshold.
    // For RAT with static BW above HIGH_BANDWIDTH_THRESHOLD_KBPS, it uses the following table.
    // For others RATs, the thresholds are derived from the static BW values.
    // The following table is defined per signal level, int [NUM_SIGNAL_LEVEL].
    private static final int HIGH_BANDWIDTH_THRESHOLD_KBPS = 5000;
    //Array dimension : int [NUM_LINK_DIRECTION][NUM_SIGNAL_LEVEL]
    private static final int[][] BYTE_DELTA_THRESHOLD_KB =
            {{200, 300, 400, 600, 1000}, {400, 600, 800, 1000, 1000}};
    // Used to derive byte count threshold from avg BW
    private static final int LOW_BW_TO_AVG_BW_RATIO_NUM = 3;
    private static final int LOW_BW_TO_AVG_BW_RATIO_DEN = 8;
    private static final int MAX_BW_TO_STATIC_BW_RATIO = 15;
    private static final int BYTE_DELTA_THRESHOLD_MIN_KB = 10;
    private static final int MAX_ERROR_PERCENT = 100 * 100;
    private static final String[] AVG_BW_PER_RAT = {
            "GPRS:24,24", "EDGE:70,18", "UMTS:115,115", "CDMA:14,14",
            "CDMA - 1xRTT:30,30", "CDMA - EvDo rev. 0:750,48", "CDMA - EvDo rev. A:950,550",
            "HSDPA:4300,620", "HSUPA:4300,1800", "HSPA:4300,1800", "CDMA - EvDo rev. B:1500,550",
            "CDMA - eHRPD:750,48", "HSPA+:13000,3400", "TD_SCDMA:115,115",
            "LTE:30000,15000", "NR_NSA:47000,18000",
            "NR_NSA_MMWAVE:145000,60000", "NR:145000,60000", "NR_MMWAVE:145000,60000"};
    private static final Map<String, Pair<Integer, Integer>> AVG_BW_PER_RAT_MAP = new ArrayMap<>();
    private static final String UNKNOWN_PLMN = "";

    // To be used in the long term avg, each count needs to be above the following value
    public static final int BW_STATS_COUNT_THRESHOLD = 5;
    public static final int NUM_SIGNAL_LEVEL = 5;
    public static final int LINK_TX = 0;
    public static final int LINK_RX = 1;
    public static final int NUM_LINK_DIRECTION = 2;

    // One common timestamp for all sim to avoid frequent modem polling
    private final Phone mPhone;
    private final TelephonyFacade mTelephonyFacade;
    private final TelephonyManager mTelephonyManager;
    private final ConnectivityManager mConnectivityManager;
    private final LocalLog mLocalLog = new LocalLog(512);
    private boolean mScreenOn = false;
    private boolean mIsOnDefaultRoute = false;
    private boolean mIsOnActiveData = false;
    private long mLastModemPollTimeMs;
    private boolean mLastTrafficValid = true;
    private long mLastMobileTxBytes;
    private long mLastMobileRxBytes;
    private long mTxBytesDeltaAcc;
    private long mRxBytesDeltaAcc;

    private ModemActivityInfo mLastModemActivityInfo = null;
    private final TelephonyCallback mTelephonyCallback = new TelephonyCallbackImpl();
    private int mSignalStrengthDbm;
    private int mSignalLevel;
    private int mDataRat = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    private int mTac;
    private String mPlmn = UNKNOWN_PLMN;
    private NetworkCapabilities mNetworkCapabilities;
    private NetworkBandwidth mPlaceholderNetwork;
    private long mFilterUpdateTimeMs;

    private int mBandwidthUpdateSignalDbm = -1;
    private int mBandwidthUpdateSignalLevel = -1;
    private int mBandwidthUpdateDataRat = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    private String mBandwidthUpdatePlmn = UNKNOWN_PLMN;
    private BandwidthState mTxState = new BandwidthState(LINK_TX);
    private BandwidthState mRxState = new BandwidthState(LINK_RX);
    private RegistrantList mBandwidthChangedRegistrants = new RegistrantList();
    private long mLastPlmnOrRatChangeTimeMs;
    private long mLastDrsOrRatChangeTimeMs;

    private static void initAvgBwPerRatTable() {
        for (String config : AVG_BW_PER_RAT) {
            int rxKbps = 14;
            int txKbps = 14;
            String[] kv = config.split(":");
            if (kv.length == 2) {
                String[] split = kv[1].split(",");
                if (split.length == 2) {
                    try {
                        rxKbps = Integer.parseInt(split[0]);
                        txKbps = Integer.parseInt(split[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                AVG_BW_PER_RAT_MAP.put(kv[0], new Pair<>(rxKbps, txKbps));
            }
        }
    }

    private final DisplayManager.DisplayListener mDisplayListener =
            new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    obtainMessage(MSG_SCREEN_STATE_CHANGED, isScreenOn()).sendToTarget();
                }
            };

    private final OutcomeReceiver<ModemActivityInfo, TelephonyManager.ModemActivityInfoException>
            mOutcomeReceiver =
            new OutcomeReceiver<ModemActivityInfo, TelephonyManager.ModemActivityInfoException>() {
                @Override
                public void onResult(ModemActivityInfo result) {
                    obtainMessage(MSG_MODEM_ACTIVITY_RETURNED, result).sendToTarget();
                }

                @Override
                public void onError(TelephonyManager.ModemActivityInfoException e) {
                    Rlog.e(TAG, "error reading modem stats:" + e);
                    obtainMessage(MSG_MODEM_ACTIVITY_RETURNED, null).sendToTarget();
                }
            };

    private final ConnectivityManager.NetworkCallback mDefaultNetworkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onCapabilitiesChanged(@NonNull Network network,
                        @NonNull NetworkCapabilities networkCapabilities) {
                    obtainMessage(MSG_DEFAULT_NETWORK_CHANGED, networkCapabilities).sendToTarget();
                }

                public void onLost(@NonNull Network network) {
                    obtainMessage(MSG_DEFAULT_NETWORK_CHANGED, null).sendToTarget();
                }
            };

    public LinkBandwidthEstimator(Phone phone, TelephonyFacade telephonyFacade) {
        mPhone = phone;
        mTelephonyFacade = telephonyFacade;
        mTelephonyManager = phone.getContext()
                .getSystemService(TelephonyManager.class)
                .createForSubscriptionId(phone.getSubId());
        mConnectivityManager = phone.getContext().getSystemService(ConnectivityManager.class);
        DisplayManager dm = (DisplayManager) phone.getContext().getSystemService(
                Context.DISPLAY_SERVICE);
        dm.registerDisplayListener(mDisplayListener, null);
        handleScreenStateChanged(isScreenOn());
        mConnectivityManager.registerDefaultNetworkCallback(mDefaultNetworkCallback, this);
        mTelephonyManager.registerTelephonyCallback(new HandlerExecutor(this),
                mTelephonyCallback);
        mPlaceholderNetwork = new NetworkBandwidth(UNKNOWN_PLMN);
        initAvgBwPerRatTable();
        registerNrStateFrequencyChange();
        mPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(AccessNetworkConstants
                .TRANSPORT_TYPE_WWAN, this, MSG_DATA_REG_STATE_OR_RAT_CHANGED, null);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_SCREEN_STATE_CHANGED:
                handleScreenStateChanged((boolean) msg.obj);
                break;
            case MSG_TRAFFIC_STATS_POLL:
                handleTrafficStatsPoll();
                break;
            case MSG_MODEM_ACTIVITY_RETURNED:
                handleModemActivityReturned((ModemActivityInfo) msg.obj);
                break;
            case MSG_DEFAULT_NETWORK_CHANGED:
                handleDefaultNetworkChanged((NetworkCapabilities) msg.obj);
                break;
            case MSG_SIGNAL_STRENGTH_CHANGED:
                handleSignalStrengthChanged((SignalStrength) msg.obj);
                break;
            case MSG_NR_FREQUENCY_CHANGED:
                // fall through
            case MSG_NR_STATE_CHANGED:
                updateStaticBwValueResetFilter();
                break;
            case MSG_ACTIVE_PHONE_CHANGED:
                handleActivePhoneChanged((int) msg.obj);
                break;
            case MSG_DATA_REG_STATE_OR_RAT_CHANGED:
                handleDrsOrRatChanged((AsyncResult) msg.obj);
                break;
            default:
                Rlog.e(TAG, "invalid message " + msg.what);
                break;
        }
    }

    /**
     * Registers for bandwidth estimation change. The bandwidth will be returned
     *      * {@link AsyncResult#result} as a {@link Pair} Object.
     *      * The {@link AsyncResult} will be in the notification {@link Message#obj}.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForBandwidthChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        mBandwidthChangedRegistrants.add(r);
    }

    /**
     * Unregisters for bandwidth estimation change.
     * @param h handler to notify
     */
    public void unregisterForBandwidthChanged(Handler h) {
        mBandwidthChangedRegistrants.remove(h);
    }
    /**
     * @return True if one the device's screen (e.g. main screen, wifi display, HDMI display etc...)
     * is on.
     */
    private boolean isScreenOn() {
        // Note that we don't listen to Intent.SCREEN_ON and Intent.SCREEN_OFF because they are no
        // longer adequate for monitoring the screen state since they are not sent in cases where
        // the screen is turned off transiently such as due to the proximity sensor.
        final DisplayManager dm = (DisplayManager) mPhone.getContext().getSystemService(
                Context.DISPLAY_SERVICE);
        Display[] displays = dm.getDisplays();

        if (displays != null) {
            for (Display display : displays) {
                // Anything other than STATE_ON is treated as screen off, such as STATE_DOZE,
                // STATE_DOZE_SUSPEND, etc...
                if (display.getState() == Display.STATE_ON) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    private void handleScreenStateChanged(boolean screenOn) {
        if (mScreenOn == screenOn) {
            return;
        }
        mScreenOn = screenOn;
        handleTrafficStatsPollConditionChanged();
    }

    private void handleDefaultNetworkChanged(NetworkCapabilities networkCapabilities) {
        mNetworkCapabilities = networkCapabilities;
        boolean isOnDefaultRoute;
        if (networkCapabilities == null) {
            isOnDefaultRoute = false;
        } else {
            isOnDefaultRoute = networkCapabilities.hasTransport(TRANSPORT_CELLULAR);
        }
        if (mIsOnDefaultRoute == isOnDefaultRoute) {
            return;
        }
        mIsOnDefaultRoute = isOnDefaultRoute;
        handleTrafficStatsPollConditionChanged();
    }

    private void handleActivePhoneChanged(int activeDataSubId) {
        boolean isOnActiveData = activeDataSubId == mPhone.getSubId();
        if (mIsOnActiveData == isOnActiveData) {
            return;
        }
        mIsOnActiveData = isOnActiveData;
        logd("mIsOnActiveData " + mIsOnActiveData + " activeDataSubId " + activeDataSubId);
        handleTrafficStatsPollConditionChanged();
    }

    private void handleDrsOrRatChanged(AsyncResult ar) {
        Pair<Integer, Integer> drsRatPair = (Pair<Integer, Integer>) ar.result;
        logd("DrsOrRatChanged dataRegState " + drsRatPair.first + " rilRat " + drsRatPair.second);
        mLastDrsOrRatChangeTimeMs = mTelephonyFacade.getElapsedSinceBootMillis();
    }

    private void handleTrafficStatsPollConditionChanged() {
        removeMessages(MSG_TRAFFIC_STATS_POLL);
        if (mScreenOn && mIsOnDefaultRoute && mIsOnActiveData) {
            updateDataRatCellIdentityBandwidth();
            handleTrafficStatsPoll();
        }
    }

    private void handleTrafficStatsPoll() {
        invalidateTxRxSamples();
        long mobileTxBytes = mTelephonyFacade.getMobileTxBytes();
        long mobileRxBytes = mTelephonyFacade.getMobileRxBytes();
        long txBytesDelta = mobileTxBytes - mLastMobileTxBytes;
        long rxBytesDelta = mobileRxBytes - mLastMobileRxBytes;

        // Schedule the next traffic stats poll
        sendEmptyMessageDelayed(MSG_TRAFFIC_STATS_POLL, TRAFFIC_STATS_POLL_INTERVAL_MS);

        mLastMobileTxBytes = mobileTxBytes;
        mLastMobileRxBytes = mobileRxBytes;
        // Sometimes TrafficStats byte counts return invalid values
        // Ignore two polls if it happens
        boolean trafficValid = txBytesDelta >= 0 && rxBytesDelta >= 0;
        if (!mLastTrafficValid || !trafficValid) {
            mLastTrafficValid = trafficValid;
            Rlog.e(TAG, " run into invalid traffic count");
            return;
        }

        mTxBytesDeltaAcc += txBytesDelta;
        mRxBytesDeltaAcc += rxBytesDelta;

        boolean doModemPoll = true;
        // Check if it meets the requirement to request modem activity
        long txByteDeltaThr = Math.min(mTxState.mByteDeltaAccThr / TRAFFIC_MODEM_POLL_BYTE_RATIO,
                TRAFFIC_POLL_BYTE_THRESHOLD_MAX);
        long rxByteDeltaThr = Math.min(mRxState.mByteDeltaAccThr / TRAFFIC_MODEM_POLL_BYTE_RATIO,
                TRAFFIC_POLL_BYTE_THRESHOLD_MAX);
        if (txBytesDelta < txByteDeltaThr && rxBytesDelta < rxByteDeltaThr
                && mTxBytesDeltaAcc < mTxState.mByteDeltaAccThr
                && mRxBytesDeltaAcc < mRxState.mByteDeltaAccThr) {
            doModemPoll = false;
        }

        long currTimeMs = mTelephonyFacade.getElapsedSinceBootMillis();
        long timeSinceLastModemPollMs = currTimeMs - mLastModemPollTimeMs;
        if (timeSinceLastModemPollMs < MODEM_POLL_MIN_INTERVAL_MS) {
            doModemPoll = false;
        }

        if (doModemPoll) {
            StringBuilder sb = new StringBuilder();
            logd(sb.append("txByteDelta ").append(txBytesDelta)
                    .append(" rxByteDelta ").append(rxBytesDelta)
                    .append(" txByteDeltaAcc ").append(mTxBytesDeltaAcc)
                    .append(" rxByteDeltaAcc ").append(mRxBytesDeltaAcc)
                    .append(" trigger modem activity request").toString());
            updateDataRatCellIdentityBandwidth();
            // Filter update will happen after the request
            makeRequestModemActivity();
            return;
        }

        long timeSinceLastFilterUpdateMs = currTimeMs - mFilterUpdateTimeMs;
        // Update filter
        if (timeSinceLastFilterUpdateMs >= FILTER_UPDATE_MAX_INTERVAL_MS) {
            if (!updateDataRatCellIdentityBandwidth()) {
                updateTxRxBandwidthFilterSendToDataConnection();
            }
        }
    }

    private void makeRequestModemActivity() {
        mLastModemPollTimeMs = mTelephonyFacade.getElapsedSinceBootMillis();
        // TODO: add CountDown in case that onResult/OnError() never happen
        mTelephonyManager.requestModemActivityInfo(Runnable::run, mOutcomeReceiver);
    }

    private void handleModemActivityReturned(ModemActivityInfo result) {
        updateBandwidthTxRxSamples(result);
        updateTxRxBandwidthFilterSendToDataConnection();
        mLastModemActivityInfo = result;
        // Update for next poll
        resetByteDeltaAcc();
    }

    private void resetByteDeltaAcc() {
        mTxBytesDeltaAcc = 0;
        mRxBytesDeltaAcc = 0;
    }

    private void invalidateTxRxSamples() {
        mTxState.mBwSampleValid = false;
        mRxState.mBwSampleValid = false;
    }

    private void updateBandwidthTxRxSamples(ModemActivityInfo modemActivityInfo) {
        if (mLastModemActivityInfo == null || modemActivityInfo == null
                || mNetworkCapabilities == null || hasRecentDataRegStatePlmnOrRatChange()) {
            return;
        }

        long lastTimeMs = mLastModemActivityInfo.getTimestampMillis();
        long currTimeMs = modemActivityInfo.getTimestampMillis();
        long timeDeltaMs = currTimeMs - lastTimeMs;

        if (timeDeltaMs > MODEM_POLL_TIME_DELTA_MAX_MS || timeDeltaMs <= 0) {
            return;
        }
        ModemActivityInfo deltaInfo = mLastModemActivityInfo.getDelta(modemActivityInfo);
        long txTimeDeltaMs = getModemTxTimeMs(deltaInfo);
        long rxTimeDeltaMs = deltaInfo.getReceiveTimeMillis();

        // Check if txTimeDeltaMs / rxTimeDeltaMs > TX_OVER_RX_TIME_RATIO_THRESHOLD
        boolean isTxTimeOverRxTimeRatioLarge = (txTimeDeltaMs * TX_OVER_RX_TIME_RATIO_THRESHOLD_DEN
                > rxTimeDeltaMs * TX_OVER_RX_TIME_RATIO_THRESHOLD_NUM);
        long rxTimeBwEstMs = isTxTimeOverRxTimeRatioLarge
                ? (txTimeDeltaMs + rxTimeDeltaMs) : rxTimeDeltaMs;

        mTxState.updateBandwidthSample(mTxBytesDeltaAcc, txTimeDeltaMs);
        mRxState.updateBandwidthSample(mRxBytesDeltaAcc, rxTimeBwEstMs);

        int reportedTxTputKbps = mNetworkCapabilities.getLinkUpstreamBandwidthKbps();
        int reportedRxTputKbps = mNetworkCapabilities.getLinkDownstreamBandwidthKbps();

        StringBuilder sb = new StringBuilder();
        logd(sb.append("UpdateBwSample")
                .append(" dBm ").append(mSignalStrengthDbm)
                .append(" level ").append(mSignalLevel)
                .append(" rat ").append(getDataRatName(mDataRat))
                .append(" plmn ").append(mPlmn)
                .append(" tac ").append(mTac)
                .append(" reportedTxKbps ").append(reportedTxTputKbps)
                .append(" reportedRxKbps ").append(reportedRxTputKbps)
                .append(" txMs ").append(txTimeDeltaMs)
                .append(" rxMs ").append(rxTimeDeltaMs)
                .append(" txKB ").append(mTxBytesDeltaAcc / 1024)
                .append(" rxKB ").append(mRxBytesDeltaAcc / 1024)
                .append(" txKBThr ").append(mTxState.mByteDeltaAccThr / 1024)
                .append(" rxKBThr ").append(mRxState.mByteDeltaAccThr / 1024)
                .toString());
    }

    private boolean hasRecentDataRegStatePlmnOrRatChange() {
        if (mLastModemActivityInfo == null) {
            return false;
        }
        return (mLastDrsOrRatChangeTimeMs > mLastModemActivityInfo.getTimestampMillis()
            || mLastPlmnOrRatChangeTimeMs > mLastModemActivityInfo.getTimestampMillis());
    }

    private long getModemTxTimeMs(ModemActivityInfo modemActivity) {
        long txTimeMs = 0;
        for (int lvl = 0; lvl < ModemActivityInfo.getNumTxPowerLevels(); lvl++) {
            txTimeMs += modemActivity.getTransmitDurationMillisAtPowerLevel(lvl);
        }
        return txTimeMs;
    }

    private void updateTxRxBandwidthFilterSendToDataConnection() {
        mFilterUpdateTimeMs = mTelephonyFacade.getElapsedSinceBootMillis();
        mTxState.updateBandwidthFilter();
        mRxState.updateBandwidthFilter();

        boolean isNetworkChanged = mTxState.hasLargeBwChange()
                || mRxState.hasLargeBwChange()
                || mBandwidthUpdateDataRat != mDataRat
                || mBandwidthUpdateSignalLevel != mSignalLevel
                || !mBandwidthUpdatePlmn.equals(mPlmn);
        if (isValidNetwork() && isNetworkChanged) {
            mTxState.mLastReportedBwKbps = mTxState.mAvgUsedKbps < 0 ? -1 : mTxState.mFilterKbps;
            mRxState.mLastReportedBwKbps  = mRxState.mAvgUsedKbps < 0 ? -1 : mRxState.mFilterKbps;
            sendLinkBandwidthToDataConnection(
                    mTxState.mLastReportedBwKbps,
                    mRxState.mLastReportedBwKbps);
        }
        mBandwidthUpdateSignalDbm = mSignalStrengthDbm;
        mBandwidthUpdateSignalLevel = mSignalLevel;
        mBandwidthUpdateDataRat = mDataRat;
        mBandwidthUpdatePlmn = mPlmn;

        mTxState.calculateError();
        mRxState.calculateError();
    }

    private boolean isValidNetwork() {
        return !mPlmn.equals(UNKNOWN_PLMN) && mDataRat != TelephonyManager.NETWORK_TYPE_UNKNOWN;
    }

    private class BandwidthState {
        private final int mLink;
        int mFilterKbps;
        int mByteDeltaAccThr = BYTE_DELTA_THRESHOLD_KB[0][0];
        int mAvgUsedKbps;
        int mBwSampleKbps;
        boolean mBwSampleValid;
        long mBwSampleValidTimeMs;
        int mStaticBwKbps;
        int mLastReportedBwKbps;

        BandwidthState(int link) {
            mLink = link;
        }

        private void updateBandwidthSample(long bytesDelta, long timeDeltaMs) {
            updateByteCountThr();
            if (bytesDelta < mByteDeltaAccThr) {
                return;
            }
            if (timeDeltaMs < TX_RX_TIME_MIN_MS) {
                return;
            }
            long linkBandwidthLongKbps = bytesDelta * 8 / timeDeltaMs * 1000 / 1024;
            if (linkBandwidthLongKbps > (long) mStaticBwKbps * MAX_BW_TO_STATIC_BW_RATIO
                    || linkBandwidthLongKbps < 0) {
                return;
            }
            int linkBandwidthKbps = (int) linkBandwidthLongKbps;
            mBwSampleValid = true;
            mBwSampleKbps = linkBandwidthKbps;

            String dataRatName = getDataRatName(mDataRat);
            NetworkBandwidth network = lookupNetwork(mPlmn, dataRatName);
            // Update per RAT stats of all TAC
            network.update(linkBandwidthKbps, mLink, mSignalLevel);

            // Update per TAC stats
            network = lookupNetwork(mPlmn, mTac, dataRatName);
            network.update(linkBandwidthKbps, mLink, mSignalLevel);
        }

        private void updateBandwidthFilter() {
            int avgKbps = getAvgLinkBandwidthKbps();
            // Feed the filter with the long term avg if there is no valid BW sample so that filter
            // will gradually converge the long term avg.
            int filterInKbps = mBwSampleValid ? mBwSampleKbps : avgKbps;

            long currTimeMs = mTelephonyFacade.getElapsedSinceBootMillis();
            int timeDeltaSec = (int) (currTimeMs - mBwSampleValidTimeMs) / 1000;

            // If the operation condition changes significantly since the last update
            // or the sample has higher BW, use a faster filter. Otherwise, use a slow filter
            int timeConstantSec;
            if (Math.abs(mBandwidthUpdateSignalDbm - mSignalStrengthDbm) > RSSI_DELTA_THRESHOLD_DB
                    || !mBandwidthUpdatePlmn.equals(mPlmn)
                    || mBandwidthUpdateDataRat != mDataRat
                    || (mBwSampleValid && mBwSampleKbps > avgKbps)) {
                timeConstantSec = TIME_CONSTANT_SMALL_SEC;
            } else {
                timeConstantSec = TIME_CONSTANT_LARGE_SEC;
            }
            // Update timestamp for next iteration
            if (mBwSampleValid) {
                mBwSampleValidTimeMs = currTimeMs;
            }

            if (filterInKbps == mFilterKbps) {
                return;
            }

            int alpha = timeDeltaSec > LARGE_TIME_DECAY_RATIO * timeConstantSec ? 0
                    : (int) (FILTER_SCALE * Math.exp(-1.0 * timeDeltaSec / timeConstantSec));
            if (alpha == 0) {
                mFilterKbps = filterInKbps;
                return;
            }
            long filterOutKbps = (long) mFilterKbps * alpha
                    + filterInKbps * FILTER_SCALE - filterInKbps * alpha;
            filterOutKbps = filterOutKbps / FILTER_SCALE;
            mFilterKbps = (int) Math.min(filterOutKbps, Integer.MAX_VALUE);

            StringBuilder sb = new StringBuilder();
            logv(sb.append(mLink)
                    .append(" lastSampleWeight=").append(alpha)
                    .append("/").append(FILTER_SCALE)
                    .append(" filterInKbps=").append(filterInKbps)
                    .append(" avgKbps=").append(avgKbps)
                    .append(" filterOutKbps=").append(mFilterKbps)
                    .toString());
        }

        private int getAvgUsedLinkBandwidthKbps() {
            // Check if current TAC/RAT/level has enough stats
            String dataRatName = getDataRatName(mDataRat);
            NetworkBandwidth network = lookupNetwork(mPlmn, mTac, dataRatName);
            int count = network.getCount(mLink, mSignalLevel);
            if (count >= BW_STATS_COUNT_THRESHOLD) {
                return (int) (network.getValue(mLink, mSignalLevel) / count);
            }

            // Check if current RAT/level has enough stats
            network = lookupNetwork(mPlmn, dataRatName);
            count = network.getCount(mLink, mSignalLevel);
            if (count >= BW_STATS_COUNT_THRESHOLD) {
                return (int) (network.getValue(mLink, mSignalLevel) / count);
            }
            return -1;
        }

        private int getCurrentCount() {
            String dataRatName = getDataRatName(mDataRat);
            NetworkBandwidth network = lookupNetwork(mPlmn, dataRatName);
            return network.getCount(mLink, mSignalLevel);
        }

        /** get a long term avg value (PLMN/RAT/TAC/level dependent) or static value */
        private int getAvgLinkBandwidthKbps() {
            mAvgUsedKbps = getAvgUsedLinkBandwidthKbps();
            if (mAvgUsedKbps > 0) {
                return mAvgUsedKbps;
            }
            // Fall back to static value
            return mStaticBwKbps;
        }

        private void resetBandwidthFilter() {
            mBwSampleValid = false;
            mFilterKbps = getAvgLinkBandwidthKbps();
        }

        private void updateByteCountThr() {
            // For high BW RAT cases, use predefined value + threshold derived from avg usage BW
            if (mStaticBwKbps > HIGH_BANDWIDTH_THRESHOLD_KBPS) {
                int lowBytes = calculateByteCountThreshold(getAvgUsedLinkBandwidthKbps(),
                        MODEM_POLL_MIN_INTERVAL_MS);
                // Start with a predefined value
                mByteDeltaAccThr = BYTE_DELTA_THRESHOLD_KB[mLink][mSignalLevel] * 1024;
                if (lowBytes > 0) {
                    // Raise the threshold if the avg usage BW is high
                    mByteDeltaAccThr = Math.max(lowBytes, mByteDeltaAccThr);
                    mByteDeltaAccThr = Math.min(mByteDeltaAccThr,
                            BYTE_DELTA_ACC_THRESHOLD_MAX_KB * 1024);
                }
                return;
            }
            // For low BW RAT cases, derive the threshold from avg BW values
            mByteDeltaAccThr = calculateByteCountThreshold(mStaticBwKbps,
                    MODEM_POLL_MIN_INTERVAL_MS);

            mByteDeltaAccThr = Math.max(mByteDeltaAccThr, BYTE_DELTA_THRESHOLD_MIN_KB * 1024);
            // Low BW RAT threshold value should be no more than high BW one.
            mByteDeltaAccThr = Math.min(mByteDeltaAccThr, BYTE_DELTA_THRESHOLD_KB[mLink][0] * 1024);
        }

        // Calculate a byte count threshold for the given avg BW and observation window size
        private int calculateByteCountThreshold(int avgBwKbps, int durationMs) {
            long avgBytes = (long) avgBwKbps / 8 * durationMs;
            long result = avgBytes * LOW_BW_TO_AVG_BW_RATIO_NUM / LOW_BW_TO_AVG_BW_RATIO_DEN;
            return (int) Math.min(result, Integer.MAX_VALUE);
        }

        public boolean hasLargeBwChange() {
            int deltaKbps = Math.abs(mLastReportedBwKbps - mFilterKbps);
            return mAvgUsedKbps > 0
                    && deltaKbps * 100 > BW_UPDATE_THRESHOLD_PERCENT * mLastReportedBwKbps;
        }

        public void calculateError() {
            if (!mBwSampleValid || getCurrentCount() <= BW_STATS_COUNT_THRESHOLD + 1
                    || mAvgUsedKbps <= 0) {
                return;
            }
            int bwEstExtErrPercent = calculateErrorPercent(mLastReportedBwKbps, mBwSampleKbps);
            int bwEstAvgErrPercent = calculateErrorPercent(mAvgUsedKbps, mBwSampleKbps);
            int bwEstIntErrPercent = calculateErrorPercent(mFilterKbps, mBwSampleKbps);
            int coldStartErrPercent = calculateErrorPercent(mStaticBwKbps, mBwSampleKbps);

            TelephonyMetrics.getInstance().writeBandwidthStats(mLink, mDataRat, getNrMode(mDataRat),
                    mSignalLevel, bwEstExtErrPercent, coldStartErrPercent, mBwSampleKbps);

            StringBuilder sb = new StringBuilder();
            logd(sb.append(mLink)
                    .append(" sampKbps ").append(mBwSampleKbps)
                    .append(" filtKbps ").append(mFilterKbps)
                    .append(" reportKbps ").append(mLastReportedBwKbps)
                    .append(" avgUsedKbps ").append(mAvgUsedKbps)
                    .append(" csKbps ").append(mStaticBwKbps)
                    .append(" intErrPercent ").append(bwEstIntErrPercent)
                    .append(" avgErrPercent ").append(bwEstAvgErrPercent)
                    .append(" extErrPercent ").append(bwEstExtErrPercent)
                    .append(" csErrPercent ").append(coldStartErrPercent)
                    .toString());
        }

        private int calculateErrorPercent(int inKbps, int bwSampleKbps) {
            long errorPercent = 100L * (inKbps - bwSampleKbps) / bwSampleKbps;
            return (int) Math.max(-MAX_ERROR_PERCENT, Math.min(errorPercent, MAX_ERROR_PERCENT));
        }
    }

    /**
     * Update the byte count threshold.
     * It should be called whenever the RAT or signal level is changed.
     * For the RAT with high BW (4G and beyond), use BYTE_DELTA_THRESHOLD_KB table.
     * For other RATs, derive the threshold based on the static BW values.
     */
    private void updateByteCountThr() {
        mTxState.updateByteCountThr();
        mRxState.updateByteCountThr();
    }

    // Reset BW filter to a long term avg value (PLMN/RAT/TAC dependent) or static BW value.
    // It should be called whenever PLMN/RAT or static BW value is changed;
    private void resetBandwidthFilter() {
        mTxState.resetBandwidthFilter();
        mRxState.resetBandwidthFilter();
    }

    private void sendLinkBandwidthToDataConnection(int linkBandwidthTxKps, int linkBandwidthRxKps) {
        logv("send to DC tx " + linkBandwidthTxKps + " rx " + linkBandwidthRxKps);
        Pair<Integer, Integer> bandwidthInfo =
                new Pair<Integer, Integer>(linkBandwidthTxKps, linkBandwidthRxKps);
        mBandwidthChangedRegistrants.notifyRegistrants(new AsyncResult(null, bandwidthInfo, null));
    }

    private void handleSignalStrengthChanged(SignalStrength signalStrength) {
        if (signalStrength == null) {
            return;
        }

        mSignalStrengthDbm = signalStrength.getDbm();
        mSignalLevel = signalStrength.getLevel();
        updateByteCountThr();
        if (updateDataRatCellIdentityBandwidth()) {
            return;
        }

        if (Math.abs(mBandwidthUpdateSignalDbm - mSignalStrengthDbm) > RSSI_DELTA_THRESHOLD_DB) {
            updateTxRxBandwidthFilterSendToDataConnection();
        }
    }

    private void registerNrStateFrequencyChange() {
        mPhone.getServiceStateTracker().registerForNrStateChanged(this,
                MSG_NR_STATE_CHANGED, null);
        mPhone.getServiceStateTracker().registerForNrFrequencyChanged(this,
                MSG_NR_FREQUENCY_CHANGED, null);
    }

    /**
     * Get a string based on current RAT
     */
    public String getDataRatName(int rat) {
        return getDataRatName(rat, getNrMode(rat));
    }

    private int getNrMode(int rat) {
        if (rat == TelephonyManager.NETWORK_TYPE_LTE && isNrNsaConnected()) {
            return mPhone.getServiceState().getNrFrequencyRange()
                    == ServiceState.FREQUENCY_RANGE_MMWAVE
                    ? NrMode.NR_NSA_MMWAVE : NrMode.NR_NSA;
        } else if (rat == TelephonyManager.NETWORK_TYPE_NR) {
            return mPhone.getServiceState().getNrFrequencyRange()
                    == ServiceState.FREQUENCY_RANGE_MMWAVE
                    ? NrMode.NR_SA_MMWAVE : NrMode.NR_SA;
        }
        return NrMode.NR_NONE;
    }

    /**
     * Get a string based on current RAT and NR operation mode.
     */
    public static String getDataRatName(int rat, int nrMode) {
        if (rat == TelephonyManager.NETWORK_TYPE_LTE
                && (nrMode == NrMode.NR_NSA || nrMode == NrMode.NR_NSA_MMWAVE)) {
            return nrMode == NrMode.NR_NSA
                    ? DctConstants.RAT_NAME_NR_NSA : DctConstants.RAT_NAME_NR_NSA_MMWAVE;
        } else if (rat == TelephonyManager.NETWORK_TYPE_NR) {
            return nrMode == NrMode.NR_SA
                    ? TelephonyManager.getNetworkTypeName(rat) : DctConstants.RAT_NAME_NR_SA_MMWAVE;
        }
        return TelephonyManager.getNetworkTypeName(rat);
    }

    /**
     * Check if the device is connected to NR 5G Non-Standalone network
     */
    private boolean isNrNsaConnected() {
        return mPhone.getServiceState().getNrState()
                == NetworkRegistrationInfo.NR_STATE_CONNECTED;
    }

    // Update avg BW values.
    // It should be called whenever the RAT could be changed.
    // return true if avg value is changed;
    private boolean updateStaticBwValue(int dataRat) {
        Pair<Integer, Integer> values = getStaticAvgBw(dataRat);
        if (values == null) {
            mTxState.mStaticBwKbps = DEFAULT_LINK_BAND_WIDTH_KBPS;
            mRxState.mStaticBwKbps = DEFAULT_LINK_BAND_WIDTH_KBPS;
            return true;
        }
        if (mTxState.mStaticBwKbps != values.second
                || mRxState.mStaticBwKbps != values.first) {
            mTxState.mStaticBwKbps = values.second;
            mRxState.mStaticBwKbps = values.first;
            return true;
        }
        return false;
    }

    /** get per-RAT static bandwidth value */
    public Pair<Integer, Integer> getStaticAvgBw(int dataRat) {
        String dataRatName = getDataRatName(dataRat);
        Pair<Integer, Integer> values = AVG_BW_PER_RAT_MAP.get(dataRatName);
        if (values == null) {
            Rlog.e(TAG, dataRatName + " is not found in Avg BW table");
        }
        return values;
    }

    private void updateStaticBwValueResetFilter() {
        if (updateStaticBwValue(mDataRat)) {
            updateByteCountThr();
            resetBandwidthFilter();
            updateTxRxBandwidthFilterSendToDataConnection();
        }
    }

    private NetworkRegistrationInfo getDataNri() {
        return  mPhone.getServiceState().getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
    }

    private boolean updateDataRatCellIdentityBandwidth() {
        boolean updatedPlmn = false;
        CellIdentity cellIdentity = mPhone.getCurrentCellIdentity();
        mTac = getTac(cellIdentity);
        String plmn;

        if (mPhone.getServiceState().getOperatorNumeric() != null) {
            plmn = mPhone.getServiceState().getOperatorNumeric();
        } else {
            if (cellIdentity.getPlmn() != null) {
                plmn = cellIdentity.getPlmn();
            } else {
                plmn = UNKNOWN_PLMN;
            }
        }
        if (mPlmn == null || !plmn.equals(mPlmn)) {
            updatedPlmn = true;
            mPlmn = plmn;
        }

        boolean updatedRat = false;
        NetworkRegistrationInfo nri = getDataNri();
        if (nri != null) {
            int dataRat = nri.getAccessNetworkTechnology();
            if (dataRat != mDataRat) {
                updatedRat = true;
                mDataRat = dataRat;
                updateStaticBwValue(mDataRat);
                updateByteCountThr();
            }
        }

        boolean updatedPlmnOrRat = updatedPlmn || updatedRat;
        if (updatedPlmnOrRat) {
            resetBandwidthFilter();
            updateTxRxBandwidthFilterSendToDataConnection();
            mLastPlmnOrRatChangeTimeMs = mTelephonyFacade.getElapsedSinceBootMillis();
        }
        return updatedPlmnOrRat;
    }

    private int getTac(@NonNull CellIdentity cellIdentity) {
        if (cellIdentity instanceof CellIdentityLte) {
            return ((CellIdentityLte) cellIdentity).getTac();
        }
        if (cellIdentity instanceof CellIdentityNr) {
            return ((CellIdentityNr) cellIdentity).getTac();
        }
        if (cellIdentity instanceof CellIdentityWcdma) {
            return ((CellIdentityWcdma) cellIdentity).getLac();
        }
        if (cellIdentity instanceof CellIdentityTdscdma) {
            return ((CellIdentityTdscdma) cellIdentity).getLac();
        }
        if (cellIdentity instanceof CellIdentityGsm) {
            return ((CellIdentityGsm) cellIdentity).getLac();
        }
        return 0;
    }

    private class TelephonyCallbackImpl extends TelephonyCallback implements
            TelephonyCallback.SignalStrengthsListener,
            TelephonyCallback.ActiveDataSubscriptionIdListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            obtainMessage(MSG_SIGNAL_STRENGTH_CHANGED, signalStrength).sendToTarget();
        }
        @Override
        public void onActiveDataSubscriptionIdChanged(int subId) {
            obtainMessage(MSG_ACTIVE_PHONE_CHANGED, subId).sendToTarget();
        }
    }

    void logv(String msg) {
        if (DBG) Rlog.v(TAG, msg);
    }

    void logd(String msg) {
        if (DBG) Rlog.d(TAG, msg);
        mLocalLog.log(msg);
    }

    @VisibleForTesting
    static final int UNKNOWN_TAC = -1;
    // Map with NetworkKey as the key and NetworkBandwidth as the value.
    // NetworkKey is specified by the PLMN, data RAT and TAC of network.
    // NetworkBandwidth represents the bandwidth related stats of each network.
    private final Map<NetworkKey, NetworkBandwidth> mNetworkMap = new ArrayMap<>();
    private static class NetworkKey {
        private final String mPlmn;
        private final String mDataRat;
        private final int mTac;
        NetworkKey(String plmn, int tac, String dataRat) {
            mPlmn = plmn;
            mTac = tac;
            mDataRat = dataRat;
        }
        @Override
        public boolean equals(@Nullable Object o) {
            if (o == null || !(o instanceof NetworkKey) || hashCode() != o.hashCode()) {
                return false;
            }

            if (this == o) {
                return true;
            }

            NetworkKey that = (NetworkKey) o;
            return mPlmn.equals(that.mPlmn)
                    && mTac == that.mTac
                    && mDataRat.equals(that.mDataRat);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mPlmn, mDataRat, mTac);
        }
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Plmn").append(mPlmn)
                    .append("Rat").append(mDataRat)
                    .append("Tac").append(mTac)
                    .toString();
            return sb.toString();
        }
    }

    @NonNull
    private NetworkBandwidth lookupNetwork(String plmn, String dataRat) {
        return lookupNetwork(plmn, UNKNOWN_TAC, dataRat);
    }

    /** Look up NetworkBandwidth and create a new one if it doesn't exist */
    @VisibleForTesting
    @NonNull
    public NetworkBandwidth lookupNetwork(String plmn, int tac, String dataRat) {
        if (plmn == null || dataRat.equals(
                TelephonyManager.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_UNKNOWN))) {
            return mPlaceholderNetwork;
        }
        NetworkKey key = new NetworkKey(plmn, tac, dataRat);
        NetworkBandwidth ans = mNetworkMap.get(key);
        if (ans == null) {
            ans = new NetworkBandwidth(key.toString());
            mNetworkMap.put(key, ans);
        }
        return ans;
    }

    /** A class holding link bandwidth related stats */
    @VisibleForTesting
    public class NetworkBandwidth {
        private final String mKey;
        NetworkBandwidth(String key) {
            mKey = key;
        }

        /** Update link bandwidth stats */
        public void update(long value, int link, int level) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                    mPhone.getContext());
            String valueKey = getValueKey(link, level);
            String countKey = getCountKey(link, level);
            SharedPreferences.Editor editor = sp.edit();
            long currValue = sp.getLong(valueKey, 0);
            int currCount = sp.getInt(countKey, 0);
            editor.putLong(valueKey, currValue + value);
            editor.putInt(countKey, currCount + 1);
            editor.apply();
        }

        private String getValueKey(int link, int level) {
            return getDataKey(link, level) + "Data";
        }

        private String getCountKey(int link, int level) {
            return getDataKey(link, level) + "Count";
        }

        private String getDataKey(int link, int level) {
            StringBuilder sb = new StringBuilder();
            return sb.append(mKey)
                    .append("Link").append(link)
                    .append("Level").append(level)
                    .toString();
        }

        /** Get the accumulated bandwidth value */
        public long getValue(int link, int level) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                    mPhone.getContext());
            String valueKey = getValueKey(link, level);
            return sp.getLong(valueKey, 0);
        }

        /** Get the accumulated bandwidth count */
        public int getCount(int link, int level) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                    mPhone.getContext());
            String countKey = getCountKey(link, level);
            return sp.getInt(countKey, 0);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(mKey);
            sb.append("\n");
            for (int link = 0; link < NUM_LINK_DIRECTION; link++) {
                sb.append((link == 0 ? "tx" : "rx"));
                sb.append("\n avgKbps");
                for (int level = 0; level < NUM_SIGNAL_LEVEL; level++) {
                    int count = getCount(link, level);
                    int avgKbps = count == 0 ? 0 : (int) (getValue(link, level) / count);
                    sb.append(" ").append(avgKbps);
                }
                sb.append("\n count");
                for (int level = 0; level < NUM_SIGNAL_LEVEL; level++) {
                    int count = getCount(link, level);
                    sb.append(" ").append(count);
                }
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Dump the internal state and local logs
     */
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, " ");
        pw.increaseIndent();
        pw.println("current PLMN " + mPlmn + " TAC " + mTac + " RAT " + getDataRatName(mDataRat));
        pw.println("all networks visited since device boot");
        for (NetworkBandwidth network : mNetworkMap.values()) {
            pw.println(network.toString());
        }

        try {
            mLocalLog.dump(fd, pw, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        pw.decreaseIndent();
        pw.println();
        pw.flush();
    }

}
