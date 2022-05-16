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

package android.view;

import com.android.SdkConstants;
import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.ide.common.rendering.api.LayoutlibCallback;
import com.android.ide.common.rendering.api.MergeCookie;
import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.layoutlib.bridge.Bridge;
import com.android.layoutlib.bridge.BridgeConstants;
import com.android.layoutlib.bridge.MockView;
import com.android.layoutlib.bridge.android.BridgeContext;
import com.android.layoutlib.bridge.android.BridgeXmlBlockParser;
import com.android.layoutlib.bridge.android.support.DrawerLayoutUtil;
import com.android.layoutlib.bridge.android.support.RecyclerViewUtil;
import com.android.layoutlib.bridge.impl.ParserFactory;
import com.android.layoutlib.bridge.util.ReflectionUtils;
import com.android.tools.layoutlib.annotations.NotNull;
import com.android.tools.layoutlib.annotations.Nullable;

import org.xmlpull.v1.XmlPullParser;

import android.annotation.NonNull;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.ResolvingAttributeSet;
import android.view.View.OnAttachStateChangeListener;
import android.widget.ImageView;
import android.widget.NumberPicker;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static com.android.layoutlib.bridge.android.BridgeContext.getBaseContext;

/**
 * Custom implementation of {@link LayoutInflater} to handle custom views.
 */
public final class BridgeInflater extends LayoutInflater {
    private static final String INFLATER_CLASS_ATTR_NAME = "viewInflaterClass";
    private static final ResourceReference RES_AUTO_INFLATER_CLASS_ATTR =
            ResourceReference.attr(ResourceNamespace.RES_AUTO, INFLATER_CLASS_ATTR_NAME);
    private static final ResourceReference LEGACY_APPCOMPAT_INFLATER_CLASS_ATTR =
            ResourceReference.attr(ResourceNamespace.APPCOMPAT_LEGACY, INFLATER_CLASS_ATTR_NAME);
    private static final ResourceReference ANDROIDX_APPCOMPAT_INFLATER_CLASS_ATTR =
            ResourceReference.attr(ResourceNamespace.APPCOMPAT, INFLATER_CLASS_ATTR_NAME);
    private static final String LEGACY_DEFAULT_APPCOMPAT_INFLATER_NAME =
            "android.support.v7.app.AppCompatViewInflater";
    private static final String ANDROIDX_DEFAULT_APPCOMPAT_INFLATER_NAME =
            "androidx.appcompat.app.AppCompatViewInflater";
    private final LayoutlibCallback mLayoutlibCallback;

    private boolean mIsInMerge = false;
    private ResourceReference mResourceReference;
    private Map<View, String> mOpenDrawerLayouts;

    // Keep in sync with the same value in LayoutInflater.
    private static final int[] ATTRS_THEME = new int[] {com.android.internal.R.attr.theme };

    /**
     * List of class prefixes which are tried first by default.
     * <p/>
     * This should match the list in com.android.internal.policy.impl.PhoneLayoutInflater.
     */
    private static final String[] sClassPrefixList = {
        "android.widget.",
        "android.webkit.",
        "android.app."
    };
    private BiFunction<String, AttributeSet, View> mCustomInflater;

    public static String[] getClassPrefixList() {
        return sClassPrefixList;
    }

    private BridgeInflater(LayoutInflater original, Context newContext) {
        super(original, newContext);
        newContext = getBaseContext(newContext);
        mLayoutlibCallback = (newContext instanceof BridgeContext) ?
                ((BridgeContext) newContext).getLayoutlibCallback() :
                null;
    }

    /**
     * Instantiate a new BridgeInflater with an {@link LayoutlibCallback} object.
     *
     * @param context The Android application context.
     * @param layoutlibCallback the {@link LayoutlibCallback} object.
     */
    public BridgeInflater(BridgeContext context, LayoutlibCallback layoutlibCallback) {
        super(context);
        mLayoutlibCallback = layoutlibCallback;
        mConstructorArgs[0] = context;
    }

    @Override
    public View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
        View view = createViewFromCustomInflater(name, attrs);

        if (view == null) {
            try {
                // First try to find a class using the default Android prefixes
                for (String prefix : sClassPrefixList) {
                    try {
                        view = createView(name, prefix, attrs);
                        if (view != null) {
                            break;
                        }
                    } catch (ClassNotFoundException e) {
                        // Ignore. We'll try again using the base class below.
                    }
                }

                // Next try using the parent loader. This will most likely only work for
                // fully-qualified class names.
                try {
                    if (view == null) {
                        view = super.onCreateView(name, attrs);
                    }
                } catch (ClassNotFoundException e) {
                    // Ignore. We'll try again using the custom view loader below.
                }

                // Finally try again using the custom view loader
                if (view == null) {
                    view = loadCustomView(name, attrs);
                }
            } catch (InflateException e) {
                // Don't catch the InflateException below as that results in hiding the real cause.
                throw e;
            } catch (Exception e) {
                // Wrap the real exception in a ClassNotFoundException, so that the calling method
                // can deal with it.
                throw new ClassNotFoundException("onCreateView", e);
            }
        }

        setupViewInContext(view, attrs);

        return view;
    }

    /**
     * Finds the createView method in the given customInflaterClass. Since createView is
     * currently package protected, it will show in the declared class so we iterate up the
     * hierarchy and return the first instance we find.
     * The returned method will be accessible.
     */
    @NotNull
    private static Method getCreateViewMethod(Class<?> customInflaterClass) throws NoSuchMethodException {
        Class<?> current = customInflaterClass;
        do {
            try {
                Method method = current.getDeclaredMethod("createView", View.class, String.class,
                                Context.class, AttributeSet.class, boolean.class, boolean.class,
                                boolean.class, boolean.class);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignore) {
            }
            current = current.getSuperclass();
        } while (current != null && current != Object.class);

        throw new NoSuchMethodException();
    }

    /**
     * Finds the custom inflater class. If it's defined in the theme, we'll use that one (if the
     * class does not exist, null is returned).
     * If {@code viewInflaterClass} is not defined in the theme, we'll try to instantiate
     * {@code android.support.v7.app.AppCompatViewInflater}
     */
    @Nullable
    private static Class<?> findCustomInflater(@NotNull BridgeContext bc,
            @NotNull LayoutlibCallback layoutlibCallback) {
        ResourceReference attrRef;
        if (layoutlibCallback.isResourceNamespacingRequired()) {
            if (layoutlibCallback.hasLegacyAppCompat()) {
                attrRef = LEGACY_APPCOMPAT_INFLATER_CLASS_ATTR;
            } else if (layoutlibCallback.hasAndroidXAppCompat()) {
                attrRef = ANDROIDX_APPCOMPAT_INFLATER_CLASS_ATTR;
            } else {
                return null;
            }
        } else {
            attrRef = RES_AUTO_INFLATER_CLASS_ATTR;
        }
        ResourceValue value = bc.getRenderResources().findItemInTheme(attrRef);
        String inflaterName = value != null ? value.getValue() : null;

        if (inflaterName != null) {
            try {
                return layoutlibCallback.findClass(inflaterName);
            } catch (ClassNotFoundException ignore) {
            }

            // viewInflaterClass was defined but we couldn't find the class.
        } else if (bc.isAppCompatTheme()) {
            // Older versions of AppCompat do not define the viewInflaterClass so try to get it
            // manually.
            try {
                if (layoutlibCallback.hasLegacyAppCompat()) {
                    return layoutlibCallback.findClass(LEGACY_DEFAULT_APPCOMPAT_INFLATER_NAME);
                } else if (layoutlibCallback.hasAndroidXAppCompat()) {
                    return layoutlibCallback.findClass(ANDROIDX_DEFAULT_APPCOMPAT_INFLATER_NAME);
                }
            } catch (ClassNotFoundException ignore) {
            }
        }

        return null;
    }

    /**
     * Checks if there is a custom inflater and, when present, tries to instantiate the view
     * using it.
     */
    @Nullable
    private View createViewFromCustomInflater(@NotNull String name, @NotNull AttributeSet attrs) {
        if (mCustomInflater == null) {
            Context context = getContext();
            context = getBaseContext(context);
            if (context instanceof BridgeContext) {
                BridgeContext bc = (BridgeContext) context;
                Class<?> inflaterClass = findCustomInflater(bc, mLayoutlibCallback);

                if (inflaterClass != null) {
                    try {
                        Constructor<?> constructor =  inflaterClass.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        Object inflater = constructor.newInstance();
                        Method method = getCreateViewMethod(inflaterClass);
                        mCustomInflater = (viewName, attributeSet) -> {
                            try {
                                return (View) method.invoke(inflater, null, viewName,
                                        mConstructorArgs[0],
                                        attributeSet,
                                        false,
                                        false /*readAndroidTheme*/, // No need after L
                                        true /*readAppTheme*/,
                                        true /*wrapContext*/);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                Bridge.getLog().error(ILayoutLog.TAG_BROKEN, e.getMessage(), e,
                                        null, null);
                            }
                            return null;
                        };
                    } catch (InvocationTargetException | IllegalAccessException |
                            NoSuchMethodException | InstantiationException ignore) {
                    }
                }
            }

            if (mCustomInflater == null) {
                // There is no custom inflater. We'll create a nop custom inflater to avoid the
                // penalty of trying to instantiate again
                mCustomInflater = (s, attributeSet) -> null;
            }
        }

        return mCustomInflater.apply(name, attrs);
    }

    @Override
    public View createViewFromTag(View parent, String name, Context context, AttributeSet attrs,
            boolean ignoreThemeAttr) {
        View view = null;
        if (name.equals("view")) {
            // This is usually done by the superclass but this allows us catching the error and
            // reporting something useful.
            name = attrs.getAttributeValue(null, "class");

            if (name == null) {
                Bridge.getLog().error(ILayoutLog.TAG_BROKEN, "Unable to inflate view tag without " +
                  "class attribute", null, null);
                // We weren't able to resolve the view so we just pass a mock View to be able to
                // continue rendering.
                view = new MockView(context, attrs);
                ((MockView) view).setText("view");
            }
        }

        try {
            if (view == null) {
                view = super.createViewFromTag(parent, name, context, attrs, ignoreThemeAttr);
            }
        } catch (InflateException e) {
            // Creation of ContextThemeWrapper code is same as in the super method.
            // Apply a theme wrapper, if allowed and one is specified.
            if (!ignoreThemeAttr) {
                final TypedArray ta = context.obtainStyledAttributes(attrs, ATTRS_THEME);
                final int themeResId = ta.getResourceId(0, 0);
                if (themeResId != 0) {
                    context = new ContextThemeWrapper(context, themeResId);
                }
                ta.recycle();
            }
            if (!(e.getCause() instanceof ClassNotFoundException)) {
                // There is some unknown inflation exception in inflating a View that was found.
                view = new MockView(context, attrs);
                ((MockView) view).setText(name);
                Bridge.getLog().error(ILayoutLog.TAG_BROKEN, e.getMessage(), e, null, null);
            } else {
                final Object lastContext = mConstructorArgs[0];
                mConstructorArgs[0] = context;
                // try to load the class from using the custom view loader
                try {
                    view = loadCustomView(name, attrs);
                } catch (Exception e2) {
                    // Wrap the real exception in an InflateException so that the calling
                    // method can deal with it.
                    InflateException exception = new InflateException();
                    if (!e2.getClass().equals(ClassNotFoundException.class)) {
                        exception.initCause(e2);
                    } else {
                        exception.initCause(e);
                    }
                    throw exception;
                } finally {
                    mConstructorArgs[0] = lastContext;
                }
            }
        }

        setupViewInContext(view, attrs);

        return view;
    }

    @Override
    public View inflate(int resource, ViewGroup root) {
        Context context = getContext();
        context = getBaseContext(context);
        if (context instanceof BridgeContext) {
            BridgeContext bridgeContext = (BridgeContext)context;

            ResourceValue value = null;

            ResourceReference layoutInfo = Bridge.resolveResourceId(resource);
            if (layoutInfo == null) {
                layoutInfo = mLayoutlibCallback.resolveResourceId(resource);

            }
            if (layoutInfo != null) {
                value = bridgeContext.getRenderResources().getResolvedResource(layoutInfo);
            }

            if (value != null) {
                String path = value.getValue();
                try {
                    XmlPullParser parser = ParserFactory.create(path, true);
                    if (parser == null) {
                        return null;
                    }

                    BridgeXmlBlockParser bridgeParser = new BridgeXmlBlockParser(
                            parser, bridgeContext, value.getNamespace());

                    return inflate(bridgeParser, root);
                } catch (Exception e) {
                    Bridge.getLog().error(ILayoutLog.TAG_RESOURCES_READ,
                            "Failed to parse file " + path, e, null, null);

                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Instantiates the given view name and returns the instance. If the view doesn't exist, a
     * MockView or null might be returned.
     * @param name the custom view name
     * @param attrs the {@link AttributeSet} to be passed to the view constructor
     * @param silent if true, errors while loading the view won't be reported and, if the view
     * doesn't exist, null will be returned.
     */
    private View loadCustomView(String name, AttributeSet attrs, boolean silent) throws Exception {
        if (mLayoutlibCallback != null) {
            // first get the classname in case it's not the node name
            if (name.equals("view")) {
                name = attrs.getAttributeValue(null, "class");
                if (name == null) {
                    return null;
                }
            }

            mConstructorArgs[1] = attrs;

            Object customView = silent ?
                    mLayoutlibCallback.loadClass(name, mConstructorSignature, mConstructorArgs)
                    : mLayoutlibCallback.loadView(name, mConstructorSignature, mConstructorArgs);

            if (customView instanceof View) {
                return (View)customView;
            }
        }

        return null;
    }

    private View loadCustomView(String name, AttributeSet attrs) throws Exception {
        return loadCustomView(name, attrs, false);
    }

    private void setupViewInContext(View view, AttributeSet attrs) {
        Context context = getContext();
        context = getBaseContext(context);
        if (!(context instanceof BridgeContext)) {
            return;
        }

        BridgeContext bc = (BridgeContext) context;
        // get the view key
        Object viewKey = getViewKeyFromParser(attrs, bc, mResourceReference, mIsInMerge);
        if (viewKey != null) {
            bc.addViewKey(view, viewKey);
        }
        String scrollPosX = attrs.getAttributeValue(BridgeConstants.NS_RESOURCES, "scrollX");
        if (scrollPosX != null && scrollPosX.endsWith("px")) {
            int value = Integer.parseInt(scrollPosX.substring(0, scrollPosX.length() - 2));
            bc.setScrollXPos(view, value);
        }
        String scrollPosY = attrs.getAttributeValue(BridgeConstants.NS_RESOURCES, "scrollY");
        if (scrollPosY != null && scrollPosY.endsWith("px")) {
            int value = Integer.parseInt(scrollPosY.substring(0, scrollPosY.length() - 2));
            bc.setScrollYPos(view, value);
        }
        if (ReflectionUtils.isInstanceOf(view, RecyclerViewUtil.CN_RECYCLER_VIEW)) {
            int resourceId = 0;
            int attrItemCountValue = attrs.getAttributeIntValue(BridgeConstants.NS_TOOLS_URI,
                    BridgeConstants.ATTR_ITEM_COUNT, -1);
            if (attrs instanceof ResolvingAttributeSet) {
                ResourceValue attrListItemValue =
                        ((ResolvingAttributeSet) attrs).getResolvedAttributeValue(
                                BridgeConstants.NS_TOOLS_URI, BridgeConstants.ATTR_LIST_ITEM);
                if (attrListItemValue != null) {
                    resourceId = bc.getResourceId(attrListItemValue.asReference(), 0);
                }
            }
            RecyclerViewUtil.setAdapter(view, bc, mLayoutlibCallback, resourceId, attrItemCountValue);
        } else if (ReflectionUtils.isInstanceOf(view, DrawerLayoutUtil.CN_DRAWER_LAYOUT)) {
            String attrVal = attrs.getAttributeValue(BridgeConstants.NS_TOOLS_URI,
                    BridgeConstants.ATTR_OPEN_DRAWER);
            if (attrVal != null) {
                getDrawerLayoutMap().put(view, attrVal);
            }
        }
        else if (view instanceof NumberPicker) {
            NumberPicker numberPicker = (NumberPicker) view;
            String minValue = attrs.getAttributeValue(BridgeConstants.NS_TOOLS_URI, "minValue");
            if (minValue != null) {
                numberPicker.setMinValue(Integer.parseInt(minValue));
            }
            String maxValue = attrs.getAttributeValue(BridgeConstants.NS_TOOLS_URI, "maxValue");
            if (maxValue != null) {
                numberPicker.setMaxValue(Integer.parseInt(maxValue));
            }
        }
        else if (view instanceof ImageView) {
            ImageView img = (ImageView) view;
            Drawable drawable = img.getDrawable();
            if (drawable instanceof Animatable) {
                if (!((Animatable) drawable).isRunning()) {
                    ((Animatable) drawable).start();
                }
            }
        }
        else if (view instanceof ViewStub) {
            // By default, ViewStub will be set to GONE and won't be inflate. If the XML has the
            // tools:visibility attribute we'll workaround that behavior.
            String visibility = attrs.getAttributeValue(BridgeConstants.NS_TOOLS_URI,
                    SdkConstants.ATTR_VISIBILITY);

            boolean isVisible = "visible".equals(visibility);
            if (isVisible || "invisible".equals(visibility)) {
                // We can not inflate the view until is attached to its parent so we need to delay
                // the setVisible call until after that happens.
                final int visibilityValue = isVisible ? View.VISIBLE : View.INVISIBLE;
                view.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        v.removeOnAttachStateChangeListener(this);
                        view.setVisibility(visibilityValue);
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {}
                });
            }
        }

    }

    public void setIsInMerge(boolean isInMerge) {
        mIsInMerge = isInMerge;
    }

    public void setResourceReference(ResourceReference reference) {
        mResourceReference = reference;
    }

    @Override
    public LayoutInflater cloneInContext(Context newContext) {
        return new BridgeInflater(this, newContext);
    }

    /*package*/ static Object getViewKeyFromParser(AttributeSet attrs, BridgeContext bc,
            ResourceReference resourceReference, boolean isInMerge) {

        if (!(attrs instanceof BridgeXmlBlockParser)) {
            return null;
        }
        BridgeXmlBlockParser parser = ((BridgeXmlBlockParser) attrs);

        // get the view key
        Object viewKey = parser.getViewCookie();

        if (viewKey == null) {
            int currentDepth = parser.getDepth();

            // test whether we are in an included file or in a adapter binding view.
            BridgeXmlBlockParser previousParser = bc.getPreviousParser();
            if (previousParser != null) {
                // looks like we are inside an embedded layout.
                // only apply the cookie of the calling node (<include>) if we are at the
                // top level of the embedded layout. If there is a merge tag, then
                // skip it and look for the 2nd level
                int testDepth = isInMerge ? 2 : 1;
                if (currentDepth == testDepth) {
                    viewKey = previousParser.getViewCookie();
                    // if we are in a merge, wrap the cookie in a MergeCookie.
                    if (viewKey != null && isInMerge) {
                        viewKey = new MergeCookie(viewKey);
                    }
                }
            } else if (resourceReference != null && currentDepth == 1) {
                // else if there's a resource reference, this means we are in an adapter
                // binding case. Set the resource ref as the view cookie only for the top
                // level view.
                viewKey = resourceReference;
            }
        }

        return viewKey;
    }

    public void postInflateProcess(View view) {
        if (mOpenDrawerLayouts != null) {
            String gravity = mOpenDrawerLayouts.get(view);
            if (gravity != null) {
                DrawerLayoutUtil.openDrawer(view, gravity);
            }
            mOpenDrawerLayouts.remove(view);
        }
    }

    @NonNull
    private Map<View, String> getDrawerLayoutMap() {
        if (mOpenDrawerLayouts == null) {
            mOpenDrawerLayouts = new HashMap<>(4);
        }
        return mOpenDrawerLayouts;
    }

    public void onDoneInflation() {
        if (mOpenDrawerLayouts != null) {
            mOpenDrawerLayouts.clear();
        }
    }
}
