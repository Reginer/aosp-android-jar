/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.annotation.Nullable;
import android.net.LinkAddress;
import android.net.NetworkAgent;
import android.net.QosFilter;
import android.net.QosSession;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.telephony.data.EpsBearerQosSessionAttributes;
import android.telephony.data.EpsQos;
import android.telephony.data.NrQos;
import android.telephony.data.NrQosSessionAttributes;
import android.telephony.data.QosBearerFilter;
import android.telephony.data.QosBearerSession;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.metrics.RcsStats;
import com.android.telephony.Rlog;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Matches filters with qos sessions and send corresponding available and lost events.
 */
public class QosCallbackTracker extends Handler {
    private static final int DEDICATED_BEARER_EVENT_STATE_NONE = 0;
    private static final int DEDICATED_BEARER_EVENT_STATE_ADDED = 1;
    private static final int DEDICATED_BEARER_EVENT_STATE_MODIFIED = 2;
    private static final int DEDICATED_BEARER_EVENT_STATE_DELETED = 3;

    @NonNull
    private final String mLogTag;
    @NonNull
    private final TelephonyNetworkAgent mNetworkAgent;
    @NonNull
    private final Map<Integer, QosBearerSession> mQosBearerSessions;
    @NonNull
    private final RcsStats mRcsStats;

    // We perform an exact match on the address
    @NonNull
    private final Map<Integer, IFilter> mCallbacksToFilter;

    private final int mPhoneId;

    /**
     * QOS sessions filter interface
     */
    public interface IFilter {
        /**
         * Filter using the local address.
         *
         * @param address The local address.
         * @param startPort Starting port.
         * @param endPort Ending port.
         * @return {@code true} if matches, {@code false} otherwise.
         */
        boolean matchesLocalAddress(InetAddress address, int startPort, int endPort);

        /**
         * Filter using the remote address.
         *
         * @param address The remote address.
         * @param startPort Starting port.
         * @param endPort Ending port.
         * @return {@code true} if matches, {@code false} otherwise.
         */
        boolean matchesRemoteAddress(InetAddress address, int startPort, int endPort);

        /**
         * Filter using the protocol
         *
         * @param protocol ID
         * @return {@code true} if matches, {@code false} otherwise.
         */
        boolean matchesProtocol(int protocol);
    }

    /**
     * Constructor
     *
     * @param networkAgent The network agent to send events to.
     * @param phone The phone instance.
     */
    public QosCallbackTracker(@NonNull TelephonyNetworkAgent networkAgent, @NonNull Phone phone) {
        mQosBearerSessions = new HashMap<>();
        mCallbacksToFilter = new HashMap<>();
        mNetworkAgent = networkAgent;
        mPhoneId = phone.getPhoneId();
        mRcsStats = RcsStats.getInstance();
        mLogTag = "QOSCT" + "-" + ((NetworkAgent) mNetworkAgent).getNetwork().getNetId();

        networkAgent.registerCallback(
                new TelephonyNetworkAgent.TelephonyNetworkAgentCallback(this::post) {
                    @Override
                    public void onQosCallbackRegistered(int qosCallbackId,
                            @NonNull QosFilter filter) {
                        addFilter(qosCallbackId,
                                new QosCallbackTracker.IFilter() {
                                    @Override
                                    public boolean matchesLocalAddress(
                                            @NonNull InetAddress address, int startPort,
                                            int endPort) {
                                        return filter.matchesLocalAddress(address, startPort,
                                                endPort);
                                    }

                                    @Override
                                    public boolean matchesRemoteAddress(
                                            @NonNull InetAddress address, int startPort,
                                            int endPort) {
                                        return filter.matchesRemoteAddress(address, startPort,
                                                endPort);
                                    }

                                    @Override
                                    public boolean matchesProtocol(int protocol) {
                                        return filter.matchesProtocol(protocol);
                                    }
                                });
                    }
                });
    }

    /**
     * Add new filter that is to receive events.
     *
     * @param callbackId the associated callback id.
     * @param filter provides the matching logic.
     */
    public void addFilter(final int callbackId, final IFilter filter) {
        post(() -> {
            log("addFilter: callbackId=" + callbackId);
            // Called from mDcNetworkAgent
            mCallbacksToFilter.put(callbackId, filter);

            //On first change. Check all sessions and send.
            for (final QosBearerSession session : mQosBearerSessions.values()) {
                if (doFiltersMatch(session, filter)) {
                    sendSessionAvailable(callbackId, session, filter);

                    notifyMetricDedicatedBearerListenerAdded(callbackId, session);
                }
            }
        });
    }

    /**
     * Remove the filter with the associated callback id.
     *
     * @param callbackId the qos callback id.
     */
    public void removeFilter(final int callbackId) {
        post(() -> {
            log("removeFilter: callbackId=" + callbackId);
            mCallbacksToFilter.remove(callbackId);
            notifyMetricDedicatedBearerListenerRemoved(callbackId);
        });
    }

    /**
     * Update the list of qos sessions and send out corresponding events
     *
     * @param sessions the new list of qos sessions
     */
    public void updateSessions(@NonNull final List<QosBearerSession> sessions) {
        post(() -> {
            log("updateSessions: sessions size=" + sessions.size());

            int bearerState = DEDICATED_BEARER_EVENT_STATE_NONE;

            final List<QosBearerSession> sessionsToAdd = new ArrayList<>();
            final Map<Integer, QosBearerSession> incomingSessions = new HashMap<>();
            final HashSet<Integer> sessionsReportedToMetric = new HashSet<>();
            for (final QosBearerSession incomingSession : sessions) {
                int sessionId = incomingSession.getQosBearerSessionId();
                incomingSessions.put(sessionId, incomingSession);

                final QosBearerSession existingSession = mQosBearerSessions.get(sessionId);
                for (final int callbackId : mCallbacksToFilter.keySet()) {
                    final IFilter filter = mCallbacksToFilter.get(callbackId);

                    final boolean incomingSessionMatch = doFiltersMatch(incomingSession, filter);
                    final boolean existingSessionMatch =
                            existingSession != null && doFiltersMatch(existingSession, filter);

                    if (!existingSessionMatch && incomingSessionMatch) {
                        // The filter matches now and didn't match earlier
                        sendSessionAvailable(callbackId, incomingSession, filter);

                        bearerState = DEDICATED_BEARER_EVENT_STATE_ADDED;
                    }

                    if (existingSessionMatch && incomingSessionMatch) {
                        // The same sessions matches the same filter, but if the qos changed,
                        // the callback still needs to be notified
                        if (!incomingSession.getQos().equals(existingSession.getQos())) {
                            sendSessionAvailable(callbackId, incomingSession, filter);
                            bearerState = DEDICATED_BEARER_EVENT_STATE_MODIFIED;
                        }
                    }

                    // this QosBearerSession has registered QosCallbackId
                    if (!sessionsReportedToMetric.contains(sessionId) && incomingSessionMatch) {
                        // this session has listener
                        notifyMetricDedicatedBearerEvent(incomingSession, bearerState, true);
                        sessionsReportedToMetric.add(sessionId);
                    }
                }

                // this QosBearerSession does not have registered QosCallbackId
                if (!sessionsReportedToMetric.contains(sessionId)) {
                    // no listener is registered to this session
                    bearerState = DEDICATED_BEARER_EVENT_STATE_ADDED;
                    notifyMetricDedicatedBearerEvent(incomingSession, bearerState, false);
                    sessionsReportedToMetric.add(sessionId);
                }
                sessionsToAdd.add(incomingSession);
            }

            final List<Integer> sessionsToRemove = new ArrayList<>();
            sessionsReportedToMetric.clear();
            bearerState = DEDICATED_BEARER_EVENT_STATE_DELETED;
            // Find sessions that no longer exist
            for (final QosBearerSession existingSession : mQosBearerSessions.values()) {
                final int sessionId = existingSession.getQosBearerSessionId();
                if (!incomingSessions.containsKey(sessionId)) {
                    for (final int callbackId : mCallbacksToFilter.keySet()) {
                        final IFilter filter = mCallbacksToFilter.get(callbackId);
                        // The filter matches which means it was previously available, and now is
                        // lost
                        if (doFiltersMatch(existingSession, filter)) {
                            sendSessionLost(callbackId, existingSession);
                            notifyMetricDedicatedBearerEvent(existingSession, bearerState, true);
                            sessionsReportedToMetric.add(sessionId);
                        }
                    }
                    sessionsToRemove.add(sessionId);
                    if (!sessionsReportedToMetric.contains(sessionId)) {
                        notifyMetricDedicatedBearerEvent(existingSession, bearerState, false);
                        sessionsReportedToMetric.add(sessionId);
                    }
                }
            }

            // Add in the new or existing sessions with updated information
            for (final QosBearerSession sessionToAdd : sessionsToAdd) {
                mQosBearerSessions.put(sessionToAdd.getQosBearerSessionId(), sessionToAdd);
            }

            // Remove any old sessions
            for (final int sessionToRemove : sessionsToRemove) {
                mQosBearerSessions.remove(sessionToRemove);
            }
        });
    }

    private boolean doFiltersMatch(@NonNull final QosBearerSession qosBearerSession,
                                   @NonNull final IFilter filter) {
        return getMatchingQosBearerFilter(qosBearerSession, filter) != null;
    }

    private boolean matchesByLocalAddress(@NonNull final QosBearerFilter sessionFilter,
                                          @NonNull final IFilter filter) {
        int portStart;
        int portEnd;
        if (sessionFilter.getLocalPortRange() == null) {
            portStart = QosBearerFilter.QOS_MIN_PORT;
            portEnd = QosBearerFilter.QOS_MAX_PORT;
        } else if (sessionFilter.getLocalPortRange().isValid()) {
            portStart = sessionFilter.getLocalPortRange().getStart();
            portEnd = sessionFilter.getLocalPortRange().getEnd();
        } else {
            return false;
        }
        if (sessionFilter.getLocalAddresses().isEmpty()) {
            InetAddress anyAddress;
            try {
                anyAddress = InetAddress.getByAddress(new byte[] {0, 0, 0, 0});
            } catch (UnknownHostException e) {
                return false;
            }
            return filter.matchesLocalAddress(anyAddress, portStart, portEnd);
        } else {
            for (final LinkAddress qosAddress : sessionFilter.getLocalAddresses()) {
                return filter.matchesLocalAddress(qosAddress.getAddress(), portStart, portEnd);
            }
        }
        return false;
    }

    private boolean matchesByRemoteAddress(@NonNull QosBearerFilter sessionFilter,
                                           @NonNull final IFilter filter) {
        int portStart;
        int portEnd;
        boolean result = false;
        if (sessionFilter.getRemotePortRange() == null) {
            portStart = QosBearerFilter.QOS_MIN_PORT;
            portEnd = QosBearerFilter.QOS_MAX_PORT;
        } else if (sessionFilter.getRemotePortRange().isValid()) {
            portStart = sessionFilter.getRemotePortRange().getStart();
            portEnd = sessionFilter.getRemotePortRange().getEnd();
        } else {
            return false;
        }
        if (sessionFilter.getRemoteAddresses().isEmpty()) {
            InetAddress anyAddress;
            try {
                anyAddress = InetAddress.getByAddress(new byte[] {0, 0, 0, 0});
            } catch (UnknownHostException e) {
                return false;
            }
            result = filter.matchesRemoteAddress(anyAddress, portStart, portEnd);
        } else {
            for (final LinkAddress qosAddress : sessionFilter.getRemoteAddresses()) {
                result = filter.matchesRemoteAddress(qosAddress.getAddress(), portStart, portEnd);
            }
        }
        return result;
    }

    private boolean matchesByProtocol(@NonNull QosBearerFilter sessionFilter,
                                      @NonNull final IFilter filter, boolean hasMatchedFilter) {
        boolean result;
        int protocol = sessionFilter.getProtocol();
        if (protocol == QosBearerFilter.QOS_PROTOCOL_TCP
                || protocol == QosBearerFilter.QOS_PROTOCOL_UDP) {
            result = filter.matchesProtocol(protocol);
        } else {
            // FWK currently doesn't support filtering based on protocol ID ESP & AH. We will follow
            // match results of other filters.
            result = hasMatchedFilter;
        }
        return result;
    }

    private QosBearerFilter getFilterByPrecedence(
            @Nullable QosBearerFilter qosFilter, QosBearerFilter sessionFilter) {
        // Find for the highest precedence filter, lower the value is the higher the precedence
        return qosFilter == null || sessionFilter.getPrecedence() < qosFilter.getPrecedence()
                ? sessionFilter : qosFilter;
    }

    @Nullable
    private QosBearerFilter getMatchingQosBearerFilter(
            @NonNull QosBearerSession qosBearerSession, @NonNull final IFilter filter) {
        QosBearerFilter qosFilter = null;

        for (final QosBearerFilter sessionFilter : qosBearerSession.getQosBearerFilterList()) {
            boolean unMatched = false;
            boolean hasMatchedFilter = false;
            if (!sessionFilter.getLocalAddresses().isEmpty()
                    || sessionFilter.getLocalPortRange() != null) {
                if (!matchesByLocalAddress(sessionFilter, filter)) {
                    unMatched = true;
                } else {
                    hasMatchedFilter = true;
                }
            }
            if (!sessionFilter.getRemoteAddresses().isEmpty()
                    || sessionFilter.getRemotePortRange() != null) {
                if (!matchesByRemoteAddress(sessionFilter, filter)) {
                    unMatched = true;
                } else {
                    hasMatchedFilter = true;
                }
            }

            if (sessionFilter.getProtocol() != QosBearerFilter.QOS_PROTOCOL_UNSPECIFIED) {
                if (!matchesByProtocol(sessionFilter, filter, hasMatchedFilter)) {
                    unMatched = true;
                } else {
                    hasMatchedFilter = true;
                }
            }

            if (!unMatched && hasMatchedFilter) {
                qosFilter = getFilterByPrecedence(qosFilter, sessionFilter);
            }
        }
        return qosFilter;
    }

    private void sendSessionAvailable(final int callbackId, @NonNull final QosBearerSession session,
                                      @NonNull IFilter filter) {
        QosBearerFilter qosBearerFilter = getMatchingQosBearerFilter(session, filter);
        List<InetSocketAddress> remoteAddresses = new ArrayList<>();
        if (qosBearerFilter != null && !qosBearerFilter.getRemoteAddresses().isEmpty()
                && qosBearerFilter.getRemotePortRange() != null) {
            remoteAddresses.add(
                    new InetSocketAddress(qosBearerFilter.getRemoteAddresses().get(0).getAddress(),
                            qosBearerFilter.getRemotePortRange().getStart()));
        }

        if (session.getQos() instanceof EpsQos) {
            EpsQos qos = (EpsQos) session.getQos();
            EpsBearerQosSessionAttributes epsBearerAttr =
                    new EpsBearerQosSessionAttributes(qos.getQci(),
                            qos.getUplinkBandwidth().getMaxBitrateKbps(),
                            qos.getDownlinkBandwidth().getMaxBitrateKbps(),
                            qos.getDownlinkBandwidth().getGuaranteedBitrateKbps(),
                            qos.getUplinkBandwidth().getGuaranteedBitrateKbps(),
                            remoteAddresses);
            mNetworkAgent.sendQosSessionAvailable(
                    callbackId, session.getQosBearerSessionId(), epsBearerAttr);
        } else {
            NrQos qos = (NrQos) session.getQos();
            NrQosSessionAttributes nrQosAttr =
                    new NrQosSessionAttributes(qos.get5Qi(), qos.getQfi(),
                            qos.getUplinkBandwidth().getMaxBitrateKbps(),
                            qos.getDownlinkBandwidth().getMaxBitrateKbps(),
                            qos.getDownlinkBandwidth().getGuaranteedBitrateKbps(),
                            qos.getUplinkBandwidth().getGuaranteedBitrateKbps(),
                            qos.getAveragingWindow(), remoteAddresses);
            mNetworkAgent.sendQosSessionAvailable(
                    callbackId, session.getQosBearerSessionId(), nrQosAttr);
        }

        // added to notify to Metric for passing DedicatedBearerEstablished info
        notifyMetricDedicatedBearerListenerBearerUpdateSession(callbackId, session);

        log("sendSessionAvailable, callbackId=" + callbackId);
    }

    private void sendSessionLost(int callbackId, @NonNull QosBearerSession session) {
        mNetworkAgent.sendQosSessionLost(callbackId, session.getQosBearerSessionId(),
                session.getQos() instanceof EpsQos
                        ? QosSession.TYPE_EPS_BEARER : QosSession.TYPE_NR_BEARER);
        log("sendSessionLost, callbackId=" + callbackId);
    }

    private void notifyMetricDedicatedBearerListenerAdded(final int callbackId,
                                                          @NonNull final QosBearerSession session) {

        final int rat = getRatInfoFromSessionInfo(session);
        final int qci = getQCIFromSessionInfo(session);

        mRcsStats.onImsDedicatedBearerListenerAdded(callbackId, mPhoneId, rat, qci);
    }

    private void notifyMetricDedicatedBearerListenerBearerUpdateSession(
            final int callbackId, @NonNull final QosBearerSession session) {
        mRcsStats.onImsDedicatedBearerListenerUpdateSession(callbackId, mPhoneId,
                getRatInfoFromSessionInfo(session), getQCIFromSessionInfo(session), true);
    }

    private void notifyMetricDedicatedBearerListenerRemoved(final int callbackId) {
        mRcsStats.onImsDedicatedBearerListenerRemoved(callbackId);
    }

    private int getQCIFromSessionInfo(final QosBearerSession session) {
        if (session.getQos() instanceof EpsQos) {
            return ((EpsQos) session.getQos()).getQci();
        } else if (session.getQos() instanceof NrQos) {
            return ((NrQos) session.getQos()).get5Qi();
        }

        return 0;
    }

    private int getRatInfoFromSessionInfo(final QosBearerSession session) {
        if (session.getQos() instanceof EpsQos) {
            return TelephonyManager.NETWORK_TYPE_LTE;
        } else if (session.getQos() instanceof NrQos) {
            return TelephonyManager.NETWORK_TYPE_NR;
        }

        return 0;
    }

    private boolean doesLocalConnectionInfoExist(final QosBearerSession qosBearerSession) {
        for (final QosBearerFilter sessionFilter : qosBearerSession.getQosBearerFilterList()) {
            if (!sessionFilter.getLocalAddresses().isEmpty()
                    && sessionFilter.getLocalPortRange() != null
                    && sessionFilter.getLocalPortRange().isValid()) {
                return true;
            }
        }
        return false;
    }

    private boolean doesRemoteConnectionInfoExist(final QosBearerSession qosBearerSession) {
        for (final QosBearerFilter sessionFilter : qosBearerSession.getQosBearerFilterList()) {
            if (!sessionFilter.getRemoteAddresses().isEmpty()
                    && sessionFilter.getRemotePortRange() != null
                    && sessionFilter.getRemotePortRange().isValid()) {
                return true;
            }
        }
        return false;
    }

    private void notifyMetricDedicatedBearerEvent(final QosBearerSession session,
            final int bearerState, final boolean hasListener) {
        int ratAtEnd = getRatInfoFromSessionInfo(session);
        int qci = getQCIFromSessionInfo(session);
        boolean localConnectionInfoReceived = doesLocalConnectionInfoExist(session);
        boolean remoteConnectionInfoReceived = doesRemoteConnectionInfoExist(session);

        mRcsStats.onImsDedicatedBearerEvent(mPhoneId, ratAtEnd, qci, bearerState,
                localConnectionInfoReceived, remoteConnectionInfoReceived, hasListener);
    }

    /**
     * Log debug messages.
     * @param s debug messages
     */
    private void log(@NonNull String s) {
        Rlog.d(mLogTag, s);
    }
}
