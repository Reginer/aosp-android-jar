/*
 * Copyright (C) 2006, 2012 The Android Open Source Project
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

package com.android.internal.telephony.uicc;

import android.annotation.NonNull;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.util.IndentingPrintWriter;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.PersoSubState;
import com.android.internal.telephony.uicc.IccCardStatus.PinState;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * {@hide}
 */
public class UiccCardApplication {
    private static final String LOG_TAG = "UiccCardApplication";
    private static final boolean DBG = true;

    private static final int EVENT_PIN1_PUK1_DONE = 1;
    private static final int EVENT_CHANGE_PIN1_DONE = 2;
    private static final int EVENT_CHANGE_PIN2_DONE = 3;
    private static final int EVENT_QUERY_FACILITY_FDN_DONE = 4;
    private static final int EVENT_CHANGE_FACILITY_FDN_DONE = 5;
    private static final int EVENT_QUERY_FACILITY_LOCK_DONE = 6;
    private static final int EVENT_CHANGE_FACILITY_LOCK_DONE = 7;
    private static final int EVENT_PIN2_PUK2_DONE = 8;
    private static final int EVENT_RADIO_UNAVAILABLE = 9;

    /**
     * These values are for authContext (parameter P2) per 3GPP TS 31.102 (Section 7.1.2)
     */
    public static final int AUTH_CONTEXT_EAP_SIM = PhoneConstants.AUTH_CONTEXT_EAP_SIM;
    public static final int AUTH_CONTEXT_EAP_AKA = PhoneConstants.AUTH_CONTEXT_EAP_AKA;
    public static final int AUTH_CONTEXT_GBA_BOOTSTRAP = PhoneConstants.AUTH_CONTEXT_GBA_BOOTSTRAP;
    public static final int AUTHTYPE_GBA_NAF_KEY_EXTERNAL =
            PhoneConstants.AUTHTYPE_GBA_NAF_KEY_EXTERNAL;
    public static final int AUTH_CONTEXT_UNDEFINED = PhoneConstants.AUTH_CONTEXT_UNDEFINED;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final Object  mLock = new Object();
    private UiccProfile   mUiccProfile; //parent
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private AppState      mAppState;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private AppType       mAppType;
    private int           mAuthContext;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private PersoSubState mPersoSubState;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private String        mAid;
    private String        mAppLabel;
    private boolean       mPin1Replaced;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private PinState      mPin1State;
    private PinState      mPin2State;
    private boolean       mIccFdnEnabled;
    private boolean       mDesiredFdnEnabled;
    private boolean       mIccLockEnabled;
    private boolean       mDesiredPinLocked;

    // App state will be ignored while deciding whether the card is ready or not.
    private boolean       mIgnoreApp;
    private boolean       mIccFdnAvailable = true; // Default is enabled.

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private CommandsInterface mCi;
    private Context mContext;
    private IccRecords mIccRecords;
    private IccFileHandler mIccFh;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean mDestroyed;//set to true once this App is commanded to be disposed of.

    private RegistrantList mReadyRegistrants = new RegistrantList();
    private RegistrantList mDetectedRegistrants = new RegistrantList();
    private RegistrantList mPinLockedRegistrants = new RegistrantList();
    private RegistrantList mNetworkLockedRegistrants = new RegistrantList();

    @NonNull
    private final FeatureFlags mFeatureFlags;

    public UiccCardApplication(@NonNull UiccProfile uiccProfile,
            @NonNull IccCardApplicationStatus as, @NonNull Context c, @NonNull CommandsInterface ci,
            @NonNull FeatureFlags flags) {
        if (DBG) log("Creating UiccApp: " + as);
        mFeatureFlags = flags;
        mUiccProfile = uiccProfile;
        mAppState = as.app_state;
        mAppType = as.app_type;
        mAuthContext = getAuthContext(mAppType);
        mPersoSubState = as.perso_substate;
        mAid = as.aid;
        mAppLabel = as.app_label;
        mPin1Replaced = as.pin1_replaced;
        mPin1State = as.pin1;
        mPin2State = as.pin2;
        mIgnoreApp = false;

        mContext = c;
        mCi = ci;

        mIccFh = createIccFileHandler(as.app_type);
        mIccRecords = createIccRecords(as.app_type, mContext, mCi);
        if (mAppState == AppState.APPSTATE_READY) {
            queryFdn();
            queryPin1State();
        }
        mCi.registerForNotAvailable(mHandler, EVENT_RADIO_UNAVAILABLE, null);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void update (IccCardApplicationStatus as, Context c, CommandsInterface ci) {
        synchronized (mLock) {
            if (mDestroyed) {
                loge("Application updated after destroyed! Fix me!");
                return;
            }

            if (DBG) log(mAppType + " update. New " + as);
            mContext = c;
            mCi = ci;
            AppType oldAppType = mAppType;
            AppState oldAppState = mAppState;
            PersoSubState oldPersoSubState = mPersoSubState;
            PinState oldPin1State = mPin1State;
            mAppType = as.app_type;
            mAuthContext = getAuthContext(mAppType);
            mAppState = as.app_state;
            mPersoSubState = as.perso_substate;
            mAid = as.aid;
            mAppLabel = as.app_label;
            mPin1Replaced = as.pin1_replaced;
            mPin1State = as.pin1;
            mPin2State = as.pin2;

            if (mAppType != oldAppType) {
                if (mIccFh != null) { mIccFh.dispose();}
                if (mIccRecords != null) { mIccRecords.dispose();}
                mIccFh = createIccFileHandler(as.app_type);
                mIccRecords = createIccRecords(as.app_type, c, ci);
            }

            if (mPersoSubState != oldPersoSubState &&
                    PersoSubState.isPersoLocked(mPersoSubState)) {
                notifyNetworkLockedRegistrantsIfNeeded(null);
            }

            if (mAppState != oldAppState) {
                if (DBG) log(oldAppType + " changed state: " + oldAppState + " -> " + mAppState);
                // If the app state turns to APPSTATE_READY, then query FDN status,
                //as it might have failed in earlier attempt.
                if (mAppState == AppState.APPSTATE_READY) {
                    queryFdn();
                    queryPin1State();
                }
                notifyPinLockedRegistrantsIfNeeded(null);
                notifyReadyRegistrantsIfNeeded(null);
                notifyDetectedRegistrantsIfNeeded(null);
            } else {
                if (mPin1State != oldPin1State)
                    queryPin1State();
            }
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    void dispose() {
        synchronized (mLock) {
            if (DBG) log(mAppType + " being Disposed");
            mDestroyed = true;
            if (mIccRecords != null) { mIccRecords.dispose();}
            if (mIccFh != null) { mIccFh.dispose();}
            mIccRecords = null;
            mIccFh = null;
            mCi.unregisterForNotAvailable(mHandler);
        }
    }

    private IccRecords createIccRecords(AppType type, Context c, CommandsInterface ci) {
        if (type == AppType.APPTYPE_USIM || type == AppType.APPTYPE_SIM) {
            return new SIMRecords(this, c, ci);
        } else if (type == AppType.APPTYPE_RUIM || type == AppType.APPTYPE_CSIM){
            return new RuimRecords(this, c, ci);
        } else if (type == AppType.APPTYPE_ISIM) {
            return new IsimUiccRecords(this, c, ci, mFeatureFlags);
        } else {
            // Unknown app type (maybe detection is still in progress)
            return null;
        }
    }

    private IccFileHandler createIccFileHandler(AppType type) {
        switch (type) {
            case APPTYPE_SIM:
                return new SIMFileHandler(this, mAid, mCi);
            case APPTYPE_RUIM:
                return new RuimFileHandler(this, mAid, mCi);
            case APPTYPE_USIM:
                return new UsimFileHandler(this, mAid, mCi);
            case APPTYPE_CSIM:
                return new CsimFileHandler(this, mAid, mCi);
            case APPTYPE_ISIM:
                return new IsimFileHandler(this, mAid, mCi);
            default:
                return null;
        }
    }

    /** Assumes mLock is held. */
    public void queryFdn() {
        //This shouldn't change run-time. So needs to be called only once.
        int serviceClassX;

        serviceClassX = CommandsInterface.SERVICE_CLASS_VOICE +
                        CommandsInterface.SERVICE_CLASS_DATA +
                        CommandsInterface.SERVICE_CLASS_FAX;
        mCi.queryFacilityLockForApp (
                CommandsInterface.CB_FACILITY_BA_FD, "", serviceClassX,
                mAid, mHandler.obtainMessage(EVENT_QUERY_FACILITY_FDN_DONE));
    }
    /**
     * Interpret EVENT_QUERY_FACILITY_LOCK_DONE
     * @param ar is asyncResult of Query_Facility_Locked
     */
    private void onQueryFdnEnabled(AsyncResult ar) {
        synchronized (mLock) {
            if (ar.exception != null) {
                if (DBG) log("Error in querying facility lock:" + ar.exception);
                return;
            }

            int[] result = (int[])ar.result;
            if(result.length != 0) {
                //0 - Available & Disabled, 1-Available & Enabled, 2-Unavailable.
                if (result[0] == 2) {
                    mIccFdnEnabled = false;
                    mIccFdnAvailable = false;
                } else {
                    mIccFdnEnabled = (result[0] == 1) ? true : false;
                    mIccFdnAvailable = true;
                }
                log("Query facility FDN : FDN service available: "+ mIccFdnAvailable
                        +" enabled: "  + mIccFdnEnabled);
            } else {
                loge("Bogus facility lock response");
            }
            if (mIccFdnEnabled && mIccFdnAvailable) {
                mIccRecords.loadFdnRecords();
            }
        }
    }

    private void onChangeFdnDone(AsyncResult ar) {
        synchronized (mLock) {
            int attemptsRemaining = -1;

            if (ar.exception == null) {
                mIccFdnEnabled = mDesiredFdnEnabled;
                if (DBG) log("EVENT_CHANGE_FACILITY_FDN_DONE: " +
                        "mIccFdnEnabled=" + mIccFdnEnabled);
            } else {
                attemptsRemaining = parsePinPukErrorResult(ar);
                loge("Error change facility fdn with exception " + ar.exception);
            }
            Message response = (Message)ar.userObj;
            response.arg1 = attemptsRemaining;
            AsyncResult.forMessage(response).exception = ar.exception;
            response.sendToTarget();
        }
    }

    /** REMOVE when mIccLockEnabled is not needed, assumes mLock is held */
    private void queryPin1State() {
        int serviceClassX = CommandsInterface.SERVICE_CLASS_VOICE +
                CommandsInterface.SERVICE_CLASS_DATA +
                CommandsInterface.SERVICE_CLASS_FAX;
        mCi.queryFacilityLockForApp (
            CommandsInterface.CB_FACILITY_BA_SIM, "", serviceClassX,
            mAid, mHandler.obtainMessage(EVENT_QUERY_FACILITY_LOCK_DONE));
    }

    /** REMOVE when mIccLockEnabled is not needed*/
    private void onQueryFacilityLock(AsyncResult ar) {
        synchronized (mLock) {
            if(ar.exception != null) {
                if (DBG) log("Error in querying facility lock:" + ar.exception);
                return;
            }

            int[] ints = (int[])ar.result;
            if(ints.length != 0) {
                if (DBG) log("Query facility lock : "  + ints[0]);

                mIccLockEnabled = (ints[0] != 0);

                // Correctness check: we expect mPin1State to match mIccLockEnabled.
                // When mPin1State is DISABLED mIccLockEanbled should be false.
                // When mPin1State is ENABLED mIccLockEnabled should be true.
                //
                // Here we validate these assumptions to assist in identifying which ril/radio's
                // have not correctly implemented GET_SIM_STATUS
                switch (mPin1State) {
                    case PINSTATE_DISABLED:
                        if (mIccLockEnabled) {
                            loge("QUERY_FACILITY_LOCK:enabled GET_SIM_STATUS.Pin1:disabled."
                                    + " Fixme");
                        }
                        break;
                    case PINSTATE_ENABLED_NOT_VERIFIED:
                    case PINSTATE_ENABLED_VERIFIED:
                    case PINSTATE_ENABLED_BLOCKED:
                    case PINSTATE_ENABLED_PERM_BLOCKED:
                        if (!mIccLockEnabled) {
                            loge("QUERY_FACILITY_LOCK:disabled GET_SIM_STATUS.Pin1:enabled."
                                    + " Fixme");
                        }
                    case PINSTATE_UNKNOWN:
                    default:
                        if (DBG) log("Ignoring: pin1state=" + mPin1State);
                        break;
                }
            } else {
                loge("Bogus facility lock response");
            }
        }
    }

    /** REMOVE when mIccLockEnabled is not needed */
    private void onChangeFacilityLock(AsyncResult ar) {
        synchronized (mLock) {
            int attemptsRemaining = -1;

            if (ar.exception == null) {
                mIccLockEnabled = mDesiredPinLocked;
                if (DBG) log( "EVENT_CHANGE_FACILITY_LOCK_DONE: mIccLockEnabled= "
                        + mIccLockEnabled);
            } else {
                attemptsRemaining = parsePinPukErrorResult(ar);
                loge("Error change facility lock with exception " + ar.exception);
            }
            Message response = (Message)ar.userObj;
            AsyncResult.forMessage(response).exception = ar.exception;
            response.arg1 = attemptsRemaining;
            response.sendToTarget();
        }
    }

    /**
     * Parse the error response to obtain number of attempts remaining
     */
    private int parsePinPukErrorResult(AsyncResult ar) {
        int[] result = (int[]) ar.result;
        if (result == null) {
            return -1;
        } else {
            int length = result.length;
            int attemptsRemaining = -1;
            if (length > 0) {
                attemptsRemaining = result[0];
            }
            log("parsePinPukErrorResult: attemptsRemaining=" + attemptsRemaining);
            return attemptsRemaining;
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg){
            AsyncResult ar;

            if (mDestroyed) {
                loge("Received message " + msg + "[" + msg.what
                        + "] while being destroyed. Ignoring.");
                //When UiccCardApp dispose,unlock SIM PIN message and need return exception.
                if (msg.what == EVENT_PIN1_PUK1_DONE) {
                    ar = (AsyncResult) msg.obj;
                    if (ar != null) {
                        ar.exception = new CommandException(CommandException.Error.ABORTED);
                        Message response = (Message) ar.userObj;
                        if (response != null) {
                            AsyncResult.forMessage(response).exception = ar.exception;
                            response.sendToTarget();
                        }
                    }
                }
                return;
            }

            switch (msg.what) {
                case EVENT_PIN1_PUK1_DONE:
                case EVENT_PIN2_PUK2_DONE:
                case EVENT_CHANGE_PIN1_DONE:
                case EVENT_CHANGE_PIN2_DONE:
                    // a PIN/PUK/PIN2/PUK2 complete
                    // request has completed. ar.userObj is the response Message
                    ar = (AsyncResult)msg.obj;
                    int attemptsRemaining = parsePinPukErrorResult(ar);
                    Message response = (Message)ar.userObj;
                    AsyncResult.forMessage(response).exception = ar.exception;
                    response.arg1 = attemptsRemaining;
                    response.sendToTarget();
                    break;
                case EVENT_QUERY_FACILITY_FDN_DONE:
                    ar = (AsyncResult)msg.obj;
                    onQueryFdnEnabled(ar);
                    break;
                case EVENT_CHANGE_FACILITY_FDN_DONE:
                    ar = (AsyncResult)msg.obj;
                    onChangeFdnDone(ar);
                    break;
                case EVENT_QUERY_FACILITY_LOCK_DONE:
                    ar = (AsyncResult)msg.obj;
                    onQueryFacilityLock(ar);
                    break;
                case EVENT_CHANGE_FACILITY_LOCK_DONE:
                    ar = (AsyncResult)msg.obj;
                    onChangeFacilityLock(ar);
                    break;
                case EVENT_RADIO_UNAVAILABLE:
                    if (DBG) log("handleMessage (EVENT_RADIO_UNAVAILABLE)");
                    mAppState = AppState.APPSTATE_UNKNOWN;
                    break;
                default:
                    loge("Unknown Event " + msg.what);
            }
        }
    };

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void registerForReady(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant (h, what, obj);
            mReadyRegistrants.add(r);
            notifyReadyRegistrantsIfNeeded(r);
        }
    }

    @UnsupportedAppUsage
    public void unregisterForReady(Handler h) {
        synchronized (mLock) {
            mReadyRegistrants.remove(h);
        }
    }

    public void registerForDetected(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant(h, what, obj);
            mDetectedRegistrants.add(r);
            notifyDetectedRegistrantsIfNeeded(r);
        }
    }

    public void unregisterForDetected(Handler h) {
        synchronized (mLock) {
            mDetectedRegistrants.remove(h);
        }
    }

    /**
     * Notifies handler of any transition into State.isPinLocked()
     */
    protected void registerForLocked(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant (h, what, obj);
            mPinLockedRegistrants.add(r);
            notifyPinLockedRegistrantsIfNeeded(r);
        }
    }

    protected void unregisterForLocked(Handler h) {
        synchronized (mLock) {
            mPinLockedRegistrants.remove(h);
        }
    }

    /**
     * Notifies handler of any transition into State.NETWORK_LOCKED
     */
    protected void registerForNetworkLocked(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant (h, what, obj);
            mNetworkLockedRegistrants.add(r);
            notifyNetworkLockedRegistrantsIfNeeded(r);
        }
    }

    protected void unregisterForNetworkLocked(Handler h) {
        synchronized (mLock) {
            mNetworkLockedRegistrants.remove(h);
        }
    }

    /**
     * Notifies specified registrant, assume mLock is held.
     *
     * @param r Registrant to be notified. If null - all registrants will be notified
     */
    private void notifyReadyRegistrantsIfNeeded(Registrant r) {
        if (mDestroyed) {
            return;
        }
        if (mAppState == AppState.APPSTATE_READY) {
            if (mPin1State == PinState.PINSTATE_ENABLED_NOT_VERIFIED ||
                    mPin1State == PinState.PINSTATE_ENABLED_BLOCKED ||
                    mPin1State == PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
                loge("Sanity check failed! APPSTATE is ready while PIN1 is not verified!!!");
                // Don't notify if application is in an invalid state
                return;
            }
            if (r == null) {
                if (DBG) log("Notifying registrants: READY");
                mReadyRegistrants.notifyRegistrants();
            } else {
                if (DBG) log("Notifying 1 registrant: READY");
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    /**
     * Notifies specified registrant, assume mLock is held.
     *
     * @param r Registrant to be notified. If null - all registrants will be notified
     */
    private void notifyDetectedRegistrantsIfNeeded(Registrant r) {
        if (mDestroyed) {
            return;
        }
        if (mAppState == AppState.APPSTATE_DETECTED) {
            if (r == null) {
                if (DBG) log("Notifying registrants: DETECTED");
                mDetectedRegistrants.notifyRegistrants();
            } else {
                if (DBG) log("Notifying 1 registrant: DETECTED");
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    /**
     * Notifies specified registrant, assume mLock is held.
     *
     * @param r Registrant to be notified. If null - all registrants will be notified
     */
    private void notifyPinLockedRegistrantsIfNeeded(Registrant r) {
        if (mDestroyed) {
            return;
        }

        if (mAppState == AppState.APPSTATE_PIN ||
                mAppState == AppState.APPSTATE_PUK) {
            if (mPin1State == PinState.PINSTATE_ENABLED_VERIFIED ||
                    mPin1State == PinState.PINSTATE_DISABLED) {
                loge("Sanity check failed! APPSTATE is locked while PIN1 is not!!!");
                //Don't notify if application is in an invalid state
                return;
            }
            if (r == null) {
                if (DBG) log("Notifying registrants: LOCKED");
                mPinLockedRegistrants.notifyRegistrants();
            } else {
                if (DBG) log("Notifying 1 registrant: LOCKED");
                r.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }
    }

    /**
     * Notifies specified registrant, assume mLock is held.
     *
     * @param r Registrant to be notified. If null - all registrants will be notified
     */
    private void notifyNetworkLockedRegistrantsIfNeeded(Registrant r) {
        if (mDestroyed) {
            return;
        }

        if (mAppState == AppState.APPSTATE_SUBSCRIPTION_PERSO &&
                PersoSubState.isPersoLocked(mPersoSubState)) {
            AsyncResult ar = new AsyncResult(null, mPersoSubState.ordinal(), null);
            if (r == null) {
                if (DBG) log("Notifying registrants: NETWORK_LOCKED with mPersoSubState" + mPersoSubState);
                mNetworkLockedRegistrants.notifyRegistrants(ar);
            } else {
                if (DBG) log("Notifying 1 registrant: NETWORK_LOCKED with mPersoSubState" + mPersoSubState);
                r.notifyRegistrant(ar);
            }
        }
    }

    @UnsupportedAppUsage
    public AppState getState() {
        synchronized (mLock) {
            return mAppState;
        }
    }

    @UnsupportedAppUsage
    public AppType getType() {
        synchronized (mLock) {
            return mAppType;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int getAuthContext() {
        synchronized (mLock) {
            return mAuthContext;
        }
    }

    /**
     * Returns the authContext based on the type of UiccCard.
     *
     * @param appType the app type
     * @return authContext corresponding to the type or AUTH_CONTEXT_UNDEFINED if appType not
     * supported
     */
    private static int getAuthContext(AppType appType) {
        int authContext;

        switch (appType) {
            case APPTYPE_SIM:
                authContext = AUTH_CONTEXT_EAP_SIM;
                break;

            case APPTYPE_USIM:
                authContext = AUTH_CONTEXT_EAP_AKA;
                break;

            default:
                authContext = AUTH_CONTEXT_UNDEFINED;
                break;
        }

        return authContext;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public PersoSubState getPersoSubState() {
        synchronized (mLock) {
            return mPersoSubState;
        }
    }

    @UnsupportedAppUsage
    public String getAid() {
        synchronized (mLock) {
            return mAid;
        }
    }

    public String getAppLabel() {
        return mAppLabel;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public PinState getPin1State() {
        synchronized (mLock) {
            if (mPin1Replaced) {
                return mUiccProfile.getUniversalPinState();
            }
            return mPin1State;
        }
    }

    @UnsupportedAppUsage
    public IccFileHandler getIccFileHandler() {
        synchronized (mLock) {
            return mIccFh;
        }
    }

    @UnsupportedAppUsage
    public IccRecords getIccRecords() {
        synchronized (mLock) {
            return mIccRecords;
        }
    }

    /**
     * Supply the ICC PIN to the ICC
     *
     * When the operation is complete, onComplete will be sent to its
     * Handler.
     *
     * onComplete.obj will be an AsyncResult
     * onComplete.arg1 = remaining attempts before puk locked or -1 if unknown
     *
     * ((AsyncResult)onComplete.obj).exception == null on success
     * ((AsyncResult)onComplete.obj).exception != null on fail
     *
     * If the supplied PIN is incorrect:
     * ((AsyncResult)onComplete.obj).exception != null
     * && ((AsyncResult)onComplete.obj).exception
     *       instanceof com.android.internal.telephony.gsm.CommandException)
     * && ((CommandException)(((AsyncResult)onComplete.obj).exception))
     *          .getCommandError() == CommandException.Error.PASSWORD_INCORRECT
     */
    public void supplyPin (String pin, Message onComplete) {
        synchronized (mLock) {
            mCi.supplyIccPinForApp(pin, mAid, mHandler.obtainMessage(EVENT_PIN1_PUK1_DONE,
                    onComplete));
        }
    }

    /**
     * Supply the ICC PUK to the ICC
     *
     * When the operation is complete, onComplete will be sent to its
     * Handler.
     *
     * onComplete.obj will be an AsyncResult
     * onComplete.arg1 = remaining attempts before Icc will be permanently unusable
     * or -1 if unknown
     *
     * ((AsyncResult)onComplete.obj).exception == null on success
     * ((AsyncResult)onComplete.obj).exception != null on fail
     *
     * If the supplied PIN is incorrect:
     * ((AsyncResult)onComplete.obj).exception != null
     * && ((AsyncResult)onComplete.obj).exception
     *       instanceof com.android.internal.telephony.gsm.CommandException)
     * && ((CommandException)(((AsyncResult)onComplete.obj).exception))
     *          .getCommandError() == CommandException.Error.PASSWORD_INCORRECT
     *
     *
     */
    public void supplyPuk (String puk, String newPin, Message onComplete) {
        synchronized (mLock) {
        mCi.supplyIccPukForApp(puk, newPin, mAid,
                mHandler.obtainMessage(EVENT_PIN1_PUK1_DONE, onComplete));
        }
    }

    public void supplyPin2 (String pin2, Message onComplete) {
        synchronized (mLock) {
            mCi.supplyIccPin2ForApp(pin2, mAid,
                    mHandler.obtainMessage(EVENT_PIN2_PUK2_DONE, onComplete));
        }
    }

    public void supplyPuk2 (String puk2, String newPin2, Message onComplete) {
        synchronized (mLock) {
            mCi.supplyIccPuk2ForApp(puk2, newPin2, mAid,
                    mHandler.obtainMessage(EVENT_PIN2_PUK2_DONE, onComplete));
        }
    }

    public void supplyNetworkDepersonalization (String pin, Message onComplete) {
        synchronized (mLock) {
            if (DBG) log("supplyNetworkDepersonalization");
            mCi.supplyNetworkDepersonalization(pin, onComplete);
        }
    }

    public void supplySimDepersonalization(PersoSubState persoType,
                                           String pin, Message onComplete) {
        synchronized (mLock) {
            if (DBG) log("supplySimDepersonalization");
            mCi.supplySimDepersonalization(persoType, pin, onComplete);
        }
    }

    /**
     * Check whether ICC pin lock is enabled
     * This is a sync call which returns the cached pin enabled state
     *
     * @return true for ICC locked enabled
     *         false for ICC locked disabled
     */
    public boolean getIccLockEnabled() {
        return mIccLockEnabled;
        /* STOPSHIP: Remove line above and all code associated with setting
           mIccLockEanbled once all RIL correctly sends the pin1 state.
        // Use getPin1State to take into account pin1Replaced flag
        PinState pinState = getPin1State();
        return pinState == PinState.PINSTATE_ENABLED_NOT_VERIFIED ||
               pinState == PinState.PINSTATE_ENABLED_VERIFIED ||
               pinState == PinState.PINSTATE_ENABLED_BLOCKED ||
               pinState == PinState.PINSTATE_ENABLED_PERM_BLOCKED;*/
     }

    /**
     * Check whether ICC fdn (fixed dialing number) is enabled
     * This is a sync call which returns the cached pin enabled state
     *
     * @return true for ICC fdn enabled
     *         false for ICC fdn disabled
     */
    public boolean getIccFdnEnabled() {
        synchronized (mLock) {
            return mIccFdnEnabled;
        }
    }

    /**
     * Check whether fdn (fixed dialing number) service is available.
     * @return true if ICC fdn service available
     *         false if ICC fdn service not available
     */
    public boolean getIccFdnAvailable() {
        return mIccFdnAvailable;
    }

    /**
     * Set the ICC pin lock enabled or disabled
     * When the operation is complete, onComplete will be sent to its handler
     *
     * @param enabled "true" for locked "false" for unlocked.
     * @param password needed to change the ICC pin state, aka. Pin1
     * @param onComplete
     *        onComplete.obj will be an AsyncResult
     *        ((AsyncResult)onComplete.obj).exception == null on success
     *        ((AsyncResult)onComplete.obj).exception != null on fail
     */
    public void setIccLockEnabled (boolean enabled,
            String password, Message onComplete) {
        synchronized (mLock) {
            int serviceClassX;
            serviceClassX = CommandsInterface.SERVICE_CLASS_VOICE +
                    CommandsInterface.SERVICE_CLASS_DATA +
                    CommandsInterface.SERVICE_CLASS_FAX;

            mDesiredPinLocked = enabled;

            mCi.setFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_SIM,
                    enabled, password, serviceClassX, mAid,
                    mHandler.obtainMessage(EVENT_CHANGE_FACILITY_LOCK_DONE, onComplete));
        }
    }

    /**
     * Set the ICC fdn enabled or disabled
     * When the operation is complete, onComplete will be sent to its handler
     *
     * @param enabled "true" for locked "false" for unlocked.
     * @param password needed to change the ICC fdn enable, aka Pin2
     * @param onComplete
     *        onComplete.obj will be an AsyncResult
     *        ((AsyncResult)onComplete.obj).exception == null on success
     *        ((AsyncResult)onComplete.obj).exception != null on fail
     */
    public void setIccFdnEnabled (boolean enabled,
            String password, Message onComplete) {
        synchronized (mLock) {
            int serviceClassX;
            serviceClassX = CommandsInterface.SERVICE_CLASS_VOICE +
                    CommandsInterface.SERVICE_CLASS_DATA +
                    CommandsInterface.SERVICE_CLASS_FAX +
                    CommandsInterface.SERVICE_CLASS_SMS;

            mDesiredFdnEnabled = enabled;

            mCi.setFacilityLockForApp(CommandsInterface.CB_FACILITY_BA_FD,
                    enabled, password, serviceClassX, mAid,
                    mHandler.obtainMessage(EVENT_CHANGE_FACILITY_FDN_DONE, onComplete));
        }
    }

    /**
     * Change the ICC password used in ICC pin lock
     * When the operation is complete, onComplete will be sent to its handler
     *
     * @param oldPassword is the old password
     * @param newPassword is the new password
     * @param onComplete
     *        onComplete.obj will be an AsyncResult
     *        onComplete.arg1 = attempts remaining or -1 if unknown
     *        ((AsyncResult)onComplete.obj).exception == null on success
     *        ((AsyncResult)onComplete.obj).exception != null on fail
     */
    public void changeIccLockPassword(String oldPassword, String newPassword,
            Message onComplete) {
        synchronized (mLock) {
            if (DBG) log("changeIccLockPassword");
            mCi.changeIccPinForApp(oldPassword, newPassword, mAid,
                    mHandler.obtainMessage(EVENT_CHANGE_PIN1_DONE, onComplete));
        }
    }

    /**
     * Change the ICC password used in ICC fdn enable
     * When the operation is complete, onComplete will be sent to its handler
     *
     * @param oldPassword is the old password
     * @param newPassword is the new password
     * @param onComplete
     *        onComplete.obj will be an AsyncResult
     *        ((AsyncResult)onComplete.obj).exception == null on success
     *        ((AsyncResult)onComplete.obj).exception != null on fail
     */
    public void changeIccFdnPassword(String oldPassword, String newPassword,
            Message onComplete) {
        synchronized (mLock) {
            if (DBG) log("changeIccFdnPassword");
            mCi.changeIccPin2ForApp(oldPassword, newPassword, mAid,
                    mHandler.obtainMessage(EVENT_CHANGE_PIN2_DONE, onComplete));
        }
    }

    /**
     * @return true if the UiccCardApplication is ready.
     */
    public boolean isReady() {
        synchronized (mLock) {
            if (mAppState != AppState.APPSTATE_READY) {
                return false;
            } else if (mPin1State == PinState.PINSTATE_ENABLED_NOT_VERIFIED
                    || mPin1State == PinState.PINSTATE_ENABLED_BLOCKED
                    || mPin1State == PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
                loge("Sanity check failed! APPSTATE is ready while PIN1 is not verified!!!");
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * @return true if ICC card is PIN2 blocked
     */
    public boolean getIccPin2Blocked() {
        synchronized (mLock) {
            return mPin2State == PinState.PINSTATE_ENABLED_BLOCKED;
        }
    }

    /**
     * @return true if ICC card is PUK2 blocked
     */
    public boolean getIccPuk2Blocked() {
        synchronized (mLock) {
            return mPin2State == PinState.PINSTATE_ENABLED_PERM_BLOCKED;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public int getPhoneId() {
        return mUiccProfile.getPhoneId();
    }

    public boolean isAppIgnored() {
        return mIgnoreApp;
    }

    public void setAppIgnoreState(boolean ignore) {
        mIgnoreApp = ignore;
    }

    protected UiccProfile getUiccProfile() {
        return mUiccProfile;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println("UiccCardApplication: ");
        pw.increaseIndent();
        pw.println("mUiccProfile=" + mUiccProfile);
        pw.println("mAppState=" + mAppState);
        pw.println("mAppType=" + mAppType);
        pw.println("mPersoSubState=" + mPersoSubState);
        pw.println("mAid=" + mAid);
        pw.println("mAppLabel=" + mAppLabel);
        pw.println("mPin1Replaced=" + mPin1Replaced);
        pw.println("mPin1State=" + mPin1State);
        pw.println("mPin2State=" + mPin2State);
        pw.println("mIccFdnEnabled=" + mIccFdnEnabled);
        pw.println("mDesiredFdnEnabled=" + mDesiredFdnEnabled);
        pw.println("mIccLockEnabled=" + mIccLockEnabled);
        pw.println("mDesiredPinLocked=" + mDesiredPinLocked);
        pw.println("mIccRecords=" + mIccRecords);
        pw.println("mIccFh=" + mIccFh);
        pw.println("mDestroyed=" + mDestroyed);
        pw.decreaseIndent();
        pw.flush();
    }
}
