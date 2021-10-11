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

package com.android.internal.telephony.uicc.euicc.apdu;

import java.util.ArrayList;
import java.util.List;

/**
 * A helper class to build a request for sending APDU commands. It contains a few convenient methods
 * to add different APDU commands to the request.
 *
 * @hide
 */
public class RequestBuilder {
    // The maximum bytes of the data of an APDU command. If more bytes need to be sent than this
    // limit, we need to separate them into multiple commands.
    private static final int MAX_APDU_DATA_LEN = 0xFF;
    // The maximum bytes of the data of an extended APDU command. If more bytes need to be sent than
    // this limit, we need to separate them into multiple commands.
    private static final int MAX_EXT_APDU_DATA_LEN = 0xFFFF;

    // Parameters used by STORE DATA command sent to ISD-R, defined by SGP.22 v2.0.
    private static final int CLA_STORE_DATA = 0x80;
    private static final int INS_STORE_DATA = 0xE2;
    private static final int P1_STORE_DATA_INTERM = 0x11;
    private static final int P1_STORE_DATA_END = 0x91;

    private final int mChannel;
    private final int mMaxApduDataLen;
    private final List<ApduCommand> mCommands = new ArrayList<>();

    /**
     * Adds an APDU command by specifying every parts. The parameters are defined as in
     * GlobalPlatform Card Specification v.2.3.
     */
    public void addApdu(int cla, int ins, int p1, int p2, int p3, String cmdHex) {
        mCommands.add(new ApduCommand(mChannel, cla, ins, p1, p2, p3, cmdHex));
    }

    /**
     * Adds an APDU command with given command data. P3 will be the length of the command data
     * bytes. The parameters are defined as in GlobalPlatform Card Specification v.2.3.
     */
    public void addApdu(int cla, int ins, int p1, int p2, String cmdHex) {
        mCommands.add(new ApduCommand(mChannel, cla, ins, p1, p2, cmdHex.length() / 2, cmdHex));
    }

    /**
     * Adds an APDU command with empty command data. The parameters are defined as in GlobalPlatform
     * Card Specification v.2.3.
     */
    public void addApdu(int cla, int ins, int p1, int p2) {
        mCommands.add(new ApduCommand(mChannel, cla, ins, p1, p2, 0, ""));
    }

    /**
     * Adds a STORE DATA command. Long command length of which is larger than {@link
     * #mMaxApduDataLen} will be automatically split into multiple ones.
     *
     * @param cmdHex The STORE DATA command in a hex string as defined in GlobalPlatform Card
     *     Specification v.2.3.
     */
    public void addStoreData(String cmdHex) {
        final int cmdLen = mMaxApduDataLen * 2;
        int startPos = 0;
        int totalLen = cmdHex.length() / 2;
        int totalSubCmds = totalLen == 0 ? 1 : (totalLen + mMaxApduDataLen - 1) / mMaxApduDataLen;
        for (int i = 1; i < totalSubCmds; ++i) {
            String data = cmdHex.substring(startPos, startPos + cmdLen);
            addApdu(CLA_STORE_DATA, INS_STORE_DATA, P1_STORE_DATA_INTERM, i - 1, data);
            startPos += cmdLen;
        }
        String data = cmdHex.substring(startPos);
        addApdu(CLA_STORE_DATA, INS_STORE_DATA, P1_STORE_DATA_END, totalSubCmds - 1, data);
    }

    List<ApduCommand> getCommands() {
        return mCommands;
    }

    RequestBuilder(int channel, boolean supportExtendedApdu) {
        mChannel = channel;
        mMaxApduDataLen = supportExtendedApdu ? MAX_EXT_APDU_DATA_LEN : MAX_APDU_DATA_LEN;
    }
}
