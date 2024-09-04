// Copyright 2014 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.net.impl;

// Version based on chrome/VERSION.
public class ImplVersion {
    private static final String CRONET_VERSION = "121.0.6167.71";
    private static final int API_LEVEL = 26;
    private static final String LAST_CHANGE = "4b7cc55f9aca1fe017b847e8e10091161961ddab-refs/branch-heads/6167@{#1425}";

   /**
    * Private constructor. All members of this class should be static.
    */
    private ImplVersion() {}

    public static String getCronetVersionWithLastChange() {
        return CRONET_VERSION + "@" + LAST_CHANGE.substring(0, 8);
    }

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
