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

package android.adservices.appsetid;

import static android.adservices.common.AdServicesStatusUtils.STATUS_SUCCESS;

import android.adservices.AdServicesFrameworkHelper;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SdkConstant;
import android.annotation.SystemApi;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import java.io.IOException;

/**
 * Abstract Base class for provider service to implement generation of AppSetId with appropriate
 * appSetId scope value.
 *
 * <p>The implementor of this service needs to override the onGetAppSetIdProvider method and provide
 * an app-scoped or developer-account scoped unique appSetId.
 *
 * @hide
 */
@SystemApi
public abstract class AppSetIdProviderService extends Service {

    /** The intent that the service must respond to. Add it to the intent filter of the service. */
    @SdkConstant(SdkConstant.SdkConstantType.SERVICE_ACTION)
    public static final String SERVICE_INTERFACE =
            "android.adservices.appsetid.AppSetIdProviderService";

    /** Abstract method which will be overridden by provider to provide the appsetid. */
    @NonNull
    public abstract AppSetId onGetAppSetId(int clientUid, @NonNull String clientPackageName)
            throws IOException;

    private final android.adservices.appsetid.IAppSetIdProviderService mInterface =
            new android.adservices.appsetid.IAppSetIdProviderService.Stub() {
                @Override
                public void getAppSetId(
                        int appUID,
                        @NonNull String packageName,
                        @NonNull IGetAppSetIdProviderCallback resultCallback)
                        throws RemoteException {
                    try {
                        AppSetId appsetId = onGetAppSetId(appUID, packageName);
                        GetAppSetIdResult appsetIdInternal =
                                new GetAppSetIdResult.Builder()
                                        .setStatusCode(STATUS_SUCCESS)
                                        .setErrorMessage("")
                                        .setAppSetId(appsetId.getId())
                                        .setAppSetIdScope(appsetId.getScope())
                                        .build();

                        resultCallback.onResult(appsetIdInternal);
                    } catch (Throwable e) {
                        resultCallback.onError(
                                AdServicesFrameworkHelper.getExceptionStackTraceString(e));
                    }
                }
            };

    @Nullable
    @Override
    public final IBinder onBind(@Nullable Intent intent) {
        return mInterface.asBinder();
    }
}
