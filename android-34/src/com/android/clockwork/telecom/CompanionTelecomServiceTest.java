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

package com.android.clockwork.telecom;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.telephony.emergency.EmergencyNumber;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class CompanionTelecomServiceTest {

    private static Integer sSubscriptionId = 1;
    private static String sNumber = "911";
    private static String sIso = "us";
    private static String sMnc = "320123";
    private static int sCategory = 0;
    private static List<String> sUrns = new ArrayList<>();
    private static int sSources = 1;
    private static int sRouting = 2;

    private CompanionTelecomService mCompanionTelecomService;
    @Mock EmergencyNumberDbHelper mMockEmergencyNumberDbHelper;

    @Before
    public void saetup() {
        initMocks(this);
        mCompanionTelecomService = new CompanionTelecomService(mMockEmergencyNumberDbHelper);
    }

    @Test
    public void setEmergencyNumbers() {
        Map<Integer, List<EmergencyNumber>> input = getTestMap();
        mCompanionTelecomService.setEmergencyNumbers(input);

        Map<Integer, List<EmergencyNumber>> result = mCompanionTelecomService.getEmergencyNumbers();
        assertTrue(input.equals(result));
    }

    @Test
    public void isEmergencyNumber() {
        Map<Integer, List<EmergencyNumber>> input = getTestMap();
        mCompanionTelecomService.setEmergencyNumbers(input);

        assertTrue(mCompanionTelecomService.isEmergencyNumber("911"));
        verify(mMockEmergencyNumberDbHelper).updateEmergencyNumbers(input);
    }

    private Map<Integer, List<EmergencyNumber>> getTestMap() {
        HashMap<Integer, List<EmergencyNumber>> temp = new HashMap<>();
        temp.put(sSubscriptionId, new ArrayList<>());
        temp.get(sSubscriptionId)
                .add(
                        new EmergencyNumber(
                                sNumber, sIso, sMnc, sCategory, sUrns, sSources, sRouting));
        return temp;
    }
}
