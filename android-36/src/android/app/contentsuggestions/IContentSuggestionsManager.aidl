/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.app.contentsuggestions;

import android.app.contentsuggestions.IClassificationsCallback;
import android.app.contentsuggestions.ISelectionsCallback;
import android.app.contentsuggestions.ClassificationsRequest;
import android.app.contentsuggestions.SelectionsRequest;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.UserHandle;
import com.android.internal.os.IResultReceiver;

/** @hide */
oneway interface IContentSuggestionsManager {
    void provideContextImage(
            int userId,
            int taskId,
            in Bundle imageContextRequestExtras);
    void provideContextBitmap(
            int userId,
            in Bitmap bitmap,
            in Bundle imageContextRequestExtras);
    void suggestContentSelections(
            int userId,
            in SelectionsRequest request,
            in ISelectionsCallback callback);
    void classifyContentSelections(
            int userId,
            in ClassificationsRequest request,
            in IClassificationsCallback callback);
    void notifyInteraction(int userId, in String requestId, in Bundle interaction);
    void isEnabled(int userId, in IResultReceiver receiver);
    void resetTemporaryService(int userId);
    void setTemporaryService(int userId, in String serviceName, int duration);
    void setDefaultServiceEnabled(int userId, boolean enabled);
}
