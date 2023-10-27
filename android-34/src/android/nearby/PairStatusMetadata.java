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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Metadata about an ongoing paring. Wraps transient data like status and progress.
 *
 * @hide
 */
public final class PairStatusMetadata implements Parcelable {

    @Status
    private final int mStatus;

    /** The status of the pairing. */
    @IntDef({
            Status.UNKNOWN,
            Status.SUCCESS,
            Status.FAIL,
            Status.DISMISS
    })
    public @interface Status {
        int UNKNOWN = 1000;
        int SUCCESS = 1001;
        int FAIL = 1002;
        int DISMISS = 1003;
    }

    /** Converts the status to readable string. */
    public static String statusToString(@Status int status) {
        switch (status) {
            case Status.SUCCESS:
                return "SUCCESS";
            case Status.FAIL:
                return "FAIL";
            case Status.DISMISS:
                return "DISMISS";
            case Status.UNKNOWN:
            default:
                return "UNKNOWN";
        }
    }

    public int getStatus() {
        return mStatus;
    }

    @Override
    public String toString() {
        return "PairStatusMetadata[ status=" + statusToString(mStatus) + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof PairStatusMetadata) {
            return mStatus == ((PairStatusMetadata) other).mStatus;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStatus);
    }

    public PairStatusMetadata(@Status int status) {
        mStatus = status;
    }

    public static final Creator<PairStatusMetadata> CREATOR = new Creator<PairStatusMetadata>() {
        @Override
        public PairStatusMetadata createFromParcel(Parcel in) {
            return new PairStatusMetadata(in.readInt());
        }

        @Override
        public PairStatusMetadata[] newArray(int size) {
            return new PairStatusMetadata[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public int getStability() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mStatus);
    }
}
