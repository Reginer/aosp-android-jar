/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.server.pm;

import android.annotation.ColorRes;
import android.annotation.DrawableRes;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.StringRes;
import android.content.pm.UserInfo;
import android.content.pm.UserInfo.UserInfoFlag;
import android.content.pm.UserProperties;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserManager;

import com.android.internal.util.Preconditions;
import com.android.server.BundleUtils;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains the details about a multiuser "user type", such as a
 * {@link UserManager#USER_TYPE_PROFILE_MANAGED}.
 *
 * Tests are located in UserManagerServiceUserTypeTest.java.
 * @hide
 */
public final class UserTypeDetails {

    /** Indicates that there is no limit to the number of users allowed. */
    public static final int UNLIMITED_NUMBER_OF_USERS = -1;

    /** Name of the user type, such as {@link UserManager#USER_TYPE_PROFILE_MANAGED}. */
    private final @NonNull String mName;

    /** Whether users of this type can be created. */
    private final boolean mEnabled;

    /**
     * Resource IDs ({@link StringRes}) of the user's labels. This might be used to label a
     * user/profile in tabbed views, etc.
     * The values are resource IDs referring to the strings not the strings themselves.
     *
     * <p>This is an array because, in general, there may be multiple users of the same user type.
     * In this case, the user is indexed according to its {@link UserInfo#profileBadge}.
     */
    private final @Nullable int[] mLabels;

    /**
     * Maximum number of this user type allowed on the device.
     * Use {@link #UNLIMITED_NUMBER_OF_USERS} to indicate that there is no hard limit.
     */
    private final int mMaxAllowed;

    /**
     * Maximum number of this user type allowed per parent (for user types, like profiles, that
     * have parents).
     * Use {@link #UNLIMITED_NUMBER_OF_USERS} to indicate that there is no hard limit.
     */
    // TODO(b/142482943): Should this also apply to restricted profiles?
    private final int mMaxAllowedPerParent;

    // TODO(b/143784345): Update doc when we clean up UserInfo.
    /** The {@link UserInfo.UserInfoFlag} representing the base type of this user. */
    private final @UserInfoFlag int mBaseType;

    // TODO(b/143784345): Update doc/name when we clean up UserInfo.
    /** The {@link UserInfoFlag}s to apply by default to newly created users of this type. */
    private final @UserInfoFlag int mDefaultUserInfoPropertyFlags;

    /**
     * List of User Restrictions to apply by default to newly created users of this type.
     * <p>Does not apply to SYSTEM users (since they are not formally created); for them use
     * {@link com.android.internal.R.array#config_defaultFirstUserRestrictions} instead.
     * The Bundle is of the form used by {@link UserRestrictionsUtils}.
     */
    private final @Nullable Bundle mDefaultRestrictions;

    /**
     * List of {@link android.provider.Settings.System} to apply by default to newly created users
     * of this type.
     */
    private final @Nullable Bundle mDefaultSystemSettings;

    /**
     * List of {@link android.provider.Settings.Secure} to apply by default to newly created users
     * of this type.
     */
    private final @Nullable Bundle mDefaultSecureSettings;

    /**
     * List of {@link DefaultCrossProfileIntentFilter} to allow by default for newly created
     * profiles.
     */
    private final @Nullable List<DefaultCrossProfileIntentFilter> mDefaultCrossProfileIntentFilters;

    /** Whether the {@link #isProfile() profile} requires having a parent (as most profiles do). */
    private final boolean mProfileParentRequired;


    // Fields for profiles only, controlling the nature of their badges.
    // All badge information should be set if {@link #hasBadge()} is true.

    /** Resource ID of the badge put on icons. */
    private @DrawableRes final int mIconBadge;
    /** Resource ID of the badge. Should be set if mIconBadge is set. */
    private @DrawableRes final int mBadgePlain;
    /** Resource ID of the badge without a background. Should be set if mIconBadge is set. */
    private @DrawableRes final int mBadgeNoBackground;

    /** Resource ID of the status bar icon. */
    private @DrawableRes final int mStatusBarIcon;

    /**
     * Resource ID ({@link StringRes}) of the labels to describe badged apps; should be the
     * same format as com.android.internal.R.color.profile_badge_1. These are used for accessibility
     * services.
     *
     * <p>This is an array because, in general, there may be multiple users of the same user type.
     * In this case, the user is indexed according to its {@link UserInfo#profileBadge}.
     *
     * <p>Must be set if mIconBadge is set.
     */
    private final @Nullable int[] mBadgeLabels;

    /**
     * Resource ID ({@link ColorRes}) of the colors badge put on icons.
     * (The value is a resource ID referring to the color; it is not the color value itself).
     *
     * <p>This is an array because, in general, there may be multiple users of the same user type.
     * In this case, the user is indexed according to its {@link UserInfo#profileBadge}.
     *
     * <p>Must be set if mIconBadge is set.
     */
    private final @Nullable int[] mBadgeColors;

    /**
     * Resource ID ({@link ColorRes}) of the colors badge put on icons when in dark theme.
     * (The value is a resource ID referring to the color; it is not the color value itself).
     *
     * <p>This is an array because, in general, there may be multiple users of the same user type.
     * In this case, the user is indexed according to its {@link UserInfo#profileBadge}.
     *
     * <p>Must be set if mIconBadge is set.
     */
    private final @Nullable int[] mDarkThemeBadgeColors;

    /**
     * Resource ID ({@link StringRes}) of the accessibility string that describes the user type.
     * This is used by accessibility services like Talkback.
     */
    private final @StringRes int mAccessibilityString;

    /**
     * The default {@link UserProperties} for the user type.
     * <p> The uninitialized value of each property is implied by {@link UserProperties.Builder}.
     */
    private final @NonNull UserProperties mDefaultUserProperties;

    private UserTypeDetails(@NonNull String name, boolean enabled, int maxAllowed,
            @UserInfoFlag int baseType, @UserInfoFlag int defaultUserInfoPropertyFlags,
            @Nullable int[] labels, int maxAllowedPerParent, boolean profileParentRequired,
            int iconBadge, int badgePlain, int badgeNoBackground,
            int statusBarIcon,
            @Nullable int[] badgeLabels, @Nullable int[] badgeColors,
            @Nullable int[] darkThemeBadgeColors,
            @Nullable Bundle defaultRestrictions,
            @Nullable Bundle defaultSystemSettings,
            @Nullable Bundle defaultSecureSettings,
            @Nullable List<DefaultCrossProfileIntentFilter> defaultCrossProfileIntentFilters,
            @StringRes int accessibilityString,
            @NonNull UserProperties defaultUserProperties) {
        this.mName = name;
        this.mEnabled = enabled;
        this.mMaxAllowed = maxAllowed;
        this.mMaxAllowedPerParent = maxAllowedPerParent;
        this.mBaseType = baseType;
        this.mDefaultUserInfoPropertyFlags = defaultUserInfoPropertyFlags;
        this.mDefaultRestrictions = defaultRestrictions;
        this.mDefaultSystemSettings = defaultSystemSettings;
        this.mDefaultSecureSettings = defaultSecureSettings;
        this.mDefaultCrossProfileIntentFilters = defaultCrossProfileIntentFilters;
        this.mProfileParentRequired = profileParentRequired;
        this.mIconBadge = iconBadge;
        this.mBadgePlain = badgePlain;
        this.mBadgeNoBackground = badgeNoBackground;
        this.mStatusBarIcon = statusBarIcon;
        this.mLabels = labels;
        this.mBadgeLabels = badgeLabels;
        this.mBadgeColors = badgeColors;
        this.mDarkThemeBadgeColors = darkThemeBadgeColors;
        this.mAccessibilityString = accessibilityString;
        this.mDefaultUserProperties = defaultUserProperties;
    }

    /**
     * Returns the name of the user type, such as {@link UserManager#USER_TYPE_PROFILE_MANAGED}.
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns whether this user type is enabled.
     * If it is not enabled, all future attempts to create users of this type will be rejected.
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Returns the maximum number of this user type allowed on the device.
     * <p>Returns {@link #UNLIMITED_NUMBER_OF_USERS} to indicate that there is no hard limit.
     */
    public int getMaxAllowed() {
        return mMaxAllowed;
    }

    /**
     * Returns the maximum number of this user type allowed per parent (for user types, like
     * profiles, that have parents).
     * Under certain circumstances (such as after a change-user-type) the max value can actually
     * be exceeded: this is allowed in order to keep the device in a usable state.
     * An error is logged in {@link UserManagerService#upgradeProfileToTypeLU}
     * <p>Returns {@link #UNLIMITED_NUMBER_OF_USERS} to indicate that there is no hard limit.
     */
    public int getMaxAllowedPerParent() {
        return mMaxAllowedPerParent;
    }

    // TODO(b/143784345): Update comment when UserInfo is reorganized.
    /** The {@link UserInfoFlag}s to apply by default to newly created users of this type. */
    public int getDefaultUserInfoFlags() {
        return mDefaultUserInfoPropertyFlags | mBaseType;
    }

    /**
     * Returns whether this type of {@link #isProfile() profile} requires having a parent.
     * Inapplicable to non-profiles (and therefore inapplicable to restricted profiles).
     */
    public boolean isProfileParentRequired() {
        return mProfileParentRequired;
    }

    /**
     * Returns the resource ID corresponding to the badgeIndexth label name where the badgeIndex is
     * expected to be the {@link UserInfo#profileBadge} of the user. If badgeIndex exceeds the
     * number of labels, returns the label for the highest index.
     */
    public @StringRes int getLabel(int badgeIndex) {
        if (mLabels == null || mLabels.length == 0 || badgeIndex < 0) {
            return Resources.ID_NULL;
        }
        return mLabels[Math.min(badgeIndex, mLabels.length - 1)];
    }

    /** Returns whether users of this user type should be badged. */
    public boolean hasBadge() {
        return mIconBadge != Resources.ID_NULL;
    }

    /** Resource ID of the badge to put on icons. */
    public @DrawableRes int getIconBadge() {
        return mIconBadge;
    }

    /** Resource ID of the badge. Used for {@link UserManager#getUserBadgeResId(int)}. */
    public @DrawableRes int getBadgePlain() {
        return mBadgePlain;
    }

    /** Resource ID of the badge without a background. */
    public @DrawableRes int getBadgeNoBackground() {
        return mBadgeNoBackground;
    }

    /** Resource ID of the status bar icon. */
    public @DrawableRes int getStatusBarIcon() {
        return mStatusBarIcon;
    }

    /**
     * Returns the Resource ID of the badgeIndexth badge label, where the badgeIndex is expected
     * to be the {@link UserInfo#profileBadge} of the user.
     * If badgeIndex exceeds the number of labels, returns the label for the highest index.
     */
    public @StringRes int getBadgeLabel(int badgeIndex) {
        if (mBadgeLabels == null || mBadgeLabels.length == 0 || badgeIndex < 0) {
            return Resources.ID_NULL;
        }
        return mBadgeLabels[Math.min(badgeIndex, mBadgeLabels.length - 1)];
    }

    /**
     * Returns the Resource ID of the badgeIndexth badge color, where the badgeIndex is expected
     * to be the {@link UserInfo#profileBadge} of the user.
     * If badgeIndex exceeds the number of colors, returns the color for the highest index.
     */
    public @ColorRes int getBadgeColor(int badgeIndex) {
        if (mBadgeColors == null || mBadgeColors.length == 0 || badgeIndex < 0) {
            return Resources.ID_NULL;
        }
        return mBadgeColors[Math.min(badgeIndex, mBadgeColors.length - 1)];
    }

    /**
     * Returns the Resource ID of the badgeIndexth dark theme badge color, where the badgeIndex is
     * expected to be the {@link UserInfo#profileBadge} of the user.
     * If dark theme badge colors haven't been set, use the light theme badge color.
     * If badgeIndex exceeds the number of colors, returns the color for the highest index.
     */
    public @ColorRes int getDarkThemeBadgeColor(int badgeIndex) {
        if (mDarkThemeBadgeColors == null || mDarkThemeBadgeColors.length == 0 || badgeIndex < 0) {
            return getBadgeColor(badgeIndex);
        }
        return mDarkThemeBadgeColors[Math.min(badgeIndex, mDarkThemeBadgeColors.length - 1)];
    }


    /**
     * Returns the reference to the default {@link UserProperties} for this type of user.
     * This is not a copy. Do NOT modify this object.
     */
    public @NonNull UserProperties getDefaultUserPropertiesReference() {
        return mDefaultUserProperties;
    }

    public @StringRes int getAccessibilityString() {
        return mAccessibilityString;
    }

    public boolean isProfile() {
        return (mBaseType & UserInfo.FLAG_PROFILE) != 0;
    }

    public boolean isFull() {
        return (mBaseType & UserInfo.FLAG_FULL) != 0;
    }

    public boolean isSystem() {
        return (mBaseType & UserInfo.FLAG_SYSTEM) != 0;
    }

    /** Returns a {@link Bundle} representing the default user restrictions. */
    @NonNull Bundle getDefaultRestrictions() {
        return BundleUtils.clone(mDefaultRestrictions);
    }

    /** Adds the default user restrictions to the given bundle of restrictions. */
    public void addDefaultRestrictionsTo(@NonNull Bundle currentRestrictions) {
        UserRestrictionsUtils.merge(currentRestrictions, mDefaultRestrictions);
    }

    /** Returns a {@link Bundle} representing the default system settings. */
    @NonNull Bundle getDefaultSystemSettings() {
        return BundleUtils.clone(mDefaultSystemSettings);
    }

    /** Returns a {@link Bundle} representing the default secure settings. */
    @NonNull Bundle getDefaultSecureSettings() {
        return BundleUtils.clone(mDefaultSecureSettings);
    }

    /** Returns a list of default cross profile intent filters. */
    @NonNull List<DefaultCrossProfileIntentFilter> getDefaultCrossProfileIntentFilters() {
        return mDefaultCrossProfileIntentFilters != null
                ? new ArrayList<>(mDefaultCrossProfileIntentFilters)
                : Collections.emptyList();
    }

    /** Dumps details of the UserTypeDetails. Do not parse this. */
    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix); pw.print("mName: "); pw.println(mName);
        pw.print(prefix); pw.print("mBaseType: "); pw.println(UserInfo.flagsToString(mBaseType));
        pw.print(prefix); pw.print("mEnabled: "); pw.println(mEnabled);
        pw.print(prefix); pw.print("mMaxAllowed: "); pw.println(mMaxAllowed);
        pw.print(prefix); pw.print("mMaxAllowedPerParent: "); pw.println(mMaxAllowedPerParent);
        pw.print(prefix); pw.print("mDefaultUserInfoFlags: ");
        pw.println(UserInfo.flagsToString(mDefaultUserInfoPropertyFlags));
        mDefaultUserProperties.println(pw, prefix);

        final String restrictionsPrefix = prefix + "    ";
        if (isSystem()) {
            pw.print(prefix); pw.println("config_defaultFirstUserRestrictions: ");
            try {
                final Bundle restrictions = new Bundle();
                final String[] defaultFirstUserRestrictions = Resources.getSystem().getStringArray(
                        com.android.internal.R.array.config_defaultFirstUserRestrictions);
                for (String userRestriction : defaultFirstUserRestrictions) {
                    if (UserRestrictionsUtils.isValidRestriction(userRestriction)) {
                        restrictions.putBoolean(userRestriction, true);
                    }
                }
                UserRestrictionsUtils.dumpRestrictions(pw, restrictionsPrefix, restrictions);
            } catch (Resources.NotFoundException e) {
                pw.print(restrictionsPrefix); pw.println("none - resource not found");
            }
        } else {
            pw.print(prefix); pw.println("mDefaultRestrictions: ");
            UserRestrictionsUtils.dumpRestrictions(pw, restrictionsPrefix, mDefaultRestrictions);
        }

        pw.print(prefix); pw.print("mProfileParentRequired: "); pw.println(mProfileParentRequired);
        pw.print(prefix); pw.print("mIconBadge: "); pw.println(mIconBadge);
        pw.print(prefix); pw.print("mBadgePlain: "); pw.println(mBadgePlain);
        pw.print(prefix); pw.print("mBadgeNoBackground: "); pw.println(mBadgeNoBackground);
        pw.print(prefix); pw.print("mStatusBarIcon: "); pw.println(mStatusBarIcon);
        pw.print(prefix); pw.print("mBadgeLabels.length: ");
        pw.println(mBadgeLabels != null ? mBadgeLabels.length : "0(null)");
        pw.print(prefix); pw.print("mBadgeColors.length: ");
        pw.println(mBadgeColors != null ? mBadgeColors.length : "0(null)");
        pw.print(prefix); pw.print("mDarkThemeBadgeColors.length: ");
        pw.println(mDarkThemeBadgeColors != null ? mDarkThemeBadgeColors.length : "0(null)");
        pw.print(prefix); pw.print("mLabels.length: ");
        pw.println(mLabels != null ? mLabels.length : "0(null)");
    }

    /** Builder for a {@link UserTypeDetails}; see that class for documentation. */
    public static final class Builder {
        // UserTypeDetails properties and their default values.
        private String mName; // This MUST be explicitly set.
        private int mBaseType; // This MUST be explicitly set.
        private int mMaxAllowed = UNLIMITED_NUMBER_OF_USERS;
        private int mMaxAllowedPerParent = UNLIMITED_NUMBER_OF_USERS;
        private int mDefaultUserInfoPropertyFlags = 0;
        private @Nullable Bundle mDefaultRestrictions = null;
        private @Nullable Bundle mDefaultSystemSettings = null;
        private @Nullable Bundle mDefaultSecureSettings = null;
        private @Nullable List<DefaultCrossProfileIntentFilter> mDefaultCrossProfileIntentFilters =
                null;
        private int mEnabled = 1;
        private @Nullable int[] mLabels = null;
        private @Nullable int[] mBadgeLabels = null;
        private @Nullable int[] mBadgeColors = null;
        private @Nullable int[] mDarkThemeBadgeColors = null;
        private boolean mProfileParentRequired = false;
        private @DrawableRes int mIconBadge = Resources.ID_NULL;
        private @DrawableRes int mBadgePlain = Resources.ID_NULL;
        private @DrawableRes int mBadgeNoBackground = Resources.ID_NULL;
        private @DrawableRes int mStatusBarIcon = Resources.ID_NULL;
        private @StringRes int mAccessibilityString = Resources.ID_NULL;
        // Default UserProperties cannot be null but for efficiency we don't initialize it now.
        // If it isn't set explicitly, {@link UserProperties.Builder#build()} will be used.
        private @Nullable UserProperties mDefaultUserProperties = null;

        public Builder setName(String name) {
            mName = name;
            return this;
        }

        public Builder setEnabled(int enabled) {
            mEnabled = enabled;
            return this;
        }

        public Builder setMaxAllowed(int maxAllowed) {
            mMaxAllowed = maxAllowed;
            return this;
        }

        public Builder setMaxAllowedPerParent(int maxAllowedPerParent) {
            mMaxAllowedPerParent = maxAllowedPerParent;
            return this;
        }

        public Builder setBaseType(@UserInfoFlag int baseType) {
            mBaseType = baseType;
            return this;
        }

        public Builder setDefaultUserInfoPropertyFlags(@UserInfoFlag int flags) {
            mDefaultUserInfoPropertyFlags = flags;
            return this;
        }

        public Builder setBadgeLabels(@StringRes int ... badgeLabels) {
            mBadgeLabels = badgeLabels;
            return this;
        }

        public Builder setBadgeColors(@ColorRes int ... badgeColors) {
            mBadgeColors = badgeColors;
            return this;
        }

        /** The badge colors when the badge is on a dark background. */
        public Builder setDarkThemeBadgeColors(@ColorRes int ... darkThemeBadgeColors) {
            mDarkThemeBadgeColors = darkThemeBadgeColors;
            return this;
        }

        public Builder setProfileParentRequired(boolean profileParentRequired) {
            mProfileParentRequired = profileParentRequired;
            return this;
        }

        public Builder setIconBadge(@DrawableRes int badgeIcon) {
            mIconBadge = badgeIcon;
            return this;
        }

        public Builder setBadgePlain(@DrawableRes int badgePlain) {
            mBadgePlain = badgePlain;
            return this;
        }

        public Builder setBadgeNoBackground(@DrawableRes int badgeNoBackground) {
            mBadgeNoBackground = badgeNoBackground;
            return this;
        }

        public Builder setStatusBarIcon(@DrawableRes int statusBarIcon) {
            mStatusBarIcon = statusBarIcon;
            return this;
        }

        public Builder setLabels(@StringRes int ... labels) {
            mLabels = labels;
            return this;
        }

        public Builder setDefaultRestrictions(@Nullable Bundle restrictions) {
            mDefaultRestrictions = restrictions;
            return this;
        }

        public Builder setDefaultSystemSettings(@Nullable Bundle settings) {
            mDefaultSystemSettings = settings;
            return this;
        }

        public Builder setDefaultSecureSettings(@Nullable Bundle settings) {
            mDefaultSecureSettings = settings;
            return this;
        }

        public Builder setDefaultCrossProfileIntentFilters(
                @Nullable List<DefaultCrossProfileIntentFilter> intentFilters) {
            mDefaultCrossProfileIntentFilters = intentFilters;
            return this;
        }

        /** Sets the accessibility label associated with the user */
        public Builder setAccessibilityString(@StringRes int accessibilityString) {
            mAccessibilityString = accessibilityString;
            return this;
        }

        /**
         * Sets (replacing if necessary) the default UserProperties object for this user type.
         * Takes a builder, rather than a built object, to efficiently ensure that a fresh copy of
         * properties is stored (since it later might be modified by UserProperties#updateFromXml).
         */
        public Builder setDefaultUserProperties(UserProperties.Builder userPropertiesBuilder) {
            mDefaultUserProperties = userPropertiesBuilder.build();
            return this;
        }

        public @NonNull UserProperties getDefaultUserProperties() {
            if (mDefaultUserProperties == null) {
                mDefaultUserProperties = new UserProperties.Builder().build();
            }
            return mDefaultUserProperties;
        }

        @UserInfoFlag int getBaseType() {
            return mBaseType;
        }

        public UserTypeDetails createUserTypeDetails() {
            Preconditions.checkArgument(mName != null,
                    "Cannot create a UserTypeDetails with no name.");
            Preconditions.checkArgument(hasValidBaseType(),
                    "UserTypeDetails " + mName + " has invalid baseType: " + mBaseType);
            Preconditions.checkArgument(hasValidPropertyFlags(),
                    "UserTypeDetails " + mName + " has invalid flags: "
                            + Integer.toHexString(mDefaultUserInfoPropertyFlags));
            checkSystemAndMainUserPreconditions();
            if (hasBadge()) {
                Preconditions.checkArgument(mBadgeLabels != null && mBadgeLabels.length != 0,
                        "UserTypeDetails " + mName + " has badge but no badgeLabels.");
                Preconditions.checkArgument(mBadgeColors != null && mBadgeColors.length != 0,
                        "UserTypeDetails " + mName + " has badge but no badgeColors.");
            }
            if (!isProfile()) {
                Preconditions.checkArgument(mDefaultCrossProfileIntentFilters == null
                                || mDefaultCrossProfileIntentFilters.isEmpty(),
                        "UserTypeDetails %s has a non empty "
                                + "defaultCrossProfileIntentFilters", mName);
                Preconditions.checkArgument(!mProfileParentRequired,
                        "UserTypeDetails %s requires a parent but isn't a profile", mName);
            }
            return new UserTypeDetails(
                    mName,
                    mEnabled != 0,
                    mMaxAllowed,
                    mBaseType,
                    mDefaultUserInfoPropertyFlags,
                    mLabels,
                    mMaxAllowedPerParent,
                    mProfileParentRequired,
                    mIconBadge,
                    mBadgePlain,
                    mBadgeNoBackground,
                    mStatusBarIcon,
                    mBadgeLabels,
                    mBadgeColors,
                    mDarkThemeBadgeColors == null ? mBadgeColors : mDarkThemeBadgeColors,
                    mDefaultRestrictions,
                    mDefaultSystemSettings,
                    mDefaultSecureSettings,
                    mDefaultCrossProfileIntentFilters,
                    mAccessibilityString,
                    getDefaultUserProperties());
        }

        private boolean hasBadge() {
            return mIconBadge != Resources.ID_NULL;
        }

        private boolean isProfile() {
            return (mBaseType & UserInfo.FLAG_PROFILE) != 0;
        }

        // TODO(b/143784345): Refactor this when we clean up UserInfo.
        private boolean hasValidBaseType() {
            return mBaseType == UserInfo.FLAG_FULL
                    || mBaseType == UserInfo.FLAG_PROFILE
                    || mBaseType == UserInfo.FLAG_SYSTEM
                    || mBaseType == (UserInfo.FLAG_FULL | UserInfo.FLAG_SYSTEM);
        }

        // TODO(b/143784345): Refactor this when we clean up UserInfo.
        private boolean hasValidPropertyFlags() {
            final int forbiddenMask =
                    UserInfo.FLAG_INITIALIZED |
                    UserInfo.FLAG_QUIET_MODE |
                    UserInfo.FLAG_FULL |
                    UserInfo.FLAG_SYSTEM |
                    UserInfo.FLAG_PROFILE;
            return (mDefaultUserInfoPropertyFlags & forbiddenMask) == 0;
        }

        private void checkSystemAndMainUserPreconditions() {
            // Primary must be synonymous with System.
            Preconditions.checkArgument(
                    ((mBaseType & UserInfo.FLAG_SYSTEM) != 0) ==
                            ((mDefaultUserInfoPropertyFlags & UserInfo.FLAG_PRIMARY) != 0),
                    "UserTypeDetails " + mName + " cannot be SYSTEM xor PRIMARY.");
            // At most one MainUser is ever allowed at a time.
            Preconditions.checkArgument(
                    ((mDefaultUserInfoPropertyFlags & UserInfo.FLAG_MAIN) == 0) || mMaxAllowed == 1,
                    "UserTypeDetails " + mName + " must not sanction more than one MainUser.");
        }
    }

    /**
     * Returns whether the user type is a managed profile
     * (i.e. {@link UserManager#USER_TYPE_PROFILE_MANAGED}).
     */
    public boolean isManagedProfile() {
        return UserManager.isUserTypeManagedProfile(mName);
    }

    /**
     * Returns whether the user type is a communal profile
     * (i.e. {@link UserManager#USER_TYPE_PROFILE_COMMUNAL}).
     */
    public boolean isCommunalProfile() {
        return UserManager.isUserTypeCommunalProfile(mName);
    }

    /**
     * Returns whether the user type is a private profile
     * (i.e. {@link UserManager#USER_TYPE_PROFILE_PRIVATE}).
     */
    public boolean isPrivateProfile() {
        return UserManager.isUserTypePrivateProfile(mName);
    }
}
