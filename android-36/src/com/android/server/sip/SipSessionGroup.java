/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.server.sip;

import gov.nist.javax.sip.clientauthutils.AccountManager;
import gov.nist.javax.sip.clientauthutils.UserCredentials;
import gov.nist.javax.sip.header.ProxyAuthenticate;
import gov.nist.javax.sip.header.ReferTo;
import gov.nist.javax.sip.header.SIPHeaderNames;
import gov.nist.javax.sip.header.StatusLine;
import gov.nist.javax.sip.header.WWWAuthenticate;
import gov.nist.javax.sip.header.extensions.ReferredByHeader;
import gov.nist.javax.sip.header.extensions.ReplacesHeader;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPResponse;

import android.net.sip.ISipSession;
import android.net.sip.ISipSessionListener;
import android.net.sip.SipErrorCode;
import android.net.sip.SipProfile;
import android.net.sip.SipSession;
import android.net.sip.SipSessionAdapter;
import android.text.TextUtils;
import android.telephony.Rlog;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ObjectInUseException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.Transaction;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.MinExpiresHeader;
import javax.sip.header.ReferToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;


/**
 * Manages {@link ISipSession}'s for a SIP account.
 */
class SipSessionGroup implements SipListener {
    private static final String TAG = "SipSession";
    private static final boolean DBG = false;
    private static final boolean DBG_PING = false;
    private static final String ANONYMOUS = "anonymous";
    // Limit the size of thread pool to 1 for the order issue when the phone is
    // waken up from sleep and there are many packets to be processed in the SIP
    // stack. Note: The default thread pool size in NIST SIP stack is -1 which is
    // unlimited.
    private static final String THREAD_POOL_SIZE = "1";
    private static final int EXPIRY_TIME = 3600; // in seconds
    private static final int CANCEL_CALL_TIMER = 3; // in seconds
    private static final int END_CALL_TIMER = 3; // in seconds
    private static final int KEEPALIVE_TIMEOUT = 5; // in seconds
    private static final int INCALL_KEEPALIVE_INTERVAL = 10; // in seconds
    private static final long WAKE_LOCK_HOLDING_TIME = 500; // in milliseconds

    private static final EventObject DEREGISTER = new EventObject("Deregister");
    private static final EventObject END_CALL = new EventObject("End call");

    private final SipProfile mLocalProfile;
    private final String mPassword;

    private SipStack mSipStack;
    private SipHelper mSipHelper;

    // session that processes INVITE requests
    private SipSessionImpl mCallReceiverSession;
    private String mLocalIp;

    private SipWakeupTimer mWakeupTimer;
    private SipWakeLock mWakeLock;

    // call-id-to-SipSession map
    private Map<String, SipSessionImpl> mSessionMap =
            new HashMap<String, SipSessionImpl>();

    // external address observed from any response
    private String mExternalIp;
    private int mExternalPort;

    /**
     * @param profile the local profile with password crossed out
     * @param password the password of the profile
     * @throws SipException if cannot assign requested address
     */
    public SipSessionGroup(SipProfile profile, String password,
            SipWakeupTimer timer, SipWakeLock wakeLock) throws SipException {
        mLocalProfile = profile;
        mPassword = password;
        mWakeupTimer = timer;
        mWakeLock = wakeLock;
        reset();
    }

    // TODO: remove this method once SipWakeupTimer can better handle variety
    // of timeout values
    void setWakeupTimer(SipWakeupTimer timer) {
        mWakeupTimer = timer;
    }

    synchronized void reset() throws SipException {
        Properties properties = new Properties();

        String protocol = mLocalProfile.getProtocol();
        int port = mLocalProfile.getPort();
        String server = mLocalProfile.getProxyAddress();

        if (!TextUtils.isEmpty(server)) {
            properties.setProperty("javax.sip.OUTBOUND_PROXY",
                    server + ':' + port + '/' + protocol);
        } else {
            server = mLocalProfile.getSipDomain();
        }
        if (server.startsWith("[") && server.endsWith("]")) {
            server = server.substring(1, server.length() - 1);
        }

        String local = null;
        try {
            for (InetAddress remote : InetAddress.getAllByName(server)) {
                DatagramSocket socket = new DatagramSocket();
                socket.connect(remote, port);
                if (socket.isConnected()) {
                    local = socket.getLocalAddress().getHostAddress();
                    port = socket.getLocalPort();
                    socket.close();
                    break;
                }
                socket.close();
            }
        } catch (Exception e) {
            // ignore.
        }
        if (local == null) {
            // We are unable to reach the server. Just bail out.
            return;
        }

        close();
        mLocalIp = local;

        properties.setProperty("javax.sip.STACK_NAME", getStackName());
        properties.setProperty(
                "gov.nist.javax.sip.THREAD_POOL_SIZE", THREAD_POOL_SIZE);
        mSipStack = SipFactory.getInstance().createSipStack(properties);
        try {
            SipProvider provider = mSipStack.createSipProvider(
                    mSipStack.createListeningPoint(local, port, protocol));
            provider.addSipListener(this);
            mSipHelper = new SipHelper(mSipStack, provider);
        } catch (SipException e) {
            throw e;
        } catch (Exception e) {
            throw new SipException("failed to initialize SIP stack", e);
        }

        if (DBG) log("reset: start stack for " + mLocalProfile.getUriString());
        mSipStack.start();
    }

    synchronized void onConnectivityChanged() {
        SipSessionImpl[] ss = mSessionMap.values().toArray(
                    new SipSessionImpl[mSessionMap.size()]);
        // Iterate on the copied array instead of directly on mSessionMap to
        // avoid ConcurrentModificationException being thrown when
        // SipSessionImpl removes itself from mSessionMap in onError() in the
        // following loop.
        for (SipSessionImpl s : ss) {
            s.onError(SipErrorCode.DATA_CONNECTION_LOST,
                    "data connection lost");
        }
    }

    synchronized void resetExternalAddress() {
        if (DBG) {
            log("resetExternalAddress: " + mSipStack);
        }
        mExternalIp = null;
        mExternalPort = 0;
    }

    public SipProfile getLocalProfile() {
        return mLocalProfile;
    }

    public String getLocalProfileUri() {
        return mLocalProfile.getUriString();
    }

    private String getStackName() {
        return "stack" + System.currentTimeMillis();
    }

    public synchronized void close() {
        if (DBG) log("close: " + SipService.obfuscateSipUri(mLocalProfile.getUriString()));
        onConnectivityChanged();
        mSessionMap.clear();
        closeToNotReceiveCalls();
        if (mSipStack != null) {
            mSipStack.stop();
            mSipStack = null;
            mSipHelper = null;
        }
        resetExternalAddress();
    }

    public synchronized boolean isClosed() {
        return (mSipStack == null);
    }

    // For internal use, require listener not to block in callbacks.
    public synchronized void openToReceiveCalls(ISipSessionListener listener) {
        if (mCallReceiverSession == null) {
            mCallReceiverSession = new SipSessionCallReceiverImpl(listener);
        } else {
            mCallReceiverSession.setListener(listener);
        }
    }

    public synchronized void closeToNotReceiveCalls() {
        mCallReceiverSession = null;
    }

    public ISipSession createSession(ISipSessionListener listener) {
        return (isClosed() ? null : new SipSessionImpl(listener));
    }

    synchronized boolean containsSession(String callId) {
        return mSessionMap.containsKey(callId);
    }

    private synchronized SipSessionImpl getSipSession(EventObject event) {
        String key = SipHelper.getCallId(event);
        SipSessionImpl session = mSessionMap.get(key);
        if ((session != null) && isLoggable(session)) {
            if (DBG) log("getSipSession: event=" + key);
            if (DBG) log("getSipSession: active sessions:");
            for (String k : mSessionMap.keySet()) {
                if (DBG) log("getSipSession: ..." + k + ": " + mSessionMap.get(k));
            }
        }
        return ((session != null) ? session : mCallReceiverSession);
    }

    private synchronized void addSipSession(SipSessionImpl newSession) {
        removeSipSession(newSession);
        String key = newSession.getCallId();
        mSessionMap.put(key, newSession);
        if (isLoggable(newSession)) {
            if (DBG) log("addSipSession: key='" + key + "'");
            for (String k : mSessionMap.keySet()) {
                if (DBG) log("addSipSession:  " + k + ": " + mSessionMap.get(k));
            }
        }
    }

    private synchronized void removeSipSession(SipSessionImpl session) {
        if (session == mCallReceiverSession) return;
        String key = session.getCallId();
        SipSessionImpl s = mSessionMap.remove(key);
        // sanity check
        if ((s != null) && (s != session)) {
            if (DBG) log("removeSession: " + session + " is not associated with key '"
                    + key + "'");
            mSessionMap.put(key, s);
            for (Map.Entry<String, SipSessionImpl> entry
                    : mSessionMap.entrySet()) {
                if (entry.getValue() == s) {
                    key = entry.getKey();
                    mSessionMap.remove(key);
                }
            }
        }

        if ((s != null) && isLoggable(s)) {
            if (DBG) log("removeSession: " + session + " @key '" + key + "'");
            for (String k : mSessionMap.keySet()) {
                if (DBG) log("removeSession:  " + k + ": " + mSessionMap.get(k));
            }
        }
    }

    @Override
    public void processRequest(final RequestEvent event) {
        if (isRequestEvent(Request.INVITE, event)) {
            if (DBG) log("processRequest: mWakeLock.acquire got INVITE, thread:"
                    + Thread.currentThread());
            // Acquire a wake lock and keep it for WAKE_LOCK_HOLDING_TIME;
            // should be large enough to bring up the app.
            mWakeLock.acquire(WAKE_LOCK_HOLDING_TIME);
        }
        process(event);
    }

    @Override
    public void processResponse(ResponseEvent event) {
        process(event);
    }

    @Override
    public void processIOException(IOExceptionEvent event) {
        process(event);
    }

    @Override
    public void processTimeout(TimeoutEvent event) {
        process(event);
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent event) {
        process(event);
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent event) {
        process(event);
    }

    private synchronized void process(EventObject event) {
        SipSessionImpl session = getSipSession(event);
        try {
            boolean isLoggable = isLoggable(session, event);
            boolean processed = (session != null) && session.process(event);
            if (isLoggable && processed) {
                log("process: event new state after: "
                        + SipSession.State.toString(session.mState));
            }
        } catch (Throwable e) {
            loge("process: error event=" + event, getRootCause(e));
            session.onError(e);
        }
    }

    private String extractContent(Message message) {
        // Currently we do not support secure MIME bodies.
        byte[] bytes = message.getRawContent();
        if (bytes != null) {
            try {
                if (message instanceof SIPMessage) {
                    return ((SIPMessage) message).getMessageContent();
                } else {
                    return new String(bytes, "UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
            }
        }
        return null;
    }

    private void extractExternalAddress(ResponseEvent evt) {
        Response response = evt.getResponse();
        ViaHeader viaHeader = (ViaHeader)(response.getHeader(
                SIPHeaderNames.VIA));
        if (viaHeader == null) return;
        int rport = viaHeader.getRPort();
        String externalIp = viaHeader.getReceived();
        if ((rport > 0) && (externalIp != null)) {
            mExternalIp = externalIp;
            mExternalPort = rport;
            if (DBG) {
                log("extractExternalAddress: external addr " + externalIp + ":" + rport
                        + " on " + mSipStack);
            }
        }
    }

    private Throwable getRootCause(Throwable exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            exception = cause;
            cause = exception.getCause();
        }
        return exception;
    }

    private SipSessionImpl createNewSession(RequestEvent event,
            ISipSessionListener listener, ServerTransaction transaction,
            int newState) throws SipException {
        SipSessionImpl newSession = new SipSessionImpl(listener);
        newSession.mServerTransaction = transaction;
        newSession.mState = newState;
        newSession.mDialog = newSession.mServerTransaction.getDialog();
        newSession.mInviteReceived = event;
        newSession.mPeerProfile = createPeerProfile((HeaderAddress)
                event.getRequest().getHeader(FromHeader.NAME));
        newSession.mPeerSessionDescription =
                extractContent(event.getRequest());
        return newSession;
    }

    private class SipSessionCallReceiverImpl extends SipSessionImpl {
        private static final String SSCRI_TAG = "SipSessionCallReceiverImpl";
        private static final boolean SSCRI_DBG = true;

        public SipSessionCallReceiverImpl(ISipSessionListener listener) {
            super(listener);
        }

        private int processInviteWithReplaces(RequestEvent event,
                ReplacesHeader replaces) {
            String callId = replaces.getCallId();
            SipSessionImpl session = mSessionMap.get(callId);
            if (session == null) {
                return Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST;
            }

            Dialog dialog = session.mDialog;
            if (dialog == null) return Response.DECLINE;

            if (!dialog.getLocalTag().equals(replaces.getToTag()) ||
                    !dialog.getRemoteTag().equals(replaces.getFromTag())) {
                // No match is found, returns 481.
                return Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST;
            }

            ReferredByHeader referredBy = (ReferredByHeader) event.getRequest()
                    .getHeader(ReferredByHeader.NAME);
            if ((referredBy == null) ||
                    !dialog.getRemoteParty().equals(referredBy.getAddress())) {
                return Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST;
            }
            return Response.OK;
        }

        private void processNewInviteRequest(RequestEvent event)
                throws SipException {
            ReplacesHeader replaces = (ReplacesHeader) event.getRequest()
                    .getHeader(ReplacesHeader.NAME);
            SipSessionImpl newSession = null;
            if (replaces != null) {
                int response = processInviteWithReplaces(event, replaces);
                if (SSCRI_DBG) {
                    log("processNewInviteRequest: " + replaces
                            + " response=" + response);
                }
                if (response == Response.OK) {
                    SipSessionImpl replacedSession =
                            mSessionMap.get(replaces.getCallId());
                    // got INVITE w/ replaces request.
                    newSession = createNewSession(event,
                            replacedSession.mProxy.getListener(),
                            mSipHelper.getServerTransaction(event),
                            SipSession.State.INCOMING_CALL);
                    newSession.mProxy.onCallTransferring(newSession,
                            newSession.mPeerSessionDescription);
                } else {
                    mSipHelper.sendResponse(event, response);
                }
            } else {
                // New Incoming call.
                newSession = createNewSession(event, mProxy,
                        mSipHelper.sendRinging(event, generateTag()),
                        SipSession.State.INCOMING_CALL);
                mProxy.onRinging(newSession, newSession.mPeerProfile,
                        newSession.mPeerSessionDescription);
            }
            if (newSession != null) addSipSession(newSession);
        }

        @Override
        public boolean process(EventObject evt) throws SipException {
            if (isLoggable(this, evt)) log("process: " + this + ": "
                    + SipSession.State.toString(mState) + ": processing "
                    + logEvt(evt));
            if (isRequestEvent(Request.INVITE, evt)) {
                processNewInviteRequest((RequestEvent) evt);
                return true;
            } else if (isRequestEvent(Request.OPTIONS, evt)) {
                mSipHelper.sendResponse((RequestEvent) evt, Response.OK);
                return true;
            } else {
                return false;
            }
        }

        private void log(String s) {
            Rlog.d(SSCRI_TAG, s);
        }
    }

    static interface KeepAliveProcessCallback {
        /** Invoked when the response of keeping alive comes back. */
        void onResponse(boolean portChanged);
        void onError(int errorCode, String description);
    }

    class SipSessionImpl extends ISipSession.Stub {
        private static final String SSI_TAG = "SipSessionImpl";
        private static final boolean SSI_DBG = true;

        SipProfile mPeerProfile;
        SipSessionListenerProxy mProxy = new SipSessionListenerProxy();
        int mState = SipSession.State.READY_TO_CALL;
        RequestEvent mInviteReceived;
        Dialog mDialog;
        ServerTransaction mServerTransaction;
        ClientTransaction mClientTransaction;
        String mPeerSessionDescription;
        boolean mInCall;
        SessionTimer mSessionTimer;
        int mAuthenticationRetryCount;

        private SipKeepAlive mSipKeepAlive;

        private SipSessionImpl mSipSessionImpl;

        // the following three members are used for handling refer request.
        SipSessionImpl mReferSession;
        ReferredByHeader mReferredBy;
        String mReplaces;

        // lightweight timer
        class SessionTimer {
            private boolean mRunning = true;

            void start(final int timeout) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sleep(timeout);
                        if (mRunning) timeout();
                    }
                }, "SipSessionTimerThread").start();
            }

            synchronized void cancel() {
                mRunning = false;
                this.notify();
            }

            private void timeout() {
                synchronized (SipSessionGroup.this) {
                    onError(SipErrorCode.TIME_OUT, "Session timed out!");
                }
            }

            private synchronized void sleep(int timeout) {
                try {
                    this.wait(timeout * 1000);
                } catch (InterruptedException e) {
                    loge("session timer interrupted!", e);
                }
            }
        }

        public SipSessionImpl(ISipSessionListener listener) {
            setListener(listener);
        }

        SipSessionImpl duplicate() {
            return new SipSessionImpl(mProxy.getListener());
        }

        private void reset() {
            mInCall = false;
            removeSipSession(this);
            mPeerProfile = null;
            mState = SipSession.State.READY_TO_CALL;
            mInviteReceived = null;
            mPeerSessionDescription = null;
            mAuthenticationRetryCount = 0;
            mReferSession = null;
            mReferredBy = null;
            mReplaces = null;

            if (mDialog != null) mDialog.delete();
            mDialog = null;

            try {
                if (mServerTransaction != null) mServerTransaction.terminate();
            } catch (ObjectInUseException e) {
                // ignored
            }
            mServerTransaction = null;

            try {
                if (mClientTransaction != null) mClientTransaction.terminate();
            } catch (ObjectInUseException e) {
                // ignored
            }
            mClientTransaction = null;

            cancelSessionTimer();

            if (mSipSessionImpl != null) {
                mSipSessionImpl.stopKeepAliveProcess();
                mSipSessionImpl = null;
            }
        }

        @Override
        public boolean isInCall() {
            return mInCall;
        }

        @Override
        public String getLocalIp() {
            return mLocalIp;
        }

        @Override
        public SipProfile getLocalProfile() {
            return mLocalProfile;
        }

        @Override
        public SipProfile getPeerProfile() {
            return mPeerProfile;
        }

        @Override
        public String getCallId() {
            return SipHelper.getCallId(getTransaction());
        }

        private Transaction getTransaction() {
            if (mClientTransaction != null) return mClientTransaction;
            if (mServerTransaction != null) return mServerTransaction;
            return null;
        }

        @Override
        public int getState() {
            return mState;
        }

        @Override
        public void setListener(ISipSessionListener listener) {
            mProxy.setListener((listener instanceof SipSessionListenerProxy)
                    ? ((SipSessionListenerProxy) listener).getListener()
                    : listener);
        }

        // process the command in a new thread
        private void doCommandAsync(final EventObject command) {
            new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            processCommand(command);
                        } catch (Throwable e) {
                            loge("command error: " + command + ": "
                                    + mLocalProfile.getUriString(),
                                    getRootCause(e));
                            onError(e);
                        }
                    }
            }, "SipSessionAsyncCmdThread").start();
        }

        @Override
        public void makeCall(SipProfile peerProfile, String sessionDescription,
                int timeout) {
            doCommandAsync(new MakeCallCommand(peerProfile, sessionDescription,
                    timeout));
        }

        @Override
        public void answerCall(String sessionDescription, int timeout) {
            synchronized (SipSessionGroup.this) {
                if (mPeerProfile == null) return;
                doCommandAsync(new MakeCallCommand(mPeerProfile,
                        sessionDescription, timeout));
            }
        }

        @Override
        public void endCall() {
            doCommandAsync(END_CALL);
        }

        @Override
        public void changeCall(String sessionDescription, int timeout) {
            synchronized (SipSessionGroup.this) {
                if (mPeerProfile == null) return;
                doCommandAsync(new MakeCallCommand(mPeerProfile,
                        sessionDescription, timeout));
            }
        }

        @Override
        public void register(int duration) {
            doCommandAsync(new RegisterCommand(duration));
        }

        @Override
        public void unregister() {
            doCommandAsync(DEREGISTER);
        }

        private void processCommand(EventObject command) throws SipException {
            if (isLoggable(command)) log("process cmd: " + command);
            if (!process(command)) {
                onError(SipErrorCode.IN_PROGRESS,
                        "cannot initiate a new transaction to execute: "
                        + command);
            }
        }

        protected String generateTag() {
            // 32-bit randomness
            return String.valueOf((long) (Math.random() * 0x100000000L));
        }

        @Override
        public String toString() {
            try {
                String s = super.toString();
                return s.substring(s.indexOf("@")) + ":"
                        + SipSession.State.toString(mState);
            } catch (Throwable e) {
                return super.toString();
            }
        }

        public boolean process(EventObject evt) throws SipException {
            if (isLoggable(this, evt)) log(" ~~~~~   " + this + ": "
                    + SipSession.State.toString(mState) + ": processing "
                    + logEvt(evt));
            synchronized (SipSessionGroup.this) {
                if (isClosed()) return false;

                if (mSipKeepAlive != null) {
                    // event consumed by keepalive process
                    if (mSipKeepAlive.process(evt)) return true;
                }

                Dialog dialog = null;
                if (evt instanceof RequestEvent) {
                    dialog = ((RequestEvent) evt).getDialog();
                } else if (evt instanceof ResponseEvent) {
                    dialog = ((ResponseEvent) evt).getDialog();
                    extractExternalAddress((ResponseEvent) evt);
                }
                if (dialog != null) mDialog = dialog;

                boolean processed;

                switch (mState) {
                case SipSession.State.REGISTERING:
                case SipSession.State.DEREGISTERING:
                    processed = registeringToReady(evt);
                    break;
                case SipSession.State.READY_TO_CALL:
                    processed = readyForCall(evt);
                    break;
                case SipSession.State.INCOMING_CALL:
                    processed = incomingCall(evt);
                    break;
                case SipSession.State.INCOMING_CALL_ANSWERING:
                    processed = incomingCallToInCall(evt);
                    break;
                case SipSession.State.OUTGOING_CALL:
                case SipSession.State.OUTGOING_CALL_RING_BACK:
                    processed = outgoingCall(evt);
                    break;
                case SipSession.State.OUTGOING_CALL_CANCELING:
                    processed = outgoingCallToReady(evt);
                    break;
                case SipSession.State.IN_CALL:
                    processed = inCall(evt);
                    break;
                case SipSession.State.ENDING_CALL:
                    processed = endingCall(evt);
                    break;
                default:
                    processed = false;
                }
                return (processed || processExceptions(evt));
            }
        }

        private boolean processExceptions(EventObject evt) throws SipException {
            if (isRequestEvent(Request.BYE, evt)) {
                // terminate the call whenever a BYE is received
                mSipHelper.sendResponse((RequestEvent) evt, Response.OK);
                endCallNormally();
                return true;
            } else if (isRequestEvent(Request.CANCEL, evt)) {
                mSipHelper.sendResponse((RequestEvent) evt,
                        Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST);
                return true;
            } else if (evt instanceof TransactionTerminatedEvent) {
                if (isCurrentTransaction((TransactionTerminatedEvent) evt)) {
                    if (evt instanceof TimeoutEvent) {
                        processTimeout((TimeoutEvent) evt);
                    } else {
                        processTransactionTerminated(
                                (TransactionTerminatedEvent) evt);
                    }
                    return true;
                }
            } else if (isRequestEvent(Request.OPTIONS, evt)) {
                mSipHelper.sendResponse((RequestEvent) evt, Response.OK);
                return true;
            } else if (evt instanceof DialogTerminatedEvent) {
                processDialogTerminated((DialogTerminatedEvent) evt);
                return true;
            }
            return false;
        }

        private void processDialogTerminated(DialogTerminatedEvent event) {
            if (mDialog == event.getDialog()) {
                onError(new SipException("dialog terminated"));
            } else {
                if (SSI_DBG) log("not the current dialog; current=" + mDialog
                        + ", terminated=" + event.getDialog());
            }
        }

        private boolean isCurrentTransaction(TransactionTerminatedEvent event) {
            Transaction current = event.isServerTransaction()
                    ? mServerTransaction
                    : mClientTransaction;
            Transaction target = event.isServerTransaction()
                    ? event.getServerTransaction()
                    : event.getClientTransaction();

            if ((current != target) && (mState != SipSession.State.PINGING)) {
                if (SSI_DBG) log("not the current transaction; current="
                        + toString(current) + ", target=" + toString(target));
                return false;
            } else if (current != null) {
                if (SSI_DBG) log("transaction terminated: " + toString(current));
                return true;
            } else {
                // no transaction; shouldn't be here; ignored
                return true;
            }
        }

        private String toString(Transaction transaction) {
            if (transaction == null) return "null";
            Request request = transaction.getRequest();
            Dialog dialog = transaction.getDialog();
            CSeqHeader cseq = (CSeqHeader) request.getHeader(CSeqHeader.NAME);
            return String.format("req=%s,%s,s=%s,ds=%s,", request.getMethod(),
                    cseq.getSeqNumber(), transaction.getState(),
                    ((dialog == null) ? "-" : dialog.getState()));
        }

        private void processTransactionTerminated(
                TransactionTerminatedEvent event) {
            switch (mState) {
                case SipSession.State.IN_CALL:
                case SipSession.State.READY_TO_CALL:
                    if (SSI_DBG) log("Transaction terminated; do nothing");
                    break;
                default:
                    if (SSI_DBG) log("Transaction terminated early: " + this);
                    onError(SipErrorCode.TRANSACTION_TERMINTED,
                            "transaction terminated");
            }
        }

        private void processTimeout(TimeoutEvent event) {
            if (SSI_DBG) log("processing Timeout...");
            switch (mState) {
                case SipSession.State.REGISTERING:
                case SipSession.State.DEREGISTERING:
                    reset();
                    mProxy.onRegistrationTimeout(this);
                    break;
                case SipSession.State.INCOMING_CALL:
                case SipSession.State.INCOMING_CALL_ANSWERING:
                case SipSession.State.OUTGOING_CALL:
                case SipSession.State.OUTGOING_CALL_CANCELING:
                    onError(SipErrorCode.TIME_OUT, event.toString());
                    break;

                default:
                    if (SSI_DBG) log("   do nothing");
                    break;
            }
        }

        private int getExpiryTime(Response response) {
            int time = -1;
            ContactHeader contact = (ContactHeader) response.getHeader(ContactHeader.NAME);
            if (contact != null) {
                time = contact.getExpires();
            }
            ExpiresHeader expires = (ExpiresHeader) response.getHeader(ExpiresHeader.NAME);
            if (expires != null && (time < 0 || time > expires.getExpires())) {
                time = expires.getExpires();
            }
            if (time <= 0) {
                time = EXPIRY_TIME;
            }
            expires = (ExpiresHeader) response.getHeader(MinExpiresHeader.NAME);
            if (expires != null && time < expires.getExpires()) {
                time = expires.getExpires();
            }
            if (SSI_DBG) {
                log("Expiry time = " + time);
            }
            return time;
        }

        private boolean registeringToReady(EventObject evt)
                throws SipException {
            if (expectResponse(Request.REGISTER, evt)) {
                ResponseEvent event = (ResponseEvent) evt;
                Response response = event.getResponse();

                int statusCode = response.getStatusCode();
                switch (statusCode) {
                case Response.OK:
                    int state = mState;
                    onRegistrationDone((state == SipSession.State.REGISTERING)
                            ? getExpiryTime(((ResponseEvent) evt).getResponse())
                            : -1);
                    return true;
                case Response.UNAUTHORIZED:
                case Response.PROXY_AUTHENTICATION_REQUIRED:
                    handleAuthentication(event);
                    return true;
                default:
                    if (statusCode >= 500) {
                        onRegistrationFailed(response);
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean handleAuthentication(ResponseEvent event)
                throws SipException {
            Response response = event.getResponse();
            String nonce = getNonceFromResponse(response);
            if (nonce == null) {
                onError(SipErrorCode.SERVER_ERROR,
                        "server does not provide challenge");
                return false;
            } else if (mAuthenticationRetryCount < 2) {
                mClientTransaction = mSipHelper.handleChallenge(
                        event, getAccountManager());
                mDialog = mClientTransaction.getDialog();
                mAuthenticationRetryCount++;
                if (isLoggable(this, event)) {
                    if (SSI_DBG) log("   authentication retry count="
                            + mAuthenticationRetryCount);
                }
                return true;
            } else {
                if (crossDomainAuthenticationRequired(response)) {
                    onError(SipErrorCode.CROSS_DOMAIN_AUTHENTICATION,
                            getRealmFromResponse(response));
                } else {
                    onError(SipErrorCode.INVALID_CREDENTIALS,
                            "incorrect username or password");
                }
                return false;
            }
        }

        private boolean crossDomainAuthenticationRequired(Response response) {
            String realm = getRealmFromResponse(response);
            if (realm == null) realm = "";
            return !mLocalProfile.getSipDomain().trim().equals(realm.trim());
        }

        private AccountManager getAccountManager() {
            return new AccountManager() {
                @Override
                public UserCredentials getCredentials(ClientTransaction
                        challengedTransaction, String realm) {
                    return new UserCredentials() {
                        @Override
                        public String getUserName() {
                            String username = mLocalProfile.getAuthUserName();
                            return (!TextUtils.isEmpty(username) ? username :
                                    mLocalProfile.getUserName());
                        }

                        @Override
                        public String getPassword() {
                            return mPassword;
                        }

                        @Override
                        public String getSipDomain() {
                            return mLocalProfile.getSipDomain();
                        }
                    };
                }
            };
        }

        private String getRealmFromResponse(Response response) {
            WWWAuthenticate wwwAuth = (WWWAuthenticate)response.getHeader(
                    SIPHeaderNames.WWW_AUTHENTICATE);
            if (wwwAuth != null) return wwwAuth.getRealm();
            ProxyAuthenticate proxyAuth = (ProxyAuthenticate)response.getHeader(
                    SIPHeaderNames.PROXY_AUTHENTICATE);
            return (proxyAuth == null) ? null : proxyAuth.getRealm();
        }

        private String getNonceFromResponse(Response response) {
            WWWAuthenticate wwwAuth = (WWWAuthenticate)response.getHeader(
                    SIPHeaderNames.WWW_AUTHENTICATE);
            if (wwwAuth != null) return wwwAuth.getNonce();
            ProxyAuthenticate proxyAuth = (ProxyAuthenticate)response.getHeader(
                    SIPHeaderNames.PROXY_AUTHENTICATE);
            return (proxyAuth == null) ? null : proxyAuth.getNonce();
        }

        private String getResponseString(int statusCode) {
            StatusLine statusLine = new StatusLine();
            statusLine.setStatusCode(statusCode);
            statusLine.setReasonPhrase(SIPResponse.getReasonPhrase(statusCode));
            return statusLine.encode();
        }

        private boolean readyForCall(EventObject evt) throws SipException {
            // expect MakeCallCommand, RegisterCommand, DEREGISTER
            if (evt instanceof MakeCallCommand) {
                mState = SipSession.State.OUTGOING_CALL;
                MakeCallCommand cmd = (MakeCallCommand) evt;
                mPeerProfile = cmd.getPeerProfile();
                if (mReferSession != null) {
                    mSipHelper.sendReferNotify(mReferSession.mDialog,
                            getResponseString(Response.TRYING));
                }
                mClientTransaction = mSipHelper.sendInvite(
                        mLocalProfile, mPeerProfile, cmd.getSessionDescription(),
                        generateTag(), mReferredBy, mReplaces);
                mDialog = mClientTransaction.getDialog();
                addSipSession(this);
                startSessionTimer(cmd.getTimeout());
                mProxy.onCalling(this);
                return true;
            } else if (evt instanceof RegisterCommand) {
                mState = SipSession.State.REGISTERING;
                int duration = ((RegisterCommand) evt).getDuration();
                mClientTransaction = mSipHelper.sendRegister(mLocalProfile,
                        generateTag(), duration);
                mDialog = mClientTransaction.getDialog();
                addSipSession(this);
                mProxy.onRegistering(this);
                return true;
            } else if (DEREGISTER == evt) {
                mState = SipSession.State.DEREGISTERING;
                mClientTransaction = mSipHelper.sendRegister(mLocalProfile,
                        generateTag(), 0);
                mDialog = mClientTransaction.getDialog();
                addSipSession(this);
                mProxy.onRegistering(this);
                return true;
            }
            return false;
        }

        private boolean incomingCall(EventObject evt) throws SipException {
            // expect MakeCallCommand(answering) , END_CALL cmd , Cancel
            if (evt instanceof MakeCallCommand) {
                // answer call
                mState = SipSession.State.INCOMING_CALL_ANSWERING;
                mServerTransaction = mSipHelper.sendInviteOk(mInviteReceived,
                        mLocalProfile,
                        ((MakeCallCommand) evt).getSessionDescription(),
                        mServerTransaction,
                        mExternalIp, mExternalPort);
                startSessionTimer(((MakeCallCommand) evt).getTimeout());
                return true;
            } else if (END_CALL == evt) {
                mSipHelper.sendInviteBusyHere(mInviteReceived,
                        mServerTransaction);
                endCallNormally();
                return true;
            } else if (isRequestEvent(Request.CANCEL, evt)) {
                RequestEvent event = (RequestEvent) evt;
                mSipHelper.sendResponse(event, Response.OK);
                mSipHelper.sendInviteRequestTerminated(
                        mInviteReceived.getRequest(), mServerTransaction);
                endCallNormally();
                return true;
            }
            return false;
        }

        private boolean incomingCallToInCall(EventObject evt) {
            // expect ACK, CANCEL request
            if (isRequestEvent(Request.ACK, evt)) {
                String sdp = extractContent(((RequestEvent) evt).getRequest());
                if (sdp != null) mPeerSessionDescription = sdp;
                if (mPeerSessionDescription == null) {
                    onError(SipErrorCode.CLIENT_ERROR, "peer sdp is empty");
                } else {
                    establishCall(false);
                }
                return true;
            } else if (isRequestEvent(Request.CANCEL, evt)) {
                // http://tools.ietf.org/html/rfc3261#section-9.2
                // Final response has been sent; do nothing here.
                return true;
            }
            return false;
        }

        private boolean outgoingCall(EventObject evt) throws SipException {
            if (expectResponse(Request.INVITE, evt)) {
                ResponseEvent event = (ResponseEvent) evt;
                Response response = event.getResponse();

                int statusCode = response.getStatusCode();
                switch (statusCode) {
                case Response.RINGING:
                case Response.CALL_IS_BEING_FORWARDED:
                case Response.QUEUED:
                case Response.SESSION_PROGRESS:
                    // feedback any provisional responses (except TRYING) as
                    // ring back for better UX
                    if (mState == SipSession.State.OUTGOING_CALL) {
                        mState = SipSession.State.OUTGOING_CALL_RING_BACK;
                        cancelSessionTimer();
                        mProxy.onRingingBack(this);
                    }
                    return true;
                case Response.OK:
                    if (mReferSession != null) {
                        mSipHelper.sendReferNotify(mReferSession.mDialog,
                                getResponseString(Response.OK));
                        // since we don't need to remember the session anymore.
                        mReferSession = null;
                    }
                    mSipHelper.sendInviteAck(event, mDialog);
                    mPeerSessionDescription = extractContent(response);
                    establishCall(true);
                    return true;
                case Response.UNAUTHORIZED:
                case Response.PROXY_AUTHENTICATION_REQUIRED:
                    if (handleAuthentication(event)) {
                        addSipSession(this);
                    }
                    return true;
                case Response.REQUEST_PENDING:
                    // TODO: rfc3261#section-14.1; re-schedule invite
                    return true;
                default:
                    if (mReferSession != null) {
                        mSipHelper.sendReferNotify(mReferSession.mDialog,
                                getResponseString(Response.SERVICE_UNAVAILABLE));
                    }
                    if (statusCode >= 400) {
                        // error: an ack is sent automatically by the stack
                        onError(response);
                        return true;
                    } else if (statusCode >= 300) {
                        // TODO: handle 3xx (redirect)
                    } else {
                        return true;
                    }
                }
                return false;
            } else if (END_CALL == evt) {
                // RFC says that UA should not send out cancel when no
                // response comes back yet. We are cheating for not checking
                // response.
                mState = SipSession.State.OUTGOING_CALL_CANCELING;
                mSipHelper.sendCancel(mClientTransaction);
                startSessionTimer(CANCEL_CALL_TIMER);
                return true;
            } else if (isRequestEvent(Request.INVITE, evt)) {
                // Call self? Send BUSY HERE so server may redirect the call to
                // voice mailbox.
                RequestEvent event = (RequestEvent) evt;
                mSipHelper.sendInviteBusyHere(event,
                        event.getServerTransaction());
                return true;
            }
            return false;
        }

        private boolean outgoingCallToReady(EventObject evt)
                throws SipException {
            if (evt instanceof ResponseEvent) {
                ResponseEvent event = (ResponseEvent) evt;
                Response response = event.getResponse();
                int statusCode = response.getStatusCode();
                if (expectResponse(Request.CANCEL, evt)) {
                    if (statusCode == Response.OK) {
                        // do nothing; wait for REQUEST_TERMINATED
                        return true;
                    }
                } else if (expectResponse(Request.INVITE, evt)) {
                    switch (statusCode) {
                        case Response.OK:
                            outgoingCall(evt); // abort Cancel
                            return true;
                        case Response.REQUEST_TERMINATED:
                            endCallNormally();
                            return true;
                    }
                } else {
                    return false;
                }

                if (statusCode >= 400) {
                    onError(response);
                    return true;
                }
            } else if (evt instanceof TransactionTerminatedEvent) {
                // rfc3261#section-14.1:
                // if re-invite gets timed out, terminate the dialog; but
                // re-invite is not reliable, just let it go and pretend
                // nothing happened.
                onError(new SipException("timed out"));
            }
            return false;
        }

        private boolean processReferRequest(RequestEvent event)
                throws SipException {
            try {
                ReferToHeader referto = (ReferToHeader) event.getRequest()
                        .getHeader(ReferTo.NAME);
                Address address = referto.getAddress();
                SipURI uri = (SipURI) address.getURI();
                String replacesHeader = uri.getHeader(ReplacesHeader.NAME);
                String username = uri.getUser();
                if (username == null) {
                    mSipHelper.sendResponse(event, Response.BAD_REQUEST);
                    return false;
                }
                // send notify accepted
                mSipHelper.sendResponse(event, Response.ACCEPTED);
                SipSessionImpl newSession = createNewSession(event,
                        this.mProxy.getListener(),
                        mSipHelper.getServerTransaction(event),
                        SipSession.State.READY_TO_CALL);
                newSession.mReferSession = this;
                newSession.mReferredBy = (ReferredByHeader) event.getRequest()
                        .getHeader(ReferredByHeader.NAME);
                newSession.mReplaces = replacesHeader;
                newSession.mPeerProfile = createPeerProfile(referto);
                newSession.mProxy.onCallTransferring(newSession,
                        null);
                return true;
            } catch (IllegalArgumentException e) {
                throw new SipException("createPeerProfile()", e);
            }
        }

        private boolean inCall(EventObject evt) throws SipException {
            // expect END_CALL cmd, BYE request, hold call (MakeCallCommand)
            // OK retransmission is handled in SipStack
            if (END_CALL == evt) {
                // rfc3261#section-15.1.1
                mState = SipSession.State.ENDING_CALL;
                mSipHelper.sendBye(mDialog);
                mProxy.onCallEnded(this);
                startSessionTimer(END_CALL_TIMER);
                return true;
            } else if (isRequestEvent(Request.INVITE, evt)) {
                // got Re-INVITE
                mState = SipSession.State.INCOMING_CALL;
                RequestEvent event = mInviteReceived = (RequestEvent) evt;
                mPeerSessionDescription = extractContent(event.getRequest());
                mServerTransaction = null;
                mProxy.onRinging(this, mPeerProfile, mPeerSessionDescription);
                return true;
            } else if (isRequestEvent(Request.BYE, evt)) {
                mSipHelper.sendResponse((RequestEvent) evt, Response.OK);
                endCallNormally();
                return true;
            } else if (isRequestEvent(Request.REFER, evt)) {
                return processReferRequest((RequestEvent) evt);
            } else if (evt instanceof MakeCallCommand) {
                // to change call
                mState = SipSession.State.OUTGOING_CALL;
                mClientTransaction = mSipHelper.sendReinvite(mDialog,
                        ((MakeCallCommand) evt).getSessionDescription());
                startSessionTimer(((MakeCallCommand) evt).getTimeout());
                return true;
            } else if (evt instanceof ResponseEvent) {
                if (expectResponse(Request.NOTIFY, evt)) return true;
            }
            return false;
        }

        private boolean endingCall(EventObject evt) throws SipException {
            if (expectResponse(Request.BYE, evt)) {
                ResponseEvent event = (ResponseEvent) evt;
                Response response = event.getResponse();

                int statusCode = response.getStatusCode();
                switch (statusCode) {
                    case Response.UNAUTHORIZED:
                    case Response.PROXY_AUTHENTICATION_REQUIRED:
                        if (handleAuthentication(event)) {
                            return true;
                        } else {
                            // can't authenticate; pass through to end session
                        }
                }
                cancelSessionTimer();
                reset();
                return true;
            }
            return false;
        }

        // timeout in seconds
        private void startSessionTimer(int timeout) {
            if (timeout > 0) {
                mSessionTimer = new SessionTimer();
                mSessionTimer.start(timeout);
            }
        }

        private void cancelSessionTimer() {
            if (mSessionTimer != null) {
                mSessionTimer.cancel();
                mSessionTimer = null;
            }
        }

        private String createErrorMessage(Response response) {
            return String.format("%s (%d)", response.getReasonPhrase(),
                    response.getStatusCode());
        }

        private void enableKeepAlive() {
            if (mSipSessionImpl != null) {
                mSipSessionImpl.stopKeepAliveProcess();
            } else {
                mSipSessionImpl = duplicate();
            }
            try {
                mSipSessionImpl.startKeepAliveProcess(
                        INCALL_KEEPALIVE_INTERVAL, mPeerProfile, null);
            } catch (SipException e) {
                loge("keepalive cannot be enabled; ignored", e);
                mSipSessionImpl.stopKeepAliveProcess();
            }
        }

        private void establishCall(boolean enableKeepAlive) {
            mState = SipSession.State.IN_CALL;
            cancelSessionTimer();
            if (!mInCall && enableKeepAlive) enableKeepAlive();
            mInCall = true;
            mProxy.onCallEstablished(this, mPeerSessionDescription);
        }

        private void endCallNormally() {
            reset();
            mProxy.onCallEnded(this);
        }

        private void endCallOnError(int errorCode, String message) {
            reset();
            mProxy.onError(this, errorCode, message);
        }

        private void endCallOnBusy() {
            reset();
            mProxy.onCallBusy(this);
        }

        private void onError(int errorCode, String message) {
            cancelSessionTimer();
            switch (mState) {
                case SipSession.State.REGISTERING:
                case SipSession.State.DEREGISTERING:
                    onRegistrationFailed(errorCode, message);
                    break;
                default:
                    endCallOnError(errorCode, message);
            }
        }


        private void onError(Throwable exception) {
            exception = getRootCause(exception);
            onError(getErrorCode(exception), exception.toString());
        }

        private void onError(Response response) {
            int statusCode = response.getStatusCode();
            if (!mInCall && (statusCode == Response.BUSY_HERE)) {
                endCallOnBusy();
            } else {
                onError(getErrorCode(statusCode), createErrorMessage(response));
            }
        }

        private int getErrorCode(int responseStatusCode) {
            switch (responseStatusCode) {
                case Response.TEMPORARILY_UNAVAILABLE:
                case Response.FORBIDDEN:
                case Response.GONE:
                case Response.NOT_FOUND:
                case Response.NOT_ACCEPTABLE:
                case Response.NOT_ACCEPTABLE_HERE:
                    return SipErrorCode.PEER_NOT_REACHABLE;

                case Response.REQUEST_URI_TOO_LONG:
                case Response.ADDRESS_INCOMPLETE:
                case Response.AMBIGUOUS:
                    return SipErrorCode.INVALID_REMOTE_URI;

                case Response.REQUEST_TIMEOUT:
                    return SipErrorCode.TIME_OUT;

                default:
                    if (responseStatusCode < 500) {
                        return SipErrorCode.CLIENT_ERROR;
                    } else {
                        return SipErrorCode.SERVER_ERROR;
                    }
            }
        }

        private int getErrorCode(Throwable exception) {
            String message = exception.getMessage();
            if (exception instanceof UnknownHostException) {
                return SipErrorCode.SERVER_UNREACHABLE;
            } else if (exception instanceof IOException) {
                return SipErrorCode.SOCKET_ERROR;
            } else {
                return SipErrorCode.CLIENT_ERROR;
            }
        }

        private void onRegistrationDone(int duration) {
            reset();
            mProxy.onRegistrationDone(this, duration);
        }

        private void onRegistrationFailed(int errorCode, String message) {
            reset();
            mProxy.onRegistrationFailed(this, errorCode, message);
        }

        private void onRegistrationFailed(Response response) {
            int statusCode = response.getStatusCode();
            onRegistrationFailed(getErrorCode(statusCode),
                    createErrorMessage(response));
        }

        // Notes: SipSessionListener will be replaced by the keepalive process
        // @param interval in seconds
        public void startKeepAliveProcess(int interval,
                KeepAliveProcessCallback callback) throws SipException {
            synchronized (SipSessionGroup.this) {
                startKeepAliveProcess(interval, mLocalProfile, callback);
            }
        }

        // Notes: SipSessionListener will be replaced by the keepalive process
        // @param interval in seconds
        public void startKeepAliveProcess(int interval, SipProfile peerProfile,
                KeepAliveProcessCallback callback) throws SipException {
            synchronized (SipSessionGroup.this) {
                if (mSipKeepAlive != null) {
                    throw new SipException("Cannot create more than one "
                            + "keepalive process in a SipSession");
                }
                mPeerProfile = peerProfile;
                mSipKeepAlive = new SipKeepAlive();
                mProxy.setListener(mSipKeepAlive);
                mSipKeepAlive.start(interval, callback);
            }
        }

        public void stopKeepAliveProcess() {
            synchronized (SipSessionGroup.this) {
                if (mSipKeepAlive != null) {
                    mSipKeepAlive.stop();
                    mSipKeepAlive = null;
                }
            }
        }

        class SipKeepAlive extends SipSessionAdapter implements Runnable {
            private static final String SKA_TAG = "SipKeepAlive";
            private static final boolean SKA_DBG = true;

            private boolean mRunning = false;
            private KeepAliveProcessCallback mCallback;

            private boolean mPortChanged = false;
            private int mRPort = 0;
            private int mInterval; // just for debugging

            // @param interval in seconds
            void start(int interval, KeepAliveProcessCallback callback) {
                if (mRunning) return;
                mRunning = true;
                mInterval = interval;
                mCallback = new KeepAliveProcessCallbackProxy(callback);
                mWakeupTimer.set(interval * 1000, this);
                if (SKA_DBG) {
                    log("start keepalive:"
                            + mLocalProfile.getUriString());
                }

                // No need to run the first time in a separate thread for now
                run();
            }

            // return true if the event is consumed
            boolean process(EventObject evt) {
                if (mRunning && (mState == SipSession.State.PINGING)) {
                    if (evt instanceof ResponseEvent) {
                        if (parseOptionsResult(evt)) {
                            if (mPortChanged) {
                                resetExternalAddress();
                                stop();
                            } else {
                                cancelSessionTimer();
                                removeSipSession(SipSessionImpl.this);
                            }
                            mCallback.onResponse(mPortChanged);
                            return true;
                        }
                    }
                }
                return false;
            }

            // SipSessionAdapter
            // To react to the session timeout event and network error.
            @Override
            public void onError(ISipSession session, int errorCode, String message) {
                stop();
                mCallback.onError(errorCode, message);
            }

            // SipWakeupTimer timeout handler
            // To send out keepalive message.
            @Override
            public void run() {
                synchronized (SipSessionGroup.this) {
                    if (!mRunning) return;

                    if (DBG_PING) {
                        String peerUri = (mPeerProfile == null)
                                ? "null"
                                : mPeerProfile.getUriString();
                        log("keepalive: " + mLocalProfile.getUriString()
                                + " --> " + peerUri + ", interval=" + mInterval);
                    }
                    try {
                        sendKeepAlive();
                    } catch (Throwable t) {
                        if (SKA_DBG) {
                            loge("keepalive error: "
                                    + mLocalProfile.getUriString(), getRootCause(t));
                        }
                        // It's possible that the keepalive process is being stopped
                        // during session.sendKeepAlive() so need to check mRunning
                        // again here.
                        if (mRunning) SipSessionImpl.this.onError(t);
                    }
                }
            }

            void stop() {
                synchronized (SipSessionGroup.this) {
                    if (SKA_DBG) {
                        log("stop keepalive:" + mLocalProfile.getUriString()
                                + ",RPort=" + mRPort);
                    }
                    mRunning = false;
                    mWakeupTimer.cancel(this);
                    reset();
                }
            }

            private void sendKeepAlive() throws SipException {
                synchronized (SipSessionGroup.this) {
                    mState = SipSession.State.PINGING;
                    mClientTransaction = mSipHelper.sendOptions(
                            mLocalProfile, mPeerProfile, generateTag());
                    mDialog = mClientTransaction.getDialog();
                    addSipSession(SipSessionImpl.this);

                    startSessionTimer(KEEPALIVE_TIMEOUT);
                    // when timed out, onError() will be called with SipErrorCode.TIME_OUT
                }
            }

            private boolean parseOptionsResult(EventObject evt) {
                if (expectResponse(Request.OPTIONS, evt)) {
                    ResponseEvent event = (ResponseEvent) evt;
                    int rPort = getRPortFromResponse(event.getResponse());
                    if (rPort != -1) {
                        if (mRPort == 0) mRPort = rPort;
                        if (mRPort != rPort) {
                            mPortChanged = true;
                            if (SKA_DBG) log(String.format(
                                    "rport is changed: %d <> %d", mRPort, rPort));
                            mRPort = rPort;
                        } else {
                            if (SKA_DBG) log("rport is the same: " + rPort);
                        }
                    } else {
                        if (SKA_DBG) log("peer did not respond rport");
                    }
                    return true;
                }
                return false;
            }

            private int getRPortFromResponse(Response response) {
                ViaHeader viaHeader = (ViaHeader)(response.getHeader(
                        SIPHeaderNames.VIA));
                return (viaHeader == null) ? -1 : viaHeader.getRPort();
            }

            private void log(String s) {
                Rlog.d(SKA_TAG, s);
            }
        }

        private void log(String s) {
            Rlog.d(SSI_TAG, s);
        }
    }

    /**
     * @return true if the event is a request event matching the specified
     *      method; false otherwise
     */
    private static boolean isRequestEvent(String method, EventObject event) {
        try {
            if (event instanceof RequestEvent) {
                RequestEvent requestEvent = (RequestEvent) event;
                return method.equals(requestEvent.getRequest().getMethod());
            }
        } catch (Throwable e) {
        }
        return false;
    }

    private static String getCseqMethod(Message message) {
        return ((CSeqHeader) message.getHeader(CSeqHeader.NAME)).getMethod();
    }

    /**
     * @return true if the event is a response event and the CSeqHeader method
     * match the given arguments; false otherwise
     */
    private static boolean expectResponse(
            String expectedMethod, EventObject evt) {
        if (evt instanceof ResponseEvent) {
            ResponseEvent event = (ResponseEvent) evt;
            Response response = event.getResponse();
            return expectedMethod.equalsIgnoreCase(getCseqMethod(response));
        }
        return false;
    }

    private static SipProfile createPeerProfile(HeaderAddress header)
            throws SipException {
        try {
            Address address = header.getAddress();
            SipURI uri = (SipURI) address.getURI();
            String username = uri.getUser();
            if (username == null) username = ANONYMOUS;
            int port = uri.getPort();
            SipProfile.Builder builder =
                    new SipProfile.Builder(username, uri.getHost())
                    .setDisplayName(address.getDisplayName());
            if (port > 0) builder.setPort(port);
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new SipException("createPeerProfile()", e);
        } catch (ParseException e) {
            throw new SipException("createPeerProfile()", e);
        }
    }

    private static boolean isLoggable(SipSessionImpl s) {
        if (s != null) {
            switch (s.mState) {
                case SipSession.State.PINGING:
                    return DBG_PING;
            }
        }
        return DBG;
    }

    private static boolean isLoggable(EventObject evt) {
        return isLoggable(null, evt);
    }

    private static boolean isLoggable(SipSessionImpl s, EventObject evt) {
        if (!isLoggable(s)) return false;
        if (evt == null) return false;

        if (evt instanceof ResponseEvent) {
            Response response = ((ResponseEvent) evt).getResponse();
            if (Request.OPTIONS.equals(response.getHeader(CSeqHeader.NAME))) {
                return DBG_PING;
            }
            return DBG;
        } else if (evt instanceof RequestEvent) {
            if (isRequestEvent(Request.OPTIONS, evt)) {
                return DBG_PING;
            }
            return DBG;
        }
        return false;
    }

    private static String logEvt(EventObject evt) {
        if (evt instanceof RequestEvent) {
            return ((RequestEvent) evt).getRequest().toString();
        } else if (evt instanceof ResponseEvent) {
            return ((ResponseEvent) evt).getResponse().toString();
        } else {
            return evt.toString();
        }
    }

    private class RegisterCommand extends EventObject {
        private int mDuration;

        public RegisterCommand(int duration) {
            super(SipSessionGroup.this);
            mDuration = duration;
        }

        public int getDuration() {
            return mDuration;
        }
    }

    private class MakeCallCommand extends EventObject {
        private String mSessionDescription;
        private int mTimeout; // in seconds

        public MakeCallCommand(SipProfile peerProfile,
                String sessionDescription, int timeout) {
            super(peerProfile);
            mSessionDescription = sessionDescription;
            mTimeout = timeout;
        }

        public SipProfile getPeerProfile() {
            return (SipProfile) getSource();
        }

        public String getSessionDescription() {
            return mSessionDescription;
        }

        public int getTimeout() {
            return mTimeout;
        }
    }

    /** Class to help safely run KeepAliveProcessCallback in a different thread. */
    static class KeepAliveProcessCallbackProxy implements KeepAliveProcessCallback {
        private static final String KAPCP_TAG = "KeepAliveProcessCallbackProxy";
        private KeepAliveProcessCallback mCallback;

        KeepAliveProcessCallbackProxy(KeepAliveProcessCallback callback) {
            mCallback = callback;
        }

        private void proxy(Runnable runnable) {
            // One thread for each calling back.
            // Note: Guarantee ordering if the issue becomes important. Currently,
            // the chance of handling two callback events at a time is none.
            new Thread(runnable, "SIP-KeepAliveProcessCallbackThread").start();
        }

        @Override
        public void onResponse(final boolean portChanged) {
            if (mCallback == null) return;
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        mCallback.onResponse(portChanged);
                    } catch (Throwable t) {
                        loge("onResponse", t);
                    }
                }
            });
        }

        @Override
        public void onError(final int errorCode, final String description) {
            if (mCallback == null) return;
            proxy(new Runnable() {
                @Override
                public void run() {
                    try {
                        mCallback.onError(errorCode, description);
                    } catch (Throwable t) {
                        loge("onError", t);
                    }
                }
            });
        }

        private void loge(String s, Throwable t) {
            Rlog.e(KAPCP_TAG, s, t);
        }
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void loge(String s, Throwable t) {
        Rlog.e(TAG, s, t);
    }
}
