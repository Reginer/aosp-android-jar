/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.internal.os.anr;

import static android.os.Trace.TRACE_TAG_ACTIVITY_MANAGER;

import static com.android.internal.os.TimeoutRecord.TimeoutKind;
import static com.android.internal.util.FrameworkStatsLog.ANRLATENCY_REPORTED__ANR_TYPE__BROADCAST_OF_INTENT;
import static com.android.internal.util.FrameworkStatsLog.ANRLATENCY_REPORTED__ANR_TYPE__CONTENT_PROVIDER_NOT_RESPONDING;
import static com.android.internal.util.FrameworkStatsLog.ANRLATENCY_REPORTED__ANR_TYPE__EXECUTING_SERVICE;
import static com.android.internal.util.FrameworkStatsLog.ANRLATENCY_REPORTED__ANR_TYPE__INPUT_DISPATCHING_TIMEOUT;
import static com.android.internal.util.FrameworkStatsLog.ANRLATENCY_REPORTED__ANR_TYPE__INPUT_DISPATCHING_TIMEOUT_NO_FOCUSED_WINDOW;
import static com.android.internal.util.FrameworkStatsLog.ANRLATENCY_REPORTED__ANR_TYPE__JOB_SERVICE;
import static com.android.internal.util.FrameworkStatsLog.ANRLATENCY_REPORTED__ANR_TYPE__SHORT_FGS_TIMEOUT;
import static com.android.internal.util.FrameworkStatsLog.ANRLATENCY_REPORTED__ANR_TYPE__START_FOREGROUND_SERVICE;
import static com.android.internal.util.FrameworkStatsLog.ANRLATENCY_REPORTED__ANR_TYPE__UNKNOWN_ANR_TYPE;

import android.annotation.IntDef;
import android.os.SystemClock;
import android.os.Trace;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FrameworkStatsLog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Store different latencies from the ANR flow and trace functions, it records latency breakdown
 * for key methods, lock acquisition and other potentially expensive operations in the ANR
 * reporting flow and exports the data as comma separated text on calling
 * dumpAsCommaSeparatedArrayWithHeader and as an atom to statsd on being closed.
 */
public class AnrLatencyTracker implements AutoCloseable {

    /** Status of the early dumped pid. */
    @IntDef(value = {
            EarlyDumpStatus.UNKNOWN,
            EarlyDumpStatus.SUCCEEDED,
            EarlyDumpStatus.FAILED_TO_CREATE_FILE,
            EarlyDumpStatus.TIMED_OUT
    })

    @Retention(RetentionPolicy.SOURCE)
    private @interface EarlyDumpStatus {
        int UNKNOWN = 1;
        int SUCCEEDED = 2;
        int FAILED_TO_CREATE_FILE = 3;
        int TIMED_OUT = 4;
    }

    private static final AtomicInteger sNextAnrRecordPlacedOnQueueCookieGenerator =
            new AtomicInteger();

    private long mAnrTriggerUptime;
    private long mEndUptime;

    private long mAppNotRespondingStartUptime;
    private long mAnrRecordPlacedOnQueueUptime;
    private long mAnrProcessingStartedUptime;
    private long mDumpStackTracesStartUptime;

    private long mUpdateCpuStatsNowLastCallUptime;
    private long mUpdateCpuStatsNowTotalLatency = 0;
    private long mCurrentPsiStateLastCallUptime;
    private long mCurrentPsiStateTotalLatency = 0;
    private long mProcessCpuTrackerMethodsLastCallUptime;
    private long mProcessCpuTrackerMethodsTotalLatency = 0;
    private long mCriticalEventLoglastCallUptime;
    private long mCriticalEventLogTotalLatency = 0;

    private long mGlobalLockLastTryAcquireStart;
    private long mGlobalLockTotalContention = 0;
    private long mPidLockLastTryAcquireStart;
    private long mPidLockTotalContention = 0;
    private long mAMSLockLastTryAcquireStart;
    private long mAMSLockTotalContention = 0;
    private long mProcLockLastTryAcquireStart;
    private long mProcLockTotalContention = 0;
    private long mAnrRecordLastTryAcquireStart;
    private long mAnrRecordLockTotalContention = 0;

    private int mAnrQueueSize;
    private int mAnrType;
    private final AtomicInteger mDumpedProcessesCount = new AtomicInteger(0);

    private volatile @EarlyDumpStatus int mEarlyDumpStatus =
            EarlyDumpStatus.UNKNOWN;
    private volatile long mTempFileDumpingStartUptime;
    private volatile long mTempFileDumpingDuration = 0;
    private long mCopyingFirstPidStartUptime;
    private long mCopyingFirstPidDuration = 0;
    private long mEarlyDumpRequestSubmissionUptime = 0;
    private long mEarlyDumpExecutorPidCount = 0;

    private long mFirstPidsDumpingStartUptime;
    private long mFirstPidsDumpingDuration = 0;
    private long mNativePidsDumpingStartUptime;
    private long mNativePidsDumpingDuration = 0;
    private long mExtraPidsDumpingStartUptime;
    private long mExtraPidsDumpingDuration = 0;

    private boolean mIsPushed = false;
    private boolean mIsSkipped = false;
    private boolean mCopyingFirstPidSucceeded = false;

    private long mPreDumpIfLockTooSlowStartUptime;
    private long mPreDumpIfLockTooSlowDuration = 0;
    private long mNotifyAppUnresponsiveStartUptime;
    private long mNotifyAppUnresponsiveDuration = 0;
    private long mNotifyWindowUnresponsiveStartUptime;
    private long mNotifyWindowUnresponsiveDuration = 0;

    private final int mAnrRecordPlacedOnQueueCookie =
            sNextAnrRecordPlacedOnQueueCookieGenerator.incrementAndGet();

    public AnrLatencyTracker(@TimeoutKind int timeoutKind, long anrTriggerUptime) {
        mAnrTriggerUptime = anrTriggerUptime;
        mAnrType = timeoutKindToAnrType(timeoutKind);

    }

    /** Records the start of AnrHelper#appNotResponding. */
    public void appNotRespondingStarted() {
        mAppNotRespondingStartUptime = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER,
                "AnrHelper#appNotResponding()");
    }

    /** Records the end of AnrHelper#appNotResponding. */
    public void appNotRespondingEnded() {
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /**
     * Records the number of processes we are currently early-dumping, this number includes the
     * current ANR's main process.
     */
    public void earlyDumpRequestSubmittedWithSize(int currentProcessedPidCount) {
        mEarlyDumpRequestSubmissionUptime = getUptimeMillis();
        mEarlyDumpExecutorPidCount = currentProcessedPidCount;
    }

    /** Records the placing of the AnrHelper.AnrRecord instance on the processing queue. */
    public void anrRecordPlacingOnQueueWithSize(int queueSize) {
        mAnrRecordPlacedOnQueueUptime = getUptimeMillis();
        Trace.asyncTraceBegin(TRACE_TAG_ACTIVITY_MANAGER,
                "anrRecordPlacedOnQueue", mAnrRecordPlacedOnQueueCookie);
        mAnrQueueSize = queueSize;
        // Since we are recording the anr record queue size after pushing the current
        // record, we need to increment the current queue size by 1
        Trace.traceCounter(TRACE_TAG_ACTIVITY_MANAGER, "anrRecordsQueueSize", queueSize + 1);
    }

    /** Records the start of ProcessErrorStateRecord#appNotResponding. */
    public void anrProcessingStarted() {
        mAnrProcessingStartedUptime = getUptimeMillis();
        Trace.asyncTraceEnd(TRACE_TAG_ACTIVITY_MANAGER,
                "anrRecordPlacedOnQueue", mAnrRecordPlacedOnQueueCookie);
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER,
                "anrProcessing");
    }

    /** Records the end of ProcessErrorStateRecord#appNotResponding, the tracker is closed here. */
    public void anrProcessingEnded() {
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
        close();
    }

    /** Records the start of StackTracesDumpHelper#dumpStackTraces. */
    public void dumpStackTracesStarted() {
        mDumpStackTracesStartUptime = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER,
                "dumpStackTraces()");
    }

    /** Records the end of StackTracesDumpHelper#dumpStackTraces. */
    public void dumpStackTracesEnded() {
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of ActivityManagerService#updateCpuStatsNow. */
    public void updateCpuStatsNowCalled() {
        mUpdateCpuStatsNowLastCallUptime = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "updateCpuStatsNow()");
    }

    /** Records the return of ActivityManagerService#updateCpuStatsNow. */
    public void updateCpuStatsNowReturned() {
        mUpdateCpuStatsNowTotalLatency +=
                getUptimeMillis() - mUpdateCpuStatsNowLastCallUptime;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of ResourcePressureUtil#currentPsiState. */
    public void currentPsiStateCalled() {
        mCurrentPsiStateLastCallUptime = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "currentPsiState()");
    }

    /** Records the return of ResourcePressureUtil#currentPsiState. */
    public void currentPsiStateReturned() {
        mCurrentPsiStateTotalLatency += getUptimeMillis() - mCurrentPsiStateLastCallUptime;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of ProcessCpuTracker methods. */
    public void processCpuTrackerMethodsCalled() {
        mProcessCpuTrackerMethodsLastCallUptime = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "processCpuTracker");
    }

    /** Records the return of ProcessCpuTracker methods. */
    public void processCpuTrackerMethodsReturned() {
        mProcessCpuTrackerMethodsTotalLatency +=
                getUptimeMillis() - mProcessCpuTrackerMethodsLastCallUptime;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of ANR headers dumping to file (subject and criticalEventSection). */
    public void criticalEventLogStarted() {
        mCriticalEventLoglastCallUptime = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "criticalEventLog");
    }

    /** Records the end of ANR headers dumping to file (subject and criticalEventSection). */
    public void criticalEventLogEnded() {
        mCriticalEventLogTotalLatency +=
                getUptimeMillis() - mCriticalEventLoglastCallUptime;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of native pid collection. */
    public void nativePidCollectionStarted() {
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "nativePidCollection");
    }

    /** Records the end of native pid collection. */
    public void nativePidCollectionEnded() {
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of pid dumping to file. */
    public void dumpingPidStarted(int pid) {
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "dumpingPid#" + pid);
    }

    /** Records the end of pid dumping to file. */
    public void dumpingPidEnded() {
        mDumpedProcessesCount.incrementAndGet();
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of first pids dumping to file. */
    public void dumpingFirstPidsStarted() {
        mFirstPidsDumpingStartUptime = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "dumpingFirstPids");
    }

    /** Records the end of first pids dumping to file. */
    public void dumpingFirstPidsEnded() {
        mFirstPidsDumpingDuration = getUptimeMillis() - mFirstPidsDumpingStartUptime;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }


    /** Records the start of the copying of the pre-dumped first pid. */
    public void copyingFirstPidStarted() {
        mCopyingFirstPidStartUptime = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "copyingFirstPid");
    }

    /** Records the end of the copying of the pre-dumped first pid. */
    public void copyingFirstPidEnded(boolean copySucceeded) {
        mCopyingFirstPidDuration = getUptimeMillis() - mCopyingFirstPidStartUptime;
        mCopyingFirstPidSucceeded = copySucceeded;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of pre-dumping. */
    public void dumpStackTracesTempFileStarted() {
        mTempFileDumpingStartUptime = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "dumpStackTracesTempFile");
    }

    /** Records the end of pre-dumping. */
    public void dumpStackTracesTempFileEnded() {
        mTempFileDumpingDuration = getUptimeMillis() - mTempFileDumpingStartUptime;
        if (mEarlyDumpStatus == EarlyDumpStatus.UNKNOWN) {
            mEarlyDumpStatus = EarlyDumpStatus.SUCCEEDED;
        }
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records file creation failure events in dumpStackTracesTempFile. */
    public void dumpStackTracesTempFileCreationFailed() {
        mEarlyDumpStatus = EarlyDumpStatus.FAILED_TO_CREATE_FILE;
        Trace.instant(TRACE_TAG_ACTIVITY_MANAGER, "dumpStackTracesTempFileCreationFailed");
    }

    /** Records timeout events in dumpStackTracesTempFile. */
    public void dumpStackTracesTempFileTimedOut() {
        mEarlyDumpStatus = EarlyDumpStatus.TIMED_OUT;
        Trace.instant(TRACE_TAG_ACTIVITY_MANAGER, "dumpStackTracesTempFileTimedOut");
    }

    /** Records the start of native pids dumping to file. */
    public void dumpingNativePidsStarted() {
        mNativePidsDumpingStartUptime = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "dumpingNativePids");
    }

    /** Records the end of native pids dumping to file . */
    public void dumpingNativePidsEnded() {
        mNativePidsDumpingDuration =  getUptimeMillis() - mNativePidsDumpingStartUptime;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of extra pids dumping to file. */
    public void dumpingExtraPidsStarted() {
        mExtraPidsDumpingStartUptime = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "dumpingExtraPids");
    }

    /** Records the end of extra pids dumping to file. */
    public void dumpingExtraPidsEnded() {
        mExtraPidsDumpingDuration =  getUptimeMillis() - mExtraPidsDumpingStartUptime;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of contention on ActivityManagerService.mGlobalLock. */
    public void waitingOnGlobalLockStarted() {
        mGlobalLockLastTryAcquireStart = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "globalLock");
    }

    /** Records the end of contention on ActivityManagerService.mGlobalLock. */
    public void waitingOnGlobalLockEnded() {
        mGlobalLockTotalContention += getUptimeMillis() - mGlobalLockLastTryAcquireStart;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of contention on ActivityManagerService.mPidsSelfLocked. */
    public void waitingOnPidLockStarted() {
        mPidLockLastTryAcquireStart = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "pidLockContention");
    }

    /** Records the end of contention on ActivityManagerService.mPidsSelfLocked. */
    public void waitingOnPidLockEnded() {
        mPidLockTotalContention += getUptimeMillis() - mPidLockLastTryAcquireStart;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of contention on ActivityManagerService. */
    public void waitingOnAMSLockStarted() {
        mAMSLockLastTryAcquireStart = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "AMSLockContention");
    }

    /** Records the end of contention on ActivityManagerService. */
    public void waitingOnAMSLockEnded() {
        mAMSLockTotalContention += getUptimeMillis() - mAMSLockLastTryAcquireStart;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of contention on ActivityManagerService.mProcLock. */
    public void waitingOnProcLockStarted() {
        mProcLockLastTryAcquireStart = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "procLockContention");
    }

    /** Records the start of contention on ActivityManagerService.mProcLock. */
    public void waitingOnProcLockEnded() {
        mProcLockTotalContention += getUptimeMillis() - mProcLockLastTryAcquireStart;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of contention on AnrHelper.mAnrRecords. */
    public void waitingOnAnrRecordLockStarted() {
        mAnrRecordLastTryAcquireStart = getUptimeMillis();
        Trace.traceBegin(TRACE_TAG_ACTIVITY_MANAGER, "anrRecordLockContention");
    }

    /** Records the end of contention on AnrHelper.mAnrRecords. */
    public void waitingOnAnrRecordLockEnded() {
        mAnrRecordLockTotalContention +=
                getUptimeMillis() - mAnrRecordLastTryAcquireStart;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Counts the number of records in the records queue when the ANR record is popped. */
    public void anrRecordsQueueSizeWhenPopped(int queueSize) {
        Trace.traceCounter(TRACE_TAG_ACTIVITY_MANAGER, "anrRecordsQueueSize", queueSize);
    }

    /** Records the start of AnrController#preDumpIfLockTooSlow. */
    public void preDumpIfLockTooSlowStarted() {
        mPreDumpIfLockTooSlowStartUptime = getUptimeMillis();
    }

    /** Records the end of AnrController#preDumpIfLockTooSlow. */
    public void preDumpIfLockTooSlowEnded() {
        mPreDumpIfLockTooSlowDuration +=
                getUptimeMillis() - mPreDumpIfLockTooSlowStartUptime;
    }

    /** Records a skipped ANR in ProcessErrorStateRecord#appNotResponding. */
    public void anrSkippedProcessErrorStateRecordAppNotResponding() {
        anrSkipped("appNotResponding");
    }

    /** Records a skipped ANR in StackTracesDumpHelper#dumpStackTraces. */
    public void anrSkippedDumpStackTraces() {
        anrSkipped("dumpStackTraces");
    }

    /** Records the start of AnrController#notifyAppUnresponsive. */
    public void notifyAppUnresponsiveStarted() {
        mNotifyAppUnresponsiveStartUptime = getUptimeMillis();
        Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "notifyAppUnresponsive()");
    }

    /** Records the end of AnrController#notifyAppUnresponsive. */
    public void notifyAppUnresponsiveEnded() {
        mNotifyAppUnresponsiveDuration = getUptimeMillis() - mNotifyAppUnresponsiveStartUptime;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /** Records the start of AnrController#notifyWindowUnresponsive. */
    public void notifyWindowUnresponsiveStarted() {
        mNotifyWindowUnresponsiveStartUptime = getUptimeMillis();
        Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "notifyWindowUnresponsive()");
    }

    /** Records the end of AnrController#notifyWindowUnresponsive. */
    public void notifyWindowUnresponsiveEnded() {
        mNotifyWindowUnresponsiveDuration = getUptimeMillis()
                - mNotifyWindowUnresponsiveStartUptime;
        Trace.traceEnd(TRACE_TAG_ACTIVITY_MANAGER);
    }

    /**
     * Returns latency data as a comma separated value string for inclusion in ANR report.
     */
    public String dumpAsCommaSeparatedArrayWithHeader() {
        return "DurationsV5: " + mAnrTriggerUptime
                /* triggering_to_app_not_responding_duration = */
                + "," + (mAppNotRespondingStartUptime -  mAnrTriggerUptime)
                /* app_not_responding_duration = */
                + "," + (mAnrRecordPlacedOnQueueUptime -  mAppNotRespondingStartUptime)
                /* anr_record_placed_on_queue_duration = */
                + "," + (mAnrProcessingStartedUptime - mAnrRecordPlacedOnQueueUptime)
                /* anr_processing_duration = */
                + "," + (mDumpStackTracesStartUptime - mAnrProcessingStartedUptime)

                /* update_cpu_stats_now_total_duration = */
                + "," + mUpdateCpuStatsNowTotalLatency
                /* current_psi_state_total_duration = */
                + "," + mCurrentPsiStateTotalLatency
                /* process_cpu_tracker_methods_total_duration = */
                + "," + mProcessCpuTrackerMethodsTotalLatency
                /* critical_event_log_duration = */
                + "," + mCriticalEventLogTotalLatency

                /* global_lock_total_contention = */
                + "," + mGlobalLockTotalContention
                /* pid_lock_total_contention = */
                + "," + mPidLockTotalContention
                /* ams_lock_total_contention = */
                + "," + mAMSLockTotalContention
                /* proc_lock_total_contention = */
                + "," + mProcLockTotalContention
                /* anr_record_lock_total_contention = */
                + "," + mAnrRecordLockTotalContention

                /* anr_queue_size_when_pushed = */
                + "," + mAnrQueueSize
                /* dump_stack_traces_io_time = */
                // We use copyingFirstPidUptime if we're dumping the durations list before the
                // first pids ie after copying the early dump stacks.
                + "," + ((mFirstPidsDumpingStartUptime > 0 ? mFirstPidsDumpingStartUptime
                        : mCopyingFirstPidStartUptime) - mDumpStackTracesStartUptime)
                /* temp_file_dump_duration = */
                + "," + mTempFileDumpingDuration
                /* temp_dump_request_on_queue_duration = */
                + "," + (mTempFileDumpingStartUptime - mEarlyDumpRequestSubmissionUptime)
                /* temp_dump_pid_count_when_pushed = */
                + "," + mEarlyDumpExecutorPidCount
                /* first_pid_copying_time = */
                + "," + mCopyingFirstPidDuration
                /* early_dump_status = */
                + "," + mEarlyDumpStatus
                /* copying_first_pid_succeeded = */
                + "," + (mCopyingFirstPidSucceeded ? 1 : 0)
                /* preDumpIfLockTooSlow_duration = */
                + "," + mPreDumpIfLockTooSlowDuration
                /* notifyAppUnresponsive_duration = */
                + "," + mNotifyAppUnresponsiveDuration
                /* notifyWindowUnresponsive_duration = */
                + "," + mNotifyWindowUnresponsiveDuration
                + "\n\n";

    }

    /**
     * Closes the ANR latency instance by writing the atom to statsd, this method is idempotent.
     */
    @Override
    public void close() {
        if (!mIsSkipped && !mIsPushed) {
            mEndUptime = getUptimeMillis();
            pushAtom();
            mIsPushed = true;
        }
    }

    private static int timeoutKindToAnrType(@TimeoutKind int timeoutKind) {
        switch (timeoutKind) {
            case TimeoutKind.INPUT_DISPATCH_NO_FOCUSED_WINDOW:
                return ANRLATENCY_REPORTED__ANR_TYPE__INPUT_DISPATCHING_TIMEOUT_NO_FOCUSED_WINDOW;
            case TimeoutKind.INPUT_DISPATCH_WINDOW_UNRESPONSIVE:
                return ANRLATENCY_REPORTED__ANR_TYPE__INPUT_DISPATCHING_TIMEOUT;
            case TimeoutKind.BROADCAST_RECEIVER:
                return ANRLATENCY_REPORTED__ANR_TYPE__BROADCAST_OF_INTENT;
            case TimeoutKind.SERVICE_START:
                return ANRLATENCY_REPORTED__ANR_TYPE__START_FOREGROUND_SERVICE;
            case TimeoutKind.SERVICE_EXEC:
                return ANRLATENCY_REPORTED__ANR_TYPE__EXECUTING_SERVICE;
            case TimeoutKind.CONTENT_PROVIDER:
                return ANRLATENCY_REPORTED__ANR_TYPE__CONTENT_PROVIDER_NOT_RESPONDING;
            case TimeoutKind.SHORT_FGS_TIMEOUT:
                return ANRLATENCY_REPORTED__ANR_TYPE__SHORT_FGS_TIMEOUT;
            case TimeoutKind.JOB_SERVICE:
                return ANRLATENCY_REPORTED__ANR_TYPE__JOB_SERVICE;
            default:
                return ANRLATENCY_REPORTED__ANR_TYPE__UNKNOWN_ANR_TYPE;
        }
    }

    /** @hide */
    @VisibleForTesting
    public long getUptimeMillis() {
        return SystemClock.uptimeMillis();
    }

    /** @hide */
    @VisibleForTesting
    public void pushAtom() {
        FrameworkStatsLog.write(
                FrameworkStatsLog.ANR_LATENCY_REPORTED,

            /* total_duration = */ mEndUptime - mAnrTriggerUptime,
            /* triggering_to_stack_dump_duration = */
                    mFirstPidsDumpingStartUptime - mAnrTriggerUptime,
            /* triggering_to_app_not_responding_duration = */
                    mAppNotRespondingStartUptime -  mAnrTriggerUptime,
            /* app_not_responding_duration = */
                    mAnrRecordPlacedOnQueueUptime - mAppNotRespondingStartUptime,
            /* anr_record_placed_on_queue_duration = */
                mAnrProcessingStartedUptime - mAnrRecordPlacedOnQueueUptime,
            /* anr_processing_duration = */
                mDumpStackTracesStartUptime - mAnrProcessingStartedUptime,
            /* dump_stack_traces_duration = */ mFirstPidsDumpingDuration
                + mNativePidsDumpingDuration
                + mExtraPidsDumpingDuration,

            /* update_cpu_stats_now_total_duration = */ mUpdateCpuStatsNowTotalLatency,
            /* current_psi_state_total_duration = */ mCurrentPsiStateTotalLatency,
            /* process_cpu_tracker_methods_total_duration = */
                mProcessCpuTrackerMethodsTotalLatency,
            /* critical_event_log_duration = */ mCriticalEventLogTotalLatency,

            /* global_lock_total_contention = */ mGlobalLockTotalContention,
            /* pid_lock_total_contention = */ mPidLockTotalContention,
            /* ams_lock_total_contention = */ mAMSLockTotalContention,
            /* proc_lock_total_contention = */ mProcLockTotalContention,
            /* anr_record_lock_total_contention = */ mAnrRecordLockTotalContention,

            /* anr_queue_size_when_pushed = */ mAnrQueueSize,
            /* anr_type = */ mAnrType,
            /* dumped_processes_count = */ mDumpedProcessesCount.get());
    }

    private void anrSkipped(String method) {
        Trace.instant(TRACE_TAG_ACTIVITY_MANAGER, "AnrSkipped@" + method);
        mIsSkipped = true;
    }
}
