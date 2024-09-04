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
import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import java.util.Objects;

/**
 * A static, stateless entry in the Safety Center.
 *
 * <p>Static entries have no changing severity level or associated issues. They provide simple links
 * or actions for safety-related features via {@link #getPendingIntent()}.
 *
 * @hide
 */
@SystemApi
@RequiresApi(TIRAMISU)
public final class SafetyCenterStaticEntry implements Parcelable {

    @NonNull
    public static final Creator<SafetyCenterStaticEntry> CREATOR =
            new Creator<SafetyCenterStaticEntry>() {
                @Override
                public SafetyCenterStaticEntry createFromParcel(Parcel in) {
                    CharSequence title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
                    return new SafetyCenterStaticEntry.Builder(title)
                            .setSummary(TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in))
                            .setPendingIntent(in.readTypedObject(PendingIntent.CREATOR))
                            .build();
                }

                @Override
                public SafetyCenterStaticEntry[] newArray(int size) {
                    return new SafetyCenterStaticEntry[size];
                }
            };

    @NonNull private final CharSequence mTitle;
    @Nullable private final CharSequence mSummary;
    @Nullable private final PendingIntent mPendingIntent;

    private SafetyCenterStaticEntry(
            @NonNull CharSequence title,
            @Nullable CharSequence summary,
            @Nullable PendingIntent pendingIntent) {
        mTitle = title;
        mSummary = summary;
        mPendingIntent = pendingIntent;
    }

    /** Returns the title that describes this entry. */
    @NonNull
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Returns the optional summary text that describes this entry if present, or {@code null}
     * otherwise.
     */
    @Nullable
    public CharSequence getSummary() {
        return mSummary;
    }

    /**
     * Returns the optional {@link PendingIntent} to execute when this entry is selected if present,
     * or {@code null} otherwise.
     */
    @Nullable
    public PendingIntent getPendingIntent() {
        return mPendingIntent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetyCenterStaticEntry)) return false;
        SafetyCenterStaticEntry that = (SafetyCenterStaticEntry) o;
        return TextUtils.equals(mTitle, that.mTitle)
                && TextUtils.equals(mSummary, that.mSummary)
                && Objects.equals(mPendingIntent, that.mPendingIntent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mSummary, mPendingIntent);
    }

    @Override
    public String toString() {
        return "SafetyCenterStaticEntry{"
                + "mTitle="
                + mTitle
                + ", mSummary="
                + mSummary
                + ", mPendingIntent="
                + mPendingIntent
                + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        TextUtils.writeToParcel(mTitle, dest, flags);
        TextUtils.writeToParcel(mSummary, dest, flags);
        dest.writeTypedObject(mPendingIntent, flags);
    }

    /** Builder class for {@link SafetyCenterStaticEntry}. */
    public static final class Builder {

        @NonNull private CharSequence mTitle;
        @Nullable private CharSequence mSummary;
        @Nullable private PendingIntent mPendingIntent;

        /**
         * Creates a {@link Builder} for a {@link SafetyCenterEntry}.
         *
         * @param title a title that describes this static entry
         */
        public Builder(@NonNull CharSequence title) {
            mTitle = requireNonNull(title);
        }

        /**
         * Creates a {@link Builder} with the values from the given {@link SafetyCenterStaticEntry}.
         */
        public Builder(@NonNull SafetyCenterStaticEntry safetyCenterStaticEntry) {
            mTitle = safetyCenterStaticEntry.mTitle;
            mSummary = safetyCenterStaticEntry.mSummary;
            mPendingIntent = safetyCenterStaticEntry.mPendingIntent;
        }

        /** Sets the title for this entry. */
        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            mTitle = requireNonNull(title);
            return this;
        }

        /** Sets the optional summary text for this entry. */
        @NonNull
        public Builder setSummary(@Nullable CharSequence summary) {
            mSummary = summary;
            return this;
        }

        /** Sets the optional {@link PendingIntent} to execute when this entry is selected. */
        @NonNull
        public Builder setPendingIntent(@Nullable PendingIntent pendingIntent) {
            mPendingIntent = pendingIntent;
            return this;
        }

        /** Creates the {@link SafetyCenterStaticEntry} defined by this {@link Builder}. */
        @NonNull
        public SafetyCenterStaticEntry build() {
            return new SafetyCenterStaticEntry(mTitle, mSummary, mPendingIntent);
        }
    }
}
