/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.StringDef;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.providers.media.flags.Flags;

import java.lang.annotation.Retention;
import java.util.UUID;

/**
 * Defines the contract between a cloud media provider and the OS.
 * <p>
 * To create a cloud media provider, extend {@link CloudMediaProvider}, which
 * provides a foundational implementation of this contract.
 *
 * @see CloudMediaProvider
 */
public final class CloudMediaProviderContract {
    private static final String TAG = "CloudMediaProviderContract";

    private CloudMediaProviderContract() {}

    /**
     * {@link Intent} action used to identify {@link CloudMediaProvider} instances. This
     * is used in the {@code <intent-filter>} of the {@code <provider>}.
     */
    public static final String PROVIDER_INTERFACE = "android.content.action.CLOUD_MEDIA_PROVIDER";

    /**
     * Permission required to protect {@link CloudMediaProvider} instances. Providers should
     * require this in the {@code permission} attribute in their {@code <provider>} tag.
     * The OS will not connect to a provider without this protection.
     */
    public static final String MANAGE_CLOUD_MEDIA_PROVIDERS_PERMISSION =
            "com.android.providers.media.permission.MANAGE_CLOUD_MEDIA_PROVIDERS";

    /**
     * Information about what capabilities a CloudMediaProvider can support. This
     * will be used by the system to inform which APIs should be expected to return
     * data. This object is returned from {@link CloudMediaProvider#onGetCapabilities}.
     *
     * This object enumerates which capabilities are provided by the
     * CloudMediaProvider implementation that supplied this object.
     *
     * @see CloudMediaProvider#onGetCapabilities()
     */
    @FlaggedApi(Flags.FLAG_ENABLE_CLOUD_MEDIA_PROVIDER_CAPABILITIES)
    public static final class Capabilities implements Parcelable {

        private boolean mSearchEnabled;
        private boolean mMediaCategoriesEnabled;
        private boolean mAlbumsAsCategory;

        Capabilities(@NonNull Builder builder) {
            this.mSearchEnabled = builder.mSearchEnabled;
            this.mMediaCategoriesEnabled = builder.mMediaCategoriesEnabled;
            this.mAlbumsAsCategory = builder.mAlbumsAsCategoryEnabled;
        }


        /**
         * If the CloudMediaProvider supports Search functionality.
         *
         * In order for search to be enabled the CloudMediaProvider needs to
         * implement the following APIs:
         *
         * @see CloudMediaProvider#onSearchMedia
         * @see CloudMediaProvider#onQuerySearchSuggestions
         *
         * This capability is disabled by default.
         *
         * @return true if search is enabled for this CloudMediaProvider.
         */
        @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
        public boolean isSearchEnabled() {
            return mSearchEnabled;
        }

        /**
         * If the CloudMediaProvider supports MediaCategories.
         *
         * In order for MediaCategories to be enabled the CloudMediaProvider needs to
         * implement the following APIs:
         *
         * @see CloudMediaProvider#onQueryMediaCategories
         * @see CloudMediaProvider#onQueryMediaSets
         *
         * This capability is disabled by default.
         *
         * @return true if media categories are enabled for this CloudMediaProvider.
         */
        @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
        public boolean isMediaCategoriesEnabled() {
            return mMediaCategoriesEnabled;
        }

        /**
        * If the CloudMediaProvider will return user albums as a grouped category.
        *
        * When this capability is enabled, {@link CloudMediaProvider#onQueryAlbums} will
        * no longer be called to sync the users albums, and it is expected that a
        * category with the type {@link #MEDIA_CATEGORY_TYPE_USER_ALBUMS} will be
        * provided in the {@link CloudMediaProvider#onQueryMediaCategories} for
        * providing the user's custom albums. If no such category is returned,
        * then there will be no data for user custom albums.
        *
        * NOTE: This capability requires the
        * {@link Capabilities#isMediaCategoriesEnabled} capability to also be enabled
        * for the CloudMediaProvider. If it is not, this Capability has no effect and
        * will be ignored.
        *
        * @see CloudMediaProvider#onQueryMediaCategories
        * @see #MEDIA_CATEGORY_TYPE_USER_ALBUMS
        *
        * This capability is disabled by default.
        *
        * @return true if albums will be returned as a MediaCategory.
        *
        * @hide
        */
        public boolean isAlbumsAsCategoryEnabled() {
            return mAlbumsAsCategory;
        }

        /**
         * @hide
         */
        @Override
        public String toString() {
            return " isSearchEnabled=" + this.mSearchEnabled
                    + " isMediaCategoriesEnabled=" + this.mMediaCategoriesEnabled
                    + " isAlbumsAsCategoryEnabled=" + this.mAlbumsAsCategory;
        }

        /**
         * Implemented for {@link Parcelable}
         */
        @Override
        public int describeContents() {
            return 0;
        }

        /**
         * Implemented for {@link Parcelable}
         */
        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeBoolean(mSearchEnabled);
            dest.writeBoolean(mMediaCategoriesEnabled);
            dest.writeBoolean(mAlbumsAsCategory);
        }

        /**
         * Implemented for {@link Parcelable}
         */
        @NonNull
        public static final Parcelable.Creator<Capabilities> CREATOR =
                new Parcelable.Creator<Capabilities>() {

                    @NonNull
                    @Override
                    public Capabilities createFromParcel(Parcel source) {
                        boolean searchEnabled = source.readBoolean();
                        boolean mediaCategoriesEnabled = source.readBoolean();
                        boolean mAlbumsAsCategoryEnabled = source.readBoolean();

                        Capabilities.Builder builder = new Capabilities.Builder();

                        if (Flags.cloudMediaProviderSearch()) {
                            builder
                                    .setSearchEnabled(searchEnabled)
                                    .setMediaCategoriesEnabled(mediaCategoriesEnabled)
                                    .setAlbumsAsCategoryEnabled(mAlbumsAsCategoryEnabled);
                        }

                        return builder.build();
                    }

                    @NonNull
                    @Override
                    public Capabilities[] newArray(int size) {
                        return new Capabilities[size];
                    }
                };

        /**
         * Builder for a {@link CloudMediaProviderContract.Capabilities} object.
         *
         * @see Capabilities
         */
        @FlaggedApi(Flags.FLAG_ENABLE_CLOUD_MEDIA_PROVIDER_CAPABILITIES)
        public static final class Builder {

            // Default values for each capability. These are used if not explicitly changed.
            private boolean mSearchEnabled = false;
            private boolean mMediaCategoriesEnabled = false;
            private boolean mAlbumsAsCategoryEnabled = false;

            public Builder() {
            }


            /**
             * The SearchEnabled capability informs that search related APIs are supported
             * and can be invoked on this provider.
             *
             * @see CloudMediaProvider#onSearchMedia
             * @see CloudMediaProvider#onQuerySearchSuggestions
             *
             * @param enabled true if this capability is supported, the default value is false.
             */
            @NonNull
            @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
            public Builder setSearchEnabled(boolean enabled) {
                mSearchEnabled = enabled;
                return this;
            }

            /**
             * The MediaCategories capability informs that category related APIs are
             * supported and can be invoked on this provider.
             *
             * @see CloudMediaProvider#onQueryMediaCategories
             * @see CloudMediaProvider#onQueryMediaSets
             *
             * @param enabled true if this capability is supported, the default value is false.
             */
            @NonNull
            @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
            public Builder setMediaCategoriesEnabled(boolean enabled) {
                mMediaCategoriesEnabled = enabled;
                return this;
            }

            /**
             * If the CloudMediaProvider will return user albums as a grouped category.
             *
             * When this capability is enabled, {@link CloudMediaProvider#onQueryAlbums} will
             * no longer be called to sync the users albums, and it is expected that a
             * category with the type {@link #MEDIA_CATEGORY_TYPE_USER_ALBUMS} will be
             * provided in the {@link CloudMediaProvider#onQueryMediaCategories} for
             * providing the user's custom albums. If no such category is returned,
             * then there will be no data for user custom albums.
             *
             * NOTE: This capability requires the
             * {@link Capabilities#isMediaCategoriesEnabled} capability to also be enabled
             * for the CloudMediaProvider. If it is not, this Capability has no effect and
             * will be ignored.
             *
             * @see CloudMediaProvider#onQueryMediaCategories
             * @see #MEDIA_CATEGORY_TYPE_USER_ALBUMS
             *
             * @param enabled true if this capability is supported, the default value is false.
             *
             * @hide
             */
            @NonNull
            public Builder setAlbumsAsCategoryEnabled(boolean enabled) {
                mAlbumsAsCategoryEnabled = enabled;
                return this;
            }

            /**
             * Create a new {@link CloudMediaProviderContract.Capabilities} object with the
             * current builder's Capabilities.
             */
            @NonNull
            public Capabilities build() {
                return new Capabilities(this);
            }

        }

    }

    /** Constants related to a media item, including {@link Cursor} column names */
    public static final class MediaColumns {
        private MediaColumns() {}

        /**
         * Unique ID of a media item. This ID is both provided by and interpreted
         * by a {@link CloudMediaProvider}, and should be treated as an opaque
         * value by client applications.
         *
         * <p>
         * Each media item must have a unique ID within a provider.
         *
         * <p>
         * A provider must always return stable IDs, since they will be used to
         * issue long-term URI permission grants when an application interacts
         * with {@link MediaStore#ACTION_PICK_IMAGES}.
         * <p>
         * Type: STRING
         */
        public static final String ID = "id";

        /**
         * Timestamp when a media item was capture, in milliseconds since
         * January 1, 1970 00:00:00.0 UTC.
         * <p>
         * Implementations should extract this data from the metadata embedded in the media
         * file. If this information is not available, a reasonable heuristic can be used, e.g.
         * the time the media file was added to the media collection.
         * <p>
         * Type: LONG
         *
         * @see CloudMediaProviderContract.AlbumColumns#DATE_TAKEN_MILLIS
         * @see System#currentTimeMillis()
         */
        public static final String DATE_TAKEN_MILLIS = "date_taken_millis";

        /**
         * Non-negative number associated with a media item indicating what generation or batch the
         * media item was synced into the media collection.
         * <p>
         * Providers should associate a monotonically increasing sync generation number to each
         * media item which is expected to increase for each atomic modification on the media item.
         * This is useful for the OS to quickly identify that a media item has changed since a
         * previous point in time. Note that this does not need to be unique across all media items,
         * i.e. multiple media items can have the same SYNC_GENERATION value. However, the
         * modification of a media item should increase the
         * {@link MediaCollectionInfo#LAST_MEDIA_SYNC_GENERATION}.
         * <p>
         * Type: LONG
         *
         * @see MediaCollectionInfo#LAST_MEDIA_SYNC_GENERATION
         */
        public static final String SYNC_GENERATION = "sync_generation";

        /**
         * Concrete MIME type of a media file. For example, "image/png" or
         * "video/mp4".
         * <p>
         * Type: STRING
         */
        public static final String MIME_TYPE = "mime_type";

        /**
         * Mime-type extension representing special format for a media item.
         *
         * Photo Picker requires special format tagging for media items.
         * This is essential as media items can have various formats like
         * Motion Photos, GIFs etc, which are not identifiable by
         * {@link #MIME_TYPE}.
         * <p>
         * Type: INTEGER
         */
        public static final String STANDARD_MIME_TYPE_EXTENSION = "standard_mime_type_extension";

        /**
         * Constant for the {@link #STANDARD_MIME_TYPE_EXTENSION} column indicating
         * that the media item doesn't have any special format associated with it.
         */
        public static final int STANDARD_MIME_TYPE_EXTENSION_NONE = 0;

        /**
         * Constant for the {@link #STANDARD_MIME_TYPE_EXTENSION} column indicating
         * that the media item is a GIF.
         */
        public static final int STANDARD_MIME_TYPE_EXTENSION_GIF = 1;

        /**
         * Constant for the {@link #STANDARD_MIME_TYPE_EXTENSION} column indicating
         * that the media item is a Motion Photo.
         */
        public static final int STANDARD_MIME_TYPE_EXTENSION_MOTION_PHOTO = 2;

        /**
         * Constant for the {@link #STANDARD_MIME_TYPE_EXTENSION} column indicating
         * that the media item is an Animated Webp.
         */
        public static final int STANDARD_MIME_TYPE_EXTENSION_ANIMATED_WEBP = 3;

        /**
         * Size of a media file, in bytes.
         * <p>
         * Type: LONG
         */
        public static final String SIZE_BYTES = "size_bytes";

        /**
         * {@link MediaStore} URI of a media file if the file is available locally on the device.
         * <p>
         * If it's a cloud-only media file, this field should not be set.
         * Any of the following URIs can be used: {@link MediaStore.Files},
         * {@link MediaStore.Images} or {@link MediaStore.Video} e.g.
         * {@code content://media/file/45}.
         * <p>
         * Implementations don't need to handle the {@link MediaStore} URI becoming invalid after
         * the local item has been deleted or modified. If the URI becomes invalid or the
         * local and cloud file content diverges, the OS will treat the cloud media item as a
         * cloud-only item.
         * <p>
         * Type: STRING
         */
        public static final String MEDIA_STORE_URI = "media_store_uri";

        /**
         * Duration of a video file in ms. If the file is an image for which duration is not
         * applicable, this field can be left empty or set to {@code zero}.
         * <p>
         * Type: LONG
         */
        public static final String DURATION_MILLIS = "duration_millis";

        /**
         * Whether the item has been favourited in the media collection. If {@code non-zero}, this
         * media item will appear in the favourites category in the Photo Picker.
         * <p>
         * Type: INTEGER
         */
        public static final String IS_FAVORITE = "is_favorite";

        /**
         * This column contains the width of the image or video.
         */
        public static final String WIDTH = "width";

        /**
         * This column contains the height of the image or video.
         */
        public static final String HEIGHT = "height";

        /**
         * This column contains the orientation, if available.
         * <p>
         * For consistency the indexed value is expressed in degrees, such as 0,
         * 90, 180, or 270.
         */
        public static final String ORIENTATION = "orientation";

        /**
         * Authority of the media item
         * <p>
         * Type: STRING
         *
         * @hide
         */
        public static final String AUTHORITY = "authority";

        /**
         * File path of the media item
         * <p>
         * Type: STRING
         *
         * @hide
         */
        public static final String DATA = "data";

        /**
         * Owner package of the media item
         * <p>
         * Type: STRING
         *
         * @hide
         */
        public static final String OWNER_PACKAGE_NAME = "owner_package_name";

        /**
         * package user id of the media item
         * <p>
         * Type: STRING
         *
         * @hide
         */
        public static final String USER_ID = "_user_id";

        /**
         * Array of all {@link MediaColumn} fields.
         *
         * @hide
         */
        public static final String[] ALL_PROJECTION = new String[] {
            ID,
            DATE_TAKEN_MILLIS,
            SYNC_GENERATION,
            MIME_TYPE,
            STANDARD_MIME_TYPE_EXTENSION,
            SIZE_BYTES,
            MEDIA_STORE_URI,
            DURATION_MILLIS,
            IS_FAVORITE,
            WIDTH,
            HEIGHT,
            ORIENTATION,
            DATA,
            AUTHORITY,
            OWNER_PACKAGE_NAME,
            USER_ID,
        };
    }

    /**
     * <p>
     * {@link Intent#EXTRA_MIME_TYPES} extra can be passed as a {@link Bundle} parameter to
     * the CloudMediaProvider#onQueryAlbums method. The value is an Array of String Mime types.
     * The provider should only return items matching at least one of the given Mime types.
     *
     * <p>
     * This may be a pattern, such as *&#47;*,to query for all available MIME types that
     * match the pattern,e.g.{@code image/*} should match {@code image/jpeg} and
     * {@code image/png}.
     *
     * <p>
     * Type: String[] (It is an string array of meme type filters)
     */

    /** Constants related to an album item, including {@link Cursor} column names */
    public static final class AlbumColumns {
        private AlbumColumns() {}

        /**
         * Unique ID of an album. This ID is both provided by and interpreted
         * by a {@link CloudMediaProvider}.
         * <p>
         * Each album item must have a unique ID within a media collection.
         * <p>
         * A provider should return durable IDs, since they will be used to cache
         * album information in the OS.
         * <p>
         * Type: STRING
         */
        public static final String ID = "id";

        /**
         * Display name of a an album, used as the primary title displayed to a
         * user.
         * <p>
         * Type: STRING
         */
        public static final String DISPLAY_NAME = "display_name";

        /**
         * Timestamp of the most recently taken photo in an album, in milliseconds since
         * January 1, 1970 00:00:00.0 UTC.
         * <p>
         * Type: LONG
         *
         * @see CloudMediaProviderContract.MediaColumns#DATE_TAKEN_MILLIS
         * @see System#currentTimeMillis()
         */
        public static final String DATE_TAKEN_MILLIS = "date_taken_millis";

        /**
         * Media id to use as the album cover photo.
         * <p>
         * If this field is not provided, albums will be shown in the Photo Picker without a cover
         * photo.
         * <p>
         * Type: LONG
         *
         * @see CloudMediaProviderContract.MediaColumns#ID
         */
        public static final String MEDIA_COVER_ID = "album_media_cover_id";

        /**
         * Total count of all media within the album, including photos and videos.
         * <p>
         * If this field is not provided, albums will be shown without a count in the Photo Picker.
         * <p>
         * Empty albums should be omitted from the {@link CloudMediaProvider#onQueryAlbums} result,
         * i.e. zero is not a valid media count.
         * <p>
         * Type: LONG
         */
        public static final String MEDIA_COUNT = "album_media_count";

        /**
         * Authority of the album item
         * <p>
         * Type: STRING
         *
         * @hide
         */
        public static final String AUTHORITY = "authority";

        /**
         * Whether the album item was generated locally
         * <p>
         * Type: STRING
         *
         * @hide
         */
        public static final String IS_LOCAL = "is_local";

        /**
         * Array of all {@link AlbumColumn} fields.
         *
         * @hide
         */
        public static final String[] ALL_PROJECTION = new String[] {
            ID,
            DATE_TAKEN_MILLIS,
            DISPLAY_NAME,
            MEDIA_COVER_ID,
            MEDIA_COUNT,
            AUTHORITY,
        };

        /**
         * Includes local media present in any directory containing
         * {@link Environment#DIRECTORY_SCREENSHOTS} in relative path
         *
         * @hide
         */
        public static final String ALBUM_ID_SCREENSHOTS = "Screenshots";

        /**
         * Includes local images/videos that are present in the
         * {@link Environment#DIRECTORY_DCIM}/Camera directory.
         *
         * @hide
         */
        public static final String ALBUM_ID_CAMERA = "Camera";

        /**
         * Includes local and cloud videos only.
         *
         * @hide
         */
        public static final String ALBUM_ID_VIDEOS = "Videos";

        /**
         * Includes local images/videos that have {@link MediaStore.MediaColumns#IS_DOWNLOAD} set.
         *
         * @hide
         */
        public static final String ALBUM_ID_DOWNLOADS = "Downloads";

        /**
         * Includes local and cloud images/videos that have been favorited by the user.
         *
         * @hide
         */
        public static final String ALBUM_ID_FAVORITES = "Favorites";
    }

    /** Constants related to a media collection */
    public static final class MediaCollectionInfo {
        private MediaCollectionInfo() {}

        /**
         * Media collection identifier
         * <p>
         * The only requirement on the collection ID is uniqueness on a device.
         * <p>
         * This value will not be interpreted by the OS, however it will be used to check the
         * validity of cached data and URI grants to client apps. Anytime the media or album ids
         * get re-indexed, a new collection with a new and unique id should be created so that the
         * OS can clear its cache and more importantly, revoke any URI grants to apps.
         * <p>
         * Apps are recommended to generate unique collection ids with, {@link UUID#randomUUID}.
         * This is preferred to using a simple monotonic sequence because the provider data could
         * get cleared and it might have to re-index media items on the device without any history
         * of its last ID. With random UUIDs, if data gets cleared, a new one can easily be
         * generated safely.
         * <p>
         * Type: STRING
         *
         * @see CloudMediaProvider#onGetMediaCollectionInfo
         */
        public static final String MEDIA_COLLECTION_ID = "media_collection_id";

        /**
         * Last {@link CloudMediaProviderContract.MediaColumns#SYNC_GENERATION} in the media
         * collection including deleted media items.
         * <p>
         * Providers should associate a monotonically increasing sync generation to each
         * media item change (insertion/deletion/update). This is useful for the OS to quickly
         * identify exactly which media items have changed since a previous point in time.
         * <p>
         * Type: LONG
         *
         * @see CloudMediaProviderContract#EXTRA_SYNC_GENERATION
         * @see CloudMediaProvider#onGetMediaCollectionInfo
         * @see CloudMediaProviderContract.MediaColumns#SYNC_GENERATION
         */
        public static final String LAST_MEDIA_SYNC_GENERATION = "last_media_sync_generation";

        /**
         * Name of the account that owns the media collection.
         * <p>
         * Type: STRING
         *
         * @see CloudMediaProvider#onGetMediaCollectionInfo
         */
        public static final String ACCOUNT_NAME = "account_name";

        /**
         * {@link Intent} Intent to launch an {@link Activity} to allow users configure their media
         * collection account information like the account name.
         * <p>
         * Type: PARCELABLE
         *
         * @see CloudMediaProvider#onGetMediaCollectionInfo
         */
        public static final String ACCOUNT_CONFIGURATION_INTENT = "account_configuration_intent";
    }

    /**
     * Opaque pagination token to retrieve the next page (cursor) from a media or album query.
     * <p>
     * Providers can optionally set this token as part of the {@link Cursor#setExtras}
     * {@link Bundle}. If a token is set, the OS can pass it as a {@link Bundle} parameter when
     * querying for media or albums to fetch subsequent pages. The provider can keep returning
     * pagination tokens until the last page at which point it should not set a token on the
     * {@link Cursor}.
     * <p>
     * If the provider handled the page token as part of the query, they must add
     * the {@link #EXTRA_PAGE_TOKEN} key to the array of {@link ContentResolver#EXTRA_HONORED_ARGS}
     * as part of the returned {@link Cursor#setExtras} {@link Bundle}.
     *
     * @see CloudMediaProvider#onQueryMedia
     * @see CloudMediaProvider#onQueryAlbums
     * <p>
     * Type: STRING
     */
    public static final String EXTRA_PAGE_TOKEN = "android.provider.extra.PAGE_TOKEN";

    /**
     * {@link MediaCollectionInfo#MEDIA_COLLECTION_ID} on which the media or album query occurred.
     *
     * <p>
     * Providers must set this token as part of the {@link Cursor#setExtras}
     * {@link Bundle} returned from the cursors on query.
     * This allows the OS to verify that the returned results match the
     * {@link MediaCollectionInfo#MEDIA_COLLECTION_ID} queried via
     * {@link CloudMediaProvider#onGetMediaCollectionInfo}. If the collection differs, the OS will
     * ignore the result and may try again.
     *
     * @see CloudMediaProvider#onQueryMedia
     * @see CloudMediaProvider#onQueryDeletedMedia
     * @see CloudMediaProvider#onQueryAlbums
     * <p>
     * Type: STRING
     */
    public static final String EXTRA_MEDIA_COLLECTION_ID =
            "android.provider.extra.MEDIA_COLLECTION_ID";

    /**
     * Generation number to fetch the latest media or album metadata changes from the media
     * collection.
     * <p>
     * The provider should associate a monotonically increasing sync generation to each media
     * item change (insertion/deletion/update). This is useful to quickly identify exactly which
     * media items have changed since a previous point in time.
     * <p>
     * Providers should also associate a separate monotonically increasing sync generation
     * for album changes (insertion/deletion/update). This album sync generation, should record
     * both changes to the album metadata itself and changes to the media items contained in the
     * album. E.g. a direct change to an album's
     * {@link CloudMediaProviderContract.AlbumColumns#DISPLAY_NAME} will increase the
     * album sync generation, likewise adding a photo to that album should also increase the
     * sync generation.
     * <p>
     * Note that multiple media (or album) items can share a sync generation as long as the entire
     * change appears atomic from the perspective of the query APIs. E.g. each item in a batch photo
     * sync from the cloud can have the same sync generation if they were all synced atomically into
     * the collection from the perspective of an external observer.
     * <p>
     * This extra can be passed as a {@link Bundle} parameter to the media or album query methods
     * and the provider should only return items with a sync generation that is strictly greater
     * than the one provided in the filter.
     * <p>
     * If the provider supports this filter, it must support the respective
     * {@link CloudMediaProvider#onGetMediaCollectionInfo} methods to return the {@code count} and
     * {@code max generation} for media or albums.
     * <p>
     * If the provider handled the generation, they must add the
     * {@link #EXTRA_SYNC_GENERATION} key to the array of {@link ContentResolver#EXTRA_HONORED_ARGS}
     * as part of the returned {@link Cursor#setExtras} {@link Bundle}.
     *
     * @see MediaCollectionInfo#LAST_MEDIA_SYNC_GENERATION
     * @see CloudMediaProvider#onQueryMedia
     * @see CloudMediaProvider#onQueryAlbums
     * @see MediaStore.MediaColumns#GENERATION_MODIFIED
     * <p>
     * Type: LONG
     */
    public static final String EXTRA_SYNC_GENERATION = "android.provider.extra.SYNC_GENERATION";

    /**
     * Limits the query results to only media items matching the given album id.
     * <p>
     * If the provider handled the album filter, they must also add the {@link #EXTRA_ALBUM_ID}
     * key to the array of {@link ContentResolver#EXTRA_HONORED_ARGS} as part of the returned
     * {@link Cursor#setExtras} {@link Bundle}.
     *
     * @see CloudMediaProvider#onQueryMedia
     * <p>
     * Type: STRING
     */
    public static final String EXTRA_ALBUM_ID = "android.provider.extra.ALBUM_ID";

    /**
     * The maximum number of query results that should be included in a batch when syncing metadata
     * with cloud provider.
     *
     * This extra can be passed as a {@link Bundle} parameter to the media or album query methods.
     *
     * It is optional for the provider to honor this extra and return results at max page size.
     *
     * @see CloudMediaProvider#onQueryMedia
     * @see CloudMediaProvider#onQueryAlbums
     *
     * <p>
     * Type: INTEGER
     */
    public static final String EXTRA_PAGE_SIZE = "android.provider.extra.PAGE_SIZE";

    /**
     * Limits the query results to only media items less than the given file size in bytes.
     * <p>
     * This is only intended for the MediaProvider to implement for cross-user communication. Not
     * for third party apps.
     *
     * @see CloudMediaProvider#onQueryMedia
     * <p>
     * Type: LONG
     * @hide
     */
    public static final String EXTRA_SIZE_LIMIT_BYTES =
            "android.provider.extra.EXTRA_SIZE_LIMIT_BYTES";

    /**
     * Forces the {@link CloudMediaProvider#onOpenPreview} file descriptor to return a thumbnail
     * image. This is only useful for videos where the OS can either request a video or image
     * for preview.
     *
     * @see CloudMediaProvider#onOpenPreview
     * <p>
     * Type: BOOLEAN
     */
    public static final String EXTRA_PREVIEW_THUMBNAIL =
            "android.provider.extra.PREVIEW_THUMBNAIL";

    /**
     * Extra used to specify the sorting behavior when querying from {@link CloudMediaProvider}.
     * The value associated with this extra should be one of the integer constants
     * defined in the {@link SortOrders}.
     * <p>
     * Type: INTEGER
     *
     * @see CloudMediaProvider#onSearchMedia
     */
    @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
    public static final String EXTRA_SORT_ORDER = "android.provider.extra.SORT_ORDER";

    /**
     * Sort items in descending order by the {@code DATE_TAKEN_MILLIS}.
     * <p>
     * This means the most recently taken photos or videos will appear first.
     * <p>
     * Type: INTEGER
     */
    @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
    public static final int SORT_ORDER_DESC_DATE_TAKEN = 1;

    /**
     * Defines integer constants to be used with the {@link #EXTRA_SORT_ORDER} extra
     * for specifying the sorting order of media items.
     * @hide
     */
    @IntDef(value = {SORT_ORDER_DESC_DATE_TAKEN})
    @Retention(SOURCE)
    public @interface SortOrder {}

    /**
     * A boolean to indicate {@link com.android.providers.media.photopicker.PhotoPickerProvider}
     * this request is requesting a cached thumbnail file from MediaStore.
     *
     * Type: BOOLEAN
     *
     * {@hide}
     */
    public static final String EXTRA_MEDIASTORE_THUMB = "android.provider.extra.MEDIASTORE_THUMB";

    /**
     * Constant used to execute {@link CloudMediaProvider#onGetMediaCollectionInfo} via
     * {@link ContentProvider#call}.
     *
     * {@hide}
     */
    public static final String METHOD_GET_MEDIA_COLLECTION_INFO = "android:getMediaCollectionInfo";

    /**
     * Constant used to execute {@link CloudMediaProvider#onCreateCloudMediaSurfaceController} via
     * {@link ContentProvider#call}.
     *
     * {@hide}
     */
    public static final String METHOD_CREATE_SURFACE_CONTROLLER = "android:createSurfaceController";

    /**
     * Gets surface controller from {@link CloudMediaProvider#onCreateCloudMediaSurfaceController}.
     * {@hide}
     */
    public static final String EXTRA_SURFACE_CONTROLLER =
            "android.provider.extra.SURFACE_CONTROLLER";

    /**
     * Indicates whether to enable looping playback of media items.
     * <p>
     * In case this is not present, the default value should be false.
     *
     * @see CloudMediaProvider#onCreateCloudMediaSurfaceController
     * @see CloudMediaProvider.CloudMediaSurfaceController#onConfigChange
     * <p>
     * Type: BOOLEAN
     * By default, the value is true
     */
    public static final String EXTRA_LOOPING_PLAYBACK_ENABLED =
            "android.provider.extra.LOOPING_PLAYBACK_ENABLED";

    /**
     * Indicates whether to mute audio during preview of media items.
     *
     * @see CloudMediaProvider#onCreateCloudMediaSurfaceController
     * @see CloudMediaProvider.CloudMediaSurfaceController#onConfigChange
     * <p>
     * Type: BOOLEAN
     * By default, the value is false
     */
    public static final String EXTRA_SURFACE_CONTROLLER_AUDIO_MUTE_ENABLED =
            "android.provider.extra.SURFACE_CONTROLLER_AUDIO_MUTE_ENABLED";

    /**
     * Gets surface state callback from picker launched via
     * {@link MediaStore#ACTION_PICK_IMAGES}).
     *
     * @see MediaStore#ACTION_PICK_IMAGES
     *
     * {@hide}
     */
    public static final String EXTRA_SURFACE_STATE_CALLBACK =
            "android.provider.extra.SURFACE_STATE_CALLBACK";

    /**
     * Constant used to execute {@link CloudMediaProvider#onGetAsyncContentProvider()} via
     * {@link android.content.ContentProvider#call}.
     *
     * {@hide}
     */
    public static final String METHOD_GET_ASYNC_CONTENT_PROVIDER =
            "android:getAsyncContentProvider";

    /**
     * Constant used to execute {@link CloudMediaProvider#onGetCapabilities()} via
     * {@link android.content.ContentProvider#call}.
     *
     * {@hide}
     */
    public static final String METHOD_GET_CAPABILITIES = "android:getCapabilities";

    /**
     * Constant used to get/set {@link Capabilities} in {@link Bundle}.
     *
     * {@hide}
     */
    public static final String EXTRA_PROVIDER_CAPABILITIES =
            "android.provider.extra.PROVIDER_CAPABILITIES";


    /**
     * Constant used to get/set {@link IAsyncContentProvider} in {@link Bundle}.
     *
     * {@hide}
     */
    public static final String EXTRA_ASYNC_CONTENT_PROVIDER =
            "android.provider.extra.ASYNC_CONTENT_PROVIDER";

    /**
     * Constant used to get/set {@link android.os.ParcelFileDescriptor} in {@link Bundle}.
     *
     * {@hide}
     */
    public static final String EXTRA_FILE_DESCRIPTOR = "android.provider.extra.file_descriptor";

    /**
     * Constant used to get/set CMP exception message in {@link Bundle}.
     *
     * {@hide}
     */
    public static final String EXTRA_ERROR_MESSAGE = "android.provider.extra.error_message";

    /**
     * Constant used to get/set the {@link CloudMediaProvider} authority.
     *
     * {@hide}
     */
    public static final String EXTRA_AUTHORITY = "android.provider.extra.authority";

    /**
     * URI path for {@link CloudMediaProvider#onQueryMedia}
     *
     * {@hide}
     */
    public static final String URI_PATH_MEDIA = "media";

    /**
     * URI path for {@link CloudMediaProvider#onQueryDeletedMedia}
     *
     * {@hide}
     */
    public static final String URI_PATH_DELETED_MEDIA = "deleted_media";

    /**
     * URI path for {@link CloudMediaProvider#onQueryAlbums}
     *
     * {@hide}
     */
    public static final String URI_PATH_ALBUM = "album";

    /**
     * URI path for {@link CloudMediaProvider#onGetMediaCollectionInfo}
     *
     * {@hide}
     */
    public static final String URI_PATH_MEDIA_COLLECTION_INFO = "media_collection_info";

    /**
     * URI path for {@link CloudMediaProvider#onCreateCloudMediaSurfaceController}
     *
     * {@hide}
     */
    public static final String URI_PATH_SURFACE_CONTROLLER = "surface_controller";

    /**
     * URI path for {@link CloudMediaProvider#onQueryMediaCategories}
     *
     * @hide
     */
    public static final String URI_PATH_MEDIA_CATEGORY = "media_category";

    /**
     * URI path for {@link CloudMediaProvider#onQueryMediaSets}
     *
     * @hide
     */
    public static final String URI_PATH_MEDIA_SET = "media_set";

    /**
     * URI path for {@link CloudMediaProvider#onQuerySearchSuggestions}
     *
     * @hide
     */
    public static final String URI_PATH_SEARCH_SUGGESTION = "search_suggestion";

    /**
     * URI path for {@link CloudMediaProvider#onSearchMedia}
     *
     * @hide
     */
    public static final String URI_PATH_SEARCH_MEDIA = "search_media";

    /**
     * URI path for {@link CloudMediaProvider#onQueryMediaInMediaSet}
     *
     * @hide
     */
    public static final String URI_PATH_MEDIA_IN_MEDIA_SET =
            "query_media_in_media_set";

    /**
     * Key for passing parent category Id as a parameter in the bundle
     *
     * @hide
     */
    public static final String KEY_PARENT_CATEGORY_ID = "parent_category_id";

    /**
     * Key for passing media category Id as a parameter in the bundle
     *
     * @hide
     */
    public static final String KEY_MEDIA_CATEGORY_ID = "media_category_id";

    /**
     * Key for passing media set Id as a parameter in the bundle
     *
     * @hide
     */
    public static final String KEY_MEDIA_SET_ID = "media_set_id";

    /**
     * Key for passing prefix text as a parameter in the bundle
     *
     * @hide
     */
    public static final String KEY_PREFIX_TEXT = "prefix_text";

    /**
     * Key for passing search query as a parameter in the bundle
     *
     * @hide
     */
    public static final String KEY_SEARCH_TEXT = "search_text";

    /**
     * MediaSet represents a cohesive collection of related unique media items,
     * sharing a common meaningful context or theme.
     * This is the basic and fundamental unit for organizing related media items.
     *
     * MediaSet in this context is represented
     * by a set of columns present in {@link MediaSetColumns}
     *
     * Examples of media sets include:
     * <ul>
     *   <li>Faces of the same person</li>
     *   <li>Photos of a specific location</li>
     *   <li>All media as a search result to mountains</li>
     * </ul>
     *
     *  Note: {@link AlbumColumns} which denotes an album can also be represented
     *  using {@link MediaSetColumns}. But, it is recommended to keep using {@link AlbumColumns}
     *  for existing user albums and use MediaSet only for supported MediaCategories .
     *
     * The currently supported MediaCategory in photo picker are
     * {@link #MEDIA_CATEGORY_TYPE_PEOPLE_AND_PETS}.
     *
     * These are the fields of a MediaSet.
     *
     * @see MediaCategoryColumns
     */
    @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
    public static final class MediaSetColumns {

        private MediaSetColumns() {}

        /**
         * Unique ID of the media set. This ID is both provided by and interpreted
         * by the {@link CloudMediaProvider}.
         *
         * Each media set must have a unique ID.
         *
         * A provider should return IDs which are stable,
         * meaning it remains the same if nothing inside it changes,
         * since they will be used to cache media set information in the OS.
         *
         * Type: STRING
         */
        public static final String ID = "id";

        /**
         * Display name of the media set.
         * This display name provided should match the current devices locale settings.
         * If there is no display name, pass {@code null} in this column.
         *
         * Type: STRING
         */
        public static final String DISPLAY_NAME = "display_name";

        /**
         * Total count of all media within the media set, including photos and videos.
         *
         * If this field is not provided,
         * media sets will be shown without a count in the Photo Picker.
         *
         * Type: LONG
         */
        public static final String MEDIA_COUNT = "media_count";

        /**
         * Media ID to use as the media set cover photo.
         *
         * If this field is not provided,
         * media sets will be shown in the Photo Picker with a default icon.
         *
         * Type: STRING
         *
         * @see CloudMediaProviderContract.MediaColumns#ID
         */
        public static final String MEDIA_COVER_ID = "media_cover_id";

        /**
         * Contains all column names for {@link MediaSetColumns} as an array.
         * @hide
         */
        public static final String[] ALL_PROJECTION = new String[] {
                MediaSetColumns.ID,
                MediaSetColumns.DISPLAY_NAME,
                MediaSetColumns.MEDIA_COUNT,
                MediaSetColumns.MEDIA_COVER_ID
        };
    }

    /**
     * MediaCategory represents a broader structure
     * that a {@link MediaSetColumns} or another {@link MediaCategoryColumns} belongs to.
     *
     * A MediaCategory in this context is represented by a set of columns present in
     * {@link MediaCategoryColumns}
     *
     * A MediaCategory can have instances of other MediaCategories
     * to support a multilevel hierarchy.
     * Examples of MediaCategory:
     * <ul>
     *   <li>A MediaCategory of people and pet faces which contains instances of MediaSets
     *   for different faces</li>
     *   <li>A MediaCategory of locations which contains instances of MediaSets for
     *   different locations</li>
     * </ul>
     *
     * The currently supported MediaCategory in photo picker are
     * {@link #MEDIA_CATEGORY_TYPE_PEOPLE_AND_PETS}.
     *
     * These are the fields of MediaCategory.
     * @see CloudMediaProvider#onQueryMediaCategories
     */
    @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
    public static final class MediaCategoryColumns {

        private MediaCategoryColumns() {}

        /**
         * The unique identifier of the media category.
         * This ID is both provided by and interpreted by the {@link CloudMediaProvider}.
         *
         * A provider should return IDs which are stable,
         * meaning it remains the same if nothing inside it changes,
         * since they will be used to cache information in the OS.
         *
         * Type: STRING
         */
        public static final String ID = "id";

        /**
         * The display name of the media category.
         * This display name provided should match the current devices locale settings.
         *
         * If there is no display name, pass {@code null} in this column.
         *
         * Type: STRING
         */
        public static final String DISPLAY_NAME = "display_name";

        /**
         * The type of the media category.
         * This must contain one of the values from the supported media category types.
         * Currently supported types are:
         *      {@link #MEDIA_CATEGORY_TYPE_PEOPLE_AND_PETS}
         *      {@link #MEDIA_CATEGORY_TYPE_USER_ALBUMS}
         *
         * Type: INTEGER
         */
        public static final String MEDIA_CATEGORY_TYPE = "media_category_type";

        /**
         * The first cover media ID for displaying.
         * <p>
         * If none of the MEDIA_COVER_ID is provided,
         * media category will be shown in the Photo Picker with a default icon.
         * Otherwise, Photo Picker will show as many MEDIA_COVER_IDs as provided.
         * <p>
         * Type: STRING
         */
        public static final String MEDIA_COVER_ID1 = "media_cover_id1";

        /**
         * The second cover media ID for displaying.
         * <p>
         * If none of the MEDIA_COVER_ID is provided,
         * media category will be shown in the Photo Picker with a default icon.
         * Otherwise, Photo Picker will show as many MEDIA_COVER_IDs as provided.
         * <p>
         * Type: STRING
         */
        public static final String MEDIA_COVER_ID2 = "media_cover_id2";

        /**
         * The third cover media ID for displaying.
         * <p>
         * If none of the MEDIA_COVER_ID is provided,
         * media category will be shown in the Photo Picker with a default icon.
         * Otherwise, Photo Picker will show as many MEDIA_COVER_IDs as provided.
         * <p>
         * Type: STRING
         */
        public static final String MEDIA_COVER_ID3 = "media_cover_id3";

        /**
         * The fourth cover media ID for displaying.
         * <p>
         * If none of the MEDIA_COVER_ID is provided,
         * media category will be shown in the Photo Picker with a default icon.
         * Otherwise, Photo Picker will show as many MEDIA_COVER_IDs as provided.
         * <p>
         * Type: STRING
         */
        public static final String MEDIA_COVER_ID4 = "media_cover_id4";

        /**
         * Contains all column names for {@link MediaCategoryColumns} as an array.
         *
         * @hide
         */
        public static final String[] ALL_PROJECTION = new String[] {
                MediaCategoryColumns.ID,
                MediaCategoryColumns.DISPLAY_NAME,
                MediaCategoryColumns.MEDIA_CATEGORY_TYPE,
                MediaCategoryColumns.MEDIA_COVER_ID1,
                MediaCategoryColumns.MEDIA_COVER_ID2,
                MediaCategoryColumns.MEDIA_COVER_ID3,
                MediaCategoryColumns.MEDIA_COVER_ID4
        };

    }

    /**
     * Represents media category related to faces of people and pets.
     * @see MediaCategoryColumns#MEDIA_CATEGORY_TYPE
     * Type: STRING
     */
    @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
    public static final String MEDIA_CATEGORY_TYPE_PEOPLE_AND_PETS =
            "com.android.providers.media.MEDIA_CATEGORY_TYPE_PEOPLE_AND_PETS";

    /**
     * Represents media category related to a user's custom albums.
     * @see MediaCategoryColumns#MEDIA_CATEGORY_TYPE
     * Type: STRING
     *
     * @hide
     */
    public static final String MEDIA_CATEGORY_TYPE_USER_ALBUMS =
            "com.android.providers.media.MEDIA_CATEGORY_TYPE_USER_ALBUMS";

    /**
     * Defines the types of media categories available and supported in photo picker.
     * All MediaCategories returned must be of any type from the fields available in this class.
     *
     * @see MediaCategoryColumns#MEDIA_CATEGORY_TYPE
     * @hide
     */
    @StringDef(value = {
            MEDIA_CATEGORY_TYPE_PEOPLE_AND_PETS,
            MEDIA_CATEGORY_TYPE_USER_ALBUMS
    })
    @Retention(SOURCE)
    public @interface MediaCategoryType {}

    /**
     * Represents a search suggestion provided by the {@link CloudMediaProvider}.
     * This is based on the user entered query.
     * When the input query is empty (zero state), the provider can still return suggestions.
     * Photo picker will show these zero state suggestions to the user,
     * when nothing has been typed for search.
     *
     * This class contains the fields of SearchSuggestion.
     *
     * @see CloudMediaProvider#onQuerySearchSuggestions
     */
    @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
    public static final class SearchSuggestionColumns {

        private SearchSuggestionColumns() {}

        /**
         * The unique identifier of the media set associated with the search suggestion.
         * This will be used to query media items if user clicked on this suggestion.
         *
         * <p>
         * Type: STRING
         *
         * @see MediaSetColumns#ID
         */
        public static final String MEDIA_SET_ID = "media_set_id";
        /**
         * The display text for the search suggestion.
         * <p>
         * This is the text shown to the user as a suggestion.
         * Display text provided should match the current devices locale settings.
         *
         * If no display text, pass {@code null} in this column.
         *
         * <p>
         * Type: STRING
         */
        public static final String DISPLAY_TEXT = "display_text";
        /**
         * The type of the search suggestion.
         * <p>
         * This must contain one of the values from various supported search suggestion types.
         * These are: {@link #SEARCH_SUGGESTION_TEXT},  {@link #SEARCH_SUGGESTION_FACE},
         *  {@link #SEARCH_SUGGESTION_DATE},  {@link #SEARCH_SUGGESTION_LOCATION},
         *  {@link #SEARCH_SUGGESTION_ALBUM}
         * <p>
         * This will be used to display to user different suggestions in different way.
         * As examples: for Location type, a thumbnail of location will be used.
         * For faces, face cover id (if provided) will be used.
         * Type: INTEGER
         */
        public static final String TYPE = "type";

        /**
         * Media ID to use as the cover image for the search suggestion.
         * <p>
         * If this field is not provided,
         * the search suggestion will be shown with a default cover.
         * <p>
         * Type: LONG
         */
        public static final String MEDIA_COVER_ID = "media_cover_id";

        /**
         * Contains all column names for {@link SearchSuggestionColumns} as an array.
         *
         * @hide
         */
        public static final String[] ALL_PROJECTION = new String[] {
                SearchSuggestionColumns.MEDIA_SET_ID,
                SearchSuggestionColumns.DISPLAY_TEXT,
                SearchSuggestionColumns.TYPE,
                SearchSuggestionColumns.MEDIA_COVER_ID
        };
    }

    /**
     * Represents a generic text search suggestion. This can be treated as a default when the type
     * of search suggestions is unknown.
     * @see SearchSuggestionColumns#TYPE
     * Type: STRING
     */
    @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
    public static final String SEARCH_SUGGESTION_TEXT =
            "com.android.providers.media.SEARCH_SUGGESTION_TEXT";

    /**
     * Suggestion based on faces detected in photos.
     * @see SearchSuggestionColumns#TYPE
     * Type: STRING
     */
    @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
    public static final String SEARCH_SUGGESTION_FACE =
            "com.android.providers.media.SEARCH_SUGGESTION_FACE";

    /**
     * Suggestion based on location data associated with photos.
     * @see SearchSuggestionColumns#TYPE
     * Type: STRING
     */
    @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
    public static final String SEARCH_SUGGESTION_LOCATION =
            "com.android.providers.media.SEARCH_SUGGESTION_LOCATION";

    /**
     * Suggestion based on the date photos were taken.
     * @see SearchSuggestionColumns#TYPE
     * Type: STRING
     */
    @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
    public static final String SEARCH_SUGGESTION_DATE =
            "com.android.providers.media.SEARCH_SUGGESTION_DATE";


    /**
     * Suggestion based on user albums.
     * @see SearchSuggestionColumns#TYPE
     * Type: STRING
     */
    @FlaggedApi(Flags.FLAG_CLOUD_MEDIA_PROVIDER_SEARCH)
    public static final String SEARCH_SUGGESTION_ALBUM =
            "com.android.providers.media.SEARCH_SUGGESTION_ALBUM";

    /**
     * Suggestion based on user's search history.
     * @see SearchSuggestionColumns#TYPE
     * Type: STRING
     *
     * @hide
     */
    public static final String SEARCH_SUGGESTION_HISTORY =
            "com.android.providers.media.SEARCH_SUGGESTION_HISTORY";

    /**
     * Favorite's album suggestion
     * @see SearchSuggestionColumns#TYPE
     * Type: STRING
     *
     * @hide
     */
    public static final String SEARCH_SUGGESTION_FAVORITES_ALBUM =
            "com.android.providers.media.SEARCH_SUGGESTION_FAVORITES_ALBUM";

    /**
     * Screenshot's album suggestion
     * @see SearchSuggestionColumns#TYPE
     * Type: STRING
     *
     * @hide
     */
    public static final String SEARCH_SUGGESTION_SCREENSHOTS_ALBUM =
            "com.android.providers.media.SEARCH_SUGGESTION_SCREENSHOTS_ALBUM";

    /**
     * Videos's album suggestion
     * @see SearchSuggestionColumns#TYPE
     * Type: STRING
     *
     * @hide
     */
    public static final String SEARCH_SUGGESTION_VIDEOS_ALBUM =
            "com.android.providers.media.SEARCH_SUGGESTION_VIDEOS_ALBUM";

    /**
     * Defines the different types of search suggestions available and supported in photo picker.
     *
     * @see SearchSuggestionColumns#TYPE
     * @hide
     */
    @StringDef(value = {
            SEARCH_SUGGESTION_TEXT,
            SEARCH_SUGGESTION_FACE,
            SEARCH_SUGGESTION_LOCATION,
            SEARCH_SUGGESTION_DATE,
            SEARCH_SUGGESTION_ALBUM,
            SEARCH_SUGGESTION_HISTORY
    })
    @Retention(SOURCE)
    public @interface SearchSuggestionType {}
}
