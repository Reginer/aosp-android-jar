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

package android.hardware.input;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Configurations to create virtual Dpad.
 *
 * @hide
 */
@SystemApi
public final class VirtualDpadConfig extends VirtualInputDeviceConfig implements Parcelable {
    @NonNull
    public static final Creator<VirtualDpadConfig> CREATOR = new Creator<VirtualDpadConfig>() {
        @Override
        public VirtualDpadConfig createFromParcel(Parcel in) {
            return new VirtualDpadConfig(in);
        }

        @Override
        public VirtualDpadConfig[] newArray(int size) {
            return new VirtualDpadConfig[size];
        }
    };

    private VirtualDpadConfig(@NonNull VirtualDpadConfig.Builder builder) {
        super(builder);
    }

    private VirtualDpadConfig(@NonNull Parcel in) {
        super(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    /**
     * Builder for creating a {@link VirtualDpadConfig}.
     */
    public static final class Builder extends VirtualInputDeviceConfig.Builder<Builder> {

        /**
         * Builds the {@link VirtualDpadConfig} instance.
         */
        @NonNull
        public VirtualDpadConfig build() {
            return new VirtualDpadConfig(this);
        }
    }
}
