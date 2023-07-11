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

package com.android.internal.telephony.dataconnection;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Message;
import android.telephony.Annotation.ApnType;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.SparseIntArray;

import com.android.internal.R;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.RetryManager;
import com.android.internal.telephony.dataconnection.DcTracker.ReleaseNetworkType;
import com.android.internal.telephony.dataconnection.DcTracker.RequestNetworkType;
import com.android.internal.util.IndentingPrintWriter;
import com.android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Maintain the Apn context
 */
public class ApnContext {

    public final String LOG_TAG;
    private final static String SLOG_TAG = "ApnContext";

    protected static final boolean DBG = false;

    private final Phone mPhone;

    private final String mApnType;

    private DctConstants.State mState;

    private int mPriority;

    private ApnSetting mApnSetting;

    private DataConnection mDataConnection;

    private String mReason;

    /**
     * user/app requested connection on this APN
     */
    AtomicBoolean mDataEnabled;

    private final Object mRefCountLock = new Object();

    private final DcTracker mDcTracker;


    /**
     * Remember this as a change in this value to a more permissive state
     * should cause us to retry even permanent failures
     */
    private boolean mConcurrentVoiceAndDataAllowed;

    /**
     * used to track a single connection request so disconnects can get ignored if
     * obsolete.
     */
    private final AtomicInteger mConnectionGeneration = new AtomicInteger(0);

    /**
     * Retry manager that handles the APN retry and delays.
     */
    private final RetryManager mRetryManager;

    /**
     * ApnContext constructor
     * @param phone phone object
     * @param typeId APN type Id
     * @param logTag Tag for logging
     * @param tracker Data call tracker
     * @param priority Priority of APN type
     */
    public ApnContext(Phone phone, int typeId, String logTag, DcTracker tracker, int priority) {
        this(phone, ApnSetting.getApnTypeString(typeId), logTag, tracker, priority);
    }

    /**
     * ApnContext constructor
     * @param phone phone object
     * @param apnType APN type (e.g. default, supl, mms, etc...)
     * @param logTag Tag for logging
     * @param tracker Data call tracker
     * @param priority Priority of APN type
     */
    public ApnContext(Phone phone, String apnType, String logTag, DcTracker tracker, int priority) {
        mPhone = phone;
        mApnType = apnType;
        mState = DctConstants.State.IDLE;
        setReason(Phone.REASON_DATA_ENABLED);
        mDataEnabled = new AtomicBoolean(false);
        mPriority = priority;
        LOG_TAG = logTag;
        mDcTracker = tracker;
        mRetryManager = new RetryManager(phone, tracker.getDataThrottler(),
                ApnSetting.getApnTypesBitmaskFromString(apnType));
    }



    /**
     * Get the APN type
     * @return The APN type
     */
    public String getApnType() {
        return mApnType;
    }

    /**
     * Gets the APN type bitmask.
     * @return The APN type bitmask
     */
    public int getApnTypeBitmask() {
        return ApnSetting.getApnTypesBitmaskFromString(mApnType);
    }

    /**
     * Get the associated data connection
     * @return The data connection
     */
    public synchronized DataConnection getDataConnection() {
        return mDataConnection;
    }

    /**
     * This priority is taken into account when concurrent data connections are not allowed.  The
     * APN with the HIGHER priority is given preference.
     * @return The priority of the APN type
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * Updates the priority of this context.
     * @param priority The priority of the APN type
     */
    public void setPriority(int priority) {
        mPriority = priority;
    }

    /**
     * Keeping for backwards compatibility and in case it's needed in the future
     * @return true
     */
    public boolean isDependencyMet() {
        return true;
    }

    /**
     * Set the associated data connection.
     * @param dc data connection
     */
    public synchronized void setDataConnection(DataConnection dc) {
        log("setDataConnectionAc: old=" + mDataConnection + ",new=" + dc + " this=" + this);
        mDataConnection = dc;
    }

    /**
     * Release data connection.
     * @param reason The reason of releasing data connection
     */
    public synchronized void releaseDataConnection(String reason) {
        if (mDataConnection != null) {
            mDataConnection.tearDown(this, reason, null);
            mDataConnection = null;
        }
        setState(DctConstants.State.IDLE);
    }

    /**
     * Get the current APN setting.
     * @return APN setting
     */
    public synchronized ApnSetting getApnSetting() {
        log("getApnSetting: apnSetting=" + mApnSetting);
        return mApnSetting;
    }

    /**
     * Set the APN setting.
     * @param apnSetting APN setting
     */
    public synchronized void setApnSetting(ApnSetting apnSetting) {
        log("setApnSetting: apnSetting=" + apnSetting);
        mApnSetting = apnSetting;
    }

    /**
     * Set the list of APN candidates which will be used for data call setup later.
     * @param waitingApns List of APN candidates
     */
    public synchronized void setWaitingApns(ArrayList<ApnSetting> waitingApns) {
        mRetryManager.setWaitingApns(waitingApns);
    }

    /**
     * Get the next available APN to try.
     * @return APN setting which will be used for data call setup.{@code null} if there is no
     * APN can be retried.
     */
    public @Nullable ApnSetting getNextApnSetting() {
        return mRetryManager.getNextApnSetting();
    }

    /**
     * Get the delay for trying the next APN setting if the current one failed.
     * @param failFastEnabled True if fail fast mode enabled. In this case we'll use a shorter
     * delay.
     * @return The delay in milliseconds
     */
    public long getDelayForNextApn(boolean failFastEnabled) {
        return mRetryManager.getDelayForNextApn(failFastEnabled || isFastRetryReason());
    }

    /**
     * Mark the current APN setting permanently failed, which means it will not be retried anymore.
     * @param apn APN setting
     */
    public void markApnPermanentFailed(ApnSetting apn) {
        mRetryManager.markApnPermanentFailed(apn);
    }

    /**
     * Get the list of waiting APNs.
     * @return the list of waiting APNs
     */
    public @NonNull ArrayList<ApnSetting> getWaitingApns() {
        return mRetryManager.getWaitingApns();
    }

    /**
     * Save the state indicating concurrent voice/data allowed.
     * @param allowed True if concurrent voice/data is allowed
     */
    public synchronized void setConcurrentVoiceAndDataAllowed(boolean allowed) {
        mConcurrentVoiceAndDataAllowed = allowed;
    }

    /**
     * Get the state indicating concurrent voice/data allowed.
     * @return True if concurrent voice/data is allowed
     */
    public synchronized boolean isConcurrentVoiceAndDataAllowed() {
        return mConcurrentVoiceAndDataAllowed;
    }

    /**
     * Set the current data call state.
     * @param s Current data call state
     */
    public synchronized void setState(DctConstants.State s) {
        log("setState: " + s + ", previous state:" + mState);

        if (mState != s) {
            mStateLocalLog.log("State changed from " + mState + " to " + s);
            mState = s;
        }

        if (mState == DctConstants.State.FAILED) {
            // when teardown the connection and set to IDLE
            mRetryManager.getWaitingApns().clear();
        }
    }

    /**
     * Get the current data call state.
     * @return The current data call state
     */
    public synchronized DctConstants.State getState() {
        return mState;
    }

    /**
     * Check whether the data call is disconnected or not.
     * @return True if the data call is disconnected
     */
    public boolean isDisconnected() {
        DctConstants.State currentState = getState();
        return ((currentState == DctConstants.State.IDLE) ||
                    currentState == DctConstants.State.FAILED);
    }

    /**
     * Set the reason for data call connection.
     * @param reason Reason for data call connection
     */
    public synchronized void setReason(String reason) {
        log("set reason as " + reason + ",current state " + mState);
        mReason = reason;
    }

    /**
     * Get the reason for data call connection.
     * @return The reason for data call connection
     */
    public synchronized String getReason() {
        return mReason;
    }

    /**
     * Check if ready for data call connection
     * @return True if ready, otherwise false.
     */
    public boolean isReady() {
        return mDataEnabled.get() && isDependencyMet();
    }

    /**
     * Check if the data call is in the state which allow connecting.
     * @return True if allowed, otherwise false.
     */
    public boolean isConnectable() {
        return isReady() && ((mState == DctConstants.State.IDLE)
                                || (mState == DctConstants.State.RETRYING)
                                || (mState == DctConstants.State.FAILED));
    }

    /**
     * Check if apn reason is fast retry reason which should apply shorter delay between apn re-try.
     * @return True if it is fast retry reason, otherwise false.
     */
    private boolean isFastRetryReason() {
        return Phone.REASON_NW_TYPE_CHANGED.equals(mReason) ||
                Phone.REASON_APN_CHANGED.equals(mReason);
    }

    /** Check if the data call is in connected or connecting state.
     * @return True if the data call is in connected or connecting state
     */
    public boolean isConnectedOrConnecting() {
        return isReady() && ((mState == DctConstants.State.CONNECTED)
                                || (mState == DctConstants.State.CONNECTING)
                                || (mState == DctConstants.State.RETRYING));
    }

    /**
     * Set data call enabled/disabled state.
     * @param enabled True if data call is enabled
     */
    public void setEnabled(boolean enabled) {
        log("set enabled as " + enabled + ", current state is " + mDataEnabled.get());
        mDataEnabled.set(enabled);
    }

    /**
     * Check if the data call is enabled or not.
     * @return True if enabled
     */
    public boolean isEnabled() {
        return mDataEnabled.get();
    }

    public boolean isProvisioningApn() {
        String provisioningApn = mPhone.getContext().getResources()
                .getString(R.string.mobile_provisioning_apn);
        if (!TextUtils.isEmpty(provisioningApn) &&
                (mApnSetting != null) && (mApnSetting.getApnName() != null)) {
            return (mApnSetting.getApnName().equals(provisioningApn));
        } else {
            return false;
        }
    }

    private final ArraySet<NetworkRequest> mNetworkRequests = new ArraySet<>();
    private final LocalLog mStateLocalLog = new LocalLog(32);

    private static final LocalLog sLocalLog = new LocalLog(256);

    /** Add a line to the ApnContext local log. */
    public static void requestLog(ApnContext apnContext, String str) {
        if (apnContext != null) {
            String logString = "[ApnContext:" + apnContext.getApnType() + "] " + str;
            if (DBG) {
                Rlog.d(SLOG_TAG, logString);
            }
            synchronized (sLocalLog) {
                sLocalLog.log(logString);
            }
        }
    }

    /**
     * Request a network
     *
     * @param networkRequest Network request from clients
     * @param type The request type
     * @param onHandoverCompleteMsg When request type is handover, this message will be sent when
     * handover is completed. For normal request, this should be null.
     */
    public void requestNetwork(NetworkRequest networkRequest, @RequestNetworkType int type,
            Message onHandoverCompleteMsg) {
        synchronized (mRefCountLock) {
            mNetworkRequests.add(networkRequest);
            requestLog(this, "requestNetwork for " + networkRequest + ", type="
                    + DcTracker.requestTypeToString(type));
            mDcTracker.enableApn(ApnSetting.getApnTypesBitmaskFromString(mApnType), type,
                    onHandoverCompleteMsg);
            if (mDataConnection != null) {
                // New network request added. Should re-evaluate properties of
                // the data connection. For example, the score may change.
                mDataConnection.reevaluateDataConnectionProperties();
            }
        }
    }

    public void releaseNetwork(NetworkRequest networkRequest, @ReleaseNetworkType int type) {
        synchronized (mRefCountLock) {
            if (mNetworkRequests.contains(networkRequest)) {
                mNetworkRequests.remove(networkRequest);
                if (mDataConnection != null) {
                    // New network request added. Should re-evaluate properties of
                    // the data connection. For example, the score may change.
                    mDataConnection.reevaluateDataConnectionProperties();
                }
                requestLog(this, "releaseNetwork left with " + mNetworkRequests.size()
                        + " requests.");
                if (mNetworkRequests.size() == 0
                        || type == DcTracker.RELEASE_TYPE_DETACH
                        || type == DcTracker.RELEASE_TYPE_HANDOVER) {
                    mDcTracker.disableApn(ApnSetting.getApnTypesBitmaskFromString(mApnType), type);
                }
            }
        }
    }

    /**
     * @param excludeDun True if excluding requests that have DUN capability
     * @return True if the attached network requests contain restricted capability.
     */
    public boolean hasRestrictedRequests(boolean excludeDun) {
        synchronized (mRefCountLock) {
            for (NetworkRequest nr : mNetworkRequests) {
                if (excludeDun &&
                        nr.hasCapability(NetworkCapabilities.NET_CAPABILITY_DUN)) {
                    continue;
                }
                if (!nr.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)) {
                    return true;
                }
            }
        }
        return false;
    }

    private final SparseIntArray mRetriesLeftPerErrorCode = new SparseIntArray();

    public void resetErrorCodeRetries() {
        requestLog(this, "resetErrorCodeRetries");

        String[] config = mPhone.getContext().getResources().getStringArray(
                com.android.internal.R.array.config_cell_retries_per_error_code);
        synchronized (mRetriesLeftPerErrorCode) {
            mRetriesLeftPerErrorCode.clear();

            for (String c : config) {
                String errorValue[] = c.split(",");
                if (errorValue != null && errorValue.length == 2) {
                    int count = 0;
                    int errorCode = 0;
                    try {
                        errorCode = Integer.parseInt(errorValue[0]);
                        count = Integer.parseInt(errorValue[1]);
                    } catch (NumberFormatException e) {
                        log("Exception parsing config_retries_per_error_code: " + e);
                        continue;
                    }
                    if (count > 0 && errorCode > 0) {
                        mRetriesLeftPerErrorCode.put(errorCode, count);
                    }
                } else {
                    log("Exception parsing config_retries_per_error_code: " + c);
                }
            }
        }
    }

    public boolean restartOnError(int errorCode) {
        boolean result = false;
        int retriesLeft = 0;
        synchronized(mRetriesLeftPerErrorCode) {
            retriesLeft = mRetriesLeftPerErrorCode.get(errorCode);
            switch (retriesLeft) {
                case 0: {
                    // not set, never restart modem
                    break;
                }
                case 1: {
                    resetErrorCodeRetries();
                    result = true;
                    break;
                }
                default: {
                    mRetriesLeftPerErrorCode.put(errorCode, retriesLeft - 1);
                    result = false;
                }
            }
        }
        requestLog(this, "restartOnError(" + errorCode + ") found " + retriesLeft
                + " and returned " + result);
        return result;
    }

    public int incAndGetConnectionGeneration() {
        return mConnectionGeneration.incrementAndGet();
    }

    public int getConnectionGeneration() {
        return mConnectionGeneration.get();
    }

    long getRetryAfterDisconnectDelay() {
        return mRetryManager.getRetryAfterDisconnectDelay();
    }

    /**
     * Get APN type from the network request.
     *
     * @param nr The network request.
     * @return The APN type.
     */
    public static @ApnType int getApnTypeFromNetworkRequest(NetworkRequest nr) {
        // For now, ignore the bandwidth stuff
        if (nr.getTransportTypes().length > 0
                && !nr.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return ApnSetting.TYPE_NONE;
        }

        // in the near term just do 1-1 matches.
        // TODO - actually try to match the set of capabilities
        int apnType = ApnSetting.TYPE_NONE;
        boolean error = false;

        if (nr.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            apnType = ApnSetting.TYPE_DEFAULT;
        }
        if (nr.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMS)) {
            if (apnType != ApnSetting.TYPE_NONE) error = true;
            apnType = ApnSetting.TYPE_MMS;
        }
        if (nr.hasCapability(NetworkCapabilities.NET_CAPABILITY_SUPL)) {
            if (apnType != ApnSetting.TYPE_NONE) error = true;
            apnType = ApnSetting.TYPE_SUPL;
        }
        if (nr.hasCapability(NetworkCapabilities.NET_CAPABILITY_DUN)) {
            if (apnType != ApnSetting.TYPE_NONE) error = true;
            apnType = ApnSetting.TYPE_DUN;
        }
        if (nr.hasCapability(NetworkCapabilities.NET_CAPABILITY_FOTA)) {
            if (apnType != ApnSetting.TYPE_NONE) error = true;
            apnType = ApnSetting.TYPE_FOTA;
        }
        if (nr.hasCapability(NetworkCapabilities.NET_CAPABILITY_IMS)) {
            if (apnType != ApnSetting.TYPE_NONE) error = true;
            apnType = ApnSetting.TYPE_IMS;
        }
        if (nr.hasCapability(NetworkCapabilities.NET_CAPABILITY_CBS)) {
            if (apnType != ApnSetting.TYPE_NONE) error = true;
            apnType = ApnSetting.TYPE_CBS;
        }
        if (nr.hasCapability(NetworkCapabilities.NET_CAPABILITY_IA)) {
            if (apnType != ApnSetting.TYPE_NONE) error = true;
            apnType = ApnSetting.TYPE_IA;
        }
        if (nr.hasCapability(NetworkCapabilities.NET_CAPABILITY_EIMS)) {
            if (apnType != ApnSetting.TYPE_NONE) error = true;
            apnType = ApnSetting.TYPE_EMERGENCY;
        }
        if (nr.hasCapability(NetworkCapabilities.NET_CAPABILITY_MCX)) {
            if (apnType != ApnSetting.TYPE_NONE) error = true;
            apnType = ApnSetting.TYPE_MCX;
        }
        if (nr.hasCapability(NetworkCapabilities.NET_CAPABILITY_XCAP)) {
            if (apnType != ApnSetting.TYPE_NONE) error = true;
            apnType = ApnSetting.TYPE_XCAP;
        }
        if (nr.hasCapability(NetworkCapabilities.NET_CAPABILITY_ENTERPRISE)) {
            if (apnType != ApnSetting.TYPE_NONE) error = true;
            apnType = ApnSetting.TYPE_ENTERPRISE;
        }
        if (error) {
            // TODO: If this error condition is removed, the framework's handling of
            // NET_CAPABILITY_NOT_RESTRICTED will need to be updated so requests for
            // say FOTA and INTERNET are marked as restricted.  This is not how
            // NetworkCapabilities.maybeMarkCapabilitiesRestricted currently works.
            Rlog.d(SLOG_TAG, "Multiple apn types specified in request - result is unspecified!");
        }
        if (apnType == ApnSetting.TYPE_NONE) {
            Rlog.d(SLOG_TAG, "Unsupported NetworkRequest in Telephony: nr=" + nr);
        }
        return apnType;
    }

    public List<NetworkRequest> getNetworkRequests() {
        synchronized (mRefCountLock) {
            return new ArrayList<NetworkRequest>(mNetworkRequests);
        }
    }

    @Override
    public synchronized String toString() {
        // We don't print mDataConnection because its recursive.
        return "{mApnType=" + mApnType + " mState=" + getState() + " mWaitingApns={"
                    + mRetryManager.getWaitingApns() + " priority=" + mPriority + "}"
                    + " mApnSetting={" + mApnSetting
                    + "} mReason=" + mReason + " mDataEnabled=" + mDataEnabled + "}";
    }

    private void log(String s) {
        if (DBG) {
            Rlog.d(LOG_TAG, "[ApnContext:" + mApnType + "] " + s);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
        synchronized (mRefCountLock) {
            pw.println(toString());
            if (mNetworkRequests.size() > 0) {
                pw.println("NetworkRequests:");
                pw.increaseIndent();
                for (NetworkRequest nr : mNetworkRequests) {
                    pw.println(nr);
                }
                pw.decreaseIndent();
            }
            pw.println("Historical APN state:");
            pw.increaseIndent();
            mStateLocalLog.dump(fd, pw, args);
            pw.decreaseIndent();
            pw.println(mRetryManager);
            pw.println("--------------------------");
        }
    }

    /** Dumps the ApnContext local log. */
    public static void dumpLocalLog(FileDescriptor fd, PrintWriter printWriter, String[] args) {
        printWriter.println("Local log:");
        synchronized (sLocalLog) {
            sLocalLog.dump(fd, printWriter, args);
        }
    }
}
