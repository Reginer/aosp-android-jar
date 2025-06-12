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
import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Class that holds the embedded photopicker view wrapped in
 * {@link SurfaceControlViewHost.SurfacePackage} that can be embedded by the caller in their
 * view hierarchy by placing it in a {@link SurfaceView} via its
 * {@link SurfaceView#setChildSurfacePackage} api.
 *
 * Callers of {@link EmbeddedPhotoPickerProvider#openSession} will asynchronously receive instance
 * of this class from the service upon its successful execution via
 * {@link EmbeddedPhotoPickerClient#onSessionOpened} callback.
 *
 * <p> Instance of this class can be then used by callers to notify PhotoPicker about
 * different events for service to act upon them.
 *
 * <p> When a session is no longer being used, it should be closed by callers to help system
 * release the resources.
 *
 * @see EmbeddedPhotoPickerProvider
 * @see EmbeddedPhotoPickerClient
 *
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@FlaggedApi("com.android.providers.media.flags.enable_embedded_photopicker")
@SuppressLint({"NotCloseable", "PackageLayering"})
public interface EmbeddedPhotoPickerSession {

    /**
     * Returns the {@link SurfaceControlViewHost.SurfacePackage} that contains view representing
     * embedded picker.
     *
     * <p> Callers can attach this view in their hierarchy using
     * {@link SurfaceView#setChildSurfacePackage} api.
     */
    @NonNull
    SurfaceControlViewHost.SurfacePackage getSurfacePackage();

    /**
     * Close the session, i.e. photopicker will release resources associated with this
     * session. Any further notifications to this Session will be ignored by the service.
     */
    void close();

    /**
     * Notify that embedded photopicker view is visible or not to the user.
     *
     * <p> This helps photopicker to close upstream work and manage the lifecycle
     * of this Session instance.
     *
     * @param isVisible True if view visible to the user, false if not.
     */
    void notifyVisibilityChanged(boolean isVisible);

    /**
     * Notify that caller's presentation area has changed and photopicker's dimensions
     * should change accordingly.
     *
     * @param width width of the view, in pixels
     * @param height height of the view, in pixels
     */
    void notifyResized(int width, int height);

    /**
     * Notifies photopicker that host side configuration has changed.
     *
     * @param configuration new configuration of caller
     */
    void notifyConfigurationChanged(@NonNull Configuration configuration);

    /**
     * Notify that user switched photopicker between expanded/collapsed state.
     *
     * <p> Some photopicker features (like Profile selector, Album grid etc.)
     * are only shown in full/expanded view and are hidden in collapsed view.
     *
     * @param isExpanded true if expanded, false if collapsed.
     */
    void notifyPhotoPickerExpanded(boolean isExpanded);

    /**
     * Notify that the user deselected some items.
     *
     * @param uris The {@link Uri} list of the deselected items.
     */
    void requestRevokeUriPermission(@NonNull List<Uri> uris);
}
