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

package com.android.modules.utils.build;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.util.SparseArray;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class UnboundedSdkLevelTest {
    private static final SparseArray<Set<String>> PREVIOUS_CODENAMES = new SparseArray<>();

    static {
        PREVIOUS_CODENAMES.put(29, Set.of("Q"));
        PREVIOUS_CODENAMES.put(30, Set.of("Q", "R"));
        PREVIOUS_CODENAMES.put(31, Set.of("Q", "R", "S"));
        PREVIOUS_CODENAMES.put(32, Set.of("Q", "R", "S", "Sv2"));
    }

    @Test
    public void testFinalizedSdk_S() {
        // Test against finalized S / 31 release
        UnboundedSdkLevel sdkLevel = new UnboundedSdkLevel(31, "REL", PREVIOUS_CODENAMES.get(31));

        assertThat(sdkLevel.isAtLeastInternal("30")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("31")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("32")).isFalse();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal("R"));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal("S"));
        assertThat(sdkLevel.isAtLeastInternal("Sv2")).isFalse();
        assertThat(sdkLevel.isAtLeastInternal("Tiramisu")).isFalse();
        assertThat(sdkLevel.isAtLeastInternal("U")).isFalse();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal("current"));

        assertThat(sdkLevel.isAtMostInternal("30")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("31")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("32")).isTrue();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal("R"));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal("S"));
        assertThat(sdkLevel.isAtMostInternal("Sv2")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("Tiramisu")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("U")).isTrue();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal("current"));
    }

    @Test
    public void testNonFinalizedSdk_Sv2() {
        UnboundedSdkLevel sdkLevel = new UnboundedSdkLevel(31, "Sv2", Set.of("Q", "R", "S", "Sv2"));

        assertThat(sdkLevel.isAtLeastInternal("30")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("31")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("32")).isFalse();

        assertThat(sdkLevel.isAtLeastInternal("R")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("S")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("Sv2")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("Tiramisu")).isFalse();
        assertThat(sdkLevel.isAtLeastInternal("U")).isFalse();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal("current"));

        assertThat(sdkLevel.isAtMostInternal("30")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("31")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("32")).isTrue();

        assertThat(sdkLevel.isAtMostInternal("R")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("S")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("Sv2")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("Tiramisu")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("U")).isTrue();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal("current"));
    }

    @Test
    public void testNonFinalizedSdk_Tiramisu() {
        UnboundedSdkLevel sdkLevel = new UnboundedSdkLevel(31, "Tiramisu",
                Set.of("Q", "R", "S", "Sv2", "Tiramisu"));

        assertThat(sdkLevel.isAtLeastInternal("30")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("31")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("32")).isFalse();

        assertThat(sdkLevel.isAtLeastInternal("R")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("S")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("Sv2")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("Tiramisu")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("U")).isFalse();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtLeastInternal("current"));

        assertThat(sdkLevel.isAtMostInternal("30")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("31")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("32")).isTrue();

        assertThat(sdkLevel.isAtMostInternal("R")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("S")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("Sv2")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("Tiramisu")).isTrue();
        assertThat(sdkLevel.isAtMostInternal("U")).isTrue();

        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevel.isAtMostInternal("current"));
    }

    @Test
    public void testLegacySdkR() {
        UnboundedSdkLevel sdkLevelR = new UnboundedSdkLevel(30, "REL",
                PREVIOUS_CODENAMES.get(30));
        assertThat(sdkLevelR.isAtLeastInternal("29")).isTrue();
        assertThat(sdkLevelR.isAtLeastInternal("30")).isTrue();
        assertThat(sdkLevelR.isAtLeastInternal("31")).isFalse();

        assertThrows(IllegalArgumentException.class,
                () -> assertThat(sdkLevelR.isAtLeastInternal("Q")));
        assertThrows(IllegalArgumentException.class,
                () -> assertThat(sdkLevelR.isAtLeastInternal("R")));
        assertThat(sdkLevelR.isAtLeastInternal("S")).isFalse();
        assertThat(sdkLevelR.isAtLeastInternal("Sv2")).isFalse();
        assertThat(sdkLevelR.isAtLeastInternal("Tiramisu")).isFalse();

        assertThat(sdkLevelR.isAtMostInternal("29")).isFalse();
        assertThat(sdkLevelR.isAtMostInternal("30")).isTrue();
        assertThat(sdkLevelR.isAtMostInternal("31")).isTrue();

        assertThrows(IllegalArgumentException.class,
                () -> assertThat(sdkLevelR.isAtMostInternal("Q")));
        assertThrows(IllegalArgumentException.class,
                () -> assertThat(sdkLevelR.isAtMostInternal("R")));
        assertThat(sdkLevelR.isAtMostInternal("S")).isTrue();
        assertThat(sdkLevelR.isAtMostInternal("Sv2")).isTrue();
        assertThat(sdkLevelR.isAtMostInternal("Tiramisu")).isTrue();

        assertThrows(IllegalArgumentException.class, () -> sdkLevelR.isAtMostInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevelR.isAtMostInternal("current"));
    }

    @Test
    public void testLegacySdkQ() {
        UnboundedSdkLevel sdkLevelQ = new UnboundedSdkLevel(29, "REL",
                PREVIOUS_CODENAMES.get(29));

        assertThat(sdkLevelQ.isAtLeastInternal("28")).isTrue();
        assertThat(sdkLevelQ.isAtLeastInternal("29")).isTrue();
        assertThat(sdkLevelQ.isAtLeastInternal("30")).isFalse();
        assertThat(sdkLevelQ.isAtLeastInternal("31")).isFalse();

        assertThrows(IllegalArgumentException.class,
                () -> assertThat(sdkLevelQ.isAtLeastInternal("Q")));
        assertThat(sdkLevelQ.isAtLeastInternal("R")).isFalse();
        assertThat(sdkLevelQ.isAtLeastInternal("S")).isFalse();
        assertThat(sdkLevelQ.isAtLeastInternal("Sv2")).isFalse();
        assertThat(sdkLevelQ.isAtLeastInternal("Tiramisu")).isFalse();

        assertThat(sdkLevelQ.isAtMostInternal("28")).isFalse();
        assertThat(sdkLevelQ.isAtMostInternal("29")).isTrue();
        assertThat(sdkLevelQ.isAtMostInternal("30")).isTrue();
        assertThat(sdkLevelQ.isAtMostInternal("31")).isTrue();

        assertThrows(IllegalArgumentException.class,
                () -> assertThat(sdkLevelQ.isAtMostInternal("Q")));
        assertThat(sdkLevelQ.isAtMostInternal("R")).isTrue();
        assertThat(sdkLevelQ.isAtMostInternal("S")).isTrue();
        assertThat(sdkLevelQ.isAtMostInternal("Sv2")).isTrue();
        assertThat(sdkLevelQ.isAtMostInternal("Tiramisu")).isTrue();

        assertThrows(IllegalArgumentException.class, () -> sdkLevelQ.isAtMostInternal(""));
        assertThrows(IllegalArgumentException.class, () -> sdkLevelQ.isAtMostInternal("current"));
    }

    @Test
    public void testCodenamesWithFingerprint() {
        UnboundedSdkLevel sdkLevel = new UnboundedSdkLevel(30, "R",
            PREVIOUS_CODENAMES.get(30));

        assertThat(sdkLevel.isAtMostInternal("Q")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("Tiramisu")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("Q")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("Tiramisu")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("Q.fingerprint")).isFalse();
        assertThat(sdkLevel.isAtMostInternal("Tiramisu.fingerprint")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("Q.fingerprint")).isTrue();
        assertThat(sdkLevel.isAtLeastInternal("Tiramisu.fingerprint")).isFalse();
    }
}
