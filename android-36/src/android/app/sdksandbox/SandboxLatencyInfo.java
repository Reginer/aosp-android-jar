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

package android.app.sdksandbox;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * To be used to send sandbox latency information via callback
 *
 * @hide
 */
public final class SandboxLatencyInfo implements Parcelable {
    @IntDef(
            prefix = "METHOD_",
            value = {
                METHOD_UNSPECIFIED,
                METHOD_LOAD_SDK,
                METHOD_REQUEST_SURFACE_PACKAGE,
                METHOD_GET_SANDBOXED_SDKS,
                METHOD_SYNC_DATA_FROM_CLIENT,
                METHOD_UNLOAD_SDK,
                METHOD_ADD_SDK_SANDBOX_LIFECYCLE_CALLBACK,
                METHOD_REMOVE_SDK_SANDBOX_LIFECYCLE_CALLBACK,
                METHOD_GET_SANDBOXED_SDKS_VIA_CONTROLLER,
                METHOD_REGISTER_APP_OWNED_SDK_SANDBOX_INTERFACE,
                METHOD_UNREGISTER_APP_OWNED_SDK_SANDBOX_INTERFACE,
                METHOD_GET_APP_OWNED_SDK_SANDBOX_INTERFACES,
                METHOD_LOAD_SDK_VIA_CONTROLLER,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Method {}

    public static final int METHOD_UNSPECIFIED = 0;
    public static final int METHOD_LOAD_SDK = 1;
    public static final int METHOD_REQUEST_SURFACE_PACKAGE = 3;
    public static final int METHOD_GET_SANDBOXED_SDKS = 5;
    public static final int METHOD_SYNC_DATA_FROM_CLIENT = 6;
    public static final int METHOD_UNLOAD_SDK = 7;
    public static final int METHOD_ADD_SDK_SANDBOX_LIFECYCLE_CALLBACK = 8;
    public static final int METHOD_REMOVE_SDK_SANDBOX_LIFECYCLE_CALLBACK = 9;
    public static final int METHOD_GET_SANDBOXED_SDKS_VIA_CONTROLLER = 10;
    public static final int METHOD_REGISTER_APP_OWNED_SDK_SANDBOX_INTERFACE = 11;
    public static final int METHOD_UNREGISTER_APP_OWNED_SDK_SANDBOX_INTERFACE = 12;
    public static final int METHOD_GET_APP_OWNED_SDK_SANDBOX_INTERFACES = 13;
    public static final int METHOD_LOAD_SDK_VIA_CONTROLLER = 14;

    @IntDef(
            prefix = "SANDBOX_STATUS_",
            value = {
                SANDBOX_STATUS_SUCCESS,
                SANDBOX_STATUS_FAILED_AT_APP_TO_SYSTEM_SERVER,
                SANDBOX_STATUS_FAILED_AT_SYSTEM_SERVER_APP_TO_SANDBOX,
                SANDBOX_STATUS_FAILED_AT_LOAD_SANDBOX,
                SANDBOX_STATUS_FAILED_AT_SYSTEM_SERVER_TO_SANDBOX,
                SANDBOX_STATUS_FAILED_AT_SANDBOX,
                SANDBOX_STATUS_FAILED_AT_SDK,
                SANDBOX_STATUS_FAILED_AT_SANDBOX_TO_SYSTEM_SERVER,
                SANDBOX_STATUS_FAILED_AT_SYSTEM_SERVER_SANDBOX_TO_APP,
                SANDBOX_STATUS_FAILED_AT_SYSTEM_SERVER_TO_APP,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SandboxStatus {}

    public static final int SANDBOX_STATUS_SUCCESS = 1;
    public static final int SANDBOX_STATUS_FAILED_AT_APP_TO_SYSTEM_SERVER = 2;
    public static final int SANDBOX_STATUS_FAILED_AT_SYSTEM_SERVER_APP_TO_SANDBOX = 3;
    public static final int SANDBOX_STATUS_FAILED_AT_LOAD_SANDBOX = 4;
    public static final int SANDBOX_STATUS_FAILED_AT_SYSTEM_SERVER_TO_SANDBOX = 5;
    public static final int SANDBOX_STATUS_FAILED_AT_SANDBOX = 6;
    public static final int SANDBOX_STATUS_FAILED_AT_SDK = 7;
    public static final int SANDBOX_STATUS_FAILED_AT_SANDBOX_TO_SYSTEM_SERVER = 8;
    public static final int SANDBOX_STATUS_FAILED_AT_SYSTEM_SERVER_SANDBOX_TO_APP = 9;
    public static final int SANDBOX_STATUS_FAILED_AT_SYSTEM_SERVER_TO_APP = 10;

    @IntDef(
            prefix = "RESULT_CODE_",
            value = {
                RESULT_CODE_UNSPECIFIED,
                RESULT_CODE_LOAD_SDK_NOT_FOUND,
                RESULT_CODE_LOAD_SDK_ALREADY_LOADED,
                RESULT_CODE_LOAD_SDK_SDK_DEFINED_ERROR,
                RESULT_CODE_LOAD_SDK_SDK_SANDBOX_DISABLED,
                RESULT_CODE_LOAD_SDK_INTERNAL_ERROR,
                RESULT_CODE_SDK_SANDBOX_PROCESS_NOT_AVAILABLE
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResultCode {}

    public static final int RESULT_CODE_UNSPECIFIED = 0;
    public static final int RESULT_CODE_LOAD_SDK_NOT_FOUND = 1;
    public static final int RESULT_CODE_LOAD_SDK_ALREADY_LOADED = 2;
    public static final int RESULT_CODE_LOAD_SDK_SDK_DEFINED_ERROR = 3;
    public static final int RESULT_CODE_LOAD_SDK_SDK_SANDBOX_DISABLED = 4;
    public static final int RESULT_CODE_LOAD_SDK_INTERNAL_ERROR = 5;
    public static final int RESULT_CODE_SDK_SANDBOX_PROCESS_NOT_AVAILABLE = 6;

    private final @Method int mMethod;
    private long mTimeAppCalledSystemServer = -1;
    private long mTimeSystemServerReceivedCallFromApp = -1;
    private long mTimeLoadSandboxStarted = -1;
    private long mTimeSandboxLoaded = -1;
    private long mTimeSystemServerCallFinished = -1;
    private long mTimeSandboxReceivedCallFromSystemServer = -1;
    private long mTimeSandboxCalledSdk = -1;
    private long mTimeSdkCallCompleted = -1;
    private long mTimeSandboxCalledSystemServer = -1;
    private long mTimeSystemServerReceivedCallFromSandbox = -1;
    private long mTimeSystemServerCalledApp = -1;
    private long mTimeAppReceivedCallFromSystemServer = -1;
    private @SandboxStatus int mSandboxStatus = SANDBOX_STATUS_SUCCESS;
    private @ResultCode int mResultCode = RESULT_CODE_UNSPECIFIED;

    public static final @NonNull Parcelable.Creator<SandboxLatencyInfo> CREATOR =
            new Parcelable.Creator<SandboxLatencyInfo>() {
                public SandboxLatencyInfo createFromParcel(Parcel in) {
                    return new SandboxLatencyInfo(in);
                }

                public SandboxLatencyInfo[] newArray(int size) {
                    return new SandboxLatencyInfo[size];
                }
            };

    // TODO(b/297352617): add timeAppCalledSystemServer to the constructor.
    public SandboxLatencyInfo(@Method int method) {
        mMethod = method;
    }

    public SandboxLatencyInfo() {
        mMethod = SandboxLatencyInfo.METHOD_UNSPECIFIED;
    }

    private SandboxLatencyInfo(Parcel in) {
        mMethod = in.readInt();
        mTimeAppCalledSystemServer = in.readLong();
        mTimeSystemServerReceivedCallFromApp = in.readLong();
        mTimeLoadSandboxStarted = in.readLong();
        mTimeSandboxLoaded = in.readLong();
        mTimeSystemServerCallFinished = in.readLong();
        mTimeSandboxReceivedCallFromSystemServer = in.readLong();
        mTimeSandboxCalledSdk = in.readLong();
        mTimeSdkCallCompleted = in.readLong();
        mTimeSandboxCalledSystemServer = in.readLong();
        mTimeSystemServerReceivedCallFromSandbox = in.readLong();
        mTimeSystemServerCalledApp = in.readLong();
        mTimeAppReceivedCallFromSystemServer = in.readLong();
        mSandboxStatus = in.readInt();
        mResultCode = in.readInt();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof SandboxLatencyInfo)) return false;
        SandboxLatencyInfo that = (SandboxLatencyInfo) object;
        return mMethod == that.mMethod
                && mTimeAppCalledSystemServer == that.mTimeAppCalledSystemServer
                && mTimeSystemServerReceivedCallFromApp == that.mTimeSystemServerReceivedCallFromApp
                && mTimeLoadSandboxStarted == that.mTimeLoadSandboxStarted
                && mTimeSandboxLoaded == that.mTimeSandboxLoaded
                && mTimeSystemServerCallFinished == that.mTimeSystemServerCallFinished
                && mTimeSandboxReceivedCallFromSystemServer
                        == that.mTimeSandboxReceivedCallFromSystemServer
                && mTimeSandboxCalledSdk == that.mTimeSandboxCalledSdk
                && mTimeSdkCallCompleted == that.mTimeSdkCallCompleted
                && mTimeSandboxCalledSystemServer == that.mTimeSandboxCalledSystemServer
                && mTimeSystemServerReceivedCallFromSandbox
                        == that.mTimeSystemServerReceivedCallFromSandbox
                && mTimeSystemServerCalledApp == that.mTimeSystemServerCalledApp
                && mTimeAppReceivedCallFromSystemServer == that.mTimeAppReceivedCallFromSystemServer
                && mSandboxStatus == that.mSandboxStatus
                && mResultCode == that.mResultCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mMethod,
                mTimeAppCalledSystemServer,
                mTimeSystemServerReceivedCallFromApp,
                mTimeLoadSandboxStarted,
                mTimeSandboxLoaded,
                mTimeSystemServerCallFinished,
                mTimeSandboxReceivedCallFromSystemServer,
                mTimeSandboxCalledSdk,
                mTimeSdkCallCompleted,
                mTimeSandboxCalledSystemServer,
                mTimeSystemServerReceivedCallFromSandbox,
                mTimeSystemServerCalledApp,
                mTimeAppReceivedCallFromSystemServer,
                mSandboxStatus,
                mResultCode);
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        out.writeInt(mMethod);
        out.writeLong(mTimeAppCalledSystemServer);
        out.writeLong(mTimeSystemServerReceivedCallFromApp);
        out.writeLong(mTimeLoadSandboxStarted);
        out.writeLong(mTimeSandboxLoaded);
        out.writeLong(mTimeSystemServerCallFinished);
        out.writeLong(mTimeSandboxReceivedCallFromSystemServer);
        out.writeLong(mTimeSandboxCalledSdk);
        out.writeLong(mTimeSdkCallCompleted);
        out.writeLong(mTimeSandboxCalledSystemServer);
        out.writeLong(mTimeSystemServerReceivedCallFromSandbox);
        out.writeLong(mTimeSystemServerCalledApp);
        out.writeLong(mTimeAppReceivedCallFromSystemServer);
        out.writeInt(mSandboxStatus);
        out.writeInt(mResultCode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public @Method int getMethod() {
        return mMethod;
    }

    public void setTimeAppCalledSystemServer(long timeAppCalledSystemServer) {
        mTimeAppCalledSystemServer = timeAppCalledSystemServer;
    }

    public void setTimeSystemServerReceivedCallFromApp(long timeSystemServerReceivedCallFromApp) {
        mTimeSystemServerReceivedCallFromApp = timeSystemServerReceivedCallFromApp;
    }

    public void setTimeLoadSandboxStarted(long timeLoadSandboxStarted) {
        mTimeLoadSandboxStarted = timeLoadSandboxStarted;
    }

    public void setTimeSandboxLoaded(long timeSandboxLoaded) {
        mTimeSandboxLoaded = timeSandboxLoaded;
    }

    public long getTimeSystemServerCallFinished() {
        return mTimeSystemServerCallFinished;
    }

    public void setTimeSystemServerCallFinished(long timeSystemServerCallFinished) {
        mTimeSystemServerCallFinished = timeSystemServerCallFinished;
    }

    public void setTimeSandboxReceivedCallFromSystemServer(
            long timeSandboxReceivedCallFromSystemServer) {
        mTimeSandboxReceivedCallFromSystemServer = timeSandboxReceivedCallFromSystemServer;
    }

    public void setTimeSandboxCalledSdk(long timeSandboxCalledSdk) {
        mTimeSandboxCalledSdk = timeSandboxCalledSdk;
    }

    public void setTimeSdkCallCompleted(long timeSdkCallCompleted) {
        mTimeSdkCallCompleted = timeSdkCallCompleted;
    }

    public long getTimeSandboxCalledSystemServer() {
        return mTimeSandboxCalledSystemServer;
    }

    public void setTimeSandboxCalledSystemServer(long timeSandboxCalledSystemServer) {
        mTimeSandboxCalledSystemServer = timeSandboxCalledSystemServer;
    }

    public void setTimeSystemServerReceivedCallFromSandbox(
            long timeSystemServerReceivedCallFromSandbox) {
        mTimeSystemServerReceivedCallFromSandbox = timeSystemServerReceivedCallFromSandbox;
    }

    public void setTimeSystemServerCalledApp(long timeSystemServerCalledApp) {
        mTimeSystemServerCalledApp = timeSystemServerCalledApp;
    }

    public void setTimeAppReceivedCallFromSystemServer(long timeAppReceivedCallFromSystemServer) {
        mTimeAppReceivedCallFromSystemServer = timeAppReceivedCallFromSystemServer;
    }

    public void setSandboxStatus(@SandboxStatus int sandboxStatus) {
        mSandboxStatus = sandboxStatus;
    }

    public void setResultCode(@ResultCode int resultCode) {
        mResultCode = resultCode;
    }

    /** Returns latency of the IPC call from App call to System Server. */
    public int getAppToSystemServerLatency() {
        return getLatency(mTimeAppCalledSystemServer, mTimeSystemServerReceivedCallFromApp);
    }

    /** Returns latency of the System Server stage of the call that was received from App. */
    public int getSystemServerAppToSandboxLatency() {
        int systemServerAppToSandboxLatency =
                getLatency(mTimeSystemServerReceivedCallFromApp, mTimeSystemServerCallFinished);
        int loadSandboxLatency = getLoadSandboxLatency();
        return loadSandboxLatency == -1
                ? systemServerAppToSandboxLatency
                : systemServerAppToSandboxLatency - loadSandboxLatency;
    }

    /** Returns latency of the LoadSandbox stage of the call. */
    public int getLoadSandboxLatency() {
        return getLatency(mTimeLoadSandboxStarted, mTimeSandboxLoaded);
    }

    /** Returns latency of the IPC call from System Server to Sandbox. */
    public int getSystemServerToSandboxLatency() {
        return getLatency(mTimeSystemServerCallFinished, mTimeSandboxReceivedCallFromSystemServer);
    }

    /** Returns latency of the Sandbox stage of the call. */
    public int getSandboxLatency() {
        int sandboxLatency =
                getLatency(
                        mTimeSandboxReceivedCallFromSystemServer, mTimeSandboxCalledSystemServer);
        int sdkLatency = getSdkLatency();
        if (sdkLatency != -1) {
            sandboxLatency -= sdkLatency;
        }
        return sandboxLatency;
    }

    /** Returns latency of the SDK stage of the call. */
    public int getSdkLatency() {
        return getLatency(mTimeSandboxCalledSdk, mTimeSdkCallCompleted);
    }

    /** Returns latency of the Sandbox call to System Server. */
    public int getSandboxToSystemServerLatency() {
        return getLatency(mTimeSandboxCalledSystemServer, mTimeSystemServerReceivedCallFromSandbox);
    }

    /** Returns latency of the System Server stage of the call that was received from Sandbox. */
    public int getSystemServerSandboxToAppLatency() {
        return getLatency(mTimeSystemServerReceivedCallFromSandbox, mTimeSystemServerCalledApp);
    }

    /** Returns latency of the IPC call from System Server to App. */
    public int getSystemServerToAppLatency() {
        return getLatency(mTimeSystemServerCalledApp, mTimeAppReceivedCallFromSystemServer);
    }

    /**
     * Returns total latency of the API call. Call finish time is defined depending on the API
     * called.
     */
    public int getTotalCallLatency() {
        return getLatency(mTimeAppCalledSystemServer, getTotalCallFinishTime());
    }

    public boolean isSuccessfulAtAppToSystemServer() {
        return mSandboxStatus != SANDBOX_STATUS_FAILED_AT_APP_TO_SYSTEM_SERVER;
    }

    public boolean isSuccessfulAtSystemServerAppToSandbox() {
        return mSandboxStatus != SANDBOX_STATUS_FAILED_AT_SYSTEM_SERVER_APP_TO_SANDBOX;
    }

    public boolean isSuccessfulAtLoadSandbox() {
        return mSandboxStatus != SANDBOX_STATUS_FAILED_AT_LOAD_SANDBOX;
    }

    public boolean isSuccessfulAtSystemServerToSandbox() {
        return mSandboxStatus != SANDBOX_STATUS_FAILED_AT_SYSTEM_SERVER_TO_SANDBOX;
    }

    public boolean isSuccessfulAtSdk() {
        return mSandboxStatus != SANDBOX_STATUS_FAILED_AT_SDK;
    }

    public boolean isSuccessfulAtSandbox() {
        return mSandboxStatus != SANDBOX_STATUS_FAILED_AT_SANDBOX;
    }

    public boolean isSuccessfulAtSandboxToSystemServer() {
        return mSandboxStatus != SANDBOX_STATUS_FAILED_AT_SANDBOX_TO_SYSTEM_SERVER;
    }

    public boolean isSuccessfulAtSystemServerSandboxToApp() {
        return mSandboxStatus != SANDBOX_STATUS_FAILED_AT_SYSTEM_SERVER_SANDBOX_TO_APP;
    }

    public boolean isSuccessfulAtSystemServerToApp() {
        return mSandboxStatus != SANDBOX_STATUS_FAILED_AT_SYSTEM_SERVER_TO_APP;
    }

    public boolean isTotalCallSuccessful() {
        return mSandboxStatus == SANDBOX_STATUS_SUCCESS;
    }

    public @ResultCode int getResultCode() {
        return mResultCode;
    }

    @VisibleForTesting
    public long getTimeAppCalledSystemServer() {
        return mTimeAppCalledSystemServer;
    }

    @VisibleForTesting
    public long getTimeAppReceivedCallFromSystemServer() {
        return mTimeAppReceivedCallFromSystemServer;
    }

    private long getTotalCallFinishTime() {
        switch (mMethod) {
            case METHOD_LOAD_SDK:
            case METHOD_LOAD_SDK_VIA_CONTROLLER:
            case METHOD_REQUEST_SURFACE_PACKAGE:
                return mTimeAppReceivedCallFromSystemServer;
            case METHOD_GET_SANDBOXED_SDKS:
            case METHOD_GET_SANDBOXED_SDKS_VIA_CONTROLLER:
            case METHOD_REGISTER_APP_OWNED_SDK_SANDBOX_INTERFACE:
            case METHOD_UNREGISTER_APP_OWNED_SDK_SANDBOX_INTERFACE:
            case METHOD_GET_APP_OWNED_SDK_SANDBOX_INTERFACES:
            case METHOD_ADD_SDK_SANDBOX_LIFECYCLE_CALLBACK:
            case METHOD_REMOVE_SDK_SANDBOX_LIFECYCLE_CALLBACK:
                return mTimeSystemServerCallFinished;
                // TODO(b/243367105): change finish time for the method once latency for all stages
                // is logged.
            case METHOD_SYNC_DATA_FROM_CLIENT:
                return mTimeSystemServerReceivedCallFromApp;
            case METHOD_UNLOAD_SDK:
                return mTimeSystemServerCalledApp;
            default:
                return -1;
        }
    }

    private int getLatency(long timeEventStarted, long timeEventFinished) {
        if (timeEventStarted != -1 && timeEventFinished != -1) {
            return ((int) (timeEventFinished - timeEventStarted));
        }
        return -1;
    }
}
