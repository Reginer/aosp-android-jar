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

package android.provider;

import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

/**
 * Delegate that provides alternative implementation for methods in {@link DeviceConfig}
 * <p/>
 * Through the layoutlib_create tool, selected methods of DeviceConfig have been replaced by
 * calls to methods of the same name in this delegate class.
 */
public class DeviceConfig_Delegate {

    @LayoutlibDelegate
    public static String getString(String namespace, String name, String defaultValue) {
        return defaultValue;
    }

    @LayoutlibDelegate
    public static boolean getBoolean(String namespace, String name, boolean defaultValue) {
        return defaultValue;
    }

    @LayoutlibDelegate
    public static int getInt(String namespace, String name, int defaultValue) {
        return defaultValue;
    }

    @LayoutlibDelegate
    public static long getLong(String namespace, String name, long defaultValue) {
        return defaultValue;
    }

    @LayoutlibDelegate
    public static float getFloat(String namespace, String name, float defaultValue) {
        return defaultValue;
    }
}
