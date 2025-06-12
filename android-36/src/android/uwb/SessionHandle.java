/*
 * Copyright 2020 The Android Open Source Project
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

package android.uwb;

import android.content.AttributionSource;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * @hide
 */
public final class SessionHandle implements Parcelable  {
    private final int mId;
    private final String mPackageName;
    private final int mUid;
    private final int mPid;

    public SessionHandle(int id, AttributionSource attributionSource, int pid) {
        this(id, attributionSource.getPackageName(), attributionSource.getUid(), pid);
    }

    private SessionHandle(int id, String packageName, int uid, int pid) {
        mId = id;
        mPackageName = packageName;
        mUid = uid;
        mPid = pid;
    }

    protected SessionHandle(Parcel in) {
        mId = in.readInt();
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

    public int getId() {
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
        dest.writeInt(mId);
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
        return "SessionHandle [id=" + mId + ", package-name: " + mPackageName
                + ", uid: " + mUid + ", pid: " + mPid + "]";
    }
}
