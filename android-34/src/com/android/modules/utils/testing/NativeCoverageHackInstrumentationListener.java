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
 * limitations under the License
 */

package com.android.modules.utils.testing;

import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import androidx.test.internal.runner.listener.InstrumentationRunListener;

import org.junit.runner.Result;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Instrumentation listener that dumps Clang coverage when the test run finishes.
 *
 * This is necessary as native coverage is not dumped automatically after Java/JNI tests
 * (b/185074329) and should be replaced by more generic solutions (b/185822084).
 */
public class NativeCoverageHackInstrumentationListener extends InstrumentationRunListener {
    private static final String LOG_TAG =
            NativeCoverageHackInstrumentationListener.class.getSimpleName();
    // Signal used to trigger a dump of Clang coverage information.
    // See {@code maybeDumpNativeCoverage} below.
    private static final int COVERAGE_SIGNAL = 37;

    @Override
    public void testRunFinished(Result result) throws Exception {
        maybeDumpNativeCoverage();
        super.testRunFinished(result);
    }

    /**
     * If this test process is instrumented for native coverage, then trigger a dump
     * of the coverage data and wait until either we detect the dumping has finished or 60 seconds,
     * whichever is shorter.
     *
     * Background: Coverage builds install a signal handler for signal 37 which flushes coverage
     * data to disk, which may take a few seconds.  Tests running as an app process will get
     * killed with SIGKILL once the app code exits, even if the coverage handler is still running.
     *
     * Method: If a handler is installed for signal 37, then assume this is a coverage run and
     * send signal 37.  The handler is non-reentrant and so signal 37 will then be blocked until
     * the handler completes. So after we send the signal, we loop checking the blocked status
     * for signal 37 until we hit the 60 second deadline.  If the signal is blocked then sleep for
     * 2 seconds, and if it becomes unblocked then the handler exitted so we can return early.
     * If the signal is not blocked at the start of the loop then most likely the handler has
     * not yet been invoked.  This should almost never happen as it should get blocked on delivery
     * when we call {@code Os.kill()}, so sleep for a shorter duration (100ms) and try again.  There
     * is a race condition here where the handler is delayed but then runs for less than 100ms and
     * gets missed, in which case this method will loop with 100ms sleeps until the deadline.
     *
     * In the case where the handler runs for more than 60 seconds, the test process will be allowed
     * to exit so coverage information may be incomplete.
     *
     * There is no API for determining signal dispositions, so this method uses the
     * {@link SignalMaskInfo} class to read the data from /proc.  If there is an error parsing
     * the /proc data then this method will also loop until the 60s deadline passes.
     */
    private void maybeDumpNativeCoverage() {
        SignalMaskInfo siginfo = new SignalMaskInfo();
        if (!siginfo.isValid()) {
            Log.e(LOG_TAG, "Invalid signal info");
            return;
        }

        if (!siginfo.isCaught(COVERAGE_SIGNAL)) {
            // Process is not instrumented for coverage
            Log.i(LOG_TAG, "Not dumping coverage, no handler installed");
            return;
        }

        Log.i(LOG_TAG,
                String.format("Sending coverage dump signal %d to pid %d uid %d", COVERAGE_SIGNAL,
                        Os.getpid(), Os.getuid()));
        try {
            Os.kill(Os.getpid(), COVERAGE_SIGNAL);
        } catch (ErrnoException e) {
            Log.e(LOG_TAG, "Unable to send coverage signal", e);
            return;
        }

        long start = System.currentTimeMillis();
        long deadline = start + 60 * 1000L;
        while (System.currentTimeMillis() < deadline) {
            siginfo.refresh();
            try {
                if (siginfo.isValid() && siginfo.isBlocked(COVERAGE_SIGNAL)) {
                    // Signal is currently blocked so assume a handler is running
                    Thread.sleep(2000L);
                    siginfo.refresh();
                    if (siginfo.isValid() && !siginfo.isBlocked(COVERAGE_SIGNAL)) {
                        // Coverage handler exited while we were asleep
                        Log.i(LOG_TAG,
                                String.format("Coverage dump detected finished after %dms",
                                        System.currentTimeMillis() - start));
                        break;
                    }
                } else {
                    // Coverage signal handler not yet started or invalid siginfo
                    Thread.sleep(100L);
                }
            } catch (InterruptedException e) {
                // ignored
            }
        }
    }

    /**
     * Class for reading a process' signal masks from the /proc filesystem.  Looks for the
     * BLOCKED, CAUGHT, IGNORED and PENDING masks from /proc/self/status, each of which is a
     * 64 bit bitmask with one bit per signal.
     *
     * Maintains a map from SignalMaskInfo.Type to the bitmask.  The {@code isValid} method
     * will only return true if all 4 masks were successfully parsed. Provides lookup
     * methods per signal, e.g. {@code isPending(signum)} which will throw
     * {@code IllegalStateException} if the current data is not valid.
     */
    private static class SignalMaskInfo {
        private enum Type {
            BLOCKED("SigBlk"),
            CAUGHT("SigCgt"),
            IGNORED("SigIgn"),
            PENDING("SigPnd");
            // The tag for this mask in /proc/self/status
            private final String tag;

            Type(String tag) {
                this.tag = tag + ":\t";
            }

            public String getTag() {
                return tag;
            }

            public static Map<Type, Long> parseProcinfo(String path) {
                Map<Type, Long> map = new HashMap<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        for (Type mask : Type.values()) {
                            long value = mask.tryToParse(line);
                            if (value >= 0) {
                                map.put(mask, value);
                            }
                        }
                    }
                } catch (NumberFormatException | IOException e) {
                    // Ignored - the map will end up being invalid instead.
                }
                return map;
            }

            private long tryToParse(String line) {
                if (line.startsWith(tag)) {
                    return Long.valueOf(line.substring(tag.length()), 16);
                } else {
                    return -1;
                }
            }
        }

        private static final String PROCFS_PATH = "/proc/self/status";
        private Map<Type, Long> maskMap = null;

        SignalMaskInfo() {
            refresh();
        }

        public void refresh() {
            maskMap = Type.parseProcinfo(PROCFS_PATH);
        }

        public boolean isValid() {
            return (maskMap != null && maskMap.size() == Type.values().length);
        }

        public boolean isCaught(int signal) {
            return isSignalInMask(signal, Type.CAUGHT);
        }

        public boolean isBlocked(int signal) {
            return isSignalInMask(signal, Type.BLOCKED);
        }

        public boolean isPending(int signal) {
            return isSignalInMask(signal, Type.PENDING);
        }

        public boolean isIgnored(int signal) {
            return isSignalInMask(signal, Type.IGNORED);
        }

        private void checkValid() {
            if (!isValid()) {
                throw new IllegalStateException();
            }
        }

        private boolean isSignalInMask(int signal, Type mask) {
            long bit = 1L << (signal - 1);
            return (getSignalMask(mask) & bit) != 0;
        }

        private long getSignalMask(Type mask) {
            checkValid();
            Long value = maskMap.get(mask);
            if (value == null) {
                throw new IllegalStateException();
            }
            return value;
        }
    }
}