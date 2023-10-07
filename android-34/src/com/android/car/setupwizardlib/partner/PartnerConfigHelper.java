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

package com.android.car.setupwizardlib.partner;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import java.util.EnumMap;

/** The helper reads and caches the partner configurations from Car Setup Wizard. */
public class PartnerConfigHelper {

    private static final String TAG = PartnerConfigHelper.class.getSimpleName();

    @VisibleForTesting
    static final String SUW_AUTHORITY = "com.google.android.car.setupwizard.partner";

    @VisibleForTesting
    static final String SUW_GET_PARTNER_CONFIG_METHOD = "getOverlayConfig";
    private static volatile PartnerConfigHelper sInstance = null;

    @VisibleForTesting Bundle mResultBundle = null;

    @VisibleForTesting
    final EnumMap<PartnerConfig, Object> mPartnerResourceCache = new EnumMap<>(PartnerConfig.class);

    /** Factory method to get an instance */
    public static PartnerConfigHelper get(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (PartnerConfigHelper.class) {
                if (sInstance == null) {
                    sInstance = new PartnerConfigHelper(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * Returns the color of given {@code partnerConfig}, or 0 if the given {@code partnerConfig}
     * is not found. If the {@code ResourceType} of the given {@code partnerConfig} is not color,
     * IllegalArgumentException will be thrown.
     *
     * @param context The context of client activity
     * @param partnerConfig The {@code PartnerConfig} of target resource
     */
    @ColorInt
    public int getColor(@NonNull Context context, PartnerConfig partnerConfig) {
        if (partnerConfig.getResourceType() != PartnerConfig.ResourceType.COLOR) {
            throw new IllegalArgumentException("Not a color resource");
        }

        if (mPartnerResourceCache.containsKey(partnerConfig)) {
            Object cacheValue = mPartnerResourceCache.get(partnerConfig);
            if (cacheValue instanceof Integer) {
                return (int) cacheValue;
            }
        }

        int result = 0;
        try {
            String resourceName = partnerConfig.getResourceName();
            ResourceEntry resourceEntry = getResourceEntryFromKey(resourceName);
            if (resourceEntry == null) {
                Log.w(TAG, "Resource not found: " + resourceName);
                return 0;
            }

            Resources resource = getResourcesByPackageName(context, resourceEntry.getPackageName());
            result = resource.getColor(resourceEntry.getResourceId(), null);
            mPartnerResourceCache.put(partnerConfig, result);
        } catch (PackageManager.NameNotFoundException exception) {
            Log.e(TAG, exception.getMessage());
        }
        return result;
    }

    /**
     * Returns the {@link android.content.res.ColorStateList} of given {@link PartnerConfig}, or
     * {@code null} if the given {@code partnerConfig} is not found. If the {@code ResourceType} of
     * the given {@link PartnerConfig} is not a color, IllegalArgumentException will be thrown.
     *
     * @param context The context of client activity
     * @param partnerConfig The {@link PartnerConfig} of target resource
     */
    @Nullable
    public ColorStateList getColorStateList(@NonNull Context context, PartnerConfig partnerConfig) {
        if (partnerConfig.getResourceType() != PartnerConfig.ResourceType.COLOR) {
            throw new IllegalArgumentException("Not a color resource");
        }

        if (mPartnerResourceCache.containsKey(partnerConfig)) {
            Object cacheValue = mPartnerResourceCache.get(partnerConfig);
            if (cacheValue instanceof ColorStateList) {
                return (ColorStateList) cacheValue;
            }
        }

        ColorStateList result = null;
        try {
            String resourceName = partnerConfig.getResourceName();
            ResourceEntry resourceEntry = getResourceEntryFromKey(resourceName);
            if (resourceEntry == null) {
                Log.w(TAG, "Resource not found: " + resourceName);
                return null;
            }

            Resources resource = getResourcesByPackageName(context, resourceEntry.getPackageName());

            // In case the resource is {@code null} it's simply returned as it is.
            TypedValue outValue = new TypedValue();
            resource.getValue(resourceEntry.getResourceId(), outValue, true);
            if (outValue.type == TypedValue.TYPE_REFERENCE && outValue.data == 0) {
                return result;
            }

            result = resource.getColorStateList(resourceEntry.getResourceId(), null);
            mPartnerResourceCache.put(partnerConfig, result);
        } catch (PackageManager.NameNotFoundException exception) {
            Log.e(TAG, exception.getMessage());
        }
        return result;
    }

    /**
     * Returns the {@code Drawable} of given {@code partnerConfig}, or {@code null} if the given
     * {@code partnerConfig} is not found. If the {@code ResourceType} of the given {@code
     * resourceConfig} is not drawable, IllegalArgumentException will be thrown.
     *
     * @param context The context of client activity
     * @param partnerConfig The {@code PartnerConfig} of target resource
     */
    @Nullable
    public Drawable getDrawable(@NonNull Context context, PartnerConfig partnerConfig) {
        if (partnerConfig.getResourceType() != PartnerConfig.ResourceType.DRAWABLE) {
            throw new IllegalArgumentException("Not a drawable resource");
        }

        if (mPartnerResourceCache.containsKey(partnerConfig)) {
            return (Drawable) mPartnerResourceCache.get(partnerConfig);
        }

        Drawable result = null;
        try {
            String resourceName = partnerConfig.getResourceName();
            ResourceEntry resourceEntry = getResourceEntryFromKey(resourceName);
            if (resourceEntry == null) {
                Log.w(TAG, "Resource not found: " + resourceName);
                return null;
            }
            Resources resource = getResourcesByPackageName(context, resourceEntry.getPackageName());

            // for @null
            TypedValue outValue = new TypedValue();
            resource.getValue(resourceEntry.getResourceId(), outValue, true);
            if (outValue.type == TypedValue.TYPE_REFERENCE && outValue.data == 0) {
                return result;
            }

            result = resource.getDrawable(resourceEntry.getResourceId(), null);
            mPartnerResourceCache.put(partnerConfig, result);
        } catch (PackageManager.NameNotFoundException | NotFoundException exception) {
            Log.e(TAG, exception.getMessage());
        }
        return result;
    }

    /**
     * Returns the string of the given {@code partnerConfig}, or {@code null} if the given {@code
     * resourceConfig} is not found. If the {@code ResourceType} of the given {@code partnerConfig}
     * is not string, IllegalArgumentException will be thrown.
     *
     * @param context The context of client activity
     * @param partnerConfig The {@code PartnerConfig} of target resource
     */
    @Nullable
    public String getString(@NonNull Context context, PartnerConfig partnerConfig) {
        if (partnerConfig.getResourceType() != PartnerConfig.ResourceType.STRING) {
            throw new IllegalArgumentException("Not a string resource");
        }

        if (mPartnerResourceCache.containsKey(partnerConfig)) {
            return (String) mPartnerResourceCache.get(partnerConfig);
        }

        String result = null;
        try {
            String resourceName = partnerConfig.getResourceName();
            ResourceEntry resourceEntry = getResourceEntryFromKey(resourceName);
            if (resourceEntry == null) {
                Log.w(TAG, "Resource not found: " + resourceName);
                return null;
            }
            Resources resource = getResourcesByPackageName(context, resourceEntry.getPackageName());
            result = resource.getString(resourceEntry.getResourceId());
            mPartnerResourceCache.put(partnerConfig, result);
        } catch (PackageManager.NameNotFoundException exception) {
            Log.e(TAG, exception.getMessage());
        }
        return result;
    }

    /**
     * Returns the dimension of given {@code partnerConfig}. The default return value is 0.
     *
     * @param context The context of client activity
     * @param resourceConfig The {@code PartnerConfig} of target resource
     */
    public float getDimension(@NonNull Context context, PartnerConfig resourceConfig) {
        return getDimension(context, resourceConfig, 0);
    }

    /**
     * Returns the dimension of given {@code partnerConfig}. If the given {@code partnerConfig}
     * not found, will return {@code defaultValue}. If the {@code ResourceType} of given {@code
     * resourceConfig} is not dimension, will throw IllegalArgumentException.
     *
     * @param context The context of client activity
     * @param partnerConfig The {@code PartnerConfig} of target resource
     * @param defaultValue The default value
     */
    public float getDimension(
            @NonNull Context context, PartnerConfig partnerConfig, float defaultValue) {
        if (partnerConfig.getResourceType() != PartnerConfig.ResourceType.DIMENSION) {
            throw new IllegalArgumentException("Not a dimension resource");
        }

        if (mPartnerResourceCache.containsKey(partnerConfig)) {
            return (float) mPartnerResourceCache.get(partnerConfig);
        }

        float result = defaultValue;
        try {
            String resourceName = partnerConfig.getResourceName();
            ResourceEntry resourceEntry = getResourceEntryFromKey(resourceName);
            if (resourceEntry == null) {
                Log.w(TAG, "Resource not found: " + resourceName);
                return defaultValue;
            }
            Resources resource = getResourcesByPackageName(context, resourceEntry.getPackageName());
            result = resource.getDimension(resourceEntry.getResourceId());
            mPartnerResourceCache.put(partnerConfig, result);
        } catch (PackageManager.NameNotFoundException exception) {
            Log.e(TAG, exception.getMessage());
        }
        return result;
    }

    /**
     * Returns the boolean value of given {@code partnerConfig}. If the given {@code partnerConfig}
     * not found, will return {@code defaultValue}. If the {@code ResourceType} of given {@code
     * resourceConfig} is not boolean, will throw IllegalArgumentException.
     *
     * @param context The context of client activity
     * @param partnerConfig The {@code PartnerConfig} of target resource
     * @param defaultValue The default value
     */
    public boolean getBoolean(
            @NonNull Context context, PartnerConfig partnerConfig, boolean defaultValue) {
        if (partnerConfig.getResourceType() != PartnerConfig.ResourceType.BOOLEAN) {
            throw new IllegalArgumentException("Not a boolean resource");
        }

        if (mPartnerResourceCache.containsKey(partnerConfig)) {
            return (boolean) mPartnerResourceCache.get(partnerConfig);
        }

        boolean result = defaultValue;
        try {
            String resourceName = partnerConfig.getResourceName();
            ResourceEntry resourceEntry = getResourceEntryFromKey(resourceName);
            if (resourceEntry == null) {
                Log.w(TAG, "Resource not found: " + resourceName);
                return defaultValue;
            }
            Resources resource = getResourcesByPackageName(context, resourceEntry.getPackageName());
            result = resource.getBoolean(resourceEntry.getResourceId());
            mPartnerResourceCache.put(partnerConfig, result);
        } catch (PackageManager.NameNotFoundException exception) {
            Log.e(TAG, exception.getMessage());
        }
        return result;
    }

    private void getPartnerConfigBundle(Context context) {
        if (mResultBundle == null) {
            try {
                Uri contentUri =
                        new Uri.Builder()
                                .scheme(ContentResolver.SCHEME_CONTENT)
                                .authority(SUW_AUTHORITY)
                                .appendPath(SUW_GET_PARTNER_CONFIG_METHOD)
                                .build();
                mResultBundle = context.getContentResolver().call(
                        contentUri,
                        SUW_GET_PARTNER_CONFIG_METHOD,
                        /* arg= */ null,
                        /* extras= */ null);
                mPartnerResourceCache.clear();
            } catch (IllegalArgumentException exception) {
                Log.w(TAG, "Fail to get config from suw provider");
            }
        }
    }

    private Resources getResourcesByPackageName(Context context, String packageName)
            throws PackageManager.NameNotFoundException {
        PackageManager manager = context.getPackageManager();
        return manager.getResourcesForApplication(packageName);
    }

    private ResourceEntry getResourceEntryFromKey(String resourceName) {
        if (mResultBundle == null) {
            return null;
        }
        return ResourceEntry.fromBundle(mResultBundle.getBundle(resourceName));
    }

    private PartnerConfigHelper(Context context) {
        getPartnerConfigBundle(context);
    }


    @VisibleForTesting
    static synchronized void resetForTesting() {
        sInstance = null;
    }
}
