/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.security.advancedprotection;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.os.UserManager.DISALLOW_CELLULAR_2G;
import static android.os.UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY;

import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserManager;
import android.security.Flags;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * <p>Advanced Protection is a mode that users can enroll their device into, that enhances security
 * by enabling features and restrictions across both the platform and user apps.
 *
 * <p>This class provides methods to query and control the advanced protection mode
 * for the device.
 */
@FlaggedApi(Flags.FLAG_AAPM_API)
@SystemService(Context.ADVANCED_PROTECTION_SERVICE)
public final class AdvancedProtectionManager {
    private static final String TAG = "AdvancedProtectionMgr";
    private static final String PKG_SETTINGS = "com.android.settings";

    //TODO(b/378931989): Switch to android.app.admin.DevicePolicyIdentifiers.MEMORY_TAGGING_POLICY
    //when the appropriate flag is launched.
    private static final String MEMORY_TAGGING_POLICY = "memoryTagging";

    /**
     * Advanced Protection's identifier for setting policies or restrictions in
     * {@link DevicePolicyManager}.
     *
     * @hide */
    public static final String ADVANCED_PROTECTION_SYSTEM_ENTITY =
            "android.security.advancedprotection";

    /**
     * Feature identifier for disallowing connections to 2G networks.
     *
     * @see UserManager#DISALLOW_CELLULAR_2G
     * @hide */
    @SystemApi
    public static final int FEATURE_ID_DISALLOW_CELLULAR_2G = 0;

    /**
     * Feature identifier for disallowing installs of apps from unknown sources.
     *
     * @see UserManager#DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY
     * @hide */
    @SystemApi
    public static final int FEATURE_ID_DISALLOW_INSTALL_UNKNOWN_SOURCES = 1;

    /**
     * Feature identifier for disallowing USB connections.
     *
     * @hide */
    @SystemApi
    public static final int FEATURE_ID_DISALLOW_USB = 2;

    /**
     * Feature identifier for disallowing connections to Wi-Fi Wired Equivalent Privacy (WEP)
     * networks.
     *
     * @see WifiManager#isWepSupported()
     * @hide */
    @SystemApi
    public static final int FEATURE_ID_DISALLOW_WEP = 3;

    /**
     * Feature identifier for enabling the Memory Tagging Extension (MTE). MTE is a CPU extension
     * that allows to protect against certain classes of security problems at a small runtime
     * performance cost overhead.
     *
     * @see DevicePolicyManager#setMtePolicy(int)
     * @hide */
    @SystemApi
    public static final int FEATURE_ID_ENABLE_MTE = 4;

    /** @hide */
    @IntDef(prefix = { "FEATURE_ID_" }, value = {
            FEATURE_ID_DISALLOW_CELLULAR_2G,
            FEATURE_ID_DISALLOW_INSTALL_UNKNOWN_SOURCES,
            FEATURE_ID_DISALLOW_USB,
            FEATURE_ID_DISALLOW_WEP,
            FEATURE_ID_ENABLE_MTE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FeatureId {}

    /** @hide */
    public static String featureIdToString(@FeatureId int featureId) {
        return switch(featureId) {
            case FEATURE_ID_DISALLOW_CELLULAR_2G -> "DISALLOW_CELLULAR_2G";
            case FEATURE_ID_DISALLOW_INSTALL_UNKNOWN_SOURCES -> "DISALLOW_INSTALL_UNKNOWN_SOURCES";
            case FEATURE_ID_DISALLOW_USB -> "DISALLOW_USB";
            case FEATURE_ID_DISALLOW_WEP -> "DISALLOW_WEP";
            case FEATURE_ID_ENABLE_MTE -> "ENABLE_MTE";
            default -> "UNKNOWN";
        };
    }

    private static final Set<Integer> ALL_FEATURE_IDS = Set.of(
            FEATURE_ID_DISALLOW_CELLULAR_2G,
            FEATURE_ID_DISALLOW_INSTALL_UNKNOWN_SOURCES,
            FEATURE_ID_DISALLOW_USB,
            FEATURE_ID_DISALLOW_WEP,
            FEATURE_ID_ENABLE_MTE);

    /**
     * Activity Action: Show a dialog with disabled by advanced protection message.
     * <p> If a user action or a setting toggle is disabled by advanced protection, this dialog can
     * be triggered to let the user know about this.
     * <p>
     * Input:
     * <p>{@link #EXTRA_SUPPORT_DIALOG_FEATURE}: The feature identifier.
     * <p>{@link #EXTRA_SUPPORT_DIALOG_TYPE}: The type of the action.
     * <p>
     * Output: Nothing.
     *
     * @hide */
    public static final String ACTION_SHOW_ADVANCED_PROTECTION_SUPPORT_DIALOG =
            "android.security.advancedprotection.action.SHOW_ADVANCED_PROTECTION_SUPPORT_DIALOG";

    /**
     * An int extra used with {@link #createSupportIntent} to identify the feature that needs to
     * show a support dialog explaining it was disabled by advanced protection.
     *
     * @hide */
    @FeatureId
    public static final String EXTRA_SUPPORT_DIALOG_FEATURE =
            "android.security.advancedprotection.extra.SUPPORT_DIALOG_FEATURE";

    /**
     * An int extra used with {@link #createSupportIntent} to identify the type of the action that
     * needs to be explained in the support dialog.
     *
     * @hide */
    @SupportDialogType
    public static final String EXTRA_SUPPORT_DIALOG_TYPE =
            "android.security.advancedprotection.extra.SUPPORT_DIALOG_TYPE";

    /**
     * Type for {@link #EXTRA_SUPPORT_DIALOG_TYPE} indicating an unknown action was blocked by
     * advanced protection, hence the support dialog should display a default explanation.
     *
     * @hide */
    public static final int SUPPORT_DIALOG_TYPE_UNKNOWN = 0;

    /**
     * Type for {@link #EXTRA_SUPPORT_DIALOG_TYPE} indicating a user performed an action that was
     * blocked by advanced protection.
     *
     * @hide */
    public static final int SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION = 1;

    /**
     * Type for {@link #EXTRA_SUPPORT_DIALOG_TYPE} indicating a user pressed on a setting toggle
     * that was disabled by advanced protection.
     *
     * @hide */
    public static final int SUPPORT_DIALOG_TYPE_DISABLED_SETTING = 2;

    /** @hide */
    @IntDef(prefix = { "SUPPORT_DIALOG_TYPE_" }, value = {
            SUPPORT_DIALOG_TYPE_UNKNOWN,
            SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION,
            SUPPORT_DIALOG_TYPE_DISABLED_SETTING,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SupportDialogType {}

    /** @hide */
    public static String supportDialogTypeToString(@SupportDialogType int type) {
        return switch(type) {
            case SUPPORT_DIALOG_TYPE_UNKNOWN -> "UNKNOWN";
            case SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION -> "BLOCKED_INTERACTION";
            case SUPPORT_DIALOG_TYPE_DISABLED_SETTING -> "DISABLED_SETTING";
            default -> "UNKNOWN";
        };
    }

    private static final Set<Integer> ALL_SUPPORT_DIALOG_TYPES = Set.of(
            SUPPORT_DIALOG_TYPE_UNKNOWN,
            SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION,
            SUPPORT_DIALOG_TYPE_DISABLED_SETTING);

    private final ConcurrentHashMap<Callback, IAdvancedProtectionCallback>
            mCallbackMap = new ConcurrentHashMap<>();

    @NonNull
    private final IAdvancedProtectionService mService;

    /** @hide */
    public AdvancedProtectionManager(@NonNull IAdvancedProtectionService service) {
        mService = service;
    }

    /**
     * Checks if advanced protection is enabled on the device.
     *
     * @return {@code true} if advanced protection is enabled, {@code false} otherwise.
     */
    @RequiresPermission(Manifest.permission.QUERY_ADVANCED_PROTECTION_MODE)
    public boolean isAdvancedProtectionEnabled() {
        try {
            return mService.isAdvancedProtectionEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Registers a {@link Callback} to be notified of changes to the Advanced Protection state.
     *
     * <p>The provided callback will be called on the specified executor with the updated
     * state. Methods are called when the state changes, as well as once
     * on initial registration.
     *
     * @param executor The executor of where the callback will execute.
     * @param callback The {@link Callback} object to register..
     */
    @RequiresPermission(Manifest.permission.QUERY_ADVANCED_PROTECTION_MODE)
    public void registerAdvancedProtectionCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull Callback callback) {
        if (mCallbackMap.get(callback) != null) {
            Log.d(TAG, "registerAdvancedProtectionCallback callback already present");
            return;
        }

        IAdvancedProtectionCallback delegate = new IAdvancedProtectionCallback.Stub() {
            @Override
            public void onAdvancedProtectionChanged(boolean enabled) {
                final long identity = Binder.clearCallingIdentity();
                try {
                    executor.execute(() -> callback.onAdvancedProtectionChanged(enabled));
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        };

        try {
            mService.registerAdvancedProtectionCallback(delegate);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }

        mCallbackMap.put(callback, delegate);
    }

    /**
     * Unregister an existing {@link Callback}.
     *
     * @param callback The {@link Callback} object to unregister.
     */
    @RequiresPermission(Manifest.permission.QUERY_ADVANCED_PROTECTION_MODE)
    public void unregisterAdvancedProtectionCallback(@NonNull Callback callback) {
        IAdvancedProtectionCallback delegate = mCallbackMap.get(callback);
        if (delegate == null) {
            Log.d(TAG, "unregisterAdvancedProtectionCallback callback not present");
            return;
        }

        try {
            mService.unregisterAdvancedProtectionCallback(delegate);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }

        mCallbackMap.remove(callback);
    }

    /**
     * Enables or disables advanced protection on the device. Can only be called by an admin user.
     *
     * @param enabled {@code true} to enable advanced protection, {@code false} to disable it.
     * @hide
     */
    @SystemApi
    @RequiresPermission(Manifest.permission.MANAGE_ADVANCED_PROTECTION_MODE)
    public void setAdvancedProtectionEnabled(boolean enabled) {
        try {
            mService.setAdvancedProtectionEnabled(enabled);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns the list of advanced protection features which are available on this device.
     *
     * @hide
     */
    @SystemApi
    @NonNull
    @RequiresPermission(Manifest.permission.MANAGE_ADVANCED_PROTECTION_MODE)
    public List<AdvancedProtectionFeature> getAdvancedProtectionFeatures() {
        try {
            return mService.getAdvancedProtectionFeatures();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Called by a feature to display a support dialog when a feature was disabled by advanced
     * protection. This returns an intent that can be used with
     * {@link Context#startActivity(Intent)} to display the dialog.
     *
     * <p>Note that this method doesn't check if the feature is actually disabled, i.e. this method
     * will always return an intent.
     *
     * @param featureId The feature identifier.
     * @param type The type of the feature describing the action that needs to be explained
     *                 in the dialog or {@link #SUPPORT_DIALOG_TYPE_UNKNOWN} for default
     *                 explanation.
     * @return Intent An intent to be used to start the dialog-activity that explains a feature was
     *                disabled by advanced protection.
     * @hide
     */
    public static @NonNull Intent createSupportIntent(@FeatureId int featureId,
            @SupportDialogType int type) {
        if (!ALL_FEATURE_IDS.contains(featureId)) {
            throw new IllegalArgumentException(featureId + " is not a valid feature ID. See"
                    + " FEATURE_ID_* APIs.");
        }
        if (!ALL_SUPPORT_DIALOG_TYPES.contains(type)) {
            throw new IllegalArgumentException(type + " is not a valid type. See"
                    + " SUPPORT_DIALOG_TYPE_* APIs.");
        }

        Intent intent = new Intent(ACTION_SHOW_ADVANCED_PROTECTION_SUPPORT_DIALOG);
        intent.setPackage(PKG_SETTINGS);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_SUPPORT_DIALOG_FEATURE, featureId);
        intent.putExtra(EXTRA_SUPPORT_DIALOG_TYPE, type);
        return intent;
    }

    /**
     * Called by a feature to display a support dialog when a feature was disabled by advanced
     * protection based on a policy identifier or restriction. This returns an intent that can be
     * used with {@link Context#startActivity(Intent)} to display the dialog.
     *
     * <p>At the moment, if the dialog is for {@link #FEATURE_ID_DISALLOW_CELLULAR_2G} or
     * {@link #FEATURE_ID_ENABLE_MTE} and the provided type is
     * {@link #SUPPORT_DIALOG_TYPE_UNKNOWN}, the type will be changed to
     * {@link #SUPPORT_DIALOG_TYPE_DISABLED_SETTING} in the returned intent, as these features only
     * have a disabled setting UI.
     *
     * <p>Note that this method doesn't check if the feature is actually disabled, i.e. this method
     * will always return an intent.
     *
     * @param identifier The policy identifier or restriction.
     * @param type The type of the feature describing the action that needs to be explained
     *                 in the dialog or {@link #SUPPORT_DIALOG_TYPE_UNKNOWN} for default
     *                 explanation.
     * @return Intent An intent to be used to start the dialog-activity that explains a feature was
     *                disabled by advanced protection.
     * @hide */
    public static @NonNull Intent createSupportIntentForPolicyIdentifierOrRestriction(
            @NonNull String identifier, @SupportDialogType int type) {
        Objects.requireNonNull(identifier);
        if (!ALL_SUPPORT_DIALOG_TYPES.contains(type)) {
            throw new IllegalArgumentException(type + " is not a valid type. See"
                    + " SUPPORT_DIALOG_TYPE_* APIs.");
        }
        final int featureId;
        int dialogType = type;
        if (DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY.equals(identifier)) {
            featureId = FEATURE_ID_DISALLOW_INSTALL_UNKNOWN_SOURCES;
        } else if (DISALLOW_CELLULAR_2G.equals(identifier)) {
            featureId = FEATURE_ID_DISALLOW_CELLULAR_2G;
            dialogType = (dialogType == SUPPORT_DIALOG_TYPE_UNKNOWN)
                    ? SUPPORT_DIALOG_TYPE_DISABLED_SETTING : dialogType;
        } else if (MEMORY_TAGGING_POLICY.equals(identifier)) {
            featureId = FEATURE_ID_ENABLE_MTE;
            dialogType = (dialogType == SUPPORT_DIALOG_TYPE_UNKNOWN)
                    ? SUPPORT_DIALOG_TYPE_DISABLED_SETTING : dialogType;
        } else {
            throw new UnsupportedOperationException("Unsupported identifier: " + identifier);
        }
        return createSupportIntent(featureId, dialogType);
    }

    /** @hide */
    @RequiresPermission(Manifest.permission.MANAGE_ADVANCED_PROTECTION_MODE)
    public void logDialogShown(@FeatureId int featureId, @SupportDialogType int type,
            boolean learnMoreClicked) {
        try {
            mService.logDialogShown(featureId, type, learnMoreClicked);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * A callback class for monitoring changes to Advanced Protection state
     *
     * <p>To register a callback, implement this interface, and register it with
     * {@link AdvancedProtectionManager#registerAdvancedProtectionCallback(Executor, Callback)}.
     * Methods are called when the state changes, as well as once on initial registration.
     */
    @FlaggedApi(Flags.FLAG_AAPM_API)
    public interface Callback {
        /**
         * Called when advanced protection state changes
         * @param enabled the new state
         */
        void onAdvancedProtectionChanged(boolean enabled);
    }
}
