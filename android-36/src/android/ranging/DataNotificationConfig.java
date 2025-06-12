/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.ranging;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.ranging.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents the configuration for data notifications in ranging operations.
 *
 * <p>This class holds the configuration settings for how notifications are sent
 * regarding the proximity of ranging devices.
 */
@FlaggedApi(Flags.FLAG_RANGING_STACK_ENABLED)
public final class DataNotificationConfig implements Parcelable {

    private final @NotificationConfigType int mNotificationConfigType;
    private final int mProximityNearCm;
    private final int mProximityFarCm;

    private DataNotificationConfig(Parcel in) {
        mNotificationConfigType = in.readInt();
        mProximityNearCm = in.readInt();
        mProximityFarCm = in.readInt();
    }

    @NonNull
    public static final Creator<DataNotificationConfig> CREATOR = new Creator<>() {
        @Override
        public DataNotificationConfig createFromParcel(Parcel in) {
            return new DataNotificationConfig(in);
        }

        @Override
        public DataNotificationConfig[] newArray(int size) {
            return new DataNotificationConfig[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mNotificationConfigType);
        dest.writeInt(mProximityNearCm);
        dest.writeInt(mProximityFarCm);
    }

    /**
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            NOTIFICATION_CONFIG_DISABLE,
            NOTIFICATION_CONFIG_ENABLE,
            NOTIFICATION_CONFIG_PROXIMITY_LEVEL,
            NOTIFICATION_CONFIG_PROXIMITY_EDGE,
    })
    public @interface NotificationConfigType {
    }

    // Range data notification will be disabled.
    public static final int NOTIFICATION_CONFIG_DISABLE = 0;
    // Range data notification will be enabled (default).
    public static final int NOTIFICATION_CONFIG_ENABLE = 1;
    // Range data notification is enabled when peer device is in the configured range - [near, far].
    public static final int NOTIFICATION_CONFIG_PROXIMITY_LEVEL = 2;
    //Range data notification is enabled when peer device enters or exits the configured range -
    // [near, far].
    public static final int NOTIFICATION_CONFIG_PROXIMITY_EDGE = 3;


    private DataNotificationConfig(Builder builder) {
        if (builder.mProximityNearCm > builder.mProximityFarCm) {
            throw new IllegalArgumentException(
                    "Ntf proximity near cannot be greater than Ntf proximity far");
        }
        mNotificationConfigType = builder.mNotificationConfigType;
        mProximityNearCm = builder.mProximityNearCm;
        mProximityFarCm = builder.mProximityFarCm;
    }

    /**
     * Returns the configured notification configuration type.
     *
     * @return the notification configuration type.
     *
     */
    @NotificationConfigType
    public int getNotificationConfigType() {
        return mNotificationConfigType;
    }

    /**
     * Returns the near proximity threshold in centimeters.
     *
     * @return the near proximity in centimeters.
     */
    @IntRange(from = 0, to = 20000)
    public int getProximityNearCm() {
        return mProximityNearCm;
    }

    /**
     * Returns the far proximity threshold in centimeters.
     *
     * @return the far proximity in centimeters.
     */
    @IntRange(from = 0, to = 20000)
    public int getProximityFarCm() {
        return mProximityFarCm;
    }

    /** Builder for {@link DataNotificationConfig} */
    public static final class Builder {
        @NotificationConfigType
        private int mNotificationConfigType = NOTIFICATION_CONFIG_ENABLE;
        private int mProximityNearCm = 0;
        private int mProximityFarCm = 20_000;

        /**
         * Sets the notification configuration type.
         *  <p> defaults to {@link NotificationConfigType#NOTIFICATION_CONFIG_ENABLE}
         *
         * @param config The notification configuration type to set.
         * @return this Builder instance.
         */
        @NonNull
        public Builder setNotificationConfigType(@NotificationConfigType int config) {
            mNotificationConfigType = config;
            return this;
        }

        /**
         * Sets the near proximity threshold in centimeters.
         * <p> defaults to 0 cm.
         *
         * @param proximityCm The near proximity to set, in centimeters.
         * @return this Builder instance.
         */
        @NonNull
        public Builder setProximityNearCm(@IntRange(from = 0, to = 20000) int proximityCm) {
            mProximityNearCm = proximityCm;
            return this;
        }

        /**
         * Sets the far proximity threshold in centimeters.
         * <p> defaults to 20000 cm.
         *
         * @param proximityCm The far proximity to set, in centimeters.
         * @return this Builder instance.
         */
        @NonNull
        public Builder setProximityFarCm(@IntRange(from = 0, to = 20000) int proximityCm) {
            mProximityFarCm = proximityCm;
            return this;
        }

        /**
         * Builds a new instance of {@link DataNotificationConfig}.
         *
         * @return a new {@link DataNotificationConfig} instance created using the current state of
         * the builder.
         */
        @NonNull
        public DataNotificationConfig build() {
            return new DataNotificationConfig(this);
        }
    }

    @Override
    public String toString() {
        return "DataNotificationConfig{ "
                + "mNotificationConfigType="
                + mNotificationConfigType
                + ", mProximityNearCm="
                + mProximityNearCm
                + ", mProximityFarCm="
                + mProximityFarCm
                + " }";
    }
}
