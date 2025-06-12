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
import android.annotation.SystemApi;
import android.health.connect.HealthConnectException;
import android.health.connect.aidl.HealthConnectExceptionParcel;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Exception specifically encountered while staging HealthConnect data. Wraps errors encountered for
 * all files while staging all the files.
 *
 * @hide
 */
@SystemApi
public final class StageRemoteDataException extends RuntimeException implements Parcelable {
    private final Map<String, HealthConnectException> mExceptionsByFileNames = new ArrayMap<>();

    private StageRemoteDataException(Parcel in) {
        int size = in.readInt();
        for (int i = 0; i < size; ++i) {
            mExceptionsByFileNames.put(
                    in.readString(),
                    in.readParcelable(
                                    HealthConnectExceptionParcel.class.getClassLoader(),
                                    HealthConnectExceptionParcel.class)
                            .getHealthConnectException());
        }
    }

    /** @hide */
    public StageRemoteDataException(
            @NonNull Map<String, HealthConnectException> exceptionsByFileNames) {
        super("StageRemoteDataException");
        Objects.requireNonNull(exceptionsByFileNames);
        mExceptionsByFileNames.putAll(exceptionsByFileNames);
    }

    @NonNull
    public static final Creator<StageRemoteDataException> CREATOR =
            new Creator<>() {
                @Override
                public StageRemoteDataException createFromParcel(Parcel in) {
                    return new StageRemoteDataException(in);
                }

                @Override
                public StageRemoteDataException[] newArray(int size) {
                    return new StageRemoteDataException[size];
                }
            };

    /** Returns a {@link Map} which maps the file name with the error encountered for that file. */
    @NonNull
    public Map<String, HealthConnectException> getExceptionsByFileNames() {
        return Collections.unmodifiableMap(mExceptionsByFileNames);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mExceptionsByFileNames.size());
        for (var fileNamePfd : mExceptionsByFileNames.entrySet()) {
            dest.writeString(fileNamePfd.getKey());
            dest.writeParcelable(
                    new HealthConnectExceptionParcel(fileNamePfd.getValue()), /* parcelableFlags */
                    0);
        }
    }
}
