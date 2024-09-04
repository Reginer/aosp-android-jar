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

package android.net.thread;

import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.content.Context;
import android.os.RemoteException;

import com.android.net.module.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * Provides the primary API for managing app aspects of Thread network connectivity.
 *
 * @hide
 */
@FlaggedApi(ThreadNetworkFlags.FLAG_THREAD_ENABLED)
@SystemApi
@SystemService(ThreadNetworkManager.SERVICE_NAME)
public final class ThreadNetworkManager {
    /**
     * This value tracks {@link Context#THREAD_NETWORK_SERVICE}.
     *
     * <p>This is needed because at the time this service is created, it needs to support both
     * Android U and V but {@link Context#THREAD_NETWORK_SERVICE} Is only available on the V branch.
     *
     * <p>Note that this is not added to NetworkStack ConstantsShim because we need this constant in
     * the framework library while ConstantsShim is only linked against the service library.
     *
     * @hide
     */
    public static final String SERVICE_NAME = "thread_network";

    /**
     * This value tracks {@link PackageManager#FEATURE_THREAD_NETWORK}.
     *
     * <p>This is needed because at the time this service is created, it needs to support both
     * Android U and V but {@link PackageManager#FEATURE_THREAD_NETWORK} Is only available on the V
     * branch.
     *
     * <p>Note that this is not added to NetworkStack COnstantsShim because we need this constant in
     * the framework library while ConstantsShim is only linked against the service library.
     *
     * @hide
     */
    public static final String FEATURE_NAME = "android.hardware.thread_network";

    /**
     * Permission allows changing Thread network state and access to Thread network credentials such
     * as Network Key and PSKc.
     *
     * <p>This is the same value as android.Manifest.permission.THREAD_NETWORK_PRIVILEGED. That
     * symbol is not available on U while this feature needs to support Android U TV devices, so
     * here is making a copy of android.Manifest.permission.THREAD_NETWORK_PRIVILEGED.
     *
     * @hide
     */
    public static final String PERMISSION_THREAD_NETWORK_PRIVILEGED =
            "android.permission.THREAD_NETWORK_PRIVILEGED";

    /**
     * This user restriction specifies if Thread network is disallowed on the device. If Thread
     * network is disallowed it cannot be turned on via Settings.
     *
     * <p>this is a mirror of {@link UserManager#DISALLOW_THREAD_NETWORK} which is not available on
     * Android U devices.
     *
     * @hide
     */
    public static final String DISALLOW_THREAD_NETWORK = "no_thread_network";

    @NonNull private final Context mContext;
    @NonNull private final List<ThreadNetworkController> mUnmodifiableControllerServices;

    /**
     * Creates a new ThreadNetworkManager instance.
     *
     * @hide
     */
    public ThreadNetworkManager(
            @NonNull Context context, @NonNull IThreadNetworkManager managerService) {
        this(context, makeControllers(managerService));
    }

    private static List<ThreadNetworkController> makeControllers(
            @NonNull IThreadNetworkManager managerService) {
        requireNonNull(managerService, "managerService cannot be null");

        List<IThreadNetworkController> controllerServices;

        try {
            controllerServices = managerService.getAllThreadNetworkControllers();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
            return Collections.emptyList();
        }

        return CollectionUtils.map(controllerServices, ThreadNetworkController::new);
    }

    private ThreadNetworkManager(
            @NonNull Context context, @NonNull List<ThreadNetworkController> controllerServices) {
        mContext = context;
        mUnmodifiableControllerServices = Collections.unmodifiableList(controllerServices);
    }

    /** Returns the {@link ThreadNetworkController} object of all Thread networks. */
    @NonNull
    public List<ThreadNetworkController> getAllThreadNetworkControllers() {
        return mUnmodifiableControllerServices;
    }
}
