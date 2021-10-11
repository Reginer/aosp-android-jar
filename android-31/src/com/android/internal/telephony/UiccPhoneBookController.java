/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (c) 2011-2013, 2021 The Linux Foundation. All rights reserved.
 * Not a Contribution.
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

import android.compat.annotation.UnsupportedAppUsage;
import android.os.Build;
import android.os.TelephonyServiceManager.ServiceRegisterer;
import android.telephony.TelephonyFrameworkInitializer;
import android.content.ContentValues;

import com.android.internal.telephony.uicc.AdnCapacity;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.telephony.Rlog;

import java.util.List;

public class UiccPhoneBookController extends IIccPhoneBook.Stub {
    private static final String TAG = "UiccPhoneBookController";

    /* only one UiccPhoneBookController exists */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public UiccPhoneBookController() {
        ServiceRegisterer iccPhoneBookServiceRegisterer = TelephonyFrameworkInitializer
                .getTelephonyServiceManager()
                .getIccPhoneBookServiceRegisterer();
        if (iccPhoneBookServiceRegisterer.get() == null) {
            iccPhoneBookServiceRegisterer.register(this);
        }
    }

    @Override
    public boolean
    updateAdnRecordsInEfBySearch (int efid, String oldTag, String oldPhoneNumber,
            String newTag, String newPhoneNumber, String pin2) throws android.os.RemoteException {
        ContentValues values = new ContentValues();
        values.put(IccProvider.STR_TAG, oldTag);
        values.put(IccProvider.STR_NUMBER, oldPhoneNumber);
        values.put(IccProvider.STR_NEW_TAG, newTag);
        values.put(IccProvider.STR_NEW_NUMBER, newPhoneNumber);
        return updateAdnRecordsInEfBySearchForSubscriber(getDefaultSubscription(),
                efid, values, pin2);
    }

    @Override
    public boolean
    updateAdnRecordsInEfByIndexForSubscriber(int subId, int efid, ContentValues values,
            int index, String pin2) throws android.os.RemoteException {
        IccPhoneBookInterfaceManager iccPbkIntMgr =
                             getIccPhoneBookInterfaceManager(subId);
        if (iccPbkIntMgr != null) {
            return iccPbkIntMgr.updateAdnRecordsInEfByIndex(efid, values,
                    index, pin2);
        } else {
            Rlog.e(TAG,"updateAdnRecordsInEfByIndex iccPbkIntMgr is" +
                      " null for Subscription:"+subId);
            return false;
        }
    }

    @Override
    public int[] getAdnRecordsSize(int efid) throws android.os.RemoteException {
        return getAdnRecordsSizeForSubscriber(getDefaultSubscription(), efid);
    }

    @Override
    public int[]
    getAdnRecordsSizeForSubscriber(int subId, int efid) throws android.os.RemoteException {
        IccPhoneBookInterfaceManager iccPbkIntMgr =
                             getIccPhoneBookInterfaceManager(subId);
        if (iccPbkIntMgr != null) {
            return iccPbkIntMgr.getAdnRecordsSize(efid);
        } else {
            Rlog.e(TAG,"getAdnRecordsSize iccPbkIntMgr is" +
                      " null for Subscription:"+subId);
            return null;
        }
    }

    @Override
    public List<AdnRecord> getAdnRecordsInEf(int efid) throws android.os.RemoteException {
        return getAdnRecordsInEfForSubscriber(getDefaultSubscription(), efid);
    }

    @Override
    public List<AdnRecord> getAdnRecordsInEfForSubscriber(int subId, int efid)
           throws android.os.RemoteException {
        IccPhoneBookInterfaceManager iccPbkIntMgr =
                             getIccPhoneBookInterfaceManager(subId);
        if (iccPbkIntMgr != null) {
            return iccPbkIntMgr.getAdnRecordsInEf(efid);
        } else {
            Rlog.e(TAG,"getAdnRecordsInEf iccPbkIntMgr is" +
                      "null for Subscription:"+subId);
            return null;
        }
    }

    @Override
    public AdnCapacity getAdnRecordsCapacityForSubscriber(int subId)
           throws android.os.RemoteException {
        IccPhoneBookInterfaceManager iccPbkIntMgr = getIccPhoneBookInterfaceManager(subId);
        if (iccPbkIntMgr != null) {
            return iccPbkIntMgr.getAdnRecordsCapacity();
        } else {
            Rlog.e(TAG, "getAdnRecordsCapacity iccPbkIntMgr is null for Subscription:" + subId);
            return null;
        }
    }

    @Override
    public boolean
    updateAdnRecordsInEfBySearchForSubscriber(int subId, int efid,
            ContentValues values, String pin2)
            throws android.os.RemoteException {
        IccPhoneBookInterfaceManager iccPbkIntMgr = getIccPhoneBookInterfaceManager(subId);
        if (iccPbkIntMgr != null) {
            return iccPbkIntMgr.updateAdnRecordsInEfBySearchForSubscriber(
                efid, values, pin2);
        } else {
            Rlog.e(TAG,"updateAdnRecordsInEfBySearchForSubscriber " +
                "iccPbkIntMgr is null for Subscription:"+subId);
            return false;
        }
    }

    /**
     * get phone book interface manager object based on subscription.
     **/
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private IccPhoneBookInterfaceManager
            getIccPhoneBookInterfaceManager(int subId) {

        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        try {
            return PhoneFactory.getPhone(phoneId).getIccPhoneBookInterfaceManager();
        } catch (NullPointerException e) {
            Rlog.e(TAG, "Exception is :"+e.toString()+" For subscription :"+subId );
            e.printStackTrace(); //To print stack trace
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            Rlog.e(TAG, "Exception is :"+e.toString()+" For subscription :"+subId );
            e.printStackTrace();
            return null;
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int getDefaultSubscription() {
        return PhoneFactory.getDefaultSubscription();
    }
}
