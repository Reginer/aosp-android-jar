/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.telephony.AccessNetworkConstants;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsConferenceState;
import android.telephony.ims.ImsExternalCallState;
import android.telephony.ims.ImsReasonInfo;

import com.android.ims.ImsCall;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.imsphone.ImsExternalCallTracker;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCall;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker;
import com.android.internal.telephony.test.TestConferenceEventPackageParser;
import com.android.internal.telephony.util.TelephonyUtils;
import com.android.telephony.Rlog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Telephony tester receives the following intents where {name} is the phone name
 *
 * adb shell am broadcast -a com.android.internal.telephony.{name}.action_detached
 * adb shell am broadcast -a com.android.internal.telephony.{name}.action_attached
 * adb shell am broadcast -a com.android.internal.telephony.TestConferenceEventPackage -e filename
 *      test_filename.xml
 * adb shell am broadcast -a com.android.internal.telephony.TestServiceState --ei data_rat 10 --ei
 *      data_roaming_type 3
 * adb shell am broadcast -a com.android.internal.telephony.TestServiceState --es action reset
 *
 */
public class TelephonyTester {
    private static final String LOG_TAG = "TelephonyTester";
    private static final boolean DBG = true;

    /**
     * Test-only intent used to send a test conference event package to the IMS framework.
     */
    private static final String ACTION_TEST_CONFERENCE_EVENT_PACKAGE =
            "com.android.internal.telephony.TestConferenceEventPackage";

    /**
     * Test-only intent used to send a test dialog event package to the IMS framework.
     */
    private static final String ACTION_TEST_DIALOG_EVENT_PACKAGE =
            "com.android.internal.telephony.TestDialogEventPackage";

    private static final String EXTRA_FILENAME = "filename";
    /**
     * Used to inject the conference event package by bypassing the ImsCall and doing the
     * injection via ImsPhoneCallTracker.  This is useful in scenarios where the
     * adb shell cmd phone ims conference-event-package disable
     * command is used to disable network CEP data and it is desired to still inject CEP data.
     * Where the network CEP data is not explicitly disabled using the command above, it is not
     * necessary to bypass the ImsCall.
     */
    private static final String EXTRA_BYPASS_IMSCALL = "bypassImsCall";
    private static final String EXTRA_STARTPACKAGE = "startPackage";
    private static final String EXTRA_SENDPACKAGE = "sendPackage";
    private static final String EXTRA_DIALOGID = "dialogId";
    private static final String EXTRA_NUMBER = "number";
    private static final String EXTRA_STATE = "state";
    private static final String EXTRA_CANPULL = "canPull";

    /**
     * Test-only intent used to trigger supp service notification failure.
     */
    private static final String ACTION_TEST_SUPP_SRVC_FAIL =
            "com.android.internal.telephony.TestSuppSrvcFail";
    private static final String EXTRA_FAILURE_CODE = "failureCode";

    /**
     * Test-only intent used to trigger the signalling which occurs when a handover to WIFI fails.
     */
    private static final String ACTION_TEST_HANDOVER_FAIL =
            "com.android.internal.telephony.TestHandoverFail";

    /**
     * Test-only intent used to trigger signalling of a
     * {@link com.android.internal.telephony.gsm.SuppServiceNotification} to the {@link ImsPhone}.
     * Use {@link #EXTRA_CODE} to specify the
     * {@link com.android.internal.telephony.gsm.SuppServiceNotification#code}.
     */
    private static final String ACTION_TEST_SUPP_SRVC_NOTIFICATION =
            "com.android.internal.telephony.TestSuppSrvcNotification";

    private static final String EXTRA_CODE = "code";
    private static final String EXTRA_TYPE = "type";

    /**
     * Test-only intent used to trigger signalling that an IMS call is an emergency call.
     */
    private static final String ACTION_TEST_IMS_E_CALL =
            "com.android.internal.telephony.TestImsECall";

    /**
     * Test-only intent used to trigger signalling that an IMS call received a DTMF tone.
     */
    private static final String ACTION_TEST_RECEIVE_DTMF =
            "com.android.internal.telephony.TestReceiveDtmf";

    private static final String EXTRA_DIGIT = "digit";

    /**
     * Test-only intent used to trigger a change to the current call's phone number.
     * Use the {@link #EXTRA_NUMBER} extra to specify the new phone number.
     */
    private static final String ACTION_TEST_CHANGE_NUMBER =
            "com.android.internal.telephony.TestChangeNumber";

    private static final String ACTION_TEST_SERVICE_STATE =
            "com.android.internal.telephony.TestServiceState";

    private static final String EXTRA_ACTION = "action";
    private static final String EXTRA_VOICE_RAT = "voice_rat";
    private static final String EXTRA_DATA_RAT = "data_rat";
    private static final String EXTRA_VOICE_REG_STATE = "voice_reg_state";
    private static final String EXTRA_DATA_REG_STATE = "data_reg_state";
    private static final String EXTRA_VOICE_ROAMING_TYPE = "voice_roaming_type";
    private static final String EXTRA_DATA_ROAMING_TYPE = "data_roaming_type";
    private static final String EXTRA_NR_FREQUENCY_RANGE = "nr_frequency_range";
    private static final String EXTRA_NR_STATE = "nr_state";
    private static final String EXTRA_OPERATOR = "operator";
    private static final String EXTRA_OPERATOR_RAW = "operator_raw";

    private static final String ACTION_RESET = "reset";

    private static List<ImsExternalCallState> mImsExternalCallStates = null;

    private Intent mServiceStateTestIntent;

    private Phone mPhone;

    // The static intent receiver one for all instances and we assume this
    // is running on the same thread as Dcc.
    protected BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
            @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            try {
                if (DBG) log("sIntentReceiver.onReceive: action=" + action);
                if (action.equals(mPhone.getActionDetached())) {
                    log("simulate detaching");
                    mPhone.getServiceStateTracker().mDetachedRegistrants.get(
                            AccessNetworkConstants.TRANSPORT_TYPE_WWAN).notifyRegistrants();
                } else if (action.equals(mPhone.getActionAttached())) {
                    log("simulate attaching");
                    mPhone.getServiceStateTracker().mAttachedRegistrants.get(
                            AccessNetworkConstants.TRANSPORT_TYPE_WWAN).notifyRegistrants();
                } else if (action.equals(ACTION_TEST_CONFERENCE_EVENT_PACKAGE)) {
                    log("inject simulated conference event package");
                    handleTestConferenceEventPackage(context,
                            intent.getStringExtra(EXTRA_FILENAME),
                            intent.getBooleanExtra(EXTRA_BYPASS_IMSCALL, false));
                } else if (action.equals(ACTION_TEST_DIALOG_EVENT_PACKAGE)) {
                    log("handle test dialog event package intent");
                    handleTestDialogEventPackageIntent(intent);
                } else if (action.equals(ACTION_TEST_SUPP_SRVC_FAIL)) {
                    log("handle test supp svc failed intent");
                    handleSuppServiceFailedIntent(intent);
                } else if (action.equals(ACTION_TEST_HANDOVER_FAIL)) {
                    log("handle handover fail test intent");
                    handleHandoverFailedIntent();
                } else if (action.equals(ACTION_TEST_SUPP_SRVC_NOTIFICATION)) {
                    log("handle supp service notification test intent");
                    sendTestSuppServiceNotification(intent);
                } else if (action.equals(ACTION_TEST_SERVICE_STATE)) {
                    log("handle test service state changed intent");
                    // Trigger the service state update. The replacement will be done in
                    // overrideServiceState().
                    mServiceStateTestIntent = intent;
                    mPhone.getServiceStateTracker().sendEmptyMessage(
                            ServiceStateTracker.EVENT_NETWORK_STATE_CHANGED);
                } else if (action.equals(ACTION_TEST_IMS_E_CALL)) {
                    log("handle test IMS ecall intent");
                    testImsECall();
                } else if (action.equals(ACTION_TEST_RECEIVE_DTMF)) {
                    log("handle test DTMF intent");
                    testImsReceiveDtmf(intent);
                } else if (action.equals(ACTION_TEST_CHANGE_NUMBER)) {
                    log("handle test change number intent");
                    testChangeNumber(intent);
                } else {
                    if (DBG) log("onReceive: unknown action=" + action);
                }
            } catch (BadParcelableException e) {
                Rlog.w(LOG_TAG, e);
            }
        }
    };

    TelephonyTester(Phone phone) {
        mPhone = phone;

        if (TelephonyUtils.IS_DEBUGGABLE) {
            IntentFilter filter = new IntentFilter();

            filter.addAction(mPhone.getActionDetached());
            log("register for intent action=" + mPhone.getActionDetached());

            filter.addAction(mPhone.getActionAttached());
            log("register for intent action=" + mPhone.getActionAttached());

            if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_IMS) {
                log("register for intent action=" + ACTION_TEST_CONFERENCE_EVENT_PACKAGE);
                filter.addAction(ACTION_TEST_CONFERENCE_EVENT_PACKAGE);
                filter.addAction(ACTION_TEST_DIALOG_EVENT_PACKAGE);
                filter.addAction(ACTION_TEST_SUPP_SRVC_FAIL);
                filter.addAction(ACTION_TEST_HANDOVER_FAIL);
                filter.addAction(ACTION_TEST_SUPP_SRVC_NOTIFICATION);
                filter.addAction(ACTION_TEST_IMS_E_CALL);
                filter.addAction(ACTION_TEST_RECEIVE_DTMF);
                mImsExternalCallStates = new ArrayList<ImsExternalCallState>();
            }

            filter.addAction(ACTION_TEST_SERVICE_STATE);
            log("register for intent action=" + ACTION_TEST_SERVICE_STATE);

            filter.addAction(ACTION_TEST_CHANGE_NUMBER);
            log("register for intent action=" + ACTION_TEST_CHANGE_NUMBER);
            phone.getContext().registerReceiver(mIntentReceiver, filter, null, mPhone.getHandler(),
                    Context.RECEIVER_EXPORTED);
        }
    }

    void dispose() {
        if (TelephonyUtils.IS_DEBUGGABLE) {
            mPhone.getContext().unregisterReceiver(mIntentReceiver);
        }
    }

    private static void log(String s) {
        Rlog.d(LOG_TAG, s);
    }

    private void handleSuppServiceFailedIntent(Intent intent) {
        ImsPhone imsPhone = (ImsPhone) mPhone;
        if (imsPhone == null) {
            return;
        }
        int code = intent.getIntExtra(EXTRA_FAILURE_CODE, 0);
        imsPhone.notifySuppServiceFailed(PhoneInternalInterface.SuppService.values()[code]);
    }

    private void handleHandoverFailedIntent() {
        // Attempt to get the active IMS call
        ImsCall imsCall = getImsCall();
        if (imsCall == null) {
            return;
        }

        imsCall.getImsCallSessionListenerProxy().callSessionHandoverFailed(imsCall.getCallSession(),
                TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_IWLAN,
                new ImsReasonInfo());
    }

    /**
     * Handles request to send a test conference event package to the active Ims call.
     *
     * @see com.android.internal.telephony.test.TestConferenceEventPackageParser
     * @param context The context.
     * @param fileName The name of the test conference event package file to read.
     */
    private void handleTestConferenceEventPackage(Context context, String fileName,
            boolean isBypassingImsCall) {
        // Attempt to get the active IMS call before parsing the test XML file.
        ImsPhone imsPhone = (ImsPhone) mPhone;
        if (imsPhone == null) {
            return;
        }

        ImsPhoneCallTracker tracker = (ImsPhoneCallTracker) imsPhone.getCallTracker();

        File packageFile = new File(context.getFilesDir(), fileName);
        final FileInputStream is;
        try {
            is = new FileInputStream(packageFile);
        } catch (FileNotFoundException ex) {
            log("Test conference event package file not found: " + packageFile.getAbsolutePath());
            return;
        }

        TestConferenceEventPackageParser parser = new TestConferenceEventPackageParser(is);
        ImsConferenceState imsConferenceState = parser.parse();
        if (imsConferenceState == null) {
            return;
        }

        if (isBypassingImsCall) {
            tracker.injectTestConferenceState(imsConferenceState);
        } else {
            ImsPhoneCall imsPhoneCall = imsPhone.getForegroundCall();
            if (imsPhoneCall == null) {
                return;
            }

            ImsCall imsCall = imsPhoneCall.getImsCall();
            if (imsCall == null) {
                return;
            }

            imsCall.conferenceStateUpdated(imsConferenceState);
        }
    }

    /**
     * Handles intents containing test dialog event package data.
     *
     * @param intent
     */
    private void handleTestDialogEventPackageIntent(Intent intent) {
        ImsPhone imsPhone = (ImsPhone) mPhone;
        if (imsPhone == null) {
            return;
        }
        ImsExternalCallTracker externalCallTracker = imsPhone.getExternalCallTracker();
        if (externalCallTracker == null) {
            return;
        }

        if (intent.hasExtra(EXTRA_STARTPACKAGE)) {
            mImsExternalCallStates.clear();
        } else if (intent.hasExtra(EXTRA_SENDPACKAGE)) {
            externalCallTracker.refreshExternalCallState(mImsExternalCallStates);
            mImsExternalCallStates.clear();
        } else if (intent.hasExtra(EXTRA_DIALOGID)) {
            ImsExternalCallState state = new ImsExternalCallState(
                    intent.getIntExtra(EXTRA_DIALOGID, 0),
                    Uri.parse(intent.getStringExtra(EXTRA_NUMBER)),
                    intent.getBooleanExtra(EXTRA_CANPULL, true),
                    intent.getIntExtra(EXTRA_STATE,
                            ImsExternalCallState.CALL_STATE_CONFIRMED),
                    ImsCallProfile.CALL_TYPE_VOICE,
                    false /* isHeld */
                    );
            mImsExternalCallStates.add(state);
        }
    }

    private void sendTestSuppServiceNotification(Intent intent) {
        if (intent.hasExtra(EXTRA_CODE) && intent.hasExtra(EXTRA_TYPE)) {
            int code = intent.getIntExtra(EXTRA_CODE, -1);
            int type = intent.getIntExtra(EXTRA_TYPE, -1);
            ImsPhone imsPhone = (ImsPhone) mPhone;
            if (imsPhone == null) {
                return;
            }
            log("Test supp service notification:" + code);
            SuppServiceNotification suppServiceNotification = new SuppServiceNotification();
            suppServiceNotification.code = code;
            suppServiceNotification.notificationType = type;
            imsPhone.notifySuppSvcNotification(suppServiceNotification);
        }
    }

    void overrideServiceState(ServiceState ss) {
        if (mServiceStateTestIntent == null || ss == null) return;
        if (mServiceStateTestIntent.hasExtra(EXTRA_ACTION)
                && ACTION_RESET.equals(mServiceStateTestIntent.getStringExtra(EXTRA_ACTION))) {
            log("Service state override reset");
            return;
        }

        if (mServiceStateTestIntent.hasExtra(EXTRA_VOICE_REG_STATE)) {
            ss.setVoiceRegState(mServiceStateTestIntent.getIntExtra(EXTRA_VOICE_REG_STATE,
                    ServiceState.STATE_OUT_OF_SERVICE));
            log("Override voice service state with " + ss.getState());
        }
        if (mServiceStateTestIntent.hasExtra(EXTRA_DATA_REG_STATE)) {
            ss.setDataRegState(mServiceStateTestIntent.getIntExtra(EXTRA_DATA_REG_STATE,
                    ServiceState.STATE_OUT_OF_SERVICE));
            log("Override data service state with " + ss.getDataRegistrationState());
        }
        if (mServiceStateTestIntent.hasExtra(EXTRA_OPERATOR)) {
            String[] data = mServiceStateTestIntent.getStringExtra(EXTRA_OPERATOR).split(",");
            String operatorAlphaLong = data.length > 0 ? data[0] : "";
            String operatorAlphaShort = data.length > 1 ? data[1] : operatorAlphaLong;
            String operatorNumeric = data.length > 2 ? data[2] : "";
            ss.setOperatorName(operatorAlphaLong, operatorAlphaShort, operatorNumeric);
            log("Override operator with " + Arrays.toString(data));
        }
        if (mServiceStateTestIntent.hasExtra(EXTRA_OPERATOR_RAW)) {
            String operator_raw = mServiceStateTestIntent.getStringExtra(EXTRA_OPERATOR_RAW);
            ss.setOperatorAlphaLongRaw(operator_raw);
            ss.setOperatorAlphaShortRaw(operator_raw);
            log("Override operator_raw with " + operator_raw);
        }
        if (mServiceStateTestIntent.hasExtra(EXTRA_NR_FREQUENCY_RANGE)) {
            ss.setNrFrequencyRange(mServiceStateTestIntent.getIntExtra(EXTRA_NR_FREQUENCY_RANGE,
                    ServiceState.FREQUENCY_RANGE_UNKNOWN));
            log("Override NR frequency range with " + ss.getNrFrequencyRange());
        }
        if (mServiceStateTestIntent.hasExtra(EXTRA_NR_STATE)) {
            NetworkRegistrationInfo nri = ss.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
            if (nri == null) {
                nri = new NetworkRegistrationInfo.Builder()
                        .setDomain(NetworkRegistrationInfo.DOMAIN_PS)
                        .setTransportType(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                        .build();
            }
            nri.setNrState(mServiceStateTestIntent.getIntExtra(EXTRA_NR_STATE,
                    NetworkRegistrationInfo.NR_STATE_NONE));
            ss.addNetworkRegistrationInfo(nri);
            log("Override NR state with " + ss.getNrState());
        }
        if (mServiceStateTestIntent.hasExtra(EXTRA_VOICE_RAT)) {
            NetworkRegistrationInfo nri = ss.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_CS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
            if (nri == null) {
                nri = new NetworkRegistrationInfo.Builder()
                        .setDomain(NetworkRegistrationInfo.DOMAIN_CS)
                        .setTransportType(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                        .build();
            }
            nri.setAccessNetworkTechnology(ServiceState.rilRadioTechnologyToNetworkType(
                    mServiceStateTestIntent.getIntExtra(EXTRA_VOICE_RAT,
                    ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN)));
            ss.addNetworkRegistrationInfo(nri);
            log("Override voice rat with " + ss.getRilVoiceRadioTechnology());
        }
        if (mServiceStateTestIntent.hasExtra(EXTRA_DATA_RAT)) {
            NetworkRegistrationInfo nri = ss.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
            if (nri == null) {
                nri = new NetworkRegistrationInfo.Builder()
                        .setDomain(NetworkRegistrationInfo.DOMAIN_PS)
                        .setTransportType(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                        .build();
            }
            nri.setAccessNetworkTechnology(ServiceState.rilRadioTechnologyToNetworkType(
                    mServiceStateTestIntent.getIntExtra(EXTRA_DATA_RAT,
                    ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN)));
            ss.addNetworkRegistrationInfo(nri);
            log("Override data rat with " + ss.getRilDataRadioTechnology());
        }
        if (mServiceStateTestIntent.hasExtra(EXTRA_VOICE_ROAMING_TYPE)) {
            NetworkRegistrationInfo nri = ss.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_CS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
            if (nri == null) {
                nri = new NetworkRegistrationInfo.Builder()
                        .setDomain(NetworkRegistrationInfo.DOMAIN_CS)
                        .setTransportType(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                        .build();
            }
            nri.setRoamingType(mServiceStateTestIntent.getIntExtra(EXTRA_VOICE_ROAMING_TYPE,
                    ServiceState.ROAMING_TYPE_UNKNOWN));
            ss.addNetworkRegistrationInfo(nri);
            log("Override voice roaming type with " + ss.getVoiceRoamingType());
        }
        if (mServiceStateTestIntent.hasExtra(EXTRA_DATA_ROAMING_TYPE)) {
            NetworkRegistrationInfo nri = ss.getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS, AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
            if (nri == null) {
                nri = new NetworkRegistrationInfo.Builder()
                        .setDomain(NetworkRegistrationInfo.DOMAIN_PS)
                        .setTransportType(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                        .build();
            }
            nri.setRoamingType(mServiceStateTestIntent.getIntExtra(EXTRA_DATA_ROAMING_TYPE,
                    ServiceState.ROAMING_TYPE_UNKNOWN));
            ss.addNetworkRegistrationInfo(nri);
            log("Override data roaming type with " + ss.getDataRoamingType());
        }
    }

    void testImsECall() {
        // Attempt to get the active IMS call before parsing the test XML file.
        ImsCall imsCall = getImsCall();
        if (imsCall == null) return;

        ImsCallProfile callProfile = imsCall.getCallProfile();
        Bundle extras = callProfile.getCallExtras();
        if (extras == null) {
            extras = new Bundle();
        }
        extras.putBoolean(ImsCallProfile.EXTRA_EMERGENCY_CALL, true);
        callProfile.mCallExtras = extras;
        imsCall.getImsCallSessionListenerProxy().callSessionUpdated(imsCall.getSession(),
                callProfile);
    }

    private ImsCall getImsCall() {
        ImsPhone imsPhone = (ImsPhone) mPhone;
        if (imsPhone == null) {
            return null;
        }

        ImsPhoneCall imsPhoneCall = imsPhone.getForegroundCall();
        if (imsPhoneCall == null) {
            return null;
        }

        ImsCall imsCall = imsPhoneCall.getImsCall();
        if (imsCall == null) {
            return null;
        }
        return imsCall;
    }

    void testImsReceiveDtmf(Intent intent) {
        if (!intent.hasExtra(EXTRA_DIGIT)) {
            return;
        }
        char digit = intent.getStringExtra(EXTRA_DIGIT).charAt(0);

        ImsCall imsCall = getImsCall();
        if (imsCall == null) {
            return;
        }

        imsCall.getImsCallSessionListenerProxy().callSessionDtmfReceived(digit);
    }

    void testChangeNumber(Intent intent) {
        if (!intent.hasExtra(EXTRA_NUMBER)) {
            return;
        }

        String newNumber = intent.getStringExtra(EXTRA_NUMBER);

        // Update all the calls.
        mPhone.getForegroundCall().getConnections()
                .stream()
                .forEach(c -> {
                    c.setAddress(newNumber, PhoneConstants.PRESENTATION_ALLOWED);
                    c.setDialString(newNumber);
                });

        // <sigh>
        if (mPhone instanceof GsmCdmaPhone) {
            ((GsmCdmaPhone) mPhone).notifyPhoneStateChanged();
            ((GsmCdmaPhone) mPhone).notifyPreciseCallStateChanged();
        } else if (mPhone instanceof ImsPhone) {
            ((ImsPhone) mPhone).notifyPhoneStateChanged();
            ((ImsPhone) mPhone).notifyPreciseCallStateChanged();
        }
    }
}
