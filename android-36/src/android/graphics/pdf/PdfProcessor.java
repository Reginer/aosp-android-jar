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

package android.graphics.pdf;

import static android.graphics.pdf.PdfLinearizationTypes.PDF_DOCUMENT_TYPE_LINEARIZED;
import static android.graphics.pdf.PdfLinearizationTypes.PDF_DOCUMENT_TYPE_NON_LINEARIZED;

import android.annotation.FlaggedApi;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.pdf.component.PdfAnnotation;
import android.graphics.pdf.component.PdfAnnotationType;
import android.graphics.pdf.component.PdfPageObject;
import android.graphics.pdf.content.PdfPageGotoLinkContent;
import android.graphics.pdf.content.PdfPageImageContent;
import android.graphics.pdf.content.PdfPageLinkContent;
import android.graphics.pdf.content.PdfPageTextContent;
import android.graphics.pdf.flags.Flags;
import android.graphics.pdf.logging.PdfEventLogger;
import android.graphics.pdf.models.FormEditRecord;
import android.graphics.pdf.models.FormWidgetInfo;
import android.graphics.pdf.models.PageMatchBounds;
import android.graphics.pdf.models.jni.LoadPdfResult;
import android.graphics.pdf.models.selection.PageSelection;
import android.graphics.pdf.models.selection.SelectionBoundary;
import android.graphics.pdf.utils.Preconditions;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a PDF document processing class.
 *
 * @hide
 */
public class PdfProcessor {
    /** Represents a PDF without form fields */
    public static final int PDF_FORM_TYPE_NONE = 0;

    /** Represents a PDF with form fields specified using the AcroForm spec */
    public static final int PDF_FORM_TYPE_ACRO_FORM = 1;

    /** Represents a PDF with form fields specified using the entire XFA spec */
    public static final int PDF_FORM_TYPE_XFA_FULL = 2;

    /** Represents a PDF with form fields specified using the XFAF subset of the XFA spec */
    public static final int PDF_FORM_TYPE_XFA_FOREGROUND = 3;

    private static final String TAG = "PdfProcessor";
    private static final Object sPdfiumLock = new Object();
    private final PdfEventLogger mPdfEventLogger;
    private PdfDocumentProxy mPdfDocument;
    private final HashMap<Integer, PdfPageComponentsIdManager> mPageObjectIdManagerMap;
    private final HashMap<Integer, PdfPageComponentsIdManager> mPageAnnotationsIdManagerMap;

    public PdfProcessor() {
        PdfDocumentProxy.loadLibPdf();

        mPdfEventLogger = new PdfEventLogger(
                /* processId = */ Binder.getCallingUid(),
                /* docId = */ new SecureRandom().nextLong());
        mPageObjectIdManagerMap = new HashMap<>();
        mPageAnnotationsIdManagerMap = new HashMap<>();
    }

    /**
     * Creates an instance of {@link PdfDocumentProxy} on successful loading of the PDF document.
     * This method ensures that an older {@link PdfDocumentProxy} instance is closed and then loads
     * the new document. This method should be run on a {@link android.annotation.WorkerThread} as
     * it is long-running task.
     *
     * @param fileDescriptor {@link ParcelFileDescriptor} for the input PDF document.
     * @param params         instance of {@link LoadParams} which includes the password as well.
     * @throws IOException       if an error occurred during the processing of the PDF document.
     * @throws SecurityException if the password is incorrect.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public void create(ParcelFileDescriptor fileDescriptor, @Nullable LoadParams params)
            throws IOException {
        long loadingStartTime = System.currentTimeMillis();

        Preconditions.checkNotNull(fileDescriptor, "Input FD cannot be null");
        ensurePdfDestroyed();
        try {
            Os.lseek(fileDescriptor.getFileDescriptor(), 0, OsConstants.SEEK_SET);
        } catch (ErrnoException ee) {
            throw new IllegalArgumentException("File descriptor not seekable");
        }

        String password = (params != null) ? params.getPassword() : null;
        synchronized (sPdfiumLock) {
            LoadPdfResult result = PdfDocumentProxy.createFromFd(fileDescriptor.detachFd(),
                    password);
            switch (result.status) {
                case NEED_MORE_DATA, PDF_ERROR, FILE_ERROR -> {

                    mPdfEventLogger.logPdfLoadReportedEvent(
                            /* loadDurationMillis= */ System.currentTimeMillis() - loadingStartTime,
                            /* pdfSizeInKb = */ result.pdfSizeInKb,
                            /* pdfLoadResult = */ PdfEventLogger.PdfLoadResults.ERROR,
                            /* linearizationType = */ PdfEventLogger.LinearizationTypes.UNKNOWN,
                            /* numPages = */ -1);
                    throw new IOException("Unable to load the document!");
                }
                case REQUIRES_PASSWORD -> {

                    mPdfEventLogger.logPdfLoadReportedEvent(
                            /* loadDurationMillis= */ System.currentTimeMillis() - loadingStartTime,
                            /* pdfSizeInKb = */ result.pdfSizeInKb,
                            /* pdfLoadResult = */ PdfEventLogger.PdfLoadResults.WRONG_PASSWORD,
                            /* linearizationType = */ PdfEventLogger.LinearizationTypes.UNKNOWN,
                            /* numPages = */ -1);
                    throw new SecurityException("Password required to access document");
                }
                case LOADED -> {

                    this.mPdfDocument = result.pdfDocument;

                    @PdfEventLogger.LinearizationTypes.LinearizationType int linearizationType =
                            mPdfDocument.isPdfLinearized()
                                    ? PdfEventLogger.LinearizationTypes.LINEARIZED
                                    : PdfEventLogger.LinearizationTypes.NON_LINEARIZED;

                    // Log pdf loaded successfully.
                    mPdfEventLogger.logPdfLoadReportedEvent(
                            /* loadDurationMillis= */ System.currentTimeMillis() - loadingStartTime,
                            /* pdfSizeInKb = */ result.pdfSizeInKb,
                            /* pdfLoadResult = */ PdfEventLogger.PdfLoadResults.LOADED,
                            /* linearizationType = */ linearizationType,
                            /* numPages = */ mPdfDocument.getNumPages());
                }
                default -> {

                    mPdfEventLogger.logPdfLoadReportedEvent(
                            /* loadDurationMillis= */ System.currentTimeMillis() - loadingStartTime,
                            /* pdfSizeInKb = */ result.pdfSizeInKb,
                            /* pdfLoadResult = */ PdfEventLogger.PdfLoadResults.UNKNOWN,
                            /* linearizationType = */ PdfEventLogger.LinearizationTypes.UNKNOWN,
                            /* numPages = */ -1);
                    throw new RuntimeException("Unexpected error has occurred!");
                }
            }
        }
    }

    /** Returns the number of pages in the PDF document */
    public int getNumPages() {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            return mPdfDocument.getNumPages();
        }
    }

    /**
     * Returns the {@link List} of {@link PdfPageTextContent} for the page number specified. In case
     * of the multiple column textual content, the order is not guaranteed and the text is returned
     * as it is seen by the processing library.
     *
     * @param pageNum page number of the document
     * @return list of the textual content encountered on the page.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public List<PdfPageTextContent> getPageTextContents(int pageNum) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            PdfPageTextContent content = new PdfPageTextContent(mPdfDocument.getPageText(pageNum));
            return List.of(content);
        }
    }

    /**
     * Returns the alternate text for each image encountered on the specified page as a
     * {@link List} of {@link PdfPageImageContent}. The primary use case of this method is for
     * accessibility.
     *
     * @param pageNum page number of the document
     * @return list of the alt text for each image on the page.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public List<PdfPageImageContent> getPageImageContents(int pageNum) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            return mPdfDocument.getPageAltText(pageNum).stream().map(
                    PdfPageImageContent::new).collect(Collectors.toList());
        }
    }

    /**
     * Returns the width of the given page of the PDF document. It is not guaranteed that all the
     * pages of the document will have the same dimensions
     */
    public int getPageWidth(int pageNum) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            return mPdfDocument.getPageWidth(pageNum);
        }
    }

    /**
     * Returns the height of the given page of the PDF document. It is not guaranteed that all the
     * pages of the document will have the same dimensions
     */
    public int getPageHeight(int pageNum) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            return mPdfDocument.getPageHeight(pageNum);
        }
    }

    /**
     * Renders a page to a bitmap for the specified page number.
     *
     * <p>Should be invoked on the {@link android.annotation.WorkerThread} as it is a long-running
     * task.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public void renderPage(int pageNum, Bitmap bitmap, Rect destClip, Matrix transform,
            RenderParams params, boolean renderFormFields) {
        Preconditions.checkNotNull(bitmap, "Destination bitmap cannot be null");
        Preconditions.checkNotNull(params, "RenderParams cannot be null");
        Preconditions.checkArgument(bitmap.getConfig() == Bitmap.Config.ARGB_8888,
                "Unsupported pixel format");
        Preconditions.checkArgument(transform == null || transform.isAffine(),
                "Transform not affine");
        int renderMode = params.getRenderMode();
        Preconditions.checkArgument(renderMode == RenderParams.RENDER_MODE_FOR_DISPLAY
                || renderMode == RenderParams.RENDER_MODE_FOR_PRINT, "Unsupported render mode");
        Preconditions.checkArgument(clipInBitmap(destClip, bitmap), "destClip not in bounds");
        final int contentLeft = (destClip != null) ? destClip.left : 0;
        final int contentTop = (destClip != null) ? destClip.top : 0;
        final int contentRight = (destClip != null) ? destClip.right : bitmap.getWidth();
        final int contentBottom = (destClip != null) ? destClip.bottom : bitmap.getHeight();

        // If transform is not set, stretch page to whole clipped area
        if (transform == null) {
            int clipWidth = contentRight - contentLeft;
            int clipHeight = contentBottom - contentTop;
            transform = new Matrix();
            transform.postScale((float) clipWidth / getPageWidth(pageNum),
                    (float) clipHeight / getPageHeight(pageNum));
            transform.postTranslate(contentLeft, contentTop);
        }

        float[] transformArr = new float[9];
        transform.getValues(transformArr);

        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            mPdfDocument.render(
                    pageNum,
                    bitmap,
                    contentLeft,
                    contentTop,
                    contentRight,
                    contentBottom,
                    transformArr,
                    renderMode,
                    params.getRenderAnnotations(),
                    renderFormFields);
        }
    }

    /**
     * Searches the specified page with the specified query. Should be run on the
     * {@link android.annotation.WorkerThread} as it is a long-running task.
     *
     * @param pageNum page number of the document
     * @param query   the search query
     * @return list of {@link PageMatchBounds} that represents the highlighters which can span
     * multiple
     * lines as well.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public List<PageMatchBounds> searchPageText(int pageNum, String query) {
        Preconditions.checkNotNull(query, "Search query cannot be null");
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            long searchStartTime = System.currentTimeMillis();
            List<PageMatchBounds> searchPageTextResult =
                    mPdfDocument.searchPageText(pageNum, query).unflattenToList();

            // In the current version we are only interested in the results where query length is
            // 1 as there is no much change in latency after words.
            if (pageNum == 0 && query.length() == 1) {
                mPdfEventLogger.logSearchReportedEvent(
                        /* loadDurationMillis = */ System.currentTimeMillis() - searchStartTime,
                        /* queryLength = */ query.length(),
                        /* queryPageNumber = */ pageNum,
                        /* apiResponse = */ PdfEventLogger.ApiResponseTypes.SUCCESS,
                        /* numPages = */ mPdfDocument.getNumPages(),
                        /* matchCount = */ searchPageTextResult.size());
            }

            return searchPageTextResult;
        }
    }

    /**
     * Return a PageSelection which represents the selected content that spans between the
     * two boundaries, both of which can be either exactly defined with text indexes, or
     * approximately defined with points on the page.The resulting Selection will also be
     * exactly defined with both indexes and points.If the start and stop boundary are both
     * the same point, selects the word at that point.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public PageSelection selectPageText(int pageNum, SelectionBoundary start,
            SelectionBoundary stop) {
        Preconditions.checkNotNull(start, "Start selection boundary cannot be null");
        Preconditions.checkNotNull(stop, "Stop selection boundary cannot be null");
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            android.graphics.pdf.models.jni.PageSelection legacyPageSelection =
                    mPdfDocument.selectPageText(pageNum,
                            android.graphics.pdf.models.jni.SelectionBoundary.convert(start),
                            android.graphics.pdf.models.jni.SelectionBoundary.convert(stop));
            if (legacyPageSelection != null) {

                mPdfEventLogger.logPdfApiUsageReportedEvent(
                        /* apiType = */ PdfEventLogger.ApiTypes.SELECT_CONTENT,
                        /* apiResponse = */ PdfEventLogger.ApiResponseTypes.SUCCESS);

                return legacyPageSelection.convert();
            }
            return null;
        }
    }

    /** Get the bounds and URLs of all the links on the given page. */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public List<PdfPageLinkContent> getPageLinkContents(int pageNum) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            return mPdfDocument.getPageLinks(pageNum).unflattenToList();
        }
    }

    /** Returns bookmarks and other goto links (within the current document) on a page */
    public List<PdfPageGotoLinkContent> getPageGotoLinks(int pageNum) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            return mPdfDocument.getPageGotoLinks(pageNum);
        }
    }

    /** Retains object in memory related to a page when that page becomes visible. */
    public void retainPage(int pageNum) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            if (!mPageObjectIdManagerMap.containsKey(pageNum)) {
                mPageObjectIdManagerMap.put(pageNum, new PdfPageComponentsIdManager());
            }
            if (!mPageAnnotationsIdManagerMap.containsKey(pageNum)) {
                mPageAnnotationsIdManagerMap.put(pageNum, new PdfPageComponentsIdManager());
            }
            mPdfDocument.retainPage(pageNum);
        }
    }

    /** Releases object in memory related to a page when that page is no longer visible. */
    public void releasePage(int pageNum) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            mPageObjectIdManagerMap.remove(pageNum);
            mPageAnnotationsIdManagerMap.remove(pageNum);
            mPdfDocument.releasePage(pageNum);
        }
    }

    /**
     * Returns the linearization flag on the PDF document.
     */
    @PdfLinearizationTypes.PdfLinearizationType
    public int getDocumentLinearizationType() {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            return mPdfDocument.isPdfLinearized() ? PDF_DOCUMENT_TYPE_LINEARIZED
                    : PDF_DOCUMENT_TYPE_NON_LINEARIZED;
        }
    }

    /**
     * Returns the form type of the loaded PDF
     *
     * @throws IllegalArgumentException if an unrecognized PDF form type is returned
     */
    public int getPdfFormType() {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            int pdfFormType = mPdfDocument.getFormType();
            return switch (pdfFormType) {
                case PDF_FORM_TYPE_ACRO_FORM, PDF_FORM_TYPE_XFA_FULL,
                     PDF_FORM_TYPE_XFA_FOREGROUND -> pdfFormType;
                default -> PDF_FORM_TYPE_NONE;
            };
        }
    }

    /** Returns true if this PDF prefers to be scaled for printing. */
    public boolean scaleForPrinting() {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            return mPdfDocument.scaleForPrinting();
        }
    }

    /**
     * Returns information about all form widgets on the page, or an empty list if there are no form
     * widgets on the page.
     *
     * <p>Optionally restricted by {@code types}. If {@code types} is empty, all form widgets on the
     * page will be returned.
     */
    @NonNull
    public List<FormWidgetInfo> getFormWidgetInfos(int pageNum,
            @NonNull @FormWidgetInfo.WidgetType int[] types) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            return mPdfDocument.getFormWidgetInfos(pageNum, types);
        }
    }

    /**
     * Returns information about the widget with {@code annotationIndex}.
     *
     * <p>{@code annotationIndex} refers to the index of the annotation within the page's "Annot"
     * array in the PDF document. This info is available on results of previous calls via {@link
     * FormWidgetInfo#getWidgetIndex()}.
     */
    @NonNull
    FormWidgetInfo getFormWidgetInfoAtIndex(int pageNum, int annotationIndex) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            FormWidgetInfo result = mPdfDocument.getFormWidgetInfo(pageNum, annotationIndex);
            if (result == null) {
                throw new IllegalArgumentException("No widget found at this index.");
            }
            return result;
        }
    }

    /** Returns information about the widget at the given point. */
    @NonNull
    public FormWidgetInfo getFormWidgetInfoAtPosition(int pageNum, int x, int y) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            FormWidgetInfo result = mPdfDocument.getFormWidgetInfo(pageNum, x, y);
            if (result == null) {
                throw new IllegalArgumentException("No widget found at point.");
            }
            return result;
        }

    }

    /**
     * Applies a {@link FormEditRecord} to the PDF.
     *
     * @return a list of rectangular areas invalidated by form widget operation
     * <p>For click type {@link FormEditRecord}s, performs a click on {@link
     * FormEditRecord#getClickPoint()}
     * <p>For set text type {@link FormEditRecord}s, sets the text value of the form widget.
     * <p>For set indices type {@link FormEditRecord}s, sets the {@link
     * FormEditRecord#getSelectedIndices()} as selected and all others as unselected for the
     * form widget indicated by the record.
     */
    @NonNull
    public List<Rect> applyEdit(int pageNum, @NonNull FormEditRecord editRecord) {
        Preconditions.checkNotNull(editRecord, "Edit record cannot be null");
        Preconditions.checkArgument(pageNum >= 0, "Invalid page number");
        if (editRecord.getType() == FormEditRecord.EDIT_TYPE_CLICK) {
            return applyEditTypeClick(pageNum, editRecord);
        } else if (editRecord.getType() == FormEditRecord.EDIT_TYPE_SET_INDICES) {
            return applyEditTypeSetIndices(pageNum, editRecord);
        } else if (editRecord.getType() == FormEditRecord.EDIT_TYPE_SET_TEXT) {
            return applyEditSetText(pageNum, editRecord);
        }
        return Collections.emptyList();
    }

    @FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
    private List<Rect> applyEditTypeClick(int pageNum, @NonNull FormEditRecord editRecord) {
        Preconditions.checkNotNull(editRecord.getClickPoint(),
                "Can't apply click edit record without point");
        Point clickPoint = editRecord.getClickPoint();
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            List<Rect> results = mPdfDocument.clickOnPage(pageNum, clickPoint.x, clickPoint.y);
            if (results == null) {
                throw new IllegalArgumentException("Cannot click on this widget.");
            }
            return results;
        }
    }

    @FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
    private List<Rect> applyEditTypeSetIndices(int pageNum, @NonNull FormEditRecord editRecord) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            int[] selectedIndices = editRecord.getSelectedIndices();
            List<Rect> results = mPdfDocument.setFormFieldSelectedIndices(pageNum,
                    editRecord.getWidgetIndex(), selectedIndices);
            if (results == null) {
                throw new IllegalArgumentException("Cannot set selected indices on this widget.");
            }
            return results;
        }
    }

    @FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
    private List<Rect> applyEditSetText(int pageNum, @NonNull FormEditRecord editRecord) {
        Preconditions.checkNotNull(editRecord.getText(),
                "Can't apply set text record without text");
        String text = editRecord.getText();
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            List<Rect> results = mPdfDocument.setFormFieldText(pageNum, editRecord.getWidgetIndex(),
                    text);
            if (results == null) {
                throw new IllegalArgumentException("Cannot set form field text on this widget.");
            }
            return results;
        }
    }

    /** Ensures that any previous {@link PdfDocumentProxy} instance is closed. */
    public void ensurePdfDestroyed() {
        synchronized (sPdfiumLock) {
            if (mPdfDocument != null) {
                try {
                    mPdfDocument.destroy();
                } catch (Throwable t) {
                    Log.e(TAG, "Error closing PdfDocumentProxy", t);
                } finally {
                    mPdfDocument = null;
                }
            }
        }
    }

    /**
     * Saves the current state of the loaded PDF document to the given writable
     * ParcelFileDescriptor.
     */
    public void write(ParcelFileDescriptor destination, boolean removePasswordProtection) {
        Preconditions.checkNotNull(destination, "Destination FD cannot be null");
        if (removePasswordProtection) {
            cloneWithoutSecurity(destination);
        } else {
            saveAs(destination);
        }
    }

    /**
     * Returns a list of pairs, where each pair consists of a supported
     * {@link PdfAnnotation} and its corresponding ID on the specified page.
     * Refer to {@link PdfAnnotationType} for the supported annotation types.
     *
     * <p>
     * The returned list will be empty if no supported annotations are present
     * on the page, even if the page contains other annotation types.
     *
     * @param pageNum page number whose annotations' list to be retrieved
     * @return A list of pairs representing the supported annotations and their ids on the page.
     */
    @NonNull
    public List<Pair<Integer, PdfAnnotation>> getPageAnnotations(@IntRange(from = 0) int pageNum) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            PdfPageComponentsIdManager pageAnnotationIdManager =
                    mPageAnnotationsIdManagerMap.get(pageNum);
            List<PdfAnnotation> pdfAnnotations = mPdfDocument.getPageAnnotations(pageNum);
            List<Pair<Integer, PdfAnnotation>> pdfAnnotationIdPairs = new ArrayList<>();
            for (int i = 0; i < pdfAnnotations.size(); i++) {
                if (pdfAnnotations.get(i) != null) {
                    pdfAnnotationIdPairs.add(
                            new Pair<>(pageAnnotationIdManager.getIdForIndex(i),
                                    pdfAnnotations.get(i)));
                }
            }
            return pdfAnnotationIdPairs;
        }
    }

    /**
     * Adds the given annotation to the page. The annotation should be of
     * supported type. See {@link PdfAnnotationType} for the supported types
     *
     * @param annotation the {@link PdfAnnotation} object to
     *                   add
     * @param pageNum    the page number where the annotation to be added
     * @return the index of the added annotation,
     * or -1 if the annotation cannot be added. The
     * index is guaranteed to be non-negative if
     * the annotation is added successfully.
     */
    public int addPageAnnotation(@IntRange(from = 0) int pageNum,
            PdfAnnotation annotation) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            int addedAnnotationIndex = mPdfDocument.addPageAnnotation(pageNum, annotation);
            if (addedAnnotationIndex == -1) {
                throw new IllegalArgumentException("Failed to add annotation");
            }
            return mPageAnnotationsIdManagerMap.get(pageNum).getIdForIndex(addedAnnotationIndex);
        }
    }

    /**
     * Removes the annotation with the specified index.
     *
     * @param annotationId the Id of the annotation to remove
     *                        from the page
     * @param pageNum         page number from which annotation is to be removed
     */
    public void removePageAnnotation(@IntRange(from = 0) int pageNum,
            int annotationId) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            PdfPageComponentsIdManager pdfAnnotationsIdManager =
                    mPageAnnotationsIdManagerMap.get(pageNum);
            int annotationIndex = pdfAnnotationsIdManager.getIndexForId(annotationId);
            if (annotationIndex == -1) {
                throw new IllegalArgumentException("Unknown annotationId. getPageAnnotations() "
                        + "call never made?");
            }
            if (!mPdfDocument.removePageAnnotation(pageNum, annotationIndex)) {
                throw new IllegalArgumentException("Annotation cannot be removed.");
            }
            pdfAnnotationsIdManager.deleteId(annotationId);
        }
    }

    /**
     * Update the given {@link PdfAnnotation} to the page.
     *
     * @param annotationId id corresponding to which the annotation is to be updated
     * @param annotation the annotation to update
     * @return true if annotation is updated, false otherwise
     * @throws IllegalArgumentException f the provided annotation is null or of
     *                                  unsupported type i.e. {@link PdfAnnotationType#UNKNOWN}
     *                                  or if the provided annotation id is negative
     **/
    @FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_PAGE_OBJECTS)
    public boolean updatePageAnnotation(int pageNum, int annotationId,
            @NonNull PdfAnnotation annotation) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            int annotationIndex = mPageAnnotationsIdManagerMap.get(pageNum)
                    .getIndexForId(annotationId);
            if (annotationIndex == -1) {
                throw new IllegalArgumentException("Unknown annotation Id. getPageAnnotations()"
                        + " call never made?");
            }
            if (!mPdfDocument.updatePageAnnotation(pageNum, annotationIndex, annotation)) {
                throw new IllegalArgumentException("Update Failed");
            }
            return true;
        }
    }

    /**
     * Return list of supported {@link PdfPageObject} present on
     * the page.
     * The list will be empty  if there are no supported page
     * objects present on the page, even if the page contains
     * other page object types.
     *
     * @return A {@link List} of {@link Pair} objects, where each pair contains:
     * - An {@link Integer} representing the object ID.
     * - A {@link PdfPageObject} representing the page object.
     * @throws IllegalStateException if the {@link PdfRenderer.Page} is
     *                               closed before invocation
     */
    @FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_PAGE_OBJECTS)
    public List<Pair<Integer, PdfPageObject>> getPageObjects(int pageNum) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            PdfPageComponentsIdManager pageObjectIdManager = mPageObjectIdManagerMap.get(pageNum);
            List<PdfPageObject> pageObjects = mPdfDocument.getPageObjects(pageNum);
            List<Pair<Integer, PdfPageObject>> pageObjectIdPairs = new ArrayList<>();
            for (int i = 0; i < pageObjects.size(); i++) {
                if (pageObjects.get(i) != null) {
                    pageObjectIdPairs.add(
                            new Pair<>(pageObjectIdManager.getIdForIndex(i), pageObjects.get(i)));
                }
            }
            return pageObjectIdPairs;
        }
    }

    /**
     * Adds the given {@link PdfPageObject} to the page.
     *
     * @param pageObject the {@link PdfPageObject} object to
     *                   add
     * @return object id of added page object, -1 otherwise
     * @throws IllegalArgumentException if the provided {@link PdfPageObject} is unknown or null.
     * @throws IllegalStateException    if the {@link PdfRenderer.Page} is closed before invocation.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_PAGE_OBJECTS)
    public int addPageObject(int pageNum, @NonNull PdfPageObject pageObject) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            int addedObjectIndex = mPdfDocument.addPageObject(pageNum, pageObject);
            if (addedObjectIndex == -1) {
                throw new IllegalArgumentException("Failed to add PageObject");
            }
            return mPageObjectIdManagerMap.get(pageNum).getIdForIndex(addedObjectIndex);
        }
    }

    /**
     * Update the given {@link PdfPageObject} to the page.
     *
     * @param objectId   The unique identifier of the page object to update.
     * @param pageObject the {@link PdfPageObject} object to add.
     * @return true if page object is updated, false otherwise.
     * @throws IllegalArgumentException if the provided {@link PdfPageObject} is unknown or null.
     * @throws IllegalStateException    if the {@link PdfRenderer.Page} is closed before invocation.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_PAGE_OBJECTS)
    public boolean updatePageObject(int pageNum, int objectId,
            @NonNull PdfPageObject pageObject) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            int objectIndex = mPageObjectIdManagerMap.get(pageNum).getIndexForId(objectId);
            if (objectIndex == -1) {
                throw new IllegalArgumentException("Unknown objectId. "
                        + "getPageObjects() call never made?");
            }
            if (!mPdfDocument.updatePageObject(pageNum, objectIndex, pageObject)) {
                throw new IllegalArgumentException("Update Failed");
            }
            return true;
        }
    }

    /**
     * Removes the {@link PdfPageObject} with the specified ID.
     *
     * @param objectId the id of the page object to remove
     *                 from the page
     * @throws IllegalStateException if the provided
     *                               objectId doesn't exist.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_PAGE_OBJECTS)
    public void removePageObject(int pageNum, int objectId) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            PdfPageComponentsIdManager pageObjectIdManager =
                    mPageObjectIdManagerMap.get(pageNum);
            int objectIndex = pageObjectIdManager.getIndexForId(objectId);
            if (objectIndex == -1) {
                throw new IllegalArgumentException("Unknown objectId. getPageObjects() "
                        + "call never made ?");
            }
            if (!mPdfDocument.removePageObject(pageNum, objectIndex)) {
                throw new IllegalArgumentException("Page object cannot be removed.");
            }
            pageObjectIdManager.deleteId(objectId);
        }
    }

    /**
     * Creates a copy of the current document without security, if it is password protected. This
     * may be necessary for the PrintManager which can't handle password-protected files.
     *
     * @param destination points to where pdfclient should make a copy of the pdf without security.
     */
    private void cloneWithoutSecurity(ParcelFileDescriptor destination) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            mPdfDocument.cloneWithoutSecurity(destination);
        }
    }

    /**
     * Saves the current document to the given {@link ParcelFileDescriptor}.
     *
     * @param destination where the currently open PDF should be written.
     */
    private void saveAs(ParcelFileDescriptor destination) {
        synchronized (sPdfiumLock) {
            assertPdfDocumentNotNull();
            mPdfDocument.saveAs(destination);
        }
    }

    private boolean clipInBitmap(@Nullable Rect clip, Bitmap destination) {
        if (clip == null) {
            return true;
        }
        return clip.left >= 0 && clip.top >= 0 && clip.right <= destination.getWidth()
                && clip.bottom <= destination.getHeight();
    }

    private void assertPdfDocumentNotNull() {
        Preconditions.checkNotNull(mPdfDocument, "PdfDocumentProxy cannot be null");
    }

}
