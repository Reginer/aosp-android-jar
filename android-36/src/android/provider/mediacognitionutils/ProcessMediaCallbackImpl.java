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

package android.provider.mediacognitionutils;

import static android.provider.MediaCognitionService.ProcessingTypes;

import android.annotation.NonNull;
import android.database.CursorWindow;
import android.os.RemoteException;
import android.provider.MediaCognitionProcessingCallback;
import android.provider.MediaCognitionProcessingRequest;
import android.provider.MediaCognitionProcessingResponse;
import android.provider.MediaCognitionService;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * @hide
 */
public final class ProcessMediaCallbackImpl implements MediaCognitionProcessingCallback {

    private final ICognitionProcessMediaCallbackInternal mBinderCallback;

    private static final String RESPONSE_CURSOR_NAME = "MediaCognitionResponseCursorWindow";

    /**
     * To ensure a callback instance is called only once.
     */
    private boolean mCalled;

    public ProcessMediaCallbackImpl(ICognitionProcessMediaCallbackInternal binderCallback) {
        mBinderCallback = binderCallback;
        mCalled = false;
    }

    @Override
    public void onSuccess(@NonNull List<MediaCognitionProcessingResponse> responses) {
        if (mCalled) {
            Log.w(MediaCognitionService.TAG, "The callback can only be called once");
            return;
        }
        mCalled = true;
        if (responses.size() == 0) {
            Log.w(MediaCognitionService.TAG, "Empty response");
            return;
        }
        try {
            mBinderCallback.onProcessMediaSuccess(wrapInCursorWindows(responses));
        } catch (RemoteException e) {
            Log.w(MediaCognitionService.TAG,
                    "Unable to send callback of Process Media result", e);
        }

    }

    @Override
    public void onFailure(@NonNull String message) {
        if (mCalled) {
            Log.w(MediaCognitionService.TAG, "The callback can only be called once");
            return;
        }
        mCalled = true;
        try {
            mBinderCallback.onProcessMediaFailure(message);
        } catch (RemoteException e) {
            Log.w(MediaCognitionService.TAG,
                    "Unable to send callback of Process Media failure", e);
        }
    }


    /**
     * This runs in the app's process which has implemented {@link MediaCognitionService}
     * Before passing the response to MediaProvider, the responses should be wrapped
     * using {@link CursorWindow} to avoid risk of parcelable limit of an ipc.
     *
     */
    private CursorWindow[] wrapInCursorWindows(List<MediaCognitionProcessingResponse> responses) {
        ArrayList<CursorWindow> windows = new ArrayList<CursorWindow>();
        CursorWindow window = new CursorWindow(RESPONSE_CURSOR_NAME);
        // number of columns =  number of processing available + Id of the media
        final int numColumns = ProcessingTypes.class.getDeclaredFields().length + 1;
        window.setNumColumns(numColumns);
        windows.add(window);
        /*
        In the following loop, we iterate through responses and add them to current CursorWindow.
        If the window is full then addition of a row to that window will fail.
        At this point we create a new window.
         */
        try {
            boolean retryingRowAddition = false;
            for (int row = 0; row < responses.size(); row++) {
                // Allocate a new row for each entry.
                if (!window.allocRow()) {
                    // This window is full. Allocate a new CursorWindow.
                    Log.d(MediaCognitionService.TAG, "Allocating additional cursor"
                            + " window for large data set (row " + row + ")");
                    window = new CursorWindow(RESPONSE_CURSOR_NAME);
                    window.setStartPosition(row);
                    window.setNumColumns(numColumns);
                    windows.add(window);
                    if (!window.allocRow()) {
                        // If we couldn't allocate a row even after creating a new CursorWindow, we
                        // can't proceed.
                        Log.e(MediaCognitionService.TAG, "Unable to allocate row"
                                + " to hold data.");
                        windows.remove(window);
                        return windows.toArray(new CursorWindow[windows.size()]);
                    }
                }

                // current window is ready to be populated with data
                boolean dataAdded = true;

                // Adding ID of the media
                final MediaCognitionProcessingRequest request = responses.get(row).getRequest();
                if (request == null || request.getUri() == null
                        || request.getUri().getLastPathSegment() == null) {
                    Log.w(MediaCognitionService.TAG, "Correct request not"
                            + " set in the response");
                    window.freeLastRow();
                    retryingRowAddition = false;
                    continue;
                }
                dataAdded &= window.putString(request.getUri().getLastPathSegment(),
                        row, 0);
                dataAdded &= addImageOcrLatin(window, responses.get(row), request, row);
                dataAdded &= addImageLabels(window, responses.get(row), request, row);

                if (!dataAdded) {
                    // if data addition failed, retry with a new window
                    if (retryingRowAddition) {
                        // if already retrying and failed again, skip this response
                        Log.w(MediaCognitionService.TAG, "Could not add value to cursorWindow"
                                + " The size may be larger than limit ");
                        // prepare for next item
                        window.clear();
                        window.setStartPosition(row + 1);
                        window.setNumColumns(numColumns);
                        retryingRowAddition = false;
                        continue;
                    }
                    Log.d(MediaCognitionService.TAG, "Couldn't populate window "
                            + "data for row " + row + " - allocating new window.");
                    window.freeLastRow();
                    window = new CursorWindow(RESPONSE_CURSOR_NAME);
                    window.setStartPosition(row);
                    window.setNumColumns(numColumns);
                    windows.add(window);
                    row--;
                    retryingRowAddition = true;
                } else {
                    retryingRowAddition = false;
                }
            }
        } catch (RuntimeException re) {
            // Something went wrong while filling the window. Close all windows, and re-throw.
            for (int i = 0, count = windows.size(); i < count; i++) {
                windows.get(i).close();
            }
            throw re;
        }
        return windows.toArray(new CursorWindow[windows.size()]);
    }

    private boolean addImageOcrLatin(CursorWindow window, MediaCognitionProcessingResponse response,
            MediaCognitionProcessingRequest request, int row) {
        final int column = Integer.numberOfTrailingZeros(ProcessingTypes.IMAGE_OCR_LATIN) + 1;
        // check if ImageOcrLatin request was set
        if (request.checkProcessingRequired(ProcessingTypes.IMAGE_OCR_LATIN)
                && response.getImageOcrLatin() != null) {
            return window.putString(response.getImageOcrLatin(), row, column);
        }
        return window.putNull(row, column);
    }

    private boolean addImageLabels(CursorWindow window, MediaCognitionProcessingResponse response,
            MediaCognitionProcessingRequest request, int row) {
        final int column = Integer.numberOfTrailingZeros(ProcessingTypes.IMAGE_LABEL) + 1;
        if (request.checkProcessingRequired(ProcessingTypes.IMAGE_LABEL)
                && response.getImageLabels() != null) {
            return window.putString(String.join("|", response.getImageLabels()),
                    row, column);
        }
        return window.putNull(row, column);
    }
}

