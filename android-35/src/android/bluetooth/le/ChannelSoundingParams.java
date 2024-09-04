/*
 * Copyright 2024 The Android Open Source Project
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

package android.bluetooth.le;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.bluetooth.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The {@link ChannelSoundingParams} provide a way to adjust distance measurement preferences for
 * {@link DISTANCE_MEASUREMENT_METHOD_CHANNEL_SOUNDING}. Use {@link ChannelSoundingParams.Builder}
 * to create an instance of this class.
 *
 * @hide
 */
@FlaggedApi(Flags.FLAG_CHANNEL_SOUNDING)
@SystemApi
public final class ChannelSoundingParams implements Parcelable {

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                SIGHT_TYPE_UNKNOWN,
                SIGHT_TYPE_LINE_OF_SIGHT,
                SIGHT_TYPE_NON_LINE_OF_SIGHT,
            })
    @interface SightType {}

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {LOCATION_TYPE_UNKNOWN, LOCATION_TYPE_INDOOR, LOCATION_TYPE_OUTDOOR})
    @interface LocationType {}

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            value = {
                CS_SECURITY_LEVEL_UNKNOWN,
                CS_SECURITY_LEVEL_ONE,
                CS_SECURITY_LEVEL_TWO,
                CS_SECURITY_LEVEL_THREE,
                CS_SECURITY_LEVEL_FOUR
            })
    @interface CsSecurityLevel {}

    /**
     * Sight type is unknown.
     *
     * @hide
     */
    @SystemApi public static final int SIGHT_TYPE_UNKNOWN = 0;

    /**
     * Remote device is in line of sight.
     *
     * @hide
     */
    @SystemApi public static final int SIGHT_TYPE_LINE_OF_SIGHT = 1;

    /**
     * Remote device is not in line of sight.
     *
     * @hide
     */
    @SystemApi public static final int SIGHT_TYPE_NON_LINE_OF_SIGHT = 2;

    /**
     * Location type is unknown.
     *
     * @hide
     */
    @SystemApi public static final int LOCATION_TYPE_UNKNOWN = 0;

    /**
     * The location of the usecase is indoor.
     *
     * @hide
     */
    @SystemApi public static final int LOCATION_TYPE_INDOOR = 1;

    /**
     * The location of the usecase is outdoor.
     *
     * @hide
     */
    @SystemApi public static final int LOCATION_TYPE_OUTDOOR = 2;

    /**
     * Return value for {@link
     * DistanceMeasurementManager#getChannelSoundingMaxSupportedSecurityLevel(BluetoothDevice)} and
     * {@link DistanceMeasurementManager#getLocalChannelSoundingMaxSupportedSecurityLevel()} when
     * Channel Sounding is not supported, or encounters an internal error.
     *
     * @hide
     */
    @SystemApi public static final int CS_SECURITY_LEVEL_UNKNOWN = 0;

    /**
     * Either CS tone or CS RTT.
     *
     * @hide
     */
    @SystemApi public static final int CS_SECURITY_LEVEL_ONE = 1;

    /**
     * 150 ns CS RTT accuracy and CS tones.
     *
     * @hide
     */
    @SystemApi public static final int CS_SECURITY_LEVEL_TWO = 2;

    /**
     * 10 ns CS RTT accuracy and CS tones.
     *
     * @hide
     */
    @SystemApi public static final int CS_SECURITY_LEVEL_THREE = 3;

    /**
     * Level 3 with the addition of CS RTT sounding sequence or random sequence payloads, and
     * support of the Normalized Attack Detector Metric requirements.
     *
     * @hide
     */
    @SystemApi public static final int CS_SECURITY_LEVEL_FOUR = 4;

    private int mSightType;
    private int mLocationType;
    private int mCsSecurityLevel;

    /** @hide */
    public ChannelSoundingParams(int sightType, int locationType, int csSecurityLevel) {
        mSightType = sightType;
        mLocationType = locationType;
        mCsSecurityLevel = csSecurityLevel;
    }

    /**
     * Returns sight type of this ChannelSoundingParams.
     *
     * @hide
     */
    @SystemApi
    @SightType
    public int getSightType() {
        return mSightType;
    }

    /**
     * Returns location type of this ChannelSoundingParams.
     *
     * @hide
     */
    @SystemApi
    @LocationType
    public int getLocationType() {
        return mLocationType;
    }

    /**
     * Returns CS security level of this ChannelSoundingParams.
     *
     * @hide
     */
    @SystemApi
    @CsSecurityLevel
    public int getCsSecurityLevel() {
        return mCsSecurityLevel;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeInt(mSightType);
        out.writeInt(mLocationType);
        out.writeInt(mCsSecurityLevel);
    }

    /** A {@link Parcelable.Creator} to create {@link ChannelSoundingParams} from parcel. */
    public static final @NonNull Parcelable.Creator<ChannelSoundingParams> CREATOR =
            new Parcelable.Creator<ChannelSoundingParams>() {
                @Override
                public @NonNull ChannelSoundingParams createFromParcel(@NonNull Parcel in) {
                    Builder builder = new Builder();
                    builder.setSightType(in.readInt());
                    builder.setLocationType(in.readInt());
                    builder.setCsSecurityLevel(in.readInt());
                    return builder.build();
                }

                @Override
                public @NonNull ChannelSoundingParams[] newArray(int size) {
                    return new ChannelSoundingParams[size];
                }
            };

    /**
     * Builder for {@link ChannelSoundingParams}.
     *
     * @hide
     */
    @SystemApi
    public static final class Builder {
        private int mSightType = SIGHT_TYPE_UNKNOWN;
        private int mLocationType = LOCATION_TYPE_UNKNOWN;
        private int mCsSecurityLevel = CS_SECURITY_LEVEL_ONE;

        /**
         * Set sight type for the ChannelSoundingParams.
         *
         * @param sightType sight type of this ChannelSoundingParams
         * @return the same Builder instance
         * @hide
         */
        @SystemApi
        public @NonNull Builder setSightType(@SightType int sightType) {
            switch (sightType) {
                case SIGHT_TYPE_UNKNOWN:
                case SIGHT_TYPE_LINE_OF_SIGHT:
                case SIGHT_TYPE_NON_LINE_OF_SIGHT:
                    mSightType = sightType;
                    break;
                default:
                    throw new IllegalArgumentException("unknown sight type " + sightType);
            }
            return this;
        }

        /**
         * Set location type for the ChannelSoundingParams.
         *
         * @param locationType location type of this ChannelSoundingParams
         * @return the same Builder instance
         * @hide
         */
        @SystemApi
        public @NonNull Builder setLocationType(@LocationType int locationType) {
            switch (locationType) {
                case LOCATION_TYPE_UNKNOWN:
                case LOCATION_TYPE_INDOOR:
                case LOCATION_TYPE_OUTDOOR:
                    mLocationType = locationType;
                    break;
                default:
                    throw new IllegalArgumentException("unknown location type " + locationType);
            }
            return this;
        }

        /**
         * Set CS security level for the ChannelSoundingParams.
         *
         * <p>See: https://bluetooth.com/specifications/specs/channel-sounding-cr-pr/
         *
         * @param csSecurityLevel cs security level of this ChannelSoundingParams
         * @return the same Builder instance
         * @hide
         */
        @SystemApi
        public @NonNull Builder setCsSecurityLevel(@CsSecurityLevel int csSecurityLevel) {
            switch (csSecurityLevel) {
                case CS_SECURITY_LEVEL_ONE:
                case CS_SECURITY_LEVEL_TWO:
                case CS_SECURITY_LEVEL_THREE:
                case CS_SECURITY_LEVEL_FOUR:
                    mCsSecurityLevel = csSecurityLevel;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "unknown CS security level " + csSecurityLevel);
            }
            return this;
        }

        /**
         * Build the {@link ChannelSoundingParams} object.
         *
         * @hide
         */
        @SystemApi
        public @NonNull ChannelSoundingParams build() {
            return new ChannelSoundingParams(mSightType, mLocationType, mCsSecurityLevel);
        }
    }
}
