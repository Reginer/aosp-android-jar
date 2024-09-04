/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.safetycenter;

import static android.os.Build.VERSION_CODES.TIRAMISU;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import java.util.Objects;

/**
 * Details of an error that a Safety Source may report to the Safety Center.
 *
 * @hide
 */
@SystemApi
@RequiresApi(TIRAMISU)
public final class SafetySourceErrorDetails implements Parcelable {

    @NonNull
    public static final Creator<SafetySourceErrorDetails> CREATOR =
            new Creator<SafetySourceErrorDetails>() {
                @Override
                public SafetySourceErrorDetails createFromParcel(Parcel in) {
                    SafetyEvent safetyEvent = in.readTypedObject(SafetyEvent.CREATOR);
                    return new SafetySourceErrorDetails(safetyEvent);
                }

                @Override
                public SafetySourceErrorDetails[] newArray(int size) {
                    return new SafetySourceErrorDetails[0];
                }
            };

    @NonNull private final SafetyEvent mSafetyEvent;

    public SafetySourceErrorDetails(@NonNull SafetyEvent safetyEvent) {
        mSafetyEvent = requireNonNull(safetyEvent);
    }

    /** Returns the safety event associated with this error. */
    @NonNull
    public SafetyEvent getSafetyEvent() {
        return mSafetyEvent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetySourceErrorDetails)) return false;
        SafetySourceErrorDetails that = (SafetySourceErrorDetails) o;
        return mSafetyEvent.equals(that.mSafetyEvent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSafetyEvent);
    }

    @Override
    public String toString() {
        return "SafetySourceErrorDetails{" + "mSafetyEvent=" + mSafetyEvent + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeTypedObject(mSafetyEvent, flags);
    }
}
