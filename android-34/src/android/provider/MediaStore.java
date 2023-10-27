/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.annotation.BytesLong;
import android.annotation.CurrentTimeMillisLong;
import android.annotation.CurrentTimeSecondsLong;
import android.annotation.DurationMillisLong;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.annotation.WorkerThread;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.ClipData;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.PostProcessor;
import android.media.ApplicationMediaCapabilities;
import android.media.ExifInterface;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.OperationCanceledException;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Size;

import androidx.annotation.RequiresApi;

import com.android.internal.annotations.VisibleForTesting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The contract between the media provider and applications. Contains
 * definitions for the supported URIs and columns.
 * <p>
 * The media provider provides an indexed collection of common media types, such
 * as {@link Audio}, {@link Video}, and {@link Images}, from any attached
 * storage devices. Each collection is organized based on the primary MIME type
 * of the underlying content; for example, {@code image/*} content is indexed
 * under {@link Images}. The {@link Files} collection provides a broad view
 * across all collections, and does not filter by MIME type.
 */
public final class MediaStore {
    private final static String TAG = "MediaStore";

    /** The authority for the media provider */
    public static final String AUTHORITY = "media";
    /** A content:// style uri to the authority for the media provider */
    public static final @NonNull Uri AUTHORITY_URI =
            Uri.parse("content://" + AUTHORITY);

    /**
     * The authority for a legacy instance of the media provider, before it was
     * converted into a Mainline module. When initializing for the first time,
     * the Mainline module will connect to this legacy instance to migrate
     * important user settings, such as {@link BaseColumns#_ID},
     * {@link MediaColumns#IS_FAVORITE}, and more.
     * <p>
     * The legacy instance is expected to meet the exact same API contract
     * expressed here in {@link MediaStore}, to facilitate smooth data
     * migrations. Interactions that would normally interact with
     * {@link #AUTHORITY} can be redirected to work with the legacy instance
     * using {@link #rewriteToLegacy(Uri)}.
     *
     * @hide
     */
    @SystemApi
    public static final String AUTHORITY_LEGACY = "media_legacy";
    /**
     * @see #AUTHORITY_LEGACY
     * @hide
     */
    @SystemApi
    public static final @NonNull Uri AUTHORITY_LEGACY_URI =
            Uri.parse("content://" + AUTHORITY_LEGACY);

    /**
     * Synthetic volume name that provides a view of all content across the
     * "internal" storage of the device.
     * <p>
     * This synthetic volume provides a merged view of all media distributed
     * with the device, such as built-in ringtones and wallpapers.
     * <p>
     * Because this is a synthetic volume, you can't insert new content into
     * this volume.
     */
    public static final String VOLUME_INTERNAL = "internal";

    /**
     * Synthetic volume name that provides a view of all content across the
     * "external" storage of the device.
     * <p>
     * This synthetic volume provides a merged view of all media across all
     * currently attached external storage devices.
     * <p>
     * Because this is a synthetic volume, you can't insert new content into
     * this volume. Instead, you can insert content into a specific storage
     * volume obtained from {@link #getExternalVolumeNames(Context)}.
     */
    public static final String VOLUME_EXTERNAL = "external";

    /**
     * Specific volume name that represents the primary external storage device
     * at {@link Environment#getExternalStorageDirectory()}.
     * <p>
     * This volume may not always be available, such as when the user has
     * ejected the device. You can find a list of all specific volume names
     * using {@link #getExternalVolumeNames(Context)}.
     */
    public static final String VOLUME_EXTERNAL_PRIMARY = "external_primary";

    /** {@hide} */
    public static final String VOLUME_DEMO = "demo";

    /** {@hide} */
    public static final String RESOLVE_PLAYLIST_MEMBERS_CALL = "resolve_playlist_members";
    /** {@hide} */
    public static final String RUN_IDLE_MAINTENANCE_CALL = "run_idle_maintenance";
    /** {@hide} */
    public static final String WAIT_FOR_IDLE_CALL = "wait_for_idle";
    /** {@hide} */
    public static final String SCAN_FILE_CALL = "scan_file";
    /** {@hide} */
    public static final String SCAN_VOLUME_CALL = "scan_volume";
    /** {@hide} */
    public static final String CREATE_WRITE_REQUEST_CALL = "create_write_request";
    /** {@hide} */
    public static final String CREATE_TRASH_REQUEST_CALL = "create_trash_request";
    /** {@hide} */
    public static final String CREATE_FAVORITE_REQUEST_CALL = "create_favorite_request";
    /** {@hide} */
    public static final String CREATE_DELETE_REQUEST_CALL = "create_delete_request";

    /** {@hide} */
    public static final String GET_VERSION_CALL = "get_version";
    /** {@hide} */
    public static final String GET_GENERATION_CALL = "get_generation";

    /** {@hide} */
    public static final String START_LEGACY_MIGRATION_CALL = "start_legacy_migration";
    /** {@hide} */
    public static final String FINISH_LEGACY_MIGRATION_CALL = "finish_legacy_migration";

    /** {@hide} */
    @Deprecated
    public static final String EXTERNAL_STORAGE_PROVIDER_AUTHORITY =
            "com.android.externalstorage.documents";

    /** {@hide} */
    public static final String GET_DOCUMENT_URI_CALL = "get_document_uri";
    /** {@hide} */
    public static final String GET_MEDIA_URI_CALL = "get_media_uri";

    /** {@hide} */
    public static final String GET_REDACTED_MEDIA_URI_CALL = "get_redacted_media_uri";
    /** {@hide} */
    public static final String GET_REDACTED_MEDIA_URI_LIST_CALL = "get_redacted_media_uri_list";
    /** {@hide} */
    public static final String EXTRA_URI_LIST = "uri_list";
    /** {@hide} */
    public static final String QUERY_ARG_REDACTED_URI = "android:query-arg-redacted-uri";

    /** {@hide} */
    public static final String EXTRA_URI = "uri";
    /** {@hide} */
    public static final String EXTRA_URI_PERMISSIONS = "uriPermissions";

    /** {@hide} */
    public static final String EXTRA_CLIP_DATA = "clip_data";
    /** {@hide} */
    public static final String EXTRA_CONTENT_VALUES = "content_values";
    /** {@hide} */
    public static final String EXTRA_RESULT = "result";
    /** {@hide} */
    public static final String EXTRA_FILE_DESCRIPTOR = "file_descriptor";
    /** {@hide} */
    public static final String EXTRA_LOCAL_PROVIDER = "local_provider";

    /** {@hide} */
    public static final String IS_SYSTEM_GALLERY_CALL = "is_system_gallery";
    /** {@hide} */
    public static final String EXTRA_IS_SYSTEM_GALLERY_UID = "is_system_gallery_uid";
    /** {@hide} */
    public static final String EXTRA_IS_SYSTEM_GALLERY_RESPONSE = "is_system_gallery_response";

    /** {@hide} */
    public static final String IS_CURRENT_CLOUD_PROVIDER_CALL = "is_current_cloud_provider";
    /** {@hide} */
    public static final String IS_SUPPORTED_CLOUD_PROVIDER_CALL = "is_supported_cloud_provider";
    /** {@hide} */
    public static final String NOTIFY_CLOUD_MEDIA_CHANGED_EVENT_CALL =
            "notify_cloud_media_changed_event";
    /** {@hide} */
    public static final String SYNC_PROVIDERS_CALL = "sync_providers";
    /** {@hide} */
    public static final String GET_CLOUD_PROVIDER_CALL = "get_cloud_provider";
    /** {@hide} */
    public static final String GET_CLOUD_PROVIDER_RESULT = "get_cloud_provider_result";
    /** {@hide} */
    public static final String SET_CLOUD_PROVIDER_CALL = "set_cloud_provider";
    /** {@hide} */
    public static final String EXTRA_CLOUD_PROVIDER = "cloud_provider";
    /** {@hide} */
    public static final String EXTRA_CLOUD_PROVIDER_RESULT = "cloud_provider_result";
    /** {@hide} */
    public static final String CREATE_SURFACE_CONTROLLER = "create_surface_controller";

    /** @hide */
    public static final String GRANT_MEDIA_READ_FOR_PACKAGE_CALL =
            "grant_media_read_for_package";

    /** {@hide} */
    public static final String USES_FUSE_PASSTHROUGH = "uses_fuse_passthrough";
    /** {@hide} */
    public static final String USES_FUSE_PASSTHROUGH_RESULT = "uses_fuse_passthrough_result";

    /**
     * Only used for testing.
     * {@hide}
     */
    @VisibleForTesting
    public static final String RUN_IDLE_MAINTENANCE_FOR_STABLE_URIS =
            "idle_maintenance_for_stable_uris";

    /**
     * Only used for testing.
     * {@hide}
     */
    @VisibleForTesting
    public static final String READ_BACKED_UP_FILE_PATHS = "read_backed_up_file_paths";

    /**
     * Only used for testing.
     * {@hide}
     */
    @VisibleForTesting
    public static final String GET_BACKUP_FILES = "get_backup_files";

    /**
     * Only used for testing.
     * {@hide}
     */
    @VisibleForTesting
    public static final String DELETE_BACKED_UP_FILE_PATHS = "delete_backed_up_file_paths";

    /** {@hide} */
    public static final String QUERY_ARG_MIME_TYPE = "android:query-arg-mime_type";
    /** {@hide} */
    public static final String QUERY_ARG_SIZE_BYTES = "android:query-arg-size_bytes";
    /** {@hide} */
    public static final String QUERY_ARG_ALBUM_ID = "android:query-arg-album_id";
    /** {@hide} */
    public static final String QUERY_ARG_ALBUM_AUTHORITY = "android:query-arg-album_authority";

    /**
     * This is for internal use by the media scanner only.
     * Name of the (optional) Uri parameter that determines whether to skip deleting
     * the file pointed to by the _data column, when deleting the database entry.
     * The only appropriate value for this parameter is "false", in which case the
     * delete will be skipped. Note especially that setting this to true, or omitting
     * the parameter altogether, will perform the default action, which is different
     * for different types of media.
     * @hide
     */
    public static final String PARAM_DELETE_DATA = "deletedata";

    /** {@hide} */
    public static final String PARAM_INCLUDE_PENDING = "includePending";
    /** {@hide} */
    public static final String PARAM_PROGRESS = "progress";
    /** {@hide} */
    public static final String PARAM_REQUIRE_ORIGINAL = "requireOriginal";
    /** {@hide} */
    public static final String PARAM_LIMIT = "limit";

    /** {@hide} */
    public static final int MY_USER_ID = UserHandle.myUserId();
    /** {@hide} */
    public static final int MY_UID = android.os.Process.myUid();
    // Stolen from: UserHandle#getUserId
    /** {@hide} */
    public static final int PER_USER_RANGE = 100000;

    private static final int PICK_IMAGES_MAX_LIMIT = 100;

    /**
     * Activity Action: Launch a music player.
     * The activity should be able to play, browse, or manipulate music files stored on the device.
     *
     * @deprecated Use {@link android.content.Intent#CATEGORY_APP_MUSIC} instead.
     */
    @Deprecated
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String INTENT_ACTION_MUSIC_PLAYER = "android.intent.action.MUSIC_PLAYER";

    /**
     * Activity Action: Perform a search for media.
     * Contains at least the {@link android.app.SearchManager#QUERY} extra.
     * May also contain any combination of the following extras:
     * EXTRA_MEDIA_ARTIST, EXTRA_MEDIA_ALBUM, EXTRA_MEDIA_TITLE, EXTRA_MEDIA_FOCUS
     *
     * @see android.provider.MediaStore#EXTRA_MEDIA_ARTIST
     * @see android.provider.MediaStore#EXTRA_MEDIA_ALBUM
     * @see android.provider.MediaStore#EXTRA_MEDIA_TITLE
     * @see android.provider.MediaStore#EXTRA_MEDIA_FOCUS
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String INTENT_ACTION_MEDIA_SEARCH = "android.intent.action.MEDIA_SEARCH";

    /**
     * An intent to perform a search for music media and automatically play content from the
     * result when possible. This can be fired, for example, by the result of a voice recognition
     * command to listen to music.
     * <p>This intent always includes the {@link android.provider.MediaStore#EXTRA_MEDIA_FOCUS}
     * and {@link android.app.SearchManager#QUERY} extras. The
     * {@link android.provider.MediaStore#EXTRA_MEDIA_FOCUS} extra determines the search mode, and
     * the value of the {@link android.app.SearchManager#QUERY} extra depends on the search mode.
     * For more information about the search modes for this intent, see
     * <a href="{@docRoot}guide/components/intents-common.html#PlaySearch">Play music based
     * on a search query</a> in <a href="{@docRoot}guide/components/intents-common.html">Common
     * Intents</a>.</p>
     *
     * <p>This intent makes the most sense for apps that can support large-scale search of music,
     * such as services connected to an online database of music which can be streamed and played
     * on the device.</p>
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH =
            "android.media.action.MEDIA_PLAY_FROM_SEARCH";

    /**
     * An intent to perform a search for readable media and automatically play content from the
     * result when possible. This can be fired, for example, by the result of a voice recognition
     * command to read a book or magazine.
     * <p>
     * Contains the {@link android.app.SearchManager#QUERY} extra, which is a string that can
     * contain any type of unstructured text search, like the name of a book or magazine, an author
     * a genre, a publisher, or any combination of these.
     * <p>
     * Because this intent includes an open-ended unstructured search string, it makes the most
     * sense for apps that can support large-scale search of text media, such as services connected
     * to an online database of books and/or magazines which can be read on the device.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String INTENT_ACTION_TEXT_OPEN_FROM_SEARCH =
            "android.media.action.TEXT_OPEN_FROM_SEARCH";

    /**
     * An intent to perform a search for video media and automatically play content from the
     * result when possible. This can be fired, for example, by the result of a voice recognition
     * command to play movies.
     * <p>
     * Contains the {@link android.app.SearchManager#QUERY} extra, which is a string that can
     * contain any type of unstructured video search, like the name of a movie, one or more actors,
     * a genre, or any combination of these.
     * <p>
     * Because this intent includes an open-ended unstructured search string, it makes the most
     * sense for apps that can support large-scale search of video, such as services connected to an
     * online database of videos which can be streamed and played on the device.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String INTENT_ACTION_VIDEO_PLAY_FROM_SEARCH =
            "android.media.action.VIDEO_PLAY_FROM_SEARCH";

    /**
     * The name of the Intent-extra used to define the artist
     */
    public static final String EXTRA_MEDIA_ARTIST = "android.intent.extra.artist";
    /**
     * The name of the Intent-extra used to define the album
     */
    public static final String EXTRA_MEDIA_ALBUM = "android.intent.extra.album";
    /**
     * The name of the Intent-extra used to define the song title
     */
    public static final String EXTRA_MEDIA_TITLE = "android.intent.extra.title";
    /**
     * The name of the Intent-extra used to define the genre.
     */
    public static final String EXTRA_MEDIA_GENRE = "android.intent.extra.genre";
    /**
     * The name of the Intent-extra used to define the playlist.
     *
     * @deprecated Android playlists are now deprecated. We will keep the current
     *             functionality for compatibility resons, but we will no longer take feature
     *             request. We do not advise adding new usages of Android Playlists. M3U files can
     *             be used as an alternative.
     */
    @Deprecated
    public static final String EXTRA_MEDIA_PLAYLIST = "android.intent.extra.playlist";
    /**
     * The name of the Intent-extra used to define the radio channel.
     */
    public static final String EXTRA_MEDIA_RADIO_CHANNEL = "android.intent.extra.radio_channel";
    /**
     * The name of the Intent-extra used to define the search focus. The search focus
     * indicates whether the search should be for things related to the artist, album
     * or song that is identified by the other extras.
     */
    public static final String EXTRA_MEDIA_FOCUS = "android.intent.extra.focus";

    /**
     * The name of the Intent-extra used to control the orientation of a ViewImage or a MovieView.
     * This is an int property that overrides the activity's requestedOrientation.
     * @see android.content.pm.ActivityInfo#SCREEN_ORIENTATION_UNSPECIFIED
     */
    public static final String EXTRA_SCREEN_ORIENTATION = "android.intent.extra.screenOrientation";

    /**
     * The name of an Intent-extra used to control the UI of a ViewImage.
     * This is a boolean property that overrides the activity's default fullscreen state.
     */
    public static final String EXTRA_FULL_SCREEN = "android.intent.extra.fullScreen";

    /**
     * The name of an Intent-extra used to control the UI of a ViewImage.
     * This is a boolean property that specifies whether or not to show action icons.
     */
    public static final String EXTRA_SHOW_ACTION_ICONS = "android.intent.extra.showActionIcons";

    /**
     * The name of the Intent-extra used to control the onCompletion behavior of a MovieView.
     * This is a boolean property that specifies whether or not to finish the MovieView activity
     * when the movie completes playing. The default value is true, which means to automatically
     * exit the movie player activity when the movie completes playing.
     */
    public static final String EXTRA_FINISH_ON_COMPLETION = "android.intent.extra.finishOnCompletion";

    /**
     * The name of the Intent action used to launch a camera in still image mode.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String INTENT_ACTION_STILL_IMAGE_CAMERA = "android.media.action.STILL_IMAGE_CAMERA";

    /**
     * Name under which an activity handling {@link #INTENT_ACTION_STILL_IMAGE_CAMERA} or
     * {@link #INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE} publishes the service name for its prewarm
     * service.
     * <p>
     * This meta-data should reference the fully qualified class name of the prewarm service
     * extending {@code CameraPrewarmService}.
     * <p>
     * The prewarm service will get bound and receive a prewarm signal
     * {@code CameraPrewarmService#onPrewarm()} when a camera launch intent fire might be imminent.
     * An application implementing a prewarm service should do the absolute minimum amount of work
     * to initialize the camera in order to reduce startup time in likely case that shortly after a
     * camera launch intent would be sent.
     */
    public static final String META_DATA_STILL_IMAGE_CAMERA_PREWARM_SERVICE =
            "android.media.still_image_camera_preview_service";

    /**
     * Name under which an activity handling {@link #ACTION_REVIEW} or
     * {@link #ACTION_REVIEW_SECURE} publishes the service name for its prewarm
     * service.
     * <p>
     * This meta-data should reference the fully qualified class name of the prewarm service
     * <p>
     * The prewarm service can be bound before starting {@link #ACTION_REVIEW} or
     * {@link #ACTION_REVIEW_SECURE}.
     * An application implementing this prewarm service should do the absolute minimum amount of
     * work to initialize its resources to efficiently handle an {@link #ACTION_REVIEW} or
     * {@link #ACTION_REVIEW_SECURE} in the near future.
     */
    public static final java.lang.String META_DATA_REVIEW_GALLERY_PREWARM_SERVICE =
            "android.media.review_gallery_prewarm_service";

    /**
     * The name of the Intent action used to launch a camera in still image mode
     * for use when the device is secured (e.g. with a pin, password, pattern,
     * or face unlock). Applications responding to this intent must not expose
     * any personal content like existing photos or videos on the device. The
     * applications should be careful not to share any photo or video with other
     * applications or internet. The activity should use {@link
     * Activity#setShowWhenLocked} to display
     * on top of the lock screen while secured. There is no activity stack when
     * this flag is used, so launching more than one activity is strongly
     * discouraged.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE =
            "android.media.action.STILL_IMAGE_CAMERA_SECURE";

    /**
     * The name of the Intent action used to launch a camera in video mode.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String INTENT_ACTION_VIDEO_CAMERA = "android.media.action.VIDEO_CAMERA";

    /**
     * Standard Intent action that can be sent to have the camera application
     * capture an image and return it.
     * <p>
     * The caller may pass an extra EXTRA_OUTPUT to control where this image will be written.
     * If the EXTRA_OUTPUT is not present, then a small sized image is returned as a Bitmap
     * object in the extra field. This is useful for applications that only need a small image.
     * If the EXTRA_OUTPUT is present, then the full-sized image will be written to the Uri
     * value of EXTRA_OUTPUT.
     * As of {@link android.os.Build.VERSION_CODES#LOLLIPOP}, this uri can also be supplied through
     * {@link android.content.Intent#setClipData(ClipData)}. If using this approach, you still must
     * supply the uri through the EXTRA_OUTPUT field for compatibility with old applications.
     * If you don't set a ClipData, it will be copied there for you when calling
     * {@link Context#startActivity(Intent)}.
     * <p>
     * Regardless of whether or not EXTRA_OUTPUT is present, when an image is captured via this
     * intent, {@link android.hardware.Camera#ACTION_NEW_PICTURE} won't be broadcasted.
     * <p>
     * Note: if you app targets {@link android.os.Build.VERSION_CODES#M M} and above
     * and declares as using the {@link android.Manifest.permission#CAMERA} permission which
     * is not granted, then attempting to use this action will result in a {@link
     * java.lang.SecurityException}.
     *
     *  @see #EXTRA_OUTPUT
     *  @see android.hardware.Camera#ACTION_NEW_PICTURE
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public final static String ACTION_IMAGE_CAPTURE = "android.media.action.IMAGE_CAPTURE";

    /**
     * Intent action that can be sent to have the camera application capture an image and return
     * it when the device is secured (e.g. with a pin, password, pattern, or face unlock).
     * Applications responding to this intent must not expose any personal content like existing
     * photos or videos on the device. The applications should be careful not to share any photo
     * or video with other applications or Internet. The activity should use {@link
     * Activity#setShowWhenLocked} to display on top of the
     * lock screen while secured. There is no activity stack when this flag is used, so
     * launching more than one activity is strongly discouraged.
     * <p>
     * The caller may pass an extra EXTRA_OUTPUT to control where this image will be written.
     * If the EXTRA_OUTPUT is not present, then a small sized image is returned as a Bitmap
     * object in the extra field. This is useful for applications that only need a small image.
     * If the EXTRA_OUTPUT is present, then the full-sized image will be written to the Uri
     * value of EXTRA_OUTPUT.
     * As of {@link android.os.Build.VERSION_CODES#LOLLIPOP}, this uri can also be supplied through
     * {@link android.content.Intent#setClipData(ClipData)}. If using this approach, you still must
     * supply the uri through the EXTRA_OUTPUT field for compatibility with old applications.
     * If you don't set a ClipData, it will be copied there for you when calling
     * {@link Context#startActivity(Intent)}.
     * <p>
     * Regardless of whether or not EXTRA_OUTPUT is present, when an image is captured via this
     * intent, {@link android.hardware.Camera#ACTION_NEW_PICTURE} won't be broadcasted.
     *
     * @see #ACTION_IMAGE_CAPTURE
     * @see #EXTRA_OUTPUT
     * @see android.hardware.Camera#ACTION_NEW_PICTURE
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_IMAGE_CAPTURE_SECURE =
            "android.media.action.IMAGE_CAPTURE_SECURE";

    /**
     * Standard Intent action that can be sent to have the camera application
     * capture a video and return it.
     * <p>
     * The caller may pass in an extra EXTRA_VIDEO_QUALITY to control the video quality.
     * <p>
     * The caller may pass in an extra EXTRA_OUTPUT to control
     * where the video is written.
     * <ul>
     * <li>If EXTRA_OUTPUT is not present, the video will be written to the standard location
     * for videos, and the Uri of that location will be returned in the data field of the Uri.
     * {@link android.hardware.Camera#ACTION_NEW_VIDEO} will also be broadcasted when the video
     * is recorded.
     * <li>If EXTRA_OUTPUT is assigned a Uri value, no
     * {@link android.hardware.Camera#ACTION_NEW_VIDEO} will be broadcasted. As of
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP}, this uri can also be
     * supplied through {@link android.content.Intent#setClipData(ClipData)}.  If using this
     * approach, you still must supply the uri through the EXTRA_OUTPUT field for compatibility
     * with old applications. If you don't set a ClipData, it will be copied there for you when
     * calling {@link Context#startActivity(Intent)}.
     * </ul>
     *
     * <p>Note: if you app targets {@link android.os.Build.VERSION_CODES#M M} and above
     * and declares as using the {@link android.Manifest.permission#CAMERA} permission which
     * is not granted, then atempting to use this action will result in a {@link
     * java.lang.SecurityException}.
     *
     * @see #EXTRA_OUTPUT
     * @see #EXTRA_VIDEO_QUALITY
     * @see #EXTRA_SIZE_LIMIT
     * @see #EXTRA_DURATION_LIMIT
     * @see android.hardware.Camera#ACTION_NEW_VIDEO
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public final static String ACTION_VIDEO_CAPTURE = "android.media.action.VIDEO_CAPTURE";

    /**
     * Standard action that can be sent to review the given media file.
     * <p>
     * The launched application is expected to provide a large-scale view of the
     * given media file, while allowing the user to quickly access other
     * recently captured media files.
     * <p>
     * Input: {@link Intent#getData} is URI of the primary media item to
     * initially display.
     *
     * @see #ACTION_REVIEW_SECURE
     * @see #EXTRA_BRIGHTNESS
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public final static String ACTION_REVIEW = "android.provider.action.REVIEW";

    /**
     * Standard action that can be sent to review the given media file when the
     * device is secured (e.g. with a pin, password, pattern, or face unlock).
     * The applications should be careful not to share any media with other
     * applications or Internet. The activity should use
     * {@link Activity#setShowWhenLocked} to display on top of the lock screen
     * while secured. There is no activity stack when this flag is used, so
     * launching more than one activity is strongly discouraged.
     * <p>
     * The launched application is expected to provide a large-scale view of the
     * given primary media file, while only allowing the user to quickly access
     * other media from an explicit secondary list.
     * <p>
     * Input: {@link Intent#getData} is URI of the primary media item to
     * initially display. {@link Intent#getClipData} is the limited list of
     * secondary media items that the user is allowed to review. If
     * {@link Intent#getClipData} is undefined, then no other media access
     * should be allowed.
     *
     * @see #EXTRA_BRIGHTNESS
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public final static String ACTION_REVIEW_SECURE = "android.provider.action.REVIEW_SECURE";

    /**
     * When defined, the launched application is requested to set the given
     * brightness value via
     * {@link android.view.WindowManager.LayoutParams#screenBrightness} to help
     * ensure a smooth transition when launching {@link #ACTION_REVIEW} or
     * {@link #ACTION_REVIEW_SECURE} intents.
     */
    public final static String EXTRA_BRIGHTNESS = "android.provider.extra.BRIGHTNESS";

    /**
     * The name of the Intent-extra used to control the quality of a recorded video. This is an
     * integer property. Currently value 0 means low quality, suitable for MMS messages, and
     * value 1 means high quality. In the future other quality levels may be added.
     */
    public final static String EXTRA_VIDEO_QUALITY = "android.intent.extra.videoQuality";

    /**
     * Specify the maximum allowed size.
     */
    public final static String EXTRA_SIZE_LIMIT = "android.intent.extra.sizeLimit";

    /**
     * Specify the maximum allowed recording duration in seconds.
     */
    public final static String EXTRA_DURATION_LIMIT = "android.intent.extra.durationLimit";

    /**
     * The name of the Intent-extra used to indicate a content resolver Uri to be used to
     * store the requested image or video.
     */
    public final static String EXTRA_OUTPUT = "output";

    /**
     * Activity Action: Allow the user to select images or videos provided by
     * system and return it. This is different than {@link Intent#ACTION_PICK}
     * and {@link Intent#ACTION_GET_CONTENT} in that
     * <ul>
     * <li> the data for this action is provided by the system
     * <li> this action is only used for picking images and videos
     * <li> caller gets read access to user picked items even without storage
     * permissions
     * </ul>
     * <p>
     * Callers can optionally specify MIME type (such as {@code image/*} or
     * {@code video/*}), resulting in a range of content selection that the
     * caller is interested in. The optional MIME type can be requested with
     * {@link Intent#setType(String)}.
     * <p>
     * If the caller needs multiple returned items (or caller wants to allow
     * multiple selection), then it can specify
     * {@link MediaStore#EXTRA_PICK_IMAGES_MAX} to indicate this.
     * <p>
     * When the caller requests multiple selection, the value of
     * {@link MediaStore#EXTRA_PICK_IMAGES_MAX} must be a positive integer
     * greater than 1 and less than or equal to
     * {@link MediaStore#getPickImagesMaxLimit}, otherwise
     * {@link Activity#RESULT_CANCELED} is returned.
     * <p>
     * Callers may use {@link Intent#EXTRA_LOCAL_ONLY} to limit content
     * selection to local data.
     * <p>
     * Output: MediaStore content URI(s) of the item(s) that was picked.
     * Unlike other MediaStore URIs, these are referred to as 'picker' URIs and
     * expose a limited set of read-only operations. Specifically, picker URIs
     * can only be opened for read and queried for columns in {@link PickerMediaColumns}.
     * <p>
     * Before this API, apps could use {@link Intent#ACTION_GET_CONTENT}. However,
     * {@link #ACTION_PICK_IMAGES} is now the recommended option for images and videos,
     * since it offers a better user experience.
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_PICK_IMAGES = "android.provider.action.PICK_IMAGES";

    /**
     * Activity Action: This is a system action for when users choose to select media to share with
     * an app rather than granting allow all visual media.
     *
     * <p>
     * Callers must specify the intent-extra integer
     * {@link Intent#EXTRA_UID} with the uid of the app that
     * will receive the MediaProvider grants for the selected files.
     * <p>
     * Callers can optionally specify MIME type (such as {@code image/*} or {@code video/*}),
     * resulting in a range of content selection that the caller is interested in. The optional MIME
     * type can be requested with {@link Intent#setType(String)}.
     * <p>
     * This action does not alter any permission state for the app, and does not check any
     * permission state for the app in the underlying media provider file access grants.
     *
     * <p>If images/videos were successfully picked this will return {@link Activity#RESULT_OK}
     * otherwise {@link Activity#RESULT_CANCELED} is returned.
     *
     * <p><strong>NOTE:</strong> You should probably not use this. This action requires the {@link
     * Manifest.permission#GRANT_RUNTIME_PERMISSIONS } permission.
     *
     * @hide
     */
    @SystemApi
    public static final String ACTION_USER_SELECT_IMAGES_FOR_APP =
            "android.provider.action.USER_SELECT_IMAGES_FOR_APP";

    /**
     * Activity Action: Launch settings controlling images or videos selection with
     * {@link #ACTION_PICK_IMAGES}.
     *
     * The settings page allows a user to change the enabled {@link CloudMediaProvider} on the
     * device and other media selection configurations.
     *
     * @see #ACTION_PICK_IMAGES
     * @see #isCurrentCloudMediaProviderAuthority(ContentResolver, String)
     */
    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_PICK_IMAGES_SETTINGS =
            "android.provider.action.PICK_IMAGES_SETTINGS";

    /**
     * The name of an optional intent-extra used to allow multiple selection of
     * items and constrain maximum number of items that can be returned by
     * {@link MediaStore#ACTION_PICK_IMAGES}, action may still return nothing
     * (0 items) if the user chooses to cancel.
     * <p>
     * The value of this intent-extra should be a positive integer greater
     * than 1 and less than or equal to
     * {@link MediaStore#getPickImagesMaxLimit}, otherwise
     * {@link Activity#RESULT_CANCELED} is returned.
     */
    public final static String EXTRA_PICK_IMAGES_MAX = "android.provider.extra.PICK_IMAGES_MAX";

    /**
     * The maximum limit for the number of items that can be selected using
     * {@link MediaStore#ACTION_PICK_IMAGES} when launched in multiple selection mode.
     * This can be used as a constant value for {@link MediaStore#EXTRA_PICK_IMAGES_MAX}.
     */
    public static int getPickImagesMaxLimit() {
        return PICK_IMAGES_MAX_LIMIT;
    }

    /**
     * Specify that the caller wants to receive the original media format without transcoding.
     *
     * <b>Caution: using this flag can cause app
     * compatibility issues whenever Android adds support for new media formats.</b>
     * Clients should instead specify their supported media capabilities explicitly
     * in their manifest or with the {@link #EXTRA_MEDIA_CAPABILITIES} {@code open} flag.
     *
     * This option is useful for apps that don't attempt to parse the actual byte contents of media
     * files, such as playback using {@link MediaPlayer} or for off-device backup. Note that the
     * {@link android.Manifest.permission#ACCESS_MEDIA_LOCATION} permission will still be required
     * to avoid sensitive metadata redaction, similar to {@link #setRequireOriginal(Uri)}.
     * </ul>
     *
     * Note that this flag overrides any explicitly declared {@code media_capabilities.xml} or
     * {@link ApplicationMediaCapabilities} extras specified in the same {@code open} request.
     *
     * <p>This option can be added to the {@code opts} {@link Bundle} in various
     * {@link ContentResolver} {@code open} methods.
     *
     * @see ContentResolver#openTypedAssetFileDescriptor(Uri, String, Bundle)
     * @see ContentResolver#openTypedAssetFile(Uri, String, Bundle, CancellationSignal)
     * @see #setRequireOriginal(Uri)
     * @see MediaStore#getOriginalMediaFormatFileDescriptor(Context, ParcelFileDescriptor)
     */
    public final static String EXTRA_ACCEPT_ORIGINAL_MEDIA_FORMAT =
            "android.provider.extra.ACCEPT_ORIGINAL_MEDIA_FORMAT";

    /**
     * Specify the {@link ApplicationMediaCapabilities} that should be used while opening a media.
     *
     * If the capabilities specified matches the format of the original file, the app will receive
     * the original file, otherwise, it will get transcoded to a default supported format.
     *
     * This flag takes higher precedence over the applications declared
     * {@code media_capabilities.xml} and is useful for apps that want to have more granular control
     * over their supported media capabilities.
     *
     * <p>This option can be added to the {@code opts} {@link Bundle} in various
     * {@link ContentResolver} {@code open} methods.
     *
     * @see ContentResolver#openTypedAssetFileDescriptor(Uri, String, Bundle)
     * @see ContentResolver#openTypedAssetFile(Uri, String, Bundle, CancellationSignal)
     */
    public final static String EXTRA_MEDIA_CAPABILITIES =
            "android.provider.extra.MEDIA_CAPABILITIES";

    /**
     * Specify the UID of the app that should be used to determine supported media capabilities
     * while opening a media.
     *
     * If this specified UID is found to be capable of handling the original media file format, the
     * app will receive the original file, otherwise, the file will get transcoded to a default
     * format supported by the specified UID.
     */
    public static final String EXTRA_MEDIA_CAPABILITIES_UID =
            "android.provider.extra.MEDIA_CAPABILITIES_UID";

    /**
     * Flag used to set file mode in bundle for opening a document.
     *
     * @hide
     */
    public static final String EXTRA_MODE = "android.provider.extra.MODE";

    /**
      * The string that is used when a media attribute is not known. For example,
      * if an audio file does not have any meta data, the artist and album columns
      * will be set to this value.
      */
    public static final String UNKNOWN_STRING = "<unknown>";

    /**
     * Specify a {@link Uri} that is "related" to the current operation being
     * performed.
     * <p>
     * This is typically used to allow an operation that may normally be
     * rejected, such as making a copy of a pre-existing image located under a
     * {@link MediaColumns#RELATIVE_PATH} where new images are not allowed.
     * <p>
     * It's strongly recommended that when making a copy of pre-existing content
     * that you define the "original document ID" GUID as defined by the <em>XMP
     * Media Management</em> standard.
     * <p>
     * This key can be placed in a {@link Bundle} of extras and passed to
     * {@link ContentResolver#insert}.
     */
    public static final String QUERY_ARG_RELATED_URI = "android:query-arg-related-uri";

    /**
     * Flag that can be used to enable movement of media items on disk through
     * {@link ContentResolver#update} calls. This is typically true for
     * third-party apps, but false for system components.
     *
     * @hide
     */
    public static final String QUERY_ARG_ALLOW_MOVEMENT = "android:query-arg-allow-movement";

    /**
     * Flag that indicates that a media scan that was triggered as part of
     * {@link ContentResolver#update} should be asynchronous. This flag should
     * only be used when {@link ContentResolver#update} operation needs to
     * return early without updating metadata for the file. This may make other
     * apps see incomplete metadata for the updated file as scan runs
     * asynchronously here.
     * Note that when this flag is set, the published file will not appear in
     * default query until the deferred scan is complete.
     * Most apps shouldn't set this flag.
     *
     * @hide
     */
    @SystemApi
    public static final String QUERY_ARG_DEFER_SCAN = "android:query-arg-defer-scan";

    /**
     * Flag that requests {@link ContentResolver#query} to include content from
     * recently unmounted volumes.
     * <p>
     * When the flag is set, {@link ContentResolver#query} will return content
     * from all volumes(i.e., both mounted and recently unmounted volume whose
     * content is still held by MediaProvider).
     * <p>
     * Note that the query result doesn't provide any hint for content from
     * unmounted volume. It's strongly recommended to use default query to
     * avoid accessing/operating on the content that are not available on the
     * device.
     * <p>
     * The flag is useful for apps which manage their own database and
     * query MediaStore in order to synchronize between MediaStore database
     * and their own database.
     */
    public static final String QUERY_ARG_INCLUDE_RECENTLY_UNMOUNTED_VOLUMES =
            "android:query-arg-recently-unmounted-volumes";

    /**
     * Specify how {@link MediaColumns#IS_PENDING} items should be filtered when
     * performing a {@link MediaStore} operation.
     * <p>
     * This key can be placed in a {@link Bundle} of extras and passed to
     * {@link ContentResolver#query}, {@link ContentResolver#update}, or
     * {@link ContentResolver#delete}.
     * <p>
     * By default, pending items are filtered away from operations.
     */
    @Match
    public static final String QUERY_ARG_MATCH_PENDING = "android:query-arg-match-pending";

    /**
     * Specify how {@link MediaColumns#IS_TRASHED} items should be filtered when
     * performing a {@link MediaStore} operation.
     * <p>
     * This key can be placed in a {@link Bundle} of extras and passed to
     * {@link ContentResolver#query}, {@link ContentResolver#update}, or
     * {@link ContentResolver#delete}.
     * <p>
     * By default, trashed items are filtered away from operations.
     *
     * @see MediaColumns#IS_TRASHED
     * @see MediaStore#QUERY_ARG_MATCH_TRASHED
     * @see MediaStore#createTrashRequest
     */
    @Match
    public static final String QUERY_ARG_MATCH_TRASHED = "android:query-arg-match-trashed";

    /**
     * Specify how {@link MediaColumns#IS_FAVORITE} items should be filtered
     * when performing a {@link MediaStore} operation.
     * <p>
     * This key can be placed in a {@link Bundle} of extras and passed to
     * {@link ContentResolver#query}, {@link ContentResolver#update}, or
     * {@link ContentResolver#delete}.
     * <p>
     * By default, favorite items are <em>not</em> filtered away from
     * operations.
     *
     * @see MediaColumns#IS_FAVORITE
     * @see MediaStore#QUERY_ARG_MATCH_FAVORITE
     * @see MediaStore#createFavoriteRequest
     */
    @Match
    public static final String QUERY_ARG_MATCH_FAVORITE = "android:query-arg-match-favorite";

    /** @hide */
    @IntDef(flag = true, prefix = { "MATCH_" }, value = {
            MATCH_DEFAULT,
            MATCH_INCLUDE,
            MATCH_EXCLUDE,
            MATCH_ONLY,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Match {}

    /**
     * Value indicating that the default matching behavior should be used, as
     * defined by the key documentation.
     */
    public static final int MATCH_DEFAULT = 0;

    /**
     * Value indicating that operations should include items matching the
     * criteria defined by this key.
     * <p>
     * Note that items <em>not</em> matching the criteria <em>may</em> also be
     * included depending on the default behavior documented by the key. If you
     * want to operate exclusively on matching items, use {@link #MATCH_ONLY}.
     */
    public static final int MATCH_INCLUDE = 1;

    /**
     * Value indicating that operations should exclude items matching the
     * criteria defined by this key.
     */
    public static final int MATCH_EXCLUDE = 2;

    /**
     * Value indicating that operations should only operate on items explicitly
     * matching the criteria defined by this key.
     */
    public static final int MATCH_ONLY = 3;

    /**
     * Update the given {@link Uri} to also include any pending media items from
     * calls such as
     * {@link ContentResolver#query(Uri, String[], Bundle, CancellationSignal)}.
     * By default no pending items are returned.
     *
     * @see MediaColumns#IS_PENDING
     * @deprecated consider migrating to {@link #QUERY_ARG_MATCH_PENDING} which
     *             is more expressive.
     */
    @Deprecated
    public static @NonNull Uri setIncludePending(@NonNull Uri uri) {
        return setIncludePending(uri.buildUpon()).build();
    }

    /** @hide */
    @Deprecated
    public static @NonNull Uri.Builder setIncludePending(@NonNull Uri.Builder uriBuilder) {
        return uriBuilder.appendQueryParameter(PARAM_INCLUDE_PENDING, "1");
    }

    /** @hide */
    @Deprecated
    public static boolean getIncludePending(@NonNull Uri uri) {
        return uri.getBooleanQueryParameter(MediaStore.PARAM_INCLUDE_PENDING, false);
    }

    /**
     * Update the given {@link Uri} to indicate that the caller requires the
     * original file contents when calling
     * {@link ContentResolver#openFileDescriptor(Uri, String)}.
     * <p>
     * This can be useful when the caller wants to ensure they're backing up the
     * exact bytes of the underlying media, without any Exif redaction being
     * performed.
     * <p>
     * If the original file contents cannot be provided, a
     * {@link UnsupportedOperationException} will be thrown when the returned
     * {@link Uri} is used, such as when the caller doesn't hold
     * {@link android.Manifest.permission#ACCESS_MEDIA_LOCATION}.
     *
     * @see MediaStore#getRequireOriginal(Uri)
     */
    public static @NonNull Uri setRequireOriginal(@NonNull Uri uri) {
        return uri.buildUpon().appendQueryParameter(PARAM_REQUIRE_ORIGINAL, "1").build();
    }

    /**
     * Return if the caller requires the original file contents when calling
     * {@link ContentResolver#openFileDescriptor(Uri, String)}.
     *
     * @see MediaStore#setRequireOriginal(Uri)
     */
    public static boolean getRequireOriginal(@NonNull Uri uri) {
        return uri.getBooleanQueryParameter(MediaStore.PARAM_REQUIRE_ORIGINAL, false);
    }

    /**
     * Returns {@link ParcelFileDescriptor} representing the original media file format for
     * {@code fileDescriptor}.
     *
     * <p>Media files may get transcoded based on an application's media capabilities requirements.
     * However, in various cases, when the application needs access to the original media file, or
     * doesn't attempt to parse the actual byte contents of media files, such as playback using
     * {@link MediaPlayer} or for off-device backup, this method can be useful.
     *
     * <p>This method is applicable only for media files managed by {@link MediaStore}.
     *
     * <p>The method returns the original file descriptor with the same permission that the caller
     * has for the input file descriptor.
     *
     * @throws IOException if the given {@link ParcelFileDescriptor} could not be converted
     *
     * @see MediaStore#EXTRA_ACCEPT_ORIGINAL_MEDIA_FORMAT
     */
    public static @NonNull ParcelFileDescriptor getOriginalMediaFormatFileDescriptor(
            @NonNull Context context,
            @NonNull ParcelFileDescriptor fileDescriptor) throws IOException {
        Bundle input = new Bundle();
        input.putParcelable(EXTRA_FILE_DESCRIPTOR, fileDescriptor);

        return context.getContentResolver().openTypedAssetFileDescriptor(Files.EXTERNAL_CONTENT_URI,
                "*/*", input).getParcelFileDescriptor();
    }

    /**
     * Rewrite the given {@link Uri} to point at
     * {@link MediaStore#AUTHORITY_LEGACY}.
     *
     * @see #AUTHORITY_LEGACY
     * @hide
     */
    @SystemApi
    public static @NonNull Uri rewriteToLegacy(@NonNull Uri uri) {
        return uri.buildUpon().authority(MediaStore.AUTHORITY_LEGACY).build();
    }

    /**
     * Called by the Mainline module to signal to {@link #AUTHORITY_LEGACY} that
     * data migration is starting.
     *
     * @hide
     */
    public static void startLegacyMigration(@NonNull ContentResolver resolver,
            @NonNull String volumeName) {
        try {
            resolver.call(AUTHORITY_LEGACY, START_LEGACY_MIGRATION_CALL, volumeName, null);
        } catch (Exception e) {
            Log.wtf(TAG, "Failed to deliver legacy migration event", e);
        }
    }

    /**
     * Called by the Mainline module to signal to {@link #AUTHORITY_LEGACY} that
     * data migration is finished. The legacy provider may choose to perform
     * clean-up operations at this point, such as deleting databases.
     *
     * @hide
     */
    public static void finishLegacyMigration(@NonNull ContentResolver resolver,
            @NonNull String volumeName) {
        try {
            resolver.call(AUTHORITY_LEGACY, FINISH_LEGACY_MIGRATION_CALL, volumeName, null);
        } catch (Exception e) {
            Log.wtf(TAG, "Failed to deliver legacy migration event", e);
        }
    }

    private static @NonNull PendingIntent createRequest(@NonNull ContentResolver resolver,
            @NonNull String method, @NonNull Collection<Uri> uris, @Nullable ContentValues values) {
        Objects.requireNonNull(resolver);
        Objects.requireNonNull(uris);

        final Iterator<Uri> it = uris.iterator();
        final ClipData clipData = ClipData.newRawUri(null, it.next());
        while (it.hasNext()) {
            clipData.addItem(new ClipData.Item(it.next()));
        }

        final Bundle extras = new Bundle();
        extras.putParcelable(EXTRA_CLIP_DATA, clipData);
        extras.putParcelable(EXTRA_CONTENT_VALUES, values);
        return resolver.call(AUTHORITY, method, null, extras).getParcelable(EXTRA_RESULT);
    }

    /**
     * Create a {@link PendingIntent} that will prompt the user to grant your
     * app write access for the requested media items.
     * <p>
     * This call only generates the request for a prompt; to display the prompt,
     * call {@link Activity#startIntentSenderForResult} with
     * {@link PendingIntent#getIntentSender()}. You can then determine if the
     * user granted your request by testing for {@link Activity#RESULT_OK} in
     * {@link Activity#onActivityResult}. The requested operation will have
     * completely finished before this activity result is delivered.
     * <p>
     * Permissions granted through this mechanism are tied to the lifecycle of
     * the {@link Activity} that requests them. If you need to retain
     * longer-term access for background actions, you can place items into a
     * {@link ClipData} or {@link Intent} which can then be passed to
     * {@link Context#startService} or
     * {@link android.app.job.JobInfo.Builder#setClipData}. Be sure to include
     * any relevant access modes you want to retain, such as
     * {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION}.
     * <p>
     * The displayed prompt will reflect all the media items you're requesting,
     * including those for which you already hold write access. If you want to
     * determine if you already hold write access before requesting access, use
     * {@link Context#checkUriPermission(Uri, int, int, int)} with
     * {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION}.
     * <p>
     * For security and performance reasons this method does not support
     * {@link Intent#FLAG_GRANT_PERSISTABLE_URI_PERMISSION} or
     * {@link Intent#FLAG_GRANT_PREFIX_URI_PERMISSION}.
     * <p>
     * The write access granted through this request is general-purpose, and
     * once obtained you can directly {@link ContentResolver#update} columns
     * like {@link MediaColumns#IS_FAVORITE}, {@link MediaColumns#IS_TRASHED},
     * or {@link ContentResolver#delete}.
     *
     * @param resolver Used to connect with {@link MediaStore#AUTHORITY}.
     *            Typically this value is {@link Context#getContentResolver()},
     *            but if you need more explicit lifecycle controls, you can
     *            obtain a {@link ContentProviderClient} and wrap it using
     *            {@link ContentResolver#wrap(ContentProviderClient)}.
     * @param uris The set of media items to include in this request. Each item
     *            must be hosted by {@link MediaStore#AUTHORITY} and must
     *            reference a specific media item by {@link BaseColumns#_ID}.
     */
    public static @NonNull PendingIntent createWriteRequest(@NonNull ContentResolver resolver,
            @NonNull Collection<Uri> uris) {
        return createRequest(resolver, CREATE_WRITE_REQUEST_CALL, uris, null);
    }

    /**
     * Create a {@link PendingIntent} that will prompt the user to trash the
     * requested media items. When the user approves this request,
     * {@link MediaColumns#IS_TRASHED} is set on these items.
     * <p>
     * This call only generates the request for a prompt; to display the prompt,
     * call {@link Activity#startIntentSenderForResult} with
     * {@link PendingIntent#getIntentSender()}. You can then determine if the
     * user granted your request by testing for {@link Activity#RESULT_OK} in
     * {@link Activity#onActivityResult}. The requested operation will have
     * completely finished before this activity result is delivered.
     * <p>
     * The displayed prompt will reflect all the media items you're requesting,
     * including those for which you already hold write access. If you want to
     * determine if you already hold write access before requesting access, use
     * {@link Context#checkUriPermission(Uri, int, int, int)} with
     * {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION}.
     *
     * @param resolver Used to connect with {@link MediaStore#AUTHORITY}.
     *            Typically this value is {@link Context#getContentResolver()},
     *            but if you need more explicit lifecycle controls, you can
     *            obtain a {@link ContentProviderClient} and wrap it using
     *            {@link ContentResolver#wrap(ContentProviderClient)}.
     * @param uris The set of media items to include in this request. Each item
     *            must be hosted by {@link MediaStore#AUTHORITY} and must
     *            reference a specific media item by {@link BaseColumns#_ID}.
     * @param value The {@link MediaColumns#IS_TRASHED} value to apply.
     * @see MediaColumns#IS_TRASHED
     * @see MediaStore#QUERY_ARG_MATCH_TRASHED
     */
    public static @NonNull PendingIntent createTrashRequest(@NonNull ContentResolver resolver,
            @NonNull Collection<Uri> uris, boolean value) {
        final ContentValues values = new ContentValues();
        if (value) {
            values.put(MediaColumns.IS_TRASHED, 1);
        } else {
            values.put(MediaColumns.IS_TRASHED, 0);
        }
        return createRequest(resolver, CREATE_TRASH_REQUEST_CALL, uris, values);
    }

    /**
     * Create a {@link PendingIntent} that will prompt the user to favorite the
     * requested media items. When the user approves this request,
     * {@link MediaColumns#IS_FAVORITE} is set on these items.
     * <p>
     * This call only generates the request for a prompt; to display the prompt,
     * call {@link Activity#startIntentSenderForResult} with
     * {@link PendingIntent#getIntentSender()}. You can then determine if the
     * user granted your request by testing for {@link Activity#RESULT_OK} in
     * {@link Activity#onActivityResult}. The requested operation will have
     * completely finished before this activity result is delivered.
     * <p>
     * The displayed prompt will reflect all the media items you're requesting,
     * including those for which you already hold write access. If you want to
     * determine if you already hold write access before requesting access, use
     * {@link Context#checkUriPermission(Uri, int, int, int)} with
     * {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION}.
     *
     * @param resolver Used to connect with {@link MediaStore#AUTHORITY}.
     *            Typically this value is {@link Context#getContentResolver()},
     *            but if you need more explicit lifecycle controls, you can
     *            obtain a {@link ContentProviderClient} and wrap it using
     *            {@link ContentResolver#wrap(ContentProviderClient)}.
     * @param uris The set of media items to include in this request. Each item
     *            must be hosted by {@link MediaStore#AUTHORITY} and must
     *            reference a specific media item by {@link BaseColumns#_ID}.
     * @param value The {@link MediaColumns#IS_FAVORITE} value to apply.
     * @see MediaColumns#IS_FAVORITE
     * @see MediaStore#QUERY_ARG_MATCH_FAVORITE
     */
    public static @NonNull PendingIntent createFavoriteRequest(@NonNull ContentResolver resolver,
            @NonNull Collection<Uri> uris, boolean value) {
        final ContentValues values = new ContentValues();
        if (value) {
            values.put(MediaColumns.IS_FAVORITE, 1);
        } else {
            values.put(MediaColumns.IS_FAVORITE, 0);
        }
        return createRequest(resolver, CREATE_FAVORITE_REQUEST_CALL, uris, values);
    }

    /**
     * Create a {@link PendingIntent} that will prompt the user to permanently
     * delete the requested media items. When the user approves this request,
     * {@link ContentResolver#delete} will be called on these items.
     * <p>
     * This call only generates the request for a prompt; to display the prompt,
     * call {@link Activity#startIntentSenderForResult} with
     * {@link PendingIntent#getIntentSender()}. You can then determine if the
     * user granted your request by testing for {@link Activity#RESULT_OK} in
     * {@link Activity#onActivityResult}. The requested operation will have
     * completely finished before this activity result is delivered.
     * <p>
     * The displayed prompt will reflect all the media items you're requesting,
     * including those for which you already hold write access. If you want to
     * determine if you already hold write access before requesting access, use
     * {@link Context#checkUriPermission(Uri, int, int, int)} with
     * {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION}.
     *
     * @param resolver Used to connect with {@link MediaStore#AUTHORITY}.
     *            Typically this value is {@link Context#getContentResolver()},
     *            but if you need more explicit lifecycle controls, you can
     *            obtain a {@link ContentProviderClient} and wrap it using
     *            {@link ContentResolver#wrap(ContentProviderClient)}.
     * @param uris The set of media items to include in this request. Each item
     *            must be hosted by {@link MediaStore#AUTHORITY} and must
     *            reference a specific media item by {@link BaseColumns#_ID}.
     */
    public static @NonNull PendingIntent createDeleteRequest(@NonNull ContentResolver resolver,
            @NonNull Collection<Uri> uris) {
        return createRequest(resolver, CREATE_DELETE_REQUEST_CALL, uris, null);
    }

    /**
     * Common media metadata columns.
     */
    public interface MediaColumns extends BaseColumns {
        /**
         * Absolute filesystem path to the media item on disk.
         * <p>
         * Apps may use this path to do file operations. However, they should not assume that the
         * file is always available. Apps must be prepared to handle any file-based I/O errors that
         * could occur.
         * <p>
         * From Android 11 onwards, this column is read-only for apps that target
         * {@link android.os.Build.VERSION_CODES#R R} and higher. On those devices, when creating or
         * updating a uri, this column's value is not accepted. Instead, to update the
         * filesystem location of a file, use the values of the {@link #DISPLAY_NAME} and
         * {@link #RELATIVE_PATH} columns.
         * <p>
         * Though direct file operations are supported,
         * {@link ContentResolver#openFileDescriptor(Uri, String)} API is recommended for better
         * performance.
         *
         */
        @Column(Cursor.FIELD_TYPE_STRING)
        public static final String DATA = "_data";

        /**
         * Indexed value of {@link File#length()} extracted from this media
         * item.
         */
        @BytesLong
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String SIZE = "_size";

        /**
         * The display name of the media item.
         * <p>
         * For example, an item stored at
         * {@code /storage/0000-0000/DCIM/Vacation/IMG1024.JPG} would have a
         * display name of {@code IMG1024.JPG}.
         */
        @Column(Cursor.FIELD_TYPE_STRING)
        public static final String DISPLAY_NAME = "_display_name";

        /**
         * The time the media item was first added.
         */
        @CurrentTimeSecondsLong
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String DATE_ADDED = "date_added";

        /**
         * Indexed value of {@link File#lastModified()} extracted from this
         * media item.
         */
        @CurrentTimeSecondsLong
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String DATE_MODIFIED = "date_modified";

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_DATE} or
         * {@link ExifInterface#TAG_DATETIME_ORIGINAL} extracted from this media
         * item.
         * <p>
         * Note that images must define both
         * {@link ExifInterface#TAG_DATETIME_ORIGINAL} and
         * {@code ExifInterface#TAG_OFFSET_TIME_ORIGINAL} to reliably determine
         * this value in relation to the epoch.
         */
        @CurrentTimeMillisLong
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String DATE_TAKEN = "datetaken";

        /**
         * The MIME type of the media item.
         * <p>
         * This is typically defined based on the file extension of the media
         * item. However, it may be the value of the {@code format} attribute
         * defined by the <em>Dublin Core Media Initiative</em> standard,
         * extracted from any XMP metadata contained within this media item.
         * <p class="note">
         * Note: the {@code format} attribute may be ignored if the top-level
         * MIME type disagrees with the file extension. For example, it's
         * reasonable for an {@code image/jpeg} file to declare a {@code format}
         * of {@code image/vnd.google.panorama360+jpg}, but declaring a
         * {@code format} of {@code audio/ogg} would be ignored.
         * <p>
         * This is a read-only column that is automatically computed.
         */
        @Column(Cursor.FIELD_TYPE_STRING)
        public static final String MIME_TYPE = "mime_type";

        /**
         * Flag indicating if a media item is DRM protected.
         */
        @Column(Cursor.FIELD_TYPE_INTEGER)
        public static final String IS_DRM = "is_drm";

        /**
         * Flag indicating if a media item is pending, and still being inserted
         * by its owner. While this flag is set, only the owner of the item can
         * open the underlying file; requests from other apps will be rejected.
         * <p>
         * Pending items are retained either until they are published by setting
         * the field to {@code 0}, or until they expire as defined by
         * {@link #DATE_EXPIRES}.
         *
         * @see MediaStore#QUERY_ARG_MATCH_PENDING
         */
        @Column(Cursor.FIELD_TYPE_INTEGER)
        public static final String IS_PENDING = "is_pending";

        /**
         * Flag indicating if a media item is trashed.
         * <p>
         * Trashed items are retained until they expire as defined by
         * {@link #DATE_EXPIRES}.
         *
         * @see MediaColumns#IS_TRASHED
         * @see MediaStore#QUERY_ARG_MATCH_TRASHED
         * @see MediaStore#createTrashRequest
         */
        @Column(Cursor.FIELD_TYPE_INTEGER)
        public static final String IS_TRASHED = "is_trashed";

        /**
         * The time the media item should be considered expired. Typically only
         * meaningful in the context of {@link #IS_PENDING} or
         * {@link #IS_TRASHED}.
         * <p>
         * The value stored in this column is automatically calculated when
         * {@link #IS_PENDING} or {@link #IS_TRASHED} is changed. The default
         * pending expiration is typically 7 days, and the default trashed
         * expiration is typically 30 days.
         * <p>
         * Expired media items are automatically deleted once their expiration
         * time has passed, typically during during the next device idle period.
         */
        @CurrentTimeSecondsLong
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String DATE_EXPIRES = "date_expires";

        /**
         * Indexed value of
         * {@link MediaMetadataRetriever#METADATA_KEY_VIDEO_WIDTH},
         * {@link MediaMetadataRetriever#METADATA_KEY_IMAGE_WIDTH} or
         * {@link ExifInterface#TAG_IMAGE_WIDTH} extracted from this media item.
         * <p>
         * Type: INTEGER
         */
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String WIDTH = "width";

        /**
         * Indexed value of
         * {@link MediaMetadataRetriever#METADATA_KEY_VIDEO_HEIGHT},
         * {@link MediaMetadataRetriever#METADATA_KEY_IMAGE_HEIGHT} or
         * {@link ExifInterface#TAG_IMAGE_LENGTH} extracted from this media
         * item.
         * <p>
         * Type: INTEGER
         */
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String HEIGHT = "height";

        /**
         * Calculated value that combines {@link #WIDTH} and {@link #HEIGHT}
         * into a user-presentable string.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String RESOLUTION = "resolution";

        /**
         * Package name that contributed this media. The value may be
         * {@code NULL} if ownership cannot be reliably determined.
         * <p>
         * From Android {@link Build.VERSION_CODES#UPSIDE_DOWN_CAKE} onwards,
         * visibility and query of this field will depend on
         * <a href="/training/basics/intents/package-visibility">package visibility</a>.
         * For {@link ContentResolver#query} operation, result set will
         * be restricted to visible packages only.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String OWNER_PACKAGE_NAME = "owner_package_name";

        /**
         * Volume name of the specific storage device where this media item is
         * persisted. The value is typically one of the volume names returned
         * from {@link MediaStore#getExternalVolumeNames(Context)}.
         * <p>
         * This is a read-only column that is automatically computed.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String VOLUME_NAME = "volume_name";

        /**
         * Relative path of this media item within the storage device where it
         * is persisted. For example, an item stored at
         * {@code /storage/0000-0000/DCIM/Vacation/IMG1024.JPG} would have a
         * path of {@code DCIM/Vacation/}.
         * <p>
         * This value should only be used for organizational purposes, and you
         * should not attempt to construct or access a raw filesystem path using
         * this value. If you need to open a media item, use an API like
         * {@link ContentResolver#openFileDescriptor(Uri, String)}.
         * <p>
         * When this value is set to {@code NULL} during an
         * {@link ContentResolver#insert} operation, the newly created item will
         * be placed in a relevant default location based on the type of media
         * being inserted. For example, a {@code image/jpeg} item will be placed
         * under {@link Environment#DIRECTORY_PICTURES}.
         * <p>
         * You can modify this column during an {@link ContentResolver#update}
         * call, which will move the underlying file on disk.
         * <p>
         * In both cases above, content must be placed under a top-level
         * directory that is relevant to the media type. For example, attempting
         * to place a {@code audio/mpeg} file under
         * {@link Environment#DIRECTORY_PICTURES} will be rejected.
         */
        @Column(Cursor.FIELD_TYPE_STRING)
        public static final String RELATIVE_PATH = "relative_path";

        /**
         * The primary bucket ID of this media item. This can be useful to
         * present the user a first-level clustering of related media items.
         * This is a read-only column that is automatically computed.
         */
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String BUCKET_ID = "bucket_id";

        /**
         * The primary bucket display name of this media item. This can be
         * useful to present the user a first-level clustering of related
         * media items. This is a read-only column that is automatically
         * computed.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String BUCKET_DISPLAY_NAME = "bucket_display_name";

        /**
         * The group ID of this media item. This can be useful to present
         * the user a grouping of related media items, such a burst of
         * images, or a {@code JPG} and {@code DNG} version of the same
         * image.
         * <p>
         * This is a read-only column that is automatically computed based
         * on the first portion of the filename. For example,
         * {@code IMG1024.BURST001.JPG} and {@code IMG1024.BURST002.JPG}
         * will have the same {@link #GROUP_ID} because the first portion of
         * their filenames is identical.
         *
         * @removed
         */
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        @Deprecated
        public static final String GROUP_ID = "group_id";

        /**
         * The "document ID" GUID as defined by the <em>XMP Media
         * Management</em> standard, extracted from any XMP metadata contained
         * within this media item. The value is {@code null} when no metadata
         * was found.
         * <p>
         * Each "document ID" is created once for each new resource. Different
         * renditions of that resource are expected to have different IDs.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String DOCUMENT_ID = "document_id";

        /**
         * The "instance ID" GUID as defined by the <em>XMP Media
         * Management</em> standard, extracted from any XMP metadata contained
         * within this media item. The value is {@code null} when no metadata
         * was found.
         * <p>
         * This "instance ID" changes with each save operation of a specific
         * "document ID".
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String INSTANCE_ID = "instance_id";

        /**
         * The "original document ID" GUID as defined by the <em>XMP Media
         * Management</em> standard, extracted from any XMP metadata contained
         * within this media item.
         * <p>
         * This "original document ID" links a resource to its original source.
         * For example, when you save a PSD document as a JPEG, then convert the
         * JPEG to GIF format, the "original document ID" of both the JPEG and
         * GIF files is the "document ID" of the original PSD file.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String ORIGINAL_DOCUMENT_ID = "original_document_id";

        /**
         * Indexed value of
         * {@link MediaMetadataRetriever#METADATA_KEY_VIDEO_ROTATION},
         * {@link MediaMetadataRetriever#METADATA_KEY_IMAGE_ROTATION}, or
         * {@link ExifInterface#TAG_ORIENTATION} extracted from this media item.
         * <p>
         * For consistency the indexed value is expressed in degrees, such as 0,
         * 90, 180, or 270.
         * <p>
         * Type: INTEGER
         */
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String ORIENTATION = "orientation";

        /**
         * Flag indicating if the media item has been marked as being a
         * "favorite" by the user.
         *
         * @see MediaColumns#IS_FAVORITE
         * @see MediaStore#QUERY_ARG_MATCH_FAVORITE
         * @see MediaStore#createFavoriteRequest
         */
        @Column(Cursor.FIELD_TYPE_INTEGER)
        public static final String IS_FAVORITE = "is_favorite";

        /**
         * Flag indicating if the media item has been marked as being part of
         * the {@link Downloads} collection.
         */
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String IS_DOWNLOAD = "is_download";

        /**
         * Generation number at which metadata for this media item was first
         * inserted. This is useful for apps that are attempting to quickly
         * identify exactly which media items have been added since a previous
         * point in time. Generation numbers are monotonically increasing over
         * time, and can be safely arithmetically compared.
         * <p>
         * Detecting media additions using generation numbers is more robust
         * than using {@link #DATE_ADDED}, since those values may change in
         * unexpected ways when apps use {@link File#setLastModified(long)} or
         * when the system clock is set incorrectly.
         * <p>
         * Note that before comparing these detailed generation values, you
         * should first confirm that the overall version hasn't changed by
         * checking {@link MediaStore#getVersion(Context, String)}, since that
         * indicates when a more radical change has occurred. If the overall
         * version changes, you should assume that generation numbers have been
         * reset and perform a full synchronization pass.
         *
         * @see MediaStore#getGeneration(Context, String)
         */
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String GENERATION_ADDED = "generation_added";

        /**
         * Generation number at which metadata for this media item was last
         * changed. This is useful for apps that are attempting to quickly
         * identify exactly which media items have changed since a previous
         * point in time. Generation numbers are monotonically increasing over
         * time, and can be safely arithmetically compared.
         * <p>
         * Detecting media changes using generation numbers is more robust than
         * using {@link #DATE_MODIFIED}, since those values may change in
         * unexpected ways when apps use {@link File#setLastModified(long)} or
         * when the system clock is set incorrectly.
         * <p>
         * Note that before comparing these detailed generation values, you
         * should first confirm that the overall version hasn't changed by
         * checking {@link MediaStore#getVersion(Context, String)}, since that
         * indicates when a more radical change has occurred. If the overall
         * version changes, you should assume that generation numbers have been
         * reset and perform a full synchronization pass.
         *
         * @see MediaStore#getGeneration(Context, String)
         */
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String GENERATION_MODIFIED = "generation_modified";

        /**
         * Indexed XMP metadata extracted from this media item.
         * <p>
         * The structure of this metadata is defined by the <a href=
         * "https://en.wikipedia.org/wiki/Extensible_Metadata_Platform"><em>XMP
         * Media Management</em> standard</a>, published as ISO 16684-1:2012.
         * <p>
         * This metadata is typically extracted from a
         * {@link ExifInterface#TAG_XMP} contained inside an image file or from
         * a {@code XMP_} box contained inside an ISO/IEC base media file format
         * (MPEG-4 Part 12).
         * <p>
         * Note that any location details are redacted from this metadata for
         * privacy reasons.
         */
        @Column(value = Cursor.FIELD_TYPE_BLOB, readOnly = true)
        public static final String XMP = "xmp";

        // =======================================
        // ==== MediaMetadataRetriever values ====
        // =======================================

        /**
         * Indexed value of
         * {@link MediaMetadataRetriever#METADATA_KEY_CD_TRACK_NUMBER} extracted
         * from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String CD_TRACK_NUMBER = "cd_track_number";

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_ALBUM}
         * extracted from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String ALBUM = "album";

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_ARTIST}
         * or {@link ExifInterface#TAG_ARTIST} extracted from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String ARTIST = "artist";

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_AUTHOR}
         * extracted from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String AUTHOR = "author";

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_COMPOSER}
         * extracted from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String COMPOSER = "composer";

        // METADATA_KEY_DATE is DATE_TAKEN

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_GENRE}
         * extracted from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String GENRE = "genre";

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_TITLE}
         * extracted from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String TITLE = "title";

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_YEAR}
         * extracted from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String YEAR = "year";

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_DURATION}
         * extracted from this media item.
         */
        @DurationMillisLong
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String DURATION = "duration";

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_NUM_TRACKS}
         * extracted from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String NUM_TRACKS = "num_tracks";

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_WRITER}
         * extracted from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String WRITER = "writer";

        // METADATA_KEY_MIMETYPE is MIME_TYPE

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_ALBUMARTIST}
         * extracted from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String ALBUM_ARTIST = "album_artist";

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_DISC_NUMBER}
         * extracted from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String DISC_NUMBER = "disc_number";

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_COMPILATION}
         * extracted from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String COMPILATION = "compilation";

        // HAS_AUDIO is ignored
        // HAS_VIDEO is ignored
        // VIDEO_WIDTH is WIDTH
        // VIDEO_HEIGHT is HEIGHT

        /**
         * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_BITRATE}
         * extracted from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String BITRATE = "bitrate";

        // TIMED_TEXT_LANGUAGES is ignored
        // IS_DRM is ignored
        // LOCATION is LATITUDE and LONGITUDE
        // VIDEO_ROTATION is ORIENTATION

        /**
         * Indexed value of
         * {@link MediaMetadataRetriever#METADATA_KEY_CAPTURE_FRAMERATE}
         * extracted from this media item.
         */
        @Column(value = Cursor.FIELD_TYPE_FLOAT, readOnly = true)
        public static final String CAPTURE_FRAMERATE = "capture_framerate";

        // HAS_IMAGE is ignored
        // IMAGE_COUNT is ignored
        // IMAGE_PRIMARY is ignored
        // IMAGE_WIDTH is WIDTH
        // IMAGE_HEIGHT is HEIGHT
        // IMAGE_ROTATION is ORIENTATION
        // VIDEO_FRAME_COUNT is ignored
        // EXIF_OFFSET is ignored
        // EXIF_LENGTH is ignored
        // COLOR_STANDARD is ignored
        // COLOR_TRANSFER is ignored
        // COLOR_RANGE is ignored
        // SAMPLERATE is ignored
        // BITS_PER_SAMPLE is ignored
    }

    /**
     * Photo picker metadata columns.
     *
     * @see #ACTION_PICK_IMAGES
     */
    public static class PickerMediaColumns {
        private PickerMediaColumns() {}

        /**
         * This is identical to {@link MediaColumns#DATA}, however, apps should not assume that the
         * file is always available because the file may be backed by a {@link CloudMediaProvider}
         * fetching content over a network. Therefore, apps must be prepared to handle any
         * additional file-based I/O errors that could occur as a result of network errors.
         *
         * @see MediaColumns#DATA
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String DATA = MediaColumns.DATA;

        /**
         * This is identical to {@link MediaColumns#SIZE}.
         *
         * @see MediaColumns#SIZE
         */
        @BytesLong
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String SIZE = MediaColumns.SIZE;

        /**
         * This is identical to {@link MediaColumns#DISPLAY_NAME}.
         *
         * @see MediaColumns#DISPLAY_NAME
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String DISPLAY_NAME = MediaColumns.DISPLAY_NAME;

        /**
         * This is identical to {@link MediaColumns#DATE_TAKEN}.
         *
         * @see MediaColumns#DATE_TAKEN
         */
        @CurrentTimeMillisLong
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String DATE_TAKEN = MediaColumns.DATE_TAKEN;

        /**
         * This is identical to {@link MediaColumns#MIME_TYPE}.
         *
         * @see MediaColumns#MIME_TYPE
         */
        @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
        public static final String MIME_TYPE = MediaColumns.MIME_TYPE;

        /**
         * This is identical to {@link MediaColumns#DURATION}.
         *
         * @see MediaColumns#DURATION
         */
        @DurationMillisLong
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String DURATION_MILLIS = MediaColumns.DURATION;

        /**
         * This is identical to {@link MediaColumns#WIDTH}.
         *
         * @see MediaColumns#WIDTH
         */
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String WIDTH = "width";

        /**
         * This is identical to {@link MediaColumns#HEIGHT}.
         *
         * @see MediaColumns#HEIGHT
         */
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String HEIGHT = "height";

        /**
         * This is identical to {@link MediaColumns#ORIENTATION}.
         *
         * @see MediaColumns#ORIENTATION
         */
        @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
        public static final String ORIENTATION = "orientation";
    }

    /**
     * Media provider table containing an index of all files in the media storage,
     * including non-media files.  This should be used by applications that work with
     * non-media file types (text, HTML, PDF, etc) as well as applications that need to
     * work with multiple media file types in a single query.
     */
    public static final class Files {
        /** @hide */
        public static final String TABLE = "files";

        /** @hide */
        public static final Uri EXTERNAL_CONTENT_URI = getContentUri(VOLUME_EXTERNAL);

        /**
         * Get the content:// style URI for the files table on the
         * given volume.
         *
         * @param volumeName the name of the volume to get the URI for
         * @return the URI to the files table on the given volume
         */
        public static Uri getContentUri(String volumeName) {
            return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("file").build();
        }

        /**
         * Get the content:// style URI for a single row in the files table on the
         * given volume.
         *
         * @param volumeName the name of the volume to get the URI for
         * @param rowId the file to get the URI for
         * @return the URI to the files table on the given volume
         */
        public static final Uri getContentUri(String volumeName,
                long rowId) {
            return ContentUris.withAppendedId(getContentUri(volumeName), rowId);
        }

        /** {@hide} */
        @UnsupportedAppUsage
        public static Uri getMtpObjectsUri(@NonNull String volumeName) {
            return MediaStore.Files.getContentUri(volumeName);
        }

        /** {@hide} */
        @UnsupportedAppUsage
        public static final Uri getMtpObjectsUri(@NonNull String volumeName, long fileId) {
            return MediaStore.Files.getContentUri(volumeName, fileId);
        }

        /** {@hide} */
        @UnsupportedAppUsage
        public static final Uri getMtpReferencesUri(@NonNull String volumeName, long fileId) {
            return MediaStore.Files.getContentUri(volumeName, fileId);
        }

        /**
         * Used to trigger special logic for directories.
         * @hide
         */
        public static final Uri getDirectoryUri(String volumeName) {
            return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("dir").build();
        }

        /** @hide */
        public static final Uri getContentUriForPath(String path) {
            return getContentUri(getVolumeName(new File(path)));
        }

        /**
         * File metadata columns.
         */
        public interface FileColumns extends MediaColumns {
            /**
             * The MTP storage ID of the file
             * @hide
             */
            @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
            @Deprecated
            // @Column(Cursor.FIELD_TYPE_INTEGER)
            public static final String STORAGE_ID = "storage_id";

            /**
             * The MTP format code of the file
             * @hide
             */
            @UnsupportedAppUsage
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String FORMAT = "format";

            /**
             * The index of the parent directory of the file
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String PARENT = "parent";

            /**
             * The MIME type of the media item.
             * <p>
             * This is typically defined based on the file extension of the media
             * item. However, it may be the value of the {@code format} attribute
             * defined by the <em>Dublin Core Media Initiative</em> standard,
             * extracted from any XMP metadata contained within this media item.
             * <p class="note">
             * Note: the {@code format} attribute may be ignored if the top-level
             * MIME type disagrees with the file extension. For example, it's
             * reasonable for an {@code image/jpeg} file to declare a {@code format}
             * of {@code image/vnd.google.panorama360+jpg}, but declaring a
             * {@code format} of {@code audio/ogg} would be ignored.
             * <p>
             * This is a read-only column that is automatically computed.
             */
            @Column(Cursor.FIELD_TYPE_STRING)
            public static final String MIME_TYPE = "mime_type";

            /** @removed promoted to parent interface */
            public static final String TITLE = "title";

            /**
             * The media type (audio, video, image, document, playlist or subtitle)
             * of the file, or 0 for not a media file
             */
            @Column(Cursor.FIELD_TYPE_INTEGER)
            public static final String MEDIA_TYPE = "media_type";

            /**
             * Constant for the {@link #MEDIA_TYPE} column indicating that file
             * is not an audio, image, video, document, playlist, or subtitles file.
             */
            public static final int MEDIA_TYPE_NONE = 0;

            /**
             * Constant for the {@link #MEDIA_TYPE} column indicating that file
             * is an image file.
             */
            public static final int MEDIA_TYPE_IMAGE = 1;

            /**
             * Constant for the {@link #MEDIA_TYPE} column indicating that file
             * is an audio file.
             */
            public static final int MEDIA_TYPE_AUDIO = 2;

            /**
             * Constant for the {@link #MEDIA_TYPE} column indicating that file
             * is a video file.
             */
            public static final int MEDIA_TYPE_VIDEO = 3;

            /**
             * Constant for the {@link #MEDIA_TYPE} column indicating that file
             * is a playlist file.
             *
             * @deprecated Android playlists are now deprecated. We will keep the current
             *             functionality for compatibility reasons, but we will no longer take
             *             feature request. We do not advise adding new usages of Android Playlists.
             *             M3U files can be used as an alternative.
             */
            @Deprecated
            public static final int MEDIA_TYPE_PLAYLIST = 4;

            /**
             * Constant for the {@link #MEDIA_TYPE} column indicating that file
             * is a subtitles or lyrics file.
             */
            public static final int MEDIA_TYPE_SUBTITLE = 5;

            /**
             * Constant for the {@link #MEDIA_TYPE} column indicating that file is a document file.
             */
            public static final int MEDIA_TYPE_DOCUMENT = 6;

            /**
             * Constant indicating the count of {@link #MEDIA_TYPE} columns.
             * @hide
             */
            public static final int MEDIA_TYPE_COUNT = 7;

            /**
             * Modifier of the database row
             *
             * Specifies the last modifying operation of the database row. This
             * does not give any information on the package that modified the
             * database row.
             * Initially, this column will be populated by
             * {@link ContentResolver}#insert and media scan operations. And,
             * the column will be used to identify if the file was previously
             * scanned.
             * @hide
             */
            // @Column(value = Cursor.FIELD_TYPE_INTEGER)
            public static final String _MODIFIER = "_modifier";

            /**
             * Constant for the {@link #_MODIFIER} column indicating
             * that the last modifier of the database row is FUSE operation.
             * @hide
             */
            public static final int _MODIFIER_FUSE = 1;

            /**
             * Constant for the {@link #_MODIFIER} column indicating
             * that the last modifier of the database row is explicit
             * {@link ContentResolver} operation from app.
             * @hide
             */
            public static final int _MODIFIER_CR = 2;

            /**
             * Constant for the {@link #_MODIFIER} column indicating
             * that the last modifier of the database row is a media scan
             * operation.
             * @hide
             */
            public static final int _MODIFIER_MEDIA_SCAN = 3;

            /**
             * Constant for the {@link #_MODIFIER} column indicating
             * that the last modifier of the database row is explicit
             * {@link ContentResolver} operation and is waiting for metadata
             * update.
             * @hide
             */
            public static final int _MODIFIER_CR_PENDING_METADATA = 4;

            /**
             * Status of the transcode file
             *
             * For apps that do not support modern media formats for video, we
             * seamlessly transcode the file and return transcoded file for
             * both file path and ContentResolver operations. This column tracks
             * the status of the transcoded file.
             *
             * @hide
             */
            // @Column(value = Cursor.FIELD_TYPE_INTEGER)
            public static final String _TRANSCODE_STATUS = "_transcode_status";

            /**
             * Constant for the {@link #_TRANSCODE_STATUS} column indicating
             * that the transcode file if exists is empty or never transcoded.
             * @hide
             */
            public static final int TRANSCODE_EMPTY = 0;

            /**
             * Constant for the {@link #_TRANSCODE_STATUS} column indicating
             * that the transcode file if exists contains transcoded video.
             * @hide
             */
            public static final int TRANSCODE_COMPLETE = 1;

            /**
             * Indexed value of {@link MediaMetadataRetriever#METADATA_KEY_VIDEO_CODEC_TYPE}
             * extracted from the video file. This value be null for non-video files.
             *
             * @hide
             */
            // @Column(value = Cursor.FIELD_TYPE_INTEGER)
            public static final String _VIDEO_CODEC_TYPE = "_video_codec_type";

            /**
             * Redacted Uri-ID corresponding to this DB entry. The value will be null if no
             * redacted uri has ever been created for this uri.
             *
             * @hide
             */
            // @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String REDACTED_URI_ID = "redacted_uri_id";

            /**
             * Indexed value of {@link UserIdInt} to which the file belongs.
             *
             * @hide
             */
            // @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String _USER_ID = "_user_id";

            /**
             * Special format for a file.
             *
             * Photo Picker requires special format tagging for media files.
             * This is essential as {@link Images} collection can include
             * images of various formats like Motion Photos, GIFs etc, which
             * is not identifiable by {@link #MIME_TYPE}.
             *
             * @hide
             */
            // @Column(value = Cursor.FIELD_TYPE_INTEGER)
            public static final String _SPECIAL_FORMAT = "_special_format";

            /**
             * Constant for the {@link #_SPECIAL_FORMAT} column indicating
             * that the file doesn't have any special format associated with it.
             *
             * @hide
             */
            public static final int _SPECIAL_FORMAT_NONE =
                    CloudMediaProviderContract.MediaColumns.STANDARD_MIME_TYPE_EXTENSION_NONE;

            /**
             * Constant for the {@link #_SPECIAL_FORMAT} column indicating
             * that the file is a GIF file.
             *
             * @hide
             */
            public static final int _SPECIAL_FORMAT_GIF =
                    CloudMediaProviderContract.MediaColumns.STANDARD_MIME_TYPE_EXTENSION_GIF;

            /**
             * Constant for the {@link #_SPECIAL_FORMAT} column indicating
             * that the file is a Motion Photo.
             *
             * @hide
             */
            public static final int _SPECIAL_FORMAT_MOTION_PHOTO =
                    CloudMediaProviderContract.MediaColumns.
                            STANDARD_MIME_TYPE_EXTENSION_MOTION_PHOTO;

            /**
             * Constant for the {@link #_SPECIAL_FORMAT} column indicating
             * that the file is an Animated Webp.
             *
             * @hide
             */
            public static final int _SPECIAL_FORMAT_ANIMATED_WEBP =
                    CloudMediaProviderContract.MediaColumns.
                            STANDARD_MIME_TYPE_EXTENSION_ANIMATED_WEBP;
        }
    }

    /** @hide */
    public static class ThumbnailConstants {
        public static final int MINI_KIND = 1;
        public static final int FULL_SCREEN_KIND = 2;
        public static final int MICRO_KIND = 3;

        public static final Size MINI_SIZE = new Size(512, 384);
        public static final Size FULL_SCREEN_SIZE = new Size(1024, 786);
        public static final Size MICRO_SIZE = new Size(96, 96);

        public static @NonNull Size getKindSize(int kind) {
            if (kind == ThumbnailConstants.MICRO_KIND) {
                return ThumbnailConstants.MICRO_SIZE;
            } else if (kind == ThumbnailConstants.FULL_SCREEN_KIND) {
                return ThumbnailConstants.FULL_SCREEN_SIZE;
            } else if (kind == ThumbnailConstants.MINI_KIND) {
                return ThumbnailConstants.MINI_SIZE;
            } else {
                throw new IllegalArgumentException("Unsupported kind: " + kind);
            }
        }
    }

    /**
     * Download metadata columns.
     */
    public interface DownloadColumns extends MediaColumns {
        /**
         * Uri indicating where the item has been downloaded from.
         */
        @Column(Cursor.FIELD_TYPE_STRING)
        String DOWNLOAD_URI = "download_uri";

        /**
         * Uri indicating HTTP referer of {@link #DOWNLOAD_URI}.
         */
        @Column(Cursor.FIELD_TYPE_STRING)
        String REFERER_URI = "referer_uri";

        /**
         * The description of the download.
         *
         * @removed
         */
        @Deprecated
        @Column(Cursor.FIELD_TYPE_STRING)
        String DESCRIPTION = "description";
    }

    /**
     * Collection of downloaded items.
     */
    public static final class Downloads implements DownloadColumns {
        private Downloads() {}

        /**
         * The content:// style URI for the internal storage.
         */
        @NonNull
        public static final Uri INTERNAL_CONTENT_URI =
                getContentUri("internal");

        /**
         * The content:// style URI for the "primary" external storage
         * volume.
         */
        @NonNull
        public static final Uri EXTERNAL_CONTENT_URI =
                getContentUri("external");

        /**
         * The MIME type for this table.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/download";

        /**
         * Get the content:// style URI for the downloads table on the
         * given volume.
         *
         * @param volumeName the name of the volume to get the URI for
         * @return the URI to the image media table on the given volume
         */
        public static @NonNull Uri getContentUri(@NonNull String volumeName) {
            return AUTHORITY_URI.buildUpon().appendPath(volumeName)
                    .appendPath("downloads").build();
        }

        /**
         * Get the content:// style URI for a single row in the downloads table
         * on the given volume.
         *
         * @param volumeName the name of the volume to get the URI for
         * @param id the download to get the URI for
         * @return the URI to the downloads table on the given volume
         */
        public static @NonNull Uri getContentUri(@NonNull String volumeName, long id) {
            return ContentUris.withAppendedId(getContentUri(volumeName), id);
        }

        /** @hide */
        public static @NonNull Uri getContentUriForPath(@NonNull String path) {
            return getContentUri(getVolumeName(new File(path)));
        }
    }

    /**
     * Regex that matches paths under well-known storage paths.
     * Copied from FileUtils.java
     */
    private static final Pattern PATTERN_VOLUME_NAME = Pattern.compile(
            "(?i)^/storage/([^/]+)");

    /**
     * @deprecated since this method doesn't have a {@link Context}, we can't
     *             find the actual {@link StorageVolume} for the given path, so
     *             only a vague guess is returned. Callers should use
     *             {@link StorageManager#getStorageVolume(File)} instead.
     * @hide
     */
    @Deprecated
    public static @NonNull String getVolumeName(@NonNull File path) {
        // Ideally we'd find the relevant StorageVolume, but we don't have a
        // Context to obtain it from, so the best we can do is assume
        // Borrowed the logic from FileUtils.extractVolumeName
        final Matcher matcher = PATTERN_VOLUME_NAME.matcher(path.getAbsolutePath());
        if (matcher.find()) {
            final String volumeName = matcher.group(1);
            if (volumeName.equals("emulated")) {
                return MediaStore.VOLUME_EXTERNAL_PRIMARY;
            } else {
                return volumeName.toLowerCase(Locale.ROOT);
            }
        } else {
            return MediaStore.VOLUME_INTERNAL;
        }
    }

    /**
     * This class is used internally by Images.Thumbnails and Video.Thumbnails, it's not intended
     * to be accessed elsewhere.
     */
    @Deprecated
    private static class InternalThumbnails implements BaseColumns {
        /**
         * Currently outstanding thumbnail requests that can be cancelled.
         */
        // @GuardedBy("sPending")
        private static ArrayMap<Uri, CancellationSignal> sPending = new ArrayMap<>();

        /**
         * Make a blocking request to obtain the given thumbnail, generating it
         * if needed.
         *
         * @see #cancelThumbnail(ContentResolver, Uri)
         */
        @Deprecated
        static @Nullable Bitmap getThumbnail(@NonNull ContentResolver cr, @NonNull Uri uri,
                int kind, @Nullable BitmapFactory.Options opts) {
            final Size size = ThumbnailConstants.getKindSize(kind);

            CancellationSignal signal = null;
            synchronized (sPending) {
                signal = sPending.get(uri);
                if (signal == null) {
                    signal = new CancellationSignal();
                    sPending.put(uri, signal);
                }
            }

            try {
                return cr.loadThumbnail(uri, size, signal);
            } catch (IOException e) {
                Log.w(TAG, "Failed to obtain thumbnail for " + uri, e);
                return null;
            } finally {
                synchronized (sPending) {
                    sPending.remove(uri);
                }
            }
        }

        /**
         * This method cancels the thumbnail request so clients waiting for
         * {@link #getThumbnail} will be interrupted and return immediately.
         * Only the original process which made the request can cancel their own
         * requests.
         */
        @Deprecated
        static void cancelThumbnail(@NonNull ContentResolver cr, @NonNull Uri uri) {
            synchronized (sPending) {
                final CancellationSignal signal = sPending.get(uri);
                if (signal != null) {
                    signal.cancel();
                }
            }
        }
    }

    /**
     * Collection of all media with MIME type of {@code image/*}.
     */
    public static final class Images {
        /**
         * Image metadata columns.
         */
        public interface ImageColumns extends MediaColumns {
            /**
             * The picasa id of the image
             *
             * @deprecated this value was only relevant for images hosted on
             *             Picasa, which are no longer supported.
             */
            @Deprecated
            @Column(Cursor.FIELD_TYPE_STRING)
            public static final String PICASA_ID = "picasa_id";

            /**
             * Whether the image should be published as public or private
             */
            @Column(Cursor.FIELD_TYPE_INTEGER)
            public static final String IS_PRIVATE = "isprivate";

            /**
             * The latitude where the image was captured.
             *
             * @deprecated location details are no longer indexed for privacy
             *             reasons, and this value is now always {@code null}.
             *             You can still manually obtain location metadata using
             *             {@link ExifInterface#getLatLong(float[])}.
             */
            @Deprecated
            @Column(value = Cursor.FIELD_TYPE_FLOAT, readOnly = true)
            public static final String LATITUDE = "latitude";

            /**
             * The longitude where the image was captured.
             *
             * @deprecated location details are no longer indexed for privacy
             *             reasons, and this value is now always {@code null}.
             *             You can still manually obtain location metadata using
             *             {@link ExifInterface#getLatLong(float[])}.
             */
            @Deprecated
            @Column(value = Cursor.FIELD_TYPE_FLOAT, readOnly = true)
            public static final String LONGITUDE = "longitude";

            /** @removed promoted to parent interface */
            public static final String DATE_TAKEN = "datetaken";
            /** @removed promoted to parent interface */
            public static final String ORIENTATION = "orientation";

            /**
             * The mini thumb id.
             *
             * @deprecated all thumbnails should be obtained via
             *             {@link MediaStore.Images.Thumbnails#getThumbnail}, as this
             *             value is no longer supported.
             */
            @Deprecated
            @Column(Cursor.FIELD_TYPE_INTEGER)
            public static final String MINI_THUMB_MAGIC = "mini_thumb_magic";

            /** @removed promoted to parent interface */
            public static final String BUCKET_ID = "bucket_id";
            /** @removed promoted to parent interface */
            public static final String BUCKET_DISPLAY_NAME = "bucket_display_name";
            /** @removed promoted to parent interface */
            public static final String GROUP_ID = "group_id";

            /**
             * Indexed value of {@link ExifInterface#TAG_IMAGE_DESCRIPTION}
             * extracted from this media item.
             */
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String DESCRIPTION = "description";

            /**
             * Indexed value of {@link ExifInterface#TAG_EXPOSURE_TIME}
             * extracted from this media item.
             */
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String EXPOSURE_TIME = "exposure_time";

            /**
             * Indexed value of {@link ExifInterface#TAG_F_NUMBER}
             * extracted from this media item.
             */
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String F_NUMBER = "f_number";

            /**
             * Indexed value of {@link ExifInterface#TAG_ISO_SPEED_RATINGS}
             * extracted from this media item.
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String ISO = "iso";

            /**
             * Indexed value of {@link ExifInterface#TAG_SCENE_CAPTURE_TYPE}
             * extracted from this media item.
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String SCENE_CAPTURE_TYPE = "scene_capture_type";
        }

        public static final class Media implements ImageColumns {
            /**
             * @deprecated all queries should be performed through
             *             {@link ContentResolver} directly, which offers modern
             *             features like {@link CancellationSignal}.
             */
            @Deprecated
            public static final Cursor query(ContentResolver cr, Uri uri, String[] projection) {
                return cr.query(uri, projection, null, null, DEFAULT_SORT_ORDER);
            }

            /**
             * @deprecated all queries should be performed through
             *             {@link ContentResolver} directly, which offers modern
             *             features like {@link CancellationSignal}.
             */
            @Deprecated
            public static final Cursor query(ContentResolver cr, Uri uri, String[] projection,
                    String where, String orderBy) {
                return cr.query(uri, projection, where,
                                             null, orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
            }

            /**
             * @deprecated all queries should be performed through
             *             {@link ContentResolver} directly, which offers modern
             *             features like {@link CancellationSignal}.
             */
            @Deprecated
            public static final Cursor query(ContentResolver cr, Uri uri, String[] projection,
                    String selection, String [] selectionArgs, String orderBy) {
                return cr.query(uri, projection, selection,
                        selectionArgs, orderBy == null ? DEFAULT_SORT_ORDER : orderBy);
            }

            /**
             * Retrieves an image for the given url as a {@link Bitmap}.
             *
             * @param cr The content resolver to use
             * @param url The url of the image
             * @deprecated loading of images should be performed through
             *             {@link ImageDecoder#createSource(ContentResolver, Uri)},
             *             which offers modern features like
             *             {@link PostProcessor}.
             */
            @Deprecated
            public static final Bitmap getBitmap(ContentResolver cr, Uri url)
                    throws FileNotFoundException, IOException {
                InputStream input = cr.openInputStream(url);
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                return bitmap;
            }

            /**
             * Insert an image and create a thumbnail for it.
             *
             * @param cr The content resolver to use
             * @param imagePath The path to the image to insert
             * @param name The name of the image
             * @param description The description of the image
             * @return The URL to the newly created image
             * @deprecated inserting of images should be performed using
             *             {@link MediaColumns#IS_PENDING}, which offers richer
             *             control over lifecycle.
             */
            @Deprecated
            public static final String insertImage(ContentResolver cr, String imagePath,
                    String name, String description) throws FileNotFoundException {
                final Bitmap source;
                try {
                    source = ImageDecoder
                            .decodeBitmap(ImageDecoder.createSource(new File(imagePath)));
                } catch (IOException e) {
                    throw new FileNotFoundException(e.getMessage());
                }
                return insertImage(cr, source, name, description);
            }

            /**
             * Insert an image and create a thumbnail for it.
             *
             * @param cr The content resolver to use
             * @param source The stream to use for the image
             * @param title The name of the image
             * @param description The description of the image
             * @return The URL to the newly created image, or <code>null</code> if the image failed to be stored
             *              for any reason.
             * @deprecated inserting of images should be performed using
             *             {@link MediaColumns#IS_PENDING}, which offers richer
             *             control over lifecycle.
             */
            @Deprecated
            public static final String insertImage(ContentResolver cr, Bitmap source, String title,
                    String description) {
                if (TextUtils.isEmpty(title)) title = "Image";

                final long now = System.currentTimeMillis();
                final ContentValues values = new ContentValues();
                values.put(MediaColumns.DISPLAY_NAME, title);
                values.put(MediaColumns.MIME_TYPE, "image/jpeg");
                values.put(MediaColumns.DATE_ADDED, now / 1000);
                values.put(MediaColumns.DATE_MODIFIED, now / 1000);
                values.put(MediaColumns.IS_PENDING, 1);

                final Uri uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                try {
                    try (OutputStream out = cr.openOutputStream(uri)) {
                        source.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    }

                    // Everything went well above, publish it!
                    values.clear();
                    values.put(MediaColumns.IS_PENDING, 0);
                    cr.update(uri, values, null, null);
                    return uri.toString();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to insert image", e);
                    cr.delete(uri, null, null);
                    return null;
                }
            }

            /**
             * Get the content:// style URI for the image media table on the
             * given volume.
             *
             * @param volumeName the name of the volume to get the URI for
             * @return the URI to the image media table on the given volume
             */
            public static Uri getContentUri(String volumeName) {
                return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("images")
                        .appendPath("media").build();
            }

            /**
             * Get the content:// style URI for a single row in the images table
             * on the given volume.
             *
             * @param volumeName the name of the volume to get the URI for
             * @param id the image to get the URI for
             * @return the URI to the images table on the given volume
             */
            public static @NonNull Uri getContentUri(@NonNull String volumeName, long id) {
                return ContentUris.withAppendedId(getContentUri(volumeName), id);
            }

            /**
             * The content:// style URI for the internal storage.
             */
            public static final Uri INTERNAL_CONTENT_URI =
                    getContentUri("internal");

            /**
             * The content:// style URI for the "primary" external storage
             * volume.
             */
            public static final Uri EXTERNAL_CONTENT_URI =
                    getContentUri("external");

            /**
             * The MIME type of this directory of
             * images.  Note that each entry in this directory will have a standard
             * image MIME type as appropriate -- for example, image/jpeg.
             */
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/image";

            /**
             * The default sort order for this table
             */
            public static final String DEFAULT_SORT_ORDER = ImageColumns.BUCKET_DISPLAY_NAME;
        }

        /**
         * This class provides utility methods to obtain thumbnails for various
         * {@link Images} items.
         *
         * @deprecated Callers should migrate to using
         *             {@link ContentResolver#loadThumbnail}, since it offers
         *             richer control over requested thumbnail sizes and
         *             cancellation behavior.
         */
        @Deprecated
        public static class Thumbnails implements BaseColumns {
            /**
             * @deprecated all queries should be performed through
             *             {@link ContentResolver} directly, which offers modern
             *             features like {@link CancellationSignal}.
             */
            @Deprecated
            public static final Cursor query(ContentResolver cr, Uri uri, String[] projection) {
                return cr.query(uri, projection, null, null, DEFAULT_SORT_ORDER);
            }

            /**
             * @deprecated all queries should be performed through
             *             {@link ContentResolver} directly, which offers modern
             *             features like {@link CancellationSignal}.
             */
            @Deprecated
            public static final Cursor queryMiniThumbnails(ContentResolver cr, Uri uri, int kind,
                    String[] projection) {
                return cr.query(uri, projection, "kind = " + kind, null, DEFAULT_SORT_ORDER);
            }

            /**
             * @deprecated all queries should be performed through
             *             {@link ContentResolver} directly, which offers modern
             *             features like {@link CancellationSignal}.
             */
            @Deprecated
            public static final Cursor queryMiniThumbnail(ContentResolver cr, long origId, int kind,
                    String[] projection) {
                return cr.query(EXTERNAL_CONTENT_URI, projection,
                        IMAGE_ID + " = " + origId + " AND " + KIND + " = " +
                        kind, null, null);
            }

            /**
             * Cancel any outstanding {@link #getThumbnail} requests, causing
             * them to return by throwing a {@link OperationCanceledException}.
             * <p>
             * This method has no effect on
             * {@link ContentResolver#loadThumbnail} calls, since they provide
             * their own {@link CancellationSignal}.
             *
             * @deprecated Callers should migrate to using
             *             {@link ContentResolver#loadThumbnail}, since it
             *             offers richer control over requested thumbnail sizes
             *             and cancellation behavior.
             */
            @Deprecated
            public static void cancelThumbnailRequest(ContentResolver cr, long origId) {
                final Uri uri = ContentUris.withAppendedId(
                        Images.Media.EXTERNAL_CONTENT_URI, origId);
                InternalThumbnails.cancelThumbnail(cr, uri);
            }

            /**
             * Return thumbnail representing a specific image item. If a
             * thumbnail doesn't exist, this method will block until it's
             * generated. Callers are responsible for their own in-memory
             * caching of returned values.
             *
             * As of {@link android.os.Build.VERSION_CODES#Q}, this output
             * of the thumbnail has correct rotation, don't need to rotate
             * it again.
             *
             * @param imageId the image item to obtain a thumbnail for.
             * @param kind optimal thumbnail size desired.
             * @return decoded thumbnail, or {@code null} if problem was
             *         encountered.
             * @deprecated Callers should migrate to using
             *             {@link ContentResolver#loadThumbnail}, since it
             *             offers richer control over requested thumbnail sizes
             *             and cancellation behavior.
             */
            @Deprecated
            public static Bitmap getThumbnail(ContentResolver cr, long imageId, int kind,
                    BitmapFactory.Options options) {
                final Uri uri = ContentUris.withAppendedId(
                        Images.Media.EXTERNAL_CONTENT_URI, imageId);
                return InternalThumbnails.getThumbnail(cr, uri, kind, options);
            }

            /**
             * Cancel any outstanding {@link #getThumbnail} requests, causing
             * them to return by throwing a {@link OperationCanceledException}.
             * <p>
             * This method has no effect on
             * {@link ContentResolver#loadThumbnail} calls, since they provide
             * their own {@link CancellationSignal}.
             *
             * @deprecated Callers should migrate to using
             *             {@link ContentResolver#loadThumbnail}, since it
             *             offers richer control over requested thumbnail sizes
             *             and cancellation behavior.
             */
            @Deprecated
            public static void cancelThumbnailRequest(ContentResolver cr, long origId,
                    long groupId) {
                cancelThumbnailRequest(cr, origId);
            }

            /**
             * Return thumbnail representing a specific image item. If a
             * thumbnail doesn't exist, this method will block until it's
             * generated. Callers are responsible for their own in-memory
             * caching of returned values.
             *
             * As of {@link android.os.Build.VERSION_CODES#Q}, this output
             * of the thumbnail has correct rotation, don't need to rotate
             * it again.
             *
             * @param imageId the image item to obtain a thumbnail for.
             * @param kind optimal thumbnail size desired.
             * @return decoded thumbnail, or {@code null} if problem was
             *         encountered.
             * @deprecated Callers should migrate to using
             *             {@link ContentResolver#loadThumbnail}, since it
             *             offers richer control over requested thumbnail sizes
             *             and cancellation behavior.
             */
            @Deprecated
            public static Bitmap getThumbnail(ContentResolver cr, long imageId, long groupId,
                    int kind, BitmapFactory.Options options) {
                return getThumbnail(cr, imageId, kind, options);
            }

            /**
             * Get the content:// style URI for the image media table on the
             * given volume.
             *
             * @param volumeName the name of the volume to get the URI for
             * @return the URI to the image media table on the given volume
             */
            public static Uri getContentUri(String volumeName) {
                return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("images")
                        .appendPath("thumbnails").build();
            }

            /**
             * The content:// style URI for the internal storage.
             */
            public static final Uri INTERNAL_CONTENT_URI =
                    getContentUri("internal");

            /**
             * The content:// style URI for the "primary" external storage
             * volume.
             */
            public static final Uri EXTERNAL_CONTENT_URI =
                    getContentUri("external");

            /**
             * The default sort order for this table
             */
            public static final String DEFAULT_SORT_ORDER = "image_id ASC";

            /**
             * Path to the thumbnail file on disk.
             *
             * As of {@link android.os.Build.VERSION_CODES#Q}, this thumbnail
             * has correct rotation, don't need to rotate it again.
             */
            @Column(Cursor.FIELD_TYPE_STRING)
            public static final String DATA = "_data";

            /**
             * The original image for the thumbnal
             */
            @Column(Cursor.FIELD_TYPE_INTEGER)
            public static final String IMAGE_ID = "image_id";

            /**
             * The kind of the thumbnail
             */
            @Column(Cursor.FIELD_TYPE_INTEGER)
            public static final String KIND = "kind";

            public static final int MINI_KIND = ThumbnailConstants.MINI_KIND;
            public static final int FULL_SCREEN_KIND = ThumbnailConstants.FULL_SCREEN_KIND;
            public static final int MICRO_KIND = ThumbnailConstants.MICRO_KIND;

            /**
             * Return the typical {@link Size} (in pixels) used internally when
             * the given thumbnail kind is requested.
             *
             * @deprecated Callers should migrate to using
             *             {@link ContentResolver#loadThumbnail}, since it
             *             offers richer control over requested thumbnail sizes
             *             and cancellation behavior.
             */
            @Deprecated
            public static @NonNull Size getKindSize(int kind) {
                return ThumbnailConstants.getKindSize(kind);
            }

            /**
             * The blob raw data of thumbnail
             *
             * @deprecated this column never existed internally, and could never
             *             have returned valid data.
             */
            @Deprecated
            @Column(Cursor.FIELD_TYPE_BLOB)
            public static final String THUMB_DATA = "thumb_data";

            /**
             * The width of the thumbnal
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String WIDTH = "width";

            /**
             * The height of the thumbnail
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String HEIGHT = "height";
        }
    }

    /**
     * Collection of all media with MIME type of {@code audio/*}.
     */
    public static final class Audio {
        /**
         * Audio metadata columns.
         */
        public interface AudioColumns extends MediaColumns {

            /**
             * A non human readable key calculated from the TITLE, used for
             * searching, sorting and grouping
             *
             * @see Audio#keyFor(String)
             * @deprecated These keys are generated using
             *             {@link java.util.Locale#ROOT}, which means they don't
             *             reflect locale-specific sorting preferences. To apply
             *             locale-specific sorting preferences, use
             *             {@link ContentResolver#QUERY_ARG_SQL_SORT_ORDER} with
             *             {@code COLLATE LOCALIZED}, or
             *             {@link ContentResolver#QUERY_ARG_SORT_LOCALE}.
             */
            @Deprecated
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String TITLE_KEY = "title_key";

            /** @removed promoted to parent interface */
            public static final String DURATION = "duration";

            /**
             * The position within the audio item at which playback should be
             * resumed.
             */
            @DurationMillisLong
            @Column(Cursor.FIELD_TYPE_INTEGER)
            public static final String BOOKMARK = "bookmark";

            /**
             * The id of the artist who created the audio file, if any
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String ARTIST_ID = "artist_id";

            /** @removed promoted to parent interface */
            public static final String ARTIST = "artist";

            /**
             * The artist credited for the album that contains the audio file
             * @hide
             */
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String ALBUM_ARTIST = "album_artist";

            /**
             * A non human readable key calculated from the ARTIST, used for
             * searching, sorting and grouping
             *
             * @see Audio#keyFor(String)
             * @deprecated These keys are generated using
             *             {@link java.util.Locale#ROOT}, which means they don't
             *             reflect locale-specific sorting preferences. To apply
             *             locale-specific sorting preferences, use
             *             {@link ContentResolver#QUERY_ARG_SQL_SORT_ORDER} with
             *             {@code COLLATE LOCALIZED}, or
             *             {@link ContentResolver#QUERY_ARG_SORT_LOCALE}.
             */
            @Deprecated
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String ARTIST_KEY = "artist_key";

            /** @removed promoted to parent interface */
            public static final String COMPOSER = "composer";

            /**
             * The id of the album the audio file is from, if any
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String ALBUM_ID = "album_id";

            /** @removed promoted to parent interface */
            public static final String ALBUM = "album";

            /**
             * A non human readable key calculated from the ALBUM, used for
             * searching, sorting and grouping
             *
             * @see Audio#keyFor(String)
             * @deprecated These keys are generated using
             *             {@link java.util.Locale#ROOT}, which means they don't
             *             reflect locale-specific sorting preferences. To apply
             *             locale-specific sorting preferences, use
             *             {@link ContentResolver#QUERY_ARG_SQL_SORT_ORDER} with
             *             {@code COLLATE LOCALIZED}, or
             *             {@link ContentResolver#QUERY_ARG_SORT_LOCALE}.
             */
            @Deprecated
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String ALBUM_KEY = "album_key";

            /**
             * The track number of this song on the album, if any.
             * This number encodes both the track number and the
             * disc number. For multi-disc sets, this number will
             * be 1xxx for tracks on the first disc, 2xxx for tracks
             * on the second disc, etc.
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String TRACK = "track";

            /**
             * The year the audio file was recorded, if any
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String YEAR = "year";

            /**
             * Non-zero if the audio file is music
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String IS_MUSIC = "is_music";

            /**
             * Non-zero if the audio file is a podcast
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String IS_PODCAST = "is_podcast";

            /**
             * Non-zero if the audio file may be a ringtone
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String IS_RINGTONE = "is_ringtone";

            /**
             * Non-zero if the audio file may be an alarm
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String IS_ALARM = "is_alarm";

            /**
             * Non-zero if the audio file may be a notification sound
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String IS_NOTIFICATION = "is_notification";

            /**
             * Non-zero if the audio file is an audiobook
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String IS_AUDIOBOOK = "is_audiobook";

            /**
             * Non-zero if the audio file is a voice recording recorded
             * by voice recorder apps
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String IS_RECORDING = "is_recording";

            /**
             * The id of the genre the audio file is from, if any
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String GENRE_ID = "genre_id";

            /**
             * The genre of the audio file, if any.
             */
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String GENRE = "genre";

            /**
             * A non human readable key calculated from the GENRE, used for
             * searching, sorting and grouping
             *
             * @see Audio#keyFor(String)
             * @deprecated These keys are generated using
             *             {@link java.util.Locale#ROOT}, which means they don't
             *             reflect locale-specific sorting preferences. To apply
             *             locale-specific sorting preferences, use
             *             {@link ContentResolver#QUERY_ARG_SQL_SORT_ORDER} with
             *             {@code COLLATE LOCALIZED}, or
             *             {@link ContentResolver#QUERY_ARG_SORT_LOCALE}.
             */
            @Deprecated
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String GENRE_KEY = "genre_key";

            /**
             * The resource URI of a localized title, if any.
             * <p>
             * Conforms to this pattern:
             * <ul>
             * <li>Scheme: {@link ContentResolver#SCHEME_ANDROID_RESOURCE}
             * <li>Authority: Package Name of ringtone title provider
             * <li>First Path Segment: Type of resource (must be "string")
             * <li>Second Path Segment: Resource ID of title
             * </ul>
             */
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String TITLE_RESOURCE_URI = "title_resource_uri";
        }

        private static final Pattern PATTERN_TRIM_BEFORE = Pattern.compile(
                "(?i)(^(the|an|a) |,\\s*(the|an|a)$|[^\\w\\s]|^\\s+|\\s+$)");
        private static final Pattern PATTERN_TRIM_AFTER = Pattern.compile(
                "(^(00)+|(00)+$)");

        /**
         * Converts a user-visible string into a "key" that can be used for
         * grouping, sorting, and searching.
         *
         * @return Opaque token that should not be parsed or displayed to users.
         * @deprecated These keys are generated using
         *             {@link java.util.Locale#ROOT}, which means they don't
         *             reflect locale-specific sorting preferences. To apply
         *             locale-specific sorting preferences, use
         *             {@link ContentResolver#QUERY_ARG_SQL_SORT_ORDER} with
         *             {@code COLLATE LOCALIZED}, or
         *             {@link ContentResolver#QUERY_ARG_SORT_LOCALE}.
         */
        @Deprecated
        public static @Nullable String keyFor(@Nullable String name) {
            if (TextUtils.isEmpty(name)) return "";

            if (UNKNOWN_STRING.equals(name)) {
                return "01";
            }

            final boolean sortFirst = name.startsWith("\001");

            name = PATTERN_TRIM_BEFORE.matcher(name).replaceAll("");
            if (TextUtils.isEmpty(name)) return "";

            final Collator c = Collator.getInstance(Locale.ROOT);
            c.setStrength(Collator.PRIMARY);
            name = encodeToString(c.getCollationKey(name).toByteArray());

            name = PATTERN_TRIM_AFTER.matcher(name).replaceAll("");
            if (sortFirst) {
                name = "01" + name;
            }
            return name;
        }

        private static String encodeToString(byte[] bytes) {
            final StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }

        public static final class Media implements AudioColumns {
            /**
             * Get the content:// style URI for the audio media table on the
             * given volume.
             *
             * @param volumeName the name of the volume to get the URI for
             * @return the URI to the audio media table on the given volume
             */
            public static Uri getContentUri(String volumeName) {
                return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("audio")
                        .appendPath("media").build();
            }

            /**
             * Get the content:// style URI for a single row in the audio table
             * on the given volume.
             *
             * @param volumeName the name of the volume to get the URI for
             * @param id the audio to get the URI for
             * @return the URI to the audio table on the given volume
             */
            public static @NonNull Uri getContentUri(@NonNull String volumeName, long id) {
                return ContentUris.withAppendedId(getContentUri(volumeName), id);
            }

            /**
             * Get the content:// style URI for the given audio media file.
             *
             * @deprecated Apps may not have filesystem permissions to directly
             *             access this path.
             */
            @Deprecated
            public static @Nullable Uri getContentUriForPath(@NonNull String path) {
                return getContentUri(getVolumeName(new File(path)));
            }

            /**
             * The content:// style URI for the internal storage.
             */
            public static final Uri INTERNAL_CONTENT_URI =
                    getContentUri("internal");

            /**
             * The content:// style URI for the "primary" external storage
             * volume.
             */
            public static final Uri EXTERNAL_CONTENT_URI =
                    getContentUri("external");

            /**
             * The MIME type for this table.
             */
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/audio";

            /**
             * The MIME type for an audio track.
             */
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/audio";

            /**
             * The default sort order for this table
             */
            public static final String DEFAULT_SORT_ORDER = TITLE_KEY;

            /**
             * Activity Action: Start SoundRecorder application.
             * <p>Input: nothing.
             * <p>Output: An uri to the recorded sound stored in the Media Library
             * if the recording was successful.
             * May also contain the extra EXTRA_MAX_BYTES.
             * @see #EXTRA_MAX_BYTES
             */
            @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
            public static final String RECORD_SOUND_ACTION =
                    "android.provider.MediaStore.RECORD_SOUND";

            /**
             * The name of the Intent-extra used to define a maximum file size for
             * a recording made by the SoundRecorder application.
             *
             * @see #RECORD_SOUND_ACTION
             */
             public static final String EXTRA_MAX_BYTES =
                    "android.provider.MediaStore.extra.MAX_BYTES";
        }

        /**
         * Audio genre metadata columns.
         */
        public interface GenresColumns {
            /**
             * The name of the genre
             */
            @Column(Cursor.FIELD_TYPE_STRING)
            public static final String NAME = "name";
        }

        /**
         * Contains all genres for audio files
         */
        public static final class Genres implements BaseColumns, GenresColumns {
            /**
             * Get the content:// style URI for the audio genres table on the
             * given volume.
             *
             * @param volumeName the name of the volume to get the URI for
             * @return the URI to the audio genres table on the given volume
             */
            public static Uri getContentUri(String volumeName) {
                return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("audio")
                        .appendPath("genres").build();
            }

            /**
             * Get the content:// style URI for querying the genres of an audio file.
             *
             * @param volumeName the name of the volume to get the URI for
             * @param audioId the ID of the audio file for which to retrieve the genres
             * @return the URI to for querying the genres for the audio file
             * with the given the volume and audioID
             */
            public static Uri getContentUriForAudioId(String volumeName, int audioId) {
                return ContentUris.withAppendedId(Audio.Media.getContentUri(volumeName), audioId)
                        .buildUpon().appendPath("genres").build();
            }

            /**
             * The content:// style URI for the internal storage.
             */
            public static final Uri INTERNAL_CONTENT_URI =
                    getContentUri("internal");

            /**
             * The content:// style URI for the "primary" external storage
             * volume.
             */
            public static final Uri EXTERNAL_CONTENT_URI =
                    getContentUri("external");

            /**
             * The MIME type for this table.
             */
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/genre";

            /**
             * The MIME type for entries in this table.
             */
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/genre";

            /**
             * The default sort order for this table
             */
            public static final String DEFAULT_SORT_ORDER = NAME;

            /**
             * Sub-directory of each genre containing all members.
             */
            public static final class Members implements AudioColumns {

                public static final Uri getContentUri(String volumeName, long genreId) {
                    return ContentUris
                            .withAppendedId(Audio.Genres.getContentUri(volumeName), genreId)
                            .buildUpon().appendPath("members").build();
                }

                /**
                 * A subdirectory of each genre containing all member audio files.
                 */
                public static final String CONTENT_DIRECTORY = "members";

                /**
                 * The default sort order for this table
                 */
                public static final String DEFAULT_SORT_ORDER = TITLE_KEY;

                /**
                 * The ID of the audio file
                 */
                @Column(Cursor.FIELD_TYPE_INTEGER)
                public static final String AUDIO_ID = "audio_id";

                /**
                 * The ID of the genre
                 */
                @Column(Cursor.FIELD_TYPE_INTEGER)
                public static final String GENRE_ID = "genre_id";
            }
        }

        /**
         * Audio playlist metadata columns.
         *
         * @deprecated Android playlists are now deprecated. We will keep the current
         *             functionality for compatibility reasons, but we will no longer take
         *             feature request. We do not advise adding new usages of Android Playlists.
         *             M3U files can be used as an alternative.
         */
        @Deprecated
        public interface PlaylistsColumns extends MediaColumns {
            /**
             * The name of the playlist
             */
            @Column(Cursor.FIELD_TYPE_STRING)
            public static final String NAME = "name";

            /**
             * Path to the playlist file on disk.
             */
            @Column(Cursor.FIELD_TYPE_STRING)
            public static final String DATA = "_data";

            /**
             * The time the media item was first added.
             */
            @CurrentTimeSecondsLong
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String DATE_ADDED = "date_added";

            /**
             * The time the media item was last modified.
             */
            @CurrentTimeSecondsLong
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String DATE_MODIFIED = "date_modified";
        }

        /**
         * Contains playlists for audio files
         *
         * @deprecated Android playlists are now deprecated. We will keep the current
         *             functionality for compatibility resons, but we will no longer take
         *             feature request. We do not advise adding new usages of Android Playlists.
         *             M3U files can be used as an alternative.
         */
        @Deprecated
        public static final class Playlists implements BaseColumns,
                PlaylistsColumns {
            /**
             * Get the content:// style URI for the audio playlists table on the
             * given volume.
             *
             * @param volumeName the name of the volume to get the URI for
             * @return the URI to the audio playlists table on the given volume
             */
            public static Uri getContentUri(String volumeName) {
                return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("audio")
                        .appendPath("playlists").build();
            }

            /**
             * The content:// style URI for the internal storage.
             */
            public static final Uri INTERNAL_CONTENT_URI =
                    getContentUri("internal");

            /**
             * The content:// style URI for the "primary" external storage
             * volume.
             */
            public static final Uri EXTERNAL_CONTENT_URI =
                    getContentUri("external");

            /**
             * The MIME type for this table.
             */
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/playlist";

            /**
             * The MIME type for entries in this table.
             */
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/playlist";

            /**
             * The default sort order for this table
             */
            public static final String DEFAULT_SORT_ORDER = NAME;

            /**
             * Sub-directory of each playlist containing all members.
             */
            public static final class Members implements AudioColumns {
                public static final Uri getContentUri(String volumeName, long playlistId) {
                    return ContentUris
                            .withAppendedId(Audio.Playlists.getContentUri(volumeName), playlistId)
                            .buildUpon().appendPath("members").build();
                }

                /**
                 * Convenience method to move a playlist item to a new location
                 * @param res The content resolver to use
                 * @param playlistId The numeric id of the playlist
                 * @param from The position of the item to move
                 * @param to The position to move the item to
                 * @return true on success
                 */
                public static final boolean moveItem(ContentResolver res,
                        long playlistId, int from, int to) {
                    Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external",
                            playlistId)
                            .buildUpon()
                            .appendEncodedPath(String.valueOf(from))
                            .appendQueryParameter("move", "true")
                            .build();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, to);
                    return res.update(uri, values, null, null) != 0;
                }

                /**
                 * The ID within the playlist.
                 */
                @Column(Cursor.FIELD_TYPE_INTEGER)
                public static final String _ID = "_id";

                /**
                 * A subdirectory of each playlist containing all member audio
                 * files.
                 */
                public static final String CONTENT_DIRECTORY = "members";

                /**
                 * The ID of the audio file
                 */
                @Column(Cursor.FIELD_TYPE_INTEGER)
                public static final String AUDIO_ID = "audio_id";

                /**
                 * The ID of the playlist
                 */
                @Column(Cursor.FIELD_TYPE_INTEGER)
                public static final String PLAYLIST_ID = "playlist_id";

                /**
                 * The order of the songs in the playlist
                 */
                @Column(Cursor.FIELD_TYPE_INTEGER)
                public static final String PLAY_ORDER = "play_order";

                /**
                 * The default sort order for this table
                 */
                public static final String DEFAULT_SORT_ORDER = PLAY_ORDER;
            }
        }

        /**
         * Audio artist metadata columns.
         */
        public interface ArtistColumns {
            /**
             * The artist who created the audio file, if any
             */
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String ARTIST = "artist";

            /**
             * A non human readable key calculated from the ARTIST, used for
             * searching, sorting and grouping
             *
             * @see Audio#keyFor(String)
             * @deprecated These keys are generated using
             *             {@link java.util.Locale#ROOT}, which means they don't
             *             reflect locale-specific sorting preferences. To apply
             *             locale-specific sorting preferences, use
             *             {@link ContentResolver#QUERY_ARG_SQL_SORT_ORDER} with
             *             {@code COLLATE LOCALIZED}, or
             *             {@link ContentResolver#QUERY_ARG_SORT_LOCALE}.
             */
            @Deprecated
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String ARTIST_KEY = "artist_key";

            /**
             * The number of albums in the database for this artist
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String NUMBER_OF_ALBUMS = "number_of_albums";

            /**
             * The number of albums in the database for this artist
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String NUMBER_OF_TRACKS = "number_of_tracks";
        }

        /**
         * Contains artists for audio files
         */
        public static final class Artists implements BaseColumns, ArtistColumns {
            /**
             * Get the content:// style URI for the artists table on the
             * given volume.
             *
             * @param volumeName the name of the volume to get the URI for
             * @return the URI to the audio artists table on the given volume
             */
            public static Uri getContentUri(String volumeName) {
                return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("audio")
                        .appendPath("artists").build();
            }

            /**
             * The content:// style URI for the internal storage.
             */
            public static final Uri INTERNAL_CONTENT_URI =
                    getContentUri("internal");

            /**
             * The content:// style URI for the "primary" external storage
             * volume.
             */
            public static final Uri EXTERNAL_CONTENT_URI =
                    getContentUri("external");

            /**
             * The MIME type for this table.
             */
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/artists";

            /**
             * The MIME type for entries in this table.
             */
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/artist";

            /**
             * The default sort order for this table
             */
            public static final String DEFAULT_SORT_ORDER = ARTIST_KEY;

            /**
             * Sub-directory of each artist containing all albums on which
             * a song by the artist appears.
             */
            public static final class Albums implements BaseColumns, AlbumColumns {
                public static final Uri getContentUri(String volumeName,long artistId) {
                    return ContentUris
                            .withAppendedId(Audio.Artists.getContentUri(volumeName), artistId)
                            .buildUpon().appendPath("albums").build();
                }
            }
        }

        /**
         * Audio album metadata columns.
         */
        public interface AlbumColumns {

            /**
             * The id for the album
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String ALBUM_ID = "album_id";

            /**
             * The album on which the audio file appears, if any
             */
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String ALBUM = "album";

            /**
             * The ID of the artist whose songs appear on this album.
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String ARTIST_ID = "artist_id";

            /**
             * The name of the artist whose songs appear on this album.
             */
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String ARTIST = "artist";

            /**
             * A non human readable key calculated from the ARTIST, used for
             * searching, sorting and grouping
             *
             * @see Audio#keyFor(String)
             * @deprecated These keys are generated using
             *             {@link java.util.Locale#ROOT}, which means they don't
             *             reflect locale-specific sorting preferences. To apply
             *             locale-specific sorting preferences, use
             *             {@link ContentResolver#QUERY_ARG_SQL_SORT_ORDER} with
             *             {@code COLLATE LOCALIZED}, or
             *             {@link ContentResolver#QUERY_ARG_SORT_LOCALE}.
             */
            @Deprecated
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String ARTIST_KEY = "artist_key";

            /**
             * The number of songs on this album
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String NUMBER_OF_SONGS = "numsongs";

            /**
             * This column is available when getting album info via artist,
             * and indicates the number of songs on the album by the given
             * artist.
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String NUMBER_OF_SONGS_FOR_ARTIST = "numsongs_by_artist";

            /**
             * The year in which the earliest songs
             * on this album were released. This will often
             * be the same as {@link #LAST_YEAR}, but for compilation albums
             * they might differ.
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String FIRST_YEAR = "minyear";

            /**
             * The year in which the latest songs
             * on this album were released. This will often
             * be the same as {@link #FIRST_YEAR}, but for compilation albums
             * they might differ.
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String LAST_YEAR = "maxyear";

            /**
             * A non human readable key calculated from the ALBUM, used for
             * searching, sorting and grouping
             *
             * @see Audio#keyFor(String)
             * @deprecated These keys are generated using
             *             {@link java.util.Locale#ROOT}, which means they don't
             *             reflect locale-specific sorting preferences. To apply
             *             locale-specific sorting preferences, use
             *             {@link ContentResolver#QUERY_ARG_SQL_SORT_ORDER} with
             *             {@code COLLATE LOCALIZED}, or
             *             {@link ContentResolver#QUERY_ARG_SORT_LOCALE}.
             */
            @Deprecated
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String ALBUM_KEY = "album_key";

            /**
             * Cached album art.
             *
             * @deprecated Apps may not have filesystem permissions to directly
             *             access this path. Instead of trying to open this path
             *             directly, apps should use
             *             {@link ContentResolver#loadThumbnail}
             *             to gain access.
             */
            @Deprecated
            @Column(Cursor.FIELD_TYPE_STRING)
            public static final String ALBUM_ART = "album_art";
        }

        /**
         * Contains artists for audio files
         */
        public static final class Albums implements BaseColumns, AlbumColumns {
            /**
             * Get the content:// style URI for the albums table on the
             * given volume.
             *
             * @param volumeName the name of the volume to get the URI for
             * @return the URI to the audio albums table on the given volume
             */
            public static Uri getContentUri(String volumeName) {
                return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("audio")
                        .appendPath("albums").build();
            }

            /**
             * The content:// style URI for the internal storage.
             */
            public static final Uri INTERNAL_CONTENT_URI =
                    getContentUri("internal");

            /**
             * The content:// style URI for the "primary" external storage
             * volume.
             */
            public static final Uri EXTERNAL_CONTENT_URI =
                    getContentUri("external");

            /**
             * The MIME type for this table.
             */
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/albums";

            /**
             * The MIME type for entries in this table.
             */
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/album";

            /**
             * The default sort order for this table
             */
            public static final String DEFAULT_SORT_ORDER = ALBUM_KEY;
        }

        public static final class Radio {
            /**
             * The MIME type for entries in this table.
             */
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/radio";

            // Not instantiable.
            private Radio() { }
        }

        /**
         * This class provides utility methods to obtain thumbnails for various
         * {@link Audio} items.
         *
         * @deprecated Callers should migrate to using
         *             {@link ContentResolver#loadThumbnail}, since it offers
         *             richer control over requested thumbnail sizes and
         *             cancellation behavior.
         * @hide
         */
        @Deprecated
        public static class Thumbnails implements BaseColumns {
            /**
             * Path to the thumbnail file on disk.
             */
            @Column(Cursor.FIELD_TYPE_STRING)
            public static final String DATA = "_data";

            @Column(Cursor.FIELD_TYPE_INTEGER)
            public static final String ALBUM_ID = "album_id";
        }
    }

    /**
     * Collection of all media with MIME type of {@code video/*}.
     */
    public static final class Video {

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = MediaColumns.DISPLAY_NAME;

        /**
         * @deprecated all queries should be performed through
         *             {@link ContentResolver} directly, which offers modern
         *             features like {@link CancellationSignal}.
         */
        @Deprecated
        public static final Cursor query(ContentResolver cr, Uri uri, String[] projection) {
            return cr.query(uri, projection, null, null, DEFAULT_SORT_ORDER);
        }

        /**
         * Video metadata columns.
         */
        public interface VideoColumns extends MediaColumns {
            /** @removed promoted to parent interface */
            public static final String DURATION = "duration";
            /** @removed promoted to parent interface */
            public static final String ARTIST = "artist";
            /** @removed promoted to parent interface */
            public static final String ALBUM = "album";
            /** @removed promoted to parent interface */
            public static final String RESOLUTION = "resolution";

            /**
             * The description of the video recording
             */
            @Column(value = Cursor.FIELD_TYPE_STRING, readOnly = true)
            public static final String DESCRIPTION = "description";

            /**
             * Whether the video should be published as public or private
             */
            @Column(Cursor.FIELD_TYPE_INTEGER)
            public static final String IS_PRIVATE = "isprivate";

            /**
             * The user-added tags associated with a video
             */
            @Column(Cursor.FIELD_TYPE_STRING)
            public static final String TAGS = "tags";

            /**
             * The YouTube category of the video
             */
            @Column(Cursor.FIELD_TYPE_STRING)
            public static final String CATEGORY = "category";

            /**
             * The language of the video
             */
            @Column(Cursor.FIELD_TYPE_STRING)
            public static final String LANGUAGE = "language";

            /**
             * The latitude where the video was captured.
             *
             * @deprecated location details are no longer indexed for privacy
             *             reasons, and this value is now always {@code null}.
             *             You can still manually obtain location metadata using
             *             {@link MediaMetadataRetriever#METADATA_KEY_LOCATION}.
             */
            @Deprecated
            @Column(value = Cursor.FIELD_TYPE_FLOAT, readOnly = true)
            public static final String LATITUDE = "latitude";

            /**
             * The longitude where the video was captured.
             *
             * @deprecated location details are no longer indexed for privacy
             *             reasons, and this value is now always {@code null}.
             *             You can still manually obtain location metadata using
             *             {@link MediaMetadataRetriever#METADATA_KEY_LOCATION}.
             */
            @Deprecated
            @Column(value = Cursor.FIELD_TYPE_FLOAT, readOnly = true)
            public static final String LONGITUDE = "longitude";

            /** @removed promoted to parent interface */
            public static final String DATE_TAKEN = "datetaken";

            /**
             * The mini thumb id.
             *
             * @deprecated all thumbnails should be obtained via
             *             {@link MediaStore.Images.Thumbnails#getThumbnail}, as this
             *             value is no longer supported.
             */
            @Deprecated
            @Column(Cursor.FIELD_TYPE_INTEGER)
            public static final String MINI_THUMB_MAGIC = "mini_thumb_magic";

            /** @removed promoted to parent interface */
            public static final String BUCKET_ID = "bucket_id";
            /** @removed promoted to parent interface */
            public static final String BUCKET_DISPLAY_NAME = "bucket_display_name";
            /** @removed promoted to parent interface */
            public static final String GROUP_ID = "group_id";

            /**
             * The position within the video item at which playback should be
             * resumed.
             */
            @DurationMillisLong
            @Column(Cursor.FIELD_TYPE_INTEGER)
            public static final String BOOKMARK = "bookmark";

            /**
             * The color standard of this media file, if available.
             *
             * @see MediaFormat#COLOR_STANDARD_BT709
             * @see MediaFormat#COLOR_STANDARD_BT601_PAL
             * @see MediaFormat#COLOR_STANDARD_BT601_NTSC
             * @see MediaFormat#COLOR_STANDARD_BT2020
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String COLOR_STANDARD = "color_standard";

            /**
             * The color transfer of this media file, if available.
             *
             * @see MediaFormat#COLOR_TRANSFER_LINEAR
             * @see MediaFormat#COLOR_TRANSFER_SDR_VIDEO
             * @see MediaFormat#COLOR_TRANSFER_ST2084
             * @see MediaFormat#COLOR_TRANSFER_HLG
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String COLOR_TRANSFER = "color_transfer";

            /**
             * The color range of this media file, if available.
             *
             * @see MediaFormat#COLOR_RANGE_LIMITED
             * @see MediaFormat#COLOR_RANGE_FULL
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String COLOR_RANGE = "color_range";
        }

        public static final class Media implements VideoColumns {
            /**
             * Get the content:// style URI for the video media table on the
             * given volume.
             *
             * @param volumeName the name of the volume to get the URI for
             * @return the URI to the video media table on the given volume
             */
            public static Uri getContentUri(String volumeName) {
                return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("video")
                        .appendPath("media").build();
            }

            /**
             * Get the content:// style URI for a single row in the videos table
             * on the given volume.
             *
             * @param volumeName the name of the volume to get the URI for
             * @param id the video to get the URI for
             * @return the URI to the videos table on the given volume
             */
            public static @NonNull Uri getContentUri(@NonNull String volumeName, long id) {
                return ContentUris.withAppendedId(getContentUri(volumeName), id);
            }

            /**
             * The content:// style URI for the internal storage.
             */
            public static final Uri INTERNAL_CONTENT_URI =
                    getContentUri("internal");

            /**
             * The content:// style URI for the "primary" external storage
             * volume.
             */
            public static final Uri EXTERNAL_CONTENT_URI =
                    getContentUri("external");

            /**
             * The MIME type for this table.
             */
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/video";

            /**
             * The default sort order for this table
             */
            public static final String DEFAULT_SORT_ORDER = TITLE;
        }

        /**
         * This class provides utility methods to obtain thumbnails for various
         * {@link Video} items.
         *
         * @deprecated Callers should migrate to using
         *             {@link ContentResolver#loadThumbnail}, since it offers
         *             richer control over requested thumbnail sizes and
         *             cancellation behavior.
         */
        @Deprecated
        public static class Thumbnails implements BaseColumns {
            /**
             * Cancel any outstanding {@link #getThumbnail} requests, causing
             * them to return by throwing a {@link OperationCanceledException}.
             * <p>
             * This method has no effect on
             * {@link ContentResolver#loadThumbnail} calls, since they provide
             * their own {@link CancellationSignal}.
             *
             * @deprecated Callers should migrate to using
             *             {@link ContentResolver#loadThumbnail}, since it
             *             offers richer control over requested thumbnail sizes
             *             and cancellation behavior.
             */
            @Deprecated
            public static void cancelThumbnailRequest(ContentResolver cr, long origId) {
                final Uri uri = ContentUris.withAppendedId(
                        Video.Media.EXTERNAL_CONTENT_URI, origId);
                InternalThumbnails.cancelThumbnail(cr, uri);
            }

            /**
             * Return thumbnail representing a specific video item. If a
             * thumbnail doesn't exist, this method will block until it's
             * generated. Callers are responsible for their own in-memory
             * caching of returned values.
             *
             * @param videoId the video item to obtain a thumbnail for.
             * @param kind optimal thumbnail size desired.
             * @return decoded thumbnail, or {@code null} if problem was
             *         encountered.
             * @deprecated Callers should migrate to using
             *             {@link ContentResolver#loadThumbnail}, since it
             *             offers richer control over requested thumbnail sizes
             *             and cancellation behavior.
             */
            @Deprecated
            public static Bitmap getThumbnail(ContentResolver cr, long videoId, int kind,
                    BitmapFactory.Options options) {
                final Uri uri = ContentUris.withAppendedId(
                        Video.Media.EXTERNAL_CONTENT_URI, videoId);
                return InternalThumbnails.getThumbnail(cr, uri, kind, options);
            }

            /**
             * Cancel any outstanding {@link #getThumbnail} requests, causing
             * them to return by throwing a {@link OperationCanceledException}.
             * <p>
             * This method has no effect on
             * {@link ContentResolver#loadThumbnail} calls, since they provide
             * their own {@link CancellationSignal}.
             *
             * @deprecated Callers should migrate to using
             *             {@link ContentResolver#loadThumbnail}, since it
             *             offers richer control over requested thumbnail sizes
             *             and cancellation behavior.
             */
            @Deprecated
            public static void cancelThumbnailRequest(ContentResolver cr, long videoId,
                    long groupId) {
                cancelThumbnailRequest(cr, videoId);
            }

            /**
             * Return thumbnail representing a specific video item. If a
             * thumbnail doesn't exist, this method will block until it's
             * generated. Callers are responsible for their own in-memory
             * caching of returned values.
             *
             * @param videoId the video item to obtain a thumbnail for.
             * @param kind optimal thumbnail size desired.
             * @return decoded thumbnail, or {@code null} if problem was
             *         encountered.
             * @deprecated Callers should migrate to using
             *             {@link ContentResolver#loadThumbnail}, since it
             *             offers richer control over requested thumbnail sizes
             *             and cancellation behavior.
             */
            @Deprecated
            public static Bitmap getThumbnail(ContentResolver cr, long videoId, long groupId,
                    int kind, BitmapFactory.Options options) {
                return getThumbnail(cr, videoId, kind, options);
            }

            /**
             * Get the content:// style URI for the image media table on the
             * given volume.
             *
             * @param volumeName the name of the volume to get the URI for
             * @return the URI to the image media table on the given volume
             */
            public static Uri getContentUri(String volumeName) {
                return AUTHORITY_URI.buildUpon().appendPath(volumeName).appendPath("video")
                        .appendPath("thumbnails").build();
            }

            /**
             * The content:// style URI for the internal storage.
             */
            public static final Uri INTERNAL_CONTENT_URI =
                    getContentUri("internal");

            /**
             * The content:// style URI for the "primary" external storage
             * volume.
             */
            public static final Uri EXTERNAL_CONTENT_URI =
                    getContentUri("external");

            /**
             * The default sort order for this table
             */
            public static final String DEFAULT_SORT_ORDER = "video_id ASC";

            /**
             * Path to the thumbnail file on disk.
             */
            @Column(Cursor.FIELD_TYPE_STRING)
            public static final String DATA = "_data";

            /**
             * The original image for the thumbnal
             */
            @Column(Cursor.FIELD_TYPE_INTEGER)
            public static final String VIDEO_ID = "video_id";

            /**
             * The kind of the thumbnail
             */
            @Column(Cursor.FIELD_TYPE_INTEGER)
            public static final String KIND = "kind";

            public static final int MINI_KIND = ThumbnailConstants.MINI_KIND;
            public static final int FULL_SCREEN_KIND = ThumbnailConstants.FULL_SCREEN_KIND;
            public static final int MICRO_KIND = ThumbnailConstants.MICRO_KIND;

            /**
             * Return the typical {@link Size} (in pixels) used internally when
             * the given thumbnail kind is requested.
             *
             * @deprecated Callers should migrate to using
             *             {@link ContentResolver#loadThumbnail}, since it
             *             offers richer control over requested thumbnail sizes
             *             and cancellation behavior.
             */
            @Deprecated
            public static @NonNull Size getKindSize(int kind) {
                return ThumbnailConstants.getKindSize(kind);
            }

            /**
             * The width of the thumbnal
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String WIDTH = "width";

            /**
             * The height of the thumbnail
             */
            @Column(value = Cursor.FIELD_TYPE_INTEGER, readOnly = true)
            public static final String HEIGHT = "height";
        }
    }

    /**
     * Return list of all specific volume names that make up
     * {@link #VOLUME_EXTERNAL}. This includes a unique volume name for each
     * shared storage device that is currently attached, which typically
     * includes {@link MediaStore#VOLUME_EXTERNAL_PRIMARY}.
     * <p>
     * Each specific volume name can be passed to APIs like
     * {@link MediaStore.Images.Media#getContentUri(String)} to interact with
     * media on that storage device.
     */
    public static @NonNull Set<String> getExternalVolumeNames(@NonNull Context context) {
        final StorageManager sm = context.getSystemService(StorageManager.class);
        final Set<String> res = new ArraySet<>();
        for (StorageVolume sv : sm.getStorageVolumes()) {
            Log.v(TAG, "Examining volume " + sv.getId() + " with name "
                    + sv.getMediaStoreVolumeName() + " and state " + sv.getState());
            switch (sv.getState()) {
                case Environment.MEDIA_MOUNTED:
                case Environment.MEDIA_MOUNTED_READ_ONLY: {
                    final String volumeName = sv.getMediaStoreVolumeName();
                    if (volumeName != null) {
                        res.add(volumeName);
                    }
                    break;
                }
            }
        }
        return res;
    }

    /**
     * Return list of all recent volume names that have been part of
     * {@link #VOLUME_EXTERNAL}.
     * <p>
     * These volume names are not currently mounted, but they're likely to
     * reappear in the future, so apps are encouraged to preserve any indexed
     * metadata related to these volumes to optimize user experiences.
     * <p>
     * Each specific volume name can be passed to APIs like
     * {@link MediaStore.Images.Media#getContentUri(String)} to interact with
     * media on that storage device.
     */
    public static @NonNull Set<String> getRecentExternalVolumeNames(@NonNull Context context) {
        final StorageManager sm = context.getSystemService(StorageManager.class);
        final Set<String> res = new ArraySet<>();
        for (StorageVolume sv : sm.getRecentStorageVolumes()) {
            final String volumeName = sv.getMediaStoreVolumeName();
            if (volumeName != null) {
                res.add(volumeName);
            }
        }
        return res;
    }

    /**
     * Return the volume name that the given {@link Uri} references.
     */
    public static @NonNull String getVolumeName(@NonNull Uri uri) {
        final List<String> segments = uri.getPathSegments();
        switch (uri.getAuthority()) {
            case AUTHORITY:
            case AUTHORITY_LEGACY: {
                if (segments != null && segments.size() > 0) {
                    return segments.get(0);
                }
            }
        }
        throw new IllegalArgumentException("Missing volume name: " + uri);
    }

    /** {@hide} */
    public static boolean isKnownVolume(@NonNull String volumeName) {
        if (VOLUME_INTERNAL.equals(volumeName)) return true;
        if (VOLUME_EXTERNAL.equals(volumeName)) return true;
        if (VOLUME_EXTERNAL_PRIMARY.equals(volumeName)) return true;
        if (VOLUME_DEMO.equals(volumeName)) return true;
        return false;
    }

    /** {@hide} */
    public static @NonNull String checkArgumentVolumeName(@NonNull String volumeName) {
        if (TextUtils.isEmpty(volumeName)) {
            throw new IllegalArgumentException();
        }

        if (isKnownVolume(volumeName)) return volumeName;

        // When not one of the well-known values above, it must be a hex UUID
        for (int i = 0; i < volumeName.length(); i++) {
            final char c = volumeName.charAt(i);
            if (('a' <= c && c <= 'f') || ('0' <= c && c <= '9') || (c == '-')) {
                continue;
            } else {
                throw new IllegalArgumentException("Invalid volume name: " + volumeName);
            }
        }
        return volumeName;
    }

    /**
     * Uri for querying the state of the media scanner.
     */
    public static Uri getMediaScannerUri() {
        return AUTHORITY_URI.buildUpon().appendPath("none").appendPath("media_scanner").build();
    }

    /**
     * Name of current volume being scanned by the media scanner.
     */
    public static final String MEDIA_SCANNER_VOLUME = "volume";

    /**
     * Name of the file signaling the media scanner to ignore media in the containing directory
     * and its subdirectories. Developers should use this to avoid application graphics showing
     * up in the Gallery and likewise prevent application sounds and music from showing up in
     * the Music app.
     */
    public static final String MEDIA_IGNORE_FILENAME = ".nomedia";

    /**
     * Return an opaque version string describing the {@link MediaStore} state.
     * <p>
     * Applications that import data from {@link MediaStore} into their own
     * caches can use this to detect that {@link MediaStore} has undergone
     * substantial changes, and that data should be rescanned.
     * <p>
     * No other assumptions should be made about the meaning of the version.
     * <p>
     * This method returns the version for
     * {@link MediaStore#VOLUME_EXTERNAL_PRIMARY}; to obtain a version for a
     * different volume, use {@link #getVersion(Context, String)}.
     */
    public static @NonNull String getVersion(@NonNull Context context) {
        return getVersion(context, VOLUME_EXTERNAL_PRIMARY);
    }

    /**
     * Return an opaque version string describing the {@link MediaStore} state.
     * <p>
     * Applications that import data from {@link MediaStore} into their own
     * caches can use this to detect that {@link MediaStore} has undergone
     * substantial changes, and that data should be rescanned.
     * <p>
     * No other assumptions should be made about the meaning of the version.
     *
     * @param volumeName specific volume to obtain an opaque version string for.
     *            Must be one of the values returned from
     *            {@link #getExternalVolumeNames(Context)}.
     */
    public static @NonNull String getVersion(@NonNull Context context, @NonNull String volumeName) {
        final ContentResolver resolver = context.getContentResolver();
        try (ContentProviderClient client = resolver.acquireContentProviderClient(AUTHORITY)) {
            final Bundle in = new Bundle();
            in.putString(Intent.EXTRA_TEXT, volumeName);
            final Bundle out = client.call(GET_VERSION_CALL, null, in);
            return out.getString(Intent.EXTRA_TEXT);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Return the latest generation value for the given volume.
     * <p>
     * Generation numbers are useful for apps that are attempting to quickly
     * identify exactly which media items have been added or changed since a
     * previous point in time. Generation numbers are monotonically increasing
     * over time, and can be safely arithmetically compared.
     * <p>
     * Detecting media changes using generation numbers is more robust than
     * using {@link MediaColumns#DATE_ADDED} or
     * {@link MediaColumns#DATE_MODIFIED}, since those values may change in
     * unexpected ways when apps use {@link File#setLastModified(long)} or when
     * the system clock is set incorrectly.
     * <p>
     * Note that before comparing these detailed generation values, you should
     * first confirm that the overall version hasn't changed by checking
     * {@link MediaStore#getVersion(Context, String)}, since that indicates when
     * a more radical change has occurred. If the overall version changes, you
     * should assume that generation numbers have been reset and perform a full
     * synchronization pass.
     *
     * @param volumeName specific volume to obtain an generation value for. Must
     *            be one of the values returned from
     *            {@link #getExternalVolumeNames(Context)}.
     * @see MediaColumns#GENERATION_ADDED
     * @see MediaColumns#GENERATION_MODIFIED
     */
    public static long getGeneration(@NonNull Context context, @NonNull String volumeName) {
        return getGeneration(context.getContentResolver(), volumeName);
    }

    /** {@hide} */
    public static long getGeneration(@NonNull ContentResolver resolver,
            @NonNull String volumeName) {
        final Bundle in = new Bundle();
        in.putString(Intent.EXTRA_TEXT, volumeName);
        final Bundle out = resolver.call(AUTHORITY, GET_GENERATION_CALL, null, in);
        return out.getLong(Intent.EXTRA_INDEX);
    }

    /**
     * Return a {@link DocumentsProvider} Uri that is an equivalent to the given
     * {@link MediaStore} Uri.
     * <p>
     * This allows apps with Storage Access Framework permissions to convert
     * between {@link MediaStore} and {@link DocumentsProvider} Uris that refer
     * to the same underlying item. Note that this method doesn't grant any new
     * permissions; callers must already hold permissions obtained with
     * {@link Intent#ACTION_OPEN_DOCUMENT} or related APIs.
     *
     * @param mediaUri The {@link MediaStore} Uri to convert.
     * @return An equivalent {@link DocumentsProvider} Uri. Returns {@code null}
     *         if no equivalent was found.
     * @see #getMediaUri(Context, Uri)
     */
    public static @Nullable Uri getDocumentUri(@NonNull Context context, @NonNull Uri mediaUri) {
        final ContentResolver resolver = context.getContentResolver();
        final List<UriPermission> uriPermissions = resolver.getPersistedUriPermissions();

        try (ContentProviderClient client = resolver.acquireContentProviderClient(AUTHORITY)) {
            final Bundle in = new Bundle();
            in.putParcelable(EXTRA_URI, mediaUri);
            in.putParcelableArrayList(EXTRA_URI_PERMISSIONS, new ArrayList<>(uriPermissions));
            final Bundle out = client.call(GET_DOCUMENT_URI_CALL, null, in);
            return out.getParcelable(EXTRA_URI);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Return a {@link MediaStore} Uri that is an equivalent to the given
     * {@link DocumentsProvider} Uri. This only supports {@code ExternalStorageProvider}
     * and {@code MediaDocumentsProvider} Uris.
     * <p>
     * This allows apps with Storage Access Framework permissions to convert
     * between {@link MediaStore} and {@link DocumentsProvider} Uris that refer
     * to the same underlying item.
     * Note that this method doesn't grant any new permissions, but it grants the same access to
     * the Media Store Uri as the caller has to the given DocumentsProvider Uri; callers must
     * already hold permissions for documentUri obtained with {@link Intent#ACTION_OPEN_DOCUMENT}
     * or related APIs.
     *
     * @param documentUri The {@link DocumentsProvider} Uri to convert.
     * @return An equivalent {@link MediaStore} Uri. Returns {@code null} if no
     *         equivalent was found.
     * @see #getDocumentUri(Context, Uri)
     */
    public static @Nullable Uri getMediaUri(@NonNull Context context, @NonNull Uri documentUri) {
        final ContentResolver resolver = context.getContentResolver();
        final List<UriPermission> uriPermissions = resolver.getPersistedUriPermissions();

        try (ContentProviderClient client = resolver.acquireContentProviderClient(AUTHORITY)) {
            final Bundle in = new Bundle();
            in.putParcelable(EXTRA_URI, documentUri);
            in.putParcelableArrayList(EXTRA_URI_PERMISSIONS, new ArrayList<>(uriPermissions));
            final Bundle out = client.call(GET_MEDIA_URI_CALL, null, in);
            return out.getParcelable(EXTRA_URI);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Returns true if the given application is the current system gallery of the device.
     * <p>
     * The system gallery is one app chosen by the OEM that has read & write access to all photos
     * and videos on the device and control over folders in media collections.
     *
     * @param resolver The {@link ContentResolver} used to connect with
     * {@link MediaStore#AUTHORITY}. Typically this value is {@link Context#getContentResolver()}.
     * @param uid The uid to be checked if it is the current system gallery.
     * @param packageName The package name to be checked if it is the current system gallery.
     */
    public static boolean isCurrentSystemGallery(
            @NonNull ContentResolver resolver,
            int uid,
            @NonNull String packageName) {
        Bundle in = new Bundle();
        in.putInt(EXTRA_IS_SYSTEM_GALLERY_UID, uid);
        final Bundle out = resolver.call(AUTHORITY, IS_SYSTEM_GALLERY_CALL, packageName, in);
        return out.getBoolean(EXTRA_IS_SYSTEM_GALLERY_RESPONSE);
    }

    private static Uri maybeRemoveUserId(@NonNull Uri uri) {
        if (uri.getUserInfo() == null) return uri;

        Uri.Builder builder = uri.buildUpon();
        builder.authority(uri.getHost());
        return builder.build();
    }

    private static List<Uri> maybeRemoveUserId(@NonNull List<Uri> uris) {
        List<Uri> newUriList = new ArrayList<>();
        for (Uri uri : uris) {
            newUriList.add(maybeRemoveUserId(uri));
        }
        return newUriList;
    }

    private static int getUserIdFromUri(Uri uri) {
        final String userId = uri.getUserInfo();
        return userId == null ? MY_USER_ID : Integer.parseInt(userId);
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static Uri maybeAddUserId(@NonNull Uri uri, String userId) {
        if (userId == null) {
            return uri;
        }

        return ContentProvider.createContentUriForUser(uri,
            UserHandle.of(Integer.parseInt(userId)));
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private static List<Uri> maybeAddUserId(@NonNull List<Uri> uris, String userId) {
        if (userId == null) {
            return uris;
        }

        List<Uri> newUris = new ArrayList<>();
        for (Uri uri : uris) {
            newUris.add(maybeAddUserId(uri, userId));
        }
        return newUris;
    }

    /**
     * Returns an EXIF redacted version of {@code uri} i.e. a {@link Uri} with metadata such as
     * location, GPS datestamp etc. redacted from the EXIF headers.
     * <p>
     * A redacted Uri can be used to share a file with another application wherein exposing
     * sensitive information in EXIF headers is not desirable.
     * Note:
     * 1. Redacted uris cannot be granted write access and can neither be used to perform any kind
     * of write operations.
     * 2. To get a redacted uri the caller must hold read permission to {@code uri}.
     *
     * @param resolver The {@link ContentResolver} used to connect with
     * {@link MediaStore#AUTHORITY}. Typically this value is gotten from
     * {@link Context#getContentResolver()}
     * @param uri the {@link Uri} Uri to convert
     * @return redacted version of the {@code uri}. Returns {@code null} when the given
     * {@link Uri} could not be found or is unsupported
     * @throws SecurityException if the caller doesn't have the read access to {@code uri}
     * @see #getRedactedUri(ContentResolver, List)
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @Nullable
    public static Uri getRedactedUri(@NonNull ContentResolver resolver, @NonNull Uri uri) {
        final String authority = uri.getAuthority();
        try (ContentProviderClient client = resolver.acquireContentProviderClient(authority)) {
            final Bundle in = new Bundle();
            final String userId = uri.getUserInfo();
            // NOTE: The user-id in URI authority is ONLY required to find the correct MediaProvider
            // process. Once in the correct process, the field is no longer required and may cause
            // breakage in MediaProvider code. This is because per process logic is agnostic of
            // user-id. Hence strip away the user ids from URI, if present.
            in.putParcelable(EXTRA_URI, maybeRemoveUserId(uri));
            final Bundle out = client.call(GET_REDACTED_MEDIA_URI_CALL, null, in);
            // Add the user-id back to the URI if we had striped it earlier.
            return maybeAddUserId((Uri) out.getParcelable(EXTRA_URI), userId);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    private static void verifyUrisBelongToSingleUserId(@NonNull List<Uri> uris) {
        final int userId = getUserIdFromUri(uris.get(0));
        for (Uri uri : uris) {
            if (userId != getUserIdFromUri(uri)) {
                throw new IllegalArgumentException(
                    "All the uris should belong to a single user-id");
            }
        }
    }

    /**
     * Returns a list of EXIF redacted version of {@code uris} i.e. a {@link Uri} with metadata
     * such as location, GPS datestamp etc. redacted from the EXIF headers.
     * <p>
     * A redacted Uri can be used to share a file with another application wherein exposing
     * sensitive information in EXIF headers is not desirable.
     * Note:
     * 1. Order of the returned uris follow the order of the {@code uris}.
     * 2. Redacted uris cannot be granted write access and can neither be used to perform any kind
     * of write operations.
     * 3. To get a redacted uri the caller must hold read permission to its corresponding uri.
     *
     * @param resolver The {@link ContentResolver} used to connect with
     * {@link MediaStore#AUTHORITY}. Typically this value is gotten from
     * {@link Context#getContentResolver()}
     * @param uris the list of {@link Uri} Uri to convert
     * @return a list with redacted version of {@code uris}, in the same order. Returns {@code null}
     * when the corresponding {@link Uri} could not be found or is unsupported
     * @throws SecurityException if the caller doesn't have the read access to all the elements
     * in {@code uris}
     * @throws IllegalArgumentException if all the uris in {@code uris} don't belong to same user id
     * @see #getRedactedUri(ContentResolver, Uri)
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @NonNull
    public static List<Uri> getRedactedUri(@NonNull ContentResolver resolver,
            @NonNull List<Uri> uris) {
        verifyUrisBelongToSingleUserId(uris);
        final String authority = uris.get(0).getAuthority();
        try (ContentProviderClient client = resolver.acquireContentProviderClient(authority)) {
            final String userId = uris.get(0).getUserInfo();
            final Bundle in = new Bundle();
            // NOTE: The user-id in URI authority is ONLY required to find the correct MediaProvider
            // process. Once in the correct process, the field is no longer required and may cause
            // breakage in MediaProvider code. This is because per process logic is agnostic of
            // user-id. Hence strip away the user ids from URIs, if present.
            in.putParcelableArrayList(EXTRA_URI_LIST,
                (ArrayList<? extends Parcelable>) maybeRemoveUserId(uris));
            final Bundle out = client.call(GET_REDACTED_MEDIA_URI_LIST_CALL, null, in);
            // Add the user-id back to the URI if we had striped it earlier.
            return maybeAddUserId(out.getParcelableArrayList(EXTRA_URI_LIST), userId);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /** {@hide} */
    public static void resolvePlaylistMembers(@NonNull ContentResolver resolver,
            @NonNull Uri playlistUri) {
        final Bundle in = new Bundle();
        in.putParcelable(EXTRA_URI, playlistUri);
        resolver.call(AUTHORITY, RESOLVE_PLAYLIST_MEMBERS_CALL, null, in);
    }

    /** {@hide} */
    public static void runIdleMaintenance(@NonNull ContentResolver resolver) {
        resolver.call(AUTHORITY, RUN_IDLE_MAINTENANCE_CALL, null, null);
    }

    /**
     * Only used for testing.
     * {@hide}
     */
    @VisibleForTesting
    public static void runIdleMaintenanceForStableUris(@NonNull ContentResolver resolver) {
        resolver.call(AUTHORITY, RUN_IDLE_MAINTENANCE_FOR_STABLE_URIS, null, null);
    }

    /**
     * Only used for testing.
     * {@hide}
     */
    @VisibleForTesting
    public static String[] readBackedUpFilePaths(@NonNull ContentResolver resolver,
            String volumeName) {
        Bundle bundle = resolver.call(AUTHORITY, READ_BACKED_UP_FILE_PATHS, volumeName, null);
        return bundle.getStringArray(READ_BACKED_UP_FILE_PATHS);
    }

    /**
     * Only used for testing.
     * {@hide}
     */
    @VisibleForTesting
    public static void deleteBackedUpFilePaths(@NonNull ContentResolver resolver,
            String volumeName) {
        resolver.call(AUTHORITY, DELETE_BACKED_UP_FILE_PATHS, volumeName, null);
    }

    /**
     * Only used for testing.
     * {@hide}
     */
    @VisibleForTesting
    public static String[] getBackupFiles(@NonNull ContentResolver resolver) {
        Bundle bundle = resolver.call(AUTHORITY, GET_BACKUP_FILES, null, null);
        return bundle.getStringArray(GET_BACKUP_FILES);
    }

    /**
     * Block until any pending operations have finished, such as
     * {@link #scanFile} or {@link #scanVolume} requests.
     *
     * @hide
     */
    @SystemApi
    @WorkerThread
    public static void waitForIdle(@NonNull ContentResolver resolver) {
        resolver.call(AUTHORITY, WAIT_FOR_IDLE_CALL, null, null);
    }

    /**
     * Perform a blocking scan of the given {@link File}, returning the
     * {@link Uri} of the scanned file.
     *
     * @hide
     */
    @SystemApi
    @WorkerThread
    @SuppressLint("StreamFiles")
    public static @NonNull Uri scanFile(@NonNull ContentResolver resolver, @NonNull File file) {
        final Bundle out = resolver.call(AUTHORITY, SCAN_FILE_CALL, file.getAbsolutePath(), null);
        return out.getParcelable(Intent.EXTRA_STREAM);
    }

    /**
     * Perform a blocking scan of the given storage volume.
     *
     * @hide
     */
    @SystemApi
    @WorkerThread
    public static void scanVolume(@NonNull ContentResolver resolver, @NonNull String volumeName) {
        resolver.call(AUTHORITY, SCAN_VOLUME_CALL, volumeName, null);
    }

    /**
     * Returns whether the calling app is granted {@link android.Manifest.permission#MANAGE_MEDIA}
     * or not.
     * <p>Declaring the permission {@link android.Manifest.permission#MANAGE_MEDIA} isn't
     * enough to gain the access.
     * <p>To request access, use {@link android.provider.Settings#ACTION_REQUEST_MANAGE_MEDIA}.
     *
     * @param context the request context
     * @return true, the calling app is granted the permission. Otherwise, false
     *
     * @see android.Manifest.permission#MANAGE_MEDIA
     * @see android.provider.Settings#ACTION_REQUEST_MANAGE_MEDIA
     * @see #createDeleteRequest(ContentResolver, Collection)
     * @see #createTrashRequest(ContentResolver, Collection, boolean)
     * @see #createWriteRequest(ContentResolver, Collection)
     */
    @RequiresApi(Build.VERSION_CODES.S)
    public static boolean canManageMedia(@NonNull Context context) {
        Objects.requireNonNull(context);
        final String packageName = context.getOpPackageName();
        final int uid = context.getApplicationInfo().uid;
        final String permission = android.Manifest.permission.MANAGE_MEDIA;

        final AppOpsManager appOps = context.getSystemService(AppOpsManager.class);
        final int opMode = appOps.unsafeCheckOpNoThrow(AppOpsManager.permissionToOp(permission),
                uid, packageName);

        switch (opMode) {
            case AppOpsManager.MODE_DEFAULT:
                return PackageManager.PERMISSION_GRANTED == context.checkPermission(
                        permission, android.os.Process.myPid(), uid);
            case AppOpsManager.MODE_ALLOWED:
                return true;
            case AppOpsManager.MODE_ERRORED:
            case AppOpsManager.MODE_IGNORED:
                return false;
            default:
                Log.w(TAG, "Unknown AppOpsManager mode " + opMode);
                return false;
        }
    }

    /**
     * Returns {@code true} if and only if the caller with {@code authority} is the currently
     * enabled {@link CloudMediaProvider}. More specifically, {@code false} is also returned
     * if the calling uid doesn't match the uid of the {@code authority}.
     *
     * @see android.provider.CloudMediaProvider
     * @see #isSupportedCloudMediaProviderAuthority(ContentResolver, String)
     */
    public static boolean isCurrentCloudMediaProviderAuthority(@NonNull ContentResolver resolver,
            @NonNull String authority) {
        return callForCloudProvider(resolver, IS_CURRENT_CLOUD_PROVIDER_CALL, authority);
    }

    /**
     * Returns {@code true} if and only if the caller with {@code authority} is a supported
     * {@link CloudMediaProvider}. More specifically, {@code false} is also returned
     * if the calling uid doesn't match the uid of the {@code authority}.
     *
     * @see android.provider.CloudMediaProvider
     * @see #isCurrentCloudMediaProviderAuthority(ContentResolver, String)
     */
    public static boolean isSupportedCloudMediaProviderAuthority(@NonNull ContentResolver resolver,
            @NonNull String authority) {
        return callForCloudProvider(resolver, IS_SUPPORTED_CLOUD_PROVIDER_CALL, authority);
    }

    /**
     * Notifies the OS about a cloud media event requiring a full or incremental media collection
     * sync for the currently enabled cloud provider, {@code authority}.
     *
     * The OS will schedule the sync in the background and will attempt to batch frequent
     * notifications into a single sync event.
     *
     * If the caller is not the currently enabled cloud provider as returned by
     * {@link #isCurrentCloudMediaProviderAuthority(ContentResolver, String)}, the request will be
     * unsuccessful.
     *
     * @throws SecurityException if the request was unsuccessful.
     */
    public static void notifyCloudMediaChangedEvent(@NonNull ContentResolver resolver,
            @NonNull String authority, @NonNull String currentMediaCollectionId)
            throws SecurityException {
        if (!callForCloudProvider(resolver, NOTIFY_CLOUD_MEDIA_CHANGED_EVENT_CALL, authority)) {
            throw new SecurityException("Failed to notify cloud media changed event");
        }
    }

    private static boolean callForCloudProvider(ContentResolver resolver, String method,
            String callingAuthority) {
        Objects.requireNonNull(resolver);
        Objects.requireNonNull(method);
        Objects.requireNonNull(callingAuthority);

        final Bundle out = resolver.call(AUTHORITY, method, callingAuthority, /* extras */ null);
        return out.getBoolean(EXTRA_CLOUD_PROVIDER_RESULT);
    }

    /** {@hide} */
    public static String getCurrentCloudProvider(@NonNull ContentResolver resolver) {
        try (ContentProviderClient client = resolver.acquireContentProviderClient(AUTHORITY)) {
            final Bundle out = client.call(GET_CLOUD_PROVIDER_CALL, /* arg */ null,
                    /* extras */ null);
            return out.getString(GET_CLOUD_PROVIDER_RESULT);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }

    /**
     * Grant {@link com.android.providers.media.MediaGrants} for the given package, for the
     * list of local (to the device) content uris. These must be valid picker uris.
     *
     * @hide
     */
    public static void grantMediaReadForPackage(
            @NonNull Context context, int packageUid, List<Uri> uris) {
        final ContentResolver resolver = context.getContentResolver();
        try (ContentProviderClient client = resolver.acquireContentProviderClient(AUTHORITY)) {
            final Bundle extras = new Bundle();
            extras.putInt(Intent.EXTRA_UID, packageUid);
            extras.putParcelableArrayList(EXTRA_URI_LIST, new ArrayList<Uri>(uris));
            client.call(GRANT_MEDIA_READ_FOR_PACKAGE_CALL,
                    /* arg= */ null,
                    /* extras= */ extras);
        } catch (RemoteException e) {
            throw e.rethrowAsRuntimeException();
        }
    }
}
