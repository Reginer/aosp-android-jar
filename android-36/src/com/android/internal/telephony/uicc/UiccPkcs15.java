/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;

import com.android.internal.telephony.uicc.UiccCarrierPrivilegeRules.TLV;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Class that reads PKCS15-based rules for carrier privileges.
 *
 * The spec for the rules:
 *     GP Secure Element Access Control:
 *     https://www.globalplatform.org/specificationsdevice.asp
 *
 * The UiccPkcs15 class handles overall flow of finding/selecting PKCS15 applet
 * and reading/parsing each file. Because PKCS15 can be selected in 2 different ways:
 * via logical channel or EF_DIR, PKCS15Selector is a handler to encapsulate the flow.
 * Similarly, FileHandler is used for selecting/reading each file, so common codes are
 * all in same place.
 *
 * {@hide}
 */
public class UiccPkcs15 extends Handler {
    private static final String LOG_TAG = "UiccPkcs15";
    private static final boolean DBG = true;

    // File handler for PKCS15 files, select file and read binary,
    // convert to String then send to callback message.
    private class FileHandler extends Handler {
        // EF path for PKCS15 root, eg. "3F007F50"
        // null if logical channel is used for PKCS15 access.
        final String mPkcs15Path;
        // Message to send when file has been parsed.
        private Message mCallback;
        // File id to read data from, eg. "5031"
        private String mFileId;

        // async events for the sequence of select and read
        static protected final int EVENT_SELECT_FILE_DONE = 101;
        static protected final int EVENT_READ_BINARY_DONE = 102;

        // pkcs15Path is nullable when using logical channel
        public FileHandler(String pkcs15Path) {
            log("Creating FileHandler, pkcs15Path: " + pkcs15Path);
            mPkcs15Path = pkcs15Path;
        }

        public boolean loadFile(String fileId, Message callBack) {
            log("loadFile: " + fileId);
            if (fileId == null || callBack == null) return false;
            mFileId = fileId;
            mCallback = callBack;
            selectFile();
            return true;
        }

        private void selectFile() {
            if (mChannelId >= 0) {
                mUiccProfile.iccTransmitApduLogicalChannel(mChannelId, 0x00, 0xA4, 0x00, 0x04, 0x02,
                        mFileId, false /*isEs10Command*/, obtainMessage(EVENT_SELECT_FILE_DONE));
            } else {
                log("EF based");
            }
        }

        private void readBinary() {
            if (mChannelId >=0 ) {
                mUiccProfile.iccTransmitApduLogicalChannel(mChannelId, 0x00, 0xB0, 0x00, 0x00, 0x00,
                        "",  false /*isEs10Command*/, obtainMessage(EVENT_READ_BINARY_DONE));
            } else {
                log("EF based");
            }
        }

        @Override
        public void handleMessage(Message msg) {
            log("handleMessage: " + msg.what);
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null || ar.result == null) {
                log("Error: " + ar.exception);
                AsyncResult.forMessage(mCallback, null, ar.exception);
                mCallback.sendToTarget();
                return;
            }
            IccIoResult response;
            switch (msg.what) {
                case EVENT_SELECT_FILE_DONE:
                    response = (IccIoResult) ar.result;
                    if (response.getException() == null) {
                        readBinary();
                    } else {
                        log("Select file error : " + response.getException());
                        AsyncResult.forMessage(mCallback, null, response.getException());
                        mCallback.sendToTarget();
                    }
                    break;

                case EVENT_READ_BINARY_DONE:
                    response = (IccIoResult) ar.result;
                    String result = IccUtils.bytesToHexString(response.payload)
                            .toUpperCase(Locale.US);
                    log("IccIoResult: " + response + " payload: " + result);
                    AsyncResult.forMessage(mCallback, result, (result == null) ?
                            new IccException("Error: null response for " + mFileId) : null);
                    mCallback.sendToTarget();
                    break;

                default:
                    log("Unknown event" + msg.what);
            }
        }
    }

    private class Pkcs15Selector extends Handler {
        private static final String PKCS15_AID = "A000000063504B43532D3135";
        private Message mCallback;
        private static final int EVENT_OPEN_LOGICAL_CHANNEL_DONE = 201;

        public Pkcs15Selector(Message callBack) {
            mCallback = callBack;
            // Specified in ISO 7816-4 clause 7.1.1 0x04 means that FCP template is requested.
            int p2 = 0x04;
            mUiccProfile.iccOpenLogicalChannel(PKCS15_AID, p2, /* supported P2 value */
                    obtainMessage(EVENT_OPEN_LOGICAL_CHANNEL_DONE));
        }

        @Override
        public void handleMessage(Message msg) {
            log("handleMessage: " + msg.what);
            AsyncResult ar;

            switch (msg.what) {
              case EVENT_OPEN_LOGICAL_CHANNEL_DONE:
                  ar = (AsyncResult) msg.obj;
                  if (ar.exception == null && ar.result != null) {
                      mChannelId = ((int[]) ar.result)[0];
                      log("mChannelId: " + mChannelId);
                      AsyncResult.forMessage(mCallback, null, null);
                  } else {
                      log("error: " + ar.exception);
                      AsyncResult.forMessage(mCallback, null, ar.exception);
                      // TODO: don't sendToTarget and read EF_DIR to find PKCS15
                  }
                  mCallback.sendToTarget();
                  break;

              default:
                  log("Unknown event" + msg.what);
            }
        }
    }

    private UiccProfile mUiccProfile;  // Parent
    private Message mLoadedCallback;
    private int mChannelId = -1; // Channel Id for communicating with UICC.
    private List<String> mRules = null;
    private Pkcs15Selector mPkcs15Selector;
    private FileHandler mFh;

    private static final int EVENT_SELECT_PKCS15_DONE = 1;
    private static final int EVENT_LOAD_ODF_DONE = 2;
    private static final int EVENT_LOAD_DODF_DONE = 3;
    private static final int EVENT_LOAD_ACMF_DONE = 4;
    private static final int EVENT_LOAD_ACRF_DONE = 5;
    private static final int EVENT_LOAD_ACCF_DONE = 6;
    private static final int EVENT_CLOSE_LOGICAL_CHANNEL_DONE = 7;

    public UiccPkcs15(UiccProfile uiccProfile, Message loadedCallback) {
        log("Creating UiccPkcs15");
        mUiccProfile = uiccProfile;
        mLoadedCallback = loadedCallback;
        mPkcs15Selector = new Pkcs15Selector(obtainMessage(EVENT_SELECT_PKCS15_DONE));
    }

    @Override
    public void handleMessage(Message msg) {
        log("handleMessage: " + msg.what);
        AsyncResult ar = (AsyncResult) msg.obj;

        switch (msg.what) {
            case EVENT_SELECT_PKCS15_DONE:
                if (ar.exception == null) {
                    // ar.result is null if using logical channel,
                    // or string for pkcs15 path if using file access.
                    mFh = new FileHandler((String) ar.result);
                    if (!mFh.loadFile(EFODF_PATH, obtainMessage(EVENT_LOAD_ODF_DONE))) {
                        startFromAcrf();
                    }
                } else {
                    log("select pkcs15 failed: " + ar.exception);
                    // select PKCS15 failed, notify uiccCarrierPrivilegeRules
                    mLoadedCallback.sendToTarget();
                }
                break;

            case EVENT_LOAD_ODF_DONE:
                if (ar.exception == null && ar.result != null) {
                    String idDodf = parseOdf((String) ar.result);
                    if (!mFh.loadFile(idDodf, obtainMessage(EVENT_LOAD_DODF_DONE))) {
                        startFromAcrf();
                    }
                } else {
                    startFromAcrf();
                }
                break;

            case EVENT_LOAD_DODF_DONE:
                if (ar.exception == null && ar.result != null) {
                    String idAcmf = parseDodf((String) ar.result);
                    if (!mFh.loadFile(idAcmf, obtainMessage(EVENT_LOAD_ACMF_DONE))) {
                        startFromAcrf();
                    }
                } else {
                    startFromAcrf();
                }
                break;

            case EVENT_LOAD_ACMF_DONE:
                if (ar.exception == null && ar.result != null) {
                    String idAcrf = parseAcmf((String) ar.result);
                    if (!mFh.loadFile(idAcrf, obtainMessage(EVENT_LOAD_ACRF_DONE))) {
                        startFromAcrf();
                    }
                } else {
                    startFromAcrf();
                }
                break;

            case EVENT_LOAD_ACRF_DONE:
                if (ar.exception == null && ar.result != null) {
                    mRules = new ArrayList<String>();
                    String idAccf = parseAcrf((String) ar.result);
                    if (!mFh.loadFile(idAccf, obtainMessage(EVENT_LOAD_ACCF_DONE))) {
                        cleanUp();
                    }
                } else {
                    cleanUp();
                }
                break;

            case EVENT_LOAD_ACCF_DONE:
                if (ar.exception == null && ar.result != null) {
                    parseAccf((String) ar.result);
                }
                // We are done here, no more file to read
                cleanUp();
                break;

            case EVENT_CLOSE_LOGICAL_CHANNEL_DONE:
                break;

            default:
                Rlog.e(LOG_TAG, "Unknown event " + msg.what);
        }
    }

    private void startFromAcrf() {
        log("Fallback to use ACRF_PATH");
        if (!mFh.loadFile(ACRF_PATH, obtainMessage(EVENT_LOAD_ACRF_DONE))) {
            cleanUp();
        }
    }

    private void cleanUp() {
        log("cleanUp");
        if (mChannelId >= 0) {
            mUiccProfile.iccCloseLogicalChannel(mChannelId, false /*isEs10*/,
                    obtainMessage(EVENT_CLOSE_LOGICAL_CHANNEL_DONE));
            mChannelId = -1;
        }
        mLoadedCallback.sendToTarget();
    }

    // Constants defined in specs, needed for parsing
    private static final String CARRIER_RULE_AID = "FFFFFFFFFFFF"; // AID for carrier privilege rule
    private static final String ACRF_PATH = "4300";
    private static final String EFODF_PATH = "5031";
    private static final String TAG_ASN_SEQUENCE = "30";
    private static final String TAG_ASN_OCTET_STRING = "04";
    private static final String TAG_ASN_OID = "06";
    private static final String TAG_TARGET_AID = "A0";
    private static final String TAG_ODF = "A7";
    private static final String TAG_DODF = "A1";
    private static final String REFRESH_TAG_LEN = "08";
    // OID defined by Global Platform for the "Access Control". The hexstring here can be converted
    // to OID string value 1.2.840.114283.200.1.1
    public static final String AC_OID = "060A2A864886FC6B81480101";


    // parse ODF file to get file id for DODF file
    // data is hex string, return file id if parse success, null otherwise
    private String parseOdf(String data) {
        // Example:
        // [A7] 06 [30] 04 [04] 02 52 07
        try {
            TLV tlvRule = new TLV(TAG_ODF); // A7
            tlvRule.parse(data, false);
            String ruleString = tlvRule.getValue();
            TLV tlvAsnPath = new TLV(TAG_ASN_SEQUENCE); // 30
            TLV tlvPath = new TLV(TAG_ASN_OCTET_STRING);  // 04
            tlvAsnPath.parse(ruleString, true);
            tlvPath.parse(tlvAsnPath.getValue(), true);
            return tlvPath.getValue();
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            log("Error: " + ex);
            return null;
        }
    }

    // parse DODF file to get file id for ACMF file
    // data is hex string, return file id if parse success, null otherwise
    private String parseDodf(String data) {
        // Example:
        // [A1] 29 [30] 00 [30] 0F 0C 0D 47 50 20 53 45 20 41 63 63 20 43 74 6C [A1] 14 [30] 12
        // [06] 0A 2A 86 48 86 FC 6B 81 48 01 01 [30] 04 04 02 42 00
        String ret = null;
        String acRules = data;
        while (!acRules.isEmpty()) {
            TLV dodfTag = new TLV(TAG_DODF); // A1
            try {
                acRules = dodfTag.parse(acRules, false);
                String ruleString = dodfTag.getValue();
                // Skip the Common Object Attributes
                TLV commonObjectAttributes = new TLV(TAG_ASN_SEQUENCE); // 30
                ruleString = commonObjectAttributes.parse(ruleString, false);

                // Skip the Common Data Object Attributes
                TLV commonDataObjectAttributes = new TLV(TAG_ASN_SEQUENCE); // 30
                ruleString = commonDataObjectAttributes.parse(ruleString, false);

                if (ruleString.startsWith(TAG_TARGET_AID)) {
                    // Skip SubClassAttributes [Optional]
                    TLV subClassAttributes = new TLV(TAG_TARGET_AID); // A0
                    ruleString = subClassAttributes.parse(ruleString, false);
                }

                if (ruleString.startsWith(TAG_DODF)) {
                    TLV oidDoTag = new TLV(TAG_DODF); // A1
                    oidDoTag.parse(ruleString, true);
                    ruleString = oidDoTag.getValue();

                    TLV oidDo = new TLV(TAG_ASN_SEQUENCE); // 30
                    oidDo.parse(ruleString, true);
                    ruleString = oidDo.getValue();

                    TLV oidTag = new TLV(TAG_ASN_OID); // 06
                    oidTag.parse(ruleString, false);
                    // Example : [06] 0A 2A 86 48 86 FC 6B 81 48 01 01
                    String oid = oidTag.getValue();
                    if (oid.equals(AC_OID)) {
                        // Skip OID and get the AC to the ACCM
                        ruleString = oidTag.parse(ruleString, false);
                        TLV tlvAsnPath = new TLV(TAG_ASN_SEQUENCE); // 30
                        TLV tlvPath = new TLV(TAG_ASN_OCTET_STRING);  // 04
                        tlvAsnPath.parse(ruleString, true);
                        tlvPath.parse(tlvAsnPath.getValue(), true);
                        return tlvPath.getValue();
                    }
                }
                continue; // skip current rule as it doesn't have expected TAG
            } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
                log("Error: " + ex);
                break; // Bad data, ignore all remaining ACRules
            }
        }
        return ret;
    }

    // parse ACMF file to get file id for ACRF file
    // data is hex string, return file id if parse success, null otherwise
    private String parseAcmf(String data) {
        try {
            // [30] 10 [04] 08 01 02 03 04 05 06 07 08 [30] 04 [04] 02 43 00
            TLV acmfTag = new TLV(TAG_ASN_SEQUENCE); // 30
            acmfTag.parse(data, false);
            String ruleString = acmfTag.getValue();
            TLV refreshTag = new TLV(TAG_ASN_OCTET_STRING); // 04
            String refreshTagLength = refreshTag.parseLength(ruleString);
            if (!refreshTagLength.equals(REFRESH_TAG_LEN)) {
                log("Error: refresh tag in ACMF must be 8.");
                return null;
            }
            ruleString = refreshTag.parse(ruleString, false);
            TLV tlvAsnPath = new TLV(TAG_ASN_SEQUENCE); // 30
            TLV tlvPath = new TLV(TAG_ASN_OCTET_STRING);  // 04
            tlvAsnPath.parse(ruleString, true);
            tlvPath.parse(tlvAsnPath.getValue(), true);
            return tlvPath.getValue();
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            log("Error: " + ex);
            return null;
        }

    }

    // parse ACRF file to get file id for ACCF file
    // data is hex string, return file id if parse success, null otherwise
    private String parseAcrf(String data) {
        String ret = null;

        String acRules = data;
        while (!acRules.isEmpty()) {
            // Example:
            // [30] 10 [A0] 08 04 06 FF FF FF FF FF FF [30] 04 [04] 02 43 10
            // bytes in [] are tags for the data
            TLV tlvRule = new TLV(TAG_ASN_SEQUENCE);
            try {
                acRules = tlvRule.parse(acRules, false);
                String ruleString = tlvRule.getValue();
                if (ruleString.startsWith(TAG_TARGET_AID)) {
                    TLV tlvTarget = new TLV(TAG_TARGET_AID); // A0
                    TLV tlvAid = new TLV(TAG_ASN_OCTET_STRING); // 04
                    TLV tlvAsnPath = new TLV(TAG_ASN_SEQUENCE); // 30
                    TLV tlvPath = new TLV(TAG_ASN_OCTET_STRING);  // 04

                    // populate tlvTarget.value with aid data,
                    // ruleString has remaining data for path
                    ruleString = tlvTarget.parse(ruleString, false);
                    // parse tlvTarget.value to get actual strings for AID.
                    // no other tags expected so shouldConsumeAll is true.
                    tlvAid.parse(tlvTarget.getValue(), true);

                    if (CARRIER_RULE_AID.equals(tlvAid.getValue())) {
                        tlvAsnPath.parse(ruleString, true);
                        tlvPath.parse(tlvAsnPath.getValue(), true);
                        ret = tlvPath.getValue();
                    }
                }
                continue; // skip current rule as it doesn't have expected TAG
            } catch (IllegalArgumentException|IndexOutOfBoundsException ex) {
                log("Error: " + ex);
                break; // Bad data, ignore all remaining ACRules
            }
        }
        return ret;
    }

    // parse ACCF and add to mRules
    private void parseAccf(String data) {
        String acCondition = data;
        while (!acCondition.isEmpty()) {
            TLV tlvCondition = new TLV(TAG_ASN_SEQUENCE);
            TLV tlvCert = new TLV(TAG_ASN_OCTET_STRING);
            try {
                acCondition = tlvCondition.parse(acCondition, false);
                tlvCert.parse(tlvCondition.getValue(), true);
                if (!tlvCert.getValue().isEmpty()) {
                    mRules.add(tlvCert.getValue());
                }
            } catch (IllegalArgumentException|IndexOutOfBoundsException ex) {
                log("Error: " + ex);
                break; // Bad data, ignore all remaining acCondition data
            }
        }
    }

    public List<String> getRules() {
        return mRules;
    }

    private static void log(String msg) {
        if (DBG) Rlog.d(LOG_TAG, msg);
    }

    /**
     * Dumps info to Dumpsys - useful for debugging.
     */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (mRules != null) {
            pw.println(" mRules:");
            for (String cert : mRules) {
                pw.println("  " + cert);
            }
        }
    }
}
