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

package android.graphics.pdf.logging;

import android.annotation.IntDef;
import android.graphics.pdf.PdfStatsLog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class to log all the PDFViewer event related data to statsD.
 *
 * @hide
 */
public class PdfEventLogger {

    private final int mProcessId;
    private final long mDocId;

    /**
     * Creates a new object for the PdfEventLogger class.
     *
     * @param processId the uid of the process which is calling PdfViewer APIs.
     * @param docId     Unique identifier for a particular loaded document.
     */
    public PdfEventLogger(int processId, long docId) {
        mProcessId = processId;
        mDocId = docId;
    }

    /**
     * Logs data related to Pdf search events to statsD.
     *
     * @param loadDurationMillis Time take to load the search result.
     * @param queryLength        The number of characters in the search query.
     * @param queryPageNumber    The page number of the query.
     * @param apiResponse        The response type of the search API call.
     * @param numPages           The number of pages in the pdf being searched
     * @param matchCount         The number of matches for a particular query.
     * @see com.android.os.pdf.PdfSearchReported
     */
    public void logSearchReportedEvent(
            long loadDurationMillis,
            int queryLength,
            int queryPageNumber,
            @ApiResponseTypes.ApiResponseType int apiResponse,
            int numPages,
            int matchCount) {
        PdfStatsLog.write(PdfStatsLog.PDF_SEARCH_REPORTED, mProcessId, loadDurationMillis,
                queryLength, queryPageNumber, apiResponse, mDocId, numPages, matchCount);
    }

    /**
     * Logs data related to Pdf load events to statsD.
     *
     * @param loadDurationMillis the time taken to load a PDF document in milliseconds.
     * @param pdfSizeInKb        The size of the PDF document which is loaded
     * @param pdfLoadResult      The result/ status of a PDF document which is loaded
     * @param linearizationType  The linearization type of the PDF document which is loaded
     * @param numPages           The number of pages in PDF document which is loaded
     * @see com.android.os.pdf.PdfLoadReported
     */
    public void logPdfLoadReportedEvent(
            long loadDurationMillis,
            float pdfSizeInKb,
            @PdfLoadResults.PdfLoadResult int pdfLoadResult,
            @LinearizationTypes.LinearizationType int linearizationType,
            int numPages) {
        PdfStatsLog.write(PdfStatsLog.PDF_LOAD_REPORTED, mProcessId, loadDurationMillis,
                pdfSizeInKb, pdfLoadResult, linearizationType, numPages, mDocId);
    }

    /**
     * Logs data related to Pdf API usage events to statsD.
     *
     * @param apiType     The type of API being called.
     * @param apiResponse The response type of the API call.
     * @see com.android.os.pdf.PdfApiUsageReported
     */
    public void logPdfApiUsageReportedEvent(
            @ApiTypes.ApiType int apiType,
            @ApiResponseTypes.ApiResponseType int apiResponse) {
        PdfStatsLog.write(PdfStatsLog.PDF_API_USAGE_REPORTED, mProcessId, mDocId, apiType,
                apiResponse);
    }

    // Represent the linearization type of the PDF document.
    public static final class LinearizationTypes {
        public static final int UNKNOWN = PdfStatsLog.PDF_LOAD_REPORTED__TYPE__UNKNOWN_TYPE;
        public static final int LINEARIZED = PdfStatsLog.PDF_LOAD_REPORTED__TYPE__LINEARIZED_TYPE;
        public static final int NON_LINEARIZED =
                PdfStatsLog.PDF_LOAD_REPORTED__TYPE__NON_LINEARIZED_TYPE;

        @IntDef({UNKNOWN, LINEARIZED, NON_LINEARIZED})
        @Retention(RetentionPolicy.RUNTIME)
        public @interface LinearizationType {
        }
    }

    // Represents the status of the PDF load called on a document.
    public static final class PdfLoadResults {
        public static final int UNKNOWN =
                PdfStatsLog.PDF_LOAD_REPORTED__LOAD_RESULT__RESULT_UNKNOWN;
        public static final int LOADED = PdfStatsLog.PDF_LOAD_REPORTED__LOAD_RESULT__RESULT_LOADED;
        public static final int ERROR = PdfStatsLog.PDF_LOAD_REPORTED__LOAD_RESULT__RESULT_ERROR;
        public static final int WRONG_PASSWORD =
                PdfStatsLog.PDF_LOAD_REPORTED__LOAD_RESULT__RESULT_WRONG_PASSWORD;

        @IntDef({UNKNOWN, LOADED, ERROR, WRONG_PASSWORD})
        @Retention(RetentionPolicy.RUNTIME)
        public @interface PdfLoadResult {
        }
    }

    // Represents the types of PdfViewer API being called.
    public static final class ApiTypes {
        public static final int UNKNOWN =
                PdfStatsLog.PDF_API_USAGE_REPORTED__API_TYPE__API_TYPE_UNKNOWN;
        public static final int SELECT_CONTENT =
                PdfStatsLog.PDF_API_USAGE_REPORTED__API_TYPE__API_TYPE_SELECT_CONTENT;

        @IntDef({UNKNOWN, SELECT_CONTENT})
        @Retention(RetentionPolicy.RUNTIME)
        public @interface ApiType {
        }
    }

    // Represents the response of the API (success/failure) call.
    public static final class ApiResponseTypes {
        public static final int UNKNOWN =
                PdfStatsLog.PDF_API_USAGE_REPORTED__API_RESPONSE_STATUS__RESPONSE_UNKNOWN;
        public static final int SUCCESS =
                PdfStatsLog.PDF_API_USAGE_REPORTED__API_RESPONSE_STATUS__RESPONSE_SUCCESS;
        public static final int FAILURE =
                PdfStatsLog.PDF_API_USAGE_REPORTED__API_RESPONSE_STATUS__RESPONSE_FAILURE;

        @IntDef({UNKNOWN, SUCCESS, FAILURE})
        @Retention(RetentionPolicy.RUNTIME)
        public @interface ApiResponseType {
        }
    }

}
