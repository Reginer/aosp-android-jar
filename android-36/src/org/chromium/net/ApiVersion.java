// Copyright 2014 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.net;

/**
 * Build version information for Cronet API code.
 *
 * <p>Note that this class will not necessarily return the same information as
 * {@link org.chromium.net.impl.ImplVersion}. Notably, in the case of Cronet
 * being loaded via Google Play Services, the API and impl are shipped
 * separately and the app can end up running impl code that was not built from
 * the same version as the API code.
 *
 * <p>CAUTION: this class is part of Cronet API code, but is called directly
 * from impl code - be very careful when changing the API/ABI of this class, and
 * keep in mind the caller code is not necessarily built from the same version
 * as this code.
 *
 * @see org.chromium.net.impl.ImplVersion
 * {@hide as it's only used internally}
 */
public class ApiVersion {
    private static final String CRONET_VERSION = "133.0.6876.3";
    private static final int API_LEVEL = 34;
    /**
     * The minimum API level of implementations that are compatible with this API.
     * The last API level which broke backwards API compatibility. In other words, the
     * Cronet API that this class is part of won't work with Cronet implementations that implement
     * API levels less than this value. That is if
     * ImplVersion.getApiLevel() < ApiVersion.getApiLevel(), then the Cronet implementation
     * providing ImplVersion cannot be used with the Cronet API providing ApiVersion; if they are
     * used together various unexpected Errors, like AbstractMethodError, may result.
     */
    private static final int MIN_COMPATIBLE_API_LEVEL = 3;
    private static final String LAST_CHANGE = "dba20708ac7c0069ed00b5debe9da8f855e38be0-refs/heads/main@{#1419267}";

    /**
     * Private constructor. All members of this class should be static.
     */
    private ApiVersion() {}

    public static String getCronetVersionWithLastChange() {
        return CRONET_VERSION + "@" + LAST_CHANGE.substring(0, 8);
    }

    /**
     * Returns API level of the API linked into the application. This is the maximum API
     * level the application can use, even if the application is run with a newer implementation.
     */
    public static int getMaximumAvailableApiLevel() {
        return API_LEVEL;
    }

    /**
     * The *minimum* API level of implementations that are compatible with this API.
     * Not to be confused with the *current* API level, which is returned by {@link
     * #getMaximumAvailableApiLevel}.
     * Returns the last API level which broke backwards API compatibility. In other words, the
     * Cronet API that this class is part of won't work with Cronet implementations that implement
     * API levels less than this value. That is if
     * ImplVersion.getApiLevel() < ApiVersion.getApiLevel(), then the Cronet implementation
     * providing ImplVersion cannot be used with the Cronet API providing ApiVersion; if they are
     * used together various unexpected Errors, like AbstractMethodError, may result.
     */
    public static int getApiLevel() {
        return MIN_COMPATIBLE_API_LEVEL;
    }

    public static String getCronetVersion() {
        return CRONET_VERSION;
    }

    public static String getLastChange() {
        return LAST_CHANGE;
    }
}
