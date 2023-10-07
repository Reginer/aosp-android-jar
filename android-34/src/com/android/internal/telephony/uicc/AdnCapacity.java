/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.internal.telephony.uicc;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Used to present ADN capacity
 *
 * {@hide}
 */
public class AdnCapacity implements Parcelable {

    private int mMaxAdnCount;
    private int mUsedAdnCount;
    private int mMaxEmailCount;
    private int mUsedEmailCount;
    private int mMaxAnrCount;
    private int mUsedAnrCount;
    private int mMaxNameLength;
    private int mMaxNumberLength;
    private int mMaxEmailLength;
    private int mMaxAnrLength;

    private int mHashCode = 0;

    public AdnCapacity(int maxAdnCount, int usedAdnCount, int maxEmailCount,
            int usedEmailCount, int maxAnrCount, int usedAnrCount, int maxNameLength,
            int maxNumberLength, int maxEmailLength, int maxAnrLength) {
        mMaxAdnCount = maxAdnCount;
        mUsedAdnCount = usedAdnCount;
        mMaxEmailCount = maxEmailCount;
        mUsedEmailCount = usedEmailCount;
        mMaxAnrCount = maxAnrCount;
        mUsedAnrCount = usedAnrCount;
        mMaxNameLength = maxNameLength;
        mMaxNumberLength = maxNumberLength;
        mMaxEmailLength = maxEmailLength;
        mMaxAnrLength = maxAnrLength;
    }

    public AdnCapacity() {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public int getMaxAdnCount() {
        return mMaxAdnCount;
    }

    public int getUsedAdnCount() {
        return mUsedAdnCount;
    }

    public int getMaxEmailCount() {
        return mMaxEmailCount;
    }

    public int getUsedEmailCount() {
        return mUsedEmailCount;
    }

    public int getMaxAnrCount() {
        return mMaxAnrCount;
    }

    public int getUsedAnrCount() {
        return mUsedAnrCount;
    }

    public int getMaxNameLength() {
        return mMaxNameLength;
    }

    public int getMaxNumberLength() {
        return mMaxNumberLength;
    }

    public int getMaxEmailLength() {
        return mMaxEmailLength;
    }

    public int getMaxAnrLength() {
        return mMaxAnrLength;
    }

    public boolean isSimFull() {
        return mMaxAdnCount == mUsedAdnCount;
    }

    public boolean isSimEmpty() {
        return mUsedAdnCount == 0;
    }

    public boolean isSimValid() {
        return mMaxAdnCount > 0;
    }

    @Override
    public String toString() {
        String capacity = "getAdnRecordsCapacity : max adn=" + mMaxAdnCount
                + ", used adn=" + mUsedAdnCount
                + ", max email=" + mMaxEmailCount
                + ", used email=" + mUsedEmailCount
                + ", max anr=" + mMaxAnrCount
                + ", used anr=" + mUsedAnrCount
                + ", max name length=" + mMaxNameLength
                + ", max number length =" + mMaxNumberLength
                + ", max email length =" + mMaxEmailLength
                + ", max anr length =" + mMaxAnrLength;
        return capacity;
    }

    public static final Parcelable.Creator<AdnCapacity> CREATOR
            = new Parcelable.Creator<AdnCapacity>() {
        @Override
        public AdnCapacity createFromParcel(Parcel source) {
            final int maxAdnCount = source.readInt();
            final int usedAdnCount = source.readInt();
            final int maxEmailCount = source.readInt();
            final int usedEmailCount = source.readInt();
            final int maxAnrCount = source.readInt();
            final int usedAnrCount = source.readInt();
            final int maxNameLength = source.readInt();
            final int maxNumberLength = source.readInt();
            final int maxEmailLength = source.readInt();
            final int maxAnrLength = source.readInt();
            return new AdnCapacity(maxAdnCount, usedAdnCount, maxEmailCount,
                    usedEmailCount, maxAnrCount, usedAnrCount, maxNameLength,
                    maxNumberLength, maxEmailLength, maxAnrLength);
        }

        @Override
        public AdnCapacity[] newArray(int size) {
            return new AdnCapacity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mMaxAdnCount);
        dest.writeInt(mUsedAdnCount);
        dest.writeInt(mMaxEmailCount);
        dest.writeInt(mUsedEmailCount);
        dest.writeInt(mMaxAnrCount);
        dest.writeInt(mUsedAnrCount);
        dest.writeInt(mMaxNameLength);
        dest.writeInt(mMaxNumberLength);
        dest.writeInt(mMaxEmailLength);
        dest.writeInt(mMaxAnrLength);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AdnCapacity) {
            AdnCapacity capacity = (AdnCapacity)obj;
            return capacity.getMaxAdnCount() == mMaxAdnCount
                    && capacity.getUsedAdnCount() == mUsedAdnCount
                    && capacity.getMaxEmailCount() == mMaxEmailCount
                    && capacity.getUsedEmailCount() == mUsedEmailCount
                    && capacity.getMaxAnrCount() == mMaxAnrCount
                    && capacity.getUsedAnrCount() == mUsedAnrCount
                    && capacity.getMaxNameLength() == mMaxNameLength
                    && capacity.getMaxNumberLength() == mMaxNumberLength
                    && capacity.getMaxEmailLength() == mMaxEmailLength
                    && capacity.getMaxAnrLength() == mMaxAnrLength;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        if (mHashCode == 0) {
            mHashCode = mMaxAdnCount;
            mHashCode = 31 * mHashCode + mUsedAdnCount;
            mHashCode = 31 * mHashCode + mMaxEmailCount;
            mHashCode = 31 * mHashCode + mUsedEmailCount;
            mHashCode = 31 * mHashCode + mMaxAnrCount;
            mHashCode = 31 * mHashCode + mUsedAnrCount;
            mHashCode = 31 * mHashCode + mMaxNameLength;
            mHashCode = 31 * mHashCode + mMaxNumberLength;
            mHashCode = 31 * mHashCode + mMaxEmailLength;
            mHashCode = 31 * mHashCode + mMaxAnrLength;
        }
        return mHashCode;
    }
}
