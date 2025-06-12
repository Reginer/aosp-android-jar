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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.InputEvent;

import com.android.adservices.AdServicesParcelableUtil;
import com.android.internal.util.Preconditions;

import java.util.Objects;

/**
 * Input object wrapping the required arguments needed to report an interaction.
 *
 * @hide
 */
public final class ReportInteractionInput implements Parcelable {

    private static final int UNSET_REPORTING_DESTINATIONS = 0;
    private static final String UNSET_REPORTING_DESTINATIONS_MESSAGE =
            "Reporting Destinations bitfield not set.";

    private final long mAdSelectionId;
    @NonNull private final String mInteractionKey;
    @NonNull private final String mInteractionData;
    @NonNull private final String mCallerPackageName;
    private final int mReportingDestinations; // buyer, seller, or both
    @Nullable private final InputEvent mInputEvent;
    @Nullable private final String mAdId;
    @Nullable private final String mCallerSdkName;

    @NonNull
    public static final Creator<ReportInteractionInput> CREATOR =
            new Creator<ReportInteractionInput>() {
                @Override
                public ReportInteractionInput createFromParcel(@NonNull Parcel in) {
                    Objects.requireNonNull(in);

                    return new ReportInteractionInput(in);
                }

                @Override
                public ReportInteractionInput[] newArray(int size) {
                    return new ReportInteractionInput[size];
                }
            };

    private ReportInteractionInput(
            long adSelectionId,
            @NonNull String interactionKey,
            @NonNull String interactionData,
            @NonNull String callerPackageName,
            int reportingDestinations,
            @Nullable InputEvent inputEvent,
            @Nullable String adId,
            @Nullable String callerSdkName) {
        Objects.requireNonNull(interactionKey);
        Objects.requireNonNull(interactionData);
        Objects.requireNonNull(callerPackageName);

        this.mAdSelectionId = adSelectionId;
        this.mInteractionKey = interactionKey;
        this.mInteractionData = interactionData;
        this.mCallerPackageName = callerPackageName;
        this.mReportingDestinations = reportingDestinations;
        this.mInputEvent = inputEvent;
        this.mAdId = adId;
        this.mCallerSdkName = callerSdkName;
    }

    private ReportInteractionInput(@NonNull Parcel in) {
        this.mAdSelectionId = in.readLong();
        this.mInteractionKey = in.readString();
        this.mInteractionData = in.readString();
        this.mCallerPackageName = in.readString();
        this.mReportingDestinations = in.readInt();
        this.mInputEvent =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, InputEvent.CREATOR::createFromParcel);
        this.mAdId =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, (sourceParcel -> in.readString()));
        this.mCallerSdkName =
                AdServicesParcelableUtil.readNullableFromParcel(
                        in, (sourceParcel -> in.readString()));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Objects.requireNonNull(dest);
        dest.writeLong(mAdSelectionId);
        dest.writeString(mInteractionKey);
        dest.writeString(mInteractionData);
        dest.writeString(mCallerPackageName);
        dest.writeInt(mReportingDestinations);
        AdServicesParcelableUtil.writeNullableToParcel(
                dest,
                mInputEvent,
                (targetParcel, sourceInputEvent) ->
                        sourceInputEvent.writeToParcel(targetParcel, flags));
        AdServicesParcelableUtil.writeNullableToParcel(dest, mAdId, Parcel::writeString);
        AdServicesParcelableUtil.writeNullableToParcel(dest, mCallerSdkName, Parcel::writeString);
    }

    /** Returns the adSelectionId, the primary identifier of an ad selection process. */
    public long getAdSelectionId() {
        return mAdSelectionId;
    }

    /**
     * Returns the interaction key, the type of interaction to be reported.
     *
     * <p>This will be used to fetch the {@code interactionReportingUri} associated with the {@code
     * interactionKey} registered in {@code registerAdBeacon} after ad selection.
     */
    @NonNull
    public String getInteractionKey() {
        return mInteractionKey;
    }

    /**
     * Returns the interaction data.
     *
     * <p>After ad selection, this data is generated by the caller, and will be attached in a POST
     * request to the {@code interactionReportingUri} registered in {@code registerAdBeacon}.
     */
    @NonNull
    public String getInteractionData() {
        return mInteractionData;
    }

    /** Returns the caller package name */
    @NonNull
    public String getCallerPackageName() {
        return mCallerPackageName;
    }

    /** Returns the bitfield of reporting destinations to report to (buyer, seller, or both) */
    public int getReportingDestinations() {
        return mReportingDestinations;
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
     * Returns the {@code AdId} to enable event-level debug reporting.
     *
     * <p>This field is either set and non-{@code null}, representing a valid and enabled {@code
     * AdId} or {@code null}, representing an invalid or disabled {@code AdId}.
     */
    @Nullable
    public String getAdId() {
        return mAdId;
    }

    /** Returns the caller's sdk name. */
    @Nullable
    public String getCallerSdkName() {
        return mCallerSdkName;
    }

    /**
     * Builder for {@link ReportInteractionInput} objects.
     *
     * @hide
     */
    public static final class Builder {
        private long mAdSelectionId = UNSET_AD_SELECTION_ID;
        @Nullable private String mInteractionKey;
        @Nullable private String mInteractionData;
        @Nullable private String mCallerPackageName;
        @Nullable private String mCallerSdkName;
        private int mReportingDestinations = UNSET_REPORTING_DESTINATIONS;
        @Nullable private InputEvent mInputEvent;
        @Nullable private String mAdId;

        public Builder() {}

        /** Sets the adSelectionId. */
        @NonNull
        public ReportInteractionInput.Builder setAdSelectionId(long adSelectionId) {
            mAdSelectionId = adSelectionId;
            return this;
        }

        /** Sets the interactionKey. */
        @NonNull
        public ReportInteractionInput.Builder setInteractionKey(@NonNull String interactionKey) {
            Objects.requireNonNull(interactionKey);

            mInteractionKey = interactionKey;
            return this;
        }

        /** Sets the interactionData. */
        @NonNull
        public ReportInteractionInput.Builder setInteractionData(@NonNull String interactionData) {
            Objects.requireNonNull(interactionData);

            mInteractionData = interactionData;
            return this;
        }

        /** Sets the caller's package name. */
        @NonNull
        public ReportInteractionInput.Builder setCallerPackageName(
                @NonNull String callerPackageName) {
            Objects.requireNonNull(callerPackageName);

            mCallerPackageName = callerPackageName;
            return this;
        }

        /** Sets the bitfield of reporting destinations. */
        @NonNull
        public ReportInteractionInput.Builder setReportingDestinations(int reportingDestinations) {
            Preconditions.checkArgument(
                    reportingDestinations != UNSET_REPORTING_DESTINATIONS,
                    UNSET_REPORTING_DESTINATIONS_MESSAGE);

            mReportingDestinations = reportingDestinations;
            return this;
        }

        /** Sets the input event associated with the user interaction. */
        @NonNull
        public ReportInteractionInput.Builder setInputEvent(@Nullable InputEvent inputEvent) {
            mInputEvent = inputEvent;
            return this;
        }

        /** Sets the {@code AdId}. */
        @NonNull
        public ReportInteractionInput.Builder setAdId(@Nullable String adId) {
            mAdId = adId;
            return this;
        }

        /** Sets the caller's sdk name. */
        @NonNull
        public ReportInteractionInput.Builder setCallerSdkName(@Nullable String callerSdkName) {
            mCallerSdkName = callerSdkName;
            return this;
        }

        /** Builds a {@link ReportInteractionInput} instance. */
        @NonNull
        public ReportInteractionInput build() {
            Objects.requireNonNull(mInteractionKey);
            Objects.requireNonNull(mInteractionData);
            Objects.requireNonNull(mCallerPackageName);

            Preconditions.checkArgument(
                    mAdSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);
            Preconditions.checkArgument(
                    mReportingDestinations != UNSET_REPORTING_DESTINATIONS,
                    UNSET_REPORTING_DESTINATIONS_MESSAGE);

            return new ReportInteractionInput(
                    mAdSelectionId,
                    mInteractionKey,
                    mInteractionData,
                    mCallerPackageName,
                    mReportingDestinations,
                    mInputEvent,
                    mAdId,
                    mCallerSdkName);
        }
    }
}
