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

package android.nearby;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;

import com.android.internal.util.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Filter for scanning a nearby presence device.
 *
 * @hide
 */
@SystemApi
public final class PresenceScanFilter extends ScanFilter implements Parcelable {

    private final List<PublicCredential> mCredentials;
    private final List<Integer> mPresenceActions;
    private final List<DataElement> mExtendedProperties;

    /**
     * A list of credentials to filter on.
     */
    @NonNull
    public List<PublicCredential> getCredentials() {
        return mCredentials;
    }

    /**
     * A list of presence actions for matching.
     */
    @NonNull
    public List<Integer> getPresenceActions() {
        return mPresenceActions;
    }

    /**
     * A bundle of extended properties for matching.
     */
    @NonNull
    public List<DataElement> getExtendedProperties() {
        return mExtendedProperties;
    }

    private PresenceScanFilter(int rssiThreshold, List<PublicCredential> credentials,
            List<Integer> presenceActions, List<DataElement> extendedProperties) {
        super(ScanRequest.SCAN_TYPE_NEARBY_PRESENCE, rssiThreshold);
        mCredentials = new ArrayList<>(credentials);
        mPresenceActions = new ArrayList<>(presenceActions);
        mExtendedProperties = new ArrayList<>(extendedProperties);
    }

    private PresenceScanFilter(Parcel in) {
        super(ScanRequest.SCAN_TYPE_NEARBY_PRESENCE, in);
        mCredentials = new ArrayList<>();
        if (in.readInt() != 0) {
            in.readParcelableList(mCredentials, PublicCredential.class.getClassLoader(),
                    PublicCredential.class);
        }
        mPresenceActions = new ArrayList<>();
        if (in.readInt() != 0) {
            in.readList(mPresenceActions, Integer.class.getClassLoader(), Integer.class);
        }
        mExtendedProperties = new ArrayList<>();
        if (in.readInt() != 0) {
            in.readParcelableList(mExtendedProperties, DataElement.class.getClassLoader(),
                    DataElement.class);
        }
    }

    @NonNull
    public static final Creator<PresenceScanFilter> CREATOR = new Creator<PresenceScanFilter>() {
        @Override
        public PresenceScanFilter createFromParcel(Parcel in) {
            // Skip Scan Filter type as it's used for parent class.
            in.readInt();
            return createFromParcelBody(in);
        }

        @Override
        public PresenceScanFilter[] newArray(int size) {
            return new PresenceScanFilter[size];
        }
    };

    /**
     * Create a {@link PresenceScanFilter} from the parcel body. Scan Filter type is skipped.
     */
    static PresenceScanFilter createFromParcelBody(Parcel in) {
        return new PresenceScanFilter(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mCredentials.size());
        if (!mCredentials.isEmpty()) {
            dest.writeParcelableList(mCredentials, 0);
        }
        dest.writeInt(mPresenceActions.size());
        if (!mPresenceActions.isEmpty()) {
            dest.writeList(mPresenceActions);
        }
        dest.writeInt(mExtendedProperties.size());
        if (!mExtendedProperties.isEmpty()) {
            dest.writeParcelableList(mExtendedProperties, 0);
        }
    }

    /**
     * Builder for {@link PresenceScanFilter}.
     */
    public static final class Builder {
        private int mMaxPathLoss;
        private final Set<PublicCredential> mCredentials;
        private final Set<Integer> mPresenceIdentities;
        private final Set<Integer> mPresenceActions;
        private final List<DataElement> mExtendedProperties;

        public Builder() {
            mMaxPathLoss = 127;
            mCredentials = new ArraySet<>();
            mPresenceIdentities = new ArraySet<>();
            mPresenceActions = new ArraySet<>();
            mExtendedProperties = new ArrayList<>();
        }

        /**
         * Sets the max path loss (in dBm) for the scan request. The path loss is the attenuation
         * of radio energy between sender and receiver. Path loss here is defined as (TxPower -
         * Rssi).
         */
        @NonNull
        public Builder setMaxPathLoss(@IntRange(from = 0, to = 127) int maxPathLoss) {
            mMaxPathLoss = maxPathLoss;
            return this;
        }

        /**
         * Adds a credential the scan filter is expected to match.
         */

        @NonNull
        public Builder addCredential(@NonNull PublicCredential credential) {
            Objects.requireNonNull(credential);
            mCredentials.add(credential);
            return this;
        }

        /**
         * Adds a presence action for filtering, which is an action the discoverer could take
         * when it receives the broadcast of a presence device.
         */
        @NonNull
        public Builder addPresenceAction(@IntRange(from = 1, to = 255) int action) {
            mPresenceActions.add(action);
            return this;
        }

        /**
         * Add an extended property for scan filtering.
         */
        @NonNull
        public Builder addExtendedProperty(@NonNull DataElement dataElement) {
            Objects.requireNonNull(dataElement);
            mExtendedProperties.add(dataElement);
            return this;
        }

        /**
         * Builds the scan filter.
         */
        @NonNull
        public PresenceScanFilter build() {
            Preconditions.checkState(!mCredentials.isEmpty(), "credentials cannot be empty");
            return new PresenceScanFilter(mMaxPathLoss,
                    new ArrayList<>(mCredentials),
                    new ArrayList<>(mPresenceActions),
                    mExtendedProperties);
        }
    }
}
