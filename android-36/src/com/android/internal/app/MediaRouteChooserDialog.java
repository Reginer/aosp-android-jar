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
import android.content.Context;
import android.media.MediaRouter;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.android.internal.R;

/**
 * This class implements the route chooser dialog for {@link MediaRouter}.
 * <p>
 * This dialog allows the user to choose a route that matches a given selector.
 * </p>
 *
 * @see MediaRouteButton
 * @see MediaRouteActionProvider
 *
 * TODO: Move this back into the API, as in the support library media router.
 */
public class MediaRouteChooserDialog extends AlertDialog implements
        MediaRouteChooserContentManager.Delegate {
    private View.OnClickListener mExtendedSettingsClickListener;
    private Button mExtendedSettingsButton;
    private final boolean mShowProgressBarWhenEmpty;

    private final MediaRouteChooserContentManager mContentManager;

    public MediaRouteChooserDialog(Context context, int theme) {
        this(context, theme, true);
    }

    public MediaRouteChooserDialog(Context context, int theme, boolean showProgressBarWhenEmpty) {
        super(context, theme);

        mShowProgressBarWhenEmpty = showProgressBarWhenEmpty;
        mContentManager = new MediaRouteChooserContentManager(context, this);
    }

    /**
     * Sets the types of routes that will be shown in the media route chooser dialog
     * launched by this button.
     *
     * @param types The route types to match.
     */
    public void setRouteTypes(int types) {
        mContentManager.setRouteTypes(types);
    }

    public void setExtendedSettingsClickListener(View.OnClickListener listener) {
        if (listener != mExtendedSettingsClickListener) {
            mExtendedSettingsClickListener = listener;
            updateExtendedSettingsButton();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Note: setView must be called before super.onCreate().
        View containerView = LayoutInflater.from(getContext()).inflate(
                R.layout.media_route_chooser_dialog, null);
        setView(containerView);

        setTitle(mContentManager.getRouteTypes() == MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY
                ? R.string.media_route_chooser_title_for_remote_display
                : R.string.media_route_chooser_title);

        setIcon(isLightTheme(getContext()) ? R.drawable.ic_media_route_off_holo_light
                : R.drawable.ic_media_route_off_holo_dark);

        super.onCreate(savedInstanceState);

        mExtendedSettingsButton = findViewById(R.id.media_route_extended_settings_button);
        updateExtendedSettingsButton();

        mContentManager.bindViews(containerView);
    }

    private void updateExtendedSettingsButton() {
        if (mExtendedSettingsButton != null) {
            mExtendedSettingsButton.setOnClickListener(mExtendedSettingsClickListener);
            mExtendedSettingsButton.setVisibility(
                    mExtendedSettingsClickListener != null ? View.VISIBLE : View.GONE);
        }
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
    public void dismissView() {
        dismiss();
    }

    @Override
    public boolean showProgressBarWhenEmpty() {
        return mShowProgressBarWhenEmpty;
    }

    static boolean isLightTheme(Context context) {
        TypedValue value = new TypedValue();
        return context.getTheme().resolveAttribute(R.attr.isLightTheme, value, true)
                && value.data != 0;
    }
}
