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

import static com.android.healthfitness.flags.Flags.FLAG_EXPORT_IMPORT;

import android.annotation.DrawableRes;
import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Document provider that can be used with export/import.
 *
 * @hide
 */
@FlaggedApi(FLAG_EXPORT_IMPORT)
public final class ExportImportDocumentProvider implements Parcelable {
    @NonNull
    public static final Creator<ExportImportDocumentProvider> CREATOR =
            new Creator<>() {
                @Override
                public ExportImportDocumentProvider createFromParcel(Parcel in) {
                    return new ExportImportDocumentProvider(in);
                }

                @Override
                public ExportImportDocumentProvider[] newArray(int size) {
                    return new ExportImportDocumentProvider[size];
                }
            };

    @NonNull private final String mTitle;
    @NonNull private final String mSummary;
    private final @DrawableRes int mIconResource;
    @NonNull private final Uri mRootUri;

    private ExportImportDocumentProvider(@NonNull Parcel in) {
        mTitle = Objects.requireNonNull(in.readString());
        mSummary = Objects.requireNonNull(in.readString());
        mIconResource = in.readInt();
        mRootUri = Uri.parse(Objects.requireNonNull(in.readString()));
    }

    public ExportImportDocumentProvider(
            @NonNull String title,
            @NonNull String summary,
            @DrawableRes int iconResource,
            @NonNull Uri rootUri) {
        mTitle = title;
        mSummary = summary;
        mIconResource = iconResource;
        mRootUri = rootUri;
    }

    /** Returns the title for the document provider (usually corresponds to the app name). */
    @NonNull
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the summary for the document provider (usually corresponds to the user's account
     * name).
     */
    @NonNull
    public String getSummary() {
        return mSummary;
    }

    /**
     * Returns the icon resource ID for the document provider (usually corresponds to the app icon).
     */
    public @DrawableRes int getIconResource() {
        return mIconResource;
    }

    /**
     * Returns the URI for the document provider. The URI can be passed to ACTION_CREATE_DOCUMENT or
     * ACTION_OPEN_DOCUMENT with DocumentsContract.EXTRA_INITIAL_URI.
     */
    @NonNull
    public Uri getRootUri() {
        return mRootUri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mTitle);
        dest.writeString(mSummary);
        dest.writeInt(mIconResource);
        dest.writeString(mRootUri.toString());
    }
}
