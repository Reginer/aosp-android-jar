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
import android.annotation.Nullable;

import androidx.annotation.NonNull;

import com.android.adservices.flags.Flags;
import com.android.internal.util.Preconditions;

import java.util.Arrays;
import java.util.Objects;

/** Represents ad selection data collected from device for ad selection. */
public final class GetAdSelectionDataOutcome {
    private final long mAdSelectionId;
    @Nullable private final byte[] mAdSelectionData;

    private GetAdSelectionDataOutcome(long adSelectionId, @Nullable byte[] adSelectionData) {
        this.mAdSelectionId = adSelectionId;
        this.mAdSelectionData = adSelectionData;
    }

    /**
     * Returns the adSelectionId that identifies the AdSelection.
     *
     * @deprecated Use the {@link #getAdSelectionDataId()} instead.
     */
    public long getAdSelectionId() {
        return mAdSelectionId;
    }

    /** Returns the id that uniquely identifies this GetAdSelectionData payload. */
    @FlaggedApi(Flags.FLAG_FLEDGE_AUCTION_SERVER_GET_AD_SELECTION_DATA_ID_ENABLED)
    public long getAdSelectionDataId() {
        return mAdSelectionId;
    }

    /** Returns the adSelectionData that is collected from device. */
    @Nullable
    public byte[] getAdSelectionData() {
        if (Objects.isNull(mAdSelectionData)) {
            return null;
        } else {
            return Arrays.copyOf(mAdSelectionData, mAdSelectionData.length);
        }
    }

    /**
     * Builder for {@link GetAdSelectionDataOutcome} objects.
     *
     * @hide
     */
    public static final class Builder {
        private long mAdSelectionId;
        @Nullable private byte[] mAdSelectionData;

        public Builder() {}

        /** Sets the adSelectionId. */
        @NonNull
        public GetAdSelectionDataOutcome.Builder setAdSelectionId(long adSelectionId) {
            this.mAdSelectionId = adSelectionId;
            return this;
        }

        /** Sets the adSelectionData. */
        @NonNull
        public GetAdSelectionDataOutcome.Builder setAdSelectionData(
                @Nullable byte[] adSelectionData) {
            if (!Objects.isNull(adSelectionData)) {
                this.mAdSelectionData = Arrays.copyOf(adSelectionData, adSelectionData.length);
            } else {
                this.mAdSelectionData = null;
            }
            return this;
        }

        /**
         * Builds a {@link GetAdSelectionDataOutcome} instance.
         *
         * @throws IllegalArgumentException if the adSelectionId is not set
         */
        @NonNull
        public GetAdSelectionDataOutcome build() {
            Preconditions.checkArgument(
                    mAdSelectionId != UNSET_AD_SELECTION_ID, UNSET_AD_SELECTION_ID_MESSAGE);

            return new GetAdSelectionDataOutcome(mAdSelectionId, mAdSelectionData);
        }
    }
}
