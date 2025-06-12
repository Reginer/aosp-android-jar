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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.app.Service;
import android.content.Intent;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.provider.mediacognitionutils.GetProcessingVersionsCallbackImpl;
import android.provider.mediacognitionutils.ICognitionGetVersionsCallbackInternal;
import android.provider.mediacognitionutils.ICognitionProcessMediaCallbackInternal;
import android.provider.mediacognitionutils.IMediaCognitionService;
import android.provider.mediacognitionutils.ProcessMediaCallbackImpl;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.android.providers.media.flags.Flags;

import java.lang.annotation.Retention;
import java.util.List;


/**
 * <p> Base class for a service which can be implemented by privileged APKs.
 * This service gets request only from {@link com.android.providers.media.MediaProvider} to extract
 * data from media using various smart processing like image ocr, image labeling, etc. </p>
 *
 * <p>
 * <h3>Manifest entry</h3>
 * <p>MediaCognitionService must require the permission
 * "com.android.providers.media.permission.BIND_MEDIA_COGNITION_SERVICE". </p>
 *
 * <pre class="prettyprint">
 * {@literal
 * <service
 *     android:name=".MyMediaCognitionService"
 *     android:exported="true"
 *     android:permission="com.android.providers.media.permission.BIND_MEDIA_COGNITION_SERVICE">
 *     <intent-filter>
 *         <action android:name="android.provider.MediaCognitionService" />
 *         <category android:name="android.intent.category.DEFAULT"/>
 *     </intent-filter>
 * </service>}
 * </pre>
 * </p>
 *
 * Only one instance of MediaCognitionService will be in function at a time.
 * OEMs can specify the default MediaCognitionService through runtime resource overlay,
 * by setting value of the resource {@code config_default_media_cognition_service_package}.
 * The overlayable subset which has this resource is {@code MediaProviderConfig}
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_MEDIA_COGNITION_SERVICE)
public abstract class MediaCognitionService extends Service {

    /**
     * @hide
     */
    public static final String TAG = "MediaCognitionService";

    public static final String SERVICE_INTERFACE = "android.provider.MediaCognitionService";

    /**
     * Permission required to protect {@link MediaCognitionService} instances. Implementation should
     * require this in the {@code permission} attribute in their {@code <service>} tag.
     * The OS will not bind to a service without this protection.
     */
    public static final String BIND_MEDIA_COGNITION_SERVICE =
            "com.android.providers.media.permission.BIND_MEDIA_COGNITION_SERVICE";

    /**
     * This contains variables representing different processing types that
     * MediaProvider can request the service.
     */
    public interface ProcessingTypes {
        /**
         * Variable for representing processing of image ocr of latin script.
         */
        public static final int IMAGE_OCR_LATIN = 1 << 0;

        /**
         * Variable for representing processing of image labeling.
         */
        public static final int IMAGE_LABEL = 1 << 1;
    }

    /**
     * IntDef for bitmasks of ProcessingTypes
     * @hide
     */
    @IntDef(flag = true, value = {ProcessingTypes.IMAGE_OCR_LATIN,
            ProcessingTypes.IMAGE_LABEL})
    @Retention(SOURCE)
    public @interface ProcessingCombination {
    }

    /**
     * IntDef for the values in ProcessingTypes. No bitmask.
     * @hide
     */
    @IntDef(value = {ProcessingTypes.IMAGE_OCR_LATIN,
            ProcessingTypes.IMAGE_LABEL})
    @Retention(SOURCE)
    public @interface ProcessingType {
    }

    @Override
    @Nullable
    public final IBinder onBind(@Nullable Intent intent) {
        if (!SERVICE_INTERFACE.equals(intent.getAction())) {
            Log.w(TAG, "Wrong action");
            return null;
        }
        return mInterface.asBinder();
    }

    /**
     * <p> Implement this to process media for different cognition processing. Return the results
     * through the callback. For every {@link MediaCognitionProcessingRequest} prepare a
     * {@link MediaCognitionProcessingResponse} and return list of responses
     * through the callback function: {@link MediaCognitionProcessingCallback#onSuccess(List)}.
     * The responses must be returned all at once, for all the requests given.
     * If one of the request could not be processed, add an empty response for that specific request
     * in the returned responses.
     * </p>
     * <p> Each MediaCognitionProcessingRequest object contains a checker function
     * {@link MediaCognitionProcessingRequest#checkProcessingRequired(int)} in which you can pass any
     * processing type from the available ones here: {@link ProcessingTypes}.
     * If the request requires a processing include the result
     * in {@link MediaCognitionProcessingResponse}.
     * </p>
     *
     *
     *
     * <p> There is a time limit to respond to the callback.
     * Callback must be responded within less than 5 minutes.</p>
     *
     * @param requests           List of {@link MediaCognitionProcessingRequest}
     * @param cancellationSignal Signal to attach a listener to, and receive cancellation signals
     *                           from the client.
     * @param callback           Callback for giving the result
     */
    public abstract void onProcessMedia(@NonNull List<MediaCognitionProcessingRequest> requests,
            @Nullable CancellationSignal cancellationSignal,
            @NonNull MediaCognitionProcessingCallback callback);

    /**
     * <p>Implement this to return version of all processing types.
     * This will be used by MediaProvider to assess if reprocessing is required,
     * once the version changes. </p>
     *
     * <p>Set versions of each processing types available here: {@link ProcessingTypes}.
     * The result need to be composed using {@link MediaCognitionProcessingVersions}.
     * Return the result via the callback function:
     * {@link MediaCognitionGetVersionsCallback#onSuccess(MediaCognitionProcessingVersions)}
     * </p>
     *
     * <p> There is a time limit to respond to the callback.
     * Callback must be responded within less than 5 minutes.</p>
     *
     */
    public abstract void onGetProcessingVersions(
            @NonNull MediaCognitionGetVersionsCallback callback);


    private final IMediaCognitionService mInterface = new IMediaCognitionService.Stub() {
        @Override
        public void processMedia(List<MediaCognitionProcessingRequest> requests,
                ICognitionProcessMediaCallbackInternal binderCallback) {
            onProcessMedia(requests, null, new ProcessMediaCallbackImpl(binderCallback));
        }

        @Override
        public void getProcessingVersions(ICognitionGetVersionsCallbackInternal binderCallback) {
            onGetProcessingVersions(new GetProcessingVersionsCallbackImpl(binderCallback));
        }
    };

}
