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

package android.credentials;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.util.AnnotationValidations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * A request to unregister a {@link ComponentName} that contains an actively provisioned
 * {@link Credential} represented by a {@link CredentialDescription}. *
 *
 */
public final class UnregisterCredentialDescriptionRequest implements Parcelable {

    @NonNull
    private final List<CredentialDescription> mCredentialDescriptions;

    public UnregisterCredentialDescriptionRequest(
            @NonNull CredentialDescription credentialDescription) {
        mCredentialDescriptions = Arrays.asList(requireNonNull(credentialDescription));
    }

    public UnregisterCredentialDescriptionRequest(
            @NonNull Set<CredentialDescription> credentialDescriptions) {
        mCredentialDescriptions = new ArrayList<>(requireNonNull(credentialDescriptions));
    }

    private UnregisterCredentialDescriptionRequest(@NonNull Parcel in) {
        List<CredentialDescription> credentialDescriptions = new ArrayList<>();
        in.readTypedList(credentialDescriptions, CredentialDescription.CREATOR);

        mCredentialDescriptions = new ArrayList<>();
        AnnotationValidations.validate(android.annotation.NonNull.class, null,
                credentialDescriptions);
        mCredentialDescriptions.addAll(credentialDescriptions);
    }

    public static final @NonNull Parcelable.Creator<UnregisterCredentialDescriptionRequest>
            CREATOR = new Parcelable.Creator<UnregisterCredentialDescriptionRequest>() {
                @Override
                public UnregisterCredentialDescriptionRequest createFromParcel(Parcel in) {
                    return new UnregisterCredentialDescriptionRequest(in);
                }

                @Override
                public UnregisterCredentialDescriptionRequest[] newArray(int size) {
                    return new UnregisterCredentialDescriptionRequest[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeTypedList(mCredentialDescriptions, flags);
    }

    @NonNull
    public Set<CredentialDescription> getCredentialDescriptions() {
        return new HashSet<>(mCredentialDescriptions);
    }
}
