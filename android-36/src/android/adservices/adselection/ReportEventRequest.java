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

package android.adservices.adselection;

import static android.adservices.adselection.AdSelectionOutcome.UNSET_AD_SELECTION_ID;
import static android.adservices.adselection.AdSelectionOutcome.UNSET_AD_SELECTION_ID_MESSAGE;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.view.InputEvent;

import com.android.adservices.flags.Flags;
import com.android.internal.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Request object wrapping the required arguments needed to report an ad event.
 */
public class ReportEventRequest {
    /** This is used to represent seller as the destination for report event API */
    public static final int FLAG_REPORTING_DESTINATION_SELLER = 1 << 0;

    /** This is used to represent buyer as the destination for report event API. */
    public static final int FLAG_REPORTING_DESTINATION_BUYER = 1 << 1;

    /** This is used to component seller as the destination for report event API */
    @FlaggedApi(Flags.FLAG_FLEDGE_ENABLE_REPORT_EVENT_FOR_COMPONENT_SELLER)
    public static final int FLAG_REPORTING_DESTINATION_COMPONENT_SELLER = 1 << 2;

    private static final int UNSET_REPORTING_DESTINATIONS = 0;
    private static final String UNSET_REPORTING_DESTINATIONS_MESSAGE =
            "Reporting destinations bitfield not set.";
    private static final String INVALID_REPORTING_DESTINATIONS_MESSAGE =
            "Invalid reporting destinations bitfield!";

    /** @hide */
    public static final long REPORT_EVENT_MAX_INTERACTION_DATA_SIZE_B = 64 * 1024; // 64 KB

    private static final String EVENT_DATA_SIZE_MAX_EXCEEDED =
            "Event data should not exceed " + REPORT_EVENT_MAX_INTERACTION_DATA_SIZE_B + " bytes";

    private final long mAdSelectionId;
    @NonNull private final String mEventKey;
    @Nullable private final InputEvent mInputEvent;
    @NonNull private final String mEventData;

    @ReportingDestination
    private final int mReportingDestinations; // buyer, seller, component seller or all

    private ReportEventRequest(@NonNull Builder builder) {
        Objects.requireNonNull(builder);

        Preconditions.checkArgument(
                builder.mAdSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);
        Preconditions.checkArgument(
                builder.mReportingDestinations != UNSET_REPORTING_DESTINATIONS,
                UNSET_REPORTING_DESTINATIONS_MESSAGE);
        Preconditions.checkArgument(
                isValidDestination(builder.mReportingDestinations),
                INVALID_REPORTING_DESTINATIONS_MESSAGE);
        Preconditions.checkArgument(
                builder.mEventData.getBytes(StandardCharsets.UTF_8).length
                        <= REPORT_EVENT_MAX_INTERACTION_DATA_SIZE_B,
                EVENT_DATA_SIZE_MAX_EXCEEDED);

        this.mAdSelectionId = builder.mAdSelectionId;
        this.mEventKey = builder.mEventKey;
        this.mInputEvent = builder.mInputEvent;
        this.mEventData = builder.mEventData;
        this.mReportingDestinations = builder.mReportingDestinations;
    }

    /**
     * Returns the adSelectionId, the primary identifier of an ad selection process.
     */
    public long getAdSelectionId() {
        return mAdSelectionId;
    }

    /**
     * Returns the event key, the type of ad event to be reported.
     *
     * <p>This field will be used to fetch the {@code reportingUri} associated with the {@code
     * eventKey} registered in {@code registerAdBeacon} after ad selection.
     *
     * <p>This field should be an exact match to the {@code eventKey} registered in {@code
     * registerAdBeacon}. Specific details about {@code registerAdBeacon} can be found at the
     * documentation of {@link AdSelectionManager#reportImpression}
     *
     * <p>The event key (when inspecting its byte array with {@link String#getBytes()}) in {@code
     * UTF-8} format should not exceed 40 bytes. Any key exceeding this limit will not be registered
     * during the {@code registerAdBeacon} call.
     */
    @NonNull
    public String getKey() {
        return mEventKey;
    }

    /**
     * Returns the input event associated with the user interaction.
     *
     * <p>This field is either {@code null}, representing a <em>view</em> event, or has an {@link
     * InputEvent} object, representing a <em>click</em> event.
     */
    @Nullable
    public InputEvent getInputEvent() {
        return mInputEvent;
    }

    /**
     * Returns the ad event data.
     *
     * <p>After ad selection, this data is generated by the caller. The caller can then call {@link
     * AdSelectionManager#reportEvent}. This data will be attached in a POST request to the {@code
     * reportingUri} registered in {@code registerAdBeacon}.
     *
     * <p>The size of {@link String#getBytes()} in {@code UTF-8} format should be below 64KB.
     */
    @NonNull
    public String getData() {
        return mEventData;
    }

    /**
     * Returns the bitfield of reporting destinations to report to (buyer, seller, component seller
     * or any of the combination of them).
     *
     * <p>To create this bitfield, place an {@code |} bitwise operator between each {@code
     * reportingDestination} to be reported to. For example to only report to buyer, set the
     * reportingDestinations field to {@link #FLAG_REPORTING_DESTINATION_BUYER} To only report to
     * seller, set the reportingDestinations field to {@link #FLAG_REPORTING_DESTINATION_SELLER} To
     * report to buyers and sellers, set the reportingDestinations field to {@link
     * #FLAG_REPORTING_DESTINATION_BUYER} | {@link #FLAG_REPORTING_DESTINATION_SELLER}. To report to
     * buyer, seller and component seller, set the reportingDestinations field to {@link
     * #FLAG_REPORTING_DESTINATION_BUYER} | {@link #FLAG_REPORTING_DESTINATION_SELLER} | {@link
     * #FLAG_REPORTING_DESTINATION_COMPONENT_SELLER}.
     */
    @ReportingDestination
    public int getReportingDestinations() {
        return mReportingDestinations;
    }

    /** @hide */
    @IntDef(
            flag = true,
            prefix = {"FLAG_REPORTING_DESTINATION"},
            value = {
                FLAG_REPORTING_DESTINATION_SELLER,
                FLAG_REPORTING_DESTINATION_BUYER,
                FLAG_REPORTING_DESTINATION_COMPONENT_SELLER
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ReportingDestination {}

    private static boolean isValidDestination(@ReportingDestination int reportingDestinations) {
        return 0 < reportingDestinations
                && reportingDestinations
                        <= (FLAG_REPORTING_DESTINATION_SELLER
                                | FLAG_REPORTING_DESTINATION_BUYER
                                | FLAG_REPORTING_DESTINATION_COMPONENT_SELLER);
    }

    /** Builder for {@link ReportEventRequest} objects. */
    public static final class Builder {

        private long mAdSelectionId;
        @NonNull private String mEventKey;
        @Nullable private InputEvent mInputEvent;
        @NonNull private String mEventData;
        @ReportingDestination private int mReportingDestinations; // buyer, seller, or both

        public Builder(
                long adSelectionId,
                @NonNull String eventKey,
                @NonNull String eventData,
                @ReportingDestination int reportingDestinations) {
            Objects.requireNonNull(eventKey);
            Objects.requireNonNull(eventData);

            Preconditions.checkArgument(
                    adSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);
            Preconditions.checkArgument(
                    reportingDestinations != UNSET_REPORTING_DESTINATIONS,
                    UNSET_REPORTING_DESTINATIONS_MESSAGE);
            Preconditions.checkArgument(
                    isValidDestination(reportingDestinations),
                    INVALID_REPORTING_DESTINATIONS_MESSAGE);
            Preconditions.checkArgument(
                    eventData.getBytes(StandardCharsets.UTF_8).length
                            <= REPORT_EVENT_MAX_INTERACTION_DATA_SIZE_B,
                    EVENT_DATA_SIZE_MAX_EXCEEDED);

            this.mAdSelectionId = adSelectionId;
            this.mEventKey = eventKey;
            this.mEventData = eventData;
            this.mReportingDestinations = reportingDestinations;
        }

        /**
         * Sets the ad selection ID with which the rendered ad's events are associated.
         *
         * <p>See {@link #getAdSelectionId()} for more information.
         */
        @NonNull
        public Builder setAdSelectionId(long adSelectionId) {
            Preconditions.checkArgument(
                    adSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);
            mAdSelectionId = adSelectionId;
            return this;
        }

        /**
         * Sets the event key, the type of ad event to be reported.
         *
         * <p>See {@link #getKey()} for more information.
         */
        @NonNull
        public Builder setKey(@NonNull String eventKey) {
            Objects.requireNonNull(eventKey);

            mEventKey = eventKey;
            return this;
        }

        /**
         * Sets the input event associated with the user interaction.
         *
         * <p>See {@link #getInputEvent()} for more information.
         */
        @NonNull
        public Builder setInputEvent(@Nullable InputEvent inputEvent) {
            mInputEvent = inputEvent;
            return this;
        }

        /**
         * Sets the ad event data.
         *
         * <p>See {@link #getData()} for more information.
         */
        @NonNull
        public Builder setData(@NonNull String eventData) {
            Objects.requireNonNull(eventData);

            mEventData = eventData;
            return this;
        }

        /**
         * Sets the bitfield of reporting destinations to report to (buyer, seller, or both).
         *
         * <p>See {@link #getReportingDestinations()} for more information.
         */
        @NonNull
        public Builder setReportingDestinations(@ReportingDestination int reportingDestinations) {
            Preconditions.checkArgument(
                    isValidDestination(reportingDestinations),
                    INVALID_REPORTING_DESTINATIONS_MESSAGE);

            mReportingDestinations = reportingDestinations;
            return this;
        }

        /** Builds the {@link ReportEventRequest} object. */
        @NonNull
        public ReportEventRequest build() {
            return new ReportEventRequest(this);
        }
    }
}
