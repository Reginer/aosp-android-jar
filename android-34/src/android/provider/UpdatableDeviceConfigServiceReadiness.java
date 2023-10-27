package android.provider;

import android.annotation.SystemApi;

/**
 * This class reports the readiness of the updatable code to be used.
 * @hide
 */
@SystemApi
public final class UpdatableDeviceConfigServiceReadiness {

    private UpdatableDeviceConfigServiceReadiness() {
        // do not instantiate
    }

    /**
     * Returns true if the updatable service (part of mainline) is ready to be used.
     * Otherwise the platform shell service should be started.
     *
     * <p>see {@code com.android.providers.settings.DeviceConfigService}
     * <p>see {@code android.provider.DeviceConfig}
     *
     * @return true if the updatable code should be used
     */
    public static boolean shouldStartUpdatableService() {
        return false;
    }

}
