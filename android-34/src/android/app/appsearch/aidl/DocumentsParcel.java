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
import android.app.appsearch.ParcelableUtil;
import android.app.appsearch.GenericDocument;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The Parcelable object contains a List of {@link GenericDocument}.
 *
 * <P>This class will batch a list of {@link GenericDocument}. If the number of documents is too
 * large for a transact, they will be put to Android Shared Memory.
 *
 * @see Parcel#writeBlob(byte[])
 * @hide
 */
public final class DocumentsParcel implements Parcelable {
    private final List<GenericDocument> mDocuments;

    public DocumentsParcel(@NonNull List<GenericDocument> documents) {
        mDocuments = Objects.requireNonNull(documents);
    }

    private DocumentsParcel(@NonNull Parcel in) {
        mDocuments = readFromParcel(in);
    }

    private List<GenericDocument> readFromParcel(Parcel source) {
        byte[] dataBlob = ParcelableUtil.readBlob(source);
        // Create a parcel object to un-serialize the byte array we are reading from
        // Parcel.readBlob(). Parcel.WriteBlob() could take care of whether to pass data via
        // binder directly or Android shared memory if the data is large.
        Parcel unmarshallParcel = Parcel.obtain();
        try {
            unmarshallParcel.unmarshall(dataBlob, 0, dataBlob.length);
            unmarshallParcel.setDataPosition(0);
            // read the number of document that stored in here.
            int size = unmarshallParcel.readInt();
            List<GenericDocument> documentList = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                // Read document's bundle and convert them.
                documentList.add(new GenericDocument(
                        unmarshallParcel.readBundle(getClass().getClassLoader())));
            }
            return documentList;
        } finally {
            unmarshallParcel.recycle();
        }
    }

    public static final Creator<DocumentsParcel> CREATOR = new Creator<DocumentsParcel>() {
        @Override
        public DocumentsParcel createFromParcel(Parcel in) {
            return new DocumentsParcel(in);
        }

        @Override
        public DocumentsParcel[] newArray(int size) {
            return new DocumentsParcel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        byte[] documentsByteArray = serializeToByteArray();
        ParcelableUtil.writeBlob(dest, documentsByteArray);
    }

    /**
     * Serializes the whole object, So that we can use Parcel.writeBlob() to send data. WriteBlob()
     * could take care of whether to pass data via binder directly or Android shared memory if the
     * data is large.
     */
    @NonNull
    private byte[] serializeToByteArray() {
        byte[] bytes;
        Parcel data = Parcel.obtain();
        try {
            // Save the number documents to the temporary Parcel object.
            data.writeInt(mDocuments.size());
            // Save all document's bundle to the temporary Parcel object.
            for (int i = 0; i < mDocuments.size(); i++) {
                data.writeBundle(mDocuments.get(i).getBundle());
            }
            bytes = data.marshall();
        } finally {
            data.recycle();
        }
        return bytes;
    }

    /**  Returns the List of {@link GenericDocument} of this object. */
    @NonNull
    public List<GenericDocument> getDocuments() {
        return mDocuments;
    }
}
