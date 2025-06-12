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

package android.adservices.extdata;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SdkConstant;
import android.annotation.SystemApi;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.adservices.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Abstract base class to implement AdServicesExtDataStorageService.
 *
 * <p>The implementor of this service needs to override the onGetAdServicesExtData and
 * onPutAdServicesExtData methods
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_ADEXT_DATA_SERVICE_APIS_ENABLED)
public abstract class AdServicesExtDataStorageService extends Service {
    /**
     * Supported data field IDs.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = "FIELD_",
            value = {
                FIELD_IS_NOTIFICATION_DISPLAYED,
                FIELD_IS_MEASUREMENT_CONSENTED,
                FIELD_IS_U18_ACCOUNT,
                FIELD_IS_ADULT_ACCOUNT,
                FIELD_MANUAL_INTERACTION_WITH_CONSENT_STATUS,
                FIELD_MEASUREMENT_ROLLBACK_APEX_VERSION,
            })
    public @interface AdServicesExtDataFieldId {}

    /** Field to represent whether AdServices consent notification has been shown on Android R. */
    public static final int FIELD_IS_NOTIFICATION_DISPLAYED = 0;

    /** Field to represent whether user provided consent for Measurement API. */
    public static final int FIELD_IS_MEASUREMENT_CONSENTED = 1;

    /** Field to represent whether account is U18. */
    public static final int FIELD_IS_U18_ACCOUNT = 2;

    /** Field to represent whether it's an adult account. */
    public static final int FIELD_IS_ADULT_ACCOUNT = 3;

    /** Field to represent whether user manually interacted with consent */
    public static final int FIELD_MANUAL_INTERACTION_WITH_CONSENT_STATUS = 4;

    /** Field to represent ExtServices apex version for measurement rollback handling. */
    public static final int FIELD_MEASUREMENT_ROLLBACK_APEX_VERSION = 5;

    /** The intent that the service must respond to. Add it to the intent filter of the service. */
    @SdkConstant(SdkConstant.SdkConstantType.SERVICE_ACTION)
    public static final String SERVICE_INTERFACE =
            "android.adservices.extdata.AdServicesExtDataStorageService";

    public AdServicesExtDataStorageService() {}

    @Nullable
    @Override
    public final IBinder onBind(@Nullable Intent intent) {
        return mInterface.asBinder();
    }

    /** Abstract onGetAdServicesExtData method to get all stored ext data values from data store. */
    @NonNull
    public abstract AdServicesExtDataParams onGetAdServicesExtData();

    /**
     * Abstract onPutAdServicesExtData method to update values of fields in data store.
     *
     * @param adServicesExtDataParams data object that stores fields to be updated.
     * @param adServicesExtDataFields explicit list of fields that need to be updated in data store.
     */
    public abstract void onPutAdServicesExtData(
            @NonNull AdServicesExtDataParams adServicesExtDataParams,
            @NonNull @AdServicesExtDataFieldId int[] adServicesExtDataFields);

    private final IAdServicesExtDataStorageService mInterface =
            new IAdServicesExtDataStorageService.Stub() {

                @Override
                public void getAdServicesExtData(@NonNull IGetAdServicesExtDataCallback callback)
                        throws RemoteException {
                    Objects.requireNonNull(callback);
                    callback.onError("AdServicesExtDataStorageService is not implemented.");
                }

                @Override
                public void putAdServicesExtData(
                        @NonNull AdServicesExtDataParams params,
                        @NonNull @AdServicesExtDataFieldId int[] adServicesExtDataFields,
                        @NonNull IGetAdServicesExtDataCallback callback)
                        throws RemoteException {
                    Objects.requireNonNull(params);
                    Objects.requireNonNull(adServicesExtDataFields);
                    Objects.requireNonNull(callback);

                    callback.onError("AdServicesExtDataStorageService is not implemented.");
                }
            };
}
