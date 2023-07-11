package com.android.clockwork.connectivity;

import android.annotation.NonNull;
import android.app.AlarmManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.euicc.EuiccManager;

import com.android.clockwork.bluetooth.BluetoothLogger;
import com.android.clockwork.bluetooth.BluetoothScanModeEnforcer;
import com.android.clockwork.bluetooth.BluetoothShardRunner;
import com.android.clockwork.bluetooth.CompanionTracker;
import com.android.clockwork.bluetooth.DeviceInformationGattServer;
import com.android.clockwork.bluetooth.WearBluetoothMediator;
import com.android.clockwork.bluetooth.WearBluetoothMediatorSettings;
import com.android.clockwork.bluetooth.proxy.ProxyGattServer;
import com.android.clockwork.bluetooth.proxy.ProxyLinkProperties;
import com.android.clockwork.bluetooth.proxy.ProxyPinger;
import com.android.clockwork.bluetooth.proxy.ProxyServiceHelper;
import com.android.clockwork.cellular.WearCellularMediator;
import com.android.clockwork.cellular.WearCellularMediatorSettings;
import com.android.clockwork.common.ActivityModeTracker;
import com.android.clockwork.common.CellOnlyMode;
import com.android.clockwork.common.DeviceEnableSetting;
import com.android.clockwork.flags.BooleanFlag;
import com.android.clockwork.flags.ClockworkFlags;
import com.android.clockwork.power.PowerTracker;
import com.android.clockwork.power.TimeOnlyMode;
import com.android.clockwork.power.WearPowerServiceInternal;
import com.android.clockwork.wifi.SimpleTimerWifiBackoff;
import com.android.clockwork.wifi.WearWifiMediator;
import com.android.clockwork.wifi.WearWifiMediatorSettings;
import com.android.clockwork.wifi.WifiLogger;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.SystemService;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * WearConnectivityService determines which connectivity mechanisms should be activated
 * for a given set of conditions.
 *
 * Design doc: go/wear-connectivity-service
 */
public class WearConnectivityService extends SystemService {
    public static final String SERVICE_NAME = WearConnectivityService.class.getSimpleName();

    /** Feature flag for Local Edition/Sino Wear (LE/SW) version. */
    @VisibleForTesting static final String FEATURE_CN_GOOGLE = "cn.google";

    /** An old feature flag for Local Edition/Sino Wear (LE/SW) version. */
    @VisibleForTesting static final String FEATURE_SIDEWINDER = "com.google.sidewinder";

    private WearConnectivityController mController;
    private BluetoothScanModeEnforcer mBtScanModeEnforcer;
    private WearNetworkObserver mWearNetworkObserver;

    private BooleanFlag mUserAbsentRadiosOff;

    public WearConnectivityService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        publishBinderService(SERVICE_NAME, new BinderService());
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == com.android.server.SystemService.PHASE_SYSTEM_SERVICES_READY) {
            mUserAbsentRadiosOff =
                ClockworkFlags.userAbsentRadiosOff(getContext().getContentResolver());

            WearPowerServiceInternal powerService = getLocalService(WearPowerServiceInternal.class);

            PowerTracker powerTracker = powerService.getPowerTracker();
            TimeOnlyMode timeOnlyMode = powerService.getTimeOnlyMode();

            DeviceEnableSetting deviceEnableSetting =
                    new DeviceEnableSetting(getContext(), getContext().getContentResolver());

            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            CompanionTracker companionTracker =
                    new CompanionTracker(getContext().getContentResolver(), btAdapter);
            WearBluetoothMediator btMediator = null;
            // btAdapter == null means we're on emulator (or an unsupported device)
            if (btAdapter != null) {
                BluetoothLogger btLogger = new BluetoothLogger();
                mBtScanModeEnforcer =
                        new BluetoothScanModeEnforcer(getContext(), btAdapter, companionTracker);
                ProxyServiceHelper proxyServiceHelper = new ProxyServiceHelper(
                        getContext(), new NetworkCapabilities.Builder(), new ProxyLinkProperties(
                            new LinkProperties(), isLocalEditionDevice(getContext())));
                BluetoothShardRunner btShardRunner =
                        new BluetoothShardRunner(getContext(), companionTracker,
                            proxyServiceHelper);
                DeviceInformationGattServer deviceInformationServer =
                    new DeviceInformationGattServer(getContext());
                ProxyGattServer proxyGattServer = new ProxyGattServer(getContext());

                btMediator = new WearBluetoothMediator(
                        getContext(),
                        getContext().getSystemService(AlarmManager.class),
                        new WearBluetoothMediatorSettings(getContext().getContentResolver()),
                        btAdapter,
                        btLogger,
                        btShardRunner,
                        companionTracker,
                        powerTracker,
                        deviceEnableSetting,
                        mUserAbsentRadiosOff,
                        timeOnlyMode,
                        deviceInformationServer,
                        proxyGattServer,
                        new ProxyPinger(proxyGattServer));
            }

            WearConnectivityPackageManager wearConnectivityPackageManager =
                    new WearConnectivityPackageManager(getContext());
            WearCellularMediator cellMediator = null;
            PackageManager packageManager = getContext().getPackageManager();
            // Don't mediate cell in emulator.  The emulator relies on cellular to be present
            // and enabled to have the network connectivity required for local/TCP pairing.
            if (btAdapter != null
                    && packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                TelephonyManager telephonyManager =
                        getContext().getSystemService(TelephonyManager.class);
                EuiccManager euiccManager =
                        getContext().getSystemService(EuiccManager.class);
                cellMediator = new WearCellularMediator(
                        getContext(),
                        getContext().getSystemService(AlarmManager.class),
                        telephonyManager,
                        euiccManager,
                        getContext().getSystemService(SubscriptionManager.class),
                        new WearCellularMediatorSettings(
                                getContext(),
                                isLocalEditionDevice(getContext()),
                                telephonyManager.getSimOperator()),
                        powerTracker,
                        deviceEnableSetting,
                        wearConnectivityPackageManager,
                        mUserAbsentRadiosOff);
            }

            WearWifiMediator wifiMediator = null;
            WifiManager wifiManager = getContext().getSystemService(WifiManager.class);
            if (wifiManager != null) {
                WifiLogger wifiLogger = new WifiLogger();
                wifiMediator = new WearWifiMediator(
                        getContext(),
                        getContext().getSystemService(AlarmManager.class),
                        new WearWifiMediatorSettings(getContext().getContentResolver()),
                        companionTracker,
                        powerTracker,
                        deviceEnableSetting,
                        wearConnectivityPackageManager,
                        mUserAbsentRadiosOff,
                        new SimpleTimerWifiBackoff(getContext(), wifiLogger),
                        wifiManager,
                        wifiLogger);
            }

            WearProxyNetworkAgent proxyNetworkAgent = new WearProxyNetworkAgent(
                    getContext().getSystemService(ConnectivityManager.class));

            mController = new WearConnectivityController(
                    getContext(),
                    getContext().getSystemService(AlarmManager.class),
                    btMediator,
                    wifiMediator,
                    cellMediator,
                    wearConnectivityPackageManager,
                    proxyNetworkAgent,
                    new ActivityModeTracker(getContext()),
                    new CellOnlyMode(getContext(), getContext().getContentResolver(), getContext().getSystemService(AlarmManager.class)));
            mWearNetworkObserver = new WearNetworkObserver(
                getContext(),
                wearConnectivityPackageManager,
                mController);
            mWearNetworkObserver.register();
        } else if (phase == com.android.server.SystemService.PHASE_BOOT_COMPLETED) {
            mUserAbsentRadiosOff.register();
            mController.onBootCompleted();
        }
    }

    private final class BinderService extends Binder {
        @Override
        protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
            IndentingPrintWriter ipw = new IndentingPrintWriter(writer, "  " /* singleIndent */);;
            mController.dump(ipw);
            ipw.println();
            mWearNetworkObserver.dump(ipw);
            ipw.println();
            if (mBtScanModeEnforcer != null) {
                mBtScanModeEnforcer.dump(ipw);
                ipw.println();
            }
        }
    }

    // Return true if we are running in a special mode for devices in China.
    private static boolean isLocalEditionDevice(@NonNull final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        final PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(FEATURE_SIDEWINDER)
              || pm.hasSystemFeature(FEATURE_CN_GOOGLE);
    }
}
