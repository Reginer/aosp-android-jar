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

package android.os;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.profiling.Flags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Encapsulates results of a single profiling request operation.
 */
@FlaggedApi(Flags.FLAG_TELEMETRY_APIS)
public final class ProfilingResult implements Parcelable {

    /** @see #getErrorCode */
    final @ErrorCode int mErrorCode;

    /** @see #getResultFilePath */
    @Nullable final String mResultFilePath;

    /** @see #getTag */
    @Nullable final String mTag;

    /** @see #getErrorMessage */
    @Nullable final String mErrorMessage;

    /** The request was executed and succeeded. */
    public static final int ERROR_NONE = 0;

    /** The request was denied due to system level rate limiting. */
    public static final int ERROR_FAILED_RATE_LIMIT_SYSTEM = 1;

    /** The request was denied due to process level rate limiting. */
    public static final int ERROR_FAILED_RATE_LIMIT_PROCESS = 2;

    /** The request was denied due to profiling already in progress. */
    public static final int ERROR_FAILED_PROFILING_IN_PROGRESS = 3;

    /** The request was executed and failed for a reason not specified below. */
    public static final int ERROR_FAILED_EXECUTING = 4;

    /** The request was executed but post processing failed and the result was discarded. */
    public static final int ERROR_FAILED_POST_PROCESSING = 5;

    /** The request was executed and failed due to a lack of disk space. */
    public static final int ERROR_FAILED_NO_DISK_SPACE = 6;

    /** The request failed due to invalid ProfilingRequest. */
    public static final int ERROR_FAILED_INVALID_REQUEST = 7;

    /** The request was denied or failed for an unspecified reason. */
    public static final int ERROR_UNKNOWN = 8;

    /** @hide */
    @IntDef(value = {
            ERROR_NONE,
            ERROR_FAILED_RATE_LIMIT_SYSTEM,
            ERROR_FAILED_RATE_LIMIT_PROCESS,
            ERROR_FAILED_PROFILING_IN_PROGRESS,
            ERROR_FAILED_EXECUTING,
            ERROR_FAILED_POST_PROCESSING,
            ERROR_FAILED_NO_DISK_SPACE,
            ERROR_UNKNOWN,
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface ErrorCode {}

    ProfilingResult(@ErrorCode int errorCode, String resultFilePath, String tag,
            String errorMessage) {
        mErrorCode = errorCode;
        mResultFilePath = resultFilePath;
        mTag = tag;
        mErrorMessage = errorMessage;
    }

    private ProfilingResult(@NonNull Parcel in) {
        mErrorCode = in.readInt();
        mResultFilePath = in.readString();
        mTag = in.readString();
        mErrorMessage = in.readString();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mErrorCode);
        dest.writeString(mResultFilePath);
        dest.writeString(mTag);
        dest.writeString(mErrorMessage);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public @NonNull static final Creator<ProfilingResult> CREATOR =
            new Creator<ProfilingResult>() {
                @Override
                public ProfilingResult createFromParcel(Parcel in) {
                    return new ProfilingResult(in);
                }

                @Override
                public ProfilingResult[] newArray(int size) {
                    return new ProfilingResult[size];
                }
            };

    /**
     * The result ErrorCode for the profiling request indicating the failure reason if applicable.
     */
    public @ErrorCode int getErrorCode() {
        return mErrorCode;
    }

    /**
     * The file path of the profiling result data.
     *
     * Will be null if {@see #getErrorCode} returns code other than {@see #ERROR_NONE}.
     */
    public @Nullable String getResultFilePath() {
        return mResultFilePath;
    }

    /**
     * The tag defined by the caller at request time.
     */
    public @Nullable String getTag() {
        return mTag;
    }

    /**
     * Additional details about failures that occurred, if applicable.
     */
    public @Nullable String getErrorMessage() {
        return mErrorMessage;
    }
}
