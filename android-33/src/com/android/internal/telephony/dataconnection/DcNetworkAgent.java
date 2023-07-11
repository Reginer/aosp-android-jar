/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.telephony.dataconnection;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.KeepalivePacketData;
import android.net.LinkProperties;
import android.net.NattKeepalivePacketData;
import android.net.NetworkAgent;
import android.net.NetworkAgentConfig;
import android.net.NetworkCapabilities;
import android.net.NetworkProvider;
import android.net.QosFilter;
import android.net.QosSessionAttributes;
import android.net.SocketKeepalive;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.Annotation.NetworkType;
import android.telephony.AnomalyReporter;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.data.QosBearerSession;
import android.util.LocalLog;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.SlidingWindowEventCounter;
import com.android.internal.telephony.data.KeepaliveStatus;
import com.android.internal.telephony.data.NotifyQosSessionInterface;
import com.android.internal.telephony.data.QosCallbackTracker;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This class represents a network agent which is communication channel between
 * {@link DataConnection} and {@link com.android.server.ConnectivityService}. The agent is
 * created when data connection enters {@link DataConnection.DcActiveState} until it exits that
 * state.
 *
 * Note that in IWLAN handover scenario, this agent could be transferred to the new
 * {@link DataConnection} so for a short window of time this object might be accessed by two
 * different {@link DataConnection}. Thus each method in this class needs to be synchronized.
 */
public class DcNetworkAgent extends NetworkAgent implements NotifyQosSessionInterface {
    private final String mTag;

    private final int mId;

    private final Phone mPhone;

    private final Handler mHandler;

    private int mTransportType;

    private NetworkCapabilities mNetworkCapabilities;

    public final DcKeepaliveTracker keepaliveTracker = new DcKeepaliveTracker();

    private final QosCallbackTracker mQosCallbackTracker;

    private final Executor mQosCallbackExecutor = Executors.newSingleThreadExecutor();

    private DataConnection mDataConnection;

    private final LocalLog mNetCapsLocalLog = new LocalLog(32);

    // For interface duplicate detection. Key is the net id, value is the interface name in string.
    private static Map<Integer, String> sInterfaceNames = new ConcurrentHashMap<>();

    private static final long NETWORK_UNWANTED_ANOMALY_WINDOW_MS = TimeUnit.MINUTES.toMillis(5);
    private static final int NETWORK_UNWANTED_ANOMALY_NUM_OCCURRENCES =  12;

    private static final int EVENT_UNWANTED_TIMEOUT = 1;

    @VisibleForTesting
    public DcNetworkAgent(DataConnection dc, Phone phone, int score, NetworkAgentConfig config,
            NetworkProvider networkProvider, int transportType) {
        super(phone.getContext(), dc.getHandler().getLooper(), "DcNetworkAgent",
                dc.getNetworkCapabilities(), dc.getLinkProperties(), score, config,
                networkProvider);
        register();
        mId = getNetwork().getNetId();
        mTag = "DcNetworkAgent" + "-" + mId;
        mPhone = phone;
        mHandler = new Handler(dc.getHandler().getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == EVENT_UNWANTED_TIMEOUT) {
                    loge("onNetworkUnwanted timed out. Perform silent de-register.");
                    logd("Unregister from connectivity service. " + sInterfaceNames.get(mId)
                            + " removed.");
                    sInterfaceNames.remove(mId);
                    DcNetworkAgent.this.unregister();
                }
            }
        };
        mNetworkCapabilities = dc.getNetworkCapabilities();
        mTransportType = transportType;
        mDataConnection = dc;
        if (dc.getLinkProperties() != null) {
            checkDuplicateInterface(mId, dc.getLinkProperties().getInterfaceName());
            logd("created for data connection " + dc.getName() + ", "
                    + dc.getLinkProperties().getInterfaceName());
        } else {
            loge("The connection does not have a valid link properties.");
        }
        mQosCallbackTracker = new QosCallbackTracker(this, mPhone);
    }

    private @NetworkType int getNetworkType() {
        ServiceState ss = mPhone.getServiceState();
        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;

        NetworkRegistrationInfo nri = ss.getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS, mTransportType);
        if (nri != null) {
            networkType = nri.getAccessNetworkTechnology();
        }

        return networkType;
    }

    private void checkDuplicateInterface(int netId, @Nullable String interfaceName) {
        for (Map.Entry<Integer, String> entry: sInterfaceNames.entrySet()) {
            if (Objects.equals(interfaceName, entry.getValue())) {
                String message = "Duplicate interface " + interfaceName
                        + " is detected. DcNetworkAgent-" + entry.getKey()
                        + " already used this interface name.";
                loge(message);
                // Using fixed UUID to avoid duplicate bugreport notification
                AnomalyReporter.reportAnomaly(
                        UUID.fromString("02f3d3f6-4613-4415-b6cb-8d92c8a938a6"),
                        message, mPhone.getCarrierId());
                return;
            }
        }
        sInterfaceNames.put(netId, interfaceName);
    }

    /**
     * @return The tag
     */
    String getTag() {
        return mTag;
    }

    /**
     * Set the data connection that owns this network agent.
     *
     * @param dc Data connection owning this network agent.
     * @param transportType Transport that this data connection is on.
     */
    public synchronized void acquireOwnership(@NonNull DataConnection dc,
                                              @TransportType int transportType) {
        mDataConnection = dc;
        mTransportType = transportType;
        logd(dc.getName() + " acquired the ownership of this agent.");
    }

    /**
     * Release the ownership of network agent.
     */
    public synchronized void releaseOwnership(DataConnection dc) {
        if (mDataConnection == null) {
            loge("releaseOwnership called on no-owner DcNetworkAgent!");
            return;
        } else if (mDataConnection != dc) {
            loge("releaseOwnership: This agent belongs to "
                    + mDataConnection.getName() + ", ignored the request from " + dc.getName());
            return;
        }
        logd("Data connection " + mDataConnection.getName() + " released the ownership.");
        mDataConnection = null;
    }

    /**
     * @return The data connection that owns this agent
     */
    public synchronized DataConnection getDataConnection() {
        return mDataConnection;
    }

    private static final SlidingWindowEventCounter sNetworkUnwantedCounter =
            new SlidingWindowEventCounter(NETWORK_UNWANTED_ANOMALY_WINDOW_MS,
                    NETWORK_UNWANTED_ANOMALY_NUM_OCCURRENCES);

    @Override
    public synchronized void onNetworkUnwanted() {
        mHandler.sendEmptyMessageDelayed(EVENT_UNWANTED_TIMEOUT, TimeUnit.SECONDS.toMillis(30));
        trackNetworkUnwanted();
        if (mDataConnection == null) {
            loge("onNetworkUnwanted found called on no-owner DcNetworkAgent!");
            return;
        }

        logd("onNetworkUnwanted called. Now tear down the data connection "
                + mDataConnection.getName());
        mDataConnection.tearDownAll(Phone.REASON_RELEASED_BY_CONNECTIVITY_SERVICE,
                DcTracker.RELEASE_TYPE_DETACH, null);
    }

    /**
     * There have been several bugs where a RECONNECT loop kicks off where a DataConnection
     * connects to the Network, ConnectivityService indicates that the Network is unwanted,
     * and then the DataConnection reconnects.  By the time we get the bug report it's too late
     * because there have already been hundreds of RECONNECTS.  This is meant to capture the issue
     * when it first starts.
     *
     * The unwanted counter is configured to only take an anomaly report in extreme cases.
     * This is to avoid having the anomaly message show up on several devices.
     *
     * This is directly related to b/175845538.  But, there have been several other occurrences of
     * this issue.
     */
    private void trackNetworkUnwanted() {
        if (sNetworkUnwantedCounter.addOccurrence()) {
            AnomalyReporter.reportAnomaly(
                    UUID.fromString("3f578b5c-64e9-11eb-ae93-0242ac130002"),
                    "Network Unwanted called 12 times in 5 minutes.", mPhone.getCarrierId());
        }
    }

    @Override
    public synchronized void onBandwidthUpdateRequested() {
        if (mDataConnection == null) {
            loge("onBandwidthUpdateRequested called on no-owner DcNetworkAgent!");
            return;
        }

        if (mPhone.getLceStatus() == RILConstants.LCE_ACTIVE     // active LCE service
                && mTransportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN) {
            mPhone.mCi.pullLceData(mDataConnection.obtainMessage(
                    DataConnection.EVENT_BW_REFRESH_RESPONSE));
        }
    }

    @Override
    public synchronized void onValidationStatus(int status, Uri redirectUri) {
        if (mDataConnection == null) {
            loge("onValidationStatus called on no-owner DcNetworkAgent!");
            return;
        }

        logd("validation status: " + status + " with redirection URL: " + redirectUri);
        DcTracker dct = mPhone.getDcTracker(mTransportType);
        if (dct != null) {
            Message msg = dct.obtainMessage(DctConstants.EVENT_NETWORK_STATUS_CHANGED,
                    status, mDataConnection.getCid(), redirectUri.toString());
            msg.sendToTarget();
        }
    }

    private synchronized boolean isOwned(DataConnection dc, String reason) {
        if (mDataConnection == null) {
            loge(reason + " called on no-owner DcNetworkAgent!");
            return false;
        } else if (mDataConnection != dc) {
            loge(reason + ": This agent belongs to "
                    + mDataConnection.getName() + ", ignored the request from " + dc.getName());
            return false;
        }
        return true;
    }

    /**
     * Update the legacy sub type (i.e. data network type).
     *
     * @param dc The data connection that invokes this method.
     */
    public synchronized void updateLegacySubtype(DataConnection dc) {
        if (!isOwned(dc, "updateLegacySubtype")) return;

        int networkType = getNetworkType();
        setLegacySubtype(networkType, TelephonyManager.getNetworkTypeName(networkType));
    }

    /**
     * Set the network capabilities.
     *
     * @param networkCapabilities The network capabilities.
     * @param dc The data connection that invokes this method.
     */
    public synchronized void sendNetworkCapabilities(NetworkCapabilities networkCapabilities,
                                                     DataConnection dc) {
        if (!isOwned(dc, "sendNetworkCapabilities")) return;

        if (!networkCapabilities.equals(mNetworkCapabilities)) {
            String logStr = "Changed from " + mNetworkCapabilities + " to "
                    + networkCapabilities + ", Data RAT="
                    + mPhone.getServiceState().getRilDataRadioTechnology()
                    + ", dc=" + mDataConnection.getName();
            logd(logStr);
            mNetCapsLocalLog.log(logStr);
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                // only log metrics for DataConnection with NET_CAPABILITY_INTERNET
                if (mNetworkCapabilities == null
                        || networkCapabilities.hasCapability(
                                NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)
                        != mNetworkCapabilities.hasCapability(
                                NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED)) {
                    TelephonyMetrics.getInstance().writeNetworkCapabilitiesChangedEvent(
                            mPhone.getPhoneId(), networkCapabilities);
                }
            }
            mNetworkCapabilities = networkCapabilities;
        }
        sendNetworkCapabilities(networkCapabilities);
    }

    /**
     * Set the link properties
     *
     * @param linkProperties The link properties
     * @param dc The data connection that invokes this method.
     */
    public synchronized void sendLinkProperties(@NonNull LinkProperties linkProperties,
                                                DataConnection dc) {
        if (!isOwned(dc, "sendLinkProperties")) return;

        sInterfaceNames.put(mId, dc.getLinkProperties().getInterfaceName());
        sendLinkProperties(linkProperties);
    }

    /**
     * Set the network score.
     *
     * @param score The network score.
     * @param dc The data connection that invokes this method.
     */
    public synchronized void sendNetworkScore(int score, DataConnection dc) {
        if (!isOwned(dc, "sendNetworkScore")) return;
        sendNetworkScore(score);
    }

    /**
     * Unregister the network agent from connectivity service.
     *
     * @param dc The data connection that invokes this method.
     */
    public synchronized void unregister(DataConnection dc) {
        if (!isOwned(dc, "unregister")) return;

        mHandler.removeMessages(EVENT_UNWANTED_TIMEOUT);
        logd("Unregister from connectivity service. " + sInterfaceNames.get(mId) + " removed.");
        sInterfaceNames.remove(mId);
        super.unregister();
    }

    @Override
    public synchronized void onStartSocketKeepalive(int slot, @NonNull Duration interval,
            @NonNull KeepalivePacketData packet) {
        if (mDataConnection == null) {
            loge("onStartSocketKeepalive called on no-owner DcNetworkAgent!");
            return;
        }

        if (packet instanceof NattKeepalivePacketData) {
            mDataConnection.obtainMessage(DataConnection.EVENT_KEEPALIVE_START_REQUEST,
                    slot, (int) interval.getSeconds(), packet).sendToTarget();
        } else {
            sendSocketKeepaliveEvent(slot, SocketKeepalive.ERROR_UNSUPPORTED);
        }
    }

    @Override
    public synchronized void onStopSocketKeepalive(int slot) {
        if (mDataConnection == null) {
            loge("onStopSocketKeepalive called on no-owner DcNetworkAgent!");
            return;
        }

        mDataConnection.obtainMessage(DataConnection.EVENT_KEEPALIVE_STOP_REQUEST, slot)
                .sendToTarget();
    }

    @Override
    public void onQosCallbackRegistered(final int qosCallbackId, final @NonNull QosFilter filter) {
        mQosCallbackExecutor.execute(() -> mQosCallbackTracker.addFilter(qosCallbackId,
              new QosCallbackTracker.IFilter() {
                  @Override
                  public boolean matchesLocalAddress(
                          InetAddress address, int startPort, int endPort) {
                      return filter.matchesLocalAddress(address, startPort, endPort);
                  }

                  @Override
                  public boolean matchesRemoteAddress(
                          InetAddress address, int startPort, int endPort) {
                      return filter.matchesRemoteAddress(address, startPort, endPort);
                  }
              }));
    }

    @Override
    public void onQosCallbackUnregistered(final int qosCallbackId) {
        mQosCallbackExecutor.execute(() -> mQosCallbackTracker.removeFilter(qosCallbackId));
    }

    void updateQosBearerSessions(final List<QosBearerSession> qosBearerSessions) {
        mQosCallbackExecutor.execute(() -> mQosCallbackTracker.updateSessions(qosBearerSessions));
    }

    @Override
    public void notifyQosSessionAvailable(final int qosCallbackId, final int sessionId,
            @NonNull final QosSessionAttributes attributes) {
        super.sendQosSessionAvailable(qosCallbackId, sessionId, attributes);
    }

    @Override
    public void notifyQosSessionLost(final int qosCallbackId,
            final int sessionId, final int qosSessionType) {
        super.sendQosSessionLost(qosCallbackId, sessionId, qosSessionType);
    }

    @Override
    public String toString() {
        return "DcNetworkAgent-"
                + mId
                + " mDataConnection="
                + ((mDataConnection != null) ? mDataConnection.getName() : null)
                + " mTransportType="
                + AccessNetworkConstants.transportTypeToString(mTransportType)
                + " " + ((mDataConnection != null) ? mDataConnection.getLinkProperties() : null)
                + " mNetworkCapabilities=" + mNetworkCapabilities;
    }

    /**
     * Dump the state of transport manager
     *
     * @param fd File descriptor
     * @param printWriter Print writer
     * @param args Arguments
     */
    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        pw.println(toString());
        pw.increaseIndent();
        pw.println("Net caps logs:");
        mNetCapsLocalLog.dump(fd, pw, args);
        pw.decreaseIndent();
    }

    /**
     * Log with debug level
     *
     * @param s is string log
     */
    private void logd(String s) {
        Rlog.d(mTag, s);
    }

    /**
     * Log with error level
     *
     * @param s is string log
     */
    private void loge(String s) {
        Rlog.e(mTag, s);
    }

    class DcKeepaliveTracker {
        private class KeepaliveRecord {
            public int slotId;
            public int currentStatus;

            KeepaliveRecord(int slotId, int status) {
                this.slotId = slotId;
                this.currentStatus = status;
            }
        }

        private final SparseArray<KeepaliveRecord> mKeepalives = new SparseArray();

        int getHandleForSlot(int slotId) {
            for (int i = 0; i < mKeepalives.size(); i++) {
                KeepaliveRecord kr = mKeepalives.valueAt(i);
                if (kr.slotId == slotId) return mKeepalives.keyAt(i);
            }
            return -1;
        }

        int keepaliveStatusErrorToPacketKeepaliveError(int error) {
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

        void handleKeepaliveStarted(final int slot, KeepaliveStatus ks) {
            switch (ks.statusCode) {
                case KeepaliveStatus.STATUS_INACTIVE:
                    DcNetworkAgent.this.sendSocketKeepaliveEvent(slot,
                            keepaliveStatusErrorToPacketKeepaliveError(ks.errorCode));
                    break;
                case KeepaliveStatus.STATUS_ACTIVE:
                    DcNetworkAgent.this.sendSocketKeepaliveEvent(
                            slot, SocketKeepalive.SUCCESS);
                    // fall through to add record
                case KeepaliveStatus.STATUS_PENDING:
                    logd("Adding keepalive handle="
                            + ks.sessionHandle + " slot = " + slot);
                    mKeepalives.put(ks.sessionHandle,
                            new KeepaliveRecord(
                                    slot, ks.statusCode));
                    break;
                default:
                    logd("Invalid KeepaliveStatus Code: " + ks.statusCode);
                    break;
            }
        }

        void handleKeepaliveStatus(KeepaliveStatus ks) {
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
                    logd("Inactive Keepalive received status!");
                    DcNetworkAgent.this.sendSocketKeepaliveEvent(
                            kr.slotId, SocketKeepalive.ERROR_HARDWARE_ERROR);
                    break;
                case KeepaliveStatus.STATUS_PENDING:
                    switch (ks.statusCode) {
                        case KeepaliveStatus.STATUS_INACTIVE:
                            DcNetworkAgent.this.sendSocketKeepaliveEvent(kr.slotId,
                                    keepaliveStatusErrorToPacketKeepaliveError(ks.errorCode));
                            kr.currentStatus = KeepaliveStatus.STATUS_INACTIVE;
                            mKeepalives.remove(ks.sessionHandle);
                            break;
                        case KeepaliveStatus.STATUS_ACTIVE:
                            logd("Pending Keepalive received active status!");
                            kr.currentStatus = KeepaliveStatus.STATUS_ACTIVE;
                            DcNetworkAgent.this.sendSocketKeepaliveEvent(
                                    kr.slotId, SocketKeepalive.SUCCESS);
                            break;
                        case KeepaliveStatus.STATUS_PENDING:
                            loge("Invalid unsolicied Keepalive Pending Status!");
                            break;
                        default:
                            loge("Invalid Keepalive Status received, " + ks.statusCode);
                    }
                    break;
                case KeepaliveStatus.STATUS_ACTIVE:
                    switch (ks.statusCode) {
                        case KeepaliveStatus.STATUS_INACTIVE:
                            logd("Keepalive received stopped status!");
                            DcNetworkAgent.this.sendSocketKeepaliveEvent(
                                    kr.slotId, SocketKeepalive.SUCCESS);

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
    }
}
