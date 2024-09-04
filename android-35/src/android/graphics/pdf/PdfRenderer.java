/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.compat.annotation.UnsupportedAppUsage;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.pdf.content.PdfPageGotoLinkContent;
import android.graphics.pdf.content.PdfPageImageContent;
import android.graphics.pdf.content.PdfPageLinkContent;
import android.graphics.pdf.content.PdfPageTextContent;
import android.graphics.pdf.flags.Flags;
import android.graphics.pdf.models.FormEditRecord;
import android.graphics.pdf.models.FormWidgetInfo;
import android.graphics.pdf.models.PageMatchBounds;
import android.graphics.pdf.models.selection.PageSelection;
import android.graphics.pdf.models.selection.SelectionBoundary;
import android.graphics.pdf.utils.Preconditions;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.CloseGuard;
import android.util.Log;

import androidx.annotation.RestrictTo;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * <p>
 * This class enables rendering a PDF document and selecting, searching, fast scrolling,
 * annotations, etc. from Android V. This class is thread safe and can be called by
 * multiple threads however only one thread will be executed at a time as the access is
 * synchronized by internal locking.
 * <p>
 * If you want to render a PDF, you create a renderer and for every page you want
 * to render, you open the page, render it, and close the page. After you are done
 * with rendering, you close the renderer. After the renderer is closed it should not
 * be used anymore. Note that the pages are rendered one by one, i.e. you can have
 * only a single page opened at any given time.
 * <p>
 * A typical use of the APIs to render a PDF looks like this:
 * <pre>
 * // create a new renderer
 * PdfRenderer renderer = new PdfRenderer(getSeekableFileDescriptor());
 *
 * // let us just render all pages
 * final int pageCount = renderer.getPageCount();
 * for (int i = 0; i < pageCount; i++) {
 *     Page page = renderer.openPage(i);
 *
 *     // say we render for showing on the screen
 *     page.render(mBitmap, null, null, Page.RENDER_MODE_FOR_DISPLAY);
 *
 *     // do stuff with the bitmap
 *
 *     // close the page
 *     page.close();
 * }
 *
 * // close the renderer
 * renderer.close();
 * </pre>
 *
 * <h3>Print preview and print output</h3>
 * <p>
 * If you are using this class to rasterize a PDF for printing or show a print
 * preview, it is recommended that you respect the following contract in order
 * to provide a consistent user experience when seeing a preview and printing,
 * i.e. the user sees a preview that is the same as the printout.
 * <ul>
 * <li>
 * Respect the property whether the document would like to be scaled for printing
 * as per {@link #shouldScaleForPrinting()}.
 * </li>
 * <li>
 * When scaling a document for printing the aspect ratio should be preserved.
 * </li>
 * <li>
 * Do not inset the content with any margins from the {@link android.print.PrintAttributes}
 * as the application is responsible to render it such that the margins are respected.
 * </li>
 * <li>
 * If document page size is greater than the printed media size the content should
 * be anchored to the upper left corner of the page for left-to-right locales and
 * top right corner for right-to-left locales.
 * </li>
 * </ul>
 *
 * @see #close()
 */
@SuppressLint("UnflaggedApi")
public final class PdfRenderer implements AutoCloseable {
    /** Represents a non-linearized PDF document. */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public static final int DOCUMENT_LINEARIZED_TYPE_NON_LINEARIZED = 0;

    /** Represents a linearized PDF document. */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public static final int DOCUMENT_LINEARIZED_TYPE_LINEARIZED = 1;

    /** Represents a PDF without form fields */
    @FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
    public static final int PDF_FORM_TYPE_NONE = 0;
    /** Represents a PDF with form fields specified using the AcroForm spec */
    @FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
    public static final int PDF_FORM_TYPE_ACRO_FORM = 1;
    /** Represents a PDF with form fields specified using the entire XFA spec */
    @FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
    public static final int PDF_FORM_TYPE_XFA_FULL = 2;
    /** Represents a PDF with form fields specified using the XFAF subset of the XFA spec */
    @FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
    public static final int PDF_FORM_TYPE_XFA_FOREGROUND = 3;

    private static final String TAG = PdfRenderer.class.getSimpleName();
    private final CloseGuard mCloseGuard = new CloseGuard();
    private final int mPageCount;
    private ParcelFileDescriptor mInput;

    private PdfProcessor mPdfProcessor;

    /**
     * Creates a new instance.
     *
     * <p><strong>Note:</strong> The provided file descriptor must be <strong>seekable</strong>,
     * i.e. its data being randomly accessed, e.g. pointing to a file.
     * <p>
     * <strong>Note:</strong> This class takes ownership of the passed in file descriptor
     * and is responsible for closing it when the renderer is closed.
     * <p>
     * If the file is from an untrusted source it is recommended to run the renderer in a separate,
     * isolated process with minimal permissions to limit the impact of security exploits.
     * <strong>Note:</strong> The constructor should be instantiated on the
     * {@link android.annotation.WorkerThread} as it can be long-running while loading the
     * document.
     *
     * @param fileDescriptor Seekable file descriptor to read from.
     * @throws java.io.IOException         If an error occurs while reading the file.
     * @throws java.lang.SecurityException If the file requires a password or
     *                                     the security scheme is not supported.
     * @throws IllegalArgumentException    If the {@link ParcelFileDescriptor} is not seekable.
     * @throws NullPointerException        If the file descriptor is null.
     */
    @SuppressLint("UnflaggedApi")
    public PdfRenderer(@NonNull ParcelFileDescriptor fileDescriptor) throws IOException {
        Preconditions.checkNotNull(fileDescriptor, "File descriptor cannot be null!");
        mInput = fileDescriptor;

        try {
            mPdfProcessor = new PdfProcessor();
            mPdfProcessor.create(mInput, null);
            mPageCount = mPdfProcessor.getNumPages();
        } catch (Throwable t) {
            doClose();
            throw t;
        }

        mCloseGuard.open("close");
    }

    /**
     * Creates a new instance of PdfRenderer class.
     * <p>
     * <strong>Note:</strong> The provided file descriptor must be <strong>seekable</strong>,
     * i.e. its data being randomly accessed, e.g. pointing to a file. If the password passed in
     * {@link android.graphics.pdf.LoadParams} is incorrect, the
     * {@link android.graphics.pdf.PdfRenderer} will throw a {@link SecurityException}.
     * <p>
     * <strong>Note:</strong> This class takes ownership of the passed in file descriptor
     * and is responsible for closing it when the renderer is closed.
     * <p>
     * If the file is from an untrusted source it is recommended to run the renderer in a separate,
     * isolated process with minimal permissions to limit the impact of security exploits.
     * <strong>Note:</strong> The constructor should be instantiated on the
     * {@link android.annotation.WorkerThread} as it can be long-running while loading the document.
     *
     * @param fileDescriptor Seekable file descriptor to read from.
     * @param params         Instance of {@link LoadParams} specifying params for loading PDF
     *                       document.
     * @throws java.io.IOException         If an error occurs while reading the file.
     * @throws java.lang.SecurityException If the file requires a password or
     *                                     the security scheme is not supported by the renderer.
     * @throws IllegalArgumentException    If the {@link ParcelFileDescriptor} is not seekable.
     * @throws NullPointerException        If the file descriptor or load params is null.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public PdfRenderer(@NonNull ParcelFileDescriptor fileDescriptor, @NonNull LoadParams params)
            throws IOException {
        Preconditions.checkNotNull(fileDescriptor, "input cannot be null");
        Preconditions.checkNotNull(params, "Load params cannot be null");
        mInput = fileDescriptor;

        try {
            mPdfProcessor = new PdfProcessor();
            mPdfProcessor.create(mInput, params);
            mPageCount = mPdfProcessor.getNumPages();
        } catch (Throwable t) {
            doClose();
            throw t;
        }

        mCloseGuard.open("close");
    }

    /**
     * Closes this renderer. You should not use this instance
     * after this method is called.
     *
     * @throws IllegalStateException If {@link #close()} is called before invoking this or if any
     *                               page is opened and not closed
     * @see Page#close()
     */
    @SuppressLint("UnflaggedApi")
    public void close() {
        throwIfDocumentClosed();
        doClose();
    }

    /**
     * Gets the number of pages in the document.
     *
     * @return The page count.
     * @throws IllegalStateException If {@link #close()} is called before invoking this.
     */
    @SuppressLint("UnflaggedApi")
    @IntRange(from = 0)
    public int getPageCount() {
        throwIfDocumentClosed();
        return mPageCount;
    }

    /**
     * Gets whether the document prefers to be scaled for printing.
     * You should take this info account if the document is rendered
     * for printing and the target media size differs from the page
     * size.
     *
     * @return If to scale the document.
     * @throws IllegalStateException If {@link #close()} is called before invoking this.
     */
    @SuppressLint("UnflaggedApi")
    public boolean shouldScaleForPrinting() {
        throwIfDocumentClosed();

        return mPdfProcessor.scaleForPrinting();
    }

    /**
     * Gets the type of the PDF document.
     *
     * @return The PDF document type.
     * @throws IllegalStateException If {@link #close()} is called before invoking this.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    @PdfDocumentLinearizationType
    public int getDocumentLinearizationType() {
        throwIfDocumentClosed();
        int documentType = mPdfProcessor.getDocumentLinearizationType();
        if (documentType == PDF_DOCUMENT_TYPE_LINEARIZED) {
            return DOCUMENT_LINEARIZED_TYPE_LINEARIZED;
        } else {
            return DOCUMENT_LINEARIZED_TYPE_NON_LINEARIZED;
        }
    }

    /**
     * Opens a page for rendering.
     *
     * @param index The page index to open, starting from index 0.
     * @return A page that can be rendered.
     * @throws IllegalStateException    If {@link #close()} is called before invoking this.
     * @throws IllegalArgumentException If the page number is less than 0 or greater than or equal
     *                                  to the total page count.
     * @see Page#close()
     */
    @SuppressLint("UnflaggedApi")
    @NonNull
    public Page openPage(@IntRange(from = 0) int index) {
        throwIfDocumentClosed();
        throwIfPageNotInDocument(index);
        return new Page(index);
    }

    /**
     * Returns the form type of the loaded PDF
     *
     * @throws IllegalStateException    if the renderer is closed
     * @throws IllegalArgumentException if an unexpected PDF form type is returned
     */
    @PdfFormType
    @FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
    public int getPdfFormType() {
        throwIfDocumentClosed();
        int pdfFormType = mPdfProcessor.getPdfFormType();
        if (pdfFormType == PDF_FORM_TYPE_ACRO_FORM) {
            return PDF_FORM_TYPE_ACRO_FORM;
        } else if (pdfFormType == PDF_FORM_TYPE_XFA_FULL) {
            return PDF_FORM_TYPE_XFA_FULL;
        } else if (pdfFormType == PDF_FORM_TYPE_XFA_FOREGROUND) {
            return PDF_FORM_TYPE_XFA_FOREGROUND;
        } else {
            return PDF_FORM_TYPE_NONE;
        }
    }

    /**
     * Saves the current state of the loaded PDF document to the given writable
     * {@link ParcelFileDescriptor}. If the document is password-protected then setting
     * {@code removePasswordProtection} removes the protection before saving. The PDF document
     * should already be decrypted with the correct password before writing. Useful for printing or
     * sharing.
     * <strong>Note:</strong> This method closes the provided file descriptor.
     *
     * @param destination              The writable {@link ParcelFileDescriptor}
     * @param removePasswordProtection If true, removes password protection from the PDF before
     *                                 saving.
     * @throws IOException           If there's a write error, or if 'removePasswordSecurity' is
     *                               {@code true} but the document remains encrypted.
     * @throws IllegalStateException If {@link #close()} is called before invoking this.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
    public void write(@NonNull ParcelFileDescriptor destination, boolean removePasswordProtection)
            throws IOException {
        throwIfDocumentClosed();
        mPdfProcessor.write(destination, removePasswordProtection);
    }

    // SuppressLint: Finalize needs to be overridden to make sure all resources are closed
    // gracefully
    @Override
    @SuppressLint("GenericException")
    protected void finalize() throws Throwable {
        try {
            if (mCloseGuard != null) {
                mCloseGuard.warnIfOpen();
            }

            doClose();
        } finally {
            super.finalize();
        }
    }

    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private void doClose() {
        if (mPdfProcessor != null) {
            mPdfProcessor.ensurePdfDestroyed();
            mPdfProcessor = null;
        }

        if (mInput != null) {
            closeQuietly(mInput);
            mInput = null;
        }

        mCloseGuard.close();
    }

    private void closeQuietly(@NonNull AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (RuntimeException rethrown) {
            throw rethrown;
        } catch (Exception ignored) {
            Log.w(TAG, "close operation failed.");
        }
    }

    private void throwIfDocumentClosed() {
        if (mPdfProcessor == null) {
            throw new IllegalStateException("Document already closed");
        }
    }

    private void throwIfPageNotInDocument(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= mPageCount) {
            throw new IllegalArgumentException("Invalid page index");
        }
    }

    /** @hide */
    @IntDef({PDF_FORM_TYPE_NONE, PDF_FORM_TYPE_ACRO_FORM, PDF_FORM_TYPE_XFA_FULL,
            PDF_FORM_TYPE_XFA_FOREGROUND})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PdfFormType {
    }

    /** @hide */
    @IntDef({Page.RENDER_MODE_FOR_DISPLAY, Page.RENDER_MODE_FOR_PRINT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RenderMode {
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"PDF_DOCUMENT_TYPE_"}, value = {DOCUMENT_LINEARIZED_TYPE_NON_LINEARIZED,
            DOCUMENT_LINEARIZED_TYPE_LINEARIZED})
    public @interface PdfDocumentLinearizationType {
    }

    /**
     * This class represents a PDF document page for rendering.
     */
    @SuppressLint("UnflaggedApi")
    public final class Page implements AutoCloseable {

        /**
         * Mode to render the content for display on a screen.
         */
        @SuppressLint("UnflaggedApi")
        public static final int RENDER_MODE_FOR_DISPLAY = 1;
        /**
         * Mode to render the content for printing.
         */
        @SuppressLint("UnflaggedApi")
        public static final int RENDER_MODE_FOR_PRINT = 2;
        private final CloseGuard mCloseGuard = new CloseGuard();
        private final int mWidth;
        private final int mHeight;
        private int mIndex;

        private Page(int index) {
            mIndex = index;
            mPdfProcessor.retainPage(mIndex);
            mWidth = mPdfProcessor.getPageWidth(mIndex);
            mHeight = mPdfProcessor.getPageHeight(mIndex);

            mCloseGuard.open("close");
        }

        /**
         * Gets the page index.
         *
         * @return The index.
         */
        @SuppressLint("UnflaggedApi")
        @IntRange(from = 0)
        public int getIndex() {
            return mIndex;
        }

        /**
         * Returns the width of the {@link PdfRenderer.Page} object in points (1/72"). It is
         * not guaranteed that all pages will have the same width and the viewport should be resized
         * to the page width.
         *
         * @return width of the page
         * @throws IllegalStateException If the document/page is closed before invocation.
         */
        @SuppressLint("UnflaggedApi")
        @IntRange(from = 0)
        public int getWidth() {
            return mWidth;
        }

        /**
         * Returns the height of the {@link PdfRenderer.Page} object in points (1/72"). It is
         * not guaranteed that all pages will have the same height and the viewport should be
         * resized to the page height.
         *
         * @return height of the page
         * @throws IllegalStateException If the document/page is closed before invocation.
         */
        @SuppressLint("UnflaggedApi")
        @IntRange(from = 0)
        public int getHeight() {
            return mHeight;
        }

        /**
         * Renders a page to a bitmap.
         * <p>
         * You may optionally specify a rectangular clip in the bitmap bounds. No rendering
         * outside the clip will be performed, hence it is your responsibility to initialize
         * the bitmap outside the clip.
         * <p>
         * You may optionally specify a matrix to transform the content from page coordinates
         * which are in points (1/72") to bitmap coordinates which are in pixels. If this
         * matrix is not provided this method will apply a transformation that will fit the
         * whole page to the destination clip if provided or the destination bitmap if no
         * clip is provided.
         * <p>
         * The clip and transformation are useful for implementing tile rendering where the
         * destination bitmap contains a portion of the image, for example when zooming.
         * Another useful application is for printing where the size of the bitmap holding
         * the page is too large and a client can render the page in stripes.
         * <p>
         * <strong>Note: </strong> The destination bitmap format must be
         * {@link Config#ARGB_8888 ARGB}.
         * <p>
         * <strong>Note: </strong> The optional transformation matrix must be affine as per
         * {@link android.graphics.Matrix#isAffine() Matrix.isAffine()}. Hence, you can specify
         * rotation, scaling, translation but not a perspective transformation.
         *
         * @param destination Destination bitmap to which to render.
         * @param destClip    Optional clip in the bitmap bounds.
         * @param transform   Optional transformation to apply when rendering.
         * @param renderMode  The render mode.
         * @see #RENDER_MODE_FOR_DISPLAY
         * @see #RENDER_MODE_FOR_PRINT
         */
        @SuppressLint("UnflaggedApi")
        public void render(@NonNull Bitmap destination, @Nullable Rect destClip,
                @Nullable Matrix transform, @RenderMode int renderMode) {
            mPdfProcessor.renderPage(mIndex, destination, destClip, transform,
                    new RenderParams.Builder(renderMode).build());
        }

        /**
         * Renders a page to a bitmap. In case of default zoom, the {@link Bitmap} dimensions will
         * be equal to the page dimensions. In this case, {@link Rect} parameter can be null.
         *
         * <p>In case of zoom, the {@link Rect} parameter needs to be specified which represents
         * the offset from top and left for tile generation purposes. In this case, the
         * {@link Bitmap} dimensions should be equal to the tile dimensions.
         * <p>
         * <strong>Note:</strong> The method will take care of closing the bitmap. Should be
         * invoked
         * on the {@link android.annotation.WorkerThread} as it is long-running task.
         *
         * @param destination Destination bitmap to write to.
         * @param destClip    If null, default zoom is applied. In case the value is non-null, the
         *                    value specifies the top top-left corner of the tile.
         * @param transform   Applied to scale the bitmap up/down from default 1/72 points.
         * @param params      Render params for the changing display mode and/or annotations.
         * @throws IllegalStateException If the document/page is closed before invocation.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
        public void render(@NonNull Bitmap destination, @Nullable Rect destClip,
                @Nullable Matrix transform, @NonNull RenderParams params) {
            throwIfDocumentOrPageClosed();
            mPdfProcessor.renderPage(mIndex, destination, destClip, transform, params);
        }

        /**
         * Return list of {@link PdfPageTextContent} found on the page, ordered left to right
         * and top to bottom. It contains all the content associated with text found on the page.
         * The list will be empty if there are no results found. Currently, localisation does
         * not have any impact on the order in which {@link PdfPageTextContent} is returned.
         *
         * @return list of text content found on the page.
         * @throws IllegalStateException If the document/page is closed before invocation.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
        @NonNull
        public List<PdfPageTextContent> getTextContents() {
            throwIfDocumentOrPageClosed();
            return mPdfProcessor.getPageTextContents(mIndex);
        }

        /**
         * Return list of {@link PdfPageImageContent} found on the page, ordered left to right
         * and top to bottom. It contains all the content associated with images found on the
         * page including alt text. The list will be empty if there are no results found.
         * Currently, localisation does not have any impact on the order in which
         * {@link PdfPageImageContent} is returned.
         *
         * @return list of image content found on the page.
         * @throws IllegalStateException If the document/page is closed before invocation.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
        @NonNull
        public List<PdfPageImageContent> getImageContents() {
            throwIfDocumentOrPageClosed();
            return mPdfProcessor.getPageImageContents(mIndex);
        }

        /**
         * Search for the given string on the page and returns the bounds of all the matches. The
         * list will be empty if there are no matches on the page. If this function was
         * invoked previously for any page, it will wait for that operation to
         * complete before this operation is started.
         * <p>
         * <strong>Note:</strong> Should be invoked on the {@link android.annotation.WorkerThread}
         * as it is long-running task.
         *
         * @param query plain search string for querying the document
         * @return List of {@link PageMatchBounds} representing the bounds of each match on the
         * page.
         * @throws IllegalStateException If the document/page is closed before invocation.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
        @NonNull
        public List<PageMatchBounds> searchText(@NonNull String query) {
            throwIfDocumentOrPageClosed();
            return mPdfProcessor.searchPageText(mIndex, query);
        }

        /**
         * Return a {@link PageSelection} which represents the selected content that spans between
         * the two boundaries. The boundaries can be either exactly defined with text indexes, or
         * approximately defined with points on the page. The resulting selection will also be
         * exactly defined with both indexes and points. If the start and stop boundary are both at
         * the same point, selects the word at that point. In case the selection from the given
         * boundaries result in an empty space, then the method returns {@code null}. The start and
         * stop {@link SelectionBoundary} in {@link PageSelection} resolves to the "nearest" index
         * when returned.
         * <p>
         * <strong>Note:</strong> Should be invoked on a {@link android.annotation.WorkerThread}
         * as it is long-running task.
         *
         * @param start boundary where the selection starts (inclusive)
         * @param stop  boundary where the selection stops (exclusive)
         * @return collection of the selected content for text, images, etc.
         * @throws IllegalStateException If the document/page is closed before invocation.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
        @Nullable
        public PageSelection selectContent(@NonNull SelectionBoundary start,
                @NonNull SelectionBoundary stop) {
            throwIfDocumentOrPageClosed();
            return mPdfProcessor.selectPageText(mIndex, start, stop);
        }


        /**
         * Get the bounds and URLs of all the links on the page.
         *
         * @return list of all links on the page.
         * @throws IllegalStateException If the document/page is closed before invocation.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
        @NonNull
        public List<PdfPageLinkContent> getLinkContents() {
            throwIfDocumentOrPageClosed();
            return mPdfProcessor.getPageLinkContents(mIndex);
        }

        /**
         * Gets bookmarks and goto links present on the page of a pdf document. Goto Links
         * are the internal navigation links which directs the user to different location
         * within the same document.
         *
         * @return list of all goto links {@link PdfPageGotoLinkContent} on a page, ordered
         * left to right and top to bottom.
         * @throws IllegalStateException If the document/page is closed before invocation.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
        @NonNull
        public List<PdfPageGotoLinkContent> getGotoLinks() {
            throwIfDocumentOrPageClosed();
            return mPdfProcessor.getPageGotoLinks(mIndex);
        }

        /**
         * Returns information about all form widgets on the page, or an empty list if there are no
         * form widgets on the page.
         *
         * @throws IllegalStateException if the renderer or page is closed
         */
        @androidx.annotation.NonNull
        @FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
        public List<FormWidgetInfo> getFormWidgetInfos() {
            return getFormWidgetInfos(new int[0]);
        }

        /**
         * Returns information about all form widgets of the specified types on the page, or an
         * empty list if there are no form widgets of the specified types on the page.
         *
         * @param types the types of form widgets to return, or an empty array to return all widgets
         * @throws IllegalStateException if the renderer or page is closed
         */
        @NonNull
        @FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
        public List<FormWidgetInfo> getFormWidgetInfos(
                @NonNull @FormWidgetInfo.WidgetType int[] types) {
            throwIfDocumentOrPageClosed();
            return mPdfProcessor.getFormWidgetInfos(mIndex, types);
        }

        /**
         * Returns information about the widget with {@code widgetIndex}.
         *
         * @param widgetIndex the index of the widget within the page's "Annot" array in the PDF
         *                    document, available on results of previous calls to {@link
         *                    #getFormWidgetInfos(int[])} or
         *                    {@link #getFormWidgetInfoAtPosition(int, int)} via
         *                    {@link FormWidgetInfo#getWidgetIndex()}.
         * @throws IllegalArgumentException if there is no form widget at the provided index.
         * @throws IllegalStateException    if the renderer or page is closed
         */
        @NonNull
        @FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
        public FormWidgetInfo getFormWidgetInfoAtIndex(@IntRange(from = 0) int widgetIndex) {
            throwIfDocumentOrPageClosed();
            return mPdfProcessor.getFormWidgetInfoAtIndex(mIndex, widgetIndex);
        }

        /**
         * Returns information about the widget at the given point.
         *
         * @param x the x position of the widget on the page, in points
         * @param y the y position of the widget on the page, in points
         * @throws IllegalArgumentException if there is no form widget at the provided position.
         * @throws IllegalStateException    if the renderer or page is closed
         */
        @NonNull
        @FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
        public FormWidgetInfo getFormWidgetInfoAtPosition(int x, int y) {
            throwIfDocumentOrPageClosed();
            return mPdfProcessor.getFormWidgetInfoAtPosition(mIndex, x, y);
        }

        /**
         * Applies a {@link FormEditRecord} to the PDF.
         *
         * <p>Apps must call {@link #render(Bitmap, Rect, Matrix, RenderParams)} to render new
         * bitmaps for the corresponding areas of the page.
         *
         * <p>For click type {@link FormEditRecord}s, performs a click on {@link
         * FormEditRecord#getClickPoint()}
         *
         * <p>For set text type {@link FormEditRecord}s, sets the text value of the form widget.
         *
         * <p>For set indices type {@link FormEditRecord}s, sets the {@link
         * FormEditRecord#getSelectedIndices()} as selected and all others as unselected for the
         * form widget indicated by the record.
         *
         * @param editRecord the {@link FormEditRecord} to be applied
         * @return Rectangular areas of the page bitmap that have been invalidated by this action.
         * @throws IllegalArgumentException if the provided {@link FormEditRecord} cannot be
         *                                  applied to the widget indicated by the index, or if the
         *                                  index does not correspond to a widget on the page.
         * @throws IllegalStateException    If the document is already closed.
         * @throws IllegalStateException    If the page is already closed.
         */
        @NonNull
        @FlaggedApi(Flags.FLAG_ENABLE_FORM_FILLING)
        public List<Rect> applyEdit(@NonNull FormEditRecord editRecord) {
            throwIfDocumentOrPageClosed();
            return mPdfProcessor.applyEdit(mIndex, editRecord);
        }

        /**
         * Closes this page.
         *
         * @see android.graphics.pdf.PdfRenderer#openPage(int)
         */
        @SuppressLint("UnflaggedApi")
        @Override
        public void close() {
            throwIfDocumentOrPageClosed();
            doClose();
        }

        @Override
        @SuppressLint("GenericException")
        protected void finalize() throws Throwable {
            try {
                if (mCloseGuard != null) {
                    mCloseGuard.warnIfOpen();
                }

                doClose();
            } finally {
                super.finalize();
            }
        }

        private void doClose() {
            if (mPdfProcessor != null) {
                mPdfProcessor.releasePage(mIndex);
                mIndex = -1;
            }

            mCloseGuard.close();
        }

        private void throwIfDocumentOrPageClosed() {
            throwIfDocumentClosed();
            throwIfPageClosed();
        }

        private void throwIfPageClosed() {
            if (mIndex == -1) {
                throw new IllegalStateException("Page already closed");
            }
        }
    }
}
