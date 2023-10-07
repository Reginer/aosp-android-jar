package com.android.clockwork.connectivity;

import static com.android.clockwork.common.ThermalEmergencyTracker.ThermalEmergencyMode;
import static com.android.clockwork.connectivity.WearWifiNetworkAgent.WifiConnectivityStatus;

import android.annotation.Nullable;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkPolicyManager;

import com.android.clockwork.bluetooth.WearBluetoothMediator;
import com.android.clockwork.cellular.WearCellularMediator;
import com.android.clockwork.common.ActivityModeTracker;
import com.android.clockwork.common.CellOnlyMode;
import com.android.clockwork.common.LogUtil;
import com.android.clockwork.common.ProxyConnectivityDebounce;
import com.android.clockwork.common.ProxyConnectivityDebounce.ProxyStatus;
import com.android.clockwork.common.ThermalEmergencyTracker;
import com.android.clockwork.common.WearResourceUtil;
import com.android.clockwork.wifi.WearWifiMediator;
import com.android.internal.util.IndentingPrintWriter;

import java.util.concurrent.TimeUnit;

/**
 * WearConnectivityController routes inputs and signals from various sources and relays the
 * appropriate info to the respective WiFi/Cellular Mediators.
 *
 * <p>The WifiMediator is expected to always exist. The BtMediator and CellMediator may be null.
 */
public class WearConnectivityController
        implements ActivityModeTracker.Listener,
                CellOnlyMode.Listener,
                ThermalEmergencyTracker.Listener,
                WearNetworkObserver.Listener,
                WearWifiNetworkAgent.Listener {

    static final String TAG = "WearConnectivityService";
    static final String ACTION_PROXY_STATUS_CHANGE =
            "com.android.clockwork.connectivity.action.PROXY_STATUS_CHANGE";

    private static final long MAX_ACCEPTABLE_DELAY_MS = TimeUnit.SECONDS.toMillis(60);

    // dependencies
    private final Context mContext;
    private final AlarmManager mAlarmManager;
    @Nullable private final WearBluetoothMediator mBtMediator;
    @Nullable private final WearCellularMediator mCellMediator;
    @Nullable private final WearWifiMediator mWifiMediator;
    private final WearConnectivityPackageManager mWearConnectivityPackageManager;
    private final WearProxyNetworkAgent mProxyNetworkAgent;
    private final ActivityModeTracker mActivityModeTracker;
    private final ThermalEmergencyTracker mThermalEmergencyTracker;
    private final CellOnlyMode mCellOnlyMode;
    private final ProxyConnectivityDebounce mWifiDebounce;
    private final ProxyConnectivityDebounce mCellDebounce;
    private final WearWifiNetworkAgent mWearWifiNetworkAgent;

    // state
    private int mNumWifiRequests = 0;
    private int mNumCellularRequests;
    private int mNumHighBandwidthRequests = 0;
    private int mNumUnmeteredRequests = 0;
    private long mExtendCellDelay = 0;

    // Receiver of first user unlock, exit form direct boot mode
    private final BroadcastReceiver mUnlockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context mContext, Intent intent) {
            LogUtil.logD(TAG, "unlockReceiver %s", intent.getAction());
            mContext.unregisterReceiver(mUnlockReceiver);
            if (mBtMediator != null) {
                mBtMediator.onUserUnlocked();
            }
            if (mWifiMediator != null) {
                mWifiMediator.onUserUnlocked();
            }
            if (mCellMediator != null) {
                mCellMediator.onUserUnlocked();
            }
        }
    };

    public WearConnectivityController(
            Context context,
            AlarmManager alarmManager,
            WearBluetoothMediator btMediator,
            WearWifiMediator wifiMediator,
            WearCellularMediator cellMediator,
            WearConnectivityPackageManager wearConnectivityPackageManager,
            WearProxyNetworkAgent proxyNetworkAgent,
            ActivityModeTracker activityModeTracker,
            CellOnlyMode cellOnlyMode,
            ThermalEmergencyTracker thermalEmergencyTracker) {
        mContext = context;
        mAlarmManager = alarmManager;
        mBtMediator = btMediator;
        mWifiMediator = wifiMediator;
        mCellMediator = cellMediator;
        mWearConnectivityPackageManager = wearConnectivityPackageManager;
        mProxyNetworkAgent = proxyNetworkAgent;
        mActivityModeTracker = activityModeTracker;
        mActivityModeTracker.addListener(this);
        mCellOnlyMode = cellOnlyMode;
        mCellOnlyMode.addListener(this);
        mThermalEmergencyTracker = thermalEmergencyTracker;
        mThermalEmergencyTracker.addListener(this);
        mBtMediator.setProxyStatusListener(this::notifyProxyStatusChange);
        long delayWifi = WearResourceUtil.getWearableResources(context).getInteger(
                com.android.wearable.resources.R.integer.proxy_connectivity_delay_wifi);
        mWifiDebounce = new ProxyConnectivityDebounce("wifi", alarmManager, wifiMediator,
                delayWifi);
        long delayCell = WearResourceUtil.getWearableResources(context).getInteger(
                com.android.wearable.resources.R.integer.proxy_connectivity_delay_cell);
        mCellDebounce = new ProxyConnectivityDebounce("cell", alarmManager, cellMediator,
                delayCell);
        mExtendCellDelay =
                WearResourceUtil.getWearableResources(context)
                        .getInteger(
                                com.android.wearable.resources.R.integer
                                        .wifi_connectivity_extend_cell_delay);
        mWearWifiNetworkAgent = new WearWifiNetworkAgent(this);
        mWearWifiNetworkAgent.register(context);
    }

    public void onBootCompleted() {
        LogUtil.logD(TAG, "onBootCompleted");
        IntentFilter unlockedIntentFilter = new IntentFilter();
        unlockedIntentFilter.addAction(Intent.ACTION_USER_UNLOCKED);
        mContext.registerReceiver(mUnlockReceiver, unlockedIntentFilter);

        mCellOnlyMode.onBootCompleted();

        if (mBtMediator != null) {
            mBtMediator.onBootCompleted();
        }
        if (mWifiMediator != null) {
            mWifiMediator.onBootCompleted(mBtMediator.isProxyConnected());
        }
        if (mCellMediator != null) {
            mCellMediator.onBootCompleted(mBtMediator.isProxyConnected());
        }

        if (mActivityModeTracker.isActivityModeEnabled()) {
            onActivityModeChanged(true);
        }

        // Enable data-saver mode to reduce power consumption due to LTE network
        // see b/217234665
        if (WearResourceUtil.getWearableResources(mContext).getBoolean(
                com.android.wearable.resources.R.bool.data_saver_always_on)) {
            NetworkPolicyManager policyManager = NetworkPolicyManager.from(mContext);
            policyManager.setRestrictBackground(true);
        }
    }

    private void notifyProxyStatusChange(ProxyStatus proxyStatus) {
        mWifiDebounce.updateProxyStatus(proxyStatus);
        mCellDebounce.updateProxyStatus(proxyStatus);
    }

    @Override
    public void onUnmeteredRequestsChanged(int numUnmeteredRequests) {
        mNumUnmeteredRequests = numUnmeteredRequests;
        if (mWifiMediator != null) {
            mWifiMediator.updateNumUnmeteredRequests(numUnmeteredRequests);
        }
    }

    @Override
    public void onHighBandwidthRequestsChanged(int numHighBandwidthRequests) {
        mNumHighBandwidthRequests = numHighBandwidthRequests;
        if (mCellMediator != null) {
            mCellMediator.updateNumHighBandwidthRequests(numHighBandwidthRequests);
        }
        if (mWifiMediator != null) {
            mWifiMediator.updateNumHighBandwidthRequests(numHighBandwidthRequests);
        }
    }

    @Override
    public void onWifiRequestsChanged(int numWifiRequests) {
        mNumWifiRequests = numWifiRequests;
        if (mWifiMediator != null) {
            mWifiMediator.updateNumWifiRequests(numWifiRequests);
        }
    }

    @Override
    public void onCellularRequestsChanged(int numCellularRequests) {
        mNumCellularRequests = numCellularRequests;
        if (mCellMediator != null) {
            mCellMediator.updateNumCellularRequests(numCellularRequests);
        }
    }

    @Override
    public void onActivityModeChanged(boolean enabled) {
        if (mBtMediator != null && mActivityModeTracker.affectsBluetooth()) {
            mBtMediator.updateActivityMode(enabled);
        }

        if (mWifiMediator != null && mActivityModeTracker.affectsWifi()) {
            mWifiMediator.updateActivityMode(enabled);
        }

        if (mCellMediator != null && mActivityModeTracker.affectsCellular()) {
            mCellMediator.updateActivityMode(enabled);
        }
    }

    @Override
    public void onCellOnlyModeChanged(boolean enabled) {
        // update all mediators
        if (mBtMediator != null) {
            mBtMediator.updateCellOnlyMode(enabled);
        }

        if (mWifiMediator != null) {
            mWifiMediator.updateCellOnlyMode(enabled);
        }

        if (mCellMediator != null) {
            mCellMediator.updateCellOnlyMode(enabled);
        }
    }

    @Override
    public void onThermalEmergencyChanged(ThermalEmergencyMode mode) {
        if (mBtMediator != null) {
            mBtMediator.updateThermalEmergencyMode(mode);
        }

        if (mWifiMediator != null) {
            mWifiMediator.updateThermalEmergencyMode(mode);
        }

        if (mCellMediator != null) {
            mCellMediator.updateThermalEmergencyMode(mode);
        }
    }

    @Override
    public void onWifiConnectionChange(WifiConnectivityStatus status) {
        if (mExtendCellDelay != 0 && status == WifiConnectivityStatus.STATUS_WIFI_CONNECTED) {
            mCellDebounce.extend(mExtendCellDelay);
        }
        if (mCellMediator != null) {
            mCellMediator.onWifiConnectedChange(
                    status == WifiConnectivityStatus.STATUS_WIFI_CONNECTED);
        }
    }

    public void dump(IndentingPrintWriter ipw) {
        ipw.println("================ WearConnectivityService ================");
        ipw.println("Proxy NetworkAgent connection status:" +
                (mProxyNetworkAgent.isProxyConnected() ? "connected" : "disconnected"));
        mWifiDebounce.dump(ipw);
        mCellDebounce.dump(ipw);
        mActivityModeTracker.dump(ipw);
        mThermalEmergencyTracker.dump(ipw);
        ipw.println();
        ipw.printPair("mNumHighBandwidthRequests", mNumHighBandwidthRequests);
        ipw.printPair("mNumUnmeteredRequests", mNumUnmeteredRequests);
        ipw.printPair("mNumWifiRequests", mNumWifiRequests);
        ipw.printPair("mNumCellularRequests", mNumCellularRequests);
        ipw.println();

        ipw.increaseIndent();

        ipw.println();
        if (mBtMediator != null) {
            mBtMediator.dump(ipw);
        } else {
            ipw.println("Wear Bluetooth disabled because BluetoothAdapter is missing.");
        }

        ipw.println();
        if (mWifiMediator != null) {
            mWifiMediator.dump(ipw);
        } else {
            ipw.println("Wear Wifi Mediator disabled because WifiManager is missing.");
        }

        ipw.println();
        if (mCellMediator != null) {
            mCellMediator.dump(ipw);
        } else {
            ipw.println("Wear Cellular Mediator disabled on this device.");
        }

        ipw.println();
        mWearConnectivityPackageManager.dump(ipw);

        ipw.println();
        ipw.decreaseIndent();
    }
}
