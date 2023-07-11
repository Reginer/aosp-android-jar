package com.android.clockwork.bluetooth.proxy;

import static com.android.clockwork.bluetooth.proxy.WearProxyConstants.PROXY_UUID_V1;
import static com.android.clockwork.bluetooth.proxy.WearProxyConstants.PROXY_UUID_V2;
import static com.android.clockwork.bluetooth.proxy.WearProxyConstants.PROXY_VERSION_V1;
import static com.android.clockwork.bluetooth.proxy.WearProxyConstants.PROXY_VERSION_V15;
import static com.android.clockwork.bluetooth.proxy.WearProxyConstants.PROXY_VERSION_V2;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.ParcelUuid;
import android.os.SystemProperties;
import android.util.Log;

import com.android.clockwork.bluetooth.WearBluetoothSettings;
import com.android.clockwork.common.LogUtil;
import com.android.clockwork.common.WearResourceUtil;

/**
 * Sysproxy version and related RFCOMM service UUID.
 *
 * Detects and operates on sysproxy versions: companion device RFCOMM service records discovery
 * and control/enabling of sysproxy version on local device.
 */
public class ProxyServiceVersion {
    private static final String TAG = WearBluetoothSettings.LOG_TAG;

    public static final ProxyServiceVersion DEFAULT_PROXY_SERVICE_VERSION =
            new ProxyServiceVersion(PROXY_VERSION_V1, PROXY_UUID_V1);

    public final int mVersionCode;
    public final ParcelUuid mRfcommServiceUuid;

    private ProxyServiceVersion(int versionCode, ParcelUuid rfcommServiceUuid) {
        mVersionCode = versionCode;
        mRfcommServiceUuid = rfcommServiceUuid;
    }

    /** Return true if sysproxy on this device can use more recent version that this one. */
    public boolean isUpgradeAvailable(Context context) {
        if (mVersionCode == PROXY_VERSION_V1) {
            return isSysproxyV2Enabled(context) || getSysproxyV15Uuid() != null;
        }
        if (mVersionCode  == PROXY_VERSION_V15) {
            return isSysproxyV2Enabled(context);
        }
        return false;
    }


    /** Return version sysproxy must use and available based on service record UUIDs. */
    public static ProxyServiceVersion detectVersion(Context context, ParcelUuid[] uuids) {
        if (isSysproxyV2Enabled(context) && hasUuid(uuids, PROXY_UUID_V2)) {
            return new ProxyServiceVersion(PROXY_VERSION_V2, PROXY_UUID_V2);
        }

        ParcelUuid service15Uuid = getSysproxyV15Uuid();
        if (service15Uuid != null && hasUuid(uuids, service15Uuid)) {
            return new ProxyServiceVersion(PROXY_VERSION_V15, service15Uuid);
        }

        return DEFAULT_PROXY_SERVICE_VERSION;
    }

    /** Return version sysproxy must use and available on companion device */
    public static ProxyServiceVersion detectVersion(Context context, BluetoothDevice device) {
        ParcelUuid[] uuidArray = device.getUuids();
        if (uuidArray == null) {
            Log.e(TAG, "[ProxyServiceVersion] failed get UUIDs from companion device.");
            return DEFAULT_PROXY_SERVICE_VERSION;
        }

        return detectVersion(context, uuidArray);
    }


    private static ParcelUuid getSysproxyV15Uuid() {
        String proxyService = SystemProperties.get("ro.cw.bt.proxy_service");
        return proxyService.isEmpty() ? null : ParcelUuid.fromString(proxyService);
    }

    private static boolean isSysproxyV2Enabled(Context context) {
        return WearResourceUtil.getWearableResources(context).getBoolean(
                          com.android.wearable.resources.R.bool.config_enableSysproxyV2);
    }

    private static boolean hasUuid(ParcelUuid[] uuidArray, ParcelUuid uuid) {
        for (ParcelUuid element : uuidArray) {
            if (element.equals(uuid)) {
                LogUtil.logD(TAG, "[ProxyServiceVersion] match " + uuid);
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "v" + mVersionCode + " " + mRfcommServiceUuid;
    }
}
