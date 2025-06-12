/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.nfc;

import android.app.Application;
import android.compat.annotation.UnsupportedAppUsage;
import android.util.Log;

/**
 * NFC State associated with an {@link Application}.
 *
 * @hide
 */
public class NfcApplicationState {
    static final String TAG = NfcAdapter.TAG;
    /**
     * @hide
     */
    @UnsupportedAppUsage
    int refCount = 0;
    /**
     * @hide
     */
    @UnsupportedAppUsage
    final Application app;
    /**
     * @hide
     */
    @UnsupportedAppUsage
    private final NfcActivityManager nfcActivityManager;

    /**
     * @hide
     */
    public NfcApplicationState(Application app, NfcActivityManager activityManager) {
        this.app = app;
        this.nfcActivityManager = activityManager;
    }

    /**
     * @hide
     */
    public void register() {
        refCount++;
        if (refCount == 1) {
            this.app.registerActivityLifecycleCallbacks(nfcActivityManager);
        }
    }

    /**
     * @hide
     */
    public void unregister() {
        refCount--;
        if (refCount == 0) {
            this.app.unregisterActivityLifecycleCallbacks(nfcActivityManager);
        } else if (refCount < 0) {
            Log.e(TAG, "-ve refcount for " + app);
        }
    }
}
