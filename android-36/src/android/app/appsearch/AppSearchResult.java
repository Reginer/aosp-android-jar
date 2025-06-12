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
package android.app.appsearch;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.exceptions.AppSearchException;
import android.app.appsearch.util.LogUtil;
import android.util.Log;

import com.android.appsearch.flags.Flags;
import com.android.internal.util.Preconditions;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Information about the success or failure of an AppSearch call.
 *
 * @param <ValueType> The type of result object for successful calls.
 */
// TODO(b/384721898): Switch to JSpecify annotations
@SuppressWarnings("JSpecifyNullness")
public final class AppSearchResult<ValueType> {
    private static final String TAG = "AppSearchResult";

    /**
     * Result codes from {@link AppSearchSession} methods.
     *
     * @hide
     */
    @IntDef(
            value = {
                RESULT_OK,
                RESULT_UNKNOWN_ERROR,
                RESULT_INTERNAL_ERROR,
                RESULT_INVALID_ARGUMENT,
                RESULT_IO_ERROR,
                RESULT_OUT_OF_SPACE,
                RESULT_NOT_FOUND,
                RESULT_INVALID_SCHEMA,
                RESULT_SECURITY_ERROR,
                RESULT_DENIED,
                RESULT_RATE_LIMITED,
                RESULT_ALREADY_EXISTS
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResultCode {}

    /** The call was successful. */
    public static final int RESULT_OK = 0;

    /** An unknown error occurred while processing the call. */
    public static final int RESULT_UNKNOWN_ERROR = 1;

    /**
     * An internal error occurred within AppSearch, which the caller cannot address.
     *
     * <p>This error may be considered similar to {@link IllegalStateException}
     */
    public static final int RESULT_INTERNAL_ERROR = 2;

    /**
     * The caller supplied invalid arguments to the call.
     *
     * <p>This error may be considered similar to {@link IllegalArgumentException}.
     */
    public static final int RESULT_INVALID_ARGUMENT = 3;

    /**
     * An issue occurred reading or writing to storage. The call might succeed if repeated.
     *
     * <p>This error may be considered similar to {@link java.io.IOException}.
     */
    public static final int RESULT_IO_ERROR = 4;

    /** Storage is out of space, and no more space could be reclaimed. */
    public static final int RESULT_OUT_OF_SPACE = 5;

    /** An entity the caller requested to interact with does not exist in the system. */
    public static final int RESULT_NOT_FOUND = 6;

    /** The caller supplied a schema which is invalid or incompatible with the previous schema. */
    public static final int RESULT_INVALID_SCHEMA = 7;

    /** The caller requested an operation it does not have privileges for. */
    public static final int RESULT_SECURITY_ERROR = 8;

    /**
     * The requested operation is denied for the caller. This error is logged and returned for
     * denylist rejections.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_RESULT_DENIED_AND_RESULT_RATE_LIMITED)
    public static final int RESULT_DENIED = 9;

    /**
     * The caller has hit AppSearch's rate limit and the requested operation has been rejected. The
     * caller is recommended to reschedule tasks with exponential backoff.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_RESULT_DENIED_AND_RESULT_RATE_LIMITED)
    public static final int RESULT_RATE_LIMITED = 10;

    /** The operation is invalid because the resource already exists and can't be replaced. */
    @FlaggedApi(Flags.FLAG_ENABLE_RESULT_ALREADY_EXISTS)
    public static final int RESULT_ALREADY_EXISTS = 12;

    @ResultCode private final int mResultCode;
    private final @Nullable ValueType mResultValue;
    private final @Nullable String mErrorMessage;

    private AppSearchResult(
            @ResultCode int resultCode,
            @Nullable ValueType resultValue,
            @Nullable String errorMessage) {
        mResultCode = resultCode;
        mResultValue = resultValue;
        mErrorMessage = errorMessage;
    }

    /** Returns {@code true} if {@link #getResultCode} equals {@link AppSearchResult#RESULT_OK}. */
    public boolean isSuccess() {
        return getResultCode() == RESULT_OK;
    }

    /** Returns one of the {@code RESULT} constants defined in {@link AppSearchResult}. */
    @ResultCode
    public int getResultCode() {
        return mResultCode;
    }

    /**
     * Returns the result value associated with this result, if it was successful.
     *
     * <p>See the documentation of the particular {@link AppSearchSession} call producing this
     * {@link AppSearchResult} for what is placed in the result value by that call.
     *
     * @throws IllegalStateException if this {@link AppSearchResult} is not successful.
     */
    public @Nullable ValueType getResultValue() {
        if (!isSuccess()) {
            throw new IllegalStateException("AppSearchResult is a failure: " + this);
        }
        return mResultValue;
    }

    /**
     * Returns the error message associated with this result.
     *
     * <p>If {@link #isSuccess} is {@code true}, the error message is always {@code null}. The error
     * message may be {@code null} even if {@link #isSuccess} is {@code false}. See the
     * documentation of the particular {@link AppSearchSession} call producing this {@link
     * AppSearchResult} for what is returned by {@link #getErrorMessage}.
     */
    public @Nullable String getErrorMessage() {
        return mErrorMessage;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AppSearchResult)) {
            return false;
        }
        AppSearchResult<?> otherResult = (AppSearchResult<?>) other;
        return mResultCode == otherResult.mResultCode
                && Objects.equals(mResultValue, otherResult.mResultValue)
                && Objects.equals(mErrorMessage, otherResult.mErrorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mResultCode, mResultValue, mErrorMessage);
    }

    @Override
    public @NonNull String toString() {
        if (isSuccess()) {
            return "[SUCCESS]: " + mResultValue;
        }
        return "[FAILURE(" + mResultCode + ")]: " + mErrorMessage;
    }

    /**
     * Creates a new successful {@link AppSearchResult}.
     *
     * @param value An optional value to associate with the successful result of the operation being
     *     performed.
     */
    public static @NonNull <ValueType> AppSearchResult<ValueType> newSuccessfulResult(
            @Nullable ValueType value) {
        return new AppSearchResult<>(RESULT_OK, value, /* errorMessage= */ null);
    }

    /**
     * Creates a new failed {@link AppSearchResult}.
     *
     * @param resultCode One of the constants documented in {@link AppSearchResult#getResultCode}.
     * @param errorMessage An optional string describing the reason or nature of the failure.
     */
    public static @NonNull <ValueType> AppSearchResult<ValueType> newFailedResult(
            @ResultCode int resultCode, @Nullable String errorMessage) {
        return new AppSearchResult<>(resultCode, /* resultValue= */ null, errorMessage);
    }

    /**
     * Creates a new failed {@link AppSearchResult} by a AppSearchResult in another type.
     *
     * @hide
     */
    public static @NonNull <ValueType> AppSearchResult<ValueType> newFailedResult(
            @NonNull AppSearchResult<?> otherFailedResult) {
        Preconditions.checkState(
                !otherFailedResult.isSuccess(),
                "Cannot convert a success result to a failed result");
        return AppSearchResult.newFailedResult(
                otherFailedResult.getResultCode(), otherFailedResult.getErrorMessage());
    }

    /** @hide */
    public static @NonNull <ValueType> AppSearchResult<ValueType> throwableToFailedResult(
            @NonNull Throwable t) {
        // Log for traceability. NOT_FOUND is logged at VERBOSE because this error can occur during
        // the regular operation of the system (b/183550974). Everything else is indicative of an
        // actual problem and is logged at WARN.
        if (t instanceof AppSearchException
                && ((AppSearchException) t).getResultCode() == RESULT_NOT_FOUND) {
            if (LogUtil.DEBUG) {
                Log.v(TAG, "Converting throwable to failed result: " + t);
            }
        } else {
            Log.w(TAG, "Converting throwable to failed result.", t);
        }

        if (t instanceof AppSearchException) {
            return ((AppSearchException) t).toAppSearchResult();
        }

        String exceptionClass = t.getClass().getSimpleName();
        @AppSearchResult.ResultCode int resultCode;
        if (t instanceof IllegalStateException || t instanceof NullPointerException) {
            resultCode = AppSearchResult.RESULT_INTERNAL_ERROR;
        } else if (t instanceof IllegalArgumentException) {
            resultCode = AppSearchResult.RESULT_INVALID_ARGUMENT;
        } else if (t instanceof IOException) {
            resultCode = AppSearchResult.RESULT_IO_ERROR;
        } else if (t instanceof SecurityException) {
            resultCode = AppSearchResult.RESULT_SECURITY_ERROR;
        } else {
            resultCode = AppSearchResult.RESULT_UNKNOWN_ERROR;
        }
        return AppSearchResult.newFailedResult(resultCode, exceptionClass + ": " + t.getMessage());
    }
}
