/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.net.TetheringManager;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.modules.utils.build.SdkLevel;
import com.android.wifi.flags.Flags;

import java.util.Objects;

/**
 * A class representing the current state of SoftAp.
 * @see WifiManager.SoftApCallback#onStateChanged(SoftApState)
 *
 * @hide
 */
@FlaggedApi(Flags.FLAG_ANDROID_V_WIFI_API)
@SystemApi
public final class SoftApState implements Parcelable {

    @WifiManager.WifiApState
    final int mState;
    @WifiManager.SapStartFailure
    final int mFailureReason;
    @Nullable
    final TetheringManager.TetheringRequest mTetheringRequest;
    @Nullable
    final String mIface;

    /**
     * SoftApState constructor.
     *
     * @param state Current state of the Soft AP.
     * @param failureReason Failure reason if the current state is
     *                      {@link WifiManager#WIFI_AP_STATE_FAILED}.
     * @param tetheringRequest TetheringRequest if one was specified when Soft AP was requested,
     *                         else {@code null}.
     * @param iface Interface name if an interface was created, else {@code null}.
     * @hide
     */
    public SoftApState(@WifiManager.WifiApState int state,
            @WifiManager.SapStartFailure int failureReason,
            @Nullable TetheringManager.TetheringRequest tetheringRequest,
            @Nullable String iface) {
        mState = state;
        mFailureReason = failureReason;
        mTetheringRequest = tetheringRequest;
        mIface = iface;
    }

    private SoftApState(@NonNull Parcel in) {
        mState = in.readInt();
        mFailureReason = in.readInt();
        if (SdkLevel.isAtLeastV()) {
            // TetheringRequest is parcelable starting in V.
            mTetheringRequest = in.readParcelable(
                    TetheringManager.TetheringRequest.class.getClassLoader());
        } else {
            mTetheringRequest = null;
        }
        mIface = in.readString();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mState);
        dest.writeInt(mFailureReason);
        if (SdkLevel.isAtLeastV()) {
            // TetheringRequest is parcelable starting in V.
            dest.writeParcelable(mTetheringRequest, flags);
        }
        dest.writeString(mIface);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public static final Creator<SoftApState> CREATOR = new Creator<SoftApState>() {
        @Override
        @NonNull
        public SoftApState createFromParcel(Parcel in) {
            return new SoftApState(in);
        }

        @Override
        @NonNull
        public SoftApState[] newArray(int size) {
            return new SoftApState[size];
        }
    };

    /**
     * Get the AP state.
     *
     * @return One of {@link WifiManager#WIFI_AP_STATE_DISABLED},
     *                {@link WifiManager#WIFI_AP_STATE_DISABLING},
     *                {@link WifiManager#WIFI_AP_STATE_ENABLED},
     *                {@link WifiManager#WIFI_AP_STATE_ENABLING},
     *                {@link WifiManager#WIFI_AP_STATE_FAILED}
     */
    @WifiManager.WifiApState
    public int getState() {
        return mState;
    }

    /**
     * Get the failure reason if the state is {@link WifiManager#WIFI_AP_STATE_FAILED}.
     *
     * @return One of {@link WifiManager#SAP_START_FAILURE_GENERAL},
     *                {@link WifiManager#SAP_START_FAILURE_NO_CHANNEL},
     *                {@link WifiManager#SAP_START_FAILURE_UNSUPPORTED_CONFIGURATION},
     *                {@link WifiManager#SAP_START_FAILURE_USER_REJECTED}
     * @throws IllegalStateException if the state is not {@link WifiManager#WIFI_AP_STATE_FAILED}.
     */
    @WifiManager.SapStartFailure
    public int getFailureReason() {
        if (mState != WifiManager.WIFI_AP_STATE_FAILED) {
            throw new IllegalStateException("Called getFailureReason() when state is not"
                    + " WIFI_AP_STATE_FAILED. Actual state is " + mState);
        }
        return getFailureReasonInternal();
    }

    /**
     * @hide
     */
    @WifiManager.SapStartFailure
    public int getFailureReasonInternal() {
        return mFailureReason;
    }

    /**
     * Gets the TetheringRequest of the Soft AP, if one was specified via
     * {@link WifiManager#startTetheredHotspotRequest(TetheringManager.TetheringRequest)}.
     * Otherwise, returns {@code null}.
     */
    @Nullable
    public TetheringManager.TetheringRequest getTetheringRequest() {
        return mTetheringRequest;
    }

    /**
     * Gets the interface name of the Soft AP (e.g. "wlan0") once the Soft AP starts enabling, i.e.
     * {@link #getState()} returns {@link WifiManager#WIFI_AP_STATE_ENABLING}). Returns {@code null}
     * if the Soft AP hasn't started enabling yet, or if it failed with
     * {@link WifiManager#WIFI_AP_STATE_FAILED} without starting enabling.
     */
    @Nullable
    public String getIface() {
        return mIface;
    }

    @Override
    public String toString() {
        return "SoftApState{"
                + "mState=" + mState
                + ", mFailureReason=" + mFailureReason
                + ", mTetheringRequest=" + mTetheringRequest
                + ", mIface='" + mIface + '\''
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SoftApState stateInfo)) return false;
        return mState == stateInfo.mState && mFailureReason == stateInfo.mFailureReason
                && Objects.equals(mTetheringRequest, stateInfo.mTetheringRequest)
                && Objects.equals(mIface, stateInfo.mIface);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mState, mFailureReason, mTetheringRequest, mIface);
    }
}
