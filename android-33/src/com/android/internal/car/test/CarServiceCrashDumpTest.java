/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.internal.car.test;

import static com.google.common.truth.Truth.assertWithMessage;

import com.android.compatibility.common.util.CommonTestUtils;
import com.android.compatibility.common.util.PollingCheck;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;

import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(DeviceJUnit4ClassRunner.class)
public final class CarServiceCrashDumpTest extends BaseHostJUnit4Test {
    private static final int DEFAULT_TIMEOUT_SEC = 20;
    private static final long POLL_TIMEOUT_MS = 20000;
    private static final String BUILD_TYPE_PROPERTY = "ro.build.type";

    // This must be in sync with WatchDog lib.
    private static final List<String> HAL_INTERFACES_OF_INTEREST = Arrays.asList(
            "android.hardware.audio@4.0::IDevicesFactory",
            "android.hardware.audio@5.0::IDevicesFactory",
            "android.hardware.audio@6.0::IDevicesFactory",
            "android.hardware.audio@7.0::IDevicesFactory",
            "android.hardware.biometrics.face@1.0::IBiometricsFace",
            "android.hardware.biometrics.fingerprint@2.1::IBiometricsFingerprint",
            "android.hardware.bluetooth@1.0::IBluetoothHci",
            "android.hardware.camera.provider@2.4::ICameraProvider",
            "android.hardware.gnss@1.0::IGnss",
            "android.hardware.graphics.allocator@2.0::IAllocator",
            "android.hardware.graphics.composer@2.1::IComposer",
            "android.hardware.health@2.0::IHealth",
            "android.hardware.light@2.0::ILight",
            "android.hardware.media.c2@1.0::IComponentStore",
            "android.hardware.media.omx@1.0::IOmx",
            "android.hardware.media.omx@1.0::IOmxStore",
            "android.hardware.neuralnetworks@1.0::IDevice",
            "android.hardware.power.stats@1.0::IPowerStats",
            "android.hardware.sensors@1.0::ISensors",
            "android.hardware.sensors@2.0::ISensors",
            "android.hardware.sensors@2.1::ISensors",
            "android.hardware.vr@1.0::IVr",
            "android.system.suspend@1.0::ISystemSuspend"
    );

    // Which native processes to dump into dropbox's stack traces, must be in sync with Watchdog
    // lib.
    private static final String[] NATIVE_STACKS_OF_INTEREST = new String[] {
        "/system/bin/audioserver",
        "/system/bin/cameraserver",
        "/system/bin/drmserver",
        "/system/bin/keystore2",
        "/system/bin/mediadrmserver",
        "/system/bin/mediaserver",
        "/system/bin/netd",
        "/system/bin/sdcard",
        "/system/bin/surfaceflinger",
        "/system/bin/vold",
        "media.extractor", // system/bin/mediaextractor
        "media.metrics", // system/bin/mediametrics
        "media.codec", // vendor/bin/hw/android.hardware.media.omx@1.0-service
        "media.swcodec", // /apex/com.android.media.swcodec/bin/mediaswcodec
        "media.transcoding", // Media transcoding service
        "com.android.bluetooth",  // Bluetooth service
        "/apex/com.android.os.statsd/bin/statsd",  // Stats daemon
    };

    /**
     * Executes the shell command and returns the output.
     */
    private String executeCommand(String command, Object... args) throws Exception {
        String fullCommand = String.format(command, args);
        return getDevice().executeShellCommand(fullCommand);
    }

    /**
     * Waits until the car service is ready.
     */
    private void waitForCarServiceReady() throws Exception {
        CommonTestUtils.waitUntil("timed out waiting for car service ",
                DEFAULT_TIMEOUT_SEC, () -> isCarServiceReady());
    }

    private boolean isCarServiceReady() {
        String cmd = "service check car_service";
        try {
            String output = getDevice().executeShellCommand(cmd).strip();
            return !output.endsWith("not found");
        } catch (Exception e) {
            CLog.w("%s failed: %s", cmd, e.getMessage());
        }
        return false;
    }

    @Before
    public void setUp() throws Exception {
        executeCommand("logcat -c");
    }

    /**
     * Read the content of the dumped file.
     */
    private String getDumpFile() throws Exception {
        AtomicReference<String> log = new AtomicReference<>();
        String dumpString = "ActivityManager: Dumping to ";
        String doneDumpingString = "ActivityManager: Done dumping";
        PollingCheck.check("dumpStackTrace not found in log", POLL_TIMEOUT_MS, () -> {
            String logString = executeCommand("logcat -d");
            if (logString.contains("ActivityManager: dumpStackTraces") && logString.contains(
                    dumpString) && logString.contains(doneDumpingString)) {
                log.set(logString);
                return true;
            }
            return false;
        });
        String logString = log.get();
        int start = logString.indexOf(dumpString) + dumpString.length();
        int end = logString.indexOf("\n", start);
        if (end == -1) {
            end = logString.length();
        }
        return logString.substring(start, end);
    }

    /**
     * Get a list of PIDs for the interesting HALs that would be dumped.
     */
    private List<String> getHalPids() throws Exception {
        String lshalResult = executeCommand("lshal -i -p");
        List<String> pids = new ArrayList<String>();
        int i = 0;
        for (String line: lshalResult.split("\n")) {
            line = line.strip();
            if (line.equals("")) {
                // When we see an empty line, we stops the parsing.
                break;
            }
            if (i < 2) {
                // Skip the first two lines
                i++;
                continue;
            }
            String[] fields = line.split("\\s+");
            for (String interestHal: HAL_INTERFACES_OF_INTEREST) {
                if (fields[0].contains(interestHal)) {
                    pids.add(fields[1]);
                    break;
                }
            }
            i++;
        }
        return pids;
    }

    /**
     * Get a list of PIDs for the native services that would be dumped.
     */
    private List<String> getNativePids() throws Exception {
        List<String> pids = new ArrayList<String>();
        for (String name: NATIVE_STACKS_OF_INTEREST) {
            String pid = executeCommand(String.format("pidof %s", name)).strip();
            if (!pid.equals("")) {
                pids.add(pid);
            }
        }
        return pids;
    }

    @Test
    public void testCarServiceCrashDump() throws Exception {
        String buildType = getDevice().getProperty("ro.build.type");
        // Only run on userdebug devices.
        assumeTrue(buildType.equals("userdebug") || buildType.equals("eng"));

        List<String> pids = new ArrayList<String>();

        getDevice().enableAdbRoot();

        String systemServerPid = executeCommand(String.format("pidof %s", "system_server")).strip();
        assertWithMessage("system_service pid not empty").that(systemServerPid).isNotEmpty();
        pids.add(systemServerPid);

        List<String> halPids = getHalPids();
        pids.addAll(halPids);
        assertWithMessage("hal pids").that(halPids.size() > 0).isTrue();

        List<String> nativePids = getNativePids();
        pids.addAll(nativePids);
        assertWithMessage("native pids").that(nativePids.size() > 0).isTrue();

        executeCommand("am crash --user 0 com.android.car");

        String dumpFile = getDumpFile();
        assertWithMessage("dump file").that(dumpFile).isNotEmpty();

        String grepResult = executeCommand("cat %s", dumpFile);

        assertWithMessage("dumped content not empty").that(grepResult)
                .isNotEmpty();

        for (String pid : pids) {
            assertWithMessage("dumped content contains interesting pid").that(grepResult)
                    .contains(String.format("----- pid %s at", pid));
        }
    }

    @After
    public void tearDown() throws Exception {
        waitForCarServiceReady();
    }
}
