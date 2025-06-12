/*
 * Copyright 2024 The Android Open Source Project
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

package android.ranging.oob;

import android.os.Parcel;
import android.os.Parcelable;
import android.ranging.RangingDevice;
import android.ranging.SessionHandle;

import java.util.Objects;

/**
 * OobHandle identifies a unique session and device pair.
 *
 * @hide
 */
public final class OobHandle implements Parcelable {
    public static final Creator<OobHandle> CREATOR = new Creator<OobHandle>() {
        @Override
        public OobHandle createFromParcel(Parcel in) {
            return new OobHandle(in);
        }

        @Override
        public OobHandle[] newArray(int size) {
            return new OobHandle[size];
        }
    };
    private final SessionHandle mSessionHandle;
    private final RangingDevice mRangingDevice;

    public OobHandle(SessionHandle sessionHandle, RangingDevice device) {
        mSessionHandle = sessionHandle;
        mRangingDevice = device;
    }

    protected OobHandle(Parcel in) {
        mSessionHandle = in.readParcelable(SessionHandle.class.getClassLoader(),
                SessionHandle.class);
        mRangingDevice = in.readParcelable(RangingDevice.class.getClassLoader(),
                RangingDevice.class);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mSessionHandle, flags);
        dest.writeParcelable(mRangingDevice, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public SessionHandle getSessionHandle() {
        return mSessionHandle;
    }

    public RangingDevice getRangingDevice() {
        return mRangingDevice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OobHandle oobHandle)) return false;
        return Objects.equals(getSessionHandle(), oobHandle.getSessionHandle())
                && Objects.equals(getRangingDevice(), oobHandle.getRangingDevice());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSessionHandle(), getRangingDevice());
    }

    @Override
    public String toString() {
        return "OobHandle{ "
                + "mSessionHandle="
                + mSessionHandle
                + ", mRangingDevice="
                + mRangingDevice
                + " }";
    }
}
