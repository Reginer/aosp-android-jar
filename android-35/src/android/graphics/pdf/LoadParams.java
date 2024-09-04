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

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.pdf.flags.Flags;

/**
 * Represents a set of parameters to load the PDF document.
 */
@FlaggedApi(Flags.FLAG_ENABLE_PDF_VIEWER)
public final class LoadParams {
    @Nullable
    private final String mPassword;

    private LoadParams(String password) {
        mPassword = password;
    }

    /**
     * Gets the password for the loaded document. Returns {@code null} if not set.
     *
     * @return the password for the document.
     */
    @Nullable
    public String getPassword() {
        return mPassword;
    }

    /**
     * Builder for constructing {@link LoadParams}.
     */
    public static final class Builder {
        @Nullable
        private String mPassword;

        /** Constructor for builder to create {@link LoadParams}. */
        public Builder() {
        }

        /**
         * Sets the optional password for a protected PDF document. A {@code null} value will be
         * treated as no password supplied or document is unprotected.
         *
         * @param password Password for the protected PDF document.
         */
        @NonNull
        public Builder setPassword(@Nullable String password) {
            mPassword = password;
            return this;
        }

        /**
         * Builds the {@link LoadParams} after the optional values has been set.
         *
         * @return new instance of {@link LoadParams}
         */
        @NonNull
        public LoadParams build() {
            return new LoadParams(mPassword);
        }
    }
}
