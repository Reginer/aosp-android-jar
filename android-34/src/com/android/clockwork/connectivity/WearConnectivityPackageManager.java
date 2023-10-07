package com.android.clockwork.connectivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.util.Log;

import com.android.clockwork.common.WearResourceUtil;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Manages packages that will be enabled and run when WiFi or Cellular are enabled. There are two
 * sets of packages - one for WiFi and one for Cellular. Packages can be in only one list or can be
 * in both. Packages in both lists will be enabled if either WiFi or Cellular are enabled.
 */
public class WearConnectivityPackageManager {

    private static final String TAG = "WearConnPkgMgr";
    // Action name of the intent to wake up disabled apps
    @VisibleForTesting
    static final String ACTION_WEAR_APP_ENABLED =
            "com.android.clockwork.connectivity.action.APP_ENABLED";

    private final Context mContext;
    private final ArrayList<String> mWifiEnabledPackages = new ArrayList<>();
    private final ArrayList<String> mCellularEnabledPackages = new ArrayList<>();
    private final ArrayList<String> mWifiAndCellularEnabledPackages;
    private final HashSet<String> mSuppressedCellularRequestors = new HashSet<>();
    private boolean mWifiEnabled;
    private boolean mCellularEnabled;
    private boolean mWifiOrCellEnabled;

    WearConnectivityPackageManager(Context context) {
        mContext = context;
        for (String wifiPackage : WearResourceUtil.getWearableResources(context).getStringArray(
                com.android.wearable.resources.R.array.config_wearWifiEnabledPackages)) {
            mWifiEnabledPackages.add(wifiPackage);
        }
        for (String cellPackage : WearResourceUtil.getWearableResources(context).getStringArray(
                com.android.wearable.resources.R.array.config_wearCellularEnabledPackages)) {
            mCellularEnabledPackages.add(cellPackage);
        }
        mWifiAndCellularEnabledPackages = new ArrayList(mWifiEnabledPackages);
        mWifiAndCellularEnabledPackages.retainAll(mCellularEnabledPackages);
        mWifiEnabledPackages.removeAll(mWifiAndCellularEnabledPackages);
        mCellularEnabledPackages.removeAll(mWifiAndCellularEnabledPackages);

        for (String cellPackage : WearResourceUtil.getWearableResources(context).getStringArray(
                com.android.wearable.resources.R.array.config_wearSuppressedCellularRequestors)) {
            mSuppressedCellularRequestors.add(cellPackage);
        }
    }

    /**
     * Ensures that all Wifi enabled packages are in the same state as the WiFi radio.
     */
    public synchronized void onWifiRadioState(boolean enabled) {
        // Wifi packages are already in the correct state.
        if (mWifiEnabled == enabled) {
            return;
        }
        for (String wifiPackage : getAppOrder(mWifiEnabledPackages, enabled)) {
            setPackageState(wifiPackage, enabled);
        }
        mWifiEnabled = enabled;
        setCommonPackageState();
    }

    /**
     * Ensures that all Cellular enabled packages are in the same state as the Cellular radio.
     */
    public synchronized void onCellularRadioState(boolean enabled) {
        // Cellular packages are already in the correct state.
        if (mCellularEnabled == enabled) {
            return;
        }
        for (String cellularPackage : getAppOrder(mCellularEnabledPackages, enabled)) {
            setPackageState(cellularPackage, enabled);
        }
        mCellularEnabled = enabled;
        setCommonPackageState();
    }

    /** Returns true if cellular network requests are ignored for the given package. */
    public boolean isSuppressedCellularRequestor(String packageName) {
      return mSuppressedCellularRequestors.contains(packageName);
    }

    /** Information printed when a dumpsys is requested. */
    public void dump(IndentingPrintWriter ipw) {
        PackageManager pkgMgr = mContext.getPackageManager();
        ipw.println("======== WearConnectivityPackageManager ========");
        for (String cellularPackage : mCellularEnabledPackages) {
            try {
                ipw.printPair("Cellular package=" + cellularPackage,
                        pkgMgr.getApplicationEnabledSetting(cellularPackage));
                ipw.println();
            } catch (IllegalArgumentException e) {
                ipw.println("Cellular package=" + cellularPackage + " not found");
            }
        }
        for (String wifiPackage : mWifiEnabledPackages) {
            try {
                ipw.printPair("WiFi package=" + wifiPackage,
                        pkgMgr.getApplicationEnabledSetting(wifiPackage));
                ipw.println();
            } catch (IllegalArgumentException e) {
                ipw.println("WiFi package=" + wifiPackage + " not found");
            }
        }
        for (String commonPackage : mWifiAndCellularEnabledPackages) {
            try {
                ipw.printPair("Cellular & WiFi package=" + commonPackage,
                        pkgMgr.getApplicationEnabledSetting(commonPackage));
                ipw.println();
            } catch (IllegalArgumentException e) {
                ipw.println("Cellular & WiFi package package=" + commonPackage + " not found");
            }
        }
        for (String cellularPackage : mSuppressedCellularRequestors) {
            ipw.println("Suppressed cellular requestor=" + cellularPackage);
        }
    }

    /**
     * Applications are enabled in the order they are listed in the configuration overlay.
     * Applications are disabled in reverse order. This ensures that applications with dependencies
     * on each other are started/stopped in the appropriate order.
     */
    private List<String> getAppOrder(List<String> appList, boolean enabled) {
        List<String> orderedAppList = new ArrayList<String>(appList);
        if (!enabled) {
            Collections.reverse(orderedAppList);
        }
        return orderedAppList;
    }

    /**
     * Ensures that all packages contained in both WiFi and Cellular lists are in the proper state:
     *   enabled if either WiFi or Cellular is enabled.
     *   disabled if both WiFi and Cellular are disabled.
     */
    private void setCommonPackageState() {
        final boolean newState = mWifiEnabled || mCellularEnabled;
        if (newState == mWifiOrCellEnabled) {
            return;
        }
        for (String commonPackage : getAppOrder(mWifiAndCellularEnabledPackages, newState)) {
            setPackageState(commonPackage, newState);
        }
        mWifiOrCellEnabled = newState;
    }

    /**
     * Puts the package in the requested enabled or disabled state and sends a new intent to notify
     * package to perform any application specific initialization.
     */
    private void setPackageState(String app, boolean enabled) {
        if (enabled) {
            enableApp(mContext, app);
        } else {
            disableApp(mContext, app);
        }
    }

    private static void disableApp(Context context, String app) {
        PackageManager pkgMgr = context.getPackageManager();
        try {
            if (pkgMgr.getApplicationEnabledSetting(app)
                    == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                Log.d(TAG, app + " already in disabled state, skipping.");
                return;
            }
            Log.d(TAG, "Disabling app: " + app);
            pkgMgr.setApplicationEnabledSetting(app,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, app + " not found, skipping disable.");
        }
    }

    private static void enableApp(Context context, String app) {
        PackageManager pkgMgr = context.getPackageManager();
        try {
            int state = pkgMgr.getApplicationEnabledSetting(app);
            if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
                Log.d(TAG, app + " disabled by user, skipping enable.");
                // TODO: Should COMPONENT_ENABLED_STATE_DEFAULT be used instead?
            } else if (state
                    == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                Log.d(TAG, app + " already enabled, skipping.");
            } else {
                Log.d(TAG, "Enabling app: " + app);
                pkgMgr.setApplicationEnabledSetting(
                        app, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
                Intent intent = new Intent(ACTION_WEAR_APP_ENABLED).setPackage(app);
                context.sendBroadcastAsUser(intent, UserHandle.ALL);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, app + " not found, failed to enable.");
        }
    }
}
