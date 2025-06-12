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

import static android.provider.VerificationLogsHelper.createIsNotNullLog;
import static android.provider.VerificationLogsHelper.createIsNotValidLog;
import static android.provider.VerificationLogsHelper.createIsNullLog;
import static android.provider.VerificationLogsHelper.logVerifications;
import static android.provider.VerificationLogsHelper.verifyCursorNotNullAndMediaCollectionIdPresent;
import static android.provider.VerificationLogsHelper.verifyMediaCollectionId;
import static android.provider.VerificationLogsHelper.verifyProjectionForCursor;
import static android.provider.VerificationLogsHelper.verifyTotalTimeForExecution;

import android.annotation.StringDef;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.util.Log;

import com.android.providers.media.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides helper methods that help verify that the received results from cloud provider
 * implementations are staying true to contract by returning non null outputs and setting required
 * extras/states in the result.
 *
 * Note: logs for local provider and not printed.
 */
final class CmpApiVerifier {
    private static final String LOCAL_PROVIDER_AUTHORITY =
            "com.android.providers.media.photopicker";

    private static boolean isCloudMediaProviderLoggingEnabled() {
        return (SystemProperties.getInt("ro.debuggable", 0) == 1) && Log.isLoggable(
                "CloudMediaProvider", Log.VERBOSE);
    }

    /**
     * Verifies and logs results received by CloudMediaProvider Apis.
     *
     * <p><b>Note:</b> It only logs the errors and does not throw any exceptions.
     */
    static void verifyApiResult(CmpApiResult result, long totalTimeTakenForExecution,
            String authority) {
        // Do not perform any operation if the authority is of the local provider or when the
        // logging is not enabled.
        if (!LOCAL_PROVIDER_AUTHORITY.equals(authority)
                && isCloudMediaProviderLoggingEnabled()) {
            try {
                ArrayList<String> verificationResult = new ArrayList<>();
                ArrayList<String> errors = new ArrayList<>();
                verifyTotalTimeForExecution(totalTimeTakenForExecution,
                        CMP_API_TO_THRESHOLD_MAP.get(result.getApi()), errors);

                switch (result.getApi()) {
                    case CloudMediaProviderApis.OnGetCapabilities: {
                        verifyOnGetCapabilities(result.getBundle(), verificationResult, errors);
                        break;
                    }
                    case CloudMediaProviderApis.OnGetMediaCollectionInfo: {
                        verifyOnGetMediaCollectionInfo(result.getBundle(), verificationResult,
                                errors);
                        break;
                    }
                    case CloudMediaProviderApis.OnQueryMedia: {
                        verifyOnQueryMedia(result.getCursor(), verificationResult, errors);
                        break;
                    }
                    case CloudMediaProviderApis.OnQueryDeletedMedia: {
                        verifyOnQueryDeletedMedia(result.getCursor(), verificationResult, errors);
                        break;
                    }
                    case CloudMediaProviderApis.OnQueryAlbums: {
                        verifyOnQueryAlbums(result.getCursor(), verificationResult, errors);
                        break;
                    }
                    case CloudMediaProviderApis.OnOpenPreview: {
                        verifyOnOpenPreview(result.getAssetFileDescriptor(), result.getDimensions(),
                                verificationResult, errors);
                        break;
                    }
                    case CloudMediaProviderApis.OnOpenMedia: {
                        verifyOnOpenMedia(result.getParcelFileDescriptor(), verificationResult,
                                errors);
                        break;
                    }
                    case CloudMediaProviderApis.OnQueryMediaCategories: {
                        verifyOnQueryMediaCategories(result.getCursor(),
                                verificationResult, errors);
                        break;
                    }
                    case CloudMediaProviderApis.OnQueryMediaSets: {
                        verifyOnQueryMediaSets(result.getCursor(),
                                verificationResult, errors);
                        break;
                    }
                    case CloudMediaProviderApis.OnQuerySearchSuggestions: {
                        verifyOnQuerySearchSuggestions(result.getCursor(),
                                verificationResult, errors);
                        break;
                    }
                    case CloudMediaProviderApis.OnSearchMedia: {
                        verifyOnSearchMedia(result.getCursor(),
                                verificationResult, errors);
                        break;
                    }
                    case CloudMediaProviderApis.OnQueryMediaInMediaSet: {
                        verifyOnQueryMediaInMediaSet(result.getCursor(),
                                verificationResult, errors);
                    }
                    default:
                        throw new UnsupportedOperationException(
                                "The verification for requested API is not supported.");
                }
                logVerifications(authority, result.getApi(), totalTimeTakenForExecution,
                        verificationResult, errors);
            } catch (Exception e) {
                VerificationLogsHelper.logException(e.getMessage());
            }
        }
    }

    /**
     * Verifies the {@link CloudMediaProvider#onGetCapabilities()} API.
     *
     * Verifies the Capabilities object returned is non-null.
     */
    static void verifyOnGetCapabilities(
            Bundle outputBundle, List<String> verificationResult, List<String> errors) {

        // Only Verify if the flag for capabilities is on.
        if (Flags.enableCloudMediaProviderCapabilities()) {

            if (outputBundle != null
                    && outputBundle.containsKey(
                            CloudMediaProviderContract.EXTRA_PROVIDER_CAPABILITIES)) {

                verificationResult.add("Capabilities is present.");

                CloudMediaProviderContract.Capabilities capabilities = outputBundle
                        .getParcelable(CloudMediaProviderContract.EXTRA_PROVIDER_CAPABILITIES);

                // Verify CMP search capabilities if the search flag is on.
                if (Flags.cloudMediaProviderSearch()) {
                    if (capabilities.isAlbumsAsCategoryEnabled()
                            && !capabilities.isMediaCategoriesEnabled()) {
                        errors.add(createIsNotValidLog("Declared capabilities are invalid. "
                                + "AlbumsAsCategory capability can only be enabled when "
                                + "MediaCollections is enabled."));
                    } else {
                        verificationResult.add("Declared Capabilities are valid.");
                    }

                }
            } else {
                errors.add(createIsNullLog("Capabilities were not returned by OnGetCapabilities"));
            }
        }
    }

    /**
     * Verifies OnGetMediaCollectionInfo API by performing and logging the following checks:
     *
     * <ul>
     * <li>Received Bundle is not null.</li>
     * <li>Bundle contains media collection ID:
     * {@link CloudMediaProviderContract.MediaCollectionInfo#MEDIA_COLLECTION_ID}</li>
     * <li>Bundle contains last sync generation:
     * {@link CloudMediaProviderContract.MediaCollectionInfo#LAST_MEDIA_SYNC_GENERATION}</li>
     * <li>Bundle contains account name:
     * {@link CloudMediaProviderContract.MediaCollectionInfo#ACCOUNT_NAME}</li>
     * <li>Bundle contains account configuration intent:
     * {@link CloudMediaProviderContract.MediaCollectionInfo#ACCOUNT_CONFIGURATION_INTENT}</li>
     * </ul>
     */
    static void verifyOnGetMediaCollectionInfo(
            Bundle outputBundle, List<String> verificationResult, List<String> errors
    ) {
        if (outputBundle != null) {
            verificationResult.add(createIsNotNullLog("Received bundle"));

            String mediaCollectionId = outputBundle.getString(
                    CloudMediaProviderContract.MediaCollectionInfo.MEDIA_COLLECTION_ID
            );
            // verifies media collection id.
            verifyMediaCollectionId(
                    mediaCollectionId,
                    verificationResult,
                    errors
            );

            long syncGeneration = outputBundle.getLong(
                    CloudMediaProviderContract.MediaCollectionInfo.LAST_MEDIA_SYNC_GENERATION,
                    -1L
            );

            // verified last sync generation.
            if (syncGeneration != -1L) {
                if (syncGeneration >= 0) {
                    verificationResult.add(
                            CloudMediaProviderContract.MediaCollectionInfo
                                    .LAST_MEDIA_SYNC_GENERATION + " : " + syncGeneration
                    );
                } else {
                    errors.add(
                            CloudMediaProviderContract.MediaCollectionInfo
                                    .LAST_MEDIA_SYNC_GENERATION + " is < 0"
                    );
                }
            } else {
                errors.add(
                        createIsNotValidLog(
                                CloudMediaProviderContract.MediaCollectionInfo
                                        .LAST_MEDIA_SYNC_GENERATION
                        )
                );
            }

            String accountName = outputBundle.getString(
                    CloudMediaProviderContract.MediaCollectionInfo.ACCOUNT_NAME
            );

            // verifies account name.
            if (accountName != null) {
                if (!accountName.isEmpty()) {
                    // In future if the cloud media provider is extended to have multiple
                    // accounts then logging account name itself might be a useful
                    // information to log but for now only logging its presence.
                    verificationResult.add(
                            CloudMediaProviderContract.MediaCollectionInfo.ACCOUNT_NAME
                                    + " is present "
                    );
                } else {
                    errors.add(
                            CloudMediaProviderContract.MediaCollectionInfo.ACCOUNT_NAME
                                    + " is empty"
                    );
                }
            } else {
                errors.add(createIsNullLog(
                                CloudMediaProviderContract.MediaCollectionInfo.ACCOUNT_NAME
                        )
                );
            }

            Intent intent = outputBundle.getParcelable(
                    CloudMediaProviderContract.MediaCollectionInfo.ACCOUNT_CONFIGURATION_INTENT
            );
            // verified the presence of account configuration intent.
            if (intent != null) {
                verificationResult.add(
                        CloudMediaProviderContract.MediaCollectionInfo
                                .ACCOUNT_CONFIGURATION_INTENT
                                + " is present."
                );
            } else {
                errors.add(createIsNullLog(
                                CloudMediaProviderContract.MediaCollectionInfo
                                        .ACCOUNT_CONFIGURATION_INTENT
                        )
                );
            }

        } else {
            errors.add(createIsNullLog("Received output bundle"));
        }
    }

    /**
     * Verifies OnQueryMedia API by performing and logging the following checks:
     *
     * <ul>
     * <li>Received Cursor is not null.</li>
     * <li>Cursor contains non empty media collection ID:
     * {@link CloudMediaProviderContract#EXTRA_MEDIA_COLLECTION_ID}</li>
     * <li>Projection for cursor is as expected:
     * {@link CloudMediaProviderContract.MediaColumns#ALL_PROJECTION}</li>
     * <li>Logs count of rows in the cursor, if cursor is non null.</li>
     * </ul>
     */
    static void verifyOnQueryMedia(
            Cursor c, List<String> verificationResult, List<String> errors
    ) {
        if (c != null) {
            verifyCursorNotNullAndMediaCollectionIdPresent(
                    c,
                    verificationResult,
                    errors
            );
            // verify that all columns are present per CloudMediaProviderContract.AlbumColumns
            verifyProjectionForCursor(
                    c,
                    Arrays.asList(CloudMediaProviderContract.MediaColumns.ALL_PROJECTION),
                    errors
            );
        } else {
            errors.add(createIsNullLog("Received cursor"));
        }
    }

    /**
     * Verifies OnQueryDeletedMedia API by performing and logging the following checks:
     *
     * <ul>
     * <li>Received Cursor is not null.</li>
     * <li>Cursor contains non empty media collection ID:
     * {@link CloudMediaProviderContract#EXTRA_MEDIA_COLLECTION_ID}</li>
     * <li>Logs count of rows in the cursor, if cursor is non null.</li>
     * </ul>
     */
    static void verifyOnQueryDeletedMedia(
            Cursor c, List<String> verificationResult, List<String> errors
    ) {
        verifyCursorNotNullAndMediaCollectionIdPresent(c, verificationResult, errors);
    }

    /**
     * Verifies OnQueryAlbums API by performing and logging the following checks:
     *
     * <ul>
     * <li>Received Cursor is not null.</li>
     * <li>Cursor contains non empty media collection ID:
     * {@link CloudMediaProviderContract#EXTRA_MEDIA_COLLECTION_ID}</li>
     * <li>Projection for cursor is as expected:
     * {@link CloudMediaProviderContract.AlbumColumns#ALL_PROJECTION}</li>
     * <li>Logs count of rows in the cursor and the album names, if cursor is non null.</li>
     * </ul>
     */
    static void verifyOnQueryAlbums(
            Cursor c, List<String> verificationResult, List<String> errors
    ) {
        if (c != null) {
            verifyCursorNotNullAndMediaCollectionIdPresent(c, verificationResult, errors);

            // verify that all columns are present per CloudMediaProviderContract.AlbumColumns
            verifyProjectionForCursor(
                    c,
                    Arrays.asList(CloudMediaProviderContract.AlbumColumns.ALL_PROJECTION),
                    errors
            );
            if (c.getCount() > 0) {
                // Only log album data if projection and other checks have returned positive
                // results.
                StringBuilder strBuilder = new StringBuilder("Albums present and their count: ");
                int columnIndexForId = c.getColumnIndex(CloudMediaProviderContract.AlbumColumns.ID);
                int columnIndexForItemCount = c.getColumnIndex(
                        CloudMediaProviderContract.AlbumColumns.MEDIA_COUNT);
                c.moveToPosition(-1);
                while (c.moveToNext()) {
                    strBuilder.append("\n\t\t\t" + c.getString(columnIndexForId) + ", " + c.getLong(
                            columnIndexForItemCount));
                }
                c.moveToPosition(-1);
                verificationResult.add(strBuilder.toString());
            }
        }
    }

    /**
     * Verifies OnQueryMediaCategories API by performing and logging the following checks:
     *
     * <ul>
     * <li>Received Cursor is not null.</li>
     * <li>Cursor contains non empty media collection ID:
     * {@link CloudMediaProviderContract#EXTRA_MEDIA_COLLECTION_ID}</li>
     * <li>Projection for cursor is as expected:
     * {@link CloudMediaProviderContract.MediaCategoryColumns#ALL_PROJECTION}</li>
     * </ul>
     */
    static void verifyOnQueryMediaCategories(
            Cursor cursor, List<String> verificationResult, List<String> errors
    ) {
        verifyCursorNotNullAndMediaCollectionIdPresent(cursor, verificationResult, errors);
        if (cursor != null && Flags.cloudMediaProviderSearch()) {

            verifyProjectionForCursor(
                    cursor,
                    Arrays.asList(CloudMediaProviderContract.MediaCategoryColumns.ALL_PROJECTION),
                    errors
            );
        }
    }

    /**
     * Verifies OnQueryMediaSets API by performing and logging the following checks:
     *
     * <ul>
     * <li>Received Cursor is not null.</li>
     * <li>Cursor contains non empty media collection ID:
     * {@link CloudMediaProviderContract#EXTRA_MEDIA_COLLECTION_ID}</li>
     * <li>Projection for cursor is as expected:
     * {@link CloudMediaProviderContract.MediaSetColumns#ALL_PROJECTION}</li>
     * </ul>
     */
    static void verifyOnQueryMediaSets(
            Cursor cursor, List<String> verificationResult, List<String> errors
    ) {
        verifyCursorNotNullAndMediaCollectionIdPresent(cursor, verificationResult, errors);
        if (cursor != null && Flags.cloudMediaProviderSearch()) {

            verifyProjectionForCursor(
                    cursor,
                    Arrays.asList(CloudMediaProviderContract.MediaSetColumns.ALL_PROJECTION),
                    errors
            );
        }
    }

    /**
     * Verifies OnQuerySearchSuggestions API by performing and logging the following checks:
     *
     * <ul>
     * <li>Received Cursor is not null.</li>
     * <li>Cursor contains non empty media collection ID:
     * {@link CloudMediaProviderContract#EXTRA_MEDIA_COLLECTION_ID}</li>
     * <li>Projection for cursor is as expected:
     * {@link CloudMediaProviderContract.SearchSuggestionColumns#ALL_PROJECTION}</li>
     * </ul>
     */
    static void verifyOnQuerySearchSuggestions(
            Cursor cursor, List<String> verificationResult, List<String> errors
    ) {
        verifyCursorNotNullAndMediaCollectionIdPresent(cursor, verificationResult, errors);
        if (cursor != null && Flags.cloudMediaProviderSearch()) {

            verifyProjectionForCursor(
                    cursor,
                    Arrays.asList(
                            CloudMediaProviderContract.SearchSuggestionColumns.ALL_PROJECTION),
                    errors
            );
        }
    }

    /**
     * Verifies OnSearchMedia API by performing and logging the following checks:
     *
     * <ul>
     * <li>Received Cursor is not null.</li>
     * <li>Cursor contains non empty media collection ID:
     * {@link CloudMediaProviderContract#EXTRA_MEDIA_COLLECTION_ID}</li>
     * <li>Projection for cursor is as expected:
     * {@link CloudMediaProviderContract.MediaColumns#ALL_PROJECTION}</li>
     * </ul>
     */
    static void verifyOnSearchMedia(
            Cursor cursor, List<String> verificationResult, List<String> errors
    ) {
        verifyCursorNotNullAndMediaCollectionIdPresent(cursor, verificationResult, errors);
        if (cursor != null && Flags.cloudMediaProviderSearch()) {

            verifyProjectionForCursor(
                    cursor,
                    Arrays.asList(
                            CloudMediaProviderContract.MediaColumns.ALL_PROJECTION),
                    errors
            );
        }
    }

    /**
     * Verifies OnQueryMediaInMediaSet by performing and logging the following checks:
     *
     * <ul>
     * <li>Received Cursor is not null.</li>
     * <li>Cursor contains non empty media collection ID:
     * {@link CloudMediaProviderContract#EXTRA_MEDIA_COLLECTION_ID}</li>
     * <li>Projection for cursor is as expected:
     * {@link CloudMediaProviderContract.MediaColumns#ALL_PROJECTION}</li>
     * </ul>
     */
    static void verifyOnQueryMediaInMediaSet(
            Cursor cursor, List<String> verificationResult, List<String> errors
    ) {
        verifyCursorNotNullAndMediaCollectionIdPresent(cursor, verificationResult, errors);
        if (cursor != null && Flags.cloudMediaProviderSearch()) {

            verifyProjectionForCursor(
                    cursor,
                    Arrays.asList(
                            CloudMediaProviderContract.MediaColumns.ALL_PROJECTION),
                    errors
            );
        }
    }



    /**
     * Verifies OnOpenPreview API by performing and logging the following checks:
     *
     * <ul>
     * <li>Received AssetFileDescriptor is not null.</li>
     * <li>Logs size of the thumbnail.</li>
     * </ul>
     */
    static void verifyOnOpenPreview(
            AssetFileDescriptor assetFileDescriptor,
            Point expectedSize, List<String> verificationResult, List<String> errors
    ) {
        if (assetFileDescriptor == null) {
            errors.add(createIsNullLog("Received AssetFileDescriptor"));
        } else {
            verificationResult.add(createIsNotNullLog("Received AssetFileDescriptor"));
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true; // Only decode the bounds
            BitmapFactory.decodeFileDescriptor(assetFileDescriptor.getFileDescriptor(), null,
                    options);

            int width = options.outWidth;
            int height = options.outHeight;

            verificationResult.add("Dimensions of file received: "
                    + "Width: " + width + ", Height: " + height + ", expected: " + expectedSize.x
                    + ", " + expectedSize.y);
        }
    }

    /**
     * Verifies OnOpenMedia API by performing and logging the following checks:
     *
     * <ul>
     * <li>Received ParcelFileDescriptor is not null.</li>
     * </ul>
     */
    static void verifyOnOpenMedia(
            ParcelFileDescriptor fd,
            List<String> verificationResult, List<String> errors
    ) {
        if (fd == null) {
            errors.add(createIsNullLog("Received FileDescriptor"));
        } else {
            verificationResult.add(createIsNotNullLog("Received FileDescriptor"));
        }
    }

    @StringDef({
            CloudMediaProviderApis.OnGetCapabilities,
            CloudMediaProviderApis.OnGetMediaCollectionInfo,
            CloudMediaProviderApis.OnQueryMedia,
            CloudMediaProviderApis.OnQueryDeletedMedia,
            CloudMediaProviderApis.OnQueryAlbums,
            CloudMediaProviderApis.OnOpenPreview,
            CloudMediaProviderApis.OnOpenMedia,
            CloudMediaProviderApis.OnQueryMediaCategories,
            CloudMediaProviderApis.OnQueryMediaSets,
            CloudMediaProviderApis.OnQuerySearchSuggestions,
            CloudMediaProviderApis.OnSearchMedia,
            CloudMediaProviderApis.OnQueryMediaInMediaSet,
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface CloudMediaProviderApis {
        String OnGetCapabilities = "OnGetCapabilities";
        String OnGetMediaCollectionInfo = "onGetMediaCollectionInfo";
        String OnQueryMedia = "onQueryMedia";
        String OnQueryDeletedMedia = "onQueryDeletedMedia";
        String OnQueryAlbums = "onQueryAlbums";
        String OnOpenPreview = "onOpenPreview";
        String OnOpenMedia = "onOpenMedia";
        String OnQueryMediaCategories = "onQueryMediaCategories";
        String OnQueryMediaSets = "onQueryMediaSets";
        String OnQuerySearchSuggestions = "onQuerySearchSuggestions";
        String OnSearchMedia = "onSearchMedia";
        String OnQueryMediaInMediaSet = "onQueryMediaInMediaSet";
    }

    private static final Map<String, Long> CMP_API_TO_THRESHOLD_MAP = new HashMap<>(Map.ofEntries(
            Map.entry(CloudMediaProviderApis.OnGetCapabilities, 200L),
            Map.entry(CloudMediaProviderApis.OnGetMediaCollectionInfo, 200L),
            Map.entry(CloudMediaProviderApis.OnQueryMedia, 500L),
            Map.entry(CloudMediaProviderApis.OnQueryDeletedMedia, 500L),
            Map.entry(CloudMediaProviderApis.OnQueryAlbums, 500L),
            Map.entry(CloudMediaProviderApis.OnOpenPreview, 1000L),
            Map.entry(CloudMediaProviderApis.OnOpenMedia, 1000L),
            Map.entry(CloudMediaProviderApis.OnQueryMediaCategories, 500L),
            Map.entry(CloudMediaProviderApis.OnQueryMediaSets, 500L),
            Map.entry(CloudMediaProviderApis.OnQuerySearchSuggestions, 300L),
            Map.entry(CloudMediaProviderApis.OnSearchMedia, 3000L),
            Map.entry(CloudMediaProviderApis.OnQueryMediaInMediaSet, 500L)
    ));

}
