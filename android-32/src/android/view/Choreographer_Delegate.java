/*
 * Copyright (C) 2012 The Android Open Source Project
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

import com.android.tools.layoutlib.annotations.LayoutlibDelegate;

import android.animation.AnimationHandler;
import android.util.TimeUtils;
import android.view.animation.AnimationUtils;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Delegate used to provide new implementation of a select few methods of {@link Choreographer}
 *
 * Through the layoutlib_create tool, the original  methods of Choreographer have been
 * replaced by calls to methods of the same name in this delegate class.
 *
 */
public class Choreographer_Delegate {
    private static final AtomicReference<Choreographer> mInstance = new AtomicReference<Choreographer>();

    private static final int MS_16 = 16000000;

    @LayoutlibDelegate
    public static Choreographer getInstance() {
        if (mInstance.get() == null) {
            mInstance.compareAndSet(null, Choreographer.getInstance_Original());
        }

        return mInstance.get();
    }

    @LayoutlibDelegate
    public static float getRefreshRate() {
        return 60.f;
    }

    @LayoutlibDelegate
    static void scheduleVsyncLocked(Choreographer thisChoreographer) {
        // do nothing
    }

    public static void doFrame(long frameTimeNanos) {
        Choreographer thisChoreographer = Choreographer.getInstance();

        AnimationUtils.lockAnimationClock(frameTimeNanos / TimeUtils.NANOS_PER_MS);

        try {
            thisChoreographer.mLastFrameTimeNanos = frameTimeNanos - thisChoreographer.getFrameIntervalNanos();
            thisChoreographer.mFrameInfo.markInputHandlingStart();
            thisChoreographer.doCallbacks(Choreographer.CALLBACK_INPUT, frameTimeNanos, MS_16);

            thisChoreographer.mFrameInfo.markAnimationsStart();
            thisChoreographer.doCallbacks(Choreographer.CALLBACK_ANIMATION, frameTimeNanos, MS_16);

            thisChoreographer.mFrameInfo.markPerformTraversalsStart();
            thisChoreographer.doCallbacks(Choreographer.CALLBACK_TRAVERSAL, frameTimeNanos, MS_16);

            thisChoreographer.doCallbacks(Choreographer.CALLBACK_COMMIT, frameTimeNanos, MS_16);
        } finally {
            AnimationUtils.unlockAnimationClock();
        }
    }

    public static void clearFrames() {
        Choreographer thisChoreographer = Choreographer.getInstance();

        thisChoreographer.removeCallbacks(Choreographer.CALLBACK_INPUT, null, null);
        thisChoreographer.removeCallbacks(Choreographer.CALLBACK_ANIMATION, null, null);
        thisChoreographer.removeCallbacks(Choreographer.CALLBACK_TRAVERSAL, null, null);
        thisChoreographer.removeCallbacks(Choreographer.CALLBACK_COMMIT, null, null);

        // Release animation handler instance since it holds references to the callbacks
        AnimationHandler.sAnimatorHandler.set(null);
    }

    public static void dispose() {
        clearFrames();
        Choreographer.releaseInstance();
    }
}
