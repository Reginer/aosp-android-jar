/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.service.autofill;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.TestApi;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;

/**
 * Helper object used to obtain the value of a field in the screen being autofilled.
 *
 * @hide
 */
@TestApi
public interface ValueFinder {

    /**
     * Gets the value of a field as String, or {@code null} when not found.
     */
    @Nullable
    default String findByAutofillId(@NonNull AutofillId id) {
        final AutofillValue value = findRawValueByAutofillId(id);
        return (value == null || !value.isText()) ? null : value.getTextValue().toString();
    }

    /**
     * Gets the value of a field, or {@code null} when not found.
     */
    @Nullable
    AutofillValue findRawValueByAutofillId(@NonNull AutofillId id);
}
