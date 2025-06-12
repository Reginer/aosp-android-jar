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
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteCallback;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.providers.media.flags.Flags;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * <p> Base class for a service which can be implemented by privileged APKs.
 * This service gets request only from {@link com.android.providers.media.MediaProvider} to extract
 * metadata from files. </p>
 *
 * <p>
 * <h3>Manifest entry</h3>
 * <p>OemMetadataService must require the permission
 * "com.android.providers.media.permission.BIND_OEM_METADATA_SERVICE". Service will be ignored for
 * binding if permission is missing. </p>
 *
 * <pre class="prettyprint">
 * {@literal
 * <service
 *     android:name=".MyOemMetadataService"
 *     android:exported="true"
 *     android:permission="com.android.providers.media.permission.BIND_OEM_METADATA_SERVICE">
 *     <intent-filter>
 *         <action android:name="android.provider.OemMetadataService" />
 *         <category android:name="android.intent.category.DEFAULT"/>
 *     </intent-filter>
 * </service>}
 * </pre>
 * </p>
 *
 * Only one instance of OemMetadataService will be in function at a time.
 * OEMs can specify the default behavior through runtime resource overlay,
 * by setting value of the resource {@code config_default_media_oem_metadata_service_package}.
 * The overlayable subset which has this resource is {@code MediaProviderConfig}
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_ENABLE_OEM_METADATA)
public abstract class OemMetadataService extends Service {
    /**
     * @hide
     */
    private static final String TAG = "OemMetadataService";

    public static final String SERVICE_INTERFACE = "android.provider.OemMetadataService";

    /**
     * @hide
     */
    public static final String EXTRA_OEM_SUPPORTED_MIME_TYPES =
            "android.provider.extra.OEM_SUPPORTED_MIME_TYPES";

    /**
     * @hide
     */
    public static final String EXTRA_OEM_DATA_KEYS = "android.provider.extra.OEM_DATA_KEYS";

    /**
     * @hide
     */
    public static final String EXTRA_OEM_DATA_VALUES = "android.provider.extra.OEM_DATA_VALUES";

    /**
     * Permission required to protect {@link OemMetadataService} instances. Implementation should
     * require this in the {@code permission} attribute in their {@code <service>} tag.
     */
    public static final String BIND_OEM_METADATA_SERVICE_PERMISSION =
            "com.android.providers.media.permission.BIND_OEM_METADATA_SERVICE";

    @Override
    @NonNull
    public final IBinder onBind(@Nullable Intent intent) {
        if (!SERVICE_INTERFACE.equals(intent.getAction())) {
            Log.w(TAG, "Unexpected action:" + intent.getAction());
            return null;
        }

        return mInterface.asBinder();
    }

    /**
     * Returns set of {@link MediaStore.Files.FileColumns#MIME_TYPE} for which OEMs wants to store
     * custom metadata. OEM metadata will be requested for a file only if it has one of the
     * supported mime types. Supported mime type can be any mime type and need not be a media mime
     * type. Returns an empty set if no mime types are supported.
     *
     * @return set of {@link MediaStore.Files.FileColumns#MIME_TYPE}
     */
    @NonNull
    public abstract Set<String> onGetSupportedMimeTypes();

    /**
     * Returns a key-value {@link Map} of {@link String} which OEMs wants to store as custom
     * metadata for a file. Returns an empty map if no custom data is present for the file.
     *
     * @param fd file descriptor of the file in lower file system
     * @return map of key-value pairs of string
     */
    @NonNull
    public abstract Map<String, String> onGetOemCustomData(@NonNull ParcelFileDescriptor fd);


    private final IOemMetadataService mInterface = new IOemMetadataService.Stub() {
        @Override
        public void getSupportedMimeTypes(RemoteCallback callback) {
            Set<String> supportedMimeTypes = onGetSupportedMimeTypes();
            sendResultForSupportedMimeTypes(supportedMimeTypes, callback);
        }

        @Override
        public void getOemCustomData(ParcelFileDescriptor pfd, RemoteCallback callback) {
            Map<String, String> oemCustomData = onGetOemCustomData(pfd);
            sendResultForOemCustomData(oemCustomData, callback);
        }

        private void sendResultForOemCustomData(Map<String, String> oemCustomData,
                RemoteCallback callback) {
            Bundle bundle = new Bundle();
            if (oemCustomData != null && !oemCustomData.isEmpty()) {
                ArrayList<String> keyList = new ArrayList<String>(oemCustomData.size());
                ArrayList<String> valueList = new ArrayList<String>(oemCustomData.size());
                for (String key : oemCustomData.keySet()) {
                    keyList.add(key);
                    valueList.add(oemCustomData.get(key));
                }
                bundle.putStringArrayList(EXTRA_OEM_DATA_KEYS, keyList);
                bundle.putStringArrayList(EXTRA_OEM_DATA_VALUES, valueList);
            }

            callback.sendResult(bundle);
        }

        private void sendResultForSupportedMimeTypes(Set<String> supportedMimeTypes,
                RemoteCallback callback) {
            Bundle bundle = new Bundle();
            if (supportedMimeTypes != null && !supportedMimeTypes.isEmpty()) {
                bundle.putStringArrayList(EXTRA_OEM_SUPPORTED_MIME_TYPES,
                        new ArrayList<String>(supportedMimeTypes));
            }
            callback.sendResult(bundle);
        }
    };
}
