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
import android.health.connect.FetchDataOriginsPriorityOrderResponse;
import android.health.connect.datatypes.DataOrigin;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.stream.Collectors;

/** @hide */
public final class GetPriorityResponseParcel implements Parcelable {
    public static final Creator<GetPriorityResponseParcel> CREATOR =
            new Creator<GetPriorityResponseParcel>() {
                @Override
                public GetPriorityResponseParcel createFromParcel(Parcel in) {
                    return new GetPriorityResponseParcel(in);
                }

                @Override
                public GetPriorityResponseParcel[] newArray(int size) {
                    return new GetPriorityResponseParcel[size];
                }
            };
    private final List<String> mPackagesInPriorityOrder;

    private GetPriorityResponseParcel(Parcel in) {
        mPackagesInPriorityOrder = in.createStringArrayList();
    }

    public GetPriorityResponseParcel(
            @NonNull FetchDataOriginsPriorityOrderResponse fetchDataOriginsPriorityOrderResponse) {
        mPackagesInPriorityOrder =
                fetchDataOriginsPriorityOrderResponse.getDataOriginsPriorityOrder().stream()
                        .map(DataOrigin::getPackageName)
                        .collect(Collectors.toList());
    }

    public FetchDataOriginsPriorityOrderResponse getPriorityResponse() {
        return new FetchDataOriginsPriorityOrderResponse(
                mPackagesInPriorityOrder.stream()
                        .map(
                                packageName ->
                                        new DataOrigin.Builder()
                                                .setPackageName(packageName)
                                                .build())
                        .collect(Collectors.toList()));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStringList(mPackagesInPriorityOrder);
    }
}
