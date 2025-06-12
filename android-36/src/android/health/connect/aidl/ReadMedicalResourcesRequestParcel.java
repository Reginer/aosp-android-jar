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

import static android.health.connect.Constants.MAXIMUM_PAGE_SIZE;
import static android.health.connect.Constants.MINIMUM_PAGE_SIZE;
import static android.health.connect.datatypes.MedicalDataSource.validateMedicalDataSourceIds;
import static android.health.connect.datatypes.MedicalResource.validateMedicalResourceType;
import static android.health.connect.datatypes.validation.ValidationUtils.requireInRange;

import static java.util.Objects.requireNonNull;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.HealthConnectManager;
import android.health.connect.ReadMedicalResourcesInitialRequest;
import android.health.connect.ReadMedicalResourcesPageRequest;
import android.health.connect.datatypes.MedicalResource;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * A Parcel to carry read request to {@link HealthConnectManager#readMedicalResources}.
 *
 * <p>If {@code pageToken} in the request parcel is {@code null}, it means it's the initial request.
 *
 * @hide
 */
public class ReadMedicalResourcesRequestParcel implements Parcelable {
    private final boolean mIsPageRequest;
    @MedicalResource.MedicalResourceType private int mMedicalResourceType;
    @NonNull private Set<String> mDataSourceIds = Set.of();
    @Nullable private String mPageToken = null;
    private final int mPageSize;

    public ReadMedicalResourcesRequestParcel(ReadMedicalResourcesInitialRequest request) {
        mIsPageRequest = false;
        mMedicalResourceType = request.getMedicalResourceType();
        mDataSourceIds = request.getDataSourceIds();
        mPageSize = request.getPageSize();
    }

    public ReadMedicalResourcesRequestParcel(ReadMedicalResourcesPageRequest request) {
        mIsPageRequest = true;
        mPageToken = request.getPageToken();
        mPageSize = request.getPageSize();
    }

    private ReadMedicalResourcesRequestParcel(Parcel in) {
        mIsPageRequest = in.readBoolean();
        mMedicalResourceType = in.readInt();
        mDataSourceIds = new HashSet<>(requireNonNull(in.createStringArrayList()));
        validateMedicalDataSourceIds(mDataSourceIds);
        mPageToken = in.readString();
        mPageSize = in.readInt();
        requireInRange(mPageSize, MINIMUM_PAGE_SIZE, MAXIMUM_PAGE_SIZE, "pageSize");

        if (mIsPageRequest && mPageToken == null) {
            throw new IllegalArgumentException(
                    "pageToken cannot be null when reading Parcel from page request.");
        } else if (!mIsPageRequest) {
            validateMedicalResourceType(mMedicalResourceType);
        }
    }

    public static final Creator<ReadMedicalResourcesRequestParcel> CREATOR =
            new Creator<>() {
                @Override
                public ReadMedicalResourcesRequestParcel createFromParcel(Parcel in) {
                    return new ReadMedicalResourcesRequestParcel(in);
                }

                @Override
                public ReadMedicalResourcesRequestParcel[] newArray(int size) {
                    return new ReadMedicalResourcesRequestParcel[size];
                }
            };

    @MedicalResource.MedicalResourceType
    public int getMedicalResourceType() {
        return mMedicalResourceType;
    }

    @NonNull
    public Set<String> getDataSourceIds() {
        return new ArraySet<>(mDataSourceIds);
    }

    @Nullable
    public String getPageToken() {
        return mPageToken;
    }

    @IntRange(from = MINIMUM_PAGE_SIZE, to = MAXIMUM_PAGE_SIZE)
    public int getPageSize() {
        return mPageSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeBoolean(mIsPageRequest);
        dest.writeInt(mMedicalResourceType);
        dest.writeStringList(new ArrayList<>(mDataSourceIds));
        dest.writeString(mPageToken);
        dest.writeInt(mPageSize);
    }
}
