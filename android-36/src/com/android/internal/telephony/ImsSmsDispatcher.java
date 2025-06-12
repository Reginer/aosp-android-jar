/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.internal.telephony;

import android.app.Activity;
import android.content.Context;
import android.os.Binder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.provider.Telephony.Sms.Intents;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.RegistrationManager;
import android.telephony.ims.aidl.IImsSmsListener;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.telephony.ims.stub.ImsSmsImplBase;
import android.telephony.ims.stub.ImsSmsImplBase.SendStatusResult;

import com.android.ims.FeatureConnector;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.GsmAlphabet.TextEncodingDetails;
import com.android.internal.telephony.analytics.TelephonyAnalytics;
import com.android.internal.telephony.analytics.TelephonyAnalytics.SmsMmsAnalytics;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.util.SMSDispatcherUtil;
import com.android.telephony.Rlog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Responsible for communications with {@link com.android.ims.ImsManager} to send/receive messages
 * over IMS.
 * @hide
 */
public class ImsSmsDispatcher extends SMSDispatcher {

    private static final String TAG = "ImsSmsDispatcher";
    private static final int CONNECT_DELAY_MS = 5000; // 5 seconds;
    public static final int MAX_SEND_RETRIES_OVER_IMS = MAX_SEND_RETRIES;

    /**
     * Creates FeatureConnector instances for ImsManager, used during testing to inject mock
     * connector instances.
     */
    @VisibleForTesting
    public interface FeatureConnectorFactory {
        /**
         * Create a new FeatureConnector for ImsManager.
         */
        FeatureConnector<ImsManager> create(Context context, int phoneId, String logPrefix,
                FeatureConnector.Listener<ImsManager> listener, Executor executor);
    }

    public List<Integer> mMemoryAvailableNotifierList = new ArrayList<Integer>();
    @VisibleForTesting
    public Map<Integer, SmsTracker> mTrackers = new ConcurrentHashMap<>();
    @VisibleForTesting
    public AtomicInteger mNextToken = new AtomicInteger();
    private final Object mLock = new Object();
    private volatile boolean mIsSmsCapable;
    private volatile boolean mIsImsServiceUp;
    private volatile boolean mIsRegistered;
    private final FeatureConnector<ImsManager> mImsManagerConnector;
    /** Telephony metrics instance for logging metrics event */
    private TelephonyMetrics mMetrics = TelephonyMetrics.getInstance();
    private ImsManager mImsManager;
    private FeatureConnectorFactory mConnectorFactory;

    private Runnable mConnectRunnable = new Runnable() {
        @Override
        public void run() {
            mImsManagerConnector.connect();
        }
    };

    /**
     * Listen to the IMS service state change
     *
     */
    private RegistrationManager.RegistrationCallback mRegistrationCallback =
            new RegistrationManager.RegistrationCallback() {
                @Override
                public void onRegistered(
                        @ImsRegistrationImplBase.ImsRegistrationTech int imsRadioTech) {
                    logd("onImsConnected imsRadioTech=" + imsRadioTech);
                    synchronized (mLock) {
                        mIsRegistered = true;
                    }
                }

                @Override
                public void onRegistering(
                        @ImsRegistrationImplBase.ImsRegistrationTech int imsRadioTech) {
                    logd("onImsProgressing imsRadioTech=" + imsRadioTech);
                    synchronized (mLock) {
                        mIsRegistered = false;
                    }
                }

                @Override
                public void onUnregistered(ImsReasonInfo info) {
                    logd("onImsDisconnected imsReasonInfo=" + info);
                    synchronized (mLock) {
                        mIsRegistered = false;
                    }
                }
            };

    private android.telephony.ims.ImsMmTelManager.CapabilityCallback mCapabilityCallback =
            new android.telephony.ims.ImsMmTelManager.CapabilityCallback() {
                @Override
                public void onCapabilitiesStatusChanged(
                        MmTelFeature.MmTelCapabilities capabilities) {
                    synchronized (mLock) {
                        mIsSmsCapable = capabilities.isCapable(
                                MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_SMS);
                    }
                }
    };

    private final IImsSmsListener mImsSmsListener = new IImsSmsListener.Stub() {
        @Override
        public void onMemoryAvailableResult(int token, @SendStatusResult int status,
                int networkReasonCode) {
            final long identity = Binder.clearCallingIdentity();
            try {
                logd("onMemoryAvailableResult token=" + token + " status=" + status
                        + " networkReasonCode=" + networkReasonCode);
                if (!mMemoryAvailableNotifierList.contains(token)) {
                    loge("onMemoryAvailableResult Invalid token");
                    return;
                }
                mMemoryAvailableNotifierList.remove(Integer.valueOf(token));

                /**
                 * The Retrans flag is set and reset As per section 6.3.3.1.2 in TS 124011
                 * Note: Assuming that SEND_STATUS_ERROR_RETRY is sent in case of temporary failure
                 */
                if (status ==  ImsSmsImplBase.SEND_STATUS_ERROR_RETRY) {
                    if (!mRPSmmaRetried) {
                        sendMessageDelayed(obtainMessage(EVENT_RETRY_SMMA), SEND_RETRY_DELAY);
                        mRPSmmaRetried = true;
                    } else {
                        mRPSmmaRetried = false;
                    }
                } else {
                    mRPSmmaRetried = false;
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
        @Override
        public void onSendSmsResult(int token, int messageRef, @SendStatusResult int status,
                @SmsManager.Result int reason, int networkReasonCode) {
            final long identity = Binder.clearCallingIdentity();
            try {
                logd("onSendSmsResult token=" + token + " messageRef=" + messageRef
                        + " status=" + status + " reason=" + reason + " networkReasonCode="
                        + networkReasonCode);
                // TODO integrate networkReasonCode into IMS SMS metrics.
                SmsTracker tracker = mTrackers.get(token);
                mMetrics.writeOnImsServiceSmsSolicitedResponse(mPhone.getPhoneId(), status, reason,
                        (tracker != null ? tracker.mMessageId : 0L));
                if (tracker == null) {
                    throw new IllegalArgumentException("Invalid token.");
                }
                tracker.mMessageRef = messageRef;
                switch(status) {
                    case ImsSmsImplBase.SEND_STATUS_OK:
                        if (tracker.mDeliveryIntent != null) {
                            // Expecting a status report. Put this tracker to the map.
                            mSmsDispatchersController.putDeliveryPendingTracker(tracker);
                        }
                        tracker.onSent(mContext);
                        mTrackers.remove(token);
                        mPhone.notifySmsSent(tracker.mDestAddress);
                        mSmsDispatchersController.notifySmsSent(tracker, true,
                                tracker.isSinglePartOrLastPart(), true /*success*/);
                        break;
                    case ImsSmsImplBase.SEND_STATUS_ERROR:
                        tracker.onFailed(mContext, reason, networkReasonCode);
                        mTrackers.remove(token);
                        notifySmsSentFailedToEmergencyStateTracker(tracker, true);
                        break;
                    case ImsSmsImplBase.SEND_STATUS_ERROR_RETRY:
                        int maxRetryCountOverIms = getMaxRetryCountOverIms();
                        if (tracker.mRetryCount < getMaxSmsRetryCount()) {
                            if (maxRetryCountOverIms < getMaxSmsRetryCount()
                                    && tracker.mRetryCount >= maxRetryCountOverIms) {
                                tracker.mRetryCount += 1;
                                mTrackers.remove(token);
                                fallbackToPstn(tracker);
                                break;
                            }
                            tracker.mRetryCount += 1;
                            sendMessageDelayed(
                                    obtainMessage(EVENT_SEND_RETRY, tracker),
                                    getSmsRetryDelayValue());
                        } else {
                            tracker.onFailed(mContext, reason, networkReasonCode);
                            mTrackers.remove(token);
                            notifySmsSentFailedToEmergencyStateTracker(tracker, true);
                        }
                        break;
                    case ImsSmsImplBase.SEND_STATUS_ERROR_FALLBACK:
                        // Skip MAX_SEND_RETRIES checking here. It allows CSFB after
                        // SEND_STATUS_ERROR_RETRY up to MAX_SEND_RETRIES even.
                        tracker.mRetryCount += 1;
                        mTrackers.remove(token);
                        fallbackToPstn(tracker);
                        break;
                    default:
                }
                mPhone.getSmsStats().onOutgoingSms(
                        true /* isOverIms */,
                        SmsConstants.FORMAT_3GPP2.equals(getFormat()),
                        status == ImsSmsImplBase.SEND_STATUS_ERROR_FALLBACK,
                        reason,
                        networkReasonCode,
                        tracker.mMessageId,
                        tracker.isFromDefaultSmsApplication(mContext),
                        tracker.getInterval(),
                        mTelephonyManager.isEmergencyNumber(tracker.mDestAddress),
                        tracker.isMtSmsPollingMessage(mContext));
                if (mPhone != null) {
                    TelephonyAnalytics telephonyAnalytics = mPhone.getTelephonyAnalytics();
                    if (telephonyAnalytics != null) {
                        SmsMmsAnalytics smsMmsAnalytics = telephonyAnalytics.getSmsMmsAnalytics();
                        if (smsMmsAnalytics != null) {
                            smsMmsAnalytics.onOutgoingSms(
                                    true /* isOverIms */,
                                    reason);
                        }
                    }
                }

            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void onSmsStatusReportReceived(int token, String format, byte[] pdu)
                throws RemoteException {
            final long identity = Binder.clearCallingIdentity();
            try {
                logd("Status report received.");
                android.telephony.SmsMessage message =
                        android.telephony.SmsMessage.createFromPdu(pdu, format);
                if (message == null || message.mWrappedSmsMessage == null) {
                    throw new RemoteException(
                            "Status report received with a PDU that could not be parsed.");
                }
                mSmsDispatchersController.handleSmsStatusReport(format, pdu);
                try {
                    getImsManager().acknowledgeSmsReport(
                            token,
                            message.mWrappedSmsMessage.mMessageRef,
                            ImsSmsImplBase.STATUS_REPORT_STATUS_OK);
                } catch (ImsException e) {
                    loge("Failed to acknowledgeSmsReport(). Error: " + e.getMessage());
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void onSmsReceived(int token, String format, byte[] pdu) {
            final long identity = Binder.clearCallingIdentity();
            try {
                logd("SMS received.");
                android.telephony.SmsMessage message =
                        android.telephony.SmsMessage.createFromPdu(pdu, format);
                mSmsDispatchersController.injectSmsPdu(message, format, result -> {
                    logd("SMS handled result: " + result);
                    int mappedResult;
                    switch (result) {
                        case Intents.RESULT_SMS_HANDLED:
                            mappedResult = ImsSmsImplBase.DELIVER_STATUS_OK;
                            if (message != null) {
                                mSmsDispatchersController
                                        .notifySmsReceivedViaImsToEmergencyStateTracker(
                                                message.getOriginatingAddress());
                            }
                            break;
                        case Intents.RESULT_SMS_OUT_OF_MEMORY:
                            mappedResult = ImsSmsImplBase.DELIVER_STATUS_ERROR_NO_MEMORY;
                            break;
                        case Intents.RESULT_SMS_UNSUPPORTED:
                            mappedResult =
                                    ImsSmsImplBase.DELIVER_STATUS_ERROR_REQUEST_NOT_SUPPORTED;
                            break;
                        case Activity.RESULT_OK:
                            // class2 message saving to SIM operation is in progress, defer ack
                            // until saving to SIM is success/failure
                            return;
                        default:
                            mappedResult = ImsSmsImplBase.DELIVER_STATUS_ERROR_GENERIC;
                            break;
                    }
                    try {
                        if (message != null && message.mWrappedSmsMessage != null) {
                            getImsManager().acknowledgeSms(token,
                                    message.mWrappedSmsMessage.mMessageRef, mappedResult);
                        } else {
                            logw("SMS Received with a PDU that could not be parsed.");
                            getImsManager().acknowledgeSms(token, 0, mappedResult);
                        }
                    } catch (ImsException e) {
                        loge("Failed to acknowledgeSms(). Error: " + e.getMessage());
                    }
                }, true /* ignoreClass */, true /* isOverIms */, token);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    };

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_SEND_RETRY:
                logd("SMS retry..");
                sendSms((SmsTracker) msg.obj);
                break;
            case EVENT_RETRY_SMMA:
                logd("SMMA Notification retry..");
                onMemoryAvailable();
                break;
            default:
                super.handleMessage(msg);
        }
    }

    public ImsSmsDispatcher(Phone phone, SmsDispatchersController smsDispatchersController,
            FeatureConnectorFactory factory) {
        super(phone, smsDispatchersController);
        mConnectorFactory = factory;

        mImsManagerConnector = mConnectorFactory.create(mContext, mPhone.getPhoneId(), TAG,
                new FeatureConnector.Listener<ImsManager>() {
                    public void connectionReady(ImsManager manager, int subId) throws ImsException {
                        logd("ImsManager: connection ready.");
                        synchronized (mLock) {
                            mImsManager = manager;
                            setListeners();
                            mIsImsServiceUp = true;

                            /* set ImsManager */
                            mSmsDispatchersController.setImsManager(mImsManager);
                        }
                    }

                    @Override
                    public void connectionUnavailable(int reason) {
                        logd("ImsManager: connection unavailable, reason=" + reason);
                        if (reason == FeatureConnector.UNAVAILABLE_REASON_SERVER_UNAVAILABLE) {
                            loge("connectionUnavailable: unexpected, received server error");
                            removeCallbacks(mConnectRunnable);
                            postDelayed(mConnectRunnable, CONNECT_DELAY_MS);
                        }
                        synchronized (mLock) {
                            mImsManager = null;
                            mIsImsServiceUp = false;

                            /* unset ImsManager */
                            mSmsDispatchersController.setImsManager(null);
                        }
                    }
                }, this::post);
        post(mConnectRunnable);
    }

    private void setListeners() throws ImsException {
        getImsManager().addRegistrationCallback(mRegistrationCallback, this::post);
        getImsManager().addCapabilitiesCallback(mCapabilityCallback, this::post);
        getImsManager().setSmsListener(getSmsListener());
        getImsManager().onSmsReady();
    }

    private boolean isLteService() {
        return ((mPhone.getServiceState().getRilDataRadioTechnology() ==
            ServiceState.RIL_RADIO_TECHNOLOGY_LTE) && (mPhone.getServiceState().
                getDataRegistrationState() == ServiceState.STATE_IN_SERVICE));
    }

    private boolean isLimitedLteService() {
        return ((mPhone.getServiceState().getRilVoiceRadioTechnology() ==
            ServiceState.RIL_RADIO_TECHNOLOGY_LTE) && mPhone.getServiceState().isEmergencyOnly());
    }

    private boolean isEmergencySmsPossible() {
        return isLteService() || isLimitedLteService();
    }

    public boolean isEmergencySmsSupport(String destAddr) {
        PersistableBundle b;
        boolean eSmsCarrierSupport = false;
        if (!mTelephonyManager.isEmergencyNumber(destAddr)) {
            logi(Rlog.pii(TAG, destAddr) + " is not emergency number");
            return false;
        }

        final long identity = Binder.clearCallingIdentity();
        try {
            CarrierConfigManager configManager = (CarrierConfigManager) mContext
                    .getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (configManager == null) {
                loge("configManager is null");
                return false;
            }
            b = configManager.getConfigForSubId(getSubId());
            if (b == null) {
                loge("PersistableBundle is null");
                return false;
            }
            eSmsCarrierSupport = b.getBoolean(
                    CarrierConfigManager.KEY_SUPPORT_EMERGENCY_SMS_OVER_IMS_BOOL);
            boolean lteOrLimitedLte = isEmergencySmsPossible();
            logi("isEmergencySmsSupport emergencySmsCarrierSupport: "
                    + eSmsCarrierSupport + " destAddr: " + Rlog.pii(TAG, destAddr)
                    + " mIsImsServiceUp: " + mIsImsServiceUp + " lteOrLimitedLte: "
                    + lteOrLimitedLte);

            return eSmsCarrierSupport && mIsImsServiceUp && lteOrLimitedLte;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean isAvailable() {
        synchronized (mLock) {
            logd("isAvailable: up=" + mIsImsServiceUp + ", reg= " + mIsRegistered
                    + ", cap= " + mIsSmsCapable);
            return mIsImsServiceUp && mIsRegistered && mIsSmsCapable;
        }
    }

    @Override
    protected String getFormat() {
        // This is called in the constructor before ImsSmsDispatcher has a chance to initialize
        // mLock. ImsManager will not be up anyway at this point, so report UNKNOWN.
        if (mLock == null) return SmsConstants.FORMAT_UNKNOWN;
        try {
            return getImsManager().getSmsFormat();
        } catch (ImsException e) {
            loge("Failed to get sms format. Error: " + e.getMessage());
            return SmsConstants.FORMAT_UNKNOWN;
        }
    }

    @Override
    public int getMaxSmsRetryCount() {
        int retryCount = MAX_SEND_RETRIES;
        CarrierConfigManager mConfigManager;

        mConfigManager = (CarrierConfigManager)  mContext.getSystemService(
                Context.CARRIER_CONFIG_SERVICE);

        if (mConfigManager != null) {
            PersistableBundle carrierConfig = mConfigManager.getConfigForSubId(
                    getSubId());

            if (carrierConfig != null) {
                retryCount = carrierConfig.getInt(
                        CarrierConfigManager.ImsSms.KEY_SMS_MAX_RETRY_COUNT_INT);
            }
        }

        Rlog.d(TAG, "Retry Count: " + retryCount);

        return retryCount;
    }

    /**
     * Returns the number of times SMS can be sent over IMS
     *
     * @return  retry count over Ims from  carrier configuration
     */
    @VisibleForTesting
    public int getMaxRetryCountOverIms() {
        int retryCountOverIms = MAX_SEND_RETRIES_OVER_IMS;
        CarrierConfigManager mConfigManager;

        mConfigManager = (CarrierConfigManager) mContext.getSystemService(Context
                                                        .CARRIER_CONFIG_SERVICE);

        if (mConfigManager != null) {
            PersistableBundle carrierConfig = mConfigManager.getConfigForSubId(
                    getSubId());


            if (carrierConfig != null) {
                retryCountOverIms = carrierConfig.getInt(
                        CarrierConfigManager.ImsSms.KEY_SMS_MAX_RETRY_OVER_IMS_COUNT_INT);
            }
        }

        Rlog.d(TAG, "Retry Count Over Ims: " + retryCountOverIms);

        return retryCountOverIms;
    }

    @Override
    public int getSmsRetryDelayValue() {
        int retryDelay = SEND_RETRY_DELAY;
        CarrierConfigManager mConfigManager;

        mConfigManager = (CarrierConfigManager)  mContext.getSystemService(
                Context.CARRIER_CONFIG_SERVICE);

        if (mConfigManager != null) {
            PersistableBundle carrierConfig = mConfigManager.getConfigForSubId(
                    getSubId());

            if (carrierConfig != null) {
                retryDelay = carrierConfig.getInt(
                        CarrierConfigManager.ImsSms.KEY_SMS_OVER_IMS_SEND_RETRY_DELAY_MILLIS_INT);
            }
        }

        Rlog.d(TAG, "Retry delay: " + retryDelay);

        return retryDelay;
    }

    @Override
    protected boolean shouldBlockSmsForEcbm() {
        // We should not block outgoing SMS during ECM on IMS. It only applies to outgoing CDMA
        // SMS.
        return false;
    }

    @Override
    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String scAddr, String destAddr,
            String message, boolean statusReportRequested, SmsHeader smsHeader, int priority,
            int validityPeriod) {
        return SMSDispatcherUtil.getSubmitPdu(isCdmaMo(), scAddr, destAddr, message,
                statusReportRequested, smsHeader, priority, validityPeriod);
    }

    @Override
    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String scAddr, String destAddr,
            int destPort, byte[] message, boolean statusReportRequested) {
        return SMSDispatcherUtil.getSubmitPdu(isCdmaMo(), scAddr, destAddr, destPort, message,
                statusReportRequested);
    }

    @Override
    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String scAddr, String destAddr,
            String message, boolean statusReportRequested, SmsHeader smsHeader, int priority,
            int validityPeriod, int messageRef) {
        return SMSDispatcherUtil.getSubmitPdu(isCdmaMo(), scAddr, destAddr, message,
                statusReportRequested, smsHeader, priority, validityPeriod, messageRef);
    }

    @Override
    protected SmsMessageBase.SubmitPduBase getSubmitPdu(String scAddr, String destAddr,
            int destPort, byte[] message, boolean statusReportRequested, int messageRef) {
        return SMSDispatcherUtil.getSubmitPdu(isCdmaMo(), scAddr, destAddr, destPort, message,
                statusReportRequested, messageRef);
    }

    @Override
    protected TextEncodingDetails calculateLength(CharSequence messageBody, boolean use7bitOnly) {
        return SMSDispatcherUtil.calculateLength(isCdmaMo(), messageBody, use7bitOnly);
    }

    /**
     * Send the Memory Available Event to the ImsService
     */
    public void onMemoryAvailable() {
        logd("onMemoryAvailable ");
        int token = mNextToken.incrementAndGet();
        try {
            logd("onMemoryAvailable: token = " + token);
            mMemoryAvailableNotifierList.add(token);
            getImsManager().onMemoryAvailable(token);
        } catch (ImsException e) {
            loge("onMemoryAvailable failed: " + e.getMessage());
            if (mMemoryAvailableNotifierList.contains(token)) {
                mMemoryAvailableNotifierList.remove(Integer.valueOf(token));
            }
        }
    }

    @Override
    public void sendSms(SmsTracker tracker) {
        logd("sendSms: "
                + " mRetryCount=" + tracker.mRetryCount
                + " mMessageRef=" + tracker.mMessageRef
                + " SS=" + mPhone.getServiceState().getState());

        // Flag that this Tracker is using the ImsService implementation of SMS over IMS for sending
        // this message. Any fallbacks will happen over CS only.
        tracker.mUsesImsServiceForIms = true;

        HashMap<String, Object> map = tracker.getData();

        byte[] pdu = (byte[]) map.get(MAP_KEY_PDU);
        byte smsc[] = (byte[]) map.get(MAP_KEY_SMSC);
        boolean isRetry = tracker.mRetryCount > 0;
        String format = getFormat();

        if (SmsConstants.FORMAT_3GPP.equals(format) && isRetry) {
            // per TS 23.040 Section 9.2.3.6:  If TP-MTI SMS-SUBMIT (0x01) type
            //   TP-RD (bit 2) is 1 for retry
            //   and TP-MR is set to previously failed sms TP-MR
            if (((0x01 & pdu[0]) == 0x01)) {
                pdu[0] |= 0x04; // TP-RD
                pdu[1] = (byte) tracker.mMessageRef; // TP-MR
            }
        }

        int token = mNextToken.incrementAndGet();
        mTrackers.put(token, tracker);
        try {
            getImsManager().sendSms(
                    token,
                    tracker.mMessageRef,
                    format,
                    smsc != null ? IccUtils.bytesToHexString(smsc) : null,
                    isRetry,
                    pdu);
            mMetrics.writeImsServiceSendSms(mPhone.getPhoneId(), format,
                    ImsSmsImplBase.SEND_STATUS_OK, tracker.mMessageId);
        } catch (ImsException e) {
            loge("sendSms failed. Falling back to PSTN. Error: " + e.getMessage());
            mTrackers.remove(token);
            fallbackToPstn(tracker);
            mMetrics.writeImsServiceSendSms(mPhone.getPhoneId(), format,
                    ImsSmsImplBase.SEND_STATUS_ERROR_FALLBACK, tracker.mMessageId);
            mPhone.getSmsStats().onOutgoingSms(
                    true /* isOverIms */,
                    SmsConstants.FORMAT_3GPP2.equals(format),
                    true /* fallbackToCs */,
                    SmsManager.RESULT_SYSTEM_ERROR,
                    tracker.mMessageId,
                    tracker.isFromDefaultSmsApplication(mContext),
                    tracker.getInterval(),
                    mTelephonyManager.isEmergencyNumber(tracker.mDestAddress),
                    tracker.isMtSmsPollingMessage(mContext));
            if (mPhone != null) {
                TelephonyAnalytics telephonyAnalytics = mPhone.getTelephonyAnalytics();
                if (telephonyAnalytics != null) {
                    SmsMmsAnalytics smsMmsAnalytics = telephonyAnalytics.getSmsMmsAnalytics();
                    if (smsMmsAnalytics != null) {
                        smsMmsAnalytics.onOutgoingSms(
                                true /* isOverIms */,
                                SmsManager.RESULT_SYSTEM_ERROR
                        );
                    }
                }
            }
        }
    }

    private ImsManager getImsManager() throws ImsException {
        synchronized (mLock) {
            if (mImsManager == null) {
                throw new ImsException("ImsManager not up",
                        ImsReasonInfo.CODE_LOCAL_IMS_SERVICE_DOWN);
            }
            return mImsManager;
        }
    }

    @VisibleForTesting
    public void fallbackToPstn(SmsTracker tracker) {
        mSmsDispatchersController.sendRetrySms(tracker);
    }

    @Override
    protected boolean isCdmaMo() {
        return mSmsDispatchersController.isCdmaFormat(getFormat());
    }

    @VisibleForTesting
    public IImsSmsListener getSmsListener() {
        return mImsSmsListener;
    }

    private void logd(String s) {
        Rlog.d(TAG + " [" + getPhoneId(mPhone) + "]", s);
    }

    private void logi(String s) {
        Rlog.i(TAG + " [" + getPhoneId(mPhone) + "]", s);
    }

    private void logw(String s) {
        Rlog.w(TAG + " [" + getPhoneId(mPhone) + "]", s);
    }

    private void loge(String s) {
        Rlog.e(TAG + " [" + getPhoneId(mPhone) + "]", s);
    }

    private static String getPhoneId(Phone phone) {
        return (phone != null) ? Integer.toString(phone.getPhoneId()) : "?";
    }
}
