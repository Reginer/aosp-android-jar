/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.graphics;

import com.android.ide.common.rendering.api.ILayoutLog;
import com.android.layoutlib.bridge.Bridge;
import com.android.layoutlib.bridge.impl.DelegateManager;
import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.FontFamily_Delegate.FontVariant;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.FontMetricsInt;
import android.text.TextUtils;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import libcore.util.NativeAllocationRegistry_Delegate;

/**
 * Delegate implementing the native methods of android.graphics.Paint
 *
 * Through the layoutlib_create tool, the original native methods of Paint have been replaced
 * by calls to methods of the same name in this delegate class.
 *
 * This class behaves like the original native implementation, but in Java, keeping previously
 * native data into its own objects and mapping them to int that are sent back and forth between
 * it and the original Paint class.
 *
 * @see DelegateManager
 *
 */
public class Paint_Delegate {
    private static final float DEFAULT_TEXT_SIZE = 20.f;
    private static final float DEFAULT_TEXT_SCALE_X = 1.f;
    private static final float DEFAULT_TEXT_SKEW_X = 0.f;

    /**
     * Class associating a {@link Font} and its {@link java.awt.FontMetrics}.
     */
    /*package*/ static final class FontInfo {
        final Font mFont;
        final java.awt.FontMetrics mMetrics;

        FontInfo(@NonNull Font font, @NonNull java.awt.FontMetrics fontMetrics) {
            this.mFont = font;
            this.mMetrics = fontMetrics;
        }
    }

    // ---- delegate manager ----
    private static final DelegateManager<Paint_Delegate> sManager =
            new DelegateManager<>(Paint_Delegate.class);
    private static long sFinalizer = -1;

    // ---- delegate helper data ----

    // This list can contain null elements.
    @Nullable
    private List<FontInfo> mFonts;

    // ---- delegate data ----
    private int mFlags;
    private int mColor;
    private int mStyle;
    private int mCap;
    private int mJoin;
    private int mTextAlign;
    private Typeface_Delegate mTypeface;
    private float mStrokeWidth;
    private float mStrokeMiter;
    private float mTextSize;
    private float mTextScaleX;
    private float mTextSkewX;
    private int mHintingMode = Paint.HINTING_ON;
    private int mStartHyphenEdit;
    private int mEndHyphenEdit;
    private float mLetterSpacing;  // not used in actual text rendering.
    private float mWordSpacing;  // not used in actual text rendering.
    // Variant of the font. A paint's variant can only be compact or elegant.
    private FontVariant mFontVariant = FontVariant.COMPACT;

    private int mPorterDuffMode = Xfermode.DEFAULT;
    private ColorFilter_Delegate mColorFilter;
    private Shader_Delegate mShader;
    private PathEffect_Delegate mPathEffect;
    private MaskFilter_Delegate mMaskFilter;

    @SuppressWarnings("FieldCanBeLocal") // Used to store the locale for future use
    private Locale mLocale = Locale.getDefault();

    // ---- Public Helper methods ----

    @Nullable
    public static Paint_Delegate getDelegate(long native_paint) {
        return sManager.getDelegate(native_paint);
    }

    /**
     * Returns the list of {@link Font} objects.
     */
    @NonNull
    public List<FontInfo> getFonts() {
        Typeface_Delegate typeface = mTypeface;
        if (typeface == null) {
            if (Typeface.sDefaultTypeface == null) {
                return Collections.emptyList();
            }

            typeface = Typeface_Delegate.getDelegate(Typeface.sDefaultTypeface.native_instance);
        }

        if (mFonts != null) {
            return mFonts;
        }

        // Apply an optional transformation for skew and scale
        AffineTransform affineTransform = mTextScaleX != 1.0 || mTextSkewX != 0 ?
                new AffineTransform(mTextScaleX, mTextSkewX, 0, 1, 0, 0) :
                null;

        List<FontInfo> infoList = StreamSupport.stream(typeface.getFonts(mFontVariant).spliterator
                (), false)
                .filter(Objects::nonNull)
                .map(font -> getFontInfo(font, mTextSize, affineTransform))
                .collect(Collectors.toList());
        mFonts = Collections.unmodifiableList(infoList);

        return mFonts;
    }

    public boolean isAntiAliased() {
        return (mFlags & Paint.ANTI_ALIAS_FLAG) != 0;
    }

    public boolean isFilterBitmap() {
        return (mFlags & Paint.FILTER_BITMAP_FLAG) != 0;
    }

    public int getStyle() {
        return mStyle;
    }

    public int getColor() {
        return mColor;
    }

    public int getAlpha() {
        return mColor >>> 24;
    }

    public void setAlpha(int alpha) {
        mColor = (alpha << 24) | (mColor & 0x00FFFFFF);
    }

    public int getTextAlign() {
        return mTextAlign;
    }

    public float getStrokeWidth() {
        return mStrokeWidth;
    }

    /**
     * returns the value of stroke miter needed by the java api.
     */
    public float getJavaStrokeMiter() {
        return mStrokeMiter;
    }

    public int getJavaCap() {
        switch (Paint.sCapArray[mCap]) {
            case BUTT:
                return BasicStroke.CAP_BUTT;
            case ROUND:
                return BasicStroke.CAP_ROUND;
            default:
            case SQUARE:
                return BasicStroke.CAP_SQUARE;
        }
    }

    public int getJavaJoin() {
        switch (Paint.sJoinArray[mJoin]) {
            default:
            case MITER:
                return BasicStroke.JOIN_MITER;
            case ROUND:
                return BasicStroke.JOIN_ROUND;
            case BEVEL:
                return BasicStroke.JOIN_BEVEL;
        }
    }

    public Stroke getJavaStroke() {
        if (mPathEffect != null) {
            if (mPathEffect.isSupported()) {
                Stroke stroke = mPathEffect.getStroke(this);
                assert stroke != null;
                //noinspection ConstantConditions
                if (stroke != null) {
                    return stroke;
                }
            } else {
                Bridge.getLog().fidelityWarning(ILayoutLog.TAG_PATHEFFECT,
                        mPathEffect.getSupportMessage(),
                        null, null, null /*data*/);
            }
        }

        // if no custom stroke as been set, set the default one.
        return new BasicStroke(
                    getStrokeWidth(),
                    getJavaCap(),
                    getJavaJoin(),
                    getJavaStrokeMiter());
    }

    /**
     * Returns the {@link PorterDuff.Mode} as an int
     */
    public int getPorterDuffMode() {
        return mPorterDuffMode;
    }

    /**
     * Returns the {@link ColorFilter} delegate or null if none have been set
     *
     * @return the delegate or null.
     */
    public ColorFilter_Delegate getColorFilter() {
        return mColorFilter;
    }

    public void setColorFilter(long colorFilterPtr) {
        mColorFilter = ColorFilter_Delegate.getDelegate(colorFilterPtr);
    }

    public void setShader(long shaderPtr) {
        mShader = Shader_Delegate.getDelegate(shaderPtr);
    }

    /**
     * Returns the {@link Shader} delegate or null if none have been set
     *
     * @return the delegate or null.
     */
    public Shader_Delegate getShader() {
        return mShader;
    }

    /**
     * Returns the {@link MaskFilter} delegate or null if none have been set
     *
     * @return the delegate or null.
     */
    public MaskFilter_Delegate getMaskFilter() {
        return mMaskFilter;
    }

    // ---- native methods ----

    @LayoutlibDelegate
    /*package*/ static int nGetFlags(long nativePaint) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 0;
        }

        return delegate.mFlags;
    }



    @LayoutlibDelegate
    /*package*/ static void nSetFlags(long nativePaint, int flags) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }

        delegate.mFlags = flags;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetFilterBitmap(long nativePaint, boolean filter) {
        setFlag(nativePaint, Paint.FILTER_BITMAP_FLAG, filter);
    }

    @LayoutlibDelegate
    /*package*/ static int nGetHinting(long nativePaint) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return Paint.HINTING_ON;
        }

        return delegate.mHintingMode;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetHinting(long nativePaint, int mode) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }

        delegate.mHintingMode = mode;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetAntiAlias(long nativePaint, boolean aa) {
        setFlag(nativePaint, Paint.ANTI_ALIAS_FLAG, aa);
    }

    @LayoutlibDelegate
    /*package*/ static void nSetSubpixelText(long nativePaint,
            boolean subpixelText) {
        setFlag(nativePaint, Paint.SUBPIXEL_TEXT_FLAG, subpixelText);
    }

    @LayoutlibDelegate
    /*package*/ static void nSetUnderlineText(long nativePaint,
            boolean underlineText) {
        setFlag(nativePaint, Paint.UNDERLINE_TEXT_FLAG, underlineText);
    }

    @LayoutlibDelegate
    /*package*/ static void nSetStrikeThruText(long nativePaint,
            boolean strikeThruText) {
        setFlag(nativePaint, Paint.STRIKE_THRU_TEXT_FLAG, strikeThruText);
    }

    @LayoutlibDelegate
    /*package*/ static void nSetFakeBoldText(long nativePaint,
            boolean fakeBoldText) {
        setFlag(nativePaint, Paint.FAKE_BOLD_TEXT_FLAG, fakeBoldText);
    }

    @LayoutlibDelegate
    /*package*/ static void nSetDither(long nativePaint, boolean dither) {
        setFlag(nativePaint, Paint.DITHER_FLAG, dither);
    }

    @LayoutlibDelegate
    /*package*/ static void nSetLinearText(long nativePaint, boolean linearText) {
        setFlag(nativePaint, Paint.LINEAR_TEXT_FLAG, linearText);
    }

    @LayoutlibDelegate
    /*package*/ static void nSetColor(long paintPtr, long colorSpaceHandle, long color) {
        Paint_Delegate delegate = sManager.getDelegate(paintPtr);
        if (delegate == null) {
            return;
        }

        delegate.mColor = Color.toArgb(color);
    }

    @LayoutlibDelegate
    /*package*/ static void nSetColor(long paintPtr, int color) {
        Paint_Delegate delegate = sManager.getDelegate(paintPtr);
        if (delegate == null) {
            return;
        }

        delegate.mColor = color;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetAlpha(long nativePaint, int a) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }

        delegate.setAlpha(a);
    }

    @LayoutlibDelegate
    /*package*/ static float nGetStrokeWidth(long nativePaint) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 1.f;
        }

        return delegate.mStrokeWidth;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetStrokeWidth(long nativePaint, float width) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }

        delegate.mStrokeWidth = width;
    }

    @LayoutlibDelegate
    /*package*/ static float nGetStrokeMiter(long nativePaint) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 1.f;
        }

        return delegate.mStrokeMiter;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetStrokeMiter(long nativePaint, float miter) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }

        delegate.mStrokeMiter = miter;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetShadowLayer(long paintPtr,
            float radius, float dx, float dy, long colorSpaceHandle,
            long shadowColor) {
        Bridge.getLog().fidelityWarning(ILayoutLog.TAG_UNSUPPORTED,
                "Paint.setShadowLayer is not supported.", null, null, null /*data*/);
    }

    @LayoutlibDelegate
    /*package*/ static boolean nHasShadowLayer(long paint) {
        // FIXME
        Bridge.getLog().fidelityWarning(ILayoutLog.TAG_UNSUPPORTED,
                "Paint.hasShadowLayer is not supported.", null, null, null /*data*/);
        return false;
    }

    @LayoutlibDelegate
    /*package*/ static boolean nIsElegantTextHeight(long nativePaint) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        return delegate != null && delegate.mFontVariant == FontVariant.ELEGANT;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetElegantTextHeight(long nativePaint,
            boolean elegant) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }

        delegate.mFontVariant = elegant ? FontVariant.ELEGANT : FontVariant.COMPACT;
    }

    @LayoutlibDelegate
    /*package*/ static float nGetTextSize(long nativePaint) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 1.f;
        }

        return delegate.mTextSize;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetTextSize(long nativePaint, float textSize) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }

        if (delegate.mTextSize != textSize) {
            delegate.mTextSize = textSize;
            delegate.invalidateFonts();
        }
    }

    @LayoutlibDelegate
    /*package*/ static float nGetTextScaleX(long nativePaint) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 1.f;
        }

        return delegate.mTextScaleX;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetTextScaleX(long nativePaint, float scaleX) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }

        if (delegate.mTextScaleX != scaleX) {
            delegate.mTextScaleX = scaleX;
            delegate.invalidateFonts();
        }
    }

    @LayoutlibDelegate
    /*package*/ static float nGetTextSkewX(long nativePaint) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 1.f;
        }

        return delegate.mTextSkewX;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetTextSkewX(long nativePaint, float skewX) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }

        if (delegate.mTextSkewX != skewX) {
            delegate.mTextSkewX = skewX;
            delegate.invalidateFonts();
        }
    }

    @LayoutlibDelegate
    /*package*/ static float nAscent(long nativePaint) {
        // get the delegate
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 0;
        }

        List<FontInfo> fonts = delegate.getFonts();
        if (fonts.size() > 0) {
            java.awt.FontMetrics javaMetrics = fonts.get(0).mMetrics;
            // Android expects negative ascent so we invert the value from Java.
            return - javaMetrics.getAscent();
        }

        return 0;
    }

    @LayoutlibDelegate
    /*package*/ static float nDescent(long nativePaint) {
        // get the delegate
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 0;
        }

        List<FontInfo> fonts = delegate.getFonts();
        if (fonts.size() > 0) {
            java.awt.FontMetrics javaMetrics = fonts.get(0).mMetrics;
            return javaMetrics.getDescent();
        }

        return 0;

    }

    @LayoutlibDelegate
    /*package*/ static float nGetFontMetrics(long nativePaint,
            FontMetrics metrics) {
        // get the delegate
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 0;
        }

        return delegate.getFontMetrics(metrics);
    }

    @LayoutlibDelegate
    /*package*/ static int nGetFontMetricsInt(long nativePaint, FontMetricsInt fmi) {
        // get the delegate
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 0;
        }

        List<FontInfo> fonts = delegate.getFonts();
        if (fonts.size() > 0) {
            java.awt.FontMetrics javaMetrics = fonts.get(0).mMetrics;
            if (fmi != null) {
                // Android expects negative ascent so we invert the value from Java.
                fmi.top = (int)(- javaMetrics.getMaxAscent() * 1.15);
                fmi.ascent = - javaMetrics.getAscent();
                fmi.descent = javaMetrics.getDescent();
                fmi.bottom = (int)(javaMetrics.getMaxDescent() * 1.15);
                fmi.leading = javaMetrics.getLeading();
            }

            return javaMetrics.getHeight();
        }

        return 0;
    }

    @LayoutlibDelegate
    /*package*/ static int nBreakText(long nativePaint, char[] text,
            int index, int count, float maxWidth, int bidiFlags, float[] measuredWidth) {

        // get the delegate
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 0;
        }

        int inc = count > 0 ? 1 : -1;

        int measureIndex = 0;
        for (int i = index; i != index + count; i += inc, measureIndex++) {
            int start, end;
            if (i < index) {
                start = i;
                end = index;
            } else {
                start = index;
                end = i;
            }

            // measure from start to end
            RectF bounds = delegate.measureText(text, start, end - start + 1, null, 0, bidiFlags);
            float res = bounds.right - bounds.left;

            if (measuredWidth != null) {
                measuredWidth[measureIndex] = res;
            }

            if (res > maxWidth) {
                // we should not return this char index, but since it's 0-based
                // and we need to return a count, we simply return measureIndex;
                return measureIndex;
            }

        }

        return measureIndex;
    }

    @LayoutlibDelegate
    /*package*/ static int nBreakText(long nativePaint, String text, boolean measureForwards,
            float maxWidth, int bidiFlags, float[] measuredWidth) {
        return nBreakText(nativePaint, text.toCharArray(), 0, text.length(),
                maxWidth, bidiFlags, measuredWidth);
    }

    @LayoutlibDelegate
    /*package*/ static long nInit() {
        Paint_Delegate newDelegate = new Paint_Delegate();
        return sManager.addNewDelegate(newDelegate);
    }

    @LayoutlibDelegate
    /*package*/ static long nInitWithPaint(long paint) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(paint);
        if (delegate == null) {
            return 0;
        }

        Paint_Delegate newDelegate = new Paint_Delegate(delegate);
        return sManager.addNewDelegate(newDelegate);
    }

    @LayoutlibDelegate
    /*package*/ static void nReset(long native_object) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return;
        }

        delegate.reset();
    }

    @LayoutlibDelegate
    /*package*/ static void nSet(long native_dst, long native_src) {
        // get the delegate from the native int.
        Paint_Delegate delegate_dst = sManager.getDelegate(native_dst);
        if (delegate_dst == null) {
            return;
        }

        // get the delegate from the native int.
        Paint_Delegate delegate_src = sManager.getDelegate(native_src);
        if (delegate_src == null) {
            return;
        }

        delegate_dst.set(delegate_src);
    }

    @LayoutlibDelegate
    /*package*/ static int nGetStyle(long native_object) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return 0;
        }

        return delegate.mStyle;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetStyle(long native_object, int style) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return;
        }

        delegate.mStyle = style;
    }

    @LayoutlibDelegate
    /*package*/ static int nGetStrokeCap(long native_object) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return 0;
        }

        return delegate.mCap;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetStrokeCap(long native_object, int cap) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return;
        }

        delegate.mCap = cap;
    }

    @LayoutlibDelegate
    /*package*/ static int nGetStrokeJoin(long native_object) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return 0;
        }

        return delegate.mJoin;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetStrokeJoin(long native_object, int join) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return;
        }

        delegate.mJoin = join;
    }

    @LayoutlibDelegate
    /*package*/ static boolean nGetFillPath(long native_object, long src, long dst) {
        Paint_Delegate paint = sManager.getDelegate(native_object);
        if (paint == null) {
            return false;
        }

        Path_Delegate srcPath = Path_Delegate.getDelegate(src);
        if (srcPath == null) {
            return true;
        }

        Path_Delegate dstPath = Path_Delegate.getDelegate(dst);
        if (dstPath == null) {
            return true;
        }

        Stroke stroke = paint.getJavaStroke();
        Shape strokeShape = stroke.createStrokedShape(srcPath.getJavaShape());

        dstPath.setJavaShape(strokeShape);

        // FIXME figure out the return value?
        return true;
    }

    @LayoutlibDelegate
    /*package*/ static long nSetShader(long native_object, long shader) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return shader;
        }

        delegate.mShader = Shader_Delegate.getDelegate(shader);

        return shader;
    }

    @LayoutlibDelegate
    /*package*/ static long nSetColorFilter(long native_object, long filter) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return filter;
        }

        delegate.mColorFilter = ColorFilter_Delegate.getDelegate(filter);

        // Log warning if it's not supported.
        if (delegate.mColorFilter != null && !delegate.mColorFilter.isSupported()) {
            Bridge.getLog().fidelityWarning(ILayoutLog.TAG_COLORFILTER,
                    delegate.mColorFilter.getSupportMessage(), null, null, null /*data*/);
        }

        return filter;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetXfermode(long native_object, int xfermode) {
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return;
        }
        delegate.mPorterDuffMode = xfermode;
    }

    @LayoutlibDelegate
    /*package*/ static long nSetPathEffect(long native_object, long effect) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return effect;
        }

        delegate.mPathEffect = PathEffect_Delegate.getDelegate(effect);

        return effect;
    }

    @LayoutlibDelegate
    /*package*/ static long nSetMaskFilter(long native_object, long maskfilter) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return maskfilter;
        }

        delegate.mMaskFilter = MaskFilter_Delegate.getDelegate(maskfilter);

        // since none of those are supported, display a fidelity warning right away
        if (delegate.mMaskFilter != null && !delegate.mMaskFilter.isSupported()) {
            Bridge.getLog().fidelityWarning(ILayoutLog.TAG_MASKFILTER,
                    delegate.mMaskFilter.getSupportMessage(), null, null, null /*data*/);
        }

        return maskfilter;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetTypeface(long native_object, long typeface) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return;
        }

        Typeface_Delegate typefaceDelegate = Typeface_Delegate.getDelegate(typeface);
        if (delegate.mTypeface != typefaceDelegate) {
            delegate.mTypeface = typefaceDelegate;
            delegate.invalidateFonts();
        }
    }

    @LayoutlibDelegate
    /*package*/ static int nGetTextAlign(long native_object) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return 0;
        }

        return delegate.mTextAlign;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetTextAlign(long native_object, int align) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return;
        }

        delegate.mTextAlign = align;
    }

    @LayoutlibDelegate
    /*package*/ static int nSetTextLocales(long native_object, String locale) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return 0;
        }

        delegate.setTextLocale(locale);
        return 0;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetTextLocalesByMinikinLocaleListId(long paintPtr,
            int mMinikinLangListId) {
        // FIXME
    }

    @LayoutlibDelegate
    /*package*/ static float nGetTextAdvances(long native_object, char[] text, int index,
            int count, int contextIndex, int contextCount,
            int bidiFlags, float[] advances, int advancesIndex) {

        if (advances != null)
            for (int i = advancesIndex; i< advancesIndex+count; i++)
                advances[i]=0;
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(native_object);
        if (delegate == null) {
            return 0.f;
        }

        RectF bounds = delegate.measureText(text, index, count, advances, advancesIndex, bidiFlags);
        return bounds.right - bounds.left;
    }

    @LayoutlibDelegate
    /*package*/ static float nGetTextAdvances(long native_object, String text, int start, int end,
            int contextStart, int contextEnd, int bidiFlags, float[] advances, int advancesIndex) {
        // FIXME: support contextStart and contextEnd
        int count = end - start;
        char[] buffer = TemporaryBuffer.obtain(count);
        TextUtils.getChars(text, start, end, buffer, 0);

        return nGetTextAdvances(native_object, buffer, 0, count,
                contextStart, contextEnd - contextStart, bidiFlags, advances, advancesIndex);
    }

    @LayoutlibDelegate
    /*package*/ static int nGetTextRunCursor(Paint paint, long native_object, char[] text,
            int contextStart, int contextLength, int flags, int offset, int cursorOpt) {
        // FIXME
        Bridge.getLog().fidelityWarning(ILayoutLog.TAG_UNSUPPORTED,
                "Paint.getTextRunCursor is not supported.", null, null, null /*data*/);
        return 0;
    }

    @LayoutlibDelegate
    /*package*/ static int nGetTextRunCursor(Paint paint, long native_object, String text,
            int contextStart, int contextEnd, int flags, int offset, int cursorOpt) {
        // FIXME
        Bridge.getLog().fidelityWarning(ILayoutLog.TAG_UNSUPPORTED,
                "Paint.getTextRunCursor is not supported.", null, null, null /*data*/);
        return 0;
    }

    @LayoutlibDelegate
    /*package*/ static void nGetTextPath(long native_object, int bidiFlags, char[] text,
            int index, int count, float x, float y, long path) {
        // FIXME
        Bridge.getLog().fidelityWarning(ILayoutLog.TAG_UNSUPPORTED,
                "Paint.getTextPath is not supported.", null, null, null /*data*/);
    }

    @LayoutlibDelegate
    /*package*/ static void nGetTextPath(long native_object, int bidiFlags, String text, int start,
            int end, float x, float y, long path) {
        // FIXME
        Bridge.getLog().fidelityWarning(ILayoutLog.TAG_UNSUPPORTED,
                "Paint.getTextPath is not supported.", null, null, null /*data*/);
    }

    @LayoutlibDelegate
    /*package*/ static void nGetStringBounds(long nativePaint, String text, int start, int end,
            int bidiFlags, Rect bounds) {
        nGetCharArrayBounds(nativePaint, text.toCharArray(), start,
                end - start, bidiFlags, bounds);
    }

    @LayoutlibDelegate
    public static void nGetCharArrayBounds(long nativePaint, char[] text, int index,
            int count, int bidiFlags, Rect bounds) {

        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }

        delegate.measureText(text, index, count, null, 0, bidiFlags).roundOut(bounds);
    }

    @LayoutlibDelegate
    /*package*/ static long nGetNativeFinalizer() {
        synchronized (Paint_Delegate.class) {
            if (sFinalizer == -1) {
                sFinalizer = NativeAllocationRegistry_Delegate.createFinalizer(
                        sManager::removeJavaReferenceFor);
            }
        }
        return sFinalizer;
    }

    @LayoutlibDelegate
    /*package*/ static float nGetLetterSpacing(long nativePaint) {
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 0;
        }
        return delegate.mLetterSpacing;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetLetterSpacing(long nativePaint, float letterSpacing) {
        Bridge.getLog().fidelityWarning(ILayoutLog.TAG_TEXT_RENDERING,
                "Paint.setLetterSpacing() not supported.", null, null, null);
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }
        delegate.mLetterSpacing = letterSpacing;
    }

    @LayoutlibDelegate
    /*package*/ static float nGetWordSpacing(long nativePaint) {
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 0;
        }
        return delegate.mWordSpacing;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetWordSpacing(long nativePaint, float wordSpacing) {
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }
        delegate.mWordSpacing = wordSpacing;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetFontFeatureSettings(long nativePaint, String settings) {
        Bridge.getLog().fidelityWarning(ILayoutLog.TAG_TEXT_RENDERING,
                "Paint.setFontFeatureSettings() not supported.", null, null, null);
    }

    @LayoutlibDelegate
    /*package*/ static int nGetStartHyphenEdit(long nativePaint) {
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 0;
        }
        return delegate.mStartHyphenEdit;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetStartHyphenEdit(long nativePaint, int hyphen) {
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }
        delegate.mStartHyphenEdit = hyphen;
    }

    @LayoutlibDelegate
    /*package*/ static int nGetEndHyphenEdit(long nativePaint) {
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return 0;
        }
        return delegate.mEndHyphenEdit;
    }

    @LayoutlibDelegate
    /*package*/ static void nSetEndHyphenEdit(long nativePaint, int hyphen) {
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }
        delegate.mEndHyphenEdit = hyphen;
    }

    @LayoutlibDelegate
    /*package*/ static boolean nHasGlyph(long nativePaint, int bidiFlags, String string) {
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return false;
        }
        if (string.length() == 0) {
            return false;
        }
        if (string.length() > 1) {
            Bridge.getLog().fidelityWarning(ILayoutLog.TAG_TEXT_RENDERING,
                    "Paint.hasGlyph() is not supported for ligatures.", null, null, null);
            return false;
        }

        char c = string.charAt(0);
        for (Font font : delegate.mTypeface.getFonts(delegate.mFontVariant)) {
            if (font.canDisplay(c)) {
                return true;
            }
        }
        return false;
    }


    @LayoutlibDelegate
    /*package*/ static float nGetRunAdvance(long nativePaint, @NonNull char[] text, int start,
            int end, int contextStart, int contextEnd,
            boolean isRtl, int offset) {
        int count = end - start;
        float[] advances = new float[count];
        int bidiFlags = isRtl ? Paint.BIDI_FORCE_RTL : Paint.BIDI_FORCE_LTR;
        nGetTextAdvances(nativePaint, text, start, count, contextStart,
                contextEnd - contextStart, bidiFlags, advances, 0);
        int startOffset = offset - start;  // offset from start.
        float sum = 0;
        for (int i = 0; i < startOffset; i++) {
            sum += advances[i];
        }
        return sum;
    }

    @LayoutlibDelegate
    /*package*/ static int nGetOffsetForAdvance(long nativePaint, char[] text, int start,
            int end, int contextStart, int contextEnd, boolean isRtl, float advance) {
        int count = end - start;
        float[] advances = new float[count];
        int bidiFlags = isRtl ? Paint.BIDI_FORCE_RTL : Paint.BIDI_FORCE_LTR;
        nGetTextAdvances(nativePaint, text, start, count, contextStart,
                contextEnd - contextStart, bidiFlags, advances, 0);
        float sum = 0;
        int i;
        for (i = 0; i < count && sum < advance; i++) {
            sum += advances[i];
        }
        float distanceToI = sum - advance;
        float distanceToIMinus1 = advance - (sum - advances[i]);
        return distanceToI > distanceToIMinus1 ? i : i - 1;
    }

    @LayoutlibDelegate
    /*package*/ static float nGetUnderlinePosition(long paintPtr) {
        return (1.0f / 9.0f) * nGetTextSize(paintPtr);
    }

    @LayoutlibDelegate
    /*package*/ static float nGetUnderlineThickness(long paintPtr) {
        return (1.0f / 18.0f) * nGetTextSize(paintPtr);
    }

    @LayoutlibDelegate
    /*package*/ static float nGetStrikeThruPosition(long paintPtr) {
        return (-79.0f / 252.0f) * nGetTextSize(paintPtr);
    }

    @LayoutlibDelegate
    /*package*/ static float nGetStrikeThruThickness(long paintPtr) {
        return (1.0f / 18.0f) * nGetTextSize(paintPtr);
    }

    @LayoutlibDelegate
    /*package*/ static boolean nEqualsForTextMeasurement(long leftPaintPtr, long rightPaintPtr) {
        return leftPaintPtr == rightPaintPtr;
    }

    // ---- Private delegate/helper methods ----

    /*package*/ Paint_Delegate() {
        reset();
    }

    private Paint_Delegate(Paint_Delegate paint) {
        set(paint);
    }

    private void set(Paint_Delegate paint) {
        mFlags = paint.mFlags;
        mColor = paint.mColor;
        mStyle = paint.mStyle;
        mCap = paint.mCap;
        mJoin = paint.mJoin;
        mTextAlign = paint.mTextAlign;

        if (mTypeface != paint.mTypeface) {
            mTypeface = paint.mTypeface;
            invalidateFonts();
        }

        if (mTextSize != paint.mTextSize) {
            mTextSize = paint.mTextSize;
            invalidateFonts();
        }

        if (mTextScaleX != paint.mTextScaleX) {
            mTextScaleX = paint.mTextScaleX;
            invalidateFonts();
        }

        if (mTextSkewX != paint.mTextSkewX) {
            mTextSkewX = paint.mTextSkewX;
            invalidateFonts();
        }

        mStrokeWidth = paint.mStrokeWidth;
        mStrokeMiter = paint.mStrokeMiter;
        mPorterDuffMode = paint.mPorterDuffMode;
        mColorFilter = paint.mColorFilter;
        mShader = paint.mShader;
        mPathEffect = paint.mPathEffect;
        mMaskFilter = paint.mMaskFilter;
        mHintingMode = paint.mHintingMode;
    }

    private void reset() {
        Typeface_Delegate defaultTypeface =
                Typeface_Delegate.getDelegate(Typeface.sDefaults[0].native_instance);

        mFlags = Paint.HIDDEN_DEFAULT_PAINT_FLAGS;
        mColor = 0xFF000000;
        mStyle = Paint.Style.FILL.nativeInt;
        mCap = Paint.Cap.BUTT.nativeInt;
        mJoin = Paint.Join.MITER.nativeInt;
        mTextAlign = 0;

        if (mTypeface != defaultTypeface) {
            mTypeface = defaultTypeface;
            invalidateFonts();
        }

        mStrokeWidth = 1.f;
        mStrokeMiter = 4.f;

        if (mTextSize != DEFAULT_TEXT_SIZE) {
            mTextSize = DEFAULT_TEXT_SIZE;
            invalidateFonts();
        }

        if (mTextScaleX != DEFAULT_TEXT_SCALE_X) {
            mTextScaleX = DEFAULT_TEXT_SCALE_X;
            invalidateFonts();
        }

        if (mTextSkewX != DEFAULT_TEXT_SKEW_X) {
            mTextSkewX = DEFAULT_TEXT_SKEW_X;
            invalidateFonts();
        }

        mPorterDuffMode = Xfermode.DEFAULT;
        mColorFilter = null;
        mShader = null;
        mPathEffect = null;
        mMaskFilter = null;
        mHintingMode = Paint.HINTING_ON;
    }

    private void invalidateFonts() {
        mFonts = null;
    }

    @Nullable
    private static FontInfo getFontInfo(@Nullable Font font, float textSize,
            @Nullable AffineTransform transform) {
        if (font == null) {
            return null;
        }

        Font transformedFont = font.deriveFont(textSize);
        if (transform != null) {
            // TODO: support skew
            transformedFont = transformedFont.deriveFont(transform);
        }

        // The metrics here don't have anti-aliasing set.
        return new FontInfo(transformedFont,
                Toolkit.getDefaultToolkit().getFontMetrics(transformedFont));
    }

    /*package*/ RectF measureText(char[] text, int index, int count, float[] advances,
            int advancesIndex, int bidiFlags) {
        return new BidiRenderer(null, this, text)
                .renderText(index, index + count, bidiFlags, advances, advancesIndex, false);
    }

    /*package*/ RectF measureText(char[] text, int index, int count, float[] advances,
            int advancesIndex, boolean isRtl) {
        return new BidiRenderer(null, this, text)
                .renderText(index, index + count, isRtl, advances, advancesIndex, false);
    }

    private float getFontMetrics(FontMetrics metrics) {
        List<FontInfo> fonts = getFonts();
        if (fonts.size() > 0) {
            java.awt.FontMetrics javaMetrics = fonts.get(0).mMetrics;
            if (metrics != null) {
                // Android expects negative ascent so we invert the value from Java.
                metrics.top = - javaMetrics.getMaxAscent();
                metrics.ascent = - javaMetrics.getAscent();
                metrics.descent = javaMetrics.getDescent();
                metrics.bottom = javaMetrics.getMaxDescent();
                metrics.leading = javaMetrics.getLeading();
            }

            return javaMetrics.getHeight();
        }

        return 0;
    }

    private void setTextLocale(String locale) {
        mLocale = new Locale(locale);
    }

    private static void setFlag(long nativePaint, int flagMask, boolean flagValue) {
        // get the delegate from the native int.
        Paint_Delegate delegate = sManager.getDelegate(nativePaint);
        if (delegate == null) {
            return;
        }

        if (flagValue) {
            delegate.mFlags |= flagMask;
        } else {
            delegate.mFlags &= ~flagMask;
        }
    }
}
