/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.layoutlib.bridge.android;

import com.android.SdkConstants;
import com.android.ide.common.rendering.api.AssetRepository;
import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.ide.common.rendering.api.ILayoutPullParser;
import com.android.ide.common.rendering.api.LayoutlibCallback;
import com.android.ide.common.rendering.api.RenderResources;
import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.ide.common.rendering.api.ResourceNamespace.Resolver;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.ResourceValueImpl;
import com.android.ide.common.rendering.api.StyleResourceValue;
import com.android.layoutlib.bridge.Bridge;
import com.android.layoutlib.bridge.BridgeConstants;
import com.android.layoutlib.bridge.impl.ParserFactory;
import com.android.layoutlib.bridge.impl.ResourceHelper;
import com.android.layoutlib.bridge.impl.Stack;
import com.android.resources.ResourceType;
import com.android.utils.Pair;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.app.ActivityManager_Accessor;
import android.app.SystemServiceRegistry;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.ComponentCallbacks;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.BridgeAssetManager;
import android.content.res.BridgeTypedArray;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.Resources_Delegate;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.BridgeInflater;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.view.accessibility.AccessibilityManager;
import android.view.autofill.AutofillManager;
import android.view.autofill.IAutoFillManager.Default;
import android.view.inputmethod.InputMethodManager;
import android.view.textservice.TextServicesManager;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static android.os._Original_Build.VERSION_CODES.JELLY_BEAN_MR1;
import static com.android.layoutlib.bridge.android.RenderParamsFlags.FLAG_KEY_APPLICATION_PACKAGE;

/**
 * Custom implementation of Context/Activity to handle non compiled resources.
 */
public class BridgeContext extends Context {
    private static final String PREFIX_THEME_APPCOMPAT = "Theme.AppCompat";

    private static final Map<String, ResourceValue> FRAMEWORK_PATCHED_VALUES = new HashMap<>(2);
    private static final Map<String, ResourceValue> FRAMEWORK_REPLACE_VALUES = new HashMap<>(3);

    static {
        FRAMEWORK_PATCHED_VALUES.put("animateFirstView",
                new ResourceValueImpl(ResourceNamespace.ANDROID, ResourceType.BOOL,
                        "animateFirstView", "false"));
        FRAMEWORK_PATCHED_VALUES.put("animateLayoutChanges",
                new ResourceValueImpl(ResourceNamespace.ANDROID, ResourceType.BOOL,
                        "animateLayoutChanges", "false"));


        FRAMEWORK_REPLACE_VALUES.put("textEditSuggestionItemLayout",
                new ResourceValueImpl(ResourceNamespace.ANDROID, ResourceType.LAYOUT,
                        "textEditSuggestionItemLayout", "text_edit_suggestion_item"));
        FRAMEWORK_REPLACE_VALUES.put("textEditSuggestionContainerLayout",
                new ResourceValueImpl(ResourceNamespace.ANDROID, ResourceType.LAYOUT,
                        "textEditSuggestionContainerLayout", "text_edit_suggestion_container"));
        FRAMEWORK_REPLACE_VALUES.put("textEditSuggestionHighlightStyle",
                new ResourceValueImpl(ResourceNamespace.ANDROID, ResourceType.STYLE,
                        "textEditSuggestionHighlightStyle",
                        "TextAppearance.Holo.SuggestionHighlight"));
    }

    /** The map adds cookies to each view so that IDE can link xml tags to views. */
    private final HashMap<View, Object> mViewKeyMap = new HashMap<>();
    /**
     * In some cases, when inflating an xml, some objects are created. Then later, the objects are
     * converted to views. This map stores the mapping from objects to cookies which can then be
     * used to populate the mViewKeyMap.
     */
    private final HashMap<Object, Object> mViewKeyHelpMap = new HashMap<>();
    private final BridgeAssetManager mAssets;
    private final boolean mShadowsEnabled;
    private final boolean mHighQualityShadows;
    private Resources mSystemResources;
    private final Object mProjectKey;
    private final DisplayMetrics mMetrics;
    private final RenderResources mRenderResources;
    private final Configuration mConfig;
    private final ApplicationInfo mApplicationInfo;
    private final LayoutlibCallback mLayoutlibCallback;
    private final WindowManager mWindowManager;
    private final DisplayManager mDisplayManager;
    private final AutofillManager mAutofillManager;
    private final ClipboardManager mClipboardManager;
    private final ActivityManager mActivityManager;
    private final ConnectivityManager mConnectivityManager;
    private final HashMap<View, Integer> mScrollYPos = new HashMap<>();
    private final HashMap<View, Integer> mScrollXPos = new HashMap<>();

    private Resources.Theme mTheme;

    private final Map<Object, Map<ResourceReference, ResourceValue>> mDefaultPropMaps =
            new IdentityHashMap<>();
    private final Map<Object, ResourceReference> mDefaultStyleMap = new IdentityHashMap<>();

    // cache for TypedArray generated from StyleResourceValue object
    private TypedArrayCache mTypedArrayCache;
    private BridgeInflater mBridgeInflater;

    private BridgeContentResolver mContentResolver;

    private final Stack<BridgeXmlBlockParser> mParserStack = new Stack<>();
    private SharedPreferences mSharedPreferences;
    private ClassLoader mClassLoader;
    private IBinder mBinder;
    private PackageManager mPackageManager;
    private Boolean mIsThemeAppCompat;
    private final ResourceNamespace mAppCompatNamespace;
    private final Map<Key<?>, Object> mUserData = new HashMap<>();

    /**
     * Some applications that target both pre API 17 and post API 17, set the newer attrs to
     * reference the older ones. For example, android:paddingStart will resolve to
     * android:paddingLeft. This way the apps need to only define paddingLeft at any other place.
     * This a map from value to attribute name. Warning for missing references shouldn't be logged
     * if value and attr name pair is the same as an entry in this map.
     */
    private static Map<String, String> RTL_ATTRS = new HashMap<>(10);

    static {
        RTL_ATTRS.put("?android:attr/paddingLeft", "paddingStart");
        RTL_ATTRS.put("?android:attr/paddingRight", "paddingEnd");
        RTL_ATTRS.put("?android:attr/layout_marginLeft", "layout_marginStart");
        RTL_ATTRS.put("?android:attr/layout_marginRight", "layout_marginEnd");
        RTL_ATTRS.put("?android:attr/layout_toLeftOf", "layout_toStartOf");
        RTL_ATTRS.put("?android:attr/layout_toRightOf", "layout_toEndOf");
        RTL_ATTRS.put("?android:attr/layout_alignParentLeft", "layout_alignParentStart");
        RTL_ATTRS.put("?android:attr/layout_alignParentRight", "layout_alignParentEnd");
        RTL_ATTRS.put("?android:attr/drawableLeft", "drawableStart");
        RTL_ATTRS.put("?android:attr/drawableRight", "drawableEnd");
    }

    /**
     * @param projectKey An Object identifying the project. This is used for the cache mechanism.
     * @param metrics the {@link DisplayMetrics}.
     * @param renderResources the configured resources (both framework and projects) for this
     * render.
     * @param config the Configuration object for this render.
     * @param targetSdkVersion the targetSdkVersion of the application.
     */
    public BridgeContext(Object projectKey, @NonNull DisplayMetrics metrics,
            @NonNull RenderResources renderResources,
            @NonNull AssetRepository assets,
            @NonNull LayoutlibCallback layoutlibCallback,
            @NonNull Configuration config,
            int targetSdkVersion,
            boolean hasRtlSupport,
            boolean shadowsEnabled,
            boolean highQualityShadows) {
        mProjectKey = projectKey;
        mMetrics = metrics;
        mLayoutlibCallback = layoutlibCallback;

        mRenderResources = renderResources;
        mConfig = config;
        AssetManager systemAssetManager = AssetManager.getSystem();
        if (systemAssetManager instanceof BridgeAssetManager) {
            mAssets = (BridgeAssetManager) systemAssetManager;
        } else {
            throw new AssertionError("Creating BridgeContext without initializing Bridge");
        }
        mAssets.setAssetRepository(assets);

        mApplicationInfo = new ApplicationInfo();
        mApplicationInfo.targetSdkVersion = targetSdkVersion;
        if (hasRtlSupport) {
            mApplicationInfo.flags = mApplicationInfo.flags | ApplicationInfo.FLAG_SUPPORTS_RTL;
        }

        mWindowManager = new WindowManagerImpl(this, mMetrics);
        mDisplayManager = new DisplayManager(this);
        mAutofillManager = new AutofillManager(this, new Default());
        mClipboardManager = new ClipboardManager(this, null);
        mActivityManager = ActivityManager_Accessor.getActivityManagerInstance(this);
        mConnectivityManager = new ConnectivityManager(this, null);

        if (mLayoutlibCallback.isResourceNamespacingRequired()) {
            if (mLayoutlibCallback.hasAndroidXAppCompat()) {
                mAppCompatNamespace = ResourceNamespace.APPCOMPAT;
            } else {
                mAppCompatNamespace = ResourceNamespace.APPCOMPAT_LEGACY;
            }
        } else {
            mAppCompatNamespace = ResourceNamespace.RES_AUTO;
        }

        mShadowsEnabled = shadowsEnabled;
        mHighQualityShadows = highQualityShadows;
    }

    /**
     * Initializes the {@link Resources} singleton to be linked to this {@link Context}, its
     * {@link DisplayMetrics}, {@link Configuration}, and {@link LayoutlibCallback}.
     *
     * @see #disposeResources()
     */
    public void initResources() {
        AssetManager assetManager = AssetManager.getSystem();

        mSystemResources = Resources_Delegate.initSystem(
                this,
                assetManager,
                mMetrics,
                mConfig,
                mLayoutlibCallback);
        mTheme = mSystemResources.newTheme();
    }

    /**
     * Disposes the {@link Resources} singleton and the AssetRepository inside BridgeAssetManager.
     */
    public void disposeResources() {
        Resources_Delegate.disposeSystem();

        // The BridgeAssetManager pointed to by the mAssets field is a long-lived object, but
        // the AssetRepository is not. To prevent it from leaking clear a reference to it from
        // the BridgeAssetManager.
        mAssets.releaseAssetRepository();
    }

    public void setBridgeInflater(BridgeInflater inflater) {
        mBridgeInflater = inflater;
    }

    public void addViewKey(View view, Object viewKey) {
        mViewKeyMap.put(view, viewKey);
    }

    public Object getViewKey(View view) {
        return mViewKeyMap.get(view);
    }

    public void addCookie(Object o, Object cookie) {
        mViewKeyHelpMap.put(o, cookie);
    }

    public Object getCookie(Object o) {
        return mViewKeyHelpMap.get(o);
    }

    public Object getProjectKey() {
        return mProjectKey;
    }

    public DisplayMetrics getMetrics() {
        return mMetrics;
    }

    public LayoutlibCallback getLayoutlibCallback() {
        return mLayoutlibCallback;
    }

    public RenderResources getRenderResources() {
        return mRenderResources;
    }

    public Map<Object, Map<ResourceReference, ResourceValue>> getDefaultProperties() {
        return mDefaultPropMaps;
    }

    public Map<Object, ResourceReference> getDefaultNamespacedStyles() {
        return mDefaultStyleMap;
    }

    public Configuration getConfiguration() {
        return mConfig;
    }

    /**
     * Adds a parser to the stack.
     * @param parser the parser to add.
     */
    public void pushParser(BridgeXmlBlockParser parser) {
        if (ParserFactory.LOG_PARSER) {
            System.out.println("PUSH " + parser.getParser().toString());
        }
        mParserStack.push(parser);
    }

    /**
     * Removes the parser at the top of the stack
     */
    public void popParser() {
        BridgeXmlBlockParser parser = mParserStack.pop();
        if (ParserFactory.LOG_PARSER) {
            System.out.println("POPD " + parser.getParser().toString());
        }
    }

    /**
     * Returns the current parser at the top the of the stack.
     * @return a parser or null.
     */
    private BridgeXmlBlockParser getCurrentParser() {
        return mParserStack.peek();
    }

    /**
     * Returns the previous parser.
     * @return a parser or null if there isn't any previous parser
     */
    public BridgeXmlBlockParser getPreviousParser() {
        if (mParserStack.size() < 2) {
            return null;
        }
        return mParserStack.get(mParserStack.size() - 2);
    }

    public boolean resolveThemeAttribute(int resId, TypedValue outValue, boolean resolveRefs) {
        ResourceReference resourceInfo = Bridge.resolveResourceId(resId);
        if (resourceInfo == null) {
            resourceInfo = mLayoutlibCallback.resolveResourceId(resId);
        }

        if (resourceInfo == null || resourceInfo.getResourceType() != ResourceType.ATTR) {
            return false;
        }

        ResourceValue value = mRenderResources.findItemInTheme(resourceInfo);
        if (resolveRefs) {
            value = mRenderResources.resolveResValue(value);
        }

        if (value == null) {
            // unable to find the attribute.
            return false;
        }

        // check if this is a style resource
        if (value instanceof StyleResourceValue) {
            // get the id that will represent this style.
            outValue.resourceId = getDynamicIdByStyle((StyleResourceValue) value);
            return true;
        }

        String stringValue = value.getValue();
        if (!stringValue.isEmpty()) {
            if (stringValue.charAt(0) == '#') {
                outValue.data = ResourceHelper.getColor(stringValue);
                switch (stringValue.length()) {
                    case 4:
                        outValue.type = TypedValue.TYPE_INT_COLOR_RGB4;
                        break;
                    case 5:
                        outValue.type = TypedValue.TYPE_INT_COLOR_ARGB4;
                        break;
                    case 7:
                        outValue.type = TypedValue.TYPE_INT_COLOR_RGB8;
                        break;
                    default:
                        outValue.type = TypedValue.TYPE_INT_COLOR_ARGB8;
                }
            }
            else if (stringValue.charAt(0) == '@') {
                outValue.type = TypedValue.TYPE_REFERENCE;
            }
            else if ("true".equals(stringValue) || "false".equals(stringValue)) {
                outValue.type = TypedValue.TYPE_INT_BOOLEAN;
                outValue.data = "true".equals(stringValue) ? 1 : 0;
            }
        }

        int a = getResourceId(value.asReference(), 0 /*defValue*/);

        if (a != 0) {
            outValue.resourceId = a;
            return true;
        }

        // If the value is not a valid reference, fallback to pass the value as a string.
        outValue.string = stringValue;
        return true;
    }


    public ResourceReference resolveId(int id) {
        // first get the String related to this id in the framework
        ResourceReference resourceInfo = Bridge.resolveResourceId(id);

        if (resourceInfo != null) {
            return resourceInfo;
        }

        // didn't find a match in the framework? look in the project.
        if (mLayoutlibCallback != null) {
            resourceInfo = mLayoutlibCallback.resolveResourceId(id);

            if (resourceInfo != null) {
                return resourceInfo;
            }
        }

        return null;
    }

    public Pair<View, Boolean> inflateView(ResourceReference layout, ViewGroup parent,
            @SuppressWarnings("SameParameterValue") boolean attachToRoot,
            boolean skipCallbackParser) {
        boolean isPlatformLayout = layout.getNamespace().equals(ResourceNamespace.ANDROID);

        if (!isPlatformLayout && !skipCallbackParser) {
            // check if the project callback can provide us with a custom parser.
            ILayoutPullParser parser = null;
            ResourceValue layoutValue = mRenderResources.getResolvedResource(layout);
            if (layoutValue != null) {
                parser = getLayoutlibCallback().getParser(layoutValue);
            }

            if (parser != null) {
                BridgeXmlBlockParser blockParser =
                        new BridgeXmlBlockParser(parser, this, layout.getNamespace());
                try {
                    pushParser(blockParser);
                    return Pair.of(
                            mBridgeInflater.inflate(blockParser, parent, attachToRoot),
                            Boolean.TRUE);
                } finally {
                    popParser();
                }
            }
        }

        ResourceValue resValue = mRenderResources.getResolvedResource(layout);

        if (resValue != null) {
            String path = resValue.getValue();
            // We need to create a pull parser around the layout XML file, and then
            // give that to our XmlBlockParser.
            try {
                XmlPullParser parser = ParserFactory.create(path, true);
                if (parser != null) {
                    // Set the layout ref to have correct view cookies.
                    mBridgeInflater.setResourceReference(layout);

                    BridgeXmlBlockParser blockParser =
                            new BridgeXmlBlockParser(parser, this, layout.getNamespace());
                    try {
                        pushParser(blockParser);
                        return Pair.of(mBridgeInflater.inflate(blockParser, parent, attachToRoot),
                                Boolean.FALSE);
                    } finally {
                        popParser();
                    }
                } else {
                    Bridge.getLog().error(ILayoutLog.TAG_BROKEN,
                            String.format("File %s is missing!", path), null, null);
                }
            } catch (XmlPullParserException e) {
                Bridge.getLog().error(ILayoutLog.TAG_BROKEN,
                        "Failed to parse file " + path, e, null, null /*data*/);
                // we'll return null below.
            } finally {
                mBridgeInflater.setResourceReference(null);
            }
        } else {
            Bridge.getLog().error(ILayoutLog.TAG_BROKEN,
                    String.format("Layout %s%s does not exist.", isPlatformLayout ? "android:" : "",
                            layout.getName()), null, null);
        }

        return Pair.of(null, Boolean.FALSE);
    }

    /**
     * Returns whether the current selected theme is based on AppCompat
     */
    public boolean isAppCompatTheme() {
        // If a cached value exists, return it.
        if (mIsThemeAppCompat != null) {
            return mIsThemeAppCompat;
        }
        // Ideally, we should check if the corresponding activity extends
        // android.support.v7.app.ActionBarActivity, and not care about the theme name at all.
        StyleResourceValue defaultTheme = mRenderResources.getDefaultTheme();
        // We can't simply check for parent using resources.themeIsParentOf() since the
        // inheritance structure isn't really what one would expect. The first common parent
        // between Theme.AppCompat.Light and Theme.AppCompat is Theme.Material (for v21).
        boolean isThemeAppCompat = false;
        for (int i = 0; i < 50; i++) {
            if (defaultTheme == null) {
                break;
            }
            // for loop ensures that we don't run into cyclic theme inheritance.
            if (defaultTheme.getName().startsWith(PREFIX_THEME_APPCOMPAT)) {
                isThemeAppCompat = true;
                break;
            }
            defaultTheme = mRenderResources.getParent(defaultTheme);
        }
        mIsThemeAppCompat = isThemeAppCompat;
        return isThemeAppCompat;
    }

    // ------------ Context methods

    @Override
    public Resources getResources() {
        return mSystemResources;
    }

    @Override
    public Theme getTheme() {
        return mTheme;
    }

    @Override
    public ClassLoader getClassLoader() {
        // The documentation for this method states that it should return a class loader one can
        // use to retrieve classes in this package. However, when called by LayoutInflater, we do
        // not want the class loader to return app's custom views.
        // This is so that the IDE can instantiate the custom views and also generate proper error
        // messages in case of failure. This also enables the IDE to fallback to MockView in case
        // there's an exception thrown when trying to inflate the custom view.
        // To work around this issue, LayoutInflater is modified via LayoutLib Create tool to
        // replace invocations of this method to a new method: getFrameworkClassLoader(). Also,
        // the method is injected into Context. The implementation of getFrameworkClassLoader() is:
        // "return getClass().getClassLoader();". This means that when LayoutInflater asks for
        // the context ClassLoader, it gets only LayoutLib's ClassLoader which doesn't have
        // access to the apps's custom views.
        // This method can now return the right ClassLoader, which CustomViews can use to do the
        // right thing.
        if (mClassLoader == null) {
            mClassLoader = new ClassLoader(getClass().getClassLoader()) {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    for (String prefix : BridgeInflater.getClassPrefixList()) {
                        if (name.startsWith(prefix)) {
                            // These are framework classes and should not be loaded from the app.
                            throw new ClassNotFoundException(name + " not found");
                        }
                    }
                    return BridgeContext.this.mLayoutlibCallback.findClass(name);
                }
            };
        }
        return mClassLoader;
    }

    @Override
    public Object getSystemService(String service) {
        switch (service) {
            case LAYOUT_INFLATER_SERVICE:
                return mBridgeInflater;

            case TEXT_SERVICES_MANAGER_SERVICE:
                // we need to return a valid service to avoid NPE
                return TextServicesManager.getInstance();

            case WINDOW_SERVICE:
                return mWindowManager;

            case POWER_SERVICE:
                return new PowerManager(this, new BridgePowerManager(), new BridgeThermalService(),
                        new Handler());

            case DISPLAY_SERVICE:
                return mDisplayManager;

            case ACCESSIBILITY_SERVICE:
                return AccessibilityManager.getInstance(this);

            case INPUT_METHOD_SERVICE:  // needed by SearchView and Compose
                return InputMethodManager.forContext(this);

            case AUTOFILL_MANAGER_SERVICE:
                return mAutofillManager;

            case CLIPBOARD_SERVICE:
                return mClipboardManager;

            case ACTIVITY_SERVICE:
                return mActivityManager;

            case CONNECTIVITY_SERVICE:
                return mConnectivityManager;

            case AUDIO_SERVICE:
            case TEXT_CLASSIFICATION_SERVICE:
            case CONTENT_CAPTURE_MANAGER_SERVICE:
                return null;
            default:
                assert false : "Unsupported Service: " + service;
        }

        return null;
    }

    @Override
    public String getSystemServiceName(Class<?> serviceClass) {
        return SystemServiceRegistry.getSystemServiceName(serviceClass);
    }

    /**
     * Same as Context#obtainStyledAttributes. We do not override the base method to give the
     * original Context the chance to override the theme when needed.
     */
    @Nullable
    public final BridgeTypedArray internalObtainStyledAttributes(int resId, int[] attrs)
            throws Resources.NotFoundException {
        StyleResourceValue style = null;
        // get the StyleResourceValue based on the resId;
        if (resId != 0) {
            style = getStyleByDynamicId(resId);

            if (style == null) {
                // In some cases, style may not be a dynamic id, so we do a full search.
                ResourceReference ref = resolveId(resId);
                if (ref != null) {
                    style = mRenderResources.getStyle(ref);
                }
            }

            if (style == null) {
                Bridge.getLog().error(ILayoutLog.TAG_RESOURCES_RESOLVE,
                        "Failed to find style with " + resId, null, null);
                return null;
            }
        }

        if (mTypedArrayCache == null) {
            mTypedArrayCache = new TypedArrayCache();
        }

        List<StyleResourceValue> currentThemes = mRenderResources.getAllThemes();

        Pair<BridgeTypedArray, Map<ResourceReference, ResourceValue>> typeArrayAndPropertiesPair =
                mTypedArrayCache.get(attrs, currentThemes, resId);

        if (typeArrayAndPropertiesPair == null) {
            typeArrayAndPropertiesPair = createStyleBasedTypedArray(style, attrs);
            mTypedArrayCache.put(attrs, currentThemes, resId, typeArrayAndPropertiesPair);
        }
        // Add value to defaultPropsMap if needed
        if (typeArrayAndPropertiesPair.getSecond() != null) {
            BridgeXmlBlockParser parser = getCurrentParser();
            Object key = parser != null ? parser.getViewCookie() : null;
            if (key != null) {
                Map<ResourceReference, ResourceValue> defaultPropMap = mDefaultPropMaps.get(key);
                if (defaultPropMap == null) {
                    defaultPropMap = typeArrayAndPropertiesPair.getSecond();
                    mDefaultPropMaps.put(key, defaultPropMap);
                } else {
                    defaultPropMap.putAll(typeArrayAndPropertiesPair.getSecond());
                }
            }
        }
        return typeArrayAndPropertiesPair.getFirst();
    }

    /**
     * Same as Context#obtainStyledAttributes. We do not override the base method to give the
     * original Context the chance to override the theme when needed.
     */
    @Nullable
    public BridgeTypedArray internalObtainStyledAttributes(@Nullable AttributeSet set, int[] attrs,
            int defStyleAttr, int defStyleRes) {

        Map<ResourceReference, ResourceValue> defaultPropMap = null;
        Object key = null;

        ResourceNamespace currentFileNamespace;
        ResourceNamespace.Resolver resolver;

        // Hint: for XmlPullParser, attach source //DEVICE_SRC/dalvik/libcore/xml/src/java
        if (set instanceof BridgeXmlBlockParser) {
            BridgeXmlBlockParser parser;
            parser = (BridgeXmlBlockParser)set;

            key = parser.getViewCookie();
            if (key != null) {
                defaultPropMap = mDefaultPropMaps.computeIfAbsent(key, k -> new HashMap<>());
            }

            currentFileNamespace = parser.getFileResourceNamespace();
            resolver = new XmlPullParserResolver(parser, mLayoutlibCallback.getImplicitNamespaces());
        } else if (set instanceof BridgeLayoutParamsMapAttributes) {
            // This is for temp layout params generated dynamically in MockView. The set contains
            // hardcoded values and we don't need to worry about resolving them.
            currentFileNamespace = ResourceNamespace.RES_AUTO;
            resolver = Resolver.EMPTY_RESOLVER;
        } else if (set != null) {
            // really this should not be happening since its instantiated in Bridge
            Bridge.getLog().error(ILayoutLog.TAG_BROKEN,
                    "Parser is not a BridgeXmlBlockParser!", null, null);
            return null;
        } else {
            // `set` is null, so there will be no values to resolve.
            currentFileNamespace = ResourceNamespace.RES_AUTO;
            resolver = Resolver.EMPTY_RESOLVER;
        }

        List<AttributeHolder> attributeList = searchAttrs(attrs);

        BridgeTypedArray ta =
                Resources_Delegate.newTypeArray(mSystemResources, attrs.length);

        // Look for a custom style.
        StyleResourceValue customStyleValues = null;
        if (set != null) {
            String customStyle = set.getAttributeValue(null, "style");
            if (customStyle != null) {
                ResourceValue resolved = mRenderResources.resolveResValue(
                        new UnresolvedResourceValue(customStyle, currentFileNamespace, resolver));

                if (resolved instanceof StyleResourceValue) {
                    customStyleValues = (StyleResourceValue) resolved;
                }
            }
        }

        // resolve the defStyleAttr value into a StyleResourceValue
        StyleResourceValue defStyleValues = null;

        if (defStyleAttr != 0) {
            // get the name from the int.
            ResourceReference defStyleAttribute = searchAttr(defStyleAttr);

            if (defStyleAttribute == null) {
                // This should be rare. Happens trying to map R.style.foo to @style/foo fails.
                // This will happen if the user explicitly used a non existing int value for
                // defStyleAttr or there's something wrong with the project structure/build.
                Bridge.getLog().error(ILayoutLog.TAG_RESOURCES_RESOLVE,
                        "Failed to find the style corresponding to the id " + defStyleAttr, null,
                        null);
            } else {
                // look for the style in the current theme, and its parent:
                ResourceValue item = mRenderResources.findItemInTheme(defStyleAttribute);

                if (item != null) {
                    if (key != null) {
                        mDefaultStyleMap.put(key, defStyleAttribute);
                    }
                    // item is a reference to a style entry. Search for it.
                    item = mRenderResources.resolveResValue(item);
                    if (item instanceof StyleResourceValue) {
                        defStyleValues = (StyleResourceValue) item;
                    }
                }
            }
        }

        if (defStyleValues == null && defStyleRes != 0) {
            StyleResourceValue item = getStyleByDynamicId(defStyleRes);
            if (item != null) {
                defStyleValues = item;
            } else {
                ResourceReference value = Bridge.resolveResourceId(defStyleRes);
                if (value == null) {
                    value = mLayoutlibCallback.resolveResourceId(defStyleRes);
                }

                if (value != null) {
                    if ((value.getResourceType() == ResourceType.STYLE)) {
                        // look for the style in all resources:
                        item = mRenderResources.getStyle(value);
                        if (item != null) {
                            if (key != null) {
                                mDefaultStyleMap.put(key, item.asReference());
                            }

                            defStyleValues = item;
                        } else {
                            Bridge.getLog().error(null,
                                    String.format(
                                            "Style with id 0x%x (resolved to '%s') does not exist.",
                                            defStyleRes, value.getName()),
                                    null, null);
                        }
                    } else {
                        Bridge.getLog().error(null,
                                String.format(
                                        "Resource id 0x%x is not of type STYLE (instead %s)",
                                        defStyleRes, value.getResourceType().name()),
                                null, null);
                    }
                } else {
                    Bridge.getLog().error(null,
                            String.format(
                                    "Failed to find style with id 0x%x in current theme",
                                    defStyleRes),
                            null, null);
                }
            }
        }

        if (attributeList != null) {
            for (int index = 0 ; index < attributeList.size() ; index++) {
                AttributeHolder attributeHolder = attributeList.get(index);

                if (attributeHolder == null) {
                    continue;
                }

                String attrName = attributeHolder.getName();
                String value = null;
                if (set != null) {
                    value = set.getAttributeValue(
                            attributeHolder.getNamespace().getXmlNamespaceUri(), attrName);

                    // if this is an app attribute, and the first get fails, try with the
                    // new res-auto namespace as well
                    if (attributeHolder.getNamespace() != ResourceNamespace.ANDROID && value == null) {
                        value = set.getAttributeValue(BridgeConstants.NS_APP_RES_AUTO, attrName);
                    }
                }

                // Calculate the default value from the Theme in two cases:
                //   - If defaultPropMap is not null, get the default value to add it to the list
                //   of default values of properties.
                //   - If value is null, it means that the attribute is not directly set as an
                //   attribute in the XML so try to get the default value.
                ResourceValue defaultValue = null;
                if (defaultPropMap != null || value == null) {
                    // look for the value in the custom style first (and its parent if needed)
                    ResourceReference attrRef = attributeHolder.asReference();
                    if (customStyleValues != null) {
                        defaultValue =
                                mRenderResources.findItemInStyle(customStyleValues, attrRef);
                    }

                    // then look for the value in the default Style (and its parent if needed)
                    if (defaultValue == null && defStyleValues != null) {
                        defaultValue =
                                mRenderResources.findItemInStyle(defStyleValues, attrRef);
                    }

                    // if the item is not present in the defStyle, we look in the main theme (and
                    // its parent themes)
                    if (defaultValue == null) {
                        defaultValue =
                                mRenderResources.findItemInTheme(attrRef);
                    }

                    // if we found a value, we make sure this doesn't reference another value.
                    // So we resolve it.
                    if (defaultValue != null) {
                        if (defaultPropMap != null) {
                            defaultPropMap.put(attrRef, defaultValue);
                        }

                        defaultValue = mRenderResources.resolveResValue(defaultValue);
                    }
                }
                // Done calculating the defaultValue.

                // If there's no direct value for this attribute in the XML, we look for default
                // values in the widget defStyle, and then in the theme.
                if (value == null) {
                    if (attributeHolder.getNamespace() == ResourceNamespace.ANDROID) {
                        // For some framework values, layoutlib patches the actual value in the
                        // theme when it helps to improve the final preview. In most cases
                        // we just disable animations.
                        ResourceValue patchedValue = FRAMEWORK_PATCHED_VALUES.get(attrName);
                        if (patchedValue != null) {
                            defaultValue = patchedValue;
                        }
                    }

                    // If we found a value, we make sure this doesn't reference another value.
                    // So we resolve it.
                    if (defaultValue != null) {
                        // If the value is a reference to another theme attribute that doesn't
                        // exist, we should log a warning and omit it.
                        String val = defaultValue.getValue();
                        if (val != null && val.startsWith(SdkConstants.PREFIX_THEME_REF)) {
                            // Because we always use the latest framework code, some resources might
                            // fail to resolve when using old themes (they haven't been backported).
                            // Since this is an artifact caused by us using always the latest
                            // code, we check for some of those values and replace them here.
                            ResourceReference reference = defaultValue.getReference();
                            defaultValue = FRAMEWORK_REPLACE_VALUES.get(attrName);

                            // Only log a warning if the referenced value isn't one of the RTL
                            // attributes, or the app targets old API.
                            if (defaultValue == null &&
                                    (getApplicationInfo().targetSdkVersion < JELLY_BEAN_MR1 ||
                                    !attrName.equals(RTL_ATTRS.get(val)))) {
                                if (reference != null) {
                                    val = reference.getResourceUrl().toString();
                                }
                                Bridge.getLog().warning(ILayoutLog.TAG_RESOURCES_RESOLVE_THEME_ATTR,
                                        String.format("Failed to find '%s' in current theme.", val),
                                        null, val);
                            }
                        }
                    }

                    ta.bridgeSetValue(
                            index,
                            attrName, attributeHolder.getNamespace(),
                            attributeHolder.getResourceId(),
                            defaultValue);
                } else {
                    // There is a value in the XML, but we need to resolve it in case it's
                    // referencing another resource or a theme value.
                    ta.bridgeSetValue(
                            index,
                            attrName, attributeHolder.getNamespace(),
                            attributeHolder.getResourceId(),
                            mRenderResources.resolveResValue(
                                    new UnresolvedResourceValue(
                                            value, currentFileNamespace, resolver)));
                }
            }
        }

        ta.sealArray();

        return ta;
    }

    @Override
    public Looper getMainLooper() {
        return Looper.myLooper();
    }


    @Override
    public String getPackageName() {
        if (mApplicationInfo.packageName == null) {
            mApplicationInfo.packageName = mLayoutlibCallback.getFlag(FLAG_KEY_APPLICATION_PACKAGE);
        }
        return mApplicationInfo.packageName;
    }

    @Override
    public PackageManager getPackageManager() {
        if (mPackageManager == null) {
            mPackageManager = new BridgePackageManager();
        }
        return mPackageManager;
    }

    @Override
    public void registerComponentCallbacks(ComponentCallbacks callback) {}

    @Override
    public void unregisterComponentCallbacks(ComponentCallbacks callback) {}

    // ------------- private new methods

    /**
     * Creates a {@link BridgeTypedArray} by filling the values defined by the int[] with the
     * values found in the given style. If no style is specified, the default theme, along with the
     * styles applied to it are used.
     *
     * @see #obtainStyledAttributes(int, int[])
     */
    private Pair<BridgeTypedArray, Map<ResourceReference, ResourceValue>> createStyleBasedTypedArray(
            @Nullable StyleResourceValue style, int[] attrs) throws Resources.NotFoundException {
        List<AttributeHolder> attributes = searchAttrs(attrs);

        BridgeTypedArray ta =
                Resources_Delegate.newTypeArray(mSystemResources, attrs.length);

        Map<ResourceReference, ResourceValue> defaultPropMap = new HashMap<>();
        // for each attribute, get its name so that we can search it in the style
        for (int i = 0; i < attrs.length; i++) {
            AttributeHolder attrHolder = attributes.get(i);

            if (attrHolder != null) {
                // look for the value in the given style
                ResourceValue resValue;
                if (style != null) {
                    resValue = mRenderResources.findItemInStyle(style, attrHolder.asReference());
                } else {
                    resValue = mRenderResources.findItemInTheme(attrHolder.asReference());
                }

                if (resValue != null) {
                    defaultPropMap.put(attrHolder.asReference(), resValue);
                    // resolve it to make sure there are no references left.
                    resValue = mRenderResources.resolveResValue(resValue);
                    ta.bridgeSetValue(
                            i, attrHolder.getName(), attrHolder.getNamespace(),
                            attrHolder.getResourceId(),
                            resValue);
                }
            }
        }

        ta.sealArray();

        return Pair.of(ta, defaultPropMap);
    }

    /**
     * The input int[] attributeIds is a list of attributes. The returns a list of information about
     * each attributes. The information is (name, isFramework)
     * <p/>
     *
     * @param attributeIds An attribute array reference given to obtainStyledAttributes.
     * @return List of attribute information.
     */
    private List<AttributeHolder> searchAttrs(int[] attributeIds) {
        List<AttributeHolder> results = new ArrayList<>(attributeIds.length);

        // for each attribute, get its name so that we can search it in the style
        for (int id : attributeIds) {
            ResourceReference refForId = Bridge.resolveResourceId(id);
            if (refForId == null) {
                refForId = mLayoutlibCallback.resolveResourceId(id);
            }

            if (refForId != null) {
                results.add(new AttributeHolder(id, refForId));
            } else {
                results.add(null);
            }
        }

        return results;
    }

    /**
     * Searches for the attribute referenced by its internal id.
     */
    private ResourceReference searchAttr(int attrId) {
        ResourceReference attr = Bridge.resolveResourceId(attrId);
        if (attr == null) {
            attr = mLayoutlibCallback.resolveResourceId(attrId);
        }

        return attr;
    }

    /**
     * Maps a given style to a numeric id.
     *
     * <p>For now Bridge handles numeric ids (both fixed and dynamic) for framework and the callback
     * for non-framework. TODO(b/156609434): teach the IDE about fixed framework ids and handle this
     * all in the callback.
     */
    public int getDynamicIdByStyle(StyleResourceValue resValue) {
        if (resValue.isFramework()) {
            return Bridge.getResourceId(resValue.getResourceType(), resValue.getName());
        } else {
            return mLayoutlibCallback.getOrGenerateResourceId(resValue.asReference());
        }
    }

    /**
     * Maps a numeric id back to {@link StyleResourceValue}.
     *
     * <p>For now framework numeric ids are handled by Bridge, so try there first and fall back to
     * the callback, which manages ids for non-framework resources. TODO(b/156609434): manage all
     * ids in the IDE.
     *
     * <p>Once we the resource for the given id, we ask the IDE to get the
     * {@link StyleResourceValue} for it.
     */
    @Nullable
    private StyleResourceValue getStyleByDynamicId(int id) {
        ResourceReference reference = Bridge.resolveResourceId(id);
        if (reference == null) {
            reference = mLayoutlibCallback.resolveResourceId(id);
        }

        if (reference == null) {
            return null;
        }

        return mRenderResources.getStyle(reference);
    }

    public int getResourceId(@NonNull ResourceReference resource, int defValue) {
        if (getRenderResources().getUnresolvedResource(resource) != null) {
            if (resource.getNamespace().equals(ResourceNamespace.ANDROID)) {
                return Bridge.getResourceId(resource.getResourceType(), resource.getName());
            } else if (mLayoutlibCallback != null) {
                return mLayoutlibCallback.getOrGenerateResourceId(resource);
            }
        }

        return defValue;
    }

    public static Context getBaseContext(Context context) {
        while (context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
        }
        return context;
    }

    /**
     * Returns the Framework attr resource reference with the given name.
     */
    @NonNull
    public static ResourceReference createFrameworkAttrReference(@NonNull String name) {
        return createFrameworkResourceReference(ResourceType.ATTR, name);
    }

    /**
     * Returns the Framework resource reference with the given type and name.
     */
    @NonNull
    public static ResourceReference createFrameworkResourceReference(@NonNull ResourceType type,
            @NonNull String name) {
        return new ResourceReference(ResourceNamespace.ANDROID, type, name);
    }

    /**
     * Returns the AppCompat attr resource reference with the given name.
     */
    @NonNull
    public ResourceReference createAppCompatAttrReference(@NonNull String name) {
        return createAppCompatResourceReference(ResourceType.ATTR, name);
    }

    /**
     * Returns the AppCompat resource reference with the given type and name.
     */
    @NonNull
    public ResourceReference createAppCompatResourceReference(@NonNull ResourceType type,
            @NonNull String name) {
        return new ResourceReference(mAppCompatNamespace, type, name);
    }

    public IBinder getBinder() {
        if (mBinder == null) {
            // create a no-op binder. We only need it be not null.
            mBinder = new IBinder() {
                @Override
                public String getInterfaceDescriptor() throws RemoteException {
                    return null;
                }

                @Override
                public boolean pingBinder() {
                    return false;
                }

                @Override
                public boolean isBinderAlive() {
                    return false;
                }

                @Override
                public IInterface queryLocalInterface(String descriptor) {
                    return null;
                }

                @Override
                public void dump(FileDescriptor fd, String[] args) throws RemoteException {

                }

                @Override
                public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {

                }

                @Override
                public boolean transact(int code, Parcel data, Parcel reply, int flags)
                        throws RemoteException {
                    return false;
                }

                @Override
                public void linkToDeath(DeathRecipient recipient, int flags)
                        throws RemoteException {

                }

                @Override
                public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
                    return false;
                }

                @Override
                public void shellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err,
                  String[] args, ShellCallback shellCallback, ResultReceiver resultReceiver) {
                }
            };
        }
        return mBinder;
    }

    //------------ NOT OVERRIDEN --------------------

    @Override
    public boolean bindService(Intent arg0, ServiceConnection arg1, int arg2) {
        // pass
        return false;
    }

    @Override
    public boolean bindService(Intent arg0, int arg1, Executor arg2, ServiceConnection arg3) {
        return false;
    }

    @Override
    public boolean bindIsolatedService(Intent arg0,
            int arg1, String arg2, Executor arg3, ServiceConnection arg4) {
        return false;
    }

    @Override
    public int checkCallingOrSelfPermission(String arg0) {
        // pass
        return 0;
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri arg0, int arg1) {
        // pass
        return 0;
    }

    @Override
    public int checkCallingPermission(String arg0) {
        // pass
        return 0;
    }

    @Override
    public int checkCallingUriPermission(Uri arg0, int arg1) {
        // pass
        return 0;
    }

    @Override
    public int checkPermission(String arg0, int arg1, int arg2) {
        // pass
        return 0;
    }

    @Override
    public int checkSelfPermission(String arg0) {
        // pass
        return 0;
    }

    @Override
    public int checkPermission(String arg0, int arg1, int arg2, IBinder arg3) {
        // pass
        return 0;
    }

    @Override
    public int checkUriPermission(Uri arg0, int arg1, int arg2, int arg3) {
        // pass
        return 0;
    }

    @Override
    public int checkUriPermission(Uri arg0, int arg1, int arg2, int arg3, IBinder arg4) {
        // pass
        return 0;
    }

    @Override
    public int checkUriPermission(Uri arg0, String arg1, String arg2, int arg3,
            int arg4, int arg5) {
        // pass
        return 0;
    }

    @Override
    public void clearWallpaper() {
        // pass

    }

    @Override
    public Context createPackageContext(String arg0, int arg1) {
        // pass
        return null;
    }

    @Override
    public Context createPackageContextAsUser(String arg0, int arg1, UserHandle user) {
        // pass
        return null;
    }

    @Override
    public Context createConfigurationContext(Configuration overrideConfiguration) {
        // pass
        return null;
    }

    @Override
    public Context createDisplayContext(Display display) {
        // pass
        return null;
    }

    @Override
    public Context createContextForSplit(String splitName) {
        // pass
        return null;
    }

    @Override
    public String[] databaseList() {
        // pass
        return null;
    }

    @Override
    public Context createApplicationContext(ApplicationInfo application, int flags)
            throws PackageManager.NameNotFoundException {
        return null;
    }

    @Override
    public boolean moveDatabaseFrom(Context sourceContext, String name) {
        // pass
        return false;
    }

    @Override
    public boolean deleteDatabase(String arg0) {
        // pass
        return false;
    }

    @Override
    public boolean deleteFile(String arg0) {
        // pass
        return false;
    }

    @Override
    public void enforceCallingOrSelfPermission(String arg0, String arg1) {
        // pass

    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri arg0, int arg1,
            String arg2) {
        // pass

    }

    @Override
    public void enforceCallingPermission(String arg0, String arg1) {
        // pass

    }

    @Override
    public void enforceCallingUriPermission(Uri arg0, int arg1, String arg2) {
        // pass

    }

    @Override
    public void enforcePermission(String arg0, int arg1, int arg2, String arg3) {
        // pass

    }

    @Override
    public void enforceUriPermission(Uri arg0, int arg1, int arg2, int arg3,
            String arg4) {
        // pass

    }

    @Override
    public void enforceUriPermission(Uri arg0, String arg1, String arg2,
            int arg3, int arg4, int arg5, String arg6) {
        // pass

    }

    @Override
    public String[] fileList() {
        // pass
        return null;
    }

    @Override
    public BridgeAssetManager getAssets() {
        return mAssets;
    }

    @Override
    public File getCacheDir() {
        // pass
        return null;
    }

    @Override
    public File getCodeCacheDir() {
        // pass
        return null;
    }

    @Override
    public File getExternalCacheDir() {
        // pass
        return null;
    }

    @Override
    public File getPreloadsFileCache() {
        // pass
        return null;
    }

    @Override
    public ContentResolver getContentResolver() {
        if (mContentResolver == null) {
            mContentResolver = new BridgeContentResolver(this);
        }
        return mContentResolver;
    }

    @Override
    public File getDatabasePath(String arg0) {
        // pass
        return null;
    }

    @Override
    public File getDir(String arg0, int arg1) {
        // pass
        return null;
    }

    @Override
    public File getFileStreamPath(String arg0) {
        // pass
        return null;
    }

    @Override
    public File getSharedPreferencesPath(String name) {
        // pass
        return null;
    }

    @Override
    public File getDataDir() {
        // pass
        return null;
    }

    @Override
    public File getFilesDir() {
        // pass
        return null;
    }

    @Override
    public File getNoBackupFilesDir() {
        // pass
        return null;
    }

    @Override
    public File getExternalFilesDir(String type) {
        // pass
        return null;
    }

    @Override
    public String getPackageCodePath() {
        // pass
        return null;
    }

    @Override
    public String getBasePackageName() {
        // pass
        return null;
    }

    @Override
    public String getOpPackageName() {
        // pass
        return null;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return mApplicationInfo;
    }

    @Override
    public String getPackageResourcePath() {
        // pass
        return null;
    }

    @Override
    public SharedPreferences getSharedPreferences(String arg0, int arg1) {
        if (mSharedPreferences == null) {
            mSharedPreferences = new BridgeSharedPreferences();
        }
        return mSharedPreferences;
    }

    @Override
    public SharedPreferences getSharedPreferences(File arg0, int arg1) {
        if (mSharedPreferences == null) {
            mSharedPreferences = new BridgeSharedPreferences();
        }
        return mSharedPreferences;
    }

    @Override
    public void reloadSharedPreferences() {
        // intentional noop
    }

    @Override
    public boolean moveSharedPreferencesFrom(Context sourceContext, String name) {
        // pass
        return false;
    }

    @Override
    public boolean deleteSharedPreferences(String name) {
        // pass
        return false;
    }

    @Override
    public Drawable getWallpaper() {
        // pass
        return null;
    }

    @Override
    public int getWallpaperDesiredMinimumWidth() {
        return -1;
    }

    @Override
    public int getWallpaperDesiredMinimumHeight() {
        return -1;
    }

    @Override
    public void grantUriPermission(String arg0, Uri arg1, int arg2) {
        // pass

    }

    @Override
    public FileInputStream openFileInput(String arg0) throws FileNotFoundException {
        // pass
        return null;
    }

    @Override
    public FileOutputStream openFileOutput(String arg0, int arg1) throws FileNotFoundException {
        // pass
        return null;
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String arg0, int arg1, CursorFactory arg2) {
        // pass
        return null;
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String arg0, int arg1,
            CursorFactory arg2, DatabaseErrorHandler arg3) {
        // pass
        return null;
    }

    @Override
    public Drawable peekWallpaper() {
        // pass
        return null;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver arg0, IntentFilter arg1) {
        // pass
        return null;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver arg0, IntentFilter arg1, int arg2) {
        // pass
        return null;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver arg0, IntentFilter arg1,
            String arg2, Handler arg3) {
        // pass
        return null;
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver arg0, IntentFilter arg1,
            String arg2, Handler arg3, int arg4) {
        // pass
        return null;
    }

    @Override
    public Intent registerReceiverAsUser(BroadcastReceiver arg0, UserHandle arg0p5,
            IntentFilter arg1, String arg2, Handler arg3) {
        // pass
        return null;
    }

    @Override
    public void removeStickyBroadcast(Intent arg0) {
        // pass

    }

    @Override
    public void revokeUriPermission(Uri arg0, int arg1) {
        // pass

    }

    @Override
    public void revokeUriPermission(String arg0, Uri arg1, int arg2) {
        // pass

    }

    @Override
    public void sendBroadcast(Intent arg0) {
        // pass

    }

    @Override
    public void sendBroadcast(Intent arg0, String arg1) {
        // pass

    }

    @Override
    public void sendBroadcastMultiplePermissions(Intent intent, String[] receiverPermissions) {
        // pass

    }

    @Override
    public void sendBroadcastAsUserMultiplePermissions(Intent intent, UserHandle user,
            String[] receiverPermissions) {
        // pass

    }

    @Override
    public void sendBroadcast(Intent arg0, String arg1, Bundle arg2) {
        // pass

    }

    @Override
    public void sendBroadcast(Intent intent, String receiverPermission, int appOp) {
        // pass
    }

    @Override
    public void sendOrderedBroadcast(Intent arg0, String arg1) {
        // pass

    }

    @Override
    public void sendOrderedBroadcast(Intent arg0, String arg1,
            BroadcastReceiver arg2, Handler arg3, int arg4, String arg5,
            Bundle arg6) {
        // pass

    }

    @Override
    public void sendOrderedBroadcast(Intent arg0, String arg1,
            Bundle arg7, BroadcastReceiver arg2, Handler arg3, int arg4, String arg5,
            Bundle arg6) {
        // pass

    }

    @Override
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, int appOp,
            BroadcastReceiver resultReceiver, Handler scheduler, int initialCode,
            String initialData, Bundle initialExtras) {
        // pass
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user) {
        // pass
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user,
            String receiverPermission) {
        // pass
    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user,
            String receiverPermission, Bundle options) {
        // pass
    }

    public void sendBroadcastAsUser(Intent intent, UserHandle user,
            String receiverPermission, int appOp) {
        // pass
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user,
            String receiverPermission, BroadcastReceiver resultReceiver, Handler scheduler,
            int initialCode, String initialData, Bundle initialExtras) {
        // pass
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user,
            String receiverPermission, int appOp, BroadcastReceiver resultReceiver,
            Handler scheduler,
            int initialCode, String initialData, Bundle initialExtras) {
        // pass
    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user,
            String receiverPermission, int appOp, Bundle options, BroadcastReceiver resultReceiver,
            Handler scheduler,
            int initialCode, String initialData, Bundle initialExtras) {
        // pass
    }

    @Override
    public void sendStickyBroadcast(Intent arg0) {
        // pass

    }

    @Override
    public void sendStickyOrderedBroadcast(Intent intent,
            BroadcastReceiver resultReceiver, Handler scheduler, int initialCode, String initialData,
           Bundle initialExtras) {
        // pass
    }

    @Override
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {
        // pass
    }

    @Override
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle user, Bundle options) {
        // pass
    }

    @Override
    public void sendStickyOrderedBroadcastAsUser(Intent intent,
            UserHandle user, BroadcastReceiver resultReceiver,
            Handler scheduler, int initialCode, String initialData,
            Bundle initialExtras) {
        // pass
    }

    @Override
    public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {
        // pass
    }

    @Override
    public void setTheme(int arg0) {
        // pass

    }

    @Override
    public void setWallpaper(Bitmap arg0) throws IOException {
        // pass

    }

    @Override
    public void setWallpaper(InputStream arg0) throws IOException {
        // pass

    }

    @Override
    public void startActivity(Intent arg0) {
        // pass
    }

    @Override
    public void startActivity(Intent arg0, Bundle arg1) {
        // pass
    }

    @Override
    public void startIntentSender(IntentSender intent,
            Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags)
            throws IntentSender.SendIntentException {
        // pass
    }

    @Override
    public void startIntentSender(IntentSender intent,
            Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags,
            Bundle options) throws IntentSender.SendIntentException {
        // pass
    }

    @Override
    public boolean startInstrumentation(ComponentName arg0, String arg1,
            Bundle arg2) {
        // pass
        return false;
    }

    @Override
    public ComponentName startService(Intent arg0) {
        // pass
        return null;
    }

    @Override
    public ComponentName startForegroundService(Intent service) {
        // pass
        return null;
    }

    @Override
    public ComponentName startForegroundServiceAsUser(Intent service, UserHandle user) {
        // pass
        return null;
    }

    @Override
    public boolean stopService(Intent arg0) {
        // pass
        return false;
    }

    @Override
    public ComponentName startServiceAsUser(Intent arg0, UserHandle arg1) {
        // pass
        return null;
    }

    @Override
    public boolean stopServiceAsUser(Intent arg0, UserHandle arg1) {
        // pass
        return false;
    }

    @Override
    public void updateServiceGroup(@NonNull ServiceConnection conn, int group,
            int importance) {
        // pass
    }

    @Override
    public void unbindService(ServiceConnection arg0) {
        // pass

    }

    @Override
    public void unregisterReceiver(BroadcastReceiver arg0) {
        // pass

    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public void startActivities(Intent[] arg0) {
        // pass

    }

    @Override
    public void startActivities(Intent[] arg0, Bundle arg1) {
        // pass

    }

    @Override
    public boolean isRestricted() {
        return false;
    }

    @Override
    public File getObbDir() {
        Bridge.getLog().error(ILayoutLog.TAG_UNSUPPORTED, "OBB not supported", null, null);
        return null;
    }

    @Override
    public DisplayAdjustments getDisplayAdjustments(int displayId) {
        // pass
        return null;
    }

    @Override
    public Display getDisplay() {
        // pass
        return null;
    }

    @Override
    public int getDisplayId() {
        // pass
        return 0;
    }

    @Override
    public void updateDisplay(int displayId) {
        // pass
    }

    @Override
    public int getUserId() {
        return 0; // not used
    }

    @Override
    public File[] getExternalFilesDirs(String type) {
        // pass
        return new File[0];
    }

    @Override
    public File[] getObbDirs() {
        // pass
        return new File[0];
    }

    @Override
    public File[] getExternalCacheDirs() {
        // pass
        return new File[0];
    }

    @Override
    public File[] getExternalMediaDirs() {
        // pass
        return new File[0];
    }

    public void setScrollYPos(@NonNull View view, int scrollPos) {
        mScrollYPos.put(view, scrollPos);
    }

    public int getScrollYPos(@NonNull View view) {
        Integer pos = mScrollYPos.get(view);
        return pos != null ? pos : 0;
    }

    public void setScrollXPos(@NonNull View view, int scrollPos) {
        mScrollXPos.put(view, scrollPos);
    }

    public int getScrollXPos(@NonNull View view) {
        Integer pos = mScrollXPos.get(view);
        return pos != null ? pos : 0;
    }

    @Override
    public Context createDeviceProtectedStorageContext() {
        // pass
        return null;
    }

    @Override
    public Context createCredentialProtectedStorageContext() {
        // pass
        return null;
    }

    @Override
    public boolean isDeviceProtectedStorage() {
        return false;
    }

    @Override
    public boolean isCredentialProtectedStorage() {
        return false;
    }

    @Override
    public boolean canLoadUnsafeResources() {
        return true;
    }

    @Override
    public boolean isUiContext() {
        return true;
    }

    /**
     * Returns whether shadows should be rendered or not
     */
    public boolean isShadowsEnabled() {
        return mShadowsEnabled;
    }

    /**
     * Returns whether high quality shadows should be used
     */
    public boolean isHighQualityShadows() {
        return mHighQualityShadows;
    }

    public <T> void putUserData(@NonNull Key<T> key, @Nullable T data) {
        mUserData.put(key, data);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getUserData(@NonNull Key<T> key) {
        return (T) mUserData.get(key);
    }

    /** Logs an error message to the error log of the host application. */
    public void error(@NonNull String message, @NonNull String... details) {
        mLayoutlibCallback.error(message, details);
    }

    /** Logs an error message to the error log of the host application. */
    public void error(@NonNull String message, @Nullable Throwable t) {
        mLayoutlibCallback.error(message, t);
    }

    /** Logs an error message to the error log of the host application. */
    public void error(@NonNull Throwable t) {
        mLayoutlibCallback.error(t);
    }

    /** Logs a warning to the log of the host application. */
    public void warn(@NonNull String message, @Nullable Throwable t) {
        mLayoutlibCallback.warn(message, t);
    }

    /** Logs a warning to the log of the host application. */
    public void warn(@NonNull Throwable t) {
        mLayoutlibCallback.warn(t);
    }

    /**
     * No two Key instances are considered equal.
     *
     * @param <T> the type of values associated with the key
     */
    public static final class Key<T> {
        private final String name;

        @NonNull
        public static <T> Key<T> create(@NonNull String name) {
            return new Key<T>(name);
        }

        private Key(@NonNull String name) {
            this.name = name;
        }

        /** For debugging only. */
        @Override
        public String toString() {
            return name;
        }
    }

    private class AttributeHolder {
        private final int resourceId;
        @NonNull private final ResourceReference reference;

        private AttributeHolder(int resourceId, @NonNull ResourceReference reference) {
            this.resourceId = resourceId;
            this.reference = reference;
        }

        @NonNull
        private ResourceReference asReference() {
            return reference;
        }

        private int getResourceId() {
            return resourceId;
        }

        @NonNull
        private String getName() {
            return reference.getName();
        }

        @NonNull
        private ResourceNamespace getNamespace() {
            return reference.getNamespace();
        }
    }

    /**
     * The cached value depends on
     * <ol>
     * <li>{@code int[]}: the attributes for which TypedArray is created </li>
     * <li>{@code List<StyleResourceValue>}: the themes set on the context at the time of
     * creation of the TypedArray</li>
     * <li>{@code Integer}: the default style used at the time of creation</li>
     * </ol>
     *
     * The class is created by using nested maps resolving one dependency at a time.
     * <p/>
     * The final value of the nested maps is a pair of the typed array and a map of properties
     * that should be added to {@link #mDefaultPropMaps}, if needed.
     */
    private static class TypedArrayCache {

        private Map<int[],
                Map<List<StyleResourceValue>,
                        Map<Integer, Pair<BridgeTypedArray,
                                Map<ResourceReference, ResourceValue>>>>> mCache;

        private TypedArrayCache() {
            mCache = new IdentityHashMap<>();
        }

        public Pair<BridgeTypedArray, Map<ResourceReference, ResourceValue>> get(int[] attrs,
                List<StyleResourceValue> themes, int resId) {
            Map<List<StyleResourceValue>, Map<Integer, Pair<BridgeTypedArray, Map<ResourceReference,
                    ResourceValue>>>>
                    cacheFromThemes = mCache.get(attrs);
            if (cacheFromThemes != null) {
                Map<Integer, Pair<BridgeTypedArray, Map<ResourceReference, ResourceValue>>> cacheFromResId =
                        cacheFromThemes.get(themes);
                if (cacheFromResId != null) {
                    return cacheFromResId.get(resId);
                }
            }
            return null;
        }

        public void put(int[] attrs, List<StyleResourceValue> themes, int resId,
                Pair<BridgeTypedArray, Map<ResourceReference, ResourceValue>> value) {
            Map<List<StyleResourceValue>, Map<Integer, Pair<BridgeTypedArray, Map<ResourceReference,
                    ResourceValue>>>>
                    cacheFromThemes = mCache.computeIfAbsent(attrs, k -> new HashMap<>());
            Map<Integer, Pair<BridgeTypedArray, Map<ResourceReference, ResourceValue>>> cacheFromResId =
                    cacheFromThemes.computeIfAbsent(themes, k -> new HashMap<>());
            cacheFromResId.put(resId, value);
        }

    }
}
