/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.health.connect.internal;

import android.annotation.NonNull;
import android.os.IBinder;
import android.os.Parcel;
import android.os.SharedMemory;
import android.system.ErrnoException;

import com.android.internal.annotations.VisibleForTesting;

import java.nio.ByteBuffer;

/** @hide */
public final class ParcelUtils {
    @VisibleForTesting public static final int USING_SHARED_MEMORY = 0;
    @VisibleForTesting public static final int USING_PARCEL = 1;

    @VisibleForTesting
    public static final int IPC_PARCEL_LIMIT = IBinder.getSuggestedMaxIpcSizeBytes() / 2;

    public interface IPutToParcelRunnable {
        void writeToParcel(Parcel dest);
    }

    @NonNull
    public static Parcel getParcelForSharedMemoryIfRequired(Parcel in) {
        int parcelType = in.readInt();
        if (parcelType == USING_SHARED_MEMORY) {
            try (SharedMemory memory = SharedMemory.CREATOR.createFromParcel(in)) {
                Parcel dataParcel = Parcel.obtain();
                ByteBuffer buffer = memory.mapReadOnly();
                byte[] payload = new byte[buffer.limit()];
                buffer.get(payload);
                dataParcel.unmarshall(payload, 0, payload.length);
                dataParcel.setDataPosition(0);
                return dataParcel;
            } catch (ErrnoException e) {
                throw new RuntimeException(e);
            }
        }
        return in;
    }

    public static SharedMemory getSharedMemoryForParcel(Parcel dataParcel, int dataParcelSize) {
        try {
            SharedMemory sharedMemory =
                    SharedMemory.create("RecordsParcelSharedMemory", dataParcelSize);
            ByteBuffer buffer = sharedMemory.mapReadWrite();
            byte[] data = dataParcel.marshall();
            buffer.put(data, 0, dataParcelSize);
            return sharedMemory;
        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Determines which memory to use and puts the {@code parcel} in it, and details of it in {@code
     * dest}
     */
    public static void putToRequiredMemory(
            Parcel dest, int flags, IPutToParcelRunnable parcelRunnable) {
        final Parcel dataParcel = Parcel.obtain();
        try {
            parcelRunnable.writeToParcel(dataParcel);
            final int dataParcelSize = dataParcel.dataSize();
            if (dataParcelSize > IPC_PARCEL_LIMIT) {
                SharedMemory sharedMemory =
                        ParcelUtils.getSharedMemoryForParcel(dataParcel, dataParcelSize);
                dest.writeInt(USING_SHARED_MEMORY);
                sharedMemory.writeToParcel(dest, flags);
            } else {
                dest.writeInt(USING_PARCEL);
                parcelRunnable.writeToParcel(dest);
            }
        } finally {
            dataParcel.recycle();
        }
    }
}
