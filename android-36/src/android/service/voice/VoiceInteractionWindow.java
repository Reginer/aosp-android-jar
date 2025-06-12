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

package android.service.voice;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.IntDef;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Debug;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.lang.annotation.Retention;

/**
 * A {@link VoiceInteractionWindow} is a {@link Dialog} that is intended to be used for a top-level
 * {@link VoiceInteractionSession}. It will be displayed along the edge of the screen, moving the
 * application user interface away from it so that the focused item is always visible.
 */
final class VoiceInteractionWindow extends Dialog {
    private static final boolean DEBUG = false;
    private static final String TAG = "VoiceInteractionWindow";

    private final String mName;
    private final Callback mCallback;
    private final KeyEvent.Callback mKeyEventCallback;
    private final KeyEvent.DispatcherState mDispatcherState;
    private final int mWindowType;
    private final int mGravity;
    private final boolean mTakesFocus;
    private final Rect mBounds = new Rect();

    @Retention(SOURCE)
    @IntDef(value = {WindowState.TOKEN_PENDING, WindowState.TOKEN_SET,
            WindowState.SHOWN_AT_LEAST_ONCE, WindowState.REJECTED_AT_LEAST_ONCE,
            WindowState.DESTROYED})
    private @interface WindowState {
        /**
         * The window token is not set yet.
         */
        int TOKEN_PENDING = 0;
        /**
         * The window token was set, but the window is not shown yet.
         */
        int TOKEN_SET = 1;
        /**
         * The window was shown at least once.
         */
        int SHOWN_AT_LEAST_ONCE = 2;
        /**
         * {@link WindowManager.BadTokenException} was sent when calling
         * {@link Dialog#show()} at least once.
         */
        int REJECTED_AT_LEAST_ONCE = 3;
        /**
         * The window is considered destroyed.  Any incoming request should be ignored.
         */
        int DESTROYED = 4;
    }

    @WindowState
    private int mWindowState = WindowState.TOKEN_PENDING;

    /**
     * Used to provide callbacks.
     */
    interface Callback {
        /**
         * Used to be notified when {@link Dialog#onBackPressed()} gets called.
         */
        void onBackPressed();
    }

    /**
     * Set {@link IBinder} window token to the window.
     *
     * <p>This method can be called only once.</p>
     * @param token {@link IBinder} token to be associated with the window.
     */
    void setToken(IBinder token) {
        switch (mWindowState) {
            case WindowState.TOKEN_PENDING:
                // Normal scenario.  Nothing to worry about.
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.token = token;
                getWindow().setAttributes(lp);
                updateWindowState(WindowState.TOKEN_SET);

                // As soon as we have a token, make sure the window is added (but not shown) by
                // setting visibility to INVISIBLE and calling show() on Dialog. Note that
                // WindowInsetsController.OnControllableInsetsChangedListener relies on the window
                // being added to function.
                getWindow().getDecorView().setVisibility(View.INVISIBLE);
                show();
                return;
            case WindowState.TOKEN_SET:
            case WindowState.SHOWN_AT_LEAST_ONCE:
            case WindowState.REJECTED_AT_LEAST_ONCE:
                throw new IllegalStateException("setToken can be called only once");
            case WindowState.DESTROYED:
                // Just ignore.  Since there are multiple event queues from the token is issued
                // in the system server to the timing when it arrives here, it can be delivered
                // after the is already destroyed.  No one should be blamed because of such an
                // unfortunate but possible scenario.
                Log.i(TAG, "Ignoring setToken() because window is already destroyed.");
                return;
            default:
                throw new IllegalStateException("Unexpected state=" + mWindowState);
        }
    }

    /**
     * Create a {@link VoiceInteractionWindow} that uses a custom style.
     *
     * @param context The Context in which the DockWindow should run. In
     *        particular, it uses the window manager and theme from this context
     *        to present its UI.
     * @param theme A style resource describing the theme to use for the window.
     *        See <a href="{@docRoot}reference/available-resources.html#stylesandthemes">Style
     *        and Theme Resources</a> for more information about defining and
     *        using styles. This theme is applied on top of the current theme in
     *        <var>context</var>. If 0, the default dialog theme will be used.
     */
    VoiceInteractionWindow(Context context, String name, int theme, Callback callback,
            KeyEvent.Callback keyEventCallback, KeyEvent.DispatcherState dispatcherState,
            int windowType, int gravity, boolean takesFocus) {
        super(context, theme);
        mName = name;
        mCallback = callback;
        mKeyEventCallback = keyEventCallback;
        mDispatcherState = dispatcherState;
        mWindowType = windowType;
        mGravity = gravity;
        mTakesFocus = takesFocus;
        initDockWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mDispatcherState.reset();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        getWindow().getDecorView().getHitRect(mBounds);

        if (ev.isWithinBoundsNoHistory(mBounds.left, mBounds.top,
                mBounds.right - 1, mBounds.bottom - 1)) {
            return super.dispatchTouchEvent(ev);
        } else {
            MotionEvent temp = ev.clampNoHistory(mBounds.left, mBounds.top,
                    mBounds.right - 1, mBounds.bottom - 1);
            boolean handled = super.dispatchTouchEvent(temp);
            temp.recycle();
            return handled;
        }
    }

    private void updateWidthHeight(WindowManager.LayoutParams lp) {
        if (lp.gravity == Gravity.TOP || lp.gravity == Gravity.BOTTOM) {
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        } else {
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mKeyEventCallback != null && mKeyEventCallback.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (mKeyEventCallback != null && mKeyEventCallback.onKeyLongPress(keyCode, event)) {
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mKeyEventCallback != null && mKeyEventCallback.onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
        if (mKeyEventCallback != null && mKeyEventCallback.onKeyMultiple(keyCode, count, event)) {
            return true;
        }
        return super.onKeyMultiple(keyCode, count, event);
    }

    @Override
    public void onBackPressed() {
        if (mCallback != null) {
            mCallback.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    private void initDockWindow() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();

        lp.type = mWindowType;
        lp.setTitle(mName);

        lp.gravity = mGravity;
        updateWidthHeight(lp);

        getWindow().setAttributes(lp);

        int windowSetFlags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        int windowModFlags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_DIM_BEHIND;

        if (!mTakesFocus) {
            windowSetFlags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        } else {
            windowSetFlags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            windowModFlags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        }

        getWindow().setFlags(windowSetFlags, windowModFlags);
    }

    @Override
    public void show() {
        switch (mWindowState) {
            case WindowState.TOKEN_PENDING:
                throw new IllegalStateException("Window token is not set yet.");
            case WindowState.TOKEN_SET:
            case WindowState.SHOWN_AT_LEAST_ONCE:
                // Normal scenario.  Nothing to worry about.
                try {
                    super.show();
                    updateWindowState(WindowState.SHOWN_AT_LEAST_ONCE);
                } catch (WindowManager.BadTokenException e) {
                    // Just ignore this exception.  Since show() can be requested from other
                    // components such as the system and there could be multiple event queues before
                    // the request finally arrives here, the system may have already invalidated the
                    // window token attached to our window.  In such a scenario, receiving
                    // BadTokenException here is an expected behavior.  We just ignore it and update
                    // the state so that we do not touch this window later.
                    Log.i(TAG, "Probably the IME window token is already invalidated."
                            + " show() does nothing.");
                    updateWindowState(WindowState.REJECTED_AT_LEAST_ONCE);
                }
                return;
            case WindowState.REJECTED_AT_LEAST_ONCE:
                // Just ignore.  In general we cannot completely avoid this kind of race condition.
                Log.i(TAG, "Not trying to call show() because it was already rejected once.");
                return;
            case WindowState.DESTROYED:
                // Just ignore.  In general we cannot completely avoid this kind of race condition.
                Log.i(TAG, "Ignoring show() because the window is already destroyed.");
                return;
            default:
                throw new IllegalStateException("Unexpected state=" + mWindowState);
        }
    }

    private void updateWindowState(@WindowState int newState) {
        if (DEBUG) {
            if (mWindowState != newState) {
                Log.d(TAG, "WindowState: " + stateToString(mWindowState) + " -> "
                        + stateToString(newState) + " @ " + Debug.getCaller());
            }
        }
        mWindowState = newState;
    }

    private static String stateToString(@WindowState int state) {
        switch (state) {
            case WindowState.TOKEN_PENDING:
                return "TOKEN_PENDING";
            case WindowState.TOKEN_SET:
                return "TOKEN_SET";
            case WindowState.SHOWN_AT_LEAST_ONCE:
                return "SHOWN_AT_LEAST_ONCE";
            case WindowState.REJECTED_AT_LEAST_ONCE:
                return "REJECTED_AT_LEAST_ONCE";
            case WindowState.DESTROYED:
                return "DESTROYED";
            default:
                throw new IllegalStateException("Unknown state=" + state);
        }
    }
}
