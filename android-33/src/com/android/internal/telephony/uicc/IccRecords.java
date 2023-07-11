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

package com.android.internal.telephony.uicc;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemClock;
import android.telephony.CellIdentity;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.util.ArrayUtils;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@hide}
 */
public abstract class IccRecords extends Handler implements IccConstants {
    protected static final boolean DBG = true;
    protected static final boolean VDBG = false; // STOPSHIP if true

    public static final int PLMN_MIN_LENGTH = CellIdentity.MCC_LENGTH
            + CellIdentity.MNC_MIN_LENGTH;
    public static final int PLMN_MAX_LENGTH = CellIdentity.MCC_LENGTH
            + CellIdentity.MNC_MAX_LENGTH;

    // Lookup table for carriers known to produce SIMs which incorrectly indicate MNC length.
    private static final String[] MCCMNC_CODES_HAVING_3DIGITS_MNC = {
        "302370", "302720", "310260",
        "405025", "405026", "405027", "405028", "405029", "405030", "405031", "405032",
        "405033", "405034", "405035", "405036", "405037", "405038", "405039", "405040",
        "405041", "405042", "405043", "405044", "405045", "405046", "405047", "405750",
        "405751", "405752", "405753", "405754", "405755", "405756", "405799", "405800",
        "405801", "405802", "405803", "405804", "405805", "405806", "405807", "405808",
        "405809", "405810", "405811", "405812", "405813", "405814", "405815", "405816",
        "405817", "405818", "405819", "405820", "405821", "405822", "405823", "405824",
        "405825", "405826", "405827", "405828", "405829", "405830", "405831", "405832",
        "405833", "405834", "405835", "405836", "405837", "405838", "405839", "405840",
        "405841", "405842", "405843", "405844", "405845", "405846", "405847", "405848",
        "405849", "405850", "405851", "405852", "405853", "405854", "405855", "405856",
        "405857", "405858", "405859", "405860", "405861", "405862", "405863", "405864",
        "405865", "405866", "405867", "405868", "405869", "405870", "405871", "405872",
        "405873", "405874", "405875", "405876", "405877", "405878", "405879", "405880",
        "405881", "405882", "405883", "405884", "405885", "405886", "405908", "405909",
        "405910", "405911", "405912", "405913", "405914", "405915", "405916", "405917",
        "405918", "405919", "405920", "405921", "405922", "405923", "405924", "405925",
        "405926", "405927", "405928", "405929", "405930", "405931", "405932", "502142",
        "502143", "502145", "502146", "502147", "502148"
    };

    // ***** Instance Variables
    protected AtomicBoolean mDestroyed = new AtomicBoolean(false);
    protected AtomicBoolean mLoaded = new AtomicBoolean(false);
    protected Context mContext;
    protected CommandsInterface mCi;
    protected IccFileHandler mFh;
    protected UiccCardApplication mParentApp;
    protected TelephonyManager mTelephonyManager;

    protected RegistrantList mRecordsLoadedRegistrants = new RegistrantList();
    protected RegistrantList mLockedRecordsLoadedRegistrants = new RegistrantList();
    protected RegistrantList mNetworkLockedRecordsLoadedRegistrants = new RegistrantList();
    protected RegistrantList mImsiReadyRegistrants = new RegistrantList();
    protected RegistrantList mRecordsEventsRegistrants = new RegistrantList();
    protected RegistrantList mNewSmsRegistrants = new RegistrantList();
    protected RegistrantList mNetworkSelectionModeAutomaticRegistrants = new RegistrantList();
    protected RegistrantList mSpnUpdatedRegistrants = new RegistrantList();
    protected RegistrantList mRecordsOverrideRegistrants = new RegistrantList();

    protected int mRecordsToLoad;  // number of pending load requests

    protected AdnRecordCache mAdnCache;

    // ***** Cached SIM State; cleared on channel close

    // SIM is not locked
    protected static final int LOCKED_RECORDS_REQ_REASON_NONE = 0;
    // Records requested for PIN or PUK locked SIM
    protected static final int LOCKED_RECORDS_REQ_REASON_LOCKED = 1;
    // Records requested for network locked SIM
    protected static final int LOCKED_RECORDS_REQ_REASON_NETWORK_LOCKED = 2;

    protected boolean mRecordsRequested = false; // true if we've made requests for the sim records
    protected int mLockedRecordsReqReason = LOCKED_RECORDS_REQ_REASON_NONE;

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PROTECTED)
    public String mIccId;  // Includes only decimals (no hex)

    protected String mFullIccId;  // Includes hex characters in ICCID
    protected String mMsisdn = null;  // My mobile number
    protected String mMsisdnTag = null;
    protected String mNewMsisdn = null;
    protected String mNewMsisdnTag = null;
    protected String mVoiceMailNum = null;
    protected String mVoiceMailTag = null;
    protected String mNewVoiceMailNum = null;
    protected String mNewVoiceMailTag = null;
    protected boolean mIsVoiceMailFixed = false;
    protected String mImsi; // IMSI must be only valid numeric characters 0-9 without padding 'f's

    protected int mMncLength = UNINITIALIZED;
    protected int mMailboxIndex = 0; // 0 is no mailbox dailing number associated

    protected int mSmsCountOnIcc = 0;

    private String mSpn;

    protected String mGid1;
    protected String mGid2;

    protected String mPnnHomeName;

    protected String mPrefLang;

    protected PlmnActRecord[] mHplmnActRecords;
    protected PlmnActRecord[] mOplmnActRecords;
    protected PlmnActRecord[] mPlmnActRecords;

    // A list of PLMN in which the SPN shall be displayed.
    // Reference: 3GPP TS 31.102 Section 4.2.66
    protected String[] mSpdi;

    // A list of PLMN Network Name (PNN).
    // Reference: 3GPP TS 31.102 Section 4.2.58
    protected PlmnNetworkName[] mPnns;

    // Operator PLMN List (OPL).
    // Reference: 3GPP TS 31.102 Section 4.2.59
    protected OperatorPlmnInfo[] mOpl;


    // Carrier name display condition bitmask
    // Reference: 3GPP TS 131.102 section 4.2.12 EF_SPN Display Condition
    protected int mCarrierNameDisplayCondition;

    protected String[] mEhplmns;
    protected String[] mFplmns;

    CarrierTestOverride mCarrierTestOverride;

    //Arbitrary offset for the Handler
    protected static final int HANDLER_ACTION_BASE = 0x12E500;
    protected static final int HANDLER_ACTION_NONE = HANDLER_ACTION_BASE + 0;
    protected static final int HANDLER_ACTION_SEND_RESPONSE = HANDLER_ACTION_BASE + 1;
    protected static AtomicInteger sNextRequestId = new AtomicInteger(1);
    protected final HashMap<Integer, Pair<Message, Object>> mPendingTransactions = new HashMap<>();
    // ***** Constants

    // Markers for mncLength
    protected static final int UNINITIALIZED = -1;
    protected static final int UNKNOWN = 0;

    // Bitmask for carrier name display condition.
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"CARRIER_NAME_DISPLAY_CONDITION_BITMASK_"},
            value = {CARRIER_NAME_DISPLAY_CONDITION_BITMASK_PLMN,
                    CARRIER_NAME_DISPLAY_CONDITION_BITMASK_SPN},
            flag = true)
    public @interface CarrierNameDisplayConditionBitmask {}
    public static final int CARRIER_NAME_DISPLAY_CONDITION_BITMASK_PLMN = 1;
    public static final int CARRIER_NAME_DISPLAY_CONDITION_BITMASK_SPN = 2;


    // See {@link CarrierConfigManager#KEY_SPN_DISPLAY_CONDITION_OVERRIDE_INT}.
    public static final int INVALID_CARRIER_NAME_DISPLAY_CONDITION_BITMASK = -1;

    // Display SPN only and only if registered to Home PLMNs.
    // Display PLMN only and only if registered to Non-Home PLMNs.
    public static final int DEFAULT_CARRIER_NAME_DISPLAY_CONDITION = 0;

    // ***** Event Constants
    public static final int EVENT_MWI = 0; // Message Waiting indication
    public static final int EVENT_CFI = 1; // Call Forwarding indication
    public static final int EVENT_SPN = 2; // Service Provider Name

    public static final int EVENT_GET_ICC_RECORD_DONE = 100;
    public static final int EVENT_REFRESH = 31; // ICC refresh occurred
    private static final int EVENT_AKA_AUTHENTICATE_DONE = 90;
    protected static final int EVENT_GET_SMS_RECORD_SIZE_DONE = 28;

    protected static final int SYSTEM_EVENT_BASE = 0x100;
    protected static final int EVENT_APP_READY = 1 + SYSTEM_EVENT_BASE;
    protected static final int EVENT_APP_LOCKED = 2 + SYSTEM_EVENT_BASE;
    protected static final int EVENT_APP_NETWORK_LOCKED = 3 + SYSTEM_EVENT_BASE;
    protected static final int EVENT_APP_DETECTED = 4 + SYSTEM_EVENT_BASE;

    public static final int CALL_FORWARDING_STATUS_DISABLED = 0;
    public static final int CALL_FORWARDING_STATUS_ENABLED = 1;
    public static final int CALL_FORWARDING_STATUS_UNKNOWN = -1;

    public static final int DEFAULT_VOICE_MESSAGE_COUNT = -2;
    public static final int UNKNOWN_VOICE_MESSAGE_COUNT = -1;

    // Maximum time in millisecond to wait for a IccSim Challenge before assuming it will not
    // arrive and returning null to the callers.
    private static final long ICC_SIM_CHALLENGE_TIMEOUT_MILLIS = 2500;

    /**
     * There are two purposes for this class. First, each instance of AuthAsyncResponse acts as a
     * lock to for calling thead to wait in getIccSimChallengeResponse(). Second, pass the IMS
     * authentication response to the getIccSimChallengeResponse().
     */
    private static class AuthAsyncResponse {
        public IccIoResult authRsp;
        public Throwable exception;
    }

    @Override
    public String toString() {
        String iccIdToPrint = SubscriptionInfo.givePrintableIccid(mFullIccId);
        return "mDestroyed=" + mDestroyed
                + " mContext=" + mContext
                + " mCi=" + mCi
                + " mFh=" + mFh
                + " mParentApp=" + mParentApp
                + " recordsToLoad=" + mRecordsToLoad
                + " adnCache=" + mAdnCache
                + " recordsRequested=" + mRecordsRequested
                + " lockedRecordsReqReason=" + mLockedRecordsReqReason
                + " iccid=" + iccIdToPrint
                + (mCarrierTestOverride.isInTestMode() ? "mFakeIccid="
                + mCarrierTestOverride.getFakeIccid() : "")
                + " msisdnTag=" + mMsisdnTag
                + " voiceMailNum=" + Rlog.pii(VDBG, mVoiceMailNum)
                + " voiceMailTag=" + mVoiceMailTag
                + " voiceMailNum=" + Rlog.pii(VDBG, mNewVoiceMailNum)
                + " newVoiceMailTag=" + mNewVoiceMailTag
                + " isVoiceMailFixed=" + mIsVoiceMailFixed
                + " mImsi=" + ((mImsi != null) ?
                mImsi.substring(0, 6) + Rlog.pii(VDBG, mImsi.substring(6)) : "null")
                + (mCarrierTestOverride.isInTestMode() ? " mFakeImsi="
                + mCarrierTestOverride.getFakeIMSI() : "")
                + " mncLength=" + mMncLength
                + " mailboxIndex=" + mMailboxIndex
                + " spn=" + mSpn
                + (mCarrierTestOverride.isInTestMode() ? " mFakeSpn="
                + mCarrierTestOverride.getFakeSpn() : "");
    }

    /**
     * Generic ICC record loaded callback. Subclasses can call EF load methods on
     * {@link IccFileHandler} passing a Message for onLoaded with the what field set to
     * {@link #EVENT_GET_ICC_RECORD_DONE} and the obj field set to an instance
     * of this interface. The {@link #handleMessage} method in this class will print a
     * log message using {@link #getEfName()} and decrement {@link #mRecordsToLoad}.
     *
     * If the record load was successful, {@link #onRecordLoaded} will be called with the result.
     * Otherwise, an error log message will be output by {@link #handleMessage} and
     * {@link #onRecordLoaded} will not be called.
     */
    public interface IccRecordLoaded {
        String getEfName();
        void onRecordLoaded(AsyncResult ar);
    }

    // ***** Constructor
    public IccRecords(UiccCardApplication app, Context c, CommandsInterface ci) {
        mContext = c;
        mCi = ci;
        mFh = app.getIccFileHandler();
        mParentApp = app;
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);

        mCarrierTestOverride = new CarrierTestOverride(mParentApp.getPhoneId());
        mCi.registerForIccRefresh(this, EVENT_REFRESH, null);

        mParentApp.registerForReady(this, EVENT_APP_READY, null);
        mParentApp.registerForDetected(this, EVENT_APP_DETECTED, null);
        mParentApp.registerForLocked(this, EVENT_APP_LOCKED, null);
        mParentApp.registerForNetworkLocked(this, EVENT_APP_NETWORK_LOCKED, null);
    }

    // Override IccRecords for testing
    public void setCarrierTestOverride(String mccmnc, String imsi, String iccid, String gid1,
            String gid2, String pnn, String spn)  {
        mCarrierTestOverride.override(mccmnc, imsi, iccid, gid1, gid2, pnn, spn);
        mTelephonyManager.setSimOperatorNameForPhone(mParentApp.getPhoneId(), spn);
        mTelephonyManager.setSimOperatorNumericForPhone(mParentApp.getPhoneId(), mccmnc);
        mRecordsOverrideRegistrants.notifyRegistrants();
    }

    /**
     * Call when the IccRecords object is no longer going to be used.
     */
    public void dispose() {
        mDestroyed.set(true);

        mCi.unregisterForIccRefresh(this);
        mParentApp.unregisterForReady(this);
        mParentApp.unregisterForDetected(this);
        mParentApp.unregisterForLocked(this);
        mParentApp.unregisterForNetworkLocked(this);

        mParentApp = null;
        mFh = null;
        mCi = null;
        mContext = null;
        if (mAdnCache != null) {
            mAdnCache.reset();
        }
        mLoaded.set(false);
    }

    protected abstract void onReady();

    protected void onDetected() {
        mRecordsRequested = false;
        mLoaded.set(false);
    }

    protected void onLocked() {
        // The LOADED state should not be indicated while the lock is effective.
        mRecordsRequested = false;
        mLoaded.set(false);
    }

    //***** Public Methods
    public AdnRecordCache getAdnCache() {
        return mAdnCache;
    }

    /**
     * Adds a message to the pending requests list by generating a unique (integer)
     * hash key and returning it. The message should never be null.
     *
     * @param msg Message of the transaction to be stored
     * @return the unique (integer) hash key to retrieve the pending transaction
     */
    public int storePendingTransaction(Message msg) {
        return storePendingTransaction(msg, null);
    }

    /**
     * Adds a message and obj pair to the pending requests list by generating a unique (integer)
     * hash key and returning it. The message should never be null.
     *
     * @param msg Message of the transaction to be stored
     * @param obj Object of the transaction to be stored
     * @return the unique (integer) hash key to retrieve the pending transaction
     */
    public int storePendingTransaction(Message msg, Object obj) {
        int key = sNextRequestId.getAndIncrement();
        Pair<Message, Object> pair = new Pair<Message, Object>(msg, obj);
        synchronized (mPendingTransactions) {
            mPendingTransactions.put(key, pair);
        }
        return key;
    }

    /**
     * Returns the pending transaction and free it from memory, if any or null
     *
     * @param key key of the entry to retrieve
     * @return The pending transaction.
     */
    public Pair<Message, Object> retrievePendingTransaction(Integer key) {
        synchronized (mPendingTransactions) {
            return mPendingTransactions.remove(key);
        }
    }

    /**
     * Returns the ICC ID stripped at the first hex character. Some SIMs have ICC IDs
     * containing hex digits; {@link #getFullIccId()} should be used to get the full ID including
     * hex digits.
     * @return ICC ID without hex digits
     */
    public String getIccId() {
        if (mCarrierTestOverride.isInTestMode()) {
            String fakeIccId = mCarrierTestOverride.getFakeIccid();
            if (fakeIccId != null) {
                return fakeIccId;
            }
        }
        return mIccId;
    }

    /**
     * Returns the full ICC ID including hex digits.
     * @return full ICC ID including hex digits
     */
    public String getFullIccId() {
        return mFullIccId;
    }

    public void registerForRecordsLoaded(Handler h, int what, Object obj) {
        if (mDestroyed.get()) {
            return;
        }

        Registrant r = new Registrant(h, what, obj);
        mRecordsLoadedRegistrants.add(r);

        if (getRecordsLoaded()) {
            r.notifyRegistrant(new AsyncResult(null, null, null));
        }
    }

    public void unregisterForRecordsLoaded(Handler h) {
        mRecordsLoadedRegistrants.remove(h);
    }

    public void unregisterForRecordsOverride(Handler h) {
        mRecordsOverrideRegistrants.remove(h);
    }

    public void registerForRecordsOverride(Handler h, int what, Object obj) {
        if (mDestroyed.get()) {
            return;
        }

        Registrant r = new Registrant(h, what, obj);
        mRecordsOverrideRegistrants.add(r);

        if (getRecordsLoaded()) {
            r.notifyRegistrant(new AsyncResult(null, null, null));
        }
    }

    /**
     * Register to be notified when records are loaded for a PIN or PUK locked SIM
     */
    public void registerForLockedRecordsLoaded(Handler h, int what, Object obj) {
        if (mDestroyed.get()) {
            return;
        }

        Registrant r = new Registrant(h, what, obj);
        mLockedRecordsLoadedRegistrants.add(r);

        if (getLockedRecordsLoaded()) {
            r.notifyRegistrant(new AsyncResult(null, null, null));
        }
    }

    /**
     * Unregister corresponding to registerForLockedRecordsLoaded()
     */
    public void unregisterForLockedRecordsLoaded(Handler h) {
        mLockedRecordsLoadedRegistrants.remove(h);
    }

    /**
     * Register to be notified when records are loaded for a network locked SIM
     */
    public void registerForNetworkLockedRecordsLoaded(Handler h, int what, Object obj) {
        if (mDestroyed.get()) {
            return;
        }

        Registrant r = new Registrant(h, what, obj);
        mNetworkLockedRecordsLoadedRegistrants.add(r);

        if (getNetworkLockedRecordsLoaded()) {
            r.notifyRegistrant(new AsyncResult(null, null, null));
        }
    }

    /**
     * Unregister corresponding to registerForLockedRecordsLoaded()
     */
    public void unregisterForNetworkLockedRecordsLoaded(Handler h) {
        mNetworkLockedRecordsLoadedRegistrants.remove(h);
    }

    public void registerForImsiReady(Handler h, int what, Object obj) {
        if (mDestroyed.get()) {
            return;
        }

        Registrant r = new Registrant(h, what, obj);
        mImsiReadyRegistrants.add(r);

        if (getIMSI() != null) {
            r.notifyRegistrant(new AsyncResult(null, null, null));
        }
    }
    public void unregisterForImsiReady(Handler h) {
        mImsiReadyRegistrants.remove(h);
    }

    public void registerForSpnUpdate(Handler h, int what, Object obj) {
        if (mDestroyed.get()) {
            return;
        }

        Registrant r = new Registrant(h, what, obj);
        mSpnUpdatedRegistrants.add(r);

        if (!TextUtils.isEmpty(mSpn)) {
            r.notifyRegistrant(new AsyncResult(null, null, null));
        }
    }
    public void unregisterForSpnUpdate(Handler h) {
        mSpnUpdatedRegistrants.remove(h);
    }

    public void registerForRecordsEvents(Handler h, int what, Object obj) {
        Registrant r = new Registrant (h, what, obj);
        mRecordsEventsRegistrants.add(r);

        /* Notify registrant of all the possible events. This is to make sure registrant is
        notified even if event occurred in the past. */
        r.notifyResult(EVENT_MWI);
        r.notifyResult(EVENT_CFI);
    }

    public void unregisterForRecordsEvents(Handler h) {
        mRecordsEventsRegistrants.remove(h);
    }

    public void registerForNewSms(Handler h, int what, Object obj) {
        Registrant r = new Registrant (h, what, obj);
        mNewSmsRegistrants.add(r);
    }

    public void unregisterForNewSms(Handler h) {
        mNewSmsRegistrants.remove(h);
    }

    public void registerForNetworkSelectionModeAutomatic(
            Handler h, int what, Object obj) {
        Registrant r = new Registrant (h, what, obj);
        mNetworkSelectionModeAutomaticRegistrants.add(r);
    }
    public void unregisterForNetworkSelectionModeAutomatic(Handler h) {
        mNetworkSelectionModeAutomaticRegistrants.remove(h);
    }

    /**
     * Get the International Mobile Subscriber ID (IMSI) on a SIM
     * for GSM, UMTS and like networks. Default is null if IMSI is
     * not supported or unavailable.
     *
     * @return null if SIM is not yet ready or unavailable
     */
    public String getIMSI() {
        if (mCarrierTestOverride.isInTestMode()) {
            String fakeImsi = mCarrierTestOverride.getFakeIMSI();
            if (fakeImsi != null) {
                return fakeImsi;
            }
        }
        return mImsi;
    }

    /**
     * Update IMSI record and try to extract the PLMN information and notify registrants.
     * @param inImsi the IMSI value
     */
    public void setImsi(String inImsi) {
        // Remove trailing F's if present in IMSI.
        mImsi = IccUtils.stripTrailingFs(inImsi);
        if (!Objects.equals(mImsi, inImsi)) {
            loge("Invalid IMSI padding digits received.");
        }

        if (TextUtils.isEmpty(mImsi)) mImsi = null;

        if (mImsi != null && !mImsi.matches("[0-9]+")) {
            loge("Invalid non-numeric IMSI digits received.");
            mImsi = null;
        }

        // IMSI (MCC+MNC+MSIN) is at least 6 digits, but not more
        // than 15 (and usually 15).
        // This will also handle un-set IMSI records (all Fs)
        if (mImsi != null && (mImsi.length() < 6 || mImsi.length() > 15)) {
            loge("invalid IMSI " + mImsi);
            mImsi = null;
        }

        log("IMSI: mMncLength=" + mMncLength);

        if (mImsi != null && mImsi.length() >= 6) {
            log("IMSI: " + mImsi.substring(0, 6) + Rlog.pii(VDBG, mImsi.substring(6)));
        }

        // IMSI has changed so the PLMN might have changed as well
        updateOperatorPlmn();

        mImsiReadyRegistrants.notifyRegistrants();
    }

    protected void updateOperatorPlmn() {
        // In case of a test override, use the test IMSI
        String imsi = getIMSI();

        if (imsi != null) {
            // First try to guess the length based on a table of known 3-digit MNCs.
            if (((mMncLength == UNKNOWN) || (mMncLength == 2)) && imsi.length() >= 6) {
                String mccmncCode = imsi.substring(0, 6);
                for (String mccmnc : MCCMNC_CODES_HAVING_3DIGITS_MNC) {
                    if (mccmnc.equals(mccmncCode)) {
                        mMncLength = 3;
                        log("IMSI: setting1 mMncLength=" + mMncLength);
                        break;
                    }
                }
            }

            // If still unknown, guess using the MCC.
            if (mMncLength == UNKNOWN) {
                try {
                    int mcc = Integer.parseInt(imsi.substring(0, 3));
                    mMncLength = MccTable.smallestDigitsMccForMnc(mcc);
                    log("setting2 mMncLength=" + mMncLength);
                } catch (NumberFormatException e) {
                    loge("Corrupt IMSI! setting3 mMncLength=" + mMncLength);
                }
            }

            if (mMncLength != UNKNOWN && mMncLength != UNINITIALIZED
                    && imsi.length() >= 3 + mMncLength) {
                log("update mccmnc=" + imsi.substring(0, 3 + mMncLength));
                // finally have both the imsi and the mncLength and
                // can parse the imsi properly
                MccTable.updateMccMncConfiguration(mContext, imsi.substring(0, 3 + mMncLength));
            }
        }
    }

    /**
     * Get the Network Access ID (NAI) on a CSIM for CDMA like networks. Default is null if IMSI is
     * not supported or unavailable.
     *
     * @return null if NAI is not yet ready or unavailable
     */
    public String getNAI() {
        return null;
    }

    public String getMsisdnNumber() {
        return mMsisdn;
    }

    /**
     * Get the Group Identifier Level 1 (GID1) on a SIM for GSM.
     * @return null if SIM is not yet ready
     */
    public String getGid1() {
        if (mCarrierTestOverride.isInTestMode()) {
            String fakeGid1 = mCarrierTestOverride.getFakeGid1();
            if (fakeGid1 != null) {
                return fakeGid1;
            }
        }
        return mGid1;
    }

    /**
     * Get the Group Identifier Level 2 (GID2) on a SIM.
     * @return null if SIM is not yet ready
     */
    public String getGid2() {
        if (mCarrierTestOverride.isInTestMode()) {
            String fakeGid2 = mCarrierTestOverride.getFakeGid2();
            if (fakeGid2 != null) {
                return fakeGid2;
            }
        }
        return mGid2;
    }

    /**
     * Get the PLMN network name on a SIM.
     * @return null if SIM is not yet ready
     */
    public String getPnnHomeName() {
        if (mCarrierTestOverride.isInTestMode()) {
            String fakePnnHomeName = mCarrierTestOverride.getFakePnnHomeName();
            if (fakePnnHomeName != null) {
                return fakePnnHomeName;
            }
        }
        return mPnnHomeName;
    }

    public PlmnNetworkName[] getPnns() {
        return mPnns;
    }

    public OperatorPlmnInfo[] getOpl() {
        return mOpl;
    }

    public void setMsisdnNumber(String alphaTag, String number,
            Message onComplete) {
        loge("setMsisdn() should not be invoked on base IccRecords");
        // synthesize a "File Not Found" exception and return it
        AsyncResult.forMessage(onComplete).exception =
            (new IccIoResult(0x6A, 0x82, (byte[]) null)).getException();
        onComplete.sendToTarget();
    }

    public String getMsisdnAlphaTag() {
        return mMsisdnTag;
    }

    public String getVoiceMailNumber() {
        return mVoiceMailNum;
    }

    /**
     * Return Service Provider Name stored in SIM (EF_SPN=0x6F46) or in RUIM (EF_RUIM_SPN=0x6F41).
     *
     * @return null if SIM is not yet ready or no RUIM entry
     */
    public String getServiceProviderName() {
        if (mCarrierTestOverride.isInTestMode()) {
            String fakeSpn = mCarrierTestOverride.getFakeSpn();
            if (fakeSpn != null) {
                return fakeSpn;
            }
        }
        return mSpn;
    }

    /**
     * Return Service Provider Name stored in SIM (EF_SPN=0x6F46) or in RUIM (EF_RUIM_SPN=0x6F41) or
     * the brand override. The brand override has higher priority than the SPN from SIM.
     *
     * @return service provider name.
     */
    @Nullable
    public String getServiceProviderNameWithBrandOverride() {
        if (mParentApp != null && mParentApp.getUiccProfile() != null) {
            String brandOverride = mParentApp.getUiccProfile().getOperatorBrandOverride();
            if (!TextUtils.isEmpty(brandOverride)) {
                return brandOverride;
            }
        }
        return mSpn;
    }

    protected void setServiceProviderName(String spn) {
        if (!TextUtils.equals(mSpn, spn)) {
            mSpn = spn != null ? spn.trim() : null;
            mSpnUpdatedRegistrants.notifyRegistrants();
        }
    }

    /**
     * Set voice mail number to SIM record
     *
     * The voice mail number can be stored either in EF_MBDN (TS 51.011) or
     * EF_MAILBOX_CPHS (CPHS 4.2)
     *
     * If EF_MBDN is available, store the voice mail number to EF_MBDN
     *
     * If EF_MAILBOX_CPHS is enabled, store the voice mail number to EF_CHPS
     *
     * So the voice mail number will be stored in both EFs if both are available
     *
     * Return error only if both EF_MBDN and EF_MAILBOX_CPHS fail.
     *
     * When the operation is complete, onComplete will be sent to its handler
     *
     * @param alphaTag alpha-tagging of the dailing nubmer (upto 10 characters)
     * @param voiceNumber dailing nubmer (upto 20 digits)
     *        if the number is start with '+', then set to international TOA
     * @param onComplete
     *        onComplete.obj will be an AsyncResult
     *        ((AsyncResult)onComplete.obj).exception == null on success
     *        ((AsyncResult)onComplete.obj).exception != null on fail
     */
    public abstract void setVoiceMailNumber(String alphaTag, String voiceNumber,
            Message onComplete);

    public String getVoiceMailAlphaTag() {
        return mVoiceMailTag;
    }

    /**
     * Sets the SIM voice message waiting indicator records
     * @param line GSM Subscriber Profile Number, one-based. Only '1' is supported
     * @param countWaiting The number of messages waiting, if known. Use
     *                     -1 to indicate that an unknown number of
     *                      messages are waiting
     */
    public abstract void setVoiceMessageWaiting(int line, int countWaiting);

    /**
     * Called by GsmCdmaPhone to update VoiceMail count
     */
    public abstract int getVoiceMessageCount();

    /**
     * Called by STK Service when REFRESH is received.
     * @param fileChanged indicates whether any files changed
     * @param fileList if non-null, a list of EF files that changed
     */
    public abstract void onRefresh(boolean fileChanged, int[] fileList);

    public boolean getRecordsLoaded() {
        return mRecordsToLoad == 0 && mRecordsRequested;
    }

    protected boolean getLockedRecordsLoaded() {
        return mRecordsToLoad == 0
                && mLockedRecordsReqReason == LOCKED_RECORDS_REQ_REASON_LOCKED;
    }

    protected boolean getNetworkLockedRecordsLoaded() {
        return mRecordsToLoad == 0
                && mLockedRecordsReqReason == LOCKED_RECORDS_REQ_REASON_NETWORK_LOCKED;
    }

    //***** Overridden from Handler
    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar;

        switch (msg.what) {
            case EVENT_APP_READY:
                mLockedRecordsReqReason = LOCKED_RECORDS_REQ_REASON_NONE;
                onReady();
                break;

            case EVENT_APP_DETECTED:
                mLockedRecordsReqReason = LOCKED_RECORDS_REQ_REASON_NONE;
                onDetected();
                break;

            case EVENT_APP_LOCKED:
                mLockedRecordsReqReason = LOCKED_RECORDS_REQ_REASON_LOCKED;
                onLocked();
                break;

            case EVENT_APP_NETWORK_LOCKED:
                mLockedRecordsReqReason = LOCKED_RECORDS_REQ_REASON_NETWORK_LOCKED;
                onLocked();
                break;

            case EVENT_GET_ICC_RECORD_DONE:
                try {
                    ar = (AsyncResult) msg.obj;
                    IccRecordLoaded recordLoaded = (IccRecordLoaded) ar.userObj;
                    if (DBG) log(recordLoaded.getEfName() + " LOADED");

                    if (ar.exception != null) {
                        loge("Record Load Exception: " + ar.exception);
                    } else {
                        recordLoaded.onRecordLoaded(ar);
                    }
                }catch (RuntimeException exc) {
                    // I don't want these exceptions to be fatal
                    loge("Exception parsing SIM record: " + exc);
                } finally {
                    // Count up record load responses even if they are fails
                    onRecordLoaded();
                }
                break;

            case EVENT_REFRESH:
                ar = (AsyncResult)msg.obj;
                if (DBG) log("Card REFRESH occurred: ");
                if (ar.exception == null) {
                    handleRefresh((IccRefreshResponse)ar.result);
                } else {
                    loge("Icc refresh Exception: " + ar.exception);
                }
                break;

            case EVENT_AKA_AUTHENTICATE_DONE:
                ar = (AsyncResult) msg.obj;
                AuthAsyncResponse rsp = (AuthAsyncResponse) ar.userObj;
                if (DBG) log("EVENT_AKA_AUTHENTICATE_DONE");

                synchronized (rsp) {
                    if (ar.exception != null) {
                        rsp.exception = ar.exception;
                        loge("Exception ICC SIM AKA: " + ar.exception);
                    } else if (ar.result == null) {
                        rsp.exception = new NullPointerException(
                                "Null SIM authentication response");
                        loge("EVENT_AKA_AUTHENTICATE_DONE: null response");
                    } else {
                        try {
                            rsp.authRsp = (IccIoResult) ar.result;
                            if (VDBG) log("ICC SIM AKA: authRsp = " + rsp.authRsp);
                        } catch (ClassCastException e) {
                            rsp.exception = e;
                            loge("Failed to parse ICC SIM AKA contents: " + e);
                        }
                    }
                    rsp.notifyAll();
                }

                break;

            case EVENT_GET_SMS_RECORD_SIZE_DONE:
                ar = (AsyncResult) msg.obj;

                if (ar.exception != null) {
                    onRecordLoaded();
                    loge("Exception in EVENT_GET_SMS_RECORD_SIZE_DONE " + ar.exception);
                    break;
                }

                int[] recordSize = (int[])ar.result;
                try {
                    // recordSize[0]  is the record length
                    // recordSize[1]  is the total length of the EF file
                    // recordSize[2]  is the number of records in the EF file
                    mSmsCountOnIcc = recordSize[2];
                    log("EVENT_GET_SMS_RECORD_SIZE_DONE Size " + recordSize[0]
                            + " total " + recordSize[1]
                                    + " record " + recordSize[2]);
                } catch (ArrayIndexOutOfBoundsException exc) {
                    mSmsCountOnIcc = -1;
                    loge("ArrayIndexOutOfBoundsException in EVENT_GET_SMS_RECORD_SIZE_DONE: "
                            + exc.toString());
                } finally {
                    onRecordLoaded();
                }
                break;

            default:
                super.handleMessage(msg);
        }
    }

    /**
     * Returns the SIM language derived from the EF-LI and EF-PL sim records.
     */
    public String getSimLanguage() {
        return mPrefLang;
    }

    protected void setSimLanguage(byte[] efLi, byte[] efPl) {
        String[] locales = mContext.getAssets().getLocales();
        try {
            mPrefLang = findBestLanguage(efLi, locales);
        } catch (UnsupportedEncodingException uee) {
            log("Unable to parse EF-LI: " + Arrays.toString(efLi));
        }

        if (mPrefLang == null) {
            try {
                mPrefLang = findBestLanguage(efPl, locales);
            } catch (UnsupportedEncodingException uee) {
                log("Unable to parse EF-PL: " + Arrays.toString(efLi));
            }
        }
    }

    protected static String findBestLanguage(byte[] languages, String[] locales)
            throws UnsupportedEncodingException {
        if ((languages == null) || (locales == null)) return null;

        // Each 2-bytes consists of one language
        for (int i = 0; (i + 1) < languages.length; i += 2) {
            String lang = new String(languages, i, 2, "ISO-8859-1");
            for (int j = 0; j < locales.length; j++) {
                if (locales[j] != null && locales[j].length() >= 2 &&
                        locales[j].substring(0, 2).equalsIgnoreCase(lang)) {
                    return lang;
                }
            }
        }

        // no match found. return null
        return null;
    }

    protected abstract void handleFileUpdate(int efid);

    protected void handleRefresh(IccRefreshResponse refreshResponse){
        if (refreshResponse == null) {
            if (DBG) log("handleRefresh received without input");
            return;
        }

        if (!TextUtils.isEmpty(refreshResponse.aid) &&
                !refreshResponse.aid.equals(mParentApp.getAid())) {
            // This is for different app. Ignore.
            return;
        }

        switch (refreshResponse.refreshResult) {
            case IccRefreshResponse.REFRESH_RESULT_FILE_UPDATE:
                if (DBG) log("handleRefresh with SIM_FILE_UPDATED");
                handleFileUpdate(refreshResponse.efId);
                break;
            default:
                // unknown refresh operation
                if (DBG) log("handleRefresh with unknown operation");
                break;
        }
    }

    protected abstract void onRecordLoaded();

    protected abstract void onAllRecordsLoaded();

    /**
     * Retrieves the SPN/PLMN display condition from UICC.
     *
     * Display of service provider name is required when registered PLMN is neither HPLMN nor a PLMN
     * in the service provider PLMN list(EF_SPDI).
     *
     * Display of PLMN network name is required when registered PLMN is either HPLMN or a PLMN in
     * the service provider PLMN list(EF_SPDI).
     *
     * Reference: 3GPP TS 131.102 section 4.2.12 EF_SPN Display Condition
     *
     * @return a bitmask represent the carrier name display condition.
     */
    @CarrierNameDisplayConditionBitmask
    public int getCarrierNameDisplayCondition() {
        return mCarrierNameDisplayCondition;
    }

    /**
     * Retrieves the service provider display information. This is a list of PLMNs in which the
     * service provider name shall be displayed.
     *
     * Reference: 3GPP TS 131.102 section 4.2.66 EF_SPDI
     *
     * @return a list of PLMN(mcc+mnc) if EF_SPDI is existed, otherwise return null.
     */
    public String[] getServiceProviderDisplayInformation() {
        return mSpdi;
    }

    /**
     * Get home PLMN list.
     *
     * @see #getEhplmns()
     * @see #getServiceProviderDisplayInformation()
     *
     * @return a list of HPLMN if existed, otherwise return null.
     */
    public String[] getHomePlmns() {
        // hplmn from imsi.
        String hplmn = getOperatorNumeric();

        // hplmn from ehplmn list.
        String[] hplmns = getEhplmns();

        // plmn from ef_spdi.
        String[] spdi = getServiceProviderDisplayInformation();

        // Use the plmn from imsi as the hplmn if Ehplmn not present.
        if (ArrayUtils.isEmpty(hplmns)) {
            hplmns = new String[] {hplmn};
        }

        if (!ArrayUtils.isEmpty(spdi)) {
            hplmns = ArrayUtils.concatElements(String.class, hplmns, spdi);
        }
        // If hplmns don't contain hplmn, we need to add hplmn to hplmns
        hplmns = ArrayUtils.appendElement(String.class, hplmns, hplmn);
        return hplmns;
    }

    /**
     * Return true if "Restriction of menu options for manual PLMN selection"
     * bit is set or EF_CSP data is unavailable, return false otherwise.
     * Generally used for GSM/UMTS and the like SIMs.
     */
    public boolean isCspPlmnEnabled() {
        return false;
    }

    /**
     * Returns the 5 or 6 digit MCC/MNC of the operator that
     * provided the SIM card. Returns null of SIM is not yet ready
     * or is not valid for the type of IccCard. Generally used for
     * GSM/UMTS and the like SIMS
     */
    public String getOperatorNumeric() {
        return null;
    }

    /**
     * Get the current Voice call forwarding flag for GSM/UMTS and the like SIMs
     *
     * @return CALL_FORWARDING_STATUS_XXX (DISABLED/ENABLED/UNKNOWN)
     */
    public int getVoiceCallForwardingFlag() {
        return CALL_FORWARDING_STATUS_UNKNOWN;
    }

    /**
     * Set the voice call forwarding flag for GSM/UMTS and the like SIMs
     *
     * @param line to enable/disable
     * @param enable
     * @param number to which CFU is enabled
     */
    public void setVoiceCallForwardingFlag(int line, boolean enable, String number) {
    }

    /**
     * Indicates wether the ICC records have been loaded or not
     *
     * @return true if the records have been loaded, false otherwise.
     */
    public boolean isLoaded() {
        return mLoaded.get();
    }

    /**
     * Indicates wether SIM is in provisioned state or not.
     * Overridden only if SIM can be dynamically provisioned via OTA.
     *
     * @return true if provisioned
     */
    public boolean isProvisioned () {
        return true;
    }

    /**
     * Write string to log file
     *
     * @param s is the string to write
     */
    protected abstract void log(String s);

    /**
     * Write error string to log file.
     *
     * @param s is the string to write
     */
    protected abstract void loge(String s);

    /**
     * @return String array containing EHPLMNs associated with the card.
     */
    public String[] getEhplmns() {
        return mEhplmns;
    }

    /**
     * @return String array containing PLMN from HplmnActRecord.
     */
    public String[] getPlmnsFromHplmnActRecord() {
        if (mHplmnActRecords == null) return null;
        String[] hplmns = new String[mHplmnActRecords.length];
        for (int i = 0; i < mHplmnActRecords.length; i++) {
            hplmns[i] = mHplmnActRecords[i].plmn;
        }
        return hplmns;
    }

    /**
     * Return an interface to retrieve the ISIM records for IMS, if available.
     * @return the interface to retrieve the ISIM records, or null if not supported
     */
    public IsimRecords getIsimRecords() {
        return null;
    }

    public UsimServiceTable getUsimServiceTable() {
        return null;
    }

    /**
     * Solve authentication leakage issue. See b/147463955.
     * Returns the response of the SIM application on the UICC to authentication
     * challenge/response algorithm. The data string and challenge response are
     * Base64 encoded Strings.
     * Can support EAP-SIM, EAP-AKA with results encoded per 3GPP TS 31.102.
     *
     * @param authContext parameter P2 that specifies the authentication context
     * per 3GPP TS 31.102 (Section 7.1.2)
     * @param data authentication challenge data
     * @return challenge response
     */
    @Nullable
    public String getIccSimChallengeResponse(int authContext, String data) {
        if (VDBG) log("getIccSimChallengeResponse:");

        //final here is for defensive copy.
        final CommandsInterface ci = mCi;
        final UiccCardApplication parentApp = mParentApp;
        if (ci == null || parentApp == null) {
            loge("getIccSimChallengeResponse: Fail, ci or parentApp is null");
            return null;
        }

        AuthAsyncResponse rsp = new AuthAsyncResponse();

        synchronized (rsp) {
            ci.requestIccSimAuthentication(authContext, data, parentApp.getAid(),
                    obtainMessage(EVENT_AKA_AUTHENTICATE_DONE, 0, 0, rsp));
            //TODO: factor wait with timeout into a separate method
            final long startTime = SystemClock.elapsedRealtime();
            do {
                try {
                    long sleepTime = startTime + ICC_SIM_CHALLENGE_TIMEOUT_MILLIS
                            - SystemClock.elapsedRealtime();
                    if (sleepTime > 0) rsp.wait(sleepTime);
                } catch (InterruptedException e) {
                    Rlog.w("IccRecords", "getIccSimChallengeResponse: InterruptedException.");
                }
            } while (SystemClock.elapsedRealtime() - startTime < ICC_SIM_CHALLENGE_TIMEOUT_MILLIS
                    && rsp.authRsp == null && rsp.exception == null);

            if (SystemClock.elapsedRealtime() - startTime >= ICC_SIM_CHALLENGE_TIMEOUT_MILLIS
                    && rsp.authRsp == null && rsp.exception == null) {
                loge("getIccSimChallengeResponse timeout!");
                return null;
            }

            if (rsp.exception != null) {
                loge("getIccSimChallengeResponse exception: " + rsp.exception);
                //TODO: propagate better exceptions up to the user now that we have them available
                //in the call stack.
                return null;
            }

            if (rsp.authRsp == null) {
                loge("getIccSimChallengeResponse: No authentication response");
                return null;
            }
        }
        if (VDBG) log("getIccSimChallengeResponse: return rsp.authRsp");

        return rsp.authRsp.payload != null ? new String(rsp.authRsp.payload) : null;
    }

    /**
     * Convert the spn display condition to a bitmask
     * {@link com.android.internal.telephony.uicc.IccRecords.CarrierNameDisplayConditionBitmask}.
     *
     * b1 is the last bit of the display condition which is used to determine whether display of
     * PLMN network name is required when registered PLMN is **either** HPLMN or a PLMN in the
     * service provider PLMN list.
     *
     * b2 is the second last bit of the display condtion which is used to determine
     * whether display of Service Provider Name is required when registered PLMN is
     * **neither** HPLMN nor PLMN in the service provider PLMN list.
     *
     * Reference: 3GPP TS 31.102 section 4.2.12 EF_SPN
     *
     * @return a carrier name display condtion bitmask.
     */
    @CarrierNameDisplayConditionBitmask
    public static int convertSpnDisplayConditionToBitmask(int condition) {
        int carrierNameDisplayCondition = 0;
        // b1 = 0: display of registered PLMN name not required when registered PLMN is
        // either HPLMN or a PLMN in the service provider PLMN list.
        // b1 = 1: display of registered PLMN name required when registered PLMN is
        // either HPLMN or a PLMN in the service provider PLMN list.
        if ((condition & 0x1) == 0x1) {
            carrierNameDisplayCondition |= CARRIER_NAME_DISPLAY_CONDITION_BITMASK_PLMN;
        }

        // b2 = 0: display of the service provider name is **required** when registered
        // PLMN is neither HPLMN nor a PLMN in the service provider PLMN list.
        // b2 = 1: display of the servier provider name is **not required** when
        // registered PLMN is neither HPLMN nor PLMN in the service provider PLMN list.
        if ((condition & 0x2) == 0) {
            carrierNameDisplayCondition |= CARRIER_NAME_DISPLAY_CONDITION_BITMASK_SPN;
        }

        return carrierNameDisplayCondition;
    }

    /**
     * Get SMS capacity count on ICC card.
     */
    public int getSmsCapacityOnIcc() {
        if (DBG) log("getSmsCapacityOnIcc: " + mSmsCountOnIcc);
        return mSmsCountOnIcc;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("IccRecords: " + this);
        pw.println(" mDestroyed=" + mDestroyed);
        pw.println(" mCi=" + mCi);
        pw.println(" mFh=" + mFh);
        pw.println(" mParentApp=" + mParentApp);
        pw.println(" recordsLoadedRegistrants: size=" + mRecordsLoadedRegistrants.size());
        for (int i = 0; i < mRecordsLoadedRegistrants.size(); i++) {
            pw.println("  recordsLoadedRegistrants[" + i + "]="
                    + ((Registrant)mRecordsLoadedRegistrants.get(i)).getHandler());
        }
        pw.println(" mLockedRecordsLoadedRegistrants: size="
                + mLockedRecordsLoadedRegistrants.size());
        for (int i = 0; i < mLockedRecordsLoadedRegistrants.size(); i++) {
            pw.println("  mLockedRecordsLoadedRegistrants[" + i + "]="
                    + ((Registrant) mLockedRecordsLoadedRegistrants.get(i)).getHandler());
        }
        pw.println(" mNetworkLockedRecordsLoadedRegistrants: size="
                + mNetworkLockedRecordsLoadedRegistrants.size());
        for (int i = 0; i < mNetworkLockedRecordsLoadedRegistrants.size(); i++) {
            pw.println("  mLockedRecordsLoadedRegistrants[" + i + "]="
                    + ((Registrant) mNetworkLockedRecordsLoadedRegistrants.get(i)).getHandler());
        }
        pw.println(" mImsiReadyRegistrants: size=" + mImsiReadyRegistrants.size());
        for (int i = 0; i < mImsiReadyRegistrants.size(); i++) {
            pw.println("  mImsiReadyRegistrants[" + i + "]="
                    + ((Registrant)mImsiReadyRegistrants.get(i)).getHandler());
        }
        pw.println(" mRecordsEventsRegistrants: size=" + mRecordsEventsRegistrants.size());
        for (int i = 0; i < mRecordsEventsRegistrants.size(); i++) {
            pw.println("  mRecordsEventsRegistrants[" + i + "]="
                    + ((Registrant)mRecordsEventsRegistrants.get(i)).getHandler());
        }
        pw.println(" mNewSmsRegistrants: size=" + mNewSmsRegistrants.size());
        for (int i = 0; i < mNewSmsRegistrants.size(); i++) {
            pw.println("  mNewSmsRegistrants[" + i + "]="
                    + ((Registrant)mNewSmsRegistrants.get(i)).getHandler());
        }
        pw.println(" mNetworkSelectionModeAutomaticRegistrants: size="
                + mNetworkSelectionModeAutomaticRegistrants.size());
        for (int i = 0; i < mNetworkSelectionModeAutomaticRegistrants.size(); i++) {
            pw.println("  mNetworkSelectionModeAutomaticRegistrants[" + i + "]="
                    + ((Registrant)mNetworkSelectionModeAutomaticRegistrants.get(i)).getHandler());
        }
        pw.println(" mRecordsRequested=" + mRecordsRequested);
        pw.println(" mLockedRecordsReqReason=" + mLockedRecordsReqReason);
        pw.println(" mRecordsToLoad=" + mRecordsToLoad);
        pw.println(" mRdnCache=" + mAdnCache);

        String iccIdToPrint = SubscriptionInfo.givePrintableIccid(mFullIccId);
        pw.println(" iccid=" + iccIdToPrint);
        pw.println(" mMsisdn=" + Rlog.pii(VDBG, mMsisdn));
        pw.println(" mMsisdnTag=" + mMsisdnTag);
        pw.println(" mVoiceMailNum=" + Rlog.pii(VDBG, mVoiceMailNum));
        pw.println(" mVoiceMailTag=" + mVoiceMailTag);
        pw.println(" mNewVoiceMailNum=" + Rlog.pii(VDBG, mNewVoiceMailNum));
        pw.println(" mNewVoiceMailTag=" + mNewVoiceMailTag);
        pw.println(" mIsVoiceMailFixed=" + mIsVoiceMailFixed);
        pw.println(" mImsi=" + ((mImsi != null) ?
                mImsi.substring(0, 6) + Rlog.pii(VDBG, mImsi.substring(6)) : "null"));
        if (mCarrierTestOverride.isInTestMode()) {
            pw.println(" mFakeImsi=" + mCarrierTestOverride.getFakeIMSI());
        }
        pw.println(" mMncLength=" + mMncLength);
        pw.println(" mMailboxIndex=" + mMailboxIndex);
        pw.println(" mSpn=" + mSpn);
        if (mCarrierTestOverride.isInTestMode()) {
            pw.println(" mFakeSpn=" + mCarrierTestOverride.getFakeSpn());
        }
        pw.flush();
    }

    /**
     * Get network name in PNN for the provided PLMN and LAC/TAC.
     *
     * @param opls OPL.
     * @param pnns PNN list.
     * @param plmn PLMN.
     * @param lacTac LAC/TAC
     * @return network Name for the provided PLMN and LAC/TAC.
     */
    @Nullable public static String getNetworkNameForPlmnFromPnnOpl(PlmnNetworkName[] pnns,
            OperatorPlmnInfo[] opls, @Nullable String plmn, int lacTac) {
        if (opls == null || pnns == null || plmn == null || plmn.length() < PLMN_MIN_LENGTH
                || plmn.length() > PLMN_MAX_LENGTH) {
            return null;
        }

        for (OperatorPlmnInfo operatorPlmnInfo: opls) {
            int pnnIdx = operatorPlmnInfo.getPnnIdx(plmn, lacTac);
            if (pnnIdx >= 0) {
                if (pnnIdx < pnns.length && pnns[pnnIdx] != null) {
                    return pnns[pnnIdx].getName();
                } else {
                    Rlog.e("IccRecords", "Invalid PNN record for Record" + pnnIdx);
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Operator PLMN information. This contains the location area information or tracking area
     * that are used to associate a specific name contained in EF_PNN.
     *
     * Reference: 3GPP TS 31.102 section 4.2.59 EF_OPL
     */
    public static final class OperatorPlmnInfo {
        // PLMN numeric that may contains wildcard character "D".
        // A BCD value of 'D' in any of the MCC and/or MNC digits shall be used to indicate
        // a "wild" value for that corresponding MCC/MNC digit.
        // For example, the pattern "123DDD" could match all PLMN which mcc is 123.
        public final String plmnNumericPattern;
        public final int lacTacStart;
        public final int lacTacEnd;
        // Identifier of operator name in PNN to be displayed.
        // 0 indicates that the name is to be taken from other sources, see 3GPP TS 22.101.
        // pnnRecordId > 0 indicates record # (pnnRecordId - 1) in PNNs.
        public final int pnnRecordId;

        public OperatorPlmnInfo(@NonNull String plmnNumericPattern, int lacTacStart, int lacTacEnd,
                                int pnnRecordId) {
            this.plmnNumericPattern = plmnNumericPattern;
            this.lacTacStart = lacTacStart;
            this.lacTacEnd = lacTacEnd;
            this.pnnRecordId = pnnRecordId;
        }

        /**
         * Check whether provided plmn and lacTac matches the stored OperatorPlmnInfo.
         *
         * @return -1 if not matching.
         */
        public int getPnnIdx(@Nullable String plmn, int lacTac) {
            if (plmn == null || plmn.length() != plmnNumericPattern.length()) return -1;

            // Check whether PLMN matches with the plmnNumericPattern
            // Character-by-character check is for performance reasons.
            for (int i = 0; i < plmn.length(); i++) {
                if (plmn.charAt(i) != plmnNumericPattern.charAt(i)
                        && plmnNumericPattern.charAt(i) != 'D') {
                    return -1;
                }
            }
            // As defiend in 3GPP TS 31.102 section 4.2.59 , lacTacStart = 0 and lacTacEnd = 0xFFFE
            // are used to indicate the entire range of LACs/TACs for the given PLMN.
            if (lacTacStart == 0 && lacTacEnd == 0xFFFE) {
                return pnnRecordId - 1;
            }
            if (lacTac < lacTacStart || lacTac > lacTacEnd) return -1;
            return pnnRecordId - 1;
        }

        @Override
        public int hashCode() {
            return Objects.hash(plmnNumericPattern, lacTacStart, lacTacEnd,
                    pnnRecordId);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof OperatorPlmnInfo)) return false;

            OperatorPlmnInfo opi = (OperatorPlmnInfo) other;
            return TextUtils.equals(plmnNumericPattern, opi.plmnNumericPattern)
                    && lacTacStart == opi.lacTacStart
                    && lacTacEnd == opi.lacTacEnd
                    && pnnRecordId == opi.pnnRecordId;
        }

        @Override
        public String toString() {
            return "{plmnNumericPattern = " + plmnNumericPattern + ", "
                    + "lacTacStart = " + lacTacStart + ", "
                    + "lacTacEnd = " + lacTacEnd + ", "
                    + "pnnRecordId = " + pnnRecordId
                    + "}";
        }
    }

    /**
     * Full and short version of PLMN network name.
     */
    public static final class PlmnNetworkName {
        public final String fullName;
        public final String shortName;

        public PlmnNetworkName(String fullName, String shortName) {
            this.fullName = fullName;
            this.shortName = shortName;
        }

        /**
         * Get the name stored in the PlmnNetworkName.
         * @return the full name if it's available; otherwise, short Name.
         */
        @Nullable public String getName() {
            if (!TextUtils.isEmpty(fullName)) {
                return fullName;
            } else {
                return shortName;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(fullName, shortName);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof PlmnNetworkName)) return false;

            PlmnNetworkName pnn = (PlmnNetworkName) other;
            return TextUtils.equals(fullName, pnn.fullName)
                    && TextUtils.equals(shortName, pnn.shortName);
        }

        @Override
        public String toString() {
            return "{fullName = " + fullName + ", shortName = " + shortName + "}";
        }
    }
}
