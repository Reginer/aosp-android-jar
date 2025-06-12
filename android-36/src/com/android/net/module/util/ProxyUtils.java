/*
 * Copyright 2020 The Android Open Source Project
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

package com.android.net.module.util;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collection of network common utilities.
 *
 * @hide
 */
public final class ProxyUtils {

    public static final int PROXY_VALID             = 0;
    public static final int PROXY_HOSTNAME_EMPTY    = 1;
    public static final int PROXY_HOSTNAME_INVALID  = 2;
    public static final int PROXY_PORT_EMPTY        = 3;
    public static final int PROXY_PORT_INVALID      = 4;
    public static final int PROXY_EXCLLIST_INVALID  = 5;

    // Hostname / IP REGEX validation
    // Matches blank input, ips, and domain names
    private static final String NAME_IP_REGEX =
            "[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*(\\.[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*)*";
    private static final Pattern HOSTNAME_PATTERN;
    private static final String HOSTNAME_REGEXP = "^$|^" + NAME_IP_REGEX + "$";
    private static final Pattern EXCLLIST_PATTERN;
    private static final String EXCL_REGEX =
            "[a-zA-Z0-9*]+(\\-[a-zA-Z0-9*]+)*(\\.[a-zA-Z0-9*]+(\\-[a-zA-Z0-9*]+)*)*";
    private static final String EXCLLIST_REGEXP = "^$|^" + EXCL_REGEX + "(," + EXCL_REGEX + ")*$";
    static {
        HOSTNAME_PATTERN = Pattern.compile(HOSTNAME_REGEXP);
        EXCLLIST_PATTERN = Pattern.compile(EXCLLIST_REGEXP);
    }

    /** Converts exclusion list from String to List. */
    public static List<String> exclusionStringAsList(String exclusionList) {
        if (exclusionList == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(exclusionList.toLowerCase(Locale.ROOT).split(","));
    }

    /** Converts exclusion list from List to string */
    public static String exclusionListAsString(String[] exclusionList) {
        if (exclusionList == null) {
            return "";
        }
        return TextUtils.join(",", exclusionList);
    }

    /**
     * Validate syntax of hostname, port and exclusion list entries
     */
    public static int validate(String hostname, String port, String exclList) {
        Matcher match = HOSTNAME_PATTERN.matcher(hostname);
        Matcher listMatch = EXCLLIST_PATTERN.matcher(exclList);

        if (!match.matches()) return PROXY_HOSTNAME_INVALID;

        if (!listMatch.matches()) return PROXY_EXCLLIST_INVALID;

        if (hostname.length() > 0 && port.length() == 0) return PROXY_PORT_EMPTY;

        if (port.length() > 0) {
            if (hostname.length() == 0) return PROXY_HOSTNAME_EMPTY;
            int portVal = -1;
            try {
                portVal = Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                return PROXY_PORT_INVALID;
            }
            if (portVal <= 0 || portVal > 0xFFFF) return PROXY_PORT_INVALID;
        }
        return PROXY_VALID;
    }
}
