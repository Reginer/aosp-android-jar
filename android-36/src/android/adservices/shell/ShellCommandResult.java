/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.adservices.shell;

import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Represents the response from the runShellCommand API.
 *
 * @hide
 */
public final class ShellCommandResult implements Parcelable {

    private static final int RESULT_OK = 0;

    private final int mResultCode;
    @Nullable private final String mOut;
    @Nullable private final String mErr;

    private ShellCommandResult(int resultCode, @Nullable String out, @Nullable String err) {
        mResultCode = resultCode;
        mOut = out;
        mErr = err;
    }

    private ShellCommandResult(Parcel in) {
        this(in.readInt(), in.readString(), in.readString());
    }

    private ShellCommandResult(Builder builder) {
        this(builder.mResultCode, builder.mOut, builder.mErr);
    }

    public static final Creator<ShellCommandResult> CREATOR =
            new Parcelable.Creator<>() {
                @Override
                public ShellCommandResult createFromParcel(Parcel in) {
                    Objects.requireNonNull(in);
                    return new ShellCommandResult(in);
                }

                @Override
                public ShellCommandResult[] newArray(int size) {
                    return new ShellCommandResult[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Objects.requireNonNull(dest);
        dest.writeInt(mResultCode);
        dest.writeString(mOut);
        dest.writeString(mErr);
    }

    /** Returns the command status. */
    public int getResultCode() {
        return mResultCode;
    }

    /** Returns {@code true} if {@link #getResultCode} is greater than equal to 0. */
    public boolean isSuccess() {
        return getResultCode() >= 0;
    }

    /** Returns the output of the shell command result. */
    @Nullable
    public String getOut() {
        return mOut;
    }

    /** Returns the error message associated with this response. */
    @Nullable
    public String getErr() {
        return mErr;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("ShellCommandResult[code=").append(mResultCode);
        // Redact the output, as it could be too large
        if (mOut != null) {
            string.append(", out_size=").append(mOut.length());
        }
        if (mErr != null) {
            string.append(", err_size=").append(mErr.length());
        }
        return string.append(']').toString();
    }

    /**
     * Builder for {@link ShellCommandResult}.
     *
     * @hide
     */
    public static final class Builder {
        private int mResultCode = RESULT_OK;
        @Nullable private String mOut;
        @Nullable private String mErr;

        public Builder() {}

        /** Sets the Status Code. */
        public Builder setResultCode(int resultCode) {
            mResultCode = resultCode;
            return this;
        }

        /** Sets the shell command output in case of success. */
        public Builder setOut(@Nullable String out) {
            mOut = out;
            return this;
        }

        /** Sets the error message in case of command failure. */
        public Builder setErr(@Nullable String err) {
            mErr = err;
            return this;
        }

        /** Builds a {@link ShellCommandResult} object. */
        public ShellCommandResult build() {
            return new ShellCommandResult(this);
        }
    }
}
