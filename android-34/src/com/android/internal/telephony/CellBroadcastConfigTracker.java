/*
 * Copyright 2022 The Android Open Source Project
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

import android.annotation.NonNull;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Message;
import android.telephony.CellBroadcastIdRange;
import android.telephony.SmsCbMessage;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.cdma.CdmaSmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * This class is to track the state to set cell broadcast config
 */

public final class CellBroadcastConfigTracker extends StateMachine {
    private static final boolean DBG = Build.IS_DEBUGGABLE;

    private static final int EVENT_REQUEST = 1;
    private static final int EVENT_CONFIGURATION_DONE = 2;
    private static final int EVENT_ACTIVATION_DONE = 3;
    private static final int EVENT_RADIO_OFF = 4;
    private static final int EVENT_SUBSCRIPTION_CHANGED = 5;

    private static final int SMS_CB_CODE_SCHEME_MIN = 0;
    private static final int SMS_CB_CODE_SCHEME_MAX = 255;

    // Cache of current cell broadcast id ranges of 3gpp
    private List<CellBroadcastIdRange> mCbRanges3gpp = new CopyOnWriteArrayList<>();
    // Cache of current cell broadcast id ranges of 3gpp2
    private List<CellBroadcastIdRange> mCbRanges3gpp2 = new CopyOnWriteArrayList<>();
    private Phone mPhone;
    @VisibleForTesting
    public int mSubId;
    @VisibleForTesting
    public final SubscriptionManager.OnSubscriptionsChangedListener mSubChangedListener =
            new SubscriptionManager.OnSubscriptionsChangedListener() {
                @Override
                public void onSubscriptionsChanged() {
                    sendMessage(EVENT_SUBSCRIPTION_CHANGED);
                }
            };

    /**
     * The class is to present the request to set cell broadcast id ranges
     */
    private static class Request {
        private final List<CellBroadcastIdRange> mCbRangesRequest3gpp =
                new CopyOnWriteArrayList<>();
        private final List<CellBroadcastIdRange> mCbRangesRequest3gpp2 =
                new CopyOnWriteArrayList<>();
        Consumer<Integer> mCallback;

        Request(@NonNull List<CellBroadcastIdRange> ranges, @NonNull Consumer<Integer> callback) {
            ranges.forEach(r -> {
                if (r.getType() == SmsCbMessage.MESSAGE_FORMAT_3GPP) {
                    mCbRangesRequest3gpp.add(r);
                } else {
                    mCbRangesRequest3gpp2.add(r);
                }
            });
            mCallback = callback;
        }

        List<CellBroadcastIdRange> get3gppRanges() {
            return mCbRangesRequest3gpp;
        }

        List<CellBroadcastIdRange> get3gpp2Ranges() {
            return mCbRangesRequest3gpp2;
        }

        Consumer<Integer> getCallback() {
            return mCallback;
        }

        @Override
        public String toString() {
            return "Request[mCbRangesRequest3gpp = " + mCbRangesRequest3gpp + ", "
                    + "mCbRangesRequest3gpp2 = " + mCbRangesRequest3gpp2 + ", "
                    + "mCallback = " + mCallback + "]";
        }
    }

    /**
     * The default state.
     */
    private class DefaultState extends State {
        @Override
        public void enter() {
            mPhone.registerForRadioOffOrNotAvailable(getHandler(), EVENT_RADIO_OFF, null);
            mPhone.getContext().getSystemService(SubscriptionManager.class)
                    .addOnSubscriptionsChangedListener(new HandlerExecutor(getHandler()),
                            mSubChangedListener);
        }

        @Override
        public void exit() {
            mPhone.unregisterForRadioOffOrNotAvailable(getHandler());
            mPhone.getContext().getSystemService(SubscriptionManager.class)
                    .removeOnSubscriptionsChangedListener(mSubChangedListener);
        }

        @Override
        public boolean processMessage(Message msg) {
            boolean retVal = HANDLED;
            if (DBG) {
                logd("DefaultState message:" + msg.what);
            }
            switch (msg.what) {
                case EVENT_RADIO_OFF:
                    resetConfig();
                    break;
                case EVENT_SUBSCRIPTION_CHANGED:
                    int subId = mPhone.getSubId();
                    if (mSubId != subId) {
                        log("SubId changed from " + mSubId + " to " + subId);
                        mSubId = subId;
                        resetConfig();
                    }
                    break;
                default:
                    log("unexpected message!");
                    break;

            }

            return retVal;
        }
    }

    private DefaultState mDefaultState = new DefaultState();

    /*
     * The idle state which does not have ongoing radio request.
     */
    private class IdleState extends State {
        @Override
        public boolean processMessage(Message msg) {
            boolean retVal = NOT_HANDLED;
            if (DBG) {
                logd("IdleState message:" + msg.what);
            }
            switch (msg.what) {
                case EVENT_REQUEST:
                    Request request = (Request) msg.obj;
                    if (DBG) {
                        logd("IdleState handle EVENT_REQUEST with request:" + request);
                    }
                    if (!mCbRanges3gpp.equals(request.get3gppRanges())) {
                        // set gsm config if the config is changed
                        setGsmConfig(request.get3gppRanges(), request);
                        transitionTo(mGsmConfiguringState);
                    } else if (!mCbRanges3gpp2.equals(request.get3gpp2Ranges())) {
                        // set cdma config directly if no gsm config change but cdma config is
                        // changed
                        setCdmaConfig(request.get3gpp2Ranges(), request);
                        transitionTo(mCdmaConfiguringState);
                    } else {
                        logd("Do nothing as the requested ranges are same as now");
                        request.getCallback().accept(
                                TelephonyManager.CELL_BROADCAST_RESULT_SUCCESS);
                    }
                    retVal = HANDLED;
                    break;
                default:
                    break;
            }
            return retVal;
        }
    }
    private IdleState mIdleState = new IdleState();

    /*
     * The state waiting for the result to set gsm config.
     */
    private class GsmConfiguringState extends State {
        @Override
        public boolean processMessage(Message msg) {
            boolean retVal = NOT_HANDLED;
            if (DBG) {
                logd("GsmConfiguringState message:" + msg.what);
            }
            switch (msg.what) {
                case EVENT_REQUEST:
                    deferMessage(msg);
                    retVal = HANDLED;
                    break;
                case EVENT_CONFIGURATION_DONE:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    Request request = (Request) ar.userObj;
                    if (DBG) {
                        logd("GsmConfiguringState handle EVENT_CONFIGURATION_DONE with request:"
                                + request);
                    }
                    if (ar.exception == null) {
                        // set gsm activation and transit to gsm activating state
                        setActivation(SmsCbMessage.MESSAGE_FORMAT_3GPP,
                                !request.get3gppRanges().isEmpty(), request);
                        transitionTo(mGsmActivatingState);
                    } else {
                        logd("Failed to set gsm config");
                        request.getCallback().accept(
                                TelephonyManager.CELL_BROADCAST_RESULT_FAIL_CONFIG);
                        // transit to idle state on the failure case
                        transitionTo(mIdleState);
                    }
                    retVal = HANDLED;
                    break;
                default:
                    break;
            }
            return retVal;
        }
    }
    private GsmConfiguringState mGsmConfiguringState = new GsmConfiguringState();

    /*
     * The state waiting for the result to set gsm activation.
     */
    private class GsmActivatingState extends State {
        @Override
        public boolean processMessage(Message msg) {
            boolean retVal = NOT_HANDLED;
            if (DBG) {
                logd("GsmActivatingState message:" + msg.what);
            }
            switch (msg.what) {
                case EVENT_REQUEST:
                    deferMessage(msg);
                    retVal = HANDLED;
                    break;
                case EVENT_ACTIVATION_DONE:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    Request request = (Request) ar.userObj;
                    if (DBG) {
                        logd("GsmActivatingState handle EVENT_ACTIVATION_DONE with request:"
                                + request);
                    }
                    if (ar.exception == null) {
                        mCbRanges3gpp = request.get3gppRanges();
                        if (!mCbRanges3gpp2.equals(request.get3gpp2Ranges())) {
                            // set cdma config and transit to cdma configuring state if the config
                            // is changed.
                            setCdmaConfig(request.get3gpp2Ranges(), request);
                            transitionTo(mCdmaConfiguringState);
                        } else {
                            logd("Done as no need to update ranges for 3gpp2");
                            request.getCallback().accept(
                                    TelephonyManager.CELL_BROADCAST_RESULT_SUCCESS);
                            // transit to idle state if there is no cdma config change
                            transitionTo(mIdleState);
                        }
                    } else {
                        logd("Failed to set gsm activation");
                        request.getCallback().accept(
                                TelephonyManager.CELL_BROADCAST_RESULT_FAIL_ACTIVATION);
                        // transit to idle state on the failure case
                        transitionTo(mIdleState);
                    }
                    retVal = HANDLED;
                    break;
                default:
                    break;
            }
            return retVal;
        }
    }
    private GsmActivatingState mGsmActivatingState = new GsmActivatingState();

    /*
     * The state waiting for the result to set cdma config.
     */
    private class CdmaConfiguringState extends State {
        @Override
        public boolean processMessage(Message msg) {
            boolean retVal = NOT_HANDLED;
            if (DBG) {
                logd("CdmaConfiguringState message:" + msg.what);
            }
            switch (msg.what) {
                case EVENT_REQUEST:
                    deferMessage(msg);
                    retVal = HANDLED;
                    break;
                case EVENT_CONFIGURATION_DONE:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    Request request = (Request) ar.userObj;
                    if (DBG) {
                        logd("CdmaConfiguringState handle EVENT_ACTIVATION_DONE with request:"
                                + request);
                    }
                    if (ar.exception == null) {
                        // set cdma activation and transit to cdma activating state
                        setActivation(SmsCbMessage.MESSAGE_FORMAT_3GPP2,
                                !request.get3gpp2Ranges().isEmpty(), request);
                        transitionTo(mCdmaActivatingState);
                    } else {
                        logd("Failed to set cdma config");
                        request.getCallback().accept(
                                TelephonyManager.CELL_BROADCAST_RESULT_FAIL_CONFIG);
                        // transit to idle state on the failure case
                        transitionTo(mIdleState);
                    }
                    retVal = HANDLED;
                    break;
                default:
                    break;
            }
            return retVal;
        }
    }
    private CdmaConfiguringState mCdmaConfiguringState = new CdmaConfiguringState();

    /*
     * The state waiting for the result to set cdma activation.
     */
    private class CdmaActivatingState extends State {
        @Override
        public boolean processMessage(Message msg) {
            boolean retVal = NOT_HANDLED;
            if (DBG) {
                logd("CdmaActivatingState message:" + msg.what);
            }
            switch (msg.what) {
                case EVENT_REQUEST:
                    deferMessage(msg);
                    retVal = HANDLED;
                    break;
                case EVENT_ACTIVATION_DONE:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    Request request = (Request) ar.userObj;
                    if (DBG) {
                        logd("CdmaActivatingState handle EVENT_ACTIVATION_DONE with request:"
                                + request);
                    }
                    if (ar.exception == null) {
                        mCbRanges3gpp2 = request.get3gpp2Ranges();
                        request.getCallback().accept(
                                    TelephonyManager.CELL_BROADCAST_RESULT_SUCCESS);
                    } else {
                        logd("Failed to set cdma activation");
                        request.getCallback().accept(
                                TelephonyManager.CELL_BROADCAST_RESULT_FAIL_ACTIVATION);
                    }
                    // transit to idle state anyway
                    transitionTo(mIdleState);
                    retVal = HANDLED;
                    break;
                default:
                    break;
            }
            return retVal;
        }
    }
    private CdmaActivatingState mCdmaActivatingState = new CdmaActivatingState();

    private CellBroadcastConfigTracker(Phone phone) {
        super("CellBroadcastConfigTracker-" + phone.getPhoneId());
        init(phone);
    }

    private CellBroadcastConfigTracker(Phone phone, Handler handler) {
        super("CellBroadcastConfigTracker-" + phone.getPhoneId(), handler);
        init(phone);
    }

    private void init(Phone phone) {
        logd("init");
        mPhone = phone;
        mSubId = mPhone.getSubId();

        addState(mDefaultState);
        addState(mIdleState, mDefaultState);
        addState(mGsmConfiguringState, mDefaultState);
        addState(mGsmActivatingState, mDefaultState);
        addState(mCdmaConfiguringState, mDefaultState);
        addState(mCdmaActivatingState, mDefaultState);
        setInitialState(mIdleState);
    }

    /**
     * create a CellBroadcastConfigTracker instance for the phone
     */
    public static CellBroadcastConfigTracker make(Phone phone, Handler handler,
            boolean shouldStart) {
        CellBroadcastConfigTracker tracker = handler == null
                ? new CellBroadcastConfigTracker(phone)
                : new CellBroadcastConfigTracker(phone, handler);
        if (shouldStart) {
            tracker.start();
        }
        return tracker;
    }

    /**
     * Return current cell broadcast ranges.
     */
    @NonNull public List<CellBroadcastIdRange> getCellBroadcastIdRanges() {
        List<CellBroadcastIdRange> ranges = new ArrayList<>();
        ranges.addAll(mCbRanges3gpp);
        ranges.addAll(mCbRanges3gpp2);
        return ranges;
    }

    /**
     * Set reception of cell broadcast messages with the list of the given ranges.
     */
    public void setCellBroadcastIdRanges(
            @NonNull List<CellBroadcastIdRange> ranges, @NonNull Consumer<Integer> callback) {
        if (DBG) {
            logd("setCellBroadcastIdRanges with ranges:" + ranges);
        }
        ranges = mergeRangesAsNeeded(ranges);
        sendMessage(EVENT_REQUEST, new Request(ranges, callback));
    }

    /**
     * Merge the overlapped CellBroadcastIdRanges in the list as needed
     * @param ranges the list of CellBroadcastIdRanges
     * @return the list of CellBroadcastIdRanges without overlapping
     *
     * @throws IllegalArgumentException if there is conflict of the ranges. For instance,
     * the channel is enabled in some range, but disable in others.
     */
    @VisibleForTesting
    public static @NonNull List<CellBroadcastIdRange> mergeRangesAsNeeded(
            @NonNull List<CellBroadcastIdRange> ranges) throws IllegalArgumentException {
        ranges.sort((r1, r2) -> r1.getType() != r2.getType() ? r1.getType() - r2.getType()
                : (r1.getStartId() != r2.getStartId() ? r1.getStartId() - r2.getStartId()
                : r2.getEndId() - r1.getEndId()));
        final List<CellBroadcastIdRange> newRanges = new ArrayList<>();
        ranges.forEach(r -> {
            if (newRanges.isEmpty() || newRanges.get(newRanges.size() - 1).getType() != r.getType()
                    || newRanges.get(newRanges.size() - 1).getEndId() + 1 < r.getStartId()
                    || (newRanges.get(newRanges.size() - 1).getEndId() + 1 == r.getStartId()
                    && newRanges.get(newRanges.size() - 1).isEnabled() != r.isEnabled())) {
                newRanges.add(new CellBroadcastIdRange(r.getStartId(), r.getEndId(),
                        r.getType(), r.isEnabled()));
            } else {
                if (newRanges.get(newRanges.size() - 1).isEnabled() != r.isEnabled()) {
                    throw new IllegalArgumentException("range conflict " + r);
                }
                if (r.getEndId() > newRanges.get(newRanges.size() - 1).getEndId()) {
                    CellBroadcastIdRange range = newRanges.get(newRanges.size() - 1);
                    newRanges.set(newRanges.size() - 1, new CellBroadcastIdRange(
                            range.getStartId(), r.getEndId(), range.getType(), range.isEnabled()));
                }
            }
        });
        return newRanges;
    }

    private void resetConfig() {
        mCbRanges3gpp.clear();
        mCbRanges3gpp2.clear();
    }

    private void setGsmConfig(List<CellBroadcastIdRange> ranges, Request request) {
        if (DBG) {
            logd("setGsmConfig with " + ranges);
        }

        SmsBroadcastConfigInfo[] configs = new SmsBroadcastConfigInfo[ranges.size()];
        for (int i = 0; i < configs.length; i++) {
            CellBroadcastIdRange r = ranges.get(i);
            configs[i] = new SmsBroadcastConfigInfo(r.getStartId(), r.getEndId(),
                    SMS_CB_CODE_SCHEME_MIN, SMS_CB_CODE_SCHEME_MAX, r.isEnabled());
        }

        Message response = obtainMessage(EVENT_CONFIGURATION_DONE, request);
        mPhone.mCi.setGsmBroadcastConfig(configs, response);
    }

    private void setCdmaConfig(List<CellBroadcastIdRange> ranges, Request request) {
        if (DBG) {
            logd("setCdmaConfig with " + ranges);
        }

        CdmaSmsBroadcastConfigInfo[] configs =
                new CdmaSmsBroadcastConfigInfo[ranges.size()];
        for (int i = 0; i < configs.length; i++) {
            CellBroadcastIdRange r = ranges.get(i);
            configs[i] = new CdmaSmsBroadcastConfigInfo(
                    r.getStartId(), r.getEndId(), 1, r.isEnabled());
        }

        Message response = obtainMessage(EVENT_CONFIGURATION_DONE, request);
        mPhone.mCi.setCdmaBroadcastConfig(configs, response);
    }

    private void setActivation(int type, boolean activate, Request request) {
        if (DBG) {
            logd("setActivation(" + type + "." + activate + ')');
        }

        Message response = obtainMessage(EVENT_ACTIVATION_DONE, request);

        if (type == SmsCbMessage.MESSAGE_FORMAT_3GPP) {
            mPhone.mCi.setGsmBroadcastActivation(activate, response);
        } else if (type == SmsCbMessage.MESSAGE_FORMAT_3GPP2) {
            mPhone.mCi.setCdmaBroadcastActivation(activate, response);
        }
    }
}
