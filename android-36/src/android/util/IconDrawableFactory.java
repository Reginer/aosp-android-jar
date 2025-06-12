/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.util;

import static android.app.admin.DevicePolicyResources.Drawables.Style.SOLID_COLORED;
import static android.app.admin.DevicePolicyResources.Drawables.WORK_PROFILE_ICON_BADGE;
import static android.app.admin.DevicePolicyResources.UNDEFINED;

import android.annotation.UserIdInt;
import android.app.admin.DevicePolicyManager;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;

/**
 * Utility class to load app drawables with appropriate badging.
 *
 * @hide
 */
public class IconDrawableFactory {

    protected final Context mContext;
    protected final PackageManager mPm;
    protected final UserManager mUm;
    protected final DevicePolicyManager mDpm;
    protected final LauncherIcons mLauncherIcons;
    protected final boolean mEmbedShadow;

    private IconDrawableFactory(Context context, boolean embedShadow) {
        mContext = context;
        mPm = context.getPackageManager();
        mUm = context.getSystemService(UserManager.class);
        mDpm = context.getSystemService(DevicePolicyManager.class);
        mLauncherIcons = new LauncherIcons(context);
        mEmbedShadow = embedShadow;
    }

    protected boolean needsBadging(ApplicationInfo appInfo, @UserIdInt int userId) {
        return appInfo.isInstantApp() || mUm.hasBadge(userId);
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public Drawable getBadgedIcon(ApplicationInfo appInfo) {
        return getBadgedIcon(appInfo, UserHandle.getUserId(appInfo.uid));
    }

    public Drawable getBadgedIcon(ApplicationInfo appInfo, @UserIdInt int userId) {
        return getBadgedIcon(appInfo, appInfo, userId);
    }

    @UnsupportedAppUsage
    public Drawable getBadgedIcon(PackageItemInfo itemInfo, ApplicationInfo appInfo,
            @UserIdInt int userId) {
        Drawable icon = mPm.loadUnbadgedItemIcon(itemInfo, appInfo);
        if (!mEmbedShadow && !needsBadging(appInfo, userId)) {
            return icon;
        }

        icon = getShadowedIcon(icon);
        if (appInfo.isInstantApp()) {
            int badgeColor = Resources.getSystem().getColor(
                    com.android.internal.R.color.instant_app_badge, null);
            Drawable badge = mContext.getDrawable(
                    com.android.internal.R.drawable.ic_instant_icon_badge_bolt);
            icon = mLauncherIcons.getBadgedDrawable(icon,
                    badge,
                    badgeColor);
        }
        if (mUm.hasBadge(userId)) {

            Drawable badge = mDpm.getResources().getDrawable(
                    getUpdatableUserIconBadgeId(userId),
                    SOLID_COLORED,
                    () -> getDefaultUserIconBadge(userId));

            icon = mLauncherIcons.getBadgedDrawable(icon, badge, mUm.getUserBadgeColor(userId));
        }
        return icon;
    }

    private String getUpdatableUserIconBadgeId(int userId) {
        return mUm.isManagedProfile(userId) ? WORK_PROFILE_ICON_BADGE : UNDEFINED;
    }

    private Drawable getDefaultUserIconBadge(int userId) {
        return mContext.getResources().getDrawable(mUm.getUserIconBadgeResId(userId));
    }

    /**
     * Add shadow to the icon if {@link AdaptiveIconDrawable}
     */
    public Drawable getShadowedIcon(Drawable icon) {
        return mLauncherIcons.wrapIconDrawableWithShadow(icon);
    }

    @UnsupportedAppUsage
    public static IconDrawableFactory newInstance(Context context) {
        return new IconDrawableFactory(context, true);
    }

    public static IconDrawableFactory newInstance(Context context, boolean embedShadow) {
        return new IconDrawableFactory(context, embedShadow);
    }
}
