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

package android.widget.photopicker;

import android.annotation.RequiresApi;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.SurfaceControlViewHost;

import androidx.annotation.NonNull;

/**
 * Response for {@link EmbeddedPhotoPickerProvider#openSession} api for internal use
 *
 * <p> The service encapsulates the response containing {@link EmbeddedPhotoPickerSession}
 * and {@link SurfaceControlViewHost.SurfacePackage} and notifies it to the
 * {@link EmbeddedPhotoPickerClientWrapper#onSessionOpened}. The
 * {@link EmbeddedPhotoPickerClientWrapper} in turn notifies the
 * {@link EmbeddedPhotoPickerClient} delegate.
 *
 * @see EmbeddedPhotoPickerClientWrapper#onSessionOpened
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class EmbeddedPhotoPickerSessionResponse implements Parcelable {

    private IEmbeddedPhotoPickerSession mSession;
    private SurfaceControlViewHost.SurfacePackage mSurfacePackage;

    public EmbeddedPhotoPickerSessionResponse(@NonNull IEmbeddedPhotoPickerSession session,
            @NonNull SurfaceControlViewHost.SurfacePackage surfacePackage) {
        mSession = session;
        mSurfacePackage = surfacePackage;
    }

    @NonNull
    public IEmbeddedPhotoPickerSession getSession() {
        return mSession;
    }

    @NonNull
    public SurfaceControlViewHost.SurfacePackage getSurfacePackage() {
        return mSurfacePackage;
    }

    private EmbeddedPhotoPickerSessionResponse(Parcel in) {
        mSession = IEmbeddedPhotoPickerSession.Stub.asInterface(in.readStrongBinder());
        mSurfacePackage = in.readParcelable(
                SurfaceControlViewHost.SurfacePackage.class.getClassLoader(),
                SurfaceControlViewHost.SurfacePackage.class);
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongBinder(mSession.asBinder());
        dest.writeParcelable(mSurfacePackage, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<EmbeddedPhotoPickerSessionResponse> CREATOR =
            new Creator<EmbeddedPhotoPickerSessionResponse>() {
                @Override
                public EmbeddedPhotoPickerSessionResponse createFromParcel(Parcel in) {
                    return new EmbeddedPhotoPickerSessionResponse(in);
                }

                @Override
                public EmbeddedPhotoPickerSessionResponse[] newArray(int size) {
                    return new EmbeddedPhotoPickerSessionResponse[size];
                }
            };
}
