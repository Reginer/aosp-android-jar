/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.layoutlib.bridge.android.support;

import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.layoutlib.bridge.Bridge;
import com.android.layoutlib.bridge.util.ReflectionUtils.ReflectionException;

import android.content.Context;
import android.widget.TabHost;

import static com.android.layoutlib.bridge.util.ReflectionUtils.getCause;
import static com.android.layoutlib.bridge.util.ReflectionUtils.getMethod;
import static com.android.layoutlib.bridge.util.ReflectionUtils.invoke;

/**
 * Utility class for working with android.support.v4.app.FragmentTabHost
 */
public class FragmentTabHostUtil {

    public static final String[] CN_FRAGMENT_TAB_HOST = {
            "android.support.v4.app.FragmentTabHost",
            "androidx.fragment.app.FragmentTabHost"
    };

    private static final String[] CN_FRAGMENT_MANAGER = {
            "android.support.v4.app.FragmentManager",
            "androidx.fragment.app.FragmentManager"
    };

    /**
     * Calls the setup method for the FragmentTabHost tabHost
     */
    public static void setup(TabHost tabHost, Context context) {
        Class<?> fragmentManager = null;

        for (int i = CN_FRAGMENT_MANAGER.length - 1; i >= 0; i--) {
            String className = CN_FRAGMENT_MANAGER[i];
            try {
                fragmentManager = Class.forName(className, true,
                        tabHost.getClass().getClassLoader());
                break;
            } catch (ClassNotFoundException ignore) {
            }
        }

        if (fragmentManager == null) {
            Bridge.getLog().error(ILayoutLog.TAG_BROKEN,
                    "Unable to find FragmentManager.", null, null);
            return;
        }

        try {
            invoke(getMethod(tabHost.getClass(), "setup", Context.class,
                    fragmentManager, int.class), tabHost, context, null,
                    android.R.id.tabcontent);
        } catch (ReflectionException e) {
            Throwable cause = getCause(e);
            Bridge.getLog().error(ILayoutLog.TAG_BROKEN,
                    "Error occurred while trying to setup FragmentTabHost.", cause, null, null);
        }
    }
}
