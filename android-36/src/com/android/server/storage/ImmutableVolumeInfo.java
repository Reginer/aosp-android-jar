/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.storage;

import android.content.Context;
import android.os.storage.DiskInfo;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;

import com.android.internal.util.IndentingPrintWriter;

import java.io.File;

/**
 * An immutable version of {@link VolumeInfo} with only getters.
 *
 * @hide
 */
public final class ImmutableVolumeInfo {
    private final VolumeInfo mVolumeInfo;

    private ImmutableVolumeInfo(VolumeInfo volumeInfo) {
        mVolumeInfo = new VolumeInfo(volumeInfo);
    }

    public static ImmutableVolumeInfo fromVolumeInfo(VolumeInfo info) {
        return new ImmutableVolumeInfo(info);
    }

    public ImmutableVolumeInfo clone() {
        return fromVolumeInfo(mVolumeInfo.clone());
    }

    public StorageVolume buildStorageVolume(Context context, int userId, boolean reportUnmounted) {
        return mVolumeInfo.buildStorageVolume(context, userId, reportUnmounted);
    }

    public void dump(IndentingPrintWriter pw) {
        mVolumeInfo.dump(pw);
    }

    public DiskInfo getDisk() {
        return mVolumeInfo.getDisk();
    }

    public String getDiskId() {
        return mVolumeInfo.getDiskId();
    }

    public String getFsLabel() {
        return mVolumeInfo.fsLabel;
    }

    public String getFsPath() {
        return mVolumeInfo.path;
    }

    public String getFsType() {
        return mVolumeInfo.fsType;
    }

    public String getFsUuid() {
        return mVolumeInfo.fsUuid;
    }

    public String getId() {
        return mVolumeInfo.id;
    }

    public File getInternalPath() {
        return mVolumeInfo.getInternalPath();
    }

    public int getMountFlags() {
        return mVolumeInfo.mountFlags;
    }

    public int getMountUserId() {
        return mVolumeInfo.mountUserId;
    }

    public String getPartGuid() {
        return mVolumeInfo.partGuid;
    }

    public File getPath() {
        return mVolumeInfo.getPath();
    }

    public int getState() {
        return mVolumeInfo.state;
    }

    public int getType() {
        return mVolumeInfo.type;
    }

    public VolumeInfo getVolumeInfo() {
        return new VolumeInfo(mVolumeInfo); // Return a copy, not the original
    }

    public boolean isMountedReadable() {
        return mVolumeInfo.isMountedReadable();
    }

    public boolean isMountedWritable() {
        return mVolumeInfo.isMountedWritable();
    }

    public boolean isPrimary() {
        return mVolumeInfo.isPrimary();
    }

    public boolean isVisible() {
        return mVolumeInfo.isVisible();
    }

    public boolean isVisibleForUser(int userId) {
        return mVolumeInfo.isVisibleForUser(userId);
    }

    public boolean isVisibleForWrite(int userId) {
        return mVolumeInfo.isVisibleForWrite(userId);
    }

    @Override
    public String toString() {
        return mVolumeInfo.toString();
    }
}
