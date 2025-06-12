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

package com.android.internal.telephony.metrics;

import static android.net.ConnectivityDiagnosticsManager.ConnectivityReport.KEY_NETWORK_PROBES_ATTEMPTED_BITMASK;
import static android.net.ConnectivityDiagnosticsManager.ConnectivityReport.KEY_NETWORK_PROBES_SUCCEEDED_BITMASK;
import static android.net.ConnectivityDiagnosticsManager.ConnectivityReport.KEY_NETWORK_VALIDATION_RESULT;
import static android.net.ConnectivityDiagnosticsManager.DataStallReport.KEY_TCP_METRICS_COLLECTION_PERIOD_MILLIS;
import static android.net.ConnectivityDiagnosticsManager.DataStallReport.KEY_TCP_PACKET_FAIL_RATE;
import static android.net.ConnectivityDiagnosticsManager.DataStallReport.KEY_DNS_CONSECUTIVE_TIMEOUTS;

import android.annotation.ElapsedRealtimeLong;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.ConnectivityDiagnosticsManager;
import android.net.ConnectivityDiagnosticsManager.ConnectivityDiagnosticsCallback;
import android.net.ConnectivityDiagnosticsManager.ConnectivityReport;
import android.net.ConnectivityDiagnosticsManager.DataStallReport;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.telephony.AccessNetworkConstants;
import android.telephony.Annotation.NetworkType;
import android.telephony.CellSignalStrength;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataCallResponse.LinkStatus;
import android.text.TextUtils;

import com.android.internal.os.BackgroundThread;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyStatsLog;
import com.android.internal.telephony.data.DataNetwork;
import com.android.internal.telephony.data.DataNetworkController;
import com.android.internal.telephony.data.DataNetworkController.DataNetworkControllerCallback;
import com.android.internal.telephony.data.DataStallRecoveryManager;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.subscription.SubscriptionInfoInternal;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.telephony.Rlog;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Generates metrics related to data stall recovery events per phone ID for the pushed atom.
 */
public class DataStallRecoveryStats {

    /**
     * Value indicating that link bandwidth is unspecified.
     * Copied from {@code NetworkCapabilities#LINK_BANDWIDTH_UNSPECIFIED}
     */
    private static final int LINK_BANDWIDTH_UNSPECIFIED = 0;

    private static final String TAG = "DSRS-";

    private static final int UNSET_DIAGNOSTIC_STATE = -1;

    private static final long REFRESH_DURATION_IN_MILLIS = TimeUnit.MINUTES.toMillis(3);

    // Handler to upload metrics.
    private final @NonNull Handler mHandler;

    private final @NonNull String mTag;
    private final @NonNull Phone mPhone;
    private final @NonNull TelephonyManager mTelephonyManager;
    private final @NonNull FeatureFlags mFeatureFlags;

    // The interface name of the internet network.
    private @Nullable String mIfaceName = null;

    /* Metrics and stats data variables */
    // Record metrics refresh time in milliseconds to decide whether to refresh data again
    @ElapsedRealtimeLong
    private long mMetricsReflashTime = 0L;
    private int mPhoneId = 0;
    private int mCarrierId = TelephonyManager.UNKNOWN_CARRIER_ID;
    private int mConvertedMccMnc = -1;
    private int mSignalStrength = CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
    private int mBand = 0;
    // The RAT used for data (including IWLAN).
    private @NetworkType int mRat = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    private boolean mIsOpportunistic = false;
    private boolean mIsMultiSim = false;
    private int mNetworkRegState = NetworkRegistrationInfo
                    .REGISTRATION_STATE_NOT_REGISTERED_OR_SEARCHING;
    // Info of the other device in case of DSDS
    private int mOtherSignalStrength = CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
    private int mOtherNetworkRegState = NetworkRegistrationInfo
                    .REGISTRATION_STATE_NOT_REGISTERED_OR_SEARCHING;
    // Link status of the data network
    private @LinkStatus int mInternetLinkStatus = DataCallResponse.LINK_STATUS_UNKNOWN;

    // The link bandwidth of the data network
    private int mLinkDownBandwidthKbps = LINK_BANDWIDTH_UNSPECIFIED;
    private int mLinkUpBandwidthKbps = LINK_BANDWIDTH_UNSPECIFIED;

    // Connectivity diagnostics states
    private int mNetworkProbesResult = UNSET_DIAGNOSTIC_STATE;
    private int mNetworkProbesType = UNSET_DIAGNOSTIC_STATE;
    private int mNetworkValidationResult = UNSET_DIAGNOSTIC_STATE;
    private int mTcpMetricsCollectionPeriodMillis = UNSET_DIAGNOSTIC_STATE;
    private int mTcpPacketFailRate = UNSET_DIAGNOSTIC_STATE;
    private int mDnsConsecutiveTimeouts = UNSET_DIAGNOSTIC_STATE;

    private ConnectivityDiagnosticsManager mConnectivityDiagnosticsManager = null;
    private ConnectivityDiagnosticsCallback mConnectivityDiagnosticsCallback = null;
    private static final Executor INLINE_EXECUTOR = x -> x.run();

    /**
     * Constructs a new instance of {@link DataStallRecoveryStats}.
     */
    public DataStallRecoveryStats(
            @NonNull final Phone phone,
            @NonNull FeatureFlags featureFlags,
            @NonNull final DataNetworkController dataNetworkController) {
        mTag = TAG + phone.getPhoneId();
        mPhone = phone;
        mFeatureFlags = featureFlags;

        if (mFeatureFlags.threadShred()) {
            mHandler = new Handler(BackgroundThread.get().getLooper());
        } else {
            HandlerThread handlerThread = new HandlerThread(mTag + "-thread");
            handlerThread.start();
            mHandler = new Handler(handlerThread.getLooper());
        }

        mTelephonyManager = mPhone.getContext().getSystemService(TelephonyManager.class);

        dataNetworkController.registerDataNetworkControllerCallback(
                new DataNetworkControllerCallback(mHandler::post) {
                @Override
                public void onConnectedInternetDataNetworksChanged(
                        @NonNull Set<DataNetwork> internetNetworks) {
                    mIfaceName = null;
                    for (DataNetwork dataNetwork : internetNetworks) {
                        mIfaceName = dataNetwork.getLinkProperties().getInterfaceName();
                        break;
                    }
                }

                @Override
                public void onPhysicalLinkStatusChanged(@LinkStatus int status) {
                    mInternetLinkStatus = status;
                }
            });

        try {
            // Register ConnectivityDiagnosticsCallback to get diagnostics states
            mConnectivityDiagnosticsManager =
                mPhone.getContext().getSystemService(ConnectivityDiagnosticsManager.class);
            mConnectivityDiagnosticsCallback = new ConnectivityDiagnosticsCallback() {
                @Override
                public void onConnectivityReportAvailable(@NonNull ConnectivityReport report) {
                    PersistableBundle bundle = report.getAdditionalInfo();
                    mNetworkProbesResult = bundle.getInt(KEY_NETWORK_PROBES_SUCCEEDED_BITMASK);
                    mNetworkProbesType = bundle.getInt(KEY_NETWORK_PROBES_ATTEMPTED_BITMASK);
                    mNetworkValidationResult = bundle.getInt(KEY_NETWORK_VALIDATION_RESULT);
                }

                @Override
                public void onDataStallSuspected(@NonNull DataStallReport report) {
                    PersistableBundle bundle = report.getStallDetails();
                    mTcpMetricsCollectionPeriodMillis =
                        bundle.getInt(KEY_TCP_METRICS_COLLECTION_PERIOD_MILLIS);
                    mTcpPacketFailRate = bundle.getInt(KEY_TCP_PACKET_FAIL_RATE);
                    mDnsConsecutiveTimeouts = bundle.getInt(KEY_DNS_CONSECUTIVE_TIMEOUTS);
                }
            };
            mConnectivityDiagnosticsManager.registerConnectivityDiagnosticsCallback(
                new NetworkRequest.Builder()
                    .clearCapabilities()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .build(),
                    INLINE_EXECUTOR,
                    mConnectivityDiagnosticsCallback
            );
        } catch (Exception e) {
            mConnectivityDiagnosticsManager = null;
            mConnectivityDiagnosticsCallback = null;
        }
    }

    /**
     * Create and push new atom when there is a data stall recovery event.
     *
     * @param action The recovery action.
     * @param isRecovered Whether the data stall has been recovered.
     * @param duration The duration from data stall occurred in milliseconds.
     * @param reason The reason for the recovery.
     * @param isFirstValidation Whether this is the first validation after recovery.
     * @param durationOfAction The duration of the current action in milliseconds.
     */
    public void uploadMetrics(
            @DataStallRecoveryManager.RecoveryAction int action,
            boolean isRecovered,
            int duration,
            @DataStallRecoveryManager.RecoveredReason int reason,
            boolean isFirstValidation,
            int durationOfAction) {

        mHandler.post(() -> {
            // Update data stall stats
            log("Set recovery action to " + action);

            // Refreshes the metrics data.
            try {
                refreshMetricsData();
            } catch (Exception e) {
                loge("The metrics data cannot be refreshed.", e);
                return;
            }

            TelephonyStatsLog.write(
                    TelephonyStatsLog.DATA_STALL_RECOVERY_REPORTED,
                    mCarrierId,
                    mRat,
                    mSignalStrength,
                    action,
                    mIsOpportunistic,
                    mIsMultiSim,
                    mBand,
                    isRecovered,
                    duration,
                    reason,
                    mOtherSignalStrength,
                    mOtherNetworkRegState,
                    mNetworkRegState,
                    isFirstValidation,
                    mPhoneId,
                    durationOfAction,
                    mInternetLinkStatus,
                    mLinkUpBandwidthKbps,
                    mLinkDownBandwidthKbps);

            log("Upload stats: "
                    + "Action:"
                    + action
                    + ", Recovered:"
                    + isRecovered
                    + ", Duration:"
                    + duration
                    + ", Reason:"
                    + reason
                    + ", First validation:"
                    + isFirstValidation
                    + ", Duration of action:"
                    + durationOfAction
                    + ", "
                    + this);
        });
    }

    /**
     * Refreshes the metrics data.
     */
    private void refreshMetricsData() {
        logd("Refreshes the metrics data.");
        // Update the metrics reflash time
        mMetricsReflashTime = SystemClock.elapsedRealtime();
        // Update phone id/carrier id and signal strength
        mPhoneId = mPhone.getPhoneId() + 1;
        mCarrierId = mPhone.getCarrierId();
        mSignalStrength = mPhone.getSignalStrength().getLevel();
        // Get the MCCMNC and convert it to an int
        String networkOperator = mTelephonyManager.getNetworkOperator();
        if (!TextUtils.isEmpty(networkOperator)) {
            try {
                mConvertedMccMnc = Integer.parseInt(networkOperator);
            } catch (NumberFormatException e) {
                loge("Invalid MCCMNC format: " + networkOperator);
                mConvertedMccMnc = -1;
            }
        } else {
            mConvertedMccMnc = -1;
        }

        // Update the bandwidth.
        updateBandwidths();

        // Update the RAT and band.
        updateRatAndBand();

        // Update the opportunistic state.
        mIsOpportunistic = getIsOpportunistic(mPhone);

        // Update the multi-SIM state.
        mIsMultiSim = SimSlotState.getCurrentState().numActiveSims > 1;

        // Update the network registration state.
        updateNetworkRegState();

        // Update the DSDS information.
        updateDsdsInfo();
    }

    /**
     * Updates the bandwidth for the current data network.
     */
    private void updateBandwidths() {
        mLinkDownBandwidthKbps = mLinkUpBandwidthKbps = LINK_BANDWIDTH_UNSPECIFIED;

        if (mIfaceName == null) {
            loge("Interface name is null");
            return;
        }

        DataNetworkController dataNetworkController = mPhone.getDataNetworkController();
        if (dataNetworkController == null) {
            loge("DataNetworkController is null");
            return;
        }

        DataNetwork dataNetwork = dataNetworkController.getDataNetworkByInterface(mIfaceName);
        if (dataNetwork == null) {
            loge("DataNetwork is null");
            return;
        }
        NetworkCapabilities networkCapabilities = dataNetwork.getNetworkCapabilities();
        if (networkCapabilities == null) {
            loge("NetworkCapabilities is null");
            return;
        }

        mLinkDownBandwidthKbps = networkCapabilities.getLinkDownstreamBandwidthKbps();
        mLinkUpBandwidthKbps = networkCapabilities.getLinkUpstreamBandwidthKbps();
    }

    private void updateRatAndBand() {
        mRat = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        mBand = 0;
        ServiceState serviceState = mPhone.getServiceState();
        if (serviceState == null) {
            loge("ServiceState is null");
            return;
        }

        mRat = serviceState.getDataNetworkType();
        mBand =
            (mRat == TelephonyManager.NETWORK_TYPE_IWLAN) ? 0 : ServiceStateStats.getBand(mPhone);
    }

    private static boolean getIsOpportunistic(@NonNull Phone phone) {
        SubscriptionInfoInternal subInfo = SubscriptionManagerService.getInstance()
                .getSubscriptionInfoInternal(phone.getSubId());
        return subInfo != null && subInfo.isOpportunistic();
    }

    private void updateNetworkRegState() {
        mNetworkRegState = NetworkRegistrationInfo
            .REGISTRATION_STATE_NOT_REGISTERED_OR_SEARCHING;

        NetworkRegistrationInfo phoneRegInfo = mPhone.getServiceState()
                .getNetworkRegistrationInfo(NetworkRegistrationInfo.DOMAIN_PS,
                AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        if (phoneRegInfo != null) {
            mNetworkRegState = phoneRegInfo.getRegistrationState();
        }
    }

    private void updateDsdsInfo() {
        mOtherSignalStrength = CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN;
        mOtherNetworkRegState = NetworkRegistrationInfo
            .REGISTRATION_STATE_NOT_REGISTERED_OR_SEARCHING;
        for (Phone otherPhone : PhoneFactory.getPhones()) {
            if (otherPhone.getPhoneId() == mPhone.getPhoneId()) continue;
            if (!getIsOpportunistic(otherPhone)) {
                mOtherSignalStrength = otherPhone.getSignalStrength().getLevel();
                NetworkRegistrationInfo regInfo = otherPhone.getServiceState()
                        .getNetworkRegistrationInfo(NetworkRegistrationInfo.DOMAIN_PS,
                        AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
                if (regInfo != null) {
                    mOtherNetworkRegState = regInfo.getRegistrationState();
                }
                break;
            }
        }
    }

    /**
     * Return bundled data stall recovery metrics data.
     *
     * @param action The recovery action.
     * @param isRecovered Whether the data stall has been recovered.
     * @param duration The duration from data stall occurred in milliseconds.
     * @param reason The reason for the recovery.
     * @param validationCount The total number of validation duration a data stall.
     * @param actionValidationCount The number of validation for current action during a data stall
     * @param durationOfAction The duration of the current action in milliseconds.
     */
    public Bundle getDataStallRecoveryMetricsData(
            @DataStallRecoveryManager.RecoveryAction int action,
            boolean isRecovered,
            int duration,
            @DataStallRecoveryManager.RecoveredReason int reason,
            int validationCount,
            int actionValidationCount,
            int durationOfAction) {

        // Refresh data if the data has not been updated within 3 minutes
        final long refreshDuration = SystemClock.elapsedRealtime() - mMetricsReflashTime;
        if (refreshDuration > REFRESH_DURATION_IN_MILLIS) {
            // Refreshes the metrics data.
            try {
                refreshMetricsData();
            } catch (Exception e) {
                loge("The metrics data cannot be refreshed.", e);
            }
        }

        Bundle bundle = new Bundle();

        bundle.putInt("Action", action);
        bundle.putInt("IsRecovered", isRecovered ? 1 : 0);
        bundle.putInt("Duration", duration);
        bundle.putInt("Reason", reason);
        bundle.putInt("DurationOfAction", durationOfAction);
        bundle.putInt("ValidationCount", validationCount);
        bundle.putInt("ActionValidationCount", actionValidationCount);
        bundle.putInt("PhoneId", mPhoneId);
        bundle.putInt("CarrierId", mCarrierId);
        bundle.putInt("MccMnc", mConvertedMccMnc);
        bundle.putInt("SignalStrength", mSignalStrength);
        bundle.putInt("Band", mBand);
        bundle.putInt("Rat", mRat);
        bundle.putInt("IsOpportunistic", mIsOpportunistic ? 1 : 0);
        bundle.putInt("IsMultiSim", mIsMultiSim ? 1 : 0);
        bundle.putInt("NetworkRegState", mNetworkRegState);
        bundle.putInt("OtherSignalStrength", mOtherSignalStrength);
        bundle.putInt("OtherNetworkRegState", mOtherNetworkRegState);
        bundle.putInt("InternetLinkStatus", mInternetLinkStatus);
        bundle.putInt("LinkDownBandwidthKbps", mLinkDownBandwidthKbps);
        bundle.putInt("LinkUpBandwidthKbps", mLinkUpBandwidthKbps);
        bundle.putInt("NetworkProbesResult", mNetworkProbesResult);
        bundle.putInt("NetworkProbesType", mNetworkProbesType);
        bundle.putInt("NetworkValidationResult", mNetworkValidationResult);
        bundle.putInt("TcpMetricsCollectionPeriodMillis", mTcpMetricsCollectionPeriodMillis);
        bundle.putInt("TcpPacketFailRate", mTcpPacketFailRate);
        bundle.putInt("DnsConsecutiveTimeouts", mDnsConsecutiveTimeouts);
        return bundle;
    }

    private void log(@NonNull String s) {
        Rlog.i(mTag, s);
    }

    private void logd(@NonNull String s) {
        Rlog.d(mTag, s);
    }

    private void loge(@NonNull String s) {
        Rlog.e(mTag, s);
    }

    private void loge(@NonNull String s, Throwable tr) {
        Rlog.e(mTag, s, tr);
    }

    @Override
    public String toString() {
        return "DataStallRecoveryStats {"
            + "Phone id:"
            + mPhoneId
            + ", Signal strength:"
            + mSignalStrength
            + ", Band:" + mBand
            + ", RAT:" + mRat
            + ", Opportunistic:"
            + mIsOpportunistic
            + ", Multi-SIM:"
            + mIsMultiSim
            + ", Network reg state:"
            + mNetworkRegState
            + ", Other signal strength:"
            + mOtherSignalStrength
            + ", Other network reg state:"
            + mOtherNetworkRegState
            + ", Link status:"
            + mInternetLinkStatus
            + ", Link down bandwidth:"
            + mLinkDownBandwidthKbps
            + ", Link up bandwidth:"
            + mLinkUpBandwidthKbps
            + "}";
    }
}
