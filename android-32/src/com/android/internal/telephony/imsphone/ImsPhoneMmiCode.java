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

package com.android.internal.telephony.imsphone;

import static android.telephony.CarrierConfigManager.USSD_OVER_CS_ONLY;
import static android.telephony.CarrierConfigManager.USSD_OVER_CS_PREFERRED;
import static android.telephony.CarrierConfigManager.USSD_OVER_IMS_ONLY;
import static android.telephony.CarrierConfigManager.USSD_OVER_IMS_PREFERRED;
import static android.telephony.ServiceState.STATE_IN_SERVICE;

import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_DATA;
import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_DATA_ASYNC;
import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_DATA_SYNC;
import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_FAX;
import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_MAX;
import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_NONE;
import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_PACKET;
import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_PAD;
import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_SMS;
import static com.android.internal.telephony.CommandsInterface.SERVICE_CLASS_VOICE;

import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.ResultReceiver;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsCallForwardInfo;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsSsData;
import android.telephony.ims.ImsSsInfo;
import android.telephony.ims.ImsUtListener;
import android.telephony.ims.stub.ImsUtImplBase;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import com.android.ims.ImsException;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gsm.GsmMmiCode;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.telephony.Rlog;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The motto for this file is:
 *
 * "NOTE:    By using the # as a separator, most cases are expected to be unambiguous."
 *   -- TS 22.030 6.5.2
 *
 * {@hide}
 *
 */
public final class ImsPhoneMmiCode extends Handler implements MmiCode {
    static final String LOG_TAG = "ImsPhoneMmiCode";

    //***** Constants

    // Max Size of the Short Code (aka Short String from TS 22.030 6.5.2)
    private static final int MAX_LENGTH_SHORT_CODE = 2;

    // TS 22.030 6.5.2 Every Short String USSD command will end with #-key
    // (known as #-String)
    private static final char END_OF_USSD_COMMAND = '#';

    // From TS 22.030 6.5.2
    private static final String ACTION_ACTIVATE = "*";
    private static final String ACTION_DEACTIVATE = "#";
    private static final String ACTION_INTERROGATE = "*#";
    private static final String ACTION_REGISTER = "**";
    private static final String ACTION_ERASURE = "##";

    // Supp Service codes from TS 22.030 Annex B

    //Called line presentation
    private static final String SC_CLIP    = "30";
    private static final String SC_CLIR    = "31";
    private static final String SC_COLP    = "76";
    private static final String SC_COLR    = "77";

    //Calling name presentation
    private static final String SC_CNAP    = "300";

    // Call Forwarding
    private static final String SC_CFU     = "21";
    private static final String SC_CFB     = "67";
    private static final String SC_CFNRy   = "61";
    private static final String SC_CFNR    = "62";
    // Call Forwarding unconditional Timer
    private static final String SC_CFUT     = "22";

    private static final String SC_CF_All = "002";
    private static final String SC_CF_All_Conditional = "004";

    // Call Waiting
    private static final String SC_WAIT     = "43";

    // Call Barring
    private static final String SC_BAOC         = "33";
    private static final String SC_BAOIC        = "331";
    private static final String SC_BAOICxH      = "332";
    private static final String SC_BAIC         = "35";
    private static final String SC_BAICr        = "351";

    private static final String SC_BA_ALL       = "330";
    private static final String SC_BA_MO        = "333";
    private static final String SC_BA_MT        = "353";

    // Incoming/Anonymous call barring
    private static final String SC_BS_MT        = "156";
    private static final String SC_BAICa        = "157";

    // Supp Service Password registration
    private static final String SC_PWD          = "03";

    // PIN/PIN2/PUK/PUK2
    private static final String SC_PIN          = "04";
    private static final String SC_PIN2         = "042";
    private static final String SC_PUK          = "05";
    private static final String SC_PUK2         = "052";

    //***** Event Constants

    private static final int EVENT_SET_COMPLETE            = 0;
    private static final int EVENT_QUERY_CF_COMPLETE       = 1;
    private static final int EVENT_USSD_COMPLETE           = 2;
    private static final int EVENT_QUERY_COMPLETE          = 3;
    private static final int EVENT_SET_CFF_COMPLETE        = 4;
    private static final int EVENT_USSD_CANCEL_COMPLETE    = 5;
    private static final int EVENT_GET_CLIR_COMPLETE       = 6;
    private static final int EVENT_SUPP_SVC_QUERY_COMPLETE = 7;
    private static final int EVENT_QUERY_ICB_COMPLETE      = 10;

    //***** Calling Line Presentation Constants
    private static final int NUM_PRESENTATION_ALLOWED     = 0;
    private static final int NUM_PRESENTATION_RESTRICTED  = 1;

    //***** Supplementary Service Query Bundle Keys
    // Used by IMS Service layer to put supp. serv. query
    // responses into the ssInfo Bundle.
    /**
     * @deprecated Use {@link ImsUtListener#onLineIdentificationSupplementaryServiceResponse(int,
     * ImsSsInfo)} API instead.
     */
    @Deprecated
    // Not used, only kept around to not break vendors using this key.
    public static final String UT_BUNDLE_KEY_CLIR = "queryClir";
    /**
     * @deprecated Use {@link ImsUtListener#onLineIdentificationSupplementaryServiceResponse(int,
     * ImsSsInfo)} API instead.
     */
    @Deprecated
    // Not used, only kept around to not break vendors using this key.
    public static final String UT_BUNDLE_KEY_SSINFO = "imsSsInfo";

    //***** Instance Variables

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private ImsPhone mPhone;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private Context mContext;
    private IccRecords mIccRecords;

    private String mAction;              // One of ACTION_*
    private String mSc;                  // Service Code
    private String mSia, mSib, mSic;       // Service Info a,b,c
    private String mPoundString;         // Entire MMI string up to and including #
    private String mDialingNumber;
    private String mPwd;                 // For password registration
    private ResultReceiver mCallbackReceiver;

    private boolean mIsPendingUSSD;

    private boolean mIsUssdRequest;

    private boolean mIsCallFwdReg;

    private boolean mIsNetworkInitiatedUSSD;

    private State mState = State.PENDING;
    private CharSequence mMessage;
    private boolean mIsSsInfo = false;
    //resgister/erasure of ICB (Specific DN)
    static final String IcbDnMmi = "Specific Incoming Call Barring";
    //ICB (Anonymous)
    static final String IcbAnonymousMmi = "Anonymous Incoming Call Barring";
    //***** Class Variables


    // See TS 22.030 6.5.2 "Structure of the MMI"

    private static Pattern sPatternSuppService = Pattern.compile(
        "((\\*|#|\\*#|\\*\\*|##)(\\d{2,3})(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*)(\\*([^*#]*))?)?)?)?#)(.*)");
/*       1  2                    3          4  5       6   7         8    9     10  11             12

         1 = Full string up to and including #
         2 = action (activation/interrogation/registration/erasure)
         3 = service code
         5 = SIA
         7 = SIB
         9 = SIC
         10 = dialing number
*/

    private static final int MATCH_GROUP_POUND_STRING = 1;

    private static final int MATCH_GROUP_ACTION = 2;
                        //(activation/interrogation/registration/erasure)

    private static final int MATCH_GROUP_SERVICE_CODE = 3;
    private static final int MATCH_GROUP_SIA = 5;
    private static final int MATCH_GROUP_SIB = 7;
    private static final int MATCH_GROUP_SIC = 9;
    private static final int MATCH_GROUP_PWD_CONFIRM = 11;
    private static final int MATCH_GROUP_DIALING_NUMBER = 12;
    static private String[] sTwoDigitNumberPattern;

    //***** Public Class methods

    /**
     * Some dial strings in GSM are defined to do non-call setup
     * things, such as modify or query supplementary service settings (eg, call
     * forwarding). These are generally referred to as "MMI codes".
     * We look to see if the dial string contains a valid MMI code (potentially
     * with a dial string at the end as well) and return info here.
     *
     * If the dial string contains no MMI code, we return an instance with
     * only "dialingNumber" set
     *
     * Please see flow chart in TS 22.030 6.5.3.2
     */

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @VisibleForTesting
    public static ImsPhoneMmiCode newFromDialString(String dialString, ImsPhone phone) {
       return newFromDialString(dialString, phone, null);
    }

    static ImsPhoneMmiCode newFromDialString(String dialString,
                                             ImsPhone phone, ResultReceiver wrappedCallback) {
        Matcher m;
        ImsPhoneMmiCode ret = null;

        if ((phone.getDefaultPhone().getServiceState().getVoiceRoaming()
                && phone.getDefaultPhone().supportsConversionOfCdmaCallerIdMmiCodesWhileRoaming())
                        || (isEmergencyNumber(phone, dialString)
                                && isCarrierSupportCallerIdVerticalServiceCodes(phone))) {
            /* The CDMA MMI coded dialString will be converted to a 3GPP MMI Coded dialString
               so that it can be processed by the matcher and code below. This can be triggered if
               the dialing string is an emergency number and carrier supports caller ID vertical
               service codes *67, *82.
             */
            dialString = convertCdmaMmiCodesTo3gppMmiCodes(dialString);
        }

        m = sPatternSuppService.matcher(dialString);

        // Is this formatted like a standard supplementary service code?
        if (m.matches()) {
            ret = new ImsPhoneMmiCode(phone);
            ret.mPoundString = makeEmptyNull(m.group(MATCH_GROUP_POUND_STRING));
            ret.mAction = makeEmptyNull(m.group(MATCH_GROUP_ACTION));
            ret.mSc = makeEmptyNull(m.group(MATCH_GROUP_SERVICE_CODE));
            ret.mSia = makeEmptyNull(m.group(MATCH_GROUP_SIA));
            ret.mSib = makeEmptyNull(m.group(MATCH_GROUP_SIB));
            ret.mSic = makeEmptyNull(m.group(MATCH_GROUP_SIC));
            ret.mPwd = makeEmptyNull(m.group(MATCH_GROUP_PWD_CONFIRM));
            ret.mDialingNumber = makeEmptyNull(m.group(MATCH_GROUP_DIALING_NUMBER));
            ret.mCallbackReceiver = wrappedCallback;
            // According to TS 22.030 6.5.2 "Structure of the MMI",
            // the dialing number should not ending with #.
            // The dialing number ending # is treated as unique USSD,
            // eg, *400#16 digit number# to recharge the prepaid card
            // in India operator(Mumbai MTNL)
            if (ret.mDialingNumber != null &&
                    ret.mDialingNumber.endsWith("#") &&
                    dialString.endsWith("#")){
                ret = new ImsPhoneMmiCode(phone);
                ret.mPoundString = dialString;
            }
        } else if (dialString.endsWith("#")) {
            // TS 22.030 sec 6.5.3.2
            // "Entry of any characters defined in the 3GPP TS 23.038 [8] Default Alphabet
            // (up to the maximum defined in 3GPP TS 24.080 [10]), followed by #SEND".

            ret = new ImsPhoneMmiCode(phone);
            ret.mPoundString = dialString;
        } else if (GsmMmiCode.isTwoDigitShortCode(phone.getContext(), phone.getSubId(),
                dialString)) {
            //Is a country-specific exception to short codes as defined in TS 22.030, 6.5.3.2
            ret = null;
        } else if (isShortCode(dialString, phone)) {
            // this may be a short code, as defined in TS 22.030, 6.5.3.2
            ret = new ImsPhoneMmiCode(phone);
            ret.mDialingNumber = dialString;
        }
        return ret;
    }

    private static String convertCdmaMmiCodesTo3gppMmiCodes(String dialString) {
        Matcher m;
        m = sPatternCdmaMmiCodeWhileRoaming.matcher(dialString);

        if (m.matches()) {
            String serviceCode = makeEmptyNull(m.group(MATCH_GROUP_CDMA_MMI_CODE_SERVICE_CODE));
            String prefix = m.group(MATCH_GROUP_CDMA_MMI_CODE_NUMBER_PREFIX);
            String number = makeEmptyNull(m.group(MATCH_GROUP_CDMA_MMI_CODE_NUMBER));

            if (serviceCode.equals("67") && number != null) {
                // "#31#number" to invoke CLIR
                dialString = ACTION_DEACTIVATE + SC_CLIR + ACTION_DEACTIVATE + prefix + number;
            } else if (serviceCode.equals("82") && number != null) {
                // "*31#number" to suppress CLIR
                dialString = ACTION_ACTIVATE + SC_CLIR + ACTION_DEACTIVATE + prefix + number;
            }
        }
        return dialString;
    }

    public static ImsPhoneMmiCode newNetworkInitiatedUssd(String ussdMessage,
            boolean isUssdRequest, ImsPhone phone) {
        ImsPhoneMmiCode ret;

        ret = new ImsPhoneMmiCode(phone);

        ret.mMessage = ussdMessage;
        ret.mIsUssdRequest = isUssdRequest;
        ret.mIsNetworkInitiatedUSSD = true;

        // If it's a request, set to PENDING so that it's cancelable.
        if (isUssdRequest) {
            ret.mIsPendingUSSD = true;
            ret.mState = State.PENDING;
        } else {
            ret.mState = State.COMPLETE;
        }

        return ret;
    }

    static ImsPhoneMmiCode newFromUssdUserInput(String ussdMessge, ImsPhone phone) {
        ImsPhoneMmiCode ret = new ImsPhoneMmiCode(phone);

        ret.mMessage = ussdMessge;
        ret.mState = State.PENDING;
        ret.mIsPendingUSSD = true;

        return ret;
    }

    //***** Private Class methods

    /** make empty strings be null.
     *  Regexp returns empty strings for empty groups
     */
    private static String
    makeEmptyNull (String s) {
        if (s != null && s.length() == 0) return null;

        return s;
    }

    static boolean isScMatchesSuppServType(String dialString) {
        boolean isMatch = false;
        Matcher m = sPatternSuppService.matcher(dialString);
        if (m.matches()) {
            String sc = makeEmptyNull(m.group(MATCH_GROUP_SERVICE_CODE));
            if (sc.equals(SC_CFUT)) {
                isMatch = true;
            } else if(sc.equals(SC_BS_MT)) {
                isMatch = true;
            }
        }
        return isMatch;
    }

    /** returns true of the string is empty or null */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private static boolean
    isEmptyOrNull(CharSequence s) {
        return s == null || (s.length() == 0);
    }

    private static int
    scToCallForwardReason(String sc) {
        if (sc == null) {
            throw new RuntimeException ("invalid call forward sc");
        }

        if (sc.equals(SC_CF_All)) {
           return CommandsInterface.CF_REASON_ALL;
        } else if (sc.equals(SC_CFU)) {
            return CommandsInterface.CF_REASON_UNCONDITIONAL;
        } else if (sc.equals(SC_CFB)) {
            return CommandsInterface.CF_REASON_BUSY;
        } else if (sc.equals(SC_CFNR)) {
            return CommandsInterface.CF_REASON_NOT_REACHABLE;
        } else if (sc.equals(SC_CFNRy)) {
            return CommandsInterface.CF_REASON_NO_REPLY;
        } else if (sc.equals(SC_CF_All_Conditional)) {
           return CommandsInterface.CF_REASON_ALL_CONDITIONAL;
        } else {
            throw new RuntimeException ("invalid call forward sc");
        }
    }

    private static int
    siToServiceClass(String si) {
        if (si == null || si.length() == 0) {
                return  SERVICE_CLASS_NONE;
        } else {
            // NumberFormatException should cause MMI fail
            int serviceCode = Integer.parseInt(si, 10);

            switch (serviceCode) {
                case 10: return SERVICE_CLASS_SMS + SERVICE_CLASS_FAX  + SERVICE_CLASS_VOICE;
                case 11: return SERVICE_CLASS_VOICE;
                case 12: return SERVICE_CLASS_SMS + SERVICE_CLASS_FAX;
                case 13: return SERVICE_CLASS_FAX;

                case 16: return SERVICE_CLASS_SMS;

                case 19: return SERVICE_CLASS_FAX + SERVICE_CLASS_VOICE;

                case 20: return SERVICE_CLASS_DATA_ASYNC + SERVICE_CLASS_DATA_SYNC;

                case 21: return SERVICE_CLASS_PAD + SERVICE_CLASS_DATA_ASYNC;
                case 22: return SERVICE_CLASS_PACKET + SERVICE_CLASS_DATA_SYNC;
                case 24: return SERVICE_CLASS_DATA_SYNC;
                case 25: return SERVICE_CLASS_DATA_ASYNC;
                case 26: return SERVICE_CLASS_DATA_SYNC + SERVICE_CLASS_VOICE;
                case 99: return SERVICE_CLASS_PACKET;

                default:
                    throw new RuntimeException("unsupported MMI service code " + si);
            }
        }
    }

    private static int
    siToTime (String si) {
        if (si == null || si.length() == 0) {
            return 0;
        } else {
            // NumberFormatException should cause MMI fail
            return Integer.parseInt(si, 10);
        }
    }

    static boolean
    isServiceCodeCallForwarding(String sc) {
        return sc != null &&
                (sc.equals(SC_CFU)
                || sc.equals(SC_CFB) || sc.equals(SC_CFNRy)
                || sc.equals(SC_CFNR) || sc.equals(SC_CF_All)
                || sc.equals(SC_CF_All_Conditional));
    }

    static boolean
    isServiceCodeCallBarring(String sc) {
        Resources resource = Resources.getSystem();
        if (sc != null) {
            String[] barringMMI = resource.getStringArray(
                com.android.internal.R.array.config_callBarringMMI_for_ims);
            if (barringMMI != null) {
                for (String match : barringMMI) {
                    if (sc.equals(match)) return true;
                }
            }
        }
        return false;
    }

    static boolean isPinPukCommand(String sc) {
        return sc != null && (sc.equals(SC_PIN) || sc.equals(SC_PIN2)
                || sc.equals(SC_PUK) || sc.equals(SC_PUK2));
    }

    /**
     * Whether the dial string is supplementary service code.
     *
     * @param dialString The dial string.
     * @return true if the dial string is supplementary service code, and {@code false} otherwise.
     */
    public static boolean isSuppServiceCodes(String dialString, Phone phone) {
        if (phone != null && phone.getServiceState().getVoiceRoaming()
                && phone.getDefaultPhone().supportsConversionOfCdmaCallerIdMmiCodesWhileRoaming()) {
            /* The CDMA MMI coded dialString will be converted to a 3GPP MMI Coded dialString
               so that it can be processed by the matcher and code below
             */
            dialString = convertCdmaMmiCodesTo3gppMmiCodes(dialString);
        }

        Matcher m = sPatternSuppService.matcher(dialString);
        if (m.matches()) {
            String sc = makeEmptyNull(m.group(MATCH_GROUP_SERVICE_CODE));
            if (isServiceCodeCallForwarding(sc)) {
                return true;
            } else if (isServiceCodeCallBarring(sc)) {
                return true;
            } else if (sc != null && sc.equals(SC_CFUT)) {
                return true;
            } else if (sc != null && sc.equals(SC_CLIP)) {
                return true;
            } else if (sc != null && sc.equals(SC_CLIR)) {
                return true;
            } else if (sc != null && sc.equals(SC_COLP)) {
                return true;
            } else if (sc != null && sc.equals(SC_COLR)) {
                return true;
            } else if (sc != null && sc.equals(SC_CNAP)) {
                return true;
            } else if (sc != null && sc.equals(SC_BS_MT)) {
                return true;
            } else if (sc != null && sc.equals(SC_BAICa)) {
                return true;
            } else if (sc != null && sc.equals(SC_PWD)) {
                return true;
            } else if (sc != null && sc.equals(SC_WAIT)) {
                return true;
            } else if (isPinPukCommand(sc)) {
                return true;
            }
        }
        return false;
    }

    static String
    scToBarringFacility(String sc) {
        if (sc == null) {
            throw new RuntimeException ("invalid call barring sc");
        }

        if (sc.equals(SC_BAOC)) {
            return CommandsInterface.CB_FACILITY_BAOC;
        } else if (sc.equals(SC_BAOIC)) {
            return CommandsInterface.CB_FACILITY_BAOIC;
        } else if (sc.equals(SC_BAOICxH)) {
            return CommandsInterface.CB_FACILITY_BAOICxH;
        } else if (sc.equals(SC_BAIC)) {
            return CommandsInterface.CB_FACILITY_BAIC;
        } else if (sc.equals(SC_BAICr)) {
            return CommandsInterface.CB_FACILITY_BAICr;
        } else if (sc.equals(SC_BA_ALL)) {
            return CommandsInterface.CB_FACILITY_BA_ALL;
        } else if (sc.equals(SC_BA_MO)) {
            return CommandsInterface.CB_FACILITY_BA_MO;
        } else if (sc.equals(SC_BA_MT)) {
            return CommandsInterface.CB_FACILITY_BA_MT;
        } else {
            throw new RuntimeException ("invalid call barring sc");
        }
    }

    //***** Constructor

    public ImsPhoneMmiCode(ImsPhone phone) {
        // The telephony unit-test cases may create ImsPhoneMmiCode's
        // in secondary threads
        super(phone.getHandler().getLooper());
        mPhone = phone;
        mContext = phone.getContext();
        mIccRecords = mPhone.mDefaultPhone.getIccRecords();
    }

    //***** MmiCode implementation

    @Override
    public State
    getState() {
        return mState;
    }

    @Override
    public CharSequence
    getMessage() {
        return mMessage;
    }

    @Override
    public Phone getPhone() { return mPhone; }

    // inherited javadoc suffices
    @Override
    public void
    cancel() {
        // Complete or failed cannot be cancelled
        if (mState == State.COMPLETE || mState == State.FAILED) {
            return;
        }

        mState = State.CANCELLED;

        if (mIsPendingUSSD) {
            mPhone.cancelUSSD(obtainMessage(EVENT_USSD_CANCEL_COMPLETE, this));
        } else {
            mPhone.onMMIDone (this);
        }

    }

    @Override
    public boolean isCancelable() {
        /* Can only cancel pending USSD sessions. */
        return mIsPendingUSSD;
    }

    //***** Instance Methods

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    String getDialingNumber() {
        return mDialingNumber;
    }

    /** Does this dial string contain a structured or unstructured MMI code? */
    boolean
    isMMI() {
        return mPoundString != null;
    }

    /* Is this a 1 or 2 digit "short code" as defined in TS 22.030 sec 6.5.3.2? */
    boolean
    isShortCode() {
        return mPoundString == null
                    && mDialingNumber != null && mDialingNumber.length() <= 2;

    }

    @Override
    public String getDialString() {
        return mPoundString;
    }

    /**
     * Helper function for newFromDialString. Returns true if dialString appears
     * to be a short code AND conditions are correct for it to be treated as
     * such.
     */
    static private boolean isShortCode(String dialString, ImsPhone phone) {
        // Refer to TS 22.030 Figure 3.5.3.2:
        if (dialString == null) {
            return false;
        }

        // Illegal dial string characters will give a ZERO length.
        // At this point we do not want to crash as any application with
        // call privileges may send a non dial string.
        // It return false as when the dialString is equal to NULL.
        if (dialString.length() == 0) {
            return false;
        }
        if (isEmergencyNumber(phone, dialString)) {
            return false;
        } else {
            return isShortCodeUSSD(dialString, phone);
        }
    }

    /**
     * Helper function for isShortCode. Returns true if dialString appears to be
     * a short code and it is a USSD structure
     *
     * According to the 3PGG TS 22.030 specification Figure 3.5.3.2: A 1 or 2
     * digit "short code" is treated as USSD if it is entered while on a call or
     * does not satisfy the condition (exactly 2 digits && starts with '1'), there
     * are however exceptions to this rule (see below)
     *
     * Exception (1) to Call initiation is: If the user of the device is already in a call
     * and enters a Short String without any #-key at the end and the length of the Short String is
     * equal or less then the MAX_LENGTH_SHORT_CODE [constant that is equal to 2]
     *
     * The phone shall initiate a USSD/SS commands.
     */
    static private boolean isShortCodeUSSD(String dialString, ImsPhone phone) {
        if (dialString != null && dialString.length() <= MAX_LENGTH_SHORT_CODE) {
            if (phone.isInCall()) {
                return true;
            }

            if (dialString.length() != MAX_LENGTH_SHORT_CODE ||
                    dialString.charAt(0) != '1') {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the Service Code is PIN/PIN2/PUK/PUK2-related
     */
    public boolean isPinPukCommand() {
        return isPinPukCommand(mSc);
    }

    /**
     * See TS 22.030 Annex B.
     * In temporary mode, to suppress CLIR for a single call, enter:
     *      " * 31 # [called number] SEND "
     *  In temporary mode, to invoke CLIR for a single call enter:
     *       " # 31 # [called number] SEND "
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    @VisibleForTesting
    public boolean isTemporaryModeCLIR() {
        return mSc != null && mSc.equals(SC_CLIR)
                && mDialingNumber != null && (isActivate() || isDeactivate());
    }

    /**
     * Checks if the dialing string is an emergency number.
     */
    @VisibleForTesting
    public static boolean isEmergencyNumber(Phone phone, String dialString) {
        try {
            TelephonyManager tm = phone.getContext().getSystemService(TelephonyManager.class);
            return tm.isEmergencyNumber(dialString);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * Checks if carrier supports caller id vertical service codes by checking with
     * {@link CarrierConfigManager#KEY_CARRIER_SUPPORTS_CALLER_ID_VERTICAL_SERVICE_CODES_BOOL}.
     */
    @VisibleForTesting
    public static boolean isCarrierSupportCallerIdVerticalServiceCodes(Phone phone) {
        CarrierConfigManager configManager = phone.getContext().getSystemService(
                CarrierConfigManager.class);
        PersistableBundle b = null;
        if (configManager != null) {
            // If an invalid subId is used, this bundle will contain default values.
            b = configManager.getConfigForSubId(phone.getSubId());
        }
        if (b != null) {
            return b == null ? false : b.getBoolean(CarrierConfigManager
                .KEY_CARRIER_SUPPORTS_CALLER_ID_VERTICAL_SERVICE_CODES_BOOL);
        }
        return false;
    }

    /**
     * returns CommandsInterface.CLIR_*
     * See also isTemporaryModeCLIR()
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    int
    getCLIRMode() {
        if (mSc != null && mSc.equals(SC_CLIR)) {
            if (isActivate()) {
                return CommandsInterface.CLIR_SUPPRESSION;
            } else if (isDeactivate()) {
                return CommandsInterface.CLIR_INVOCATION;
            }
        }

        return CommandsInterface.CLIR_DEFAULT;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    boolean isActivate() {
        return mAction != null && mAction.equals(ACTION_ACTIVATE);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    boolean isDeactivate() {
        return mAction != null && mAction.equals(ACTION_DEACTIVATE);
    }

    boolean isInterrogate() {
        return mAction != null && mAction.equals(ACTION_INTERROGATE);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    boolean isRegister() {
        return mAction != null && mAction.equals(ACTION_REGISTER);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    boolean isErasure() {
        return mAction != null && mAction.equals(ACTION_ERASURE);
    }

    /**
     * Returns true if this is a USSD code that's been submitted to the
     * network...eg, after processCode() is called
     */
    public boolean isPendingUSSD() {
        return mIsPendingUSSD;
    }

    @Override
    public boolean isUssdRequest() {
        return mIsUssdRequest;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    boolean
    isSupportedOverImsPhone() {
        if (isShortCode()) return true;
        else if (isServiceCodeCallForwarding(mSc)
                || isServiceCodeCallBarring(mSc)
                || (mSc != null && mSc.equals(SC_WAIT))
                || (mSc != null && mSc.equals(SC_CLIR))
                || (mSc != null && mSc.equals(SC_CLIP))
                || (mSc != null && mSc.equals(SC_COLR))
                || (mSc != null && mSc.equals(SC_COLP))
                || (mSc != null && mSc.equals(SC_BS_MT))
                || (mSc != null && mSc.equals(SC_BAICa))) {

            try {
                int serviceClass = siToServiceClass(mSib);
                if (serviceClass != SERVICE_CLASS_NONE
                        && serviceClass != SERVICE_CLASS_VOICE
                        && serviceClass != (SERVICE_CLASS_PACKET
                            + SERVICE_CLASS_DATA_SYNC)) {
                    return false;
                }
                return true;
            } catch (RuntimeException exc) {
                Rlog.d(LOG_TAG, "Invalid service class " + exc);
            }
        } else if (isPinPukCommand()
                || (mSc != null
                    && (mSc.equals(SC_PWD) || mSc.equals(SC_CLIP) || mSc.equals(SC_CLIR)))) {
            return false;
        } else if (mPoundString != null) return true;

        return false;
    }

    /*
     * The below actions are IMS/Volte CallBarring actions.We have not defined
     * these actions in ImscommandInterface.However we have reused existing
     * actions of CallForwarding as, both CF and CB actions are used for same
     * purpose.
     */
    public int callBarAction(String dialingNumber) {
        if (isActivate()) {
            return CommandsInterface.CF_ACTION_ENABLE;
        } else if (isDeactivate()) {
            return CommandsInterface.CF_ACTION_DISABLE;
        } else if (isRegister()) {
            if (!isEmptyOrNull(dialingNumber)) {
                return CommandsInterface.CF_ACTION_REGISTRATION;
            } else {
                throw new RuntimeException ("invalid action");
            }
        } else if (isErasure()) {
            return CommandsInterface.CF_ACTION_ERASURE;
        } else {
            throw new RuntimeException ("invalid action");
        }
    }

    /** Process a MMI code or short code...anything that isn't a dialing number */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public void
    processCode () throws CallStateException {
        try {
            if (isShortCode()) {
                Rlog.d(LOG_TAG, "processCode: isShortCode");

                // These just get treated as USSD.
                Rlog.d(LOG_TAG, "processCode: Sending short code '"
                       + mDialingNumber + "' over CS pipe.");
                throw new CallStateException(Phone.CS_FALLBACK);
            } else if (isServiceCodeCallForwarding(mSc)) {
                Rlog.d(LOG_TAG, "processCode: is CF");

                String dialingNumber = mSia;
                int reason = scToCallForwardReason(mSc);
                int serviceClass = siToServiceClass(mSib);
                int time = siToTime(mSic);

                if (isInterrogate()) {
                    mPhone.getCallForwardingOption(reason, serviceClass,
                            obtainMessage(EVENT_QUERY_CF_COMPLETE, this));
                } else {
                    int cfAction;

                    if (isActivate()) {
                        // 3GPP TS 22.030 6.5.2
                        // a call forwarding request with a single * would be
                        // interpreted as registration if containing a forwarded-to
                        // number, or an activation if not
                        if (isEmptyOrNull(dialingNumber)) {
                            cfAction = CommandsInterface.CF_ACTION_ENABLE;
                            mIsCallFwdReg = false;
                        } else {
                            cfAction = CommandsInterface.CF_ACTION_REGISTRATION;
                            mIsCallFwdReg = true;
                        }
                    } else if (isDeactivate()) {
                        cfAction = CommandsInterface.CF_ACTION_DISABLE;
                    } else if (isRegister()) {
                        cfAction = CommandsInterface.CF_ACTION_REGISTRATION;
                    } else if (isErasure()) {
                        cfAction = CommandsInterface.CF_ACTION_ERASURE;
                    } else {
                        throw new RuntimeException ("invalid action");
                    }

                    int isSettingUnconditional =
                            ((reason == CommandsInterface.CF_REASON_UNCONDITIONAL) ||
                             (reason == CommandsInterface.CF_REASON_ALL)) ? 1 : 0;

                    int isEnableDesired =
                        ((cfAction == CommandsInterface.CF_ACTION_ENABLE) ||
                                (cfAction == CommandsInterface.CF_ACTION_REGISTRATION)) ? 1 : 0;

                    Rlog.d(LOG_TAG, "processCode: is CF setCallForward");
                    mPhone.setCallForwardingOption(cfAction, reason,
                            dialingNumber, serviceClass, time, obtainMessage(
                                    EVENT_SET_CFF_COMPLETE,
                                    isSettingUnconditional,
                                    isEnableDesired, this));
                }
            } else if (isServiceCodeCallBarring(mSc)) {
                // sia = password
                // sib = basic service group
                // service group is not supported

                String password = mSia;
                String facility = scToBarringFacility(mSc);
                int serviceClass = siToServiceClass(mSib);

                if (isInterrogate()) {
                    mPhone.getCallBarring(facility,
                            obtainMessage(EVENT_SUPP_SVC_QUERY_COMPLETE, this), serviceClass);
                } else if (isActivate() || isDeactivate()) {
                    mPhone.setCallBarring(facility, isActivate(), password,
                            obtainMessage(EVENT_SET_COMPLETE, this), serviceClass);
                } else {
                    throw new RuntimeException ("Invalid or Unsupported MMI Code");
                }
            } else if (mSc != null && mSc.equals(SC_CLIR)) {
                // NOTE: Since these supplementary services are accessed only
                //       via MMI codes, methods have not been added to ImsPhone.
                //       Only the UT interface handle is used.
                if (isActivate()
                        && !mPhone.getDefaultPhone().isClirActivationAndDeactivationPrevented()) {
                    try {
                        mPhone.setOutgoingCallerIdDisplay(CommandsInterface.CLIR_INVOCATION,
                            obtainMessage(EVENT_SET_COMPLETE, this));
                    } catch (Exception e) {
                        Rlog.d(LOG_TAG, "processCode: Could not get UT handle for updateCLIR.");
                    }
                } else if (isDeactivate()
                        && !mPhone.getDefaultPhone().isClirActivationAndDeactivationPrevented()) {
                    try {
                        mPhone.setOutgoingCallerIdDisplay(CommandsInterface.CLIR_SUPPRESSION,
                            obtainMessage(EVENT_SET_COMPLETE, this));
                    } catch (Exception e) {
                        Rlog.d(LOG_TAG, "processCode: Could not get UT handle for updateCLIR.");
                    }
                } else if (isInterrogate()) {
                    try {
                        mPhone.getOutgoingCallerIdDisplay(obtainMessage(EVENT_GET_CLIR_COMPLETE, this));
                    } catch (Exception e) {
                        Rlog.d(LOG_TAG, "processCode: Could not get UT handle for queryCLIR.");
                    }
                } else {
                    throw new RuntimeException ("Invalid or Unsupported MMI Code");
                }
            } else if (mSc != null && mSc.equals(SC_CLIP)) {
                // NOTE: Refer to the note above.
                if (isInterrogate()) {
                    try {
                        mPhone.queryCLIP(obtainMessage(EVENT_SUPP_SVC_QUERY_COMPLETE, this));
                    } catch (Exception e) {
                        Rlog.d(LOG_TAG, "processCode: Could not get UT handle for queryCLIP.");
                    }
                } else if (isActivate() || isDeactivate()) {
                    try {
                        mPhone.mCT.getUtInterface().updateCLIP(isActivate(),
                                obtainMessage(EVENT_SET_COMPLETE, this));
                    } catch (ImsException e) {
                        Rlog.d(LOG_TAG, "processCode: Could not get UT handle for updateCLIP.");
                    }
                } else {
                    throw new RuntimeException ("Invalid or Unsupported MMI Code");
                }
            } else if (mSc != null && mSc.equals(SC_COLP)) {
                // NOTE: Refer to the note above.
                if (isInterrogate()) {
                    try {
                        mPhone.mCT.getUtInterface()
                            .queryCOLP(obtainMessage(EVENT_SUPP_SVC_QUERY_COMPLETE, this));
                    } catch (ImsException e) {
                        Rlog.d(LOG_TAG, "processCode: Could not get UT handle for queryCOLP.");
                    }
                } else if (isActivate() || isDeactivate()) {
                    try {
                        mPhone.mCT.getUtInterface().updateCOLP(isActivate(),
                                 obtainMessage(EVENT_SET_COMPLETE, this));
                     } catch (ImsException e) {
                         Rlog.d(LOG_TAG, "processCode: Could not get UT handle for updateCOLP.");
                     }
                } else {
                    throw new RuntimeException ("Invalid or Unsupported MMI Code");
                }
            } else if (mSc != null && mSc.equals(SC_COLR)) {
                // NOTE: Refer to the note above.
                if (isActivate()) {
                    try {
                        mPhone.mCT.getUtInterface().updateCOLR(NUM_PRESENTATION_RESTRICTED,
                                obtainMessage(EVENT_SET_COMPLETE, this));
                    } catch (ImsException e) {
                        Rlog.d(LOG_TAG, "processCode: Could not get UT handle for updateCOLR.");
                    }
                } else if (isDeactivate()) {
                    try {
                        mPhone.mCT.getUtInterface().updateCOLR(NUM_PRESENTATION_ALLOWED,
                                obtainMessage(EVENT_SET_COMPLETE, this));
                    } catch (ImsException e) {
                        Rlog.d(LOG_TAG, "processCode: Could not get UT handle for updateCOLR.");
                    }
                } else if (isInterrogate()) {
                    try {
                        mPhone.mCT.getUtInterface()
                            .queryCOLR(obtainMessage(EVENT_SUPP_SVC_QUERY_COMPLETE, this));
                    } catch (ImsException e) {
                        Rlog.d(LOG_TAG, "processCode: Could not get UT handle for queryCOLR.");
                    }
                } else {
                    throw new RuntimeException ("Invalid or Unsupported MMI Code");
                }
            } else if (mSc != null && (mSc.equals(SC_BS_MT))) {
                try {
                    if (isInterrogate()) {
                        mPhone.mCT.getUtInterface().queryCallBarring(
                                ImsUtImplBase.CALL_BARRING_SPECIFIC_INCOMING_CALLS,
                                obtainMessage(EVENT_QUERY_ICB_COMPLETE, this));
                    } else {
                        processIcbMmiCodeForUpdate();
                    }
                 // TODO: isRegister() case needs to be handled.
                } catch (ImsException e) {
                    Rlog.d(LOG_TAG, "processCode: Could not get UT handle for ICB.");
                }
            } else if (mSc != null && mSc.equals(SC_BAICa)) {
                int callAction =0;
                // TODO: Should we route through queryCallBarring() here?
                try {
                    if (isInterrogate()) {
                        mPhone.mCT.getUtInterface().queryCallBarring(
                                ImsUtImplBase.CALL_BARRING_ANONYMOUS_INCOMING,
                                obtainMessage(EVENT_QUERY_ICB_COMPLETE, this));
                    } else {
                        if (isActivate()) {
                            callAction = CommandsInterface.CF_ACTION_ENABLE;
                        } else if (isDeactivate()) {
                            callAction = CommandsInterface.CF_ACTION_DISABLE;
                        }
                        mPhone.mCT.getUtInterface()
                                .updateCallBarring(ImsUtImplBase.CALL_BARRING_ANONYMOUS_INCOMING,
                                callAction,
                                obtainMessage(EVENT_SET_COMPLETE,this),
                                null);
                    }
                } catch (ImsException e) {
                    Rlog.d(LOG_TAG, "processCode: Could not get UT handle for ICBa.");
                }
            } else if (mSc != null && mSc.equals(SC_WAIT)) {
                // sia = basic service group
                int serviceClass = siToServiceClass(mSia);

                if (isActivate() || isDeactivate()) {
                    mPhone.setCallWaiting(isActivate(), serviceClass,
                            obtainMessage(EVENT_SET_COMPLETE, this));
                } else if (isInterrogate()) {
                    mPhone.getCallWaiting(obtainMessage(EVENT_QUERY_COMPLETE, this));
                } else {
                    throw new RuntimeException ("Invalid or Unsupported MMI Code");
                }
            } else if (mPoundString != null) {
                if (mContext.getResources().getBoolean(
                        com.android.internal.R.bool.config_allow_ussd_over_ims)) {
                    int ussd_method = getIntCarrierConfig(
                                    CarrierConfigManager.KEY_CARRIER_USSD_METHOD_INT);

                    switch (ussd_method) {
                        case USSD_OVER_CS_PREFERRED:
                            // We'll normally send USSD over the CS pipe, but if it happens that
                            // the CS phone is out of service, we'll just try over IMS instead.
                            if (mPhone.getDefaultPhone().getServiceStateTracker().mSS.getState()
                                    == STATE_IN_SERVICE) {
                                Rlog.i(LOG_TAG, "processCode: Sending ussd string '"
                                        + Rlog.pii(LOG_TAG, mPoundString) + "' over CS pipe "
                                        + "(allowed over ims).");
                                throw new CallStateException(Phone.CS_FALLBACK);
                            } else {
                                Rlog.i(LOG_TAG, "processCode: CS is out of service, "
                                        + "sending ussd string '"
                                        + Rlog.pii(LOG_TAG, mPoundString) + "' over IMS pipe.");
                                sendUssd(mPoundString);
                            }
                            break;
                        case USSD_OVER_IMS_PREFERRED:
                        case USSD_OVER_IMS_ONLY:
                            Rlog.i(LOG_TAG, "processCode: Sending ussd string '"
                                    + Rlog.pii(LOG_TAG, mPoundString) + "' over IMS pipe.");
                            sendUssd(mPoundString);
                            break;
                        case USSD_OVER_CS_ONLY:
                            Rlog.i(LOG_TAG, "processCode: Sending ussd string '"
                                    + Rlog.pii(LOG_TAG, mPoundString) + "' over CS pipe.");
                            throw new CallStateException(Phone.CS_FALLBACK);
                        default:
                            Rlog.i(LOG_TAG, "processCode: Sending ussd string '"
                                    + Rlog.pii(LOG_TAG, mPoundString) + "' over CS pipe."
                                    + "(unsupported method)");
                            throw new CallStateException(Phone.CS_FALLBACK);
                    }
                } else {
                    // USSD codes are not supported over IMS due to modem limitations; send over
                    // the CS pipe instead.  This should be fixed in the future.
                    Rlog.i(LOG_TAG, "processCode: Sending ussd string '"
                            + Rlog.pii(LOG_TAG, mPoundString) + "' over CS pipe.");
                    throw new CallStateException(Phone.CS_FALLBACK);
                }
            } else {
                Rlog.d(LOG_TAG, "processCode: invalid or unsupported MMI");
                throw new RuntimeException ("Invalid or Unsupported MMI Code");
            }
        } catch (RuntimeException exc) {
            mState = State.FAILED;
            mMessage = mContext.getText(com.android.internal.R.string.mmiError);
            Rlog.d(LOG_TAG, "processCode: RuntimeException = " + exc);
            mPhone.onMMIDone(this);
        }
    }

    /**
     * Called from ImsPhone
     *
     * An unsolicited USSD NOTIFY or REQUEST has come in matching
     * up with this pending USSD request
     *
     * Note: If REQUEST, this exchange is complete, but the session remains
     *       active (ie, the network expects user input).
     */
    void
    onUssdFinished(String ussdMessage, boolean isUssdRequest) {
        if (mState == State.PENDING) {
            if (TextUtils.isEmpty(ussdMessage)) {
                mMessage = mContext.getText(com.android.internal.R.string.mmiComplete);
                Rlog.v(LOG_TAG, "onUssdFinished: no message; using: " + mMessage);
            } else {
                Rlog.v(LOG_TAG, "onUssdFinished: message: " + ussdMessage);
                mMessage = ussdMessage;
            }
            mIsUssdRequest = isUssdRequest;
            // If it's a request, leave it PENDING so that it's cancelable.
            if (!isUssdRequest) {
                mState = State.COMPLETE;
            }
            mPhone.onMMIDone(this);
        }
    }

    /**
     * Called from ImsPhone
     *
     * The radio has reset, and this is still pending
     */
    public void onUssdFinishedError() {
        if (mState == State.PENDING) {
            mState = State.FAILED;
            if (TextUtils.isEmpty(mMessage)) {
                mMessage = mContext.getText(com.android.internal.R.string.mmiError);
            }
            Rlog.d(LOG_TAG, "onUssdFinishedError: mmi=" + this);
            mPhone.onMMIDone(this);
        }
    }

    void sendUssd(String ussdMessage) {
        // Treat this as a USSD string
        mIsPendingUSSD = true;

        // Note that unlike most everything else, the USSD complete
        // response does not complete this MMI code...we wait for
        // an unsolicited USSD "Notify" or "Request".
        // The matching up of this is done in ImsPhone.

        mPhone.sendUSSD(ussdMessage,
            obtainMessage(EVENT_USSD_COMPLETE, this));
    }

    /** Called from ImsPhone.handleMessage; not a Handler subclass */
    @Override
    public void
    handleMessage (Message msg) {
        AsyncResult ar;

        switch (msg.what) {
            case EVENT_SET_COMPLETE:
                ar = (AsyncResult) (msg.obj);

                onSetComplete(msg, ar);
                break;

            case EVENT_SET_CFF_COMPLETE:
                ar = (AsyncResult) (msg.obj);

                /*
                * msg.arg1 = 1 means to set unconditional voice call forwarding
                * msg.arg2 = 1 means to enable voice call forwarding
                */
                if ((ar.exception == null) && (msg.arg1 == 1)) {
                    boolean cffEnabled = (msg.arg2 == 1);
                    if (mIccRecords != null) {
                        mPhone.setVoiceCallForwardingFlag(mIccRecords,
                                1, cffEnabled, mDialingNumber);
                    }
                }

                onSetComplete(msg, ar);
                break;

            case EVENT_QUERY_CF_COMPLETE:
                ar = (AsyncResult) (msg.obj);
                onQueryCfComplete(ar);
                break;

            case EVENT_QUERY_COMPLETE:
                ar = (AsyncResult) (msg.obj);
                onQueryComplete(ar);
                break;

            case EVENT_USSD_COMPLETE:
                ar = (AsyncResult) (msg.obj);

                if (ar.exception != null) {
                    mState = State.FAILED;
                    mMessage = getErrorMessage(ar);

                    mPhone.onMMIDone(this);
                }

                // Note that unlike most everything else, the USSD complete
                // response does not complete this MMI code...we wait for
                // an unsolicited USSD "Notify" or "Request".
                // The matching up of this is done in ImsPhone.

                break;

            case EVENT_USSD_CANCEL_COMPLETE:
                mPhone.onMMIDone(this);
                break;

            case EVENT_SUPP_SVC_QUERY_COMPLETE:
                ar = (AsyncResult) (msg.obj);
                onSuppSvcQueryComplete(ar);
                break;

            case EVENT_QUERY_ICB_COMPLETE:
                ar = (AsyncResult) (msg.obj);
                onIcbQueryComplete(ar);
                break;

            case EVENT_GET_CLIR_COMPLETE:
                ar = (AsyncResult) (msg.obj);
                onQueryClirComplete(ar);
                break;

            default:
                break;
        }
    }

    //***** Private instance methods

    private void
    processIcbMmiCodeForUpdate () {
        String dialingNumber = mSia;
        String[] icbNum = null;
        int callAction;
        if (dialingNumber != null) {
            icbNum = dialingNumber.split("\\$");
        }
        callAction = callBarAction(dialingNumber);

        try {
            mPhone.mCT.getUtInterface().updateCallBarring(
                    ImsUtImplBase.CALL_BARRING_SPECIFIC_INCOMING_CALLS,
                    callAction,
                    obtainMessage(EVENT_SET_COMPLETE, this),
                    icbNum);
        } catch (ImsException e) {
            Rlog.d(LOG_TAG, "processIcbMmiCodeForUpdate:Could not get UT handle for updating ICB.");
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private CharSequence getErrorMessage(AsyncResult ar) {
        CharSequence errorMessage;
        return ((errorMessage = getMmiErrorMessage(ar)) != null) ? errorMessage :
                mContext.getText(com.android.internal.R.string.mmiError);
    }

    private CharSequence getMmiErrorMessage(AsyncResult ar) {
        if (ar.exception instanceof ImsException) {
            switch (((ImsException) ar.exception).getCode()) {
                case ImsReasonInfo.CODE_FDN_BLOCKED:
                    return mContext.getText(com.android.internal.R.string.mmiFdnError);
                case ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_DIAL:
                    return mContext.getText(com.android.internal.R.string.stk_cc_ss_to_dial);
                case ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_USSD:
                    return mContext.getText(com.android.internal.R.string.stk_cc_ss_to_ussd);
                case ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_SS:
                    return mContext.getText(com.android.internal.R.string.stk_cc_ss_to_ss);
                case ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_DIAL_VIDEO:
                    return mContext.getText(com.android.internal.R.string.stk_cc_ss_to_dial_video);
                default:
                    return null;
            }
        } else if (ar.exception instanceof CommandException) {
            CommandException err = (CommandException) ar.exception;
            if (err.getCommandError() == CommandException.Error.FDN_CHECK_FAILURE) {
                return mContext.getText(com.android.internal.R.string.mmiFdnError);
            } else if (err.getCommandError() == CommandException.Error.SS_MODIFIED_TO_DIAL) {
                return mContext.getText(com.android.internal.R.string.stk_cc_ss_to_dial);
            } else if (err.getCommandError() == CommandException.Error.SS_MODIFIED_TO_USSD) {
                return mContext.getText(com.android.internal.R.string.stk_cc_ss_to_ussd);
            } else if (err.getCommandError() == CommandException.Error.SS_MODIFIED_TO_SS) {
                return mContext.getText(com.android.internal.R.string.stk_cc_ss_to_ss);
            } else if (err.getCommandError() == CommandException.Error.SS_MODIFIED_TO_DIAL_VIDEO) {
                return mContext.getText(com.android.internal.R.string.stk_cc_ss_to_dial_video);
            } else if (err.getCommandError() == CommandException.Error.INTERNAL_ERR) {
                return mContext.getText(com.android.internal.R.string.mmiError);
            }
        }
        return null;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private CharSequence getScString() {
        if (mSc != null) {
            if (isServiceCodeCallBarring(mSc)) {
                return mContext.getText(com.android.internal.R.string.BaMmi);
            } else if (isServiceCodeCallForwarding(mSc)) {
                return mContext.getText(com.android.internal.R.string.CfMmi);
            } else if (mSc.equals(SC_PWD)) {
                return mContext.getText(com.android.internal.R.string.PwdMmi);
            } else if (mSc.equals(SC_WAIT)) {
                return mContext.getText(com.android.internal.R.string.CwMmi);
            } else if (mSc.equals(SC_CLIP)) {
                return mContext.getText(com.android.internal.R.string.ClipMmi);
            } else if (mSc.equals(SC_CLIR)) {
                return mContext.getText(com.android.internal.R.string.ClirMmi);
            } else if (mSc.equals(SC_COLP)) {
                return mContext.getText(com.android.internal.R.string.ColpMmi);
            } else if (mSc.equals(SC_COLR)) {
                return mContext.getText(com.android.internal.R.string.ColrMmi);
            } else if (mSc.equals(SC_BS_MT)) {
                return IcbDnMmi;
            } else if (mSc.equals(SC_BAICa)) {
                return IcbAnonymousMmi;
            }
        }

        return "";
    }

    private void
    onSetComplete(Message msg, AsyncResult ar){
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");

        if (ar.exception != null) {
            mState = State.FAILED;

            if (ar.exception instanceof CommandException) {
                CommandException err = (CommandException) ar.exception;
                CharSequence errorMessage;
                if (err.getCommandError() == CommandException.Error.PASSWORD_INCORRECT) {
                    sb.append(mContext.getText(
                            com.android.internal.R.string.passwordIncorrect));
                } else if ((errorMessage = getMmiErrorMessage(ar)) != null) {
                    sb.append(errorMessage);
                } else if (err.getMessage() != null) {
                    sb.append(err.getMessage());
                } else {
                    sb.append(mContext.getText(com.android.internal.R.string.mmiError));
                }
            } else if (ar.exception instanceof ImsException) {
                sb.append(getImsErrorMessage(ar));
            }
        } else if (ar.result != null && ar.result instanceof Integer
                && (int) ar.result == CommandsInterface.SS_STATUS_UNKNOWN) {
            mState = State.FAILED;
            sb = null;
        } else if (isActivate()) {
            mState = State.COMPLETE;
            if (mIsCallFwdReg) {
                sb.append(mContext.getText(
                        com.android.internal.R.string.serviceRegistered));
            } else {
                sb.append(mContext.getText(
                        com.android.internal.R.string.serviceEnabled));
            }
            // Record CLIR setting
            if (mSc.equals(SC_CLIR)) {
                mPhone.saveClirSetting(CommandsInterface.CLIR_INVOCATION);
            }
        } else if (isDeactivate()) {
            mState = State.COMPLETE;
            sb.append(mContext.getText(
                    com.android.internal.R.string.serviceDisabled));
            // Record CLIR setting
            if (mSc.equals(SC_CLIR)) {
                mPhone.saveClirSetting(CommandsInterface.CLIR_SUPPRESSION);
            }
        } else if (isRegister()) {
            mState = State.COMPLETE;
            sb.append(mContext.getText(
                    com.android.internal.R.string.serviceRegistered));
        } else if (isErasure()) {
            mState = State.COMPLETE;
            sb.append(mContext.getText(
                    com.android.internal.R.string.serviceErased));
        } else {
            mState = State.FAILED;
            sb.append(mContext.getText(
                    com.android.internal.R.string.mmiError));
        }

        mMessage = sb;
        Rlog.d(LOG_TAG, "onSetComplete: mmi=" + this);
        mPhone.onMMIDone(this);
    }

    /**
     * @param serviceClass 1 bit of the service class bit vectory
     * @return String to be used for call forward query MMI response text.
     *        Returns null if unrecognized
     */

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private CharSequence
    serviceClassToCFString (int serviceClass) {
        switch (serviceClass) {
            case SERVICE_CLASS_VOICE:
                return mContext.getText(com.android.internal.R.string.serviceClassVoice);
            case SERVICE_CLASS_DATA:
                return mContext.getText(com.android.internal.R.string.serviceClassData);
            case SERVICE_CLASS_FAX:
                return mContext.getText(com.android.internal.R.string.serviceClassFAX);
            case SERVICE_CLASS_SMS:
                return mContext.getText(com.android.internal.R.string.serviceClassSMS);
            case SERVICE_CLASS_DATA_SYNC:
                return mContext.getText(com.android.internal.R.string.serviceClassDataSync);
            case SERVICE_CLASS_DATA_ASYNC:
                return mContext.getText(com.android.internal.R.string.serviceClassDataAsync);
            case SERVICE_CLASS_PACKET:
                return mContext.getText(com.android.internal.R.string.serviceClassPacket);
            case SERVICE_CLASS_PAD:
                return mContext.getText(com.android.internal.R.string.serviceClassPAD);
            default:
                return null;
        }
    }

    /** one CallForwardInfo + serviceClassMask -> one line of text */
    private CharSequence
    makeCFQueryResultMessage(CallForwardInfo info, int serviceClassMask) {
        CharSequence template;
        String sources[] = {"{0}", "{1}", "{2}"};
        CharSequence destinations[] = new CharSequence[3];
        boolean needTimeTemplate;

        // CF_REASON_NO_REPLY also has a time value associated with
        // it. All others don't.

        needTimeTemplate =
            (info.reason == CommandsInterface.CF_REASON_NO_REPLY);

        if (info.status == 1) {
            if (needTimeTemplate) {
                template = mContext.getText(
                        com.android.internal.R.string.cfTemplateForwardedTime);
            } else {
                template = mContext.getText(
                        com.android.internal.R.string.cfTemplateForwarded);
            }
        } else if (info.status == 0 && isEmptyOrNull(info.number)) {
            template = mContext.getText(
                        com.android.internal.R.string.cfTemplateNotForwarded);
        } else { /* (info.status == 0) && !isEmptyOrNull(info.number) */
            // A call forward record that is not active but contains
            // a phone number is considered "registered"

            if (needTimeTemplate) {
                template = mContext.getText(
                        com.android.internal.R.string.cfTemplateRegisteredTime);
            } else {
                template = mContext.getText(
                        com.android.internal.R.string.cfTemplateRegistered);
            }
        }

        // In the template (from strings.xmls)
        //         {0} is one of "bearerServiceCode*"
        //        {1} is dialing number
        //      {2} is time in seconds

        destinations[0] = serviceClassToCFString(info.serviceClass & serviceClassMask);
        destinations[1] = PhoneNumberUtils.stringFromStringAndTOA(info.number, info.toa);
        destinations[2] = Integer.toString(info.timeSeconds);

        if (info.reason == CommandsInterface.CF_REASON_UNCONDITIONAL &&
                (info.serviceClass & serviceClassMask)
                        == CommandsInterface.SERVICE_CLASS_VOICE) {
            boolean cffEnabled = (info.status == 1);
            mPhone.setVoiceCallForwardingFlag(mIccRecords, 1, cffEnabled, info.number);
        }

        return TextUtils.replace(template, sources, destinations);
    }


    private void
    onQueryCfComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");

        if (ar.exception != null) {
            mState = State.FAILED;

            if (ar.exception instanceof ImsException) {
                sb.append(getImsErrorMessage(ar));
            }
            else {
                sb.append(getErrorMessage(ar));
            }
        } else if (ar.result instanceof CallForwardInfo[] &&
                   ((CallForwardInfo[]) ar.result)[0].status
                    == CommandsInterface.SS_STATUS_UNKNOWN) {
            sb = null;
            mState = State.FAILED;
        } else {
            CallForwardInfo infos[];

            infos = (CallForwardInfo[]) ar.result;

            if (infos == null || infos.length == 0) {
                // Assume the default is not active
                sb.append(mContext.getText(com.android.internal.R.string.serviceDisabled));

                // Set unconditional CFF in SIM to false
                mPhone.setVoiceCallForwardingFlag(mIccRecords, 1, false, null);
            } else {

                SpannableStringBuilder tb = new SpannableStringBuilder();

                // Each bit in the service class gets its own result line
                // The service classes may be split up over multiple
                // CallForwardInfos. So, for each service class, find out
                // which CallForwardInfo represents it and then build
                // the response text based on that

                for (int serviceClassMask = 1
                            ; serviceClassMask <= SERVICE_CLASS_MAX
                            ; serviceClassMask <<= 1
                ) {
                    for (int i = 0, s = infos.length; i < s ; i++) {
                        if ((serviceClassMask & infos[i].serviceClass) != 0) {
                            tb.append(makeCFQueryResultMessage(infos[i],
                                            serviceClassMask));
                            tb.append("\n");
                        }
                    }
                }
                sb.append(tb);
            }

            mState = State.COMPLETE;
        }

        mMessage = sb;
        Rlog.d(LOG_TAG, "onQueryCfComplete: mmi=" + this);
        mPhone.onMMIDone(this);

    }

    private void onSuppSvcQueryComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");

        mState = State.FAILED;
        if (ar.exception != null) {
            if (ar.exception instanceof ImsException) {
                sb.append(getImsErrorMessage(ar));
            } else {
                sb.append(getErrorMessage(ar));
            }
        } else {
            if (ar.result instanceof ImsSsInfo) {
                Rlog.d(LOG_TAG, "onSuppSvcQueryComplete: Received CLIP/COLP/COLR Response.");
                // Response for CLIP, COLP and COLR queries.
                ImsSsInfo ssInfo = (ImsSsInfo) ar.result;
                if (ssInfo != null) {
                    Rlog.d(LOG_TAG,
                            "onSuppSvcQueryComplete: ImsSsInfo mStatus = " + ssInfo.getStatus());
                    if (ssInfo.getProvisionStatus() == ImsSsInfo.SERVICE_NOT_PROVISIONED) {
                        sb.append(mContext.getText(
                                com.android.internal.R.string.serviceNotProvisioned));
                        mState = State.COMPLETE;
                    } else if (ssInfo.getStatus() == ImsSsInfo.DISABLED) {
                        sb.append(mContext.getText(com.android.internal.R.string.serviceDisabled));
                        mState = State.COMPLETE;
                    } else if (ssInfo.getStatus() == ImsSsInfo.ENABLED) {
                        sb.append(mContext.getText(com.android.internal.R.string.serviceEnabled));
                        mState = State.COMPLETE;
                    } else {
                        sb.append(mContext.getText(com.android.internal.R.string.mmiError));
                    }
                } else {
                    sb.append(mContext.getText(com.android.internal.R.string.mmiError));
                }

            } else {
                Rlog.d(LOG_TAG,
                        "onSuppSvcQueryComplete: Received Call Barring/CSFB CLIP Response.");
                // Response for Call Barring and CSFB CLIP queries.
                int[] infos = (int[]) ar.result;
                if (infos == null || infos.length == 0) {
                    sb.append(mContext.getText(com.android.internal.R.string.mmiError));
                } else {
                    if (infos[0] != 0) {
                        sb.append(mContext.getText(com.android.internal.R.string.serviceEnabled));
                        mState = State.COMPLETE;
                    } else {
                        sb.append(mContext.getText(com.android.internal.R.string.serviceDisabled));
                        mState = State.COMPLETE;
                    }
                }
            }
        }

        mMessage = sb;
        Rlog.d(LOG_TAG, "onSuppSvcQueryComplete mmi=" + this);
        mPhone.onMMIDone(this);
    }

    private void onIcbQueryComplete(AsyncResult ar) {
        Rlog.d(LOG_TAG, "onIcbQueryComplete mmi=" + this);
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");

        if (ar.exception != null) {
            mState = State.FAILED;

            if (ar.exception instanceof ImsException) {
                sb.append(getImsErrorMessage(ar));
            } else {
                sb.append(getErrorMessage(ar));
            }
        } else {
            List<ImsSsInfo> infos = null;
            try {
                infos = (List<ImsSsInfo>) ar.result;
            } catch (ClassCastException cce) {
                // TODO in R: #157# still has ImsSsInfo[] type, fix the type in IImsUtListener.aidl.
                infos = Arrays.asList((ImsSsInfo[]) ar.result);
            }
            if (infos == null || infos.size() == 0) {
                sb.append(mContext.getText(com.android.internal.R.string.serviceDisabled));
            } else {
                ImsSsInfo info;
                for (int i = 0, s = infos.size(); i < s; i++) {
                    info = infos.get(i);
                    if (info.getIncomingCommunicationBarringNumber() != null) {
                        sb.append("Num: " + info.getIncomingCommunicationBarringNumber()
                                + " status: " + info.getStatus() + "\n");
                    } else if (info.getStatus() == 1) {
                        sb.append(mContext.getText(com.android.internal
                                .R.string.serviceEnabled));
                    } else {
                        sb.append(mContext.getText(com.android.internal
                                .R.string.serviceDisabled));
                    }
                }
            }
            mState = State.COMPLETE;
        }
        mMessage = sb;
        mPhone.onMMIDone(this);
    }

    private void onQueryClirComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");
        mState = State.FAILED;

        if (ar.exception != null) {
            if (ar.exception instanceof ImsException) {
                sb.append(getImsErrorMessage(ar));
            } else {
                sb.append(getErrorMessage(ar));
            }
        } else {
            int[] clirInfo = (int[]) ar.result;
            // ssInfo.getClirOutgoingState() = The 'n' parameter from TS 27.007 7.7
            // ssInfo.getClirInterrogationStatus() = The 'm' parameter from TS 27.007 7.7
            Rlog.d(LOG_TAG, "onQueryClirComplete: CLIR param n=" + clirInfo[0]
                    + " m=" + clirInfo[1]);

            // 'm' parameter.
            switch (clirInfo[1]) {
                case ImsSsInfo.CLIR_STATUS_NOT_PROVISIONED:
                    sb.append(mContext.getText(
                            com.android.internal.R.string.serviceNotProvisioned));
                    mState = State.COMPLETE;
                    break;
                case ImsSsInfo.CLIR_STATUS_PROVISIONED_PERMANENT:
                    sb.append(mContext.getText(
                            com.android.internal.R.string.CLIRPermanent));
                    mState = State.COMPLETE;
                    break;
                case ImsSsInfo.CLIR_STATUS_TEMPORARILY_RESTRICTED:
                    // 'n' parameter.
                    switch (clirInfo[0]) {
                        case ImsSsInfo.CLIR_OUTGOING_DEFAULT:
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.CLIRDefaultOnNextCallOn));
                            mState = State.COMPLETE;
                            break;
                        case ImsSsInfo.CLIR_OUTGOING_INVOCATION:
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.CLIRDefaultOnNextCallOn));
                            mState = State.COMPLETE;
                            break;
                        case ImsSsInfo.CLIR_OUTGOING_SUPPRESSION:
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.CLIRDefaultOnNextCallOff));
                            mState = State.COMPLETE;
                            break;
                        default:
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.mmiError));
                            mState = State.FAILED;
                    }
                    break;
                case ImsSsInfo.CLIR_STATUS_TEMPORARILY_ALLOWED:
                    // 'n' parameter.
                    switch (clirInfo[0]) {
                        case ImsSsInfo.CLIR_OUTGOING_DEFAULT:
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.CLIRDefaultOffNextCallOff));
                            mState = State.COMPLETE;
                            break;
                        case ImsSsInfo.CLIR_OUTGOING_INVOCATION:
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.CLIRDefaultOffNextCallOn));
                            mState = State.COMPLETE;
                            break;
                        case ImsSsInfo.CLIR_OUTGOING_SUPPRESSION:
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.CLIRDefaultOffNextCallOff));
                            mState = State.COMPLETE;
                            break;
                        default:
                            sb.append(mContext.getText(
                                    com.android.internal.R.string.mmiError));
                            mState = State.FAILED;
                    }
                    break;
                default:
                    sb.append(mContext.getText(
                            com.android.internal.R.string.mmiError));
                    mState = State.FAILED;
            }
        }

        mMessage = sb;
        Rlog.d(LOG_TAG, "onQueryClirComplete mmi=" + this);
        mPhone.onMMIDone(this);
    }

    private void
    onQueryComplete(AsyncResult ar) {
        StringBuilder sb = new StringBuilder(getScString());
        sb.append("\n");

        mState = State.FAILED;
        if (ar.exception != null) {
            if (ar.exception instanceof ImsException) {
                sb.append(getImsErrorMessage(ar));
            } else {
                sb.append(getErrorMessage(ar));
            }
        } else if ((ar.result instanceof int[]) &&
                   ((int[])ar.result)[0] == CommandsInterface.SS_STATUS_UNKNOWN) {
            sb = null;
        } else {
            int[] ints = (int[])ar.result;

            if (ints != null && ints.length != 0) {
                if (ints[0] == 0) {
                    sb.append(mContext.getText(com.android.internal.R.string.serviceDisabled));
                    mState = State.COMPLETE;
                } else if (mSc.equals(SC_WAIT)) {
                    // Call Waiting includes additional data in the response.
                    sb.append(createQueryCallWaitingResultMessage(ints[1]));
                    mState = State.COMPLETE;
                } else if (ints[0] == 1) {
                    // for all other services, treat it as a boolean
                    sb.append(mContext.getText(com.android.internal.R.string.serviceEnabled));
                    mState = State.COMPLETE;
                } else {
                    sb.append(mContext.getText(com.android.internal.R.string.mmiError));
                }
            } else {
                sb.append(mContext.getText(com.android.internal.R.string.mmiError));
            }
        }

        mMessage = sb;
        Rlog.d(LOG_TAG, "onQueryComplete mmi=" + this);
        mPhone.onMMIDone(this);
    }

    private CharSequence
    createQueryCallWaitingResultMessage(int serviceClass) {
        StringBuilder sb = new StringBuilder(
                mContext.getText(com.android.internal.R.string.serviceEnabledFor));

        for (int classMask = 1
                    ; classMask <= SERVICE_CLASS_MAX
                    ; classMask <<= 1
        ) {
            if ((classMask & serviceClass) != 0) {
                sb.append("\n");
                sb.append(serviceClassToCFString(classMask & serviceClass));
            }
        }
        return sb;
    }

    private CharSequence getImsErrorMessage(AsyncResult ar) {
        ImsException error = (ImsException) ar.exception;
        CharSequence errorMessage;
        if ((errorMessage = getMmiErrorMessage(ar)) != null) {
            return errorMessage;
        } else if (error.getMessage() != null) {
            return error.getMessage();
        } else {
            return getErrorMessage(ar);
        }
    }

    /**
     * Get the int config from carrier config manager.
     *
     * @param key config key defined in CarrierConfigManager
     * @return integer value of corresponding key.
     */
    private int getIntCarrierConfig(String key) {
        CarrierConfigManager ConfigManager =
                (CarrierConfigManager) mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = null;
        if (ConfigManager != null) {
            // If an invalid subId is used, this bundle will contain default values.
            b = ConfigManager.getConfigForSubId(mPhone.getSubId());
        }
        if (b != null) {
            return b.getInt(key);
        } else {
            // Return static default defined in CarrierConfigManager.
            return CarrierConfigManager.getDefaultConfig().getInt(key);
        }
    }

    @Override
    public ResultReceiver getUssdCallbackReceiver() {
        return this.mCallbackReceiver;
    }

    /**
     * Process IMS SS Data received.
     */
    public void processImsSsData(AsyncResult data) throws ImsException {
        try {
            ImsSsData ssData = (ImsSsData) data.result;
            parseSsData(ssData);
        } catch (ClassCastException | NullPointerException ex) {
            throw new ImsException("Exception in parsing SS Data", 0);
        }
    }

    void parseSsData(ImsSsData ssData) {
        ImsException ex = (ssData.getResult() != ImsSsData.RESULT_SUCCESS)
                ? new ImsException(null, ssData.getResult()) : null;
        mSc = getScStringFromScType(ssData.getServiceType());
        mAction = getActionStringFromReqType(ssData.getRequestType());
        Rlog.d(LOG_TAG, "parseSsData msc = " + mSc + ", action = " + mAction + ", ex = " + ex);

        switch (ssData.getRequestType()) {
            case ImsSsData.SS_ACTIVATION:
            case ImsSsData.SS_DEACTIVATION:
            case ImsSsData.SS_REGISTRATION:
            case ImsSsData.SS_ERASURE:
                if ((ssData.getResult() == ImsSsData.RESULT_SUCCESS)
                        && ssData.isTypeUnConditional()) {
                    /*
                     * When ssData.serviceType is unconditional (SS_CFU or SS_CF_ALL) and
                     * ssData.requestType is activate/register and
                     * ServiceClass is Voice/Video/None, turn on voice call forwarding.
                     */
                    boolean cffEnabled = ((ssData.getRequestType() == ImsSsData.SS_ACTIVATION
                            || ssData.getRequestType() == ImsSsData.SS_REGISTRATION)
                            && isServiceClassVoiceVideoOrNone(ssData.getServiceClass()));

                    Rlog.d(LOG_TAG, "setCallForwardingFlag cffEnabled: " + cffEnabled);
                    if (mIccRecords != null) {
                        Rlog.d(LOG_TAG, "setVoiceCallForwardingFlag done from SS Info.");
                        //Only CF status is set here as part of activation/registration,
                        //number is not available until interrogation.
                        mPhone.setVoiceCallForwardingFlag(1, cffEnabled, null);
                    } else {
                        Rlog.e(LOG_TAG, "setCallForwardingFlag aborted. sim records is null.");
                    }
                }
                onSetComplete(null, new AsyncResult(null, ssData.getCallForwardInfo(), ex));
                break;
            case ImsSsData.SS_INTERROGATION:
                if (ssData.isTypeClir()) {
                    Rlog.d(LOG_TAG, "CLIR INTERROGATION");
                    onQueryClirComplete(new AsyncResult(null, ssData.getSuppServiceInfoCompat(), ex));
                } else if (ssData.isTypeCF()) {
                    Rlog.d(LOG_TAG, "CALL FORWARD INTERROGATION");
                    // Have to translate to an array, since the modem still returns it in the
                    // ImsCallForwardInfo[] format.
                    List<ImsCallForwardInfo> mCfInfos = ssData.getCallForwardInfo();
                    ImsCallForwardInfo[] mCfInfosCompat = null;
                    if (mCfInfos != null) {
                        mCfInfosCompat = new ImsCallForwardInfo[mCfInfos.size()];
                        mCfInfosCompat = mCfInfos.toArray(mCfInfosCompat);
                    }
                    onQueryCfComplete(new AsyncResult(null, mPhone.handleCfQueryResult(
                            mCfInfosCompat), ex));
                } else if (ssData.isTypeBarring()) {
                    onSuppSvcQueryComplete(new AsyncResult(null, ssData.getSuppServiceInfoCompat(),
                            ex));
                } else if (ssData.isTypeColr() || ssData.isTypeClip() || ssData.isTypeColp()) {
                    onSuppSvcQueryComplete(new AsyncResult(null, ssData.getSuppServiceInfo().get(0),
                            ex));
                } else if (ssData.isTypeIcb()) {
                    onIcbQueryComplete(new AsyncResult(null, ssData.getSuppServiceInfo(), ex));
                } else {
                    onQueryComplete(new AsyncResult(null, ssData.getSuppServiceInfoCompat(), ex));
                }
                break;
            default:
                Rlog.e(LOG_TAG, "Invaid requestType in SSData : " + ssData.getRequestType());
                break;
        }
    }

    private String getScStringFromScType(int serviceType) {
        switch (serviceType) {
            case ImsSsData.SS_CFU:
                return SC_CFU;
            case ImsSsData.SS_CF_BUSY:
                return SC_CFB;
            case ImsSsData.SS_CF_NO_REPLY:
                return SC_CFNRy;
            case ImsSsData.SS_CF_NOT_REACHABLE:
                return SC_CFNR;
            case ImsSsData.SS_CF_ALL:
                return SC_CF_All;
            case ImsSsData.SS_CF_ALL_CONDITIONAL:
                return SC_CF_All_Conditional;
            case ImsSsData.SS_CLIP:
                return SC_CLIP;
            case ImsSsData.SS_CLIR:
                return SC_CLIR;
            case ImsSsData.SS_COLP:
                return SC_COLP;
            case ImsSsData.SS_COLR:
                return SC_COLR;
            case ImsSsData.SS_CNAP:
                return SC_CNAP;
            case ImsSsData.SS_WAIT:
                return SC_WAIT;
            case ImsSsData.SS_BAOC:
                return SC_BAOC;
            case ImsSsData.SS_BAOIC:
                return SC_BAOIC;
            case ImsSsData.SS_BAOIC_EXC_HOME:
                return SC_BAOICxH;
            case ImsSsData.SS_BAIC:
                return SC_BAIC;
            case ImsSsData.SS_BAIC_ROAMING:
                return SC_BAICr;
            case ImsSsData.SS_ALL_BARRING:
                return SC_BA_ALL;
            case ImsSsData.SS_OUTGOING_BARRING:
                return SC_BA_MO;
            case ImsSsData.SS_INCOMING_BARRING:
                return SC_BA_MT;
            case ImsSsData.SS_INCOMING_BARRING_DN:
                return SC_BS_MT;
            case ImsSsData.SS_INCOMING_BARRING_ANONYMOUS:
                return SC_BAICa;
            default:
                return null;
        }
    }

    private String getActionStringFromReqType(int requestType) {
        switch (requestType) {
            case ImsSsData.SS_ACTIVATION:
                return ACTION_ACTIVATE;
            case ImsSsData.SS_DEACTIVATION:
                return ACTION_DEACTIVATE;
            case ImsSsData.SS_INTERROGATION:
                return ACTION_INTERROGATE;
            case ImsSsData.SS_REGISTRATION:
                return ACTION_REGISTER;
            case ImsSsData.SS_ERASURE:
                return ACTION_ERASURE;
            default:
                return null;
        }
    }

    private boolean isServiceClassVoiceVideoOrNone(int serviceClass) {
        return ((serviceClass == SERVICE_CLASS_NONE) || (serviceClass == SERVICE_CLASS_VOICE)
                || (serviceClass == (SERVICE_CLASS_PACKET + SERVICE_CLASS_DATA_SYNC)));
    }

    public boolean isSsInfo() {
        return mIsSsInfo;
    }

    public void setIsSsInfo(boolean isSsInfo) {
        mIsSsInfo = isSsInfo;
    }

    /***
     * TODO: It would be nice to have a method here that can take in a dialstring and
     * figure out if there is an MMI code embedded within it.  This code would replace
     * some of the string parsing functionality in the Phone App's
     * SpecialCharSequenceMgr class.
     */

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ImsPhoneMmiCode {");

        sb.append("State=" + getState());
        if (mAction != null) sb.append(" action=" + mAction);
        if (mSc != null) sb.append(" sc=" + mSc);
        if (mSia != null) sb.append(" sia=" + mSia);
        if (mSib != null) sb.append(" sib=" + mSib);
        if (mSic != null) sb.append(" sic=" + mSic);
        if (mPoundString != null) sb.append(" poundString=" + Rlog.pii(LOG_TAG, mPoundString));
        if (mDialingNumber != null) sb.append(" dialingNumber="
                + Rlog.pii(LOG_TAG, mDialingNumber));
        if (mPwd != null) sb.append(" pwd=" + Rlog.pii(LOG_TAG, mPwd));
        if (mCallbackReceiver != null) sb.append(" hasReceiver");
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean isNetworkInitiatedUssd() {
        return mIsNetworkInitiatedUSSD;
    }
}
