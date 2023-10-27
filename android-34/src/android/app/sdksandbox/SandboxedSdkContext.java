/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.app.sdksandbox;

import static android.app.sdksandbox.SdkSandboxSystemServiceRegistry.ServiceMutator;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;

import com.android.internal.annotations.VisibleForTesting;

import java.io.File;

/**
 * Refers to the context of the SDK loaded in the SDK sandbox process.
 *
 * <p>It is a wrapper of the client application (which loading SDK to the sandbox) context, to
 * represent the context of the SDK loaded by that application.
 *
 * <p>An instance of the {@link SandboxedSdkContext} will be created by the SDK sandbox, and then
 * attached to the {@link SandboxedSdkProvider} after the SDK is loaded.
 *
 * <p>Each sdk will get their own private storage directories and the file storage API on this
 * object will utilize those areas.
 *
 * @hide
 */
public final class SandboxedSdkContext extends ContextWrapper {

    private final Resources mResources;
    private final AssetManager mAssets;
    private final String mClientPackageName;
    private final String mSdkName;
    private final ApplicationInfo mSdkProviderInfo;
    @Nullable private final File mCeDataDir;
    @Nullable private final File mDeDataDir;
    private final SdkSandboxSystemServiceRegistry mSdkSandboxSystemServiceRegistry;
    private final ClassLoader mClassLoader;
    private final boolean mCustomizedSdkContextEnabled;

    public SandboxedSdkContext(
            @NonNull Context baseContext,
            @NonNull ClassLoader classLoader,
            @NonNull String clientPackageName,
            @NonNull ApplicationInfo info,
            @NonNull String sdkName,
            @Nullable String sdkCeDataDir,
            @Nullable String sdkDeDataDir,
            boolean isCustomizedSdkContextEnabled) {
        this(
                baseContext,
                classLoader,
                clientPackageName,
                info,
                sdkName,
                sdkCeDataDir,
                sdkDeDataDir,
                isCustomizedSdkContextEnabled,
                SdkSandboxSystemServiceRegistry.getInstance());
    }

    @VisibleForTesting
    public SandboxedSdkContext(
            @NonNull Context baseContext,
            @NonNull ClassLoader classLoader,
            @NonNull String clientPackageName,
            @NonNull ApplicationInfo info,
            @NonNull String sdkName,
            @Nullable String sdkCeDataDir,
            @Nullable String sdkDeDataDir,
            boolean isCustomizedSdkContextEnabled,
            SdkSandboxSystemServiceRegistry sdkSandboxSystemServiceRegistry) {
        super(baseContext);
        mClientPackageName = clientPackageName;
        mSdkName = sdkName;
        mSdkProviderInfo = info;
        Resources resources = null;
        try {
            resources = baseContext.getPackageManager().getResourcesForApplication(info);
        } catch (Exception ignored) {
        }

        if (resources != null) {
            mResources = resources;
            mAssets = resources.getAssets();
        } else {
            mResources = null;
            mAssets = null;
        }

        mCeDataDir = (sdkCeDataDir != null) ? new File(sdkCeDataDir) : null;
        mDeDataDir = (sdkDeDataDir != null) ? new File(sdkDeDataDir) : null;

        mSdkSandboxSystemServiceRegistry = sdkSandboxSystemServiceRegistry;
        mClassLoader = classLoader;
        mCustomizedSdkContextEnabled = isCustomizedSdkContextEnabled;
    }

    /**
     * Return a new Context object for the current SandboxedSdkContext but whose storage APIs are
     * backed by sdk specific credential-protected storage.
     *
     * @see Context#isCredentialProtectedStorage()
     */
    @Override
    @NonNull
    public Context createCredentialProtectedStorageContext() {
        Context newBaseContext = getBaseContext().createCredentialProtectedStorageContext();
        return new SandboxedSdkContext(
                newBaseContext,
                mClassLoader,
                mClientPackageName,
                mSdkProviderInfo,
                mSdkName,
                (mCeDataDir != null) ? mCeDataDir.toString() : null,
                (mDeDataDir != null) ? mDeDataDir.toString() : null,
                mCustomizedSdkContextEnabled);
    }

    /**
     * Return a new Context object for the current SandboxedSdkContext but whose storage
     * APIs are backed by sdk specific device-protected storage.
     *
     * @see Context#isDeviceProtectedStorage()
     */
    @Override
    @NonNull
    public Context createDeviceProtectedStorageContext() {
        Context newBaseContext = getBaseContext().createDeviceProtectedStorageContext();
        return new SandboxedSdkContext(
                newBaseContext,
                mClassLoader,
                mClientPackageName,
                mSdkProviderInfo,
                mSdkName,
                (mCeDataDir != null) ? mCeDataDir.toString() : null,
                (mDeDataDir != null) ? mDeDataDir.toString() : null,
                mCustomizedSdkContextEnabled);
    }

    /**
     * Returns the SDK name defined in the SDK's manifest.
     */
    @NonNull
    public String getSdkName() {
        return mSdkName;
    }

    /**
     * Returns the SDK package name defined in the SDK's manifest.
     *
     * @hide
     */
    @NonNull
    public String getSdkPackageName() {
        return mSdkProviderInfo.packageName;
    }

    /**
     * Returns the package name of the client application corresponding to the sandbox.
     *
     */
    @NonNull
    public String getClientPackageName() {
        return mClientPackageName;
    }

    /** Returns the resources defined in the SDK's .apk file. */
    @Override
    @Nullable
    public Resources getResources() {
        if (mCustomizedSdkContextEnabled) {
            return getBaseContext().getResources();
        }
        return mResources;
    }

    /** Returns the assets defined in the SDK's .apk file. */
    @Override
    @Nullable
    public AssetManager getAssets() {
        if (mCustomizedSdkContextEnabled) {
            return getBaseContext().getAssets();
        }
        return mAssets;
    }

    /** Returns sdk-specific internal storage directory. */
    @Override
    @Nullable
    public File getDataDir() {
        if (mCustomizedSdkContextEnabled) {
            return getBaseContext().getDataDir();
        }

        File res = null;
        if (isCredentialProtectedStorage()) {
            res = mCeDataDir;
        } else if (isDeviceProtectedStorage()) {
            res = mDeDataDir;
        }
        if (res == null) {
            throw new RuntimeException("No data directory found for sdk: " + getSdkName());
        }
        return res;
    }

    @Override
    @Nullable
    public Object getSystemService(String name) {
        if (name == null) {
            return null;
        }
        Object service = getBaseContext().getSystemService(name);
        ServiceMutator serviceMutator = mSdkSandboxSystemServiceRegistry.getServiceMutator(name);
        if (serviceMutator != null) {
            service = serviceMutator.setContext(service, this);
        }
        return service;
    }

    @Override
    public ClassLoader getClassLoader() {
        if (mCustomizedSdkContextEnabled) {
            return getBaseContext().getClassLoader();
        }
        return mClassLoader;
    }
}
