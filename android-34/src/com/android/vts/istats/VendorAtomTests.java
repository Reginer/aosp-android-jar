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
package com.android.vts.istats;

import static com.google.common.truth.Truth.assertThat;

import android.cts.statsdatom.lib.AtomTestUtils;
import android.cts.statsdatom.lib.ConfigUtils;
import android.cts.statsdatom.lib.DeviceUtils;
import android.cts.statsdatom.lib.ReportUtils;
import android.hardware.istats.TestVendorAtom;
import com.android.compatibility.common.util.NonApiTest;
import com.android.os.StatsLog.EventMetricData;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.TestRunResult;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/* VTS test to verify IStats reportVendorAtom APIs. */
@NonApiTest(exemptionReasons = {}, justification = "METRIC")
public class VendorAtomTests extends DeviceTestCase implements IBuildReceiver {
    private static final String ISTATS_TEST_PKG = "com.android.vts.istats.vendoratom";

    private IBuildInfo mVtsBuild;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertThat(mVtsBuild).isNotNull();
        ConfigUtils.removeConfig(getDevice());
        ReportUtils.clearReports(getDevice());
        Thread.sleep(AtomTestUtils.WAIT_TIME_LONG);
    }

    @Override
    protected void tearDown() throws Exception {
        ConfigUtils.removeConfig(getDevice());
        ReportUtils.clearReports(getDevice());
        super.tearDown();
    }

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mVtsBuild = buildInfo;
    }

    public void testReportVendorAtomWrongId() throws Exception {
        assertThat(isIStatsPresentOnDevice()).isTrue();
        ConfigUtils.uploadConfigForPushedAtom(getDevice(), ISTATS_TEST_PKG, 1000);
        List<EventMetricData> data = runVendorAtomDeviceTests("testReportVendorAtomWrongId");
        assertThat(data).hasSize(0);
    }

    public void testReportVendorAtomInt() throws Exception {
        assertThat(isIStatsPresentOnDevice()).isTrue();
        ConfigUtils.uploadConfigForPushedAtom(getDevice(), ISTATS_TEST_PKG,
            TestVendorAtom.Atom.TEST_VENDOR_ATOM_REPORTED_FIELD_NUMBER);

        List<EventMetricData> data = runVendorAtomDeviceTests("testReportVendorAtomInt");
        final TestVendorAtom.TestVendorAtomReported vendorAtom = getVendorAtom(data);
        assertThat(vendorAtom.getReverseDomainName()).isEqualTo("com.test.domain");
        assertThat(vendorAtom.getIntValue()).isEqualTo(7);
    }

    public void testReportVendorAtomRepeated() throws Exception {
        assertThat(isIStatsPresentOnDevice()).isTrue();
        ConfigUtils.uploadConfigForPushedAtom(getDevice(), ISTATS_TEST_PKG,
            TestVendorAtom.Atom.TEST_VENDOR_ATOM_REPORTED_FIELD_NUMBER);

        List<EventMetricData> data = runVendorAtomDeviceTests("testReportVendorAtomRepeated");
        final TestVendorAtom.TestVendorAtomReported vendorAtom = getVendorAtom(data);
        assertThat(vendorAtom.getReverseDomainName()).isEqualTo("com.test.domain");
        assertThat(vendorAtom.getIntValue()).isEqualTo(7);
        assertThat(vendorAtom.getLongValue()).isEqualTo(70000L);
        assertThat(vendorAtom.getFloatValue()).isEqualTo(8.5f);
        assertThat(vendorAtom.getStringValue()).isEqualTo("testString");
        assertThat(vendorAtom.getBoolValue()).isEqualTo(true);

        assertThat(vendorAtom.getRepeatedIntValueList()).isEqualTo(Arrays.asList(11, 12, 13));
        assertThat(vendorAtom.getRepeatedLongValueList())
            .isEqualTo(Arrays.asList(11000L, 12000L, 13000L));
        assertThat(vendorAtom.getRepeatedFloatValueList())
            .isEqualTo(Arrays.asList(0.1f, 0.2f, 0.3f));
        assertThat(vendorAtom.getRepeatedStringValueList())
            .isEqualTo(Arrays.asList("abc", "def", "xyz"));
        assertThat(vendorAtom.getRepeatedBoolValueList())
            .isEqualTo(Arrays.asList(true, false, false, true));
    }

    private boolean isIStatsPresentOnDevice() throws Exception {
        return checkDeviceFor("testIStatsPresent");
    }

    private boolean checkDeviceFor(String methodName) throws Exception {
        try {
            runDeviceTestsOnVendorAtom(getDevice(), methodName);
            // Test passes, meaning that the answer is true.
            CLog.d(methodName + "() indicates true.");
            return true;
        } catch (AssertionError e) {
            // Method is designed to fail if the answer is false.
            CLog.d(methodName + "() indicates false.");
            return false;
        }
    }

    private TestVendorAtom.TestVendorAtomReported getVendorAtom(@Nonnull List<EventMetricData> data)
        throws Exception {
        assertThat(data).hasSize(1);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CodedOutputStream codedos = CodedOutputStream.newInstance(outputStream);
        data.get(0).getAtom().writeTo(codedos);
        codedos.flush();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        CodedInputStream codedis = CodedInputStream.newInstance(inputStream);
        final TestVendorAtom.Atom atom = TestVendorAtom.Atom.parseFrom(codedis);
        assertThat(atom.hasTestVendorAtomReported()).isTrue();

        return atom.getTestVendorAtomReported();
    }

    private List<EventMetricData> runVendorAtomDeviceTests(String testMethodName) throws Exception {
        runDeviceTestsOnVendorAtom(getDevice(), testMethodName);
        Thread.sleep(AtomTestUtils.WAIT_TIME_LONG);
        // Sorted list of events in order in which they occurred.
        return ReportUtils.getEventMetricDataList(getDevice());
    }

    /** Runs device side tests from the com.android.vts.istats.vendoratom package. */
    private static @Nonnull TestRunResult runDeviceTestsOnVendorAtom(
        ITestDevice device, @Nullable String testMethodName) throws DeviceNotAvailableException {
        return DeviceUtils.runDeviceTests(
            device, ISTATS_TEST_PKG, ".VtsVendorAtomJavaTest", testMethodName);
    }
}
