/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.internal.telephony.cat;

import static com.android.internal.telephony.cat.CatCmdMessage.SetupEventListConstants.IDLE_SCREEN_AVAILABLE_EVENT;
import static com.android.internal.telephony.cat.CatCmdMessage.SetupEventListConstants.LANGUAGE_SELECTION_EVENT;
import static com.android.internal.telephony.cat.CatCmdMessage.SetupEventListConstants.USER_ACTIVITY_EVENT;

import android.app.ActivityManager;
import android.app.backup.BackupManager;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources.NotFoundException;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.LocaleList;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;

class RilMessage {
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    int mId;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    Object mData;
    ResultCode mResCode;

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    RilMessage(int msgId, String rawData) {
        mId = msgId;
        mData = rawData;
    }

    RilMessage(RilMessage other) {
        mId = other.mId;
        mData = other.mData;
        mResCode = other.mResCode;
    }
}

/**
 * Class that implements SIM Toolkit Telephony Service. Interacts with the RIL
 * and application.
 *
 * {@hide}
 */
public class CatService extends Handler implements AppInterface {
    private static final boolean DBG = false;

    // Class members
    private static IccRecords mIccRecords;
    private static UiccCardApplication mUiccApplication;

    // Service members.
    // Protects singleton instance lazy initialization.
    @UnsupportedAppUsage
    private static final Object sInstanceLock = new Object();
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private static CatService[] sInstance = null;
    @UnsupportedAppUsage
    private CommandsInterface mCmdIf;
    @UnsupportedAppUsage
    private Context mContext;
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private CatCmdMessage mCurrntCmd = null;
    @UnsupportedAppUsage
    private CatCmdMessage mMenuCmd = null;

    @UnsupportedAppUsage
    private RilMessageDecoder mMsgDecoder = null;
    @UnsupportedAppUsage
    private boolean mStkAppInstalled = false;

    @UnsupportedAppUsage
    private UiccController mUiccController;
    private CardState mCardState = CardState.CARDSTATE_ABSENT;

    // Service constants.
    protected static final int MSG_ID_SESSION_END              = 1;
    protected static final int MSG_ID_PROACTIVE_COMMAND        = 2;
    protected static final int MSG_ID_EVENT_NOTIFY             = 3;
    protected static final int MSG_ID_CALL_SETUP               = 4;
    static final int MSG_ID_REFRESH                  = 5;
    static final int MSG_ID_RESPONSE                 = 6;
    static final int MSG_ID_SIM_READY                = 7;

    protected static final int MSG_ID_ICC_CHANGED    = 8;
    protected static final int MSG_ID_ALPHA_NOTIFY   = 9;

    static final int MSG_ID_RIL_MSG_DECODED          = 10;

    // Events to signal SIM presence or absent in the device.
    private static final int MSG_ID_ICC_RECORDS_LOADED       = 20;

    //Events to signal SIM REFRESH notificatations
    private static final int MSG_ID_ICC_REFRESH  = 30;

    private static final int DEV_ID_KEYPAD      = 0x01;
    private static final int DEV_ID_DISPLAY     = 0x02;
    private static final int DEV_ID_UICC        = 0x81;
    private static final int DEV_ID_TERMINAL    = 0x82;
    private static final int DEV_ID_NETWORK     = 0x83;

    static final String STK_DEFAULT = "Default Message";

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mSlotId;

    /* For multisim catservice should not be singleton */
    private CatService(CommandsInterface ci, UiccCardApplication ca, IccRecords ir,
            Context context, IccFileHandler fh, UiccProfile uiccProfile, int slotId) {
        if (ci == null || ca == null || ir == null || context == null || fh == null
                || uiccProfile == null) {
            throw new NullPointerException(
                    "Service: Input parameters must not be null");
        }
        mCmdIf = ci;
        mContext = context;
        mSlotId = slotId;

        // Get the RilMessagesDecoder for decoding the messages.
        mMsgDecoder = RilMessageDecoder.getInstance(this, fh, context, slotId);
        if (null == mMsgDecoder) {
            CatLog.d(this, "Null RilMessageDecoder instance");
            return;
        }
        mMsgDecoder.start();

        // Register ril events handling.
        mCmdIf.setOnCatSessionEnd(this, MSG_ID_SESSION_END, null);
        mCmdIf.setOnCatProactiveCmd(this, MSG_ID_PROACTIVE_COMMAND, null);
        mCmdIf.setOnCatEvent(this, MSG_ID_EVENT_NOTIFY, null);
        mCmdIf.setOnCatCallSetUp(this, MSG_ID_CALL_SETUP, null);
        //mCmdIf.setOnSimRefresh(this, MSG_ID_REFRESH, null);

        mCmdIf.registerForIccRefresh(this, MSG_ID_ICC_REFRESH, null);
        mCmdIf.setOnCatCcAlphaNotify(this, MSG_ID_ALPHA_NOTIFY, null);

        mIccRecords = ir;
        mUiccApplication = ca;

        // Register for SIM ready event.
        mIccRecords.registerForRecordsLoaded(this, MSG_ID_ICC_RECORDS_LOADED, null);
        CatLog.d(this, "registerForRecordsLoaded slotid=" + mSlotId + " instance:" + this);


        mUiccController = UiccController.getInstance();
        mUiccController.registerForIccChanged(this, MSG_ID_ICC_CHANGED, null);

        // Check if STK application is available
        mStkAppInstalled = isStkAppInstalled();

        CatLog.d(this, "Running CAT service on Slotid: " + mSlotId +
                ". STK app installed:" + mStkAppInstalled);
    }

    /**
     * Used for instantiating the Service from the Card.
     *
     * @param ci CommandsInterface object
     * @param context phone app context
     * @param ic Icc card
     * @param slotId to know the index of card
     * @return The only Service object in the system
     */
    public static CatService getInstance(CommandsInterface ci,
            Context context, UiccProfile uiccProfile, int slotId) {
        UiccCardApplication ca = null;
        IccFileHandler fh = null;
        IccRecords ir = null;
        if (uiccProfile != null) {
            /* Since Cat is not tied to any application, but rather is Uicc application
             * in itself - just get first FileHandler and IccRecords object
             */
            ca = uiccProfile.getApplicationIndex(0);
            if (ca != null) {
                fh = ca.getIccFileHandler();
                ir = ca.getIccRecords();
            }
        }

        synchronized (sInstanceLock) {
            if (sInstance == null) {
                int simCount = TelephonyManager.getDefault().getSupportedModemCount();
                sInstance = new CatService[simCount];
                for (int i = 0; i < simCount; i++) {
                    sInstance[i] = null;
                }
            }
            if (sInstance[slotId] == null) {
                if (ci == null || ca == null || ir == null || context == null || fh == null
                        || uiccProfile == null) {
                    return null;
                }

                sInstance[slotId] = new CatService(ci, ca, ir, context, fh, uiccProfile, slotId);
            } else if ((ir != null) && (mIccRecords != ir)) {
                if (mIccRecords != null) {
                    mIccRecords.unregisterForRecordsLoaded(sInstance[slotId]);
                }

                mIccRecords = ir;
                mUiccApplication = ca;

                mIccRecords.registerForRecordsLoaded(sInstance[slotId],
                        MSG_ID_ICC_RECORDS_LOADED, null);
                CatLog.d(sInstance[slotId], "registerForRecordsLoaded slotid=" + slotId
                        + " instance:" + sInstance[slotId]);
            }
            return sInstance[slotId];
        }
    }

    @UnsupportedAppUsage
    @Override
    public void dispose() {
        synchronized (sInstanceLock) {
            CatLog.d(this, "Disposing CatService object");
            mIccRecords.unregisterForRecordsLoaded(this);

            // Clean up stk icon if dispose is called
            broadcastCardStateAndIccRefreshResp(CardState.CARDSTATE_ABSENT, null);

            mCmdIf.unSetOnCatSessionEnd(this);
            mCmdIf.unSetOnCatProactiveCmd(this);
            mCmdIf.unSetOnCatEvent(this);
            mCmdIf.unSetOnCatCallSetUp(this);
            mCmdIf.unSetOnCatCcAlphaNotify(this);

            mCmdIf.unregisterForIccRefresh(this);
            if (mUiccController != null) {
                mUiccController.unregisterForIccChanged(this);
                mUiccController = null;
            }
            if (mMsgDecoder != null) {
                mMsgDecoder.dispose();
                mMsgDecoder = null;
            }
            removeCallbacksAndMessages(null);
            if (sInstance != null) {
                if (mSlotId >= 0 && mSlotId < sInstance.length) {
                    sInstance[mSlotId] = null;
                } else {
                    CatLog.d(this, "error: invaild slot id: " + mSlotId);
                }
            }
        }
    }

    @Override
    protected void finalize() {
        CatLog.d(this, "Service finalized");
    }

    private void handleRilMsg(RilMessage rilMsg) {
        if (rilMsg == null) {
            return;
        }

        // dispatch messages
        CommandParams cmdParams = null;
        switch (rilMsg.mId) {
        case MSG_ID_EVENT_NOTIFY:
            if (rilMsg.mResCode == ResultCode.OK) {
                cmdParams = (CommandParams) rilMsg.mData;
                if (cmdParams != null) {
                    handleCommand(cmdParams, false);
                }
            }
            break;
        case MSG_ID_PROACTIVE_COMMAND:
            try {
                cmdParams = (CommandParams) rilMsg.mData;
            } catch (ClassCastException e) {
                // for error handling : cast exception
                CatLog.d(this, "Fail to parse proactive command");
                // Don't send Terminal Resp if command detail is not available
                if (mCurrntCmd != null) {
                    sendTerminalResponse(mCurrntCmd.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD,
                                     false, 0x00, null);
                }
                break;
            }
            if (cmdParams != null) {
                if (rilMsg.mResCode == ResultCode.OK) {
                    handleCommand(cmdParams, true);
                } else {
                    // for proactive commands that couldn't be decoded
                    // successfully respond with the code generated by the
                    // message decoder.
                    sendTerminalResponse(cmdParams.mCmdDet, rilMsg.mResCode,
                            false, 0, null);
                }
            }
            break;
        case MSG_ID_REFRESH:
            cmdParams = (CommandParams) rilMsg.mData;
            if (cmdParams != null) {
                handleCommand(cmdParams, false);
            }
            break;
        case MSG_ID_SESSION_END:
            handleSessionEnd();
            break;
        case MSG_ID_CALL_SETUP:
            // prior event notify command supplied all the information
            // needed for set up call processing.
            break;
        }
    }

    /**
     * This function validates the events in SETUP_EVENT_LIST which are currently
     * supported by the Android framework. In case of SETUP_EVENT_LIST has NULL events
     * or no events, all the events need to be reset.
     */
    private boolean isSupportedSetupEventCommand(CatCmdMessage cmdMsg) {
        boolean flag = true;

        for (int eventVal: cmdMsg.getSetEventList().eventList) {
            CatLog.d(this,"Event: " + eventVal);
            switch (eventVal) {
                /* Currently android is supporting only the below events in SetupEventList
                 * Language Selection.  */
                case IDLE_SCREEN_AVAILABLE_EVENT:
                case LANGUAGE_SELECTION_EVENT:
                case USER_ACTIVITY_EVENT:
                    break;
                default:
                    flag = false;
            }
        }
        return flag;
    }

    /**
     * Handles RIL_UNSOL_STK_EVENT_NOTIFY or RIL_UNSOL_STK_PROACTIVE_COMMAND command
     * from RIL.
     * Sends valid proactive command data to the application using intents.
     * RIL_REQUEST_STK_SEND_TERMINAL_RESPONSE will be send back if the command is
     * from RIL_UNSOL_STK_PROACTIVE_COMMAND.
     */
    private void handleCommand(CommandParams cmdParams, boolean isProactiveCmd) {
        CatLog.d(this, cmdParams.getCommandType().name());

        // Log all proactive commands.
        if (isProactiveCmd) {
            UiccController.addLocalLog("CatService[" + mSlotId + "]: ProactiveCommand " +
                    " cmdParams=" + cmdParams);
        }

        CharSequence message;
        ResultCode resultCode;
        CatCmdMessage cmdMsg = new CatCmdMessage(cmdParams);
        switch (cmdParams.getCommandType()) {
            case SET_UP_MENU:
                if (removeMenu(cmdMsg.getMenu())) {
                    mMenuCmd = null;
                } else {
                    mMenuCmd = cmdMsg;
                }
                resultCode = cmdParams.mLoadIconFailed ? ResultCode.PRFRMD_ICON_NOT_DISPLAYED
                                                                            : ResultCode.OK;
                sendTerminalResponse(cmdParams.mCmdDet, resultCode, false, 0, null);
                break;
            case DISPLAY_TEXT:
                break;
            case SET_UP_IDLE_MODE_TEXT:
                resultCode = cmdParams.mLoadIconFailed ? ResultCode.PRFRMD_ICON_NOT_DISPLAYED
                                                                            : ResultCode.OK;
                sendTerminalResponse(cmdParams.mCmdDet,resultCode, false, 0, null);
                break;
            case SET_UP_EVENT_LIST:
                if (isSupportedSetupEventCommand(cmdMsg)) {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                } else {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.BEYOND_TERMINAL_CAPABILITY,
                            false, 0, null);
                }
                break;
            case PROVIDE_LOCAL_INFORMATION:
                ResponseData resp;
                switch (cmdParams.mCmdDet.commandQualifier) {
                    case CommandParamsFactory.DTTZ_SETTING:
                        resp = new DTTZResponseData(null);
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, resp);
                        break;
                    case CommandParamsFactory.LANGUAGE_SETTING:
                        resp = new LanguageResponseData(Locale.getDefault().getLanguage());
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, resp);
                        break;
                    default:
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                }
                // No need to start STK app here.
                return;
            case LAUNCH_BROWSER:
                if ((((LaunchBrowserParams) cmdParams).mConfirmMsg.text != null)
                        && (((LaunchBrowserParams) cmdParams).mConfirmMsg.text.equals(STK_DEFAULT))) {
                    message = mContext.getText(com.android.internal.R.string.launchBrowserDefault);
                    ((LaunchBrowserParams) cmdParams).mConfirmMsg.text = message.toString();
                }
                break;
            case SELECT_ITEM:
            case GET_INPUT:
            case GET_INKEY:
                break;
            case REFRESH:
            case RUN_AT:
                if (STK_DEFAULT.equals(((DisplayTextParams)cmdParams).mTextMsg.text)) {
                    // Remove the default text which was temporarily added and shall not be shown
                    ((DisplayTextParams)cmdParams).mTextMsg.text = null;
                }
                break;
            case SEND_DTMF:
            case SEND_SMS:
            case SEND_SS:
            case SEND_USSD:
                if ((((DisplayTextParams)cmdParams).mTextMsg.text != null)
                        && (((DisplayTextParams)cmdParams).mTextMsg.text.equals(STK_DEFAULT))) {
                    message = mContext.getText(com.android.internal.R.string.sending);
                    ((DisplayTextParams)cmdParams).mTextMsg.text = message.toString();
                }
                break;
            case PLAY_TONE:
                break;
            case SET_UP_CALL:
                if ((((CallSetupParams) cmdParams).mConfirmMsg.text != null)
                        && (((CallSetupParams) cmdParams).mConfirmMsg.text.equals(STK_DEFAULT))) {
                    message = mContext.getText(com.android.internal.R.string.SetupCallDefault);
                    ((CallSetupParams) cmdParams).mConfirmMsg.text = message.toString();
                }
                break;
            case LANGUAGE_NOTIFICATION:
                String language = ((LanguageParams) cmdParams).mLanguage;
                ResultCode result = ResultCode.OK;
                if (language != null && language.length() > 0) {
                    try {
                        changeLanguage(language);
                    } catch (RemoteException e) {
                        result = ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS;
                    }
                }
                sendTerminalResponse(cmdParams.mCmdDet, result, false, 0, null);
                return;
            case OPEN_CHANNEL:
            case CLOSE_CHANNEL:
            case RECEIVE_DATA:
            case SEND_DATA:
                BIPClientParams cmd = (BIPClientParams) cmdParams;
                /* Per 3GPP specification 102.223,
                 * if the alpha identifier is not provided by the UICC,
                 * the terminal MAY give information to the user
                 * noAlphaUsrCnf defines if you need to show user confirmation or not
                 */
                boolean noAlphaUsrCnf = false;
                try {
                    noAlphaUsrCnf = mContext.getResources().getBoolean(
                            com.android.internal.R.bool.config_stkNoAlphaUsrCnf);
                } catch (NotFoundException e) {
                    noAlphaUsrCnf = false;
                }
                if ((cmd.mTextMsg.text == null) && (cmd.mHasAlphaId || noAlphaUsrCnf)) {
                    CatLog.d(this, "cmd " + cmdParams.getCommandType() + " with null alpha id");
                    // If alpha length is zero, we just respond with OK.
                    if (isProactiveCmd) {
                        sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                    } else if (cmdParams.getCommandType() == CommandType.OPEN_CHANNEL) {
                        mCmdIf.handleCallSetupRequestFromSim(true, null);
                    }
                    return;
                }
                // Respond with permanent failure to avoid retry if STK app is not present.
                if (!mStkAppInstalled) {
                    CatLog.d(this, "No STK application found.");
                    if (isProactiveCmd) {
                        sendTerminalResponse(cmdParams.mCmdDet,
                                             ResultCode.BEYOND_TERMINAL_CAPABILITY,
                                             false, 0, null);
                        return;
                    }
                }
                /*
                 * CLOSE_CHANNEL, RECEIVE_DATA and SEND_DATA can be delivered by
                 * either PROACTIVE_COMMAND or EVENT_NOTIFY.
                 * If PROACTIVE_COMMAND is used for those commands, send terminal
                 * response here.
                 */
                if (isProactiveCmd &&
                    ((cmdParams.getCommandType() == CommandType.CLOSE_CHANNEL) ||
                     (cmdParams.getCommandType() == CommandType.RECEIVE_DATA) ||
                     (cmdParams.getCommandType() == CommandType.SEND_DATA))) {
                    sendTerminalResponse(cmdParams.mCmdDet, ResultCode.OK, false, 0, null);
                }
                break;
            default:
                CatLog.d(this, "Unsupported command");
                return;
        }
        mCurrntCmd = cmdMsg;
        broadcastCatCmdIntent(cmdMsg);
    }


    private void broadcastCatCmdIntent(CatCmdMessage cmdMsg) {
        Intent intent = new Intent(AppInterface.CAT_CMD_ACTION);
        intent.putExtra( "STK CMD", cmdMsg);
        intent.putExtra("SLOT_ID", mSlotId);
        intent.setComponent(AppInterface.getDefaultSTKApplication());
        CatLog.d(this, "Sending CmdMsg: " + cmdMsg+ " on slotid:" + mSlotId);
        mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    /**
     * Handles RIL_UNSOL_STK_SESSION_END unsolicited command from RIL.
     *
     */
    private void handleSessionEnd() {
        CatLog.d(this, "SESSION END on "+ mSlotId);

        mCurrntCmd = mMenuCmd;
        Intent intent = new Intent(AppInterface.CAT_SESSION_END_ACTION);
        intent.putExtra("SLOT_ID", mSlotId);
        intent.setComponent(AppInterface.getDefaultSTKApplication());
        mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }


    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void sendTerminalResponse(CommandDetails cmdDet,
            ResultCode resultCode, boolean includeAdditionalInfo,
            int additionalInfo, ResponseData resp) {

        if (cmdDet == null) {
            return;
        }
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        Input cmdInput = null;
        if (mCurrntCmd != null) {
            cmdInput = mCurrntCmd.geInput();
        }

        // command details
        int tag = ComprehensionTlvTag.COMMAND_DETAILS.value();
        if (cmdDet.compRequired) {
            tag |= 0x80;
        }
        buf.write(tag);
        buf.write(0x03); // length
        buf.write(cmdDet.commandNumber);
        buf.write(cmdDet.typeOfCommand);
        buf.write(cmdDet.commandQualifier);

        // device identities
        // According to TS102.223/TS31.111 section 6.8 Structure of
        // TERMINAL RESPONSE, "For all SIMPLE-TLV objects with Min=N,
        // the ME should set the CR(comprehension required) flag to
        // comprehension not required.(CR=0)"
        // Since DEVICE_IDENTITIES and DURATION TLVs have Min=N,
        // the CR flag is not set.
        tag = ComprehensionTlvTag.DEVICE_IDENTITIES.value();
        buf.write(tag);
        buf.write(0x02); // length
        buf.write(DEV_ID_TERMINAL); // source device id
        buf.write(DEV_ID_UICC); // destination device id

        // result
        tag = ComprehensionTlvTag.RESULT.value();
        if (cmdDet.compRequired) {
            tag |= 0x80;
        }
        buf.write(tag);
        int length = includeAdditionalInfo ? 2 : 1;
        buf.write(length);
        buf.write(resultCode.value());

        // additional info
        if (includeAdditionalInfo) {
            buf.write(additionalInfo);
        }

        // Fill optional data for each corresponding command
        if (resp != null) {
            resp.format(buf);
        } else {
            encodeOptionalTags(cmdDet, resultCode, cmdInput, buf);
        }

        byte[] rawData = buf.toByteArray();
        String hexString = IccUtils.bytesToHexString(rawData);
        if (DBG) {
            CatLog.d(this, "TERMINAL RESPONSE: " + hexString);
        }

        mCmdIf.sendTerminalResponse(hexString, null);
    }

    private void encodeOptionalTags(CommandDetails cmdDet,
            ResultCode resultCode, Input cmdInput, ByteArrayOutputStream buf) {
        CommandType cmdType = AppInterface.CommandType.fromInt(cmdDet.typeOfCommand);
        if (cmdType != null) {
            switch (cmdType) {
                case GET_INPUT:
                case GET_INKEY:
                    // Please refer to the clause 6.8.21 of ETSI 102.223.
                    // The terminal shall supply the command execution duration
                    // when it issues TERMINAL RESPONSE for GET INKEY command with variable timeout.
                    // GET INPUT command should also be handled in the same manner.
                    if ((resultCode.value() == ResultCode.NO_RESPONSE_FROM_USER.value()) &&
                        (cmdInput != null) && (cmdInput.duration != null)) {
                        getInKeyResponse(buf, cmdInput);
                    }
                    break;
                case PROVIDE_LOCAL_INFORMATION:
                    if ((cmdDet.commandQualifier == CommandParamsFactory.LANGUAGE_SETTING) &&
                        (resultCode.value() == ResultCode.OK.value())) {
                        getPliResponse(buf);
                    }
                    break;
                default:
                    CatLog.d(this, "encodeOptionalTags() Unsupported Cmd details=" + cmdDet);
                    break;
            }
        } else {
            CatLog.d(this, "encodeOptionalTags() bad Cmd details=" + cmdDet);
        }
    }

    private void getInKeyResponse(ByteArrayOutputStream buf, Input cmdInput) {
        int tag = ComprehensionTlvTag.DURATION.value();

        buf.write(tag);
        buf.write(0x02); // length
        buf.write(cmdInput.duration.timeUnit.SECOND.value()); // Time (Unit,Seconds)
        buf.write(cmdInput.duration.timeInterval); // Time Duration
    }

    private void getPliResponse(ByteArrayOutputStream buf) {
        // Locale Language Setting
        final String lang = Locale.getDefault().getLanguage();

        if (lang != null) {
            // tag
            int tag = ComprehensionTlvTag.LANGUAGE.value();
            buf.write(tag);
            ResponseData.writeLength(buf, lang.length());
            buf.write(lang.getBytes(), 0, lang.length());
        }
    }

    private void sendMenuSelection(int menuId, boolean helpRequired) {

        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        // tag
        int tag = BerTlv.BER_MENU_SELECTION_TAG;
        buf.write(tag);

        // length
        buf.write(0x00); // place holder

        // device identities
        tag = 0x80 | ComprehensionTlvTag.DEVICE_IDENTITIES.value();
        buf.write(tag);
        buf.write(0x02); // length
        buf.write(DEV_ID_KEYPAD); // source device id
        buf.write(DEV_ID_UICC); // destination device id

        // item identifier
        tag = 0x80 | ComprehensionTlvTag.ITEM_ID.value();
        buf.write(tag);
        buf.write(0x01); // length
        buf.write(menuId); // menu identifier chosen

        // help request
        if (helpRequired) {
            tag = ComprehensionTlvTag.HELP_REQUEST.value();
            buf.write(tag);
            buf.write(0x00); // length
        }

        byte[] rawData = buf.toByteArray();

        // write real length
        int len = rawData.length - 2; // minus (tag + length)
        rawData[1] = (byte) len;

        String hexString = IccUtils.bytesToHexString(rawData);

        mCmdIf.sendEnvelope(hexString, null);
    }

    private void eventDownload(int event, int sourceId, int destinationId,
            byte[] additionalInfo, boolean oneShot) {

        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        // tag
        int tag = BerTlv.BER_EVENT_DOWNLOAD_TAG;
        buf.write(tag);

        // length
        buf.write(0x00); // place holder, assume length < 128.

        // event list
        tag = 0x80 | ComprehensionTlvTag.EVENT_LIST.value();
        buf.write(tag);
        buf.write(0x01); // length
        buf.write(event); // event value

        // device identities
        tag = 0x80 | ComprehensionTlvTag.DEVICE_IDENTITIES.value();
        buf.write(tag);
        buf.write(0x02); // length
        buf.write(sourceId); // source device id
        buf.write(destinationId); // destination device id

        /*
         * Check for type of event download to be sent to UICC - Browser
         * termination,Idle screen available, User activity, Language selection
         * etc as mentioned under ETSI TS 102 223 section 7.5
         */

        /*
         * Currently the below events are supported:
         * Language Selection Event.
         * Other event download commands should be encoded similar way
         */
        /* TODO: eventDownload should be extended for other Envelope Commands */
        switch (event) {
            case IDLE_SCREEN_AVAILABLE_EVENT:
                CatLog.d(sInstance, " Sending Idle Screen Available event download to ICC");
                break;
            case LANGUAGE_SELECTION_EVENT:
                CatLog.d(sInstance, " Sending Language Selection event download to ICC");
                tag = 0x80 | ComprehensionTlvTag.LANGUAGE.value();
                buf.write(tag);
                // Language length should be 2 byte
                buf.write(0x02);
                break;
            case USER_ACTIVITY_EVENT:
                break;
            default:
                break;
        }

        // additional information
        if (additionalInfo != null) {
            for (byte b : additionalInfo) {
                buf.write(b);
            }
        }

        byte[] rawData = buf.toByteArray();

        // write real length
        int len = rawData.length - 2; // minus (tag + length)
        rawData[1] = (byte) len;

        String hexString = IccUtils.bytesToHexString(rawData);

        CatLog.d(this, "ENVELOPE COMMAND: " + hexString);

        mCmdIf.sendEnvelope(hexString, null);
    }

    /**
     * Used by application to get an AppInterface object.
     *
     * @return The only Service object in the system
     */
    //TODO Need to take care for MSIM
    public static AppInterface getInstance() {
        int slotId = PhoneConstants.DEFAULT_SLOT_INDEX;
        SubscriptionController sControl = SubscriptionController.getInstance();
        if (sControl != null) {
            slotId = sControl.getSlotIndex(sControl.getDefaultSubId());
        }
        return getInstance(null, null, null, slotId);
    }

    /**
     * Used by application to get an AppInterface object.
     *
     * @return The only Service object in the system
     */
    public static AppInterface getInstance(int slotId) {
        return getInstance(null, null, null, slotId);
    }

    @Override
    public void handleMessage(Message msg) {
        CatLog.d(this, "handleMessage[" + msg.what + "]");

        switch (msg.what) {
        case MSG_ID_SESSION_END:
        case MSG_ID_PROACTIVE_COMMAND:
        case MSG_ID_EVENT_NOTIFY:
        case MSG_ID_REFRESH:
            CatLog.d(this, "ril message arrived,slotid:" + mSlotId);
            String data = null;
            if (msg.obj != null) {
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar != null && ar.result != null) {
                    try {
                        data = (String) ar.result;
                    } catch (ClassCastException e) {
                        break;
                    }
                }
            }
            mMsgDecoder.sendStartDecodingMessageParams(new RilMessage(msg.what, data));
            break;
        case MSG_ID_CALL_SETUP:
            mMsgDecoder.sendStartDecodingMessageParams(new RilMessage(msg.what, null));
            break;
        case MSG_ID_ICC_RECORDS_LOADED:
            break;
        case MSG_ID_RIL_MSG_DECODED:
            handleRilMsg((RilMessage) msg.obj);
            break;
        case MSG_ID_RESPONSE:
            handleCmdResponse((CatResponseMessage) msg.obj);
            break;
        case MSG_ID_ICC_CHANGED:
            CatLog.d(this, "MSG_ID_ICC_CHANGED");
            updateIccAvailability();
            break;
        case MSG_ID_ICC_REFRESH:
            if (msg.obj != null) {
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar != null && ar.result != null) {
                    broadcastCardStateAndIccRefreshResp(CardState.CARDSTATE_PRESENT,
                                  (IccRefreshResponse) ar.result);
                } else {
                    CatLog.d(this,"Icc REFRESH with exception: " + ar.exception);
                }
            } else {
                CatLog.d(this, "IccRefresh Message is null");
            }
            break;
        case MSG_ID_ALPHA_NOTIFY:
            CatLog.d(this, "Received CAT CC Alpha message from card");
            if (msg.obj != null) {
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar != null && ar.result != null) {
                    broadcastAlphaMessage((String)ar.result);
                } else {
                    CatLog.d(this, "CAT Alpha message: ar.result is null");
                }
            } else {
                CatLog.d(this, "CAT Alpha message: msg.obj is null");
            }
            break;
        default:
            throw new AssertionError("Unrecognized CAT command: " + msg.what);
        }
    }

    /**
     ** This function sends a CARD status (ABSENT, PRESENT, REFRESH) to STK_APP.
     ** This is triggered during ICC_REFRESH or CARD STATE changes. In case
     ** REFRESH, additional information is sent in 'refresh_result'
     **
     **/
    private void  broadcastCardStateAndIccRefreshResp(CardState cardState,
            IccRefreshResponse iccRefreshState) {
        Intent intent = new Intent(AppInterface.CAT_ICC_STATUS_CHANGE);
        boolean cardPresent = (cardState == CardState.CARDSTATE_PRESENT);

        if (iccRefreshState != null) {
            //This case is when MSG_ID_ICC_REFRESH is received.
            intent.putExtra(AppInterface.REFRESH_RESULT, iccRefreshState.refreshResult);
            CatLog.d(this, "Sending IccResult with Result: "
                    + iccRefreshState.refreshResult);
        }

        // This sends an intent with CARD_ABSENT (0 - false) /CARD_PRESENT (1 - true).
        intent.putExtra(AppInterface.CARD_STATUS, cardPresent);
        intent.setComponent(AppInterface.getDefaultSTKApplication());
        intent.putExtra("SLOT_ID", mSlotId);
        CatLog.d(this, "Sending Card Status: "
                + cardState + " " + "cardPresent: " + cardPresent +  "SLOT_ID: " +  mSlotId);
        mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    private void broadcastAlphaMessage(String alphaString) {
        CatLog.d(this, "Broadcasting CAT Alpha message from card: " + alphaString);
        Intent intent = new Intent(AppInterface.CAT_ALPHA_NOTIFY_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(AppInterface.ALPHA_STRING, alphaString);
        intent.putExtra("SLOT_ID", mSlotId);
        intent.setComponent(AppInterface.getDefaultSTKApplication());
        mContext.sendBroadcast(intent, AppInterface.STK_PERMISSION);
    }

    @Override
    public synchronized void onCmdResponse(CatResponseMessage resMsg) {
        if (resMsg == null) {
            return;
        }
        // queue a response message.
        Message msg = obtainMessage(MSG_ID_RESPONSE, resMsg);
        msg.sendToTarget();
    }

    private boolean validateResponse(CatResponseMessage resMsg) {
        boolean validResponse = false;
        if ((resMsg.mCmdDet.typeOfCommand == CommandType.SET_UP_EVENT_LIST.value())
                || (resMsg.mCmdDet.typeOfCommand == CommandType.SET_UP_MENU.value())) {
            CatLog.d(this, "CmdType: " + resMsg.mCmdDet.typeOfCommand);
            validResponse = true;
        } else if (mCurrntCmd != null) {
            validResponse = resMsg.mCmdDet.compareTo(mCurrntCmd.mCmdDet);
            CatLog.d(this, "isResponse for last valid cmd: " + validResponse);
        }
        return validResponse;
    }

    private boolean removeMenu(Menu menu) {
        try {
            if (menu.items.size() == 1 && menu.items.get(0) == null) {
                return true;
            }
        } catch (NullPointerException e) {
            CatLog.d(this, "Unable to get Menu's items size");
            return true;
        }
        return false;
    }

    private void handleCmdResponse(CatResponseMessage resMsg) {
        // Make sure the response details match the last valid command. An invalid
        // response is a one that doesn't have a corresponding proactive command
        // and sending it can "confuse" the baseband/ril.
        // One reason for out of order responses can be UI glitches. For example,
        // if the application launch an activity, and that activity is stored
        // by the framework inside the history stack. That activity will be
        // available for relaunch using the latest application dialog
        // (long press on the home button). Relaunching that activity can send
        // the same command's result again to the CatService and can cause it to
        // get out of sync with the SIM. This can happen in case of
        // non-interactive type Setup Event List and SETUP_MENU proactive commands.
        // Stk framework would have already sent Terminal Response to Setup Event
        // List and SETUP_MENU proactive commands. After sometime Stk app will send
        // Envelope Command/Event Download. In which case, the response details doesn't
        // match with last valid command (which are not related).
        // However, we should allow Stk framework to send the message to ICC.
        if (!validateResponse(resMsg)) {
            return;
        }
        ResponseData resp = null;
        boolean helpRequired = false;
        CommandDetails cmdDet = resMsg.getCmdDetails();
        AppInterface.CommandType type = AppInterface.CommandType.fromInt(cmdDet.typeOfCommand);

        switch (resMsg.mResCode) {
        case HELP_INFO_REQUIRED:
            helpRequired = true;
            // fall through
        case OK:
        case PRFRMD_WITH_PARTIAL_COMPREHENSION:
        case PRFRMD_WITH_MISSING_INFO:
        case PRFRMD_WITH_ADDITIONAL_EFS_READ:
        case PRFRMD_ICON_NOT_DISPLAYED:
        case PRFRMD_MODIFIED_BY_NAA:
        case PRFRMD_LIMITED_SERVICE:
        case PRFRMD_WITH_MODIFICATION:
        case PRFRMD_NAA_NOT_ACTIVE:
        case PRFRMD_TONE_NOT_PLAYED:
        case LAUNCH_BROWSER_ERROR:
        case TERMINAL_CRNTLY_UNABLE_TO_PROCESS:
            switch (type) {
            case SET_UP_MENU:
                helpRequired = resMsg.mResCode == ResultCode.HELP_INFO_REQUIRED;
                sendMenuSelection(resMsg.mUsersMenuSelection, helpRequired);
                return;
            case SELECT_ITEM:
                resp = new SelectItemResponseData(resMsg.mUsersMenuSelection);
                break;
            case GET_INPUT:
            case GET_INKEY:
                Input input = mCurrntCmd.geInput();
                if (!input.yesNo) {
                    // when help is requested there is no need to send the text
                    // string object.
                    if (!helpRequired) {
                        resp = new GetInkeyInputResponseData(resMsg.mUsersInput,
                                input.ucs2, input.packed);
                    }
                } else {
                    resp = new GetInkeyInputResponseData(
                            resMsg.mUsersYesNoSelection);
                }
                break;
            case DISPLAY_TEXT:
                if (resMsg.mResCode == ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS) {
                    // For screenbusy case there will be addtional information in the terminal
                    // response. And the value of the additional information byte is 0x01.
                    resMsg.setAdditionalInfo(0x01);
                } else {
                    resMsg.mIncludeAdditionalInfo = false;
                    resMsg.mAdditionalInfo = 0;
                }
                break;
            case LAUNCH_BROWSER:
                if (resMsg.mResCode == ResultCode.LAUNCH_BROWSER_ERROR) {
                    // Additional info for Default URL unavailable.
                    resMsg.setAdditionalInfo(0x04);
                } else {
                    resMsg.mIncludeAdditionalInfo = false;
                    resMsg.mAdditionalInfo = 0;
                }
                break;
            // 3GPP TS.102.223: Open Channel alpha confirmation should not send TR
            case OPEN_CHANNEL:
            case SET_UP_CALL:
                mCmdIf.handleCallSetupRequestFromSim(resMsg.mUsersConfirm, null);
                // No need to send terminal response for SET UP CALL. The user's
                // confirmation result is send back using a dedicated ril message
                // invoked by the CommandInterface call above.
                mCurrntCmd = null;
                return;
            case SET_UP_EVENT_LIST:
                if (IDLE_SCREEN_AVAILABLE_EVENT == resMsg.mEventValue) {
                    eventDownload(resMsg.mEventValue, DEV_ID_DISPLAY, DEV_ID_UICC,
                            resMsg.mAddedInfo, false);
                 } else {
                     eventDownload(resMsg.mEventValue, DEV_ID_TERMINAL, DEV_ID_UICC,
                            resMsg.mAddedInfo, false);
                 }
                // No need to send the terminal response after event download.
                return;
            default:
                break;
            }
            break;
        case BACKWARD_MOVE_BY_USER:
        case USER_NOT_ACCEPT:
            // if the user dismissed the alert dialog for a
            // setup call/open channel, consider that as the user
            // rejecting the call. Use dedicated API for this, rather than
            // sending a terminal response.
            if (type == CommandType.SET_UP_CALL || type == CommandType.OPEN_CHANNEL) {
                mCmdIf.handleCallSetupRequestFromSim(false, null);
                mCurrntCmd = null;
                return;
            } else {
                resp = null;
            }
            break;
        case NO_RESPONSE_FROM_USER:
            // No need to send terminal response for SET UP CALL on user timeout,
            // instead use dedicated API
            if (type == CommandType.SET_UP_CALL) {
                mCmdIf.handleCallSetupRequestFromSim(false, null);
                mCurrntCmd = null;
                return;
            }
        case UICC_SESSION_TERM_BY_USER:
            resp = null;
            break;
        default:
            return;
        }
        sendTerminalResponse(cmdDet, resMsg.mResCode, resMsg.mIncludeAdditionalInfo,
                resMsg.mAdditionalInfo, resp);
        mCurrntCmd = null;
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private boolean isStkAppInstalled() {
        Intent intent = new Intent(AppInterface.CAT_CMD_ACTION);
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> broadcastReceivers =
                            pm.queryBroadcastReceivers(intent, PackageManager.GET_META_DATA);
        int numReceiver = broadcastReceivers == null ? 0 : broadcastReceivers.size();

        return (numReceiver > 0);
    }

    public void update(CommandsInterface ci,
            Context context, UiccProfile uiccProfile) {
        UiccCardApplication ca = null;
        IccRecords ir = null;

        if (uiccProfile != null) {
            /* Since Cat is not tied to any application, but rather is Uicc application
             * in itself - just get first FileHandler and IccRecords object
             */
            ca = uiccProfile.getApplicationIndex(0);
            if (ca != null) {
                ir = ca.getIccRecords();
            }
        }

        synchronized (sInstanceLock) {
            if ((ir != null) && (mIccRecords != ir)) {
                if (mIccRecords != null) {
                    mIccRecords.unregisterForRecordsLoaded(this);
                }

                CatLog.d(this,
                        "Reinitialize the Service with SIMRecords and UiccCardApplication");
                mIccRecords = ir;
                mUiccApplication = ca;

                // re-Register for SIM ready event.
                mIccRecords.registerForRecordsLoaded(this, MSG_ID_ICC_RECORDS_LOADED, null);
                CatLog.d(this, "registerForRecordsLoaded slotid=" + mSlotId + " instance:" + this);
            }
        }
    }

    void updateIccAvailability() {
        if (null == mUiccController) {
            return;
        }

        CardState newState = CardState.CARDSTATE_ABSENT;
        UiccCard newCard = mUiccController.getUiccCard(mSlotId);
        if (newCard != null) {
            newState = newCard.getCardState();
        }
        CardState oldState = mCardState;
        mCardState = newState;
        CatLog.d(this,"New Card State = " + newState + " " + "Old Card State = " + oldState);
        if (oldState == CardState.CARDSTATE_PRESENT &&
                newState != CardState.CARDSTATE_PRESENT) {
            broadcastCardStateAndIccRefreshResp(newState, null);
        } else if (oldState != CardState.CARDSTATE_PRESENT &&
                newState == CardState.CARDSTATE_PRESENT) {
            // Card moved to PRESENT STATE.
            mCmdIf.reportStkServiceIsRunning(null);
        }
    }

    private void changeLanguage(String language) throws RemoteException {
        // get locale list, combined with language locale and default locale list.
        LocaleList defaultLocaleList = LocaleList.getDefault();
        Locale[] locales = new Locale[defaultLocaleList.size() + 1];
        locales[0] = new Locale(language);
        for (int i = 0; i < defaultLocaleList.size(); i++) {
            locales[i+1] = defaultLocaleList.get(i);
        }
        mContext.getSystemService(ActivityManager.class).setDeviceLocales(new LocaleList(locales));
        BackupManager.dataChanged("com.android.providers.settings");
    }
}
