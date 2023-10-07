package com.android.clockwork.setup;

import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE;
import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE_ANDROID;
import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE_IOS;
import static android.provider.Settings.Global.Wearable.PAIRED_DEVICE_OS_TYPE_UNKNOWN;

import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;

import com.android.clockwork.common.WearResourceUtil;
import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class to assist with the selective configuration of packages/components.
 * Packages/components can be configured for selective enablement/disablement based on their
 * paired-state, using overlay values for config_postSetupPackageConfigList (string array).
 *
 * Each configuration in the list is of the form:
 *
 * <package_name>:<config_value>
 *
 * Where valid config values are:
 *
 * 0 = disable app for all users
 * 1 = disable app for Android users
 * 2 = disable app for iOS users
 * 3 = disable launchers for all users
 * 4 = disable launchers for Android users
 * 5 = disable launchers for iOS users
 *
 * 100 = enable app for all users
 * 101 = enable launcher for all users
 *
 * e.g. -
 *
 * disable maps for iOS users
 * <item>com.google.android.apps.maps:2</item>
 *
 * disable any launcher apps (those that would appear in the launcher) for dialer for Android
 * <item>com.google.android.dialer:1</item>
 *
 * Note then when enabling/disabling an app, the configuration will be applied to the entire
 * package based on the name provided. When enabling/disabling a launcher component, the
 * configuration will be applied to any "launcher apps" associated with the provided package (i.e.
 * those activities which specify android.intent.action.MAIN and android.intent.category.LAUNCHER in
 * the manifest).
 *
 * By convention, higher value configs are applied first thereby ensuring that ordering is
 * deterministic (i.e. by reserving higher order for enabling for example, we can ensure those
 * configurations are applied first - so in a potential scenario where a package is disabled for
 * only iOS but must subsequently be disabled for only Android, this could be achieved by including
 * two configs for the same package - one to enable for all users [100] and one to disable for
 * Android users [1], ensuring a known end state for every run).
 *
 */
public class PostSetupPackageHelper {
    private static final String TAG = "PostSetupPackageHelper";

    private static final String PACKAGE_CONFIG_DELIM = ":";

    public static final int DISABLE_APP_FOR_ALL = 0;
    public static final int DISABLE_APP_FOR_ANDROID = 1;
    public static final int DISABLE_APP_FOR_IOS = 2;

    public static final int DISABLE_LAUNCHER_FOR_ALL = 3;
    public static final int DISABLE_LAUNCHER_FOR_ANDROID = 4;
    public static final int DISABLE_LAUNCHER_FOR_IOS = 5;

    // higher order config values have precedence (will be applied first)
    public static final int ENABLE_APP_FOR_ALL = 100;
    public static final int ENABLE_LAUNCHER_FOR_ALL = 101;

    // Internal class for Package Configurations, comprising both a package name and configuration
    // value, to be applied via package manager (on either a package or component level).
    class PackageConfig implements Comparable {
        String mPackageName;
        int mConfigValue = -1;

        PackageConfig(String config) {
            String[] parts = config.split(PACKAGE_CONFIG_DELIM);

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "New package config: " + config);
            }
            if (parts.length != 2) {
                Log.e(TAG, "Invalid package config: " + config
                        + " - expected format <package name>:<config value>");
                return;
            }

            mPackageName = parts[0];
            try {
                mConfigValue = Integer.valueOf(parts[1]);
            } catch (NumberFormatException nfe) {
                Log.e(TAG, "Invalid package config: " + config, nfe);
            }
        }

        public boolean isValid() {
            return mPackageName != null && mConfigValue >= 0;
        }

        @Override
        public int compareTo(Object o) {
            if (this.isValid() && (o instanceof PackageConfig && ((PackageConfig) o).isValid())) {
                return ((PackageConfig) o).mConfigValue - this.mConfigValue;
            }
            return 0;
        }

        @Override
        public String toString() {
            return ((mPackageName != null) ? mPackageName : "")
                    + PACKAGE_CONFIG_DELIM + mConfigValue;
        }
    }

    private final Context mContext;
    private final int mPairedDeviceOSType;
    private final String[] mConfigs;

    public PostSetupPackageHelper(Context context) {
        this(context, WearResourceUtil.getWearableResources(context));
    }

    @VisibleForTesting
    PostSetupPackageHelper(Context context, @Nullable Resources resources) {
        mContext = context;
        mConfigs = getPackageConfigs(resources);
        mPairedDeviceOSType = Settings.Global.getInt(
                context.getContentResolver(), PAIRED_DEVICE_OS_TYPE, PAIRED_DEVICE_OS_TYPE_UNKNOWN);
    }

    /**
     * Run the post setup package helper, resulting in the selective
     * disabling of any packages specified using the config list.
     */
    public void run() {
        Arrays.stream(mConfigs).map(PackageConfig::new).sorted().forEach(this::processConfig);
    }

    public static String[] getPackageConfigs(@Nullable Resources resources) {
        if (resources == null) {
            return new String[] {};
        }

        try {
            return resources.getStringArray(
                    com.android.wearable.resources.R.array.config_postSetupPackageConfigList);
        } catch (Resources.NotFoundException nfe) {
            return new String[] {};
        }
    }

    private void processConfig(PackageConfig packageConfig) {
        if (!packageConfig.isValid()) {
            Log.w(TAG, "Skipping invalid config: " + packageConfig);
            return;
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Applying package config: " + packageConfig);
            }
        }

        switch (packageConfig.mConfigValue) {
            case DISABLE_APP_FOR_ALL:
                if (mPairedDeviceOSType != PAIRED_DEVICE_OS_TYPE_UNKNOWN) {
                    updatePackageIfExists(packageConfig.mPackageName, false);
                }
                break;
            case DISABLE_APP_FOR_ANDROID:
                if (mPairedDeviceOSType == PAIRED_DEVICE_OS_TYPE_ANDROID) {
                    updatePackageIfExists(packageConfig.mPackageName, false);
                }
                break;
            case DISABLE_APP_FOR_IOS:
                if (mPairedDeviceOSType == PAIRED_DEVICE_OS_TYPE_IOS) {
                    updatePackageIfExists(packageConfig.mPackageName, false);
                }
                break;
            case ENABLE_APP_FOR_ALL:
                if (mPairedDeviceOSType != PAIRED_DEVICE_OS_TYPE_UNKNOWN) {
                    updatePackageIfExists(packageConfig.mPackageName, true);
                }
                break;
            case DISABLE_LAUNCHER_FOR_ALL:
                if (mPairedDeviceOSType != PAIRED_DEVICE_OS_TYPE_UNKNOWN) {
                    updateLauncherIfExists(packageConfig.mPackageName, false);
                }
                break;
            case DISABLE_LAUNCHER_FOR_ANDROID:
                if (mPairedDeviceOSType == PAIRED_DEVICE_OS_TYPE_ANDROID) {
                    updateLauncherIfExists(packageConfig.mPackageName, false);
                }
                break;
            case DISABLE_LAUNCHER_FOR_IOS:
                if (mPairedDeviceOSType == PAIRED_DEVICE_OS_TYPE_IOS) {
                    updateLauncherIfExists(packageConfig.mPackageName, false);
                }
                break;
            case ENABLE_LAUNCHER_FOR_ALL:
                if (mPairedDeviceOSType != PAIRED_DEVICE_OS_TYPE_UNKNOWN) {
                    updateLauncherIfExists(packageConfig.mPackageName, true);
                }
                break;
            default:
                Log.w(TAG, "Skipping invalid package config: " + packageConfig
                        + " - unrecognized config value: " + packageConfig.mConfigValue);
                break;
        }
    }

    private void updatePackageIfExists(String pkgName, boolean enable) {
        PackageManager manager = mContext.getPackageManager();

        try {
            manager.getPackageInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to get package info for : " + pkgName, e);
            return;
        }

        Log.i(TAG, (enable ? "Enabling" : "Disabling") + " package: " + pkgName);

        manager.setApplicationEnabledSetting(
                pkgName,
                enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                0);
    }

    private void updateLauncherIfExists(String pkgName, boolean enable) {
        PackageManager manager = mContext.getApplicationContext().getPackageManager();
        try {
            manager.getPackageInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to get package info for : " + pkgName, e);
            return;
        }

        // Component name(s) matching launcher activities
        List<ComponentName> componentNames = new ArrayList();

        Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(pkgName);
        final int flags = (enable) ? (PackageManager.MATCH_DISABLED_COMPONENTS) : 0;
        List<ResolveInfo> resolveList = manager.queryIntentActivities(intent, flags);
        ActivityInfo activityInfo = null;

        for (ResolveInfo resolveInfo : resolveList) {

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Checking resolve info: " + resolveInfo.toString());
            }
            activityInfo = resolveInfo.activityInfo;
            if (activityInfo == null) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "skipping resolve - no activity info");
                }
                continue;
            }

            final ComponentName launcherComponent = activityInfo.getComponentName();
            if (launcherComponent != null) {
                componentNames.add(launcherComponent);
            }
        }

        for (ComponentName componentName: componentNames) {
            Log.i(TAG, (enable ? "Enabling" : "Disabling") + " component: "
                    + componentName.flattenToShortString());

            manager.setComponentEnabledSetting(
                    componentName,
                    enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    0);
        }
    }
}