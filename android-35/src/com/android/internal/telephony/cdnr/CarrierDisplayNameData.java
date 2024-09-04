/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.telephony.cdnr;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * A container of carrier display name.
 */
public class CarrierDisplayNameData implements Parcelable {
    /** Service provider name. */
    private final String mSpn;

    /** Data service provider name. */
    private final String mDataSpn;

    /** PLMN network name */
    private final String mPlmn;

    /** {@code True} if display service provider name is required. */
    private final boolean mShowSpn;

    /** {@code True} if display PLMN network name is required. */
    private final boolean mShowPlmn;

    private CarrierDisplayNameData(String spn, String dataSpn, boolean showSpn, String plmn,
            boolean showPlmn) {
        this.mSpn = spn;
        this.mDataSpn = dataSpn;
        this.mShowSpn = showSpn;
        this.mPlmn = plmn;
        this.mShowPlmn = showPlmn;
    }

    /**
     * Get the service provider name.
     * @return service provider name.
     */
    public String getSpn() {
        return mSpn;
    }

    /**
     * Get the service provider name of data provider.
     * @return service provider name of data provider.
     */
    public String getDataSpn() {
        return mDataSpn;
    }

    /**
     * Get the PLMN network name.
     * @return PLMN network name.
     */
    public String getPlmn() {
        return mPlmn;
    }

    /**
     * Whether the spn should be displayed.
     * @return
     */
    public boolean shouldShowSpn() {
        return mShowSpn;
    }

    /**
     * Whether the PLMN should be displayed.
     * @return
     */
    public boolean shouldShowPlmn() {
        return mShowPlmn;
    }

    @Override
    public String toString() {
        return String.format("{ spn = %s, dataSpn = %s, showSpn = %b, plmn = %s, showPlmn = %b",
                mSpn, mDataSpn, mShowSpn, mPlmn, mShowPlmn);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSpn);
        dest.writeString(mDataSpn);
        dest.writeString(mPlmn);
        dest.writeBoolean(mShowSpn);
        dest.writeBoolean(mShowPlmn);
    }

    private CarrierDisplayNameData(Parcel source) {
        mSpn = source.readString();
        mDataSpn = source.readString();
        mPlmn = source.readString();
        mShowSpn = source.readBoolean();
        mShowPlmn = source.readBoolean();
    }

    public static final Parcelable.Creator<CarrierDisplayNameData> CREATOR =
            new Creator<CarrierDisplayNameData>() {
                @Override
                public CarrierDisplayNameData createFromParcel(Parcel source) {
                    return new CarrierDisplayNameData(source);
                }

                @Override
                public CarrierDisplayNameData[] newArray(int size) {
                    return new CarrierDisplayNameData[size];
                }
            };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CarrierDisplayNameData that = (CarrierDisplayNameData) o;
        return mShowSpn == that.mShowSpn
                && mShowPlmn == that.mShowPlmn
                && Objects.equals(mSpn, that.mSpn)
                && Objects.equals(mDataSpn, that.mDataSpn)
                && Objects.equals(mPlmn, that.mPlmn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSpn, mDataSpn, mPlmn, mShowSpn, mShowPlmn);
    }

    /** Builder class for {@link com.android.internal.telephony.cdnr.CarrierDisplayNameData}. */
    public static final class Builder {
        private String mSpn;
        private String mDataSpn;
        private String mPlmn;
        private boolean mShowSpn;
        private boolean mShowPlmn;

        public Builder() {
            mSpn = null;
            mDataSpn = null;
            mPlmn = null;
            mShowPlmn = false;
            mShowSpn = false;
        }

        /** Create a {@link com.android.internal.telephony.cdnr.CarrierDisplayNameData} instance. */
        public CarrierDisplayNameData build() {
            return new CarrierDisplayNameData(mSpn, mDataSpn, mShowSpn, mPlmn, mShowPlmn);
        }

        /** Set service provider name. */
        public Builder setSpn(String spn) {
            mSpn = spn;
            return this;
        }

        /** Set data service provider name. */
        public Builder setDataSpn(String dataSpn) {
            mDataSpn = dataSpn;
            return this;
        }

        /** Set PLMN network name. */
        public Builder setPlmn(String plmn) {
            mPlmn = plmn;
            return this;
        }

        /** Set whether the service provider name should be displayed. */
        public Builder setShowSpn(boolean showSpn) {
            mShowSpn = showSpn;
            return this;
        }

        /** Set whether the PLMN network name should be displayed. */
        public Builder setShowPlmn(boolean showPlmn) {
            mShowPlmn = showPlmn;
            return this;
        }
    }
}
