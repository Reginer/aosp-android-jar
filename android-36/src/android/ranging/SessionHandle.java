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

package android.ranging;

import android.content.AttributionSource;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * @hide
 */
public final class SessionHandle implements Parcelable  {
    private final long mId;
    private final String mPackageName;
    private final int mUid;
    private final int mPid;

    public SessionHandle(long id, AttributionSource attributionSource, int pid) {
        this(id, attributionSource.getPackageName(), attributionSource.getUid(), pid);
    }

    private SessionHandle(long id, String packageName, int uid, int pid) {
        mId = id;
        mPackageName = packageName;
        mUid = uid;
        mPid = pid;
    }

    protected SessionHandle(Parcel in) {
        mId = in.readLong();
        mPackageName = in.readString();
        mUid = in.readInt();
        mPid = in.readInt();
    }

    public static final Creator<SessionHandle> CREATOR = new Creator<SessionHandle>() {
        @Override
        public SessionHandle createFromParcel(Parcel in) {
            return new SessionHandle(in);
        }

        @Override
        public SessionHandle[] newArray(int size) {
            return new SessionHandle[size];
        }
    };

    public long getId() {
        return mId;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public int getUid() {
        return mUid;
    }

    public int getPid() {
        return mPid;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeString(mPackageName);
        dest.writeInt(mUid);
        dest.writeInt(mPid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof SessionHandle) {
            SessionHandle other = (SessionHandle) obj;
            return mId == other.mId
                    && Objects.equals(mPackageName, other.mPackageName)
                    && mUid == other.mUid
                    && mPid == other.mPid;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mPackageName, mUid, mPid);
    }

    @Override
    public String toString() {
        return "SessionHandle{ "
                + "mId="
                + mId
                + ", mPackageName='"
                + mPackageName
                + ", mUid="
                + mUid
                + ", mPid="
                + mPid
                + " }";
    }
}
