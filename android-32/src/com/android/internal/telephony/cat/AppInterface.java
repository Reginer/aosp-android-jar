/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.compat.annotation.UnsupportedAppUsage;
import android.content.ComponentName;
import android.os.Build;

/**
 * Interface for communication between STK App and CAT Telephony
 *
 * {@hide}
 */
public interface AppInterface {

    /*
     * Intent's actions which are broadcasted by the Telephony once a new CAT
     * proactive command, session end, ALPHA during STK CC arrive.
     */
    public static final String CAT_CMD_ACTION =
                                    "com.android.internal.stk.command";
    public static final String CAT_SESSION_END_ACTION =
                                    "com.android.internal.stk.session_end";
    public static final String CAT_ALPHA_NOTIFY_ACTION =
                                    "com.android.internal.stk.alpha_notify";

    //This is used to send ALPHA string from card to STK App.
    public static final String ALPHA_STRING = "alpha_string";

    // This is used to send refresh-result when MSG_ID_ICC_REFRESH is received.
    public static final String REFRESH_RESULT = "refresh_result";
    //This is used to send card status from card to STK App.
    public static final String CARD_STATUS = "card_status";
    //Intent's actions are broadcasted by Telephony once IccRefresh occurs.
    public static final String CAT_ICC_STATUS_CHANGE =
                                    "com.android.internal.stk.icc_status_change";

    // Permission required by STK command receiver
    public static final String STK_PERMISSION = "android.permission.RECEIVE_STK_COMMANDS";

    // Only forwards cat broadcast to the system default stk app
    public static ComponentName getDefaultSTKApplication() {
        return ComponentName.unflattenFromString("com.android.stk/.StkCmdReceiver");
    }

    /*
     * Callback function from app to telephony to pass a result code and user's
     * input back to the ICC.
     */
    void onCmdResponse(CatResponseMessage resMsg);

    /**
     * Dispose when the service is not longer needed.
     */
    void dispose();

    /*
     * Enumeration for representing "Type of Command" of proactive commands.
     * Those are the only commands which are supported by the Telephony. Any app
     * implementation should support those.
     * Refer to ETSI TS 102.223 section 9.4
     */
    @UnsupportedAppUsage(implicitMember =
            "values()[Lcom/android/internal/telephony/cat/AppInterface$CommandType;")
    public static enum CommandType {
        @UnsupportedAppUsage
        DISPLAY_TEXT(0x21),
        @UnsupportedAppUsage
        GET_INKEY(0x22),
        @UnsupportedAppUsage
        GET_INPUT(0x23),
        @UnsupportedAppUsage
        LAUNCH_BROWSER(0x15),
        @UnsupportedAppUsage
        PLAY_TONE(0x20),
        @UnsupportedAppUsage
        REFRESH(0x01),
        @UnsupportedAppUsage
        SELECT_ITEM(0x24),
        @UnsupportedAppUsage
        SEND_SS(0x11),
        @UnsupportedAppUsage
        SEND_USSD(0x12),
        @UnsupportedAppUsage
        SEND_SMS(0x13),
        RUN_AT(0x34),
        @UnsupportedAppUsage
        SEND_DTMF(0x14),
        @UnsupportedAppUsage
        SET_UP_EVENT_LIST(0x05),
        @UnsupportedAppUsage
        SET_UP_IDLE_MODE_TEXT(0x28),
        @UnsupportedAppUsage
        SET_UP_MENU(0x25),
        @UnsupportedAppUsage
        SET_UP_CALL(0x10),
        @UnsupportedAppUsage
        PROVIDE_LOCAL_INFORMATION(0x26),
        @UnsupportedAppUsage
        LANGUAGE_NOTIFICATION(0x35),
        @UnsupportedAppUsage
        OPEN_CHANNEL(0x40),
        @UnsupportedAppUsage
        CLOSE_CHANNEL(0x41),
        @UnsupportedAppUsage
        RECEIVE_DATA(0x42),
        @UnsupportedAppUsage
        SEND_DATA(0x43),
        @UnsupportedAppUsage
        GET_CHANNEL_STATUS(0x44);

        private int mValue;

        CommandType(int value) {
            mValue = value;
        }

        public int value() {
            return mValue;
        }

        /**
         * Create a CommandType object.
         *
         * @param value Integer value to be converted to a CommandType object.
         * @return CommandType object whose "Type of Command" value is {@code
         *         value}. If no CommandType object has that value, null is
         *         returned.
         */
        @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
        public static CommandType fromInt(int value) {
            for (CommandType e : CommandType.values()) {
                if (e.mValue == value) {
                    return e;
                }
            }
            return null;
        }
    }
}
