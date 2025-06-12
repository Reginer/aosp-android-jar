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

package android.window;

import android.annotation.NonNull;
import android.content.ComponentCallbacks;
import android.content.ComponentCallbacksController;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;

import com.android.window.flags.Flags;

/**
 * System Context to be used for UI. This Context has resources that can be themed.
 *
 * @see android.app.ActivityThread#getSystemUiContext(int)
 *
 * @hide
 */
public class SystemUiContext extends ContextWrapper implements ConfigurationDispatcher {

    private final ComponentCallbacksController mCallbacksController =
            new ComponentCallbacksController();

    public SystemUiContext(Context base) {
        super(base);
        if (!Flags.trackSystemUiContextBeforeWms()) {
            throw new UnsupportedOperationException("SystemUiContext can only be used after"
                    + " flag is enabled.");
        }
    }

    @Override
    public void registerComponentCallbacks(@NonNull ComponentCallbacks callback) {
        mCallbacksController.registerCallbacks(callback);
    }

    @Override
    public void unregisterComponentCallbacks(@NonNull ComponentCallbacks callback) {
        mCallbacksController.unregisterCallbacks(callback);
    }

    /** Dispatch {@link Configuration} to each {@link ComponentCallbacks}. */
    @Override
    public void dispatchConfigurationChanged(@NonNull Configuration newConfig) {
        mCallbacksController.dispatchConfigurationChanged(newConfig);
    }

    @Override
    public boolean shouldReportPrivateChanges() {
        // We should report all config changes to update fields obtained from resources.
        return true;
    }
}
