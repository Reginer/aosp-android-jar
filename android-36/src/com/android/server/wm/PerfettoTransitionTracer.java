/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.tracing.perfetto.DataSourceParams.PERFETTO_DS_BUFFER_EXHAUSTED_POLICY_DROP;

import android.annotation.NonNull;
import android.internal.perfetto.protos.ShellTransitionOuterClass.ShellTransition;
import android.internal.perfetto.protos.TracePacketOuterClass.TracePacket;
import android.os.SystemClock;
import android.os.Trace;
import android.tracing.perfetto.DataSourceParams;
import android.tracing.perfetto.InitArguments;
import android.tracing.perfetto.Producer;
import android.tracing.transition.TransitionDataSource;
import android.util.proto.ProtoOutputStream;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

class PerfettoTransitionTracer implements TransitionTracer {
    private final AtomicInteger mActiveTraces = new AtomicInteger(0);
    private final TransitionDataSource mDataSource =
            new TransitionDataSource(this.mActiveTraces::incrementAndGet, () -> {},
                    this.mActiveTraces::decrementAndGet);

    PerfettoTransitionTracer() {
        Producer.init(InitArguments.DEFAULTS);
        DataSourceParams params =
                new DataSourceParams.Builder()
                        .setBufferExhaustedPolicy(
                                PERFETTO_DS_BUFFER_EXHAUSTED_POLICY_DROP)
                        .build();
        mDataSource.register(params);
    }

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
    public void logSentTransition(Transition transition, ArrayList<Transition.ChangeInfo> targets) {
        if (!isTracing()) {
            return;
        }

        Trace.traceBegin(Trace.TRACE_TAG_WINDOW_MANAGER, "logSentTransition");
        try {
            doLogSentTransition(transition, targets);
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_WINDOW_MANAGER);
        }
    }

    private void doLogSentTransition(
            Transition transition, ArrayList<Transition.ChangeInfo> targets) {
        mDataSource.trace((ctx) -> {
            final ProtoOutputStream os = ctx.newTracePacket();

            final long token = os.start(TracePacket.SHELL_TRANSITION);

            os.write(ShellTransition.ID, transition.getSyncId());
            os.write(ShellTransition.CREATE_TIME_NS,
                    transition.mLogger.mCreateTimeNs);
            os.write(ShellTransition.SEND_TIME_NS, transition.mLogger.mSendTimeNs);
            os.write(ShellTransition.START_TRANSACTION_ID,
                    transition.getStartTransaction().getId());
            os.write(ShellTransition.FINISH_TRANSACTION_ID,
                    transition.getFinishTransaction().getId());
            os.write(ShellTransition.TYPE, transition.mType);
            os.write(ShellTransition.FLAGS, transition.getFlags());

            addTransitionTargetsToProto(os, targets);

            os.end(token);
        });
    }

    /**
     * Completes the information dumped in {@link #logSentTransition} for a transition
     * that has finished or aborted, and add the proto object to the trace buffer.
     *
     * @param transition The transition that has finished.
     */
    @Override
    public void logFinishedTransition(Transition transition) {
        if (!isTracing()) {
            return;
        }

        Trace.traceBegin(Trace.TRACE_TAG_WINDOW_MANAGER, "logFinishedTransition");
        try {
            doLogFinishTransition(transition);
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_WINDOW_MANAGER);
        }
    }

    private void doLogFinishTransition(Transition transition) {
        mDataSource.trace((ctx) -> {
            final ProtoOutputStream os = ctx.newTracePacket();

            final long token = os.start(TracePacket.SHELL_TRANSITION);
            os.write(ShellTransition.ID, transition.getSyncId());
            os.write(ShellTransition.FINISH_TIME_NS,
                    transition.mLogger.mFinishTimeNs);
            os.end(token);
        });
    }

    /**
     * Same as {@link #logFinishedTransition} but don't add the transition to the trace buffer
     * unless actively tracing.
     *
     * @param transition The transition that has been aborted
     */
    @Override
    public void logAbortedTransition(Transition transition) {
        if (!isTracing()) {
            return;
        }

        Trace.traceBegin(Trace.TRACE_TAG_WINDOW_MANAGER, "logAbortedTransition");
        try {
            doLogAbortedTransition(transition);
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_WINDOW_MANAGER);
        }
    }

    private void doLogAbortedTransition(Transition transition) {
        mDataSource.trace((ctx) -> {
            final ProtoOutputStream os = ctx.newTracePacket();

            final long token = os.start(TracePacket.SHELL_TRANSITION);
            os.write(ShellTransition.ID, transition.getSyncId());
            os.write(ShellTransition.WM_ABORT_TIME_NS,
                    transition.mLogger.mAbortTimeNs);
            os.end(token);
        });
    }

    @Override
    public void logRemovingStartingWindow(@NonNull StartingData startingData) {
        if (!isTracing()) {
            return;
        }

        Trace.traceBegin(Trace.TRACE_TAG_WINDOW_MANAGER, "logRemovingStartingWindow");
        try {
            doLogRemovingStartingWindow(startingData);
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_WINDOW_MANAGER);
        }
    }

    public void doLogRemovingStartingWindow(@NonNull StartingData startingData) {
        mDataSource.trace((ctx) -> {
            final ProtoOutputStream os = ctx.newTracePacket();

            final long token = os.start(TracePacket.SHELL_TRANSITION);
            os.write(ShellTransition.ID, startingData.mTransitionId);
            os.write(ShellTransition.STARTING_WINDOW_REMOVE_TIME_NS,
                    SystemClock.elapsedRealtimeNanos());
            os.end(token);
        });
    }

    @Override
    public void startTrace(PrintWriter pw) {
        // No-op
    }

    @Override
    public void stopTrace(PrintWriter pw) {
        // No-op
    }

    @Override
    public void saveForBugreport(PrintWriter pw) {
        // Nothing to do here. Handled by Perfetto.
    }

    @Override
    public boolean isTracing() {
        return mActiveTraces.get() > 0;
    }

    private void addTransitionTargetsToProto(
            ProtoOutputStream os,
            ArrayList<Transition.ChangeInfo> targets
    ) {
        for (int i = 0; i < targets.size(); ++i) {
            final Transition.ChangeInfo target = targets.get(i);

            final int layerId;
            if (target.mContainer.mSurfaceControl.isValid()) {
                layerId = target.mContainer.mSurfaceControl.getLayerId();
            } else {
                layerId = -1;
            }
            final int windowId = System.identityHashCode(target.mContainer);

            final long token = os.start(ShellTransition.TARGETS);
            os.write(ShellTransition.Target.MODE, target.mReadyMode);
            os.write(ShellTransition.Target.FLAGS, target.mReadyFlags);
            os.write(ShellTransition.Target.LAYER_ID, layerId);
            os.write(ShellTransition.Target.WINDOW_ID, windowId);
            os.end(token);
        }
    }
}
