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

package android.graphics.pdf.models.jni;

import android.annotation.Nullable;
import android.graphics.pdf.PdfDocumentProxy;
import android.graphics.pdf.utils.Preconditions;

/**
 * A class that holds either a successfully loaded {@link PdfDocumentProxy}, or the reason why it
 * failed.
 *
 * @hide
 */
public class LoadPdfResult {

    public final PdfStatus status;
    @Nullable
    public final PdfDocumentProxy pdfDocument;

    public final float pdfSizeInKb;

    public LoadPdfResult(int status, @Nullable PdfDocumentProxy pdfDocument, float pdfSizeInKb) {
        if (status == PdfStatus.LOADED.getNumber()) {
            Preconditions.checkArgument(pdfDocument != null, "Missing PdfDocumentProxy");
        } else {
            Preconditions.checkArgument(pdfDocument == null, "Shouldn't construct "
                    + "broken PdfDocumentProxy");
        }
        // TODO(b/324910716): Potentially error-prone as is dependent on Status in document.h
        this.status = PdfStatus.values()[status];
        this.pdfDocument = pdfDocument;
        this.pdfSizeInKb = pdfSizeInKb;
    }
}
