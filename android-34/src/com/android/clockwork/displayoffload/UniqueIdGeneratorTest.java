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

package com.android.clockwork.displayoffload;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link com.google.android.clockwork.displayoffload.UniqueIdGenerator}.
 */
@RunWith(AndroidJUnit4.class)
public class UniqueIdGeneratorTest {
    private final Set<Integer> mReserved = new HashSet<>();
    private final UniqueIdGenerator mUniqueIdGenerator = new UniqueIdGenerator(this::isReserved);

    @Before
    public void setup() {
        mUniqueIdGenerator.reset();
        mReserved.clear();
    }

    @Test
    public void nextId_shouldCountFromZero() {
        assertThat(mUniqueIdGenerator.nextId()).isEqualTo(0);
    }

    @Test
    public void nextId_shouldGenerateDifferentIdForSubsequentCalls() {
        int i1 = mUniqueIdGenerator.nextId();
        int i2 = mUniqueIdGenerator.nextId();
        int i3 = mUniqueIdGenerator.nextId();
        int[] array = new int[]{i1, i2, i3};
        assertThat(Arrays.stream(array).distinct().toArray().length).isEqualTo(array.length);
    }

    @Test
    public void nextId_shouldAvoidReservedIds() {
        mReserved.add(0);
        mReserved.add(3);
        int i1 = mUniqueIdGenerator.nextId(); // 1
        int i2 = mUniqueIdGenerator.nextId(); // 2
        int i3 = mUniqueIdGenerator.nextId(); // 4
        assertThat(i1).isEqualTo(1);
        assertThat(i2).isEqualTo(2);
        assertThat(i3).isEqualTo(4);
    }

    @Test
    public void getId_shouldBeConsistent() {
        Object o1 = new Object();
        Object o2 = new Object();
        int i1 = mUniqueIdGenerator.getId(o1); // 1
        int i2 = mUniqueIdGenerator.getId(o2); // 2
        int i3 = mUniqueIdGenerator.getId(o1); // 1
        int i4 = mUniqueIdGenerator.getId(o2); // 2
        assertThat(i1).isEqualTo(i3);
        assertThat(i2).isEqualTo(i4);
    }

    @Test
    public void getIdAndNextId_shouldNotCollide() {
        int o1 = 1;
        int i1 = mUniqueIdGenerator.nextId();  // 0
        int i2 = mUniqueIdGenerator.nextId();  // 1
        int i3 = mUniqueIdGenerator.getId(o1); // 2
        int i4 = mUniqueIdGenerator.nextId();  // 3
        assertThat(i1).isEqualTo(0);
        assertThat(i2).isEqualTo(1);
        assertThat(i3).isEqualTo(2);
        assertThat(i4).isEqualTo(3);
    }

    @Test
    public void getId_shouldAllowExplicitReservedIdPassthrough() {
        int explicitPassthrough = 42;
        mReserved.add(explicitPassthrough);
        assertThat(mUniqueIdGenerator.getId(explicitPassthrough)).isEqualTo(explicitPassthrough);
    }

    @Test
    public void getIdAndNextId_shouldAvoidReservedIds() {
        Object o1 = new Object();
        Object o2 = new Object();
        Object o3 = new Object();
        mReserved.add(0);
        mReserved.add(3);
        int i1 = mUniqueIdGenerator.getId(o1); // 1
        int i2 = mUniqueIdGenerator.getId(o2); // 2
        int i3 = mUniqueIdGenerator.nextId(); // 4
        int i4 = mUniqueIdGenerator.getId(o3); // 5
        int i5 = mUniqueIdGenerator.nextId(); // 6
        assertThat(i1).isEqualTo(1);
        assertThat(i2).isEqualTo(2);
        assertThat(i3).isEqualTo(4);
        assertThat(i4).isEqualTo(5);
        assertThat(i5).isEqualTo(6);
    }

    public boolean isReserved(Integer id) {
        return mReserved.contains(id);
    }
}
