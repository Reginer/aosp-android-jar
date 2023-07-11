/*
 * Copyright 2017 The Android Open Source Project
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
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.UserHandle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.WindowManager;

import com.android.internal.R;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.euicc.EuiccCard;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class represents a physical slot on the device.
 */
public class UiccSlot extends Handler {
    private static final String TAG = "UiccSlot";
    private static final boolean DBG = true;

    public static final String EXTRA_ICC_CARD_ADDED =
            "com.android.internal.telephony.uicc.ICC_CARD_ADDED";
    public static final int INVALID_PHONE_ID = -1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"VOLTAGE_CLASS_"},
            value = {VOLTAGE_CLASS_UNKNOWN, VOLTAGE_CLASS_A, VOLTAGE_CLASS_B, VOLTAGE_CLASS_C})
    public @interface VoltageClass {}

    public static final int VOLTAGE_CLASS_UNKNOWN = 0;
    public static final int VOLTAGE_CLASS_A = 1;
    public static final int VOLTAGE_CLASS_B = 2;
    public static final int VOLTAGE_CLASS_C = 3;

    private final Object mLock = new Object();
    private boolean mActive;
    private boolean mStateIsUnknown = true;
    private CardState mCardState;
    private Context mContext;
    private UiccCard mUiccCard;
    private boolean mIsEuicc;
    private @VoltageClass int mMinimumVoltageClass;
    private String mEid;
    private AnswerToReset mAtr;
    private boolean mIsRemovable;
    // Map each available portIdx to phoneId
    private HashMap<Integer, Integer> mPortIdxToPhoneId = new HashMap<>();
    //Map each available portIdx with old radio state for state checking
    private HashMap<Integer, Integer> mLastRadioState = new HashMap<>();
    // Store iccId of each port.
    private HashMap<Integer, String> mIccIds = new HashMap<>();

    private static final int EVENT_CARD_REMOVED = 13;
    private static final int EVENT_CARD_ADDED = 14;

    public UiccSlot(Context c, boolean isActive) {
        if (DBG) log("Creating");
        mContext = c;
        mActive = isActive;
        mCardState = null;
    }

    /**
     * Update slot. The main trigger for this is a change in the ICC Card status.
     */
    public void update(CommandsInterface ci, IccCardStatus ics, int phoneId, int slotIndex) {
        if (DBG) log("cardStatus update: " + ics.toString());
        synchronized (mLock) {
            mPortIdxToPhoneId.put(ics.mSlotPortMapping.mPortIndex, phoneId);
            CardState oldState = mCardState;
            mCardState = ics.mCardState;
            mIccIds.put(ics.mSlotPortMapping.mPortIndex, ics.iccid);
            parseAtr(ics.atr);
            mIsRemovable = isSlotRemovable(slotIndex);

            int radioState = ci.getRadioState();
            if (DBG) {
                log("update: radioState=" + radioState + " mLastRadioState=" + mLastRadioState);
            }

            if (absentStateUpdateNeeded(oldState)) {
                updateCardStateAbsent(ci.getRadioState(), phoneId,
                        ics.mSlotPortMapping.mPortIndex);
            // Because mUiccCard may be updated in both IccCardStatus and IccSlotStatus, we need to
            // create a new UiccCard instance in two scenarios:
            //   1. mCardState is changing from ABSENT to non ABSENT.
            //   2. The latest mCardState is not ABSENT, but there is no UiccCard instance.
            } else if ((oldState == null || oldState == CardState.CARDSTATE_ABSENT
                    || mUiccCard == null) && mCardState != CardState.CARDSTATE_ABSENT) {
                // No notification while we are just powering up
                if (radioState != TelephonyManager.RADIO_POWER_UNAVAILABLE
                        && mLastRadioState.getOrDefault(ics.mSlotPortMapping.mPortIndex,
                        TelephonyManager.RADIO_POWER_UNAVAILABLE)
                        != TelephonyManager.RADIO_POWER_UNAVAILABLE) {
                    if (DBG) log("update: notify card added");
                    sendMessage(obtainMessage(EVENT_CARD_ADDED, null));
                }

                // card is present in the slot now; create new mUiccCard
                if (mUiccCard != null) {
                    loge("update: mUiccCard != null when card was present; disposing it now");
                    mUiccCard.dispose();
                }

                if (!mIsEuicc) {
                    // Uicc does not support MEP, passing false by default.
                    mUiccCard = new UiccCard(mContext, ci, ics, phoneId, mLock, false);
                } else {
                    // The EID should be reported with the card status, but in case it's not we want
                    // to catch that here
                    if (TextUtils.isEmpty(ics.eid)) {
                        loge("update: eid is missing. ics.eid="
                                + Rlog.pii(TelephonyUtils.IS_DEBUGGABLE, ics.eid));
                    }
                    mUiccCard = new EuiccCard(mContext, ci, ics, phoneId, mLock,
                            isMultipleEnabledProfileSupported());
                }
            } else {
                if (mUiccCard != null) {
                    mUiccCard.update(mContext, ci, ics, phoneId);
                }
            }
            mLastRadioState.put(ics.mSlotPortMapping.mPortIndex, radioState);
        }
    }

    /**
     * Update slot based on IccSlotStatus.
     */
    public void update(CommandsInterface[] ci, IccSlotStatus iss, int slotIndex) {
        if (DBG) log("slotStatus update: " + iss.toString());
        synchronized (mLock) {
            IccSimPortInfo[] simPortInfos = iss.mSimPortInfos;
            CardState oldState = mCardState;
            parseAtr(iss.atr);
            mCardState = iss.cardState;
            mEid = iss.eid;
            mIsRemovable = isSlotRemovable(slotIndex);

            for (int i = 0; i < simPortInfos.length; i++) {
                int phoneId = iss.mSimPortInfos[i].mLogicalSlotIndex;
                mIccIds.put(i, simPortInfos[i].mIccId);
                if (!iss.mSimPortInfos[i].mPortActive) {
                    // TODO: (b/79432584) evaluate whether should broadcast card state change
                    // even if it's inactive.
                    UiccController.updateInternalIccStateForInactivePort(mContext,
                            mPortIdxToPhoneId.getOrDefault(i, INVALID_PHONE_ID),
                            iss.mSimPortInfos[i].mIccId);
                    mLastRadioState.put(i, TelephonyManager.RADIO_POWER_UNAVAILABLE);
                    if (mUiccCard != null) {
                        // Dispose the port
                        mUiccCard.disposePort(i);
                    }
                } else {
                    if (absentStateUpdateNeeded(oldState)) {
                        int radioState = SubscriptionManager.isValidPhoneId(phoneId) ?
                                ci[phoneId].getRadioState() :
                                TelephonyManager.RADIO_POWER_UNAVAILABLE;
                        updateCardStateAbsent(radioState, phoneId, i);
                    }
                    // TODO: (b/79432584) Create UiccCard or EuiccCard object here.
                    // Right now It's OK not creating it because Card status update will do it.
                    // But we should really make them symmetric.
                }
            }
            // From MEP, Card can have multiple ports. So dispose UiccCard only when all the
            // ports are inactive.
            if (!hasActivePort(simPortInfos)) {
                if (mActive) {
                    mActive = false;
                    nullifyUiccCard(true /* sim state is unknown */);
                }
            } else {
                mActive = true;
            }
            mPortIdxToPhoneId.clear();
            for (int i = 0; i < simPortInfos.length; i++) {
                // If port is not active, update with invalid phone id(i.e. -1)
                mPortIdxToPhoneId.put(i, simPortInfos[i].mPortActive ?
                        simPortInfos[i].mLogicalSlotIndex : INVALID_PHONE_ID);
            }
            // Since the MEP capability is related with number ports reported, thus need to
            // update the flag after UiccCard creation.
            if (mUiccCard != null) {
                mUiccCard.updateSupportMultipleEnabledProfile(isMultipleEnabledProfileSupported());
            }
        }
    }

    private boolean hasActivePort(IccSimPortInfo[] simPortInfos) {
        for (IccSimPortInfo simPortInfo : simPortInfos) {
            if (simPortInfo.mPortActive) {
                return true;
            }
        }
        return false;
    }

    /* Return valid phoneId if possible from the portIdx mapping*/
    private int getAnyValidPhoneId() {
        for (int phoneId : mPortIdxToPhoneId.values()) {
            if (SubscriptionManager.isValidPhoneId(phoneId)) {
                return phoneId;
            }
        }
        return INVALID_PHONE_ID;
    }

    @NonNull
    public int[] getPortList() {
        synchronized (mLock) {
            return mPortIdxToPhoneId.keySet().stream().mapToInt(Integer::valueOf).toArray();
        }
    }

    /** Return whether the passing portIndex belong to this physical slot */
    public boolean isValidPortIndex(int portIndex) {
        return mPortIdxToPhoneId.containsKey(portIndex);
    }

    public int getPortIndexFromPhoneId(int phoneId) {
        synchronized (mLock) {
            for (Map.Entry<Integer, Integer> entry : mPortIdxToPhoneId.entrySet()) {
                if (entry.getValue() == phoneId) {
                    return entry.getKey();
                }
            }
            return TelephonyManager.DEFAULT_PORT_INDEX;
        }
    }

    public int getPortIndexFromIccId(String iccId) {
        synchronized (mLock) {
            for (Map.Entry<Integer, String> entry : mIccIds.entrySet()) {
                if (IccUtils.compareIgnoreTrailingFs(entry.getValue(), iccId)) {
                    return entry.getKey();
                }
            }
            // If iccId is not found, return invalid port index.
            return TelephonyManager.INVALID_PORT_INDEX;
        }
    }

    public int getPhoneIdFromPortIndex(int portIndex) {
        synchronized (mLock) {
            return mPortIdxToPhoneId.getOrDefault(portIndex, INVALID_PHONE_ID);
        }
    }

    public boolean isPortActive(int portIdx) {
        synchronized (mLock) {
            return SubscriptionManager.isValidPhoneId(
                    mPortIdxToPhoneId.getOrDefault(portIdx, INVALID_PHONE_ID));
        }
    }

    /* Returns true if multiple enabled profiles are supported */
    public boolean isMultipleEnabledProfileSupported() {
        // even ATR suggest UICC supports multiple enabled profiles, MEP can be disabled per
        // carrier restrictions, so checking the real number of ports reported from modem is
        // necessary.
        return mPortIdxToPhoneId.size() > 1 && mAtr != null &&
                mAtr.isMultipleEnabledProfilesSupported();
    }

    private boolean absentStateUpdateNeeded(CardState oldState) {
        return (oldState != CardState.CARDSTATE_ABSENT || mUiccCard != null)
                && mCardState == CardState.CARDSTATE_ABSENT;
    }

    private void updateCardStateAbsent(int radioState, int phoneId, int portIndex) {
        // No notification while we are just powering up
        if (radioState != TelephonyManager.RADIO_POWER_UNAVAILABLE
                && mLastRadioState.getOrDefault(
                        portIndex, TelephonyManager.RADIO_POWER_UNAVAILABLE)
                != TelephonyManager.RADIO_POWER_UNAVAILABLE) {
            if (DBG) log("update: notify card removed");
            sendMessage(obtainMessage(EVENT_CARD_REMOVED, null));
        }
        UiccController.updateInternalIccState(mContext, IccCardConstants.State.ABSENT,
                null, phoneId);
        // no card present in the slot now; dispose card and make mUiccCard null
        nullifyUiccCard(false /* sim state is not unknown */);
        mLastRadioState.put(portIndex, TelephonyManager.RADIO_POWER_UNAVAILABLE);
    }

    // whenever we set mUiccCard to null, we lose the ability to differentiate between absent and
    // unknown states. To mitigate this, we will us mStateIsUnknown to keep track. The sim is only
    // unknown if we haven't heard from the radio or if the radio has become unavailable.
    private void nullifyUiccCard(boolean stateUnknown) {
        if (mUiccCard != null) {
            mUiccCard.dispose();
        }
        mStateIsUnknown = stateUnknown;
        mUiccCard = null;
    }

    public boolean isStateUnknown() {
        if (mCardState == null || mCardState == CardState.CARDSTATE_ABSENT) {
            // mStateIsUnknown is valid only in this scenario.
            return mStateIsUnknown;
        }
        // if mUiccCard is null, assume the state to be UNKNOWN for now.
        // The state may be known but since the actual card object is not available,
        // it is safer to return UNKNOWN.
        return mUiccCard == null;
    }

    // Return true if a slot index is for removable UICCs or eUICCs
    private boolean isSlotRemovable(int slotIndex) {
        int[] euiccSlots = mContext.getResources()
                .getIntArray(com.android.internal.R.array.non_removable_euicc_slots);
        if (euiccSlots == null) {
            return true;
        }
        for (int euiccSlot : euiccSlots) {
            if (euiccSlot == slotIndex) {
                return false;
            }
        }

        return true;
    }

    private void checkIsEuiccSupported() {
        if (mAtr == null) {
            mIsEuicc = false;
            return;
        }
        mIsEuicc = mAtr.isEuiccSupported();
        log(" checkIsEuiccSupported : " + mIsEuicc);
    }

    private void checkMinimumVoltageClass() {
        mMinimumVoltageClass = VOLTAGE_CLASS_UNKNOWN;
        if (mAtr == null) {
            return;
        }
        // Supported voltage classes are stored in the 5 least significant bits of the TA byte for
        // global interface.
        List<AnswerToReset.InterfaceByte> interfaceBytes = mAtr.getInterfaceBytes();
        for (int i = 0; i < interfaceBytes.size() - 1; i++) {
            if (interfaceBytes.get(i).getTD() != null
                    && (interfaceBytes.get(i).getTD() & AnswerToReset.T_MASK)
                            == AnswerToReset.T_VALUE_FOR_GLOBAL_INTERFACE
                    && interfaceBytes.get(i + 1).getTA() != null) {
                byte ta = interfaceBytes.get(i + 1).getTA();
                if ((ta & 0x01) != 0) {
                    mMinimumVoltageClass = VOLTAGE_CLASS_A;
                }
                if ((ta & 0x02) != 0) {
                    mMinimumVoltageClass = VOLTAGE_CLASS_B;
                }
                if ((ta & 0x04) != 0) {
                    mMinimumVoltageClass = VOLTAGE_CLASS_C;
                }
                return;
            }
        }
        // Use default value - only class A
        mMinimumVoltageClass = VOLTAGE_CLASS_A;
    }

    private void parseAtr(String atr) {
        mAtr = AnswerToReset.parseAtr(atr);
        checkIsEuiccSupported();
        checkMinimumVoltageClass();
    }

    public boolean isEuicc() {
        return mIsEuicc;
    }

    @VoltageClass
    public int getMinimumVoltageClass() {
        return mMinimumVoltageClass;
    }

    public boolean isActive() {
        return mActive;
    }

    public boolean isRemovable() {
        return mIsRemovable;
    }

    /**
     *  Returns the iccId specific to the port index.
     *  Always use {@link com.android.internal.telephony.uicc.UiccPort#getIccId} to get the iccId.
     *  Use this API to get the iccId of the inactive port only.
     */
    public String getIccId(int portIdx) {
        return mIccIds.get(portIdx);
    }

    public String getEid() {
        return mEid;
    }

    public boolean isExtendedApduSupported() {
        return  (mAtr != null && mAtr.isExtendedApduSupported());
    }

    @Override
    protected void finalize() {
        if (DBG) log("UiccSlot finalized");
    }

    private void onIccSwap(boolean isAdded) {

        boolean isHotSwapSupported = mContext.getResources().getBoolean(
                R.bool.config_hotswapCapable);

        if (isHotSwapSupported) {
            log("onIccSwap: isHotSwapSupported is true, don't prompt for rebooting");
            return;
        }
        // As this check is for shutdown status check, use any phoneId
        Phone phone = PhoneFactory.getPhone(getAnyValidPhoneId());
        if (phone != null && phone.isShuttingDown()) {
            log("onIccSwap: already doing shutdown, no need to prompt");
            return;
        }

        log("onIccSwap: isHotSwapSupported is false, prompt for rebooting");

        promptForRestart(isAdded);
    }

    private void promptForRestart(boolean isAdded) {
        synchronized (mLock) {
            final Resources res = mContext.getResources();
            final ComponentName dialogComponent = ComponentName.unflattenFromString(
                    res.getString(R.string.config_iccHotswapPromptForRestartDialogComponent));
            if (dialogComponent != null) {
                Intent intent = new Intent().setComponent(dialogComponent)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(EXTRA_ICC_CARD_ADDED, isAdded);
                try {
                    mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    return;
                } catch (ActivityNotFoundException e) {
                    loge("Unable to find ICC hotswap prompt for restart activity: " + e);
                }
            }

            // TODO: Here we assume the device can't handle SIM hot-swap
            //      and has to reboot. We may want to add a property,
            //      e.g. REBOOT_ON_SIM_SWAP, to indicate if modem support
            //      hot-swap.
            DialogInterface.OnClickListener listener = null;


            // TODO: SimRecords is not reset while SIM ABSENT (only reset while
            //       Radio_off_or_not_available). Have to reset in both both
            //       added or removed situation.
            listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    synchronized (mLock) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            if (DBG) log("Reboot due to SIM swap");
                            PowerManager pm = (PowerManager) mContext
                                    .getSystemService(Context.POWER_SERVICE);
                            pm.reboot("SIM is added.");
                        }
                    }
                }

            };

            Resources r = Resources.getSystem();

            String title = (isAdded) ? r.getString(R.string.sim_added_title) :
                    r.getString(R.string.sim_removed_title);
            String message = (isAdded) ? r.getString(R.string.sim_added_message) :
                    r.getString(R.string.sim_removed_message);
            String buttonTxt = r.getString(R.string.sim_restart_button);

            AlertDialog dialog = new AlertDialog.Builder(mContext)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(buttonTxt, listener)
                    .create();
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            dialog.show();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_CARD_REMOVED:
                onIccSwap(false);
                break;
            case EVENT_CARD_ADDED:
                onIccSwap(true);
                break;
            default:
                loge("Unknown Event " + msg.what);
        }
    }

    /**
     * Returns the state of the UiccCard in the slot.
     * @return
     */
    public CardState getCardState() {
        synchronized (mLock) {
            if (mCardState == null) {
                return CardState.CARDSTATE_ABSENT;
            } else {
                return mCardState;
            }
        }
    }

    /**
     * Returns the UiccCard in the slot.
     */
    public UiccCard getUiccCard() {
        synchronized (mLock) {
            return mUiccCard;
        }
    }

    /**
     * Processes radio state unavailable event
     */
    public void onRadioStateUnavailable(int phoneId) {
        nullifyUiccCard(true /* sim state is unknown */);

        if (phoneId != INVALID_PHONE_ID) {
            UiccController.updateInternalIccState(
                    mContext, IccCardConstants.State.UNKNOWN, null, phoneId);
            mLastRadioState.put(getPortIndexFromPhoneId(phoneId),
                    TelephonyManager.RADIO_POWER_UNAVAILABLE);
        }

        mCardState = null;
    }

    private void log(String msg) {
        Rlog.d(TAG, msg);
    }

    private void loge(String msg) {
        Rlog.e(TAG, msg);
    }

    private Map<Integer, String> getPrintableIccIds() {
        Map<Integer, String> printableIccIds = mIccIds.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> SubscriptionInfo.givePrintableIccid(e.getValue())));
        return printableIccIds;
    }

    /**
     * Dump
     */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("UiccSlot:");
        pw.println(" mActive=" + mActive);
        pw.println(" mIsEuicc=" + mIsEuicc);
        pw.println(" isEuiccSupportsMultipleEnabledProfiles="
                + isMultipleEnabledProfileSupported());
        pw.println(" mIsRemovable=" + mIsRemovable);
        pw.println(" mLastRadioState=" + mLastRadioState);
        pw.println(" mIccIds=" + getPrintableIccIds());
        pw.println(" mPortIdxToPhoneId=" + mPortIdxToPhoneId);
        pw.println(" mEid=" + Rlog.pii(TelephonyUtils.IS_DEBUGGABLE, mEid));
        pw.println(" mCardState=" + mCardState);
        if (mUiccCard != null) {
            pw.println(" mUiccCard=" + mUiccCard);
            mUiccCard.dump(fd, pw, args);
        } else {
            pw.println(" mUiccCard=null");
        }
        pw.println();
        pw.flush();
    }
}
