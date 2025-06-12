/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.app.appsearch.safeparcel;

import android.annotation.FlaggedApi;

import com.android.appsearch.flags.Flags;

/**
 * Implements {@link SafeParcelable} and implements some default methods defined by {@link
 * android.os.Parcelable}.
 *
 * @hide
 */
// Include the SafeParcel source code directly in AppSearch until it gets officially open-sourced.
public abstract class AbstractSafeParcelable implements SafeParcelable {

    /** @hide */
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public final int describeContents() {
        return 0;
    }
}
