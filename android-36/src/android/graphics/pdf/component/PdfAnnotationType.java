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

package android.graphics.pdf.component;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.graphics.pdf.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This class holds the set of constants representing the types of a PDF annotation.
 */
@FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_ANNOTATIONS)
public final class PdfAnnotationType {

    // Private constructor
    private PdfAnnotationType() {
    }


    /**
     * Represents the type of annotation that cannot be determined
     */
    public static final int UNKNOWN = 0;

    /**
     * Represents a freetext annotation
     */
    // Todo: b/382076427 - Add test for validating the behavior for this flag enabled/disabled
    @FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_TEXT_ANNOTATIONS)
    public static final int FREETEXT = 1;

    /**
     * Represents a highlight annotation
     */
    public static final int HIGHLIGHT = 2;

    /**
     * Represents a stamp annotation
     */
    // Todo: b/382036496 - Add stamp annotation class in a follow up cl
    public static final int STAMP = 3;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({UNKNOWN, FREETEXT, HIGHLIGHT, STAMP})
    public @interface Type {
    }
}


