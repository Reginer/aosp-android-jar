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
import android.annotation.NonNull;
import android.annotation.RequiresApi;
import android.net.Uri;
import android.os.Build;

import java.util.List;

/**
 * Callback to define mechanisms by which can apps can receive notifications about
 * different events from embedded photopicker for the corresponding Session
 * ({@link EmbeddedPhotoPickerSession}).
 *
 * <p> PhotoPicker will invoke the methods of this interface on the Executor provided by
 * the caller in {@link EmbeddedPhotoPickerProvider#openSession}
 *
 * <p> Any methods on a single instance of this object will always be invoked in a non-concurrent
 * or thread safe way. In other words, all methods are invoked in a serial execution manner
 * on the executor passed by the caller. Hence callers wouldn't need any buffer or locking
 * mechanism on their end.
 *
 * @see EmbeddedPhotoPickerProvider
 *
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@FlaggedApi("com.android.providers.media.flags.enable_embedded_photopicker")
public interface EmbeddedPhotoPickerClient {

    /**
     * Reports that session of app with photopicker was established successfully.
     * Also shares {@link EmbeddedPhotoPickerSession} handle containing the view
     * with the caller that should be used to notify the session of UI events.
     */
    void onSessionOpened(@NonNull EmbeddedPhotoPickerSession session);

    /**
     * Reports that terminal error has occurred in the session. Any further events
     * notified on this session will be ignored. The embedded photopicker view will be
     * torn down along with session upon error.
     */
    void onSessionError(@NonNull Throwable cause);

    /**
     * Reports that URI permission has been granted to the item selected by the user.
     *
     * <p> It is possible that the permission to the URI was revoked if the item was unselected
     * by user before the URI is actually accessed by the caller. Hence callers must
     * handle {@code SecurityException} when attempting to read or use the URI in
     * response to this callback.
     */
    void onUriPermissionGranted(@NonNull List<Uri> uris);

    /**
     * Reports that URI permission has been revoked of the item deselected by the
     * user.
     */
    void onUriPermissionRevoked(@NonNull List<Uri> uris);

    /**
     * Reports that the user is done with their selection and should collapse the picker.
     *
     * <p> This doesn't necessarily mean that the session should be closed, but rather the user
     * has indicated that they are done selecting images and should go back to the app. </p>
     */
    void onSelectionComplete();
}
