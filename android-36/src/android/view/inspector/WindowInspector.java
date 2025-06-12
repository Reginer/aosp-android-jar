/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.view.inspector;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.view.View;
import android.view.WindowManagerGlobal;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Provides access to window inspection information.
 */
public final class WindowInspector {
    private WindowInspector() {
        // Non-instantiable.
    }

    /**
     * @return the list of all window views attached to the current process
     */
    @NonNull
    public static List<View> getGlobalWindowViews() {
        return WindowManagerGlobal.getInstance().getWindowViews();
    }

    /**
     * Adds a listener that is notified whenever the value of {@link #getGlobalWindowViews()}
     * changes. The current value is provided immediately using the provided {@link Executor}.
     * If this {@link Consumer} is already registered, then this method is a no op.
     * @see #getGlobalWindowViews()
     */
    @FlaggedApi(android.view.flags.Flags.FLAG_ROOT_VIEW_CHANGED_LISTENER)
    public static void addGlobalWindowViewsListener(@NonNull Executor executor,
            @NonNull Consumer<List<View>> consumer) {
        WindowManagerGlobal.getInstance().addWindowViewsListener(executor, consumer);
    }

    /**
     * Removes a listener from getting notifications of global window views changes. If the
     * {@link Consumer} is not registered this method is a no op.
     * @see #addGlobalWindowViewsListener(Executor, Consumer)
     */
    @FlaggedApi(android.view.flags.Flags.FLAG_ROOT_VIEW_CHANGED_LISTENER)
    public static void removeGlobalWindowViewsListener(@NonNull Consumer<List<View>> consumer) {
        WindowManagerGlobal.getInstance().removeWindowViewsListener(consumer);
    }
}
