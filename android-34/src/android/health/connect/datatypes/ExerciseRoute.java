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

package android.health.connect.datatypes;

import android.annotation.FloatRange;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.validation.ValidationUtils;
import android.health.connect.internal.datatypes.ExerciseRouteInternal;
import android.os.Parcel;
import android.os.Parcelable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Route of the exercise session. Contains sequence of location points with timestamps. */
public final class ExerciseRoute implements Parcelable {
    private final List<Location> mRouteLocations;

    @NonNull
    public static final Creator<ExerciseRoute> CREATOR =
            new Creator<>() {

                @Override
                public ExerciseRoute createFromParcel(Parcel source) {
                    int size = source.readInt();
                    List<Location> locations = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        locations.add(i, Location.CREATOR.createFromParcel(source));
                    }
                    return new ExerciseRoute(locations);
                }

                @Override
                public ExerciseRoute[] newArray(int size) {
                    return new ExerciseRoute[size];
                }
            };

    /**
     * Creates {@link ExerciseRoute} instance
     *
     * @param routeLocations list of locations with timestamps that make up the route
     */
    public ExerciseRoute(@NonNull List<Location> routeLocations) {
        Objects.requireNonNull(routeLocations);
        mRouteLocations = routeLocations;
    }

    @NonNull
    public List<Location> getRouteLocations() {
        return mRouteLocations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExerciseRoute)) return false;
        ExerciseRoute that = (ExerciseRoute) o;
        return getRouteLocations().equals(that.getRouteLocations());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRouteLocations());
    }

    /** @hide */
    public ExerciseRouteInternal toRouteInternal() {
        List<ExerciseRouteInternal.LocationInternal> routeLocations =
                new ArrayList<>(getRouteLocations().size());
        for (ExerciseRoute.Location location : getRouteLocations()) {
            routeLocations.add(location.toExerciseRouteLocationInternal());
        }
        return new ExerciseRouteInternal(routeLocations);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mRouteLocations.size());
        for (int i = 0; i < mRouteLocations.size(); i++) {
            mRouteLocations.get(i).writeToParcel(dest, flags);
        }
    }

    /** Point in the time and space. Used in {@link ExerciseRoute}. */
    public static final class Location implements Parcelable {
        // Values are used for FloatRange annotation in latitude/longitude getters and constructor.
        private static final double MIN_LONGITUDE = -180;
        private static final double MAX_LONGITUDE = 180;
        private static final double MIN_LATITUDE = -90;
        private static final double MAX_LATITUDE = 90;

        private final Instant mTime;
        private final double mLatitude;
        private final double mLongitude;
        private final Length mHorizontalAccuracy;
        private final Length mVerticalAccuracy;
        private final Length mAltitude;

        @NonNull
        public static final Creator<Location> CREATOR =
                new Creator<>() {
                    @Override
                    public Location createFromParcel(Parcel source) {
                        Instant timestamp = Instant.ofEpochMilli(source.readLong());
                        double lat = source.readDouble();
                        double lon = source.readDouble();
                        Builder builder = new Builder(timestamp, lat, lon);
                        if (source.readBoolean()) {
                            builder.setHorizontalAccuracy(Length.fromMeters(source.readDouble()));
                        }
                        if (source.readBoolean()) {
                            builder.setVerticalAccuracy(Length.fromMeters(source.readDouble()));
                        }
                        if (source.readBoolean()) {
                            builder.setAltitude(Length.fromMeters(source.readDouble()));
                        }
                        return builder.build();
                    }

                    @Override
                    public Location[] newArray(int size) {
                        return new Location[size];
                    }
                };

        /**
         * Represents a single location in an exercise route.
         *
         * @param time The point in time when the measurement was taken.
         * @param latitude Latitude of a location represented as a double, in degrees. Valid range:
         *     from -90 to 90 degrees.
         * @param longitude Longitude of a location represented as a double, in degrees. Valid
         *     range: from -180 to 180 degrees.
         * @param horizontalAccuracy The radius of uncertainty for the location, in [Length] unit.
         *     Must be non-negative value.
         * @param verticalAccuracy The validity of the altitude values, and their estimated
         *     uncertainty, in [Length] unit. Must be non-negative value.
         * @param altitude An altitude of a location represented as a float, in [Length] unit above
         *     sea level.
         * @param skipValidation Boolean flag to skip validation of record values.
         * @see ExerciseRoute
         */
        private Location(
                @NonNull Instant time,
                @FloatRange(from = MIN_LATITUDE, to = MAX_LATITUDE) double latitude,
                @FloatRange(from = MIN_LONGITUDE, to = MAX_LONGITUDE) double longitude,
                @Nullable Length horizontalAccuracy,
                @Nullable Length verticalAccuracy,
                @Nullable Length altitude,
                boolean skipValidation) {
            Objects.requireNonNull(time);

            if (!skipValidation) {
                ValidationUtils.requireInRange(latitude, MIN_LATITUDE, MAX_LATITUDE, "Latitude");
                ValidationUtils.requireInRange(
                        longitude, MIN_LONGITUDE, MAX_LONGITUDE, "Longitude");

                if (horizontalAccuracy != null) {
                    ValidationUtils.requireNonNegative(
                            horizontalAccuracy.getInMeters(), "Horizontal accuracy");
                }

                if (verticalAccuracy != null && verticalAccuracy.getInMeters() < 0) {
                    ValidationUtils.requireNonNegative(
                            verticalAccuracy.getInMeters(), "Vertical accuracy");
                }
            }

            mTime = time;
            mLatitude = latitude;
            mLongitude = longitude;
            mHorizontalAccuracy = horizontalAccuracy;
            mVerticalAccuracy = verticalAccuracy;
            mAltitude = altitude;
        }

        /** Returns time when this location has been recorded */
        @NonNull
        public Instant getTime() {
            return mTime;
        }

        /** Returns longitude of this location */
        @FloatRange(from = -180.0, to = 180.0)
        public double getLongitude() {
            return mLongitude;
        }

        /** Returns latitude of this location */
        @FloatRange(from = -90.0, to = 90.0)
        public double getLatitude() {
            return mLatitude;
        }

        /**
         * Returns horizontal accuracy of this location time point. Returns null if no horizontal
         * accuracy was specified.
         */
        @Nullable
        public Length getHorizontalAccuracy() {
            return mHorizontalAccuracy;
        }

        /**
         * Returns vertical accuracy of this location time point. Returns null if no vertical
         * accuracy was specified.
         */
        @Nullable
        public Length getVerticalAccuracy() {
            return mVerticalAccuracy;
        }

        /**
         * Returns altitude of this location time point. Returns null if no altitude was specified.
         */
        @Nullable
        public Length getAltitude() {
            return mAltitude;
        }

        /** @hide */
        public ExerciseRouteInternal.LocationInternal toExerciseRouteLocationInternal() {
            ExerciseRouteInternal.LocationInternal locationInternal =
                    new ExerciseRouteInternal.LocationInternal()
                            .setTime(getTime().toEpochMilli())
                            .setLatitude(getLatitude())
                            .setLongitude(getLongitude());

            if (getHorizontalAccuracy() != null) {
                locationInternal.setHorizontalAccuracy(getHorizontalAccuracy().getInMeters());
            }

            if (getVerticalAccuracy() != null) {
                locationInternal.setVerticalAccuracy(getVerticalAccuracy().getInMeters());
            }

            if (getAltitude() != null) {
                locationInternal.setAltitude(getAltitude().getInMeters());
            }
            return locationInternal;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Location)) return false;
            Location that = (Location) o;
            return Objects.equals(getAltitude(), that.getAltitude())
                    && getTime().equals(that.getTime())
                    && (getLatitude() == that.getLatitude())
                    && (getLongitude() == that.getLongitude())
                    && Objects.equals(getHorizontalAccuracy(), that.getHorizontalAccuracy())
                    && Objects.equals(getVerticalAccuracy(), that.getVerticalAccuracy());
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

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeLong(mTime.toEpochMilli());
            dest.writeDouble(mLatitude);
            dest.writeDouble(mLongitude);
            dest.writeBoolean(mHorizontalAccuracy != null);
            if (mHorizontalAccuracy != null) {
                dest.writeDouble(mHorizontalAccuracy.getInMeters());
            }
            dest.writeBoolean(mVerticalAccuracy != null);
            if (mVerticalAccuracy != null) {
                dest.writeDouble(mVerticalAccuracy.getInMeters());
            }
            dest.writeBoolean(mAltitude != null);
            if (mAltitude != null) {
                dest.writeDouble(mAltitude.getInMeters());
            }
        }

        /** Builder class for {@link Location} */
        public static final class Builder {
            @NonNull private final Instant mTime;

            @FloatRange(from = MIN_LATITUDE, to = MAX_LATITUDE)
            private final double mLatitude;

            @FloatRange(from = MIN_LONGITUDE, to = MAX_LONGITUDE)
            private final double mLongitude;

            @Nullable private Length mHorizontalAccuracy;
            @Nullable private Length mVerticalAccuracy;
            @Nullable private Length mAltitude;

            /** Sets time, longitude and latitude to the point. */
            public Builder(
                    @NonNull Instant time,
                    @FloatRange(from = -90.0, to = 90.0) double latitude,
                    @FloatRange(from = -180.0, to = 180.0) double longitude) {
                Objects.requireNonNull(time);
                mTime = time;
                mLatitude = latitude;
                mLongitude = longitude;
            }

            /** Sets horizontal accuracy to the point. */
            @NonNull
            public Builder setHorizontalAccuracy(@NonNull Length horizontalAccuracy) {
                Objects.requireNonNull(horizontalAccuracy);
                mHorizontalAccuracy = horizontalAccuracy;
                return this;
            }

            /** Sets vertical accuracy to the point. */
            @NonNull
            public Builder setVerticalAccuracy(@NonNull Length verticalAccuracy) {
                Objects.requireNonNull(verticalAccuracy);
                mVerticalAccuracy = verticalAccuracy;
                return this;
            }

            /** Sets altitude to the point. */
            @NonNull
            public Builder setAltitude(@NonNull Length altitude) {
                Objects.requireNonNull(altitude);
                mAltitude = altitude;
                return this;
            }

            /**
             * @return Object of {@link Location} without validating the values.
             * @hide
             */
            @NonNull
            public Location buildWithoutValidation() {
                return new Location(
                        mTime,
                        mLatitude,
                        mLongitude,
                        mHorizontalAccuracy,
                        mVerticalAccuracy,
                        mAltitude,
                        true);
            }

            /** Builds {@link Location} */
            @NonNull
            public Location build() {
                return new Location(
                        mTime,
                        mLatitude,
                        mLongitude,
                        mHorizontalAccuracy,
                        mVerticalAccuracy,
                        mAltitude,
                        false);
            }
        }
    }
}
