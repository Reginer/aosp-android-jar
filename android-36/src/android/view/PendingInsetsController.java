/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.CancellationSignal;
import android.view.WindowInsets.Type.InsetsType;
import android.view.animation.Interpolator;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;

/**
 * An insets controller that keeps track of pending requests. This is such that an app can freely
 * use {@link WindowInsetsController} before the view root is attached during activity startup.
 * @hide
 */
public class PendingInsetsController implements WindowInsetsController {

    private static final int KEEP_BEHAVIOR = -1;
    private final ArrayList<PendingRequest> mRequests = new ArrayList<>();
    private @Appearance int mAppearance;
    private @Appearance int mAppearanceMask;
    private @Appearance int mAppearanceFromResource;
    private @Appearance int mAppearanceFromResourceMask;
    private @Behavior int mBehavior = KEEP_BEHAVIOR;
    private boolean mAnimationsDisabled;
    private final InsetsState mDummyState = new InsetsState();
    private InsetsController mReplayedInsetsController;
    private ArrayList<OnControllableInsetsChangedListener> mControllableInsetsChangedListeners
            = new ArrayList<>();
    private int mImeCaptionBarInsetsHeight = 0;
    private WindowInsetsAnimationControlListener mLoggingListener;
    private @InsetsType int mRequestedVisibleTypes = WindowInsets.Type.defaultVisible();

    @Override
    public void show(int types) {
        if (mReplayedInsetsController != null) {
            mReplayedInsetsController.show(types);
        } else {
            mRequests.add(new ShowRequest(types));
            mRequestedVisibleTypes |= types;
        }
    }

    @Override
    public void hide(int types) {
        if (mReplayedInsetsController != null) {
            mReplayedInsetsController.hide(types);
        } else {
            mRequests.add(new HideRequest(types));
            mRequestedVisibleTypes &= ~types;
        }
    }

    @Override
    public void setSystemBarsAppearance(int appearance, int mask) {
        if (mReplayedInsetsController != null) {
            mReplayedInsetsController.setSystemBarsAppearance(appearance, mask);
        } else {
            mAppearance = (mAppearance & ~mask) | (appearance & mask);
            mAppearanceMask |= mask;
        }
    }

    @Override
    public void setSystemBarsAppearanceFromResource(int appearance, int mask) {
        if (mReplayedInsetsController != null) {
            mReplayedInsetsController.setSystemBarsAppearanceFromResource(appearance, mask);
        } else {
            mAppearanceFromResource = (mAppearanceFromResource & ~mask) | (appearance & mask);
            mAppearanceFromResourceMask |= mask;
        }
    }

    @Override
    public int getSystemBarsAppearance() {
        if (mReplayedInsetsController != null) {
            return mReplayedInsetsController.getSystemBarsAppearance();
        }
        return mAppearance | (mAppearanceFromResource & ~mAppearanceMask);
    }

    @Override
    public void setImeCaptionBarInsetsHeight(int height) {
        mImeCaptionBarInsetsHeight = height;
    }

    @Override
    public void setSystemBarsBehavior(int behavior) {
        if (mReplayedInsetsController != null) {
            mReplayedInsetsController.setSystemBarsBehavior(behavior);
        } else {
            mBehavior = behavior;
        }
    }

    @Override
    public int getSystemBarsBehavior() {
        if (mReplayedInsetsController != null) {
            return mReplayedInsetsController.getSystemBarsBehavior();
        }
        if (mBehavior == KEEP_BEHAVIOR) {
            return BEHAVIOR_DEFAULT;
        }
        return mBehavior;
    }

    @Override
    public void setAnimationsDisabled(boolean disable) {
        if (mReplayedInsetsController != null) {
            mReplayedInsetsController.setAnimationsDisabled(disable);
        } else {
            mAnimationsDisabled = disable;
        }
    }

    @Override
    public InsetsState getState() {
        return mDummyState;
    }

    @Override
    public @InsetsType int getRequestedVisibleTypes() {
        if (mReplayedInsetsController != null) {
            return mReplayedInsetsController.getRequestedVisibleTypes();
        }
        return mRequestedVisibleTypes;
    }

    @Override
    public void addOnControllableInsetsChangedListener(
            OnControllableInsetsChangedListener listener) {
        if (mReplayedInsetsController != null) {
            mReplayedInsetsController.addOnControllableInsetsChangedListener(listener);
        } else {
            mControllableInsetsChangedListeners.add(listener);
            listener.onControllableInsetsChanged(this, 0);
        }
    }

    @Override
    public void removeOnControllableInsetsChangedListener(
            OnControllableInsetsChangedListener listener) {
        if (mReplayedInsetsController != null) {
            mReplayedInsetsController.removeOnControllableInsetsChangedListener(listener);
        } else {
            mControllableInsetsChangedListeners.remove(listener);
        }
    }

    /**
     * Replays the commands on {@code controller} and attaches it to this instance such that any
     * calls will be forwarded to the real instance in the future.
     */
    @VisibleForTesting
    public void replayAndAttach(InsetsController controller) {
        if (mBehavior != KEEP_BEHAVIOR) {
            controller.setSystemBarsBehavior(mBehavior);
        }
        if (mAppearanceMask != 0) {
            controller.setSystemBarsAppearance(mAppearance, mAppearanceMask);
        }
        if (mAppearanceFromResourceMask != 0) {
            controller.setSystemBarsAppearanceFromResource(
                    mAppearanceFromResource, mAppearanceFromResourceMask);
        }
        if (mImeCaptionBarInsetsHeight != 0) {
            controller.setImeCaptionBarInsetsHeight(mImeCaptionBarInsetsHeight);
        }
        if (mAnimationsDisabled) {
            controller.setAnimationsDisabled(true);
        }
        int size = mRequests.size();
        for (int i = 0; i < size; i++) {
            mRequests.get(i).replay(controller);
        }
        size = mControllableInsetsChangedListeners.size();
        for (int i = 0; i < size; i++) {
            controller.addOnControllableInsetsChangedListener(
                    mControllableInsetsChangedListeners.get(i));
        }
        if (mLoggingListener != null) {
            controller.setSystemDrivenInsetsAnimationLoggingListener(mLoggingListener);
        }

        // Reset all state so it doesn't get applied twice just in case
        mRequests.clear();
        mControllableInsetsChangedListeners.clear();
        mBehavior = KEEP_BEHAVIOR;
        mAppearance = 0;
        mAppearanceMask = 0;
        mAppearanceFromResource = 0;
        mAppearanceFromResourceMask = 0;
        mAnimationsDisabled = false;
        mLoggingListener = null;
        mRequestedVisibleTypes = WindowInsets.Type.defaultVisible();
        // After replaying, we forward everything directly to the replayed instance.
        mReplayedInsetsController = controller;
    }

    /**
     * Detaches the controller to no longer forward calls to the real instance.
     */
    @VisibleForTesting
    public void detach() {
        mReplayedInsetsController = null;
    }

    @Override
    public void setSystemDrivenInsetsAnimationLoggingListener(
            @Nullable WindowInsetsAnimationControlListener listener) {
        if (mReplayedInsetsController != null) {
            mReplayedInsetsController.setSystemDrivenInsetsAnimationLoggingListener(listener);
        } else {
            mLoggingListener = listener;
        }
    }

    @Override
    public void controlWindowInsetsAnimation(@InsetsType int types, long durationMillis,
            @Nullable Interpolator interpolator,
            CancellationSignal cancellationSignal,
            @NonNull WindowInsetsAnimationControlListener listener) {
        if (mReplayedInsetsController != null) {
            mReplayedInsetsController.controlWindowInsetsAnimation(types, durationMillis,
                    interpolator, cancellationSignal, listener);
        } else {
            listener.onCancelled(null);
        }
    }

    private interface PendingRequest {
        void replay(InsetsController controller);
    }

    private static class ShowRequest implements PendingRequest {

        private final @InsetsType int mTypes;

        public ShowRequest(int types) {
            mTypes = types;
        }

        @Override
        public void replay(InsetsController controller) {
            controller.show(mTypes);
        }
    }

    private static class HideRequest implements PendingRequest {

        private final @InsetsType int mTypes;

        public HideRequest(int types) {
            mTypes = types;
        }

        @Override
        public void replay(InsetsController controller) {
            controller.hide(mTypes);
        }
    }
}
