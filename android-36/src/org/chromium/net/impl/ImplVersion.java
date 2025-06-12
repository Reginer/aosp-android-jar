// Copyright 2014 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.net.impl;

/**
 * Build version information for Cronet impl code.
 *
 * <p>Note that this class will not necessarily return the same information as
 * {@link org.chromium.net.ApiVersion}. Notably, in the case of Cronet being
 * loaded via Google Play Services, the API and impl are shipped separately
 * and the app can end up running API code that was not built from the same
 * version as the impl code.
 *
 * <p>CAUTION: this class is used through reflection from the Cronet API code -
 * be very careful when changing the API/ABI of this class, and keep in mind the
 * caller code is not necessarily built from the same version as this code.
 *
 * @see org.chromium.net.ApiVersion
 */
public class ImplVersion {
    private static final String CRONET_VERSION = "133.0.6876.3";
    private static final int API_LEVEL = 34;
    private static final String LAST_CHANGE = "dba20708ac7c0069ed00b5debe9da8f855e38be0-refs/heads/main@{#1419267}";

   /**
    * Private constructor. All members of this class should be static.
    */
    private ImplVersion() {}

    public static String getCronetVersionWithLastChange() {
        return CRONET_VERSION + "@" + LAST_CHANGE.substring(0, 8);
    }

    /**
     * The level of API code that this impl was built against.
     *
     * <p>Note this is *NOT* necessarily the same as the level of the API code
     * that this impl is currently *running* against. The runtime API level can
     * be obtained using {@link
     * org.chromium.net.ApiVersion#getMaximumAvailableApiLevel}.
     */
    public static int getApiLevel() {
        return API_LEVEL;
    }

    public static String getCronetVersion() {
        return CRONET_VERSION;
    }

    public static String getLastChange() {
        return LAST_CHANGE;
    }
}
