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
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.SurfaceControlViewHost;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Wrapper class to {@link EmbeddedPhotoPickerSession} for internal use that helps with IPC between
 * caller of {@link EmbeddedPhotoPickerProvider#openSession} api and service inside PhotoPicker apk.
 *
 * <p> This class implements the {@link EmbeddedPhotoPickerSession} interface to convert incoming
 * calls on to it from app and send it to the service. It uses {@link IEmbeddedPhotoPickerSession}
 * as the delegate
 *
 * @see EmbeddedPhotoPickerSession
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class EmbeddedPhotoPickerSessionWrapper implements EmbeddedPhotoPickerSession,
        IBinder.DeathRecipient {
    private final EmbeddedPhotoPickerProviderFactory mProvider;
    private final EmbeddedPhotoPickerSessionResponse mSessionResponse;
    private final IEmbeddedPhotoPickerSession mSession;
    private final EmbeddedPhotoPickerClient mClient;
    private final EmbeddedPhotoPickerClientWrapper mClientWrapper;

    EmbeddedPhotoPickerSessionWrapper(EmbeddedPhotoPickerProviderFactory provider,
            EmbeddedPhotoPickerSessionResponse mSessionResponse, EmbeddedPhotoPickerClient mClient,
            EmbeddedPhotoPickerClientWrapper clientWrapper) {
        this.mProvider = provider;
        this.mSessionResponse = mSessionResponse;
        this.mSession = mSessionResponse.getSession();
        this.mClient = mClient;
        this.mClientWrapper = clientWrapper;

        linkDeathRecipient();
    }

    private void linkDeathRecipient() {
        try {
            mSession.asBinder().linkToDeath(this, 0 /* flags*/);
        } catch (RemoteException e) {
            this.binderDied();
        }
    }

    @Override
    public SurfaceControlViewHost.SurfacePackage getSurfacePackage() {
        return mSessionResponse.getSurfacePackage();
    }

    @Override
    public void close() {
        mProvider.onSessionClosed(mClient);
        try {
            mSession.close();
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    public void notifyVisibilityChanged(boolean isVisible) {
        try {
            mSession.notifyVisibilityChanged(isVisible);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    @Override
    public void notifyResized(int width, int height) {
        try {
            mSession.notifyResized(width, height);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    @Override
    public void notifyConfigurationChanged(Configuration configuration) {
        try {
            mSession.notifyConfigurationChanged(configuration);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    @Override
    public void notifyPhotoPickerExpanded(boolean isExpanded) {
        try {
            mSession.notifyPhotopickerExpanded(isExpanded);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    @Override
    public void requestRevokeUriPermission(@NonNull List<Uri> uris) {
        try {
            mSession.requestRevokeUriPermission(uris);
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
    }

    @Override
    public void binderDied() {
        mClientWrapper.onSessionError(new ParcelableException(
                new RuntimeException("Binder object hosting this session has died. "
                        + "Clean up resources.")));
    }

}

