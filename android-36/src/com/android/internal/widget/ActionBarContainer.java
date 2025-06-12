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

package com.android.internal.widget;

import android.annotation.NonNull;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * This class acts as a container for the action bar view and action mode context views.
 * It applies special styles as needed to help handle animated transitions between them.
 * @hide
 */
public class ActionBarContainer extends FrameLayout {
    private boolean mIsTransitioning;
    private View mTabContainer;
    private View mActionBarView;
    private View mActionContextView;

    private Drawable mBackground;
    private Drawable mStackedBackground;
    private Drawable mSplitBackground;
    private boolean mIsSplit;
    private boolean mIsStacked;
    private int mHeight;

    public ActionBarContainer(Context context) {
        this(context, null);
    }

    public ActionBarContainer(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Set a transparent background so that we project appropriately.
        setBackground(new ActionBarBackgroundDrawable());

        TypedArray a = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.ActionBar);
        mBackground = a.getDrawable(com.android.internal.R.styleable.ActionBar_background);
        mStackedBackground = a.getDrawable(
                com.android.internal.R.styleable.ActionBar_backgroundStacked);
        mHeight = a.getDimensionPixelSize(com.android.internal.R.styleable.ActionBar_height, -1);

        if (getId() == com.android.internal.R.id.split_action_bar) {
            mIsSplit = true;
            mSplitBackground = a.getDrawable(
                    com.android.internal.R.styleable.ActionBar_backgroundSplit);
        }
        a.recycle();

        setWillNotDraw(mIsSplit ? mSplitBackground == null :
                mBackground == null && mStackedBackground == null);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mActionBarView = findViewById(com.android.internal.R.id.action_bar);
        mActionContextView = findViewById(com.android.internal.R.id.action_context_bar);
    }

    public void setPrimaryBackground(Drawable bg) {
        if (mBackground != null) {
            mBackground.setCallback(null);
            unscheduleDrawable(mBackground);
        }
        mBackground = bg;
        if (bg != null) {
            bg.setCallback(this);
            if (mActionBarView != null) {
                bg.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            }
        }
        setWillNotDraw(mIsSplit ? mSplitBackground == null :
                mBackground == null && mStackedBackground == null);
        invalidate();
    }

    public void setStackedBackground(Drawable bg) {
        if (mStackedBackground != null) {
            mStackedBackground.setCallback(null);
            unscheduleDrawable(mStackedBackground);
        }
        mStackedBackground = bg;
        if (bg != null) {
            bg.setCallback(this);
            if ((mIsStacked && mStackedBackground != null)) {
                mStackedBackground.setBounds(mTabContainer.getLeft(), mTabContainer.getTop(),
                        mTabContainer.getRight(), mTabContainer.getBottom());
            }
        }
        setWillNotDraw(mIsSplit ? mSplitBackground == null :
                mBackground == null && mStackedBackground == null);
        invalidate();
    }

    public void setSplitBackground(Drawable bg) {
        if (mSplitBackground != null) {
            mSplitBackground.setCallback(null);
            unscheduleDrawable(mSplitBackground);
        }
        mSplitBackground = bg;
        if (bg != null) {
            bg.setCallback(this);
            if (mIsSplit && mSplitBackground != null) {
                mSplitBackground.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
            }
        }
        setWillNotDraw(mIsSplit ? mSplitBackground == null :
                mBackground == null && mStackedBackground == null);
        invalidate();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        final boolean isVisible = visibility == VISIBLE;
        if (mBackground != null) mBackground.setVisible(isVisible, false);
        if (mStackedBackground != null) mStackedBackground.setVisible(isVisible, false);
        if (mSplitBackground != null) mSplitBackground.setVisible(isVisible, false);
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return (who == mBackground && !mIsSplit) || (who == mStackedBackground && mIsStacked) ||
                (who == mSplitBackground && mIsSplit) || super.verifyDrawable(who);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        final int[] state = getDrawableState();
        boolean changed = false;

        final Drawable background = mBackground;
        if (background != null && background.isStateful()) {
            changed |= background.setState(state);
        }

        final Drawable stackedBackground = mStackedBackground;
        if (stackedBackground != null && stackedBackground.isStateful()) {
            changed |= stackedBackground.setState(state);
        }

        final Drawable splitBackground = mSplitBackground;
        if (splitBackground != null && splitBackground.isStateful()) {
            changed |= splitBackground.setState(state);
        }

        if (changed) {
            invalidate();
        }
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mBackground != null) {
            mBackground.jumpToCurrentState();
        }
        if (mStackedBackground != null) {
            mStackedBackground.jumpToCurrentState();
        }
        if (mSplitBackground != null) {
            mSplitBackground.jumpToCurrentState();
        }
    }

    /**
     * @hide
     */
    @Override
    public void onResolveDrawables(int layoutDirection) {
        super.onResolveDrawables(layoutDirection);
        if (mBackground != null) {
            mBackground.setLayoutDirection(layoutDirection);
        }
        if (mStackedBackground != null) {
            mStackedBackground.setLayoutDirection(layoutDirection);
        }
        if (mSplitBackground != null) {
            mSplitBackground.setLayoutDirection(layoutDirection);
        }
    }

    /**
     * Set the action bar into a "transitioning" state. While transitioning
     * the bar will block focus and touch from all of its descendants. This
     * prevents the user from interacting with the bar while it is animating
     * in or out.
     *
     * @param isTransitioning true if the bar is currently transitioning, false otherwise.
     */
    public void setTransitioning(boolean isTransitioning) {
        mIsTransitioning = isTransitioning;
        setDescendantFocusability(isTransitioning ? FOCUS_BLOCK_DESCENDANTS
                : FOCUS_AFTER_DESCENDANTS);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mIsTransitioning || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);

        // An action bar always eats touch events.
        return true;
    }

    @Override
    public boolean onHoverEvent(MotionEvent ev) {
        super.onHoverEvent(ev);

        // An action bar always eats hover events.
        return true;
    }

    public void setTabContainer(ScrollingTabContainerView tabView) {
        if (mTabContainer != null) {
            removeView(mTabContainer);
        }
        mTabContainer = tabView;
        if (tabView != null) {
            addView(tabView);
            final ViewGroup.LayoutParams lp = tabView.getLayoutParams();
            lp.width = LayoutParams.MATCH_PARENT;
            lp.height = LayoutParams.WRAP_CONTENT;
            tabView.setAllowCollapse(false);
        }
    }

    public View getTabContainer() {
        return mTabContainer;
    }

    @Override
    public ActionMode startActionModeForChild(
            View child, ActionMode.Callback callback, int type) {
        if (type != ActionMode.TYPE_PRIMARY) {
            return super.startActionModeForChild(child, callback, type);
        }
        return null;
    }

    private static boolean isCollapsed(View view) {
        return view == null || view.getVisibility() == GONE || view.getMeasuredHeight() == 0;
    }

    private int getMeasuredHeightWithMargins(View view) {
        final LayoutParams lp = (LayoutParams) view.getLayoutParams();
        return view.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mActionBarView == null &&
                MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST && mHeight >= 0) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    Math.min(mHeight, MeasureSpec.getSize(heightMeasureSpec)), MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mActionBarView == null) return;

        if (mTabContainer != null && mTabContainer.getVisibility() != GONE) {
            final int verticalPadding = getPaddingTop() + getPaddingBottom();
            int nonTabMaxHeight = 0;
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if (child == mTabContainer) {
                    continue;
                }
                nonTabMaxHeight = Math.max(nonTabMaxHeight, isCollapsed(child) ? 0 :
                        getMeasuredHeightWithMargins(child));
            }
            final int mode = MeasureSpec.getMode(heightMeasureSpec);
            final int maxHeight = mode == MeasureSpec.AT_MOST ?
                    MeasureSpec.getSize(heightMeasureSpec) : Integer.MAX_VALUE;
            setMeasuredDimension(getMeasuredWidth(),
                    Math.min(
                            verticalPadding + nonTabMaxHeight
                                    + getMeasuredHeightWithMargins(mTabContainer),
                            maxHeight));
        }
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        final View tabContainer = mTabContainer;
        final boolean hasTabs = tabContainer != null && tabContainer.getVisibility() != GONE;

        if (tabContainer != null && tabContainer.getVisibility() != GONE) {
            final int containerHeight = getMeasuredHeight();
            final LayoutParams lp = (LayoutParams) tabContainer.getLayoutParams();
            final int tabHeight = tabContainer.getMeasuredHeight();
            tabContainer.layout(l, containerHeight - tabHeight - lp.bottomMargin, r,
                    containerHeight - lp.bottomMargin);
        }

        boolean needsInvalidate = false;
        if (mIsSplit) {
            if (mSplitBackground != null) {
                mSplitBackground.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                needsInvalidate = true;
            }
        } else {
            if (mBackground != null) {
                if ((mActionBarView.getVisibility() == View.VISIBLE) || (mActionContextView != null
                        && mActionContextView.getVisibility() == View.VISIBLE)) {
                    mBackground.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                } else {
                    mBackground.setBounds(0, 0, 0, 0);
                }
                needsInvalidate = true;
            }
            mIsStacked = hasTabs;
            if (hasTabs && mStackedBackground != null) {
                mStackedBackground.setBounds(tabContainer.getLeft(), tabContainer.getTop(),
                        tabContainer.getRight(), tabContainer.getBottom());
                needsInvalidate = true;
            }
        }

        if (needsInvalidate) {
            invalidate();
        }
    }

    /**
     * Placeholder drawable so that we don't break background display lists and
     * projection surfaces.
     */
    private class ActionBarBackgroundDrawable extends Drawable {
        @Override
        public void draw(Canvas canvas) {
            if (mIsSplit) {
                if (mSplitBackground != null) {
                    mSplitBackground.draw(canvas);
                }
            } else {
                if (mBackground != null) {
                    mBackground.draw(canvas);
                }
                if (mStackedBackground != null && mIsStacked) {
                    mStackedBackground.draw(canvas);
                }
            }
        }

        @Override
        public void getOutline(@NonNull Outline outline) {
            if (mIsSplit) {
                if (mSplitBackground != null) {
                    mSplitBackground.getOutline(outline);
                }
            } else {
                // ignore the stacked background for shadow casting
                if (mBackground != null) {
                    mBackground.getOutline(outline);
                }
            }
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            if (mIsSplit) {
                if (mSplitBackground != null
                        && mSplitBackground.getOpacity() == PixelFormat.OPAQUE) {
                    return PixelFormat.OPAQUE;
                }
            } else {
                if (mIsStacked && (mStackedBackground == null
                        || mStackedBackground.getOpacity() != PixelFormat.OPAQUE)) {
                    return PixelFormat.UNKNOWN;
                }
                if (!isCollapsed(mActionBarView) && mBackground != null
                        && mBackground.getOpacity() == PixelFormat.OPAQUE) {
                    return PixelFormat.OPAQUE;
                }
            }

            return PixelFormat.UNKNOWN;
        }
    }
}
