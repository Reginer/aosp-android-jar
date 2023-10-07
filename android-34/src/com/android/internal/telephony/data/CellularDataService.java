/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.internal.telephony.data;

import static android.telephony.data.DataServiceCallback.RESULT_SUCCESS;

import android.annotation.Nullable;
import android.net.LinkProperties;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.SubscriptionManager;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataProfile;
import android.telephony.data.DataService;
import android.telephony.data.DataServiceCallback;
import android.telephony.data.NetworkSliceInfo;
import android.telephony.data.TrafficDescriptor;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.telephony.Rlog;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents cellular data service which handles telephony data requests and response
 * from the cellular modem.
 */
public class CellularDataService extends DataService {
    private static final String TAG = CellularDataService.class.getSimpleName();

    private static final boolean DBG = false;

    private static final int SETUP_DATA_CALL_COMPLETE               = 1;
    private static final int DEACTIVATE_DATA_CALL_COMPLETE          = 2;
    private static final int SET_INITIAL_ATTACH_APN_COMPLETE        = 3;
    private static final int SET_DATA_PROFILE_COMPLETE              = 4;
    private static final int REQUEST_DATA_CALL_LIST_COMPLETE        = 5;
    private static final int DATA_CALL_LIST_CHANGED                 = 6;
    private static final int START_HANDOVER                         = 7;
    private static final int CANCEL_HANDOVER                        = 8;
    private static final int APN_UNTHROTTLED                        = 9;

    private class CellularDataServiceProvider extends DataService.DataServiceProvider {

        private final Map<Message, DataServiceCallback> mCallbackMap = new HashMap<>();

        private final Handler mHandler;

        private final Phone mPhone;

        private CellularDataServiceProvider(int slotId) {
            super(slotId);

            mPhone = PhoneFactory.getPhone(getSlotIndex());

            mHandler = new Handler(Looper.myLooper()) {
                @Override
                public void handleMessage(Message message) {
                    DataServiceCallback callback = mCallbackMap.remove(message);

                    AsyncResult ar = (AsyncResult) message.obj;
                    switch (message.what) {
                        case SETUP_DATA_CALL_COMPLETE:
                            DataCallResponse response = (DataCallResponse) ar.result;
                            callback.onSetupDataCallComplete(ar.exception != null
                                    ? DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE
                                    : RESULT_SUCCESS,
                                    response);
                            break;
                        case DEACTIVATE_DATA_CALL_COMPLETE:
                            callback.onDeactivateDataCallComplete(ar.exception != null
                                    ? DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE
                                    : RESULT_SUCCESS);
                            break;
                        case SET_INITIAL_ATTACH_APN_COMPLETE:
                            callback.onSetInitialAttachApnComplete(ar.exception != null
                                    ? DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE
                                    : RESULT_SUCCESS);
                            break;
                        case SET_DATA_PROFILE_COMPLETE:
                            callback.onSetDataProfileComplete(ar.exception != null
                                    ? DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE
                                    : RESULT_SUCCESS);
                            break;
                        case REQUEST_DATA_CALL_LIST_COMPLETE:
                            callback.onRequestDataCallListComplete(
                                    ar.exception != null
                                            ? DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE
                                            : RESULT_SUCCESS,
                                    ar.result != null ? (List<DataCallResponse>) ar.result
                                            : Collections.EMPTY_LIST
                                    );
                            break;
                        case DATA_CALL_LIST_CHANGED:
                            notifyDataCallListChanged((List<DataCallResponse>) ar.result);
                            break;
                        case START_HANDOVER:
                            callback.onHandoverStarted(toResultCode(ar.exception));
                            break;
                        case CANCEL_HANDOVER:
                            callback.onHandoverCancelled(toResultCode(ar.exception));
                            break;
                        case APN_UNTHROTTLED:
                            if (ar.result instanceof DataProfile) {
                                notifyDataProfileUnthrottled((DataProfile) ar.result);
                            } else {
                                notifyApnUnthrottled((String) ar.result);
                            }
                            break;
                        default:
                            loge("Unexpected event: " + message.what);
                    }
                }
            };

            if (DBG) log("Register for data call list changed.");
            mPhone.mCi.registerForDataCallListChanged(mHandler, DATA_CALL_LIST_CHANGED, null);

            if (DBG) log("Register for apn unthrottled.");
            mPhone.mCi.registerForApnUnthrottled(mHandler, APN_UNTHROTTLED, null);
        }


        /* Converts the result code for start handover and cancel handover */
        @DataServiceCallback.ResultCode private int toResultCode(@Nullable Throwable t) {
            if (t == null) {
                return RESULT_SUCCESS;
            } else {
                if (t instanceof CommandException) {
                    CommandException ce = (CommandException) t;
                    if (ce.getCommandError() == CommandException.Error.REQUEST_NOT_SUPPORTED) {
                        return DataServiceCallback.RESULT_ERROR_UNSUPPORTED;
                    } else {
                        return DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE;
                    }
                } else {
                    loge("Throwable is of type " + t.getClass().getSimpleName()
                            + " but should be CommandException");
                    return DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE;
                }
            }
        }

        @Override
        public void setupDataCall(int accessNetworkType, DataProfile dataProfile,
                boolean isRoaming, boolean allowRoaming, int reason, LinkProperties linkProperties,
                int pduSessionId, NetworkSliceInfo sliceInfo, TrafficDescriptor trafficDescriptor,
                boolean matchAllRuleAllowed, DataServiceCallback callback) {
            if (DBG) log("setupDataCall " + getSlotIndex());

            Message message = null;
            // Only obtain the message when the caller wants a callback. If the caller doesn't care
            // the request completed or results, then no need to pass the message down.
            if (callback != null) {
                message = Message.obtain(mHandler, SETUP_DATA_CALL_COMPLETE);
                mCallbackMap.put(message, callback);
            }

            mPhone.mCi.setupDataCall(accessNetworkType, dataProfile, isRoaming, allowRoaming,
                    reason, linkProperties, pduSessionId, sliceInfo, trafficDescriptor,
                    matchAllRuleAllowed, message);
        }

        @Override
        public void deactivateDataCall(int cid, int reason, DataServiceCallback callback) {
            if (DBG) log("deactivateDataCall " + getSlotIndex());

            Message message = null;
            // Only obtain the message when the caller wants a callback. If the caller doesn't care
            // the request completed or results, then no need to pass the message down.
            if (callback != null) {
                message = Message.obtain(mHandler, DEACTIVATE_DATA_CALL_COMPLETE);
                mCallbackMap.put(message, callback);
            }

            mPhone.mCi.deactivateDataCall(cid, reason, message);
        }

        @Override
        public void setInitialAttachApn(DataProfile dataProfile, boolean isRoaming,
                                        DataServiceCallback callback) {
            if (DBG) log("setInitialAttachApn " + getSlotIndex());

            Message message = null;
            // Only obtain the message when the caller wants a callback. If the caller doesn't care
            // the request completed or results, then no need to pass the message down.
            if (callback != null) {
                message = Message.obtain(mHandler, SET_INITIAL_ATTACH_APN_COMPLETE);
                mCallbackMap.put(message, callback);
            }

            mPhone.mCi.setInitialAttachApn(dataProfile, isRoaming, message);
        }

        @Override
        public void setDataProfile(List<DataProfile> dps, boolean isRoaming,
                                   DataServiceCallback callback) {
            if (DBG) log("setDataProfile " + getSlotIndex());

            Message message = null;
            // Only obtain the message when the caller wants a callback. If the caller doesn't care
            // the request completed or results, then no need to pass the message down.
            if (callback != null) {
                message = Message.obtain(mHandler, SET_DATA_PROFILE_COMPLETE);
                mCallbackMap.put(message, callback);
            }

            mPhone.mCi.setDataProfile(dps.toArray(new DataProfile[dps.size()]), isRoaming, message);
        }

        @Override
        public void requestDataCallList(DataServiceCallback callback) {
            if (DBG) log("requestDataCallList " + getSlotIndex());

            Message message = null;
            // Only obtain the message when the caller wants a callback. If the caller doesn't care
            // the request completed or results, then no need to pass the message down.
            if (callback != null) {
                message = Message.obtain(mHandler, REQUEST_DATA_CALL_LIST_COMPLETE);
                mCallbackMap.put(message, callback);
            }
            mPhone.mCi.getDataCallList(message);
        }

        @Override
        public void startHandover(int cid, DataServiceCallback callback) {
            if (DBG) log("startHandover " + getSlotIndex());
            Message message = null;
            // Only obtain the message when the caller wants a callback. If the caller doesn't care
            // the request completed or results, then no need to pass the message down.
            if (callback != null) {
                message = Message.obtain(mHandler, START_HANDOVER);
                mCallbackMap.put(message, callback);
            }
            mPhone.mCi.startHandover(message, cid);
        }

        @Override
        public void cancelHandover(int cid, DataServiceCallback callback) {
            Message message = null;
            // Only obtain the message when the caller wants a callback. If the caller doesn't care
            // the request completed or results, then no need to pass the message down.
            if (callback != null) {
                message = Message.obtain(mHandler, CANCEL_HANDOVER);
                mCallbackMap.put(message, callback);
            }
            mPhone.mCi.cancelHandover(message, cid);
        }

        @Override
        public void close() {
            mPhone.mCi.unregisterForDataCallListChanged(mHandler);
        }
    }

    @Override
    public DataServiceProvider onCreateDataServiceProvider(int slotIndex) {
        log("Cellular data service created for slot " + slotIndex);
        if (!SubscriptionManager.isValidSlotIndex(slotIndex)) {
            loge("Tried to cellular data service with invalid slotId " + slotIndex);
            return null;
        }
        return new CellularDataServiceProvider(slotIndex);
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void loge(String s) {
        Rlog.e(TAG, s);
    }
}
