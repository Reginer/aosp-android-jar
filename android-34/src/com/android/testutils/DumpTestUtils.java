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

package com.android.testutils;

import static com.android.testutils.TestPermissionUtil.runAsShell;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.system.ErrnoException;
import android.system.Os;

import libcore.io.IoUtils;
import libcore.io.Streams;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utilities for testing output of service dumps.
 */
public class DumpTestUtils {

    private static String dumpService(String serviceName, boolean adoptPermission, String... args)
            throws RemoteException, InterruptedException, ErrnoException {
        final IBinder ib = ServiceManager.getService(serviceName);
        FileDescriptor[] pipe = Os.pipe();

        // Start a thread to read the dump output, or dump might block if it fills the pipe.
        final CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> output = new AtomicReference<>();
        // Used to send exceptions back to the main thread to ensure that the test fails cleanly.
        AtomicReference<Exception> exception = new AtomicReference<>();
        new Thread(() -> {
            try {
                output.set(Streams.readFully(
                        new InputStreamReader(new FileInputStream(pipe[0]),
                                StandardCharsets.UTF_8)));
                latch.countDown();
            } catch (Exception e) {
                exception.set(e);
                latch.countDown();
            }
        }).start();

        final int timeoutMs = 5_000;
        final String what = "service '" + serviceName + "' with args: " + Arrays.toString(args);
        try {
            if (adoptPermission) {
                runAsShell(android.Manifest.permission.DUMP, () -> ib.dump(pipe[1], args));
            } else {
                ib.dump(pipe[1], args);
            }
            IoUtils.closeQuietly(pipe[1]);
            assertTrue("Dump of " + what + " timed out after " + timeoutMs + "ms",
                    latch.await(timeoutMs, TimeUnit.MILLISECONDS));
        } finally {
            // Closing the fds will terminate the thread if it's blocked on read.
            IoUtils.closeQuietly(pipe[0]);
            if (pipe[1].valid()) IoUtils.closeQuietly(pipe[1]);
        }
        if (exception.get() != null) {
            fail("Exception dumping " + what + ": " + exception.get());
        }
        return output.get();
    }

    /**
     * Dumps the specified service and returns a string. Sends a dump IPC to the given service
     * with the specified args and a pipe, then reads from the pipe in a separate thread.
     * The current process must already have the DUMP permission.
     *
     * @param serviceName the service to dump.
     * @param args the arguments to pass to the dump function.
     * @return The dump text.
     * @throws RemoteException dumping the service failed.
     * @throws InterruptedException the dump timed out.
     * @throws ErrnoException opening or closing the pipe for the dump failed.
     */
    public static String dumpService(String serviceName, String... args)
            throws RemoteException, InterruptedException, ErrnoException {
        return dumpService(serviceName, false, args);
    }

    /**
     * Dumps the specified service and returns a string. Sends a dump IPC to the given service
     * with the specified args and a pipe, then reads from the pipe in a separate thread.
     * Adopts the {@code DUMP} permission via {@code adoptShellPermissionIdentity} and then releases
     * it. This method should not be used if the caller already has the shell permission identity.
     * TODO: when Q and R are no longer supported, use
     * {@link android.app.UiAutomation#getAdoptedShellPermissions} to automatically acquire the
     * shell permission if the caller does not already have it.
     *
     * @param serviceName the service to dump.
     * @param args the arguments to pass to the dump function.
     * @return The dump text.
     * @throws RemoteException dumping the service failed.
     * @throws InterruptedException the dump timed out.
     * @throws ErrnoException opening or closing the pipe for the dump failed.
     */
    public static String dumpServiceWithShellPermission(String serviceName, String... args)
            throws RemoteException, InterruptedException, ErrnoException {
        return dumpService(serviceName, true, args);
    }
}
