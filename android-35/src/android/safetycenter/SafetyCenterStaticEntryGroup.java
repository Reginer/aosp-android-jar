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

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A group of related {@link SafetyCenterStaticEntry} objects in the Safety Center.
 *
 * @hide
 */
@SystemApi
@RequiresApi(TIRAMISU)
public final class SafetyCenterStaticEntryGroup implements Parcelable {

    @NonNull
    public static final Creator<SafetyCenterStaticEntryGroup> CREATOR =
            new Creator<SafetyCenterStaticEntryGroup>() {
                @Override
                public SafetyCenterStaticEntryGroup createFromParcel(Parcel source) {
                    CharSequence title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
                    List<SafetyCenterStaticEntry> staticEntries =
                            source.createTypedArrayList(SafetyCenterStaticEntry.CREATOR);
                    return new SafetyCenterStaticEntryGroup(title, staticEntries);
                }

                @Override
                public SafetyCenterStaticEntryGroup[] newArray(int size) {
                    return new SafetyCenterStaticEntryGroup[size];
                }
            };

    @NonNull private final CharSequence mTitle;
    @NonNull private final List<SafetyCenterStaticEntry> mStaticEntries;

    /** Creates a {@link SafetyCenterStaticEntryGroup} with the given title and entries. */
    public SafetyCenterStaticEntryGroup(
            @NonNull CharSequence title, @NonNull List<SafetyCenterStaticEntry> staticEntries) {
        mTitle = requireNonNull(title);
        mStaticEntries = unmodifiableList(new ArrayList<>(requireNonNull(staticEntries)));
    }

    /** Returns the title that describes this entry group. */
    @NonNull
    public CharSequence getTitle() {
        return mTitle;
    }

    /** Returns the entries that comprise this entry group. */
    @NonNull
    public List<SafetyCenterStaticEntry> getStaticEntries() {
        return mStaticEntries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyCenterStaticEntryGroup)) return false;
        SafetyCenterStaticEntryGroup that = (SafetyCenterStaticEntryGroup) o;
        return TextUtils.equals(mTitle, that.mTitle)
                && Objects.equals(mStaticEntries, that.mStaticEntries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mStaticEntries);
    }

    @Override
    public String toString() {
        return "SafetyCenterStaticEntryGroup{"
                + "mTitle="
                + mTitle
                + ", mStaticEntries="
                + mStaticEntries
                + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        TextUtils.writeToParcel(mTitle, dest, flags);
        dest.writeTypedList(mStaticEntries);
    }
}
