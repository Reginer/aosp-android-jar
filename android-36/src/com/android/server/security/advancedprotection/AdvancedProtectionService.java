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

package com.android.server.security.advancedprotection;

import static android.provider.Settings.Secure.ADVANCED_PROTECTION_MODE;
import static android.provider.Settings.Secure.AAPM_USB_DATA_PROTECTION;
import static com.android.internal.util.ConcurrentUtils.DIRECT_EXECUTOR;

import android.Manifest;
import android.annotation.EnforcePermission;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.StatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PermissionEnforcer;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.provider.Settings;
import android.security.advancedprotection.AdvancedProtectionFeature;
import android.security.advancedprotection.AdvancedProtectionManager;
import android.security.advancedprotection.AdvancedProtectionManager.FeatureId;
import android.security.advancedprotection.AdvancedProtectionManager.SupportDialogType;
import android.security.advancedprotection.IAdvancedProtectionCallback;
import android.security.advancedprotection.IAdvancedProtectionService;
import android.security.advancedprotection.AdvancedProtectionProtoEnums;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.StatsEvent;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.UserManagerInternal;
import com.android.server.security.advancedprotection.features.AdvancedProtectionHook;
import com.android.server.security.advancedprotection.features.AdvancedProtectionProvider;
import com.android.server.security.advancedprotection.features.DisallowCellular2GAdvancedProtectionHook;
import com.android.server.security.advancedprotection.features.DisallowInstallUnknownSourcesAdvancedProtectionHook;
import com.android.server.security.advancedprotection.features.MemoryTaggingExtensionHook;
import com.android.server.security.advancedprotection.features.UsbDataAdvancedProtectionHook;
import com.android.server.security.advancedprotection.features.DisallowWepAdvancedProtectionProvider;

import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** @hide */
public class AdvancedProtectionService extends IAdvancedProtectionService.Stub {
    private static final String TAG = "AdvancedProtectionService";
    private static final int MODE_CHANGED = 0;
    private static final int CALLBACK_ADDED = 1;

    // Shared preferences keys
    private static final String PREFERENCE = "advanced_protection_preference";
    private static final String ENABLED_CHANGE_TIME = "enabled_change_time";
    private static final String LAST_DIALOG_FEATURE_ID = "last_dialog_feature_id";
    private static final String LAST_DIALOG_TYPE = "last_dialog_type";
    private static final String LAST_DIALOG_HOURS_SINCE_ENABLED = "last_dialog_hours_since_enabled";
    private static final String LAST_DIALOG_LEARN_MORE_CLICKED = "last_dialog_learn_more_clicked";
    private static final long MILLIS_PER_HOUR = 60 * 60 * 1000;

    private final Context mContext;
    private final Handler mHandler;
    private final AdvancedProtectionStore mStore;
    private final UserManagerInternal mUserManager;

    // Features living with the service - their code will be executed when state changes
    private final ArrayList<AdvancedProtectionHook> mHooks = new ArrayList<>();
    // External features - they will be called on state change
    private final ArrayMap<IBinder, IAdvancedProtectionCallback> mCallbacks = new ArrayMap<>();
    // For tracking only - not called on state change
    private final ArrayList<AdvancedProtectionProvider> mProviders = new ArrayList<>();

    // Used to store logging data
    private SharedPreferences mSharedPreferences;
    private boolean mEmitLogs = true;

    private AdvancedProtectionService(@NonNull Context context) {
        super(PermissionEnforcer.fromContext(context));
        mContext = context;
        mHandler = new AdvancedProtectionHandler(FgThread.get().getLooper());
        mStore = new AdvancedProtectionStore(mContext);
        mUserManager = LocalServices.getService(UserManagerInternal.class);
    }

    private void initFeatures(boolean enabled) {
        if (android.security.Flags.aapmFeatureDisableInstallUnknownSources()) {
          try {
            mHooks.add(new DisallowInstallUnknownSourcesAdvancedProtectionHook(mContext, enabled));
          } catch (Exception e) {
            Slog.e(TAG, "Failed to initialize DisallowInstallUnknownSources", e);
          }
        }
        if (android.security.Flags.aapmFeatureMemoryTaggingExtension()) {
          try {
            mHooks.add(new MemoryTaggingExtensionHook(mContext, enabled));
          } catch (Exception e) {
            Slog.e(TAG, "Failed to initialize MemoryTaggingExtension", e);
          }
        }
        if (android.security.Flags.aapmFeatureDisableCellular2g()) {
          try {
            mHooks.add(new DisallowCellular2GAdvancedProtectionHook(mContext, enabled));
          } catch (Exception e) {
            Slog.e(TAG, "Failed to initialize DisallowCellular2g", e);
          }
        }
        if (android.security.Flags.aapmFeatureUsbDataProtection()
                // Usb data protection is enabled by default
                && mStore.retrieveInt(AAPM_USB_DATA_PROTECTION, AdvancedProtectionStore.ON)
                == AdvancedProtectionStore.ON) {
          try {
            mHooks.add(new UsbDataAdvancedProtectionHook(mContext, enabled));
          } catch (Exception e) {
            Slog.e(TAG, "Failed to initialize UsbDataAdvancedProtection", e);
          }
        }

        mProviders.add(new DisallowWepAdvancedProtectionProvider());
    }

    private void initLogging() {
        StatsManager statsManager = mContext.getSystemService(StatsManager.class);
        statsManager.setPullAtomCallback(
                FrameworkStatsLog.ADVANCED_PROTECTION_STATE_INFO,
                null, // use default PullAtomMetadata values
                DIRECT_EXECUTOR,
                new AdvancedProtectionStatePullAtomCallback());
    }

    // Only for tests
    @VisibleForTesting
    AdvancedProtectionService(
            @NonNull Context context,
            @NonNull AdvancedProtectionStore store,
            @NonNull UserManagerInternal userManager,
            @NonNull Looper looper,
            @NonNull PermissionEnforcer permissionEnforcer,
            @Nullable AdvancedProtectionHook hook,
            @Nullable AdvancedProtectionProvider provider) {
        super(permissionEnforcer);
        mContext = context;
        mStore = store;
        mUserManager = userManager;
        mHandler = new AdvancedProtectionHandler(looper);
        if (hook != null) {
            mHooks.add(hook);
        }

        if (provider != null) {
            mProviders.add(provider);
        }

        mEmitLogs = false;
    }

    @Override
    @EnforcePermission(Manifest.permission.QUERY_ADVANCED_PROTECTION_MODE)
    public boolean isAdvancedProtectionEnabled() {
        isAdvancedProtectionEnabled_enforcePermission();
        final long identity = Binder.clearCallingIdentity();
        try {
            return isAdvancedProtectionEnabledInternal();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    // Without permission check
    private boolean isAdvancedProtectionEnabledInternal() {
        return mStore.retrieveAdvancedProtectionModeEnabled();
    }

    @Override
    @EnforcePermission(Manifest.permission.QUERY_ADVANCED_PROTECTION_MODE)
    public void registerAdvancedProtectionCallback(@NonNull IAdvancedProtectionCallback callback)
            throws RemoteException {
        registerAdvancedProtectionCallback_enforcePermission();
        IBinder b = callback.asBinder();
        b.linkToDeath(new DeathRecipient(b), 0);
        synchronized (mCallbacks) {
            mCallbacks.put(b, callback);
            sendCallbackAdded(isAdvancedProtectionEnabledInternal(), callback);
        }
    }

    @Override
    @EnforcePermission(Manifest.permission.QUERY_ADVANCED_PROTECTION_MODE)
    public void unregisterAdvancedProtectionCallback(
            @NonNull IAdvancedProtectionCallback callback) {
        unregisterAdvancedProtectionCallback_enforcePermission();
        synchronized (mCallbacks) {
            mCallbacks.remove(callback.asBinder());
        }
    }

    @Override
    @EnforcePermission(Manifest.permission.MANAGE_ADVANCED_PROTECTION_MODE)
    public void setAdvancedProtectionEnabled(boolean enabled) {
        setAdvancedProtectionEnabled_enforcePermission();
        final UserHandle user = Binder.getCallingUserHandle();
        final long identity = Binder.clearCallingIdentity();
        try {
            enforceAdminUser(user);
            synchronized (mCallbacks) {
                if (enabled != isAdvancedProtectionEnabledInternal()) {
                    mStore.storeAdvancedProtectionModeEnabled(enabled);
                    sendModeChanged(enabled);
                    logAdvancedProtectionEnabled(enabled);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void setUsbDataProtectionEnabled(boolean enabled) {
        int value = enabled ? AdvancedProtectionStore.ON
                : AdvancedProtectionStore.OFF;
        setAdvancedProtectionSubSettingInt(AAPM_USB_DATA_PROTECTION, value);
    }

    private void setAdvancedProtectionSubSettingInt(String key, int value) {
        final long identity = Binder.clearCallingIdentity();
        try {
            synchronized (mCallbacks) {
                mStore.storeInt(key, value);
                Slog.i(TAG, "Advanced protection: subsetting" + key + " is " + value);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean isUsbDataProtectionEnabled() {
        final long identity = Binder.clearCallingIdentity();
        try {
            return mStore.retrieveInt(AAPM_USB_DATA_PROTECTION, AdvancedProtectionStore.ON)
                == AdvancedProtectionStore.ON;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    @Override
    @EnforcePermission(Manifest.permission.MANAGE_ADVANCED_PROTECTION_MODE)
    public void logDialogShown(@FeatureId int featureId, @SupportDialogType int type,
            boolean learnMoreClicked) {
        logDialogShown_enforcePermission();

        if (!mEmitLogs) {
            return;
        }

        int hoursSinceEnabled = hoursSinceLastChange();
        FrameworkStatsLog.write(FrameworkStatsLog.ADVANCED_PROTECTION_SUPPORT_DIALOG_DISPLAYED,
                /*feature_id*/ featureIdToLogEnum(featureId),
                /*dialogue_type*/ dialogueTypeToLogEnum(type),
                /*learn_more_clicked*/ learnMoreClicked,
                /*hours_since_last_change*/ hoursSinceEnabled);

        getSharedPreferences().edit()
                .putInt(LAST_DIALOG_FEATURE_ID, featureId)
                .putInt(LAST_DIALOG_TYPE, type)
                .putBoolean(LAST_DIALOG_LEARN_MORE_CLICKED, learnMoreClicked)
                .putInt(LAST_DIALOG_HOURS_SINCE_ENABLED, hoursSinceEnabled)
                .apply();
    }

    private int featureIdToLogEnum(@FeatureId int featureId) {
        switch (featureId) {
            case AdvancedProtectionManager.FEATURE_ID_DISALLOW_CELLULAR_2G:
                return AdvancedProtectionProtoEnums.FEATURE_ID_DISALLOW_CELLULAR_2G;
            case AdvancedProtectionManager.FEATURE_ID_DISALLOW_INSTALL_UNKNOWN_SOURCES:
                return AdvancedProtectionProtoEnums.FEATURE_ID_DISALLOW_INSTALL_UNKNOWN_SOURCES;
            case AdvancedProtectionManager.FEATURE_ID_DISALLOW_USB:
                return AdvancedProtectionProtoEnums.FEATURE_ID_DISALLOW_USB;
            case AdvancedProtectionManager.FEATURE_ID_DISALLOW_WEP:
                return AdvancedProtectionProtoEnums.FEATURE_ID_DISALLOW_WEP;
            case AdvancedProtectionManager.FEATURE_ID_ENABLE_MTE:
                return AdvancedProtectionProtoEnums.FEATURE_ID_ENABLE_MTE;
            default:
                return AdvancedProtectionProtoEnums.FEATURE_ID_UNKNOWN;
        }
    }

    private int dialogueTypeToLogEnum(@SupportDialogType int type) {
        switch (type) {
            case AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_UNKNOWN:
                return AdvancedProtectionProtoEnums.DIALOGUE_TYPE_UNKNOWN;
            case AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION:
                return AdvancedProtectionProtoEnums.DIALOGUE_TYPE_BLOCKED_INTERACTION;
            case AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_DISABLED_SETTING:
                return AdvancedProtectionProtoEnums.DIALOGUE_TYPE_DISABLED_SETTING;
            default:
                return AdvancedProtectionProtoEnums.DIALOGUE_TYPE_UNKNOWN;
        }
    }

    private void logAdvancedProtectionEnabled(boolean enabled) {
        if (!mEmitLogs) {
            return;
        }

        Slog.i(TAG, "Advanced protection has been " + (enabled ? "enabled" : "disabled"));
        SharedPreferences prefs = getSharedPreferences();
        FrameworkStatsLog.write(FrameworkStatsLog.ADVANCED_PROTECTION_STATE_CHANGED,
                /*enabled*/ enabled,
                /*hours_since_enabled*/ hoursSinceLastChange(),
                /*last_dialog_feature_id*/ featureIdToLogEnum(
                    prefs.getInt(LAST_DIALOG_FEATURE_ID, -1)),
                /*_type*/ dialogueTypeToLogEnum(prefs.getInt(LAST_DIALOG_TYPE, -1)),
                /*_learn_more_clicked*/ prefs.getBoolean(LAST_DIALOG_LEARN_MORE_CLICKED, false),
                /*_hours_since_enabled*/ prefs.getInt(LAST_DIALOG_HOURS_SINCE_ENABLED, -1));
        prefs.edit()
                .putLong(ENABLED_CHANGE_TIME, System.currentTimeMillis())
                .apply();
    }

    private int hoursSinceLastChange() {
        int hoursSinceEnabled = -1;
        long lastChangeTimeMillis = getSharedPreferences().getLong(ENABLED_CHANGE_TIME, -1);
        if (lastChangeTimeMillis != -1) {
            hoursSinceEnabled = (int)
                    ((System.currentTimeMillis() - lastChangeTimeMillis) / MILLIS_PER_HOUR);
        }
        return hoursSinceEnabled;
    }

    @Override
    @EnforcePermission(Manifest.permission.MANAGE_ADVANCED_PROTECTION_MODE)
    public List<AdvancedProtectionFeature> getAdvancedProtectionFeatures() {
        getAdvancedProtectionFeatures_enforcePermission();
        List<AdvancedProtectionFeature> features = new ArrayList<>();
        for (int i = 0; i < mProviders.size(); i++) {
            features.addAll(mProviders.get(i).getFeatures(mContext));
        }

        for (int i = 0; i < mHooks.size(); i++) {
            AdvancedProtectionHook hook = mHooks.get(i);
            if (hook.isAvailable()) {
                features.add(hook.getFeature());
            }
        }

        return features;
    }

    private void enforceAdminUser(UserHandle user) {
        UserInfo info = mUserManager.getUserInfo(user.getIdentifier());
        if (!info.isAdmin()) {
            throw new SecurityException("Only an admin user can manage advanced protection mode");
        }
    }

    @Override
    public void onShellCommand(FileDescriptor in, FileDescriptor out,
            FileDescriptor err, @NonNull String[] args, ShellCallback callback,
            @NonNull ResultReceiver resultReceiver) {
        (new AdvancedProtectionShellCommand(this))
                .exec(this, in, out, err, args, callback, resultReceiver);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (!DumpUtils.checkDumpPermission(mContext, TAG, writer)) return;
        writer.println("AdvancedProtectionService");
        writer.println("  isAdvancedProtectionEnabled: " + isAdvancedProtectionEnabledInternal());
        writer.println("  mHooks.size(): " + mHooks.size());
        writer.println("  mCallbacks.size(): " + mCallbacks.size());
        writer.println("  mProviders.size(): " + mProviders.size());

        writer.println("Hooks: ");
        mHooks.stream().forEach(hook -> {
            writer.println("    " + hook.getClass().getSimpleName() +
                                   " available: " + hook.isAvailable());
        });
        writer.println("  Providers: ");
        mProviders.stream().forEach(provider -> {
            writer.println("    " + provider.getClass().getSimpleName());
            provider.getFeatures(mContext).stream().forEach(feature -> {
                writer.println("      " + feature.getClass().getSimpleName());
            });
        });
        writer.println("  mSharedPreferences: " + getSharedPreferences().getAll());
    }

    void sendModeChanged(boolean enabled) {
        Message.obtain(mHandler, MODE_CHANGED, /*enabled*/ enabled ? 1 : 0, /*unused */ -1)
                .sendToTarget();
    }

    void sendCallbackAdded(boolean enabled, IAdvancedProtectionCallback callback) {
        Message.obtain(mHandler, CALLBACK_ADDED, /*enabled*/ enabled ? 1 : 0, /*unused*/ -1,
                        /*callback*/ callback)
                .sendToTarget();
    }

    private SharedPreferences getSharedPreferences() {
        if (mSharedPreferences == null) {
            initSharedPreferences();
        }
        return mSharedPreferences;
    }

    private synchronized void initSharedPreferences() {
        if (mSharedPreferences == null) {
            Context deviceContext = mContext.createDeviceProtectedStorageContext();
            File sharedPrefs = new File(Environment.getDataSystemDirectory(), PREFERENCE);
            mSharedPreferences = deviceContext.getSharedPreferences(sharedPrefs,
                    Context.MODE_PRIVATE);
        }
    }

    public static final class Lifecycle extends SystemService {
        private final AdvancedProtectionService mService;

        public Lifecycle(@NonNull Context context) {
            super(context);
            mService = new AdvancedProtectionService(context);
        }

        @Override
        public void onStart() {
            publishBinderService(Context.ADVANCED_PROTECTION_SERVICE, mService);
        }

        @Override
        public void onBootPhase(@BootPhase int phase) {
            if (phase == PHASE_SYSTEM_SERVICES_READY) {
                boolean enabled = mService.isAdvancedProtectionEnabledInternal();
                if (enabled) {
                    Slog.i(TAG, "Advanced protection is enabled");
                }
                mService.initFeatures(enabled);
                mService.initLogging();
            }
        }
    }

    @VisibleForTesting
    static class AdvancedProtectionStore {
        static final int ON = 1;
        static final int OFF = 0;
        private final Context mContext;

        AdvancedProtectionStore(@NonNull Context context) {
            mContext = context;
        }

        void storeAdvancedProtectionModeEnabled(boolean enabled) {
            Settings.Secure.putIntForUser(mContext.getContentResolver(),
                    ADVANCED_PROTECTION_MODE, enabled ? ON : OFF,
                    UserHandle.USER_SYSTEM);
        }

        boolean retrieveAdvancedProtectionModeEnabled() {
            return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                    ADVANCED_PROTECTION_MODE, OFF, UserHandle.USER_SYSTEM) == ON;
        }

        void storeInt(String key, int value) {
            Settings.Secure.putIntForUser(mContext.getContentResolver(),
                    key, value,
                    UserHandle.USER_SYSTEM);
        }

        int retrieveInt(String key, int defaultValue) {
            return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                    key, defaultValue, UserHandle.USER_SYSTEM);
        }
    }

    private class AdvancedProtectionHandler extends Handler {
        private AdvancedProtectionHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                // arg1 == enabled
                case MODE_CHANGED:
                    handleAllCallbacks(msg.arg1 == 1);
                    break;
                // arg1 == enabled
                // obj == callback
                case CALLBACK_ADDED:
                    handleSingleCallback(msg.arg1 == 1, (IAdvancedProtectionCallback) msg.obj);
                    break;
            }
        }

        private void handleAllCallbacks(boolean enabled) {
            ArrayList<IAdvancedProtectionCallback> deadObjects = new ArrayList<>();

            for (int i = 0; i < mHooks.size(); i++) {
                AdvancedProtectionHook feature = mHooks.get(i);
                try {
                    if (feature.isAvailable()) {
                        feature.onAdvancedProtectionChanged(enabled);
                    }
                } catch (Exception e) {
                    Slog.e(TAG, "Failed to call hook for feature "
                            + feature.getFeature().getId(), e);
                }
            }
            synchronized (mCallbacks) {
                for (int i = 0; i < mCallbacks.size(); i++) {
                    IAdvancedProtectionCallback callback = mCallbacks.valueAt(i);
                    try {
                        callback.onAdvancedProtectionChanged(enabled);
                    } catch (RemoteException e) {
                        deadObjects.add(callback);
                    }
                }

                for (int i = 0; i < deadObjects.size(); i++) {
                    mCallbacks.remove(deadObjects.get(i).asBinder());
                }
            }
        }

        private void handleSingleCallback(boolean enabled, IAdvancedProtectionCallback callback) {
            try {
                callback.onAdvancedProtectionChanged(enabled);
            } catch (RemoteException e) {
                mCallbacks.remove(callback.asBinder());
            }
        }
    }

    private final class DeathRecipient implements IBinder.DeathRecipient {
        private final IBinder mBinder;

        DeathRecipient(IBinder binder) {
            mBinder = binder;
        }

        @Override
        public void binderDied() {
            synchronized (mCallbacks) {
                mCallbacks.remove(mBinder);
            }
        }
    }

    private class AdvancedProtectionStatePullAtomCallback
            implements StatsManager.StatsPullAtomCallback {

        @Override
        public int onPullAtom(int atomTag, List<StatsEvent> data) {
            if (atomTag != FrameworkStatsLog.ADVANCED_PROTECTION_STATE_INFO) {
                return StatsManager.PULL_SKIP;
            }

            data.add(
                    FrameworkStatsLog.buildStatsEvent(
                            FrameworkStatsLog.ADVANCED_PROTECTION_STATE_INFO,
                            /*enabled*/ isAdvancedProtectionEnabledInternal(),
                            /*hours_since_enabled*/ hoursSinceLastChange()));
            return StatsManager.PULL_SUCCESS;
        }
    }
}
