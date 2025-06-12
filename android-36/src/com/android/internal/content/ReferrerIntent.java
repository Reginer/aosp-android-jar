/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.internal.content;

import android.compat.annotation.UnsupportedAppUsage;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;

import java.util.Objects;

/**
 * Subclass of Intent that also contains referrer (as a package name) information.
 */
public class ReferrerIntent extends Intent {
    @UnsupportedAppUsage
    public final String mReferrer;

    public final IBinder mCallerToken;

    @UnsupportedAppUsage
    public ReferrerIntent(Intent baseIntent, String referrer) {
        this(baseIntent, referrer, /* callerToken */ null);
    }

    public ReferrerIntent(Intent baseIntent, String referrer, IBinder callerToken) {
        super(baseIntent);
        mReferrer = referrer;
        mCallerToken = callerToken;
    }

    public void writeToParcel(Parcel dest, int parcelableFlags) {
        super.writeToParcel(dest, parcelableFlags);
        dest.writeString(mReferrer);
        dest.writeStrongBinder(mCallerToken);
    }

    ReferrerIntent(Parcel in) {
        readFromParcel(in);
        mReferrer = in.readString();
        mCallerToken = in.readStrongBinder();
    }

    public static final Creator<ReferrerIntent> CREATOR = new Creator<ReferrerIntent>() {
        public ReferrerIntent createFromParcel(Parcel source) {
            return new ReferrerIntent(source);
        }
        public ReferrerIntent[] newArray(int size) {
            return new ReferrerIntent[size];
        }
    };

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ReferrerIntent)) {
            return false;
        }
        final ReferrerIntent other = (ReferrerIntent) obj;
        return filterEquals(other) && Objects.equals(mReferrer, other.mReferrer)
                && Objects.equals(mCallerToken, other.mCallerToken);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + filterHashCode();
        result = 31 * result + Objects.hashCode(mReferrer);
        result = 31 * result + Objects.hashCode(mCallerToken);
        return result;
    }
}
