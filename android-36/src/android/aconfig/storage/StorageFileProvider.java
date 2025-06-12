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

package android.aconfig.storage;

import java.io.Closeable;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** @hide */
public class StorageFileProvider {

    private static final String DEFAULT_MAP_PATH = "/metadata/aconfig/maps/";
    private static final String DEFAULT_BOOT_PATH = "/metadata/aconfig/boot/";
    private static final String PMAP_FILE_EXT = ".package.map";
    private static final String FMAP_FILE_EXT = ".flag.map";
    private static final String VAL_FILE_EXT = ".val";
    private static final StorageFileProvider DEFAULT_INSTANCE =
            new StorageFileProvider(DEFAULT_MAP_PATH, DEFAULT_BOOT_PATH);

    private final String mMapPath;
    private final String mBootPath;

    /** @hide */
    public static StorageFileProvider getDefaultProvider() {
        return DEFAULT_INSTANCE;
    }

    /** @hide */
    public StorageFileProvider(String mapPath, String bootPath) {
        mMapPath = mapPath;
        mBootPath = bootPath;
    }

    /** @hide */
    public List<String> listContainers(String[] excludes) {
        List<String> result = new ArrayList<>();
        Set<String> set = new HashSet<>(Arrays.asList(excludes));

        try {
            DirectoryStream<Path> stream =
                    Files.newDirectoryStream(Paths.get(mMapPath), "*" + PMAP_FILE_EXT);
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();
                String container =
                        fileName.substring(0, fileName.length() - PMAP_FILE_EXT.length());
                if (!set.contains(container)) {
                    result.add(container);
                }
            }
        } catch (NoSuchFileException e) {
            return result;
        } catch (Exception e) {
            throw new AconfigStorageException(
                    String.format("Fail to list map files in path %s", mMapPath), e);
        }

        return result;
    }

    /** @hide */
    public PackageTable getPackageTable(String container) {
        return PackageTable.fromBytes(
                mapStorageFile(
                        Paths.get(mMapPath, container + PMAP_FILE_EXT), FileType.PACKAGE_MAP));
    }

    /** @hide */
    public FlagTable getFlagTable(String container) {
        return FlagTable.fromBytes(
                mapStorageFile(Paths.get(mMapPath, container + FMAP_FILE_EXT), FileType.FLAG_MAP));
    }

    /** @hide */
    public FlagValueList getFlagValueList(String container) {
        return FlagValueList.fromBytes(
                mapStorageFile(Paths.get(mBootPath, container + VAL_FILE_EXT), FileType.FLAG_VAL));
    }

    // Map a storage file given file path
    private static MappedByteBuffer mapStorageFile(Path file, FileType type) {
        FileChannel channel = null;
        try {
            channel = FileChannel.open(file, StandardOpenOption.READ);
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        } catch (Exception e) {
            throw new AconfigStorageException(
                    AconfigStorageException.ERROR_CANNOT_READ_STORAGE_FILE,
                    String.format("Fail to mmap storage %s file %s", type.toString(), file),
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
