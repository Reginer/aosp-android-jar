/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.hardware.radio.RadioError;
import android.hardware.radio.RadioResponseInfo;
import android.hardware.radio.messaging.IRadioMessagingResponse;

import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;

import java.util.ArrayList;

/**
 * Interface declaring response functions to solicited radio requests for messaging APIs.
 */
public class MessagingResponse extends IRadioMessagingResponse.Stub {
    private final RIL mRil;

    public MessagingResponse(RIL ril) {
        mRil = ril;
    }

    private void responseSms(RadioResponseInfo responseInfo,
            android.hardware.radio.messaging.SendSmsResult sms) {
        RILRequest rr = mRil.processResponse(RIL.MESSAGING_SERVICE, responseInfo);

        if (rr != null) {
            long messageId = RIL.getOutgoingSmsMessageId(rr.mResult);
            SmsResponse ret = new SmsResponse(sms.messageRef, sms.ackPDU, sms.errorCode, messageId);
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * Acknowledge the receipt of radio request sent to the vendor. This must be sent only for
     * radio request which take long time to respond.
     * For more details, refer https://source.android.com/devices/tech/connect/ril.html
     * @param serial Serial no. of the request whose acknowledgement is sent.
     */
    public void acknowledgeRequest(int serial) {
        mRil.processRequestAck(serial);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acknowledgeIncomingGsmSmsWithPduResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MESSAGING_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acknowledgeLastIncomingCdmaSmsResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MESSAGING_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void acknowledgeLastIncomingGsmSmsResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MESSAGING_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void deleteSmsOnRuimResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MESSAGING_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void deleteSmsOnSimResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MESSAGING_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param configs Vector of CDMA broadcast SMS configs
     */
    public void getCdmaBroadcastConfigResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.messaging.CdmaBroadcastSmsConfigInfo[] configs) {
        RILRequest rr = mRil.processResponse(RIL.MESSAGING_SERVICE, responseInfo);

        if (rr != null) {
            int[] ret;
            int numServiceCategories = configs.length;
            if (numServiceCategories == 0) {
                // TODO: The logic of providing default values should not be done by this transport
                // layer; it needs to be done by the vendor ril or application logic.
                int numInts;
                numInts = RILUtils.CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES
                        * RILUtils.CDMA_BSI_NO_OF_INTS_STRUCT + 1;
                ret = new int[numInts];

                // Faking a default record for all possible records.
                ret[0] = RILUtils.CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES;

                // Loop over CDMA_BROADCAST_SMS_NO_OF_SERVICE_CATEGORIES set 'english' as
                // default language and selection status to false for all.
                for (int i = 1; i < numInts; i += RILUtils.CDMA_BSI_NO_OF_INTS_STRUCT) {
                    ret[i] = i / RILUtils.CDMA_BSI_NO_OF_INTS_STRUCT;
                    ret[i + 1] = 1;
                    ret[i + 2] = 0;
                }
            } else {
                int numInts;
                numInts = (numServiceCategories * RILUtils.CDMA_BSI_NO_OF_INTS_STRUCT) + 1;
                ret = new int[numInts];

                ret[0] = numServiceCategories;
                for (int i = 1, j = 0; j < configs.length;
                        j++, i = i + RILUtils.CDMA_BSI_NO_OF_INTS_STRUCT) {
                    ret[i] = configs[i].serviceCategory;
                    ret[i + 1] = configs[i].language;
                    ret[i + 2] = configs[i].selected ? 1 : 0;
                }
            }
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param configs Vector of GSM/WCDMA Cell broadcast SMS configs
     */
    public void getGsmBroadcastConfigResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.messaging.GsmBroadcastSmsConfigInfo[] configs) {
        RILRequest rr = mRil.processResponse(RIL.MESSAGING_SERVICE, responseInfo);

        if (rr != null) {
            ArrayList<SmsBroadcastConfigInfo> ret = new ArrayList<>();
            for (android.hardware.radio.messaging.GsmBroadcastSmsConfigInfo info : configs) {
                ret.add(new SmsBroadcastConfigInfo(info.fromServiceId, info.toServiceId,
                        info.fromCodeScheme, info.toCodeScheme, info.selected));
            }
            if (responseInfo.error == RadioError.NONE) {
                RadioResponse.sendMessageResponse(rr.mResult, ret);
            }
            mRil.processResponseDone(rr, responseInfo, ret);
        }
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param smsc Short Message Service Center address on the device
     */
    public void getSmscAddressResponse(RadioResponseInfo responseInfo, String smsc) {
        RadioResponse.responseString(RIL.MESSAGING_SERVICE, mRil, responseInfo, smsc);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void reportSmsMemoryStatusResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MESSAGING_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to SMS sent as defined by SendSmsResult
     */
    public void sendCdmaSmsExpectMoreResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.messaging.SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to SMS sent as defined by SendSmsResult
     */
    public void sendCdmaSmsResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.messaging.SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to SMS sent as defined by SendSmsResult
     */
    public void sendImsSmsResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.messaging.SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to SMS sent as defined by SendSmsResult
     */
    public void sendSmsExpectMoreResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.messaging.SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param sms Response to sms sent as defined by SendSmsResult
     */
    public void sendSmsResponse(RadioResponseInfo responseInfo,
            android.hardware.radio.messaging.SendSmsResult sms) {
        responseSms(responseInfo, sms);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaBroadcastActivationResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MESSAGING_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setCdmaBroadcastConfigResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MESSAGING_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setGsmBroadcastActivationResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MESSAGING_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setGsmBroadcastConfigResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MESSAGING_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     */
    public void setSmscAddressResponse(RadioResponseInfo responseInfo) {
        RadioResponse.responseVoid(RIL.MESSAGING_SERVICE, mRil, responseInfo);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param index record index where the CDMA SMS message is stored
     */
    public void writeSmsToRuimResponse(RadioResponseInfo responseInfo, int index) {
        RadioResponse.responseInts(RIL.MESSAGING_SERVICE, mRil, responseInfo, index);
    }

    /**
     * @param responseInfo Response info struct containing response type, serial no. and error
     * @param index record index where the message is stored
     */
    public void writeSmsToSimResponse(RadioResponseInfo responseInfo, int index) {
        RadioResponse.responseInts(RIL.MESSAGING_SERVICE, mRil, responseInfo, index);
    }

    @Override
    public String getInterfaceHash() {
        return IRadioMessagingResponse.HASH;
    }

    @Override
    public int getInterfaceVersion() {
        return IRadioMessagingResponse.VERSION;
    }
}
