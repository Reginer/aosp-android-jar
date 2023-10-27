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

package android.os.ext;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Build.VERSION_CODES;
import android.os.SystemProperties;

import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Methods for interacting with the extension SDK.
 *
 * This class provides information about the extension SDK versions present
 * on this device. Use the {@link #getExtensionVersion(int) getExtension} method
 * to lookup the version of a given extension.
 *
 * The extension version advances as the platform evolves and new APIs are added,
 * so is suitable to use for determining API availability at runtime.
 */
public class SdkExtensions {

    public static final int AD_SERVICES = 1_000_000;

    private static final int R_EXTENSION_INT;
    private static final int S_EXTENSION_INT;
    private static final int T_EXTENSION_INT;
    private static final int U_EXTENSION_INT;
    private static final int AD_SERVICES_EXTENSION_INT;
    private static final Map<Integer, Integer> ALL_EXTENSION_INTS;
    static {
        R_EXTENSION_INT = SystemProperties.getInt("build.version.extensions.r", 0);
        S_EXTENSION_INT = SystemProperties.getInt("build.version.extensions.s", 0);
        T_EXTENSION_INT = SystemProperties.getInt("build.version.extensions.t", 0);
        U_EXTENSION_INT = SystemProperties.getInt("build.version.extensions.u", 0);
        AD_SERVICES_EXTENSION_INT =
                SystemProperties.getInt("build.version.extensions.ad_services", 0);
        Map<Integer, Integer> extensions = new HashMap<Integer, Integer>();
        extensions.put(VERSION_CODES.R, R_EXTENSION_INT);
        if (SdkLevel.isAtLeastS()) {
            extensions.put(VERSION_CODES.S, S_EXTENSION_INT);
        }
        if (SdkLevel.isAtLeastT()) {
            extensions.put(VERSION_CODES.TIRAMISU, T_EXTENSION_INT);
            extensions.put(AD_SERVICES, AD_SERVICES_EXTENSION_INT);
        }
        if (SdkLevel.isAtLeastU()) {
            extensions.put(VERSION_CODES.UPSIDE_DOWN_CAKE, U_EXTENSION_INT);
        }
        ALL_EXTENSION_INTS = Collections.unmodifiableMap(extensions);
    }

    /**
     * Values suitable as parameters for {@link #getExtensionVersion(int)}.
     * @hide
     */
    @IntDef(value = {
          VERSION_CODES.R,
          VERSION_CODES.S,
          VERSION_CODES.TIRAMISU,
          VERSION_CODES.UPSIDE_DOWN_CAKE,
          AD_SERVICES,
        })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Extension {}

    private SdkExtensions() { }

    /**
     * Return the version of the specified extensions.
     *
     * This method is suitable to use in conditional statements to determine whether an API is
     * available and is safe to use. For example:
     * <pre>
     * if (getExtensionVersion(VERSION_CODES.R) >= 3) {
     *   // Safely use API available since R extensions version 3
     * }
     * </pre>
     *
     * @param extension the extension to get the version of.
     * @throws IllegalArgumentException if extension is not a valid extension
     */
    public static int getExtensionVersion(@Extension int extension) {
        if (extension < VERSION_CODES.R) {
            throw new IllegalArgumentException("not a valid extension: " + extension);
        }

        if (extension == VERSION_CODES.R) {
            return R_EXTENSION_INT;
        }
        if (extension == VERSION_CODES.S) {
            return S_EXTENSION_INT;
        }
        if (extension == VERSION_CODES.TIRAMISU) {
            return T_EXTENSION_INT;
        }
        if (extension == VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return U_EXTENSION_INT;
        }
        if (extension == AD_SERVICES) {
            return AD_SERVICES_EXTENSION_INT;
        }
        return 0;
    }

    /**
     * Return all extension versions that exist on this device.
     *
     * @return a map from extension to extension version.
     */
    @NonNull
    public static Map<Integer, Integer> getAllExtensionVersions() {
        return ALL_EXTENSION_INTS;
    }

}
