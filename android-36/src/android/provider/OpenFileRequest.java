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
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A request made from {@link MediaStore} to {@link com.android.providers.media.MediaProvider}
 * to open {@link android.os.ParcelFileDescriptor}
 *
 * @hide
 */
public final class OpenFileRequest implements Parcelable {

    private final Uri mUri;
    private final IOpenFileCallback mCallback;
    private final IMPCancellationSignal mCancellationSignal;

    public OpenFileRequest(@NonNull Uri uri, @NonNull IOpenFileCallback callback,
            @Nullable IMPCancellationSignal cancellationSignal) {
        mUri = uri;
        mCallback = callback;
        mCancellationSignal = cancellationSignal;
    }

    private OpenFileRequest(Parcel p) {
        mUri = Uri.CREATOR.createFromParcel(p);
        mCallback = IOpenFileCallback.Stub.asInterface(p.readStrongBinder());
        mCancellationSignal = IMPCancellationSignal.Stub.asInterface(p.readStrongBinder());
    }

    @NonNull
    public Uri getUri() {
        return mUri;
    }

    @NonNull
    public IOpenFileCallback getCallback() {
        return mCallback;
    }

    @Nullable
    public IMPCancellationSignal getCancellationSignal() {
        return mCancellationSignal;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        mUri.writeToParcel(dest, flags);
        dest.writeStrongBinder(mCallback.asBinder());
        dest.writeStrongBinder(mCancellationSignal != null ? mCancellationSignal.asBinder() : null);
    }

    @NonNull
    public static final Creator<OpenFileRequest> CREATOR = new Creator<OpenFileRequest>() {
        @Override
        public OpenFileRequest createFromParcel(Parcel source) {
            return new OpenFileRequest(source);
        }

        @Override
        public OpenFileRequest[] newArray(int size) {
            return new OpenFileRequest[size];
        }
    };
}
