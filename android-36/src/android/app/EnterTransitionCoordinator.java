/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.SharedElementCallback.OnSharedElementsReadyListener;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.transition.TransitionManager;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;

import com.android.internal.view.OneShotPreDrawListener;

import java.util.ArrayList;

/**
 * This ActivityTransitionCoordinator is created by the Activity to manage
 * the enter scene and shared element transfer into the Scene, either during
 * launch of an Activity or returning from a launched Activity.
 */
class EnterTransitionCoordinator extends ActivityTransitionCoordinator {
    private static final String TAG = "EnterTransitionCoordinator";

    private static final int MIN_ANIMATION_FRAMES = 2;

    private boolean mSharedElementTransitionStarted;
    private Activity mActivity;
    private boolean mIsTaskRoot;
    private boolean mHasStopped;
    private boolean mIsCanceled;
    private ObjectAnimator mBackgroundAnimator;
    private boolean mIsExitTransitionComplete;
    private boolean mIsReadyForTransition;
    private Bundle mSharedElementsBundle;
    private boolean mWasOpaque;
    private boolean mAreViewsReady;
    private boolean mIsViewsTransitionStarted;
    private Transition mEnterViewsTransition;
    private OneShotPreDrawListener mViewsReadyListener;
    private final boolean mIsCrossTask;
    private Drawable mReplacedBackground;
    private ArrayList<String> mPendingExitNames;
    private Runnable mOnTransitionComplete;

    EnterTransitionCoordinator(Activity activity, ResultReceiver resultReceiver,
            ArrayList<String> sharedElementNames, boolean isReturning, boolean isCrossTask) {
        super(activity.getWindow(), sharedElementNames,
                getListener(activity, isReturning && !isCrossTask), isReturning);
        mActivity = activity;
        mIsCrossTask = isCrossTask;
        setResultReceiver(resultReceiver);
        prepareEnter();
        Bundle resultReceiverBundle = new Bundle();
        resultReceiverBundle.putParcelable(KEY_REMOTE_RECEIVER, this);
        mResultReceiver.send(MSG_SET_REMOTE_RECEIVER, resultReceiverBundle);
        final View decorView = getDecor();
        if (decorView != null) {
            final ViewTreeObserver viewTreeObserver = decorView.getViewTreeObserver();
            viewTreeObserver.addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            if (mIsReadyForTransition) {
                                if (viewTreeObserver.isAlive()) {
                                    viewTreeObserver.removeOnPreDrawListener(this);
                                } else {
                                    decorView.getViewTreeObserver().removeOnPreDrawListener(this);
                                }
                            }
                            return false;
                        }
                    });
        }
    }

    boolean isCrossTask() {
        return mIsCrossTask;
    }

    public void viewInstancesReady(ArrayList<String> accepted, ArrayList<String> localNames,
            ArrayList<View> localViews) {
        boolean remap = false;
        for (int i = 0; i < localViews.size(); i++) {
            View view = localViews.get(i);
            if (!TextUtils.equals(view.getTransitionName(), localNames.get(i))
                    || !view.isAttachedToWindow()) {
                remap = true;
                break;
            }
        }
        if (remap) {
            triggerViewsReady(mapNamedElements(accepted, localNames));
        } else {
            triggerViewsReady(mapSharedElements(accepted, localViews));
        }
    }

    public void namedViewsReady(ArrayList<String> accepted, ArrayList<String> localNames) {
        triggerViewsReady(mapNamedElements(accepted, localNames));
    }

    public Transition getEnterViewsTransition() {
        return mEnterViewsTransition;
    }

    @Override
    protected void viewsReady(ArrayMap<String, View> sharedElements) {
        super.viewsReady(sharedElements);
        mIsReadyForTransition = true;
        hideViews(mSharedElements);
        Transition viewsTransition = getViewsTransition();
        if (viewsTransition != null && mTransitioningViews != null) {
            removeExcludedViews(viewsTransition, mTransitioningViews);
            stripOffscreenViews();
            hideViews(mTransitioningViews);
        }
        if (mIsReturning) {
            sendSharedElementDestination();
        } else {
            moveSharedElementsToOverlay();
        }
        if (mSharedElementsBundle != null) {
            onTakeSharedElements();
        }
    }

    private void triggerViewsReady(final ArrayMap<String, View> sharedElements) {
        if (mAreViewsReady) {
            return;
        }
        mAreViewsReady = true;
        final ViewGroup decor = getDecor();
        // Ensure the views have been laid out before capturing the views -- we need the epicenter.
        if (decor == null || (decor.isAttachedToWindow() &&
                (sharedElements.isEmpty() || !sharedElements.valueAt(0).isLayoutRequested()))) {
            viewsReady(sharedElements);
        } else {
            mViewsReadyListener = OneShotPreDrawListener.add(decor, () -> {
                mViewsReadyListener = null;
                viewsReady(sharedElements);
            });
            decor.invalidate();
        }
    }

    private ArrayMap<String, View> mapNamedElements(ArrayList<String> accepted,
            ArrayList<String> localNames) {
        ArrayMap<String, View> sharedElements = new ArrayMap<String, View>();
        ViewGroup decorView = getDecor();
        if (decorView != null) {
            decorView.findNamedViews(sharedElements);
        }
        if (accepted != null) {
            for (int i = 0; i < localNames.size(); i++) {
                String localName = localNames.get(i);
                String acceptedName = accepted.get(i);
                if (localName != null && !localName.equals(acceptedName)) {
                    View view = sharedElements.get(localName);
                    if (view != null) {
                        sharedElements.put(acceptedName, view);
                    }
                }
            }
        }
        return sharedElements;
    }

    private void sendSharedElementDestination() {
        boolean allReady;
        final View decorView = getDecor();
        if (allowOverlappingTransitions() && getEnterViewsTransition() != null) {
            allReady = false;
        } else if (decorView == null) {
            allReady = true;
        } else {
            allReady = !decorView.isLayoutRequested();
            if (allReady) {
                for (int i = 0; i < mSharedElements.size(); i++) {
                    if (mSharedElements.get(i).isLayoutRequested()) {
                        allReady = false;
                        break;
                    }
                }
            }
        }
        if (allReady) {
            Bundle state = captureSharedElementState();
            moveSharedElementsToOverlay();
            mResultReceiver.send(MSG_SHARED_ELEMENT_DESTINATION, state);
        } else if (decorView != null) {
            OneShotPreDrawListener.add(decorView, () -> {
                if (mResultReceiver != null) {
                    Bundle state = captureSharedElementState();
                    moveSharedElementsToOverlay();
                    mResultReceiver.send(MSG_SHARED_ELEMENT_DESTINATION, state);
                }
            });
        }
        if (allowOverlappingTransitions()) {
            startEnterTransitionOnly();
        }
    }

    private static SharedElementCallback getListener(Activity activity, boolean isReturning) {
        return isReturning ? activity.mExitTransitionListener : activity.mEnterTransitionListener;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case MSG_TAKE_SHARED_ELEMENTS:
                if (!mIsCanceled) {
                    mSharedElementsBundle = resultData;
                    onTakeSharedElements();
                }
                break;
            case MSG_EXIT_TRANSITION_COMPLETE:
                if (!mIsCanceled) {
                    mIsExitTransitionComplete = true;
                    if (mSharedElementTransitionStarted) {
                        onRemoteExitTransitionComplete();
                    }
                }
                break;
            case MSG_CANCEL:
                cancel();
                break;
            case MSG_ALLOW_RETURN_TRANSITION:
                if (!mIsCanceled && !mIsTaskRoot) {
                    mPendingExitNames = mAllSharedElementNames;
                }
                break;
        }
    }

    public boolean isWaitingForRemoteExit() {
        return mIsReturning && mResultReceiver != null;
    }

    public ArrayList<String> getPendingExitSharedElementNames() {
        return mPendingExitNames;
    }

    /**
     * This is called onResume. If an Activity is resuming and the transitions
     * haven't started yet, force the views to appear. This is likely to be
     * caused by the top Activity finishing before the transitions started.
     * In that case, we can finish any transition that was started, but we
     * should cancel any pending transition and just bring those Views visible.
     */
    public void forceViewsToAppear() {
        if (!mIsReturning) {
            return;
        }
        if (!mIsReadyForTransition) {
            mIsReadyForTransition = true;
            final ViewGroup decor = getDecor();
            if (decor != null && mViewsReadyListener != null) {
                mViewsReadyListener.removeListener();
                mViewsReadyListener = null;
            }
            showViews(mTransitioningViews, true);
            setTransitioningViewsVisiblity(View.VISIBLE, true);
            mSharedElements.clear();
            mAllSharedElementNames.clear();
            mTransitioningViews.clear();
            mIsReadyForTransition = true;
            viewsTransitionComplete();
            sharedElementTransitionComplete();
        } else {
            if (!mSharedElementTransitionStarted) {
                moveSharedElementsFromOverlay();
                mSharedElementTransitionStarted = true;
                showViews(mSharedElements, true);
                mSharedElements.clear();
                sharedElementTransitionComplete();
            }
            if (!mIsViewsTransitionStarted) {
                mIsViewsTransitionStarted = true;
                showViews(mTransitioningViews, true);
                setTransitioningViewsVisiblity(View.VISIBLE, true);
                mTransitioningViews.clear();
                viewsTransitionComplete();
            }
            cancelPendingTransitions();
        }
        mAreViewsReady = true;
        if (mResultReceiver != null) {
            mResultReceiver.send(MSG_CANCEL, null);
            mResultReceiver = null;
        }
    }

    private void cancel() {
        if (!mIsCanceled) {
            mIsCanceled = true;
            if (getViewsTransition() == null || mIsViewsTransitionStarted) {
                showViews(mSharedElements, true);
            } else if (mTransitioningViews != null) {
                mTransitioningViews.addAll(mSharedElements);
            }
            moveSharedElementsFromOverlay();
            mSharedElementNames.clear();
            mSharedElements.clear();
            mAllSharedElementNames.clear();
            startSharedElementTransition(null);
            onRemoteExitTransitionComplete();
        }
    }

    public boolean isReturning() {
        return mIsReturning;
    }

    protected void prepareEnter() {
        ViewGroup decorView = getDecor();
        if (mActivity == null || decorView == null) {
            return;
        }

        mIsTaskRoot = mActivity.isTaskRoot();

        if (!isCrossTask()) {
            mActivity.overridePendingTransition(0, 0);
        }
        if (!mIsReturning) {
            mWasOpaque = mActivity.convertToTranslucent(null, null);
            Drawable background = decorView.getBackground();
            if (background == null) {
                background = new ColorDrawable(Color.TRANSPARENT);
                mReplacedBackground = background;
            } else {
                getWindow().setBackgroundDrawable(null);
                background = background.mutate();
                background.setAlpha(0);
            }
            getWindow().setBackgroundDrawable(background);
        } else {
            mActivity = null; // all done with it now.
        }
    }

    @Override
    protected Transition getViewsTransition() {
        Window window = getWindow();
        if (window == null) {
            return null;
        }
        if (mIsReturning) {
            return window.getReenterTransition();
        } else {
            return window.getEnterTransition();
        }
    }

    protected Transition getSharedElementTransition() {
        Window window = getWindow();
        if (window == null) {
            return null;
        }
        if (mIsReturning) {
            return window.getSharedElementReenterTransition();
        } else {
            return window.getSharedElementEnterTransition();
        }
    }

    private void startSharedElementTransition(Bundle sharedElementState) {
        ViewGroup decorView = getDecor();
        if (decorView == null) {
            return;
        }
        // Remove rejected shared elements
        ArrayList<String> rejectedNames = new ArrayList<String>(mAllSharedElementNames);
        rejectedNames.removeAll(mSharedElementNames);
        ArrayList<View> rejectedSnapshots = createSnapshots(sharedElementState, rejectedNames);
        if (mListener != null) {
            mListener.onRejectSharedElements(rejectedSnapshots);
        }
        removeNullViews(rejectedSnapshots);
        startRejectedAnimations(rejectedSnapshots);

        // Now start shared element transition
        ArrayList<View> sharedElementSnapshots = createSnapshots(sharedElementState,
                mSharedElementNames);
        showViews(mSharedElements, true);
        scheduleSetSharedElementEnd(sharedElementSnapshots);
        ArrayList<SharedElementOriginalState> originalImageViewState =
                setSharedElementState(sharedElementState, sharedElementSnapshots);
        requestLayoutForSharedElements();

        boolean startEnterTransition = allowOverlappingTransitions() && !mIsReturning;
        boolean startSharedElementTransition = true;
        setGhostVisibility(View.INVISIBLE);
        scheduleGhostVisibilityChange(View.INVISIBLE);
        pauseInput();
        Transition transition = beginTransition(decorView, startEnterTransition,
                startSharedElementTransition);
        scheduleGhostVisibilityChange(View.VISIBLE);
        setGhostVisibility(View.VISIBLE);

        if (startEnterTransition) {
            startEnterTransition(transition);
        }

        setOriginalSharedElementState(mSharedElements, originalImageViewState);

        if (mResultReceiver != null) {
            // We can't trust that the view will disappear on the same frame that the shared
            // element appears here. Assure that we get at least 2 frames for double-buffering.
            decorView.postOnAnimation(new Runnable() {
                int mAnimations;

                @Override
                public void run() {
                    if (mAnimations++ < MIN_ANIMATION_FRAMES) {
                        View decorView = getDecor();
                        if (decorView != null) {
                            decorView.postOnAnimation(this);
                        }
                    } else if (mResultReceiver != null) {
                        mResultReceiver.send(MSG_HIDE_SHARED_ELEMENTS, null);
                        mResultReceiver = null; // all done sending messages.
                    }
                }
            });
        }
    }

    private static void removeNullViews(ArrayList<View> views) {
        if (views != null) {
            for (int i = views.size() - 1; i >= 0; i--) {
                if (views.get(i) == null) {
                    views.remove(i);
                }
            }
        }
    }

    private void onTakeSharedElements() {
        if (!mIsReadyForTransition || mSharedElementsBundle == null) {
            return;
        }
        final Bundle sharedElementState = mSharedElementsBundle;
        mSharedElementsBundle = null;
        OnSharedElementsReadyListener listener = new OnSharedElementsReadyListener() {
            @Override
            public void onSharedElementsReady() {
                final View decorView = getDecor();
                if (decorView != null) {
                    OneShotPreDrawListener.add(decorView, false, () -> {
                        startTransition(() -> {
                                startSharedElementTransition(sharedElementState);
                        });
                    });
                    decorView.invalidate();
                }
            }
        };
        if (mListener == null) {
            listener.onSharedElementsReady();
        } else {
            mListener.onSharedElementsArrived(mSharedElementNames, mSharedElements, listener);
        }
    }

    private void requestLayoutForSharedElements() {
        int numSharedElements = mSharedElements.size();
        for (int i = 0; i < numSharedElements; i++) {
            mSharedElements.get(i).requestLayout();
        }
    }

    private Transition beginTransition(ViewGroup decorView, boolean startEnterTransition,
            boolean startSharedElementTransition) {
        Transition sharedElementTransition = null;
        if (startSharedElementTransition) {
            if (!mSharedElementNames.isEmpty()) {
                sharedElementTransition = configureTransition(getSharedElementTransition(), false);
            }
            if (sharedElementTransition == null) {
                sharedElementTransitionStarted();
                sharedElementTransitionComplete();
            } else {
                sharedElementTransition.addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionStart(Transition transition) {
                        sharedElementTransitionStarted();
                    }

                    @Override
                    public void onTransitionEnd(Transition transition) {
                        transition.removeListener(this);
                        sharedElementTransitionComplete();
                    }
                });
            }
        }
        Transition viewsTransition = null;
        if (startEnterTransition) {
            mIsViewsTransitionStarted = true;
            if (mTransitioningViews != null && !mTransitioningViews.isEmpty()) {
                viewsTransition = configureTransition(getViewsTransition(), true);
            }
            if (viewsTransition == null) {
                viewsTransitionComplete();
            } else {
                final ArrayList<View> transitioningViews = mTransitioningViews;
                viewsTransition.addListener(new ContinueTransitionListener() {
                    @Override
                    public void onTransitionStart(Transition transition) {
                        mEnterViewsTransition = transition;
                        if (transitioningViews != null) {
                            showViews(transitioningViews, false);
                        }
                        super.onTransitionStart(transition);
                    }

                    @Override
                    public void onTransitionEnd(Transition transition) {
                        mEnterViewsTransition = null;
                        transition.removeListener(this);
                        viewsTransitionComplete();
                        super.onTransitionEnd(transition);
                    }
                });
            }
        }

        Transition transition = mergeTransitions(sharedElementTransition, viewsTransition);
        if (transition != null) {
            transition.addListener(new ContinueTransitionListener());
            if (startEnterTransition) {
                setTransitioningViewsVisiblity(View.INVISIBLE, false);
            }
            TransitionManager.beginDelayedTransition(decorView, transition);
            if (startEnterTransition) {
                setTransitioningViewsVisiblity(View.VISIBLE, false);
            }
            decorView.invalidate();
        } else {
            transitionStarted();
        }
        return transition;
    }

    public void runAfterTransitionsComplete(Runnable onTransitionComplete) {
        if (!isTransitionRunning()) {
            onTransitionsComplete();
        } else {
            mOnTransitionComplete = onTransitionComplete;
        }
    }

    @Override
    protected void onTransitionsComplete() {
        moveSharedElementsFromOverlay();
        final ViewGroup decorView = getDecor();
        if (decorView != null) {
            decorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);

            Window window = getWindow();
            if (window != null && mReplacedBackground == decorView.getBackground()) {
                window.setBackgroundDrawable(null);
            }
        }
        if (mOnTransitionComplete != null) {
            mOnTransitionComplete.run();
            mOnTransitionComplete = null;
        }
    }

    private void sharedElementTransitionStarted() {
        mSharedElementTransitionStarted = true;
        if (mIsExitTransitionComplete) {
            send(MSG_EXIT_TRANSITION_COMPLETE, null);
        }
    }

    private void startEnterTransition(Transition transition) {
        ViewGroup decorView = getDecor();
        if (!mIsReturning && decorView != null) {
            Drawable background = decorView.getBackground();
            if (background != null) {
                background = background.mutate();
                getWindow().setBackgroundDrawable(background);
                mBackgroundAnimator = ObjectAnimator.ofInt(background, "alpha", 255);
                mBackgroundAnimator.setDuration(getFadeDuration());
                mBackgroundAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        makeOpaque();
                        backgroundAnimatorComplete();
                    }
                });
                mBackgroundAnimator.start();
            } else if (transition != null) {
                transition.addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionEnd(Transition transition) {
                        transition.removeListener(this);
                        makeOpaque();
                    }
                });
                backgroundAnimatorComplete();
            } else {
                makeOpaque();
                backgroundAnimatorComplete();
            }
        } else {
            backgroundAnimatorComplete();
        }
    }

    public void stop() {
        // Restore the background to its previous state since the
        // Activity is stopping.
        if (mBackgroundAnimator != null) {
            mBackgroundAnimator.end();
            mBackgroundAnimator = null;
        } else if (mWasOpaque) {
            ViewGroup decorView = getDecor();
            if (decorView != null) {
                Drawable drawable = decorView.getBackground();
                if (drawable != null) {
                    drawable.setAlpha(255);
                }
            }
        }
        makeOpaque();
        mIsCanceled = true;
        mResultReceiver = null;
        mActivity = null;
        moveSharedElementsFromOverlay();
        if (mTransitioningViews != null) {
            showViews(mTransitioningViews, true);
            setTransitioningViewsVisiblity(View.VISIBLE, true);
        }
        showViews(mSharedElements, true);
        clearState();
    }

    /**
     * Cancels the enter transition.
     * @return True if the enter transition is still pending capturing the target state. If so,
     * any transition started on the decor will do nothing.
     */
    public boolean cancelEnter() {
        setGhostVisibility(View.INVISIBLE);
        mHasStopped = true;
        mIsCanceled = true;
        clearState();
        return super.cancelPendingTransitions();
    }

    @Override
    protected void clearState() {
        mSharedElementsBundle = null;
        mEnterViewsTransition = null;
        mResultReceiver = null;
        if (mBackgroundAnimator != null) {
            mBackgroundAnimator.cancel();
            mBackgroundAnimator = null;
        }
        if (mOnTransitionComplete != null) {
            mOnTransitionComplete.run();
            mOnTransitionComplete = null;
        }
        super.clearState();
    }

    private void makeOpaque() {
        if (!mHasStopped && mActivity != null) {
            if (mWasOpaque) {
                mActivity.convertFromTranslucent();
            }
            mActivity = null;
        }
    }

    private boolean allowOverlappingTransitions() {
        final Window window = getWindow();
        if (window == null) {
            return false;
        }
        return mIsReturning ? window.getAllowReturnTransitionOverlap()
                : window.getAllowEnterTransitionOverlap();
    }

    private void startRejectedAnimations(final ArrayList<View> rejectedSnapshots) {
        if (rejectedSnapshots == null || rejectedSnapshots.isEmpty()) {
            return;
        }
        final ViewGroup decorView = getDecor();
        if (decorView != null) {
            ViewGroupOverlay overlay = decorView.getOverlay();
            ObjectAnimator animator = null;
            int numRejected = rejectedSnapshots.size();
            for (int i = 0; i < numRejected; i++) {
                View snapshot = rejectedSnapshots.get(i);
                overlay.add(snapshot);
                animator = ObjectAnimator.ofFloat(snapshot, View.ALPHA, 1, 0);
                animator.start();
            }
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ViewGroupOverlay overlay = decorView.getOverlay();
                    int numRejected = rejectedSnapshots.size();
                    for (int i = 0; i < numRejected; i++) {
                        overlay.remove(rejectedSnapshots.get(i));
                    }
                }
            });
        }
    }

    protected void onRemoteExitTransitionComplete() {
        if (!allowOverlappingTransitions()) {
            startEnterTransitionOnly();
        }
    }

    private void startEnterTransitionOnly() {
        startTransition(new Runnable() {
            @Override
            public void run() {
                boolean startEnterTransition = true;
                boolean startSharedElementTransition = false;
                ViewGroup decorView = getDecor();
                if (decorView != null) {
                    Transition transition = beginTransition(decorView, startEnterTransition,
                            startSharedElementTransition);
                    startEnterTransition(transition);
                }
            }
        });
    }
}
