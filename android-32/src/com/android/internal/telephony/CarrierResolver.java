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
package com.android.internal.telephony;

import static android.provider.Telephony.CarrierId;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;
import android.service.carrier.CarrierIdentifier;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.metrics.CarrierIdMatchStats;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CarrierResolver identifies the subscription carrier and returns a canonical carrier Id
 * and a user friendly carrier name. CarrierResolver reads subscription info and check against
 * all carrier matching rules stored in CarrierIdProvider. It is msim aware, each phone has a
 * dedicated CarrierResolver.
 */
public class CarrierResolver extends Handler {
    private static final String LOG_TAG = CarrierResolver.class.getSimpleName();
    private static final boolean DBG = true;
    private static final boolean VDBG = Rlog.isLoggable(LOG_TAG, Log.VERBOSE);

    // events to trigger carrier identification
    private static final int SIM_LOAD_EVENT             = 1;
    private static final int ICC_CHANGED_EVENT          = 2;
    private static final int PREFER_APN_UPDATE_EVENT    = 3;
    private static final int CARRIER_ID_DB_UPDATE_EVENT = 4;

    private static final Uri CONTENT_URL_PREFER_APN = Uri.withAppendedPath(
            Telephony.Carriers.CONTENT_URI, "preferapn");

    // Test purpose only.
    private static final String TEST_ACTION = "com.android.internal.telephony"
            + ".ACTION_TEST_OVERRIDE_CARRIER_ID";

    // cached version of the carrier list, so that we don't need to re-query it every time.
    private Integer mCarrierListVersion;
    // cached matching rules based mccmnc to speed up resolution
    private List<CarrierMatchingRule> mCarrierMatchingRulesOnMccMnc = new ArrayList<>();
    // cached carrier Id
    private int mCarrierId = TelephonyManager.UNKNOWN_CARRIER_ID;
    // cached specific carrier Id
    private int mSpecificCarrierId = TelephonyManager.UNKNOWN_CARRIER_ID;
    // cached MNO carrier Id. mno carrier shares the same mccmnc as cid and can be solely
    // identified by mccmnc only. If there is no such mno carrier, mno carrier id equals to
    // the cid.
    private int mMnoCarrierId = TelephonyManager.UNKNOWN_CARRIER_ID;
    // cached carrier name
    private String mCarrierName;
    private String mSpecificCarrierName;
    // cached preferapn name
    private String mPreferApn;
    // override for testing purpose
    private String mTestOverrideApn;
    private String mTestOverrideCarrierPriviledgeRule;
    // cached service provider name. telephonyManager API returns empty string as default value.
    // some carriers need to target devices with Empty SPN. In that case, carrier matching rule
    // should specify "" spn explicitly.
    private String mSpn = "";

    private Context mContext;
    private Phone mPhone;
    private IccRecords mIccRecords;
    private final LocalLog mCarrierIdLocalLog = new LocalLog(20);
    private final TelephonyManager mTelephonyMgr;

    private final ContentObserver mContentObserver = new ContentObserver(this) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (Telephony.Carriers.CONTENT_URI.equals(uri)) {
                logd("onChange URI: " + uri);
                sendEmptyMessage(PREFER_APN_UPDATE_EVENT);
            } else if (CarrierId.All.CONTENT_URI.equals(uri)) {
                logd("onChange URI: " + uri);
                sendEmptyMessage(CARRIER_ID_DB_UPDATE_EVENT);
            }
        }
    };

    /**
     * A broadcast receiver used for overriding carrier id for testing. There are six parameters,
     * only override_carrier_id is required, the others are options.
     *
     * To override carrier id by adb command, e.g.:
     * adb shell am broadcast -a com.android.internal.telephony.ACTION_TEST_OVERRIDE_CARRIER_ID \
     * --ei override_carrier_id 1
     * --ei override_specific_carrier_id 1
     * --ei override_mno_carrier_id 1
     * --es override_carrier_name test
     * --es override_specific_carrier_name test
     * --ei sub_id 1
     */
    private final BroadcastReceiver mCarrierIdTestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int phoneId = mPhone.getPhoneId();
            int carrierId = intent.getIntExtra("override_carrier_id",
                    TelephonyManager.UNKNOWN_CARRIER_ID);
            int specificCarrierId = intent.getIntExtra("override_specific_carrier_id", carrierId);
            int mnoCarrierId = intent.getIntExtra("override_mno_carrier_id", carrierId);
            String carrierName = intent.getStringExtra("override_carrier_name");
            String specificCarrierName = intent.getStringExtra("override_specific_carrier_name");
            int subId = intent.getIntExtra("sub_id",
                    SubscriptionManager.getDefaultSubscriptionId());

            if (carrierId <= 0) {
                logd("Override carrier id must be greater than 0.", phoneId);
                return;
            } else if (subId != mPhone.getSubId()) {
                logd("Override carrier id failed. The sub id doesn't same as phone's sub id.",
                        phoneId);
                return;
            } else {
                logd("Override carrier id to: " + carrierId, phoneId);
                logd("Override specific carrier id to: " + specificCarrierId, phoneId);
                logd("Override mno carrier id to: " + mnoCarrierId, phoneId);
                logd("Override carrier name to: " + carrierName, phoneId);
                logd("Override specific carrier name to: " + specificCarrierName, phoneId);
                updateCarrierIdAndName(
                    carrierId, carrierName != null ? carrierName : "",
                    specificCarrierId, specificCarrierName != null ? carrierName : "",
                    mnoCarrierId, false);
            }
        }
    };

    public CarrierResolver(Phone phone) {
        logd("Creating CarrierResolver[" + phone.getPhoneId() + "]");
        mContext = phone.getContext();
        mPhone = phone;
        mTelephonyMgr = TelephonyManager.from(mContext);

        // register events
        mContext.getContentResolver().registerContentObserver(CONTENT_URL_PREFER_APN, false,
                mContentObserver);
        mContext.getContentResolver().registerContentObserver(
                CarrierId.All.CONTENT_URI, false, mContentObserver);
        UiccController.getInstance().registerForIccChanged(this, ICC_CHANGED_EVENT, null);

        if (TelephonyUtils.IS_DEBUGGABLE) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(TEST_ACTION);
            mContext.registerReceiver(mCarrierIdTestReceiver, filter);
        }
    }

    /**
     * This is triggered from SubscriptionInfoUpdater after sim state change.
     * The sequence of sim loading would be
     *  1. ACTION_SUBINFO_CONTENT_CHANGE
     *  2. ACTION_SIM_STATE_CHANGED/ACTION_SIM_CARD_STATE_CHANGED
     *  /ACTION_SIM_APPLICATION_STATE_CHANGED
     *  3. ACTION_SUBSCRIPTION_CARRIER_IDENTITY_CHANGED
     *
     *  For SIM refresh either reset or init refresh type, SubscriptionInfoUpdater will re-trigger
     *  carrier identification with sim loaded state. Framework today silently handle single file
     *  refresh type.
     *  TODO: check fileId from single file refresh, if the refresh file is IMSI, gid1 or other
     *  records which might change carrier id, framework should trigger sim loaded state just like
     *  other refresh events: INIT or RESET and which will ultimately trigger carrier
     *  re-identification.
     */
    public void resolveSubscriptionCarrierId(String simState) {
        logd("[resolveSubscriptionCarrierId] simState: " + simState);
        switch (simState) {
            case IccCardConstants.INTENT_VALUE_ICC_ABSENT:
            case IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR:
                // only clear carrier id on absent to avoid transition to unknown carrier id during
                // intermediate states of sim refresh
                handleSimAbsent();
                break;
            case IccCardConstants.INTENT_VALUE_ICC_LOADED:
                handleSimLoaded(false);
                break;
        }
    }

    private void handleSimLoaded(boolean isSimOverride) {
        if (mIccRecords != null) {
            /**
             * returns empty string to be consistent with
             * {@link TelephonyManager#getSimOperatorName()}
             */
            mSpn = (mIccRecords.getServiceProviderName() == null) ? ""
                    : mIccRecords.getServiceProviderName();
        } else {
            loge("mIccRecords is null on SIM_LOAD_EVENT, could not get SPN");
        }
        mPreferApn = getPreferApn();
        loadCarrierMatchingRulesOnMccMnc(
                false /* update carrier config */,
                isSimOverride);
    }

    private void handleSimAbsent() {
        mCarrierMatchingRulesOnMccMnc.clear();
        mSpn = null;
        mPreferApn = null;
        updateCarrierIdAndName(TelephonyManager.UNKNOWN_CARRIER_ID, null,
                TelephonyManager.UNKNOWN_CARRIER_ID, null,
                TelephonyManager.UNKNOWN_CARRIER_ID, false);
    }

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {
        }
    };

    /**
     * Entry point for the carrier identification.
     *
     *    1. SIM_LOAD_EVENT
     *        This indicates that all SIM records has been loaded and its first entry point for the
     *        carrier identification. Note, there are other attributes could be changed on the fly
     *        like APN. We cached all carrier matching rules based on MCCMNC to speed
     *        up carrier resolution on following trigger events.
     *
     *    2. PREFER_APN_UPDATE_EVENT
     *        This indicates prefer apn has been changed. It could be triggered when user modified
     *        APN settings or when default data connection first establishes on the current carrier.
     *        We follow up on this by querying prefer apn sqlite and re-issue carrier identification
     *        with the updated prefer apn name.
     *
     *    3. CARRIER_ID_DB_UPDATE_EVENT
     *        This indicates that carrierIdentification database which stores all matching rules
     *        has been updated. It could be triggered from OTA or assets update.
     */
    @Override
    public void handleMessage(Message msg) {
        if (DBG) logd("handleMessage: " + msg.what);
        switch (msg.what) {
            case SIM_LOAD_EVENT:
                AsyncResult result = (AsyncResult) msg.obj;
                boolean isSimOverride = false;
                if (result != null) {
                    isSimOverride = result.userObj instanceof Boolean && (Boolean) result.userObj;
                }
                handleSimLoaded(isSimOverride);
                break;
            case CARRIER_ID_DB_UPDATE_EVENT:
                // clean the cached carrier list version, so that a new one will be queried.
                mCarrierListVersion = null;
                loadCarrierMatchingRulesOnMccMnc(true /* update carrier config*/, false);
                break;
            case PREFER_APN_UPDATE_EVENT:
                String preferApn = getPreferApn();
                if (!equals(mPreferApn, preferApn, true)) {
                    logd("[updatePreferApn] from:" + mPreferApn + " to:" + preferApn);
                    mPreferApn = preferApn;
                    matchSubscriptionCarrier(true /* update carrier config*/, false);
                }
                break;
            case ICC_CHANGED_EVENT:
                // all records used for carrier identification are from SimRecord.
                final IccRecords newIccRecords = UiccController.getInstance().getIccRecords(
                        mPhone.getPhoneId(), UiccController.APP_FAM_3GPP);
                if (mIccRecords != newIccRecords) {
                    if (mIccRecords != null) {
                        logd("Removing stale icc objects.");
                        mIccRecords.unregisterForRecordsOverride(this);
                        mIccRecords = null;
                    }
                    if (newIccRecords != null) {
                        logd("new Icc object");
                        newIccRecords.registerForRecordsOverride(this, SIM_LOAD_EVENT,
                                /* is sim override*/true);
                        mIccRecords = newIccRecords;
                    }
                }
                break;
            default:
                loge("invalid msg: " + msg.what);
                break;
        }
    }

    private void loadCarrierMatchingRulesOnMccMnc(
            boolean updateCarrierConfig,
            boolean isSimOverride) {
        try {
            String mccmnc = mTelephonyMgr.getSimOperatorNumericForPhone(mPhone.getPhoneId());
            Cursor cursor = mContext.getContentResolver().query(
                    CarrierId.All.CONTENT_URI,
                    /* projection */ null,
                    /* selection */ CarrierId.All.MCCMNC + "=?",
                    /* selectionArgs */ new String[]{mccmnc}, null);
            try {
                if (cursor != null) {
                    if (VDBG) {
                        logd("[loadCarrierMatchingRules]- " + cursor.getCount()
                                + " Records(s) in DB" + " mccmnc: " + mccmnc);
                    }
                    mCarrierMatchingRulesOnMccMnc.clear();
                    while (cursor.moveToNext()) {
                        mCarrierMatchingRulesOnMccMnc.add(makeCarrierMatchingRule(cursor));
                    }
                    matchSubscriptionCarrier(updateCarrierConfig, isSimOverride);

                    // Generate metrics related to carrier ID table version.
                    CarrierIdMatchStats.sendCarrierIdTableVersion(getCarrierListVersion());
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Exception ex) {
            loge("[loadCarrierMatchingRules]- ex: " + ex);
        }
    }

    private String getCarrierNameFromId(int cid) {
        try {
            Cursor cursor = mContext.getContentResolver().query(
                    CarrierId.All.CONTENT_URI,
                    /* projection */ null,
                    /* selection */ CarrierId.CARRIER_ID + "=?",
                    /* selectionArgs */ new String[]{cid + ""}, null);
            try {
                if (cursor != null) {
                    if (VDBG) {
                        logd("[getCarrierNameFromId]- " + cursor.getCount()
                                + " Records(s) in DB" + " cid: " + cid);
                    }
                    while (cursor.moveToNext()) {
                        return cursor.getString(cursor.getColumnIndex(CarrierId.CARRIER_NAME));
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Exception ex) {
            loge("[getCarrierNameFromId]- ex: " + ex);
        }
        return null;
    }

    private static List<CarrierMatchingRule> getCarrierMatchingRulesFromMccMnc(
            @NonNull Context context, String mccmnc) {
        List<CarrierMatchingRule> rules = new ArrayList<>();
        try {
            Cursor cursor = context.getContentResolver().query(
                    CarrierId.All.CONTENT_URI,
                    /* projection */ null,
                    /* selection */ CarrierId.All.MCCMNC + "=?",
                    /* selectionArgs */ new String[]{mccmnc}, null);
            try {
                if (cursor != null) {
                    if (VDBG) {
                        logd("[loadCarrierMatchingRules]- " + cursor.getCount()
                                + " Records(s) in DB" + " mccmnc: " + mccmnc);
                    }
                    rules.clear();
                    while (cursor.moveToNext()) {
                        rules.add(makeCarrierMatchingRule(cursor));
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Exception ex) {
            loge("[loadCarrierMatchingRules]- ex: " + ex);
        }
        return rules;
    }

    private String getPreferApn() {
        // return test overrides if present
        if (!TextUtils.isEmpty(mTestOverrideApn)) {
            logd("[getPreferApn]- " + mTestOverrideApn + " test override");
            return mTestOverrideApn;
        }
        Cursor cursor = mContext.getContentResolver().query(
                Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "preferapn/subId/"
                + mPhone.getSubId()), /* projection */ new String[]{Telephony.Carriers.APN},
                /* selection */ null, /* selectionArgs */ null, /* sortOrder */ null);
        try {
            if (cursor != null) {
                if (VDBG) {
                    logd("[getPreferApn]- " + cursor.getCount() + " Records(s) in DB");
                }
                while (cursor.moveToNext()) {
                    String apn = cursor.getString(cursor.getColumnIndexOrThrow(
                            Telephony.Carriers.APN));
                    logd("[getPreferApn]- " + apn);
                    return apn;
                }
            }
        } catch (Exception ex) {
            loge("[getPreferApn]- exception: " + ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private boolean isPreferApnUserEdited(@NonNull String preferApn) {
        try (Cursor cursor = mContext.getContentResolver().query(
                Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI,
                        "preferapn/subId/" + mPhone.getSubId()),
                /* projection */ new String[]{Telephony.Carriers.EDITED_STATUS},
                /* selection */ Telephony.Carriers.APN + "=?",
                /* selectionArgs */ new String[]{preferApn}, /* sortOrder */ null) ) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.EDITED_STATUS)) == Telephony.Carriers.USER_EDITED;
            }
        } catch (Exception ex) {
            loge("[isPreferApnUserEdited]- exception: " + ex);
        }
        return false;
    }

    public void setTestOverrideApn(String apn) {
        logd("[setTestOverrideApn]: " + apn);
        mTestOverrideApn = apn;
    }

    public void setTestOverrideCarrierPriviledgeRule(String rule) {
        logd("[setTestOverrideCarrierPriviledgeRule]: " + rule);
        mTestOverrideCarrierPriviledgeRule = rule;
    }

    private void updateCarrierIdAndName(int cid, String name,
                                        int specificCarrierId, String specificCarrierName,
                                        int mnoCid, boolean isSimOverride) {
        boolean update = false;
        if (specificCarrierId != mSpecificCarrierId) {
            logd("[updateSpecificCarrierId] from:" + mSpecificCarrierId + " to:"
                    + specificCarrierId);
            mSpecificCarrierId = specificCarrierId;
            update = true;
        }
        if (specificCarrierName != mSpecificCarrierName) {
            logd("[updateSpecificCarrierName] from:" + mSpecificCarrierName + " to:"
                    + specificCarrierName);
            mSpecificCarrierName = specificCarrierName;
            update = true;
        }
        if (update) {
            mCarrierIdLocalLog.log("[updateSpecificCarrierIdAndName] cid:"
                    + mSpecificCarrierId + " name:" + mSpecificCarrierName);
            final Intent intent = new Intent(TelephonyManager
                    .ACTION_SUBSCRIPTION_SPECIFIC_CARRIER_IDENTITY_CHANGED);
            intent.putExtra(TelephonyManager.EXTRA_SPECIFIC_CARRIER_ID, mSpecificCarrierId);
            intent.putExtra(TelephonyManager.EXTRA_SPECIFIC_CARRIER_NAME, mSpecificCarrierName);
            intent.putExtra(TelephonyManager.EXTRA_SUBSCRIPTION_ID, mPhone.getSubId());
            mContext.sendBroadcast(intent);

            // notify content observers for specific carrier id change event.
            ContentValues cv = new ContentValues();
            cv.put(CarrierId.SPECIFIC_CARRIER_ID, mSpecificCarrierId);
            cv.put(CarrierId.SPECIFIC_CARRIER_ID_NAME, mSpecificCarrierName);
            mContext.getContentResolver().update(
                    Telephony.CarrierId.getSpecificCarrierIdUriForSubscriptionId(mPhone.getSubId()),
                    cv, null, null);
        }

        update = false;
        if (!equals(name, mCarrierName, true)) {
            logd("[updateCarrierName] from:" + mCarrierName + " to:" + name);
            mCarrierName = name;
            update = true;
        }
        if (cid != mCarrierId) {
            logd("[updateCarrierId] from:" + mCarrierId + " to:" + cid);
            mCarrierId = cid;
            update = true;
        }
        if (mnoCid != mMnoCarrierId) {
            logd("[updateMnoCarrierId] from:" + mMnoCarrierId + " to:" + mnoCid);
            mMnoCarrierId = mnoCid;
            update = true;
        }
        if (update) {
            mCarrierIdLocalLog.log("[updateCarrierIdAndName] cid:" + mCarrierId + " name:"
                    + mCarrierName + " mnoCid:" + mMnoCarrierId);
            final Intent intent = new Intent(TelephonyManager
                    .ACTION_SUBSCRIPTION_CARRIER_IDENTITY_CHANGED);
            intent.putExtra(TelephonyManager.EXTRA_CARRIER_ID, mCarrierId);
            intent.putExtra(TelephonyManager.EXTRA_CARRIER_NAME, mCarrierName);
            intent.putExtra(TelephonyManager.EXTRA_SUBSCRIPTION_ID, mPhone.getSubId());
            mContext.sendBroadcast(intent);

            // notify content observers for carrier id change event
            ContentValues cv = new ContentValues();
            cv.put(CarrierId.CARRIER_ID, mCarrierId);
            cv.put(CarrierId.CARRIER_NAME, mCarrierName);
            mContext.getContentResolver().update(
                    Telephony.CarrierId.getUriForSubscriptionId(mPhone.getSubId()), cv, null, null);
        }
        // during esim profile switch, there is no sim absent thus carrier id will persist and
        // might not trigger an update if switch profiles for the same carrier. thus always update
        // subscriptioninfo db to make sure we have correct carrier id set.
        if (SubscriptionManager.isValidSubscriptionId(mPhone.getSubId()) && !isSimOverride) {
            // only persist carrier id to simInfo db when subId is valid.
            SubscriptionController.getInstance().setCarrierId(mCarrierId, mPhone.getSubId());
        }
    }

    private static CarrierMatchingRule makeCarrierMatchingRule(Cursor cursor) {
        String certs = cursor.getString(
                cursor.getColumnIndexOrThrow(CarrierId.All.PRIVILEGE_ACCESS_RULE));
        return new CarrierMatchingRule(
                cursor.getString(cursor.getColumnIndexOrThrow(CarrierId.All.MCCMNC)),
                cursor.getString(cursor.getColumnIndexOrThrow(
                        CarrierId.All.IMSI_PREFIX_XPATTERN)),
                cursor.getString(cursor.getColumnIndexOrThrow(
                        CarrierId.All.ICCID_PREFIX)),
                cursor.getString(cursor.getColumnIndexOrThrow(CarrierId.All.GID1)),
                cursor.getString(cursor.getColumnIndexOrThrow(CarrierId.All.GID2)),
                cursor.getString(cursor.getColumnIndexOrThrow(CarrierId.All.PLMN)),
                cursor.getString(cursor.getColumnIndexOrThrow(CarrierId.All.SPN)),
                cursor.getString(cursor.getColumnIndexOrThrow(CarrierId.All.APN)),
                (TextUtils.isEmpty(certs) ? null : new ArrayList<>(Arrays.asList(certs))),
                cursor.getInt(cursor.getColumnIndexOrThrow(CarrierId.CARRIER_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(CarrierId.CARRIER_NAME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(CarrierId.PARENT_CARRIER_ID)));
    }

    /**
     * carrier matching attributes with corresponding cid
     */
    public static class CarrierMatchingRule {
        /**
         * These scores provide the hierarchical relationship between the attributes, intended to
         * resolve conflicts in a deterministic way. The scores are constructed such that a match
         * from a higher tier will beat any subsequent match which does not match at that tier,
         * so MCCMNC beats everything else. This avoids problems when two (or more) carriers rule
         * matches as the score helps to find the best match uniquely. e.g.,
         * rule 1 {mccmnc, imsi} rule 2 {mccmnc, imsi, gid1} and rule 3 {mccmnc, imsi, gid2} all
         * matches with subscription data. rule 2 wins with the highest matching score.
         */
        private static final int SCORE_MCCMNC                   = 1 << 8;
        private static final int SCORE_IMSI_PREFIX              = 1 << 7;
        private static final int SCORE_ICCID_PREFIX             = 1 << 6;
        private static final int SCORE_GID1                     = 1 << 5;
        private static final int SCORE_GID2                     = 1 << 4;
        private static final int SCORE_PLMN                     = 1 << 3;
        private static final int SCORE_PRIVILEGE_ACCESS_RULE    = 1 << 2;
        private static final int SCORE_SPN                      = 1 << 1;
        private static final int SCORE_APN                      = 1 << 0;

        private static final int SCORE_INVALID                  = -1;

        // carrier matching attributes
        public final String mccMnc;
        public final String imsiPrefixPattern;
        public final String iccidPrefix;
        public final String gid1;
        public final String gid2;
        public final String plmn;
        public final String spn;
        public final String apn;
        // there can be multiple certs configured in the UICC
        public final List<String> privilegeAccessRule;

        // user-facing carrier name
        private String mName;
        // unique carrier id
        private int mCid;
        // unique parent carrier id
        private int mParentCid;

        private int mScore = 0;

        @VisibleForTesting
        public CarrierMatchingRule(String mccmnc, String imsiPrefixPattern, String iccidPrefix,
                String gid1, String gid2, String plmn, String spn, String apn,
                List<String> privilegeAccessRule, int cid, String name, int parentCid) {
            mccMnc = mccmnc;
            this.imsiPrefixPattern = imsiPrefixPattern;
            this.iccidPrefix = iccidPrefix;
            this.gid1 = gid1;
            this.gid2 = gid2;
            this.plmn = plmn;
            this.spn = spn;
            this.apn = apn;
            this.privilegeAccessRule = privilegeAccessRule;
            mCid = cid;
            mName = name;
            mParentCid = parentCid;
        }

        private CarrierMatchingRule(CarrierMatchingRule rule) {
            mccMnc = rule.mccMnc;
            imsiPrefixPattern = rule.imsiPrefixPattern;
            iccidPrefix = rule.iccidPrefix;
            gid1 = rule.gid1;
            gid2 = rule.gid2;
            plmn = rule.plmn;
            spn = rule.spn;
            apn = rule.apn;
            privilegeAccessRule = rule.privilegeAccessRule;
            mCid = rule.mCid;
            mName = rule.mName;
            mParentCid = rule.mParentCid;
        }

        // Calculate matching score. Values which aren't set in the rule are considered "wild".
        // All values in the rule must match in order for the subscription to be considered part of
        // the carrier. Otherwise, a invalid score -1 will be assigned. A match from a higher tier
        // will beat any subsequent match which does not match at that tier. When there are multiple
        // matches at the same tier, the match with highest score will be used.
        public void match(CarrierMatchingRule subscriptionRule) {
            mScore = 0;
            if (mccMnc != null) {
                if (!CarrierResolver.equals(subscriptionRule.mccMnc, mccMnc, false)) {
                    mScore = SCORE_INVALID;
                    return;
                }
                mScore += SCORE_MCCMNC;
            }
            if (imsiPrefixPattern != null) {
                if (!imsiPrefixMatch(subscriptionRule.imsiPrefixPattern, imsiPrefixPattern)) {
                    mScore = SCORE_INVALID;
                    return;
                }
                mScore += SCORE_IMSI_PREFIX;
            }
            if (iccidPrefix != null) {
                if (!iccidPrefixMatch(subscriptionRule.iccidPrefix, iccidPrefix)) {
                    mScore = SCORE_INVALID;
                    return;
                }
                mScore += SCORE_ICCID_PREFIX;
            }
            if (gid1 != null) {
                if (!gidMatch(subscriptionRule.gid1, gid1)) {
                    mScore = SCORE_INVALID;
                    return;
                }
                mScore += SCORE_GID1;
            }
            if (gid2 != null) {
                if (!gidMatch(subscriptionRule.gid2, gid2)) {
                    mScore = SCORE_INVALID;
                    return;
                }
                mScore += SCORE_GID2;
            }
            if (plmn != null) {
                if (!CarrierResolver.equals(subscriptionRule.plmn, plmn, true)) {
                    mScore = SCORE_INVALID;
                    return;
                }
                mScore += SCORE_PLMN;
            }
            if (spn != null) {
                if (!CarrierResolver.equals(subscriptionRule.spn, spn, true)) {
                    mScore = SCORE_INVALID;
                    return;
                }
                mScore += SCORE_SPN;
            }

            if (privilegeAccessRule != null && !privilegeAccessRule.isEmpty()) {
                if (!carrierPrivilegeRulesMatch(subscriptionRule.privilegeAccessRule,
                        privilegeAccessRule)) {
                    mScore = SCORE_INVALID;
                    return;
                }
                mScore += SCORE_PRIVILEGE_ACCESS_RULE;
            }

            if (apn != null) {
                if (!CarrierResolver.equals(subscriptionRule.apn, apn, true)) {
                    mScore = SCORE_INVALID;
                    return;
                }
                mScore += SCORE_APN;
            }
        }

        private boolean imsiPrefixMatch(String imsi, String prefixXPattern) {
            if (TextUtils.isEmpty(prefixXPattern)) return true;
            if (TextUtils.isEmpty(imsi)) return false;
            if (imsi.length() < prefixXPattern.length()) {
                return false;
            }
            for (int i = 0; i < prefixXPattern.length(); i++) {
                if ((prefixXPattern.charAt(i) != 'x') && (prefixXPattern.charAt(i) != 'X')
                        && (prefixXPattern.charAt(i) != imsi.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        private boolean iccidPrefixMatch(String iccid, String prefix) {
            if (iccid == null || prefix == null) {
                return false;
            }
            return iccid.startsWith(prefix);
        }

        // We are doing prefix and case insensitive match.
        // Ideally we should do full string match. However due to SIM manufacture issues
        // gid from some SIM might has garbage tail.
        private boolean gidMatch(String gidFromSim, String gid) {
            return (gidFromSim != null) && gidFromSim.toLowerCase().startsWith(gid.toLowerCase());
        }

        private boolean carrierPrivilegeRulesMatch(List<String> certsFromSubscription,
                                                   List<String> certs) {
            if (certsFromSubscription == null || certsFromSubscription.isEmpty()) {
                return false;
            }
            for (String cert : certs) {
                for (String certFromSubscription : certsFromSubscription) {
                    if (!TextUtils.isEmpty(cert)
                            && cert.equalsIgnoreCase(certFromSubscription)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public String toString() {
            return "[CarrierMatchingRule] -"
                    + " mccmnc: " + mccMnc
                    + " gid1: " + gid1
                    + " gid2: " + gid2
                    + " plmn: " + plmn
                    + " imsi_prefix: " + imsiPrefixPattern
                    + " iccid_prefix" + iccidPrefix
                    + " spn: " + spn
                    + " privilege_access_rule: " + privilegeAccessRule
                    + " apn: " + apn
                    + " name: " + mName
                    + " cid: " + mCid
                    + " score: " + mScore;
        }
    }

    private CarrierMatchingRule getSubscriptionMatchingRule() {
        final String mccmnc = mTelephonyMgr.getSimOperatorNumericForPhone(mPhone.getPhoneId());
        final String iccid = mPhone.getIccSerialNumber();
        final String gid1 = mPhone.getGroupIdLevel1();
        final String gid2 = mPhone.getGroupIdLevel2();
        final String imsi = mPhone.getSubscriberId();
        final String plmn = mPhone.getPlmn();
        final String spn = mSpn;
        final String apn = mPreferApn;
        List<String> accessRules;
        // check if test override present
        if (!TextUtils.isEmpty(mTestOverrideCarrierPriviledgeRule)) {
            accessRules = new ArrayList<>(Arrays.asList(mTestOverrideCarrierPriviledgeRule));
        } else {
            accessRules = mTelephonyMgr.createForSubscriptionId(mPhone.getSubId())
                    .getCertsFromCarrierPrivilegeAccessRules();
        }

        if (VDBG) {
            logd("[matchSubscriptionCarrier]"
                    + " mnnmnc:" + mccmnc
                    + " gid1: " + gid1
                    + " gid2: " + gid2
                    + " imsi: " + Rlog.pii(LOG_TAG, imsi)
                    + " iccid: " + Rlog.pii(LOG_TAG, iccid)
                    + " plmn: " + plmn
                    + " spn: " + spn
                    + " apn: " + apn
                    + " accessRules: " + ((accessRules != null) ? accessRules : null));
        }
        return new CarrierMatchingRule(
                mccmnc, imsi, iccid, gid1, gid2, plmn, spn, apn, accessRules,
                TelephonyManager.UNKNOWN_CARRIER_ID, null,
                TelephonyManager.UNKNOWN_CARRIER_ID);
    }

    private void updateCarrierConfig() {
        IccCard iccCard = mPhone.getIccCard();
        IccCardConstants.State simState = IccCardConstants.State.UNKNOWN;
        if (iccCard != null) {
            simState = iccCard.getState();
        }
        CarrierConfigManager configManager = (CarrierConfigManager)
                mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        configManager.updateConfigForPhoneId(mPhone.getPhoneId(),
                UiccController.getIccStateIntentString(simState));
    }

    /**
     * find the best matching carrier from candidates with matched subscription MCCMNC.
     */
    private void matchSubscriptionCarrier(boolean updateCarrierConfig, boolean isSimOverride) {
        if (!SubscriptionManager.isValidSubscriptionId(mPhone.getSubId())) {
            logd("[matchSubscriptionCarrier]" + "skip before sim records loaded");
            return;
        }
        int maxScore = CarrierMatchingRule.SCORE_INVALID;
        /**
         * For child-parent relationship. either child and parent have the same matching
         * score, or child's matching score > parents' matching score.
         */
        CarrierMatchingRule maxRule = null;
        CarrierMatchingRule maxRuleParent = null;
        /**
         * matching rule with mccmnc only. If mnoRule is found, then mno carrier id equals to the
         * cid from mnoRule. otherwise, mno carrier id is same as cid.
         */
        CarrierMatchingRule mnoRule = null;
        CarrierMatchingRule subscriptionRule = getSubscriptionMatchingRule();

        for (CarrierMatchingRule rule : mCarrierMatchingRulesOnMccMnc) {
            rule.match(subscriptionRule);
            if (rule.mScore > maxScore) {
                maxScore = rule.mScore;
                maxRule = rule;
                maxRuleParent = rule;
            } else if (maxScore > CarrierMatchingRule.SCORE_INVALID && rule.mScore == maxScore) {
                // to handle the case that child parent has the same matching score, we need to
                // differentiate who is child who is parent.
                if (rule.mParentCid == maxRule.mCid) {
                    maxRule = rule;
                } else if (maxRule.mParentCid == rule.mCid) {
                    maxRuleParent = rule;
                }
            }
            if (rule.mScore == CarrierMatchingRule.SCORE_MCCMNC) {
                mnoRule = rule;
            }
        }
        if (maxScore == CarrierMatchingRule.SCORE_INVALID) {
            logd("[matchSubscriptionCarrier - no match] cid: " + TelephonyManager.UNKNOWN_CARRIER_ID
                    + " name: " + null);
            updateCarrierIdAndName(TelephonyManager.UNKNOWN_CARRIER_ID, null,
                    TelephonyManager.UNKNOWN_CARRIER_ID, null,
                    TelephonyManager.UNKNOWN_CARRIER_ID, isSimOverride);
        } else {
            // if there is a single matching result, check if this rule has parent cid assigned.
            if ((maxRule == maxRuleParent)
                    && maxRule.mParentCid != TelephonyManager.UNKNOWN_CARRIER_ID) {
                maxRuleParent = new CarrierMatchingRule(maxRule);
                maxRuleParent.mCid = maxRuleParent.mParentCid;
                maxRuleParent.mName = getCarrierNameFromId(maxRuleParent.mCid);
            }
            logd("[matchSubscriptionCarrier] specific cid: " + maxRule.mCid
                    + " specific name: " + maxRule.mName +" cid: " + maxRuleParent.mCid
                    + " name: " + maxRuleParent.mName);
            updateCarrierIdAndName(maxRuleParent.mCid, maxRuleParent.mName,
                    maxRule.mCid, maxRule.mName,
                    (mnoRule == null) ? maxRule.mCid : mnoRule.mCid, isSimOverride);

            if (updateCarrierConfig) {
                logd("[matchSubscriptionCarrier] - Calling updateCarrierConfig()");
                updateCarrierConfig();
            }
        }

        /*
         * Write Carrier Identification Matching event, logging with the
         * carrierId, mccmnc, gid1 and carrier list version to differentiate below cases of metrics:
         * 1) unknown mccmnc - the Carrier Id provider contains no rule that matches the
         * read mccmnc.
         * 2) the Carrier Id provider contains some rule(s) that match the read mccmnc,
         * but the read gid1 is not matched within the highest-scored rule.
         * 3) successfully found a matched carrier id in the provider.
         * 4) use carrier list version to compare the unknown carrier ratio between each version.
         */
        String unknownGid1ToLog = ((maxScore & CarrierMatchingRule.SCORE_GID1) == 0
                && !TextUtils.isEmpty(subscriptionRule.gid1)) ? subscriptionRule.gid1 : null;
        String unknownMccmncToLog = ((maxScore == CarrierMatchingRule.SCORE_INVALID
                || (maxScore & CarrierMatchingRule.SCORE_GID1) == 0)
                && !TextUtils.isEmpty(subscriptionRule.mccMnc)) ? subscriptionRule.mccMnc : null;

        // pass subscription rule to metrics. scrub all possible PII before uploading.
        // only log apn if not user edited.
        String apn = (subscriptionRule.apn != null
                && !isPreferApnUserEdited(subscriptionRule.apn))
                ? subscriptionRule.apn : null;
        // only log first 7 bits of iccid
        String iccidPrefix = (subscriptionRule.iccidPrefix != null)
                && (subscriptionRule.iccidPrefix.length() >= 7)
                ? subscriptionRule.iccidPrefix.substring(0, 7) : subscriptionRule.iccidPrefix;
        // only log first 8 bits of imsi
        String imsiPrefix = (subscriptionRule.imsiPrefixPattern != null)
                && (subscriptionRule.imsiPrefixPattern.length() >= 8)
                ? subscriptionRule.imsiPrefixPattern.substring(0, 8)
                : subscriptionRule.imsiPrefixPattern;

        CarrierMatchingRule simInfo = new CarrierMatchingRule(
                subscriptionRule.mccMnc,
                imsiPrefix,
                iccidPrefix,
                subscriptionRule.gid1,
                subscriptionRule.gid2,
                subscriptionRule.plmn,
                subscriptionRule.spn,
                apn,
                subscriptionRule.privilegeAccessRule,
                -1, null, -1);

        TelephonyMetrics.getInstance().writeCarrierIdMatchingEvent(
                mPhone.getPhoneId(), getCarrierListVersion(), mCarrierId,
                unknownMccmncToLog, unknownGid1ToLog, simInfo);

        // Generate statsd metrics only when MCC/MNC is unknown or there is no match for GID1.
        if (unknownMccmncToLog != null || unknownGid1ToLog != null) {
            // Pass the PNN value to metrics only if the SPN is empty
            String pnn = TextUtils.isEmpty(subscriptionRule.spn) ? subscriptionRule.plmn : "";
            CarrierIdMatchStats.onCarrierIdMismatch(
                    mCarrierId, unknownMccmncToLog, unknownGid1ToLog, subscriptionRule.spn, pnn);
        }
    }

    public int getCarrierListVersion() {
        // Use the cached value if it exists, otherwise retrieve it.
        if (mCarrierListVersion == null) {
            final Cursor cursor = mContext.getContentResolver().query(
                    Uri.withAppendedPath(CarrierId.All.CONTENT_URI,
                    "get_version"), null, null, null);
            cursor.moveToFirst();
            mCarrierListVersion = cursor.getInt(0);
        }
        return mCarrierListVersion;
    }

    public int getCarrierId() {
        return mCarrierId;
    }
    /**
     * Returns fine-grained carrier id of the current subscription. Carrier ids with a valid parent
     * id are specific carrier ids.
     *
     * A specific carrier ID can represent the fact that a carrier may be in effect an aggregation
     * of other carriers (ie in an MVNO type scenario) where each of these specific carriers which
     * are used to make up the actual carrier service may have different carrier configurations.
     * A specific carrier ID could also be used, for example, in a scenario where a carrier requires
     * different carrier configuration for different service offering such as a prepaid plan.
     * e.g, {@link #getCarrierId()} will always return Tracfone (id 2022) for a Tracfone SIM, while
     * {@link #getSpecificCarrierId()} can return Tracfone AT&T or Tracfone T-Mobile based on the
     * IMSI from the current subscription.
     *
     * For carriers without any fine-grained carrier ids, return {@link #getCarrierId()}
     */
    public int getSpecificCarrierId() {
        return mSpecificCarrierId;
    }

    public String getCarrierName() {
        return mCarrierName;
    }

    public String getSpecificCarrierName() {
        return mSpecificCarrierName;
    }

    public int getMnoCarrierId() {
        return mMnoCarrierId;
    }

    /**
     * a util function to convert carrierIdentifier to the best matching carrier id.
     *
     * @return the best matching carrier id.
     */
    public static int getCarrierIdFromIdentifier(@NonNull Context context,
                                                 @NonNull CarrierIdentifier carrierIdentifier) {
        final String mccmnc = carrierIdentifier.getMcc() + carrierIdentifier.getMnc();
        final String gid1 = carrierIdentifier.getGid1();
        final String gid2 = carrierIdentifier.getGid2();
        final String imsi = carrierIdentifier.getImsi();
        final String spn = carrierIdentifier.getSpn();
        if (VDBG) {
            logd("[getCarrierIdFromIdentifier]"
                    + " mnnmnc:" + mccmnc
                    + " gid1: " + gid1
                    + " gid2: " + gid2
                    + " imsi: " + Rlog.pii(LOG_TAG, imsi)
                    + " spn: " + spn);
        }
        // assign null to other fields which are not supported by carrierIdentifier.
        CarrierMatchingRule targetRule =
                new CarrierMatchingRule(mccmnc, imsi, null, gid1, gid2, null,
                        spn, null, null,
                        TelephonyManager.UNKNOWN_CARRIER_ID_LIST_VERSION, null,
                        TelephonyManager.UNKNOWN_CARRIER_ID);

        int carrierId = TelephonyManager.UNKNOWN_CARRIER_ID;
        int maxScore = CarrierMatchingRule.SCORE_INVALID;
        List<CarrierMatchingRule> rules = getCarrierMatchingRulesFromMccMnc(
                context, targetRule.mccMnc);
        for (CarrierMatchingRule rule : rules) {
            rule.match(targetRule);
            if (rule.mScore > maxScore) {
                maxScore = rule.mScore;
                carrierId = rule.mCid;
            }
        }
        return carrierId;
    }

    /**
     * a util function to convert {mccmnc, mvno_type, mvno_data} to all matching carrier ids.
     *
     * @return a list of id with matching {mccmnc, mvno_type, mvno_data}
     */
    public static List<Integer> getCarrierIdsFromApnQuery(@NonNull Context context,
                                                          String mccmnc, String mvnoCase,
                                                          String mvnoData) {
        String selection = CarrierId.All.MCCMNC + "=" + mccmnc;
        // build the proper query
        if ("spn".equals(mvnoCase) && mvnoData != null) {
            selection += " AND " + CarrierId.All.SPN + "='" + mvnoData + "'";
        } else if ("imsi".equals(mvnoCase) && mvnoData != null) {
            selection += " AND " + CarrierId.All.IMSI_PREFIX_XPATTERN + "='" + mvnoData + "'";
        } else if ("gid1".equals(mvnoCase) && mvnoData != null) {
            selection += " AND " + CarrierId.All.GID1 + "='" + mvnoData + "'";
        } else if ("gid2".equals(mvnoCase) && mvnoData != null) {
            selection += " AND " + CarrierId.All.GID2 + "='" + mvnoData +"'";
        } else {
            logd("mvno case empty or other invalid values");
        }

        List<Integer> ids = new ArrayList<>();
        try {
            Cursor cursor = context.getContentResolver().query(
                    CarrierId.All.CONTENT_URI,
                    /* projection */ null,
                    /* selection */ selection,
                    /* selectionArgs */ null, null);
            try {
                if (cursor != null) {
                    if (VDBG) {
                        logd("[getCarrierIdsFromApnQuery]- " + cursor.getCount()
                                + " Records(s) in DB");
                    }
                    while (cursor.moveToNext()) {
                        int cid = cursor.getInt(cursor.getColumnIndex(CarrierId.CARRIER_ID));
                        if (!ids.contains(cid)) {
                            ids.add(cid);
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } catch (Exception ex) {
            loge("[getCarrierIdsFromApnQuery]- ex: " + ex);
        }
        logd(selection + " " + ids);
        return ids;
    }

    // static helper function to get carrier id from mccmnc
    public static int getCarrierIdFromMccMnc(@NonNull Context context, String mccmnc) {
        try (Cursor cursor = getCursorForMccMnc(context, mccmnc)) {
            if (cursor == null || !cursor.moveToNext()) return TelephonyManager.UNKNOWN_CARRIER_ID;
            if (VDBG) {
                logd("[getCarrierIdFromMccMnc]- " + cursor.getCount()
                        + " Records(s) in DB" + " mccmnc: " + mccmnc);
            }
            return cursor.getInt(cursor.getColumnIndex(CarrierId.CARRIER_ID));
        } catch (Exception ex) {
            loge("[getCarrierIdFromMccMnc]- ex: " + ex);
        }
        return TelephonyManager.UNKNOWN_CARRIER_ID;
    }

    /**
     * Static helper function to get carrier name from mccmnc
     * @param context Context
     * @param mccmnc PLMN
     * @return Carrier name string given mccmnc/PLMN
     *
     * @hide
     */
    @Nullable
    public static String getCarrierNameFromMccMnc(@NonNull Context context, String mccmnc) {
        try (Cursor cursor = getCursorForMccMnc(context, mccmnc)) {
            if (cursor == null || !cursor.moveToNext()) return null;
            if (VDBG) {
                logd("[getCarrierNameFromMccMnc]- " + cursor.getCount()
                        + " Records(s) in DB" + " mccmnc: " + mccmnc);
            }
            return cursor.getString(cursor.getColumnIndex(CarrierId.CARRIER_NAME));
        } catch (Exception ex) {
            loge("[getCarrierNameFromMccMnc]- ex: " + ex);
        }
        return null;
    }

    @Nullable
    private static Cursor getCursorForMccMnc(@NonNull Context context, String mccmnc) {
        try {
            Cursor cursor = context.getContentResolver().query(
                    CarrierId.All.CONTENT_URI,
                    /* projection */ null,
                    /* selection */ CarrierId.All.MCCMNC + "=? AND "
                            + CarrierId.All.GID1 + " is NULL AND "
                            + CarrierId.All.GID2 + " is NULL AND "
                            + CarrierId.All.IMSI_PREFIX_XPATTERN + " is NULL AND "
                            + CarrierId.All.SPN + " is NULL AND "
                            + CarrierId.All.ICCID_PREFIX + " is NULL AND "
                            + CarrierId.All.PLMN + " is NULL AND "
                            + CarrierId.All.PRIVILEGE_ACCESS_RULE + " is NULL AND "
                            + CarrierId.All.APN + " is NULL",
                    /* selectionArgs */ new String[]{mccmnc},
                    null);
            return cursor;
        } catch (Exception ex) {
            loge("[getCursorForMccMnc]- ex: " + ex);
            return null;
        }
    }

    private static boolean equals(String a, String b, boolean ignoreCase) {
        if (a == null && b == null) return true;
        if (a != null && b != null) {
            return (ignoreCase) ? a.equalsIgnoreCase(b) : a.equals(b);
        }
        return false;
    }

    private static void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }
    private static void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    private static void logd(String str, int phoneId) {
        Rlog.d(LOG_TAG + "[" + phoneId + "]", str);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        final IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        ipw.println("mCarrierResolverLocalLogs:");
        ipw.increaseIndent();
        mCarrierIdLocalLog.dump(fd, pw, args);
        ipw.decreaseIndent();

        ipw.println("mCarrierId: " + mCarrierId);
        ipw.println("mSpecificCarrierId: " + mSpecificCarrierId);
        ipw.println("mMnoCarrierId: " + mMnoCarrierId);
        ipw.println("mCarrierName: " + mCarrierName);
        ipw.println("mSpecificCarrierName: " + mSpecificCarrierName);
        ipw.println("carrier_list_version: " + getCarrierListVersion());

        ipw.println("mCarrierMatchingRules on mccmnc: "
                + mTelephonyMgr.getSimOperatorNumericForPhone(mPhone.getPhoneId()));
        ipw.increaseIndent();
        for (CarrierMatchingRule rule : mCarrierMatchingRulesOnMccMnc) {
            ipw.println(rule.toString());
        }
        ipw.decreaseIndent();

        ipw.println("mSpn: " + mSpn);
        ipw.println("mPreferApn: " + mPreferApn);
        ipw.flush();
    }
}
