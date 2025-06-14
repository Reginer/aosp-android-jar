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
package com.android.internal.widget.remotecompose.player.platform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.internal.widget.remotecompose.core.CoreDocument;
import com.android.internal.widget.remotecompose.core.RemoteContext;
import com.android.internal.widget.remotecompose.core.operations.Header;
import com.android.internal.widget.remotecompose.core.operations.RootContentBehavior;
import com.android.internal.widget.remotecompose.core.operations.Theme;
import com.android.internal.widget.remotecompose.player.RemoteComposeDocument;

import java.util.Set;

/** Internal view handling the actual painting / interactions */
public class RemoteComposeCanvas extends FrameLayout implements View.OnAttachStateChangeListener {

    static final boolean USE_VIEW_AREA_CLICK = true; // Use views to represent click areas
    static final float DEFAULT_FRAME_RATE = 60f;
    static final float POST_TO_NEXT_FRAME_THRESHOLD = 60f;

    RemoteComposeDocument mDocument = null;
    int mTheme = Theme.LIGHT;
    boolean mInActionDown = false;
    int mDebug = 0;
    boolean mHasClickAreas = false;
    Point mActionDownPoint = new Point(0, 0);
    AndroidRemoteContext mARContext = new AndroidRemoteContext();
    float mDensity = Float.NaN;
    long mStart = System.nanoTime();

    long mLastFrameDelay = 1;
    float mMaxFrameRate = DEFAULT_FRAME_RATE; // frames per seconds
    long mMaxFrameDelay = (long) (1000 / mMaxFrameRate);

    long mLastFrameCall = System.currentTimeMillis();

    private Choreographer mChoreographer;
    private Choreographer.FrameCallback mFrameCallback =
            new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    mARContext.currentTime = frameTimeNanos / 1000000;
                    mARContext.setDebug(mDebug);
                    postInvalidateOnAnimation();
                }
            };

    public RemoteComposeCanvas(Context context) {
        super(context);
        addOnAttachStateChangeListener(this);
    }

    public RemoteComposeCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        addOnAttachStateChangeListener(this);
    }

    public RemoteComposeCanvas(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.WHITE);
        addOnAttachStateChangeListener(this);
    }

    public void setDebug(int value) {
        if (mDebug != value) {
            mDebug = value;
            if (USE_VIEW_AREA_CLICK) {
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    if (child instanceof ClickAreaView) {
                        ((ClickAreaView) child).setDebug(mDebug == 1);
                    }
                }
            }
            invalidate();
        }
    }

    public void setDocument(RemoteComposeDocument value) {
        mDocument = value;
        mMaxFrameRate = DEFAULT_FRAME_RATE;
        mDocument.initializeContext(mARContext);
        mDisable = false;
        mARContext.setDocLoadTime();
        mARContext.setAnimationEnabled(true);
        mARContext.setDensity(mDensity);
        mARContext.setUseChoreographer(true);
        setContentDescription(mDocument.getDocument().getContentDescription());

        updateClickAreas();
        requestLayout();
        mARContext.loadFloat(RemoteContext.ID_TOUCH_EVENT_TIME, -Float.MAX_VALUE);
        mARContext.loadFloat(RemoteContext.ID_FONT_SIZE, getDefaultTextSize());

        invalidate();
        Integer fps = (Integer) mDocument.getDocument().getProperty(Header.DOC_DESIRED_FPS);
        if (fps != null && fps > 0) {
            mMaxFrameRate = fps;
            mMaxFrameDelay = (long) (1000 / mMaxFrameRate);
        }
    }

    @Override
    public void onViewAttachedToWindow(View view) {
        if (mChoreographer == null) {
            mChoreographer = Choreographer.getInstance();
            mChoreographer.postFrameCallback(mFrameCallback);
        }
        mDensity = getContext().getResources().getDisplayMetrics().density;
        mARContext.setDensity(mDensity);
        if (mDocument == null) {
            return;
        }
        updateClickAreas();
    }

    private void updateClickAreas() {
        if (USE_VIEW_AREA_CLICK && mDocument != null) {
            mHasClickAreas = false;
            Set<CoreDocument.ClickAreaRepresentation> clickAreas =
                    mDocument.getDocument().getClickAreas();
            removeAllViews();
            for (CoreDocument.ClickAreaRepresentation area : clickAreas) {
                ClickAreaView viewArea =
                        new ClickAreaView(
                                getContext(),
                                mDebug == 1,
                                area.getId(),
                                area.getContentDescription(),
                                area.getMetadata());
                int w = (int) area.width();
                int h = (int) area.height();
                FrameLayout.LayoutParams param = new FrameLayout.LayoutParams(w, h);
                param.width = w;
                param.height = h;
                param.leftMargin = (int) area.getLeft();
                param.topMargin = (int) area.getTop();
                viewArea.setOnClickListener(
                        view1 ->
                                mDocument
                                        .getDocument()
                                        .performClick(
                                                mARContext, area.getId(), area.getMetadata()));
                addView(viewArea, param);
            }
            if (!clickAreas.isEmpty()) {
                mHasClickAreas = true;
            }
        }
    }

    public void setHapticEngine(CoreDocument.HapticEngine engine) {
        mDocument.getDocument().setHapticEngine(engine);
    }

    @Override
    public void onViewDetachedFromWindow(View view) {
        if (mChoreographer != null) {
            mChoreographer.removeFrameCallback(mFrameCallback);
            mChoreographer = null;
        }
        removeAllViews();
    }

    public String[] getNamedColors() {
        return mDocument.getNamedColors();
    }

    /**
     * Gets a array of Names of the named variables of a specific type defined in the loaded doc.
     *
     * @param type the type of variable NamedVariable.COLOR_TYPE, STRING_TYPE, etc
     * @return array of name or null
     */
    public String[] getNamedVariables(int type) {
        return mDocument.getNamedVariables(type);
    }

    /**
     * set the color associated with this name.
     *
     * @param colorName Name of color typically "android.xxx"
     * @param colorValue "the argb value"
     */
    public void setColor(String colorName, int colorValue) {
        mARContext.setNamedColorOverride(colorName, colorValue);
    }

    /**
     * set the value of a long associated with this name.
     *
     * @param name Name of color typically "android.xxx"
     * @param value the long value
     */
    public void setLong(String name, long value) {
        mARContext.setNamedLong(name, value);
    }

    public RemoteComposeDocument getDocument() {
        return mDocument;
    }

    public void setLocalString(String name, String content) {
        mARContext.setNamedStringOverride(name, content);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    public void clearLocalString(String name) {
        mARContext.clearNamedStringOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    public void setLocalInt(String name, int content) {
        mARContext.setNamedIntegerOverride(name, content);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    public void clearLocalInt(String name) {
        mARContext.clearNamedIntegerOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Set a local named color
     *
     * @param name
     * @param content
     */
    public void setLocalColor(String name, int content) {
        mARContext.setNamedColorOverride(name, content);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    /**
     * Clear a local named color
     *
     * @param name
     */
    public void clearLocalColor(String name) {
        mARContext.clearNamedDataOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    public void setLocalFloat(String name, Float content) {
        mARContext.setNamedFloatOverride(name, content);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    public void clearLocalFloat(String name) {
        mARContext.clearNamedFloatOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    public void setLocalBitmap(String name, Bitmap content) {
        mARContext.setNamedDataOverride(name, content);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    public void clearLocalBitmap(String name) {
        mARContext.clearNamedDataOverride(name);
        if (mDocument != null) {
            mDocument.invalidate();
        }
    }

    public int hasSensorListeners(int[] ids) {
        int count = 0;
        for (int id = RemoteContext.ID_ACCELERATION_X; id <= RemoteContext.ID_LIGHT; id++) {
            if (mARContext.mRemoteComposeState.hasListener(id)) {
                ids[count++] = id;
            }
        }
        return count;
    }

    /**
     * set a float externally
     *
     * @param id
     * @param value
     */
    public void setExternalFloat(int id, float value) {
        mARContext.loadFloat(id, value);
    }

    /**
     * Returns true if the document supports drag touch events
     *
     * @return true if draggable content, false otherwise
     */
    public boolean isDraggable() {
        if (mDocument == null) {
            return false;
        }
        return mDocument.getDocument().hasTouchListener();
    }

    /**
     * Check shaders and disable them
     *
     * @param shaderControl the callback to validate the shader
     */
    public void checkShaders(CoreDocument.ShaderControl shaderControl) {
        mDocument.getDocument().checkShaders(mARContext, shaderControl);
    }

    /**
     * Set to true to use the choreographer
     *
     * @param value
     */
    public void setUseChoreographer(boolean value) {
        mARContext.setUseChoreographer(value);
    }

    public RemoteContext getRemoteContext() {
        return mARContext;
    }

    public interface ClickCallbacks {
        void click(int id, String metadata);
    }

    public void addIdActionListener(ClickCallbacks callback) {
        if (mDocument == null) {
            return;
        }
        mDocument.getDocument().addIdActionListener((id, metadata) -> callback.click(id, metadata));
    }

    public int getTheme() {
        return mTheme;
    }

    public void setTheme(int theme) {
        this.mTheme = theme;
    }

    private VelocityTracker mVelocityTracker = null;

    public boolean onTouchEvent(MotionEvent event) {
        int index = event.getActionIndex();
        int action = event.getActionMasked();
        int pointerId = event.getPointerId(index);
        if (USE_VIEW_AREA_CLICK && mHasClickAreas) {
            return super.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mActionDownPoint.x = (int) event.getX();
                mActionDownPoint.y = (int) event.getY();
                CoreDocument doc = mDocument.getDocument();
                if (doc.hasTouchListener()) {
                    mARContext.loadFloat(
                            RemoteContext.ID_TOUCH_EVENT_TIME, mARContext.getAnimationTime());
                    mInActionDown = true;
                    if (mVelocityTracker == null) {
                        mVelocityTracker = VelocityTracker.obtain();
                    } else {
                        mVelocityTracker.clear();
                    }
                    mVelocityTracker.addMovement(event);
                    doc.touchDown(mARContext, event.getX(), event.getY());
                    invalidate();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_CANCEL:
                mInActionDown = false;
                doc = mDocument.getDocument();
                if (doc.hasTouchListener()) {
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float dx = mVelocityTracker.getXVelocity(pointerId);
                    float dy = mVelocityTracker.getYVelocity(pointerId);
                    doc.touchCancel(mARContext, event.getX(), event.getY(), dx, dy);
                    invalidate();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_UP:
                mInActionDown = false;
                performClick();
                doc = mDocument.getDocument();
                if (doc.hasTouchListener()) {
                    mARContext.loadFloat(
                            RemoteContext.ID_TOUCH_EVENT_TIME, mARContext.getAnimationTime());
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float dx = mVelocityTracker.getXVelocity(pointerId);
                    float dy = mVelocityTracker.getYVelocity(pointerId);
                    doc.touchUp(mARContext, event.getX(), event.getY(), dx, dy);
                    invalidate();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                if (mInActionDown) {
                    if (mVelocityTracker != null) {
                        mARContext.loadFloat(
                                RemoteContext.ID_TOUCH_EVENT_TIME, mARContext.getAnimationTime());
                        mVelocityTracker.addMovement(event);
                        doc = mDocument.getDocument();
                        boolean repaint = doc.touchDrag(mARContext, event.getX(), event.getY());
                        if (repaint) {
                            invalidate();
                        }
                    }
                    return true;
                }
                return false;
        }
        return false;
    }

    @Override
    public boolean performClick() {
        if (USE_VIEW_AREA_CLICK && mHasClickAreas) {
            return super.performClick();
        }
        mDocument
                .getDocument()
                .onClick(mARContext, (float) mActionDownPoint.x, (float) mActionDownPoint.y);
        super.performClick();
        invalidate();
        return true;
    }

    public int measureDimension(int measureSpec, int intrinsicSize) {
        int result = intrinsicSize;
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        switch (mode) {
            case MeasureSpec.EXACTLY:
                result = size;
                break;
            case MeasureSpec.AT_MOST:
                result = Integer.min(size, intrinsicSize);
                break;
            case MeasureSpec.UNSPECIFIED:
                result = intrinsicSize;
        }
        return result;
    }

    private static final float[] sScaleOutput = new float[2];

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mDocument == null) {
            return;
        }
        int preWidth = getWidth();
        int preHeight = getHeight();
        int w = measureDimension(widthMeasureSpec, mDocument.getWidth());
        int h = measureDimension(heightMeasureSpec, mDocument.getHeight());

        if (!USE_VIEW_AREA_CLICK) {
            if (mDocument.getDocument().getContentSizing() == RootContentBehavior.SIZING_SCALE) {
                mDocument.getDocument().computeScale(w, h, sScaleOutput);
                w = (int) (mDocument.getWidth() * sScaleOutput[0]);
                h = (int) (mDocument.getHeight() * sScaleOutput[1]);
            }
        }
        setMeasuredDimension(w, h);
        if (preWidth != w || preHeight != h) {
            mDocument.getDocument().invalidateMeasure();
        }
    }

    private int mCount;
    private long mTime = System.nanoTime();
    private long mDuration;
    private boolean mEvalTime = false; // turn on to measure eval time
    private float mLastAnimationTime = 0.1f; // set to random non 0 number
    private boolean mDisable = false;

    /**
     * This returns the amount of time in ms the player used to evalueate a pass it is averaged over
     * a number of evaluations.
     *
     * @return time in ms
     */
    public float getEvalTime() {
        if (!mEvalTime) {
            mEvalTime = true;
            return 0.0f;
        }
        double avg = mDuration / (double) mCount;
        if (mCount > 100) {
            mDuration /= 2;
            mCount /= 2;
        }
        return (float) (avg * 1E-6); // ms
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDocument == null) {
            return;
        }
        if (mDisable) {
            drawDisable(canvas);
            return;
        }
        try {

            long start = mEvalTime ? System.nanoTime() : 0; // measure execut of commands

            float animationTime = (System.nanoTime() - mStart) * 1E-9f;
            mARContext.setAnimationTime(animationTime);
            mARContext.loadFloat(RemoteContext.ID_ANIMATION_TIME, animationTime);
            float loopTime = animationTime - mLastAnimationTime;
            mARContext.loadFloat(RemoteContext.ID_ANIMATION_DELTA_TIME, loopTime);
            mLastAnimationTime = animationTime;
            mARContext.setAnimationEnabled(true);
            mARContext.currentTime = System.currentTimeMillis();
            mARContext.setDebug(mDebug);
            float density = getContext().getResources().getDisplayMetrics().density;
            mARContext.useCanvas(canvas);
            mARContext.mWidth = getWidth();
            mARContext.mHeight = getHeight();
            mDocument.paint(mARContext, mTheme);
            if (mDebug == 1) {
                mCount++;
                if (System.nanoTime() - mTime > 1000000000L) {
                    System.out.println(" count " + mCount + " fps");
                    mCount = 0;
                    mTime = System.nanoTime();
                }
            }
            int nextFrame = mDocument.needsRepaint();
            if (nextFrame > 0) {
                if (mMaxFrameRate >= POST_TO_NEXT_FRAME_THRESHOLD) {
                    mLastFrameDelay = nextFrame;
                } else {
                    mLastFrameDelay = Math.max(mMaxFrameDelay, nextFrame);
                }
                if (mChoreographer != null) {
                    if (mDebug == 1) {
                        System.err.println(
                                "RC : POST CHOREOGRAPHER WITH "
                                        + mLastFrameDelay
                                        + " (nextFrame was "
                                        + nextFrame
                                        + ", max delay "
                                        + mMaxFrameDelay
                                        + ", "
                                        + " max framerate is "
                                        + mMaxFrameRate
                                        + ")");
                    }
                    mChoreographer.postFrameCallbackDelayed(mFrameCallback, mLastFrameDelay);
                }
                if (!mARContext.useChoreographer()) {
                    invalidate();
                }
            } else {
                if (mChoreographer != null) {
                    mChoreographer.removeFrameCallback(mFrameCallback);
                }
            }
            if (mEvalTime) {
                mDuration += System.nanoTime() - start;
                mCount++;
            }
        } catch (Exception ex) {
            mARContext.getLastOpCount();
            mDisable = true;
            invalidate();
        }
        if (mDebug == 1) {
            long frameDelay = System.currentTimeMillis() - mLastFrameCall;
            System.err.println(
                    "RC : Delay since last frame "
                            + frameDelay
                            + " ms ("
                            + (1000f / (float) frameDelay)
                            + " fps)");
            mLastFrameCall = System.currentTimeMillis();
        }
    }

    private void drawDisable(Canvas canvas) {
        Rect rect = new Rect();
        canvas.drawColor(Color.BLACK);
        Paint paint = new Paint();
        paint.setTextSize(128f);
        paint.setColor(Color.RED);
        int w = getWidth();
        int h = getHeight();

        String str = "⚠";
        paint.getTextBounds(str, 0, 1, rect);

        float x = w / 2f - rect.width() / 2f - rect.left;
        float y = h / 2f + rect.height() / 2f - rect.bottom;

        canvas.drawText(str, x, y, paint);
    }

    private float getDefaultTextSize() {
        return new TextView(getContext()).getTextSize();
    }
}
