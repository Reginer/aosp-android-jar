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

package com.android.internal.annotations;

import androidx.test.filters.SmallTest;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
@SmallTest
@Keep
public class KeepForWeakReferenceTest extends TestCase {
    @Test
    public void testAnnotatedMemberKept() throws Exception {
        // Note: This code is simply to exercise the class behavior to ensure
        // that it's kept during compilation.
        List<WeakReference<Object>> weakRefs = new ArrayList<>();
        ClassWithWeaklyReferencedField instance = new ClassWithWeaklyReferencedField(weakRefs);

        // Ensure annotated fields are kept.
        // Note: We use an intermediate string field variable to avoid R8 using the reflection
        // call itself (with the string constant) as an implicit Keep signal.
        String[] keptFields = {"mKeptField"};
        for (String field : keptFields) {
            assertTrue(instance.getClass().getDeclaredField(field) != null);
        }
    }
}
