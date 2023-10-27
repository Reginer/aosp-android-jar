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

import com.android.internal.util.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Request for Nearby Presence Broadcast.
 *
 * @hide
 */
@SystemApi
public final class PresenceBroadcastRequest extends BroadcastRequest implements Parcelable {
    private final byte[] mSalt;
    private final List<Integer> mActions;
    private final PrivateCredential mCredential;
    private final List<DataElement> mExtendedProperties;

    private PresenceBroadcastRequest(@BroadcastVersion int version, int txPower,
            List<Integer> mediums, byte[] salt, List<Integer> actions,
            PrivateCredential credential, List<DataElement> extendedProperties) {
        super(BROADCAST_TYPE_NEARBY_PRESENCE, version, txPower, mediums);
        mSalt = salt;
        mActions = actions;
        mCredential = credential;
        mExtendedProperties = extendedProperties;
    }

    private PresenceBroadcastRequest(Parcel in) {
        super(BROADCAST_TYPE_NEARBY_PRESENCE, in);
        mSalt = new byte[in.readInt()];
        in.readByteArray(mSalt);

        mActions = new ArrayList<>();
        in.readList(mActions, Integer.class.getClassLoader(), Integer.class);
        mCredential = in.readParcelable(PrivateCredential.class.getClassLoader(),
                PrivateCredential.class);
        mExtendedProperties = new ArrayList<>();
        in.readList(mExtendedProperties, DataElement.class.getClassLoader(), DataElement.class);
    }

    @NonNull
    public static final Creator<PresenceBroadcastRequest> CREATOR =
            new Creator<PresenceBroadcastRequest>() {
                @Override
                public PresenceBroadcastRequest createFromParcel(Parcel in) {
                    // Skip Broadcast request type - it's used by parent class.
                    in.readInt();
                    return createFromParcelBody(in);
                }

                @Override
                public PresenceBroadcastRequest[] newArray(int size) {
                    return new PresenceBroadcastRequest[size];
                }
            };

    static PresenceBroadcastRequest createFromParcelBody(Parcel in) {
        return new PresenceBroadcastRequest(in);
    }

    /**
     * Returns the salt associated with this broadcast request.
     */
    @NonNull
    public byte[] getSalt() {
        return mSalt;
    }

    /**
     * Returns actions associated with this broadcast request.
     */
    @NonNull
    public List<Integer> getActions() {
        return mActions;
    }

    /**
     * Returns the private credential associated with this broadcast request.
     */
    @NonNull
    public PrivateCredential getCredential() {
        return mCredential;
    }

    /**
     * Returns extended property information associated with this broadcast request.
     */
    @NonNull
    public List<DataElement> getExtendedProperties() {
        return mExtendedProperties;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mSalt.length);
        dest.writeByteArray(mSalt);
        dest.writeList(mActions);
        dest.writeParcelable(mCredential, /** parcelableFlags= */0);
        dest.writeList(mExtendedProperties);
    }

    /**
     * Builder for {@link PresenceBroadcastRequest}.
     */
    public static final class Builder {
        private final List<Integer> mMediums;
        private final List<Integer> mActions;
        private final List<DataElement> mExtendedProperties;
        private final byte[] mSalt;
        private final PrivateCredential mCredential;

        private int mVersion;
        private int mTxPower;

        public Builder(@NonNull List<Integer> mediums, @NonNull byte[] salt,
                @NonNull PrivateCredential credential) {
            Preconditions.checkState(!mediums.isEmpty(), "mediums cannot be empty");
            Preconditions.checkState(salt != null && salt.length > 0, "salt cannot be empty");

            mVersion = PRESENCE_VERSION_V0;
            mTxPower = UNKNOWN_TX_POWER;
            mCredential = credential;
            mActions = new ArrayList<>();
            mExtendedProperties = new ArrayList<>();

            mSalt = salt;
            mMediums = mediums;
        }

        /**
         * Sets the version for this request.
         */
        @NonNull
        public Builder setVersion(@BroadcastVersion int version) {
            mVersion = version;
            return this;
        }

        /**
         * Sets the calibrated tx power level in dBm for this request. The tx power level should
         * be between -127 dBm and 126 dBm.
         */
        @NonNull
        public Builder setTxPower(@IntRange(from = -127, to = 126) int txPower) {
            mTxPower = txPower;
            return this;
        }

        /**
         * Adds an action for the presence broadcast request.
         */
        @NonNull
        public Builder addAction(@IntRange(from = 1, to = 255) int action) {
            mActions.add(action);
            return this;
        }

        /**
         * Adds an extended property for the presence broadcast request.
         */
        @NonNull
        public Builder addExtendedProperty(@NonNull DataElement dataElement) {
            Objects.requireNonNull(dataElement);
            mExtendedProperties.add(dataElement);
            return this;
        }

        /**
         * Builds a {@link PresenceBroadcastRequest}.
         */
        @NonNull
        public PresenceBroadcastRequest build() {
            return new PresenceBroadcastRequest(mVersion, mTxPower, mMediums, mSalt, mActions,
                    mCredential, mExtendedProperties);
        }
    }
}
