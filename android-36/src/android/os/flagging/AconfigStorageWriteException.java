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

package android.os.flagging;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.provider.flags.Flags;
import android.util.AndroidRuntimeException;

/**
 * Exception raised when there is an error writing to aconfig flag storage.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_NEW_STORAGE_PUBLIC_API)
public class AconfigStorageWriteException extends AndroidRuntimeException {
    @FlaggedApi(Flags.FLAG_NEW_STORAGE_PUBLIC_API)
    public AconfigStorageWriteException(@NonNull String message) {
        super(message);
    }

    @FlaggedApi(Flags.FLAG_NEW_STORAGE_PUBLIC_API)
    public AconfigStorageWriteException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }
}
