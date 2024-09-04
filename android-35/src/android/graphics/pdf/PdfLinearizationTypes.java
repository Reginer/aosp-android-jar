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

import android.annotation.IntDef;

import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This class holds the set of constants representing the type of PDF document.
 *
 * @hide
 */
public final class PdfLinearizationTypes {
    /** Represents the type of PDF document that cannot be determined. */
    public static final int PDF_DOCUMENT_TYPE_UNKNOWN = 0;

    /** Represents a non-linearized PDF document. */
    public static final int PDF_DOCUMENT_TYPE_NON_LINEARIZED = 1;

    /** Represents a linearized PDF document. */
    public static final int PDF_DOCUMENT_TYPE_LINEARIZED = 2;

    private PdfLinearizationTypes() {
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = {"PDF_DOCUMENT_TYPE_"}, value = {PDF_DOCUMENT_TYPE_UNKNOWN,
            PDF_DOCUMENT_TYPE_NON_LINEARIZED,
            PDF_DOCUMENT_TYPE_LINEARIZED})
    public @interface PdfLinearizationType {
    }
}
