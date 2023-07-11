/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.android.internal.telephony.TelephonyStatsLog.PIN_STORAGE_EVENT;
import static com.android.internal.telephony.TelephonyStatsLog.PIN_STORAGE_EVENT__EVENT__PIN_VERIFICATION_FAILURE;
import static com.android.internal.telephony.TelephonyStatsLog.PIN_STORAGE_EVENT__EVENT__PIN_VERIFICATION_SUCCESS;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccAccessRule;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CarrierAppUtils;
import com.android.internal.telephony.CarrierPrivilegesTracker;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyStatsLog;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.PersoSubState;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.IccCardStatus.PinState;
import com.android.internal.telephony.uicc.euicc.EuiccCard;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class represents the carrier profiles in the {@link UiccCard}. Each profile contains
 * multiple {@link UiccCardApplication}, one {@link UiccCarrierPrivilegeRules} and one
 * {@link CatService}.
 *
 * Profile is related to {@link android.telephony.SubscriptionInfo} but those two concepts are
 * different. {@link android.telephony.SubscriptionInfo} contains all the subscription information
 * while Profile contains all the {@link UiccCardApplication} which will be used to fetch those
 * subscription information from the {@link UiccCard}.
 *
 * {@hide}
 */
public class UiccProfile extends IccCard {
    protected static final String LOG_TAG = "UiccProfile";
    protected static final boolean DBG = true;
    private static final boolean VDBG = false; //STOPSHIP if true

    private static final String OPERATOR_BRAND_OVERRIDE_PREFIX = "operator_branding_";

    // The lock object is created by UiccSlot that owns the UiccCard that owns this UiccProfile.
    // This is to share the lock between UiccSlot, UiccCard and UiccProfile for now.
    private final Object mLock;
    private PinState mUniversalPinState;
    private int mGsmUmtsSubscriptionAppIndex;
    private int mCdmaSubscriptionAppIndex;
    private int mImsSubscriptionAppIndex;
    private UiccCardApplication[] mUiccApplications =
            new UiccCardApplication[IccCardStatus.CARD_MAX_APPS];
    private Context mContext;
    private CommandsInterface mCi;
    private final UiccCard mUiccCard;
    private CatService mCatService;
    private UiccCarrierPrivilegeRules mCarrierPrivilegeRules;
    private UiccCarrierPrivilegeRules mTestOverrideCarrierPrivilegeRules;
    private boolean mDisposed = false;

    private RegistrantList mOperatorBrandOverrideRegistrants = new RegistrantList();

    private final int mPhoneId;
    private final PinStorage mPinStorage;

    private static final int EVENT_RADIO_OFF_OR_UNAVAILABLE = 1;
    private static final int EVENT_ICC_LOCKED = 2;
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public static final int EVENT_APP_READY = 3;
    private static final int EVENT_RECORDS_LOADED = 4;
    private static final int EVENT_NETWORK_LOCKED = 5;
    private static final int EVENT_EID_READY = 6;
    private static final int EVENT_ICC_RECORD_EVENTS = 7;
    private static final int EVENT_OPEN_LOGICAL_CHANNEL_DONE = 8;
    private static final int EVENT_CLOSE_LOGICAL_CHANNEL_DONE = 9;
    private static final int EVENT_TRANSMIT_APDU_LOGICAL_CHANNEL_DONE = 10;
    private static final int EVENT_TRANSMIT_APDU_BASIC_CHANNEL_DONE = 11;
    private static final int EVENT_SIM_IO_DONE = 12;
    private static final int EVENT_CARRIER_PRIVILEGES_LOADED = 13;
    private static final int EVENT_CARRIER_CONFIG_CHANGED = 14;
    private static final int EVENT_CARRIER_PRIVILEGES_TEST_OVERRIDE_SET = 15;
    private static final int EVENT_SUPPLY_ICC_PIN_DONE = 16;
    // NOTE: any new EVENT_* values must be added to eventToString.

    private TelephonyManager mTelephonyManager;

    private RegistrantList mNetworkLockedRegistrants = new RegistrantList();

    @VisibleForTesting
    public int mCurrentAppType = UiccController.APP_FAM_3GPP; //default to 3gpp?
    private int mRadioTech = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
    private UiccCardApplication mUiccApplication = null;
    private IccRecords mIccRecords = null;
    private IccCardConstants.State mExternalState = IccCardConstants.State.UNKNOWN;

    // The number of UiccApplications modem reported. It's different from mUiccApplications.length
    // which is always CARD_MAX_APPS, and only updated when modem sends an update, and NOT updated
    // during SIM refresh. It's currently only used to help identify empty profile.
    private int mLastReportedNumOfUiccApplications;

    private final ContentObserver mProvisionCompleteContentObserver =
            new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange) {
                    synchronized (mLock) {
                        mContext.getContentResolver().unregisterContentObserver(this);
                        mProvisionCompleteContentObserverRegistered = false;
                        showCarrierAppNotificationsIfPossible();
                    }
                }
            };
    private boolean mProvisionCompleteContentObserverRegistered;

    private final BroadcastReceiver mUserUnlockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mLock) {
                mContext.unregisterReceiver(this);
                mUserUnlockReceiverRegistered = false;
                showCarrierAppNotificationsIfPossible();
            }
        }
    };
    private boolean mUserUnlockReceiverRegistered;

    private final BroadcastReceiver mCarrierConfigChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED)) {
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_CARRIER_CONFIG_CHANGED));
            }
        }
    };

    @VisibleForTesting
    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String eventName = eventToString(msg.what);
            // We still need to handle the following response messages even the UiccProfile has been
            // disposed because whoever sent the request may be still waiting for the response.
            if (mDisposed && msg.what != EVENT_OPEN_LOGICAL_CHANNEL_DONE
                    && msg.what != EVENT_CLOSE_LOGICAL_CHANNEL_DONE
                    && msg.what != EVENT_TRANSMIT_APDU_LOGICAL_CHANNEL_DONE
                    && msg.what != EVENT_TRANSMIT_APDU_BASIC_CHANNEL_DONE
                    && msg.what != EVENT_SIM_IO_DONE) {
                loge("handleMessage: Received " + eventName
                        + " after dispose(); ignoring the message");
                return;
            }
            logWithLocalLog("handleMessage: Received " + eventName + " for phoneId " + mPhoneId);
            switch (msg.what) {
                case EVENT_NETWORK_LOCKED:
                    if (mUiccApplication != null) {
                        mNetworkLockedRegistrants.notifyRegistrants(new AsyncResult(
                                null, mUiccApplication.getPersoSubState().ordinal(), null));
                    } else {
                        log("EVENT_NETWORK_LOCKED: mUiccApplication is NULL, "
                                + "mNetworkLockedRegistrants not notified.");
                    }
                    // intentional fall through
                case EVENT_RADIO_OFF_OR_UNAVAILABLE:
                case EVENT_ICC_LOCKED:
                case EVENT_APP_READY:
                case EVENT_RECORDS_LOADED:
                case EVENT_EID_READY:
                    if (VDBG) log("handleMessage: Received " + eventName);
                    updateExternalState();
                    break;

                case EVENT_ICC_RECORD_EVENTS:
                    if ((mCurrentAppType == UiccController.APP_FAM_3GPP) && (mIccRecords != null)) {
                        AsyncResult ar = (AsyncResult) msg.obj;
                        int eventCode = (Integer) ar.result;
                        if (eventCode == SIMRecords.EVENT_SPN) {
                            mTelephonyManager.setSimOperatorNameForPhone(
                                    mPhoneId, mIccRecords.getServiceProviderName());
                        }
                    }
                    break;

                case EVENT_CARRIER_PRIVILEGES_LOADED:
                    if (VDBG) log("handleMessage: EVENT_CARRIER_PRIVILEGES_LOADED");
                    Phone phone = PhoneFactory.getPhone(mPhoneId);
                    if (phone != null) {
                        CarrierPrivilegesTracker cpt = phone.getCarrierPrivilegesTracker();
                        if (cpt != null) {
                            cpt.onUiccAccessRulesLoaded();
                        }
                    }
                    onCarrierPrivilegesLoadedMessage();
                    updateExternalState();
                    break;

                case EVENT_CARRIER_CONFIG_CHANGED:
                    handleCarrierNameOverride();
                    handleSimCountryIsoOverride();
                    break;

                case EVENT_OPEN_LOGICAL_CHANNEL_DONE:
                case EVENT_CLOSE_LOGICAL_CHANNEL_DONE:
                case EVENT_TRANSMIT_APDU_LOGICAL_CHANNEL_DONE:
                case EVENT_TRANSMIT_APDU_BASIC_CHANNEL_DONE:
                case EVENT_SIM_IO_DONE: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        logWithLocalLog("handleMessage: Error in SIM access with exception "
                                + ar.exception);
                    }
                    if (ar.userObj != null) {
                        AsyncResult.forMessage((Message) ar.userObj, ar.result, ar.exception);
                        ((Message) ar.userObj).sendToTarget();
                    } else {
                        loge("handleMessage: ar.userObj is null in event:" + eventName
                                + ", failed to post status back to caller");
                    }
                    break;
                }

                case EVENT_CARRIER_PRIVILEGES_TEST_OVERRIDE_SET:
                    if (msg.obj == null) {
                        mTestOverrideCarrierPrivilegeRules = null;
                    } else {
                        mTestOverrideCarrierPrivilegeRules =
                                new UiccCarrierPrivilegeRules((List<UiccAccessRule>) msg.obj);
                    }
                    refresh();
                    break;

                case EVENT_SUPPLY_ICC_PIN_DONE: {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        // An error occurred during automatic PIN verification. At this point,
                        // clear the cache and propagate the state.
                        loge("An error occurred during internal PIN verification");
                        mPinStorage.clearPin(mPhoneId);
                        updateExternalState();
                    } else {
                        log("Internal PIN verification was successful!");
                        // Nothing to do.
                    }
                    // Update metrics:
                    TelephonyStatsLog.write(
                            PIN_STORAGE_EVENT,
                            ar.exception != null
                                    ? PIN_STORAGE_EVENT__EVENT__PIN_VERIFICATION_FAILURE
                                    : PIN_STORAGE_EVENT__EVENT__PIN_VERIFICATION_SUCCESS,
                            /* number_of_pins= */ 1);
                    break;
                }

                default:
                    loge("handleMessage: Unhandled message with number: " + msg.what);
                    break;
            }
        }
    };

    public UiccProfile(Context c, CommandsInterface ci, IccCardStatus ics, int phoneId,
            UiccCard uiccCard, Object lock) {
        if (DBG) log("Creating profile");
        mLock = lock;
        mUiccCard = uiccCard;
        mPhoneId = phoneId;
        // set current app type based on phone type - do this before calling update() as that
        // calls updateIccAvailability() which uses mCurrentAppType
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null) {
            setCurrentAppType(phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM);
        }

        if (mUiccCard instanceof EuiccCard) {
            // for RadioConfig<1.2 eid is not known when the EuiccCard is constructed
            ((EuiccCard) mUiccCard).registerForEidReady(mHandler, EVENT_EID_READY, null);
        }
        mPinStorage = UiccController.getInstance().getPinStorage();

        update(c, ci, ics);
        ci.registerForOffOrNotAvailable(mHandler, EVENT_RADIO_OFF_OR_UNAVAILABLE, null);
        resetProperties();

        IntentFilter intentfilter = new IntentFilter();
        intentfilter.addAction(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        c.registerReceiver(mCarrierConfigChangedReceiver, intentfilter);
    }

    /**
     * Dispose the UiccProfile.
     */
    public void dispose() {
        if (DBG) log("Disposing profile");

        // mUiccCard is outside of mLock in order to prevent deadlocking. This is safe because
        // EuiccCard#unregisterForEidReady handles its own lock
        if (mUiccCard instanceof EuiccCard) {
            ((EuiccCard) mUiccCard).unregisterForEidReady(mHandler);
        }
        synchronized (mLock) {
            unregisterAllAppEvents();
            unregisterCurrAppEvents();

            if (mProvisionCompleteContentObserverRegistered) {
                mContext.getContentResolver()
                        .unregisterContentObserver(mProvisionCompleteContentObserver);
                mProvisionCompleteContentObserverRegistered = false;
            }

            if (mUserUnlockReceiverRegistered) {
                mContext.unregisterReceiver(mUserUnlockReceiver);
                mUserUnlockReceiverRegistered = false;
            }

            InstallCarrierAppUtils.hideAllNotifications(mContext);
            InstallCarrierAppUtils.unregisterPackageInstallReceiver(mContext);

            mCi.unregisterForOffOrNotAvailable(mHandler);
            mContext.unregisterReceiver(mCarrierConfigChangedReceiver);

            if (mCatService != null) mCatService.dispose();
            for (UiccCardApplication app : mUiccApplications) {
                if (app != null) {
                    app.dispose();
                }
            }
            mCatService = null;
            mUiccApplications = null;
            mRadioTech = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
            mCarrierPrivilegeRules = null;
            mContext.getContentResolver().unregisterContentObserver(
                    mProvisionCompleteContentObserver);
            mDisposed = true;
        }
    }

    /**
     * The card application that the external world sees will be based on the
     * voice radio technology only!
     */
    public void setVoiceRadioTech(int radioTech) {
        synchronized (mLock) {
            if (DBG) {
                log("Setting radio tech " + ServiceState.rilRadioTechnologyToString(radioTech));
            }
            mRadioTech = radioTech;
            setCurrentAppType(ServiceState.isGsm(radioTech));
            updateIccAvailability(false);
        }
    }

    private void setCurrentAppType(boolean isGsm) {
        if (VDBG) log("setCurrentAppType");
        int primaryAppType;
        int secondaryAppType;
        if (isGsm) {
            primaryAppType = UiccController.APP_FAM_3GPP;
            secondaryAppType = UiccController.APP_FAM_3GPP2;
        } else {
            primaryAppType = UiccController.APP_FAM_3GPP2;
            secondaryAppType = UiccController.APP_FAM_3GPP;
        }
        synchronized (mLock) {
            UiccCardApplication newApp = getApplication(primaryAppType);
            if (newApp != null || getApplication(secondaryAppType) == null) {
                mCurrentAppType = primaryAppType;
            } else {
                mCurrentAppType = secondaryAppType;
            }
        }
    }

    /**
     * Override the carrier name with either carrier config or SPN
     * if an override is provided.
     */
    private void handleCarrierNameOverride() {
        SubscriptionController subCon = SubscriptionController.getInstance();
        final int subId = subCon.getSubIdUsingPhoneId(mPhoneId);
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            loge("subId not valid for Phone " + mPhoneId);
            return;
        }

        CarrierConfigManager configLoader = (CarrierConfigManager)
                mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configLoader == null) {
            loge("Failed to load a Carrier Config");
            return;
        }

        PersistableBundle config = configLoader.getConfigForSubId(subId);
        boolean preferCcName = config.getBoolean(
                CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, false);
        String ccName = config.getString(CarrierConfigManager.KEY_CARRIER_NAME_STRING);

        String newCarrierName = null;
        String currSpn = getServiceProviderName();  // Get the name from EF_SPN.
        int nameSource = SubscriptionManager.NAME_SOURCE_SIM_SPN;
        // If carrier config is priority, use it regardless - the preference
        // and the name were both set by the carrier, so this is safe;
        // otherwise, if the SPN is priority but we don't have one *and* we have
        // a name in carrier config, use the carrier config name as a backup.
        if (preferCcName || (TextUtils.isEmpty(currSpn) && !TextUtils.isEmpty(ccName))) {
            newCarrierName = ccName;
            nameSource = SubscriptionManager.NAME_SOURCE_CARRIER;
        } else if (TextUtils.isEmpty(currSpn)) {
            // currSpn is empty and could not get name from carrier config; get name from PNN or
            // carrier id
            Phone phone = PhoneFactory.getPhone(mPhoneId);
            if (phone != null) {
                String currPnn = phone.getPlmn();   // Get the name from EF_PNN.
                if (!TextUtils.isEmpty(currPnn)) {
                    newCarrierName = currPnn;
                    nameSource = SubscriptionManager.NAME_SOURCE_SIM_PNN;
                } else {
                    newCarrierName = phone.getCarrierName();    // Get the name from carrier id.
                    nameSource = SubscriptionManager.NAME_SOURCE_CARRIER_ID;
                }
            }
        }

        if (!TextUtils.isEmpty(newCarrierName)) {
            mTelephonyManager.setSimOperatorNameForPhone(mPhoneId, newCarrierName);
            mOperatorBrandOverrideRegistrants.notifyRegistrants();
        }

        updateCarrierNameForSubscription(subCon, subId, nameSource);
    }

    /**
     * Override sim country iso based on carrier config.
     * Telephony country iso is based on MCC table which is coarse and doesn't work with dual IMSI
     * SIM. e.g, a US carrier might have a roaming agreement with carriers from Europe. Devices
     * will switch to different IMSI (differnt mccmnc) when enter roaming state. As a result, sim
     * country iso (locale) will change to non-US.
     *
     * Each sim carrier should have a single country code. We should improve the accuracy of
     * SIM country code look-up by using carrierid-to-countrycode table as an override on top of
     * MCC table
     */
    private void handleSimCountryIsoOverride() {
        SubscriptionController subCon = SubscriptionController.getInstance();
        final int subId = subCon.getSubIdUsingPhoneId(mPhoneId);
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            loge("subId not valid for Phone " + mPhoneId);
            return;
        }

        CarrierConfigManager configLoader = (CarrierConfigManager)
                mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configLoader == null) {
            loge("Failed to load a Carrier Config");
            return;
        }

        PersistableBundle config = configLoader.getConfigForSubId(subId);
        String iso = config.getString(CarrierConfigManager.KEY_SIM_COUNTRY_ISO_OVERRIDE_STRING);
        if (!TextUtils.isEmpty(iso) &&
                !iso.equals(mTelephonyManager.getSimCountryIsoForPhone(mPhoneId))) {
            mTelephonyManager.setSimCountryIsoForPhone(mPhoneId, iso);
            subCon.setCountryIso(iso, subId);
        }
    }

    private void updateCarrierNameForSubscription(SubscriptionController subCon, int subId,
            int nameSource) {
        /* update display name with carrier override */
        SubscriptionInfo subInfo = subCon.getActiveSubscriptionInfo(
                subId, mContext.getOpPackageName(), mContext.getAttributionTag());

        if (subInfo == null) {
            return;
        }

        CharSequence oldSubName = subInfo.getDisplayName();
        String newCarrierName = mTelephonyManager.getSimOperatorName(subId);

        if (!TextUtils.isEmpty(newCarrierName) && !newCarrierName.equals(oldSubName)) {
            log("sim name[" + mPhoneId + "] = " + newCarrierName);
            subCon.setDisplayNameUsingSrc(newCarrierName, subId, nameSource);
        }
    }

    /**
     * ICC availability/state changed. Update corresponding fields and external state if needed.
     */
    private void updateIccAvailability(boolean allAppsChanged) {
        synchronized (mLock) {
            UiccCardApplication newApp;
            IccRecords newRecords = null;
            newApp = getApplication(mCurrentAppType);
            if (newApp != null) {
                newRecords = newApp.getIccRecords();
            }

            if (allAppsChanged) {
                unregisterAllAppEvents();
                registerAllAppEvents();
            }

            if (mIccRecords != newRecords || mUiccApplication != newApp) {
                if (DBG) log("Icc changed. Reregistering.");
                unregisterCurrAppEvents();
                mUiccApplication = newApp;
                mIccRecords = newRecords;
                registerCurrAppEvents();
            }
            updateExternalState();
        }
    }

    void resetProperties() {
        if (mCurrentAppType == UiccController.APP_FAM_3GPP) {
            log("update icc_operator_numeric=" + "");
            mTelephonyManager.setSimOperatorNumericForPhone(mPhoneId, "");
            mTelephonyManager.setSimCountryIsoForPhone(mPhoneId, "");
            mTelephonyManager.setSimOperatorNameForPhone(mPhoneId, "");
        }
    }

    /**
     * Update the external SIM state
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void updateExternalState() {
        // First check if card state is IO_ERROR or RESTRICTED
        if (mUiccCard.getCardState() == IccCardStatus.CardState.CARDSTATE_ERROR) {
            setExternalState(IccCardConstants.State.CARD_IO_ERROR);
            return;
        }

        if (mUiccCard.getCardState() == IccCardStatus.CardState.CARDSTATE_RESTRICTED) {
            setExternalState(IccCardConstants.State.CARD_RESTRICTED);
            return;
        }

        if (mUiccCard instanceof EuiccCard && ((EuiccCard) mUiccCard).getEid() == null) {
            // for RadioConfig<1.2 the EID is not known when the EuiccCard is constructed
            if (DBG) log("EID is not ready yet.");
            return;
        }

        // By process of elimination, the UICC Card State = PRESENT and state needs to be decided
        // based on apps
        if (mUiccApplication == null) {
            loge("updateExternalState: setting state to NOT_READY because mUiccApplication is "
                    + "null");
            setExternalState(IccCardConstants.State.NOT_READY);
            return;
        }

        // Check if SIM is locked
        boolean cardLocked = false;
        IccCardConstants.State lockedState = null;
        IccCardApplicationStatus.AppState appState = mUiccApplication.getState();

        PinState pin1State = mUiccApplication.getPin1State();
        if (pin1State == PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
            if (VDBG) log("updateExternalState: PERM_DISABLED");
            cardLocked = true;
            lockedState = IccCardConstants.State.PERM_DISABLED;
        } else {
            if (appState == IccCardApplicationStatus.AppState.APPSTATE_PIN) {
                if (VDBG) log("updateExternalState: PIN_REQUIRED");
                cardLocked = true;
                lockedState = IccCardConstants.State.PIN_REQUIRED;
            } else if (appState == IccCardApplicationStatus.AppState.APPSTATE_PUK) {
                if (VDBG) log("updateExternalState: PUK_REQUIRED");
                cardLocked = true;
                lockedState = IccCardConstants.State.PUK_REQUIRED;
            } else if (appState == IccCardApplicationStatus.AppState.APPSTATE_SUBSCRIPTION_PERSO) {
                if (PersoSubState.isPersoLocked(mUiccApplication.getPersoSubState())) {
                    if (VDBG) log("updateExternalState: PERSOSUBSTATE_SIM_NETWORK");
                    cardLocked = true;
                    lockedState = IccCardConstants.State.NETWORK_LOCKED;
                }
            }
        }

        // If SIM is locked, broadcast state as NOT_READY/LOCKED depending on if records are loaded
        if (cardLocked) {
            if (mIccRecords != null && (mIccRecords.getLockedRecordsLoaded()
                    || mIccRecords.getNetworkLockedRecordsLoaded())) { // locked records loaded
                if (VDBG) {
                    log("updateExternalState: card locked and records loaded; "
                            + "setting state to locked");
                }
                // If the PIN code is required and an available cached PIN is available, intercept
                // the update of external state and perform an internal PIN verification.
                if (lockedState == IccCardConstants.State.PIN_REQUIRED) {
                    String pin = mPinStorage.getPin(mPhoneId, mIccRecords.getFullIccId());
                    if (!pin.isEmpty()) {
                        log("PIN_REQUIRED[" + mPhoneId + "] - Cache present");
                        mCi.supplyIccPin(pin, mHandler.obtainMessage(EVENT_SUPPLY_ICC_PIN_DONE));
                        return;
                    }
                }

                setExternalState(lockedState);
            } else {
                if (VDBG) {
                    log("updateExternalState: card locked but records not loaded; "
                            + "setting state to NOT_READY");
                }
                setExternalState(IccCardConstants.State.NOT_READY);
            }
            return;
        }

        // Check for remaining app states
        switch (appState) {
            case APPSTATE_UNKNOWN:
                /*
                 * APPSTATE_UNKNOWN is a catch-all state reported whenever the app
                 * is not explicitly in one of the other states. To differentiate the
                 * case where we know that there is a card present, but the APP is not
                 * ready, we choose NOT_READY here instead of unknown. This is possible
                 * in at least two cases:
                 * 1) A transient during the process of the SIM bringup
                 * 2) There is no valid App on the SIM to load, which can be the case with an
                 *    eSIM/soft SIM.
                 */
                if (VDBG) {
                    log("updateExternalState: app state is unknown; setting state to NOT_READY");
                }
                setExternalState(IccCardConstants.State.NOT_READY);
                break;
            case APPSTATE_DETECTED:
                if (VDBG) {
                    log("updateExternalState: app state is detected; setting state to NOT_READY");
                }
                setExternalState(IccCardConstants.State.NOT_READY);
                break;
            case APPSTATE_READY:
                checkAndUpdateIfAnyAppToBeIgnored();
                if (areAllApplicationsReady()) {
                    if (areAllRecordsLoaded() && areCarrierPrivilegeRulesLoaded()) {
                        if (VDBG) log("updateExternalState: setting state to LOADED");
                        setExternalState(IccCardConstants.State.LOADED);
                    } else {
                        if (VDBG) {
                            log("updateExternalState: setting state to READY; records loaded "
                                    + areAllRecordsLoaded() + ", carrier privilige rules loaded "
                                    + areCarrierPrivilegeRulesLoaded());
                        }
                        setExternalState(IccCardConstants.State.READY);
                    }
                } else {
                    if (VDBG) {
                        log("updateExternalState: app state is READY but not for all apps; "
                                + "setting state to NOT_READY");
                    }
                    setExternalState(IccCardConstants.State.NOT_READY);
                }
                break;
        }
    }

    private void registerAllAppEvents() {
        // todo: all of these should be notified to UiccProfile directly without needing to register
        for (UiccCardApplication app : mUiccApplications) {
            if (app != null) {
                if (VDBG) log("registerUiccCardEvents: registering for EVENT_APP_READY");
                app.registerForReady(mHandler, EVENT_APP_READY, null);
                IccRecords ir = app.getIccRecords();
                if (ir != null) {
                    if (VDBG) log("registerUiccCardEvents: registering for EVENT_RECORDS_LOADED");
                    ir.registerForRecordsLoaded(mHandler, EVENT_RECORDS_LOADED, null);
                    ir.registerForRecordsEvents(mHandler, EVENT_ICC_RECORD_EVENTS, null);
                }
            }
        }
    }

    private void unregisterAllAppEvents() {
        for (UiccCardApplication app : mUiccApplications) {
            if (app != null) {
                app.unregisterForReady(mHandler);
                IccRecords ir = app.getIccRecords();
                if (ir != null) {
                    ir.unregisterForRecordsLoaded(mHandler);
                    ir.unregisterForRecordsEvents(mHandler);
                }
            }
        }
    }

    private void registerCurrAppEvents() {
        // In case of locked, only listen to the current application.
        if (mIccRecords != null) {
            mIccRecords.registerForLockedRecordsLoaded(mHandler, EVENT_ICC_LOCKED, null);
            mIccRecords.registerForNetworkLockedRecordsLoaded(mHandler, EVENT_NETWORK_LOCKED, null);
        }
    }

    private void unregisterCurrAppEvents() {
        if (mIccRecords != null) {
            mIccRecords.unregisterForLockedRecordsLoaded(mHandler);
            mIccRecords.unregisterForNetworkLockedRecordsLoaded(mHandler);
        }
    }

    private void setExternalState(IccCardConstants.State newState, boolean override) {
        synchronized (mLock) {
            if (!SubscriptionManager.isValidSlotIndex(mPhoneId)) {
                loge("setExternalState: mPhoneId=" + mPhoneId + " is invalid; Return!!");
                return;
            }

            if (!override && newState == mExternalState) {
                log("setExternalState: !override and newstate unchanged from " + newState);
                return;
            }
            mExternalState = newState;
            if (mExternalState == IccCardConstants.State.LOADED) {
                // Update the MCC/MNC.
                if (mIccRecords != null) {
                    String operator = mIccRecords.getOperatorNumeric();
                    log("setExternalState: operator=" + operator + " mPhoneId=" + mPhoneId);

                    if (!TextUtils.isEmpty(operator)) {
                        mTelephonyManager.setSimOperatorNumericForPhone(mPhoneId, operator);
                        String countryCode = operator.substring(0, 3);
                        if (countryCode != null) {
                            mTelephonyManager.setSimCountryIsoForPhone(mPhoneId,
                                    MccTable.countryCodeForMcc(countryCode));
                        } else {
                            loge("setExternalState: state LOADED; Country code is null");
                        }
                    } else {
                        loge("setExternalState: state LOADED; Operator name is null");
                    }
                }
            }
            log("setExternalState: set mPhoneId=" + mPhoneId + " mExternalState=" + mExternalState);

            UiccController.updateInternalIccState(mContext, mExternalState,
                    getIccStateReason(mExternalState), mPhoneId);
        }
    }

    private void setExternalState(IccCardConstants.State newState) {
        setExternalState(newState, false);
    }

    /**
     * Function to check if all ICC records have been loaded
     * @return true if all ICC records have been loaded, false otherwise.
     */
    public boolean getIccRecordsLoaded() {
        synchronized (mLock) {
            if (mIccRecords != null) {
                return mIccRecords.getRecordsLoaded();
            }
            return false;
        }
    }

    /**
     * Locked state have a reason (PIN, PUK, NETWORK, PERM_DISABLED, CARD_IO_ERROR)
     * @return reason
     */
    private String getIccStateReason(IccCardConstants.State state) {
        switch (state) {
            case PIN_REQUIRED: return IccCardConstants.INTENT_VALUE_LOCKED_ON_PIN;
            case PUK_REQUIRED: return IccCardConstants.INTENT_VALUE_LOCKED_ON_PUK;
            case NETWORK_LOCKED: return IccCardConstants.INTENT_VALUE_LOCKED_NETWORK;
            case PERM_DISABLED: return IccCardConstants.INTENT_VALUE_ABSENT_ON_PERM_DISABLED;
            case CARD_IO_ERROR: return IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR;
            case CARD_RESTRICTED: return IccCardConstants.INTENT_VALUE_ICC_CARD_RESTRICTED;
            default: return null;
        }
    }

    /* IccCard interface implementation */
    @Override
    public IccCardConstants.State getState() {
        synchronized (mLock) {
            return mExternalState;
        }
    }

    @Override
    public IccRecords getIccRecords() {
        synchronized (mLock) {
            return mIccRecords;
        }
    }

    /**
     * Notifies handler of any transition into State.NETWORK_LOCKED
     */
    @Override
    public void registerForNetworkLocked(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant(h, what, obj);

            mNetworkLockedRegistrants.add(r);

            if (getState() == IccCardConstants.State.NETWORK_LOCKED) {
                if (mUiccApplication != null) {
                    r.notifyRegistrant(
                            new AsyncResult(null, mUiccApplication.getPersoSubState().ordinal(),
                                    null));

                } else {
                    log("registerForNetworkLocked: not notifying registrants, "
                            + "mUiccApplication == null");
                }
            }
        }
    }

    @Override
    public void unregisterForNetworkLocked(Handler h) {
        synchronized (mLock) {
            mNetworkLockedRegistrants.remove(h);
        }
    }

    @Override
    public void supplyPin(String pin, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.supplyPin(pin, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public void supplyPuk(String puk, String newPin, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.supplyPuk(puk, newPin, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public void supplyPin2(String pin2, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.supplyPin2(pin2, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public void supplyPuk2(String puk2, String newPin2, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.supplyPuk2(puk2, newPin2, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public void supplyNetworkDepersonalization(String pin, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.supplyNetworkDepersonalization(pin, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("CommandsInterface is not set.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public void supplySimDepersonalization(PersoSubState persoType, String pin, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.supplySimDepersonalization(persoType, pin, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("CommandsInterface is not set.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public boolean getIccLockEnabled() {
        synchronized (mLock) {
            /* defaults to false, if ICC is absent/deactivated */
            return mUiccApplication != null && mUiccApplication.getIccLockEnabled();
        }
    }

    @Override
    public boolean getIccFdnEnabled() {
        synchronized (mLock) {
            return mUiccApplication != null && mUiccApplication.getIccFdnEnabled();
        }
    }

    @Override
    public boolean getIccFdnAvailable() {
        synchronized (mLock) {
            return mUiccApplication != null && mUiccApplication.getIccFdnAvailable();
        }
    }

    @Override
    public boolean getIccPin2Blocked() {
        /* defaults to disabled */
        return mUiccApplication != null && mUiccApplication.getIccPin2Blocked();
    }

    @Override
    public boolean getIccPuk2Blocked() {
        /* defaults to disabled */
        return mUiccApplication != null && mUiccApplication.getIccPuk2Blocked();
    }

    @Override
    public boolean isEmptyProfile() {
        // If there's no UiccCardApplication, it's an empty profile.
        // Empty profile is a valid case of eSIM (default boot profile).
        // But we clear all apps of mUiccCardApplication to be null during refresh (see
        // resetAppWithAid) but not mLastReportedNumOfUiccApplications.
        // So if mLastReportedNumOfUiccApplications == 0, it means modem confirmed that we landed
        // on empty profile.
        return mLastReportedNumOfUiccApplications == 0;
    }

    @Override
    public void setIccLockEnabled(boolean enabled, String password, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.setIccLockEnabled(enabled, password, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public void setIccFdnEnabled(boolean enabled, String password, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.setIccFdnEnabled(enabled, password, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public void changeIccLockPassword(String oldPassword, String newPassword, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.changeIccLockPassword(oldPassword, newPassword, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public void changeIccFdnPassword(String oldPassword, String newPassword, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.changeIccFdnPassword(oldPassword, newPassword, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public String getServiceProviderName() {
        synchronized (mLock) {
            if (mIccRecords != null) {
                return mIccRecords.getServiceProviderName();
            }
            return null;
        }
    }

    @Override
    public boolean hasIccCard() {
        // mUiccCard is initialized in constructor, so won't be null
        if (mUiccCard.getCardState()
                != IccCardStatus.CardState.CARDSTATE_ABSENT) {
            return true;
        }
        loge("hasIccCard: UiccProfile is not null but UiccCard is null or card state is "
                + "ABSENT");
        return false;
    }

    /**
     * Update the UiccProfile.
     */
    public void update(Context c, CommandsInterface ci, IccCardStatus ics) {
        synchronized (mLock) {
            mUniversalPinState = ics.mUniversalPinState;
            mGsmUmtsSubscriptionAppIndex = ics.mGsmUmtsSubscriptionAppIndex;
            mCdmaSubscriptionAppIndex = ics.mCdmaSubscriptionAppIndex;
            mImsSubscriptionAppIndex = ics.mImsSubscriptionAppIndex;
            mContext = c;
            mCi = ci;
            mTelephonyManager = (TelephonyManager) mContext.getSystemService(
                    Context.TELEPHONY_SERVICE);

            //update applications
            if (DBG) log(ics.mApplications.length + " applications");
            mLastReportedNumOfUiccApplications = ics.mApplications.length;

            for (int i = 0; i < mUiccApplications.length; i++) {
                if (mUiccApplications[i] == null) {
                    //Create newly added Applications
                    if (i < ics.mApplications.length) {
                        mUiccApplications[i] = new UiccCardApplication(this,
                                ics.mApplications[i], mContext, mCi);
                    }
                } else if (i >= ics.mApplications.length) {
                    //Delete removed applications
                    mUiccApplications[i].dispose();
                    mUiccApplications[i] = null;
                } else {
                    //Update the rest
                    mUiccApplications[i].update(ics.mApplications[i], mContext, mCi);
                }
            }

            createAndUpdateCatServiceLocked();

            // Reload the carrier privilege rules if necessary.
            log("Before privilege rules: " + mCarrierPrivilegeRules + " : " + ics.mCardState);
            if (mCarrierPrivilegeRules == null && ics.mCardState == CardState.CARDSTATE_PRESENT) {
                mCarrierPrivilegeRules = new UiccCarrierPrivilegeRules(this,
                        mHandler.obtainMessage(EVENT_CARRIER_PRIVILEGES_LOADED));
            } else if (mCarrierPrivilegeRules != null
                    && ics.mCardState != CardState.CARDSTATE_PRESENT) {
                mCarrierPrivilegeRules = null;
                mContext.getContentResolver().unregisterContentObserver(
                        mProvisionCompleteContentObserver);
            }

            sanitizeApplicationIndexesLocked();
            if (mRadioTech != ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN) {
                setCurrentAppType(ServiceState.isGsm(mRadioTech));
            }
            updateIccAvailability(true);
        }
    }

    private void createAndUpdateCatServiceLocked() {
        if (mUiccApplications.length > 0 && mUiccApplications[0] != null) {
            // Initialize or Reinitialize CatService
            if (mCatService == null) {
                mCatService = CatService.getInstance(mCi, mContext, this, mPhoneId);
            } else {
                mCatService.update(mCi, mContext, this);
            }
        } else {
            if (mCatService != null) {
                mCatService.dispose();
            }
            mCatService = null;
        }
    }

    @Override
    protected void finalize() {
        if (DBG) log("UiccProfile finalized");
    }

    /**
     * This function makes sure that application indexes are valid
     * and resets invalid indexes. (This should never happen, but in case
     * RIL misbehaves we need to manage situation gracefully)
     */
    private void sanitizeApplicationIndexesLocked() {
        mGsmUmtsSubscriptionAppIndex =
                checkIndexLocked(
                        mGsmUmtsSubscriptionAppIndex, AppType.APPTYPE_SIM, AppType.APPTYPE_USIM);
        mCdmaSubscriptionAppIndex =
                checkIndexLocked(
                        mCdmaSubscriptionAppIndex, AppType.APPTYPE_RUIM, AppType.APPTYPE_CSIM);
        mImsSubscriptionAppIndex =
                checkIndexLocked(mImsSubscriptionAppIndex, AppType.APPTYPE_ISIM, null);
    }

    /**
     * Checks if the app is supported for the purposes of checking if all apps are ready/loaded, so
     * this only checks for SIM/USIM and CSIM/RUIM apps. ISIM is considered not supported for this
     * purpose as there are cards that have ISIM app that is never read (there are SIMs for which
     * the state of ISIM goes to DETECTED but never to READY).
     * CSIM/RUIM apps are considered not supported if CDMA is not supported.
     */
    private boolean isSupportedApplication(UiccCardApplication app) {
        // TODO: 2/15/18 Add check to see if ISIM app will go to READY state, and if yes, check for
        // ISIM also (currently ISIM is considered as not supported in this function)
        if (app.getType() == AppType.APPTYPE_USIM || app.getType() == AppType.APPTYPE_SIM
                || (UiccController.isCdmaSupported(mContext)
                && (app.getType() == AppType.APPTYPE_CSIM
                || app.getType() == AppType.APPTYPE_RUIM))) {
            return true;
        }
        return false;
    }

    private void checkAndUpdateIfAnyAppToBeIgnored() {
        boolean[] appReadyStateTracker = new boolean[AppType.APPTYPE_ISIM.ordinal() + 1];
        for (UiccCardApplication app : mUiccApplications) {
            if (app != null && isSupportedApplication(app) && app.isReady()) {
                appReadyStateTracker[app.getType().ordinal()] = true;
            }
        }

        for (UiccCardApplication app : mUiccApplications) {
            if (app != null && isSupportedApplication(app) && !app.isReady()) {
                /* Checks if the  appReadyStateTracker has already an entry in ready state
                   with same type as app */
                if (appReadyStateTracker[app.getType().ordinal()]) {
                    app.setAppIgnoreState(true);
                }
            }
        }
    }

    private boolean areAllApplicationsReady() {
        for (UiccCardApplication app : mUiccApplications) {
            if (app != null && isSupportedApplication(app) && !app.isReady()
                    && !app.isAppIgnored()) {
                if (VDBG) log("areAllApplicationsReady: return false");
                return false;
            }
        }

        if (VDBG) {
            log("areAllApplicationsReady: outside loop, return " + (mUiccApplication != null));
        }
        return mUiccApplication != null;
    }

    private boolean areAllRecordsLoaded() {
        for (UiccCardApplication app : mUiccApplications) {
            if (app != null && isSupportedApplication(app) && !app.isAppIgnored()) {
                IccRecords ir = app.getIccRecords();
                if (ir == null || !ir.isLoaded()) {
                    if (VDBG) log("areAllRecordsLoaded: return false");
                    return false;
                }
            }
        }
        if (VDBG) {
            log("areAllRecordsLoaded: outside loop, return " + (mUiccApplication != null));
        }
        return mUiccApplication != null;
    }

    private int checkIndexLocked(int index, AppType expectedAppType, AppType altExpectedAppType) {
        if (mUiccApplications == null || index >= mUiccApplications.length) {
            loge("App index " + index + " is invalid since there are no applications");
            return -1;
        }

        if (index < 0) {
            // This is normal. (i.e. no application of this type)
            return -1;
        }

        if (mUiccApplications[index].getType() != expectedAppType
                && mUiccApplications[index].getType() != altExpectedAppType) {
            loge("App index " + index + " is invalid since it's not "
                    + expectedAppType + " and not " + altExpectedAppType);
            return -1;
        }

        // Seems to be valid
        return index;
    }

    /**
     * Registers the handler when operator brand name is overridden.
     *
     * @param h Handler for notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForOpertorBrandOverride(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant(h, what, obj);
            mOperatorBrandOverrideRegistrants.add(r);
        }
    }

    /**
     * Unregister for notifications when operator brand name is overriden.
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForOperatorBrandOverride(Handler h) {
        synchronized (mLock) {
            mOperatorBrandOverrideRegistrants.remove(h);
        }
    }

    static boolean isPackageBundled(Context context, String pkgName) {
        PackageManager pm = context.getPackageManager();
        try {
            // We also match hidden-until-installed apps. The assumption here is that some other
            // mechanism (like CarrierAppUtils) would automatically enable such an app, so we
            // shouldn't prompt the user about it.
            pm.getApplicationInfo(pkgName, PackageManager.MATCH_HIDDEN_UNTIL_INSTALLED_COMPONENTS);
            if (DBG) log(pkgName + " is installed.");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            if (DBG) log(pkgName + " is not installed.");
            return false;
        }
    }

    private void promptInstallCarrierApp(String pkgName) {
        Intent showDialogIntent = InstallCarrierAppTrampolineActivity.get(mContext, pkgName);
        showDialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(showDialogIntent);
    }

    private void onCarrierPrivilegesLoadedMessage() {
        // TODO(b/211796398): clean up logic below once all carrier privilege check migration done
        // Update set of enabled carrier apps now that the privilege rules may have changed.
        ActivityManager am = mContext.getSystemService(ActivityManager.class);
        CarrierAppUtils.disableCarrierAppsUntilPrivileged(mContext.getOpPackageName(),
                mTelephonyManager, am.getCurrentUser(), mContext);

        UsageStatsManager usm = (UsageStatsManager) mContext.getSystemService(
                Context.USAGE_STATS_SERVICE);
        if (usm != null) {
            usm.onCarrierPrivilegedAppsChanged();
        }

        InstallCarrierAppUtils.hideAllNotifications(mContext);
        InstallCarrierAppUtils.unregisterPackageInstallReceiver(mContext);

        synchronized (mLock) {
            boolean isProvisioned = isProvisioned();
            boolean isUnlocked = isUserUnlocked();
            // Only show dialog if the phone is through with Setup Wizard and is unlocked.
            // Otherwise, wait for completion and unlock and show a notification instead.
            if (isProvisioned && isUnlocked) {
                for (String pkgName : getUninstalledCarrierPackages()) {
                    promptInstallCarrierApp(pkgName);
                }
            } else {
                if (!isProvisioned) {
                    final Uri uri = Settings.Global.getUriFor(Settings.Global.DEVICE_PROVISIONED);
                    mContext.getContentResolver().registerContentObserver(
                            uri,
                            false,
                            mProvisionCompleteContentObserver);
                    mProvisionCompleteContentObserverRegistered = true;
                }
                if (!isUnlocked) {
                    mContext.registerReceiver(
                            mUserUnlockReceiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
                    mUserUnlockReceiverRegistered = true;
                }
            }
        }
    }

    private boolean isProvisioned() {
        return Settings.Global.getInt(
                mContext.getContentResolver(),
                Settings.Global.DEVICE_PROVISIONED, 1) == 1;
    }

    private boolean isUserUnlocked() {
        return mContext.getSystemService(UserManager.class).isUserUnlocked();
    }

    private void showCarrierAppNotificationsIfPossible() {
        if (isProvisioned() && isUserUnlocked()) {
            for (String pkgName : getUninstalledCarrierPackages()) {
                InstallCarrierAppUtils.showNotification(mContext, pkgName);
                InstallCarrierAppUtils.registerPackageInstallReceiver(mContext);
            }
        }
    }

    private Set<String> getUninstalledCarrierPackages() {
        String allowListSetting = Settings.Global.getString(
                mContext.getContentResolver(),
                Settings.Global.CARRIER_APP_WHITELIST);
        if (TextUtils.isEmpty(allowListSetting)) {
            return Collections.emptySet();
        }
        Map<String, String> certPackageMap = parseToCertificateToPackageMap(allowListSetting);
        if (certPackageMap.isEmpty()) {
            return Collections.emptySet();
        }
        UiccCarrierPrivilegeRules rules = getCarrierPrivilegeRules();
        if (rules == null) {
            return Collections.emptySet();
        }
        Set<String> uninstalledCarrierPackages = new ArraySet<>();
        List<UiccAccessRule> accessRules = rules.getAccessRules();
        for (UiccAccessRule accessRule : accessRules) {
            String certHexString = accessRule.getCertificateHexString().toUpperCase();
            String pkgName = certPackageMap.get(certHexString);
            if (!TextUtils.isEmpty(pkgName) && !isPackageBundled(mContext, pkgName)) {
                uninstalledCarrierPackages.add(pkgName);
            }
        }
        return uninstalledCarrierPackages;
    }

    /**
     * Converts a string in the format: key1:value1;key2:value2... into a map where the keys are
     * hex representations of app certificates - all upper case - and the values are package names
     * @hide
     */
    @VisibleForTesting
    public static Map<String, String> parseToCertificateToPackageMap(String allowListSetting) {
        final String pairDelim = "\\s*;\\s*";
        final String keyValueDelim = "\\s*:\\s*";

        List<String> keyValuePairList = Arrays.asList(allowListSetting.split(pairDelim));

        if (keyValuePairList.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new ArrayMap<>(keyValuePairList.size());
        for (String keyValueString: keyValuePairList) {
            String[] keyValue = keyValueString.split(keyValueDelim);

            if (keyValue.length == 2) {
                map.put(keyValue[0].toUpperCase(), keyValue[1]);
            } else {
                loge("Incorrect length of key-value pair in carrier app allow list map.  "
                        + "Length should be exactly 2");
            }
        }

        return map;
    }

    /**
     * Check whether the specified type of application exists in the profile.
     *
     * @param type UICC application type.
     */
    public boolean isApplicationOnIcc(IccCardApplicationStatus.AppType type) {
        synchronized (mLock) {
            for (int i = 0; i < mUiccApplications.length; i++) {
                if (mUiccApplications[i] != null && mUiccApplications[i].getType() == type) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Return the universal pin state of the profile.
     */
    public PinState getUniversalPinState() {
        synchronized (mLock) {
            return mUniversalPinState;
        }
    }

    /**
     * Return the application of the specified family.
     *
     * @param family UICC application family.
     * @return application corresponding to family or a null if no match found
     */
    public UiccCardApplication getApplication(int family) {
        synchronized (mLock) {
            int index = IccCardStatus.CARD_MAX_APPS;
            switch (family) {
                case UiccController.APP_FAM_3GPP:
                    index = mGsmUmtsSubscriptionAppIndex;
                    break;
                case UiccController.APP_FAM_3GPP2:
                    index = mCdmaSubscriptionAppIndex;
                    break;
                case UiccController.APP_FAM_IMS:
                    index = mImsSubscriptionAppIndex;
                    break;
            }
            if (index >= 0 && index < mUiccApplications.length) {
                return mUiccApplications[index];
            }
            return null;
        }
    }

    /**
     * Return the application with the index of the array.
     *
     * @param index Index of the application array.
     * @return application corresponding to index or a null if no match found
     */
    public UiccCardApplication getApplicationIndex(int index) {
        synchronized (mLock) {
            if (index >= 0 && index < mUiccApplications.length) {
                return mUiccApplications[index];
            }
            return null;
        }
    }

    /**
     * Returns the SIM application of the specified type.
     *
     * @param type ICC application type
     * (@see com.android.internal.telephony.PhoneConstants#APPTYPE_xxx)
     * @return application corresponding to type or a null if no match found
     */
    public UiccCardApplication getApplicationByType(int type) {
        synchronized (mLock) {
            for (int i = 0; i < mUiccApplications.length; i++) {
                if (mUiccApplications[i] != null
                        && mUiccApplications[i].getType().ordinal() == type) {
                    return mUiccApplications[i];
                }
            }
            return null;
        }
    }

    /**
     * Resets the application with the input AID.
     *
     * A null aid implies a card level reset - all applications must be reset.
     *
     * @param aid aid of the application which should be reset; null imples all applications
     * @param reset true if reset is required. false for initialization.
     * @return boolean indicating if there was any change made as part of the reset which
     * requires carrier config to be reset too (for e.g. if only ISIM app is refreshed carrier
     * config should not be reset)
     */
    @VisibleForTesting
    public boolean resetAppWithAid(String aid, boolean reset) {
        synchronized (mLock) {
            boolean changed = false;
            boolean isIsimRefresh = false;
            for (int i = 0; i < mUiccApplications.length; i++) {
                if (mUiccApplications[i] != null
                        && (TextUtils.isEmpty(aid) || aid.equals(mUiccApplications[i].getAid()))) {
                    // Resetting only ISIM does not need to be treated as a change from caller
                    // perspective, as it does not affect SIM state now or even later when ISIM
                    // is re-loaded, hence return false.
                    if (!TextUtils.isEmpty(aid)
                            && mUiccApplications[i].getType() == AppType.APPTYPE_ISIM) {
                        isIsimRefresh = true;
                    }

                    // Delete removed applications
                    mUiccApplications[i].dispose();
                    mUiccApplications[i] = null;
                    changed = true;
                }
            }
            if (reset && TextUtils.isEmpty(aid)) {
                if (mCarrierPrivilegeRules != null) {
                    mCarrierPrivilegeRules = null;
                    mContext.getContentResolver().unregisterContentObserver(
                            mProvisionCompleteContentObserver);
                    changed = true;
                }
                // CatService shall be disposed only when a card level reset happens.
                if (mCatService != null) {
                    mCatService.dispose();
                    mCatService = null;
                    changed = true;
                }
            }
            return changed && !isIsimRefresh;
        }
    }

    /**
     * Exposes {@link CommandsInterface#iccOpenLogicalChannel}
     */
    public void iccOpenLogicalChannel(String aid, int p2, Message response) {
        logWithLocalLog("iccOpenLogicalChannel: " + aid + " , " + p2 + " by pid:"
                + Binder.getCallingPid() + " uid:" + Binder.getCallingUid());
        mCi.iccOpenLogicalChannel(aid, p2,
                mHandler.obtainMessage(EVENT_OPEN_LOGICAL_CHANNEL_DONE, response));
    }

    /**
     * Exposes {@link CommandsInterface#iccCloseLogicalChannel}
     */
    public void iccCloseLogicalChannel(int channel, Message response) {
        logWithLocalLog("iccCloseLogicalChannel: " + channel);
        mCi.iccCloseLogicalChannel(channel,
                mHandler.obtainMessage(EVENT_CLOSE_LOGICAL_CHANNEL_DONE, response));
    }

    /**
     * Exposes {@link CommandsInterface#iccTransmitApduLogicalChannel}
     */
    public void iccTransmitApduLogicalChannel(int channel, int cla, int command,
            int p1, int p2, int p3, String data, Message response) {
        mCi.iccTransmitApduLogicalChannel(channel, cla, command, p1, p2, p3,
                data, mHandler.obtainMessage(EVENT_TRANSMIT_APDU_LOGICAL_CHANNEL_DONE, response));
    }

    /**
     * Exposes {@link CommandsInterface#iccTransmitApduBasicChannel}
     */
    public void iccTransmitApduBasicChannel(int cla, int command,
            int p1, int p2, int p3, String data, Message response) {
        mCi.iccTransmitApduBasicChannel(cla, command, p1, p2, p3,
                data, mHandler.obtainMessage(EVENT_TRANSMIT_APDU_BASIC_CHANNEL_DONE, response));
    }

    /**
     * Exposes {@link CommandsInterface#iccIO}
     */
    public void iccExchangeSimIO(int fileID, int command, int p1, int p2, int p3,
            String pathID, Message response) {
        mCi.iccIO(command, fileID, pathID, p1, p2, p3, null, null,
                mHandler.obtainMessage(EVENT_SIM_IO_DONE, response));
    }

    /**
     * Exposes {@link CommandsInterface#sendEnvelopeWithStatus}
     */
    public void sendEnvelopeWithStatus(String contents, Message response) {
        mCi.sendEnvelopeWithStatus(contents, response);
    }

    /**
     * Returns number of applications on this card
     */
    public int getNumApplications() {
        return mLastReportedNumOfUiccApplications;
    }

    /**
     * Returns the id of the phone which is associated with this profile.
     */
    public int getPhoneId() {
        return mPhoneId;
    }

    /**
     * Returns true iff carrier privileges rules are null (dont need to be loaded) or loaded.
     */
    @VisibleForTesting
    public boolean areCarrierPrivilegeRulesLoaded() {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        return carrierPrivilegeRules == null
                || carrierPrivilegeRules.areCarrierPriviligeRulesLoaded();
    }

    /**
     * Return a list of certs in hex string from loaded carrier privileges access rules.
     *
     * @return a list of certificate in hex string. return {@code null} if there is no certs
     * or privilege rules are not loaded yet.
     */
    public List<String> getCertsFromCarrierPrivilegeAccessRules() {
        final List<String> certs = new ArrayList<>();
        final UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules != null) {
            List<UiccAccessRule> accessRules = carrierPrivilegeRules.getAccessRules();
            for (UiccAccessRule accessRule : accessRules) {
                certs.add(accessRule.getCertificateHexString());
            }
        }
        return certs.isEmpty() ? null : certs;
    }

    /** @return a list of {@link UiccAccessRule}s, or an empty list if none have been loaded yet. */
    public List<UiccAccessRule> getCarrierPrivilegeAccessRules() {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return Collections.EMPTY_LIST;
        }
        return new ArrayList<>(carrierPrivilegeRules.getAccessRules());
    }

    /** Returns a reference to the current {@link UiccCarrierPrivilegeRules}. */
    private UiccCarrierPrivilegeRules getCarrierPrivilegeRules() {
        synchronized (mLock) {
            if (mTestOverrideCarrierPrivilegeRules != null) {
                return mTestOverrideCarrierPrivilegeRules;
            }
            return mCarrierPrivilegeRules;
        }
    }

    /**
     * Sets the overridden operator brand.
     */
    public boolean setOperatorBrandOverride(String brand) {
        log("setOperatorBrandOverride: " + brand);
        log("current iccId: " + SubscriptionInfo.givePrintableIccid(getIccId()));

        String iccId = getIccId();
        if (TextUtils.isEmpty(iccId)) {
            return false;
        }
        if (!SubscriptionController.getInstance().checkPhoneIdAndIccIdMatch(getPhoneId(), iccId)) {
            loge("iccId doesn't match current active subId.");
            return false;
        }

        SharedPreferences.Editor spEditor =
                PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        String key = OPERATOR_BRAND_OVERRIDE_PREFIX + iccId;
        if (brand == null) {
            spEditor.remove(key).commit();
        } else {
            spEditor.putString(key, brand).commit();
        }
        mOperatorBrandOverrideRegistrants.notifyRegistrants();
        return true;
    }

    /**
     * Returns the overridden operator brand.
     */
    public String getOperatorBrandOverride() {
        String iccId = getIccId();
        if (TextUtils.isEmpty(iccId)) {
            return null;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sp.getString(OPERATOR_BRAND_OVERRIDE_PREFIX + iccId, null);
    }

    /**
     * Returns the iccid of the profile.
     */
    public String getIccId() {
        // ICCID should be same across all the apps.
        for (UiccCardApplication app : mUiccApplications) {
            if (app != null) {
                IccRecords ir = app.getIccRecords();
                if (ir != null && ir.getIccId() != null) {
                    return ir.getIccId();
                }
            }
        }
        return null;
    }

    private static String eventToString(int event) {
        switch (event) {
            case EVENT_RADIO_OFF_OR_UNAVAILABLE: return "RADIO_OFF_OR_UNAVAILABLE";
            case EVENT_ICC_LOCKED: return "ICC_LOCKED";
            case EVENT_APP_READY: return "APP_READY";
            case EVENT_RECORDS_LOADED: return "RECORDS_LOADED";
            case EVENT_NETWORK_LOCKED: return "NETWORK_LOCKED";
            case EVENT_EID_READY: return "EID_READY";
            case EVENT_ICC_RECORD_EVENTS: return "ICC_RECORD_EVENTS";
            case EVENT_OPEN_LOGICAL_CHANNEL_DONE: return "OPEN_LOGICAL_CHANNEL_DONE";
            case EVENT_CLOSE_LOGICAL_CHANNEL_DONE: return "CLOSE_LOGICAL_CHANNEL_DONE";
            case EVENT_TRANSMIT_APDU_LOGICAL_CHANNEL_DONE:
                return "TRANSMIT_APDU_LOGICAL_CHANNEL_DONE";
            case EVENT_TRANSMIT_APDU_BASIC_CHANNEL_DONE: return "TRANSMIT_APDU_BASIC_CHANNEL_DONE";
            case EVENT_SIM_IO_DONE: return "SIM_IO_DONE";
            case EVENT_CARRIER_PRIVILEGES_LOADED: return "CARRIER_PRIVILEGES_LOADED";
            case EVENT_CARRIER_CONFIG_CHANGED: return "CARRIER_CONFIG_CHANGED";
            case EVENT_CARRIER_PRIVILEGES_TEST_OVERRIDE_SET:
                return "CARRIER_PRIVILEGES_TEST_OVERRIDE_SET";
            case EVENT_SUPPLY_ICC_PIN_DONE: return "SUPPLY_ICC_PIN_DONE";
            default: return "UNKNOWN(" + event + ")";
        }
    }

    private static void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private static void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    private void logWithLocalLog(String msg) {
        Rlog.d(LOG_TAG, msg);
        if (DBG) UiccController.addLocalLog("UiccProfile[" + mPhoneId + "]: " + msg);
    }

    /**
     * Reloads carrier privileges as if a change were just detected.  Useful to force a profile
     * refresh without having to physically insert or remove a SIM card.
     */
    @VisibleForTesting
    public void refresh() {
        mHandler.sendMessage(mHandler.obtainMessage(EVENT_CARRIER_PRIVILEGES_LOADED));
    }

    /**
     * Set a test set of carrier privilege rules which will override the actual rules on the SIM.
     *
     * <p>May be null, in which case the rules on the SIM will be used and any previous overrides
     * will be cleared.
     *
     * @see TelephonyManager#setCarrierTestOverride
     */
    public void setTestOverrideCarrierPrivilegeRules(@Nullable List<UiccAccessRule> rules) {
        mHandler.sendMessage(
                mHandler.obtainMessage(EVENT_CARRIER_PRIVILEGES_TEST_OVERRIDE_SET, rules));
    }

    /**
     * Dump
     */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("UiccProfile:");
        pw.println(" mCi=" + mCi);
        pw.println(" mCatService=" + mCatService);
        for (int i = 0; i < mOperatorBrandOverrideRegistrants.size(); i++) {
            pw.println("  mOperatorBrandOverrideRegistrants[" + i + "]="
                    + ((Registrant) mOperatorBrandOverrideRegistrants.get(i)).getHandler());
        }
        pw.println(" mUniversalPinState=" + mUniversalPinState);
        pw.println(" mGsmUmtsSubscriptionAppIndex=" + mGsmUmtsSubscriptionAppIndex);
        pw.println(" mCdmaSubscriptionAppIndex=" + mCdmaSubscriptionAppIndex);
        pw.println(" mImsSubscriptionAppIndex=" + mImsSubscriptionAppIndex);
        pw.println(" mUiccApplications: length=" + mUiccApplications.length);
        for (int i = 0; i < mUiccApplications.length; i++) {
            if (mUiccApplications[i] == null) {
                pw.println("  mUiccApplications[" + i + "]=" + null);
            } else {
                pw.println("  mUiccApplications[" + i + "]="
                        + mUiccApplications[i].getType() + " " + mUiccApplications[i]);
            }
        }
        pw.println();
        // Print details of all applications
        for (UiccCardApplication app : mUiccApplications) {
            if (app != null) {
                app.dump(fd, pw, args);
                pw.println();
            }
        }
        // Print details of all IccRecords
        for (UiccCardApplication app : mUiccApplications) {
            if (app != null) {
                IccRecords ir = app.getIccRecords();
                if (ir != null) {
                    ir.dump(fd, pw, args);
                    pw.println();
                }
            }
        }
        // Print UiccCarrierPrivilegeRules and registrants.
        if (mCarrierPrivilegeRules == null) {
            pw.println(" mCarrierPrivilegeRules: null");
        } else {
            pw.println(" mCarrierPrivilegeRules: " + mCarrierPrivilegeRules);
            mCarrierPrivilegeRules.dump(fd, pw, args);
        }
        if (mTestOverrideCarrierPrivilegeRules != null) {
            pw.println(" mTestOverrideCarrierPrivilegeRules: "
                    + mTestOverrideCarrierPrivilegeRules);
            mTestOverrideCarrierPrivilegeRules.dump(fd, pw, args);
        }
        pw.flush();

        pw.println(" mNetworkLockedRegistrants: size=" + mNetworkLockedRegistrants.size());
        for (int i = 0; i < mNetworkLockedRegistrants.size(); i++) {
            pw.println("  mNetworkLockedRegistrants[" + i + "]="
                    + ((Registrant) mNetworkLockedRegistrants.get(i)).getHandler());
        }
        pw.println(" mCurrentAppType=" + mCurrentAppType);
        pw.println(" mUiccCard=" + mUiccCard);
        pw.println(" mUiccApplication=" + mUiccApplication);
        pw.println(" mIccRecords=" + mIccRecords);
        pw.println(" mExternalState=" + mExternalState);
        pw.flush();
    }
}
