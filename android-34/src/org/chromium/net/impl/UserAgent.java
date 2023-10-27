// Copyright 2014 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.net.impl;

import android.os.Build;

import java.util.Locale;

/**
 * Constructs a User-Agent string.
 */
public final class UserAgent {

    private UserAgent() {}

    /**
     * Returns a default User-Agent string including  system build version, model and Id,
     * and Cronet version.
     */
    public static String getDefault() {
        return UserAgentsHolder.DEFAULT_USER_AGENT;
    }

    /**
     * Returns default QUIC User Agent Id string including the Cronet version.
     */
    static String getDefaultQuicUserAgentId() {
        return UserAgentsHolder.DEFAULT_QUIC_USER_AGENT;
    }

    private static class UserAgentsHolder {
        static final String DEFAULT_USER_AGENT = createDefaultUserAgent();
        static final String DEFAULT_QUIC_USER_AGENT = createDefaultQuicUserAgent();

        private static String createDefaultQuicUserAgent() {
            StringBuilder builder = new StringBuilder();

            // Application name and cronet version.
            builder.append("AndroidHttpClient");
            appendCronetVersion(builder);

            return builder.toString();
        }

        private static String createDefaultUserAgent() {
            StringBuilder builder = new StringBuilder();

            // Our package name and version.
            builder.append("AndroidHttpClient");

            // The platform version.
            builder.append(" (Linux; U; Android ");
            builder.append(Build.VERSION.RELEASE);
            builder.append("; ");
            builder.append(Locale.getDefault().toString());

            String model = Build.MODEL;
            if (model.length() > 0) {
                builder.append("; ");
                builder.append(model);
            }

            String id = Build.ID;
            if (id.length() > 0) {
                builder.append("; Build/");
                builder.append(id);
            }

            builder.append(";");
            appendCronetVersion(builder);

            builder.append(')');

            return builder.toString();
        }

        private static void appendCronetVersion(StringBuilder builder) {
            builder.append(" Cronet/");
            builder.append(ImplVersion.getCronetVersion());
        }
    }
}
