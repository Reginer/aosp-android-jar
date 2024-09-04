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

package android.health.connect.restore;

import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.util.ArrayMap;

import java.util.Map;
import java.util.Objects;

/**
 * Parcelable for passing the request for the {@link
 * HealthConnectManager#stageAllHealthConnectRemoteData} call.
 *
 * @hide
 */
public final class StageRemoteDataRequest implements Parcelable {
    private final Map<String, ParcelFileDescriptor> mPfdsByFileName;

    private StageRemoteDataRequest(Parcel in) {
        int size = in.readInt();
        mPfdsByFileName = new ArrayMap<>(size);
        for (int i = 0; i < size; ++i) {
            mPfdsByFileName.put(
                    in.readString(),
                    in.readParcelable(
                            ParcelFileDescriptor.class.getClassLoader(),
                            ParcelFileDescriptor.class));
        }
    }

    public StageRemoteDataRequest(@NonNull Map<String, ParcelFileDescriptor> pfdsByFileName) {
        Objects.requireNonNull(pfdsByFileName);
        mPfdsByFileName = pfdsByFileName;
    }

    @NonNull
    public static final Creator<StageRemoteDataRequest> CREATOR =
            new Creator<>() {
                @Override
                public StageRemoteDataRequest createFromParcel(Parcel in) {
                    return new StageRemoteDataRequest(in);
                }

                @Override
                public StageRemoteDataRequest[] newArray(int size) {
                    return new StageRemoteDataRequest[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mPfdsByFileName.size());
        for (var fileNamePfd : mPfdsByFileName.entrySet()) {
            dest.writeString(fileNamePfd.getKey());
            dest.writeParcelable(fileNamePfd.getValue(), /* parcelableFlags */ 0);
        }
    }

    @NonNull
    public Map<String, ParcelFileDescriptor> getPfdsByFileName() {
        return mPfdsByFileName;
    }
}
