/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.clockwork.globalactions;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

/**
 * A class that animates a view entrance by sliding it in from the bottom.
 * This is meant to mirror the start up animation used in
 * {@link com.google.android.clockwork.common.wearable.wearmaterial.list.FadingWearableRecyclerView}
 */
public class ViewEntranceAnimationManager implements View.OnAttachStateChangeListener {

    private final int ANIMATION_DURATION = 300;
    private final int ANIMATION_DELAY = 150;
    private final Interpolator ANIMATION_INTERPOLATOR =
            new PathInterpolator(/* controlX1= */ 0,
                    /* controlY1= */ 0,
                    /* controlX2= */ 0,
                    /* controlY2= */ 1);

    private final Animator.AnimatorListener mAnimatorListener;

    ViewEntranceAnimationManager(Animator.AnimatorListener animatorListener) {
        mAnimatorListener = animatorListener;
    }

    @Override
    public void onViewAttachedToWindow(View attachedView) {
        int origPaddingStart = attachedView.getPaddingStart();
        int origPaddingTop = attachedView.getPaddingTop();
        int origPaddingEnd = attachedView.getPaddingEnd();
        int origPaddingBottom = attachedView.getPaddingBottom();
        int offset = getScreenHeight(attachedView.getContext());

        attachedView.setPaddingRelative(
                origPaddingStart,
                origPaddingTop + offset,
                origPaddingEnd,
                origPaddingBottom);
        ValueAnimator animator = ValueAnimator.ofInt(offset, 0);
        animator.addUpdateListener(
                valueAnimator ->
                        attachedView.setPaddingRelative(
                                origPaddingStart,
                                origPaddingTop + (Integer) valueAnimator.getAnimatedValue(),
                                origPaddingEnd,
                                origPaddingBottom));
        animator.setStartDelay(ANIMATION_DELAY);
        animator.setDuration(ANIMATION_DURATION);
        animator.setInterpolator(ANIMATION_INTERPOLATOR);
        animator.addListener(mAnimatorListener);
        animator.start();
    }

    @Override
    public void onViewDetachedFromWindow(View detachedView) {
    }

    private static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }
}
