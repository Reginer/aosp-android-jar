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
import static android.provider.flags.Flags.FLAG_NEW_STORAGE_PUBLIC_API;
import static android.provider.flags.Flags.readPlatformFromPlatformApi;

import android.aconfig.storage.AconfigStorageException;
import android.aconfig.storage.FlagTable;
import android.aconfig.storage.FlagValueList;
import android.aconfig.storage.PackageTable;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.os.Build;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@code aconfig} package containing the enabled state of its flags.
 *
 * <p><strong>Note: this is intended only to be used by generated code. To determine if a given flag
 * is enabled in app code, the generated android flags should be used.</strong>
 *
 * <p>This class is used to read the flag from Aconfig Package.Each instance of this class will
 * cache information related to one package. To read flags from a different package, a new instance
 * of this class should be {@link #load loaded}.
 */
@FlaggedApi(FLAG_NEW_STORAGE_PUBLIC_API)
public class AconfigPackage {
    private static final String TAG = "AconfigPackage";
    private static final String MAP_PATH = "/metadata/aconfig/maps/";
    private static final String BOOT_PATH = "/metadata/aconfig/boot/";
    private static final String PMAP_FILE_EXT = ".package.map";

    private static final boolean READ_PLATFORM_FROM_PLATFORM_API =
            readPlatformFromPlatformApi() && Build.VERSION.SDK_INT > 35;

    private FlagTable mFlagTable;
    private FlagValueList mFlagValueList;

    private int mPackageBooleanStartOffset = -1;
    private int mPackageId = -1;

    private PlatformAconfigPackage mPlatformAconfigPackage = null;

    /** @hide */
    static final Map<String, StorageFilesBundle> sStorageFilesCache = new HashMap<>();

    private AconfigPackage() {}

    static {
        File mapDir = new File(MAP_PATH);
        String[] mapFiles = mapDir.list();
        if (mapFiles != null) {
            for (String file : mapFiles) {
                if (!file.endsWith(PMAP_FILE_EXT)
                        || (READ_PLATFORM_FROM_PLATFORM_API
                                && PlatformAconfigPackage.PLATFORM_PACKAGE_MAP_FILES.contains(
                                        file))) {
                    continue;
                }
                try {
                    PackageTable pTable = PackageTable.fromBytes(mapStorageFile(MAP_PATH + file));
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
    }

    /**
     * Loads an Aconfig Package from Aconfig Storage.
     *
     * <p>This method attempts to load the specified Aconfig package.
     *
     * @param packageName The name of the Aconfig package to load.
     * @return An instance of {@link AconfigPackage}, which may be empty if the package is not found
     *     in the container.
     * @throws AconfigStorageReadException if there is an error reading from Aconfig Storage, such
     *     as if the storage system is not found, the package is not found, or there is an error
     *     reading the storage file. The specific error code can be obtained using {@link
     *     AconfigStorageReadException#getErrorCode()}.
     */
    @FlaggedApi(FLAG_NEW_STORAGE_PUBLIC_API)
    public static @NonNull AconfigPackage load(@NonNull String packageName) {
        try {
            AconfigPackage aconfigPackage = new AconfigPackage();

            if (READ_PLATFORM_FROM_PLATFORM_API) {
                aconfigPackage.mPlatformAconfigPackage = PlatformAconfigPackage.load(packageName);
                if (aconfigPackage.mPlatformAconfigPackage != null) {
                    return aconfigPackage;
                }
            }

            StorageFilesBundle files = sStorageFilesCache.get(packageName);
            if (files == null) {
                throw new AconfigStorageReadException(
                        AconfigStorageReadException.ERROR_PACKAGE_NOT_FOUND,
                        "package " + packageName + " cannot be found on the device");
            }

            PackageTable.Node pNode = files.packageTable.get(packageName);
            aconfigPackage.mFlagTable = files.flagTable;
            aconfigPackage.mFlagValueList = files.flagValueList;
            aconfigPackage.mPackageBooleanStartOffset = pNode.getBooleanStartIndex();
            aconfigPackage.mPackageId = pNode.getPackageId();
            return aconfigPackage;
        } catch (AconfigStorageReadException e) {
            throw e;
        } catch (AconfigStorageException e) {
            throw new AconfigStorageReadException(
                    e.getErrorCode(), "Fail to create AconfigPackage", e);
        } catch (Exception e) {
            throw new AconfigStorageReadException(
                    AconfigStorageReadException.ERROR_GENERIC, "Fail to create AconfigPackage", e);
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
     */
    @FlaggedApi(FLAG_NEW_STORAGE_PUBLIC_API)
    public boolean getBooleanFlagValue(@NonNull String flagName, boolean defaultValue) {
        if (READ_PLATFORM_FROM_PLATFORM_API && mPlatformAconfigPackage != null) {
            return mPlatformAconfigPackage.getBooleanFlagValue(flagName, defaultValue);
        }

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
