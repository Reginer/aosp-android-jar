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

package com.android.server.wm;

import static android.os.Build.IS_USER;

import static com.android.server.wm.shell.TransitionTraceProto.MAGIC_NUMBER;
import static com.android.server.wm.shell.TransitionTraceProto.MAGIC_NUMBER_H;
import static com.android.server.wm.shell.TransitionTraceProto.MAGIC_NUMBER_L;
import static com.android.server.wm.shell.TransitionTraceProto.REAL_TO_ELAPSED_TIME_OFFSET_NANOS;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.util.proto.ProtoOutputStream;

import com.android.internal.util.TraceBuffer;
import com.android.server.wm.Transition.ChangeInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to collect and dump transition traces.
 */
class LegacyTransitionTracer implements TransitionTracer {

    private static final String LOG_TAG = "TransitionTracer";

    private static final int ALWAYS_ON_TRACING_CAPACITY = 15 * 1024; // 15 KB
    private static final int ACTIVE_TRACING_BUFFER_CAPACITY = 5000 * 1024; // 5 MB

    // This will be the size the proto output streams are initialized to.
    // Ideally this should fit most or all the proto objects we will create and be no bigger than
    // that to ensure to don't use excessive amounts of memory.
    private static final int CHUNK_SIZE = 64;

    static final String WINSCOPE_EXT = ".winscope";
    private static final String TRACE_FILE =
            "/data/misc/wmtrace/wm_transition_trace" + WINSCOPE_EXT;
    private static final long MAGIC_NUMBER_VALUE = ((long) MAGIC_NUMBER_H << 32) | MAGIC_NUMBER_L;

    private final TraceBuffer mTraceBuffer = new TraceBuffer(ALWAYS_ON_TRACING_CAPACITY);

    private final Object mEnabledLock = new Object();
    private volatile boolean mActiveTracingEnabled = false;

    /**
     * Records key information about a transition that has been sent to Shell to be played.
     * More information will be appended to the same proto object once the transition is finished or
     * aborted.
     * Transition information won't be added to the trace buffer until
     * {@link #logFinishedTransition} or {@link #logAbortedTransition} is called for this
     * transition.
     *
     * @param transition The transition that has been sent to Shell.
     * @param targets Information about the target windows of the transition.
     */
    @Override
    public void logSentTransition(Transition transition, ArrayList<ChangeInfo> targets) {
        try {
            final ProtoOutputStream outputStream = new ProtoOutputStream(CHUNK_SIZE);
            final long protoToken = outputStream
                    .start(com.android.server.wm.shell.TransitionTraceProto.TRANSITIONS);
            outputStream.write(com.android.server.wm.shell.Transition.ID, transition.getSyncId());
            outputStream.write(com.android.server.wm.shell.Transition.CREATE_TIME_NS,
                    transition.mLogger.mCreateTimeNs);
            outputStream.write(com.android.server.wm.shell.Transition.SEND_TIME_NS,
                    transition.mLogger.mSendTimeNs);
            outputStream.write(com.android.server.wm.shell.Transition.START_TRANSACTION_ID,
                    transition.getStartTransaction().getId());
            outputStream.write(com.android.server.wm.shell.Transition.FINISH_TRANSACTION_ID,
                    transition.getFinishTransaction().getId());
            dumpTransitionTargetsToProto(outputStream, transition, targets);
            outputStream.end(protoToken);

            mTraceBuffer.add(outputStream);
        } catch (Exception e) {
            // Don't let any errors in the tracing cause the transition to fail
            Log.e(LOG_TAG, "Unexpected exception thrown while logging transitions", e);
        }
    }

    /**
     * Completes the information dumped in {@link #logSentTransition} for a transition
     * that has finished or aborted, and add the proto object to the trace buffer.
     *
     * @param transition The transition that has finished.
     */
    @Override
    public void logFinishedTransition(Transition transition) {
        try {
            final ProtoOutputStream outputStream = new ProtoOutputStream(CHUNK_SIZE);
            final long protoToken = outputStream
                    .start(com.android.server.wm.shell.TransitionTraceProto.TRANSITIONS);
            outputStream.write(com.android.server.wm.shell.Transition.ID, transition.getSyncId());
            outputStream.write(com.android.server.wm.shell.Transition.FINISH_TIME_NS,
                    transition.mLogger.mFinishTimeNs);
            outputStream.end(protoToken);

            mTraceBuffer.add(outputStream);
        } catch (Exception e) {
            // Don't let any errors in the tracing cause the transition to fail
            Log.e(LOG_TAG, "Unexpected exception thrown while logging transitions", e);
        }
    }

    /**
     * Same as {@link #logFinishedTransition} but don't add the transition to the trace buffer
     * unless actively tracing.
     *
     * @param transition The transition that has been aborted
     */
    @Override
    public void logAbortedTransition(Transition transition) {
        try {
            final ProtoOutputStream outputStream = new ProtoOutputStream(CHUNK_SIZE);
            final long protoToken = outputStream
                    .start(com.android.server.wm.shell.TransitionTraceProto.TRANSITIONS);
            outputStream.write(com.android.server.wm.shell.Transition.ID, transition.getSyncId());
            outputStream.write(com.android.server.wm.shell.Transition.ABORT_TIME_NS,
                    transition.mLogger.mAbortTimeNs);
            outputStream.end(protoToken);

            mTraceBuffer.add(outputStream);
        } catch (Exception e) {
            // Don't let any errors in the tracing cause the transition to fail
            Log.e(LOG_TAG, "Unexpected exception thrown while logging transitions", e);
        }
    }

    @Override
    public void logRemovingStartingWindow(@NonNull StartingData startingData) {
        if (startingData.mTransitionId == 0) {
            return;
        }
        try {
            final ProtoOutputStream outputStream = new ProtoOutputStream(CHUNK_SIZE);
            final long protoToken = outputStream
                    .start(com.android.server.wm.shell.TransitionTraceProto.TRANSITIONS);
            outputStream.write(com.android.server.wm.shell.Transition.ID,
                    startingData.mTransitionId);
            outputStream.write(
                    com.android.server.wm.shell.Transition.STARTING_WINDOW_REMOVE_TIME_NS,
                    SystemClock.elapsedRealtimeNanos());
            outputStream.end(protoToken);

            mTraceBuffer.add(outputStream);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unexpected exception thrown while logging transitions", e);
        }
    }

    private void dumpTransitionTargetsToProto(ProtoOutputStream outputStream,
            Transition transition, ArrayList<ChangeInfo> targets) {
        Trace.beginSection("TransitionTracer#dumpTransitionTargetsToProto");
        if (mActiveTracingEnabled) {
            outputStream.write(com.android.server.wm.shell.Transition.ID,
                    transition.getSyncId());
        }

        outputStream.write(com.android.server.wm.shell.Transition.TYPE, transition.mType);
        outputStream.write(com.android.server.wm.shell.Transition.FLAGS, transition.getFlags());

        for (int i = 0; i < targets.size(); ++i) {
            final long changeToken = outputStream
                    .start(com.android.server.wm.shell.Transition.TARGETS);

            final Transition.ChangeInfo target = targets.get(i);

            final int layerId;
            if (target.mContainer.mSurfaceControl.isValid()) {
                layerId = target.mContainer.mSurfaceControl.getLayerId();
            } else {
                layerId = -1;
            }

            outputStream.write(com.android.server.wm.shell.Target.MODE, target.mReadyMode);
            outputStream.write(com.android.server.wm.shell.Target.FLAGS, target.mReadyFlags);
            outputStream.write(com.android.server.wm.shell.Target.LAYER_ID, layerId);

            if (mActiveTracingEnabled) {
                // What we use in the WM trace
                final int windowId = System.identityHashCode(target.mContainer);
                outputStream.write(com.android.server.wm.shell.Target.WINDOW_ID, windowId);
            }

            outputStream.end(changeToken);
        }

        Trace.endSection();
    }

    /**
     * Starts collecting transitions for the trace.
     * If called while a trace is already running, this will reset the trace.
     */
    @Override
    public void startTrace(@Nullable PrintWriter pw) {
        if (IS_USER) {
            LogAndPrintln.e(pw, "Tracing is not supported on user builds.");
            return;
        }
        Trace.beginSection("TransitionTracer#startTrace");
        LogAndPrintln.i(pw, "Starting shell transition trace.");
        synchronized (mEnabledLock) {
            mActiveTracingEnabled = true;
            mTraceBuffer.resetBuffer();
            mTraceBuffer.setCapacity(ACTIVE_TRACING_BUFFER_CAPACITY);
        }
        Trace.endSection();
    }

    /**
     * Stops collecting the transition trace and dump to trace to file.
     *
     * Dumps the trace to @link{TRACE_FILE}.
     */
    @Override
    public void stopTrace(@Nullable PrintWriter pw) {
        stopTrace(pw, new File(TRACE_FILE));
    }

    /**
     * Stops collecting the transition trace and dump to trace to file.
     * @param outputFile The file to dump the transition trace to.
     */
    public void stopTrace(@Nullable PrintWriter pw, File outputFile) {
        if (IS_USER) {
            LogAndPrintln.e(pw, "Tracing is not supported on user builds.");
            return;
        }
        Trace.beginSection("TransitionTracer#stopTrace");
        LogAndPrintln.i(pw, "Stopping shell transition trace.");
        synchronized (mEnabledLock) {
            mActiveTracingEnabled = false;
            writeTraceToFileLocked(pw, outputFile);
            mTraceBuffer.resetBuffer();
            mTraceBuffer.setCapacity(ALWAYS_ON_TRACING_CAPACITY);
        }
        Trace.endSection();
    }

    /**
     * Being called while taking a bugreport so that tracing files can be included in the bugreport.
     *
     * @param pw Print writer
     */
    @Override
    public void saveForBugreport(@Nullable PrintWriter pw) {
        if (IS_USER) {
            LogAndPrintln.e(pw, "Tracing is not supported on user builds.");
            return;
        }
        Trace.beginSection("TransitionTracer#saveForBugreport");
        synchronized (mEnabledLock) {
            final File outputFile = new File(TRACE_FILE);
            writeTraceToFileLocked(pw, outputFile);
        }
        Trace.endSection();
    }

    @Override
    public boolean isTracing() {
        return mActiveTracingEnabled;
    }

    private void writeTraceToFileLocked(@Nullable PrintWriter pw, File file) {
        Trace.beginSection("TransitionTracer#writeTraceToFileLocked");
        try {
            ProtoOutputStream proto = new ProtoOutputStream(CHUNK_SIZE);
            proto.write(MAGIC_NUMBER, MAGIC_NUMBER_VALUE);
            long timeOffsetNs =
                    TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis())
                            - SystemClock.elapsedRealtimeNanos();
            proto.write(REAL_TO_ELAPSED_TIME_OFFSET_NANOS, timeOffsetNs);
            int pid = android.os.Process.myPid();
            LogAndPrintln.i(pw, "Writing file to " + file.getAbsolutePath()
                    + " from process " + pid);
            mTraceBuffer.writeTraceToFile(file, proto);
        } catch (IOException e) {
            LogAndPrintln.e(pw, "Unable to write buffer to file", e);
        }
        Trace.endSection();
    }

    private static class LogAndPrintln {
        private static void i(@Nullable PrintWriter pw, String msg) {
            Log.i(LOG_TAG, msg);
            if (pw != null) {
                pw.println(msg);
                pw.flush();
            }
        }

        private static void e(@Nullable PrintWriter pw, String msg) {
            Log.e(LOG_TAG, msg);
            if (pw != null) {
                pw.println("ERROR: " + msg);
                pw.flush();
            }
        }

        private static void e(@Nullable PrintWriter pw, String msg, @NonNull Exception e) {
            Log.e(LOG_TAG, msg, e);
            if (pw != null) {
                pw.println("ERROR: " + msg + " ::\n " + e);
                pw.flush();
            }
        }
    }
}
