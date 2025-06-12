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

package android.net.wifi.usd;

import android.annotation.FlaggedApi;
import android.annotation.SystemApi;
import android.net.wifi.flags.Flags;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * The characteristics of the USD implementation.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(Flags.FLAG_USD)
public final class Characteristics implements Parcelable {
    private final Bundle mCharacteristics;
    /** @hide */
    public static final String KEY_MAX_SERVICE_NAME_LENGTH = "key_max_service_name_length";
    /** @hide */
    public static final String KEY_MAX_SERVICE_SPECIFIC_INFO_LENGTH =
            "key_max_service_specific_info_length";
    /** @hide */
    public static final String KEY_MAX_MATCH_FILTER_LENGTH = "key_max_match_filter_length";
    /** @hide */
    public static final String KEY_MAX_NUM_PUBLISH_SESSIONS = "key_max_num_publish_session";
    /** @hide */
    public static final String KEY_MAX_NUM_SUBSCRIBE_SESSIONS = "key_max_num_subscribe_session";


    /** @hide : should not be created by apps */
    public Characteristics(Bundle characteristics) {
        mCharacteristics = characteristics;
    }

    private Characteristics(@NonNull Parcel in) {
        mCharacteristics = in.readBundle(getClass().getClassLoader());
    }

    @NonNull
    public static final Creator<Characteristics> CREATOR = new Creator<Characteristics>() {
        @Override
        public Characteristics createFromParcel(Parcel in) {
            return new Characteristics(in);
        }

        @Override
        public Characteristics[] newArray(int size) {
            return new Characteristics[size];
        }
    };

    /**
     * Returns the maximum string length that can be used to specify a USD service name.
     *
     * @return A positive integer, maximum string length of USD service name.
     */
    public int getMaxServiceNameLength() {
        return mCharacteristics.getInt(KEY_MAX_SERVICE_NAME_LENGTH);
    }

    /**
     * Returns the maximum length of byte array that can be used to specify a service specific
     * information field: the arbitrary load used in discovery or the message length of USD
     * message exchange. Restricts the parameters of the
     * {@link PublishConfig.Builder#setServiceSpecificInfo(byte[])},
     * {@link SubscribeConfig.Builder#setServiceSpecificInfo(byte[])},
     * {@link PublishSession#sendMessage(int, byte[], Executor, Consumer)}  and
     * {@link SubscribeSession#sendMessage(int, byte[], Executor, Consumer)}
     * variants.
     *
     * @return A positive integer, maximum length of byte array for USD messaging.
     */
    public int getMaxServiceSpecificInfoLength() {
        return mCharacteristics.getInt(KEY_MAX_SERVICE_SPECIFIC_INFO_LENGTH);
    }

    /**
     * Returns the maximum length of byte array that can be used to specify a USD match filter.
     * Restricts the parameters of the
     * {@link PublishConfig.Builder#setTxMatchFilter(List)},
     * {@link PublishConfig.Builder#setRxMatchFilter(List)},
     * {@link SubscribeConfig.Builder#setTxMatchFilter(List)} and
     * {@link SubscribeConfig.Builder#setRxMatchFilter(List)}
     *
     * @return A positive integer, maximum length of byte array for USD discovery match filter.
     */
    public int getMaxMatchFilterLength() {
        return mCharacteristics.getInt(KEY_MAX_MATCH_FILTER_LENGTH);
    }

    /**
     * Returns the maximum number of publish sessions supported by USD
     *
     * @return A positive integer
     */
    public int getMaxNumberOfPublishSessions() {
        return mCharacteristics.getInt(KEY_MAX_NUM_PUBLISH_SESSIONS);
    }

    /**
     * Returns the maximum number of subscribe sessions supported by USD
     *
     * @return A positive integer
     */
    public int getMaxNumberOfSubscribeSessions() {
        return mCharacteristics.getInt(KEY_MAX_NUM_SUBSCRIBE_SESSIONS);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeBundle(mCharacteristics);
    }
}
