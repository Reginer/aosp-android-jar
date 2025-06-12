/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.app.appsearch.aidl;

import android.annotation.NonNull;
import android.app.appsearch.GenericDocument;
import android.app.appsearch.ParcelableUtil;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.GenericDocumentParcel;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.Objects;

/**
 * The Parcelable object contains a List of {@link GenericDocument}.
 *
 * <p>This class will batch a list of {@link GenericDocument}. If the number of documents is too
 * large for a transaction, they will be put to Android Shared Memory.
 *
 * @see Parcel#writeBlob(byte[])
 * @hide
 */
@SafeParcelable.Class(creator = "DocumentsParcelCreator", creatorIsFinal = false)
public final class DocumentsParcel extends AbstractSafeParcelable {
    public static final Parcelable.Creator<DocumentsParcel> CREATOR =
            new DocumentsParcelCreator() {
                @Override
                public DocumentsParcel createFromParcel(Parcel in) {
                    byte[] dataBlob = Objects.requireNonNull(ParcelableUtil.readBlob(in));
                    // Create a parcel object to un-serialize the byte array we are reading from
                    // Parcel.readBlob(). Parcel.WriteBlob() could take care of whether to pass data
                    // via
                    // binder directly or Android shared memory if the data is large.
                    Parcel unmarshallParcel = Parcel.obtain();
                    try {
                        unmarshallParcel.unmarshall(dataBlob, 0, dataBlob.length);
                        unmarshallParcel.setDataPosition(0);
                        return super.createFromParcel(unmarshallParcel);
                    } finally {
                        unmarshallParcel.recycle();
                    }
                }
            };

    @Field(id = 1, getter = "getDocumentParcels")
    final List<GenericDocumentParcel> mDocumentParcels;

    @Field(id = 2, getter = "getTakenActionGenericDocumentParcels")
    final List<GenericDocumentParcel> mTakenActionGenericDocumentParcels;

    @Constructor
    public DocumentsParcel(
            @Param(id = 1) List<GenericDocumentParcel> documentParcels,
            @Param(id = 2) List<GenericDocumentParcel> takenActionGenericDocumentParcels) {
        mDocumentParcels = documentParcels;
        mTakenActionGenericDocumentParcels = takenActionGenericDocumentParcels;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ParcelableUtil.writeBlob(dest, serializeToByteArray(flags));
    }

    /**
     * Serializes the provided list of documents, So that we can use Parcel.writeBlob() to send
     * data.
     *
     * <p>WriteBlob() will take care of whether to pass data via binder directly or Android shared
     * memory if the data is large.
     */
    @NonNull
    private byte[] serializeToByteArray(int flags) {
        byte[] bytes;
        Parcel data = Parcel.obtain();
        try {
            DocumentsParcelCreator.writeToParcel(this, data, flags);
            bytes = data.marshall();
        } finally {
            data.recycle();
        }
        return bytes;
    }

    /** Returns the List of {@link GenericDocument} of this object. */
    @NonNull
    public List<GenericDocumentParcel> getDocumentParcels() {
        return mDocumentParcels;
    }

    /** Returns the List of TakenActions as {@link GenericDocument}. */
    @NonNull
    public List<GenericDocumentParcel> getTakenActionGenericDocumentParcels() {
        return mTakenActionGenericDocumentParcels;
    }

    /** Returns sum of the counts of Documents and TakenActionGenericDocuments. */
    public int getTotalDocumentCount() {
        return mDocumentParcels.size() + mTakenActionGenericDocumentParcels.size();
    }
}
