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

package android.graphics.drawable;

import android.annotation.DrawableRes;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.TestApi;
import android.app.ActivityThread;
import android.content.pm.ActivityInfo.Config;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.PathParser;

import com.android.internal.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * <p>This class can also be created via XML inflation using <code>&lt;adaptive-icon></code> tag
 * in addition to dynamic creation.
 *
 * <p>This drawable supports two drawable layers: foreground and background. The layers are clipped
 * when rendering using the mask defined in the device configuration.
 *
 * <ul>
 * <li>Both foreground and background layers should be sized at 108 x 108 dp.</li>
 * <li>The inner 72 x 72 dp  of the icon appears within the masked viewport.</li>
 * <li>The outer 18 dp on each of the 4 sides of the layers is reserved for use by the system UI
 * surfaces to create interesting visual effects, such as parallax or pulsing.</li>
 * </ul>
 *
 * Such motion effect is achieved by internally setting the bounds of the foreground and
 * background layer as following:
 * <pre>
 * Rect(getBounds().left - getBounds().getWidth() * #getExtraInsetFraction(),
 *      getBounds().top - getBounds().getHeight() * #getExtraInsetFraction(),
 *      getBounds().right + getBounds().getWidth() * #getExtraInsetFraction(),
 *      getBounds().bottom + getBounds().getHeight() * #getExtraInsetFraction())
 * </pre>
 *
 * <p>An alternate drawable can be specified using <code>&lt;monochrome></code> tag which can be
 * drawn in place of the two (background and foreground) layers. This drawable is tinted
 * according to the device or surface theme.
 */
public class AdaptiveIconDrawable extends Drawable implements Drawable.Callback {

    /**
     * Mask path is defined inside device configuration in following dimension: [100 x 100]
     * @hide
     */
    @TestApi
    public static final float MASK_SIZE = 100f;

    /**
     * Launcher icons design guideline
     */
    private static final float SAFEZONE_SCALE = 66f/72f;

    /**
     * All four sides of the layers are padded with extra inset so as to provide
     * extra content to reveal within the clip path when performing affine transformations on the
     * layers.
     *
     * Each layers will reserve 25% of its width and height.
     *
     * As a result, the view port of the layers is smaller than their intrinsic width and height.
     */
    private static final float EXTRA_INSET_PERCENTAGE = 1 / 4f;
    private static final float DEFAULT_VIEW_PORT_SCALE = 1f / (1 + 2 * EXTRA_INSET_PERCENTAGE);

    /**
     * Clip path defined in R.string.config_icon_mask.
     */
    private static Path sMask;

    /**
     * Scaled mask based on the view bounds.
     */
    private final Path mMask;
    private final Path mMaskScaleOnly;
    private final Matrix mMaskMatrix;
    private final Region mTransparentRegion;

    /**
     * Indices used to access {@link #mLayerState.mChildDrawable} array for foreground and
     * background layer.
     */
    private static final int BACKGROUND_ID = 0;
    private static final int FOREGROUND_ID = 1;
    private static final int MONOCHROME_ID = 2;

    /**
     * State variable that maintains the {@link ChildDrawable} array.
     */
    LayerState mLayerState;

    private Shader mLayersShader;
    private Bitmap mLayersBitmap;

    private final Rect mTmpOutRect = new Rect();
    private Rect mHotspotBounds;
    private boolean mMutated;

    private boolean mSuspendChildInvalidation;
    private boolean mChildRequestedInvalidation;
    private final Canvas mCanvas;
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG |
        Paint.FILTER_BITMAP_FLAG);

    /**
     * Constructor used for xml inflation.
     */
    AdaptiveIconDrawable() {
        this((LayerState) null, null);
    }

    /**
     * The one constructor to rule them all. This is called by all public
     * constructors to set the state and initialize local properties.
     */
    AdaptiveIconDrawable(@Nullable LayerState state, @Nullable Resources res) {
        mLayerState = createConstantState(state, res);
        // config_icon_mask from context bound resource may have been chaged using
        // OverlayManager. Read that one first.
        Resources r = ActivityThread.currentActivityThread() == null
                ? Resources.getSystem()
                : ActivityThread.currentActivityThread().getApplication().getResources();
        // TODO: either make sMask update only when config_icon_mask changes OR
        // get rid of it all-together in layoutlib
        sMask = PathParser.createPathFromPathData(r.getString(R.string.config_icon_mask));
        mMask = new Path(sMask);
        mMaskScaleOnly = new Path(mMask);
        mMaskMatrix = new Matrix();
        mCanvas = new Canvas();
        mTransparentRegion = new Region();
    }

    private ChildDrawable createChildDrawable(Drawable drawable) {
        final ChildDrawable layer = new ChildDrawable(mLayerState.mDensity);
        layer.mDrawable = drawable;
        layer.mDrawable.setCallback(this);
        mLayerState.mChildrenChangingConfigurations |=
            layer.mDrawable.getChangingConfigurations();
        return layer;
    }

    LayerState createConstantState(@Nullable LayerState state, @Nullable Resources res) {
        return new LayerState(state, this, res);
    }

    /**
     * Constructor used to dynamically create this drawable.
     *
     * @param backgroundDrawable drawable that should be rendered in the background
     * @param foregroundDrawable drawable that should be rendered in the foreground
     */
    public AdaptiveIconDrawable(Drawable backgroundDrawable,
            Drawable foregroundDrawable) {
        this(backgroundDrawable, foregroundDrawable, null);
    }

    /**
     * Constructor used to dynamically create this drawable.
     *
     * @param backgroundDrawable drawable that should be rendered in the background
     * @param foregroundDrawable drawable that should be rendered in the foreground
     * @param monochromeDrawable an alternate drawable which can be tinted per system theme color
     */
    public AdaptiveIconDrawable(@Nullable Drawable backgroundDrawable,
            @Nullable Drawable foregroundDrawable, @Nullable Drawable monochromeDrawable) {
        this((LayerState)null, null);
        if (backgroundDrawable != null) {
            addLayer(BACKGROUND_ID, createChildDrawable(backgroundDrawable));
        }
        if (foregroundDrawable != null) {
            addLayer(FOREGROUND_ID, createChildDrawable(foregroundDrawable));
        }
        if (monochromeDrawable != null) {
            addLayer(MONOCHROME_ID, createChildDrawable(monochromeDrawable));
        }
    }

    /**
     * Sets the layer to the {@param index} and invalidates cache.
     *
     * @param index The index of the layer.
     * @param layer The layer to add.
     */
    private void addLayer(int index, @NonNull ChildDrawable layer) {
        mLayerState.mChildren[index] = layer;
        mLayerState.invalidateCache();
    }

    @Override
    public void inflate(@NonNull Resources r, @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs, @Nullable Theme theme)
            throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs, theme);

        final LayerState state = mLayerState;
        if (state == null) {
            return;
        }

        // The density may have changed since the last update. This will
        // apply scaling to any existing constant state properties.
        final int deviceDensity = Drawable.resolveDensity(r, 0);
        state.setDensity(deviceDensity);
        state.mSrcDensityOverride = mSrcDensityOverride;
        state.mSourceDrawableId = Resources.getAttributeSetSourceResId(attrs);

        final ChildDrawable[] array = state.mChildren;
        for (int i = 0; i < array.length; i++) {
            array[i].setDensity(deviceDensity);
        }

        inflateLayers(r, parser, attrs, theme);
    }

    /**
     * All four sides of the layers are padded with extra inset so as to provide
     * extra content to reveal within the clip path when performing affine transformations on the
     * layers.
     *
     * @see #getForeground() and #getBackground() for more info on how this value is used
     */
    public static float getExtraInsetFraction() {
        return EXTRA_INSET_PERCENTAGE;
    }

    /**
     * @hide
     */
    public static float getExtraInsetPercentage() {
        return EXTRA_INSET_PERCENTAGE;
    }

    /**
     * When called before the bound is set, the returned path is identical to
     * R.string.config_icon_mask. After the bound is set, the
     * returned path's computed bound is same as the #getBounds().
     *
     * @return the mask path object used to clip the drawable
     */
    public Path getIconMask() {
        return mMask;
    }

    /**
     * Returns the foreground drawable managed by this class. The bound of this drawable is
     * extended by {@link #getExtraInsetFraction()} * getBounds().width on left/right sides and by
     * {@link #getExtraInsetFraction()} * getBounds().height on top/bottom sides.
     *
     * @return the foreground drawable managed by this drawable
     */
    public Drawable getForeground() {
        return mLayerState.mChildren[FOREGROUND_ID].mDrawable;
    }

    /**
     * Returns the foreground drawable managed by this class. The bound of this drawable is
     * extended by {@link #getExtraInsetFraction()} * getBounds().width on left/right sides and by
     * {@link #getExtraInsetFraction()} * getBounds().height on top/bottom sides.
     *
     * @return the background drawable managed by this drawable
     */
    public Drawable getBackground() {
        return mLayerState.mChildren[BACKGROUND_ID].mDrawable;
    }


    /**
     * Returns the monochrome version of this drawable. Callers can use a tinted version of
     * this drawable instead of the original drawable on surfaces stressing user theming.
     *
     *  @return the monochrome drawable
     */
    @Nullable
    public Drawable getMonochrome() {
        return mLayerState.mChildren[MONOCHROME_ID].mDrawable;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (bounds.isEmpty()) {
            return;
        }
        updateLayerBounds(bounds);
    }

    private void updateLayerBounds(Rect bounds) {
        if (bounds.isEmpty()) {
            return;
        }
        try {
            suspendChildInvalidation();
            updateLayerBoundsInternal(bounds);
            updateMaskBoundsInternal(bounds);
        } finally {
            resumeChildInvalidation();
        }
    }

    /**
     * Set the child layer bounds bigger than the view port size by {@link #DEFAULT_VIEW_PORT_SCALE}
     */
    private void updateLayerBoundsInternal(Rect bounds) {
        int cX = bounds.width() / 2;
        int cY = bounds.height() / 2;

        for (int i = 0, count = mLayerState.N_CHILDREN; i < count; i++) {
            final ChildDrawable r = mLayerState.mChildren[i];
            final Drawable d = r.mDrawable;
            if (d == null) {
                continue;
            }

            int insetWidth = (int) (bounds.width() / (DEFAULT_VIEW_PORT_SCALE * 2));
            int insetHeight = (int) (bounds.height() / (DEFAULT_VIEW_PORT_SCALE * 2));
            final Rect outRect = mTmpOutRect;
            outRect.set(cX - insetWidth, cY - insetHeight, cX + insetWidth, cY + insetHeight);

            d.setBounds(outRect);
        }
    }

    private void updateMaskBoundsInternal(Rect b) {
        // reset everything that depends on the view bounds
        mMaskMatrix.setScale(b.width() / MASK_SIZE, b.height() / MASK_SIZE);
        sMask.transform(mMaskMatrix, mMaskScaleOnly);

        mMaskMatrix.postTranslate(b.left, b.top);
        sMask.transform(mMaskMatrix, mMask);

        if (mLayersBitmap == null || mLayersBitmap.getWidth() != b.width()
                || mLayersBitmap.getHeight() != b.height()) {
            mLayersBitmap = Bitmap.createBitmap(b.width(), b.height(), Bitmap.Config.ARGB_8888);
        }

        mPaint.setShader(null);
        mTransparentRegion.setEmpty();
        mLayersShader = null;
    }

    @Override
    public void draw(Canvas canvas) {
        if (mLayersBitmap == null) {
            return;
        }
        if (mLayersShader == null) {
            mCanvas.setBitmap(mLayersBitmap);
            mCanvas.drawColor(Color.BLACK);
            if (mLayerState.mChildren[BACKGROUND_ID].mDrawable != null) {
                mLayerState.mChildren[BACKGROUND_ID].mDrawable.draw(mCanvas);
            }
            if (mLayerState.mChildren[FOREGROUND_ID].mDrawable != null) {
                mLayerState.mChildren[FOREGROUND_ID].mDrawable.draw(mCanvas);
            }
            mLayersShader = new BitmapShader(mLayersBitmap, TileMode.CLAMP, TileMode.CLAMP);
            mPaint.setShader(mLayersShader);
        }
        if (mMaskScaleOnly != null) {
            Rect bounds = getBounds();
            canvas.translate(bounds.left, bounds.top);
            canvas.drawPath(mMaskScaleOnly, mPaint);
            canvas.translate(-bounds.left, -bounds.top);
        }
    }

    @Override
    public void invalidateSelf() {
        mLayersShader = null;
        super.invalidateSelf();
    }

    @Override
    public void getOutline(@NonNull Outline outline) {
        outline.setPath(mMask);
    }

    /** @hide */
    @TestApi
    public Region getSafeZone() {
        Path mask = getIconMask();
        mMaskMatrix.setScale(SAFEZONE_SCALE, SAFEZONE_SCALE, getBounds().centerX(), getBounds().centerY());
        Path p = new Path();
        mask.transform(mMaskMatrix, p);
        Region safezoneRegion = new Region(getBounds());
        safezoneRegion.setPath(p, safezoneRegion);
        return safezoneRegion;
    }

    @Override
    public @Nullable Region getTransparentRegion() {
        if (mTransparentRegion.isEmpty()) {
            mMask.toggleInverseFillType();
            mTransparentRegion.set(getBounds());
            mTransparentRegion.setPath(mMask, mTransparentRegion);
            mMask.toggleInverseFillType();
        }
        return mTransparentRegion;
    }

    @Override
    public void applyTheme(@NonNull Theme t) {
        super.applyTheme(t);

        final LayerState state = mLayerState;
        if (state == null) {
            return;
        }

        final int density = Drawable.resolveDensity(t.getResources(), 0);
        state.setDensity(density);

        final ChildDrawable[] array = state.mChildren;
        for (int i = 0; i < state.N_CHILDREN; i++) {
            final ChildDrawable layer = array[i];
            layer.setDensity(density);

            if (layer.mThemeAttrs != null) {
                final TypedArray a = t.resolveAttributes(
                    layer.mThemeAttrs, R.styleable.AdaptiveIconDrawableLayer);
                updateLayerFromTypedArray(layer, a);
                a.recycle();
            }

            final Drawable d = layer.mDrawable;
            if (d != null && d.canApplyTheme()) {
                d.applyTheme(t);

                // Update cached mask of child changing configurations.
                state.mChildrenChangingConfigurations |= d.getChangingConfigurations();
            }
        }
    }

    /**
     * If the drawable was inflated from XML, this returns the resource ID for the drawable
     *
     * @hide
     */
    @DrawableRes
    public int getSourceDrawableResId() {
        final LayerState state = mLayerState;
        return state == null ? Resources.ID_NULL : state.mSourceDrawableId;
    }

    /**
     * Inflates child layers using the specified parser.
     */
    private void inflateLayers(@NonNull Resources r, @NonNull XmlPullParser parser,
            @NonNull AttributeSet attrs, @Nullable Theme theme)
            throws XmlPullParserException, IOException {
        final LayerState state = mLayerState;

        final int innerDepth = parser.getDepth() + 1;
        int type;
        int depth;
        int childIndex = 0;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && ((depth = parser.getDepth()) >= innerDepth || type != XmlPullParser.END_TAG)) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            if (depth > innerDepth) {
                continue;
            }
            String tagName = parser.getName();
            switch (tagName) {
                case "background":
                    childIndex = BACKGROUND_ID;
                    break;
                case "foreground":
                    childIndex = FOREGROUND_ID;
                    break;
                case "monochrome":
                    childIndex = MONOCHROME_ID;
                    break;
                default:
                    continue;
            }

            final ChildDrawable layer = new ChildDrawable(state.mDensity);
            final TypedArray a = obtainAttributes(r, theme, attrs,
                R.styleable.AdaptiveIconDrawableLayer);
            updateLayerFromTypedArray(layer, a);
            a.recycle();

            // If the layer doesn't have a drawable or unresolved theme
            // attribute for a drawable, attempt to parse one from the child
            // element. If multiple child elements exist, we'll only use the
            // first one.
            if (layer.mDrawable == null && (layer.mThemeAttrs == null)) {
                while ((type = parser.next()) == XmlPullParser.TEXT) {
                }
                if (type != XmlPullParser.START_TAG) {
                    throw new XmlPullParserException(parser.getPositionDescription()
                            + ": <foreground> or <background> tag requires a 'drawable'"
                            + "attribute or child tag defining a drawable");
                }

                // We found a child drawable. Take ownership.
                layer.mDrawable = Drawable.createFromXmlInnerForDensity(r, parser, attrs,
                        mLayerState.mSrcDensityOverride, theme);
                layer.mDrawable.setCallback(this);
                state.mChildrenChangingConfigurations |=
                        layer.mDrawable.getChangingConfigurations();
            }
            addLayer(childIndex, layer);
        }
    }

    private void updateLayerFromTypedArray(@NonNull ChildDrawable layer, @NonNull TypedArray a) {
        final LayerState state = mLayerState;

        // Account for any configuration changes.
        state.mChildrenChangingConfigurations |= a.getChangingConfigurations();

        // Extract the theme attributes, if any.
        layer.mThemeAttrs = a.extractThemeAttrs();

        Drawable dr = a.getDrawableForDensity(R.styleable.AdaptiveIconDrawableLayer_drawable,
                state.mSrcDensityOverride);
        if (dr != null) {
            if (layer.mDrawable != null) {
                // It's possible that a drawable was already set, in which case
                // we should clear the callback. We may have also integrated the
                // drawable's changing configurations, but we don't have enough
                // information to revert that change.
                layer.mDrawable.setCallback(null);
            }

            // Take ownership of the new drawable.
            layer.mDrawable = dr;
            layer.mDrawable.setCallback(this);
            state.mChildrenChangingConfigurations |=
                layer.mDrawable.getChangingConfigurations();
        }
    }

    @Override
    public boolean canApplyTheme() {
        return (mLayerState != null && mLayerState.canApplyTheme()) || super.canApplyTheme();
    }

    /**
     * @hide
     */
    @Override
    public boolean isProjected() {
        if (super.isProjected()) {
            return true;
        }

        final ChildDrawable[] layers = mLayerState.mChildren;
        for (int i = 0; i < mLayerState.N_CHILDREN; i++) {
            if (layers[i].mDrawable != null && layers[i].mDrawable.isProjected()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Temporarily suspends child invalidation.
     *
     * @see #resumeChildInvalidation()
     */
    private void suspendChildInvalidation() {
        mSuspendChildInvalidation = true;
    }

    /**
     * Resumes child invalidation after suspension, immediately performing an
     * invalidation if one was requested by a child during suspension.
     *
     * @see #suspendChildInvalidation()
     */
    private void resumeChildInvalidation() {
        mSuspendChildInvalidation = false;

        if (mChildRequestedInvalidation) {
            mChildRequestedInvalidation = false;
            invalidateSelf();
        }
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable who) {
        if (mSuspendChildInvalidation) {
            mChildRequestedInvalidation = true;
        } else {
            invalidateSelf();
        }
    }

    @Override
    public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
        unscheduleSelf(what);
    }

    @Override
    public @Config int getChangingConfigurations() {
        return super.getChangingConfigurations() | mLayerState.getChangingConfigurations();
    }

    @Override
    public void setHotspot(float x, float y) {
        final ChildDrawable[] array = mLayerState.mChildren;
        for (int i = 0; i < mLayerState.N_CHILDREN; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setHotspot(x, y);
            }
        }
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom) {
        final ChildDrawable[] array = mLayerState.mChildren;
        for (int i = 0; i < mLayerState.N_CHILDREN; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setHotspotBounds(left, top, right, bottom);
            }
        }

        if (mHotspotBounds == null) {
            mHotspotBounds = new Rect(left, top, right, bottom);
        } else {
            mHotspotBounds.set(left, top, right, bottom);
        }
    }

    @Override
    public void getHotspotBounds(Rect outRect) {
        if (mHotspotBounds != null) {
            outRect.set(mHotspotBounds);
        } else {
            super.getHotspotBounds(outRect);
        }
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        final boolean changed = super.setVisible(visible, restart);
        final ChildDrawable[] array = mLayerState.mChildren;

        for (int i = 0; i < mLayerState.N_CHILDREN; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setVisible(visible, restart);
            }
        }

        return changed;
    }

    @Override
    public void setDither(boolean dither) {
        final ChildDrawable[] array = mLayerState.mChildren;
        for (int i = 0; i < mLayerState.N_CHILDREN; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setDither(dither);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public int getAlpha() {
        return mPaint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        final ChildDrawable[] array = mLayerState.mChildren;
        for (int i = 0; i < mLayerState.N_CHILDREN; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setColorFilter(colorFilter);
            }
        }
    }

    @Override
    public void setTintList(ColorStateList tint) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.N_CHILDREN;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setTintList(tint);
            }
        }
    }

    @Override
    public void setTintBlendMode(@NonNull BlendMode blendMode) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.N_CHILDREN;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setTintBlendMode(blendMode);
            }
        }
    }

    public void setOpacity(int opacity) {
        mLayerState.mOpacityOverride = opacity;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        mLayerState.mAutoMirrored = mirrored;

        final ChildDrawable[] array = mLayerState.mChildren;
        for (int i = 0; i < mLayerState.N_CHILDREN; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setAutoMirrored(mirrored);
            }
        }
    }

    @Override
    public boolean isAutoMirrored() {
        return mLayerState.mAutoMirrored;
    }

    @Override
    public void jumpToCurrentState() {
        final ChildDrawable[] array = mLayerState.mChildren;
        for (int i = 0; i < mLayerState.N_CHILDREN; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.jumpToCurrentState();
            }
        }
    }

    @Override
    public boolean isStateful() {
        return mLayerState.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return mLayerState.hasFocusStateSpecified();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        boolean changed = false;

        final ChildDrawable[] array = mLayerState.mChildren;
        for (int i = 0; i < mLayerState.N_CHILDREN; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null && dr.isStateful() && dr.setState(state)) {
                changed = true;
            }
        }

        if (changed) {
            updateLayerBounds(getBounds());
        }

        return changed;
    }

    @Override
    protected boolean onLevelChange(int level) {
        boolean changed = false;

        final ChildDrawable[] array = mLayerState.mChildren;
        for (int i = 0; i < mLayerState.N_CHILDREN; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null && dr.setLevel(level)) {
                changed = true;
            }
        }

        if (changed) {
            updateLayerBounds(getBounds());
        }

        return changed;
    }

    @Override
    public int getIntrinsicWidth() {
        return (int)(getMaxIntrinsicWidth() * DEFAULT_VIEW_PORT_SCALE);
    }

    private int getMaxIntrinsicWidth() {
        int width = -1;
        for (int i = 0; i < mLayerState.N_CHILDREN; i++) {
            final ChildDrawable r = mLayerState.mChildren[i];
            if (r.mDrawable == null) {
                continue;
            }
            final int w = r.mDrawable.getIntrinsicWidth();
            if (w > width) {
                width = w;
            }
        }
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return (int)(getMaxIntrinsicHeight() * DEFAULT_VIEW_PORT_SCALE);
    }

    private int getMaxIntrinsicHeight() {
        int height = -1;
        for (int i = 0; i < mLayerState.N_CHILDREN; i++) {
            final ChildDrawable r = mLayerState.mChildren[i];
            if (r.mDrawable == null) {
                continue;
            }
            final int h = r.mDrawable.getIntrinsicHeight();
            if (h > height) {
                height = h;
            }
        }
        return height;
    }

    @Override
    public ConstantState getConstantState() {
        if (mLayerState.canConstantState()) {
            mLayerState.mChangingConfigurations = getChangingConfigurations();
            return mLayerState;
        }
        return null;
    }

    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mLayerState = createConstantState(mLayerState, null);
            for (int i = 0; i < mLayerState.N_CHILDREN; i++) {
                final Drawable dr = mLayerState.mChildren[i].mDrawable;
                if (dr != null) {
                    dr.mutate();
                }
            }
            mMutated = true;
        }
        return this;
    }

    /**
     * @hide
     */
    public void clearMutated() {
        super.clearMutated();
        final ChildDrawable[] array = mLayerState.mChildren;
        for (int i = 0; i < mLayerState.N_CHILDREN; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.clearMutated();
            }
        }
        mMutated = false;
    }

    static class ChildDrawable {
        public Drawable mDrawable;
        public int[] mThemeAttrs;
        public int mDensity = DisplayMetrics.DENSITY_DEFAULT;

        ChildDrawable(int density) {
            mDensity = density;
        }

        ChildDrawable(@NonNull ChildDrawable orig, @NonNull AdaptiveIconDrawable owner,
                @Nullable Resources res) {

            final Drawable dr = orig.mDrawable;
            final Drawable clone;
            if (dr != null) {
                final ConstantState cs = dr.getConstantState();
                if (cs == null) {
                    clone = dr;
                } else if (res != null) {
                    clone = cs.newDrawable(res);
                } else {
                    clone = cs.newDrawable();
                }
                clone.setCallback(owner);
                clone.setBounds(dr.getBounds());
                clone.setLevel(dr.getLevel());
            } else {
                clone = null;
            }

            mDrawable = clone;
            mThemeAttrs = orig.mThemeAttrs;

            mDensity = Drawable.resolveDensity(res, orig.mDensity);
        }

        public boolean canApplyTheme() {
            return mThemeAttrs != null
                    || (mDrawable != null && mDrawable.canApplyTheme());
        }

        public final void setDensity(int targetDensity) {
            if (mDensity != targetDensity) {
                mDensity = targetDensity;
            }
        }
    }

    static class LayerState extends ConstantState {
        private int[] mThemeAttrs;

        static final int N_CHILDREN = 3;
        ChildDrawable[] mChildren;

        // The density at which to render the drawable and its children.
        int mDensity;

        // The density to use when inflating/looking up the children drawables. A value of 0 means
        // use the system's density.
        int mSrcDensityOverride = 0;

        int mOpacityOverride = PixelFormat.UNKNOWN;

        @Config int mChangingConfigurations;
        @Config int mChildrenChangingConfigurations;

        @DrawableRes int mSourceDrawableId = Resources.ID_NULL;

        private boolean mCheckedOpacity;
        private int mOpacity;

        private boolean mCheckedStateful;
        private boolean mIsStateful;
        private boolean mAutoMirrored = false;

        LayerState(@Nullable LayerState orig, @NonNull AdaptiveIconDrawable owner,
                @Nullable Resources res) {
            mDensity = Drawable.resolveDensity(res, orig != null ? orig.mDensity : 0);
            mChildren = new ChildDrawable[N_CHILDREN];
            if (orig != null) {
                final ChildDrawable[] origChildDrawable = orig.mChildren;

                mChangingConfigurations = orig.mChangingConfigurations;
                mChildrenChangingConfigurations = orig.mChildrenChangingConfigurations;
                mSourceDrawableId = orig.mSourceDrawableId;

                for (int i = 0; i < N_CHILDREN; i++) {
                    final ChildDrawable or = origChildDrawable[i];
                    mChildren[i] = new ChildDrawable(or, owner, res);
                }

                mCheckedOpacity = orig.mCheckedOpacity;
                mOpacity = orig.mOpacity;
                mCheckedStateful = orig.mCheckedStateful;
                mIsStateful = orig.mIsStateful;
                mAutoMirrored = orig.mAutoMirrored;
                mThemeAttrs = orig.mThemeAttrs;
                mOpacityOverride = orig.mOpacityOverride;
                mSrcDensityOverride = orig.mSrcDensityOverride;
            } else {
                for (int i = 0; i < N_CHILDREN; i++) {
                    mChildren[i] = new ChildDrawable(mDensity);
                }
            }
        }

        public final void setDensity(int targetDensity) {
            if (mDensity != targetDensity) {
                mDensity = targetDensity;
            }
        }

        @Override
        public boolean canApplyTheme() {
            if (mThemeAttrs != null || super.canApplyTheme()) {
                return true;
            }

            final ChildDrawable[] array = mChildren;
            for (int i = 0; i < N_CHILDREN; i++) {
                final ChildDrawable layer = array[i];
                if (layer.canApplyTheme()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Drawable newDrawable() {
            return new AdaptiveIconDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(@Nullable Resources res) {
            return new AdaptiveIconDrawable(this, res);
        }

        @Override
        public @Config int getChangingConfigurations() {
            return mChangingConfigurations
                    | mChildrenChangingConfigurations;
        }

        public final int getOpacity() {
            if (mCheckedOpacity) {
                return mOpacity;
            }

            final ChildDrawable[] array = mChildren;

            // Seek to the first non-null drawable.
            int firstIndex = -1;
            for (int i = 0; i < N_CHILDREN; i++) {
                if (array[i].mDrawable != null) {
                    firstIndex = i;
                    break;
                }
            }

            int op;
            if (firstIndex >= 0) {
                op = array[firstIndex].mDrawable.getOpacity();
            } else {
                op = PixelFormat.TRANSPARENT;
            }

            // Merge all remaining non-null drawables.
            for (int i = firstIndex + 1; i < N_CHILDREN; i++) {
                final Drawable dr = array[i].mDrawable;
                if (dr != null) {
                    op = Drawable.resolveOpacity(op, dr.getOpacity());
                }
            }

            mOpacity = op;
            mCheckedOpacity = true;
            return op;
        }

        public final boolean isStateful() {
            if (mCheckedStateful) {
                return mIsStateful;
            }

            final ChildDrawable[] array = mChildren;
            boolean isStateful = false;
            for (int i = 0; i < N_CHILDREN; i++) {
                final Drawable dr = array[i].mDrawable;
                if (dr != null && dr.isStateful()) {
                    isStateful = true;
                    break;
                }
            }

            mIsStateful = isStateful;
            mCheckedStateful = true;
            return isStateful;
        }

        public final boolean hasFocusStateSpecified() {
            final ChildDrawable[] array = mChildren;
            for (int i = 0; i < N_CHILDREN; i++) {
                final Drawable dr = array[i].mDrawable;
                if (dr != null && dr.hasFocusStateSpecified()) {
                    return true;
                }
            }
            return false;
        }

        public final boolean canConstantState() {
            final ChildDrawable[] array = mChildren;
            for (int i = 0; i < N_CHILDREN; i++) {
                final Drawable dr = array[i].mDrawable;
                if (dr != null && dr.getConstantState() == null) {
                    return false;
                }
            }

            // Don't cache the result, this method is not called very often.
            return true;
        }

        public void invalidateCache() {
            mCheckedOpacity = false;
            mCheckedStateful = false;
        }
    }
}
