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

package com.android.server.storage;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.storage.DiskInfo;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;

import com.android.internal.util.IndentingPrintWriter;
import com.android.server.utils.Watchable;
import com.android.server.utils.WatchableImpl;

import java.io.File;

/**
 * A wrapper for {@link VolumeInfo}  implementing the {@link Watchable} interface.
 *
 * The {@link VolumeInfo} class itself cannot safely implement Watchable, because it has several
 * UnsupportedAppUsage annotations and public fields, which allow it to be modified without
 * notifying watchers.
 *
 * @hide
 */
public class WatchedVolumeInfo extends WatchableImpl {
    private final VolumeInfo mVolumeInfo;

    private WatchedVolumeInfo(VolumeInfo volumeInfo) {
        mVolumeInfo = volumeInfo;
    }

    public WatchedVolumeInfo(WatchedVolumeInfo watchedVolumeInfo) {
        mVolumeInfo = new VolumeInfo(watchedVolumeInfo.mVolumeInfo);
    }

    public static WatchedVolumeInfo fromVolumeInfo(VolumeInfo info) {
        return new WatchedVolumeInfo(info);
    }

    /**
     * Returns a copy of the embedded VolumeInfo object, to be used by components
     * that just need it for retrieving some state from it.
     *
     * @return A copy of the embedded VolumeInfo object
     */

    public WatchedVolumeInfo clone() {
        return fromVolumeInfo(mVolumeInfo.clone());
    }

    public ImmutableVolumeInfo getImmutableVolumeInfo() {
        return ImmutableVolumeInfo.fromVolumeInfo(mVolumeInfo);
    }

    public ImmutableVolumeInfo getClonedImmutableVolumeInfo() {
        return ImmutableVolumeInfo.fromVolumeInfo(mVolumeInfo.clone());
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

    public void setFsLabel(String fsLabel) {
        mVolumeInfo.fsLabel = fsLabel;
        dispatchChange(this);
    }

    public String getFsPath() {
        return mVolumeInfo.path;
    }

    public void setFsPath(String path) {
        mVolumeInfo.path = path;
        dispatchChange(this);
    }

    public String getFsType() {
        return mVolumeInfo.fsType;
    }

    public void setFsType(String fsType) {
        mVolumeInfo.fsType = fsType;
        dispatchChange(this);
    }

    public @Nullable String getFsUuid() {
        return mVolumeInfo.fsUuid;
    }

    public void setFsUuid(String fsUuid) {
        mVolumeInfo.fsUuid = fsUuid;
        dispatchChange(this);
    }

    public @NonNull String getId() {
        return mVolumeInfo.id;
    }

    public File getInternalPath() {
        return mVolumeInfo.getInternalPath();
    }

    public void setInternalPath(String internalPath) {
        mVolumeInfo.internalPath = internalPath;
        dispatchChange(this);
    }

    public int getMountFlags() {
        return mVolumeInfo.mountFlags;
    }

    public void setMountFlags(int mountFlags) {
        mVolumeInfo.mountFlags = mountFlags;
        dispatchChange(this);
    }

    public int getMountUserId() {
        return mVolumeInfo.mountUserId;
    }

    public void setMountUserId(int mountUserId) {
        mVolumeInfo.mountUserId = mountUserId;
        dispatchChange(this);
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

    public int getState(int state) {
        return mVolumeInfo.state;
    }

    public void setState(int state) {
        mVolumeInfo.state = state;
        dispatchChange(this);
    }

    public int getType() {
        return mVolumeInfo.type;
    }

    public VolumeInfo getVolumeInfo() {
        return new VolumeInfo(mVolumeInfo);
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