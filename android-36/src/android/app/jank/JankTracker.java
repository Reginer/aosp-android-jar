/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.app.jank;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.AttachedSurfaceControl;
import android.view.Choreographer;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewRootImpl;
import android.view.ViewTreeObserver;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is responsible for registering callbacks that will receive JankData batches.
 * It handles managing the background thread that JankData will be processed on. As well as acting
 * as an intermediary between widgets and the state tracker, routing state changes to the tracker.
 *
 * @hide
 */
@FlaggedApi(Flags.FLAG_DETAILED_APP_JANK_METRICS_API)
public class JankTracker {
    private static final boolean DEBUG = false;
    private static final String DEBUG_KEY = "JANKTRACKER";
    // How long to delay the JankData listener registration.
    //TODO b/394956095 see if this can be reduced or eliminated.
    private static final int REGISTRATION_DELAY_MS = 1000;
    // Tracks states reported by widgets.
    private StateTracker mStateTracker;
    // Processes JankData batches and associates frames to widget states.
    private JankDataProcessor mJankDataProcessor;

    // Background thread responsible for processing JankData batches.
    private HandlerThread mHandlerThread = new HandlerThread("AppJankTracker");
    private Handler mHandler = null;

    // Handle to a registered OnJankData listener.
    private SurfaceControl.OnJankDataListenerRegistration mJankDataListenerRegistration;

    // The interface to the windowing system that enables us to register for JankData.
    private AttachedSurfaceControl mSurfaceControl;
    // Name of the activity that is currently tracking Jank metrics.
    private String mActivityName;
    // The apps uid.
    private int mAppUid;
    // View that gives us access to ViewTreeObserver.
    private View mDecorView;

    /**
     * Set by the activity to enable or disable jank tracking. Activities may disable tracking if
     * they are paused or not enable tracking if they are not visible or if the app category is not
     * set.
     */
    private boolean mTrackingEnabled = false;
    /**
     * Set to true once listeners are registered and JankData will start to be received. Both
     * mTrackingEnabled and mListenersRegistered need to be true for JankData to be processed.
     */
    private boolean mListenersRegistered = false;

    @FlaggedApi(com.android.window.flags.Flags.FLAG_JANK_API)
    private final SurfaceControl.OnJankDataListener mJankDataListener =
            new SurfaceControl.OnJankDataListener() {
                @Override
                public void onJankDataAvailable(
                        @androidx.annotation.NonNull List<SurfaceControl.JankData> jankData) {
                    if (mJankDataProcessor == null) return;
                    mJankDataProcessor.processJankData(jankData, mActivityName, mAppUid);
                }
            };

    private final ViewTreeObserver.OnWindowAttachListener mOnWindowAttachListener =
            new ViewTreeObserver.OnWindowAttachListener() {
                @Override
                public void onWindowAttached() {
                    getHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mDecorView.getViewTreeObserver()
                                    .removeOnWindowAttachListener(mOnWindowAttachListener);
                            initializeJankTrackingComponents();
                        }
                    }, REGISTRATION_DELAY_MS);
                }

                // Leave this empty. Only need to know when the DecorView is attached to the Window
                // in order to get a handle to AttachedSurfaceControl. There is no need to tie
                // anything to when the view is detached as all un-registration code is tied to
                // the lifecycle of the enclosing activity.
                @Override
                public void onWindowDetached() {

                }
            };

    // TODO remove this once the viewroot_choreographer bugfix has been rolled out. b/399724640
    public JankTracker(Choreographer choreographer, View decorView) {
        mStateTracker = new StateTracker(choreographer);
        mJankDataProcessor = new JankDataProcessor(mStateTracker);
        mDecorView = decorView;
        mHandlerThread.start();
        registerWindowListeners();
    }

    /**
     * Using this constructor delays the instantiation of the StateTracker and JankDataProcessor
     * until after the OnWindowAttachListener is fired and the instance of Choreographer attached to
     * the ViewRootImpl can be passed to StateTracker. This should ensures the vsync ids we are
     * using to keep track of active states line up with the ids that are being returned by
     * OnJankDataListener.
     */
    public JankTracker(View decorView) {
        mDecorView = decorView;
        mHandlerThread.start();
        registerWindowListeners();
    }

    /**
     * Merges app jank stats reported by components outside the platform to the current pending
     * stats
     */
    public void mergeAppJankStats(AppJankStats appJankStats) {
        if (appJankStats.getUid() != mAppUid) {
            if (DEBUG) {
                Log.d(DEBUG_KEY, "Reported JankStats AppUID does not match AppUID of "
                        + "enclosing activity.");
            }
            return;
        }
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (mJankDataProcessor == null) {
                    return;
                }
                mJankDataProcessor.mergeJankStats(appJankStats, mActivityName);
            }
        });
    }

    public void setActivityName(@NonNull String activityName) {
        mActivityName = activityName;
    }

    public void setAppUid(int uid) {
        mAppUid = uid;
    }

    /**
     * Will add the widget category, id and state as a UI state to associate frames to it.
     *
     * @param widgetCategory preselected general widget category
     * @param widgetId       developer defined widget id if available.
     * @param widgetState    the current active widget state.
     */
    public void addUiState(String widgetCategory, String widgetId, String widgetState) {
        if (!shouldTrack()) return;

        mStateTracker.putState(widgetCategory, widgetId, widgetState);
    }

    /**
     * Will remove the widget category, id and state as a ui state and no longer attribute frames
     * to it.
     *
     * @param widgetCategory preselected general widget category
     * @param widgetId       developer defined widget id if available.
     * @param widgetState    no longer active widget state.
     */
    public void removeUiState(String widgetCategory, String widgetId, String widgetState) {
        if (!shouldTrack()) return;

        mStateTracker.removeState(widgetCategory, widgetId, widgetState);
    }

    /**
     * Call to update a jank state to a different state.
     *
     * @param widgetCategory preselected general widget category.
     * @param widgetId       developer defined widget id if available.
     * @param currentState   current state of the widget.
     * @param nextState      the state the widget will be in.
     */
    public void updateUiState(String widgetCategory, String widgetId, String currentState,
            String nextState) {
        if (!shouldTrack()) return;

        mStateTracker.updateState(widgetCategory, widgetId, currentState, nextState);
    }

    /**
     * Will enable jank tracking, and add the activity as a state to associate frames to.
     */
    public void enableAppJankTracking() {
        // Add the activity as a state, this will ensure we track frames to the activity without the
        // need for a decorated widget to be used.
        addActivityToStateTracking();
        mTrackingEnabled = true;
        registerForJankData();
    }

    /**
     * Will disable jank tracking, and remove the activity as a state to associate frames to.
     */
    public void disableAppJankTracking() {
        mTrackingEnabled = false;
        removeActivityFromStateTracking();
        unregisterForJankData();
    }

    /**
     * Retrieve all pending widget states, this is intended for testing purposes only. If
     * this is called before StateTracker has been created the method will just return without
     * copying any data to the stateDataList parameter.
     *
     * @param stateDataList the ArrayList that will be populated with the pending states.
     */
    @VisibleForTesting
    public void getAllUiStates(@NonNull ArrayList<StateTracker.StateData> stateDataList) {
        if (mStateTracker == null) return;
        mStateTracker.retrieveAllStates(stateDataList);
    }

    /**
     * Retrieve all pending jank stats before they are logged, this is intended for testing
     * purposes only. If this method is called before JankDataProcessor is created it will return
     * an empty HashMap.
     */
    @VisibleForTesting
    public HashMap<String, JankDataProcessor.PendingJankStat> getPendingJankStats() {
        if (mJankDataProcessor == null) {
            return new HashMap<>();
        }
        return mJankDataProcessor.getPendingJankStats();
    }

    /**
     * Only intended to be used by tests, the runnable that registers the listeners may not run
     * in time for tests to pass. This forces them to run immediately.
     */
    @VisibleForTesting
    public void forceListenerRegistration() {
        addActivityToStateTracking();
        mSurfaceControl = mDecorView.getRootSurfaceControl();
        registerJankDataListener();
        mListenersRegistered = true;
    }

    private void unregisterForJankData() {
        if (mJankDataListenerRegistration == null) return;

        if (com.android.window.flags.Flags.jankApi()) {
            mJankDataListenerRegistration.release();
        }
        mJankDataListenerRegistration = null;
        mListenersRegistered = false;
    }

    private void registerForJankData() {
        if (mDecorView == null) return;

        mSurfaceControl = mDecorView.getRootSurfaceControl();

        if (mSurfaceControl == null || mListenersRegistered) return;

        // Wait a short time before registering the listener. During development it was observed
        // that if a listener is registered too quickly after a hot or warm start no data is
        // received b/394956095.
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                registerJankDataListener();
            }
        }, REGISTRATION_DELAY_MS);
    }

    /**
     * Returns whether jank tracking is enabled or not.
     */
    @VisibleForTesting
    public boolean shouldTrack() {
        if (DEBUG) {
            Log.d(DEBUG_KEY, String.format("mTrackingEnabled: %s | mListenersRegistered: %s",
                    mTrackingEnabled, mListenersRegistered));
        }
        return mTrackingEnabled && mListenersRegistered;
    }

    /**
     * Need to know when the decor view gets attached to the window in order to get
     * AttachedSurfaceControl. In order to register a callback for OnJankDataListener
     * AttachedSurfaceControl needs to be created which only happens after onWindowAttached is
     * called. This is why there is a delay in posting the runnable.
     */
    private void registerWindowListeners() {
        if (mDecorView == null) return;
        mDecorView.getViewTreeObserver().addOnWindowAttachListener(mOnWindowAttachListener);
    }

    private void registerJankDataListener() {
        if (mSurfaceControl == null) {
            if (DEBUG) {
                Log.d(DEBUG_KEY, "SurfaceControl is Null");
            }
            return;
        }

        if (com.android.window.flags.Flags.jankApi()) {
            mJankDataListenerRegistration = mSurfaceControl.registerOnJankDataListener(
                    mHandlerThread.getThreadExecutor(), mJankDataListener);

            if (mJankDataListenerRegistration
                    == SurfaceControl.OnJankDataListenerRegistration.NONE) {
                if (DEBUG) {
                    Log.d(DEBUG_KEY, "OnJankDataListenerRegistration is assigned NONE");
                }
                return;
            }
            mListenersRegistered = true;
        }
    }

    private Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(mHandlerThread.getLooper());
        }
        return mHandler;
    }

    private void addActivityToStateTracking() {
        if (mStateTracker == null) return;

        mStateTracker.putState(AppJankStats.WIDGET_CATEGORY_UNSPECIFIED, mActivityName,
                AppJankStats.WIDGET_STATE_UNSPECIFIED);
    }

    private void removeActivityFromStateTracking() {
        if (mStateTracker == null) return;

        mStateTracker.removeState(AppJankStats.WIDGET_CATEGORY_UNSPECIFIED, mActivityName,
                AppJankStats.WIDGET_STATE_UNSPECIFIED);
    }

    private void initializeJankTrackingComponents() {
        ViewRootImpl viewRoot = mDecorView.getViewRootImpl();
        if (viewRoot == null || viewRoot.getChoreographer() == null) {
            return;
        }

        if (mStateTracker == null) {
            mStateTracker = new StateTracker(viewRoot.getChoreographer());
        }

        if (mJankDataProcessor == null) {
            mJankDataProcessor = new JankDataProcessor(mStateTracker);
        }

        addActivityToStateTracking();
        registerForJankData();
    }
}
