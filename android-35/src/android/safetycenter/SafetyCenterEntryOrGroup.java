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

package android.safetycenter;

import static android.os.Build.VERSION_CODES.TIRAMISU;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import java.util.Objects;

/**
 * Contains either a single {@link SafetyCenterEntry} or a group of them in a {@link
 * SafetyCenterEntryGroup}.
 *
 * @hide
 */
@SystemApi
@RequiresApi(TIRAMISU)
public final class SafetyCenterEntryOrGroup implements Parcelable {

    @NonNull
    public static final Creator<SafetyCenterEntryOrGroup> CREATOR =
            new Creator<SafetyCenterEntryOrGroup>() {
                @Override
                public SafetyCenterEntryOrGroup createFromParcel(Parcel in) {
                    SafetyCenterEntry maybeEntry = in.readTypedObject(SafetyCenterEntry.CREATOR);
                    SafetyCenterEntryGroup maybeEntryGroup =
                            in.readTypedObject(SafetyCenterEntryGroup.CREATOR);
                    return maybeEntry != null
                            ? new SafetyCenterEntryOrGroup(maybeEntry)
                            : new SafetyCenterEntryOrGroup(maybeEntryGroup);
                }

                @Override
                public SafetyCenterEntryOrGroup[] newArray(int size) {
                    return new SafetyCenterEntryOrGroup[size];
                }
            };

    @Nullable private final SafetyCenterEntry mEntry;
    @Nullable private final SafetyCenterEntryGroup mEntryGroup;

    /** Create for a {@link SafetyCenterEntry}. */
    public SafetyCenterEntryOrGroup(@NonNull SafetyCenterEntry entry) {
        mEntry = requireNonNull(entry);
        mEntryGroup = null;
    }

    /** Create for a {@link SafetyCenterEntryGroup}. */
    public SafetyCenterEntryOrGroup(@NonNull SafetyCenterEntryGroup entryGroup) {
        mEntry = null;
        mEntryGroup = requireNonNull(entryGroup);
    }

    /**
     * Returns the {@link SafetyCenterEntry} if this holder contains one, {@code null} otherwise.
     *
     * <p>If this returns {@code null}, {@link #getEntryGroup()} must return a non-null value.
     */
    @Nullable
    public SafetyCenterEntry getEntry() {
        return mEntry;
    }

    /**
     * Returns the {@link SafetyCenterEntryGroup} if this holder contains one, {@code null}
     * otherwise.
     *
     * <p>If this returns {@code null}, {@link #getEntry()} must return a non-null value.
     */
    @Nullable
    public SafetyCenterEntryGroup getEntryGroup() {
        return mEntryGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyCenterEntryOrGroup)) return false;
        SafetyCenterEntryOrGroup that = (SafetyCenterEntryOrGroup) o;
        return Objects.equals(mEntry, that.mEntry) && Objects.equals(mEntryGroup, that.mEntryGroup);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mEntry, mEntryGroup);
    }

    @Override
    public String toString() {
        return "SafetyCenterEntryOrGroup{"
                + "mEntry="
                + mEntry
                + ", mEntryGroup="
                + mEntryGroup
                + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeTypedObject(mEntry, flags);
        dest.writeTypedObject(mEntryGroup, flags);
    }
}
