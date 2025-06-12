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
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.internal.ParcelUtils;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper to carry a list of entries of type {@link MedicalResource} from and to {@link
 * HealthConnectManager} and write the parcel to shared memory.
 *
 * @hide
 */
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public class MedicalResourceListParcel implements Parcelable {
    private final List<MedicalResource> mMedicalResources;

    public MedicalResourceListParcel(List<MedicalResource> medicalResources) {
        requireNonNull(medicalResources);
        mMedicalResources = medicalResources;
    }

    private MedicalResourceListParcel(Parcel in) {
        requireNonNull(in);
        in = ParcelUtils.getParcelForSharedMemoryIfRequired(in);
        mMedicalResources = new ArrayList<>();
        in.readParcelableList(
                mMedicalResources, MedicalResource.class.getClassLoader(), MedicalResource.class);
    }

    public static final Creator<MedicalResourceListParcel> CREATOR =
            new Creator<MedicalResourceListParcel>() {
                @Override
                public MedicalResourceListParcel createFromParcel(Parcel in) {
                    return new MedicalResourceListParcel(in);
                }

                @Override
                public MedicalResourceListParcel[] newArray(int size) {
                    return new MedicalResourceListParcel[size];
                }
            };

    /** Returns the list of {@link MedicalResource}s. */
    public List<MedicalResource> getMedicalResources() {
        return mMedicalResources;
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

    private void writeToParcelInternal(Parcel dest) {
        dest.writeParcelableList(mMedicalResources, 0);
    }
}
