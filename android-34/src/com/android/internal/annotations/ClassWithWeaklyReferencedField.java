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

import java.lang.ref.WeakReference;
import java.util.List;

public class ClassWithWeaklyReferencedField {

    // Without this annotation, `mKeptField` could be optimized away after
    // tree shaking.
    @KeepForWeakReference private final Object mKeptField = new Integer(1);

    public ClassWithWeaklyReferencedField(List<WeakReference<Object>> weakRefs) {
        weakRefs.add(new WeakReference<>(mKeptField));
    }
}
