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

package android.app.appsearch;

import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Wrapper class to provide implementation for readBlob/writeBlob for all API levels.
 *
 * @hide
 */
public class ParcelableUtil {
    private static final String TAG = "AppSearchParcel";
    private static final String TEMP_FILE_PREFIX = "AppSearchSerializedBytes";
    private static final String TEMP_FILE_SUFFIX = ".tmp";
    // Same as IBinder.MAX_IPC_LIMIT. Limit that should be placed on IPC sizes to keep them safely
    // under the transaction buffer limit.
    private static final int DOCUMENT_SIZE_LIMIT_IN_BYTES = 64 * 1024;


    // TODO(b/232805516): Update SDK_INT in Android.bp to safeguard from unexpected compiler issues.
    @SuppressLint("ObsoleteSdkInt")
    public static void writeBlob(@NonNull Parcel parcel, @NonNull byte[] bytes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Since parcel.writeBlob was added in API level 33, it is not available
            // on lower API levels.
            parcel.writeBlob(bytes);
        } else {
            writeToParcelForSAndBelow(parcel, bytes);
        }
    }

    private static void writeToParcelForSAndBelow(Parcel parcel, byte[] bytes) {
        try {
            parcel.writeInt(bytes.length);
            if (bytes.length <= DOCUMENT_SIZE_LIMIT_IN_BYTES) {
                parcel.writeByteArray(bytes);
            } else {
                ParcelFileDescriptor parcelFileDescriptor =
                        writeDataToTempFileAndUnlinkFile(bytes);
                parcel.writeFileDescriptor(parcelFileDescriptor.getFileDescriptor());
            }
        } catch (IOException e) {
            // TODO(b/232805516): Add abstraction to handle the exception based on environment.
            Log.w(TAG, "Couldn't write to unlinked file.", e);
        }
    }

    @NonNull
    // TODO(b/232805516): Update SDK_INT in Android.bp to safeguard from unexpected compiler issues.
    @SuppressLint("ObsoleteSdkInt")
    public static byte[] readBlob(Parcel parcel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Since parcel.readBlob was added in API level 33, it is not available
            // on lower API levels.
            return parcel.readBlob();
        } else {
            return readFromParcelForSAndBelow(parcel);
        }
    }

    private static byte[] readFromParcelForSAndBelow(Parcel parcel) {
        try {
            int length = parcel.readInt();
            if (length <= DOCUMENT_SIZE_LIMIT_IN_BYTES) {
                byte[] documentByteArray = new byte[length];
                parcel.readByteArray(documentByteArray);
                return documentByteArray;
            } else {
                ParcelFileDescriptor pfd = parcel.readFileDescriptor();
                return getDataFromFd(pfd, length);
            }
        } catch (IOException e) {
            // TODO(b/232805516): Add abstraction to handle the exception based on environment.
            Log.w(TAG, "Couldn't read from unlinked file.", e);
            return null;
        }
    }

    /**
     * Reads data bytes from file using provided FileDescriptor. It also closes the PFD so that
     * will delete the underlying file if it's the only reference left.
     *
     * @param pfd ParcelFileDescriptor for the file to read.
     * @param length Number of bytes to read from the file.
     */
    private static byte[] getDataFromFd(ParcelFileDescriptor pfd,
            int length) throws IOException {
        try(DataInputStream in =
                new DataInputStream(new ParcelFileDescriptor.AutoCloseInputStream(pfd))){
            byte[] data = new byte[length];
            in.read(data);
            return data;
        }
    }

    /**
     * Writes to a temp file owned by the caller, then unlinks/deletes it, and returns an FD which
     * is the only remaining reference to the tmp file.
     *
     * @param data Data in the form of byte array to write to the file.
     */
    private static ParcelFileDescriptor writeDataToTempFileAndUnlinkFile(byte[] data)
            throws IOException {
        // TODO(b/232959004):  Update directory to app-specific cache dir instead of null.
        File unlinkedFile =
                File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX, /* directory= */ null);
        try(DataOutputStream out = new DataOutputStream(new FileOutputStream(unlinkedFile))) {
            out.write(data);
            out.flush();
        }
        ParcelFileDescriptor parcelFileDescriptor =
                ParcelFileDescriptor.open(unlinkedFile,
                        ParcelFileDescriptor.MODE_CREATE
                                | ParcelFileDescriptor.MODE_READ_WRITE);
        unlinkedFile.delete();
        return parcelFileDescriptor;
    }

    private ParcelableUtil() {}
}
