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

import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.IccCardStatus.PinState;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

/**
 * {@hide}
 */
public class UiccCard {
    protected static final String LOG_TAG = "UiccCard";
    protected static final boolean DBG = true;

    public static final String EXTRA_ICC_CARD_ADDED =
            "com.android.internal.telephony.uicc.ICC_CARD_ADDED";

    // The lock object is created by UiccSlot that owns this UiccCard - this is to share the lock
    // between UiccSlot, UiccCard, EuiccCard, and UiccProfile for now.
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected final Object mLock;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private CardState mCardState;
    private String mIccid;
    protected String mCardId;
    private UiccProfile mUiccProfile;
    @UnsupportedAppUsage
    private Context mContext;
    @UnsupportedAppUsage
    private CommandsInterface mCi;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private final int mPhoneId;

    public UiccCard(Context c, CommandsInterface ci, IccCardStatus ics, int phoneId, Object lock) {
        if (DBG) log("Creating");
        mCardState = ics.mCardState;
        mPhoneId = phoneId;
        mLock = lock;
        update(c, ci, ics);
    }

    public void dispose() {
        synchronized (mLock) {
            if (DBG) log("Disposing card");
            if (mUiccProfile != null) {
                mUiccProfile.dispose();
            }
            mUiccProfile = null;
        }
    }

    public void update(Context c, CommandsInterface ci, IccCardStatus ics) {
        synchronized (mLock) {
            mCardState = ics.mCardState;
            mContext = c;
            mCi = ci;
            mIccid = ics.iccid;
            updateCardId();

            if (mCardState != CardState.CARDSTATE_ABSENT) {
                if (mUiccProfile == null) {
                    mUiccProfile = TelephonyComponentFactory.getInstance()
                            .inject(UiccProfile.class.getName()).makeUiccProfile(
                            mContext, mCi, ics, mPhoneId, this, mLock);
                } else {
                    mUiccProfile.update(mContext, mCi, ics);
                }
            } else {
                throw new RuntimeException("Card state is absent when updating!");
            }
        }
    }

    @Override
    protected void finalize() {
        if (DBG) log("UiccCard finalized");
    }

    /**
     * Updates the ID of the SIM card.
     *
     * <p>Whenever the {@link UiccCard#update(Context, CommandsInterface, IccCardStatus)} is called,
     * this function needs to be called to update the card ID. Subclasses of {@link UiccCard}
     * could override this function to set the {@link UiccCard#mCardId} to be something else instead
     * of {@link UiccCard#mIccid}.</p>
     */
    protected void updateCardId() {
        mCardId = mIccid;
    }

    /**
     * Notifies handler when carrier privilege rules are loaded.
     * @deprecated Please use
     * {@link UiccProfile#registerForCarrierPrivilegeRulesLoaded(Handler, int, Object)} instead.
     */
    @Deprecated
    public void registerForCarrierPrivilegeRulesLoaded(Handler h, int what, Object obj) {
        synchronized (mLock) {
            if (mUiccProfile != null) {
                mUiccProfile.registerForCarrierPrivilegeRulesLoaded(h, what, obj);
            } else {
                loge("registerForCarrierPrivilegeRulesLoaded Failed!");
            }
        }
    }

    /**
     * @deprecated Please use
     * {@link UiccProfile#unregisterForCarrierPrivilegeRulesLoaded(Handler)} instead.
     */
    @Deprecated
    public void unregisterForCarrierPrivilegeRulesLoaded(Handler h) {
        synchronized (mLock) {
            if (mUiccProfile != null) {
                mUiccProfile.unregisterForCarrierPrivilegeRulesLoaded(h);
            } else {
                loge("unregisterForCarrierPrivilegeRulesLoaded Failed!");
            }
        }
    }

    /**
     * @deprecated Please use {@link UiccProfile#isApplicationOnIcc(AppType)} instead.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Deprecated
    public boolean isApplicationOnIcc(IccCardApplicationStatus.AppType type) {
        synchronized (mLock) {
            if (mUiccProfile != null) {
                return mUiccProfile.isApplicationOnIcc(type);
            } else {
                return false;
            }
        }
    }

    @UnsupportedAppUsage
    public CardState getCardState() {
        synchronized (mLock) {
            return mCardState;
        }
    }

    /**
     * @deprecated Please use {@link UiccProfile#getUniversalPinState()} instead.
     */
    @Deprecated
    public PinState getUniversalPinState() {
        synchronized (mLock) {
            if (mUiccProfile != null) {
                return mUiccProfile.getUniversalPinState();
            } else {
                return PinState.PINSTATE_UNKNOWN;
            }
        }
    }

    /**
     * @deprecated Please use {@link UiccProfile#getApplication(int)} instead.
     */
    @UnsupportedAppUsage
    @Deprecated
    public UiccCardApplication getApplication(int family) {
        synchronized (mLock) {
            if (mUiccProfile != null) {
                return mUiccProfile.getApplication(family);
            } else {
                return null;
            }
        }
    }

    /**
     * @deprecated Please use {@link UiccProfile#getApplicationIndex(int)} instead.
     */
    @UnsupportedAppUsage
    @Deprecated
    public UiccCardApplication getApplicationIndex(int index) {
        synchronized (mLock) {
            if (mUiccProfile != null) {
                return mUiccProfile.getApplicationIndex(index);
            } else {
                return null;
            }
        }
    }

    /**
     * Returns the SIM application of the specified type.
     *
     * @param type ICC application type (@see com.android.internal.telephony.PhoneConstants#APPTYPE_xxx)
     * @return application corresponding to type or a null if no match found
     *
     * @deprecated Please use {@link UiccProfile#getApplicationByType(int)} instead.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Deprecated
    public UiccCardApplication getApplicationByType(int type) {
        synchronized (mLock) {
            if (mUiccProfile != null) {
                return mUiccProfile.getApplicationByType(type);
            } else {
                return null;
            }
        }
    }

    /**
     * Resets the application with the input AID. Returns true if any changes were made.
     *
     * A null aid implies a card level reset - all applications must be reset.
     *
     * @deprecated Please use {@link UiccProfile#resetAppWithAid(String, boolean)} instead.
     */
    @Deprecated
    public boolean resetAppWithAid(String aid, boolean reset) {
        synchronized (mLock) {
            if (mUiccProfile != null) {
                return mUiccProfile.resetAppWithAid(aid, reset);
            } else {
                return false;
            }
        }
    }

    /**
     * Exposes {@link CommandsInterface#iccOpenLogicalChannel}
     * @deprecated Please use
     * {@link UiccProfile#iccOpenLogicalChannel(String, int, Message)} instead.
     */
    @Deprecated
    public void iccOpenLogicalChannel(String AID, int p2, Message response) {
        if (mUiccProfile != null) {
            mUiccProfile.iccOpenLogicalChannel(AID, p2, response);
        } else {
            loge("iccOpenLogicalChannel Failed!");
        }
    }

    /**
     * Exposes {@link CommandsInterface#iccCloseLogicalChannel}
     * @deprecated Please use
     * {@link UiccProfile#iccCloseLogicalChannel(int, Message)} instead.
     */
    @Deprecated
    public void iccCloseLogicalChannel(int channel, Message response) {
        if (mUiccProfile != null) {
            mUiccProfile.iccCloseLogicalChannel(channel, response);
        } else {
            loge("iccCloseLogicalChannel Failed!");
        }
    }

    /**
     * Exposes {@link CommandsInterface#iccTransmitApduLogicalChannel}
     * @deprecated Please use {@link
     * UiccProfile#iccTransmitApduLogicalChannel(int, int, int, int, int, int, String, Message)}
     * instead.
     */
    @Deprecated
    public void iccTransmitApduLogicalChannel(int channel, int cla, int command,
            int p1, int p2, int p3, String data, Message response) {
        if (mUiccProfile != null) {
            mUiccProfile.iccTransmitApduLogicalChannel(channel, cla, command, p1, p2, p3,
                    data, response);
        } else {
            loge("iccTransmitApduLogicalChannel Failed!");
        }
    }

    /**
     * Exposes {@link CommandsInterface#iccTransmitApduBasicChannel}
     * @deprecated Please use
     * {@link UiccProfile#iccTransmitApduBasicChannel(int, int, int, int, int, String, Message)}
     * instead.
     */
    @Deprecated
    public void iccTransmitApduBasicChannel(int cla, int command,
            int p1, int p2, int p3, String data, Message response) {
        if (mUiccProfile != null) {
            mUiccProfile.iccTransmitApduBasicChannel(cla, command, p1, p2, p3, data, response);
        } else {
            loge("iccTransmitApduBasicChannel Failed!");
        }
    }

    /**
     * Exposes {@link CommandsInterface#iccIO}
     * @deprecated Please use
     * {@link UiccProfile#iccExchangeSimIO(int, int, int, int, int, String, Message)} instead.
     */
    @Deprecated
    public void iccExchangeSimIO(int fileID, int command, int p1, int p2, int p3,
            String pathID, Message response) {
        if (mUiccProfile != null) {
            mUiccProfile.iccExchangeSimIO(fileID, command, p1, p2, p3, pathID, response);
        } else {
            loge("iccExchangeSimIO Failed!");
        }
    }

    /**
     * Exposes {@link CommandsInterface#sendEnvelopeWithStatus}
     * @deprecated Please use {@link UiccProfile#sendEnvelopeWithStatus(String, Message)} instead.
     */
    @Deprecated
    public void sendEnvelopeWithStatus(String contents, Message response) {
        if (mUiccProfile != null) {
            mUiccProfile.sendEnvelopeWithStatus(contents, response);
        } else {
            loge("sendEnvelopeWithStatus Failed!");
        }
    }

    /**
     * Returns number of applications on this card
     * @deprecated Please use {@link UiccProfile#getNumApplications()} instead.
     */
    @UnsupportedAppUsage
    @Deprecated
    public int getNumApplications() {
        if (mUiccProfile != null) {
            return mUiccProfile.getNumApplications();
        } else {
            return 0;
        }
    }

    public int getPhoneId() {
        return mPhoneId;
    }

    public UiccProfile getUiccProfile() {
        return mUiccProfile;
    }

    /**
     * Returns true iff carrier privileges rules are null (dont need to be loaded) or loaded.
     * @deprecated Please use {@link UiccProfile#areCarrierPriviligeRulesLoaded()} instead.
     */
    @Deprecated
    public boolean areCarrierPriviligeRulesLoaded() {
        if (mUiccProfile != null) {
            return mUiccProfile.areCarrierPriviligeRulesLoaded();
        } else {
            return false;
        }
    }

    /**
     * Returns true if there are some carrier privilege rules loaded and specified.
     * @deprecated Please use {@link UiccProfile#hasCarrierPrivilegeRules()} instead.
     */
    @Deprecated
    public boolean hasCarrierPrivilegeRules() {
        if (mUiccProfile != null) {
            return mUiccProfile.hasCarrierPrivilegeRules();
        } else {
            return false;
        }
    }

    /**
     * Exposes {@link UiccCarrierPrivilegeRules#getCarrierPrivilegeStatus}.
     * @deprecated Please use
     * {@link UiccProfile#getCarrierPrivilegeStatus(Signature, String)} instead.
     */
    @Deprecated
    public int getCarrierPrivilegeStatus(Signature signature, String packageName) {
        if (mUiccProfile != null) {
            return mUiccProfile.getCarrierPrivilegeStatus(signature, packageName);
        } else {
            return TelephonyManager.CARRIER_PRIVILEGE_STATUS_RULES_NOT_LOADED;
        }
    }

    /**
     * Exposes {@link UiccCarrierPrivilegeRules#getCarrierPrivilegeStatus}.
     * @deprecated Please use
     * {@link UiccProfile#getCarrierPrivilegeStatus(PackageManager, String)} instead.
     */
    @Deprecated
    public int getCarrierPrivilegeStatus(PackageManager packageManager, String packageName) {
        if (mUiccProfile != null) {
            return mUiccProfile.getCarrierPrivilegeStatus(packageManager, packageName);
        } else {
            return TelephonyManager.CARRIER_PRIVILEGE_STATUS_RULES_NOT_LOADED;
        }
    }

    /**
     * Exposes {@link UiccCarrierPrivilegeRules#getCarrierPrivilegeStatus}.
     * @deprecated Please use {@link UiccProfile#getCarrierPrivilegeStatus(PackageInfo)} instead.
     */
    @Deprecated
    public int getCarrierPrivilegeStatus(PackageInfo packageInfo) {
        if (mUiccProfile != null) {
            return mUiccProfile.getCarrierPrivilegeStatus(packageInfo);
        } else {
            return TelephonyManager.CARRIER_PRIVILEGE_STATUS_RULES_NOT_LOADED;
        }
    }

    /**
     * Exposes {@link UiccCarrierPrivilegeRules#getCarrierPrivilegeStatusForCurrentTransaction}.
     * @deprecated Please use
     * {@link UiccProfile#getCarrierPrivilegeStatusForCurrentTransaction(PackageManager)} instead.
     */
    @Deprecated
    public int getCarrierPrivilegeStatusForCurrentTransaction(PackageManager packageManager) {
        if (mUiccProfile != null) {
            return mUiccProfile.getCarrierPrivilegeStatusForCurrentTransaction(packageManager);
        } else {
            return TelephonyManager.CARRIER_PRIVILEGE_STATUS_RULES_NOT_LOADED;
        }
    }

    /**
     * Exposes {@link UiccCarrierPrivilegeRules#getCarrierPackageNamesForIntent}.
     * @deprecated Please use
     * {@link UiccProfile#getCarrierPackageNamesForIntent(PackageManager, Intent)} instead.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Deprecated
    public List<String> getCarrierPackageNamesForIntent(
            PackageManager packageManager, Intent intent) {
        if (mUiccProfile != null) {
            return mUiccProfile.getCarrierPackageNamesForIntent(packageManager, intent);
        } else {
            return null;
        }
    }

    /**
     * @deprecated Please use {@link UiccProfile#setOperatorBrandOverride(String)} instead.
     */
    @Deprecated
    public boolean setOperatorBrandOverride(String brand) {
        if (mUiccProfile != null) {
            return mUiccProfile.setOperatorBrandOverride(brand);
        } else {
            return false;
        }
    }

    /**
     * @deprecated Please use {@link UiccProfile#getOperatorBrandOverride()} instead.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Deprecated
    public String getOperatorBrandOverride() {
        if (mUiccProfile != null) {
            return mUiccProfile.getOperatorBrandOverride();
        } else {
            return null;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public String getIccId() {
        if (mIccid != null) {
            return mIccid;
        } else if (mUiccProfile != null) {
            return mUiccProfile.getIccId();
        } else {
            return null;
        }
    }

    /**
     * Returns the ID of this SIM card, it is the ICCID of the active profile on the card for a UICC
     * card or the EID of the card for an eUICC card.
     */
    public String getCardId() {
        if (!TextUtils.isEmpty(mCardId)) {
            return mCardId;
        } else if (mUiccProfile != null) {
            return mUiccProfile.getIccId();
        } else {
            return null;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("UiccCard:");
        pw.println(" mCi=" + mCi);
        pw.println(" mCardState=" + mCardState);
        pw.println(" mCardId=" + mCardId);
        pw.println(" mPhoneId=" + mPhoneId);
        pw.println();
        if (mUiccProfile != null) {
            mUiccProfile.dump(fd, pw, args);
        }
    }
}
