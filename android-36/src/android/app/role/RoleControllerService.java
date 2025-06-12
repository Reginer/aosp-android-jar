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
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.annotation.WorkerThread;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.UserHandle;
import android.permission.flags.Flags;
import android.util.Log;

import com.android.internal.util.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Abstract base class for the role controller service.
 * <p>
 * Subclass should implement the business logic for role management, including enforcing role
 * requirements and granting or revoking relevant privileges of roles. This class can only be
 * implemented by the permission controller app which is registered in {@code PackageManager}.
 *
 * @deprecated The role controller service is an internal implementation detail inside role, and it
 *             may be replaced by other mechanisms in the future and no longer be called.
 *
 * @hide
 */
@Deprecated
@SystemApi
public abstract class RoleControllerService extends Service {
    private static final String LOG_TAG = RoleControllerService.class.getSimpleName();

    /**
     * The {@link Intent} that must be declared as handled by the service.
     */
    public static final String SERVICE_INTERFACE = "android.app.role.RoleControllerService";

    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        mWorkerThread = new HandlerThread(RoleControllerService.class.getSimpleName());
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mWorkerThread.quitSafely();
    }

    @Nullable
    @Override
    public final IBinder onBind(@Nullable Intent intent) {
        return new IRoleController.Stub() {

            @Override
            public void grantDefaultRoles(RemoteCallback callback) {
                enforceCallerSystemUid("grantDefaultRoles");
                Objects.requireNonNull(callback, "callback cannot be null");

                mWorkerHandler.post(() -> RoleControllerService.this.grantDefaultRoles(callback));
            }

            @Override
            public void onAddRoleHolder(String roleName, String packageName, int flags,
                    RemoteCallback callback) {
                enforceCallerSystemUid("onAddRoleHolder");
                Objects.requireNonNull(callback, "callback cannot be null");

                mWorkerHandler.post(() -> RoleControllerService.this.onAddRoleHolder(roleName,
                        packageName, flags, callback));
            }

            @Override
            public void onRemoveRoleHolder(String roleName, String packageName, int flags,
                    RemoteCallback callback) {
                enforceCallerSystemUid("onRemoveRoleHolder");
                Objects.requireNonNull(callback, "callback cannot be null");

                mWorkerHandler.post(() -> RoleControllerService.this.onRemoveRoleHolder(roleName,
                        packageName, flags, callback));
            }

            @Override
            public void onClearRoleHolders(String roleName, int flags, RemoteCallback callback) {
                enforceCallerSystemUid("onClearRoleHolders");
                Objects.requireNonNull(callback, "callback cannot be null");

                mWorkerHandler.post(() -> RoleControllerService.this.onClearRoleHolders(roleName,
                        flags, callback));
            }

            private void enforceCallerSystemUid(@NonNull String methodName) {
                if (Binder.getCallingUid() != Process.SYSTEM_UID) {
                    throw new SecurityException("Only the system process can call " + methodName
                            + "()");
                }
            }

            @Override
            public void isApplicationQualifiedForRole(String roleName, String packageName,
                    RemoteCallback callback) {
                enforceCallingPermission(Manifest.permission.MANAGE_ROLE_HOLDERS, null);
                Objects.requireNonNull(callback, "callback cannot be null");

                Bundle result = new Bundle();
                try {
                    Preconditions.checkStringNotEmpty(roleName, "roleName cannot be null or empty");
                    Preconditions.checkStringNotEmpty(packageName,
                            "packageName cannot be null or empty");
                    boolean qualified = onIsApplicationQualifiedForRole(roleName, packageName);
                    result.putBoolean(RoleControllerManager.KEY_RESULT, qualified);
                } catch (Exception e) {
                    result.putSerializable(RoleControllerManager.KEY_EXCEPTION, e);
                }
                callback.sendResult(result);
            }

            @Override
            public void isApplicationVisibleForRole(String roleName, String packageName,
                    RemoteCallback callback) {
                enforceCallingPermission(Manifest.permission.MANAGE_ROLE_HOLDERS, null);
                Objects.requireNonNull(callback, "callback cannot be null");

                Bundle result = new Bundle();
                try {
                    Preconditions.checkStringNotEmpty(roleName, "roleName cannot be null or empty");
                    Preconditions.checkStringNotEmpty(packageName,
                            "packageName cannot be null or empty");
                    boolean visible = onIsApplicationVisibleForRole(roleName, packageName);
                    result.putBoolean(RoleControllerManager.KEY_RESULT, visible);
                } catch (Exception e) {
                    result.putSerializable(RoleControllerManager.KEY_EXCEPTION, e);
                }
                callback.sendResult(result);
            }

            @Override
            public void isRoleVisible(String roleName, RemoteCallback callback) {
                enforceCallingPermission(Manifest.permission.MANAGE_ROLE_HOLDERS, null);
                Objects.requireNonNull(callback, "callback cannot be null");

                Bundle result = new Bundle();
                try {
                    Preconditions.checkStringNotEmpty(roleName, "roleName cannot be null or empty");
                    boolean visible = onIsRoleVisible(roleName);
                    result.putBoolean(RoleControllerManager.KEY_RESULT, visible);
                } catch (Exception e) {
                    result.putSerializable(RoleControllerManager.KEY_EXCEPTION, e);
                }
                callback.sendResult(result);
            }

            @Override
            public void getLegacyFallbackDisabledRoles(RemoteCallback callback) {
                enforceCallerSystemUid("getLegacyFallbackDisabledRoles");

                Objects.requireNonNull(callback, "callback cannot be null");

                Bundle result = new Bundle();
                try {
                    List<String> legacyFallbackDisabledRoles = onGetLegacyFallbackDisabledRoles();
                    result.putStringArrayList(RoleControllerManager.KEY_RESULT,
                            new ArrayList<>(legacyFallbackDisabledRoles));
                } catch (Exception e) {
                    result.putSerializable(RoleControllerManager.KEY_EXCEPTION, e);
                }
                callback.sendResult(result);
            }
        };
    }

    private void grantDefaultRoles(RemoteCallback callback) {
        Bundle result = new Bundle();
        try {
            boolean successful = onGrantDefaultRoles();
            result.putBoolean(RoleControllerManager.KEY_RESULT, successful);
        } catch (Exception e) {
            result.putSerializable(RoleControllerManager.KEY_EXCEPTION, e);
        }
        callback.sendResult(result);
    }

    private void onAddRoleHolder(@NonNull String roleName, @NonNull String packageName,
            @RoleManager.ManageHoldersFlags int flags, RemoteCallback callback) {
        Bundle result = new Bundle();
        try {
            Preconditions.checkStringNotEmpty(roleName, "roleName cannot be null or empty");
            Preconditions.checkStringNotEmpty(packageName,
                    "packageName cannot be null or empty");
            boolean successful = onAddRoleHolder(roleName, packageName, flags);
            result.putBoolean(RoleControllerManager.KEY_RESULT, successful);
        } catch (Exception e) {
            result.putSerializable(RoleControllerManager.KEY_EXCEPTION, e);
        }
        callback.sendResult(result);
    }

    private void onRemoveRoleHolder(@NonNull String roleName, @NonNull String packageName,
            @RoleManager.ManageHoldersFlags int flags, RemoteCallback callback) {
        Bundle result = new Bundle();
        try {
            Preconditions.checkStringNotEmpty(roleName, "roleName cannot be null or empty");
            Preconditions.checkStringNotEmpty(packageName,
                    "packageName cannot be null or empty");
            boolean successful = onRemoveRoleHolder(roleName, packageName, flags);
            result.putBoolean(RoleControllerManager.KEY_RESULT, successful);
        } catch (Exception e) {
            result.putSerializable(RoleControllerManager.KEY_EXCEPTION, e);
        }
        callback.sendResult(result);
    }

    private void onClearRoleHolders(@NonNull String roleName,
            @RoleManager.ManageHoldersFlags int flags, RemoteCallback callback) {
        Bundle result = new Bundle();
        try {
            Preconditions.checkStringNotEmpty(roleName, "roleName cannot be null or empty");
            boolean successful = onClearRoleHolders(roleName, flags);
            result.putBoolean(RoleControllerManager.KEY_RESULT, successful);
        } catch (Exception e) {
            result.putSerializable(RoleControllerManager.KEY_EXCEPTION, e);
        }
        callback.sendResult(result);
    }

    /**
     * Called by system to grant default permissions and roles.
     * <p>
     * This is typically when creating a new user or upgrading either system or
     * permission controller package
     *
     * @return whether this call was successful
     */
    @WorkerThread
    public abstract boolean onGrantDefaultRoles();

    /**
     * Add a specific application to the holders of a role. If the role is exclusive, the previous
     * holder will be replaced.
     * <p>
     * Implementation should enforce the role requirements and grant or revoke the relevant
     * privileges of roles.
     *
     * @param roleName the name of the role to add the role holder for
     * @param packageName the package name of the application to add to the role holders
     * @param flags optional behavior flags
     *
     * @return whether this call was successful
     *
     * @see RoleManager#addRoleHolderAsUser(String, String, int, UserHandle, Executor,
     *      RemoteCallback)
     */
    @WorkerThread
    public abstract boolean onAddRoleHolder(@NonNull String roleName, @NonNull String packageName,
            @RoleManager.ManageHoldersFlags int flags);

    /**
     * Remove a specific application from the holders of a role.
     *
     * @param roleName the name of the role to remove the role holder for
     * @param packageName the package name of the application to remove from the role holders
     * @param flags optional behavior flags
     *
     * @return whether this call was successful
     *
     * @see RoleManager#removeRoleHolderAsUser(String, String, int, UserHandle, Executor,
     *      RemoteCallback)
     */
    @WorkerThread
    public abstract boolean onRemoveRoleHolder(@NonNull String roleName,
            @NonNull String packageName, @RoleManager.ManageHoldersFlags int flags);

    /**
     * Remove all holders of a role.
     *
     * @param roleName the name of the role to remove role holders for
     * @param flags optional behavior flags
     *
     * @return whether this call was successful
     *
     * @see RoleManager#clearRoleHoldersAsUser(String, int, UserHandle, Executor, RemoteCallback)
     */
    @WorkerThread
    public abstract boolean onClearRoleHolders(@NonNull String roleName,
            @RoleManager.ManageHoldersFlags int flags);

    /**
     * Check whether an application is qualified for a role.
     *
     * @param roleName name of the role to check for
     * @param packageName package name of the application to check for
     *
     * @return whether the application is qualified for the role
     *
     * @deprecated Implement {@link #onIsApplicationVisibleForRole(String, String)} instead.
     */
    @Deprecated
    public abstract boolean onIsApplicationQualifiedForRole(@NonNull String roleName,
            @NonNull String packageName);

    /**
     * Check whether an application is visible for a role.
     *
     * While an application can be qualified for a role, it can still stay hidden from user (thus
     * not visible). If an application is visible for a role, we may show things related to the role
     * for it, e.g. showing an entry pointing to the role settings in its application info page.
     *
     * @param roleName name of the role to check for
     * @param packageName package name of the application to check for
     *
     * @return whether the application is visible for the role
     */
    public boolean onIsApplicationVisibleForRole(@NonNull String roleName,
            @NonNull String packageName) {
        return onIsApplicationQualifiedForRole(roleName, packageName);
    }

    /**
     * Check whether a role should be visible to user.
     *
     * @param roleName name of the role to check for
     *
     * @return whether the role should be visible to user
     */
    public abstract boolean onIsRoleVisible(@NonNull String roleName);

    /**
     * Get the legacy fallback disabled state.
     *
     * @return A list of role names with disabled fallback state.
     */
    @FlaggedApi(Flags.FLAG_SYSTEM_SERVER_ROLE_CONTROLLER_ENABLED)
    @NonNull
    public List<String> onGetLegacyFallbackDisabledRoles() {
        Log.wtf(LOG_TAG, "onGetLegacyFallbackDisabledRoles is unsupported by this version of"
                + " PermissionController");
        throw new UnsupportedOperationException();
    }
}
