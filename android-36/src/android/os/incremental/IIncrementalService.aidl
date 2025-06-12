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

package android.os.incremental;

import android.content.pm.DataLoaderParamsParcel;
import android.content.pm.IDataLoaderStatusListener;
import android.os.incremental.IncrementalNewFileParams;
import android.os.incremental.IStorageLoadingProgressListener;
import android.os.incremental.IStorageHealthListener;
import android.os.incremental.PerUidReadTimeouts;
import android.os.incremental.StorageHealthCheckParams;
import android.os.PersistableBundle;

/** @hide */
interface IIncrementalService {
    /**
     * A set of flags for the |createMode| parameters when creating a new Incremental storage.
     */
    const int CREATE_MODE_TEMPORARY_BIND = 1;
    const int CREATE_MODE_PERMANENT_BIND = 2;
    const int CREATE_MODE_CREATE = 4;
    const int CREATE_MODE_OPEN_EXISTING = 8;

    /**
     * Opens or creates a storage given a target path and data loader params. Returns the storage ID.
     */
    int openStorage(in @utf8InCpp String path);
    int createStorage(in @utf8InCpp String path, in DataLoaderParamsParcel params, int createMode);
    int createLinkedStorage(in @utf8InCpp String path, int otherStorageId, int createMode);

    /**
     * Loops DataLoader through bind/create/start with params.
     */
    boolean startLoading(int storageId,
                         in DataLoaderParamsParcel params,
                         in IDataLoaderStatusListener statusListener,
                         in StorageHealthCheckParams healthCheckParams,
                         in IStorageHealthListener healthListener,
                         in PerUidReadTimeouts[] perUidReadTimeouts);

    /**
     * PM/system is done with this storage, ok to increase timeouts.
     */
    void onInstallationComplete(int storageId);

    /**
     * Bind-mounts a path under a storage to a full path. Can be permanent or temporary.
     */
    const int BIND_TEMPORARY = 0;
    const int BIND_PERMANENT = 1;
    int makeBindMount(int storageId, in @utf8InCpp String sourcePath, in @utf8InCpp String targetFullPath, int bindType);

    /**
     * Deletes an existing bind mount on a path under a storage. Returns 0 on success, and -errno on failure.
     */
    int deleteBindMount(int storageId, in @utf8InCpp String targetFullPath);

    /**
     * Creates a directory under a storage. The target directory is specified by its path.
     */
    int makeDirectory(int storageId, in @utf8InCpp String path);

    /**
     * Recursively creates a directory under a storage. The target directory is specified by its path.
     * All the parent directories of the target directory will be created if they do not exist already.
     */
    int makeDirectories(int storageId, in @utf8InCpp String path);

    /**
     * Creates a file under a storage.
     */
    int makeFile(int storageId, in @utf8InCpp String path, int mode, in IncrementalNewFileParams params, in @nullable byte[] content);

    /**
     * Creates a file under a storage. Content of the file is from a range inside another file.
     * Both files are specified by their paths.
     */
    int makeFileFromRange(int storageId, in @utf8InCpp String targetPath, in @utf8InCpp String sourcePath, long start, long end);

    /**
     * Creates a hard link between two files in two storage instances.
     * Source and dest specified by parent storage IDs and their paths.
     * The source and dest storage instances should be in the same fs mount.
     * Note: destStorageId can be the same as sourceStorageId.
     */
    int makeLink(int sourceStorageId, in @utf8InCpp String sourcePath, int destStorageId, in @utf8InCpp String destPath);

    /**
     * Deletes a hard link in a storage, specified by its path.
     */
    int unlink(int storageId, in @utf8InCpp String path);

    /**
     * Checks if a file is fully loaded. File is specified by its path.
     * 0 - fully loaded
     * >0 - certain pages missing
     * <0 - -errcode
     */
    int isFileFullyLoaded(int storageId, in @utf8InCpp String path);

    /**
     * Checks if all files in the storage are fully loaded.
     * 0 - fully loaded
     * >0 - certain pages missing
     * <0 - -errcode
     */
    int isFullyLoaded(int storageId);

    /**
     * Returns overall loading progress of all the files on a storage, progress value between [0,1].
     * Returns a negative value on error.
     */
    float getLoadingProgress(int storageId);

    /**
     * Reads the metadata of a file. File is specified by either its path or 16 byte id.
     */
    byte[] getMetadataByPath(int storageId, in @utf8InCpp String path);
    byte[] getMetadataById(int storageId, in byte[] fileId);

    /**
     * Deletes a storage given its ID. Deletes its bind mounts and unmount it. Stop its data loader.
     */
    void deleteStorage(int storageId);

    /**
     * Permanently disable readlogs reporting for a storage given its ID.
     */
    void disallowReadLogs(int storageId);

    /**
     * Setting up native library directories and extract native libs onto a storage if needed.
     */
    boolean configureNativeBinaries(int storageId, in @utf8InCpp String apkFullPath, in @utf8InCpp String libDirRelativePath, in @utf8InCpp String abi, boolean extractNativeLibs);

    /**
     * Waits until all native library extraction is done for the storage
     */
    boolean waitForNativeBinariesExtraction(int storageId);

    /**
     * Register to start listening for loading progress change for a storage.
     */
    boolean registerLoadingProgressListener(int storageId, IStorageLoadingProgressListener listener);

    /**
     * Stop listening for the loading progress change for a storage.
     */
    boolean unregisterLoadingProgressListener(int storageId);

    /**
     * Metrics key for the duration in milliseconds between now and the oldest pending read. The value is a long.
     */
    const @utf8InCpp String METRICS_MILLIS_SINCE_OLDEST_PENDING_READ = "millisSinceOldestPendingRead";
    /**
     * Metrics key for whether read logs are enabled. The value is a boolean.
     */
    const @utf8InCpp String METRICS_READ_LOGS_ENABLED = "readLogsEnabled";
    /**
     * Metrics key for the storage health status. The value is an int.
     */
    const @utf8InCpp String METRICS_STORAGE_HEALTH_STATUS_CODE = "storageHealthStatusCode";
    /**
     * Metrics key for the data loader status. The value is an int.
     */
    const @utf8InCpp String METRICS_DATA_LOADER_STATUS_CODE = "dataLoaderStatusCode";
    /**
     * Metrics key for duration since last data loader binding attempt. The value is a long.
     */
    const @utf8InCpp String METRICS_MILLIS_SINCE_LAST_DATA_LOADER_BIND = "millisSinceLastDataLoaderBind";
    /**
     * Metrics key for delay in milliseconds to retry data loader binding. The value is a long.
     */
    const @utf8InCpp String METRICS_DATA_LOADER_BIND_DELAY_MILLIS = "dataLoaderBindDelayMillis";
    /**
     * Metrics key for total count of delayed reads caused by pending reads. The value is an int.
     */
    const @utf8InCpp String METRICS_TOTAL_DELAYED_READS = "totalDelayedReads";
    /**
     * Metrics key for total count of delayed reads caused by pending reads. The value is an int.
     */
    const @utf8InCpp String METRICS_TOTAL_DELAYED_READS_MILLIS = "totalDelayedReadsMillis";
    /**
     * Metrics key for total count of failed reads. The value is an int.
     */
    const @utf8InCpp String METRICS_TOTAL_FAILED_READS = "totalFailedReads";
    /**
     * Metrics key for the uid of the last read error. The value is an int.
     */
    const @utf8InCpp String METRICS_LAST_READ_ERROR_UID = "lastReadErrorUid";
    /**
     * Metrics key for duration in milliseconds since the last read error. The value is a long.
     */
    const @utf8InCpp String METRICS_MILLIS_SINCE_LAST_READ_ERROR = "millisSinceLastReadError";
    /**
     * Metrics key for the error number of the last read error. The value is an int.
     */
    const @utf8InCpp String METRICS_LAST_READ_ERROR_NUMBER = "lastReadErrorNo";
    /**
     * Return a bundle containing the requested metrics keys and their values.
     */
    PersistableBundle getMetrics(int storageId);
}
