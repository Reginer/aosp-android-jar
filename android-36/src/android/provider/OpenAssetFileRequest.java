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

package android.provider;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A request made from {@link MediaStore} to {@link com.android.providers.media.MediaProvider}
 * to open {@link android.content.res.AssetFileDescriptor}
 *
 * @hide
 */
public final class OpenAssetFileRequest implements Parcelable {

    private final Uri mUri;
    private final String mMimeType;
    private final Bundle mOpts;
    private final IOpenAssetFileCallback mCallback;
    private final IMPCancellationSignal mCancellationSignal;

    public OpenAssetFileRequest(@NonNull Uri uri, @NonNull String mimeType,
            @Nullable Bundle opts, @NonNull IOpenAssetFileCallback callback,
            @Nullable IMPCancellationSignal cancellationSignal) {
        mUri = uri;
        mMimeType = mimeType;
        mOpts = opts;
        mCallback = callback;
        mCancellationSignal = cancellationSignal;
    }

    @NonNull
    public Uri getUri() {
        return mUri;
    }

    @NonNull
    public String getMimeType() {
        return mMimeType;
    }

    @Nullable
    public Bundle getOpts() {
        return mOpts;
    }

    @NonNull
    public IOpenAssetFileCallback getCallback() {
        return mCallback;
    }

    @Nullable
    public IMPCancellationSignal getCancellationSignal() {
        return mCancellationSignal;
    }

    @SuppressLint("ParcelClassLoader")
    private OpenAssetFileRequest(Parcel p) {
        mUri = Uri.CREATOR.createFromParcel(p);
        mMimeType = p.readString();
        mOpts = p.readBundle();
        mCallback = IOpenAssetFileCallback.Stub.asInterface(p.readStrongBinder());
        mCancellationSignal = IMPCancellationSignal.Stub.asInterface(p.readStrongBinder());
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        mUri.writeToParcel(dest, flags);
        dest.writeString(mMimeType);
        dest.writeBundle(mOpts);
        dest.writeStrongBinder(mCallback.asBinder());
        dest.writeStrongBinder(mCancellationSignal != null ? mCancellationSignal.asBinder() : null);
    }

    @NonNull
    public static final Creator<OpenAssetFileRequest> CREATOR =
            new Creator<OpenAssetFileRequest>() {
                @Override
                public OpenAssetFileRequest createFromParcel(Parcel source) {
                    return new OpenAssetFileRequest(source);
                }

                @Override
                public OpenAssetFileRequest[] newArray(int size) {
                    return new OpenAssetFileRequest[size];
                }
            };
}
