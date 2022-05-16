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
import static com.android.internal.util.Preconditions.checkNotNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.hardware.radio.V1_0.Carrier;
import android.hardware.radio.V1_0.CarrierRestrictions;
import android.hardware.radio.V1_0.CdmaBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.CdmaSmsAck;
import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.hardware.radio.V1_0.CdmaSmsWriteArgs;
import android.hardware.radio.V1_0.DataProfileId;
import android.hardware.radio.V1_0.Dial;
import android.hardware.radio.V1_0.GsmBroadcastSmsConfigInfo;
import android.hardware.radio.V1_0.GsmSmsMessage;
import android.hardware.radio.V1_0.HardwareConfigModem;
import android.hardware.radio.V1_0.IRadio;
import android.hardware.radio.V1_0.IccIo;
import android.hardware.radio.V1_0.ImsSmsMessage;
import android.hardware.radio.V1_0.LceDataInfo;
import android.hardware.radio.V1_0.MvnoType;
import android.hardware.radio.V1_0.NvWriteItem;
import android.hardware.radio.V1_0.RadioError;
import android.hardware.radio.V1_0.RadioIndicationType;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.RadioResponseType;
import android.hardware.radio.V1_0.RadioTechnologyFamily;
import android.hardware.radio.V1_0.ResetNvType;
import android.hardware.radio.V1_0.SelectUiccSub;
import android.hardware.radio.V1_0.SimApdu;
import android.hardware.radio.V1_0.SmsWriteArgs;
import android.hardware.radio.V1_0.UusInfo;
import android.hardware.radio.V1_4.CarrierRestrictionsWithPriority;
import android.hardware.radio.V1_4.SimLockMultiSimPolicy;
import android.hardware.radio.V1_5.AccessNetwork;
import android.hardware.radio.V1_5.IndicationFilter;
import android.hardware.radio.V1_5.PersoSubstate;
import android.hardware.radio.V1_5.RadioAccessNetworks;
import android.hardware.radio.V1_6.OptionalDnn;
import android.hardware.radio.V1_6.OptionalOsAppId;
import android.hardware.radio.V1_6.OptionalSliceInfo;
import android.hardware.radio.V1_6.OptionalTrafficDescriptor;
import android.net.InetAddresses;
import android.net.KeepalivePacketData;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.HwBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.provider.Settings;
import android.service.carrier.CarrierIdentifier;
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
import android.telephony.LinkCapacityEstimate;
import android.telephony.ModemActivityInfo;
import android.telephony.NeighboringCellInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.PhoneNumberUtils;
import android.telephony.RadioAccessFamily;
import android.telephony.RadioAccessSpecifier;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SignalThresholdInfo;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyHistogram;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.PrefNetworkMode;
import android.telephony.data.ApnSetting;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataCallResponse.HandoverFailureMode;
import android.telephony.data.DataProfile;
import android.telephony.data.DataService;
import android.telephony.data.NetworkSliceInfo;
import android.telephony.data.Qos;
import android.telephony.data.QosBearerSession;
import android.telephony.data.TrafficDescriptor;
import android.telephony.emergency.EmergencyNumber;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.cat.ComprehensionTlv;
import com.android.internal.telephony.cat.ComprehensionTlvTag;
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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

    // IRadio version
    private HalVersion mRadioVersion = RADIO_HAL_VERSION_UNKNOWN;

    private static final int INDICATION_FILTERS_ALL_V1_0 =
            IndicationFilter.SIGNAL_STRENGTH
            | IndicationFilter.FULL_NETWORK_STATE
            | IndicationFilter.DATA_CALL_DORMANCY_CHANGED;
    private static final int INDICATION_FILTERS_ALL_V1_2 =
            INDICATION_FILTERS_ALL_V1_0
            | IndicationFilter.LINK_CAPACITY_ESTIMATE
            | IndicationFilter.PHYSICAL_CHANNEL_CONFIG;
    private static final  int INDICATION_FILTERS_ALL_V1_5 =
            INDICATION_FILTERS_ALL_V1_2
            | IndicationFilter.REGISTRATION_FAILURE
            | IndicationFilter.BARRING_INFO;

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
    SparseArray<RILRequest> mRequestList = new SparseArray<RILRequest>();
    static SparseArray<TelephonyHistogram> mRilTimeHistograms = new
            SparseArray<TelephonyHistogram>();

    Object[] mLastNITZTimeInfo;

    int mLastRadioPowerResult = RadioError.NONE;

    // When we are testing emergency calls using ril.test.emergencynumber, this will trigger test
    // ECbM when the call is ended.
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    AtomicBoolean mTestingEmergencyCall = new AtomicBoolean(false);

    final Integer mPhoneId;

    private static final String PROPERTY_IS_VONR_ENABLED = "persist.radio.is_vonr_enabled_";

    /**
     * A set that records if radio service is disabled in hal for
     * a specific phone id slot to avoid further getService request.
     */
    Set<Integer> mDisabledRadioServices = new HashSet();

    /* default work source which will blame phone process */
    private WorkSource mRILDefaultWorkSource;

    /* Worksource containing all applications causing wakelock to be held */
    private WorkSource mActiveWakelockWorkSource;

    /** Telephony metrics instance for logging metrics event */
    private TelephonyMetrics mMetrics = TelephonyMetrics.getInstance();
    /** Radio bug detector instance */
    private RadioBugDetector mRadioBugDetector = null;

    boolean mIsCellularSupported;
    RadioResponse mRadioResponse;
    RadioIndication mRadioIndication;
    volatile IRadio mRadioProxy = null;
    final AtomicLong mRadioProxyCookie = new AtomicLong(0);
    final RadioProxyDeathRecipient mRadioProxyDeathRecipient;
    final RilHandler mRilHandler;

    // Thread-safe HashMap to map from RIL_REQUEST_XXX constant to HalVersion.
    // This is for Radio HAL Fallback Compatibility feature. When a RIL request
    // is received, the HAL method from the mapping HalVersion here (if present),
    // instead of the latest HalVersion, will be invoked.
    private ConcurrentHashMap<Integer, HalVersion> mCompatOverrides =
            new ConcurrentHashMap<>();

    //***** Events
    static final int EVENT_WAKE_LOCK_TIMEOUT    = 2;
    static final int EVENT_ACK_WAKE_LOCK_TIMEOUT    = 4;
    static final int EVENT_BLOCKING_RESPONSE_TIMEOUT = 5;
    static final int EVENT_RADIO_PROXY_DEAD     = 6;

    //***** Constants

    static final String[] HIDL_SERVICE_NAME = {"slot1", "slot2", "slot3"};

    static final int IRADIO_GET_SERVICE_DELAY_MILLIS = 4 * 1000;

    static final String EMPTY_ALPHA_LONG = "";
    static final String EMPTY_ALPHA_SHORT = "";

    public static List<TelephonyHistogram> getTelephonyRILTimingHistograms() {
        List<TelephonyHistogram> list;
        synchronized (mRilTimeHistograms) {
            list = new ArrayList<>(mRilTimeHistograms.size());
            for (int i = 0; i < mRilTimeHistograms.size(); i++) {
                TelephonyHistogram entry = new TelephonyHistogram(mRilTimeHistograms.valueAt(i));
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
                                            + requestToString(rr.mRequest));
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
                    int serial = msg.arg1;
                    rr = findAndRemoveRequestFromList(serial);
                    // If the request has already been processed, do nothing
                    if(rr == null) {
                        break;
                    }

                    //build a response if expected
                    if (rr.mResult != null) {
                        Object timeoutResponse = getResponseForTimedOutRILRequest(rr);
                        AsyncResult.forMessage( rr.mResult, timeoutResponse, null);
                        rr.mResult.sendToTarget();
                        mMetrics.writeOnRilTimeoutResponse(mPhoneId, rr.mSerial, rr.mRequest);
                    }

                    decrementWakeLock(rr);
                    rr.release();
                    break;

                case EVENT_RADIO_PROXY_DEAD:
                    riljLog("handleMessage: EVENT_RADIO_PROXY_DEAD cookie = " + msg.obj +
                            " mRadioProxyCookie = " + mRadioProxyCookie.get());
                    if ((long) msg.obj == mRadioProxyCookie.get()) {
                        resetProxyAndRequestList();
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
            mRilHandler.sendMessage(mRilHandler.obtainMessage(EVENT_RADIO_PROXY_DEAD, cookie));
        }
    }

    private synchronized void resetProxyAndRequestList() {
        mRadioProxy = null;

        // increment the cookie so that death notification can be ignored
        mRadioProxyCookie.incrementAndGet();

        setRadioState(TelephonyManager.RADIO_POWER_UNAVAILABLE, true /* forceNotifyRegistrants */);

        RILRequest.resetSerial();
        // Clear request list on close
        clearRequestList(RADIO_NOT_AVAILABLE, false);

        getRadioProxy(null);
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
            if (mDisabledRadioServices.contains(mPhoneId)) {
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
                    mRadioProxy.linkToDeath(mRadioProxyDeathRecipient,
                            mRadioProxyCookie.incrementAndGet());
                    mRadioProxy.setResponseFunctions(mRadioResponse, mRadioIndication);
                } else {
                    mDisabledRadioServices.add(mPhoneId);
                    riljLoge("getRadioProxy: mRadioProxy for "
                            + HIDL_SERVICE_NAME[mPhoneId] + " is disabled");
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

    @Override
    public synchronized void onSlotActiveStatusChange(boolean active) {
        if (active) {
            // Try to connect to RIL services and set response functions.
            getRadioProxy(null);
        } else {
            resetProxyAndRequestList();
        }
    }

    //***** Constructors

    @UnsupportedAppUsage
    public RIL(Context context, int allowedNetworkTypes, int cdmaSubscription) {
        this(context, allowedNetworkTypes, cdmaSubscription, null);
    }

    @UnsupportedAppUsage
    public RIL(Context context, int allowedNetworkTypes,
            int cdmaSubscription, Integer instanceId) {
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

        TelephonyManager tm = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        mIsCellularSupported = tm.isVoiceCapable() || tm.isSmsCapable() || tm.isDataCapable();

        mRadioResponse = new RadioResponse(this);
        mRadioIndication = new RadioIndication(this);
        mRilHandler = new RilHandler();
        mRadioProxyDeathRecipient = new RadioProxyDeathRecipient();

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
        tdc.registerRIL(this);

        // set radio callback; needed to set RadioIndication callback (should be done after
        // wakelock stuff is initialized above as callbacks are received on separate binder threads)
        getRadioProxy(null);

        if (RILJ_LOGD) {
            riljLog("Radio HAL version: " + mRadioVersion);
        }
    }

    private boolean isRadioBugDetectionEnabled() {
        return Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.ENABLE_RADIO_BUG_DETECTION,
                1) != 0;
    }

    @Override
    public void setOnNITZTime(Handler h, int what, Object obj) {
        super.setOnNITZTime(h, what, obj);

        // Send the last NITZ time if we have it
        if (mLastNITZTimeInfo != null) {
            mNITZTimeRegistrant
                .notifyRegistrant(
                    new AsyncResult (null, mLastNITZTimeInfo, null));
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

    private void handleRadioProxyExceptionForRR(RILRequest rr, String caller, Exception e) {
        riljLoge(caller + ": " + e);
        resetProxyAndRequestList();
    }

    private static String convertNullToEmptyString(String string) {
        return string != null ? string : "";
    }

    @Override
    public void getIccCardStatus(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_SIM_STATUS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getIccCardStatus(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getIccCardStatus", e);
            }
        }
    }

    @Override
    public void getIccSlotsStatus(Message result) {
        if (result != null) {
            AsyncResult.forMessage(result, null,
                    CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
            result.sendToTarget();
        }
    }

    @Override
    public void setLogicalToPhysicalSlotMapping(int[] physicalSlots, Message result) {
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
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_PIN, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " aid = " + aid);
            }

            try {
                radioProxy.supplyIccPinForApp(rr.mSerial,
                        convertNullToEmptyString(pin),
                        convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "supplyIccPinForApp", e);
            }
        }
    }

    @Override
    public void supplyIccPuk(String puk, String newPin, Message result) {
        supplyIccPukForApp(puk, newPin, null, result);
    }

    @Override
    public void supplyIccPukForApp(String puk, String newPin, String aid, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_PUK, result,
                    mRILDefaultWorkSource);

            String pukStr = convertNullToEmptyString(puk);
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " isPukEmpty = " + pukStr.isEmpty()
                        + " aid = " + aid);
            }

            try {
                radioProxy.supplyIccPukForApp(rr.mSerial,
                        pukStr,
                        convertNullToEmptyString(newPin),
                        convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "supplyIccPukForApp", e);
            }
        }
    }

    @Override
    public void supplyIccPin2(String pin, Message result) {
        supplyIccPin2ForApp(pin, null, result);
    }

    @Override
    public void supplyIccPin2ForApp(String pin, String aid, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_PIN2, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " aid = " + aid);
            }

            try {
                radioProxy.supplyIccPin2ForApp(rr.mSerial,
                        convertNullToEmptyString(pin),
                        convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "supplyIccPin2ForApp", e);
            }
        }
    }

    @Override
    public void supplyIccPuk2(String puk2, String newPin2, Message result) {
        supplyIccPuk2ForApp(puk2, newPin2, null, result);
    }

    @Override
    public void supplyIccPuk2ForApp(String puk, String newPin2, String aid, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_PUK2, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " aid = " + aid);
            }

            try {
                radioProxy.supplyIccPuk2ForApp(rr.mSerial,
                        convertNullToEmptyString(puk),
                        convertNullToEmptyString(newPin2),
                        convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "supplyIccPuk2ForApp", e);
            }
        }
    }

    @Override
    public void changeIccPin(String oldPin, String newPin, Message result) {
        changeIccPinForApp(oldPin, newPin, null, result);
    }

    @Override
    public void changeIccPinForApp(String oldPin, String newPin, String aid, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CHANGE_SIM_PIN, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " oldPin = "
                        + oldPin + " newPin = " + newPin + " aid = " + aid);
            }

            try {
                radioProxy.changeIccPinForApp(rr.mSerial,
                        convertNullToEmptyString(oldPin),
                        convertNullToEmptyString(newPin),
                        convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "changeIccPinForApp", e);
            }
        }
    }

    @Override
    public void changeIccPin2(String oldPin2, String newPin2, Message result) {
        changeIccPin2ForApp(oldPin2, newPin2, null, result);
    }

    @Override
    public void changeIccPin2ForApp(String oldPin2, String newPin2, String aid, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CHANGE_SIM_PIN2, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " oldPin = "
                        + oldPin2 + " newPin = " + newPin2 + " aid = " + aid);
            }

            try {
                radioProxy.changeIccPin2ForApp(rr.mSerial,
                        convertNullToEmptyString(oldPin2),
                        convertNullToEmptyString(newPin2),
                        convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "changeIccPin2ForApp", e);
            }
        }
    }

    @Override
    public void supplyNetworkDepersonalization(String netpin, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " netpin = "
                        + netpin);
            }

            try {
                radioProxy.supplyNetworkDepersonalization(rr.mSerial,
                        convertNullToEmptyString(netpin));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "supplyNetworkDepersonalization", e);
            }
        }
    }

    @Override
    public void supplySimDepersonalization(PersoSubState persoType,
            String controlKey, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        // IRadio V1.5
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
            android.hardware.radio.V1_5.IRadio radioProxy15 =
                (android.hardware.radio.V1_5.IRadio) radioProxy;
            if (radioProxy15 != null) {
                RILRequest rr = obtainRequest(RIL_REQUEST_ENTER_SIM_DEPERSONALIZATION, result,
                        mRILDefaultWorkSource);
                if (RILJ_LOGD) {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " controlKey = "
                        + controlKey + " persoType" + persoType);
                }
                try {
                    radioProxy15.supplySimDepersonalization(rr.mSerial,
                            convertPersoTypeToHalPersoType(persoType),
                            convertNullToEmptyString(controlKey));
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "supplySimDepersonalization", e);
                }
            }
        } else {
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    private static int convertPersoTypeToHalPersoType(PersoSubState persoType) {

        switch(persoType) {

            case PERSOSUBSTATE_IN_PROGRESS:
                return PersoSubstate.IN_PROGRESS;
            case  PERSOSUBSTATE_READY:
                return PersoSubstate.READY;
            case PERSOSUBSTATE_SIM_NETWORK:
                return PersoSubstate.SIM_NETWORK;
            case PERSOSUBSTATE_SIM_NETWORK_SUBSET:
                return PersoSubstate.SIM_NETWORK_SUBSET;
            case PERSOSUBSTATE_SIM_CORPORATE:
                return PersoSubstate.SIM_CORPORATE;
            case PERSOSUBSTATE_SIM_SERVICE_PROVIDER:
                return PersoSubstate.SIM_SERVICE_PROVIDER;
            case PERSOSUBSTATE_SIM_SIM:
                return PersoSubstate.SIM_SIM;
            case PERSOSUBSTATE_SIM_NETWORK_PUK:
                return PersoSubstate.SIM_NETWORK_PUK;
            case PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK:
                return PersoSubstate.SIM_NETWORK_SUBSET_PUK;
            case PERSOSUBSTATE_SIM_CORPORATE_PUK:
                return PersoSubstate.SIM_CORPORATE_PUK;
            case PERSOSUBSTATE_SIM_SERVICE_PROVIDER_PUK:
                return PersoSubstate.SIM_SERVICE_PROVIDER_PUK;
            case PERSOSUBSTATE_SIM_SIM_PUK:
                return PersoSubstate.SIM_SIM_PUK;
            case PERSOSUBSTATE_RUIM_NETWORK1:
                return PersoSubstate.RUIM_NETWORK1;
            case PERSOSUBSTATE_RUIM_NETWORK2:
                return PersoSubstate.RUIM_NETWORK2;
            case PERSOSUBSTATE_RUIM_HRPD:
                return PersoSubstate.RUIM_HRPD;
            case PERSOSUBSTATE_RUIM_CORPORATE:
                return PersoSubstate.RUIM_CORPORATE;
            case PERSOSUBSTATE_RUIM_SERVICE_PROVIDER:
                return PersoSubstate.RUIM_SERVICE_PROVIDER;
            case PERSOSUBSTATE_RUIM_RUIM:
                return PersoSubstate.RUIM_RUIM;
            case PERSOSUBSTATE_RUIM_NETWORK1_PUK:
                return PersoSubstate.RUIM_NETWORK1_PUK;
            case PERSOSUBSTATE_RUIM_NETWORK2_PUK:
                return PersoSubstate.RUIM_NETWORK2_PUK;
            case PERSOSUBSTATE_RUIM_HRPD_PUK:
                return PersoSubstate.RUIM_HRPD_PUK;
            case PERSOSUBSTATE_RUIM_CORPORATE_PUK:
                return PersoSubstate.RUIM_CORPORATE_PUK;
            case PERSOSUBSTATE_RUIM_SERVICE_PROVIDER_PUK:
                return PersoSubstate.RUIM_SERVICE_PROVIDER_PUK;
            case PERSOSUBSTATE_RUIM_RUIM_PUK:
                return PersoSubstate.RUIM_RUIM_PUK;
            case PERSOSUBSTATE_SIM_SPN:
                return PersoSubstate.SIM_SPN;
            case PERSOSUBSTATE_SIM_SPN_PUK:
                return PersoSubstate.SIM_SPN_PUK;
            case PERSOSUBSTATE_SIM_SP_EHPLMN:
                return PersoSubstate.SIM_SP_EHPLMN;
            case PERSOSUBSTATE_SIM_SP_EHPLMN_PUK:
                return PersoSubstate.SIM_SP_EHPLMN_PUK;
            case PERSOSUBSTATE_SIM_ICCID:
                return PersoSubstate.SIM_ICCID;
            case PERSOSUBSTATE_SIM_ICCID_PUK:
                return PersoSubstate.SIM_ICCID_PUK;
            case PERSOSUBSTATE_SIM_IMPI:
                return PersoSubstate.SIM_IMPI;
            case PERSOSUBSTATE_SIM_IMPI_PUK:
                return PersoSubstate.SIM_IMPI_PUK;
            case PERSOSUBSTATE_SIM_NS_SP:
                return PersoSubstate.SIM_NS_SP;
            case PERSOSUBSTATE_SIM_NS_SP_PUK:
                return PersoSubstate.SIM_NS_SP_PUK;
            default:
                return PersoSubstate.UNKNOWN;
        }
    }

    @Override
    public void getCurrentCalls(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_CURRENT_CALLS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                    // IRadio V1.6
                    android.hardware.radio.V1_6.IRadio radioProxy16 =
                            (android.hardware.radio.V1_6.IRadio) radioProxy;
                    radioProxy16.getCurrentCalls_1_6(rr.mSerial);
                } else {
                    radioProxy.getCurrentCalls(rr.mSerial);
                }
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getCurrentCalls", e);
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
        IRadio radioProxy = getRadioProxy(result);
        if (mRadioVersion.less(RADIO_HAL_VERSION_1_3)) {
            if (RILJ_LOGV) riljLog("enableModem: not supported.");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
            return;
        }

        android.hardware.radio.V1_3.IRadio radioProxy13 =
                (android.hardware.radio.V1_3.IRadio) radioProxy;
        if (radioProxy13 != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ENABLE_MODEM, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " enable = "
                        + enable);
            }

            try {
                radioProxy13.enableModem(rr.mSerial, enable);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "enableModem", e);
            }
        }
    }

    @Override
    public void setSystemSelectionChannels(@NonNull List<RadioAccessSpecifier> specifiers,
            Message onComplete) {
        IRadio radioProxy = getRadioProxy(onComplete);
        if (mRadioVersion.less(RADIO_HAL_VERSION_1_3)) {
            if (RILJ_LOGV) riljLog("setSystemSelectionChannels: not supported.");
            if (onComplete != null) {
                AsyncResult.forMessage(onComplete, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                onComplete.sendToTarget();
            }
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_SYSTEM_SELECTION_CHANNELS, onComplete,
                mRILDefaultWorkSource);

        if (mRadioVersion.less(RADIO_HAL_VERSION_1_5)) {
            android.hardware.radio.V1_3.IRadio radioProxy13 =
                    (android.hardware.radio.V1_3.IRadio) radioProxy;
            if (radioProxy13 != null) {
                if (RILJ_LOGD) {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                            + " setSystemSelectionChannels_1.3= "
                            + specifiers);
                }

                ArrayList<android.hardware.radio.V1_1.RadioAccessSpecifier> halSpecifiers =
                        specifiers.stream()
                                .map(this::convertRadioAccessSpecifierToRadioHAL)
                                .collect(Collectors.toCollection(ArrayList::new));

                try {
                    radioProxy13.setSystemSelectionChannels(rr.mSerial,
                            !halSpecifiers.isEmpty(),
                            halSpecifiers);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setSystemSelectionChannels", e);
                }
            }
        }

        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
            android.hardware.radio.V1_5.IRadio radioProxy15 =
                    (android.hardware.radio.V1_5.IRadio) radioProxy;

            if (radioProxy15 != null) {
                if (RILJ_LOGD) {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                            + " setSystemSelectionChannels_1.5= "
                            + specifiers);
                }

                ArrayList<android.hardware.radio.V1_5.RadioAccessSpecifier> halSpecifiers =
                        specifiers.stream()
                                .map(this::convertRadioAccessSpecifierToRadioHAL_1_5)
                                .collect(Collectors.toCollection(ArrayList::new));

                try {
                    radioProxy15.setSystemSelectionChannels_1_5(rr.mSerial,
                            !halSpecifiers.isEmpty(),
                            halSpecifiers);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setSystemSelectionChannels", e);
                }
            }
        }
    }

    @Override
    public void getSystemSelectionChannels(Message onComplete) {
        IRadio radioProxy = getRadioProxy(onComplete);
        if (mRadioVersion.less(RADIO_HAL_VERSION_1_6)) {
            if (RILJ_LOGV) riljLog("getSystemSelectionChannels: not supported.");
            if (onComplete != null) {
                AsyncResult.forMessage(onComplete, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                onComplete.sendToTarget();
            }
            return;
        }

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_SYSTEM_SELECTION_CHANNELS, onComplete,
                mRILDefaultWorkSource);

        android.hardware.radio.V1_6.IRadio radioProxy16 =
                (android.hardware.radio.V1_6.IRadio) radioProxy;

        if (radioProxy16 != null) {
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " getSystemSelectionChannels");
            }

            try {
                radioProxy16.getSystemSelectionChannels(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getSystemSelectionChannels", e);
            }
        }
    }

    @Override
    public void getModemStatus(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (mRadioVersion.less(RADIO_HAL_VERSION_1_3)) {
            if (RILJ_LOGV) riljLog("getModemStatus: not supported.");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
            return;
        }

        android.hardware.radio.V1_3.IRadio radioProxy13 =
                (android.hardware.radio.V1_3.IRadio) radioProxy;
        if (radioProxy13 != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_MODEM_STATUS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy13.getModemStackStatus(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getModemStatus", e);
            }
        }
    }

    @Override
    public void dial(String address, boolean isEmergencyCall, EmergencyNumber emergencyNumberInfo,
                     boolean hasKnownUserIntentEmergency, int clirMode, UUSInfo uusInfo,
                     Message result) {
        if (isEmergencyCall && mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_4)
                && emergencyNumberInfo != null) {
            emergencyDial(address, emergencyNumberInfo, hasKnownUserIntentEmergency, clirMode,
                    uusInfo, result);
            return;
        }
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DIAL, result,
                    mRILDefaultWorkSource);

            Dial dialInfo = new Dial();
            dialInfo.address = convertNullToEmptyString(address);
            dialInfo.clir = clirMode;
            if (uusInfo != null) {
                UusInfo info = new UusInfo();
                info.uusType = uusInfo.getType();
                info.uusDcs = uusInfo.getDcs();
                info.uusData = new String(uusInfo.getUserData());
                dialInfo.uusInfo.add(info);
            }

            if (RILJ_LOGD) {
                // Do not log function arg for privacy
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.dial(rr.mSerial, dialInfo);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "dial", e);
            }
        }
    }

    private void emergencyDial(String address, EmergencyNumber emergencyNumberInfo,
            boolean hasKnownUserIntentEmergency, int clirMode, UUSInfo uusInfo, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_EMERGENCY_DIAL, result,
                    mRILDefaultWorkSource);
            Dial dialInfo = new Dial();
            dialInfo.address = convertNullToEmptyString(address);
            dialInfo.clir = clirMode;
            if (uusInfo != null) {
                UusInfo info = new UusInfo();
                info.uusType = uusInfo.getType();
                info.uusDcs = uusInfo.getDcs();
                info.uusData = new String(uusInfo.getUserData());
                dialInfo.uusInfo.add(info);
            }

            if (RILJ_LOGD) {
                // Do not log function arg for privacy
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                android.hardware.radio.V1_6.IRadio radioProxy16 =
                        (android.hardware.radio.V1_6.IRadio) radioProxy;
                try {
                    radioProxy16.emergencyDial_1_6(rr.mSerial, dialInfo,
                        emergencyNumberInfo.getEmergencyServiceCategoryBitmaskInternalDial(),
                        emergencyNumberInfo.getEmergencyUrns() != null
                                ? new ArrayList(emergencyNumberInfo.getEmergencyUrns())
                                        : new ArrayList<>(),
                        emergencyNumberInfo.getEmergencyCallRouting(),
                        hasKnownUserIntentEmergency,
                        emergencyNumberInfo.getEmergencyNumberSourceBitmask()
                                == EmergencyNumber.EMERGENCY_NUMBER_SOURCE_TEST);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "emergencyDial_1_6", e);
                }
            } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_4)) {
                android.hardware.radio.V1_4.IRadio radioProxy14 =
                        (android.hardware.radio.V1_4.IRadio) radioProxy;
                try {
                    radioProxy14.emergencyDial(rr.mSerial, dialInfo,
                            emergencyNumberInfo.getEmergencyServiceCategoryBitmaskInternalDial(),
                            emergencyNumberInfo.getEmergencyUrns() != null
                                    ? new ArrayList(emergencyNumberInfo.getEmergencyUrns())
                                            : new ArrayList<>(),
                            emergencyNumberInfo.getEmergencyCallRouting(),
                            hasKnownUserIntentEmergency,
                            emergencyNumberInfo.getEmergencyNumberSourceBitmask()
                                    == EmergencyNumber.EMERGENCY_NUMBER_SOURCE_TEST);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "emergencyDial", e);
                }
            } else {
                riljLoge("emergencyDial is not supported with 1.4 below IRadio");
            }
        }
    }

    @Override
    public void getIMSI(Message result) {
        getIMSIForApp(null, result);
    }

    @Override
    public void getIMSIForApp(String aid, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_IMSI, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString()
                        + ">  " + requestToString(rr.mRequest) + " aid = " + aid);
            }
            try {
                radioProxy.getImsiForApp(rr.mSerial, convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getIMSIForApp", e);
            }
        }
    }

    @Override
    public void hangupConnection(int gsmIndex, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_HANGUP, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " gsmIndex = "
                        + gsmIndex);
            }

            try {
                radioProxy.hangup(rr.mSerial, gsmIndex);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "hangupConnection", e);
            }
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void hangupWaitingOrBackground(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.hangupWaitingOrBackground(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "hangupWaitingOrBackground", e);
            }
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void hangupForegroundResumeBackground(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.hangupForegroundResumeBackground(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "hangupForegroundResumeBackground", e);
            }
        }
    }

    @Override
    public void switchWaitingOrHoldingAndActive(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.switchWaitingOrHoldingAndActive(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "switchWaitingOrHoldingAndActive", e);
            }
        }
    }

    @Override
    public void conference(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CONFERENCE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.conference(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "conference", e);
            }
        }
    }

    @Override
    public void rejectCall(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_UDUB, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.rejectCall(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "rejectCall", e);
            }
        }
    }

    @Override
    public void getLastCallFailCause(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_LAST_CALL_FAIL_CAUSE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getLastCallFailCause(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getLastCallFailCause", e);
            }
        }
    }

    @Override
    public void getSignalStrength(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIGNAL_STRENGTH, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                android.hardware.radio.V1_6.IRadio radioProxy16 =
                        (android.hardware.radio.V1_6.IRadio) radioProxy;
                try {
                    radioProxy16.getSignalStrength_1_6(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "getSignalStrength_1_6", e);
                }
            } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_4)) {
                android.hardware.radio.V1_4.IRadio radioProxy14 =
                        (android.hardware.radio.V1_4.IRadio) radioProxy;
                try {
                    radioProxy14.getSignalStrength_1_4(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "getSignalStrength_1_4", e);
                }
            } else {
                try {
                    radioProxy.getSignalStrength(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "getSignalStrength", e);
                }
            }
        }
    }

    @Override
    public void getVoiceRegistrationState(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_VOICE_REGISTRATION_STATE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            HalVersion overrideHalVersion = getCompatVersion(RIL_REQUEST_VOICE_REGISTRATION_STATE);
            if (RILJ_LOGD) {
                riljLog("getVoiceRegistrationState: overrideHalVersion=" + overrideHalVersion);
            }

            if ((overrideHalVersion == null
                        || overrideHalVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6))
                    && mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                final android.hardware.radio.V1_6.IRadio radioProxy16 =
                        (android.hardware.radio.V1_6.IRadio) radioProxy;
                try {
                    radioProxy16.getVoiceRegistrationState_1_6(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "getVoiceRegistrationState_1_6", e);
                }
            } else if ((overrideHalVersion == null
                        || overrideHalVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5))
                    && mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
                final android.hardware.radio.V1_5.IRadio radioProxy15 =
                        (android.hardware.radio.V1_5.IRadio) radioProxy;
                try {
                    radioProxy15.getVoiceRegistrationState_1_5(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "getVoiceRegistrationState_1_5", e);
                }
            } else {
                try {
                    radioProxy.getVoiceRegistrationState(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "getVoiceRegistrationState", e);
                }
            }
        }
    }

    @Override
    public void getDataRegistrationState(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DATA_REGISTRATION_STATE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            HalVersion overrideHalVersion = getCompatVersion(RIL_REQUEST_DATA_REGISTRATION_STATE);
            if (RILJ_LOGD) {
                riljLog("getDataRegistrationState: overrideHalVersion=" + overrideHalVersion);
            }

            if ((overrideHalVersion == null
                        || overrideHalVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6))
                    && mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                final android.hardware.radio.V1_6.IRadio radioProxy16 =
                        (android.hardware.radio.V1_6.IRadio) radioProxy;
                try {
                    radioProxy16.getDataRegistrationState_1_6(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "getDataRegistrationState_1_6", e);
                }
            } else if ((overrideHalVersion == null
                        || overrideHalVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5))
                    && mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
                final android.hardware.radio.V1_5.IRadio radioProxy15 =
                        (android.hardware.radio.V1_5.IRadio) radioProxy;
                try {
                    radioProxy15.getDataRegistrationState_1_5(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "getDataRegistrationState_1_5", e);
                }
            } else {
                try {
                    radioProxy.getDataRegistrationState(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "getDataRegistrationState", e);
                }
            }
        }
    }

    @Override
    public void getOperator(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_OPERATOR, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getOperator(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getOperator", e);
            }
        }
    }

    @UnsupportedAppUsage
    @Override
    public void setRadioPower(boolean on, boolean forEmergencyCall,
            boolean preferredForEmergencyCall, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_RADIO_POWER, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " on = " + on + " forEmergencyCall= " + forEmergencyCall
                        + " preferredForEmergencyCall="  + preferredForEmergencyCall);
            }

            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                android.hardware.radio.V1_6.IRadio radioProxy16 =
                        (android.hardware.radio.V1_6.IRadio) radioProxy;
                try {
                    radioProxy16.setRadioPower_1_6(rr.mSerial, on, forEmergencyCall,
                            preferredForEmergencyCall);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setRadioPower_1_6", e);
                }
            } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
                android.hardware.radio.V1_5.IRadio radioProxy15 =
                        (android.hardware.radio.V1_5.IRadio) radioProxy;
                try {
                    radioProxy15.setRadioPower_1_5(rr.mSerial, on, forEmergencyCall,
                            preferredForEmergencyCall);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setRadioPower_1_5", e);
                }
            } else {
                try {
                    radioProxy.setRadioPower(rr.mSerial, on);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setRadioPower", e);
                }
            }
        }
    }

    @Override
    public void sendDtmf(char c, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DTMF, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                // Do not log function arg for privacy
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.sendDtmf(rr.mSerial, c + "");
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "sendDtmf", e);
            }
        }
    }

    private GsmSmsMessage constructGsmSendSmsRilRequest(String smscPdu, String pdu) {
        GsmSmsMessage msg = new GsmSmsMessage();
        msg.smscPdu = smscPdu == null ? "" : smscPdu;
        msg.pdu = pdu == null ? "" : pdu;
        return msg;
    }

    @Override
    public void sendSMS(String smscPdu, String pdu, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SEND_SMS, result,
                    mRILDefaultWorkSource);

            // Do not log function args for privacy
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            GsmSmsMessage msg = constructGsmSendSmsRilRequest(smscPdu, pdu);
            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                try {
                    android.hardware.radio.V1_6.IRadio radioProxy16 =
                            (android.hardware.radio.V1_6.IRadio) radioProxy;
                    radioProxy16.sendSms_1_6(rr.mSerial, msg);
                    mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_GSM,
                            SmsSession.Event.Format.SMS_FORMAT_3GPP,
                            getOutgoingSmsMessageId(result));
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "sendSMS", e);
                }
            } else {
                try {
                    radioProxy.sendSms(rr.mSerial, msg);
                    mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_GSM,
                            SmsSession.Event.Format.SMS_FORMAT_3GPP,
                            getOutgoingSmsMessageId(result));
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "sendSMS", e);
                }
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
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SEND_SMS_EXPECT_MORE, result,
                    mRILDefaultWorkSource);

            // Do not log function arg for privacy
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            GsmSmsMessage msg = constructGsmSendSmsRilRequest(smscPdu, pdu);
            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                try {
                    android.hardware.radio.V1_6.IRadio radioProxy16 =
                            (android.hardware.radio.V1_6.IRadio) radioProxy;
                    radioProxy16.sendSmsExpectMore_1_6(rr.mSerial, msg);
                    mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_GSM,
                            SmsSession.Event.Format.SMS_FORMAT_3GPP,
                            getOutgoingSmsMessageId(result));
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "sendSMSExpectMore", e);
                }
            } else {
                try {
                    radioProxy.sendSMSExpectMore(rr.mSerial, msg);
                    mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_GSM,
                            SmsSession.Event.Format.SMS_FORMAT_3GPP,
                            getOutgoingSmsMessageId(result));
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "sendSMSExpectMore", e);
                }
            }
        }
    }

    /**
     * Convert MVNO type string into MvnoType defined in types.hal.
     * @param mvnoType MVNO type
     * @return MVNO type in integer
     */
    private static int convertToHalMvnoType(String mvnoType) {
        switch (mvnoType) {
            case "imsi" : return MvnoType.IMSI;
            case "gid" : return MvnoType.GID;
            case "spn" : return MvnoType.SPN;
            default: return MvnoType.NONE;
        }
    }

    /**
     * Convert to DataProfileInfo defined in radio/1.0/types.hal
     * @param dp Data profile
     * @return A converted data profile
     */
    private static android.hardware.radio.V1_0.DataProfileInfo convertToHalDataProfile10(
            DataProfile dp) {
        android.hardware.radio.V1_0.DataProfileInfo dpi =
                new android.hardware.radio.V1_0.DataProfileInfo();

        dpi.profileId = dp.getProfileId();
        dpi.apn = dp.getApn();
        dpi.protocol = ApnSetting.getProtocolStringFromInt(dp.getProtocolType());
        dpi.roamingProtocol = ApnSetting.getProtocolStringFromInt(dp.getRoamingProtocolType());
        dpi.authType = dp.getAuthType();
        dpi.user = dp.getUserName();
        dpi.password = dp.getPassword();
        dpi.type = dp.getType();
        dpi.maxConnsTime = dp.getMaxConnectionsTime();
        dpi.maxConns = dp.getMaxConnections();
        dpi.waitTime = dp.getWaitTime();
        dpi.enabled = dp.isEnabled();
        dpi.supportedApnTypesBitmap = dp.getSupportedApnTypesBitmask();
        // Shift by 1 bit due to the discrepancy between
        // android.hardware.radio.V1_0.RadioAccessFamily and the bitmask version of
        // ServiceState.RIL_RADIO_TECHNOLOGY_XXXX.
        dpi.bearerBitmap = ServiceState.convertNetworkTypeBitmaskToBearerBitmask(
                dp.getBearerBitmask()) << 1;
        dpi.mtu = dp.getMtuV4();
        dpi.mvnoType = MvnoType.NONE;
        dpi.mvnoMatchData = "";

        return dpi;
    }

    /**
     * Convert to DataProfileInfo defined in radio/1.4/types.hal
     * @param dp Data profile
     * @return A converted data profile
     */
    private static android.hardware.radio.V1_4.DataProfileInfo convertToHalDataProfile14(
            DataProfile dp) {
        android.hardware.radio.V1_4.DataProfileInfo dpi =
                new android.hardware.radio.V1_4.DataProfileInfo();

        dpi.apn = dp.getApn();
        dpi.protocol = dp.getProtocolType();
        dpi.roamingProtocol = dp.getRoamingProtocolType();
        dpi.authType = dp.getAuthType();
        dpi.user = dp.getUserName();
        dpi.password = dp.getPassword();
        dpi.type = dp.getType();
        dpi.maxConnsTime = dp.getMaxConnectionsTime();
        dpi.maxConns = dp.getMaxConnections();
        dpi.waitTime = dp.getWaitTime();
        dpi.enabled = dp.isEnabled();
        dpi.supportedApnTypesBitmap = dp.getSupportedApnTypesBitmask();
        // Shift by 1 bit due to the discrepancy between
        // android.hardware.radio.V1_0.RadioAccessFamily and the bitmask version of
        // ServiceState.RIL_RADIO_TECHNOLOGY_XXXX.
        dpi.bearerBitmap = ServiceState.convertNetworkTypeBitmaskToBearerBitmask(
                dp.getBearerBitmask()) << 1;
        dpi.mtu = dp.getMtuV4();
        dpi.persistent = dp.isPersistent();
        dpi.preferred = dp.isPreferred();

        // profile id is only meaningful when it's persistent on the modem.
        dpi.profileId = (dpi.persistent) ? dp.getProfileId() : DataProfileId.INVALID;

        return dpi;
    }

    private static OptionalSliceInfo convertToHalSliceInfo(@Nullable NetworkSliceInfo sliceInfo) {
        OptionalSliceInfo optionalSliceInfo = new OptionalSliceInfo();
        if (sliceInfo == null) {
            return optionalSliceInfo;
        }

        android.hardware.radio.V1_6.SliceInfo si = new android.hardware.radio.V1_6.SliceInfo();
        si.sst = (byte) sliceInfo.getSliceServiceType();
        si.mappedHplmnSst = (byte) sliceInfo.getMappedHplmnSliceServiceType();
        si.sliceDifferentiator = sliceInfo.getSliceDifferentiator();
        si.mappedHplmnSD = sliceInfo.getMappedHplmnSliceDifferentiator();
        optionalSliceInfo.value(si);
        return optionalSliceInfo;
    }

    private static OptionalTrafficDescriptor convertToHalTrafficDescriptor(
            @Nullable TrafficDescriptor trafficDescriptor) {
        OptionalTrafficDescriptor optionalTrafficDescriptor = new OptionalTrafficDescriptor();
        if (trafficDescriptor == null) {
            return optionalTrafficDescriptor;
        }

        android.hardware.radio.V1_6.TrafficDescriptor td =
                new android.hardware.radio.V1_6.TrafficDescriptor();

        OptionalDnn optionalDnn = new OptionalDnn();
        if (trafficDescriptor.getDataNetworkName() != null) {
            optionalDnn.value(trafficDescriptor.getDataNetworkName());
        }
        td.dnn = optionalDnn;

        OptionalOsAppId optionalOsAppId = new OptionalOsAppId();
        if (trafficDescriptor.getOsAppId() != null) {
            android.hardware.radio.V1_6.OsAppId osAppId = new android.hardware.radio.V1_6.OsAppId();
            osAppId.osAppId = primitiveArrayToArrayList(trafficDescriptor.getOsAppId());
            optionalOsAppId.value(osAppId);
        }
        td.osAppId = optionalOsAppId;

        optionalTrafficDescriptor.value(td);
        return optionalTrafficDescriptor;
    }

    private static ArrayList<android.hardware.radio.V1_5.LinkAddress> convertToHalLinkProperties15(
            LinkProperties linkProperties) {
        ArrayList<android.hardware.radio.V1_5.LinkAddress> addresses15 = new ArrayList<>();
        if (linkProperties != null) {
            for (LinkAddress la : linkProperties.getAllLinkAddresses()) {
                android.hardware.radio.V1_5.LinkAddress linkAddress =
                        new android.hardware.radio.V1_5.LinkAddress();
                linkAddress.address = la.getAddress().getHostAddress();
                linkAddress.properties = la.getFlags();
                linkAddress.deprecationTime = la.getDeprecationTime();
                linkAddress.expirationTime = la.getExpirationTime();
                addresses15.add(linkAddress);
            }
        }
        return addresses15;
    }

    /**
     * Convert to DataProfileInfo defined in radio/1.5/types.hal
     * @param dp Data profile
     * @return A converted data profile
     */
    private static android.hardware.radio.V1_5.DataProfileInfo convertToHalDataProfile15(
            DataProfile dp) {
        android.hardware.radio.V1_5.DataProfileInfo dpi =
                new android.hardware.radio.V1_5.DataProfileInfo();

        dpi.apn = dp.getApn();
        dpi.protocol = dp.getProtocolType();
        dpi.roamingProtocol = dp.getRoamingProtocolType();
        dpi.authType = dp.getAuthType();
        dpi.user = dp.getUserName();
        dpi.password = dp.getPassword();
        dpi.type = dp.getType();
        dpi.maxConnsTime = dp.getMaxConnectionsTime();
        dpi.maxConns = dp.getMaxConnections();
        dpi.waitTime = dp.getWaitTime();
        dpi.enabled = dp.isEnabled();
        dpi.supportedApnTypesBitmap = dp.getSupportedApnTypesBitmask();
        // Shift by 1 bit due to the discrepancy between
        // android.hardware.radio.V1_0.RadioAccessFamily and the bitmask version of
        // ServiceState.RIL_RADIO_TECHNOLOGY_XXXX.
        dpi.bearerBitmap = ServiceState.convertNetworkTypeBitmaskToBearerBitmask(
            dp.getBearerBitmask()) << 1;
        dpi.mtuV4 = dp.getMtuV4();
        dpi.mtuV6 = dp.getMtuV6();
        dpi.persistent = dp.isPersistent();
        dpi.preferred = dp.isPreferred();

        // profile id is only meaningful when it's persistent on the modem.
        dpi.profileId = (dpi.persistent) ? dp.getProfileId() : DataProfileId.INVALID;

        return dpi;
    }

    /**
     * Convert NV reset type into ResetNvType defined in types.hal.
     * @param resetType NV reset type.
     * @return Converted reset type in integer or -1 if param is invalid.
     */
    private static int convertToHalResetNvType(int resetType) {
        /**
         * resetType values
         * 1 - reload all NV items
         * 2 - erase NV reset (SCRTN)
         * 3 - factory reset (RTN)
         */
        switch (resetType) {
            case 1: return ResetNvType.RELOAD;
            case 2: return ResetNvType.ERASE;
            case 3: return ResetNvType.FACTORY_RESET;
        }
        return -1;
    }

    @Override
    public void setupDataCall(int accessNetworkType, DataProfile dataProfile, boolean isRoaming,
            boolean allowRoaming, int reason, LinkProperties linkProperties, int pduSessionId,
            NetworkSliceInfo sliceInfo, TrafficDescriptor trafficDescriptor,
            boolean matchAllRuleAllowed, Message result) {
        IRadio radioProxy = getRadioProxy(result);

        if (radioProxy != null) {

            RILRequest rr = obtainRequest(RIL_REQUEST_SETUP_DATA_CALL, result,
                    mRILDefaultWorkSource);

            ArrayList<String> addresses = new ArrayList<>();
            ArrayList<String> dnses = new ArrayList<>();
            if (linkProperties != null) {
                for (InetAddress address : linkProperties.getAddresses()) {
                    addresses.add(address.getHostAddress());
                }
                for (InetAddress dns : linkProperties.getDnsServers()) {
                    dnses.add(dns.getHostAddress());
                }
            }

            try {
                if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                    // IRadio V1.6
                    android.hardware.radio.V1_6.IRadio radioProxy16 =
                            (android.hardware.radio.V1_6.IRadio) radioProxy;

                    // Convert to HAL data profile
                    android.hardware.radio.V1_5.DataProfileInfo dpi =
                            convertToHalDataProfile15(dataProfile);

                    OptionalSliceInfo si = convertToHalSliceInfo(sliceInfo);

                    ArrayList<android.hardware.radio.V1_5.LinkAddress> addresses15 =
                            convertToHalLinkProperties15(linkProperties);

                    OptionalTrafficDescriptor td = convertToHalTrafficDescriptor(trafficDescriptor);

                    if (RILJ_LOGD) {
                        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                                + ",accessNetworkType="
                                + AccessNetworkType.toString(accessNetworkType) + ",isRoaming="
                                + isRoaming + ",allowRoaming=" + allowRoaming + "," + dataProfile
                                + ",addresses=" + addresses15 + ",dnses=" + dnses
                                + ",pduSessionId=" + pduSessionId + ",sliceInfo=" + si
                                + ",trafficDescriptor=" + td + ",matchAllRuleAllowed="
                                + matchAllRuleAllowed);
                    }

                    radioProxy16.setupDataCall_1_6(rr.mSerial, accessNetworkType, dpi, allowRoaming,
                            reason, addresses15, dnses, pduSessionId, si, td, matchAllRuleAllowed);
                } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
                    // IRadio V1.5
                    android.hardware.radio.V1_5.IRadio radioProxy15 =
                            (android.hardware.radio.V1_5.IRadio) radioProxy;

                    // Convert to HAL data profile
                    android.hardware.radio.V1_5.DataProfileInfo dpi =
                            convertToHalDataProfile15(dataProfile);

                    ArrayList<android.hardware.radio.V1_5.LinkAddress> addresses15 =
                            convertToHalLinkProperties15(linkProperties);

                    if (RILJ_LOGD) {
                        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                                + ",accessNetworkType="
                                + AccessNetworkType.toString(accessNetworkType) + ",isRoaming="
                                + isRoaming + ",allowRoaming=" + allowRoaming + "," + dataProfile
                                + ",addresses=" + addresses15 + ",dnses=" + dnses);
                    }

                    radioProxy15.setupDataCall_1_5(rr.mSerial, accessNetworkType, dpi, allowRoaming,
                             reason, addresses15, dnses);
                } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_4)) {
                    // IRadio V1.4
                    android.hardware.radio.V1_4.IRadio radioProxy14 =
                            (android.hardware.radio.V1_4.IRadio) radioProxy;

                    // Convert to HAL data profile
                    android.hardware.radio.V1_4.DataProfileInfo dpi =
                            convertToHalDataProfile14(dataProfile);

                    if (RILJ_LOGD) {
                        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                                + ",accessNetworkType="
                                + AccessNetworkType.toString(accessNetworkType) + ",isRoaming="
                                + isRoaming + ",allowRoaming=" + allowRoaming + "," + dataProfile
                                + ",addresses=" + addresses + ",dnses=" + dnses);
                    }

                    radioProxy14.setupDataCall_1_4(rr.mSerial, accessNetworkType, dpi, allowRoaming,
                            reason, addresses, dnses);
                } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_2)) {
                    // IRadio V1.2 and IRadio V1.3
                    android.hardware.radio.V1_2.IRadio radioProxy12 =
                            (android.hardware.radio.V1_2.IRadio) radioProxy;

                    // Convert to HAL data profile
                    android.hardware.radio.V1_0.DataProfileInfo dpi =
                            convertToHalDataProfile10(dataProfile);

                    if (RILJ_LOGD) {
                        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                                + ",accessNetworkType="
                                + AccessNetworkType.toString(accessNetworkType) + ",isRoaming="
                                + isRoaming + ",allowRoaming=" + allowRoaming + ","
                                + dataProfile + ",addresses=" + addresses + ",dnses=" + dnses);
                    }

                    radioProxy12.setupDataCall_1_2(rr.mSerial, accessNetworkType, dpi,
                            dataProfile.isPersistent(), allowRoaming, isRoaming, reason,
                            addresses, dnses);
                } else {
                    // IRadio V1.0 and IRadio V1.1

                    // Convert to HAL data profile
                    android.hardware.radio.V1_0.DataProfileInfo dpi =
                            convertToHalDataProfile10(dataProfile);

                    // Getting data RAT here is just a workaround to support the older 1.0
                    // vendor RIL. The new data service interface passes access network type
                    // instead of RAT for setup data request. It is impossible to convert access
                    // network type back to RAT here, so we directly get the data RAT from
                    // phone.
                    int dataRat = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
                    Phone phone = PhoneFactory.getPhone(mPhoneId);
                    if (phone != null) {
                        ServiceState ss = phone.getServiceState();
                        if (ss != null) {
                            dataRat = ss.getRilDataRadioTechnology();
                        }
                    }
                    if (RILJ_LOGD) {
                        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                                + ",dataRat=" + dataRat + ",isRoaming=" + isRoaming
                                + ",allowRoaming=" + allowRoaming + "," + dataProfile);
                    }

                    radioProxy.setupDataCall(rr.mSerial, dataRat, dpi,
                            dataProfile.isPersistent(), allowRoaming, isRoaming);
                }
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setupDataCall", e);
            }
        }
    }

    @Override
    public void iccIO(int command, int fileId, String path, int p1, int p2, int p3,
                      String data, String pin2, Message result) {
        iccIOForApp(command, fileId, path, p1, p2, p3, data, pin2, null, result);
    }

    @Override
    public void iccIOForApp(int command, int fileId, String path, int p1, int p2, int p3,
                 String data, String pin2, String aid, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIM_IO, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                if (TelephonyUtils.IS_DEBUGGABLE) {
                    riljLog(rr.serialString() + "> iccIO: "
                            + requestToString(rr.mRequest) + " command = 0x"
                            + Integer.toHexString(command) + " fileId = 0x"
                            + Integer.toHexString(fileId) + " path = " + path + " p1 = "
                            + p1 + " p2 = " + p2 + " p3 = " + " data = " + data
                            + " aid = " + aid);
                } else {
                    riljLog(rr.serialString() + "> iccIO: " + requestToString(rr.mRequest));
                }
            }

            IccIo iccIo = new IccIo();
            iccIo.command = command;
            iccIo.fileId = fileId;
            iccIo.path = convertNullToEmptyString(path);
            iccIo.p1 = p1;
            iccIo.p2 = p2;
            iccIo.p3 = p3;
            iccIo.data = convertNullToEmptyString(data);
            iccIo.pin2 = convertNullToEmptyString(pin2);
            iccIo.aid = convertNullToEmptyString(aid);

            try {
                radioProxy.iccIOForApp(rr.mSerial, iccIo);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "iccIOForApp", e);
            }
        }
    }

    @Override
    public void sendUSSD(String ussd, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SEND_USSD, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                String logUssd = "*******";
                if (RILJ_LOGV) logUssd = ussd;
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " ussd = " + logUssd);
            }

            try {
                radioProxy.sendUssd(rr.mSerial, convertNullToEmptyString(ussd));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "sendUSSD", e);
            }
        }
    }

    @Override
    public void cancelPendingUssd(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CANCEL_USSD, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString()
                        + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.cancelPendingUssd(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "cancelPendingUssd", e);
            }
        }
    }

    @Override
    public void getCLIR(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_CLIR, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getClir(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getCLIR", e);
            }
        }
    }

    @Override
    public void setCLIR(int clirMode, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_CLIR, result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " clirMode = " + clirMode);
            }

            try {
                radioProxy.setClir(rr.mSerial, clirMode);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setCLIR", e);
            }
        }
    }

    @Override
    public void queryCallForwardStatus(int cfReason, int serviceClass,
                           String number, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_CALL_FORWARD_STATUS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " cfreason = " + cfReason + " serviceClass = " + serviceClass);
            }

            android.hardware.radio.V1_0.CallForwardInfo cfInfo =
                    new android.hardware.radio.V1_0.CallForwardInfo();
            cfInfo.reason = cfReason;
            cfInfo.serviceClass = serviceClass;
            cfInfo.toa = PhoneNumberUtils.toaFromString(number);
            cfInfo.number = convertNullToEmptyString(number);
            cfInfo.timeSeconds = 0;

            try {
                radioProxy.getCallForwardStatus(rr.mSerial, cfInfo);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "queryCallForwardStatus", e);
            }
        }
    }

    @Override
    public void setCallForward(int action, int cfReason, int serviceClass,
                   String number, int timeSeconds, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_CALL_FORWARD, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " action = " + action + " cfReason = " + cfReason + " serviceClass = "
                        + serviceClass + " timeSeconds = " + timeSeconds);
            }

            android.hardware.radio.V1_0.CallForwardInfo cfInfo =
                    new android.hardware.radio.V1_0.CallForwardInfo();
            cfInfo.status = action;
            cfInfo.reason = cfReason;
            cfInfo.serviceClass = serviceClass;
            cfInfo.toa = PhoneNumberUtils.toaFromString(number);
            cfInfo.number = convertNullToEmptyString(number);
            cfInfo.timeSeconds = timeSeconds;

            try {
                radioProxy.setCallForward(rr.mSerial, cfInfo);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setCallForward", e);

            }
        }
    }

    @Override
    public void queryCallWaiting(int serviceClass, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_CALL_WAITING, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " serviceClass = " + serviceClass);
            }

            try {
                radioProxy.getCallWaiting(rr.mSerial, serviceClass);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "queryCallWaiting", e);
            }
        }
    }

    @Override
    public void setCallWaiting(boolean enable, int serviceClass, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_CALL_WAITING, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " enable = " + enable + " serviceClass = " + serviceClass);
            }

            try {
                radioProxy.setCallWaiting(rr.mSerial, enable, serviceClass);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setCallWaiting", e);
            }
        }
    }

    @Override
    public void acknowledgeLastIncomingGsmSms(boolean success, int cause, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SMS_ACKNOWLEDGE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " success = " + success + " cause = " + cause);
            }

            try {
                radioProxy.acknowledgeLastIncomingGsmSms(rr.mSerial, success, cause);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "acknowledgeLastIncomingGsmSms", e);
            }
        }
    }

    @Override
    public void acceptCall(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ANSWER, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.acceptCall(rr.mSerial);
                mMetrics.writeRilAnswer(mPhoneId, rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "acceptCall", e);
            }
        }
    }

    @Override
    public void deactivateDataCall(int cid, int reason, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DEACTIVATE_DATA_CALL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> "
                        + requestToString(rr.mRequest) + " cid = " + cid + " reason = " + reason);
            }

            try {
                if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_2)) {
                    android.hardware.radio.V1_2.IRadio radioProxy12 =
                            (android.hardware.radio.V1_2.IRadio) radioProxy;

                    radioProxy12.deactivateDataCall_1_2(rr.mSerial, cid, reason);
                } else {
                    radioProxy.deactivateDataCall(rr.mSerial, cid,
                            (reason == DataService.REQUEST_REASON_SHUTDOWN));
                }
                mMetrics.writeRilDeactivateDataCall(mPhoneId, rr.mSerial, cid, reason);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "deactivateDataCall", e);
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
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_FACILITY_LOCK, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " facility = " + facility + " serviceClass = " + serviceClass
                        + " appId = " + appId);
            }

            try {
                radioProxy.getFacilityLockForApp(rr.mSerial,
                        convertNullToEmptyString(facility),
                        convertNullToEmptyString(password),
                        serviceClass,
                        convertNullToEmptyString(appId));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getFacilityLockForApp", e);
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
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_FACILITY_LOCK, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " facility = " + facility + " lockstate = " + lockState
                        + " serviceClass = " + serviceClass + " appId = " + appId);
            }

            try {
                radioProxy.setFacilityLockForApp(rr.mSerial,
                        convertNullToEmptyString(facility),
                        lockState,
                        convertNullToEmptyString(password),
                        serviceClass,
                        convertNullToEmptyString(appId));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setFacilityLockForApp", e);
            }
        }
    }

    @Override
    public void changeBarringPassword(String facility, String oldPwd, String newPwd,
                                      Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CHANGE_BARRING_PASSWORD, result,
                    mRILDefaultWorkSource);

            // Do not log all function args for privacy
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + "facility = " + facility);
            }

            try {
                radioProxy.setBarringPassword(rr.mSerial,
                        convertNullToEmptyString(facility),
                        convertNullToEmptyString(oldPwd),
                        convertNullToEmptyString(newPwd));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "changeBarringPassword", e);
            }
        }
    }

    @Override
    public void getNetworkSelectionMode(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getNetworkSelectionMode(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getNetworkSelectionMode", e);
            }
        }
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.setNetworkSelectionModeAutomatic(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setNetworkSelectionModeAutomatic", e);
            }
        }
    }

    @Override
    public void setNetworkSelectionModeManual(String operatorNumeric, int ran, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL, result,
                    mRILDefaultWorkSource);
            try {
                int halRan = convertAntToRan(ran);
                if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
                    android.hardware.radio.V1_5.IRadio radioProxy15 =
                            (android.hardware.radio.V1_5.IRadio) radioProxy;
                    if (RILJ_LOGD) {
                        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                                + " operatorNumeric = " + operatorNumeric
                                + ", ran = " + halRan);
                    }
                    radioProxy15.setNetworkSelectionModeManual_1_5(rr.mSerial,
                            convertNullToEmptyString(operatorNumeric), halRan);
                } else {
                    if (RILJ_LOGD) {
                        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                                + " operatorNumeric = " + operatorNumeric);
                    }
                    radioProxy.setNetworkSelectionModeManual(rr.mSerial,
                            convertNullToEmptyString(operatorNumeric));
                }
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setNetworkSelectionModeManual", e);
            }
        }
    }

    @Override
    public void getAvailableNetworks(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_AVAILABLE_NETWORKS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getAvailableNetworks(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getAvailableNetworks", e);
            }
        }
    }

    private android.hardware.radio.V1_1.RadioAccessSpecifier convertRadioAccessSpecifierToRadioHAL(
            RadioAccessSpecifier ras) {
        android.hardware.radio.V1_1.RadioAccessSpecifier rasInHalFormat =
                new android.hardware.radio.V1_1.RadioAccessSpecifier();
        rasInHalFormat.radioAccessNetwork = ras.getRadioAccessNetwork();
        ArrayList<Integer> bands = new ArrayList<>();
        if (ras.getBands() != null) {
            for (int band : ras.getBands()) {
                bands.add(band);
            }
        }
        switch (ras.getRadioAccessNetwork()) {
            case AccessNetworkType.GERAN:
                rasInHalFormat.geranBands = bands;
                break;
            case AccessNetworkType.UTRAN:
                rasInHalFormat.utranBands = bands;
                break;
            case AccessNetworkType.EUTRAN:
                rasInHalFormat.eutranBands = bands;
                break;
            default:
                Log.wtf(RILJ_LOG_TAG, "radioAccessNetwork " + ras.getRadioAccessNetwork()
                        + " not supported on IRadio < 1.5!");
                return null;
        }

        if (ras.getChannels() != null) {
            for (int channel : ras.getChannels()) {
                rasInHalFormat.channels.add(channel);
            }
        }

        return rasInHalFormat;
    }

    private android.hardware.radio.V1_5.RadioAccessSpecifier
            convertRadioAccessSpecifierToRadioHAL_1_5(RadioAccessSpecifier ras) {
        android.hardware.radio.V1_5.RadioAccessSpecifier rasInHalFormat =
                new android.hardware.radio.V1_5.RadioAccessSpecifier();
        android.hardware.radio.V1_5.RadioAccessSpecifier.Bands bandsInHalFormat =
                new android.hardware.radio.V1_5.RadioAccessSpecifier.Bands();
        rasInHalFormat.radioAccessNetwork = convertAntToRan(ras.getRadioAccessNetwork());
        ArrayList<Integer> bands = new ArrayList<>();
        if (ras.getBands() != null) {
            for (int band : ras.getBands()) {
                bands.add(band);
            }
        }
        switch (ras.getRadioAccessNetwork()) {
            case AccessNetworkType.GERAN:
                bandsInHalFormat.geranBands(bands);
                break;
            case AccessNetworkType.UTRAN:
                bandsInHalFormat.utranBands(bands);
                break;
            case AccessNetworkType.EUTRAN:
                bandsInHalFormat.eutranBands(bands);
                break;
            case AccessNetworkType.NGRAN:
                bandsInHalFormat.ngranBands(bands);
                break;
            default:
                Log.wtf(RILJ_LOG_TAG, "radioAccessNetwork " + ras.getRadioAccessNetwork()
                        + " not supported on IRadio 1.5!");
                return null;
        }
        rasInHalFormat.bands = bandsInHalFormat;

        if (ras.getChannels() != null) {
            for (int channel : ras.getChannels()) {
                rasInHalFormat.channels.add(channel);
            }
        }

        return rasInHalFormat;
    }

    /**
     * Radio HAL fallback compatibility feature (b/151106728) assumes that the input parameter
     * networkScanRequest is immutable (read-only) here. Once the caller invokes the method, the
     * parameter networkScanRequest should not be modified. This helps us keep a consistent and
     * simple data model that avoid copying it in the scan result.
     */
    @Override
    public void startNetworkScan(NetworkScanRequest networkScanRequest, Message result) {
        final NetworkScanRequest nsr = networkScanRequest;
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {

            HalVersion overrideHalVersion = getCompatVersion(RIL_REQUEST_START_NETWORK_SCAN);
            if (RILJ_LOGD) {
                riljLog("startNetworkScan: overrideHalVersion=" + overrideHalVersion);
            }
            if ((overrideHalVersion == null
                        || overrideHalVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5))
                    && mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
                android.hardware.radio.V1_5.NetworkScanRequest request =
                        new android.hardware.radio.V1_5.NetworkScanRequest();
                request.type = nsr.getScanType();
                request.interval = nsr.getSearchPeriodicity();
                request.maxSearchTime = nsr.getMaxSearchTime();
                request.incrementalResultsPeriodicity = nsr.getIncrementalResultsPeriodicity();
                request.incrementalResults = nsr.getIncrementalResults();

                for (RadioAccessSpecifier ras : nsr.getSpecifiers()) {
                    android.hardware.radio.V1_5.RadioAccessSpecifier rasInHalFormat =
                            convertRadioAccessSpecifierToRadioHAL_1_5(ras);
                    if (rasInHalFormat == null) {
                        AsyncResult.forMessage(result, null,
                                CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                        result.sendToTarget();
                        return;
                    }
                    request.specifiers.add(rasInHalFormat);
                }

                request.mccMncs.addAll(nsr.getPlmns());
                RILRequest rr = obtainRequest(RIL_REQUEST_START_NETWORK_SCAN, result,
                        mRILDefaultWorkSource, nsr);

                if (RILJ_LOGD) {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
                }

                try {
                    android.hardware.radio.V1_5.IRadio radioProxy15 =
                            (android.hardware.radio.V1_5.IRadio) radioProxy;
                    radioProxy15.startNetworkScan_1_5(rr.mSerial, request);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "startNetworkScan", e);
                }
            } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_2)) {
                android.hardware.radio.V1_2.NetworkScanRequest request =
                        new android.hardware.radio.V1_2.NetworkScanRequest();
                request.type = nsr.getScanType();
                request.interval = nsr.getSearchPeriodicity();
                request.maxSearchTime = nsr.getMaxSearchTime();
                request.incrementalResultsPeriodicity = nsr.getIncrementalResultsPeriodicity();
                request.incrementalResults = nsr.getIncrementalResults();

                for (RadioAccessSpecifier ras : nsr.getSpecifiers()) {

                    android.hardware.radio.V1_1.RadioAccessSpecifier rasInHalFormat =
                            convertRadioAccessSpecifierToRadioHAL(ras);
                    if (rasInHalFormat == null) {
                        AsyncResult.forMessage(result, null,
                                CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                        result.sendToTarget();
                        return;
                    }

                    request.specifiers.add(rasInHalFormat);
                }

                request.mccMncs.addAll(nsr.getPlmns());
                RILRequest rr = obtainRequest(RIL_REQUEST_START_NETWORK_SCAN, result,
                        mRILDefaultWorkSource);

                if (RILJ_LOGD) {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
                }

                try {
                    if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_4)) {
                        android.hardware.radio.V1_4.IRadio radioProxy14 =
                                (android.hardware.radio.V1_4.IRadio) radioProxy;
                        radioProxy14.startNetworkScan_1_4(rr.mSerial, request);
                    } else {
                        android.hardware.radio.V1_2.IRadio radioProxy12 =
                                (android.hardware.radio.V1_2.IRadio) radioProxy;
                        radioProxy12.startNetworkScan_1_2(rr.mSerial, request);
                    }
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "startNetworkScan", e);
                }
            } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_1)) {
                android.hardware.radio.V1_1.IRadio radioProxy11 =
                        (android.hardware.radio.V1_1.IRadio) radioProxy;

                android.hardware.radio.V1_1.NetworkScanRequest request =
                        new android.hardware.radio.V1_1.NetworkScanRequest();
                request.type = nsr.getScanType();
                request.interval = nsr.getSearchPeriodicity();
                for (RadioAccessSpecifier ras : nsr.getSpecifiers()) {
                    android.hardware.radio.V1_1.RadioAccessSpecifier rasInHalFormat =
                            convertRadioAccessSpecifierToRadioHAL(ras);
                    if (rasInHalFormat == null) {
                        AsyncResult.forMessage(result, null,
                                CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                        result.sendToTarget();
                        return;
                    }

                    request.specifiers.add(rasInHalFormat);
                }

                RILRequest rr = obtainRequest(RIL_REQUEST_START_NETWORK_SCAN, result,
                        mRILDefaultWorkSource);

                if (RILJ_LOGD) {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
                }

                try {
                    radioProxy11.startNetworkScan(rr.mSerial, request);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "startNetworkScan", e);
                }
            } else if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void stopNetworkScan(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_1)) {
                android.hardware.radio.V1_1.IRadio radioProxy11 =
                        (android.hardware.radio.V1_1.IRadio) radioProxy;

                RILRequest rr = obtainRequest(RIL_REQUEST_STOP_NETWORK_SCAN, result,
                        mRILDefaultWorkSource);

                if (RILJ_LOGD) {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
                }

                try {
                    radioProxy11.stopNetworkScan(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "stopNetworkScan", e);
                }
            } else if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void startDtmf(char c, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DTMF_START, result,
                    mRILDefaultWorkSource);

            // Do not log function arg for privacy
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.startDtmf(rr.mSerial, c + "");
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "startDtmf", e);
            }
        }
    }

    @Override
    public void stopDtmf(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DTMF_STOP, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.stopDtmf(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "stopDtmf", e);
            }
        }
    }

    @Override
    public void separateConnection(int gsmIndex, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SEPARATE_CONNECTION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " gsmIndex = " + gsmIndex);
            }

            try {
                radioProxy.separateConnection(rr.mSerial, gsmIndex);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "separateConnection", e);
            }
        }
    }

    @Override
    public void getBasebandVersion(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_BASEBAND_VERSION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getBasebandVersion(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getBasebandVersion", e);
            }
        }
    }

    @Override
    public void setMute(boolean enableMute, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_MUTE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " enableMute = " + enableMute);
            }

            try {
                radioProxy.setMute(rr.mSerial, enableMute);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setMute", e);
            }
        }
    }

    @Override
    public void getMute(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_MUTE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getMute(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getMute", e);
            }
        }
    }

    @Override
    public void queryCLIP(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_CLIP, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getClip(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "queryCLIP", e);
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
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DATA_CALL_LIST, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                    android.hardware.radio.V1_6.IRadio radioProxy16 =
                            (android.hardware.radio.V1_6.IRadio) radioProxy;
                    radioProxy16.getDataCallList_1_6(rr.mSerial);
                } else {
                    radioProxy.getDataCallList(rr.mSerial);
                }
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getDataCallList", e);
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
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " enable = "
                        + enable);
            }

            try {
                radioProxy.setSuppServiceNotifications(rr.mSerial, enable);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setSuppServiceNotifications", e);
            }
        }
    }

    @Override
    public void writeSmsToSim(int status, String smsc, String pdu, Message result) {
        status = translateStatus(status);
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_WRITE_SMS_TO_SIM, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGV) {
                riljLog(rr.serialString() + "> "
                        + requestToString(rr.mRequest)
                        + " " + status);
            }

            SmsWriteArgs args = new SmsWriteArgs();
            args.status = status;
            args.smsc = convertNullToEmptyString(smsc);
            args.pdu = convertNullToEmptyString(pdu);

            try {
                radioProxy.writeSmsToSim(rr.mSerial, args);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "writeSmsToSim", e);
            }
        }
    }

    @Override
    public void deleteSmsOnSim(int index, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DELETE_SMS_ON_SIM, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGV) {
                riljLog(rr.serialString() + "> "
                        + requestToString(rr.mRequest) + " index = " + index);
            }

            try {
                radioProxy.deleteSmsOnSim(rr.mSerial, index);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "deleteSmsOnSim", e);
            }
        }
    }

    @Override
    public void setBandMode(int bandMode, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_BAND_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " bandMode = " + bandMode);
            }

            try {
                radioProxy.setBandMode(rr.mSerial, bandMode);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setBandMode", e);
            }
        }
    }

    @Override
    public void queryAvailableBandMode(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getAvailableBandModes(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "queryAvailableBandMode", e);
            }
        }
    }

    @Override
    public void sendEnvelope(String contents, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " contents = "
                        + contents);
            }

            try {
                radioProxy.sendEnvelope(rr.mSerial, convertNullToEmptyString(contents));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "sendEnvelope", e);
            }
        }
    }

    @Override
    public void sendTerminalResponse(String contents, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " contents = "
                        + (TelephonyUtils.IS_DEBUGGABLE
                            ? contents : censoredTerminalResponse(contents)));
            }

            try {
                radioProxy.sendTerminalResponseToSim(rr.mSerial,
                        convertNullToEmptyString(contents));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "sendTerminalResponse", e);
            }
        }
    }

    private String censoredTerminalResponse(String terminalResponse) {
        try {
            byte[] bytes = IccUtils.hexStringToBytes(terminalResponse);
            if (bytes != null) {
                List<ComprehensionTlv> ctlvs = ComprehensionTlv.decodeMany(bytes, 0);
                int from = 0;
                for (ComprehensionTlv ctlv : ctlvs) {
                    // Find text strings which might be personal information input by user,
                    // then replace it with "********".
                    if (ComprehensionTlvTag.TEXT_STRING.value() == ctlv.getTag()) {
                        byte[] target = Arrays.copyOfRange(ctlv.getRawValue(), from,
                                ctlv.getValueIndex() + ctlv.getLength());
                        terminalResponse = terminalResponse.toLowerCase().replace(
                                IccUtils.bytesToHexString(target).toLowerCase(), "********");
                    }
                    // The text string tag and the length field should also be hidden.
                    from = ctlv.getValueIndex() + ctlv.getLength();
                }
            }
        } catch (Exception e) {
            Rlog.e(RILJ_LOG_TAG, "Could not censor the terminal response: " + e);
            terminalResponse = null;
        }

        return terminalResponse;
    }

    @Override
    public void sendEnvelopeWithStatus(String contents, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " contents = "
                        + contents);
            }

            try {
                radioProxy.sendEnvelopeWithStatus(rr.mSerial, convertNullToEmptyString(contents));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "sendEnvelopeWithStatus", e);
            }
        }
    }

    @Override
    public void explicitCallTransfer(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_EXPLICIT_CALL_TRANSFER, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.explicitCallTransfer(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "explicitCallTransfer", e);
            }
        }
    }

    @Override
    public void setPreferredNetworkType(@PrefNetworkMode int networkType , Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " networkType = " + networkType);
            }
            mAllowedNetworkTypesBitmask = RadioAccessFamily.getRafFromNetworkType(networkType);
            mMetrics.writeSetPreferredNetworkType(mPhoneId, networkType);

            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_4)) {
                android.hardware.radio.V1_4.IRadio radioProxy14 =
                        (android.hardware.radio.V1_4.IRadio) radioProxy;
                try {
                    radioProxy14.setPreferredNetworkTypeBitmap(
                            rr.mSerial, convertToHalRadioAccessFamily(mAllowedNetworkTypesBitmask));
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setPreferredNetworkTypeBitmap", e);
                }
            } else {
                try {
                    radioProxy.setPreferredNetworkType(rr.mSerial, networkType);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setPreferredNetworkType", e);
                }
            }
        }
    }

    /**
     * convert RAF from {@link android.hardware.radio.V1_0.RadioAccessFamily} to
     * {@link TelephonyManager.NetworkTypeBitMask}, the bitmask represented by
     * {@link android.telephony.Annotation.NetworkType}.
     *
     * @param raf {@link android.hardware.radio.V1_0.RadioAccessFamily}
     * @return {@link TelephonyManager.NetworkTypeBitMask}
     */
    @TelephonyManager.NetworkTypeBitMask
    public static int convertToNetworkTypeBitMask(int raf) {
        int networkTypeRaf = 0;

        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.GSM) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_GSM;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.GPRS) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_GPRS;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.EDGE) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_EDGE;
        }
        // convert both IS95A/IS95B to CDMA as network mode doesn't support CDMA
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.IS95A) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_CDMA;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.IS95B) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_CDMA;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.ONE_X_RTT) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_1xRTT;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.EVDO_0) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_0;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.EVDO_A) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_A;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.EVDO_B) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_B;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.EHRPD) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_EHRPD;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.HSUPA) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_HSUPA;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.HSDPA) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_HSDPA;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.HSPA) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_HSPA;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.HSPAP) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_HSPAP;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.UMTS) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_UMTS;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.TD_SCDMA) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_TD_SCDMA;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.LTE) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_LTE;
        }
        if ((raf & android.hardware.radio.V1_0.RadioAccessFamily.LTE_CA) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_LTE_CA;
        }
        if ((raf & android.hardware.radio.V1_4.RadioAccessFamily.NR) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_NR;
        }
        // TODO: need hal definition
        if ((raf & (1 << ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN)) != 0) {
            networkTypeRaf |= TelephonyManager.NETWORK_TYPE_BITMASK_IWLAN;
        }
        return (networkTypeRaf == 0) ? TelephonyManager.NETWORK_TYPE_UNKNOWN : networkTypeRaf;
    }

    // convert to android.hardware.radio.V1_0.RadioAccessFamily
    private static int convertToHalRadioAccessFamily(
            @TelephonyManager.NetworkTypeBitMask int networkTypeBitmask) {
        int raf = 0;

        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_GSM) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.GSM;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_GPRS) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.GPRS;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EDGE) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.EDGE;
        }
        // convert CDMA to IS95A, consistent with ServiceState.networkTypeToRilRadioTechnology
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_CDMA) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.IS95A;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_1xRTT) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.ONE_X_RTT;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_0) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.EVDO_0;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_A) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.EVDO_A;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EVDO_B) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.EVDO_B;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_EHRPD) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.EHRPD;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_HSUPA) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.HSUPA;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_HSDPA) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.HSDPA;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_HSPA) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.HSPA;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_HSPAP) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.HSPAP;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_UMTS) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.UMTS;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_TD_SCDMA) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.TD_SCDMA;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_LTE) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.LTE;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_LTE_CA) != 0) {
            raf |= android.hardware.radio.V1_0.RadioAccessFamily.LTE_CA;
        }
        if ((networkTypeBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_NR) != 0) {
            raf |= android.hardware.radio.V1_4.RadioAccessFamily.NR;
        }
        // TODO: need hal definition for IWLAN
        return (raf == 0) ? android.hardware.radio.V1_4.RadioAccessFamily.UNKNOWN : raf;
    }

    @Override
    public void getPreferredNetworkType(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_4)) {
                android.hardware.radio.V1_4.IRadio radioProxy14 =
                        (android.hardware.radio.V1_4.IRadio) radioProxy;
                try {
                    radioProxy14.getPreferredNetworkTypeBitmap(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "getPreferredNetworkTypeBitmap", e);
                }
            } else {
                try {
                    radioProxy.getPreferredNetworkType(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "getPreferredNetworkType", e);
                }
            }
        }
    }

    @Override
    public void setAllowedNetworkTypesBitmap(
            @TelephonyManager.NetworkTypeBitMask int networkTypeBitmask, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            if (mRadioVersion.less(RADIO_HAL_VERSION_1_6)) {
                // For older HAL, redirects the call to setPreferredNetworkType.
                setPreferredNetworkType(
                        RadioAccessFamily.getNetworkTypeFromRaf(networkTypeBitmask), result);
                return;
            }

            android.hardware.radio.V1_6.IRadio radioProxy16 =
                    (android.hardware.radio.V1_6.IRadio) radioProxy;
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_ALLOWED_NETWORK_TYPES_BITMAP, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }
            mAllowedNetworkTypesBitmask = networkTypeBitmask;
            try {
                radioProxy16.setAllowedNetworkTypesBitmap(rr.mSerial,
                        convertToHalRadioAccessFamily(mAllowedNetworkTypesBitmask));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setAllowedNetworkTypeBitmask", e);
            }
        }
    }

    @Override
    public void getAllowedNetworkTypesBitmap(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            if (mRadioVersion.less(RADIO_HAL_VERSION_1_6)) {
                // For older HAL, redirects the call to getPreferredNetworkType.
                getPreferredNetworkType(result);
                return;
            }

            android.hardware.radio.V1_6.IRadio radioProxy16 =
                    (android.hardware.radio.V1_6.IRadio) radioProxy;
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_ALLOWED_NETWORK_TYPES_BITMAP, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy16.getAllowedNetworkTypesBitmap(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getAllowedNetworkTypeBitmask", e);
            }
        }
    }

    @Override
    public void setLocationUpdates(boolean enable, WorkSource workSource, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_LOCATION_UPDATES, result,
                    workSource == null ? mRILDefaultWorkSource : workSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> "
                        + requestToString(rr.mRequest) + " enable = " + enable);
            }

            try {
                radioProxy.setLocationUpdates(rr.mSerial, enable);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setLocationUpdates", e);
            }
        }
    }

    /**
     * Is E-UTRA-NR Dual Connectivity enabled
     */
    @Override
    public void isNrDualConnectivityEnabled(Message result, WorkSource workSource) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            if (mRadioVersion.less(RADIO_HAL_VERSION_1_6)) {
                if (result != null) {
                    AsyncResult.forMessage(result, null,
                            CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                    result.sendToTarget();
                }
                return;
            }

            android.hardware.radio.V1_6.IRadio radioProxy16 =
                    (android.hardware.radio.V1_6.IRadio) radioProxy;

            RILRequest rr = obtainRequest(RIL_REQUEST_IS_NR_DUAL_CONNECTIVITY_ENABLED, result,
                    workSource == null ? mRILDefaultWorkSource : workSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> "
                        + requestToString(rr.mRequest));
            }

            try {
                radioProxy16.isNrDualConnectivityEnabled(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "isNRDualConnectivityEnabled", e);
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
    public void setNrDualConnectivityState(int nrDualConnectivityState,
            Message result, WorkSource workSource) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            if (mRadioVersion.less(RADIO_HAL_VERSION_1_6)) {
                if (result != null) {
                    AsyncResult.forMessage(result, null,
                            CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                    result.sendToTarget();
                }
                return;
            }

            android.hardware.radio.V1_6.IRadio radioProxy16 =
                    (android.hardware.radio.V1_6.IRadio) radioProxy;
            RILRequest rr = obtainRequest(RIL_REQUEST_ENABLE_NR_DUAL_CONNECTIVITY, result,
                    workSource == null ? mRILDefaultWorkSource : workSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> "
                        + requestToString(rr.mRequest) + " enable = " + nrDualConnectivityState);
            }

            try {
                radioProxy16.setNrDualConnectivityState(rr.mSerial,
                        (byte) nrDualConnectivityState);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "enableNRDualConnectivity", e);
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
        boolean isEnabled = isVoNrEnabled();
        if (result != null) {
            AsyncResult.forMessage(result, isEnabled, null);
            result.sendToTarget();
        }
    }

    /**
     * Enable or disable Voice over NR (VoNR)
     * @param enabled enable or disable VoNR.
     */
    @Override
    public void setVoNrEnabled(boolean enabled, Message result, WorkSource workSource) {
        setVoNrEnabled(enabled);
        /* calling a query api to let HAL know that VoNREnabled state is updated.
           This is a temporary work around as new HIDL API is not allowed.
           HAL can check the value of PROPERTY_IS_VONR_ENABLED property to determine
           if there is any change whenever it receives isNrDualConnectivityEnabled request.
           This behavior will be removed in Android T.
         */
        isNrDualConnectivityEnabled(null, workSource);
        if (result != null) {
            AsyncResult.forMessage(result, null, null);
            result.sendToTarget();
        }
    }

    @Override
    public void setCdmaSubscriptionSource(int cdmaSubscription , Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " cdmaSubscription = " + cdmaSubscription);
            }

            try {
                radioProxy.setCdmaSubscriptionSource(rr.mSerial, cdmaSubscription);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setCdmaSubscriptionSource", e);
            }
        }
    }

    @Override
    public void queryCdmaRoamingPreference(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getCdmaRoamingPreference(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "queryCdmaRoamingPreference", e);
            }
        }
    }

    @Override
    public void setCdmaRoamingPreference(int cdmaRoamingType, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " cdmaRoamingType = " + cdmaRoamingType);
            }

            try {
                radioProxy.setCdmaRoamingPreference(rr.mSerial, cdmaRoamingType);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setCdmaRoamingPreference", e);
            }
        }
    }

    @Override
    public void queryTTYMode(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_QUERY_TTY_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getTTYMode(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "queryTTYMode", e);
            }
        }
    }

    @Override
    public void setTTYMode(int ttyMode, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_TTY_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " ttyMode = " + ttyMode);
            }

            try {
                radioProxy.setTTYMode(rr.mSerial, ttyMode);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setTTYMode", e);
            }
        }
    }

    @Override
    public void setPreferredVoicePrivacy(boolean enable, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " enable = " + enable);
            }

            try {
                radioProxy.setPreferredVoicePrivacy(rr.mSerial, enable);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setPreferredVoicePrivacy", e);
            }
        }
    }

    @Override
    public void getPreferredVoicePrivacy(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getPreferredVoicePrivacy(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getPreferredVoicePrivacy", e);
            }
        }
    }

    @Override
    public void sendCDMAFeatureCode(String featureCode, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_FLASH, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " featureCode = " + featureCode);
            }

            try {
                radioProxy.sendCDMAFeatureCode(rr.mSerial, convertNullToEmptyString(featureCode));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "sendCDMAFeatureCode", e);
            }
        }
    }

    @Override
    public void sendBurstDtmf(String dtmfString, int on, int off, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_BURST_DTMF, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " dtmfString = " + dtmfString + " on = " + on + " off = " + off);
            }

            try {
                radioProxy.sendBurstDtmf(rr.mSerial, convertNullToEmptyString(dtmfString), on, off);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "sendBurstDtmf", e);
            }
        }
    }

    private void constructCdmaSendSmsRilRequest(CdmaSmsMessage msg, byte[] pdu) {
        int addrNbrOfDigits;
        int subaddrNbrOfDigits;
        int bearerDataLength;
        ByteArrayInputStream bais = new ByteArrayInputStream(pdu);
        DataInputStream dis = new DataInputStream(bais);

        try {
            msg.teleserviceId = dis.readInt(); // teleServiceId
            msg.isServicePresent = (byte) dis.readInt() == 1 ? true : false; // servicePresent
            msg.serviceCategory = dis.readInt(); // serviceCategory
            msg.address.digitMode = dis.read();  // address digit mode
            msg.address.numberMode = dis.read(); // address number mode
            msg.address.numberType = dis.read(); // address number type
            msg.address.numberPlan = dis.read(); // address number plan
            addrNbrOfDigits = (byte) dis.read();
            for (int i = 0; i < addrNbrOfDigits; i++) {
                msg.address.digits.add(dis.readByte()); // address_orig_bytes[i]
            }
            msg.subAddress.subaddressType = dis.read(); //subaddressType
            msg.subAddress.odd = (byte) dis.read() == 1 ? true : false; //subaddr odd
            subaddrNbrOfDigits = (byte) dis.read();
            for (int i = 0; i < subaddrNbrOfDigits; i++) {
                msg.subAddress.digits.add(dis.readByte()); //subaddr_orig_bytes[i]
            }

            bearerDataLength = dis.read();
            for (int i = 0; i < bearerDataLength; i++) {
                msg.bearerData.add(dis.readByte()); //bearerData[i]
            }
        } catch (IOException ex) {
            if (RILJ_LOGD) {
                riljLog("sendSmsCdma: conversion from input stream to object failed: "
                        + ex);
            }
        }
    }

    @Override
    public void sendCdmaSMSExpectMore(byte[] pdu, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SEND_SMS_EXPECT_MORE, result,
                    mRILDefaultWorkSource);

            // Do not log function arg for privacy
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            CdmaSmsMessage msg = new CdmaSmsMessage();
            constructCdmaSendSmsRilRequest(msg, pdu);
            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                android.hardware.radio.V1_6.IRadio radioProxy16 =
                        (android.hardware.radio.V1_6.IRadio) radioProxy;
                try {
                    radioProxy16.sendCdmaSmsExpectMore_1_6(rr.mSerial, msg);
                    mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_CDMA,
                            SmsSession.Event.Format.SMS_FORMAT_3GPP2,
                            getOutgoingSmsMessageId(result));
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "sendCdmaSMSExpectMore", e);
                }
            } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
                android.hardware.radio.V1_5.IRadio radioProxy15 =
                        (android.hardware.radio.V1_5.IRadio) radioProxy;
                try {
                    radioProxy15.sendCdmaSmsExpectMore(rr.mSerial, msg);
                    mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_CDMA,
                            SmsSession.Event.Format.SMS_FORMAT_3GPP2,
                            getOutgoingSmsMessageId(result));
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "sendCdmaSMSExpectMore", e);
                }
            } else {
                sendCdmaSms(pdu, result);
            }
        }
    }

    @Override
    public void sendCdmaSms(byte[] pdu, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SEND_SMS, result,
                    mRILDefaultWorkSource);

            // Do not log function arg for privacy
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            CdmaSmsMessage msg = new CdmaSmsMessage();
            constructCdmaSendSmsRilRequest(msg, pdu);
            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                try {
                    android.hardware.radio.V1_6.IRadio radioProxy16 =
                            (android.hardware.radio.V1_6.IRadio) radioProxy;
                    radioProxy16.sendCdmaSms_1_6(rr.mSerial, msg);
                    mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_CDMA,
                            SmsSession.Event.Format.SMS_FORMAT_3GPP2,
                            getOutgoingSmsMessageId(result));
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "sendCdmaSms", e);
                }
            } else {
                try {
                    radioProxy.sendCdmaSms(rr.mSerial, msg);
                    mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_CDMA,
                            SmsSession.Event.Format.SMS_FORMAT_3GPP2,
                            getOutgoingSmsMessageId(result));
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "sendCdmaSms", e);
                }
            }
        }
    }

    @Override
    public void acknowledgeLastIncomingCdmaSms(boolean success, int cause, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " success = " + success + " cause = " + cause);
            }

            CdmaSmsAck msg = new CdmaSmsAck();
            msg.errorClass = success ? 0 : 1;
            msg.smsCauseCode = cause;

            try {
                radioProxy.acknowledgeLastIncomingCdmaSms(rr.mSerial, msg);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "acknowledgeLastIncomingCdmaSms", e);
            }
        }
    }

    @Override
    public void getGsmBroadcastConfig(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GSM_GET_BROADCAST_CONFIG, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getGsmBroadcastConfig(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getGsmBroadcastConfig", e);
            }
        }
    }

    @Override
    public void setGsmBroadcastConfig(SmsBroadcastConfigInfo[] config, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GSM_SET_BROADCAST_CONFIG, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " with " + config.length + " configs : ");
                for (int i = 0; i < config.length; i++) {
                    riljLog(config[i].toString());
                }
            }

            ArrayList<GsmBroadcastSmsConfigInfo> configs = new ArrayList<>();

            int numOfConfig = config.length;
            GsmBroadcastSmsConfigInfo info;

            for (int i = 0; i < numOfConfig; i++) {
                info = new GsmBroadcastSmsConfigInfo();
                info.fromServiceId = config[i].getFromServiceId();
                info.toServiceId = config[i].getToServiceId();
                info.fromCodeScheme = config[i].getFromCodeScheme();
                info.toCodeScheme = config[i].getToCodeScheme();
                info.selected = config[i].isSelected();
                configs.add(info);
            }

            try {
                radioProxy.setGsmBroadcastConfig(rr.mSerial, configs);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setGsmBroadcastConfig", e);
            }
        }
    }

    @Override
    public void setGsmBroadcastActivation(boolean activate, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GSM_BROADCAST_ACTIVATION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " activate = " + activate);
            }

            try {
                radioProxy.setGsmBroadcastActivation(rr.mSerial, activate);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setGsmBroadcastActivation", e);
            }
        }
    }

    @Override
    public void getCdmaBroadcastConfig(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getCdmaBroadcastConfig(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getCdmaBroadcastConfig", e);
            }
        }
    }

    @Override
    public void setCdmaBroadcastConfig(CdmaSmsBroadcastConfigInfo[] configs, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG, result,
                    mRILDefaultWorkSource);

            ArrayList<CdmaBroadcastSmsConfigInfo> halConfigs = new ArrayList<>();

            for (CdmaSmsBroadcastConfigInfo config: configs) {
                for (int i = config.getFromServiceCategory();
                        i <= config.getToServiceCategory();
                        i++) {
                    CdmaBroadcastSmsConfigInfo info = new CdmaBroadcastSmsConfigInfo();
                    info.serviceCategory = i;
                    info.language = config.getLanguage();
                    info.selected = config.isSelected();
                    halConfigs.add(info);
                }
            }

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " with " + halConfigs.size() + " configs : ");
                for (CdmaBroadcastSmsConfigInfo config : halConfigs) {
                    riljLog(config.toString());
                }
            }

            try {
                radioProxy.setCdmaBroadcastConfig(rr.mSerial, halConfigs);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setCdmaBroadcastConfig", e);
            }
        }
    }

    @Override
    public void setCdmaBroadcastActivation(boolean activate, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_BROADCAST_ACTIVATION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " activate = " + activate);
            }

            try {
                radioProxy.setCdmaBroadcastActivation(rr.mSerial, activate);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setCdmaBroadcastActivation", e);
            }
        }
    }

    @Override
    public void getCDMASubscription(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_SUBSCRIPTION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getCDMASubscription(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getCDMASubscription", e);
            }
        }
    }

    @Override
    public void writeSmsToRuim(int status, byte[] pdu, Message result) {
        status = translateStatus(status);
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGV) {
                riljLog(rr.serialString() + "> "
                        + requestToString(rr.mRequest)
                        + " status = " + status);
            }

            CdmaSmsWriteArgs args = new CdmaSmsWriteArgs();
            args.status = status;
            constructCdmaSendSmsRilRequest(args.message, pdu);

            try {
                radioProxy.writeSmsToRuim(rr.mSerial, args);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "writeSmsToRuim", e);
            }
        }
    }

    @Override
    public void deleteSmsOnRuim(int index, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGV) {
                riljLog(rr.serialString() + "> "
                        + requestToString(rr.mRequest)
                        + " index = " + index);
            }

            try {
                radioProxy.deleteSmsOnRuim(rr.mSerial, index);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "deleteSmsOnRuim", e);
            }
        }
    }

    @Override
    public void getDeviceIdentity(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_DEVICE_IDENTITY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getDeviceIdentity(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getDeviceIdentity", e);
            }
        }
    }

    @Override
    public void exitEmergencyCallbackMode(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.exitEmergencyCallbackMode(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "exitEmergencyCallbackMode", e);
            }
        }
    }

    @Override
    public void getSmscAddress(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_SMSC_ADDRESS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getSmscAddress(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getSmscAddress", e);
            }
        }
    }

    @Override
    public void setSmscAddress(String address, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_SMSC_ADDRESS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " address = " + address);
            }

            try {
                radioProxy.setSmscAddress(rr.mSerial, convertNullToEmptyString(address));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setSmscAddress", e);
            }
        }
    }

    @Override
    public void reportSmsMemoryStatus(boolean available, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_REPORT_SMS_MEMORY_STATUS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> "
                        + requestToString(rr.mRequest) + " available = " + available);
            }

            try {
                radioProxy.reportSmsMemoryStatus(rr.mSerial, available);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "reportSmsMemoryStatus", e);
            }
        }
    }

    @Override
    public void reportStkServiceIsRunning(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.reportStkServiceIsRunning(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "reportStkServiceIsRunning", e);
            }
        }
    }

    @Override
    public void getCdmaSubscriptionSource(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getCdmaSubscriptionSource(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getCdmaSubscriptionSource", e);
            }
        }
    }

    @Override
    public void acknowledgeIncomingGsmSmsWithPdu(boolean success, String ackPdu, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " success = " + success);
            }

            try {
                radioProxy.acknowledgeIncomingGsmSmsWithPdu(rr.mSerial, success,
                        convertNullToEmptyString(ackPdu));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "acknowledgeIncomingGsmSmsWithPdu", e);
            }
        }
    }

    @Override
    public void getVoiceRadioTechnology(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_VOICE_RADIO_TECH, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getVoiceRadioTechnology(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getVoiceRadioTechnology", e);
            }
        }
    }

    @Override
    public void getCellInfoList(Message result, WorkSource workSource) {
        workSource = getDefaultWorkSourceIfInvalid(workSource);
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_CELL_INFO_LIST, result,
                    workSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                    android.hardware.radio.V1_6.IRadio radioProxy16 =
                            (android.hardware.radio.V1_6.IRadio) radioProxy;
                    radioProxy16.getCellInfoList_1_6(rr.mSerial);

                } else {
                    radioProxy.getCellInfoList(rr.mSerial);
                }
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getCellInfoList", e);
            }
        }
    }

    @Override
    public void setCellInfoListRate(int rateInMillis, Message result, WorkSource workSource) {
        workSource = getDefaultWorkSourceIfInvalid(workSource);
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_UNSOL_CELL_INFO_LIST_RATE, result,
                    workSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " rateInMillis = " + rateInMillis);
            }

            try {
                radioProxy.setCellInfoListRate(rr.mSerial, rateInMillis);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setCellInfoListRate", e);
            }
        }
    }

    @Override
    public void setInitialAttachApn(DataProfile dataProfile, boolean isRoaming, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_INITIAL_ATTACH_APN, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + dataProfile);
            }

            try {
                if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
                    // v1.5
                    android.hardware.radio.V1_5.IRadio radioProxy15 =
                            (android.hardware.radio.V1_5.IRadio) radioProxy;
                    radioProxy15.setInitialAttachApn_1_5(rr.mSerial,
                            convertToHalDataProfile15(dataProfile));
                } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_4)) {
                    // v1.4
                    android.hardware.radio.V1_4.IRadio radioProxy14 =
                            (android.hardware.radio.V1_4.IRadio) radioProxy;
                    radioProxy14.setInitialAttachApn_1_4(rr.mSerial,
                            convertToHalDataProfile14(dataProfile));
                } else {
                    // v1.3, v1.2, v1.1, and v1.0
                    radioProxy.setInitialAttachApn(rr.mSerial,
                            convertToHalDataProfile10(dataProfile), dataProfile.isPersistent(),
                            isRoaming);
                }
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setInitialAttachApn", e);
            }
        }
    }

    @Override
    public void getImsRegistrationState(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_IMS_REGISTRATION_STATE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.getImsRegistrationState(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getImsRegistrationState", e);
            }
        }
    }

    @Override
    public void sendImsGsmSms(String smscPdu, String pdu, int retry, int messageRef,
                   Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_IMS_SEND_SMS, result,
                    mRILDefaultWorkSource);

            // Do not log function args for privacy
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            ImsSmsMessage msg = new ImsSmsMessage();
            msg.tech = RadioTechnologyFamily.THREE_GPP;
            msg.retry = (byte) retry >= 1 ? true : false;
            msg.messageRef = messageRef;

            GsmSmsMessage gsmMsg = constructGsmSendSmsRilRequest(smscPdu, pdu);
            msg.gsmMessage.add(gsmMsg);
            try {
                radioProxy.sendImsSms(rr.mSerial, msg);
                mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_IMS,
                        SmsSession.Event.Format.SMS_FORMAT_3GPP, getOutgoingSmsMessageId(result));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "sendImsGsmSms", e);
            }
        }
    }

    @Override
    public void sendImsCdmaSms(byte[] pdu, int retry, int messageRef, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_IMS_SEND_SMS, result,
                    mRILDefaultWorkSource);

            // Do not log function args for privacy
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            ImsSmsMessage msg = new ImsSmsMessage();
            msg.tech = RadioTechnologyFamily.THREE_GPP2;
            msg.retry = (byte) retry >= 1 ? true : false;
            msg.messageRef = messageRef;

            CdmaSmsMessage cdmaMsg = new CdmaSmsMessage();
            constructCdmaSendSmsRilRequest(cdmaMsg, pdu);
            msg.cdmaMessage.add(cdmaMsg);

            try {
                radioProxy.sendImsSms(rr.mSerial, msg);
                mMetrics.writeRilSendSms(mPhoneId, rr.mSerial, SmsSession.Event.Tech.SMS_IMS,
                        SmsSession.Event.Format.SMS_FORMAT_3GPP2, getOutgoingSmsMessageId(result));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "sendImsCdmaSms", e);
            }
        }
    }

    private SimApdu createSimApdu(int channel, int cla, int instruction, int p1, int p2, int p3,
                                  String data) {
        SimApdu msg = new SimApdu();
        msg.sessionId = channel;
        msg.cla = cla;
        msg.instruction = instruction;
        msg.p1 = p1;
        msg.p2 = p2;
        msg.p3 = p3;
        msg.data = convertNullToEmptyString(data);
        return msg;
    }

    @Override
    public void iccTransmitApduBasicChannel(int cla, int instruction, int p1, int p2,
                                            int p3, String data, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIM_TRANSMIT_APDU_BASIC, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                if (TelephonyUtils.IS_DEBUGGABLE) {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                            + String.format(" cla = 0x%02X ins = 0x%02X", cla, instruction)
                            + String.format(" p1 = 0x%02X p2 = 0x%02X p3 = 0x%02X", p1, p2, p3)
                            + " data = " + data);
                } else {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
                }
            }

            SimApdu msg = createSimApdu(0, cla, instruction, p1, p2, p3, data);
            try {
                radioProxy.iccTransmitApduBasicChannel(rr.mSerial, msg);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "iccTransmitApduBasicChannel", e);
            }
        }
    }

    @Override
    public void iccOpenLogicalChannel(String aid, int p2, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIM_OPEN_CHANNEL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                if (TelephonyUtils.IS_DEBUGGABLE) {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " aid = " + aid
                            + " p2 = " + p2);
                } else {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
                }
            }

            try {
                radioProxy.iccOpenLogicalChannel(rr.mSerial, convertNullToEmptyString(aid), p2);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "iccOpenLogicalChannel", e);
            }
        }
    }

    @Override
    public void iccCloseLogicalChannel(int channel, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIM_CLOSE_CHANNEL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " channel = "
                        + channel);
            }

            try {
                radioProxy.iccCloseLogicalChannel(rr.mSerial, channel);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "iccCloseLogicalChannel", e);
            }
        }
    }

    @Override
    public void iccTransmitApduLogicalChannel(int channel, int cla, int instruction,
                                              int p1, int p2, int p3, String data,
                                              Message result) {
        if (channel <= 0) {
            throw new RuntimeException(
                    "Invalid channel in iccTransmitApduLogicalChannel: " + channel);
        }

        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                if (TelephonyUtils.IS_DEBUGGABLE) {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                            + String.format(" channel = %d", channel)
                            + String.format(" cla = 0x%02X ins = 0x%02X", cla, instruction)
                            + String.format(" p1 = 0x%02X p2 = 0x%02X p3 = 0x%02X", p1, p2, p3)
                            + " data = " + data);
                } else {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
                }
            }

            SimApdu msg = createSimApdu(channel, cla, instruction, p1, p2, p3, data);

            try {
                radioProxy.iccTransmitApduLogicalChannel(rr.mSerial, msg);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "iccTransmitApduLogicalChannel", e);
            }
        }
    }

    @Override
    public void nvReadItem(int itemID, Message result, WorkSource workSource) {
        workSource = getDefaultWorkSourceIfInvalid(workSource);
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_NV_READ_ITEM, result,
                    workSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " itemId = " + itemID);
            }

            try {
                radioProxy.nvReadItem(rr.mSerial, itemID);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "nvReadItem", e);
            }
        }
    }

    @Override
    public void nvWriteItem(int itemId, String itemValue, Message result, WorkSource workSource) {
        workSource = getDefaultWorkSourceIfInvalid(workSource);
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_NV_WRITE_ITEM, result,
                    workSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " itemId = " + itemId + " itemValue = " + itemValue);
            }

            NvWriteItem item = new NvWriteItem();
            item.itemId = itemId;
            item.value = convertNullToEmptyString(itemValue);

            try {
                radioProxy.nvWriteItem(rr.mSerial, item);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "nvWriteItem", e);
            }
        }
    }

    @Override
    public void nvWriteCdmaPrl(byte[] preferredRoamingList, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_NV_WRITE_CDMA_PRL, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " PreferredRoamingList = 0x"
                        + IccUtils.bytesToHexString(preferredRoamingList));
            }

            ArrayList<Byte> arrList = new ArrayList<>();
            for (int i = 0; i < preferredRoamingList.length; i++) {
                arrList.add(preferredRoamingList[i]);
            }

            try {
                radioProxy.nvWriteCdmaPrl(rr.mSerial, arrList);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "nvWriteCdmaPrl", e);
            }
        }
    }

    @Override
    public void nvResetConfig(int resetType, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_NV_RESET_CONFIG, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " resetType = " + resetType);
            }

            try {
                radioProxy.nvResetConfig(rr.mSerial, convertToHalResetNvType(resetType));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "nvResetConfig", e);
            }
        }
    }

    @Override
    public void setUiccSubscription(int slotId, int appIndex, int subId,
                                    int subStatus, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_UICC_SUBSCRIPTION, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " slot = " + slotId + " appIndex = " + appIndex
                        + " subId = " + subId + " subStatus = " + subStatus);
            }

            SelectUiccSub info = new SelectUiccSub();
            info.slot = slotId;
            info.appIndex = appIndex;
            info.subType = subId;
            info.actStatus = subStatus;

            try {
                radioProxy.setUiccSubscription(rr.mSerial, info);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setUiccSubscription", e);
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
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ALLOW_DATA, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " allowed = " + allowed);
            }

            try {
                radioProxy.setDataAllowed(rr.mSerial, allowed);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setDataAllowed", e);
            }
        }
    }

    @Override
    public void getHardwareConfig(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_HARDWARE_CONFIG, result,
                    mRILDefaultWorkSource);

            // Do not log function args for privacy
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.getHardwareConfig(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getHardwareConfig", e);
            }
        }
    }

    @Override
    public void requestIccSimAuthentication(int authContext, String data, String aid,
                                            Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SIM_AUTHENTICATION, result,
                    mRILDefaultWorkSource);

            // Do not log function args for privacy
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy.requestIccSimAuthentication(rr.mSerial,
                        authContext,
                        convertNullToEmptyString(data),
                        convertNullToEmptyString(aid));
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "requestIccSimAuthentication", e);
            }
        }
    }

    @Override
    public void setDataProfile(DataProfile[] dps, boolean isRoaming, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_DATA_PROFILE, result,
                    mRILDefaultWorkSource);
            try {
                if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
                    // V1.5
                    android.hardware.radio.V1_5.IRadio radioProxy15 =
                            (android.hardware.radio.V1_5.IRadio) radioProxy;

                    ArrayList<android.hardware.radio.V1_5.DataProfileInfo> dpis = new ArrayList<>();
                    for (DataProfile dp : dps) {
                        dpis.add(convertToHalDataProfile15(dp));
                    }

                    if (RILJ_LOGD) {
                        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                                + " with data profiles : ");
                        for (DataProfile profile : dps) {
                            riljLog(profile.toString());
                        }
                    }

                    radioProxy15.setDataProfile_1_5(rr.mSerial, dpis);
                } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_4)) {
                    // V1.4
                    android.hardware.radio.V1_4.IRadio radioProxy14 =
                            (android.hardware.radio.V1_4.IRadio) radioProxy;

                    ArrayList<android.hardware.radio.V1_4.DataProfileInfo> dpis = new ArrayList<>();
                    for (DataProfile dp : dps) {
                        dpis.add(convertToHalDataProfile14(dp));
                    }

                    if (RILJ_LOGD) {
                        riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                                + " with data profiles : ");
                        for (DataProfile profile : dps) {
                            riljLog(profile.toString());
                        }
                    }

                    radioProxy14.setDataProfile_1_4(rr.mSerial, dpis);
                } else {
                    // V1.0, 1.1, 1,2 and 1.3
                    ArrayList<android.hardware.radio.V1_0.DataProfileInfo> dpis = new ArrayList<>();
                    for (DataProfile dp : dps) {
                        // For v1.0 to v1.2, we only send data profiles that has the persistent
                        // (a.k.a modem cognitive) bit set to true.
                        if (dp.isPersistent()) {
                            dpis.add(convertToHalDataProfile10(dp));
                        }
                    }

                    if (!dpis.isEmpty()) {
                        if (RILJ_LOGD) {
                            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                                    + " with data profiles : ");
                            for (DataProfile profile : dps) {
                                riljLog(profile.toString());
                            }
                        }

                        radioProxy.setDataProfile(rr.mSerial, dpis, isRoaming);
                    }
                }
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setDataProfile", e);
            }
        }
    }

    @Override
    public void requestShutdown(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SHUTDOWN, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.requestShutdown(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "requestShutdown", e);
            }
        }
    }

    @Override
    public void getRadioCapability(Message response) {
        IRadio radioProxy = getRadioProxy(response);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_RADIO_CAPABILITY, response,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.getRadioCapability(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getRadioCapability", e);
            }
        }
    }

    @Override
    public void setRadioCapability(RadioCapability rc, Message response) {
        IRadio radioProxy = getRadioProxy(response);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_RADIO_CAPABILITY, response,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " RadioCapability = " + rc.toString());
            }

            android.hardware.radio.V1_0.RadioCapability halRc =
                    new android.hardware.radio.V1_0.RadioCapability();

            halRc.session = rc.getSession();
            halRc.phase = rc.getPhase();
            halRc.raf = rc.getRadioAccessFamily();
            halRc.logicalModemUuid = convertNullToEmptyString(rc.getLogicalModemUuid());
            halRc.status = rc.getStatus();

            try {
                radioProxy.setRadioCapability(rr.mSerial, halRc);
            } catch (Exception e) {
                handleRadioProxyExceptionForRR(rr, "setRadioCapability", e);
            }
        }
    }

    @Override
    public void startLceService(int reportIntervalMs, boolean pullMode, Message result) {
        IRadio radioProxy = getRadioProxy(result);

        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_2)) {
            // We have a 1.2 or later radio, so the LCE 1.0 LCE service control path is unused.
            // Instead the LCE functionality is always-on and provides unsolicited indications.
            return;
        }

        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_START_LCE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " reportIntervalMs = " + reportIntervalMs + " pullMode = " + pullMode);
            }

            try {
                radioProxy.startLceService(rr.mSerial, reportIntervalMs, pullMode);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "startLceService", e);
            }
        }
    }

    @Override
    public void stopLceService(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_2)) {
            // We have a 1.2 or later radio, so the LCE 1.0 LCE service control is unused.
            // Instead the LCE functionality is always-on and provides unsolicited indications.
            return;
        }

        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_STOP_LCE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.stopLceService(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "stopLceService", e);
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
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            if (mRadioVersion.less(RADIO_HAL_VERSION_1_6)) {
                if (result != null) {
                    AsyncResult.forMessage(result, null,
                            CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                    result.sendToTarget();
                }
                return;
            }

            android.hardware.radio.V1_6.IRadio radioProxy16 =
                    (android.hardware.radio.V1_6.IRadio) radioProxy;
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_DATA_THROTTLING, result,
                    workSource == null ? mRILDefaultWorkSource : workSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> "
                        + requestToString(rr.mRequest)
                        + " dataThrottlingAction = " + dataThrottlingAction
                        + " completionWindowMillis " + completionWindowMillis);
            }

            try {
                radioProxy16.setDataThrottling(rr.mSerial, (byte) dataThrottlingAction,
                        completionWindowMillis);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setDataThrottling", e);
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
    public void pullLceData(Message response) {
        IRadio radioProxy = getRadioProxy(response);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_PULL_LCEDATA, response,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.pullLceData(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "pullLceData", e);
            }
        }
    }

    @Override
    public void getModemActivityInfo(Message result, WorkSource workSource) {
        workSource = getDefaultWorkSourceIfInvalid(workSource);
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_ACTIVITY_INFO, result,
                    workSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.getModemActivityInfo(rr.mSerial);

                Message msg = mRilHandler.obtainMessage(EVENT_BLOCKING_RESPONSE_TIMEOUT);
                msg.obj = null;
                msg.arg1 = rr.mSerial;
                mRilHandler.sendMessageDelayed(msg, DEFAULT_BLOCKING_MESSAGE_RESPONSE_TIMEOUT_MS);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getModemActivityInfo", e);
            }
        }


    }

    /**
     * Convert a list of CarrierIdentifier into a list of Carrier defined in 1.0/types.hal.
     * @param carriers List of CarrierIdentifier
     * @return List of converted objects
     */
    @VisibleForTesting
    public static ArrayList<Carrier> createCarrierRestrictionList(
            List<CarrierIdentifier> carriers) {
        ArrayList<Carrier> result = new ArrayList<>();
        for (CarrierIdentifier ci : carriers) {
            Carrier c = new Carrier();
            c.mcc = convertNullToEmptyString(ci.getMcc());
            c.mnc = convertNullToEmptyString(ci.getMnc());
            int matchType = CarrierIdentifier.MatchType.ALL;
            String matchData = null;
            if (!TextUtils.isEmpty(ci.getSpn())) {
                matchType = CarrierIdentifier.MatchType.SPN;
                matchData = ci.getSpn();
            } else if (!TextUtils.isEmpty(ci.getImsi())) {
                matchType = CarrierIdentifier.MatchType.IMSI_PREFIX;
                matchData = ci.getImsi();
            } else if (!TextUtils.isEmpty(ci.getGid1())) {
                matchType = CarrierIdentifier.MatchType.GID1;
                matchData = ci.getGid1();
            } else if (!TextUtils.isEmpty(ci.getGid2())) {
                matchType = CarrierIdentifier.MatchType.GID2;
                matchData = ci.getGid2();
            }
            c.matchType = matchType;
            c.matchData = convertNullToEmptyString(matchData);
            result.add(c);
        }
        return result;
    }

    @Override
    public void setAllowedCarriers(CarrierRestrictionRules carrierRestrictionRules,
            Message result, WorkSource workSource) {
        riljLog("RIL.java - setAllowedCarriers");

        checkNotNull(carrierRestrictionRules, "Carrier restriction cannot be null.");
        workSource = getDefaultWorkSourceIfInvalid(workSource);

        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy == null) return;

        RILRequest rr = obtainRequest(RIL_REQUEST_SET_ALLOWED_CARRIERS, result, workSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " params: "
                    + carrierRestrictionRules);
        }

        // Extract multisim policy
        int policy = SimLockMultiSimPolicy.NO_MULTISIM_POLICY;
        switch (carrierRestrictionRules.getMultiSimPolicy()) {
            case CarrierRestrictionRules.MULTISIM_POLICY_ONE_VALID_SIM_MUST_BE_PRESENT:
                policy = SimLockMultiSimPolicy.ONE_VALID_SIM_MUST_BE_PRESENT;
                break;
        }

        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_4)) {
            riljLog("RIL.java - Using IRadio 1.4 or greater");

            android.hardware.radio.V1_4.IRadio radioProxy14 =
                    (android.hardware.radio.V1_4.IRadio) radioProxy;

            // Prepare structure with allowed list, excluded list and priority
            CarrierRestrictionsWithPriority carrierRestrictions =
                    new CarrierRestrictionsWithPriority();
            carrierRestrictions.allowedCarriers =
                    createCarrierRestrictionList(carrierRestrictionRules.getAllowedCarriers());
            carrierRestrictions.excludedCarriers =
                    createCarrierRestrictionList(carrierRestrictionRules.getExcludedCarriers());
            carrierRestrictions.allowedCarriersPrioritized =
                    (carrierRestrictionRules.getDefaultCarrierRestriction()
                        == CarrierRestrictionRules.CARRIER_RESTRICTION_DEFAULT_NOT_ALLOWED);

            try {
                radioProxy14.setAllowedCarriers_1_4(rr.mSerial, carrierRestrictions, policy);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setAllowedCarriers_1_4", e);
            }
        } else {
            boolean isAllCarriersAllowed = carrierRestrictionRules.isAllCarriersAllowed();

            boolean supported = (isAllCarriersAllowed
                    || (carrierRestrictionRules.getExcludedCarriers().isEmpty()
                        && (carrierRestrictionRules.getDefaultCarrierRestriction()
                            == CarrierRestrictionRules.CARRIER_RESTRICTION_DEFAULT_NOT_ALLOWED)));
            supported = supported && (policy == SimLockMultiSimPolicy.NO_MULTISIM_POLICY);

            if (!supported) {
                // Feature is not supported by IRadio interface
                riljLoge("setAllowedCarriers does not support excluded list on IRadio version"
                        + " less than 1.4");
                if (result != null) {
                    AsyncResult.forMessage(result, null,
                            CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                    result.sendToTarget();
                }
                return;
            }
            riljLog("RIL.java - Using IRadio 1.3 or lower");

            // Prepare structure with allowed list
            CarrierRestrictions carrierRestrictions = new CarrierRestrictions();
            carrierRestrictions.allowedCarriers =
                    createCarrierRestrictionList(carrierRestrictionRules.getAllowedCarriers());

            try {
                radioProxy.setAllowedCarriers(rr.mSerial, isAllCarriersAllowed,
                        carrierRestrictions);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setAllowedCarriers", e);
            }
        }
    }

    @Override
    public void getAllowedCarriers(Message result, WorkSource workSource) {
        workSource = getDefaultWorkSourceIfInvalid(workSource);

        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy == null) return;

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_ALLOWED_CARRIERS, result,
                workSource);

        if (RILJ_LOGD) {
            riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
        }

        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_4)) {
            riljLog("RIL.java - Using IRadio 1.4 or greater");

            android.hardware.radio.V1_4.IRadio radioProxy14 =
                    (android.hardware.radio.V1_4.IRadio) radioProxy;

            try {
                radioProxy14.getAllowedCarriers_1_4(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getAllowedCarriers_1_4", e);
            }
        } else {
            riljLog("RIL.java - Using IRadio 1.3 or lower");

            try {
                radioProxy.getAllowedCarriers(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getAllowedCarriers", e);
            }
        }
    }

    @Override
    public void sendDeviceState(int stateType, boolean state,
                                Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SEND_DEVICE_STATE, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " "
                        + stateType + ":" + state);
            }

            try {
                radioProxy.sendDeviceState(rr.mSerial, stateType, state);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "sendDeviceState", e);
            }
        }
    }

    @Override
    public void setUnsolResponseFilter(int filter, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_UNSOLICITED_RESPONSE_FILTER, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + filter);
            }

            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
                try {
                    android.hardware.radio.V1_5.IRadio radioProxy15 =
                            (android.hardware.radio.V1_5.IRadio) radioProxy;

                    filter &= INDICATION_FILTERS_ALL_V1_5;
                    radioProxy15.setIndicationFilter_1_5(rr.mSerial, filter);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setIndicationFilter_1_5", e);
                }
            } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_2)) {
                try {
                    android.hardware.radio.V1_2.IRadio radioProxy12 =
                            (android.hardware.radio.V1_2.IRadio) radioProxy;

                    filter &= INDICATION_FILTERS_ALL_V1_2;
                    radioProxy12.setIndicationFilter_1_2(rr.mSerial, filter);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setIndicationFilter_1_2", e);
                }
            } else {
                try {
                    filter &= INDICATION_FILTERS_ALL_V1_0;
                    radioProxy.setIndicationFilter(rr.mSerial, filter);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setIndicationFilter", e);
                }
            }
        }
    }

    @Override
    public void setSignalStrengthReportingCriteria(SignalThresholdInfo signalThresholdInfo,
            int ran, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            if (mRadioVersion.less(RADIO_HAL_VERSION_1_2)) {
                riljLoge("setSignalStrengthReportingCriteria ignored on IRadio version less "
                        + "than 1.2");
                return;
            }
            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_2)
                    && mRadioVersion.less(RADIO_HAL_VERSION_1_5)) {
                RILRequest rr = obtainRequest(RIL_REQUEST_SET_SIGNAL_STRENGTH_REPORTING_CRITERIA,
                        result, mRILDefaultWorkSource);
                if (RILJ_LOGD) {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
                }
                try {
                    android.hardware.radio.V1_2.IRadio radioProxy12 =
                        (android.hardware.radio.V1_2.IRadio) radioProxy;
                    radioProxy12.setSignalStrengthReportingCriteria(rr.mSerial,
                            signalThresholdInfo.getHysteresisMs(),
                            signalThresholdInfo.getHysteresisDb(),
                            primitiveArrayToArrayList(signalThresholdInfo.getThresholds()),
                            convertAntToHalAnt(ran));
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setSignalStrengthReportingCriteria", e);
                }
            }
            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
                RILRequest rr = obtainRequest(RIL_REQUEST_SET_SIGNAL_STRENGTH_REPORTING_CRITERIA,
                        result, mRILDefaultWorkSource);
                if (RILJ_LOGD) {
                    riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
                }
                try {
                    android.hardware.radio.V1_5.IRadio radioProxy15 =
                            (android.hardware.radio.V1_5.IRadio) radioProxy;
                    radioProxy15.setSignalStrengthReportingCriteria_1_5(rr.mSerial,
                            convertToHalSignalThresholdInfo(signalThresholdInfo),
                            convertAntToHalAnt(ran));
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(
                            rr, "setSignalStrengthReportingCriteria_1_5", e);
                }
            }
        }
    }

    private static android.hardware.radio.V1_5.SignalThresholdInfo convertToHalSignalThresholdInfo(
            SignalThresholdInfo signalThresholdInfo) {
        android.hardware.radio.V1_5.SignalThresholdInfo signalThresholdInfoHal =
                new android.hardware.radio.V1_5.SignalThresholdInfo();
        signalThresholdInfoHal.signalMeasurement = signalThresholdInfo.getSignalMeasurementType();
        signalThresholdInfoHal.hysteresisMs = signalThresholdInfo.getHysteresisMs();
        signalThresholdInfoHal.hysteresisDb = signalThresholdInfo.getHysteresisDb();
        signalThresholdInfoHal.thresholds = primitiveArrayToArrayList(
                signalThresholdInfo.getThresholds());
        signalThresholdInfoHal.isEnabled = signalThresholdInfo.isEnabled();
        return signalThresholdInfoHal;
    }

    @Override
    public void setLinkCapacityReportingCriteria(int hysteresisMs, int hysteresisDlKbps,
            int hysteresisUlKbps, int[] thresholdsDlKbps, int[] thresholdsUlKbps, int ran,
            Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_LINK_CAPACITY_REPORTING_CRITERIA, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }
            try {
                if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
                    android.hardware.radio.V1_5.IRadio radioProxy15 =
                            (android.hardware.radio.V1_5.IRadio) radioProxy;
                    radioProxy15.setLinkCapacityReportingCriteria_1_5(rr.mSerial, hysteresisMs,
                            hysteresisDlKbps, hysteresisUlKbps,
                            primitiveArrayToArrayList(thresholdsDlKbps),
                            primitiveArrayToArrayList(thresholdsUlKbps), convertAntToHalAnt(ran));
                } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_2)) {
                    android.hardware.radio.V1_2.IRadio radioProxy12 =
                            (android.hardware.radio.V1_2.IRadio) radioProxy;
                    if (ran == AccessNetworkType.NGRAN) {
                        throw new RuntimeException("NGRAN unsupported on IRadio version 1.2.");
                    }
                    radioProxy12.setLinkCapacityReportingCriteria(rr.mSerial, hysteresisMs,
                            hysteresisDlKbps, hysteresisUlKbps,
                            primitiveArrayToArrayList(thresholdsDlKbps),
                            primitiveArrayToArrayList(thresholdsUlKbps), convertAntToHalAnt(ran));
                } else {
                    riljLoge("setLinkCapacityReportingCriteria ignored on IRadio version less "
                            + "than 1.2");
                }
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "setLinkCapacityReportingCriteria", e);
            }
        }
    }

    /** Converts from AccessNetworkType in frameworks to AccessNetwork in HAL. */
    private static int convertAntToHalAnt(int radioAccessNetwork) {
        switch (radioAccessNetwork) {
            case AccessNetworkType.GERAN:
                return AccessNetwork.GERAN;
            case AccessNetworkType.UTRAN:
                return AccessNetwork.UTRAN;
            case AccessNetworkType.EUTRAN:
                return AccessNetwork.EUTRAN;
            case AccessNetworkType.CDMA2000:
                return AccessNetwork.CDMA2000;
            case AccessNetworkType.IWLAN:
                return AccessNetwork.IWLAN;
            case AccessNetworkType.NGRAN:
                return AccessNetwork.NGRAN;
            case AccessNetworkType.UNKNOWN:
            default:
                return AccessNetwork.UNKNOWN;
        }
    }

    /** Converts from AccessNetworkType in frameworks to RadioAccessNetworks in HAL. */
    private static int convertAntToRan(int accessNetworkType) {
        switch (accessNetworkType) {
            case AccessNetworkType.GERAN:
                return RadioAccessNetworks.GERAN;
            case AccessNetworkType.UTRAN:
                return RadioAccessNetworks.UTRAN;
            case AccessNetworkType.EUTRAN:
                return RadioAccessNetworks.EUTRAN;
            case AccessNetworkType.NGRAN:
                return RadioAccessNetworks.NGRAN;
            case AccessNetworkType.CDMA2000:
                return RadioAccessNetworks.CDMA2000;
            case AccessNetworkType.UNKNOWN:
            default:
                return RadioAccessNetworks.UNKNOWN;
        }
    }

    @Override
    public void setSimCardPower(int state, Message result, WorkSource workSource) {
        workSource = getDefaultWorkSourceIfInvalid(workSource);
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_SET_SIM_CARD_POWER, result,
                    workSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest) + " " + state);
            }

            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                try {
                    android.hardware.radio.V1_6.IRadio radioProxy16 =
                            (android.hardware.radio.V1_6.IRadio) radioProxy;
                    radioProxy16.setSimCardPower_1_6(rr.mSerial, state);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setSimCardPower", e);
                }
            } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_1)) {
                try {
                    android.hardware.radio.V1_1.IRadio radioProxy11 =
                            (android.hardware.radio.V1_1.IRadio) radioProxy;
                    radioProxy11.setSimCardPower_1_1(rr.mSerial, state);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setSimCardPower", e);
                }
            } else {
                try {
                    switch (state) {
                        case TelephonyManager.CARD_POWER_DOWN: {
                            radioProxy.setSimCardPower(rr.mSerial, false);
                            break;
                        }
                        case TelephonyManager.CARD_POWER_UP: {
                            radioProxy.setSimCardPower(rr.mSerial, true);
                            break;
                        }
                        default: {
                            if (result != null) {
                                AsyncResult.forMessage(result, null,
                                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                                result.sendToTarget();
                            }
                        }
                    }
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setSimCardPower", e);
                }
            }
        }
    }

    @Override
    public void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo,
                                                Message result) {
        checkNotNull(imsiEncryptionInfo, "ImsiEncryptionInfo cannot be null.");
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                android.hardware.radio.V1_6.IRadio radioProxy16 =
                        (android.hardware.radio.V1_6.IRadio ) radioProxy;

                RILRequest rr = obtainRequest(RIL_REQUEST_SET_CARRIER_INFO_IMSI_ENCRYPTION, result,
                        mRILDefaultWorkSource);
                if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

                try {
                    android.hardware.radio.V1_6.ImsiEncryptionInfo halImsiInfo =
                            new android.hardware.radio.V1_6.ImsiEncryptionInfo();
                    halImsiInfo.base.mnc = imsiEncryptionInfo.getMnc();
                    halImsiInfo.base.mcc = imsiEncryptionInfo.getMcc();
                    halImsiInfo.base.keyIdentifier = imsiEncryptionInfo.getKeyIdentifier();
                    if (imsiEncryptionInfo.getExpirationTime() != null) {
                        halImsiInfo.base.expirationTime =
                                imsiEncryptionInfo.getExpirationTime().getTime();
                    }
                    for (byte b : imsiEncryptionInfo.getPublicKey().getEncoded()) {
                        halImsiInfo.base.carrierKey.add(new Byte(b));
                    }
                    halImsiInfo.keyType = (byte) imsiEncryptionInfo.getKeyType();

                    radioProxy16.setCarrierInfoForImsiEncryption_1_6(
                            rr.mSerial, halImsiInfo);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setCarrierInfoForImsiEncryption", e);
                }
            } else if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_1)) {
                android.hardware.radio.V1_1.IRadio radioProxy11 =
                        (android.hardware.radio.V1_1.IRadio ) radioProxy;

                RILRequest rr = obtainRequest(RIL_REQUEST_SET_CARRIER_INFO_IMSI_ENCRYPTION, result,
                        mRILDefaultWorkSource);
                if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

                try {
                    android.hardware.radio.V1_1.ImsiEncryptionInfo halImsiInfo =
                            new android.hardware.radio.V1_1.ImsiEncryptionInfo();
                    halImsiInfo.mnc = imsiEncryptionInfo.getMnc();
                    halImsiInfo.mcc = imsiEncryptionInfo.getMcc();
                    halImsiInfo.keyIdentifier = imsiEncryptionInfo.getKeyIdentifier();
                    if (imsiEncryptionInfo.getExpirationTime() != null) {
                        halImsiInfo.expirationTime =
                                imsiEncryptionInfo.getExpirationTime().getTime();
                    }
                    for (byte b : imsiEncryptionInfo.getPublicKey().getEncoded()) {
                        halImsiInfo.carrierKey.add(new Byte(b));
                    }

                    radioProxy11.setCarrierInfoForImsiEncryption(
                            rr.mSerial, halImsiInfo);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "setCarrierInfoForImsiEncryption", e);
                }
            } else if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
        }
    }

    @Override
    public void startNattKeepalive(
            int contextId, KeepalivePacketData packetData, int intervalMillis, Message result) {
        checkNotNull(packetData, "KeepaliveRequest cannot be null.");
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy == null) return;

        if (mRadioVersion.less(RADIO_HAL_VERSION_1_1)) {
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
            return;
        }

        android.hardware.radio.V1_1.IRadio radioProxy11 =
                (android.hardware.radio.V1_1.IRadio) radioProxy;

        RILRequest rr = obtainRequest(
                RIL_REQUEST_START_KEEPALIVE, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        try {
            android.hardware.radio.V1_1.KeepaliveRequest req =
                    new android.hardware.radio.V1_1.KeepaliveRequest();

            req.cid = contextId;

            if (packetData.getDstAddress() instanceof Inet4Address) {
                req.type = android.hardware.radio.V1_1.KeepaliveType.NATT_IPV4;
            } else if (packetData.getDstAddress() instanceof Inet6Address) {
                req.type = android.hardware.radio.V1_1.KeepaliveType.NATT_IPV6;
            } else {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(INVALID_ARGUMENTS));
                result.sendToTarget();
                return;
            }

            final InetAddress srcAddress = packetData.getSrcAddress();
            final InetAddress dstAddress = packetData.getDstAddress();
            appendPrimitiveArrayToArrayList(
                    srcAddress.getAddress(), req.sourceAddress);
            req.sourcePort = packetData.getSrcPort();
            appendPrimitiveArrayToArrayList(
                    dstAddress.getAddress(), req.destinationAddress);
            req.destinationPort = packetData.getDstPort();
            req.maxKeepaliveIntervalMillis = intervalMillis;

            radioProxy11.startKeepalive(rr.mSerial, req);
        } catch (RemoteException | RuntimeException e) {
            handleRadioProxyExceptionForRR(rr, "startNattKeepalive", e);
        }
    }

    @Override
    public void stopNattKeepalive(int sessionHandle, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy == null) return;

        if (mRadioVersion.less(RADIO_HAL_VERSION_1_1)) {
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
            return;
        }

        android.hardware.radio.V1_1.IRadio radioProxy11 =
                (android.hardware.radio.V1_1.IRadio) radioProxy;

        RILRequest rr = obtainRequest(
                RIL_REQUEST_STOP_KEEPALIVE, result, mRILDefaultWorkSource);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        try {
            radioProxy11.stopKeepalive(rr.mSerial, sessionHandle);
        } catch (RemoteException | RuntimeException e) {
            handleRadioProxyExceptionForRR(rr, "stopNattKeepalive", e);
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
     * @param onCompleteMessage a Message to return to the requester
     */
    @Override
    public void enableUiccApplications(boolean enable, Message onCompleteMessage) {
        IRadio radioProxy = getRadioProxy(onCompleteMessage);
        if (radioProxy == null) return;

        if (mRadioVersion.less(RADIO_HAL_VERSION_1_5)) {
            if (onCompleteMessage != null) {
                AsyncResult.forMessage(onCompleteMessage, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                onCompleteMessage.sendToTarget();
            }
            return;
        }

        android.hardware.radio.V1_5.IRadio radioProxy15 =
                (android.hardware.radio.V1_5.IRadio) radioProxy;

        RILRequest rr = obtainRequest(RIL_REQUEST_ENABLE_UICC_APPLICATIONS,
                onCompleteMessage, mRILDefaultWorkSource);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        try {
            radioProxy15.enableUiccApplications(rr.mSerial, enable);
        } catch (RemoteException | RuntimeException e) {
            handleRadioProxyExceptionForRR(rr, "enableUiccApplications", e);
        }
    }

    /**
     * Whether uicc applications are enabled or not.
     *
     * @param onCompleteMessage a Message to return to the requester
     */
    @Override
    public void areUiccApplicationsEnabled(Message onCompleteMessage) {
        IRadio radioProxy = getRadioProxy(onCompleteMessage);
        if (radioProxy == null) return;

        if (mRadioVersion.less(RADIO_HAL_VERSION_1_5)) {
            if (onCompleteMessage != null) {
                AsyncResult.forMessage(onCompleteMessage, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                onCompleteMessage.sendToTarget();
            }
            return;
        }

        android.hardware.radio.V1_5.IRadio radioProxy15 =
                (android.hardware.radio.V1_5.IRadio) radioProxy;

        RILRequest rr = obtainRequest(RIL_REQUEST_GET_UICC_APPLICATIONS_ENABLEMENT,
                onCompleteMessage, mRILDefaultWorkSource);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

        try {
            radioProxy15.areUiccApplicationsEnabled(rr.mSerial);
        } catch (RemoteException | RuntimeException e) {
            handleRadioProxyExceptionForRR(rr, "areUiccApplicationsEnabled", e);
        }
    }

    /**
     * Whether {@link #enableUiccApplications} is supported, which is supported in 1.5 version.
     */
    @Override
    public boolean canToggleUiccApplicationsEnablement() {
        return getRadioProxy(null) != null && mRadioVersion
                .greaterOrEqual(RADIO_HAL_VERSION_1_5);
    }

    /**
     *  Translates EF_SMS status bits to a status value compatible with
     *  SMS AT commands.  See TS 27.005 3.1.
     */
    private int translateStatus(int status) {
        switch(status & 0x7) {
            case SmsManager.STATUS_ON_ICC_READ:
                return 1;
            case SmsManager.STATUS_ON_ICC_UNREAD:
                return 0;
            case SmsManager.STATUS_ON_ICC_SENT:
                return 3;
            case SmsManager.STATUS_ON_ICC_UNSENT:
                return 2;
        }

        // Default to READ.
        return 1;
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
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM,
                    result, mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            try {
                radioProxy.handleStkCallSetupRequestFromSim(rr.mSerial, accept);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getAllowedCarriers", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getBarringInfo(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy == null) return;

        if (mRadioVersion.less(RADIO_HAL_VERSION_1_5)) {
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                result.sendToTarget();
            }
            return;
        }

        android.hardware.radio.V1_5.IRadio radioProxy15 =
                (android.hardware.radio.V1_5.IRadio) radioProxy;
        if (radioProxy15 != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_BARRING_INFO, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy15.getBarringInfo(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getBarringInfo", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void allocatePduSessionId(Message result) {
        android.hardware.radio.V1_6.IRadio radioProxy16 = getRadioV16(result);

        if (radioProxy16 != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_ALLOCATE_PDU_SESSION_ID, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy16.allocatePduSessionId(rr.mSerial);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rr, "allocatePduSessionId", e);
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
        android.hardware.radio.V1_6.IRadio radioProxy16 = getRadioV16(result);

        if (radioProxy16 != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_RELEASE_PDU_SESSION_ID, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy16.releasePduSessionId(rr.mSerial, pduSessionId);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rr, "releasePduSessionId", e);
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
        android.hardware.radio.V1_6.IRadio radioProxy16 = getRadioV16(result);

        if (radioProxy16 != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_START_HANDOVER, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy16.startHandover(rr.mSerial, callId);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rr, "startHandover", e);
            }
        } else {
            if (RILJ_LOGD) Rlog.d(RILJ_LOG_TAG, "startHandover: REQUEST_NOT_SUPPORTED");
            AsyncResult.forMessage(result, null,
                    CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
            result.sendToTarget();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelHandover(Message result, int callId) {
        android.hardware.radio.V1_6.IRadio radioProxy16 = getRadioV16(result);

        if (radioProxy16 != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_CANCEL_HANDOVER, result,
                    mRILDefaultWorkSource);
            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy16.cancelHandover(rr.mSerial, callId);
            } catch (RemoteException e) {
                handleRadioProxyExceptionForRR(rr, "cancelHandover", e);
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
        android.hardware.radio.V1_6.IRadio radioProxy16 = getRadioV16(result);

        if (radioProxy16 != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_SLICING_CONFIG, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));

            try {
                radioProxy16.getSlicingConfig(rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "getSlicingConfig", e);
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
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_SIM_PHONEBOOK_RECORDS, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                android.hardware.radio.V1_6.IRadio radioProxy16 =
                            android.hardware.radio.V1_6.IRadio.castFrom(radioProxy);
                try {
                    radioProxy16.getSimPhonebookRecords(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "getPhonebookRecords", e);
                }
            } else {
                riljLog("Unsupported API in lower than version 1.6 radio HAL" );
                if (result != null) {
                    AsyncResult.forMessage(result, null,
                    CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                    result.sendToTarget();
                }
            }
        }
    }

    @Override
    public void getSimPhonebookCapacity(Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_GET_SIM_PHONEBOOK_CAPACITY, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }

            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                android.hardware.radio.V1_6.IRadio radioProxy16 =
                            android.hardware.radio.V1_6.IRadio.castFrom(radioProxy);
                try {
                    radioProxy16.getSimPhonebookCapacity(rr.mSerial);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "getPhonebookRecords", e);
                }
            } else {
                riljLog("Unsupported API in lower than version 1.6 radio HAL" );
                if (result != null) {
                    AsyncResult.forMessage(result, null,
                    CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                    result.sendToTarget();
                }
            }
        }
    }

    @Override
    public void updateSimPhonebookRecord(SimPhonebookRecord phonebookRecord, Message result) {
        IRadio radioProxy = getRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = obtainRequest(RIL_REQUEST_UPDATE_SIM_PHONEBOOK_RECORD, result,
                    mRILDefaultWorkSource);

            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                        + " with " + phonebookRecord.toString());
            }

            if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
                android.hardware.radio.V1_6.IRadio radioProxy16 =
                        android.hardware.radio.V1_6.IRadio.castFrom(radioProxy);

                android.hardware.radio.V1_6.PhonebookRecordInfo pbRecordInfo =
                        phonebookRecord.toPhonebookRecordInfo();
                try {
                     radioProxy16.updateSimPhonebookRecords(rr.mSerial, pbRecordInfo);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(rr, "updatePhonebookRecord", e);
                }
            } else {
                riljLog("Unsupported API in lower than version 1.6 radio HAL" );
                if (result != null) {
                    AsyncResult.forMessage(result, null,
                    CommandException.fromRilErrno(REQUEST_NOT_SUPPORTED));
                    result.sendToTarget();
                }
            }
        }
    }

    //***** Private Methods
    /** Helper that gets V1.6 of the radio interface OR sends back REQUEST_NOT_SUPPORTED */
    @Nullable private android.hardware.radio.V1_6.IRadio getRadioV16(Message msg) {
        IRadio radioProxy = getRadioProxy(msg);
        if (mRadioVersion.greaterOrEqual(RADIO_HAL_VERSION_1_6)) {
            return (android.hardware.radio.V1_6.IRadio) radioProxy;
        } else {
            return (android.hardware.radio.V1_6.IRadio) null;
        }
    }


    /**
     * This is a helper function to be called when a RadioIndication callback is called.
     * It takes care of acquiring wakelock and sending ack if needed.
     * @param indicationType RadioIndicationType received
     */
    void processIndication(int indicationType) {
        if (indicationType == RadioIndicationType.UNSOLICITED_ACK_EXP) {
            sendAck();
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
            Rlog.w(RIL.RILJ_LOG_TAG, "processRequestAck: Unexpected solicited ack response! "
                    + "serial: " + serial);
        } else {
            decrementWakeLock(rr);
            if (RIL.RILJ_LOGD) {
                riljLog(rr.serialString() + " Ack < " + RIL.requestToString(rr.mRequest));
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
        return processResponseInternal(responseInfo.serial, responseInfo.error, responseInfo.type);
    }

    /**
     * This is a helper function for V1_6.RadioResponseInfo to be called when a RadioResponse
     * callback is called.
     * It takes care of acks, wakelocks, and finds and returns RILRequest corresponding to the
     * response if one is found.
     * @param responseInfo RadioResponseInfo received in response callback
     * @return RILRequest corresponding to the response
     */
    @VisibleForTesting
    public RILRequest processResponse_1_6(
                    android.hardware.radio.V1_6.RadioResponseInfo responseInfo) {
        return processResponseInternal(responseInfo.serial, responseInfo.error, responseInfo.type);
    }

    private RILRequest processResponseInternal(int serial, int error, int type) {
        RILRequest rr = null;

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
                    riljLog(rr.serialString() + " Ack < " + requestToString(rr.mRequest));
                }
            }
            return rr;
        }

        rr = findAndRemoveRequestFromList(serial);
        if (rr == null) {
            Rlog.e(RIL.RILJ_LOG_TAG, "processResponse: Unexpected response! serial: " + serial
                    + " error: " + error);
            return null;
        }

        // Time logging for RIL command and storing it in TelephonyHistogram.
        addToRilHistogram(rr);
        if (mRadioBugDetector != null) {
            mRadioBugDetector.detectRadioBug(rr.mRequest, error);
        }
        if (type == RadioResponseType.SOLICITED_ACK_EXP) {
            sendAck();
            if (RIL.RILJ_LOGD) {
                riljLog("Response received for " + rr.serialString() + " "
                        + RIL.requestToString(rr.mRequest) + " Sending ack to ril.cpp");
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
    public void processResponseDone_1_6(
                    RILRequest rr, android.hardware.radio.V1_6.RadioResponseInfo responseInfo,
                    Object ret) {
        processResponseDoneInternal(rr, responseInfo.error, responseInfo.type, ret);
    }

    private void processResponseDoneInternal(
            RILRequest rr, int rilError, int responseType, Object ret) {
        if (rilError == 0) {
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
                        + " " + retToString(rr.mRequest, ret));
            }
        } else {
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
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
            riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
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
    private void sendAck() {
        // TODO: Remove rr and clean up acquireWakelock for response and ack
        RILRequest rr = RILRequest.obtain(RIL_RESPONSE_ACKNOWLEDGEMENT, null,
                mRILDefaultWorkSource);
        acquireWakeLock(rr, RIL.FOR_ACK_WAKELOCK);
        IRadio radioProxy = getRadioProxy(null);
        if (radioProxy != null) {
            try {
                radioProxy.responseAcknowledgement();
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRR(rr, "sendAck", e);
                riljLoge("sendAck: " + e);
            }
        } else {
            Rlog.e(RILJ_LOG_TAG, "Error trying to send ack, radioProxy = null");
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
                            + requestToString(rr.mRequest));
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
        RILRequest rr = null;
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

        synchronized (mRilTimeHistograms) {
            TelephonyHistogram entry = mRilTimeHistograms.get(rr.mRequest);
            if (entry == null) {
                // We would have total #RIL_HISTOGRAM_BUCKET_COUNT range buckets for RIL commands
                entry = new TelephonyHistogram(TelephonyHistogram.TELEPHONY_CATEGORY_RIL,
                        rr.mRequest, RIL_HISTOGRAM_BUCKET_COUNT);
                mRilTimeHistograms.put(rr.mRequest, entry);
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
            s = ret.toString();
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
    void
    notifyRegistrantsCdmaInfoRec(CdmaInformationRecords infoRec) {
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
    static String requestToString(int request) {
        switch(request) {
            case RIL_REQUEST_GET_SIM_STATUS:
                return "GET_SIM_STATUS";
            case RIL_REQUEST_ENTER_SIM_PIN:
                return "ENTER_SIM_PIN";
            case RIL_REQUEST_ENTER_SIM_PUK:
                return "ENTER_SIM_PUK";
            case RIL_REQUEST_ENTER_SIM_PIN2:
                return "ENTER_SIM_PIN2";
            case RIL_REQUEST_ENTER_SIM_PUK2:
                return "ENTER_SIM_PUK2";
            case RIL_REQUEST_CHANGE_SIM_PIN:
                return "CHANGE_SIM_PIN";
            case RIL_REQUEST_CHANGE_SIM_PIN2:
                return "CHANGE_SIM_PIN2";
            case RIL_REQUEST_ENTER_NETWORK_DEPERSONALIZATION:
                return "ENTER_NETWORK_DEPERSONALIZATION";
            case RIL_REQUEST_GET_CURRENT_CALLS:
                return "GET_CURRENT_CALLS";
            case RIL_REQUEST_DIAL:
                return "DIAL";
            case RIL_REQUEST_GET_IMSI:
                return "GET_IMSI";
            case RIL_REQUEST_HANGUP:
                return "HANGUP";
            case RIL_REQUEST_HANGUP_WAITING_OR_BACKGROUND:
                return "HANGUP_WAITING_OR_BACKGROUND";
            case RIL_REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND:
                return "HANGUP_FOREGROUND_RESUME_BACKGROUND";
            case RIL_REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE:
                return "REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE";
            case RIL_REQUEST_CONFERENCE:
                return "CONFERENCE";
            case RIL_REQUEST_UDUB:
                return "UDUB";
            case RIL_REQUEST_LAST_CALL_FAIL_CAUSE:
                return "LAST_CALL_FAIL_CAUSE";
            case RIL_REQUEST_SIGNAL_STRENGTH:
                return "SIGNAL_STRENGTH";
            case RIL_REQUEST_VOICE_REGISTRATION_STATE:
                return "VOICE_REGISTRATION_STATE";
            case RIL_REQUEST_DATA_REGISTRATION_STATE:
                return "DATA_REGISTRATION_STATE";
            case RIL_REQUEST_OPERATOR:
                return "OPERATOR";
            case RIL_REQUEST_RADIO_POWER:
                return "RADIO_POWER";
            case RIL_REQUEST_DTMF:
                return "DTMF";
            case RIL_REQUEST_SEND_SMS:
                return "SEND_SMS";
            case RIL_REQUEST_SEND_SMS_EXPECT_MORE:
                return "SEND_SMS_EXPECT_MORE";
            case RIL_REQUEST_SETUP_DATA_CALL:
                return "SETUP_DATA_CALL";
            case RIL_REQUEST_SIM_IO:
                return "SIM_IO";
            case RIL_REQUEST_SEND_USSD:
                return "SEND_USSD";
            case RIL_REQUEST_CANCEL_USSD:
                return "CANCEL_USSD";
            case RIL_REQUEST_GET_CLIR:
                return "GET_CLIR";
            case RIL_REQUEST_SET_CLIR:
                return "SET_CLIR";
            case RIL_REQUEST_QUERY_CALL_FORWARD_STATUS:
                return "QUERY_CALL_FORWARD_STATUS";
            case RIL_REQUEST_SET_CALL_FORWARD:
                return "SET_CALL_FORWARD";
            case RIL_REQUEST_QUERY_CALL_WAITING:
                return "QUERY_CALL_WAITING";
            case RIL_REQUEST_SET_CALL_WAITING:
                return "SET_CALL_WAITING";
            case RIL_REQUEST_SMS_ACKNOWLEDGE:
                return "SMS_ACKNOWLEDGE";
            case RIL_REQUEST_GET_IMEI:
                return "GET_IMEI";
            case RIL_REQUEST_GET_IMEISV:
                return "GET_IMEISV";
            case RIL_REQUEST_ANSWER:
                return "ANSWER";
            case RIL_REQUEST_DEACTIVATE_DATA_CALL:
                return "DEACTIVATE_DATA_CALL";
            case RIL_REQUEST_QUERY_FACILITY_LOCK:
                return "QUERY_FACILITY_LOCK";
            case RIL_REQUEST_SET_FACILITY_LOCK:
                return "SET_FACILITY_LOCK";
            case RIL_REQUEST_CHANGE_BARRING_PASSWORD:
                return "CHANGE_BARRING_PASSWORD";
            case RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE:
                return "QUERY_NETWORK_SELECTION_MODE";
            case RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC:
                return "SET_NETWORK_SELECTION_AUTOMATIC";
            case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL:
                return "SET_NETWORK_SELECTION_MANUAL";
            case RIL_REQUEST_QUERY_AVAILABLE_NETWORKS :
                return "QUERY_AVAILABLE_NETWORKS ";
            case RIL_REQUEST_DTMF_START:
                return "DTMF_START";
            case RIL_REQUEST_DTMF_STOP:
                return "DTMF_STOP";
            case RIL_REQUEST_BASEBAND_VERSION:
                return "BASEBAND_VERSION";
            case RIL_REQUEST_SEPARATE_CONNECTION:
                return "SEPARATE_CONNECTION";
            case RIL_REQUEST_SET_MUTE:
                return "SET_MUTE";
            case RIL_REQUEST_GET_MUTE:
                return "GET_MUTE";
            case RIL_REQUEST_QUERY_CLIP:
                return "QUERY_CLIP";
            case RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE:
                return "LAST_DATA_CALL_FAIL_CAUSE";
            case RIL_REQUEST_DATA_CALL_LIST:
                return "DATA_CALL_LIST";
            case RIL_REQUEST_RESET_RADIO:
                return "RESET_RADIO";
            case RIL_REQUEST_OEM_HOOK_RAW:
                return "OEM_HOOK_RAW";
            case RIL_REQUEST_OEM_HOOK_STRINGS:
                return "OEM_HOOK_STRINGS";
            case RIL_REQUEST_SCREEN_STATE:
                return "SCREEN_STATE";
            case RIL_REQUEST_SET_SUPP_SVC_NOTIFICATION:
                return "SET_SUPP_SVC_NOTIFICATION";
            case RIL_REQUEST_WRITE_SMS_TO_SIM:
                return "WRITE_SMS_TO_SIM";
            case RIL_REQUEST_DELETE_SMS_ON_SIM:
                return "DELETE_SMS_ON_SIM";
            case RIL_REQUEST_SET_BAND_MODE:
                return "SET_BAND_MODE";
            case RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE:
                return "QUERY_AVAILABLE_BAND_MODE";
            case RIL_REQUEST_STK_GET_PROFILE:
                return "STK_GET_PROFILE";
            case RIL_REQUEST_STK_SET_PROFILE:
                return "STK_SET_PROFILE";
            case RIL_REQUEST_STK_SEND_ENVELOPE_COMMAND:
                return "STK_SEND_ENVELOPE_COMMAND";
            case RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE:
                return "STK_SEND_TERMINAL_RESPONSE";
            case RIL_REQUEST_STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM:
                return "STK_HANDLE_CALL_SETUP_REQUESTED_FROM_SIM";
            case RIL_REQUEST_EXPLICIT_CALL_TRANSFER:
                return "EXPLICIT_CALL_TRANSFER";
            case RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE:
                return "SET_PREFERRED_NETWORK_TYPE";
            case RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE:
                return "GET_PREFERRED_NETWORK_TYPE";
            case RIL_REQUEST_GET_NEIGHBORING_CELL_IDS:
                return "GET_NEIGHBORING_CELL_IDS";
            case RIL_REQUEST_SET_LOCATION_UPDATES:
                return "SET_LOCATION_UPDATES";
            case RIL_REQUEST_CDMA_SET_SUBSCRIPTION_SOURCE:
                return "CDMA_SET_SUBSCRIPTION_SOURCE";
            case RIL_REQUEST_CDMA_SET_ROAMING_PREFERENCE:
                return "CDMA_SET_ROAMING_PREFERENCE";
            case RIL_REQUEST_CDMA_QUERY_ROAMING_PREFERENCE:
                return "CDMA_QUERY_ROAMING_PREFERENCE";
            case RIL_REQUEST_SET_TTY_MODE:
                return "SET_TTY_MODE";
            case RIL_REQUEST_QUERY_TTY_MODE:
                return "QUERY_TTY_MODE";
            case RIL_REQUEST_CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE:
                return "CDMA_SET_PREFERRED_VOICE_PRIVACY_MODE";
            case RIL_REQUEST_CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE:
                return "CDMA_QUERY_PREFERRED_VOICE_PRIVACY_MODE";
            case RIL_REQUEST_CDMA_FLASH:
                return "CDMA_FLASH";
            case RIL_REQUEST_CDMA_BURST_DTMF:
                return "CDMA_BURST_DTMF";
            case RIL_REQUEST_CDMA_VALIDATE_AND_WRITE_AKEY:
                return "CDMA_VALIDATE_AND_WRITE_AKEY";
            case RIL_REQUEST_CDMA_SEND_SMS:
                return "CDMA_SEND_SMS";
            case RIL_REQUEST_CDMA_SMS_ACKNOWLEDGE:
                return "CDMA_SMS_ACKNOWLEDGE";
            case RIL_REQUEST_GSM_GET_BROADCAST_CONFIG:
                return "GSM_GET_BROADCAST_CONFIG";
            case RIL_REQUEST_GSM_SET_BROADCAST_CONFIG:
                return "GSM_SET_BROADCAST_CONFIG";
            case RIL_REQUEST_GSM_BROADCAST_ACTIVATION:
                return "GSM_BROADCAST_ACTIVATION";
            case RIL_REQUEST_CDMA_GET_BROADCAST_CONFIG:
                return "CDMA_GET_BROADCAST_CONFIG";
            case RIL_REQUEST_CDMA_SET_BROADCAST_CONFIG:
                return "CDMA_SET_BROADCAST_CONFIG";
            case RIL_REQUEST_CDMA_BROADCAST_ACTIVATION:
                return "CDMA_BROADCAST_ACTIVATION";
            case RIL_REQUEST_CDMA_SUBSCRIPTION:
                return "CDMA_SUBSCRIPTION";
            case RIL_REQUEST_CDMA_WRITE_SMS_TO_RUIM:
                return "CDMA_WRITE_SMS_TO_RUIM";
            case RIL_REQUEST_CDMA_DELETE_SMS_ON_RUIM:
                return "CDMA_DELETE_SMS_ON_RUIM";
            case RIL_REQUEST_DEVICE_IDENTITY:
                return "DEVICE_IDENTITY";
            case RIL_REQUEST_EXIT_EMERGENCY_CALLBACK_MODE:
                return "EXIT_EMERGENCY_CALLBACK_MODE";
            case RIL_REQUEST_GET_SMSC_ADDRESS:
                return "GET_SMSC_ADDRESS";
            case RIL_REQUEST_SET_SMSC_ADDRESS:
                return "SET_SMSC_ADDRESS";
            case RIL_REQUEST_REPORT_SMS_MEMORY_STATUS:
                return "REPORT_SMS_MEMORY_STATUS";
            case RIL_REQUEST_REPORT_STK_SERVICE_IS_RUNNING:
                return "REPORT_STK_SERVICE_IS_RUNNING";
            case RIL_REQUEST_CDMA_GET_SUBSCRIPTION_SOURCE:
                return "CDMA_GET_SUBSCRIPTION_SOURCE";
            case RIL_REQUEST_ISIM_AUTHENTICATION:
                return "ISIM_AUTHENTICATION";
            case RIL_REQUEST_ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU:
                return "ACKNOWLEDGE_INCOMING_GSM_SMS_WITH_PDU";
            case RIL_REQUEST_STK_SEND_ENVELOPE_WITH_STATUS:
                return "STK_SEND_ENVELOPE_WITH_STATUS";
            case RIL_REQUEST_VOICE_RADIO_TECH:
                return "VOICE_RADIO_TECH";
            case RIL_REQUEST_GET_CELL_INFO_LIST:
                return "GET_CELL_INFO_LIST";
            case RIL_REQUEST_SET_UNSOL_CELL_INFO_LIST_RATE:
                return "SET_CELL_INFO_LIST_RATE";
            case RIL_REQUEST_SET_INITIAL_ATTACH_APN:
                return "SET_INITIAL_ATTACH_APN";
            case RIL_REQUEST_IMS_REGISTRATION_STATE:
                return "IMS_REGISTRATION_STATE";
            case RIL_REQUEST_IMS_SEND_SMS:
                return "IMS_SEND_SMS";
            case RIL_REQUEST_SIM_TRANSMIT_APDU_BASIC:
                return "SIM_TRANSMIT_APDU_BASIC";
            case RIL_REQUEST_SIM_OPEN_CHANNEL:
                return "SIM_OPEN_CHANNEL";
            case RIL_REQUEST_SIM_CLOSE_CHANNEL:
                return "SIM_CLOSE_CHANNEL";
            case RIL_REQUEST_SIM_TRANSMIT_APDU_CHANNEL:
                return "SIM_TRANSMIT_APDU_CHANNEL";
            case RIL_REQUEST_NV_READ_ITEM:
                return "NV_READ_ITEM";
            case RIL_REQUEST_NV_WRITE_ITEM:
                return "NV_WRITE_ITEM";
            case RIL_REQUEST_NV_WRITE_CDMA_PRL:
                return "NV_WRITE_CDMA_PRL";
            case RIL_REQUEST_NV_RESET_CONFIG:
                return "NV_RESET_CONFIG";
            case RIL_REQUEST_SET_UICC_SUBSCRIPTION:
                return "SET_UICC_SUBSCRIPTION";
            case RIL_REQUEST_ALLOW_DATA:
                return "ALLOW_DATA";
            case RIL_REQUEST_GET_HARDWARE_CONFIG:
                return "GET_HARDWARE_CONFIG";
            case RIL_REQUEST_SIM_AUTHENTICATION:
                return "SIM_AUTHENTICATION";
            case RIL_REQUEST_GET_DC_RT_INFO:
                return "GET_DC_RT_INFO";
            case RIL_REQUEST_SET_DC_RT_INFO_RATE:
                return "SET_DC_RT_INFO_RATE";
            case RIL_REQUEST_SET_DATA_PROFILE:
                return "SET_DATA_PROFILE";
            case RIL_REQUEST_SHUTDOWN:
                return "SHUTDOWN";
            case RIL_REQUEST_GET_RADIO_CAPABILITY:
                return "GET_RADIO_CAPABILITY";
            case RIL_REQUEST_SET_RADIO_CAPABILITY:
                return "SET_RADIO_CAPABILITY";
            case RIL_REQUEST_START_LCE:
                return "START_LCE";
            case RIL_REQUEST_STOP_LCE:
                return "STOP_LCE";
            case RIL_REQUEST_PULL_LCEDATA:
                return "PULL_LCEDATA";
            case RIL_REQUEST_GET_ACTIVITY_INFO:
                return "GET_ACTIVITY_INFO";
            case RIL_REQUEST_SET_ALLOWED_CARRIERS:
                return "SET_ALLOWED_CARRIERS";
            case RIL_REQUEST_GET_ALLOWED_CARRIERS:
                return "GET_ALLOWED_CARRIERS";
            case RIL_REQUEST_SEND_DEVICE_STATE:
                return "SEND_DEVICE_STATE";
            case RIL_REQUEST_SET_UNSOLICITED_RESPONSE_FILTER:
                return "SET_UNSOLICITED_RESPONSE_FILTER";
            case RIL_REQUEST_SET_SIM_CARD_POWER:
                return "SET_SIM_CARD_POWER";
            case RIL_REQUEST_SET_CARRIER_INFO_IMSI_ENCRYPTION:
                return "SET_CARRIER_INFO_IMSI_ENCRYPTION";
            case RIL_REQUEST_START_NETWORK_SCAN:
                return "START_NETWORK_SCAN";
            case RIL_REQUEST_STOP_NETWORK_SCAN:
                return "STOP_NETWORK_SCAN";
            case RIL_REQUEST_START_KEEPALIVE:
                return "START_KEEPALIVE";
            case RIL_REQUEST_STOP_KEEPALIVE:
                return "STOP_KEEPALIVE";
            case RIL_REQUEST_ENABLE_MODEM:
                return "ENABLE_MODEM";
            case RIL_REQUEST_GET_MODEM_STATUS:
                return "GET_MODEM_STATUS";
            case RIL_REQUEST_CDMA_SEND_SMS_EXPECT_MORE:
                return "CDMA_SEND_SMS_EXPECT_MORE";
            case RIL_REQUEST_GET_SLOT_STATUS:
                return "GET_SLOT_STATUS";
            case RIL_REQUEST_SET_LOGICAL_TO_PHYSICAL_SLOT_MAPPING:
                return "SET_LOGICAL_TO_PHYSICAL_SLOT_MAPPING";
            case RIL_REQUEST_SET_SIGNAL_STRENGTH_REPORTING_CRITERIA:
                return "SET_SIGNAL_STRENGTH_REPORTING_CRITERIA";
            case RIL_REQUEST_SET_LINK_CAPACITY_REPORTING_CRITERIA:
                return "SET_LINK_CAPACITY_REPORTING_CRITERIA";
            case RIL_REQUEST_SET_PREFERRED_DATA_MODEM:
                return "SET_PREFERRED_DATA_MODEM";
            case RIL_REQUEST_EMERGENCY_DIAL:
                return "EMERGENCY_DIAL";
            case RIL_REQUEST_GET_PHONE_CAPABILITY:
                return "GET_PHONE_CAPABILITY";
            case RIL_REQUEST_SWITCH_DUAL_SIM_CONFIG:
                return "SWITCH_DUAL_SIM_CONFIG";
            case RIL_REQUEST_ENABLE_UICC_APPLICATIONS:
                return "ENABLE_UICC_APPLICATIONS";
            case RIL_REQUEST_GET_UICC_APPLICATIONS_ENABLEMENT:
                return "GET_UICC_APPLICATIONS_ENABLEMENT";
            case RIL_REQUEST_SET_SYSTEM_SELECTION_CHANNELS:
                return "SET_SYSTEM_SELECTION_CHANNELS";
            case RIL_REQUEST_GET_BARRING_INFO:
                return "GET_BARRING_INFO";
            case RIL_REQUEST_ENTER_SIM_DEPERSONALIZATION:
                return "ENTER_SIM_DEPERSONALIZATION";
            case RIL_REQUEST_ENABLE_NR_DUAL_CONNECTIVITY:
                return "ENABLE_NR_DUAL_CONNECTIVITY";
            case RIL_REQUEST_IS_NR_DUAL_CONNECTIVITY_ENABLED:
                return "IS_NR_DUAL_CONNECTIVITY_ENABLED";
            case RIL_REQUEST_ALLOCATE_PDU_SESSION_ID:
                return "ALLOCATE_PDU_SESSION_ID";
            case RIL_REQUEST_RELEASE_PDU_SESSION_ID:
                return "RELEASE_PDU_SESSION_ID";
            case RIL_REQUEST_START_HANDOVER:
                return "START_HANDOVER";
            case RIL_REQUEST_CANCEL_HANDOVER:
                return "CANCEL_HANDOVER";
            case RIL_REQUEST_GET_SYSTEM_SELECTION_CHANNELS:
                return "GET_SYSTEM_SELECTION_CHANNELS";
            case RIL_REQUEST_GET_HAL_DEVICE_CAPABILITIES:
                return "GET_HAL_DEVICE_CAPABILITIES";
            case RIL_REQUEST_SET_DATA_THROTTLING:
                return "SET_DATA_THROTTLING";
            case RIL_REQUEST_SET_ALLOWED_NETWORK_TYPES_BITMAP:
                return "SET_ALLOWED_NETWORK_TYPES_BITMAP";
            case RIL_REQUEST_GET_ALLOWED_NETWORK_TYPES_BITMAP:
                return "GET_ALLOWED_NETWORK_TYPES_BITMAP";
            case RIL_REQUEST_GET_SLICING_CONFIG:
                return "GET_SLICING_CONFIG";
            case RIL_REQUEST_GET_SIM_PHONEBOOK_RECORDS:
                return "GET_SIM_PHONEBOOK_RECORDS";
            case RIL_REQUEST_UPDATE_SIM_PHONEBOOK_RECORD:
                return "UPDATE_SIM_PHONEBOOK_RECORD";
            case RIL_REQUEST_GET_SIM_PHONEBOOK_CAPACITY:
                return "GET_SIM_PHONEBOOK_CAPACITY";
            default: return "<unknown request>";
        }
    }

    @UnsupportedAppUsage
    static String responseToString(int request) {
        switch(request) {
            case RIL_UNSOL_RESPONSE_RADIO_STATE_CHANGED:
                return "UNSOL_RESPONSE_RADIO_STATE_CHANGED";
            case RIL_UNSOL_RESPONSE_CALL_STATE_CHANGED:
                return "UNSOL_RESPONSE_CALL_STATE_CHANGED";
            case RIL_UNSOL_RESPONSE_NETWORK_STATE_CHANGED:
                return "UNSOL_RESPONSE_NETWORK_STATE_CHANGED";
            case RIL_UNSOL_RESPONSE_NEW_SMS:
                return "UNSOL_RESPONSE_NEW_SMS";
            case RIL_UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT:
                return "UNSOL_RESPONSE_NEW_SMS_STATUS_REPORT";
            case RIL_UNSOL_RESPONSE_NEW_SMS_ON_SIM:
                return "UNSOL_RESPONSE_NEW_SMS_ON_SIM";
            case RIL_UNSOL_ON_USSD:
                return "UNSOL_ON_USSD";
            case RIL_UNSOL_ON_USSD_REQUEST:
                return "UNSOL_ON_USSD_REQUEST";
            case RIL_UNSOL_NITZ_TIME_RECEIVED:
                return "UNSOL_NITZ_TIME_RECEIVED";
            case RIL_UNSOL_SIGNAL_STRENGTH:
                return "UNSOL_SIGNAL_STRENGTH";
            case RIL_UNSOL_DATA_CALL_LIST_CHANGED:
                return "UNSOL_DATA_CALL_LIST_CHANGED";
            case RIL_UNSOL_SUPP_SVC_NOTIFICATION:
                return "UNSOL_SUPP_SVC_NOTIFICATION";
            case RIL_UNSOL_STK_SESSION_END:
                return "UNSOL_STK_SESSION_END";
            case RIL_UNSOL_STK_PROACTIVE_COMMAND:
                return "UNSOL_STK_PROACTIVE_COMMAND";
            case RIL_UNSOL_STK_EVENT_NOTIFY:
                return "UNSOL_STK_EVENT_NOTIFY";
            case RIL_UNSOL_STK_CALL_SETUP:
                return "UNSOL_STK_CALL_SETUP";
            case RIL_UNSOL_SIM_SMS_STORAGE_FULL:
                return "UNSOL_SIM_SMS_STORAGE_FULL";
            case RIL_UNSOL_SIM_REFRESH:
                return "UNSOL_SIM_REFRESH";
            case RIL_UNSOL_CALL_RING:
                return "UNSOL_CALL_RING";
            case RIL_UNSOL_RESPONSE_SIM_STATUS_CHANGED:
                return "UNSOL_RESPONSE_SIM_STATUS_CHANGED";
            case RIL_UNSOL_RESPONSE_CDMA_NEW_SMS:
                return "UNSOL_RESPONSE_CDMA_NEW_SMS";
            case RIL_UNSOL_RESPONSE_NEW_BROADCAST_SMS:
                return "UNSOL_RESPONSE_NEW_BROADCAST_SMS";
            case RIL_UNSOL_CDMA_RUIM_SMS_STORAGE_FULL:
                return "UNSOL_CDMA_RUIM_SMS_STORAGE_FULL";
            case RIL_UNSOL_RESTRICTED_STATE_CHANGED:
                return "UNSOL_RESTRICTED_STATE_CHANGED";
            case RIL_UNSOL_ENTER_EMERGENCY_CALLBACK_MODE:
                return "UNSOL_ENTER_EMERGENCY_CALLBACK_MODE";
            case RIL_UNSOL_CDMA_CALL_WAITING:
                return "UNSOL_CDMA_CALL_WAITING";
            case RIL_UNSOL_CDMA_OTA_PROVISION_STATUS:
                return "UNSOL_CDMA_OTA_PROVISION_STATUS";
            case RIL_UNSOL_CDMA_INFO_REC:
                return "UNSOL_CDMA_INFO_REC";
            case RIL_UNSOL_OEM_HOOK_RAW:
                return "UNSOL_OEM_HOOK_RAW";
            case RIL_UNSOL_RINGBACK_TONE:
                return "UNSOL_RINGBACK_TONE";
            case RIL_UNSOL_RESEND_INCALL_MUTE:
                return "UNSOL_RESEND_INCALL_MUTE";
            case RIL_UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED:
                return "UNSOL_CDMA_SUBSCRIPTION_SOURCE_CHANGED";
            case RIL_UNSOl_CDMA_PRL_CHANGED:
                return "UNSOL_CDMA_PRL_CHANGED";
            case RIL_UNSOL_EXIT_EMERGENCY_CALLBACK_MODE:
                return "UNSOL_EXIT_EMERGENCY_CALLBACK_MODE";
            case RIL_UNSOL_RIL_CONNECTED:
                return "UNSOL_RIL_CONNECTED";
            case RIL_UNSOL_VOICE_RADIO_TECH_CHANGED:
                return "UNSOL_VOICE_RADIO_TECH_CHANGED";
            case RIL_UNSOL_CELL_INFO_LIST:
                return "UNSOL_CELL_INFO_LIST";
            case RIL_UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED:
                return "UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED";
            case RIL_UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED:
                return "UNSOL_UICC_SUBSCRIPTION_STATUS_CHANGED";
            case RIL_UNSOL_SRVCC_STATE_NOTIFY:
                return "UNSOL_SRVCC_STATE_NOTIFY";
            case RIL_UNSOL_HARDWARE_CONFIG_CHANGED:
                return "UNSOL_HARDWARE_CONFIG_CHANGED";
            case RIL_UNSOL_DC_RT_INFO_CHANGED:
                return "UNSOL_DC_RT_INFO_CHANGED";
            case RIL_UNSOL_RADIO_CAPABILITY:
                return "UNSOL_RADIO_CAPABILITY";
            case RIL_UNSOL_ON_SS:
                return "UNSOL_ON_SS";
            case RIL_UNSOL_STK_CC_ALPHA_NOTIFY:
                return "UNSOL_STK_CC_ALPHA_NOTIFY";
            case RIL_UNSOL_LCEDATA_RECV:
                return "UNSOL_LCE_INFO_RECV";
            case RIL_UNSOL_PCO_DATA:
                return "UNSOL_PCO_DATA";
            case RIL_UNSOL_MODEM_RESTART:
                return "UNSOL_MODEM_RESTART";
            case RIL_UNSOL_CARRIER_INFO_IMSI_ENCRYPTION:
                return "UNSOL_CARRIER_INFO_IMSI_ENCRYPTION";
            case RIL_UNSOL_NETWORK_SCAN_RESULT:
                return "UNSOL_NETWORK_SCAN_RESULT";
            case RIL_UNSOL_KEEPALIVE_STATUS:
                return "UNSOL_KEEPALIVE_STATUS";
            case RIL_UNSOL_UNTHROTTLE_APN:
                return "UNSOL_UNTHROTTLE_APN";
            case RIL_UNSOL_ICC_SLOT_STATUS:
                return "UNSOL_ICC_SLOT_STATUS";
            case RIL_UNSOL_PHYSICAL_CHANNEL_CONFIG:
                return "UNSOL_PHYSICAL_CHANNEL_CONFIG";
            case RIL_UNSOL_EMERGENCY_NUMBER_LIST:
                return "UNSOL_EMERGENCY_NUMBER_LIST";
            case RIL_UNSOL_UICC_APPLICATIONS_ENABLEMENT_CHANGED:
                return "UNSOL_UICC_APPLICATIONS_ENABLEMENT_CHANGED";
            case RIL_UNSOL_REGISTRATION_FAILED:
                return "UNSOL_REGISTRATION_FAILED";
            case RIL_UNSOL_BARRING_INFO_CHANGED:
                return "UNSOL_BARRING_INFO_CHANGED";
            case RIL_UNSOL_RESPONSE_SIM_PHONEBOOK_CHANGED:
                return "UNSOL_RESPONSE_SIM_PHONEBOOK_CHANGED";
            case RIL_UNSOL_RESPONSE_SIM_PHONEBOOK_RECORDS_RECEIVED:
                return "UNSOL_RESPONSE_SIM_PHONEBOOK_RECORDS_RECEIVED";
            default:
                return "<unknown response>";
        }
    }

    @UnsupportedAppUsage
    void riljLog(String msg) {
        Rlog.d(RILJ_LOG_TAG, msg + (" [PHONE" + mPhoneId + "]"));
    }

    void riljLoge(String msg) {
        Rlog.e(RILJ_LOG_TAG, msg + (" [PHONE" + mPhoneId + "]"));
    }

    void riljLoge(String msg, Exception e) {
        Rlog.e(RILJ_LOG_TAG, msg + (" [PHONE" + mPhoneId + "]"), e);
    }

    void riljLogv(String msg) {
        Rlog.v(RILJ_LOG_TAG, msg + (" [PHONE" + mPhoneId + "]"));
    }

    @UnsupportedAppUsage
    void unsljLog(int response) {
        riljLog("[UNSL]< " + responseToString(response));
    }

    @UnsupportedAppUsage
    void unsljLogMore(int response, String more) {
        riljLog("[UNSL]< " + responseToString(response) + " " + more);
    }

    @UnsupportedAppUsage
    void unsljLogRet(int response, Object ret) {
        riljLog("[UNSL]< " + responseToString(response) + " " + retToString(response, ret));
    }

    @UnsupportedAppUsage
    void unsljLogvRet(int response, Object ret) {
        riljLogv("[UNSL]< " + responseToString(response) + " " + retToString(response, ret));
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
                pw.println("  [" + rr.mSerial + "] " + requestToString(rr.mRequest));
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

    /** Append the data to the end of an ArrayList */
    public static void appendPrimitiveArrayToArrayList(byte[] src, ArrayList<Byte> dst) {
        for (byte b : src) {
            dst.add(b);
        }
    }

    public static ArrayList<Byte> primitiveArrayToArrayList(byte[] arr) {
        ArrayList<Byte> arrayList = new ArrayList<>(arr.length);
        for (byte b : arr) {
            arrayList.add(b);
        }
        return arrayList;
    }

    /** Convert a primitive int array to an ArrayList<Integer>. */
    public static ArrayList<Integer> primitiveArrayToArrayList(int[] arr) {
        ArrayList<Integer> arrayList = new ArrayList<>(arr.length);
        for (int i : arr) {
            arrayList.add(i);
        }
        return arrayList;
    }

    /** Convert an ArrayList of Bytes to an exactly-sized primitive array */
    public static byte[] arrayListToPrimitiveArray(ArrayList<Byte> bytes) {
        byte[] ret = new byte[bytes.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = bytes.get(i);
        }
        return ret;
    }

    static ArrayList<HardwareConfig> convertHalHwConfigList(
            ArrayList<android.hardware.radio.V1_0.HardwareConfig> hwListRil,
            RIL ril) {
        int num;
        ArrayList<HardwareConfig> response;
        HardwareConfig hw;

        num = hwListRil.size();
        response = new ArrayList<HardwareConfig>(num);

        if (RILJ_LOGV) {
            ril.riljLog("convertHalHwConfigList: num=" + num);
        }
        for (android.hardware.radio.V1_0.HardwareConfig hwRil : hwListRil) {
            int type = hwRil.type;
            switch(type) {
                case HardwareConfig.DEV_HARDWARE_TYPE_MODEM: {
                    hw = new HardwareConfig(type);
                    HardwareConfigModem hwModem = hwRil.modem.get(0);
                    hw.assignModem(hwRil.uuid, hwRil.state, hwModem.rilModel, hwModem.rat,
                            hwModem.maxVoice, hwModem.maxData, hwModem.maxStandby);
                    break;
                }
                case HardwareConfig.DEV_HARDWARE_TYPE_SIM: {
                    hw = new HardwareConfig(type);
                    hw.assignSim(hwRil.uuid, hwRil.state, hwRil.sim.get(0).modemUuid);
                    break;
                }
                default: {
                    throw new RuntimeException(
                            "RIL_REQUEST_GET_HARDWARE_CONFIG invalid hardward type:" + type);
                }
            }

            response.add(hw);
        }

        return response;
    }

    static RadioCapability convertHalRadioCapability(
            android.hardware.radio.V1_0.RadioCapability rcRil, RIL ril) {
        int session = rcRil.session;
        int phase = rcRil.phase;
        // convert to public bitmask {@link TelephonyManager.NetworkTypeBitMask}
        int rat = convertToNetworkTypeBitMask(rcRil.raf);
        String logicModemUuid = rcRil.logicalModemUuid;
        int status = rcRil.status;

        ril.riljLog("convertHalRadioCapability: session=" + session +
                ", phase=" + phase +
                ", rat=" + rat +
                ", logicModemUuid=" + logicModemUuid +
                ", status=" + status + ", rcRil.raf=" + rcRil.raf);
        RadioCapability rc = new RadioCapability(
                ril.mPhoneId, session, phase, rat, logicModemUuid, status);
        return rc;
    }

    static List<LinkCapacityEstimate> convertHalLceData(LceDataInfo halData, RIL ril) {
        final List<LinkCapacityEstimate> lceList = new ArrayList<>();
        lceList.add(new LinkCapacityEstimate(LinkCapacityEstimate.LCE_TYPE_COMBINED,
                halData.lastHopCapacityKbps,
                LinkCapacityEstimate.INVALID));
        ril.riljLog("LCE capacity information received:" + lceList);
        return lceList;
    }

    static List<LinkCapacityEstimate> convertHalLceData(
            android.hardware.radio.V1_2.LinkCapacityEstimate halData, RIL ril) {
        final List<LinkCapacityEstimate> lceList = new ArrayList<>();
        lceList.add(new LinkCapacityEstimate(LinkCapacityEstimate.LCE_TYPE_COMBINED,
                halData.downlinkCapacityKbps,
                halData.uplinkCapacityKbps));
        ril.riljLog("LCE capacity information received:" + lceList);
        return lceList;
    }

    static List<LinkCapacityEstimate> convertHalLceData(
            android.hardware.radio.V1_6.LinkCapacityEstimate halData, RIL ril) {
        final List<LinkCapacityEstimate> lceList = new ArrayList<>();
        int primaryDownlinkCapacityKbps = halData.downlinkCapacityKbps;
        int primaryUplinkCapacityKbps = halData.uplinkCapacityKbps;
        if (primaryDownlinkCapacityKbps != LinkCapacityEstimate.INVALID
                && halData.secondaryDownlinkCapacityKbps != LinkCapacityEstimate.INVALID) {
            primaryDownlinkCapacityKbps =
                    halData.downlinkCapacityKbps - halData.secondaryDownlinkCapacityKbps;
        }
        if (primaryUplinkCapacityKbps != LinkCapacityEstimate.INVALID
                && halData.secondaryUplinkCapacityKbps != LinkCapacityEstimate.INVALID) {
            primaryUplinkCapacityKbps =
                    halData.uplinkCapacityKbps - halData.secondaryUplinkCapacityKbps;
        }

        lceList.add(new LinkCapacityEstimate(LinkCapacityEstimate.LCE_TYPE_PRIMARY,
                primaryDownlinkCapacityKbps,
                primaryUplinkCapacityKbps));
        lceList.add(new LinkCapacityEstimate(LinkCapacityEstimate.LCE_TYPE_SECONDARY,
                halData.secondaryDownlinkCapacityKbps,
                halData.secondaryUplinkCapacityKbps));
        ril.riljLog("LCE capacity information received:" + lceList);
        return lceList;
    }

    /**
     * Convert CellInfo defined in 1.0/types.hal to CellInfo type.
     * @param records List of CellInfo defined in 1.0/types.hal
     * @return List of converted CellInfo object
     */
    @VisibleForTesting
    public static ArrayList<CellInfo> convertHalCellInfoList(
            ArrayList<android.hardware.radio.V1_0.CellInfo> records) {
        ArrayList<CellInfo> response = new ArrayList<CellInfo>(records.size());

        final long nanotime = SystemClock.elapsedRealtimeNanos();
        for (android.hardware.radio.V1_0.CellInfo record : records) {
            record.timeStamp = nanotime;
            response.add(CellInfo.create(record));
        }

        return response;
    }

    /**
     * Convert CellInfo defined in 1.2/types.hal to CellInfo type.
     * @param records List of CellInfo defined in 1.2/types.hal
     * @return List of converted CellInfo object
     */
    @VisibleForTesting
    public static ArrayList<CellInfo> convertHalCellInfoList_1_2(
            ArrayList<android.hardware.radio.V1_2.CellInfo> records) {
        ArrayList<CellInfo> response = new ArrayList<CellInfo>(records.size());

        final long nanotime = SystemClock.elapsedRealtimeNanos();
        for (android.hardware.radio.V1_2.CellInfo record : records) {
            record.timeStamp = nanotime;
            response.add(CellInfo.create(record));
        }
        return response;
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
     * Convert CellInfo defined in 1.4/types.hal to CellInfo type.
     * @param records List of CellInfo defined in 1.4/types.hal.
     * @return List of converted CellInfo object.
     */
    @VisibleForTesting
    public static ArrayList<CellInfo> convertHalCellInfoList_1_4(
            ArrayList<android.hardware.radio.V1_4.CellInfo> records) {
        ArrayList<CellInfo> response = new ArrayList<CellInfo>(records.size());

        final long nanotime = SystemClock.elapsedRealtimeNanos();
        for (android.hardware.radio.V1_4.CellInfo record : records) {
            response.add(CellInfo.create(record, nanotime));
        }
        return response;
    }

    /**
     * Convert CellInfo defined in 1.5/types.hal to CellInfo type.
     * @param records List of CellInfo defined in 1.5/types.hal.
     * @return List of converted CellInfo object.
     */
    @VisibleForTesting
    public static ArrayList<CellInfo> convertHalCellInfoList_1_5(
            ArrayList<android.hardware.radio.V1_5.CellInfo> records) {
        ArrayList<CellInfo> response = new ArrayList<>(records.size());

        final long nanotime = SystemClock.elapsedRealtimeNanos();
        for (android.hardware.radio.V1_5.CellInfo record : records) {
            response.add(CellInfo.create(record, nanotime));
        }
        return response;
    }

    /**
     * Convert CellInfo defined in 1.6/types.hal to CellInfo type.
     * @param records List of CellInfo defined in 1.6/types.hal.
     * @return List of converted CellInfo object.
     */
    @VisibleForTesting
    public static ArrayList<CellInfo> convertHalCellInfoList_1_6(
            ArrayList<android.hardware.radio.V1_6.CellInfo> records) {
        ArrayList<CellInfo> response = new ArrayList<>(records.size());

        final long nanotime = SystemClock.elapsedRealtimeNanos();
        for (android.hardware.radio.V1_6.CellInfo record : records) {
            response.add(CellInfo.create(record, nanotime));
        }
        return response;
    }

    private static LinkAddress createLinkAddressFromString(String addressString) {
        return createLinkAddressFromString(addressString, 0, LinkAddress.LIFETIME_UNKNOWN,
                LinkAddress.LIFETIME_UNKNOWN);
    }

    private static LinkAddress createLinkAddressFromString(String addressString, int properties,
            long deprecationTime, long expirationTime) {
        addressString = addressString.trim();
        InetAddress address = null;
        int prefixLength = -1;
        try {
            String[] pieces = addressString.split("/", 2);
            address = InetAddresses.parseNumericAddress(pieces[0]);
            if (pieces.length == 1) {
                prefixLength = (address instanceof Inet4Address) ? 32 : 128;
            } else if (pieces.length == 2) {
                prefixLength = Integer.parseInt(pieces[1]);
            }
        } catch (NullPointerException e) {            // Null string.
        } catch (ArrayIndexOutOfBoundsException e) {  // No prefix length.
        } catch (NumberFormatException e) {           // Non-numeric prefix.
        } catch (IllegalArgumentException e) {        // Invalid IP address.
        }

        if (address == null || prefixLength == -1) {
            throw new IllegalArgumentException("Invalid link address " + addressString);
        }

        return new LinkAddress(address, prefixLength, properties, 0,
                deprecationTime, expirationTime);
    }

    /**
     * Convert SetupDataCallResult defined in 1.0, 1.4, 1.5 or 1.6 types.hal into DataCallResponse
     * @param dcResult setup data call result
     * @return converted DataCallResponse object
     */
    @VisibleForTesting
    public static DataCallResponse convertDataCallResult(Object dcResult) {
        if (dcResult == null) return null;

        int cause, cid, active, mtu, mtuV4, mtuV6;
        long suggestedRetryTime;
        String ifname;
        int protocolType;
        String[] addresses = null;
        String[] dnses = null;
        String[] gateways = null;
        String[] pcscfs = null;
        Qos defaultQos = null;

        @HandoverFailureMode
        int handoverFailureMode = DataCallResponse.HANDOVER_FAILURE_MODE_LEGACY;

        int pduSessionId = DataCallResponse.PDU_SESSION_ID_NOT_SET;

        List<LinkAddress> laList = new ArrayList<>();
        List<QosBearerSession> qosSessions = new ArrayList<>();
        NetworkSliceInfo sliceInfo = null;
        List<TrafficDescriptor> trafficDescriptors = new ArrayList<>();

        if (dcResult instanceof android.hardware.radio.V1_0.SetupDataCallResult) {
            final android.hardware.radio.V1_0.SetupDataCallResult result =
                    (android.hardware.radio.V1_0.SetupDataCallResult) dcResult;
            cause = result.status;
            suggestedRetryTime = result.suggestedRetryTime;
            cid = result.cid;
            active = result.active;
            protocolType = ApnSetting.getProtocolIntFromString(result.type);
            ifname = result.ifname;
            if (!TextUtils.isEmpty(result.addresses)) {
                addresses = result.addresses.split("\\s+");
            }
            if (!TextUtils.isEmpty(result.dnses)) {
                dnses = result.dnses.split("\\s+");
            }
            if (!TextUtils.isEmpty(result.gateways)) {
                gateways = result.gateways.split("\\s+");
            }
            if (!TextUtils.isEmpty(result.pcscf)) {
                pcscfs = result.pcscf.split("\\s+");
            }
            mtu = mtuV4 = mtuV6 = result.mtu;
            if (addresses != null) {
                for (String address : addresses) {
                    laList.add(createLinkAddressFromString(address));
                }
            }
        } else if (dcResult instanceof android.hardware.radio.V1_4.SetupDataCallResult) {
            final android.hardware.radio.V1_4.SetupDataCallResult result =
                    (android.hardware.radio.V1_4.SetupDataCallResult) dcResult;
            cause = result.cause;
            suggestedRetryTime = result.suggestedRetryTime;
            cid = result.cid;
            active = result.active;
            protocolType = result.type;
            ifname = result.ifname;
            addresses = result.addresses.stream().toArray(String[]::new);
            dnses = result.dnses.stream().toArray(String[]::new);
            gateways = result.gateways.stream().toArray(String[]::new);
            pcscfs = result.pcscf.stream().toArray(String[]::new);
            mtu = mtuV4 = mtuV6 = result.mtu;
            if (addresses != null) {
                for (String address : addresses) {
                    laList.add(createLinkAddressFromString(address));
                }
            }
        } else if (dcResult instanceof android.hardware.radio.V1_5.SetupDataCallResult) {
            final android.hardware.radio.V1_5.SetupDataCallResult result =
                    (android.hardware.radio.V1_5.SetupDataCallResult) dcResult;
            cause = result.cause;
            suggestedRetryTime = result.suggestedRetryTime;
            cid = result.cid;
            active = result.active;
            protocolType = result.type;
            ifname = result.ifname;
            laList = result.addresses.stream().map(la -> createLinkAddressFromString(
                    la.address, la.properties, la.deprecationTime, la.expirationTime))
                    .collect(Collectors.toList());

            dnses = result.dnses.stream().toArray(String[]::new);
            gateways = result.gateways.stream().toArray(String[]::new);
            pcscfs = result.pcscf.stream().toArray(String[]::new);
            mtu = Math.max(result.mtuV4, result.mtuV6);
            mtuV4 = result.mtuV4;
            mtuV6 = result.mtuV6;
        } else if (dcResult instanceof android.hardware.radio.V1_6.SetupDataCallResult) {
            final android.hardware.radio.V1_6.SetupDataCallResult result =
                    (android.hardware.radio.V1_6.SetupDataCallResult) dcResult;
            cause = result.cause;
            suggestedRetryTime = result.suggestedRetryTime;
            cid = result.cid;
            active = result.active;
            protocolType = result.type;
            ifname = result.ifname;
            laList = result.addresses.stream().map(la -> createLinkAddressFromString(
                    la.address, la.properties, la.deprecationTime, la.expirationTime))
                    .collect(Collectors.toList());
            dnses = result.dnses.stream().toArray(String[]::new);
            gateways = result.gateways.stream().toArray(String[]::new);
            pcscfs = result.pcscf.stream().toArray(String[]::new);
            mtu = Math.max(result.mtuV4, result.mtuV6);
            mtuV4 = result.mtuV4;
            mtuV6 = result.mtuV6;
            handoverFailureMode = result.handoverFailureMode;
            pduSessionId = result.pduSessionId;
            defaultQos = Qos.create(result.defaultQos);
            qosSessions = result.qosSessions.stream().map(session ->
                    QosBearerSession.create(session)).collect(Collectors.toList());
            sliceInfo = convertToSliceInfo(result.sliceInfo);
            trafficDescriptors = result.trafficDescriptors.stream().map(td ->
                    convertToTrafficDescriptor(td)).collect(Collectors.toList());
        } else {
            Rlog.e(RILJ_LOG_TAG, "Unsupported SetupDataCallResult " + dcResult);
            return null;
        }

        // Process dns
        List<InetAddress> dnsList = new ArrayList<>();
        if (dnses != null) {
            for (String dns : dnses) {
                dns = dns.trim();
                InetAddress ia;
                try {
                    ia = InetAddresses.parseNumericAddress(dns);
                    dnsList.add(ia);
                } catch (IllegalArgumentException e) {
                    Rlog.e(RILJ_LOG_TAG, "Unknown dns: " + dns, e);
                }
            }
        }

        // Process gateway
        List<InetAddress> gatewayList = new ArrayList<>();
        if (gateways != null) {
            for (String gateway : gateways) {
                gateway = gateway.trim();
                InetAddress ia;
                try {
                    ia = InetAddresses.parseNumericAddress(gateway);
                    gatewayList.add(ia);
                } catch (IllegalArgumentException e) {
                    Rlog.e(RILJ_LOG_TAG, "Unknown gateway: " + gateway, e);
                }
            }
        }

        // Process gateway
        List<InetAddress> pcscfList = new ArrayList<>();
        if (pcscfs != null) {
            for (String pcscf : pcscfs) {
                pcscf = pcscf.trim();
                InetAddress ia;
                try {
                    ia = InetAddresses.parseNumericAddress(pcscf);
                    pcscfList.add(ia);
                } catch (IllegalArgumentException e) {
                    Rlog.e(RILJ_LOG_TAG, "Unknown pcscf: " + pcscf, e);
                }
            }
        }

        return new DataCallResponse.Builder()
                .setCause(cause)
                .setRetryDurationMillis(suggestedRetryTime)
                .setId(cid)
                .setLinkStatus(active)
                .setProtocolType(protocolType)
                .setInterfaceName(ifname)
                .setAddresses(laList)
                .setDnsAddresses(dnsList)
                .setGatewayAddresses(gatewayList)
                .setPcscfAddresses(pcscfList)
                .setMtu(mtu)
                .setMtuV4(mtuV4)
                .setMtuV6(mtuV6)
                .setHandoverFailureMode(handoverFailureMode)
                .setPduSessionId(pduSessionId)
                .setDefaultQos(defaultQos)
                .setQosBearerSessions(qosSessions)
                .setSliceInfo(sliceInfo)
                .setTrafficDescriptors(trafficDescriptors)
                .build();
    }

    private static NetworkSliceInfo convertToSliceInfo(OptionalSliceInfo optionalSliceInfo) {
        if (optionalSliceInfo.getDiscriminator() == OptionalSliceInfo.hidl_discriminator.noinit) {
            return null;
        }

        android.hardware.radio.V1_6.SliceInfo si = optionalSliceInfo.value();
        NetworkSliceInfo.Builder builder =
                new NetworkSliceInfo.Builder()
                .setSliceServiceType(si.sst)
                .setMappedHplmnSliceServiceType(si.mappedHplmnSst);
        if (si.sliceDifferentiator != NetworkSliceInfo.SLICE_DIFFERENTIATOR_NO_SLICE) {
            builder
                .setSliceDifferentiator(si.sliceDifferentiator)
                .setMappedHplmnSliceDifferentiator(si.mappedHplmnSD);
        }
        return builder.build();
    }

    private static TrafficDescriptor convertToTrafficDescriptor(
            android.hardware.radio.V1_6.TrafficDescriptor td) {
        String dnn = td.dnn.getDiscriminator() == OptionalDnn.hidl_discriminator.noinit
                ? null : td.dnn.value();
        byte[] osAppId = td.osAppId.getDiscriminator() == OptionalOsAppId.hidl_discriminator.noinit
                ? null : arrayListToPrimitiveArray(td.osAppId.value().osAppId);
        TrafficDescriptor.Builder builder = new TrafficDescriptor.Builder();
        if (dnn != null) {
            builder.setDataNetworkName(dnn);
        }
        if (osAppId != null) {
            builder.setOsAppId(osAppId);
        }
        return builder.build();
    }

    /**
     * Convert SetupDataCallResult defined in 1.0 or 1.4/types.hal into DataCallResponse
     * @param dataCallResultList List of SetupDataCallResult defined in 1.0 or 1.4/types.hal
     * @return List of converted DataCallResponse object
     */
    @VisibleForTesting
    public static ArrayList<DataCallResponse> convertDataCallResultList(
            List<? extends Object> dataCallResultList) {
        ArrayList<DataCallResponse> response =
                new ArrayList<DataCallResponse>(dataCallResultList.size());

        for (Object obj : dataCallResultList) {
            response.add(convertDataCallResult(obj));
        }
        return response;
    }

    /**
     * Get the HAL version.
     *
     * @return the current HalVersion
     */
    public HalVersion getHalVersion() {
        return mRadioVersion;
    }
}
