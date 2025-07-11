/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.server.wm;

import static android.content.ClipDescription.EXTRA_HIDE_DRAG_SOURCE_TASK_ID;
import static android.content.ClipDescription.MIMETYPE_APPLICATION_ACTIVITY;
import static android.content.ClipDescription.MIMETYPE_APPLICATION_SHORTCUT;
import static android.content.ClipDescription.MIMETYPE_APPLICATION_TASK;
import static android.os.InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
import static android.os.Trace.TRACE_TAG_WINDOW_MANAGER;
import static android.view.WindowManager.LayoutParams.PRIVATE_FLAG_INTERCEPT_GLOBAL_DRAG_AND_DROP;

import static com.android.internal.protolog.WmProtoLogGroups.WM_DEBUG_ORIENTATION;
import static com.android.internal.protolog.WmProtoLogGroups.WM_SHOW_TRANSACTIONS;
import static com.android.server.wm.DragDropController.MSG_ANIMATION_END;
import static com.android.server.wm.DragDropController.MSG_DRAG_END_TIMEOUT;
import static com.android.server.wm.DragDropController.MSG_REMOVE_DRAG_SURFACE_TIMEOUT;
import static com.android.server.wm.DragDropController.MSG_TEAR_DOWN_DRAG_AND_DROP_INPUT;
import static com.android.server.wm.WindowManagerDebugConfig.DEBUG_DRAG;
import static com.android.server.wm.WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS;
import static com.android.server.wm.WindowManagerDebugConfig.TAG_WM;
import static com.android.server.wm.WindowManagerService.MY_PID;
import static com.android.server.wm.WindowManagerService.MY_UID;

import static java.util.concurrent.CompletableFuture.completedFuture;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.Nullable;
import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Slog;
import android.view.Display;
import android.view.DragEvent;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
import android.view.InputWindowHandle;
import android.view.SurfaceControl;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.android.internal.protolog.ProtoLog;
import com.android.internal.view.IDragAndDropPermissions;
import com.android.server.LocalServices;
import com.android.server.pm.UserManagerInternal;
import com.android.window.flags.Flags;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Drag/drop state
 */
class DragState {
    private static final long MIN_ANIMATION_DURATION_MS = 195;
    private static final long MAX_ANIMATION_DURATION_MS = 375;
    private static final float DIFFERENT_DISPLAY_RETURN_ANIMATION_SCALE = 0.75f;

    private static final int DRAG_FLAGS_URI_ACCESS = View.DRAG_FLAG_GLOBAL_URI_READ |
            View.DRAG_FLAG_GLOBAL_URI_WRITE;

    private static final int DRAG_FLAGS_URI_PERMISSIONS = DRAG_FLAGS_URI_ACCESS |
            View.DRAG_FLAG_GLOBAL_PERSISTABLE_URI_PERMISSION |
            View.DRAG_FLAG_GLOBAL_PREFIX_URI_PERMISSION;

    // Property names for animations
    private static final String ANIMATED_PROPERTY_X = "x";
    private static final String ANIMATED_PROPERTY_Y = "y";
    private static final String ANIMATED_PROPERTY_ALPHA = "alpha";
    private static final String ANIMATED_PROPERTY_SCALE = "scale";

    final WindowManagerService mService;
    final DragDropController mDragDropController;
    IBinder mToken;
    /**
     * Do not use the variable from the out of animation thread while mAnimator is not null.
     */
    SurfaceControl mSurfaceControl;
    int mFlags;
    IBinder mLocalWin;
    int mPid;
    int mUid;
    int mSourceUserId;
    boolean mCrossProfileCopyAllowed;
    ClipData mData;
    ClipDescription mDataDescription;
    boolean mDragResult;
    boolean mRelinquishDragSurfaceToDropTarget;
    float mAnimatedScale = 1.0f;
    float mStartDragAlpha;
    // Coords are in display coordinates space.
    float mStartDragDisplayX, mStartDragDisplayY;
    float mCurrentDisplayX, mCurrentDisplayY;
    float mThumbOffsetX, mThumbOffsetY;
    InputInterceptor mInputInterceptor;
    ArrayList<WindowState> mNotifiedWindows;
    private boolean mDragInProgress;
    // Set to non -1 value if a valid app requests DRAG_FLAG_HIDE_CALLING_TASK_ON_DRAG_START
    int mCallingTaskIdToHide;
    /**
     * Whether if animation is completed. Needs to be volatile to update from the animation thread
     * without having a WM lock.
     */
    volatile boolean mAnimationCompleted = false;
    /**
     * The display on which the drag originally started. Note that it's possible for either/both
     * mStartDragDisplayContent and mCurrentDisplayContent to be invalid if DisplayTopology was
     * changed or removed in the middle of the drag. In this case, drag will also be cancelled as
     * soon as listener is notified.
     */
    DisplayContent mStartDragDisplayContent;
    /**
     * The display on which the drag is happening. If it goes into a different display this will
     * be updated.
     */
    DisplayContent mCurrentDisplayContent;

    @Nullable private ValueAnimator mAnimator;
    private final Interpolator mCubicEaseOutInterpolator = new DecelerateInterpolator(1.5f);
    private final Point mDisplaySize = new Point();

    // A surface used to catch input events for the drag-and-drop operation.
    SurfaceControl mInputSurface;

    final SurfaceControl.Transaction mTransaction;

    private final Rect mTmpClipRect = new Rect();

    /**
     * Whether we are finishing this drag and drop. This starts with {@code false}, and is set to
     * {@code true} when {@link #closeLocked()} is called.
     */
    private boolean mIsClosing;

    // Stores the last drop event which was reported to a valid drop target window, or null
    // otherwise. This drop event will contain private info and should only be consumed by the
    // unhandled drag listener.
    DragEvent mUnhandledDropEvent;

    DragState(WindowManagerService service, DragDropController controller, IBinder token,
            SurfaceControl surface, int flags, IBinder localWin) {
        mService = service;
        mDragDropController = controller;
        mToken = token;
        mSurfaceControl = surface;
        mFlags = flags;
        mLocalWin = localWin;
        mNotifiedWindows = new ArrayList<>();
        mTransaction = service.mTransactionFactory.get();
    }

    boolean isClosing() {
        return mIsClosing;
    }

    /**
     * @return a future that completes after window info is sent.
     */
    private CompletableFuture<Void> showInputSurface() {
        if (mInputSurface == null) {
            mInputSurface = mService.makeSurfaceBuilder()
                    .setContainerLayer()
                    .setName("Drag and Drop Input Consumer")
                    .setCallsite("DragState.showInputSurface")
                    .setParent(mCurrentDisplayContent.getOverlayLayer())
                    .build();
        }
        final InputWindowHandle h = getInputWindowHandle();
        if (h == null) {
            Slog.w(TAG_WM, "Drag is in progress but there is no "
                    + "drag window handle.");
            return completedFuture(null);
        }

        // Crop the input surface to the display size.
        mTmpClipRect.set(0, 0, mDisplaySize.x, mDisplaySize.y);

        // Make trusted overlay to not block any touches while D&D ongoing and allowing
        // touches to pass through to windows underneath. This allows user to interact with the
        // UI to navigate while dragging.
        h.setTrustedOverlay(mTransaction, mInputSurface, true);
        mTransaction.show(mInputSurface)
                .setInputWindowInfo(mInputSurface, h)
                .setLayer(mInputSurface, Integer.MAX_VALUE)
                .setCrop(mInputSurface, mTmpClipRect);

        // A completableFuture is returned to ensure that input window info is sent before the
        // transferTouchFocus is called.
        CompletableFuture<Void> result = new CompletableFuture<>();
        mTransaction
            .addWindowInfosReportedListener(() -> result.complete(null))
            .apply();
        return result;
    }

    /**
     * After calling this, DragDropController#onDragStateClosedLocked is invoked, which causes
     * DragDropController#mDragState becomes null.
     */
    void closeLocked() {
        mIsClosing = true;
        // Unregister the input interceptor.
        if (mInputInterceptor != null) {
            if (DEBUG_DRAG) Slog.d(TAG_WM, "Unregistering drag input channel");

            // Input channel should be disposed on the thread where the input is being handled.
            mDragDropController.sendHandlerMessage(
                    MSG_TEAR_DOWN_DRAG_AND_DROP_INPUT, mInputInterceptor);
            mInputInterceptor = null;
        }

        // Send drag end broadcast if drag start has been sent.
        if (mDragInProgress) {
            if (DEBUG_DRAG) Slog.d(TAG_WM, "Broadcasting DRAG_ENDED");
            for (WindowState ws : mNotifiedWindows) {
                float inWindowX = 0;
                float inWindowY = 0;
                boolean includeDragSurface = false;
                if (!mDragResult && (ws.mSession.mPid == mPid)) {
                    // Report unconsumed drop location back to the app that started the drag.
                    inWindowX = ws.translateToWindowX(mCurrentDisplayX);
                    inWindowY = ws.translateToWindowY(mCurrentDisplayY);
                    if (relinquishDragSurfaceToDragSource()) {
                        // If requested (and allowed), report the drag surface back to the app
                        // starting the drag to handle the return animation
                        includeDragSurface = true;
                    }
                }
                DragEvent event = obtainDragEndedEvent(inWindowX, inWindowY, includeDragSurface);
                try {
                    if (DEBUG_DRAG) Slog.d(TAG_WM, "Sending DRAG_ENDED to " + ws);
                    ws.mClient.dispatchDragEvent(event);
                } catch (RemoteException e) {
                    Slog.w(TAG_WM, "Unable to drag-end window " + ws);
                }
                // if the current window is in the same process,
                // the dispatch has already recycled the event
                if (MY_PID != ws.mSession.mPid) {
                    event.recycle();
                }
            }
            mNotifiedWindows.clear();
            mDragInProgress = false;
            Trace.instant(TRACE_TAG_WINDOW_MANAGER, "DragDropController#DRAG_ENDED");
        }

        // Clear the internal variables.
        if (mInputSurface != null) {
            mTransaction.remove(mInputSurface).apply();
            mInputSurface = null;
        }
        if (mSurfaceControl != null) {
            if (!mRelinquishDragSurfaceToDropTarget && !relinquishDragSurfaceToDragSource()) {
                mTransaction.remove(mSurfaceControl).apply();
            } else {
                mDragDropController.sendTimeoutMessage(MSG_REMOVE_DRAG_SURFACE_TIMEOUT,
                        mSurfaceControl, DragDropController.DRAG_TIMEOUT_MS);
            }
            mSurfaceControl = null;
        }
        if (mAnimator != null && !mAnimationCompleted) {
            Slog.wtf(TAG_WM,
                    "Unexpectedly destroying mSurfaceControl while animation is running");
        }
        mFlags = 0;
        mLocalWin = null;
        mToken = null;
        mData = null;
        mThumbOffsetX = mThumbOffsetY = 0;
        mNotifiedWindows = null;
        if (mUnhandledDropEvent != null) {
            mUnhandledDropEvent.recycle();
            mUnhandledDropEvent = null;
        }

        // Notifies the controller that the drag state is closed.
        mDragDropController.onDragStateClosedLocked(this);
    }

    /**
     * Creates the drop event for dispatching to the unhandled drag.
     */
    private DragEvent createUnhandledDropEvent(float inDisplayX, float inDisplayY) {
        return obtainDragEvent(DragEvent.ACTION_DROP, inDisplayX, inDisplayY, mDataDescription,
                mData,
                /* includeDragSurface= */ true,
                /* includeDragFlags= */ true, null /* dragAndDropPermissions */);
    }

    /**
     * Creates the drop event for this drag gesture.
     */
    private DragEvent createDropEvent(float inWindowX, float inWindowY, WindowState touchedWin) {
        final int targetUserId = UserHandle.getUserId(touchedWin.getOwningUid());
        final DragAndDropPermissionsHandler dragAndDropPermissions;
        if ((mFlags & View.DRAG_FLAG_GLOBAL) != 0 && (mFlags & DRAG_FLAGS_URI_ACCESS) != 0
                && mData != null) {
            dragAndDropPermissions = new DragAndDropPermissionsHandler(mService.mGlobalLock, mData,
                    mUid, touchedWin.getOwningPackage(), mFlags & DRAG_FLAGS_URI_PERMISSIONS,
                    mSourceUserId, targetUserId);
        } else {
            dragAndDropPermissions = null;
        }
        if (mSourceUserId != targetUserId) {
            if (mData != null) {
                mData.fixUris(mSourceUserId);
            }
        }
        final boolean targetInterceptsGlobalDrag = targetInterceptsGlobalDrag(touchedWin);
        return obtainDragEvent(DragEvent.ACTION_DROP, inWindowX, inWindowY, mDataDescription, mData,
                /* includeDragSurface= */ targetInterceptsGlobalDrag,
                /* includeDragFlags= */ targetInterceptsGlobalDrag, dragAndDropPermissions);
    }

    /**
     * Notify the drop target and tells it about the data. If the drop event is not sent to the
     * target, invokes {@code endDragLocked} after the unhandled drag listener gets a chance to
     * handle the drop.
     * @param inWindowX if `token` refers to a dragEvent-accepting window, `inWindowX` will be
     *                  inside the window, else values might be invalid (0, 0).
     * @param inWindowY if `token` refers to a dragEvent-accepting window, `inWindowY` will be
     *                  inside the window, else values might be invalid (0, 0).
     */
    boolean reportDropWindowLock(IBinder token, float inWindowX, float inWindowY) {
        if (mAnimator != null) {
            return false;
        }
        try {
            Trace.traceBegin(TRACE_TAG_WINDOW_MANAGER, "DragDropController#DROP");
            return reportDropWindowLockInner(token, inWindowX, inWindowY);
        } finally {
            Trace.traceEnd(TRACE_TAG_WINDOW_MANAGER);
        }
    }

    private boolean reportDropWindowLockInner(IBinder token, float inWindowX, float inWindowY) {
        if (mAnimator != null) {
            return false;
        }

        final WindowState touchedWin = mService.mInputToWindowMap.get(token);
        if (!isWindowNotified(touchedWin)) {
            final DragEvent unhandledDropEvent = createUnhandledDropEvent(inWindowX, inWindowY);
            // Delegate to the unhandled drag listener as a first pass
            if (mDragDropController.notifyUnhandledDrop(unhandledDropEvent, "unhandled-drop")) {
                // The unhandled drag listener will call back to notify whether it has consumed
                // the drag, so return here
                return true;
            }

            Trace.traceBegin(TRACE_TAG_WINDOW_MANAGER, "DragDropController#noWindow");
            // "drop" outside a valid window -- no recipient to apply a timeout to, and we can send
            // the drag-ended message immediately.
            endDragLocked(false /* consumed */, false /* relinquishDragSurfaceToDropTarget */);
            if (DEBUG_DRAG) Slog.d(TAG_WM, "Drop outside a valid window " + touchedWin);
            Trace.traceEnd(TRACE_TAG_WINDOW_MANAGER);
            return false;
        }

        if (DEBUG_DRAG) Slog.d(TAG_WM, "Sending DROP to " + touchedWin);
        final DragEvent unhandledDropEvent = createUnhandledDropEvent(
                touchedWin.getBounds().left + inWindowX, touchedWin.getBounds().top + inWindowY);

        final IBinder clientToken = touchedWin.mClient.asBinder();
        final DragEvent event = createDropEvent(inWindowX, inWindowY, touchedWin);
        try {
            Trace.traceBegin(TRACE_TAG_WINDOW_MANAGER, "DragDropController#dispatchDrop");
            touchedWin.mClient.dispatchDragEvent(event);

            // 5 second timeout for this window to respond to the drop
            mDragDropController.sendTimeoutMessage(MSG_DRAG_END_TIMEOUT, clientToken,
                    DragDropController.DRAG_TIMEOUT_MS);
        } catch (RemoteException e) {
            Slog.w(TAG_WM, "can't send drop notification to win " + touchedWin);
            endDragLocked(false /* consumed */, false /* relinquishDragSurfaceToDropTarget */);
            return false;
        } finally {
            if (MY_PID != touchedWin.mSession.mPid) {
                event.recycle();
            }
            Trace.traceEnd(TRACE_TAG_WINDOW_MANAGER);
        }
        mToken = clientToken;
        mUnhandledDropEvent = unhandledDropEvent;
        return true;
    }

    class InputInterceptor {
        InputChannel mClientChannel;
        DragInputEventReceiver mInputEventReceiver;
        InputApplicationHandle mDragApplicationHandle;
        InputWindowHandle mDragWindowHandle;

        InputInterceptor(Display display) {
            mClientChannel = mService.mInputManager.createInputChannel("drag");
            mInputEventReceiver = new DragInputEventReceiver(mClientChannel,
                    mService.mH.getLooper(), mDragDropController);

            mDragApplicationHandle = new InputApplicationHandle(new Binder(), "drag",
                    DEFAULT_DISPATCHING_TIMEOUT_MILLIS);

            mDragWindowHandle = new InputWindowHandle(mDragApplicationHandle,
                    display.getDisplayId());
            mDragWindowHandle.name = "drag";
            mDragWindowHandle.token = mClientChannel.getToken();
            mDragWindowHandle.layoutParamsType = WindowManager.LayoutParams.TYPE_DRAG;
            mDragWindowHandle.dispatchingTimeoutMillis = DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
            mDragWindowHandle.ownerPid = MY_PID;
            mDragWindowHandle.ownerUid = MY_UID;
            mDragWindowHandle.scaleFactor = 1.0f;

            // The drag window cannot receive new touches.
            mDragWindowHandle.touchableRegion.setEmpty();

            // Pause rotations before a drag.
            ProtoLog.d(WM_DEBUG_ORIENTATION, "Pausing rotation during drag");
            mService.mRoot.forAllDisplays(dc -> {
                dc.getDisplayRotation().pause();
            });
        }

        void tearDown() {
            mService.mInputManager.removeInputChannel(mClientChannel.getToken());
            mInputEventReceiver.dispose();
            mInputEventReceiver = null;
            mClientChannel.dispose();
            mClientChannel = null;

            mDragWindowHandle = null;
            mDragApplicationHandle = null;

            // Resume rotations after a drag.
            ProtoLog.d(WM_DEBUG_ORIENTATION, "Resuming rotation after drag");
            mService.mRoot.forAllDisplays(dc -> {
                dc.getDisplayRotation().resume();
            });
        }
    }

    InputWindowHandle getInputWindowHandle() {
        return mInputInterceptor == null ? null : mInputInterceptor.mDragWindowHandle;
    }

    IBinder getInputToken() {
        if (mInputInterceptor == null || mInputInterceptor.mClientChannel == null) {
            return null;
        }
        return mInputInterceptor.mClientChannel.getToken();
    }

    /**
     * @param display The Display that the window being dragged is on.
     */
    CompletableFuture<Void> register(Display display) {
        display.getRealSize(mDisplaySize);
        if (DEBUG_DRAG) Slog.d(TAG_WM, "Registering drag input channel");
        if (mInputInterceptor != null) {
            Slog.e(TAG_WM, "Duplicate register of drag input channel");
            return completedFuture(null);
        } else {
            mInputInterceptor = new InputInterceptor(display);
            return showInputSurface();
        }
    }

    /* call out to each visible window/session informing it about the drag
     */
    void broadcastDragStartedLocked(final float touchX, final float touchY) {
        Trace.instant(TRACE_TAG_WINDOW_MANAGER, "DragDropController#DRAG_STARTED");
        mStartDragDisplayX = mCurrentDisplayX = touchX;
        mStartDragDisplayY = mCurrentDisplayY = touchY;

        // Cache a base-class instance of the clip metadata so that parceling
        // works correctly in calling out to the apps.
        mDataDescription = (mData != null) ? mData.getDescription() : null;
        mNotifiedWindows.clear();
        mDragInProgress = true;

        mSourceUserId = UserHandle.getUserId(mUid);

        final UserManagerInternal userManager = LocalServices.getService(UserManagerInternal.class);
        mCrossProfileCopyAllowed = !userManager.getUserRestriction(
                mSourceUserId, UserManager.DISALLOW_CROSS_PROFILE_COPY_PASTE);

        if (DEBUG_DRAG) {
            Slog.d(TAG_WM, "Broadcasting DRAG_STARTED at (" + touchX + ", " + touchY + ")");
        }

        final boolean containsAppExtras = containsApplicationExtras(mDataDescription);
        mService.mRoot.forAllWindows(w -> {
            sendDragStartedLocked(w, touchX, touchY, containsAppExtras);
        }, false /* traverseTopToBottom */);
    }

    /* helper - send a ACTION_DRAG_STARTED event, if the
     * designated window is potentially a drop recipient.  There are race situations
     * around DRAG_ENDED broadcast, so we make sure that once we've declared that
     * the drag has ended, we never send out another DRAG_STARTED for this drag action.
     *
     * This method clones the 'event' parameter if it's being delivered to the same
     * process, so it's safe for the caller to call recycle() on the event afterwards.
     */
    private void sendDragStartedLocked(WindowState newWin, float touchX, float touchY,
            boolean containsAppExtras) {
        final boolean interceptsGlobalDrag = targetInterceptsGlobalDrag(newWin);
        if (mDragInProgress && isValidDropTarget(newWin, containsAppExtras, interceptsGlobalDrag)) {
            if (DEBUG_DRAG) {
                Slog.d(TAG_WM, "Sending DRAG_STARTED to new window " + newWin);
            }
            // Only allow the extras to be dispatched to a global-intercepting drag target
            ClipData data = null;
            if (interceptsGlobalDrag && mData != null) {
                data = mData.copyForTransferWithActivityInfo();
                PersistableBundle extras = data.getDescription().getExtras() != null
                        ? data.getDescription().getExtras()
                        : new PersistableBundle();
                extras.putInt(EXTRA_HIDE_DRAG_SOURCE_TASK_ID, mCallingTaskIdToHide);
                // Note that setting extras always copies the bundle
                data.getDescription().setExtras(extras);
                if (DEBUG_DRAG) {
                    Slog.d(TAG_WM, "Adding EXTRA_HIDE_DRAG_SOURCE_TASK_ID=" + mCallingTaskIdToHide);
                }
            }
            ClipDescription description = data != null ? data.getDescription() : mDataDescription;

            // Note this can be negative numbers if touch coords are left or top of the window.
            PointF relativeToWindowCoords = new PointF(newWin.translateToWindowX(touchX),
                    newWin.translateToWindowY(touchY));
            if (Flags.enableConnectedDisplaysDnd()
                    && mCurrentDisplayContent.getDisplayId() != newWin.getDisplayId()) {
                // Currently DRAG_STARTED coords are sent relative to the window target in **px**
                // coordinates. However, this cannot be extended to connected displays scenario,
                // as there's only global **dp** coordinates and no global **px** coordinates.
                // Hence, the coords sent here will only try to indicate that drag started outside
                // this window display, but relative distance should not be calculated or depended
                // on.
                relativeToWindowCoords = new PointF(-newWin.getBounds().left - 1,
                        -newWin.getBounds().top - 1);
            }

            DragEvent event = obtainDragEvent(DragEvent.ACTION_DRAG_STARTED,
                    relativeToWindowCoords.x, relativeToWindowCoords.y, description, data,
                    false /* includeDragSurface */, true /* includeDragFlags */,
                    null /* dragAndDropPermission */);
            try {
                newWin.mClient.dispatchDragEvent(event);
                // track each window that we've notified that the drag is starting
                mNotifiedWindows.add(newWin);
            } catch (RemoteException e) {
                Slog.w(TAG_WM, "Unable to drag-start window " + newWin);
            } finally {
                // if the callee was local, the dispatch has already recycled the event
                if (MY_PID != newWin.mSession.mPid) {
                    event.recycle();
                }
            }
        }
    }

    /**
     * Returns true if this is a drag of an application mime type.
     */
    private boolean containsApplicationExtras(ClipDescription desc) {
        if (desc == null) {
            return false;
        }
        return desc.hasMimeType(MIMETYPE_APPLICATION_ACTIVITY)
                || desc.hasMimeType(MIMETYPE_APPLICATION_SHORTCUT)
                || desc.hasMimeType(MIMETYPE_APPLICATION_TASK);
    }

    private boolean isValidDropTarget(WindowState targetWin, boolean containsAppExtras,
            boolean interceptsGlobalDrag) {
        if (targetWin == null) {
            return false;
        }
        final boolean isLocalWindow = mLocalWin == targetWin.mClient.asBinder();
        if (!isLocalWindow && !interceptsGlobalDrag && containsAppExtras) {
            // App-drags can only go to local windows or windows that can intercept global drag, and
            // not to other app windows
            return false;
        }
        if (!targetWin.isPotentialDragTarget(interceptsGlobalDrag)) {
            // Window should not be a target
            return false;
        }
        final boolean isGlobalSameAppDrag = (mFlags & View.DRAG_FLAG_GLOBAL_SAME_APPLICATION) != 0;
        final boolean isGlobalDrag = (mFlags & View.DRAG_FLAG_GLOBAL) != 0;
        final boolean isAnyGlobalDrag = isGlobalDrag || isGlobalSameAppDrag;
        if (!isAnyGlobalDrag || !targetWindowSupportsGlobalDrag(targetWin)) {
            // Drag is limited to the current window.
            if (!isLocalWindow) {
                return false;
            }
        }
        if (isGlobalSameAppDrag) {
            // Drag is limited to app windows from the same uid or windows that can intercept global
            // drag
            if (!interceptsGlobalDrag && mUid != targetWin.getUid()) {
                return false;
            }
        }

        return interceptsGlobalDrag
                || mCrossProfileCopyAllowed
                || mSourceUserId == UserHandle.getUserId(targetWin.getOwningUid());
    }

    private boolean targetWindowSupportsGlobalDrag(WindowState targetWin) {
        // Global drags are limited to system windows, and windows for apps that are targeting N and
        // above.
        return targetWin.mActivityRecord == null
                || targetWin.mActivityRecord.mTargetSdk >= Build.VERSION_CODES.N;
    }

    /**
     * @return whether the given window {@param targetWin} can intercept global drags.
     */
    public boolean targetInterceptsGlobalDrag(@Nullable WindowState targetWin) {
        if (targetWin == null) {
            return false;
        }
        return (targetWin.mAttrs.privateFlags & PRIVATE_FLAG_INTERCEPT_GLOBAL_DRAG_AND_DROP) != 0;
    }

    /* helper - send a ACTION_DRAG_STARTED event only if the window has not
     * previously been notified, i.e. it became visible after the drag operation
     * was begun.  This is a rare case.
     */
    void sendDragStartedIfNeededLocked(WindowState newWin) {
        if (mDragInProgress) {
            // If we have sent the drag-started, we needn't do so again
            if (isWindowNotified(newWin)) {
                return;
            }
            sendDragStartedLocked(newWin, mCurrentDisplayX, mCurrentDisplayY,
                    containsApplicationExtras(mDataDescription));
        }
    }

    boolean isWindowNotified(WindowState newWin) {
        for (WindowState ws : mNotifiedWindows) {
            if (ws == newWin) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ends the current drag, animating the drag surface back to the source if the drop was not
     * consumed by the receiving window.
     */
    void endDragLocked(boolean dropConsumed, boolean relinquishDragSurfaceToDropTarget) {
        mDragResult = dropConsumed;
        mRelinquishDragSurfaceToDropTarget = relinquishDragSurfaceToDropTarget;
        if (mAnimator != null) {
            return;
        }
        if (!mDragResult) {
            if (!isAccessibilityDragDrop() && !relinquishDragSurfaceToDragSource()) {
                mAnimator = createReturnAnimationLocked();
                return;  // Will call closeLocked() when the animation is done.
            }
        }
        closeLocked();
    }

    void cancelDragLocked(boolean skipAnimation) {
        if (mAnimator != null) {
            return;
        }
        if (!mDragInProgress || skipAnimation || isAccessibilityDragDrop()) {
            // mDragInProgress is false if an app invokes Session#cancelDragAndDrop before
            // Session#performDrag. Reset the drag state without playing the cancel animation
            // because H.DRAG_START_TIMEOUT may be sent to WindowManagerService, which will cause
            // DragState#reset() while playing the cancel animation.
            // skipAnimation is true when a caller requests to skip the drag cancel animation.
            closeLocked();
            return;
        }
        mAnimator = createCancelAnimationLocked();
    }

    /**
     * Updates the position of the drag surface.
     *
     * @param keepHandling whether to keep handling the drag.
     * @param displayId the display ID of the drag surface.
     * @param displayX the x-coordinate of the drag surface in the display's coordinate frame.
     * @param displayY the y-coordinate of the drag surface in the display's coordinate frame.
     */
    void updateDragSurfaceLocked(boolean keepHandling, int displayId, float displayX,
            float displayY) {
        if (mAnimator != null) {
            return;
        }
        mCurrentDisplayX = displayX;
        mCurrentDisplayY = displayY;

        final DisplayContent lastSetDisplayContent = mCurrentDisplayContent;
        boolean cursorMovedToDifferentDisplay = false;
        // Keep latest display up-to-date even when drag has stopped.
        if (Flags.enableConnectedDisplaysDnd() && mCurrentDisplayContent.mDisplayId != displayId) {
            final DisplayContent newDisplay = mService.mRoot.getDisplayContent(displayId);
            if (newDisplay == null) {
                Slog.e(TAG_WM, "Target displayId=" + displayId + " was not found, ending drag.");
                endDragLocked(false /* dropConsumed */,
                        false /* relinquishDragSurfaceToDropTarget */);
                return;
            }
            cursorMovedToDifferentDisplay = true;
            mCurrentDisplayContent = newDisplay;
        }
        if (!keepHandling) {
            return;
        }

        // Move the surface to the given touch
        if (SHOW_LIGHT_TRANSACTIONS) {
            Slog.i(TAG_WM, ">>> OPEN TRANSACTION notifyMoveLocked");
        }
        if (cursorMovedToDifferentDisplay) {
            mAnimatedScale = mAnimatedScale * mCurrentDisplayContent.mBaseDisplayDensity
                    / lastSetDisplayContent.mBaseDisplayDensity;
            mThumbOffsetX = mThumbOffsetX * mCurrentDisplayContent.mBaseDisplayDensity
                    / lastSetDisplayContent.mBaseDisplayDensity;
            mThumbOffsetY = mThumbOffsetY * mCurrentDisplayContent.mBaseDisplayDensity
                    / lastSetDisplayContent.mBaseDisplayDensity;
            mTransaction.reparent(mSurfaceControl, mCurrentDisplayContent.getSurfaceControl());
            mTransaction.setScale(mSurfaceControl, mAnimatedScale, mAnimatedScale);

            final InputWindowHandle inputWindowHandle = getInputWindowHandle();
            if (inputWindowHandle == null) {
                Slog.w(TAG_WM, "Drag is in progress but there is no drag window handle.");
                return;
            }
            inputWindowHandle.displayId = displayId;
            mTransaction.setInputWindowInfo(mInputSurface, inputWindowHandle);
        }
        mTransaction.setPosition(mSurfaceControl, displayX - mThumbOffsetX,
                displayY - mThumbOffsetY).apply();
        ProtoLog.i(WM_SHOW_TRANSACTIONS, "DRAG %s: displayId=%d, pos=(%d,%d)", mSurfaceControl,
                displayId, (int) (displayX - mThumbOffsetX), (int) (displayY - mThumbOffsetY));
    }

    private DragEvent obtainDragEndedEvent(float x, float y, boolean includeDragSurface) {
        return obtainDragEvent(DragEvent.ACTION_DRAG_ENDED, x, y, /* description= */
                null, /* data= */ null, includeDragSurface, /* includeDragFlags= */
                true, /* dragAndDropPermissions= */ null, mDragResult);
    }

    private DragEvent obtainDragEvent(int action, float x, float y, ClipDescription description,
            ClipData data, boolean includeDragSurface, boolean includeDragFlags,
            IDragAndDropPermissions dragAndDropPermissions) {
        return obtainDragEvent(action, x, y, description, data, includeDragSurface,
                includeDragFlags, dragAndDropPermissions, /* dragResult= */ false);
    }

    /**
     * `x` and `y` here varies between local window coordinate, relative coordinate to another
     * window and local display coordinate, all depending on the `action`. Please take a look
     * at the callers to determine the type.
     * - ACTION_DRAG_STARTED: (x, y) is relative coordinate to the target window's origin
     *                          (possible to have negative values).
     * - ACTION_DROP:
     * --- UnhandledDropEvent: (x, y) is in display space coordinate.
     * --- DropEvent: (x, y) is in local window coordinate where event is targeted to.
     * - ACTION_DRAG_ENDED: (x, y) is in local window coordinate where event is targeted to.
     */
    private DragEvent obtainDragEvent(int action, float x, float y, ClipDescription description,
            ClipData data, boolean includeDragSurface, boolean includeDragFlags,
            IDragAndDropPermissions dragAndDropPermissions, boolean dragResult) {
        return DragEvent.obtain(action, x, y, mThumbOffsetX, mThumbOffsetY,
                mCurrentDisplayContent.getDisplayId(), includeDragFlags ? mFlags : 0,
                null  /* localState */, description, data,
                includeDragSurface ? mSurfaceControl : null, dragAndDropPermissions, dragResult);
    }

    private ValueAnimator createReturnAnimationLocked() {
        final ValueAnimator animator;
        final long duration;
        if (mCallingTaskIdToHide != -1) {
            animator = ValueAnimator.ofPropertyValuesHolder(
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_X, mCurrentDisplayX,
                            mCurrentDisplayX),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_Y, mCurrentDisplayY,
                            mCurrentDisplayY),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_SCALE, mAnimatedScale,
                            mAnimatedScale),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_ALPHA, mStartDragAlpha, 0f));
            duration = MIN_ANIMATION_DURATION_MS;
        } else if (Flags.enableConnectedDisplaysDnd() && mCurrentDisplayContent.getDisplayId()
                != mStartDragDisplayContent.getDisplayId()) {
            animator = ValueAnimator.ofPropertyValuesHolder(
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_X,
                            mCurrentDisplayX - mThumbOffsetX, mCurrentDisplayX - mThumbOffsetX),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_Y,
                            mCurrentDisplayY - mThumbOffsetY, mCurrentDisplayY - mThumbOffsetY),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_SCALE, mAnimatedScale,
                            DIFFERENT_DISPLAY_RETURN_ANIMATION_SCALE * mAnimatedScale),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_ALPHA, mStartDragAlpha, 0f));
            duration = MIN_ANIMATION_DURATION_MS;
        } else {
            animator = ValueAnimator.ofPropertyValuesHolder(
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_X,
                            mCurrentDisplayX - mThumbOffsetX, mStartDragDisplayX - mThumbOffsetX),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_Y,
                            mCurrentDisplayY - mThumbOffsetY, mStartDragDisplayY - mThumbOffsetY),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_SCALE, mAnimatedScale,
                            mAnimatedScale),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_ALPHA, mStartDragAlpha,
                            mStartDragAlpha / 2));

            final float translateX = mStartDragDisplayX - mCurrentDisplayX;
            final float translateY = mStartDragDisplayY - mCurrentDisplayY;
            // Adjust the duration to the travel distance.
            final double travelDistance = Math.sqrt(
                    translateX * translateX + translateY * translateY);
            final double displayDiagonal = Math.sqrt(
                    mDisplaySize.x * mDisplaySize.x + mDisplaySize.y * mDisplaySize.y);
            duration = MIN_ANIMATION_DURATION_MS + (long) (travelDistance / displayDiagonal * (
                    MAX_ANIMATION_DURATION_MS - MIN_ANIMATION_DURATION_MS));
        }

        final AnimationListener listener = new AnimationListener();
        animator.setDuration(duration);
        animator.setInterpolator(mCubicEaseOutInterpolator);
        animator.addListener(listener);
        animator.addUpdateListener(listener);

        mService.mAnimationHandler.post(() -> animator.start());
        return animator;
    }

    private ValueAnimator createCancelAnimationLocked() {
        final ValueAnimator animator;
        if (mCallingTaskIdToHide != -1) {
            animator = ValueAnimator.ofPropertyValuesHolder(
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_X, mCurrentDisplayX,
                            mCurrentDisplayX),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_Y, mCurrentDisplayY,
                            mCurrentDisplayY),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_SCALE, mAnimatedScale,
                            mAnimatedScale),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_ALPHA, mStartDragAlpha, 0f));
        } else {
            animator = ValueAnimator.ofPropertyValuesHolder(
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_X,
                            mCurrentDisplayX - mThumbOffsetX, mCurrentDisplayX),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_Y,
                            mCurrentDisplayY - mThumbOffsetY, mCurrentDisplayY),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_SCALE, mAnimatedScale, 0),
                    PropertyValuesHolder.ofFloat(ANIMATED_PROPERTY_ALPHA, mStartDragAlpha, 0));
        }

        final AnimationListener listener = new AnimationListener();
        animator.setDuration(MIN_ANIMATION_DURATION_MS);
        animator.setInterpolator(mCubicEaseOutInterpolator);
        animator.addListener(listener);
        animator.addUpdateListener(listener);

        mService.mAnimationHandler.post(() -> animator.start());
        return animator;
    }

    private class AnimationListener
            implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            try (SurfaceControl.Transaction transaction =
                         mService.mTransactionFactory.get()) {
                transaction.setPosition(
                        mSurfaceControl,
                        (float) animation.getAnimatedValue(ANIMATED_PROPERTY_X),
                        (float) animation.getAnimatedValue(ANIMATED_PROPERTY_Y));
                transaction.setAlpha(
                        mSurfaceControl,
                        (float) animation.getAnimatedValue(ANIMATED_PROPERTY_ALPHA));
                transaction.setMatrix(
                        mSurfaceControl,
                        (float) animation.getAnimatedValue(ANIMATED_PROPERTY_SCALE), 0,
                        0, (float) animation.getAnimatedValue(ANIMATED_PROPERTY_SCALE));
                transaction.apply();
            }
        }

        @Override
        public void onAnimationStart(Animator animator) {}

        @Override
        public void onAnimationCancel(Animator animator) {}

        @Override
        public void onAnimationRepeat(Animator animator) {}

        @Override
        public void onAnimationEnd(Animator animator) {
            mAnimationCompleted = true;
            // Updating mDragState requires the WM lock so continues it on the out of
            // AnimationThread.
            mDragDropController.sendHandlerMessage(MSG_ANIMATION_END, null);
        }
    }

    boolean isAccessibilityDragDrop() {
        return (mFlags & View.DRAG_FLAG_ACCESSIBILITY_ACTION) != 0;
    }

    private boolean relinquishDragSurfaceToDragSource() {
        return (mFlags & View.DRAG_FLAG_REQUEST_SURFACE_FOR_RETURN_ANIMATION) != 0;
    }
}
