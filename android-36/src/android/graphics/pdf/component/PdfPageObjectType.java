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
 * This class holds the set of constants representing the types of a PDF page objects.
 */
@FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_PAGE_OBJECTS)
public final class PdfPageObjectType {
    // Private constructor
    private PdfPageObjectType() {
    }

    /**
     * Represents the type of page object that cannot be determined
     */
    public static final int UNKNOWN = 0;

    /**
     * Represents a text page object
     */
    @FlaggedApi(Flags.FLAG_ENABLE_EDIT_PDF_TEXT_OBJECTS)
    public static final int TEXT = 1;
    /**
     * Represents a path page object
     */
    public static final int PATH = 2;

    /**
     * Represents an image page object
     */
    public static final int IMAGE = 3;


    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({UNKNOWN, TEXT, PATH, IMAGE})
    public @interface Type {
    }

    /**
     * Checks if the given type is a valid PDF page object type.
     *
     * @param type The type to check.
     * @return {@code true} if the type is valid, {@code false} otherwise.
     */
    public static boolean isValidType(int type) {
        if (Flags.enableEditPdfTextObjects()) {
            return type == TEXT || type == PATH || type == IMAGE;
        }
        return type == PATH || type == IMAGE;
    }
}
