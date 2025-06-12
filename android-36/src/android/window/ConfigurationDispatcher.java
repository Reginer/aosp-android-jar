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
import android.content.res.Configuration;

/**
 * Indicates a {@link android.content.Context} could propagate the
 * {@link android.content.res.Configuration} from the server side and users may listen to the
 * updates through {@link android.content.Context#registerComponentCallbacks(ComponentCallbacks)}.
 *
 * @hide
 */
public interface ConfigurationDispatcher {

    /**
     * Called when there's configuration update from the server side.
     */
    void dispatchConfigurationChanged(@NonNull Configuration configuration);

    /**
     * Indicates that if this dispatcher should report the change even if it's not
     * {@link Configuration#diffPublicOnly}.
     */
    default boolean shouldReportPrivateChanges() {
        return false;
    }
}
