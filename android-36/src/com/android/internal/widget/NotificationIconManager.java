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

package com.android.internal.widget;

import android.graphics.drawable.Icon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An interface used for Notification views to delegate handling the loading of icons.
 */
public interface NotificationIconManager {

    /**
     * Called when a new icon is provided to display.
     *
     * @param drawableConsumer a consumer, which can display the loaded drawable.
     * @param icon             the updated icon to be displayed.
     *
     * @return a {@link Runnable} that sets the drawable on the consumer
     */
    @NonNull
    Runnable updateIcon(
            @NonNull NotificationDrawableConsumer drawableConsumer,
            @Nullable Icon icon
    );
}
