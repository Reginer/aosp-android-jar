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
import android.health.connect.HealthDataCategory;
import android.health.connect.UpdateDataOriginPriorityOrderRequest;
import android.health.connect.datatypes.DataOrigin;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.stream.Collectors;

/** @hide */
public final class UpdatePriorityRequestParcel implements Parcelable {
    public static final Creator<UpdatePriorityRequestParcel> CREATOR =
            new Creator<>() {
                @Override
                public UpdatePriorityRequestParcel createFromParcel(Parcel in) {
                    return new UpdatePriorityRequestParcel(in);
                }

                @Override
                public UpdatePriorityRequestParcel[] newArray(int size) {
                    return new UpdatePriorityRequestParcel[size];
                }
            };
    private final List<String> mPackagePriorityOrder;
    @HealthDataCategory.Type private final int mDataCategory;

    private UpdatePriorityRequestParcel(Parcel in) {
        mPackagePriorityOrder = in.createStringArrayList();
        mDataCategory = in.readInt();
    }

    public UpdatePriorityRequestParcel(
            UpdateDataOriginPriorityOrderRequest updateDataOriginPriorityOrderRequest) {
        mPackagePriorityOrder =
                updateDataOriginPriorityOrderRequest.getDataOriginInOrder().stream()
                        .map(DataOrigin::getPackageName)
                        .collect(Collectors.toList());
        mDataCategory = updateDataOriginPriorityOrderRequest.getDataCategory();
    }

    @NonNull
    public List<String> getPackagePriorityOrder() {
        return mPackagePriorityOrder;
    }

    public int getDataCategory() {
        return mDataCategory;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStringList(mPackagePriorityOrder);
        dest.writeInt(mDataCategory);
    }
}
