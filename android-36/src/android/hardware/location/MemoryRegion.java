/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.hardware.location;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.chre.flags.Flags;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * @hide
 */
@SystemApi
public class MemoryRegion implements Parcelable{

    private int mSizeBytes;
    private int mSizeBytesFree;
    private boolean mIsReadable;
    private boolean mIsWritable;
    private boolean mIsExecutable;

    /**
     * get the capacity of the memory region in bytes
     *
     * @return int - the memory capacity in bytes
     */
    public int getCapacityBytes() {
        return mSizeBytes;
    }

    /**
     * get the free capacity of the memory region in bytes
     *
     * @return int - free bytes
     */
    public int getFreeCapacityBytes() {
        return mSizeBytesFree;
    }

    /**
     * Is the memory readable
     *
     * @return boolean - true if memory is readable, false otherwise
     */
    public boolean isReadable() {
        return mIsReadable;
    }

    /**
     * Is the memory writable
     *
     * @return boolean - true if memory is writable, false otherwise
     */
    public boolean isWritable() {
        return mIsWritable;
    }

    /**
     * Is the memory executable
     *
     * @return boolean - true if memory is executable, false
     *         otherwise
     */
    public boolean isExecutable() {
        return mIsExecutable;
    }

    @NonNull
    @Override
    public String toString() {
        String mask = "";

        if (isReadable()) {
            mask += "r";
        } else {
            mask += "-";
        }

        if (isWritable()) {
            mask += "w";
        } else {
            mask += "-";
        }

        if (isExecutable()) {
            mask += "x";
        } else {
            mask += "-";
        }

        String retVal = "[ " + mSizeBytesFree + "/ " + mSizeBytes + " ] : " + mask;

        return retVal;
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        }

        boolean isEqual = false;
        if (object instanceof MemoryRegion) {
            MemoryRegion other = (MemoryRegion) object;
            isEqual = (other.getCapacityBytes() == mSizeBytes)
                    && (other.getFreeCapacityBytes() == mSizeBytesFree)
                    && (other.isReadable() == mIsReadable)
                    && (other.isWritable() == mIsWritable)
                    && (other.isExecutable() == mIsExecutable);
        }

        return isEqual;
    }

    @Override
    public int hashCode() {
        if (!Flags.fixApiCheck()) {
            return super.hashCode();
        }

        return Objects.hash(mSizeBytes, mSizeBytesFree, mIsReadable,
                mIsWritable, mIsExecutable);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mSizeBytes);
        dest.writeInt(mSizeBytesFree);
        dest.writeInt(mIsReadable ? 1 : 0);
        dest.writeInt(mIsWritable ? 1 : 0);
        dest.writeInt(mIsExecutable ? 1 : 0);
    }

    public MemoryRegion(Parcel source) {
        mSizeBytes = source.readInt();
        mSizeBytesFree = source.readInt();
        mIsReadable = source.readInt() != 0;
        mIsWritable = source.readInt() != 0;
        mIsExecutable = source.readInt() != 0;
    }

    public static final @android.annotation.NonNull Parcelable.Creator<MemoryRegion> CREATOR
            = new Parcelable.Creator<MemoryRegion>() {
        public MemoryRegion createFromParcel(Parcel in) {
            return new MemoryRegion(in);
        }

        public MemoryRegion[] newArray(int size) {
            return new MemoryRegion[size];
        }
    };

}
