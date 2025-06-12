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
import android.net.Uri;
import android.os.Build;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Wrapper class to {@link EmbeddedPhotoPickerClient} for internal use that helps with IPC
 * between caller of {@link EmbeddedPhotoPickerProvider#openSession} api and service inside
 * PhotoPicker apk.
 *
 * <p> The caller implements {@link EmbeddedPhotoPickerClient} and passes it into
 * {@link EmbeddedPhotoPickerProvider#openSession} APIs that run on the caller's process.
 * The openSession api wraps Client object sent by caller around this class while doing
 * the actual IPC.
 *
 * <p> This wrapper class implements the internal {@link IEmbeddedPhotoPickerClient} interface to
 * convert incoming calls on to it from service back to call on the public
 * {@link EmbeddedPhotoPickerClient} interface to send it to callers.
 *
 * @see EmbeddedPhotoPickerClient
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class EmbeddedPhotoPickerClientWrapper extends IEmbeddedPhotoPickerClient.Stub {
    private final EmbeddedPhotoPickerProviderFactory mProvider;
    private final EmbeddedPhotoPickerClient mClientCallback;
    private final Executor mClientExecutor;

    EmbeddedPhotoPickerClientWrapper(
            EmbeddedPhotoPickerProviderFactory provider,
            EmbeddedPhotoPickerClient clientCallback,
            Executor clientExecutor) {
        this.mProvider = provider;
        this.mClientCallback = clientCallback;
        this.mClientExecutor = clientExecutor;
    }

    @Override
    public void onSessionOpened(EmbeddedPhotoPickerSessionResponse response) {
        final EmbeddedPhotoPickerSession session =
                new EmbeddedPhotoPickerSessionWrapper(mProvider, response, mClientCallback, this);
        mClientExecutor.execute(() -> mClientCallback.onSessionOpened(session));
    }

    @Override
    public void onSessionError(ParcelableException exception) {
        // Notify {@link EmbeddedPhotoPickerProviderFactory} that this client no longer exists.
        mProvider.onSessionClosed(mClientCallback);
        mClientExecutor.execute(() -> mClientCallback.onSessionError(exception.getCause()));
    }

    @Override
    public void onUriPermissionGranted(List<Uri> uris) {
        mClientExecutor.execute(() -> mClientCallback.onUriPermissionGranted(uris));
    }

    @Override
    public void onUriPermissionRevoked(List<Uri> uris) {
        mClientExecutor.execute(() -> mClientCallback.onUriPermissionRevoked(uris));
    }

    @Override
    public void onSelectionComplete() {
        mClientExecutor.execute(() -> mClientCallback.onSelectionComplete());
    }
}
