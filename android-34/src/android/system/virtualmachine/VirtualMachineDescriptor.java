/*
 * Copyright 2022 The Android Open Source Project
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

package android.system.virtualmachine;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

import java.io.IOException;

/**
 * A VM descriptor that captures the state of a Virtual Machine.
 *
 * <p>You can capture the current state of VM by creating an instance of this class with {@link
 * VirtualMachine#toDescriptor}, optionally pass it to another App, and then build an identical VM
 * with the descriptor received.
 *
 * @hide
 */
@SystemApi
public final class VirtualMachineDescriptor implements Parcelable, AutoCloseable {
    private volatile boolean mClosed = false;
    @NonNull private final ParcelFileDescriptor mConfigFd;
    @NonNull private final ParcelFileDescriptor mInstanceImgFd;
    // File descriptor of the image backing the encrypted storage - Will be null if encrypted
    // storage is not enabled. */
    @Nullable private final ParcelFileDescriptor mEncryptedStoreFd;

    @Override
    public int describeContents() {
        return CONTENTS_FILE_DESCRIPTOR;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        checkNotClosed();
        out.writeParcelable(mConfigFd, flags);
        out.writeParcelable(mInstanceImgFd, flags);
        out.writeParcelable(mEncryptedStoreFd, flags);
    }

    @NonNull
    public static final Parcelable.Creator<VirtualMachineDescriptor> CREATOR =
            new Parcelable.Creator<>() {
                public VirtualMachineDescriptor createFromParcel(Parcel in) {
                    return new VirtualMachineDescriptor(in);
                }

                public VirtualMachineDescriptor[] newArray(int size) {
                    return new VirtualMachineDescriptor[size];
                }
            };

    /**
     * @return File descriptor of the VM configuration file config.xml.
     */
    @NonNull
    ParcelFileDescriptor getConfigFd() {
        checkNotClosed();
        return mConfigFd;
    }

    /**
     * @return File descriptor of the instance.img of the VM.
     */
    @NonNull
    ParcelFileDescriptor getInstanceImgFd() {
        checkNotClosed();
        return mInstanceImgFd;
    }

    /**
     * @return File descriptor of image backing the encrypted storage.
     *     <p>This method will return null if encrypted storage is not enabled.
     */
    @Nullable
    ParcelFileDescriptor getEncryptedStoreFd() {
        checkNotClosed();
        return mEncryptedStoreFd;
    }

    VirtualMachineDescriptor(
            @NonNull ParcelFileDescriptor configFd,
            @NonNull ParcelFileDescriptor instanceImgFd,
            @Nullable ParcelFileDescriptor encryptedStoreFd) {
        mConfigFd = requireNonNull(configFd);
        mInstanceImgFd = requireNonNull(instanceImgFd);
        mEncryptedStoreFd = encryptedStoreFd;
    }

    private VirtualMachineDescriptor(Parcel in) {
        mConfigFd = requireNonNull(readParcelFileDescriptor(in));
        mInstanceImgFd = requireNonNull(readParcelFileDescriptor(in));
        mEncryptedStoreFd = readParcelFileDescriptor(in);
    }

    private ParcelFileDescriptor readParcelFileDescriptor(Parcel in) {
        return in.readParcelable(
                ParcelFileDescriptor.class.getClassLoader(), ParcelFileDescriptor.class);
    }

    /**
     * Release any resources held by this descriptor. Calling {@code close} on an already-closed
     * descriptor has no effect.
     */
    @Override
    public void close() {
        mClosed = true;
        // Let the compiler do the work: close everything, throw if any of them fail, skipping null.
        try (mConfigFd;
                mInstanceImgFd;
                mEncryptedStoreFd) {
        } catch (IOException ignored) {
            // PFD already swallows exceptions from closing the fd. There's no reason to propagate
            // this to the caller.
        }
    }

    private void checkNotClosed() {
        if (mClosed) {
            throw new IllegalStateException("Descriptor has been closed");
        }
    }
}
