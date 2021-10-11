/*
 * Copyright (C) 2015 The Android Open Source Project
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
import com.android.ide.common.rendering.api.LayoutlibCallback;
import com.android.layoutlib.bridge.Bridge;
import com.android.layoutlib.bridge.android.BridgeContext;
import com.android.layoutlib.bridge.android.RenderParamsFlags;
import com.android.layoutlib.bridge.util.ReflectionUtils;
import com.android.layoutlib.bridge.util.ReflectionUtils.ReflectionException;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.view.View;

import static com.android.layoutlib.bridge.util.ReflectionUtils.getCause;
import static com.android.layoutlib.bridge.util.ReflectionUtils.getMethod;
import static com.android.layoutlib.bridge.util.ReflectionUtils.invoke;

/**
 * Utility class for working with android.support.v7.widget.RecyclerView and
 * androidx.widget.RecyclerView
 */
public class RecyclerViewUtil {
    public static final String[] CN_RECYCLER_VIEW = {
            "android.support.v7.widget.RecyclerView",
            "androidx.recyclerview.widget.RecyclerView"
    };

    private static final Class<?>[] LLM_CONSTRUCTOR_SIGNATURE = new Class<?>[]{Context.class};

    /**
     * Tries to create an Adapter ({@code android.support.v7.widget.RecyclerView.Adapter} and a
     * LayoutManager {@code RecyclerView.LayoutManager} and assign these to the {@code RecyclerView}
     * that is passed.
     * <p/>
     * Any exceptions thrown during the process are logged in {@link Bridge#getLog()}
     */
    public static void setAdapter(@NonNull View recyclerView, @NonNull BridgeContext context,
            @NonNull LayoutlibCallback layoutlibCallback, int adapterLayout, int itemCount) {
        String recyclerViewClassName =
                ReflectionUtils.getParentClass(recyclerView, RecyclerViewUtil.CN_RECYCLER_VIEW);
        String adapterClassName = recyclerViewClassName + "$Adapter";
        String layoutMgrClassName = recyclerViewClassName + "$LayoutManager";

        try {
            setLayoutManager(recyclerView, layoutMgrClassName, context, layoutlibCallback);
            Object adapter = createAdapter(layoutlibCallback, adapterClassName);
            if (adapter != null) {
                setProperty(recyclerView, adapterClassName, adapter, "setAdapter");
                setProperty(adapter, int.class, adapterLayout, "setLayoutId");

                if (itemCount != -1) {
                    setProperty(adapter, int.class, itemCount, "setItemCount");
                }
            }
        } catch (ReflectionException e) {
            Throwable cause = getCause(e);
            Bridge.getLog().error(ILayoutLog.TAG_BROKEN,
                    "Error occurred while trying to setup RecyclerView.", cause, null, null);
        }
    }

    private static void setLayoutManager(@NonNull View recyclerView,
            @NonNull String layoutMgrClassName, @NonNull BridgeContext context,
            @NonNull LayoutlibCallback callback) throws ReflectionException {
        if (getLayoutManager(recyclerView) == null) {
            String linearLayoutMgrClassManager =
                    recyclerView.getClass().getPackage().getName() + ".LinearLayoutManager";
            // Only set the layout manager if not already set by the recycler view.
            Object layoutManager =
                    createLayoutManager(context, linearLayoutMgrClassManager, callback);
            if (layoutManager != null) {
                setProperty(recyclerView, layoutMgrClassName, layoutManager, "setLayoutManager");
            }
        }
    }

    /** Creates a LinearLayoutManager using the provided context. */
    @Nullable
    private static Object createLayoutManager(@NonNull Context context,
            @NonNull String linearLayoutMgrClassName, @NonNull LayoutlibCallback callback)
            throws ReflectionException {
        try {
            return callback.loadClass(linearLayoutMgrClassName, LLM_CONSTRUCTOR_SIGNATURE,
                    new Object[]{context});
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    @Nullable
    private static Object getLayoutManager(View recyclerView) throws ReflectionException {
        return invoke(getMethod(recyclerView.getClass(), "getLayoutManager"), recyclerView);
    }

    @Nullable
    private static Object createAdapter(@NonNull LayoutlibCallback layoutlibCallback,
            @NonNull String adapterClassName) throws ReflectionException {
        Boolean ideSupport =
                layoutlibCallback.getFlag(RenderParamsFlags.FLAG_KEY_RECYCLER_VIEW_SUPPORT);
        if (ideSupport != Boolean.TRUE) {
            return null;
        }
        try {
            return layoutlibCallback.loadClass(adapterClassName, new Class[0], new Object[0]);
        } catch (Exception e) {
            throw new ReflectionException(e);
        }
    }

    private static void setProperty(@NonNull Object object, @NonNull String propertyClassName,
            @NonNull Object propertyValue, @NonNull String propertySetter)
            throws ReflectionException {
        Class<?> propertyClass = ReflectionUtils.getClassInstance(propertyValue, propertyClassName);
        setProperty(object, propertyClass, propertyValue, propertySetter);
    }

    private static void setProperty(@NonNull Object object, @NonNull Class<?> propertyClass,
            @Nullable Object propertyValue, @NonNull String propertySetter)
            throws ReflectionException {
        invoke(getMethod(object.getClass(), propertySetter, propertyClass), object, propertyValue);
    }

}
