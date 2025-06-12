/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.app.sdksandbox;


import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.pm.SharedLibraryInfo;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Represents an SDK loaded in the sandbox process.
 *
 * <p>Returned in response to {@link SdkSandboxManager#loadSdk}, on success. An application can
 * obtain it by calling {@link SdkSandboxManager#loadSdk}. It should use this object to obtain an
 * interface to the SDK through {@link #getInterface()}.
 *
 * <p>The SDK should create it when {@link SandboxedSdkProvider#onLoadSdk} is called, and drop all
 * references to it when {@link SandboxedSdkProvider#beforeUnloadSdk()} is called. Additionally, the
 * SDK should fail calls made to the {@code IBinder} returned from {@link #getInterface()} after
 * {@link SandboxedSdkProvider#beforeUnloadSdk()} has been called.
 */
public final class SandboxedSdk implements Parcelable {
    public static final @NonNull Creator<SandboxedSdk> CREATOR =
            new Creator<SandboxedSdk>() {
                @Override
                public SandboxedSdk createFromParcel(Parcel in) {
                    return new SandboxedSdk(in);
                }

                @Override
                public SandboxedSdk[] newArray(int size) {
                    return new SandboxedSdk[size];
                }
            };
    private IBinder mInterface;
    private @Nullable SharedLibraryInfo mSharedLibraryInfo;

    /**
     * Creates a {@link SandboxedSdk} object.
     *
     * @param sdkInterface The SDK's interface. This will be the entrypoint into the sandboxed SDK
     *     for the application. The SDK should keep this valid until it's loaded in the sandbox, and
     *     start failing calls to this interface once it has been unloaded.
     *     <p>This interface can later be retrieved using {@link #getInterface()}.
     */
    public SandboxedSdk(@NonNull IBinder sdkInterface) {
        mInterface = sdkInterface;
    }

    private SandboxedSdk(@NonNull Parcel in) {
        mInterface = in.readStrongBinder();
        if (in.readInt() != 0) {
            mSharedLibraryInfo = SharedLibraryInfo.CREATOR.createFromParcel(in);
        }
    }

    /**
     * Attaches information about the SDK like name, version and others which may be useful to
     * identify the SDK.
     *
     * <p>This is used by the system service to attach the library info to the {@link SandboxedSdk}
     * object return by the SDK after it has been loaded
     *
     * @param sharedLibraryInfo The SDK's library info. This contains the name, version and other
     *     details about the sdk.
     * @throws IllegalStateException if a base sharedLibraryInfo has already been set.
     * @hide
     */
    public void attachSharedLibraryInfo(@NonNull SharedLibraryInfo sharedLibraryInfo) {
        if (mSharedLibraryInfo != null) {
            throw new IllegalStateException("SharedLibraryInfo already set");
        }
        Objects.requireNonNull(sharedLibraryInfo, "SharedLibraryInfo cannot be null");
        mSharedLibraryInfo = sharedLibraryInfo;
    }

    /**
     * Returns the interface to the SDK that was loaded in response to {@link
     * SdkSandboxManager#loadSdk}. A {@code null} interface is returned if the Binder has since
     * become unavailable, in response to the SDK being unloaded.
     */
    public @Nullable IBinder getInterface() {
        // This will be null if the remote SDK has been unloaded and the IBinder originally provided
        // is now a dead object.
        return mInterface;
    }

    /**
     * Returns the {@link SharedLibraryInfo} for the SDK.
     *
     * @throws IllegalStateException if the system service has not yet attached {@link
     *     SharedLibraryInfo} to the {@link SandboxedSdk} object sent by the SDK.
     */
    public @NonNull SharedLibraryInfo getSharedLibraryInfo() {
        if (mSharedLibraryInfo == null) {
            throw new IllegalStateException(
                    "SharedLibraryInfo has not been set. This is populated by our system service "
                            + "once the SandboxedSdk is sent back from as a response to "
                            + "android.app.sdksandbox.SandboxedSdkProvider$onLoadSdk. Please use "
                            + "android.app.sdksandbox.SdkSandboxManager#getSandboxedSdks or "
                            + "android.app.sdksandbox.SdkSandboxController#getSandboxedSdks to "
                            + "get the correctly populated SandboxedSdks.");
        }
        return mSharedLibraryInfo;
    }

    /** {@inheritDoc} */
    @Override
    public int describeContents() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStrongBinder(mInterface);
        if (mSharedLibraryInfo != null) {
            dest.writeInt(1);
            mSharedLibraryInfo.writeToParcel(dest, 0);
        } else {
            dest.writeInt(0);
        }
    }
}
