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

package android.adservices.measurement;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.Uri;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Deletion Request. */
public class DeletionRequest {

    /**
     * Deletion modes for matched records.
     *
     * @hide
     */
    @IntDef(value = {DELETION_MODE_ALL, DELETION_MODE_EXCLUDE_INTERNAL_DATA})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DeletionMode {}

    /**
     * Matching Behaviors for params.
     *
     * @hide
     */
    @IntDef(value = {MATCH_BEHAVIOR_DELETE, MATCH_BEHAVIOR_PRESERVE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MatchBehavior {}

    /** Deletion mode to delete all data associated with the selected records. */
    public static final int DELETION_MODE_ALL = 0;

    /**
     * Deletion mode to delete all data except the internal data (e.g. rate limits) for the selected
     * records.
     */
    public static final int DELETION_MODE_EXCLUDE_INTERNAL_DATA = 1;

    /** Match behavior option to delete the supplied params (Origin/Domains). */
    public static final int MATCH_BEHAVIOR_DELETE = 0;

    /**
     * Match behavior option to preserve the supplied params (Origin/Domains) and delete everything
     * else.
     */
    public static final int MATCH_BEHAVIOR_PRESERVE = 1;

    private final Instant mStart;
    private final Instant mEnd;
    private final List<Uri> mOriginUris;
    private final List<Uri> mDomainUris;
    private final @MatchBehavior int mMatchBehavior;
    private final @DeletionMode int mDeletionMode;

    private DeletionRequest(@NonNull Builder builder) {
        mOriginUris = builder.mOriginUris;
        mDomainUris = builder.mDomainUris;
        mMatchBehavior = builder.mMatchBehavior;
        mDeletionMode = builder.mDeletionMode;
        mStart = builder.mStart;
        mEnd = builder.mEnd;
    }

    /** Get the list of origin URIs. */
    @NonNull
    public List<Uri> getOriginUris() {
        return mOriginUris;
    }

    /** Get the list of domain URIs. */
    @NonNull
    public List<Uri> getDomainUris() {
        return mDomainUris;
    }

    /** Get the deletion mode. */
    public @DeletionMode int getDeletionMode() {
        return mDeletionMode;
    }

    /** Get the match behavior. */
    public @MatchBehavior int getMatchBehavior() {
        return mMatchBehavior;
    }

    /** Get the start of the deletion range. */
    @NonNull
    public Instant getStart() {
        return mStart;
    }

    /** Get the end of the deletion range. */
    @NonNull
    public Instant getEnd() {
        return mEnd;
    }

    /** Builder for {@link DeletionRequest} objects. */
    public static final class Builder {
        private Instant mStart = Instant.MIN;
        private Instant mEnd = Instant.MAX;
        private List<Uri> mOriginUris;
        private List<Uri> mDomainUris;
        @MatchBehavior private int mMatchBehavior;
        @DeletionMode private int mDeletionMode;

        public Builder() {}

        /**
         * Set the list of origin URI which will be used for matching. These will be matched with
         * records using the same origin only, i.e. subdomains won't match. E.g. If originUri is
         * {@code https://a.example.com}, then {@code https://a.example.com} will match; {@code
         * https://example.com}, {@code https://b.example.com} and {@code https://abcexample.com}
         * will NOT match. A null or empty list will match everything.
         */
        public @NonNull Builder setOriginUris(@Nullable List<Uri> originUris) {
            mOriginUris = originUris;
            return this;
        }

        /**
         * Set the list of domain URI which will be used for matching. These will be matched with
         * records using the same domain or any subdomains. E.g. If domainUri is {@code
         * https://example.com}, then {@code https://a.example.com}, {@code https://example.com} and
         * {@code https://b.example.com} will match; {@code https://abcexample.com} will NOT match.
         * A null or empty list will match everything.
         */
        public @NonNull Builder setDomainUris(@Nullable List<Uri> domainUris) {
            mDomainUris = domainUris;
            return this;
        }

        /**
         * Set the match behavior for the supplied params. {@link #MATCH_BEHAVIOR_DELETE}: This
         * option will use the supplied params (Origin URIs & Domain URIs) for selecting records for
         * deletion. {@link #MATCH_BEHAVIOR_PRESERVE}: This option will preserve the data associated
         * with the supplied params (Origin URIs & Domain URIs) and select remaining records for
         * deletion.
         */
        public @NonNull Builder setMatchBehavior(@MatchBehavior int matchBehavior) {
            mMatchBehavior = matchBehavior;
            return this;
        }

        /**
         * Set the match behavior for the supplied params. {@link #DELETION_MODE_ALL}: All data
         * associated with the selected records will be deleted. {@link
         * #DELETION_MODE_EXCLUDE_INTERNAL_DATA}: All data except the internal system data (e.g.
         * rate limits) associated with the selected records will be deleted.
         */
        public @NonNull Builder setDeletionMode(@DeletionMode int deletionMode) {
            mDeletionMode = deletionMode;
            return this;
        }

        /**
         * Set the start of the deletion range. Passing in {@link java.time.Instant#MIN} will cause
         * everything from the oldest record to the specified end be deleted. No set start will
         * default to {@link java.time.Instant#MIN}.
         */
        public @NonNull Builder setStart(@NonNull Instant start) {
            Objects.requireNonNull(start);
            mStart = start;
            return this;
        }

        /**
         * Set the end of the deletion range. Passing in {@link java.time.Instant#MAX} will cause
         * everything from the specified start until the newest record to be deleted. No set end
         * will default to {@link java.time.Instant#MAX}.
         */
        public @NonNull Builder setEnd(@NonNull Instant end) {
            Objects.requireNonNull(end);
            mEnd = end;
            return this;
        }

        /** Builds a {@link DeletionRequest} instance. */
        public @NonNull DeletionRequest build() {
            if (mDomainUris == null) {
                mDomainUris = new ArrayList<>();
            }
            if (mOriginUris == null) {
                mOriginUris = new ArrayList<>();
            }
            return new DeletionRequest(this);
        }
    }
}
