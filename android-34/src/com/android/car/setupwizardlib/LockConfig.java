/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.setupwizardlib;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Encompasses the information about a lock type's configuration.
 */
public class LockConfig implements Parcelable {

    /**
     * Builder object for creating lock config from parcel.
     */
    public static final Parcelable.Creator<LockConfig> CREATOR =
            new Parcelable.Creator<LockConfig>() {

                @Override
                public LockConfig createFromParcel(Parcel source) {
                    return new LockConfig(source);
                }

                @Override
                public LockConfig[] newArray(int size) {
                    return new LockConfig[size];
                }
            };

    /**
     * Whether this lock type is enabled on the device.
     */
    public boolean enabled;
    /**
     * The minimum length for the lock, this is displayed to the user so the UI must know about it.
     */
    public int minLockLength;

    /**
     * Creates a {@link LockConfig} from a {@link Parcel}.
     */
    public LockConfig(Parcel in) {
        readFromParcel(in);
    }

    /**
     * Creates a {@link LockConfig} from the state passed in.
     */
    public LockConfig(boolean enabled, int minLockLength) {
        this.enabled = enabled;
        this.minLockLength = minLockLength;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(enabled ? 1 : 0);
        dest.writeInt(minLockLength);
    }

    private void readFromParcel(Parcel in) {
        enabled = in.readInt() != 0;
        minLockLength = in.readInt();
    }
}

