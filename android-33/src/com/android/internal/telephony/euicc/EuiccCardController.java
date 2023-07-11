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
 * limitations under the License.
 */

package com.android.internal.telephony.euicc;

import android.annotation.Nullable;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ComponentInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.euicc.EuiccProfileInfo;
import android.telephony.TelephonyFrameworkInitializer;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccCardManager;
import android.telephony.euicc.EuiccNotification;
import android.telephony.euicc.EuiccRulesAuthTable;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccPort;
import com.android.internal.telephony.uicc.UiccSlot;
import com.android.internal.telephony.uicc.euicc.EuiccCard;
import com.android.internal.telephony.uicc.euicc.EuiccCardErrorException;
import com.android.internal.telephony.uicc.euicc.EuiccPort;
import com.android.internal.telephony.uicc.euicc.async.AsyncResultCallback;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/** Backing implementation of {@link EuiccCardManager}. */
public class EuiccCardController extends IEuiccCardController.Stub {
    private static final String TAG = "EuiccCardController";
    private static final String KEY_LAST_BOOT_COUNT = "last_boot_count";

    private final Context mContext;
    private AppOpsManager mAppOps;
    private String mCallingPackage;
    private ComponentInfo mBestComponent;
    private Handler mEuiccMainThreadHandler;
    private SimSlotStatusChangedBroadcastReceiver mSimSlotStatusChangeReceiver;
    private EuiccController mEuiccController;
    private UiccController mUiccController;

    private static EuiccCardController sInstance;

    private class SimSlotStatusChangedBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyManager.ACTION_SIM_SLOT_STATUS_CHANGED.equals(intent.getAction())) {
                // We want to keep listening if card is not present yet since the first state might
                // be an error state
                if (!isEmbeddedCardPresent()) {
                    return;
                }
                if (isEmbeddedSlotActivated()) {
                    mEuiccController.startOtaUpdatingIfNecessary();
                }
                mContext.unregisterReceiver(mSimSlotStatusChangeReceiver);
            }
        }
    }

    /** Initialize the instance. Should only be called once. */
    public static EuiccCardController init(Context context) {
        synchronized (EuiccCardController.class) {
            if (sInstance == null) {
                sInstance = new EuiccCardController(context);
            } else {
                Log.wtf(TAG, "init() called multiple times! sInstance = " + sInstance);
            }
        }
        return sInstance;
    }

    /** Get an instance. Assumes one has already been initialized with {@link #init}. */
    public static EuiccCardController get() {
        if (sInstance == null) {
            synchronized (EuiccCardController.class) {
                if (sInstance == null) {
                    throw new IllegalStateException("get() called before init()");
                }
            }
        }
        return sInstance;
    }

    private EuiccCardController(Context context) {
        this(context, new Handler(), EuiccController.get(), UiccController.getInstance());
        TelephonyFrameworkInitializer
                .getTelephonyServiceManager()
                .getEuiccCardControllerServiceRegisterer()
                .register(this);
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public EuiccCardController(
            Context context,
            Handler handler,
            EuiccController euiccController,
            UiccController uiccController) {
        mContext = context;
        mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        mEuiccMainThreadHandler = handler;
        mUiccController = uiccController;
        mEuiccController = euiccController;

        if (isBootUp(mContext)) {
            mSimSlotStatusChangeReceiver = new SimSlotStatusChangedBroadcastReceiver();
            mContext.registerReceiver(
                    mSimSlotStatusChangeReceiver,
                    new IntentFilter(TelephonyManager.ACTION_SIM_SLOT_STATUS_CHANGED));
        }
    }

    /**
     * Check whether the restored boot count is the same as current one. If not, update the restored
     * one.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public static boolean isBootUp(Context context) {
        int bootCount = Settings.Global.getInt(
                context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int lastBootCount = sp.getInt(KEY_LAST_BOOT_COUNT, -1);
        if (bootCount == -1 || lastBootCount == -1 || bootCount != lastBootCount) {
            sp.edit().putInt(KEY_LAST_BOOT_COUNT, bootCount).apply();
            return true;
        }
        return false;
    }

    /** Whether embedded slot is activated or not. */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public boolean isEmbeddedSlotActivated() {
        UiccSlot[] slots = mUiccController.getUiccSlots();
        if (slots == null) {
            return false;
        }
        for (int i = 0; i < slots.length; ++i) {
            UiccSlot slotInfo = slots[i];
            if (slotInfo != null && !slotInfo.isRemovable() && slotInfo.isActive()) {
                return true;
            }
        }
        return false;
    }

    /** Whether embedded card is present or not */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public boolean isEmbeddedCardPresent() {
        UiccSlot[] slots = mUiccController.getUiccSlots();
        if (slots == null) {
            return false;
        }
        for (UiccSlot slotInfo : slots) {
            if (slotInfo != null
                    && !slotInfo.isRemovable()
                    && slotInfo.getCardState() != null
                    && slotInfo.getCardState().isCardPresent()) {
                return true;
            }
        }
        return false;
    }

    private void checkCallingPackage(String callingPackage) {
        // Check the caller is LPA.
        mAppOps.checkPackage(Binder.getCallingUid(), callingPackage);
        mCallingPackage = callingPackage;
        mBestComponent = EuiccConnector.findBestComponent(mContext.getPackageManager());
        if (mBestComponent == null
                || !TextUtils.equals(mCallingPackage, mBestComponent.packageName)) {
            throw new SecurityException("The calling package can only be LPA.");
        }
    }

    private UiccSlot getUiccSlotForEmbeddedCard(String cardId) {
        int slotId = mUiccController.getUiccSlotForCardId(cardId);
        UiccSlot slot = mUiccController.getUiccSlot(slotId);
        if (slot == null) {
            loge("UiccSlot is null. slotId : " + slotId + " cardId : " + cardId);
            return null;
        }
        if (!slot.isEuicc()) {
            loge("UiccSlot is not embedded slot : " + slotId + " cardId : " + cardId);
            return null;
        }
        return slot;
    }

    private EuiccCard getEuiccCard(String cardId) {
        UiccSlot slot = getUiccSlotForEmbeddedCard(cardId);
        if (slot == null) {
            return null;
        }
        UiccCard card = slot.getUiccCard();
        if (card == null) {
            loge("UiccCard is null. cardId : " + cardId);
            return null;
        }
        return (EuiccCard) card;
    }

    private EuiccPort getEuiccPortFromIccId(String cardId, String iccid) {
        UiccSlot slot = getUiccSlotForEmbeddedCard(cardId);
        if (slot == null) {
            return null;
        }
        UiccCard card = slot.getUiccCard();
        if (card == null) {
            loge("UiccCard is null. cardId : " + cardId);
            return null;
        }
        int portIndex = slot.getPortIndexFromIccId(iccid);
        UiccPort port = card.getUiccPort(portIndex);
        if (port == null) {
            loge("UiccPort is null. cardId : " + cardId + " portIndex : " + portIndex);
            return null;
        }
        return (EuiccPort) port;
    }

    private EuiccPort getFirstActiveEuiccPort(String cardId) {
        EuiccCard card = getEuiccCard(cardId);
        if (card == null) {
            return null;
        }
        if (card.getUiccPortList().length > 0 ) {
            return (EuiccPort) card.getUiccPortList()[0]; // return first active port.
        }
        loge("No active ports exists. cardId : " + cardId);
        return null;
    }

    private EuiccPort getEuiccPort(String cardId, int portIndex) {
        EuiccCard card = getEuiccCard(cardId);
        if (card == null) {
            return null;
        }
        UiccPort port = card.getUiccPort(portIndex);
        if (port == null) {
            loge("UiccPort is null. cardId : " + cardId + " portIndex : " + portIndex);
            return null;
        }
        return (EuiccPort) port;
    }

    private int getResultCode(Throwable e) {
        if (e instanceof EuiccCardErrorException) {
            return ((EuiccCardErrorException) e).getErrorCode();
        }
        return EuiccCardManager.RESULT_UNKNOWN_ERROR;
    }

    @Override
    public void getAllProfiles(String callingPackage, String cardId,
            IGetAllProfilesCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("getAllProfiles callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<EuiccProfileInfo[]> cardCb =
                new AsyncResultCallback<EuiccProfileInfo[]>() {
            @Override
            public void onResult(EuiccProfileInfo[] result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK, result);
                } catch (RemoteException exception) {
                    loge("getAllProfiles callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("getAllProfiles callback onException: ", e);
                    callback.onComplete(getResultCode(e), null);
                } catch (RemoteException exception) {
                    loge("getAllProfiles callback failure.", exception);
                }
            }
        };

        port.getAllProfiles(cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void getProfile(String callingPackage, String cardId, String iccid,
            IGetProfileCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("getProfile callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<EuiccProfileInfo> cardCb = new AsyncResultCallback<EuiccProfileInfo>() {
                    @Override
                    public void onResult(EuiccProfileInfo result) {
                        try {
                            callback.onComplete(EuiccCardManager.RESULT_OK, result);
                        } catch (RemoteException exception) {
                            loge("getProfile callback failure.", exception);
                        }
                    }

                    @Override
                    public void onException(Throwable e) {
                        try {
                            loge("getProfile callback onException: ", e);
                            callback.onComplete(getResultCode(e), null);
                        } catch (RemoteException exception) {
                            loge("getProfile callback failure.", exception);
                        }
                    }
                };

        port.getProfile(iccid, cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void getEnabledProfile(String callingPackage, String cardId, int portIndex,
            IGetProfileCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        String iccId = null;
        boolean isValidSlotPort = false;
        // get the iccid whether or not the port is active
        for (UiccSlot slot : mUiccController.getUiccSlots()) {
            if (slot.getEid().equals(cardId)) {
                // find the matching slot. first validate if the passing port index is valid.
                if (slot.isValidPortIndex(portIndex)) {
                    isValidSlotPort = true;
                    iccId = slot.getIccId(portIndex);
                }
            }
        }
        if(!isValidSlotPort) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("getEnabledProfile callback failure due to invalid port slot.",
                        exception);
            }
            return;
        }
        // if there is no iccid enabled on this port, return null.
        if (TextUtils.isEmpty(iccId)) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_PROFILE_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("getEnabledProfile callback failure.", exception);
            }
            return;
        }

        EuiccPort port = getEuiccPort(cardId, portIndex);
        if (port == null) {
            // If the port is inactive, send the APDU on the first active port
            port = getFirstActiveEuiccPort(cardId);
            if (port == null) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
                } catch (RemoteException exception) {
                    loge("getEnabledProfile callback failure.", exception);
                }
                return;
            }
        }

        AsyncResultCallback<EuiccProfileInfo> cardCb = new AsyncResultCallback<EuiccProfileInfo>() {
            @Override
            public void onResult(EuiccProfileInfo result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK, result);
                } catch (RemoteException exception) {
                    loge("getEnabledProfile callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("getEnabledProfile callback onException: ", e);
                    callback.onComplete(getResultCode(e), null);
                } catch (RemoteException exception) {
                    loge("getEnabledProfile callback failure.", exception);
                }
            }
        };

        port.getProfile(iccId, cardCb, mEuiccMainThreadHandler);

    }

    @Override
    public void disableProfile(String callingPackage, String cardId, String iccid, boolean refresh,
            IDisableProfileCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getEuiccPortFromIccId(cardId, iccid);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND);
            } catch (RemoteException exception) {
                loge("disableProfile callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<Void> cardCb = new AsyncResultCallback<Void>() {
            @Override
            public void onResult(Void result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK);
                } catch (RemoteException exception) {
                    loge("disableProfile callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("disableProfile callback onException: ", e);
                    callback.onComplete(getResultCode(e));
                } catch (RemoteException exception) {
                    loge("disableProfile callback failure.", exception);
                }
            }
        };

        port.disableProfile(iccid, refresh, cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void switchToProfile(String callingPackage, String cardId, String iccid, int portIndex,
            boolean refresh, ISwitchToProfileCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getEuiccPort(cardId, portIndex);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("switchToProfile callback failure for portIndex :" + portIndex, exception);
            }
            return;
        }

        AsyncResultCallback<EuiccProfileInfo> profileCb =
                new AsyncResultCallback<EuiccProfileInfo>() {
            @Override
            public void onResult(EuiccProfileInfo profile) {
                AsyncResultCallback<Void> switchCb = new AsyncResultCallback<Void>() {
                    @Override
                    public void onResult(Void result) {
                        try {
                            callback.onComplete(EuiccCardManager.RESULT_OK, profile);
                        } catch (RemoteException exception) {
                            loge("switchToProfile callback failure.", exception);
                        }
                    }

                    @Override
                    public void onException(Throwable e) {
                        try {
                            loge("switchToProfile callback onException: ", e);
                            callback.onComplete(getResultCode(e), profile);
                        } catch (RemoteException exception) {
                            loge("switchToProfile callback failure.", exception);
                        }
                    }
                };

                port.switchToProfile(iccid, refresh, switchCb, mEuiccMainThreadHandler);
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("getProfile in switchToProfile callback onException: ", e);
                    callback.onComplete(getResultCode(e), null);
                } catch (RemoteException exception) {
                    loge("switchToProfile callback failure.", exception);
                }
            }
        };

        port.getProfile(iccid, profileCb, mEuiccMainThreadHandler);
    }

    @Override
    public void setNickname(String callingPackage, String cardId, String iccid, String nickname,
            ISetNicknameCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND);
            } catch (RemoteException exception) {
                loge("setNickname callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<Void> cardCb = new AsyncResultCallback<Void>() {
            @Override
            public void onResult(Void result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK);
                } catch (RemoteException exception) {
                    loge("setNickname callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("setNickname callback onException: ", e);
                    callback.onComplete(getResultCode(e));
                } catch (RemoteException exception) {
                    loge("setNickname callback failure.", exception);
                }
            }
        };

        port.setNickname(iccid, nickname, cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void deleteProfile(String callingPackage, String cardId, String iccid,
            IDeleteProfileCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND);
            } catch (RemoteException exception) {
                loge("deleteProfile callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<Void> cardCb = new AsyncResultCallback<Void>() {
            @Override
            public void onResult(Void result) {
                Log.i(TAG, "Request subscription info list refresh after delete.");
                SubscriptionController.getInstance()
                        .requestEmbeddedSubscriptionInfoListRefresh(
                                mUiccController.convertToPublicCardId(cardId));
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK);
                } catch (RemoteException exception) {
                    loge("deleteProfile callback failure.", exception);
                }
            };

            @Override
            public void onException(Throwable e) {
                try {
                    loge("deleteProfile callback onException: ", e);
                    callback.onComplete(getResultCode(e));
                } catch (RemoteException exception) {
                    loge("deleteProfile callback failure.", exception);
                }
            }
        };

        port.deleteProfile(iccid, cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void resetMemory(String callingPackage, String cardId,
            @EuiccCardManager.ResetOption int options, IResetMemoryCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND);
            } catch (RemoteException exception) {
                loge("resetMemory callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<Void> cardCb = new AsyncResultCallback<Void>() {
            @Override
            public void onResult(Void result) {
                Log.i(TAG, "Request subscription info list refresh after reset memory.");
                SubscriptionController.getInstance()
                        .requestEmbeddedSubscriptionInfoListRefresh(
                                mUiccController.convertToPublicCardId(cardId));
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK);
                } catch (RemoteException exception) {
                    loge("resetMemory callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("resetMemory callback onException: ", e);
                    callback.onComplete(getResultCode(e));
                } catch (RemoteException exception) {
                    loge("resetMemory callback failure.", exception);
                }
            }
        };

        port.resetMemory(options, cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void getDefaultSmdpAddress(String callingPackage, String cardId,
            IGetDefaultSmdpAddressCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("getDefaultSmdpAddress callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<String> cardCb = new AsyncResultCallback<String>() {
            @Override
            public void onResult(String result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK, result);
                } catch (RemoteException exception) {
                    loge("getDefaultSmdpAddress callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("getDefaultSmdpAddress callback onException: ", e);
                    callback.onComplete(getResultCode(e), null);
                } catch (RemoteException exception) {
                    loge("getDefaultSmdpAddress callback failure.", exception);
                }
            }
        };

        port.getDefaultSmdpAddress(cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void getSmdsAddress(String callingPackage, String cardId,
            IGetSmdsAddressCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("getSmdsAddress callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<String> cardCb = new AsyncResultCallback<String>() {
            @Override
            public void onResult(String result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK, result);
                } catch (RemoteException exception) {
                    loge("getSmdsAddress callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("getSmdsAddress callback onException: ", e);
                    callback.onComplete(getResultCode(e), null);
                } catch (RemoteException exception) {
                    loge("getSmdsAddress callback failure.", exception);
                }
            }
        };

        port.getSmdsAddress(cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void setDefaultSmdpAddress(String callingPackage, String cardId, String address,
            ISetDefaultSmdpAddressCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND);
            } catch (RemoteException exception) {
                loge("setDefaultSmdpAddress callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<Void> cardCb = new AsyncResultCallback<Void>() {
            @Override
            public void onResult(Void result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK);
                } catch (RemoteException exception) {
                    loge("setDefaultSmdpAddress callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("setDefaultSmdpAddress callback onException: ", e);
                    callback.onComplete(getResultCode(e));
                } catch (RemoteException exception) {
                    loge("setDefaultSmdpAddress callback failure.", exception);
                }
            }
        };

        port.setDefaultSmdpAddress(address, cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void getRulesAuthTable(String callingPackage, String cardId,
            IGetRulesAuthTableCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("getRulesAuthTable callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<EuiccRulesAuthTable> cardCb =
                new AsyncResultCallback<EuiccRulesAuthTable>() {
            @Override
            public void onResult(EuiccRulesAuthTable result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK, result);
                } catch (RemoteException exception) {
                    loge("getRulesAuthTable callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("getRulesAuthTable callback onException: ", e);
                    callback.onComplete(getResultCode(e), null);
                } catch (RemoteException exception) {
                    loge("getRulesAuthTable callback failure.", exception);
                }
            }
        };

        port.getRulesAuthTable(cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void getEuiccChallenge(String callingPackage, String cardId,
            IGetEuiccChallengeCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("getEuiccChallenge callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<byte[]> cardCb = new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK, result);
                } catch (RemoteException exception) {
                    loge("getEuiccChallenge callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("getEuiccChallenge callback onException: ", e);
                    callback.onComplete(getResultCode(e), null);
                } catch (RemoteException exception) {
                    loge("getEuiccChallenge callback failure.", exception);
                }
            }
        };

        port.getEuiccChallenge(cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void getEuiccInfo1(String callingPackage, String cardId,
            IGetEuiccInfo1Callback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("getEuiccInfo1 callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<byte[]> cardCb = new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK, result);
                } catch (RemoteException exception) {
                    loge("getEuiccInfo1 callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("getEuiccInfo1 callback onException: ", e);
                    callback.onComplete(getResultCode(e), null);
                } catch (RemoteException exception) {
                    loge("getEuiccInfo1 callback failure.", exception);
                }
            }
        };

        port.getEuiccInfo1(cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void getEuiccInfo2(String callingPackage, String cardId,
            IGetEuiccInfo2Callback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("getEuiccInfo2 callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<byte[]> cardCb = new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK, result);
                } catch (RemoteException exception) {
                    loge("getEuiccInfo2 callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("getEuiccInfo2 callback onException: ", e);
                    callback.onComplete(getResultCode(e), null);
                } catch (RemoteException exception) {
                    loge("getEuiccInfo2 callback failure.", exception);
                }
            }
        };

        port.getEuiccInfo2(cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void authenticateServer(String callingPackage, String cardId, String matchingId,
            byte[] serverSigned1, byte[] serverSignature1, byte[] euiccCiPkIdToBeUsed,
            byte[] serverCertificate, IAuthenticateServerCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("authenticateServer callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<byte[]> cardCb = new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK, result);
                } catch (RemoteException exception) {
                    loge("authenticateServer callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("authenticateServer callback onException: ", e);
                    callback.onComplete(getResultCode(e), null);
                } catch (RemoteException exception) {
                    loge("authenticateServer callback failure.", exception);
                }
            }
        };

        port.authenticateServer(matchingId, serverSigned1, serverSignature1, euiccCiPkIdToBeUsed,
                serverCertificate, cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void prepareDownload(String callingPackage, String cardId, @Nullable byte[] hashCc,
            byte[] smdpSigned2, byte[] smdpSignature2, byte[] smdpCertificate,
            IPrepareDownloadCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("prepareDownload callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<byte[]> cardCb = new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK, result);
                } catch (RemoteException exception) {
                    loge("prepareDownload callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("prepareDownload callback onException: ", e);
                    callback.onComplete(getResultCode(e), null);
                } catch (RemoteException exception) {
                    loge("prepareDownload callback failure.", exception);
                }
            }
        };

        port.prepareDownload(hashCc, smdpSigned2, smdpSignature2, smdpCertificate, cardCb,
                mEuiccMainThreadHandler);
    }

    @Override
    public void loadBoundProfilePackage(String callingPackage, String cardId,
            byte[] boundProfilePackage, ILoadBoundProfilePackageCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("loadBoundProfilePackage callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<byte[]> cardCb = new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] result) {
                Log.i(TAG, "Request subscription info list refresh after install.");
                SubscriptionController.getInstance()
                        .requestEmbeddedSubscriptionInfoListRefresh(
                                mUiccController.convertToPublicCardId(cardId));
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK, result);
                } catch (RemoteException exception) {
                    loge("loadBoundProfilePackage callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("loadBoundProfilePackage callback onException: ", e);
                    callback.onComplete(getResultCode(e), null);
                } catch (RemoteException exception) {
                    loge("loadBoundProfilePackage callback failure.", exception);
                }
            }
        };

        port.loadBoundProfilePackage(boundProfilePackage, cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void cancelSession(String callingPackage, String cardId, byte[] transactionId,
            @EuiccCardManager.CancelReason int reason, ICancelSessionCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("cancelSession callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<byte[]> cardCb = new AsyncResultCallback<byte[]>() {
            @Override
            public void onResult(byte[] result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK, result);
                } catch (RemoteException exception) {
                    loge("cancelSession callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("cancelSession callback onException: ", e);
                    callback.onComplete(getResultCode(e), null);
                } catch (RemoteException exception) {
                    loge("cancelSession callback failure.", exception);
                }
            }
        };

        port.cancelSession(transactionId, reason, cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void listNotifications(String callingPackage, String cardId,
            @EuiccNotification.Event int events, IListNotificationsCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("listNotifications callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<EuiccNotification[]> cardCb =
                new AsyncResultCallback<EuiccNotification[]>() {
            @Override
            public void onResult(EuiccNotification[] result) {
                try {
                    callback.onComplete(EuiccCardManager.RESULT_OK, result);
                } catch (RemoteException exception) {
                    loge("listNotifications callback failure.", exception);
                }
            }

            @Override
            public void onException(Throwable e) {
                try {
                    loge("listNotifications callback onException: ", e);
                    callback.onComplete(getResultCode(e), null);
                } catch (RemoteException exception) {
                    loge("listNotifications callback failure.", exception);
                }
            }
        };

        port.listNotifications(events, cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void retrieveNotificationList(String callingPackage, String cardId,
            @EuiccNotification.Event int events, IRetrieveNotificationListCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("retrieveNotificationList callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<EuiccNotification[]> cardCb =
                new AsyncResultCallback<EuiccNotification[]>() {
                    @Override
                    public void onResult(EuiccNotification[] result) {
                        try {
                            callback.onComplete(EuiccCardManager.RESULT_OK, result);
                        } catch (RemoteException exception) {
                            loge("retrieveNotificationList callback failure.", exception);
                        }
                    }

                    @Override
                    public void onException(Throwable e) {
                        try {
                            loge("retrieveNotificationList callback onException: ", e);
                            callback.onComplete(getResultCode(e), null);
                        } catch (RemoteException exception) {
                            loge("retrieveNotificationList callback failure.", exception);
                        }
                    }
                };

        port.retrieveNotificationList(events, cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void retrieveNotification(String callingPackage, String cardId, int seqNumber,
            IRetrieveNotificationCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED, null);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND, null);
            } catch (RemoteException exception) {
                loge("retrieveNotification callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<EuiccNotification> cardCb =
                new AsyncResultCallback<EuiccNotification>() {
                    @Override
                    public void onResult(EuiccNotification result) {
                        try {
                            callback.onComplete(EuiccCardManager.RESULT_OK, result);
                        } catch (RemoteException exception) {
                            loge("retrieveNotification callback failure.", exception);
                        }
                    }

                    @Override
                    public void onException(Throwable e) {
                        try {
                            loge("retrieveNotification callback onException: ", e);
                            callback.onComplete(getResultCode(e), null);
                        } catch (RemoteException exception) {
                            loge("retrieveNotification callback failure.", exception);
                        }
                    }
                };

        port.retrieveNotification(seqNumber, cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void removeNotificationFromList(String callingPackage, String cardId, int seqNumber,
            IRemoveNotificationFromListCallback callback) {
        try {
            checkCallingPackage(callingPackage);
        } catch (SecurityException se) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_CALLER_NOT_ALLOWED);
            } catch (RemoteException re) {
                loge("callback onComplete failure after checkCallingPackage.", re);
            }
            return;
        }

        EuiccPort port = getFirstActiveEuiccPort(cardId);
        if (port == null) {
            try {
                callback.onComplete(EuiccCardManager.RESULT_EUICC_NOT_FOUND);
            } catch (RemoteException exception) {
                loge("removeNotificationFromList callback failure.", exception);
            }
            return;
        }

        AsyncResultCallback<Void> cardCb = new AsyncResultCallback<Void>() {
                    @Override
                    public void onResult(Void result) {
                        try {
                            callback.onComplete(EuiccCardManager.RESULT_OK);
                        } catch (RemoteException exception) {
                            loge("removeNotificationFromList callback failure.", exception);
                        }

                    }

                    @Override
                    public void onException(Throwable e) {
                        try {
                            loge("removeNotificationFromList callback onException: ", e);
                            callback.onComplete(getResultCode(e));
                        } catch (RemoteException exception) {
                            loge("removeNotificationFromList callback failure.", exception);
                        }
                    }
                };

        port.removeNotificationFromList(seqNumber, cardCb, mEuiccMainThreadHandler);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.DUMP, "Requires DUMP");
        final long token = Binder.clearCallingIdentity();

        super.dump(fd, pw, args);
        // TODO(b/38206971): dump more information.
        pw.println("mCallingPackage=" + mCallingPackage);
        pw.println("mBestComponent=" + mBestComponent);

        Binder.restoreCallingIdentity(token);
    }

    private static void loge(String message) {
        Log.e(TAG, message);
    }

    private static void loge(String message, Throwable tr) {
        Log.e(TAG, message, tr);
    }
}
