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

package com.android.internal.widget;

import android.annotation.ColorInt;
import android.annotation.Nullable;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RemoteViews;

import com.android.internal.R;

/**
 * A close button in a notification
 */
@RemoteViews.RemoteView
public class NotificationCloseButton extends ImageView {

    @ColorInt private int mBackgroundColor;
    @ColorInt private int mForegroundColor;

    private Drawable mPillDrawable;

    public NotificationCloseButton(Context context) {
        this(context, null, 0, 0);
    }

    public NotificationCloseButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public NotificationCloseButton(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationCloseButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setContentDescription(mContext.getText(R.string.close_button_text));

        final LayerDrawable layeredPill = (LayerDrawable) this.getBackground();
        mPillDrawable = layeredPill.findDrawableByLayerId(R.id.close_button_pill_colorized_layer);

        boolean notificationCloseButtonSupported = Resources.getSystem().getBoolean(
                com.android.internal.R.bool.config_notificationCloseButtonSupported);
        this.setVisibility(notificationCloseButtonSupported ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(Button.class.getName());
    }


    private void updateColors() {
        if (mBackgroundColor != 0) {
            // TODO(http://b/365585705): Ensure this close button compatible with the ongoing effort
            // that makes notification rows partially-transparent.
            this.mPillDrawable.setTintList(ColorStateList.valueOf(mBackgroundColor));
        }

        if (mForegroundColor != 0) {
            this.setImageTintList(ColorStateList.valueOf(mForegroundColor));
        }
    }

    /**
     * Set the color used for the foreground.
     */
    @RemotableViewMethod
    public void setForegroundColor(@ColorInt int color) {
        mForegroundColor = color;
        updateColors();
    }

    /**
     * Sets the color used for the background.
     */
    @RemotableViewMethod
    public void setBackgroundColor(@ColorInt int color) {
        mBackgroundColor = color;
        updateColors();
    }
}
