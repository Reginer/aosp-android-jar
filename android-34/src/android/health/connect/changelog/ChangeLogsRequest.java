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

import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;
import static android.health.connect.Constants.MAXIMUM_PAGE_SIZE;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/** Request class for {@link HealthConnectManager#getChangeLogs} */
public final class ChangeLogsRequest implements Parcelable {
    private final String mToken;
    private final int mPageSize;

    /**
     * @see Builder
     */
    private ChangeLogsRequest(@NonNull String token, int pageSize) {
        Objects.requireNonNull(token);

        mToken = token;
        mPageSize = pageSize;
    }

    private ChangeLogsRequest(Parcel in) {
        mToken = in.readString();
        mPageSize = in.readInt();
    }

    @NonNull
    public static final Creator<ChangeLogsRequest> CREATOR =
            new Creator<ChangeLogsRequest>() {
                @Override
                public ChangeLogsRequest createFromParcel(Parcel in) {
                    return new ChangeLogsRequest(in);
                }

                @Override
                public ChangeLogsRequest[] newArray(int size) {
                    return new ChangeLogsRequest[size];
                }
            };

    /** Returns the token for the change logs request */
    @NonNull
    public String getToken() {
        return mToken;
    }

    /**
     * Returns the maximum number of records requested using {@link
     * HealthConnectManager#getChangeLogs} operation
     */
    @IntRange(from = 1, to = 5000)
    public int getPageSize() {
        return mPageSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mToken);
        dest.writeInt(mPageSize);
    }

    /** Builder class for {@link ChangeLogsRequest} */
    public static final class Builder {
        private String mToken;
        private int mPageSize = DEFAULT_PAGE_SIZE;

        public Builder(@NonNull String token) {
            Objects.requireNonNull(token);

            mToken = token;
        }

        /**
         * @param pageSize number of change logs to be returned, this corresponds to the maximum
         *     number of entries to be returned i.e. sum of the response within {@link
         *     ChangeLogsResponse}
         *     <p>If not set default is 1000 and maximum value is 5000.
         * @throws IllegalArgumentException if requested pageSize > 5000
         */
        @NonNull
        public Builder setPageSize(@IntRange(from = 1, to = 5000) int pageSize) {
            if (pageSize > MAXIMUM_PAGE_SIZE) {
                throw new IllegalArgumentException(
                        "Maximum page size " + MAXIMUM_PAGE_SIZE + " requested " + pageSize);
            }
            mPageSize = pageSize;
            return this;
        }

        /** Returns Object of {@link ChangeLogsRequest} */
        @NonNull
        public ChangeLogsRequest build() {
            return new ChangeLogsRequest(mToken, mPageSize);
        }
    }
}
