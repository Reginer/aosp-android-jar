package com.android.clockwork.bluetooth.proxy;

import android.os.ParcelUuid;
import android.os.SystemProperties;

import java.util.Objects;

/**
 * Parameters for establishing L2CAP or RFCOMM connection to Phone side SysProxy.
 */
public final class ProxyServiceConfig {
    public enum Type {
        UNKNOWN,
        V1_ANDROID,
        V15_ANDROID,
        V2_ANDROID,
        V1_IOS,
    }

    private static final int AIDL_TYPE_RFCOMM = 1;
    private static final int AIDL_TYPE_L2CAP_LE = 4;

    private static final int AIDL_SEC_FLAG_ENCRYPT = 1 << 0;
    private static final int AIDL_SEC_FLAG_AUTH = 1 << 1;

    private static final int JNI_PROXY_VERSION_V1 = 1;
    private static final int JNI_PROXY_VERSION_V15 = 5; // v1.5

    public final Type type;
    public final int connectionPort;
    public final int channelChangeId;
    public final ParcelUuid serviceUuid;

    public static ProxyServiceConfig forAndroidV1() {
        return new ProxyServiceConfig(Type.V1_ANDROID, 0, 0, WearProxyConstants.PROXY_UUID_V1);
    }

    public static ProxyServiceConfig forAndroidV15(ParcelUuid serviceUuid) {
        return new ProxyServiceConfig(Type.V15_ANDROID, 0, 0, serviceUuid);
    }

    public static ProxyServiceConfig forAndroidV2(int connectionPort, int channelChangeId) {
        return new ProxyServiceConfig(Type.V2_ANDROID, connectionPort, channelChangeId,
            WearProxyConstants.PROXY_UUID_V1);
    }

    public static ProxyServiceConfig forIosV1(int connectionPort, int channelChangeId) {
        return new ProxyServiceConfig(Type.V1_IOS, connectionPort, channelChangeId,
            WearProxyConstants.PROXY_UUID_V1);
    }

    public ProxyServiceConfig() {
        this(Type.UNKNOWN, 0, 0, null);
    }

    private ProxyServiceConfig(Type type, int connectionPort,
            int channelChangeId, ParcelUuid serviceUuid) {
        this.type = type;
        this.connectionPort = connectionPort;
        this.channelChangeId = channelChangeId;
        this.serviceUuid = serviceUuid;
    }

    public boolean isIos() {
        return (type == Type.V1_IOS);
    }

    public int getAidlConnectionType() {
        switch (type) {
            case V2_ANDROID:
            case V1_IOS:
                return AIDL_TYPE_L2CAP_LE;
            default:
                return AIDL_TYPE_RFCOMM;
        }
    }

    public int getAidlConnectionFlags() {
        return isIos() ? 0 : (AIDL_SEC_FLAG_AUTH | AIDL_SEC_FLAG_ENCRYPT);
    }

    public int getJniSysproxyVersion() {
        return (type == Type.V15_ANDROID ? JNI_PROXY_VERSION_V15 : JNI_PROXY_VERSION_V1);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProxyServiceConfig)) {
            return false;
        }
        ProxyServiceConfig other = (ProxyServiceConfig) obj;
        return (type == other.type && connectionPort == other.connectionPort
                && channelChangeId == other.channelChangeId
                && equals(serviceUuid, other.serviceUuid));
    }

    private static boolean equals(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        return obj1.equals(obj2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, connectionPort, channelChangeId, serviceUuid);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{type=");
        sb.append(type);
        if (connectionPort != 0) {
            sb.append(",port=");
            sb.append(connectionPort);
        }
        if (channelChangeId != 0) {
            sb.append(",change=");
            sb.append(channelChangeId);
        }
        if (serviceUuid != null) {
            sb.append(",uuid=");
            sb.append(serviceUuid);
        }
        sb.append('}');
        return sb.toString();
    }
}
