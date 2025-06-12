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

package com.android.internal.widget;

import android.annotation.AttrRes;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.StyleRes;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.flags.Flags;

import com.android.internal.R;

/**
 * An ImageView used by BigPicture Notifications to correctly resolve the Uri in an Icon using the
 * LocalImageResolver, allowing it to support animated drawables which are not supported by
 * Icon.loadDrawable().
 */
@RemoteViews.RemoteView
public class BigPictureNotificationImageView extends ImageView implements
        NotificationDrawableConsumer {

    private static final String TAG = BigPictureNotificationImageView.class.getSimpleName();

    private final int mMaximumDrawableWidth;
    private final int mMaximumDrawableHeight;

    private NotificationIconManager mIconManager;

    public BigPictureNotificationImageView(@NonNull Context context) {
        this(context, null, 0, 0);
    }

    public BigPictureNotificationImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public BigPictureNotificationImageView(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BigPictureNotificationImageView(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        boolean isLowRam = ActivityManager.isLowRamDeviceStatic();
        mMaximumDrawableWidth = context.getResources().getDimensionPixelSize(
                isLowRam ? R.dimen.notification_big_picture_max_width_low_ram
                        : R.dimen.notification_big_picture_max_width);
        mMaximumDrawableHeight = context.getResources().getDimensionPixelSize(
                isLowRam ? R.dimen.notification_big_picture_max_height_low_ram
                        : R.dimen.notification_big_picture_max_height);
    }


    /**
     * Sets an {@link NotificationIconManager} on this ImageView, which handles the loading of
     * icons, instead of using the {@link LocalImageResolver} directly.
     * If set, it overrides the behaviour of {@link #setImageIconAsync} and {@link #setImageIcon},
     * and it expects that the content of this imageView is only updated calling these two methods.
     *
     * @param iconManager to be called, when the icon is updated
     */
    public void setIconManager(NotificationIconManager iconManager) {
        mIconManager = iconManager;
    }

    @Override
    @android.view.RemotableViewMethod(asyncImpl = "setImageURIAsync")
    public void setImageURI(@Nullable Uri uri) {
        setImageDrawable(loadImage(uri));
    }

    /** @hide **/
    public Runnable setImageURIAsync(@Nullable Uri uri) {
        final Drawable drawable = loadImage(uri);
        return () -> setImageDrawable(drawable);
    }

    @Override
    @android.view.RemotableViewMethod(asyncImpl = "setImageIconAsync")
    public void setImageIcon(@Nullable Icon icon) {
        if (mIconManager != null) {
            mIconManager.updateIcon(this, icon).run();
            return;
        }
        // old code path
        setImageDrawable(loadImage(icon));
    }

    /** @hide **/
    public Runnable setImageIconAsync(@Nullable Icon icon) {
        if (mIconManager != null) {
            return mIconManager.updateIcon(this, icon);
        }
        // old code path
        final Drawable drawable = loadImage(icon);
        return () -> setImageDrawable(drawable);
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        if (drawable instanceof BitmapDrawable bitmapDrawable) {
            if (bitmapDrawable.getBitmap() == null) {
                if (Flags.bigPictureStyleDiscardEmptyIconBitmapDrawables()) {
                    Log.e(TAG, "discarding BitmapDrawable with null Bitmap (invalid image file?)");
                    drawable = null;
                } else {
                    Log.e(TAG, "setting BitmapDrawable with null Bitmap (invalid image file?)");
                }
            }
        }

        super.setImageDrawable(drawable);
    }

    private Drawable loadImage(Uri uri) {
        if (uri == null) return null;
        return LocalImageResolver.resolveImage(uri, mContext, mMaximumDrawableWidth,
                mMaximumDrawableHeight);
    }

    private Drawable loadImage(Icon icon) {
        if (icon == null) return null;

        Drawable drawable = LocalImageResolver.resolveImage(icon, mContext, mMaximumDrawableWidth,
                mMaximumDrawableHeight);
        if (drawable != null) {
            return drawable;
        }

        drawable = icon.loadDrawable(mContext);
        if (drawable != null) {
            return drawable;
        }

        Log.e(TAG, "Couldn't load drawable for icon: " + icon);
        return null;
    }
}
