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

import android.annotation.FlaggedApi;
import android.annotation.RequiresApi;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.IBinder;
import android.view.AttachedSurfaceControl;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * This interface provides an api that callers can use to get a session of embedded PhotoPicker
 * ({@link EmbeddedPhotoPickerSession}).
 *
 * <p> Callers can get instance of this class using
 * {@link EmbeddedPhotoPickerProviderFactory#create(Context)}.
 *
 * <p> Under the hood, a service connection with photopicker is established by the implementation
 * of this api. To help establish this connection, a caller must include in their Manifest:
 * <pre>{@code
 * <queries>
 *   <intent>
 *       <action android:name="com.android.photopicker.core.embedded.EmbeddedService.BIND"/>
 *   </intent>
 * </queries>
 * }</pre>
 *
 * <p> When a session opens successfully, they would receive an instance of
 * {@link EmbeddedPhotoPickerSession} and {@link android.view.SurfaceControlViewHost.SurfacePackage}
 * via the {@link EmbeddedPhotoPickerClient#onSessionOpened api}
 *
 * <p> Callers pass an instance of {@link EmbeddedPhotoPickerClient} which is used by service to
 * notify about different events (like sessionError, uri granted/revoked etc) to them.
 * One-to-one relationship of client to session must be maintained by a caller i.e. they shouldn't
 * reuse same callback for more than one openSession requests.
 *
 * <p> The {@link EmbeddedPhotoPickerSession} instance can be used to notify photopicker about
 * different events (like resize, configChange etc).
 *
 * <p> This api is supported on api versions Android U+.
 *
 * @see EmbeddedPhotoPickerClient
 * @see EmbeddedPhotoPickerSession
 * @see EmbeddedPhotoPickerProviderFactory
 *
 * todo(b/358513325): Move this to new package when its ready
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@FlaggedApi("com.android.providers.media.flags.enable_embedded_photopicker")
public interface EmbeddedPhotoPickerProvider {

    /**
     * Open a new session for displaying content with an initial size of
     * width x height pixels. {@link EmbeddedPhotoPickerClient} will receive all incoming
     * communication from the PhotoPicker. All incoming calls to {@link EmbeddedPhotoPickerClient}
     * will be made through the provided {@code clientExecutor}
     *
     * @param hostToken Token used for constructing {@link android.view.SurfaceControlViewHost}.
     *                  Use {@link AttachedSurfaceControl#getInputTransferToken()} to
     *                  get token of attached
     *                  {@link android.view.SurfaceControlViewHost.SurfacePackage}.
     * @param displayId Application display id. Use
     *                  {@link DisplayManager#getDisplays()} to get the id.
     * @param width width of the view, in pixels.
     * @param height height of the view, in pixels.
     * @param featureInfo {@link EmbeddedPhotoPickerFeatureInfo} object containing all
     *                     the required features for the given session.
     * @param clientExecutor {@link Executor} to invoke callbacks.
     * @param callback {@link EmbeddedPhotoPickerClient} object to receive callbacks
     *                  from photopicker.
     */
    void openSession(
            @NonNull IBinder hostToken,
            int displayId,
            int width,
            int height,
            @NonNull EmbeddedPhotoPickerFeatureInfo featureInfo,
            @NonNull Executor clientExecutor,
            @NonNull EmbeddedPhotoPickerClient callback);
}

