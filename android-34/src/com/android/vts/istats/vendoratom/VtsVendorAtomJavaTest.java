/*
 * Copyright (C) 2022 Google LLC
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

package com.android.vts.istats.vendoratom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.frameworks.stats.IStats;
import android.frameworks.stats.VendorAtom;
import android.frameworks.stats.VendorAtomValue;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class VtsVendorAtomJavaTest {
    private static final String TAG = "VtsTest";
    Optional<IStats> statsService;

    private static final int TestVendorAtomId = 109999;

    @Before
    public void setUp() {
        try {
            final String[] instances = ServiceManager.getDeclaredInstances(IStats.DESCRIPTOR);
            assertEquals(1, instances.length);
            assertEquals(instances[0], "default");

            final String instanceName = IStats.DESCRIPTOR + "/default";
            if (!ServiceManager.isDeclared(instanceName)) {
                Log.e(TAG, "IStats is not registered");
                statsService = Optional.empty();
            } else {
                statsService = Optional.ofNullable(
                    IStats.Stub.asInterface(ServiceManager.waitForDeclaredService(instanceName)));
            }
            assertTrue(statsService.isPresent());
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to connect to IStats service", e);
        } catch (NullPointerException e) {
            Log.e(TAG, "Failed to connect to IStats service", e);
        }
        Log.i(TAG, "Setup done");
    }

    @Test
    public void testIStatsPresent() {
        assertTrue(statsService.isPresent());
    }

    /*
     * Test IStats::reportVendorAtom with int field
     */
    @Test
    public void testReportVendorAtomInt() {
        VendorAtom atom = new VendorAtom();
        atom.atomId = TestVendorAtomId;
        atom.reverseDomainName = "com.test.domain";
        atom.values = new VendorAtomValue[1];
        atom.values[0] = VendorAtomValue.intValue(7);

        try {
            statsService.get().reportVendorAtom(atom);
        } catch (NoSuchElementException e) {
            Log.e(TAG, "Failed to get IStats service", e);
            fail();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to log atom to IStats service", e);
            fail();
        }
    }

    /*
     * Test IStats::reportVendorAtom with wrong atom code - this event will be dropped.
     */
    @Test
    public void testReportVendorAtomWrongId() {
        VendorAtom atom = new VendorAtom();
        atom.atomId = 1000;
        atom.reverseDomainName = "com.test.domain";
        atom.values = new VendorAtomValue[1];
        atom.values[0] = VendorAtomValue.intValue(7);
        try {
            statsService.get().reportVendorAtom(atom);
        } catch (NoSuchElementException e) {
            Log.e(TAG, "Failed to get IStats service", e);
            fail();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to log atom to IStats service", e);
            fail();
        }
    }

    /*
     * Test IStats::reportVendorAtom with repeated fields.
     */
    @Test
    public void testReportVendorAtomRepeated() {
        VendorAtom atom = new VendorAtom();
        atom.atomId = TestVendorAtomId;
        atom.reverseDomainName = "com.test.domain";
        atom.values = new VendorAtomValue[10];
        atom.values[0] = VendorAtomValue.intValue(7);
        atom.values[1] = VendorAtomValue.longValue(70000L);
        atom.values[2] = VendorAtomValue.floatValue(8.5f);
        atom.values[3] = VendorAtomValue.stringValue("testString");
        atom.values[4] = VendorAtomValue.boolValue(true);
        atom.values[5] = VendorAtomValue.repeatedIntValue(new int[] {11, 12, 13});
        atom.values[6] = VendorAtomValue.repeatedLongValue(new long[] {11000L, 12000L, 13000L});
        atom.values[7] = VendorAtomValue.repeatedFloatValue(new float[] {0.1f, 0.2f, 0.3f});
        atom.values[8] = VendorAtomValue.repeatedStringValue(new String[] {"abc", "def", "xyz"});
        atom.values[9] =
            VendorAtomValue.repeatedBoolValue(new boolean[] {true, false, false, true});

        try {
            statsService.get().reportVendorAtom(atom);
        } catch (NoSuchElementException e) {
            Log.e(TAG, "Failed to get IStats service", e);
            fail();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to log atom to IStats service", e);
            fail();
        }
    }
}
