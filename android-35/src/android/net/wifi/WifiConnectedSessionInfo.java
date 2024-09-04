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

package android.net.wifi;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class representing the session information of a connected Wi-Fi network for external Wi-Fi
 * scorer to identify the Wi-Fi network.
 *
 * @hide
 */
@SystemApi
public final class WifiConnectedSessionInfo implements Parcelable {
    private final int mSessionId;
    private final boolean mIsUserSelected;

    /** Create a new WifiConnectedSessionInfo object */
    private WifiConnectedSessionInfo(int sessionId, boolean isUserSelected) {
        mSessionId = sessionId;
        mIsUserSelected = isUserSelected;
    }

    /** Builder for WifiConnectedSessionInfo */
    public static final class Builder {
        private final int mSessionId;
        private boolean mIsUserSelected = false;

        /** Create a new builder */
        public Builder(int sessionId) {
            mSessionId = sessionId;
        }

        /** Set whether this network is user selected */
        @NonNull public Builder setUserSelected(boolean isUserSelected) {
            mIsUserSelected = isUserSelected;
            return this;
        }

        /** Build the WifiConnectedSessionInfo object represented by this builder */
        @NonNull public WifiConnectedSessionInfo build() {
            return new WifiConnectedSessionInfo(mSessionId, mIsUserSelected);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mSessionId);
        dest.writeBoolean(mIsUserSelected);
    }

    /** Implement the Parcelable interface */
    public static final @NonNull Creator<WifiConnectedSessionInfo> CREATOR =
            new Creator<WifiConnectedSessionInfo>() {
        public WifiConnectedSessionInfo createFromParcel(Parcel in) {
            return new WifiConnectedSessionInfo(in.readInt(), in.readBoolean());
        }

        public WifiConnectedSessionInfo[] newArray(int size) {
            return new WifiConnectedSessionInfo[size];
        }
    };

    /** The ID to indicate current Wi-Fi network connection */
    public int getSessionId() {
        return mSessionId;
    }

    /** Indicate whether current Wi-Fi network is selected by the user */
    public boolean isUserSelected() {
        return mIsUserSelected;
    }
}
