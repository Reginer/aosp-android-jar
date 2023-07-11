/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.internal.telephony.imsphone;

import static android.provider.Telephony.SimInfo.COLUMN_PHONE_NUMBER_SOURCE_IMS;
import static android.telephony.ims.ImsManager.EXTRA_WFC_REGISTRATION_FAILURE_MESSAGE;
import static android.telephony.ims.ImsManager.EXTRA_WFC_REGISTRATION_FAILURE_TITLE;

import static com.android.internal.telephony.CommandsInterface.CB_FACILITY_BAIC;
import static com.android.internal.telephony.CommandsInterface.CB_FACILITY_BAICr;
import static com.android.internal.telephony.CommandsInterface.CB_FACILITY_BAOC;
import static com.android.internal.telephony.CommandsInterface.CB_FACILITY_BAOIC;
import static com.android.internal.telephony.CommandsInterface.CB_FACILITY_BAOICxH;
import static com.android.internal.telephony.CommandsInterface.CB_FACILITY_BA_ALL;
import static com.android.internal.telephony.CommandsInterface.CB_FACILITY_BA_MO;
import static com.android.internal.telephony.CommandsInterface.CB_FACILITY_BA_MT;
import static com.android.internal.telephony.CommandsInterface.CB_FACILITY_BIC_ACR;
import static com.android.internal.telephony.CommandsInterface.CF_ACTION_DISABLE;
import static com.android.internal.telephony.CommandsInterface.CF_ACTION_ENABLE;
import static com.android.internal.telephony.CommandsInterface.CF_ACTION_ERASURE;
import static com.android.internal.telephony.CommandsInterface.CF_ACTION_REGISTRATION;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_ALL;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_ALL_CONDITIONAL;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_BUSY;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_NOT_REACHABLE;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_NO_REPLY;
import static com.android.internal.telephony.CommandsInterface.CF_REASON_UNCONDITIONAL;
import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_NONE;
import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_VOICE;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.ResultReceiver;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.sysprop.TelephonyProperties;
import android.telephony.AccessNetworkConstants;
import android.telephony.CarrierConfigManager;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UssdResponse;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.ims.ImsCallForwardInfo;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsSsData;
import android.telephony.ims.ImsSsInfo;
import android.telephony.ims.RegistrationManager;
import android.telephony.ims.stub.ImsUtImplBase;
import android.text.TextUtils;
import android.util.LocalLog;

import com.android.ims.ImsEcbm;
import com.android.ims.ImsEcbmStateListener;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsUtInterface;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallTracker;
import com.android.internal.telephony.CarrierPrivilegesTracker;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.emergency.EmergencyNumberTracker;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.metrics.ImsStats;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.metrics.VoiceCallSessionStats;
import com.android.internal.telephony.nano.TelephonyProto.ImsConnectionState;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.util.NotificationChannelController;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * {@hide}
 */
public class ImsPhone extends ImsPhoneBase {
    private static final String LOG_TAG = "ImsPhone";
    private static final boolean DBG = true;
    private static final boolean VDBG = false; // STOPSHIP if true

    private static final int EVENT_SET_CALL_BARRING_DONE             = EVENT_LAST + 1;
    private static final int EVENT_GET_CALL_BARRING_DONE             = EVENT_LAST + 2;
    private static final int EVENT_SET_CALL_WAITING_DONE             = EVENT_LAST + 3;
    private static final int EVENT_GET_CALL_WAITING_DONE             = EVENT_LAST + 4;
    private static final int EVENT_SET_CLIR_DONE                     = EVENT_LAST + 5;
    private static final int EVENT_GET_CLIR_DONE                     = EVENT_LAST + 6;
    private static final int EVENT_DEFAULT_PHONE_DATA_STATE_CHANGED  = EVENT_LAST + 7;
    @VisibleForTesting
    public static final int EVENT_SERVICE_STATE_CHANGED             = EVENT_LAST + 8;
    private static final int EVENT_VOICE_CALL_ENDED                  = EVENT_LAST + 9;
    private static final int EVENT_INITIATE_VOLTE_SILENT_REDIAL      = EVENT_LAST + 10;
    private static final int EVENT_GET_CLIP_DONE                     = EVENT_LAST + 11;

    static final int RESTART_ECM_TIMER = 0; // restart Ecm timer
    static final int CANCEL_ECM_TIMER  = 1; // cancel Ecm timer

    // Default Emergency Callback Mode exit timer
    private static final long DEFAULT_ECM_EXIT_TIMER_VALUE = 300000;

    // String to Call Composer Option Prefix set by user
    private static final String PREF_USER_SET_CALL_COMPOSER_PREFIX = "userset_callcomposer_prefix";

    /**
     * Used to create ImsManager instances, which may be injected during testing.
     */
    @VisibleForTesting
    public interface ImsManagerFactory {
        /**
         * Create a new instance of ImsManager for the specified phoneId.
         */
        ImsManager create(Context context, int phoneId);
    }

    public static class ImsDialArgs extends DialArgs {
        public static class Builder extends DialArgs.Builder<ImsDialArgs.Builder> {
            private android.telecom.Connection.RttTextStream mRttTextStream;
            private int mRetryCallFailCause = ImsReasonInfo.CODE_UNSPECIFIED;
            private int mRetryCallFailNetworkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
            private boolean mIsWpsCall = false;

            public static ImsDialArgs.Builder from(DialArgs dialArgs) {
                if (dialArgs instanceof ImsDialArgs) {
                    return new ImsDialArgs.Builder()
                            .setUusInfo(dialArgs.uusInfo)
                            .setIsEmergency(dialArgs.isEmergency)
                            .setVideoState(dialArgs.videoState)
                            .setIntentExtras(dialArgs.intentExtras)
                            .setRttTextStream(((ImsDialArgs)dialArgs).rttTextStream)
                            .setClirMode(dialArgs.clirMode)
                            .setRetryCallFailCause(((ImsDialArgs)dialArgs).retryCallFailCause)
                            .setRetryCallFailNetworkType(
                                    ((ImsDialArgs)dialArgs).retryCallFailNetworkType)
                            .setIsWpsCall(((ImsDialArgs)dialArgs).isWpsCall);
                }
                return new ImsDialArgs.Builder()
                        .setUusInfo(dialArgs.uusInfo)
                        .setIsEmergency(dialArgs.isEmergency)
                        .setVideoState(dialArgs.videoState)
                        .setClirMode(dialArgs.clirMode)
                        .setIntentExtras(dialArgs.intentExtras);
            }

            public ImsDialArgs.Builder setRttTextStream(
                    android.telecom.Connection.RttTextStream s) {
                mRttTextStream = s;
                return this;
            }

            public ImsDialArgs.Builder setRetryCallFailCause(int retryCallFailCause) {
                this.mRetryCallFailCause = retryCallFailCause;
                return this;
            }

            public ImsDialArgs.Builder setRetryCallFailNetworkType(int retryCallFailNetworkType) {
                this.mRetryCallFailNetworkType = retryCallFailNetworkType;
                return this;
            }

            public ImsDialArgs.Builder setIsWpsCall(boolean isWpsCall) {
                this.mIsWpsCall = isWpsCall;
                return this;
            }

            public ImsDialArgs build() {
                return new ImsDialArgs(this);
            }
        }

        /**
         * The RTT text stream. If non-null, indicates that connection supports RTT
         * communication with the in-call app.
         */
        public final android.telecom.Connection.RttTextStream rttTextStream;

        public final int retryCallFailCause;
        public final int retryCallFailNetworkType;

        /** Indicates the call is Wireless Priority Service call */
        public final boolean isWpsCall;

        private ImsDialArgs(ImsDialArgs.Builder b) {
            super(b);
            this.rttTextStream = b.mRttTextStream;
            this.retryCallFailCause = b.mRetryCallFailCause;
            this.retryCallFailNetworkType = b.mRetryCallFailNetworkType;
            this.isWpsCall = b.mIsWpsCall;
        }
    }

    // Instance Variables
    Phone mDefaultPhone;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    ImsPhoneCallTracker mCT;
    ImsExternalCallTracker mExternalCallTracker;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private ArrayList <ImsPhoneMmiCode> mPendingMMIs = new ArrayList<ImsPhoneMmiCode>();
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private ServiceState mSS = new ServiceState();

    private final ImsManagerFactory mImsManagerFactory;

    private SharedPreferences mImsPhoneSharedPreferences;

    // To redial silently through GSM or CDMA when dialing through IMS fails
    private String mLastDialString;

    private WakeLock mWakeLock;

    // mEcmExitRespRegistrant is informed after the phone has been exited the emergency
    // callback mode keep track of if phone is in emergency callback mode
    private Registrant mEcmExitRespRegistrant;

    private final RegistrantList mSilentRedialRegistrants = new RegistrantList();

    private final LocalLog mRegLocalLog = new LocalLog(64);
    private TelephonyMetrics mMetrics;

    // The helper class to receive and store the MmTel registration status updated.
    private ImsRegistrationCallbackHelper mImsMmTelRegistrationHelper;

    // The roaming state if currently in service, or the last roaming state when was in service.
    private boolean mLastKnownRoamingState = false;

    private boolean mIsInImsEcm = false;

    // List of Registrants to send supplementary service notifications to.
    private RegistrantList mSsnRegistrants = new RegistrantList();

    private ImsStats mImsStats;

    // A runnable which is used to automatically exit from Ecm after a period of time.
    private Runnable mExitEcmRunnable = new Runnable() {
        @Override
        public void run() {
            exitEmergencyCallbackMode();
        }
    };

    private Uri[] mCurrentSubscriberUris;

    protected void setCurrentSubscriberUris(Uri[] currentSubscriberUris) {
        this.mCurrentSubscriberUris = currentSubscriberUris;
    }

    @Override
    public Uri[] getCurrentSubscriberUris() {
        return mCurrentSubscriberUris;
    }

    /** Set call composer status from users for the current subscription */
    public void setCallComposerStatus(int status) {
        mImsPhoneSharedPreferences.edit().putInt(
                PREF_USER_SET_CALL_COMPOSER_PREFIX + getSubId(), status).commit();
    }

    /** Get call composer status from users for the current subscription */
    public int getCallComposerStatus() {
        return mImsPhoneSharedPreferences.getInt(PREF_USER_SET_CALL_COMPOSER_PREFIX + getSubId(),
                TelephonyManager.CALL_COMPOSER_STATUS_OFF);
    }

    @Override
    public int getEmergencyNumberDbVersion() {
        return getEmergencyNumberTracker().getEmergencyNumberDbVersion();
    }

    @Override
    public EmergencyNumberTracker getEmergencyNumberTracker() {
        return mDefaultPhone.getEmergencyNumberTracker();
    }

    @Override
    public ServiceStateTracker getServiceStateTracker() {
        return mDefaultPhone.getServiceStateTracker();
    }

    // Create Cf (Call forward) so that dialling number &
    // mIsCfu (true if reason is call forward unconditional)
    // mOnComplete (Message object passed by client) can be packed &
    // given as a single Cf object as user data to UtInterface.
    private static class Cf {
        final String mSetCfNumber;
        final Message mOnComplete;
        final boolean mIsCfu;

        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        Cf(String cfNumber, boolean isCfu, Message onComplete) {
            mSetCfNumber = cfNumber;
            mIsCfu = isCfu;
            mOnComplete = onComplete;
        }
    }

    // Create SS (Supplementary Service) so that save SS params &
    // mOnComplete (Message object passed by client) can be packed
    // given as a single SS object as user data to UtInterface.
    @VisibleForTesting
    public static class SS {
        int mCfAction;
        int mCfReason;
        String mDialingNumber;
        int mTimerSeconds;
        boolean mEnable;
        int mClirMode;
        String mFacility;
        boolean mLockState;
        String mPassword;
        int mServiceClass;
        @VisibleForTesting
        public Message mOnComplete;

        // Default // Query CW, CLIR, CLIP
        SS(Message onComplete) {
            mOnComplete = onComplete;
        }

        // Update CLIP
        SS(boolean enable, Message onComplete) {
            mEnable = enable;
            mOnComplete = onComplete;
        }

        // Update CLIR
        SS(int clirMode, Message onComplete) {
            mClirMode = clirMode;
            mOnComplete = onComplete;
        }

        // Update CW
        SS(boolean enable, int serviceClass, Message onComplete) {
            mEnable = enable;
            mServiceClass = serviceClass;
            mOnComplete = onComplete;
        }

        // Query CF
        SS(int cfReason, int serviceClass, Message onComplete) {
            mCfReason = cfReason;
            mServiceClass = serviceClass;
            mOnComplete = onComplete;
        }

        // Update CF
        SS(int cfAction, int cfReason, String dialingNumber,
           int serviceClass, int timerSeconds, Message onComplete) {
            mCfAction = cfAction;
            mCfReason = cfReason;
            mDialingNumber = dialingNumber;
            mServiceClass = serviceClass;
            mTimerSeconds = timerSeconds;
            mOnComplete = onComplete;
        }

        // Query CB
        SS(String facility, String password, int serviceClass, Message onComplete) {
            mFacility = facility;
            mPassword = password;
            mServiceClass = serviceClass;
            mOnComplete = onComplete;
        }

        // Update CB
        SS(String facility, boolean lockState, String password,
                int serviceClass, Message onComplete) {
            mFacility = facility;
            mLockState = lockState;
            mPassword = password;
            mServiceClass = serviceClass;
            mOnComplete = onComplete;
        }
    }

    // Constructors
    public ImsPhone(Context context, PhoneNotifier notifier, Phone defaultPhone) {
        this(context, notifier, defaultPhone, ImsManager::getInstance, false);
    }

    @VisibleForTesting
    public ImsPhone(Context context, PhoneNotifier notifier, Phone defaultPhone,
            ImsManagerFactory imsManagerFactory, boolean unitTestMode) {
        super("ImsPhone", context, notifier, unitTestMode);

        mDefaultPhone = defaultPhone;
        mImsManagerFactory = imsManagerFactory;
        mImsPhoneSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mImsStats = new ImsStats(this);
        // The ImsExternalCallTracker needs to be defined before the ImsPhoneCallTracker, as the
        // ImsPhoneCallTracker uses a thread to spool up the ImsManager.  Part of this involves
        // setting the multiendpoint listener on the external call tracker.  So we need to ensure
        // the external call tracker is available first to avoid potential timing issues.
        mExternalCallTracker =
                TelephonyComponentFactory.getInstance()
                        .inject(ImsExternalCallTracker.class.getName())
                        .makeImsExternalCallTracker(this);
        mCT = TelephonyComponentFactory.getInstance().inject(ImsPhoneCallTracker.class.getName())
                .makeImsPhoneCallTracker(this);
        mCT.registerPhoneStateListener(mExternalCallTracker);
        mExternalCallTracker.setCallPuller(mCT);

        boolean legacyMode = true;
        if (mDefaultPhone.getAccessNetworksManager() != null) {
            legacyMode = mDefaultPhone.getAccessNetworksManager().isInLegacyMode();
        }
        mSS.setOutOfService(legacyMode, false);

        mPhoneId = mDefaultPhone.getPhoneId();

        mMetrics = TelephonyMetrics.getInstance();

        mImsMmTelRegistrationHelper = new ImsRegistrationCallbackHelper(mMmTelRegistrationUpdate,
                context.getMainExecutor());

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
        mWakeLock.setReferenceCounted(false);

        if (mDefaultPhone.getServiceStateTracker() != null
                && mDefaultPhone.getAccessNetworksManager() != null) {
            for (int transport : mDefaultPhone.getAccessNetworksManager()
                    .getAvailableTransports()) {
                mDefaultPhone.getServiceStateTracker()
                        .registerForDataRegStateOrRatChanged(transport, this,
                                EVENT_DEFAULT_PHONE_DATA_STATE_CHANGED, null);
            }
        }
        // Sets the Voice reg state to STATE_OUT_OF_SERVICE and also queries the data service
        // state. We don't ever need the voice reg state to be anything other than in or out of
        // service.
        setServiceState(ServiceState.STATE_OUT_OF_SERVICE);

        mDefaultPhone.registerForServiceStateChanged(this, EVENT_SERVICE_STATE_CHANGED, null);
        // Force initial roaming state update later, on EVENT_CARRIER_CONFIG_CHANGED.
        // Settings provider or CarrierConfig may not be loaded now.

        mDefaultPhone.registerForVolteSilentRedial(this, EVENT_INITIATE_VOLTE_SILENT_REDIAL, null);
    }

    //todo: get rid of this function. It is not needed since parentPhone obj never changes
    @Override
    public void dispose() {
        logd("dispose");
        // Nothing to dispose in Phone
        //super.dispose();
        mPendingMMIs.clear();
        mExternalCallTracker.tearDown();
        mCT.unregisterPhoneStateListener(mExternalCallTracker);
        mCT.unregisterForVoiceCallEnded(this);
        mCT.dispose();

        //Force all referenced classes to unregister their former registered events
        if (mDefaultPhone != null && mDefaultPhone.getServiceStateTracker() != null) {
            for (int transport : mDefaultPhone.getAccessNetworksManager()
                    .getAvailableTransports()) {
                mDefaultPhone.getServiceStateTracker()
                        .unregisterForDataRegStateOrRatChanged(transport, this);
            }
            mDefaultPhone.unregisterForServiceStateChanged(this);
        }

        if (mDefaultPhone != null) {
            mDefaultPhone.unregisterForVolteSilentRedial(this);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public ServiceState getServiceState() {
        return new ServiceState(mSS);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @VisibleForTesting
    public void setServiceState(int state) {
        boolean isVoiceRegStateChanged = false;

        synchronized (this) {
            isVoiceRegStateChanged = mSS.getState() != state;
            mSS.setVoiceRegState(state);
        }
        updateDataServiceState();

        if (isVoiceRegStateChanged) {
            if (mDefaultPhone.getServiceStateTracker() != null) {
                mDefaultPhone.getServiceStateTracker().onImsServiceStateChanged();
            }
        }
    }

    @Override
    public CallTracker getCallTracker() {
        return mCT;
    }

    public ImsExternalCallTracker getExternalCallTracker() {
        return mExternalCallTracker;
    }

    @Override
    public List<? extends ImsPhoneMmiCode>
    getPendingMmiCodes() {
        return mPendingMMIs;
    }

    @Override
    public void
    acceptCall(int videoState) throws CallStateException {
        mCT.acceptCall(videoState);
    }

    @Override
    public void
    rejectCall() throws CallStateException {
        mCT.rejectCall();
    }

    @Override
    public void
    switchHoldingAndActive() throws CallStateException {
        throw new UnsupportedOperationException("Use hold() and unhold() instead.");
    }

    @Override
    public boolean canConference() {
        return mCT.canConference();
    }

    public boolean canDial() {
        try {
            mCT.checkForDialIssues();
        } catch (CallStateException cse) {
            return false;
        }
        return true;
    }

    @Override
    public void conference() {
        mCT.conference();
    }

    @Override
    public void clearDisconnected() {
        mCT.clearDisconnected();
    }

    @Override
    public boolean canTransfer() {
        return mCT.canTransfer();
    }

    @Override
    public void explicitCallTransfer() throws CallStateException {
        mCT.explicitCallTransfer();
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public ImsPhoneCall
    getForegroundCall() {
        return mCT.mForegroundCall;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public ImsPhoneCall
    getBackgroundCall() {
        return mCT.mBackgroundCall;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public ImsPhoneCall
    getRingingCall() {
        return mCT.mRingingCall;
    }

    @Override
    public boolean isImsAvailable() {
        return mCT.isImsServiceReady();
    }

    @Override
    public CarrierPrivilegesTracker getCarrierPrivilegesTracker() {
        return mDefaultPhone.getCarrierPrivilegesTracker();
    }

    /**
     * Hold the currently active call, possibly unholding a currently held call.
     * @throws CallStateException
     */
    public void holdActiveCall() throws CallStateException {
        mCT.holdActiveCall();
    }

    /**
     * Unhold the currently active call, possibly holding a currently active call.
     * If the call tracker is already in the middle of a hold operation, this is a noop.
     * @throws CallStateException
     */
    public void unholdHeldCall() throws CallStateException {
        mCT.unholdHeldCall();
    }

    private boolean handleCallDeflectionIncallSupplementaryService(
            String dialString) {
        if (dialString.length() > 1) {
            return false;
        }

        if (getRingingCall().getState() != ImsPhoneCall.State.IDLE) {
            if (DBG) logd("MmiCode 0: rejectCall");
            try {
                mCT.rejectCall();
            } catch (CallStateException e) {
                if (DBG) Rlog.d(LOG_TAG, "reject failed", e);
                notifySuppServiceFailed(Phone.SuppService.REJECT);
            }
        } else if (getBackgroundCall().getState() != ImsPhoneCall.State.IDLE) {
            if (DBG) logd("MmiCode 0: hangupWaitingOrBackground");
            try {
                mCT.hangup(getBackgroundCall());
            } catch (CallStateException e) {
                if (DBG) Rlog.d(LOG_TAG, "hangup failed", e);
            }
        }

        return true;
    }

    private void sendUssdResponse(String ussdRequest, CharSequence message, int returnCode,
                                   ResultReceiver wrappedCallback) {
        UssdResponse response = new UssdResponse(ussdRequest, message);
        Bundle returnData = new Bundle();
        returnData.putParcelable(TelephonyManager.USSD_RESPONSE, response);
        wrappedCallback.send(returnCode, returnData);

    }

    @Override
    public boolean handleUssdRequest(String ussdRequest, ResultReceiver wrappedCallback)
            throws CallStateException {
        if (mPendingMMIs.size() > 0) {
            // There are MMI codes in progress; fail attempt now.
            logi("handleUssdRequest: queue full: " + Rlog.pii(LOG_TAG, ussdRequest));
            sendUssdResponse(ussdRequest, null, TelephonyManager.USSD_RETURN_FAILURE,
                    wrappedCallback );
            return true;
        }
        try {
            dialInternal(ussdRequest, new ImsDialArgs.Builder().build(), wrappedCallback);
        } catch (CallStateException cse) {
            if (CS_FALLBACK.equals(cse.getMessage())) {
                throw cse;
            } else {
                Rlog.w(LOG_TAG, "Could not execute USSD " + cse);
                sendUssdResponse(ussdRequest, null, TelephonyManager.USSD_RETURN_FAILURE,
                        wrappedCallback);
            }
        } catch (Exception e) {
            Rlog.w(LOG_TAG, "Could not execute USSD " + e);
            sendUssdResponse(ussdRequest, null, TelephonyManager.USSD_RETURN_FAILURE,
                    wrappedCallback);
            return false;
        }
        return true;
    }

    private boolean handleCallWaitingIncallSupplementaryService(
            String dialString) {
        int len = dialString.length();

        if (len > 2) {
            return false;
        }

        ImsPhoneCall call = getForegroundCall();

        try {
            if (len > 1) {
                if (DBG) logd("not support 1X SEND");
                notifySuppServiceFailed(Phone.SuppService.HANGUP);
            } else {
                if (call.getState() != ImsPhoneCall.State.IDLE) {
                    if (DBG) logd("MmiCode 1: hangup foreground");
                    mCT.hangup(call);
                } else {
                    if (DBG) logd("MmiCode 1: holdActiveCallForWaitingCall");
                    mCT.holdActiveCallForWaitingCall();
                }
            }
        } catch (CallStateException e) {
            if (DBG) Rlog.d(LOG_TAG, "hangup failed", e);
            notifySuppServiceFailed(Phone.SuppService.HANGUP);
        }

        return true;
    }

    private boolean handleCallHoldIncallSupplementaryService(String dialString) {
        int len = dialString.length();

        if (len > 2) {
            return false;
        }

        if (len > 1) {
            if (DBG) logd("separate not supported");
            notifySuppServiceFailed(Phone.SuppService.SEPARATE);
        } else {
            try {
                if (getRingingCall().getState() != ImsPhoneCall.State.IDLE) {
                    if (DBG) logd("MmiCode 2: accept ringing call");
                    mCT.acceptCall(ImsCallProfile.CALL_TYPE_VOICE);
                } else if (getBackgroundCall().getState() == ImsPhoneCall.State.HOLDING) {
                    // If there's an active ongoing call as well, hold it and the background one
                    // should automatically unhold. Otherwise just unhold the background call.
                    if (getForegroundCall().getState() != ImsPhoneCall.State.IDLE) {
                        if (DBG) logd("MmiCode 2: switch holding and active");
                        mCT.holdActiveCall();
                    } else {
                        if (DBG) logd("MmiCode 2: unhold held call");
                        mCT.unholdHeldCall();
                    }
                } else if (getForegroundCall().getState() != ImsPhoneCall.State.IDLE) {
                    if (DBG) logd("MmiCode 2: hold active call");
                    mCT.holdActiveCall();
                }
            } catch (CallStateException e) {
                if (DBG) Rlog.d(LOG_TAG, "switch failed", e);
                notifySuppServiceFailed(Phone.SuppService.SWITCH);
            }
        }

        return true;
    }

    private boolean handleMultipartyIncallSupplementaryService(
            String dialString) {
        if (dialString.length() > 1) {
            return false;
        }

        if (DBG) logd("MmiCode 3: merge calls");
        conference();
        return true;
    }

    private boolean handleEctIncallSupplementaryService(String dialString) {
        if (dialString.length() != 1) {
            return false;
        }

        if (DBG) logd("MmiCode 4: explicit call transfer");
        try {
            explicitCallTransfer();
        } catch (CallStateException e) {
            if (DBG) Rlog.d(LOG_TAG, "explicit call transfer failed", e);
            notifySuppServiceFailed(Phone.SuppService.TRANSFER);
        }
        return true;
    }

    private boolean handleCcbsIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return false;
        }

        logi("MmiCode 5: CCBS not supported!");
        // Treat it as an "unknown" service.
        notifySuppServiceFailed(Phone.SuppService.UNKNOWN);
        return true;
    }

    public void notifySuppSvcNotification(SuppServiceNotification suppSvc) {
        logd("notifySuppSvcNotification: suppSvc = " + suppSvc);

        AsyncResult ar = new AsyncResult(null, suppSvc, null);
        mSsnRegistrants.notifyRegistrants(ar);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public boolean handleInCallMmiCommands(String dialString) {
        if (!isInCall()) {
            return false;
        }

        if (TextUtils.isEmpty(dialString)) {
            return false;
        }

        boolean result = false;
        char ch = dialString.charAt(0);
        switch (ch) {
            case '0':
                result = handleCallDeflectionIncallSupplementaryService(
                        dialString);
                break;
            case '1':
                result = handleCallWaitingIncallSupplementaryService(
                        dialString);
                break;
            case '2':
                result = handleCallHoldIncallSupplementaryService(dialString);
                break;
            case '3':
                result = handleMultipartyIncallSupplementaryService(dialString);
                break;
            case '4':
                result = handleEctIncallSupplementaryService(dialString);
                break;
            case '5':
                result = handleCcbsIncallSupplementaryService(dialString);
                break;
            default:
                break;
        }

        return result;
    }

    boolean isInCall() {
        ImsPhoneCall.State foregroundCallState = getForegroundCall().getState();
        ImsPhoneCall.State backgroundCallState = getBackgroundCall().getState();
        ImsPhoneCall.State ringingCallState = getRingingCall().getState();

       return (foregroundCallState.isAlive() ||
               backgroundCallState.isAlive() ||
               ringingCallState.isAlive());
    }

    @Override
    public boolean isInImsEcm() {
        return mIsInImsEcm;
    }

    @Override
    public boolean isInEcm() {
        return mDefaultPhone.isInEcm();
    }

    @Override
    public void setIsInEcm(boolean isInEcm){
        mIsInImsEcm = isInEcm;
        mDefaultPhone.setIsInEcm(isInEcm);
    }

    public void notifyNewRingingConnection(Connection c) {
        mDefaultPhone.notifyNewRingingConnectionP(c);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    void notifyUnknownConnection(Connection c) {
        mDefaultPhone.notifyUnknownConnectionP(c);
    }

    @Override
    public void notifyForVideoCapabilityChanged(boolean isVideoCapable) {
        mIsVideoCapable = isVideoCapable;
        mDefaultPhone.notifyForVideoCapabilityChanged(isVideoCapable);
    }

    @Override
    public void setRadioPower(boolean on, boolean forEmergencyCall,
            boolean isSelectedPhoneForEmergencyCall, boolean forceApply) {
        mDefaultPhone.setRadioPower(on, forEmergencyCall, isSelectedPhoneForEmergencyCall,
                forceApply);
    }

    @Override
    public Connection startConference(String[] participantsToDial, DialArgs dialArgs)
            throws CallStateException {
         ImsDialArgs.Builder imsDialArgsBuilder;
         imsDialArgsBuilder = ImsDialArgs.Builder.from(dialArgs);
         return mCT.startConference(participantsToDial, imsDialArgsBuilder.build());
    }

    @Override
    public Connection dial(String dialString, DialArgs dialArgs,
            Consumer<Phone> chosenPhoneConsumer) throws CallStateException {
        chosenPhoneConsumer.accept(this);
        return dialInternal(dialString, dialArgs, null);
    }

    private Connection dialInternal(String dialString, DialArgs dialArgs,
                                    ResultReceiver wrappedCallback)
            throws CallStateException {

        mLastDialString = dialString;

        // Need to make sure dialString gets parsed properly
        String newDialString = PhoneNumberUtils.stripSeparators(dialString);

        // handle in-call MMI first if applicable
        if (handleInCallMmiCommands(newDialString)) {
            return null;
        }

        ImsDialArgs.Builder imsDialArgsBuilder;
        imsDialArgsBuilder = ImsDialArgs.Builder.from(dialArgs);
        // Get the CLIR info if needed
        imsDialArgsBuilder.setClirMode(mCT.getClirMode());

        if (mDefaultPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            return mCT.dial(dialString, imsDialArgsBuilder.build());
        }

        // Only look at the Network portion for mmi
        String networkPortion = PhoneNumberUtils.extractNetworkPortionAlt(newDialString);
        ImsPhoneMmiCode mmi =
                ImsPhoneMmiCode.newFromDialString(networkPortion, this, wrappedCallback);
        if (DBG) logd("dialInternal: dialing w/ mmi '" + mmi + "'...");

        if (mmi == null) {
            return mCT.dial(dialString, imsDialArgsBuilder.build());
        } else if (mmi.isTemporaryModeCLIR()) {
            imsDialArgsBuilder.setClirMode(mmi.getCLIRMode());
            return mCT.dial(mmi.getDialingNumber(), imsDialArgsBuilder.build());
        } else if (!mmi.isSupportedOverImsPhone()) {
            // If the mmi is not supported by IMS service,
            // try to initiate dialing with default phone
            // Note: This code is never reached; there is a bug in isSupportedOverImsPhone which
            // causes it to return true even though the "processCode" method ultimately throws the
            // exception.
            logi("dialInternal: USSD not supported by IMS; fallback to CS.");
            throw new CallStateException(CS_FALLBACK);
        } else {
            mPendingMMIs.add(mmi);
            mMmiRegistrants.notifyRegistrants(new AsyncResult(null, mmi, null));

            try {
                mmi.processCode();
            } catch (CallStateException cse) {
                if (CS_FALLBACK.equals(cse.getMessage())) {
                    logi("dialInternal: fallback to GSM required.");
                    // Make sure we remove from the list of pending MMIs since it will handover to
                    // GSM.
                    mPendingMMIs.remove(mmi);
                    throw cse;
                }
            }

            return null;
        }
    }

    @Override
    public void
    sendDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            loge("sendDtmf called with invalid character '" + c + "'");
        } else {
            if (mCT.getState() ==  PhoneConstants.State.OFFHOOK) {
                mCT.sendDtmf(c, null);
            }
        }
    }

    @Override
    public void
    startDtmf(char c) {
        if (!(PhoneNumberUtils.is12Key(c) || (c >= 'A' && c <= 'D'))) {
            loge("startDtmf called with invalid character '" + c + "'");
        } else {
            mCT.startDtmf(c);
        }
    }

    @Override
    public void
    stopDtmf() {
        mCT.stopDtmf();
    }

    public void notifyIncomingRing() {
        if (DBG) logd("notifyIncomingRing");
        AsyncResult ar = new AsyncResult(null, null, null);
        sendMessage(obtainMessage(EVENT_CALL_RING, ar));
    }

    @Override
    public void setMute(boolean muted) {
        mCT.setMute(muted);
    }

    @Override
    public void setTTYMode(int ttyMode, Message onComplete) {
        mCT.setTtyMode(ttyMode);
    }

    @Override
    public void setUiTTYMode(int uiTtyMode, Message onComplete) {
        mCT.setUiTTYMode(uiTtyMode, onComplete);
    }

    @Override
    public boolean getMute() {
        return mCT.getMute();
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public PhoneConstants.State getState() {
        return mCT.getState();
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean isValidCommandInterfaceCFReason (int commandInterfaceCFReason) {
        switch (commandInterfaceCFReason) {
        case CF_REASON_UNCONDITIONAL:
        case CF_REASON_BUSY:
        case CF_REASON_NO_REPLY:
        case CF_REASON_NOT_REACHABLE:
        case CF_REASON_ALL:
        case CF_REASON_ALL_CONDITIONAL:
            return true;
        default:
            return false;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean isValidCommandInterfaceCFAction (int commandInterfaceCFAction) {
        switch (commandInterfaceCFAction) {
        case CF_ACTION_DISABLE:
        case CF_ACTION_ENABLE:
        case CF_ACTION_REGISTRATION:
        case CF_ACTION_ERASURE:
            return true;
        default:
            return false;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private  boolean isCfEnable(int action) {
        return (action == CF_ACTION_ENABLE) || (action == CF_ACTION_REGISTRATION);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int getConditionFromCFReason(int reason) {
        switch(reason) {
            case CF_REASON_UNCONDITIONAL: return ImsUtInterface.CDIV_CF_UNCONDITIONAL;
            case CF_REASON_BUSY: return ImsUtInterface.CDIV_CF_BUSY;
            case CF_REASON_NO_REPLY: return ImsUtInterface.CDIV_CF_NO_REPLY;
            case CF_REASON_NOT_REACHABLE: return ImsUtInterface.CDIV_CF_NOT_REACHABLE;
            case CF_REASON_ALL: return ImsUtInterface.CDIV_CF_ALL;
            case CF_REASON_ALL_CONDITIONAL: return ImsUtInterface.CDIV_CF_ALL_CONDITIONAL;
            default:
                break;
        }

        return ImsUtInterface.INVALID;
    }

    private int getCFReasonFromCondition(int condition) {
        switch(condition) {
            case ImsUtInterface.CDIV_CF_UNCONDITIONAL: return CF_REASON_UNCONDITIONAL;
            case ImsUtInterface.CDIV_CF_BUSY: return CF_REASON_BUSY;
            case ImsUtInterface.CDIV_CF_NO_REPLY: return CF_REASON_NO_REPLY;
            case ImsUtInterface.CDIV_CF_NOT_REACHABLE: return CF_REASON_NOT_REACHABLE;
            case ImsUtInterface.CDIV_CF_ALL: return CF_REASON_ALL;
            case ImsUtInterface.CDIV_CF_ALL_CONDITIONAL: return CF_REASON_ALL_CONDITIONAL;
            default:
                break;
        }

        return CF_REASON_NOT_REACHABLE;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int getActionFromCFAction(int action) {
        switch(action) {
            case CF_ACTION_DISABLE: return ImsUtInterface.ACTION_DEACTIVATION;
            case CF_ACTION_ENABLE: return ImsUtInterface.ACTION_ACTIVATION;
            case CF_ACTION_ERASURE: return ImsUtInterface.ACTION_ERASURE;
            case CF_ACTION_REGISTRATION: return ImsUtInterface.ACTION_REGISTRATION;
            default:
                break;
        }

        return ImsUtInterface.INVALID;
    }

    @Override
    public void getOutgoingCallerIdDisplay(Message onComplete) {
        if (DBG) logd("getCLIR");
        Message resp;
        SS ss = new SS(onComplete);
        resp = obtainMessage(EVENT_GET_CLIR_DONE, ss);

        try {
            ImsUtInterface ut = mCT.getUtInterface();
            ut.queryCLIR(resp);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    @Override
    public void setOutgoingCallerIdDisplay(int clirMode, Message onComplete) {
        if (DBG) logd("setCLIR action= " + clirMode);
        Message resp;
        // Packing CLIR value in the message. This will be required for
        // SharedPreference caching, if the message comes back as part of
        // a success response.
        SS ss = new SS(clirMode, onComplete);
        resp = obtainMessage(EVENT_SET_CLIR_DONE, ss);
        try {
            ImsUtInterface ut = mCT.getUtInterface();
            ut.updateCLIR(clirMode, resp);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    @Override
    public void queryCLIP(Message onComplete) {
        Message resp;
        SS ss = new SS(onComplete);
        resp = obtainMessage(EVENT_GET_CLIP_DONE, ss);

        try {
            Rlog.d(LOG_TAG, "ut.queryCLIP");
            ImsUtInterface ut = mCT.getUtInterface();
            ut.queryCLIP(resp);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void getCallForwardingOption(int commandInterfaceCFReason,
            Message onComplete) {
        getCallForwardingOption(commandInterfaceCFReason,
                SERVICE_CLASS_VOICE, onComplete);
    }

    @Override
    public void getCallForwardingOption(int commandInterfaceCFReason, int serviceClass,
            Message onComplete) {
        if (DBG) logd("getCallForwardingOption reason=" + commandInterfaceCFReason);
        if (isValidCommandInterfaceCFReason(commandInterfaceCFReason)) {
            if (DBG) logd("requesting call forwarding query.");
            Message resp;
            SS ss = new SS(commandInterfaceCFReason, serviceClass, onComplete);
            resp = obtainMessage(EVENT_GET_CALL_FORWARD_DONE, ss);

            try {
                ImsUtInterface ut = mCT.getUtInterface();
                ut.queryCallForward(getConditionFromCFReason(commandInterfaceCFReason), null, resp);
            } catch (ImsException e) {
                sendErrorResponse(onComplete, e);
            }
        } else if (onComplete != null) {
            sendErrorResponse(onComplete);
        }
    }

    @Override
    public void setCallForwardingOption(int commandInterfaceCFAction,
            int commandInterfaceCFReason,
            String dialingNumber,
            int timerSeconds,
            Message onComplete) {
        setCallForwardingOption(commandInterfaceCFAction, commandInterfaceCFReason, dialingNumber,
                CommandsInterface.SERVICE_CLASS_VOICE, timerSeconds, onComplete);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void setCallForwardingOption(int commandInterfaceCFAction,
            int commandInterfaceCFReason,
            String dialingNumber,
            int serviceClass,
            int timerSeconds,
            Message onComplete) {
        if (DBG) {
            logd("setCallForwardingOption action=" + commandInterfaceCFAction
                    + ", reason=" + commandInterfaceCFReason + " serviceClass=" + serviceClass);
        }
        if ((isValidCommandInterfaceCFAction(commandInterfaceCFAction)) &&
                (isValidCommandInterfaceCFReason(commandInterfaceCFReason))) {
            Message resp;
            SS ss = new SS(commandInterfaceCFAction, commandInterfaceCFReason,
                    dialingNumber, serviceClass, timerSeconds, onComplete);
            resp = obtainMessage(EVENT_SET_CALL_FORWARD_DONE, ss);

            try {
                ImsUtInterface ut = mCT.getUtInterface();
                ut.updateCallForward(getActionFromCFAction(commandInterfaceCFAction),
                        getConditionFromCFReason(commandInterfaceCFReason),
                        dialingNumber,
                        serviceClass,
                        timerSeconds,
                        resp);
            } catch (ImsException e) {
                sendErrorResponse(onComplete, e);
            }
        } else if (onComplete != null) {
            sendErrorResponse(onComplete);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void getCallWaiting(Message onComplete) {
        if (DBG) logd("getCallWaiting");
        Message resp;
        SS ss = new SS(onComplete);
        resp = obtainMessage(EVENT_GET_CALL_WAITING_DONE, ss);

        try {
            ImsUtInterface ut = mCT.getUtInterface();
            ut.queryCallWaiting(resp);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void setCallWaiting(boolean enable, Message onComplete) {
        int serviceClass = CommandsInterface.SERVICE_CLASS_VOICE;
        CarrierConfigManager configManager = (CarrierConfigManager)
                getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = configManager.getConfigForSubId(getSubId());
        if (b != null) {
            serviceClass = b.getInt(CarrierConfigManager.KEY_CALL_WAITING_SERVICE_CLASS_INT,
                    CommandsInterface.SERVICE_CLASS_VOICE);
        }
        setCallWaiting(enable, serviceClass, onComplete);
    }

    public void setCallWaiting(boolean enable, int serviceClass, Message onComplete) {
        if (DBG) logd("setCallWaiting enable=" + enable);
        Message resp;
        SS ss = new SS(enable, serviceClass, onComplete);
        resp = obtainMessage(EVENT_SET_CALL_WAITING_DONE, ss);

        try {
            ImsUtInterface ut = mCT.getUtInterface();
            ut.updateCallWaiting(enable, serviceClass, resp);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    private int getCBTypeFromFacility(String facility) {
        if (CB_FACILITY_BAOC.equals(facility)) {
            return ImsUtImplBase.CALL_BARRING_ALL_OUTGOING;
        } else if (CB_FACILITY_BAOIC.equals(facility)) {
            return ImsUtImplBase.CALL_BARRING_OUTGOING_INTL;
        } else if (CB_FACILITY_BAOICxH.equals(facility)) {
            return ImsUtImplBase.CALL_BARRING_OUTGOING_INTL_EXCL_HOME;
        } else if (CB_FACILITY_BAIC.equals(facility)) {
            return ImsUtImplBase.CALL_BARRING_ALL_INCOMING;
        } else if (CB_FACILITY_BAICr.equals(facility)) {
            return ImsUtImplBase.CALL_BLOCKING_INCOMING_WHEN_ROAMING;
        } else if (CB_FACILITY_BA_ALL.equals(facility)) {
            return ImsUtImplBase.CALL_BARRING_ALL;
        } else if (CB_FACILITY_BA_MO.equals(facility)) {
            return ImsUtImplBase.CALL_BARRING_OUTGOING_ALL_SERVICES;
        } else if (CB_FACILITY_BA_MT.equals(facility)) {
            return ImsUtImplBase.CALL_BARRING_INCOMING_ALL_SERVICES;
        } else if (CB_FACILITY_BIC_ACR.equals(facility)) {
            return ImsUtImplBase.CALL_BARRING_ANONYMOUS_INCOMING;
        }

        return 0;
    }

    public void getCallBarring(String facility, Message onComplete) {
        getCallBarring(facility, onComplete, CommandsInterface.SERVICE_CLASS_VOICE);
    }

    public void getCallBarring(String facility, Message onComplete, int serviceClass) {
        getCallBarring(facility, "", onComplete, serviceClass);
    }

    @Override
    public void getCallBarring(String facility, String password, Message onComplete,
            int serviceClass) {
        if (DBG) logd("getCallBarring facility=" + facility + ", serviceClass = " + serviceClass);
        Message resp;
        SS ss = new SS(facility, password, serviceClass, onComplete);
        resp = obtainMessage(EVENT_GET_CALL_BARRING_DONE, ss);

        try {
            ImsUtInterface ut = mCT.getUtInterface();
            // password is not required with Ut interface
            ut.queryCallBarring(getCBTypeFromFacility(facility), resp, serviceClass);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    public void setCallBarring(String facility, boolean lockState, String password,
            Message onComplete) {
        setCallBarring(facility, lockState, password, onComplete,
                CommandsInterface.SERVICE_CLASS_VOICE);
    }

    @Override
    public void setCallBarring(String facility, boolean lockState, String password,
            Message onComplete, int serviceClass) {
        if (DBG) {
            logd("setCallBarring facility=" + facility
                    + ", lockState=" + lockState + ", serviceClass = " + serviceClass);
        }
        Message resp;
        SS ss = new SS(facility, lockState, password, serviceClass, onComplete);
        resp = obtainMessage(EVENT_SET_CALL_BARRING_DONE, ss);

        int action;
        if (lockState) {
            action = CommandsInterface.CF_ACTION_ENABLE;
        }
        else {
            action = CommandsInterface.CF_ACTION_DISABLE;
        }

        try {
            ImsUtInterface ut = mCT.getUtInterface();
            ut.updateCallBarring(getCBTypeFromFacility(facility), action,
                    resp, null, serviceClass, password);
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    @Override
    public void sendUssdResponse(String ussdMessge) {
        logd("sendUssdResponse");
        ImsPhoneMmiCode mmi = ImsPhoneMmiCode.newFromUssdUserInput(ussdMessge, this);
        mPendingMMIs.add(mmi);
        mMmiRegistrants.notifyRegistrants(new AsyncResult(null, mmi, null));
        mmi.sendUssd(ussdMessge);
    }

    public void sendUSSD(String ussdString, Message response) {
        Rlog.d(LOG_TAG, "sendUssd ussdString = " + ussdString);
        mLastDialString = ussdString;
        mCT.sendUSSD(ussdString, response);
    }

    @Override
    public void cancelUSSD(Message msg) {
        mCT.cancelUSSD(msg);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void sendErrorResponse(Message onComplete) {
        logd("sendErrorResponse");
        if (onComplete != null) {
            AsyncResult.forMessage(onComplete, null,
                    new CommandException(CommandException.Error.GENERIC_FAILURE));
            onComplete.sendToTarget();
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @VisibleForTesting
    public void sendErrorResponse(Message onComplete, Throwable e) {
        logd("sendErrorResponse");
        if (onComplete != null) {
            AsyncResult.forMessage(onComplete, null, getCommandException(e));
            onComplete.sendToTarget();
        }
    }

    private CommandException getCommandException(int code, String errorString) {
        logd("getCommandException code= " + code + ", errorString= " + errorString);
        CommandException.Error error = CommandException.Error.GENERIC_FAILURE;

        switch(code) {
            case ImsReasonInfo.CODE_UT_NOT_SUPPORTED:
                // fall through
            case ImsReasonInfo.CODE_UT_OPERATION_NOT_ALLOWED:
                // not allowed is reported by operators when the network doesn't support a specific
                // type of barring.
                error = CommandException.Error.REQUEST_NOT_SUPPORTED;
                break;
            case ImsReasonInfo.CODE_UT_CB_PASSWORD_MISMATCH:
                error = CommandException.Error.PASSWORD_INCORRECT;
                break;
            case ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE:
                error = CommandException.Error.RADIO_NOT_AVAILABLE;
                break;
            case ImsReasonInfo.CODE_FDN_BLOCKED:
                error = CommandException.Error.FDN_CHECK_FAILURE;
                break;
            case ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_DIAL:
                error = CommandException.Error.SS_MODIFIED_TO_DIAL;
                break;
            case ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_USSD:
                error = CommandException.Error.SS_MODIFIED_TO_USSD;
                break;
            case ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_SS:
                error = CommandException.Error.SS_MODIFIED_TO_SS;
                break;
            case ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_DIAL_VIDEO:
                error = CommandException.Error.SS_MODIFIED_TO_DIAL_VIDEO;
                break;
            default:
                break;
        }

        return new CommandException(error, errorString);
    }

    private CommandException getCommandException(Throwable e) {
        CommandException ex = null;

        if (e instanceof ImsException) {
            ex = getCommandException(((ImsException)e).getCode(), e.getMessage());
        } else {
            logd("getCommandException generic failure");
            ex = new CommandException(CommandException.Error.GENERIC_FAILURE);
        }
        return ex;
    }

    private void
    onNetworkInitiatedUssd(ImsPhoneMmiCode mmi) {
        logd("onNetworkInitiatedUssd");
        mMmiCompleteRegistrants.notifyRegistrants(
            new AsyncResult(null, mmi, null));
    }

    /* package */
    void onIncomingUSSD(int ussdMode, String ussdMessage) {
        if (DBG) logd("onIncomingUSSD ussdMode=" + ussdMode);

        boolean isUssdError;
        boolean isUssdRequest;

        isUssdRequest
            = (ussdMode == CommandsInterface.USSD_MODE_REQUEST);

        isUssdError
            = (ussdMode != CommandsInterface.USSD_MODE_NOTIFY
                && ussdMode != CommandsInterface.USSD_MODE_REQUEST);

        ImsPhoneMmiCode found = null;
        for (int i = 0, s = mPendingMMIs.size() ; i < s; i++) {
            if(mPendingMMIs.get(i).isPendingUSSD()) {
                found = mPendingMMIs.get(i);
                break;
            }
        }

        if (found != null) {
            // Complete pending USSD
            if (isUssdError) {
                found.onUssdFinishedError();
            } else {
                found.onUssdFinished(ussdMessage, isUssdRequest);
            }
        } else if (!isUssdError && !TextUtils.isEmpty(ussdMessage)) {
                // pending USSD not found
                // The network may initiate its own USSD request

                // ignore everything that isnt a Notify or a Request
                // also, discard if there is no message to present
                ImsPhoneMmiCode mmi;
                mmi = ImsPhoneMmiCode.newNetworkInitiatedUssd(ussdMessage,
                        isUssdRequest,
                        this);
                onNetworkInitiatedUssd(mmi);
        } else if (isUssdError) {
            ImsPhoneMmiCode mmi;
            mmi = ImsPhoneMmiCode.newNetworkInitiatedUssd(ussdMessage,
                    true,
                    this);
            mmi.onUssdFinishedError();
        }
    }

    /**
     * Removes the given MMI from the pending list and notifies
     * registrants that it is complete.
     * @param mmi MMI that is done
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void onMMIDone(ImsPhoneMmiCode mmi) {
        /* Only notify complete if it's on the pending list.
         * Otherwise, it's already been handled (eg, previously canceled).
         * The exception is cancellation of an incoming USSD-REQUEST, which is
         * not on the list.
         */
        logd("onMMIDone: mmi=" + mmi);
        if (mPendingMMIs.remove(mmi) || mmi.isUssdRequest() || mmi.isSsInfo()) {
            ResultReceiver receiverCallback = mmi.getUssdCallbackReceiver();
            if (receiverCallback != null) {
                int returnCode = (mmi.getState() ==  MmiCode.State.COMPLETE) ?
                        TelephonyManager.USSD_RETURN_SUCCESS : TelephonyManager.USSD_RETURN_FAILURE;
                sendUssdResponse(mmi.getDialString(), mmi.getMessage(), returnCode,
                        receiverCallback );
            } else {
                logv("onMMIDone: notifyRegistrants");
                mMmiCompleteRegistrants.notifyRegistrants(
                    new AsyncResult(null, mmi, null));
            }
        }
    }

    @Override
    public ArrayList<Connection> getHandoverConnection() {
        ArrayList<Connection> connList = new ArrayList<Connection>();
        // Add all foreground call connections
        connList.addAll(getForegroundCall().getConnections());
        // Add all background call connections
        connList.addAll(getBackgroundCall().getConnections());
        // Add all background call connections
        connList.addAll(getRingingCall().getConnections());
        if (connList.size() > 0) {
            return connList;
        } else {
            return null;
        }
    }

    @Override
    public void notifySrvccState(Call.SrvccState state) {
        mCT.notifySrvccState(state);
    }

    /* package */ void
    initiateSilentRedial() {
        initiateSilentRedial(false, EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED);
    }

    /* package */ void
    initiateSilentRedial(boolean isEmergency, int eccCategory) {
        DialArgs dialArgs = new DialArgs.Builder()
                                        .setIsEmergency(isEmergency)
                                        .setEccCategory(eccCategory)
                                        .build();
        int cause = CallFailCause.LOCAL_CALL_CS_RETRY_REQUIRED;
        AsyncResult ar = new AsyncResult(null,
                                         new SilentRedialParam(mLastDialString, cause, dialArgs),
                                         null);
        if (ar != null) {
            // There is a race condition that can happen in some cases:
            // (Main thread) dial start
            // (Binder Thread) onCallSessionFailed
            // (Binder Thread) schedule a redial for CS on the main thread
            // (Main Thread) dial finish
            // (Main Thread) schedule to associate ImsPhoneConnection with
            //               GsmConnection on the main thread
            // If scheduling the CS redial occurs before the command to schedule the
            // ImsPhoneConnection to be  associated with the GsmConnection, the CS redial will occur
            // before GsmConnection has had callbacks to ImsPhone correctly updated. This will cause
            // Callbacks back to GsmCdmaPhone to never be set up correctly and we will lose track of
            // the instance.
            // Instead, schedule this redial to happen on the main thread, so that we know dial has
            // finished before scheduling a redial:
            // (Main thread) dial start
            // (Binder Thread) onCallSessionFailed -> move notify registrants to main thread
            // (Main Thread) dial finish
            // (Main Thread) schedule on main thread to associate ImsPhoneConnection with
            //               GsmConnection
            // (Main Thread) schedule a redial for CS
            mContext.getMainExecutor().execute(() -> {
                logd("initiateSilentRedial: notifying registrants, isEmergency=" + isEmergency
                        + ", eccCategory=" + eccCategory);
                mSilentRedialRegistrants.notifyRegistrants(ar);
            });
        }
    }

    @Override
    public void registerForSilentRedial(Handler h, int what, Object obj) {
        mSilentRedialRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForSilentRedial(Handler h) {
        mSilentRedialRegistrants.remove(h);
    }

    @Override
    public void registerForSuppServiceNotification(Handler h, int what, Object obj) {
        mSsnRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForSuppServiceNotification(Handler h) {
        mSsnRegistrants.remove(h);
    }

    @Override
    public int getSubId() {
        return mDefaultPhone.getSubId();
    }

    @Override
    public int getPhoneId() {
        return mDefaultPhone.getPhoneId();
    }

    private CallForwardInfo getCallForwardInfo(ImsCallForwardInfo info) {
        CallForwardInfo cfInfo = new CallForwardInfo();
        cfInfo.status = info.getStatus();
        cfInfo.reason = getCFReasonFromCondition(info.getCondition());
        cfInfo.serviceClass = SERVICE_CLASS_VOICE;
        cfInfo.toa = info.getToA();
        cfInfo.number = info.getNumber();
        cfInfo.timeSeconds = info.getTimeSeconds();
        return cfInfo;
    }

    @Override
    public String getLine1Number() {
        return mDefaultPhone.getLine1Number();
    }

    /**
     * Used to Convert ImsCallForwardInfo[] to CallForwardInfo[].
     * Update received call forward status to default IccRecords.
     */
    public CallForwardInfo[] handleCfQueryResult(ImsCallForwardInfo[] infos) {
        CallForwardInfo[] cfInfos = null;

        if (infos != null && infos.length != 0) {
            cfInfos = new CallForwardInfo[infos.length];
        }

        if (infos == null || infos.length == 0) {
            // Assume the default is not active
            // Set unconditional CFF in SIM to false
            setVoiceCallForwardingFlag(getIccRecords(), 1, false, null);
        } else {
            for (int i = 0, s = infos.length; i < s; i++) {
                if (infos[i].getCondition() == ImsUtInterface.CDIV_CF_UNCONDITIONAL) {
                    setVoiceCallForwardingFlag(getIccRecords(), 1, (infos[i].getStatus() == 1),
                        infos[i].getNumber());
                }
                cfInfos[i] = getCallForwardInfo(infos[i]);
            }
        }

        return cfInfos;
    }

    private int[] handleCbQueryResult(ImsSsInfo[] infos) {
        int[] cbInfos = new int[1];
        cbInfos[0] = SERVICE_CLASS_NONE;

        if (infos[0].getStatus() == 1) {
            cbInfos[0] = SERVICE_CLASS_VOICE;
        }

        return cbInfos;
    }

    private int[] handleCwQueryResult(ImsSsInfo[] infos) {
        int[] cwInfos = new int[2];
        cwInfos[0] = 0;

        if (infos[0].getStatus() == 1) {
            cwInfos[0] = 1;
            cwInfos[1] = SERVICE_CLASS_VOICE;
        }

        return cwInfos;
    }

    private void
    sendResponse(Message onComplete, Object result, Throwable e) {
        if (onComplete != null) {
            CommandException ex = null;
            if (e != null) {
                ex = getCommandException(e);
            }
            AsyncResult.forMessage(onComplete, result, ex);
            onComplete.sendToTarget();
        }
    }

    private void updateDataServiceState() {
        if (mSS != null && mDefaultPhone.getServiceStateTracker() != null
                && mDefaultPhone.getServiceStateTracker().mSS != null) {
            ServiceState ss = mDefaultPhone.getServiceStateTracker().mSS;
            mSS.setDataRegState(ss.getDataRegistrationState());
            List<NetworkRegistrationInfo> nriList =
                    ss.getNetworkRegistrationInfoListForDomain(NetworkRegistrationInfo.DOMAIN_PS);
            for (NetworkRegistrationInfo nri : nriList) {
                mSS.addNetworkRegistrationInfo(nri);
            }

            mSS.setIwlanPreferred(ss.isIwlanPreferred());
            logd("updateDataServiceState: defSs = " + ss + " imsSs = " + mSS);
        }
    }

    boolean isCsRetryException(Throwable e) {
        if ((e != null) && (e instanceof ImsException)
                && (((ImsException)e).getCode()
                    == ImsReasonInfo.CODE_LOCAL_CALL_CS_RETRY_REQUIRED)) {
            return true;
        }
        return false;
    }

    private Bundle setCsfbBundle(boolean isCsRetry) {
        Bundle b = new Bundle();
        b.putBoolean(CS_FALLBACK_SS, isCsRetry);
        return b;
    }

    private void sendResponseOrRetryOnCsfbSs(SS ss, int what, Throwable e, Object obj) {
        if (!isCsRetryException(e)) {
            sendResponse(ss.mOnComplete, obj, e);
            return;
        }

        Rlog.d(LOG_TAG, "Try CSFB: " + what);
        ss.mOnComplete.setData(setCsfbBundle(true));

        switch (what) {
            case EVENT_GET_CALL_FORWARD_DONE:
                mDefaultPhone.getCallForwardingOption(ss.mCfReason,
                                                      ss.mServiceClass,
                                                      ss.mOnComplete);
                break;
            case EVENT_SET_CALL_FORWARD_DONE:
                mDefaultPhone.setCallForwardingOption(ss.mCfAction,
                                                      ss.mCfReason,
                                                      ss.mDialingNumber,
                                                      ss.mServiceClass,
                                                      ss.mTimerSeconds,
                                                      ss.mOnComplete);
                break;
            case EVENT_GET_CALL_BARRING_DONE:
                mDefaultPhone.getCallBarring(ss.mFacility,
                                             ss.mPassword,
                                             ss.mOnComplete,
                                             ss.mServiceClass);
                break;
            case EVENT_SET_CALL_BARRING_DONE:
                mDefaultPhone.setCallBarring(ss.mFacility,
                                             ss.mLockState,
                                             ss.mPassword,
                                             ss.mOnComplete,
                                             ss.mServiceClass);
                break;
            case EVENT_GET_CALL_WAITING_DONE:
                mDefaultPhone.getCallWaiting(ss.mOnComplete);
                break;
            case EVENT_SET_CALL_WAITING_DONE:
                mDefaultPhone.setCallWaiting(ss.mEnable,
                                             ss.mServiceClass,
                                             ss.mOnComplete);
                break;
            case EVENT_GET_CLIR_DONE:
                mDefaultPhone.getOutgoingCallerIdDisplay(ss.mOnComplete);
                break;
            case EVENT_SET_CLIR_DONE:
                mDefaultPhone.setOutgoingCallerIdDisplay(ss.mClirMode, ss.mOnComplete);
                break;
            case EVENT_GET_CLIP_DONE:
                mDefaultPhone.queryCLIP(ss.mOnComplete);
                break;
            default:
                break;
        }
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;
        Message onComplete;
        SS ss = null;
        if (ar != null && ar.userObj instanceof SS) {
            ss = (SS) ar.userObj;
        }

        if (DBG) logd("handleMessage what=" + msg.what);
        switch (msg.what) {
            case EVENT_SET_CALL_FORWARD_DONE:
                if (ar.exception == null && ss != null &&
                    (ss.mCfReason == CF_REASON_UNCONDITIONAL)) {
                    setVoiceCallForwardingFlag(getIccRecords(), 1, isCfEnable(ss.mCfAction),
                                               ss.mDialingNumber);
                }
                if (ss != null) {
                    sendResponseOrRetryOnCsfbSs(ss, msg.what, ar.exception, null);
                }
                break;

            case EVENT_GET_CALL_FORWARD_DONE:
                CallForwardInfo[] cfInfos = null;
                if (ar.exception == null) {
                    cfInfos = handleCfQueryResult((ImsCallForwardInfo[])ar.result);
                }
                if (ss != null) {
                    sendResponseOrRetryOnCsfbSs(ss, msg.what, ar.exception, cfInfos);
                }
                break;

            case EVENT_GET_CALL_BARRING_DONE:
            case EVENT_GET_CALL_WAITING_DONE:
                int[] ssInfos = null;
                if (ar.exception == null) {
                    if (msg.what == EVENT_GET_CALL_BARRING_DONE) {
                        ssInfos = handleCbQueryResult((ImsSsInfo[])ar.result);
                    } else if (msg.what == EVENT_GET_CALL_WAITING_DONE) {
                        ssInfos = handleCwQueryResult((ImsSsInfo[])ar.result);
                    }
                }
                if (ss != null) {
                    sendResponseOrRetryOnCsfbSs(ss, msg.what, ar.exception, ssInfos);
                }
                break;

            case EVENT_GET_CLIR_DONE:
                ImsSsInfo ssInfo = (ImsSsInfo) ar.result;
                int[] clirInfo = null;
                if (ssInfo != null) {
                    // Unfortunately callers still use the old {n,m} format of ImsSsInfo, so return
                    // that for compatibility
                    clirInfo = ssInfo.getCompatArray(ImsSsData.SS_CLIR);
                }
                if (ss != null) {
                    sendResponseOrRetryOnCsfbSs(ss, msg.what, ar.exception, clirInfo);
                }
                break;

            case EVENT_GET_CLIP_DONE:
                ImsSsInfo ssInfoResp = null;
                if (ar.exception == null && ar.result instanceof ImsSsInfo) {
                    ssInfoResp = (ImsSsInfo) ar.result;
                }
                if (ss != null) {
                    sendResponseOrRetryOnCsfbSs(ss, msg.what, ar.exception, ssInfoResp);
                }
                break;

            case EVENT_SET_CLIR_DONE:
                if (ar.exception == null) {
                    if (ss != null) {
                        saveClirSetting(ss.mClirMode);
                    }
                }
                 // (Intentional fallthrough)
            case EVENT_SET_CALL_BARRING_DONE:
            case EVENT_SET_CALL_WAITING_DONE:
                if (ss != null) {
                    sendResponseOrRetryOnCsfbSs(ss, msg.what, ar.exception, null);
                }
                break;

            case EVENT_DEFAULT_PHONE_DATA_STATE_CHANGED:
                if (DBG) logd("EVENT_DEFAULT_PHONE_DATA_STATE_CHANGED");
                updateDataServiceState();
                break;

            case EVENT_SERVICE_STATE_CHANGED:
                if (VDBG) logd("EVENT_SERVICE_STATE_CHANGED");
                ar = (AsyncResult) msg.obj;
                ServiceState newServiceState = (ServiceState) ar.result;
                updateRoamingState(newServiceState);
                break;
            case EVENT_VOICE_CALL_ENDED:
                if (DBG) logd("Voice call ended. Handle pending updateRoamingState.");
                mCT.unregisterForVoiceCallEnded(this);
                // Get the current unmodified ServiceState from the tracker, as it has more info
                // about the cell roaming state.
                ServiceStateTracker sst = getDefaultPhone().getServiceStateTracker();
                if (sst != null) {
                    updateRoamingState(sst.mSS);
                }
                break;
            case EVENT_INITIATE_VOLTE_SILENT_REDIAL: {
                // This is a CS -> IMS redial
                if (VDBG) logd("EVENT_INITIATE_VOLTE_SILENT_REDIAL");
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null && ar.result != null) {
                    SilentRedialParam result = (SilentRedialParam) ar.result;
                    String dialString = result.dialString;
                    int causeCode = result.causeCode;
                    DialArgs dialArgs = result.dialArgs;
                    if (VDBG) logd("dialString=" + dialString + " causeCode=" + causeCode);

                    try {
                        Connection cn = dial(dialString,
                                updateDialArgsForVolteSilentRedial(dialArgs, causeCode));
                        // The GSM/CDMA Connection that is owned by the GsmCdmaPhone is currently
                        // the one with a callback registered to TelephonyConnection. Notify the
                        // redial happened over that Phone so that it can be replaced with the
                        // new ImsPhoneConnection.
                        Rlog.d(LOG_TAG, "Notify volte redial connection changed cn: " + cn);
                        if (mDefaultPhone != null) {
                            // don't care it is null or not.
                            mDefaultPhone.notifyRedialConnectionChanged(cn);
                        }
                    } catch (CallStateException e) {
                        Rlog.e(LOG_TAG, "volte silent redial failed: " + e);
                        if (mDefaultPhone != null) {
                            mDefaultPhone.notifyRedialConnectionChanged(null);
                        }
                    }
                } else {
                    if (VDBG) logd("EVENT_INITIATE_VOLTE_SILENT_REDIAL" +
                                   " has exception or empty result");
                }
                break;
            }

            default:
                super.handleMessage(msg);
                break;
        }
    }

    /**
     * Listen to the IMS ECBM state change
     */
    private ImsEcbmStateListener mImsEcbmStateListener =
            new ImsEcbmStateListener(mContext.getMainExecutor()) {
                @Override
                public void onECBMEntered(Executor executor) {
                    if (DBG) logd("onECBMEntered");

                    TelephonyUtils.runWithCleanCallingIdentity(()->
                            handleEnterEmergencyCallbackMode(), executor);
                }



                @Override
                public void onECBMExited(Executor executor) {
                    if (DBG) logd("onECBMExited");
                    TelephonyUtils.runWithCleanCallingIdentity(()->
                            handleExitEmergencyCallbackMode(), executor);
                }
            };

    @VisibleForTesting
    public ImsEcbmStateListener getImsEcbmStateListener() {
        return mImsEcbmStateListener;
    }

    @Override
    public boolean isInEmergencyCall() {
        return mCT.isInEmergencyCall();
    }

    private void sendEmergencyCallbackModeChange() {
        // Send an Intent
        Intent intent = new Intent(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
        intent.putExtra(TelephonyManager.EXTRA_PHONE_IN_ECM_STATE, isInEcm());
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, getPhoneId());
        mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        if (DBG) logd("sendEmergencyCallbackModeChange: isInEcm=" + isInEcm());
    }

    @Override
    public void exitEmergencyCallbackMode() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        if (DBG) logd("exitEmergencyCallbackMode()");

        // Send a message which will invoke handleExitEmergencyCallbackMode
        ImsEcbm ecbm;
        try {
            ecbm = mCT.getEcbmInterface();
            ecbm.exitEmergencyCallbackMode();
        } catch (ImsException e) {
            e.printStackTrace();
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void handleEnterEmergencyCallbackMode() {
        if (DBG) logd("handleEnterEmergencyCallbackMode,mIsPhoneInEcmState= " + isInEcm());
        // if phone is not in Ecm mode, and it's changed to Ecm mode
        if (!isInEcm()) {
            setIsInEcm(true);
            // notify change
            sendEmergencyCallbackModeChange();
            ((GsmCdmaPhone) mDefaultPhone).notifyEmergencyCallRegistrants(true);

            // Post this runnable so we will automatically exit
            // if no one invokes exitEmergencyCallbackMode() directly.
            long delayInMillis = TelephonyProperties.ecm_exit_timer()
                    .orElse(DEFAULT_ECM_EXIT_TIMER_VALUE);
            postDelayed(mExitEcmRunnable, delayInMillis);
            // We don't want to go to sleep while in Ecm
            mWakeLock.acquire();
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    protected void handleExitEmergencyCallbackMode() {
        if (DBG) logd("handleExitEmergencyCallbackMode: mIsPhoneInEcmState = " + isInEcm());

        if (isInEcm()) {
            setIsInEcm(false);
        }

        // Remove pending exit Ecm runnable, if any
        removeCallbacks(mExitEcmRunnable);

        if (mEcmExitRespRegistrant != null) {
            mEcmExitRespRegistrant.notifyResult(Boolean.TRUE);
        }

        // release wakeLock
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        // send an Intent
        sendEmergencyCallbackModeChange();
        ((GsmCdmaPhone) mDefaultPhone).notifyEmergencyCallRegistrants(false);
    }

    /**
     * Handle to cancel or restart Ecm timer in emergency call back mode if action is
     * CANCEL_ECM_TIMER, cancel Ecm timer and notify apps the timer is canceled; otherwise, restart
     * Ecm timer and notify apps the timer is restarted.
     */
    void handleTimerInEmergencyCallbackMode(int action) {
        switch (action) {
            case CANCEL_ECM_TIMER:
                removeCallbacks(mExitEcmRunnable);
                ((GsmCdmaPhone) mDefaultPhone).notifyEcbmTimerReset(Boolean.TRUE);
                setEcmCanceledForEmergency(true /*isCanceled*/);
                break;
            case RESTART_ECM_TIMER:
                long delayInMillis = TelephonyProperties.ecm_exit_timer()
                        .orElse(DEFAULT_ECM_EXIT_TIMER_VALUE);
                postDelayed(mExitEcmRunnable, delayInMillis);
                ((GsmCdmaPhone) mDefaultPhone).notifyEcbmTimerReset(Boolean.FALSE);
                setEcmCanceledForEmergency(false /*isCanceled*/);
                break;
            default:
                loge("handleTimerInEmergencyCallbackMode, unsupported action " + action);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public void setOnEcbModeExitResponse(Handler h, int what, Object obj) {
        mEcmExitRespRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unsetOnEcbModeExitResponse(Handler h) {
        mEcmExitRespRegistrant.clear();
    }

    public void onFeatureCapabilityChanged() {
        mDefaultPhone.getServiceStateTracker().onImsCapabilityChanged();
    }

    @Override
    public boolean isImsCapabilityAvailable(int capability, int regTech) throws ImsException {
        return mCT.isImsCapabilityAvailable(capability, regTech);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public boolean isVolteEnabled() {
        return isVoiceOverCellularImsEnabled();
    }

    @Override
    public boolean isVoiceOverCellularImsEnabled() {
        return mCT.isVoiceOverCellularImsEnabled();
    }

    @Override
    public boolean isWifiCallingEnabled() {
        return mCT.isVowifiEnabled();
    }

    @Override
    public boolean isVideoEnabled() {
        return mCT.isVideoCallEnabled();
    }

    @Override
    public int getImsRegistrationTech() {
        return mCT.getImsRegistrationTech();
    }

    @Override
    public void getImsRegistrationTech(Consumer<Integer> callback) {
        mCT.getImsRegistrationTech(callback);
    }

    @Override
    public void getImsRegistrationState(Consumer<Integer> callback) {
        callback.accept(mImsMmTelRegistrationHelper.getImsRegistrationState());
    }

    @Override
    public Phone getDefaultPhone() {
        return mDefaultPhone;
    }

    @Override
    public boolean isImsRegistered() {
        return mImsMmTelRegistrationHelper.isImsRegistered();
    }

    // Not used, but not removed due to UnsupportedAppUsage tag.
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void setImsRegistered(boolean isRegistered) {
        mImsMmTelRegistrationHelper.updateRegistrationState(
                isRegistered ? RegistrationManager.REGISTRATION_STATE_REGISTERED :
                        RegistrationManager.REGISTRATION_STATE_NOT_REGISTERED);
    }

    @Override
    public void callEndCleanupHandOverCallIfAny() {
        mCT.callEndCleanupHandOverCallIfAny();
    }

    private BroadcastReceiver mResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Add notification only if alert was not shown by WfcSettings
            if (getResultCode() == Activity.RESULT_OK) {
                // Default result code (as passed to sendOrderedBroadcast)
                // means that intent was not received by WfcSettings.

                CharSequence title =
                        intent.getCharSequenceExtra(EXTRA_WFC_REGISTRATION_FAILURE_TITLE);
                CharSequence messageAlert =
                        intent.getCharSequenceExtra(EXTRA_WFC_REGISTRATION_FAILURE_MESSAGE);
                CharSequence messageNotification =
                        intent.getCharSequenceExtra(EXTRA_KEY_NOTIFICATION_MESSAGE);

                Intent resultIntent = new Intent(Intent.ACTION_MAIN);
                // Note: If the classname below is ever removed, the call to
                // PendingIntent.getActivity should also specify FLAG_IMMUTABLE to ensure the
                // pending intent cannot be tampered with.
                resultIntent.setClassName("com.android.settings",
                        "com.android.settings.Settings$WifiCallingSettingsActivity");
                resultIntent.putExtra(EXTRA_KEY_ALERT_SHOW, true);
                resultIntent.putExtra(EXTRA_WFC_REGISTRATION_FAILURE_TITLE, title);
                resultIntent.putExtra(EXTRA_WFC_REGISTRATION_FAILURE_MESSAGE, messageAlert);
                PendingIntent resultPendingIntent =
                        PendingIntent.getActivity(
                                mContext,
                                0,
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        );

                final Notification notification = new Notification.Builder(mContext)
                                .setSmallIcon(android.R.drawable.stat_sys_warning)
                                .setContentTitle(title)
                                .setContentText(messageNotification)
                                .setAutoCancel(true)
                                .setContentIntent(resultPendingIntent)
                                .setStyle(new Notification.BigTextStyle()
                                .bigText(messageNotification))
                                .setChannelId(NotificationChannelController.CHANNEL_ID_WFC)
                                .build();
                final String notificationTag = "wifi_calling";
                final int notificationId = 1;

                NotificationManager notificationManager =
                        (NotificationManager) mContext.getSystemService(
                                Context.NOTIFICATION_SERVICE);
                notificationManager.notify(notificationTag, notificationId,
                        notification);
            }
        }
    };

    /**
     * Show notification in case of some error codes.
     */
    public void processDisconnectReason(ImsReasonInfo imsReasonInfo) {
        if (imsReasonInfo.mCode == imsReasonInfo.CODE_REGISTRATION_ERROR
                && imsReasonInfo.mExtraMessage != null) {
            // Suppress WFC Registration notifications if WFC is not enabled by the user.
            if (mImsManagerFactory.create(mContext, mPhoneId).isWfcEnabledByUser()) {
                processWfcDisconnectForNotification(imsReasonInfo);
            }
        }
    }

    // Processes an IMS disconnect cause for possible WFC registration errors and optionally
    // disable WFC.
    private void processWfcDisconnectForNotification(ImsReasonInfo imsReasonInfo) {
        CarrierConfigManager configManager =
                (CarrierConfigManager) mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager == null) {
            loge("processDisconnectReason: CarrierConfigManager is not ready");
            return;
        }
        PersistableBundle pb = configManager.getConfigForSubId(getSubId());
        if (pb == null) {
            loge("processDisconnectReason: no config for subId " + getSubId());
            return;
        }
        final String[] wfcOperatorErrorCodes =
                pb.getStringArray(
                        CarrierConfigManager.KEY_WFC_OPERATOR_ERROR_CODES_STRING_ARRAY);
        if (wfcOperatorErrorCodes == null) {
            // no operator-specific error codes
            return;
        }

        final String[] wfcOperatorErrorAlertMessages =
                mContext.getResources().getStringArray(
                        com.android.internal.R.array.wfcOperatorErrorAlertMessages);
        final String[] wfcOperatorErrorNotificationMessages =
                mContext.getResources().getStringArray(
                        com.android.internal.R.array.wfcOperatorErrorNotificationMessages);

        for (int i = 0; i < wfcOperatorErrorCodes.length; i++) {
            String[] codes = wfcOperatorErrorCodes[i].split("\\|");
            if (codes.length != 2) {
                loge("Invalid carrier config: " + wfcOperatorErrorCodes[i]);
                continue;
            }

            // Match error code.
            if (!imsReasonInfo.mExtraMessage.startsWith(
                    codes[0])) {
                continue;
            }
            // If there is no delimiter at the end of error code string
            // then we need to verify that we are not matching partial code.
            // EXAMPLE: "REG9" must not match "REG99".
            // NOTE: Error code must not be empty.
            int codeStringLength = codes[0].length();
            char lastChar = codes[0].charAt(codeStringLength - 1);
            if (Character.isLetterOrDigit(lastChar)) {
                if (imsReasonInfo.mExtraMessage.length() > codeStringLength) {
                    char nextChar = imsReasonInfo.mExtraMessage.charAt(codeStringLength);
                    if (Character.isLetterOrDigit(nextChar)) {
                        continue;
                    }
                }
            }

            final CharSequence title = mContext.getText(
                    com.android.internal.R.string.wfcRegErrorTitle);

            int idx = Integer.parseInt(codes[1]);
            if (idx < 0
                    || idx >= wfcOperatorErrorAlertMessages.length
                    || idx >= wfcOperatorErrorNotificationMessages.length) {
                loge("Invalid index: " + wfcOperatorErrorCodes[i]);
                continue;
            }
            String messageAlert = imsReasonInfo.mExtraMessage;
            String messageNotification = imsReasonInfo.mExtraMessage;
            if (!wfcOperatorErrorAlertMessages[idx].isEmpty()) {
                messageAlert = String.format(
                        wfcOperatorErrorAlertMessages[idx],
                        imsReasonInfo.mExtraMessage); // Fill IMS error code into alert message
            }
            if (!wfcOperatorErrorNotificationMessages[idx].isEmpty()) {
                messageNotification = String.format(
                        wfcOperatorErrorNotificationMessages[idx],
                        imsReasonInfo.mExtraMessage); // Fill IMS error code into notification
            }

            // If WfcSettings are active then alert will be shown
            // otherwise notification will be added.
            Intent intent = new Intent(
                    android.telephony.ims.ImsManager.ACTION_WFC_IMS_REGISTRATION_ERROR);
            intent.putExtra(EXTRA_WFC_REGISTRATION_FAILURE_TITLE, title);
            intent.putExtra(EXTRA_WFC_REGISTRATION_FAILURE_MESSAGE, messageAlert);
            intent.putExtra(EXTRA_KEY_NOTIFICATION_MESSAGE, messageNotification);
            mContext.sendOrderedBroadcast(intent, null, mResultReceiver,
                    null, Activity.RESULT_OK, null, null);

            // We can only match a single error code
            // so should break the loop after a successful match.
            break;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    public boolean isUtEnabled() {
        return mCT.isUtEnabled();
    }

    @Override
    public void sendEmergencyCallStateChange(boolean callActive) {
        mDefaultPhone.sendEmergencyCallStateChange(callActive);
    }

    @Override
    public void setBroadcastEmergencyCallStateChanges(boolean broadcast) {
        mDefaultPhone.setBroadcastEmergencyCallStateChanges(broadcast);
    }

    @VisibleForTesting
    public PowerManager.WakeLock getWakeLock() {
        return mWakeLock;
    }

    /**
     * Update roaming state and WFC mode in the following situations:
     *     1) voice is in service.
     *     2) data is in service and it is not IWLAN (if in legacy mode).
     * @param ss non-null ServiceState
     */
    private void updateRoamingState(ServiceState ss) {
        if (ss == null) {
            loge("updateRoamingState: null ServiceState!");
            return;
        }
        boolean newRoamingState = ss.getRoaming();
        // Do not recalculate if there is no change to state.
        if (mLastKnownRoamingState == newRoamingState) {
            return;
        }
        boolean isInService = (ss.getState() == ServiceState.STATE_IN_SERVICE
                || ss.getDataRegistrationState() == ServiceState.STATE_IN_SERVICE);
        // If we are not IN_SERVICE for voice or data, ignore change roaming state, as we always
        // move to home in this case.
        if (!isInService || !mDefaultPhone.isRadioOn()) {
            logi("updateRoamingState: we are not IN_SERVICE, ignoring roaming change.");
            return;
        }
        // We ignore roaming changes when moving to IWLAN because it always sets the roaming
        // mode to home and masks the actual cellular roaming status if voice is not registered. If
        // we just moved to IWLAN because WFC roaming mode is IWLAN preferred and WFC home mode is
        // cell preferred, we can get into a condition where the modem keeps bouncing between
        // IWLAN->cell->IWLAN->cell...
        if (isCsNotInServiceAndPsWwanReportingWlan(ss)) {
            logi("updateRoamingState: IWLAN masking roaming, ignore roaming change.");
            return;
        }
        if (mCT.getState() == PhoneConstants.State.IDLE) {
            if (DBG) logd("updateRoamingState now: " + newRoamingState);
            mLastKnownRoamingState = newRoamingState;
            CarrierConfigManager configManager = (CarrierConfigManager)
                    getContext().getSystemService(Context.CARRIER_CONFIG_SERVICE);
            // Don't set wfc mode if carrierconfig has not loaded. It will be set by GsmCdmaPhone
            // when receives ACTION_CARRIER_CONFIG_CHANGED broadcast.
            if (configManager != null && CarrierConfigManager.isConfigForIdentifiedCarrier(
                    configManager.getConfigForSubId(getSubId()))) {
                ImsManager imsManager = mImsManagerFactory.create(mContext, mPhoneId);
                imsManager.setWfcMode(imsManager.getWfcMode(newRoamingState), newRoamingState);
            }
        } else {
            if (DBG) logd("updateRoamingState postponed: " + newRoamingState);
            mCT.registerForVoiceCallEnded(this, EVENT_VOICE_CALL_ENDED, null);
        }
    }

    /**
     * In legacy mode, data registration will report IWLAN when we are using WLAN for data,
     * effectively masking the true roaming state of the device if voice is not registered.
     *
     * @return true if we are reporting not in service for CS domain over WWAN transport and WLAN
     * for PS domain over WWAN transport.
     */
    private boolean isCsNotInServiceAndPsWwanReportingWlan(ServiceState ss) {
        // We can not get into this condition if we are in AP-Assisted mode.
        if (mDefaultPhone.getAccessNetworksManager() == null
                || !mDefaultPhone.getAccessNetworksManager().isInLegacyMode()) {
            return false;
        }
        NetworkRegistrationInfo csInfo = ss.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_CS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        NetworkRegistrationInfo psInfo = ss.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
        // We will return roaming state correctly if the CS domain is in service because
        // ss.getRoaming() returns isVoiceRoaming||isDataRoaming result and isDataRoaming==false
        // when the modem reports IWLAN RAT.
        return psInfo != null && csInfo != null && !csInfo.isInService()
                && psInfo.getAccessNetworkTechnology() == TelephonyManager.NETWORK_TYPE_IWLAN;
    }

    public RegistrationManager.RegistrationCallback getImsMmTelRegistrationCallback() {
        return mImsMmTelRegistrationHelper.getCallback();
    }

    /**
     * Reset the IMS registration state.
     */
    public void resetImsRegistrationState() {
        if (DBG) logd("resetImsRegistrationState");
        mImsMmTelRegistrationHelper.reset();
    }

    private ImsRegistrationCallbackHelper.ImsRegistrationUpdate mMmTelRegistrationUpdate = new
            ImsRegistrationCallbackHelper.ImsRegistrationUpdate() {
        @Override
        public void handleImsRegistered(int imsRadioTech) {
            if (DBG) {
                logd("handleImsRegistered: onImsMmTelConnected imsRadioTech="
                        + AccessNetworkConstants.transportTypeToString(imsRadioTech));
            }
            mRegLocalLog.log("handleImsRegistered: onImsMmTelConnected imsRadioTech="
                    + AccessNetworkConstants.transportTypeToString(imsRadioTech));
            setServiceState(ServiceState.STATE_IN_SERVICE);
            getDefaultPhone().setImsRegistrationState(true);
            mMetrics.writeOnImsConnectionState(mPhoneId, ImsConnectionState.State.CONNECTED, null);
            mImsStats.onImsRegistered(imsRadioTech);
        }

        @Override
        public void handleImsRegistering(int imsRadioTech) {
            if (DBG) {
                logd("handleImsRegistering: onImsMmTelProgressing imsRadioTech="
                        + AccessNetworkConstants.transportTypeToString(imsRadioTech));
            }
            mRegLocalLog.log("handleImsRegistering: onImsMmTelProgressing imsRadioTech="
                    + AccessNetworkConstants.transportTypeToString(imsRadioTech));
            setServiceState(ServiceState.STATE_OUT_OF_SERVICE);
            getDefaultPhone().setImsRegistrationState(false);
            mMetrics.writeOnImsConnectionState(mPhoneId, ImsConnectionState.State.PROGRESSING,
                    null);
            mImsStats.onImsRegistering(imsRadioTech);
        }

        @Override
        public void handleImsUnregistered(ImsReasonInfo imsReasonInfo) {
            if (DBG) {
                logd("handleImsUnregistered: onImsMmTelDisconnected imsReasonInfo="
                        + imsReasonInfo);
            }
            mRegLocalLog.log("handleImsUnregistered: onImsMmTelDisconnected imsRadioTech="
                    + imsReasonInfo);
            setServiceState(ServiceState.STATE_OUT_OF_SERVICE);
            processDisconnectReason(imsReasonInfo);
            getDefaultPhone().setImsRegistrationState(false);
            mMetrics.writeOnImsConnectionState(mPhoneId, ImsConnectionState.State.DISCONNECTED,
                    imsReasonInfo);
            mImsStats.onImsUnregistered(imsReasonInfo);
        }

        @Override
        public void handleImsSubscriberAssociatedUriChanged(Uri[] uris) {
            if (DBG) logd("handleImsSubscriberAssociatedUriChanged");
            setCurrentSubscriberUris(uris);
            setPhoneNumberForSourceIms(uris);
        }
    };

    /** Sets the IMS phone number from IMS associated URIs, if any found. */
    @VisibleForTesting
    public void setPhoneNumberForSourceIms(Uri[] uris) {
        String phoneNumber = extractPhoneNumberFromAssociatedUris(uris);
        if (phoneNumber == null) {
            return;
        }
        int subId = getSubId();
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            // Defending b/219080264:
            // SubscriptionController.setSubscriptionProperty validates input subId
            // so do not proceed if subId invalid. This may be happening because cached
            // IMS callbacks are sent back to telephony after SIM state changed.
            return;
        }
        SubscriptionController subController = SubscriptionController.getInstance();
        String countryIso = getCountryIso(subController, subId);
        // Format the number as one more defense to reject garbage values:
        // phoneNumber will become null.
        phoneNumber = PhoneNumberUtils.formatNumberToE164(phoneNumber, countryIso);
        if (phoneNumber == null) {
            return;
        }
        subController.setSubscriptionProperty(subId, COLUMN_PHONE_NUMBER_SOURCE_IMS, phoneNumber);
    }

    private static String getCountryIso(SubscriptionController subController, int subId) {
        SubscriptionInfo info = subController.getSubscriptionInfo(subId);
        String countryIso = info == null ? "" : info.getCountryIso();
        // info.getCountryIso() may return null
        return countryIso == null ? "" : countryIso;
    }

    /**
     * Finds the phone number from associated URIs.
     *
     * <p>Associated URIs are public user identities, and phone number could be used:
     * see 3GPP TS 24.229 5.4.1.2 and 3GPP TS 23.003 13.4. This algotihm look for the
     * possible "global number" in E.164 format.
     */
    private static String extractPhoneNumberFromAssociatedUris(Uri[] uris) {
        if (uris == null) {
            return null;
        }
        return Arrays.stream(uris)
                // Phone number is an opaque URI "tel:<phone-number>" or "sip:<phone-number>@<...>"
                .filter(u -> u != null && u.isOpaque())
                .filter(u -> "tel".equalsIgnoreCase(u.getScheme())
                        || "sip".equalsIgnoreCase(u.getScheme()))
                .map(Uri::getSchemeSpecificPart)
                // "Global number" should be in E.164 format starting with "+" e.g. "+447539447777"
                .filter(ssp -> ssp != null && ssp.startsWith("+"))
                // Remove whatever after "@" for sip URI
                .map(ssp -> ssp.split("@")[0])
                // Returns the first winner
                .findFirst()
                .orElse(null);
    }

    public IccRecords getIccRecords() {
        return mDefaultPhone.getIccRecords();
    }

    public DialArgs updateDialArgsForVolteSilentRedial(DialArgs dialArgs, int causeCode) {
        if (dialArgs != null) {
            ImsPhone.ImsDialArgs.Builder imsDialArgsBuilder;
            imsDialArgsBuilder = ImsPhone.ImsDialArgs.Builder.from(dialArgs);

            Bundle extras = new Bundle(dialArgs.intentExtras);
            if (causeCode == CallFailCause.EMC_REDIAL_ON_VOWIFI) {
                extras.putString(ImsCallProfile.EXTRA_CALL_RAT_TYPE,
                        String.valueOf(ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN));
                logd("trigger VoWifi emergency call");
                imsDialArgsBuilder.setIntentExtras(extras);
            } else if (causeCode == CallFailCause.EMC_REDIAL_ON_IMS) {
                logd("trigger VoLte emergency call");
            }
            return imsDialArgsBuilder.build();
        }
        return new DialArgs.Builder<>().build();
    }

    @Override
    public VoiceCallSessionStats getVoiceCallSessionStats() {
        return mDefaultPhone.getVoiceCallSessionStats();
    }

    /** Returns the {@link ImsStats} for this IMS phone. */
    public ImsStats getImsStats() {
        return mImsStats;
    }

    /** Sets the {@link ImsStats} mock for this IMS phone during unit testing. */
    @VisibleForTesting
    public void setImsStats(ImsStats imsStats) {
        mImsStats = imsStats;
    }

    public boolean hasAliveCall() {
        return (getForegroundCall().getState() != Call.State.IDLE ||
                getBackgroundCall().getState() != Call.State.IDLE);
    }

    public boolean getLastKnownRoamingState() {
        return mLastKnownRoamingState;
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println("ImsPhone extends:");
        super.dump(fd, pw, args);
        pw.flush();

        pw.println("ImsPhone:");
        pw.println("  mDefaultPhone = " + mDefaultPhone);
        pw.println("  mPendingMMIs = " + mPendingMMIs);
        pw.println("  mPostDialHandler = " + mPostDialHandler);
        pw.println("  mSS = " + mSS);
        pw.println("  mWakeLock = " + mWakeLock);
        pw.println("  mIsPhoneInEcmState = " + isInEcm());
        pw.println("  mEcmExitRespRegistrant = " + mEcmExitRespRegistrant);
        pw.println("  mSilentRedialRegistrants = " + mSilentRedialRegistrants);
        pw.println("  mImsMmTelRegistrationState = "
                + mImsMmTelRegistrationHelper.getImsRegistrationState());
        pw.println("  mLastKnownRoamingState = " + mLastKnownRoamingState);
        pw.println("  mSsnRegistrants = " + mSsnRegistrants);
        pw.println(" Registration Log:");
        pw.increaseIndent();
        mRegLocalLog.dump(pw);
        pw.decreaseIndent();
        pw.flush();
    }

    private void logi(String s) {
        Rlog.i(LOG_TAG, "[" + mPhoneId + "] " + s);
    }

    private void logv(String s) {
        Rlog.v(LOG_TAG, "[" + mPhoneId + "] " + s);
    }

    private void logd(String s) {
        Rlog.d(LOG_TAG, "[" + mPhoneId + "] " + s);
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, "[" + mPhoneId + "] " + s);
    }
}
