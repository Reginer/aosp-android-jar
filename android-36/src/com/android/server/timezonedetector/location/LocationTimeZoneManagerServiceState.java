/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.timezonedetector.location;

import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.server.timezonedetector.LocationAlgorithmEvent;
import com.android.server.timezonedetector.location.LocationTimeZoneProvider.ProviderState;
import com.android.server.timezonedetector.location.LocationTimeZoneProviderController.State;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A snapshot of the location time zone manager service's state for tests. */
final class LocationTimeZoneManagerServiceState {

    private final @State String mControllerState;
    @Nullable private final LocationAlgorithmEvent mLastEvent;
    @NonNull private final List<@State String> mControllerStates;
    @NonNull private final List<ProviderState> mPrimaryProviderStates;
    @NonNull private final List<ProviderState> mSecondaryProviderStates;

    LocationTimeZoneManagerServiceState(@NonNull Builder builder) {
        mControllerState = builder.mControllerState;
        mLastEvent = builder.mLastEvent;
        mControllerStates = Objects.requireNonNull(builder.mControllerStates);
        mPrimaryProviderStates = Objects.requireNonNull(builder.mPrimaryProviderStates);
        mSecondaryProviderStates = Objects.requireNonNull(builder.mSecondaryProviderStates);
    }

    public @State String getControllerState() {
        return mControllerState;
    }

    @Nullable
    public LocationAlgorithmEvent getLastEvent() {
        return mLastEvent;
    }

    @NonNull
    public List<@State String> getControllerStates() {
        return mControllerStates;
    }

    @NonNull
    public List<ProviderState> getPrimaryProviderStates() {
        return Collections.unmodifiableList(mPrimaryProviderStates);
    }

    @NonNull
    public List<ProviderState> getSecondaryProviderStates() {
        return Collections.unmodifiableList(mSecondaryProviderStates);
    }

    @Override
    public String toString() {
        return "LocationTimeZoneManagerServiceState{"
                + "mControllerState=" + mControllerState
                + ", mLastEvent=" + mLastEvent
                + ", mControllerStates=" + mControllerStates
                + ", mPrimaryProviderStates=" + mPrimaryProviderStates
                + ", mSecondaryProviderStates=" + mSecondaryProviderStates
                + '}';
    }

    static final class Builder {

        private @State String mControllerState;
        private LocationAlgorithmEvent mLastEvent;
        private List<@State String> mControllerStates;
        private List<ProviderState> mPrimaryProviderStates;
        private List<ProviderState> mSecondaryProviderStates;

        @NonNull
        public Builder setControllerState(@State String stateEnum) {
            mControllerState = stateEnum;
            return this;
        }

        @NonNull
        Builder setLastEvent(@NonNull LocationAlgorithmEvent lastEvent) {
            mLastEvent = Objects.requireNonNull(lastEvent);
            return this;
        }

        @NonNull
        public Builder setStateChanges(@NonNull List<@State String> states) {
            mControllerStates = new ArrayList<>(states);
            return this;
        }

        @NonNull
        Builder setPrimaryProviderStateChanges(@NonNull List<ProviderState> primaryProviderStates) {
            mPrimaryProviderStates = new ArrayList<>(primaryProviderStates);
            return this;
        }

        @NonNull
        Builder setSecondaryProviderStateChanges(
                @NonNull List<ProviderState> secondaryProviderStates) {
            mSecondaryProviderStates = new ArrayList<>(secondaryProviderStates);
            return this;
        }

        @NonNull
        LocationTimeZoneManagerServiceState build() {
            return new LocationTimeZoneManagerServiceState(this);
        }
    }
}
