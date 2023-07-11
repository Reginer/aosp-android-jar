/*
* Copyright (C) 2014 The Android Open Source Project
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

import android.Manifest;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.service.carrier.CarrierIdentifier;
import android.service.euicc.EuiccProfileInfo;
import android.service.euicc.EuiccService;
import android.service.euicc.GetEuiccProfileInfoListResult;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.UsageSetting;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.SimState;
import android.telephony.UiccAccessRule;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.euicc.EuiccController;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccPort;
import com.android.internal.telephony.uicc.UiccSlot;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *@hide
 */
public class SubscriptionInfoUpdater extends Handler {
    private static final String LOG_TAG = "SubscriptionInfoUpdater";
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private static final int SUPPORTED_MODEM_COUNT = TelephonyManager.getDefault()
            .getSupportedModemCount();

    private static final boolean DBG = true;

    private static final int EVENT_INVALID = -1;
    private static final int EVENT_GET_NETWORK_SELECTION_MODE_DONE = 2;
    private static final int EVENT_SIM_LOADED = 3;
    private static final int EVENT_SIM_ABSENT = 4;
    private static final int EVENT_SIM_LOCKED = 5;
    private static final int EVENT_SIM_IO_ERROR = 6;
    private static final int EVENT_SIM_UNKNOWN = 7;
    private static final int EVENT_SIM_RESTRICTED = 8;
    private static final int EVENT_SIM_NOT_READY = 9;
    private static final int EVENT_SIM_READY = 10;
    private static final int EVENT_SIM_IMSI = 11;
    private static final int EVENT_REFRESH_EMBEDDED_SUBSCRIPTIONS = 12;
    private static final int EVENT_MULTI_SIM_CONFIG_CHANGED = 13;
    private static final int EVENT_INACTIVE_SLOT_ICC_STATE_CHANGED = 14;

    private static final String ICCID_STRING_FOR_NO_SIM = "";

    private static final ParcelUuid REMOVE_GROUP_UUID =
            ParcelUuid.fromString(CarrierConfigManager.REMOVE_GROUP_UUID_STRING);

    // Key used to read/write the current IMSI. Updated on SIM_STATE_CHANGED - LOADED.
    public static final String CURR_SUBID = "curr_subid";

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private static Context sContext = null;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)

    protected static String[] sIccId = new String[SUPPORTED_MODEM_COUNT];
    protected SubscriptionController mSubscriptionController = null;
    private static String[] sInactiveIccIds = new String[SUPPORTED_MODEM_COUNT];
    private static int[] sSimCardState = new int[SUPPORTED_MODEM_COUNT];
    private static int[] sSimApplicationState = new int[SUPPORTED_MODEM_COUNT];
    private static boolean sIsSubInfoInitialized = false;
    private SubscriptionManager mSubscriptionManager = null;
    private EuiccManager mEuiccManager;
    private Handler mBackgroundHandler;

    // The current foreground user ID.
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mCurrentlyActiveUserId;
    private CarrierServiceBindHelper mCarrierServiceBindHelper;

    private volatile boolean shouldRetryUpdateEmbeddedSubscriptions = false;
    private final CopyOnWriteArraySet<Integer> retryUpdateEmbeddedSubscriptionCards =
        new CopyOnWriteArraySet<>();
    private final BroadcastReceiver mUserUnlockedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_UNLOCKED.equals(intent.getAction())) {
                // The LPA may not have been ready before user unlock, and so previous attempts
                // to refresh the list of embedded subscriptions may have failed. This retries
                // the refresh operation after user unlock.
                if (shouldRetryUpdateEmbeddedSubscriptions) {
                    logd("Retrying refresh embedded subscriptions after user unlock.");
                    for (int cardId : retryUpdateEmbeddedSubscriptionCards){
                        requestEmbeddedSubscriptionInfoListRefresh(cardId, null);
                    }
                    retryUpdateEmbeddedSubscriptionCards.clear();
                    sContext.unregisterReceiver(mUserUnlockedReceiver);
                }
            }
        }
    };

    /**
     * Runnable with a boolean parameter. This is used in
     * updateEmbeddedSubscriptions(List<Integer> cardIds, @Nullable UpdateEmbeddedSubsCallback).
     */
    protected interface UpdateEmbeddedSubsCallback {
        /**
         * Callback of the Runnable.
         * @param hasChanges Whether there is any subscription info change. If yes, we need to
         * notify the listeners.
         */
        void run(boolean hasChanges);
    }

    @VisibleForTesting
    public SubscriptionInfoUpdater(Looper looper, Context context, SubscriptionController sc) {
        logd("Constructor invoked");
        mBackgroundHandler = new Handler(looper);

        sContext = context;
        mSubscriptionController = sc;
        mSubscriptionManager = SubscriptionManager.from(sContext);
        mEuiccManager = (EuiccManager) sContext.getSystemService(Context.EUICC_SERVICE);

        mCarrierServiceBindHelper = new CarrierServiceBindHelper(sContext);

        sContext.registerReceiver(
                mUserUnlockedReceiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED));

        initializeCarrierApps();

        PhoneConfigurationManager.registerForMultiSimConfigChange(
                this, EVENT_MULTI_SIM_CONFIG_CHANGED, null);
    }

    private void initializeCarrierApps() {
        // Initialize carrier apps:
        // -Now (on system startup)
        // -Whenever new carrier privilege rules might change (new SIM is loaded)
        // -Whenever we switch to a new user
        mCurrentlyActiveUserId = 0;
        sContext.registerReceiverForAllUsers(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Remove this line after testing
                if (Intent.ACTION_USER_FOREGROUND.equals(intent.getAction())) {
                    UserHandle userHandle = intent.getParcelableExtra(Intent.EXTRA_USER);
                    // If couldn't get current user ID, guess it's 0.
                    mCurrentlyActiveUserId = userHandle != null ? userHandle.getIdentifier() : 0;
                    CarrierAppUtils.disableCarrierAppsUntilPrivileged(sContext.getOpPackageName(),
                            TelephonyManager.getDefault(), mCurrentlyActiveUserId, sContext);
                }
            }
        }, new IntentFilter(Intent.ACTION_USER_FOREGROUND), null, null);
        ActivityManager am = (ActivityManager) sContext.getSystemService(Context.ACTIVITY_SERVICE);
        mCurrentlyActiveUserId = am.getCurrentUser();
        CarrierAppUtils.disableCarrierAppsUntilPrivileged(sContext.getOpPackageName(),
                TelephonyManager.getDefault(), mCurrentlyActiveUserId, sContext);
    }

    /**
     * Update subscriptions when given a new ICC state.
     */
    public void updateInternalIccState(String simStatus, String reason, int phoneId) {
        logd("updateInternalIccState to simStatus " + simStatus + " reason " + reason
                + " phoneId " + phoneId);
        int message = internalIccStateToMessage(simStatus);
        if (message != EVENT_INVALID) {
            sendMessage(obtainMessage(message, phoneId, 0, reason));
        }
    }

    /**
     * Update subscriptions if needed when there's a change in inactive port.
     * @param prevActivePhoneId is the corresponding phoneId of the port if port was previously
     *                          active. It could be INVALID if it was already inactive.
     * @param iccId iccId in that port, if any.
     */
    public void updateInternalIccStateForInactivePort(int prevActivePhoneId, String iccId) {
        sendMessage(obtainMessage(EVENT_INACTIVE_SLOT_ICC_STATE_CHANGED, prevActivePhoneId,
                0, iccId));
    }

    private int internalIccStateToMessage(String simStatus) {
        switch(simStatus) {
            case IccCardConstants.INTENT_VALUE_ICC_ABSENT: return EVENT_SIM_ABSENT;
            case IccCardConstants.INTENT_VALUE_ICC_UNKNOWN: return EVENT_SIM_UNKNOWN;
            case IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR: return EVENT_SIM_IO_ERROR;
            case IccCardConstants.INTENT_VALUE_ICC_CARD_RESTRICTED: return EVENT_SIM_RESTRICTED;
            case IccCardConstants.INTENT_VALUE_ICC_NOT_READY: return EVENT_SIM_NOT_READY;
            case IccCardConstants.INTENT_VALUE_ICC_LOCKED: return EVENT_SIM_LOCKED;
            case IccCardConstants.INTENT_VALUE_ICC_LOADED: return EVENT_SIM_LOADED;
            case IccCardConstants.INTENT_VALUE_ICC_READY: return EVENT_SIM_READY;
            case IccCardConstants.INTENT_VALUE_ICC_IMSI: return EVENT_SIM_IMSI;
            default:
                logd("Ignoring simStatus: " + simStatus);
                return EVENT_INVALID;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected boolean isAllIccIdQueryDone() {
        for (int i = 0; i < TelephonyManager.getDefault().getActiveModemCount(); i++) {
            UiccSlot slot = UiccController.getInstance().getUiccSlotForPhone(i);
            int slotId = UiccController.getInstance().getSlotIdFromPhoneId(i);
            // When psim card is absent there is no port object even the port state is active.
            // We should check the slot state for psim and port state for esim(MEP eUICC).
            if  (sIccId[i] == null || slot == null || !slot.isActive()
                    || (slot.isEuicc() && UiccController.getInstance().getUiccPort(i) == null)) {
                if (sIccId[i] == null) {
                    logd("Wait for SIM " + i + " Iccid");
                } else {
                    logd(String.format("Wait for port corresponding to phone %d to be active, "
                        + "slotId is %d" + " , portIndex is %d", i, slotId,
                            slot.getPortIndexFromPhoneId(i)));
                }
                return false;
            }
        }
        logd("All IccIds query complete");

        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        List<Integer> cardIds = new ArrayList<>();
        switch (msg.what) {
            case EVENT_GET_NETWORK_SELECTION_MODE_DONE: {
                AsyncResult ar = (AsyncResult)msg.obj;
                Integer slotId = (Integer)ar.userObj;
                if (ar.exception == null && ar.result != null) {
                    int[] modes = (int[])ar.result;
                    if (modes[0] == 1) {  // Manual mode.
                        PhoneFactory.getPhone(slotId).setNetworkSelectionModeAutomatic(null);
                    }
                } else {
                    logd("EVENT_GET_NETWORK_SELECTION_MODE_DONE: error getting network mode.");
                }
                break;
            }

            case EVENT_SIM_LOADED:
                handleSimLoaded(msg.arg1);
                break;

            case EVENT_SIM_ABSENT:
                handleSimAbsent(msg.arg1);
                break;

            case EVENT_INACTIVE_SLOT_ICC_STATE_CHANGED:
                handleInactivePortIccStateChange(msg.arg1, (String) msg.obj);
                break;

            case EVENT_SIM_LOCKED:
                handleSimLocked(msg.arg1, (String) msg.obj);
                break;

            case EVENT_SIM_UNKNOWN:
                broadcastSimStateChanged(msg.arg1, IccCardConstants.INTENT_VALUE_ICC_UNKNOWN, null);
                broadcastSimCardStateChanged(msg.arg1, TelephonyManager.SIM_STATE_UNKNOWN);
                broadcastSimApplicationStateChanged(msg.arg1, TelephonyManager.SIM_STATE_UNKNOWN);
                updateSubscriptionCarrierId(msg.arg1, IccCardConstants.INTENT_VALUE_ICC_UNKNOWN);
                updateCarrierServices(msg.arg1, IccCardConstants.INTENT_VALUE_ICC_UNKNOWN);
                break;

            case EVENT_SIM_IO_ERROR:
                handleSimError(msg.arg1);
                break;

            case EVENT_SIM_RESTRICTED:
                broadcastSimStateChanged(msg.arg1,
                        IccCardConstants.INTENT_VALUE_ICC_CARD_RESTRICTED,
                        IccCardConstants.INTENT_VALUE_ICC_CARD_RESTRICTED);
                broadcastSimCardStateChanged(msg.arg1, TelephonyManager.SIM_STATE_CARD_RESTRICTED);
                broadcastSimApplicationStateChanged(msg.arg1, TelephonyManager.SIM_STATE_NOT_READY);
                updateSubscriptionCarrierId(msg.arg1,
                        IccCardConstants.INTENT_VALUE_ICC_CARD_RESTRICTED);
                updateCarrierServices(msg.arg1, IccCardConstants.INTENT_VALUE_ICC_CARD_RESTRICTED);
                break;

            case EVENT_SIM_READY:
                handleSimReady(msg.arg1);
                break;

            case EVENT_SIM_IMSI:
                broadcastSimStateChanged(msg.arg1, IccCardConstants.INTENT_VALUE_ICC_IMSI, null);
                break;

            case EVENT_SIM_NOT_READY:
                // an eUICC with no active subscriptions never becomes ready, so we need to trigger
                // the embedded subscriptions update here
                cardIds.add(getCardIdFromPhoneId(msg.arg1));
                updateEmbeddedSubscriptions(cardIds, (hasChanges) -> {
                    if (hasChanges) {
                        mSubscriptionController.notifySubscriptionInfoChanged();
                    }
                });
                handleSimNotReady(msg.arg1);
                break;

            case EVENT_REFRESH_EMBEDDED_SUBSCRIPTIONS:
                cardIds.add(msg.arg1);
                Runnable r = (Runnable) msg.obj;
                updateEmbeddedSubscriptions(cardIds, (hasChanges) -> {
                    if (hasChanges) {
                        mSubscriptionController.notifySubscriptionInfoChanged();
                    }
                    if (r != null) {
                        r.run();
                    }
                });
                break;

            case EVENT_MULTI_SIM_CONFIG_CHANGED:
                onMultiSimConfigChanged();
                break;

            default:
                logd("Unknown msg:" + msg.what);
        }
    }

    private void onMultiSimConfigChanged() {
        int activeModemCount = ((TelephonyManager) sContext.getSystemService(
                Context.TELEPHONY_SERVICE)).getActiveModemCount();
        // For inactive modems, reset its states.
        for (int phoneId = activeModemCount; phoneId < SUPPORTED_MODEM_COUNT; phoneId++) {
            sIccId[phoneId] = null;
            sSimCardState[phoneId] = TelephonyManager.SIM_STATE_UNKNOWN;
            sSimApplicationState[phoneId] = TelephonyManager.SIM_STATE_UNKNOWN;
        }
    }

    protected int getCardIdFromPhoneId(int phoneId) {
        UiccController uiccController = UiccController.getInstance();
        UiccCard card = uiccController.getUiccCardForPhone(phoneId);
        if (card != null) {
            return uiccController.convertToPublicCardId(card.getCardId());
        }
        return TelephonyManager.UNINITIALIZED_CARD_ID;
    }

    void requestEmbeddedSubscriptionInfoListRefresh(int cardId, @Nullable Runnable callback) {
        sendMessage(obtainMessage(
                EVENT_REFRESH_EMBEDDED_SUBSCRIPTIONS, cardId, 0 /* arg2 */, callback));
    }

    protected void handleSimLocked(int phoneId, String reason) {
        if (sIccId[phoneId] != null && sIccId[phoneId].equals(ICCID_STRING_FOR_NO_SIM)) {
            logd("SIM" + (phoneId + 1) + " hot plug in");
            sIccId[phoneId] = null;
        }

        IccCard iccCard = PhoneFactory.getPhone(phoneId).getIccCard();
        if (iccCard == null) {
            logd("handleSimLocked: IccCard null");
            return;
        }
        IccRecords records = iccCard.getIccRecords();
        if (records == null) {
            logd("handleSimLocked: IccRecords null");
            return;
        }
        if (IccUtils.stripTrailingFs(records.getFullIccId()) == null) {
            logd("handleSimLocked: IccID null");
            return;
        }
        sIccId[phoneId] = IccUtils.stripTrailingFs(records.getFullIccId());

        updateSubscriptionInfoByIccId(phoneId, true /* updateEmbeddedSubs */);

        broadcastSimStateChanged(phoneId, IccCardConstants.INTENT_VALUE_ICC_LOCKED, reason);
        broadcastSimCardStateChanged(phoneId, TelephonyManager.SIM_STATE_PRESENT);
        broadcastSimApplicationStateChanged(phoneId, getSimStateFromLockedReason(reason));
        updateSubscriptionCarrierId(phoneId, IccCardConstants.INTENT_VALUE_ICC_LOCKED);
        updateCarrierServices(phoneId, IccCardConstants.INTENT_VALUE_ICC_LOCKED);
    }

    private static int getSimStateFromLockedReason(String lockedReason) {
        switch (lockedReason) {
            case IccCardConstants.INTENT_VALUE_LOCKED_ON_PIN:
                return TelephonyManager.SIM_STATE_PIN_REQUIRED;
            case IccCardConstants.INTENT_VALUE_LOCKED_ON_PUK:
                return TelephonyManager.SIM_STATE_PUK_REQUIRED;
            case IccCardConstants.INTENT_VALUE_LOCKED_NETWORK:
                return TelephonyManager.SIM_STATE_NETWORK_LOCKED;
            case IccCardConstants.INTENT_VALUE_ABSENT_ON_PERM_DISABLED:
                return TelephonyManager.SIM_STATE_PERM_DISABLED;
            default:
                Rlog.e(LOG_TAG, "Unexpected SIM locked reason " + lockedReason);
                return TelephonyManager.SIM_STATE_UNKNOWN;
        }
    }

    protected void handleSimReady(int phoneId) {
        List<Integer> cardIds = new ArrayList<>();
        logd("handleSimReady: phoneId: " + phoneId);

        if (sIccId[phoneId] != null && sIccId[phoneId].equals(ICCID_STRING_FOR_NO_SIM)) {
            logd(" SIM" + (phoneId + 1) + " hot plug in");
            sIccId[phoneId] = null;
        }

        // ICCID is not available in IccRecords by the time SIM Ready event received
        // hence get ICCID from UiccPort.
        UiccPort port = UiccController.getInstance().getUiccPort(phoneId);
        String iccId = (port == null) ? null : IccUtils.stripTrailingFs(port.getIccId());

        if (!TextUtils.isEmpty(iccId)) {
            sIccId[phoneId] = iccId;
            updateSubscriptionInfoByIccId(phoneId, true /* updateEmbeddedSubs */);
        }

        cardIds.add(getCardIdFromPhoneId(phoneId));
        updateEmbeddedSubscriptions(cardIds, (hasChanges) -> {
            if (hasChanges) {
                mSubscriptionController.notifySubscriptionInfoChanged();
            }
        });
        broadcastSimStateChanged(phoneId, IccCardConstants.INTENT_VALUE_ICC_READY, null);
        broadcastSimCardStateChanged(phoneId, TelephonyManager.SIM_STATE_PRESENT);
        broadcastSimApplicationStateChanged(phoneId, TelephonyManager.SIM_STATE_NOT_READY);
    }

    protected void handleSimNotReady(int phoneId) {
        logd("handleSimNotReady: phoneId: " + phoneId);
        boolean isFinalState = false;

        IccCard iccCard = PhoneFactory.getPhone(phoneId).getIccCard();
        boolean uiccAppsDisabled = areUiccAppsDisabledOnCard(phoneId);
        if (iccCard.isEmptyProfile() || uiccAppsDisabled) {
            if (uiccAppsDisabled) {
                UiccPort port = UiccController.getInstance().getUiccPort(phoneId);
                String iccId = (port == null) ? null : port.getIccId();
                sInactiveIccIds[phoneId] = IccUtils.stripTrailingFs(iccId);
            }
            isFinalState = true;
            // ICC_NOT_READY is a terminal state for
            // 1) It's an empty profile as there's no uicc applications. Or
            // 2) Its uicc applications are set to be disabled.
            // At this phase, the subscription list is accessible. Treating NOT_READY
            // as equivalent to ABSENT, once the rest of the system can handle it.
            sIccId[phoneId] = ICCID_STRING_FOR_NO_SIM;
            updateSubscriptionInfoByIccId(phoneId, false /* updateEmbeddedSubs */);
        } else {
            sIccId[phoneId] = null;
        }

        broadcastSimStateChanged(phoneId, IccCardConstants.INTENT_VALUE_ICC_NOT_READY,
                null);
        broadcastSimCardStateChanged(phoneId, TelephonyManager.SIM_STATE_PRESENT);
        broadcastSimApplicationStateChanged(phoneId, TelephonyManager.SIM_STATE_NOT_READY);
        if (isFinalState) {
            updateCarrierServices(phoneId, IccCardConstants.INTENT_VALUE_ICC_NOT_READY);
        }
    }

    private boolean areUiccAppsDisabledOnCard(int phoneId) {
        // When uicc apps are disabled(supported in IRadio 1.5), we will still get IccId from
        // cardStatus (since IRadio 1.2). Amd upon cardStatus change we'll receive another
        // handleSimNotReady so this will be evaluated again.
        UiccSlot slot = UiccController.getInstance().getUiccSlotForPhone(phoneId);
        if (slot == null) return false;
        UiccPort port = UiccController.getInstance().getUiccPort(phoneId);
        String iccId = (port == null) ? null : port.getIccId();
        if (iccId == null) {
            return false;
        }
        SubscriptionInfo info =
                mSubscriptionController.getSubInfoForIccId(
                        IccUtils.stripTrailingFs(iccId));
        return info != null && !info.areUiccApplicationsEnabled();
    }

    protected void handleSimLoaded(int phoneId) {
        logd("handleSimLoaded: phoneId: " + phoneId);

        // The SIM should be loaded at this state, but it is possible in cases such as SIM being
        // removed or a refresh RESET that the IccRecords could be null. The right behavior is to
        // not broadcast the SIM loaded.
        IccCard iccCard = PhoneFactory.getPhone(phoneId).getIccCard();
        if (iccCard == null) {  // Possibly a race condition.
            logd("handleSimLoaded: IccCard null");
            return;
        }
        IccRecords records = iccCard.getIccRecords();
        if (records == null) {  // Possibly a race condition.
            logd("handleSimLoaded: IccRecords null");
            return;
        }
        if (IccUtils.stripTrailingFs(records.getFullIccId()) == null) {
            logd("handleSimLoaded: IccID null");
            return;
        }

        // Call updateSubscriptionInfoByIccId() only if was not done earlier from SIM READY event
        if (sIccId[phoneId] == null) {
            sIccId[phoneId] = IccUtils.stripTrailingFs(records.getFullIccId());

            updateSubscriptionInfoByIccId(phoneId, true /* updateEmbeddedSubs */);
        }

        List<SubscriptionInfo> subscriptionInfos =
                mSubscriptionController.getSubInfoUsingSlotIndexPrivileged(phoneId);
        if (subscriptionInfos == null || subscriptionInfos.isEmpty()) {
            loge("empty subinfo for phoneId: " + phoneId + "could not update ContentResolver");
        } else {
            for (SubscriptionInfo sub : subscriptionInfos) {
                int subId = sub.getSubscriptionId();
                TelephonyManager tm = (TelephonyManager)
                        sContext.getSystemService(Context.TELEPHONY_SERVICE);
                String operator = tm.getSimOperatorNumeric(subId);

                if (!TextUtils.isEmpty(operator)) {
                    if (subId == mSubscriptionController.getDefaultSubId()) {
                        MccTable.updateMccMncConfiguration(sContext, operator);
                    }
                    mSubscriptionController.setMccMnc(operator, subId);
                } else {
                    logd("EVENT_RECORDS_LOADED Operator name is null");
                }

                String iso = tm.getSimCountryIsoForPhone(phoneId);

                if (!TextUtils.isEmpty(iso)) {
                    mSubscriptionController.setCountryIso(iso, subId);
                } else {
                    logd("EVENT_RECORDS_LOADED sim country iso is null");
                }

                String msisdn = tm.getLine1Number(subId);
                if (msisdn != null) {
                    mSubscriptionController.setDisplayNumber(msisdn, subId);
                }

                String imsi = tm.createForSubscriptionId(subId).getSubscriberId();
                if (imsi != null) {
                    mSubscriptionController.setImsi(imsi, subId);
                }

                String[] ehplmns = records.getEhplmns();
                String[] hplmns = records.getPlmnsFromHplmnActRecord();
                if (ehplmns != null || hplmns != null) {
                    mSubscriptionController.setAssociatedPlmns(ehplmns, hplmns, subId);
                }

                /* Update preferred network type and network selection mode on SIM change.
                 * Storing last subId in SharedPreference for now to detect SIM change.
                 */
                SharedPreferences sp =
                        PreferenceManager.getDefaultSharedPreferences(sContext);
                int storedSubId = sp.getInt(CURR_SUBID + phoneId, -1);

                if (storedSubId != subId) {
                    // Only support automatic selection mode on SIM change.
                    PhoneFactory.getPhone(phoneId).getNetworkSelectionMode(
                            obtainMessage(EVENT_GET_NETWORK_SELECTION_MODE_DONE,
                                    new Integer(phoneId)));
                    // Update stored subId
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putInt(CURR_SUBID + phoneId, subId);
                    editor.apply();
                }
            }
        }

        /**
         * The sim loading sequence will be
         *  1. ACTION_SUBINFO_CONTENT_CHANGE happens through updateSubscriptionInfoByIccId() above.
         *  2. ACTION_SIM_STATE_CHANGED/ACTION_SIM_CARD_STATE_CHANGED
         *  /ACTION_SIM_APPLICATION_STATE_CHANGED
         *  3. ACTION_SUBSCRIPTION_CARRIER_IDENTITY_CHANGED
         *  4. restore sim-specific settings
         *  5. ACTION_CARRIER_CONFIG_CHANGED
         */
        broadcastSimStateChanged(phoneId, IccCardConstants.INTENT_VALUE_ICC_LOADED, null);
        broadcastSimCardStateChanged(phoneId, TelephonyManager.SIM_STATE_PRESENT);
        broadcastSimApplicationStateChanged(phoneId, TelephonyManager.SIM_STATE_LOADED);
        updateSubscriptionCarrierId(phoneId, IccCardConstants.INTENT_VALUE_ICC_LOADED);
        /* Sim-specific settings restore depends on knowing both the mccmnc and the carrierId of the
        sim which is why it must be done after #updateSubscriptionCarrierId(). It is done before
        carrier config update to avoid any race conditions with user settings that depend on
        carrier config*/
        restoreSimSpecificSettingsForPhone(phoneId);
        updateCarrierServices(phoneId, IccCardConstants.INTENT_VALUE_ICC_LOADED);
    }

    /**
     * Calculate the usage setting based on the carrier request.
     *
     * @param currentUsageSetting the current setting in the subscription DB
     * @param preferredUsageSetting provided by the carrier config
     * @return the calculated usage setting.
     */
    @VisibleForTesting
    @UsageSetting public int calculateUsageSetting(
            @UsageSetting int currentUsageSetting, @UsageSetting int preferredUsageSetting) {
        int defaultUsageSetting;
        int[] supportedUsageSettings;

        //  Load the resources to provide the device capability
        try {
            defaultUsageSetting = sContext.getResources().getInteger(
                com.android.internal.R.integer.config_default_cellular_usage_setting);
            supportedUsageSettings = sContext.getResources().getIntArray(
                com.android.internal.R.array.config_supported_cellular_usage_settings);
            // If usage settings are not supported, return the default setting, which is UNKNOWN.
            if (supportedUsageSettings == null
                    || supportedUsageSettings.length < 1) return currentUsageSetting;
        } catch (Resources.NotFoundException nfe) {
            loge("Failed to load usage setting resources!");
            return currentUsageSetting;
        }

        // If the current setting is invalid, including the first time the value is set,
        // update it to default (this will trigger a change in the DB).
        if (currentUsageSetting < SubscriptionManager.USAGE_SETTING_DEFAULT
                || currentUsageSetting > SubscriptionManager.USAGE_SETTING_DATA_CENTRIC) {
            logd("Updating usage setting for current subscription");
            currentUsageSetting = SubscriptionManager.USAGE_SETTING_DEFAULT;
        }

        // Range check the inputs, and on failure, make no changes
        if (preferredUsageSetting < SubscriptionManager.USAGE_SETTING_DEFAULT
                || preferredUsageSetting > SubscriptionManager.USAGE_SETTING_DATA_CENTRIC) {
            loge("Invalid usage setting!" + preferredUsageSetting);
            return currentUsageSetting;
        }

        // Default is always allowed
        if (preferredUsageSetting == SubscriptionManager.USAGE_SETTING_DEFAULT) {
            return preferredUsageSetting;
        }

        // Forced setting must be explicitly supported
        for (int i = 0; i < supportedUsageSettings.length; i++) {
            if (preferredUsageSetting == supportedUsageSettings[i]) return preferredUsageSetting;
        }

        // If the preferred setting is not possible, just keep the current setting.
        return currentUsageSetting;
    }

    private void restoreSimSpecificSettingsForPhone(int phoneId) {
        SubscriptionManager subManager = SubscriptionManager.from(sContext);
        subManager.restoreSimSpecificSettingsForIccIdFromBackup(sIccId[phoneId]);
    }

    private void updateCarrierServices(int phoneId, String simState) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            logd("Ignore updateCarrierServices request with invalid phoneId " + phoneId);
            return;
        }
        CarrierConfigManager configManager =
                (CarrierConfigManager) sContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        configManager.updateConfigForPhoneId(phoneId, simState);
        mCarrierServiceBindHelper.updateForPhoneId(phoneId, simState);
    }

    private void updateSubscriptionCarrierId(int phoneId, String simState) {
        if (PhoneFactory.getPhone(phoneId) != null) {
            PhoneFactory.getPhone(phoneId).resolveSubscriptionCarrierId(simState);
        }
    }

    /**
     * PhoneId is the corresponding phoneId of the port if port was previously active.
     * It could be INVALID if it was already inactive.
     */
    private void handleInactivePortIccStateChange(int phoneId, String iccId) {
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            // If phoneId is valid, it means the physical slot was previously active in that
            // phoneId. In this case, found the subId and set its phoneId to invalid.
            if (sIccId[phoneId] != null && !sIccId[phoneId].equals(ICCID_STRING_FOR_NO_SIM)) {
                logd("Slot of SIM" + (phoneId + 1) + " becomes inactive");
            }
            cleanSubscriptionInPhone(phoneId, false);
        }
        if (!TextUtils.isEmpty(iccId)) {
            // If iccId is new, add a subscription record in the db.
            String strippedIccId = IccUtils.stripTrailingFs(iccId);
            if (mSubscriptionController.getSubInfoForIccId(strippedIccId) == null) {
                mSubscriptionController.insertEmptySubInfoRecord(
                        strippedIccId, "CARD", SubscriptionManager.INVALID_PHONE_INDEX,
                        SubscriptionManager.SUBSCRIPTION_TYPE_LOCAL_SIM);
            }
        }
    }

    /**
     * Clean subscription info when sim state becomes ABSENT. There are 2 scenarios for this:
     * 1. SIM is actually removed
     * 2. Slot becomes inactive, which results in SIM being treated as ABSENT, but SIM may not
     * have been removed.
     * @param phoneId phoneId for which the cleanup needs to be done
     * @param isSimAbsent boolean to indicate if the SIM is actually ABSENT (case 1 above)
     */
    private void cleanSubscriptionInPhone(int phoneId, boolean isSimAbsent) {
        if (sInactiveIccIds[phoneId] != null || (isSimAbsent && sIccId[phoneId] != null
                && !sIccId[phoneId].equals(ICCID_STRING_FOR_NO_SIM))) {
            // When a SIM is unplugged, mark uicc applications enabled. This is to make sure when
            // user unplugs and re-inserts the SIM card, we re-enable it.
            // In certain cases this can happen before sInactiveIccIds is updated, which is why we
            // check for sIccId as well (in case of isSimAbsent). The scenario is: after SIM
            // deactivate request is sent to RIL, SIM is removed before SIM state is updated to
            // NOT_READY. We do not need to check if this exact scenario is hit, because marking
            // uicc applications enabled when SIM is removed should be okay to do regardless.
            logd("cleanSubscriptionInPhone: " + phoneId + ", inactive iccid "
                    + sInactiveIccIds[phoneId]);
            if (sInactiveIccIds[phoneId] == null) {
                logd("cleanSubscriptionInPhone: " + phoneId + ", isSimAbsent=" + isSimAbsent
                        + ", iccid=" + sIccId[phoneId]);
            }
            String iccId = sInactiveIccIds[phoneId] != null
                    ? sInactiveIccIds[phoneId] : sIccId[phoneId];
            ContentValues value = new ContentValues();
            value.put(SubscriptionManager.UICC_APPLICATIONS_ENABLED, true);
            if (isSimAbsent) {
                // When sim is absent, set the port index to invalid port index -1;
                value.put(SubscriptionManager.PORT_INDEX, TelephonyManager.INVALID_PORT_INDEX);
            }
            sContext.getContentResolver().update(SubscriptionManager.CONTENT_URI, value,
                    SubscriptionManager.ICC_ID + "=\'" + iccId + "\'", null);
            sInactiveIccIds[phoneId] = null;
        }
        sIccId[phoneId] = ICCID_STRING_FOR_NO_SIM;
        updateSubscriptionInfoByIccId(phoneId, true /* updateEmbeddedSubs */);
    }

    protected void handleSimAbsent(int phoneId) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            logd("handleSimAbsent on invalid phoneId");
            return;
        }
        if (sIccId[phoneId] != null && !sIccId[phoneId].equals(ICCID_STRING_FOR_NO_SIM)) {
            logd("SIM" + (phoneId + 1) + " hot plug out");
        }
        cleanSubscriptionInPhone(phoneId, true);

        broadcastSimStateChanged(phoneId, IccCardConstants.INTENT_VALUE_ICC_ABSENT, null);
        broadcastSimCardStateChanged(phoneId, TelephonyManager.SIM_STATE_ABSENT);
        broadcastSimApplicationStateChanged(phoneId, TelephonyManager.SIM_STATE_UNKNOWN);
        updateSubscriptionCarrierId(phoneId, IccCardConstants.INTENT_VALUE_ICC_ABSENT);
        updateCarrierServices(phoneId, IccCardConstants.INTENT_VALUE_ICC_ABSENT);
    }

    protected void handleSimError(int phoneId) {
        if (sIccId[phoneId] != null && !sIccId[phoneId].equals(ICCID_STRING_FOR_NO_SIM)) {
            logd("SIM" + (phoneId + 1) + " Error ");
        }
        sIccId[phoneId] = ICCID_STRING_FOR_NO_SIM;
        updateSubscriptionInfoByIccId(phoneId, true /* updateEmbeddedSubs */);
        broadcastSimStateChanged(phoneId, IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR,
                IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR);
        broadcastSimCardStateChanged(phoneId, TelephonyManager.SIM_STATE_CARD_IO_ERROR);
        broadcastSimApplicationStateChanged(phoneId, TelephonyManager.SIM_STATE_NOT_READY);
        updateSubscriptionCarrierId(phoneId, IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR);
        updateCarrierServices(phoneId, IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR);
    }

    protected synchronized void updateSubscriptionInfoByIccId(int phoneId,
            boolean updateEmbeddedSubs) {
        logd("updateSubscriptionInfoByIccId:+ Start - phoneId: " + phoneId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            loge("[updateSubscriptionInfoByIccId]- invalid phoneId=" + phoneId);
            return;
        }
        logd("updateSubscriptionInfoByIccId: removing subscription info record: phoneId "
                + phoneId);
        // Clear phoneId only when sim absent is not enough. It's possible to switch SIM profile
        // within the same slot. Need to clear the slot index of the previous sub. Thus always clear
        // for the changing slot first.
        mSubscriptionController.clearSubInfoRecord(phoneId);

        // If SIM is not absent, insert new record or update existing record.
        if (!ICCID_STRING_FOR_NO_SIM.equals(sIccId[phoneId]) && sIccId[phoneId] != null) {
            logd("updateSubscriptionInfoByIccId: adding subscription info record: iccid: "
                    + sIccId[phoneId] + ", phoneId:" + phoneId);
            mSubscriptionManager.addSubscriptionInfoRecord(sIccId[phoneId], phoneId);
        }

        List<SubscriptionInfo> subInfos =
                mSubscriptionController.getSubInfoUsingSlotIndexPrivileged(phoneId);
        if (subInfos != null) {
            boolean changed = false;
            for (int i = 0; i < subInfos.size(); i++) {
                SubscriptionInfo temp = subInfos.get(i);
                ContentValues value = new ContentValues(1);

                String msisdn = TelephonyManager.getDefault().getLine1Number(
                        temp.getSubscriptionId());

                if (!TextUtils.equals(msisdn, temp.getNumber())) {
                    value.put(SubscriptionManager.NUMBER, msisdn);
                    sContext.getContentResolver().update(SubscriptionManager
                            .getUriForSubscriptionId(temp.getSubscriptionId()), value, null, null);
                    changed = true;
                }
            }
            if (changed) {
                // refresh Cached Active Subscription Info List
                mSubscriptionController.refreshCachedActiveSubscriptionInfoList();
            }
        }

        // TODO investigate if we can update for each slot separately.
        if (isAllIccIdQueryDone()) {
            // Ensure the modems are mapped correctly
            if (mSubscriptionManager.isActiveSubId(
                    mSubscriptionManager.getDefaultDataSubscriptionId())) {
                mSubscriptionManager.setDefaultDataSubId(
                        mSubscriptionManager.getDefaultDataSubscriptionId());
            } else {
                logd("bypass reset default data sub if inactive");
            }
            setSubInfoInitialized();
        }

        UiccController uiccController = UiccController.getInstance();
        UiccSlot[] uiccSlots = uiccController.getUiccSlots();
        if (uiccSlots != null && updateEmbeddedSubs) {
            List<Integer> cardIds = new ArrayList<>();
            for (UiccSlot uiccSlot : uiccSlots) {
                if (uiccSlot != null && uiccSlot.getUiccCard() != null) {
                    int cardId = uiccController.convertToPublicCardId(
                            uiccSlot.getUiccCard().getCardId());
                    cardIds.add(cardId);
                }
            }
            updateEmbeddedSubscriptions(cardIds, (hasChanges) -> {
                if (hasChanges) {
                    mSubscriptionController.notifySubscriptionInfoChanged();
                }
                if (DBG) logd("updateSubscriptionInfoByIccId: SubscriptionInfo update complete");
            });
        }

        mSubscriptionController.notifySubscriptionInfoChanged();
        if (DBG) logd("updateSubscriptionInfoByIccId: SubscriptionInfo update complete");
    }

    private void setSubInfoInitialized() {
        // Should only be triggered once.
        if (!sIsSubInfoInitialized) {
            if (DBG) logd("SubInfo Initialized");
            sIsSubInfoInitialized = true;
            mSubscriptionController.notifySubInfoReady();
        }
        MultiSimSettingController.getInstance().notifyAllSubscriptionLoaded();
    }

    /**
     * Whether subscriptions of all SIMs are initialized.
     */
    public static boolean isSubInfoInitialized() {
        return sIsSubInfoInitialized;
    }

    /**
     * Updates the cached list of embedded subscription for the eUICC with the given list of card
     * IDs {@code cardIds}. The step of reading the embedded subscription list from eUICC card is
     * executed in background thread. The callback {@code callback} is executed after the cache is
     * refreshed. The callback is executed in main thread.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void updateEmbeddedSubscriptions(List<Integer> cardIds,
            @Nullable UpdateEmbeddedSubsCallback callback) {
        // Do nothing if eUICCs are disabled. (Previous entries may remain in the cache, but they
        // are filtered out of list calls as long as EuiccManager.isEnabled returns false).
        if (!mEuiccManager.isEnabled()) {
            if (DBG) logd("updateEmbeddedSubscriptions: eUICC not enabled");
            callback.run(false /* hasChanges */);
            return;
        }

        mBackgroundHandler.post(() -> {
            List<Pair<Integer, GetEuiccProfileInfoListResult>> results = new ArrayList<>();
            for (int cardId : cardIds) {
                GetEuiccProfileInfoListResult result =
                        EuiccController.get().blockingGetEuiccProfileInfoList(cardId);
                if (DBG) logd("blockingGetEuiccProfileInfoList cardId " + cardId);
                results.add(Pair.create(cardId, result));
            }

            // The runnable will be executed in the main thread.
            this.post(() -> {
                boolean hasChanges = false;
                for (Pair<Integer, GetEuiccProfileInfoListResult> cardIdAndResult : results) {
                    if (updateEmbeddedSubscriptionsCache(cardIdAndResult.first,
                            cardIdAndResult.second)) {
                        hasChanges = true;
                    }
                }
                // The latest state in the main thread may be changed when the callback is
                // triggered.
                if (callback != null) {
                    callback.run(hasChanges);
                }
            });
        });
    }

    /**
     * Update the cached list of embedded subscription based on the passed in
     * GetEuiccProfileInfoListResult {@code result}.
     *
     * @return true if changes may have been made. This is not a guarantee that changes were made,
     * but notifications about subscription changes may be skipped if this returns false as an
     * optimization to avoid spurious notifications.
     */
    private boolean updateEmbeddedSubscriptionsCache(int cardId,
            GetEuiccProfileInfoListResult result) {
        if (DBG) logd("updateEmbeddedSubscriptionsCache");

        if (result == null) {
            if (DBG) logd("updateEmbeddedSubscriptionsCache: IPC to the eUICC controller failed");
            retryUpdateEmbeddedSubscriptionCards.add(cardId);
            shouldRetryUpdateEmbeddedSubscriptions = true;
            return false;
        }

        // If the returned result is not RESULT_OK or the profile list is null, don't update cache.
        // Otherwise, update the cache.
        final EuiccProfileInfo[] embeddedProfiles;
        List<EuiccProfileInfo> list = result.getProfiles();
        if (result.getResult() == EuiccService.RESULT_OK && list != null) {
            embeddedProfiles = list.toArray(new EuiccProfileInfo[list.size()]);
            if (DBG) {
                logd("blockingGetEuiccProfileInfoList: got " + result.getProfiles().size()
                        + " profiles");
            }
        } else {
            if (DBG) {
                logd("blockingGetEuiccProfileInfoList returns an error. "
                        + "Result code=" + result.getResult()
                        + ". Null profile list=" + (result.getProfiles() == null));
            }
            return false;
        }

        final boolean isRemovable = result.getIsRemovable();

        final String[] embeddedIccids = new String[embeddedProfiles.length];
        for (int i = 0; i < embeddedProfiles.length; i++) {
            embeddedIccids[i] = embeddedProfiles[i].getIccid();
        }

        if (DBG) logd("Get eUICC profile list of size " + embeddedProfiles.length);

        // Note that this only tracks whether we make any writes to the DB. It's possible this will
        // be set to true for an update even when the row contents remain exactly unchanged from
        // before, since we don't compare against the previous value. Since this is only intended to
        // avoid some spurious broadcasts (particularly for users who don't use eSIM at all), this
        // is fine.
        boolean hasChanges = false;

        // Update or insert records for all embedded subscriptions (except non-removable ones if the
        // current eUICC is non-removable, since we assume these are still accessible though not
        // returned by the eUICC controller).
        List<SubscriptionInfo> existingSubscriptions =
                mSubscriptionController.getSubscriptionInfoListForEmbeddedSubscriptionUpdate(
                        embeddedIccids, isRemovable);
        ContentResolver contentResolver = sContext.getContentResolver();
        for (EuiccProfileInfo embeddedProfile : embeddedProfiles) {
            int index =
                    findSubscriptionInfoForIccid(existingSubscriptions, embeddedProfile.getIccid());
            int prevCarrierId = TelephonyManager.UNKNOWN_CARRIER_ID;
            int nameSource = SubscriptionManager.NAME_SOURCE_CARRIER_ID;
            if (index < 0) {
                // No existing entry for this ICCID; create an empty one.
                mSubscriptionController.insertEmptySubInfoRecord(
                        embeddedProfile.getIccid(), SubscriptionManager.SIM_NOT_INSERTED);
            } else {
                nameSource = existingSubscriptions.get(index).getNameSource();
                prevCarrierId = existingSubscriptions.get(index).getCarrierId();
                existingSubscriptions.remove(index);
            }

            if (DBG) {
                logd("embeddedProfile " + embeddedProfile + " existing record "
                        + (index < 0 ? "not found" : "found"));
            }

            ContentValues values = new ContentValues();
            values.put(SubscriptionManager.IS_EMBEDDED, 1);
            List<UiccAccessRule> ruleList = embeddedProfile.getUiccAccessRules();
            boolean isRuleListEmpty = false;
            if (ruleList == null || ruleList.size() == 0) {
                isRuleListEmpty = true;
            }
            values.put(SubscriptionManager.ACCESS_RULES,
                    isRuleListEmpty ? null : UiccAccessRule.encodeRules(
                            ruleList.toArray(new UiccAccessRule[ruleList.size()])));
            values.put(SubscriptionManager.IS_REMOVABLE, isRemovable);
            // override DISPLAY_NAME if the priority of existing nameSource is <= carrier
            if (SubscriptionController.getNameSourcePriority(nameSource)
                    <= SubscriptionController.getNameSourcePriority(
                            SubscriptionManager.NAME_SOURCE_CARRIER)) {
                values.put(SubscriptionManager.DISPLAY_NAME, embeddedProfile.getNickname());
                values.put(SubscriptionManager.NAME_SOURCE,
                        SubscriptionManager.NAME_SOURCE_CARRIER);
            }
            values.put(SubscriptionManager.PROFILE_CLASS, embeddedProfile.getProfileClass());
            values.put(SubscriptionManager.PORT_INDEX,
                    getEmbeddedProfilePortIndex(embeddedProfile.getIccid()));
            CarrierIdentifier cid = embeddedProfile.getCarrierIdentifier();
            if (cid != null) {
                // Due to the limited subscription information, carrier id identified here might
                // not be accurate compared with CarrierResolver. Only update carrier id if there
                // is no valid carrier id present.
                if (prevCarrierId == TelephonyManager.UNKNOWN_CARRIER_ID) {
                    values.put(SubscriptionManager.CARRIER_ID,
                            CarrierResolver.getCarrierIdFromIdentifier(sContext, cid));
                }
                String mcc = cid.getMcc();
                String mnc = cid.getMnc();
                values.put(SubscriptionManager.MCC_STRING, mcc);
                values.put(SubscriptionManager.MCC, mcc);
                values.put(SubscriptionManager.MNC_STRING, mnc);
                values.put(SubscriptionManager.MNC, mnc);
            }
            // If cardId = unsupported or unitialized, we have no reason to update DB.
            // Additionally, if the device does not support cardId for default eUICC, the CARD_ID
            // field should not contain the EID
            UiccController uiccController = UiccController.getInstance();
            if (cardId >= 0 && uiccController.getCardIdForDefaultEuicc()
                    != TelephonyManager.UNSUPPORTED_CARD_ID) {
                values.put(SubscriptionManager.CARD_ID, uiccController.convertToCardString(cardId));
            }
            hasChanges = true;
            contentResolver.update(SubscriptionManager.CONTENT_URI, values,
                    SubscriptionManager.ICC_ID + "='" + embeddedProfile.getIccid() + "'", null);

            // refresh Cached Active Subscription Info List
            mSubscriptionController.refreshCachedActiveSubscriptionInfoList();
        }

        // Remove all remaining subscriptions which have embedded = true. We set embedded to false
        // to ensure they are not returned in the list of embedded subscriptions (but keep them
        // around in case the subscription is added back later, which is equivalent to a removable
        // SIM being removed and reinserted).
        if (!existingSubscriptions.isEmpty()) {
            if (DBG) {
                logd("Removing existing embedded subscriptions of size"
                        + existingSubscriptions.size());
            }
            List<String> iccidsToRemove = new ArrayList<>();
            for (int i = 0; i < existingSubscriptions.size(); i++) {
                SubscriptionInfo info = existingSubscriptions.get(i);
                if (info.isEmbedded()) {
                    if (DBG) logd("Removing embedded subscription of IccId " + info.getIccId());
                    iccidsToRemove.add("'" + info.getIccId() + "'");
                }
            }
            String whereClause = SubscriptionManager.ICC_ID + " IN ("
                    + TextUtils.join(",", iccidsToRemove) + ")";
            ContentValues values = new ContentValues();
            values.put(SubscriptionManager.IS_EMBEDDED, 0);
            hasChanges = true;
            contentResolver.update(SubscriptionManager.CONTENT_URI, values, whereClause, null);

            // refresh Cached Active Subscription Info List
            mSubscriptionController.refreshCachedActiveSubscriptionInfoList();
        }

        if (DBG) logd("updateEmbeddedSubscriptions done hasChanges=" + hasChanges);
        return hasChanges;
    }

    private int getEmbeddedProfilePortIndex(String iccId) {
        UiccSlot[] slots = UiccController.getInstance().getUiccSlots();
        for (UiccSlot slot : slots) {
            if (slot != null && slot.isEuicc()
                    && slot.getPortIndexFromIccId(iccId) != TelephonyManager.INVALID_PORT_INDEX) {
                return slot.getPortIndexFromIccId(iccId);
            }
        }
        return TelephonyManager.INVALID_PORT_INDEX;
    }
    /**
     * Called by CarrierConfigLoader to update the subscription before sending a broadcast.
     */
    public void updateSubscriptionByCarrierConfigAndNotifyComplete(int phoneId,
            String configPackageName, PersistableBundle config, Message onComplete) {
        post(() -> {
            updateSubscriptionByCarrierConfig(phoneId, configPackageName, config);
            onComplete.sendToTarget();
        });
    }

    private String getDefaultCarrierServicePackageName() {
        CarrierConfigManager configManager =
                (CarrierConfigManager) sContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        return configManager.getDefaultCarrierServicePackageName();
    }

    private boolean isCarrierServicePackage(int phoneId, String pkgName) {
        if (pkgName.equals(getDefaultCarrierServicePackageName())) return false;

        String carrierPackageName = TelephonyManager.from(sContext)
                .getCarrierServicePackageNameForLogicalSlot(phoneId);
        if (DBG) logd("Carrier service package for subscription = " + carrierPackageName);
        return pkgName.equals(carrierPackageName);
    }

    /**
     * Update the currently active Subscription based on information from CarrierConfig
     */
    @VisibleForTesting
    public void updateSubscriptionByCarrierConfig(
            int phoneId, String configPackageName, PersistableBundle config) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)
                || TextUtils.isEmpty(configPackageName) || config == null) {
            if (DBG) {
                logd("In updateSubscriptionByCarrierConfig(): phoneId=" + phoneId
                        + " configPackageName=" + configPackageName + " config="
                        + ((config == null) ? "null" : config.hashCode()));
            }
            return;
        }

        int currentSubId = mSubscriptionController.getSubIdUsingPhoneId(phoneId);
        if (!SubscriptionManager.isValidSubscriptionId(currentSubId)
                || currentSubId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
            if (DBG) logd("No subscription is active for phone being updated");
            return;
        }

        SubscriptionInfo currentSubInfo = mSubscriptionController.getSubscriptionInfo(currentSubId);
        if (currentSubInfo == null) {
            loge("Couldn't retrieve subscription info for current subscription");
            return;
        }

        ContentValues cv = new ContentValues();
        ParcelUuid groupUuid = null;

        // carrier certificates are not subscription-specific, so we want to load them even if
        // this current package is not a CarrierServicePackage
        String[] certs = config.getStringArray(
            CarrierConfigManager.KEY_CARRIER_CERTIFICATE_STRING_ARRAY);
        UiccAccessRule[] carrierConfigAccessRules = UiccAccessRule.decodeRulesFromCarrierConfig(
            certs);
        cv.put(SubscriptionManager.ACCESS_RULES_FROM_CARRIER_CONFIGS,
                UiccAccessRule.encodeRules(carrierConfigAccessRules));

        if (!isCarrierServicePackage(phoneId, configPackageName)) {
            loge("Cannot manage subId=" + currentSubId + ", carrierPackage=" + configPackageName);
        } else {
            boolean isOpportunistic = config.getBoolean(
                    CarrierConfigManager.KEY_IS_OPPORTUNISTIC_SUBSCRIPTION_BOOL, false);
            if (currentSubInfo.isOpportunistic() != isOpportunistic) {
                if (DBG) logd("Set SubId=" + currentSubId + " isOpportunistic=" + isOpportunistic);
                cv.put(SubscriptionManager.IS_OPPORTUNISTIC, isOpportunistic ? "1" : "0");
            }

            String groupUuidString =
                config.getString(CarrierConfigManager.KEY_SUBSCRIPTION_GROUP_UUID_STRING, "");
            if (!TextUtils.isEmpty(groupUuidString)) {
                try {
                    // Update via a UUID Structure to ensure consistent formatting
                    groupUuid = ParcelUuid.fromString(groupUuidString);
                    if (groupUuid.equals(REMOVE_GROUP_UUID)
                            && currentSubInfo.getGroupUuid() != null) {
                        cv.put(SubscriptionManager.GROUP_UUID, (String) null);
                        if (DBG) logd("Group Removed for" + currentSubId);
                    } else if (mSubscriptionController.canPackageManageGroup(
                            groupUuid, configPackageName)) {
                        cv.put(SubscriptionManager.GROUP_UUID, groupUuid.toString());
                        cv.put(SubscriptionManager.GROUP_OWNER, configPackageName);
                        if (DBG) logd("Group Added for" + currentSubId);
                    } else {
                        loge("configPackageName " + configPackageName + " doesn't own grouUuid "
                            + groupUuid);
                    }
                } catch (IllegalArgumentException e) {
                    loge("Invalid Group UUID=" + groupUuidString);
                }
            }
        }

        final int preferredUsageSetting =
                config.getInt(
                        CarrierConfigManager.KEY_CELLULAR_USAGE_SETTING_INT,
                        SubscriptionManager.USAGE_SETTING_UNKNOWN);

        @UsageSetting int newUsageSetting = calculateUsageSetting(
                currentSubInfo.getUsageSetting(),
                preferredUsageSetting);

        if (newUsageSetting != currentSubInfo.getUsageSetting()) {
            cv.put(SubscriptionManager.USAGE_SETTING, newUsageSetting);
            if (DBG) {
                logd("UsageSetting changed,"
                        + " oldSetting=" + currentSubInfo.getUsageSetting()
                        + " preferredSetting=" + preferredUsageSetting
                        + " newSetting=" + newUsageSetting);
            }
        } else {
            if (DBG) {
                logd("UsageSetting unchanged,"
                        + " oldSetting=" + currentSubInfo.getUsageSetting()
                        + " preferredSetting=" + preferredUsageSetting
                        + " newSetting=" + newUsageSetting);
            }
        }

        if (cv.size() > 0 && sContext.getContentResolver().update(SubscriptionManager
                    .getUriForSubscriptionId(currentSubId), cv, null, null) > 0) {
            mSubscriptionController.refreshCachedActiveSubscriptionInfoList();
            mSubscriptionController.notifySubscriptionInfoChanged();
            MultiSimSettingController.getInstance().notifySubscriptionGroupChanged(groupUuid);
        }
    }

    private static int findSubscriptionInfoForIccid(List<SubscriptionInfo> list, String iccid) {
        for (int i = 0; i < list.size(); i++) {
            if (TextUtils.equals(iccid, list.get(i).getIccId())) {
                return i;
            }
        }
        return -1;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected void broadcastSimStateChanged(int phoneId, String state, String reason) {
        // Note: This intent is way deprecated and is only being kept around because there's no
        // graceful way to deprecate a sticky broadcast that has a lot of listeners.
        // DO NOT add any new extras to this broadcast -- it is not protected by any permissions.
        Intent i = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        i.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        i.putExtra(PhoneConstants.PHONE_NAME_KEY, "Phone");
        i.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE, state);
        i.putExtra(IccCardConstants.INTENT_KEY_LOCKED_REASON, reason);
        SubscriptionManager.putPhoneIdAndSubIdExtra(i, phoneId);
        logd("Broadcasting intent ACTION_SIM_STATE_CHANGED " + state + " reason " + reason +
                " for phone: " + phoneId);
        IntentBroadcaster.getInstance().broadcastStickyIntent(sContext, i, phoneId);
    }

    protected void broadcastSimCardStateChanged(int phoneId, int state) {
        if (state != sSimCardState[phoneId]) {
            sSimCardState[phoneId] = state;
            Intent i = new Intent(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED);
            i.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            i.putExtra(TelephonyManager.EXTRA_SIM_STATE, state);
            SubscriptionManager.putPhoneIdAndSubIdExtra(i, phoneId);
            // TODO(b/130664115) we manually populate this intent with the slotId. In the future we
            // should do a review of whether to make this public
            UiccSlot slot = UiccController.getInstance().getUiccSlotForPhone(phoneId);
            int slotId = UiccController.getInstance().getSlotIdFromPhoneId(phoneId);
            i.putExtra(PhoneConstants.SLOT_KEY, slotId);
            if (slot != null) {
                i.putExtra(PhoneConstants.PORT_KEY, slot.getPortIndexFromPhoneId(phoneId));
            }
            logd("Broadcasting intent ACTION_SIM_CARD_STATE_CHANGED " + simStateString(state)
                    + " for phone: " + phoneId + " slot: " + slotId + " port: "
                    + slot.getPortIndexFromPhoneId(phoneId));
            sContext.sendBroadcast(i, Manifest.permission.READ_PRIVILEGED_PHONE_STATE);
            TelephonyMetrics.getInstance().updateSimState(phoneId, state);
        }
    }

    protected void broadcastSimApplicationStateChanged(int phoneId, int state) {
        // Broadcast if the state has changed, except if old state was UNKNOWN and new is NOT_READY,
        // because that's the initial state and a broadcast should be sent only on a transition
        // after SIM is PRESENT. The only exception is eSIM boot profile, where NOT_READY is the
        // terminal state.
        boolean isUnknownToNotReady =
                (sSimApplicationState[phoneId] == TelephonyManager.SIM_STATE_UNKNOWN
                        && state == TelephonyManager.SIM_STATE_NOT_READY);
        IccCard iccCard = PhoneFactory.getPhone(phoneId).getIccCard();
        boolean emptyProfile = iccCard != null && iccCard.isEmptyProfile();
        if (state != sSimApplicationState[phoneId] && (!isUnknownToNotReady || emptyProfile)) {
            sSimApplicationState[phoneId] = state;
            Intent i = new Intent(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
            i.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            i.putExtra(TelephonyManager.EXTRA_SIM_STATE, state);
            SubscriptionManager.putPhoneIdAndSubIdExtra(i, phoneId);
            // TODO(b/130664115) we populate this intent with the actual slotId. In the future we
            // should do a review of whether to make this public
            UiccSlot slot = UiccController.getInstance().getUiccSlotForPhone(phoneId);
            int slotId = UiccController.getInstance().getSlotIdFromPhoneId(phoneId);
            i.putExtra(PhoneConstants.SLOT_KEY, slotId);
            if (slot != null) {
                i.putExtra(PhoneConstants.PORT_KEY, slot.getPortIndexFromPhoneId(phoneId));
            }
            logd("Broadcasting intent ACTION_SIM_APPLICATION_STATE_CHANGED " + simStateString(state)
                    + " for phone: " + phoneId + " slot: " + slotId + "port: "
                    + slot.getPortIndexFromPhoneId(phoneId));
            sContext.sendBroadcast(i, Manifest.permission.READ_PRIVILEGED_PHONE_STATE);
            TelephonyMetrics.getInstance().updateSimState(phoneId, state);
        }
    }

    /**
     * Convert SIM state into string
     *
     * @param state SIM state
     * @return SIM state in string format
     */
    public static String simStateString(@SimState int state) {
        switch (state) {
            case TelephonyManager.SIM_STATE_UNKNOWN:
                return "UNKNOWN";
            case TelephonyManager.SIM_STATE_ABSENT:
                return "ABSENT";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return "PIN_REQUIRED";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return "PUK_REQUIRED";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return "NETWORK_LOCKED";
            case TelephonyManager.SIM_STATE_READY:
                return "READY";
            case TelephonyManager.SIM_STATE_NOT_READY:
                return "NOT_READY";
            case TelephonyManager.SIM_STATE_PERM_DISABLED:
                return "PERM_DISABLED";
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                return "CARD_IO_ERROR";
            case TelephonyManager.SIM_STATE_CARD_RESTRICTED:
                return "CARD_RESTRICTED";
            case TelephonyManager.SIM_STATE_LOADED:
                return "LOADED";
            case TelephonyManager.SIM_STATE_PRESENT:
                return "PRESENT";
            default:
                return "INVALID";
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private static void logd(String message) {
        Rlog.d(LOG_TAG, message);
    }

    private static void loge(String message) {
        Rlog.e(LOG_TAG, message);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("SubscriptionInfoUpdater:");
        mCarrierServiceBindHelper.dump(fd, pw, args);
    }
}
