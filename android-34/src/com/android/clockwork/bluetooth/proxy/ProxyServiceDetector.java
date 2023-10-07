package com.android.clockwork.bluetooth.proxy;

import android.annotation.MainThread;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.util.Log;

import com.android.clockwork.bluetooth.CompanionTracker;
import com.android.clockwork.common.LogUtil;
import com.android.clockwork.common.WearBluetoothSettings;
import com.android.clockwork.common.WearResourceUtil;
import com.android.internal.util.IndentingPrintWriter;
import com.android.networkstack.tethering.companionproxy.bt.GattDiscoveryClient;

import java.util.concurrent.TimeUnit;

/**
 * Detects Sysproxy version and related RFCOMM service UUID.
 */
public class ProxyServiceDetector {
    private static final String TAG = WearBluetoothSettings.LOG_TAG;

    private static final long V2_FAILURE_BACKOFF_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5);

    public interface Listener {
        void onBackgroundProxyConfigUpdated(String reason);
    }

    private final Context mContext;
    private final Listener mListener;
    private final CompanionTracker mCompanionTracker;
    private GattDiscoveryClient mV2DiscoveryClient;
    private SdpUuidReceiver mSdpUuidReceiver;
    private boolean mHadConnectError;
    private boolean mIsFetchingUuids;
    private ParcelUuid[] mUuids;
    private int mConnectionPort;
    private int mChannelChangeId;
    private ProxyServiceConfig mConfig = new ProxyServiceConfig();
    private long mEarliestV2RetryTime;

    public ProxyServiceDetector(Context context, Listener listener,
            CompanionTracker companionTracker) {
        mContext = context;
        mListener = listener;
        mCompanionTracker = companionTracker;
    }

    @MainThread
    public void startDiscovery() {
        if (mV2DiscoveryClient == null && isV2Enabled()) {
            mV2DiscoveryClient = new GattDiscoveryClient(
                mContext, new V2DiscoveryClientListener());
            onCompanionChanged();
            mV2DiscoveryClient.start();
        }
    }

    @MainThread
    public void stopDiscovery() {
        GattDiscoveryClient v2DiscoveryClient = mV2DiscoveryClient;
        mV2DiscoveryClient = null;
        if (v2DiscoveryClient != null) {
            v2DiscoveryClient.shutdown();
        }
    }

    @MainThread
    public void onCompanionChanged() {
        if (mV2DiscoveryClient != null) {
            mV2DiscoveryClient.setPeerDevice(mCompanionTracker.getCompanion());
        }
    }

    @MainThread
    public boolean setIosV1Params(int psm, int changeId) {
        mConnectionPort = psm;
        mChannelChangeId = changeId;
        return maybeUpdateVersion();
    }

    @MainThread
    private void setV2Params(int psm, int changeId) {
        mContext.getMainExecutor().execute(() -> {
            mConnectionPort = psm;
            mChannelChangeId = changeId;

            if (maybeUpdateVersion()) {
                mListener.onBackgroundProxyConfigUpdated("V2 params changed");
            }
        });
    }

    @MainThread
    public void update() {
        maybeUpdateVersion();
    }

    @MainThread
    public ProxyServiceConfig getCurrentConfig() {
        return mConfig;
    }

    @MainThread
    public void handleProxyConnectError(ProxyServiceConfig failedConfig) {
        Log.d(TAG, "[ProxyServiceDetector] Noted connection error, updating");
        mHadConnectError = true;

        mContext.getMainExecutor().execute(() -> {
            if (failedConfig.type == ProxyServiceConfig.Type.V2_ANDROID) {
                mEarliestV2RetryTime = System.currentTimeMillis() + V2_FAILURE_BACKOFF_TIMEOUT_MS;
            }

            maybeUpdateVersion();
        });
    }

    private boolean maybeUpdateVersion() {
        BluetoothDevice companion = mCompanionTracker.getCompanion();
        if (companion != null && mUuids == null) {
            mUuids = companion.getUuids();
        }

        ProxyServiceConfig newConfig = getNewConfig();
        if (newConfig.equals(mConfig)) {
            return false;
        }

        Log.i(TAG, "[ProxyServiceDetector] Updating proxy config to " + newConfig);

        mConfig = newConfig;
        return true;
    }

    private ProxyServiceConfig getNewConfig() {
        if (mCompanionTracker.isCompanionBle()) {
            return ProxyServiceConfig.forIosV1(mConnectionPort, mChannelChangeId);
        }

        if (mHadConnectError) {
            fetchUuidsWithSdp("Recovering from error");
        }

        if (mV2DiscoveryClient != null && mConnectionPort != 0
                && (mEarliestV2RetryTime == 0
                    || System.currentTimeMillis() >= mEarliestV2RetryTime)) {
            mEarliestV2RetryTime = 0;
            return ProxyServiceConfig.forAndroidV2(mConnectionPort, mChannelChangeId);
        }

        ParcelUuid service15Uuid = getSysproxyV15Uuid();
        if (service15Uuid != null) {
            return ProxyServiceConfig.forAndroidV15(service15Uuid);
        }

        return ProxyServiceConfig.forAndroidV1();
    }

    private void fetchUuidsWithSdp(String reason) {
        if (mIsFetchingUuids) {
            return;
        }

        BluetoothDevice companion = mCompanionTracker.getCompanion();
        if (companion != null && companion.fetchUuidsWithSdp()) {
            Log.d(TAG, "[ProxyServiceDetector] Starting UUID fetch operation: " + reason);

            mIsFetchingUuids = true;
            mHadConnectError = false;

            if (mSdpUuidReceiver == null) {
                mSdpUuidReceiver = new SdpUuidReceiver();
                mContext.registerReceiver(mSdpUuidReceiver,
                    new IntentFilter(BluetoothDevice.ACTION_UUID));
            }
        }
    }

    private void removeSdpUuidReceiver() {
        SdpUuidReceiver receiver = mSdpUuidReceiver;
        mSdpUuidReceiver = null;
        if (receiver != null) {
            mContext.unregisterReceiver(receiver);
        }
    }

    @MainThread
    public void dump(IndentingPrintWriter ipw) {
        ipw.increaseIndent();
        ipw.printPair("hadConnectError", mHadConnectError);
        ipw.printPair("connectionPort", mConnectionPort);
        ipw.printPair("channelChangeId", mChannelChangeId);
        ipw.printPair("isFetchingUuids", mIsFetchingUuids);
        ipw.printPair("hasUuidReceiver", mSdpUuidReceiver != null);
        if (mEarliestV2RetryTime != 0) {
            ipw.printPair(
                "earliestV2RetryTime", mEarliestV2RetryTime - System.currentTimeMillis());
        }
        ipw.println();

        ipw.printPair("uuids", uuidsToString(mUuids));
        ipw.println();

        ipw.printPair("config", mConfig);
        ipw.println();

        if (mV2DiscoveryClient != null) {
            mV2DiscoveryClient.dump(ipw);
        }

        ipw.decreaseIndent();
    }

    private static ParcelUuid getSysproxyV15Uuid() {
        String proxyService = SystemProperties.get("ro.cw.bt.proxy_service");
        return proxyService.isEmpty() ? null : ParcelUuid.fromString(proxyService);
    }

    private boolean isV2Enabled() {
        return WearResourceUtil.getWearableResources(mContext).getBoolean(
            com.android.wearable.resources.R.bool.config_enableSysproxyV2);
    }

    private static boolean hasUuid(ParcelUuid[] uuids, ParcelUuid uuid) {
        if (uuids != null) {
            for (ParcelUuid element : uuids) {
                if (element.equals(uuid)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String uuidsToString(ParcelUuid[] uuids) {
        StringBuilder str = new StringBuilder();
        if (uuids != null) {
            for (int i = 0; i < uuids.length; i++) {
                str.append(' ').append(uuids[i].getUuid().toString());
            }
        } else {
            str.append("<null>");
        }
        return str.toString();
    }

    private class SdpUuidReceiver extends BroadcastReceiver {
        @MainThread
        @Override
        public void onReceive(Context context, Intent intent) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null || mCompanionTracker.getCompanion() == null
                    || !device.getAddress().equals(mCompanionTracker.getCompanion().getAddress())) {
                LogUtil.logD(TAG, "[ProxyServiceDetector] UUID event for non-companion device");
                return;
            }

            mIsFetchingUuids = false;

            Parcelable[] uuidsParcelArray =
                intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
            if (uuidsParcelArray == null) {
                Log.e(TAG, "[ProxyServiceDetector] Null UUID array; handling aborted");
                return;
            }

            ParcelUuid[] uuids = new ParcelUuid[uuidsParcelArray.length];
            for (int i = 0; i < uuidsParcelArray.length; i++) {
                uuids[i] = (ParcelUuid) uuidsParcelArray[i];
            }

            LogUtil.logD(TAG, "[ProxyServiceDetector] Action UUIDs: " + uuidsToString(uuids));

            mUuids = uuids;

            removeSdpUuidReceiver();

            if (maybeUpdateVersion()) {
                mListener.onBackgroundProxyConfigUpdated("UUID list update");
            }
        }
    };

    private class V2DiscoveryClientListener implements GattDiscoveryClient.Listener {
        @Override
        public void onProxyConfigUpdate(int psmValue, int channelChangeId) {
            setV2Params(psmValue, channelChangeId);
        }
    }
}
