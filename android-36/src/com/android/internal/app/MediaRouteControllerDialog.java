/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.AlertDialog;
import android.app.MediaRouteActionProvider;
import android.app.MediaRouteButton;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.MediaRouter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import com.android.internal.R;

/**
 * This class implements the route controller dialog for {@link MediaRouter}.
 * <p>
 * This dialog allows the user to control or disconnect from the currently selected route.
 * </p>
 *
 * @see MediaRouteButton
 * @see MediaRouteActionProvider
 *
 * TODO: Move this back into the API, as in the support library media router.
 */
public class MediaRouteControllerDialog extends AlertDialog implements
        MediaRouteControllerContentManager.Delegate {
    private final MediaRouteControllerContentManager mContentManager;

    public MediaRouteControllerDialog(Context context, int theme) {
        super(context, theme);
        mContentManager = new MediaRouteControllerContentManager(context, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Resources res = getContext().getResources();
        setButton(BUTTON_NEGATIVE, res.getString(R.string.media_route_controller_disconnect),
                (dialogInterface, id) -> mContentManager.onDisconnectButtonClick());
        View customView = getLayoutInflater().inflate(R.layout.media_route_controller_dialog, null);
        setView(customView, 0, 0, 0, 0);
        mContentManager.bindViews(customView);
        super.onCreate(savedInstanceState);

        View customPanelView = getWindow().findViewById(R.id.customPanel);
        if (customPanelView != null) {
            customPanelView.setMinimumHeight(0);
        }

        mContentManager.update();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mContentManager.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        mContentManager.onDetachedFromWindow();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            mContentManager.requestUpdateRouteVolume(
                    keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ? -1 : 1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void setMediaRouteDeviceTitle(CharSequence title) {
        setTitle(title);
    }

    @Override
    public void setMediaRouteDeviceIcon(Drawable icon) {
        setIcon(icon);
    }

    @Override
    public void dismissView() {
        dismiss();
    }
}
