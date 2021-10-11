/*
 * Copyright (C) 2011-2012 The Android Open Source Project
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

import static android.telephony.TelephonyManager.UNINITIALIZED_CARD_ID;
import static android.telephony.TelephonyManager.UNSUPPORTED_CARD_ID;

import static java.util.Arrays.copyOf;

import android.app.BroadcastOptions;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.storage.StorageManager;
import android.preference.PreferenceManager;
import android.sysprop.TelephonyProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccCardInfo;
import android.text.TextUtils;
import android.util.LocalLog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConfigurationManager;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.RadioConfig;
import com.android.internal.telephony.SubscriptionInfoUpdater;
import com.android.internal.telephony.uicc.euicc.EuiccCard;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is responsible for keeping all knowledge about
 * Universal Integrated Circuit Card (UICC), also know as SIM's,
 * in the system. It is also used as API to get appropriate
 * applications to pass them to phone and service trackers.
 *
 * UiccController is created with the call to make() function.
 * UiccController is a singleton and make() must only be called once
 * and throws an exception if called multiple times.
 *
 * Once created UiccController registers with RIL for "on" and "unsol_sim_status_changed"
 * notifications. When such notification arrives UiccController will call
 * getIccCardStatus (GET_SIM_STATUS). Based on the response of GET_SIM_STATUS
 * request appropriate tree of uicc objects will be created.
 *
 * Following is class diagram for uicc classes:
 *
 *                       UiccController
 *                            #
 *                            |
 *                        UiccSlot[]
 *                            #
 *                            |
 *                        UiccCard
 *                            #
 *                            |
 *                       UiccProfile
 *                          #   #
 *                          |   ------------------
 *                    UiccCardApplication    CatService
 *                      #            #
 *                      |            |
 *                 IccRecords    IccFileHandler
 *                 ^ ^ ^           ^ ^ ^ ^ ^
 *    SIMRecords---- | |           | | | | ---SIMFileHandler
 *    RuimRecords----- |           | | | ----RuimFileHandler
 *    IsimUiccRecords---           | | -----UsimFileHandler
 *                                 | ------CsimFileHandler
 *                                 ----IsimFileHandler
 *
 * Legend: # stands for Composition
 *         ^ stands for Generalization
 *
 * See also {@link com.android.internal.telephony.IccCard}
 */
public class UiccController extends Handler {
    private static final boolean DBG = true;
    private static final boolean VDBG = false; //STOPSHIP if true
    private static final String LOG_TAG = "UiccController";

    public static final int INVALID_SLOT_ID = -1;

    public static final int APP_FAM_3GPP =  1;
    public static final int APP_FAM_3GPP2 = 2;
    public static final int APP_FAM_IMS   = 3;

    private static final int EVENT_ICC_STATUS_CHANGED = 1;
    private static final int EVENT_SLOT_STATUS_CHANGED = 2;
    private static final int EVENT_GET_ICC_STATUS_DONE = 3;
    private static final int EVENT_GET_SLOT_STATUS_DONE = 4;
    private static final int EVENT_RADIO_ON = 5;
    private static final int EVENT_RADIO_AVAILABLE = 6;
    private static final int EVENT_RADIO_UNAVAILABLE = 7;
    private static final int EVENT_SIM_REFRESH = 8;
    private static final int EVENT_EID_READY = 9;
    private static final int EVENT_MULTI_SIM_CONFIG_CHANGED = 10;
    // NOTE: any new EVENT_* values must be added to eventToString.

    // this needs to be here, because on bootup we dont know which index maps to which UiccSlot
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private CommandsInterface[] mCis;
    @VisibleForTesting
    public UiccSlot[] mUiccSlots;
    private int[] mPhoneIdToSlotId;
    private boolean mIsSlotStatusSupported = true;

    // This maps the externally exposed card ID (int) to the internal card ID string (ICCID/EID).
    // The array index is the card ID (int).
    // This mapping exists to expose card-based functionality without exposing the EID, which is
    // considered sensetive information.
    // mCardStrings is populated using values from the IccSlotStatus and IccCardStatus. For
    // HAL < 1.2, these do not contain the EID or the ICCID, so mCardStrings will be empty
    private ArrayList<String> mCardStrings;

    // This is the card ID of the default eUICC. It starts as UNINITIALIZED_CARD_ID.
    // When we load the EID (either with slot status or from the EuiccCard), we set it to the eUICC
    // with the lowest slot index.
    // If EID is not supported (e.g. on HAL version < 1.2), we set it to UNSUPPORTED_CARD_ID
    private int mDefaultEuiccCardId;

    // Default Euicc Card ID used when the device is temporarily unable to read the EID (e.g. on HAL
    // 1.2-1.3 if the eUICC is currently inactive). This value is only used within the
    // UiccController and should be converted to UNSUPPORTED_CARD_ID when others ask.
    // (This value is -3 because UNSUPPORTED_CARD_ID and UNINITIALIZED_CARD_ID are -1 and -2)
    private static final int TEMPORARILY_UNSUPPORTED_CARD_ID = -3;

    // GSM SGP.02 section 2.2.2 states that the EID is always 32 digits long
    private static final int EID_LENGTH = 32;

    // SharedPreference key for saving the known card strings (ICCIDs and EIDs) ordered by card ID
    private static final String CARD_STRINGS = "card_strings";

    // Whether the device has an eUICC built in.
    private boolean mHasBuiltInEuicc = false;

    // Whether the device has a currently active built in eUICC
    private boolean mHasActiveBuiltInEuicc = false;

    // The physical slots which correspond to built-in eUICCs
    private final int[] mEuiccSlots;

    // SharedPreferences key for saving the default euicc card ID
    private static final String DEFAULT_CARD = "default_card";

    @UnsupportedAppUsage
    private static final Object mLock = new Object();
    @UnsupportedAppUsage
    private static UiccController mInstance;
    @VisibleForTesting
    public static ArrayList<IccSlotStatus> sLastSlotStatus;

    @UnsupportedAppUsage
    @VisibleForTesting
    public Context mContext;

    protected RegistrantList mIccChangedRegistrants = new RegistrantList();

    private UiccStateChangedLauncher mLauncher;
    private RadioConfig mRadioConfig;

    /* The storage for the PIN codes. */
    private final PinStorage mPinStorage;

    // LocalLog buffer to hold important SIM related events for debugging
    private static LocalLog sLocalLog = new LocalLog(TelephonyUtils.IS_DEBUGGABLE ? 250 : 100);

    /**
     * API to make UiccController singleton if not already created.
     */
    public static UiccController make(Context c) {
        synchronized (mLock) {
            if (mInstance != null) {
                throw new RuntimeException("UiccController.make() should only be called once");
            }
            mInstance = new UiccController(c);
            return mInstance;
        }
    }

    private UiccController(Context c) {
        if (DBG) log("Creating UiccController");
        mContext = c;
        mCis = PhoneFactory.getCommandsInterfaces();
        int numPhysicalSlots = c.getResources().getInteger(
                com.android.internal.R.integer.config_num_physical_slots);
        numPhysicalSlots = TelephonyProperties.sim_slots_count().orElse(numPhysicalSlots);
        if (DBG) {
            logWithLocalLog("config_num_physical_slots = " + numPhysicalSlots);
        }
        // Minimum number of physical slot count should be equals to or greater than phone count,
        // if it is less than phone count use phone count as physical slot count.
        if (numPhysicalSlots < mCis.length) {
            numPhysicalSlots = mCis.length;
        }

        mUiccSlots = new UiccSlot[numPhysicalSlots];
        mPhoneIdToSlotId = new int[mCis.length];
        Arrays.fill(mPhoneIdToSlotId, INVALID_SLOT_ID);
        if (VDBG) logPhoneIdToSlotIdMapping();
        mRadioConfig = RadioConfig.getInstance();
        mRadioConfig.registerForSimSlotStatusChanged(this, EVENT_SLOT_STATUS_CHANGED, null);
        for (int i = 0; i < mCis.length; i++) {
            mCis[i].registerForIccStatusChanged(this, EVENT_ICC_STATUS_CHANGED, i);

            if (!StorageManager.inCryptKeeperBounce()) {
                mCis[i].registerForAvailable(this, EVENT_RADIO_AVAILABLE, i);
            } else {
                mCis[i].registerForOn(this, EVENT_RADIO_ON, i);
            }

            mCis[i].registerForNotAvailable(this, EVENT_RADIO_UNAVAILABLE, i);
            mCis[i].registerForIccRefresh(this, EVENT_SIM_REFRESH, i);
        }

        mLauncher = new UiccStateChangedLauncher(c, this);
        mCardStrings = loadCardStrings();
        mDefaultEuiccCardId = UNINITIALIZED_CARD_ID;

        mEuiccSlots = mContext.getResources()
                .getIntArray(com.android.internal.R.array.non_removable_euicc_slots);
        mHasBuiltInEuicc = hasBuiltInEuicc();

        PhoneConfigurationManager.registerForMultiSimConfigChange(
                this, EVENT_MULTI_SIM_CONFIG_CHANGED, null);

        mPinStorage = new PinStorage(mContext);
    }

    /**
     * Given the slot index, return the phone ID, or -1 if no phone is associated with the given
     * slot.
     * @param slotId the slot index to check
     * @return the associated phone ID or -1
     */
    public int getPhoneIdFromSlotId(int slotId) {
        for (int i = 0; i < mPhoneIdToSlotId.length; i++) {
            if (mPhoneIdToSlotId[i] == slotId) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Return the physical slot id associated with the given phoneId, or INVALID_SLOT_ID.
     * @param phoneId the phoneId to check
     */
    public int getSlotIdFromPhoneId(int phoneId) {
        try {
            return mPhoneIdToSlotId[phoneId];
        } catch (ArrayIndexOutOfBoundsException e) {
            return INVALID_SLOT_ID;
        }
    }

    @UnsupportedAppUsage
    public static UiccController getInstance() {
        synchronized (mLock) {
            if (mInstance == null) {
                throw new RuntimeException(
                        "UiccController.getInstance can't be called before make()");
            }
            return mInstance;
        }
    }

    @UnsupportedAppUsage
    public UiccCard getUiccCard(int phoneId) {
        synchronized (mLock) {
            return getUiccCardForPhone(phoneId);
        }
    }

    /**
     * API to get UiccCard corresponding to given physical slot index
     * @param slotId index of physical slot on the device
     * @return UiccCard object corresponting to given physical slot index; null if card is
     * absent
     */
    public UiccCard getUiccCardForSlot(int slotId) {
        synchronized (mLock) {
            UiccSlot uiccSlot = getUiccSlot(slotId);
            if (uiccSlot != null) {
                return uiccSlot.getUiccCard();
            }
            return null;
        }
    }

    /**
     * API to get UiccCard corresponding to given phone id
     * @return UiccCard object corresponding to given phone id; null if there is no card present for
     * the phone id
     */
    public UiccCard getUiccCardForPhone(int phoneId) {
        synchronized (mLock) {
            if (isValidPhoneIndex(phoneId)) {
                UiccSlot uiccSlot = getUiccSlotForPhone(phoneId);
                if (uiccSlot != null) {
                    return uiccSlot.getUiccCard();
                }
            }
            return null;
        }
    }

    /**
     * API to get UiccProfile corresponding to given phone id
     * @return UiccProfile object corresponding to given phone id; null if there is no card/profile
     * present for the phone id
     */
    public UiccProfile getUiccProfileForPhone(int phoneId) {
        synchronized (mLock) {
            if (isValidPhoneIndex(phoneId)) {
                UiccCard uiccCard = getUiccCardForPhone(phoneId);
                return uiccCard != null ? uiccCard.getUiccProfile() : null;
            }
            return null;
        }
    }

    /**
     * API to get all the UICC slots.
     * @return UiccSlots array.
     */
    public UiccSlot[] getUiccSlots() {
        synchronized (mLock) {
            return mUiccSlots;
        }
    }

    /** Map logicalSlot to physicalSlot, and activate the physicalSlot if it is inactive. */
    public void switchSlots(int[] physicalSlots, Message response) {
        logWithLocalLog("switchSlots: " + Arrays.toString(physicalSlots));
        mRadioConfig.setSimSlotsMapping(physicalSlots, response);
    }

    /**
     * API to get UiccSlot object for a specific physical slot index on the device
     * @return UiccSlot object for the given physical slot index
     */
    public UiccSlot getUiccSlot(int slotId) {
        synchronized (mLock) {
            if (isValidSlotIndex(slotId)) {
                return mUiccSlots[slotId];
            }
            return null;
        }
    }

    /**
     * API to get UiccSlot object for a given phone id
     * @return UiccSlot object for the given phone id
     */
    public UiccSlot getUiccSlotForPhone(int phoneId) {
        synchronized (mLock) {
            if (isValidPhoneIndex(phoneId)) {
                int slotId = getSlotIdFromPhoneId(phoneId);
                if (isValidSlotIndex(slotId)) {
                    return mUiccSlots[slotId];
                }
            }
            return null;
        }
    }

    /**
     * API to get UiccSlot object for a given cardId
     * @param cardId Identifier for a SIM. This can be an ICCID, or an EID in case of an eSIM.
     * @return int Index of UiccSlot for the given cardId if one is found, {@link #INVALID_SLOT_ID}
     * otherwise
     */
    public int getUiccSlotForCardId(String cardId) {
        synchronized (mLock) {
            // first look up based on cardId
            for (int idx = 0; idx < mUiccSlots.length; idx++) {
                if (mUiccSlots[idx] != null) {
                    UiccCard uiccCard = mUiccSlots[idx].getUiccCard();
                    if (uiccCard != null && cardId.equals(uiccCard.getCardId())) {
                        return idx;
                    }
                }
            }
            // if a match is not found, do a lookup based on ICCID
            for (int idx = 0; idx < mUiccSlots.length; idx++) {
                if (mUiccSlots[idx] != null && cardId.equals(mUiccSlots[idx].getIccId())) {
                    return idx;
                }
            }
            return INVALID_SLOT_ID;
        }
    }

    // Easy to use API
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public IccRecords getIccRecords(int phoneId, int family) {
        synchronized (mLock) {
            UiccCardApplication app = getUiccCardApplication(phoneId, family);
            if (app != null) {
                return app.getIccRecords();
            }
            return null;
        }
    }

    // Easy to use API
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public IccFileHandler getIccFileHandler(int phoneId, int family) {
        synchronized (mLock) {
            UiccCardApplication app = getUiccCardApplication(phoneId, family);
            if (app != null) {
                return app.getIccFileHandler();
            }
            return null;
        }
    }


    //Notifies when card status changes
    @UnsupportedAppUsage
    public void registerForIccChanged(Handler h, int what, Object obj) {
        synchronized (mLock) {
            mIccChangedRegistrants.addUnique(h, what, obj);
        }
        //Notify registrant right after registering, so that it will get the latest ICC status,
        //otherwise which may not happen until there is an actual change in ICC status.
        Message.obtain(h, what, new AsyncResult(obj, null, null)).sendToTarget();
    }

    public void unregisterForIccChanged(Handler h) {
        synchronized (mLock) {
            mIccChangedRegistrants.remove(h);
        }
    }

    @Override
    public void handleMessage (Message msg) {
        synchronized (mLock) {
            Integer phoneId = getCiIndex(msg);
            String eventName = eventToString(msg.what);

            if (phoneId < 0 || phoneId >= mCis.length) {
                Rlog.e(LOG_TAG, "Invalid phoneId : " + phoneId + " received with event "
                        + eventName);
                return;
            }

            logWithLocalLog("handleMessage: Received " + eventName + " for phoneId " + phoneId);

            AsyncResult ar = (AsyncResult)msg.obj;
            switch (msg.what) {
                case EVENT_ICC_STATUS_CHANGED:
                    if (DBG) log("Received EVENT_ICC_STATUS_CHANGED, calling getIccCardStatus");
                    mCis[phoneId].getIccCardStatus(obtainMessage(EVENT_GET_ICC_STATUS_DONE,
                            phoneId));
                    break;
                case EVENT_RADIO_AVAILABLE:
                case EVENT_RADIO_ON:
                    if (DBG) {
                        log("Received EVENT_RADIO_AVAILABLE/EVENT_RADIO_ON, calling "
                                + "getIccCardStatus");
                    }
                    mCis[phoneId].getIccCardStatus(obtainMessage(EVENT_GET_ICC_STATUS_DONE,
                            phoneId));
                    // slot status should be the same on all RILs; request it only for phoneId 0
                    if (phoneId == 0) {
                        if (DBG) {
                            log("Received EVENT_RADIO_AVAILABLE/EVENT_RADIO_ON for phoneId 0, "
                                    + "calling getIccSlotsStatus");
                        }
                        mRadioConfig.getSimSlotsStatus(obtainMessage(EVENT_GET_SLOT_STATUS_DONE,
                                phoneId));
                    }
                    break;
                case EVENT_GET_ICC_STATUS_DONE:
                    if (DBG) log("Received EVENT_GET_ICC_STATUS_DONE");
                    onGetIccCardStatusDone(ar, phoneId);
                    break;
                case EVENT_SLOT_STATUS_CHANGED:
                case EVENT_GET_SLOT_STATUS_DONE:
                    if (DBG) {
                        log("Received EVENT_SLOT_STATUS_CHANGED or EVENT_GET_SLOT_STATUS_DONE");
                    }
                    onGetSlotStatusDone(ar);
                    break;
                case EVENT_RADIO_UNAVAILABLE:
                    if (DBG) log("EVENT_RADIO_UNAVAILABLE, dispose card");
                    UiccSlot uiccSlot = getUiccSlotForPhone(phoneId);
                    if (uiccSlot != null) {
                        uiccSlot.onRadioStateUnavailable();
                    }
                    mIccChangedRegistrants.notifyRegistrants(new AsyncResult(null, phoneId, null));
                    break;
                case EVENT_SIM_REFRESH:
                    if (DBG) log("Received EVENT_SIM_REFRESH");
                    onSimRefresh(ar, phoneId);
                    break;
                case EVENT_EID_READY:
                    if (DBG) log("Received EVENT_EID_READY");
                    onEidReady(ar, phoneId);
                    break;
                case EVENT_MULTI_SIM_CONFIG_CHANGED:
                    if (DBG) log("Received EVENT_MULTI_SIM_CONFIG_CHANGED");
                    int activeModemCount = (int) ((AsyncResult) msg.obj).result;
                    onMultiSimConfigChanged(activeModemCount);
                    break;
                default:
                    Rlog.e(LOG_TAG, " Unknown Event " + msg.what);
                    break;
            }
        }
    }

    private void onMultiSimConfigChanged(int newActiveModemCount) {
        int prevActiveModemCount = mCis.length;
        mCis = PhoneFactory.getCommandsInterfaces();

        logWithLocalLog("onMultiSimConfigChanged: prevActiveModemCount " + prevActiveModemCount
                + ", newActiveModemCount " + newActiveModemCount);

        // Resize array.
        mPhoneIdToSlotId = copyOf(mPhoneIdToSlotId, newActiveModemCount);

        // Register for new active modem for ss -> ds switch.
        // For ds -> ss switch, there's no need to unregister as the mCis should unregister
        // everything itself.
        for (int i = prevActiveModemCount; i < newActiveModemCount; i++) {
            mPhoneIdToSlotId[i] = INVALID_SLOT_ID;
            mCis[i].registerForIccStatusChanged(this, EVENT_ICC_STATUS_CHANGED, i);

            /*
             * To support FDE (deprecated), additional check is needed:
             *
             * if (!StorageManager.inCryptKeeperBounce()) {
             *     mCis[i].registerForAvailable(this, EVENT_RADIO_AVAILABLE, i);
             * } else {
             *     mCis[i].registerForOn(this, EVENT_RADIO_ON, i);
             * }
             */
            mCis[i].registerForAvailable(this, EVENT_RADIO_AVAILABLE, i);

            mCis[i].registerForNotAvailable(this, EVENT_RADIO_UNAVAILABLE, i);
            mCis[i].registerForIccRefresh(this, EVENT_SIM_REFRESH, i);
        }
    }

    private Integer getCiIndex(Message msg) {
        AsyncResult ar;
        Integer index = new Integer(PhoneConstants.DEFAULT_SLOT_INDEX);

        /*
         * The events can be come in two ways. By explicitly sending it using
         * sendMessage, in this case the user object passed is msg.obj and from
         * the CommandsInterface, in this case the user object is msg.obj.userObj
         */
        if (msg != null) {
            if (msg.obj != null && msg.obj instanceof Integer) {
                index = (Integer)msg.obj;
            } else if(msg.obj != null && msg.obj instanceof AsyncResult) {
                ar = (AsyncResult)msg.obj;
                if (ar.userObj != null && ar.userObj instanceof Integer) {
                    index = (Integer)ar.userObj;
                }
            }
        }
        return index;
    }

    private static String eventToString(int event) {
        switch (event) {
            case EVENT_ICC_STATUS_CHANGED: return "ICC_STATUS_CHANGED";
            case EVENT_SLOT_STATUS_CHANGED: return "SLOT_STATUS_CHANGED";
            case EVENT_GET_ICC_STATUS_DONE: return "GET_ICC_STATUS_DONE";
            case EVENT_GET_SLOT_STATUS_DONE: return "GET_SLOT_STATUS_DONE";
            case EVENT_RADIO_ON: return "RADIO_ON";
            case EVENT_RADIO_AVAILABLE: return "RADIO_AVAILABLE";
            case EVENT_RADIO_UNAVAILABLE: return "RADIO_UNAVAILABLE";
            case EVENT_SIM_REFRESH: return "SIM_REFRESH";
            case EVENT_EID_READY: return "EID_READY";
            case EVENT_MULTI_SIM_CONFIG_CHANGED: return "MULTI_SIM_CONFIG_CHANGED";
            default: return "UNKNOWN(" + event + ")";
        }
    }

    // Easy to use API
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public UiccCardApplication getUiccCardApplication(int phoneId, int family) {
        synchronized (mLock) {
            UiccCard uiccCard = getUiccCardForPhone(phoneId);
            if (uiccCard != null) {
                return uiccCard.getApplication(family);
            }
            return null;
        }
    }

    /**
     * Convert IccCardConstants.State enum values to corresponding IccCardConstants String
     * constants
     * @param state IccCardConstants.State enum value
     * @return IccCardConstants String constant representing ICC state
     */
    public static String getIccStateIntentString(IccCardConstants.State state) {
        switch (state) {
            case ABSENT: return IccCardConstants.INTENT_VALUE_ICC_ABSENT;
            case PIN_REQUIRED: return IccCardConstants.INTENT_VALUE_ICC_LOCKED;
            case PUK_REQUIRED: return IccCardConstants.INTENT_VALUE_ICC_LOCKED;
            case NETWORK_LOCKED: return IccCardConstants.INTENT_VALUE_ICC_LOCKED;
            case READY: return IccCardConstants.INTENT_VALUE_ICC_READY;
            case NOT_READY: return IccCardConstants.INTENT_VALUE_ICC_NOT_READY;
            case PERM_DISABLED: return IccCardConstants.INTENT_VALUE_ICC_LOCKED;
            case CARD_IO_ERROR: return IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR;
            case CARD_RESTRICTED: return IccCardConstants.INTENT_VALUE_ICC_CARD_RESTRICTED;
            case LOADED: return IccCardConstants.INTENT_VALUE_ICC_LOADED;
            default: return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
        }
    }

    static void updateInternalIccStateForInactiveSlot(
            Context context, int prevActivePhoneId, String iccId) {
        if (SubscriptionManager.isValidPhoneId(prevActivePhoneId)) {
            // Mark SIM state as ABSENT on previously phoneId.
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(
                    Context.TELEPHONY_SERVICE);
            telephonyManager.setSimStateForPhone(prevActivePhoneId,
                    IccCardConstants.State.ABSENT.toString());
        }

        SubscriptionInfoUpdater subInfoUpdator = PhoneFactory.getSubscriptionInfoUpdater();
        if (subInfoUpdator != null) {
            subInfoUpdator.updateInternalIccStateForInactiveSlot(prevActivePhoneId, iccId);
        } else {
            Rlog.e(LOG_TAG, "subInfoUpdate is null.");
        }
    }

    static void updateInternalIccState(Context context, IccCardConstants.State state, String reason,
            int phoneId) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        telephonyManager.setSimStateForPhone(phoneId, state.toString());

        SubscriptionInfoUpdater subInfoUpdator = PhoneFactory.getSubscriptionInfoUpdater();
        if (subInfoUpdator != null) {
            subInfoUpdator.updateInternalIccState(getIccStateIntentString(state), reason, phoneId);
        } else {
            Rlog.e(LOG_TAG, "subInfoUpdate is null.");
        }
    }

    private synchronized void onGetIccCardStatusDone(AsyncResult ar, Integer index) {
        if (ar.exception != null) {
            Rlog.e(LOG_TAG,"Error getting ICC status. "
                    + "RIL_REQUEST_GET_ICC_STATUS should "
                    + "never return an error", ar.exception);
            return;
        }
        if (!isValidPhoneIndex(index)) {
            Rlog.e(LOG_TAG,"onGetIccCardStatusDone: invalid index : " + index);
            return;
        }
        if (isShuttingDown()) {
            // Do not process the SIM/SLOT events during device shutdown,
            // as it may unnecessarily modify the persistent information
            // like, SubscriptionManager.UICC_APPLICATIONS_ENABLED.
            log("onGetIccCardStatusDone: shudown in progress ignore event");
            return;
        }

        IccCardStatus status = (IccCardStatus)ar.result;

        logWithLocalLog("onGetIccCardStatusDone: phoneId " + index + " IccCardStatus: " + status);

        int slotId = status.physicalSlotIndex;
        if (VDBG) log("onGetIccCardStatusDone: phoneId " + index + " physicalSlotIndex " + slotId);
        if (slotId == INVALID_SLOT_ID) {
            slotId = index;
        }

        if (eidIsNotSupported(status)) {
            // we will never get EID from the HAL, so set mDefaultEuiccCardId to UNSUPPORTED_CARD_ID
            if (DBG) log("eid is not supported");
            mDefaultEuiccCardId = UNSUPPORTED_CARD_ID;
        }
        mPhoneIdToSlotId[index] = slotId;

        if (VDBG) logPhoneIdToSlotIdMapping();

        if (mUiccSlots[slotId] == null) {
            if (VDBG) {
                log("Creating mUiccSlots[" + slotId + "]; mUiccSlots.length = "
                        + mUiccSlots.length);
            }
            mUiccSlots[slotId] = new UiccSlot(mContext, true);
        }

        mUiccSlots[slotId].update(mCis[index], status, index, slotId);

        UiccCard card = mUiccSlots[slotId].getUiccCard();
        if (card == null) {
            if (DBG) log("mUiccSlots[" + slotId + "] has no card. Notifying IccChangedRegistrants");
            mIccChangedRegistrants.notifyRegistrants(new AsyncResult(null, index, null));
            return;
        }

        String cardString = null;
        boolean isEuicc = mUiccSlots[slotId].isEuicc();
        if (isEuicc) {
            cardString = ((EuiccCard) card).getEid();
        } else {
            cardString = card.getIccId();
        }

        if (cardString != null) {
            addCardId(cardString);
        }

        // EID is unpopulated if Radio HAL < 1.4 (RadioConfig < 1.2)
        // If so, just register for EID loaded and skip this stuff
        if (isEuicc && mDefaultEuiccCardId != UNSUPPORTED_CARD_ID) {
            if (cardString == null) {
                ((EuiccCard) card).registerForEidReady(this, EVENT_EID_READY, index);
            } else {
                // If we know the EID from IccCardStatus, just use it to set mDefaultEuiccCardId if
                // it's not already set.
                // This is needed in cases where slot status doesn't include EID, and we don't want
                // to register for EID from APDU because we already know cardString from a previous
                // APDU
                if (mDefaultEuiccCardId == UNINITIALIZED_CARD_ID
                        || mDefaultEuiccCardId == TEMPORARILY_UNSUPPORTED_CARD_ID) {
                    mDefaultEuiccCardId = convertToPublicCardId(cardString);
                    logWithLocalLog("IccCardStatus eid=" + cardString + " slot=" + slotId
                            + " mDefaultEuiccCardId=" + mDefaultEuiccCardId);
                }
            }
        }

        if (DBG) log("Notifying IccChangedRegistrants");
        mIccChangedRegistrants.notifyRegistrants(new AsyncResult(null, index, null));
    }

    /**
     * Returns true if EID is not supproted.
     */
    private boolean eidIsNotSupported(IccCardStatus status) {
        // if card status does not contain slot ID, we know we are on HAL < 1.2, so EID will never
        // be available
        return status.physicalSlotIndex == INVALID_SLOT_ID;
    }

    /**
     * Add a cardString to mCardStrings. If this is an ICCID, trailing Fs will be automatically
     * stripped.
     */
    private void addCardId(String cardString) {
        if (TextUtils.isEmpty(cardString)) {
            return;
        }
        if (cardString.length() < EID_LENGTH) {
            cardString = IccUtils.stripTrailingFs(cardString);
        }
        if (!mCardStrings.contains(cardString)) {
            mCardStrings.add(cardString);
            saveCardStrings();
        }
    }

    /**
     * Converts an integer cardId (public card ID) to a card string.
     * @param cardId to convert
     * @return cardString, or null if the cardId is not valid
     */
    public String convertToCardString(int cardId) {
        if (cardId < 0 || cardId >= mCardStrings.size()) {
            log("convertToCardString: cardId " + cardId + " is not valid");
            return null;
        }
        return mCardStrings.get(cardId);
    }

    /**
     * Converts the card string (the ICCID/EID, formerly named card ID) to the public int cardId.
     * If the given cardString is an ICCID, trailing Fs will be automatically stripped before trying
     * to match to a card ID.
     *
     * @return the matching cardId, or UNINITIALIZED_CARD_ID if the card string does not map to a
     * currently loaded cardId, or UNSUPPORTED_CARD_ID if the device does not support card IDs
     */
    public int convertToPublicCardId(String cardString) {
        if (mDefaultEuiccCardId == UNSUPPORTED_CARD_ID) {
            // even if cardString is not an EID, if EID is not supported (e.g. HAL < 1.2) we can't
            // guarentee a working card ID implementation, so return UNSUPPORTED_CARD_ID
            return UNSUPPORTED_CARD_ID;
        }
        if (TextUtils.isEmpty(cardString)) {
            return UNINITIALIZED_CARD_ID;
        }

        if (cardString.length() < EID_LENGTH) {
            cardString = IccUtils.stripTrailingFs(cardString);
        }
        int id = mCardStrings.indexOf(cardString);
        if (id == -1) {
            return UNINITIALIZED_CARD_ID;
        } else {
            return id;
        }
    }

    /**
     * Returns the UiccCardInfo of all currently inserted UICCs and embedded eUICCs.
     */
    public ArrayList<UiccCardInfo> getAllUiccCardInfos() {
        ArrayList<UiccCardInfo> infos = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < mUiccSlots.length; slotIndex++) {
            final UiccSlot slot = mUiccSlots[slotIndex];
            if (slot == null) continue;
            boolean isEuicc = slot.isEuicc();
            String eid = null;
            UiccCard card = slot.getUiccCard();
            String iccid = null;
            int cardId = UNINITIALIZED_CARD_ID;
            boolean isRemovable = slot.isRemovable();

            // first we try to populate UiccCardInfo using the UiccCard, but if it doesn't exist
            // (e.g. the slot is for an inactive eUICC) then we try using the UiccSlot.
            if (card != null) {
                iccid = card.getIccId();
                if (isEuicc) {
                    eid = ((EuiccCard) card).getEid();
                    cardId = convertToPublicCardId(eid);
                } else {
                    // leave eid null if the UICC is not embedded
                    cardId = convertToPublicCardId(iccid);
                }
            } else {
                iccid = slot.getIccId();
                // Fill in the fields we can
                if (!isEuicc && !TextUtils.isEmpty(iccid)) {
                    cardId = convertToPublicCardId(iccid);
                }
            }
            UiccCardInfo info = new UiccCardInfo(isEuicc, cardId, eid,
                    IccUtils.stripTrailingFs(iccid), slotIndex, isRemovable);
            infos.add(info);
        }
        return infos;
    }

    /**
     * Get the card ID of the default eUICC.
     */
    public int getCardIdForDefaultEuicc() {
        if (mDefaultEuiccCardId == TEMPORARILY_UNSUPPORTED_CARD_ID) {
            return UNSUPPORTED_CARD_ID;
        }
        return mDefaultEuiccCardId;
    }

    /** Get the {@link PinStorage}. */
    public PinStorage getPinStorage() {
        return mPinStorage;
    }

    private ArrayList<String> loadCardStrings() {
        String cardStrings =
                PreferenceManager.getDefaultSharedPreferences(mContext).getString(CARD_STRINGS, "");
        if (TextUtils.isEmpty(cardStrings)) {
            // just return an empty list, since String.split would return the list { "" }
            return new ArrayList<String>();
        }
        return new ArrayList<String>(Arrays.asList(cardStrings.split(",")));
    }

    private void saveCardStrings() {
        SharedPreferences.Editor editor =
                PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        editor.putString(CARD_STRINGS, TextUtils.join(",", mCardStrings));
        editor.commit();
    }

    private synchronized void onGetSlotStatusDone(AsyncResult ar) {
        if (!mIsSlotStatusSupported) {
            if (VDBG) log("onGetSlotStatusDone: ignoring since mIsSlotStatusSupported is false");
            return;
        }
        Throwable e = ar.exception;
        if (e != null) {
            if (!(e instanceof CommandException) || ((CommandException) e).getCommandError()
                    != CommandException.Error.REQUEST_NOT_SUPPORTED) {
                // this is not expected; there should be no exception other than
                // REQUEST_NOT_SUPPORTED
                logeWithLocalLog("Unexpected error getting slot status: " + ar.exception);
            } else {
                // REQUEST_NOT_SUPPORTED
                logWithLocalLog("onGetSlotStatusDone: request not supported; marking "
                        + "mIsSlotStatusSupported to false");
                mIsSlotStatusSupported = false;
            }
            return;
        }
        if (isShuttingDown()) {
            // Do not process the SIM/SLOT events during device shutdown,
            // as it may unnecessarily modify the persistent information
            // like, SubscriptionManager.UICC_APPLICATIONS_ENABLED.
            log("onGetSlotStatusDone: shudown in progress ignore event");
            return;
        }

        ArrayList<IccSlotStatus> status = (ArrayList<IccSlotStatus>) ar.result;

        if (!slotStatusChanged(status)) {
            log("onGetSlotStatusDone: No change in slot status");
            return;
        }
        logWithLocalLog("onGetSlotStatusDone: " + status);

        sLastSlotStatus = status;

        int numActiveSlots = 0;
        boolean isDefaultEuiccCardIdSet = false;
        boolean anyEuiccIsActive = false;
        mHasActiveBuiltInEuicc = false;

        int numSlots = status.size();
        if (mUiccSlots.length < numSlots) {
            logeWithLocalLog("The number of the physical slots reported " + numSlots
                    + " is greater than the expectation " + mUiccSlots.length);
            numSlots = mUiccSlots.length;
        }

        for (int i = 0; i < numSlots; i++) {
            IccSlotStatus iss = status.get(i);
            boolean isActive = (iss.slotState == IccSlotStatus.SlotState.SLOTSTATE_ACTIVE);
            if (isActive) {
                numActiveSlots++;

                // Correctness check: logicalSlotIndex should be valid for an active slot
                if (!isValidPhoneIndex(iss.logicalSlotIndex)) {
                    Rlog.e(LOG_TAG, "Skipping slot " + i + " as phone " + iss.logicalSlotIndex
                               + " is not available to communicate with this slot");
                } else {
                    mPhoneIdToSlotId[iss.logicalSlotIndex] = i;
                }
            }

            if (mUiccSlots[i] == null) {
                if (VDBG) {
                    log("Creating mUiccSlot[" + i + "]; mUiccSlots.length = " + mUiccSlots.length);
                }
                mUiccSlots[i] = new UiccSlot(mContext, isActive);
            }

            if (!isValidPhoneIndex(iss.logicalSlotIndex)) {
                mUiccSlots[i].update(null, iss, i /* slotIndex */);
            } else {
                mUiccSlots[i].update(isActive ? mCis[iss.logicalSlotIndex] : null, iss,
                        i /* slotIndex */);
            }

            if (mUiccSlots[i].isEuicc()) {
                if (isActive) {
                    anyEuiccIsActive = true;

                    if (isBuiltInEuiccSlot(i)) {
                        mHasActiveBuiltInEuicc = true;
                    }
                }
                String eid = iss.eid;
                if (TextUtils.isEmpty(eid)) {
                    // iss.eid is not populated on HAL<1.4
                    continue;
                }

                addCardId(eid);

                // whenever slot status is received, set default card to the non-removable eUICC
                // with the lowest slot index.
                if (!mUiccSlots[i].isRemovable() && !isDefaultEuiccCardIdSet) {
                    isDefaultEuiccCardIdSet = true;
                    mDefaultEuiccCardId = convertToPublicCardId(eid);
                    logWithLocalLog("Using eid=" + eid + " in slot=" + i
                            + " to set mDefaultEuiccCardId=" + mDefaultEuiccCardId);
                }
            }
        }

        if (!mHasActiveBuiltInEuicc && !isDefaultEuiccCardIdSet) {
            // if there are no active built-in eUICCs, then consider setting a removable eUICC to
            // the default.
            // Note that on HAL<1.2, it's possible that a built-in eUICC exists, but does not
            // correspond to any slot in mUiccSlots. This logic is still safe in that case because
            // SlotStatus is only for HAL >= 1.2
            for (int i = 0; i < numSlots; i++) {
                if (mUiccSlots[i].isEuicc()) {
                    String eid = status.get(i).eid;
                    if (!TextUtils.isEmpty(eid)) {
                        isDefaultEuiccCardIdSet = true;
                        mDefaultEuiccCardId = convertToPublicCardId(eid);
                        logWithLocalLog("Using eid=" + eid + " from removable eUICC in slot="
                                + i + " to set mDefaultEuiccCardId=" + mDefaultEuiccCardId);
                        break;
                    }
                }
            }
        }

        if (mHasBuiltInEuicc && !anyEuiccIsActive && !isDefaultEuiccCardIdSet) {
            logWithLocalLog(
                    "onGetSlotStatusDone: mDefaultEuiccCardId=TEMPORARILY_UNSUPPORTED_CARD_ID");
            isDefaultEuiccCardIdSet = true;
            mDefaultEuiccCardId = TEMPORARILY_UNSUPPORTED_CARD_ID;
        }


        if (!isDefaultEuiccCardIdSet) {
            if (mDefaultEuiccCardId >= 0) {
                // if mDefaultEuiccCardId has already been set to an actual eUICC,
                // don't overwrite mDefaultEuiccCardId unless that eUICC is no longer inserted
                boolean defaultEuiccCardIdIsStillInserted = false;
                String cardString = mCardStrings.get(mDefaultEuiccCardId);
                for (UiccSlot slot : mUiccSlots) {
                    if (slot.getUiccCard() == null) {
                        continue;
                    }
                    if (cardString.equals(
                            IccUtils.stripTrailingFs(slot.getUiccCard().getCardId()))) {
                        defaultEuiccCardIdIsStillInserted = true;
                    }
                }
                if (!defaultEuiccCardIdIsStillInserted) {
                    logWithLocalLog("onGetSlotStatusDone: mDefaultEuiccCardId="
                            + mDefaultEuiccCardId
                            + " is no longer inserted. Setting mDefaultEuiccCardId=UNINITIALIZED");
                    mDefaultEuiccCardId = UNINITIALIZED_CARD_ID;
                }
            } else {
                // no known eUICCs at all (it's possible that an eUICC is inserted and we just don't
                // know it's EID)
                logWithLocalLog("onGetSlotStatusDone: mDefaultEuiccCardId=UNINITIALIZED");
                mDefaultEuiccCardId = UNINITIALIZED_CARD_ID;
            }
        }

        if (VDBG) logPhoneIdToSlotIdMapping();

        // Correctness check: number of active slots should be valid
        if (numActiveSlots != mPhoneIdToSlotId.length) {
            Rlog.e(LOG_TAG, "Number of active slots " + numActiveSlots
                       + " does not match the number of Phones" + mPhoneIdToSlotId.length);
        }

        // Correctness check: slotIds should be unique in mPhoneIdToSlotId
        Set<Integer> slotIds = new HashSet<>();
        for (int slotId : mPhoneIdToSlotId) {
            if (slotIds.contains(slotId)) {
                throw new RuntimeException("slotId " + slotId + " mapped to multiple phoneIds");
            }
            slotIds.add(slotId);
        }

        // broadcast slot status changed
        final BroadcastOptions options = BroadcastOptions.makeBasic();
        options.setBackgroundActivityStartsAllowed(true);
        Intent intent = new Intent(TelephonyManager.ACTION_SIM_SLOT_STATUS_CHANGED);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        mContext.sendBroadcast(intent, android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE,
                options.toBundle());
    }

    private boolean slotStatusChanged(ArrayList<IccSlotStatus> slotStatusList) {
        if (sLastSlotStatus == null || sLastSlotStatus.size() != slotStatusList.size()) {
            return true;
        }
        for (IccSlotStatus iccSlotStatus : slotStatusList) {
            if (!sLastSlotStatus.contains(iccSlotStatus)) {
                return true;
            }
        }
        return false;
    }

    private void logPhoneIdToSlotIdMapping() {
        log("mPhoneIdToSlotId mapping:");
        for (int i = 0; i < mPhoneIdToSlotId.length; i++) {
            log("    phoneId " + i + " slotId " + mPhoneIdToSlotId[i]);
        }
    }

    private void onSimRefresh(AsyncResult ar, Integer index) {
        if (ar.exception != null) {
            Rlog.e(LOG_TAG, "onSimRefresh: Sim REFRESH with exception: " + ar.exception);
            return;
        }

        if (!isValidPhoneIndex(index)) {
            Rlog.e(LOG_TAG,"onSimRefresh: invalid index : " + index);
            return;
        }

        IccRefreshResponse resp = (IccRefreshResponse) ar.result;
        logWithLocalLog("onSimRefresh: index " + index + ", " + resp);

        if (resp == null) {
            Rlog.e(LOG_TAG, "onSimRefresh: received without input");
            return;
        }

        UiccCard uiccCard = getUiccCardForPhone(index);
        if (uiccCard == null) {
            Rlog.e(LOG_TAG,"onSimRefresh: refresh on null card : " + index);
            return;
        }

        boolean changed = false;
        switch(resp.refreshResult) {
            // Reset the required apps when we know about the refresh so that
            // anyone interested does not get stale state.
            case IccRefreshResponse.REFRESH_RESULT_RESET:
                changed = uiccCard.resetAppWithAid(resp.aid, true /* reset */);
                break;
            case IccRefreshResponse.REFRESH_RESULT_INIT:
                // don't dispose CatService on SIM REFRESH of type INIT
                changed = uiccCard.resetAppWithAid(resp.aid, false /* initialize */);
                break;
            default:
                return;
        }

        if (changed && resp.refreshResult == IccRefreshResponse.REFRESH_RESULT_RESET) {
            // If there is any change on RESET, reset carrier config as well. From carrier config
            // perspective, this is treated the same as sim state unknown
            CarrierConfigManager configManager = (CarrierConfigManager)
                    mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            configManager.updateConfigForPhoneId(index, IccCardConstants.INTENT_VALUE_ICC_UNKNOWN);
        }

        // The card status could have changed. Get the latest state.
        mCis[index].getIccCardStatus(obtainMessage(EVENT_GET_ICC_STATUS_DONE, index));
    }

    // for HAL 1.2-1.3 we register for EID ready, set mCardStrings and mDefaultEuiccCardId here.
    // Note that if there are multiple eUICCs on HAL 1.2-1.3, the default eUICC is the one whose EID
    // is first loaded
    private void onEidReady(AsyncResult ar, Integer index) {
        if (ar.exception != null) {
            Rlog.e(LOG_TAG, "onEidReady: exception: " + ar.exception);
            return;
        }

        if (!isValidPhoneIndex(index)) {
            Rlog.e(LOG_TAG, "onEidReady: invalid index: " + index);
            return;
        }
        int slotId = mPhoneIdToSlotId[index];
        EuiccCard card = (EuiccCard) mUiccSlots[slotId].getUiccCard();
        if (card == null) {
            Rlog.e(LOG_TAG, "onEidReady: UiccCard in slot " + slotId + " is null");
            return;
        }

        // set mCardStrings and the defaultEuiccCardId using the now available EID
        String eid = card.getEid();
        addCardId(eid);
        if (mDefaultEuiccCardId == UNINITIALIZED_CARD_ID
                || mDefaultEuiccCardId == TEMPORARILY_UNSUPPORTED_CARD_ID) {
            if (!mUiccSlots[slotId].isRemovable()) {
                mDefaultEuiccCardId = convertToPublicCardId(eid);
                logWithLocalLog("onEidReady: eid=" + eid + " slot=" + slotId
                        + " mDefaultEuiccCardId=" + mDefaultEuiccCardId);
            } else if (!mHasActiveBuiltInEuicc) {
                // we only set a removable eUICC to the default if there are no active non-removable
                // eUICCs
                mDefaultEuiccCardId = convertToPublicCardId(eid);
                logWithLocalLog("onEidReady: eid=" + eid + " from removable eUICC in slot=" + slotId
                        + " mDefaultEuiccCardId=" + mDefaultEuiccCardId);
            }
        }
        card.unregisterForEidReady(this);
    }

    // Return true if the device has at least one built in eUICC based on the resource overlay
    private boolean hasBuiltInEuicc() {
        return mEuiccSlots != null &&  mEuiccSlots.length > 0;
    }

    private boolean isBuiltInEuiccSlot(int slotIndex) {
        if (!mHasBuiltInEuicc) {
            return false;
        }
        for (int slot : mEuiccSlots) {
            if (slot == slotIndex) {
                return true;
            }
        }
        return false;
    }

    /**
     * static method to return whether CDMA is supported on the device
     * @param context object representative of the application that is calling this method
     * @return true if CDMA is supported by the device
     */
    public static boolean isCdmaSupported(Context context) {
        PackageManager packageManager = context.getPackageManager();
        boolean isCdmaSupported =
                packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA);
        return isCdmaSupported;
    }

    private boolean isValidPhoneIndex(int index) {
        return (index >= 0 && index < TelephonyManager.getDefault().getPhoneCount());
    }

    private boolean isValidSlotIndex(int index) {
        return (index >= 0 && index < mUiccSlots.length);
    }

    private boolean isShuttingDown() {
        for (int i = 0; i < TelephonyManager.getDefault().getActiveModemCount(); i++) {
            if (PhoneFactory.getPhone(i) != null &&
                    PhoneFactory.getPhone(i).isShuttingDown()) {
                return true;
            }
        }
        return false;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void log(String string) {
        Rlog.d(LOG_TAG, string);
    }

    private void logWithLocalLog(String string) {
        Rlog.d(LOG_TAG, string);
        sLocalLog.log("UiccController: " + string);
    }

    private void logeWithLocalLog(String string) {
        Rlog.e(LOG_TAG, string);
        sLocalLog.log("UiccController: " + string);
    }

    /** The supplied log should also indicate the caller to avoid ambiguity. */
    public static void addLocalLog(String data) {
        sLocalLog.log(data);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("UiccController: " + this);
        pw.println(" mContext=" + mContext);
        pw.println(" mInstance=" + mInstance);
        pw.println(" mIccChangedRegistrants: size=" + mIccChangedRegistrants.size());
        for (int i = 0; i < mIccChangedRegistrants.size(); i++) {
            pw.println("  mIccChangedRegistrants[" + i + "]="
                    + ((Registrant)mIccChangedRegistrants.get(i)).getHandler());
        }
        pw.println();
        pw.flush();
        pw.println(" mIsCdmaSupported=" + isCdmaSupported(mContext));
        pw.println(" mHasBuiltInEuicc=" + mHasBuiltInEuicc);
        pw.println(" mHasActiveBuiltInEuicc=" + mHasActiveBuiltInEuicc);
        pw.println(" mCardStrings=" + mCardStrings);
        pw.println(" mDefaultEuiccCardId=" + mDefaultEuiccCardId);
        pw.println(" mPhoneIdToSlotId=" + Arrays.toString(mPhoneIdToSlotId));
        pw.println(" mUiccSlots: size=" + mUiccSlots.length);
        for (int i = 0; i < mUiccSlots.length; i++) {
            if (mUiccSlots[i] == null) {
                pw.println("  mUiccSlots[" + i + "]=null");
            } else {
                pw.println("  mUiccSlots[" + i + "]=" + mUiccSlots[i]);
                mUiccSlots[i].dump(fd, pw, args);
            }
        }
        pw.println(" sLocalLog= ");
        sLocalLog.dump(fd, pw, args);
        mPinStorage.dump(fd, pw, args);
    }
}
