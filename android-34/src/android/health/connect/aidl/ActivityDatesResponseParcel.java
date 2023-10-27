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
import android.os.Parcel;
import android.os.Parcelable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** @hide */
public class ActivityDatesResponseParcel implements Parcelable {
    private final List<LocalDate> mLocalDates;

    public ActivityDatesResponseParcel(@NonNull List<LocalDate> localDates) {
        Objects.requireNonNull(localDates);
        mLocalDates = localDates;
    }

    public static final Creator<ActivityDatesResponseParcel> CREATOR =
            new Creator<ActivityDatesResponseParcel>() {
                @Override
                public ActivityDatesResponseParcel createFromParcel(Parcel in) {
                    return new ActivityDatesResponseParcel(in);
                }

                @Override
                public ActivityDatesResponseParcel[] newArray(int size) {
                    return new ActivityDatesResponseParcel[size];
                }
            };

    protected ActivityDatesResponseParcel(Parcel in) {
        int size = in.readInt();

        mLocalDates = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String date = in.readString();
            mLocalDates.add(LocalDate.parse(date));
        }
    }

    @NonNull
    public List<LocalDate> getDates() {
        return mLocalDates;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written. May be 0 or {@link
     *     #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mLocalDates.size());
        mLocalDates.forEach(
                (localDate -> {
                    dest.writeString(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
                }));
    }
}
