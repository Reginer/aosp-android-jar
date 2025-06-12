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

package android.health.connect.exportimport;


import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Status for a data import.
 *
 * @hide
 */
public final class ImportStatus implements Parcelable {

    /**
     * No error during the last data import.
     *
     * @hide
     */
    public static final int DATA_IMPORT_ERROR_NONE = 0;

    /**
     * Unknown error during the last data import.
     *
     * @hide
     */
    public static final int DATA_IMPORT_ERROR_UNKNOWN = 1;

    /**
     * Indicates that the last import failed because the user picked a file that was not exported by
     * Health Connect.
     *
     * @hide
     */
    public static final int DATA_IMPORT_ERROR_WRONG_FILE = 2;

    /**
     * Indicates that the last import failed because the version of the on device schema does not
     * match the version of the imported database.
     *
     * @hide
     */
    public static final int DATA_IMPORT_ERROR_VERSION_MISMATCH = 3;

    /**
     * Indicates that an import was started and has not yet completed.
     *
     * @hide
     */
    public static final int DATA_IMPORT_STARTED = 4;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        DATA_IMPORT_ERROR_NONE,
        DATA_IMPORT_ERROR_UNKNOWN,
        DATA_IMPORT_ERROR_WRONG_FILE,
        DATA_IMPORT_ERROR_VERSION_MISMATCH,
        DATA_IMPORT_STARTED,
    })
    public @interface DataImportState {}

    @NonNull
    public static final Creator<ImportStatus> CREATOR =
            new Creator<>() {
                @Override
                public ImportStatus createFromParcel(Parcel in) {
                    return new ImportStatus(in);
                }

                @Override
                public ImportStatus[] newArray(int size) {
                    return new ImportStatus[size];
                }
            };

    @DataImportState private final int mDataImportState;

    public ImportStatus(@DataImportState int dataImportState) {
        mDataImportState = dataImportState;
    }

    /**
     * Returns the state of the last import attempt, which can indicate that the import has started,
     * has failed or has succeeded
     */
    public int getDataImportState() {
        return mDataImportState;
    }

    /** Convenience method to check if an import is ongoing. */
    public boolean isImportOngoing() {
        return mDataImportState == DATA_IMPORT_STARTED;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private ImportStatus(@NonNull Parcel in) {
        mDataImportState = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mDataImportState);
    }
}
