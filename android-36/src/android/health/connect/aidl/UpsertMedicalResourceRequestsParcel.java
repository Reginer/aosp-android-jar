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

package android.health.connect.aidl;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;

import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.health.connect.UpsertMedicalResourceRequest;
import android.health.connect.internal.ParcelUtils;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper to carry a list of entries of type {@link UpsertMedicalResourceRequest} from and to
 * {@link HealthConnectManager} and write the parcel to shared memory.
 *
 * @hide
 */
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public class UpsertMedicalResourceRequestsParcel implements Parcelable {
    private final List<UpsertMedicalResourceRequest> mUpsertRequests;

    public UpsertMedicalResourceRequestsParcel(List<UpsertMedicalResourceRequest> upsertRequests) {
        requireNonNull(upsertRequests);
        mUpsertRequests = upsertRequests;
    }

    private UpsertMedicalResourceRequestsParcel(Parcel in) {
        requireNonNull(in);
        in = ParcelUtils.getParcelForSharedMemoryIfRequired(in);

        mUpsertRequests = new ArrayList<>();
        in.readParcelableList(
                mUpsertRequests,
                UpsertMedicalResourceRequest.class.getClassLoader(),
                UpsertMedicalResourceRequest.class);
    }

    public static final Creator<UpsertMedicalResourceRequestsParcel> CREATOR =
            new Creator<>() {
                @Override
                public UpsertMedicalResourceRequestsParcel createFromParcel(Parcel in) {
                    return new UpsertMedicalResourceRequestsParcel(in);
                }

                @Override
                public UpsertMedicalResourceRequestsParcel[] newArray(int size) {
                    return new UpsertMedicalResourceRequestsParcel[size];
                }
            };

    /** Returns a list of {@link UpsertMedicalResourceRequest}s. */
    public List<UpsertMedicalResourceRequest> getUpsertRequests() {
        return mUpsertRequests;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        requireNonNull(dest);
        ParcelUtils.putToRequiredMemory(dest, flags, this::writeToParcelInternal);
    }

    private void writeToParcelInternal(@NonNull Parcel dest) {
        dest.writeParcelableList(mUpsertRequests, 0);
    }
}
