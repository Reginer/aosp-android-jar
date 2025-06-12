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

package android.animation;

import android.view.animation.AnimationUtils;

/**
 * This class provides a simple callback mechanism to listeners that is synchronized with all
 * other animators in the system. There is no duration, interpolation, or object value-setting
 * with this Animator. Instead, it is simply started, after which it proceeds to send out events
 * on every animation frame to its TimeListener (if set), with information about this animator,
 * the total elapsed time, and the elapsed time since the previous animation frame.
 */
public class TimeAnimator extends ValueAnimator {

    private TimeListener mListener;
    private long mPreviousTime = -1;

    @Override
    public void start() {
        mPreviousTime = -1;
        super.start();
    }

    @Override
    boolean animateBasedOnTime(long currentTime) {
        if (mListener != null) {
            long totalTime = currentTime - mStartTime;
            long deltaTime = (mPreviousTime < 0) ? 0 : (currentTime - mPreviousTime);
            mPreviousTime = currentTime;
            mListener.onTimeUpdate(this, totalTime, deltaTime);
        }
        return false;
    }

    @Override
    public void setCurrentPlayTime(long playTime) {
        long currentTime = AnimationUtils.currentAnimationTimeMillis();
        mStartTime = Math.max(mStartTime, currentTime - playTime);
        mStartTimeCommitted = true; // do not allow start time to be compensated for jank
        animateBasedOnTime(currentTime);
    }

    /**
     * Sets a listener that is sent update events throughout the life of
     * an animation.
     *
     * @param listener the listener to be set.
     */
    public void setTimeListener(TimeListener listener) {
        mListener = listener;
    }

    @Override
    void animateValue(float fraction) {
        // Noop
    }

    @Override
    void initAnimation() {
        // noop
    }

    /**
     * Implementors of this interface can set themselves as update listeners
     * to a <code>TimeAnimator</code> instance to receive callbacks on every animation
     * frame to receive the total time since the animator started and the delta time
     * since the last frame. The first time the listener is called,
     * deltaTime will be zero. The same is true for totalTime, unless the animator was
     * set to a specific {@link ValueAnimator#setCurrentPlayTime(long) currentPlayTime}
     * prior to starting.
     */
    public static interface TimeListener {
        /**
         * <p>Notifies listeners of the occurrence of another frame of the animation,
         * along with information about the elapsed time.</p>
         *
         * @param animation The animator sending out the notification.
         * @param totalTime The total time elapsed since the animator started, in milliseconds.
         * @param deltaTime The time elapsed since the previous frame, in milliseconds.
         */
        void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime);

    }
}
