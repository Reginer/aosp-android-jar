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

import android.database.Cursor;
import android.util.Log;

import java.util.List;

/**
 * Helper class to create log strings.
 */
final class VerificationLogsHelper {
    public static final String TAG = "CMP_verifications_";

    /**
     * Verifies that all expected columns are present in the returned cursor.
     */
    static void verifyProjectionForCursor(
            Cursor cursor,
            List<String> expectedColumns,
            List<String> errors
    ) {
        // TODO: ignore non compulsory columns?
        for (String column : expectedColumns) {
            if (cursor.getColumnIndex(column) == -1) {
                errors.add(createColumnNotPresentLog(column));
            }
        }
    }

    /**
     * Verifies and logs whether MediaCollectionId is null or empty.
     */
    static void verifyMediaCollectionId(
            String mediaCollectionId,
            List<String> verificationResult,
            List<String> errors
    ) {
        if (mediaCollectionId != null && !mediaCollectionId.isEmpty()) {
            verificationResult.add(
                    CloudMediaProviderContract.MediaCollectionInfo.MEDIA_COLLECTION_ID
                            + " : " + mediaCollectionId
            );
        } else {
            errors.add(CloudMediaProviderContract.MediaCollectionInfo.MEDIA_COLLECTION_ID
                    + (mediaCollectionId == null ? " is null" : " is empty"));
        }
    }

    /**
     * Verifies and logs if the cursor is null or not and the mediaCollectionId inside it.
     */
    static void verifyCursorNotNullAndMediaCollectionIdPresent(
            Cursor c,
            List<String> verificationResult,
            List<String> errors
    ) {
        if (c != null) {
            verificationResult.add(createIsNotNullLog("Received cursor"));
            verificationResult.add(String.format("Number of items in cursor: %s", c.getCount()));
            verifyMediaCollectionId(
                    c.getExtras().getString(CloudMediaProviderContract.EXTRA_MEDIA_COLLECTION_ID),
                    verificationResult,
                    errors
            );
        } else {
            errors.add(createIsNullLog("Received cursor"));
        }
    }

    /**
     * Helps log an error for when execution time for an API exceeds the designated threshold.
     */
    static void verifyTotalTimeForExecution(long totalTimeTakenForExecution, long threshold,
            List<String> errors) {
        if (threshold < totalTimeTakenForExecution) {
            errors.add("Total time for execution exceeded threshold.  threshold = " + threshold
                    + "ms, totalTimeForExecution = " + totalTimeTakenForExecution + "ms");
        }
    }

    static String createIsNotNullLog(String value) {
        return String.format("%s is not null.", value);
    }

    static String createIsNullLog(String value) {
        return String.format("%s is null.", value);
    }

    static String createIsNotValidLog(String value) {
        return String.format("%s is not valid.", value);
    }

    static String createColumnNotPresentLog(String value) {
        return String.format("%s column is not present in the returned cursor.", value);
    }

    static void logVerifications(String authority, String apiName,
            long totalTimeTakenForExecution,
            List<String> verifications, List<String> errors) {
        StringBuilder strb = new StringBuilder("Verifications for : " + apiName + "\n");
        strb.append("\tTotal time for execution: ").append(totalTimeTakenForExecution).append(
                "ms \n");
        if (!verifications.isEmpty()) {
            strb.append("\tVerifications:\n");
            for (String verification : verifications) {
                strb.append("\t\t").append(verification).append("\n");
            }
        }
        if (!errors.isEmpty()) {
            strb.append("\tErrors:\n");
            for (String error : errors) {
                strb.append("\t\t").append(error).append("\n");
            }
        }
        Log.d(TAG + authority, strb.toString());
    }

    static void logException(String exceptionMessage) {
        Log.d(TAG + "Exception", exceptionMessage);
    }
}
