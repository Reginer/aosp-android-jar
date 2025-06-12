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

package android.net.wifi;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.wifi.flags.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Mirrored Stream Classification Service (MSCS) parameters.
 * Refer to section 3.1 of the Wi-Fi QoS Management Specification v3.0.
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
public final class MscsParams implements Parcelable {
    /** IP version used by the traffic stream */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int FRAME_CLASSIFIER_IP_VERSION = 1 << 0;

    /** Source IP address used by the traffic stream */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int FRAME_CLASSIFIER_SRC_IP_ADDR = 1 << 1;

    /** Destination IP address used by the traffic stream */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int FRAME_CLASSIFIER_DST_IP_ADDR = 1 << 2;

    /** Source port used by the traffic stream */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int FRAME_CLASSIFIER_SRC_PORT = 1 << 3;

    /** Destination port used by the traffic stream */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int FRAME_CLASSIFIER_DST_PORT = 1 << 4;

    /** DSCP value used by the traffic stream */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int FRAME_CLASSIFIER_DSCP = 1 << 5;

    /** Indicates Protocol if using IPv4, or Next Header if using IPv6 */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int FRAME_CLASSIFIER_PROTOCOL_NEXT_HDR = 1 << 6;

    /** Flow label. Only applicable if using IPv6 */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final int FRAME_CLASSIFIER_FLOW_LABEL = 1 << 7;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, prefix = { "FRAME_CLASSIFIER_" }, value = {
            FRAME_CLASSIFIER_IP_VERSION,
            FRAME_CLASSIFIER_SRC_IP_ADDR,
            FRAME_CLASSIFIER_DST_IP_ADDR,
            FRAME_CLASSIFIER_SRC_PORT,
            FRAME_CLASSIFIER_DST_PORT,
            FRAME_CLASSIFIER_DSCP,
            FRAME_CLASSIFIER_PROTOCOL_NEXT_HDR,
            FRAME_CLASSIFIER_FLOW_LABEL,
    })
    public @interface FrameClassifierField {}

    /** @hide */
    public static final int DEFAULT_FRAME_CLASSIFIER_FIELDS =
            FRAME_CLASSIFIER_IP_VERSION
                | FRAME_CLASSIFIER_SRC_IP_ADDR
                | FRAME_CLASSIFIER_DST_IP_ADDR
                | FRAME_CLASSIFIER_SRC_PORT
                | FRAME_CLASSIFIER_DST_PORT
                | FRAME_CLASSIFIER_PROTOCOL_NEXT_HDR;
    /** @hide */
    public static final int DEFAULT_USER_PRIORITY_BITMAP = (1 << 6) | (1 << 7);
    /** @hide */
    public static final int DEFAULT_USER_PRIORITY_LIMIT = 7;   // keep the original priority
    /** @hide */
    public static final int MAX_STREAM_TIMEOUT_US = 60_000_000;    // 60 seconds

    private final int mFrameClassifierFields;
    private final int mUserPriorityBitmap;
    private final int mUserPriorityLimit;
    private final int mStreamTimeoutUs;

    private MscsParams(int frameClassifierFields, int userPriorityBitmap,
            int userPriorityLimit, int streamTimeoutUs) {
        mFrameClassifierFields = frameClassifierFields;
        mUserPriorityBitmap = userPriorityBitmap;
        mUserPriorityLimit = userPriorityLimit;
        mStreamTimeoutUs = streamTimeoutUs;
    }

    /**
     * Get the frame classifier fields bitmap.
     * See {@link Builder#setFrameClassifierFields(int)}
     *
     * @return Bitmap of {@link FrameClassifierField} represented as an int.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public @FrameClassifierField int getFrameClassifierFields() {
        return mFrameClassifierFields;
    }

    /**
     * Get the user priority bitmap. See {@link Builder#setUserPriorityBitmap(int)}
     *
     * @return Bitmap of user priorities represented as an int.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int getUserPriorityBitmap() {
        return mUserPriorityBitmap;
    }

    /**
     * Get the user priority limit. See {@link Builder#setUserPriorityLimit(int)}
     *
     * @return User priority limit.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @IntRange(from = 0, to = 7)
    public int getUserPriorityLimit() {
        return mUserPriorityLimit;
    }

    /**
     * Get the stream timeout in microseconds. See {@link Builder#setStreamTimeoutUs(int)}
     *
     * @return Stream timeout in microseconds.
     */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @IntRange(from = 0, to = MAX_STREAM_TIMEOUT_US)
    public int getStreamTimeoutUs() {
        return mStreamTimeoutUs;
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MscsParams that = (MscsParams) o;
        return mFrameClassifierFields == that.mFrameClassifierFields
                && mUserPriorityBitmap == that.mUserPriorityBitmap
                && mUserPriorityLimit == that.mUserPriorityLimit
                && mStreamTimeoutUs == that.mStreamTimeoutUs;
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    @Override
    public int hashCode() {
        return Objects.hash(mFrameClassifierFields, mUserPriorityBitmap,
                mUserPriorityLimit, mStreamTimeoutUs);
    }

    @Override
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mFrameClassifierFields);
        dest.writeInt(mUserPriorityBitmap);
        dest.writeInt(mUserPriorityLimit);
        dest.writeInt(mStreamTimeoutUs);
    }

    /** @hide */
    MscsParams(@NonNull Parcel in) {
        this.mFrameClassifierFields = in.readInt();
        this.mUserPriorityBitmap = in.readInt();
        this.mUserPriorityLimit = in.readInt();
        this.mStreamTimeoutUs = in.readInt();
    }

    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final @NonNull Parcelable.Creator<MscsParams> CREATOR =
            new Parcelable.Creator<MscsParams>() {
                @Override
                public MscsParams createFromParcel(Parcel in) {
                    return new MscsParams(in);
                }

                @Override
                public MscsParams[] newArray(int size) {
                    return new MscsParams[size];
                }
            };

    /** Builder for {@link MscsParams}. */
    @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
    public static final class Builder {
        private int mFrameClassifierFields = DEFAULT_FRAME_CLASSIFIER_FIELDS;
        private int mUserPriorityBitmap = DEFAULT_USER_PRIORITY_BITMAP;
        private int mUserPriorityLimit = DEFAULT_USER_PRIORITY_LIMIT;
        private int mStreamTimeoutUs = MAX_STREAM_TIMEOUT_US;

        /**
         * Constructor for {@link Builder}.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        public Builder() {}

        /**
         * Sets a bitmap of {@link FrameClassifierField} indicating which TCLAS Type 4 frame
         * classifier fields should be used to build a classifier.
         *
         * @param frameClassifierFields Bitmap indicating the requested fields.
         * @throws IllegalArgumentException if any bits other than bits 0-7 are set.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setFrameClassifierFields(@FrameClassifierField int frameClassifierFields) {
            if ((frameClassifierFields & 0xFFFFFF00) != 0) {
                throw new IllegalArgumentException("frameClassifierFields can only use bits 0-7");
            }
            mFrameClassifierFields = frameClassifierFields;
            return this;
        }

        /**
         * Sets a bitmap indicating which User Priorities (UPs) should be classified using MSCS.
         * The least significant bit corresponds to UP 0, and the most significant
         * bit to UP 7. Setting a bit to 1 indicates that UP should be classified.
         *
         * @param userPriorityBitmap Bitmap indicating which UPs should be classified.
         * @throws IllegalArgumentException if any bits other than bits 0-7 are set.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setUserPriorityBitmap(int userPriorityBitmap) {
            if ((userPriorityBitmap & 0xFFFFFF00) != 0) {
                throw new IllegalArgumentException("userPriorityBitmap can only use bits 0-7");
            }
            mUserPriorityBitmap = userPriorityBitmap;
            return this;
        }

        /**
         * Sets the maximum user priority that can be assigned using the MSCS service.
         * Value must be between 0 and 7 (inclusive).
         *
         * @param userPriorityLimit Maximum user priority that can be assigned by MSCS.
         * @throws IllegalArgumentException if the provided value is outside the expected range of
         *                                  0-7 (inclusive).
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setUserPriorityLimit(@IntRange(from = 0, to = 7) int userPriorityLimit) {
            if (userPriorityLimit < 0 || userPriorityLimit > 7) {
                throw new IllegalArgumentException(
                        "userPriorityLimit must be between 0-7 (inclusive)");
            }
            mUserPriorityLimit = userPriorityLimit;
            return this;
        }

        /**
         * Set the minimum timeout (in microseconds) to keep this request in the MSCS list.
         *
         * @param streamTimeoutUs Minimum timeout in microseconds.
         * @throws IllegalArgumentException if the provided value is outside the expected range of
         *                                  0-60 seconds (inclusive).
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public Builder setStreamTimeoutUs(
                @IntRange(from = 0, to = MAX_STREAM_TIMEOUT_US) int streamTimeoutUs) {
            if (streamTimeoutUs < 0 || streamTimeoutUs > MAX_STREAM_TIMEOUT_US) {
                throw new IllegalArgumentException("streamTimeoutUs must be 60 seconds or less");
            }
            mStreamTimeoutUs = streamTimeoutUs;
            return this;
        }

        /**
         * Construct an MscsParams object using the specified parameters.
         */
        @FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
        @NonNull
        public MscsParams build() {
            return new MscsParams(mFrameClassifierFields, mUserPriorityBitmap,
                    mUserPriorityLimit, mStreamTimeoutUs);
        }
    }
}
