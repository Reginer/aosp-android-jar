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

package android.provider;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;

import com.android.providers.media.flags.Flags;

/**
 * <p> A callback interface passed to {@link MediaCognitionService} functions which will be
 * used to return results of the method
 * {@link MediaCognitionService#onGetProcessingVersions(MediaCognitionGetVersionsCallback)}. </p>
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_MEDIA_COGNITION_SERVICE)
public interface MediaCognitionGetVersionsCallback {

    /**
     * Call this on success response of {@link MediaCognitionService#onGetProcessingVersions}
     *
     * @param processingVersions Versions of different processing types.
     */
    void onSuccess(@NonNull MediaCognitionProcessingVersions processingVersions);

    /**
     * Call this on failure of the request from MediaCognitionService
     * @param message Failure message
     */
    void onFailure(@NonNull String message);
}
