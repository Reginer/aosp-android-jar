/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.app.role;

import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteCallback;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.infra.AndroidFuture;
import com.android.internal.infra.ServiceConnector;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Interface for communicating with the role controller.
 *
 * @hide
 */
public class RoleControllerManager {

    /**
     * Bundle key for the payload of RoleController APIs
     */
    public static final String KEY_RESULT = RoleControllerManager.class.getName() + ".key.RESULT";

    /**
     * Bundle key for the error of RoleController APIs
     */
    public static final String KEY_EXCEPTION = RoleControllerManager.class.getName()
            + ".key.EXCEPTION";

    private static final String LOG_TAG = RoleControllerManager.class.getSimpleName();

    private static final long REQUEST_TIMEOUT_MILLIS = 15 * 1000;

    private static volatile ComponentName sRemoteServiceComponentName;

    private static final Object sRemoteServicesLock = new Object();

    /**
     * Global remote services (per user) used by all {@link RoleControllerManager managers}.
     */
    @GuardedBy("sRemoteServicesLock")
    private static final SparseArray<ServiceConnector<IRoleController>> sRemoteServices =
            new SparseArray<>();

    @NonNull
    private final ServiceConnector<IRoleController> mRemoteService;

    /**
     * Initialize the remote service component name once so that we can avoid acquiring the
     * PackageManagerService lock in constructor.
     *
     * @see #createWithInitializedRemoteServiceComponentName(Handler, Context)
     *
     * @hide
     */
    public static void initializeRemoteServiceComponentName(@NonNull Context context) {
        sRemoteServiceComponentName = getRemoteServiceComponentName(context);
    }

    /**
     * Create a {@link RoleControllerManager} instance with the initialized remote service component
     * name so that we can avoid acquiring the PackageManagerService lock in constructor.
     *
     * @see #initializeRemoteServiceComponentName(Context)
     *
     * @hide
     */
    @NonNull
    public static RoleControllerManager createWithInitializedRemoteServiceComponentName(
            @NonNull Handler handler, @NonNull Context context) {
        return new RoleControllerManager(sRemoteServiceComponentName, handler, context);
    }

    private RoleControllerManager(@NonNull ComponentName remoteServiceComponentName,
            @NonNull Handler handler, @NonNull Context context) {
        synchronized (sRemoteServicesLock) {
            int userId = context.getUser().getIdentifier();
            ServiceConnector<IRoleController> remoteService = sRemoteServices.get(userId);
            if (remoteService == null) {
                remoteService = new ServiceConnector.Impl<IRoleController>(
                        context.getApplicationContext(),
                        new Intent(RoleControllerService.SERVICE_INTERFACE)
                                .setComponent(remoteServiceComponentName),
                        0 /* bindingFlags */, userId, IRoleController.Stub::asInterface) {

                    @Override
                    protected Handler getJobHandler() {
                        return handler;
                    }
                };
                sRemoteServices.put(userId, remoteService);
            }
            mRemoteService = remoteService;
        }
    }

    /**
     * @hide
     */
    public RoleControllerManager(@NonNull Context context) {
        this(getRemoteServiceComponentName(context), new Handler(Looper.getMainLooper()), context);
    }

    @NonNull
    private static ComponentName getRemoteServiceComponentName(@NonNull Context context) {
        Intent intent = new Intent(RoleControllerService.SERVICE_INTERFACE);
        PackageManager packageManager = context.getPackageManager();
        intent.setPackage(packageManager.getPermissionControllerPackageName());
        ServiceInfo serviceInfo = packageManager.resolveService(intent, 0).serviceInfo;
        return new ComponentName(serviceInfo.packageName, serviceInfo.name);
    }

    /**
     * @see RoleControllerService#onGrantDefaultRoles()
     *
     * @hide
     */
    public void grantDefaultRoles(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<Boolean> callback) {
        AndroidFuture<Boolean> operation = mRemoteService.postAsync(service -> {
            AndroidFuture<Boolean> future = new AndroidFuture<>();
            service.grantDefaultRoles(createBooleanRemoteCallback(future));
            return future;
        });
        propagateCallback(operation, "grantDefaultRoles", executor, callback);
    }

    /**
     * @see RoleControllerService#onAddRoleHolder(String, String, int)
     *
     * @hide
     */
    public void onAddRoleHolder(@NonNull String roleName, @NonNull String packageName,
            @RoleManager.ManageHoldersFlags int flags, @NonNull RemoteCallback callback) {
        AndroidFuture<Boolean> operation = mRemoteService.postAsync(service -> {
            AndroidFuture<Boolean> future = new AndroidFuture<>();
            service.onAddRoleHolder(roleName, packageName, flags,
                    createBooleanRemoteCallback(future));
            return future;
        });
        propagateCallback(operation, "onAddRoleHolder", callback);
    }

    /**
     * @see RoleControllerService#onRemoveRoleHolder(String, String, int)
     *
     * @hide
     */
    public void onRemoveRoleHolder(@NonNull String roleName, @NonNull String packageName,
            @RoleManager.ManageHoldersFlags int flags, @NonNull RemoteCallback callback) {
        AndroidFuture<Boolean> operation = mRemoteService.postAsync(service -> {
            AndroidFuture<Boolean> future = new AndroidFuture<>();
            service.onRemoveRoleHolder(roleName, packageName, flags,
                    createBooleanRemoteCallback(future));
            return future;
        });
        propagateCallback(operation, "onRemoveRoleHolder", callback);
    }

    /**
     * @see RoleControllerService#onClearRoleHolders(String, int)
     *
     * @hide
     */
    public void onClearRoleHolders(@NonNull String roleName,
            @RoleManager.ManageHoldersFlags int flags, @NonNull RemoteCallback callback) {
        AndroidFuture<Boolean> operation = mRemoteService.postAsync(service -> {
            AndroidFuture<Boolean> future = new AndroidFuture<>();
            service.onClearRoleHolders(roleName, flags, createBooleanRemoteCallback(future));
            return future;
        });
        propagateCallback(operation, "onClearRoleHolders", callback);
    }

    /**
     * @see RoleControllerService#onIsApplicationVisibleForRole(String, String)
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.MANAGE_ROLE_HOLDERS)
    public void isApplicationVisibleForRole(@NonNull String roleName, @NonNull String packageName,
            @NonNull @CallbackExecutor Executor executor, @NonNull Consumer<Boolean> callback) {
        AndroidFuture<Boolean> operation = mRemoteService.postAsync(service -> {
            AndroidFuture<Boolean> future = new AndroidFuture<>();
            service.isApplicationVisibleForRole(roleName, packageName,
                    createBooleanRemoteCallback(future));
            return future;
        });
        propagateCallback(operation, "isApplicationVisibleForRole", executor, callback);
    }

    /**
     * @see RoleControllerService#onIsRoleVisible(String)
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.MANAGE_ROLE_HOLDERS)
    public void isRoleVisible(@NonNull String roleName,
            @NonNull @CallbackExecutor Executor executor, @NonNull Consumer<Boolean> callback) {
        AndroidFuture<Boolean> operation = mRemoteService.postAsync(service -> {
            AndroidFuture<Boolean> future = new AndroidFuture<>();
            service.isRoleVisible(roleName, createBooleanRemoteCallback(future));
            return future;
        });
        propagateCallback(operation, "isRoleVisible", executor, callback);
    }

    /**
     * @see RoleControllerService#onGrantDefaultRoles()
     *
     * @hide
     */
    public void getLegacyFallbackDisabledRoles(@NonNull @CallbackExecutor Executor executor,
            @NonNull Consumer<List<String>> callback) {
        mRemoteService.postAsync(service -> {
            AndroidFuture<List<String>> future = new AndroidFuture<>();
            service.getLegacyFallbackDisabledRoles(new RemoteCallback(result -> {
                Exception exception = (Exception) result.getSerializable(KEY_EXCEPTION);
                if (exception != null) {
                    future.completeExceptionally(exception);
                } else {
                    future.complete(result.getStringArrayList(KEY_RESULT));
                }
            }));
            return future;
        }).orTimeout(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .whenComplete((res, err) -> executor.execute(() -> {
                    final long token = Binder.clearCallingIdentity();
                    try {
                        if (err != null) {
                            Log.e(LOG_TAG, "Error calling getLegacyFallbackDisabledRoles()",
                                    err);
                            callback.accept(null);
                        } else {
                            callback.accept(res);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                }));
    }

    @NonNull
    private RemoteCallback createBooleanRemoteCallback(@NonNull AndroidFuture<Boolean> future) {
        return new RemoteCallback(result -> {
            Exception exception = (Exception) result.getSerializable(KEY_EXCEPTION);
            if (exception != null) {
                future.completeExceptionally(exception);
            } else {
                future.complete(result.getBoolean(KEY_RESULT));
            }
        });
    }

    private void propagateCallback(AndroidFuture<Boolean> operation, String opName,
            @CallbackExecutor @NonNull Executor executor,
            Consumer<Boolean> destination) {
        operation.orTimeout(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .whenComplete((res, err) -> executor.execute(() -> {
                    final long token = Binder.clearCallingIdentity();
                    try {
                        if (err != null) {
                            Log.e(LOG_TAG, "Error calling " + opName + "()", err);
                            destination.accept(false);
                        } else {
                            destination.accept(res);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                }));
    }

    private void propagateCallback(AndroidFuture<Boolean> operation, String opName,
            RemoteCallback destination) {
        operation.orTimeout(REQUEST_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .whenComplete((res, err) -> {
                    final long token = Binder.clearCallingIdentity();
                    try {
                        if (err != null) {
                            Log.e(LOG_TAG, "Error calling " + opName + "()", err);
                            destination.sendResult(null);
                        } else {
                            destination.sendResult(res ? Bundle.EMPTY : null);
                        }
                    } finally {
                        Binder.restoreCallingIdentity(token);
                    }
                });
    }
}
