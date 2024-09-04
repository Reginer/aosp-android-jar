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

package android.health.connect.changelog;

import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Response class for {@link HealthConnectManager#getChangeLogToken}}
 *
 * @see HealthConnectManager#getChangeLogToken
 */
public final class ChangeLogTokenResponse implements Parcelable {
    private final String mToken;

    /**
     * Response for {@link HealthConnectManager#getChangeLogToken}
     *
     * @hide
     */
    public ChangeLogTokenResponse(@NonNull String token) {
        Objects.requireNonNull(token);

        mToken = token;
    }

    private ChangeLogTokenResponse(Parcel in) {
        mToken = in.readString();
    }

    @NonNull
    public static final Parcelable.Creator<ChangeLogTokenResponse> CREATOR =
            new Parcelable.Creator<>() {
                @Override
                public ChangeLogTokenResponse createFromParcel(Parcel in) {
                    return new ChangeLogTokenResponse(in);
                }

                @Override
                public ChangeLogTokenResponse[] newArray(int size) {
                    return new ChangeLogTokenResponse[size];
                }
            };

    /** Returns the token for the change logs request */
    @NonNull
    public String getToken() {
        return mToken;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mToken);
    }
}
