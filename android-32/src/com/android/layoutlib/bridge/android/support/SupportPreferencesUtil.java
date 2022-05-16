/*
 * Copyright (C) 2016 The Android Open Source Project
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

import com.android.ide.common.rendering.api.LayoutlibCallback;
import com.android.ide.common.rendering.api.RenderResources;
import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.StyleResourceValue;
import com.android.layoutlib.bridge.android.BridgeContext;
import com.android.layoutlib.bridge.android.BridgeXmlBlockParser;
import com.android.layoutlib.bridge.util.ReflectionUtils.ReflectionException;
import com.android.resources.ResourceType;
import com.android.tools.layoutlib.annotations.NotNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static com.android.layoutlib.bridge.util.ReflectionUtils.getAccessibleMethod;
import static com.android.layoutlib.bridge.util.ReflectionUtils.getClassInstance;
import static com.android.layoutlib.bridge.util.ReflectionUtils.getMethod;
import static com.android.layoutlib.bridge.util.ReflectionUtils.invoke;

/**
 * Class with utility methods to instantiate Preferences provided by the support library.
 * This class uses reflection to access the support preference objects so it heavily depends on
 * the API being stable.
 */
public class SupportPreferencesUtil {
    private static final String[] PREFERENCES_PKG_NAMES = {
            "android.support.v7.preference",
            "androidx.preference"
    };

    private SupportPreferencesUtil() {
    }

    @NonNull
    private static Object instantiateClass(@NonNull LayoutlibCallback callback,
            @NonNull String className, @Nullable Class[] constructorSignature,
            @Nullable Object[] constructorArgs) throws ReflectionException {
        try {
            Object instance = callback.loadClass(className, constructorSignature, constructorArgs);
            if (instance == null) {
                throw new ClassNotFoundException(className + " class not found");
            }
            return instance;
        } catch (ClassNotFoundException e) {
            throw new ReflectionException(e);
        }
    }

    @NonNull
    private static Object createPreferenceGroupAdapter(@NonNull LayoutlibCallback callback,
            @NonNull String preferenceGroupClassName,
            @NonNull String preferenceGroupAdapterClassName, @NonNull Object preferenceScreen)
            throws ReflectionException {
        Class<?> preferenceGroupClass =
                getClassInstance(preferenceScreen, preferenceGroupClassName);

        return instantiateClass(callback, preferenceGroupAdapterClassName,
                new Class[]{preferenceGroupClass}, new Object[]{preferenceScreen});
    }

    @NonNull
    private static Object createInflatedPreference(@NonNull LayoutlibCallback callback,
            @NonNull String preferenceGroupClassName, @NonNull String preferenceInflaterClassName,
            @NonNull Context context, @NonNull XmlPullParser parser,
            @NonNull Object preferenceScreen, @NonNull Object preferenceManager)
            throws ReflectionException {
        Class<?> preferenceGroupClass =
                getClassInstance(preferenceScreen, preferenceGroupClassName);
        Object preferenceInflater = instantiateClass(callback, preferenceInflaterClassName,
                new Class[]{Context.class, preferenceManager.getClass()},
                new Object[]{context, preferenceManager});
        Object inflatedPreference =
                invoke(getAccessibleMethod(preferenceInflater.getClass(), "inflate",
                        XmlPullParser.class, preferenceGroupClass), preferenceInflater, parser,
                        null);

        if (inflatedPreference == null) {
            throw new ReflectionException("inflate method returned null");
        }

        return inflatedPreference;
    }

    /**
     * Returns a themed wrapper context of {@link BridgeContext} with the theme specified in
     * ?attr/preferenceTheme applied to it.
     */
    @NotNull
    private static Context getThemedContext(@NonNull BridgeContext bridgeContext) {
        RenderResources resources = bridgeContext.getRenderResources();
        ResourceValue preferenceTheme = resources.findItemInTheme(
                bridgeContext.createAppCompatAttrReference("preferenceTheme"));

        if (preferenceTheme != null) {
            // resolve it, if needed.
            preferenceTheme = resources.resolveResValue(preferenceTheme);
        } else {
            // The current theme does not define "preferenceTheme" so we will use the default
            // "PreferenceThemeOverlay" if available.
            preferenceTheme = resources.getStyle(
                    bridgeContext.createAppCompatResourceReference(ResourceType.STYLE,
                            "PreferenceThemeOverlay"));
        }
        if (preferenceTheme instanceof StyleResourceValue) {
            int styleId = bridgeContext.getDynamicIdByStyle(((StyleResourceValue) preferenceTheme));
            if (styleId != 0) {
                return new ContextThemeWrapper(bridgeContext, styleId);
            }
        }

        // We were not able to find any preferences theme so return the original Context without
        // any theme wrapping
        return bridgeContext;
    }

    /**
     * Returns a {@link LinearLayout} containing all the UI widgets representing the preferences
     * passed in the group adapter.
     */
    @Nullable
    private static LinearLayout setUpPreferencesListView(@NonNull BridgeContext bridgeContext,
            @NonNull Context themedContext, @NonNull ArrayList<Object> viewCookie,
            @NonNull Object preferenceGroupAdapter) throws ReflectionException {
        // Setup the LinearLayout that will contain the preferences
        LinearLayout listView = new LinearLayout(themedContext);
        listView.setOrientation(LinearLayout.VERTICAL);
        listView.setLayoutParams(
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        if (!viewCookie.isEmpty()) {
            bridgeContext.addViewKey(listView, viewCookie.get(0));
        }

        // Get all the preferences and add them to the LinearLayout
        Integer preferencesCount =
                (Integer) invoke(getMethod(preferenceGroupAdapter.getClass(), "getItemCount"),
                        preferenceGroupAdapter);
        if (preferencesCount == null) {
            return listView;
        }

        Method getItemId = getMethod(preferenceGroupAdapter.getClass(), "getItemId", int.class);
        Method getItemViewType =
                getMethod(preferenceGroupAdapter.getClass(), "getItemViewType", int.class);
        Method onCreateViewHolder =
                getMethod(preferenceGroupAdapter.getClass(), "onCreateViewHolder", ViewGroup.class,
                        int.class);
        for (int i = 0; i < preferencesCount; i++) {
            Long id = (Long) invoke(getItemId, preferenceGroupAdapter, i);
            if (id == null) {
                continue;
            }

            // Get the type of the preference layout and bind it to a newly created view holder
            Integer type = (Integer) invoke(getItemViewType, preferenceGroupAdapter, i);
            Object viewHolder = invoke(onCreateViewHolder, preferenceGroupAdapter, listView, type);
            if (viewHolder == null) {
                continue;
            }
            invoke(getMethod(preferenceGroupAdapter.getClass(), "onBindViewHolder",
                    viewHolder.getClass(), int.class), preferenceGroupAdapter, viewHolder, i);

            try {
                // Get the view from the view holder and add it to our layout
                View itemView = (View) viewHolder.getClass().getField("itemView").get(viewHolder);

                int arrayPosition = id.intValue() - 1; // IDs are 1 based
                if (arrayPosition >= 0 && arrayPosition < viewCookie.size()) {
                    bridgeContext.addViewKey(itemView, viewCookie.get(arrayPosition));
                }
                listView.addView(itemView);
            } catch (IllegalAccessException | NoSuchFieldException ignored) {
            }
        }

        return listView;
    }

    /**
     * Inflates a preferences layout using the support library. If the support library is not
     * available, this method will return null without advancing the parsers.
     */
    @Nullable
    public static View inflatePreference(@NonNull BridgeContext bridgeContext,
            @NonNull XmlPullParser parser, @Nullable ViewGroup root) {
        String preferencePackageName = null;
        String preferenceManagerClassName = null;
        // Find the correct package for the classes
        for (int i = PREFERENCES_PKG_NAMES.length - 1; i >= 0; i--) {
            preferencePackageName = PREFERENCES_PKG_NAMES[i];
            preferenceManagerClassName = preferencePackageName + ".PreferenceManager";
            try {
                bridgeContext.getLayoutlibCallback().findClass(preferenceManagerClassName);
                break;
            } catch (ClassNotFoundException ignore) {
            }
        }

        assert preferencePackageName != null;
        String preferenceGroupClassName = preferencePackageName + ".PreferenceGroup";
        String preferenceGroupAdapterClassName = preferencePackageName + ".PreferenceGroupAdapter";
        String preferenceInflaterClassName = preferencePackageName + ".PreferenceInflater";

        try {
            LayoutlibCallback callback = bridgeContext.getLayoutlibCallback();
            Context context = getThemedContext(bridgeContext);

            // Create PreferenceManager
            Object preferenceManager = instantiateClass(callback, preferenceManagerClassName,
                    new Class[]{Context.class}, new Object[]{context});

            // From this moment on, we can assume that we found the support library and that
            // nothing should fail

            // Create PreferenceScreen
            Object preferenceScreen =
                    invoke(getMethod(preferenceManager.getClass(), "createPreferenceScreen",
                            Context.class), preferenceManager, context);
            if (preferenceScreen == null) {
                return null;
            }

            // Setup a parser that stores the list of cookies in the same order as the preferences
            // are inflated. That way we can later reconstruct the list using the preference id
            // since they are sequential and start in 1.
            ArrayList<Object> viewCookie = new ArrayList<>();
            if (parser instanceof BridgeXmlBlockParser) {
                // Setup a parser that stores the XmlTag
                parser =
                        new BridgeXmlBlockParser(
                                parser,
                                null,
                                ((BridgeXmlBlockParser) parser).getFileResourceNamespace()) {
                            @Override
                            public Object getViewCookie() {
                                return ((BridgeXmlBlockParser) getParser()).getViewCookie();
                            }

                            @Override
                            public int next() throws XmlPullParserException, IOException {
                                int ev = super.next();
                                if (ev == XmlPullParser.START_TAG) {
                                    viewCookie.add(this.getViewCookie());
                                }

                                return ev;
                            }
                        };
            }

            // Create the PreferenceInflater
            Object inflatedPreference = createInflatedPreference(callback, preferenceGroupClassName,
                    preferenceInflaterClassName, context, parser, preferenceScreen,
                    preferenceManager);

            // Setup the RecyclerView (set adapter and layout manager)
            Object preferenceGroupAdapter =
                    createPreferenceGroupAdapter(callback, preferenceGroupClassName,
                            preferenceGroupAdapterClassName, inflatedPreference);

            // Instead of just setting the group adapter as adapter for a RecyclerView, we manually
            // get all the items and add them to a LinearLayout. This allows us to set the view
            // cookies so the preferences are correctly linked to their XML.
            LinearLayout listView = setUpPreferencesListView(bridgeContext, context, viewCookie,
                    preferenceGroupAdapter);

            ScrollView scrollView = new ScrollView(context);
            scrollView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            scrollView.addView(listView);

            if (root != null) {
                root.addView(scrollView);
            }

            return scrollView;
        } catch (ReflectionException e) {
            return null;
        }
    }

    /**
     * Returns true if the given root tag is any of the support library {@code PreferenceScreen}
     * tags.
     */
    public static boolean isSupportRootTag(@Nullable String rootTag) {
        if (rootTag != null) {
            for (String supportPrefix : PREFERENCES_PKG_NAMES) {
                if (rootTag.equals(supportPrefix + ".PreferenceScreen")) {
                    return true;
                }
            }
        }

        return false;
    }
}
