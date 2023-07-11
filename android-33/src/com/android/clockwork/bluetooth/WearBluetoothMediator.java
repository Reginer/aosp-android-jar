package com.android.clockwork.bluetooth;

import static com.android.clockwork.bluetooth.WearBluetoothSettings.PROXY_SCORE_BLE;
import static com.android.clockwork.bluetooth.WearBluetoothSettings.PROXY_SCORE_CLASSIC;
import static com.android.clockwork.bluetooth.WearBluetoothSettings.PROXY_SCORE_ON_CHARGER;

import android.annotation.MainThread;
import android.annotation.NonNull;
import android.annotation.WorkerThread;
import android.app.AlarmManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.EventLog;
import android.util.Log;

import com.android.clockwork.bluetooth.proxy.ProxyGattServer;
import com.android.clockwork.bluetooth.proxy.ProxyPinger;
import com.android.clockwork.common.DeviceEnableSetting;
import com.android.clockwork.common.EventHistory;
import com.android.clockwork.common.LogUtil;
import com.android.clockwork.flags.BooleanFlag;
import com.android.clockwork.power.PowerTracker;
import com.android.clockwork.power.TimeOnlyMode;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class manages a collection of Shards, a set of objects that interact with the Bluetooth
 * subsystem. To ensure correct use of the Bluetooth APIs, this class instantiates Shards only when
 * it is safe to call Bluetooth APIs and destroys that when it is no longer safe to operate on
 * Bluetooth.
 *
 * In particular, this class guarantees that Shards are only active when the following conditions
 * are true:
 *
 * 1) A Bluetooth adapter exists on the device (not true in the Android emulator)
 * 2) The Bluetooth adapter is enabled
 * 3) The device is paired with a companion phone and the companion's BluetoothDevice object is
 * available.
 *
 * Eventually, this class will also guarantee that the companion device is nearby and connectable
 * before instantiating the Shards. This functionality is not currently available.
 */

public class WearBluetoothMediator implements
        CompanionProxyShard.Listener,
        CompanionTracker.Listener,
        WearBluetoothMediatorSettings.Listener,
        PowerTracker.Listener,
        TimeOnlyMode.Listener,
        ProxyGattServer.Listener,
        DeviceEnableSetting.Listener {
    static final String TAG = WearBluetoothSettings.LOG_TAG;

    /** After attempting to connect proxy upon bootup, wait this long before giving up. */
    static final Long CANCEL_ON_BOOT_CONNECT_DELAY_MS = TimeUnit.MINUTES.toMillis(5);
    static final String ACTION_CANCEL_ON_BOOT_CONNECT =
            "com.android.clockwork.bluetooth.action.CANCEL_ON_BOOT_CONNECT";
    /** Intent originally defined in and sent from HFPBroadcastReceiver.java. */
    @VisibleForTesting
    static final String ACTION_TOGGLE_BT_ADAPTER_HFP_ENABLED =
            "com.google.android.clockwork.settings.action.toggle_bt_enable_hfp";
    @VisibleForTesting
    static final String EXTRA_ADAPTER_ENABLE = "adapter_enable";
    @VisibleForTesting
    static final int MSG_DISABLE_BT = 0;
    @VisibleForTesting
    static final int MSG_ENABLE_BT = 1;
    // A default timeoue of two minutes seems to be used by most devices at the moment.
    @VisibleForTesting
    static final int DEFAULT_DISCOVERABLE_TIMEOUT_SECS = 120;
    @VisibleForTesting
    static final int PORT_RFCOMM = 0;
    private static final long WAIT_FOR_SET_RADIO_POWER_IN_MS = TimeUnit.SECONDS.toMillis(2);
    private final Object mLock = new Object();
    // TODO(cmanton) Do we need to keep a reference to this as it only used on boot
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean mProxyConnected = new AtomicBoolean(false);
    private final AtomicBoolean mFirstAdapterEnableAfterBoot = new AtomicBoolean(true);
    private final EventHistory<ProxyConnectionEvent> mProxyHistory =
            new EventHistory<>("Proxy Connection History", 30, false);
    private final EventHistory<BtDecision> mHistory =
            new EventHistory<>("Bluetooth Radio Power History", 30, false);
    private final AlarmManager mAlarmManager;
    private final BluetoothAdapter mAdapter;
    private final BluetoothLogger mBtLogger;
    private final BluetoothShardRunner mShardRunner;
    private final CompanionTracker mCompanionTracker;
    private final Context mContext;
    private final PowerTracker mPowerTracker;
    private final WearBluetoothMediatorSettings mSettings;
    private final DeviceInformationGattServer mDeviceInformationServer;
    private final ProxyGattServer mProxyGattServer;
    private final ProxyPinger mProxyPinger;
    private final DeviceEnableSetting mDeviceEnableSetting;
    private final BooleanFlag mUserAbsentRadiosOff;
    private final BroadcastReceiver bondStateReceiver = new BroadcastReceiver() {
        @MainThread
        @Override
        public void onReceive(Context mContext, Intent intent) {
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                final BluetoothDevice device = intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE);
                final int previousBondState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
                final int currentBondState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);

                Log.i(TAG, "Device " + device + " changed bond state: " + currentBondState);
                if (currentBondState == BluetoothDevice.BOND_BONDED) {
                    mCompanionTracker.receivedBondedAction(device);
                }

                if (previousBondState == BluetoothDevice.BOND_BONDED
                        && currentBondState == BluetoothDevice.BOND_BONDING) {
                    mBtLogger.logUnexpectedPairingEvent(device);
                }
            }
        }
    };
    @VisibleForTesting
    HandlerThread mRadioPowerThread;
    @VisibleForTesting
    Handler mRadioPowerHandler;
    private final BroadcastReceiver hfpEnableStateReceiver = new BroadcastReceiver() {
        @MainThread
        @Override
        public void onReceive(Context mContext, Intent intent) {
            if (ACTION_TOGGLE_BT_ADAPTER_HFP_ENABLED.equals(intent.getAction())) {
                final boolean enable = intent.getBooleanExtra(EXTRA_ADAPTER_ENABLE, false);
                if (enable) {
                    changeRadioPower(true, Reason.ON_HFP_ENABLE);
                } else {
                    changeRadioPower(false, Reason.OFF_HFP_ENABLE);
                }
            }
        }
    };
    private boolean mDeviceConnected;
    private boolean mHfpConnected;
    private final BroadcastReceiver adapterStateReceiver = new BroadcastReceiver() {
        @MainThread
        @Override
        public void onReceive(Context mContext, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                final int adapterState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.STATE_OFF);
                final int previousAdapterState = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_PREVIOUS_STATE,
                        BluetoothAdapter.STATE_OFF);
                if (adapterState == BluetoothAdapter.STATE_ON) {
                    onAdapterEnabled();
                } else if (previousAdapterState == BluetoothAdapter.STATE_ON) {
                    // All transition states are treated as if the adapter is off so that flows such
                    // as "ON -> TURNING_OFF -> TURNING_ON -> ON" are detected. This is useful to
                    // restart the GATT servers when Bluetooth process restarts.
                    onAdapterDisabled();
                }
            }
        }
    };
    private final BroadcastReceiver aclStateReceiver = new BroadcastReceiver() {
        @MainThread
        @Override
        public void onReceive(Context mContext, Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (mCompanionTracker.getCompanion() == null
                    || !device.getAddress().equals(mCompanionTracker.getCompanion().getAddress())) {
                LogUtil.logD(TAG, "Ignoring ACL connection event for non-companion device.");
                return;
            }

            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    LogUtil.logD(TAG, "ACL_CONNECTED for device: %s", device.getAddress());
                    if (device.isConnected()) {
                        onCompanionDeviceConnected();
                    }
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    LogUtil.logD(TAG, "ACL_DISCONNECTED for device: %s", device.getAddress());
                    if (!device.isConnected()) {
                        onCompanionDeviceDisconnected();
                    } else {
                        LogUtil.logD(TAG,
                                "Ignoring ACL Disconnected because device is still connected.");
                    }
                    break;
            }
        }
    };
    private boolean mActivityMode;
    private boolean mCellOnlyMode;
    private boolean mTimeOnlyMode;
    private boolean mIsAirplaneModeOn;
    private boolean mIsSettingsPreferenceBluetoothOn;
    // The latest proxy channel change ID received from the iOS companion. Or 0, if none received.
    private int mLatestChannelChangeId = 0;

    public WearBluetoothMediator(final Context context,
            final AlarmManager alarmManager,
            final WearBluetoothMediatorSettings btSettings,
            final BluetoothAdapter btAdapter,
            final BluetoothLogger btLogger,
            final BluetoothShardRunner shardRunner,
            final CompanionTracker companionTracker,
            final PowerTracker powerTracker,
            final DeviceEnableSetting deviceEnableSetting,
            final BooleanFlag userAbsentRadiosOff,
            final TimeOnlyMode timeOnlyMode,
            final DeviceInformationGattServer deviceInformationServer,
            final ProxyGattServer proxyGattServer,
            final ProxyPinger proxyPinger) {
        mContext = context;
        mAlarmManager = alarmManager;
        mSettings = btSettings;
        mAdapter = btAdapter;
        mBtLogger = btLogger;
        mShardRunner = shardRunner;
        mCompanionTracker = companionTracker;
        mDeviceEnableSetting = deviceEnableSetting;
        mUserAbsentRadiosOff = userAbsentRadiosOff;
        mPowerTracker = powerTracker;
        mProxyGattServer = proxyGattServer;
        mProxyPinger = proxyPinger;

        mCompanionTracker.addListener(this);
        mPowerTracker.addListener(this);
        mSettings.addListener(this);
        mUserAbsentRadiosOff.addListener(this::onUserAbsentRadiosOffChanged);
        timeOnlyMode.addListener(this);
        proxyGattServer.setListener(this);
        mDeviceEnableSetting.addListener(this);

        mIsAirplaneModeOn = mSettings.getIsInAirplaneMode();
        mIsSettingsPreferenceBluetoothOn = mSettings.getIsSettingsPreferenceBluetoothOn();
        mDeviceInformationServer = deviceInformationServer;
        mRadioPowerThread = new HandlerThread(TAG + ".RadioPowerHandler");
        mRadioPowerThread.start();
        mRadioPowerHandler = new RadioPowerHandler(mRadioPowerThread.getLooper());
    }

    public void onBootCompleted() {
        IntentFilter aclIntentFilter = new IntentFilter();
        aclIntentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        aclIntentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mContext.registerReceiver(aclStateReceiver, aclIntentFilter);
        mContext.registerReceiver(adapterStateReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        mContext.registerReceiver(bondStateReceiver,
                new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        mContext.registerReceiver(hfpEnableStateReceiver,
                new IntentFilter(ACTION_TOGGLE_BT_ADAPTER_HFP_ENABLED),
                Context.RECEIVER_EXPORTED);

        // onBootCompleted does NOT execute on the main thread, but all of this stuff needs to
        // run on the main thread, so we redirect the work to the main mRadioPowerHandler here
        mMainHandler.post(() -> {
            if (mDeviceConnected || mProxyConnected.get()) {
                return;
            }

            // The adapter should always be enabled on boot (unless airplane mode is on).
            if (mAdapter.isEnabled()) {
                onAdapterEnabled();
            } else {
                // Not enabled. Enable if airplane mode is NOT on.
                if (!mSettings.getIsInAirplaneMode()) {
                    Log.w(TAG, "Enabling an unexpectedly disabled Bluetooth adapter.");
                    changeRadioPower(true, Reason.ON_BOOT_AUTO);
                    mSettings.setSettingsPreferenceBluetoothOn(true);
                }
            }
        });
    }

    /**
     * Note that transitions back to deviceEnabled are explicitly not handled here. Shard starts
     * are triggered by an incoming BT connection, which occurs only if the user has re-enabled
     * the device from the remote Companion app.
     *
     * <p>Effectively, re-enabling the device just allows for the next transition to disabled.
     */
    @Override
    public void onDeviceEnableChanged() {
        if (!mDeviceEnableSetting.isDeviceEnabled()) {
            mShardRunner.stopProxyShard();
        }
    }

    @Override
    public void onTimeOnlyModeChanged(boolean timeOnlyMode) {
        if (mTimeOnlyMode != timeOnlyMode) {
            mTimeOnlyMode = timeOnlyMode;
            updateRadioPower();
        }
    }

    public void updateActivityMode(boolean activeMode) {
        if (mActivityMode != activeMode) {
            mActivityMode = activeMode;
            updateRadioPower();
        }
    }

    public void updateCellOnlyMode(boolean cellOnlyMode) {
        if (mCellOnlyMode != cellOnlyMode) {
            mCellOnlyMode = cellOnlyMode;
            updateRadioPower();
        }
    }

    public void onUserAbsentRadiosOffChanged(boolean isEnabled) {
        updateRadioPower();
    }

    private void updateRadioPower() {
        if (mIsAirplaneModeOn) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Disabling mediator while airplane mode enabled");
            }
            return;
        } else if (mCellOnlyMode) {
            changeRadioPower(false, Reason.OFF_CELL_ONLY_MODE);
        } else if (mActivityMode) {
            changeRadioPower(false, Reason.OFF_ACTIVITY_MODE);
        } else if (mPowerTracker.isDeviceIdle() && mUserAbsentRadiosOff.isEnabled()) {
            changeRadioPower(false, Reason.OFF_USER_ABSENT);
        } else if (mTimeOnlyMode) {
            changeRadioPower(false, Reason.OFF_TIME_ONLY_MODE);
        } else if (!mIsSettingsPreferenceBluetoothOn) {
            changeRadioPower(false, Reason.OFF_SETTINGS_PREFERENCE);
        } else {
            changeRadioPower(true, Reason.ON_AUTO);
        }
    }

    private void changeRadioPower(boolean enable, Reason reason) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, reason.name() + " attempt to change radio power: " + enable);
        }

        Message msg = Message.obtain(mRadioPowerHandler,
                enable ? MSG_ENABLE_BT : MSG_DISABLE_BT, reason);
        mRadioPowerHandler.removeMessages(MSG_ENABLE_BT);
        mRadioPowerHandler.removeMessages(MSG_DISABLE_BT);
        mRadioPowerHandler.sendMessage(msg);
    }

    @Override
    public void onPowerSaveModeChanged() {
        // BluetoothMediator does not respond directly to PowerSaveMode changes.
    }

    @Override
    public void onChargingStateChanged() {
        mShardRunner.updateProxyShard(getScoreForProxy());
    }

    @Override
    public void onDeviceIdleModeChanged() {
        if (!mPowerTracker.getDozeModeAllowListedFeatures().get(PowerTracker.DOZE_MODE_BT_INDEX)) {
            updateRadioPower();
        } else {
            Log.d(TAG, "Ignoring doze mode intent as BT is being kept enabled during doze.");
        }
    }

    @Override  // WearBluetoothMediatorSettings.Listener
    public void onAirplaneModeSettingChanged(boolean isAirplaneModeOn) {
        mIsAirplaneModeOn = isAirplaneModeOn;
    }

    @Override  // WearBluetoothMediatorSettings.Listener
    public void onSettingsPreferenceBluetoothSettingChanged(
            boolean isSettingsPreferenceBluetoothOn) {
        mIsSettingsPreferenceBluetoothOn = isSettingsPreferenceBluetoothOn;
    }

    /**
     * Similar to meteredness, it is expected that DnsServers will change while proxy is running,
     * such as whenever the phone transitions between various modes of connectivity.  This ensures
     * that the proxy updates alongside the phone.
     */
    @Override // WearBluetoothMediatorSettings.Listener
    public void onDnsServersChanged() {
        mShardRunner.updateProxyShard(mSettings.getDnsServers());
    }

    @Override  // ProxyGattServer.Listener
    public void onProxyConfigUpdate(int psm, int channelChangeId, int minPingIntervalSeconds) {
        mMainHandler.post(() -> {
            if (mProxyConnected.get() && mShardRunner.getConnectionPort() == psm
                    && mLatestChannelChangeId == channelChangeId) {
                Log.i(TAG, "Proxy already connected. Ignoring config update with psm: " + psm
                        + " change ID: " + channelChangeId);
                return;
            }
            mLatestChannelChangeId = channelChangeId;
            mProxyPinger.setMinPingIntervalMs(minPingIntervalSeconds * 1000);
            mShardRunner.startProxyShard(
                    getScoreForProxy(),
                    mSettings.getDnsServers(),
                    this,
                    "PSM Update Received",
                    psm);
        });
    }

    @Override  // CompanionProxyShard.Listener
    public void onProxyBleData() {
        mProxyPinger.pingIfNeeded();
    }

    @Override  // CompanionProxyShard.Listener
    public void onProxyConnectionChange(boolean isConnected, int proxyScore, boolean withInternet) {
        if (isConnected) {
            LogUtil.logD(TAG, "sysproxy connection changed - connected"
                    + (withInternet ? " with internet score (" + proxyScore + ")"
                    : " but with no internet"));
        } else {
            LogUtil.logD(TAG, "sysproxy connection changed - disconnected");
        }
        mProxyConnected.set(isConnected);
        mBtLogger.logProxyConnectionChange(isConnected);
        mProxyHistory.recordEvent(new ProxyConnectionEvent(
                isConnected,
                withInternet,
                proxyScore));

        if (!isConnected && !isUsingRfcomm()) {
            // If the iOS companion app is terminated, e.g. because it crashed, the L2CAP socket is
            // closed and sysproxy disconnects. However, iOS Bluetooth state restoration feature
            // keeps the companion connected over GATT. The watch pings the companion app over GATT
            // here to launch it again and reconnect over L2CAP.
            mProxyPinger.ping();
        }
    }

    public boolean isProxyConnected() {
        return mProxyConnected.get();
    }

    @Override
    public void onCompanionChanged() {
        mBtLogger.logCompanionPairingEvent(mCompanionTracker.isCompanionBle());
        mProxyGattServer.setCompanionDevice(mCompanionTracker.getCompanion());
        setDeviceConnected(true);
        if (isUsingRfcomm()) {
            LogUtil.logD(TAG, "Starting Proxy Shard because new companion device paired.");
            mShardRunner.startProxyShard(
                    getScoreForProxy(),
                    mSettings.getDnsServers(),
                    this,
                    "Companion Found",
                    PORT_RFCOMM);
        }
        LogUtil.logD(TAG, "New companion device paired. Starting HfcShard.");
    }

    private int getScoreForProxy() {
        return mCompanionTracker.isCompanionBle() ? PROXY_SCORE_BLE :
            (mPowerTracker.isCharging() ? PROXY_SCORE_ON_CHARGER : PROXY_SCORE_CLASSIC);
    }

    private void onAdapterEnabled() {
        boolean firstEnableAfterBoot = mFirstAdapterEnableAfterBoot.getAndSet(false);
        if (firstEnableAfterBoot) {
            mCompanionTracker.onBluetoothAdapterReady();
        }

        mDeviceInformationServer.start();

        BluetoothDevice companionDevice = mCompanionTracker.getCompanion();
        if (companionDevice == null) {
            LogUtil.logD(TAG, "Starting proxy GATT server as watch is not paired.");
            // Proxy GATT server is only used for iOS but it starts in the unpaired state as well
            // in case the watch is paired with iOS later. Starting it right after pairing would
            // unnecessarily invalidate GATT handles and cause a temporary disconnection on the
            // wearable transport.
            mProxyGattServer.start();

            return;  // if no companion paired, we're done.
        }

        // Ensure that discoverable timeout isn't infinite when in paired
        // state. This code is for handling a corner case and should not be
        // relied upon to ensure that the adapter is in the expected state.
        if (firstEnableAfterBoot && mAdapter.getDiscoverableTimeout().toSeconds() == 0) {
            Log.w(TAG, "Detected infinite discoverable timeout while paired. "
                    + "Setting to default value of " + DEFAULT_DISCOVERABLE_TIMEOUT_SECS);
            mAdapter.setDiscoverableTimeout(Duration.ofSeconds(DEFAULT_DISCOVERABLE_TIMEOUT_SECS));
        }

        if (!isUsingRfcomm()) {
            LogUtil.logD(TAG, "Starting proxy GATT server.");
            mProxyGattServer.setCompanionDevice(companionDevice);
            mProxyGattServer.start();
        }
    }

    private void onAdapterDisabled() {
        LogUtil.logD(TAG, "Bluetooth Adapter disabled. Stopping all shards.");
        mShardRunner.stopProxyShard();
        mDeviceInformationServer.stop();
        mProxyGattServer.stop();
        mHfpConnected = false;
        if (mDeviceConnected) {
            setDeviceConnected(false);
        }
    }

    private void onCompanionDeviceConnected() {
        setDeviceConnected(true);
        // If proxy is connected via some other means, then we don't need to start it again.
        if (!mProxyConnected.get() && isUsingRfcomm()) {
            LogUtil.logD(TAG, "Companion device connected. Starting proxy shard.");
            mShardRunner.startProxyShard(
                    getScoreForProxy(),
                    mSettings.getDnsServers(),
                    this,
                    "Companion Connected",
                    PORT_RFCOMM);
        }
    }

    private void onCompanionDeviceDisconnected() {
        LogUtil.logD(TAG, "Companion device disconnected. Stopping proxy shard.");
        setDeviceConnected(false);
        mHfpConnected = false;
        mShardRunner.stopProxyShard();
    }

    private void setDeviceConnected(boolean deviceConnected) {
        mDeviceConnected = deviceConnected;
        mBtLogger.logAclConnectionChange(deviceConnected);
    }

    /** Returns whether the proxy uses an RFCOMM socket */
    private boolean isUsingRfcomm() {
        return !mCompanionTracker.isCompanionBle();
    }

    public void dump(@NonNull final IndentingPrintWriter ipw) {
        ipw.println("======== WearBluetoothMediator ========");
        ipw.printPair("Companion address",
                mCompanionTracker.getCompanionAddress()
                        + ((mCompanionTracker.getCompanion() == null) ? "(not bonded)" : ""));
        ipw.printPair("Companion type", mCompanionTracker.isCompanionBle() ? "BLE" : "CLASSIC");
        ipw.println();

        ipw.printPair("Device", mDeviceConnected ? "connected" : "disconnected");
        ipw.printPair("Proxy", mProxyConnected.get() ? "connected" : "disconnected");
        ipw.printPair("btAdapter", mAdapter.isEnabled() ? "enabled" : "disabled");
        ipw.println();
        ipw.printPair("mIsAirplaneModeOn", mIsAirplaneModeOn);
        ipw.printPair("mIsSettingsPreferenceBluetoothOn", mIsSettingsPreferenceBluetoothOn);
        ipw.println();
        ipw.printPair("mActivityMode", mActivityMode);
        ipw.printPair("mTimeOnlyMode", mTimeOnlyMode);
        ipw.println();
        ipw.printPair("Allowed during doze mode", mPowerTracker.getDozeModeAllowListedFeatures()
                .get(PowerTracker.DOZE_MODE_BT_INDEX));
        ipw.println();

        mHistory.dump(ipw);
        ipw.println();

        mShardRunner.dumpShards(ipw);
        ipw.println();
        mProxyHistory.dump(ipw);
        ipw.println();

        mDeviceInformationServer.dump(ipw);
        ipw.println();

        mProxyGattServer.dump(ipw);
        ipw.println();
    }

    /** The reason that Bluetooth radio power changed. */
    public enum Reason {
        OFF_ACTIVITY_MODE,
        OFF_CELL_ONLY_MODE,
        OFF_TIME_ONLY_MODE,
        OFF_USER_ABSENT,
        OFF_SETTINGS_PREFERENCE,
        ON_AUTO,
        ON_BOOT_AUTO,
        OFF_HFP_ENABLE,
        ON_HFP_ENABLE
    }

    /** Encapsulate the decision process for modifying the bluetooth radio power state */
    public class BtDecision extends EventHistory.Event {
        public final Reason mReason;

        public BtDecision(Reason reason) {
            mReason = reason;
        }

        @Override
        public String getName() {
            return mReason.name();
        }
    }

    /**
     * Information describing a proxy connection event
     *
     * @param connected    Indicates watch has active rfcomm connection to phone.
     * @param withInternet Indicates phone has validated default network.
     * @param timestamp    The timestamp in ms when the event triggered.
     * @param score        The current advertised network score for the network.
     */
    @VisibleForTesting
    final class ProxyConnectionEvent extends EventHistory.Event {
        public final boolean connected;
        public final boolean withInternet;
        public final int score;

        public ProxyConnectionEvent(boolean connected, boolean withInternet, int score) {
            this.connected = connected;
            this.withInternet = withInternet;
            this.score = score;
        }

        @Override
        public String getName() {
            if (connected) {
                if (withInternet) {
                    return "CONNECTED [SCORE:" + score + "]";
                } else {
                    return "CONNECTED [NO INTERNET]";
                }
            } else {
                return "DISCONNECTED";
            }
        }

        @Override
        public boolean isDuplicateOf(EventHistory.Event event) {
            if (!(event instanceof ProxyConnectionEvent)) {
                return false;
            }
            ProxyConnectionEvent that = (ProxyConnectionEvent) event;
            // Ignore different network score if there is no internet
            if (that.withInternet || withInternet) {
                return that.connected == connected && that.withInternet == withInternet
                        && that.score == score;
            } else {
                return that.connected == connected;
            }
        }
    }

    private class RadioPowerHandler extends Handler {
        public RadioPowerHandler(Looper looper) {
            super(looper);
        }

        @WorkerThread
        @Override
        public void handleMessage(Message msg) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "handleMessage: " + msg);
            }

            boolean enable = (msg.what == MSG_ENABLE_BT);
            Reason reason = (Reason) msg.obj;

            if (enable) {
                mAdapter.enable();
            } else {
                mAdapter.disable();
            }
            // Log the radio change event.
            final BtDecision decision = new BtDecision(reason);
            EventLog.writeEvent(
                    EventLogTags.BT_RADIO_POWER_CHANGE_EVENT,
                    enable ? 1 : 0,
                    decision.getName(),
                    decision.getTimestampMs());
            Log.i(TAG, decision.getName() + " changed radio power: " + enable);
            mHistory.recordEvent(decision);

            try {
                synchronized (mLock) {
                    // Block the thread to ensure the service state is changed.
                    // 2 seconds timeout is enough for the radio power toggle.
                    mLock.wait(WAIT_FOR_SET_RADIO_POWER_IN_MS);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "wait() interrupted!", e);
            }
        }
    }
}
