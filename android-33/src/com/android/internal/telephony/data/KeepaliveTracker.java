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

package com.android.internal.telephony.data;

import android.annotation.NonNull;
import android.net.KeepalivePacketData;
import android.net.NattKeepalivePacketData;
import android.net.SocketKeepalive;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.AccessNetworkConstants;
import android.util.SparseArray;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.data.KeepaliveStatus.KeepaliveStatusCode;
import com.android.internal.telephony.data.TelephonyNetworkAgent.TelephonyNetworkAgentCallback;
import com.android.telephony.Rlog;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Keepalive tracker tracks the active keepalive requests from the connectivity service, forwards
 * it to the modem, and handles keepalive status update from the modem.
 */
public class KeepaliveTracker extends Handler {
    /** Event for keepalive session started. */
    private static final int EVENT_KEEPALIVE_STARTED = 1;

    /** Event for keepalive session stopped. */
    private static final int EVENT_KEEPALIVE_STOPPED = 2;

    /** Event for keepalive status updated from the modem. */
    private static final int EVENT_KEEPALIVE_STATUS = 3;

    /** Event for registering keepalive status. */
    private static final int EVENT_REGISTER_FOR_KEEPALIVE_STATUS = 4;

    /** Event for unregistering keepalive status. */
    private static final int EVENT_UNREGISTER_FOR_KEEPALIVE_STATUS = 5;

    /** The phone instance. */
    private final @NonNull Phone mPhone;

    /** The parent data network. */
    private final @NonNull DataNetwork mDataNetwork;

    /** The associated network agent. */
    private final @NonNull TelephonyNetworkAgent mNetworkAgent;

    /** The log tag. */
    private final @NonNull String mLogTag;

    /** The keepalive records. */
    private final @NonNull SparseArray<KeepaliveRecord> mKeepalives = new SparseArray<>();

    /**
     * Keepalive session record
     */
    private static class KeepaliveRecord {
        /** Associated SIM slot index. */
        public int slotIndex;

        /** The current status. */
        public @KeepaliveStatusCode int currentStatus;

        /**
         * Constructor
         *
         * @param slotIndex The associated SIM slot index.
         * @param status The keepalive status.
         */
        KeepaliveRecord(int slotIndex, @KeepaliveStatusCode int status) {
            this.slotIndex = slotIndex;
            this.currentStatus = status;
        }
    }

    /**
     * Constructor
     *
     * @param phone The phone instance.
     * @param looper The looper to be used by the handler. Currently the handler thread is the
     * phone process's main thread.
     * @param dataNetwork The parent data network.
     * @param networkAgent The associated network agent.
     */
    public KeepaliveTracker(@NonNull Phone phone, @NonNull Looper looper,
            @NonNull DataNetwork dataNetwork, @NonNull TelephonyNetworkAgent networkAgent) {
        super(looper);
        mPhone = phone;
        mDataNetwork = dataNetwork;
        mNetworkAgent = networkAgent;
        mLogTag = "KT-" + networkAgent.getId();
        mNetworkAgent.registerCallback(new TelephonyNetworkAgentCallback(this::post) {
            @Override
            public void onStartSocketKeepalive(int slot, @NonNull Duration interval,
                    @NonNull KeepalivePacketData packet) {
                onStartSocketKeepaliveRequested(slot, interval, packet);
            }

            @Override
            public void onStopSocketKeepalive(int slot) {
                onStopSocketKeepaliveRequested(slot);
            }
        });
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        AsyncResult ar;
        KeepaliveStatus ks;
        int slotIndex;
        switch (msg.what) {
            case EVENT_KEEPALIVE_STARTED:
                ar = (AsyncResult) msg.obj;
                slotIndex = msg.arg1;
                if (ar.exception != null || ar.result == null) {
                    loge("EVENT_KEEPALIVE_STARTED: error starting keepalive, e="
                            + ar.exception);
                    mNetworkAgent.sendSocketKeepaliveEvent(
                            slotIndex, SocketKeepalive.ERROR_HARDWARE_ERROR);
                    break;
                }
                ks = (KeepaliveStatus) ar.result;
                onSocketKeepaliveStarted(slotIndex, ks);
                break;
            case EVENT_KEEPALIVE_STOPPED:
                ar = (AsyncResult) msg.obj;
                final int handle = msg.arg1;

                if (ar.exception != null) {
                    loge("EVENT_KEEPALIVE_STOPPED: error stopping keepalive for handle="
                            + handle + " e=" + ar.exception);
                    onKeepaliveStatus(new KeepaliveStatus(
                            KeepaliveStatus.ERROR_UNKNOWN));
                } else {
                    log("Keepalive Stop Requested for handle=" + handle);
                    onKeepaliveStatus(new KeepaliveStatus(
                            handle, KeepaliveStatus.STATUS_INACTIVE));
                }
                break;
            case EVENT_KEEPALIVE_STATUS:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    loge("EVENT_KEEPALIVE_STATUS: error in keepalive, e=" + ar.exception);
                    // We have no way to notify connectivity in this case.
                } else if (ar.result != null) {
                    ks = (KeepaliveStatus) ar.result;
                    onKeepaliveStatus(ks);
                }
                break;
            case EVENT_REGISTER_FOR_KEEPALIVE_STATUS:
                mPhone.mCi.registerForNattKeepaliveStatus(this, EVENT_KEEPALIVE_STATUS, null);
                break;
            case EVENT_UNREGISTER_FOR_KEEPALIVE_STATUS:
                mPhone.mCi.unregisterForNattKeepaliveStatus(this);
                break;
            default:
                loge("Unexpected message " + msg);
        }
    }

    /**
     * Called when connectivity service requests that the network hardware send the specified
     * packet at the specified interval.
     *
     * @param slotIndex the hardware slot on which to start the keepalive.
     * @param interval the interval between packets, between 10 and 3600. Note that this API
     * does not support sub-second precision and will round off the request.
     * @param packet the packet to send.
     */
    private void onStartSocketKeepaliveRequested(int slotIndex, @NonNull Duration interval,
            @NonNull KeepalivePacketData packet) {
        log("onStartSocketKeepaliveRequested: slot=" + slotIndex + ", interval="
                + interval.getSeconds() + "s, packet=" + packet);
        if (packet instanceof NattKeepalivePacketData) {
            if (mDataNetwork.getTransport() == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
                mPhone.mCi.startNattKeepalive(mDataNetwork.getId(), packet,
                        (int) TimeUnit.SECONDS.toMillis(interval.getSeconds()),
                        obtainMessage(EVENT_KEEPALIVE_STARTED, slotIndex, 0, null));
            } else {
                // We currently do not support NATT Keepalive requests using the
                // DataService API, so unless the request is WWAN (always bound via
                // the CommandsInterface), the request cannot be honored.
                //
                // TODO: b/72331356 to add support for Keepalive to the DataService
                // so that keepalive requests can be handled (if supported) by the
                // underlying transport.
                mNetworkAgent.sendSocketKeepaliveEvent(
                        slotIndex, SocketKeepalive.ERROR_INVALID_NETWORK);
            }
        } else {
            mNetworkAgent.sendSocketKeepaliveEvent(slotIndex, SocketKeepalive.ERROR_UNSUPPORTED);
        }
    }

    /**
     * Called when connectivity service requests that the network hardware stop a previously-started
     * keepalive.
     *
     * @param slotIndex the hardware slot on which to stop the keepalive.
     */
    private void onStopSocketKeepaliveRequested(int slotIndex) {
        log("onStopSocketKeepaliveRequested: slot=" + slotIndex);
        int handle = getHandleForSlot(slotIndex);
        if (handle < 0) {
            loge("No slot found for stopSocketKeepalive! " + slotIndex);
            mNetworkAgent.sendSocketKeepaliveEvent(
                    slotIndex, SocketKeepalive.ERROR_NO_SUCH_SLOT);
            return;
        }

        log("Stopping keepalive with handle: " + handle);
        mPhone.mCi.stopNattKeepalive(handle, obtainMessage(EVENT_KEEPALIVE_STOPPED, handle,
                slotIndex, null));
    }

    /**
     * Get the keepalive handle for the slot.
     *
     * @param slotIndex The associated SIM slot index.
     *
     * @return The keepalive handle.
     */
    private int getHandleForSlot(int slotIndex) {
        for (int i = 0; i < mKeepalives.size(); i++) {
            KeepaliveRecord kr = mKeepalives.valueAt(i);
            if (kr.slotIndex == slotIndex) return mKeepalives.keyAt(i);
        }
        return -1;
    }

    /**
     * Convert the error code.
     *
     * @param error The keepalive status error.
     * @return The socket alive error.
     */
    private int keepaliveStatusErrorToPacketKeepaliveError(int error) {
        switch(error) {
            case KeepaliveStatus.ERROR_NONE:
                return SocketKeepalive.SUCCESS;
            case KeepaliveStatus.ERROR_UNSUPPORTED:
                return SocketKeepalive.ERROR_UNSUPPORTED;
            case KeepaliveStatus.ERROR_NO_RESOURCES:
                return SocketKeepalive.ERROR_INSUFFICIENT_RESOURCES;
            case KeepaliveStatus.ERROR_UNKNOWN:
            default:
                return SocketKeepalive.ERROR_HARDWARE_ERROR;
        }
    }

    /**
     * Called when keepalive session started.
     *
     * @param slotIndex The SIM slot index.
     * @param ks Keepalive status.
     */
    private void onSocketKeepaliveStarted(int slotIndex, @NonNull KeepaliveStatus ks) {
        log("onSocketKeepaliveStarted: slot=" + slotIndex + ", keepaliveStatus=" + ks);
        switch (ks.statusCode) {
            case KeepaliveStatus.STATUS_INACTIVE:
                mNetworkAgent.sendSocketKeepaliveEvent(slotIndex,
                        keepaliveStatusErrorToPacketKeepaliveError(ks.errorCode));
                break;
            case KeepaliveStatus.STATUS_ACTIVE:
                mNetworkAgent.sendSocketKeepaliveEvent(slotIndex, SocketKeepalive.SUCCESS);
                // fall through to add record
            case KeepaliveStatus.STATUS_PENDING:
                log("Adding keepalive handle=" + ks.sessionHandle + " slotIndex = " + slotIndex);
                mKeepalives.put(ks.sessionHandle, new KeepaliveRecord(slotIndex, ks.statusCode));
                break;
            default:
                log("Invalid KeepaliveStatus Code: " + ks.statusCode);
                break;
        }
    }

    /**
     * Called when receiving keepalive status from the modem.
     *
     * @param ks Keepalive status.
     */
    private void onKeepaliveStatus(@NonNull KeepaliveStatus ks) {
        log("onKeepaliveStatus: " + ks);
        final KeepaliveRecord kr;
        kr = mKeepalives.get(ks.sessionHandle);

        if (kr == null) {
            // If there is no slot for the session handle, we received an event
            // for a different data connection. This is not an error because the
            // keepalive session events are broadcast to all listeners.
            loge("Discarding keepalive event for different data connection:" + ks);
            return;
        }
        // Switch on the current state, to see what we do with the status update
        switch (kr.currentStatus) {
            case KeepaliveStatus.STATUS_INACTIVE:
                log("Inactive Keepalive received status!");
                mNetworkAgent.sendSocketKeepaliveEvent(
                        kr.slotIndex, SocketKeepalive.ERROR_HARDWARE_ERROR);
                break;
            case KeepaliveStatus.STATUS_PENDING:
                switch (ks.statusCode) {
                    case KeepaliveStatus.STATUS_INACTIVE:
                        mNetworkAgent.sendSocketKeepaliveEvent(kr.slotIndex,
                                keepaliveStatusErrorToPacketKeepaliveError(ks.errorCode));
                        kr.currentStatus = KeepaliveStatus.STATUS_INACTIVE;
                        mKeepalives.remove(ks.sessionHandle);
                        break;
                    case KeepaliveStatus.STATUS_ACTIVE:
                        log("Pending Keepalive received active status!");
                        kr.currentStatus = KeepaliveStatus.STATUS_ACTIVE;
                        mNetworkAgent.sendSocketKeepaliveEvent(
                                kr.slotIndex, SocketKeepalive.SUCCESS);
                        break;
                    case KeepaliveStatus.STATUS_PENDING:
                        loge("Invalid unsolicited Keepalive Pending Status!");
                        break;
                    default:
                        loge("Invalid Keepalive Status received, " + ks.statusCode);
                }
                break;
            case KeepaliveStatus.STATUS_ACTIVE:
                switch (ks.statusCode) {
                    case KeepaliveStatus.STATUS_INACTIVE:
                        log("Keepalive received stopped status!");
                        mNetworkAgent.sendSocketKeepaliveEvent(kr.slotIndex,
                                SocketKeepalive.SUCCESS);

                        kr.currentStatus = KeepaliveStatus.STATUS_INACTIVE;
                        mKeepalives.remove(ks.sessionHandle);
                        break;
                    case KeepaliveStatus.STATUS_PENDING:
                    case KeepaliveStatus.STATUS_ACTIVE:
                        loge("Active Keepalive received invalid status!");
                        break;
                    default:
                        loge("Invalid Keepalive Status received, " + ks.statusCode);
                }
                break;
            default:
                loge("Invalid Keepalive Status received, " + kr.currentStatus);
        }
    }

    /**
     * Register keepalive status update from the modem. Calling this multiple times won't result in
     * multiple status update.
     */
    public void registerForKeepaliveStatus() {
        sendEmptyMessage(EVENT_REGISTER_FOR_KEEPALIVE_STATUS);
    }

    /**
     * Unregister keepalive status update from the modem.
     */
    public void unregisterForKeepaliveStatus() {
        sendEmptyMessage(EVENT_UNREGISTER_FOR_KEEPALIVE_STATUS);
    }

    /**
     * Log debug messages.
     * @param s debug messages
     */
    private void log(@NonNull String s) {
        Rlog.d(mLogTag, s);
    }

    /**
     * Log error messages.
     * @param s error messages
     */
    private void loge(@NonNull String s) {
        Rlog.e(mLogTag, s);
    }
}
