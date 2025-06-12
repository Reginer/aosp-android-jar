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

package android.health.connect.internal.datatypes;

import android.annotation.FloatRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.Constants;
import android.health.connect.datatypes.ExerciseRoute;
import android.health.connect.datatypes.units.Length;
import android.os.Parcel;

import com.android.internal.annotations.VisibleForTesting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @see ExerciseRoute
 * @hide
 */
public class ExerciseRouteInternal {
    private final List<LocationInternal> mRouteExerciseRouteLocations;

    public ExerciseRouteInternal(@NonNull List<LocationInternal> routeExerciseRouteLocations) {
        Objects.requireNonNull(routeExerciseRouteLocations);
        mRouteExerciseRouteLocations = new ArrayList<>(routeExerciseRouteLocations);
    }

    @NonNull
    public List<LocationInternal> getRouteLocations() {
        return mRouteExerciseRouteLocations;
    }

    /** Read the route from parcel. */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    @Nullable
    public static ExerciseRouteInternal readFromParcel(@NonNull Parcel parcel) {
        boolean routeIsNull = parcel.readBoolean();
        if (routeIsNull) {
            return null;
        }

        int routeSize = parcel.readInt();
        ArrayList<LocationInternal> routeLocations = new ArrayList<>(routeSize);
        for (int i = 0; i < routeSize; i++) {
            routeLocations.add(LocationInternal.readFromParcel(parcel));
        }
        return new ExerciseRouteInternal(routeLocations);
    }

    /** Write the route to parcel. */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public static void writeToParcel(
            @Nullable ExerciseRouteInternal route, @NonNull Parcel parcel) {
        // Write if the route is null first to restore correctly.
        if (route == null) {
            parcel.writeBoolean(true);
        } else {
            parcel.writeBoolean(false);
            route.writeToParcel(parcel);
        }
    }

    private void writeToParcel(@NonNull Parcel parcel) {
        parcel.writeInt(mRouteExerciseRouteLocations.size());
        for (LocationInternal location : mRouteExerciseRouteLocations) {
            location.writeToParcel(parcel);
        }
    }

    /** Convert internal route to external route object. */
    @VisibleForTesting
    public ExerciseRoute toExternalRoute() {
        List<ExerciseRoute.Location> routeLocations =
                new ArrayList<>(mRouteExerciseRouteLocations.size());
        for (LocationInternal location : mRouteExerciseRouteLocations) {
            routeLocations.add(location.toExternalExerciseRouteLocation());
        }
        return new ExerciseRoute(routeLocations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExerciseRouteInternal)) return false;
        ExerciseRouteInternal that = (ExerciseRouteInternal) o;
        return getRouteLocations().equals(that.getRouteLocations());
    }

    /** Add location to the route */
    void addLocation(LocationInternal location) {
        mRouteExerciseRouteLocations.add(location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mRouteExerciseRouteLocations);
    }
    /**
     * @see ExerciseRoute.Location
     * @hide
     */
    public static final class LocationInternal {
        private long mTime = Constants.DEFAULT_LONG;
        private double mLatitude = Constants.DEFAULT_DOUBLE;
        private double mLongitude = Constants.DEFAULT_DOUBLE;
        private double mHorizontalAccuracy = Constants.DEFAULT_DOUBLE;
        private double mVerticalAccuracy = Constants.DEFAULT_DOUBLE;
        private double mAltitude = Constants.DEFAULT_DOUBLE;

        /**
         * @return time of this location time point
         */
        public long getTime() {
            return mTime;
        }

        /** returns this object with the specified time */
        @NonNull
        public LocationInternal setTime(long time) {
            this.mTime = time;
            return this;
        }

        /**
         * @return longitude of this location time point
         */
        public double getLongitude() {
            return mLongitude;
        }

        /** returns this object with the specified longitude */
        @NonNull
        public LocationInternal setLongitude(
                @FloatRange(from = -180.0, to = 180.0) double longitude) {
            this.mLongitude = longitude;
            return this;
        }

        /**
         * @return latitude of this location time point
         */
        public double getLatitude() {
            return mLatitude;
        }

        /** returns this object with the specified latitude */
        @NonNull
        public LocationInternal setLatitude(@FloatRange(from = -90.0, to = 90.0) double latitude) {
            this.mLatitude = latitude;
            return this;
        }

        /**
         * @return horizontal accuracy of this location time point
         */
        public double getHorizontalAccuracy() {
            return mHorizontalAccuracy;
        }

        /** returns this object with the specified horizontal accuracy */
        @NonNull
        public LocationInternal setHorizontalAccuracy(double horizontalAccuracy) {
            this.mHorizontalAccuracy = horizontalAccuracy;
            return this;
        }

        /**
         * @return vertical accuracy of this location time point
         */
        public double getVerticalAccuracy() {
            return mVerticalAccuracy;
        }

        /** returns this object with the specified vertical accuracy */
        @NonNull
        public LocationInternal setVerticalAccuracy(double verticalAccuracy) {
            this.mVerticalAccuracy = verticalAccuracy;
            return this;
        }

        /**
         * @return altitude of this location time point
         */
        public double getAltitude() {
            return mAltitude;
        }

        /** returns this object with the specified altitude */
        @NonNull
        public LocationInternal setAltitude(double altitude) {
            this.mAltitude = altitude;
            return this;
        }

        /** Read Location object from Parcel. */
        @VisibleForTesting
        public static LocationInternal readFromParcel(@NonNull Parcel parcel) {
            return new LocationInternal()
                    .setTime(parcel.readLong())
                    .setLatitude(parcel.readDouble())
                    .setLongitude(parcel.readDouble())
                    .setHorizontalAccuracy(parcel.readDouble())
                    .setVerticalAccuracy(parcel.readDouble())
                    .setAltitude(parcel.readDouble());
        }

        /** Write Location object from Parcel. */
        @VisibleForTesting
        public void writeToParcel(@NonNull Parcel parcel) {
            parcel.writeLong(getTime());
            parcel.writeDouble(getLatitude());
            parcel.writeDouble(getLongitude());
            parcel.writeDouble(getHorizontalAccuracy());
            parcel.writeDouble(getVerticalAccuracy());
            parcel.writeDouble(getAltitude());
        }

        /** Convert LocationInternal to Location external object. */
        @VisibleForTesting
        public ExerciseRoute.Location toExternalExerciseRouteLocation() {
            ExerciseRoute.Location.Builder builder =
                    new ExerciseRoute.Location.Builder(
                            Instant.ofEpochMilli(getTime()), getLatitude(), getLongitude());

            if (getHorizontalAccuracy() != Constants.DEFAULT_DOUBLE) {
                builder.setHorizontalAccuracy(Length.fromMeters(getHorizontalAccuracy()));
            }

            if (getVerticalAccuracy() != Constants.DEFAULT_DOUBLE) {
                builder.setVerticalAccuracy(Length.fromMeters(getVerticalAccuracy()));
            }

            if (getAltitude() != Constants.DEFAULT_DOUBLE) {
                builder.setAltitude(Length.fromMeters(getAltitude()));
            }
            return builder.buildWithoutValidation();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LocationInternal)) return false;
            LocationInternal that = (LocationInternal) o;
            return (getLatitude() == that.getLatitude())
                    && (getLongitude() == that.getLongitude())
                    && (getTime() == that.getTime())
                    && (getHorizontalAccuracy() == that.getHorizontalAccuracy())
                    && (getVerticalAccuracy() == that.getVerticalAccuracy())
                    && (getAltitude() == that.getAltitude());
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    getTime(),
                    getLatitude(),
                    getLongitude(),
                    getHorizontalAccuracy(),
                    getVerticalAccuracy(),
                    getAltitude());
        }
    }
}
