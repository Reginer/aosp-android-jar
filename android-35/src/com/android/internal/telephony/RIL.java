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

import static android.telephony.TelephonyManager.HAL_SERVICE_DATA;
import static android.telephony.TelephonyManager.HAL_SERVICE_IMS;
import static android.telephony.TelephonyManager.HAL_SERVICE_MESSAGING;
import static android.telephony.TelephonyManager.HAL_SERVICE_MODEM;
import static android.telephony.TelephonyManager.HAL_SERVICE_NETWORK;
import static android.telephony.TelephonyManager.HAL_SERVICE_RADIO;
import static android.telephony.TelephonyManager.HAL_SERVICE_SIM;
import static android.telephony.TelephonyManager.HAL_SERVICE_VOICE;

import static com.android.internal.telephony.RILConstants.*;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.radio.V1_0.IRadio;
import android.hardware.radio.V1_0.RadioError;
import android.hardware.radio.V1_0.RadioIndicationType;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.RadioResponseType;
import android.hardware.radio.modem.ImeiInfo;
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
import android.os.Trace;
import android.os.WorkSource;
import android.provider.Settings;
import android.sysprop.TelephonyProperties;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.BarringInfo;
import android.telephony.CarrierRestrictionRules;
import android.telephony.ClientRequestStats;
import android.telephony.DomainSelectionService;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.ModemActivityInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.RadioAccessFamily;
import android.telephony.RadioAccessSpecifier;
import android.telephony.SignalThresholdInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyHistogram;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.HalService;
import android.telephony.TelephonyManager.PrefNetworkMode;
import android.telephony.data.DataProfile;
import android.telephony.data.NetworkSliceInfo;
import android.telephony.data.TrafficDescriptor;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.ims.RegistrationManager;
import android.telephony.ims.feature.ConnectionFailureInfo;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.text.TextUtils;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.cdma.CdmaInformationRecords;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.emergency.EmergencyConstants;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.imsphone.ImsCallInfo;
import com.android.internal.telephony.metrics.ModemRestartStats;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.nano.TelephonyProto.SmsSession;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.PersoSubState;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.SimPhonebookRecord;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RIL implementation of the CommandsInterface.
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
     * Wake lock timeout should be longer than the longest timeout in the vendor ril.
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
    public static final HalVersion RADIO_HAL_VERSION_UNSUPPORTED = HalVersion.UNSUPPORTED;

    /** @hide */
    public static final HalVersion RADIO_HAL_VERSION_UNKNOWN = HalVersion.UNKNOWN;

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

    /** @hide */
    public static final HalVersion RADIO_HAL_VERSION_2_1 = new HalVersion(2, 1);

    /** @hide */
    public static final HalVersion RADIO_HAL_VERSION_2_2 = new HalVersion(2, 2);

    // Hal version
    private final Map<Integer, HalVersion> mHalVersion = new HashMap<>();

    //***** Instance Variables

    @UnsupportedAppUsage
    @VisibleForTesting
    public final WakeLock mWakeLock;           // Wake lock associated with request/response
    @VisibleForTesting
    public final WakeLock mAckWakeLock;        // Wake lock associated with ack sent
    final int mWakeLockTimeout;         // Timeout associated with request/response
    final int mAckWakeLockTimeout;      // Timeout associated with ack sent
    // The number of wakelock requests currently active. Don't release the lock until dec'd to 0.
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

    Boolean mIsRadioVersion20Cached = null;

    // When we are testing emergency calls using ril.test.emergencynumber, this will trigger test
    // ECbM when the call is ended.
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    AtomicBoolean mTestingEmergencyCall = new AtomicBoolean(false);

    final Integer mPhoneId;

    private static final String PROPERTY_IS_VONR_ENABLED = "persist.radio.is_vonr_enabled_";

    public static final int MIN_SERVICE_IDX = HAL_SERVICE_RADIO;

    public static final int MAX_SERVICE_IDX = HAL_SERVICE_IMS;

    @NonNull private final FeatureFlags mFeatureFlags;

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
    private ImsResponse mImsResponse;
    private ImsIndication mImsIndication;
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

    private static final Map<String, Integer> FEATURES_TO_SERVICES = Map.ofEntries(
            Map.entry(PackageManager.FEATURE_TELEPHONY_CALLING, HAL_SERVICE_VOICE),
            Map.entry(PackageManager.FEATURE_TELEPHONY_DATA, HAL_SERVICE_DATA),
            Map.entry(PackageManager.FEATURE_TELEPHONY_MESSAGING, HAL_SERVICE_MESSAGING),
            Map.entry(PackageManager.FEATURE_TELEPHONY_IMS, HAL_SERVICE_IMS)
    );

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
                                riljLog("WAKE_LOCK_TIMEOUT mRequestList=" + count);
                                for (int i = 0; i < count; i++) {
                                    rr = mRequestList.valueAt(i);
                                    riljLog(i + ": [" + rr.mSerial + "] "
                                            + RILUtils.requestToString(rr.mRequest));
                                }
                            }
                        }
                    }
                    break;

                case EVENT_ACK_WAKE_LOCK_TIMEOUT:
                    if (msg.arg1 == mAckWlSequenceNum && clearWakeLock(FOR_ACK_WAKELOCK)) {
                        if (RILJ_LOGV) {
                            riljLog("ACK_WAKE_LOCK_TIMEOUT");
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
                    long msgCookie = (long) msg.obj;
                    if (mFeatureFlags.combineRilDeathHandle()) {
                        if (msgCookie == mServiceCookies.get(aidlService).get()) {
                            riljLog("handleMessage: EVENT_AIDL_PROXY_DEAD cookie = " + msgCookie
                                    + ", service = " + serviceToString(aidlService) + ", cookie = "
                                    + mServiceCookies.get(aidlService));
                            mIsRadioProxyInitialized = false;
                            resetProxyAndRequestList(aidlService);
                            // Remove duplicate death message to avoid duplicate reset.
                            mRilHandler.removeMessages(EVENT_AIDL_PROXY_DEAD);
                        } else {
                            riljLog("Ignore stale EVENT_AIDL_PROXY_DEAD for service "
                                    + serviceToString(aidlService));
                        }
                    } else {
                        riljLog("handleMessage: EVENT_AIDL_PROXY_DEAD cookie = " + msgCookie
                                + ", service = " + serviceToString(aidlService) + ", cookie = "
                                + mServiceCookies.get(aidlService));
                        if (msgCookie == mServiceCookies.get(aidlService).get()) {
                            mIsRadioProxyInitialized = false;
                            resetProxyAndRequestList(aidlService);
                        }
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
            if (mFeatureFlags.combineRilDeathHandle()) {
                mRilHandler.sendMessageAtFrontOfQueue(mRilHandler.obtainMessage(
                        EVENT_RADIO_PROXY_DEAD,
                        HAL_SERVICE_RADIO, 0 /* ignored arg2 */, cookie));
            } else {
                mRilHandler.sendMessage(mRilHandler.obtainMessage(EVENT_RADIO_PROXY_DEAD,
                        HAL_SERVICE_RADIO, 0 /* ignored arg2 */, cookie));
            }
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
                riljLog("Linked to death for service " + serviceToString(mService));
                mBinder = service;
                mBinder.linkToDeath(this, (int) mServiceCookies.get(mService).incrementAndGet());
            } else {
                riljLoge("Unable to link to death for service " + serviceToString(mService));
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
            if (mFeatureFlags.combineRilDeathHandle()) {
                mRilHandler.sendMessageAtFrontOfQueue(mRilHandler.obtainMessage(
                        EVENT_AIDL_PROXY_DEAD, mService, 0 /* ignored arg2 */,
                        mServiceCookies.get(mService).get()));
            } else {
                mRilHandler.sendMessage(mRilHandler.obtainMessage(EVENT_AIDL_PROXY_DEAD, mService,
                        0 /* ignored arg2 */, mServiceCookies.get(mService).get()));
            }
            unlinkToDeath();
        }
    }

    /**
     * Reset services. If one of the AIDL service is reset, all the other AIDL services will be
     * reset as well.
     * @param service The service to reset.
     */
    private synchronized void resetProxyAndRequestList(@HalService int service) {
        if (service == HAL_SERVICE_RADIO) {
            mRadioProxy = null;
            // Increment the cookie so that death notification can be ignored
            mServiceCookies.get(service).incrementAndGet();
        } else {
            if (mFeatureFlags.combineRilDeathHandle()) {
                // Reset all aidl services.
                for (int i = MIN_SERVICE_IDX; i <= MAX_SERVICE_IDX; i++) {
                    if (i == HAL_SERVICE_RADIO) continue;
                    if (mServiceProxies.get(i) == null) {
                        // This should only happen in tests
                        riljLoge("Null service proxy for service " + serviceToString(i));
                        continue;
                    }
                    mServiceProxies.get(i).clear();
                    // Increment the cookie so that death notification can be ignored
                    mServiceCookies.get(i).incrementAndGet();
                }
            } else {
                mServiceProxies.get(service).clear();
                // Increment the cookie so that death notification can be ignored
                mServiceCookies.get(service).incrementAndGet();
            }
        }

        setRadioState(TelephonyManager.RADIO_POWER_UNAVAILABLE, true /* forceNotifyRegistrants */);

        RILRequest.resetSerial();
        // Clear request list on close
        clearRequestList(RADIO_NOT_AVAILABLE, false);

        if (service == HAL_SERVICE_RADIO) {
            getRadioProxy();
        } else {
            if (mFeatureFlags.combineRilDeathHandle()) {
                // Reset all aidl services.
                for (int i = MIN_SERVICE_IDX; i <= MAX_SERVICE_IDX; i++) {
                    if (i == HAL_SERVICE_RADIO) continue;
                    if (mServiceProxies.get(i) == null) {
                        // This should only happen in tests
                        riljLoge("Null service proxy for service " + serviceToString(i));
                        continue;
                    }
                    getRadioServiceProxy(i);
                }
            } else {
                getRadioServiceProxy(service);
            }
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

            // Disable HIDL service
            if (mRadioProxy != null) {
                riljLog("Disable HIDL service");
                mDisabledRadioServices.get(HAL_SERVICE_RADIO).add(mPhoneId);
            }

            mMockModem.bindAllMockModemService();

            for (int service = MIN_SERVICE_IDX; service <= MAX_SERVICE_IDX; service++) {
                if (service == HAL_SERVICE_RADIO) continue;

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
                if (mFeatureFlags.combineRilDeathHandle()) {
                    // Reset both hidl and aidl proxies.
                    resetProxyAndRequestList(HAL_SERVICE_RADIO);
                    resetProxyAndRequestList(HAL_SERVICE_DATA);
                } else {
                    for (int service = MIN_SERVICE_IDX; service <= MAX_SERVICE_IDX; service++) {
                        resetProxyAndRequestList(service);
                    }
                }
            }
        }

        if ((serviceName == null) || (!serviceBound)) {
            if (serviceBound) riljLog("Unbinding to MockModemService");

            if (mDisabledRadioServices.get(HAL_SERVICE_RADIO).contains(mPhoneId)) {
                mDisabledRadioServices.get(HAL_SERVICE_RADIO).clear();
            }

            if (mMockModem != null) {
                mMockModem = null;
                for (int service = MIN_SERVICE_IDX; service <= MAX_SERVICE_IDX; service++) {
                    if (service == HAL_SERVICE_RADIO) {
                        if (isRadioVersion2_0()) {
                            mHalVersion.put(service, RADIO_HAL_VERSION_2_0);
                        } else {
                            mHalVersion.put(service, RADIO_HAL_VERSION_UNKNOWN);
                        }
                    } else {
                        if (isRadioServiceSupported(service)) {
                            mHalVersion.put(service, RADIO_HAL_VERSION_UNKNOWN);
                        } else {
                            mHalVersion.put(service, RADIO_HAL_VERSION_UNSUPPORTED);
                        }
                    }
                    if (!mFeatureFlags.combineRilDeathHandle()) {
                        resetProxyAndRequestList(service);
                    }
                }
                if (mFeatureFlags.combineRilDeathHandle()) {
                    // Reset both hidl and aidl proxies. Must be after cleaning mocked halVersion,
                    // otherwise an aidl service will be incorrectly considered as disabled.
                    resetProxyAndRequestList(HAL_SERVICE_RADIO);
                    resetProxyAndRequestList(HAL_SERVICE_DATA);
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
        // Do not allow to set same or greater versions
        if (oldVersion != null && halVersion.greaterOrEqual(oldVersion)) {
            riljLoge("setCompatVersion with equal or greater one, ignored, halVersion=" + halVersion
                    + ", oldVersion=" + oldVersion);
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
    public synchronized IRadio getRadioProxy() {
        if (mHalVersion.containsKey(HAL_SERVICE_RADIO)
                && mHalVersion.get(HAL_SERVICE_RADIO).greaterOrEqual(RADIO_HAL_VERSION_2_0)) {
            return null;
        }
        if (!SubscriptionManager.isValidPhoneId(mPhoneId)) return null;
        if (!mIsCellularSupported) {
            if (RILJ_LOGV) riljLog("getRadioProxy: Not calling getService(): wifi-only");
            return null;
        }

        if (mRadioProxy != null) {
            return mRadioProxy;
        }

        try {
            if (mDisabledRadioServices.get(HAL_SERVICE_RADIO).contains(mPhoneId)) {
                riljLoge("getRadioProxy: mRadioProxy for " + HIDL_SERVICE_NAME[mPhoneId]
                        + " is disabled");
                return null;
            } else {
                try {
                    mRadioProxy = android.hardware.radio.V1_6.IRadio.getService(
                            HIDL_SERVICE_NAME[mPhoneId], true);
                    mHalVersion.put(HAL_SERVICE_RADIO, RADIO_HAL_VERSION_1_6);
                } catch (NoSuchElementException e) {
                }

                if (mRadioProxy == null) {
                    try {
                        mRadioProxy = android.hardware.radio.V1_5.IRadio.getService(
                                HIDL_SERVICE_NAME[mPhoneId], true);
                        mHalVersion.put(HAL_SERVICE_RADIO, RADIO_HAL_VERSION_1_5);
                    } catch (NoSuchElementException e) {
                    }
                }

                if (mRadioProxy == null) {
                    try {
                        mRadioProxy = android.hardware.radio.V1_4.IRadio.getService(
                                HIDL_SERVICE_NAME[mPhoneId], true);
                        mHalVersion.put(HAL_SERVICE_RADIO, RADIO_HAL_VERSION_1_4);
                    } catch (NoSuchElementException e) {
                    }
                }

                if (mRadioProxy == null) {
                    riljLoge("IRadio <1.4 is no longer supported.");
                }

                if (mRadioProxy != null) {
                    if (!mIsRadioProxyInitialized) {
                        mIsRadioProxyInitialized = true;
                        mRadioProxy.linkToDeath(mRadioProxyDeathRecipient,
                                mServiceCookies.get(HAL_SERVICE_RADIO).incrementAndGet());
                        mRadioProxy.setResponseFunctions(mRadioResponse, mRadioIndication);
                    }
                } else {
                    mDisabledRadioServices.get(HAL_SERVICE_RADIO).add(mPhoneId);
                    riljLoge("getRadioProxy: set mRadioProxy for "
                            + HIDL_SERVICE_NAME[mPhoneId] + " as disabled");
                    return null;
                }
            }
        } catch (RemoteException e) {
            mRadioProxy = null;
            riljLoge("RadioProxy getService/setResponseFunctions: " + e);
        }

        if (mRadioProxy == null) {
            // getService() is a blocking call, so this should never happen
            riljLoge("getRadioProxy: mRadioProxy == null");
        }

        return mRadioProxy;
    }

    /**
     * Returns a {@link RadioDataProxy}, {@link RadioMessagingProxy}, {@link RadioModemProxy},
     * {@link RadioNetworkProxy}, {@link RadioSimProxy}, {@link RadioVoiceProxy},
     * {@link RadioImsProxy}, or null if the service is not available.
     */
    @NonNull
    public <T extends RadioServiceProxy> T getRadioServiceProxy(Class<T> serviceClass) {
        if (serviceClass == RadioDataProxy.class) {
            return (T) getRadioServiceProxy(HAL_SERVICE_DATA);
        }
        if (serviceClass == RadioMessagingProxy.class) {
            return (T) getRadioServiceProxy(HAL_SERVICE_MESSAGING);
        }
        if (serviceClass == RadioModemProxy.class) {
            return (T) getRadioServiceProxy(HAL_SERVICE_MODEM);
        }
        if (serviceClass == RadioNetworkProxy.class) {
            return (T) getRadioServiceProxy(HAL_SERVICE_NETWORK);
        }
        if (serviceClass == RadioSimProxy.class) {
            return (T) getRadioServiceProxy(HAL_SERVICE_SIM);
        }
        if (serviceClass == RadioVoiceProxy.class) {
            return (T) getRadioServiceProxy(HAL_SERVICE_VOICE);
        }
        if (serviceClass == RadioImsProxy.class) {
            return (T) getRadioServiceProxy(HAL_SERVICE_IMS);
        }
        riljLoge("getRadioServiceProxy: unrecognized " + serviceClass);
        return null;
    }

    /**
     * Returns a {@link RadioServiceProxy}, which is empty if the service is not available.
     * For HAL_SERVICE_RADIO, use {@link #getRadioProxy} instead, as this will always return null.
     */
    @VisibleForTesting
    @NonNull
    public synchronized RadioServiceProxy getRadioServiceProxy(int service) {
        if (!SubscriptionManager.isValidPhoneId(mPhoneId)) return mServiceProxies.get(service);
        if ((service >= HAL_SERVICE_IMS) && !isRadioServiceSupported(service)) {
            // Suppress the excessive logging for HAL_SERVICE_IMS when not supported.
            if (service != HAL_SERVICE_IMS) {
                riljLogw("getRadioServiceProxy: " + serviceToString(service) + " for "
                        + HIDL_SERVICE_NAME[mPhoneId] + " is not supported\n"
                        + android.util.Log.getStackTraceString(new RuntimeException()));
            }
            return mServiceProxies.get(service);
        }
        if (!mIsCellularSupported) {
            if (RILJ_LOGV) riljLog("getRadioServiceProxy: Not calling getService(): wifi-only");
            return mServiceProxies.get(service);
        }

        RadioServiceProxy serviceProxy = mServiceProxies.get(service);
        if (!serviceProxy.isEmpty()) {
            return serviceProxy;
        }

        try {
            if (mMockModem == null && mDisabledRadioServices.get(service).contains(mPhoneId)) {
                riljLoge("getRadioServiceProxy: " + serviceToString(service) + " for "
                        + HIDL_SERVICE_NAME[mPhoneId] + " is disabled\n"
                        + android.util.Log.getStackTraceString(new RuntimeException()));
                return null;
            } else {
                IBinder binder;
                switch (service) {
                    case HAL_SERVICE_DATA:
                        if (mMockModem == null) {
                            binder = ServiceManager.waitForDeclaredService(
                                    android.hardware.radio.data.IRadioData.DESCRIPTOR + "/"
                                            + HIDL_SERVICE_NAME[mPhoneId]);
                        } else {
                            binder = mMockModem.getServiceBinder(HAL_SERVICE_DATA);
                        }
                        if (binder != null) {
                            mHalVersion.put(service, ((RadioDataProxy) serviceProxy).setAidl(
                                    mHalVersion.get(service),
                                    android.hardware.radio.data.IRadioData.Stub.asInterface(
                                            binder)));
                        }
                        break;
                    case HAL_SERVICE_MESSAGING:
                        if (mMockModem == null) {
                            binder = ServiceManager.waitForDeclaredService(
                                    android.hardware.radio.messaging.IRadioMessaging.DESCRIPTOR
                                            + "/" + HIDL_SERVICE_NAME[mPhoneId]);
                        } else {
                            binder = mMockModem.getServiceBinder(HAL_SERVICE_MESSAGING);
                        }
                        if (binder != null) {
                            mHalVersion.put(service, ((RadioMessagingProxy) serviceProxy).setAidl(
                                    mHalVersion.get(service),
                                    android.hardware.radio.messaging.IRadioMessaging.Stub
                                            .asInterface(binder)));
                        }
                        break;
                    case HAL_SERVICE_MODEM:
                        if (mMockModem == null) {
                            binder = ServiceManager.waitForDeclaredService(
                                    android.hardware.radio.modem.IRadioModem.DESCRIPTOR + "/"
                                            + HIDL_SERVICE_NAME[mPhoneId]);
                        } else {
                            binder = mMockModem.getServiceBinder(HAL_SERVICE_MODEM);
                        }
                        if (binder != null) {
                            mHalVersion.put(service, ((RadioModemProxy) serviceProxy).setAidl(
                                    mHalVersion.get(service),
                                    android.hardware.radio.modem.IRadioModem.Stub
                                            .asInterface(binder)));
                        }
                        break;
                    case HAL_SERVICE_NETWORK:
                        if (mMockModem == null) {
                            binder = ServiceManager.waitForDeclaredService(
                                    android.hardware.radio.network.IRadioNetwork.DESCRIPTOR + "/"
                                            + HIDL_SERVICE_NAME[mPhoneId]);
                        } else {
                            binder = mMockModem.getServiceBinder(HAL_SERVICE_NETWORK);
                        }
                        if (binder != null) {
                            mHalVersion.put(service, ((RadioNetworkProxy) serviceProxy).setAidl(
                                    mHalVersion.get(service),
                                    android.hardware.radio.network.IRadioNetwork.Stub
                                            .asInterface(binder)));
                        }
                        break;
                    case HAL_SERVICE_SIM:
                        if (mMockModem == null) {
                            binder = ServiceManager.waitForDeclaredService(
                                    android.hardware.radio.sim.IRadioSim.DESCRIPTOR + "/"
                                            + HIDL_SERVICE_NAME[mPhoneId]);
                        } else {
                            binder = mMockModem.getServiceBinder(HAL_SERVICE_SIM);
                        }
                        if (binder != null) {
                            mHalVersion.put(service, ((RadioSimProxy) serviceProxy).setAidl(
                                    mHalVersion.get(service),
                                    android.hardware.radio.sim.IRadioSim.Stub
                                            .asInterface(binder)));
                        }
                        break;
                    case HAL_SERVICE_VOICE:
                        if (mMockModem == null) {
                            binder = ServiceManager.waitForDeclaredService(
                                    android.hardware.radio.voice.IRadioVoice.DESCRIPTOR + "/"
                                            + HIDL_SERVICE_NAME[mPhoneId]);
                        } else {
                            binder = mMockModem.getServiceBinder(HAL_SERVICE_VOICE);
                        }
                        if (binder != null) {
                            mHalVersion.put(service, ((RadioVoiceProxy) serviceProxy).setAidl(
                                    mHalVersion.get(service),
                                    android.hardware.radio.voice.IRadioVoice.Stub
                                            .asInterface(binder)));
                        }
                        break;
                    case HAL_SERVICE_IMS:
                        if (mMockModem == null) {
                            binder = ServiceManager.waitForDeclaredService(
                                    android.hardware.radio.ims.IRadioIms.DESCRIPTOR + "/"
                                            + HIDL_SERVICE_NAME[mPhoneId]);
                        } else {
                            binder = mMockModem.getServiceBinder(HAL_SERVICE_IMS);
                        }
                        if (binder != null) {
                            mHalVersion.put(service, ((RadioImsProxy) serviceProxy).setAidl(
                                    mHalVersion.get(service),
                                    android.hardware.radio.ims.IRadioIms.Stub
                                            .asInterface(binder)));
                        }
                        break;
                }

                if (serviceProxy.isEmpty()
                        && mHalVersion.get(service).less(RADIO_HAL_VERSION_2_0)) {
                    try {
                        mHalVersion.put(service, RADIO_HAL_VERSION_1_6);
                        serviceProxy.setHidl(mHalVersion.get(service),
                                android.hardware.radio.V1_6.IRadio.getService(
                                        HIDL_SERVICE_NAME[mPhoneId], true));
                    } catch (NoSuchElementException e) {
                    }
                }

                if (serviceProxy.isEmpty()
                        && mHalVersion.get(service).less(RADIO_HAL_VERSION_2_0)) {
                    try {
                        mHalVersion.put(service, RADIO_HAL_VERSION_1_5);
                        serviceProxy.setHidl(mHalVersion.get(service),
                                android.hardware.radio.V1_5.IRadio.getService(
                                        HIDL_SERVICE_NAME[mPhoneId], true));
                    } catch (NoSuchElementException e) {
                    }
                }

                if (serviceProxy.isEmpty()
                        && mHalVersion.get(service).less(RADIO_HAL_VERSION_2_0)) {
                    try {
                        mHalVersion.put(service, RADIO_HAL_VERSION_1_4);
                        serviceProxy.setHidl(mHalVersion.get(service),
                                android.hardware.radio.V1_4.IRadio.getService(
                                        HIDL_SERVICE_NAME[mPhoneId], true));
                    } catch (NoSuchElementException e) {
                    }
                }

                if (serviceProxy.isEmpty()
                        && mHalVersion.get(service).less(RADIO_HAL_VERSION_2_0)) {
                    riljLoge("IRadio <1.4 is no longer supported.");
                }

                if (!serviceProxy.isEmpty()) {
                    if (serviceProxy.isAidl()) {
                        switch (service) {
                            case HAL_SERVICE_DATA:
                                mDeathRecipients.get(service).linkToDeath(
                                        ((RadioDataProxy) serviceProxy).getAidl().asBinder());
                                ((RadioDataProxy) serviceProxy).getAidl().setResponseFunctions(
                                        mDataResponse, mDataIndication);
                                break;
                            case HAL_SERVICE_MESSAGING:
                                mDeathRecipients.get(service).linkToDeath(
                                        ((RadioMessagingProxy) serviceProxy).getAidl().asBinder());
                                ((RadioMessagingProxy) serviceProxy).getAidl().setResponseFunctions(
                                        mMessagingResponse, mMessagingIndication);
                                break;
                            case HAL_SERVICE_MODEM:
                                mDeathRecipients.get(service).linkToDeath(
                                        ((RadioModemProxy) serviceProxy).getAidl().asBinder());
                                ((RadioModemProxy) serviceProxy).getAidl().setResponseFunctions(
                                        mModemResponse, mModemIndication);
                                break;
                            case HAL_SERVICE_NETWORK:
                                mDeathRecipients.get(service).linkToDeath(
                                        ((RadioNetworkProxy) serviceProxy).getAidl().asBinder());
                                ((RadioNetworkProxy) serviceProxy).getAidl().setResponseFunctions(
                                        mNetworkResponse, mNetworkIndication);
                                break;
                            case HAL_SERVICE_SIM:
                                mDeathRecipients.get(service).linkToDeath(
                                        ((RadioSimProxy) serviceProxy).getAidl().asBinder());
                                ((RadioSimProxy) serviceProxy).getAidl().setResponseFunctions(
                                        mSimResponse, mSimIndication);
                                break;
                            case HAL_SERVICE_VOICE:
                                mDeathRecipients.get(service).linkToDeath(
                                        ((RadioVoiceProxy) serviceProxy).getAidl().asBinder());
                                ((RadioVoiceProxy) serviceProxy).getAidl().setResponseFunctions(
                                        mVoiceResponse, mVoiceIndication);
                                break;
                            case HAL_SERVICE_IMS:
                                mDeathRecipients.get(service).linkToDeath(
                                        ((RadioImsProxy) serviceProxy).getAidl().asBinder());
                                ((RadioImsProxy) serviceProxy).getAidl().setResponseFunctions(
                                        mImsResponse, mImsIndication);
                                break;
                        }
                    } else {
                        if (mHalVersion.get(service).greaterOrEqual(RADIO_HAL_VERSION_2_0)) {
                            throw new AssertionError("serviceProxy shouldn't be HIDL with HAL 2.0");
                        }
                        if (!mIsRadioProxyInitialized) {
                            mIsRadioProxyInitialized = true;
                            serviceProxy.getHidl().linkToDeath(mRadioProxyDeathRecipient,
                                    mServiceCookies.get(HAL_SERVICE_RADIO).incrementAndGet());
                            serviceProxy.getHidl().setResponseFunctions(
                                    mRadioResponse, mRadioIndication);
                        }
                    }
                } else {
                    mDisabledRadioServices.get(service).add(mPhoneId);
                    mHalVersion.put(service, RADIO_HAL_VERSION_UNKNOWN);
                    riljLoge("getRadioServiceProxy: set " + serviceToString(service) + " for "
                            + HIDL_SERVICE_NAME[mPhoneId] + " as disabled\n"
                            + android.util.Log.getStackTraceString(new RuntimeException()));
                }
            }
        } catch (RemoteException e) {
            serviceProxy.clear();
            riljLoge("ServiceProxy getService/setResponseFunctions: " + e);
        }

        if (serviceProxy.isEmpty()) {
            // getService() is a blocking call, so this should never happen
            riljLoge("getRadioServiceProxy: serviceProxy == null");
        }

        return serviceProxy;
    }

    @Override
    public synchronized void onSlotActiveStatusChange(boolean active) {
        mIsRadioProxyInitialized = false;
        if (mFeatureFlags.combineRilDeathHandle()) {
            if (active) {
                for (int service = MIN_SERVICE_IDX; service <= MAX_SERVICE_IDX; service++) {
                    // Try to connect to RIL services and set response functions.
                    if (service == HAL_SERVICE_RADIO) {
                        getRadioProxy();
                    } else {
                        getRadioServiceProxy(service);
                    }
                }
            } else {
                // Reset both hidl and aidl proxies
                resetProxyAndRequestList(HAL_SERVICE_RADIO);
                resetProxyAndRequestList(HAL_SERVICE_DATA);
            }
        } else {
            for (int service = MIN_SERVICE_IDX; service <= MAX_SERVICE_IDX; service++) {
                if (active) {
                    // Try to connect to RIL services and set response functions.
                    if (service == HAL_SERVICE_RADIO) {
                        getRadioProxy();
                    } else {
                        getRadioServiceProxy(service);
                    }
                } else {
                    resetProxyAndRequestList(service);
                }
            }
        }
    }

    //***** Constructors

    @UnsupportedAppUsage
    public RIL(Context context, int allowedNetworkTypes, int cdmaSubscription, Integer instanceId,
            @NonNull FeatureFlags flags) {
        this(context, allowedNetworkTypes, cdmaSubscription, instanceId, null, flags);
    }

    @VisibleForTesting
    public RIL(Context context, int allowedNetworkTypes, int cdmaSubscription, Integer instanceId,
            SparseArray<RadioServiceProxy> proxies, @NonNull FeatureFlags flags) {
        super(context);
        mFeatureFlags = flags;
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
            if (isRadioVersion2_0()) {
                mHalVersion.put(HAL_SERVICE_RADIO, RADIO_HAL_VERSION_2_0);
            } else {
                mHalVersion.put(HAL_SERVICE_RADIO, RADIO_HAL_VERSION_UNKNOWN);
            }
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
        mImsResponse = new ImsResponse(this);
        mImsIndication = new ImsIndication(this);
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
            if (service != HAL_SERVICE_RADIO) {
                try {
                    if (isRadioServiceSupported(service)) {
                        mHalVersion.put(service, RADIO_HAL_VERSION_UNKNOWN);
                    } else {
                        mHalVersion.put(service, RADIO_HAL_VERSION_UNSUPPORTED);
                    }
                } catch (SecurityException ex) {
                    /* TODO(b/211920208): instead of the following workaround (guessing if
                     * we're in a test based on proxies being populated), mock ServiceManager
                     * to not throw SecurityException and return correct value based on what
                     * HAL we're testing. */
                    if (proxies == null) throw ex;
                }
                mDeathRecipients.put(service, new BinderServiceDeathRecipient(service));
            }
            mDisabledRadioServices.put(service, new HashSet<>());
            mServiceCookies.put(service, new AtomicLong(0));
        }
        if (proxies == null) {
            mServiceProxies.put(HAL_SERVICE_DATA, new RadioDataProxy());
            mServiceProxies.put(HAL_SERVICE_MESSAGING, new RadioMessagingProxy());
            mServiceProxies.put(HAL_SERVICE_MODEM, new RadioModemProxy());
            mServiceProxies.put(HAL_SERVICE_NETWORK, new RadioNetworkProxy());
            mServiceProxies.put(HAL_SERVICE_SIM, new RadioSimProxy());
            mServiceProxies.put(HAL_SERVICE_VOICE, new RadioVoiceProxy());
            mServiceProxies.put(HAL_SERVICE_IMS, new RadioImsProxy());
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

        validateFeatureFlags();

        // Set radio callback; needed to set RadioIndication callback (should be done after
        // wakelock stuff is initialized above as callbacks are received on separate binder threads)
        for (int service = MIN_SERVICE_IDX; service <= MAX_SERVICE_IDX; service++) {
            if (isRadioVersion2_0() && !isRadioServiceSupported(service)) {
                riljLog("Not initializing " + serviceToString(service) + " (not supported)");
                continue;
            }

            if (service == HAL_SERVICE_RADIO) {
                getRadioProxy();
            } else {
                if (proxies == null) {
                    // Prevent telephony tests from calling the service
                    getRadioServiceProxy(service);
                }
            }

            if (RILJ_LOGD) {
                riljLog("HAL version of " + serviceToString(service)
                        + ": " + mHalVersion.get(service));
            }
        }
    }

    private boolean isRadioVersion2_0() {
        if (mIsRadioVersion20Cached != null) return mIsRadioVersion20Cached;
        for (int service = HAL_SERVICE_DATA; service <= MAX_SERVICE_IDX; service++) {
            if (isRadioServiceSupported(service)) {
                return mIsRadioVersion20Cached = true;
            }
        }
        return mIsRadioVersion20Cached = false;
    }

    private boolean isRadioServiceSupported(int service) {
        String serviceName = "";

        if (service == HAL_SERVICE_RADIO) {
            return true;
        }

        switch (service) {
            case HAL_SERVICE_DATA:
                serviceName = android.hardware.radio.data.IRadioData.DESCRIPTOR;
                break;
            case HAL_SERVICE_MESSAGING:
                serviceName = android.hardware.radio.messaging.IRadioMessaging.DESCRIPTOR;
                break;
            case HAL_SERVICE_MODEM:
                serviceName = android.hardware.radio.modem.IRadioModem.DESCRIPTOR;
                break;
            case HAL_SERVICE_NETWORK:
                serviceName = android.hardware.radio.network.IRadioNetwork.DESCRIPTOR;
                break;
            case HAL_SERVICE_SIM:
                serviceName = android.hardware.radio.sim.IRadioSim.DESCRIPTOR;
                break;
            case HAL_SERVICE_VOICE:
                serviceName = android.hardware.radio.voice.IRadioVoice.DESCRIPTOR;
                break;
            case HAL_SERVICE_IMS:
                serviceName = android.hardware.radio.ims.IRadioIms.DESCRIPTOR;
                break;
        }

        if (!serviceName.equals("")
                && ServiceManager.isDeclared(serviceName + '/' + HIDL_SERVICE_NAME[mPhoneId])) {
            return true;
        }

        return false;
    }

    private void validateFeatureFlags() {
        PackageManager pm = mContext.getPackageManager();
        for (var entry : FEATURES_TO_SERVICES.entrySet()) {
            String feature = entry.getKey();
            int service = entry.getValue();

            boolean hasFeature = pm.hasSystemFeature(feature);
            boolean hasService = isRadioServiceSupported(service);

            if (hasFeature && !hasService) {
                riljLoge("Feature " + feature + " is declared, but service "
                        + serviceToString(service) + " is missing");
            }
            if (!hasFeature && hasService) {
                riljLoge("Service " + serviceToString(service) + " is available, but feature "
                        + feature + " is not declared");
            }
        }
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
        Trace.asyncTraceForTrackBegin(
                Trace.TRACE_TAG_NETWORK, "RIL", rr.mSerial + "> "
                + RILUtils.requestToString(rr.mRequest), rr.mSerial);
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

    private void radioServiceInvokeHelper(int service, RILRequest rr, String methodName,
            FunctionalUtils.ThrowingRunnable helper) {
        try {
            helper.runOrThrow();
        } catch (RuntimeException e) {
            riljLoge(methodName + " RuntimeException: " + e);
            int error = RadioError.SYSTEM_ERR;
            int responseType = RadioResponseType.SOLICITED;
            processResponseInternal(service, rr.mSerial, error, responseType);
            processResponseDoneInternal(rr, error, responseType, null);
        } catch (Exception e) {
            handleRadioProxyExceptionForRR(service, methodName, e);
        }
    }

    private boolean canMakeRequest(String request, RadioServiceProxy proxy, Message result,
            HalVersion version) {
        int service = HAL_SERVICE_RADIO;
        if (proxy instanceof RadioDataProxy) {
            service = HAL_SERVICE_DATA;
        } else if (proxy instanceof RadioMessagingProxy) {
            service = HAL_SERVICE_MESSAGING;
        } else if (proxy instanceof RadioModemProxy) {
            service = HAL_SERVICE_MODEM;
        } else if (proxy instanceof RadioNetworkProxy) {
            service = HAL_SERVICE_NETWORK;
        } else if (proxy instanceof RadioSimProxy) {
            service = HAL_SERVICE_SIM;
        } else if (proxy instanceof RadioVoiceProxy) {
            service = HAL_SERVICE_VOICE;
        } else if (proxy instanceof RadioImsProxy) {
            service = HAL_SERVICE_IMS;
        }
        if (proxy == null || proxy.isEmpty()) {
            riljLoge(String.format("Unable to complete %s because service %s is not available.",
                    request, serviceToString(service)));
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(RADIO_NOT_AVAILABLE));
                result.sendToTarget();
            }
            return false;
        }
        if (mHalVersion.get(service).less(version)) {
            riljLoge(String.format("%s not supported on service %s < %s.",
                    request, serviceToString(service), version));
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
            return false;
        }
        return true;
    }

    @Override
    public void getIccCardStatus(Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("getIccCardStatus", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_SIM_STATUS, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "getIccCardStatus", () -> {
            simProxy.getIccCardStatus(rr.mSerial);
        });
    }

    @Override
    public void supplyIccPin(String pin, Message result) {
        supplyIccPinForApp(pin, null, result);
    }

    @Override
    public void supplyIccPinForApp(String pin, String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("supplyIccPinForApp", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_PIN, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " aid = " + aid);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "supplyIccPinForApp", () -> {
            simProxy.supplyIccPinForApp(rr.mSerial, RILUtils.convertNullToEmptyString(pin),
                    RILUtils.convertNullToEmptyString(aid));
        });
    }

    @Override
    public void supplyIccPuk(String puk, String newPin, Message result) {
        supplyIccPukForApp(puk, newPin, null, result);
    }

    @Override
    public void supplyIccPukForApp(String puk, String newPin, String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("supplyIccPukForApp", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_PUK, result, mRILDefaultWorkSource);

        String pukStr = RILUtils.convertNullToEmptyString(puk);
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " isPukEmpty = " + pukStr.isEmpty() + " aid = " + aid);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "supplyIccPukForApp", () -> {
            simProxy.supplyIccPukForApp(rr.mSerial, pukStr,
                    RILUtils.convertNullToEmptyString(newPin),
                    RILUtils.convertNullToEmptyString(aid));
        });
    }

    @Override
    public void supplyIccPin2(String pin, Message result) {
        supplyIccPin2ForApp(pin, null, result);
    }

    @Override
    public void supplyIccPin2ForApp(String pin, String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("supplyIccPin2ForApp", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_PIN2, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " aid = " + aid);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "supplyIccPin2ForApp", () -> {
            simProxy.supplyIccPin2ForApp(rr.mSerial, RILUtils.convertNullToEmptyString(pin),
                    RILUtils.convertNullToEmptyString(aid));
        });
    }

    @Override
    public void supplyIccPuk2(String puk2, String newPin2, Message result) {
        supplyIccPuk2ForApp(puk2, newPin2, null, result);
    }

    @Override
    public void supplyIccPuk2ForApp(String puk, String newPin2, String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("supplyIccPuk2ForApp", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_PUK2, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " aid = " + aid);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "supplyIccPuk2ForApp", () -> {
            simProxy.supplyIccPuk2ForApp(rr.mSerial, RILUtils.convertNullToEmptyString(puk),
                    RILUtils.convertNullToEmptyString(newPin2),
                    RILUtils.convertNullToEmptyString(aid));
        });
    }

    @Override
    public void changeIccPin(String oldPin, String newPin, Message result) {
        changeIccPinForApp(oldPin, newPin, null, result);
    }

    @Override
    public void changeIccPinForApp(String oldPin, String newPin, String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("changeIccPinForApp", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CHANGE_SIM_PIN, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " oldPin = " + oldPin + " newPin = " + newPin + " aid = " + aid);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "changeIccPinForApp", () -> {
            simProxy.changeIccPinForApp(rr.mSerial,
                    RILUtils.convertNullToEmptyString(oldPin),
                    RILUtils.convertNullToEmptyString(newPin),
                    RILUtils.convertNullToEmptyString(aid));
        });
    }

    @Override
    public void changeIccPin2(String oldPin2, String newPin2, Message result) {
        changeIccPin2ForApp(oldPin2, newPin2, null, result);
    }

    @Override
    public void changeIccPin2ForApp(String oldPin2, String newPin2, String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("changeIccPin2ForApp", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CHANGE_SIM_PIN2, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " oldPin = " + oldPin2 + " newPin = " + newPin2 + " aid = " + aid);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "changeIccPin2ForApp", () -> {
            simProxy.changeIccPin2ForApp(rr.mSerial,
                    RILUtils.convertNullToEmptyString(oldPin2),
                    RILUtils.convertNullToEmptyString(newPin2),
                    RILUtils.convertNullToEmptyString(aid));
        });
    }

    @Override
    public void supplyNetworkDepersonalization(String netpin, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("supplyNetworkDepersonalization", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " netpin = " + netpin);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "supplyNetworkDepersonalization", () -> {
            networkProxy.supplyNetworkDepersonalization(rr.mSerial,
                    RILUtils.convertNullToEmptyString(netpin));
        });
    }

    @Override
    public void supplySimDepersonalization(PersoSubState persoType, String controlKey,
            Message result) {
        if (mHalVersion.get(HAL_SERVICE_SIM).less(RADIO_HAL_VERSION_1_5)
                && PersoSubState.PERSOSUBSTATE_SIM_NETWORK == persoType) {
            supplyNetworkDepersonalization(controlKey, result);
            return;
        }
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("supplySimDepersonalization", simProxy, result,
                RADIO_HAL_VERSION_1_5)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_DEPERSONALIZATION, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " controlKey = " + controlKey + " persoType" + persoType);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "supplySimDepersonalization", () -> {
            simProxy.supplySimDepersonalization(rr.mSerial, persoType,
                    RILUtils.convertNullToEmptyString(controlKey));
        });
    }

    @Override
    public void getCurrentCalls(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("getCurrentCalls", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_CURRENT_CALLS, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "getCurrentCalls", () -> {
            voiceProxy.getCurrentCalls(rr.mSerial);
        });
    }

    @Override
    public void dial(String address, boolean isEmergencyCall, EmergencyNumber emergencyNumberInfo,
            boolean hasKnownUserIntentEmergency, int clirMode, Message result) {
        dial(address, isEmergencyCall, emergencyNumberInfo, hasKnownUserIntentEmergency,
                clirMode, null, result);
    }

    @Override
    public void enableModem(boolean enable, Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("enableModem", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_ENABLE_MODEM, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " enable = " + enable);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "enableModem", () -> {
            modemProxy.enableModem(rr.mSerial, enable);
        });
    }

    @Override
    public void setSystemSelectionChannels(@NonNull List<RadioAccessSpecifier> specifiers,
            Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setSystemSelectionChannels", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_SYSTEM_SELECTION_CHANNELS, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " setSystemSelectionChannels= " + specifiers);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setSystemSelectionChannels", () -> {
            networkProxy.setSystemSelectionChannels(rr.mSerial, specifiers);
        });
    }

    @Override
    public void getSystemSelectionChannels(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("getSystemSelectionChannels", networkProxy, result,
                RADIO_HAL_VERSION_1_6)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_SYSTEM_SELECTION_CHANNELS, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " getSystemSelectionChannels");
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "getSystemSelectionChannels", () -> {
            networkProxy.getSystemSelectionChannels(rr.mSerial);
        });
    }

    @Override
    public void getModemStatus(Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("getModemStatus", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_MODEM_STATUS, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "getModemStatus", () -> {
            modemProxy.getModemStackStatus(rr.mSerial);
        });
    }

    @Override
    public void dial(String address, boolean isEmergencyCall, EmergencyNumber emergencyNumberInfo,
            boolean hasKnownUserIntentEmergency, int clirMode, UUSInfo uusInfo, Message result) {
        if (isEmergencyCall && emergencyNumberInfo != null) {
            emergencyDial(address, emergencyNumberInfo, hasKnownUserIntentEmergency, clirMode,
                    uusInfo, result);
            return;
        }
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("dial", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_DIAL, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            // Do not log function arg for privacy
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "dial", () -> {
            voiceProxy.dial(rr.mSerial, address, clirMode, uusInfo);
        });
    }

    private void emergencyDial(String address, EmergencyNumber emergencyNumberInfo,
            boolean hasKnownUserIntentEmergency, int clirMode, UUSInfo uusInfo, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("emergencyDial", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_EMERGENCY_DIAL, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            // Do not log function arg for privacy
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "emergencyDial", () -> {
            voiceProxy.emergencyDial(rr.mSerial, RILUtils.convertNullToEmptyString(address),
                    emergencyNumberInfo, hasKnownUserIntentEmergency, clirMode, uusInfo);
        });
    }

    @Override
    public void getIMSI(Message result) {
        getIMSIForApp(null, result);
    }

    @Override
    public void getIMSIForApp(String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("getIMSIForApp", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_IMSI, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " aid = " + aid);
        }
        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "getIMSIForApp", () -> {
            simProxy.getImsiForApp(rr.mSerial, RILUtils.convertNullToEmptyString(aid));
        });
    }

    @Override
    public void hangupConnection(int gsmIndex, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("hangupConnection", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_HANGUP, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " gsmIndex = " + gsmIndex);
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "hangupConnection", () -> {
            voiceProxy.hangup(rr.mSerial, gsmIndex);
        });
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void hangupWaitingOrBackground(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("hangupWaitingOrBackground", voiceProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "hangupWaitingOrBackground", () -> {
            voiceProxy.hangupWaitingOrBackground(rr.mSerial);
        });
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void hangupForegroundResumeBackground(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("hangupForegroundResumeBackground", voiceProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "hangupForegroundResumeBackground", () -> {
            voiceProxy.hangupForegroundResumeBackground(rr.mSerial);
        });
    }

    @Override
    public void switchWaitingOrHoldingAndActive(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("switchWaitingOrHoldingAndActive", voiceProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "switchWaitingOrHoldingAndActive", () -> {
            voiceProxy.switchWaitingOrHoldingAndActive(rr.mSerial);
        });
    }

    @Override
    public void conference(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("conference", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CONFERENCE, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "conference", () -> {
            voiceProxy.conference(rr.mSerial);
        });
    }

    @Override
    public void rejectCall(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("rejectCall", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_UDUB, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "rejectCall", () -> {
            voiceProxy.rejectCall(rr.mSerial);
        });
    }

    @Override
    public void getLastCallFailCause(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("getLastCallFailCause", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_LAST_CALL_FAIL_CAUSE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "getLastCallFailCause", () -> {
            voiceProxy.getLastCallFailCause(rr.mSerial);
        });
    }

    @Override
    public void getSignalStrength(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("getSignalStrength", networkProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SIGNAL_STRENGTH, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "getSignalStrength", () -> {
            networkProxy.getSignalStrength(rr.mSerial);
        });
    }

    @Override
    public void getVoiceRegistrationState(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("getVoiceRegistrationState", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_VOICE_REGISTRATION_STATE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        HalVersion overrideHalVersion = getCompatVersion(RIL_REQUEST_VOICE_REGISTRATION_STATE);
        if (RILJ_LOGD) {
            riljLog("getVoiceRegistrationState: overrideHalVersion=" + overrideHalVersion);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "getVoiceRegistrationState", () -> {
            networkProxy.getVoiceRegistrationState(rr.mSerial, overrideHalVersion);
        });
    }

    @Override
    public void getDataRegistrationState(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("getDataRegistrationState", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_DATA_REGISTRATION_STATE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        HalVersion overrideHalVersion = getCompatVersion(RIL_REQUEST_DATA_REGISTRATION_STATE);
        if (RILJ_LOGD) {
            riljLog("getDataRegistrationState: overrideHalVersion=" + overrideHalVersion);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "getDataRegistrationState", () -> {
            networkProxy.getDataRegistrationState(rr.mSerial, overrideHalVersion);
        });
    }

    @Override
    public void getOperator(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("getOperator", networkProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_OPERATOR, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "getOperator", () -> {
            networkProxy.getOperator(rr.mSerial);
        });
    }

    @UnsupportedAppUsage
    @Override
    public void setRadioPower(boolean on, boolean forEmergencyCall,
            boolean preferredForEmergencyCall, Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("setRadioPower", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_RADIO_POWER, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " on = " + on + " forEmergencyCall= " + forEmergencyCall
                    + " preferredForEmergencyCall=" + preferredForEmergencyCall);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "setRadioPower", () -> {
            modemProxy.setRadioPower(rr.mSerial, on, forEmergencyCall,
                    preferredForEmergencyCall);
        });
    }

    @Override
    public void sendDtmf(char c, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("sendDtmf", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_DTMF, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            // Do not log function arg for privacy
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "sendDtmf", () -> {
            voiceProxy.sendDtmf(rr.mSerial, c + "");
        });
    }

    @Override
    public void sendSMS(String smscPdu, String pdu, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("sendSMS", messagingProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SEND_SMS, result, mRILDefaultWorkSource);

        // Do not log function args for privacy
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "sendSMS", () -> {
            messagingProxy.sendSms(rr.mSerial, smscPdu, pdu);
            mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_GSM,
                    SmsSession.Event.Format.SMS_FORMAT_3GPP, getOutgoingSmsMessageId(result));
        });
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
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("sendSMSExpectMore", messagingProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SEND_SMS_EXPECT_MORE, result,
                mRILDefaultWorkSource);

        // Do not log function arg for privacy
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "sendSMSExpectMore", () -> {
            messagingProxy.sendSmsExpectMore(rr.mSerial, smscPdu, pdu);
            mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_GSM,
                    SmsSession.Event.Format.SMS_FORMAT_3GPP, getOutgoingSmsMessageId(result));
        });
    }

    @Override
    public void setupDataCall(int accessNetworkType, DataProfile dataProfile, boolean allowRoaming,
            int reason, LinkProperties linkProperties, int pduSessionId, NetworkSliceInfo sliceInfo,
            TrafficDescriptor trafficDescriptor, boolean matchAllRuleAllowed, Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class);
        if (!canMakeRequest("setupDataCall", dataProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SETUP_DATA_CALL, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + ",reason=" + RILUtils.setupDataReasonToString(reason)
                    + ",accessNetworkType=" + AccessNetworkType.toString(accessNetworkType)
                    + ",dataProfile=" + dataProfile + ",allowRoaming=" + allowRoaming
                    + ",linkProperties=" + linkProperties + ",pduSessionId=" + pduSessionId
                    + ",sliceInfo=" + sliceInfo + ",trafficDescriptor=" + trafficDescriptor
                    + ",matchAllRuleAllowed=" + matchAllRuleAllowed);
        }

        radioServiceInvokeHelper(HAL_SERVICE_DATA, rr, "setupDataCall", () -> {
            dataProxy.setupDataCall(rr.mSerial, accessNetworkType, dataProfile, allowRoaming,
                    reason, linkProperties, pduSessionId, sliceInfo, trafficDescriptor,
                    matchAllRuleAllowed);
        });
    }

    @Override
    public void iccIO(int command, int fileId, String path, int p1, int p2, int p3, String data,
            String pin2, Message result) {
        iccIOForApp(command, fileId, path, p1, p2, p3, data, pin2, null, result);
    }

    @Override
    public void iccIOForApp(int command, int fileId, String path, int p1, int p2, int p3,
            String data, String pin2, String aid, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("iccIOForApp", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

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

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "iccIOForApp", () -> {
            simProxy.iccIoForApp(rr.mSerial, command, fileId,
                    RILUtils.convertNullToEmptyString(path), p1, p2, p3,
                    RILUtils.convertNullToEmptyString(data),
                    RILUtils.convertNullToEmptyString(pin2),
                    RILUtils.convertNullToEmptyString(aid));
        });
    }

    @Override
    public void sendUSSD(String ussd, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("sendUSSD", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SEND_USSD, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            String logUssd = "*******";
            if (RILJ_LOGV) logUssd = ussd;
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " ussd = " + logUssd);
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "sendUSSD", () -> {
            voiceProxy.sendUssd(rr.mSerial, RILUtils.convertNullToEmptyString(ussd));
        });
    }

    @Override
    public void cancelPendingUssd(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("cancelPendingUssd", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CANCEL_USSD, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "cancelPendingUssd", () -> {
            voiceProxy.cancelPendingUssd(rr.mSerial);
        });
    }

    @Override
    public void getCLIR(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("getCLIR", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_CLIR, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "getCLIR", () -> {
            voiceProxy.getClir(rr.mSerial);
        });
    }

    @Override
    public void setCLIR(int clirMode, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("setCLIR", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_CLIR, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " clirMode = " + clirMode);
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "setCLIR", () -> {
            voiceProxy.setClir(rr.mSerial, clirMode);
        });
    }

    @Override
    public void queryCallForwardStatus(int cfReason, int serviceClass, String number,
            Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("queryCallForwardStatus", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_CALL_FORWARD_STATUS, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " cfReason = " + cfReason + " serviceClass = " + serviceClass);
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "queryCallForwardStatus", () -> {
            voiceProxy.getCallForwardStatus(rr.mSerial, cfReason, serviceClass, number);
        });
    }

    @Override
    public void setCallForward(int action, int cfReason, int serviceClass, String number,
            int timeSeconds, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("setCallForward", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_CALL_FORWARD, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " action = " + action + " cfReason = " + cfReason + " serviceClass = "
                    + serviceClass + " timeSeconds = " + timeSeconds);
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "setCallForward", () -> {
            voiceProxy.setCallForward(
                    rr.mSerial, action, cfReason, serviceClass, number, timeSeconds);
        });
    }

    @Override
    public void queryCallWaiting(int serviceClass, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("queryCallWaiting", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_CALL_WAITING, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " serviceClass = " + serviceClass);
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "queryCallWaiting", () -> {
            voiceProxy.getCallWaiting(rr.mSerial, serviceClass);
        });
    }

    @Override
    public void setCallWaiting(boolean enable, int serviceClass, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("setCallWaiting", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_CALL_WAITING, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " enable = " + enable + " serviceClass = " + serviceClass);
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "setCallWaiting", () -> {
            voiceProxy.setCallWaiting(rr.mSerial, enable, serviceClass);
        });
    }

    @Override
    public void acknowledgeLastIncomingGsmSms(boolean success, int cause, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("acknowledgeLastIncomingGsmSms", messagingProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SMS_ACKNOWLEDGE, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " success = " + success + " cause = " + cause);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "acknowledgeLastIncomingGsmSms", () -> {
            messagingProxy.acknowledgeLastIncomingGsmSms(rr.mSerial, success, cause);
        });
    }

    @Override
    public void acceptCall(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("acceptCall", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_ANSWER, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "acceptCall", () -> {
            voiceProxy.acceptCall(rr.mSerial);
            mMetrics.writeRilAnswer(mPhoneId, rr.mSerial);
        });
    }

    @Override
    public void deactivateDataCall(int cid, int reason, Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class);
        if (!canMakeRequest("deactivateDataCall", dataProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_DEACTIVATE_DATA_CALL, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " cid = " + cid + " reason = "
                    + RILUtils.deactivateDataReasonToString(reason));
        }

        radioServiceInvokeHelper(HAL_SERVICE_DATA, rr, "deactivateDataCall", () -> {
            dataProxy.deactivateDataCall(rr.mSerial, cid, reason);
            mMetrics.writeRilDeactivateDataCall(mPhoneId, rr.mSerial, cid, reason);
        });
    }

    @Override
    public void queryFacilityLock(String facility, String password, int serviceClass,
            Message result) {
        queryFacilityLockForApp(facility, password, serviceClass, null, result);
    }

    @Override
    public void queryFacilityLockForApp(String facility, String password, int serviceClass,
            String appId, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("queryFacilityLockForApp", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_FACILITY_LOCK, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " facility = " + facility + " serviceClass = " + serviceClass
                    + " appId = " + appId);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "queryFacilityLockForApp", () -> {
            simProxy.getFacilityLockForApp(rr.mSerial,
                    RILUtils.convertNullToEmptyString(facility),
                    RILUtils.convertNullToEmptyString(password), serviceClass,
                    RILUtils.convertNullToEmptyString(appId));
        });

    }

    @Override
    public void setFacilityLock(String facility, boolean lockState, String password,
            int serviceClass, Message result) {
        setFacilityLockForApp(facility, lockState, password, serviceClass, null, result);
    }

    @Override
    public void setFacilityLockForApp(String facility, boolean lockState, String password,
            int serviceClass, String appId, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("setFacilityLockForApp", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_FACILITY_LOCK, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " facility = " + facility + " lockstate = " + lockState
                    + " serviceClass = " + serviceClass + " appId = " + appId);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "setFacilityLockForApp", () -> {
            simProxy.setFacilityLockForApp(rr.mSerial,
                    RILUtils.convertNullToEmptyString(facility), lockState,
                    RILUtils.convertNullToEmptyString(password), serviceClass,
                    RILUtils.convertNullToEmptyString(appId));
        });
    }

    @Override
    public void changeBarringPassword(String facility, String oldPwd, String newPwd,
            Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("changeBarringPassword", networkProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CHANGE_BARRING_PASSWORD, result,
                mRILDefaultWorkSource);

        // Do not log all function args for privacy
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + "facility = " + facility);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "changeBarringPassword", () -> {
            networkProxy.setBarringPassword(rr.mSerial,
                    RILUtils.convertNullToEmptyString(facility),
                    RILUtils.convertNullToEmptyString(oldPwd),
                    RILUtils.convertNullToEmptyString(newPwd));
        });
    }

    @Override
    public void getNetworkSelectionMode(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("getNetworkSelectionMode", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "getNetworkSelectionMode", () -> {
            networkProxy.getNetworkSelectionMode(rr.mSerial);
        });
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setNetworkSelectionModeAutomatic", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setNetworkSelectionModeAutomatic",
                () -> {
                    networkProxy.setNetworkSelectionModeAutomatic(rr.mSerial);
                });
    }

    @Override
    public void setNetworkSelectionModeManual(String operatorNumeric, int ran, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setNetworkSelectionModeManual", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " operatorNumeric = " + operatorNumeric + ", ran = " + ran);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setNetworkSelectionModeManual", () -> {
            networkProxy.setNetworkSelectionModeManual(rr.mSerial,
                    RILUtils.convertNullToEmptyString(operatorNumeric), ran);
        });
    }

    @Override
    public void getAvailableNetworks(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("getAvailableNetworks", networkProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_AVAILABLE_NETWORKS, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "getAvailableNetworks", () -> {
            networkProxy.getAvailableNetworks(rr.mSerial);
        });
    }

    /**
     * Radio HAL fallback compatibility feature (b/151106728) assumes that the input parameter
     * networkScanRequest is immutable (read-only) here. Once the caller invokes the method, the
     * parameter networkScanRequest should not be modified. This helps us keep a consistent and
     * simple data model that avoid copying it in the scan result.
     */
    @Override
    public void startNetworkScan(NetworkScanRequest networkScanRequest, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("startNetworkScan", networkProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        HalVersion overrideHalVersion = getCompatVersion(RIL_REQUEST_START_NETWORK_SCAN);
        if (RILJ_LOGD) {
            riljLog("startNetworkScan: overrideHalVersion=" + overrideHalVersion);
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_START_NETWORK_SCAN, result,
                mRILDefaultWorkSource, networkScanRequest);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "startNetworkScan", () -> {
            networkProxy.startNetworkScan(rr.mSerial, networkScanRequest, overrideHalVersion,
                    result);
        });
    }

    @Override
    public void stopNetworkScan(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("stopNetworkScan", networkProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_STOP_NETWORK_SCAN, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "stopNetworkScan", () -> {
            networkProxy.stopNetworkScan(rr.mSerial);
        });
    }

    @Override
    public void startDtmf(char c, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("startDtmf", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_DTMF_START, result, mRILDefaultWorkSource);

        // Do not log function arg for privacy
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "startDtmf", () -> {
            voiceProxy.startDtmf(rr.mSerial, c + "");
        });
    }

    @Override
    public void stopDtmf(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("stopDtmf", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_DTMF_STOP, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "stopDtmf", () -> {
            voiceProxy.stopDtmf(rr.mSerial);
        });
    }

    @Override
    public void separateConnection(int gsmIndex, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("separateConnection", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SEPARATE_CONNECTION, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " gsmIndex = " + gsmIndex);
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "separateConnection", () -> {
            voiceProxy.separateConnection(rr.mSerial, gsmIndex);
        });
    }

    @Override
    public void getBasebandVersion(Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("getBasebandVersion", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_BASEBAND_VERSION, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "getBasebandVersion", () -> {
            modemProxy.getBasebandVersion(rr.mSerial);
        });
    }

    @Override
    public void setMute(boolean enableMute, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("setMute", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_MUTE, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " enableMute = " + enableMute);
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "setMute", () -> {
            voiceProxy.setMute(rr.mSerial, enableMute);
        });
    }

    @Override
    public void getMute(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("getMute", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_MUTE, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "getMute", () -> {
            voiceProxy.getMute(rr.mSerial);
        });
    }

    @Override
    public void queryCLIP(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("queryCLIP", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_CLIP, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "queryCLIP", () -> {
            voiceProxy.getClip(rr.mSerial);
        });
    }

    @Override
    public void getDataCallList(Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class);
        if (!canMakeRequest("getDataCallList", dataProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_DATA_CALL_LIST, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_DATA, rr, "getDataCallList", () -> {
            dataProxy.getDataCallList(rr.mSerial);
        });
    }

    @Override
    public void setSuppServiceNotifications(boolean enable, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setSuppServiceNotifications", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " enable = " + enable);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setSuppServiceNotifications", () -> {
            networkProxy.setSuppServiceNotifications(rr.mSerial, enable);
        });
    }

    @Override
    public void writeSmsToSim(int status, String smsc, String pdu, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("writeSmsToSim", messagingProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_WRITE_SMS_TO_SIM, result, mRILDefaultWorkSource);

        if (RILJ_LOGV) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " " + status);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "writeSmsToSim", () -> {
            messagingProxy.writeSmsToSim(rr.mSerial, status,
                    RILUtils.convertNullToEmptyString(smsc),
                    RILUtils.convertNullToEmptyString(pdu));
        });
    }

    @Override
    public void deleteSmsOnSim(int index, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("deleteSmsOnSim", messagingProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_DELETE_SMS_ON_SIM, result, mRILDefaultWorkSource);

        if (RILJ_LOGV) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " index = " + index);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "deleteSmsOnSim", () -> {
            messagingProxy.deleteSmsOnSim(rr.mSerial, index);
        });
    }

    @Override
    public void setBandMode(int bandMode, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setBandMode", networkProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_BAND_MODE, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " bandMode = " + bandMode);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setBandMode", () -> {
            networkProxy.setBandMode(rr.mSerial, bandMode);
        });
    }

    @Override
    public void queryAvailableBandMode(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("queryAvailableBandMode", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "queryAvailableBandMode", () -> {
            networkProxy.getAvailableBandModes(rr.mSerial);
        });
    }

    @Override
    public void sendEnvelope(String contents, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("sendEnvelope", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " contents = " + contents);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "sendEnvelope", () -> {
            simProxy.sendEnvelope(rr.mSerial, RILUtils.convertNullToEmptyString(contents));
        });
    }

    @Override
    public void sendTerminalResponse(String contents, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("sendTerminalResponse", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " contents = " + (TelephonyUtils.IS_DEBUGGABLE
                    ? contents : RILUtils.convertToCensoredTerminalResponse(contents)));
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "sendTerminalResponse", () -> {
            simProxy.sendTerminalResponseToSim(rr.mSerial,
                    RILUtils.convertNullToEmptyString(contents));
        });
    }

    @Override
    public void sendEnvelopeWithStatus(String contents, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("sendEnvelopeWithStatus", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " contents = " + contents);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "sendEnvelopeWithStatus", () -> {
            simProxy.sendEnvelopeWithStatus(rr.mSerial,
                    RILUtils.convertNullToEmptyString(contents));
        });
    }

    @Override
    public void explicitCallTransfer(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("explicitCallTransfer", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_EXPLICIT_CALL_TRANSFER, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "explicitCallTransfer", () -> {
            voiceProxy.explicitCallTransfer(rr.mSerial);
        });
    }

    @Override
    public void setPreferredNetworkType(@PrefNetworkMode int networkType , Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setPreferredNetworkType", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " networkType = " + networkType);
        }
        mAllowedNetworkTypesBitmask = RadioAccessFamily.getRafFromNetworkType(networkType);
        mMetrics.writeSetPreferredNetworkType(mPhoneId, networkType);

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setPreferredNetworkType", () -> {
            networkProxy.setPreferredNetworkTypeBitmap(rr.mSerial, mAllowedNetworkTypesBitmask);
        });
    }

    @Override
    public void getPreferredNetworkType(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("getPreferredNetworkType", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "getPreferredNetworkType", () -> {
            networkProxy.getAllowedNetworkTypesBitmap(rr.mSerial);
        });
    }

    @Override
    public void setAllowedNetworkTypesBitmap(
            @TelephonyManager.NetworkTypeBitMask int networkTypeBitmask, Message result) {
        if (mHalVersion.get(HAL_SERVICE_NETWORK).less(RADIO_HAL_VERSION_1_6)) {
            // For older HAL, redirects the call to setPreferredNetworkType.
            setPreferredNetworkType(
                    RadioAccessFamily.getNetworkTypeFromRaf(networkTypeBitmask), result);
            return;
        }
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setAllowedNetworkTypesBitmap", networkProxy, result,
                RADIO_HAL_VERSION_1_6)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_ALLOWED_NETWORK_TYPES_BITMAP, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }
        mAllowedNetworkTypesBitmask = networkTypeBitmask;

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setAllowedNetworkTypesBitmap", () -> {
            networkProxy.setAllowedNetworkTypesBitmap(rr.mSerial, mAllowedNetworkTypesBitmask);
        });
    }

    @Override
    public void getAllowedNetworkTypesBitmap(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("getAllowedNetworkTypesBitmap", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_ALLOWED_NETWORK_TYPES_BITMAP, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "getAllowedNetworkTypesBitmap", () -> {
            networkProxy.getAllowedNetworkTypesBitmap(rr.mSerial);
        });
    }

    @Override
    public void setLocationUpdates(boolean enable, WorkSource workSource, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setLocationUpdates", networkProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_LOCATION_UPDATES, result,
                getDefaultWorkSourceIfInvalid(workSource));

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " enable = " + enable);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setLocationUpdates", () -> {
            networkProxy.setLocationUpdates(rr.mSerial, enable);
        });
    }

    /**
     * Is E-UTRA-NR Dual Connectivity enabled
     */
    @Override
    public void isNrDualConnectivityEnabled(Message result, WorkSource workSource) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("isNrDualConnectivityEnabled", networkProxy, result,
                RADIO_HAL_VERSION_1_6)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_IS_NR_DUAL_CONNECTIVITY_ENABLED, result,
                getDefaultWorkSourceIfInvalid(workSource));

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "isNrDualConnectivityEnabled", () -> {
            networkProxy.isNrDualConnectivityEnabled(rr.mSerial);
        });
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
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setNrDualConnectivityState", networkProxy, result,
                RADIO_HAL_VERSION_1_6)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_ENABLE_NR_DUAL_CONNECTIVITY, result,
                getDefaultWorkSourceIfInvalid(workSource));

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " enable = " + nrDualConnectivityState);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setNrDualConnectivityState", () -> {
            networkProxy.setNrDualConnectivityState(rr.mSerial, (byte) nrDualConnectivityState);
        });
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
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        // Send null result so errors aren't sent in canMakeRequest
        if (!canMakeRequest("isVoNrEnabled", voiceProxy, null, RADIO_HAL_VERSION_2_0)) {
            boolean isEnabled = isVoNrEnabled();
            if (result != null) {
                AsyncResult.forMessage(result, isEnabled, null);
                result.sendToTarget();
            }
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_IS_VONR_ENABLED, result,
                getDefaultWorkSourceIfInvalid(workSource));

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "isVoNrEnabled", () -> {
            voiceProxy.isVoNrEnabled(rr.mSerial);
        });
    }

    /**
     * Enable or disable Voice over NR (VoNR)
     * @param enabled enable or disable VoNR.
     */
    @Override
    public void setVoNrEnabled(boolean enabled, Message result, WorkSource workSource) {
        setVoNrEnabled(enabled);
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        // Send null result so errors aren't sent in canMakeRequest
        if (!canMakeRequest("setVoNrEnabled", voiceProxy, null, RADIO_HAL_VERSION_2_0)) {
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
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_ENABLE_VONR, result,
                getDefaultWorkSourceIfInvalid(workSource));

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "setVoNrEnabled", () -> {
            voiceProxy.setVoNrEnabled(rr.mSerial, enabled);
        });
    }

    @Override
    public void setCdmaSubscriptionSource(int cdmaSubscription, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("setCdmaSubscriptionSource", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " cdmaSubscription = " + cdmaSubscription);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "setCdmaSubscriptionSource", () -> {
            simProxy.setCdmaSubscriptionSource(rr.mSerial, cdmaSubscription);
        });
    }

    @Override
    public void queryCdmaRoamingPreference(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("queryCdmaRoamingPreference", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "queryCdmaRoamingPreference", () -> {
            networkProxy.getCdmaRoamingPreference(rr.mSerial);
        });
    }

    @Override
    public void setCdmaRoamingPreference(int cdmaRoamingType, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setCdmaRoamingPreference", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " cdmaRoamingType = " + cdmaRoamingType);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setCdmaRoamingPreference", () -> {
            networkProxy.setCdmaRoamingPreference(rr.mSerial, cdmaRoamingType);
        });
    }

    @Override
    public void queryTTYMode(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("queryTTYMode", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_TTY_MODE, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "queryTTYMode", () -> {
            voiceProxy.getTtyMode(rr.mSerial);
        });
    }

    @Override
    public void setTTYMode(int ttyMode, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("setTTYMode", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_TTY_MODE, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " ttyMode = " + ttyMode);
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "setTTYMode", () -> {
            voiceProxy.setTtyMode(rr.mSerial, ttyMode);
        });
    }

    @Override
    public void setPreferredVoicePrivacy(boolean enable, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("setPreferredVoicePrivacy", voiceProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " enable = " + enable);
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "setPreferredVoicePrivacy", () -> {
            voiceProxy.setPreferredVoicePrivacy(rr.mSerial, enable);
        });
    }

    @Override
    public void getPreferredVoicePrivacy(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("getPreferredVoicePrivacy", voiceProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE,
                result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "getPreferredVoicePrivacy", () -> {
            voiceProxy.getPreferredVoicePrivacy(rr.mSerial);
        });
    }

    @Override
    public void sendCDMAFeatureCode(String featureCode, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("sendCDMAFeatureCode", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_FLASH, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " featureCode = " + Rlog.pii(RILJ_LOG_TAG, featureCode));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "sendCDMAFeatureCode", () -> {
            voiceProxy.sendCdmaFeatureCode(rr.mSerial,
                    RILUtils.convertNullToEmptyString(featureCode));
        });
    }

    @Override
    public void sendBurstDtmf(String dtmfString, int on, int off, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("sendBurstDtmf", voiceProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_BURST_DTMF, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " dtmfString = " + dtmfString + " on = " + on + " off = " + off);
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "sendBurstDtmf", () -> {
            voiceProxy.sendBurstDtmf(rr.mSerial, RILUtils.convertNullToEmptyString(dtmfString),
                    on, off);
        });
    }

    @Override
    public void sendCdmaSMSExpectMore(byte[] pdu, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("sendCdmaSMSExpectMore", messagingProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SEND_SMS_EXPECT_MORE, result,
                mRILDefaultWorkSource);

        // Do not log function arg for privacy
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "sendCdmaSMSExpectMore", () -> {
            messagingProxy.sendCdmaSmsExpectMore(rr.mSerial, pdu);
            if (mHalVersion.get(HAL_SERVICE_MESSAGING).greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
                mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_CDMA,
                        SmsSession.Event.Format.SMS_FORMAT_3GPP2,
                        getOutgoingSmsMessageId(result));
            }
        });
    }

    @Override
    public void sendCdmaSms(byte[] pdu, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("sendCdmaSms", messagingProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SEND_SMS, result, mRILDefaultWorkSource);

        // Do not log function arg for privacy
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "sendCdmaSms", () -> {
            messagingProxy.sendCdmaSms(rr.mSerial, pdu);
            mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_CDMA,
                    SmsSession.Event.Format.SMS_FORMAT_3GPP2, getOutgoingSmsMessageId(result));
        });
    }

    @Override
    public void acknowledgeLastIncomingCdmaSms(boolean success, int cause, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("acknowledgeLastIncomingCdmaSms", messagingProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " success = " + success + " cause = " + cause);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "acknowledgeLastIncomingCdmaSms",
                () -> {
                    messagingProxy.acknowledgeLastIncomingCdmaSms(rr.mSerial, success, cause);
                });
    }

    @Override
    public void getGsmBroadcastConfig(Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("getGsmBroadcastConfig", messagingProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GSM_GET_BROADCAST_CONFIG, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "getGsmBroadcastConfig", () -> {
            messagingProxy.getGsmBroadcastConfig(rr.mSerial);
        });
    }

    @Override
    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] config, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("setGsmBroadcastConfig", messagingProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GSM_SET_BROADCAST_CONFIG, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " with " + config.length + " configs : ");
            for (int i = 0; i < config.length; i++) {
                riljLog(config[i].toString());
            }
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "setGsmBroadcastConfig", () -> {
            messagingProxy.setGsmBroadcastConfig(rr.mSerial, config);
        });
    }

    @Override
    public void setGsmBroadcastActivation(boolean activate, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("setGsmBroadcastActivation", messagingProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GSM_BROADCAST_ACTIVATION, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " activate = " + activate);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "setGsmBroadcastActivation", () -> {
            messagingProxy.setGsmBroadcastActivation(rr.mSerial, activate);
        });
    }

    @Override
    public void getCdmaBroadcastConfig(Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("getCdmaBroadcastConfig", messagingProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "getCdmaBroadcastConfig", () -> {
            messagingProxy.getCdmaBroadcastConfig(rr.mSerial);
        });
    }

    @Override
    public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] configs, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("setCdmaBroadcastConfig", messagingProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " with " + configs.length + " configs : ");
            for (CdmaSmsBroadcastConfigInfo config : configs) {
                riljLog(config.toString());
            }
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "setCdmaBroadcastConfig", () -> {
            messagingProxy.setCdmaBroadcastConfig(rr.mSerial, configs);
        });
    }

    @Override
    public void setCdmaBroadcastActivation(boolean activate, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("setCdmaBroadcastActivation", messagingProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_BROADCAST_ACTIVATION, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " activate = " + activate);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "setCdmaBroadcastActivation", () -> {
            messagingProxy.setCdmaBroadcastActivation(rr.mSerial, activate);
        });
    }

    @Override
    public void getCDMASubscription(Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("getCDMASubscription", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SUBSCRIPTION, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "getCDMASubscription", () -> {
            simProxy.getCdmaSubscription(rr.mSerial);
        });
    }

    @Override
    public void writeSmsToRuim(int status, byte[] pdu, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("writeSmsToRuim", messagingProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGV) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " status = " + status);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "writeSmsToRuim", () -> {
            messagingProxy.writeSmsToRuim(rr.mSerial, status, pdu);
        });
    }

    @Override
    public void deleteSmsOnRuim(int index, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("deleteSmsOnRuim", messagingProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGV) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " index = " + index);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "deleteSmsOnRuim", () -> {
            messagingProxy.deleteSmsOnRuim(rr.mSerial, index);
        });
    }

    @Override
    public void getDeviceIdentity(Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("getDeviceIdentity", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_DEVICE_IDENTITY, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "getDeviceIdentity", () -> {
            modemProxy.getDeviceIdentity(rr.mSerial);
        });
    }

    @Override
    public void getImei(Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("getImei", modemProxy, result, RADIO_HAL_VERSION_2_1)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_DEVICE_IMEI, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "getImei", () -> {
            modemProxy.getImei(rr.mSerial);
        });
    }

    @Override
    public void exitEmergencyCallbackMode(Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("exitEmergencyCallbackMode", voiceProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "exitEmergencyCallbackMode", () -> {
            voiceProxy.exitEmergencyCallbackMode(rr.mSerial);
        });
    }

    @Override
    public void getSmscAddress(Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("getSmscAddress", messagingProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_SMSC_ADDRESS, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "getSmscAddress", () -> {
            messagingProxy.getSmscAddress(rr.mSerial);
        });
    }

    @Override
    public void setSmscAddress(String address, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("setSmscAddress", messagingProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_SMSC_ADDRESS, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " address = " + address);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "setSmscAddress", () -> {
            messagingProxy.setSmscAddress(rr.mSerial, RILUtils.convertNullToEmptyString(address));
        });
    }

    @Override
    public void reportSmsMemoryStatus(boolean available, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("reportSmsMemoryStatus", messagingProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_REPORT_SMS_MEMORY_STATUS, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " available = " + available);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "reportSmsMemoryStatus", () -> {
            messagingProxy.reportSmsMemoryStatus(rr.mSerial, available);
        });
    }

    @Override
    public void reportStkServiceIsRunning(Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("reportStkServiceIsRunning", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "reportStkServiceIsRunning", () -> {
            simProxy.reportStkServiceIsRunning(rr.mSerial);
        });
    }

    @Override
    public void getCdmaSubscriptionSource(Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("getCdmaSubscriptionSource", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "getCdmaSubscriptionSource", () -> {
            simProxy.getCdmaSubscriptionSource(rr.mSerial);
        });
    }

    @Override
    public void acknowledgeIncomingGsmSmsWithPdu(boolean success, String ackPdu, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("acknowledgeIncomingGsmSmsWithPdu", messagingProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " success = " + success);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "acknowledgeIncomingGsmSmsWithPdu",
                () -> {
                    messagingProxy.acknowledgeIncomingGsmSmsWithPdu(rr.mSerial, success,
                            RILUtils.convertNullToEmptyString(ackPdu));
                });
    }

    @Override
    public void getVoiceRadioTechnology(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("getVoiceRadioTechnology", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_VOICE_RADIO_TECH, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "getVoiceRadioTechnology", () -> {
            networkProxy.getVoiceRadioTechnology(rr.mSerial);
        });
    }

    @Override
    public void getCellInfoList(Message result, WorkSource workSource) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("getCellInfoList", networkProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_CELL_INFO_LIST, result,
                getDefaultWorkSourceIfInvalid(workSource));

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "getCellInfoList", () -> {
            networkProxy.getCellInfoList(rr.mSerial);
        });
    }

    @Override
    public void setCellInfoListRate(int rateInMillis, Message result, WorkSource workSource) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setCellInfoListRate", networkProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_UNSOL_CELL_INFO_LIST_RATE, result,
                getDefaultWorkSourceIfInvalid(workSource));

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " rateInMillis = " + rateInMillis);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setCellInfoListRate", () -> {
            networkProxy.setCellInfoListRate(rr.mSerial, rateInMillis);
        });
    }

    @Override
    public void setInitialAttachApn(DataProfile dataProfile, Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class);
        if (!canMakeRequest("setInitialAttachApn", dataProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_INITIAL_ATTACH_APN, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest) + dataProfile);
        }

        radioServiceInvokeHelper(HAL_SERVICE_DATA, rr, "setInitialAttachApn", () -> {
            dataProxy.setInitialAttachApn(rr.mSerial, dataProfile);
        });
    }

    @Override
    public void getImsRegistrationState(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("getImsRegistrationState", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_IMS_REGISTRATION_STATE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "getImsRegistrationState", () -> {
            networkProxy.getImsRegistrationState(rr.mSerial);
        });
    }

    @Override
    public void sendImsGsmSms(String smscPdu, String pdu, int retry, int messageRef,
            Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("sendImsGsmSms", messagingProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_IMS_SEND_SMS, result, mRILDefaultWorkSource);

        // Do not log function args for privacy
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "sendImsGsmSms", () -> {
            messagingProxy.sendImsSms(rr.mSerial, smscPdu, pdu, null, retry, messageRef);
            mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_IMS,
                    SmsSession.Event.Format.SMS_FORMAT_3GPP, getOutgoingSmsMessageId(result));
        });
    }

    @Override
    public void sendImsCdmaSms(byte[] pdu, int retry, int messageRef, Message result) {
        RadioMessagingProxy messagingProxy = getRadioServiceProxy(RadioMessagingProxy.class);
        if (!canMakeRequest("sendImsCdmaSms", messagingProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_IMS_SEND_SMS, result, mRILDefaultWorkSource);

        // Do not log function args for privacy
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MESSAGING, rr, "sendImsCdmaSms", () -> {
            messagingProxy.sendImsSms(rr.mSerial, null, null, pdu, retry, messageRef);
            mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_IMS,
                    SmsSession.Event.Format.SMS_FORMAT_3GPP2, getOutgoingSmsMessageId(result));
        });
    }

    @Override
    public void iccTransmitApduBasicChannel(int cla, int instruction, int p1, int p2, int p3,
            String data, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("iccTransmitApduBasicChannel", simProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

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

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "iccTransmitApduBasicChannel", () -> {
            simProxy.iccTransmitApduBasicChannel(rr.mSerial, cla, instruction, p1, p2, p3, data);
        });
    }

    @Override
    public void iccOpenLogicalChannel(String aid, int p2, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("iccOpenLogicalChannel", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SIM_OPEN_CHANNEL, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            if (TelephonyUtils.IS_DEBUGGABLE) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + " aid = " + aid + " p2 = " + p2);
            } else {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "iccOpenLogicalChannel", () -> {
            simProxy.iccOpenLogicalChannel(rr.mSerial, RILUtils.convertNullToEmptyString(aid), p2);
        });
    }

    @Override
    public void iccCloseLogicalChannel(int channel, boolean isEs10, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("iccCloseLogicalChannel", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SIM_CLOSE_CHANNEL, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " channel = " + channel + " isEs10 = " + isEs10);
        }
        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "iccCloseLogicalChannel", () -> {
            simProxy.iccCloseLogicalChannel(rr.mSerial, channel, isEs10);
        });
    }

    @Override
    public void iccTransmitApduLogicalChannel(int channel, int cla, int instruction, int p1, int p2,
            int p3, String data, boolean isEs10Command, Message result) {
        if (channel <= 0) {
            throw new RuntimeException(
                    "Invalid channel in iccTransmitApduLogicalChannel: " + channel);
        }

        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("iccTransmitApduLogicalChannel", simProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            if (TelephonyUtils.IS_DEBUGGABLE) {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                        + String.format(" channel = %d", channel)
                        + String.format(" cla = 0x%02X ins = 0x%02X", cla, instruction)
                        + String.format(" p1 = 0x%02X p2 = 0x%02X p3 = 0x%02X", p1, p2, p3)
                        + " isEs10Command = " + isEs10Command
                        + " data = " + data);
            } else {
                riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
            }
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "iccTransmitApduLogicalChannel", () -> {
            simProxy.iccTransmitApduLogicalChannel(rr.mSerial, channel, cla, instruction, p1, p2,
                    p3, data, isEs10Command);
        });
    }

    @Override
    public void nvReadItem(int itemID, Message result, WorkSource workSource) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("nvReadItem", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_NV_READ_ITEM, result,
                getDefaultWorkSourceIfInvalid(workSource));

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " itemId = " + itemID);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "nvReadItem", () -> {
            modemProxy.nvReadItem(rr.mSerial, itemID);
        });
    }

    @Override
    public void nvWriteItem(int itemId, String itemValue, Message result, WorkSource workSource) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("nvWriteItem", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_NV_WRITE_ITEM, result,
                getDefaultWorkSourceIfInvalid(workSource));

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " itemId = " + itemId + " itemValue = " + itemValue);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "nvWriteItem", () -> {
            modemProxy.nvWriteItem(rr.mSerial, itemId,
                    RILUtils.convertNullToEmptyString(itemValue));
        });
    }

    @Override
    public void nvWriteCdmaPrl(byte[] preferredRoamingList, Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("nvWriteCdmaPrl", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_NV_WRITE_CDMA_PRL, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " PreferredRoamingList = 0x"
                    + IccUtils.bytesToHexString(preferredRoamingList));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "nvWriteCdmaPrl", () -> {
            modemProxy.nvWriteCdmaPrl(rr.mSerial, preferredRoamingList);
        });
    }

    @Override
    public void nvResetConfig(int resetType, Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("nvResetConfig", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_NV_RESET_CONFIG, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " resetType = " + resetType);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "nvResetConfig", () -> {
            modemProxy.nvResetConfig(rr.mSerial, resetType);
        });
    }

    @Override
    public void setUiccSubscription(int slotId, int appIndex, int subId, int subStatus,
            Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("setUiccSubscription", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_UICC_SUBSCRIPTION, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " slot = " + slotId + " appIndex = " + appIndex
                    + " subId = " + subId + " subStatus = " + subStatus);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "setUiccSubscription", () -> {
            simProxy.setUiccSubscription(rr.mSerial, slotId, appIndex, subId, subStatus);
        });
    }

    @Override
    public void setDataAllowed(boolean allowed, Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class);
        if (!canMakeRequest("setDataAllowed", dataProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_ALLOW_DATA, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " allowed = " + allowed);
        }

        radioServiceInvokeHelper(HAL_SERVICE_DATA, rr, "setDataAllowed", () -> {
            dataProxy.setDataAllowed(rr.mSerial, allowed);
        });
    }

    @Override
    public void getHardwareConfig(Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("getHardwareConfig", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_HARDWARE_CONFIG, result,
                mRILDefaultWorkSource);

        // Do not log function args for privacy
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "getHardwareConfig", () -> {
            modemProxy.getHardwareConfig(rr.mSerial);
        });
    }

    @Override
    public void requestIccSimAuthentication(int authContext, String data, String aid,
            Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("requestIccSimAuthentication", simProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SIM_AUTHENTICATION, result,
                mRILDefaultWorkSource);

        // Do not log function args for privacy
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "requestIccSimAuthentication", () -> {
            simProxy.requestIccSimAuthentication(rr.mSerial, authContext,
                    RILUtils.convertNullToEmptyString(data),
                    RILUtils.convertNullToEmptyString(aid));
        });
    }

    @Override
    public void setDataProfile(DataProfile[] dps, Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class);
        if (!canMakeRequest("setDataProfile", dataProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_DATA_PROFILE, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " with data profiles : ");
            for (DataProfile profile : dps) {
                riljLog(profile.toString());
            }
        }

        radioServiceInvokeHelper(HAL_SERVICE_DATA, rr, "setDataProfile", () -> {
            dataProxy.setDataProfile(rr.mSerial, dps);
        });
    }

    @Override
    public void requestShutdown(Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("requestShutdown", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SHUTDOWN, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "requestShutdown", () -> {
            modemProxy.requestShutdown(rr.mSerial);
        });
    }

    @Override
    public void getRadioCapability(Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("getRadioCapability", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_RADIO_CAPABILITY, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "getRadioCapability", () -> {
            modemProxy.getRadioCapability(rr.mSerial);
        });
    }

    @Override
    public void setRadioCapability(RadioCapability rc, Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("setRadioCapability", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_RADIO_CAPABILITY, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " RadioCapability = " + rc.toString());
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "setRadioCapability", () -> {
            modemProxy.setRadioCapability(rr.mSerial, rc);
        });
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
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class);
        if (!canMakeRequest("setDataThrottling", dataProxy, result, RADIO_HAL_VERSION_1_6)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_DATA_THROTTLING, result,
                getDefaultWorkSourceIfInvalid(workSource));

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " dataThrottlingAction = " + dataThrottlingAction
                    + " completionWindowMillis " + completionWindowMillis);
        }

        radioServiceInvokeHelper(HAL_SERVICE_DATA, rr, "setDataThrottling", () -> {
            dataProxy.setDataThrottling(rr.mSerial, (byte) dataThrottlingAction,
                    completionWindowMillis);
        });
    }

    @Override
    public void getModemActivityInfo(Message result, WorkSource workSource) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("getModemActivityInfo", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_ACTIVITY_INFO, result,
                getDefaultWorkSourceIfInvalid(workSource));

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "getModemActivityInfo", () -> {
            modemProxy.getModemActivityInfo(rr.mSerial);
            Message msg = mRilHandler.obtainMessage(EVENT_BLOCKING_RESPONSE_TIMEOUT, rr.mSerial);
            mRilHandler.sendMessageDelayed(msg, DEFAULT_BLOCKING_MESSAGE_RESPONSE_TIMEOUT_MS);
        });
    }

    @Override
    public void setAllowedCarriers(CarrierRestrictionRules carrierRestrictionRules,
            Message result, WorkSource workSource) {
        Objects.requireNonNull(carrierRestrictionRules, "Carrier restriction cannot be null.");

        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("setAllowedCarriers", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_ALLOWED_CARRIERS, result,
                getDefaultWorkSourceIfInvalid(workSource));

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " params: " + carrierRestrictionRules);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "setAllowedCarriers", () -> {
            simProxy.setAllowedCarriers(rr.mSerial, carrierRestrictionRules);
        });
    }

    @Override
    public void getAllowedCarriers(Message result, WorkSource workSource) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("getAllowedCarriers", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_ALLOWED_CARRIERS, result,
                getDefaultWorkSourceIfInvalid(workSource));

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "getAllowedCarriers", () -> {
            simProxy.getAllowedCarriers(rr.mSerial);
        });
    }

    @Override
    public void sendDeviceState(int stateType, boolean state, Message result) {
        RadioModemProxy modemProxy = getRadioServiceProxy(RadioModemProxy.class);
        if (!canMakeRequest("sendDeviceState", modemProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SEND_DEVICE_STATE, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest) + " "
                    + stateType + ":" + state);
        }

        radioServiceInvokeHelper(HAL_SERVICE_MODEM, rr, "sendDeviceState", () -> {
            modemProxy.sendDeviceState(rr.mSerial, stateType, state);
        });
    }

    @Override
    public void setUnsolResponseFilter(int filter, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setUnsolResponseFilter", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_UNSOLICITED_RESPONSE_FILTER, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " " + filter);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setUnsolResponseFilter", () -> {
            networkProxy.setIndicationFilter(rr.mSerial, filter);
        });
    }

    @Override
    public void setSignalStrengthReportingCriteria(
            @NonNull List<SignalThresholdInfo> signalThresholdInfos, @Nullable Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setSignalStrengthReportingCriteria", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_SIGNAL_STRENGTH_REPORTING_CRITERIA, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setSignalStrengthReportingCriteria",
                () -> {
                    networkProxy.setSignalStrengthReportingCriteria(rr.mSerial,
                            signalThresholdInfos);
                });
    }

    @Override
    public void setLinkCapacityReportingCriteria(int hysteresisMs, int hysteresisDlKbps,
            int hysteresisUlKbps, int[] thresholdsDlKbps, int[] thresholdsUlKbps, int ran,
            Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setLinkCapacityReportingCriteria", networkProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_LINK_CAPACITY_REPORTING_CRITERIA, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setLinkCapacityReportingCriteria",
                () -> {
                    networkProxy.setLinkCapacityReportingCriteria(rr.mSerial, hysteresisMs,
                            hysteresisDlKbps, hysteresisUlKbps, thresholdsDlKbps, thresholdsUlKbps,
                            ran);
                });
    }

    @Override
    public void setSimCardPower(int state, Message result, WorkSource workSource) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("setSimCardPower", simProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_SIM_CARD_POWER, result,
                getDefaultWorkSourceIfInvalid(workSource));

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " " + state);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "setSimCardPower", () -> {
            simProxy.setSimCardPower(rr.mSerial, state);
        });
    }

    @Override
    public void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo,
            Message result) {
        Objects.requireNonNull(imsiEncryptionInfo, "ImsiEncryptionInfo cannot be null.");
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("setCarrierInfoForImsiEncryption", simProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_CARRIER_INFO_IMSI_ENCRYPTION, result,
                mRILDefaultWorkSource);
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "setCarrierInfoForImsiEncryption", () -> {
            simProxy.setCarrierInfoForImsiEncryption(rr.mSerial, imsiEncryptionInfo);
        });
    }

    @Override
    public void startNattKeepalive(int contextId, KeepalivePacketData packetData,
            int intervalMillis, Message result) {
        Objects.requireNonNull(packetData, "KeepaliveRequest cannot be null.");
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class);
        if (!canMakeRequest("startNattKeepalive", dataProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_START_KEEPALIVE, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_DATA, rr, "startNattKeepalive", () -> {
            dataProxy.startKeepalive(rr.mSerial, contextId, packetData, intervalMillis, result);
        });
    }

    @Override
    public void stopNattKeepalive(int sessionHandle, Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class);
        if (!canMakeRequest("stopNattKeepalive", dataProxy, result, RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_STOP_KEEPALIVE, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_DATA, rr, "stopNattKeepalive", () -> {
            dataProxy.stopKeepalive(rr.mSerial, sessionHandle);
        });
    }

    /**
     * Enable or disable uicc applications on the SIM.
     *
     * @param enable whether to enable or disable uicc applications.
     * @param result a Message to return to the requester
     */
    @Override
    public void enableUiccApplications(boolean enable, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("enableUiccApplications", simProxy, result, RADIO_HAL_VERSION_1_5)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_ENABLE_UICC_APPLICATIONS, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " " + enable);
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "enableUiccApplications", () -> {
            simProxy.enableUiccApplications(rr.mSerial, enable);
        });
    }

    /**
     * Whether uicc applications are enabled or not.
     *
     * @param result a Message to return to the requester
     */
    @Override
    public void areUiccApplicationsEnabled(Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("areUiccApplicationsEnabled", simProxy, result,
                RADIO_HAL_VERSION_1_5)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_UICC_APPLICATIONS_ENABLEMENT, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "areUiccApplicationsEnabled", () -> {
            simProxy.areUiccApplicationsEnabled(rr.mSerial);
        });
    }

    /**
     * Whether {@link #enableUiccApplications} is supported, which is supported in 1.5 version.
     */
    @Override
    public boolean canToggleUiccApplicationsEnablement() {
        return canMakeRequest("canToggleUiccApplicationsEnablement",
                getRadioServiceProxy(RadioSimProxy.class), null, RADIO_HAL_VERSION_1_5);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleCallSetupRequestFromSim(boolean accept, Message result) {
        RadioVoiceProxy voiceProxy = getRadioServiceProxy(RadioVoiceProxy.class);
        if (!canMakeRequest("handleCallSetupRequestFromSim", voiceProxy, result,
                RADIO_HAL_VERSION_1_4)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM,
                result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_VOICE, rr, "handleCallSetupRequestFromSim", () -> {
            voiceProxy.handleStkCallSetupRequestFromSim(rr.mSerial, accept);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getBarringInfo(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("getBarringInfo", networkProxy, result, RADIO_HAL_VERSION_1_5)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_BARRING_INFO, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "getBarringInfo", () -> {
            networkProxy.getBarringInfo(rr.mSerial);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void allocatePduSessionId(Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class);
        if (!canMakeRequest("allocatePduSessionId", dataProxy, result, RADIO_HAL_VERSION_1_6)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_ALLOCATE_PDU_SESSION_ID, result,
                mRILDefaultWorkSource);
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_DATA, rr, "allocatePduSessionId", () -> {
            dataProxy.allocatePduSessionId(rr.mSerial);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releasePduSessionId(Message result, int pduSessionId) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class);
        if (!canMakeRequest("releasePduSessionId", dataProxy, result, RADIO_HAL_VERSION_1_6)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_RELEASE_PDU_SESSION_ID, result,
                mRILDefaultWorkSource);
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_DATA, rr, "releasePduSessionId", () -> {
            dataProxy.releasePduSessionId(rr.mSerial, pduSessionId);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startHandover(Message result, int callId) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class);
        if (!canMakeRequest("startHandover", dataProxy, result, RADIO_HAL_VERSION_1_6)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_START_HANDOVER, result, mRILDefaultWorkSource);
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_DATA, rr, "startHandover", () -> {
            dataProxy.startHandover(rr.mSerial, callId);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelHandover(Message result, int callId) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class);
        if (!canMakeRequest("cancelHandover", dataProxy, result, RADIO_HAL_VERSION_1_6)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CANCEL_HANDOVER, result, mRILDefaultWorkSource);
        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_DATA, rr, "cancelHandover", () -> {
            dataProxy.cancelHandover(rr.mSerial, callId);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getSlicingConfig(Message result) {
        RadioDataProxy dataProxy = getRadioServiceProxy(RadioDataProxy.class);
        if (!canMakeRequest("getSlicingConfig", dataProxy, result, RADIO_HAL_VERSION_1_6)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_SLICING_CONFIG, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_DATA, rr, "getSlicingConfig", () -> {
            dataProxy.getSlicingConfig(rr.mSerial);
        });
    }

    @Override
    public void getSimPhonebookRecords(Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("getSimPhonebookRecords", simProxy, result, RADIO_HAL_VERSION_1_6)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_SIM_PHONEBOOK_RECORDS, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "getSimPhonebookRecords", () -> {
            simProxy.getSimPhonebookRecords(rr.mSerial);
        });
    }

    @Override
    public void getSimPhonebookCapacity(Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("getSimPhonebookCapacity", simProxy, result, RADIO_HAL_VERSION_1_6)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_SIM_PHONEBOOK_CAPACITY, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "getSimPhonebookCapacity", () -> {
            simProxy.getSimPhonebookCapacity(rr.mSerial);
        });
    }

    @Override
    public void updateSimPhonebookRecord(SimPhonebookRecord phonebookRecord, Message result) {
        RadioSimProxy simProxy = getRadioServiceProxy(RadioSimProxy.class);
        if (!canMakeRequest("updateSimPhonebookRecord", simProxy, result, RADIO_HAL_VERSION_1_6)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_UPDATE_SIM_PHONEBOOK_RECORD, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " with " + phonebookRecord.toString());
        }

        radioServiceInvokeHelper(HAL_SERVICE_SIM, rr, "updateSimPhonebookRecord", () -> {
            simProxy.updateSimPhonebookRecords(rr.mSerial, phonebookRecord);
        });
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
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setUsageSetting", networkProxy, result, RADIO_HAL_VERSION_2_0)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_USAGE_SETTING, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setUsageSetting", () -> {
            networkProxy.setUsageSetting(rr.mSerial, usageSetting);
        });
    }

    /**
     * Get the UE's usage setting.
     *
     * @param result Callback message containing the usage setting (or a failure status).
     */
    @Override
    public void getUsageSetting(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("getUsageSetting", networkProxy, result, RADIO_HAL_VERSION_2_0)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_USAGE_SETTING, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "getUsageSetting", () -> {
            networkProxy.getUsageSetting(rr.mSerial);
        });
    }

    @Override
    public void setSrvccCallInfo(SrvccConnection[] srvccConnections, Message result) {
        RadioImsProxy imsProxy = getRadioServiceProxy(RadioImsProxy.class);
        if (!canMakeRequest("setSrvccCallInfo", imsProxy, result, RADIO_HAL_VERSION_2_0)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_SRVCC_CALL_INFO, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            // Do not log function arg for privacy
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_IMS, rr, "setSrvccCallInfo", () -> {
            imsProxy.setSrvccCallInfo(rr.mSerial, RILUtils.convertToHalSrvccCall(srvccConnections));
        });
    }

    @Override
    public void updateImsRegistrationInfo(
            @RegistrationManager.ImsRegistrationState int state,
            @ImsRegistrationImplBase.ImsRegistrationTech int imsRadioTech,
            @RegistrationManager.SuggestedAction int suggestedAction,
            int capabilities, Message result) {
        RadioImsProxy imsProxy = getRadioServiceProxy(RadioImsProxy.class);
        if (!canMakeRequest("updateImsRegistrationInfo", imsProxy, result, RADIO_HAL_VERSION_2_0)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_UPDATE_IMS_REGISTRATION_INFO, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " state=" + state + ", radioTech=" + imsRadioTech
                    + ", suggested=" + suggestedAction + ", cap=" + capabilities);
        }

        android.hardware.radio.ims.ImsRegistration registrationInfo =
                new android.hardware.radio.ims.ImsRegistration();
        registrationInfo.regState = RILUtils.convertImsRegistrationState(state);
        registrationInfo.accessNetworkType = RILUtils.convertImsRegistrationTech(imsRadioTech);
        registrationInfo.suggestedAction = suggestedAction;
        registrationInfo.capabilities = RILUtils.convertImsCapability(capabilities);

        radioServiceInvokeHelper(HAL_SERVICE_IMS, rr, "updateImsRegistrationInfo", () -> {
            imsProxy.updateImsRegistrationInfo(rr.mSerial, registrationInfo);
        });
    }

    @Override
    public void startImsTraffic(int token, int trafficType, int accessNetworkType,
            int trafficDirection, Message result) {
        RadioImsProxy imsProxy = getRadioServiceProxy(RadioImsProxy.class);
        if (!canMakeRequest("startImsTraffic", imsProxy, result, RADIO_HAL_VERSION_2_0)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_START_IMS_TRAFFIC, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + "{" + token + ", " + trafficType + ", "
                    + accessNetworkType + ", " + trafficDirection + "}");
        }

        radioServiceInvokeHelper(HAL_SERVICE_IMS, rr, "startImsTraffic", () -> {
            imsProxy.startImsTraffic(rr.mSerial, token, RILUtils.convertImsTrafficType(trafficType),
                    accessNetworkType, RILUtils.convertImsTrafficDirection(trafficDirection));
        });
    }

    @Override
    public void stopImsTraffic(int token, Message result) {
        RadioImsProxy imsProxy = getRadioServiceProxy(RadioImsProxy.class);
        if (!canMakeRequest("stopImsTraffic", imsProxy, result, RADIO_HAL_VERSION_2_0)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_STOP_IMS_TRAFFIC, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + "{" + token + "}");
        }

        radioServiceInvokeHelper(HAL_SERVICE_IMS, rr, "stopImsTraffic", () -> {
            imsProxy.stopImsTraffic(rr.mSerial, token);
        });
    }

    @Override
    public void triggerEpsFallback(int reason, Message result) {
        RadioImsProxy imsProxy = getRadioServiceProxy(RadioImsProxy.class);
        if (!canMakeRequest("triggerEpsFallback", imsProxy, result, RADIO_HAL_VERSION_2_0)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_TRIGGER_EPS_FALLBACK, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " reason=" + reason);
        }

        radioServiceInvokeHelper(HAL_SERVICE_IMS, rr, "triggerEpsFallback", () -> {
            imsProxy.triggerEpsFallback(rr.mSerial, reason);
        });
    }

    @Override
    public void sendAnbrQuery(int mediaType, int direction, int bitsPerSecond, Message result) {
        RadioImsProxy imsProxy = getRadioServiceProxy(RadioImsProxy.class);
        if (!canMakeRequest("sendAnbrQuery", imsProxy, result, RADIO_HAL_VERSION_2_0)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SEND_ANBR_QUERY, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_IMS, rr, "sendAnbrQuery", () -> {
            imsProxy.sendAnbrQuery(rr.mSerial, mediaType, direction, bitsPerSecond);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEmergencyMode(int emcMode, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setEmergencyMode", networkProxy, result, RADIO_HAL_VERSION_2_1)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_EMERGENCY_MODE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " mode=" + EmergencyConstants.emergencyModeToString(emcMode));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setEmergencyMode", () -> {
            networkProxy.setEmergencyMode(rr.mSerial, emcMode);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void triggerEmergencyNetworkScan(
            @NonNull @AccessNetworkConstants.RadioAccessNetworkType int[] accessNetwork,
            @DomainSelectionService.EmergencyScanType int scanType, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("triggerEmergencyNetworkScan", networkProxy, result,
                RADIO_HAL_VERSION_2_1)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_TRIGGER_EMERGENCY_NETWORK_SCAN, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " networkType=" + RILUtils.accessNetworkTypesToString(accessNetwork)
                    + ", scanType=" + RILUtils.scanTypeToString(scanType));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "triggerEmergencyNetworkScan", () -> {
            networkProxy.triggerEmergencyNetworkScan(rr.mSerial,
                    RILUtils.convertEmergencyNetworkScanTrigger(accessNetwork, scanType));
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelEmergencyNetworkScan(boolean resetScan, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("cancelEmergencyNetworkScan", networkProxy, result,
                RADIO_HAL_VERSION_2_1)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_CANCEL_EMERGENCY_NETWORK_SCAN, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " resetScan=" + resetScan);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "cancelEmergencyNetworkScan", () -> {
            networkProxy.cancelEmergencyNetworkScan(rr.mSerial, resetScan);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exitEmergencyMode(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("exitEmergencyMode", networkProxy, result, RADIO_HAL_VERSION_2_1)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_EXIT_EMERGENCY_MODE, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "exitEmergencyMode", () -> {
            networkProxy.exitEmergencyMode(rr.mSerial);
        });
    }

    /**
     * Set if null ciphering / null integrity modes are permitted.
     *
     * @param result Callback message containing the success or failure status.
     * @param enabled true if null ciphering / null integrity modes are permitted, false otherwise
     */
    @Override
    public void setNullCipherAndIntegrityEnabled(boolean enabled, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setNullCipherAndIntegrityEnabled", networkProxy, result,
                RADIO_HAL_VERSION_2_1)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_NULL_CIPHER_AND_INTEGRITY_ENABLED, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setNullCipherAndIntegrityEnabled",
                () -> {
                    networkProxy.setNullCipherAndIntegrityEnabled(rr.mSerial, enabled);
                });
    }

    /**
     * Get if null ciphering / null integrity are enabled / disabled.
     *
     * @param result Callback message containing the success or failure status.
     */
    @Override
    public void isNullCipherAndIntegrityEnabled(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("isNullCipherAndIntegrityEnabled", networkProxy, result,
                RADIO_HAL_VERSION_2_1)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_IS_NULL_CIPHER_AND_INTEGRITY_ENABLED, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "isNullCipherAndIntegrityEnabled", () -> {
            networkProxy.isNullCipherAndIntegrityEnabled(rr.mSerial);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateImsCallStatus(@NonNull List<ImsCallInfo> imsCallInfo, Message result) {
        RadioImsProxy imsProxy = getRadioServiceProxy(RadioImsProxy.class);
        if (!canMakeRequest("updateImsCallStatus", imsProxy, result, RADIO_HAL_VERSION_2_0)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_UPDATE_IMS_CALL_STATUS, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " " + imsCallInfo);
        }
        radioServiceInvokeHelper(HAL_SERVICE_IMS, rr, "updateImsCallStatus", () -> {
            imsProxy.updateImsCallStatus(rr.mSerial, RILUtils.convertImsCallInfo(imsCallInfo));
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setN1ModeEnabled(boolean enable, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("setN1ModeEnabled", networkProxy, result, RADIO_HAL_VERSION_2_1)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_N1_MODE_ENABLED, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " enable=" + enable);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setN1ModeEnabled", () -> {
            networkProxy.setN1ModeEnabled(rr.mSerial, enable);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void isN1ModeEnabled(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest("isN1ModeEnabled", networkProxy, result, RADIO_HAL_VERSION_2_1)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_IS_N1_MODE_ENABLED, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "isN1ModeEnabled", () -> {
            networkProxy.isN1ModeEnabled(rr.mSerial);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCellularIdentifierTransparencyEnabled(boolean enable, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest(
                "setCellularIdentifierTransparencyEnabled",
                networkProxy,
                result,
                RADIO_HAL_VERSION_2_2)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_CELLULAR_IDENTIFIER_DISCLOSED_ENABLED, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " enable=" + enable);
        }

        radioServiceInvokeHelper(
                HAL_SERVICE_NETWORK,
                rr,
                "setCellularIdentifierTransparencyEnabled",
                () -> {
                    networkProxy.setCellularIdentifierTransparencyEnabled(rr.mSerial, enable);
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void isCellularIdentifierTransparencyEnabled(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest(
                "isCellularIdentifierTransparencyEnabled",
                networkProxy,
                result,
                RADIO_HAL_VERSION_2_2)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_IS_CELLULAR_IDENTIFIER_DISCLOSED_ENABLED, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(
                HAL_SERVICE_NETWORK,
                rr,
                "isCellularIdentifierTransparencyEnabled",
                () -> {
                    networkProxy.isCellularIdentifierTransparencyEnabled(rr.mSerial);
                });
    }

   /**
     * {@inheritDoc}
     */
    @Override
    public void setSecurityAlgorithmsUpdatedEnabled(boolean enable, Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest(
                "setSecurityAlgorithmsUpdatedEnabled",
                networkProxy,
                result,
                RADIO_HAL_VERSION_2_2)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_SECURITY_ALGORITHMS_UPDATED_ENABLED, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest)
                    + " enable=" + enable);
        }

        radioServiceInvokeHelper(HAL_SERVICE_NETWORK, rr, "setSecurityAlgorithmsUpdatedEnabled",
                () -> {
                    networkProxy.setSecurityAlgorithmsUpdatedEnabled(rr.mSerial, enable);
            });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void isSecurityAlgorithmsUpdatedEnabled(Message result) {
        RadioNetworkProxy networkProxy = getRadioServiceProxy(RadioNetworkProxy.class);
        if (!canMakeRequest(
                "isSecurityAlgorithmsUpdatedEnabled",
                networkProxy,
                result,
                RADIO_HAL_VERSION_2_2)) {
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_IS_SECURITY_ALGORITHMS_UPDATED_ENABLED, result,
                mRILDefaultWorkSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + RILUtils.requestToString(rr.mRequest));
        }

        radioServiceInvokeHelper(
                HAL_SERVICE_NETWORK, rr, "isSecurityAlgorithmsUpdatedEnabled", () -> {
                networkProxy.isSecurityAlgorithmsUpdatedEnabled(rr.mSerial);
            });
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
            riljLogw("processRequestAck: Unexpected solicited ack response! serial: " + serial);
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
        return processResponseInternal(HAL_SERVICE_RADIO, responseInfo.serial, responseInfo.error,
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
        return processResponseInternal(HAL_SERVICE_RADIO, responseInfo.serial, responseInfo.error,
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
                riljLogw("Unexpected solicited ack response! sn: " + serial);
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
            riljLoge("processResponse: Unexpected response! serial: " + serial
                    + ", error: " + error);
            return null;
        }
        Trace.asyncTraceForTrackEnd(Trace.TRACE_TAG_NETWORK, "RIL", rr.mSerial);

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
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PROTECTED)
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
            if (isLogOrTrace()) {
                String logStr = rr.serialString() + "< " + RILUtils.requestToString(rr.mRequest)
                        + " " + retToString(rr.mRequest, ret);
                if (RILJ_LOGD) {
                    riljLog(logStr);
                }
                Trace.instantForTrack(Trace.TRACE_TAG_NETWORK, "RIL", logStr);
            }
        } else {
            if (isLogOrTrace()) {
                String logStr = rr.serialString() + "< " + RILUtils.requestToString(rr.mRequest)
                        + " error " + rilError;
                if (RILJ_LOGD) {
                    riljLog(logStr);
                }
                Trace.instantForTrack(Trace.TRACE_TAG_NETWORK, "RIL", logStr);
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
        if (service == HAL_SERVICE_RADIO) {
            IRadio radioProxy = getRadioProxy();
            if (radioProxy != null) {
                try {
                    radioProxy.responseAcknowledgement();
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(HAL_SERVICE_RADIO, "sendAck", e);
                    riljLoge("sendAck: " + e);
                }
            } else {
                riljLoge("Error trying to send ack, radioProxy = null");
            }
        } else {
            RadioServiceProxy serviceProxy = getRadioServiceProxy(service);
            if (!serviceProxy.isEmpty()) {
                try {
                    serviceProxy.responseAcknowledgement();
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(service, "sendAck", e);
                    riljLoge("sendAck: " + e);
                }
            } else {
                riljLoge("Error trying to send ack, serviceProxy is empty");
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
                riljLog("Failed to acquire wakelock for " + rr.serialString());
                return;
            }

            switch (wakeLockType) {
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
                    riljLogw("Acquiring Invalid Wakelock type " + wakeLockType);
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
                    riljLogw("Decrementing Invalid Wakelock type " + rr.mWakeLockType);
            }
            rr.mWakeLockType = INVALID_WAKELOCK;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean clearWakeLock(int wakeLockType) {
        if (wakeLockType == FOR_WAKELOCK) {
            synchronized (mWakeLock) {
                if (mWakeLockCount == 0 && !mWakeLock.isHeld()) return false;
                riljLog("NOTE: mWakeLockCount is " + mWakeLockCount + " at time of clearing");
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
                riljLog("clearRequestList " + " mWakeLockCount=" + mWakeLockCount
                        + " mRequestList=" + count);
            }

            for (int i = 0; i < count; i++) {
                rr = mRequestList.valueAt(i);
                if (RILJ_LOGD && loggable) {
                    riljLog(i + ": [" + rr.mSerial + "] " + RILUtils.requestToString(rr.mRequest));
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
            case RIL_REQUEST_DEVICE_IMEI:

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
        } else if (req == RIL_REQUEST_START_IMS_TRAFFIC
                || req == RIL_UNSOL_CONNECTION_SETUP_FAILURE) {
            sb = new StringBuilder("{");
            Object[] info = (Object[]) ret;
            int token = (Integer) info[0];
            sb.append(token).append(", ");
            if (info[1] != null) {
                ConnectionFailureInfo failureInfo = (ConnectionFailureInfo) info[1];
                sb.append(failureInfo.getReason()).append(", ");
                sb.append(failureInfo.getCauseCode()).append(", ");
                sb.append(failureInfo.getWaitTimeMillis());
            } else {
                sb.append("null");
            }
            sb.append("}");
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
                if (isLogOrTrace()) unsljLogRet(response, infoRec.record);
                mDisplayInfoRegistrants.notifyRegistrants(
                        new AsyncResult(null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaSignalInfoRec) {
            if (mSignalInfoRegistrants != null) {
                if (isLogOrTrace()) unsljLogRet(response, infoRec.record);
                mSignalInfoRegistrants.notifyRegistrants(
                        new AsyncResult(null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaNumberInfoRec) {
            if (mNumberInfoRegistrants != null) {
                if (isLogOrTrace()) unsljLogRet(response, infoRec.record);
                mNumberInfoRegistrants.notifyRegistrants(
                        new AsyncResult(null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaRedirectingNumberInfoRec) {
            if (mRedirNumInfoRegistrants != null) {
                if (isLogOrTrace()) unsljLogRet(response, infoRec.record);
                mRedirNumInfoRegistrants.notifyRegistrants(
                        new AsyncResult(null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaLineControlInfoRec) {
            if (mLineControlInfoRegistrants != null) {
                if (isLogOrTrace()) unsljLogRet(response, infoRec.record);
                mLineControlInfoRegistrants.notifyRegistrants(
                        new AsyncResult(null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaT53ClirInfoRec) {
            if (mT53ClirInfoRegistrants != null) {
                if (isLogOrTrace()) unsljLogRet(response, infoRec.record);
                mT53ClirInfoRegistrants.notifyRegistrants(
                        new AsyncResult(null, infoRec.record, null));
            }
        } else if (infoRec.record instanceof CdmaInformationRecords.CdmaT53AudioControlInfoRec) {
            if (mT53AudCntrlInfoRegistrants != null) {
                if (isLogOrTrace()) {
                    unsljLogRet(response, infoRec.record);
                }
                mT53AudCntrlInfoRegistrants.notifyRegistrants(
                        new AsyncResult(null, infoRec.record, null));
            }
        }
    }

    void notifyRegistrantsImeiMappingChanged(ImeiInfo imeiInfo) {
        if (mImeiInfoRegistrants != null) {
            mImeiInfoRegistrants.notifyRegistrants(
                    new AsyncResult(null, imeiInfo, null));
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

    void riljLogw(String msg) {
        Rlog.w(RILJ_LOG_TAG, msg + (" [PHONE" + mPhoneId + "]"));
    }

    boolean isLogOrTrace() {
        return RILJ_LOGD || Trace.isTagEnabled(Trace.TRACE_TAG_NETWORK);
    }

    boolean isLogvOrTrace() {
        return RILJ_LOGV || Trace.isTagEnabled(Trace.TRACE_TAG_NETWORK);
    }

    @UnsupportedAppUsage
    void unsljLog(int response) {
        String logStr = RILUtils.responseToString(response);
        if (RILJ_LOGD) {
            riljLog("[UNSL]< " + logStr);
        }
        Trace.instantForTrack(Trace.TRACE_TAG_NETWORK, "RIL", logStr);
    }

    @UnsupportedAppUsage
    void unsljLogMore(int response, String more) {
        String logStr = RILUtils.responseToString(response) + " " + more;
        if (RILJ_LOGD) {
            riljLog("[UNSL]< " + logStr);
        }
        Trace.instantForTrack(Trace.TRACE_TAG_NETWORK, "RIL", logStr);
    }

    @UnsupportedAppUsage
    void unsljLogRet(int response, Object ret) {
        String logStr = RILUtils.responseToString(response) + " " + retToString(response, ret);
        if (RILJ_LOGD) {
            riljLog("[UNSL]< " + logStr);
        }
        Trace.instantForTrack(Trace.TRACE_TAG_NETWORK, "RIL", logStr);
    }

    @UnsupportedAppUsage
    void unsljLogvRet(int response, Object ret) {
        String logStr = RILUtils.responseToString(response) + " " + retToString(response, ret);
        if (RILJ_LOGV) {
            riljLogv("[UNSL]< " + logStr);
        }
        Trace.instantForTrack(Trace.TRACE_TAG_NETWORK, "RIL", logStr);
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
        pw.println(" " + mServiceProxies.get(HAL_SERVICE_DATA));
        pw.println(" " + mServiceProxies.get(HAL_SERVICE_MESSAGING));
        pw.println(" " + mServiceProxies.get(HAL_SERVICE_MODEM));
        pw.println(" " + mServiceProxies.get(HAL_SERVICE_NETWORK));
        pw.println(" " + mServiceProxies.get(HAL_SERVICE_SIM));
        pw.println(" " + mServiceProxies.get(HAL_SERVICE_VOICE));
        pw.println(" " + mServiceProxies.get(HAL_SERVICE_IMS));
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

    void notifyBarringInfoChanged(@NonNull BarringInfo barringInfo) {
        mLastBarringInfo = barringInfo;
        mBarringInfoChangedRegistrants.notifyRegistrants(new AsyncResult(null, barringInfo, null));
    }

    /**
     * Get the HAL version with a specific service.
     *
     * @param service the hal service id
     * @return the current HalVersion
     */
    public HalVersion getHalVersion(int service) {
        HalVersion halVersion = mHalVersion.get(service);
        if (halVersion == null) {
            if (isRadioServiceSupported(service)) {
                halVersion = RADIO_HAL_VERSION_UNKNOWN;
            } else {
                halVersion = RADIO_HAL_VERSION_UNSUPPORTED;
            }
        }
        return halVersion;
    }

    /**
     * Get the HAL version corresponding to the interface version of a IRadioService module.
     * @param interfaceVersion The interface version, from IRadioService#getInterfaceVersion().
     * @return The corresponding HalVersion.
     */
    public static HalVersion getServiceHalVersion(int interfaceVersion) {
        switch (interfaceVersion) {
            case 1: return RADIO_HAL_VERSION_2_0;
            case 2: return RADIO_HAL_VERSION_2_1;
            case 3: return RADIO_HAL_VERSION_2_2;
            default: return RADIO_HAL_VERSION_UNKNOWN;
        }
    }

    private static String serviceToString(@HalService int service) {
        switch (service) {
            case HAL_SERVICE_RADIO:
                return "RADIO";
            case HAL_SERVICE_DATA:
                return "DATA";
            case HAL_SERVICE_MESSAGING:
                return "MESSAGING";
            case HAL_SERVICE_MODEM:
                return "MODEM";
            case HAL_SERVICE_NETWORK:
                return "NETWORK";
            case HAL_SERVICE_SIM:
                return "SIM";
            case HAL_SERVICE_VOICE:
                return "VOICE";
            case HAL_SERVICE_IMS:
                return "IMS";
            default:
                return "UNKNOWN:" + service;
        }
    }
}
