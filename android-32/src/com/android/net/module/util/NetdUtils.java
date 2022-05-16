/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.net.INetd.IF_STATE_DOWN;
import static android.net.INetd.IF_STATE_UP;

import android.net.INetd;
import android.net.InterfaceConfigurationParcel;
import android.os.RemoteException;
import android.os.ServiceSpecificException;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;

/**
 * Collection of utilities for netd.
 */
public class NetdUtils {
    /**
     * Get InterfaceConfigurationParcel from netd.
     */
    public static InterfaceConfigurationParcel getInterfaceConfigParcel(@NonNull INetd netd,
            @NonNull String iface) {
        try {
            return netd.interfaceGetCfg(iface);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void validateFlag(String flag) {
        if (flag.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("flag contains space: " + flag);
        }
    }

    @VisibleForTesting
    protected static String[] removeAndAddFlags(@NonNull String[] flags, @NonNull String remove,
            @NonNull String add) {
        final ArrayList<String> result = new ArrayList<>();
        try {
            // Validate the add flag first, so that the for-loop can be ignore once the format of
            // add flag is invalid.
            validateFlag(add);
            for (String flag : flags) {
                // Simply ignore both of remove and add flags first, then add the add flag after
                // exiting the loop to prevent adding the duplicate flag.
                if (remove.equals(flag) || add.equals(flag)) continue;
                result.add(flag);
            }
            result.add(add);
            return result.toArray(new String[result.size()]);
        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException("Invalid InterfaceConfigurationParcel", iae);
        }
    }

    /**
     * Set interface configuration to netd by passing InterfaceConfigurationParcel.
     */
    public static void setInterfaceConfig(INetd netd, InterfaceConfigurationParcel configParcel) {
        try {
            netd.interfaceSetCfg(configParcel);
        } catch (RemoteException | ServiceSpecificException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Set the given interface up.
     */
    public static void setInterfaceUp(INetd netd, String iface) {
        final InterfaceConfigurationParcel configParcel = getInterfaceConfigParcel(netd, iface);
        configParcel.flags = removeAndAddFlags(configParcel.flags, IF_STATE_DOWN /* remove */,
                IF_STATE_UP /* add */);
        setInterfaceConfig(netd, configParcel);
    }

    /**
     * Set the given interface down.
     */
    public static void setInterfaceDown(INetd netd, String iface) {
        final InterfaceConfigurationParcel configParcel = getInterfaceConfigParcel(netd, iface);
        configParcel.flags = removeAndAddFlags(configParcel.flags, IF_STATE_UP /* remove */,
                IF_STATE_DOWN /* add */);
        setInterfaceConfig(netd, configParcel);
    }
}
