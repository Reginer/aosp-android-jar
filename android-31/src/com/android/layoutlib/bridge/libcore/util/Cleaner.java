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
 * limitations under the License.
 */

package com.android.layoutlib.bridge.libcore.util;

import java.lang.ref.Cleaner.Cleanable;

public class Cleaner {
    private final Cleanable mCleanable;

    private Cleaner(Cleanable cleanable) {
        mCleanable = cleanable;
    }

    /**
     * Creates a new cleaner.
     *
     * @param  ob the referent object to be cleaned
     * @param  thunk
     *         The cleanup code to be run when the cleaner is invoked.  The
     *         cleanup code is run directly from the reference-handler thread,
     *         so it should be as simple and straightforward as possible.
     *
     * @return  The new cleaner
     */
    public static Cleaner create(Object ob, Runnable thunk) {
        if (thunk == null)
            return null;
        java.lang.ref.Cleaner cleaner = java.lang.ref.Cleaner.create();
        return new Cleaner(cleaner.register(ob, thunk));
    }

    /**
     * Runs this cleaner, if it has not been run before.
     */
    public void clean() {
        mCleanable.clean();
    }
}
