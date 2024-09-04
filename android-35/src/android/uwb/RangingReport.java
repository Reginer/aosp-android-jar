/*
 * Copyright 2020 The Android Open Source Project
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

package android.uwb;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.uwb.util.PersistableBundleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class contains the UWB ranging data
 *
 * @hide
 */
@SystemApi
public final class RangingReport implements Parcelable {
    private final List<RangingMeasurement> mRangingMeasurements;
    private final PersistableBundle mRangingReportMetadata;

    private RangingReport(@NonNull List<RangingMeasurement> rangingMeasurements,
            PersistableBundle rangingReportMetadata) {
        mRangingMeasurements = rangingMeasurements;
        mRangingReportMetadata = rangingReportMetadata;
    }

    /**
     * Get a {@link List} of {@link RangingMeasurement} objects in the last measurement interval
     * <p>The underlying UWB adapter may choose to do multiple measurements in each ranging
     * interval.
     *
     * <p>The entries in the {@link List} are ordered in ascending order based on
     * {@link RangingMeasurement#getElapsedRealtimeNanos()}
     *
     * @return a {@link List} of {@link RangingMeasurement} objects
     */
    @NonNull
    public List<RangingMeasurement> getMeasurements() {
        return mRangingMeasurements;
    }

    /**
     * Gets ranging report metadata passed by vendor
     *
     * @return vendor data for ranging report
     */
    @NonNull
    public PersistableBundle getRangingReportMetadata() {
        return mRangingReportMetadata;
    }

    /**
     * @hide
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof RangingReport) {
            RangingReport other = (RangingReport) obj;
            return mRangingMeasurements.equals(other.getMeasurements())
                    && PersistableBundleUtils.isEqual(mRangingReportMetadata,
                    other.getRangingReportMetadata());
        }
        return false;
    }

    /**
     * @hide
     */
    @Override
    public int hashCode() {
        return Objects.hash(mRangingMeasurements, PersistableBundleUtils
                .getHashCode(mRangingReportMetadata));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeTypedList(mRangingMeasurements);
        dest.writePersistableBundle(mRangingReportMetadata);
    }

    public static final @android.annotation.NonNull Creator<RangingReport> CREATOR =
            new Creator<RangingReport>() {
                @Override
                public RangingReport createFromParcel(Parcel in) {
                    Builder builder = new Builder();
                    builder.addMeasurements(in.createTypedArrayList(RangingMeasurement.CREATOR));
                    PersistableBundle metadata =
                            in.readPersistableBundle(getClass().getClassLoader());
                    if (metadata != null) builder.addRangingReportMetadata(metadata);
                    return builder.build();
                }

                @Override
                public RangingReport[] newArray(int size) {
                    return new RangingReport[size];
                }
    };

    /** @hide **/
    @Override
    public String toString() {
        return "RangingReport["
                + "measurements: " + mRangingMeasurements
                + ", ranging report measurement: " + mRangingReportMetadata
                + "]";
    }

    /**
     * Builder for {@link RangingReport} object
     */
    public static final class Builder {
        List<RangingMeasurement> mMeasurements = new ArrayList<>();
        private PersistableBundle mRangingReportMetadata = new PersistableBundle();

        /**
         * Add a single {@link RangingMeasurement}
         *
         * @param rangingMeasurement a ranging measurement
         */
        @NonNull
        public Builder addMeasurement(@NonNull RangingMeasurement rangingMeasurement) {
            mMeasurements.add(rangingMeasurement);
            return this;
        }

        /**
         * Add a {@link List} of {@link RangingMeasurement}s
         *
         * @param rangingMeasurements {@link List} of {@link RangingMeasurement}s to add
         */
        @NonNull
        public Builder addMeasurements(@NonNull List<RangingMeasurement> rangingMeasurements) {
            mMeasurements.addAll(rangingMeasurements);
            return this;
        }

        /**
         * Add ranging report metadata
         *
         * @param rangingReportMetadata vendor data per ranging report
         *
         * @throws IllegalStateException if rangingReportMetadata is null
         */
        @NonNull
        public Builder addRangingReportMetadata(@NonNull PersistableBundle rangingReportMetadata) {
            if (rangingReportMetadata == null) {
                throw new IllegalStateException("Expected non-null rangingReportMetadata");
            }
            mRangingReportMetadata = rangingReportMetadata;
            return this;
        }

        /**
         * Build the {@link RangingReport} object
         *
         * @throws IllegalStateException if measurements are not in monotonically increasing order
         */
        @NonNull
        public RangingReport build() {
            // Verify that all measurement timestamps are monotonically increasing
            RangingMeasurement prevMeasurement = null;
            for (int curIndex = 0; curIndex < mMeasurements.size(); curIndex++) {
                RangingMeasurement curMeasurement = mMeasurements.get(curIndex);
                if (prevMeasurement != null
                        && (prevMeasurement.getElapsedRealtimeNanos()
                                > curMeasurement.getElapsedRealtimeNanos())) {
                    throw new IllegalStateException(
                            "Timestamp (" + curMeasurement.getElapsedRealtimeNanos()
                            + ") at index " + curIndex + " is less than previous timestamp ("
                            + prevMeasurement.getElapsedRealtimeNanos() + ")");
                }
                prevMeasurement = curMeasurement;
            }
            return new RangingReport(mMeasurements, mRangingReportMetadata);
        }
    }
}

