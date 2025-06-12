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

package android.os.flagging;

import static android.aconfig.storage.TableUtils.StorageFilesBundle;

import android.aconfig.storage.AconfigStorageException;
import android.aconfig.storage.FlagTable;
import android.aconfig.storage.FlagValueList;
import android.aconfig.storage.PackageTable;
import android.compat.annotation.UnsupportedAppUsage;
import android.util.Log;

import java.io.Closeable;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An {@code aconfig} package containing the enabled state of its flags.
 *
 * <p><strong>Note: this is intended only to be used by generated code. To determine if a given flag
 * is enabled in app code, the generated android flags should be used.</strong>
 *
 * <p>This class is used to read the flag from platform Aconfig Package.Each instance of this class
 * will cache information related to one package. To read flags from a different package, a new
 * instance of this class should be {@link #load loaded}.
 *
 * @hide
 */
public class PlatformAconfigPackage {
    private static final String TAG = "PlatformAconfigPackage";
    private static final String MAP_PATH = "/metadata/aconfig/maps/";
    private static final String BOOT_PATH = "/metadata/aconfig/boot/";

    private FlagTable mFlagTable;
    private FlagValueList mFlagValueList;

    private int mPackageBooleanStartOffset = -1;
    private int mPackageId = -1;

    private PlatformAconfigPackage() {}

    /** @hide */
    static final Map<String, StorageFilesBundle> sStorageFilesCache = new HashMap<>();

    /** @hide */
    @UnsupportedAppUsage
    public static final Set<String> PLATFORM_PACKAGE_MAP_FILES =
            Set.of("system.package.map", "vendor.package.map", "product.package.map");

    static {
        for (String pf : PLATFORM_PACKAGE_MAP_FILES) {
            try {
                PackageTable pTable = PackageTable.fromBytes(mapStorageFile(MAP_PATH + pf));
                String container = pTable.getHeader().getContainer();
                FlagTable fTable =
                        FlagTable.fromBytes(mapStorageFile(MAP_PATH + container + ".flag.map"));
                FlagValueList fValueList =
                        FlagValueList.fromBytes(mapStorageFile(BOOT_PATH + container + ".val"));
                StorageFilesBundle files = new StorageFilesBundle(pTable, fTable, fValueList);
                for (String packageName : pTable.getPackageList()) {
                    sStorageFilesCache.put(packageName, files);
                }
            } catch (Exception e) {
                // pass
                Log.w(TAG, e.toString());
            }
        }
    }

    /**
     * Loads a platform Aconfig Package from Aconfig Storage.
     *
     * <p>This method attempts to load the specified platform Aconfig package.
     *
     * @param packageName The name of the Aconfig package to load.
     * @return An instance of {@link PlatformAconfigPackage}, which may be empty if the package is
     *     not found in the container. Null if the package is not found in platform partitions.
     * @throws AconfigStorageReadException if there is an error reading from Aconfig Storage, such
     *     as if the storage system is not found, or there is an error reading the storage file. The
     *     specific error code can be got using {@link AconfigStorageReadException#getErrorCode()}.
     * @hide
     */
    @UnsupportedAppUsage
    public static PlatformAconfigPackage load(String packageName) {
        try {
            PlatformAconfigPackage aconfigPackage = new PlatformAconfigPackage();
            StorageFilesBundle files = sStorageFilesCache.get(packageName);
            if (files == null) {
                return null;
            }
            PackageTable.Node pNode = files.packageTable.get(packageName);
            aconfigPackage.mFlagTable = files.flagTable;
            aconfigPackage.mFlagValueList = files.flagValueList;
            aconfigPackage.mPackageBooleanStartOffset = pNode.getBooleanStartIndex();
            aconfigPackage.mPackageId = pNode.getPackageId();
            return aconfigPackage;
        } catch (AconfigStorageException e) {
            throw new AconfigStorageReadException(
                    e.getErrorCode(), "Fail to create AconfigPackage", e);
        } catch (Exception e) {
            throw new AconfigStorageReadException(
                    AconfigStorageReadException.ERROR_GENERIC,
                    "Fail to create PlatformAconfigPackage",
                    e);
        }
    }

    /**
     * Retrieves the value of a boolean flag.
     *
     * <p>This method retrieves the value of the specified flag. If the flag exists within the
     * loaded Aconfig Package, its value is returned. Otherwise, the provided `defaultValue` is
     * returned.
     *
     * @param flagName The name of the flag (excluding any package name prefix).
     * @param defaultValue The value to return if the flag is not found.
     * @return The boolean value of the flag, or `defaultValue` if the flag is not found.
     * @hide
     */
    @UnsupportedAppUsage
    public boolean getBooleanFlagValue(String flagName, boolean defaultValue) {
        FlagTable.Node fNode = mFlagTable.get(mPackageId, flagName);
        if (fNode == null) {
            return defaultValue;
        }
        return mFlagValueList.getBoolean(fNode.getFlagIndex() + mPackageBooleanStartOffset);
    }

    // Map a storage file given file path
    private static MappedByteBuffer mapStorageFile(String file) {
        FileChannel channel = null;
        try {
            channel = FileChannel.open(Paths.get(file), StandardOpenOption.READ);
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        } catch (Exception e) {
            throw new AconfigStorageReadException(
                    AconfigStorageReadException.ERROR_CANNOT_READ_STORAGE_FILE,
                    "Fail to mmap storage",
                    e);
        } finally {
            quietlyDispose(channel);
        }
    }

    private static void quietlyDispose(Closeable closable) {
        try {
            if (closable != null) {
                closable.close();
            }
        } catch (Exception e) {
            // no need to care, at least as of now
        }
    }
}
