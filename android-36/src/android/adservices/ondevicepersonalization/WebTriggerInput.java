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

package android.adservices.ondevicepersonalization;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.net.Uri;

import com.android.adservices.ondevicepersonalization.flags.Flags;

import java.util.Objects;

/**
 * The input data for
 * {@link IsolatedWorker#onWebTrigger(WebTriggerInput, android.os.OutcomeReceiver)}.
 */
public final class WebTriggerInput {
    /** The destination URL (landing page) where the trigger event occurred. */
    @NonNull private Uri mDestinationUrl;

    /** The package name of the app where the trigger event occurred */
    @NonNull private String mAppPackageName;

    /**
     * Additional data returned by the server as part of the web trigger registration
     * to be sent to the {@link IsolatedService}. This can be {@code null} if the server
     * does not need to send data to the service for processing web triggers.
     */
    @NonNull private byte[] mData;

    /** @hide */
    public WebTriggerInput(@NonNull WebTriggerInputParcel parcel) {
        this(parcel.getDestinationUrl(), parcel.getAppPackageName(), parcel.getData());
    }

    /**
     * Creates a new WebTriggerInput.
     *
     * @param destinationUrl
     *   The destination URL (landing page) where the trigger event occurred.
     * @param appPackageName
     *   The package name of the app where the trigger event occurred
     * @param data
     *   Additional data returned by the server as part of the web trigger registration
     *   to be sent to the {@link IsolatedService}. This can be {@code null} if the server
     *   does not need to send data to the service for processing web triggers.
     */
    @FlaggedApi(Flags.FLAG_DATA_CLASS_MISSING_CTORS_AND_GETTERS_ENABLED)
    public WebTriggerInput(
            @NonNull Uri destinationUrl,
            @NonNull String appPackageName,
            @NonNull byte[] data) {
        this.mDestinationUrl = Objects.requireNonNull(destinationUrl);
        this.mAppPackageName = Objects.requireNonNull(appPackageName);
        this.mData = Objects.requireNonNull(data);
    }

    /**
     * The destination URL (landing page) where the trigger event occurred.
     */
    public @NonNull Uri getDestinationUrl() {
        return mDestinationUrl;
    }

    /**
     * The package name of the app where the trigger event occurred
     */
    public @NonNull String getAppPackageName() {
        return mAppPackageName;
    }

    /**
     * Additional data returned by the server as part of the web trigger registration
     * to be sent to the {@link IsolatedService}. This can be {@code null} if the server
     * does not need to send data to the service for processing web triggers.
     */
    public @NonNull byte[] getData() {
        return mData;
    }

    @Override
    public boolean equals(@android.annotation.Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        @SuppressWarnings("unchecked")
        WebTriggerInput that = (WebTriggerInput) o;
        //noinspection PointlessBooleanExpression
        return true
                && java.util.Objects.equals(mDestinationUrl, that.mDestinationUrl)
                && java.util.Objects.equals(mAppPackageName, that.mAppPackageName)
                && java.util.Arrays.equals(mData, that.mData);
    }

    @Override
    public int hashCode() {
        int _hash = 1;
        _hash = 31 * _hash + java.util.Objects.hashCode(mDestinationUrl);
        _hash = 31 * _hash + java.util.Objects.hashCode(mAppPackageName);
        _hash = 31 * _hash + java.util.Arrays.hashCode(mData);
        return _hash;
    }
}
