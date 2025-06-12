/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.app.time;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * A pair containing a user's {@link TimeCapabilities} and {@link TimeConfiguration}.
 *
 * @hide
 */
@SystemApi
public final class TimeCapabilitiesAndConfig implements Parcelable {

    public static final @NonNull Creator<TimeCapabilitiesAndConfig> CREATOR =
            new Creator<TimeCapabilitiesAndConfig>() {
        @Override
        public TimeCapabilitiesAndConfig createFromParcel(Parcel source) {
            return TimeCapabilitiesAndConfig.readFromParcel(source);
        }

        @Override
        public TimeCapabilitiesAndConfig[] newArray(int size) {
            return new TimeCapabilitiesAndConfig[size];
        }
    };

    @NonNull private final TimeCapabilities mCapabilities;
    @NonNull private final TimeConfiguration mConfiguration;

    /**
     * Creates a new instance.
     *
     * @hide
     */
    public TimeCapabilitiesAndConfig(@NonNull TimeCapabilities timeCapabilities,
            @NonNull TimeConfiguration timeConfiguration) {
        mCapabilities = Objects.requireNonNull(timeCapabilities);
        mConfiguration = Objects.requireNonNull(timeConfiguration);
    }

    @NonNull
    private static TimeCapabilitiesAndConfig readFromParcel(Parcel in) {
        TimeCapabilities capabilities = in.readParcelable(null, android.app.time.TimeCapabilities.class);
        TimeConfiguration configuration = in.readParcelable(null, android.app.time.TimeConfiguration.class);
        return new TimeCapabilitiesAndConfig(capabilities, configuration);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mCapabilities, flags);
        dest.writeParcelable(mConfiguration, flags);
    }

    /**
     * Returns the user's time behaviour capabilities.
     */
    @NonNull
    public TimeCapabilities getCapabilities() {
        return mCapabilities;
    }

    /**
     * Returns the user's time behaviour configuration.
     */
    @NonNull
    public TimeConfiguration getConfiguration() {
        return mConfiguration;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeCapabilitiesAndConfig that = (TimeCapabilitiesAndConfig) o;
        return mCapabilities.equals(that.mCapabilities)
                && mConfiguration.equals(that.mConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mCapabilities, mConfiguration);
    }

    @Override
    public String toString() {
        return "TimeCapabilitiesAndConfig{"
                + "mCapabilities=" + mCapabilities
                + ", mConfiguration=" + mConfiguration
                + '}';
    }
}
