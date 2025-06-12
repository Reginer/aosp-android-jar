/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.internal.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.media.MediaRouter;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.android.internal.R;

/**
 * This class manages the content display within the media route controller UI.
 */
public class MediaRouteControllerContentManager {
    /**
     * A delegate interface that a MediaRouteController UI should implement. It allows the content
     * manager to inform the UI of any UI changes that need to be made in response to content
     * updates.
     */
    public interface Delegate {
        /**
         * Updates the title of the media route device
         */
        void setMediaRouteDeviceTitle(CharSequence title);

        /**
         * Updates the icon of the media route device
         */
        void setMediaRouteDeviceIcon(Drawable icon);

        /**
         * Dismiss the UI to transition to a different workflow.
         */
        void dismissView();
    }

    private final Context mContext;
    private final Delegate mDelegate;

    // Time to wait before updating the volume when the user lets go of the seek bar
    // to allow the route provider time to propagate the change and publish a new
    // route descriptor.
    private static final int VOLUME_UPDATE_DELAY_MILLIS = 250;

    private final MediaRouter mRouter;
    private final MediaRouteControllerContentManager.MediaRouterCallback mCallback;
    private final MediaRouter.RouteInfo mRoute;

    private Drawable mMediaRouteButtonDrawable;
    private final int[] mMediaRouteConnectingState = { R.attr.state_checked, R.attr.state_enabled };
    private final int[] mMediaRouteOnState = { R.attr.state_activated, R.attr.state_enabled };
    private Drawable mCurrentIconDrawable;

    private boolean mAttachedToWindow;

    private LinearLayout mVolumeLayout;
    private SeekBar mVolumeSlider;
    private boolean mVolumeSliderTouched;

    public MediaRouteControllerContentManager(Context context, Delegate delegate) {
        mContext = context;
        mDelegate = delegate;
        mRouter = context.getSystemService(MediaRouter.class);
        mCallback = new MediaRouteControllerContentManager.MediaRouterCallback();
        mRoute = mRouter.getSelectedRoute();
    }

    /**
     * Starts binding all the views (volume layout, slider, etc.) using the
     * given container view.
     */
    public void bindViews(View containerView) {
        mDelegate.setMediaRouteDeviceTitle(mRoute.getName());
        mVolumeLayout = containerView.findViewById(R.id.media_route_volume_layout);
        mVolumeSlider = containerView.findViewById(R.id.media_route_volume_slider);
        mVolumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private final Runnable mStopTrackingTouch = new Runnable() {
                @Override
                public void run() {
                    if (mVolumeSliderTouched) {
                        mVolumeSliderTouched = false;
                        updateVolume();
                    }
                }
            };

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mVolumeSliderTouched) {
                    mVolumeSlider.removeCallbacks(mStopTrackingTouch);
                } else {
                    mVolumeSliderTouched = true;
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Defer resetting mVolumeSliderTouched to allow the media route provider
                // a little time to settle into its new state and publish the final
                // volume update.
                mVolumeSlider.postDelayed(mStopTrackingTouch, VOLUME_UPDATE_DELAY_MILLIS);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mRoute.requestSetVolume(progress);
                }
            }
        });

        mMediaRouteButtonDrawable = obtainMediaRouteButtonDrawable();
    }

    /**
     * Called when this UI is attached to a window..
     */
    public void onAttachedToWindow() {
        mAttachedToWindow = true;
        mRouter.addCallback(0, mCallback, MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS);
        update();
    }

    /**
     * Called when this UI is detached from a window..
     */
    public void onDetachedFromWindow() {
        mRouter.removeCallback(mCallback);
        mAttachedToWindow = false;
    }

    /**
     * Updates all the views to reflect new states.
     */
    public void update() {
        if (!mRoute.isSelected() || mRoute.isDefault()) {
            mDelegate.dismissView();
        }

        mDelegate.setMediaRouteDeviceTitle(mRoute.getName());
        updateVolume();

        Drawable icon = getIconDrawable();
        if (icon != mCurrentIconDrawable) {
            mCurrentIconDrawable = icon;
            if (icon instanceof AnimationDrawable animDrawable) {
                if (!mAttachedToWindow && !mRoute.isConnecting()) {
                    // When the route is already connected before the view is attached, show the
                    // last frame of the connected animation immediately.
                    if (animDrawable.isRunning()) {
                        animDrawable.stop();
                    }
                    icon = animDrawable.getFrame(animDrawable.getNumberOfFrames() - 1);
                } else if (!animDrawable.isRunning()) {
                    animDrawable.start();
                }
            }
            mDelegate.setMediaRouteDeviceIcon(icon);
        }
    }

    private void updateVolume() {
        if (!mVolumeSliderTouched) {
            if (isVolumeControlAvailable()) {
                mVolumeLayout.setVisibility(View.VISIBLE);
                mVolumeSlider.setMax(mRoute.getVolumeMax());
                mVolumeSlider.setProgress(mRoute.getVolume());
            } else {
                mVolumeLayout.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Callback function to triggered after the disconnect button is clicked.
     */
    public void onDisconnectButtonClick() {
        if (mRoute.isSelected()) {
            if (mRoute.isBluetooth()) {
                mRouter.getDefaultRoute().select();
            } else {
                mRouter.getFallbackRoute().select();
            }
        }
        mDelegate.dismissView();
    }

    /**
     * Request the media route to update volume.
     */
    public void requestUpdateRouteVolume(int direction) {
        mRoute.requestUpdateVolume(direction);
    }

    private boolean isVolumeControlAvailable() {
        return mRoute.getVolumeHandling() == MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE;
    }

    private Drawable obtainMediaRouteButtonDrawable() {
        TypedValue value = new TypedValue();
        if (!mContext.getTheme().resolveAttribute(R.attr.mediaRouteButtonStyle, value, true)) {
            return null;
        }
        int[] drawableAttrs = new int[] { R.attr.externalRouteEnabledDrawable };
        TypedArray a = mContext.obtainStyledAttributes(value.data, drawableAttrs);
        Drawable drawable = a.getDrawable(0);
        a.recycle();
        return drawable;
    }

    private Drawable getIconDrawable() {
        if (!(mMediaRouteButtonDrawable instanceof StateListDrawable)) {
            return mMediaRouteButtonDrawable;
        } else if (mRoute.isConnecting()) {
            StateListDrawable stateListDrawable = (StateListDrawable) mMediaRouteButtonDrawable;
            stateListDrawable.setState(mMediaRouteConnectingState);
            return stateListDrawable.getCurrent();
        } else {
            StateListDrawable stateListDrawable = (StateListDrawable) mMediaRouteButtonDrawable;
            stateListDrawable.setState(mMediaRouteOnState);
            return stateListDrawable.getCurrent();
        }
    }

    private final class MediaRouterCallback extends MediaRouter.SimpleCallback {
        @Override
        public void onRouteUnselected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
            update();
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            update();
        }

        @Override
        public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            if (route == mRoute) {
                updateVolume();
            }
        }

        @Override
        public void onRouteGrouped(MediaRouter router, MediaRouter.RouteInfo info,
                MediaRouter.RouteGroup group, int index) {
            update();
        }

        @Override
        public void onRouteUngrouped(MediaRouter router, MediaRouter.RouteInfo info,
                MediaRouter.RouteGroup group) {
            update();
        }
    }
}
