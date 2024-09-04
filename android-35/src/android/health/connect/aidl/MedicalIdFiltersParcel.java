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

package android.health.connect.aidl;

import android.annotation.NonNull;
import android.health.connect.MedicalIdFilter;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Parcelable} that reads and writes {@link MedicalIdFilter}s.
 *
 * @hide
 */
public class MedicalIdFiltersParcel implements Parcelable {
    public static final Creator<MedicalIdFiltersParcel> CREATOR =
            new Creator<>() {
                @Override
                public MedicalIdFiltersParcel createFromParcel(Parcel in) {
                    return new MedicalIdFiltersParcel(in);
                }

                @Override
                public MedicalIdFiltersParcel[] newArray(int size) {
                    return new MedicalIdFiltersParcel[size];
                }
            };
    private List<MedicalIdFilter> mMedicalIdFilters = new ArrayList<>();

    public MedicalIdFiltersParcel(List<MedicalIdFilter> medicalIdFilters) {
        mMedicalIdFilters = medicalIdFilters;
    }

    @SuppressWarnings("FlaggedApi") // this class is internal only
    private MedicalIdFiltersParcel(Parcel in) {
        int size = in.readInt();
        mMedicalIdFilters = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = in.readString();
            if (id != null) {
                mMedicalIdFilters.add(MedicalIdFilter.fromId(id));
            }
        }
    }

    public List<MedicalIdFilter> getMedicalIdFilters() {
        return mMedicalIdFilters;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    @SuppressWarnings("FlaggedApi") // this class is internal only
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mMedicalIdFilters.size());
        mMedicalIdFilters.forEach(medicalIdFilter -> dest.writeString(medicalIdFilter.getId()));
    }
}
