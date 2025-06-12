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

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents request which contains command and args as an input to runShellCommand API.
 *
 * @hide
 */
public class ShellCommandParam implements Parcelable {

    /* Array containing command name with all the args */
    private final String[] mCommandArgs;

    private final long mMaxCommandDurationMillis;

    public ShellCommandParam(long maxCommandDurationMillis, String... commandArgs) {
        mMaxCommandDurationMillis = maxCommandDurationMillis;
        mCommandArgs = Objects.requireNonNull(commandArgs);
    }

    private ShellCommandParam(Parcel in) {
        this(in.readLong(), in.createStringArray());
    }

    public static final Creator<ShellCommandParam> CREATOR =
            new Parcelable.Creator<>() {
                @Override
                public ShellCommandParam createFromParcel(Parcel in) {
                    return new ShellCommandParam(in);
                }

                @Override
                public ShellCommandParam[] newArray(int size) {
                    return new ShellCommandParam[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mMaxCommandDurationMillis);
        dest.writeStringArray(mCommandArgs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShellCommandParam)) {
            return false;
        }

        ShellCommandParam that = (ShellCommandParam) o;
        return (mMaxCommandDurationMillis == that.mMaxCommandDurationMillis)
                && Arrays.equals(mCommandArgs, that.mCommandArgs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mMaxCommandDurationMillis, Arrays.hashCode(mCommandArgs));
    }

    /** Get the command name with all the args as a list. */
    public String[] getCommandArgs() {
        return mCommandArgs;
    }

    /**
     * Gets the max duration of the command for which service will wait to complete command
     * execution.
     */
    public long getMaxCommandDurationMillis() {
        return mMaxCommandDurationMillis;
    }
}
