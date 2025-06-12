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

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.pdf.component.PdfAnnotation;
import android.graphics.pdf.component.PdfPageObject;
import android.graphics.pdf.content.PdfPageGotoLinkContent;
import android.graphics.pdf.models.FormWidgetInfo;
import android.graphics.pdf.models.jni.LinkRects;
import android.graphics.pdf.models.jni.LoadPdfResult;
import android.graphics.pdf.models.jni.MatchRects;
import android.graphics.pdf.models.jni.PageSelection;
import android.graphics.pdf.models.jni.SelectionBoundary;
import android.graphics.pdf.utils.StrictModeUtils;
import android.os.ParcelFileDescriptor;

import java.util.List;

/**
 * This class accesses the PdfClient tools to manipulate and render a PDF document. One instance of
 * this class corresponds to one PDF document, loads it within PdfClient and keeps an internal
 * reference to the resulting object, to be re-used in subsequent calls.
 *
 * <p>This class is mostly a JNI gateway to PdfClient.
 *
 * @hide
 */
public class PdfDocumentProxy {
    private static final String TAG = "PdfDocument";

    private static final String LIB_NAME = "pdfclient";

    /** Internal reference to a native pointer to a Document object. */
    private final long mPdfDocPtr;

    private final int mNumPages;

    /** Constructs a PdfDocument. Do not call directly from java, use {@link #createFromFd}. */
    protected PdfDocumentProxy(long pdfDocPtr, int numPages) {
        this.mPdfDocPtr = pdfDocPtr;
        this.mNumPages = numPages;
    }

    /**
     * Tries to load a PdfDocument from native file descriptor.
     *
     * @return a LoadPdfResult of status LOADED containing the PdfDocument,
     * or, an empty LoadPdfResult of a different status indicating failure.
     */
    public static native LoadPdfResult createFromFd(int fd, String password);

    /**
     * Loads the PdfClient binary library used to render PDF documents. The library will only be
     * loaded once so subsequent calls after the first will have no effect. This may be used to
     * preload the library before use.
     */
    public static void loadLibPdf() {
        // TODO(b/324549320): Cleanup if bypassing is not required
        StrictModeUtils.bypass(() -> System.loadLibrary(LIB_NAME));
    }

    public long getPdfDocPtr() {
        return mPdfDocPtr;
    }

    public int getNumPages() {
        return mNumPages;
    }

    /** Destroys the PDF document and release resources held by PdfClient. */
    public native void destroy();

    /**
     * Tries to save this PdfDocument to the given native file descriptor, which must be open for
     * write or append.
     *
     * @return true on success
     */
    public native boolean saveToFd(int fd);

    /**
     * Saves the current state of this {@link PdfDocument} to the given, writable, file descriptor.
     * The given file descriptor is closed by this function.
     *
     * @param destination the file descriptor to write to
     * @return true on success
     */
    public boolean saveAs(ParcelFileDescriptor destination) {
        return saveToFd(destination.detachFd());
    }

    /**
     * Returns the width of the given page of the PDF. This is measured in points, but we
     * zoom-to-fit, so it doesn't matter.
     */
    public native int getPageWidth(int pageNum);

    /**
     * Returns the height of the given page of the PDF. This is measured in points, but we
     * zoom-to-fit, so it doesn't matter.
     */
    public native int getPageHeight(int pageNum);

    /**
     * Renders a page to a bitmap.
     *
     * @param pageNum          the page number of the page to be rendered
     * @param clipLeft         the left coordinate of the clipping boundary in bitmap coordinates
     * @param clipTop          the top coordinate of the clipping boundary in bitmap coordinates
     * @param clipRight        the right coordinate of the clipping boundary in bitmap coordinates
     * @param clipBottom       the bottom coordinate of the clipping boundary in bitmap coordinates
     * @param transform        an affine transform matrix in the form of an array.
     * @param renderMode       the render mode
     * @param showAnnotTypes   Bitmask of renderFlags to indicate the types of annotations to
     *                         be rendered
     * @param renderFormFields true to included PDF form content in the output
     * @return true if the page was rendered into the destination bitmap
     * @see android.graphics.Matrix#getValues(float[])
     */
    public native boolean render(
            int pageNum,
            Bitmap bitmap,
            int clipLeft,
            int clipTop,
            int clipRight,
            int clipBottom,
            float[] transform,
            int renderMode,
            int showAnnotTypes,
            boolean renderFormFields);

    /**
     * Clones the currently loaded document using the provided file descriptor.
     * <p>You are required to detach the file descriptor as the native code will close it.
     *
     * @param destination native fd pointer
     * @return true if the cloning was successful
     */
    private native boolean cloneWithoutSecurity(int destination);

    /**
     * Clones the currently loaded document using the provided file descriptor.
     * <p>You are required to detach the file descriptor as the native code will close it.
     *
     * @param destination {@link ParcelFileDescriptor} to which the document needs to be written to.
     * @return true if the cloning was successful
     */
    public boolean cloneWithoutSecurity(ParcelFileDescriptor destination) {
        return cloneWithoutSecurity(destination.detachFd());
    }

    /**
     * Gets the text of the entire page as a string, in the order the text is
     * found in the PDF stream.
     */
    public native String getPageText(int pageNum);

    /**
     * Gets all pieces of alt-text found for the page, in the order the alt-text is found in the
     * PDF stream.
     */
    public native List<String> getPageAltText(int pageNum);

    /**
     * Searches for the given string on the page and returns the bounds of all of the matches.
     * The number of matches is {@link MatchRects#size()}.
     */
    public native MatchRects searchPageText(int pageNum, String query);

    /**
     * Get the text selection that spans between the two boundaries (inclusive of start and
     * exclusive of stop), both of which can be either exactly defined with text indexes, or
     * approximately defined with points on the page. The resulting selection will also be exactly
     * defined with both indexes and points. If the start and stop boundary are both the same point,
     * selects the word at that point.
     */
    public native PageSelection selectPageText(int pageNum, SelectionBoundary start,
            SelectionBoundary stop);

    /** Get the bounds and URLs of all the links on the given page. */
    public native LinkRects getPageLinks(int pageNum);

    /** Returns bookmarks and other goto links (within the current document) on a page */
    public native List<PdfPageGotoLinkContent> getPageGotoLinks(int pageNum);

    /** Loads a page object and retains it in memory when a page becomes visible. */
    public native void retainPage(int pageNum);

    /** Cleans up objects in memory related to a page after it is no longer visible. */
    public native void releasePage(int pageNum);

    /** Returns true if the PDF is linearized. (May give false negatives for <1KB PDFs). */
    public native boolean isPdfLinearized();

    /** Returns true if the document prefers to be scaled for printing. */
    public native boolean scaleForPrinting();

    /**
     * Returns an int representing the form type contained in the PDF, e.g. Acro vs XFA (if any).
     */
    public native int getFormType();

    /** Obtains information about the widget at point ({@code x}, {@code y}), if any. */
    public native FormWidgetInfo getFormWidgetInfo(int pageNum, int x, int y);

    /**
     * Obtains information about the widget with ({@code annotationIndex} on page {@code pageNum}),
     * if any.
     */
    public native FormWidgetInfo getFormWidgetInfo(int pageNum, int annotationIndex);

    /**
     * Obtains information about all form widgets on page ({@code pageNum}, if any.
     *
     * <p>Optionally restricts by {@code typeIds}. If {@code typeIds} is empty, all form widgets on
     * the page will be returned.
     */
    public native List<FormWidgetInfo> getFormWidgetInfos(int pageNum, int[] typeIds);

    /**
     * Executes an interactive click on the page at the given point ({@code x}, {@code y}).
     *
     * @return rectangular areas of the page bitmap that have been invalidated by this action
     */
    public native List<Rect> clickOnPage(int pageNum, int x, int y);

    /**
     * Sets the text of the widget at {@code annotationIndex}, if applicable.
     *
     * @return rectangular areas of the page bitmap that have been invalidated by this action
     */
    public native List<Rect> setFormFieldText(int pageNum, int annotIndex, String text);

    /**
     * Selects the {@code selectedIndices} and unselects all others for the widget at {@code
     * annotationIndex}, if applicable.
     *
     * @return Rectangular areas of the page bitmap that have been invalidated by this action
     */
    public native List<Rect> setFormFieldSelectedIndices(
            int pageNum, int annotIndex, int[] selectedIndices);

    /**
     * Returns the list of {@link PdfAnnotation} present on the page.
     * The list item is non-null for supported types (freetext, image, stamp) and
     * null for unsupported types.
     *
     * @param pageNum - page number of the page whose annotations list is to be returned
     * @return A {@link List} of {@link PdfAnnotation}
     */
    public native @NonNull List<PdfAnnotation> getPageAnnotations(
            @IntRange(from = 0) int pageNum);

    /**
     * Adds the given {@link PdfAnnotation} to the given page
     *
     * @param pageNum    - page number of the page to which annotation is to be added
     * @param annotation - {@link PdfAnnotation} to be added to the given page
     * @return index of the annotation added, -1 in case of failure
     */
    public native int addPageAnnotation(@IntRange(from = 0) int pageNum,
            @NonNull PdfAnnotation annotation);

    /**
     * Removes the {@link PdfAnnotation} with the specified index from the given page.
     *
     * @param pageNum      - page number from which {@link PdfAnnotation} is to be removed
     * @param annotationIndex - index of the {@link PdfAnnotation} to be removed
     *
     * @return true if remove was successful, false otherwise
     */
    public native boolean removePageAnnotation(@IntRange(from = 0) int pageNum,
            @IntRange(from = 0) int annotationIndex);

    /**
     * Update the given {@link PdfAnnotation} on the given page
     *
     * @param pageNum    page number on which annotation is to be updated
     * @param annotationIndex index of the annotation
     * @param annotation annotation to be updated
     *
     * @return true if page object is updated, false otherwise
     */
    public native boolean updatePageAnnotation(@IntRange(from = 0) int pageNum,
            int annotationIndex, PdfAnnotation annotation);


    /**
     * Returns the list of {@link PdfPageObject} present on the page.
     * The list item is non-null for supported types and
     * null for unsupported types.
     *
     * @param pageNum - page number of the page whose annotations list is to be returned
     * @return A {@link List} of {@link PdfPageObject}
     */
    public native List<PdfPageObject> getPageObjects(int pageNum);

    /**
     * Adds the given page object to the page.
     *
     * @param pageNum    - page number of the page to which pageObject is to be added
     * @param pageObject - {@link PdfPageObject} to be added to the given page
     * @return index of added page object, -1 in the case of failure
     */
    public native int addPageObject(int pageNum, @NonNull PdfPageObject pageObject);

    /**
     * Update the given {@link PdfPageObject} on the given page
     *
     * @param pageNum    page number on which the {@link PdfPageObject} is to be updated
     * @param objectIndex   index of the pageObject
     * @param pageObject pageObject to be updated
     *
     * @return true if page object is updated, false otherwise
     */
    public native boolean updatePageObject(int pageNum, int objectIndex,
            @NonNull PdfPageObject pageObject);

    /**
     * Removes the {@link PdfPageObject} with the specified Index from the given page.
     *
     * @param pageNum  - page number from which {@link PdfPageObject} is to be removed
     * @param objectIndex the index of the {@link PdfPageObject} to be removed
     *
     * @return true if remove was successful, false otherwise
     */
    public native boolean removePageObject(int pageNum, int objectIndex);
}
