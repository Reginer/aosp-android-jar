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

package android.health.connect.datatypes;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.Bitmap;

import java.util.Objects;

/** Application Info class containing details about a given application */
public final class AppInfo {
    /** Application name/label */
    private final String mName;

    /** Application icon as bitmap */
    private final Bitmap mIcon;

    /** Application package name */
    private final String mPackageName;

    /**
     * Builder for {@link AppInfo}
     *
     * @hide
     */
    public static final class Builder {
        private final String mPackageName;
        private final String mName;
        private final Bitmap mIcon;

        /**
         * @param packageName package name of the application
         * @param name name/label of the application. Optional
         * @param icon icon of the application. Optional.
         */
        public Builder(@NonNull String packageName, @Nullable String name, @Nullable Bitmap icon) {
            Objects.requireNonNull(packageName);
            mPackageName = packageName;
            mName = name;
            mIcon = icon;
        }

        /**
         * @return Object of {@link AppInfo}
         */
        @NonNull
        public AppInfo build() {
            return new AppInfo(mPackageName, mName, mIcon);
        }
    }

    private AppInfo(@NonNull String packageName, @Nullable String name, @Nullable Bitmap icon) {
        Objects.requireNonNull(packageName);
        mPackageName = packageName;
        mName = name;
        mIcon = icon;
    }

    /** Returns the application package name */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /** Returns the application icon as bitmap */
    @Nullable
    public Bitmap getIcon() {
        return mIcon;
    }

    /** Returns the application name/label */
    @Nullable
    public String getName() {
        return mName;
    }
}
