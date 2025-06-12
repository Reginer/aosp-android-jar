/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.vibrator;

import static com.android.server.vibrator.VibrationSession.DebugInfo.formatTime;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.media.AudioAttributes;
import android.os.CancellationSignal;
import android.os.CombinedVibration;
import android.os.ExternalVibration;
import android.os.Handler;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.VibrationAttributes;
import android.os.vibrator.IVibrationSession;
import android.os.vibrator.IVibrationSessionCallback;
import android.util.IndentingPrintWriter;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * A vibration session started by a vendor request that can trigger {@link CombinedVibration}.
 */
final class VendorVibrationSession extends IVibrationSession.Stub
        implements VibrationSession, CancellationSignal.OnCancelListener, IBinder.DeathRecipient {
    private static final String TAG = "VendorVibrationSession";
    // To enable these logs, run:
    // 'adb shell setprop persist.log.tag.VendorVibrationSession DEBUG && adb reboot'
    private static final boolean DEBUG = VibratorDebugUtils.isDebuggable(TAG);

    /** Calls into VibratorManager functionality needed for playing an {@link ExternalVibration}. */
    interface VibratorManagerHooks {

        /** Tells the manager to end the vibration session. */
        void endSession(long sessionId, boolean shouldAbort);

        /**
         * Tells the manager that the vibration session is finished and the vibrators can now be
         * used for another vibration.
         */
        void onSessionReleased(long sessionId);

        /** Request the manager to trigger a vibration within this session. */
        void vibrate(long sessionId, CallerInfo callerInfo, CombinedVibration vibration);
    }

    private final Object mLock = new Object();
    private final long mSessionId = VibrationSession.nextSessionId();
    private final ICancellationSignal mCancellationSignal = CancellationSignal.createTransport();
    private final int[] mVibratorIds;
    private final long mCreateUptime;
    private final long mCreateTime;
    private final VendorCallbackWrapper mCallback;
    private final CallerInfo mCallerInfo;
    private final VibratorManagerHooks mManagerHooks;
    private final DeviceAdapter mDeviceAdapter;
    private final Handler mHandler;
    private final List<DebugInfo> mVibrations = new ArrayList<>();

    @GuardedBy("mLock")
    private Status mStatus = Status.RUNNING;
    @GuardedBy("mLock")
    private Status mEndStatusRequest;
    @GuardedBy("mLock")
    private boolean mEndedByVendor;
    @GuardedBy("mLock")
    private long mStartTime;
    @GuardedBy("mLock")
    private long mEndUptime;
    @GuardedBy("mLock")
    private long mEndTime;
    @GuardedBy("mLock")
    private VibrationStepConductor mConductor;

    VendorVibrationSession(@NonNull CallerInfo callerInfo, @NonNull Handler handler,
            @NonNull VibratorManagerHooks managerHooks, @NonNull DeviceAdapter deviceAdapter,
            @NonNull IVibrationSessionCallback callback) {
        mCreateUptime = SystemClock.uptimeMillis();
        mCreateTime = System.currentTimeMillis();
        mVibratorIds = deviceAdapter.getAvailableVibratorIds();
        mHandler = handler;
        mCallback = new VendorCallbackWrapper(callback, handler);
        mCallerInfo = callerInfo;
        mManagerHooks = managerHooks;
        mDeviceAdapter = deviceAdapter;
        CancellationSignal.fromTransport(mCancellationSignal).setOnCancelListener(this);
    }

    @Override
    public void vibrate(CombinedVibration vibration, String reason) {
        CallerInfo vibrationCallerInfo = new CallerInfo(mCallerInfo.attrs, mCallerInfo.uid,
                mCallerInfo.deviceId, mCallerInfo.opPkg, reason);
        mManagerHooks.vibrate(mSessionId, vibrationCallerInfo, vibration);
    }

    @Override
    public void finishSession() {
        if (DEBUG) {
            Slog.d(TAG, "Session finish requested, ending vibration session...");
        }
        // Do not abort session in HAL, wait for ongoing vibration requests to complete.
        // This might take a while to end the session, but it can be aborted by cancelSession.
        requestEndSession(Status.FINISHED, /* shouldAbort= */ false, /* isVendorRequest= */ true);
    }

    @Override
    public void cancelSession() {
        if (DEBUG) {
            Slog.d(TAG, "Session cancel requested, aborting vibration session...");
        }
        // Always abort session in HAL while cancelling it.
        // This might be triggered after finishSession was already called.
        requestEndSession(Status.CANCELLED_BY_USER, /* shouldAbort= */ true,
                /* isVendorRequest= */ true);
    }

    @Override
    public long getSessionId() {
        return mSessionId;
    }

    @Override
    public long getCreateUptimeMillis() {
        return mCreateUptime;
    }

    @Override
    public boolean isRepeating() {
        return false;
    }

    @Override
    public CallerInfo getCallerInfo() {
        return mCallerInfo;
    }

    @Override
    public IBinder getCallerToken() {
        return mCallback.getBinderToken();
    }

    @Override
    public DebugInfo getDebugInfo() {
        synchronized (mLock) {
            return new DebugInfoImpl(mStatus, mCallerInfo, mCreateUptime, mCreateTime, mStartTime,
                    mEndUptime, mEndTime, mEndedByVendor, mVibrations);
        }
    }

    @Override
    public boolean wasEndRequested() {
        synchronized (mLock) {
            return mEndStatusRequest != null;
        }
    }

    @Override
    public void onCancel() {
        if (DEBUG) {
            Slog.d(TAG, "Session cancellation signal received, aborting vibration session...");
        }
        requestEndSession(Status.CANCELLED_BY_USER, /* shouldAbort= */ true,
                /* isVendorRequest= */ true);
    }

    @Override
    public void binderDied() {
        if (DEBUG) {
            Slog.d(TAG, "Session binder died, aborting vibration session...");
        }
        requestEndSession(Status.CANCELLED_BINDER_DIED, /* shouldAbort= */ true,
                /* isVendorRequest= */ false);
    }

    @Override
    public boolean linkToDeath() {
        return mCallback.linkToDeath(this);
    }

    @Override
    public void unlinkToDeath() {
        mCallback.unlinkToDeath(this);
    }

    @Override
    public void requestEnd(@NonNull Status status, @Nullable CallerInfo endedBy,
            boolean immediate) {
        // All requests to end a session should abort it to stop ongoing vibrations, even if
        // immediate flag is false. Only the #finishSession API will not abort and wait for
        // session vibrations to complete, which might take a long time.
        requestEndSession(status, /* shouldAbort= */ true, /* isVendorRequest= */ false);
    }

    @Override
    public void notifyVibratorCallback(int vibratorId, long vibrationId, long stepId) {
        if (DEBUG) {
            Slog.d(TAG, "Vibration callback received for vibration " + vibrationId
                    + " step " + stepId + " on vibrator " + vibratorId + ", ignoring...");
        }
    }

    @Override
    public void notifySyncedVibratorsCallback(long vibrationId) {
        if (DEBUG) {
            Slog.d(TAG, "Synced vibration callback received for vibration " + vibrationId
                    + ", ignoring...");
        }
    }

    @Override
    public void notifySessionCallback() {
        if (DEBUG) {
            Slog.d(TAG, "Session callback received, ending vibration session...");
        }
        synchronized (mLock) {
            // If end was not requested then the HAL has cancelled the session.
            notifyEndRequestLocked(Status.CANCELLED_BY_UNKNOWN_REASON,
                    /* isVendorRequest= */ false);
            maybeSetStatusToRequestedLocked();
            clearVibrationConductor();
            final Status endStatus = mStatus;
            mHandler.post(() -> {
                mManagerHooks.onSessionReleased(mSessionId);
                // Only trigger client callback after session is released in the manager.
                mCallback.notifyFinished(endStatus);
            });
        }
    }

    @Override
    public String toString() {
        synchronized (mLock) {
            return "createTime: " + formatTime(mCreateTime, /*includeDate=*/ true)
                    + ", startTime: " + (mStartTime == 0 ? null : formatTime(mStartTime,
                    /* includeDate= */ true))
                    + ", endTime: " + (mEndTime == 0 ? null : formatTime(mEndTime,
                    /* includeDate= */ true))
                    + ", status: " + mStatus.name().toLowerCase(Locale.ROOT)
                    + ", callerInfo: " + mCallerInfo
                    + ", vibratorIds: " + Arrays.toString(mVibratorIds)
                    + ", vibrations: " + mVibrations;
        }
    }

    public Status getStatus() {
        synchronized (mLock) {
            return mStatus;
        }
    }

    public boolean isStarted() {
        synchronized (mLock) {
            return mStartTime > 0;
        }
    }

    public boolean isEnded() {
        synchronized (mLock) {
            return mEndTime > 0;
        }
    }

    public int[] getVibratorIds() {
        return mVibratorIds;
    }

    @VisibleForTesting
    public List<DebugInfo> getVibrations() {
        synchronized (mLock) {
            return new ArrayList<>(mVibrations);
        }
    }

    public ICancellationSignal getCancellationSignal() {
        return mCancellationSignal;
    }

    public void notifyStart() {
        boolean isAlreadyEnded = false;
        synchronized (mLock) {
            if (isEnded()) {
                // Session already ended, skip start callbacks.
                isAlreadyEnded = true;
            } else {
                if (DEBUG) {
                    Slog.d(TAG, "Session started at the HAL");
                }
                mStartTime = System.currentTimeMillis();
                mCallback.notifyStarted(this);
            }
        }
        if (isAlreadyEnded) {
            if (DEBUG) {
                Slog.d(TAG, "Session already ended after starting the HAL, aborting...");
            }
            mHandler.post(() -> mManagerHooks.endSession(mSessionId, /* shouldAbort= */ true));
        }
    }

    public void notifyVibrationAttempt(DebugInfo vibrationDebugInfo) {
        mVibrations.add(vibrationDebugInfo);
    }

    @Nullable
    public VibrationStepConductor clearVibrationConductor() {
        synchronized (mLock) {
            VibrationStepConductor conductor = mConductor;
            if (conductor != null) {
                mVibrations.add(conductor.getVibration().getDebugInfo());
            }
            mConductor = null;
            return conductor;
        }
    }

    public DeviceAdapter getDeviceAdapter() {
        return mDeviceAdapter;
    }

    public boolean maybeSetVibrationConductor(VibrationStepConductor conductor) {
        synchronized (mLock) {
            if (mConductor != null) {
                if (DEBUG) {
                    Slog.d(TAG, "Session still dispatching previous vibration, new vibration "
                            + conductor.getVibration().id + " ignored");
                }
                return false;
            }
            mConductor = conductor;
            return true;
        }
    }

    private void requestEndSession(Status status, boolean shouldAbort, boolean isVendorRequest) {
        if (DEBUG) {
            Slog.d(TAG, "Session end request received with status " + status);
        }
        synchronized (mLock) {
            notifyEndRequestLocked(status, isVendorRequest);
            if (!isEnded() && isStarted()) {
                // Trigger session hook even if it was already triggered, in case a second request
                // is aborting the ongoing/ending session. This might cause it to end right away.
                // Wait for HAL callback before setting the end status.
                if (DEBUG) {
                    Slog.d(TAG, "Requesting HAL session end with abort=" + shouldAbort);
                }
                mHandler.post(() ->  mManagerHooks.endSession(mSessionId, shouldAbort));
            } else {
                // Session not active in the HAL, try to set end status right away.
                maybeSetStatusToRequestedLocked();
                // Use status used to end this session, which might be different from requested.
                mCallback.notifyFinished(mStatus);
            }
        }
    }

    @GuardedBy("mLock")
    private void notifyEndRequestLocked(Status status, boolean isVendorRequest) {
        if (mEndStatusRequest != null) {
            // End already requested, keep first requested status.
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "Session end request accepted for status " + status);
        }
        mEndStatusRequest = status;
        mEndedByVendor = isVendorRequest;
        mCallback.notifyFinishing();
        if (mConductor != null) {
            // Vibration is being dispatched when session end was requested, cancel it.
            mConductor.notifyCancelled(new Vibration.EndInfo(status),
                    /* immediate= */ status != Status.FINISHED);
        }
    }

    @GuardedBy("mLock")
    private void maybeSetStatusToRequestedLocked() {
        if (isEnded()) {
            // End already set, keep first requested status and time.
            return;
        }
        if (mEndStatusRequest == null) {
            // No end status was requested, nothing to set.
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "Session end request applied for status " + mEndStatusRequest);
        }
        mStatus = mEndStatusRequest;
        mEndTime = System.currentTimeMillis();
        mEndUptime = SystemClock.uptimeMillis();
    }

    /**
     * Wrapper class to handle client callbacks asynchronously.
     *
     * <p>This class is also responsible for link/unlink to the client process binder death, and for
     * making sure the callbacks are only triggered once. The conversion between session status and
     * the API status code is also defined here.
     */
    private static final class VendorCallbackWrapper {
        private final IVibrationSessionCallback mCallback;
        private final Handler mHandler;

        private boolean mIsStarted;
        private boolean mIsFinishing;
        private boolean mIsFinished;

        VendorCallbackWrapper(@NonNull IVibrationSessionCallback callback,
                @NonNull Handler handler) {
            mCallback = callback;
            mHandler = handler;
        }

        synchronized IBinder getBinderToken() {
            return mCallback.asBinder();
        }

        synchronized boolean linkToDeath(DeathRecipient recipient) {
            try {
                mCallback.asBinder().linkToDeath(recipient, 0);
            } catch (RemoteException e) {
                Slog.e(TAG, "Error linking session to token death", e);
                return false;
            }
            return true;
        }

        synchronized void unlinkToDeath(DeathRecipient recipient) {
            try {
                mCallback.asBinder().unlinkToDeath(recipient, 0);
            } catch (NoSuchElementException e) {
                Slog.wtf(TAG, "Failed to unlink session to token death", e);
            }
        }

        synchronized void notifyStarted(IVibrationSession session) {
            if (mIsStarted) {
                return;
            }
            mIsStarted = true;
            mHandler.post(() -> {
                try {
                    mCallback.onStarted(session);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error notifying vendor session started", e);
                }
            });
        }

        synchronized void notifyFinishing() {
            if (!mIsStarted || mIsFinishing || mIsFinished) {
                // Ignore if never started or if already finishing or finished.
                return;
            }
            mIsFinishing = true;
            mHandler.post(() -> {
                try {
                    mCallback.onFinishing();
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error notifying vendor session is finishing", e);
                }
            });
        }

        synchronized void notifyFinished(Status status) {
            if (mIsFinished) {
                return;
            }
            mIsFinished = true;
            mHandler.post(() -> {
                try {
                    mCallback.onFinished(toSessionStatus(status));
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error notifying vendor session finished", e);
                }
            });
        }

        @android.os.vibrator.VendorVibrationSession.Status
        private static int toSessionStatus(Status status) {
            // Exhaustive switch to cover all possible internal status.
            return switch (status) {
                case FINISHED
                        -> android.os.vibrator.VendorVibrationSession.STATUS_SUCCESS;
                case IGNORED_UNSUPPORTED
                        -> STATUS_UNSUPPORTED;
                case CANCELLED_BINDER_DIED, CANCELLED_BY_APP_OPS, CANCELLED_BY_USER,
                     CANCELLED_SUPERSEDED, CANCELLED_BY_FOREGROUND_USER, CANCELLED_BY_SCREEN_OFF,
                     CANCELLED_BY_SETTINGS_UPDATE, CANCELLED_BY_UNKNOWN_REASON
                        -> android.os.vibrator.VendorVibrationSession.STATUS_CANCELED;
                case IGNORED_APP_OPS, IGNORED_BACKGROUND, IGNORED_FOR_EXTERNAL, IGNORED_FOR_ONGOING,
                     IGNORED_FOR_POWER, IGNORED_FOR_SETTINGS, IGNORED_FOR_HIGHER_IMPORTANCE,
                     IGNORED_FOR_RINGER_MODE, IGNORED_FROM_VIRTUAL_DEVICE, IGNORED_SUPERSEDED,
                     IGNORED_MISSING_PERMISSION, IGNORED_ON_WIRELESS_CHARGER
                        -> android.os.vibrator.VendorVibrationSession.STATUS_IGNORED;
                case UNKNOWN, IGNORED_ERROR_APP_OPS, IGNORED_ERROR_CANCELLING,
                     IGNORED_ERROR_SCHEDULING, IGNORED_ERROR_TOKEN, FORWARDED_TO_INPUT_DEVICES,
                     FINISHED_UNEXPECTED, RUNNING
                        -> android.os.vibrator.VendorVibrationSession.STATUS_UNKNOWN_ERROR;
            };
        }
    }

    /**
     * Holds lightweight debug information about the session that could potentially be kept in
     * memory for a long time for bugreport dumpsys operations.
     *
     * Since DebugInfo can be kept in memory for a long time, it shouldn't hold any references to
     * potentially expensive or resource-linked objects, such as {@link IBinder}.
     */
    static final class DebugInfoImpl implements VibrationSession.DebugInfo {
        private final Status mStatus;
        private final CallerInfo mCallerInfo;
        private final List<DebugInfo> mVibrations;

        private final long mCreateUptime;
        private final long mCreateTime;
        private final long mStartTime;
        private final long mEndTime;
        private final long mDurationMs;
        private final boolean mEndedByVendor;

        DebugInfoImpl(Status status, CallerInfo callerInfo, long createUptime, long createTime,
                long startTime, long endUptime, long endTime, boolean endedByVendor,
                List<DebugInfo> vibrations) {
            mStatus = status;
            mCallerInfo = callerInfo;
            mCreateUptime = createUptime;
            mCreateTime = createTime;
            mStartTime = startTime;
            mEndTime = endTime;
            mEndedByVendor = endedByVendor;
            mDurationMs = endUptime > 0 ? endUptime - createUptime : -1;
            mVibrations = vibrations == null ? new ArrayList<>() : new ArrayList<>(vibrations);
        }

        @Override
        public Status getStatus() {
            return mStatus;
        }

        @Override
        public long getCreateUptimeMillis() {
            return mCreateUptime;
        }

        @Override
        public CallerInfo getCallerInfo() {
            return mCallerInfo;
        }

        @Nullable
        @Override
        public Object getDumpAggregationKey() {
            return null; // No aggregation.
        }

        @Override
        public void logMetrics(VibratorFrameworkStatsLogger statsLogger) {
            if (mStartTime > 0) {
                // Only log sessions that have started in the HAL.
                statsLogger.logVibrationVendorSessionStarted(mCallerInfo.uid);
                statsLogger.logVibrationVendorSessionVibrations(mCallerInfo.uid,
                        mVibrations.size());
                if (!mEndedByVendor) {
                    statsLogger.logVibrationVendorSessionInterrupted(mCallerInfo.uid);
                }
            }
            for (DebugInfo vibration : mVibrations) {
                vibration.logMetrics(statsLogger);
            }
        }

        @Override
        public void dump(ProtoOutputStream proto, long fieldId) {
            final long token = proto.start(fieldId);
            proto.write(VibrationProto.END_TIME, mEndTime);
            proto.write(VibrationProto.DURATION_MS, mDurationMs);
            proto.write(VibrationProto.STATUS, mStatus.ordinal());

            final long attrsToken = proto.start(VibrationProto.ATTRIBUTES);
            final VibrationAttributes attrs = mCallerInfo.attrs;
            proto.write(VibrationAttributesProto.USAGE, attrs.getUsage());
            proto.write(VibrationAttributesProto.AUDIO_USAGE, attrs.getAudioUsage());
            proto.write(VibrationAttributesProto.FLAGS, attrs.getFlags());
            proto.end(attrsToken);

            proto.end(token);
        }

        @Override
        public void dump(IndentingPrintWriter pw) {
            pw.println("VibrationSession:");
            pw.increaseIndent();
            pw.println("status = " + mStatus.name().toLowerCase(Locale.ROOT));
            pw.println("durationMs = " + mDurationMs);
            pw.println("createTime = " + formatTime(mCreateTime, /*includeDate=*/ true));
            pw.println("startTime = " + formatTime(mStartTime, /*includeDate=*/ true));
            pw.println("endTime = " + (mEndTime == 0 ? null
                    : formatTime(mEndTime, /*includeDate=*/ true)));
            pw.println("callerInfo = " + mCallerInfo);

            pw.println("vibrations:");
            pw.increaseIndent();
            for (DebugInfo vibration : mVibrations) {
                vibration.dump(pw);
            }
            pw.decreaseIndent();

            pw.decreaseIndent();
        }

        @Override
        public void dumpCompact(IndentingPrintWriter pw) {
            // Follow pattern from Vibration.DebugInfoImpl for better debugging from dumpsys.
            String timingsStr = String.format(Locale.ROOT,
                    "%s | %8s | %20s | duration: %5dms | start: %12s | end: %12s",
                    formatTime(mCreateTime, /*includeDate=*/ true),
                    "session",
                    mStatus.name().toLowerCase(Locale.ROOT),
                    mDurationMs,
                    mStartTime == 0 ? "" : formatTime(mStartTime, /*includeDate=*/ false),
                    mEndTime == 0 ? "" : formatTime(mEndTime, /*includeDate=*/ false));
            String paramStr = String.format(Locale.ROOT,
                    " | flags: %4s | usage: %s",
                    Long.toBinaryString(mCallerInfo.attrs.getFlags()),
                    mCallerInfo.attrs.usageToString());
            // Optional, most vibrations should not be defined via AudioAttributes
            // so skip them to simplify the logs
            String audioUsageStr =
                    mCallerInfo.attrs.getOriginalAudioUsage() != AudioAttributes.USAGE_UNKNOWN
                            ? " | audioUsage=" + AudioAttributes.usageToString(
                            mCallerInfo.attrs.getOriginalAudioUsage())
                            : "";
            String callerStr = String.format(Locale.ROOT,
                    " | %s (uid=%d, deviceId=%d) | reason: %s",
                    mCallerInfo.opPkg, mCallerInfo.uid, mCallerInfo.deviceId, mCallerInfo.reason);
            pw.println(timingsStr + paramStr + audioUsageStr + callerStr);

            pw.increaseIndent();
            for (DebugInfo vibration : mVibrations) {
                vibration.dumpCompact(pw);
            }
            pw.decreaseIndent();
        }

        @Override
        public String toString() {
            return "createTime: " + formatTime(mCreateTime, /* includeDate= */ true)
                    + ", startTime: " + formatTime(mStartTime, /* includeDate= */ true)
                    + ", endTime: " + (mEndTime == 0 ? null : formatTime(mEndTime,
                    /* includeDate= */ true))
                    + ", durationMs: " + mDurationMs
                    + ", status: " + mStatus.name().toLowerCase(Locale.ROOT)
                    + ", callerInfo: " + mCallerInfo
                    + ", vibrations: " + mVibrations;
        }
    }
}
