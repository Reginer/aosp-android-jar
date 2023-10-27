/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.provider;

import static android.provider.CloudMediaProviderContract.EXTRA_ERROR_MESSAGE;
import static android.provider.CloudMediaProviderContract.EXTRA_FILE_DESCRIPTOR;

import android.annotation.NonNull;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallback;
import android.os.RemoteException;

import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @hide
 */
public final class AsyncContentProvider {

    private static final long TIMEOUT_IN_SECONDS = 5L;

    private final IAsyncContentProvider mAsyncContentProvider;

    public AsyncContentProvider(@NonNull IAsyncContentProvider asyncContentProvider) {
        this.mAsyncContentProvider = asyncContentProvider;
    }

    @NonNull
    public ParcelFileDescriptor openMedia(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException, ExecutionException, InterruptedException,
            TimeoutException, RemoteException {
        String mediaId = uri.getLastPathSegment();
        CompletableFuture<ParcelFileDescriptor> future = new CompletableFuture<>();
        RemoteCallback callback = new RemoteCallback(result -> setResult(result, future));
        mAsyncContentProvider.openMedia(mediaId, callback);
        return future.get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    private void setResult(Bundle result, CompletableFuture<ParcelFileDescriptor> future) {
        if (result.containsKey(EXTRA_FILE_DESCRIPTOR)) {
            ParcelFileDescriptor pfd = result.getParcelable(EXTRA_FILE_DESCRIPTOR);
            future.complete(pfd);
        } else if (result.containsKey(EXTRA_ERROR_MESSAGE)) {
            future.completeExceptionally(
                    new RemoteException(
                            result.getString(EXTRA_ERROR_MESSAGE)));
        } else {
            future.completeExceptionally(
                    new RemoteException(
                            "File descriptor and error message missing in response from "
                                    + "CloudMediaProvider"));
        }
    }
}