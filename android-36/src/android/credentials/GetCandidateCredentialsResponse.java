/*
 * Copyright 2022 The Android Open Source Project
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

package android.credentials;

import android.annotation.Hide;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Intent;
import android.credentials.selection.GetCredentialProviderData;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.AnnotationValidations;
import com.android.internal.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of candidate credentials.
 *
 * @hide
 */
@Hide
public final class GetCandidateCredentialsResponse implements Parcelable {
    @NonNull
    private final List<GetCredentialProviderData> mCandidateProviderDataList;

    @Nullable
    private final ComponentName mPrimaryProviderComponentName;

    @NonNull
    private final Intent mIntent;

    /**
     * @hide
     */
    @Hide
    public GetCandidateCredentialsResponse(
            @NonNull List<GetCredentialProviderData> candidateProviderDataList,
            @NonNull Intent intent,
            @Nullable ComponentName primaryProviderComponentName
    ) {
        Preconditions.checkCollectionNotEmpty(
                candidateProviderDataList,
                /*valueName=*/ "candidateProviderDataList");
        mCandidateProviderDataList = new ArrayList<>(candidateProviderDataList);
        mIntent = intent;
        mPrimaryProviderComponentName = primaryProviderComponentName;
    }

    /**
     * Returns candidate provider data list.
     *
     * @hide
     */
    public List<GetCredentialProviderData> getCandidateProviderDataList() {
        return mCandidateProviderDataList;
    }

    /**
     * Returns the primary provider component name.
     *
     * @hide
     */
    @Nullable
    public ComponentName getPrimaryProviderComponentName() {
        return mPrimaryProviderComponentName;
    }

    /**
     * Returns candidate provider data list.
     *
     * @hide
     */
    @NonNull
    public Intent getIntent() {
        return mIntent;
    }

    protected GetCandidateCredentialsResponse(Parcel in) {
        List<GetCredentialProviderData> candidateProviderDataList = new ArrayList<>();
        in.readTypedList(candidateProviderDataList, GetCredentialProviderData.CREATOR);
        mCandidateProviderDataList = candidateProviderDataList;

        AnnotationValidations.validate(NonNull.class, null, mCandidateProviderDataList);
        mIntent = in.readTypedObject(Intent.CREATOR);

        mPrimaryProviderComponentName = in.readTypedObject(ComponentName.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(mCandidateProviderDataList);
        dest.writeTypedObject(mIntent, flags);
        dest.writeTypedObject(mPrimaryProviderComponentName, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GetCandidateCredentialsResponse> CREATOR =
            new Creator<>() {
                @Override
                public GetCandidateCredentialsResponse createFromParcel(Parcel in) {
                    return new GetCandidateCredentialsResponse(in);
                }

                @Override
                public GetCandidateCredentialsResponse[] newArray(int size) {
                    return new GetCandidateCredentialsResponse[size];
                }
            };
}
