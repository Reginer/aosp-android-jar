/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.net;

import static android.net.BpfNetMapsConstants.CONFIGURATION_MAP_PATH;
import static android.net.BpfNetMapsConstants.DATA_SAVER_ENABLED_MAP_PATH;
import static android.net.BpfNetMapsConstants.UID_OWNER_MAP_PATH;

import android.annotation.NonNull;
import android.annotation.RequiresApi;
import android.os.Build;
import android.os.ServiceSpecificException;
import android.system.ErrnoException;

import com.android.internal.annotations.VisibleForTesting;
import com.android.modules.utils.build.SdkLevel;
import com.android.net.module.util.BpfMap;
import com.android.net.module.util.IBpfMap;
import com.android.net.module.util.Struct.S32;
import com.android.net.module.util.Struct.U32;
import com.android.net.module.util.Struct.U8;

/**
 * A helper class to *read* java BpfMaps for network stack.
 * BpfMap operations that are not used from network stack should be in
 * {@link com.android.server.BpfNetMaps}
 * @hide
 */
// NetworkStack can not use this before U due to b/326143935
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class NetworkStackBpfNetMaps {
    private static final String TAG = NetworkStackBpfNetMaps.class.getSimpleName();

    // Locally store the handle of bpf maps. The FileDescriptors are statically cached inside the
    // BpfMap implementation.

    // Bpf map to store various networking configurations, the format of the value is different
    // for different keys. See BpfNetMapsConstants#*_CONFIGURATION_KEY for keys.
    private final IBpfMap<S32, U32> mConfigurationMap;
    // Bpf map to store per uid traffic control configurations.
    // See {@link UidOwnerValue} for more detail.
    private final IBpfMap<S32, UidOwnerValue> mUidOwnerMap;
    private final IBpfMap<S32, U8> mDataSaverEnabledMap;
    private final Dependencies mDeps;

    private static class SingletonHolder {
        static final NetworkStackBpfNetMaps sInstance = new NetworkStackBpfNetMaps();
    }

    @NonNull
    public static NetworkStackBpfNetMaps getInstance() {
        return SingletonHolder.sInstance;
    }

    private NetworkStackBpfNetMaps() {
        this(new Dependencies());
    }

    // While the production code uses the singleton to optimize for performance and deal with
    // concurrent access, the test needs to use a non-static approach for dependency injection and
    // mocking virtual bpf maps.
    @VisibleForTesting
    public NetworkStackBpfNetMaps(@NonNull Dependencies deps) {
        if (!SdkLevel.isAtLeastT()) {
            throw new UnsupportedOperationException(
                    NetworkStackBpfNetMaps.class.getSimpleName()
                            + " is not supported below Android T");
        }
        mDeps = deps;
        mConfigurationMap = mDeps.getConfigurationMap();
        mUidOwnerMap = mDeps.getUidOwnerMap();
        mDataSaverEnabledMap = mDeps.getDataSaverEnabledMap();
    }

    /**
     * Dependencies of BpfNetMapReader, for injection in tests.
     */
    @VisibleForTesting
    public static class Dependencies {
        /** Get the configuration map. */
        public IBpfMap<S32, U32> getConfigurationMap() {
            try {
                return new BpfMap<>(CONFIGURATION_MAP_PATH, BpfMap.BPF_F_RDONLY,
                        S32.class, U32.class);
            } catch (ErrnoException e) {
                throw new IllegalStateException("Cannot open configuration map", e);
            }
        }

        /** Get the uid owner map. */
        public IBpfMap<S32, UidOwnerValue> getUidOwnerMap() {
            try {
                return new BpfMap<>(UID_OWNER_MAP_PATH, BpfMap.BPF_F_RDONLY,
                        S32.class, UidOwnerValue.class);
            } catch (ErrnoException e) {
                throw new IllegalStateException("Cannot open uid owner map", e);
            }
        }

        /** Get the data saver enabled map. */
        public  IBpfMap<S32, U8> getDataSaverEnabledMap() {
            try {
                return new BpfMap<>(DATA_SAVER_ENABLED_MAP_PATH, BpfMap.BPF_F_RDONLY, S32.class,
                        U8.class);
            } catch (ErrnoException e) {
                throw new IllegalStateException("Cannot open data saver enabled map", e);
            }
        }
    }

    /**
     * Get the specified firewall chain's status.
     *
     * @param chain target chain
     * @return {@code true} if chain is enabled, {@code false} if chain is not enabled.
     * @throws UnsupportedOperationException if called on pre-T devices.
     * @throws ServiceSpecificException in case of failure, with an error code indicating the
     *                                  cause of the failure.
     */
    public boolean isChainEnabled(final int chain) {
        return BpfNetMapsUtils.isChainEnabled(mConfigurationMap, chain);
    }

    /**
     * Get firewall rule of specified firewall chain on specified uid.
     *
     * @param chain target chain
     * @param uid        target uid
     * @return either {@link ConnectivityManager#FIREWALL_RULE_ALLOW} or
     *         {@link ConnectivityManager#FIREWALL_RULE_DENY}.
     * @throws UnsupportedOperationException if called on pre-T devices.
     * @throws ServiceSpecificException in case of failure, with an error code indicating the
     *                                  cause of the failure.
     */
    public int getUidRule(final int chain, final int uid) {
        return BpfNetMapsUtils.getUidRule(mUidOwnerMap, chain, uid);
    }

    /**
     * Return whether the network is blocked by firewall chains for the given uid.
     *
     * Note that {@link #getDataSaverEnabled()} has a latency before V.
     *
     * @param uid The target uid.
     * @param isNetworkMetered Whether the target network is metered.
     *
     * @return True if the network is blocked. Otherwise, false.
     * @throws ServiceSpecificException if the read fails.
     *
     * @hide
     */
    public boolean isUidNetworkingBlocked(final int uid, boolean isNetworkMetered) {
        return BpfNetMapsUtils.isUidNetworkingBlocked(uid, isNetworkMetered,
                mConfigurationMap, mUidOwnerMap, mDataSaverEnabledMap);
    }

    /**
     * Get Data Saver enabled or disabled
     *
     * Note that before V, the data saver status in bpf is written by ConnectivityService
     * when receiving {@link ConnectivityManager#ACTION_RESTRICT_BACKGROUND_CHANGED}. Thus,
     * the status is not synchronized.
     * On V+, the data saver status is set by platform code when enabling/disabling
     * data saver, which is synchronized.
     *
     * @return whether Data Saver is enabled or disabled.
     * @throws ServiceSpecificException in case of failure, with an error code indicating the
     *                                  cause of the failure.
     */
    public boolean getDataSaverEnabled() {
        return BpfNetMapsUtils.getDataSaverEnabled(mDataSaverEnabledMap);
    }
}
