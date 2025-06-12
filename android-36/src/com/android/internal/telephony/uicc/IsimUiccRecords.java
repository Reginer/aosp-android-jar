/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.annotation.NonNull;

import static com.android.internal.telephony.util.TelephonyUtils.FORCE_VERBOSE_STATE_LOGGING;

import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Message;
import android.os.UserHandle;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.gsm.SimTlv;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * {@hide}
 */
public class IsimUiccRecords extends IccRecords implements IsimRecords {
    protected static final String LOG_TAG = "IsimUiccRecords";

    private static final boolean DBG = true;
    private static final boolean VDBG =  FORCE_VERBOSE_STATE_LOGGING ||
            Rlog.isLoggable(LOG_TAG, Log.VERBOSE);
    private static final boolean DUMP_RECORDS = false;  // Note: PII is logged when this is true
                                                        // STOPSHIP if true
    public static final String INTENT_ISIM_REFRESH = "com.android.intent.isim_refresh";

    // ISIM EF records (see 3GPP TS 31.103)
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private String mIsimImpi;               // IMS private user identity
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private String mIsimDomain;             // IMS home network domain name
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private String[] mIsimImpu;             // IMS public user identity(s)
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private String mIsimIst;                // IMS Service Table
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private String[] mIsimPcscf;            // IMS Proxy Call Session Control Function
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private String auth_rsp;

    @NonNull
    private final FeatureFlags mFeatureFlags;

    private static final int TAG_ISIM_VALUE = 0x80;     // From 3GPP TS 31.103

    @Override
    public String toString() {
        return "IsimUiccRecords: " + super.toString()
                + (DUMP_RECORDS ? (" mIsimImpi=" + mIsimImpi
                + " mIsimDomain=" + mIsimDomain
                + " mIsimImpu=" + Arrays.toString(mIsimImpu)
                + " mIsimIst=" + mIsimIst
                + " mIsimPcscf=" + Arrays.toString(mIsimPcscf)
                + " mPsiSmsc=" + mPsiSmsc
                + " mSmss TPMR=" + getSmssTpmrValue()) : "");
    }

    public IsimUiccRecords(@NonNull UiccCardApplication app, @NonNull Context c,
            @NonNull CommandsInterface ci, @NonNull FeatureFlags flags) {
        super(app, c, ci);
        mFeatureFlags = flags;
        mRecordsRequested = false;  // No load request is made till SIM ready
        //todo: currently locked state for ISIM is not handled well and may cause app state to not
        //be broadcast
        mLockedRecordsReqReason = LOCKED_RECORDS_REQ_REASON_NONE;

        // recordsToLoad is set to 0 because no requests are made yet
        mRecordsToLoad = 0;
        // Start off by setting empty state
        resetRecords();
        if (DBG) log("IsimUiccRecords X ctor this=" + this);
    }

    @Override
    public void dispose() {
        log("Disposing " + this);
        resetRecords();
        super.dispose();
    }

    // ***** Overridden from Handler
    public void handleMessage(Message msg) {
        AsyncResult ar;

        if (mDestroyed.get()) {
            Rlog.e(LOG_TAG, "Received message " + msg +
                    "[" + msg.what + "] while being destroyed. Ignoring.");
            return;
        }
        loge("IsimUiccRecords: handleMessage " + msg + "[" + msg.what + "] ");

        try {
            switch (msg.what) {
                case EVENT_REFRESH:
                    broadcastRefresh();
                    super.handleMessage(msg);
                    break;
                default:
                    super.handleMessage(msg);   // IccRecords handles generic record load responses

            }
        } catch (RuntimeException exc) {
            // I don't want these exceptions to be fatal
            Rlog.w(LOG_TAG, "Exception parsing SIM record", exc);
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected void fetchIsimRecords() {
        mRecordsRequested = true;

        mFh.loadEFTransparent(EF_IMPI, obtainMessage(
                IccRecords.EVENT_GET_ICC_RECORD_DONE, new EfIsimImpiLoaded()));
        mRecordsToLoad++;

        mFh.loadEFLinearFixedAll(EF_IMPU, obtainMessage(
                IccRecords.EVENT_GET_ICC_RECORD_DONE, new EfIsimImpuLoaded()));
        mRecordsToLoad++;

        mFh.loadEFTransparent(EF_DOMAIN, obtainMessage(
                IccRecords.EVENT_GET_ICC_RECORD_DONE, new EfIsimDomainLoaded()));
        mRecordsToLoad++;
        mFh.loadEFTransparent(EF_IST, obtainMessage(
                    IccRecords.EVENT_GET_ICC_RECORD_DONE, new EfIsimIstLoaded()));
        mRecordsToLoad++;
        mFh.loadEFLinearFixedAll(EF_PCSCF, obtainMessage(
                    IccRecords.EVENT_GET_ICC_RECORD_DONE, new EfIsimPcscfLoaded()));
        mRecordsToLoad++;
        mFh.loadEFTransparent(EF_SMSS,  obtainMessage(
                IccRecords.EVENT_GET_ICC_RECORD_DONE, new EfIsimSmssLoaded()));
        mRecordsToLoad++;

        mFh.loadEFLinearFixed(EF_PSISMSC, 1, obtainMessage(
                IccRecords.EVENT_GET_ICC_RECORD_DONE, new EfIsimPsiSmscLoaded()));
        mRecordsToLoad++;

        if (DBG) log("fetchIsimRecords " + mRecordsToLoad + " requested: " + mRecordsRequested);
    }

    protected void resetRecords() {
        // recordsRequested is set to false indicating that the SIM
        // read requests made so far are not valid. This is set to
        // true only when fresh set of read requests are made.
        mIsimImpi = null;
        mIsimDomain = null;
        mIsimImpu = null;
        mIsimIst = null;
        mIsimPcscf = null;
        auth_rsp = null;

        mRecordsRequested = false;
        mLockedRecordsReqReason = LOCKED_RECORDS_REQ_REASON_NONE;
        mLoaded.set(false);
    }

    private class EfIsimImpiLoaded implements IccRecords.IccRecordLoaded {
        public String getEfName() {
            return "EF_ISIM_IMPI";
        }
        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = (byte[]) ar.result;
            mIsimImpi = isimTlvToString(data);
            if (DUMP_RECORDS) log("EF_IMPI=" + mIsimImpi);
        }
    }

    private class EfIsimImpuLoaded implements IccRecords.IccRecordLoaded {
        public String getEfName() {
            return "EF_ISIM_IMPU";
        }
        public void onRecordLoaded(AsyncResult ar) {
            ArrayList<byte[]> impuList = (ArrayList<byte[]>) ar.result;
            if (DBG) log("EF_IMPU record count: " + impuList.size());
            mIsimImpu = new String[impuList.size()];
            int i = 0;
            for (byte[] identity : impuList) {
                String impu = isimTlvToString(identity);
                if (DUMP_RECORDS) log("EF_IMPU[" + i + "]=" + impu);
                mIsimImpu[i++] = impu;
            }
        }
    }

    private class EfIsimDomainLoaded implements IccRecords.IccRecordLoaded {
        public String getEfName() {
            return "EF_ISIM_DOMAIN";
        }
        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = (byte[]) ar.result;
            mIsimDomain = isimTlvToString(data);
            if (DUMP_RECORDS) log("EF_DOMAIN=" + mIsimDomain);
        }
    }

    private class EfIsimIstLoaded implements IccRecords.IccRecordLoaded {
        public String getEfName() {
            return "EF_ISIM_IST";
        }
        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = (byte[]) ar.result;
            mIsimIst = IccUtils.bytesToHexString(data);
            if (DUMP_RECORDS) log("EF_IST=" + mIsimIst);
        }
    }

    @VisibleForTesting
    public EfIsimIstLoaded getIsimIstObject() {
        return new EfIsimIstLoaded();
    }

    private class EfIsimSmssLoaded implements IccRecords.IccRecordLoaded {

        @Override
        public String getEfName() {
            return "EF_ISIM_SMSS";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            mSmssValues = (byte[]) ar.result;
            if (VDBG) {
                log("IsimUiccRecords - EF_SMSS TPMR value = " + getSmssTpmrValue());
            }
        }
    }

    private class EfIsimPcscfLoaded implements IccRecords.IccRecordLoaded {
        public String getEfName() {
            return "EF_ISIM_PCSCF";
        }
        public void onRecordLoaded(AsyncResult ar) {
            ArrayList<byte[]> pcscflist = (ArrayList<byte[]>) ar.result;
            if (DBG) log("EF_PCSCF record count: " + pcscflist.size());
            mIsimPcscf = new String[pcscflist.size()];
            int i = 0;
            for (byte[] identity : pcscflist) {
                String pcscf = isimTlvToString(identity);
                if (DUMP_RECORDS) log("EF_PCSCF[" + i + "]=" + pcscf);
                mIsimPcscf[i++] = pcscf;
            }
        }
    }

    private class EfIsimPsiSmscLoaded implements IccRecords.IccRecordLoaded {

        @Override
        public String getEfName() {
            return "EF_ISIM_PSISMSC";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = (byte[]) ar.result;
            if (data != null && data.length > 0) {
                mPsiSmsc = parseEfPsiSmsc(data);
                if (VDBG) {
                    log("IsimUiccRecords - EF_PSISMSC value = " + mPsiSmsc);
                }
            }
        }
    }

    @VisibleForTesting
    public EfIsimPsiSmscLoaded getPsiSmscObject() {
        return new EfIsimPsiSmscLoaded();
    }
    /**
     * ISIM records for IMS are stored inside a Tag-Length-Value record as a UTF-8 string
     * with tag value 0x80.
     * @param record the byte array containing the IMS data string
     * @return the decoded String value, or null if the record can't be decoded
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private static String isimTlvToString(byte[] record) {
        SimTlv tlv = new SimTlv(record, 0, record.length);
        do {
            if (tlv.getTag() == TAG_ISIM_VALUE) {
                return new String(tlv.getData(), Charset.forName("UTF-8"));
            }
        } while (tlv.nextObject());

        if (VDBG) {
            Rlog.d(LOG_TAG, "[ISIM] can't find TLV. record = " + IccUtils.bytesToHexString(record));
        }
        return null;
    }

    @Override
    protected void onRecordLoaded() {
        // One record loaded successfully or failed, In either case
        // we need to update the recordsToLoad count
        mRecordsToLoad -= 1;
        if (DBG) log("onRecordLoaded " + mRecordsToLoad + " requested: " + mRecordsRequested);

        if (getRecordsLoaded()) {
            onAllRecordsLoaded();
        } else if (getLockedRecordsLoaded() || getNetworkLockedRecordsLoaded()) {
            onLockedAllRecordsLoaded();
        } else if (mRecordsToLoad < 0) {
            loge("recordsToLoad <0, programmer error suspected");
            mRecordsToLoad = 0;
        }
    }

    private void onLockedAllRecordsLoaded() {
        if (DBG) log("SIM locked; record load complete");
        if (mLockedRecordsReqReason == LOCKED_RECORDS_REQ_REASON_LOCKED) {
            mLockedRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
        } else if (mLockedRecordsReqReason == LOCKED_RECORDS_REQ_REASON_NETWORK_LOCKED) {
            mNetworkLockedRecordsLoadedRegistrants.notifyRegistrants(
                    new AsyncResult(null, null, null));
        } else {
            loge("onLockedAllRecordsLoaded: unexpected mLockedRecordsReqReason "
                    + mLockedRecordsReqReason);
        }
    }

    @Override
    protected void onAllRecordsLoaded() {
       if (DBG) log("record load complete");
        mLoaded.set(true);
        mRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
    }

    @Override
    protected void handleFileUpdate(int efid) {
        switch (efid) {
            case EF_IMPI:
                mFh.loadEFTransparent(EF_IMPI, obtainMessage(
                            IccRecords.EVENT_GET_ICC_RECORD_DONE, new EfIsimImpiLoaded()));
                mRecordsToLoad++;
                break;

            case EF_IMPU:
                mFh.loadEFLinearFixedAll(EF_IMPU, obtainMessage(
                            IccRecords.EVENT_GET_ICC_RECORD_DONE, new EfIsimImpuLoaded()));
                mRecordsToLoad++;
            break;

            case EF_DOMAIN:
                mFh.loadEFTransparent(EF_DOMAIN, obtainMessage(
                            IccRecords.EVENT_GET_ICC_RECORD_DONE, new EfIsimDomainLoaded()));
                mRecordsToLoad++;
            break;

            case EF_IST:
                mFh.loadEFTransparent(EF_IST, obtainMessage(
                            IccRecords.EVENT_GET_ICC_RECORD_DONE, new EfIsimIstLoaded()));
                mRecordsToLoad++;
            break;

            case EF_PCSCF:
                mFh.loadEFLinearFixedAll(EF_PCSCF, obtainMessage(
                            IccRecords.EVENT_GET_ICC_RECORD_DONE, new EfIsimPcscfLoaded()));
                mRecordsToLoad++;

            default:
                mLoaded.set(false);
                fetchIsimRecords();
                break;
        }
    }

    private void broadcastRefresh() {
        Intent intent = new Intent(INTENT_ISIM_REFRESH);
        log("send ISim REFRESH: " + INTENT_ISIM_REFRESH);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mParentApp.getPhoneId());
        if (mFeatureFlags.hsumBroadcast()) {
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } else {
            mContext.sendBroadcast(intent);
        }
    }

    /**
     * Return the IMS private user identity (IMPI).
     * Returns null if the IMPI hasn't been loaded or isn't present on the ISIM.
     * @return the IMS private user identity string, or null if not available
     */
    @Override
    public String getIsimImpi() {
        return mIsimImpi;
    }

    /**
     * Return the IMS home network domain name.
     * Returns null if the IMS domain hasn't been loaded or isn't present on the ISIM.
     * @return the IMS home network domain name, or null if not available
     */
    @Override
    public String getIsimDomain() {
        return mIsimDomain;
    }

    /**
     * Return an array of IMS public user identities (IMPU).
     * Returns null if the IMPU hasn't been loaded or isn't present on the ISIM.
     * @return an array of IMS public user identity strings, or null if not available
     */
    @Override
    public String[] getIsimImpu() {
        return (mIsimImpu != null) ? mIsimImpu.clone() : null;
    }

    /**
     * Returns the IMS Service Table (IST) that was loaded from the ISIM.
     * @return IMS Service Table or null if not present or not loaded
     */
    @Override
    public String getIsimIst() {
        return mIsimIst;
    }

    /**
     * Returns the IMS Proxy Call Session Control Function(PCSCF) that were loaded from the ISIM.
     * @return an array of  PCSCF strings with one PCSCF per string, or null if
     *      not present or not loaded
     */
    @Override
    public String[] getIsimPcscf() {
        return (mIsimPcscf != null) ? mIsimPcscf.clone() : null;
    }

    @Override
    public void onReady() {
        fetchIsimRecords();
    }

    @Override
    public void onRefresh(boolean fileChanged, int[] fileList) {
        if (fileChanged) {
            // A future optimization would be to inspect fileList and
            // only reload those files that we care about.  For now,
            // just re-fetch all SIM records that we cache.
            fetchIsimRecords();
        }
    }

    @Override
    public void setVoiceMailNumber(String alphaTag, String voiceNumber,
            Message onComplete) {
        // Not applicable to Isim
    }

    @Override
    public void setVoiceMessageWaiting(int line, int countWaiting) {
        // Not applicable to Isim
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @Override
    protected void log(String s) {
        if (mParentApp != null) {
            Rlog.d(LOG_TAG, "[ISIM-" + mParentApp.getPhoneId() + "] " + s);
        } else {
            Rlog.d(LOG_TAG, "[ISIM] " + s);
        }
    }

    @Override
    protected void loge(String s) {
        if (mParentApp != null) {
            Rlog.e(LOG_TAG, "[ISIM-" + mParentApp.getPhoneId() + "] " + s);
        } else {
            Rlog.e(LOG_TAG, "[ISIM] " + s);
        }
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("IsimRecords: " + this);
        pw.println(" extends:");
        super.dump(fd, pw, args);
        pw.println(" mIsimServiceTable=" + getIsimServiceTable());
        if (DUMP_RECORDS) {
            pw.println(" mIsimImpi=" + mIsimImpi);
            pw.println(" mIsimDomain=" + mIsimDomain);
            pw.println(" mIsimImpu[]=" + Arrays.toString(mIsimImpu));
            pw.println(" mIsimPcscf" + Arrays.toString(mIsimPcscf));
            pw.println(" mPsismsc=" + mPsiSmsc);
            pw.println(" mSmss TPMR=" + getSmssTpmrValue());
        }
        pw.flush();
    }

    // Just to return the Enums of service table to print in DUMP
    private IsimServiceTable getIsimServiceTable() {
        if (mIsimIst != null) {
            byte[] istTable = IccUtils.hexStringToBytes(mIsimIst);
            return new IsimServiceTable(istTable);
        }
        return null;
    }
    @Override
    public int getVoiceMessageCount() {
        return 0; // Not applicable to Isim
    }
}