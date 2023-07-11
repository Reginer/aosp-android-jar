/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.ims.rcs.uce.eab;

import static android.telephony.ims.RcsContactUceCapability.CAPABILITY_MECHANISM_OPTIONS;
import static android.telephony.ims.RcsContactUceCapability.CAPABILITY_MECHANISM_PRESENCE;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsRcsManager;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.aidl.IRcsUceControllerCallback;
import android.util.Log;

import com.android.ims.rcs.uce.UceController;

import java.util.ArrayList;
import java.util.List;

public final class EabBulkCapabilityUpdater {
    private final String TAG = this.getClass().getSimpleName();

    private static final Uri USER_EAB_SETTING = Uri.withAppendedPath(Telephony.SimInfo.CONTENT_URI,
            Telephony.SimInfo.COLUMN_IMS_RCS_UCE_ENABLED);
    private static final int NUM_SECS_IN_DAY = 86400;

    private final int mSubId;
    private final Context mContext;
    private final Handler mHandler;

    private final AlarmManager.OnAlarmListener mCapabilityExpiredListener;
    private final ContactChangedListener mContactProviderListener;
    private final EabSettingsListener mEabSettingListener;
    private final EabControllerImpl mEabControllerImpl;
    private final EabContactSyncController mEabContactSyncController;

    private UceController.UceControllerCallback mUceControllerCallback;
    private List<Uri> mRefreshContactList;

    private boolean mIsContactProviderListenerRegistered = false;
    private boolean mIsEabSettingListenerRegistered = false;
    private boolean mIsCarrierConfigListenerRegistered = false;
    private boolean mIsCarrierConfigEnabled = false;

    /**
     * Listen capability expired intent. Only registered when
     * {@link CarrierConfigManager.Ims#KEY_RCS_BULK_CAPABILITY_EXCHANGE_BOOL} has enabled bulk
     * capability exchange.
     */
    private class CapabilityExpiredListener implements AlarmManager.OnAlarmListener {
        @Override
        public void onAlarm() {
            Log.d(TAG, "Capability expired.");
            try {
                List<Uri> expiredContactList = getExpiredContactList();
                if (expiredContactList.size() > 0) {
                    mUceControllerCallback.refreshCapabilities(
                            getExpiredContactList(),
                            mRcsUceControllerCallback);
                } else {
                    Log.d(TAG, "expiredContactList is empty.");
                }
            } catch (RemoteException e) {
                Log.e(TAG, "CapabilityExpiredListener RemoteException", e);
            }
        }
    }

    /**
     * Listen contact provider change. Only registered when
     * {@link CarrierConfigManager.Ims#KEY_RCS_BULK_CAPABILITY_EXCHANGE_BOOL} has enabled bulk
     * capability exchange.
     */
    private class ContactChangedListener extends ContentObserver {
        public ContactChangedListener(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "Contact changed");
            syncContactAndRefreshCapabilities();
        }
    }

    /**
     * Listen EAB settings change. Only registered when
     * {@link CarrierConfigManager.Ims#KEY_RCS_BULK_CAPABILITY_EXCHANGE_BOOL} has enabled bulk
     * capability exchange.
     */
    private class EabSettingsListener extends ContentObserver {
        public EabSettingsListener(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            boolean isUserEnableUce = isUserEnableUce();
            Log.d(TAG, "EAB user setting changed: " + isUserEnableUce);
            if (isUserEnableUce) {
                mHandler.post(new SyncContactRunnable());
            } else {
                unRegisterContactProviderListener();
                cancelTimeAlert(mContext);
            }
        }
    }

    private IRcsUceControllerCallback mRcsUceControllerCallback = new IRcsUceControllerCallback() {
        @Override
        public void onCapabilitiesReceived(List<RcsContactUceCapability> contactCapabilities) {
            Log.d(TAG, "onCapabilitiesReceived");
            mEabControllerImpl.saveCapabilities(contactCapabilities);
        }

        @Override
        public void onComplete() {
            Log.d(TAG, "onComplete");
        }

        @Override
        public void onError(int errorCode, long retryAfterMilliseconds) {
            Log.d(TAG, "Refresh capabilities failed. Error code: " + errorCode
                    + ", retryAfterMilliseconds: " + retryAfterMilliseconds);
            if (retryAfterMilliseconds != 0) {
                mHandler.postDelayed(new retryRunnable(), retryAfterMilliseconds);
            }
        }

        @Override
        public IBinder asBinder() {
            return null;
        }
    };

    private class SyncContactRunnable implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "Sync contact from contact provider");
            syncContactAndRefreshCapabilities();
            registerContactProviderListener();
            registerEabUserSettingsListener();
        }
    }

    /**
     * Re-refresh capability if error happened.
     */
    private class retryRunnable implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "Retry refreshCapabilities()");

            try {
                mUceControllerCallback.refreshCapabilities(
                        mRefreshContactList, mRcsUceControllerCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "refreshCapabilities RemoteException" , e);
            }
        }
    }

    public EabBulkCapabilityUpdater(Context context,
            int subId,
            EabControllerImpl eabControllerImpl,
            EabContactSyncController eabContactSyncController,
            UceController.UceControllerCallback uceControllerCallback,
            Handler handler) {
        mContext = context;
        mSubId = subId;
        mEabControllerImpl = eabControllerImpl;
        mEabContactSyncController = eabContactSyncController;
        mUceControllerCallback = uceControllerCallback;

        mHandler = handler;
        mContactProviderListener = new ContactChangedListener(mHandler);
        mEabSettingListener = new EabSettingsListener(mHandler);
        mCapabilityExpiredListener = new CapabilityExpiredListener();

        Log.d(TAG, "create EabBulkCapabilityUpdater() subId: " + mSubId);

        enableBulkCapability();
        updateExpiredTimeAlert();
    }

    private void enableBulkCapability() {
        boolean isUserEnableUce = isUserEnableUce();
        boolean isSupportBulkCapabilityExchange = getBooleanCarrierConfig(
                CarrierConfigManager.Ims.KEY_RCS_BULK_CAPABILITY_EXCHANGE_BOOL, mSubId);

        Log.d(TAG, "isUserEnableUce: " + isUserEnableUce
                + ", isSupportBulkCapabilityExchange: " + isSupportBulkCapabilityExchange);

        if (isUserEnableUce && isSupportBulkCapabilityExchange) {
            mHandler.post(new SyncContactRunnable());
            mIsCarrierConfigEnabled = true;
        } else if (!isUserEnableUce && isSupportBulkCapabilityExchange) {
            registerEabUserSettingsListener();
            mIsCarrierConfigEnabled = false;
        } else {
            Log.d(TAG, "Not support bulk capability exchange.");
        }
    }

    private void syncContactAndRefreshCapabilities() {
        mRefreshContactList = mEabContactSyncController.syncContactToEabProvider(mContext);
        Log.d(TAG, "refresh contacts number: " + mRefreshContactList.size());

        if (mUceControllerCallback == null) {
            Log.d(TAG, "mUceControllerCallback is null.");
            return;
        }

        try {
            if (mRefreshContactList.size() > 0) {
                mUceControllerCallback.refreshCapabilities(
                        mRefreshContactList, mRcsUceControllerCallback);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "mUceControllerCallback RemoteException.", e);
        }
    }

    protected void updateExpiredTimeAlert() {
        boolean isUserEnableUce = isUserEnableUce();
        boolean isSupportBulkCapabilityExchange = getBooleanCarrierConfig(
                CarrierConfigManager.Ims.KEY_RCS_BULK_CAPABILITY_EXCHANGE_BOOL, mSubId);

        Log.d(TAG, " updateExpiredTimeAlert(), isUserEnableUce: " + isUserEnableUce
                + ", isSupportBulkCapabilityExchange: " + isSupportBulkCapabilityExchange);

        if (isUserEnableUce && isSupportBulkCapabilityExchange) {
            long expiredTimestamp = getLeastExpiredTimestamp();
            if (expiredTimestamp == Long.MAX_VALUE) {
                Log.d(TAG, "Can't find min timestamp in eab provider");
                return;
            }
            expiredTimestamp += mEabControllerImpl.getCapabilityCacheExpiration(mSubId);
            Log.d(TAG, "set time alert at " + expiredTimestamp);
            cancelTimeAlert(mContext);
            setTimeAlert(mContext, expiredTimestamp);
        }
    }

    private long getLeastExpiredTimestamp() {
        String selection = "("
                // Query presence timestamp
                + EabProvider.EabCommonColumns.MECHANISM + "=" + CAPABILITY_MECHANISM_PRESENCE
                + " AND "
                + EabProvider.PresenceTupleColumns.REQUEST_TIMESTAMP + " IS NOT NULL) "

                // Query options timestamp
                + " OR " + "(" + EabProvider.EabCommonColumns.MECHANISM + "="
                + CAPABILITY_MECHANISM_OPTIONS + " AND "
                + EabProvider.OptionsColumns.REQUEST_TIMESTAMP + " IS NOT NULL) "

                // filter by sub id
                + " AND " + EabProvider.EabCommonColumns.SUBSCRIPTION_ID + "=" + mSubId

                // filter the contact that not come from contact provider
                + " AND " + EabProvider.ContactColumns.RAW_CONTACT_ID + " IS NOT NULL "
                + " AND " + EabProvider.ContactColumns.DATA_ID + " IS NOT NULL ";

        long minTimestamp = Long.MAX_VALUE;
        Cursor result = mContext.getContentResolver().query(EabProvider.ALL_DATA_URI, null,
                selection,
                null, null);

        if (result != null) {
            while (result.moveToNext()) {
                int mechanism = result.getInt(
                        result.getColumnIndex(EabProvider.EabCommonColumns.MECHANISM));
                long timestamp;
                if (mechanism == CAPABILITY_MECHANISM_PRESENCE) {
                    timestamp = result.getLong(result.getColumnIndex(
                            EabProvider.PresenceTupleColumns.REQUEST_TIMESTAMP));
                } else {
                    timestamp = result.getLong(result.getColumnIndex(
                            EabProvider.OptionsColumns.REQUEST_TIMESTAMP));
                }

                if (timestamp < minTimestamp) {
                    minTimestamp = timestamp;
                }
            }
            result.close();
        } else {
            Log.d(TAG, "getLeastExpiredTimestamp() cursor is null");
        }
        return minTimestamp;
    }

    private void setTimeAlert(Context context, long wakeupTimeMs) {
        AlarmManager am = context.getSystemService(AlarmManager.class);

        // To prevent all devices from sending requests to the server at the same time, add a jitter
        // time (0 sec ~ 2 days) randomly.
        int jitterTimeSec = (int) (Math.random() * (NUM_SECS_IN_DAY * 2));
        Log.d(TAG, " setTimeAlert: " + wakeupTimeMs + ", jitterTimeSec: " + jitterTimeSec);
        am.set(AlarmManager.RTC_WAKEUP,
                (wakeupTimeMs * 1000) + jitterTimeSec,
                TAG,
                mCapabilityExpiredListener,
                mHandler);
    }

    private void cancelTimeAlert(Context context) {
        Log.d(TAG, "cancelTimeAlert.");
        AlarmManager am = context.getSystemService(AlarmManager.class);
        am.cancel(mCapabilityExpiredListener);
    }

    private boolean getBooleanCarrierConfig(String key, int subId) {
        CarrierConfigManager mConfigManager = mContext.getSystemService(CarrierConfigManager.class);
        PersistableBundle b = null;
        if (mConfigManager != null) {
            b = mConfigManager.getConfigForSubId(subId);
        }
        if (b != null) {
            return b.getBoolean(key);
        } else {
            Log.w(TAG, "getConfigForSubId(subId) is null. Return the default value of " + key);
            return CarrierConfigManager.getDefaultConfig().getBoolean(key);
        }
    }

    private boolean isUserEnableUce() {
        ImsManager manager = mContext.getSystemService(ImsManager.class);
        if (manager == null) {
            Log.e(TAG, "ImsManager is null");
            return false;
        }
        try {
            ImsRcsManager rcsManager = manager.getImsRcsManager(mSubId);
            return (rcsManager != null) && rcsManager.getUceAdapter().isUceSettingEnabled();
        } catch (Exception e) {
            Log.e(TAG, "hasUserEnabledUce: exception = " + e.getMessage());
        }
        return false;
    }

    private List<Uri> getExpiredContactList() {
        List<Uri> refreshList = new ArrayList<>();
        long expiredTime = (System.currentTimeMillis() / 1000)
                + mEabControllerImpl.getCapabilityCacheExpiration(mSubId);
        String selection = "("
                + EabProvider.EabCommonColumns.MECHANISM + "=" + CAPABILITY_MECHANISM_PRESENCE
                + " AND " + EabProvider.PresenceTupleColumns.REQUEST_TIMESTAMP + "<"
                + expiredTime + ")";
        selection += " OR " + "(" + EabProvider.EabCommonColumns.MECHANISM + "="
                + CAPABILITY_MECHANISM_OPTIONS + " AND "
                + EabProvider.OptionsColumns.REQUEST_TIMESTAMP + "<" + expiredTime + ")";

        Cursor result = mContext.getContentResolver().query(EabProvider.ALL_DATA_URI, null,
                selection,
                null, null);
        while (result.moveToNext()) {
            String phoneNumber = result.getString(
                    result.getColumnIndex(EabProvider.ContactColumns.PHONE_NUMBER));
            refreshList.add(Uri.parse(phoneNumber));
        }
        result.close();
        return refreshList;
    }

    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        cancelTimeAlert(mContext);
        unRegisterContactProviderListener();
        unRegisterEabUserSettings();
    }

    private void registerContactProviderListener() {
        Log.d(TAG, "registerContactProviderListener");
        mIsContactProviderListenerRegistered = true;
        mContext.getContentResolver().registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                mContactProviderListener);
    }

    private void registerEabUserSettingsListener() {
        Log.d(TAG, "registerEabUserSettingsListener");
        mIsEabSettingListenerRegistered = true;
        mContext.getContentResolver().registerContentObserver(
                USER_EAB_SETTING,
                true,
                mEabSettingListener);
    }

    private void unRegisterContactProviderListener() {
        Log.d(TAG, "unRegisterContactProviderListener");
        if (mIsContactProviderListenerRegistered) {
            mIsContactProviderListenerRegistered = false;
            mContext.getContentResolver().unregisterContentObserver(mContactProviderListener);
        }
    }

    private void unRegisterEabUserSettings() {
        Log.d(TAG, "unRegisterEabUserSettings");
        if (mIsEabSettingListenerRegistered) {
            mIsEabSettingListenerRegistered = false;
            mContext.getContentResolver().unregisterContentObserver(mEabSettingListener);
        }
    }

    public void setUceRequestCallback(UceController.UceControllerCallback uceControllerCallback) {
        mUceControllerCallback = uceControllerCallback;
    }

    public void onCarrierConfigChanged() {
        boolean isSupportBulkCapabilityExchange = getBooleanCarrierConfig(
                CarrierConfigManager.Ims.KEY_RCS_BULK_CAPABILITY_EXCHANGE_BOOL, mSubId);

        Log.d(TAG, "Carrier config changed. "
                + "isCarrierConfigEnabled: " + mIsCarrierConfigEnabled
                + ", isSupportBulkCapabilityExchange: " + isSupportBulkCapabilityExchange);
        if (!mIsCarrierConfigEnabled && isSupportBulkCapabilityExchange) {
            enableBulkCapability();
            updateExpiredTimeAlert();
            mIsCarrierConfigEnabled = true;
        } else if (mIsCarrierConfigEnabled && !isSupportBulkCapabilityExchange) {
            onDestroy();
        }
    }
}
