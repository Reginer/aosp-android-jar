/*
 * Copyright (C) 2006 The Android Open Source Project
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

import static com.android.internal.telephony.RILConstants.*;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.hardware.radio.V1_0.IRadio;
import android.hardware.radio.V1_0.RadioError;
import android.hardware.radio.V1_0.RadioIndicationType;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.RadioResponseType;
import android.net.KeepalivePacketData;
import android.net.LinkProperties;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.HwBinder;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.provider.Settings;
import android.sysprop.TelephonyProperties;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.CarrierRestrictionRules;
import android.telephony.CellInfo;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthTdscdma;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.ClientRequestStats;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.ModemActivityInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.RadioAccessFamily;
import android.telephony.RadioAccessSpecifier;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SignalThresholdInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyHistogram;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.PrefNetworkMode;
import android.telephony.data.DataProfile;
import android.telephony.data.NetworkSliceInfo;
import android.telephony.data.TrafficDescriptor;
import android.telephony.emergency.EmergencyNumber;
import android.text.TextUtils;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.cdma.CdmaInformationRecords;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.metrics.ModemRestartStats;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.nano.TelephonyProto.SmsSession;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.PersoSubState;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.SimPhonebookRecord;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RIL implementation of the CommandsInterface.
 *
 * {@hide}
 */
public class RIL extends BaseCommands implements CommandsInterface {
    static final String RILJ_LOG_TAG = "RILJ";
    static final String RILJ_WAKELOCK_TAG = "*telephony-radio*";
    // Have a separate wakelock instance for Ack
    static final String RILJ_ACK_WAKELOCK_NAME = "RILJ_ACK_WL";
    static final boolean RILJ_LOGD = true;
    static final boolean RILJ_LOGV = false; // STOPSHIP if true
    static final int RIL_HISTOGRAM_BUCKET_COUNT = 5;

    /**
     * Wake lock timeout should be longer than the longest timeout in
     * the vendor ril.
     */
    private static final int DEFAULT_WAKE_LOCK_TIMEOUT_MS = 60000;

    // Wake lock default timeout associated with ack
    private static final int DEFAULT_ACK_WAKE_LOCK_TIMEOUT_MS = 200;

    private static final int DEFAULT_BLOCKING_MESSAGE_RESPONSE_TIMEOUT_MS = 2000;

    // Variables used to differentiate ack messages from request while calling clearWakeLock()
    public static final int INVALID_WAKELOCK = -1;
    public static final int FOR_WAKELOCK = 0;
    public static final int FOR_ACK_WAKELOCK = 1;
    private final ClientWakelockTracker mClientWakelockTracker = new ClientWakelockTracker();

    /** @hide */
    public static final HalVersion RADIO_HAL_VERSION_UNKNOWN = HalVersion.UNKNOWN;

    /** @hide */
    public static final HalVersion RADIO_HAL_VERSION_1_0 = new HalVersion(1, 0);

    /** @hide */
    public static final HalVersion RADIO_HAL_VERSION_1_1 = new HalVersion(1, 1);

    /** @hide */
    public static final HalVersion RADIO_HAL_VERSION_1_2 = new HalVersion(1, 2);

    /** @hide */
    public static final HalVersion RADIO_HAL_VERSION_1_3 = new HalVersion(1, 3);

    /** @hide */
    public static final HalVersion RADIO_HAL_VERSION_1_4 = new HalVersion(1, 4);

    /** @hide */
    public static final HalVersion RADIO_HAL_VERSION_1_5 = new HalVersion(1, 5);

    /** @hide */
    public static final HalVersion RADIO_HAL_VERSION_1_6 = new HalVersion(1, 6);

    /** @hide */
    public static final HalVersion RADIO_HAL_VERSION_2_0 = new HalVersion(2, 0);

    // IRadio version
    private HalVersion mRadioVersion = RADIO_HAL_VERSION_UNKNOWN;

    //***** Instance Variables

    @UnsupportedAppUsage
    @VisibleForTesting
    public final WakeLock mWakeLock;           // Wake lock associated with request/response
    @VisibleForTesting
    public final WakeLock mAckWakeLock;        // Wake lock associated with ack sent
    final int mWakeLockTimeout;         // Timeout associated with request/response
    final int mAckWakeLockTimeout;      // Timeout associated with ack sent
    // The number of wakelock requests currently active.  Don't release the lock
    // until dec'd to 0
    int mWakeLockCount;

    // Variables used to identify releasing of WL on wakelock timeouts
    volatile int mWlSequenceNum = 0;
    volatile int mAckWlSequenceNum = 0;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    SparseArray<RILRequest> mRequestList = new SparseArray<>();
    static SparseArray<TelephonyHistogram> sRilTimeHistograms = new SparseArray<>();

    Object[] mLastNITZTimeInfo;

    int mLastRadioPowerResult = RadioError.NONE;

    boolean mIsRadioProxyInitialized = false;

    // When we are testing emergency calls using ril.test.emergencynumber, this will trigger test
    // ECbM when the call is ended.
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    AtomicBoolean mTestingEmergencyCall = new AtomicBoolean(false);

    final Integer mPhoneId;

    private static final String PROPERTY_IS_VONR_ENABLED = "persist.radio.is_vonr_enabled_";

    static final int RADIO_SERVICE = 0;
    static final int DATA_SERVICE = 1;
    static final int MESSAGING_SERVICE = 2;
    static final int MODEM_SERVICE = 3;
    static final int NETWORK_SERVICE = 4;
    static final int SIM_SERVICE = 5;
    static final int VOICE_SERVICE = 6;
    static final int MIN_SERVICE_IDX = RADIO_SERVICE;
    static final int MAX_SERVICE_IDX = VOICE_SERVICE;

    /**
     * An array of sets that records if services are disabled in the HAL for a specific phone ID
     * slot to avoid further getService requests for that service. See XXX_SERVICE for the indices.
     * RADIO_SERVICE is the HIDL IRadio service, and mDisabledRadioServices.get(RADIO_SERVICE)
     * will return a set of all phone ID slots that are disabled for IRadio.
     */
    private final SparseArray<Set<Integer>> mDisabledRadioServices = new SparseArray<>();

    /* default work source which will blame phone process */
    private WorkSource mRILDefaultWorkSource;

    /* Worksource containing all applications causing wakelock to be held */
    private WorkSource mActiveWakelockWorkSource;

    /** Telephony metrics instance for logging metrics event */
    private TelephonyMetrics mMetrics = TelephonyMetrics.getInstance();
    /** Radio bug detector instance */
    private RadioBugDetector mRadioBugDetector = null;

    private boolean mIsCellularSupported;
    private RadioResponse mRadioResponse;
    private RadioIndication mRadioIndication;
    private volatile IRadio mRadioProxy = null;
    private DataResponse mDataResponse;
    private DataIndication mDataIndication;
    private MessagingResponse mMessagingResponse;
    private MessagingIndication mMessagingIndication;
    private ModemResponse mModemResponse;
    private ModemIndication mModemIndication;
    private NetworkResponse mNetworkResponse;
    private NetworkIndication mNetworkIndication;
    private SimResponse mSimResponse;
    private SimIndication mSimIndication;
    private VoiceResponse mVoiceResponse;
    private VoiceIndication mVoiceIndication;
    private SparseArray<RadioServiceProxy> mServiceProxies = new SparseArray<>();
    private final SparseArray<BinderServiceDeathRecipient> mDeathRecipients = new SparseArray<>();
    private final SparseArray<AtomicLong> mServiceCookies = new SparseArray<>();
    private final RadioProxyDeathRecipient mRadioProxyDeathRecipient;
    final RilHandler mRilHandler;
    private MockModem mMockModem;

    // Thread-safe HashMap to map from RIL_REQUEST_XXX constant to HalVersion.
    // This is for Radio HAL Fallback Compatibility feature. When a RIL request
    // is received, the HAL method from the mapping HalVersion here (if present),
    // instead of the latest HalVersion, will be invoked.
    private final ConcurrentHashMap<Integer, HalVersion> mCompatOverrides =
            new ConcurrentHashMap<>();

    //***** Events
    static final int EVENT_WAKE_LOCK_TIMEOUT = 2;
    static final int EVENT_ACK_WAKE_LOCK_TIMEOUT = 4;
    static final int EVENT_BLOCKING_RESPONSE_TIMEOUT = 5;
    static final int EVENT_RADIO_PROXY_DEAD = 6;
    static final int EVENT_AIDL_PROXY_DEAD = 7;

    //***** Constants

    static final String[] HIDL_SERVICE_NAME = {"slot1", "slot2", "slot3"};

    public static List<TelephonyHistogram> getTelephonyRILTimingHistograms() {
        List<TelephonyHistogram> list;
        synchronized (sRilTimeHistograms) {
            list = new ArrayList<>(sRilTimeHistograms.size());
            for (int i = 0; i < sRilTimeHistograms.size(); i++) {
                TelephonyHistogram entry = new TelephonyHistogram(sRilTimeHistograms.valueAt(i));
                list.add(entry);
            }
        }
        return list;
    }

    /** The handler used to handle the internal event of RIL. */
    @VisibleForTesting
    public class RilHandler extends Handler {

        //***** Handler implementation
        @Override
        public void handleMessage(Message msg) {
            RILRequest rr;

            switch (msg.what) {
                case EVENT_WAKE_LOCK_TIMEOUT:
                    // Haven't heard back from the last request.  Assume we're
                    // not getting a response and  release the wake lock.

                    // The timer of WAKE_LOCK_TIMEOUT is reset with each
                    // new send request. So when WAKE_LOCK_TIMEOUT occurs
                    // all requests in mRequestList already waited at
                    // least DEFAULT_WAKE_LOCK_TIMEOUT_MS but no response.
                    //
                    // Note: Keep mRequestList so that delayed response
                    // can still be handled when response finally comes.

                    synchronized (mRequestList) {
                        if (msg.arg1 == mWlSequenceNum && clearWakeLock(FOR_WAKELOCK)) {
                            if (mRadioBugDetector != null) {
                                mRadioBugDetector.processWakelockTimeout();
                            }
                            if (RILJ_LOGD) {
                                int count = mRequestList.size();
                                Rlog.d(RILJ_LOG_TAG, "WAKE_LOCK_TIMEOUT " +
                                        " mRequestList=" + count);
                                for (int i = 0; i < count; i++) {
                                    rr = mRequestList.valueAt(i);
                                    Rlog.d(RILJ_LOG_TAG, i + ": [" + rr.mSerial + "] "
                                            + RILUtils.requestToString(rr.mRequest));
                                }
                            }
                        }
                    }
                    break;

                case EVENT_ACK_WAKE_LOCK_TIMEOUT:
                    if (msg.arg1 == mAckWlSequenceNum && clearWakeLock(FOR_ACK_WAKELOCK)) {
                        if (RILJ_LOGV) {
                            Rlog.d(RILJ_LOG_TAG, "ACK_WAKE_LOCK_TIMEOUT");
                        }
                    }
                    break;

                case EVENT_BLOCKING_RESPONSE_TIMEOUT:
                    int serial = (int) msg.obj;
                    rr = findAndRemoveRequestFromList(serial);
                    // If the request has already been processed, do nothing
                    if (rr == null) {
                        break;
                    }

                    // Build a response if expected
                    if (rr.mResult != null) {
                        Object timeoutResponse = getResponseForTimedOutRILRequest(rr);
                        AsyncResult.forMessage(rr.mResult, timeoutResponse, null);
                        rr.mResult.sendToTarget();
                        mMetrics.writeOnRilTimeoutResponse(mPhoneId, rr.mSerial, rr.mRequest);
                    }

                    decrementWakeLock(rr);
                    rr.release();
                    break;

                case EVENT_RADIO_PROXY_DEAD:
                    int service = msg.arg1;
                    riljLog("handleMessage: EVENT_RADIO_PROXY_DEAD cookie = " + msg.obj
                            + ", service = " + serviceToString(service) + ", service cookie = "
                            + mServiceCookies.get(service));
                    if ((long) msg.obj == mServiceCookies.get(service).get()) {
                        mIsRadioProxyInitialized = false;
                        resetProxyAndRequestList(service);
                    }
                    break;

                case EVENT_AIDL_PROXY_DEAD:
                    int aidlService = msg.arg1;
                    AtomicLong obj = (AtomicLong) msg.obj;
                    riljLog("handleMessage: EVENT_AIDL_PROXY_DEAD cookie = " + msg.obj
                            + ", service = " + serviceToString(aidlService) + ", cookie = "
                            + mServiceCookies.get(aidlService));
                    if (obj.get() == mServiceCookies.get(aidlService).get()) {
                        mIsRadioProxyInitialized = false;
                        resetProxyAndRequestList(aidlService);
                    }
                    break;
            }
        }
    }

    /** Return RadioBugDetector instance for testing. */
    @VisibleForTesting
    public RadioBugDetector getRadioBugDetector() {
        if (mRadioBugDetector == null) {
            mRadioBugDetector = new RadioBugDetector(mContext, mPhoneId);
        }
        return mRadioBugDetector;
    }

    /**
     * In order to prevent calls to Telephony from waiting indefinitely
     * low-latency blocking calls will eventually time out. In the event of
     * a timeout, this function generates a response that is returned to the
     * higher layers to unblock the call. This is in lieu of a meaningful
     * response.
     * @param rr The RIL Request that has timed out.
     * @return A default object, such as the one generated by a normal response
     * that is returned to the higher layers.
     **/
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private static Object getResponseForTimedOutRILRequest(RILRequest rr) {
        if (rr == null ) return null;

        Object timeoutResponse = null;
        switch(rr.mRequest) {
            case RIL_REQUEST_GET_ACTIVITY_INFO:
                timeoutResponse = new ModemActivityInfo(
                        0, 0, 0, new int [ModemActivityInfo.getNumTxPowerLevels()], 0);
                break;
        };
        return timeoutResponse;
    }

    final class RadioProxyDeathRecipient implements HwBinder.DeathRecipient {
        @Override
        public void serviceDied(long cookie) {
            // Deal with service going away
            riljLog("serviceDied");
            mRilHandler.sendMessage(mRilHandler.obtainMessage(EVENT_RADIO_PROXY_DEAD, RADIO_SERVICE,
                    0 /* ignored arg2 */, cookie));
        }
    }

    private final class BinderServiceDeathRecipient implements IBinder.DeathRecipient {
        private IBinder mBinder;
        private final int mService;

        BinderServiceDeathRecipient(int service) {
            mService = service;
        }

        public void linkToDeath(IBinder service) throws RemoteException {
            if (service != null) {
                mBinder = service;
                mBinder.linkToDeath(this, (int) mServiceCookies.get(mService).incrementAndGet());
            }
        }

        public synchronized void unlinkToDeath() {
            if (mBinder != null) {
                mBinder.unlinkToDeath(this, 0);
                mBinder = null;
            }
        }

        @Override
        public void binderDied() {
            riljLog("Service " + serviceToString(mService) + " has died.");
            mRilHandler.sendMessage(mRilHandler.obtainMessage(EVENT_AIDL_PROXY_DEAD, mService,
                    0 /* ignored arg2 */, mServiceCookies.get(mService)));
            unlinkToDeath();
        }
    }

    private synchronized void resetProxyAndRequestList(int service) {
        if (service == RADIO_SERVICE) {
            mRadioProxy = null;
        } else {
            mServiceProxies.get(service).clear();
        }

        // Increment the cookie so that death notification can be ignored
        mServiceCookies.get(service).incrementAndGet();

        // TODO: If a service doesn't exist or is unimplemented, it shouldn't cause the radio to
        //  become unavailable for all other services
        setRadioState(TelephonyManager.RADIO_POWER_UNAVAILABLE, true /* forceNotifyRegistrants */);

        RILRequest.resetSerial();
        // Clear request list on close
        clearRequestList(RADIO_NOT_AVAILABLE, false);

        if (service == RADIO_SERVICE) {
            getRadioProxy(null);
        } else {
            getRadioServiceProxy(service, null);
        }
    }

    /**
     * Request to enable/disable the mock modem service.
     * This is invoked from shell commands during CTS testing only.
     *
     * @param serviceName the service name we want to bind to
     */
    public boolean setModemService(String serviceName) {
        boolean serviceBound = true;

        if (serviceName != null) {
            riljLog("Binding to MockModemService");
            mMockModem = null;

            mMockModem = new MockModem(mContext, serviceName, mPhoneId);
            if (mMockModem == null) {
                riljLoge("MockModem create fail.");
                return false;
            }

            // Disable HIDL service
            if (mRadioProxy != null) {
                riljLog("Disable HIDL service");
                mDisabledRadioServices.get(RADIO_SERVICE).add(mPhoneId);
            }

            mMockModem.bindAllMockModemService();

            for (int service = MIN_SERVICE_IDX; service <= MAX_SERVICE_IDX; service++) {
                if (service == RADIO_SERVICE) continue;

                int retryCount = 0;
                IBinder binder;
                do {
                    binder = mMockModem.getServiceBinder(service);

                    retryCount++;
                    if (binder == null) {
                        riljLog("Retry(" + retryCount + ") Service " + serviceToString(service));
                        try {
                            Thread.sleep(MockModem.BINDER_RETRY_MILLIS);
                        } catch (InterruptedException e) {
                        }
                    }
                } while ((binder == null) && (retryCount < MockModem.BINDER_MAX_RETRY));

                if (binder == null) {
                    riljLoge("Service " + serviceToString(service) + " bind fail");
                    serviceBound = false;
                    break;
                }
            }

            if (serviceBound) {
                mIsRadioProxyInitialized = false;
                for (int service = MIN_SERVICE_IDX; service <= MAX_SERVICE_IDX; service++) {
                    resetProxyAndRequestList(service);
                }
            }
        }

        if ((serviceName == null) || (!serviceBound)) {
            if (serviceBound) riljLog("Unbinding to MockModemService");

            if (mDisabledRadioServices.get(RADIO_SERVICE).contains(mPhoneId)) {
                mDisabledRadioServices.get(RADIO_SERVICE).clear();
            }

            if (mMockModem != null) {
                mRadioVersion = RADIO_HAL_VERSION_UNKNOWN;
                mMockModem = null;
                for (int service = MIN_SERVICE_IDX; service <= MAX_SERVICE_IDX; service++) {
                    resetProxyAndRequestList(service);
                }
            }
        }

        return serviceBound;
    }

    /**
     * Get current bound service in Radio Module
     */
    public String getModemService() {
        if (mMockModem != null) {
            return mMockModem.getServiceName();
        } else {
            return "default";
        }
    }

    /** Set a radio HAL fallback compatibility override. */
    @VisibleForTesting
    public void setCompatVersion(int rilRequest, @NonNull HalVersion halVersion) {
        HalVersion oldVersion = getCompatVersion(rilRequest);
        // Do not allow to set same or greater verions
        if (oldVersion != null && halVersion.greaterOrEqual(oldVersion)) {
            riljLoge("setCompatVersion with equal or greater one, ignored, halVerion=" + halVersion
                    + ", oldVerion=" + oldVersion);
            return;
        }
        mCompatOverrides.put(rilRequest, halVersion);
    }

    /** Get a radio HAL fallback compatibility override, or null if not exist. */
    @VisibleForTesting
    public @Nullable HalVersion getCompatVersion(int rilRequest) {
        return mCompatOverrides.getOrDefault(rilRequest, null);
    }

    /** Returns a {@link IRadio} instance or null if the service is not available. */
    @VisibleForTesting
    public synchronized IRadio getRadioProxy(Message result) {
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_2_0)) return null;
        if (!SubscriptionManager.isValidPhoneId(mPhoneId)) return null;
        if (!mIsCellularSupported) {
            if (RILJ_LOGV) riljLog("getRadioProxy: Not calling getService(): wifi-only");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(RADIO_NOT_AVAILABLE));
                result.sendToTarget();
            }
            return null;
        }

        if (mRadioProxy != null) {
            return mRadioProxy;
        }

        try {
            if (mDisabledRadioServices.get(RADIO_SERVICE).contains(mPhoneId)) {
                riljLoge("getRadioProxy: mRadioProxy for " + HIDL_SERVICE_NAME[mPhoneId]
                        + " is disabled");
            } else {
                try {
                    mRadioProxy = android.hardware.radio.V1_6.IRadio.getService(
                            HIDL_SERVICE_NAME[mPhoneId], true);
                    mRadioVersion = RADIO_HAL_VERSION_1_6;
                } catch (NoSuchElementException e) {
                }

                if (mRadioProxy == null) {
                    try {
                        mRadioProxy = android.hardware.radio.V1_5.IRadio.getService(
                                HIDL_SERVICE_NAME[mPhoneId], true);
                        mRadioVersion = RADIO_HAL_VERSION_1_5;
                    } catch (NoSuchElementException e) {
                    }
                }

                if (mRadioProxy == null) {
                    try {
                        mRadioProxy = android.hardware.radio.V1_4.IRadio.getService(
                                HIDL_SERVICE_NAME[mPhoneId], true);
                        mRadioVersion = RADIO_HAL_VERSION_1_4;
                    } catch (NoSuchElementException e) {
                    }
                }

                if (mRadioProxy == null) {
                    try {
                        mRadioProxy = android.hardware.radio.V1_3.IRadio.getService(
                                HIDL_SERVICE_NAME[mPhoneId], true);
                        mRadioVersion = RADIO_HAL_VERSION_1_3;
                    } catch (NoSuchElementException e) {
                    }
                }

                if (mRadioProxy == null) {
                    try {
                        mRadioProxy = android.hardware.radio.V1_2.IRadio.getService(
                                HIDL_SERVICE_NAME[mPhoneId], true);
                        mRadioVersion = RADIO_HAL_VERSION_1_2;
                    } catch (NoSuchElementException e) {
                    }
                }

                if (mRadioProxy == null) {
                    try {
                        mRadioProxy = android.hardware.radio.V1_1.IRadio.getService(
                                HIDL_SERVICE_NAME[mPhoneId], true);
                        mRadioVersion = RADIO_HAL_VERSION_1_1;
                    } catch (NoSuchElementException e) {
                    }
                }

                if (mRadioProxy == null) {
                    try {
                        mRadioProxy = android.hardware.radio.V1_0.IRadio.getService(
                                HIDL_SERVICE_NAME[mPhoneId], true);
                        mRadioVersion = RADIO_HAL_VERSION_1_0;
                    } catch (NoSuchElementException e) {
                    }
                }

                if (mRadioProxy != null) {
                    if (!mIsRadioProxyInitialized) {
                        mIsRadioProxyInitialized = true;
                        mRadioProxy.linkToDeath(mRadioProxyDeathRecipient,
                                mServiceCookies.get(RADIO_SERVICE).incrementAndGet());
                        mRadioProxy.setResponseFunctions(mRadioResponse, mRadioIndication);
                    }
                } else {
                    mDisabledRadioServices.get(RADIO_SERVICE).add(mPhoneId);
                    riljLoge("getRadioProxy: set mRadioProxy for "
                            + HIDL_SERVICE_NAME[mPhoneId] + " as disabled");
                }
            }
        } catch (RemoteException e) {
            mRadioProxy = null;
            riljLoge("RadioProxy getService/setResponseFunctions: " + e);
        }

        if (mRadioProxy == null) {
            // getService() is a blocking call, so this should never happen
            riljLoge("getRadioProxy: mRadioProxy == null");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(RADIO_NOT_AVAILABLE));
                result.sendToTarget();
            }
        }

        return mRadioProxy;
    }

    /**
     * Returns a {@link RadioDataProxy}, {@link RadioMessagingProxy}, {@link RadioModemProxy},
     * {@link RadioNetworkProxy}, {@link RadioSimProxy}, {@link RadioVoiceProxy}, or an empty {@link RadioServiceProxy}
     * if the service is not available.
     */
    @NonNull
    public <T extends RadioServiceProxy> T getRadioServiceProxy(Class<T> serviceClass,
            Message result) {
        if (serviceClass == RadioDataProxy.class) {
            return (T) getRadioServiceProxy(DATA_SERVICE, result);
        }
        if (serviceClass == RadioMessagingProxy.class) {
            return (T) getRadioServiceProxy(MESSAGING_SERVICE, result);
        }
        if (serviceClass == RadioModemProxy.class) {
            return (T) getRadioServiceProxy(MODEM_SERVICE, result);
        }
        if (serviceClass == RadioNetworkProxy.class) {
            return (T) getRadioServiceProxy(NETWORK_SERVICE, result);
        }
        if (serviceClass == RadioSimProxy.class) {
            return (T) getRadioServiceProxy(SIM_SERVICE, result);
        }
        if (serviceClass == RadioVoiceProxy.class) {
            return (T) getRadioServiceProxy(VOICE_SERVICE, result);
        }
        riljLoge("getRadioServiceProxy: unrecognized " + serviceClass);
        return null;
    }

    /**
     * Returns a {@link RadioServiceProxy}, which is empty if the service is not available.
     * For RADIO_SERVICE, use {@link #getRadioProxy} instead, as this will always return null.
     */
    @VisibleForTesting
    @NonNull
    public synchronized RadioServiceProxy getRadioServiceProxy(int service, Message result) {
        if (!SubscriptionManager.isValidPhoneId(mPhoneId)) return mServiceProxies.get(service);
        if (!mIsCellularSupported) {
            if (RILJ_LOGV) riljLog("getRadioServiceProxy: Not calling getService(): wifi-only");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(RADIO_NOT_AVAILABLE));
                result.sendToTarget();
            }
            return mServiceProxies.get(service);
        }

        RadioServiceProxy serviceProxy = mServiceProxies.get(service);
        if (!serviceProxy.isEmpty()) {
            return serviceProxy;
        }

        try {
            if (mDisabledRadioServices.get(service).contains(mPhoneId)) {
                riljLoge("getRadioServiceProxy: " + serviceToString(service) + " for "
                        + HIDL_SERVICE_NAME[mPhoneId] + " is disabled");
            } else {
                IBinder binder;
                switch (service) {
                    case DATA_SERVICE:
                        if (mMockModem == null) {
                            binder = ServiceManager.waitForDeclaredService(
                                    android.hardware.radio.data.IRadioData.DESCRIPTOR + "/"
                                            + HIDL_SERVICE_NAME[mPhoneId]);
                        } else {
                            binder = mMockModem.getServiceBinder(DATA_SERVICE);
                        }
                        if (binder != null) {
                            mRadioVersion = RADIO_HAL_VERSION_2_0;
                            ((RadioDataProxy) serviceProxy).setAidl(mRadioVersion,
                                    android.hardware.radio.data.IRadioData.Stub.asInterface(
                                            binder));
                        }
                        break;
                    case MESSAGING_SERVICE:
                        if (mMockModem == null) {
                            binder = ServiceManager.waitForDeclaredService(
                                    android.hardware.radio.messaging.IRadioMessaging.DESCRIPTOR
                                            + "/" + HIDL_SERVICE_NAME[mPhoneId]);
                        } else {
                            binder = mMockModem.getServiceBinder(MESSAGING_SERVICE);
                        }
                        if (binder != null) {
                            mRadioVersion = RADIO_HAL_VERSION_2_0;
                            ((RadioMessagingProxy) serviceProxy).setAidl(mRadioVersion,
                                    android.hardware.radio.messaging.IRadioMessaging.Stub
                                            .asInterface(binder));
                        }
                        break;
                    case MODEM_SERVICE:
                        if (mMockModem == null) {
                            binder = ServiceManager.waitForDeclaredService(
                                    android.hardware.radio.modem.IRadioModem.DESCRIPTOR + "/"
                                            + HIDL_SERVICE_NAME[mPhoneId]);
                        } else {
                            binder = mMockModem.getServiceBinder(MODEM_SERVICE);
                        }
                        if (binder != null) {
                            mRadioVersion = RADIO_HAL_VERSION_2_0;
                            ((RadioModemProxy) serviceProxy).setAidl(mRadioVersion,
                                    android.hardware.radio.modem.IRadioModem.Stub
                                            .asInterface(binder));
                        }
                        break;
                    case NETWORK_SERVICE:
                        if (mMockModem == null) {
                            binder = ServiceManager.waitForDeclaredService(
                                    android.hardware.radio.network.IRadioNetwork.DESCRIPTOR + "/"
                                            + HIDL_SERVICE_NAME[mPhoneId]);
                        } else {
                            binder = mMockModem.getServiceBinder(NETWORK_SERVICE);
                        }
                        if (binder != null) {
                            mRadioVersion = RADIO_HAL_VERSION_2_0;
                            ((RadioNetworkProxy) serviceProxy).setAidl(mRadioVersion,
                                    android.hardware.radio.network.IRadioNetwork.Stub
                                            .asInterface(binder));
                        }
                        break;
                    case SIM_SERVICE:
                        if (mMockModem == null) {
                            binder = ServiceManager.waitForDeclaredService(
                                    android.hardware.radio.sim.IRadioSim.DESCRIPTOR + "/"
                                            + HIDL_SERVICE_NAME[mPhoneId]);
                        } else {
                            binder = mMockModem.getServiceBinder(SIM_SERVICE);
                        }
                        if (binder != null) {
                            mRadioVersion = RADIO_HAL_VERSION_2_0;
                            ((RadioSimProxy) serviceProxy).setAidl(mRadioVersion,
                                    android.hardware.radio.sim.IRadioSim.Stub
                                            .asInterface(binder));
                        }
                        break;
                    case VOICE_SERVICE:
                        if (mMockModem == null) {
                            binder = ServiceManager.waitForDeclaredService(
                                    android.hardware.radio.voice.IRadioVoice.DESCRIPTOR + "/"
                                            + HIDL_SERVICE_NAME[mPhoneId]);
                        } else {
                            binder = mMockModem.getServiceBinder(VOICE_SERVICE);
                        }
                        if (binder != null) {
                            mRadioVersion = RADIO_HAL_VERSION_2_0;
                            ((RadioVoiceProxy) serviceProxy).setAidl(mRadioVersion,
                                    android.hardware.radio.voice.IRadioVoice.Stub
                                            .asInterface(binder));
                        }
                        break;
                }

                if (serviceProxy.isEmpty() && mRadioVersion.less(RADIO_HAL_VERSION_2_0)) {
                    try {
                        mRadioVersion = RADIO_HAL_VERSION_1_6;
                        serviceProxy.setHidl(mRadioVersion,
                                android.hardware.radio.V1_6.IRadio.getService(
                                        HIDL_SERVICE_NAME[mPhoneId], true));
                    } catch (NoSuchElementException e) {
                    }
                }

                if (serviceProxy.isEmpty() && mRadioVersion.less(RADIO_HAL_VERSION_2_0)) {
                    try {
                        mRadioVersion = RADIO_HAL_VERSION_1_5;
                        serviceProxy.setHidl(mRadioVersion,
                                android.hardware.radio.V1_5.IRadio.getService(
                                        HIDL_SERVICE_NAME[mPhoneId], true));
                    } catch (NoSuchElementException e) {
                    }
                }

                if (serviceProxy.isEmpty() && mRadioVersion.less(RADIO_HAL_VERSION_2_0)) {
                    try {
                        mRadioVersion = RADIO_HAL_VERSION_1_4;
                        serviceProxy.setHidl(mRadioVersion,
                                android.hardware.radio.V1_4.IRadio.getService(
                                        HIDL_SERVICE_NAME[mPhoneId], true));
                    } catch (NoSuchElementException e) {
                    }
                }

                if (serviceProxy.isEmpty() && mRadioVersion.less(RADIO_HAL_VERSION_2_0)) {
                    try {
                        mRadioVersion = RADIO_HAL_VERSION_1_3;
                        serviceProxy.setHidl(mRadioVersion,
                                android.hardware.radio.V1_3.IRadio.getService(
                                        HIDL_SERVICE_NAME[mPhoneId], true));
                    } catch (NoSuchElementException e) {
                    }
                }

                if (serviceProxy.isEmpty() && mRadioVersion.less(RADIO_HAL_VERSION_2_0)) {
                    try {
                        mRadioVersion = RADIO_HAL_VERSION_1_2;
                        serviceProxy.setHidl(mRadioVersion,
                                android.hardware.radio.V1_2.IRadio.getService(
                                        HIDL_SERVICE_NAME[mPhoneId], true));
                    } catch (NoSuchElementException e) {
                    }
                }

                if (serviceProxy.isEmpty() && mRadioVersion.less(RADIO_HAL_VERSION_2_0)) {
                    try {
                        mRadioVersion = RADIO_HAL_VERSION_1_1;
                        serviceProxy.setHidl(mRadioVersion,
                                android.hardware.radio.V1_1.IRadio.getService(
                                        HIDL_SERVICE_NAME[mPhoneId], true));
                    } catch (NoSuchElementException e) {
                    }
                }

                if (serviceProxy.isEmpty() && mRadioVersion.less(RADIO_HAL_VERSION_2_0)) {
                    try {
                        mRadioVersion = RADIO_HAL_VERSION_1_0;
                        serviceProxy.setHidl(mRadioVersion,
                                android.hardware.radio.V1_0.IRadio.getService(
                                        HIDL_SERVICE_NAME[mPhoneId], true));
                    } catch (NoSuchElementException e) {
                    }
                }

                if (!serviceProxy.isEmpty()) {
                    if (serviceProxy.isAidl()) {
                        switch (service) {
                            case DATA_SERVICE:
                                mDeathRecipients.get(service).linkToDeath(
                                        ((RadioDataProxy) serviceProxy).getAidl().asBinder());
                                ((RadioDataProxy) serviceProxy).getAidl().setResponseFunctions(
                                        mDataResponse, mDataIndication);
                                break;
                            case MESSAGING_SERVICE:
                                mDeathRecipients.get(service).linkToDeath(
                                        ((RadioMessagingProxy) serviceProxy).getAidl().asBinder());
                                ((RadioMessagingProxy) serviceProxy).getAidl().setResponseFunctions(
                                        mMessagingResponse, mMessagingIndication);
                                break;
                            case MODEM_SERVICE:
                                mDeathRecipients.get(service).linkToDeath(
                                        ((RadioModemProxy) serviceProxy).getAidl().asBinder());
                                ((RadioModemProxy) serviceProxy).getAidl().setResponseFunctions(
                                        mModemResponse, mModemIndication);
                                break;
                            case NETWORK_SERVICE:
                                mDeathRecipients.get(service).linkToDeath(
                                        ((RadioNetworkProxy) serviceProxy).getAidl().asBinder());
                                ((RadioNetworkProxy) serviceProxy).getAidl().setResponseFunctions(
                                        mNetworkResponse, mNetworkIndication);
                                break;
                            case SIM_SERVICE:
                                mDeathRecipients.get(service).linkToDeath(
                                        ((RadioSimProxy) serviceProxy).getAidl().asBinder());
                                ((RadioSimProxy) serviceProxy).getAidl().setResponseFunctions(
                                        mSimResponse, mSimIndication);
                                break;
                            case VOICE_SERVICE:
                                mDeathRecipients.get(service).linkToDeath(
                                        ((RadioVoiceProxy) serviceProxy).getAidl().asBinder());
                                ((RadioVoiceProxy) serviceProxy).getAidl().setResponseFunctions(
                                        mVoiceResponse, mVoiceIndication);
                                break;
                        }
                    } else {
                        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_2_0)) {
                            throw new AssertionError("serviceProxy shouldn't be HIDL with HAL 2.0");
                        }
                        if (!mIsRadioProxyInitialized) {
                            mIsRadioProxyInitialized = true;
                            serviceProxy.getHidl().linkToDeath(mRadioProxyDeathRecipient,
                                    mServiceCookies.get(service).incrementAndGet());
                            serviceProxy.getHidl().setResponseFunctions(
                                    mRadioResponse, mRadioIndication);
                        }
                    }
                } else {
                    mDisabledRadioServices.get(service).add(mPhoneId);
                    riljLoge("getRadioServiceProxy: set " + serviceToString(service) + " for "
                            + HIDL_SERVICE_NAME[mPhoneId] + " as disabled");
                }
            }
        } catch (RemoteException e) {
            serviceProxy.clear();
            riljLoge("ServiceProxy getService/setResponseFunctions: " + e);
        }

        if (serviceProxy.isEmpty()) {
            // getService() is a blocking call, so this should never happen
            riljLoge("getRadioServiceProxy: serviceProxy == null");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(RADIO_NOT_AVAILABLE));
                result.sendToTarget();
            }
        }

        return serviceProxy;
    }

    @Override
    public synchronized void onSlotActiveStatusChange(boolean active) {
        mIsRadioProxyInitialized = false;
        for (int service = MIN_SERVICE_IDX; service <= MAX_SERVICE_IDX; service++) {
            if (active) {
                // Try to connect to RIL services and set response functions.
                if (service == RADIO_SERVICE) {
                    getRadioProxy(null);
                } else {
                    getRadioServiceProxy(service, null);
                }
            } else {
                resetProxyAndRequestList(service);
            }
        }
    }

    //***** Constructors

    @UnsupportedAppUsage
    public RIL(Context context, int allowedNetworkTypes, int cdmaSubscription) {
        this(context, allowedNetworkTypes, cdmaSubscription, null);
    }

    @UnsupportedAppUsage
    public RIL(Context context, int allowedNetworkTypes, int cdmaSubscription, Integer instanceId) {
        this(context, allowedNetworkTypes, cdmaSubscription, instanceId, null);
    }

    @VisibleForTesting
    public RIL(Context context, int allowedNetworkTypes, int cdmaSubscription, Integer instanceId,
            SparseArray<RadioServiceProxy> proxies) {
        super(context);
        if (RILJ_LOGD) {
            riljLog("RIL: init allowedNetworkTypes=" + allowedNetworkTypes
                    + " cdmaSubscription=" + cdmaSubscription + ")");
        }

        mContext = context;
        mCdmaSubscription  = cdmaSubscription;
        mAllowedNetworkTypesBitmask = allowedNetworkTypes;
        mPhoneType = RILConstants.NO_PHONE;
        mPhoneId = instanceId == null ? 0 : instanceId;
        if (isRadioBugDetectionEnabled()) {
            mRadioBugDetector = new RadioBugDetector(context, mPhoneId);
        }
        try {
            if (isRadioVersion2_0()) mRadioVersion = RADIO_HAL_VERSION_2_0;
        } catch (SecurityException ex) {
            /* TODO(b/211920208): instead of the following workaround (guessing if we're in a test
             * based on proxies being populated), mock ServiceManager to not throw
             * SecurityException and return correct value based on what HAL we're testing. */
            if (proxies == null) throw ex;
        }

        TelephonyManager tm = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        mIsCellularSupported = tm.isVoiceCapable() || tm.isSmsCapable() || tm.isDataCapable();

        mRadioResponse = new RadioResponse(this);
        mRadioIndication = new RadioIndication(this);
        mDataResponse = new DataResponse(this);
        mDataIndication = new DataIndication(this);
        mMessagingResponse = new MessagingResponse(this);
        mMessagingIndication = new MessagingIndication(this);
        mModemResponse = new ModemResponse(this);
        mModemIndication = new ModemIndication(this);
        mNetworkResponse = new NetworkResponse(this);
        mNetworkIndication = new NetworkIndication(this);
        mSimResponse = new SimResponse(this);
        mSimIndication = new SimIndication(this);
        mVoiceResponse = new VoiceResponse(this);
        mVoiceIndication = new VoiceIndication(this);
        mRilHandler = new RilHandler();
        mRadioProxyDeathRecipient = new RadioProxyDeathRecipient();
        for (int service = MIN_SERVICE_IDX; service <= MAX_SERVICE_IDX; service++) {
            if (service != RADIO_SERVICE) {
                mDeathRecipients.put(service, new BinderServiceDeathRecipient(service));
            }
            mDisabledRadioServices.put(service, new HashSet<>());
            mServiceCookies.put(service, new AtomicLong(0));
        }
        if (proxies == null) {
            mServiceProxies.put(DATA_SERVICE, new RadioDataProxy());
            mServiceProxies.put(MESSAGING_SERVICE, new RadioMessagingProxy());
            mServiceProxies.put(MODEM_SERVICE, new RadioModemProxy());
            mServiceProxies.put(NETWORK_SERVICE, new RadioNetworkProxy());
            mServiceProxies.put(SIM_SERVICE, new RadioSimProxy());
            mServiceProxies.put(VOICE_SERVICE, new RadioVoiceProxy());
        } else {
            mServiceProxies = proxies;
        }

        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, RILJ_WAKELOCK_TAG);
        mWakeLock.setReferenceCounted(false);
        mAckWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, RILJ_ACK_WAKELOCK_NAME);
        mAckWakeLock.setReferenceCounted(false);
        mWakeLockTimeout = TelephonyProperties.wake_lock_timeout()
                .orElse(DEFAULT_WAKE_LOCK_TIMEOUT_MS);
        mAckWakeLockTimeout = TelephonyProperties.wake_lock_timeout()
                .orElse(DEFAULT_ACK_WAKE_LOCK_TIMEOUT_MS);
        mWakeLockCount = 0;
        mRILDefaultWorkSource = new WorkSource(context.getApplicationInfo().uid,
                context.getPackageName());
        mActiveWakelockWorkSource = new WorkSource();

        TelephonyDevController tdc = TelephonyDevController.getInstance();
        if (proxies == null) {
            // TelephonyDevController#registerRIL will call getHardwareConfig.
            // To prevent extra requests when running tests, only registerRIL when proxies is null
            tdc.registerRIL(this);
        }

        // Set radio callback; needed to set RadioIndication callback (should be done after
        // wakelock stuff is initialized above as callbacks are received on separate binder threads)
        for (int service = MIN_SERVICE_IDX; service <= MAX_SERVICE_IDX; service++) {
            if (service == RADIO_SERVICE) {
                getRadioProxy(null);
            } else {
                if (proxies == null) {
                    // Prevent telephony tests from calling the service
                    getRadioServiceProxy(service, null);
                }
            }
        }

        if (RILJ_LOGD) {
            riljLog("Radio HAL version: " + mRadioVersion);
        }
    }

    private boolean isRadioVersion2_0() {
        final String[] serviceNames = new String[] {
            android.hardware.radio.data.IRadioData.DESCRIPTOR,
            android.hardware.radio.messaging.IRadioMessaging.DESCRIPTOR,
            android.hardware.radio.modem.IRadioModem.DESCRIPTOR,
            android.hardware.radio.network.IRadioNetwork.DESCRIPTOR,
            android.hardware.radio.sim.IRadioSim.DESCRIPTOR,
            android.hardware.radio.voice.IRadioVoice.DESCRIPTOR,
        };
        for (String serviceName : serviceNames) {
            if (ServiceManager.isDeclared(serviceName + '/' + HIDL_SERVICE_NAME[mPhoneId])) {
                return true;
            }
        }
        return false;
    }

    private boolean isRadioBugDetectionEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ENABLE_RADIO_BUG_DETECTION, 1) != 0;
    }

    @Override
    public void setOnNITZTime(Handler h, int what, Object obj) {
        super.setOnNITZTime(h, what, obj);

        // Send the last NITZ time if we have it
        if (mLastNITZTimeInfo != null) {
            mNITZTimeRegistrant.notifyRegistrant(new AsyncResult(null, mLastNITZTimeInfo, null));
        }
    }

    private void addRequest(RILRequest rr) {
        acquireWakeLock(rr, FOR_WAKELOCK);
        synchronized (mRequestList) {
            rr.mStartTimeMs = SystemClock.elapsedRealtime();
            mRequestList.append(rr.mSerial, rr);
        }
    }

    private RILRequest obtainRequest(int request, Message result, WorkSource workSource) {
        RILRequest rr = RILRequest.obtain(request, result, workSource);
        addRequest(rr);
        return rr;
    }

    private RILRequest obtainRequest(int request, Message result, WorkSource workSource,
            Object... args) {
        RILRequest rr = RILRequest.obtain(request, result, workSource, args);
        addRequest(rr);
        return rr;
    }

    private void handleRadioProxyExceptionForRR(int service, String caller, Exception e) {
        riljLoge(caller + ": " + e);
        e.printStackTrace();
        mIsRadioProxyInitialized = false;
        resetProxyAndRequestList(service);
    }

    @Override
    public void getIccCardStatus(Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_SIM_STATUS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                simProxy.getIccCardStatus(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "getIccCardStatus", e);
            }
        }
    }

    @Override
    public void getIccSlotsStatus(Message result) {
        if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "getIccSlotsStatus: REQUEST_NOT_SUPPORTED");
        if (result != null) {
            AsyncResult.forMessage(result, null,
                    CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
            result.sendToTarget();
        }
    }

    @Override
    public void setLogicalToPhysicalSlotMapping(int[] physicalSlots, Message result) {
        if (RILJ_LOGD) {
            Rlog.d(RILJ_LOG_TAG, "setLogicalToPhysicalSlotMapping: REQUEST_NOT_SUPPORTED");
        }
        if (result != null) {
            AsyncResult.forMessage(result, null,
                    CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
            result.sendToTarget();
        }
    }

    @Override
    public void supplyIccPin(String pin, Message result) {
        supplyIccPinForApp(pin, null, result);
    }

    @Override
    public void supplyIccPinForApp(String pin, String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_PIN, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " aid = " + aid);
            }

            try {
                simProxy.supplyIccPinForApp(rr.mSerial, RILUtils.convertNullToEmptyString(pin),
                        RILUtils.convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "supplyIccPinForApp", e);
            }
        }
    }

    @Override
    public void supplyIccPuk(String puk, String newPin, Message result) {
        supplyIccPukForApp(puk, newPin, null, result);
    }

    @Override
    public void supplyIccPukForApp(String puk, String newPin, String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_PUK, result, mRILDefaultWorkSource);

            String pukStr = RILUtils.convertNullToEmptyString(puk);
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " isPukEmpty = " + pukStr.isEmpty() + " aid = " + aid);
            }

            try {
                simProxy.supplyIccPukForApp(rr.mSerial, pukStr,
                        RILUtils.convertNullToEmptyString(newPin),
                        RILUtils.convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "supplyIccPukForApp", e);
            }
        }
    }

    @Override
    public void supplyIccPin2(String pin, Message result) {
        supplyIccPin2ForApp(pin, null, result);
    }

    @Override
    public void supplyIccPin2ForApp(String pin, String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_PIN2, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " aid = " + aid);
            }

            try {
                simProxy.supplyIccPin2ForApp(rr.mSerial, RILUtils.convertNullToEmptyString(pin),
                        RILUtils.convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "supplyIccPin2ForApp", e);
            }
        }
    }

    @Override
    public void supplyIccPuk2(String puk2, String newPin2, Message result) {
        supplyIccPuk2ForApp(puk2, newPin2, null, result);
    }

    @Override
    public void supplyIccPuk2ForApp(String puk, String newPin2, String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_PUK2, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " aid = " + aid);
            }

            try {
                simProxy.supplyIccPuk2ForApp(rr.mSerial, RILUtils.convertNullToEmptyString(puk),
                        RILUtils.convertNullToEmptyString(newPin2),
                        RILUtils.convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "supplyIccPuk2ForApp", e);
            }
        }
    }

    @Override
    public void changeIccPin(String oldPin, String newPin, Message result) {
        changeIccPinForApp(oldPin, newPin, null, result);
    }

    @Override
    public void changeIccPinForApp(String oldPin, String newPin, String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CHANGE_SIM_PIN, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " oldPin = " + oldPin + " newPin = " + newPin + " aid = " + aid);
            }

            try {
                simProxy.changeIccPinForApp(rr.mSerial,
                        RILUtils.convertNullToEmptyString(oldPin),
                        RILUtils.convertNullToEmptyString(newPin),
                        RILUtils.convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "changeIccPinForApp", e);
            }
        }
    }

    @Override
    public void changeIccPin2(String oldPin2, String newPin2, Message result) {
        changeIccPin2ForApp(oldPin2, newPin2, null, result);
    }

    @Override
    public void changeIccPin2ForApp(String oldPin2, String newPin2, String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CHANGE_SIM_PIN2, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " oldPin = " + oldPin2 + " newPin = " + newPin2 + " aid = " + aid);
            }

            try {
                simProxy.changeIccPin2ForApp(rr.mSerial,
                        RILUtils.convertNullToEmptyString(oldPin2),
                        RILUtils.convertNullToEmptyString(newPin2),
                        RILUtils.convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "changeIccPin2ForApp", e);
            }
        }
    }

    @Override
    public void supplyNetworkDepersonalization(String netpin, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " netpin = " + netpin);
            }

            try {
                networkProxy.supplyNetworkDepersonalization(rr.mSerial,
                        RILUtils.convertNullToEmptyString(netpin));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(
                        NETWORK_SERVICE, "supplyNetworkDepersonalization", e);
            }
        }
    }

    @Override
    public void supplySimDepersonalization(PersoSubState persoType, String controlKey,
            Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (simProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_DEPERSONALIZATION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " controlKey = " + controlKey + " persoType" + persoType);
            }

            try {
                simProxy.supplySimDepersonalization(rr.mSerial, persoType,
                        RILUtils.convertNullToEmptyString(controlKey));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "supplySimDepersonalization", e);
            }
        } else {
            if (PersoSubState.PERSOSUBSTATE_SIM_NETWORK == persoType) {
                supplyNetworkDepersonalization(controlKey, result);
                return;
            }
            if (RILJ_LOGD) {
                Rlog.d(RILJ_LOG_TAG, "supplySimDepersonalization: REQUEST_NOT_SUPPORTED");
            }
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void getCurrentCalls(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_CURRENT_CALLS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.getCurrentCalls(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "getCurrentCalls", e);
            }
        }
    }

    @Override
    public void dial(String address, boolean isEmergencyCall, EmergencyNumber emergencyNumberInfo,
            boolean hasKnownUserIntentEmergency, int clirMode, Message result) {
        dial(address, isEmergencyCall, emergencyNumberInfo, hasKnownUserIntentEmergency,
                clirMode, null, result);
    }

    @Override
    public void enableModem(boolean enable, Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (modemProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_3)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENABLE_MODEM, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " enable = " + enable);
            }

            try {
                modemProxy.enableModem(rr.mSerial, enable);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "enableModem", e);
            }
        } else {
            if (RILJ_LOGV) riljLog("enableModem: not supported.");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void setSystemSelectionChannels(@NonNull List<RadioAccessSpecifier> specifiers,
            Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (networkProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_3)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_SYSTEM_SELECTION_CHANNELS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " setSystemSelectionChannels_1.3= " + specifiers);
            }

            try {
                networkProxy.setSystemSelectionChannels(rr.mSerial, specifiers);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "setSystemSelectionChannels", e);
            }
        } else {
            if (RILJ_LOGV) riljLog("setSystemSelectionChannels: not supported.");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void getSystemSelectionChannels(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (networkProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_SYSTEM_SELECTION_CHANNELS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " getSystemSelectionChannels");
            }

            try {
                networkProxy.getSystemSelectionChannels(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "getSystemSelectionChannels", e);
            }
        } else {
            if (RILJ_LOGV) riljLog("getSystemSelectionChannels: not supported.");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void getModemStatus(Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (modemProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_3)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_MODEM_STATUS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                modemProxy.getModemStackStatus(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "getModemStatus", e);
            }
        } else {
            if (RILJ_LOGV) riljLog("getModemStatus: not supported.");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void dial(String address, boolean isEmergencyCall, EmergencyNumber emergencyNumberInfo,
            boolean hasKnownUserIntentEmergency, int clirMode, UUSInfo uusInfo, Message result) {
        if (isEmergencyCall && mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_4)
                && emergencyNumberInfo != null) {
            emergencyDial(address, emergencyNumberInfo, hasKnownUserIntentEmergency, clirMode,
                    uusInfo, result);
            return;
        }
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DIAL, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                // Do not log function arg for privacy
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.dial(rr.mSerial, address, clirMode, uusInfo);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "dial", e);
            }
        }
    }

    private void emergencyDial(String address, EmergencyNumber emergencyNumberInfo,
            boolean hasKnownUserIntentEmergency, int clirMode, UUSInfo uusInfo, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (voiceProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_4)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_EMERGENCY_DIAL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                // Do not log function arg for privacy
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.emergencyDial(rr.mSerial, RILUtils.convertNullToEmptyString(address),
                        emergencyNumberInfo, hasKnownUserIntentEmergency, clirMode, uusInfo);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "emergencyDial", e);
            }
        } else {
            riljLoge("emergencyDial is not supported with 1.4 below IRadio");
        }
    }

    @Override
    public void getIMSI(Message result) {
        getIMSIForApp(null, result);
    }

    @Override
    public void getIMSIForApp(String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_IMSI, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + ">  " + RILUtils.requestToString(rr.mRequest)
                        + " aid = " + aid);
            }
            try {
                simProxy.getImsiForApp(rr.mSerial, RILUtils.convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "getImsiForApp", e);
            }
        }
    }

    @Override
    public void hangupConnection(int gsmIndex, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_HANGUP, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " gsmIndex = " + gsmIndex);
            }

            try {
                voiceProxy.hangup(rr.mSerial, gsmIndex);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "hangup", e);
            }
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void hangupWaitingOrBackground(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.hangupWaitingOrBackground(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "hangupWaitingOrBackground", e);
            }
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void hangupForegroundResumeBackground(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.hangupForegroundResumeBackground(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(
                        VOICE_SERVICE, "hangupForegroundResumeBackground", e);
            }
        }
    }

    @Override
    public void switchWaitingOrHoldingAndActive(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.switchWaitingOrHoldingAndActive(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "switchWaitingOrHoldingAndActive", e);
            }
        }
    }

    @Override
    public void conference(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CONFERENCE, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.conference(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "conference", e);
            }
        }
    }

    @Override
    public void rejectCall(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_UDUB, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.rejectCall(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "rejectCall", e);
            }
        }
    }

    @Override
    public void getLastCallFailCause(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_LAST_CALL_FAIL_CAUSE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.getLastCallFailCause(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "getLastCallFailCause", e);
            }
        }
    }

    @Override
    public void getSignalStrength(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIGNAL_STRENGTH, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.getSignalStrength(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "getSignalStrength", e);
            }
        }
    }

    @Override
    public void getVoiceRegistrationState(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_VOICE_REGISTRATION_STATE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            HalVersion overrideHalVersion = getCompatVersion(RIL_REQUEST_VOICE_REGISTRATION_STATE);
            if (RILJ_LOGD) {
                riljLog("getVoiceRegistrationState: overrideHalVersion=" + overrideHalVersion);
            }

            try {
                networkProxy.getVoiceRegistrationState(rr.mSerial, overrideHalVersion);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "getVoiceRegistrationState", e);
            }
        }
    }

    @Override
    public void getDataRegistrationState(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DATA_REGISTRATION_STATE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            HalVersion overrideHalVersion = getCompatVersion(RIL_REQUEST_DATA_REGISTRATION_STATE);
            if (RILJ_LOGD) {
                riljLog("getDataRegistrationState: overrideHalVersion=" + overrideHalVersion);
            }

            try {
                networkProxy.getDataRegistrationState(rr.mSerial, overrideHalVersion);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "getDataRegistrationState", e);
            }
        }
    }

    @Override
    public void getOperator(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_OPERATOR, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.getOperator(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "getOperator", e);
            }
        }
    }

    @UnsupportedAppUsage
    @Override
    public void setRadioPower(boolean on, boolean forEmergencyCall,
            boolean preferredForEmergencyCall, Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (!modemProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_RADIO_POWER, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " on = " + on + " forEmergencyCall= " + forEmergencyCall
                        + " preferredForEmergencyCall="  + preferredForEmergencyCall);
            }

            try {
                modemProxy.setRadioPower(rr.mSerial, on, forEmergencyCall,
                        preferredForEmergencyCall);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "setRadioPower", e);
            }
        }
    }

    @Override
    public void sendDtmf(char c, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DTMF, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                // Do not log function arg for privacy
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.sendDtmf(rr.mSerial, c + "");
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "sendDtmf", e);
            }
        }
    }

    @Override
    public void sendSMS(String smscPdu, String pdu, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SEND_SMS, result, mRILDefaultWorkSource);

            // Do not log function args for privacy
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                messagingProxy.sendSms(rr.mSerial, smscPdu, pdu);
                mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_GSM,
                        SmsSession.Event.Format.SMS_FORMAT_3GPP, getOutgoingSmsMessageId(result));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "sendSMS", e);
            }
        }
    }

    /**
     * Extract the outgoing sms messageId from the tracker, if there is one. This is specifically
     * for SMS related APIs.
     * @param result the result Message
     * @return messageId unique identifier or 0 if there is no message id
     */
    public static long getOutgoingSmsMessageId(Message result) {
        if (result == null || !(result.obj instanceof SMSDispatcher.SmsTracker)) {
            return 0L;
        }
        long messageId = ((SMSDispatcher.SmsTracker) result.obj).mMessageId;
        if (RILJ_LOGV) {
            Rlog.d(RILJ_LOG_TAG, "getOutgoingSmsMessageId "
                    + SmsController.formatCrossStackMessageId(messageId));
        }
        return messageId;
    }

    @Override
    public void sendSMSExpectMore(String smscPdu, String pdu, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SEND_SMS_EXPECT_MORE, result,
                    mRILDefaultWorkSource);

            // Do not log function arg for privacy
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                messagingProxy.sendSmsExpectMore(rr.mSerial, smscPdu, pdu);
                mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_GSM,
                        SmsSession.Event.Format.SMS_FORMAT_3GPP, getOutgoingSmsMessageId(result));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "sendSMSExpectMore", e);
            }
        }
    }

    @Override
    public void setupDataCall(int accessNetworkType, DataProfile dataProfile, boolean isRoaming,
            boolean allowRoaming, int reason, LinkProperties linkProperties, int pduSessionId,
            NetworkSliceInfo sliceInfo, TrafficDescriptor trafficDescriptor,
            boolean matchAllRuleAllowed, Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class, result);
        if (!dataProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SETUP_DATA_CALL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + ",reason=" + RILUtils.setupDataReasonToString(reason)
                        + ",accessNetworkType=" + AccessNetworkType.toString(accessNetworkType)
                        + ",dataProfile=" + dataProfile + ",isRoaming=" + isRoaming
                        + ",allowRoaming=" + allowRoaming
                        + ",linkProperties=" + linkProperties + ",pduSessionId=" + pduSessionId
                        + ",sliceInfo=" + sliceInfo + ",trafficDescriptor=" + trafficDescriptor
                        + ",matchAllRuleAllowed=" + matchAllRuleAllowed);
            }

            try {
                dataProxy.setupDataCall(rr.mSerial, mPhoneId, accessNetworkType, dataProfile,
                        isRoaming, allowRoaming, reason, linkProperties, pduSessionId, sliceInfo,
                        trafficDescriptor, matchAllRuleAllowed);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(DATA_SERVICE, "setupDataCall", e);
            }
        }
    }

    @Override
    public void iccIO(int command, int fileId, String path, int p1, int p2, int p3, String data,
            String pin2, Message result) {
        iccIOForApp(command, fileId, path, p1, p2, p3, data, pin2, null, result);
    }

    @Override
    public void iccIOForApp(int command, int fileId, String path, int p1, int p2, int p3,
            String data, String pin2, String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIM_IO, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                if (TelephonyUtils.IS_DEBUGGABLE) {
                    riljLog(rr.serialString() + "> iccIO: " + RILUtils.requestToString(rr.mRequest)
                            + " command = 0x" + Integer.toHexString(command) + " fileId = 0x"
                            + Integer.toHexString(fileId) + " path = " + path + " p1 = " + p1
                            + " p2 = " + p2 + " p3 = " + " data = " + data + " aid = " + aid);
                } else {
                    riljLog(rr.serialString() + "> iccIO: "
                            + RILUtils.requestToString(rr.mRequest));
                }
            }

            try {
                simProxy.iccIoForApp(rr.mSerial, command, fileId,
                        RILUtils.convertNullToEmptyString(path), p1, p2, p3,
                        RILUtils.convertNullToEmptyString(data),
                        RILUtils.convertNullToEmptyString(pin2),
                        RILUtils.convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "iccIoForApp", e);
            }
        }
    }

    @Override
    public void sendUSSD(String ussd, Message result) {
        RadioVoiceProxy voiceProxy =
                getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SEND_USSD, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                String logUssd = "*******";
                if (RILJ_LOGV) logUssd = ussd;
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " ussd = " + logUssd);
            }

            try {
                voiceProxy.sendUssd(rr.mSerial, RILUtils.convertNullToEmptyString(ussd));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "sendUssd", e);
            }
        }
    }

    @Override
    public void cancelPendingUssd(Message result) {
        RadioVoiceProxy voiceProxy =
                getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CANCEL_USSD, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.cancelPendingUssd(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "cancelPendingUssd", e);
            }
        }
    }

    @Override
    public void getCLIR(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_CLIR, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.getClir(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "getClir", e);
            }
        }
    }

    @Override
    public void setCLIR(int clirMode, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_CLIR, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " clirMode = " + clirMode);
            }

            try {
                voiceProxy.setClir(rr.mSerial, clirMode);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "setClir", e);
            }
        }
    }

    @Override
    public void queryCallForwardStatus(int cfReason, int serviceClass, String number,
            Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_CALL_FORWARD_STATUS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " cfreason = " + cfReason + " serviceClass = " + serviceClass);
            }

            try {
                voiceProxy.getCallForwardStatus(rr.mSerial, cfReason, serviceClass, number);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "getCallForwardStatus", e);
            }
        }
    }

    @Override
    public void setCallForward(int action, int cfReason, int serviceClass, String number,
            int timeSeconds, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_CALL_FORWARD, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " action = " + action + " cfReason = " + cfReason + " serviceClass = "
                        + serviceClass + " timeSeconds = " + timeSeconds);
            }

            try {
                voiceProxy.setCallForward(
                        rr.mSerial, action, cfReason, serviceClass, number, timeSeconds);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "setCallForward", e);
            }
        }
    }

    @Override
    public void queryCallWaiting(int serviceClass, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_CALL_WAITING, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " serviceClass = " + serviceClass);
            }

            try {
                voiceProxy.getCallWaiting(rr.mSerial, serviceClass);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "getCallWaiting", e);
            }
        }
    }

    @Override
    public void setCallWaiting(boolean enable, int serviceClass, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_CALL_WAITING, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " enable = " + enable + " serviceClass = " + serviceClass);
            }

            try {
                voiceProxy.setCallWaiting(rr.mSerial, enable, serviceClass);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "setCallWaiting", e);
            }
        }
    }

    @Override
    public void acknowledgeLastIncomingGsmSms(boolean success, int cause, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SMS_ACKNOWLEDGE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " success = " + success + " cause = " + cause);
            }

            try {
                messagingProxy.acknowledgeLastIncomingGsmSms(rr.mSerial, success, cause);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE,
                        "acknowledgeLastIncomingGsmSms", e);
            }
        }
    }

    @Override
    public void acceptCall(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ANSWER, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.acceptCall(rr.mSerial);
                mMetrics.writeRilAnswer(mPhoneId, rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "acceptCall", e);
            }
        }
    }

    @Override
    public void deactivateDataCall(int cid, int reason, Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class, result);
        if (!dataProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DEACTIVATE_DATA_CALL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " cid = " + cid + " reason = "
                        + RILUtils.deactivateDataReasonToString(reason));
            }

            try {
                dataProxy.deactivateDataCall(rr.mSerial, cid, reason);
                mMetrics.writeRilDeactivateDataCall(mPhoneId, rr.mSerial, cid, reason);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(DATA_SERVICE, "deactivateDataCall", e);
            }
        }
    }

    @Override
    public void queryFacilityLock(String facility, String password, int serviceClass,
            Message result) {
        queryFacilityLockForApp(facility, password, serviceClass, null, result);
    }

    @Override
    public void queryFacilityLockForApp(String facility, String password, int serviceClass,
            String appId, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_FACILITY_LOCK, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " facility = " + facility + " serviceClass = " + serviceClass
                        + " appId = " + appId);
            }

            try {
                simProxy.getFacilityLockForApp(rr.mSerial,
                        RILUtils.convertNullToEmptyString(facility),
                        RILUtils.convertNullToEmptyString(password),
                        serviceClass, RILUtils.convertNullToEmptyString(appId));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "getFacilityLockForApp", e);
            }
        }
    }

    @Override
    public void setFacilityLock(String facility, boolean lockState, String password,
            int serviceClass, Message result) {
        setFacilityLockForApp(facility, lockState, password, serviceClass, null, result);
    }

    @Override
    public void setFacilityLockForApp(String facility, boolean lockState, String password,
            int serviceClass, String appId, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_FACILITY_LOCK, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " facility = " + facility + " lockstate = " + lockState
                        + " serviceClass = " + serviceClass + " appId = " + appId);
            }

            try {
                simProxy.setFacilityLockForApp(rr.mSerial,
                        RILUtils.convertNullToEmptyString(facility), lockState,
                        RILUtils.convertNullToEmptyString(password), serviceClass,
                        RILUtils.convertNullToEmptyString(appId));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "setFacilityLockForApp", e);
            }
        }
    }

    @Override
    public void changeBarringPassword(String facility, String oldPwd, String newPwd,
            Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CHANGE_BARRING_PASSWORD, result,
                    mRILDefaultWorkSource);

            // Do not log all function args for privacy
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + "facility = " + facility);
            }

            try {
                networkProxy.setBarringPassword(rr.mSerial,
                        RILUtils.convertNullToEmptyString(facility),
                        RILUtils.convertNullToEmptyString(oldPwd),
                        RILUtils.convertNullToEmptyString(newPwd));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "changeBarringPassword", e);
            }
        }
    }

    @Override
    public void getNetworkSelectionMode(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.getNetworkSelectionMode(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "getNetworkSelectionMode", e);
            }
        }
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.setNetworkSelectionModeAutomatic(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(
                        NETWORK_SERVICE, "setNetworkSelectionModeAutomatic", e);
            }
        }
    }

    @Override
    public void setNetworkSelectionModeManual(String operatorNumeric, int ran, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " operatorNumeric = " + operatorNumeric + ", ran = " + ran);
            }

            try {
                networkProxy.setNetworkSelectionModeManual(rr.mSerial,
                        RILUtils.convertNullToEmptyString(operatorNumeric), ran);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "setNetworkSelectionModeManual", e);
            }
        }
    }

    @Override
    public void getAvailableNetworks(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_AVAILABLE_NETWORKS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.getAvailableNetworks(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "getAvailableNetworks", e);
            }
        }
    }

    /**
     * Radio HAL fallback compatibility feature (b/151106728) assumes that the input parameter
     * networkScanRequest is immutable (read-only) here. Once the caller invokes the method, the
     * parameter networkScanRequest should not be modified. This helps us keep a consistent and
     * simple data model that avoid copying it in the scan result.
     */
    @Override
    public void startNetworkScan(NetworkScanRequest networkScanRequest, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (networkProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_1)) {
            HalVersion overrideHalVersion = getCompatVersion(RIL_REQUEST_START_NETWORK_SCAN);
            if (RILJ_LOGD) {
                riljLog("startNetworkScan: overrideHalVersion=" + overrideHalVersion);
            }

            RILRequest rr = obtainRequest(RIL_REQUEST_START_NETWORK_SCAN, result,
                    mRILDefaultWorkSource, networkScanRequest);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.startNetworkScan(rr.mSerial, networkScanRequest, overrideHalVersion,
                        result);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "startNetworkScan", e);
            }
        } else {
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "startNetworkScan: REQUEST_NOT_SUPPORTED");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void stopNetworkScan(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (networkProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_1)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_STOP_NETWORK_SCAN, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.stopNetworkScan(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "stopNetworkScan", e);
            }
        } else {
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "stopNetworkScan: REQUEST_NOT_SUPPORTED");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void startDtmf(char c, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DTMF_START, result, mRILDefaultWorkSource);

            // Do not log function arg for privacy
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.startDtmf(rr.mSerial, c + "");
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "startDtmf", e);
            }
        }
    }

    @Override
    public void stopDtmf(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DTMF_STOP, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.stopDtmf(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "stopDtmf", e);
            }
        }
    }

    @Override
    public void separateConnection(int gsmIndex, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SEPARATE_CONNECTION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " gsmIndex = " + gsmIndex);
            }

            try {
                voiceProxy.separateConnection(rr.mSerial, gsmIndex);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "separateConnection", e);
            }
        }
    }

    @Override
    public void getBasebandVersion(Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (!modemProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_BASEBAND_VERSION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                modemProxy.getBasebandVersion(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "getBasebandVersion", e);
            }
        }
    }

    @Override
    public void setMute(boolean enableMute, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_MUTE, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " enableMute = " + enableMute);
            }

            try {
                voiceProxy.setMute(rr.mSerial, enableMute);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "setMute", e);
            }
        }
    }

    @Override
    public void getMute(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_MUTE, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.getMute(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "getMute", e);
            }
        }
    }

    @Override
    public void queryCLIP(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_CLIP, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.getClip(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "getClip", e);
            }
        }
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public void getPDPContextList(Message result) {
        getDataCallList(result);
    }

    @Override
    public void getDataCallList(Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class, result);
        if (!dataProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DATA_CALL_LIST, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                dataProxy.getDataCallList(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(DATA_SERVICE, "getDataCallList", e);
            }
        }
    }

    // TODO(b/171260715) Remove when HAL definition is removed
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void invokeOemRilRequestRaw(byte[] data, Message response) {
    }

    // TODO(b/171260715) Remove when HAL definition is removed
    @Override
    public void invokeOemRilRequestStrings(String[] strings, Message result) {
    }

    @Override
    public void setSuppServiceNotifications(boolean enable, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " enable = " + enable);
            }

            try {
                networkProxy.setSuppServiceNotifications(rr.mSerial, enable);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "setSuppServiceNotifications", e);
            }
        }
    }

    @Override
    public void writeSmsToSim(int status, String smsc, String pdu, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_WRITE_SMS_TO_SIM, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGV) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " " + status);
            }

            try {
                messagingProxy.writeSmsToSim(rr.mSerial, status,
                        RILUtils.convertNullToEmptyString(smsc),
                        RILUtils.convertNullToEmptyString(pdu));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "writeSmsToSim", e);
            }
        }
    }

    @Override
    public void deleteSmsOnSim(int index, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DELETE_SMS_ON_SIM, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGV) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " index = " + index);
            }

            try {
                messagingProxy.deleteSmsOnSim(rr.mSerial, index);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "deleteSmsOnSim", e);
            }
        }
    }

    @Override
    public void setBandMode(int bandMode, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_BAND_MODE, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " bandMode = " + bandMode);
            }

            try {
                networkProxy.setBandMode(rr.mSerial, bandMode);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "setBandMode", e);
            }
        }
    }

    @Override
    public void queryAvailableBandMode(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.getAvailableBandModes(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "queryAvailableBandMode", e);
            }
        }
    }

    @Override
    public void sendEnvelope(String contents, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " contents = " + contents);
            }

            try {
                simProxy.sendEnvelope(rr.mSerial, RILUtils.convertNullToEmptyString(contents));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "sendEnvelope", e);
            }
        }
    }

    @Override
    public void sendTerminalResponse(String contents, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " contents = " + (TelephonyUtils.IS_DEBUGGABLE
                        ? contents : RILUtils.convertToCensoredTerminalResponse(contents)));
            }

            try {
                simProxy.sendTerminalResponseToSim(rr.mSerial,
                        RILUtils.convertNullToEmptyString(contents));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "sendTerminalResponse", e);
            }
        }
    }

    @Override
    public void sendEnvelopeWithStatus(String contents, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " contents = " + contents);
            }

            try {
                simProxy.sendEnvelopeWithStatus(rr.mSerial,
                        RILUtils.convertNullToEmptyString(contents));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "sendEnvelopeWithStatus", e);
            }
        }
    }

    @Override
    public void explicitCallTransfer(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_EXPLICIT_CALL_TRANSFER, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.explicitCallTransfer(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "explicitCallTransfer", e);
            }
        }
    }

    @Override
    public void setPreferredNetworkType(@PrefNetworkMode int networkType , Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " networkType = " + networkType);
            }
            mAllowedNetworkTypesBitmask = RadioAccessFamily.getRafFromNetworkType(networkType);
            mMetrics.writeSetPreferredNetworkType(mPhoneId, networkType);

            try {
                networkProxy.setPreferredNetworkTypeBitmap(rr.mSerial, mAllowedNetworkTypesBitmask);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "setPreferredNetworkType", e);
            }
        }
    }

    @Override
    public void getPreferredNetworkType(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.getAllowedNetworkTypesBitmap(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "getPreferredNetworkType", e);
            }
        }
    }

    @Override
    public void setAllowedNetworkTypesBitmap(
            @TelephonyManager.NetworkTypeBitMask int networkTypeBitmask, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            if (mRadioVersion.less(RADIO_HAL_VERSION_1_6)) {
                // For older HAL, redirects the call to setPreferredNetworkType.
                setPreferredNetworkType(
                        RadioAccessFamily.getNetworkTypeFromRaf(networkTypeBitmask), result);
                return;
            }
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_ALLOWED_NETWORK_TYPES_BITMAP, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }
            mAllowedNetworkTypesBitmask = networkTypeBitmask;

            try {
                networkProxy.setAllowedNetworkTypesBitmap(rr.mSerial, mAllowedNetworkTypesBitmask);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "setAllowedNetworkTypeBitmask", e);
            }
        }
    }

    @Override
    public void getAllowedNetworkTypesBitmap(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_ALLOWED_NETWORK_TYPES_BITMAP, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.getAllowedNetworkTypesBitmap(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "getAllowedNetworkTypeBitmask", e);
            }
        }
    }

    @Override
    public void setLocationUpdates(boolean enable, WorkSource workSource, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_LOCATION_UPDATES, result,
                    getDefaultWorkSourceIfInvalid(workSource));

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " enable = " + enable);
            }

            try {
                networkProxy.setLocationUpdates(rr.mSerial, enable);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "setLocationUpdates", e);
            }
        }
    }

    /**
     * Is E-UTRA-NR Dual Connectivity enabled
     */
    @Override
    public void isNrDualConnectivityEnabled(Message result, WorkSource workSource) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (networkProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_IS_NR_DUAL_CONNECTIVITY_ENABLED, result,
                    getDefaultWorkSourceIfInvalid(workSource));

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.isNrDualConnectivityEnabled(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "isNrDualConnectivityEnabled", e);
            }
        } else {
            if (RILJ_LOGD) {
                Rlog.d(RILJ_LOG_TAG, "isNrDualConnectivityEnabled: REQUEST_NOT_SUPPORTED");
            }
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    /**
     * Enable/Disable E-UTRA-NR Dual Connectivity
     * @param nrDualConnectivityState expected NR dual connectivity state
     * This can be passed following states
     * <ol>
     * <li>Enable NR dual connectivity {@link TelephonyManager#NR_DUAL_CONNECTIVITY_ENABLE}
     * <li>Disable NR dual connectivity {@link TelephonyManager#NR_DUAL_CONNECTIVITY_DISABLE}
     * <li>Disable NR dual connectivity and force secondary cell to be released
     * {@link TelephonyManager#NR_DUAL_CONNECTIVITY_DISABLE_IMMEDIATE}
     * </ol>
     */
    @Override
    public void setNrDualConnectivityState(int nrDualConnectivityState, Message result,
            WorkSource workSource) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (networkProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENABLE_NR_DUAL_CONNECTIVITY, result,
                    getDefaultWorkSourceIfInvalid(workSource));

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " enable = " + nrDualConnectivityState);
            }

            try {
                networkProxy.setNrDualConnectivityState(rr.mSerial, (byte) nrDualConnectivityState);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "enableNrDualConnectivity", e);
            }
        } else {
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "enableNrDualConnectivity: REQUEST_NOT_SUPPORTED");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    private void setVoNrEnabled(boolean enabled) {
        SystemProperties.set(PROPERTY_IS_VONR_ENABLED + mPhoneId, String.valueOf(enabled));
    }

    private boolean isVoNrEnabled() {
        return SystemProperties.getBoolean(PROPERTY_IS_VONR_ENABLED + mPhoneId, true);
    }

    /**
     * Is voice over NR enabled
     */
    @Override
    public void isVoNrEnabled(Message result, WorkSource workSource) {

        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_2_0)) {
            RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
            if (!voiceProxy.isEmpty()) {
                RILRequest rr = obtainRequest(RIL_REQUEST_IS_VONR_ENABLED , result,
                        getDefaultWorkSourceIfInvalid(workSource));

                if (RILJ_LOGD) {
                    riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
                }

                try {
                    voiceProxy.isVoNrEnabled(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(VOICE_SERVICE, "isVoNrEnabled", e);
                }
            }
        } else {
            boolean isEnabled = isVoNrEnabled();
            if (result != null) {
                AsyncResult.forMessage(result, isEnabled, null);
                result.sendToTarget();
            }
        }
    }

    /**
     * Enable or disable Voice over NR (VoNR)
     * @param enabled enable or disable VoNR.
     */
    @Override
    public void setVoNrEnabled(boolean enabled, Message result, WorkSource workSource) {
        setVoNrEnabled(enabled);

        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_2_0)) {
            RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
            if (!voiceProxy.isEmpty()) {
                RILRequest rr = obtainRequest(RIL_REQUEST_ENABLE_VONR, result,
                        getDefaultWorkSourceIfInvalid(workSource));

                if (RILJ_LOGD) {
                    riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
                }

                try {
                    voiceProxy.setVoNrEnabled(rr.mSerial, enabled);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(VOICE_SERVICE, "setVoNrEnabled", e);
                }
            }
        } else {
            /* calling a query api to let HAL know that VoNREnabled state is updated.
               This is a work around as new AIDL API is not allowed for older HAL version devices.
               HAL can check the value of PROPERTY_IS_VONR_ENABLED property to determine
               if there is any change whenever it receives isNrDualConnectivityEnabled request.
            */
            isNrDualConnectivityEnabled(null, workSource);
            if (result != null) {
                AsyncResult.forMessage(result, null, null);
                result.sendToTarget();
            }
        }
    }

    @Override
    public void setCdmaSubscriptionSource(int cdmaSubscription, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " cdmaSubscription = " + cdmaSubscription);
            }

            try {
                simProxy.setCdmaSubscriptionSource(rr.mSerial, cdmaSubscription);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "setCdmaSubscriptionSource", e);
            }
        }
    }

    @Override
    public void queryCdmaRoamingPreference(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.getCdmaRoamingPreference(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "queryCdmaRoamingPreference", e);
            }
        }
    }

    @Override
    public void setCdmaRoamingPreference(int cdmaRoamingType, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " cdmaRoamingType = " + cdmaRoamingType);
            }

            try {
                networkProxy.setCdmaRoamingPreference(rr.mSerial, cdmaRoamingType);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "setCdmaRoamingPreference", e);
            }
        }
    }

    @Override
    public void queryTTYMode(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_TTY_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.getTtyMode(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "getTtyMode", e);
            }
        }
    }

    @Override
    public void setTTYMode(int ttyMode, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_TTY_MODE, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " ttyMode = " + ttyMode);
            }

            try {
                voiceProxy.setTtyMode(rr.mSerial, ttyMode);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "setTtyMode", e);
            }
        }
    }

    @Override
    public void setPreferredVoicePrivacy(boolean enable, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " enable = " + enable);
            }

            try {
                voiceProxy.setPreferredVoicePrivacy(rr.mSerial, enable);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "setPreferredVoicePrivacy", e);
            }
        }
    }

    @Override
    public void getPreferredVoicePrivacy(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.getPreferredVoicePrivacy(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "getPreferredVoicePrivacy", e);
            }
        }
    }

    @Override
    public void sendCDMAFeatureCode(String featureCode, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_FLASH, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " featureCode = " + Rlog.pii(RILJ_LOG_TAG, featureCode));
            }

            try {
                voiceProxy.sendCdmaFeatureCode(rr.mSerial,
                        RILUtils.convertNullToEmptyString(featureCode));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "sendCdmaFeatureCode", e);
            }
        }
    }

    @Override
    public void sendBurstDtmf(String dtmfString, int on, int off, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_BURST_DTMF, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " dtmfString = " + dtmfString + " on = " + on + " off = " + off);
            }

            try {
                voiceProxy.sendBurstDtmf(rr.mSerial, RILUtils.convertNullToEmptyString(dtmfString),
                        on, off);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "sendBurstDtmf", e);
            }
        }
    }

    @Override
    public void sendCdmaSMSExpectMore(byte[] pdu, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SEND_SMS_EXPECT_MORE, result,
                    mRILDefaultWorkSource);

            // Do not log function arg for privacy
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                messagingProxy.sendCdmaSmsExpectMore(rr.mSerial, pdu);
                if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
                    mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_CDMA,
                            SmsSession.Event.Format.SMS_FORMAT_3GPP2,
                            getOutgoingSmsMessageId(result));
                }
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "sendCdmaSMSExpectMore", e);
            }
        }
    }

    @Override
    public void sendCdmaSms(byte[] pdu, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SEND_SMS, result, mRILDefaultWorkSource);

            // Do not log function arg for privacy
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                messagingProxy.sendCdmaSms(rr.mSerial, pdu);
                mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_CDMA,
                        SmsSession.Event.Format.SMS_FORMAT_3GPP2, getOutgoingSmsMessageId(result));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "sendCdmaSms", e);
            }
        }
    }

    @Override
    public void acknowledgeLastIncomingCdmaSms(boolean success, int cause, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " success = " + success + " cause = " + cause);
            }

            try {
                messagingProxy.acknowledgeLastIncomingCdmaSms(rr.mSerial, success, cause);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE,
                        "acknowledgeLastIncomingCdmaSms", e);
            }
        }
    }

    @Override
    public void getGsmBroadcastConfig(Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GSM_GET_BROADCAST_CONFIG, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                messagingProxy.getGsmBroadcastConfig(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "getGsmBroadcastConfig", e);
            }
        }
    }

    @Override
    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] config, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GSM_SET_BROADCAST_CONFIG, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " with " + config.length + " configs : ");
                for (int i = 0; i < config.length; i++) {
                    riljLog(config[i].toString());
                }
            }

            try {
                messagingProxy.setGsmBroadcastConfig(rr.mSerial, config);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "setGsmBroadcastConfig", e);
            }
        }
    }

    @Override
    public void setGsmBroadcastActivation(boolean activate, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GSM_BROADCAST_ACTIVATION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " activate = " + activate);
            }

            try {
                messagingProxy.setGsmBroadcastActivation(rr.mSerial, activate);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "setGsmBroadcastActivation", e);
            }
        }
    }

    @Override
    public void getCdmaBroadcastConfig(Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                messagingProxy.getCdmaBroadcastConfig(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "getCdmaBroadcastConfig", e);
            }
        }
    }

    @Override
    public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] configs, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " with " + configs.length + " configs : ");
                for (CdmaSmsBroadcastConfigInfo config : configs) {
                    riljLog(config.toString());
                }
            }

            try {
                messagingProxy.setCdmaBroadcastConfig(rr.mSerial, configs);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "setCdmaBroadcastConfig", e);
            }
        }
    }

    @Override
    public void setCdmaBroadcastActivation(boolean activate, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_BROADCAST_ACTIVATION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " activate = " + activate);
            }

            try {
                messagingProxy.setCdmaBroadcastActivation(rr.mSerial, activate);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "setCdmaBroadcastActivation", e);
            }
        }
    }

    @Override
    public void getCDMASubscription(Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SUBSCRIPTION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                simProxy.getCdmaSubscription(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "getCdmaSubscription", e);
            }
        }
    }

    @Override
    public void writeSmsToRuim(int status, byte[] pdu, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGV) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " status = " + status);
            }

            try {
                messagingProxy.writeSmsToRuim(rr.mSerial, status, pdu);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "writeSmsToRuim", e);
            }
        }
    }

    @Override
    public void deleteSmsOnRuim(int index, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGV) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " index = " + index);
            }

            try {
                messagingProxy.deleteSmsOnRuim(rr.mSerial, index);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "deleteSmsOnRuim", e);
            }
        }
    }

    @Override
    public void getDeviceIdentity(Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (!modemProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DEVICE_IDENTITY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                modemProxy.getDeviceIdentity(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "getDeviceIdentity", e);
            }
        }
    }

    @Override
    public void exitEmergencyCallbackMode(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.exitEmergencyCallbackMode(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(VOICE_SERVICE, "exitEmergencyCallbackMode", e);
            }
        }
    }

    @Override
    public void getSmscAddress(Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_SMSC_ADDRESS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                messagingProxy.getSmscAddress(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "getSmscAddress", e);
            }
        }
    }

    @Override
    public void setSmscAddress(String address, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_SMSC_ADDRESS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " address = " + address);
            }

            try {
                messagingProxy.setSmscAddress(rr.mSerial,
                        RILUtils.convertNullToEmptyString(address));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "setSmscAddress", e);
            }
        }
    }

    @Override
    public void reportSmsMemoryStatus(boolean available, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_REPORT_SMS_MEMORY_STATUS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " available = " + available);
            }

            try {
                messagingProxy.reportSmsMemoryStatus(rr.mSerial, available);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "reportSmsMemoryStatus", e);
            }
        }
    }

    @Override
    public void reportStkServiceIsRunning(Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                simProxy.reportStkServiceIsRunning(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "reportStkServiceIsRunning", e);
            }
        }
    }

    @Override
    public void getCdmaSubscriptionSource(Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                simProxy.getCdmaSubscriptionSource(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "getCdmaSubscriptionSource", e);
            }
        }
    }

    @Override
    public void acknowledgeIncomingGsmSmsWithPdu(boolean success, String ackPdu, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " success = " + success);
            }

            try {
                messagingProxy.acknowledgeIncomingGsmSmsWithPdu(rr.mSerial, success,
                        RILUtils.convertNullToEmptyString(ackPdu));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE,
                        "acknowledgeIncomingGsmSmsWithPdu", e);
            }
        }
    }

    @Override
    public void getVoiceRadioTechnology(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_VOICE_RADIO_TECH, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.getVoiceRadioTechnology(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "getVoiceRadioTechnology", e);
            }
        }
    }

    @Override
    public void getCellInfoList(Message result, WorkSource workSource) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_CELL_INFO_LIST, result,
                    getDefaultWorkSourceIfInvalid(workSource));

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.getCellInfoList(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "getCellInfoList", e);
            }
        }
    }

    @Override
    public void setCellInfoListRate(int rateInMillis, Message result, WorkSource workSource) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_UNSOL_CELL_INFO_LIST_RATE, result,
                    getDefaultWorkSourceIfInvalid(workSource));

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " rateInMillis = " + rateInMillis);
            }

            try {
                networkProxy.setCellInfoListRate(rr.mSerial, rateInMillis);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "setCellInfoListRate", e);
            }
        }
    }

    @Override
    public void setInitialAttachApn(DataProfile dataProfile, boolean isRoaming, Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class, result);
        if (!dataProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_INITIAL_ATTACH_APN, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + dataProfile);
            }

            try {
                dataProxy.setInitialAttachApn(rr.mSerial, dataProfile, isRoaming);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(DATA_SERVICE, "setInitialAttachApn", e);
            }
        }
    }

    @Override
    public void getImsRegistrationState(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_IMS_REGISTRATION_STATE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.getImsRegistrationState(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "getImsRegistrationState", e);
            }
        }
    }

    @Override
    public void sendImsGsmSms(String smscPdu, String pdu, int retry, int messageRef,
            Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_IMS_SEND_SMS, result, mRILDefaultWorkSource);

            // Do not log function args for privacy
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                messagingProxy.sendImsSms(rr.mSerial, smscPdu, pdu, null, retry, messageRef);
                mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_IMS,
                        SmsSession.Event.Format.SMS_FORMAT_3GPP, getOutgoingSmsMessageId(result));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "sendImsGsmSms", e);
            }
        }
    }

    @Override
    public void sendImsCdmaSms(byte[] pdu, int retry, int messageRef, Message result) {
        RadioMessagingProxy messagingProxy =
                getRadioServiceProxy(RadioMessagingProxy.class, result);
        if (!messagingProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_IMS_SEND_SMS, result, mRILDefaultWorkSource);

            // Do not log function args for privacy
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                messagingProxy.sendImsSms(rr.mSerial, null, null, pdu, retry, messageRef);
                mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_IMS,
                        SmsSession.Event.Format.SMS_FORMAT_3GPP2, getOutgoingSmsMessageId(result));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MESSAGING_SERVICE, "sendImsCdmaSms", e);
            }
        }
    }

    @Override
    public void iccTransmitApduBasicChannel(int cla, int instruction, int p1, int p2, int p3,
            String data, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIM_TRANSMIT_APDU_BASIC, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                if (TelephonyUtils.IS_DEBUGGABLE) {
                    riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                            + String.format(" cla = 0x%02X ins = 0x%02X", cla, instruction)
                            + String.format(" p1 = 0x%02X p2 = 0x%02X p3 = 0x%02X", p1, p2, p3)
                            + " data = " + data);
                } else {
                    riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
                }
            }

            try {
                simProxy.iccTransmitApduBasicChannel(
                        rr.mSerial, cla, instruction, p1, p2, p3, data);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "iccTransmitApduBasicChannel", e);
            }
        }
    }

    @Override
    public void iccOpenLogicalChannel(String aid, int p2, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIM_OPEN_CHANNEL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                if (TelephonyUtils.IS_DEBUGGABLE) {
                    riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                            + " aid = " + aid + " p2 = " + p2);
                } else {
                    riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
                }
            }

            try {
                simProxy.iccOpenLogicalChannel(rr.mSerial, RILUtils.convertNullToEmptyString(aid),
                        p2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "iccOpenLogicalChannel", e);
            }
        }
    }

    @Override
    public void iccCloseLogicalChannel(int channel, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIM_CLOSE_CHANNEL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " channel = " + channel);
            }

            try {
                simProxy.iccCloseLogicalChannel(rr.mSerial, channel);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "iccCloseLogicalChannel", e);
            }
        }
    }

    @Override
    public void iccTransmitApduLogicalChannel(int channel, int cla, int instruction, int p1, int p2,
            int p3, String data, Message result) {
        if (channel <= 0) {
            throw new RuntimeException(
                    "Invalid channel in iccTransmitApduLogicalChannel: " + channel);
        }

        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                if (TelephonyUtils.IS_DEBUGGABLE) {
                    riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                            + String.format(" channel = %d", channel)
                            + String.format(" cla = 0x%02X ins = 0x%02X", cla, instruction)
                            + String.format(" p1 = 0x%02X p2 = 0x%02X p3 = 0x%02X", p1, p2, p3)
                            + " data = " + data);
                } else {
                    riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
                }
            }

            try {
                simProxy.iccTransmitApduLogicalChannel(
                        rr.mSerial, channel, cla, instruction, p1, p2, p3, data);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "iccTransmitApduLogicalChannel", e);
            }
        }
    }

    @Override
    public void nvReadItem(int itemID, Message result, WorkSource workSource) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (!modemProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_NV_READ_ITEM, result,
                    getDefaultWorkSourceIfInvalid(workSource));

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " itemId = " + itemID);
            }

            try {
                modemProxy.nvReadItem(rr.mSerial, itemID);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "nvReadItem", e);
            }
        }
    }

    @Override
    public void nvWriteItem(int itemId, String itemValue, Message result, WorkSource workSource) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (!modemProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_NV_WRITE_ITEM, result,
                    getDefaultWorkSourceIfInvalid(workSource));

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " itemId = " + itemId + " itemValue = " + itemValue);
            }

            try {
                modemProxy.nvWriteItem(rr.mSerial, itemId,
                        RILUtils.convertNullToEmptyString(itemValue));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "nvWriteItem", e);
            }
        }
    }

    @Override
    public void nvWriteCdmaPrl(byte[] preferredRoamingList, Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (!modemProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_NV_WRITE_CDMA_PRL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " PreferredRoamingList = 0x"
                        + IccUtils.bytesToHexString(preferredRoamingList));
            }

            try {
                modemProxy.nvWriteCdmaPrl(rr.mSerial, preferredRoamingList);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "nvWriteCdmaPrl", e);
            }
        }
    }

    @Override
    public void nvResetConfig(int resetType, Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (!modemProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_NV_RESET_CONFIG, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " resetType = " + resetType);
            }

            try {
                modemProxy.nvResetConfig(rr.mSerial, resetType);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "nvResetConfig", e);
            }
        }
    }

    @Override
    public void setUiccSubscription(int slotId, int appIndex, int subId, int subStatus,
            Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_UICC_SUBSCRIPTION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " slot = " + slotId + " appIndex = " + appIndex
                        + " subId = " + subId + " subStatus = " + subStatus);
            }

            try {
                simProxy.setUiccSubscription(rr.mSerial, slotId, appIndex, subId, subStatus);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "setUiccSubscription", e);
            }
        }
    }

    /**
     * Whether the device modem supports reporting the EID in either the slot or card status or
     * through ATR.
     * @return true if the modem supports EID.
     */
    @Override
    public boolean supportsEid() {
        // EID should be supported as long as HAL >= 1.2.
        //  - in HAL 1.2 we have EID through ATR
        //  - in later HAL versions we also have EID through slot / card status.
        return mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_2);
    }

    @Override
    public void setDataAllowed(boolean allowed, Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class, result);
        if (!dataProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ALLOW_DATA, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " allowed = " + allowed);
            }

            try {
                dataProxy.setDataAllowed(rr.mSerial, allowed);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(DATA_SERVICE, "setDataAllowed", e);
            }
        }
    }

    @Override
    public void getHardwareConfig(Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (!modemProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_HARDWARE_CONFIG, result,
                    mRILDefaultWorkSource);

            // Do not log function args for privacy
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                modemProxy.getHardwareConfig(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "getHardwareConfig", e);
            }
        }
    }

    @Override
    public void requestIccSimAuthentication(int authContext, String data, String aid,
            Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIM_AUTHENTICATION, result,
                    mRILDefaultWorkSource);

            // Do not log function args for privacy
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                simProxy.requestIccSimAuthentication(rr.mSerial, authContext,
                        RILUtils.convertNullToEmptyString(data),
                        RILUtils.convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "requestIccSimAuthentication", e);
            }
        }
    }

    @Override
    public void setDataProfile(DataProfile[] dps, boolean isRoaming, Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class, result);
        if (!dataProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_DATA_PROFILE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " with data profiles : ");
                for (DataProfile profile : dps) {
                    riljLog(profile.toString());
                }
            }

            try {
                dataProxy.setDataProfile(rr.mSerial, dps, isRoaming);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(DATA_SERVICE, "setDataProfile", e);
            }
        }
    }

    @Override
    public void requestShutdown(Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (!modemProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SHUTDOWN, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                modemProxy.requestShutdown(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "requestShutdown", e);
            }
        }
    }

    @Override
    public void getRadioCapability(Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (!modemProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_RADIO_CAPABILITY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                modemProxy.getRadioCapability(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "getRadioCapability", e);
            }
        }
    }

    @Override
    public void setRadioCapability(RadioCapability rc, Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (!modemProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_RADIO_CAPABILITY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " RadioCapability = " + rc.toString());
            }

            try {
                modemProxy.setRadioCapability(rr.mSerial, rc);
            } catch (Exception e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "setRadioCapability", e);
            }
        }
    }

    @Override
    public void startLceService(int reportIntervalMs, boolean pullMode, Message result) {
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_2)) {
            // We have a 1.2 or later radio, so the LCE 1.0 LCE service control path is unused.
            // Instead the LCE functionality is always-on and provides unsolicited indications.
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "startLceService: REQUEST_NOT_SUPPORTED");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
            return;
        }

        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_START_LCE, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " reportIntervalMs = " + reportIntervalMs + " pullMode = " + pullMode);
            }

            try {
                radioProxy.startLceService(rr.mSerial, reportIntervalMs, pullMode);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(RADIO_SERVICE, "startLceService", e);
            }
        }
    }

    @Override
    public void stopLceService(Message result) {
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_2)) {
            // We have a 1.2 or later radio, so the LCE 1.0 LCE service control is unused.
            // Instead the LCE functionality is always-on and provides unsolicited indications.
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "stopLceService: REQUEST_NOT_SUPPORTED");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
            return;
        }

        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_STOP_LCE, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                radioProxy.stopLceService(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(RADIO_SERVICE, "stopLceService", e);
            }
        }
    }

    /**
     * Control the data throttling at modem.
     *
     * @param result Message that will be sent back to the requester
     * @param workSource calling Worksource
     * @param dataThrottlingAction the DataThrottlingAction that is being requested. Defined in
     *      android.hardware.radio@1.6.types.
     * @param completionWindowMillis milliseconds in which full throttling has to be achieved.
     */
    @Override
    public void setDataThrottling(Message result, WorkSource workSource, int dataThrottlingAction,
            long completionWindowMillis) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class, result);
        if (dataProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_DATA_THROTTLING, result,
                    getDefaultWorkSourceIfInvalid(workSource));

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> "
                        + RILUtils.requestToString(rr.mRequest)
                        + " dataThrottlingAction = " + dataThrottlingAction
                        + " completionWindowMillis " + completionWindowMillis);
            }

            try {
                dataProxy.setDataThrottling(rr.mSerial, (byte) dataThrottlingAction,
                        completionWindowMillis);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(DATA_SERVICE, "setDataThrottling", e);
            }
        } else {
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "setDataThrottling: REQUEST_NOT_SUPPORTED");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    /**
     * This will only be called if the LCE service is started in PULL mode, which is
     * only enabled when using Radio HAL versions 1.1 and earlier.
     *
     * It is still possible for vendors to override this behavior and use the 1.1 version
     * of LCE; however, this is strongly discouraged and this functionality will be removed
     * when HAL 1.x support is dropped.
     *
     * @deprecated HAL 1.2 and later use an always-on LCE that relies on indications.
     */
    @Deprecated
    @Override
    public void pullLceData(Message result) {
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_2)) {
            // We have a 1.2 or later radio, so the LCE 1.0 LCE service control path is unused.
            // Instead the LCE functionality is always-on and provides unsolicited indications.
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "pullLceData: REQUEST_NOT_SUPPORTED");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
            return;
        }

        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_PULL_LCEDATA, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                radioProxy.pullLceData(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(RADIO_SERVICE, "pullLceData", e);
            }
        }
    }

    @Override
    public void getModemActivityInfo(Message result, WorkSource workSource) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (!modemProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_ACTIVITY_INFO, result,
                    getDefaultWorkSourceIfInvalid(workSource));

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                modemProxy.getModemActivityInfo(rr.mSerial);
                Message msg =
                        mRilHandler.obtainMessage(EVENT_BLOCKING_RESPONSE_TIMEOUT, rr.mSerial);
                mRilHandler.sendMessageDelayed(msg, DEFAULT_BLOCKING_MESSAGE_RESPONSE_TIMEOUT_MS);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "getModemActivityInfo", e);
            }
        }
    }

    @Override
    public void setAllowedCarriers(CarrierRestrictionRules carrierRestrictionRules,
            Message result, WorkSource workSource) {
        Objects.requireNonNull(carrierRestrictionRules, "Carrier restriction cannot be null.");

        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_ALLOWED_CARRIERS, result,
                    getDefaultWorkSourceIfInvalid(workSource));

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " params: " + carrierRestrictionRules);
            }

            try {
                simProxy.setAllowedCarriers(rr.mSerial, carrierRestrictionRules, result);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "setAllowedCarriers", e);
            }
        }
    }

    @Override
    public void getAllowedCarriers(Message result, WorkSource workSource) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_ALLOWED_CARRIERS, result,
                    getDefaultWorkSourceIfInvalid(workSource));

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                simProxy.getAllowedCarriers(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "getAllowedCarriers", e);
            }
        }
    }

    @Override
    public void sendDeviceState(int stateType, boolean state, Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class, result);
        if (!modemProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SEND_DEVICE_STATE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest) + " "
                        + stateType + ":" + state);
            }

            try {
                modemProxy.sendDeviceState(rr.mSerial, stateType, state);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(MODEM_SERVICE, "sendDeviceState", e);
            }
        }
    }

    @Override
    public void setUnsolResponseFilter(int filter, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (!networkProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_UNSOLICITED_RESPONSE_FILTER, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " " + filter);
            }

            try {
                networkProxy.setIndicationFilter(rr.mSerial, filter);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "setIndicationFilter", e);
            }
        }
    }

    @Override
    public void setSignalStrengthReportingCriteria(
            @NonNull List<SignalThresholdInfo> signalThresholdInfos, @Nullable Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (networkProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_2)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_SIGNAL_STRENGTH_REPORTING_CRITERIA,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.setSignalStrengthReportingCriteria(rr.mSerial, signalThresholdInfos);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE,
                        "setSignalStrengthReportingCriteria", e);
            }
        } else {
            riljLoge("setSignalStrengthReportingCriteria ignored on IRadio version less than 1.2");
        }
    }

    @Override
    public void setLinkCapacityReportingCriteria(int hysteresisMs, int hysteresisDlKbps,
            int hysteresisUlKbps, int[] thresholdsDlKbps, int[] thresholdsUlKbps, int ran,
            Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (networkProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_2)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_LINK_CAPACITY_REPORTING_CRITERIA, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.setLinkCapacityReportingCriteria(rr.mSerial, hysteresisMs,
                        hysteresisDlKbps, hysteresisUlKbps, thresholdsDlKbps, thresholdsUlKbps,
                        ran);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(
                        NETWORK_SERVICE, "setLinkCapacityReportingCriteria", e);
            }
        } else {
            riljLoge("setLinkCapacityReportingCriteria ignored on IRadio version less than 1.2");
        }
    }

    @Override
    public void setSimCardPower(int state, Message result, WorkSource workSource) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (!simProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_SIM_CARD_POWER, result,
                    getDefaultWorkSourceIfInvalid(workSource));

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " " + state);
            }

            try {
                simProxy.setSimCardPower(rr.mSerial, state, result);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "setSimCardPower", e);
            }
        }
    }

    @Override
    public void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo,
            Message result) {
        Objects.requireNonNull(imsiEncryptionInfo, "ImsiEncryptionInfo cannot be null.");
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (simProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_1)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_CARRIER_INFO_IMSI_ENCRYPTION, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                simProxy.setCarrierInfoForImsiEncryption(rr.mSerial, imsiEncryptionInfo);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "setCarrierInfoForImsiEncryption", e);
            }
        } else {
            if (RILJ_LOGD) {
                Rlog.d(RILJ_LOG_TAG, "setCarrierInfoForImsiEncryption: REQUEST_NOT_SUPPORTED");
            }
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void startNattKeepalive(int contextId, KeepalivePacketData packetData,
            int intervalMillis, Message result) {
        Objects.requireNonNull(packetData, "KeepaliveRequest cannot be null.");
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class, result);
        if (dataProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_1)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_START_KEEPALIVE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                dataProxy.startKeepalive(rr.mSerial, contextId, packetData, intervalMillis, result);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(DATA_SERVICE, "startNattKeepalive", e);
            }
        } else {
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "startNattKeepalive: REQUEST_NOT_SUPPORTED");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void stopNattKeepalive(int sessionHandle, Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class, result);
        if (dataProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_1)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_STOP_KEEPALIVE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                dataProxy.stopKeepalive(rr.mSerial, sessionHandle);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(DATA_SERVICE, "stopNattKeepalive", e);
            }
        } else {
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "stopNattKeepalive: REQUEST_NOT_SUPPORTED");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void getIMEI(Message result) {
        throw new RuntimeException("getIMEI not expected to be called");
    }

    @Override
    public void getIMEISV(Message result) {
        throw new RuntimeException("getIMEISV not expected to be called");
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public void getLastPdpFailCause(Message result) {
        throw new RuntimeException("getLastPdpFailCause not expected to be called");
    }

    /**
     * The preferred new alternative to getLastPdpFailCause
     */
    @Override
    public void getLastDataCallFailCause(Message result) {
        throw new RuntimeException("getLastDataCallFailCause not expected to be called");
    }

    /**
     * Enable or disable uicc applications on the SIM.
     *
     * @param enable whether to enable or disable uicc applications.
     * @param result a Message to return to the requester
     */
    @Override
    public void enableUiccApplications(boolean enable, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (simProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENABLE_UICC_APPLICATIONS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                simProxy.enableUiccApplications(rr.mSerial, enable);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "enableUiccApplications", e);
            }
        } else {
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "enableUiccApplications: REQUEST_NOT_SUPPORTED");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    /**
     * Whether uicc applications are enabled or not.
     *
     * @param result a Message to return to the requester
     */
    @Override
    public void areUiccApplicationsEnabled(Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (simProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_UICC_APPLICATIONS_ENABLEMENT, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                simProxy.areUiccApplicationsEnabled(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "areUiccApplicationsEnabled", e);
            }
        } else {
            if (RILJ_LOGD) {
                Rlog.d(RILJ_LOG_TAG, "areUiccApplicationsEnabled: REQUEST_NOT_SUPPORTED");
            }
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    /**
     * Whether {@link #enableUiccApplications} is supported, which is supported in 1.5 version.
     */
    @Override
    public boolean canToggleUiccApplicationsEnablement() {
        return !getRadioServiceProxy(RadioSimProxy.class, null).isEmpty()
                && mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5);
    }

    @Override
    public void resetRadio(Message result) {
        throw new RuntimeException("resetRadio not expected to be called");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleCallSetupRequestFromSim(boolean accept, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class, result);
        if (!voiceProxy.isEmpty()) {
            RILRequest rr = obtainRequest(RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                voiceProxy.handleStkCallSetupRequestFromSim(rr.mSerial, accept);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(
                        VOICE_SERVICE, "handleStkCallSetupRequestFromSim", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getBarringInfo(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (networkProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_BARRING_INFO, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.getBarringInfo(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "getBarringInfo", e);
            }
        } else {
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "getBarringInfo: REQUEST_NOT_SUPPORTED");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void allocatePduSessionId(Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class, result);
        if (dataProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ALLOCATE_PDU_SESSION_ID, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                dataProxy.allocatePduSessionId(rr.mSerial);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(DATA_SERVICE, "allocatePduSessionId", e);
            }
        } else {
            AsyncResult.forMessage(result, null,
                    CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
            result.sendToTarget();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releasePduSessionId(Message result, int pduSessionId) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class, result);
        if (dataProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_RELEASE_PDU_SESSION_ID, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                dataProxy.releasePduSessionId(rr.mSerial, pduSessionId);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(DATA_SERVICE, "releasePduSessionId", e);
            }
        } else {
            AsyncResult.forMessage(result, null,
                    CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
            result.sendToTarget();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startHandover(Message result, int callId) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class, result);
        if (dataProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_START_HANDOVER, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                dataProxy.startHandover(rr.mSerial, callId);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(DATA_SERVICE, "startHandover", e);
            }
        } else {
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "startHandover: REQUEST_NOT_SUPPORTED");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelHandover(Message result, int callId) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class, result);
        if (dataProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CANCEL_HANDOVER, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                dataProxy.cancelHandover(rr.mSerial, callId);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(DATA_SERVICE, "cancelHandover", e);
            }
        } else {
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "cancelHandover: REQUEST_NOT_SUPPORTED");
            AsyncResult.forMessage(result, null,
                    CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
            result.sendToTarget();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getSlicingConfig(Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class, result);
        if (dataProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_SLICING_CONFIG, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                dataProxy.getSlicingConfig(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(DATA_SERVICE, "getSlicingConfig", e);
            }
        } else {
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "getSlicingConfig: REQUEST_NOT_SUPPORTED");
            AsyncResult.forMessage(result, null,
                    CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
            result.sendToTarget();
        }
    }

    @Override
    public void getSimPhonebookRecords(Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (simProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_SIM_PHONEBOOK_RECORDS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                simProxy.getSimPhonebookRecords(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "getSimPhonebookRecords", e);
            }
        } else {
            if (RILJ_LOGD) {
                Rlog.d(RILJ_LOG_TAG, "getSimPhonebookRecords: REQUEST_NOT_SUPPORTED");
            }
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void getSimPhonebookCapacity(Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (simProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_SIM_PHONEBOOK_CAPACITY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                simProxy.getSimPhonebookCapacity(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "getSimPhonebookCapacity", e);
            }
        } else {
            if (RILJ_LOGD) {
                Rlog.d(RILJ_LOG_TAG, "getSimPhonebookCapacity: REQUEST_NOT_SUPPORTED");
            }
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void updateSimPhonebookRecord(SimPhonebookRecord phonebookRecord, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class, result);
        if (simProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_UPDATE_SIM_PHONEBOOK_RECORD, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " with " + phonebookRecord.toString());
            }

            try {
                simProxy.updateSimPhonebookRecords(rr.mSerial, phonebookRecord);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(SIM_SERVICE, "updateSimPhonebookRecords", e);
            }
        } else {
            if (RILJ_LOGD) {
                Rlog.d(RILJ_LOG_TAG, "updateSimPhonebookRecords: REQUEST_NOT_SUPPORTED");
            }
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    /**
     * Set the UE's usage setting.
     *
     * @param result Callback message containing the success or failure status.
     * @param usageSetting the UE's usage setting, either VOICE_CENTRIC or DATA_CENTRIC.
     */
    @Override
    public void setUsageSetting(Message result,
            /* @TelephonyManager.UsageSetting */ int usageSetting) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (networkProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_2_0)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_USAGE_SETTING, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.setUsageSetting(rr.mSerial, usageSetting);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "setUsageSetting", e);
            }
        } else {
            if (RILJ_LOGD) {
                Rlog.d(RILJ_LOG_TAG, "setUsageSetting: REQUEST_NOT_SUPPORTED");
            }
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    /**
     * Get the UE's usage setting.
     *
     * @param result Callback message containing the usage setting (or a failure status).
     */
    @Override
    public void getUsageSetting(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class, result);
        if (networkProxy.isEmpty()) return;
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_2_0)) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_USAGE_SETTING, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }

            try {
                networkProxy.getUsageSetting(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(NETWORK_SERVICE, "getUsageSetting", e);
            }
        } else {
            if (RILJ_LOGD) {
                Rlog.d(RILJ_LOG_TAG, "getUsageSetting: REQUEST_NOT_SUPPORTED");
            }
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    //***** Private Methods
    /**
     * This is a helper function to be called when an indication callback is called for any radio
     * service. It takes care of acquiring wakelock and sending ack if needed.
     * @param service radio service the indication is for
     * @param indicationType indication type received
     */
    void processIndication(int service, int indicationType) {
        if (indicationType == RadioIndicationType.UNSOLICITED_ACK_EXP) {
            sendAck(service);
            if (RILJ_LOGD) riljLog("Unsol response received; Sending ack to ril.cpp");
        } else {
            // ack is not expected to be sent back. Nothing is required to be done here.
        }
    }

    void processRequestAck(int serial) {
        RILRequest rr;
        synchronized (mRequestList) {
            rr = mRequestList.get(serial);
        }
        if (rr == null) {
            Rlog.w(RILJ_LOG_TAG, "processRequestAck: Unexpected solicited ack response! "
                    + "serial: " + serial);
        } else {
            decrementWakeLock(rr);
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + " Ack < " + RILUtils.requestToString(rr.mRequest));
            }
        }
    }

    /**
     * This is a helper function to be called when a RadioResponse callback is called.
     * It takes care of acks, wakelocks, and finds and returns RILRequest corresponding to the
     * response if one is found.
     * @param responseInfo RadioResponseInfo received in response callback
     * @return RILRequest corresponding to the response
     */
    @VisibleForTesting
    public RILRequest processResponse(RadioResponseInfo responseInfo) {
        return processResponseInternal(RADIO_SERVICE, responseInfo.serial, responseInfo.error,
                responseInfo.type);
    }

    /**
     * This is a helper function for V1_6.RadioResponseInfo to be called when a RadioResponse
     * callback is called. It takes care of acks, wakelocks, and finds and returns RILRequest
     * corresponding to the response if one is found.
     * @param responseInfo RadioResponseInfo received in response callback
     * @return RILRequest corresponding to the response
     */
    @VisibleForTesting
    public RILRequest processResponse_1_6(
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo) {
        return processResponseInternal(RADIO_SERVICE, responseInfo.serial, responseInfo.error,
                responseInfo.type);
    }

    /**
     * This is a helper function for an AIDL RadioResponseInfo to be called when a RadioResponse
     * callback is called. It takes care of acks, wakelocks, and finds and returns RILRequest
     * corresponding to the response if one is found.
     * @param service Radio service that received the response
     * @param responseInfo RadioResponseInfo received in response callback
     * @return RILRequest corresponding to the response
     */
    @VisibleForTesting
    public RILRequest processResponse(int service,
            android.hardware.radio.RadioResponseInfo responseInfo) {
        return processResponseInternal(service, responseInfo.serial, responseInfo.error,
                responseInfo.type);
    }

    private RILRequest processResponseInternal(int service, int serial, int error, int type) {
        RILRequest rr;

        if (type == RadioResponseType.SOLICITED_ACK) {
            synchronized (mRequestList) {
                rr = mRequestList.get(serial);
            }
            if (rr == null) {
                Rlog.w(RILJ_LOG_TAG, "Unexpected solicited ack response! sn: " + serial);
            } else {
                decrementWakeLock(rr);
                if (mRadioBugDetector != null) {
                    mRadioBugDetector.detectRadioBug(rr.mRequest, error);
                }
                if (RILJ_LOGD) {
                    riljLog(rr.serialString() + " Ack from " + serviceToString(service)
                            + " < " + RILUtils.requestToString(rr.mRequest));
                }
            }
            return rr;
        }

        rr = findAndRemoveRequestFromList(serial);
        if (rr == null) {
            Rlog.e(RILJ_LOG_TAG, "processResponse: Unexpected response! serial: " + serial
                    + " ,error: " + error);
            return null;
        }

        // Time logging for RIL command and storing it in TelephonyHistogram.
        addToRilHistogram(rr);
        if (mRadioBugDetector != null) {
            mRadioBugDetector.detectRadioBug(rr.mRequest, error);
        }
        if (type == RadioResponseType.SOLICITED_ACK_EXP) {
            sendAck(service);
            if (RILJ_LOGD) {
                riljLog("Response received from " + serviceToString(service) + " for "
                        + rr.serialString() + " " + RILUtils.requestToString(rr.mRequest)
                        + " Sending ack to ril.cpp");
            }
        } else {
            // ack sent for SOLICITED_ACK_EXP above; nothing to do for SOLICITED response
        }

        // Here and below fake RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED, see b/7255789.
        // This is needed otherwise we don't automatically transition to the main lock
        // screen when the pin or puk is entered incorrectly.
        switch (rr.mRequest) {
            case RIL_REQUEST_ENTER_SIM_PUK:
            case RIL_REQUEST_ENTER_SIM_PUK2:
                if (mIccStatusChangedRegistrants != null) {
                    if (RILJ_LOGD) {
                        riljLog("ON enter sim puk fakeSimStatusChanged: reg count="
                                + mIccStatusChangedRegistrants.size());
                    }
                    mIccStatusChangedRegistrants.notifyRegistrants();
                }
                break;
            case RIL_REQUEST_SHUTDOWN:
                setRadioState(TelephonyManager.RADIO_POWER_UNAVAILABLE,
                        false /* forceNotifyRegistrants */);
                break;
        }

        if (error != RadioError.NONE) {
            switch (rr.mRequest) {
                case RIL_REQUEST_ENTER_SIM_PIN:
                case RIL_REQUEST_ENTER_SIM_PIN2:
                case RIL_REQUEST_CHANGE_SIM_PIN:
                case RIL_REQUEST_CHANGE_SIM_PIN2:
                case RIL_REQUEST_SET_FACILITY_LOCK:
                    if (mIccStatusChangedRegistrants != null) {
                        if (RILJ_LOGD) {
                            riljLog("ON some errors fakeSimStatusChanged: reg count="
                                    + mIccStatusChangedRegistrants.size());
                        }
                        mIccStatusChangedRegistrants.notifyRegistrants();
                    }
                    break;

            }
        } else {
            switch (rr.mRequest) {
                case RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND:
                if (mTestingEmergencyCall.getAndSet(false)) {
                    if (mEmergencyCallbackModeRegistrant != null) {
                        riljLog("testing emergency call, notify ECM Registrants");
                        mEmergencyCallbackModeRegistrant.notifyRegistrant();
                    }
                }
            }
        }
        return rr;
    }

    /**
     * This is a helper function to be called at the end of all RadioResponse callbacks.
     * It takes care of sending error response, logging, decrementing wakelock if needed, and
     * releases the request from memory pool.
     * @param rr RILRequest for which response callback was called
     * @param responseInfo RadioResponseInfo received in the callback
     * @param ret object to be returned to request sender
     */
    @VisibleForTesting
    public void processResponseDone(RILRequest rr, RadioResponseInfo responseInfo, Object ret) {
        processResponseDoneInternal(rr, responseInfo.error, responseInfo.type, ret);
    }

    /**
     * This is a helper function to be called at the end of the RadioResponse callbacks using for
     * V1_6.RadioResponseInfo.
     * It takes care of sending error response, logging, decrementing wakelock if needed, and
     * releases the request from memory pool.
     * @param rr RILRequest for which response callback was called
     * @param responseInfo RadioResponseInfo received in the callback
     * @param ret object to be returned to request sender
     */
    @VisibleForTesting
    public void processResponseDone_1_6(RILRequest rr,
            android.hardware.radio.V1_6.RadioResponseInfo responseInfo, Object ret) {
        processResponseDoneInternal(rr, responseInfo.error, responseInfo.type, ret);
    }

    /**
     * This is a helper function to be called at the end of the RadioResponse callbacks using for
     * RadioResponseInfo AIDL.
     * It takes care of sending error response, logging, decrementing wakelock if needed, and
     * releases the request from memory pool.
     * @param rr RILRequest for which response callback was called
     * @param responseInfo RadioResponseInfo received in the callback
     * @param ret object to be returned to request sender
     */
    @VisibleForTesting
    public void processResponseDone(RILRequest rr,
            android.hardware.radio.RadioResponseInfo responseInfo, Object ret) {
        processResponseDoneInternal(rr, responseInfo.error, responseInfo.type, ret);
    }

    private void processResponseDoneInternal(RILRequest rr, int rilError, int responseType,
            Object ret) {
        if (rilError == 0) {
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "< " + RILUtils.requestToString(rr.mRequest)
                        + " " + retToString(rr.mRequest, ret));
            }
        } else {
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "< " + RILUtils.requestToString(rr.mRequest)
                        + " error " + rilError);
            }
            rr.onError(rilError, ret);
        }
        processResponseCleanUp(rr, rilError, responseType, ret);
    }

    /**
     * This is a helper function to be called at the end of all RadioResponse callbacks for
     * radio HAL fallback cases. It takes care of logging, decrementing wakelock if needed, and
     * releases the request from memory pool. Unlike processResponseDone, it will not send
     * error response to caller.
     * @param rr RILRequest for which response callback was called
     * @param responseInfo RadioResponseInfo received in the callback
     * @param ret object to be returned to request sender
     */
    @VisibleForTesting
    public void processResponseFallback(RILRequest rr, RadioResponseInfo responseInfo, Object ret) {
        if (responseInfo.error == REQUEST_NOT_SUPPORTED && RILJ_LOGD) {
            riljLog(rr.serialString() + "< " + RILUtils.requestToString(rr.mRequest)
                    + " request not supported, falling back");
        }
        processResponseCleanUp(rr, responseInfo.error, responseInfo.type, ret);
    }

    private void processResponseCleanUp(RILRequest rr, int rilError, int responseType, Object ret) {
        if (rr != null) {
            mMetrics.writeOnRilSolicitedResponse(mPhoneId, rr.mSerial, rilError, rr.mRequest, ret);
            if (responseType == RadioResponseType.SOLICITED) {
                decrementWakeLock(rr);
            }
            rr.release();
        }
    }

    /**
     * Function to send ack and acquire related wakelock
     */
    private void sendAck(int service) {
        // TODO: Remove rr and clean up acquireWakelock for response and ack
        RILRequest rr = RILRequest.obtain(RIL_RESPONSE_ACKNOWLEDGEMENT, null,
                mRILDefaultWorkSource);
        acquireWakeLock(rr, FOR_ACK_WAKELOCK);
        if (service == RADIO_SERVICE) {
            IRadio radioProxy = getRadioProxy(null);
            if (radioProxy != null) {
                try {
                    radioProxy.responseAcknowledgement();
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(RADIO_SERVICE, "sendAck", e);
                    riljLoge("sendAck: " + e);
                }
            } else {
                Rlog.e(RILJ_LOG_TAG, "Error trying to send ack, radioProxy = null");
            }
        } else {
            RadioServiceProxy serviceProxy = getRadioServiceProxy(service, null);
            if (!serviceProxy.isEmpty()) {
                try {
                    serviceProxy.responseAcknowledgement();
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(service, "sendAck", e);
                    riljLoge("sendAck: " + e);
                }
            } else {
                Rlog.e(RILJ_LOG_TAG, "Error trying to send ack, serviceProxy is empty");
            }
        }
        rr.release();
    }

    private WorkSource getDefaultWorkSourceIfInvalid(WorkSource workSource) {
        if (workSource == null) {
            workSource = mRILDefaultWorkSource;
        }

        return workSource;
    }


    /**
     * Holds a PARTIAL_WAKE_LOCK whenever
     * a) There is outstanding RIL request sent to RIL deamon and no replied
     * b) There is a request pending to be sent out.
     *
     * There is a WAKE_LOCK_TIMEOUT to release the lock, though it shouldn't
     * happen often.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void acquireWakeLock(RILRequest rr, int wakeLockType) {
        synchronized (rr) {
            if (rr.mWakeLockType != INVALID_WAKELOCK) {
                Rlog.d(RILJ_LOG_TAG, "Failed to aquire wakelock for " + rr.serialString());
                return;
            }

            switch(wakeLockType) {
                case FOR_WAKELOCK:
                    synchronized (mWakeLock) {
                        mWakeLock.acquire();
                        mWakeLockCount++;
                        mWlSequenceNum++;

                        String clientId = rr.getWorkSourceClientId();
                        if (!mClientWakelockTracker.isClientActive(clientId)) {
                            mActiveWakelockWorkSource.add(rr.mWorkSource);
                            mWakeLock.setWorkSource(mActiveWakelockWorkSource);
                        }

                        mClientWakelockTracker.startTracking(rr.mClientId,
                                rr.mRequest, rr.mSerial, mWakeLockCount);

                        Message msg = mRilHandler.obtainMessage(EVENT_WAKE_LOCK_TIMEOUT);
                        msg.arg1 = mWlSequenceNum;
                        mRilHandler.sendMessageDelayed(msg, mWakeLockTimeout);
                    }
                    break;
                case FOR_ACK_WAKELOCK:
                    synchronized (mAckWakeLock) {
                        mAckWakeLock.acquire();
                        mAckWlSequenceNum++;

                        Message msg = mRilHandler.obtainMessage(EVENT_ACK_WAKE_LOCK_TIMEOUT);
                        msg.arg1 = mAckWlSequenceNum;
                        mRilHandler.sendMessageDelayed(msg, mAckWakeLockTimeout);
                    }
                    break;
                default: //WTF
                    Rlog.w(RILJ_LOG_TAG, "Acquiring Invalid Wakelock type " + wakeLockType);
                    return;
            }
            rr.mWakeLockType = wakeLockType;
        }
    }

    /** Returns the wake lock of the given type. */
    @VisibleForTesting
    public WakeLock getWakeLock(int wakeLockType) {
        return wakeLockType == FOR_WAKELOCK ? mWakeLock : mAckWakeLock;
    }

    /** Returns the {@link RilHandler} instance. */
    @VisibleForTesting
    public RilHandler getRilHandler() {
        return mRilHandler;
    }

    /** Returns the Ril request list. */
    @VisibleForTesting
    public SparseArray<RILRequest> getRilRequestList() {
        return mRequestList;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void decrementWakeLock(RILRequest rr) {
        synchronized (rr) {
            switch(rr.mWakeLockType) {
                case FOR_WAKELOCK:
                    synchronized (mWakeLock) {
                        mClientWakelockTracker.stopTracking(rr.mClientId,
                                rr.mRequest, rr.mSerial,
                                (mWakeLockCount > 1) ? mWakeLockCount - 1 : 0);
                        String clientId = rr.getWorkSourceClientId();
                        if (!mClientWakelockTracker.isClientActive(clientId)) {
                            mActiveWakelockWorkSource.remove(rr.mWorkSource);
                            mWakeLock.setWorkSource(mActiveWakelockWorkSource);
                        }

                        if (mWakeLockCount > 1) {
                            mWakeLockCount--;
                        } else {
                            mWakeLockCount = 0;
                            mWakeLock.release();
                        }
                    }
                    break;
                case FOR_ACK_WAKELOCK:
                    //We do not decrement the ACK wakelock
                    break;
                case INVALID_WAKELOCK:
                    break;
                default:
                    Rlog.w(RILJ_LOG_TAG, "Decrementing Invalid Wakelock type " + rr.mWakeLockType);
            }
            rr.mWakeLockType = INVALID_WAKELOCK;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean clearWakeLock(int wakeLockType) {
        if (wakeLockType == FOR_WAKELOCK) {
            synchronized (mWakeLock) {
                if (mWakeLockCount == 0 && !mWakeLock.isHeld()) return false;
                Rlog.d(RILJ_LOG_TAG, "NOTE: mWakeLockCount is " + mWakeLockCount
                        + "at time of clearing");
                mWakeLockCount = 0;
                mWakeLock.release();
                mClientWakelockTracker.stopTrackingAll();
                mActiveWakelockWorkSource = new WorkSource();
                return true;
            }
        } else {
            synchronized (mAckWakeLock) {
                if (!mAckWakeLock.isHeld()) return false;
                mAckWakeLock.release();
                return true;
            }
        }
    }

    /**
     * Release each request in mRequestList then clear the list
     * @param error is the RIL_Errno sent back
     * @param loggable true means to print all requests in mRequestList
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void clearRequestList(int error, boolean loggable) {
        RILRequest rr;
        synchronized (mRequestList) {
            int count = mRequestList.size();
            if (RILJ_LOGD && loggable) {
                Rlog.d(RILJ_LOG_TAG, "clearRequestList " + " mWakeLockCount="
                        + mWakeLockCount + " mRequestList=" + count);
            }

            for (int i = 0; i < count; i++) {
                rr = mRequestList.valueAt(i);
                if (RILJ_LOGD && loggable) {
                    Rlog.d(RILJ_LOG_TAG, i + ": [" + rr.mSerial + "] "
                            + RILUtils.requestToString(rr.mRequest));
                }
                rr.onError(error, null);
                decrementWakeLock(rr);
                rr.release();
            }
            mRequestList.clear();
        }
    }

    @UnsupportedAppUsage
    private RILRequest findAndRemoveRequestFromList(int serial) {
        RILRequest rr;
        synchronized (mRequestList) {
            rr = mRequestList.get(serial);
            if (rr != null) {
                mRequestList.remove(serial);
            }
        }

        return rr;
    }

    private void addToRilHistogram(RILRequest rr) {
        long endTime = SystemClock.elapsedRealtime();
        int totalTime = (int) (endTime - rr.mStartTimeMs);

        synchronized (sRilTimeHistograms) {
            TelephonyHistogram entry = sRilTimeHistograms.get(rr.mRequest);
            if (entry == null) {
                // We would have total #RIL_HISTOGRAM_BUCKET_COUNT range buckets for RIL commands
                entry = new TelephonyHistogram(TelephonyHistogram.TELEPHONY_CATEGORY_RIL,
                        rr.mRequest, RIL_HISTOGRAM_BUCKET_COUNT);
                sRilTimeHistograms.put(rr.mRequest, entry);
            }
            entry.addTimeTaken(totalTime);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    RadioCapability makeStaticRadioCapability() {
        // default to UNKNOWN so we fail fast.
        int raf = RadioAccessFamily.RAF_UNKNOWN;

        String rafString = mContext.getResources().getString(
                com.android.internal.R.string.config_radio_access_family);
        if (!TextUtils.isEmpty(rafString)) {
            raf = RadioAccessFamily.rafTypeFromString(rafString);
        }
        RadioCapability rc = new RadioCapability(mPhoneId.intValue(), 0, 0, raf,
                "", RadioCapability.RC_STATUS_SUCCESS);
        if (RILJ_LOGD) riljLog("Faking RIL_REQUEST_GET_RADIO_CAPABILITY response using " + raf);
        return rc;
    }

    @UnsupportedAppUsage
    static String retToString(int req, Object ret) {
        if (ret == null) return "";
        switch (req) {
            // Don't log these return values, for privacy's sake.
            case RIL_REQUEST_GET_IMSI:
            case RIL_REQUEST_GET_IMEI:
            case RIL_REQUEST_GET_IMEISV:
            case RIL_REQUEST_SIM_OPEN_CHANNEL:
            case RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL:

                if (!RILJ_LOGV) {
                    // If not versbose logging just return and don't display IMSI and IMEI, IMEISV
                    return "";
                }
        }

        StringBuilder sb;
        String s;
        int length;
        if (ret instanceof int[]) {
            int[] intArray = (int[]) ret;
            length = intArray.length;
            sb = new StringBuilder("{");
            if (length > 0) {
                int i = 0;
                sb.append(intArray[i++]);
                while (i < length) {
                    sb.append(", ").append(intArray[i++]);
                }
            }
            sb.append("}");
            s = sb.toString();
        } else if (ret instanceof String[]) {
            String[] strings = (String[]) ret;
            length = strings.length;
            sb = new StringBuilder("{");
            if (length > 0) {
                int i = 0;
                // position 0 is IMEI in RIL_REQUEST_DEVICE_IDENTITY
                if (req == RIL_REQUEST_DEVICE_IDENTITY) {
                    sb.append(Rlog.pii(RILJ_LOG_TAG, strings[i++]));
                } else {
                    sb.append(strings[i++]);
                }
                while (i < length) {
                    sb.append(", ").append(strings[i++]);
                }
            }
            sb.append("}");
            s = sb.toString();
        } else if (req == RIL_REQUEST_GET_CURRENT_CALLS) {
            ArrayList<DriverCall> calls = (ArrayList<DriverCall>) ret;
            sb = new StringBuilder("{");
            for (DriverCall dc : calls) {
                sb.append("[").append(dc).append("] ");
            }
            sb.append("}");
            s = sb.toString();
        } else if (req == RIL_REQUEST_GET_NEIGHBORING_CELL_IDS) {
            ArrayList<NeighboringCellInfo> cells = (ArrayList<NeighboringCellInfo>) ret;
            sb = new StringBuilder("{");
            for (NeighboringCellInfo cell : cells) {
                sb.append("[").append(cell).append("] ");
            }
            sb.append("}");
            s = sb.toString();
        } else if (req == RIL_REQUEST_QUERY_CALL_FORWARD_STATUS) {
            CallForwardInfo[] cinfo = (CallForwardInfo[]) ret;
            length = cinfo.length;
            sb = new StringBuilder("{");
            for (int i = 0; i < length; i++) {
                sb.append("[").append(cinfo[i]).append("] ");
            }
            sb.append("}");
            s = sb.toString();
        } else if (req == RIL_REQUEST_GET_HARDWARE_CONFIG) {
            ArrayList<HardwareConfig> hwcfgs = (ArrayList<HardwareConfig>) ret;
            sb = new StringBuilder(" ");
            for (HardwareConfig hwcfg : hwcfgs) {
                sb.append("[").append(hwcfg).append("] ");
            }
            s = sb.toString();
        } else {
            // Check if toString() was overridden. Java classes created from HIDL have a built-in
            // toString() method, but AIDL classes only have it if the parcelable contains a
            // @JavaDerive annotation. Manually convert to String as a backup for AIDL parcelables
            // missing the annotation.
            boolean toStringExists = false;
            try {
                toStringExists = ret.getClass().getMethod("toString").getDeclaringClass()
                        != Object.class;
            } catch (NoSuchMethodException e) {
                Rlog.e(RILJ_LOG_TAG, e.getMessage());
            }
            if (toStringExists) {
                s = ret.toString();
            } else {
                s = RILUtils.convertToString(ret) + " [convertToString]";
            }
        }
        return s;
    }

    void writeMetricsCallRing(char[] response) {
        mMetrics.writeRilCallRing(mPhoneId, response);
    }

    void writeMetricsSrvcc(int state) {
        mMetrics.writeRilSrvcc(mPhoneId, state);
        PhoneFactory.getPhone(mPhoneId).getVoiceCallSessionStats().onRilSrvccStateChanged(state);
    }

    void writeMetricsModemRestartEvent(String reason) {
        mMetrics.writeModemRestartEvent(mPhoneId, reason);
        // Write metrics to statsd. Generate metric only when modem reset is detected by the
        // first instance of RIL to avoid duplicated events.
        if (mPhoneId == 0) {
            ModemRestartStats.onModemRestart(reason);
        }
    }

    /**
     * Notify all registrants that the ril has connected or disconnected.
     *
     * @param rilVer is the version of the ril or -1 if disconnected.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    void notifyRegistrantsRilConnectionChanged(int rilVer) {
        mRilVersion = rilVer;
        if (mRilConnectedRegistrants != null) {
            mRilConnectedRegistrants.notifyRegistrants(
                    new AsyncResult(null, new Integer(rilVer), null));
        }
    }

    @UnsupportedAppUsage
    void notifyRegistrantsCdmaInfoRec(CdmaInformationRecords infoRec) {
        int response = RIL_UNSOL_CDMA_INFO_REC;
        if (infoRec.record instanceof CdmaInformationRecords.CdmaDisplayInfoRec) {
            if (mDisplayInfoRegistrants != null) {
                if (RILJ_LOGD) unsljLogRet(response, infoRec.record);
                mDisplayInfoRegistrants.notifyRegistrants(
                        new AsyncResult(null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaSignalInfoRec) {
            if (mSignalInfoRegistrants != null) {
                if (RILJ_LOGD) unsljLogRet(response, infoRec.record);
                mSignalInfoRegistrants.notifyRegistrants(
                        new AsyncResult(null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaNumberInfoRec) {
            if (mNumberInfoRegistrants != null) {
                if (RILJ_LOGD) unsljLogRet(response, infoRec.record);
                mNumberInfoRegistrants.notifyRegistrants(
                        new AsyncResult(null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaRedirectingNumberInfoRec) {
            if (mRedirNumInfoRegistrants != null) {
                if (RILJ_LOGD) unsljLogRet(response, infoRec.record);
                mRedirNumInfoRegistrants.notifyRegistrants(
                        new AsyncResult(null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaLineControlInfoRec) {
            if (mLineControlInfoRegistrants != null) {
                if (RILJ_LOGD) unsljLogRet(response, infoRec.record);
                mLineControlInfoRegistrants.notifyRegistrants(
                        new AsyncResult(null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaT53ClirInfoRec) {
            if (mT53ClirInfoRegistrants != null) {
                if (RILJ_LOGD) unsljLogRet(response, infoRec.record);
                mT53ClirInfoRegistrants.notifyRegistrants(
                        new AsyncResult(null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaT53AudioControlInfoRec) {
            if (mT53AudCntrlInfoRegistrants != null) {
                if (RILJ_LOGD) {
                    unsljLogRet(response, infoRec.record);
                }
                mT53AudCntrlInfoRegistrants.notifyRegistrants(
                        new AsyncResult(null, infoRec.record, null));
            }
        }
    }

    @UnsupportedAppUsage
    void riljLog(String msg) {
        Rlog.d(RILJ_LOG_TAG, msg + (" [PHONE" + mPhoneId + "]"));
    }

    void riljLoge(String msg) {
        Rlog.e(RILJ_LOG_TAG, msg + (" [PHONE" + mPhoneId + "]"));
    }

    void riljLogv(String msg) {
        Rlog.v(RILJ_LOG_TAG, msg + (" [PHONE" + mPhoneId + "]"));
    }

    @UnsupportedAppUsage
    void unsljLog(int response) {
        riljLog("[UNSL]< " + RILUtils.responseToString(response));
    }

    @UnsupportedAppUsage
    void unsljLogMore(int response, String more) {
        riljLog("[UNSL]< " + RILUtils.responseToString(response) + " " + more);
    }

    @UnsupportedAppUsage
    void unsljLogRet(int response, Object ret) {
        riljLog("[UNSL]< " + RILUtils.responseToString(response) + " "
                + retToString(response, ret));
    }

    @UnsupportedAppUsage
    void unsljLogvRet(int response, Object ret) {
        riljLogv("[UNSL]< " + RILUtils.responseToString(response) + " "
                + retToString(response, ret));
    }

    @Override
    public void setPhoneType(int phoneType) { // Called by GsmCdmaPhone
        if (RILJ_LOGD) riljLog("setPhoneType=" + phoneType + " old value=" + mPhoneType);
        mPhoneType = phoneType;
    }

    /* (non-Javadoc)
     * @see com.android.internal.telephony.BaseCommands#testingEmergencyCall()
     */
    @Override
    public void testingEmergencyCall() {
        if (RILJ_LOGD) riljLog("testingEmergencyCall");
        mTestingEmergencyCall.set(true);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("RIL: " + this);
        pw.println(" mWakeLock=" + mWakeLock);
        pw.println(" mWakeLockTimeout=" + mWakeLockTimeout);
        synchronized (mRequestList) {
            synchronized (mWakeLock) {
                pw.println(" mWakeLockCount=" + mWakeLockCount);
            }
            int count = mRequestList.size();
            pw.println(" mRequestList count=" + count);
            for (int i = 0; i < count; i++) {
                RILRequest rr = mRequestList.valueAt(i);
                pw.println("  [" + rr.mSerial + "] " + RILUtils.requestToString(rr.mRequest));
            }
        }
        pw.println(" mLastNITZTimeInfo=" + Arrays.toString(mLastNITZTimeInfo));
        pw.println(" mLastRadioPowerResult=" + mLastRadioPowerResult);
        pw.println(" mTestingEmergencyCall=" + mTestingEmergencyCall.get());
        mClientWakelockTracker.dumpClientRequestTracker(pw);
    }

    public List<ClientRequestStats> getClientRequestStats() {
        return mClientWakelockTracker.getClientRequestStats();
    }

    /**
     * Fixup for SignalStrength 1.0 to Assume GSM to WCDMA when
     * The current RAT type is one of the UMTS RATs.
     * @param signalStrength the initial signal strength
     * @return a new SignalStrength if RAT is UMTS or existing SignalStrength
     */
    public SignalStrength fixupSignalStrength10(SignalStrength signalStrength) {
        List<CellSignalStrengthGsm> gsmList = signalStrength.getCellSignalStrengths(
                CellSignalStrengthGsm.class);
        // If GSM is not the primary type, then bail out; no fixup needed.
        if (gsmList.isEmpty() || !gsmList.get(0).isValid()) {
            return signalStrength;
        }

        CellSignalStrengthGsm gsmStrength = gsmList.get(0);

        // Use the voice RAT which is a guarantee in GSM and UMTS
        int voiceRat = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
        Phone phone = PhoneFactory.getPhone(mPhoneId);
        if (phone != null) {
            ServiceState ss = phone.getServiceState();
            if (ss != null) {
                voiceRat = ss.getRilVoiceRadioTechnology();
            }
        }
        switch (voiceRat) {
            case ServiceState.RIL_RADIO_TECHNOLOGY_UMTS: /* fallthrough */
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSDPA: /* fallthrough */
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSUPA: /* fallthrough */
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSPA: /* fallthrough */
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSPAP: /* fallthrough */
                break;
            default:
                // If we are not currently on WCDMA/HSPA, then we don't need to do a fixup.
                return signalStrength;
        }

        // The service state reports WCDMA, and the SignalStrength is reported for GSM, so at this
        // point we take an educated guess that the GSM SignalStrength report is actually for
        // WCDMA. Also, if we are in WCDMA/GSM we can safely assume that there are no other valid
        // signal strength reports (no SRLTE, which is the only supported case in HAL 1.0).
        // Thus, we just construct a new SignalStrength and migrate RSSI and BER from the
        // GSM report to the WCDMA report, leaving everything else empty.
        return new SignalStrength(
                new CellSignalStrengthCdma(), new CellSignalStrengthGsm(),
                new CellSignalStrengthWcdma(gsmStrength.getRssi(),
                        gsmStrength.getBitErrorRate(),
                        CellInfo.UNAVAILABLE, CellInfo.UNAVAILABLE),
                new CellSignalStrengthTdscdma(), new CellSignalStrengthLte(),
                new CellSignalStrengthNr());
    }

    /**
     * Get the HAL version.
     *
     * @return the current HalVersion
     */
    public HalVersion getHalVersion() {
        return mRadioVersion;
    }

    private static String serviceToString(int service) {
        switch (service) {
            case RADIO_SERVICE:
                return "RADIO";
            case DATA_SERVICE:
                return "DATA";
            case MESSAGING_SERVICE:
                return "MESSAGING";
            case MODEM_SERVICE:
                return "MODEM";
            case NETWORK_SERVICE:
                return "NETWORK";
            case SIM_SERVICE:
                return "SIM";
            case VOICE_SERVICE:
                return "VOICE";
            default:
                return "UNKNOWN:" + service;
        }
    }
}
